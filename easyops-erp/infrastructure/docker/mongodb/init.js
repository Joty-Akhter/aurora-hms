// Initialize EasyOps ERP MongoDB Database
// This script sets up the initial MongoDB structure for Phase 0

// Switch to easyops database
db = db.getSiblingDB('easyops');

// Create collections for different modules
db.createCollection('users');
db.createCollection('organizations');
db.createCollection('roles');
db.createCollection('permissions');
db.createCollection('user_sessions');
db.createCollection('audit_logs');
db.createCollection('system_logs');
db.createCollection('notifications');

// Create indexes for performance
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "isActive": 1 });
db.users.createIndex({ "createdAt": 1 });

db.organizations.createIndex({ "code": 1 }, { unique: true });
db.organizations.createIndex({ "isActive": 1 });
db.organizations.createIndex({ "createdAt": 1 });

db.roles.createIndex({ "code": 1 }, { unique: true });
db.roles.createIndex({ "isActive": 1 });
db.roles.createIndex({ "isSystemRole": 1 });

db.permissions.createIndex({ "code": 1 }, { unique: true });
db.permissions.createIndex({ "resource": 1 });
db.permissions.createIndex({ "action": 1 });

db.user_sessions.createIndex({ "userId": 1 });
db.user_sessions.createIndex({ "token": 1 }, { unique: true });
db.user_sessions.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

db.audit_logs.createIndex({ "userId": 1 });
db.audit_logs.createIndex({ "action": 1 });
db.audit_logs.createIndex({ "createdAt": 1 });
db.audit_logs.createIndex({ "resourceType": 1 });
db.audit_logs.createIndex({ "resourceId": 1 });

db.system_logs.createIndex({ "level": 1 });
db.system_logs.createIndex({ "service": 1 });
db.system_logs.createIndex({ "createdAt": 1 });

db.notifications.createIndex({ "userId": 1 });
db.notifications.createIndex({ "isRead": 1 });
db.notifications.createIndex({ "createdAt": 1 });

// Insert default system roles
db.roles.insertMany([
    {
        name: "System Administrator",
        code: "SYSTEM_ADMIN",
        description: "Full system access",
        isSystemRole: true,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Organization Administrator",
        code: "ORG_ADMIN",
        description: "Organization-level administration",
        isSystemRole: true,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "User",
        code: "USER",
        description: "Standard user access",
        isSystemRole: true,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Guest",
        code: "GUEST",
        description: "Limited guest access",
        isSystemRole: true,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

// Insert default permissions
db.permissions.insertMany([
    {
        name: "User Management",
        code: "USER_MANAGE",
        resource: "users",
        action: "manage",
        description: "Manage user accounts",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "User View",
        code: "USER_VIEW",
        resource: "users",
        action: "view",
        description: "View user information",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Role Management",
        code: "ROLE_MANAGE",
        resource: "roles",
        action: "manage",
        description: "Manage roles and permissions",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Role View",
        code: "ROLE_VIEW",
        resource: "roles",
        action: "view",
        description: "View roles and permissions",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "System Configuration",
        code: "SYSTEM_CONFIG",
        resource: "system",
        action: "configure",
        description: "Configure system settings",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "System View",
        code: "SYSTEM_VIEW",
        resource: "system",
        action: "view",
        description: "View system information",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Audit Logs",
        code: "AUDIT_VIEW",
        resource: "audit",
        action: "view",
        description: "View audit logs",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Organization Management",
        code: "ORG_MANAGE",
        resource: "organizations",
        action: "manage",
        description: "Manage organizations",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

// Create a default organization
db.organizations.insertOne({
    name: "EasyOps Demo Organization",
    code: "DEMO_ORG",
    description: "Default demo organization for testing",
    domain: "demo.easyops.com",
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date()
});

// Create a default system administrator user
// Password: Admin123! (hashed with bcrypt)
db.users.insertOne({
    username: "admin",
    email: "admin@easyops.com",
    passwordHash: "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa",
    firstName: "System",
    lastName: "Administrator",
    phone: null,
    isActive: true,
    isVerified: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    lastLoginAt: null,
    createdBy: null,
    updatedBy: null,
    organizations: ["DEMO_ORG"],
    roles: ["SYSTEM_ADMIN"]
});

// Create system configuration documents
db.system_configs.insertMany([
    {
        key: "system.name",
        value: "EasyOps ERP",
        description: "System name",
        category: "general",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "system.version",
        value: "1.0.0",
        description: "System version",
        category: "general",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "system.timezone",
        value: "UTC",
        description: "Default timezone",
        category: "general",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.password.minLength",
        value: "8",
        description: "Minimum password length",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.password.requireUppercase",
        value: "true",
        description: "Require uppercase letters in password",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.password.requireLowercase",
        value: "true",
        description: "Require lowercase letters in password",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.password.requireNumbers",
        value: "true",
        description: "Require numbers in password",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.password.requireSymbols",
        value: "true",
        description: "Require symbols in password",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.session.timeout",
        value: "3600",
        description: "Session timeout in seconds",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.maxLoginAttempts",
        value: "5",
        description: "Maximum login attempts before lockout",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        key: "auth.lockout.duration",
        value: "900",
        description: "Account lockout duration in seconds",
        category: "security",
        isEncrypted: false,
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

print("EasyOps ERP MongoDB database initialized successfully!");
