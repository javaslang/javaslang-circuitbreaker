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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.BDDAssertions.then;

public class HedgeConfigTest {

    private static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    private static final String HEDGE_TO_STRING = "HedgeConfig{false,0,true,100,null}";

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void builderTimeoutIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(HEDGE_DURATION_MUST_NOT_BE_NULL);

        HedgeConfig.Builder.fromConfig(HedgeConfig.ofDefaults())
            .preconfiguredMetrics(null);
    }

    @Test
    public void configToString() {
        then(HedgeConfig.ofDefaults().toString()).isEqualTo(HEDGE_TO_STRING);
    }

}
