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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.feign.PostponedDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.feign.test.TestService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link CircuitBreaker}
 */
public class Resilience4jFeignCircuitBreakerTest {

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
        .slidingWindowSize(3)
        .waitDurationInOpenState(Duration.ofMillis(1000))
        .build();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();
    private CircuitBreaker circuitBreaker;
    private TestService testService;

    @Before
    public void setUp() {
        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        PostponedDecorators<?> decorators = PostponedDecorators.builder()
            .withCircuitBreaker(circuitBreaker);
        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, "http://localhost:8080/");
    }

    @Test
    public void testSuccessfulCall() throws Exception {
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        setupStub(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        assertThat(metrics.getNumberOfSuccessfulCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }

    @Test
    public void testFailedCall() throws Exception {
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        boolean exceptionThrown = false;

        setupStub(400);

        try {
            testService.greeting();
        } catch (final FeignException ex) {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown)
            .describedAs("FeignException thrown")
            .isTrue();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }

    @Test
    public void testCircuitBreakerOpen() throws Exception {
        boolean exceptionThrown = false;
        final int threshold = circuitBreaker
            .getCircuitBreakerConfig()
            .getSlidingWindowSize() + 1;

        setupStub(400);

        for (int i = 0; i < threshold; i++) {
            try {
                testService.greeting();
            } catch (final FeignException ex) {
                // ignore
            } catch (final CallNotPermittedException ex) {
                exceptionThrown = true;
            }
        }

        assertThat(exceptionThrown)
            .describedAs("CallNotPermittedException thrown")
            .isTrue();
        assertThat(circuitBreaker.tryAcquirePermission())
            .describedAs("CircuitBreaker Closed")
            .isFalse();
    }


    @Test
    public void testCircuitBreakerClosed() throws Exception {
        boolean exceptionThrown = false;
        final int threshold = circuitBreaker
            .getCircuitBreakerConfig()
            .getSlidingWindowSize() - 1;

        setupStub(400);

        for (int i = 0; i < threshold; i++) {
            try {
                testService.greeting();
            } catch (final FeignException ex) {
                // ignore
            } catch (final CallNotPermittedException ex) {
                exceptionThrown = true;
            }
        }

        assertThat(exceptionThrown)
            .describedAs("CallNotPermittedException thrown")
            .isFalse();
        assertThat(circuitBreaker.tryAcquirePermission())
            .describedAs("CircuitBreaker Closed")
            .isTrue();
    }

    private void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));
    }
}
