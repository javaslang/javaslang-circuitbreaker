/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * A {@link CircuitBreakerConfig} configures a {@link CircuitBreaker}
 */
public class CircuitBreakerConfig implements Serializable {

    private static final long serialVersionUID = -5429814941777001669L;

    public static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50; // Percentage
    public static final int DEFAULT_SLOW_CALL_RATE_THRESHOLD = 100; // Percentage
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60; // Seconds
    public static final int DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100;
    public static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
    public static final int DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 60; // Seconds
    public static final int DEFAULT_WAIT_DURATION_IN_HALF_OPEN_STATE = 0; // Seconds. It is an optional parameter
    public static final SlidingWindowType DEFAULT_SLIDING_WINDOW_TYPE = SlidingWindowType.COUNT_BASED;
    public static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
    private static final Predicate<Throwable> DEFAULT_RECORD_EXCEPTION_PREDICATE = throwable -> true;
    private static final Predicate<Throwable> DEFAULT_IGNORE_EXCEPTION_PREDICATE = throwable -> false;
    private static final Function<Clock, Long> DEFAULT_TIMESTAMP_FUNCTION = clock -> System.nanoTime();
    private static final TimeUnit DEFAULT_TIMESTAMP_UNIT = TimeUnit.NANOSECONDS;
    private static final Predicate<Object> DEFAULT_RECORD_RESULT_PREDICATE = (Object object) -> false;
    // The default exception predicate counts all exceptions as failures.
    private Predicate<Throwable> recordExceptionPredicate = DEFAULT_RECORD_EXCEPTION_PREDICATE;
    // The default exception predicate ignores no exceptions.
    private Predicate<Throwable> ignoreExceptionPredicate = DEFAULT_IGNORE_EXCEPTION_PREDICATE;
    private Function<Clock, Long> currentTimestampFunction = DEFAULT_TIMESTAMP_FUNCTION;
    private TimeUnit timestampUnit = DEFAULT_TIMESTAMP_UNIT;

    private transient Predicate<Object> recordResultPredicate = DEFAULT_RECORD_RESULT_PREDICATE;

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] recordExceptions = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

    @Nullable
    private String name;
    private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
    private int permittedNumberOfCallsInHalfOpenState = DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
    private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
    private SlidingWindowType slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
    private int minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
    private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
    private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;
    private IntervalFunction waitIntervalFunctionInOpenState = IntervalFunction
        .of(Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE));
    private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;
    private Duration slowCallDurationThreshold = Duration
        .ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD);
    private Duration maxWaitDurationInHalfOpenState = Duration
        .ofSeconds(DEFAULT_WAIT_DURATION_IN_HALF_OPEN_STATE);

    protected CircuitBreakerConfig(Builder builder) {
        this.name = builder.name;
        this.waitIntervalFunctionInOpenState = builder.waitIntervalFunctionInOpenState;
        this.slidingWindowType = builder.slidingWindowType;
        this.slowCallDurationThreshold = builder.slowCallDurationThreshold;
        this.maxWaitDurationInHalfOpenState = builder.maxWaitDurationInHalfOpenState;
        this.slowCallRateThreshold = builder.slowCallRateThreshold;
        this.failureRateThreshold = builder.failureRateThreshold;
        this.slidingWindowSize = builder.slidingWindowSize;
        this.minimumNumberOfCalls = builder.minimumNumberOfCalls;
        this.permittedNumberOfCallsInHalfOpenState = builder.permittedNumberOfCallsInHalfOpenState;
        this.recordExceptions = builder.recordExceptions;
        this.ignoreExceptions = builder.ignoreExceptions;
        this.automaticTransitionFromOpenToHalfOpenEnabled = builder.automaticTransitionFromOpenToHalfOpenEnabled;
        this.writableStackTraceEnabled = builder.writableStackTraceEnabled;
        if(builder.recordExceptionPredicate != null) {
            this.recordExceptionPredicate = builder.recordExceptionPredicate;
        }
        if(builder.ignoreExceptionPredicate != null) {
            this.ignoreExceptionPredicate = builder.ignoreExceptionPredicate;
        }
        this.currentTimestampFunction = builder.currentTimestampFunction;
        this.timestampUnit = builder.timestampUnit;
        this.recordResultPredicate = builder.recordResultPredicate;
    }

    /**
     * Returns a builder to create a custom CircuitBreakerConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom CircuitBreakerConfig based on another
     * CircuitBreakerConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder from(CircuitBreakerConfig baseConfig) {
        return new Builder(baseConfig);
    }

    /**
     * Creates a default CircuitBreaker configuration.
     *
     * @return a default CircuitBreaker configuration.
     */
    public static CircuitBreakerConfig ofDefaults() {
        return new Builder().build();
    }

    public String getName() {
        return name;
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    /**
     * @deprecated since 1.2.0 You should use {@link #getWaitIntervalFunctionInOpenState()} instead.
     */
    @Deprecated
    public Duration getWaitDurationInOpenState() {
        return Duration.ofMillis(waitIntervalFunctionInOpenState.apply(1));
    }

    /**
     * Returns an interval function which controls how long the CircuitBreaker should stay open,
     * before it switches to half open.
     *
     * @return the CircuitBreakerConfig.Builder
     */
    public IntervalFunction getWaitIntervalFunctionInOpenState() {
        return waitIntervalFunctionInOpenState;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public Predicate<Throwable> getRecordExceptionPredicate() {
        return recordExceptionPredicate;
    }

    public Predicate<Object> getRecordResultPredicate() {
        return recordResultPredicate;
    }

    public Predicate<Throwable> getIgnoreExceptionPredicate() {
        return ignoreExceptionPredicate;
    }

    public Function<Clock, Long> getCurrentTimestampFunction() { return currentTimestampFunction; }

    public TimeUnit getTimestampUnit() { return timestampUnit; }

    public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() {
        return automaticTransitionFromOpenToHalfOpenEnabled;
    }

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    public int getPermittedNumberOfCallsInHalfOpenState() {
        return permittedNumberOfCallsInHalfOpenState;
    }

    public SlidingWindowType getSlidingWindowType() {
        return slidingWindowType;
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public Duration getSlowCallDurationThreshold() {
        return slowCallDurationThreshold;
    }

    public Duration getMaxWaitDurationInHalfOpenState() {
        return maxWaitDurationInHalfOpenState;
    }

    public enum SlidingWindowType {
        TIME_BASED, COUNT_BASED
    }

    @Override
    public String toString() {
        StringBuilder circuitBreakerConfig = new StringBuilder("CircuitBreakerConfig {");
        circuitBreakerConfig.append("name=");
        circuitBreakerConfig.append(name);
        circuitBreakerConfig.append("recordExceptionPredicate=");
        circuitBreakerConfig.append(recordExceptionPredicate);
        circuitBreakerConfig.append(", ignoreExceptionPredicate=");
        circuitBreakerConfig.append(ignoreExceptionPredicate);
        circuitBreakerConfig.append(", recordExceptions=");
        circuitBreakerConfig.append(Arrays.toString(recordExceptions));
        circuitBreakerConfig.append(", ignoreExceptions=");
        circuitBreakerConfig.append(Arrays.toString(ignoreExceptions));
        circuitBreakerConfig.append(", failureRateThreshold=");
        circuitBreakerConfig.append(failureRateThreshold);
        circuitBreakerConfig.append(", permittedNumberOfCallsInHalfOpenState=");
        circuitBreakerConfig.append(permittedNumberOfCallsInHalfOpenState);
        circuitBreakerConfig.append(", slidingWindowSize=");
        circuitBreakerConfig.append(slidingWindowSize);
        circuitBreakerConfig.append(", slidingWindowType=");
        circuitBreakerConfig.append(slidingWindowType);
        circuitBreakerConfig.append(", minimumNumberOfCalls=");
        circuitBreakerConfig.append(minimumNumberOfCalls);
        circuitBreakerConfig.append(", writableStackTraceEnabled=");
        circuitBreakerConfig.append(writableStackTraceEnabled);
        circuitBreakerConfig.append(", automaticTransitionFromOpenToHalfOpenEnabled=");
        circuitBreakerConfig.append(automaticTransitionFromOpenToHalfOpenEnabled);
        circuitBreakerConfig.append(", waitIntervalFunctionInOpenState=");
        circuitBreakerConfig.append(waitIntervalFunctionInOpenState);
        circuitBreakerConfig.append(", slowCallRateThreshold=");
        circuitBreakerConfig.append(slowCallRateThreshold);
        circuitBreakerConfig.append(", slowCallDurationThreshold=");
        circuitBreakerConfig.append(slowCallDurationThreshold);
        circuitBreakerConfig.append("}");
        return circuitBreakerConfig.toString();
    }

	public static class Builder {

        @Nullable
        private String name;
        @Nullable
        private Predicate<Throwable> recordExceptionPredicate;
        @Nullable
        private Predicate<Throwable> ignoreExceptionPredicate;
        private Function<Clock, Long> currentTimestampFunction = DEFAULT_TIMESTAMP_FUNCTION;
        private TimeUnit timestampUnit = DEFAULT_TIMESTAMP_UNIT;

        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] recordExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

        private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
        private int minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
        private int permittedNumberOfCallsInHalfOpenState = DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
        private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
        private Predicate<Object> recordResultPredicate = DEFAULT_RECORD_RESULT_PREDICATE;

        private IntervalFunction waitIntervalFunctionInOpenState = IntervalFunction
            .of(Duration.ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD));

        private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;
        private SlidingWindowType slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
        private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;
        private Duration slowCallDurationThreshold = Duration
            .ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD);
        private Duration maxWaitDurationInHalfOpenState = Duration
            .ofSeconds(DEFAULT_WAIT_DURATION_IN_HALF_OPEN_STATE);
        private byte createWaitIntervalFunctionCounter = 0;


        public Builder(CircuitBreakerConfig baseConfig) {
            this.name = baseConfig.name;
            this.waitIntervalFunctionInOpenState = baseConfig.waitIntervalFunctionInOpenState;
            this.permittedNumberOfCallsInHalfOpenState = baseConfig.permittedNumberOfCallsInHalfOpenState;
            this.slidingWindowSize = baseConfig.slidingWindowSize;
            this.slidingWindowType = baseConfig.slidingWindowType;
            this.minimumNumberOfCalls = baseConfig.minimumNumberOfCalls;
            this.failureRateThreshold = baseConfig.failureRateThreshold;
            this.ignoreExceptions = baseConfig.ignoreExceptions;
            this.recordExceptions = baseConfig.recordExceptions;
            this.recordExceptionPredicate = baseConfig.recordExceptionPredicate;
            this.ignoreExceptionPredicate = baseConfig.ignoreExceptionPredicate;
            this.currentTimestampFunction = baseConfig.currentTimestampFunction;
            this.timestampUnit = baseConfig.timestampUnit;
            this.automaticTransitionFromOpenToHalfOpenEnabled = baseConfig.automaticTransitionFromOpenToHalfOpenEnabled;
            this.slowCallRateThreshold = baseConfig.slowCallRateThreshold;
            this.slowCallDurationThreshold = baseConfig.slowCallDurationThreshold;
            this.maxWaitDurationInHalfOpenState = baseConfig.maxWaitDurationInHalfOpenState;
            this.writableStackTraceEnabled = baseConfig.writableStackTraceEnabled;
            this.recordResultPredicate = baseConfig.recordResultPredicate;
        }

        public Builder() {

        }


        /**
         * Configures a name for the current configuration.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Configures the failure rate threshold in percentage. If the failure rate is equal to or
         * greater than the threshold, the CircuitBreaker transitions to open and starts
         * short-circuiting calls.
         * <p>
         * The threshold must be greater than 0 and not greater than 100. Default value is 50
         * percentage.
         *
         * @param failureRateThreshold the failure rate threshold in percentage
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code failureRateThreshold <= 0 ||
         *                                  failureRateThreshold > 100}
         */
        public Builder failureRateThreshold(float failureRateThreshold) {
            if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "failureRateThreshold must be between 1 and 100");
            }
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        /**
         * Configures a threshold in percentage. The CircuitBreaker considers a call as slow when
         * the call duration is greater than {@link #slowCallDurationThreshold(Duration)}. When the
         * percentage of slow calls is equal to or greater than the threshold, the CircuitBreaker
         * transitions to open and starts short-circuiting calls.
         *
         * <p>
         * The threshold must be greater than 0 and not greater than 100. Default value is 100
         * percentage which means that all recorded calls must be slower than {@link
         * #slowCallDurationThreshold(Duration)}.
         *
         * @param slowCallRateThreshold the slow calls threshold in percentage
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code slowCallRateThreshold <= 0 ||
         *                                  slowCallRateThreshold > 100}
         */
        public Builder slowCallRateThreshold(float slowCallRateThreshold) {
            if (slowCallRateThreshold <= 0 || slowCallRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "slowCallRateThreshold must be between 1 and 100");
            }
            this.slowCallRateThreshold = slowCallRateThreshold;
            return this;
        }

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the circuit breaker
         * is open as the cause of the exceptions is already known (the circuit breaker is
         * short-circuiting calls).
         *
         * @param writableStackTraceEnabled the flag to enable writable stack traces.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * Configures an interval function with a fixed wait duration which controls how long the
         * CircuitBreaker should stay open, before it switches to half open. Default value is 60
         * seconds.
         * <p>
         * Do not use with {@link #waitIntervalFunctionInOpenState(IntervalFunction)}!
         * Please, when using, make sure not to override the value set earlier from the
         * {@link #waitIntervalFunctionInOpenState(IntervalFunction)}
         *
         * @param waitDurationInOpenState the wait duration which specifies how long the
         *                                CircuitBreaker should stay open
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code waitDurationInOpenState.toMillis() < 1}
         */
        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            long waitDurationInMillis = waitDurationInOpenState.toMillis();
            if (waitDurationInMillis < 1) {
                throw new IllegalArgumentException(
                    "waitDurationInOpenState must be at least 1[ms]");
            }
            this.waitIntervalFunctionInOpenState = IntervalFunction.of(waitDurationInMillis);
            createWaitIntervalFunctionCounter++;
            return this;
        }

        /**
         * Configures an interval function which controls how long the CircuitBreaker should stay
         * open, before it switches to half open. The default interval function returns a fixed wait
         * duration of 60 seconds.
         * <p>
         * A custom interval function is useful if you need an exponential backoff algorithm.
         * <p>
         * Do not use with {@link #waitDurationInOpenState(Duration)}!
         * Please, when using, make sure not to override the value set earlier from the
         * {@link #waitDurationInOpenState(Duration)}
         *
         * @param waitIntervalFunctionInOpenState Interval function that returns wait time as a
         *                                        function of attempts
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder waitIntervalFunctionInOpenState(
            IntervalFunction waitIntervalFunctionInOpenState) {
            this.waitIntervalFunctionInOpenState = waitIntervalFunctionInOpenState;
            createWaitIntervalFunctionCounter++;
            return this;
        }

        /**
         * Configures the duration threshold above which calls are considered as slow and increase
         * the slow calls percentage. Default value is 60 seconds.
         *
         * @param slowCallDurationThreshold the duration above which calls are considered as slow
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code slowCallDurationThreshold.toNanos() < 1}
         */
        public Builder slowCallDurationThreshold(Duration slowCallDurationThreshold) {
            if (slowCallDurationThreshold.toNanos() < 1) {
                throw new IllegalArgumentException(
                    "slowCallDurationThreshold must be at least 1[ns]");
            }
            this.slowCallDurationThreshold = slowCallDurationThreshold;
            return this;
        }

        /**
         * Configures CircuitBreaker with a fixed wait duration which controls how long the
         * CircuitBreaker should stay in Half Open state, before it switches to open. This is an
         * optional parameter.
         *
         * By default CircuitBreaker will stay in Half Open state until
         * {@code minimumNumberOfCalls} is completed with either success or failure.
         *
         * @param maxWaitDurationInHalfOpenState the wait duration which specifies how long the
         *                                CircuitBreaker should stay in Half Open
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code maxWaitDurationInHalfOpenState.toMillis() < 1}
         */
        public Builder maxWaitDurationInHalfOpenState(Duration maxWaitDurationInHalfOpenState) {
            if (maxWaitDurationInHalfOpenState.toMillis() < 1) {
                throw new IllegalArgumentException(
                    "maxWaitDurationInHalfOpenState must be at least 1[ms]");
            }
            this.maxWaitDurationInHalfOpenState = maxWaitDurationInHalfOpenState;
            return this;
        }

        /**
         * Configures the number of permitted calls when the CircuitBreaker is half open.
         * <p>
         * The size must be greater than 0. Default size is 10.
         *
         * @param permittedNumberOfCallsInHalfOpenState the permitted number of calls when the
         *                                              CircuitBreaker is half open
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code permittedNumberOfCallsInHalfOpenState < 1}
         */
        public Builder permittedNumberOfCallsInHalfOpenState(
            int permittedNumberOfCallsInHalfOpenState) {
            if (permittedNumberOfCallsInHalfOpenState < 1) {
                throw new IllegalArgumentException(
                    "permittedNumberOfCallsInHalfOpenState must be greater than 0");
            }
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
            return this;
        }

        /**
         * @deprecated Use {@link #permittedNumberOfCallsInHalfOpenState(int)} instead.
         */
        @Deprecated
        public Builder ringBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            if (ringBufferSizeInHalfOpenState < 1) {
                throw new IllegalArgumentException(
                    "ringBufferSizeInHalfOpenState must be greater than 0");
            }
            this.permittedNumberOfCallsInHalfOpenState = ringBufferSizeInHalfOpenState;
            return this;
        }

        /**
         * @deprecated Use {@link #slidingWindow(int, int, SlidingWindowType)} instead.
         */
        @Deprecated
        public Builder ringBufferSizeInClosedState(int ringBufferSizeInClosedState) {
            if (ringBufferSizeInClosedState < 1) {
                throw new IllegalArgumentException(
                    "ringBufferSizeInClosedState must be greater than 0");
            }
            return slidingWindow(ringBufferSizeInClosedState, ringBufferSizeInClosedState,
                SlidingWindowType.COUNT_BASED);
        }

        /**
         * Configures the sliding window which is used to record the outcome of calls when the
         * CircuitBreaker is closed. {@code slidingWindowSize} configures the size of the sliding
         * window. Sliding window can either be count-based or time-based, specified by {@code
         * slidingWindowType}. {@code minimumNumberOfCalls} configures the minimum number of calls
         * which are required (per sliding window period) before the CircuitBreaker can calculate
         * the error rate. For example, if {@code minimumNumberOfCalls} is 10, then at least 10
         * calls must be recorded, before the failure rate can be calculated. If only 9 calls have
         * been recorded, the CircuitBreaker will not transition to open, even if all 9 calls have
         * failed.
         * <p>
         * If {@code slidingWindowSize} is 100 and {@code slidingWindowType} is COUNT_BASED, the
         * last 100 calls are recorded and aggregated. If {@code slidingWindowSize} is 10 and {@code
         * slidingWindowType} is TIME_BASED, the calls of the last 10 seconds are recorded and
         * aggregated.
         * <p>
         * The {@code slidingWindowSize} must be greater than 0. The {@code minimumNumberOfCalls}
         * must be greater than 0. If the {@code slidingWindowType} is COUNT_BASED, the {@code
         * minimumNumberOfCalls} may not be greater than {@code slidingWindowSize}. If a greater
         * value is provided, {@code minimumNumberOfCalls} will be equal to {@code
         * slidingWindowSize}. If the {@code slidingWindowType} is TIME_BASED, the {@code
         * minimumNumberOfCalls} may be any amount.
         * <p>
         * Default slidingWindowSize is 100, minimumNumberOfCalls is 100 and slidingWindowType is
         * COUNT_BASED.
         *
         * @param slidingWindowSize    the size of the sliding window when the CircuitBreaker is
         *                             closed.
         * @param minimumNumberOfCalls the minimum number of calls that must be recorded before the
         *                             failure rate can be calculated.
         * @param slidingWindowType    the type of the sliding window. Either COUNT_BASED or
         *                             TIME_BASED.
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code slidingWindowSize < 1 || minimumNumberOfCalls
         *                                  < 1}
         */
        public Builder slidingWindow(int slidingWindowSize, int minimumNumberOfCalls,
            SlidingWindowType slidingWindowType) {
            if (slidingWindowSize < 1) {
                throw new IllegalArgumentException("slidingWindowSize must be greater than 0");
            }
            if (minimumNumberOfCalls < 1) {
                throw new IllegalArgumentException("minimumNumberOfCalls must be greater than 0");
            }
            if (slidingWindowType == SlidingWindowType.COUNT_BASED) {
                this.minimumNumberOfCalls = Math.min(minimumNumberOfCalls, slidingWindowSize);
            } else {
                this.minimumNumberOfCalls = minimumNumberOfCalls;
            }
            this.slidingWindowSize = slidingWindowSize;
            this.slidingWindowType = slidingWindowType;
            return this;
        }

        /**
         * Configures the size of the sliding window which is used to record the outcome of calls
         * when the CircuitBreaker is closed. {@code slidingWindowSize} configures the size of the
         * sliding window.
         * <p>
         * The {@code slidingWindowSize} must be greater than 0.
         * <p>
         * Default slidingWindowSize is 100.
         *
         * @param slidingWindowSize the size of the sliding window when the CircuitBreaker is
         *                          closed.
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code slidingWindowSize < 1}
         * @see #slidingWindow(int, int, SlidingWindowType)
         */
        public Builder slidingWindowSize(int slidingWindowSize) {
            if (slidingWindowSize < 1) {
                throw new IllegalArgumentException("slidingWindowSize must be greater than 0");
            }
            this.slidingWindowSize = slidingWindowSize;
            return this;
        }

        /**
         * Configures the minimum number of calls which are required (per sliding window period)
         * before the CircuitBreaker can calculate the error rate. For example, if {@code
         * minimumNumberOfCalls} is 10, then at least 10 calls must be recorded, before the failure
         * rate can be calculated. If only 9 calls have been recorded, the CircuitBreaker will not
         * transition to open, even if all 9 calls have failed.
         * <p>
         * Default minimumNumberOfCalls is 100
         *
         * @param minimumNumberOfCalls the minimum number of calls that must be recorded before the
         *                             failure rate can be calculated.
         * @return the CircuitBreakerConfig.Builder
         * @throws IllegalArgumentException if {@code minimumNumberOfCalls < 1}
         * @see #slidingWindow(int, int, SlidingWindowType)
         */
        public Builder minimumNumberOfCalls(int minimumNumberOfCalls) {
            if (minimumNumberOfCalls < 1) {
                throw new IllegalArgumentException("minimumNumberOfCalls must be greater than 0");
            }
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }

        /**
         * Configures the type of the sliding window which is used to record the outcome of calls
         * when the CircuitBreaker is closed. Sliding window can either be count-based or
         * time-based.
         * <p>
         * Default slidingWindowType is COUNT_BASED.
         *
         * @param slidingWindowType the type of the sliding window. Either COUNT_BASED or
         *                          TIME_BASED.
         * @return the CircuitBreakerConfig.Builder
         * @see #slidingWindow(int, int, SlidingWindowType)
         */
        public Builder slidingWindowType(SlidingWindowType slidingWindowType) {
            this.slidingWindowType = slidingWindowType;
            return this;
        }

        /**
         * @deprecated use {@link #recordException(Predicate)} instead.
         */
        @Deprecated
        public Builder recordFailure(Predicate<Throwable> predicate) {
            this.recordExceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be recorded as a failure
         * and thus increase the failure rate. The Predicate must return true if the exception
         * should count as a failure. The Predicate must return false, if the exception should count
         * as a success, unless the exception is explicitly ignored by {@link
         * #ignoreExceptions(Class[])} or {@link #ignoreException(Predicate)}.
         *
         * @param predicate the Predicate which evaluates if an exception should count as a failure
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder recordException(Predicate<Throwable> predicate) {
            this.recordExceptionPredicate = predicate;
            return this;
        }
        /**
         * Configures a function that returns current timestamp for CircuitBreaker.
         * Default implementation uses System.nanoTime() to compute current timestamp.
         * Configure currentTimestampFunction to provide different implementation to compute current timestamp.
         * <p>
         *
         * @param currentTimestampFunction function that computes current timestamp.
         * @param timeUnit TimeUnit of timestamp returned by the function.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder currentTimestampFunction(Function<Clock, Long> currentTimestampFunction, TimeUnit timeUnit) {
            this.timestampUnit = timeUnit;
            this.currentTimestampFunction = currentTimestampFunction;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if the result of the protected function call
         * should be recorded as a failure and thus increase the failure rate.
         * The Predicate must return true if the result should count as a failure.
         * The Predicate must return false, if the result should count
         * as a success.
         *
         * @param predicate the Predicate which evaluates if a result should count as a failure
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder recordResult(Predicate<Object> predicate) {
            this.recordResultPredicate = predicate;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be ignored and neither
         * count as a failure nor success. The Predicate must return true if the exception should be
         * ignored. The Predicate must return false, if the exception should count as a failure.
         *
         * @param predicate the Predicate which evaluates if an exception should count as a failure
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder ignoreException(Predicate<Throwable> predicate) {
            this.ignoreExceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures a list of error classes that are recorded as a failure and thus increase the
         * failure rate. Any exception matching or inheriting from one of the list should count as a
         * failure, unless ignored via {@link #ignoreExceptions(Class[])} or {@link
         * #ignoreException(Predicate)}.
         *
         * @param errorClasses the error classes that are recorded
         * @return the CircuitBreakerConfig.Builder
         * @see #ignoreExceptions(Class[]) ). Ignoring an exception has priority over recording an
         * exception.
         * <p>
         * Example: recordExceptions(Throwable.class) and ignoreExceptions(RuntimeException.class)
         * would capture all Errors and checked Exceptions, and ignore RuntimeExceptions.
         * <p>
         * For a more sophisticated exception management use the
         * @see #recordException(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder recordExceptions(
            @Nullable Class<? extends Throwable>... errorClasses) {
            this.recordExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures a list of error classes that are ignored and thus neither count as a failure
         * nor success. Any exception matching or inheriting from one of the list will not count as
         * a failure nor success, even if marked via {@link #recordExceptions(Class[])} or {@link
         * #recordException(Predicate)}.
         *
         * @param errorClasses the error classes that are ignored
         * @return the CircuitBreakerConfig.Builder
         * @see #recordExceptions(Class[]) . Ignoring an exception has priority over recording an
         * exception.
         * <p>
         * Example: ignoreExceptions(Throwable.class) and recordExceptions(Exception.class) would
         * capture nothing.
         * <p>
         * Example: ignoreExceptions(Exception.class) and recordExceptions(Throwable.class) would
         * capture Errors.
         * <p>
         * For a more sophisticated exception management use the
         * @see #ignoreException(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder ignoreExceptions(
            @Nullable Class<? extends Throwable>... errorClasses) {
            this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Enables automatic transition from OPEN to HALF_OPEN state once the
         * waitDurationInOpenState has passed.
         *
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder enableAutomaticTransitionFromOpenToHalfOpen() {
            this.automaticTransitionFromOpenToHalfOpenEnabled = true;
            return this;
        }

        /**
         * Enables automatic transition from OPEN to HALF_OPEN state once the
         * waitDurationInOpenState has passed.
         *
         * @param enableAutomaticTransitionFromOpenToHalfOpen the flag to enable the automatic
         *                                                    transitioning.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder automaticTransitionFromOpenToHalfOpenEnabled(
            boolean enableAutomaticTransitionFromOpenToHalfOpen) {
            this.automaticTransitionFromOpenToHalfOpenEnabled = enableAutomaticTransitionFromOpenToHalfOpen;
            return this;
        }

        /**
         * Builds a CircuitBreakerConfig
         *
         * @return the CircuitBreakerConfig
         * @throws IllegalStateException when the parameter is invalid
         */
        public CircuitBreakerConfig build() {
            this.waitIntervalFunctionInOpenState = validateWaitIntervalFunctionInOpenState();
            this.recordExceptionPredicate = createRecordExceptionPredicate();
            this.ignoreExceptionPredicate = createIgnoreFailurePredicate();
            return new CircuitBreakerConfig(this);
        }

        private Predicate<Throwable> createIgnoreFailurePredicate() {
            return PredicateCreator.createExceptionsPredicate(ignoreExceptions)
                .map(predicate -> ignoreExceptionPredicate != null ? predicate
                    .or(ignoreExceptionPredicate) : predicate)
                .orElseGet(() -> ignoreExceptionPredicate != null ? ignoreExceptionPredicate
                    : DEFAULT_IGNORE_EXCEPTION_PREDICATE);
        }

        private Predicate<Throwable> createRecordExceptionPredicate() {
            return PredicateCreator.createExceptionsPredicate(recordExceptions)
                .map(predicate -> recordExceptionPredicate != null ? predicate
                    .or(recordExceptionPredicate) : predicate)
                .orElseGet(() -> recordExceptionPredicate != null ? recordExceptionPredicate
                    : DEFAULT_RECORD_EXCEPTION_PREDICATE);
        }

        private IntervalFunction validateWaitIntervalFunctionInOpenState() {
            if (createWaitIntervalFunctionCounter > 1) {
                throw new IllegalStateException("The waitIntervalFunction was configured multiple times " +
                    "which could result in an undesired state. Please verify that waitIntervalFunctionInOpenState " +
                    "and waitDurationInOpenState are not used together.");
            }
            return waitIntervalFunctionInOpenState;
        }
    }
}
