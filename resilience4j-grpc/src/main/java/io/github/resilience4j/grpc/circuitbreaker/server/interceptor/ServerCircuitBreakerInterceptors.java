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
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;

import java.util.function.Predicate;

public class ServerCircuitBreakerInterceptors {

    private ServerCircuitBreakerInterceptors() {
    }

    public static ServerCircuitBreakerInterceptor from(CircuitBreaker circuitBreaker) {
        return from(circuitBreaker, Status::isOk);
    }

    public static ServerCircuitBreakerInterceptor from(
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        return ServerCircuitBreakerInterceptor.of(circuitBreaker, successStatusPredicate);
    }

    public static ServiceMethodCircuitBreakerInterceptor forMethod(
        MethodDescriptor<?, ?> methodDescriptor, CircuitBreaker circuitBreaker) {
        return forMethod(methodDescriptor, circuitBreaker, Status::isOk);
    }

    public static ServiceMethodCircuitBreakerInterceptor forMethod(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker,
        Predicate<Status> successStatusPredicate) {
        return ServiceMethodCircuitBreakerInterceptor.of(methodDescriptor, circuitBreaker, successStatusPredicate);
    }

    public static ServerCircuitBreakerDecorator decoratorFor(ServerServiceDefinition serviceDef) {
        return new ServerServiceDefinitionDecorator(serviceDef);
    }

    public static ServerCircuitBreakerDecorator decoratorFor(BindableService bindableService) {
        return new BindableServiceDecorator(bindableService);
    }

}
