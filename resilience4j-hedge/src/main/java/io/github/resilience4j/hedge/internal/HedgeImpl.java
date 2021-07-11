/*
 *
 *  Copyright 2021: Matthew Sandoz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.*;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class HedgeImpl implements Hedge {

    private static final Logger LOG = LoggerFactory.getLogger(HedgeImpl.class);

    private final String name;
    private final Map<String, String> tags;
    private final HedgeConfig hedgeConfig;
    private final HedgeEventProcessor eventProcessor;
    private final HedgeMetrics metrics;

    public HedgeImpl(String name, HedgeConfig hedgeConfig) {
        this(name, hedgeConfig, HashMap.empty());
    }

    public HedgeImpl(String name, HedgeConfig hedgeConfig,
                     io.vavr.collection.Map<String, String> tags) {
        this.name = name;
        this.tags = Objects.requireNonNull(tags, "Tags must not be null");
        this.hedgeConfig = hedgeConfig;
        this.eventProcessor = new HedgeEventProcessor();
        this.metrics = hedgeConfig.newMetrics();
        if (metrics.requiresInput()) {
            eventProcessor.onPrimarySuccess(metrics::accept);
            eventProcessor.onPrimaryFailure(metrics::accept);
            eventProcessor.onHedgeSuccess(metrics::accept);
            eventProcessor.onHedgeFailure(metrics::accept);
        }
    }

    @Override
    public HedgeMetrics getMetrics() {
        return metrics;
    }

    @Override
    public <T, F extends Future<T>> Supplier<F> decorateFuture(Supplier<F> futureSupplier) {
        return () -> {
            long start = System.nanoTime();
            io.vavr.concurrent.Future<HedgeResult<T>> primary = io.vavr.concurrent.Future.fromJavaFuture(futureSupplier.get())
                .transformValue(ts -> {
                    if (ts.isSuccess()) {
                        return Try.success(HedgeResult.of(ts.get(), true, false, null));
                    } else {
                        return Try.success(HedgeResult.of(null, true, true, ts.getCause()));
                    }
                });
            io.vavr.concurrent.Future<HedgeResult<T>> hedged = io.vavr.concurrent.Future.of(primary.executor(), () -> {
                TimeUnit.MILLISECONDS.sleep(this.metrics.getResponseTimeCutoff().toMillis());
                if (primary.isCompleted()) {
                    throw new RuntimeException("hedge canceled");
                } else {
                    HedgeResult<T> result;
                    try {
                        result = HedgeResult.of(futureSupplier.get().get(), false, false, null);
                    } catch (Exception e) {
                        result = HedgeResult.of(null, false, true, e);
                    }
                    return result;
                }
            });

            io.vavr.concurrent.Future<T> combined = io.vavr.concurrent.Future.firstCompletedOf(Arrays.asList(primary, hedged))
                .transformValue(
                    hedgeResults -> {
                        long duration = System.nanoTime() - start;
                        HedgeResult<T> result = hedgeResults.get();
                        if (result.failed) {
                            if (result.fromPrimary) {
                                onPrimaryFailure(Duration.ofNanos(duration), result.throwable);
                            } else {
                                onHedgedFailure(Duration.ofNanos(duration), result.throwable);
                            }
                            return Try.failure(result.throwable);
                        } else {
                            if (result.fromPrimary) {
                                onPrimarySuccess(Duration.ofNanos(duration));
                            } else {
                                onHedgedSuccess(Duration.ofNanos(duration));
                            }
                            return Try.success(result.value);
                        }
                    }
                );
            Future<T> combinedFuture = combined.toCompletableFuture();
            return (F) combinedFuture;
        };
    }

    @Override
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        Supplier<F> supplier) {
        return () -> {
            long start = System.nanoTime();
            CompletableFuture<HedgeResult<T>> supplied = supplier.get().toCompletableFuture()
                .handle((t, throwable) -> new HedgeResult<>(t, true, throwable != null, throwable));
            CompletableFuture<HedgeResult<T>> hedged = completeAfter(metrics.getResponseTimeCutoff().toMillis())
                .thenCompose((t) -> supplier.get().toCompletableFuture())
                .handle((t, throwable) -> new HedgeResult<>(t, false, throwable != null, throwable));
            return supplied
                .applyToEither(hedged, Function.identity())
                .handle((t, throwable) -> {
                    long duration = System.nanoTime() - start;
                    if (t.fromPrimary) {
                        hedged.cancel(false);
                        if (t.failed) {
                            onPrimaryFailure(Duration.ofNanos(duration), t.throwable);
                            throw (RuntimeException) t.throwable;
                        } else {
                            onPrimarySuccess(Duration.ofNanos(duration));
                            return t.value;
                        }
                    } else {
                        supplied.cancel(false);
                        if (t.failed) {
                            onHedgedFailure(Duration.ofNanos(duration), t.throwable);
                            throw (RuntimeException) t.throwable;
                        } else {
                            onHedgedSuccess(Duration.ofNanos(duration));
                            return t.value;
                        }
                    }
                });
        };
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public HedgeConfig getHedgeConfig() {
        return hedgeConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onPrimarySuccess(Duration duration) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new PrimarySuccessEvent(name, duration));
        }
    }

    @Override
    public void onHedgedSuccess(Duration duration) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeSuccessEvent(name, duration));
        }
    }

    @Override
    public void onPrimaryFailure(Duration duration, Throwable throwable) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new PrimaryFailureEvent(name, duration, throwable));
        }
    }

    @Override
    public void onHedgedFailure(Duration duration, Throwable throwable) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeFailureEvent(name, duration, throwable));
        }
    }

    private void publishEvent(HedgeEvent event) {
        try {
            eventProcessor.consumeEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }

    static <T> CompletableFuture<T> completeAfter(long delay) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        try {
            LOG.warn("starting sleep from hedge");
            TimeUnit.MILLISECONDS.sleep(delay);
            LOG.warn("done with sleep from hedge - about to move to next stage.");
            completableFuture.complete(null);
        } catch (InterruptedException e) {
            //do nothing
        }
        return completableFuture;
    }

    static final class HedgeResult<T> {
        final Throwable throwable;
        final boolean failed;
        final boolean fromPrimary;
        final T value;

        private static <T> HedgeResult<T> of(T value, boolean fromPrimary, boolean failed, Throwable throwable) {
            return new HedgeResult<>(value, fromPrimary, failed, throwable);
        }

        private HedgeResult(T value, boolean fromPrimary, boolean failed, Throwable throwable) {
            this.fromPrimary = fromPrimary;
            this.value = value;
            this.failed = failed;
            this.throwable = throwable;
        }
    }
}

