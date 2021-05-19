/*
 *
 * Copyright 2020
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
package io.github.resilience4j.feign;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Builder to help build stacked decorators. <br>
 *
 * <pre>
 * {
 *     &#64;code
 *     CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
 *     RateLimiter rateLimiter = RateLimiter.ofDefaults("backendName");
 *     FeignDecorators decorators = FeignDecorators.builder()
 *             .withCircuitBreaker(circuitBreaker)
 *             .withRateLimiter(rateLimiter)
 *             .build();
 *     MyService myService = Resilience4jFeign.builder(decorators).target(MyService.class, "http://localhost:8080/");
 * }
 * </pre>
 * <p>
 * The order in which decorators are applied correspond to the order in which they are declared. For
 * example, calling {@link FeignDecorators.Builder#withFallback(Object)} before {@link
 * FeignDecorators.Builder#withCircuitBreaker(CircuitBreaker)} would mean that the fallback is
 * called when the HTTP request fails, but would no longer be reachable if the CircuitBreaker were
 * open. However, reversing the order would mean that the fallback is called both when the HTTP
 * request fails and when the CircuitBreaker is open. <br> So be wary of this when designing your
 * "resilience" strategy.
 */
public class FeignDecorators implements FeignDecorator {

    private final List<FeignDecorator> decorators;

    FeignDecorators(List<FeignDecorator> decorators) {
        this.decorators = decorators;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn,
        Method method, MethodHandler methodHandler, Target<?> target) {
        CheckedFunction1<Object[], Object> decoratedFn = fn;
        for (final FeignDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method, methodHandler, target);
        }
        return decoratedFn;
    }

    public static final class Builder {

        private final List<FeignDecorator> decorators = new ArrayList<>();


        /**
         * Adds a {@link Retry} to the decorator chain.
         *
         * @param retry a fully configured {@link Retry}.
         * @return the builder
         */
        public Builder withRetry(Retry retry) {
            addFeignDecorator(fn -> Retry.decorateCheckedFunction(retry, fn));
            return this;
        }

        /**
         * Adds a {@link CircuitBreaker} to the decorator chain.
         *
         * @param circuitBreaker a fully configured {@link CircuitBreaker}.
         * @return the builder
         */
        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            addFeignDecorator(fn -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        /**
         * Adds a {@link RateLimiter} to the decorator chain.
         *
         * @param rateLimiter a fully configured {@link RateLimiter}.
         * @return the builder
         */
        public Builder withRateLimiter(RateLimiter rateLimiter) {
            addFeignDecorator(fn -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *                 {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @return the builder
         */
        public Builder withFallback(Object fallback) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback)));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception
         * thrown on error. Multiple fallbacks can be applied with the next fallback being called
         * when the previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when
         *                        calling {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory)));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *                 {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter   only {@link Exception}s matching the specified {@link Exception} will
         *                 trigger the fallback.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception
         * thrown on error. Multiple fallbacks can be applied with the next fallback being called
         * when the previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when
         *                        calling {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter          only {@link Exception}s matching the specified {@link Exception}
         *                        will trigger the fallback.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory,
            Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *                 {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter   the filter must return <code>true</code> for the fallback to be called.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. A factory can consume the exception thrown on
         * error. Multiple fallbacks can be applied with the next fallback being called when the
         * previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when
         *                        calling {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter          the filter must return <code>true</code> for the fallback to be
         *                        called.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory,
            Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }


        /**
         * Adds a {@link Bulkhead} to the decorator chain.
         *
         * @param bulkhead a fully configured {@link Bulkhead}.
         * @return the builder
         */
        public Builder withBulkhead(Bulkhead bulkhead) {
            addFeignDecorator(fn -> Bulkhead.decorateCheckedFunction(bulkhead, fn));
            return this;
        }

        private void addFeignDecorator(UnaryOperator<CheckedFunction1<Object[], Object>> decorator) {
            decorators
                .add((fn, m, mh, t) -> {
                    // prevent default methods from being decorated
                    // as they do not participate in actual web requests
                    if (m.isDefault()) {
                        return fn;
                    } else {
                        return decorator.apply(fn);
                    }
                });
        }

        /**
         * Builds the decorator chain. This can then be used to setup an instance of {@link
         * Resilience4jFeign}.
         *
         * @return the decorators.
         */
        public FeignDecorators build() {
            return new FeignDecorators(decorators);
        }

    }

}
