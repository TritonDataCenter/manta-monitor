package com.joyent.manta.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class CustomPrometheusCollector extends Collector {

    final JMXMetricsProvider jmxMetricsProvider;

    @Inject
    CustomPrometheusCollector(@Named("JMXMetricsProvider") JMXMetricsProvider jmxMetricsProvider) {
        this.jmxMetricsProvider = jmxMetricsProvider;
    }

    public List<MetricFamilySamples> collect() {
        final List<MetricFamilySamples> metricFamilySamples = new ArrayList<>();
        metricFamilySamples.add(new CounterMetricFamily("Retry", "Number of Retries", jmxMetricsProvider.getRetryCount().doubleValue()));
        if(jmxMetricsProvider.getPutRequestMeanValue() != null) {
            metricFamilySamples.add((new GaugeMetricFamily("Mean", "Put Request Mean Value", jmxMetricsProvider.getPutRequestMeanValue())));
        }
        if(jmxMetricsProvider.getPutRequestCount() != null) {
            metricFamilySamples.add((new CounterMetricFamily("Put Request Count", "", jmxMetricsProvider.getPutRequestCount())));
        }
        return metricFamilySamples;
    }
}
