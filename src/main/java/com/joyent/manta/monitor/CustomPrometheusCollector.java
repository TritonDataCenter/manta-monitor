package com.joyent.manta.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

public class CustomPrometheusCollector extends Collector {
    private final Logger LOG = LoggerFactory.getLogger(CustomPrometheusCollector.class);

    private final JMXMetricsCollector jmxMetricsCollector;
    private final Map<String, AtomicLong> clientStats;

    @Inject
    CustomPrometheusCollector(@Named("JMXMetricsCollector") JMXMetricsCollector jmxMetricsCollector,
                              @Named("SharedStats") Map<String, AtomicLong> clientStats) {
        this.jmxMetricsCollector = jmxMetricsCollector;
        this.clientStats = clientStats;
    }

    public List<MetricFamilySamples> collect() {
        final List<MetricFamilySamples> metricFamilySamples = new ArrayList<>();
        try {
            if(jmxMetricsCollector.validateMbeanObject("requests-put")) {
                try {
                    metricFamilySamples.add((new GaugeMetricFamily("requests_put_mean",
                            "Put Requests Mean Value",
                            jmxMetricsCollector.getMBeanAttributeValue("requests-put", "Mean", Double.class))));
                    metricFamilySamples.add((new GaugeMetricFamily("requests_put_50thPercentile",
                            "Put Requests 50thPercentile Value",
                            jmxMetricsCollector.getMBeanAttributeValue("requests-put", "50thPercentile", Double.class))));
                    metricFamilySamples.add((new CounterMetricFamily("requests_put_count",
                            "Put Requests Count",
                            jmxMetricsCollector.getMBeanAttributeValue("requests-put", "Count", Long.class).doubleValue())));
                } catch (Exception e) {
                    if(e instanceof ExceptionContext) {
                        LOG.error("Failed to get the metric for reason: {}", e.getMessage());
                    }
                    System.exit(1);
                }
            }
            if(jmxMetricsCollector.validateMbeanObject("retries")) {
                try {
                    metricFamilySamples.add(new CounterMetricFamily("Retry",
                            "Number of Retries",
                            jmxMetricsCollector.getMBeanAttributeValue("retries", "Count", Long.class).doubleValue()));
                } catch (Exception e) {
                    if(e instanceof ExceptionContext) {
                        LOG.error("Failed to get the metric for reason: {}", e.getMessage());
                    }
                    System.exit(1);
                }
            }
            if(!clientStats.isEmpty()) {
                clientStats.forEach((key, value) -> {
                    metricFamilySamples.add(new GaugeMetricFamily("manta_monitor_operation_chain_elapsed_time",
                            "Total time in milliseconds to complete one chain of operations",
                            value.doubleValue()));
                });
            }
        } catch (Exception e) {
            if(e instanceof ExceptionContext) {
                //Provides the message explaining the exception, including the contextual data.
                LOG.error("Validation of the MBean object failed with message: {}", e.getMessage());
            }
            System.exit(1);
        }
        return metricFamilySamples;
    }
}
