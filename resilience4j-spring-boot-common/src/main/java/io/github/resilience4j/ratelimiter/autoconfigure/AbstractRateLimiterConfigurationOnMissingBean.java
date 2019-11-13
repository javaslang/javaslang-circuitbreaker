/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.*;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

import java.util.List;
import java.util.Optional;

@Configuration
@Import(FallbackConfigurationOnMissingBean.class)
public abstract class AbstractRateLimiterConfigurationOnMissingBean {

    protected final RateLimiterConfiguration rateLimiterConfiguration;

    public AbstractRateLimiterConfigurationOnMissingBean() {
        this.rateLimiterConfiguration = new RateLimiterConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(
        RateLimiterConfigurationProperties rateLimiterProperties,
        EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
        RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer) {
        return rateLimiterConfiguration.rateLimiterRegistry(
            rateLimiterProperties, rateLimiterEventsConsumerRegistry,
            rateLimiterRegistryEventConsumer);

    }

    @Bean
    @Primary
    public RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<RateLimiter>>> optionalRegistryEventConsumers) {
        return rateLimiterConfiguration
            .rateLimiterRegistryEventConsumer(optionalRegistryEventConsumers);

    }

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RateLimiterAspectHelper rateLimiterAspectHelper(
        RateLimiterRegistry rateLimiterRegistry,
        @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList,
        FallbackDecorators fallbackDecorators) {
        return rateLimiterConfiguration.rateLimiterAspectHelper(
            rateLimiterRegistry, rateLimiterAspectExtList, fallbackDecorators);
    }

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RateLimiterAspect rateLimiterAspect(
        RateLimiterAspectHelper rateLimiterAspectHelper,
        RateLimiterConfigurationProperties rateLimiterProperties) {
        return rateLimiterConfiguration.rateLimiterAspect(
            rateLimiterAspectHelper, rateLimiterProperties);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2RateLimiterAspectExt rxJava2RateLimiterAspectExt() {
        return rateLimiterConfiguration.rxJava2RateLimiterAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorRateLimiterAspectExt reactorRateLimiterAspectExt() {
        return rateLimiterConfiguration.reactorRateLimiterAspectExt();
    }

}
