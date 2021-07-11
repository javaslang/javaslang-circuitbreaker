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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.event.HedgeEvent;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;


public class HedgeImplFutureBehaviorsTest {

    private static final Duration FIFTY_MILLIS = Duration.ofMillis(50);
    private static final String PRIMARY = "Primary";
    private static final String HEDGED = "Hedged";
    private static final int FAST_SPEED = 10;
    private static final int SLOW_SPEED = 200;
    private static final String PRIMARY_EXCEPTION_MESSAGE = "Primary threw an exception";
    private static final String HEDGE_EXCEPTION_MESSAGE = "Hedge threw an exception";
    private static final RuntimeException HEDGE_RUNTIME_EXCEPTION = new RuntimeException(HEDGE_EXCEPTION_MESSAGE);
    private static final RuntimeException PRIMARY_RUNTIME_EXCEPTION = new RuntimeException(PRIMARY_EXCEPTION_MESSAGE);
    private static final HedgeBehavior FAST_PRIMARY_SUCCESS = new HedgeBehavior(FAST_SPEED, PRIMARY, null);
    private static final HedgeBehavior FAST_PRIMARY_FAILURE = new HedgeBehavior(FAST_SPEED, PRIMARY, PRIMARY_RUNTIME_EXCEPTION);
    private static final HedgeBehavior FAST_HEDGE_SUCCESS = new HedgeBehavior(FAST_SPEED, HEDGED, null);
    private static final HedgeBehavior FAST_HEDGE_FAILURE = new HedgeBehavior(FAST_SPEED, HEDGED, HEDGE_RUNTIME_EXCEPTION);
    private static final HedgeBehavior SLOW_PRIMARY_SUCCESS = new HedgeBehavior(SLOW_SPEED, PRIMARY, null);
    private static final HedgeBehavior SLOW_PRIMARY_FAILURE = new HedgeBehavior(SLOW_SPEED, PRIMARY, PRIMARY_RUNTIME_EXCEPTION);
    private static final HedgeBehavior SLOW_HEDGE_SUCCESS = new HedgeBehavior(SLOW_SPEED, HEDGED, null);
    private static final HedgeBehavior SLOW_HEDGE_FAILURE = new HedgeBehavior(SLOW_SPEED, HEDGED, HEDGE_RUNTIME_EXCEPTION);
    private final Logger logger = mock(Logger.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(15);

    @Test
    public void slowPrimarySuccessFastHedgeSuccessReturnsHedgeSuccess() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onHedgeSuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.HEDGE_SUCCESS.toString());
    }

    @Test
    public void slowPrimarySuccessFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_FAILURE};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onHedgeFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        try {
            String result = hedged.get();
            fail("call should not succeed. instead returned " + result);
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.HEDGE_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimaryFailureFastHedgeSuccessReturnsHedgeSuccess() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onHedgeSuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.HEDGE_SUCCESS.toString());
    }

    @Test
    public void slowPrimaryFailureFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_FAILURE, FAST_HEDGE_FAILURE};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onHedgeFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.HEDGE_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimarySuccessSlowHedgeSuccessReturnsPrimarySuccess() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_SUCCESS};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimarySuccessSlowHedgeFailureReturnsPrimarySuccess() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_FAILURE};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimaryFailureSlowHedgeSuccessReturnsPrimaryFailure() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_SUCCESS};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimaryFailureSlowHedgeFailureReturnsPrimaryFailure() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_FAILURE};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void fastPrimaryFailureDoesNotHedge() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {FAST_PRIMARY_FAILURE, FAST_HEDGE_SUCCESS};
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void fastPrimarySuccessDoesNotHedge() throws Exception {
        HedgeBehavior[] hedgeBehaviors = {FAST_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Supplier<Future<String>> futureSupplier = hedgingFutureSupplier(hedgeBehaviors);
        Future<String> hedged = hedge.decorateFuture(futureSupplier).get();
        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    private Supplier<Future<String>> hedgingFutureSupplier(HedgeBehavior[] hedgeBehaviors) {
        AtomicInteger iterations = new AtomicInteger(0);
        return () -> executor.submit(() -> {
            int iteration = iterations.getAndIncrement();
            System.out.println("in the future, call #" + iteration);
            try {
                System.out.println("future call is sleeping " + hedgeBehaviors[iteration].sleep + "ms");
                Thread.sleep(hedgeBehaviors[iteration].sleep);
                System.out.println("future call is done sleeping");
            } catch (InterruptedException e) {
                //do nothing
            }
            if (hedgeBehaviors[iteration].exception != null) {
                System.out.println("future call " + iteration + " is going to throw");
                throw hedgeBehaviors[iteration].exception;
            } else {
                System.out.println("future call " + iteration + " is going to return " +  hedgeBehaviors[iteration].result);
                return hedgeBehaviors[iteration].result;
            }
        });
    }

    private static class HedgeBehavior {
        final int sleep;
        final String result;
        final RuntimeException exception;

        public HedgeBehavior(int sleep, String result, RuntimeException exception) {
            this.sleep = sleep;
            this.result = result;
            this.exception = exception;
        }
    }

}