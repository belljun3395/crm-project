variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "network_name" {
  description = "VPC network name."
  type        = string
}

variable "region" {
  description = "Region for subnet creation."
  type        = string
}

variable "subnet_ip_cidr_range" {
  description = "Primary subnet CIDR."
  type        = string
}

variable "subnet_secondary_pods_cidr" {
  description = "Secondary subnet range for pods."
  type        = string
}

variable "subnet_secondary_services_cidr" {
  description = "Secondary subnet range for services."
  type        = string
}

variable "ip_range_pods_name" {
  description = "Name for the secondary pods range."
  type        = string
}

variable "ip_range_services_name" {
  description = "Name for the secondary services range."
  type        = string
}

variable "labels" {
  description = "Labels applied to resources."
  type        = map(string)
  default     = {}
}
