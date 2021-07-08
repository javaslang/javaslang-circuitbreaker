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
import io.grpc.*;

public class ServiceMethodRateLimiterInterceptor implements ServerInterceptor {

    private final MethodDescriptor<?, ?> methodDescriptor;
    private final RateLimiter rateLimiter;

    private ServiceMethodRateLimiterInterceptor(MethodDescriptor<?, ?> methodDescriptor, RateLimiter rateLimiter) {
        this.methodDescriptor = methodDescriptor;
        this.rateLimiter = rateLimiter;
    }

    public static ServiceMethodRateLimiterInterceptor of(MethodDescriptor<?, ?> methodDescriptor,
                                                         RateLimiter rateLimiter) {
        return new ServiceMethodRateLimiterInterceptor(methodDescriptor, rateLimiter);
    }

    private void acquirePermissionOrThrowStatus() {
        try {
            RateLimiter.waitForPermission(rateLimiter);
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
        }

        return next.startCall(call, headers);
    }

}