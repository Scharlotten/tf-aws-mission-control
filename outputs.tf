# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.eks.cluster_endpoint
}

output "cluster_security_group_id" {
  description = "Security group ids attached to the cluster control plane"
  value       = module.eks.cluster_security_group_id
}

output "region" {
  description = "AWS region"
  value       = var.region
}

output "cluster_name" {
  description = "Kubernetes Cluster Name"
  value       = module.eks.cluster_name
}

output "arn_for_s3" {
  description = "Kubernetes Cluster Name"
  value       = aws_iam_policy.s3_policy_json.arn
}

output "certificate_authority" {
  description = "The certificate authority data that is needed in the kube config file"
  value = module.eks.cluster_certificate_authority_data
}

output "cluster_arn" {
  description = "The unique cluster arn that identifies the Kubernetes cluster"
  value = module.eks.cluster_arn
}

output "iam_role-database-node-group" {
  description = "The unique IAM service account that is created when the cluster node group is created"
  value = module.eks.eks_managed_node_groups.one.iam_role_arn
}

output "iam_role-platform-node-gropu" {
  description = "The unique IAM service account that is created when the cluster node group is created"
  value = module.eks.eks_managed_node_groups.two.iam_role_arn
}
