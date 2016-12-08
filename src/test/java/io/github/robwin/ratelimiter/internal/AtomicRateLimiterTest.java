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
package io.github.robwin.ratelimiter.internal;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.equalTo;

import com.jayway.awaitility.core.ConditionFactory;
import io.github.robwin.ratelimiter.RateLimiterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AtomicRateLimiter.class)
public class AtomicRateLimiterTest {

    public static final String LIMITER_NAME = "test";
    public static final long CYCLE_IN_NANOS = 500_000_000L;
    public static final long POLL_INTERVAL_IN_NANOS = 2_000_000L;
    public static final int TIMEOUT = 100;
    private RateLimiterConfig rateLimiterConfig;
    private AtomicRateLimiter rateLimiter;
    private AtomicRateLimiter.AtomicRateLimiterMetrics metrics;

    private static ConditionFactory awaitImpatiently() {
        return await()
            .pollDelay(1, TimeUnit.MICROSECONDS)
            .pollInterval(POLL_INTERVAL_IN_NANOS, TimeUnit.NANOSECONDS);
    }

    private void setTimeOnNanos(long nanoTime) throws Exception {
        PowerMockito.doReturn(nanoTime)
            .when(rateLimiter, "currentNanoTime");
    }

    @Before
    public void setup() {
        rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofNanos(CYCLE_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .build();
        AtomicRateLimiter testLimiter = new AtomicRateLimiter(LIMITER_NAME, rateLimiterConfig);
        rateLimiter = PowerMockito.spy(testLimiter);
        metrics = rateLimiter.getDetailedMetrics();
    }

    @Test
    public void acquireAndRefresh() throws Exception {
        setTimeOnNanos(CYCLE_IN_NANOS);
        boolean permission = rateLimiter.getPermission(Duration.ZERO);
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        boolean secondPermission = rateLimiter.getPermission(Duration.ZERO);
        then(secondPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);

        setTimeOnNanos(CYCLE_IN_NANOS * 2);
        boolean thirdPermission = rateLimiter.getPermission(Duration.ZERO);
        then(thirdPermission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        boolean fourthPermission = rateLimiter.getPermission(Duration.ZERO);
        then(fourthPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
    }

    @Test
    public void reserveAndRefresh() throws Exception {
        setTimeOnNanos(CYCLE_IN_NANOS);
        boolean permission = rateLimiter.getPermission(Duration.ZERO);
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);

        AtomicReference<Boolean> reservedPermission = new AtomicReference<>(null);
        Thread caller = new Thread(
            () -> reservedPermission.set(rateLimiter.getPermission(Duration.ofNanos(CYCLE_IN_NANOS))));
        caller.setDaemon(true);
        caller.start();
        awaitImpatiently()
            .atMost(TIMEOUT, MILLISECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS + CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        setTimeOnNanos(CYCLE_IN_NANOS * 2 + 10);
        awaitImpatiently()
            .atMost(CYCLE_IN_NANOS + POLL_INTERVAL_IN_NANOS, NANOSECONDS)
            .until(reservedPermission::get, equalTo(true));

        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS - 10);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void reserveFewThenSkipCyclesBeforeRefresh() throws Exception {
        setTimeOnNanos(CYCLE_IN_NANOS);
        boolean permission = rateLimiter.getPermission(Duration.ZERO);
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        AtomicReference<Boolean> firstReservedPermission = new AtomicReference<>(null);
        Thread firstCaller = new Thread(
            () -> firstReservedPermission.set(rateLimiter.getPermission(Duration.ofNanos(CYCLE_IN_NANOS))));
        firstCaller.setDaemon(true);
        firstCaller.start();
        awaitImpatiently()
            .atMost(TIMEOUT, MILLISECONDS)
            .until(firstCaller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS * 2);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);


        AtomicReference<Boolean> secondReservedPermission = new AtomicReference<>(null);
        Thread secondCaller = new Thread(
            () -> secondReservedPermission.set(rateLimiter.getPermission(Duration.ofNanos(CYCLE_IN_NANOS * 2))));
        secondCaller.setDaemon(true);
        secondCaller.start();
        awaitImpatiently()
            .atMost(TIMEOUT, MILLISECONDS)
            .until(secondCaller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-2);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS * 3);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(2);

        setTimeOnNanos(CYCLE_IN_NANOS * 6 + 10);
        awaitImpatiently()
            .atMost(CYCLE_IN_NANOS + POLL_INTERVAL_IN_NANOS, NANOSECONDS)
            .until(firstReservedPermission::get, equalTo(true));
        awaitImpatiently()
            .atMost(CYCLE_IN_NANOS * 2 + POLL_INTERVAL_IN_NANOS, NANOSECONDS)
            .until(secondReservedPermission::get, equalTo(true));
        then(metrics.getAvailablePermissions()).isEqualTo(1);
        then(metrics.getNanosToWait()).isEqualTo(0L);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void rejectedByTimeout() throws Exception {
        setTimeOnNanos(CYCLE_IN_NANOS);
        boolean permission = rateLimiter.getPermission(Duration.ZERO);
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        AtomicReference<Boolean> declinedPermission = new AtomicReference<>(null);
        Thread caller = new Thread(
            () -> declinedPermission.set(rateLimiter.getPermission(Duration.ofNanos(CYCLE_IN_NANOS - 1))));
        caller.setDaemon(true);
        caller.start();

        awaitImpatiently()
            .atMost(TIMEOUT, MILLISECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        setTimeOnNanos(CYCLE_IN_NANOS * 2 - 1);
        awaitImpatiently()
            .atMost(CYCLE_IN_NANOS + POLL_INTERVAL_IN_NANOS, NANOSECONDS)
            .until(declinedPermission::get, equalTo(false));
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(1L);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void waitingThreadIsInterrupted() throws Exception {
        setTimeOnNanos(CYCLE_IN_NANOS);
        boolean permission = rateLimiter.getPermission(Duration.ZERO);
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);

        AtomicReference<Boolean> declinedPermission = new AtomicReference<>(null);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        Thread caller = new Thread(
            () -> {
                declinedPermission.set(rateLimiter.getPermission(Duration.ofNanos(CYCLE_IN_NANOS - 1)));
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            }
        );
        caller.isDaemon();
        caller.start();

        awaitImpatiently()
            .atMost(TIMEOUT, MILLISECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        caller.interrupt();
        awaitImpatiently()
            .atMost(CYCLE_IN_NANOS + POLL_INTERVAL_IN_NANOS, NANOSECONDS)
            .until(declinedPermission::get, equalTo(false));
        then(wasInterrupted.get()).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(CYCLE_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void namePropagation() {
        then(rateLimiter.getName()).isEqualTo(LIMITER_NAME);
    }

    @Test
    public void configPropagation() {
        then(rateLimiter.getRateLimiterConfig()).isEqualTo(rateLimiterConfig);
    }

    @Test
    public void metrics() {
        then(rateLimiter.getMetrics().getNumberOfWaitingThreads()).isEqualTo(0);
    }
}