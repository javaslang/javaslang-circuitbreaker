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

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry;
import io.vavr.collection.Seq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all RateLimiter instances.
 */
public interface RateLimiterRegistry extends Registry<RateLimiter, RateLimiterConfig> {

    /**
     * Creates a RateLimiterRegistry with a custom RateLimiter configuration.
     *
     * @param defaultRateLimiterConfig a custom RateLimiter configuration
     * @return a RateLimiterRegistry instance backed by a custom RateLimiter configuration
     */
    static RateLimiterRegistry of(RateLimiterConfig defaultRateLimiterConfig) {
        return InMemoryRateLimiterRegistry.create(defaultRateLimiterConfig);
    }

    static RateLimiterRegistry of(RefillRateLimiterConfig defaultRateLimiterConfig) {
        return InMemoryRateLimiterRegistry.create(defaultRateLimiterConfig);
    }

    /**
     * Creates a RateLimiterRegistry with a custom default RateLimiter configuration and a
     * RateLimiter registry event consumer.
     *
     * @param defaultRateLimiterConfig a custom default RateLimiter configuration.
     * @param registryEventConsumer    a RateLimiter registry event consumer.
     * @return a RateLimiterRegistry with a custom RateLimiter configuration and a RateLimiter
     * registry event consumer.
     */
    static RateLimiterRegistry of(RateLimiterConfig defaultRateLimiterConfig,
                                  RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        return InMemoryRateLimiterRegistry.create(defaultRateLimiterConfig, registryEventConsumer);
    }

    /**
     * Creates a RateLimiterRegistry with a custom default RateLimiter configuration and a list of
     * RateLimiter registry event consumers.
     *
     * @param defaultRateLimiterConfig a custom default RateLimiter configuration.
     * @param registryEventConsumers   a list of RateLimiter registry event consumers.
     * @return a RateLimiterRegistry with a custom RateLimiter configuration and a list of
     * RateLimiter registry event consumers.
     */
    static RateLimiterRegistry of(RateLimiterConfig defaultRateLimiterConfig,
                                  List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        return InMemoryRateLimiterRegistry.create(defaultRateLimiterConfig, registryEventConsumers);
    }

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a default RateLimiter
     * configuration.
     *
     * @return The {@link RateLimiterConfig}
     */
    static RateLimiterRegistry ofDefaults() {
        return InMemoryRateLimiterRegistry.create(RateLimiterConfig.ofDefaults());
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs) {
        return InMemoryRateLimiterRegistry.create(configs);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @param tags    default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs,
                                  io.vavr.collection.Map<String, String> tags) {
        return InMemoryRateLimiterRegistry.create(configs, tags);
    }

    /**
     * Creates a RateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     *
     * @param configs               a Map of shared RateLimiter configurations.
     * @param registryEventConsumer a RateLimiter registry event consumer.
     * @return a RateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs,
                                  RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        return InMemoryRateLimiterRegistry.create(configs, registryEventConsumer);
    }

    /**
     * Creates a RateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     *
     * @param configs               a Map of shared RateLimiter configurations.
     * @param registryEventConsumer a RateLimiter registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a RateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs,
                                  RegistryEventConsumer<RateLimiter> registryEventConsumer,
                                  io.vavr.collection.Map<String, String> tags) {
        return InMemoryRateLimiterRegistry.create(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a RateLimiterRegistry with a Map of shared RateLimiter configurations and a list of
     * RateLimiter registry event consumers.
     *
     * @param configs                a Map of shared RateLimiter configurations.
     * @param registryEventConsumers a list of RateLimiter registry event consumers.
     * @return a RateLimiterRegistry with a Map of shared RateLimiter configurations and a list of
     * RateLimiter registry event consumers.
     */
    static RateLimiterRegistry of(Map<String, RateLimiterConfig> configs,
                                  List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        return InMemoryRateLimiterRegistry.create(configs, registryEventConsumers);
    }

    /**
     * Returns all managed {@link RateLimiter} instances.
     *
     * @return all managed {@link RateLimiter} instances.
     */
    Seq<RateLimiter> getAllRateLimiters();

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the RateLimiter
     * @param tags tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RateLimiterConfig rateLimiterConfig);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @param tags              tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RateLimiterConfig rateLimiterConfig,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a custom
     * RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier);

    /**
     * Returns a managed {@link RateLimiterConfig} or creates a new one with a custom
     * RateLimiterConfig configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @param tags                      tags added to the RateLimiter
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a builder to create a custom RateLimiterRegistry.
     *
     * @return a {@link RateLimiterRegistry.Builder}
     */
    static Builder<RateLimiterConfig> custom() {
        return new Builder<>(RateLimiterRegistryFactory.defaultConfig());
    }

    static Builder<RefillRateLimiterConfig> customRefill() {
        return new Builder<>(RateLimiterRegistryFactory.refillConfig());
    }

    class Builder<C extends RateLimiterConfig> {

        private static final String DEFAULT_CONFIG = "default";
        private RateLimiterRegistryFactory<C> rateLimiterRegistryFactory;
        private RegistryStore registryStore;
        private Map<String, C> rateLimiterConfigsMap;
        private List<RegistryEventConsumer<RateLimiter>> registryEventConsumers;
        private io.vavr.collection.Map<String, String> tags;

        public Builder(RateLimiterRegistryFactory<C> rateLimiterRegistryFactory) {
            this.rateLimiterRegistryFactory = rateLimiterRegistryFactory;
            this.rateLimiterConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        public Builder<C> withRegistryStore(RegistryStore registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a RateLimiterRegistry with a custom default RateLimiter configuration.
         *
         * @param rateLimiterConfig a custom default RateLimiter configuration
         * @return a {@link RateLimiterRegistry.Builder}
         */
        public Builder<C> withRateLimiterConfig(C rateLimiterConfig) {
            rateLimiterConfigsMap.put(DEFAULT_CONFIG, rateLimiterConfig);
            return this;
        }

        /**
         * Configures a RateLimiterRegistry with a custom RateLimiter configuration.
         *
         * @param configName    configName for a custom shared RateLimiter configuration
         * @param configuration a custom shared RateLimiter configuration
         * @return a {@link RateLimiterRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder<C> addRateLimiterConfig(String configName, C configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            rateLimiterConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a RateLimiterRegistry with a RateLimiter registry event consumer.
         *
         * @param registryEventConsumer a RateLimiter registry event consumer.
         * @return a {@link RateLimiterRegistry.Builder}
         */
        public Builder<C> addRegistryEventConsumer(RegistryEventConsumer<RateLimiter> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a RateLimiterRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link RateLimiterRegistry.Builder}
         */
        public Builder<C> withTags(io.vavr.collection.Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a RateLimiterRegistry
         *
         * @return the RateLimiterRegistry
         */
        public RateLimiterRegistry build() {
            return rateLimiterRegistryFactory.create(rateLimiterConfigsMap, registryEventConsumers, tagsAsJavaMap(), registryStore);
        }

        private Map<String, String> tagsAsJavaMap() {
            if (tags != null) {
                return tags.toJavaMap();
            } else {
                return new HashMap<>();
            }
        }
    }
}
