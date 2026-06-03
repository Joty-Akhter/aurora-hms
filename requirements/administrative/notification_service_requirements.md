# Notification Service - Detailed Requirements

## 📋 Overview

The Notification Service is a critical component that handles all communication with users through multiple channels (email, in-app notifications, SMS). It provides a unified interface for sending notifications and managing notification templates.

### Objectives
- **Multi-Channel Communication**: Support email, in-app, SMS, and push notifications
- **Template Management**: Flexible template system with variable substitution
- **Reliable Delivery**: Retry logic, delivery tracking, and failure handling
- **User Preferences**: Allow users to control notification settings
- **Audit Trail**: Complete tracking of all notifications sent

---

## 📧 1. Email Notification System

### 1.1 Core Requirements

#### Email Sending
- **SMTP Configuration**
  - Support multiple SMTP providers (Gmail, SendGrid, AWS SES, Mailgun)
  - Connection pooling for performance
  - TLS/SSL encryption
  - Authentication (username/password, API key)
  - Configurable timeout and retry settings

- **Email Composition**
  - HTML email support
  - Plain text fallback
  - Inline images and attachments
  - Custom headers
  - Reply-to address
  - CC and BCC support

- **Delivery Tracking**
  - Sent status
  - Delivery confirmation (if supported by provider)
  - Bounce handling
  - Open tracking (optional)
  - Click tracking (optional)

#### Required Email Types

1. **User Invitation Email**
   - Sent when: User is invited to organization
   - Contains: Invitation link, organization name, role, expiry date
   - Action: "Accept Invitation" button
   - Priority: HIGH

2. **Password Reset Email**
   - Sent when: User requests password reset
   - Contains: Reset link with token, expiration time
   - Action: "Reset Password" button
   - Priority: HIGH
   - Security: Token expires in 1 hour

3. **Welcome Email**
   - Sent when: User account is created
   - Contains: Welcome message, getting started guide, resources
   - Action: "Get Started" button
   - Priority: MEDIUM

4. **Email Verification**
   - Sent when: User registers with email
   - Contains: Verification link/code
   - Action: "Verify Email" button
   - Priority: HIGH

5. **Role Assignment Notification**
   - Sent when: User is assigned a new role
   - Contains: Role name, permissions summary, effective date
   - Action: "View Permissions" link
   - Priority: MEDIUM

6. **Organization Suspended/Activated**
   - Sent when: Organization status changes
   - Contains: Status, reason, action required
   - Priority: HIGH

7. **Subscription Expiring Soon**
   - Sent when: Subscription expires in 7 days
   - Contains: Expiry date, renewal options
   - Action: "Renew Subscription" button
   - Priority: MEDIUM

### 1.2 Email Template System

#### Template Structure
```json
{
  "name": "user_invitation",
  "subject": "You're invited to join {{organization.name}}",
  "bodyHtml": "<html>...</html>",
  "bodyText": "Plain text version...",
  "variables": [
    "organization.name",
    "user.name",
    "invitation.link",
    "invitation.expiresAt",
    "invited_by.name"
  ],
  "locale": "en-US",
  "category": "user_management"
}
```

#### Template Variables
- **User Variables**
  - `{{user.name}}`, `{{user.email}}`, `{{user.firstName}}`
  
- **Organization Variables**
  - `{{organization.name}}`, `{{organization.logo}}`, `{{organization.email}}`
  
- **System Variables**
  - `{{system.name}}`, `{{system.url}}`, `{{current.year}}`
  
- **Action Variables**
  - `{{action.link}}`, `{{action.token}}`, `{{action.expiresAt}}`

#### Template Features
- **Multi-Language Support**
  - Templates per locale (en-US, bn-BD, etc.)
  - Automatic locale selection based on user preference
  - Fallback to default locale

- **Organization Customization**
  - Override global templates per organization
  - Custom branding (logo, colors)
  - Custom footer text
  - Custom contact information

- **Version Control**
  - Template versioning
  - A/B testing support (optional)
  - Template history
  - Rollback capability

### 1.3 Email Queue & Delivery

#### Queue System
- **Asynchronous Processing**
  - Non-blocking email sending
  - Queue-based processing
  - Priority queue support
  - Batch sending capability

- **Retry Logic**
  - Automatic retry on failure
  - Exponential backoff (1min, 5min, 15min, 1hour)
  - Maximum retry attempts: 5
  - Dead letter queue for permanent failures

- **Rate Limiting**
  - Per-organization limits
  - Per-user limits
  - Global rate limiting
  - Burst handling

#### Delivery Tracking
```sql
CREATE TABLE notifications.email_logs (
    id UUID PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    from_email VARCHAR(255),
    subject VARCHAR(255),
    template_name VARCHAR(100),
    template_variables JSONB,
    status VARCHAR(50), -- PENDING, SENT, DELIVERED, FAILED, BOUNCED
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    bounced_at TIMESTAMP,
    failed_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    organization_id UUID,
    user_id UUID,
    created_at TIMESTAMP,
    INDEX idx_email_status (status),
    INDEX idx_email_user (user_id),
    INDEX idx_email_org (organization_id),
    INDEX idx_email_created (created_at)
);
```

---

## 🔔 2. In-App Notification System

### 2.1 Core Requirements

#### Notification Entity
```sql
CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID,
    type VARCHAR(50), -- INFO, WARNING, ERROR, SUCCESS, TASK
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    icon VARCHAR(50),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    metadata JSONB,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_unread (user_id, is_read),
    INDEX idx_notif_created (created_at)
);
```

#### Notification Types
- **INFO**: General information
- **SUCCESS**: Success messages (e.g., "Organization created")
- **WARNING**: Warning messages (e.g., "Storage limit approaching")
- **ERROR**: Error notifications (e.g., "Payment failed")
- **TASK**: Action required (e.g., "Approval pending")

### 2.2 Real-Time Delivery

#### WebSocket Implementation
- **Connection**
  - WebSocket endpoint: `/ws/notifications`
  - Authentication via JWT token
  - Automatic reconnection on disconnect
  - Heartbeat/ping-pong for connection health

- **Message Format**
  ```json
  {
    "type": "notification",
    "data": {
      "id": "uuid",
      "title": "New invitation",
      "message": "You've been invited to join ACME Corp",
      "type": "INFO",
      "actionUrl": "/invitations/123",
      "createdAt": "2024-01-13T10:00:00Z"
    }
  }
  ```

#### Fallback Mechanisms
- Server-Sent Events (SSE) as fallback
- Long polling for older browsers
- Polling interval: 30 seconds

### 2.3 Notification Preferences

#### User Preference Entity
```sql
CREATE TABLE notifications.user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    email_enabled BOOLEAN DEFAULT true,
    email_frequency VARCHAR(50) DEFAULT 'IMMEDIATE',
    inapp_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT false,
    push_enabled BOOLEAN DEFAULT true,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    notification_categories JSONB,
    updated_at TIMESTAMP
);
```

#### Preference Options
- **Frequency**
  - IMMEDIATE: Real-time
  - DAILY_DIGEST: Once per day
  - WEEKLY_DIGEST: Once per week
  - DISABLED: No notifications

- **Category Preferences**
  - System notifications
  - User mentions
  - Task assignments
  - Approval requests
  - Security alerts (always enabled)

---

## 📊 3. System Monitoring Service

### 3.1 Service Health Monitoring

#### Health Check Aggregator
- **Functionality**
  - Poll all services every 30 seconds
  - Collect health status from `/actuator/health`
  - Aggregate overall system health
  - Store historical health data
  - Detect service degradation

- **Health Metrics**
  - Service status (UP/DOWN)
  - Response time
  - Last successful check
  - Error count
  - Uptime percentage

#### Service Registry
```sql
CREATE TABLE system.services (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    base_url VARCHAR(500),
    health_endpoint VARCHAR(255) DEFAULT '/actuator/health',
    port INTEGER,
    is_critical BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP
);

CREATE TABLE system.health_checks (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    status VARCHAR(20), -- UP, DOWN, DEGRADED, UNKNOWN
    response_time_ms INTEGER,
    details JSONB,
    checked_at TIMESTAMP,
    INDEX idx_health_service (service_id),
    INDEX idx_health_time (checked_at)
);
```

### 3.2 Alert Management

#### Alert Entity
```sql
CREATE TABLE system.alerts (
    id UUID PRIMARY KEY,
    alert_name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(50), -- CRITICAL, WARNING, INFO
    service_name VARCHAR(100),
    metric_name VARCHAR(100),
    threshold_value DOUBLE PRECISION,
    current_value DOUBLE PRECISION,
    condition VARCHAR(50), -- GREATER_THAN, LESS_THAN, EQUALS
    message TEXT NOT NULL,
    details JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    notification_sent BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    INDEX idx_alert_status (status),
    INDEX idx_alert_type (alert_type),
    INDEX idx_alert_created (created_at)
);
```

#### Alert Rules
- **CPU Usage Alert**
  - Condition: CPU > 80% for 5 minutes
  - Type: WARNING
  - Action: Send notification to admins

- **Memory Usage Alert**
  - Condition: Memory > 90% for 2 minutes
  - Type: CRITICAL
  - Action: Immediate notification + escalation

- **Error Rate Alert**
  - Condition: Error rate > 5% for 1 minute
  - Type: WARNING
  - Action: Notify dev team

- **Service Down Alert**
  - Condition: Health check fails 3 times in a row
  - Type: CRITICAL
  - Action: Immediate notification + PagerDuty

---

## 🔗 4. Webhook & Integration System

### 4.1 Webhook Management

#### Webhook Configuration
```sql
CREATE TABLE integration.webhooks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255),
    events TEXT[],
    headers JSONB,
    is_active BOOLEAN DEFAULT true,
    retry_count INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    UNIQUE(organization_id, name)
);
```

#### Supported Events
- **User Events**
  - `user.created`
  - `user.updated`
  - `user.deleted`
  - `user.activated`
  - `user.deactivated`

- **Organization Events**
  - `organization.created`
  - `organization.updated`
  - `organization.suspended`
  - `organization.activated`

- **Invitation Events**
  - `invitation.sent`
  - `invitation.accepted`
  - `invitation.cancelled`

- **Role Events**
  - `role.assigned`
  - `role.revoked`
  - `permission.granted`

### 4.2 Webhook Delivery

#### Delivery Requirements
- **Request Format**
  ```json
  {
    "event": "user.created",
    "timestamp": "2024-01-13T10:00:00Z",
    "organizationId": "org-uuid",
    "data": {
      "userId": "user-uuid",
      "username": "john.doe",
      "email": "john@example.com"
    },
    "signature": "hmac-sha256-signature"
  }
  ```

- **Security**
  - HMAC-SHA256 signature in `X-Webhook-Signature` header
  - Secret key per webhook
  - Signature validation guide in docs

- **Retry Logic**
  - Retry on 5xx errors
  - Don't retry on 4xx errors
  - Exponential backoff: 1min, 5min, 15min, 1hour, 4hours
  - Maximum 5 retries
  - Move to dead letter queue after max retries

#### Delivery Tracking
```sql
CREATE TABLE integration.webhook_deliveries (
    id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    request_headers JSONB,
    response_status INTEGER,
    response_headers JSONB,
    response_body TEXT,
    delivered_at TIMESTAMP,
    duration_ms INTEGER,
    retry_attempt INTEGER DEFAULT 0,
    success BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP,
    INDEX idx_delivery_webhook (webhook_id),
    INDEX idx_delivery_status (success),
    INDEX idx_delivery_created (created_at)
);
```

---

## 📈 5. Monitoring & Metrics

### 5.1 Application Metrics

#### Prometheus Metrics to Collect

**HTTP Metrics**
- `http_server_requests_total` - Total HTTP requests (counter)
- `http_server_requests_duration_seconds` - Request duration (histogram)
- `http_server_requests_active` - Active requests (gauge)

**JVM Metrics**
- `jvm_memory_used_bytes` - Memory usage (gauge)
- `jvm_memory_max_bytes` - Max memory (gauge)
- `jvm_gc_pause_seconds` - GC pause time (histogram)
- `jvm_threads_live` - Thread count (gauge)

**Database Metrics**
- `hikaricp_connections_active` - Active DB connections (gauge)
- `hikaricp_connections_pending` - Pending connections (gauge)
- `hikaricp_connections_timeout_total` - Connection timeouts (counter)

**Business Metrics**
- `easyops_users_total` - Total users (gauge)
- `easyops_users_active_total` - Active users (gauge)
- `easyops_organizations_total` - Total organizations (gauge)
- `easyops_api_calls_total` - API calls per service (counter)
- `easyops_login_attempts_total` - Login attempts (counter)
- `easyops_login_failures_total` - Failed logins (counter)

**Custom Metrics**
- `easyops_email_sent_total` - Emails sent (counter)
- `easyops_email_failed_total` - Failed emails (counter)
- `easyops_notifications_created_total` - Notifications created (counter)
- `easyops_webhook_deliveries_total` - Webhook deliveries (counter)

### 5.2 Grafana Dashboards

#### Dashboard 1: System Overview
**Panels:**
1. Service Status (Status indicators)
2. Total API Requests (Counter with rate)
3. Average Response Time (Line chart)
4. Error Rate % (Line chart)
5. Active Users (Gauge)
6. System Resources (CPU, Memory gauges)

#### Dashboard 2: Service Details
**Panels per Service:**
1. Request Rate (Line chart)
2. Response Time Distribution (Histogram)
3. Error Count by Endpoint (Bar chart)
4. JVM Memory Usage (Area chart)
5. Database Connections (Line chart)
6. Cache Hit Rate (Line chart)

#### Dashboard 3: Business Metrics
**Panels:**
1. User Growth (Line chart with daily/weekly/monthly)
2. Organizations by Subscription Plan (Pie chart)
3. API Usage by Service (Bar chart)
4. Login Success/Failure Rate (Line chart)
5. Most Active Users (Table)
6. Storage Usage by Organization (Bar chart)

#### Dashboard 4: Alerts & Issues
**Panels:**
1. Active Alerts (List with severity)
2. Alert History (Timeline)
3. Services with Issues (List)
4. Error Rate Trend (Line chart)
5. Failed Requests by Endpoint (Table)
6. Slow Queries (Table)

---

## 🔍 6. Logging & Tracing

### 6.1 Structured Logging

#### Log Format (JSON)
```json
{
  "timestamp": "2024-01-13T10:00:00.123Z",
  "level": "INFO",
  "service": "organization-service",
  "instance": "org-service-1",
  "traceId": "abc123def456",
  "spanId": "span789",
  "userId": "user-uuid",
  "organizationId": "org-uuid",
  "requestId": "req-123",
  "method": "POST",
  "path": "/api/organizations",
  "statusCode": 201,
  "duration": 145,
  "message": "Organization created successfully",
  "context": {
    "organizationCode": "ACME",
    "action": "CREATE",
    "createdBy": "admin-uuid"
  },
  "exception": null
}
```

#### Log Correlation
- **Trace ID**: Unique ID for request across all services
- **Span ID**: Unique ID for each service operation
- **User ID**: User who initiated the request
- **Organization ID**: Organization context
- **Request ID**: Unique request identifier

### 6.2 Distributed Tracing (Optional - Future)

#### Jaeger Integration (Future Phase)
- Request flow visualization
- Service dependency mapping
- Performance bottleneck detection
- Error propagation tracking

---

## 📱 7. Frontend Requirements

### 7.1 Monitoring Dashboard Page

#### Layout
- **Header**
  - Dashboard title
  - Last update time
  - Refresh button
  - Time range selector

- **Service Status Grid**
  - Card for each service
  - Status indicator (green/yellow/red)
  - Key metrics (requests/min, errors/min)
  - Uptime percentage
  - Quick actions (view details, restart)

- **System Metrics**
  - CPU usage chart
  - Memory usage chart
  - Disk usage gauge
  - Network I/O chart

- **Recent Alerts**
  - Alert list with severity
  - Time since alert
  - Acknowledge button
  - View details link

### 7.2 Notifications Component

#### Notification Bell (Navbar)
- **Badge**
  - Unread count
  - Red dot for high priority
  - Animation for new notifications

- **Dropdown Menu**
  - Recent 5 notifications
  - "Mark all as read" button
  - "View all" link
  - Notification icons by type

#### Notifications Page
- **List View**
  - All notifications (paginated)
  - Filter by type/status
  - Search functionality
  - Bulk actions

- **Notification Card**
  - Title and message
  - Timestamp (relative time)
  - Action button (if applicable)
  - Mark as read toggle
  - Delete button

### 7.3 System Settings Page

#### Email Configuration
- **SMTP Settings**
  - Host, port, username, password
  - Test connection button
  - Encryption method (TLS/SSL)
  - From email address

- **Template Management**
  - List all templates
  - Edit template (WYSIWYG editor)
  - Preview template
  - Test send email

#### Notification Settings
- **Global Settings**
  - Enable/disable channels
  - Default preferences
  - Rate limits
  - Retention period

- **User Preference Defaults**
  - Default notification frequency
  - Default channels
  - Quiet hours

### 7.4 API Documentation Page

#### API Explorer
- **Service Selector**
  - Dropdown of all services
  - Service description
  - Base URL display

- **Endpoint Browser**
  - Grouped by tags/categories
  - HTTP method badges
  - Quick filter/search

- **Try It Out**
  - Interactive request form
  - Authentication header auto-filled
  - Request body editor (JSON)
  - Send button
  - Response viewer (formatted JSON)
  - Response time display
  - Status code indicator

- **Code Generator**
  - Generate cURL command
  - Generate JavaScript/Fetch code
  - Generate Python requests code
  - Copy to clipboard button

---

## 🔐 8. Security Requirements

### Email Security
- **Authentication**
  - Only authenticated users can trigger emails
  - Rate limiting per user/organization
  - Email spoofing prevention

- **Content Security**
  - XSS prevention in templates
  - Injection attack prevention
  - Safe variable substitution
  - HTML sanitization

### Webhook Security
- **HMAC Signature**
  - SHA-256 HMAC of payload
  - Secret key per webhook
  - Signature in header
  - Timestamp validation

- **Access Control**
  - Only organization admins can manage webhooks
  - Webhook URL validation
  - HTTPS requirement (production)
  - IP whitelist support

### Monitoring Security
- **Dashboard Access**
  - Require authentication
  - Role-based access (admin only)
  - Audit log for dashboard access

- **Metrics Security**
  - No sensitive data in metrics
  - Aggregate data only
  - No PII in logs

---

## 📊 9. API Specifications

### Notification Service APIs

#### Send Email
```
POST /api/notifications/email/send
Request:
{
  "to": "user@example.com",
  "templateName": "user_invitation",
  "variables": {
    "organization.name": "ACME Corp",
    "invitation.link": "https://...",
    "invitation.expiresAt": "2024-01-20"
  },
  "priority": "HIGH"
}
Response: 201 Created
{
  "id": "email-uuid",
  "status": "PENDING",
  "createdAt": "2024-01-13T10:00:00Z"
}
```

#### Get User Notifications
```
GET /api/notifications?page=0&size=20&unreadOnly=true
Response: 200 OK
{
  "content": [
    {
      "id": "notif-uuid",
      "title": "New Invitation",
      "message": "You've been invited...",
      "type": "INFO",
      "isRead": false,
      "createdAt": "2024-01-13T10:00:00Z"
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

### Monitoring Service APIs

#### Get Services Health
```
GET /api/monitoring/services
Response: 200 OK
{
  "services": [
    {
      "name": "organization-service",
      "status": "UP",
      "responseTime": 45,
      "uptime": 99.98,
      "lastCheck": "2024-01-13T10:00:00Z"
    }
  ],
  "overallStatus": "UP",
  "servicesUp": 6,
  "servicesDown": 0
}
```

---

## ✅ Implementation Checklist

### Phase 1: Infrastructure (Week 1)
- [ ] Add Prometheus to docker-compose
- [ ] Add Grafana to docker-compose
- [ ] Configure Prometheus scraping
- [ ] Create Grafana datasource
- [ ] Create basic Grafana dashboards
- [ ] Test metrics collection

### Phase 2: Notification Service (Week 1-2)
- [ ] Create notification-service project structure
- [ ] Implement email service with JavaMail
- [ ] Create email template engine
- [ ] Implement in-app notification system
- [ ] Add WebSocket support for real-time notifications
- [ ] Create notification REST APIs
- [ ] Implement notification preferences

### Phase 3: Monitoring Service (Week 2)
- [ ] Create monitoring-service project
- [ ] Implement health check aggregator
- [ ] Create alert management system
- [ ] Implement alert rules engine
- [ ] Create monitoring REST APIs

### Phase 4: Webhook System (Week 2-3)
- [ ] Implement webhook management
- [ ] Create webhook delivery system
- [ ] Add retry logic with exponential backoff
- [ ] Implement HMAC signature
- [ ] Create webhook APIs

### Phase 5: API Documentation (Week 3)
- [ ] Add Springdoc OpenAPI to all services
- [ ] Configure Swagger UI
- [ ] Add API annotations
- [ ] Create API documentation portal page
- [ ] Add code generation feature

### Phase 6: Frontend (Week 3-4)
- [ ] Create Monitoring Dashboard page
- [ ] Add Notification Bell component
- [ ] Create Notifications page
- [ ] Create System Settings page
- [ ] Create API Documentation page
- [ ] Add WebSocket connection

### Phase 7: Integration & Testing (Week 4)
- [ ] Integrate email service with invitations
- [ ] Integrate email service with password reset
- [ ] Write integration tests
- [ ] Performance testing
- [ ] Documentation updates

---

## 📚 Documentation Requirements

### User Documentation
- Notification preferences guide
- Webhook setup tutorial
- API documentation usage guide
- Monitoring dashboard guide
- Alert management guide

### Technical Documentation
- Email template development guide
- Webhook signature validation
- Metrics collection guide
- Custom alert rules
- Grafana dashboard creation

### API Documentation
- OpenAPI specifications
- Authentication guide
- Rate limiting information
- Error codes reference
- Example requests/responses

---

## 🎯 Success Criteria

### Functional
- ✅ Emails sent reliably
- ✅ In-app notifications delivered in real-time
- ✅ All services monitored
- ✅ Alerts generated and sent
- ✅ Webhooks delivered with retries
- ✅ API documentation accessible and interactive

### Performance
- ✅ Email delivery < 10 seconds
- ✅ Notification delivery < 1 second
- ✅ Webhook delivery < 5 seconds
- ✅ Dashboard load < 2 seconds
- ✅ Metrics collection overhead < 5%

### Reliability
- ✅ 99% email delivery rate
- ✅ 99.9% notification delivery rate
- ✅ 95% webhook success rate
- ✅ No data loss in monitoring
- ✅ Automatic recovery from failures

---

**This comprehensive requirements document covers all aspects of Phase 0.3 - Integration & Monitoring!**

