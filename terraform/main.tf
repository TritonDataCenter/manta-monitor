provider "triton" {
  version = "~> 0.4.2"

  account      = "${var.triton_account}"
  key_material = "${file(var.triton_key_path)}"
  key_id       = "${var.triton_key_id}"
  url          = "${var.triton_url}"
}

data "triton_network" "networks" {
  count = "${length(var.triton_network_names)}"
  name  = "${element(var.triton_network_names, count.index)}"
}

data "triton_image" "image" {
  name    = "${var.triton_image_name}"
  version = "${var.triton_image_version}"
}

data "template_file" "install_manta_monitor" {
  template = "${file("template_files/install_manta_monitor.sh.tpl")}"

  vars {
    container_name              = "${var.manta_monitor_container_name}"
    container_memory            = "${var.manta_monitor_container_memory}"
    jetty_server_port           = "${var.jetty_server_port}"
    java_env                    = "${var.java_env}"
    honeybadger_api_key         = "${var.honeybadger_api_key}"
    config_file                 = "${var.config_file}"
    manta_user                  = "${var.manta_user}"
    manta_public_key            = "${var.manta_public_key}"
    manta_private_key           = "${var.manta_private_key}"
    manta_url                   = "${var.manta_url}"
    manta_timeout               = "${var.manta_timeout}"
    manta_metric_reporter_mode  = "${var.manta_metric_reporter_mode}"
    manta_http_retries          = "${var.manta_http_retries}"
    enable_tls                  = "${var.enable_tls}"
    jetty_server_secure_port    = "${var.jetty_server_secure_port}"
    keystore_path               = "${var.keystore_path}"
    keystore_pass               = "${var.keystore_pass}"
    truststore_path             = "${var.truststore_path}"
    truststore_pass             = "${var.truststore_pass}"
  }

}

resource "triton_machine" "host" {
  package = "${var.triton_machine_package}"
  image = "${data.triton_image.image.id}"
  name = "${var.hostname}"

  firewall_enabled = true

  networks = ["${data.triton_network.networks.*.id}"]

  tags {
    role = "manta-monitor"
  }

  connection {
    type        = "ssh"
    user        = "${var.triton_ssh_user}"
    private_key = "${file("${var.triton_key_path}")}"
    agent       = false
  }

  provisioner "remote-exec" {
    inline = [
      "${data.template_file.install_manta_monitor.rendered}"
    ]
  }

}

resource "triton_firewall_rule" "ssh" {
  rule = "FROM any TO tag \"role\" = \"manta-monitor\" ALLOW tcp PORT 22"
  enabled     = true
  description = "SSH connection to manta-monitor instances"
}

resource "triton_firewall_rule" "http" {
  rule = "FROM any TO tag \"role\" = \"manta-monitor\" ALLOW tcp PORT ${var.enable_tls == "false" ? var.jetty_server_port : var.jetty_server_secure_port}"
  enabled     = true
  description = "http/https connection to manta-monitor instances"
}

output "primaryIp" {
  value = ["${triton_machine.host.*.primaryip}"]
}

