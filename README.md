# Manta Monitor

## Docker
Running via Docker:

```
docker run -d \
    --name manta-monitor-1
    --memory 1G \
    --label triton.cns.services=manta-monitor \
    -e ENV=production \
    -e HONEYBADGER_API_KEY=XXXXXXXX \
    -e CONFIG_FILE=manta:///user/stor/manta-monitor-config.json \
    -e MANTA_USER=user \
    -e "MANTA_PUBLIC_KEY=$(cat $HOME/.ssh/id_rsa.pub)" \
    -e "MANTA_PRIVATE_KEY=$(cat $HOME/.ssh/id_rsa | base64 -w0)" \
    -e "MANTA_URL=https://us-east.manta.joyent.com" \
    -e MANTA_TIMEOUT=4000 \
    dekobon/manta-monitor
```
## Installation from source
In order to build this project you need the following:

* [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven 3.1.x](https://maven.apache.org/)

From command line, go to the project source directory
```
joyentmac12331:manta-monitor$ mvn clean install
```
Upon successful install, the target jar can be found under:
/manta-monitor/target/

##Running the project using Intellij
The following steps describe how to run the manta-monitor project using Intellij as the IDE.

NOTE: The Intellij Community Edition is good enough to run the application.

###Import the project into Intellij
Follow the screen shots below to import the manta-monitor project into Intellij:

* Start Intellij and select Import Project
![Step 1](img/Intellij-Screen1.png?raw=true)

* Import Project from existing sources and select Maven
![](img/Intellij-Screen2.png?raw=true)

* Select the default options as presented by Intellij and hit Next

![](img/Intellij-Screen3.png?raw=true)

![](img/Intellijj-Screen4.png?raw=true)

![](img/Intellij-Screen5.png?raw=true)

![](img/Intellij-Screen6.png?raw=true)

###Installing the project from Intellij
Once you have successfully imported the project, you can use the Intellij Maven Project View to install the project.

Select the Maven projects view within Intellij and select install.
![](img/Intellij-ScreenShot7?raw=true)

###Running the project from Intellij
From Intellij 'Run' tab select Edit Configurations and add the configurations as shown below:
You will need the following env variables ready before adding the run configuration:
* HONEYBADGER_API_KEY
* MANTA_USER
* MANTA_KEY_ID
* ENV = production/development
* INSTANCE_METADATA_PROPS_FILE = src/test/resources/example-instance-metadata.properties
* MANTA_METRIC_REPORTER_MODE = JMX
![](img/Intellij-EditConfig.png?raw=true)

Save the above configuration and now you will be able to RUN the project in Intellij