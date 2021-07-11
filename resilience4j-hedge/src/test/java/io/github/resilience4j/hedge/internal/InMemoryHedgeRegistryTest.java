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

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.function.Supplier;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class InMemoryHedgeRegistryTest {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private HedgeConfig config;

    @Before
    public void init() {
        config = HedgeConfig.ofDefaults();
    }

    @Test
    public void hedgePositive() {
        HedgeRegistry registry = HedgeRegistry.of(config);

        Hedge firstHedge = registry.hedge("test");
        Hedge anotherLimit = registry.hedge("test1");
        Hedge sameAsFirst = registry.hedge("test");

        then(firstHedge).isEqualTo(sameAsFirst);
        then(firstHedge).isNotEqualTo(anotherLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void hedgePositiveWithSupplier() {
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);
        Supplier<HedgeConfig> hedgeConfigSupplier = mock(Supplier.class);
        given(hedgeConfigSupplier.get()).willReturn(config);

        Hedge firstHedge = registry.hedge("test", hedgeConfigSupplier);
        verify(hedgeConfigSupplier, times(1)).get();
        Hedge sameAsFirst = registry.hedge("test", hedgeConfigSupplier);
        verify(hedgeConfigSupplier, times(1)).get();
        Hedge anotherLimit = registry.hedge("test1", hedgeConfigSupplier);
        verify(hedgeConfigSupplier, times(2)).get();

        then(firstHedge).isEqualTo(sameAsFirst);
        then(firstHedge).isNotEqualTo(anotherLimit);
    }

    @Test
    public void hedgeConfigIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);

        new InMemoryHedgeRegistry((HedgeConfig) null);
    }

    @Test
    public void hedgeNewWithNullName() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge(null);
    }

    @Test
    public void hedgeNewWithNullNonDefaultConfig() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge("name", (HedgeConfig) null);
    }

    @Test
    public void hedgeNewWithNullNameAndNonDefaultConfig() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge(null, config);
    }

    @Test
    public void hedgeNewWithNullNameAndConfigSupplier() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge(null, () -> config);
    }

    @Test
    public void hedgeNewWithNullConfigSupplier() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Supplier must not be null");
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge("name", (Supplier<HedgeConfig>) null);
    }

    @Test
    public void hedgeGetAllHedges() {
        HedgeRegistry registry = new InMemoryHedgeRegistry(config);

        registry.hedge("foo");

        then(registry.getAllHedges().size()).isEqualTo(1);
        then(registry.getAllHedges().get(0).getName()).isEqualTo("foo");
    }

}