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

import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry;

import java.util.List;
import java.util.Map;

/**
 * Due to the {@link RateLimiterRegistry} creating RateLimiters when non existing
 * a factory method will create the equivalent RateLimiter.
 * @param <C>
 */
interface RateLimiterRegistryFactory<C extends RateLimiterConfig> {

    RateLimiterRegistry create(Map<String, C> configs,
                               List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                               Map<String, String> tags, RegistryStore<RateLimiter> registryStore);

    static RateLimiterRegistryFactory<RateLimiterConfig> defaultConfig() {
        return InMemoryRateLimiterRegistry::create;
    }

}