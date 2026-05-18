#!/usr/bin/env bash
# Usage:
#   ./demo-hpa.sh           - full demo against itinerary-service (requires full stack)
#   ./demo-hpa.sh --stress  - standalone CPU-stress demo (no stack required)
set -euo pipefail

NAMESPACE="tripplanning"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STRESS_MODE=false
[[ "${1:-}" == "--stress" ]] && STRESS_MODE=true

# ── Step 1: metrics-server ────────────────────────────────────────────────────
echo "=== Step 1: Install / verify metrics-server (Docker Desktop) ==="
if kubectl get deployment metrics-server -n kube-system &>/dev/null; then
  echo "metrics-server already present — skipping install."
else
  echo "Installing metrics-server..."
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

  echo "Patching for Docker Desktop (--kubelet-insecure-tls)..."
  kubectl patch deployment metrics-server -n kube-system \
    --type='json' \
    -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
fi

echo "Waiting for metrics-server to be ready..."
kubectl rollout status deployment/metrics-server -n kube-system --timeout=120s

# ── Step 2: namespace ─────────────────────────────────────────────────────────
echo ""
echo "=== Step 2: Ensure namespace '$NAMESPACE' exists ==="
kubectl get namespace "$NAMESPACE" &>/dev/null || kubectl create namespace "$NAMESPACE"

# ── Stress-only mode ──────────────────────────────────────────────────────────
if $STRESS_MODE; then
  echo ""
  echo "=== [STRESS MODE] Deploying standalone CPU-stress app ==="

  kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stress-demo
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: stress-demo
  template:
    metadata:
      labels:
        app: stress-demo
    spec:
      containers:
        - name: stress
          image: polinux/stress:latest
          command: ["stress"]
          args: ["--cpu", "1", "--timeout", "180s"]
          resources:
            requests:
              cpu: "200m"
              memory: "64Mi"
            limits:
              cpu: "400m"
              memory: "128Mi"
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: stress-demo-hpa
  namespace: $NAMESPACE
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: stress-demo
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 15
      policies:
        - type: Pods
          value: 2
          periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 1
          periodSeconds: 30
EOF

  echo ""
  echo "Waiting for stress-demo pod to be running..."
  kubectl rollout status deployment/stress-demo -n "$NAMESPACE" --timeout=60s

  echo ""
  echo "=== Watching HPA and pods — stress runs for 3 min then stops ==="
  echo "    CPU will spike → scale-out, then drop → scale-in after stabilization."
  echo "    Press Ctrl-C to stop watching."
  echo ""
  while true; do
    echo "--- $(date '+%H:%M:%S') ---"
    kubectl get hpa stress-demo-hpa -n "$NAMESPACE" 2>/dev/null || true
    kubectl get pods -n "$NAMESPACE" -l app=stress-demo --no-headers 2>/dev/null | awk '{print $1, $3}' || true
    echo ""
    sleep 10
  done
  exit 0
fi

# ── Full mode: itinerary-service ──────────────────────────────────────────────
echo ""
echo "=== Step 3: Apply itinerary-service deployment (with resource limits) ==="
kubectl apply -f "$DIR/itinerary-service-deployment.yaml"
kubectl rollout status deployment/itinerary-service -n "$NAMESPACE" --timeout=120s

echo ""
echo "=== Step 4: Apply HPA ==="
kubectl apply -f "$DIR/itinerary-service-hpa.yaml"
echo "HPA created:"
kubectl get hpa itinerary-service-hpa -n "$NAMESPACE"

echo ""
echo "=== Step 5: Baseline — pods before load ==="
kubectl get pods -n "$NAMESPACE" -l app=itinerary-service

echo ""
echo "=== Step 6: Start load generator (5 parallel workers × 3 min) ==="
kubectl delete job itinerary-load-generator -n "$NAMESPACE" --ignore-not-found=true
kubectl apply -f "$DIR/load-generator.yaml"

echo ""
echo "=== Step 7: Watching HPA and pods (Ctrl-C to stop) ==="
echo "    TARGETS shows current/threshold CPU%. Scale-out triggers at ≥50%."
echo ""
while true; do
  echo "--- $(date '+%H:%M:%S') ---"
  kubectl get hpa itinerary-service-hpa -n "$NAMESPACE" 2>/dev/null || true
  kubectl get pods -n "$NAMESPACE" -l app=itinerary-service --no-headers 2>/dev/null | awk '{print $1, $3}' || true
  echo ""
  sleep 10
done