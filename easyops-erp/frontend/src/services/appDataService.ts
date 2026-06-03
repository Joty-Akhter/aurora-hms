import api from './api';

export interface OrganizationAppData {
  id: string;
  organizationId: string;
  type: string;
  code: string;
  name: string;
  description?: string;
  extraAttributes?: string;
  isActive?: boolean;
  displayOrder?: number;
}

const appDataService = {
  async getAppData(
    organizationId: string,
    type: string
  ): Promise<OrganizationAppData[]> {
    const response = await api.get<OrganizationAppData[]>(`/api/organizations/${organizationId}/app-data`, {
      params: { type },
    });
    return response.data;
  },
};

export default appDataService;

