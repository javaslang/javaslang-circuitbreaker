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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

@RunWith(PowerMockRunner.class)
@PrepareForTest(io.github.resilience4j.hedge.internal.HedgeImpl.class)
public class HedgeImplTest {

    private static final String NAME = "name";
    private HedgeConfig hedgeConfig;
    private Hedge hedge;

    @Before
    public void init() {
        hedgeConfig = HedgeConfig.custom()
            .preconfiguredMetrics(Duration.ZERO)
            .build();
        io.github.resilience4j.hedge.internal.HedgeImpl testTimeout = new io.github.resilience4j.hedge.internal.HedgeImpl("name", hedgeConfig);
        hedge = PowerMockito.spy(testTimeout);
    }

    @Test
    public void shouldSetGivenName() {
        Hedge hedge = Hedge.ofDefaults(NAME);
        assertThat(hedge.getName()).isEqualTo(NAME);
    }

    @Test
    public void configPropagation() {
        then(hedge.getHedgeConfig()).isEqualTo(hedgeConfig);
    }

    @Test
    public void namePropagation() {
        then(hedge.getName()).isEqualTo(NAME);
    }
}
