# Manta Monitor

## Docker
Running via Docker:

```
docker run -d \
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
    dekobon/manta-monitor
```
The parameter MANTA_HTTP_RETRIES, above defines the number of times to retry failed HTTP requests. 
Setting this value to zero disables retries completely.
Please refer [here](https://github.com/joyent/java-manta/blob/master/USAGE.md#parameters) for more details about the parameters
## Installation from source
In order to build this project you need the following:

* [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven 3.1.x](https://maven.apache.org/)

From command line, go to the project source directory
```
$ mvn clean install
```

Please follow the documentation [here](doc/intellij-setup.md) to run the application in IntelliJ.