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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.internal.RefillRateLimiter;

import java.io.Serializable;
import java.time.Duration;

/**
 * {@link RefillRateLimiter} is a permission rate based Rate Limiter.
 * Instead of resetting permits based on a permission period the permission release is based on a rate.
 * Therefore {@link RefillRateLimiterConfig#nanosPerPermit} is used which is a product of the division
 * of {@link RateLimiterConfig#limitRefreshPeriod} to {@link RateLimiterConfig#limitForPeriod}.
 */
public class RefillRateLimiterConfig extends RateLimiterConfig implements Serializable{

    private static final long serialVersionUID = 3095810082683985263L;

    private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
    private static final String ZERO_NANOS_PER_PERMISSION_STATE = "Current settings lead to zero nanos per permission, adjust period and limit";
    private static final String ZERO_NANOS_PER_PERMISSION_ARGUMENT = "At least 1 nanos per permission should be provided";

    private final int permitCapacity;
    private final long nanosPerFullCapacity;
    private final int initialPermits;
    private final long nanosPerPermit;

    private RefillRateLimiterConfig(Duration timeoutDuration, int permitCapacity, long nanosPerPermit,
                                    long nanosPerFullCapacity,
                                    int initialPermits, boolean writableStackTraceEnabled) {
        super(timeoutDuration, Duration.ofNanos(nanosPerPermit * permitCapacity), permitCapacity, writableStackTraceEnabled);
        noZeroNanosOnPermissionArgument(nanosPerPermit);
        this.permitCapacity = permitCapacity;
        this.nanosPerFullCapacity = nanosPerFullCapacity;
        this.initialPermits = initialPermits;
        this.nanosPerPermit = nanosPerPermit;
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig.
     *
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder custom() {
        return new RefillRateLimiterConfig.Builder();
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link RefillRateLimiterConfig} prototype.
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder from(RefillRateLimiterConfig prototype) {
        return new RefillRateLimiterConfig.Builder(prototype);
    }

    /**
     * Creates a default RefillRateLimiter configuration.
     *
     * @return a default RefillRateLimiter configuration.
     */
    public static RefillRateLimiterConfig ofDefaults() {
        return new RefillRateLimiterConfig.Builder().build();
    }

    /**
     * Get the permit capacity the RefillRateLimiter should have.
     *
     * @return
     */
    public int getPermitCapacity() {
        return permitCapacity;
    }

    /**
     * Get the permits the RefillRateLimiter is configured to start with.
     *
     * @return
     */
    public int getInitialPermits() {
        return initialPermits;
    }

    /**
     * Get the nanos needed to replenish one permit.
     *
     * @return
     */
    public long getNanosPerPermit() {
        return nanosPerPermit;
    }

    /**
     * Get the nanos needed to reach full capacity
     * @return
     */
    public long getNanosPerFullCapacity() {
        return nanosPerFullCapacity;
    }

    @Override
    public String toString() {
        return "RefillRateLimiterConfig{" +
            "timeoutDuration=" + getTimeoutDuration() +
            ", permitCapacity=" + permitCapacity +
            ", nanosPerPermission=" + nanosPerPermit +
            ", writableStackTraceEnabled=" + isWritableStackTraceEnabled() +
            '}';
    }

    public static class Builder extends RateLimiterConfig.Builder {

        private Duration timeoutDuration = Duration.ofSeconds(5);
        private Duration limitRefreshPeriod = Duration.ofNanos(500);
        private int limitForPeriod = 50;
        private int permitCapacity = 0;
        private int initialPermits = 0;
        private boolean initialPermitsSet;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

        public Builder() {
        }

        public Builder(RefillRateLimiterConfig prototype) {
            this.timeoutDuration = prototype.getTimeoutDuration();
            this.limitRefreshPeriod = Duration.ofNanos(prototype.nanosPerPermit);
            this.limitForPeriod = 1;
            this.permitCapacity = prototype.permitCapacity;
            this.writableStackTraceEnabled = prototype.isWritableStackTraceEnabled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RefillRateLimiterConfig build() {
            if (permitCapacity < limitForPeriod) {
                permitCapacity = limitForPeriod;
            }

            if (!initialPermitsSet) {
                initialPermits = limitForPeriod;
            }

            final long nanosPerPermission = calculateNanosPerPermit(limitRefreshPeriod, limitForPeriod);
            noZeroNanosOnPermissionState(nanosPerPermission);

            final long nanosPerFullCapacity = calculateNanosPerFullCapacity(nanosPerPermission, permitCapacity);

            return new RefillRateLimiterConfig(timeoutDuration, permitCapacity, nanosPerPermission, nanosPerFullCapacity,
                initialPermits, writableStackTraceEnabled);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RefillRateLimiterConfig.Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RefillRateLimiterConfig.Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period needed for the permit number specified. After each period
         * permissions up to {@link RefillRateLimiterConfig.Builder#limitForPeriod} should be released.
         * Default value is 500 nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RefillRateLimiterConfig.Builder
         */
        @Override
        public RefillRateLimiterConfig.Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        /**
         * Configures the permits to release through a refresh period. Count of permissions released
         * during one rate limiter period specified by {@link RefillRateLimiterConfig.Builder#limitRefreshPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RefillRateLimiterConfig.Builder
         */
        @Override
        public RefillRateLimiterConfig.Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }

        /**
         * Configures the permissions capacity. Count of max permissions available
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param permitCapacity the capacity of permissions
         * @return the RateLimiterConfig.Builder
         */
        public RefillRateLimiterConfig.Builder permitCapacity(final int permitCapacity) {
            this.permitCapacity = permitCapacity;
            return this;
        }

        /**
         * Configures the initial permit available.
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param initialPermits the initial permits
         * @return the RateLimiterConfig.Builder
         */
        public RefillRateLimiterConfig.Builder initialPermits(final int initialPermits) {
            this.initialPermits = initialPermits;
            this.initialPermitsSet = true;
            return this;
        }

        /**
         * Calculate the nanos needed for one permit
         *
         * @param limitRefreshPeriod
         * @param limitForPeriod
         * @return
         */
        private long calculateNanosPerPermit(Duration limitRefreshPeriod, int limitForPeriod) {
            long permissionsPeriodInNanos = limitRefreshPeriod.toNanos();
            return permissionsPeriodInNanos / limitForPeriod;
        }

        private long calculateNanosPerFullCapacity(long nanosPerPermission, long permitCapacity) {
            return nanosPerPermission * permitCapacity;
        }
    }

    private static void noZeroNanosOnPermissionArgument(long nanosPerPermit ) {
        if(nanosPerPermit<=0) {
            throw new IllegalArgumentException(ZERO_NANOS_PER_PERMISSION_ARGUMENT);
        }
    }

    private static void noZeroNanosOnPermissionState(long nanosPerPermit ) {
        if(nanosPerPermit<=0) {
            throw new IllegalStateException(ZERO_NANOS_PER_PERMISSION_STATE);
        }
    }

}
