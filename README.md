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

### Run

#### Modes of operation
Manta-monitor can expose the JMX metrics coming from the manta client over http and https. Hence there are two modes in 
which the application can run.

* HTTP mode (unsecured): 
In this mode, the metrics will be exposed over HTTP and a typical execution via Docker looks like as follows:

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
    -e JETTY_SERVER_PORT=8090 \
    joyent/manta-monitor
```

* HTTPS mode (secured):
This optional mode enables TLS and exposes the metrics over https. In order to operate manta-monitor in this mode there 
are some additional configuration settings required and are as follows:
    * Providing Keys and Certificates for Server keystore : In order for our server to be able to participate in SSL, we
    need to provide it with a keystore that contains a valid certificate (can be either self signed or CA provided) and 
    a private key. Follow the below steps to generate a keystore using JDK's keytool.
    
    NOTE: Please note/remember any passwords being used for the creation of the keystore as they will be required by the
    application.
 
    The following command prompts for information about the certificate and for passwords to protect both the keystore 
    and the keys within it. The only mandatory response is to provide the fully qualified host name of the server at the
    "first and last name" prompt. For example:
    ```
    keytool -keystore keystore -genkey -keyalg RSA -sigalg SHA256withRSA
     Enter keystore password:  password
     What is your first and last name?
       [Unknown]: com.joyent.manta.monitor
     What is the name of your organizational unit?
       [Unknown]: manta.monitor
     What is the name of your organization?
       [Unknown]: Joyent
     What is the name of your City or Locality?
       [Unknown]:
     What is the name of your State or Province?
       [Unknown]:
     What is the two-letter country code for this unit?
       [Unknown]:
     Is CN=com.joyent.manta.monitor, OU=manta.monitor, O=Joyent,
     L=Unknown, ST=Unknown, C=Unknown correct?
       [no]:  yes
    
     Enter key password for <jetty>
             (RETURN if same as keystore password):
     $
    ```
    NOTE : If you already have key and certificate in different files then you need to combine them into a PKCS12 format
    file to load into a new keystore. The following OpenSSL command combines the key in server.key file and the 
    certificate in server.crt file into the server.pkcs12 file:
    ```
    openssl pkcs12 -inkey server.key -in server.crt -export -out server.pkcs12
    ```
    * Importing the keystore generated above into a PKCS12 formatted keystore: Once you have successfully created the 
    keystore (keystore or server.pkcs12, as in the above example), you need to import the same in a PKCS12 format as:
    ```
    keytool -importkeystore -srckeystore keystore -destkeystore keystore -deststoretype pkcs12
    
    OR
    
    keytool -importkeystore -srckeystore server.pkcs12 -srcstoretype PKCS12 -destkeystore keystore -deststoretype pkcs12
    ```
    * Creating a Server trust store. A server trust store contains the certificate and key of a trusted client. This
    way when a client tries to connect to the application over https, it's certificate will be validated against the 
    trusted store and thereby granted access. Hence, in order to configure a server's trust store you would need the 
    client's PEM formatted certificate and key (as well as any password associated with them). 
    
    NOTE: Please note/remember any passwords that are set during creation of the trust store.
    
    The following OpenSSL command will generate a PKCS12 formatted trust store using the client's certificate and key.
    
    ```
     openssl pkcs12 -inkey client.key -in client.crt -export -out client.pkcs12
    ```
    Once you have successfully created the above client.pkcs12 truststore, import the same using the keytool as:
    ```
    keytool -importkeystore -srckeystore client.pkcs12 -srcstoretype PKCS12 -destkeystore truststore -deststoretype pkcs12
    ```
Once you have successfully created the server's keystore and truststore, you can use the following docker command to run
the application:

```
docker run -p 8443:8443 -d \
    --name manta-monitor-1
    --memory 1G \
    --label triton.cns.services=manta-monitor \
    -v <absolute-path-to-server-keystore>:/opt/manta-monitor/keystore \
    -v <absolute-path-to-server-truststore>:/opt/manta-monitor/truststore \
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
    -e ENABLE_TLS=true \
    -e KEYSTORE_PATH=/opt/manta-monitor/keystore \
    -e KEYSTORE_PASS=//XXXXXXXX \
    -e TRUSTSTORE_PATH=//opt/manta-monitor/truststore \
    -e TRUSTSTORE_PASS=XXXXXXXX \
    -e JETTY_SERVER_SECURE_PORT=8443 \
    joyent/manta-monitor
```
NOTE : For detailed information about about generating and using keystore 
refer [here](https://www.eclipse.org/jetty/documentation/9.4.x/configuring-ssl.html#configuring-jetty-for-ssl)

Additional notes: 
* The parameter MANTA_HTTP_RETRIES, above defines the number of times to retry failed HTTP requests. 
  Setting this value to zero disables retries completely.
  Please refer [here](https://github.com/joyent/java-manta/blob/master/USAGE.md#parameters) 
  for more details about the parameters.
* In order to setup prometheus server [tls_config](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#tls_config)
  to scrape manta-monitor metrics, follow the below steps to generate the certificate and key.
  ```
  openssl req  -nodes -new -x509  -keyout prometheus.key -out prometheus.cert
  ```
  Use the above key and certificate file to add tls_config to the prometheus.yml. A typical prometheus.yml might look like:
  ```
  # my global config
  global:
    scrape_interval:     15s # By default, scrape targets every 15 seconds.
    evaluation_interval: 15s # By default, scrape targets every 15 seconds.
    # scrape_timeout is set to the global default (10s).
  
    # Attach these labels to any time series or alerts when communicating with
    # external systems (federation, remote storage, Alertmanager).
    #external_labels:
    #    monitor: 'codelab-monitor'
  
  # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
  rule_files:
    # - "first.rules"
    # - "second.rules"
  
  # A scrape configuration containing exactly one endpoint to scrape:
  scrape_configs:
    # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
    - job_name: 'manta-monitor'
  
      # Override the global default and scrape targets from this job every 5 seconds.
      scrape_interval: 5s
  
      # metrics_path defaults to '/metrics'
      # scheme defaults to 'http'.
  
      static_configs:
        - targets: ['localhost:8443']
      scheme: https
      tls_config:
        cert_file: prometheus.cert
        key_file: prometheus.key
        insecure_skip_verify: true
  ```
### Deployment
To deploy the application in a production environment, refer to the
[Manta Monitor Deployment document](doc/manta-monitor-deployment.md).
 
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
# HELP exceptions_socket_time_out_FifteenMinuteRate Fifteen Minute Rate for SocketTimeOutExceptions
# TYPE exceptions_socket_time_out_FifteenMinuteRate gauge
exceptions_socket_time_out_FifteenMinuteRate 0.0
# HELP exceptions_socket_time_out_FiveMinuteRate Five Minute Rate for SocketTimeOutExceptions
# TYPE exceptions_socket_time_out_FiveMinuteRate gauge
exceptions_socket_time_out_FiveMinuteRate 0.0
# HELP exceptions_socket_time_out_OneMinuteRate One Minute Rate for SocketTimeOutExceptions
# TYPE exceptions_socket_time_out_OneMinuteRate gauge
exceptions_socket_time_out_OneMinuteRate 0.0
# HELP exceptions_socket_time_out_count Number of SocketTimeOutExceptions
# TYPE exceptions_socket_time_out_count counter
exceptions_socket_time_out_count 0.0
# HELP exceptions_no_http_response_FifteenMinuteRate Fifteen Minute Rate for NoHttpResponseExceptions
# TYPE exceptions_no_http_response_FifteenMinuteRate gauge
exceptions_no_http_response_FifteenMinuteRate 0.0
# HELP exceptions_no_http_response_FiveMinuteRate Five Minute Rate for NoHttpResponseExceptions
# TYPE exceptions_no_http_response_FiveMinuteRate gauge
exceptions_no_http_response_FiveMinuteRate 0.0
# HELP exceptions_no_http_response_OneMinuteRate One Minute Rate for NoHttpResponseExceptions
# TYPE exceptions_no_http_response_OneMinuteRate gauge
exceptions_no_http_response_OneMinuteRate 0.0
# HELP exceptions_no_http_response_count Number of NoHttpResponseExceptions
# TYPE exceptions_no_http_response_count counter
exceptions_no_http_response_count 0.0
# HELP exceptions_connection_closed_FifteenMinuteRate Fifteen Minute Rate for ConnectionClosedExceptions
# TYPE exceptions_connection_closed_FifteenMinuteRate gauge
exceptions_connection_closed_FifteenMinuteRate 0.0
# HELP exceptions_connection_closed_FiveMinuteRate Five Minute Rate for ConnectionClosedExceptions
# TYPE exceptions_connection_closed_FiveMinuteRate gauge
exceptions_connection_closed_FiveMinuteRate 0.0
# HELP exceptions_connection_closed_OneMinuteRate One Minute Rate for ConnectionClosedExceptions
# TYPE exceptions_connection_closed_OneMinuteRate gauge
exceptions_connection_closed_OneMinuteRate 0.0
# HELP exceptions_connection_closed_count Number of ConnectionClosedExceptions
# TYPE exceptions_connection_closed_count counter
exceptions_connection_closed_count 0.0
# HELP manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain Metric that gives a cumulative observation for
# latency, in seconds, in creating a directory and uploading a file
# TYPE manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain histogram
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.005",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.01",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.025",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.05",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.075",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.1",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.25",} 0.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.5",} 12.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="0.75",} 22.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="1.0",} 22.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="2.5",} 38.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="5.0",} 46.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="7.5",} 46.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="10.0",} 46.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_bucket{le="+Inf",} 46.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_count 46.0
manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain_sum 73.735187768
```
For more details about the metrics refer to the document [here](doc/manta-monitor-metrics.md)
## License
Manta Monitor is licensed under the MPLv2. Please see the 
[LICENSE.txt](/LICENSE.txt) file for more details.