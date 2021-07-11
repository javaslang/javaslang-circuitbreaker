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
package io.github.resilience4j.hedge.metrics;

import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.HedgeEvent;

import java.time.Duration;

//this class is dangerous if we dont have some % limit that we don't go over in hedging - like %.
//but that means that it can't be as easy as hoped for because will have to track everything anyway.
public class PreconfiguredCutoffMetrics implements HedgeMetrics {

    private final Duration cutoff;

    public PreconfiguredCutoffMetrics(Duration cutoff) {
        this.cutoff = cutoff;
    }

    @Override
    public Duration getResponseTimeCutoff() {
        return cutoff;
    }

    @Override
    public boolean requiresInput() {
        return false;
    }

    @Override
    public void accept(HedgeEvent event) {
        //do nothing here - we ignore events in this case.
    }
}
