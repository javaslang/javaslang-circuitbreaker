/*
 *  Copyright 2021 Ken Dombeck
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
package io.github.resilience4j.grpc.ratelimiter.client.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.stub.AbstractStub;

public class ClientRateLimiterInterceptors {

    private ClientRateLimiterInterceptors() {
    }

    public static <S extends AbstractStub<S>> S decorate(S stub, RateLimiter rateLimiter) {
        return stub.withInterceptors(ClientRateLimiterInterceptor.of(rateLimiter));
    }

    public static Channel intercept(Channel channel, RateLimiter rateLimiter) {
        return ClientInterceptors.intercept(channel, ClientRateLimiterInterceptor.of(rateLimiter));
    }
}
