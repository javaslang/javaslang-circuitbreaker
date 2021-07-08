/*
 *  Copyright 2021 Marco Ferrer, Ken Dombeck
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
package io.github.resilience4j.grpc.circuitbreaker.server;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.server.interceptor.ServerCircuitBreakerInterceptors;
import io.grpc.BindableService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ServerCallCircuitBreakerTest {

    @Rule
    public GrpcServerRule serverRule = new GrpcServerRule();

    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

    private BindableService service = new SimpleServiceGrpc.SimpleServiceImplBase() {
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
    };

    @Test
    public void interceptSuccessServiceCall() {
        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(circuitBreaker,
                    status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics serviceMetrics = circuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }

    @Test
    public void interceptCallWithDefaultSuccessStatusPredicate() {
        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(circuitBreaker)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");

        CircuitBreaker.Metrics serviceMetrics = circuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }

    @Test
    public void interceptFailedServiceCall() {
        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(circuitBreaker)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        try {
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.INVALID_ARGUMENT.name())
                .build());
            fail("Should have thrown an exception");
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus()).isEqualTo(Status.INVALID_ARGUMENT);
        }

        CircuitBreaker.Metrics serviceMetrics = circuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(1);
    }
}
