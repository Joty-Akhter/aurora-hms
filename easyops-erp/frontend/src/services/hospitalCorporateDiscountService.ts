import api from './api';

const BASE = '/api/hospital-corporate-discount';

// ========== Types ==========

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CorporateResponse {
  id: string;
  name: string;
  code: string;
  type: string;
  status: string;
  validFrom?: string | null;
  validTo?: string | null;
  primaryContactName?: string | null;
  primaryContactPhone?: string | null;
  primaryContactEmail?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

/** Alias for corporate client (same as CorporateResponse). */
export type CorporateClient = CorporateResponse;

export interface CreateCorporateRequest {
  name: string;
  code: string;
  type: string;
  status?: string;
  validFrom?: string | null;
  validTo?: string | null;
  primaryContactName?: string | null;
  primaryContactPhone?: string | null;
  primaryContactEmail?: string | null;
}

export interface UpdateCorporateRequest {
  name?: string;
  code?: string;
  type?: string;
  status?: string;
  validFrom?: string | null;
  validTo?: string | null;
  primaryContactName?: string | null;
  primaryContactPhone?: string | null;
  primaryContactEmail?: string | null;
}

export interface ContractResponse {
  id: string;
  corporateClientId: string;
  contractCode: string;
  contractName?: string | null;
  validFrom: string;
  validTo?: string | null;
  coverageType: string;
  serviceLocations?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

/** Alias for corporate contract (same as ContractResponse). */
export type CorporateContract = ContractResponse;

export interface CreateContractRequest {
  contractCode: string;
  contractName?: string | null;
  validFrom: string;
  validTo?: string | null;
  coverageType: string;
  serviceLocations?: string | null;
}

export interface UpdateContractRequest {
  contractCode?: string;
  contractName?: string | null;
  validFrom?: string;
  validTo?: string | null;
  coverageType?: string;
  serviceLocations?: string | null;
}

// ---------- Coverage rules (Phase 2) ----------
export interface CoverageRuleResponse {
  id: string;
  corporateContractId: string;
  scopeType: string;
  scopeValue: string;
  coveragePercent: number;
  maxAmount?: number | null;
  coPayPercent?: number | null;
  deductibleAmount?: number | null;
  applicableVisitTypes?: string | null;
  createdAt?: string;
}

export interface CreateCoverageRuleRequest {
  scopeType: string;
  scopeValue: string;
  coveragePercent: number;
  maxAmount?: number | null;
  coPayPercent?: number | null;
  deductibleAmount?: number | null;
  applicableVisitTypes?: string | null;
}

// ---------- Packages (Phase 2) ----------
export interface PackageResponse {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  defaultPrice?: number | null;
  isCorporateOnly?: boolean;
  isPublic?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PackageDetailResponse extends PackageResponse {
  items: PackageItemResponse[];
}

export interface CreatePackageRequest {
  code: string;
  name: string;
  description?: string | null;
  defaultPrice?: number | null;
  isCorporateOnly?: boolean;
  isPublic?: boolean;
}

export interface UpdatePackageRequest {
  code?: string;
  name?: string;
  description?: string | null;
  defaultPrice?: number | null;
  isCorporateOnly?: boolean;
  isPublic?: boolean;
}

export interface PackageItemResponse {
  id: string;
  packageId: string;
  itemType: string;
  itemCode: string;
  quantityIncluded: number;
  createdAt?: string;
}

export interface CreatePackageItemRequest {
  itemType: string;
  itemCode: string;
  quantityIncluded?: number | null;
}

// ---------- Tariffs (Phase 2) ----------
export interface CorporateTariffResponse {
  id: string;
  corporateContractId: string;
  scopeType: string;
  scopeValue: string;
  tariffType: string;
  tariffAmount?: number | null;
  tariffPercent?: number | null;
  createdAt?: string;
}

export interface CreateCorporateTariffRequest {
  scopeType: string;
  scopeValue: string;
  tariffType: string;
  tariffAmount?: number | null;
  tariffPercent?: number | null;
}

// ---------- Corporate benefit cards (Plan Phase 3) ----------
export interface CorporateCardResponse {
  id: string;
  corporateClientId: string;
  contractId?: string | null;
  holderName: string;
  holderIdentifier: string;
  cardType: string;
  cardProductId: string;
  cardId: string;
  cardNumber: string;
  status: string;
  action?: string | null;
  title?: string | null;
  html?: string | null;
  replacedByCorporateCardId?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateCorporateCardRequest {
  corporateClientId: string;
  contractId?: string | null;
  holderName: string;
  holderIdentifier: string;
  cardType: string;
  cardProductId: string;
  cardNumber?: string | null;
}

export interface ReissueCorporateCardRequest {
  reason: string;
}

export interface BlockCorporateCardRequest {
  reason: string;
}

export interface CorporateCardValidationResponse {
  valid: boolean;
  message: string;
  corporateCardId?: string | null;
  cardId?: string | null;
  cardNumber?: string | null;
  corporateStatus?: string | null;
  registryStatus?: string | null;
  corporateClientId?: string | null;
  contractId?: string | null;
  holderIdentifier?: string | null;
  cardType?: string | null;
}

// ---------- Coverage evaluation (Phase 2) ----------
export interface EvaluateCoverageItemRequest {
  serviceCode: string;
  serviceGroupId?: string | null;
  departmentId?: string | null;
  quantity: number;
  basePrice: number;
}

export interface EvaluateCoverageRequest {
  patientId?: string | null;
  visitId?: string | null;
  corporateContractId: string;
  visitType?: string | null;
  items: EvaluateCoverageItemRequest[];
}

export interface EvaluateCoverageItemResponse {
  lineIndex: number;
  serviceCode: string;
  coveredPercent: number;
  coveredAmount: number;
  patientShare: number;
  corporateShare: number;
  maxApplicable?: number | null;
  ruleId?: string | null;
}

export interface EvaluateCoverageResponse {
  items: EvaluateCoverageItemResponse[];
  totalCovered: number;
  totalPatientShare: number;
  totalCorporateShare: number;
}

// ---------- Discount schemes (Phase 3) ----------
export interface DiscountSchemeResponse {
  id: string;
  code: string;
  name: string;
  corporateClientId?: string | null;
  visitType?: string | null;
  departmentId?: string | null;
  serviceCode?: string | null;
  patientCategory?: string | null;
  discountType: string;
  discountValue: number;
  maxDiscountAmount?: number | null;
  maxDiscountPercent?: number | null;
  requiresApproval?: boolean | null;
  status?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface DiscountSchemeDetailResponse extends DiscountSchemeResponse {
  approvalLevels: DiscountApprovalLevelResponse[];
}

export interface CreateDiscountSchemeRequest {
  code: string;
  name: string;
  corporateClientId?: string | null;
  visitType?: string | null;
  departmentId?: string | null;
  serviceCode?: string | null;
  patientCategory?: string | null;
  discountType: string;
  discountValue: number;
  maxDiscountAmount?: number | null;
  maxDiscountPercent?: number | null;
  requiresApproval?: boolean | null;
  status?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
}

export interface UpdateDiscountSchemeRequest {
  name?: string | null;
  corporateClientId?: string | null;
  visitType?: string | null;
  departmentId?: string | null;
  serviceCode?: string | null;
  patientCategory?: string | null;
  discountType?: string | null;
  discountValue?: number | null;
  maxDiscountAmount?: number | null;
  maxDiscountPercent?: number | null;
  requiresApproval?: boolean | null;
  status?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
}

export interface DiscountApprovalLevelResponse {
  id: string;
  discountSchemeId: string;
  roleOrGroupId: string;
  maxDiscountPercent?: number | null;
  maxDiscountAmount?: number | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateApprovalLevelRequest {
  roleOrGroupId: string;
  maxDiscountPercent?: number | null;
  maxDiscountAmount?: number | null;
  sortOrder?: number | null;
}

// ---------- Discount evaluation (Phase 3) ----------
export interface EvaluateDiscountsItemRequest {
  serviceCode: string;
  quantity: number;
  unitPrice: number;
  departmentId?: string | null;
}

export interface EvaluateDiscountsRequest {
  patientId?: string | null;
  visitId?: string | null;
  corporateClientId?: string | null;
  visitType?: string | null;
  departmentId?: string | null;
  items: EvaluateDiscountsItemRequest[];
  requestedSchemeId?: string | null;
  requestedDiscountPercent?: number | null;
  requestedDiscountAmount?: number | null;
  reason?: string | null;
}

export interface ApplicableSchemeDto {
  schemeId: string;
  schemeCode: string;
  recommendedPercent?: number | null;
  recommendedAmount?: number | null;
  cappedAmount?: number | null;
  requiresApproval?: boolean | null;
  requiredApprovalLevel?: string | null;
}

export interface EvaluateDiscountsResponse {
  applicableSchemes: ApplicableSchemeDto[];
  recommendedTotalDiscount: number;
  requiresApproval?: boolean | null;
  message?: string | null;
}

// ---------- Discount decisions (Phase 3) ----------
export interface CreateDiscountDecisionRequest {
  billContextId?: string | null;
  patientId?: string | null;
  corporateClientId?: string | null;
  discountSchemeId?: string | null;
  discountAmount: number;
  discountPercent?: number | null;
  decidedByUserId?: string | null;
  approvedByUserId?: string | null;
}

export interface DiscountDecisionResponse {
  id: string;
  billContextId?: string | null;
  patientId?: string | null;
  corporateClientId?: string | null;
  discountSchemeId?: string | null;
  discountAmount: number;
  discountPercent?: number | null;
  decidedByUserId?: string | null;
  approvedByUserId?: string | null;
  createdAt?: string | null;
  approvedAt?: string | null;
}

// ========== Corporates ==========

export async function createCorporate(body: CreateCorporateRequest): Promise<CorporateResponse> {
  const { data } = await api.post<CorporateResponse>(`${BASE}/corporates`, body);
  return data;
}

export async function getCorporate(id: string): Promise<CorporateResponse> {
  const { data } = await api.get<CorporateResponse>(`${BASE}/corporates/${id}`);
  return data;
}

export async function getCorporates(params?: {
  code?: string;
  type?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<CorporateResponse>> {
  const { data } = await api.get<PagedResponse<CorporateResponse>>(`${BASE}/corporates`, { params });
  return data;
}

export async function updateCorporate(id: string, body: UpdateCorporateRequest): Promise<CorporateResponse> {
  const { data } = await api.patch<CorporateResponse>(`${BASE}/corporates/${id}`, body);
  return data;
}

// ========== Contracts ==========

export async function createContract(corporateId: string, body: CreateContractRequest): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(`${BASE}/corporates/${corporateId}/contracts`, body);
  return data;
}

export async function getContract(id: string): Promise<ContractResponse> {
  const { data } = await api.get<ContractResponse>(`${BASE}/contracts/${id}`);
  return data;
}

export async function getContractsByCorporate(
  corporateId: string,
  params?: { status?: string; page?: number; size?: number }
): Promise<PagedResponse<ContractResponse>> {
  const { data } = await api.get<PagedResponse<ContractResponse>>(
    `${BASE}/corporates/${corporateId}/contracts`,
    { params }
  );
  return data;
}

export async function updateContract(id: string, body: UpdateContractRequest): Promise<ContractResponse> {
  const { data } = await api.patch<ContractResponse>(`${BASE}/contracts/${id}`, body);
  return data;
}

// ========== Coverage rules (Phase 2) ==========

export async function createCoverageRule(
  contractId: string,
  body: CreateCoverageRuleRequest
): Promise<CoverageRuleResponse> {
  const { data } = await api.post<CoverageRuleResponse>(`${BASE}/contracts/${contractId}/coverage-rules`, body);
  return data;
}

export async function getCoverageRules(contractId: string): Promise<CoverageRuleResponse[]> {
  const { data } = await api.get<CoverageRuleResponse[]>(`${BASE}/contracts/${contractId}/coverage-rules`);
  return data;
}

export async function deleteCoverageRule(id: string): Promise<void> {
  await api.delete(`${BASE}/coverage-rules/${id}`);
}

// ========== Packages (Phase 2) ==========

export async function createPackage(body: CreatePackageRequest): Promise<PackageResponse> {
  const { data } = await api.post<PackageResponse>(`${BASE}/packages`, body);
  return data;
}

export async function getPackage(id: string): Promise<PackageDetailResponse> {
  const { data } = await api.get<PackageDetailResponse>(`${BASE}/packages/${id}`);
  return data;
}

export async function getPackages(params?: {
  code?: string;
  isPublic?: boolean;
  page?: number;
  size?: number;
}): Promise<PagedResponse<PackageResponse>> {
  const { data } = await api.get<PagedResponse<PackageResponse>>(`${BASE}/packages`, { params });
  return data;
}

export async function updatePackage(id: string, body: UpdatePackageRequest): Promise<PackageResponse> {
  const { data } = await api.patch<PackageResponse>(`${BASE}/packages/${id}`, body);
  return data;
}

export async function addPackageItem(
  packageId: string,
  body: CreatePackageItemRequest
): Promise<PackageItemResponse> {
  const { data } = await api.post<PackageItemResponse>(`${BASE}/packages/${packageId}/items`, body);
  return data;
}

export async function getPackageItems(packageId: string): Promise<PackageItemResponse[]> {
  const { data } = await api.get<PackageItemResponse[]>(`${BASE}/packages/${packageId}/items`);
  return data;
}

export async function deletePackageItem(packageId: string, itemId: string): Promise<void> {
  await api.delete(`${BASE}/packages/${packageId}/items/${itemId}`);
}

// ========== Tariffs (Phase 2) ==========

export async function createTariff(
  contractId: string,
  body: CreateCorporateTariffRequest
): Promise<CorporateTariffResponse> {
  const { data } = await api.post<CorporateTariffResponse>(`${BASE}/contracts/${contractId}/tariffs`, body);
  return data;
}

export async function getTariffs(contractId: string): Promise<CorporateTariffResponse[]> {
  const { data } = await api.get<CorporateTariffResponse[]>(`${BASE}/contracts/${contractId}/tariffs`);
  return data;
}

export async function deleteTariff(id: string): Promise<void> {
  await api.delete(`${BASE}/tariffs/${id}`);
}

// ========== Corporate benefit cards (Phase 3) ==========
export async function issueCorporateCard(body: CreateCorporateCardRequest): Promise<CorporateCardResponse> {
  const { data } = await api.post<CorporateCardResponse>(`${BASE}/corporate-cards`, body);
  return data;
}

export async function reissueCorporateCard(
  corporateCardId: string,
  body: ReissueCorporateCardRequest
): Promise<CorporateCardResponse> {
  const { data } = await api.post<CorporateCardResponse>(`${BASE}/corporate-cards/${corporateCardId}/reissue`, body);
  return data;
}

export async function reprintCorporateCard(corporateCardId: string): Promise<CorporateCardResponse> {
  const { data } = await api.post<CorporateCardResponse>(`${BASE}/corporate-cards/${corporateCardId}/reprint`);
  return data;
}

export async function blockCorporateCard(
  corporateCardId: string,
  body: BlockCorporateCardRequest
): Promise<CorporateCardResponse> {
  const { data } = await api.post<CorporateCardResponse>(`${BASE}/corporate-cards/${corporateCardId}/block`, body);
  return data;
}

export async function getCorporateCard(corporateCardId: string): Promise<CorporateCardResponse> {
  const { data } = await api.get<CorporateCardResponse>(`${BASE}/corporate-cards/${corporateCardId}`);
  return data;
}

export async function getCorporateCards(params?: {
  corporateClientId?: string;
  holderIdentifier?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<CorporateCardResponse>> {
  const { data } = await api.get<PagedResponse<CorporateCardResponse>>(`${BASE}/corporate-cards`, { params });
  return data;
}

export async function validateCorporateCard(cardNumber: string): Promise<CorporateCardValidationResponse> {
  const { data } = await api.get<CorporateCardValidationResponse>(`${BASE}/corporate-cards/validate`, {
    params: { cardNumber },
  });
  return data;
}

// ========== Coverage evaluation (Phase 2) ==========

export async function evaluateCoverage(body: EvaluateCoverageRequest): Promise<EvaluateCoverageResponse> {
  const { data } = await api.post<EvaluateCoverageResponse>(`${BASE}/coverage/evaluate`, body);
  return data;
}

// ========== Discount schemes (Phase 3) ==========

export async function createDiscountScheme(body: CreateDiscountSchemeRequest): Promise<DiscountSchemeResponse> {
  const { data } = await api.post<DiscountSchemeResponse>(`${BASE}/discount-schemes`, body);
  return data;
}

export async function getDiscountScheme(id: string): Promise<DiscountSchemeDetailResponse> {
  const { data } = await api.get<DiscountSchemeDetailResponse>(`${BASE}/discount-schemes/${id}`);
  return data;
}

export async function getDiscountSchemes(params?: {
  code?: string;
  corporateClientId?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<DiscountSchemeResponse>> {
  const { data } = await api.get<PagedResponse<DiscountSchemeResponse>>(`${BASE}/discount-schemes`, { params });
  return data;
}

export async function updateDiscountScheme(
  id: string,
  body: UpdateDiscountSchemeRequest
): Promise<DiscountSchemeResponse> {
  const { data } = await api.patch<DiscountSchemeResponse>(`${BASE}/discount-schemes/${id}`, body);
  return data;
}

export async function addApprovalLevel(
  schemeId: string,
  body: CreateApprovalLevelRequest
): Promise<DiscountApprovalLevelResponse> {
  const { data } = await api.post<DiscountApprovalLevelResponse>(
    `${BASE}/discount-schemes/${schemeId}/approval-levels`,
    body
  );
  return data;
}

export async function getApprovalLevels(schemeId: string): Promise<DiscountApprovalLevelResponse[]> {
  const { data } = await api.get<DiscountApprovalLevelResponse[]>(
    `${BASE}/discount-schemes/${schemeId}/approval-levels`
  );
  return data;
}

export async function deleteApprovalLevel(schemeId: string, levelId: string): Promise<void> {
  await api.delete(`${BASE}/discount-schemes/${schemeId}/approval-levels/${levelId}`);
}

export async function evaluateDiscounts(body: EvaluateDiscountsRequest): Promise<EvaluateDiscountsResponse> {
  const { data } = await api.post<EvaluateDiscountsResponse>(`${BASE}/discounts/evaluate`, body);
  return data;
}

export async function createDiscountDecision(
  body: CreateDiscountDecisionRequest
): Promise<DiscountDecisionResponse> {
  const { data } = await api.post<DiscountDecisionResponse>(`${BASE}/discount-decisions`, body);
  return data;
}

export async function getDiscountDecisions(params?: {
  page?: number;
  size?: number;
}): Promise<PagedResponse<DiscountDecisionResponse>> {
  const { data } = await api.get<PagedResponse<DiscountDecisionResponse>>(`${BASE}/discount-decisions`, { params });
  return data;
}

export async function getDiscountDecision(id: string): Promise<DiscountDecisionResponse> {
  const { data } = await api.get<DiscountDecisionResponse>(`${BASE}/discount-decisions/${id}`);
  return data;
}

// ========== Reports ==========

export interface CorporateUtilizationItem {
  corporateId: string;
  decisionCount: number;
}

export interface CorporateUtilizationResponse {
  from: string;
  to: string;
  single?: CorporateUtilizationItem | null;
  byCorporate?: CorporateUtilizationItem[] | null;
}

export interface DiscountSummaryItem {
  schemeId: string;
  schemeCode: string;
  totalAmount: number;
  decisionCount: number;
}

export interface DiscountSummaryResponse {
  from: string;
  to: string;
  single?: DiscountSummaryItem | null;
  byScheme?: DiscountSummaryItem[] | null;
}

export async function getCorporateUtilization(params: {
  from: string;
  to: string;
  corporateId?: string | null;
}): Promise<CorporateUtilizationResponse> {
  const { data } = await api.get<CorporateUtilizationResponse>(`${BASE}/reports/corporate-utilization`, {
    params: { from: params.from, to: params.to, corporateId: params.corporateId || undefined },
  });
  return data;
}

export async function getDiscountSummary(params: {
  from: string;
  to: string;
  schemeId?: string | null;
}): Promise<DiscountSummaryResponse> {
  const { data } = await api.get<DiscountSummaryResponse>(`${BASE}/reports/discount-summary`, {
    params: { from: params.from, to: params.to, schemeId: params.schemeId || undefined },
  });
  return data;
}

const hospitalCorporateDiscountService = {
  createCorporate,
  getCorporate,
  getCorporates,
  updateCorporate,
  createContract,
  getContract,
  getContractsByCorporate,
  updateContract,
  createCoverageRule,
  getCoverageRules,
  deleteCoverageRule,
  createPackage,
  getPackage,
  getPackages,
  updatePackage,
  addPackageItem,
  getPackageItems,
  deletePackageItem,
  createTariff,
  getTariffs,
  deleteTariff,
  issueCorporateCard,
  reissueCorporateCard,
  reprintCorporateCard,
  blockCorporateCard,
  getCorporateCard,
  getCorporateCards,
  validateCorporateCard,
  evaluateCoverage,
  createDiscountScheme,
  getDiscountScheme,
  getDiscountSchemes,
  updateDiscountScheme,
  addApprovalLevel,
  getApprovalLevels,
  deleteApprovalLevel,
  evaluateDiscounts,
  createDiscountDecision,
  getDiscountDecisions,
  getDiscountDecision,
  getCorporateUtilization,
  getDiscountSummary,
};

export default hospitalCorporateDiscountService;
