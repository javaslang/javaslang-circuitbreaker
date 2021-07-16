/*
 * Copyright 2019 Ingyu Hwang, Mahmoud Romeh
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

package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class AbstractThreadPoolBulkheadMetrics extends AbstractMetrics {

    protected final ThreadPoolBulkheadMetricNames names;

    protected AbstractThreadPoolBulkheadMetrics(ThreadPoolBulkheadMetricNames names) {
        this.names = requireNonNull(names);
    }

    @Deprecated
    protected AbstractThreadPoolBulkheadMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, ThreadPoolBulkhead bulkhead) {
        List<Tag> customTags = mapToTagsList(bulkhead.getTags().toJavaMap());
        registerMetrics(meterRegistry, bulkhead, customTags);
    }

    private void registerMetrics(
        MeterRegistry meterRegistry, ThreadPoolBulkhead bulkhead, List<Tag> customTags) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, bulkhead.getName());

        Set<Meter.Id> idSet = new HashSet<>();
        idSet.add(Gauge.builder(names.getQueueDepthMetricName(), bulkhead,
            bh -> bh.getMetrics().getQueueDepth())
            .description("The queue depth")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getThreadPoolSizeMetricName(), bulkhead,
            bh -> bh.getMetrics().getThreadPoolSize())
            .description("The thread pool size")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        idSet.add(Gauge.builder(names.getQueueCapacityMetricName(), bulkhead,
            bh -> bh.getMetrics().getQueueCapacity())
            .description("The queue capacity")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        idSet.add(Gauge.builder(names.getMaxThreadPoolSizeMetricName(), bulkhead,
            bh -> bh.getMetrics().getMaximumThreadPoolSize())
            .description("The maximum thread pool size")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        idSet.add(Gauge.builder(names.getCoreThreadPoolSizeMetricName(), bulkhead,
            bh -> bh.getMetrics().getCoreThreadPoolSize())
            .description("The core thread pool size")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        idSet.add(Gauge.builder(names.getActiveThreadCountMetricName(), bulkhead,
            bh -> bh.getMetrics().getActiveThreadCount())
            .description("The number of active threads")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        meterIdMap.put(bulkhead.getName(), idSet);
    }

    @Deprecated
    public static class MetricNames extends ThreadPoolBulkheadMetricNames {
    }
}
