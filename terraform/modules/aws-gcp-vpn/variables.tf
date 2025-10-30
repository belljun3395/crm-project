variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "aws_vpc_id" {
  description = "AWS VPC ID"
  type        = string
}

variable "aws_private_route_table_ids" {
  description = "AWS private subnet route table IDs for VPN route propagation"
  type        = list(string)
}

variable "gcp_network_id" {
  description = "GCP VPC network ID"
  type        = string
}

variable "gcp_region" {
  description = "GCP region for VPN gateway"
  type        = string
}

variable "aws_bgp_asn" {
  description = "AWS BGP ASN"
  type        = number
  default     = 64512
}

variable "gcp_bgp_asn" {
  description = "GCP BGP ASN"
  type        = number
  default     = 65000
}

variable "tunnel1_inside_cidr" {
  description = "Inside CIDR for tunnel 1 (must be /30)"
  type        = string
  default     = "169.254.1.0/30"
}

variable "tunnel2_inside_cidr" {
  description = "Inside CIDR for tunnel 2 (must be /30)"
  type        = string
  default     = "169.254.2.0/30"
}

variable "tunnel1_preshared_key" {
  description = "Pre-shared key for tunnel 1"
  type        = string
  sensitive   = true
}

variable "tunnel2_preshared_key" {
  description = "Pre-shared key for tunnel 2"
  type        = string
  sensitive   = true
}

variable "advertised_ip_ranges" {
  description = "IP ranges to advertise from GCP to AWS"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags to apply to AWS resources"
  type        = map(string)
  default     = {}
}
