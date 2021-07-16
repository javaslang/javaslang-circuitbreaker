/*
 * Copyright 2019 Ingyu Hwang
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
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByNamesTag;
import static io.github.resilience4j.micrometer.tagged.ThreadPoolBulkheadMetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedThreadPoolBulkheadMetricsPublisherTest {

    private static final List<String> EXPECTED_METERS = Arrays.asList(
        "custom.max.thread.pool.size",
        "custom.core.thread.pool.size",
        "resilience4j.bulkhead.queue.depth",
        "resilience4j.bulkhead.queue.capacity",
        "resilience4j.bulkhead.thread.pool.size",
        "resilience4j.bulkhead.active.thread.count"
    );
    private  static final int EXPECTED_METER_COUNT = EXPECTED_METERS.size();
    private MeterRegistry meterRegistry;
    private ThreadPoolBulkhead bulkhead;
    private ThreadPoolBulkheadRegistry bulkheadRegistry;
    private TaggedThreadPoolBulkheadMetricsPublisher taggedBulkheadMetricsPublisher;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        taggedBulkheadMetricsPublisher = new TaggedThreadPoolBulkheadMetricsPublisher(
            meterRegistry);
        bulkheadRegistry = ThreadPoolBulkheadRegistry
            .of(ThreadPoolBulkheadConfig.ofDefaults(), taggedBulkheadMetricsPublisher);
        bulkhead = bulkheadRegistry.bulkhead("backendA");

        // record some basic stats
        bulkhead.executeSupplier(() -> "Bla");
        bulkhead.executeSupplier(() -> "Bla");

    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRetry() {
        ThreadPoolBulkhead newBulkhead = bulkheadRegistry.bulkhead("backendB");

        assertThat(taggedBulkheadMetricsPublisher.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedBulkheadMetricsPublisher.meterIdMap.get("backendA")).hasSize(EXPECTED_METER_COUNT);
        assertThat(taggedBulkheadMetricsPublisher.meterIdMap.get("backendB")).hasSize(EXPECTED_METER_COUNT);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(EXPECTED_METER_COUNT * 2);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaximumThreadPoolSize());
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(EXPECTED_METER_COUNT);

        assertThat(taggedBulkheadMetricsPublisher.meterIdMap).containsKeys("backendA");
        bulkheadRegistry.remove("backendA");

        assertThat(taggedBulkheadMetricsPublisher.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, bulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(bulkhead.getMetrics().getMaximumThreadPoolSize());

        ThreadPoolBulkhead newBulkhead = ThreadPoolBulkhead
            .of(bulkhead.getName(), ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(Runtime.getRuntime().availableProcessors() + 1).build());

        bulkheadRegistry.replace(bulkhead.getName(), newBulkhead);

        gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME).gauges();

        successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaximumThreadPoolSize());
    }


    @Test
    public void maxThreadPoolSizeGaugeIsRegistered() {
        Gauge available = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(available).isNotNull();
        assertThat(available.value()).isEqualTo(bulkhead.getMetrics().getMaximumThreadPoolSize());
    }

    @Test
    public void coreThreadPoolSizeGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getCoreThreadPoolSize());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void queueCapacityGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getQueueCapacity());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void queueDepthGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getQueueDepth());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void threadPoolSizeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getThreadPoolSize());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void activeThreadCountIsRegistered() {
        Gauge activeCount = meterRegistry.get(DEFAULT_BULKHEAD_ACTIVE_THREAD_COUNT_METRIC_NAME).gauge();

        assertThat(activeCount).isNotNull();
        // `greaterThanOrEqualTo` prevents timing issue where active count drops
        // between getting the value from the gauge and
        // asserting it is equal to the current value
        assertThat(activeCount.value()).isGreaterThanOrEqualTo(bulkhead.getMetrics().getActiveThreadCount());
        assertThat(activeCount.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TaggedThreadPoolBulkheadMetricsPublisher taggedBulkheadMetricsPublisher =
            new TaggedThreadPoolBulkheadMetricsPublisher(
                ThreadPoolBulkheadMetricNames.custom()
                    .maxThreadPoolSizeMetricName("custom.max.thread.pool.size")
                    .coreThreadPoolSizeMetricName("custom.core.thread.pool.size")
                    .build(), meterRegistry);

        ThreadPoolBulkheadRegistry bulkheadRegistry = ThreadPoolBulkheadRegistry
            .of(ThreadPoolBulkheadConfig.ofDefaults(), taggedBulkheadMetricsPublisher);
        bulkhead = bulkheadRegistry.bulkhead("backendA");

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(EXPECTED_METERS);
    }

    @Test
    public void testReplaceNewMeter(){
        ThreadPoolBulkhead oldOne = ThreadPoolBulkhead.of("backendC", ThreadPoolBulkheadConfig.ofDefaults());
        // add meters of old
        taggedBulkheadMetricsPublisher.addMetrics(meterRegistry, oldOne);
        // one success call
        oldOne.executeSupplier(() -> "Bla");

        assertThat(taggedBulkheadMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedBulkheadMetricsPublisher.meterIdMap.get("backendC")).hasSize(EXPECTED_METER_COUNT);
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();
        Optional<Gauge> successful = findMeterByNamesTag(gauges, oldOne.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(oldOne.getMetrics().getMaximumThreadPoolSize());

        ThreadPoolBulkhead newOne = ThreadPoolBulkhead.of("backendC", ThreadPoolBulkheadConfig.ofDefaults());

        // add meters of new
        taggedBulkheadMetricsPublisher.addMetrics(meterRegistry, newOne);
        // three success call
        newOne.executeSupplier(() -> "Bla");
        newOne.executeSupplier(() -> "Bla");
        newOne.executeSupplier(() -> "Bla");

        assertThat(taggedBulkheadMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedBulkheadMetricsPublisher.meterIdMap.get("backendC")).hasSize(EXPECTED_METER_COUNT);
        gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();
        successful = findMeterByNamesTag(gauges, newOne.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newOne.getMetrics().getMaximumThreadPoolSize());
    }

}
