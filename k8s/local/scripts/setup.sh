#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="crm-local"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

# ────────────────────────────────────────────────
# 1. minikube 기동
# ────────────────────────────────────────────────
echo "▶ Starting minikube..."
if ! minikube status --profile minikube &>/dev/null; then
  minikube start \
    --driver=docker \
    --cpus=4 \
    --memory=8192 \
    --disk-size=30g \
    --kubernetes-version=stable
else
  echo "  minikube is already running"
fi

# Skaffold가 minikube 내부 Docker daemon에 이미지를 빌드할 수 있도록 환경변수 설정
eval "$(minikube docker-env)"

# ────────────────────────────────────────────────
# 2. Helm repo 추가
# ────────────────────────────────────────────────
echo "▶ Adding Helm repositories..."
helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update

# ────────────────────────────────────────────────
# 3. Namespace 생성
# ────────────────────────────────────────────────
echo "▶ Creating namespace '${NAMESPACE}'..."
kubectl apply -f "${PROJECT_ROOT}/k8s/local/namespace.yaml"

# ────────────────────────────────────────────────
# 4. Database init ConfigMap 적용
# ────────────────────────────────────────────────
echo "▶ Applying database init ConfigMap..."
kubectl apply -f "${PROJECT_ROOT}/k8s/local/mysql-init/configmap.yaml"

# ────────────────────────────────────────────────
# 5. Helm 인프라 설치
# ────────────────────────────────────────────────
echo "▶ Installing database (bitnami/mysql)..."
if ! helm status crm-mysql -n "${NAMESPACE}" &>/dev/null; then
  helm install crm-mysql bitnami/mysql \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/mysql.yaml" \
    --wait --timeout=5m
else
  echo "  crm-mysql already installed, upgrading..."
  helm upgrade crm-mysql bitnami/mysql \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/mysql.yaml" \
    --wait --timeout=5m
fi

echo "▶ Installing Redis Cluster (bitnami/redis-cluster)..."
if ! helm status crm-redis -n "${NAMESPACE}" &>/dev/null; then
  helm install crm-redis bitnami/redis-cluster \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/redis-cluster.yaml" \
    --wait --timeout=8m
else
  echo "  crm-redis already installed, upgrading..."
  helm upgrade crm-redis bitnami/redis-cluster \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/redis-cluster.yaml" \
    --wait --timeout=8m
fi

echo "▶ Installing Kafka (bitnami/kafka)..."
if ! helm status crm-kafka -n "${NAMESPACE}" &>/dev/null; then
  helm install crm-kafka bitnami/kafka \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/kafka.yaml" \
    --wait --timeout=5m
else
  echo "  crm-kafka already installed, upgrading..."
  helm upgrade crm-kafka bitnami/kafka \
    -n "${NAMESPACE}" \
    -f "${PROJECT_ROOT}/k8s/local/helm-values/kafka.yaml" \
    --wait --timeout=5m
fi

# ────────────────────────────────────────────────
# 6. 나머지 서비스 매니페스트 적용 (관리 UI)
# ────────────────────────────────────────────────
echo "▶ Applying remaining service manifests..."
kubectl apply -f "${PROJECT_ROOT}/k8s/local/adminer/"
kubectl apply -f "${PROJECT_ROOT}/k8s/local/kafka-ui/"
kubectl apply -f "${PROJECT_ROOT}/k8s/local/redis-insight/"

# ────────────────────────────────────────────────
# 7. 완료 안내
# ────────────────────────────────────────────────
echo ""
echo "✅ Infrastructure is ready!"
echo ""
echo "Next step — start development with Skaffold:"
echo ""
echo "  eval \$(minikube docker-env)   # minikube Docker 환경 활성화 (터미널당 1회)"
echo "  skaffold dev                  # 백엔드/프론트엔드 빌드 + 배포 + 포트포워드"
echo ""
echo "포트 매핑 (Docker Compose와 동일):"
echo "  Database : localhost:13306"
echo "  Adminer  : http://localhost:18080"
echo "  Redis    : crm-redis-headless (클러스터 내부)"
echo "  RedisUI  : http://localhost:18081"
echo "  Kafka    : localhost:9092"
echo "  Kafka UI : http://localhost:18082"
echo "  Backend  : http://localhost:8080"
echo "  Frontend : http://localhost:3000"
