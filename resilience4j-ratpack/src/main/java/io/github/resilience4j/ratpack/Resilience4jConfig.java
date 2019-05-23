/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.ratpack;

import io.github.resilience4j.ratpack.bulkhead.BulkheadConfigurationProperties;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratpack.retry.RetryConfigurationProperties;
import ratpack.func.Function;

import static ratpack.util.Exceptions.uncheck;

public class Resilience4jConfig {
    private CircuitBreakerConfigurationProperties circuitBreaker = new CircuitBreakerConfigurationProperties();
    private RateLimiterConfigurationProperties rateLimiter = new RateLimiterConfigurationProperties();
    private RetryConfigurationProperties retry = new RetryConfigurationProperties();
    private BulkheadConfigurationProperties bulkhead = new BulkheadConfigurationProperties();
    private boolean metrics = false;
    private boolean prometheus = false;
    private EndpointsConfig endpoints = new EndpointsConfig();

    public Resilience4jConfig circuitBreaker(String name) {
        return circuitBreaker(name, config -> config);
    }

    public Resilience4jConfig circuitBreaker(String name, Function<? super CircuitBreakerConfigurationProperties.BackendConfig, ? extends CircuitBreakerConfigurationProperties.BackendConfig> configure) {
        try {
            CircuitBreakerConfigurationProperties.BackendConfig finalConfig = configure.apply(new CircuitBreakerConfigurationProperties.BackendConfig());
            circuitBreaker.getBackends().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig rateLimiter(String name) {
        return rateLimiter(name, config -> config);
    }

    public Resilience4jConfig rateLimiter(String name, Function<? super RateLimiterConfigurationProperties.BackendConfig, ? extends RateLimiterConfigurationProperties.BackendConfig> configure) {
        try {
            RateLimiterConfigurationProperties.BackendConfig finalConfig = configure.apply(new RateLimiterConfigurationProperties.BackendConfig());
            rateLimiter.getBackends().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig retry(String name) {
        return retry(name, config -> config);
    }

    public Resilience4jConfig retry(String name, Function<? super RetryConfigurationProperties.BackendConfig, ? extends RetryConfigurationProperties.BackendConfig> configure) {
        try {
            RetryConfigurationProperties.BackendConfig finalConfig = configure.apply(new RetryConfigurationProperties.BackendConfig());
            retry.getBackends().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig bulkhead(String name) {
        return bulkhead(name, config -> config);
    }

    public Resilience4jConfig bulkhead(String name, Function<? super BulkheadConfigurationProperties.BackendConfig, ? extends BulkheadConfigurationProperties.BackendConfig> configure) {
        try {
            BulkheadConfigurationProperties.BackendConfig finalConfig = configure.apply(new BulkheadConfigurationProperties.BackendConfig());
            bulkhead.getBackends().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig metrics(boolean metrics) {
        this.metrics = metrics;
        return this;
    }

    public Resilience4jConfig prometheus(boolean prometheus) {
        this.prometheus = prometheus;
        return this;
    }

    public Resilience4jConfig endpoints(Function<? super EndpointsConfig, ? extends EndpointsConfig> configure) {
        try {
            endpoints = configure.apply(new EndpointsConfig());
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public CircuitBreakerConfigurationProperties getCircuitBreaker() {
        return circuitBreaker;
    }

    public RateLimiterConfigurationProperties getRateLimiter() {
        return rateLimiter;
    }

    public RetryConfigurationProperties getRetry() {
        return retry;
    }

    public BulkheadConfigurationProperties getBulkhead() {
        return bulkhead;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public boolean isPrometheus() {
        return prometheus;
    }

    public EndpointsConfig getEndpoints() {
        return endpoints;
    }
}
