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
import io.github.resilience4j.grpc.circuitbreaker.CircuitBreakerCallOptions;
import io.github.resilience4j.grpc.circuitbreaker.client.ClientCallCircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public class ClientDynamicCircuitBreakerInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        CircuitBreaker circuitBreaker = callOptions.getOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER);

        if (circuitBreaker != null) {
            return ClientCallCircuitBreaker.decorate(
                next.newCall(method, callOptions), circuitBreaker,
                callOptions.getOption(CircuitBreakerCallOptions.SUCCESS_STATUS));
        } else {
            return next.newCall(method, callOptions);
        }
    }
}
