/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import javax.validation.ValidationException;

import java.io.InvalidObjectException;
import java.util.Set;

/**
 * Class that provides methods to extract and validate JMX metrics from the platform MBeanServer.
 */
public class JMXMetricsCollector {
    private final PlatformMbeanServerProvider platformMbeanServerProvider;

    private static final String MBEAN_DOMAIN = "com.joyent.manta.client";
    private static final String MBEAN_OBJECT_KEY = "00";

    @Inject
    public JMXMetricsCollector(final PlatformMbeanServerProvider platformMbeanServerProvider) {
        this.platformMbeanServerProvider = platformMbeanServerProvider;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Number> T getMBeanAttributeValue(final String mBeanObjectName, final String attribute, final Class<T> returnType) {
        ObjectName objectName = getObjectNameFromString(mBeanObjectName);
        if (objectName == null) {
            String message = "Requested MBean Object not found";
            MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, new InvalidObjectException(message));
            mBeanServerOperationException.addContextValue("objectName", mBeanObjectName);
            mBeanServerOperationException.addContextValue("attribute", attribute);
            mBeanServerOperationException.addContextValue("mbeanServerDomain", MBEAN_DOMAIN);
            mBeanServerOperationException.addContextValue("mbeanObjectKey", MBEAN_OBJECT_KEY);
            throw mBeanServerOperationException;
        }
        MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
        Object value;
        try {
            value = mBeanServer.getAttribute(objectName, attribute);
            final Number number;

            if (value instanceof Number) {
                number = (Number) value;
            } else {
                try {
                    number = Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    String message = "Failed to parse attribute value to number";
                    MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, e);
                    mBeanServerOperationException.addContextValue("objectName", objectName.getCanonicalName());
                    mBeanServerOperationException.addContextValue("attribute", attribute);
                    mBeanServerOperationException.addContextValue("mbeanServerDomain", MBEAN_DOMAIN);
                    mBeanServerOperationException.addContextValue("mbeanObjectKey", MBEAN_OBJECT_KEY);
                    mBeanServerOperationException.addContextValue("expectedAttributeReturnType", returnType.getCanonicalName());
                    mBeanServerOperationException.addContextValue("resultAttributeValueReturnType", value != null ? value.getClass() : null);
                    mBeanServerOperationException.addContextValue("resultAttributeValue", value);
                    throw mBeanServerOperationException;
                }
            }

            if (returnType.equals(number.getClass())) {
                return (T)number;
            } else if (returnType.equals(Short.class)) {
                return (T)Short.valueOf(number.shortValue());
            } else if (returnType.equals(Integer.class)) {
                return (T)Integer.valueOf(number.intValue());
            } else if (returnType.equals(Long.class)) {
                return (T)Long.valueOf(number.longValue());
            } else if (returnType.equals(Float.class)) {
                return (T)Float.valueOf(number.floatValue());
            } else if (returnType.equals(Double.class)) {
                return (T)Double.valueOf(number.doubleValue());
            } else if (returnType.equals(Byte.class)) {
                return (T)Byte.valueOf(number.byteValue());
            }
        } catch (AttributeNotFoundException | InstanceNotFoundException | ReflectionException | MBeanException e) {
            String message;
            if (e instanceof AttributeNotFoundException) {
                message = "The specified attribute does not exist or cannot be retrieved";
            } else if (e instanceof  InstanceNotFoundException) {
                message = "The specified MBean does not exist in the repository.";
            } else if (e instanceof MBeanException) {
                message = "Failed to retrieve the attribute from the MBean Server.";
            } else {
                message = "The requested operation is not supported by the MBean Server ";
            }
            MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, e);
            mBeanServerOperationException.addContextValue("objectName", objectName.getCanonicalName());
            mBeanServerOperationException.addContextValue("attribute", attribute);
            mBeanServerOperationException.addContextValue("mbeanServerDomain", MBEAN_DOMAIN);
            mBeanServerOperationException.addContextValue("mbeanObjectKey", MBEAN_OBJECT_KEY);
            throw mBeanServerOperationException;
        }
        return null;
    }

    public boolean validateMbeanObject(final String objectName) {
        ObjectName mbeanObject = getObjectNameFromString(objectName);
        if (mbeanObject == null) {
            // This also indicates that the mbeanObject is not registered.
            // Check to see if it is an exceptions-$class object
            if (objectName.startsWith("exceptions")) {
                // The exceptions object is not added to the MBeanServer, as there
                // were no HTTP client exceptions raised yet.
                return false;
            }
            String message = "Requested mbean object is not registered with the Platform MBean Server";
            MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, new ValidationException(message));
            mBeanServerOperationException.addContextValue("objectName", objectName);
            mBeanServerOperationException.addContextValue("mbeanServerDomain", MBEAN_DOMAIN);
            mBeanServerOperationException.addContextValue("mbeanObjectKey", MBEAN_OBJECT_KEY);
            throw mBeanServerOperationException;
        }
        MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
        return mBeanServer.isRegistered(mbeanObject);
    }

    @Nullable
    private ObjectName getObjectNameFromString(final String objectName) {
        String objectNameWithDomain = MBEAN_DOMAIN + ":" + MBEAN_OBJECT_KEY + "=" + objectName + ",*";
        MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
        ObjectName responseObjectName = null;
        try {
            ObjectName mbeanObjectName = new ObjectName(objectNameWithDomain);
            Set<ObjectName> objectNames = mBeanServer.queryNames(mbeanObjectName, null);
            for (ObjectName object: objectNames) {
                responseObjectName = object;
            }
        } catch (MalformedObjectNameException mfe) {
            String message = String.format("Error in creating mbean object name from the string %s", objectName);
            MBeanServerOperationException mBeanServerOperationException = new MBeanServerOperationException(message, mfe);
            mBeanServerOperationException.addContextValue("objectName", objectName);
            mBeanServerOperationException.addContextValue("mbeanServerDomain", MBEAN_DOMAIN);
            mBeanServerOperationException.addContextValue("mbeanObjectKey", MBEAN_OBJECT_KEY);
            throw mBeanServerOperationException;
        }
        return responseObjectName;
    }
}
