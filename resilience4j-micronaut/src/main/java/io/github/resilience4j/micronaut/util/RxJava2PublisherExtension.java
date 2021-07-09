package io.github.resilience4j.micronaut.util;

import io.github.resilience4j.AbstractSubscriber;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.transformer.RetryTransformer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.transformer.TimeLimiterTransformer;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.retry.exception.FallbackException;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.Function;

@Singleton
@Requires(classes = {Flowable.class, AbstractSubscriber.class})
public class RxJava2PublisherExtension implements PublisherExtension {
    private static final Logger logger = LoggerFactory.getLogger(RxJava2PublisherExtension.class);

    @Override
    public <T> Publisher<T> bulkhead(Publisher<T> publisher, Bulkhead bulkhead) {
        return Flowable.fromPublisher(publisher).compose(BulkheadOperator.of(bulkhead));
    }

    @Override
    public <T> Publisher<T> circuitBreaker(Publisher<T> publisher, CircuitBreaker circuitBreaker) {
        return Flowable.fromPublisher(publisher).compose(CircuitBreakerOperator.of(circuitBreaker));
    }

    @Override
    public <T> Publisher<T> timeLimiter(Publisher<T> publisher, TimeLimiter timeLimiter) {
        return Flowable.fromPublisher(publisher).compose(TimeLimiterTransformer.of(timeLimiter));
    }

    @Override
    public <T> Publisher<T> retry(Publisher<T> publisher, Retry retry) {
        return Flowable.fromPublisher(publisher).compose(RetryTransformer.of(retry));
    }

    @Override
    public <T> Publisher<T> rateLimiter(Publisher<T> publisher, RateLimiter rateLimiter) {
        return Flowable.fromPublisher(publisher).compose(RateLimiterOperator.of(rateLimiter));
    }

    @Override
    public <T> Publisher<T> fallbackPublisher(Publisher<T> publisher, MethodInvocationContext<Object, Object> context, Function<MethodInvocationContext<Object, Object>, Optional<? extends MethodExecutionHandle<?, Object>>> handler) {
        return Flowable.fromPublisher(publisher).onErrorResumeNext(throwable -> {
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
                    return Flowable.error(throwable);
                }
                if (fallbackResult == null) {
                    return Flowable.error(new FallbackException("Fallback handler [" + fallbackHandle + "] returned null value"));
                } else {
                    return ConversionService.SHARED.convert(fallbackResult, Publisher.class)
                        .orElseThrow(() -> new FallbackException("Unsupported Reactive type: " + fallbackResult));
                }
            }
            return Flowable.error(throwable);
        });
    }
}
