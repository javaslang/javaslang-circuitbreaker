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
package io.github.resilience4j.grpc.ratelimiter.server.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ServerRateLimiterInterceptorTests {

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
            } else {
                responseObserver.onError(status.asRuntimeException());
            }
        }
    };

    @Test
    public void decorateSuccessServiceCall() {
        RateLimiter methodRateLimiter = RateLimiter.of("testRateLimiter", createConfig(10));
        RateLimiter serviceRateLimiter = RateLimiter.of("testRateLimiter", createConfig(10));

        serverRule.getServiceRegistry().addService(
            ServerRateLimiterInterceptors.decoratorFor(service)
                .interceptAll(serviceRateLimiter)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodRateLimiter)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
            .newBlockingStub(serverRule.getChannel());

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: OK");
    }

    @Test
    public void decorateFailedServiceCall() {
        RateLimiter serviceRateLimiter = RateLimiter.of("testRateLimiter", createConfig(1));

        serverRule.getServiceRegistry().addService(
            ServerRateLimiterInterceptors.decoratorFor(service)
                .interceptAll(serviceRateLimiter)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(serverRule.getChannel());

        stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        try {
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.OK.name())
                .build());
            fail("Request should not have been allowed");
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
            assertThat(e.getStatus().getDescription()).isEqualTo("RateLimiter 'testRateLimiter' does not permit further calls");
        }
    }

    @Test
    public void decorateFailedMethodCall() {
        RateLimiter methodRateLimiter = RateLimiter.of("testRateLimiter", createConfig(1));

        serverRule.getServiceRegistry().addService(
            ServerRateLimiterInterceptors.decoratorFor(service)
                .interceptMethod(
                    SimpleServiceGrpc.getUnaryRpcMethod(),
                    methodRateLimiter)
                .build()
        );

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(serverRule.getChannel());

        stub.unaryRpc(SimpleRequest.newBuilder()
            .setRequestMessage(Status.Code.OK.name())
            .build());

        try {
            stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage(Status.Code.OK.name())
                .build());
            fail("Request should not have been allowed");
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.UNAVAILABLE.getCode());
            assertThat(e.getStatus().getDescription()).isEqualTo("RateLimiter 'testRateLimiter' does not permit further calls");
        }
    }

    private RateLimiterConfig createConfig(int limitForPeriod) {
        return RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(50))
            .limitRefreshPeriod(Duration.ofMillis(5000))
            .limitForPeriod(limitForPeriod)
            .build();
    }
}
