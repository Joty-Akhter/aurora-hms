import api from './api';
import {
  Organization,
  Department,
  Location,
  Invitation,
  OrganizationFormData,
  DepartmentFormData,
  LocationFormData,
  OrganizationSetting,
} from '@/types/organization';

class OrganizationService {
  // Organization endpoints
  async getAllOrganizations(page: number = 0, size: number = 10) {
    const response = await api.get<{ content: Organization[]; totalElements: number }>(
      `/api/organizations?page=${page}&size=${size}`
    );
    return response.data;
  }

  async getOrganizationById(id: string) {
    const response = await api.get<Organization>(`/api/organizations/${id}`);
    return response.data;
  }

  async getOrganizationByCode(code: string) {
    const response = await api.get<Organization>(`/api/organizations/code/${code}`);
    return response.data;
  }

  /** Organizations linked to the user (admin.user_organizations). Gateway forwards X-User-Id. */
  async getMyOrganizations(userId: string) {
    const response = await api.get<Organization[]>('/api/organizations/me', {
      headers: { 'X-User-Id': userId },
    });
    return response.data;
  }

  async createOrganization(data: OrganizationFormData) {
    const response = await api.post<Organization>('/api/organizations', data);
    return response.data;
  }

  async updateOrganization(id: string, data: Partial<OrganizationFormData>) {
    const response = await api.put<Organization>(`/api/organizations/${id}`, data);
    return response.data;
  }

  async updateTheme(id: string, theme: {
    themeMode?: string;
    themePrimaryColor?: string;
    themeSecondaryColor?: string;
    themeAccentColor?: string;
    themeSidebarColor?: string;
    themeSidebarTextColor?: string;
  }) {
    const response = await api.patch<Organization>(`/api/organizations/${id}/theme`, theme);
    return response.data;
  }

  /** Upload logo (JPEG, PNG, WebP, GIF, SVG; max 2 MB). Sets logo to GET /api/organizations/{id}/logo. */
  async uploadOrganizationLogo(organizationId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<Organization>(`/api/organizations/${organizationId}/logo`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  }

  async deleteOrganization(id: string) {
    await api.delete(`/api/organizations/${id}`);
  }

  async activateOrganization(id: string) {
    const response = await api.patch<Organization>(`/api/organizations/${id}/activate`);
    return response.data;
  }

  async suspendOrganization(id: string) {
    const response = await api.patch<Organization>(`/api/organizations/${id}/suspend`);
    return response.data;
  }

  async updateSubscription(id: string, plan: string, maxUsers?: number, maxStorage?: number) {
    const params = new URLSearchParams({ plan });
    if (maxUsers) params.append('maxUsers', maxUsers.toString());
    if (maxStorage) params.append('maxStorage', maxStorage.toString());
    
    const response = await api.patch<Organization>(
      `/api/organizations/${id}/subscription?${params.toString()}`
    );
    return response.data;
  }

  // Department endpoints
  async getDepartments(organizationId: string) {
    const response = await api.get<Department[]>(`/api/organizations/${organizationId}/departments`);
    return response.data;
  }

  async getDepartmentTree(organizationId: string) {
    const response = await api.get<Department[]>(`/api/organizations/${organizationId}/departments/tree`);
    return response.data;
  }

  async createDepartment(organizationId: string, data: DepartmentFormData) {
    const response = await api.post<Department>(`/api/organizations/${organizationId}/departments`, data);
    return response.data;
  }

  async updateDepartment(departmentId: string, data: Partial<DepartmentFormData>) {
    const response = await api.put<Department>(`/api/departments/${departmentId}`, data);
    return response.data;
  }

  async deleteDepartment(departmentId: string) {
    await api.delete(`/api/departments/${departmentId}`);
  }

  // Location endpoints
  async getLocations(organizationId: string) {
    const response = await api.get<Location[]>(`/api/organizations/${organizationId}/locations`);
    return response.data;
  }

  async getLocationsByType(organizationId: string, type: string) {
    const response = await api.get<Location[]>(`/api/organizations/${organizationId}/locations/type/${type}`);
    return response.data;
  }

  async createLocation(organizationId: string, data: LocationFormData) {
    const response = await api.post<Location>(`/api/organizations/${organizationId}/locations`, data);
    return response.data;
  }

  async updateLocation(locationId: string, data: Partial<LocationFormData>) {
    const response = await api.put<Location>(`/api/locations/${locationId}`, data);
    return response.data;
  }

  async deleteLocation(locationId: string) {
    await api.delete(`/api/locations/${locationId}`);
  }

  // Settings endpoints
  async getSettings(organizationId: string) {
    const response = await api.get<Record<string, string>>(`/api/organizations/${organizationId}/settings`);
    return response.data;
  }

  async getSetting(organizationId: string, key: string) {
    const response = await api.get<string>(`/api/organizations/${organizationId}/settings/${key}`);
    return response.data;
  }

  async setSetting(organizationId: string, setting: OrganizationSetting) {
    await api.put(`/api/organizations/${organizationId}/settings`, setting);
  }

  async deleteSetting(organizationId: string, key: string) {
    await api.delete(`/api/organizations/${organizationId}/settings/${key}`);
  }

  // Invitation endpoints
  async getInvitations(organizationId: string) {
    const response = await api.get<Invitation[]>(`/api/organizations/${organizationId}/invitations`);
    return response.data;
  }

  async sendInvitation(organizationId: string, email: string, role: string) {
    const response = await api.post<Invitation>(`/api/organizations/${organizationId}/invitations`, {
      email,
      role,
    });
    return response.data;
  }

  async acceptInvitation(token: string) {
    const response = await api.post<Invitation>(`/api/invitations/accept/${token}`);
    return response.data;
  }

  async cancelInvitation(invitationId: string) {
    await api.delete(`/api/invitations/${invitationId}`);
  }

  async getInvitationByToken(token: string) {
    const response = await api.get<Invitation>(`/api/invitations/token/${token}`);
    return response.data;
  }
}

export default new OrganizationService();

