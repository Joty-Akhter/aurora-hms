# Phase 0.3 - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Start All Services

```bash
cd easyops-erp
docker-compose up -d
```

Wait for all services to be healthy (~2-3 minutes):
```bash
docker-compose ps
```

### Step 2: Access Key Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana Dashboard** | http://localhost:3001 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Notification API** | http://localhost:8086/swagger-ui.html | - |
| **Monitoring API** | http://localhost:8087/swagger-ui.html | - |
| **Eureka Dashboard** | http://localhost:8761 | admin/admin |
| **Frontend** | http://localhost:3000 | - |

### Step 3: Verify System Health

#### Check All Services:
```bash
curl http://localhost:8087/api/monitoring/services | jq
```

Expected output:
```json
[
  {
    "serviceName": "eureka",
    "status": "UP",
    "responseTimeMs": 45,
    ...
  },
  ...
]
```

#### Check System Metrics:
```bash
curl http://localhost:8087/api/monitoring/metrics | jq
```

### Step 4: Test Notifications

#### Create a Test Notification:
```bash
curl -X POST http://localhost:8086/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "type": "INFO",
    "title": "Welcome to EasyOps!",
    "message": "Your account has been successfully created.",
    "priority": "MEDIUM"
  }'
```

#### Send a Test Email:
```bash
curl -X POST http://localhost:8086/api/notifications/email/send \
  -H "Content-Type: application/json" \
  -d '{
    "toEmail": "test@example.com",
    "subject": "Test Email",
    "body": "<h1>Hello from EasyOps!</h1><p>This is a test email.</p>"
  }'
```

### Step 5: Create a Webhook

```bash
curl -X POST http://localhost:8086/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174001",
    "name": "Test Webhook",
    "url": "https://webhook.site/your-unique-url",
    "events": ["user.created", "organization.updated"],
    "isActive": true
  }'
```

Visit https://webhook.site to create a test URL and see deliveries!

### Step 6: Explore Grafana

1. Open http://localhost:3001
2. Login with admin/admin
3. Go to **Dashboards → EasyOps - System Overview**
4. View real-time metrics!

---

## 📊 Key Features to Try

### 1. Real-Time Notifications (WebSocket)

Connect to WebSocket:
```javascript
const socket = new SockJS('http://localhost:8086/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/notifications/USER_ID', function(notification) {
        console.log('New notification:', JSON.parse(notification.body));
    });
});
```

### 2. Email Templates

Use pre-configured templates:
- `user_invitation`
- `password_reset`
- `welcome_email`
- `organization_suspended`

Example:
```bash
curl -X POST http://localhost:8086/api/notifications/email/send \
  -H "Content-Type: application/json" \
  -d '{
    "toEmail": "user@example.com",
    "subject": "Welcome!",
    "templateName": "welcome_email",
    "templateVariables": {
      "user.name": "John Doe",
      "organization.name": "ACME Corp",
      "getting_started.link": "http://example.com/start",
      "support.email": "support@acme.com"
    }
  }'
```

### 3. Monitoring Alerts

View active alerts:
```bash
curl http://localhost:8087/api/monitoring/alerts | jq
```

Acknowledge an alert:
```bash
curl -X POST http://localhost:8087/api/monitoring/alerts/{alert-id}/acknowledge \
  -H "X-User-Id: your-user-id"
```

### 4. Webhook Testing

Test a webhook immediately:
```bash
curl -X POST http://localhost:8086/api/webhooks/{webhook-id}/test \
  -H "Content-Type: application/json" \
  -d '{
    "event": "test.event",
    "data": {
      "message": "This is a test webhook delivery"
    }
  }'
```

View delivery history:
```bash
curl http://localhost:8086/api/webhooks/{webhook-id}/deliveries | jq
```

---

## 🔍 Monitoring Quick Reference

### Prometheus Queries

Access Prometheus at http://localhost:9090

**Useful Queries:**

1. **Request rate per service:**
   ```
   sum(rate(http_server_requests_total[5m])) by (job)
   ```

2. **Error rate:**
   ```
   sum(rate(http_server_requests_total{status=~"5.."}[5m])) by (job)
   ```

3. **P95 response time:**
   ```
   histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (job, le))
   ```

4. **Memory usage:**
   ```
   jvm_memory_used_bytes{area="heap"} / 1024 / 1024
   ```

5. **Service status:**
   ```
   up{job=~".+-service"}
   ```

### Grafana Dashboard Panels

The System Overview dashboard includes:
- ✅ Total Services (Stat)
- ✅ Services UP (Stat)
- ✅ Services DOWN (Stat)
- ✅ Avg Response Time (Stat)
- ✅ Request Rate by Service (Graph)
- ✅ Error Rate Percentage (Graph)
- ✅ CPU Usage (Graph)
- ✅ Memory Usage (Graph)
- ✅ Service Status Table

---

## 🛠️ Configuration

### Email Configuration

Edit `services/notification-service/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

Or use Docker environment variables in `docker-compose.yml`:

```yaml
notification-service:
  environment:
    - MAIL_HOST=smtp.gmail.com
    - MAIL_PORT=587
    - MAIL_USERNAME=your-email@gmail.com
    - MAIL_PASSWORD=your-app-password
```

### Monitoring Configuration

Edit `services/monitoring-service/src/main/resources/application.yml`:

```yaml
monitoring:
  health-check:
    interval-seconds: 30    # How often to check services
    timeout-seconds: 5      # Request timeout
  metrics:
    retention-days: 30      # How long to keep metrics
  alerts:
    enabled: true           # Enable/disable alerts
```

---

## 📱 API Documentation

### Interactive API Docs

All services have Swagger UI available:

- **Notification Service**: http://localhost:8086/swagger-ui.html
- **Monitoring Service**: http://localhost:8087/swagger-ui.html
- **Organization Service**: http://localhost:8085/swagger-ui.html
- **RBAC Service**: http://localhost:8084/swagger-ui.html
- **Auth Service**: http://localhost:8083/swagger-ui.html
- **User Management**: http://localhost:8082/swagger-ui.html

### OpenAPI Specs

JSON format available at `/v3/api-docs`:
- http://localhost:8086/v3/api-docs
- http://localhost:8087/v3/api-docs

---

## 🐛 Troubleshooting

### Issue: Services won't start

**Solution:**
```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes data)
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

### Issue: Prometheus not scraping metrics

**Check targets:**
1. Go to http://localhost:9090/targets
2. All targets should be "UP" (green)
3. If "DOWN", check service health: `docker-compose logs <service-name>`

### Issue: Grafana shows no data

**Solution:**
1. Verify Prometheus datasource: http://localhost:3001/datasources
2. Test connection
3. Check if services are exposing metrics: `curl http://localhost:8086/actuator/prometheus`

### Issue: WebSocket connection fails

**Solution:**
1. Check if notification-service is running: `docker-compose ps`
2. Try SockJS fallback endpoint: `http://localhost:8086/ws/notifications`
3. Check browser console for errors

### Issue: Emails not sending

**Solution:**
1. Check email logs: `curl http://localhost:8086/api/notifications/email/logs/{id}`
2. Verify SMTP credentials in `application.yml`
3. Check service logs: `docker-compose logs notification-service`
4. For Gmail, use an App Password (not your regular password)

---

## 🎯 Next Steps

### 1. Customize Email Templates

Update templates in the database:
```sql
UPDATE notifications.email_templates 
SET body_html = '<your-custom-html>' 
WHERE name = 'welcome_email';
```

### 2. Create Custom Alerts

Extend the monitoring service to add custom alert rules:
- High memory usage
- Slow queries
- Custom business metrics

### 3. Add More Dashboards

Create service-specific dashboards in Grafana:
- Auth Service metrics
- Organization Service metrics
- Business metrics (users, orgs, revenue)

### 4. Integrate with Frontend

Use the WebSocket connection to show real-time notifications in your React frontend.

---

## 📚 Additional Resources

- [Full Implementation Guide](./PHASE_0.3_IMPLEMENTATION_COMPLETE.md)
- [Phase 0.3 Requirements](../requirements/administrative/phase_0.3_integration_monitoring_requirements.md)
- [Monitoring Requirements](../requirements/administrative/monitoring_observability_requirements.md)
- [Architecture Documentation](./ARCHITECTURE.md)

---

**Happy Monitoring! 🎉**

All services are now observable, notifications are flowing, and webhooks are ready for integration!

