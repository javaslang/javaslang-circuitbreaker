/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.metrics;

import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.HedgeEvent;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AveragePlusMetrics implements HedgeMetrics {

    final boolean shouldUseFactorAsPercentage;
    final boolean shouldMeasureErrors;
    final int factor;
    final FixedSizeSlidingWindowMetrics metrics;

    public AveragePlusMetrics(boolean shouldUseFactorAsPercentage, int factor, boolean shouldMeasureErrors, int windowSize) {
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.factor = factor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.metrics = new FixedSizeSlidingWindowMetrics(windowSize);
    }

    @Override
    public Duration getResponseTimeCutoff() {
        final Duration result;
        if (factor == 0) {
            result = getAverageResponseTime();
        } else {
            if (shouldUseFactorAsPercentage) {
                result = getAverageResponseTime().multipliedBy(factor).dividedBy(100);
            } else {
                result = getAverageResponseTime().plus(Duration.ofMillis(factor));
            }
        }
        return result;
    }

    @Override
    public boolean requiresInput() {
        return true;
    }

    private Duration getAverageResponseTime() {
        return metrics.getSnapshot().getAverageDuration();
    }

    @Override
    public void accept(HedgeEvent event) {
        switch (event.getEventType()) {
            case PRIMARY_SUCCESS:
                metrics.record(event.getDuration().toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.SUCCESS);
                break;
            case PRIMARY_FAILURE:
                if (shouldMeasureErrors) {
                    metrics.record(event.getDuration().toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.ERROR);
                }
                break;
        }
    }

}
