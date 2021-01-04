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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.github.resilience4j.ratelimiter.internal.RefillRateLimiter;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.All)
public class RateLimiterBenchmark {

    private static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 10;
    private static final int THREAD_COUNT = 2;

    private RateLimiter semaphoreBasedRateLimiter;
    private AtomicRateLimiter atomicRateLimiter;
    private RefillRateLimiter refillRateLimiter;

    private Supplier<String> semaphoreGuardedSupplier;
    private Supplier<String> atomicGuardedSupplier;
    private Supplier<String> refillGuardedSupplier;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .addProfiler(GCProfiler.class)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(Integer.MAX_VALUE)
            .limitRefreshPeriod(Duration.ofNanos(10))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        RefillRateLimiterConfig refillRateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofNanos(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        semaphoreBasedRateLimiter = new SemaphoreBasedRateLimiter("semaphoreBased",
            rateLimiterConfig);

        atomicRateLimiter = new AtomicRateLimiter("atomicBased", rateLimiterConfig);
        refillRateLimiter = new RefillRateLimiter("refillBased", refillRateLimiterConfig);

        Supplier<String> stringSupplier = () -> {
            Blackhole.consumeCPU(1);
            return "Hello Benchmark";
        };

        semaphoreGuardedSupplier = RateLimiter
            .decorateSupplier(semaphoreBasedRateLimiter, stringSupplier);
        atomicGuardedSupplier = RateLimiter.decorateSupplier(atomicRateLimiter, stringSupplier);
        refillGuardedSupplier = RateLimiter.decorateSupplier(refillRateLimiter, stringSupplier);
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String semaphoreBasedPermission() {
        return semaphoreGuardedSupplier.get();
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String atomicPermission() {
        return atomicGuardedSupplier.get();
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String refillPermission() {
        return refillGuardedSupplier.get();
    }

}