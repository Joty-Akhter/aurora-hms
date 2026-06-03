import api from './api';

// ========== Type Definitions ==========

export interface Manufacturer {
  id: string;
  name: string;
  shortCode?: string;
  country?: string;
  contactInfo?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ManufacturerRequest {
  name: string;
  shortCode?: string;
  country?: string;
  contactInfo?: string;
  active?: boolean;
}

export interface Drug {
  id: string;
  genericName: string;
  brandName?: string;
  strength?: string;
  form?: string;
  route?: string;
  packSize?: string;
  unitOfMeasure?: string;
  therapeuticClassId?: string;
  active: boolean;
  controlledDrugFlag: boolean;
  /** P4 — jurisdiction profile (e.g. US_DEA_II); optional */
  controlledProfileCode?: string;
  batchRequired: boolean;
  expiryRequired: boolean;
  manufacturerId: string;
  manufacturerName: string;
  createdAt: string;
  updatedAt: string;
}

export interface DrugRequest {
  genericName: string;
  brandName?: string;
  strength?: string;
  form?: string;
  route?: string;
  packSize?: string;
  unitOfMeasure?: string;
  therapeuticClassId?: string;
  active?: boolean;
  controlledDrugFlag?: boolean;
  batchRequired?: boolean;
  expiryRequired?: boolean;
  manufacturerId: string;
}

/** Normalize drug payloads: trim strings, omit empty optional fields, never send "" for UUID fields. */
function buildDrugWritePayload(payload: Partial<DrugRequest>): Record<string, unknown> {
  const trim = (s: string | undefined) => (s != null ? String(s).trim() : '');
  const out: Record<string, unknown> = {};
  if (payload.genericName !== undefined) {
    out.genericName = trim(payload.genericName);
  }
  if (payload.manufacturerId !== undefined && String(payload.manufacturerId).trim() !== '') {
    out.manufacturerId = String(payload.manufacturerId).trim();
  }
  if (payload.brandName !== undefined && trim(payload.brandName) !== '') {
    out.brandName = trim(payload.brandName);
  }
  if (payload.strength !== undefined && trim(payload.strength) !== '') {
    out.strength = trim(payload.strength);
  }
  if (payload.form !== undefined && trim(payload.form) !== '') {
    out.form = trim(payload.form);
  }
  if (payload.route !== undefined && trim(payload.route) !== '') {
    out.route = trim(payload.route);
  }
  if (payload.packSize !== undefined && trim(payload.packSize) !== '') {
    out.packSize = trim(payload.packSize);
  }
  if (payload.unitOfMeasure !== undefined && trim(payload.unitOfMeasure) !== '') {
    out.unitOfMeasure = trim(payload.unitOfMeasure);
  }
  if (payload.therapeuticClassId !== undefined && String(payload.therapeuticClassId).trim() !== '') {
    out.therapeuticClassId = payload.therapeuticClassId;
  }
  if (payload.active !== undefined) out.active = payload.active;
  if (payload.controlledDrugFlag !== undefined) out.controlledDrugFlag = payload.controlledDrugFlag;
  if (payload.batchRequired !== undefined) out.batchRequired = payload.batchRequired;
  if (payload.expiryRequired !== undefined) out.expiryRequired = payload.expiryRequired;
  return out;
}

export type PharmacyWorkflowType = 'SUPPLIER' | 'CENTRAL_STORE' | 'OUTLET_PHARMACY';

export interface PharmacyLocation {
  id: string;
  name: string;
  type: 'OPD' | 'IPD' | 'store' | 'ward_store' | string;
  workflowType?: PharmacyWorkflowType | string;
  is24x7: boolean;
  operationalHours?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PharmacyLocationRequest {
  name: string;
  type: string;
  workflowType?: PharmacyWorkflowType | string;
  is24x7?: boolean;
  operationalHours?: string;
  active?: boolean;
}

export interface PharmacyStockItem {
  stockId: string;
  drugId: string;
  genericName: string;
  brandName?: string;
  strength?: string;
  form?: string;
  route?: string;
  batchNumber?: string;
  expiryDate?: string;
  quantityOnHand: number;
}

export interface StockReceiptLine {
  drugId: string;
  quantity: number;
  batchNumber?: string;
  expiryDate?: string;
  referenceType?: string;
  referenceId?: string;
  notes?: string;
}

export interface StockAdjustmentLine {
  drugId: string;
  quantityDelta: number;
  batchNumber?: string;
  expiryDate?: string;
  reason?: string;
}

export interface StockTransferLine {
  drugId: string;
  quantity: number;
  batchNumber?: string;
  expiryDate?: string;
  notes?: string;
}

export interface StockTransferMovement {
  movementId: string;
  pharmacyLocationId: string;
  pharmacyLocationName: string;
  drugId: string;
  genericName: string;
  brandName?: string;
  movementType: 'transfer_in' | 'transfer_out' | string;
  quantity: number;
  batchNumber?: string;
  movementTime: string;
  notes?: string;
}

export interface StockAdjustmentMovement {
  movementId: string;
  pharmacyLocationId: string;
  pharmacyLocationName: string;
  drugId: string;
  genericName: string;
  brandName?: string;
  quantityDelta: number;
  batchNumber?: string;
  movementTime: string;
  reason?: string;
}

export interface ConsumptionReportItemResponse {
  drugId: string;
  genericName: string;
  brandName?: string;
  strength?: string;
  form?: string;
  route?: string;
  totalQuantityIssued: number;
  /** Present on sales-summary when server applies default unit price for estimates */
  estimatedRevenue?: number;
}

/** P4 WS-L2 — includes optional revenue estimates from default unit price config */
export interface SalesSummaryResponse {
  byDrug: ConsumptionReportItemResponse[];
  totalQuantityIssued: number;
  distinctDrugCount: number;
  estimatedRevenueTotal?: number;
  revenueEstimateUnitPrice?: number;
}

/** P4 WS-H3 — controlled substance register */
export interface ControlledSubstanceRegisterRow {
  dispenseLineId: string;
  dispenseOrderId: string;
  patientId?: string;
  pharmacyLocationId: string;
  pharmacyLocationName?: string;
  drugId: string;
  genericName: string;
  brandName?: string;
  controlledProfileCode?: string;
  quantityDispensed: number;
  batchNumber?: string;
  lineStatus: string;
  dispensedAt: string;
  witnessUserId?: string;
}

export type DispenseOrderStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type DispenseContextType = 'PATIENT_PRESCRIPTION' | 'WALK_IN' | 'DEPARTMENT_ISSUE';

export interface DispenseLine {
  id: string;
  dispenseOrderId: string;
  prescriptionLineId?: string;
  drugId: string;
  drugGenericName: string;
  drugBrandName?: string;
  batchNumber?: string;
  quantityPrescribed?: number;
  quantityDispensed: number;
  quantityReturned: number;
  status:
    | 'PENDING'
    | 'DISPENSED'
    | 'PARTIALLY_DISPENSED'
    | 'FILLED_WITH_STOCK_OVERRIDE'
    | 'REFUSED'
    | 'OUT_OF_STOCK'
    | 'NOT_STARTED'
    | 'RETURNED'
    | 'CANCELLED';
  reasonCode?: string;
  documentingUserId?: string;
  overrideReasonCode?: string;
  substitutedDrugId?: string;
  formularyOverrideReason?: string;
  /** P4 WS-I — documented override when dispensing despite interaction/allergy block */
  clinicalSafetyOverrideReason?: string;
  overrideApproverId?: string;
  witnessUserId?: string;
  remainingQuantity?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DispenseOrder {
  id: string;
  prescriptionId?: string;
  visitId?: string;
  patientId?: string;
  pharmacyLocationId: string;
  pharmacyLocationName: string;
  status: DispenseOrderStatus;
  contextType: DispenseContextType;
  departmentId?: string;
  createdAt: string;
  completedAt?: string;
  lines: DispenseLine[];
  /** Phase P3 — paper / regional metadata */
  paperPrescriptionRef?: string;
  prescriptionImageAttachmentId?: string;
  externalValidationStatus?: string;
}

export interface CreateDispenseOrderRequest {
  prescriptionId?: string;
  visitId?: string;
  patientId?: string;
  departmentId?: string;
  pharmacyLocationId: string;
  contextType: DispenseContextType;
  paperPrescriptionRef?: string;
  prescriptionImageAttachmentId?: string;
  externalValidationStatus?: string;
}

export interface DispenseLineRequest {
  drugId: string;
  prescriptionLineId?: string;
  batchNumber?: string;
  quantityPrescribed?: number;
  quantityDispensed: number;
  /** When server allows negative stock override (org policy). */
  stockOverrideReason?: string;
  substitutedDrugId?: string;
  formularyOverrideReason?: string;
  clinicalSafetyOverrideReason?: string;
  witnessUserId?: string;
}

export interface PatchDispenseOrderRegionalRequest {
  paperPrescriptionRef?: string;
  prescriptionImageAttachmentId?: string;
  /** When true, clears prescription image attachment (backend ignores if prescriptionImageAttachmentId is sent). */
  clearPrescriptionImageAttachment?: boolean;
  externalValidationStatus?: string;
}

/** Phase P3 WS-L1 */
export interface StockOverrideLineReportResponse {
  dispenseLineId: string;
  dispenseOrderId: string;
  pharmacyLocationId?: string;
  pharmacyLocationName?: string;
  drugId: string;
  genericName: string;
  overrideReasonCode?: string;
  quantityDispensed: number;
  dispensedAt: string;
}

/** Read-only billable rows for billing integration (Phase P1). */
export interface BillableDispenseItemResponse {
  dispenseLineId: string;
  dispenseOrderId: string;
  drugId: string;
  drugGenericName: string;
  drugBrandName?: string;
  strength?: string;
  form?: string;
  unitOfMeasure?: string;
  batchNumber?: string;
  quantityPrescribed?: number;
  quantityDispensed: number;
  lineStatus: DispenseLine['status'];
  /** Present when line was filled with a stock override (audit for billing). */
  overrideReasonCode?: string | null;
  suggestedListPrice?: number | null;
  taxCodeHint?: string | null;
}

export interface DispenseReturnRequest {
  lines: {
    dispenseLineId: string;
    quantityReturned: number;
    reason?: string;
  }[];
}

/** Record OUT_OF_STOCK / REFUSED without issuing stock (Phase P2 — WS-C). */
export interface DispenseUnfulfilledLineRequest {
  drugId: string;
  prescriptionLineId?: string;
  quantityPrescribed?: number;
  lineStatus: 'OUT_OF_STOCK' | 'REFUSED';
  reasonCode: string;
  documentingUserId?: string;
}

const hospitalPharmacyService = {
  // Manufacturers
  async getManufacturers(params?: { name?: string; activeOnly?: boolean }): Promise<Manufacturer[]> {
    const response = await api.get<Manufacturer[]>('/api/hospital-pharmacy/manufacturers', {
      params,
    });
    return response.data;
  },

  async getManufacturerById(id: string): Promise<Manufacturer> {
    const response = await api.get<Manufacturer>(`/api/hospital-pharmacy/manufacturers/${id}`);
    return response.data;
  },

  async createManufacturer(payload: ManufacturerRequest): Promise<Manufacturer> {
    const response = await api.post<Manufacturer>('/api/hospital-pharmacy/manufacturers', payload);
    return response.data;
  },

  async updateManufacturer(id: string, payload: Partial<ManufacturerRequest>): Promise<Manufacturer> {
    const response = await api.patch<Manufacturer>(`/api/hospital-pharmacy/manufacturers/${id}`, payload);
    return response.data;
  },

  // Drugs
  async getDrugs(params?: { name?: string; activeOnly?: boolean; manufacturerId?: string }): Promise<Drug[]> {
    const response = await api.get<Drug[]>('/api/hospital-pharmacy/drugs', {
      params,
    });
    return response.data;
  },

  async getDrugById(id: string): Promise<Drug> {
    const response = await api.get<Drug>(`/api/hospital-pharmacy/drugs/${id}`);
    return response.data;
  },

  /** Phase P3 — preferred alternatives from formulary rules (for substitution UI). */
  async getFormularyAlternatives(drugId: string): Promise<Drug[]> {
    const response = await api.get<Drug[]>(`/api/hospital-pharmacy/drugs/${drugId}/formulary-alternatives`);
    return response.data;
  },

  async createDrug(payload: DrugRequest): Promise<Drug> {
    const body = buildDrugWritePayload(payload);
    const response = await api.post<Drug>('/api/hospital-pharmacy/drugs', body);
    return response.data;
  },

  async updateDrug(id: string, payload: Partial<DrugRequest>): Promise<Drug> {
    const body = buildDrugWritePayload(payload);
    const response = await api.patch<Drug>(`/api/hospital-pharmacy/drugs/${id}`, body);
    return response.data;
  },

  // Pharmacy locations
  async getPharmacies(params?: { activeOnly?: boolean }): Promise<PharmacyLocation[]> {
    const response = await api.get<PharmacyLocation[]>('/api/hospital-pharmacy/pharmacies', {
      params,
    });
    return response.data;
  },

  async getPharmacyById(id: string): Promise<PharmacyLocation> {
    const response = await api.get<PharmacyLocation>(`/api/hospital-pharmacy/pharmacies/${id}`);
    return response.data;
  },

  async createPharmacy(payload: PharmacyLocationRequest): Promise<PharmacyLocation> {
    const response = await api.post<PharmacyLocation>('/api/hospital-pharmacy/pharmacies', payload);
    return response.data;
  },

  async updatePharmacy(id: string, payload: Partial<PharmacyLocationRequest>): Promise<PharmacyLocation> {
    const response = await api.patch<PharmacyLocation>(`/api/hospital-pharmacy/pharmacies/${id}`, payload);
    return response.data;
  },

  // Stock (Phase 2)
  async getPharmacyStock(pharmacyId: string): Promise<PharmacyStockItem[]> {
    const response = await api.get<PharmacyStockItem[]>(
      `/api/hospital-pharmacy/pharmacies/${pharmacyId}/stock`
    );
    return response.data;
  },

  async receiveStock(pharmacyId: string, lines: StockReceiptLine[]): Promise<void> {
    await api.post(`/api/hospital-pharmacy/pharmacies/${pharmacyId}/stock/receipts`, { lines });
  },

  async adjustStock(
    pharmacyId: string,
    lines: StockAdjustmentLine[],
    approvedByUserId?: string
  ): Promise<void> {
    const payload: { lines: StockAdjustmentLine[]; approvedByUserId?: string } = { lines };
    if (approvedByUserId) {
      payload.approvedByUserId = approvedByUserId;
    }
    await api.post(`/api/hospital-pharmacy/pharmacies/${pharmacyId}/stock/adjustments`, payload);
  },

  async transferStock(
    sourcePharmacyId: string,
    destinationPharmacyLocationId: string,
    lines: StockTransferLine[],
    approvedByUserId?: string
  ): Promise<void> {
    const payload: {
      destinationPharmacyLocationId: string;
      lines: StockTransferLine[];
      approvedByUserId?: string;
    } = {
      destinationPharmacyLocationId,
      lines,
    };
    if (approvedByUserId) {
      payload.approvedByUserId = approvedByUserId;
    }
    await api.post(`/api/hospital-pharmacy/pharmacies/${sourcePharmacyId}/stock/transfers`, payload);
  },

  async getStockTransfers(pharmacyId: string): Promise<StockTransferMovement[]> {
    const response = await api.get<StockTransferMovement[]>(
      `/api/hospital-pharmacy/pharmacies/${pharmacyId}/stock/transfers`
    );
    return response.data;
  },

  async getStockAdjustments(pharmacyId: string): Promise<StockAdjustmentMovement[]> {
    const response = await api.get<StockAdjustmentMovement[]>(
      `/api/hospital-pharmacy/pharmacies/${pharmacyId}/stock/adjustments`
    );
    return response.data;
  },

  // Reports (Phase 4)
  async getNearExpiryStock(
    pharmacyId?: string,
    days = 30,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<PharmacyStockItem[]> {
    const response = await api.get<PharmacyStockItem[]>(
      '/api/hospital-pharmacy/reports/near-expiry',
      {
        params: {
          pharmacyId,
          days,
          productCode: filters?.productCode?.trim() || undefined,
          companyCode: filters?.companyCode?.trim() || undefined,
        },
      }
    );
    return response.data;
  },

  async getConsumptionReport(
    pharmacyId: string,
    from: string,
    to: string,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<ConsumptionReportItemResponse[]> {
    const response = await api.get<ConsumptionReportItemResponse[]>(
      '/api/hospital-pharmacy/reports/consumption',
      {
        params: {
          pharmacyId,
          from,
          to,
          productCode: filters?.productCode?.trim() || undefined,
          companyCode: filters?.companyCode?.trim() || undefined,
        },
      }
    );
    return response.data;
  },

  async getStockOverrideReport(
    pharmacyId: string,
    from: string,
    to: string,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<StockOverrideLineReportResponse[]> {
    const response = await api.get<StockOverrideLineReportResponse[]>(
      '/api/hospital-pharmacy/reports/stock-overrides',
      {
        params: {
          pharmacyId,
          from,
          to,
          productCode: filters?.productCode?.trim() || undefined,
          companyCode: filters?.companyCode?.trim() || undefined,
        },
      }
    );
    return response.data;
  },

  async getSalesSummary(
    pharmacyId: string,
    from: string,
    to: string,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<SalesSummaryResponse> {
    const response = await api.get<SalesSummaryResponse>('/api/hospital-pharmacy/reports/sales-summary', {
      params: {
        pharmacyId,
        from,
        to,
        productCode: filters?.productCode?.trim() || undefined,
        companyCode: filters?.companyCode?.trim() || undefined,
      },
    });
    return response.data;
  },

  async getControlledSubstanceRegister(
    pharmacyId: string,
    from: string,
    to: string,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<ControlledSubstanceRegisterRow[]> {
    const response = await api.get<ControlledSubstanceRegisterRow[]>(
      '/api/hospital-pharmacy/reports/controlled-substance-register',
      {
        params: {
          pharmacyId,
          from,
          to,
          productCode: filters?.productCode?.trim() || undefined,
          companyCode: filters?.companyCode?.trim() || undefined,
        },
      }
    );
    return response.data;
  },

  /** P4 L3 — CSV download (UTF-8) */
  async downloadConsumptionExport(
    pharmacyId: string,
    from: string,
    to: string,
    filters?: { productCode?: string; companyCode?: string }
  ): Promise<Blob> {
    const response = await api.get<Blob>('/api/hospital-pharmacy/reports/consumption/export', {
      params: {
        pharmacyId,
        from,
        to,
        productCode: filters?.productCode?.trim() || undefined,
        companyCode: filters?.companyCode?.trim() || undefined,
      },
      responseType: 'blob',
    });
    return response.data;
  },

  // Dispense Orders
  async createDispenseOrder(payload: CreateDispenseOrderRequest): Promise<DispenseOrder> {
    const response = await api.post<DispenseOrder>('/api/hospital-pharmacy/dispense-orders', payload);
    return response.data;
  },

  async getDispenseOrderById(id: string): Promise<DispenseOrder> {
    const response = await api.get<DispenseOrder>(`/api/hospital-pharmacy/dispense-orders/${id}`);
    return response.data;
  },

  async searchDispenseOrders(params: {
    patientId?: string;
    visitId?: string;
    pharmacyLocationId?: string;
    status?: DispenseOrderStatus;
  }): Promise<DispenseOrder[]> {
    const response = await api.get<DispenseOrder[]>('/api/hospital-pharmacy/dispense-orders', {
      params,
    });
    return response.data;
  },

  async addDispenseLines(
    orderId: string,
    lines: DispenseLineRequest[],
    options?: { idempotencyKey?: string }
  ): Promise<DispenseOrder> {
    const response = await api.post<DispenseOrder>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}/lines`,
      lines,
      {
        headers: options?.idempotencyKey
          ? { 'Idempotency-Key': options.idempotencyKey }
          : undefined,
      }
    );
    return response.data;
  },

  async getBillableItems(orderId: string): Promise<BillableDispenseItemResponse[]> {
    const response = await api.get<BillableDispenseItemResponse[]>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}/billable-items`
    );
    return response.data;
  },

  async recordReturns(
    orderId: string,
    payload: DispenseReturnRequest,
    options?: { idempotencyKey?: string }
  ): Promise<DispenseOrder> {
    const response = await api.post<DispenseOrder>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}/returns`,
      payload,
      {
        headers: options?.idempotencyKey
          ? { 'Idempotency-Key': options.idempotencyKey }
          : undefined,
      }
    );
    return response.data;
  },

  async recordUnfulfilledLine(
    orderId: string,
    payload: DispenseUnfulfilledLineRequest
  ): Promise<DispenseOrder> {
    const response = await api.post<DispenseOrder>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}/lines/unfulfilled`,
      payload
    );
    return response.data;
  },

  async updateDispenseOrderStatus(orderId: string, status: DispenseOrderStatus): Promise<DispenseOrder> {
    const response = await api.patch<DispenseOrder>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}`,
      null,
      { params: { status } }
    );
    return response.data;
  },

  async patchDispenseOrderRegional(
    orderId: string,
    payload: PatchDispenseOrderRegionalRequest
  ): Promise<DispenseOrder> {
    const response = await api.patch<DispenseOrder>(
      `/api/hospital-pharmacy/dispense-orders/${orderId}/regional`,
      payload
    );
    return response.data;
  },

  /** Opens in browser / print — caller may use URL.createObjectURL on the returned Blob. */
  async downloadDispenseReceiptPdf(orderId: string): Promise<Blob> {
    const response = await api.get(`/api/hospital-pharmacy/dispense-orders/${orderId}/receipt.pdf`, {
      responseType: 'blob',
    });
    return response.data as Blob;
  },
};

export default hospitalPharmacyService;

