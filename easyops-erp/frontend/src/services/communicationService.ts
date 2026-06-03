import axios from 'axios';
import api from './api';

export interface Phase0FoundationResponse {
  phase: string;
  status: string;
  brokerStrategy: string;
  serviceBoundary: string;
  templateEngine: string;
  adrs: string[];
  requiredEnvelopeFields: string[];
  conventions: Record<string, string>;
  v1UseCases: string[];
  acceptanceCriteria: string[];
  backlogSnapshot: string[];
  timeline: string;
}

export interface CommunicationTemplate {
  id: string;
  templateKey: string;
  channel: 'SMS' | 'EMAIL';
  locale: string;
  version: number;
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
  subjectTemplate?: string | null;
  bodyTemplate: string;
  variablesSchema: string;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
  activatedAt?: string | null;
  activatedBy?: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TemplatePreviewRequest {
  templateId: string;
  variables: Record<string, unknown>;
}

export interface TemplatePreviewResponse {
  renderedSubject: string | null;
  renderedBody: string;
}

export interface TemplateTestSendRequest {
  templateId: string;
  recipient: string;
  variables: Record<string, unknown>;
}

export interface TemplateTestSendResponse {
  channel: string;
  provider: string;
  status: string;
  providerReference: string;
}

export interface ProviderHealthResponse {
  provider: string;
  channel: string;
  status: string;
  details: string;
}

export interface InboundCommunicationEvent {
  eventId: string;
  eventType:
    | 'APPOINTMENT_CREATED'
    | 'APPOINTMENT_CONFIRMED'
    | 'APPOINTMENT_CANCELLED'
    | 'INVOICE_CREATED'
    | 'INVOICE_OVERDUE'
    | 'INVOICE_PAID';
  eventVersion: string;
  occurredAt: string;
  organizationId: string;
  entityId: string;
  actorId: string;
  correlationId: string;
  payload: Record<string, unknown>;
}

export interface CommunicationDelivery {
  id: string;
  eventId: string;
  correlationId: string;
  eventType: string;
  eventVersion: string;
  organizationId: string;
  entityId: string;
  templateKey: string | null;
  channel: string;
  recipient: string;
  templateVersion: number | null;
  idempotencyKey: string;
  status: 'QUEUED' | 'RETRYING' | 'SENT' | 'FAILED' | 'DLQ' | 'SKIPPED';
  failureCategory: string | null;
  failureReason: string | null;
  providerName: string | null;
  providerReference: string | null;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastAttemptAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function getPhase0Foundation(): Promise<Phase0FoundationResponse> {
  const { data } = await api.get<Phase0FoundationResponse>('/api/communications/foundation/phase-0');
  return data;
}

export async function listTemplates(): Promise<PagedResponse<CommunicationTemplate>> {
  const { data } = await api.get<PagedResponse<CommunicationTemplate>>('/api/communication-templates', {
    params: { size: 50, sort: 'updatedAt,desc' },
  });
  return data;
}

export async function previewTemplate(request: TemplatePreviewRequest): Promise<TemplatePreviewResponse> {
  const { data } = await api.post<TemplatePreviewResponse>('/api/communication-templates/preview', request);
  return data;
}

export async function testSendTemplate(request: TemplateTestSendRequest): Promise<TemplateTestSendResponse> {
  const { data } = await api.post<TemplateTestSendResponse>('/api/communication-templates/test-send', request);
  return data;
}

export async function getProviderHealth(): Promise<ProviderHealthResponse[]> {
  const { data } = await api.get<ProviderHealthResponse[]>('/api/communication-templates/providers/health');
  return data;
}

export async function ingestCommunicationEvent(request: InboundCommunicationEvent): Promise<CommunicationDelivery> {
  const { data } = await api.post<CommunicationDelivery>('/api/communications/events', request);
  return data;
}

const DELIVERY_CHANNELS = ['SMS', 'EMAIL'] as const;

function hasDeliveryFilter(params: {
  correlationId?: string;
  eventId?: string;
  status?: string;
  channel?: string;
}): boolean {
  return Boolean(
    params.correlationId?.trim() ||
      params.eventId?.trim() ||
      params.status?.trim() ||
      params.channel?.trim(),
  );
}

function isDeliveryFilterRequiredError(err: unknown): boolean {
  if (!axios.isAxiosError(err)) return false;
  const message = String(err.response?.data?.message ?? '');
  return err.response?.status === 400 && message.includes('Provide at least one filter');
}

function mergeDeliveryPages(
  pages: PagedResponse<CommunicationDelivery>[],
  limit: number,
): PagedResponse<CommunicationDelivery> {
  const byId = new Map<string, CommunicationDelivery>();
  for (const page of pages) {
    for (const item of page.content) {
      byId.set(item.id, item);
    }
  }
  const content = [...byId.values()]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, limit);
  return {
    content,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    size: limit,
    number: 0,
  };
}

async function queryDeliveriesAcrossChannels(
  size: number,
): Promise<PagedResponse<CommunicationDelivery>> {
  const pages = await Promise.all(
    DELIVERY_CHANNELS.map(async (channel) => {
      const { data } = await api.get<PagedResponse<CommunicationDelivery>>('/api/communications/deliveries', {
        params: { channel, size, sort: 'createdAt,desc' },
      });
      return data;
    }),
  );
  return mergeDeliveryPages(pages, size);
}

export async function queryCommunicationDeliveries(
  params: {
    correlationId?: string;
    eventId?: string;
    status?: string;
    channel?: string;
  },
  options?: { size?: number },
): Promise<PagedResponse<CommunicationDelivery>> {
  const size = options?.size ?? 50;
  const requestParams = { ...params, size, sort: 'createdAt,desc' };

  if (!hasDeliveryFilter(params)) {
    try {
      const { data } = await api.get<PagedResponse<CommunicationDelivery>>('/api/communications/deliveries', {
        params: requestParams,
      });
      return data;
    } catch (err: unknown) {
      if (!isDeliveryFilterRequiredError(err)) {
        throw err;
      }
      return queryDeliveriesAcrossChannels(size);
    }
  }

  const { data } = await api.get<PagedResponse<CommunicationDelivery>>('/api/communications/deliveries', {
    params: requestParams,
  });
  return data;
}

export async function resendCommunicationDelivery(deliveryId: string, reason?: string): Promise<CommunicationDelivery> {
  const { data } = await api.post<CommunicationDelivery>(`/api/communications/deliveries/${deliveryId}/resend`, {
    reason: reason ?? 'manual_resend',
  });
  return data;
}

export interface CommunicationTemplateCreateRequest {
  templateKey: string;
  channel: 'SMS' | 'EMAIL';
  locale: string;
  version: number;
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
  subjectTemplate?: string;
  bodyTemplate: string;
  variablesSchema: string;
}

export type CommunicationTemplateUpdateRequest = Partial<CommunicationTemplateCreateRequest>;

export interface OpsAlertStatusResponse {
  evaluatedAt: string;
  alerts: Array<{
    key: string;
    level: string;
    triggered: boolean;
    message: string;
  }>;
}

export interface ProviderSecretStatusResponse {
  providers: Array<{
    provider: string;
    channel: string;
    credentialsConfigured: boolean;
    sourcePolicy: string;
  }>;
}

export interface TestSmsSendResponse {
  status: string;
  provider: string;
  providerReference: string;
}

export async function getTemplate(id: string): Promise<CommunicationTemplate> {
  const { data } = await api.get<CommunicationTemplate>(`/api/communication-templates/${id}`);
  return data;
}

export async function createTemplate(request: CommunicationTemplateCreateRequest): Promise<CommunicationTemplate> {
  const { data } = await api.post<CommunicationTemplate>('/api/communication-templates', request);
  return data;
}

export async function updateTemplate(
  id: string,
  request: CommunicationTemplateUpdateRequest,
): Promise<CommunicationTemplate> {
  const { data } = await api.patch<CommunicationTemplate>(`/api/communication-templates/${id}`, request);
  return data;
}

export async function deleteTemplate(id: string): Promise<void> {
  await api.delete(`/api/communication-templates/${id}`);
}

export async function getOpsAlerts(): Promise<OpsAlertStatusResponse> {
  const { data } = await api.get<OpsAlertStatusResponse>('/api/communications/operations/alerts');
  return data;
}

export async function getProviderSecretsStatus(): Promise<ProviderSecretStatusResponse> {
  const { data } = await api.get<ProviderSecretStatusResponse>('/api/communications/operations/secrets/status');
  return data;
}

export async function sendTestSms(recipient: string): Promise<TestSmsSendResponse> {
  const { data } = await api.post<TestSmsSendResponse>('/api/communications/operations/sms/test', { recipient });
  return data;
}

const communicationService = {
  getPhase0Foundation,
  listTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  previewTemplate,
  testSendTemplate,
  getProviderHealth,
  getOpsAlerts,
  getProviderSecretsStatus,
  sendTestSms,
  ingestCommunicationEvent,
  queryCommunicationDeliveries,
  resendCommunicationDelivery,
};

export default communicationService;
