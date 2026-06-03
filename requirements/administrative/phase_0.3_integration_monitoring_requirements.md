# Phase 0.3 - Integration & Monitoring Requirements

## 📋 Overview

This document defines the comprehensive requirements for implementing System Integration, Monitoring, and Notification capabilities in EasyOps ERP. This completes Phase 0 - Administrative Foundation.

### Objectives
- **System Monitoring**: Real-time monitoring and alerting for all services
- **Notification System**: Email, in-app, and webhook notifications
- **API Documentation**: Interactive API documentation and testing tools
- **Observability**: Comprehensive logging, metrics, and tracing
- **Integration Tools**: Webhook management and third-party integration support

---

## 🔍 1. System Monitoring & Observability

### 1.1 Metrics Collection

#### Application Metrics (Prometheus)
- **HTTP Metrics**
  - Request count by endpoint
  - Response times (p50, p95, p99)
  - Error rates (4xx, 5xx)
  - Request throughput (req/sec)

- **JVM Metrics**
  - Heap memory usage
  - Non-heap memory usage
  - GC pause times
  - Thread count
  - CPU usage

- **Database Metrics**
  - Connection pool status
  - Query execution times
  - Transaction rates
  - Slow query tracking

- **Business Metrics**
  - Active users count
  - API calls per service
  - Organization count
  - User registrations per day

#### Service Health
- **Health Check Endpoints**
  - `/actuator/health` - Overall health
  - `/actuator/health/liveness` - Liveness probe
  - `/actuator/health/readiness` - Readiness probe
  - Component health (DB, Redis, Eureka)

- **Status Indicators**
  - UP: Service fully operational
  - DOWN: Service unavailable
  - OUT_OF_SERVICE: Temporarily offline
  - UNKNOWN: Health status unknown

### 1.2 Monitoring Dashboard (Grafana)

#### System Overview Dashboard
- **Service Status Panel**
  - All microservices status (green/red indicators)
  - Uptime percentage
  - Version information
  - Instance count

- **Resource Utilization**
  - CPU usage per service
  - Memory usage per service
  - Disk usage
  - Network I/O

- **Performance Metrics**
  - Average response time
  - Request rate per minute
  - Error rate percentage
  - P95/P99 latency

#### Service-Specific Dashboards
- **API Gateway Dashboard**
  - Total requests
  - Requests per route
  - Route-specific latency
  - Circuit breaker status

- **Database Dashboard**
  - Active connections
  - Connection pool utilization
  - Slow queries (> 1 second)
  - Transaction rate

- **Cache Dashboard**
  - Redis hit rate
  - Cache size
  - Eviction rate
  - Memory usage

#### Business Metrics Dashboard
- **User Metrics**
  - Active users (daily/monthly)
  - New registrations
  - Login success/failure rate
  - Session duration

- **Organization Metrics**
  - Total organizations
  - Active vs suspended
  - Subscription distribution
  - Storage usage per org

### 1.3 Alerting System

#### Alert Rules
- **Critical Alerts** (Immediate notification)
  - Service down for > 5 minutes
  - Database connection failure
  - Error rate > 10%
  - Memory usage > 90%
  - Disk usage > 85%

- **Warning Alerts**
  - Response time > 2 seconds
  - Error rate > 5%
  - Memory usage > 75%
  - Disk usage > 70%
  - Failed login attempts > 10/minute

- **Info Alerts**
  - New deployment
  - Service restart
  - Configuration change
  - Backup completion

#### Alert Channels
- Email notifications
- Slack integration
- PagerDuty (for production)
- In-app notifications
- SMS (critical only)

---

## 📧 2. Notification Service

### 2.1 Email Notifications

#### Email Types
- **Transactional Emails**
  - User registration/verification
  - Password reset
  - Organization invitation
  - Role assignment
  - Subscription changes
  - Payment receipts

- **System Notifications**
  - Service downtime alerts
  - Scheduled maintenance
  - Security alerts
  - Backup notifications
  - System updates

- **Business Notifications**
  - Daily/weekly reports
  - Invoice reminders
  - Approval requests
  - Task assignments

#### Email Configuration
- **SMTP Settings**
  - SMTP host/port
  - Authentication (username/password)
  - TLS/SSL support
  - Connection pooling
  - Retry logic

- **Email Templates**
  - HTML templates with variables
  - Plain text fallback
  - Multi-language support
  - Custom branding per organization
  - Template versioning

#### Email Service Entity
```sql
CREATE TABLE notifications.email_logs (
    id UUID PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    from_email VARCHAR(255),
    subject VARCHAR(255),
    template_name VARCHAR(100),
    template_variables JSONB,
    status VARCHAR(50), -- PENDING, SENT, FAILED, BOUNCED
    sent_at TIMESTAMP,
    failed_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    organization_id UUID,
    user_id UUID,
    created_at TIMESTAMP
);
```

### 2.2 In-App Notifications

#### Notification Entity
```sql
CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID,
    type VARCHAR(50), -- INFO, WARNING, ERROR, SUCCESS
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    priority VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    metadata JSONB
);
```

#### Notification Features
- **Real-time Delivery**
  - WebSocket connection
  - Server-Sent Events (SSE)
  - Long polling fallback
  - Push notifications ready

- **Notification Types**
  - System notifications
  - User mentions
  - Task assignments
  - Approval requests
  - Security alerts

- **User Preferences**
  - Email notification settings
  - In-app notification settings
  - Frequency preferences
  - Do not disturb mode

### 2.3 Notification Templates

#### Template System
- **Template Entity**
  - Template name (unique)
  - Subject template
  - Body template (HTML/plain text)
  - Variables/placeholders
  - Language/locale
  - Organization-specific override

- **Variable Substitution**
  - `{{user.name}}` - User name
  - `{{organization.name}}` - Org name
  - `{{invitation.link}}` - Invitation URL
  - `{{reset.token}}` - Password reset token
  - Custom variables per template

#### Default Templates
1. **User Invitation**
   - Subject: "You're invited to join {{organization.name}}"
   - Link to accept invitation
   - Role information
   - Expiration date

2. **Password Reset**
   - Subject: "Reset your password"
   - Reset link with token
   - Expiration time
   - Security notice

3. **Welcome Email**
   - Subject: "Welcome to {{organization.name}}"
   - Getting started guide
   - Contact information
   - Resources links

---

## 📚 3. API Documentation

### 3.1 Swagger/OpenAPI Integration

#### Configuration
- **Springdoc OpenAPI** for Spring Boot services
  - Auto-generated from code
  - Annotations for descriptions
  - Request/response examples
  - Authentication configuration

- **Documentation URL**
  - `/swagger-ui.html` - Swagger UI
  - `/v3/api-docs` - OpenAPI JSON
  - `/v3/api-docs.yaml` - OpenAPI YAML

#### API Documentation Features
- **Interactive Testing**
  - Try it out functionality
  - Authentication support
  - Request body examples
  - Response previews

- **Documentation Quality**
  - Clear endpoint descriptions
  - Parameter descriptions
  - Response schema documentation
  - Error code documentation
  - Example values

### 3.2 API Documentation Portal

#### Portal Features
- **Service Catalog**
  - List all microservices
  - Service descriptions
  - Base URLs
  - Version information
  - Status indicators

- **Interactive API Explorer**
  - Browse all endpoints
  - Test endpoints directly
  - View request/response
  - Download Postman collection
  - cURL command generation

- **API Reference**
  - Endpoint listing by service
  - Request/response schemas
  - Authentication guide
  - Error codes reference
  - Rate limiting information

---

## 🔗 4. Webhook Management

### 4.1 Webhook System

#### Webhook Entity
```sql
CREATE TABLE integration.webhooks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255), -- For HMAC signature
    events TEXT[], -- Array of event types
    is_active BOOLEAN DEFAULT true,
    retry_count INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    created_at TIMESTAMP,
    created_by UUID
);

CREATE TABLE integration.webhook_deliveries (
    id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL,
    event_type VARCHAR(100),
    payload JSONB,
    response_status INTEGER,
    response_body TEXT,
    delivered_at TIMESTAMP,
    retry_attempt INTEGER DEFAULT 0,
    success BOOLEAN,
    error_message TEXT
);
```

#### Webhook Features
- **Event Types**
  - `user.created`
  - `user.updated`
  - `organization.created`
  - `invitation.sent`
  - `role.assigned`
  - Custom events

- **Security**
  - HMAC signature validation
  - Secret key per webhook
  - IP whitelist (optional)
  - TLS/SSL required

- **Reliability**
  - Automatic retries (exponential backoff)
  - Delivery tracking
  - Failed delivery queue
  - Manual retry option

### 4.2 Webhook Management UI

#### Features
- Create/edit/delete webhooks
- Test webhook with sample payload
- View delivery history
- Retry failed deliveries
- Enable/disable webhooks
- Event type selection (multi-select)

---

## 📊 5. Log Aggregation

### 5.1 Centralized Logging

#### Log Collection
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
  - Centralized log storage
  - Real-time log streaming
  - Full-text search
  - Log retention policies

- **Log Levels**
  - ERROR: Critical errors
  - WARN: Warning messages
  - INFO: Informational
  - DEBUG: Debug information
  - TRACE: Detailed trace

#### Structured Logging
- **Log Format (JSON)**
  ```json
  {
    "timestamp": "2024-01-13T10:00:00Z",
    "level": "INFO",
    "service": "organization-service",
    "traceId": "abc123",
    "spanId": "def456",
    "userId": "user-uuid",
    "organizationId": "org-uuid",
    "message": "Organization created",
    "context": {
      "organizationCode": "ACME",
      "action": "CREATE"
    }
  }
  ```

### 5.2 Log Viewer UI

#### Features
- **Search and Filter**
  - Full-text search
  - Filter by service
  - Filter by level
  - Filter by date range
  - Filter by user/organization

- **Log Analysis**
  - Error rate trends
  - Common errors
  - Slow query detection
  - User activity tracking

---

## 🔧 6. System Administration

### 6.1 Service Health Monitoring

#### Monitoring Service (Port 8086)
- **Endpoints**
  - `GET /api/monitoring/services` - All service status
  - `GET /api/monitoring/services/{name}` - Specific service
  - `GET /api/monitoring/metrics/{service}` - Service metrics
  - `GET /api/monitoring/alerts` - Active alerts
  - `POST /api/monitoring/alerts/{id}/ack` - Acknowledge alert

#### Health Check Aggregator
- Poll all services every 30 seconds
- Aggregate health status
- Detect service degradation
- Historical uptime tracking

### 6.2 System Configuration

#### Configuration Management
- **System Settings Entity**
  ```sql
  CREATE TABLE system.settings (
      id UUID PRIMARY KEY,
      category VARCHAR(100),
      key VARCHAR(100) UNIQUE NOT NULL,
      value TEXT,
      data_type VARCHAR(50),
      is_encrypted BOOLEAN DEFAULT false,
      description TEXT,
      is_readonly BOOLEAN DEFAULT false,
      updated_at TIMESTAMP,
      updated_by UUID
  );
  ```

- **Setting Categories**
  - Email configuration
  - Notification preferences
  - Security settings
  - Feature flags
  - Integration settings

---

## 📱 7. Frontend Components

### 7.1 Monitoring Dashboard Page

#### Service Health Panel
- List of all services with status
- Uptime percentage
- Last check time
- Quick actions (restart, view logs)

#### Metrics Panel
- Request rate (line chart)
- Error rate (line chart)
- Response time (histogram)
- Resource usage (gauges)

#### Recent Alerts Panel
- Active alerts list
- Alert severity badges
- Acknowledge button
- View details link

### 7.2 Notifications Component

#### Notification Bell (Navbar)
- Badge with unread count
- Dropdown with recent notifications
- Mark as read
- View all notifications
- Notification settings

#### Notifications Page
- Full list of notifications
- Filter by type/status
- Bulk actions (mark all read, delete old)
- Notification preferences

### 7.3 API Documentation Page

#### API Explorer
- Service selector
- Endpoint browser
- Try it out interface
- Response viewer
- Example code generator

---

## 🛠️ 8. Technical Implementation

### 8.1 Technology Stack

#### Monitoring Stack
- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization and dashboards
- **Micrometer** - Metrics library for Spring Boot
- **Spring Boot Actuator** - Health and metrics endpoints

#### Notification Stack
- **JavaMail API** - Email sending
- **Thymeleaf** - Email template engine
- **WebSocket** - Real-time notifications
- **Spring Events** - Async event handling

#### Documentation Stack
- **Springdoc OpenAPI** - API documentation generation
- **Swagger UI** - Interactive API explorer
- **OpenAPI 3.0** - API specification standard

### 8.2 Database Schema

```sql
-- Notification Service Schema
CREATE SCHEMA IF NOT EXISTS notifications;

-- Notifications table
CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id),
    organization_id UUID REFERENCES admin.organizations(id),
    type VARCHAR(50) DEFAULT 'INFO',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Email logs table
CREATE TABLE notifications.email_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    to_email VARCHAR(255) NOT NULL,
    from_email VARCHAR(255),
    subject VARCHAR(255),
    template_name VARCHAR(100),
    template_variables JSONB,
    status VARCHAR(50) DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    failed_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    organization_id UUID,
    user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Email templates table
CREATE TABLE notifications.email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_html TEXT NOT NULL,
    body_text TEXT,
    variables TEXT[],
    locale VARCHAR(10) DEFAULT 'en-US',
    organization_id UUID, -- NULL for global templates
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Webhooks schema
CREATE SCHEMA IF NOT EXISTS integration;

CREATE TABLE integration.webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255),
    events TEXT[],
    is_active BOOLEAN DEFAULT true,
    retry_count INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE integration.webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID NOT NULL REFERENCES integration.webhooks(id),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    delivered_at TIMESTAMP WITH TIME ZONE,
    retry_attempt INTEGER DEFAULT 0,
    success BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- System metrics table
CREATE TABLE system.metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION,
    metric_type VARCHAR(50), -- COUNTER, GAUGE, HISTOGRAM, TIMER
    tags JSONB,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Alerts table
CREATE TABLE system.alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(50), -- CRITICAL, WARNING, INFO
    service_name VARCHAR(100),
    message TEXT NOT NULL,
    details JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📡 9. API Endpoints

### Notification Service (Port 8086)

#### Email Endpoints
```
POST   /api/notifications/email/send              - Send email
GET    /api/notifications/email/logs              - Get email logs
GET    /api/notifications/email/logs/{id}         - Get email log
POST   /api/notifications/email/retry/{id}        - Retry failed email

GET    /api/notifications/templates               - List templates
POST   /api/notifications/templates               - Create template
GET    /api/notifications/templates/{id}          - Get template
PUT    /api/notifications/templates/{id}          - Update template
DELETE /api/notifications/templates/{id}          - Delete template
```

#### In-App Notification Endpoints
```
GET    /api/notifications                         - Get user notifications
GET    /api/notifications/unread                  - Get unread count
PATCH  /api/notifications/{id}/read               - Mark as read
PATCH  /api/notifications/read-all                - Mark all as read
DELETE /api/notifications/{id}                    - Delete notification
POST   /api/notifications                         - Create notification (admin)
```

#### Webhook Endpoints
```
GET    /api/webhooks                              - List webhooks
POST   /api/webhooks                              - Create webhook
GET    /api/webhooks/{id}                         - Get webhook
PUT    /api/webhooks/{id}                         - Update webhook
DELETE /api/webhooks/{id}                         - Delete webhook
POST   /api/webhooks/{id}/test                    - Test webhook
GET    /api/webhooks/{id}/deliveries              - Get delivery history
POST   /api/webhooks/deliveries/{id}/retry        - Retry delivery
```

### Monitoring Service (Port 8087)

```
GET    /api/monitoring/services                   - All services health
GET    /api/monitoring/services/{name}            - Service health details
GET    /api/monitoring/metrics                    - System metrics summary
GET    /api/monitoring/metrics/{service}          - Service-specific metrics
GET    /api/monitoring/alerts                     - Active alerts
POST   /api/monitoring/alerts/{id}/acknowledge    - Acknowledge alert
GET    /api/monitoring/logs                       - Recent logs
POST   /api/monitoring/logs/search                - Search logs
```

---

## ✅ Acceptance Criteria

### Functional Requirements
- ✅ Real-time service health monitoring
- ✅ Prometheus metrics collection
- ✅ Grafana dashboards configured
- ✅ Email notifications working
- ✅ In-app notifications with WebSocket
- ✅ Email templates with variables
- ✅ Webhook management and delivery
- ✅ API documentation with Swagger
- ✅ Alert system with multiple channels
- ✅ Log aggregation and search

### Non-Functional Requirements
- ✅ Email delivery within 10 seconds
- ✅ Notification delivery < 1 second (WebSocket)
- ✅ Webhook delivery < 5 seconds
- ✅ Metrics collection every 30 seconds
- ✅ Dashboard load time < 2 seconds
- ✅ 99.9% notification delivery rate

### Technical Requirements
- ✅ Prometheus integration for all services
- ✅ Grafana dashboards for visualization
- ✅ SMTP configuration for emails
- ✅ WebSocket support for real-time notifications
- ✅ Swagger UI for all services
- ✅ Webhook retry with exponential backoff
- ✅ Comprehensive error handling

---

## 🧪 Testing Requirements

### Integration Tests
- Email sending and delivery
- Notification creation and delivery
- Webhook posting and retries
- Health check aggregation
- Alert triggering and acknowledgment

### Performance Tests
- Email throughput (emails/second)
- Notification delivery latency
- Webhook delivery under load
- Metrics collection overhead
- Dashboard query performance

---

## 📈 Success Metrics

### Technical Metrics
- Email delivery rate > 99%
- Notification delivery < 1 second
- Webhook success rate > 95%
- Metrics collection overhead < 5% CPU
- Dashboard load time < 2 seconds

### Business Metrics
- User engagement with notifications
- Email open rates
- Webhook integration adoption
- API documentation usage
- System uptime visibility

---

## 🔗 Dependencies

### External Services
- SMTP server (Gmail, SendGrid, AWS SES)
- Prometheus server
- Grafana server
- Elasticsearch (optional, for logs)

### Internal Services
- User Management Service
- Auth Service
- Organization Service
- All microservices (for monitoring)

---

## 📚 References

- Prometheus Documentation: https://prometheus.io/docs/
- Grafana Dashboards: https://grafana.com/docs/
- Springdoc OpenAPI: https://springdoc.org/
- JavaMail API: https://javaee.github.io/javamail/
- WebSocket Protocol: https://datatracker.ietf.org/doc/html/rfc6455

