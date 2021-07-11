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

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.Tuple;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class HedgeRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<Hedge> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    public void testCreateWithCustomConfig() {
        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredMetrics(Duration.ofMillis(500))
            .build();

        HedgeRegistry hedgeRegistry = HedgeRegistry.of(config);

        HedgeConfig hedgeConfig = hedgeRegistry.getDefaultConfig();
        assertThat(hedgeConfig).isSameAs(config);
    }

    @Test
    public void shouldInitRegistryTags() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        HedgeRegistry registry = HedgeRegistry.of(hedgeConfigs, new NoOpHedgeEventConsumer(), io.vavr.collection.HashMap.of("Tag1Key", "Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Tuple.of("Tag1Key", "Tag1Value"));
    }

    @Test
    public void noTagsByDefault() {
        Hedge Hedge = HedgeRegistry.ofDefaults()
            .hedge("testName");
        assertThat(Hedge.getTags()).hasSize(0);
    }

    @Test
    public void tagsOfRegistryAddedToInstance() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        io.vavr.collection.Map<String, String> hedgeTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(hedgeConfigs, hedgeTags);
        Hedge Hedge = hedgeRegistry.hedge("testName");

        assertThat(Hedge.getTags()).hasSameElementsAs(hedgeTags);
    }

    @Test
    public void tagsAddedToInstance() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.ofDefaults();
        io.vavr.collection.Map<String, String> hedgeTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        Hedge Hedge = hedgeRegistry
            .hedge("testName", hedgeTags);

        assertThat(Hedge.getTags()).hasSameElementsAs(hedgeTags);
    }

    @Test
    public void tagsOfHedgesShouldNotBeMixed() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.ofDefaults();
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        io.vavr.collection.Map<String, String> hedgeTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        Hedge Hedge = hedgeRegistry
            .hedge("testName", hedgeConfig, hedgeTags);
        io.vavr.collection.Map<String, String> hedgeTags2 = io.vavr.collection.HashMap
            .of("key3", "value3", "key4", "value4");
        Hedge Hedge2 = hedgeRegistry
            .hedge("otherTestName", hedgeConfig, hedgeTags2);

        assertThat(Hedge.getTags()).hasSameElementsAs(hedgeTags);
        assertThat(Hedge2.getTags()).hasSameElementsAs(hedgeTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        io.vavr.collection.Map<String, String> hedgeTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key4", "value4");
        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(hedgeConfigs, hedgeTags);
        Hedge hedge = hedgeRegistry
            .hedge("testName", hedgeConfig, instanceTags);

        io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(hedge.getTags()).hasSameElementsAs(expectedTags);
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("default", HedgeConfig.ofDefaults());
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry.of(configs);

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry.of(configs);

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(HedgeConfig.ofDefaults(), new NoOpHedgeEventConsumer());

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<Hedge>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpHedgeEventConsumer());
        registryEventConsumers.add(new NoOpHedgeEventConsumer());

        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(HedgeConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(configs, new NoOpHedgeEventConsumer());

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        List<RegistryEventConsumer<Hedge>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpHedgeEventConsumer());
        registryEventConsumers.add(new NoOpHedgeEventConsumer());

        HedgeRegistry hedgeRegistry = HedgeRegistry
            .of(configs, registryEventConsumers);

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testAddConfiguration() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.ofDefaults();
        hedgeRegistry.addConfiguration("custom", HedgeConfig.custom().build());

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.ofDefaults();

        assertThatThrownBy(() -> hedgeRegistry.hedge("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    private static class NoOpHedgeEventConsumer implements RegistryEventConsumer<Hedge> {

        @Override
        public void onEntryAddedEvent(@NonNull EntryAddedEvent<Hedge> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<Hedge> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<Hedge> entryReplacedEvent) {
        }
    }
}
