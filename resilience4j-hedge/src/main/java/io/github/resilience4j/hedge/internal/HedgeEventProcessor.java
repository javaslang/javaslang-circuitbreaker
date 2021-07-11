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


import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.event.*;

public class HedgeEventProcessor extends EventProcessor<HedgeEvent> implements
    EventConsumer<HedgeEvent>, Hedge.EventPublisher {

    @Override
    public void consumeEvent(@NonNull HedgeEvent event) {
        super.processEvent(event);
    }

    @Override
    public Hedge.EventPublisher onPrimarySuccess(
        EventConsumer<PrimarySuccessEvent> onSuccessEventConsumer) {
        registerConsumer(PrimarySuccessEvent.class.getName(), onSuccessEventConsumer);
        return this;
    }

    @Override
    public Hedge.EventPublisher onPrimaryFailure(
        EventConsumer<PrimaryFailureEvent> onFailureEventConsumer) {
        registerConsumer(PrimaryFailureEvent.class.getName(), onFailureEventConsumer);
        return this;
    }

    @Override
    public Hedge.EventPublisher onHedgeSuccess(
        EventConsumer<HedgeSuccessEvent> onHedgeSuccessEventConsumer) {
        registerConsumer(HedgeSuccessEvent.class.getName(), onHedgeSuccessEventConsumer);
        return this;
    }

    @Override
    public Hedge.EventPublisher onHedgeFailure(
        EventConsumer<HedgeFailureEvent> onOnFailureEventConsumer) {
        registerConsumer(HedgeFailureEvent.class.getName(), onOnFailureEventConsumer);
        return this;
    }
}