#!/bin/bash
# EasyOps ERP - Stop Hospital Components
# Terminates only Hospital-related business services (Inventory, Accounting, HR, Hospital, Hospital Pharmacy).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${PID_DIR:-$ROOT_DIR/logs/local-services/pids}"
# Align with scripts/start-hospital-components.sh (no organization-service; includes hospital-billing-service, hospital-pharmacy-service, hospital-card-management-service, hospital-corporate-and-discount-service, hospital-clinical-orders-service, hospital-scheduling-service)
HOSPITAL_PORTS=(8094 8088 8096 8100 8111 8110 8090 8095 8092 8093)
HOSPITAL_SERVICES=("inventory-service" "accounting-service" "hr-service" "hospital-service" "hospital-billing-service" "hospital-pharmacy-service" "hospital-card-management-service" "hospital-corporate-and-discount-service" "hospital-clinical-orders-service" "hospital-scheduling-service")

echo "🛑 Stopping EasyOps Hospital components..."

STOPPED=0

# Method 1: PID files
if [ -d "$PID_DIR" ]; then
  for service in "${HOSPITAL_SERVICES[@]}"; do
    pid_file="$PID_DIR/$service.pid"
    if [ -f "$pid_file" ]; then
      pid=$(cat "$pid_file")
      if kill -0 "$pid" >/dev/null 2>&1; then
        echo "  • Stopping $service (PID: $pid)..."
        if kill "$pid" >/dev/null 2>&1; then
          rm -f "$pid_file"
          STOPPED=$((STOPPED + 1))
        fi
      else
        rm -f "$pid_file"
      fi
    fi
  done
fi

# Method 2: Port numbers (fallback)
for port in "${HOSPITAL_PORTS[@]}"; do
  pid=$(lsof -t -i:"$port" 2>/dev/null || true)
  if [ -n "$pid" ]; then
    echo "  • Stopping process on port $port (PID: $pid)..."
    kill -9 "$pid" 2>/dev/null || true
    STOPPED=$((STOPPED + 1))
  fi
done

# Method 3: Command line patterns (fallback)
for service in "${HOSPITAL_SERVICES[@]}"; do
  pids=$(pgrep -f "$service.*spring-boot:run" || true)
  if [ -n "$pids" ]; then
    for pid in $pids; do
      echo "  • Stopping $service by pattern (PID: $pid)..."
      kill -9 "$pid" 2>/dev/null || true
      STOPPED=$((STOPPED + 1))
    done
  fi
done

echo "✅ Done. Stopped $STOPPED hospital-related process(es)."
