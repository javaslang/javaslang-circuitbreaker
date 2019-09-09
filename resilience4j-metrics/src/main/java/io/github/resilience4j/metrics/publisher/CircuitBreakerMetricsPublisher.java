/*
 * Copyright 2019 Ingyu Hwang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class CircuitBreakerMetricsPublisher extends AbstractMetricsPublisher<CircuitBreaker> {

    private final String prefix;

    public CircuitBreakerMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public CircuitBreakerMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public CircuitBreakerMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(CircuitBreaker entry) {
        String name = entry.getName();

        //state as an integer
        String state = name(prefix, name, STATE);
        String successful = name(prefix, name, SUCCESSFUL);
        String failed = name(prefix, name, FAILED);
        String notPermitted = name(prefix, name, NOT_PERMITTED);
        String numberOfBufferedCalls = name(prefix, name, BUFFERED);
        String failureRate = name(prefix, name, FAILURE_RATE);

        metricRegistry.register(state, (Gauge<Integer>)()-> entry.getState().getOrder());
        metricRegistry.register(successful, (Gauge<Integer>) () -> entry.getMetrics().getNumberOfSuccessfulCalls());
        metricRegistry.register(failed, (Gauge<Integer>) () -> entry.getMetrics().getNumberOfFailedCalls());
        metricRegistry.register(notPermitted, (Gauge<Long>) () -> entry.getMetrics().getNumberOfNotPermittedCalls());
        metricRegistry.register(numberOfBufferedCalls, (Gauge<Integer>) () -> entry.getMetrics().getNumberOfBufferedCalls());
        metricRegistry.register(failureRate, (Gauge<Float>) () -> entry.getMetrics().getFailureRate());

        List<String> metricNames = Arrays.asList(state, successful, failed, notPermitted, numberOfBufferedCalls, failureRate);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(CircuitBreaker entry) {
        removeMetrics(entry.getName());
    }
}