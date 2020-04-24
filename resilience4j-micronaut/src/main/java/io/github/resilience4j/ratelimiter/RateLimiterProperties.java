/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Map;

/**
 * Configuration for generic rate limiting options.
 *
 * @author James Kleeh
 * @since 1.0.0
 */
@ConfigurationProperties("resilience4j.ratelimiter")
public class RateLimiterProperties extends io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, InstanceProperties> configs){
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String,InstanceProperties> instances) {
        this.getInstances().putAll(instances);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}