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
package io.github.resilience4j.hedge.event;

import java.time.Duration;
import java.time.ZonedDateTime;

public abstract class AbstractHedgeEvent implements HedgeEvent {

    private final String hedgeName;
    private final ZonedDateTime creationTime;
    private final Duration duration;
    private final Type eventType;

    AbstractHedgeEvent(String hedgeName, Type eventType, Duration duration) {
        this.hedgeName = hedgeName;
        this.eventType = eventType;
        this.creationTime = ZonedDateTime.now();
        this.duration = duration;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public String getHedgeName() {
        return hedgeName;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "HedgeEvent{" +
            "type=" + getEventType() +
            ", hedgeName='" + getHedgeName() + '\'' +
            ", creationTime=" + getCreationTime() +
            '}';
    }
}
