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
package io.github.resilience4j.grpc.circuitbreaker.client.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Status;
import io.grpc.stub.AbstractStub;

import java.util.function.Predicate;

public class ClientCircuitBreakerInterceptors {

    private ClientCircuitBreakerInterceptors() {
    }

    public static <S extends AbstractStub<S>> S decorate(S stub) {
        return stub.withInterceptors(new ClientDynamicCircuitBreakerInterceptor());
    }

    public static <S extends AbstractStub<S>> S decorate(S stub, CircuitBreaker circuitBreaker) {
        return decorate(stub, circuitBreaker, Status::isOk);
    }

    public static <S extends AbstractStub<S>> S decorate(
        S stub, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        return stub.withInterceptors(ClientCircuitBreakerInterceptor.of(circuitBreaker, successStatusPredicate));
    }

    public static Channel intercept(Channel channel) {
        return ClientInterceptors.intercept(channel, new ClientDynamicCircuitBreakerInterceptor());
    }

    public static Channel intercept(Channel channel, CircuitBreaker circuitBreaker) {
        return intercept(channel, circuitBreaker, Status::isOk);
    }

    public static Channel intercept(
        Channel channel, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        return ClientInterceptors.intercept(
            channel, ClientCircuitBreakerInterceptor.of(circuitBreaker, successStatusPredicate));
    }
}
