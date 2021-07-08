/*
 *
 *  Copyright 2016 Robert Winkler, Bohdan Storozhuk and Emmanouil Gkatziouras
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.vavr.collection.Map;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static io.github.resilience4j.ratelimiter.internal.ConfigBuilderHelper.withLimitForPeriod;
import static io.github.resilience4j.ratelimiter.internal.ConfigBuilderHelper.withTimeoutDuration;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * {@link BaseAtomicLimiter} purpose is to simplify the creation of rate limiters with atomic state changes.
 * <p>All {@link BaseAtomicLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link BaseState}
 * <p> Rate limiters that extend the {@link BaseAtomicLimiter} will have ,the permission recalculation logic, executed
 * by their user threads
 */
abstract class BaseAtomicLimiter<E extends RateLimiterConfig,T extends BaseState<E>> implements RateLimiter {

    protected long nanoTimeStart;

    private final String name;
    private final AtomicInteger waitingThreads;
    private final AtomicReference<T> state;
    private final Map<String, String> tags;
    protected final RateLimiterEventProcessor eventProcessor;

    BaseAtomicLimiter(String name, AtomicReference<T> state, Map<String, String> tags) {
        this.name = name;
        this.waitingThreads = new AtomicInteger(0);
        this.state = state;
        this.tags = tags;
        this.nanoTimeStart = nanoTime();
        this.eventProcessor =  new RateLimiterEventProcessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeTimeoutDuration(final Duration timeoutDuration) {
        try {
            E newConfig = withTimeoutDuration(timeoutDuration, state.get().getConfig());
            state.updateAndGet(currentState -> currentState.withConfig(newConfig));
        } catch (ConfigBuilderHelper.NonRegisteredConfig e) {
            throw new IllegalStateException("Please override method",e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeLimitForPeriod(final int limitForPeriod) {
        try {
            E newConfig = withLimitForPeriod(limitForPeriod, state.get().getConfig());
            state.updateAndGet(currentState -> currentState.withConfig(newConfig));
        } catch (ConfigBuilderHelper.NonRegisteredConfig e) {
            throw new IllegalStateException("Please override method",e);
        }
    }

    /**
     * Calculates time elapsed from the class loading.
     */
    protected long currentNanoTime() {
        return nanoTime() - nanoTimeStart;
    }

    long getNanoTimeStart() {
        return this.nanoTimeStart;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquirePermission(final int permits) {
        long timeoutInNanos = state().get().getTimeoutInNanos();
        T modifiedState = updateStateWithBackOff(permits, timeoutInNanos);
        boolean result = waitForPermissionIfNecessary(timeoutInNanos, modifiedState.getNanosToWait());
        publishRateLimiterAcquisitionEvent(result, permits);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long reservePermission(final int permits) {
        long timeoutInNanos = state().get().getTimeoutInNanos();
        T modifiedState = updateStateWithBackOff(permits, timeoutInNanos);

        boolean canAcquireImmediately = modifiedState.getNanosToWait() <= 0;
        if (canAcquireImmediately) {
            publishRateLimiterAcquisitionEvent(true, permits);
            return 0;
        }

        boolean canAcquireInTime = timeoutInNanos >= modifiedState.getNanosToWait();
        if (canAcquireInTime) {
            publishRateLimiterAcquisitionEvent(true, permits);
            return modifiedState.getNanosToWait();
        }

        publishRateLimiterAcquisitionEvent(false, permits);
        return -1;
    }

    @Override
    public void drainPermissions() {
        T prev;
        T next;
        do {
            prev = state.get();
            next = calculateNextState(prev.getActivePermissions(), 0, prev);
        } while (!compareAndSet(prev, next));
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new RateLimiterOnDrainedEvent(name, Math.min(prev.getActivePermissions(), 0)));
        }
    }

    /**
     * Atomically updates the current {@link T} with the results of applying the {@link
     * BaseAtomicLimiter#calculateNextState}, returning the updated {@link T}. It differs from
     * {@link AtomicReference#updateAndGet(UnaryOperator)} by constant back off. It means that after
     * one try to {@link AtomicReference#compareAndSet(Object, Object)} this method will wait for a
     * while before try one more time. This technique was originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param timeoutInNanos a side-effect-free function
     * @return the updated value
     */
    protected T updateStateWithBackOff(final int permits, final long timeoutInNanos) {
        T prev;
        T next;
        do {
            prev = state.get();
            next = calculateNextState(permits, timeoutInNanos, prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the
     * expected value. It differs from {@link AtomicReference#updateAndGet(UnaryOperator)} by
     * constant back off. It means that after one try to {@link AtomicReference#compareAndSet(Object,
     * Object)} this method will wait for a while before try one more time. This technique was
     * originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param current the expected value
     * @param next    the new value
     * @return {@code true} if successful. False return indicates that the actual value was not
     * equal to the expected value.
     */
    protected boolean compareAndSet(final T current, final T next) {
        if (state().compareAndSet(current, next)) {
            return true;
        }
        parkNanos(1); // back-off
        return false;
    }

    /**
     * A side-effect-free function that can calculate next {@link T} from current. It determines
     * time duration that you should wait for the given number of permits and reserves it for you,
     * if you'll be able to wait long enough.
     *
     * @param permits        number of permits
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param activeState    current state of {@link BaseAtomicLimiter}
     * @return next {@link T}
     */
    abstract protected T calculateNextState(final int permits, final long timeoutInNanos,
                                            final T activeState);

    /**
     * If nanosToWait is bigger than 0 it tries to park {@link Thread} for nanosToWait but not
     * longer then timeoutInNanos.
     *
     * @param timeoutInNanos max time that caller can wait
     * @param nanosToWait    nanoseconds caller need to wait
     * @return true if caller was able to wait for nanosToWait without {@link Thread#interrupt} and
     * not exceed timeout
     */
    protected boolean waitForPermissionIfNecessary(final long timeoutInNanos,
                                                   final long nanosToWait) {
        boolean canAcquireImmediately = nanosToWait <= 0;

        if (canAcquireImmediately) {
            return true;
        }

        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

        if (canAcquireInTime) {
            return waitForPermission(nanosToWait);
        }
        waitForPermission(timeoutInNanos);
        return false;
    }

    /**
     * Parks {@link Thread} for nanosToWait.
     * <p>If the current thread is {@linkplain Thread#interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException}, but its
     * interrupt status will be set.
     *
     * @param nanosToWait nanoseconds caller need to wait
     * @return true if caller was not {@link Thread#interrupted} while waiting
     */
    private boolean waitForPermission(final long nanosToWait) {
        waitingThreads().incrementAndGet();
        long deadline = currentNanoTime() + nanosToWait;
        boolean wasInterrupted = false;
        while (currentNanoTime() < deadline && !wasInterrupted) {
            long sleepBlockDuration = deadline - currentNanoTime();
            parkNanos(sleepBlockDuration);
            wasInterrupted = Thread.interrupted();
        }
        waitingThreads().decrementAndGet();
        if (wasInterrupted) {
            currentThread().interrupt();
        }
        return !wasInterrupted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E getRateLimiterConfig() {
        return state.get().getConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return getDetailedMetrics();
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * Get the enhanced Metrics with some implementation specific details.
     *
     * @return the detailed metrics
     */
    public abstract <T extends Metrics> T getDetailedMetrics();

    protected void publishRateLimiterAcquisitionEvent(boolean permissionAcquired, int permits) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        if (permissionAcquired) {
            eventProcessor.consumeEvent(new RateLimiterOnSuccessEvent(name, permits));
            return;
        }
        eventProcessor.consumeEvent(new RateLimiterOnFailureEvent(name, permits));
    }


    protected AtomicInteger waitingThreads() {
        return waitingThreads;
    }

    protected AtomicReference<T> state() {
        return state;
    }

}