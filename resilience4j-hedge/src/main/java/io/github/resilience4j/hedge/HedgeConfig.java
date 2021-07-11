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

import java.io.Serializable;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class HedgeConfig implements Serializable {

    private static final long serialVersionUID = 2203981592465761602L;

    private static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";

    public enum MetricsType {
        PRECONFIGURED, AVERAGE_PLUS
    }

    private final MetricsType metricsType;
    private final boolean shouldUseFactorAsPercentage;
    private final int hedgeTimeFactor;
    private final boolean shouldMeasureErrors;
    private final int windowSize;
    //fixed only
    private final Duration cutoff;

    private HedgeConfig(MetricsType metricsType, boolean shouldUseFactorAsPercentage, int hedgeTimeFactor, boolean shouldMeasureErrors, int windowSize, Duration cutoff) {
        this.metricsType = metricsType;
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.hedgeTimeFactor = hedgeTimeFactor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.windowSize = windowSize;
        this.cutoff = cutoff;
    }

    public HedgeMetrics newMetrics() {
        if (metricsType == MetricsType.PRECONFIGURED) {
            return HedgeMetrics.ofPreconfigured(cutoff);
        } else {
            return HedgeMetrics.ofAveragePlus(shouldUseFactorAsPercentage, hedgeTimeFactor,
                shouldMeasureErrors, windowSize);
        }
    }

    /**
     * Returns a builder to create a custom HedgeConfig.
     *
     * @return a {@link HedgeConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    public static Builder from(HedgeConfig baseConfig) {
        return new Builder(baseConfig);
    }

    /**
     * Creates a default Hedge configuration.
     *
     * @return a default Hedge configuration.
     */
    public static HedgeConfig ofDefaults() {
        return new Builder().build();
    }

    private static Duration checkHedgeDuration(final Duration hedgeDuration) {
        return requireNonNull(hedgeDuration, HEDGE_DURATION_MUST_NOT_BE_NULL);
    }


    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
            .append("HedgeConfig{")
            .append(shouldUseFactorAsPercentage).append(",")
            .append(hedgeTimeFactor).append(",")
            .append(shouldMeasureErrors).append(",")
            .append(windowSize).append(",")
            .append(cutoff)
            .append("}");
        return stringBuilder.toString();
    }

    public static class Builder {

        private MetricsType metricsType = MetricsType.PRECONFIGURED;
        private boolean shouldUseFactorAsPercentage = false;
        private int hedgeTimeFactor = 0;
        private boolean shouldMeasureErrors = true;
        private int windowSize = 100;
        private Duration cutoff;

        public Builder() {
        }

        public Builder(HedgeConfig baseConfig) {
            //implement
        }

        public static Builder fromConfig(HedgeConfig baseConfig) {
            return new Builder(baseConfig);
        }

        /**
         * Builds a HedgeConfig
         *
         * @return the HedgeConfig
         */
        public HedgeConfig build() {
            return new HedgeConfig(
                metricsType,
                shouldUseFactorAsPercentage,
                hedgeTimeFactor,
                shouldMeasureErrors,
                windowSize,
                cutoff);
        }

        public Builder averagePlusPercentMetrics(int hedgeTimeFactor, boolean shouldMeasureErrors) {
            this.metricsType = MetricsType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = true;
            this.hedgeTimeFactor = hedgeTimeFactor;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return this;
        }

        public Builder averagePlusAmountMetrics(int amount, boolean shouldMeasureErrors) {
            this.metricsType = MetricsType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = false;
            this.hedgeTimeFactor = amount;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return this;
        }

        public Builder preconfiguredMetrics(Duration cutoff) {
            if (cutoff == null) {
                throw new NullPointerException(HEDGE_DURATION_MUST_NOT_BE_NULL);
            }
            this.metricsType = MetricsType.PRECONFIGURED;
            this.cutoff = cutoff;
            return this;
        }

        public Builder windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }
    }
}
