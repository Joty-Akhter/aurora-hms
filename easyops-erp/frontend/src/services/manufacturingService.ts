import axios from 'axios';

const _apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const API_BASE_URL = _apiBase ? `${_apiBase}/api/manufacturing` : '/api/manufacturing';

// ==================== Interfaces ====================

export interface BomHeader {
  bomId?: string;
  organizationId: string;
  bomNumber?: string;
  productId: string;
  productCode?: string;
  productName?: string;
  bomType?: string; // MANUFACTURING, ENGINEERING, SALES, SERVICE, PHANTOM
  versionNumber?: number;
  revision?: string;
  status?: string; // DRAFT, APPROVED, ACTIVE, OBSOLETE
  effectiveFrom?: string;
  effectiveTo?: string;
  baseQuantity?: number;
  uom?: string;
  materialCost?: number;
  laborCost?: number;
  overheadCost?: number;
  totalCost?: number;
  description?: string;
  notes?: string;
  approvedBy?: string;
  approvedDate?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface BomLine {
  bomLineId?: string;
  bomHeader?: BomHeader;
  organizationId: string;
  componentId: string;
  componentCode?: string;
  componentName?: string;
  parentLine?: BomLine;
  levelNumber?: number;
  sequenceNumber?: number;
  quantityPerUnit: number;
  uom?: string;
  componentType?: string; // RAW_MATERIAL, SUB_ASSEMBLY, FINISHED_GOOD, PACKAGING
  isOptional?: boolean;
  isPhantom?: boolean;
  canSubstitute?: boolean;
  scrapPercentage?: number;
  leadTimeDays?: number;
  unitCost?: number;
  notes?: string;
  referenceDesignator?: string;
  isActive?: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface BomVersion {
  versionId?: string;
  bomHeader?: BomHeader;
  organizationId: string;
  versionNumber: number;
  revision?: string;
  changeType?: string; // MINOR, MAJOR, ECN, ECO
  changeReason?: string;
  changeDescription?: string;
  status?: string; // PENDING, APPROVED, REJECTED
  approvedBy?: string;
  approvedDate?: string;
  createdBy?: string;
  createdAt?: string;
}

export interface ProductRouting {
  routingId?: string;
  organizationId: string;
  routingNumber?: string;
  productId: string;
  bomId?: string;
  routingName?: string;
  operationSequence: number;
  operationCode?: string;
  operationName: string;
  workCenterCode?: string;
  workCenterName?: string;
  setupTime?: number;
  runTimePerUnit: number;
  teardownTime?: number;
  costPerHour?: number;
  setupCost?: number;
  description?: string;
  instructions?: string;
  qualityCheckRequired?: boolean;
  isActive?: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface WorkOrder {
  workOrderId?: string;
  organizationId: string;
  workOrderNumber?: string;
  productId: string;
  productCode?: string;
  productName?: string;
  bomId?: string;
  routingId?: string;
  orderType?: string; // PRODUCTION, REWORK, ASSEMBLY, DISASSEMBLY
  sourceType?: string; // SALES_ORDER, STOCK_REPLENISHMENT, MRP, MANUAL
  sourceReference?: string;
  quantityPlanned: number;
  quantityCompleted?: number;
  quantityScrapped?: number;
  quantityReworked?: number;
  quantityInProcess?: number;
  status?: string; // CREATED, RELEASED, IN_PROGRESS, PAUSED, COMPLETED, CLOSED, CANCELLED
  priority?: string; // LOW, MEDIUM, HIGH, URGENT
  plannedStartDate?: string;
  plannedEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  releaseDate?: string;
  sourceWarehouseId?: string;
  targetWarehouseId?: string;
  productionLine?: string;
  workCenterId?: string;
  materialCost?: number;
  laborCost?: number;
  overheadCost?: number;
  totalCost?: number;
  completionPercentage?: number;
  operationsCompleted?: number;
  totalOperations?: number;
  notes?: string;
  specialInstructions?: string;
  createdBy?: string;
  createdAt?: string;
  releasedBy?: string;
  completedBy?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface WorkOrderOperation {
  operationId?: string;
  workOrder?: WorkOrder;
  organizationId: string;
  routingOperationId?: string;
  operationSequence: number;
  operationCode?: string;
  operationName: string;
  workCenterId?: string;
  workCenterCode?: string;
  workCenterName?: string;
  assignedTo?: string;
  assignedDate?: string;
  status?: string; // PENDING, IN_PROGRESS, COMPLETED, PAUSED, SKIPPED, FAILED
  setupTimePlanned?: number;
  runTimePlanned?: number;
  teardownTimePlanned?: number;
  totalTimePlanned?: number;
  setupTimeActual?: number;
  runTimeActual?: number;
  teardownTimeActual?: number;
  totalTimeActual?: number;
  plannedStart?: string;
  plannedEnd?: string;
  actualStart?: string;
  actualEnd?: string;
  quantityCompleted?: number;
  quantityRejected?: number;
  quantityReworked?: number;
  qualityCheckRequired?: boolean;
  qualityCheckCompleted?: boolean;
  qualityInspector?: string;
  qualityCheckDate?: string;
  qualityResult?: string; // PASS, FAIL, CONDITIONAL_PASS
  laborCost?: number;
  overheadCost?: number;
  instructions?: string;
  notes?: string;
  failureReason?: string;
  createdBy?: string;
  createdAt?: string;
  completedBy?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface WorkOrderMaterial {
  materialId?: string;
  workOrder?: WorkOrder;
  organizationId: string;
  componentId: string;
  componentCode?: string;
  componentName?: string;
  bomLineId?: string;
  operationId?: string;
  operationSequence?: number;
  quantityRequired: number;
  quantityReserved?: number;
  quantityIssued?: number;
  quantityConsumed?: number;
  quantityReturned?: number;
  quantityScrapped?: number;
  uom?: string;
  status?: string; // PLANNED, RESERVED, ISSUED, CONSUMED, RETURNED
  warehouseId?: string;
  location?: string;
  batchNumber?: string;
  serialNumber?: string;
  lotNumber?: string;
  unitCost?: number;
  totalCost?: number;
  requiredDate?: string;
  reservedDate?: string;
  issuedDate?: string;
  consumedDate?: string;
  backflush?: boolean;
  notes?: string;
  createdBy?: string;
  createdAt?: string;
  issuedBy?: string;
  updatedBy?: string;
  updatedAt?: string;
}

// ==================== BOM Header API ====================

export const bomApi = {
  getAllBoms: (organizationId: string) =>
    axios.get<BomHeader[]>(`${API_BASE_URL}/boms`, { params: { organizationId } }),

  getBomById: (bomId: string) =>
    axios.get<BomHeader>(`${API_BASE_URL}/boms/${bomId}`),

  getBomByNumber: (bomNumber: string, organizationId: string) =>
    axios.get<BomHeader>(`${API_BASE_URL}/boms/number/${bomNumber}`, { params: { organizationId } }),

  getBomsByProduct: (productId: string, organizationId: string) =>
    axios.get<BomHeader[]>(`${API_BASE_URL}/boms/product/${productId}`, { params: { organizationId } }),

  getActiveBoms: (organizationId: string) =>
    axios.get<BomHeader[]>(`${API_BASE_URL}/boms/active`, { params: { organizationId } }),

  getLatestBomForProduct: (productId: string) =>
    axios.get<BomHeader>(`${API_BASE_URL}/boms/product/${productId}/latest`),

  createBom: (bom: BomHeader) =>
    axios.post<BomHeader>(`${API_BASE_URL}/boms`, bom),

  updateBom: (bomId: string, bom: BomHeader) =>
    axios.put<BomHeader>(`${API_BASE_URL}/boms/${bomId}`, bom),

  deleteBom: (bomId: string) =>
    axios.delete(`${API_BASE_URL}/boms/${bomId}`),

  approveBom: (bomId: string, approvedBy: string) =>
    axios.post<BomHeader>(`${API_BASE_URL}/boms/${bomId}/approve`, null, { params: { approvedBy } }),

  recalculateCosts: (bomId: string) =>
    axios.post<BomHeader>(`${API_BASE_URL}/boms/${bomId}/recalculate-costs`),

  getDashboardStats: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/boms/dashboard/stats`, { params: { organizationId } }),
};

// ==================== BOM Line API ====================

export const bomLineApi = {
  getBomLines: (bomId: string) =>
    axios.get<BomLine[]>(`${API_BASE_URL}/boms/${bomId}/lines`),

  getTopLevelComponents: (bomId: string) =>
    axios.get<BomLine[]>(`${API_BASE_URL}/boms/${bomId}/lines/top-level`),

  addBomLine: (bomLine: BomLine) =>
    axios.post<BomLine>(`${API_BASE_URL}/boms/${bomLine.bomHeader?.bomId}/lines`, bomLine),

  updateBomLine: (bomLineId: string, bomLine: BomLine) =>
    axios.put<BomLine>(`${API_BASE_URL}/boms/lines/${bomLineId}`, bomLine),

  deleteBomLine: (bomLineId: string) =>
    axios.delete(`${API_BASE_URL}/boms/lines/${bomLineId}`),
};

// ==================== BOM Explosion API ====================

export const bomExplosionApi = {
  explodeBom: (bomId: string, quantity: number = 1) =>
    axios.get(`${API_BASE_URL}/boms/${bomId}/explosion`, { params: { quantity } }),
};

// ==================== BOM Version API ====================

export const bomVersionApi = {
  getBomVersions: (bomId: string) =>
    axios.get<BomVersion[]>(`${API_BASE_URL}/boms/${bomId}/versions`),

  createBomVersion: (bomVersion: BomVersion) =>
    axios.post<BomVersion>(`${API_BASE_URL}/boms/${bomVersion.bomHeader?.bomId}/versions`, bomVersion),
};

// ==================== Product Routing API ====================

export const routingApi = {
  getAllRoutings: (organizationId: string) =>
    axios.get<ProductRouting[]>(`${API_BASE_URL}/routings`, { params: { organizationId } }),

  getRoutingById: (routingId: string) =>
    axios.get<ProductRouting>(`${API_BASE_URL}/routings/${routingId}`),

  getRoutingByNumber: (routingNumber: string, organizationId: string) =>
    axios.get<ProductRouting>(`${API_BASE_URL}/routings/number/${routingNumber}`, { params: { organizationId } }),

  getRoutingsByProduct: (productId: string) =>
    axios.get<ProductRouting[]>(`${API_BASE_URL}/routings/product/${productId}`),

  getActiveRoutingsByProduct: (productId: string) =>
    axios.get<ProductRouting[]>(`${API_BASE_URL}/routings/product/${productId}/active`),

  getActiveRoutingsByBom: (bomId: string) =>
    axios.get<ProductRouting[]>(`${API_BASE_URL}/routings/bom/${bomId}/active`),

  getRoutingsByWorkCenter: (workCenterCode: string) =>
    axios.get<ProductRouting[]>(`${API_BASE_URL}/routings/work-center/${workCenterCode}`),

  createRouting: (routing: ProductRouting) =>
    axios.post<ProductRouting>(`${API_BASE_URL}/routings`, routing),

  updateRouting: (routingId: string, routing: ProductRouting) =>
    axios.put<ProductRouting>(`${API_BASE_URL}/routings/${routingId}`, routing),

  deleteRouting: (routingId: string) =>
    axios.delete(`${API_BASE_URL}/routings/${routingId}`),

  calculateProductionTime: (productId: string, quantity: number = 1) =>
    axios.get(`${API_BASE_URL}/routings/product/${productId}/production-time`, { params: { quantity } }),
};

// ==================== Work Order API ====================

export const workOrderApi = {
  getAllWorkOrders: (organizationId: string) =>
    axios.get<WorkOrder[]>(`${API_BASE_URL}/work-orders`, { params: { organizationId } }),

  getWorkOrderById: (workOrderId: string) =>
    axios.get<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}`),

  getWorkOrderByNumber: (workOrderNumber: string, organizationId: string) =>
    axios.get<WorkOrder>(`${API_BASE_URL}/work-orders/number/${workOrderNumber}`, { params: { organizationId } }),

  getWorkOrdersByStatus: (status: string, organizationId: string) =>
    axios.get<WorkOrder[]>(`${API_BASE_URL}/work-orders/status/${status}`, { params: { organizationId } }),

  getActiveWorkOrders: (organizationId: string) =>
    axios.get<WorkOrder[]>(`${API_BASE_URL}/work-orders/active`, { params: { organizationId } }),

  getOverdueWorkOrders: (organizationId: string) =>
    axios.get<WorkOrder[]>(`${API_BASE_URL}/work-orders/overdue`, { params: { organizationId } }),

  createWorkOrder: (workOrder: WorkOrder) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders`, workOrder),

  updateWorkOrder: (workOrderId: string, workOrder: WorkOrder) =>
    axios.put<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}`, workOrder),

  deleteWorkOrder: (workOrderId: string) =>
    axios.delete(`${API_BASE_URL}/work-orders/${workOrderId}`),

  // Lifecycle operations
  releaseWorkOrder: (workOrderId: string, releasedBy: string) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}/release`, null, { params: { releasedBy } }),

  startWorkOrder: (workOrderId: string, userId: string) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}/start`, null, { params: { userId } }),

  completeWorkOrder: (workOrderId: string, completedBy: string) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}/complete`, null, { params: { completedBy } }),

  closeWorkOrder: (workOrderId: string) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}/close`),

  cancelWorkOrder: (workOrderId: string, reason: string) =>
    axios.post<WorkOrder>(`${API_BASE_URL}/work-orders/${workOrderId}/cancel`, null, { params: { reason } }),

  // Progress and costs
  updateProgress: (workOrderId: string) =>
    axios.post(`${API_BASE_URL}/work-orders/${workOrderId}/progress/update`),

  recalculateCosts: (workOrderId: string) =>
    axios.post(`${API_BASE_URL}/work-orders/${workOrderId}/costs/recalculate`),

  getDashboardStats: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/work-orders/dashboard/stats`, { params: { organizationId } }),
};

// ==================== Work Order Material API ====================

export const workOrderMaterialApi = {
  reserveMaterials: (workOrderId: string) =>
    axios.post(`${API_BASE_URL}/work-orders/${workOrderId}/materials/reserve`),

  unreserveMaterials: (workOrderId: string) =>
    axios.post(`${API_BASE_URL}/work-orders/${workOrderId}/materials/unreserve`),

  issueMaterial: (materialId: string, quantity: number, issuedBy: string) =>
    axios.post<WorkOrderMaterial>(`${API_BASE_URL}/work-orders/materials/${materialId}/issue`, null, {
      params: { quantity, issuedBy }
    }),

  consumeMaterial: (materialId: string, quantity: number) =>
    axios.post<WorkOrderMaterial>(`${API_BASE_URL}/work-orders/materials/${materialId}/consume`, null, {
      params: { quantity }
    }),

  backflushMaterials: (workOrderId: string) =>
    axios.post(`${API_BASE_URL}/work-orders/${workOrderId}/materials/backflush`),
};

// ==================== Work Order Operation API ====================

export const workOrderOperationApi = {
  getWorkOrderOperations: (workOrderId: string) =>
    axios.get<WorkOrderOperation[]>(`${API_BASE_URL}/work-orders/${workOrderId}/operations`),

  startOperation: (operationId: string, userId: string) =>
    axios.post<WorkOrderOperation>(`${API_BASE_URL}/work-orders/operations/${operationId}/start`, null, {
      params: { userId }
    }),

  completeOperation: (operationId: string, quantityCompleted: number, completedBy: string) =>
    axios.post<WorkOrderOperation>(`${API_BASE_URL}/work-orders/operations/${operationId}/complete`, null, {
      params: { quantityCompleted, completedBy }
    }),
};

export interface QualityInspection {
  inspectionId?: string;
  organizationId: string;
  inspectionNumber?: string;
  workOrderId?: string;
  operationId?: string;
  productId: string;
  productCode?: string;
  productName?: string;
  inspectionType: string; // RECEIVING, IN_PROCESS, FINAL, AUDIT, FIRST_ARTICLE
  inspectionStage?: string;
  inspectionDate: string;
  inspectorId?: string;
  inspectorName?: string;
  status?: string; // PENDING, IN_PROGRESS, COMPLETED
  overallResult?: string; // PASS, FAIL, CONDITIONAL_PASS
  lotNumber?: string;
  batchNumber?: string;
  serialNumber?: string;
  sampleSize?: number;
  sampleMethod?: string;
  quantityInspected?: number;
  quantityPassed?: number;
  quantityFailed?: number;
  quantityReworked?: number;
  defectsFound?: number;
  criticalDefects?: number;
  majorDefects?: number;
  minorDefects?: number;
  passRate?: number;
  defectRate?: number;
  inspectionCriteria?: string;
  notes?: string;
  correctiveActions?: string;
  approvedBy?: string;
  approvedDate?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface QualityInspectionItem {
  itemId?: string;
  qualityInspection?: QualityInspection;
  organizationId: string;
  sequenceNumber?: number;
  parameterCode?: string;
  parameterName: string;
  parameterType?: string;
  specificationMin?: number;
  specificationMax?: number;
  specificationTarget?: number;
  specificationUnit?: string;
  specificationText?: string;
  measuredValue?: number;
  measuredText?: string;
  passFail?: string; // PASS, FAIL, NA
  isCritical?: boolean;
  deviation?: number;
  deviationPercentage?: number;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface NonConformance {
  ncId?: string;
  organizationId: string;
  ncNumber?: string;
  workOrderId?: string;
  operationId?: string;
  inspectionId?: string;
  productId?: string;
  productCode?: string;
  productName?: string;
  ncType: string; // DEFECT, DEVIATION, NON_COMPLIANCE, PROCESS_FAILURE
  severity?: string; // CRITICAL, MAJOR, MINOR
  category?: string;
  status?: string; // OPEN, IN_REVIEW, REWORK, SCRAP, APPROVED, CLOSED
  description: string;
  location?: string;
  quantityAffected?: number;
  quantityReworked?: number;
  quantityScrapped?: number;
  rootCause?: string;
  rootCauseDescription?: string;
  immediateAction?: string;
  correctiveAction?: string;
  preventiveAction?: string;
  reportedBy?: string;
  reportedDate?: string;
  assignedTo?: string;
  disposition?: string;
  resolutionNotes?: string;
  resolvedBy?: string;
  resolvedDate?: string;
  verifiedBy?: string;
  verifiedDate?: string;
  effectivenessCheck?: boolean;
  costImpact?: number;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface WorkCenter {
  workCenterId?: string;
  organizationId: string;
  workCenterCode: string;
  workCenterName: string;
  workCenterType?: string;
  category?: string;
  location?: string;
  department?: string;
  capacityPerHour?: number;
  capacityUom?: string;
  numberOfMachines?: number;
  numberOfOperators?: number;
  shiftsPerDay?: number;
  hoursPerShift?: number;
  workingDaysPerWeek?: number;
  efficiencyPercentage?: number;
  utilizationPercentage?: number;
  oeeTarget?: number;
  status?: string;
  isActive?: boolean;
  costPerHour?: number;
  setupCost?: number;
  overheadRate?: number;
  lastMaintenanceDate?: string;
  nextMaintenanceDate?: string;
  maintenanceFrequencyDays?: number;
  description?: string;
  notes?: string;
  specifications?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface EquipmentMaintenance {
  maintenanceId?: string;
  organizationId: string;
  maintenanceNumber?: string;
  workCenterId: string;
  maintenanceType: string;
  priority?: string;
  status?: string;
  scheduledDate: string;
  scheduledDurationHours?: number;
  actualStartDate?: string;
  actualEndDate?: string;
  actualDurationHours?: number;
  description: string;
  workPerformed?: string;
  partsReplaced?: string;
  findings?: string;
  recommendations?: string;
  assignedTo?: string;
  technicianId?: string;
  technicianName?: string;
  laborCost?: number;
  partsCost?: number;
  totalCost?: number;
  downtimeHours?: number;
  productionLoss?: number;
  followUpRequired?: boolean;
  followUpDate?: string;
  followUpNotes?: string;
  completedBy?: string;
  completionNotes?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

// ==================== Quality Inspection API ====================

export const qualityInspectionApi = {
  getAllInspections: (organizationId: string) =>
    axios.get<QualityInspection[]>(`${API_BASE_URL}/quality/inspections`, { params: { organizationId } }),

  getInspectionById: (inspectionId: string) =>
    axios.get<QualityInspection>(`${API_BASE_URL}/quality/inspections/${inspectionId}`),

  getInspectionsByStatus: (status: string, organizationId: string) =>
    axios.get<QualityInspection[]>(`${API_BASE_URL}/quality/inspections/status/${status}`, { params: { organizationId } }),

  getInspectionsByProduct: (productId: string, organizationId: string) =>
    axios.get<QualityInspection[]>(`${API_BASE_URL}/quality/inspections/product/${productId}`, { params: { organizationId } }),

  getInspectionsByWorkOrder: (workOrderId: string) =>
    axios.get<QualityInspection[]>(`${API_BASE_URL}/quality/inspections/work-order/${workOrderId}`),

  getPendingInspections: (organizationId: string) =>
    axios.get<QualityInspection[]>(`${API_BASE_URL}/quality/inspections/pending`, { params: { organizationId } }),

  createInspection: (inspection: QualityInspection) =>
    axios.post<QualityInspection>(`${API_BASE_URL}/quality/inspections`, inspection),

  updateInspection: (inspectionId: string, inspection: QualityInspection) =>
    axios.put<QualityInspection>(`${API_BASE_URL}/quality/inspections/${inspectionId}`, inspection),

  deleteInspection: (inspectionId: string) =>
    axios.delete(`${API_BASE_URL}/quality/inspections/${inspectionId}`),

  completeInspection: (inspectionId: string, overallResult: string, completedBy: string) =>
    axios.post<QualityInspection>(`${API_BASE_URL}/quality/inspections/${inspectionId}/complete`, null, {
      params: { overallResult, completedBy }
    }),

  getInspectionItems: (inspectionId: string) =>
    axios.get<QualityInspectionItem[]>(`${API_BASE_URL}/quality/inspections/${inspectionId}/items`),

  addInspectionItem: (item: QualityInspectionItem) =>
    axios.post<QualityInspectionItem>(`${API_BASE_URL}/quality/inspections/${item.qualityInspection?.inspectionId}/items`, item),

  updateInspectionItem: (itemId: string, item: QualityInspectionItem) =>
    axios.put<QualityInspectionItem>(`${API_BASE_URL}/quality/inspections/items/${itemId}`, item),

  deleteInspectionItem: (itemId: string) =>
    axios.delete(`${API_BASE_URL}/quality/inspections/items/${itemId}`),

  getDashboardStats: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/quality/inspections/dashboard/stats`, { params: { organizationId } }),
};

// ==================== Non-Conformance API ====================

export const nonConformanceApi = {
  getAllNonConformances: (organizationId: string) =>
    axios.get<NonConformance[]>(`${API_BASE_URL}/quality/non-conformances`, { params: { organizationId } }),

  getNonConformanceById: (ncId: string) =>
    axios.get<NonConformance>(`${API_BASE_URL}/quality/non-conformances/${ncId}`),

  getNonConformancesByStatus: (status: string, organizationId: string) =>
    axios.get<NonConformance[]>(`${API_BASE_URL}/quality/non-conformances/status/${status}`, { params: { organizationId } }),

  getOpenNonConformances: (organizationId: string) =>
    axios.get<NonConformance[]>(`${API_BASE_URL}/quality/non-conformances/open`, { params: { organizationId } }),

  getCriticalOpenNCs: (organizationId: string) =>
    axios.get<NonConformance[]>(`${API_BASE_URL}/quality/non-conformances/critical`, { params: { organizationId } }),

  getNonConformancesByWorkOrder: (workOrderId: string) =>
    axios.get<NonConformance[]>(`${API_BASE_URL}/quality/non-conformances/work-order/${workOrderId}`),

  createNonConformance: (nc: NonConformance) =>
    axios.post<NonConformance>(`${API_BASE_URL}/quality/non-conformances`, nc),

  updateNonConformance: (ncId: string, nc: NonConformance) =>
    axios.put<NonConformance>(`${API_BASE_URL}/quality/non-conformances/${ncId}`, nc),

  deleteNonConformance: (ncId: string) =>
    axios.delete(`${API_BASE_URL}/quality/non-conformances/${ncId}`),

  assignNonConformance: (ncId: string, assignedTo: string) =>
    axios.post<NonConformance>(`${API_BASE_URL}/quality/non-conformances/${ncId}/assign`, null, { params: { assignedTo } }),

  resolveNonConformance: (ncId: string, disposition: string, resolutionNotes: string, resolvedBy: string) =>
    axios.post<NonConformance>(`${API_BASE_URL}/quality/non-conformances/${ncId}/resolve`, null, {
      params: { disposition, resolutionNotes, resolvedBy }
    }),

  getDashboardStats: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/quality/non-conformances/dashboard/stats`, { params: { organizationId } }),
};

// ==================== Work Center API ====================

export const workCenterApi = {
  getAllWorkCenters: (organizationId: string) =>
    axios.get<WorkCenter[]>(`${API_BASE_URL}/work-centers`, { params: { organizationId } }),

  getWorkCenterById: (workCenterId: string) =>
    axios.get<WorkCenter>(`${API_BASE_URL}/work-centers/${workCenterId}`),

  getWorkCenterByCode: (workCenterCode: string, organizationId: string) =>
    axios.get<WorkCenter>(`${API_BASE_URL}/work-centers/code/${workCenterCode}`, { params: { organizationId } }),

  getActiveWorkCenters: (organizationId: string) =>
    axios.get<WorkCenter[]>(`${API_BASE_URL}/work-centers/active`, { params: { organizationId } }),

  getWorkCentersByType: (type: string, organizationId: string) =>
    axios.get<WorkCenter[]>(`${API_BASE_URL}/work-centers/type/${type}`, { params: { organizationId } }),

  createWorkCenter: (workCenter: WorkCenter) =>
    axios.post<WorkCenter>(`${API_BASE_URL}/work-centers`, workCenter),

  updateWorkCenter: (workCenterId: string, workCenter: WorkCenter) =>
    axios.put<WorkCenter>(`${API_BASE_URL}/work-centers/${workCenterId}`, workCenter),

  deleteWorkCenter: (workCenterId: string) =>
    axios.delete(`${API_BASE_URL}/work-centers/${workCenterId}`),

  updateWorkCenterStatus: (workCenterId: string, status: string) =>
    axios.post<WorkCenter>(`${API_BASE_URL}/work-centers/${workCenterId}/status`, null, { params: { status } }),

  getDashboardStats: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/work-centers/dashboard/stats`, { params: { organizationId } }),
};

// ==================== Equipment Maintenance API ====================

export const equipmentMaintenanceApi = {
  getAllMaintenance: (organizationId: string) =>
    axios.get<EquipmentMaintenance[]>(`${API_BASE_URL}/work-centers/maintenance`, { params: { organizationId } }),

  getMaintenanceById: (maintenanceId: string) =>
    axios.get<EquipmentMaintenance>(`${API_BASE_URL}/work-centers/maintenance/${maintenanceId}`),

  getMaintenanceByWorkCenter: (workCenterId: string) =>
    axios.get<EquipmentMaintenance[]>(`${API_BASE_URL}/work-centers/${workCenterId}/maintenance`),

  getOverdueMaintenance: () =>
    axios.get<EquipmentMaintenance[]>(`${API_BASE_URL}/work-centers/maintenance/overdue`),

  createMaintenance: (maintenance: EquipmentMaintenance) =>
    axios.post<EquipmentMaintenance>(`${API_BASE_URL}/work-centers/maintenance`, maintenance),

  updateMaintenance: (maintenanceId: string, maintenance: EquipmentMaintenance) =>
    axios.put<EquipmentMaintenance>(`${API_BASE_URL}/work-centers/maintenance/${maintenanceId}`, maintenance),

  deleteMaintenance: (maintenanceId: string) =>
    axios.delete(`${API_BASE_URL}/work-centers/maintenance/${maintenanceId}`),

  startMaintenance: (maintenanceId: string, userId: string) =>
    axios.post<EquipmentMaintenance>(`${API_BASE_URL}/work-centers/maintenance/${maintenanceId}/start`, null, {
      params: { userId }
    }),

  completeMaintenance: (maintenanceId: string, completedBy: string) =>
    axios.post<EquipmentMaintenance>(`${API_BASE_URL}/work-centers/maintenance/${maintenanceId}/complete`, null, {
      params: { completedBy }
    }),
};

// ==================== Manufacturing Analytics API ====================

export const analyticsApi = {
  getManufacturingDashboard: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/dashboard`, { params: { organizationId } }),

  calculateOEE: (organizationId: string, workCenterCode?: string, date?: string) =>
    axios.get(`${API_BASE_URL}/analytics/oee`, { 
      params: { organizationId, workCenterCode, date } 
    }),

  getProductionTrends: (organizationId: string, period: string = 'DAILY', limit: number = 30) =>
    axios.get(`${API_BASE_URL}/analytics/trends/production`, { 
      params: { organizationId, period, limit } 
    }),

  getCostAnalysisByProduct: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/costs/by-product`, { params: { organizationId } }),

  getQualityMetricsSummary: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/quality/summary`, { params: { organizationId } }),

  getProductionSummaryReport: (organizationId: string, startDate?: string, endDate?: string) =>
    axios.get(`${API_BASE_URL}/analytics/reports/production-summary`, { 
      params: { organizationId, startDate, endDate } 
    }),

  getWorkCenterPerformance: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/work-centers/performance`, { params: { organizationId } }),

  getNonConformanceAnalytics: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/quality/non-conformances`, { params: { organizationId } }),

  getPerformanceSummary: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/performance/summary`, { params: { organizationId } }),

  getShopFloorDashboard: (organizationId: string) =>
    axios.get(`${API_BASE_URL}/analytics/shop-floor/dashboard`, { params: { organizationId } }),
};

export default {
  bomApi,
  bomLineApi,
  bomExplosionApi,
  bomVersionApi,
  routingApi,
  workOrderApi,
  workOrderMaterialApi,
  workOrderOperationApi,
  qualityInspectionApi,
  nonConformanceApi,
  workCenterApi,
  equipmentMaintenanceApi,
  analyticsApi,
};

