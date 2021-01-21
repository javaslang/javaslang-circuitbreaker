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
package io.github.resilience4j.bulkhead.adaptive.internal;


import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.core.metrics.Metrics.Outcome;

class AdaptiveBulkheadMetrics implements AdaptiveBulkhead.Metrics {

    private final Metrics metrics;
    private final float failureRateThreshold;
    private final float slowCallRateThreshold;
    private final long slowCallDurationThresholdInNanos;
    private final int minimumNumberOfCalls;

    private AdaptiveBulkheadMetrics(int slidingWindowSize,
        AdaptiveBulkheadConfig.SlidingWindowType slidingWindowType,
        AdaptiveBulkheadConfig adaptiveBulkheadConfig) {
        if (slidingWindowType == AdaptiveBulkheadConfig.SlidingWindowType.COUNT_BASED) {
            this.metrics = new FixedSizeSlidingWindowMetrics(slidingWindowSize);
            this.minimumNumberOfCalls = Math
                .min(adaptiveBulkheadConfig.getMinimumNumberOfCalls(), slidingWindowSize);
        } else {
            this.metrics = new SlidingTimeWindowMetrics(slidingWindowSize);
            this.minimumNumberOfCalls = adaptiveBulkheadConfig.getMinimumNumberOfCalls();
        }
        this.failureRateThreshold = adaptiveBulkheadConfig.getFailureRateThreshold();
        this.slowCallRateThreshold = adaptiveBulkheadConfig.getSlowCallRateThreshold();
        this.slowCallDurationThresholdInNanos = adaptiveBulkheadConfig.getSlowCallDurationThreshold()
            .toNanos();
    }

    public AdaptiveBulkheadMetrics(AdaptiveBulkheadConfig adaptiveBulkheadConfig) {
        this(adaptiveBulkheadConfig.getSlidingWindowSize(), adaptiveBulkheadConfig.getSlidingWindowType(), adaptiveBulkheadConfig);
    }

    /**
     * Records a successful call and checks if the thresholds are exceeded.
     *
     * @return the result of the check
     */
    public Result onSuccess(long duration, TimeUnit durationUnit) {
        Snapshot snapshot;
        if (durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos) {
            snapshot = record(duration, durationUnit, Outcome.SLOW_SUCCESS);
        } else {
            snapshot = record(duration, durationUnit, Outcome.SUCCESS);
        }
        return checkIfThresholdsExceeded(snapshot);
    }

    /**
     * Records a failed call and checks if the thresholds are exceeded.
     *
     * @return the result of the check
     */
    public Result onError(long duration, TimeUnit durationUnit) {
        Snapshot snapshot;
        if (durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos) {
            snapshot = record(duration, durationUnit, Outcome.SLOW_ERROR);
        } else {
            snapshot = record(duration, durationUnit, Outcome.ERROR);
        }
        return checkIfThresholdsExceeded(snapshot);
    }

    Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        return metrics.record(duration, durationUnit, outcome);
    }

    /**
     * Checks if the failure rate is above the threshold or if the slow calls percentage is above
     * the threshold.
     *
     * @param snapshot a metrics snapshot
     * @return false, if the thresholds haven't been exceeded.
     */
    private Result checkIfThresholdsExceeded(Snapshot snapshot) {
        float failureRateInPercentage = getFailureRate(snapshot);
        if (failureRateInPercentage == -1) {
            return Result.BELOW_MINIMUM_CALLS_THRESHOLD;
        }
        if (failureRateInPercentage >= failureRateThreshold) {
            return Result.ABOVE_THRESHOLDS;
        }
        float slowCallsInPercentage = getSlowCallRate(snapshot);
        if (slowCallsInPercentage >= slowCallRateThreshold) {
            return Result.ABOVE_THRESHOLDS;
        }
        return Result.BELOW_THRESHOLDS;
    }

    private float getSlowCallRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < minimumNumberOfCalls) {
            return -1.0f;
        }
        return snapshot.getSlowCallRate();
    }

    private float getFailureRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < minimumNumberOfCalls) {
            return -1.0f;
        }
        return snapshot.getFailureRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFailureRate() {
        return getFailureRate(metrics.getSnapshot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getSlowCallRate() {
        return getSlowCallRate(metrics.getSnapshot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSuccessfulCalls() {
        return this.metrics.getSnapshot().getNumberOfSuccessfulCalls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfBufferedCalls() {
        return this.metrics.getSnapshot().getTotalNumberOfCalls();
    }

    @Override
    public int getNumberOfFailedCalls() {
        return this.metrics.getSnapshot().getNumberOfFailedCalls();
    }

    @Override
    public int getNumberOfSlowCalls() {
        return this.metrics.getSnapshot().getTotalNumberOfSlowCalls();
    }

    @Override
    public int getNumberOfSlowSuccessfulCalls() {
        return this.metrics.getSnapshot().getNumberOfSlowSuccessfulCalls();
    }

    @Override
    public int getNumberOfSlowFailedCalls() {
        return this.metrics.getSnapshot().getNumberOfSlowFailedCalls();
    }

    @Override
    public int getAvailableConcurrentCalls() {
        // TODO
        throw new NotImplementedException();
    }

    @Override
    public int getMaxAllowedConcurrentCalls() {
        // TODO
        throw new NotImplementedException();
    }

    enum Result {
        BELOW_THRESHOLDS,
        ABOVE_THRESHOLDS,
        BELOW_MINIMUM_CALLS_THRESHOLD
    }
}
