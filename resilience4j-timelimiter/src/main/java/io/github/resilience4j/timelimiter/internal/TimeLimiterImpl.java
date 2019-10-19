package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class TimeLimiterImpl implements TimeLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimeLimiterImpl.class);

    private final String name;
    private final TimeLimiterConfig timeLimiterConfig;
    private final TimeLimiterEventProcessor eventProcessor;

    public TimeLimiterImpl(String name, TimeLimiterConfig timeLimiterConfig) {
        this.name = name;
        this.timeLimiterConfig = timeLimiterConfig;
        this.eventProcessor = new TimeLimiterEventProcessor();
    }

    @Override
    public <T, F extends Future<T>> Callable<T> decorateFutureSupplier(Supplier<F> futureSupplier) {
        return () -> {
            Future<T> future = futureSupplier.get();
            try {
                T result = future.get(getTimeLimiterConfig().getTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS);
                onSuccess();
                return result;
            } catch (TimeoutException e) {
                onError(e);
                if (getTimeLimiterConfig().shouldCancelRunningFuture()) {
                    future.cancel(true);
                }
                throw e;
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t == null) {
                    onError(e);
                    throw e;
                }
                onError(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw (Exception) t;
            }
        };
    }

    @Override
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(Supplier<F> supplier) {
        CompletableFuture<T> future = supplier.get().toCompletableFuture();
        ScheduledFuture<?> timeoutFuture =
                Timeout.of(future, getTimeLimiterConfig().getTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS);

        return () -> future.whenComplete((result, throwable) -> {
            // complete
            if (result != null) {
                if (!timeoutFuture.isDone()) {
                    timeoutFuture.cancel(false);
                }
                onSuccess();
            }

            // exceptionally
            if (throwable != null) {
                if (throwable instanceof ExecutionException) {
                    Throwable cause = throwable.getCause();
                    if (cause == null) {
                        onError(throwable);
                    } else {
                        onError(cause);
                    }
                } else {
                    onError(throwable);
                }
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TimeLimiterConfig getTimeLimiterConfig() {
        return timeLimiterConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onSuccess() {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnSuccessEvent(name));
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            onTimeout();
        } else {
            onFailure(throwable);
        }
    }

    private void onTimeout() {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnTimeoutEvent(name));
    }

    private void onFailure(Throwable throwable) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnErrorEvent(name, throwable));
    }

    private void publishEvent(TimeLimiterEvent event) {
        try {
            eventProcessor.consumeEvent(event);
            LOG.debug("Event {} published: {}", event.getEventType(), event);
        } catch (Exception e) {
            LOG.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }

    /**
     * Completes CompletableFuture with {@link TimeoutException}.
     */
    static final class Timeout {

        private static final ScheduledThreadPoolExecutor delayer;
        static {
            (delayer = new ScheduledThreadPoolExecutor(
                    1, new DaemonThreadFactory())).setRemoveOnCancelPolicy(true);
        }

        private Timeout() { }

        static ScheduledFuture<?> of(CompletableFuture<?> future, long delay, TimeUnit unit) {
            return delayer.schedule(() -> {
                if (future != null && !future.isDone()) {
                    future.completeExceptionally(new TimeoutException());
                }
            }, delay, unit);
         }
    }

    static final class DaemonThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("TimeLimiterDelayScheduler");
            return t;
        }
    }

}