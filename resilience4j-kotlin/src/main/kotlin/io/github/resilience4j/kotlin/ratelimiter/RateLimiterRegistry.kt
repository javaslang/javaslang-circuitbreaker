/*
 *
 * Copyright 2020
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
@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.vavr.Tuple2 as VavrTuple2
import io.vavr.collection.HashMap as VavrHashMap

/**
 * Creates new custom [RateLimiterRegistry].
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     withRateLimiterConfig(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [RateLimiterRegistry.Builder] that customize resulting `RateLimiterRegistry`
 */
inline fun RateLimiterRegistry(
    config: RateLimiterRegistry.Builder<RateLimiterConfig>.() -> Unit
): RateLimiterRegistry {
    return RateLimiterRegistry.custom().apply(config).build()
}

/**
 * Configures a [RateLimiterRegistry] with a custom default RateLimiter configuration.
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     withRateLimiterConfig {
 *         limitRefreshPeriod(Duration.ofSeconds(10))
 *         limitForPeriod(10)
 *         timeoutDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param config methods of [RateLimiterConfig.Builder] that customize the default `RateLimiterConfig`
 */
inline fun RateLimiterRegistry.Builder<RateLimiterConfig>.withRateLimiterConfig(
    config: RateLimiterConfig.Builder.() -> Unit
) {
    withRateLimiterConfig(RateLimiterConfig(config))
}

/**
 * Configures a [RateLimiterRegistry] with a custom default RateLimiter configuration.
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     withRateLimiterConfig(baseRateLimiterConfig) {
 *         limitRefreshPeriod(Duration.ofSeconds(10))
 *         limitForPeriod(10)
 *         timeoutDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param baseConfig base `RateLimiterConfig`
 * @param config methods of [RateLimiterConfig.Builder] that customize the default `RateLimiterConfig`
 */
inline fun RateLimiterRegistry.Builder<RateLimiterConfig>.withRateLimiterConfig(
    baseConfig: RateLimiterConfig,
    config: RateLimiterConfig.Builder.() -> Unit
) {
    withRateLimiterConfig(RateLimiterConfig(baseConfig, config))
}

/**
 * Configures a [RateLimiterRegistry] with a custom RateLimiter configuration.
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     addRateLimiterConfig("sharedConfig1") {
 *         limitRefreshPeriod(Duration.ofSeconds(10))
 *         limitForPeriod(10)
 *         timeoutDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared RateLimiter configuration
 * @param config methods of [RateLimiterConfig.Builder] that customize resulting `RateLimiterConfig`
 */
inline fun RateLimiterRegistry.Builder<RateLimiterConfig>.addRateLimiterConfig(
    configName: String,
    config: RateLimiterConfig.Builder.() -> Unit
) {
    addRateLimiterConfig(configName, RateLimiterConfig(config))
}

/**
 * Configures a [RateLimiterRegistry] with a custom RateLimiter configuration.
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     addRateLimiterConfig("sharedConfig1", baseRateLimiterConfig) {
 *         limitRefreshPeriod(Duration.ofSeconds(10))
 *         limitForPeriod(10)
 *         timeoutDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared RateLimiter configuration
 * @param baseConfig base `RateLimiterConfig`
 * @param config methods of [RateLimiterConfig.Builder] that customize resulting `RateLimiterConfig`
 */
inline fun RateLimiterRegistry.Builder<RateLimiterConfig>.addRateLimiterConfig(
    configName: String,
    baseConfig: RateLimiterConfig,
    config: RateLimiterConfig.Builder.() -> Unit
) {
    addRateLimiterConfig(configName, RateLimiterConfig(baseConfig, config))
}

/**
 * Configures a [RateLimiterRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun RateLimiterRegistry.Builder<RateLimiterConfig>.withTags(tags: Map<String, String>) {
    withTags(VavrHashMap.ofAll(tags))
}

/**
 * Configures a [RateLimiterRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun RateLimiterRegistry.Builder<RateLimiterConfig>.withTags(vararg tags: Pair<String, String>) {
    withTags(VavrHashMap.ofEntries(tags.map { VavrTuple2(it.first, it.second) }))
}
