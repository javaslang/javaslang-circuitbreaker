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

package io.github.resilience4j.micronaut.bulkhead;

import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.micronaut.BaseInterceptor;
import io.github.resilience4j.micronaut.ResilienceInterceptPhase;
import io.github.resilience4j.micronaut.util.PublisherExtension;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * A {@link MethodInterceptor} that intercepts all method calls which are annotated with a {@link io.github.resilience4j.micronaut.annotation.Bulkhead}
 * annotation.
 **/
@Singleton
@Requires(beans = {BulkheadRegistry.class, ThreadPoolBulkheadRegistry.class})
public class BulkheadInterceptor extends BaseInterceptor implements MethodInterceptor<Object, Object> {

    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final ExecutionHandleLocator executionHandleLocator;
    private final PublisherExtension extension;

    /**
     * @param executionHandleLocator                The bean context to allow for DI of class annotated with {@link javax.inject.Inject}.
     * @param bulkheadRegistry           bulkhead registry used to retrieve {@link Bulkhead} by name
     * @param threadPoolBulkheadRegistry thread pool bulkhead registry used to retrieve {@link Bulkhead} by name
     */
    public BulkheadInterceptor(BeanContext executionHandleLocator,
                               BulkheadRegistry bulkheadRegistry, ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry, PublisherExtension extension) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.executionHandleLocator = executionHandleLocator;
        this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
        this.extension = extension;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.BULKHEAD.getPosition();
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
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.micronaut.annotation.Bulkhead.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return executionHandleLocator.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        Optional<AnnotationValue<io.github.resilience4j.micronaut.annotation.Bulkhead>> opt = context.findAnnotation(io.github.resilience4j.micronaut.annotation.Bulkhead.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }
        final io.github.resilience4j.micronaut.annotation.Bulkhead.Type type = opt.get().enumValue("type", io.github.resilience4j.micronaut.annotation.Bulkhead.Type.class).orElse(io.github.resilience4j.micronaut.annotation.Bulkhead.Type.SEMAPHORE);

        if (type == io.github.resilience4j.micronaut.annotation.Bulkhead.Type.THREADPOOL) {
            return handleThreadPoolBulkhead(context, opt.get());
        } else {

            final String name = opt.get().stringValue("name").orElse("default");
            Bulkhead bulkhead = this.bulkheadRegistry.bulkhead(name);

            InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
            try {
                switch (interceptedMethod.resultType()) {
                    case PUBLISHER:
                        return interceptedMethod.handleResult(
                            extension.fallbackPublisher(
                                extension.bulkhead(interceptedMethod.interceptResultAsPublisher(), bulkhead),
                                context,
                                this::findFallbackMethod));

                    case COMPLETION_STAGE:
                        return interceptedMethod.handleResult(
                            fallbackForFuture(
                                bulkhead.executeCompletionStage(() -> {
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
                            return bulkhead.executeCheckedSupplier(context::proceed);
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

    private CompletionStage<?> handleThreadPoolBulkhead(MethodInvocationContext<Object, Object> context, AnnotationValue<io.github.resilience4j.micronaut.annotation.Bulkhead> bulkheadAnnotationValue) {
        final String name = bulkheadAnnotationValue.stringValue().orElse("default");
        ThreadPoolBulkhead bulkhead = this.threadPoolBulkheadRegistry.bulkhead(name);

        InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
        if (interceptedMethod.resultType() == InterceptedMethod.ResultType.COMPLETION_STAGE) {
            try {
                return this.fallbackForFuture(bulkhead.executeSupplier(() -> {
                    try {
                        return ((CompletableFuture<?>) context.proceed()).get();
                    } catch (ExecutionException e) {
                        throw new CompletionException(e.getCause());
                    } catch (Throwable e) {
                        throw new CompletionException(e);
                    }
                }), context);
            } catch (BulkheadFullException ex) {
                CompletableFuture<?> future = new CompletableFuture<>();
                future.completeExceptionally(ex);
                return future;
            }
        }

        throw new IllegalStateException(
            "ThreadPool bulkhead is only applicable for completable futures");
    }
}
