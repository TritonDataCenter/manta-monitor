# Manta Monitor Deployment Guide

This guide is intended to capture the steps necessary to deploy, configure, and run the 
[manta-monitor](https://github.com/joyent/manta-monitor/) application. For our purposes, we will be using a docker image 
to run the manta-monitor; this provides us with a simplified deployment methodology.


## Configuration of the Manta Monitor

There are two sources for configuration of the manta-monitor; the first source is the environment variables and the 
second is the JSON configuration file that is stored in Manta.


### Pre-requisites

In order to deploy manta-monitor you need to make sure:

1. You have access to a [Manta](https://apidocs.joyent.com/manta/index.html) installation.
2. You have access to a working manta end-point i.e. MANTA_URL (as described below).
3. You have access to a valid [Honeybadger](https://www.honeybadger.io/) account and have a corresponding 
   HONEYBADGER_API_KEY (as described below) at hand.
4. The machine where the application is being deployed and have a working 
   [Docker](https://www.docker.com/products/docker-desktop) that can access the MANTA_URL.
5. Utility tools specifically,  the JDK [keytool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html) 
   and [openssl](https://linux.die.net/man/1/openssl). Manta-monitor requires [Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
   or [OpenJDK-8](https://openjdk.java.net/install/), like [Zulu-8](https://docs.azul.com/zulu/zuludocs/Content/ZuluUserGuide/InstallingZulu/InstallZulu.htm). 
   Keytool is a part of the JDK and it comes along with the standard installation of the JDK. You can find if the system, 
   you want to run the keytool (or openssl), already has it by running the command:

    ```
    which keytool
    /usr/bin/keytool
    ```

    OR

    ```
    which openssl
    /usr/bin/openssl

    ```

### Environment Variables

The following environment variables need to be set:

<table>
    <tr>
        <td><strong>Parameter</strong>
        </td>
        <td><strong>Detail</strong>
        </td>
        <td><strong>Valid Values</strong>
        </td>
    </tr>
    <tr>
        <td><code>JAVA_ENV</code>
        </td>
        <td>[Required] Used by Honeybadger to report the environment used to run the application, when reporting exceptions
        </td>
        <td>"development", "production", or "test"
        </td>
    </tr>
    <tr>
        <td><code>HONEYBADGER_API_KEY</code>
        </td>
        <td>[Required] The key associated with the honeybadger account, required in order to report the exceptions to honeybadger.
        </td>
        <td>An API key from Honeybadger.io
        </td>
    </tr>
    <tr>
        <td><code>CONFIG_FILE</code>
        </td>
        <td>[Required] Absolute path to the json config file used to configure manta-monitor run time parameters.
        </td>
        <td>A fully qualified manta path, i.e. <code>"/user/stor/manta-monitor-config.json" </code>
        </td>
    </tr>
    <tr>
        <td><code>MANTA_USER</code>
        </td>
        <td>[Required] The user account that will be used by the application to connect to the manta object store.
        </td>
        <td>A valid Triton Public Cloud account login name.
        </td>
        </tr>
    <tr>
        <td><code>MANTA_PUBLIC_KEY</code>
        </td>
        <td>[Required] The public key of the MANTA_USER
        </td>
        <td>A valid SSH public key in the format:
                <p> <code> ssh-rsa XXXXXXXXX </code>
                <p> For eg: The following command can be used as a value for the variable.
                <p> <code> $(cat $HOME/.ssh/id_rsa.pub) </code>
        </td>
    </tr>
    <tr>
        <td><code>MANTA_PRIVATE_KEY</code>
        </td>
        <td>[Required] The private key of the MANTA_USER
        </td>
        <td>A valid SSH private key in the format:
            <p> <code> -----BEGIN RSA PRIVATE KEY----- </code>
            <p> <code> XXXXXXXXXXXX </code>
            <p> <code> -----END RSA PRIVATE KEY----- </code>
            <p> For eg: The following command can be used as a value for the variable.
            <p> For Ubuntu (Debian based OS):
            <p> $(cat $HOME/.ssh/id_rsa | base64 -w0)
            <p> For macOS:
            <p> $(cat $HOME/.ssh/id_rsa | base64 -b0)
        </td>
    </tr>
    <tr>
        <td><code>MANTA_URL</code>
        </td>
        <td>[Required] The manta endpoint where the application will put, get, and delete the generated test files
        </td>
        <td>A valid manta HTTPS endpoint, ie <a href="https://us-east.manta.joyent.com">https://us-east.manta.joyent.com</a>
            <p> It is expected that the machine, (or the docker container, if deployed using docker) be able to access this endpoint.
        </td>
    </tr>
    <tr>
        <td><code>MANTA_TIMEOUT</code>
        </td>
        <td>[Required] The time in milliseconds to wait until giving up when trying to make a new connection to Manta
        </td>
        <td>4000
        </td>
    </tr>
    <tr>
        <td><code>MANTA_METRIC_REPORTER_MODE</code>
        </td>
        <td>[Required] Format in which manta-client metrics are reported
        </td>
        <td>The possible options are:
            <ol>
                <li>JMX
                <li>SLF4J
                <li>DISABLED
            </ol>
            <p> For values any other than 'JMX', the application will exit with the error:
            <p> <code>Metric reporter mode must be set to JMX for Manta Monitor to operate correctly. Actual setting: SLF4J</code>
            <p> <code>Process finished with exit code 255.</code>
            <p> OR
            <p> <code>Metric reporter mode must be set to JMX for Manta Monitor to operate correctly. Actual setting: DISABLED</code>
            <p> <code>Process finished with exit code 255.</code>
            <p> Hence, the value to be set for this variable is 'JMX'.</li></ol>
         </td>
    </tr>
    <tr>
        <td><code>MANTA_HTTP_RETRIES</code>
        </td>
        <td>[Required] The number of times to retry failed HTTP requests.
        </td>
        <td>Any non zero positive integer value, in the range [1-5]. Preferred value '3'
        </td>
    </tr>
    <tr>
        <td><code>ENABLE_TLS</code>
        </td>
       <td>[Required] A boolean value that tells the application to run in either http or https mode. 
       A 'true' value indicates that the application will expose metrics over https, while a 'false' value indicates 
       that metrics will be exposed over http.
       </td>
       <td>true or false
       </td>
    </tr>
    <tr>
       <td><code>JETTY_SERVER_PORT</code>
       </td>
       <td>[Required, only when ENABLE_TLS=false] The port where the manta-monitor metrics will be exposed over http. 
       </td>
       <td>An unused port on the machine where manta-monitor is being deployed. Suggested value is 8090.
            <p> Note: The client that is trying to scrape the manta-monitor metrics at this port should be able to access the port.
       </td>
    </tr>
    <tr>
       <td><code>JETTY_SERVER_SECURE_PORT</code>
       </td>
       <td>[Required, only when <code>ENABLE_TLS=true</code>] The port where the manta-monitor metrics will be exposed over https.
       </td>
       <td>An unused port on the machine where manta-monitor is being deployed. Suggested value is 8443.The client that 
       is trying to scrape the manta-monitor metrics at this port should be able to access the port.
       </td>
    </tr>
    <tr>
       <td><code>KEYSTORE_PATH</code>
       </td>
       <td>[Required, only when <code>ENABLE_TLS=true</code>] A complete path to the PKCS12 formatted file, that contains the key and 
       certificate of the server and generated as shown <a href="#step-1-generate-a-keystore">here</a> that enables the
       application to participate in SSL connection.
       </td>
       <td>Eg:
            <p> /home/ubuntu/keystore
       </td>
    </tr>
    <tr>
       <td><code>KEYSTORE_PASS</code>
       </td>
       <td>[Required, only when <code>ENABLE_TLS=true</code>] A password string used to generate the above keystore.
       </td>
       <td>A valid alphanumeric password string, for eg: AdminPassword123, or, an alphanumeric password with special 
       characters '@', '#' and '!', for eg: Admin@Pass#123!. If the keystore was generated without a password i.e. 
       if the password was left blank, then set this value as " ".
       </td>
    </tr>
    <tr>
       <td><code>TRUSTSTORE_PATH</code>
       </td>
       <td>[Required, only when <code>ENABLE_TLS=true</code>] A complete path to the PKCS12 formatted file, that contains the key and
       certificate of the trusted client, that will connect to manta-monitor, and generated as shown <a href="#step-2-generate-a-truststore">here</a> 
       that enables the application to authenticate the client connection.
       </td>
       <td>Eg:
            <p> /home/ubuntu/truststore.pkcs12
       </td>
    </tr>
    <tr>
       <td><code>TRUSTSTORE_PASS</code>
       </td>
       <td>[Required, only when <code>ENABLE_TLS=true</code>] A password string used to generate the above truststore. 
       The application will need this to open the truststore to authenticate the incoming client key and certificate
       </td>
       <td>A valid alphanumeric password string, for eg: AdminPassword123, or, an alphanumeric password with special 
       characters '@', '#' and '!', for eg: Admin@Pass#123!. If the truststore was generated without a password i.e. if 
       the password was left blank, then set this value as " ".
       </td>
    </tr>
</table>


### JSON Configuration File

The[ JSON configuration file](https://github.com/joyent/manta-monitor/blob/master/src/test/resources/test-configuration.json) 
provides the necessary configuration for the application to run. Set within this configuration are the following:

<table>
    <tr>
        <td><strong>Parameter</strong>
        </td>
        <td><strong>Detail</strong>
        </td>
        <td><strong>Valid Values</strong>
        </td>
    </tr>
    <tr>
        <td><code>chainClassName</code>
        </td>
        <td>The name of the test class used to run the application.
        </td>
        <td>There are two possible values for this configuration setting:
        <ol>
            <li>com.joyent.manta.monitor.chains.FileUploadGetDeleteChain.
            <li>com.joyent.manta.monitor.chains.FileMultipartUploadGetDeleteChain
            </li>
        </ol>
        <p> The latter performs a <a href="https://us-east.manta.joyent.com/jhendricks/public/MANTA-3321/manta-docs/mpu-reference.html#multipart-upload-overview">Multipart upload </a>
        while the former performs a simple <a href="https://apidocs.joyent.com/manta/index.html#objects">mput</a>. 
        </td>
    </tr>
    <tr>
        <td><code>name</code>
        </td>
        <td>The name for the test runner.
        </td>
        <td>A string, with no special characters or numbers and separated by a '-'. A valid example is 'simple-put' OR 'put'.
        </td>
    </tr>
    <tr>
        <td><code>threads</code>
        </td>
        <td>The number of threads to be started in order to run the chain of commands
        </td>
        <td>An integer in the range of [1-5].
        <p> Suggested value of 5.
        </td>
    </tr>
    <tr>
       <td><code>minFileSize</code>
       </td>
       <td> Minimum size of the files, in bytes, to be uploaded to the manta object store. This value represents the 
       lower bound of the file size chosen to create a test file.
       </td>
       <td> 
       <p> If using the com.joyent.manta.monitor.chains.FileUploadGetDeleteChain, as chainClassName, this value can be 
       anything in the range of [1028 - 4096], with preferred value being 4096.</p>
       <p> If using com.joyent.manta.monitor.chains.FileMultipartUploadGetDeleteChain, as the chain class name, this value 
       can be in the range of [5242880 - 7340032], with 5242880 being the preferred value. </p>
       </td>
    </tr>
    <tr>
       <td><code>maxFileSize</code>
       </td>
       <td>Maximum size of the files, in bytes, to be uploaded to the manta object store. This value represents the 
       lower bound of the file size chosen to create a test file.
       </td>
       <td> 
       <p>If using com.joyent.manta.monitor.chains.FileUploadGetDeleteChain, as chainClassName, this value can be 
       anything in the range of [65536 - 4194304], with preferred value being 65536.
       <p> If using com.joyent.manta.monitor.chains.FileMultipartUploadGetDeleteChain, as the chain class name, 
       this value can be in the range of [5242880 - 10485760], with 5242880 being the preferred value.
       </td>
     </tr>
</table>


Note: If you want  fixed size  files to be created for testing uploads, then provide the same values for minFileSize and 
maxFileSize parameters. For example, to test with an upload size of 0.625MB using com.joyent.manta.monitor.chains.FileUploadGetDeleteChain, 
set the minFileSize and the mazFileSize to 65536.


### Honeybadger

[Honeybadger](https://www.honeybadger.io/) is a service that monitors properly instrumented applications. 
The service will provide information on errors and outages based on the data being reported from the manta-monitor deployment 
(or deployments) . It provides a GUI that displays the list of exceptions, along with the frequency  of occurrence and
provides a  detailed drill-down of the summary, context, and stack trace of the exception.

If you do not already have a project setup in honeybadger, i.e. if you are a first time user from your team, 
then you can start by signing up for a free trial [here](https://www.honeybadger.io/plans/#feature-grid). 
For a single user, the SOLO plan should be enough, however, if you are planning to set it up for use by more people 
in the team, then you can opt for any plan options that are listed, as per your needs. 
Once you signup, you will be prompted to select the language of the project, that you will be monitoring, as:


![](img/HoneybadgerChooseYourLanguage.png?raw=true)


Select 'Java' in the above window, then click 'Continue'.

The next window will give you the details of your api key, the one that you will use to set up the environment variable 
HONEYBADGER_API_KEY. Go ahead and click on 'Finish Setup'. You now have a honeybadger account. 
You can change the name of the project, from the default name of 'My Project' for editing the settings like:

![](img/HoneybadgerEditProjectSettings.png?raw=true)


Using the api key associated to your account and the project, you can start monitoring the errors and exceptions. For eg:

![](img/HoneybadgerErrorsList.png?raw=true)


As you can see from the screenshot above, each entry in the list is a link to a detailed explanation of the exception 
that includes a summary, the stacktrace from the exception, the MantaConfigContext (which is the context used to 
configure the manta client from manta-monitor), the system properties, the application environment, and the history of 
occurrence of the exception, as seen below:

![](img/HoneybadgerErrorStatus.png?raw=true)

![](img/HoneybadgerFullBackTrace.png?raw=true)

![](img/HoneybadgerErrorContext.png?raw=true)

![](img/HoneybadgerApplicationEnvironment.png?raw=true)

These details can be used for problem diagnosis and remediation. This data may also be useful if there are potential issues 
with the manta-sdk. 


### User Account

Manta-monitor is designed to run under a non-privileged account; it should NOT be run with an account that has 
administrative privileges. Additionally, the account should be dedicated to manta-monitor; that is, it should not be used 
for any other purposes. This will help isolate the files it creates and enable easier clean-up if required.

As stated earlier in the [pre-reqs](#pre-requisites), the user account that manta-monitor connects to manta with, needs 
to be a valid user account for the manta installation that is being monitored. . The account used for deployment of 
manta-monitor (ie, the docker account or OS account being used to run manta-monitor) is not required to be a valid 
triton account and as long as the account has privileges to run the docker container, that is used to deploy manta-monitor, 
as well as have access to the Manta account name and keys for the Manta user used for connection.


### QoS Headers

Once the QoS project is complete, the manta-monitor process should use a header to indicate that the traffic is from 
manta-monitor. This will help with collecting server side metrics, and also help isolate the traffic if needed.


### Passing TLS Certs

_Detail on how we exchange these, how the keystore works, etc._


#### Prerequisite:

For configuring manta-monitor to work in TLS mode you need the following keys and certificates in order to create a 
keystore and a truststore:

1. Server private key: A [PEM](https://en.wikipedia.org/wiki/Privacy-Enhanced_Mail) formatted private key, in the form:

    ```
    -----BEGIN PRIVATE KEY-----
    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    -----END PRIVATE KEY-----
    ```

2. Server certificate. A PEM formatted certificate, in the form:

    ```
    -----BEGIN CERTIFICATE-----
    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    -----END CERTIFICATE-----
    ```

3. Trusted key. Similar to the above, a PEM formatted private key of the host that will talk to manta-monitor for accessing 
the metrics end point. In case if prometheus is configured to scrape manta-monitor endpoint, at a https endpoint, this 
will be the value of the key_file attribute that is used in the prometheus config file.

4. Trusted certificate. Similar to the server certificate, a PEM formatted certificate of the host that will communicate 
with manta-monitor. In case if prometheus is configured to scrape manta-monitor endpoint, at a https endpoint, this will
be the value of the cert_file attribute that is used in the prometheus config file.

5. Utility tools specifically the JDK [keytool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html) 
and [openssl](https://linux.die.net/man/1/openssl).

Follow the steps below to create the server keystore and truststore.

##### Step 1: Generate a Keystore:

A keystore is a PKCS12 formatted file that contains the server's certificate and key. This keystore will be used when 
the manta-monitor server participates in the SSL handshake. 

The following single command uses the JDK's keytool to [generate a keystore](https://www.eclipse.org/jetty/documentation/9.4.x/configuring-ssl.html#generating-key-pairs-and-certificates-JDK-keytool):

```
keytool -keystore keystore -genkey -keyalg RSA -sigalg SHA256withRSA
 Enter keystore password:  password
 What is your first and last name?
   [Unknown]:  com.joyent.manta.monitor
 What is the name of your organizational unit?
   [Unknown]:  Manta Monitor
 What is the name of your organization?
   [Unknown]:  Joyent
 What is the name of your City or Locality?
   [Unknown]:
 What is the name of your State or Province?
   [Unknown]:
 What is the two-letter country code for this unit?
   [Unknown]:
 Is CN=jcom.joyent.manta.monitor, OU=Manta Monitor, O=Joyent, L=Unknown, ST=Unknown, C=Unknown correct?
   [no]:  yes

 Enter key password for <jetty>
         (RETURN if same as keystore password):
 $
```

The keytool command takes in the following arguments:

1. keystore: A string representing the name of the keystore.
2. genkey: This tells keytool to generate a PEM formatted key that will be stored in the keystore.
3. keyalg: The cryptographic algorithm used to generate the key and the certificate.
4. sigalg: The algorithm used to sign the generated self-signed certificate.

The command will prompt you to enter the certificate details and also passwords for the key and the keystore.
Please make note of the passwords that you enter, as we will need it later to run the application. 
For the purpose of this document, we will consider the password as "JoyPass123". 

Once the above keystore is created, we need to import the same in a PKCS12 formatted keystore.

```
keytool -importkeystore -srckeystore keystore -destkeystore keystore -deststoretype pkcs12
```

The above command takes as input the name of the sourcekeystore i.e. the keystore created above and the destkeystore 
i.e. the name of the new keystore that will be imported in the type specified by the parameter deststoretype.

NOTE: Enter password for exporting and importing the keystore and note down the path and password to the newly created destkeystore.

If you already have a PEM formatted key and certificate, you will still have to create a PKCS12 keystore. 
You can use ['openssl'](https://linux.die.net/man/1/openssl) to create a keystore out of your key and certificate as follows:

```
openssl pkcs12 -inkey server.key -in server.crt -export -out server.pkcs12
```

Here, server.key and server.crt are the PEM formatted key and certificate that you are supplying to create a server.pkcs12 file. 
(You will need any passwords/passphrases associated with the server.key and server.crt). The command will also prompt 
for password for exporting the new keystore. You can also choose to leave the password blank but it is not preferred,
as it leaves the certificate and the key exposed to anyone having access to the server.pkcs12 file.

##### Step 2: Generate a Truststore:

A truststore, unlike a keystore that enables a server to participate in SSL, enables a server to trust and thereby
authenticate a client. For eg, a primary use of manta-monitor is to provide a prometheus compatible end-point, 
hence in this case a prometheus server becomes a manta-monitor client. The client.key and the client.crt are the same 
files that are provided to the prometheus tls_config for key_file and cert_file attributes respectively. 
(For transferring the client.key and client.crt from a prometheus server follow the step [here](#step-2-transfer-the-key-and-certificate-from-the-prometheus-server-to-the-machine-hosting-the-manta-monitor-application))

The truststore will be created using client's PEM formatted key and certificate, in a similar way as a keystore was created above. 
For the purpose of this document, we will assume we already have a client.key and client.crt along with the 
passwords/passphrase associated with them. We can then create the truststore as follows:

```
openssl pkcs12 -inkey client.key -in client.crt -export -out client.pkcs12
```

Here, we use the openssl command to generate the truststore for manta-monitor. This command takes as input the 
PEM formatted key and certificate and produces a file in the pkcs12 format. The command will also prompt for password 
for exporting the new keystore. You can also choose to leave the password blank but it is not preferred, as it leaves the 
certificate and the key exposed to anyone having access to the client.pkcs12 file.

Note: Please note the path to the truststore and the password used to set up the truststore.

### Cleanup

To cleanup manta-monitor you first need to stop the manta-monitor process(es). Following that you can safely delete all 
of the data under /_ACCOUNT_/stor/ where ACCOUNT is the name of the account you are running manta-monitor from.

Make sure that your shell environment is set with the same MANTA env variables as used in the [configuration](#environment-variables). 

Verify if you see the manta-monitor-data directory under the user stor, as:

```
$ mls ~~/stor/manta-monitor-data/

03629ed6-bf92-4853-ad37-b48b6d41b91f/
081ba8c4-472a-47b8-91b6-b6cfcfcbd289/
111e9f0e-6e01-4fa6-b0f1-55652fc3e568/
12f3ebde-f03b-4640-b63a-7fdc7a73e62d/
```

Now use the mrm command with the -r option to clean the manta-monitor-data dir and its contents.

```
 mrm -r ~~/stor/manta-monitor-data/
```

## Deploying the Docker Container

Details on how the docker container is deployed, with emphasis on ease of use and the fact that this is an ephemeral deployment. 

Docker is the PREFERRED way to deploy the manta-monitor application, as it allows for easy configuration via the docker
environment variables and also easy maintenance. 


### Installing Docker

Follow the official docker guide [here](https://docs.docker.com/install/) or the below steps to install docker on a 
Ubuntu 16.04 machine.

First, in order to ensure the downloads are valid, add the GPG key for the official Docker repository to your system:

```
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
```

Add the Docker repository to APT sources:

```
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
```

Next, update the package database with the Docker packages from the newly added repo:

```
sudo apt-get update
```

Finally, install Docker

```
sudo apt-get install -y docker-ce
```

### Run via docker

Manta-monitor supports two modes of operation:

1. HTTP mode (insecured)
2. HTTPS mode (secured with TLS)

Before running manta-monitor in any of the above mode, we need to pull the latest docker image. 
Use the following command to pull the manta-monitor image from the joyent docker repository.

```
docker pull joyent/manta-monitor
```

Check the images that have been pulled.

```
docker images
```

The above should output something like this:

```
REPOSITORY     TAG                 IMAGE ID            CREATED           SIZE
joyent/manta-monitor latest      eecf7f0c652e        8 days ago          410MB
ubuntu       xenial              7e87e2b3bf7a        3 weeks ago         117MB
```

#### HTTP mode:

In order to run the application in insecure mode, follow the below steps to directly run the application using the above
joyent/manta-monitor image.

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

Note : 
1. Make sure to replace all the variables with the correct values.
2. Make sure that the value for JETTY_SERVER_PORT and -p matches as this will map the machine's port to the port in the
docker container.

#### HTTPS mode:

For configuring manta-monitor in HTTPS mode you need the keystore and the truststore, as generated in the steps [here](#passing-tls-certs)

Once you have the keystore and the truststore, run the application using the following command:

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
Here, we use docker volume to mount the complete path to the keystore (including the name of the keystore file) and the 
truststore, inside the container so as to make the stores available to the application during runtime.

An alternative way to avoid mounting the _path to the keystore and the path to the truststore _is to have the keystore 
and the truststore files uploaded to the manta store, similar to the way we do it for the CONFIG_FILE. For this, we will 
need to upload the generated PKCS12 formatted [keystore](#step-1-generate-a-keystore) and the [trustore](#step-2-generate-a-truststore) 
to the MANTA_USER store using the command:

```
mput -f <absolute-path-to-server-keystore> ~~/stor/keystore

mput -f <absolute-path-to-server-keystore> ~~/stor/truststore
```

Now we no longer need to mount the files to the docker container and the command will look like:

```
docker run -p 8443:8443 -d \
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
    -e ENABLE_TLS=true \
    -e KEYSTORE_PATH=manta:///user/stor/keystore \
    -e KEYSTORE_PASS=//XXXXXXXX \
    -e TRUSTSTORE_PATH=manta:///user/stor/truststore \
    -e TRUSTSTORE_PASS=XXXXXXXX \
    -e JETTY_SERVER_SECURE_PORT=8443 \
    joyent/manta-monitor
```

Note: In the above commands we exposed the secure server port (used for https). This secured port is given by the parameter 
JETTY_SERVER_SECURE_PORT. Make sure that the value for JETTY_SERVER_SECURE_PORT and the one used with -p match.

NOTE: Configuring Docker Log Rotation. 

The logs generated by manta-monitor takes up disk-space and over a longer period of running, can lead to the docker 
container exiting out due to memory issues. Hence, you can configure [docker log rotation](https://docs.docker.com/config/containers/logging/configure/) 
to take care of such a situation. Follow the below steps to configure log rotation on Linux hosts:

Add the following values in the /etc/docker/daemon.json. Create the file if it doesn't exist.

```
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
   }
}
```

### Prometheus Side Configuration

_How does prometheus find the manta-monitor to scrape it? What other data do we need to pass over, how do we adjust the config, etc._

#### Configuring prometheus to scrape manta-monitor metrics over http

A typical prometheus config for adding the job for scraping manta-monitor target over http will look like:

```
  - job_name: 'manta-monitor'
    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 5s

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
      - targets: ['localhost:8090']
```

#### Configuring prometheus to scrape manta-monitor metrics over https

To configure prometheus to scrape manta-monitor metrics over https, we will need PEM formatted prom.key and prom.cert files, 
that the prometheus server will use to communicate with manta-monitor over HTTPS.

##### Generate a no password key and certificate:

Prometheus require a passwordless key in order to be able to participate in a SSL handshake with the server.

Hence, if you are using a pre-existing key  make sure it is passwordless and in PEM format.

To check if the prom.key is passwordless, from the directory where the prom.key is stored, issue the following command:

```
openssl rsa -in prom.key -noout
```

If the above command asks for a passphrase then the prom.key is not passwordless. Hence, proceed to the following step 
to generate a key and a certificate.

Use the following command to generate a key and certificate 

```
openssl req  -nodes -new -x509  -keyout prom.key -out prom.crt
```

The output of the above command will be something like this:

```
Generating a 2048 bit RSA private key
............................+++
..............................+++
writing new private key to 'prom.key'
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) []:
State or Province Name (full name) []:
Locality Name (eg, city) []:
Organization Name (eg, company) []:
Organizational Unit Name (eg, section) []:
Common Name (eg, fully qualified host name) []:com.joyent.manta.monitor
Email Address []:
```

You only need the Common Name (fully qualified host name), in minimum, for successfully creating a certificate. 

This will create two files in the current directory a prom.key and a prom.crt. 

Use these files in the prometheus config.yml to setup tls_config as follows:

```
  - job_name: 'manta-monitor'

    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 5s

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
      - targets: ['localhost:8443']
    scheme: https
    tls_config:
      cert_file: prom.cert
      key_file: prom.key
      insecure_skip_verify: true
```

NOTE: Before prometheus with the above tls_config, can scrape manta-monitor metrics over https, we need to create a 
truststore, using the above prom.key and prom.cert, on the machine where manta-monitor will be deployed. 
Use the commands [here](#step-2-generate-a-truststore) to generate a manta-monitor truststore and run the manta-monitor 
following the steps for [https mode](#https-mode). 

##### Summary of commands to configure a prometheus server and manta-monitor to communicate over https:

###### Step 1: Get the key and certificate of the prometheus server

From the machine hosting the prometheus server, get the prom.key and prom.cert as described [here](#generate-a-no-password-key-and-certificate). 


###### Step 2: Transfer the key and certificate from the prometheus server to the machine hosting the manta-monitor application.

With key and cert now in hand, transfer them to the machine where manta-monitor is being deployed. You can use sftp to transfer the files as:

```
sftp user@manta-monitor-host
```

Browse to the directory where you already have the [keystore](#step-1-generate-a-keystore) 
(generate one if you do not already have) and put the prom.key and prom.cert` `

```
>put prom.key prom.key
>put prom.cert prom.cert
```

This will upload the key and certificate to the manta-monitor host. 

###### Step 3: Generate a truststore for manta-monitor

Now, follow the [steps](#step-2-generate-a-truststore) to generate the PKCS12 format truststore.

###### Step 4: Run manta-monitor

With the truststore now in place, and hopefully a keystore at hand too, use the [docker command](#https-mode), to run the application.

### Ensuring that Everything Works

_How do we know that things are working?_

1. Check the prometheus targets to see if the manta-monitor metrics are being scraped. On the machine where you have 
configured the prometheus to scrape manta-monitor endpoint, go the url [http://localhost:9090/targets](http://localhost:9090/targets) 
this will show you if the manta-monitor is up or down.

![](img/PrometheusHTTP-target.png?raw=true)


Or if you have configured both manta-monitor and prometheus to run in TLS mode then

![](img/PrometheusHTTPS-target.png?raw=true)

2. Check the manta-monitor endpoint using curl. Using the ipaddress of the machine where manta-monitor is being deployed 
and running, use the following command to query the api.
 ```
 curl -k -v http://<ip>:8090/metrics

 ```

The above command should result in a list of metrics similar to what is shown [here](#what-do-the-metrics-mean)

#### Testing with manta-monitor-test-harness

We also have a standalone app that can be run in order to test the validity of the manta-monitor end-point. 
The [manta-monitor-test-harness](https://github.com/1010sachin/manta-monitor-test-harness#manta-monitor-test-harness) app
connects to a given manta-monitor endpoint and validates the metrics. Follow the below steps to run the app:

##### Download the jar

```
wget https://github.com/1010sachin/manta-monitor-test-harness/releases/download/v1.0.0/manta-monitor-test-harness-1.0.0-jar-with-dependencies.jar
```

Once the jar is downloaded, make sure manta-monitor is up and running. If you have just started the manta-monitor wait for at least 5 minutes in order for the metrics to start coming in.

Follow the below steps to:

1. [Validate a manta-monitor http end-point.](#validating-http-endpoint)
2. [Validate a manta-monitor https end-point. ](#validating-https-endpoint)

##### Validating http endpoint.

Run the jar with command line options as:

```
java -jar manta-monitor-test-harness-1.0-jar-with-dependencies.jar "http://64.30.138.24:8090/metrics"
```

The above will produce the following output:

```
Manta-monitor endpoint http://64.30.138.24:8090/metrics is alive.

Writing all the found metrics to /home/ubuntu/opt/packages/manta-monitor-metrics.out at once

Found all the Requests-Get metrics
requests_get_FifteenMinuteRate
requests_get_mean
requests_get_95thPercentile
requests_get_OneMinuteRate
requests_get_99thPercentile
requests_get_75thPercentile
requests_get_FiveMinuteRate
requests_get_count
requests_get_50thPercentile

Found all the Requests-Delete metrics
requests_delete_50thPercentile
requests_delete_99thPercentile
requests_delete_FiveMinuteRate
requests_delete_75thPercentile
requests_delete_OneMinuteRate
requests_delete_count
requests_delete_mean
requests_delete_95thPercentile
requests_delete_FifteenMinuteRate

Found all the Requests-Put metrics
requests_put_75thPercentile
requests_put_FiveMinuteRate
requests_put_OneMinuteRate
requests_put_mean
requests_put_50thPercentile
requests_put_count
requests_put_99thPercentile
requests_put_95thPercentile
requests_put_FifteenMinuteRate

Found all the Retries metrics
retries_count
retries_mean_rate

Start Request-Get count is: 425925
Start Request-Put count is: 8093408
Start Request-Delete count is: 7666720

Manta-monitor endpoint http://64.30.138.24:8090/metrics is alive.

Found all the Requests-Get metrics
requests_get_FifteenMinuteRate
requests_get_mean
requests_get_95thPercentile
requests_get_OneMinuteRate
requests_get_99thPercentile
requests_get_75thPercentile
requests_get_FiveMinuteRate
requests_get_count
requests_get_50thPercentile

Found all the Requests-Delete metrics
requests_delete_50thPercentile
requests_delete_99thPercentile
requests_delete_FiveMinuteRate
requests_delete_75thPercentile
requests_delete_OneMinuteRate
requests_delete_count
requests_delete_mean
requests_delete_95thPercentile
requests_delete_FifteenMinuteRate

Found all the Requests-Put metrics
requests_put_75thPercentile
requests_put_FiveMinuteRate
requests_put_OneMinuteRate
requests_put_mean
requests_put_50thPercentile
requests_put_count
requests_put_99thPercentile
requests_put_95thPercentile
requests_put_FifteenMinuteRate

Found all the Retries metrics
retries_count
retries_mean_rate

Request-Get count increased by: 1. Last count was 425925 and current count is 425926
Request-Put count increased by: 20. Last count was 8093408 and current count is 8093428
Request-Delete count increased by: 46. Last count was 7666720 and current count is 7666766

Manta-monitor endpoint http://64.30.138.24:8090/metrics is alive.
```

##### Validating https endpoint

The test app can be used to validate a https connection to manta-monitor and it requires two additional parameters, 
the truststore path and the truststore password in addition to the https endpoint. For instance, if you have configured 
a prometheus server with the following tls_config:

```
tls_config:
      cert_file: prom.cert
      key_file: prom.key
```

And you have already created the manta-monitor truststore, say promtruststore, as shown [here](#step-2-generate-a-truststore), 
then you can use the test app to validate if the prom.cert and prom.key will be accepted during the TLS handshake as follows:

From the directory where the promtruststore is stored:

```
java -jar manta-monitor-test-harness-1.0-jar-with-dependencies.jar \
"https://localhost:8443/metrics" \
"promtruststore" \
"Promtruststore_password"
```

A successful output of the above command will be:

```
The provided certificate and key is accepted by the manta-monitor server at https://localhost:8443/metrics
Writing all the found metrics to /Users/sachingupta/Documents/Joyent/java-manta-monitor/manta-monitor-test-harness/manta-monitor-metrics.out at once

Found all the Requests-Get metrics
requests_get_FifteenMinuteRate
requests_get_mean
requests_get_95thPercentile
requests_get_OneMinuteRate
requests_get_99thPercentile
requests_get_75thPercentile
requests_get_FiveMinuteRate
requests_get_count
requests_get_50thPercentile

Found all the Requests-Delete metrics
requests_delete_50thPercentile
requests_delete_99thPercentile
requests_delete_FiveMinuteRate
requests_delete_75thPercentile
requests_delete_OneMinuteRate
requests_delete_count
requests_delete_mean
requests_delete_95thPercentile
requests_delete_FifteenMinuteRate

Found all the Requests-Put metrics
requests_put_75thPercentile
requests_put_FiveMinuteRate
requests_put_OneMinuteRate
requests_put_mean
requests_put_50thPercentile
requests_put_count
requests_put_99thPercentile
requests_put_95thPercentile
requests_put_FifteenMinuteRate

Found all the Retries metrics
retries_count
retries_mean_rate

Start Request-Get count is: 15
Start Request-Put count is: 285
Start Request-Delete count is: 194
```

## What do the Metrics Mean?

_A list of the metrics, what they mean, etc._

The following is an example of the raw metrics that manta-monitor will return; note that these are in
[prometheus format](https://prometheus.io/docs/instrumenting/exposition_formats/)


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
```

The metrics exposed by manta-monitor are of two types:

1. [Counter](https://prometheus.io/docs/concepts/metric_types/#counter) : A counter is a prometheus metric that represents 
single monotonically increasing value that can only increase and can be reset to zero on restart.
2. [Gauge](https://prometheus.io/docs/concepts/metric_types/#gauge) : A gauge is a metric that can arbitrarily go up and down.
3. [Histogram](https://prometheus.io/docs/concepts/metric_types/#histogram) : A histogram samples observations and count 
them in buckets. For manta-monitor it represents the request latency for putDirectory and putFile.


### Metrics Reported by manta-monitor:


<table>
    <tr>
        <td><strong>Metric </strong>
        </td>
        <td><strong>Type</strong>
        </td>
        <td><strong>Description</strong>
        </td>
    </tr>
    <tr>
        <td><code>requests_$METHOD_mean</code>
        </td>
        <td>Gauge
        </td>
        <td>
        Metric that gives the mean rate, as events/second, of the number of HTTP requests processed by the manta client
        since the start of the application. Values for $METHOD include GET, PUT, DELETE. These metrics do not provide a
        moving average, hence should be refrained from being considered for performance oriented graphs.
        </td>
    </tr>
    <tr>
        <td><code>requests_$METHOD_count</code>
        </td>
        <td>Counter
        </td>
        <td>
        Metric that gives the number of HTTP requests processed, by manta client, for GET, PUT and DELETE respectively.
        </td>
    </tr>
    <tr>
        <td><code>requests_$METHOD_$PERCENTILES</code>
        </td>
        <td>Gauge
        </td>
        <td>
        Metric that gives the percentile value of the time taken by the HTTP requests to process. These are measured in milliseconds. 
        Example values of percentile are 50thPercentile, 75thPercentile, 95thPercentile and 99thPercentile for GET, PUT and DELETE requests respectively.
        </td>
    </tr>
    <tr>
        <td><code>requests_$METHOD_$MINUTERATE</code>
        </td>
        <td>Gauge
        </td>
        <td>
        Metric that gives the rate of requests/second processed by the manta client. It provides rate in terms of 
        1-, 5- and 15- minute moving averages and is measured in events/second. Example values of MINUTE_RATE are:
        <p> OneMinuteRate,
        <p> FiveMinuteRate and
        <p> FifteenMinuteRate
        </td>
    </tr>
    <tr>
        <td><code>exceptions_$CLASS_$MINUTE_RATE</code>
        </td>
        <td>Gauge
        </td>
        <td>
        Metric that gives the FIFTEEN, FIVE and ONE minute rate of the exceptions that occurred while executing HTTP requests, 
        respectively and measured as events/second. Example values of $CLASS includes SocketTimeoutException, 
        NoHTTPResponseException and ConnectionClosedException.
        </td>
    </tr>
    <tr>
        <td><code>exceptions_$CLASS_count</code>
        </td>
        <td>Counter
        </td>
        <td>Metric that gives the count of the number of exceptions occurred while executing HTTP requests. 
        <p> Example values of $CLASS includes SocketTimeoutException, NoHTTPResponseException and ConnectionClosedException.
        </td>
    </tr>
    <tr>
        <td><code>retries_count</code>
        </td>
        <td>Counter
        </td>
        <td>Metric that gives the count of number of retries attempted by the manta client to process the HTTP requests.
        </td>
    </tr>
    <tr>
        <td><code>retires_mean_rate</code>
        </td>
        <td>Gauge
        </td>
        <td>Metric that gives the mean rate of the number of retries, measured by events/second, as occurred since the 
        start of the application. 
        </td>
    </tr>
    <tr>
        <td><code>mant_monitor_put_request_latency_seconds_$ChainClassName</code>
        </td>
        <td>Histogram
        </td>
        <td>Metric that observes the time elapsed for each request for put directory and put file and presents a 
        cumulative value, observed in buckets of 
        { 0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0 } seconds.
        </td>
    </tr>
</table>


For details about the metrics refer to the document [here](https://github.com/joyent/java-manta/blob/master/USAGE.md#monitoring).


## Troubleshooting

_What are the common issues that are encountered using this software? How does one address them?_

If you are using docker to run the application, it is always best to first check the running status of the docker container.

```
docker ps -a
```

A successful run of the above command should yield something like this:

```
CONTAINERID        IMAGE                  COMMAND                  CREATED       STATUS             PORTS                    NAMES
f93a70011018   joyent/manta-monitor   "/bin/sh -c /opt/man…"   8 days ago       Up 8 days         0.0.0.0:8090->8090/tcp   manta-monitor
```


The important fields to look at here are the container id and the status. Here, the container id is 

`f93a70011018 `and the status is shown as` Up `since` 8 days. `It also shows that the port `8090` of the container is 
mapped to the port `8090` of the host machine.

If the status shows as 'exited', then next step is to check the logs to see any exceptions. Check the logs using:

```
docker logs <container id from the docker ps command>
```

A common error can be missing an environment variable while running the application using the [docker command](#deploying-the-docker-container). 
For eg, when running in [http mode](#http-mode), you might miss to provide the env variable JETTY_SERVER_PORT. 
In this case upon 'docker logs' inspection we will see the following error:


```
2019-02-16 01:29:30,253 [main] INFO  com.jcabi.manifests.Manifests [lb:  reqId: ] - 3 attributes loaded from 1 stream(s) in 94ms, 3 saved, 0 ignored: ["Archiver-Version", "Created-By", "Manifest-Version"]
2019-02-16 01:29:31,354 [main] ERROR io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler [lb:  reqId: ] - An unhandled exception has occurred [49728573-7978-4042-bbcc-7f7e039dbd8b]
java.lang.IllegalArgumentException: Missing env variable JETTY_SERVER_PORT
	at com.joyent.manta.monitor.Application.validateJettyServerPort(Application.java:207)
	at com.joyent.manta.monitor.Application.main(Application.java:54)
```

This indicates that we are missing an env variable. Once you have figured out the reason for the docker container's exit,
before being able to run the docker run command again, you first need to remove the container or else you will get an 
error, something like this:


```
docker: Error response from daemon: Conflict. The container name "/manta-monitor" is already in use by container "c82ece32debae728bfaa0465567026dbe1f8d6ed92411dc64ddc27ce2e8201e3". You have to remove (or rename) that container to be able to reuse that name.
See 'docker run --help'.


```

Remove the existing container using the docker rm command with the container id as:

Get the container id:

```
docker ps -a
CONTAINER ID   IMAGE                 COMMAND                  CREATED    STATUS  PORTS  NAMES
c82ece32deba   joyent/manta-monitor  "/bin/sh -c /opt/man…"   6 minutes ago       Exited (1) 6 minutes ago manta-monitor

docker rm c82ece32deba
c82ece32deba
```

The above returns the container id that is being removed.

Another common error might be related to SSL, when run in https mode. For eg, upon docker logs inspection you might 
see something like:

```
java.lang.IllegalStateException: no valid keystore
	at org.eclipse.jetty.util.security.CertificateUtils.getKeyStore(CertificateUtils.java:50)
	at org.eclipse.jetty.util.ssl.SslContextFactory.loadTrustStore(SslContextFactory.java:1110)
	at org.eclipse.jetty.util.ssl.SslContextFactory.load(SslContextFactory.java:276)
	at org.eclipse.jetty.util.ssl.SslContextFactory.doStart(SslContextFactory.java:241)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:138)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:117)
	at org.eclipse.jetty.server.SslConnectionFactory.doStart(SslConnectionFactory.java:94)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:138)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:117)
	at org.eclipse.jetty.server.AbstractConnector.doStart(AbstractConnector.java:282)
	at org.eclipse.jetty.server.AbstractNetworkConnector.doStart(AbstractNetworkConnector.java:81)
	at org.eclipse.jetty.server.ServerConnector.doStart(ServerConnector.java:235)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.server.Server.doStart(Server.java:395)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at com.joyent.manta.monitor.MantaMonitorJerseyServer.start(MantaMonitorJerseyServer.java:60)
	at com.joyent.manta.monitor.Application.main(Application.java:72)
```

This might be because the keystore/trustore env variables are wrongly configured. In that case, check if the paths to 
the keystore and the truststore, as given by the env variables KEYSTORE_PATH and TRUSTSTORE_PATH are valid and contains 
the complete (absolute) paths to the PKCS12 formatted keystores, respectively. 

If the password for the keystores is incorrect, you will see the following error:

```
2019-02-17 09:35:21,899 [main] ERROR com.joyent.manta.monitor.Application [lb:  reqId: ] - Failed to start Embedded Jetty Server at port 0. Additional context is as follows:
Instance ID : 0c06fbc6-d6ff-4389-9d18-9814324f16d4
Platform Image : joyent_20160929T030056Z
Image ID : e331b22a-89d8-11e6-b891-936e4e1caa46
Server ID : 44454c4c-4400-1052-804d-b5c04f383432
Owner ID : bf80aadd-8616-c9c2-a074-8bdd028e7a7b
Alias : development-box
Datacenter Name : us-east-1
java.io.IOException: keystore password was incorrect
	at sun.security.pkcs12.PKCS12KeyStore.engineLoad(PKCS12KeyStore.java:2059)
	at sun.security.provider.KeyStoreDelegator.engineLoad(KeyStoreDelegator.java:238)
	at sun.security.provider.JavaKeyStore$DualFormatJKS.engineLoad(JavaKeyStore.java:70)
	at java.security.KeyStore.load(KeyStore.java:1445)
	at org.eclipse.jetty.util.security.CertificateUtils.getKeyStore(CertificateUtils.java:54)
	at org.eclipse.jetty.util.ssl.SslContextFactory.loadTrustStore(SslContextFactory.java:1110)
	at org.eclipse.jetty.util.ssl.SslContextFactory.load(SslContextFactory.java:276)
	at org.eclipse.jetty.util.ssl.SslContextFactory.doStart(SslContextFactory.java:241)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:138)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:117)
	at org.eclipse.jetty.server.SslConnectionFactory.doStart(SslConnectionFactory.java:94)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.start(ContainerLifeCycle.java:138)
	at org.eclipse.jetty.util.component.ContainerLifeCycle.doStart(ContainerLifeCycle.java:117)
	at org.eclipse.jetty.server.AbstractConnector.doStart(AbstractConnector.java:282)
	at org.eclipse.jetty.server.AbstractNetworkConnector.doStart(AbstractNetworkConnector.java:81)
	at org.eclipse.jetty.server.ServerConnector.doStart(ServerConnector.java:235)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at org.eclipse.jetty.server.Server.doStart(Server.java:395)
	at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:68)
	at com.joyent.manta.monitor.MantaMonitorJerseyServer.start(MantaMonitorJerseyServer.java:60)
	at com.joyent.manta.monitor.Application.main(Application.java:77)
Caused by: java.security.UnrecoverableKeyException: failed to decrypt safe contents entry: javax.crypto.BadPaddingException: Given final block not properly padded. Such issues can arise if a bad key is used during decryption.
	... 23 common frames omitted

Process finished with exit code 1
```

In this case, you might also want to confirm the password value used for the env variables TRUSTSTORE_PASS and KEYSTORE_PASS. 
You can use the keytool utility to test the keystore as:

```
keytool -list -keystore keystore -storepass password -storetype PKCS12 -noprompt -v
Keystore type: PKCS12
Keystore provider: SunJSSE

Your keystore contains 1 entry

Alias name: 1
Creation date: Jan 25, 2019
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=localhost, OU=Manta, O=Joyent, L=SF, ST=CA, C=US
Issuer: CN=localhost, OU=Manta, O=Joyent, L=SF, ST=CA, C=US
Serial number: f35a0459a12405ee
Valid from: Fri Jan 25 20:19:32 PST 2019 until: Sun Feb 24 20:19:32 PST 2019
Certificate fingerprints:
	 MD5:  XXXXXXXXXXXXX
	 SHA1: XXXXXXXXXXXXX
	 SHA256: XXXXXXXXXXXXX
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 1

*******************************************
*******************************************

