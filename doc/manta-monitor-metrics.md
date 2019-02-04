This document defines the metrics collected by manta-monitor.
The metrics provided by the manta client are made available through [JMX](https://en.wikipedia.org/wiki/Java_Management_Extensions).

The following is the list of metrics exposed by manta-monitor via a [Prometheus](https://prometheus.io/) compatible endpoint.
When the application is running, these metrics can be accessed over http (eg. http://localhost:8090/metrics) or 
over https (eg. https://localhost:8443/metrics), if configured to run in secured mode.

* requests_$METHOD_mean : A [gauge](https://prometheus.io/docs/concepts/metric_types/#gauge) type metric that gives the
mean value, in milliseconds, of the time elapsed in processing of HTTP requests. Values for $METHOD include 
GET, PUT, DELETE.
* requests_$METHOD_count : A [counter](https://prometheus.io/docs/concepts/metric_types/#counter) type metric that gives
the number of HTTP requests processed for GET, PUT and DELETE respectively.
* requests_$METHOD_$PERCENTILES : A [gauge](https://prometheus.io/docs/concepts/metric_types/#gauge) type metric that 
gives the percentile value of the time taken by the HTTP requests to process. Example values of percentile are 50thPercentile, 
75thPercentile, 95thPercentile and 99thPercentile for GET, PUT and DELETE requests respectively.
* retries_count : A [counter](https://prometheus.io/docs/concepts/metric_types/#counter) type metric that gives the count
of number of retries attempted by the manta client to process the HTTP requests.
* retires_mean_rate : A [gauge](https://prometheus.io/docs/concepts/metric_types/#gauge) type metric that gives the moving
average of the retries attempted.
* exceptions_$CLASS_$MINUTE_RATE : A [gauge](https://prometheus.io/docs/concepts/metric_types/#gauge) type metric that 
gives the FIFTEEN, FIVE and ONE minute rate of the exceptions that occurred while executing HTTP requests, respectively.
Example values of $CLASS includes SocketTimeoutException, NoHTTPResponseException and ConnectionClosedException.
* exceptions_$CLASS_count :  A [counter](https://prometheus.io/docs/concepts/metric_types/#counter) type metric that
gives the count of the number of exceptions occurred while executing HTTP requests. Example values of $CLASS includes 
SocketTimeoutException, NoHTTPResponseException and ConnectionClosedException.