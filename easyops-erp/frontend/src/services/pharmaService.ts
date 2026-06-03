import api from './api';

// Types
export interface Division {
  id: string;
  organizationId: string;
  name: string;
  code?: string;
  description?: string;
  status: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Region {
  id: string;
  organizationId: string;
  divisionId: string;
  name: string;
  code?: string;
  description?: string;
  status: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Territory {
  id: string;
  organizationId: string;
  divisionId: string;
  regionId: string;
  areaId: string;
  warehouseId?: string; // Auto-created when territory is created
  name: string;
  code?: string;
  description?: string;
  status: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Area {
  id: string;
  organizationId: string;
  divisionId: string;
  regionId: string;
  name: string;
  code?: string;
  description?: string;
  status: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

/** @deprecated Use EmployeeTerritoryAssignment - assignments are now territory-based */
export interface EmployeeAreaAssignment {
  id: string;
  organizationId: string;
  employeeId: string;
  areaId: string;
  assignmentDate: string;
  endDate?: string;
  roleInArea: string;
  status: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface EmployeeTerritoryAssignment {
  id: string;
  organizationId: string;
  employeeId: string;
  territoryId: string;
  assignmentDate: string;
  endDate?: string;
  roleInTerritory: string;
  status: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface ProductReceiptLine {
  id?: string;
  productReceiptId?: string;
  productId: string;
  productName?: string;
  packSize?: number;
  tpWithVat?: number;
  mrp?: number;
  quantity: number;
  amount?: number;
  expiryDate?: string;
}

export interface ProductReceipt {
  id?: string;
  organizationId: string;
  receiptDate: string;
  receiptNumber?: string;
  totalValue?: number;
  status: string;
  userName?: string;
  userDesignation?: string;
  notes?: string;
  receiptLines?: ProductReceiptLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

// Phase 2 Types
export interface ProductDisbursementLine {
  id?: string;
  productDisbursementId?: string;
  productId: string;
  productName?: string;
  packSize?: number;
  tpWithVat?: number;
  mrp?: number;
  previousMonthOpeningQuantity?: number;
  currentMonthQuantity: number;
  totalQuantity?: number;
  productAmount?: number;
}

export interface ProductDisbursement {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId: string;
  disbursementDate: string;
  year?: number;
  month?: number;
  previousMonthOpeningTotalDue?: number;
  totalSupplyAmount?: number;
  totalBalanceAmount?: number;
  status: string;
  notes?: string;
  disbursementLines?: ProductDisbursementLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Target {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId: string;
  year: number;
  startMonth: number;
  endMonth: number;
  targetAmount: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface TargetCoverage {
  id?: string;
  targetId?: string;
  territoryId: string;
  year: number;
  month: number;
  targetAmount: number;
  coveredAmount: number;
  coveragePercentage?: number;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DepositLine {
  id?: string;
  depositId?: string;
  productId: string;
  productName?: string;
  tpWithVat?: number;
  quantitySold: number;
  currentOutstandingQuantity?: number;
  productAmount?: number;
}

export interface Deposit {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId?: string;
  depositDate: string;
  year?: number;
  month?: number;
  depositAmount: number;
  bankAccountId?: string;
  bankName: string;
  bankAccountNumber: string;
  totalProductAmount?: number;
  status: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface SoldProductEntryLine {
  id?: string;
  soldProductEntryId?: string;
  productId: string;
  productName?: string;
  tpWithVat?: number;
  quantitySold: number;
  currentOutstandingQuantity?: number;
  productAmount?: number;
}

export interface SoldProductEntry {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId?: string;
  entryDate: string;
  year?: number;
  month?: number;
  totalProductAmount?: number;
  status: string;
  notes?: string;
  lines?: SoldProductEntryLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

// Phase 3 Types
export interface ExpenseCategory {
  id?: string;
  organizationId: string;
  name: string;
  description?: string;
  isActive: boolean;
}

export interface Expense {
  id?: string;
  organizationId: string;
  territoryId: string;
  expenseCategoryId: string;
  sourceEmployeeId?: string;
  expenseAmount: number;
  description?: string;
  expenseDate: string;
  year?: number;
  month?: number;
  receiptUrl?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface AdjustmentLine {
  id?: string;
  adjustmentId?: string;
  productId: string;
  productName?: string;
  quantity: number;
  tpWithVat?: number;
  amount?: number;
  reason?: string;
}

export interface Adjustment {
  id?: string;
  organizationId: string;
  territoryId: string;
  adjustmentDate: string;
  year?: number;
  month?: number;
  adjustmentType: 'DAMAGE' | 'EXPIRY' | 'OTHER';
  notes?: string;
  status: string;
  totalAmount?: number;
  adjustmentLines?: AdjustmentLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface IncentiveDistribution {
  id?: string;
  incentiveCalculationId?: string;
  employeeId: string;
  territoryId: string;
  roleInTerritory?: string;
  roleInArea?: string;
  incentiveAmount: number;
  distributionType: string;
  status: string;
  paidDate?: string;
  calculationDate?: string;
}

export interface IncentiveCalculation {
  id?: string;
  organizationId: string;
  territoryId: string;
  year: number;
  month: number;
  targetAmount: number;
  coveredAmount: number;
  targetAchieved: boolean;
  expenseWithinLimit: boolean;
  territoryEligible: boolean;
  incentiveBaseAmount: number;
  totalSrShare: number;
  totalMpoShare: number;
  totalManagerShare: number;
  totalIncentiveDistributed: number;
  calculationDate: string;
  status: string;
  distributions?: IncentiveDistribution[];
}

class PharmaService {
  // Division APIs
  async getDivisions(organizationId: string): Promise<Division[]> {
    const response = await api.get(`/api/pharma/territories/divisions?organizationId=${organizationId}`);
    return response.data;
  }

  async getActiveDivisions(organizationId: string): Promise<Division[]> {
    const response = await api.get(`/api/pharma/territories/divisions/active?organizationId=${organizationId}`);
    return response.data;
  }

  async getDivisionById(id: string): Promise<Division> {
    const response = await api.get(`/api/pharma/territories/divisions/${id}`);
    return response.data;
  }

  async createDivision(division: Division): Promise<Division> {
    const response = await api.post('/api/pharma/territories/divisions', division);
    return response.data;
  }

  async updateDivision(id: string, division: Division): Promise<Division> {
    const response = await api.put(`/api/pharma/territories/divisions/${id}`, division);
    return response.data;
  }

  async deleteDivision(id: string): Promise<void> {
    await api.delete(`/api/pharma/territories/divisions/${id}`);
  }

  // Region APIs
  async getRegionsByDivision(divisionId: string): Promise<Region[]> {
    const response = await api.get(`/api/pharma/territories/regions?divisionId=${divisionId}`);
    return response.data;
  }

  async getRegionById(id: string): Promise<Region> {
    const response = await api.get(`/api/pharma/territories/regions/${id}`);
    return response.data;
  }

  async createRegion(region: Region): Promise<Region> {
    const response = await api.post('/api/pharma/territories/regions', region);
    return response.data;
  }

  async updateRegion(id: string, region: Region): Promise<Region> {
    const response = await api.put(`/api/pharma/territories/regions/${id}`, region);
    return response.data;
  }

  async deleteRegion(id: string): Promise<void> {
    await api.delete(`/api/pharma/territories/regions/${id}`);
  }

  // Territory APIs
  async getTerritoriesByRegion(regionId: string): Promise<Territory[]> {
    const response = await api.get(`/api/pharma/territories/territories?regionId=${regionId}`);
    return response.data;
  }

  async getTerritoriesByArea(areaId: string): Promise<Territory[]> {
    const response = await api.get(`/api/pharma/territories/territories?areaId=${areaId}`);
    return response.data;
  }

  /** Fetches all territories for an organization (via areas) */
  async getAllTerritoriesForOrganization(organizationId: string): Promise<Territory[]> {
    const areas = await this.getAreas(organizationId, undefined, { includeInactive: true });
    const territoryArrays = await Promise.all(areas.map((a) => this.getTerritoriesByArea(a.id)));
    return territoryArrays.flat();
  }

  async getTerritoryById(id: string): Promise<Territory> {
    const response = await api.get(`/api/pharma/territories/territories/${id}`);
    return response.data;
  }

  async createTerritory(territory: Territory): Promise<Territory> {
    const response = await api.post('/api/pharma/territories/territories', territory);
    return response.data;
  }

  async updateTerritory(id: string, territory: Territory): Promise<Territory> {
    const response = await api.put(`/api/pharma/territories/territories/${id}`, territory);
    return response.data;
  }

  async deleteTerritory(id: string): Promise<void> {
    await api.delete(`/api/pharma/territories/territories/${id}`);
  }

  // Area APIs
  async getAreas(organizationId?: string, regionId?: string, options?: { includeInactive?: boolean }): Promise<Area[]> {
    const params = new URLSearchParams();
    if (organizationId) params.append('organizationId', organizationId);
    if (regionId) params.append('regionId', regionId);
    if (options?.includeInactive) params.append('includeInactive', 'true');
    const response = await api.get(`/api/pharma/territories/areas?${params.toString()}`);
    return response.data;
  }

  async getAreasByRegion(regionId: string): Promise<Area[]> {
    const response = await api.get(`/api/pharma/territories/areas?regionId=${regionId}`);
    return response.data;
  }

  async getAreaById(id: string): Promise<Area> {
    const response = await api.get(`/api/pharma/territories/areas/${id}`);
    return response.data;
  }

  async createArea(area: Area): Promise<Area> {
    const response = await api.post('/api/pharma/territories/areas', area);
    return response.data;
  }

  async updateArea(id: string, area: Area): Promise<Area> {
    const response = await api.put(`/api/pharma/territories/areas/${id}`, area);
    return response.data;
  }

  async deleteArea(id: string): Promise<void> {
    await api.delete(`/api/pharma/territories/areas/${id}`);
  }

  // Employee Assignment APIs (territory-based)
  async getAssignments(organizationId: string): Promise<EmployeeTerritoryAssignment[]> {
    const response = await api.get(`/api/pharma/employee-assignments?organizationId=${organizationId}`);
    return response.data;
  }

  async getAssignmentsByEmployee(employeeId: string): Promise<EmployeeTerritoryAssignment[]> {
    const response = await api.get(`/api/pharma/employee-assignments/employee/${employeeId}`);
    return response.data;
  }

  async getActiveAssignmentsByEmployee(employeeId: string): Promise<EmployeeTerritoryAssignment[]> {
    const response = await api.get(`/api/pharma/employee-assignments/employee/${employeeId}/active`);
    return response.data;
  }

  async getAssignmentsByTerritory(territoryId: string): Promise<EmployeeTerritoryAssignment[]> {
    const response = await api.get(`/api/pharma/employee-assignments/territory/${territoryId}`);
    return response.data;
  }

  async getActiveAssignmentsByTerritory(territoryId: string): Promise<EmployeeTerritoryAssignment[]> {
    const response = await api.get(`/api/pharma/employee-assignments/territory/${territoryId}/active`);
    return response.data;
  }

  async getAssignmentById(id: string): Promise<EmployeeTerritoryAssignment> {
    const response = await api.get(`/api/pharma/employee-assignments/${id}`);
    return response.data;
  }

  async createAssignment(assignment: EmployeeTerritoryAssignment): Promise<EmployeeTerritoryAssignment> {
    const response = await api.post('/api/pharma/employee-assignments', assignment);
    return response.data;
  }

  async updateAssignment(id: string, assignment: EmployeeTerritoryAssignment): Promise<EmployeeTerritoryAssignment> {
    const response = await api.put(`/api/pharma/employee-assignments/${id}`, assignment);
    return response.data;
  }

  async deleteAssignment(id: string): Promise<void> {
    await api.delete(`/api/pharma/employee-assignments/${id}`);
  }

  // Product Receipt APIs
  async getReceipts(organizationId: string): Promise<ProductReceipt[]> {
    const response = await api.get(`/api/pharma/product-receipts?organizationId=${organizationId}`);
    return response.data;
  }

  async getReceiptsByDateRange(organizationId: string, startDate: string, endDate: string): Promise<ProductReceipt[]> {
    const response = await api.get(`/api/pharma/product-receipts/date-range?organizationId=${organizationId}&startDate=${startDate}&endDate=${endDate}`);
    return response.data;
  }

  async getReceiptById(id: string): Promise<ProductReceipt> {
    const response = await api.get(`/api/pharma/product-receipts/${id}`);
    return response.data;
  }

  async createReceipt(receipt: ProductReceipt): Promise<ProductReceipt> {
    const response = await api.post('/api/pharma/product-receipts', receipt);
    return response.data;
  }

  async updateReceipt(id: string, receipt: ProductReceipt): Promise<ProductReceipt> {
    const response = await api.put(`/api/pharma/product-receipts/${id}`, receipt);
    return response.data;
  }

  async submitReceipt(id: string): Promise<ProductReceipt> {
    const response = await api.post(`/api/pharma/product-receipts/${id}/submit`);
    return response.data;
  }

  async deleteReceipt(id: string): Promise<void> {
    await api.delete(`/api/pharma/product-receipts/${id}`);
  }

  // Product Disbursement APIs
  async getDisbursements(organizationId: string): Promise<ProductDisbursement[]> {
    const response = await api.get(`/api/pharma/product-disbursements?organizationId=${organizationId}`);
    return response.data;
  }

  async getDisbursementsByTerritory(territoryId: string): Promise<ProductDisbursement[]> {
    const response = await api.get(`/api/pharma/product-disbursements/territory/${territoryId}`);
    return response.data;
  }

  async getDisbursementsByTerritoryAndPeriod(territoryId: string, year: number, month: number): Promise<ProductDisbursement[]> {
    const response = await api.get(`/api/pharma/product-disbursements/territory/${territoryId}/period?year=${year}&month=${month}`);
    return response.data;
  }

  async getDisbursementById(id: string): Promise<ProductDisbursement> {
    const response = await api.get(`/api/pharma/product-disbursements/${id}`);
    return response.data;
  }

  async createDisbursement(disbursement: ProductDisbursement): Promise<ProductDisbursement> {
    const response = await api.post('/api/pharma/product-disbursements', disbursement);
    return response.data;
  }

  async updateDisbursement(id: string, disbursement: ProductDisbursement): Promise<ProductDisbursement> {
    const response = await api.put(`/api/pharma/product-disbursements/${id}`, disbursement);
    return response.data;
  }

  async submitDisbursement(id: string): Promise<ProductDisbursement> {
    const response = await api.post(`/api/pharma/product-disbursements/${id}/submit`);
    return response.data;
  }

  async deleteDisbursement(id: string): Promise<void> {
    await api.delete(`/api/pharma/product-disbursements/${id}`);
  }

  // Target APIs
  async getTargets(organizationId: string): Promise<Target[]> {
    const response = await api.get(`/api/pharma/targets?organizationId=${organizationId}`);
    return response.data;
  }

  async getTargetsByTerritory(territoryId: string): Promise<Target[]> {
    const response = await api.get(`/api/pharma/targets/territory/${territoryId}`);
    return response.data;
  }

  async getTargetsByEmployee(employeeId: string): Promise<Target[]> {
    const response = await api.get(`/api/pharma/targets/employee/${employeeId}`);
    return response.data;
  }

  async getActiveTargetForTerritoryAndMonth(territoryId: string, year: number, month: number): Promise<Target | null> {
    try {
      const response = await api.get(`/api/pharma/targets/territory/${territoryId}/month?year=${year}&month=${month}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async getTargetById(id: string): Promise<Target> {
    const response = await api.get(`/api/pharma/targets/${id}`);
    return response.data;
  }

  async createTarget(target: Target): Promise<Target> {
    const response = await api.post('/api/pharma/targets', target);
    return response.data;
  }

  async updateTarget(id: string, target: Target): Promise<Target> {
    const response = await api.put(`/api/pharma/targets/${id}`, target);
    return response.data;
  }

  async deleteTarget(id: string): Promise<void> {
    await api.delete(`/api/pharma/targets/${id}`);
  }

  async calculateCoverage(territoryId: string, year: number, month: number): Promise<TargetCoverage> {
    const response = await api.post(`/api/pharma/targets/coverage/calculate?territoryId=${territoryId}&year=${year}&month=${month}`);
    return response.data;
  }

  async getCoverageByTerritory(territoryId: string, year: number): Promise<TargetCoverage[]> {
    const response = await api.get(`/api/pharma/targets/coverage/territory/${territoryId}?year=${year}`);
    return response.data;
  }

  async getCoverageByTerritoryAndMonth(territoryId: string, year: number, month: number): Promise<TargetCoverage | null> {
    try {
      const response = await api.get(`/api/pharma/targets/coverage/territory/${territoryId}/month?year=${year}&month=${month}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  // Deposit APIs
  async getDeposits(organizationId: string): Promise<Deposit[]> {
    const response = await api.get(`/api/pharma/deposits?organizationId=${organizationId}`);
    return response.data;
  }

  async getDepositsByTerritory(territoryId: string): Promise<Deposit[]> {
    const response = await api.get(`/api/pharma/deposits/territory/${territoryId}`);
    return response.data;
  }

  async getDepositsByTerritoryAndPeriod(territoryId: string, year: number, month: number): Promise<Deposit[]> {
    const response = await api.get(`/api/pharma/deposits/territory/${territoryId}/period?year=${year}&month=${month}`);
    return response.data;
  }

  async getDepositById(id: string): Promise<Deposit> {
    const response = await api.get(`/api/pharma/deposits/${id}`);
    return response.data;
  }

  async getTotalCoveredAmount(territoryId: string, year: number, month: number): Promise<number> {
    const response = await api.get(`/api/pharma/deposits/territory/${territoryId}/covered-amount?year=${year}&month=${month}`);
    return response.data;
  }

  async getOutstandingQuantity(territoryId: string, productId: string): Promise<number> {
    const response = await api.get(`/api/pharma/deposits/territory/${territoryId}/outstanding-quantity?productId=${productId}`);
    return response.data;
  }

  async createDeposit(deposit: Deposit): Promise<Deposit> {
    const response = await api.post('/api/pharma/deposits', deposit);
    return response.data;
  }

  async updateDeposit(id: string, deposit: Deposit): Promise<Deposit> {
    const response = await api.put(`/api/pharma/deposits/${id}`, deposit);
    return response.data;
  }

  async submitDeposit(id: string): Promise<Deposit> {
    const response = await api.post(`/api/pharma/deposits/${id}/submit`);
    return response.data;
  }

  async completeDeposit(id: string): Promise<Deposit> {
    const response = await api.post(`/api/pharma/deposits/${id}/complete`);
    return response.data;
  }

  async deleteDeposit(id: string): Promise<void> {
    await api.delete(`/api/pharma/deposits/${id}`);
  }

  // Sold Product Entry APIs (product-wise sales)
  async getSoldProductEntries(organizationId: string): Promise<SoldProductEntry[]> {
    const response = await api.get(`/api/pharma/sold-product-entries?organizationId=${organizationId}`);
    return response.data;
  }

  async getSoldProductEntriesByTerritory(territoryId: string): Promise<SoldProductEntry[]> {
    const response = await api.get(`/api/pharma/sold-product-entries/territory/${territoryId}`);
    return response.data;
  }

  async getSoldProductEntriesByTerritoryAndPeriod(territoryId: string, year: number, month: number): Promise<SoldProductEntry[]> {
    const response = await api.get(`/api/pharma/sold-product-entries/territory/${territoryId}/period?year=${year}&month=${month}`);
    return response.data;
  }

  async getSoldProductEntryById(id: string): Promise<SoldProductEntry> {
    const response = await api.get(`/api/pharma/sold-product-entries/${id}`);
    return response.data;
  }

  async createSoldProductEntry(entry: SoldProductEntry): Promise<SoldProductEntry> {
    const response = await api.post('/api/pharma/sold-product-entries', entry);
    return response.data;
  }

  async updateSoldProductEntry(id: string, entry: SoldProductEntry): Promise<SoldProductEntry> {
    const response = await api.put(`/api/pharma/sold-product-entries/${id}`, entry);
    return response.data;
  }

  async submitSoldProductEntry(id: string): Promise<SoldProductEntry> {
    const response = await api.post(`/api/pharma/sold-product-entries/${id}/submit`);
    return response.data;
  }

  async completeSoldProductEntry(id: string): Promise<SoldProductEntry> {
    const response = await api.post(`/api/pharma/sold-product-entries/${id}/complete`);
    return response.data;
  }

  async deleteSoldProductEntry(id: string): Promise<void> {
    await api.delete(`/api/pharma/sold-product-entries/${id}`);
  }

  // Phase 3: Expense APIs
  async getExpenseCategories(organizationId: string): Promise<ExpenseCategory[]> {
    const response = await api.get(`/api/pharma/expenses/categories?organizationId=${organizationId}`);
    return response.data;
  }

  async createExpenseCategory(category: ExpenseCategory): Promise<ExpenseCategory> {
    const response = await api.post('/api/pharma/expenses/categories', category);
    return response.data;
  }

  async getExpenses(organizationId: string): Promise<Expense[]> {
    const response = await api.get(`/api/pharma/expenses?organizationId=${organizationId}`);
    return response.data;
  }

  async getExpensesByTerritory(territoryId: string): Promise<Expense[]> {
    const response = await api.get(`/api/pharma/expenses/territory/${territoryId}`);
    return response.data;
  }

  async getExpensesByTerritoryAndPeriod(territoryId: string, year: number, month: number): Promise<Expense[]> {
    const response = await api.get(`/api/pharma/expenses/territory/${territoryId}/period?year=${year}&month=${month}`);
    return response.data;
  }

  async getTotalExpensesForTerritory(territoryId: string, year: number, month: number): Promise<number> {
    const response = await api.get(`/api/pharma/expenses/territory/${territoryId}/total?year=${year}&month=${month}`);
    return response.data;
  }

  async createExpense(expense: Expense): Promise<Expense> {
    const response = await api.post('/api/pharma/expenses', expense);
    return response.data;
  }

  async updateExpense(id: string, expense: Expense): Promise<Expense> {
    const response = await api.put(`/api/pharma/expenses/${id}`, expense);
    return response.data;
  }

  async submitExpense(id: string): Promise<Expense> {
    const response = await api.post(`/api/pharma/expenses/${id}/submit`);
    return response.data;
  }

  async deleteExpense(id: string): Promise<void> {
    await api.delete(`/api/pharma/expenses/${id}`);
  }

  // Phase 3: Adjustment APIs
  async getAdjustments(organizationId: string): Promise<Adjustment[]> {
    const response = await api.get(`/api/pharma/adjustments?organizationId=${organizationId}`);
    return response.data;
  }

  async getAdjustmentsByTerritory(territoryId: string): Promise<Adjustment[]> {
    const response = await api.get(`/api/pharma/adjustments/territory/${territoryId}`);
    return response.data;
  }

  async getAdjustmentsByTerritoryAndPeriod(territoryId: string, year: number, month: number): Promise<Adjustment[]> {
    const response = await api.get(`/api/pharma/adjustments/territory/${territoryId}/period?year=${year}&month=${month}`);
    return response.data;
  }

  async createAdjustment(adjustment: Adjustment): Promise<Adjustment> {
    const response = await api.post('/api/pharma/adjustments', adjustment);
    return response.data;
  }

  async submitAdjustment(id: string): Promise<Adjustment> {
    const response = await api.post(`/api/pharma/adjustments/${id}/submit`);
    return response.data;
  }

  async deleteAdjustment(id: string): Promise<void> {
    await api.delete(`/api/pharma/adjustments/${id}`);
  }

  // Phase 3: Incentive APIs (territory-based)
  async calculateIncentive(territoryId: string, year: number, month: number, forceRecalculate?: boolean): Promise<IncentiveCalculation> {
    const params = new URLSearchParams();
    params.append('territoryId', territoryId);
    params.append('year', year.toString());
    params.append('month', month.toString());
    if (forceRecalculate) params.append('forceRecalculate', 'true');
    const response = await api.post(`/api/pharma/incentives/calculate?${params.toString()}`);
    return response.data;
  }

  async getCalculationsByTerritory(territoryId: string, year: number): Promise<IncentiveCalculation[]> {
    const response = await api.get(`/api/pharma/incentives/territory/${territoryId}?year=${year}`);
    const data = response.data;
    const list = Array.isArray(data) ? data : (data?.content ? data.content : []);
    return (list || []).filter(Boolean);
  }

  async getIncentiveByTerritoryAndMonth(territoryId: string, year: number, month: number): Promise<IncentiveCalculation | null> {
    try {
      const response = await api.get(`/api/pharma/incentives/territory/${territoryId}/month?year=${year}&month=${month}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) return null;
      throw error;
    }
  }

  async markIncentivesAsPaid(calculationId: string): Promise<void> {
    await api.post(`/api/pharma/incentives/${calculationId}/mark-paid`);
  }

  async getDistributionsByEmployee(employeeId: string): Promise<IncentiveDistribution[]> {
    const response = await api.get(`/api/pharma/incentives/employee/${employeeId}`);
    return response.data;
  }

  // Phase 4: Reporting APIs
  async getMonthlyClosingReport(
    organizationId: string,
    options: { areaId?: string; territoryId?: string },
    year: number,
    month: number,
    employeeId?: string
  ): Promise<MonthlyClosingReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('year', year.toString());
    params.append('month', month.toString());
    if (options.areaId) params.append('areaId', options.areaId);
    if (options.territoryId) params.append('territoryId', options.territoryId);
    if (employeeId) params.append('employeeId', employeeId);
    const response = await api.get(`/api/pharma/reports/monthly-closing?${params.toString()}`);
    return response.data;
  }

  async getAreaPerformanceReport(
    organizationId: string,
    areaId: string,
    year: number,
    month: number
  ): Promise<AreaPerformanceReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('areaId', areaId);
    params.append('year', year.toString());
    params.append('month', month.toString());
    const response = await api.get(`/api/pharma/reports/area-performance?${params.toString()}`);
    return response.data;
  }

  async getInStockTotalAmountReport(
    organizationId: string,
    startDate: string,
    endDate: string
  ): Promise<InStockTotalAmountReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    const response = await api.get(`/api/pharma/reports/inventory/in-stock-total?${params.toString()}`);
    return response.data;
  }

  async getInStockProductWiseReport(
    organizationId: string,
    startDate: string,
    endDate: string
  ): Promise<InStockProductWiseReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    const response = await api.get(`/api/pharma/reports/inventory/in-stock-product-wise?${params.toString()}`);
    return response.data;
  }

  async getAreaWiseAllocationReport(
    organizationId: string,
    startDate: string,
    endDate: string,
    areaId?: string
  ): Promise<AreaWiseAllocationReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    if (areaId) params.append('areaId', areaId);
    const response = await api.get(`/api/pharma/reports/inventory/area-wise-allocation?${params.toString()}`);
    return response.data;
  }

  async getAreaWiseCollectionReport(
    organizationId: string,
    startDate: string,
    endDate: string,
    areaId?: string
  ): Promise<AreaWiseCollectionReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    if (areaId) params.append('areaId', areaId);
    const response = await api.get(`/api/pharma/reports/collection/area-wise?${params.toString()}`);
    return response.data;
  }

  async getEmployeeWiseCollectionReport(
    organizationId: string,
    startDate: string,
    endDate: string,
    employeeId?: string
  ): Promise<EmployeeWiseCollectionReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    if (employeeId) params.append('employeeId', employeeId);
    const response = await api.get(`/api/pharma/reports/collection/employee-wise?${params.toString()}`);
    return response.data;
  }

  async getAccountsBalanceReport(
    organizationId: string,
    asOfDate: string
  ): Promise<AccountsBalanceReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('asOfDate', asOfDate);
    const response = await api.get(`/api/pharma/reports/financial/accounts-balance?${params.toString()}`);
    return response.data;
  }

  async getIncomeExpenseReport(
    organizationId: string,
    startDate: string,
    endDate: string
  ): Promise<IncomeExpenseReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    const response = await api.get(`/api/pharma/reports/financial/income-expense?${params.toString()}`);
    return response.data;
  }

  async getIncentiveReport(
    organizationId: string,
    year: number,
    month: number
  ): Promise<IncentiveReport> {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    params.append('year', year.toString());
    params.append('month', month.toString());
    const response = await api.get(`/api/pharma/reports/financial/incentive?${params.toString()}`);
    return response.data;
  }

  // Phase 5: Advanced Features APIs
  // Territory Analytics
  async getTerritoryAnalytics(
    territoryId: string,
    year: number,
    month: number
  ): Promise<TerritoryAnalytics> {
    const response = await api.get(`/api/pharma/territories/territories/${territoryId}/analytics?year=${year}&month=${month}`);
    return response.data;
  }

  async getTerritoryOptimization(territoryId: string): Promise<TerritoryOptimization> {
    const response = await api.get(`/api/pharma/territories/territories/${territoryId}/optimization`);
    return response.data;
  }

  // Incentive Rules Management (territory-based)
  async getIncentiveRuleForTerritory(territoryId: string, date?: string): Promise<TerritoryIncentiveRuleResponse> {
    const params = date ? `?date=${date}` : '';
    const response = await api.get(`/api/pharma/incentive-rules/territory/${territoryId}${params}`);
    return response.data;
  }

  async getIncentiveRuleWithAllocations(territoryId: string): Promise<TerritoryIncentiveRuleResponse | null> {
    try {
      const response = await api.get(`/api/pharma/incentive-rules/territory/${territoryId}`);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404) return null;
      throw err;
    }
  }

  async getAllocationsForTerritory(territoryId: string): Promise<TerritoryIncentiveAllocationItem[]> {
    const response = await api.get(`/api/pharma/incentive-rules/territory/${territoryId}/allocations`);
    return response.data;
  }

  async saveIncentiveRuleWithAllocations(request: TerritoryIncentiveRuleRequest): Promise<TerritoryIncentiveRuleResponse> {
    if (request.id) {
      const response = await api.put(`/api/pharma/incentive-rules/${request.id}`, request);
      return response.data;
    }
    const response = await api.post('/api/pharma/incentive-rules', request);
    return response.data;
  }

  async deactivateIncentiveRule(id: string): Promise<void> {
    await api.delete(`/api/pharma/incentive-rules/${id}`);
  }

  async getIncentiveRuleHistory(territoryId: string): Promise<TerritoryIncentiveRule[]> {
    const response = await api.get(`/api/pharma/incentive-rules/territory/${territoryId}/history`);
    return response.data;
  }
}

// Additional types for Phase 2
export interface ProductDisbursementLine {
  id?: string;
  productDisbursementId?: string;
  productId: string;
  productName?: string;
  packSize?: number;
  tpWithVat?: number;
  mrp?: number;
  previousMonthOpeningQuantity?: number;
  currentMonthQuantity: number;
  totalQuantity?: number;
  productAmount?: number;
}

export interface ProductDisbursement {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId: string;
  disbursementDate: string;
  year?: number;
  month?: number;
  previousMonthOpeningTotalDue?: number;
  totalSupplyAmount?: number;
  totalBalanceAmount?: number;
  status: string;
  notes?: string;
  disbursementLines?: ProductDisbursementLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface Target {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId: string;
  year: number;
  startMonth: number;
  endMonth: number;
  targetAmount: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface TargetCoverage {
  id?: string;
  targetId?: string;
  territoryId: string;
  year: number;
  month: number;
  targetAmount: number;
  coveredAmount: number;
  coveragePercentage?: number;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DepositLine {
  id?: string;
  depositId?: string;
  productId: string;
  productName?: string;
  tpWithVat?: number;
  quantitySold: number;
  currentOutstandingQuantity?: number;
  productAmount?: number;
}

export interface Deposit {
  id?: string;
  organizationId: string;
  territoryId: string;
  employeeId?: string;
  depositDate: string;
  year?: number;
  month?: number;
  depositAmount: number;
  bankAccountId?: string;
  bankName: string;
  bankAccountNumber: string;
  totalProductAmount?: number;
  status: string;
  notes?: string;
  depositLines?: DepositLine[];
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

// Phase 4: Reporting Types
export interface TransactionDetail {
  transactionDate: string;
  transactionType: string;
  quantity: number;
  reason?: string;
}

export interface ProductWiseDetail {
  productId: string;
  productName: string;
  openingBalance: number;
  quantityReceived: number;
  quantitySold: number;
  quantityAdjusted: number;
  closingBalance: number;
  tradePricePerUnit: number;
  totalValue: number;
  transactions: TransactionDetail[];
}

export interface MonthlyClosingReport {
  year: number;
  month: number;
  divisionName?: string;
  regionName?: string;
  territoryName?: string;
  areaName: string;
  employeeName?: string;
  employeeId?: string;
  designation?: string;
  productDetails: ProductWiseDetail[];
  totalProductsSuppliedValue: number;
  totalDepositsReceived: number;
  dueAmount: number;
  targetAmount?: number;
  coveredAmount?: number;
  targetCoveragePercentage?: number;
  targetAchieved?: boolean;
  collectionEfficiency?: number;
}

export interface EmployeePerformance {
  employeeId: string;
  employeeName?: string;
  employeeIdCode?: string;
  role: string;
  incentiveAmount: number;
  targetContribution?: number;
  coverageContribution?: number;
}

export interface AreaPerformanceReport {
  areaId: string;
  areaName: string;
  divisionName?: string;
  regionName?: string;
  territoryName?: string;
  targetAmount?: number;
  coveredAmount?: number;
  targetCoveragePercentage?: number;
  totalExpenses: number;
  expensePercentage?: number;
  expenseWithinLimit?: boolean;
  incentiveEligible?: boolean;
  incentiveBaseAmount?: number;
  employeePerformances: EmployeePerformance[];
}

export interface InStockTotalAmountReport {
  startDate: string;
  endDate: string;
  asOfDate: string;
  location: string;
  totalInStockAmount: number;
}

export interface ProductStockDetail {
  productId: string;
  productName: string;
  packSize?: number;
  tpWithVat?: number;
  quantity: number;
  amount?: number;
}

export interface InStockProductWiseReport {
  startDate: string;
  endDate: string;
  asOfDate: string;
  location: string;
  products: ProductStockDetail[];
  totalInStockAmount: number;
}

export interface ProductAllocationDetail {
  productId: string;
  productName: string;
  totalQuantityAllocated: number;
  totalAmount: number;
}

export interface AreaAllocationDetail {
  areaId: string;
  areaName: string;
  receivingEmployeeName?: string;
  totalAllocations: number;
  productDetails: ProductAllocationDetail[];
  totalAllocationAmount: number;
  allocationDates: string[];
}

export interface AreaWiseAllocationReport {
  startDate: string;
  endDate: string;
  areas: AreaAllocationDetail[];
}

export interface DepositDetail {
  depositId: string;
  depositDate: string;
  depositAmount: number;
  status: string;
  collectedBy?: string;
}

export interface AreaCollectionDetail {
  areaId: string;
  areaName: string;
  numberOfDeposits: number;
  totalCollectionAmount: number;
  targetAmount?: number;
  coveragePercentage?: number;
  deposits: DepositDetail[];
}

export interface AreaWiseCollectionReport {
  startDate: string;
  endDate: string;
  areas: AreaCollectionDetail[];
  grandTotalCollection: number;
}

export interface EmployeeCollectionDetail {
  employeeId: string;
  employeeName?: string;
  employeeIdCode?: string;
  role?: string;
  numberOfDeposits: number;
  totalCollectionAmount: number;
  assignedAreaIds?: string[];
  assignedAreaNames?: string[];
}

export interface EmployeeWiseCollectionReport {
  startDate: string;
  endDate: string;
  employees: EmployeeCollectionDetail[];
  grandTotalCollection: number;
}

export interface AreaBalance {
  areaId: string;
  areaName: string;
  totalProductsSupplied: number;
  totalDepositsReceived: number;
  dueAmount: number;
  overdueAmount: number;
}

export interface AccountsBalanceReport {
  asOfDate: string;
  areaBalances: AreaBalance[];
  totalDueAmount: number;
}

export interface AreaIncomeExpense {
  areaId: string;
  areaName: string;
  income: number;
  expenses: number;
  netIncome: number;
}

export interface IncomeExpenseReport {
  startDate: string;
  endDate: string;
  totalIncome: number;
  totalExpenses: number;
  netIncome: number;
  areaDetails: AreaIncomeExpense[];
}

export interface EmployeeIncentiveDetail {
  employeeId: string;
  employeeName?: string;
  employeeIdCode?: string;
  role: string;
  incentiveAmount: number;
}

export interface AreaIncentiveDetail {
  areaId: string;
  areaName: string;
  targetAmount: number;
  coveredAmount: number;
  targetCoveragePercentage?: number;
  targetAchieved: boolean;
  expenseWithinLimit: boolean;
  eligible: boolean;
  incentiveBaseAmount: number;
  employeeIncentives: EmployeeIncentiveDetail[];
}

export interface IncentiveReport {
  year: number;
  month: number;
  areaIncentives: AreaIncentiveDetail[];
  totalIncentiveAmount: number;
  eligibleAreas: number;
  totalAreas: number;
}

export interface MonthlyAllocationDetail {
  year: number;
  month: number;
  totalQuantity: number;
  totalAmount: number;
  numberOfAllocations: number;
  numberOfAreas: number;
  topProducts?: string[];
}

export interface MonthWiseAllocationReport {
  year: number;
  monthlyDetails: MonthlyAllocationDetail[];
  grandTotalQuantity: number;
  grandTotalAmount: number;
  grandTotalAllocations: number;
}

export interface AnnualAllocationReport {
  year: number;
  totalAllocations: number;
  totalQuantity: number;
  totalAmount: number;
  averageMonthlyAllocation: number;
  peakMonth?: string;
  monthlyBreakdown: MonthlyAllocationDetail[];
}

// Phase 5: Advanced Features Types
export interface TerritoryAnalytics {
  territoryId: string;
  territoryName: string;
  year: number;
  month: number;
  summary: {
    totalEmployees: number;
    totalTarget: number;
    totalCovered: number;
    totalExpenses: number;
    totalIncentives: number;
    totalDeposits: number;
    targetAchievementRate: number;
    expenseRatio: number;
    incentiveRate: number;
    territoryEfficiencyScore: number;
    targetAchieved?: boolean;
    incentiveEligible?: boolean;
  };
  territoryMetrics?: Record<string, unknown>;
  trend?: {
    coverageChangePercent?: number;
    trendDirection?: string;
  };
}

export interface TerritoryOptimization {
  territoryId: string;
  territoryName: string;
  workloadAnalysis: Array<{
    territoryId: string;
    territoryName: string;
    employeeCount: number;
    targetAmount: number;
    workloadPerEmployee: number;
  }>;
  performanceGaps: Array<{
    territoryId: string;
    territoryName: string;
    achievementRate: number;
    recommendation: string;
  }>;
  resourceAllocation?: Array<{
    territoryId: string;
    territoryName: string;
    currentWorkload: number;
    averageWorkload: number;
    recommendation: string;
  }>;
}

export interface TerritoryIncentiveRule {
  id?: string;
  organizationId: string;
  territoryId: string;
  incentivePercentage: number; // e.g., 0.04 for 4%
  srSharePercentage: number; // e.g., 0.10 for 10%
  mpoSharePercentage: number; // e.g., 0.72 for 72% of total
  managerSharePercentage: number; // e.g., 0.18 for 18% of total
  expenseLimitPercentage: number; // e.g., 0.30 for 30%
  ruleVersion?: number;
  effectiveFromDate?: string;
  effectiveToDate?: string;
  isActive?: boolean;
  description?: string;
  notes?: string;
}

/** Request for creating/updating territory incentive rule with allocations */
export interface TerritoryIncentiveRuleRequest {
  id?: string;
  organizationId: string;
  territoryId: string;
  incentivePercentage?: number;
  srSharePercentage?: number;
  developmentFundPercentage?: number;
  hasDedicatedSr?: boolean;
  dualRoleEmployeeId?: string | null;
  expenseLimitPercentage?: number;
  effectiveFromDate?: string;
  effectiveToDate?: string;
  isActive?: boolean;
  description?: string;
  notes?: string;
  allocations?: TerritoryIncentiveAllocationRequestItem[];
}

export interface TerritoryIncentiveAllocationRequestItem {
  employeeId: string;
  roleInTerritory?: string;
  allocationPercentage: number;
}

/** Response with rule + allocations */
export interface TerritoryIncentiveRuleResponse {
  id: string;
  organizationId: string;
  territoryId: string;
  incentivePercentage: number;
  srSharePercentage: number;
  developmentFundPercentage: number;
  hasDedicatedSr: boolean;
  dualRoleEmployeeId?: string | null;
  expenseLimitPercentage: number;
  ruleVersion?: number;
  effectiveFromDate?: string;
  effectiveToDate?: string;
  isActive: boolean;
  description?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  allocations: TerritoryIncentiveAllocationItem[];
  validationStatus?: string;
}

export interface TerritoryIncentiveAllocationItem {
  id?: string;
  employeeId: string;
  roleInTerritory?: string;
  allocationPercentage: number;
}

export default new PharmaService();

