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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// PostponedDecorators isn't used
@Ignore
public class FeignDecoratorsTest {

    @Test
    public void testWithNothing() throws Throwable {
        final FeignDecorators testSubject = FeignDecorators.builder().build();

        final Object result = testSubject.decorate(args -> args[0], null, null, null)
            .apply(new Object[]{"test01"});

        assertThat(result)
            .describedAs("Returned result is correct")
            .isEqualTo("test01");
    }


    @Test
    public void testWithCircuitBreaker() throws Throwable {
        final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        final FeignDecorators testSubject = FeignDecorators.builder()
            .withCircuitBreaker(circuitBreaker).build();

        final Object result = testSubject.decorate(args -> args[0], null, null, null)
            .apply(new Object[]{"test01"});

        assertThat(result)
            .describedAs("Returned result is correct")
            .isEqualTo("test01");
        assertThat(metrics.getNumberOfSuccessfulCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }


    @Test
    public void testWithRateLimiter() throws Throwable {
        final RateLimiter rateLimiter = spy(RateLimiter.ofDefaults("test"));
        final FeignDecorators testSubject = FeignDecorators.builder().withRateLimiter(rateLimiter)
            .build();

        final Object result = testSubject.decorate(args -> args[0], null, null, null)
            .apply(new Object[]{"test01"});

        assertThat(result)
            .describedAs("Returned result is correct")
            .isEqualTo("test01");
        verify(rateLimiter, times(1)).acquirePermission(1);
    }
}
