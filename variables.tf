variable "region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-1"
}

variable "user_email" {
  description = "user email used for registration when submitting request for MC"
  type = string
  default = "anna.semjen@datastax.com"
}

variable "license_id" {
  description = "Password for mission-control this is in the license file"
  type = string
  default = ""
}
variable "username" {
  description =  "A user name that you wish to use when tagging resources"
  type = string
  default = "asemjen"
}


variable "helm_override_file" {
  description = "the extra flie to overwrite default values in the helm installation"
  type = string
  default = "./mission-control-values/override.yaml"
}

variable "cluster_name" {
  description = "The name to give to the cluster to be provisioned"
  type = string
  default = "mc-cluster"
}