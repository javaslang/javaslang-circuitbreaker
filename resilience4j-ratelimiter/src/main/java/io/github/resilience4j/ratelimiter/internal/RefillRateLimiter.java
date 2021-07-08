/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Integer.min;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * {@link RefillRateLimiter} has a max capacity of permits refilled periodically.
 * <p>Each period has duration of {@link RefillRateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>By contract the initial {@link RefillRateLimiter.State#activePermissions} are set
 * to be the same with{@link RefillRateLimiterConfig#permitCapacity}
 * <p>A ratio of permit per nanoseconds is calculated using
 * {@link RefillRateLimiterConfig#limitRefreshPeriod} and {@link RefillRateLimiterConfig#limitForPeriod}.
 * On a permit request the permits that should have been replenished are calculated based on the nanos passed and the ratio of nanos per permit.
 * For the {@link RefillRateLimiter} callers it is looks like a token bucket supporting bursts and a gradual refill,
 * under the hood there is some optimisations that will skip this refresh if {@link RefillRateLimiter} is not used actively.
 * <p>All {@link RefillRateLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link RefillRateLimiter.State}
 */
public class RefillRateLimiter implements RateLimiter {

    private final long nanoTimeStart;
    private final String name;
    private final AtomicInteger waitingThreads;
    private final AtomicReference<State> state;
    private final Map<String, String> tags;
    private final RateLimiterEventProcessor eventProcessor;

    public RefillRateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    public RefillRateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig,
                             Map<String, String> tags) {
        this(name, rateLimiterConfig, tags, nanoTime());
    }

    public RefillRateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig,
                             Map<String, String> tags, long nanoTime) {
        this.name = name;
        this.tags = tags;
        this.nanoTimeStart = nanoTime();

        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new RefillRateLimiter.State(
            rateLimiterConfig, rateLimiterConfig.getInitialPermits(), 0, nanoTime
        ));
        eventProcessor = new RateLimiterEventProcessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeTimeoutDuration(final Duration timeoutDuration) {
        RefillRateLimiterConfig newConfig = RefillRateLimiterConfig.from(state.get().config)
            .timeoutDuration(timeoutDuration)
            .build();
        state.updateAndGet(currentState -> new RefillRateLimiter.State(
            newConfig, currentState.activePermissions, currentState.nanosToWait,
            currentState.timeIndex
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeLimitForPeriod(final int limitForPeriod) {
        RefillRateLimiterConfig newConfig = RefillRateLimiterConfig.from(state.get().config)
            .limitForPeriod(limitForPeriod)
            .build();
        state.updateAndGet(currentState -> new RefillRateLimiter.State(
            newConfig, currentState.activePermissions, currentState.nanosToWait,
            currentState.timeIndex
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquirePermission(final int permits) {
        long timeoutInNanos = state.get().getTimeoutInNanos();
        State modifiedState = updateStateWithBackOff(permits, timeoutInNanos);
        boolean result = waitForPermissionIfNecessary(timeoutInNanos, modifiedState.getNanosToWait());
        publishRateLimiterAcquisitionEvent(result, permits);
        return result;
    }

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

    /**
     * Atomically updates the current {@link State} with the results of applying the {@link
     * RefillRateLimiter#calculateNextState}, returning the updated {@link State}. It differs from
     * {@link AtomicReference#updateAndGet(UnaryOperator)} by constant back off. It means that after
     * one try to {@link AtomicReference#compareAndSet(Object, Object)} this method will wait for a
     * while before try one more time. This technique was originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param timeoutInNanos a side-effect-free function
     * @return the updated value
     */
    protected State updateStateWithBackOff(final int permits, final long timeoutInNanos) {
        State prev;
        State next;
        do {
            prev = state.get();
            next = calculateNextState(permits, timeoutInNanos, prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

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
        waitingThreads.incrementAndGet();
        long deadline = currentNanoTime() + nanosToWait;
        boolean wasInterrupted = false;
        while (currentNanoTime() < deadline && !wasInterrupted) {
            long sleepBlockDuration = deadline - currentNanoTime();
            parkNanos(sleepBlockDuration);
            wasInterrupted = Thread.interrupted();
        }
        waitingThreads.decrementAndGet();
        if (wasInterrupted) {
            currentThread().interrupt();
        }
        return !wasInterrupted;
    }

    /**
     * Calculates time elapsed from the class loading.
     */
    protected long currentNanoTime() {
        return nanoTime() - nanoTimeStart;
    }

    /**
     * {@inheritDoc}
     */
    protected State calculateNextState(final int permits, final long timeoutInNanos,
                                       final State activeState) {
        int permissionsNeededExtra = permissionsMissing(permits, activeState.activePermissions);

        long currentNanoTime = refillLimiterNanoTime();

        if(permissionsNeededExtra<=0) {
            return new State(activeState.config, -permissionsNeededExtra, 0, activeState.timeIndex);
        } else {
            assertLessPermitsThanCapacity(permits, activeState);
            return lazyPermissionCalculation(permits ,permissionsNeededExtra, activeState, currentNanoTime, timeoutInNanos);
        }
    }




    private void assertLessPermitsThanCapacity(int permits, State activeState) {
        if(permits > activeState.config.getPermitCapacity()) {
            throw RequestNotPermitted.createRequestNotPermitted(this);
        }
    }

    private State lazyPermissionCalculation(int permits,int permissionsNeededExtra, final State activeState, long currentNanoTime, long timeoutInNanos) {
        RefillRateLimiterConfig config = activeState.config;

        long nanosSinceLastUpdate = currentNanoTime - activeState.timeIndex;

        if(nanosSinceLastUpdate >= config.getNanosPerFullCapacity()) {
            /**
             * We reached our max capacity. We remove the permits from the max capacity
             * Regardless of the permits leased previously our permits could not exceeded the max
             * so we clean up
             */
            int permitsLeft = config.getPermitCapacity() - permits;
            return new State(activeState.config, permitsLeft, 0, currentNanoTime);
        } else {
            /**
             * We have not reached the max capacity. Thus we need to calculate the extra permissions needed.
             */
            long nanosForPermissions = nanosNeededForExtraPermissions(permissionsNeededExtra, config);
            long nanosToCurrentIndex = activeState.timeIndex + nanosForPermissions;

            long nanosToWait = nanosForPermissions - nanosSinceLastUpdate;

            if(nanosToWait>0) {
                boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

                if(canAcquireInTime) {
                    return new State(config, 0, nanosToWait, nanosToCurrentIndex);
                } else {
                    return new State(activeState.config, activeState.activePermissions, nanosToWait, activeState.timeIndex);
                }
            } else {
                return new State(config, 0, 0, nanosToCurrentIndex);
            }
        }
    }

    private long nanosNeededForExtraPermissions(int neededPermissions, RefillRateLimiterConfig config) {
        return neededPermissions * config.getNanosPerPermit();
    }

    private int permissionsMissing(int permits, int currentPermits) {
        return permits - currentPermits;
    }

    /**
     * Needed for testing. Inlining will take care of it.
     * @return
     */
    private long refillLimiterNanoTime() {
        return nanoTime();
    }

    /**
     * Used only for metrics. Not good for rate limiter use.
     * @param state
     * @return
     */
    private int availablePermissions(State state) {
        long nanosSinceLastUpdate = refillLimiterNanoTime() - state.timeIndex;
        int permitCapacity = state.config.getPermitCapacity();
        long accumulatedPermissions = nanosSinceLastUpdate / state.config.getNanosPerPermit();
        int totalPermissions =  state.getActivePermissions() + (int) accumulatedPermissions;
        return min(permitCapacity, totalPermissions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainPermissions() {
        RefillRateLimiter.State prev;
        RefillRateLimiter.State next;
        do {
            long nanoTime = refillLimiterNanoTime();
            prev = state.get();
            next = prev.withTimeIndex(nanoTime).withTimeIndex(nanoTime).withPermissions(0);
        } while (!compareAndSet(prev, next));
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new RateLimiterOnDrainedEvent(getName(), Math.min(prev.getActivePermissions(), 0)));
        }
    }

    @Override
    public String toString() {
        return "RefillRateLimiter{" +
            "name='" + getName()+ '\'' +
            ", rateLimiterConfig=" + state.get().config+
            '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long reservePermission(final int permits) {
        long timeoutInNanos = state.get().getTimeoutInNanos();
        State modifiedState = updateStateWithBackOff(permits, timeoutInNanos);

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
    public RateLimiterConfig getRateLimiterConfig() {
        return state.get().config;
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
        return new RefillRateLimiterMetrics();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    public RefillRateLimiterMetrics getDetailedMetrics() {
        return new RefillRateLimiterMetrics();
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
    protected boolean compareAndSet(final State current, final State next) {
        if (state.compareAndSet(current, next)) {
            return true;
        }
        parkNanos(1); // back-off
        return false;
    }

    /**
     * <p>{@link RefillRateLimiter.State} represents immutable state of {@link RefillRateLimiter}
     * where:
     * <ul>
     * <li>activePermissions - count of available permissions after
     * the last {@link RefillRateLimiter#acquirePermission()} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link RefillRateLimiter#acquirePermission()} call.</li>
     * <p>
     * <li>updatedAt - the last time the state was updated.</li>
     * </ul>
     */
    private static class State {

        private final RefillRateLimiterConfig config;
        private final int activePermissions;
        private final long nanosToWait;
        private final long timeoutInNanos;

        private final long timeIndex;

        private State(RefillRateLimiterConfig config, int activePermissions, long nanosToWait, long timeIndex) {
            this.config = config;
            this.activePermissions = activePermissions;
            this.timeIndex = timeIndex;

            this.timeoutInNanos = config.getTimeoutDuration().toNanos();
            this.nanosToWait = nanosToWait;
        }

        private State withConfig(RefillRateLimiterConfig config) {
            return new State(config, activePermissions, getNanosToWait(), timeIndex);
        }

        private State withTimeIndex(long timeIndex) {
            return new State(config, activePermissions, getNanosToWait(), timeIndex);
        }

        private State withPermissions(int permissions) {
            return new State(config, permissions, getNanosToWait(), timeIndex);
        }

        public int getActivePermissions() {
            return activePermissions;
        }

        public long getNanosToWait() {
            return nanosToWait;
        }

        public long getTimeoutInNanos() {
            return timeoutInNanos;
        }
    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public class RefillRateLimiterMetrics implements Metrics {

        private RefillRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
            State currentState = state.get();
            return availablePermissions(currentState);
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state.get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.getNanosToWait();
        }

    }
}
