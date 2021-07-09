/*
 * Copyright 2019 Michael Pollind
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
package io.github.resilience4j.micronaut.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micronaut.BaseInterceptor;
import io.github.resilience4j.micronaut.ResilienceInterceptPhase;
import io.github.resilience4j.micronaut.util.PublisherExtension;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletionException;

@Singleton
@Requires(beans = CircuitBreakerRegistry.class)
public class CircuitBreakerInterceptor extends BaseInterceptor implements MethodInterceptor<Object,Object> {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ExecutionHandleLocator executionHandleLocator;
    private final PublisherExtension extension;

    public CircuitBreakerInterceptor(ExecutionHandleLocator executionHandleLocator, CircuitBreakerRegistry circuitBreakerRegistry, PublisherExtension extension) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.executionHandleLocator = executionHandleLocator;
        this.extension = extension;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.CIRCUIT_BREAKER.getPosition();
    }

    /**
     * Finds a fallback method for the given context.
     *
     * @param context The context
     * @return The fallback method if it is present
     */
    @Override
    public Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.CircuitBreaker.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return executionHandleLocator.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Optional<AnnotationValue<io.github.resilience4j.micronaut.annotation.CircuitBreaker>> opt = context.findAnnotation(io.github.resilience4j.micronaut.annotation.CircuitBreaker.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.CircuitBreaker.class, "name").orElse("default");
        CircuitBreaker circuitBreaker = this.circuitBreakerRegistry.circuitBreaker(name);

        InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
        try {
            switch (interceptedMethod.resultType()) {
                case PUBLISHER:
                    return interceptedMethod.handleResult(
                        extension.fallbackPublisher(
                            extension.circuitBreaker(interceptedMethod.interceptResultAsPublisher(), circuitBreaker),
                            context,
                            this::findFallbackMethod));
                case COMPLETION_STAGE:
                    return interceptedMethod.handleResult(
                        fallbackForFuture(
                            circuitBreaker.executeCompletionStage(() -> {
                                try {
                                    return interceptedMethod.interceptResultAsCompletionStage();
                                } catch (Exception e) {
                                    throw new CompletionException(e);
                                }
                            }),
                            context)
                    );
                case SYNCHRONOUS:
                    try {
                        return circuitBreaker.executeCheckedSupplier(context::proceed);
                    } catch (Throwable exception) {
                        return fallback(context, exception);
                    }
                default:
                    return interceptedMethod.unsupported();
            }
        } catch (Exception e) {
            return interceptedMethod.handleException(e);
        }

    }
}
