/*
 *
 * Copyright 2018
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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.feign.PostponedDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.feign.test.TestService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a fallback.
 */
public class Resilience4jFeignFallbackTest {

    private static final String MOCK_URL = "http://localhost:8080/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;
    private TestService testServiceFallback;

    @Before
    public void setUp() {
        testServiceFallback = mock(TestService.class);
        when(testServiceFallback.greeting()).thenReturn("fallback");

        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback(testServiceFallback);

        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
    }

    @Test
    public void testSuccessful() throws Exception {
        setupStub(200);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFallback() throws Throwable {
        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback("not a fallback");
        Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
    }

    @Test
    public void testFallback() throws Exception {
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void testFallbackExceptionFilter() throws Exception {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback(testServiceExceptionFallback, FeignException.class)
            .withFallback(testServiceFallback);

        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("exception fallback");
        verify(testServiceFallback, times(0)).greeting();
        verify(testServiceExceptionFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void testFallbackExceptionFilterNotCalled() throws Exception {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback(testServiceExceptionFallback, CallNotPermittedException.class)
            .withFallback(testServiceFallback);

        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(testServiceExceptionFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void testFallbackFilter() throws Exception {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback(testServiceFilterFallback, ex -> true)
            .withFallback(testServiceFallback);

        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("filter fallback");
        verify(testServiceFallback, times(0)).greeting();
        verify(testServiceFilterFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void testFallbackFilterNotCalled() throws Exception {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        PostponedDecorators<TestService> decorators = PostponedDecorators.<TestService>builder()
            .withFallback(testServiceFilterFallback, ex -> false)
            .withFallback(testServiceFallback);

        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(testServiceFilterFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void testRevertFallback() throws Exception {
        setupStub(400);

        testService.greeting();
        setupStub(200);
        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(1)).greeting();
        verify(2, getRequestedFor(urlPathEqualTo("/greeting")));

    }

    private void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }
}
