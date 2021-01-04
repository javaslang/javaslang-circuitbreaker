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

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;

/**
 * The {@link ConfigConverter} depending on the underlying {@link io.github.resilience4j.ratelimiter.RateLimiter}
 * implementation will transform the RateLimiterConfig to the equivalent Configuration and vise versa.
 *
 */
interface ConfigConverter<C extends RateLimiterConfig> {

    C from(RateLimiterConfig config);

    RateLimiterConfig to(C config);

    static ConfigConverter<RateLimiterConfig> defaultConverter() {
        return new ConfigConverter<RateLimiterConfig>() {
            @Override
            public RateLimiterConfig from(RateLimiterConfig config) {
                return config;
            }

            @Override
            public RateLimiterConfig to(RateLimiterConfig config) {
                return config;
            }
        };
    }

    static ConfigConverter<RefillRateLimiterConfig> refillConverter() {
        return new ConfigConverter<RefillRateLimiterConfig>() {
            @Override
            public RefillRateLimiterConfig from(RateLimiterConfig config) {
                return new RefillRateLimiterConfig.Builder()
                    .timeoutDuration(config.getTimeoutDuration())
                    .limitRefreshPeriod(config.getLimitRefreshPeriod())
                    .limitForPeriod(config.getLimitForPeriod())
                    .writableStackTraceEnabled(config.isWritableStackTraceEnabled())
                    .build();
            }

            @Override
            public RateLimiterConfig to(RefillRateLimiterConfig config) {
                return config;
            }
        };
    }

}
