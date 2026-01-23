output "cluster_name" {
  description = "Name of the EKS cluster."
  value       = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  description = "Endpoint URL for the Kubernetes API server."
  value       = data.aws_eks_cluster.this.endpoint
}

output "cluster_certificate_authority_data" {
  description = "Certificate authority data required for kubeconfig."
  value       = data.aws_eks_cluster.this.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "Security group ID associated with the control plane."
  value       = aws_security_group.cluster.id
}

output "node_security_group_id" {
  description = "Security group ID associated with worker nodes."
  value       = aws_security_group.node.id
}

output "node_role_arn" {
  description = "IAM role ARN used by the node group."
  value       = aws_iam_role.node.arn
}

output "cluster_role_arn" {
  description = "IAM role ARN used by the control plane."
  value       = aws_iam_role.cluster.arn
}

output "oidc_provider_arn" {
  description = "IAM OIDC provider ARN attached to the cluster."
  value       = aws_iam_openid_connect_provider.this.arn
}
