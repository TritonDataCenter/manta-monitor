##Build a local image from the supplied Dockerfile

Follow the steps to build a docker image, from the project directory

###Step 1: Build the project
```
manta-monitor# mvn clean install
```

###Step 2: Build the image
```
manta-monitor# docker build . -t manta-monitor
```

The above command will build the latest image of manta-monitor. You can check the same as follows:
```
manta-monitor# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
manta-monitor       latest              3abf4580bd9d        4 days ago          402MB
ubuntu              xenial              a51debf7e1eb        4 weeks ago         116MB
``` 

###Step 3: Run the application using the image created above, as follows:
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
    manta-monitor
```
In order to check the running status of the container use the following commands:
```
manta-monitor# docker ps -a
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
20b9ffd597a0        manta-monitor-1     "/bin/sh -c /opt/man…"   3 days ago          Up 3 days           0.0.0.0:8090->8090/tcp   manta-monitor-1

manta-monitor# docker logs 20b9ffd597a0
```
