/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.micrometer.BulkheadMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-metrics.
 */
@Configuration
@ConditionalOnClass(MetricsAutoConfiguration.class)
@AutoConfigureAfter(value = {BulkheadMetricsAutoConfiguration.class, MetricsAutoConfiguration.class})
public class BulkheadMetricsAutoConfiguration {
    @Bean
    @ConditionalOnProperty(value = "resilience4j.bulkhead.metrics.enabled", matchIfMissing = true)
    public BulkheadMetrics registerBulkheadMetrics(BulkheadRegistry bulkheadRegistry){
        return BulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry);
    }
}
