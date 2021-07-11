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
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.hedge.internal.InMemoryHedgeRegistry;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all Hedge instances.
 */
public interface HedgeRegistry extends Registry<Hedge, HedgeConfig> {

    /**
     * Creates a HedgeRegistry with a custom default Hedge configuration.
     *
     * @param defaultHedgeConfig a custom default Hedge configuration
     * @return a HedgeRegistry instance backed by a custom Hedge configuration
     */
    static HedgeRegistry of(HedgeConfig defaultHedgeConfig) {
        return new InMemoryHedgeRegistry(defaultHedgeConfig);
    }

    /**
     * Creates a HedgeRegistry with a custom default Hedge configuration and a
     * Hedge registry event consumer.
     *
     * @param defaultHedgeConfig a custom default Hedge configuration.
     * @param registryEventConsumer    a Hedge registry event consumer.
     * @return a HedgeRegistry with a custom Hedge configuration and a Hedge
     * registry event consumer.
     */
    static HedgeRegistry of(HedgeConfig defaultHedgeConfig,
        RegistryEventConsumer<Hedge> registryEventConsumer) {
        return new InMemoryHedgeRegistry(defaultHedgeConfig, registryEventConsumer);
    }

    /**
     * Creates a HedgeRegistry with a custom default Hedge configuration and a list of
     * Hedge registry event consumers.
     *
     * @param defaultHedgeConfig a custom default Hedge configuration.
     * @param registryEventConsumers   a list of Hedge registry event consumers.
     * @return a HedgeRegistry with a custom Hedge configuration and list of Hedge
     * registry event consumers.
     */
    static HedgeRegistry of(HedgeConfig defaultHedgeConfig,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers) {
        return new InMemoryHedgeRegistry(defaultHedgeConfig, registryEventConsumers);
    }

    /**
     * Returns a managed {@link HedgeConfig} or creates a new one with a default Hedge
     * configuration.
     *
     * @return The {@link HedgeConfig}
     */
    static HedgeRegistry ofDefaults() {
        return new InMemoryHedgeRegistry(HedgeConfig.ofDefaults());
    }

    /**
     * Creates a HedgeRegistry with a Map of shared Hedge configurations.
     *
     * @param configs a Map of shared Hedge configurations
     * @return a HedgeRegistry with a Map of shared Hedge configurations.
     */
    static HedgeRegistry of(Map<String, HedgeConfig> configs) {
        return new InMemoryHedgeRegistry(configs);
    }

    /**
     * Creates a HedgeRegistry with a Map of shared Hedge configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared Hedge configurations
     * @param tags    default tags to add to the registry
     * @return a HedgeRegistry with a Map of shared Hedge configurations.
     */
    static HedgeRegistry of(Map<String, HedgeConfig> configs,
        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryHedgeRegistry(configs, tags);
    }

    /**
     * Creates a HedgeRegistry with a Map of shared Hedge configurations and a
     * Hedge registry event consumer.
     *
     * @param configs               a Map of shared Hedge configurations.
     * @param registryEventConsumer a Hedge registry event consumer.
     * @return a HedgeRegistry with a Map of shared Hedge configurations and a
     * Hedge registry event consumer.
     */
    static HedgeRegistry of(Map<String, HedgeConfig> configs,
        RegistryEventConsumer<Hedge> registryEventConsumer) {
        return new InMemoryHedgeRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a HedgeRegistry with a Map of shared Hedge configurations and a
     * Hedge registry event consumer.
     *
     * @param configs               a Map of shared Hedge configurations.
     * @param registryEventConsumer a Hedge registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a HedgeRegistry with a Map of shared Hedge configurations and a
     * Hedge registry event consumer.
     */
    static HedgeRegistry of(Map<String, HedgeConfig> configs,
        RegistryEventConsumer<Hedge> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryHedgeRegistry(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a HedgeRegistry with a Map of shared Hedge configurations and a list of
     * Hedge registry event consumers.
     *
     * @param configs                a Map of shared Hedge configurations.
     * @param registryEventConsumers a list of Hedge registry event consumers.
     * @return a HedgeRegistry with a Map of shared Hedge configurations and a list of
     * Hedge registry event consumers.
     */
    static HedgeRegistry of(Map<String, HedgeConfig> configs,
        List<RegistryEventConsumer<Hedge>> registryEventConsumers) {
        return new InMemoryHedgeRegistry(configs, registryEventConsumers);
    }

    /**
     * Returns all managed {@link Hedge} instances.
     *
     * @return all managed {@link Hedge} instances.
     */
    Seq<Hedge> getAllHedges();

    /**
     * Returns a managed {@link Hedge} or creates a new one with the default Hedge
     * configuration.
     *
     * @param name the name of the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name);

    /**
     * Returns a managed {@link Hedge} or creates a new one with the default Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Hedge
     * @param tags tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     *
     * @param name              the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, HedgeConfig hedgeConfig);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name              the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @param tags              tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, HedgeConfig hedgeConfig,
        io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link HedgeConfig} or creates a new one with a custom
     * HedgeConfig configuration.
     *
     * @param name                      the name of the HedgeConfig
     * @param hedgeConfigSupplier a supplier of a custom HedgeConfig configuration
     * @return The {@link HedgeConfig}
     */
    Hedge hedge(String name, Supplier<HedgeConfig> hedgeConfigSupplier);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                      the name of the Hedge
     * @param hedgeConfigSupplier a supplier of a custom Hedge configuration
     * @param tags                      tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name,
        Supplier<HedgeConfig> hedgeConfigSupplier,
        io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link Hedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, String configName);

    /**
     * Returns a managed {@link Hedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, String configName,
        io.vavr.collection.Map<String, String> tags);

}
