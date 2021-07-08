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

import com.google.common.util.concurrent.ListenableFuture;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ServerCircuitBreakerInterceptorsTests {

    @Rule
    public GrpcServerRule serverRule = new GrpcServerRule();

    private BindableService service = new SimpleServiceGrpc.SimpleServiceImplBase() {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            Status status = Status.Code.valueOf(request.getRequestMessage()).toStatus();

            if (status.isOk()) {
                responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("response: " + request.getRequestMessage())
                    .build());
                responseObserver.onCompleted();
            } else if (status.equals(Status.CANCELLED)) {
                // purposely caused the response to hang so that the client can force cancel the call
            } else {
                responseObserver.onError(status.asRuntimeException());
            }
        }
    };

    @Test
    public void decorateSuccessServiceCall() {
        CircuitBreaker methodCircuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");
        CircuitBreaker serviceCircuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

        Predicate<Status> isValidStatus = status ->
            status.isOk() || status.getCode() == Status.INVALID_ARGUMENT.getCode();

        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(serviceCircuitBreaker, isValidStatus)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodCircuitBreaker,
                    isValidStatus)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        try {
            // Service CB: Record success
            // Method CB: Record success
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.INVALID_ARGUMENT.name())
                .build());
        } catch (StatusRuntimeException ignored) {
        }

        // Service CB: Record success
        // Method CB: Record success
        stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        // Assert the expected calls to the method level circuit breaker
        final CircuitBreaker.Metrics methodMetrics = methodCircuitBreaker.getMetrics();
        assertThat(methodMetrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(methodMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfBufferedCalls()).isEqualTo(2);

        // Assert the expected calls to the service level circuit breaker
        final CircuitBreaker.Metrics serviceMetrics = serviceCircuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    public void decorateFailedServiceCall() {
        CircuitBreaker methodCircuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");
        CircuitBreaker serviceCircuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(serviceCircuitBreaker)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodCircuitBreaker,
                    status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        try {
            // Service CB: Record error
            // Method CB: Record error
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.INVALID_ARGUMENT.name())
                .build());
        } catch (StatusRuntimeException ignored) {
        }

        // Service CB: Record success
        // Method CB: Record success
        stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        try {
            // Service CB: Record error
            // Method CB: Record success
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.FAILED_PRECONDITION.name())
                .build());
        } catch (StatusRuntimeException ignored) {
        }

        // Assert the expected calls to the method level circuit breaker
        final CircuitBreaker.Metrics methodMetrics = methodCircuitBreaker.getMetrics();
        assertThat(methodMetrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(methodMetrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(methodMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfBufferedCalls()).isEqualTo(3);

        // Assert the expected calls to the service level circuit breaker
        final CircuitBreaker.Metrics serviceMetrics = serviceCircuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(3);
    }

    @Test
    public void recordFailureOnNotPermittedServiceCalls() {
        CircuitBreaker methodCircuitBreaker = CircuitBreaker.ofDefaults("testMethodCircuitBreaker");
        CircuitBreaker serviceCircuitBreaker = CircuitBreaker.ofDefaults("testServiceCircuitBreaker");

        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(serviceCircuitBreaker)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodCircuitBreaker,
                    status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        try {
            serviceCircuitBreaker.transitionToOpenState();

            // Service CB: Record not permitted
            // Method CB: Record not invoked
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.OK.name())
                .build());
            fail("circuit breaker should prevent success");
        } catch (StatusRuntimeException ignored) {
        }

        // Assert the expected calls to the method level circuit breaker
        final CircuitBreaker.Metrics methodMetrics = methodCircuitBreaker.getMetrics();
        assertThat(methodMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfBufferedCalls()).isEqualTo(0);

        // Assert the expected calls to the service level circuit breaker
        final CircuitBreaker.Metrics serviceMetrics = serviceCircuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(1);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(0);
    }

    @Test
    public void recordFailureOnNotPermittedMethodCalls() {
        CircuitBreaker methodCircuitBreaker = CircuitBreaker.ofDefaults("testMethodCircuitBreaker");
        CircuitBreaker serviceCircuitBreaker = CircuitBreaker.ofDefaults("testServiceCircuitBreaker");

        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(serviceCircuitBreaker)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodCircuitBreaker,
                    status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        try {
            methodCircuitBreaker.transitionToOpenState();

            // Service CB: Record not invoked
            // Method CB: Record not permitted
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.OK.name())
                .build());
            fail("circuit breaker should prevent success");
        } catch (StatusRuntimeException ignored) {
        }

        // Assert the expected calls to the method level circuit breaker
        final CircuitBreaker.Metrics methodMetrics = methodCircuitBreaker.getMetrics();
        assertThat(methodMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfNotPermittedCalls()).isEqualTo(1);
        assertThat(methodMetrics.getNumberOfBufferedCalls()).isEqualTo(0);

        // Assert the expected calls to the service level circuit breaker
        final CircuitBreaker.Metrics serviceMetrics = serviceCircuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldNotRecordFailureWhenServiceCallCancelled() throws InterruptedException {
        CircuitBreaker methodCircuitBreaker = CircuitBreaker.ofDefaults("testMethodCircuitBreaker");
        CircuitBreaker serviceCircuitBreaker = CircuitBreaker.ofDefaults("testServiceCircuitBreaker");

        serverRule.getServiceRegistry().addService(
            ServerCircuitBreakerInterceptors.decoratorFor(service)
                .interceptAll(serviceCircuitBreaker)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodCircuitBreaker,
                    status -> status.getCode() != Status.INVALID_ARGUMENT.getCode())
                .build()
        );

        SimpleServiceGrpc.SimpleServiceFutureStub stub = SimpleServiceGrpc
            .newFutureStub(serverRule.getChannel());

        // Service CB: Record cancelled
        // Method CB: Record cancelled
        ListenableFuture<SimpleResponse> future = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.CANCELLED.name())
            .build());

        Thread.sleep(10);
        future.cancel(true);

        // Assert the expected calls to the method level circuit breaker
        final CircuitBreaker.Metrics methodMetrics = methodCircuitBreaker.getMetrics();
        assertThat(methodMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(methodMetrics.getNumberOfBufferedCalls()).isEqualTo(0);

        // Assert the expected calls to the service level circuit breaker
        final CircuitBreaker.Metrics serviceMetrics = serviceCircuitBreaker.getMetrics();
        assertThat(serviceMetrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(serviceMetrics.getNumberOfBufferedCalls()).isEqualTo(0);
    }
}
