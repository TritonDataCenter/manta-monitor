/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Class that collects JMX metrics from {@link JMXMetricsCollector} and presents
 * them in Prometheus exposition format.
 */
public class CustomPrometheusCollector extends Collector implements CustomPrometheusCollectorInterface {
    private static final String METRIC_PREFIX = "manta_monitor";
    private final JMXMetricsCollector jmxMetricsCollector;
    private final String testType;
    private final String name;

    @Inject
    CustomPrometheusCollector(@Named("JMXMetricsCollector") final JMXMetricsCollector jmxMetricsCollector,
                              @Assisted final String testType) {
        this.jmxMetricsCollector = jmxMetricsCollector;
        this.testType = testType;
        this.name = String.format("%s_%s_", METRIC_PREFIX, testType);
    }

    @Override
    public String getTestType() {
        return this.testType;
    }

    @Override
    public JMXMetricsCollector getJMXMetricsCollector() {
        return this.jmxMetricsCollector;
    }

    private <T extends Number> T retrieveMBeanAttributeValue(final String mBeanObjectName,
                                                             final String attribute,
                                                             final Class<T> returnType) {
        return jmxMetricsCollector.getMBeanAttributeValue(mBeanObjectName, attribute, returnType);
    }

    private void importRequestsPut(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "requests_put_mean",
                "Put Requests Mean Value",
                retrieveMBeanAttributeValue("requests-put",
                        "Mean", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_count",
                "Put Requests Count",
                retrieveMBeanAttributeValue("requests-put",
                        "Count", Long.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_50thPercentile",
                "Put Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-put",
                        "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_75thPercentile",
                "Put Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-put",
                        "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_95thPercentile",
                "Put Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-put",
                        "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_99thPercentile",
                "Put Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-put",
                        "99thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_FifteenMinuteRate",
                "Put Requests Fifteen Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-put",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_FiveMinuteRate",
                "Put Requests Five Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-put",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_put_OneMinuteRate",
                "Put Requests One Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-put",
                        "OneMinuteRate", Double.class))));
    }

    private void importRequestsGet(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "requests_get_mean",
                "Get Requests Mean Value",
                retrieveMBeanAttributeValue("requests-get",
                        "Mean", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_50thPercentile",
                "Get Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-get",
                        "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_75thPercentile",
                "Get Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-get",
                        "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_95thPercentile",
                "Get Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-get",
                        "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_99thPercentile",
                "Get Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-get",
                        "99thPercentile", Double.class))));
        builder.add((new CounterMetricFamily(name + "requests_get_count",
                "Get Requests Count",
                retrieveMBeanAttributeValue("requests-get",
                        "Count", Long.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_FifteenMinuteRate",
                "Get Requests Fifteen Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-get",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_FiveMinuteRate",
                "Get Requests Five Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-get",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_get_OneMinuteRate",
                "Get Requests One Minute Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-get",
                        "OneMinuteRate", Double.class))));
    }

    private void importRequestDelete(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "requests_delete_mean",
                "Delete Requests Mean Value",
                retrieveMBeanAttributeValue("requests-delete",
                        "Mean", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_50thPercentile",
                "Delete Requests 50thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete",
                        "50thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_75thPercentile",
                "Delete Requests 75thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete",
                        "75thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_95thPercentile",
                "Delete Requests 95thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete",
                        "95thPercentile", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_99thPercentile",
                "Delete Requests 99thPercentile Value",
                retrieveMBeanAttributeValue("requests-delete",
                        "99thPercentile", Double.class))));
        builder.add((new CounterMetricFamily(name + "requests_delete_count",
                "Delete Requests Count",
                retrieveMBeanAttributeValue("requests-delete",
                        "Count", Long.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_FifteenMinuteRate",
                "Delete Requests Fifteen Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-delete",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_FiveMinuteRate",
                "Delete Requests Five Minutes Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-delete",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "requests_delete_OneMinuteRate",
                "Delete Requests One Minute Rate in milliseconds",
                retrieveMBeanAttributeValue("requests-delete",
                        "OneMinuteRate", Double.class))));
    }

    private void importRetries(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "retries_count",
                "Number of Retries",
                retrieveMBeanAttributeValue("retries",
                        "Count", Long.class))));
        builder.add((new GaugeMetricFamily(name + "retries_mean_rate",
                "Mean rate for number of retries",
                retrieveMBeanAttributeValue("retries",
                        "MeanRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "retries_FifteenMinuteRate",
                "Fifteen Minute rate for number of retries",
                retrieveMBeanAttributeValue("retries",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "retries_FiveMinuteRate",
                "Five Minute rate for number of retries",
                retrieveMBeanAttributeValue("retries",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "retries_OneMinuteRate",
                "One Minute rate for number of retries",
                retrieveMBeanAttributeValue("retries",
                        "OneMinuteRate", Double.class))));
    }

    private void importSocketTimeOutExceptionMetric(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "exceptions_socket_time_out_FifteenMinuteRate",
                "Fifteen Minute Rate for SocketTimeOutExceptions",
                retrieveMBeanAttributeValue("exceptions-SocketTimeoutException",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_socket_time_out_FiveMinuteRate",
                "Five Minute Rate for SocketTimeOutExceptions",
                retrieveMBeanAttributeValue("exceptions-SocketTimeoutException",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_socket_time_out_OneMinuteRate",
                "One Minute Rate for SocketTimeOutExceptions",
                retrieveMBeanAttributeValue("exceptions-SocketTimeoutException",
                        "OneMinuteRate", Double.class))));
        builder.add((new CounterMetricFamily(name + "exceptions_socket_time_out_count",
                "Number of SocketTimeOutExceptions",
                retrieveMBeanAttributeValue("exceptions-SocketTimeoutException",
                        "Count", Long.class))));
    }

    private void importNoHttpResponseExceptionMetric(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "exceptions_no_http_response_FifteenMinuteRate",
                "Fifteen Minute Rate for NoHttpResponseExceptions",
                retrieveMBeanAttributeValue("exceptions-NoHttpResponseException",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_no_http_response_FiveMinuteRate",
                "Five Minute Rate for NoHttpResponseExceptions",
                retrieveMBeanAttributeValue("exceptions-NoHttpResponseException",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_no_http_response_OneMinuteRate",
                "One Minute Rate for NoHttpResponseExceptions",
                retrieveMBeanAttributeValue("exceptions-NoHttpResponseException",
                        "OneMinuteRate", Double.class))));
        builder.add((new CounterMetricFamily(name + "exceptions_no_http_response_count",
                "Number of NoHttpResponseExceptions",
                retrieveMBeanAttributeValue("exceptions-NoHttpResponseException",
                        "Count", Long.class))));
    }

    private void importConnectionClosedExceptionMetric(final ImmutableList.Builder<MetricFamilySamples> builder) {
        builder.add((new GaugeMetricFamily(name + "exceptions_connection_closed_FifteenMinuteRate",
                "Fifteen Minute Rate for ConnectionClosedExceptions",
                retrieveMBeanAttributeValue("exceptions-ConnectionClosedException",
                        "FifteenMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_connection_closed_FiveMinuteRate",
                "Five Minute Rate for ConnectionClosedExceptions",
                retrieveMBeanAttributeValue("exceptions-ConnectionClosedException",
                        "FiveMinuteRate", Double.class))));
        builder.add((new GaugeMetricFamily(name + "exceptions_connection_closed_OneMinuteRate",
                "One Minute Rate for ConnectionClosedExceptions",
                retrieveMBeanAttributeValue("exceptions-ConnectionClosedException",
                        "OneMinuteRate", Double.class))));
        builder.add((new CounterMetricFamily(name + "exceptions_connection_closed_count",
                "Number of ConnectionClosedExceptions",
                retrieveMBeanAttributeValue("exceptions-ConnectionClosedException",
                        "Count", Long.class))));
    }


    @Override
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
        // No need to validate. A default value of 0 will be added in case if
        // the exceptions-$class object is not yet registered with the mbeanServer
        importSocketTimeOutExceptionMetric(metricFamilySamplesBuilder);
        importNoHttpResponseExceptionMetric(metricFamilySamplesBuilder);
        importConnectionClosedExceptionMetric(metricFamilySamplesBuilder);

        return metricFamilySamplesBuilder.build();
    }
}
