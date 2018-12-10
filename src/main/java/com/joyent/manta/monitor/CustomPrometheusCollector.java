/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.common.collect.ImmutableList;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class that collects JMX metrics from {@link JMXMetricsCollector} and presents them in Prometheus exposition format.
 */
public class CustomPrometheusCollector extends Collector {
    private final JMXMetricsCollector jmxMetricsCollector;
    private final Map<String, AtomicLong> clientStats;

    @Inject
    CustomPrometheusCollector(@Named("JMXMetricsCollector") final JMXMetricsCollector jmxMetricsCollector,
                              @Named("SharedStats") final Map<String, AtomicLong> clientStats) {
        this.jmxMetricsCollector = jmxMetricsCollector;
        this.clientStats = clientStats;
    }

    private <T extends Number> T retrieveMBeanAttributeValue(final String mBeanObjectName,
                                                             final String attribute,
                                                             final Class<T> returnType) {
        return jmxMetricsCollector.getMBeanAttributeValue(mBeanObjectName, attribute, returnType);
    }

    private void importRequestsPut(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily("requests_put_mean",
                "Put Requests Mean Value",
                retrieveMBeanAttributeValue("requests-put", "Mean", Double.class))));
        builder.add((new GaugeMetricFamily("requests_put_count",
                "Put Requests Count",
                retrieveMBeanAttributeValue("requests-put", "Count", Long.class))));
        builder.add((new GaugeMetricFamily("requests_put_50thPercentile",
                "Put Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-put", "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_put_75thPercentile",
                "Put Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-put", "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_put_95thPercentile",
                "Put Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-put", "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_put_99thPercentile",
                "Put Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-put", "99thPercentile", Double.class))));
    }

    private void importRequestsGet(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily("requests_put_mean",
                "Get Requests Mean Value",
                retrieveMBeanAttributeValue("requests-get", "Mean", Double.class))));
        builder.add((new GaugeMetricFamily("requests_get_50thPercentile",
                "Get Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-get", "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_get_75thPercentile",
                "Get Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-get", "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_get_95thPercentile",
                "Get Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-get", "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_get_99thPercentile",
                "Get Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-get", "99thPercentile", Double.class))));
        builder.add((new CounterMetricFamily("requests_get_count",
                "Get Requests Count",
                retrieveMBeanAttributeValue("requests-get", "Count", Long.class))));
    }

    private void importRequestDelete(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily("requests_delete_mean",
                "Delete Requests Mean Value",
                retrieveMBeanAttributeValue("requests-delete", "Mean", Double.class))));
        builder.add((new GaugeMetricFamily("requests_delete_50thPercentile",
                "Delete Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete", "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_delete_75thPercentile",
                "Delete Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete", "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_delete_95thPercentile",
                "Delete Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete", "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily("requests_delete_99thPercentile",
                "Delete Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete", "99thPercentile", Double.class))));
        builder.add((new CounterMetricFamily("requests_delete_count",
                "Delete Requests Count",
                retrieveMBeanAttributeValue("requests-delete", "Count", Long.class))));
    }

    private void importRetries(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily("retry",
                "Number of Retries",
                retrieveMBeanAttributeValue("retries", "Count", Long.class))));
        builder.add((new GaugeMetricFamily("retries_mean_rate",
                "Mean rate for number of retries",
                retrieveMBeanAttributeValue("retries", "MeanRate", Double.class))));
    }

    private void addElapsedTimeMetric(final ImmutableList.Builder<MetricFamilySamples> builder, final Double value) {
        builder.add((new GaugeMetricFamily("manta_monitor_operation_chain_elapsed_time",
                "Total time in milliseconds to complete one chain of operations",
                value)));
    }

    public List<MetricFamilySamples> collect() {
        final ImmutableList.Builder<MetricFamilySamples> metricFamilySamplesBuilder =
                ImmutableList.builder();

        if (jmxMetricsCollector.validateMbeanObject("requests-put")) {
            importRequestsPut(metricFamilySamplesBuilder);
        }
        if (jmxMetricsCollector.validateMbeanObject("requests-get")) {
            importRequestsGet(metricFamilySamplesBuilder);
        }
        if (jmxMetricsCollector.validateMbeanObject("requests-delete")) {
            importRequestDelete(metricFamilySamplesBuilder);
        }
        if (jmxMetricsCollector.validateMbeanObject("retries")) {
            importRetries(metricFamilySamplesBuilder);
        }
        if (!clientStats.isEmpty()) {
            clientStats.forEach((key, value) -> {
                addElapsedTimeMetric(metricFamilySamplesBuilder, value.doubleValue());
            });
        }
        return metricFamilySamplesBuilder.build();
    }
}
