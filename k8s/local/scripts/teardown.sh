#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="crm-local"

echo "▶ Removing Helm releases..."
helm uninstall crm-mysql  -n "${NAMESPACE}" 2>/dev/null || true
helm uninstall crm-redis  -n "${NAMESPACE}" 2>/dev/null || true
helm uninstall crm-kafka  -n "${NAMESPACE}" 2>/dev/null || true

echo "▶ Removing namespace..."
kubectl delete namespace "${NAMESPACE}" --ignore-not-found

echo "▶ Stopping minikube... (데이터를 완전히 삭제하려면 'minikube delete' 실행)"
minikube stop

echo "✅ Teardown complete."
