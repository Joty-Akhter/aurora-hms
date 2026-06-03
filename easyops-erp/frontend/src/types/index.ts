// User Types
export interface User {
  id: string;
  username: string;
  email?: string | null;
  firstName?: string;
  lastName?: string;
  phone?: string;
  isActive: boolean;
  isVerified: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string;
}

export interface UserCreateRequest {
  username: string;
  email?: string | null;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

// Authentication Types
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  email?: string | null;
  firstName?: string;
  lastName?: string;
  roles: string[];
  permissions: string[];
}

export interface PasswordResetOptionsResponse {
  eligible: boolean;
  channels: string[];
  maskedEmail?: string | null;
  maskedPhone?: string | null;
}

export interface PasswordResetRequest {
  /** Legacy: email-only body selects EMAIL channel */
  email?: string | null;
  usernameOrEmail?: string;
  channel?: "EMAIL" | "SMS";
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  newPassword: string;
}

// Role Types
export interface Role {
  id: string;
  name: string;
  code: string;
  description?: string;
  isSystemRole: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  permissions: Permission[];
}

export interface RoleRequest {
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  permissionIds?: string[];
}

// Permission Types
export interface Permission {
  id: string;
  name: string;
  code: string;
  resource: string;
  action: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PermissionRequest {
  name: string;
  code: string;
  resource: string;
  action: string;
  description?: string;
  isActive: boolean;
}

// Authorization Types
export interface UserRoleRequest {
  userId: string;
  roleIds: string[];
  organizationId?: string;
  expiresAt?: string;
}

export interface AuthorizationRequest {
  userId: string;
  resource: string;
  action: string;
  organizationId?: string;
}

// Pagination Types
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// API Response Types
export interface ApiError {
  error: string;
  message?: string;
  status?: number;
  timestamp?: string;
}

export interface ApiResponse<T> {
  data?: T;
  error?: ApiError;
}

// Organization Types (re-export from organization.ts)
export * from './organization';

