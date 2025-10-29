variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "cluster_version" {
  description = "EKS Kubernetes version."
  type        = string
  default     = "1.29"
}

variable "vpc_id" {
  description = "ID of the VPC that hosts the cluster."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs used for the worker nodes."
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "Public subnet IDs available for control plane to reach nodes when needed."
  type        = list(string)
  default     = []
}

variable "enable_cluster_public_access" {
  description = "Whether the EKS API server should be reachable from the public internet."
  type        = bool
  default     = false
}

variable "enable_cluster_private_access" {
  description = "Whether the EKS API server should be reachable from within the VPC."
  type        = bool
  default     = true
}

variable "cluster_public_access_cidrs" {
  description = "List of CIDR blocks allowed to reach the public API endpoint when public access is enabled."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "cluster_enabled_log_types" {
  description = "Control plane log types to enable."
  type        = list(string)
  default     = []
}

variable "node_group_desired_capacity" {
  description = "Desired number of nodes in the default node group."
  type        = number
  default     = 2
}

variable "node_group_min_size" {
  description = "Minimum number of nodes in the default node group."
  type        = number
  default     = 1
}

variable "node_group_max_size" {
  description = "Maximum number of nodes in the default node group."
  type        = number
  default     = 4
}

variable "node_group_instance_types" {
  description = "Instance types used for the default node group."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "node_group_capacity_type" {
  description = "Capacity type for the node group. Either ON_DEMAND or SPOT."
  type        = string
  default     = "ON_DEMAND"
}

variable "node_role_additional_policy_arns" {
  description = "Additional IAM policy ARNs to attach to the node group role."
  type        = list(string)
  default     = []
}

variable "cluster_additional_security_group_ids" {
  description = "Additional security groups to attach to the cluster ENIs."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Common tags applied to all resources."
  type        = map(string)
  default     = {}
}
