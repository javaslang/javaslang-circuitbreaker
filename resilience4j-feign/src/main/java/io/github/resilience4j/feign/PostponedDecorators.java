package io.github.resilience4j.feign;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO WIP
public class PostponedDecorators<T> {

    private final List<FeignDecorator> fallbacks = new ArrayList<>();
    private final List<UnaryOperator<Supplier<CompletionStage<Object>>>> operations;
    @Nullable
    private Function<Supplier<Object>, Supplier<CompletionStage<Object>>> completionStageWrapper;

    public static <T> PostponedDecorators<T> builder() {
        return new PostponedDecorators<>();
    }

    public PostponedDecorators() {
        this.operations = new ArrayList<>();
    }

    public PostponedDecorators<T> withThreadPoolBulkhead(ThreadPoolBulkhead threadPoolBulkhead) {
        if (this.completionStageWrapper != null) {
            throw new IllegalStateException(
                "CompletionStageWrapper (threadPoolBulkhead) already defined!");
        }
        this.completionStageWrapper = threadPoolBulkhead::decorateSupplier;
        return this;
    }

    public PostponedDecorators<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
        operations.add(supplier ->
            CircuitBreaker.decorateCompletionStage(circuitBreaker, supplier));
        return this;
    }

    public PostponedDecorators<T> withRetry(Retry retryContext,
                                            ScheduledExecutorService scheduler) {
        operations.add(supplier ->
            Retry.decorateCompletionStage(retryContext, scheduler, supplier));
        return this;
    }

    public PostponedDecorators<T> withBulkhead(Bulkhead bulkhead) {
        operations.add(supplier -> Bulkhead.decorateCompletionStage(bulkhead, supplier));
        return this;
    }

    public PostponedDecorators<T> withTimeLimiter(TimeLimiter timeLimiter,
                                                  ScheduledExecutorService scheduler) {
        operations.add(supplier -> timeLimiter.decorateCompletionStage(scheduler, supplier));
        return this;
    }

    public PostponedDecorators<T> withRateLimiter(RateLimiter rateLimiter) {
        return withRateLimiter(rateLimiter, 1);
    }

    public PostponedDecorators<T> withRateLimiter(RateLimiter rateLimiter, int permits) {
        operations.add(supplier ->
            RateLimiter.decorateCompletionStage(rateLimiter, permits, supplier));
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
    public PostponedDecorators<T> withFallback(Object fallback) {
        fallbacks.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback)));
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
    public PostponedDecorators<T> withFallbackFactory(Function<Exception, ?> fallbackFactory) {
        fallbacks.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory)));
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
    public PostponedDecorators<T> withFallback(Object fallback, Class<? extends Exception> filter) {
        fallbacks.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
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
    public PostponedDecorators<T> withFallbackFactory(Function<Exception, ?> fallbackFactory,
                                                      Class<? extends Exception> filter) {
        fallbacks.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
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
    public PostponedDecorators<T> withFallback(Object fallback, Predicate<Exception> filter) {
        fallbacks.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
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
    public PostponedDecorators<T> withFallbackFactory(Function<Exception, ?> fallbackFactory,
                                                      Predicate<Exception> filter) {
        fallbacks.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
        return this;
    }

    Supplier<CompletionStage<Object>> build(Supplier<Object> supplier) {
        if (completionStageWrapper != null) {
            return compose(completionStageWrapper.apply(supplier));
        } else {
            return compose(() -> CompletableFuture.completedFuture(supplier.get()));
        }
    }


    private Supplier<CompletionStage<Object>> compose(Supplier<CompletionStage<Object>> supplier) {
        return operations.stream().reduce(
            (UnaryOperator<Supplier<CompletionStage<Object>>>) x -> x,
            (a, b) -> b.compose(a)::apply,
            (ignore, me) -> null
        ).apply(supplier);
    }

    List<FeignDecorator> getFallbacks() {
        return new ArrayList<>(fallbacks);
    }
}
