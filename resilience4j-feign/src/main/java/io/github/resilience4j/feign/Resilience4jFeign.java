/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import feign.Feign;
import feign.InvocationHandlerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Main class for combining feign with Resilience4j.
 *
 * <pre>
 * {@code
 *     MyService myService = Resilience4jFeign.builder(decorators).target(MyService.class, "http://localhost:8080/");
 * }
 * </pre>
 * <p>
 * {@link Resilience4jFeign} works in the same way as the standard {@link Feign.Builder}. Only
 * {@link Feign.Builder#invocationHandlerFactory(InvocationHandlerFactory)} may not be called as
 * this is how {@link Resilience4jFeign} decorates the feign interface. <br> See {@link
 * FeignDecorators} on how to build decorators and enhance your feign interfaces.
 */
public final class Resilience4jFeign {

    public static Builder builder(FeignDecorator invocationDecorator) {
        return new Builder(invocationDecorator);
    }

    @Deprecated
    public static Feign.Builder builder1(
        Function<Supplier<Object>, Supplier<CompletionStage<Object>>> completionStageWrapper) {
        return new PostponedBuilder(completionStageWrapper);
    }


    public static <T> Feign.Builder builder(
        PostponedDecorators<T> completionStageWrapper) {
        return new PostponedBuilder<T>(completionStageWrapper);
    }

    public static final class Builder extends Feign.Builder {

        private final FeignDecorator invocationDecorator;

        public Builder(FeignDecorator invocationDecorator) {
            this.invocationDecorator = invocationDecorator;
        }

        /**
         * Will throw an {@link UnsupportedOperationException} exception.
         */
        @Override
        public Feign.Builder invocationHandlerFactory(
            InvocationHandlerFactory invocationHandlerFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Feign build() {
            super.invocationHandlerFactory(
                (target, dispatch) -> new DecoratorInvocationHandler(target, dispatch,
                    invocationDecorator));
            return super.build();
        }

    }

    public static final class PostponedBuilder<T> extends Feign.Builder {

        private final Function<Supplier<Object>, Supplier<CompletionStage<Object>>> completionStageWrapper;
        private final List<FeignDecorator> fallbacks;

        @Deprecated
        public PostponedBuilder(
            Function<Supplier<Object>, Supplier<CompletionStage<Object>>> completionStageWrapper) {
            this.completionStageWrapper = completionStageWrapper;
            this.fallbacks = Collections.emptyList();
        }

        public PostponedBuilder(
            PostponedDecorators<T> completionStageWrapper) {
            this.completionStageWrapper = completionStageWrapper::build;
            this.fallbacks = completionStageWrapper.getFallbacks();
        }

        /**
         * Will throw an {@link UnsupportedOperationException} exception.
         */
        @Override
        public Feign.Builder invocationHandlerFactory(
            InvocationHandlerFactory invocationHandlerFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Feign build() {
            FeignDecorators fallback = new FeignDecorators(fallbacks);
            super.invocationHandlerFactory((target, dispatch) ->
                new DecoratorPostponedInvocationHandler<>(
                    target, dispatch, completionStageWrapper, fallback));
            return super.build();
        }

    }
}
