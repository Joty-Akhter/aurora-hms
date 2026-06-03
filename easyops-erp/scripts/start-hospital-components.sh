#!/bin/bash

# EasyOps ERP - Start Hospital Components
# Starts Inventory, Accounting, HR, and Hospital services locally via Maven.
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

# Hospital services to start (business-domain services; organization-service is now part of core-spring services)
HOSPITAL_SERVICES=(
  "inventory-service"
  "accounting-service"
  "hr-service"
  "hospital-service"
  "hospital-billing-service"
  "hospital-pharmacy-service"
  "hospital-card-management-service"
  "hospital-corporate-and-discount-service"
  "hospital-clinical-orders-service"
  "hospital-scheduling-service"
)

mkdir -p "$LOG_DIR" "$PID_DIR"

wait_for_kafka() {
  local bootstrap="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
  local host="${bootstrap%%:*}"
  local port="${bootstrap##*:}"
  [ "$host" = "$port" ] && port=9092
  echo "⏳ Checking Kafka at ${host}:${port} (hospital-scheduling publishes appointment SMS events here)..."
  local retries=30
  until (command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" 2>/dev/null) \
      || (docker ps --format '{{.Names}}' 2>/dev/null | grep -q easyops-kafka); do
    sleep 2
    retries=$((retries - 1))
    [ "$retries" -eq 0 ] && break
  done
  if [ "$retries" -eq 0 ]; then
    echo "⚠️  Kafka is not reachable. COMM_APPOINTMENT_SMS_ENABLED will still be set, but SMS will not flow until Kafka and communication-service are running."
  else
    echo "✅ Kafka is reachable"
  fi
  echo ""
}

declare -a STARTED_SERVICES=()
declare -a STARTED_PIDS=()

cleanup() {
  echo ""
  echo "🔻 Stopping Hospital services..."
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
    if [ "$service" = "hospital-scheduling-service" ] && [ -z "${COMM_APPOINTMENT_SMS_ENABLED+x}" ]; then
      # Publish appointment SMS events to Kafka so communication-service can deliver them.
      export COMM_APPOINTMENT_SMS_ENABLED=true
    fi
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
    echo "✅ $service started (pid=$pid)"
  fi
}

echo "🚀 Launching EasyOps Hospital Components"
echo "   Root directory:   $ROOT_DIR"
echo "   Maven command:    $MAVEN_CMD"
echo "   Active profile:   $PROFILE"
echo "   Log directory:    $LOG_DIR"
echo "   Appointment SMS:  COMM_APPOINTMENT_SMS_ENABLED=true on hospital-scheduling-service (override with export COMM_APPOINTMENT_SMS_ENABLED=false)"
echo

wait_for_kafka

# Start services in order
for service in "${HOSPITAL_SERVICES[@]}"; do
  start_service "$service"
  
  port=0
  case "$service" in
    "organization-service") port=8085 ;;
    "inventory-service") port=8094 ;;
    "accounting-service") port=8088 ;;
    "hr-service") port=8096 ;;
    "hospital-service") port=8100 ;;
    "hospital-billing-service") port=8111 ;;
    "hospital-pharmacy-service") port=8110 ;;
    "hospital-card-management-service") port=8090 ;;
    "hospital-corporate-and-discount-service") port=8095 ;;
    "hospital-clinical-orders-service") port=8092 ;;
    "hospital-scheduling-service") port=8093 ;;
  esac

  if [ $port -gt 0 ]; then
    # hospital-service: first DB migration includes heavy Liquibase seed (~3+ min); default 60*2s is too short.
    max_retries=60
    if [ "$service" = "hospital-service" ]; then
      max_retries=300
    fi
    if [ -n "${EASYOPS_HEALTH_MAX_RETRIES:-}" ]; then
      max_retries="$EASYOPS_HEALTH_MAX_RETRIES"
    fi
    wait_for_service "$service" "http://localhost:$port/actuator/health" "$max_retries"
    echo ""
  fi
done

echo ""
echo "Hospital components started. Press Ctrl+C to stop them."
echo ""
echo "Service URLs:"
echo "  - Organization: http://localhost:8085"
echo "  - Inventory:    http://localhost:8094"
echo "  - Accounting:   http://localhost:8088"
echo "  - HR:           http://localhost:8096"
echo "  - Hospital:          http://localhost:8100"
echo "  - Hospital Billing:  http://localhost:8111"
echo "  - Hospital Pharmacy: http://localhost:8110"
echo "  - Hospital Card Mgmt: http://localhost:8090"
echo "  - Hospital Corporate/Discount: http://localhost:8095"
echo "  - Hospital Clinical Orders: http://localhost:8092"
echo "  - Hospital Scheduling: http://localhost:8093"
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
