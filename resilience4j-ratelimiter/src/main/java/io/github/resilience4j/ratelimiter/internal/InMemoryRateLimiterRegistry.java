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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.*;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.github.resilience4j.ratelimiter.internal.ConfigConverter.defaultConverter;
import static io.github.resilience4j.ratelimiter.internal.ConfigConverter.refillConverter;
import static io.github.resilience4j.ratelimiter.internal.InMemoryFactory.atomicFactory;
import static io.github.resilience4j.ratelimiter.internal.InMemoryFactory.refillFactory;

/**
 * Backend RateLimiter manager. Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRateLimiterRegistry<E extends RateLimiterConfig> implements RateLimiterRegistry {

    private final InMemoryRegistry<RateLimiter,E> registry;
    private final ConfigConverter<E> configConverter;
    private final InMemoryFactory<E> inMemoryFactory;

    private static final String DEFAULT_CONFIG = "default";

    private InMemoryRateLimiterRegistry(InMemoryRegistry<RateLimiter, E> registry, ConfigConverter<E> configConverter, InMemoryFactory<E> inMemoryFactory) {
        this.registry = registry;
        this.configConverter = configConverter;
        this.inMemoryFactory = inMemoryFactory;
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs) {
        return create(configs, HashMap.empty());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
        io.vavr.collection.Map<String, String> tags) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), tags);
        registry.putAllConfigurations(configs);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }


    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        return create(configs, registryEventConsumer, HashMap.empty());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
        RegistryEventConsumer<RateLimiter> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), registryEventConsumer,tags);
        registry.putAllConfigurations(configs);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        return create(configs, registryEventConsumers, HashMap.empty());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
        io.vavr.collection.Map<String, String> tags) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), registryEventConsumers,tags);
        registry.putAllConfigurations(configs);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    /**
     * Factory method with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(RateLimiterConfig defaultConfig) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(defaultConfig);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    public static InMemoryRateLimiterRegistry<RefillRateLimiterConfig> create(RefillRateLimiterConfig defaultConfig) {
        InMemoryRegistry<RateLimiter, RefillRateLimiterConfig> registry =new InMemoryRegistry<>(defaultConfig);
        return new InMemoryRateLimiterRegistry<>(registry, refillConverter(), refillFactory());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(RateLimiterConfig defaultConfig,
        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(defaultConfig, registryEventConsumer);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(RateLimiterConfig defaultConfig,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(defaultConfig, registryEventConsumers);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    public static InMemoryRateLimiterRegistry<RefillRateLimiterConfig> createRefill(Map<String, RefillRateLimiterConfig> configs,
                                                                                    List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                                                                    Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        return createRefill(configs, registryEventConsumers, HashMap.ofAll(tags), registryStore);
    }

    public static InMemoryRateLimiterRegistry<RefillRateLimiterConfig> createRefill(Map<String, RefillRateLimiterConfig> configs,
                                                                              List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                                                              io.vavr.collection.Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        InMemoryRegistry<RateLimiter, RefillRateLimiterConfig> registry = new InMemoryRegistry<>(configs.getOrDefault(DEFAULT_CONFIG, RefillRateLimiterConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(HashMap.empty()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        registry.putAllConfigurations(configs);
        return new InMemoryRateLimiterRegistry(registry, refillConverter(), atomicFactory());
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
                                                                        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                                                        Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        return create(configs, registryEventConsumers, HashMap.ofAll(tags) ,registryStore);
    }

    public static InMemoryRateLimiterRegistry<RateLimiterConfig> create(Map<String, RateLimiterConfig> configs,
                                                                        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                                                        io.vavr.collection.Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        InMemoryRegistry<RateLimiter, RateLimiterConfig> registry = new InMemoryRegistry<>(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(HashMap.empty()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        registry.putAllConfigurations(configs);
        return new InMemoryRateLimiterRegistry<>(registry, defaultConverter(), atomicFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Seq<RateLimiter> getAllRateLimiters() {
        return Array.ofAll(registry.allRateLimiters());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name) {
        return rateLimiter(name, getDefaultConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags) {
        return rateLimiter(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name, final RateLimiterConfig config) {
        return rateLimiter(name, config, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, RateLimiterConfig config,
        io.vavr.collection.Map<String, String> tags) {
        E nonNullConfig = configConverter.from(registry.configMustNotBeNull(config));
        return registry.computeIfAbsentFacade(name, () -> inMemoryFactory.create(name, nonNullConfig, registry.allTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name,
        final Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
        return rateLimiter(name, rateLimiterConfigSupplier, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name,
        Supplier<RateLimiterConfig> rateLimiterConfigSupplier,
        io.vavr.collection.Map<String, String> tags) {
        return registry.computeIfAbsentFacade(name, () -> inMemoryFactory.create(name, configConverter.from(registry.configMustNotBeNull(registry.supplierMustNotBetNull(rateLimiterConfigSupplier).get())), registry.allTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName) {
        return rateLimiter(name, configName, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName,
        io.vavr.collection.Map<String, String> tags) {
        return registry.computeIfAbsentFacade(name, () -> inMemoryFactory.create(name,registry.getConfiguration(configName).orElseThrow(() -> new ConfigurationNotFoundException(configName)) , registry.allTags(tags)));
    }

    @Override
    public void addConfiguration(String configName, RateLimiterConfig configuration) {
        registry.addConfiguration(configName, configConverter.from(configuration));
    }

    @Override
    public Optional<RateLimiter> find(String name) {
        return registry.find(name);
    }

    @Override
    public Optional<RateLimiter> remove(String name) {
        return registry.remove(name);
    }

    /**
     * Replacing a rate Limiter should be considered stateless!!!!
     * @param name     the existing name
     * @param newEntry a new entry
     * @return
     */
    @Override
    public Optional<RateLimiter> replace(String name, RateLimiter newEntry) {
        return registry.replace(name, newEntry);
    }

    @Override
    public Optional<RateLimiterConfig> getConfiguration(String configName) {
        return registry.getConfiguration(configName).map(r -> r);
    }

    @Override
    public RateLimiterConfig getDefaultConfig() {
        return registry.getDefaultConfig();
    }

    @Override
    public io.vavr.collection.Map<String, String> getTags() {
        return registry.getTags();
    }

    @Override
    public EventPublisher<RateLimiter> getEventPublisher() {
        return registry.getEventPublisher();
    }

}
