package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

public class ThreadPoolBulkheadMetricNames {
    private static final String DEFAULT_PREFIX = "resilience4j.bulkhead";

    public static final String DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME =
        DEFAULT_PREFIX + ".queue.depth";
    public static final String DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME =
        DEFAULT_PREFIX + ".queue.capacity";
    public static final String DEFAULT_THREAD_POOL_SIZE_METRIC_NAME =
        DEFAULT_PREFIX + ".thread.pool.size";
    public static final String DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME =
        DEFAULT_PREFIX + ".max.thread.pool.size";
    public static final String DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME =
        DEFAULT_PREFIX + ".core.thread.pool.size";
    public static final String DEFAULT_BULKHEAD_ACTIVE_THREAD_COUNT_METRIC_NAME =
        DEFAULT_PREFIX + ".active.thread.count";
    private String queueDepthMetricName = DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME;
    private String threadPoolSizeMetricName = DEFAULT_THREAD_POOL_SIZE_METRIC_NAME;
    private String maxThreadPoolSizeMetricName = DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME;
    private String coreThreadPoolSizeMetricName = DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME;
    private String queueCapacityMetricName = DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME;
    private String activeThreadCountMetricName = DEFAULT_BULKHEAD_ACTIVE_THREAD_COUNT_METRIC_NAME;

    protected ThreadPoolBulkheadMetricNames() {
    }

    /**
     * Returns a builder for creating custom metric names. Note that names have default values,
     * so only desired metrics can be renamed.
     *
     * @return The builder.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns default metric names.
     *
     * @return The default {@link ThreadPoolBulkheadMetricNames} instance.
     */
    public static ThreadPoolBulkheadMetricNames ofDefaults() {
        return new ThreadPoolBulkheadMetricNames();
    }

    /**
     * Returns the metric name for queue depth, defaults to {@value
     * DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME}.
     *
     * @return The queue depth metric name.
     */
    public String getQueueDepthMetricName() {
        return queueDepthMetricName;
    }

    /**
     * Returns the metric name for thread pool size, defaults to {@value
     * DEFAULT_THREAD_POOL_SIZE_METRIC_NAME}.
     *
     * @return The thread pool size metric name.
     */
    public String getThreadPoolSizeMetricName() {
        return threadPoolSizeMetricName;
    }

    /**
     * Returns the metric name for max thread pool size, defaults to {@value
     * DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME}.
     *
     * @return The max thread pool size metric name.
     */
    public String getMaxThreadPoolSizeMetricName() {
        return maxThreadPoolSizeMetricName;
    }

    /**
     * Returns the metric name for core thread pool size, defaults to {@value
     * DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME}.
     *
     * @return The core thread pool size metric name.
     */
    public String getCoreThreadPoolSizeMetricName() {
        return coreThreadPoolSizeMetricName;
    }

    /**
     * Returns the metric name for queue capacity, defaults to {@value
     * DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME}.
     *
     * @return The queue capacity metric name.
     */
    public String getQueueCapacityMetricName() {
        return queueCapacityMetricName;
    }

    /**
     * Returns the metric name for bulkhead active count, defaults to {@value
     * DEFAULT_BULKHEAD_ACTIVE_THREAD_COUNT_METRIC_NAME}.
     *
     * @return The active thread count metric name.
     */
    public String getActiveThreadCountMetricName() {
        return activeThreadCountMetricName;
    }

    /**
     * Helps building custom instance of {@link ThreadPoolBulkheadMetricNames}.
     */
    public static class Builder {

        private final ThreadPoolBulkheadMetricNames metricNames = new ThreadPoolBulkheadMetricNames();

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME}
         * with a given one.
         *
         * @param queueDepthMetricName The queue depth metric name.
         * @return The builder.
         */
        public Builder queueDepthMetricName(String queueDepthMetricName) {
            metricNames.queueDepthMetricName = requireNonNull(queueDepthMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_THREAD_POOL_SIZE_METRIC_NAME}
         * with a given one.
         *
         * @param threadPoolSizeMetricName The thread pool size metric name.
         * @return The builder.
         */
        public Builder threadPoolSizeMetricName(String threadPoolSizeMetricName) {
            metricNames.threadPoolSizeMetricName =  requireNonNull(threadPoolSizeMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME}
         * with a given one.
         *
         * @param maxThreadPoolSizeMetricName The max thread pool size metric name.
         * @return The builder.
         */
        public Builder maxThreadPoolSizeMetricName(String maxThreadPoolSizeMetricName) {
            metricNames.maxThreadPoolSizeMetricName = requireNonNull(
                maxThreadPoolSizeMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME}
         * with a given one.
         *
         * @param coreThreadPoolSizeMetricName The core thread pool size metric name.
         * @return The builder.
         */
        public Builder coreThreadPoolSizeMetricName(String coreThreadPoolSizeMetricName) {
            metricNames.coreThreadPoolSizeMetricName = requireNonNull(
                coreThreadPoolSizeMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME}
         * with a given one.
         *
         * @param queueCapacityMetricName The queue capacity metric name.
         * @return The builder.
         */
        public Builder queueCapacityMetricName(String queueCapacityMetricName) {
            metricNames.queueCapacityMetricName = requireNonNull(queueCapacityMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value ThreadPoolBulkheadMetricNames#DEFAULT_BULKHEAD_ACTIVE_THREAD_COUNT_METRIC_NAME}
         * with a given one.
         *
         * @param activeThreadCountMetricName The active thread count metric name.
         * @return The builder.
         */
        public Builder activeThreadCountMetricName(
            String activeThreadCountMetricName) {
            metricNames.activeThreadCountMetricName = requireNonNull(
                activeThreadCountMetricName);
            return this;
        }

        /**
         * Builds {@link ThreadPoolBulkheadMetricNames} instance.
         *
         * @return The built {@link ThreadPoolBulkheadMetricNames} instance.
         */
        public ThreadPoolBulkheadMetricNames build() {
            return metricNames;
        }
    }
}
