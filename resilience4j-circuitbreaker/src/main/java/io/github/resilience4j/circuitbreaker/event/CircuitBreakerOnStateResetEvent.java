/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.event;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * A CircuitBreakerEvent which informs about a reset.
 */
public class CircuitBreakerOnStateResetEvent extends AbstractCircuitBreakerEvent{

    private CircuitBreaker.StateTransition stateTransition;

    public CircuitBreakerOnStateResetEvent(String circuitBreakerName, CircuitBreaker.StateTransition stateTransition) {
        super(circuitBreakerName);
        this.stateTransition = stateTransition;
    }

    public CircuitBreaker.StateTransition getStateTransition() {
        return stateTransition;
    }

    @Override
    public Type getEventType() {
        return Type.STATE_RESET;
    }

    @Override
    public String toString(){
        return String.format("%s: CircuitBreaker '%s' reset state from %s to %s",
                getCreationTime(),
                getCircuitBreakerName(),
                getStateTransition().getFromState(),
                getStateTransition().getToState());

    }
}
