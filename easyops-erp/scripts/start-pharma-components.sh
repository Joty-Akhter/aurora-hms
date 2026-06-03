#!/bin/bash

# EasyOps ERP - Start Pharma Components
# Starts Inventory, Accounting, HR, and Pharma services locally via Maven.
#
# Prerequisites:
#   1. Core infrastructure (Postgres/Redis) should already be running.
#   2. Core Spring services (Eureka, Gateway, Auth, RBAC) should already be running.
#   3. Java 21+ and Maven Wrapper dependencies available.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAVEN_CMD="${MAVEN_CMD:-$ROOT_DIR/mvnw}"
PROFILE="${SPRING_PROFILE:-local}"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/logs/local-services}"
PID_DIR="${PID_DIR:-$LOG_DIR/pids}"

# Default log pattern for consistent structured logs
DEFAULT_LOGGING_PATTERN_CONSOLE="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
DEFAULT_LOGGING_PATTERN_FILE="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

: "${LOGGING_PATTERN_CONSOLE:=$DEFAULT_LOGGING_PATTERN_CONSOLE}"
: "${LOGGING_PATTERN_FILE:=$DEFAULT_LOGGING_PATTERN_FILE}"
export LOGGING_PATTERN_CONSOLE LOGGING_PATTERN_FILE

# Ensure Eureka clients register with a host reachable from Docker containers.
: "${EUREKA_INSTANCE_HOSTNAME:=localhost}"
: "${EUREKA_INSTANCE_PREFER_IP_ADDRESS:=false}"
export EUREKA_INSTANCE_HOSTNAME
export EUREKA_INSTANCE_PREFER_IP_ADDRESS

# Pharma services to start (bank-service required for deposit bank account dropdown; organization-service is now part of core-spring services)
PHARMA_SERVICES=(
  "inventory-service"
  "accounting-service"
  "bank-service"
  "hr-service"
  "pharma-service"
)

mkdir -p "$LOG_DIR" "$PID_DIR"

declare -a STARTED_SERVICES=()
declare -a STARTED_PIDS=()
declare -a FAILED_SERVICES=()

cleanup() {
  echo ""
  echo "🔻 Stopping Pharma services..."
  for idx in "${!STARTED_PIDS[@]}"; do
    local pid="${STARTED_PIDS[$idx]}"
    local service="${STARTED_SERVICES[$idx]}"
    if kill -0 "$pid" >/dev/null 2>&1; then
      echo "  • $service (pid=$pid)"
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" 2>/dev/null || true
    fi
    rm -f "$PID_DIR/$service.pid"
  done
}

trap cleanup INT TERM EXIT

wait_for_service() {
  local service_name="$1"
  local health_url="$2"
  local max_retries="${3:-60}"
  
  echo "⏳ Waiting for $service_name ($health_url)..."
  local retries=$max_retries
  until curl -fs "$health_url" >/dev/null 2>&1 || [ $retries -eq 0 ]; do
    sleep 2
    retries=$((retries-1))
  done
  
  if [ $retries -eq 0 ]; then
    echo "⚠️  $service_name did not become healthy within timeout"
    return 1
  else
    echo "✅ $service_name is healthy"
    return 0
  fi
}

start_service() {
  local service="$1"
  local module_dir="$ROOT_DIR/services/$service"

  if [ ! -d "$module_dir" ] || [ ! -f "$module_dir/pom.xml" ]; then
    echo "⚠️  Skipping $service (module not found)"
    return
  fi

  local log_file="$LOG_DIR/${service}.log"
  echo "➡️  Starting $service (profile=$PROFILE)"
  echo "    → log: $log_file"

  (
    cd "$module_dir"
    "$MAVEN_CMD" clean >/dev/null 2>&1 || true
    export SPRING_PROFILES_ACTIVE="$PROFILE"
    export SPRING_REDIS_HOST="localhost"
    "$MAVEN_CMD" clean spring-boot:run \
      -Dspring-boot.run.profiles="$PROFILE" \
      -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED" \
      -Dspring.redis.host=localhost \
      -Dspring.data.redis.host=localhost \
      -DskipTests=true \
      -Deureka.instance.hostname="$EUREKA_INSTANCE_HOSTNAME" \
      -Deureka.instance.preferIpAddress="$EUREKA_INSTANCE_PREFER_IP_ADDRESS" \
      ${SPRING_BOOT_EXTRAS:-} \
      >"$log_file" 2>&1
  ) &

  local pid=$!
  echo "$pid" > "$PID_DIR/${service}.pid"
  STARTED_SERVICES+=("$service")
  STARTED_PIDS+=("$pid")
  sleep 2

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    echo "❌ $service terminated immediately. Check $log_file"
  else
    echo "⏳ $service process running (pid=$pid), waiting for health..."
  fi
}

echo "🚀 Launching EasyOps Pharma Components"
echo "   Root directory:   $ROOT_DIR"
echo "   Maven command:    $MAVEN_CMD"
echo "   Active profile:   $PROFILE"
echo "   Log directory:    $LOG_DIR"
echo

# Start services in order
for service in "${PHARMA_SERVICES[@]}"; do
  start_service "$service"
  
  port=0
  case "$service" in
    "organization-service") port=8085 ;;
    "inventory-service") port=8094 ;;
    "accounting-service") port=8088 ;;
    "bank-service") port=8092 ;;
    "hr-service") port=8096 ;;
    "pharma-service") port=8095 ;;
  esac

  if [ $port -gt 0 ]; then
    if ! wait_for_service "$service" "http://localhost:$port/actuator/health" 60; then
      FAILED_SERVICES+=("$service")
    fi
    echo ""
  fi
done

if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
  echo ""
  echo "❌ One or more services failed to start:"
  for s in "${FAILED_SERVICES[@]}"; do
    echo "   - $s (check $LOG_DIR/${s}.log)"
  done
  echo ""
  exit 1
fi

echo ""
echo "Pharma components started. Press Ctrl+C to stop them."
echo ""
echo "Service URLs:"
echo "  - Organization: http://localhost:8085"
echo "  - Inventory:    http://localhost:8094"
echo "  - Accounting:   http://localhost:8088"
echo "  - Bank:         http://localhost:8092"
echo "  - HR:           http://localhost:8096"
echo "  - Pharma:       http://localhost:8095"
echo ""

# Keep alive
while true; do
  any_running=false
  for pid in "${STARTED_PIDS[@]}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      any_running=true
      break
    fi
  done
  [ "$any_running" = false ] && break
  sleep 5
done
