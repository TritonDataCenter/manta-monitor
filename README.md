# Manta Monitor

## Introduction
Manta Monitor is a stand-alone Java program that executes a continually running
workload against a [Manta](https://github.com/joyent/manta) installation. 
Runtime metrics are collected and exposed via a 
[Prometheus](https://prometheus.io/) compatible endpoint. Exceptions thrown are
collected and stored on [honeybader.io](https://www.honeybadger.io/) for later
diagnosis.

## Docker

In order to make the execution and configuration of Manta Monitor easier, a
[Docker image is provided](https://hub.docker.com/r/joyent/manta-monitor).

### Generating Keys and Certificates with JDK’s keytool

```
keytool -keystore keystore -alias jetty -genkey -keyalg RSA -sigalg SHA256withRSA

keytool -importkeystore -srckeystore keystore -destkeystore keystore -deststoretype pkcs12
```
For details about generating and using keystore refer [here](https://www.eclipse.org/jetty/documentation/9.4.x/configuring-ssl.html#configuring-jetty-for-ssl)

### Run

A typical execution via Docker looks like as follows:
```
docker run -p 8090:8090 -d \
    --name manta-monitor-1
    --memory 1G \
    --label triton.cns.services=manta-monitor \
    -e JAVA_ENV=production \
    -e HONEYBADGER_API_KEY=XXXXXXXX \
    -e CONFIG_FILE=manta:///user/stor/manta-monitor-config.json \
    -e MANTA_USER=user \
    -e "MANTA_PUBLIC_KEY=$(cat $HOME/.ssh/id_rsa.pub)" \
    -e "MANTA_PRIVATE_KEY=$(cat $HOME/.ssh/id_rsa | base64 -w0)" \
    -e "MANTA_URL=https://us-east.manta.joyent.com" \
    -e MANTA_TIMEOUT=4000 \
    -e MANTA_METRIC_REPORTER_MODE=JMX \
    -e MANTA_HTTP_RETRIES=3 \
    -e KEYSTORE_PATH=//Absolute path to the keystore \ (OPTIONAL. Required only for TLS)
    -e KEYSTORE_PASS=XXXXXXXX \ (OPTIONAL. Required only for TLS)
    -e JETTY_SERVER_PORT=8090 \
    -e JETTY_SERVER_SECURE_PORT=8443 \(OPTIONAL. Required only if the above KEYSTORE_PATH and KEYSTORE_PASS are set)
    joyent/manta-monitor
```

The parameter MANTA_HTTP_RETRIES, above defines the number of times to retry
failed HTTP requests. Setting this value to zero disables retries completely.
Please refer [here](https://github.com/joyent/java-manta/blob/master/USAGE.md#parameters) 
for more details about the parameters.
The parameters KEYSTORE_PATH, KEYSTORE_PASS and JETTY_SECURE_SERVER_PORT are optional and to be used only if you want to
enable TLS. In this case, set the KEYSTORE_PATH and KEYSTORE_PASS to the path to the keystore file stored locally and the
password used to generate the keystore, respectively. Once the keystore variables are set, the JETTY_SERVER_SECURE_PORT
value will be used to expose the manta-monitor metrics over https.
 
### Build

To build and run the application from a local Docker image refer to the 
[Docker build documentation](doc/docker-build-setup.md).

## Development

### Build
In order to build this project you need the following:

* [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven 3.1.x](https://maven.apache.org/)

From command line, go to the project source directory
```
$ mvn clean install
```

### IDE Development

Please follow the documentation [here](doc/intellij-setup.md) to run the 
application in IntelliJ.

## Metrics

The application exposes metrics in 
[prometheus exposition format](https://prometheus.io/docs/instrumenting/exposition_formats/), 
over the port 8090 of the machine where it is being deployed.

An example of the metrics gathered by the application is as follows:

```
$ curl http://localhost:8090/metrics
# HELP requests_put_mean Put Requests Mean Value
# TYPE requests_put_mean gauge
requests_put_mean 513.1039835349405
# HELP requests_put_count Put Requests Count
# TYPE requests_put_count gauge
requests_put_count 190.0
# HELP requests_put_50thPercentile Put Requests 50thPercentile Value
# TYPE requests_put_50thPercentile gauge
requests_put_50thPercentile 482.295442
# HELP requests_put_75thPercentile Put Requests 75thPercentile Value
# TYPE requests_put_75thPercentile gauge
requests_put_75thPercentile 549.051063
# HELP requests_put_95thPercentile Put Requests 95thPercentile Value
# TYPE requests_put_95thPercentile gauge
requests_put_95thPercentile 617.953976
# HELP requests_put_99thPercentile Put Requests 99thPercentile Value
# TYPE requests_put_99thPercentile gauge
requests_put_99thPercentile 964.0388019999999
# HELP requests_get_mean Get Requests Mean Value
# TYPE requests_get_mean gauge
requests_get_mean 134.7614410977741
# HELP requests_get_50thPercentile Get Requests 50thPercentile Value
# TYPE requests_get_50thPercentile gauge
requests_get_50thPercentile 97.131346
# HELP requests_get_75thPercentile Get Requests 75thPercentile Value
# TYPE requests_get_75thPercentile gauge
requests_get_75thPercentile 143.489182
# HELP requests_get_95thPercentile Get Requests 95thPercentile Value
# TYPE requests_get_95thPercentile gauge
requests_get_95thPercentile 361.778692
# HELP requests_get_99thPercentile Get Requests 99thPercentile Value
# TYPE requests_get_99thPercentile gauge
requests_get_99thPercentile 361.778692
# HELP requests_get_count Get Requests Count
# TYPE requests_get_count counter
requests_get_count 11.0
# HELP requests_delete_mean Delete Requests Mean Value
# TYPE requests_delete_mean gauge
requests_delete_mean 435.8537245980763
# HELP requests_delete_50thPercentile Delete Requests 50thPercentile Value
# TYPE requests_delete_50thPercentile gauge
requests_delete_50thPercentile 400.22918699999997
# HELP requests_delete_75thPercentile Delete Requests 75thPercentile Value
# TYPE requests_delete_75thPercentile gauge
requests_delete_75thPercentile 427.471213
# HELP requests_delete_95thPercentile Delete Requests 95thPercentile Value
# TYPE requests_delete_95thPercentile gauge
requests_delete_95thPercentile 478.13541699999996
# HELP requests_delete_99thPercentile Delete Requests 99thPercentile Value
# TYPE requests_delete_99thPercentile gauge
requests_delete_99thPercentile 1954.286699
# HELP requests_delete_count Delete Requests Count
# TYPE requests_delete_count counter
requests_delete_count 126.0
# HELP retries_count Number of Retries
# TYPE retries_count gauge
retries_count 1.0
# HELP retries_mean_rate Mean rate for number of retries
# TYPE retries_mean_rate gauge
retries_mean_rate 0.028769379866041778
# HELP manta_monitor_operation_chain_elapsed_time Total time in milliseconds to complete one chain of operations
# TYPE manta_monitor_operation_chain_elapsed_time gauge
manta_monitor_operation_chain_elapsed_time 22677.0
```
For more details about the metrics refer to the document [here](doc/manta-monitor-metrics.md)
## License
Manta Monitor is licensed under the MPLv2. Please see the 
[LICENSE.txt](/LICENSE.txt) file for more details.