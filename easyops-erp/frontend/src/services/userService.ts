import api from './api';
import {
  User,
  UserCreateRequest,
  UserUpdateRequest,
  PageRequest,
  PageResponse,
} from '../types/index';

class UserService {
  async createUser(data: UserCreateRequest): Promise<User> {
    const response = await api.post<User>('/api/users', data);
    return response.data;
  }

  async getUserById(id: string): Promise<User> {
    const response = await api.get<User>(`/api/users/${id}`);
    return response.data;
  }

  async getAllUsers(params?: PageRequest): Promise<PageResponse<User>> {
    const response = await api.get<PageResponse<User>>('/api/users', {
      params,
    });
    return response.data;
  }

  async updateUser(id: string, data: UserUpdateRequest): Promise<User> {
    const response = await api.put<User>(`/api/users/${id}`, data);
    return response.data;
  }

  async deleteUser(id: string): Promise<void> {
    await api.delete(`/api/users/${id}`);
  }

  async activateUser(id: string): Promise<User> {
    const response = await api.patch<User>(`/api/users/${id}/activate`);
    return response.data;
  }

  async deactivateUser(id: string): Promise<User> {
    const response = await api.patch<User>(`/api/users/${id}/deactivate`);
    return response.data;
  }

  async searchUsers(searchTerm: string, params?: PageRequest): Promise<PageResponse<User>> {
    const response = await api.get<PageResponse<User>>('/api/users/search', {
      params: { searchTerm, ...params },
    });
    return response.data;
  }

  async getUserStats(): Promise<{
    totalUsers: number;
    activeUsers: number;
    inactiveUsers: number;
  }> {
    const response = await api.get('/api/users/stats');
    return response.data as { totalUsers: number; activeUsers: number; inactiveUsers: number };
  }
}

export default new UserService();

