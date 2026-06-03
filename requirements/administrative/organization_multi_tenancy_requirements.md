# Organization & Multi-Tenancy Requirements - Phase 0.2

## 📋 Overview

This document defines the comprehensive requirements for implementing Organization Management and Multi-Tenancy capabilities in EasyOps ERP. This forms the foundation for tenant isolation, organizational hierarchy, and secure multi-tenant operations.

### Objectives
- **Multi-Tenant Architecture**: Enable multiple organizations to use the system securely and independently
- **Organization Management**: Provide comprehensive organizational structure and hierarchy management
- **Data Isolation**: Ensure complete separation of data between tenants
- **Tenant Administration**: Enable tenant-level configuration and customization
- **Security & Compliance**: Implement tenant-level security and compliance controls

---

## 🏢 1. Organization Management

### 1.1 Organization/Tenant Entity

#### Core Organization Fields
- **Basic Information**
  - `id` (UUID): Unique organization identifier
  - `code` (String, 20 chars): Unique organization code (e.g., "ORG001", "ACME")
  - `name` (String, 255 chars): Organization name
  - `legalName` (String, 255 chars): Legal business name
  - `description` (Text): Organization description
  - `logo` (String): Logo URL/path
  - `website` (String): Organization website
  - `email` (String): Primary contact email
  - `phone` (String): Primary contact phone

- **Business Information**
  - `industry` (String): Industry type (Manufacturing, Retail, Services, etc.)
  - `businessType` (String): Business type (Corporation, Partnership, etc.)
  - `taxId` (String): Tax identification number
  - `registrationNumber` (String): Business registration number
  - `fiscalYearStart` (Date): Fiscal year start date
  - `currency` (String): Primary currency code (USD, EUR, etc.)
  - `timezone` (String): Organization timezone
  - `locale` (String): Organization locale (en-US, es-ES, etc.)

- **Address Information**
  - `addressLine1` (String): Street address line 1
  - `addressLine2` (String): Street address line 2
  - `city` (String): City
  - `state` (String): State/Province
  - `postalCode` (String): Postal/ZIP code
  - `country` (String): Country code (ISO 3166)

- **Subscription & Status**
  - `subscriptionPlan` (Enum): FREE, BASIC, PROFESSIONAL, ENTERPRISE, CUSTOM
  - `subscriptionStatus` (Enum): ACTIVE, SUSPENDED, TRIAL, CANCELLED, EXPIRED
  - `subscriptionStartDate` (Date): Subscription start date
  - `subscriptionEndDate` (Date): Subscription end date
  - `trialEndsAt` (Date): Trial period end date
  - `maxUsers` (Integer): Maximum allowed users
  - `maxStorage` (Long): Maximum storage in bytes

- **Status & Metadata**
  - `status` (Enum): ACTIVE, INACTIVE, SUSPENDED, PENDING, ARCHIVED
  - `isActive` (Boolean): Active status flag
  - `createdAt` (Timestamp): Creation timestamp
  - `updatedAt` (Timestamp): Last update timestamp
  - `createdBy` (UUID): User who created the organization
  - `updatedBy` (UUID): User who last updated

### 1.2 Organization Settings

#### Tenant-Specific Settings
- **General Settings**
  - Date format preferences
  - Number format preferences
  - Language preferences
  - Timezone settings
  - Currency settings
  - Measurement units (metric/imperial)

- **Feature Toggles**
  - Enabled modules (Accounting, Inventory, Sales, etc.)
  - Feature flags per module
  - Custom feature configurations
  - Beta features access

- **Security Settings**
  - Password policy configuration
  - Session timeout settings
  - MFA requirements
  - IP whitelist/blacklist
  - Login attempt limits
  - Password reset policies

- **Notification Settings**
  - Email notification preferences
  - SMS notification settings
  - In-app notification rules
  - Notification templates customization

- **Integration Settings**
  - API access configuration
  - Webhook endpoints
  - Third-party integrations
  - Integration credentials (encrypted)

### 1.3 Organizational Hierarchy

#### Department/Division Structure
- **Department Entity**
  - `id` (UUID): Unique identifier
  - `organizationId` (UUID): Parent organization
  - `parentDepartmentId` (UUID): Parent department (for hierarchy)
  - `code` (String): Department code (e.g., "DEPT-FIN")
  - `name` (String): Department name
  - `description` (Text): Department description
  - `type` (Enum): DEPARTMENT, DIVISION, UNIT, TEAM, BRANCH
  - `managerId` (UUID): Department manager user ID
  - `costCenter` (String): Cost center code
  - `status` (Enum): ACTIVE, INACTIVE
  - `createdAt`, `updatedAt`: Timestamps

- **Hierarchy Features**
  - Unlimited depth organizational tree
  - Parent-child relationships
  - Department transfers
  - Reporting structure
  - Cross-department visibility rules

#### Location/Branch Management
- **Location Entity**
  - `id` (UUID): Unique identifier
  - `organizationId` (UUID): Parent organization
  - `code` (String): Location code
  - `name` (String): Location name
  - `type` (Enum): HEADQUARTERS, BRANCH, WAREHOUSE, OFFICE, RETAIL, FACTORY
  - `addressLine1`, `addressLine2`, `city`, `state`, `postalCode`, `country`: Address fields
  - `phone`, `email`: Contact information
  - `managerId` (UUID): Location manager
  - `timezone` (String): Location timezone
  - `operatingHours` (JSON): Operating hours schedule
  - `coordinates` (JSON): Latitude/longitude
  - `status` (Enum): ACTIVE, INACTIVE, TEMPORARY
  - `createdAt`, `updatedAt`: Timestamps

---

## 🔐 2. Multi-Tenant Data Isolation

### 2.1 Tenant Isolation Strategy

#### Database-Level Isolation
- **Schema-per-Tenant Approach**
  - Each tenant gets a dedicated PostgreSQL schema
  - Schema naming: `tenant_{organization_code}`
  - Automatic schema creation on tenant provisioning
  - Schema-level permissions and access control

- **Shared Tables with Tenant ID**
  - Common tables include `organizationId` column
  - Row-level security (RLS) policies
  - Automatic filtering by tenant context
  - Index optimization for tenant queries

- **Hybrid Approach**
  - Critical data: Schema-per-tenant
  - Reference data: Shared with tenant filtering
  - System data: Global shared tables

#### Application-Level Isolation
- **Tenant Context Management**
  - Thread-local tenant context storage
  - Automatic tenant resolution from JWT token
  - Request-scoped tenant isolation
  - Tenant context propagation across services

- **Data Access Layer**
  - Automatic tenant filtering in queries
  - Repository-level tenant enforcement
  - Multi-tenancy aware query builders
  - Tenant context validation

### 2.2 Tenant Provisioning

#### Automated Provisioning Process
1. **Organization Registration**
   - Create organization record
   - Generate unique organization code
   - Set initial subscription plan
   - Configure default settings

2. **Schema Creation**
   - Create dedicated schema
   - Run migration scripts
   - Create tenant-specific tables
   - Set up indexes and constraints

3. **Default Data Setup**
   - Create default admin user
   - Set up default roles and permissions
   - Create default organizational structure
   - Configure initial settings

4. **Service Configuration**
   - Configure tenant in all microservices
   - Set up tenant-specific caching
   - Configure logging and monitoring
   - Enable tenant-specific features

5. **Activation & Notification**
   - Activate tenant
   - Send welcome emails
   - Provide onboarding materials
   - Enable access

#### De-provisioning Process
1. **Data Export**
   - Export all tenant data
   - Generate data archive
   - Store backup securely
   - Verify data integrity

2. **Service Cleanup**
   - Disable tenant access
   - Clear tenant caches
   - Remove active sessions
   - Disable integrations

3. **Data Deletion**
   - Soft delete tenant data (retention period)
   - Hard delete after retention period
   - Remove schema (optional)
   - Clean up file storage

4. **Audit & Compliance**
   - Log all deletion activities
   - Maintain audit trail
   - Compliance verification
   - Generate deletion certificate

### 2.3 Tenant Security

#### Access Control
- **Tenant-Level Permissions**
  - Tenant administrators
  - Tenant users
  - Cross-tenant access prevention
  - API access restrictions

- **Data Encryption**
  - Encryption at rest (tenant-specific keys)
  - Encryption in transit (TLS 1.3)
  - Field-level encryption for sensitive data
  - Key management per tenant

#### Compliance & Audit
- **Audit Logging**
  - All tenant data access logged
  - User activity tracking
  - Admin action logging
  - Compliance event tracking

- **Data Residency**
  - Geographic data location preferences
  - Regional compliance support
  - Data sovereignty controls
  - Cross-border data transfer policies

---

## 👥 3. User-Organization Relationship

### 3.1 User-Tenant Association

#### User-Organization Mapping
- **UserOrganization Entity**
  - `id` (UUID): Unique identifier
  - `userId` (UUID): User reference
  - `organizationId` (UUID): Organization reference
  - `role` (Enum): OWNER, ADMIN, MANAGER, MEMBER, GUEST
  - `isPrimary` (Boolean): Primary organization flag
  - `joinedAt` (Timestamp): Join date
  - `invitedBy` (UUID): Inviter user ID
  - `status` (Enum): ACTIVE, PENDING, SUSPENDED, REMOVED
  - `permissions` (JSON): Additional permissions
  - `createdAt`, `updatedAt`: Timestamps

#### Multi-Organization Support
- Users can belong to multiple organizations
- Each user-org relationship has specific roles
- One primary organization per user
- Easy organization switching in UI
- Separate permissions per organization

### 3.2 User Invitations

#### Invitation Process
- **Invitation Entity**
  - `id` (UUID): Unique identifier
  - `organizationId` (UUID): Target organization
  - `email` (String): Invitee email
  - `role` (String): Assigned role
  - `invitedBy` (UUID): Inviter user ID
  - `token` (String): Unique invitation token
  - `status` (Enum): PENDING, ACCEPTED, EXPIRED, CANCELLED
  - `expiresAt` (Timestamp): Expiration date
  - `acceptedAt` (Timestamp): Acceptance date
  - `createdAt`: Timestamp

#### Invitation Workflow
1. Admin sends invitation
2. System generates secure token
3. Email sent to invitee
4. Invitee clicks link
5. Registration or login
6. Accept invitation
7. Join organization

---

## 📊 4. Organization Analytics & Reporting

### 4.1 Tenant Metrics

#### Usage Metrics
- Active users count
- Storage usage
- API calls per day/month
- Feature usage statistics
- Module activation status
- Session statistics

#### Business Metrics
- Total users
- User growth rate
- Active vs inactive users
- Department count
- Location count
- Subscription status

### 4.2 Health Monitoring

#### Tenant Health Indicators
- System performance metrics
- Error rates
- User satisfaction scores
- Support ticket volume
- Compliance status
- Security incident count

---

## 🔄 5. Organization Operations

### 5.1 Organization Lifecycle

#### Creation
- Registration form validation
- Email verification
- Initial setup wizard
- Default configuration
- Admin user creation
- Welcome notifications

#### Updates
- Organization profile updates
- Settings modification
- Subscription changes
- Feature toggles
- Integration updates
- User management

#### Suspension
- Suspend access
- Maintain data
- Send notifications
- Set reactivation conditions
- Monitor suspension period
- Audit suspension events

#### Deletion
- Soft delete with retention
- Data export option
- User notifications
- Access revocation
- Hard delete after retention
- Compliance documentation

### 5.2 Subscription Management

#### Plan Management
- **Subscription Plans**
  - FREE: Basic features, limited users
  - BASIC: Standard features, moderate limits
  - PROFESSIONAL: Advanced features, higher limits
  - ENTERPRISE: All features, custom limits
  - CUSTOM: Tailored features and pricing

- **Plan Features**
  - User limits
  - Storage limits
  - Module access
  - API rate limits
  - Support level
  - Custom features

#### Billing Integration
- Subscription creation
- Plan upgrades/downgrades
- Usage tracking
- Billing cycle management
- Payment processing hooks
- Invoice generation

---

## 🛠️ 6. Technical Implementation

### 6.1 Database Schema

#### Core Tables
```sql
-- organizations table
CREATE TABLE admin.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    description TEXT,
    logo VARCHAR(500),
    website VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    industry VARCHAR(100),
    business_type VARCHAR(100),
    tax_id VARCHAR(100),
    registration_number VARCHAR(100),
    fiscal_year_start DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(100) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    subscription_plan VARCHAR(50) DEFAULT 'FREE',
    subscription_status VARCHAR(50) DEFAULT 'TRIAL',
    subscription_start_date TIMESTAMP,
    subscription_end_date TIMESTAMP,
    trial_ends_at TIMESTAMP,
    max_users INTEGER DEFAULT 10,
    max_storage BIGINT DEFAULT 1073741824, -- 1GB in bytes
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- organization_settings table
CREATE TABLE admin.organization_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(50),
    is_encrypted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, setting_key)
);

-- departments table
CREATE TABLE admin.departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    parent_department_id UUID REFERENCES admin.departments(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) DEFAULT 'DEPARTMENT',
    manager_id UUID,
    cost_center VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, code)
);

-- locations table
CREATE TABLE admin.locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) DEFAULT 'BRANCH',
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    phone VARCHAR(50),
    email VARCHAR(255),
    manager_id UUID,
    timezone VARCHAR(100),
    operating_hours JSONB,
    coordinates JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, code)
);

-- user_organizations table
CREATE TABLE admin.user_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    is_primary BOOLEAN DEFAULT false,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    invited_by UUID,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    permissions JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, organization_id)
);

-- invitations table
CREATE TABLE admin.invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    invited_by UUID NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_org_code ON admin.organizations(code);
CREATE INDEX idx_org_status ON admin.organizations(status);
CREATE INDEX idx_org_subscription ON admin.organizations(subscription_status);
CREATE INDEX idx_dept_org ON admin.departments(organization_id);
CREATE INDEX idx_dept_parent ON admin.departments(parent_department_id);
CREATE INDEX idx_location_org ON admin.locations(organization_id);
CREATE INDEX idx_user_org_user ON admin.user_organizations(user_id);
CREATE INDEX idx_user_org_org ON admin.user_organizations(organization_id);
CREATE INDEX idx_invitation_email ON admin.invitations(email);
CREATE INDEX idx_invitation_token ON admin.invitations(token);
```

### 6.2 API Endpoints

#### Organization Management APIs

```
POST   /api/organizations                    - Create organization
GET    /api/organizations                    - List organizations (admin)
GET    /api/organizations/{id}               - Get organization details
PUT    /api/organizations/{id}               - Update organization
DELETE /api/organizations/{id}               - Delete organization
PATCH  /api/organizations/{id}/activate      - Activate organization
PATCH  /api/organizations/{id}/suspend       - Suspend organization

GET    /api/organizations/{id}/settings      - Get organization settings
PUT    /api/organizations/{id}/settings      - Update organization settings
GET    /api/organizations/{id}/settings/{key} - Get specific setting
PUT    /api/organizations/{id}/settings/{key} - Update specific setting

GET    /api/organizations/{id}/departments   - List departments
POST   /api/organizations/{id}/departments   - Create department
GET    /api/departments/{id}                 - Get department details
PUT    /api/departments/{id}                 - Update department
DELETE /api/departments/{id}                 - Delete department
GET    /api/departments/{id}/tree            - Get department tree

GET    /api/organizations/{id}/locations     - List locations
POST   /api/organizations/{id}/locations     - Create location
GET    /api/locations/{id}                   - Get location details
PUT    /api/locations/{id}                   - Update location
DELETE /api/locations/{id}                   - Delete location

GET    /api/organizations/{id}/users         - List organization users
POST   /api/organizations/{id}/users         - Add user to organization
DELETE /api/organizations/{id}/users/{userId} - Remove user from organization
PUT    /api/organizations/{id}/users/{userId}/role - Update user role

POST   /api/organizations/{id}/invitations   - Send invitation
GET    /api/organizations/{id}/invitations   - List invitations
DELETE /api/invitations/{id}                 - Cancel invitation
POST   /api/invitations/{token}/accept       - Accept invitation

GET    /api/organizations/{id}/metrics       - Get organization metrics
GET    /api/organizations/{id}/health        - Get organization health
GET    /api/organizations/{id}/audit-log     - Get audit log
```

### 6.3 Service Architecture

#### Microservices Structure
- **Organization Service** (Port 8085)
  - Organization CRUD operations
  - Settings management
  - Department/Location management
  - Invitation system
  
- **Integration Points**
  - User Management Service: User-org relationships
  - Auth Service: Tenant context in tokens
  - RBAC Service: Tenant-scoped permissions
  - All Services: Tenant filtering

---

## ✅ Acceptance Criteria

### Functional Requirements
- ✅ Create, read, update, delete organizations
- ✅ Manage organization settings and configurations
- ✅ Create and manage organizational hierarchy (departments/locations)
- ✅ Invite users to organizations
- ✅ Manage user-organization relationships
- ✅ Support multi-organization user access
- ✅ Tenant context isolation and security
- ✅ Organization metrics and reporting

### Non-Functional Requirements
- ✅ Complete tenant data isolation
- ✅ Performance: < 200ms for tenant context resolution
- ✅ Security: Encrypted sensitive settings
- ✅ Scalability: Support 10,000+ organizations
- ✅ Audit: Complete audit trail for org operations
- ✅ Compliance: GDPR-compliant data handling

### Technical Requirements
- ✅ RESTful API implementation
- ✅ JWT-based tenant context
- ✅ Database schema per tenant (hybrid approach)
- ✅ Caching for tenant configurations
- ✅ Integration with all existing services
- ✅ Comprehensive error handling
- ✅ API documentation (OpenAPI/Swagger)

---

## 🧪 Testing Requirements

### Unit Tests
- Organization service methods
- Settings management
- Department/Location operations
- Invitation workflows
- Validation logic

### Integration Tests
- Organization CRUD operations
- Multi-tenant data isolation
- User-organization relationships
- Invitation accept/reject flows
- Cross-service integration

### Security Tests
- Tenant isolation verification
- Access control validation
- Data leak prevention
- SQL injection prevention
- XSS protection

### Performance Tests
- Tenant context resolution speed
- Concurrent organization operations
- Large hierarchy handling
- Query performance with tenant filtering

---

## 📈 Success Metrics

### Technical Metrics
- API response time < 200ms (95th percentile)
- 100% tenant data isolation (no cross-tenant leaks)
- 99.9% uptime for organization service
- Zero security vulnerabilities

### Business Metrics
- Support for 10,000+ organizations
- < 5 minute organization provisioning time
- 100% successful invitations delivered
- User satisfaction score > 4.5/5

### Operational Metrics
- Automated tenant provisioning
- Zero manual intervention for org creation
- Complete audit trail coverage
- Compliance with data regulations

---

## 🔗 Dependencies

### External Dependencies
- User Management Service
- Auth Service
- RBAC Service
- Email Service
- File Storage Service

### Database Dependencies
- PostgreSQL 17
- Redis 7 (for caching)
- MongoDB (for audit logs)

### Infrastructure Dependencies
- API Gateway
- Service Discovery (Eureka)
- Message Queue (Kafka/RabbitMQ)

---

## 📚 References

- Multi-Tenancy Design Patterns: https://docs.microsoft.com/en-us/azure/architecture/guide/multitenant/overview
- SaaS Security Best Practices
- GDPR Compliance Guidelines
- Data Isolation Strategies
- Tenant Provisioning Patterns

