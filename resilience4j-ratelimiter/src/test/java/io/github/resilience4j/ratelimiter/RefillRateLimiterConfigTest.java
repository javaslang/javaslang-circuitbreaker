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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class RefillRateLimiterConfigTest {

    private static final int LIMIT = 50;
    private static final int PERMIT_CAPACITY = 60;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String REFRESH_PERIOD_MUST_NOT_BE_NULL = "RefreshPeriod must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void builderPositive() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();

        then(config.getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(LIMIT);
    }

    @Test
    public void builderLimitCapacityAdjusted() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .permitCapacity(PERMIT_CAPACITY)
            .build();

        Duration adjustedPeriod = REFRESH_PERIOD.dividedBy(LIMIT).multipliedBy(PERMIT_CAPACITY);

        then(config.getLimitForPeriod()).isEqualTo(PERMIT_CAPACITY);
        then(config.getLimitRefreshPeriod()).isEqualTo(adjustedPeriod);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(PERMIT_CAPACITY);
    }

    @Test
    public void testDefaultBurst() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();

        then(config.getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getPermitCapacity()).isEqualTo(LIMIT);
    }

    @Test
    public void testDefaultInitialPermits() throws Exception {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();

        then(config.getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getInitialPermits()).isEqualTo(LIMIT);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        RefillRateLimiterConfig.custom()
            .timeoutDuration(null);
    }

    @Test
    public void builderRefreshPeriodIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(REFRESH_PERIOD_MUST_NOT_BE_NULL);
        RefillRateLimiterConfig.custom()
            .limitRefreshPeriod(null);
    }

    @Test
    public void builderRefreshPeriodTooShort() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("RefreshPeriod is too short");
        RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(Duration.ZERO)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void builderLimitIsLessThanOne() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("LimitForPeriod should be greater than 0");
        RefillRateLimiterConfig.custom()
            .limitForPeriod(0);
    }

}
