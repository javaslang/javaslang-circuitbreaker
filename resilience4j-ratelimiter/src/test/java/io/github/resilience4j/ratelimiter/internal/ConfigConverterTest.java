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
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class ConfigConverterTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    @Test
    public void testDefaultConverter() {
        ConfigConverter<RateLimiterConfig> converter = ConfigConverter.defaultConverter();
        RateLimiterConfig config = buildDefault();
        RateLimiterConfig result = converter.from(config);
        then(config).isEqualTo(result);
    }

    @Test
    public void testRefillConverter() {
        ConfigConverter<RefillRateLimiterConfig> converter = ConfigConverter.refillConverter();
        RateLimiterConfig config = buildDefault();
        RefillRateLimiterConfig refillConfig = converter.from(config);
        then(refillConfig.getNanosPerPermit()).isEqualTo(REFRESH_PERIOD.toNanos()/LIMIT);
        then(refillConfig.getInitialPermits()).isEqualTo(50);
        then(refillConfig.getPermitCapacity()).isEqualTo(50);
        then(refillConfig.getLimitForPeriod()).isEqualTo(50);
        then(refillConfig.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
    }

    private RateLimiterConfig buildDefault() {
        return RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }


}