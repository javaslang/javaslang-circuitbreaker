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

import com.jayway.awaitility.core.ConditionFactory;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.vavr.collection.HashMap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.equalTo;


@RunWith(PowerMockRunner.class)
@PrepareForTest(RefillRateLimiter.class)
public class RefillRateLimiterTest {

    private static final String LIMITER_NAME = "test";
    private static final long PERIOD_IN_NANOS = 250_000_000L;
    private static final long POLL_INTERVAL_IN_NANOS = 2_000_000L;
    private static final int PERMISSIONS_IN_PERIOD = 1;
    private RefillRateLimiter rateLimiter;
    private RefillRateLimiter.RefillRateLimiterMetrics metrics;

    private static ConditionFactory awaitImpatiently() {
        return await()
            .pollDelay(1, TimeUnit.MICROSECONDS)
            .pollInterval(POLL_INTERVAL_IN_NANOS, TimeUnit.NANOSECONDS);
    }

    protected RateLimiter buildRateLimiter(RefillRateLimiterConfig config) {
        RefillRateLimiterConfig refillConfig = RefillRateLimiterConfig.from(config)
            .permitCapacity(10)
            .initialPermits(0)
            .build();
        return new RefillRateLimiter("refill", refillConfig);
    }

    private void setTimeOnNanos(long nanoTime) throws Exception {
        PowerMockito.doReturn(nanoTime)
            .when(rateLimiter, "refillLimiterNanoTime");
    }

    public void setup(Duration timeoutDuration) {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), timeoutDuration, PERMISSIONS_IN_PERIOD);
    }

    public void setup(Duration timeoutDuration, long startNanos) {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), timeoutDuration, PERMISSIONS_IN_PERIOD, startNanos);
    }

    public void setup(Duration periodDuration, Duration timeoutDuration, int permissionPerCycle) {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(permissionPerCycle)
            .limitRefreshPeriod(periodDuration)
            .timeoutDuration(timeoutDuration)
            .build();
        RefillRateLimiter testLimiter = new RefillRateLimiter(LIMITER_NAME, rateLimiterConfig);
        rateLimiter = PowerMockito.spy(testLimiter);
        metrics = rateLimiter.getDetailedMetrics();
    }

    public void setup(Duration periodDuration, Duration timeoutDuration, int permissionPerCycle, long startNanos) {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(permissionPerCycle)
            .limitRefreshPeriod(periodDuration)
            .timeoutDuration(timeoutDuration)
            .build();
        RefillRateLimiter testLimiter = new RefillRateLimiter(LIMITER_NAME, rateLimiterConfig, HashMap.empty(),startNanos);
        rateLimiter = PowerMockito.spy(testLimiter);
        metrics = rateLimiter.getDetailedMetrics();
    }

    /**
     * Added more nanos in order to handle the refills
     * Equivalent to {@link RateLimitersImplementationTest#acquireBigNumberOfPermitsAtStartOfCycleTest}
     */
    @Test
    public void acquireBigNumberOfPermitsAtStartTest() {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .initialPermits(0)
            .limitRefreshPeriod(Duration.ofNanos(600_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(5);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        /**
         * Due to the period being small it can be easily replenished
         */
        boolean firstNoPermission = limiter.acquirePermission(5);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInNewCyclePermission = limiter.acquirePermission(1);
        then(retryInNewCyclePermission).isTrue();
    }

    /**
     * Added more nanos in order to handle the refills
     * Equivalent to {@link RateLimitersImplementationTest#tryToAcquireBigNumberOfPermitsAtEndOfCycleTest}
     */
    @Test
    public void tryToAcquireBigNumberOfPermitsOnFullCapacity() {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .initialPermits(0)
            .limitRefreshPeriod(Duration.ofNanos(600_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(1);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(6);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInSecondCyclePermission = limiter.acquirePermission(5);
        then(retryInSecondCyclePermission).isTrue();
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#notSpyRawTest}
     */
    @Test
    public void notSpyRawTest() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(PERMISSIONS_IN_PERIOD)
            .limitRefreshPeriod(Duration.ofNanos(PERIOD_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .initialPermits(PERMISSIONS_IN_PERIOD)
            .build();
        RefillRateLimiter rateLimiter = new RefillRateLimiter("refillBasedLimiter", rateLimiterConfig);
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics = rateLimiter
            .getDetailedMetrics();

        boolean firstPermission = rateLimiter.acquirePermission();
        then(firstPermission).isTrue();

        waitForPermissionRenewal(rateLimiterMetrics, '*');
        boolean secondPermission = rateLimiter.acquirePermission();
        then(secondPermission).isTrue();

        boolean firstNoPermission = rateLimiter.acquirePermission();
        then(firstNoPermission).isFalse();

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);
        waitForPermissionRenewal(rateLimiterMetrics, '^');
        boolean thirdPermission = rateLimiter.acquirePermission();
        then(thirdPermission).isTrue();

        /**
         * Permission renewal happens per nanos thus the cycle splitting is not taking effect
         */
        waitNanos(PERIOD_IN_NANOS / 2l, '&');
        boolean fourthPermission = rateLimiter.acquirePermission();
        then(fourthPermission).isTrue();

        boolean secondNoPermission = rateLimiter.acquirePermission();
        then(secondNoPermission).isFalse();
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#notSpyRawNonBlockingTest}
     */
    @Test
    public void notSpyRawNonBlockingTest() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(PERMISSIONS_IN_PERIOD)
            .limitRefreshPeriod(Duration.ofNanos(PERIOD_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .initialPermits(PERMISSIONS_IN_PERIOD)
            .build();

        RefillRateLimiter rateLimiter = new RefillRateLimiter("refillBasedLimiter", rateLimiterConfig);
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics = rateLimiter
            .getDetailedMetrics();

        long firstPermission = rateLimiter.reservePermission();
        waitForPermissionRenewal(rateLimiterMetrics, '*');

        long secondPermission = rateLimiter.reservePermission();
        long firstNoPermission = rateLimiter.reservePermission();

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);

        /**
         * Permission renewal happens per nanos thus the cycle splitting is not taking effect
         */
        waitNanos(PERIOD_IN_NANOS, '^');
        long thirdPermission = rateLimiter.reservePermission();
        long fourthPermission = rateLimiter.reservePermission();
        long secondNoPermission = rateLimiter.reservePermission();


        then(firstPermission).isZero();
        then(secondPermission).isZero();
        then(thirdPermission).isZero();
        then(fourthPermission).isZero();

        then(firstNoPermission).isNegative();
        then(secondNoPermission).isNegative();
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#permissionsInFirstCycle}
     *
     * @throws Exception
     */
    @Test
    public void defaultPermissionsAtStartup() throws Exception {
        setup(Duration.ZERO, 0l);

        setTimeOnNanos(PERIOD_IN_NANOS);
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        int availablePermissions = metrics.getAvailablePermissions();
        then(availablePermissions).isEqualTo(PERMISSIONS_IN_PERIOD);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#acquireAndRefreshWithEventPublishing}
     *
     * @throws Exception
     */
    @Test
    public void acquireAndRefreshWithEventPublishing() throws Exception {
        setup(Duration.ZERO, PERIOD_IN_NANOS);
        setTimeOnNanos(PERIOD_IN_NANOS);

        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        boolean secondPermission = rateLimiter.acquirePermission();
        then(secondPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS * 2);
        boolean thirdPermission = rateLimiter.acquirePermission();
        then(thirdPermission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        boolean fourthPermission = rateLimiter.acquirePermission();
        then(fourthPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#reserveAndRefresh}
     *
     * @throws Exception
     */
    @Test
    public void reserveAndRefresh() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS);
        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);

        AtomicReference<Boolean> reservedPermission = new AtomicReference<>(null);
        Thread caller = new Thread(
            () -> reservedPermission.set(rateLimiter.acquirePermission()));
        caller.setDaemon(true);
        caller.start();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS*2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        setTimeOnNanos(PERIOD_IN_NANOS * 2 + 10);
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(reservedPermission::get, equalTo(true));

        /**
         * The update happens due to the reservation and is on PERIOD_IN_NANOS*2 so 10 less nanos to be needed
         */
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS-10);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#reserveFewThenSkipCyclesBeforeRefreshNonBlocking}
     *
     * @throws Exception
     */
    @Test
    public void reserveFewThenSkipNanosBeforeRefillNonBlocking() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS);
        long permission = rateLimiter.reservePermission();
        then(permission).isZero();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        long reservation = rateLimiter.reservePermission();
        then(reservation).isPositive();
        then(reservation).isLessThanOrEqualTo(PERIOD_IN_NANOS);
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        long additionalReservation = rateLimiter.reservePermission();
        then(additionalReservation).isEqualTo(-1);
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        setTimeOnNanos(PERIOD_IN_NANOS * 6 + 10);
        then(metrics.getAvailablePermissions()).isEqualTo(1);
        then(metrics.getNanosToWait()).isEqualTo(0L);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#reserveFewThenSkipCyclesBeforeRefresh}
     *
     * @throws Exception
     */
    @Test
    public void reserveFewThenWaitBeforeReachingMaxCapacity() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS);
        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        AtomicReference<Boolean> firstReservedPermission = new AtomicReference<>(null);
        Thread firstCaller = new Thread(
            () -> firstReservedPermission.set(rateLimiter.acquirePermission()));
        firstCaller.setDaemon(true);
        firstCaller.start();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(firstCaller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        AtomicReference<Boolean> secondReservedPermission = new AtomicReference<>(null);
        Thread secondCaller = new Thread(
            () -> secondReservedPermission.set(rateLimiter.acquirePermission()));
        secondCaller.setDaemon(true);
        secondCaller.start();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(secondCaller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(2);

        setTimeOnNanos(PERIOD_IN_NANOS * 6 + 10);
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(firstReservedPermission::get, equalTo(true));
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(secondReservedPermission::get, equalTo(false));
        then(metrics.getAvailablePermissions()).isEqualTo(1);
        then(metrics.getNanosToWait()).isEqualTo(0L);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * @throws Exception
     */
    @Test(expected = RequestNotPermitted.class)
    public void tryToReserveMoreThatRateLimiterCapacity() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS * 5), 0l);
        setTimeOnNanos(PERIOD_IN_NANOS);
        rateLimiter.reservePermission(PERMISSIONS_IN_PERIOD * 3);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#rejectedByTimeoutNonBlocking}
     *
     * @throws Exception
     */
    @Test
    public void rejectedByTimeoutNonBlocking() throws Exception {
        setup(Duration.ZERO, 0l);

        setTimeOnNanos(0l);
        long permission = rateLimiter.reservePermission();
        then(permission).isZero();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        long failedPermission = rateLimiter.reservePermission();
        then(failedPermission).isNegative();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        long updatedPeriod = PERIOD_IN_NANOS - 1;
        setTimeOnNanos(updatedPeriod);

        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo((1));
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#waitingThreadIsInterrupted}
     *
     * @throws Exception
     */
    @Test
    public void waitingThreadIsInterrupted() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS);
        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        AtomicReference<Boolean> reservedPermission = new AtomicReference<>(null);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        Thread caller = new Thread(
            () -> {
                reservedPermission.set(rateLimiter.acquirePermission());
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            }
        );
        caller.setDaemon(true);
        caller.start();

        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        caller.interrupt();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(reservedPermission::get, equalTo(false));
        then(wasInterrupted.get()).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#changePermissionsLimitBetweenCycles}
     *
     * @throws Exception
     */
    @Test
    public void changePermissionsLimitBetweenCycles() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS);
        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);

        AtomicReference<Boolean> reservedPermission = new AtomicReference<>(null);
        Thread caller = new Thread(
            () -> reservedPermission.set(rateLimiter.acquirePermission()));
        caller.setDaemon(true);
        caller.start();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS + PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);
        then(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
            .isEqualTo(PERMISSIONS_IN_PERIOD * 2);
        then(metrics.getAvailablePermissions()).isEqualTo(-2);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS+PERIOD_IN_NANOS/2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        setTimeOnNanos(PERIOD_IN_NANOS * 2 + 10);
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(reservedPermission::get, equalTo(true));

        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS/2 - 10);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#reservePermissionsUpfront}
     *
     * @throws Exception
     */
    @Test
    public void reservePermissionsUpfront() throws Exception {
        final int limitForPeriod = 3;
        final int tasksNum = 9;
        Duration limitRefreshPeriod = Duration.ofMillis(12);
        Duration timeoutDuration = Duration.ofMillis(12);
        long periodInNanos = limitRefreshPeriod.toNanos();

        setup(limitRefreshPeriod, timeoutDuration, limitForPeriod, periodInNanos);
        setTimeOnNanos(periodInNanos);

        ArrayList<Long> timesToWait = new ArrayList<>();
        for (int i = 0; i < tasksNum; i++) {
            setTimeOnNanos(periodInNanos);
            long timeToWait = rateLimiter.reservePermission(1);
            timesToWait.add(timeToWait);
        }
        then(timesToWait).containsExactly(
            0L, 0L, 0L,
            limitRefreshPeriod.toNanos()/3 , 2*limitRefreshPeriod.toNanos()/3, limitRefreshPeriod.toNanos(),
            -1L, -1L, -1L
        );
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#changeDefaultTimeoutDuration}
     *
     * @throws Exception
     */
    @Test
    public void changeDefaultTimeoutDuration() throws Exception {
        setup(Duration.ZERO);

        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ZERO);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(PERMISSIONS_IN_PERIOD);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofNanos(PERIOD_IN_NANOS));

        rateLimiter.changeTimeoutDuration(Duration.ofSeconds(1));
        then(rateLimiterConfig != rateLimiter.getRateLimiterConfig()).isTrue();
        rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(PERMISSIONS_IN_PERIOD);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofNanos(PERIOD_IN_NANOS));
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#changeLimitForPeriod}
     *
     * @throws Exception
     */
    @Test
    public void changeLimitForPeriod() throws Exception {
        setup(Duration.ZERO);

        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ZERO);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(PERMISSIONS_IN_PERIOD);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofNanos(PERIOD_IN_NANOS));

        rateLimiter.changeLimitForPeriod(35);
        long nanosPerPermission = PERIOD_IN_NANOS/35;
        then(rateLimiterConfig != rateLimiter.getRateLimiterConfig()).isTrue();
        rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ZERO);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(35);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofNanos(nanosPerPermission*35));
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#metricsTest}
     *
     * @throws Exception
     */
    @Test
    public void metricsTest() {
        setup(Duration.ZERO);

        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
        then(metrics.getAvailablePermissions()).isEqualTo(1);

        RefillRateLimiter.RefillRateLimiterMetrics detailedMetrics = rateLimiter
            .getDetailedMetrics();
        then(detailedMetrics.getNumberOfWaitingThreads()).isEqualTo(0);
        then(detailedMetrics.getAvailablePermissions()).isEqualTo(1);
        then(detailedMetrics.getNanosToWait()).isEqualTo(0);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#namePropagation}
     *
     * @throws Exception
     */
    @Test
    public void namePropagation() {
        setup(Duration.ZERO);
        then(rateLimiter.getName()).isEqualTo(LIMITER_NAME);
    }

    /**
     * Equivalent to {@link AtomicRateLimiterTest#metrics()}
     *
     * @throws Exception
     */
    @Test
    public void metrics() {
        setup(Duration.ZERO);
        then(rateLimiter.getMetrics().getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void refillTest() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = new RefillRateLimiter("refill", rateLimiterConfig);
        Assert.assertTrue(limiter.acquirePermission(10));
        Thread.sleep(100);
        Assert.assertTrue(limiter.acquirePermission(1));
        Assume.assumeFalse(limiter.acquirePermission(4));
        Thread.sleep(410);
        Assume.assumeTrue(limiter.acquirePermission(4));
        Assume.assumeFalse(limiter.acquirePermission(1));
    }

    @Test
    public void testMaxCapacity() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .permitCapacity(20)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter refillRateLimiter = new RefillRateLimiter("refill", rateLimiterConfig);
        Assert.assertTrue(refillRateLimiter.acquirePermission(10));

        for (int i = 1; i <= 20; i++) {
            Thread.sleep(100 * i);
            Assert.assertTrue(refillRateLimiter.acquirePermission(i));
        }
    }

    @Test
    public void testZeroInitialPermits() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .initialPermits(0)
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter refillRateLimiter = new RefillRateLimiter("refill", rateLimiterConfig);

        for (int i = 1; i <= 10; i++) {
            Thread.sleep(100 * i);
            Assert.assertTrue(refillRateLimiter.acquirePermission(i));
        }

        Assert.assertFalse(refillRateLimiter.acquirePermission(1));
    }

    @Test
    public void testScenarioTest() throws Exception {
        String scenario = "refill_no_wait_five_bucket_scenario.csv";
        List<RefillScenarioEntry> entries = getRefillScenarioEntries(scenario);

        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .initialPermits(5)
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofNanos(1))
            .permitCapacity(5)
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter refillRateLimiter = new RefillRateLimiter("refill_bucket_five", rateLimiterConfig,
            HashMap.empty(),
            1);

        rateLimiter = PowerMockito.spy(refillRateLimiter);

        for(RefillScenarioEntry entry: entries) {
            setTimeOnNanos(entry.getNano());
            boolean result = rateLimiter.acquirePermission(entry.getRequest());
            Assert.assertEquals(entry.getResult(), result);
        }
    }

    private List<RefillScenarioEntry> getRefillScenarioEntries(String scenario) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scenario);
        List<RefillScenarioEntry> entries = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                RefillScenarioEntry refillScenarioEntry = new RefillScenarioEntry(
                    Long.valueOf(values[0]),
                    Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]),
                    Integer.parseInt(values[3]),
                    Boolean.parseBoolean(values[4])
                );

                entries.add(refillScenarioEntry);
            }
        }
        return entries;
    }

    private void waitForMaxPermissions(
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics, char printedWhileWaiting) {

        while (PERMISSIONS_IN_PERIOD > rateLimiterMetrics.getAvailablePermissions()) {
            System.out.print(printedWhileWaiting);
        }

        System.out.println();
    }

    private void waitForPermissionRenewal(
        RefillRateLimiter.RefillRateLimiterMetrics rawDetailedMetrics, char printedWhileWaiting) {
        long nanosToWait = rawDetailedMetrics.getNanosToWait();
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < nanosToWait) {
            System.out.print(printedWhileWaiting);
        }
        System.out.println();
    }

    private void waitNanos(long nanosToWait, char printedWhileWaiting) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < nanosToWait) {
            System.out.print(printedWhileWaiting);
        }
        System.out.println();
    }

    protected void waitForRefresh(RateLimiter.Metrics metrics, RefillRateLimiterConfig config,
                                  char printedWhileWaiting) {
        Instant start = Instant.now();
        while (Instant.now().isBefore(start.plus(config.getLimitRefreshPeriod()))) {
            try {
                if (metrics.getAvailablePermissions() >= config.getPermitCapacity()) {
                    break;
                }
                System.out.print(printedWhileWaiting);
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        System.out.println();
    }
}