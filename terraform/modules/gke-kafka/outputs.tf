output "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers (internal)"
  value       = "${var.cluster_name}-kafka-bootstrap.${var.namespace}.svc:9092"
}
