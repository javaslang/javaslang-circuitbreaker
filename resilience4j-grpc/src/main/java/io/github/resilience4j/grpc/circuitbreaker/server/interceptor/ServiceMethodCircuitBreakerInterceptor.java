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

public class ServiceMethodCircuitBreakerInterceptor implements ServerInterceptor {

    private final MethodDescriptor<?, ?> methodDescriptor;
    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServiceMethodCircuitBreakerInterceptor(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.methodDescriptor = methodDescriptor;
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static ServiceMethodCircuitBreakerInterceptor of(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        return new ServiceMethodCircuitBreakerInterceptor(methodDescriptor, circuitBreaker, successStatusPredicate);
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

        if (call.getMethodDescriptor().getFullMethodName().equals(methodDescriptor.getFullMethodName())) {
            acquirePermissionOrThrowStatus();
            ServerCall<ReqT, RespT> callToExecute = ServerCallCircuitBreaker.decorate(call, circuitBreaker, successStatusPredicate);
            return new ServerCircuitBreakerCallListener<>(next.startCall(callToExecute, headers), circuitBreaker);
        }

        return next.startCall(call, headers);
    }

}