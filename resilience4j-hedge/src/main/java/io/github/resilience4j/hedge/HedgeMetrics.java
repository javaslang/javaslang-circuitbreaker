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
package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.event.HedgeEvent;
import io.github.resilience4j.hedge.metrics.AveragePlusMetrics;
import io.github.resilience4j.hedge.metrics.PreconfiguredCutoffMetrics;

import java.time.Duration;

public interface HedgeMetrics {

    Duration getResponseTimeCutoff();

    void accept(HedgeEvent event);

    boolean requiresInput();

    static HedgeMetrics ofAveragePlus(boolean shouldUseFactorAsPercentage, int factor, boolean shouldMeasureErrors, int windowSize) {
        return new AveragePlusMetrics(shouldUseFactorAsPercentage, factor, shouldMeasureErrors, windowSize);
    }

    static HedgeMetrics ofPreconfigured(Duration cutoff) {
        return new PreconfiguredCutoffMetrics(cutoff);
    }
}
