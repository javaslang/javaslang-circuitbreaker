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

import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.collection.Map;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

class InMemoryRegistry<T extends RateLimiter,E extends RateLimiterConfig> extends AbstractRegistry<T, E> {

    InMemoryRegistry(E defaultConfig) {
        super(defaultConfig);
    }

    InMemoryRegistry(E defaultConfig, Map<String, String> registryTags) {
        super(defaultConfig, registryTags);
    }

    InMemoryRegistry(E defaultConfig, RegistryEventConsumer<T> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    InMemoryRegistry(E defaultConfig, RegistryEventConsumer<T> registryEventConsumer, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    InMemoryRegistry(E defaultConfig, List<RegistryEventConsumer<T>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    InMemoryRegistry(E defaultConfig, List<RegistryEventConsumer<T>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    InMemoryRegistry(E defaultConfig, List<RegistryEventConsumer<T>> registryEventConsumers, Map<String, String> tags, RegistryStore<T> registryStore) {
        super(defaultConfig, registryEventConsumers, tags, registryStore);
    }

    void putAllConfigurations(java.util.Map<String, E> configs) {
        configurations.putAll(configs);

    }

    io.vavr.collection.Map<String, String> allTags(io.vavr.collection.Map<String, String> tags) {
        return getAllTags(tags);
    }

    T computeIfAbsentFacade(String name, Supplier<T> supplier) {
        return computeIfAbsent(name, supplier);
    }

    <X> X configMustNotBeNull(X config) {
        return Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
    }

    <X> X supplierMustNotBetNull(X config) {
        return Objects.requireNonNull(config,SUPPLIER_MUST_NOT_BE_NULL);
    }

    Collection<T> allRateLimiters() {
        return entryMap.values();
    }
}