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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class ConfigBuilderHelper {

    private static Map<String, BuilderRegistry> builderRegistry = new HashMap<>();

    static {
        builderRegistry.put(RateLimiterConfig.class.getName(), (BuilderRegistry<RateLimiterConfig.Builder, RateLimiterConfig>) RateLimiterConfig.Builder::new);
        builderRegistry.put(RefillRateLimiterConfig.class.getName(), (BuilderRegistry<RefillRateLimiterConfig.Builder, RefillRateLimiterConfig>) RefillRateLimiterConfig.Builder::new);
    }

    private ConfigBuilderHelper() {
    }

    static final <T extends RateLimiterConfig> T withTimeoutDuration(Duration duration, T config) {
        nonSupportedConfig(config);

        return (T) builderRegistry.get(config.getClass().getName()).create(config).timeoutDuration(duration).build();
    }

    static final <T extends RateLimiterConfig> T withLimitForPeriod(int limitForPeriod, T config) {
        nonSupportedConfig(config);

        return (T) builderRegistry.get(config.getClass().getName()).create(config).limitForPeriod(limitForPeriod).build();
    }

    private static <T extends RateLimiterConfig> void nonSupportedConfig(T config) {
        if (!builderRegistry.containsKey(config.getClass().getName())) {
            throw new NonRegisteredConfig(config.getClass());
        }
    }

    private interface BuilderRegistry<T extends RateLimiterConfig.Builder, E extends RateLimiterConfig> {
        T create(E config);
    }

    static class NonRegisteredConfig extends IllegalArgumentException {
        public NonRegisteredConfig(Class<?> clazz) {
            super(clazz.getName() + " not supported, please add a builder factory on the registry");
        }
    }

}
