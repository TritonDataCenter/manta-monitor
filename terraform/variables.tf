variable "hostname" {
  default = ""
  description = "The triton machine hostname."
}
variable "triton_account" {
  default     = ""
  description = "The Triton account name, usually the username of your root user."
}

variable "triton_key_path" {
  default     = ""
  description = "The path to a private key that is authorized to communicate with the Triton API."
}

variable "triton_key_id" {
  default     = ""
  description = "The md5 fingerprint of the key at triton_key_path. Obtained by running `ssh-keygen -E md5 -lf ~/path/to.key`"
}

variable "triton_url" {
  default     = ""
  description = "The CloudAPI endpoint URL. e.g. https://us-west-1.api.joyent.com"
}

variable "triton_network_names" {
  type        = "list"
  description = "List of Triton network names that the node(s) should be attached to."

  default = [
    "Joyent-SDC-Public"
  ]
}

variable "triton_service_name" {
  default     = "manta_monitor"
  description = "The name of the service in CNS."
}

variable "triton_image_name" {
  default     = "ubuntu-certified-16.04"
  description = "The name of the Triton image to use."
}

variable "triton_image_version" {
  default     = "20170307"
  description = "The version/tag of the Triton image to use."
}

variable "triton_ssh_user" {
  default     = "ubuntu"
  description = "The ssh user to use."
}

variable "triton_machine_package" {
  default     = "k4-general-kvm-7.75G"
  description = "The Triton machine package to use for this host. Defaults to k4-highcpu-kvm-1.75G."
}

variable "manta_user" {
  default     =  ""
  description = "The account name used to access the Manta service."
}

variable "manta_public_key" {
  default     =  ""
  description = "The public key asscoicted with the manta_user."
}

variable "manta_private_key" {
  default     =  ""
  description = "The corresponding private key of the manta_public_key."
}

variable "manta_url" {
  default     =  ""
  description = "The Manta endpoint eg: https://us-east.manta.joyent.com"
}

variable "manta_timeout" {
  default     =  "4000"
  description = "Time in milliseconds for the manta client to attempt to retry"
}

variable "manta_http_retries" {
  default     =  "3"
  description = "Number of times manta client attempts to retry before reporting error"
}

variable "manta_metric_reporter_mode" {
  default     = "JMX"
  description = "The modes in which the client will report metrics"
}

variable "config_file" {
  default     = ""
  description = "The path to the manta-monitor JSON configuration file. Eg: manta:///manta_user/store/config.json"
}

variable "honeybadger_api_key" {
  default     = ""
  description = "The Honeybadger API key associated with the honeybadger user account"
}

variable "java_env" {
  default     = "development"
  description = "The enviornment that will be reported to honeybadger for error reporting"
}

variable "jetty_server_port" {
  default     = ""
  description = "The port that will serve the manta-monitor metrics over http eg: 8090"
}

variable "jetty_server_secure_port" {
  default     = ""
  description = "The port that will serve the manta-monitor metrics over https eg: 8443"
}

variable "enable_tls" {
  default     = "false"
  description = "A boolean value that enables TLS mode for manta-monitor"
}

variable "keystore_path" {
  default     = ""
  description = "The path to the keystore file. Eg: manta:///manta_user/stor/keystore"
 }

variable "truststore_path" {
  default     = ""
  description = "The path to the truststore file. Eg: manta:///manta_user/stor/truststore"
}

variable "keystore_pass" {
  default     = ""
  description = "The passowrd to access the keystore file."
}

variable "truststore_pass" {
  default     = ""
  description = "The passowrd to access the truststore file."
}

variable "manta_monitor_container_name" {
  default = "manta-monitor"
  description = "The name of the docker container given during runtime"
}

variable "manta_monitor_container_memory" {
  default = "1G"
  description = "The maximum memory allocated to the docker container"
}
