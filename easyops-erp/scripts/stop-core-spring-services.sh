#!/bin/bash

# EasyOps ERP - Stop Core Spring Boot Services
# Terminates only core infrastructure microservices (Eureka, API Gateway, User Management, Auth, RBAC, Organization, Communication).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${PID_DIR:-$ROOT_DIR/logs/local-services/pids}"

# Align with scripts/start-core-spring-services.sh
CORE_PORTS=(8761 8081 8082 8083 8084 8085 8099)
CORE_SERVICES=("eureka" "api-gateway" "user-management" "auth-service" "rbac-service" "organization-service" "communication-service")

echo "🛑 Stopping EasyOps Core Spring Boot services..."

STOPPED=0

# Method 1: PID files
if [ -d "$PID_DIR" ]; then
  for service in "${CORE_SERVICES[@]}"; do
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

# Method 2: Port numbers (fallback; requires lsof)
if command -v lsof >/dev/null 2>&1; then
  for port in "${CORE_PORTS[@]}"; do
    pid=$(lsof -t -i:"$port" 2>/dev/null || true)
    if [ -n "$pid" ]; then
      echo "  • Stopping process on port $port (PID: $pid)..."
      kill -9 "$pid" 2>/dev/null || true
      STOPPED=$((STOPPED + 1))
    fi
  done
fi

# Method 3: Command line patterns (fallback; requires pgrep)
if command -v pgrep >/dev/null 2>&1; then
  for service in "${CORE_SERVICES[@]}"; do
    pids=$(pgrep -f "$service.*spring-boot:run" || true)
    if [ -n "$pids" ]; then
      for pid in $pids; do
        echo "  • Stopping $service by pattern (PID: $pid)..."
        kill -9 "$pid" 2>/dev/null || true
        STOPPED=$((STOPPED + 1))
      done
    fi
  done
fi

echo "✅ Done. Stopped $STOPPED core Spring Boot process(es)."

