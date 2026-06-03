import api from './api';

// ========== Type Definitions ==========

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

// Resources
export interface CreateResourceRequest {
  resourceType: string;
  externalReferenceId: string;
  name: string;
  branchId?: string;
  departmentId?: string;
  metadata?: string;
  status?: string;
}

export interface ResourceResponse {
  id: string;
  resourceType: string;
  externalReferenceId: string;
  name: string;
  branchId?: string;
  departmentId?: string;
  metadata?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface UpdateResourceRequest {
  resourceType?: string;
  externalReferenceId?: string;
  name?: string;
  branchId?: string;
  departmentId?: string;
  metadata?: string;
  status?: string;
}

// Working hours (batch: SetWorkingHoursRequest with entries)
export interface WorkingHoursEntryDto {
  dayOfWeek: number; // 0=Sunday .. 6=Saturday
  startTime: string; // "HH:mm" or ISO time
  endTime: string;
  effectiveFrom?: string; // ISO date
  effectiveTo?: string;
}

export interface SetWorkingHoursRequest {
  entries: WorkingHoursEntryDto[];
}

export interface WorkingHoursResponse {
  id: string;
  resourceId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdAt?: string;
  updatedAt?: string;
}

// Slot templates
export interface CreateSlotTemplateRequest {
  name: string;
  resourceType?: string;
  branchId?: string;
  slotDurationMinutes: number;
  slotsPerInterval: number;
  startTime: string; // "HH:mm"
  endTime: string;
  leadTimeDays?: number;
  maxAdvanceDays?: number;
  status?: string;
}

export interface SlotTemplateResponse {
  id: string;
  name: string;
  resourceType?: string;
  branchId?: string;
  slotDurationMinutes: number;
  slotsPerInterval: number;
  startTime: string;
  endTime: string;
  leadTimeDays?: number;
  maxAdvanceDays?: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

// Blackouts
export interface CreateBlackoutRequest {
  resourceId?: string;
  branchId?: string;
  blackoutDate: string; // ISO date (YYYY-MM-DD)
  reason?: string;
}

export interface BlackoutResponse {
  id: string;
  resourceId?: string;
  branchId?: string;
  blackoutDate: string;
  reason?: string;
  createdAt?: string;
  createdBy?: string;
}

// Reservations
export interface CreateReservationRequest {
  resourceId: string;
  slotStart: string; // ISO datetime
  slotEnd: string;
  status?: string;
  referenceType?: string;
  referenceId?: string;
  patientId?: string;
  idempotencyKey?: string;
}

export interface ReservationResponse {
  id: string;
  resourceId: string;
  slotStart: string;
  slotEnd: string;
  status: string;
  referenceType?: string;
  referenceId?: string;
  patientId?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface UpdateReservationStatusRequest {
  status: string;
  reason?: string;
}

export interface CheckConflictsRequest {
  resourceId: string;
  slotStart: string;
  slotEnd: string;
  excludeReservationId?: string;
}

export interface ConflictCheckResponse {
  hasConflict: boolean;
  conflictingReservations: ReservationResponse[];
}

// Multi-resource reservation (Phase 4)
export interface CreateMultiResourceReservationRequest {
  resourceIds: string[];
  slotStart: string;
  slotEnd: string;
  referenceType?: string;
  referenceId?: string;
  patientId?: string;
  idempotencyKey?: string;
}

// Roster blocks (Phase 4)
export interface CreateRosterBlockRequest {
  startTime: string; // ISO datetime
  endTime: string;
  type: 'AVAILABLE' | 'UNAVAILABLE' | 'SUBSTITUTE';
  substituteResourceId?: string;
}

export interface RosterBlockResponse {
  id: string;
  resourceId: string;
  startTime: string;
  endTime: string;
  type: string;
  substituteResourceId?: string;
  createdAt?: string;
}

// Availability
export interface SlotAvailabilityDto {
  start: string; // ISO datetime
  end: string;
  availableCount: number;
  /** When set, slot is served by this substitute resource (Phase 4). */
  substituteResourceId?: string;
}

export interface AvailabilityResponse {
  resourceId: string;
  date: string; // ISO date
  slots: SlotAvailabilityDto[];
  blackedOut: boolean;
}

// Appointments (Phase 2)
export interface CreateAppointmentRequest {
  patientId: string;
  resourceId: string;
  clinicOrLocationId?: string;
  appointmentDate: string; // ISO date (YYYY-MM-DD)
  slotStart: string; // ISO datetime
  slotEnd: string;
  appointmentType: string; // NEW, FOLLOW_UP, EMERGENCY, ROUTINE, REPORT
  idempotencyKey?: string;
  /** When slot is at capacity, provide a reason to allow overbooking. */
  overbookingOverrideReason?: string;
  /** Phase 4: optional additional resources (e.g. room); one reservation per resource. */
  additionalResourceIds?: string[];
  bookingChannel?: BookingChannel;
  bookedBy?: string;
  slotTemplateId?: string;
  sessionShift?: SessionShift;
  sessionLabel?: string;
  /** Shown in SMS; optional. */
  patientSmsDisplayName?: string;
  patientSmsPhone?: string;
  /** First token in slot when empty (doctor profile serialStartFrom). */
  serialStartFrom?: number;
}

export interface AppointmentResponse {
  id: string;
  reservationId: string;
  patientId: string;
  resourceId: string;
  clinicOrLocationId?: string;
  appointmentDate: string;
  slotStart: string;
  slotEnd: string;
  appointmentType: string;
  status: string;
  visitId?: string;
  tokenNumber?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  bookingChannel?: string;
  bookedBy?: string;
  slotTemplateId?: string;
  sessionShift?: string;
  sessionLabel?: string;
  notificationPatientPhone?: string;
  notificationPatientName?: string;
}

export interface AppointmentDetailResponse extends AppointmentResponse {
  reservation?: ReservationResponse;
}

export interface RescheduleAppointmentRequest {
  newSlotStart: string; // ISO datetime
  newSlotEnd: string;
  idempotencyKey?: string;
  patientSmsDisplayName?: string;
  patientSmsPhone?: string;
  serialStartFrom?: number;
}

export interface CancelAppointmentRequest {
  reason?: string;
  idempotencyKey?: string;
}

export interface QueueResponse {
  resourceId: string;
  date: string; // ISO date
  appointments: AppointmentResponse[];
}

// Planned admissions (Phase 3)
export interface CreatePlannedAdmissionRequest {
  patientId: string;
  preferredDate: string; // ISO date (YYYY-MM-DD)
  preferredWardOrBedClass?: string;
}

export interface PlannedAdmissionResponse {
  id: string;
  patientId: string;
  preferredDate: string;
  preferredWardOrBedClass?: string;
  status: string;
  bedGroupResourceId?: string;
  reservationId?: string;
  expiresAt?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface UpdatePlannedAdmissionStatusRequest {
  status: string; // RESERVED, CONVERTED, EXPIRED, CANCELLED
  bedGroupResourceId?: string; // required when status = RESERVED
  expiresAt?: string; // ISO datetime, optional when RESERVED
}

export interface ExpectedAdmissionsResponse {
  items: ExpectedAdmissionsItem[];
}

export interface ExpectedAdmissionsItem {
  date: string; // ISO date
  count: number;
  wardOrBedClass?: string;
}

// Reports (Phase 6)
export interface UtilizationDataPoint {
  resourceId?: string;
  date: string; // ISO date
  slotUsed: number;
  slotAvailable?: number;
}

export interface UtilizationReportResponse {
  resourceId?: string;
  fromDate: string;
  toDate: string;
  groupBy: string; // DAY, WEEK
  dataPoints: UtilizationDataPoint[];
}

export interface NoShowReportResponse {
  resourceId?: string;
  fromDate: string;
  toDate: string;
  count: number;
  totalAppointmentsInRange?: number;
  noShowRate?: number;
}

export interface CancellationReportResponse {
  resourceId?: string;
  fromDate: string;
  toDate: string;
  count: number;
  totalAppointmentsInRange?: number;
  cancellationRate?: number;
}

// Waitlist (Phase 5)
export interface CreateWaitlistEntryRequest {
  patientId: string;
  resourceId: string;
  preferredFromDate?: string; // ISO date
  preferredToDate?: string;
  priority?: number;
  priorityReason?: string;
}

export interface WaitlistEntryResponse {
  id: string;
  patientId: string;
  resourceId: string;
  preferredFromDate?: string;
  preferredToDate?: string;
  priority: number;
  priorityReason?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export interface UpdateWaitlistStatusRequest {
  status: string; // PROMOTED, CANCELLED, EXPIRED
}

export interface PromoteWaitlistRequest {
  resourceId: string;
  slotStart: string; // ISO datetime
  slotEnd: string;
  maxCandidates?: number;
}

export interface PromoteWaitlistResponse {
  appointment?: AppointmentResponse;
  candidatesContacted?: number;
}

// Phase 7 types

export type BookingChannel = 'WEB' | 'MOBILE' | 'FRONT_DESK' | 'CALL_CENTER' | 'INTERNAL';
export type SessionShift = 'MORNING' | 'EVENING' | 'NIGHT' | 'FULL_DAY' | 'CUSTOM';

export interface DoctorResourceMappingResponse {
  id: string;
  doctorUserId: string;
  resourceId: string;
  branchId?: string;
  isPrimary: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDoctorResourceMappingRequest {
  doctorUserId: string;
  resourceId: string;
  branchId?: string;
  isPrimary?: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
}

export interface UpdateDoctorResourceMappingRequest {
  branchId?: string;
  isPrimary?: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  status?: string;
}

export interface AuditLogResponse {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actorId?: string;
  actorRole?: string;
  bookingChannel?: string;
  reason?: string;
  correlationId?: string;
  beforeState?: string;
  afterState?: string;
  createdAt: string;
}

export interface QueueUpdateEvent {
  eventId?: string;
  eventType: string;
  appointmentId: string;
  patientId: string;
  tokenNumber?: number;
  status: string;
  slotStart?: string;
  timestamp?: string;
}

// ========== Service ==========

const BASE = '/api/hospital-scheduling';

const hospitalSchedulingService = {
  // Resources
  async createResource(body: CreateResourceRequest): Promise<ResourceResponse> {
    const response = await api.post<ResourceResponse>(`${BASE}/resources`, body);
    return response.data;
  },

  async getResource(id: string): Promise<ResourceResponse> {
    const response = await api.get<ResourceResponse>(`${BASE}/resources/${id}`);
    return response.data;
  },

  async getResources(params: {
    resourceType?: string;
    branchId?: string;
    departmentId?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<ResourceResponse>> {
    const response = await api.get<PagedResponse<ResourceResponse>>(`${BASE}/resources`, { params });
    return response.data;
  },

  async updateResource(id: string, body: UpdateResourceRequest): Promise<ResourceResponse> {
    const response = await api.patch<ResourceResponse>(`${BASE}/resources/${id}`, body);
    return response.data;
  },

  // Working hours
  async setWorkingHours(resourceId: string, body: SetWorkingHoursRequest): Promise<WorkingHoursResponse[]> {
    const response = await api.post<WorkingHoursResponse[]>(`${BASE}/resources/${resourceId}/working-hours`, body);
    return response.data;
  },

  async getWorkingHours(resourceId: string): Promise<WorkingHoursResponse[]> {
    const response = await api.get<WorkingHoursResponse[]>(`${BASE}/resources/${resourceId}/working-hours`);
    return response.data;
  },

  // Slot templates
  async createSlotTemplate(body: CreateSlotTemplateRequest): Promise<SlotTemplateResponse> {
    const response = await api.post<SlotTemplateResponse>(`${BASE}/slot-templates`, body);
    return response.data;
  },

  async getSlotTemplate(id: string): Promise<SlotTemplateResponse> {
    const response = await api.get<SlotTemplateResponse>(`${BASE}/slot-templates/${id}`);
    return response.data;
  },

  async getSlotTemplates(params: {
    resourceType?: string;
    branchId?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<SlotTemplateResponse>> {
    const response = await api.get<PagedResponse<SlotTemplateResponse>>(`${BASE}/slot-templates`, { params });
    return response.data;
  },

  // Blackouts
  async createBlackout(body: CreateBlackoutRequest): Promise<BlackoutResponse> {
    const response = await api.post<BlackoutResponse>(`${BASE}/blackouts`, body);
    return response.data;
  },

  async getBlackouts(params: {
    resourceId?: string;
    branchId?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<BlackoutResponse>> {
    const response = await api.get<PagedResponse<BlackoutResponse>>(`${BASE}/blackouts`, { params });
    return response.data;
  },

  async deleteBlackout(id: string): Promise<void> {
    await api.delete(`${BASE}/blackouts/${id}`);
  },

  // Availability
  async getAvailability(params: {
    resourceId: string;
    fromDate: string; // ISO date
    toDate: string;
    slotTemplateId?: string;
  }): Promise<AvailabilityResponse[]> {
    const response = await api.get<AvailabilityResponse[]>(`${BASE}/availability`, { params });
    return response.data;
  },

  // Reservations
  async createReservation(body: CreateReservationRequest): Promise<ReservationResponse> {
    const response = await api.post<ReservationResponse>(`${BASE}/reservations`, body);
    return response.data;
  },

  async getReservation(id: string): Promise<ReservationResponse> {
    const response = await api.get<ReservationResponse>(`${BASE}/reservations/${id}`);
    return response.data;
  },

  async getReservations(params: {
    resourceId?: string;
    patientId?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<ReservationResponse>> {
    const response = await api.get<PagedResponse<ReservationResponse>>(`${BASE}/reservations`, { params });
    return response.data;
  },

  async updateReservationStatus(id: string, body: UpdateReservationStatusRequest): Promise<ReservationResponse> {
    const response = await api.patch<ReservationResponse>(`${BASE}/reservations/${id}/status`, body);
    return response.data;
  },

  async checkConflicts(body: CheckConflictsRequest): Promise<ConflictCheckResponse> {
    const response = await api.post<ConflictCheckResponse>(`${BASE}/reservations/check-conflicts`, body);
    return response.data;
  },

  async createMultiResourceReservation(body: CreateMultiResourceReservationRequest): Promise<ReservationResponse[]> {
    const response = await api.post<ReservationResponse[]>(`${BASE}/reservations/multi`, body);
    return response.data;
  },

  // Roster blocks (Phase 4)
  async createRosterBlock(resourceId: string, body: CreateRosterBlockRequest): Promise<RosterBlockResponse> {
    const response = await api.post<RosterBlockResponse>(`${BASE}/resources/${resourceId}/roster-blocks`, body);
    return response.data;
  },

  async getRosterBlocks(
    resourceId: string,
    params?: { from?: string; to?: string }
  ): Promise<RosterBlockResponse[]> {
    const response = await api.get<RosterBlockResponse[]>(`${BASE}/resources/${resourceId}/roster-blocks`, {
      params: params ?? {},
    });
    return response.data;
  },

  async deleteRosterBlock(id: string): Promise<void> {
    await api.delete(`${BASE}/roster-blocks/${id}`);
  },

  // Appointments
  async createAppointment(body: CreateAppointmentRequest): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments`, body);
    return response.data;
  },

  async getAppointment(id: string): Promise<AppointmentDetailResponse> {
    const response = await api.get<AppointmentDetailResponse>(`${BASE}/appointments/${id}`);
    return response.data;
  },

   async getAppointments(params: {
     patientId?: string;
     resourceId?: string;
     clinicId?: string;
     fromDate?: string;
     toDate?: string;
     status?: string;
     appointmentType?: string;
     page?: number;
     size?: number;
   }): Promise<PagedResponse<AppointmentResponse>> {
    const response = await api.get<PagedResponse<AppointmentResponse>>(`${BASE}/appointments`, { params });
    return response.data;
  },

  async rescheduleAppointment(id: string, body: RescheduleAppointmentRequest): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments/${id}/reschedule`, body);
    return response.data;
  },

  async cancelAppointment(id: string, body?: CancelAppointmentRequest): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments/${id}/cancel`, body ?? {});
    return response.data;
  },

  async deleteAppointment(id: string): Promise<void> {
    await api.delete(`${BASE}/appointments/${id}`);
  },

  async checkInAppointment(id: string): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments/${id}/check-in`);
    return response.data;
  },

  async noShowAppointment(id: string): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments/${id}/no-show`);
    return response.data;
  },

  async completeAppointment(id: string): Promise<AppointmentResponse> {
    const response = await api.post<AppointmentResponse>(`${BASE}/appointments/${id}/complete`, {});
    return response.data;
  },

  async getAppointmentQueue(resourceId: string, date: string): Promise<QueueResponse> {
    const response = await api.get<QueueResponse>(`${BASE}/appointments/queue`, {
      params: { resourceId, date },
    });
    return response.data;
  },

  // Planned admissions
  async createPlannedAdmission(body: CreatePlannedAdmissionRequest): Promise<PlannedAdmissionResponse> {
    const response = await api.post<PlannedAdmissionResponse>(`${BASE}/planned-admissions`, body);
    return response.data;
  },

  async getPlannedAdmission(id: string): Promise<PlannedAdmissionResponse> {
    const response = await api.get<PlannedAdmissionResponse>(`${BASE}/planned-admissions/${id}`);
    return response.data;
  },

  async getPlannedAdmissions(params: {
    patientId?: string;
    preferredDateFrom?: string;
    preferredDateTo?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<PlannedAdmissionResponse>> {
    const response = await api.get<PagedResponse<PlannedAdmissionResponse>>(`${BASE}/planned-admissions`, { params });
    return response.data;
  },

  async updatePlannedAdmissionStatus(
    id: string,
    body: UpdatePlannedAdmissionStatusRequest
  ): Promise<PlannedAdmissionResponse> {
    const response = await api.patch<PlannedAdmissionResponse>(`${BASE}/planned-admissions/${id}/status`, body);
    return response.data;
  },

  async getExpectedAdmissions(
    fromDate: string,
    toDate: string,
    wardOrBedClass?: string
  ): Promise<ExpectedAdmissionsResponse> {
    const response = await api.get<ExpectedAdmissionsResponse>(`${BASE}/planned-admissions/expected`, {
      params: { fromDate, toDate, wardOrBedClass },
    });
    return response.data;
  },

  // Waitlist (Phase 5)
  async createWaitlistEntry(body: CreateWaitlistEntryRequest): Promise<WaitlistEntryResponse> {
    const response = await api.post<WaitlistEntryResponse>(`${BASE}/waitlist`, body);
    return response.data;
  },

  async getWaitlist(params: {
    resourceId?: string;
    patientId?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<WaitlistEntryResponse>> {
    const response = await api.get<PagedResponse<WaitlistEntryResponse>>(`${BASE}/waitlist`, { params });
    return response.data;
  },

  async updateWaitlistStatus(id: string, body: UpdateWaitlistStatusRequest): Promise<WaitlistEntryResponse> {
    const response = await api.patch<WaitlistEntryResponse>(`${BASE}/waitlist/${id}/status`, body);
    return response.data;
  },

  async promoteWaitlist(body: PromoteWaitlistRequest): Promise<PromoteWaitlistResponse> {
    const response = await api.post<PromoteWaitlistResponse>(`${BASE}/waitlist/promote`, body);
    return response.data;
  },

  // Reports (Phase 6)
  async getUtilizationReport(params: {
    resourceId?: string;
    fromDate: string;
    toDate: string;
    groupBy?: string;
  }): Promise<UtilizationReportResponse> {
    const response = await api.get<UtilizationReportResponse>(`${BASE}/reports/utilization`, {
      params: { groupBy: params.groupBy ?? 'DAY', ...params },
    });
    return response.data;
  },

  async getNoShowReport(params: {
    resourceId?: string;
    fromDate: string;
    toDate: string;
  }): Promise<NoShowReportResponse> {
    const response = await api.get<NoShowReportResponse>(`${BASE}/reports/no-show`, { params });
    return response.data;
  },

  async getCancellationReport(params: {
    resourceId?: string;
    fromDate: string;
    toDate: string;
  }): Promise<CancellationReportResponse> {
    const response = await api.get<CancellationReportResponse>(`${BASE}/reports/cancellations`, { params });
    return response.data;
  },

  // Phase 7 – Doctor resource mappings
  async getDoctorResourceMappings(params?: {
    doctorUserId?: string;
    resourceId?: string;
    branchId?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<DoctorResourceMappingResponse>> {
    const response = await api.get<PagedResponse<DoctorResourceMappingResponse>>(
      `${BASE}/doctor-resource-mappings`, { params: params ?? {} }
    );
    return response.data;
  },

  async createDoctorResourceMapping(body: CreateDoctorResourceMappingRequest): Promise<DoctorResourceMappingResponse> {
    const response = await api.post<DoctorResourceMappingResponse>(`${BASE}/doctor-resource-mappings`, body);
    return response.data;
  },

  async updateDoctorResourceMapping(id: string, body: UpdateDoctorResourceMappingRequest): Promise<DoctorResourceMappingResponse> {
    const response = await api.patch<DoctorResourceMappingResponse>(`${BASE}/doctor-resource-mappings/${id}`, body);
    return response.data;
  },

  async resolveDoctorResourceMapping(doctorUserId: string, branchId?: string): Promise<DoctorResourceMappingResponse> {
    const response = await api.get<DoctorResourceMappingResponse>(`${BASE}/doctor-resource-mappings/resolve`, {
      params: { doctorUserId, ...(branchId ? { branchId } : {}) },
    });
    return response.data;
  },

  // Phase 7 – Audit log
  async getAuditLog(params?: {
    entityType?: string;
    entityId?: string;
    actorId?: string;
    action?: string;
    fromDate?: string;
    toDate?: string;
    correlationId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<AuditLogResponse>> {
    const response = await api.get<PagedResponse<AuditLogResponse>>(`${BASE}/audit-log`, { params: params ?? {} });
    return response.data;
  },

  async getAuditLogEntry(id: string): Promise<AuditLogResponse> {
    const response = await api.get<AuditLogResponse>(`${BASE}/audit-log/${id}`);
    return response.data;
  },

  // Phase 7 – SSE queue stream
  subscribeQueueStream(
    resourceId: string,
    date: string,
    onEvent: (event: QueueUpdateEvent) => void,
    onSnapshot: (snapshot: any) => void,
    onError?: (err: Event) => void
  ): () => void {
    const url = `${BASE}/appointments/queue/stream?resourceId=${encodeURIComponent(resourceId)}&date=${encodeURIComponent(date)}`;
    // Note: EventSource does not support custom headers; the JWT/userId headers must be
    // passed via cookies or query params in production. For now, use polling fallback
    // until SSE auth is resolved. This method wires up EventSource for environments
    // where auth is handled at transport level.
    const es = new EventSource(url);
    es.addEventListener('SNAPSHOT', (e: MessageEvent) => {
      try { onSnapshot(JSON.parse(e.data)); } catch {}
    });
    // listen to all named events by intercepting onmessage + addEventListener for known types
    const eventTypes = ['CHECKED_IN', 'CANCELLED', 'NO_SHOW', 'RESCHEDULED', 'TOKEN_CHANGED', 'CREATED', 'COMPLETED'];
    eventTypes.forEach((type) => {
      es.addEventListener(type, (e: MessageEvent) => {
        try { onEvent({ ...JSON.parse(e.data), eventType: type }); } catch {}
      });
    });
    if (onError) es.onerror = onError;
    return () => es.close();
  },
};

export default hospitalSchedulingService;
