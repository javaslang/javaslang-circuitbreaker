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
package io.github.resilience4j.grpc.ratelimiter.server.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;

import java.util.ArrayList;
import java.util.List;

public abstract class ServerRateLimiterDecorator {

    protected final List<ServerInterceptor> interceptors = new ArrayList<>();

    public ServerRateLimiterDecorator interceptMethod(MethodDescriptor<?, ?> methodDescriptor, RateLimiter rateLimiter) {
        interceptors.add(ServerRateLimiterInterceptors.forMethod(methodDescriptor, rateLimiter));
        return this;
    }

    public ServerRateLimiterDecorator interceptAll(RateLimiter rateLimiter) {
        interceptors.add(ServerRateLimiterInterceptors.from(rateLimiter));
        return this;
    }

    abstract public ServerServiceDefinition build();
}
