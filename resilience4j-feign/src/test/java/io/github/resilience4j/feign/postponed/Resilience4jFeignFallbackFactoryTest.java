/*
 *
 * Copyright 2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign.postponed;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.FeignException;
import io.github.resilience4j.feign.PostponedDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.feign.test.TestServiceFallbackThrowingException;
import io.github.resilience4j.feign.test.TestServiceFallbackWithException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * Unit tests on fallback factories.
 */
public class Resilience4jFeignFallbackFactoryTest {

    @ClassRule
    public static final WireMockClassRule WIRE_MOCK_RULE = new WireMockClassRule(8080);
    private static final String MOCK_URL = "http://localhost:8080/";
    @Rule
    public WireMockClassRule instanceRule = WIRE_MOCK_RULE;

    private static TestService buildTestService(Function<Exception, ?> fallbackSupplier) {
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withFallbackFactory(fallbackSupplier);
        return Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
    }

    private static void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }

    @Test
    public void should_successfully_get_a_response() {
        setupStub(200);
        TestService testService = buildTestService(e -> "my fallback");

        String result = testService.greeting();

        assertThat(result).isEqualTo("Hello, world!");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_lazily_fail_on_invalid_fallback() {
        TestService testService = buildTestService(e -> "my fallback");

        Throwable throwable = catchThrowable(testService::greeting);

        assertThat(throwable).isNotNull()
            .hasMessageContaining(
                "Cannot use the fallback [class java.lang.String] for [interface io.github.resilience4j.feign.test.TestService]");
    }

    @Test
    public void should_go_to_fallback_and_consume_exception() {
        setupStub(400);
        TestService testService = buildTestService(TestServiceFallbackWithException::new);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_go_to_fallback_and_rethrow_an_exception_thrown_in_fallback() {
        setupStub(400);
        TestService testService = buildTestService(e -> new TestServiceFallbackThrowingException());

        Throwable result = catchThrowable(testService::greeting);

        assertThat(result).isNotNull()
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Exception in greeting fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_go_to_fallback_and_consume_exception_with_exception_filter() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withFallbackFactory(TestServiceFallbackWithException::new, FeignException.class)
            .withFallbackFactory(e -> uselessFallback);
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_go_to_second_fallback_and_consume_exception_with_exception_filter() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withFallbackFactory(e -> uselessFallback, MyException.class)
            .withFallbackFactory(TestServiceFallbackWithException::new);
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_go_to_fallback_and_consume_exception_with_predicate() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withFallbackFactory(TestServiceFallbackWithException::new,
                FeignException.class::isInstance)
            .withFallbackFactory(e -> uselessFallback);
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void should_go_to_second_fallback_and_consume_exception_with_predicate() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withFallbackFactory(e -> uselessFallback, MyException.class::isInstance)
            .withFallbackFactory(TestServiceFallbackWithException::new);
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    private static class MyException extends Exception {

    }
}
