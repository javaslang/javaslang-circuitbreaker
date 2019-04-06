/*
 * Copyright 2019 Kyuhyen Hwang
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
package io.github.resilience4j.recovery;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletionStageRecoveryApplier implements RecoveryApplier {

    @Override
    public boolean supports(Class target) {
        return CompletionStage.class.isAssignableFrom(target);
    }

    @Override
    public CheckedFunction1<CheckedFunction0<Object>, Object> get(String recoveryMethodName, Object[] args, Object target) {
        return (supplier) -> {
            CompletionStage completionStage = (CompletionStage) supplier.apply();

            CompletableFuture promise = new CompletableFuture();

            completionStage.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    try {
                        promise.complete(invoke(recoveryMethodName, args, (Throwable) throwable, target));
                    } catch (Throwable recoveryThrowable) {
                        promise.completeExceptionally(recoveryThrowable);
                    }
                } else {
                    promise.complete(result);
                }
            });

            return promise;
        };
    }
}
