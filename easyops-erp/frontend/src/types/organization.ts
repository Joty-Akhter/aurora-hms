export interface Organization {
  id: string;
  code: string;
  name: string;
  legalName?: string;
  description?: string;
  logo?: string;
  website?: string;
  email?: string;
  phone?: string;
  industry?: string;
  businessType?: string;
  taxId?: string;
  registrationNumber?: string;
  fiscalYearStart?: string;
  currency?: string;
  timezone?: string;
  locale?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  themeMode?: 'light' | 'dark';
  themePrimaryColor?: string;
  themeSecondaryColor?: string;
  themeAccentColor?: string;
  themeSidebarColor?: string;
  themeSidebarTextColor?: string;
  subscriptionPlan?: string;
  subscriptionStatus?: string;
  subscriptionStartDate?: string;
  subscriptionEndDate?: string;
  trialEndsAt?: string;
  maxUsers?: number;
  maxStorage?: number;
  status?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Department {
  /** Organization-service id when present */
  id?: string;
  /** HR gateway / hr-service proxy primary key (same uuid as id in many deployments) */
  departmentId?: string;
  organizationId: string;
  parentDepartmentId?: string;
  code?: string;
  name: string;
  description?: string;
  type?: string;
  managerId?: string;
  costCenter?: string;
  status?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  children?: Department[];
}

export interface Location {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  type?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  phone?: string;
  email?: string;
  managerId?: string;
  timezone?: string;
  operatingHours?: Record<string, string>;
  coordinates?: {
    latitude: number;
    longitude: number;
  };
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Invitation {
  id: string;
  organizationId: string;
  email: string;
  role: string;
  invitedBy: string;
  status: string;
  expiresAt: string;
  acceptedAt?: string;
  createdAt: string;
}

export interface OrganizationSetting {
  key: string;
  value: string;
  type?: string;
  encrypted?: boolean;
}

export interface OrganizationFormData {
  code: string;
  name: string;
  legalName?: string;
  description?: string;
  /** Public URL of the logo image (HTTPS recommended). */
  logo?: string;
  email?: string;
  phone?: string;
  website?: string;
  industry?: string;
  businessType?: string;
  taxId?: string;
  registrationNumber?: string;
  fiscalYearStart?: string;
  currency?: string;
  timezone?: string;
  locale?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  themeMode?: 'light' | 'dark';
  themePrimaryColor?: string;
  themeSecondaryColor?: string;
  themeAccentColor?: string;
  themeSidebarColor?: string;
  themeSidebarTextColor?: string;
  subscriptionPlan?: string;
  maxUsers?: number;
  maxStorage?: number;
}

export interface DepartmentFormData {
  code: string;
  name: string;
  description?: string;
  type?: string;
  parentDepartmentId?: string;
  managerId?: string;
  costCenter?: string;
  status?: string;
}

export interface LocationFormData {
  code: string;
  name: string;
  type?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  phone?: string;
  email?: string;
  managerId?: string;
  timezone?: string;
  operatingHours?: Record<string, string>;
  coordinates?: {
    latitude: number;
    longitude: number;
  };
  status?: string;
}

