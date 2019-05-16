/*
 * Copyright 2019 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.internal.disposables.EmptyDisposable;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

class MaybeRateLimiter<T> extends Maybe<T> {

    private final Maybe<T> upstream;
    private final RateLimiter rateLimiter;

    MaybeRateLimiter(Maybe<T> upstream, RateLimiter rateLimiter) {
        this.upstream = upstream;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> downstream) {
        if(rateLimiter.acquirePermission(Duration.ZERO)){
            upstream.subscribe(new RateLimiterMaybeObserver(downstream));
        }else{
            downstream.onSubscribe(EmptyDisposable.INSTANCE);
            downstream.onError(new RequestNotPermitted(rateLimiter));
        }
    }

    class RateLimiterMaybeObserver extends BaseRateLimiterObserver implements MaybeObserver<T> {

        private final MaybeObserver<? super T> downstreamObserver;

        RateLimiterMaybeObserver(MaybeObserver<? super T> childObserver) {
            super(rateLimiter);
            this.downstreamObserver = requireNonNull(childObserver);
        }

        @Override
        protected void hookOnSubscribe() {
            downstreamObserver.onSubscribe(this);
        }

        @Override
        public void onSuccess(T value) {
            whenNotCompleted(() -> {
                super.onSuccess();
                downstreamObserver.onSuccess(value);
            });
        }

        @Override
        public void onError(Throwable e) {
            whenNotCompleted(() -> {
                super.onError(e);
                downstreamObserver.onError(e);
            });
        }

        @Override
        public void onComplete() {
            whenNotCompleted(() -> {
                super.onSuccess();
                downstreamObserver.onComplete();
            });
        }
    }
}
