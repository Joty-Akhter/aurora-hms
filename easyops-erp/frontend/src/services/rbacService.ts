import api from './api';
import {
  Role,
  RoleRequest,
  Permission,
  PermissionRequest,
  UserRoleRequest,
  AuthorizationRequest,
  PageRequest,
  PageResponse,
} from '@/types/index';

class RbacService {
  // Role Management
  async createRole(data: RoleRequest): Promise<Role> {
    const response = await api.post<Role>('/api/rbac/roles', data);
    return response.data;
  }

  async getRoleById(id: string): Promise<Role> {
    const response = await api.get<Role>(`/api/rbac/roles/${id}`);
    return response.data;
  }

  async getRoleByCode(code: string): Promise<Role> {
    const response = await api.get<Role>(`/api/rbac/roles/code/${code}`);
    return response.data;
  }

  async getAllRoles(params?: PageRequest): Promise<PageResponse<Role>> {
    const response = await api.get<PageResponse<Role>>('/api/rbac/roles', {
      params,
    });
    return response.data;
  }

  async getActiveRoles(): Promise<Role[]> {
    const response = await api.get<Role[]>('/api/rbac/roles/active');
    return response.data;
  }

  async getSystemRoles(): Promise<Role[]> {
    const response = await api.get<Role[]>('/api/rbac/roles/system');
    return response.data;
  }

  async updateRole(id: string, data: RoleRequest): Promise<Role> {
    const response = await api.put<Role>(`/api/rbac/roles/${id}`, data);
    return response.data;
  }

  async deleteRole(id: string): Promise<void> {
    await api.delete(`/api/rbac/roles/${id}`);
  }

  async addPermissionToRole(roleId: string, permissionId: string): Promise<Role> {
    const response = await api.post<Role>(
      `/api/rbac/roles/${roleId}/permissions/${permissionId}`
    );
    return response.data;
  }

  async removePermissionFromRole(roleId: string, permissionId: string): Promise<Role> {
    const response = await api.delete<Role>(
      `/api/rbac/roles/${roleId}/permissions/${permissionId}`
    );
    return response.data;
  }

  async searchRoles(query: string): Promise<Role[]> {
    const response = await api.get<Role[]>('/api/rbac/roles/search', {
      params: { query },
    });
    return response.data;
  }

  // Permission Management
  async createPermission(data: PermissionRequest): Promise<Permission> {
    const response = await api.post<Permission>('/api/rbac/permissions', data);
    return response.data;
  }

  async getPermissionById(id: string): Promise<Permission> {
    const response = await api.get<Permission>(`/api/rbac/permissions/${id}`);
    return response.data;
  }

  async getAllPermissions(params?: PageRequest): Promise<PageResponse<Permission>> {
    const response = await api.get<PageResponse<Permission>>('/api/rbac/permissions', {
      params,
    });
    return response.data;
  }

  async getActivePermissions(): Promise<Permission[]> {
    const response = await api.get<Permission[]>('/api/rbac/permissions/active');
    return response.data;
  }

  async getPermissionsByResource(resource: string): Promise<Permission[]> {
    const response = await api.get<Permission[]>(`/api/rbac/permissions/resource/${resource}`);
    return response.data;
  }

  async updatePermission(id: string, data: PermissionRequest): Promise<Permission> {
    const response = await api.put<Permission>(`/api/rbac/permissions/${id}`, data);
    return response.data;
  }

  async deletePermission(id: string): Promise<void> {
    await api.delete(`/api/rbac/permissions/${id}`);
  }

  // Authorization
  async assignRolesToUser(data: UserRoleRequest): Promise<Role[]> {
    const response = await api.post<Role[]>('/api/rbac/authorization/users/roles', data);
    return response.data;
  }

  async removeRoleFromUser(userId: string, roleId: string): Promise<void> {
    await api.delete(`/api/rbac/authorization/users/${userId}/roles/${roleId}`);
  }

  async removeAllRolesFromUser(userId: string): Promise<void> {
    await api.delete(`/api/rbac/authorization/users/${userId}/roles`);
  }

  async getUserRoles(userId: string, organizationId?: string | null): Promise<Role[]> {
    const params = organizationId ? { organizationId } : {};
    const response = await api.get<Role[]>(`/api/rbac/authorization/users/${userId}/roles`, {
      params,
    });
    return response.data;
  }

  async getUserPermissions(userId: string, organizationId?: string | null): Promise<Permission[]> {
    const params = organizationId ? { organizationId } : {};
    const response = await api.get<Permission[]>(
      `/api/rbac/authorization/users/${userId}/permissions`,
      { params }
    );
    return response.data;
  }

  /** Distinct org IDs from active role assignments + whether any assignment is global (no org). */
  async getUserOrganizationContext(userId: string): Promise<{
    organizationIds: string[];
    hasGlobalRoles: boolean;
  }> {
    const response = await api.get<{
      organizationIds: string[];
      hasGlobalRoles: boolean;
    }>(`/api/rbac/authorization/users/${userId}/organization-context`);
    return response.data;
  }

  async checkPermission(data: AuthorizationRequest): Promise<boolean> {
    const response = await api.post<{ authorized: boolean }>(
      '/api/rbac/authorization/check',
      data
    );
    return response.data.authorized;
  }

  async checkRole(userId: string, roleCode: string, organizationId?: string | null): Promise<boolean> {
    const params = organizationId ? { organizationId } : {};
    const response = await api.get<{ hasRole: boolean }>(
      `/api/rbac/authorization/users/${userId}/has-role/${roleCode}`,
      { params }
    );
    return response.data.hasRole;
  }

  async getUsersByRole(roleId: string): Promise<string[]> {
    const response = await api.get<string[]>(`/api/rbac/authorization/roles/${roleId}/users`);
    return response.data;
  }

  async searchPermissions(query: string): Promise<Permission[]> {
    const response = await api.get<Permission[]>('/api/rbac/permissions/search', {
      params: { query },
    });
    return response.data;
  }
}

export default new RbacService();

