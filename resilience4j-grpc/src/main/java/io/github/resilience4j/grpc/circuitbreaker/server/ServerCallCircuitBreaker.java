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
package io.github.resilience4j.grpc.circuitbreaker.server;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ServerCallCircuitBreaker<ReqT, RespT>
    extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

    private final long startTime = System.nanoTime();
    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServerCallCircuitBreaker(
        ServerCall<ReqT, RespT> delegate,
        CircuitBreaker circuitBreaker,
        Predicate<Status> successStatusPredicate) {

        super(delegate);
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static <ReqT, RespT> ServerCallCircuitBreaker<ReqT, RespT> decorate(
        ServerCall<ReqT, RespT> call, CircuitBreaker circuitBreaker) {
        return decorate(call, circuitBreaker, Status::isOk);
    }

    public static <ReqT, RespT> ServerCallCircuitBreaker<ReqT, RespT> decorate(
        ServerCall<ReqT, RespT> call, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        return new ServerCallCircuitBreaker<>(call, circuitBreaker, successStatusPredicate);
    }

    @Override
    public void close(Status status, Metadata trailers) {
        if (successStatusPredicate.test(status)) {
            circuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        } else {
            circuitBreaker.onError(
                System.nanoTime() - startTime, TimeUnit.NANOSECONDS,
                status.asRuntimeException(trailers));
        }
        super.close(status, trailers);
    }
}
