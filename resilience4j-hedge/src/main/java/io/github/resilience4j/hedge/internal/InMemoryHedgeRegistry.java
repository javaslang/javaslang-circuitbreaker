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

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Backend Hedge manager. Constructs backend Hedges according to configuration values.
 */
public class InMemoryHedgeRegistry extends
    AbstractRegistry<Hedge, HedgeConfig> implements HedgeRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryHedgeRegistry() {
        this(HedgeConfig.ofDefaults(), HashMap.empty());
    }

    public InMemoryHedgeRegistry(io.vavr.collection.Map<String, String> tags) {
        this(HedgeConfig.ofDefaults(), tags);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()));
        this.configurations.putAll(configs);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs,
        RegistryEventConsumer<Hedge> registryEventConsumer) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()),
            registryEventConsumer);
        this.configurations.putAll(configs);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs,
        RegistryEventConsumer<Hedge> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()), registryEventConsumer,
            tags);
        this.configurations.putAll(configs);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()),
            registryEventConsumers);
        this.configurations.putAll(configs);
    }

    public InMemoryHedgeRegistry(Map<String, HedgeConfig> configs,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, HedgeConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryHedgeRegistry(HedgeConfig defaultConfig) {
        super(defaultConfig);
    }

    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
        RegistryEventConsumer<Hedge> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
        RegistryEventConsumer<Hedge> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Seq<Hedge> getAllHedges() {
        return Array.ofAll(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hedge hedge(final String name) {
        return hedge(name, getDefaultConfig(), HashMap.empty());
    }

    @Override
    public Hedge hedge(String name,
        io.vavr.collection.Map<String, String> tags) {
        return hedge(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hedge hedge(final String name, final HedgeConfig config) {
        return hedge(name, config, HashMap.empty());
    }

    @Override
    public Hedge hedge(String name,
        HedgeConfig hedgeConfig,
        io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> Hedge.of(name,
            Objects.requireNonNull(hedgeConfig, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hedge hedge(final String name,
        final Supplier<HedgeConfig> hedgeConfigSupplier) {
        return hedge(name, hedgeConfigSupplier, HashMap.empty());
    }

    @Override
    public Hedge hedge(String name,
        Supplier<HedgeConfig> hedgeConfigSupplier,
        io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> Hedge.of(name, Objects.requireNonNull(
            Objects.requireNonNull(hedgeConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hedge hedge(String name, String configName) {
        return hedge(name, configName, HashMap.empty());
    }

    @Override
    public Hedge hedge(String name, String configName,
        io.vavr.collection.Map<String, String> tags) {
        HedgeConfig config = getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return hedge(name, config, tags);
    }
}
