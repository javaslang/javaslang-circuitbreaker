/*
 *  Copyright 2019 Marco Ferrer
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
 */
package io.github.resilience4j.grpc.circuitbreaker.server.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class ServerCircuitBreakerDecorator {

    protected final List<ServerInterceptor> interceptors = new ArrayList<>();

    public ServerCircuitBreakerDecorator interceptMethod(
        MethodDescriptor<?, ?> methodDescriptor, CircuitBreaker circuitBreaker) {
        return interceptMethod(methodDescriptor, circuitBreaker, Status::isOk);
    }

    public ServerCircuitBreakerDecorator interceptMethod(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker,
        Predicate<Status> successStatusPredicate) {
        interceptors.add(ServerCircuitBreakerInterceptors
            .forMethod(methodDescriptor, circuitBreaker, successStatusPredicate));

        return this;
    }

    public ServerCircuitBreakerDecorator interceptAll(CircuitBreaker circuitBreaker) {
        return interceptAll(circuitBreaker, Status::isOk);
    }

    public ServerCircuitBreakerDecorator interceptAll(
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        interceptors.add(ServerCircuitBreakerInterceptors.from(circuitBreaker, successStatusPredicate));
        return this;
    }

    abstract public ServerServiceDefinition build();
}
