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
package io.github.resilience4j.micronaut.timelimiter;

import io.github.resilience4j.micronaut.BaseInterceptor;
import io.github.resilience4j.micronaut.ResilienceInterceptPhase;
import io.github.resilience4j.micronaut.util.PublisherExtension;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.scheduling.TaskExecutors;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
@Requires(beans = TimeLimiterRegistry.class)
public class TimeLimiterInterceptor extends BaseInterceptor implements MethodInterceptor<Object,Object> {

    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ExecutionHandleLocator executionHandleLocator;
    private final ScheduledExecutorService executorService;
    private final PublisherExtension extension;

    public TimeLimiterInterceptor(ExecutionHandleLocator executionHandleLocator, TimeLimiterRegistry timeLimiterRegistry, @Named(TaskExecutors.SCHEDULED) ExecutorService executorService, PublisherExtension extension) {
        this.executionHandleLocator = executionHandleLocator;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.executorService = (ScheduledExecutorService) executorService;
        this.extension = extension;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.TIME_LIMITER.getPosition();
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
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.TimeLimiter.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return executionHandleLocator.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Optional<AnnotationValue<io.github.resilience4j.micronaut.annotation.TimeLimiter>> opt = context.findAnnotation(io.github.resilience4j.micronaut.annotation.TimeLimiter.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }

        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.TimeLimiter.class).orElse("default");
        TimeLimiter timeLimiter = this.timeLimiterRegistry.timeLimiter(name);

        InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
        try {
            switch (interceptedMethod.resultType()) {
                case PUBLISHER:
                    return interceptedMethod.handleResult(
                        extension.fallbackPublisher(
                            extension.timeLimiter(interceptedMethod.interceptResultAsPublisher(), timeLimiter),
                            context,
                            this::findFallbackMethod));

                case COMPLETION_STAGE:
                    return interceptedMethod.handleResult(
                        fallbackForFuture(
                            timeLimiter.executeCompletionStage(executorService, () -> {
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
                        return timeLimiter.executeFutureSupplier(
                            () -> CompletableFuture.supplyAsync(context::proceed));
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
