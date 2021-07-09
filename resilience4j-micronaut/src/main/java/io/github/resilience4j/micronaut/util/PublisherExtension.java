package io.github.resilience4j.micronaut.util;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.inject.MethodExecutionHandle;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.function.Function;

public interface PublisherExtension {
    <T> Publisher<T> bulkhead(Publisher<T> publisher, Bulkhead handler);

    <T> Publisher<T> circuitBreaker(Publisher<T> publisher, CircuitBreaker handler);

    <T> Publisher<T> timeLimiter(Publisher<T> publisher, TimeLimiter handler);

    <T> Publisher<T> retry(Publisher<T> publisher, Retry handler);

    <T> Publisher<T> rateLimiter(Publisher<T> publisher, RateLimiter handler);

    <T> Publisher<T> fallbackPublisher(Publisher<T> publisher, MethodInvocationContext<Object, Object> context,  Function<MethodInvocationContext<Object, Object>, Optional<? extends MethodExecutionHandle<?, Object>>> handler);
}
