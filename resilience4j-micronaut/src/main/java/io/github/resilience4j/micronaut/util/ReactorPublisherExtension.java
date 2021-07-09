package io.github.resilience4j.micronaut.util;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.AbstractSubscriber;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.retry.exception.FallbackException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.Function;

@Singleton
@Requires(classes = {Flux.class, AbstractSubscriber.class})
public class ReactorPublisherExtension implements PublisherExtension {
    private static final Logger logger = LoggerFactory.getLogger(ReactorPublisherExtension.class);

    @Override
    public <T> Publisher<T> bulkhead(Publisher<T> publisher, Bulkhead bulkhead) {
        return Flux.from(publisher)
            .transformDeferred(BulkheadOperator.of(bulkhead));
    }

    @Override
    public <T> Publisher<T> circuitBreaker(Publisher<T> publisher, CircuitBreaker circuitBreaker) {
        return Flux.from(publisher)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    @Override
    public <T> Publisher<T> timeLimiter(Publisher<T> publisher, TimeLimiter timeLimiter) {
        return Flux.from(publisher)
            .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }

    @Override
    public <T> Publisher<T> retry(Publisher<T> publisher, Retry retry) {
        return Flux.from(publisher)
            .transformDeferred(RetryOperator.of(retry));
    }

    @Override
    public <T> Publisher<T> rateLimiter(Publisher<T> publisher, RateLimiter rateLimiter) {
        return Flux.from(publisher)
            .transformDeferred(RateLimiterOperator.of(rateLimiter));
    }

    @Override
    public <T> Publisher<T> fallbackPublisher(Publisher<T> publisher, MethodInvocationContext<Object, Object> context, Function<MethodInvocationContext<Object, Object>, Optional<? extends MethodExecutionHandle<?, Object>>> handler) {
        return Flux.from(publisher).onErrorResume(throwable -> {
            Optional<? extends MethodExecutionHandle<?, Object>> fallbackMethod = handler.apply(context);
            if (fallbackMethod.isPresent()) {
                MethodExecutionHandle<?, Object> fallbackHandle = fallbackMethod.get();
                if (logger.isDebugEnabled()) {
                    logger.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass(), fallbackHandle);
                }
                Object fallbackResult;
                try {
                    fallbackResult = fallbackHandle.invoke(context.getParameterValues());
                } catch (Exception e) {
                    return Flux.error(throwable);
                }
                if (fallbackResult == null) {
                    return Flux.error(new FallbackException("Fallback handler [" + fallbackHandle + "] returned null value"));
                } else {
                    return ConversionService.SHARED.convert(fallbackResult, Publisher.class)
                        .orElseThrow(() -> new FallbackException("Unsupported Reactive type: " + fallbackResult));
                }
            }
            return Flux.error(throwable);
        });
    }
}
