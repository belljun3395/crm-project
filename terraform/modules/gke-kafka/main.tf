resource "helm_release" "strimzi_operator" {
  name             = "strimzi-kafka-operator"
  repository       = "https://strimzi.io/charts/"
  chart            = "strimzi-kafka-operator"
  version          = var.strimzi_version
  namespace        = var.namespace
  create_namespace = true

  set {
    name  = "watchAnyNamespace"
    value = "true"
  }
}

resource "kubernetes_manifest" "kafka_cluster" {
  manifest = {
    apiVersion = "kafka.strimzi.io/v1beta2"
    kind       = "Kafka"
    metadata = {
      name      = var.cluster_name
      namespace = var.namespace
    }
    spec = {
      kafka = {
        version  = var.kafka_version
        replicas = var.replicas
        listeners = [
          {
            name = "plain"
            port = 9092
            type = "internal"
            tls  = false
          },
          {
            name = "tls"
            port = 9093
            type = "internal"
            tls  = true
          }
        ]
        config = {
          "offsets.topic.replication.factor"         = var.replicas
          "transaction.state.log.replication.factor" = var.replicas
          "transaction.state.log.min.isr"            = 1
          "default.replication.factor"               = var.replicas
          "min.insync.replicas"                      = 1
          "inter.broker.protocol.version"            = "3.6"
        }
        storage = {
          type = "jbod"
          volumes = [
            {
              id          = 0
              type        = "persistent-claim"
              size        = "10Gi"
              deleteClaim = false
            }
          ]
        }
      }
      zookeeper = {
        replicas = var.replicas
        storage = {
          type        = "persistent-claim"
          size        = "10Gi"
          deleteClaim = false
        }
      }
      entityOperator = {
        topicOperator = {}
        userOperator  = {}
      }
    }
  }

  depends_on = [helm_release.strimzi_operator]
}
