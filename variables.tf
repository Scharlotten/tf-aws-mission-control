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
  default = "2gmHTRQro9LdwoahkghX5J1l4En"
}
