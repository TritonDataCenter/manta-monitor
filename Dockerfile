FROM ubuntu:xenial

MAINTAINER Elijah Zupancic <elijah@zupancic.name>

# Metadata for Docker containers: http://label-schema.org/
LABEL org.label-schema.name="Manta Monitor" \
      org.label-schema.description="Monitors the Manta Object Store for Quality of Service" \
      org.label-schema.url="https://github.com/joyent/manta-monitor" \
      org.label-schema.vcs-url="org.label-schema.vcs-ref" \
      org.label-schema.vendor="Joyent" \
      org.label-schema.schema-version="1.0"

WORKDIR /opt/manta-monitor

ENV MANTA_MONITOR_VERSION=1.0.0-SNAPSHOT
ENV ZULE_JCE_POLICY_CHECKSUM ebe83e1bf25de382ce093cf89e93a944
ENV JAVA_HOME /usr/lib/jvm/zulu-11-amd64
ENV INSTANCE_METADATA_PROPS_FILE /opt/manta-monitor/tmp/instance.properties
ENV MANTA_MONITOR_VERSION 2.0.0

# Installed tools:
# ==============================================================================
# openssh-client:     for ssh-keygen to generate key fingerprints
# sudo:               for dropping from root to a lower permission user
# curl:               for downloading binaries
# ca-certifiactes:    for downloading via https
# zulu-8              OpenJDK certified by Azul
# libnss3             Native crypto tools for improving JVM crypo performance
# unzip:              for installing binaries
# dc:                 for calculating performance settings
# ==============================================================================

# Adds Azul Zulu OpenJDK repository
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9 && \
    echo 'deb http://repos.azulsystems.com/ubuntu stable main' > /etc/apt/sources.list.d/zulu.list

RUN export DEBIAN_FRONTEND=noninteractive && \
        apt-get update && \
        apt-get -qy upgrade && \
        apt-get install --no-install-recommends -qy \
                        sudo openssh-client curl ca-certificates zulu-11 libjna-java unzip libnss3 dc && \
        apt-get clean && \
        rm -rf /var/lib/apt/lists/* \
               /tmp/* \
               /var/tmp/* && \
        echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment

# Install Java Cryptography Extension Kit
# RUN curl --retry 6 -Ls "https://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip" > /tmp/ZuluJCEPolicies.zip && \
  #  echo "${ZULE_JCE_POLICY_CHECKSUM}  /tmp/ZuluJCEPolicies.zip" && \
   # unzip -o -j /tmp/ZuluJCEPolicies.zip -d $JAVA_HOME/lib/security ZuluJCEPolicies/US_export_policy.jar ZuluJCEPolicies/local_policy.jar && \
    #rm /tmp/ZuluJCEPolicies.zip

# Install CPU spoofing library
RUN mkdir -p /usr/local/numcpus/ && \
    curl --retry 6 -Ls "https://github.com/dekobon/libnumcpus/releases/download/1.0/libnumcpus-linux-x86_64.so" > /usr/local/numcpus/libnumcpus.so && \
    echo '6bc838db493d70a83dba9bd3d34c013d368339f288efc4629830fdcb25fa08f7  /usr/local/numcpus/libnumcpus.so' | sha256sum -c

# Add Native cryptography support via libnss and PKCS11
COPY docker_root/etc /etc
# Add supplemental system files
COPY docker_root/usr /usr
# Add application directory
COPY docker_root/opt /opt

# Download the manta-monitor binary from the repo
RUN curl --retry 7 --fail -Lso /tmp/manta-monitor.jar "https://github.com/joyent/manta-monitor/releases/download/v$MANTA_MONITOR_VERSION/manta-monitor-$MANTA_MONITOR_VERSION-jar-with-dependencies.jar" && \
    mv /tmp/manta-monitor.jar /opt/manta-monitor/lib/manta-monitor.jar && \
    chmod +x /opt/manta-monitor/lib/manta-monitor.jar

# Configure runtime user and permissions
RUN groupadd -g 1244 manta-monitor && \
    useradd -g 1244 -G sudo -u 1244 -c 'Manta Monitor User' -d /opt/manta-monitor -r -s /bin/false manta-monitor && \
    mkdir -p /opt/manta-monitor/.ssh && \
    chown -R manta-monitor:manta-monitor /opt/manta-monitor && \
    chmod og-rwx /opt/manta-monitor/.ssh && \
    chmod +x /usr/local/bin/proclimit && \
    mkdir -p /opt/manta-monitor/tmp && \
    find /opt/manta-monitor/bin -type f -exec chmod +x '{}' \;

EXPOSE 8090 8090

CMD /opt/manta-monitor/bin/run
