import api from './api';

// ========== Type Definitions ==========

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Card products
export interface CardProductResponse {
  id: string;
  code: string;
  name: string;
  description?: string;
  mediumType?: string;
  usageDomains?: string;
  defaultLimitProfileId?: string;
  validityStartDate?: string;
  validityEndDate?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface CreateCardProductRequest {
  code: string;
  name: string;
  description?: string;
  mediumType: string;
  usageDomains?: string;
  defaultLimitProfileId?: string;
  validityStartDate?: string;
  validityEndDate?: string;
  status?: string;
}

export interface UpdateCardProductRequest {
  name?: string;
  description?: string;
  mediumType?: string;
  usageDomains?: string;
  defaultLimitProfileId?: string;
  validityStartDate?: string;
  validityEndDate?: string;
  status?: string;
}

// Limit profiles
export interface LimitProfileResponse {
  id: string;
  name: string;
  description?: string;
  dailyAmountLimit?: number;
  monthlyAmountLimit?: number;
  dailyMealLimit?: number;
  dailyVisitLimit?: number;
  resetPolicy: string;
  currency?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateLimitProfileRequest {
  name: string;
  description?: string;
  dailyAmountLimit?: number;
  monthlyAmountLimit?: number;
  dailyMealLimit?: number;
  dailyVisitLimit?: number;
  resetPolicy: string;
  currency?: string;
}

export interface CardWithLimitUsageResponse {
  cardId: string;
  cardNumber: string;
  ownerType: string;
  ownerReferenceId: string;
  status: string;
  limitUsage?: LimitUsageSummary;
}

// Cards
export interface CardResponse {
  id: string;
  cardNumber: string;
  physicalSerial?: string;
  cardProductId: string;
  limitProfileId?: string;
  ownerType: string;
  ownerReferenceId: string;
  corporateId?: string;
  status: string;
  replacedByCardId?: string;
  issuedAt?: string;
  activatedAt?: string;
  blockedAt?: string;
  closedAt?: string;
  createdAt?: string;
  createdBy?: string;
  statusChangeReason?: string;
}

export interface AccountSummary {
  accountId: string;
  accountType: string;
  currentBalance: number;
  currency: string;
  creditLimit?: number;
}

export interface CardDetailResponse extends CardResponse {
  accountSummary?: AccountSummary;
}

export interface IssueCardRequest {
  cardProductId: string;
  ownerType: string;
  ownerReferenceId: string;
  corporateId?: string;
  limitProfileId?: string;
  cardNumber?: string;
  physicalSerial?: string;
}

export interface UpdateCardStatusRequest {
  status: string;
  reason?: string;
}

export interface ReplaceCardRequest {
  reason?: string;
}

// Balance and transactions
export interface LimitUsageSummary {
  periodStart?: string;
  periodEnd?: string;
  resetPolicy?: string;
  amountConsumed?: number;
  mealCountConsumed?: number;
  visitCountConsumed?: number;
  dailyAmountLimit?: number;
  monthlyAmountLimit?: number;
  dailyMealLimit?: number;
  dailyVisitLimit?: number;
}

export interface CardBalanceResponse {
  cardId: string;
  accountId: string;
  accountType: string;
  currentBalance: number;
  currency: string;
  creditLimit?: number;
  limitProfileId?: string;
  limitUsage?: LimitUsageSummary;
}

export interface TopupRequest {
  amount: number;
  currency?: string;
  reference?: string;
  idempotencyKey?: string;
}

export interface CreateAdjustmentRequest {
  amount: number;
  reason: string;
  idempotencyKey?: string;
}

export interface CardTransactionResponse {
  id: string;
  cardAccountId: string;
  transactionType: string;
  sourceSystem?: string;
  externalReferenceId?: string;
  authorizationId?: string;
  amount: number;
  currency: string;
  mealCountDelta?: number;
  status: string;
  createdAt?: string;
  postedAt?: string;
  createdBy?: string;
}

// Authorization & capture (Phase 2)
export interface AuthorizationRequest {
  cardNumber?: string;
  cardToken?: string;
  amount: number;
  currency?: string;
  usageDomain?: string;
  sourceSystem: string;
  externalReferenceId?: string;
  mealCount?: number;
  idempotencyKey?: string;
}

export interface AuthorizationResponse {
  approved: boolean;
  authorizationId?: string;
  reasonCode?: string;
  remainingBalance?: number;
  remainingLimits?: unknown;
}

export interface CaptureRequest {
  amount: number;
  idempotencyKey?: string;
}

export interface RefundRequest {
  amount: number;
  reason?: string;
  idempotencyKey?: string;
}

// Aliases for plan compatibility
export type CardProduct = CardProductResponse;
export type LimitProfile = LimitProfileResponse;
export type Card = CardResponse;

// ========== Service ==========

const BASE = '/api/hospital-card-management';

const hospitalCardManagementService = {
  // Card products
  async createCardProduct(payload: CreateCardProductRequest): Promise<CardProductResponse> {
    const { data } = await api.post<CardProductResponse>(`${BASE}/card-products`, payload);
    return data;
  },

  async getCardProduct(id: string): Promise<CardProductResponse> {
    const { data } = await api.get<CardProductResponse>(`${BASE}/card-products/${id}`);
    return data;
  },

  async getCardProducts(params: {
    code?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<CardProductResponse>> {
    const { data } = await api.get<PagedResponse<CardProductResponse>>(`${BASE}/card-products`, {
      params,
    });
    return data;
  },

  async updateCardProduct(id: string, payload: UpdateCardProductRequest): Promise<CardProductResponse> {
    const { data } = await api.patch<CardProductResponse>(`${BASE}/card-products/${id}`, payload);
    return data;
  },

  // Limit profiles
  async createLimitProfile(payload: CreateLimitProfileRequest): Promise<LimitProfileResponse> {
    const { data } = await api.post<LimitProfileResponse>(`${BASE}/limit-profiles`, payload);
    return data;
  },

  async getLimitProfile(id: string): Promise<LimitProfileResponse> {
    const { data } = await api.get<LimitProfileResponse>(`${BASE}/limit-profiles/${id}`);
    return data;
  },

  async getLimitProfiles(params: {
    name?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<LimitProfileResponse>> {
    const { data } = await api.get<PagedResponse<LimitProfileResponse>>(`${BASE}/limit-profiles`, {
      params,
    });
    return data;
  },

  async getLimitProfileCardsWithUsage(profileId: string): Promise<CardWithLimitUsageResponse[]> {
    const { data } = await api.get<CardWithLimitUsageResponse[]>(
      `${BASE}/limit-profiles/${profileId}/cards-with-usage`
    );
    return data;
  },

  // Cards
  async issueCard(payload: IssueCardRequest): Promise<CardResponse> {
    const { data } = await api.post<CardResponse>(`${BASE}/cards`, payload);
    return data;
  },

  async getCard(id: string): Promise<CardDetailResponse> {
    const { data } = await api.get<CardDetailResponse>(`${BASE}/cards/${id}`);
    return data;
  },

  async searchCards(params: {
    cardNumber?: string;
    ownerReferenceId?: string;
    ownerType?: string;
    corporateId?: string;
    cardProductId?: string;
    status?: string;
    issuedAtFrom?: string;
    issuedAtTo?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<CardResponse>> {
    const { data } = await api.get<PagedResponse<CardResponse>>(`${BASE}/cards/search`, {
      params,
    });
    return data;
  },

  async updateCardStatus(cardId: string, payload: UpdateCardStatusRequest): Promise<CardResponse> {
    const { data } = await api.patch<CardResponse>(`${BASE}/cards/${cardId}/status`, payload);
    return data;
  },

  async replaceCard(cardId: string, payload?: ReplaceCardRequest): Promise<CardResponse> {
    const { data } = await api.post<CardResponse>(`${BASE}/cards/${cardId}/replace`, payload ?? {});
    return data;
  },

  // Balance and transactions
  async getCardBalance(cardId: string): Promise<CardBalanceResponse> {
    const { data } = await api.get<CardBalanceResponse>(`${BASE}/cards/${cardId}/balance`);
    return data;
  },

  async topupCard(cardId: string, payload: TopupRequest): Promise<CardTransactionResponse> {
    const { data } = await api.post<CardTransactionResponse>(`${BASE}/cards/${cardId}/topup`, payload);
    return data;
  },

  async createAdjustment(
    cardId: string,
    payload: CreateAdjustmentRequest
  ): Promise<CardTransactionResponse> {
    const { data } = await api.post<CardTransactionResponse>(
      `${BASE}/cards/${cardId}/adjustments`,
      payload
    );
    return data;
  },

  async getCardTransactions(
    cardId: string,
    params: {
      from?: string;
      to?: string;
      type?: string;
      status?: string;
      page?: number;
      size?: number;
    }
  ): Promise<PagedResponse<CardTransactionResponse>> {
    const { data } = await api.get<PagedResponse<CardTransactionResponse>>(
      `${BASE}/cards/${cardId}/transactions`,
      { params }
    );
    return data;
  },

  // Authorization & capture
  async authorize(body: AuthorizationRequest): Promise<AuthorizationResponse> {
    const { data } = await api.post<AuthorizationResponse>(`${BASE}/authorizations`, body);
    return data;
  },

  async capture(authId: string, body: CaptureRequest): Promise<CardTransactionResponse> {
    const { data } = await api.post<CardTransactionResponse>(
      `${BASE}/authorizations/${authId}/capture`,
      body
    );
    return data;
  },

  async refundTransaction(
    transactionId: string,
    body: RefundRequest
  ): Promise<CardTransactionResponse> {
    const { data } = await api.post<CardTransactionResponse>(
      `${BASE}/transactions/${transactionId}/refund`,
      body
    );
    return data;
  },

  // Portal /me (self-service) – send owner identity via headers
  async getMyCards(ownerReferenceId: string, ownerType?: string): Promise<CardResponse[]> {
    const headers: Record<string, string> = { 'X-Owner-Reference-Id': ownerReferenceId };
    if (ownerType != null && ownerType !== '') headers['X-Owner-Type'] = ownerType;
    const { data } = await api.get<CardResponse[]>(`${BASE}/me/cards`, { headers });
    return data;
  },

  async getMyCardStatement(
    cardId: string,
    ownerReferenceId: string,
    ownerType: string | undefined,
    params: { from?: string; to?: string; page?: number; size?: number }
  ): Promise<PagedResponse<CardTransactionResponse>> {
    const headers: Record<string, string> = { 'X-Owner-Reference-Id': ownerReferenceId };
    if (ownerType != null && ownerType !== '') headers['X-Owner-Type'] = ownerType;
    const { data } = await api.get<PagedResponse<CardTransactionResponse>>(
      `${BASE}/me/cards/${cardId}/statement`,
      { params, headers }
    );
    return data;
  },

  // Reports (Phase 5.1)
  async getLiabilities(params: {
    asOf?: string;
    cardProductId?: string;
    ownerType?: string;
  }): Promise<LiabilityReportItem[]> {
    const { data } = await api.get<LiabilityReportItem[]>(`${BASE}/reports/liabilities`, {
      params,
    });
    return data;
  },

  async getUsageByDomain(params: {
    from?: string;
    to?: string;
    sourceSystem?: string;
  }): Promise<UsageByDomainItem[]> {
    const { data } = await api.get<UsageByDomainItem[]>(`${BASE}/reports/usage-by-domain`, {
      params,
    });
    return data;
  },

  async getCorporateExposure(params: {
    corporateId: string;
    asOf?: string;
  }): Promise<CorporateExposureResponse> {
    const { data } = await api.get<CorporateExposureResponse>(
      `${BASE}/reports/corporate-exposure`,
      { params }
    );
    return data;
  },
};

// Report types (Phase 5.1)
export interface LiabilityReportItem {
  cardId: string;
  cardNumber: string;
  ownerType: string;
  ownerReferenceId: string;
  currentBalance: number;
  currency: string;
}

export interface UsageByDomainItem {
  sourceSystem: string;
  totalAmount: number;
  transactionCount: number;
}

export interface CorporateExposureItem {
  cardId: string;
  cardNumber: string;
  ownerType: string;
  ownerReferenceId: string;
  currentBalance: number;
  creditLimit?: number;
  currency: string;
}

export interface CorporateExposureResponse {
  items: CorporateExposureItem[];
  totalBalance: number;
}

export default hospitalCardManagementService;
