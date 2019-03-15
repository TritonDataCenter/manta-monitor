# Manta Monitor Deployment Guide - Using Terraform

[Terraform](https://www.terraform.io) enables you to safely and predictably create, change, and improve production infrastructure. 
It is an open source tool that codifies APIs into declarative configuration files that can be shared amongst team members, 
treated as code, edited, reviewed, and versioned.

### Install Terraform
https://releases.hashicorp.com/terraform/0.11.13/terraform_0.11.13_linux_amd64.zip

```
# OS X using brew
brew install terraform

# Debian/Ubuntu/CentOS/RHEL
wget https://releases.hashicorp.com/terraform/0.11.13/terraform_0.11.13_linux_amd64.zip
unzip terraform_0.11.13_linux_amd64.zip
mv terraform /usr/local/bin/
```

Once installed, checkout the manta-monitor github repo locally and locate the terraform directory under manta-monitor/.

Create a terraform *.tfvars* file, in the terraform directory, and add the following contents to it. 
These are the variables used during application runtime.

```
hostname = "manta-monitor"
triton_account = "Replace with a valid triton account that will be billed for the instances created"
triton_key_path = "Replace with a valid path to the triton user's private key"
triton_key_id = "Replace with the md5 signature of the key added to the user's triton account"
triton_url = "https://us-sw-1.api.joyent.com"
manta_user = "Replace with a valid manta_user that will used to run the application"
manta_url = "https://us-east.manta.joyent.com"
jetty_server_port = "8090"
enable_tls = "Repalce with a sinlge value either true or false. True for secured mode false for unsecured." 
honeybadger_api_key = "Replace with a valid honeybadhger api key"
config_file = "Replace with a valid URL. Eg: manta:///sachin.gupta@joyent.com/stor/manta-monitor-config.json"
java_env = "production"
keystore_pass = "Replace with a valid password for accessing the keystore file"
truststore_pass = "Replace with a valid password for accessing the truststore file"
keystore_path = "Replace with a valid URL. Eg: manta:///sachin.gupta@joyent.com/stor/keystore"
truststore_path = "Replace with a valid URL. manta:///sachin.gupta@joyent.com/stor/prom.pkcs12"
jetty_server_secure_port = "8443"
```
You can find the description of these variables in the [file](../terraform/variables.tf)

### Terraform Init

From the terraform directory, run the terraform init command, this will install the required terraform plugins.

```
terraform init
```

### Terraform Plan

From the terraform directory run the following command to create a plan.

```
For OS X (Assuming the ssh keys are under $HOME/.ssh/)
terraform plan -var "manta_public_key=$(cat $HOME/.ssh/id_rsa.pub)" -var "manta_private_key=$(cat $HOME/.ssh/id_rsa | base64 -b0)" -var-file="env.tfvars" -out test.plan

For Debian/Ubuntu/CentOS/RHEL (Assuming the ssh keys are under $HOME/.ssh/)
terraform plan -var "manta_public_key=$(cat $HOME/.ssh/id_rsa.pub)" -var "manta_private_key=$(cat $HOME/.ssh/id_rsa | base64 -w0)" -var-file="env.tfvars" -out test.plan

```
This will output a file called *test.plan* in the current working directory.

### Terraform Apply

From the terraform directory run the following command to apply the plan, which will execute the provisioning of the instance,
apply firewall rules on the instance, install docker-ce and run manta-monitor.

```
terraform apply "test.plan"
```
The output of the above command will look like:
```
Apply complete! Resources: 3 added, 0 changed, 0 destroyed.

Outputs:

primaryIp = [
    165.225.157.249
]
```
You can use the primary ip to ssh into the newly created instance.

### Terraform Destroy

From the terraform directory run the following command to destroy the instance created above. This will also stop and 
destroy the docker container.

```
terraform destroy  -var-file="env.tfvars"
```