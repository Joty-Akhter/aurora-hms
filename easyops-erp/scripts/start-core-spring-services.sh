#!/bin/bash

# EasyOps ERP - Start Core Spring Boot Services (Eureka, Gateway, Auth, RBAC, Communication)
# Starts only the fundamental infrastructure microservices locally via Maven.
#
# Startup order: Eureka first (must be healthy for discovery), then Gateway, User Management,
# Auth, RBAC, Organization, and Communication are started in parallel to reduce total startup time.
#
# Prerequisites:
#   1. Core infrastructure (Postgres/Redis/Kafka) should already be running.
#      The docker-based start-core-services.sh script is the easiest way to achieve this.
#   2. Java 21+ and Maven Wrapper dependencies available.
#   3. Ports 8761 (Eureka), 8081 (Gateway), 8082 (User Management), 8083 (Auth), 8084 (RBAC), 8085 (Organization), and 8099 (Communication) must be free.
#   4. Run as your normal user (not sudo): service logs go to LOG_DIR and must stay user-writable.

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

# Core services to start in order
CORE_SERVICES=(
  "eureka"
  "api-gateway"
  "user-management"
  "auth-service"
  "rbac-service"
  "organization-service"
  "communication-service"
)

mkdir -p "$LOG_DIR" "$PID_DIR"

# If logs were created as root (e.g. sudo), existing *.log files are not user-writable; use a fallback dir.
_log_dir_unusable=false
if [ ! -w "$LOG_DIR" ]; then
  _log_dir_unusable=true
else
  for f in "$LOG_DIR"/*.log; do
    [ -f "$f" ] || continue
    if [ ! -w "$f" ]; then
      _log_dir_unusable=true
      break
    fi
  done
fi
if [ "$_log_dir_unusable" = true ]; then
  _requested_log_dir="$LOG_DIR"
  LOG_DIR="${TMPDIR:-/tmp}/easyops-local-services-${USER:-$(id -un)}"
  PID_DIR="$LOG_DIR/pids"
  mkdir -p "$LOG_DIR" "$PID_DIR"
  echo "⚠️  Log directory had unwritable files (often after running with sudo): $_requested_log_dir"
  echo "    Writing logs to: $LOG_DIR"
  echo "    To use the repo directory again: sudo chown -R \"\$(whoami)\" \"$_requested_log_dir\""
fi

# Appointment SMS requires Kafka (scheduling publishes → communication-service consumes).
wait_for_kafka() {
  local bootstrap="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
  local host="${bootstrap%%:*}"
  local port="${bootstrap##*:}"
  [ "$host" = "$port" ] && port=9092
  echo "⏳ Checking Kafka at ${host}:${port} (required for appointment SMS)..."
  local retries=30
  until (command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" 2>/dev/null) \
      || (docker ps --format '{{.Names}}' 2>/dev/null | grep -q easyops-kafka); do
    sleep 2
    retries=$((retries - 1))
    [ "$retries" -eq 0 ] && break
  done
  if [ "$retries" -eq 0 ]; then
    echo "⚠️  Kafka is not reachable at ${host}:${port}."
    echo "    Start infrastructure first, e.g.: cd \"$ROOT_DIR\" && docker-compose up -d postgres redis kafka"
    echo "    Appointment SMS events will not be delivered until Kafka is up."
  else
    echo "✅ Kafka is reachable"
  fi
  echo ""
}

declare -a STARTED_SERVICES=()
declare -a STARTED_PIDS=()

cleanup() {
  echo ""
  echo "🔻 Stopping core Spring Boot services..."
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
    if [ "$service" = "communication-service" ] && [ -z "${COMMUNICATION_LIQUIBASE_ENABLED+x}" ]; then
      # Communication schema is validated at startup; enable Liquibase by default so required tables exist.
      export COMMUNICATION_LIQUIBASE_ENABLED=true
    fi
    if [ "$service" = "communication-service" ] && [ -z "${COMMUNICATION_PHASE3_KAFKA_ENABLED+x}" ]; then
      # Enable Kafka consumer so appointment/invoice events are picked up and SMS is delivered.
      export COMMUNICATION_PHASE3_KAFKA_ENABLED=true
    fi
    "$MAVEN_CMD" spring-boot:run \
      -Dspring-boot.run.profiles="$PROFILE" \
      -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED" \
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

echo "🚀 Launching EasyOps Core Spring Boot services"
echo "   Root directory:   $ROOT_DIR"
echo "   Maven command:    $MAVEN_CMD"
echo "   Active profile:   $PROFILE"
echo "   Log directory:    $LOG_DIR"
echo "   Appointment SMS:  scheduling → Kafka → communication-service (Pondit: communication-service application.yml)"
echo

wait_for_kafka

echo "🔄 Preparing Maven wrapper distribution..."
if ! (cd "$ROOT_DIR" && "$MAVEN_CMD" -N -q --version >/dev/null 2>&1); then
  (cd "$ROOT_DIR" && "$MAVEN_CMD" -N --version >/dev/null 2>&1) || true
fi
echo "✅ Maven wrapper ready"
echo

# Internal modules (e.g. easyops-rbac-client) are not on Maven Central; install to ~/.m2 first.
echo "📦 Installing internal library artifacts to the local Maven repository..."
if ! (cd "$ROOT_DIR" && "$MAVEN_CMD" install -pl libraries/easyops-rbac-client -am -DskipTests -q); then
  echo "❌ Failed to install easyops-rbac-client."
  echo "   Try manually: cd \"$ROOT_DIR\" && ./mvnw install -pl libraries/easyops-rbac-client -am -DskipTests"
  exit 1
fi
echo "✅ Internal libraries ready"
echo

# Phase 1: Start Eureka and wait for it (required for discovery)
for service in "${CORE_SERVICES[@]}"; do
  start_service "$service"
  if [ "$service" = "eureka" ]; then
    wait_for_service "Eureka" "http://localhost:8761/actuator/health" 60
    echo ""
    break
  fi
done

# Phase 2: Start Gateway, User Management, Auth, RBAC, Organization, Communication in parallel (no health wait in between)
for service in api-gateway user-management auth-service rbac-service organization-service communication-service; do
  start_service "$service"
done

# Phase 3: Wait for all services started in parallel to be healthy
echo "⏳ Waiting for Gateway, User Management, Auth, RBAC, Organization, and Communication (started in parallel)..."
wait_for_service "API Gateway" "http://localhost:8081/actuator/health" 60
echo ""
wait_for_service "User Management Service" "http://localhost:8082/actuator/health" 60
echo ""
wait_for_service "Auth Service" "http://localhost:8083/actuator/health" 60
echo ""
wait_for_service "RBAC Service" "http://localhost:8084/actuator/health" 60
echo ""
wait_for_service "Organization Service" "http://localhost:8085/actuator/health" 60
echo ""
wait_for_service "Communication Service" "http://localhost:8099/actuator/health" 60
echo ""

echo ""
echo "Core services started. Press Ctrl+C to stop them."
echo ""
echo "Service URLs:"
echo "  - Eureka:      http://localhost:8761"
echo "  - API Gateway: http://localhost:8081"
echo "  - User Management Service: http://localhost:8082"
echo "  - Auth Service: http://localhost:8083"
echo "  - RBAC Service: http://localhost:8084"
echo "  - Organization Service: http://localhost:8085"
echo "  - Communication Service: http://localhost:8099"
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
