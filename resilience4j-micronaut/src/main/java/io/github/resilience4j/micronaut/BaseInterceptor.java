/*
 * Copyright 2020 Michael Pollind, Mahmoud Romeh
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
package io.github.resilience4j.micronaut;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.discovery.exceptions.NoAvailableServiceException;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.retry.exception.FallbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public abstract class BaseInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(BaseInterceptor.class);

    public abstract Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context);

    /**
     * Resolves a fallback for the given execution context and exception.
     *
     * @param context   The context
     * @param exception The exception
     * @return Returns the fallback value or throws the original exception
     */
    public Object fallback(MethodInvocationContext<Object, Object> context, Throwable exception) {
        if (exception instanceof NoAvailableServiceException) {
            NoAvailableServiceException ex = (NoAvailableServiceException) exception;
            if (logger.isErrorEnabled()) {
                logger.debug(ex.getMessage(), ex);
                logger.error("Type [{}] attempting to resolve fallback for unavailable service [{}]", context.getTarget().getClass().getName(), ex.getServiceID());
            }
        } else {
            if (logger.isErrorEnabled()) {
                logger.error("Type [{}]  executed with error: {}", context.getTarget().getClass().getName(), exception.getMessage(), exception);
            }
        }
        Optional<? extends MethodExecutionHandle<?, Object>> fallback = findFallbackMethod(context);
        if (fallback.isPresent()) {
            MethodExecutionHandle<?, Object> fallbackMethod = fallback.get();
            if (logger.isDebugEnabled()) {
                logger.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass().getName(), fallbackMethod);
            }
            return fallbackMethod.invoke(context.getParameterValues());
        } else {
            if(exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new CompletionException(exception);
            }
        }
    }

    public CompletionStage<?> fallbackForFuture(CompletionStage<?> result, MethodInvocationContext<Object, Object> context) {
        CompletableFuture<Object> newFuture = new CompletableFuture<>();
        result.whenComplete((o, throwable) -> {
            if (throwable == null) {
                newFuture.complete(o);
            } else {
                Optional<? extends MethodExecutionHandle<?, Object>> fallbackMethod = findFallbackMethod(context);
                if (fallbackMethod.isPresent()) {
                    MethodExecutionHandle<?, Object> fallbackHandle = fallbackMethod.get();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass(), fallbackHandle);
                    }
                    try {
                        CompletableFuture<Object> resultingFuture = (CompletableFuture<Object>) fallbackHandle.invoke(context.getParameterValues());
                        if (resultingFuture == null) {
                            newFuture.completeExceptionally(new FallbackException("Fallback handler [" + fallbackHandle + "] returned null value"));
                        } else {
                            resultingFuture.whenComplete((o1, throwable1) -> {
                                if (throwable1 == null) {
                                    newFuture.complete(o1);
                                } else {
                                    newFuture.completeExceptionally(throwable1);
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error("Error invoking Fallback [{}]: ", fallbackHandle, e);
                        }
                        newFuture.completeExceptionally(throwable);
                    }
                } else {
                    newFuture.completeExceptionally(throwable);
                }
            }
        });
        return newFuture;
    }
}
