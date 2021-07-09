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
package io.github.resilience4j.micronaut.ratelimiter;

import io.github.resilience4j.micronaut.BaseInterceptor;
import io.github.resilience4j.micronaut.ResilienceInterceptPhase;
import io.github.resilience4j.micronaut.util.PublisherExtension;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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
@Requires(beans = RateLimiterRegistry.class)
public class RateLimiterInterceptor extends BaseInterceptor implements MethodInterceptor<Object, Object> {
    private final RateLimiterRegistry rateLimiterRegistry;
    private final ExecutionHandleLocator executionHandleLocator;
    private final PublisherExtension extension;

    public RateLimiterInterceptor(ExecutionHandleLocator executionHandleLocator, RateLimiterRegistry rateLimiterRegistry, PublisherExtension extension) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.executionHandleLocator = executionHandleLocator;
        this.extension = extension;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.RATE_LIMITER.getPosition();
    }

    /**
     * Finds a fallback method for the given context.
     *
     * @param context The context
     * @return The fallback method if it is present
     */
    public Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.RateLimiter.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return executionHandleLocator.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }


    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Optional<AnnotationValue<io.github.resilience4j.micronaut.annotation.RateLimiter>> opt = context.findAnnotation(io.github.resilience4j.micronaut.annotation.RateLimiter.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.RateLimiter.class, "name").orElse("default");
        RateLimiter rateLimiter = this.rateLimiterRegistry.rateLimiter(name);

        InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
        try {
            switch (interceptedMethod.resultType()) {
                case PUBLISHER:
                    return interceptedMethod.handleResult(
                        extension.fallbackPublisher(
                            extension.rateLimiter(interceptedMethod.interceptResultAsPublisher(), rateLimiter),
                            context,
                            this::findFallbackMethod));
                case COMPLETION_STAGE:
                    return interceptedMethod.handleResult(
                        fallbackForFuture(
                            rateLimiter.executeCompletionStage(() -> {
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
                        return rateLimiter.executeCheckedSupplier(context::proceed);
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

