# Roles and Permissions

This document provides comprehensive definitions and details of all roles within the EasyOps ERP system. Roles are organized into system-level roles and business/functional roles.

---

## Table of Contents

1. [System-Level Roles](#system-level-roles)
2. [Business/Functional Roles](#businessfunctional-roles)
3. [Role Hierarchy](#role-hierarchy)
4. [Permission Model](#permission-model)
5. [Role Assignment Guidelines](#role-assignment-guidelines)

---

## System-Level Roles

System-level roles are predefined roles that control access to core system functionality and administrative features. These roles are created automatically during system initialization and cannot be deleted.

### 1. System Administrator (SYSTEM_ADMIN)

**Code:** `SYSTEM_ADMIN`  
**Type:** System Role  
**Description:** Full system access with unrestricted permissions across all modules and organizations.

#### Permissions
- **All Permissions:** Has access to all permissions in the system
- **User Management:** Full control over user accounts (create, update, delete, activate, deactivate)
- **Role Management:** Create, modify, and delete roles and permissions
- **System Configuration:** Configure system-wide settings, parameters, and integrations
- **Organization Management:** Create, modify, and delete organizations
- **Audit Access:** Full access to audit logs and system activity tracking
- **Data Access:** Unrestricted access to all data across all modules and organizations

#### Responsibilities
- System-wide configuration and maintenance
- User and role management across all organizations
- System security and compliance oversight
- Database and infrastructure management
- System monitoring and troubleshooting
- Backup and disaster recovery management

#### Use Cases
- IT administrators managing the entire ERP system
- System integrators and consultants
- Technical support personnel requiring full system access

---

### 2. Organization Administrator (ORG_ADMIN)

**Code:** `ORG_ADMIN`  
**Type:** System Role  
**Description:** Organization-level administration with full control within a specific organization.

#### Permissions
- **User Management:** Manage user accounts within the organization (USER_MANAGE, USER_VIEW)
- **Role View:** View roles and permissions (ROLE_VIEW)
- **System View:** View system information (SYSTEM_VIEW)
- **Audit View:** View audit logs for organization activities (AUDIT_VIEW)
- **Organization Management:** Manage organization settings, departments, and locations (ORG_MANAGE)
- **Module Access:** Full access to all modules within the organization scope

#### Limitations
- Cannot manage roles and permissions (no ROLE_MANAGE)
- Cannot configure system-wide settings (no SYSTEM_CONFIG)
- Access limited to their assigned organization(s)
- Cannot access other organizations' data

#### Responsibilities
- Manage users within their organization
- Configure organization-specific settings
- Assign roles and permissions to organization users
- Monitor organization activity through audit logs
- Manage departments, locations, and organizational structure
- Oversee organization-level security and compliance

#### Use Cases
- HR managers responsible for user management
- Department heads managing their organizational unit
- Business administrators overseeing organization operations

---

### 3. User (USER)

**Code:** `USER`  
**Type:** System Role  
**Description:** Standard user access with basic viewing permissions and module-specific access based on additional role assignments.

#### Permissions
- **User View:** View user information (USER_VIEW)
- **System View:** View basic system information (SYSTEM_VIEW)
- **Module Access:** Access to assigned modules based on business role assignments

#### Limitations
- Cannot manage users or roles
- Cannot view audit logs
- Cannot configure system or organization settings
- Limited to viewing their own data and data shared with them

#### Responsibilities
- Perform day-to-day business operations within assigned modules
- Follow organizational policies and procedures
- Maintain data accuracy in their work areas
- Report issues and request access as needed

#### Use Cases
- Regular employees performing operational tasks
- Staff members working in specific modules (Sales, HR, Accounting, etc.)
- Users requiring basic system access with module-specific permissions

---

### 4. Guest (GUEST)

**Code:** `GUEST`  
**Type:** System Role  
**Description:** Limited guest access with minimal permissions, typically for external users or temporary access.

#### Permissions
- **Minimal Access:** Very limited read-only access to specific resources
- **No Write Access:** Cannot create, update, or delete any data
- **Restricted Modules:** Access only to explicitly granted modules or features

#### Limitations
- No user management capabilities
- No system configuration access
- No audit log access
- Very restricted data visibility
- Typically time-limited access

#### Responsibilities
- Adhere to guest access policies
- Use system resources responsibly
- Report any issues encountered

#### Use Cases
- External consultants with temporary access
- Partners or vendors requiring limited system visibility
- Temporary users awaiting full access approval
- Demo or trial users

---

## Business/Functional Roles

Business roles define functional responsibilities within specific modules or business areas. These roles are typically assigned in addition to system-level roles to grant module-specific permissions.

### 1. Manager

**Type:** Business Role  
**Description:** Management-level access with oversight and approval capabilities across assigned modules.

#### Typical Permissions
- **Approval Workflows:** Approve transactions, requests, and changes
- **Data Access:** View and manage data within their department/area
- **Reporting:** Access to management reports and analytics
- **User Management:** Assign tasks and manage team members (within scope)
- **Configuration:** Configure module-specific settings for their area

#### Module-Specific Access
- **Sales Manager:** Approve quotations, manage sales team, view sales analytics
- **HR Manager:** Approve leave requests, manage employee records, view HR reports
- **Finance Manager:** Approve journal entries, manage budgets, view financial reports
- **Inventory Manager:** Approve stock adjustments, manage warehouses, view inventory reports

#### Responsibilities
- Oversee team operations and performance
- Approve transactions and requests within authority limits
- Review and analyze reports for decision-making
- Ensure compliance with organizational policies
- Manage team access and permissions

---

### 2. Salesperson

**Type:** Business Role  
**Description:** Sales team member with access to sales and CRM modules for managing customer relationships and sales processes.

#### Typical Permissions
- **Customer Management:** Create and update customer/contact records
- **Sales Transactions:** Create quotations, sales orders, and invoices
- **CRM Access:** Manage leads, opportunities, and customer communications
- **Product Catalog:** View product information and pricing
- **Sales Reports:** View personal and team sales performance

#### Limitations
- Cannot approve transactions above their authority
- Limited access to financial data
- Cannot modify system configurations
- Access restricted to their assigned customers/territories

#### Responsibilities
- Manage customer relationships and communications
- Create and follow up on sales opportunities
- Process sales orders and quotations
- Maintain accurate customer data
- Achieve sales targets and objectives

---

### 3. Accountant

**Type:** Business Role  
**Description:** Financial professional with access to accounting modules for managing financial transactions and records.

#### Typical Permissions
- **Journal Entries:** Create and post journal entries (within approval limits)
- **General Ledger:** View and manage general ledger accounts
- **Accounts Receivable:** Manage customer invoices and payments
- **Accounts Payable:** Manage vendor invoices and payments
- **Bank Reconciliation:** Perform bank reconciliations
- **Financial Reports:** Generate and view financial reports
- **Chart of Accounts:** View and suggest account modifications

#### Limitations
- May require approval for certain transactions
- Cannot modify locked periods
- Limited access to system configurations
- Cannot approve their own transactions

#### Responsibilities
- Record and maintain accurate financial transactions
- Perform reconciliations and period-end closing activities
- Generate financial reports and statements
- Ensure compliance with accounting standards
- Maintain proper documentation and audit trails

---

### 4. Finance Manager/Controller

**Type:** Business Role  
**Description:** Senior financial role with approval authority and oversight of financial operations.

#### Typical Permissions
- **All Accountant Permissions:** Plus additional approval capabilities
- **Approval Authority:** Approve journal entries, adjustments, and financial transactions
- **Period Management:** Lock and unlock accounting periods
- **Budget Management:** Create and manage budgets
- **Advanced Reporting:** Access to all financial reports and analytics
- **Configuration:** Configure accounting settings and rules

#### Responsibilities
- Oversee financial operations and controls
- Approve significant transactions and adjustments
- Manage fiscal periods and closing processes
- Review and analyze financial performance
- Ensure financial compliance and reporting accuracy

---

### 5. Auditor

**Type:** Business Role  
**Description:** Read-only access role for internal or external auditors reviewing system data and compliance.

#### Typical Permissions
- **Read-Only Access:** View all data across modules (no write access)
- **Audit Logs:** Full access to audit logs and activity tracking
- **Financial Reports:** Access to all financial reports and statements
- **Transaction Details:** Drill-down access to transaction details and supporting documents
- **Compliance Reports:** Access to compliance and regulatory reports

#### Limitations
- No write or modification permissions
- Cannot approve transactions
- Cannot configure system settings
- Access may be time-limited for external auditors

#### Responsibilities
- Review financial records and transactions
- Verify compliance with policies and regulations
- Analyze audit logs for anomalies
- Generate audit reports and findings
- Ensure data integrity and accuracy

---

### 6. Branch Accountant

**Type:** Business Role  
**Description:** Accountant with access scoped to a specific branch, warehouse, or company location.

#### Typical Permissions
- **Scoped Access:** All accountant permissions limited to assigned branch/warehouse/company
- **Branch-Specific Transactions:** Create and manage transactions for assigned location
- **Local Reporting:** Generate reports for assigned branch
- **Inventory Management:** Manage inventory for assigned warehouse

#### Limitations
- Cannot access other branches' data
- Cannot perform intercompany transactions without approval
- Limited to branch-specific configurations

#### Responsibilities
- Manage financial records for assigned branch
- Perform branch-level reconciliations
- Generate branch-specific reports
- Ensure branch compliance with accounting standards

---

### 7. Warehouse Staff

**Type:** Business Role  
**Description:** Inventory and warehouse operations personnel with access to inventory management modules.

#### Typical Permissions
- **Inventory Transactions:** Create and process inventory movements (receipts, issues, transfers)
- **Stock Management:** View stock levels and manage warehouse operations
- **Goods Receipt:** Process incoming goods and materials
- **Goods Issue:** Process outgoing goods and materials
- **Stock Adjustments:** Create stock adjustments (may require approval)
- **Inventory Reports:** View inventory reports and stock levels

#### Limitations
- Cannot approve stock adjustments above limits
- Limited access to financial data
- Cannot modify product master data
- Access restricted to assigned warehouses

#### Responsibilities
- Process inventory transactions accurately
- Maintain warehouse organization and stock accuracy
- Perform physical stock counts and reconciliations
- Follow warehouse procedures and safety protocols
- Report discrepancies and issues

---

### 8. Sales/Support

**Type:** Business Role  
**Description:** Customer service and support personnel with access to CRM and customer management features.

#### Typical Permissions
- **Contact Management:** Create and update customer contacts
- **Account Management:** Link contacts to accounts and manage relationships
- **Customer Communication:** Record and manage customer interactions
- **Support Tickets:** Create and manage support cases
- **Customer Data:** View customer information and history

#### Limitations
- Cannot merge duplicate records (requires Manager role)
- Cannot manage territories or ownership (requires Manager role)
- Limited access to financial transactions
- Cannot modify system configurations

#### Responsibilities
- Maintain accurate customer contact information
- Record customer interactions and communications
- Resolve customer inquiries and support cases
- Update customer records and preferences
- Escalate complex issues to managers

---

## Role Hierarchy

The role hierarchy in EasyOps ERP follows this structure:

```
System Administrator (SYSTEM_ADMIN)
    └── Full access across all organizations
    
Organization Administrator (ORG_ADMIN)
    └── Full access within assigned organization(s)
    
Business Roles (Manager, Accountant, etc.)
    └── Module-specific access within organization
    
Standard User (USER)
    └── Basic access with module-specific permissions
    
Guest (GUEST)
    └── Limited read-only access
```

### Role Assignment Rules

1. **Multiple Roles:** Users can have multiple roles assigned simultaneously
2. **Role Combination:** System roles (SYSTEM_ADMIN, ORG_ADMIN, USER, GUEST) can be combined with business roles
3. **Permission Aggregation:** When multiple roles are assigned, permissions are aggregated (union of all permissions)
4. **Restriction Override:** Higher-level roles (SYSTEM_ADMIN) override restrictions from lower-level roles
5. **Organization Scope:** ORG_ADMIN and business roles are scoped to specific organizations

---

## Permission Model

### Permission Structure

Permissions in EasyOps ERP follow a resource-action model:

- **Resource:** The entity or module being accessed (users, roles, system, organizations, etc.)
- **Action:** The operation being performed (view, manage, configure, etc.)

### Standard Permissions

| Permission Code | Resource | Action | Description |
|----------------|----------|--------|-------------|
| USER_MANAGE | users | manage | Manage user accounts (create, update, delete) |
| USER_VIEW | users | view | View user information |
| ROLE_MANAGE | roles | manage | Manage roles and permissions |
| ROLE_VIEW | roles | view | View roles and permissions |
| SYSTEM_CONFIG | system | configure | Configure system settings |
| SYSTEM_VIEW | system | view | View system information |
| AUDIT_VIEW | audit | view | View audit logs |
| ORG_MANAGE | organizations | manage | Manage organizations |

### Module-Specific Permissions

Additional permissions are defined for each module (Sales, HR, Accounting, Inventory, etc.) following the same resource-action pattern.

---

## Role Assignment Guidelines

### Best Practices

1. **Principle of Least Privilege:** Assign the minimum permissions necessary for users to perform their duties
2. **Role-Based Assignment:** Prefer assigning roles over individual permissions for easier management
3. **Regular Reviews:** Periodically review role assignments to ensure they remain appropriate
4. **Separation of Duties:** Ensure critical operations require multiple approvals or different roles
5. **Documentation:** Document any custom roles and their business justification

### Assignment Workflow

1. **Identify Requirements:** Determine the user's responsibilities and required access
2. **Select Base Role:** Assign appropriate system-level role (USER, ORG_ADMIN, etc.)
3. **Add Business Roles:** Assign relevant business/functional roles for module access
4. **Verify Permissions:** Confirm that assigned roles provide necessary permissions
5. **Test Access:** Verify user can access required modules and features
6. **Document Assignment:** Record role assignment with business justification

### Common Role Combinations

- **Sales Manager:** ORG_ADMIN + Manager (Sales) + Salesperson
- **Accountant:** USER + Accountant
- **Finance Controller:** ORG_ADMIN + Finance Manager/Controller + Accountant
- **Warehouse Manager:** USER + Manager (Inventory) + Warehouse Staff
- **HR Manager:** ORG_ADMIN + Manager (HR)

---

## Appendix

### Role Codes Reference

| Role | Code | Type |
|------|------|------|
| System Administrator | SYSTEM_ADMIN | System |
| Organization Administrator | ORG_ADMIN | System |
| User | USER | System |
| Guest | GUEST | System |
| Manager | MANAGER | Business |
| Salesperson | SALESPERSON | Business |
| Accountant | ACCOUNTANT | Business |
| Finance Manager/Controller | FINANCE_MANAGER | Business |
| Auditor | AUDITOR | Business |
| Branch Accountant | BRANCH_ACCOUNTANT | Business |
| Warehouse Staff | WAREHOUSE_STAFF | Business |
| Sales/Support | SALES_SUPPORT | Business |

### Related Documentation

- [RBAC Service Documentation](../easyops-erp/services/rbac-service/README.md)
- [User Management Guide](Module-HR/Administrator-Guide.md)
- [Security and Compliance Requirements](../requirements/cross-cutting/security_compliance.md)

---

**Last Updated:** 2024  
**Version:** 1.0  
**Maintained By:** EasyOps ERP Development Team
