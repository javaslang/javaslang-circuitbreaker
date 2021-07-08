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
import io.github.resilience4j.grpc.circuitbreaker.server.ServerCallCircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.server.ServerCircuitBreakerCallListener;
import io.grpc.*;

import java.util.function.Predicate;

public class ServerCircuitBreakerInterceptor implements ServerInterceptor {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServerCircuitBreakerInterceptor(
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static ServerCircuitBreakerInterceptor of(
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        return new ServerCircuitBreakerInterceptor(circuitBreaker, successStatusPredicate);
    }

    private void acquirePermissionOrThrowStatus() {
        try {
            circuitBreaker.acquirePermission();
        } catch (Exception exception) {
            throw Status.UNAVAILABLE
                .withDescription(exception.getMessage())
                .withCause(exception)
                .asRuntimeException();
        }
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        ServerCall<ReqT, RespT> callToExecute = ServerCallCircuitBreaker.decorate(
            call, circuitBreaker, successStatusPredicate);

        acquirePermissionOrThrowStatus();

        return new ServerCircuitBreakerCallListener<>(next.startCall(callToExecute, headers), circuitBreaker);
    }
}