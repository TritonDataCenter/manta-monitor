#!/bin/bash

# Install docker
sudo curl -sSL https://get.docker.com/ | sh

# Pull manta-monitor image

sudo docker pull joyent/manta-monitor

# Run manta-monitor

if [ "${enable_tls}" == "false" ]; then
sudo docker run -d -p ${jetty_server_port}:${jetty_server_port ~}
 --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 --name ${container_name ~}
 --memory ${container_memory ~}
 -e JAVA_ENV=${java_env ~}
 -e HONEYBADGER_API_KEY=${honeybadger_api_key ~}
 -e CONFIG_FILE=${config_file ~}
 -e MANTA_USER=${manta_user ~}
 -e "MANTA_PUBLIC_KEY=${manta_public_key ~}" -e "MANTA_PRIVATE_KEY=${manta_private_key ~}" -e "MANTA_URL=${manta_url ~}" -e MANTA_TIMEOUT=${manta_timeout ~}
 -e MANTA_METRIC_REPORTER_MODE=${manta_metric_reporter_mode ~}
 -e MANTA_HTTP_RETRIES=${manta_http_retries ~}
 -e JETTY_SERVER_PORT=${jetty_server_port ~}
 -e ENABLE_TLS=${enable_tls ~}
 joyent/manta-monitor
else
sudo docker run -d -p ${jetty_server_secure_port}:${jetty_server_secure_port ~}
 --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 --name ${container_name ~}
 --memory ${container_memory ~}
 -e JAVA_ENV=${java_env ~}
 -e HONEYBADGER_API_KEY=${honeybadger_api_key ~}
 -e CONFIG_FILE=${config_file ~}
 -e MANTA_USER=${manta_user ~}
 -e "MANTA_PUBLIC_KEY=${manta_public_key ~}" -e "MANTA_PRIVATE_KEY=${manta_private_key ~}" -e "MANTA_URL=${manta_url ~}" -e MANTA_TIMEOUT=${manta_timeout ~}
 -e MANTA_METRIC_REPORTER_MODE=${manta_metric_reporter_mode ~}
 -e MANTA_HTTP_RETRIES=${manta_http_retries ~}
 -e JETTY_SERVER_SECURE_PORT=${jetty_server_secure_port ~}
 -e ENABLE_TLS=${enable_tls ~}
 -e KEYSTORE_PATH=${keystore_path} -e TRUSTSTORE_PATH=${truststore_path} -e KEYSTORE_PASS=${keystore_pass} -e TRUSTSTORE_PASS=${truststore_pass ~}
 joyent/manta-monitor
fi

sleep 20

# Run docker ps -a once to display the status of the docker run command
sudo docker ps -a

DOCKER_CONTAINER_ID=$(sudo docker ps -a | sed -sn '2p' | awk '{print $1}')

DOCKER_RUN_STATUS=$(sudo docker ps -a | sed -sn '2p' | awk '{print $9}')

if [ $DOCKER_RUN_STATUS != "Up" ]; then
    echo "Manta Monitor failed to run. Here are the logs:"
    sudo docker logs $DOCKER_CONTAINER_ID
fi