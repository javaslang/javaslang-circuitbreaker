/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Long.min;

/**
 * {@link AtomicRateLimiter} splits all nanoseconds from the start of epoch into cycles.
 * <p>Each cycle has duration of {@link io.github.resilience4j.ratelimiter.RefillRateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>By contract on start of each cycle {@link AtomicRateLimiter} should
 * set {@link State#activePermissions} to {@link io.github.resilience4j.ratelimiter.RefillRateLimiterConfig#limitForPeriod}. For the {@link
 * AtomicRateLimiter} callers it is really looks so, but under the hood there is some optimisations
 * that will skip this refresh if {@link AtomicRateLimiter} is not used actively.
 * <p>All {@link AtomicRateLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link AtomicRateLimiter.State}
 */
public class AtomicRateLimiter extends BaseAtomicLimiter<RateLimiterConfig, AtomicRateLimiter.State> implements RateLimiter {

    public AtomicRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    public AtomicRateLimiter(String name, RateLimiterConfig rateLimiterConfig,
                             Map<String, String> tags) {
        super(name, new AtomicReference<>(new State(
            rateLimiterConfig, 0, rateLimiterConfig.getLimitForPeriod(), 0
        )),tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainPermissions() {
        AtomicRateLimiter.State prev;
        AtomicRateLimiter.State next;
        do {
            prev = state().get();
            next = calculateNextState(prev.getActivePermissions(), 0, prev);
        } while (!compareAndSet(prev, next));
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new RateLimiterOnDrainedEvent(getName(), Math.min(prev.getActivePermissions(), 0)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected State calculateNextState(final int permits, final long timeoutInNanos,
                                     final State activeState) {
        RateLimiterConfig config = activeState.getConfig();
        long cyclePeriodInNanos = config.getLimitRefreshPeriod().toNanos();
        int permissionsPerCycle = config.getLimitForPeriod();

        long currentNanos = currentNanoTime();
        long currentCycle = currentNanos / cyclePeriodInNanos;

        long nextCycle = activeState.activeCycle;
        int nextPermissions = activeState.getActivePermissions();
        if (nextCycle != currentCycle) {
            long elapsedCycles = currentCycle - nextCycle;
            long accumulatedPermissions = elapsedCycles * permissionsPerCycle;
            nextCycle = currentCycle;
            nextPermissions = (int) min(nextPermissions + accumulatedPermissions,
                permissionsPerCycle);
        }
        long nextNanosToWait = nanosToWaitForPermission(
            permits, cyclePeriodInNanos, permissionsPerCycle, nextPermissions, currentNanos,
            currentCycle
        );
        State nextState = reservePermissions(config, permits, timeoutInNanos, nextCycle,
            nextPermissions, nextNanosToWait);
        return nextState;
    }


    /**
     * Calculates time to wait for the required permits of permissions to get accumulated
     *
     * @param permits              permits of required permissions
     * @param cyclePeriodInNanos   current configuration values
     * @param permissionsPerCycle  current configuration values
     * @param availablePermissions currently available permissions, can be negative if some
     *                             permissions have been reserved
     * @param currentNanos         current time in nanoseconds
     * @param currentCycle         current {@link AtomicRateLimiter} cycle    @return nanoseconds to
     *                             wait for the next permission
     */
    private long nanosToWaitForPermission(final int permits, final long cyclePeriodInNanos,
                                          final int permissionsPerCycle,
                                          final int availablePermissions, final long currentNanos, final long currentCycle) {
        if (availablePermissions >= permits) {
            return 0L;
        }
        long nextCycleTimeInNanos = (currentCycle + 1) * cyclePeriodInNanos;
        long nanosToNextCycle = nextCycleTimeInNanos - currentNanos;
        int permissionsAtTheStartOfNextCycle = availablePermissions + permissionsPerCycle;
        int fullCyclesToWait = divCeil(-(permissionsAtTheStartOfNextCycle - permits), permissionsPerCycle);
        return (fullCyclesToWait * cyclePeriodInNanos) + nanosToNextCycle;
    }

    /**
     * Divide two integers and round result to the bigger near mathematical integer.
     *
     * @param x - should be > 0
     * @param y - should be > 0
     */
    private static int divCeil(int x, int y) {
        return (x + y - 1) / y;
    }

    /**
     * Determines whether caller can acquire permission before timeout or not and then creates
     * corresponding {@link State}. Reserves permissions only if caller can successfully wait for
     * permission.
     *
     * @param config
     * @param permits        permits of permissions
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param cycle          cycle for new {@link State}
     * @param permissions    permissions for new {@link State}
     * @param nanosToWait    nanoseconds to wait for the next permission
     * @return new {@link State} with possibly reserved permissions and time to wait
     */
    private State reservePermissions(final RateLimiterConfig config, final int permits,
                                     final long timeoutInNanos,
                                     final long cycle, final int permissions, final long nanosToWait) {
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;
        int permissionsWithReservation = permissions;
        if (canAcquireInTime) {
            permissionsWithReservation -= permits;
        }
        return new State(config, cycle, permissionsWithReservation, nanosToWait);
    }

    @Override
    public String toString() {
        return "AtomicRateLimiter{" +
            "name='" + getName() + '\'' +
            ", rateLimiterConfig=" + state().get().getConfig()+
            '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicRateLimiterMetrics getDetailedMetrics() {
        return new AtomicRateLimiterMetrics();
    }

    /**
     * <p>{@link AtomicRateLimiter.State} represents immutable state of {@link AtomicRateLimiter}
     * where:
     * <ul>
     * <li>activeCycle - {@link AtomicRateLimiter} cycle number that was used
     * by the last {@link AtomicRateLimiter#acquirePermission()} call.</li>
     * <p>
     * <li>activePermissions - count of available permissions after
     * the last {@link AtomicRateLimiter#acquirePermission()} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link AtomicRateLimiter#acquirePermission()} call.</li>
     * </ul>
     */
    static class State extends BaseState<RateLimiterConfig> {

        private final long activeCycle;

        private State(RateLimiterConfig config,
                      final long activeCycle, final int activePermissions, final long nanosToWait) {
            super(config, activePermissions, nanosToWait);
            this.activeCycle = activeCycle;
        }

        @Override
        State withConfig(RateLimiterConfig config) {
            return new State(config, activeCycle, getActivePermissions(), getNanosToWait());
        }

    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public class AtomicRateLimiterMetrics implements Metrics {

        private AtomicRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads().get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
            State currentState = state().get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.getActivePermissions();
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state().get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.getNanosToWait();
        }

        /**
         * @return estimated current cycle
         */
        public long getCycle() {
            State currentState = state().get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.activeCycle;
        }

    }
}
