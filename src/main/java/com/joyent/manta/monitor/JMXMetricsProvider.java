package com.joyent.manta.monitor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.*;

import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.List;

public class JMXMetricsProvider {

    private final Logger LOG = LoggerFactory.getLogger(JMXMetricsProvider.class);
    private final PlatformMbeanServerProvider platformMbeanServerProvider;

    private final List<Double> arrayOfPutRequestMeanValue = new ArrayList<>();
    private final List<Long> arrayOfPutRequestCount = new ArrayList<>();

    @Inject @Named("retryCount")
    private Long retryCount;

    @Inject
    public JMXMetricsProvider(PlatformMbeanServerProvider platformMbeanServerProvider) {
        this.platformMbeanServerProvider = platformMbeanServerProvider;
    }


    public void recordMetrics() {
        MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMbeanServer();

        try {
            Set<ObjectName> mNames = new TreeSet<>(mBeanServer.queryNames(null, null));
            for (ObjectName name : mNames) {
                if (name.getCanonicalName().startsWith("com.joyent.manta.client:00=requests-put")) {
                    System.out.println("Mean time from platform mbean server: " + mBeanServer.getAttribute(name, "Mean"));
                    arrayOfPutRequestMeanValue.add((Double) mBeanServer.getAttribute(name, "Mean"));
                    arrayOfPutRequestCount.add((Long) mBeanServer.getAttribute(name, "Count"));
                }

                if (name.getCanonicalName().startsWith("com.joyent.manta.client:00=retries")) {
                    retryCount = (Long) (mBeanServer.getAttribute(name, "Count"));
                }
            }
        } catch (AttributeNotFoundException ane) {
            LOG.error("Failed to get Mbean attribute {}", ane.getMessage());
        } catch (MBeanException mbe) {
            LOG.error("{}", mbe.getTargetException());
        } catch (Exception e) {
            LOG.error("{}", e.getStackTrace());
        }
    }

    public Double getPutRequestMeanValue() {
        if(arrayOfPutRequestMeanValue.size() > 0) {
            return arrayOfPutRequestMeanValue.get(arrayOfPutRequestMeanValue.size()-1);
        } else {
           return null;
        }
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public Long getPutRequestCount() {
        if(arrayOfPutRequestCount.size() > 0) {
            return arrayOfPutRequestCount.get(arrayOfPutRequestMeanValue.size()-1);
        } else {
            return null;
        }
    }
}
