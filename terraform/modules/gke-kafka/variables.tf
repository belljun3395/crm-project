variable "strimzi_version" {
  description = "Strimzi Helm chart version"
  type        = string
  default     = "0.38.0"
}

variable "namespace" {
  description = "Namespace to deploy Strimzi and Kafka"
  type        = string
  default     = "kafka"
}

variable "cluster_name" {
  description = "Name of the Kafka cluster"
  type        = string
  default     = "gcp-kafka"
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.6.0"
}

variable "replicas" {
  description = "Number of Kafka/Zookeeper replicas"
  type        = number
  default     = 3
}
