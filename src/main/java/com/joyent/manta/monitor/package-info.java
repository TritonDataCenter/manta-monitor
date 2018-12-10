/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * This package contains classes that let you start the manta monitor java application and view the metrics reported by
 * the manta client in prometheus exposition format, for eg:
 * requests_get_count 5.0
 * # HELP requests_delete_mean Delete Requests Mean Value
 * # TYPE requests_delete_mean gauge
 * where:
 * requests_get_count - name of the metric
 * 5.0 - value
 * HELP - description about the metric
 * TYPE - type of the metric (gauge, counter etc..)
 * More on the prometheus metric types <a href="https://prometheus.io/docs/concepts/metric_types/"></a>
 *
 * <h3>Design</h3>
 *
 * <p>The class {@link com.joyent.manta.monitor.Application} is the entry point to the manta-monitor and it takes in the
 * path of the json file that contains the configuration with which the application would run. An example configuration
 * can be found at <a href="https://github.com/joyent/manta-monitor/blob/master/src/test/resources/test-configuration.json"></a>
 * </p>
 *
 * <p>The application starts a chain of operations, as per the json file above, and creates java-manta client apis to
 * carry out various manta cli commands, found under {@link com.joyent.manta.monitor.commands}</p>
 *
 * <p>The application also extracts JMX metrics as provided by java-manta client and presents these metrics at a http endpoint,
 * which is prometheus compatible, as shown above. The metrics are available at port that is configurable in the json file
 * and viewable at the endpoint /metrics. For example the http endpoint can be 'http://localhost:8090/metrics'.
 * </p>
 *
 * <p>An error/exception arising out of the application is reported to HoneyBadger.
 * For more info about HoneyBadger <a href="https://www.honeybadger.io"></a></p>
 *
 * @author <a href="https://github.com/1010sachin">Sachin Gupta</a>
 */
package com.joyent.manta.monitor;
