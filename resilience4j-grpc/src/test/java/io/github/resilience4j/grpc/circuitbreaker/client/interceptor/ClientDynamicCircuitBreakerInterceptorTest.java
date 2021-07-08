/*
 *  Copyright 2019 Marco Ferrer, Ken Dombeck
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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ClientDynamicCircuitBreakerInterceptorTest {

    @Rule
    public GrpcServerRule serverRule = new GrpcServerRule();

    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

    @Before
    public void setup() {
        serverRule.getServiceRegistry().addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
            @Override
            public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                Status status = Status.Code.valueOf(request.getRequestMessage()).toStatus();

                if (status.isOk()) {
                    responseObserver.onNext(SimpleResponse.newBuilder()
                        .setResponseMessage("response: " + request.getRequestMessage())
                        .build());
                    responseObserver.onCompleted();
                } else {
                    responseObserver.onError(status.asRuntimeException());
                }
            }
        });
    }

    @Test
    public void interceptSuccessfulCall() {
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(ClientCircuitBreakerInterceptors.intercept(serverRule.getChannel()))
            .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker)
            .withOption(CircuitBreakerCallOptions.SUCCESS_STATUS,
                status -> status.getCode() != Status.INVALID_ARGUMENT.getCode());

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }

    @Test
    public void interceptCallWithDefaultSuccessStatusPredicate() {
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(ClientCircuitBreakerInterceptors.intercept(serverRule.getChannel()))
            .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker);

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }

    @Test
    public void interceptFailedCall() {
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(ClientCircuitBreakerInterceptors.intercept(serverRule.getChannel()))
            .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker)
            .withOption(CircuitBreakerCallOptions.SUCCESS_STATUS,
                status -> status.getCode() != Status.INVALID_ARGUMENT.getCode());

        try {
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.INVALID_ARGUMENT.name())
                .build());
            fail("Should have thrown an exception");
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus()).isEqualTo(Status.INVALID_ARGUMENT);
        }

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }

    @Test
    public void interceptWithOutSettingProperOptions() {
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(ClientCircuitBreakerInterceptors.intercept(serverRule.getChannel()));

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
    }

    @Test
    public void decorateStubSuccessfulCall() {
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        SimpleResponse response = ClientCircuitBreakerInterceptors.decorate(stub)
            .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker)
            .withOption(CircuitBreakerCallOptions.SUCCESS_STATUS,
                status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
            .unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.OK.name())
                .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }
}
