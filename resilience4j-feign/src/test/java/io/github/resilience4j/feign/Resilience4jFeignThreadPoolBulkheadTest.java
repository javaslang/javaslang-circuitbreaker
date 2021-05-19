package io.github.resilience4j.feign;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import feign.codec.StringDecoder;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.feign.test.CompletionStageTestService;
import io.vavr.control.Try;
import org.assertj.core.util.Throwables;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a ThreadPoolBulkhead.
 */
public class Resilience4jFeignThreadPoolBulkheadTest {

    private static final String MOCK_URL = "http://localhost:8080/";
    private static final String PATH = "/greeting";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private CompletionStageTestService testService;
    private ThreadPoolBulkhead bulkhead;

    @Before
    public void setUp() {
        bulkhead = createThreadPoolBulkhead();
        PostponedDecorators<CompletionStageTestService> postponedDecorators =
            PostponedDecorators.<CompletionStageTestService>builder()
                .withThreadPoolBulkhead(bulkhead);
        testService = createFeignTestService(postponedDecorators);
    }

    @After
    public void tearDown() {
        wireMockRule.resetAll();
    }

    public ThreadPoolBulkhead createThreadPoolBulkhead() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .queueCapacity(1)
            .coreThreadPoolSize(1)
            .maxThreadPoolSize(1)
            .build();
        return ThreadPoolBulkhead.of("test", config);
    }

    public CompletionStageTestService createFeignTestService(
        PostponedDecorators<CompletionStageTestService> postponedDecorators) {
        return Resilience4jFeign.builder(postponedDecorators)
            .decoder(new CompletableFutureDecoder(new StringDecoder()))
            .target(CompletionStageTestService.class, MOCK_URL);
    }

    private void givenHelloWithResponse(int statusCode) {
        stubFor(get(urlPathEqualTo(PATH))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));
    }

    String getHelloSync() throws ExecutionException, InterruptedException {
        return testService.greeting().toCompletableFuture().get();
    }

    @Test
    public void testSuccessfulCall() throws Exception {
        givenHelloWithResponse(200);

        testService.greeting().toCompletableFuture().get();

        verify(getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    public void testSuccessfulWithCircuitBreaker() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker
            .of("test", CircuitBreakerConfig.ofDefaults());
        bulkhead = createThreadPoolBulkhead();
        PostponedDecorators<CompletionStageTestService> postponedDecorators =
            PostponedDecorators.<CompletionStageTestService>builder()
                .withThreadPoolBulkhead(bulkhead)
                .withCircuitBreaker(circuitBreaker);
        testService = createFeignTestService(postponedDecorators);
        givenHelloWithResponse(200);

        testService.greeting().toCompletableFuture().get();

        verify(getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    public void testFailedWithCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreaker
            .of("test", CircuitBreakerConfig.ofDefaults());
        circuitBreaker.transitionToOpenState();
        AtomicReference<CircuitBreakerEvent> pastEvent = new AtomicReference<>();
        circuitBreaker.getEventPublisher().onEvent(pastEvent::set);
        bulkhead = createThreadPoolBulkhead();
        PostponedDecorators<CompletionStageTestService> postponedDecorators =
            PostponedDecorators.<CompletionStageTestService>builder()
                .withThreadPoolBulkhead(bulkhead)
                .withCircuitBreaker(circuitBreaker);
        testService = createFeignTestService(postponedDecorators);
        givenHelloWithResponse(200);

        Try<String> result = Try.ofCallable(this::getHelloSync);

        assertThat(result.getCause()).hasRootCauseInstanceOf(CallNotPermittedException.class);
        verify(0, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    public void testBulkheadFull() throws Exception {
        givenHelloWithResponse(200);

        List<Callable<String>> tasks = Arrays.asList(
            this::getHelloSync,
            this::getHelloSync,
            this::getHelloSync);
        ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
        List<Future<String>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();

        List<Throwable> errors = io.vavr.collection.List.ofAll(futures)
            .map(f -> Try.of(f::get))
            .filter(Try::isFailure)
            .map(Try::getCause)
            .peek(Throwable::printStackTrace)
            .map(Throwables::getRootCause)
            .toJavaList();

        assertThat(errors).extracting(Object::getClass)
            .anyMatch(c -> c.equals(BulkheadFullException.class));
        verify(2, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    public void testFailedCall() {
        givenHelloWithResponse(400);

        Try<String> result = Try.ofCallable(this::getHelloSync);

        assertThat(result.getCause()).hasRootCauseInstanceOf(FeignException.BadRequest.class);
    }
}
