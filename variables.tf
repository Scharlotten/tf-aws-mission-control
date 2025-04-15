# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "cluster_name" {
  description = "The name to give to the cluster to be provisioned"
  type = string
  default = "mc-cluster"
}

variable "loki_bucket" {
  description = "Logs for loki"
  type = string
  default = "loki-bucket-terraform"
}

variable "mimir_bucket" {
  description = "Logs for mimir"
  type = string
  default = "mimir-bucket-terraform"
}

variable "user_email" {
  description = "user email used for registration when submitting request for MC"
  type = string
  default = "anna.semjen@datastax.com"
}

variable "license_id" {
  description = "Password for mission-control this is in the license file"
  type = string
  default = "your_license"
}
variable username {
  description =  "A user name that you wish to use when tagging resources"
  type = string
  default = "asemjen"
}

variable "instance_type_db" {
  description = "The AWS instance type the DB should be provisioned on"
  type = string
  default = "r6i.xlarge"
 
}

variable "helm_override_file" {
  description = "the extra flie to overwrite default values in the helm installation"
  type = string
  default = "./override.yaml"
}