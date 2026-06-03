# Monitoring & Observability - Detailed Requirements

## 📋 Overview

Comprehensive monitoring and observability requirements for EasyOps ERP system to ensure reliability, performance, and quick issue resolution.

---

## 🎯 Core Pillars

### 1. **Metrics** (What's happening?)
- Quantitative measurements over time
- Performance indicators
- Resource utilization
- Business KPIs

### 2. **Logs** (What happened?)
- Event records
- Error messages
- Audit trails
- Debug information

### 3. **Traces** (Why did it happen?)
- Request flow across services
- Performance bottlenecks
- Error propagation
- Dependency visualization

### 4. **Health Checks** (Is it working?)
- Service availability
- Component status
- Dependency health
- Overall system health

---

## 📊 Monitoring Requirements

### Dashboard Requirements

#### 1. Real-Time System Overview Dashboard
**Purpose**: At-a-glance system health for operations team

**Panels Required:**
1. **Service Status Grid** (Top)
   - All microservices with green/red indicators
   - Uptime percentage (last 24h)
   - Last check timestamp
   - Quick stats (req/min, errors/min)

2. **Key Metrics Row**
   - Total Requests/sec (big number stat)
   - Average Response Time (big number stat)
   - Error Rate % (big number stat with threshold coloring)
   - Active Users (big number stat)

3. **Request Rate Chart** (Center)
   - Time series: Last 1 hour
   - Requests per minute by service
   - Stacked area chart
   - Auto-refresh every 5 seconds

4. **Error Rate Chart**
   - Time series: Last 1 hour  
   - Errors per minute by service
   - Line chart with threshold line at 5%

5. **Resource Utilization**
   - Gauge charts for CPU, Memory, Disk
   - Color coding: Green < 70%, Yellow < 85%, Red >= 85%

6. **Active Alerts Panel** (Bottom)
   - List of current alerts
   - Severity badges
   - Time since alert
   - Quick acknowledge action

#### 2. Service Detail Dashboards
**One dashboard per service**

**Panels:**
1. Service status and uptime
2. Request rate (last 24h)
3. Response time percentiles (p50, p95, p99)
4. Error count by endpoint
5. JVM memory usage
6. GC pause times
7. Database connection pool
8. Cache hit rate
9. Top 10 slowest endpoints
10. Recent errors log

#### 3. Business Metrics Dashboard
**Purpose**: Business intelligence and usage analytics

**Panels:**
1. **User Metrics**
   - Total users (gauge)
   - Active users today (gauge)
   - New registrations (time series)
   - Login success rate (time series)

2. **Organization Metrics**
   - Total organizations (gauge)
   - Organizations by plan (pie chart)
   - Active vs suspended (pie chart)
   - Storage usage by org (bar chart)

3. **API Usage**
   - API calls by service (bar chart)
   - Most used endpoints (table)
   - API calls trend (time series)
   - Failed API calls (time series)

4. **System Activity**
   - Actions per hour (heatmap)
   - Peak usage hours (bar chart)
   - Weekend vs weekday usage (comparison)

---

## 🚨 Alert Rules Specification

### Critical Alerts (Immediate Action Required)

#### 1. Service Down Alert
```yaml
Alert: service_down
Condition: up{job=~".+-service"} == 0
Duration: 2 minutes
Severity: CRITICAL
Action: 
  - Send email to ops team
  - Send Slack message
  - Create PagerDuty incident (production)
Message: "Service {{$labels.job}} is DOWN"
```

#### 2. High Error Rate
```yaml
Alert: high_error_rate
Condition: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.10
Duration: 1 minute
Severity: CRITICAL
Action:
  - Send email to dev team
  - Send Slack message
Message: "Error rate above 10% for {{$labels.service}}"
```

#### 3. Database Connection Pool Exhausted
```yaml
Alert: db_pool_exhausted
Condition: hikaricp_connections_pending > 5
Duration: 30 seconds
Severity: CRITICAL
Action:
  - Send email to ops and dev team
  - Trigger auto-scaling (if configured)
Message: "Database connection pool exhausted"
```

### Warning Alerts (Attention Needed)

#### 4. High Memory Usage
```yaml
Alert: high_memory_usage
Condition: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85
Duration: 5 minutes
Severity: WARNING
Action:
  - Send email to ops team
Message: "Memory usage above 85% for {{$labels.service}}"
```

#### 5. Slow Response Time
```yaml
Alert: slow_response_time
Condition: histogram_quantile(0.95, http_server_requests_duration_seconds) > 2
Duration: 5 minutes
Severity: WARNING
Action:
  - Send email to dev team
Message: "P95 response time above 2 seconds for {{$labels.service}}"
```

#### 6. Cache Hit Rate Low
```yaml
Alert: low_cache_hit_rate
Condition: redis_hit_rate < 0.80
Duration: 10 minutes
Severity: WARNING
Action:
  - Send email to dev team
Message: "Redis cache hit rate below 80%"
```

### Info Alerts (Informational)

#### 7. Service Restarted
```yaml
Alert: service_restart
Condition: changes(process_start_time_seconds[5m]) > 0
Duration: immediate
Severity: INFO
Action:
  - Log to system
  - Send Slack notification
Message: "Service {{$labels.service}} restarted"
```

---

## 📧 Notification Requirements

### Email Templates Needed

#### 1. User Invitation Template
**Template ID**: `user_invitation`
**Subject**: `You're invited to join {{organization.name}}`
**Variables:**
- organization.name
- organization.logo
- invited_by.name
- invitation.link
- invitation.role
- invitation.expiresAt

**Content:**
- Personalized greeting
- Who invited them
- What organization
- What role
- Call-to-action button
- Expiration notice
- Footer with unsubscribe

#### 2. Password Reset Template
**Template ID**: `password_reset`
**Subject**: `Reset your password for {{system.name}}`
**Variables:**
- user.name
- reset.link
- reset.expiresAt
- system.name

**Content:**
- Security notice
- Reset button
- Expiration time (1 hour)
- "Didn't request this?" notice
- Contact support link

#### 3. Welcome Email Template
**Template ID**: `welcome_email`
**Subject**: `Welcome to {{organization.name}}!`
**Variables:**
- user.name
- organization.name
- getting_started.link
- support.email

**Content:**
- Welcome message
- Quick start guide
- Key features overview
- Support information
- Next steps

#### 4. Organization Suspended Template
**Template ID**: `organization_suspended`
**Subject**: `Your organization has been suspended`
**Variables:**
- organization.name
- suspension.reason
- suspension.date
- contact.email

**Content:**
- Suspension notice
- Reason for suspension
- Action required
- Contact information
- Appeal process

---

## 🔍 Observability Best Practices

### Logging Standards
- **Log Levels Usage**
  - ERROR: System errors requiring immediate attention
  - WARN: Potential issues, degraded performance
  - INFO: Normal operations, significant events
  - DEBUG: Detailed information for debugging
  - TRACE: Very detailed information

- **What to Log**
  - All API requests/responses (with duration)
  - Authentication attempts (success/failure)
  - Database queries > 1 second
  - External API calls
  - Business transactions
  - Error stack traces
  - State changes

- **What NOT to Log**
  - Passwords or secrets
  - Credit card numbers
  - PII without redaction
  - Session tokens (full value)
  - API keys

### Metric Naming Conventions
- **Format**: `namespace_subsystem_name_unit`
- **Examples**:
  - `easyops_http_requests_total` (counter)
  - `easyops_http_request_duration_seconds` (histogram)
  - `easyops_users_active` (gauge)
  - `easyops_database_queries_total` (counter)

### Alert Naming Conventions
- **Format**: `[Severity] Service - Condition`
- **Examples**:
  - `[CRITICAL] Organization-Service - High Error Rate`
  - `[WARNING] API-Gateway - Slow Response Time`
  - `[INFO] User-Management - Service Restarted`

---

## 🎯 Performance Targets

### Response Time Targets
- **API Endpoints**: < 200ms (p95)
- **Database Queries**: < 100ms (p95)
- **Dashboard Load**: < 2 seconds
- **Notification Delivery**: < 1 second (in-app), < 10 seconds (email)

### Availability Targets
- **System Uptime**: 99.9% (8.76 hours downtime/year)
- **Service Uptime**: 99.95% per service
- **Email Delivery Rate**: 99% delivered within 1 minute
- **Notification Delivery**: 99.9% delivered immediately

### Scalability Targets
- **Concurrent Users**: 10,000+
- **API Requests**: 10,000 req/sec (aggregate)
- **Notifications**: 1,000/sec
- **Emails**: 100/minute
- **Webhook Deliveries**: 500/minute

---

## 🔗 Integration Points

### With Existing Services
- **Auth Service**: Password reset emails, 2FA codes
- **User Management**: Welcome emails, account updates
- **Organization Service**: Invitations, organization notifications
- **RBAC Service**: Role assignment notifications
- **All Services**: Health monitoring, metrics collection

### External Integrations
- **SMTP Providers**
  - SendGrid (primary)
  - AWS SES (backup)
  - Gmail (development)
  
- **Monitoring Tools**
  - Prometheus (metrics)
  - Grafana (visualization)
  - PagerDuty (incident management - production)
  - Slack (team notifications)

---

**Complete requirements for Phase 0.3 - Integration & Monitoring!**

