import api from './api';

// ========== Types ==========

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface CreateOrderLineRequest {
  orderType: string; // LAB, RADIOLOGY, PROCEDURE
  itemCode: string;
  orderingNotes?: string;
  priority?: string;
}

export interface CreateOrderSetRequest {
  patientId: string;
  visitId?: string;
  orderingDoctorId?: string;
  orderingDepartmentId?: string;
  orderContext?: string; // OPD, IPD, ED
  priority?: string;    // STAT, ROUTINE, URGENT
  orders: CreateOrderLineRequest[];
}

export interface OrderSetResponse {
  id: string;
  patientId: string;
  visitId?: string;
  orderingDoctorId?: string;
  orderingDepartmentId?: string;
  orderContext?: string;
  priority?: string;
  createdAt: string;
  createdBy?: string;
}

export interface ClinicalOrderResponse {
  id: string;
  orderSetId: string;
  orderType: string;
  itemCode: string;
  status: string;
  priority?: string;
  orderingNotes?: string;
  performedAt?: string;
  performedBy?: string;
  cancelReason?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  externalSystemId?: string;
  resultStatus?: string;
  resultAvailableAt?: string;
  createdAt: string;
  createdBy?: string;
}

export interface OrderSetDetailResponse extends OrderSetResponse {
  orders: ClinicalOrderResponse[];
}

export interface ClinicalOrderDetailResponse extends ClinicalOrderResponse {
  worklistItems: WorklistItemResponse[];
  resultLinks: ResultLinkResponse[];
}

export interface ResultLinkResponse {
  id: string;
  orderId: string;
  systemType: string;
  externalSystemId?: string;
  viewerUrl?: string;
  version?: number;
  revisedAt?: string;
  createdAt: string;
}

export interface CreateResultLinkRequest {
  systemType: string; // LIS, RIS, PACS, INTERNAL
  externalSystemId?: string;
  viewerUrl?: string;
  version?: number;
  revisedAt?: string;
  /** When FINAL, order result_status and result_available_at are updated */
  resultStatus?: string;
}

export interface CancelOrderRequest {
  reason: string;
  cancelledBy?: string;
}

export interface UpdateOrderRequest {
  priority?: string;
  orderingNotes?: string;
}

export interface WorklistItemResponse {
  id: string;
  orderId: string;
  worklistType: string;
  assignedToUserId?: string;
  assignedToRole?: string;
  scheduledTime?: string;
  status: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorklistItemDetailResponse extends WorklistItemResponse {
  order?: ClinicalOrderResponse;
  orderSetId?: string;
  patientId?: string;
  visitId?: string;
}

export interface AssignWorklistRequest {
  assignedToUserId?: string;
  assignedToRole?: string;
}

export interface UpdateWorklistStatusRequest {
  status: string;
  remarks?: string;
}

export interface CopyOrderSetRequest {
  sourceOrderSetId: string;
  orderContext?: string;
  priority?: string;
}

export interface TatReportItem {
  orderType: string;
  count: number;
  avgTatHours: number | null;
}

export interface VolumeReportItem {
  groupKey: string | null;
  count: number;
}

const BASE = '/api/hospital-clinical-orders';

const hospitalClinicalOrdersService = {
  async createOrderSet(body: CreateOrderSetRequest): Promise<OrderSetResponse> {
    const response = await api.post<OrderSetResponse>(`${BASE}/order-sets`, body);
    return response.data;
  },

  async copyOrderSet(body: CopyOrderSetRequest): Promise<OrderSetResponse> {
    const response = await api.post<OrderSetResponse>(`${BASE}/order-sets/from-order-set`, body);
    return response.data;
  },

  async getOrderSet(id: string): Promise<OrderSetDetailResponse> {
    const response = await api.get<OrderSetDetailResponse>(`${BASE}/order-sets/${id}`);
    return response.data;
  },

  async getOrderSets(params: {
    facilityId?: string;
    patientId?: string;
    visitId?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<OrderSetResponse>> {
    const response = await api.get<PagedResponse<OrderSetResponse>>(`${BASE}/order-sets`, { params });
    return response.data;
  },

  async getOrder(id: string): Promise<ClinicalOrderDetailResponse> {
    const response = await api.get<ClinicalOrderDetailResponse>(`${BASE}/orders/${id}`);
    return response.data;
  },

  async getOrders(params: {
    facilityId?: string;
    patientId?: string;
    visitId?: string;
    orderSetId?: string;
    type?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<ClinicalOrderResponse>> {
    const response = await api.get<PagedResponse<ClinicalOrderResponse>>(`${BASE}/orders`, { params });
    return response.data;
  },

  async cancelOrder(id: string, body: CancelOrderRequest): Promise<ClinicalOrderResponse> {
    const response = await api.post<ClinicalOrderResponse>(`${BASE}/orders/${id}/cancel`, body);
    return response.data;
  },

  async updateOrder(id: string, body: UpdateOrderRequest): Promise<ClinicalOrderResponse> {
    const response = await api.patch<ClinicalOrderResponse>(`${BASE}/orders/${id}`, body);
    return response.data;
  },

  async createResultLink(orderId: string, body: CreateResultLinkRequest): Promise<ResultLinkResponse> {
    const response = await api.post<ResultLinkResponse>(`${BASE}/orders/${orderId}/results`, body);
    return response.data;
  },

  async getResultLinks(orderId: string): Promise<ResultLinkResponse[]> {
    const response = await api.get<ResultLinkResponse[]>(`${BASE}/orders/${orderId}/results`);
    return response.data;
  },

  async getWorklists(params: {
    facilityId?: string;
    type?: string;
    status?: string;
    assignedTo?: string;
    departmentId?: string;
    section?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<WorklistItemDetailResponse>> {
    const response = await api.get<PagedResponse<WorklistItemDetailResponse>>(`${BASE}/worklists`, { params });
    return response.data;
  },

  async assignWorklistItem(worklistItemId: string, body: AssignWorklistRequest): Promise<WorklistItemResponse> {
    const response = await api.post<WorklistItemResponse>(`${BASE}/worklists/${worklistItemId}/assign`, body);
    return response.data;
  },

  async updateWorklistStatus(worklistItemId: string, body: UpdateWorklistStatusRequest): Promise<WorklistItemResponse> {
    const response = await api.post<WorklistItemResponse>(`${BASE}/worklists/${worklistItemId}/status`, body);
    return response.data;
  },

  async getTatReport(params: { from: string; to: string; orderType?: string }): Promise<TatReportItem[]> {
    const response = await api.get<TatReportItem[]>(`${BASE}/reports/tat`, { params });
    return response.data;
  },

  async getVolumeReport(params: { from: string; to: string; groupBy?: 'orderType' | 'department' }): Promise<VolumeReportItem[]> {
    const response = await api.get<VolumeReportItem[]>(`${BASE}/reports/volumes`, { params });
    return response.data;
  },
};

export default hospitalClinicalOrdersService;
