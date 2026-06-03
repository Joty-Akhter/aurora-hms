import { createPublicApiClient } from '../utils/publicApi';

export interface WebBookableDoctor {
  doctorId: string;
  doctorName: string;
  doctorCode?: string;
  speciality?: string;
  departmentName?: string;
  numberOfDaysCanAppointment?: number;
}

export interface WebBookingSlot {
  start: string;
  end: string;
  availableCount: number;
}

export interface WebBookingAvailabilityDay {
  date: string;
  blackedOut: boolean;
  slots: WebBookingSlot[];
}

export interface WebBookingAppointmentRequest {
  doctorId: string;
  fullName: string;
  primaryPhone: string;
  primaryEmail?: string;
  ageYears?: number;
  appointmentDate: string;
  slotStart: string;
  slotEnd: string;
}

export interface WebBookingAppointmentResponse {
  appointmentId?: string;
  patientId?: string;
  mrn?: string;
  message: string;
}

const publicApi = createPublicApiClient();

const webBookingService = {
  getDoctors: () => publicApi.get<WebBookableDoctor[]>('/api/public/web-booking/doctors'),

  getAvailability: (doctorId: string, fromDate: string, toDate: string) =>
    publicApi.get<WebBookingAvailabilityDay[]>(
      `/api/public/web-booking/doctors/${doctorId}/availability`,
      { params: { fromDate, toDate } }
    ),

  bookAppointment: (payload: WebBookingAppointmentRequest) =>
    publicApi.post<WebBookingAppointmentResponse>('/api/public/web-booking/appointments', payload),
};

export default webBookingService;
