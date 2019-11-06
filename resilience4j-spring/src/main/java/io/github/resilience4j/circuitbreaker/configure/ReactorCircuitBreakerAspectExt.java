/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.circuitbreaker.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.vavr.CheckedFunction0;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * the Reactor breaker logic support for the spring AOP Conditional on Reactor
 * class existence on spring class loader
 */
public class ReactorCircuitBreakerAspectExt implements CircuitBreakerAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(ReactorCircuitBreakerAspectExt.class);

    @Override
    public boolean canHandleReturnType(Class returnType) {
        return (Flux.class.isAssignableFrom(returnType)) || (Mono.class.isAssignableFrom(returnType));
    }

    /**
     * handle the Spring web flux (Flux /Mono) return types AOP based into
     * reactor circuit-breaker See
     * {@link io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator}
     * for details.
     */
    @Override
    public CheckedFunction0<Object> decorate(CircuitBreaker circuitBreaker, CheckedFunction0<Object> supplier) {
        return () -> {
            Object returnValue = supplier.apply();
            return handleReturnValue(circuitBreaker, returnValue);
        };
    }

    private Object handleReturnValue(CircuitBreaker circuitBreaker, Object returnValue) {
        if (Flux.class.isAssignableFrom(returnValue.getClass())) {
            Flux<?> fluxReturnValue = (Flux<?>) returnValue;
            return fluxReturnValue.compose(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator.of(circuitBreaker));
        } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
            Mono<?> monoReturnValue = (Mono<?>) returnValue;
            return monoReturnValue.compose(CircuitBreakerOperator.of(circuitBreaker));
        } else {
            logger.error("Unsupported type for Reactor circuit breaker {}", returnValue.getClass().getTypeName());
            throw new IllegalArgumentException("Not Supported type for the circuit breaker in Reactor:" + returnValue.getClass().getName());

        }
    }
}
