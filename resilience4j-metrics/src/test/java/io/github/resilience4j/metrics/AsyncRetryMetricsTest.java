package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import javax.xml.ws.WebServiceException;
import java.util.concurrent.*;

import static io.github.resilience4j.retry.utils.MetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class AsyncRetryMetricsTest {

    private MetricRegistry metricRegistry;
    private AsyncHelloWorldService helloWorldService;
    private ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor();;

    @Before
    public void setUp(){
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(AsyncHelloWorldService.class);
    }

    @Test
    public void shouldRegisterMetricsWithoutRetry() throws Throwable {
        //Given
        AsyncRetryRegistry retryRegistry = AsyncRetryRegistry.ofDefaults();
        AsyncRetry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(AsyncRetryMetrics.ofAsyncRetryRegistry(retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld())
          .willReturn(CompletableFuture.completedFuture("Hello world"));

        // Setup circuitbreaker with retry
        String value = awaitResult(retry.executeCompletionStage(scheduler, helloWorldService::returnHelloWorld));

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldRegisterMetricsWithRetry() throws Throwable {
        //Given
        AsyncRetryRegistry retryRegistry = AsyncRetryRegistry.ofDefaults();
        AsyncRetry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(AsyncRetryMetrics.ofAsyncRetryRegistry(retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"))
                .willReturn(CompletableFuture.completedFuture("Hello world"))
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"));

        // Setup circuitbreaker with retry
        String value1 = awaitResult(retry.executeCompletionStage(scheduler, helloWorldService::returnHelloWorld));
        Try.ofCallable(() -> awaitResult(AsyncRetry.decorateCompletionStage(retry, scheduler, helloWorldService::returnHelloWorld).get()));

        //Then
        assertThat(value1).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 5 times
        BDDMockito.then(helloWorldService).should(times(5)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        AsyncRetryRegistry retryRegistry = AsyncRetryRegistry.ofDefaults();
        AsyncRetry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(AsyncRetryMetrics.ofAsyncRetryRegistry("testPrefix",retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn(CompletableFuture.completedFuture("Hello world"));

        String value = awaitResult(retry.executeCompletionStage(scheduler, helloWorldService::returnHelloWorld));

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }

    private static <T> T awaitResult(CompletionStage<T> completionStage) {
        try {
            return completionStage.toCompletableFuture().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new RuntimeExecutionException(e.getCause());
        }
    }

    private static class RuntimeExecutionException extends RuntimeException {
        RuntimeExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
