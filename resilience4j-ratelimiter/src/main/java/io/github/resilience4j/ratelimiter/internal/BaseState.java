/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
 *
 */
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;

/**
 * Package private class used only for inheritance purposes
 * @param <T>
 */
abstract class BaseState<T extends RateLimiterConfig> {

    private final T config;

    private final int activePermissions;
    private final long nanosToWait;
    private final long timeoutInNanos;

    BaseState(T config, int activePermissions, long nanosToWait) {
        this.config = config;
        this.activePermissions = activePermissions;
        this.nanosToWait = nanosToWait;
        this.timeoutInNanos = config.getTimeoutDuration().toNanos();
    }

    T getConfig() {
        return config;
    }

    int getActivePermissions() {
        return activePermissions;
    }

    long getNanosToWait() {
        return nanosToWait;
    }

    long getTimeoutInNanos() {
        return timeoutInNanos;
    }

    abstract <K extends BaseState<T>> K withConfig(T config);
}
