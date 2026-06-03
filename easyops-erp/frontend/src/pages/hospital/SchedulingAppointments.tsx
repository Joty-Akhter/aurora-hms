import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  addDays,
  addMonths,
  eachDayOfInterval,
  endOfMonth,
  endOfWeek,
  format,
  isBefore,
  isSameMonth,
  isToday,
  parseISO,
  startOfDay,
  startOfMonth,
  startOfWeek,
  subMonths,
} from 'date-fns';
import {
  Autocomplete,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Checkbox,
  Dialog,
  IconButton,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormHelperText,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Typography,
  useTheme,
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  EventAvailable as RescheduleIcon,
  Cancel as CancelIcon,
  Delete as DeleteIcon,
  Login as CheckInIcon,
  PersonOff as NoShowIcon,
  Close as CloseIcon,
  PersonAdd as PersonAddIcon,
  CheckCircle as CheckCircleIcon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import { useAuth } from '@contexts/AuthContext';
import hospitalSchedulingService, {
  AppointmentDetailResponse,
  AppointmentResponse,
  CreateAppointmentRequest,
  ResourceResponse,
  RescheduleAppointmentRequest,
  SlotAvailabilityDto,
} from '../../services/hospitalSchedulingService';
import hospitalService, { Doctor, DuplicatePatientResponse, Patient } from '../../services/hospitalService';
import {
  digitsOnlyPhone,
  isValidPhoneDigitLength,
  MIN_PHONE_DIGITS_FOR_UNIQUENESS,
} from '../../utils/formValidation';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { phoneDuplicateHintMessage } from '../../utils/patientDuplicateUtils';
import { dobFromAge, formatAge } from '../../utils/ageUtils';
import {
  dedupeAvailabilitySlots,
  getConfiguredMaxSlotsForDate,
} from '../../utils/appointmentSlotUtils';
import {
  computeNextAppointmentSerial,
  projectedSerialForSlot,
  slotInstantEqual,
} from '../../utils/appointmentSerialUtils';
import {
  doctorDepartmentMap,
  doctorNameWithDepartment,
  schedulingResourceDoctorLabel,
} from '../../utils/schedulingDoctorLabel';
import {
  formatSchedulingAppointmentTypeLabel,
  formatSchedulingStatusLabel,
} from '../../utils/schedulingDisplay';
import './Hospital.css';

const APPOINTMENT_TYPES = ['NEW', 'FOLLOW_UP', 'EMERGENCY', 'ROUTINE', 'REPORT'];
const APPOINTMENT_STATUSES = ['CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
const INACTIVE_APPOINTMENT_STATUSES = new Set(['CANCELLED', 'NO_SHOW']);

function isActiveAppointmentStatus(status: string | undefined | null): boolean {
  return !!status && !INACTIVE_APPOINTMENT_STATUSES.has(status);
}

const UUID_STRING_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuidString(value: string): boolean {
  return UUID_STRING_REGEX.test(value.trim());
}

/** Patient record phone (not appointment SMS). */
function patientRegistryPhone(patient: Patient | null | undefined): string {
  if (!patient || patient.consentTextMessaging === false) {
    return '';
  }
  return digitsOnlyPhone(patient.primaryPhone?.trim() || patient.secondaryPhone?.trim() || '');
}

/** Merge sorted yyyy-MM-dd off-day entries into contiguous ranges for display. */
function mergeOffDayRanges(dates: string[]): { start: string; end: string }[] {
  const sorted = [...new Set(dates.map((d) => d.trim()).filter(Boolean))].sort();
  if (sorted.length === 0) return [];
  const ranges: { start: string; end: string }[] = [];
  for (const d of sorted) {
    const last = ranges[ranges.length - 1];
    if (last) {
      const dayAfterLast = format(addDays(parseISO(last.end), 1), 'yyyy-MM-dd');
      if (dayAfterLast === d) {
        last.end = d;
        continue;
      }
    }
    ranges.push({ start: d, end: d });
  }
  return ranges;
}

function escapeHtmlForPrint(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/** Encode slot pair for MUI Select; ISO datetimes must not be truncated (timezone matters). */
function rescheduleSlotSelectValue(start: string, end: string): string {
  if (!start?.trim() || !end?.trim()) return '';
  return `${start}|${end}`;
}

function parseRescheduleSlotSelectValue(value: string): { start: string; end: string } {
  const pipe = value.indexOf('|');
  if (pipe < 0) return { start: '', end: '' };
  return { start: value.slice(0, pipe), end: value.slice(pipe + 1) };
}

function slotsEqual(startA: string, endA: string, startB: string, endB: string): boolean {
  return slotInstantEqual(startA, startB) && slotInstantEqual(endA, endB);
}

/** Calendar date from hospital scheduling ISO datetime (avoid browser-local TZ skew). */
function slotCalendarDate(iso: string): string {
  const dt = parseISO(iso.trim());
  return isNaN(dt.getTime()) ? '' : format(dt, 'yyyy-MM-dd');
}

type PatientFilterOption = { label: string; id: string };

const SchedulingAppointmentsPage: React.FC = () => {
  const theme = useTheme();
  const { enqueueSnackbar } = useSnackbar();
  const { canManageResource, hasPermission, hasRole } = useAuth();

  /** Reschedule / cancel / bulk actions. Hospital manage covers staff; HOSPITAL_APPOINTMENT_BOOK covers call centre/attendants;
   *  HOSPITAL_APPOINTMENT_RESCHEDULE and HOSPITAL_APPOINTMENT_CANCEL cover granular role access (e.g. DOCTOR_ATTENDANTS). */
  const canRescheduleOrCancelAppointments =
    canManageResource('hospital') ||
    hasPermission('HOSPITAL_APPOINTMENT_BOOK') ||
    hasPermission('HOSPITAL_APPOINTMENT_RESCHEDULE') ||
    hasPermission('HOSPITAL_APPOINTMENT_CANCEL');
  /** Check-in / no-show / complete — hospital manage or HOSPITAL_APPOINTMENT_UPDATE_STATUS. */
  const canUpdateAppointmentStatus =
    canManageResource('hospital') || hasPermission('HOSPITAL_APPOINTMENT_UPDATE_STATUS');
  /** Create new appointment (book permission or full hospital manage). */
  const canBookAppointment =
    canManageResource('hospital') || hasPermission('HOSPITAL_APPOINTMENT_BOOK');
  /** Hard-delete — System Admin only. */
  const canDeleteAppointment =
    hasRole('SYSTEM_ADMIN') || hasRole('SYSTEM_ADMINISTRATOR') || hasRole('SUPER_ADMIN');

  const [loading, setLoading] = useState<boolean>(false);
  // Re-render timer to disable past time slots in real time.
  const [now, setNow] = useState<Date>(() => new Date());
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [selectedAppointmentIds, setSelectedAppointmentIds] = useState<Set<string>>(new Set());
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  // doctorId → scheduling resourceId (built from resources where externalReferenceId === doctorId)
  const [doctorResourceMap, setDoctorResourceMap] = useState<Map<string, string>>(new Map());
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
  const [doctorResolving, setDoctorResolving] = useState<boolean>(false);
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [filterDraft, setFilterDraft] = useState(() => ({
    patientId: '',
    resourceId: '',
    fromDate: format(new Date(), 'yyyy-MM-dd'),
    toDate: format(new Date(), 'yyyy-MM-dd'),
    status: '',
    appointmentType: '',
  }));
  const [appliedFilters, setAppliedFilters] = useState(() => ({
    patientId: '',
    resourceId: '',
    fromDate: format(new Date(), 'yyyy-MM-dd'),
    toDate: format(new Date(), 'yyyy-MM-dd'),
    status: '',
    appointmentType: '',
  }));
  const [resourceFilterInput, setResourceFilterInput] = useState<string>('');

  const doctorDepartmentById = useMemo(() => doctorDepartmentMap(doctors), [doctors]);

  /** List filter: patient autocomplete (same pattern as PharmacyDispense / billing). */
  const patientFilterSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [patientFilterInputValue, setPatientFilterInputValue] = useState('');
  const [patientFilterOptions, setPatientFilterOptions] = useState<PatientFilterOption[]>([]);
  const [patientFilterSearching, setPatientFilterSearching] = useState(false);

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(true);

  useEffect(() => {
    if (!canBookAppointment && createDialogOpen) {
      setCreateDialogOpen(false);
    }
  }, [canBookAppointment, createDialogOpen]);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 15000);
    return () => clearInterval(t);
  }, []);

  const [createForm, setCreateForm] = useState<CreateAppointmentRequest>({
    patientId: '',
    resourceId: '',
    clinicOrLocationId: '',
    appointmentDate: '',
    slotStart: '',
    slotEnd: '',
    appointmentType: 'NEW',
    idempotencyKey: '',
    overbookingOverrideReason: '',
    additionalResourceIds: [],
  });

  const [availabilitySlots, setAvailabilitySlots] = useState<SlotAvailabilityDto[]>([]);
  const [availabilityLoading, setAvailabilityLoading] = useState<boolean>(false);

  // Calendar state for the booking dialog Step 2
  const [calendarMonth, setCalendarMonth] = useState<Date>(() => new Date());
  const [monthAvailability, setMonthAvailability] = useState<Map<string, { slotCount: number; blackedOut: boolean }>>(new Map());
  const [monthAvailabilityLoading, setMonthAvailabilityLoading] = useState<boolean>(false);
  const [monthFetchDone, setMonthFetchDone] = useState<boolean>(false);
  const [monthFetchError, setMonthFetchError] = useState<boolean>(false);

  /** Next serial/token for the booking date (or today before a date is chosen), from queue API. */
  const [nextSerialInfo, setNextSerialInfo] = useState<{ n: number; date: string } | null>(null);
  const [nextSerialLoading, setNextSerialLoading] = useState<boolean>(false);
  /** Day queue for SL preview on slot cards and next-serial label. */
  const [queueDayAppointments, setQueueDayAppointments] = useState<AppointmentResponse[]>([]);

  const [viewDialogOpen, setViewDialogOpen] = useState<boolean>(false);
  const [viewDetail, setViewDetail] = useState<AppointmentDetailResponse | null>(null);
  const [viewLoading, setViewLoading] = useState<boolean>(false);

  const [rescheduleDialogOpen, setRescheduleDialogOpen] = useState<boolean>(false);
  const [rescheduleIds, setRescheduleIds] = useState<string[]>([]);
  const [rescheduleResourceId, setRescheduleResourceId] = useState<string>('');
  const [rescheduleDate, setRescheduleDate] = useState<string>('');
  const [rescheduleSlots, setRescheduleSlots] = useState<SlotAvailabilityDto[]>([]);
  const [rescheduleSlotsLoading, setRescheduleSlotsLoading] = useState<boolean>(false);
  const [rescheduleDayBlackedOut, setRescheduleDayBlackedOut] = useState<boolean>(false);
  const [rescheduleAvailabilityError, setRescheduleAvailabilityError] = useState<boolean>(false);
  /** Snapshot of slot times when dialog opens (for same-slot checks independent of list page). */
  const [rescheduleSources, setRescheduleSources] = useState<{ id: string; slotStart: string; slotEnd: string }[]>([]);
  const [rescheduleForm, setRescheduleForm] = useState<RescheduleAppointmentRequest>({
    newSlotStart: '',
    newSlotEnd: '',
    idempotencyKey: '',
  });
  const [rescheduleSubmitting, setRescheduleSubmitting] = useState<boolean>(false);
  const [reschedulePatientId, setReschedulePatientId] = useState<string>('');
  const [rescheduleAppointmentSmsPhone, setRescheduleAppointmentSmsPhone] = useState<string>('');

  const [cancelDialogOpen, setCancelDialogOpen] = useState<boolean>(false);
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState<string>('');
  const [cancelSubmitting, setCancelSubmitting] = useState<boolean>(false);

  const [deleteDialogOpen, setDeleteDialogOpen] = useState<boolean>(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState<boolean>(false);

  /** patientId → display name (empty string = lookup failed, show id as fallback) */
  const [patientNamesById, setPatientNamesById] = useState<Record<string, string>>({});
  const patientNamesByIdRef = useRef<Record<string, string>>({});
  patientNamesByIdRef.current = patientNamesById;
  const [patientDetailsById, setPatientDetailsById] = useState<Record<string, Patient>>({});
  const patientDetailsByIdRef = useRef<Record<string, Patient>>({});
  patientDetailsByIdRef.current = patientDetailsById;

  const cachePatientInState = useCallback((patientId: string, patient: Patient) => {
    setPatientDetailsById((prev) => {
      const next = { ...prev, [patientId]: patient };
      patientDetailsByIdRef.current = next;
      return next;
    });
    const name = patient.fullName?.trim();
    if (name) {
      setPatientNamesById((prev) => {
        const next = { ...prev, [patientId]: name };
        patientNamesByIdRef.current = next;
        return next;
      });
    }
  }, []);

  // Patient search / new-patient state for the booking dialog
  const [patientSearchTerm, setPatientSearchTerm] = useState<string>('');
  const [patientSearchResults, setPatientSearchResults] = useState<Patient[]>([]);
  const [patientSearchLoading, setPatientSearchLoading] = useState<boolean>(false);
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [showPatientDropdown, setShowPatientDropdown] = useState<boolean>(false);
  /** Index in combined list: patients first, then "Register new patient". -1 = none. */
  const [patientDropdownHighlight, setPatientDropdownHighlight] = useState<number>(-1);
  const [newPatientMode, setNewPatientMode] = useState<boolean>(false);
  const [newPatientName, setNewPatientName] = useState<string>('');
  const [newPatientAge, setNewPatientAge] = useState<string>('');
  const [newPatientGender, setNewPatientGender] = useState<'Male' | 'Female' | 'Other' | ''>('');
  const [newPatientPhone, setNewPatientPhone] = useState<string>('');
  const [newPatientPhoneHint, setNewPatientPhoneHint] = useState<string | null>(null);
  /** SMS/confirmation number for this booking (may differ from patient record). */
  const [appointmentSmsPhone, setAppointmentSmsPhone] = useState<string>('');
  const [appointmentSmsTouched, setAppointmentSmsTouched] = useState(false);
  const [patientBookedSameDay, setPatientBookedSameDay] = useState(false);
  const patientSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const bookingSectionRef = useRef<HTMLDivElement | null>(null);
  /** Skip one appointments effect run after programmatic filter/page sync (prevents double fetch). */
  const suppressAppointmentsLoadEffectRef = useRef(false);
  const todayStart = startOfDay(new Date());

  const bookingWindowEnd = useMemo(() => {
    const limitDays = selectedDoctor?.numberOfDaysCanAppointment;
    if (!limitDays || limitDays <= 0) return null;
    const end = new Date(todayStart);
    end.setDate(end.getDate() + (limitDays - 1));
    return end;
  }, [selectedDoctor?.numberOfDaysCanAppointment]);

  const isWithinBookingWindow = (day: Date) =>
    !bookingWindowEnd || !isBefore(bookingWindowEnd, day);

  const loadResources = useCallback(async () => {
    try {
      const [resourcesRes, doctorsRes] = await Promise.all([
        hospitalSchedulingService.getResources({ page: 0, size: 500, status: 'ACTIVE', resourceType: 'DOCTOR' }),
        hospitalService.getDoctors(),
      ]);
      const resourceList = resourcesRes.content;
      setResources(resourceList);
      setDoctors(doctorsRes.data);
      // Build doctorId → resourceId map using externalReferenceId
      const map = new Map<string, string>();
      resourceList.forEach((r) => {
        if (r.externalReferenceId) map.set(r.externalReferenceId, r.id);
      });
      setDoctorResourceMap(map);
    } catch {
      setResources([]);
      setDoctors([]);
    }
  }, []);

  type AppointmentListFilters = typeof appliedFilters;

  const loadAppointments = useCallback(async (filtersOverride?: AppointmentListFilters, pageOverride?: number) => {
    const filters = filtersOverride ?? appliedFilters;
    const pageToUse = pageOverride ?? page;
    try {
      setSelectedAppointmentIds(new Set());
      setLoading(true);
      const params: {
        page: number;
        size: number;
        patientId?: string;
        resourceId?: string;
        fromDate?: string;
        toDate?: string;
        status?: string;
        appointmentType?: string;
      } = { page: pageToUse, size };
      const rawPatient = filters.patientId.trim();
      let patientIdFilterSet: Set<string> | null = null;
      if (rawPatient) {
        if (isUuidString(rawPatient)) {
          params.patientId = rawPatient;
        } else {
          try {
            const { data } = await hospitalService.getPatientByMrn(rawPatient);
            params.patientId = data.patientId;
          } catch {
            try {
              const { data } = await hospitalService.searchPatients(rawPatient);
              const ids = (data ?? []).map((p) => p.patientId).filter(Boolean);
              if (ids.length === 0) {
                enqueueSnackbar(`No patient found for ID, MRN, or name: ${rawPatient}`, { variant: 'error' });
                setAppointments([]);
                setTotalElements(0);
                return;
              }
              patientIdFilterSet = new Set(ids);
            } catch {
              enqueueSnackbar(`No patient found for ID, MRN, or name: ${rawPatient}`, { variant: 'error' });
              setAppointments([]);
              setTotalElements(0);
              return;
            }
          }
        }
      }
       if (filters.resourceId.trim()) params.resourceId = filters.resourceId.trim();
       if (filters.fromDate.trim()) params.fromDate = filters.fromDate.trim();
       if (filters.toDate.trim()) params.toDate = filters.toDate.trim();
       if (filters.status.trim()) params.status = filters.status.trim();
       if (filters.appointmentType.trim()) params.appointmentType = filters.appointmentType.trim();
       const response = await hospitalSchedulingService.getAppointments(params);
      const content = response.content ?? [];
      const filteredContent = patientIdFilterSet
        ? content.filter((a) => patientIdFilterSet?.has(a.patientId))
        : content;
      const detailsSnapshot = patientDetailsByIdRef.current;
      setAppointments((prev) => {
        const prevById = new Map(prev.map((row) => [row.id, row]));
        return filteredContent.map((a) => {
          const prevRow = prevById.get(a.id);
          const snapshotPhone =
            a.notificationPatientPhone?.trim() || prevRow?.notificationPatientPhone?.trim();
          if (snapshotPhone) return { ...a, notificationPatientPhone: snapshotPhone };
          const registryPhone = patientRegistryPhone(detailsSnapshot[a.patientId]);
          return registryPhone ? { ...a, notificationPatientPhone: registryPhone } : a;
        });
      });
      setTotalElements(patientIdFilterSet ? filteredContent.length : (response.totalElements ?? filteredContent.length));
    } catch (err) {
      console.error('Failed to load appointments', err);
      enqueueSnackbar('Failed to load appointments', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, appliedFilters, enqueueSnackbar]);

  const fetchPatientFilterOptions = useCallback(async (raw: string) => {
    const q = raw.trim();
    if (!q) {
      setPatientFilterOptions([]);
      setPatientFilterSearching(false);
      return;
    }
    if (isUuidString(q)) {
      try {
        setPatientFilterSearching(true);
        const { data } = await hospitalService.getPatient(q.trim());
        setPatientFilterOptions([
          {
            id: data.patientId,
            label: `${data.fullName ?? '?'}${data.mrn ? ` (${data.mrn})` : ''}`,
          },
        ]);
      } catch {
        setPatientFilterOptions([]);
      } finally {
        setPatientFilterSearching(false);
      }
      return;
    }
    if (q.length < 2) {
      setPatientFilterOptions([]);
      setPatientFilterSearching(false);
      return;
    }
    try {
      setPatientFilterSearching(true);
      const res = await hospitalService.searchPatients(q);
      const list = (res.data ?? []).slice(0, 20);
      setPatientFilterOptions(
        list.map((p) => ({
          id: p.patientId,
          label: `${p.fullName ?? '?'}${p.mrn ? ` (${p.mrn})` : ''}`,
        })),
      );
    } catch {
      setPatientFilterOptions([]);
    } finally {
      setPatientFilterSearching(false);
    }
  }, []);

  useEffect(() => () => {
    if (patientFilterSearchTimer.current) clearTimeout(patientFilterSearchTimer.current);
  }, []);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    if (suppressAppointmentsLoadEffectRef.current) {
      suppressAppointmentsLoadEffectRef.current = false;
      return;
    }
    loadAppointments();
  }, [loadAppointments]);

  useEffect(() => {
    const ids = [...new Set(appointments.map((a) => a.patientId).filter(Boolean))] as string[];
    const missing = ids.filter((id) => !(id in patientNamesByIdRef.current));
    if (missing.length === 0) return;

    let cancelled = false;
    void (async () => {
      const updates: Record<string, string> = {};
      const detailUpdates: Record<string, Patient> = {};
      await Promise.all(
        missing.map(async (id) => {
          try {
            const { data } = await hospitalService.getPatient(id);
            updates[id] = data?.fullName?.trim() || '';
            detailUpdates[id] = data;
          } catch {
            updates[id] = '';
          }
        })
      );
      if (!cancelled) {
        setPatientNamesById((p) => ({ ...p, ...updates }));
        setPatientDetailsById((p) => ({ ...p, ...detailUpdates }));
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [appointments]);

  const patientDisplayLabel = (patientId: string | undefined | null) => {
    if (!patientId) return '—';
    const name = patientNamesById[patientId];
    if (name === undefined) return '…';
    return name || patientId;
  };

  useEffect(() => {
    if (createDialogOpen) {
      bookingSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [createDialogOpen]);

  // Fetch availability for the entire calendar month whenever doctor or displayed month changes
  useEffect(() => {
    if (!createDialogOpen || !createForm.resourceId?.trim()) {
      setMonthFetchDone(false);
      return;
    }
    const monthStart = startOfMonth(calendarMonth);
    const monthEnd = endOfMonth(calendarMonth);
    const fetchEndDate = bookingWindowEnd && isBefore(bookingWindowEnd, monthEnd) ? bookingWindowEnd : monthEnd;
    if (isBefore(fetchEndDate, monthStart)) {
      setMonthAvailability(new Map());
      setMonthFetchDone(true);
      setMonthFetchError(false);
      setMonthAvailabilityLoading(false);
      return;
    }
    const firstDay = format(monthStart, 'yyyy-MM-dd');
    const lastDay = format(fetchEndDate, 'yyyy-MM-dd');
    setMonthAvailabilityLoading(true);
    setMonthFetchDone(false);
    setMonthFetchError(false);
    setMonthAvailability(new Map());
    hospitalSchedulingService.getAvailability({
      resourceId: createForm.resourceId.trim(),
      fromDate: firstDay,
      toDate: lastDay,
    }).then((list) => {
      const map = new Map<string, { slotCount: number; blackedOut: boolean }>();
      list.forEach((day) => {
        if (day.date) {
          const deduped = dedupeAvailabilitySlots(day.slots ?? []);
          const available = deduped.filter((s) => s.availableCount > 0);
          const configuredMax = getConfiguredMaxSlotsForDate(
            selectedDoctor?.appointmentSlots,
            String(day.date),
          );
          map.set(String(day.date), {
            slotCount: configuredMax ?? available.length,
            blackedOut: day.blackedOut,
          });
        }
      });
      setMonthAvailability(map);
      setMonthFetchDone(true);
    }).catch((err) => {
      console.error('Failed to load month availability', err);
      setMonthFetchError(true);
      setMonthAvailability(new Map());
    }).finally(() => {
      setMonthAvailabilityLoading(false);
    });
  }, [createForm.resourceId, calendarMonth, createDialogOpen, bookingWindowEnd, selectedDoctor?.appointmentSlots]);

  // Auto-load slots whenever a specific date is selected inside the booking dialog
  useEffect(() => {
    if (!createDialogOpen || !createForm.resourceId?.trim() || !createForm.appointmentDate?.trim()) return;
    setAvailabilityLoading(true);
    setAvailabilitySlots([]);
    hospitalSchedulingService.getAvailability({
      resourceId: createForm.resourceId.trim(),
      fromDate: createForm.appointmentDate.trim(),
      toDate: createForm.appointmentDate.trim(),
    }).then((list) => {
      setAvailabilitySlots(dedupeAvailabilitySlots(list?.[0]?.slots ?? []));
    }).catch(() => {
      setAvailabilitySlots([]);
    }).finally(() => {
      setAvailabilityLoading(false);
    });
  }, [createForm.resourceId, createForm.appointmentDate, createDialogOpen]);

  // One active appointment per patient + doctor + day
  useEffect(() => {
    const pid = createForm.patientId?.trim();
    const rid = createForm.resourceId?.trim();
    const date = createForm.appointmentDate?.trim();
    if (!createDialogOpen || !pid || !rid || !date) {
      setPatientBookedSameDay(false);
      return;
    }
    let cancelled = false;
    hospitalSchedulingService
      .getAppointments({ patientId: pid, resourceId: rid, fromDate: date, toDate: date, size: 10 })
      .then((res) => {
        if (cancelled) return;
        setPatientBookedSameDay(res.content.some((a) => isActiveAppointmentStatus(a.status)));
      })
      .catch(() => {
        if (!cancelled) setPatientBookedSameDay(false);
      });
    return () => {
      cancelled = true;
    };
  }, [createDialogOpen, createForm.patientId, createForm.resourceId, createForm.appointmentDate]);

  // Next serial/token for selected slot (or day preview before slot pick), from queue API.
  useEffect(() => {
    if (!createDialogOpen || !selectedDoctor || doctorResolving || !createForm.resourceId?.trim()) {
      setNextSerialInfo(null);
      setQueueDayAppointments([]);
      return;
    }
    const dateStr = createForm.appointmentDate?.trim() || format(new Date(), 'yyyy-MM-dd');
    const slotStart = createForm.slotStart?.trim();
    const slotEnd = createForm.slotEnd?.trim();
    const serialStart = selectedDoctor.serialStartFrom ?? 1;
    let cancelled = false;
    setNextSerialLoading(true);
    hospitalSchedulingService
      .getAppointments({
        resourceId: createForm.resourceId.trim(),
        fromDate: dateStr,
        toDate: dateStr,
        size: 500,
      })
      .then((paged) => {
        const apps = (paged.content ?? []).filter((a) => isActiveAppointmentStatus(a.status));
        if (!cancelled) setQueueDayAppointments(apps);
        const nextN = computeNextAppointmentSerial(apps, serialStart, slotStart, slotEnd);
        if (!cancelled) setNextSerialInfo({ n: nextN, date: dateStr });
      })
      .catch(() => {
        if (!cancelled) {
          setQueueDayAppointments([]);
          setNextSerialInfo({ n: serialStart > 0 ? serialStart : 1, date: dateStr });
        }
      })
      .finally(() => {
        if (!cancelled) setNextSerialLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [
    createDialogOpen,
    selectedDoctor,
    doctorResolving,
    createForm.resourceId,
    createForm.appointmentDate,
    createForm.slotStart,
    createForm.slotEnd,
  ]);

  const dedupedAvailabilitySlots = useMemo(
    () => dedupeAvailabilitySlots(availabilitySlots),
    [availabilitySlots],
  );

  /** First/last slot time, count, and per-slot duration for the slots panel header. */
  const slotDaySummary = useMemo(() => {
    if (dedupedAvailabilitySlots.length === 0) return null;
    const sorted = dedupedAvailabilitySlots;
    const first = sorted[0];
    const last = sorted[sorted.length - 1];
    const firstStart = parseISO(first.start);
    const lastEnd = parseISO(last.end);
    if (isNaN(firstStart.getTime()) || isNaN(lastEnd.getTime())) return null;
    const templateSlot = sorted.find((s) => s.availableCount > 0) ?? first;
    const ms = parseISO(templateSlot.end).getTime() - parseISO(templateSlot.start).getTime();
    const minutesPerSlot = Math.max(1, Math.round(ms / 60000));
    const configuredMax = createForm.appointmentDate
      ? getConfiguredMaxSlotsForDate(selectedDoctor?.appointmentSlots, createForm.appointmentDate.trim())
      : null;
    return {
      rangeLabel: `${format(firstStart, 'h:mm a')} – ${format(lastEnd, 'h:mm a')}`,
      totalSlots: configuredMax ?? sorted.length,
      minutesPerSlot,
    };
  }, [dedupedAvailabilitySlots, createForm.appointmentDate, selectedDoctor?.appointmentSlots]);

  useEffect(() => {
    if (showPatientDropdown) setPatientDropdownHighlight(-1);
  }, [showPatientDropdown, patientSearchResults]);

  const handleApplyFilters = () => {
    setPage(0);
    setAppliedFilters(filterDraft);
  };

  /** Reload list + resources and re-sync doctor availability without clearing filter inputs. */
  const handleRefresh = () => {
    setPage(0);
    void loadResources();
    void loadAppointments();
    if (createDialogOpen && selectedDoctor) {
      void handleSelectDoctor(selectedDoctor);
    }
  };

  const resetBookingForm = (keepDoctor = true, opts?: { keepAppointmentDate?: boolean }) => {
    setAvailabilitySlots([]);
    setMonthAvailability(new Map());
    setMonthFetchDone(false);
    setMonthFetchError(false);
    setCalendarMonth(new Date());
    resetPatientFields();
    const resourceId = keepDoctor ? createForm.resourceId : '';
    const appointmentDate =
      keepDoctor && opts?.keepAppointmentDate ? createForm.appointmentDate?.trim() || '' : '';
    setCreateForm({
      patientId: '',
      resourceId,
      clinicOrLocationId: '',
      appointmentDate,
      slotStart: '',
      slotEnd: '',
      appointmentType: 'NEW',
      idempotencyKey: '',
      overbookingOverrideReason: '',
      additionalResourceIds: [],
    });
    if (!keepDoctor) {
      setSelectedDoctor(null);
      setAvailabilitySlots([]);
    }
  };

  const handleCloseCreateDialog = () => {
    setCreateDialogOpen(false);
    resetBookingForm(false);
  };

  const appointmentPhone = (a: AppointmentResponse) => {
    const snapshot = a.notificationPatientPhone?.trim();
    if (snapshot) return snapshot;
    return patientRegistryPhone(patientDetailsById[a.patientId]);
  };

  const rescheduleDoctor = useMemo(() => {
    if (!rescheduleResourceId.trim()) return null;
    return (
      doctors.find((d) => doctorResourceMap.get(d.doctorId) === rescheduleResourceId.trim()) ?? null
    );
  }, [rescheduleResourceId, doctors, doctorResourceMap]);

  const checkNewPatientPhoneHint = async (phone: string) => {
    const digits = digitsOnlyPhone(phone.trim());
    if (!isValidPhoneDigitLength(digits)) {
      setNewPatientPhoneHint(null);
      return;
    }
    try {
      const orgId =
        typeof localStorage !== 'undefined' ? localStorage.getItem('currentOrganizationId') : null;
      const dupCheck = await hospitalService.checkDuplicates({
        primaryPhone: digits,
        ...(orgId ? { organizationId: orgId } : {}),
      });
      const hint = phoneDuplicateHintMessage(dupCheck.data);
      setNewPatientPhoneHint(hint);
    } catch {
      setNewPatientPhoneHint(null);
    }
  };

  const normSlot = (s: string) => {
    const t = s.trim();
    if (!t) return t;
    // Backend expects OffsetDateTime. Preserve explicit offsets from availability slots.
    if (/[+-]\d{2}:\d{2}$/.test(t)) return t;
    if (t.length === 16) return `${t}:00+00:00`;
    if (t.length === 19) return `${t}+00:00`;
    if (t.endsWith('Z')) return `${t.slice(0, -1)}+00:00`;
    return t;
  };

  const handleSelectDoctor = async (doctor: Doctor | null) => {
    setSelectedDoctor(doctor);
    setAvailabilitySlots([]);
    setMonthAvailability(new Map());
    setMonthFetchDone(false);
    setMonthFetchError(false);
    setCalendarMonth(new Date());
    if (!doctor) {
      setCreateForm((prev) => ({ ...prev, resourceId: '', appointmentDate: '', slotStart: '', slotEnd: '' }));
      return;
    }
    const cachedResourceId = doctorResourceMap.get(doctor.doctorId);
    // Optimistically use cached mapping for instant UX while we still force a backend sync.
    if (cachedResourceId) {
      setCreateForm((prev) => ({ ...prev, resourceId: cachedResourceId, slotStart: '', slotEnd: '' }));
    }
    // Always resolve via hospital-service proxy so doctor slot config is re-synced to scheduling.
    try {
      setDoctorResolving(true);
      const res = await hospitalService.findOrCreateDoctorSchedulingResource(doctor.doctorId);
      const resourceId = res.data.resourceId;
      setDoctorResourceMap((prev) => new Map(prev).set(doctor.doctorId, resourceId));
      setCreateForm((prev) => ({ ...prev, resourceId, slotStart: '', slotEnd: '' }));
    } catch (err) {
      console.error('Failed to resolve scheduling resource for doctor', err);
      if (cachedResourceId) {
        enqueueSnackbar('Could not re-sync doctor schedule right now. Showing cached availability.', { variant: 'warning' });
      } else {
        enqueueSnackbar('Could not link doctor to scheduling system. Please re-save the doctor\'s appointment settings.', { variant: 'error' });
        setSelectedDoctor(null);
        setCreateForm((prev) => ({ ...prev, resourceId: '', slotStart: '', slotEnd: '' }));
      }
    } finally {
      setDoctorResolving(false);
    }
  };

  const resetPatientFields = () => {
    setPatientSearchTerm('');
    setPatientSearchResults([]);
    setSelectedPatient(null);
    setShowPatientDropdown(false);
    setNewPatientMode(false);
    setNewPatientName('');
    setNewPatientAge('');
    setNewPatientGender('');
    setNewPatientPhone('');
    setNewPatientPhoneHint(null);
    setAppointmentSmsPhone('');
    setAppointmentSmsTouched(false);
  };

  useEffect(() => {
    if (newPatientMode && !appointmentSmsTouched) {
      setAppointmentSmsPhone(digitsOnlyPhone(newPatientPhone));
    }
  }, [newPatientMode, newPatientPhone, appointmentSmsTouched]);

  const handlePatientSearchChange = (value: string) => {
    setPatientSearchTerm(value);
    setPatientDropdownHighlight(-1);
    setSelectedPatient(null);
    setNewPatientMode(false);
    if (patientSearchTimer.current) clearTimeout(patientSearchTimer.current);
    if (value.trim().length < 2) {
      setPatientSearchResults([]);
      setShowPatientDropdown(false);
      return;
    }
    patientSearchTimer.current = setTimeout(async () => {
      try {
        setPatientSearchLoading(true);
        const res = await hospitalService.searchPatients(value.trim());
        setPatientSearchResults(res.data ?? []);
        setShowPatientDropdown(true);
      } catch {
        setPatientSearchResults([]);
        setShowPatientDropdown(true);
      } finally {
        setPatientSearchLoading(false);
      }
    }, 350);
  };

  const patientDropdownSelectableCount =
    showPatientDropdown ? patientSearchResults.length + 1 : 0;

  const openRegisterNewPatientFromSearch = () => {
    setNewPatientMode(true);
    setShowPatientDropdown(false);
    setPatientDropdownHighlight(-1);
    setNewPatientName(patientSearchTerm);
    setNewPatientAge('');
    setNewPatientGender('');
    setNewPatientPhone('');
    setNewPatientPhoneHint(null);
    setAppointmentSmsPhone('');
    setAppointmentSmsTouched(false);
  };

  const handlePatientSearchKeyDown = (e: React.KeyboardEvent) => {
    if (!showPatientDropdown || patientDropdownSelectableCount === 0) {
      if (e.key === 'Escape') {
        setShowPatientDropdown(false);
        setPatientDropdownHighlight(-1);
      }
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setPatientDropdownHighlight((h) =>
        h < patientDropdownSelectableCount - 1 ? h + 1 : h
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setPatientDropdownHighlight((h) => (h <= 0 ? -1 : h - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const idx = patientDropdownHighlight < 0 ? 0 : patientDropdownHighlight;
      if (idx < patientSearchResults.length) {
        selectPatient(patientSearchResults[idx]);
      } else {
        openRegisterNewPatientFromSearch();
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      setShowPatientDropdown(false);
      setPatientDropdownHighlight(-1);
    }
  };

  const selectPatient = (p: Patient) => {
    cachePatientInState(p.patientId, p);
    setSelectedPatient(p);
    setPatientSearchTerm('');
    setPatientSearchResults([]);
    setShowPatientDropdown(false);
    setPatientDropdownHighlight(-1);
    setNewPatientMode(false);
    if (!appointmentSmsTouched) {
      setAppointmentSmsPhone(patientRegistryPhone(p));
    }
    setCreateForm((prev) => ({ ...prev, patientId: p.patientId }));
  };

  const clearSelectedPatient = () => {
    setSelectedPatient(null);
    setCreateForm((prev) => ({ ...prev, patientId: '' }));
    setPatientSearchTerm('');
    setPatientSearchResults([]);
    setShowPatientDropdown(false);
    setPatientDropdownHighlight(-1);
    setNewPatientMode(false);
    setNewPatientName('');
    setNewPatientAge('');
    setNewPatientGender('');
    setNewPatientPhone('');
    setAppointmentSmsPhone('');
    setAppointmentSmsTouched(false);
  };

  const handleCreate = async () => {
    if (!createForm.resourceId?.trim()) {
      enqueueSnackbar('Resource (doctor) is required', { variant: 'warning' });
      return;
    }
    if (!createForm.appointmentDate?.trim() || !createForm.slotStart?.trim() || !createForm.slotEnd?.trim()) {
      enqueueSnackbar('Date and slot start/end are required', { variant: 'warning' });
      return;
    }

    let patientId = createForm.patientId?.trim();
    /** Patient record used for SMS + list cache (set after inline registration). */
    let bookingPatient: Patient | null =
      patientId && selectedPatient?.patientId === patientId
        ? selectedPatient
        : patientId && patientDetailsById[patientId]
          ? patientDetailsById[patientId]
          : null;

    // If registering a new patient, create them first
    if (newPatientMode) {
      if (!newPatientName.trim()) {
        enqueueSnackbar('Patient name is required', { variant: 'warning' });
        return;
      }
      if (!newPatientAge.trim() || isNaN(Number(newPatientAge)) || Number(newPatientAge) < 0) {
        enqueueSnackbar('Valid patient age is required', { variant: 'warning' });
        return;
      }
      if (Number(newPatientAge) > 150) {
        enqueueSnackbar('Age cannot exceed 150 years', { variant: 'warning' });
        return;
      }
      if (!newPatientPhone.trim()) {
        enqueueSnackbar('Phone number is required', { variant: 'warning' });
        return;
      }
      if (!isValidPhoneDigitLength(newPatientPhone)) {
        enqueueSnackbar(
          `Phone number must be at least ${MIN_PHONE_DIGITS_FOR_UNIQUENESS} digits`,
          { variant: 'warning' },
        );
        return;
      }
      if (!newPatientGender) {
        enqueueSnackbar('Gender is required', { variant: 'warning' });
        return;
      }
      try {
        setLoading(true);
        const orgId =
          typeof localStorage !== 'undefined' ? localStorage.getItem('currentOrganizationId') : null;
        const phoneDigits = digitsOnlyPhone(newPatientPhone.trim());
        const ageYears = Number(newPatientAge);
        const newPatientRes = await hospitalService.createPatient({
          fullName: newPatientName.trim(),
          dateOfBirth: dobFromAge(ageYears),
          ageYears,
          gender: newPatientGender,
          primaryPhone: phoneDigits,
          primaryCountry: 'Bangladesh',
          consentTextMessaging: true,
          ...(orgId ? { organizationId: orgId } : {}),
        });
        patientId = newPatientRes.data.patientId;
        const registeredPatient = newPatientRes.data;
        cachePatientInState(patientId, registeredPatient);
        bookingPatient = registeredPatient;
        setNewPatientPhoneHint(null);
        const smsForAppt = digitsOnlyPhone(appointmentSmsPhone.trim()) || phoneDigits;
        setAppointmentSmsTouched(!!smsForAppt);
        setAppointmentSmsPhone(smsForAppt);
        selectPatient(registeredPatient);
        enqueueSnackbar(`Patient "${newPatientRes.data.fullName}" registered (MRN: ${newPatientRes.data.mrn})`, { variant: 'success' });
        if (registeredPatient.identityCardStatus === 'FAILED') {
          enqueueSnackbar(
            `Identity card could not be issued automatically: ${registeredPatient.identityCardMessage || 'Unknown error'}. You can issue a card later from Hospital → Cards.`,
            { variant: 'warning' },
          );
        }
      } catch (err: unknown) {
        console.error('Failed to register patient', err);
        const ax = err as { response?: { status?: number; data?: DuplicatePatientResponse } };
        if (ax.response?.status === 409 && ax.response.data) {
          const dup = ax.response.data;
          const proceed = window.confirm(
            'Potential duplicate patient found. Register this family member anyway?',
          );
          if (!proceed) {
            setLoading(false);
            return;
          }
          try {
            const phoneDigits = digitsOnlyPhone(newPatientPhone.trim());
            const retryAge = Number(newPatientAge);
            const retry = await hospitalService.createPatient(
              {
                fullName: newPatientName.trim(),
                dateOfBirth: dobFromAge(retryAge),
                ageYears: retryAge,
                gender: newPatientGender,
                primaryPhone: phoneDigits,
                primaryCountry: 'Bangladesh',
                consentTextMessaging: true,
                ...(typeof localStorage !== 'undefined' && localStorage.getItem('currentOrganizationId')
                  ? { organizationId: localStorage.getItem('currentOrganizationId')! }
                  : {}),
              },
              { acknowledgeDuplicate: true },
            );
            patientId = retry.data.patientId;
            bookingPatient = retry.data;
            cachePatientInState(patientId!, retry.data);
            const smsForAppt = digitsOnlyPhone(appointmentSmsPhone.trim()) || phoneDigits;
            setAppointmentSmsTouched(!!smsForAppt);
            setAppointmentSmsPhone(smsForAppt);
            selectPatient(retry.data);
          } catch (retryErr: unknown) {
            enqueueSnackbar(ehrApiErrorMessage(retryErr, 'Failed to register patient'), { variant: 'error' });
            setLoading(false);
            return;
          }
        } else {
          enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to register patient'), { variant: 'error' });
        }
        setLoading(false);
        return;
      }
    }

    if (patientId && !bookingPatient) {
      bookingPatient =
        selectedPatient?.patientId === patientId
          ? selectedPatient
          : patientDetailsById[patientId] ?? null;
    }

    if (!patientId) {
      enqueueSnackbar('Please select or register a patient', { variant: 'warning' });
      setLoading(false);
      return;
    }

    const appointmentDate = normSlot(createForm.slotStart).slice(0, 10);
    if (patientBookedSameDay) {
      enqueueSnackbar('This patient already has an appointment with this doctor on this date.', {
        variant: 'warning',
      });
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const primaryId = createForm.resourceId.trim();
      const existingSameDay = await hospitalSchedulingService.getAppointments({
        patientId,
        resourceId: primaryId,
        fromDate: appointmentDate,
        toDate: appointmentDate,
        size: 10,
      });
      if (existingSameDay.content.some((a) => isActiveAppointmentStatus(a.status))) {
        setPatientBookedSameDay(true);
        enqueueSnackbar('This patient already has an appointment with this doctor on this date.', {
          variant: 'warning',
        });
        setLoading(false);
        return;
      }
      const additionalIds = (createForm.additionalResourceIds ?? []).filter((id) => id && id !== primaryId);
      const normalizedSlotStart = normSlot(createForm.slotStart);
      const normalizedSlotEnd = normSlot(createForm.slotEnd);
      const smsName = bookingPatient?.fullName?.trim() || '';
      const consentAllowsSms =
        !bookingPatient || bookingPatient.consentTextMessaging !== false;
      const phoneFromRegistry = bookingPatient && consentAllowsSms ? patientRegistryPhone(bookingPatient) : '';
      const smsPhone =
        digitsOnlyPhone(appointmentSmsPhone.trim()) ||
        phoneFromRegistry ||
        (newPatientMode ? digitsOnlyPhone(newPatientPhone.trim()) : '');
      if (bookingPatient?.consentTextMessaging === false) {
        enqueueSnackbar('Patient has not consented to SMS; no appointment SMS will be sent.', {
          variant: 'info',
        });
      } else if (!smsPhone) {
        enqueueSnackbar('No mobile number on file for this patient; no appointment SMS will be sent.', {
          variant: 'info',
        });
      }
      const payload: CreateAppointmentRequest = {
        patientId,
        resourceId: primaryId,
        clinicOrLocationId: createForm.clinicOrLocationId?.trim() || undefined,
        // Keep appointmentDate aligned with the slot timestamp sent to backend.
        appointmentDate: normalizedSlotStart.slice(0, 10),
        slotStart: normalizedSlotStart,
        slotEnd: normalizedSlotEnd,
        appointmentType: createForm.appointmentType || 'NEW',
        bookingChannel: 'FRONT_DESK',
        idempotencyKey: createForm.idempotencyKey?.trim() || undefined,
        overbookingOverrideReason: createForm.overbookingOverrideReason?.trim() || undefined,
        additionalResourceIds: additionalIds.length > 0 ? additionalIds : undefined,
        patientSmsDisplayName: smsName || undefined,
        patientSmsPhone: smsPhone || undefined,
        serialStartFrom: selectedDoctor?.serialStartFrom,
      };
      const created = await hospitalSchedulingService.createAppointment(payload);
      let phoneForCache = '';
      if (patientId) {
        const knownName = bookingPatient?.fullName?.trim() || '';
        if (knownName) {
          setPatientNamesById((p) => ({ ...p, [patientId]: knownName }));
        }
        phoneForCache =
          created.notificationPatientPhone?.trim() ||
          digitsOnlyPhone(appointmentSmsPhone.trim()) ||
          patientRegistryPhone(bookingPatient) ||
          '';
        if (phoneForCache || bookingPatient) {
          cachePatientInState(patientId, {
            ...(bookingPatient ?? patientDetailsByIdRef.current[patientId] ?? { patientId }),
            ...(phoneForCache ? { primaryPhone: phoneForCache } : {}),
          } as Patient);
        }
      }
      const createdForList =
        phoneForCache && !created.notificationPatientPhone?.trim()
          ? { ...created, notificationPatientPhone: phoneForCache }
          : created;
      enqueueSnackbar('Appointment booked', { variant: 'success' });
      const bookedDate =
        createdForList.appointmentDate?.trim() || normalizedSlotStart.slice(0, 10);
      const sameResourceDay =
        !!createForm.resourceId?.trim() &&
        createdForList.resourceId === createForm.resourceId.trim() &&
        !!bookedDate;
      const updatedDayQueue = [
        ...queueDayAppointments.filter((a) => a.id !== createdForList.id),
        createdForList,
      ].filter((a) => isActiveAppointmentStatus(a.status));
      if (sameResourceDay) {
        setQueueDayAppointments(updatedDayQueue);
        if (selectedDoctor) {
          const serialStart = selectedDoctor.serialStartFrom ?? 1;
          setNextSerialInfo({
            n: computeNextAppointmentSerial(updatedDayQueue, serialStart),
            date: bookedDate,
          });
        }
      }
      resetBookingForm(true, { keepAppointmentDate: true });
      setCreateDialogOpen(true);
      suppressAppointmentsLoadEffectRef.current = true;
      setAppointments((prev) => [createdForList, ...prev]);
      setTotalElements((n) => n + 1);
      void loadAppointments();
    } catch (err) {
      console.error('Failed to book appointment', err);
      const data = (err as any)?.response?.data;
      const message =
        data?.message ||
        data?.detail ||   // Spring Boot 3 ProblemDetail format
        data?.error ||
        (err as any)?.message ||
        'Failed to book appointment';
      console.error('Book appointment response:', data);
      enqueueSnackbar(String(message), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openView = async (a: AppointmentResponse) => {
    setViewDetail(null);
    setViewDialogOpen(true);
    setViewLoading(true);
    try {
      const detail = await hospitalSchedulingService.getAppointment(a.id);
      setViewDetail(detail);
      const pid = detail.patientId;
      if (pid && !(pid in patientNamesByIdRef.current)) {
        try {
          const { data } = await hospitalService.getPatient(pid);
          setPatientNamesById((p) => ({ ...p, [pid]: data?.fullName?.trim() || '' }));
        } catch {
          setPatientNamesById((p) => ({ ...p, [pid]: '' }));
        }
      }
    } catch (err) {
      console.error('Failed to load appointment detail', err);
      enqueueSnackbar('Failed to load appointment detail', { variant: 'error' });
      setViewDialogOpen(false);
    } finally {
      setViewLoading(false);
    }
  };

  const resetRescheduleDialog = () => {
    setRescheduleDialogOpen(false);
    setRescheduleIds([]);
    setReschedulePatientId('');
    setRescheduleResourceId('');
    setRescheduleDate('');
    setRescheduleSlots([]);
    setRescheduleSources([]);
    setRescheduleDayBlackedOut(false);
    setRescheduleAvailabilityError(false);
    setRescheduleForm({ newSlotStart: '', newSlotEnd: '', idempotencyKey: '' });
    setRescheduleAppointmentSmsPhone('');
  };

  const openReschedule = (a: AppointmentResponse) => {
    setRescheduleIds([a.id]);
    setRescheduleSources([{ id: a.id, slotStart: a.slotStart, slotEnd: a.slotEnd }]);
    setReschedulePatientId(a.patientId || '');
    setRescheduleResourceId(a.resourceId);
    setRescheduleDate(a.appointmentDate);
    setRescheduleForm({
      newSlotStart: '',
      newSlotEnd: '',
      idempotencyKey: '',
    });
    const snapshotPhone = a.notificationPatientPhone?.trim() || '';
    const registryPhone = patientRegistryPhone(patientDetailsById[a.patientId]);
    setRescheduleAppointmentSmsPhone(snapshotPhone || registryPhone);
    setRescheduleSlots([]);
    setRescheduleDayBlackedOut(false);
    setRescheduleAvailabilityError(false);
    setRescheduleDialogOpen(true);
  };

  useEffect(() => {
    if (!rescheduleDialogOpen || !rescheduleResourceId || !rescheduleDate) {
      setRescheduleSlots([]);
      setRescheduleDayBlackedOut(false);
      setRescheduleAvailabilityError(false);
      return;
    }
    let cancelled = false;
    setRescheduleSlotsLoading(true);
    setRescheduleAvailabilityError(false);
    hospitalSchedulingService
      .getAvailability({
        resourceId: rescheduleResourceId,
        fromDate: rescheduleDate,
        toDate: rescheduleDate,
      })
      .then((list) => {
        if (!cancelled) {
          const day = list?.[0];
          setRescheduleDayBlackedOut(!!day?.blackedOut);
          setRescheduleSlots(dedupeAvailabilitySlots(day?.slots ?? []));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRescheduleSlots([]);
          setRescheduleDayBlackedOut(false);
          setRescheduleAvailabilityError(true);
        }
      })
      .finally(() => {
        if (!cancelled) setRescheduleSlotsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [rescheduleDialogOpen, rescheduleResourceId, rescheduleDate]);

  const pickSlot = (slot: SlotAvailabilityDto) => {
    if (patientBookedSameDay) {
      enqueueSnackbar('This patient already has an appointment with this doctor on this date.', {
        variant: 'warning',
      });
      return;
    }
    if (parseISO(slot.start).getTime() <= now.getTime()) {
      enqueueSnackbar('This time slot has already passed', { variant: 'warning' });
      return;
    }
    if (slot.availableCount <= 0) {
      enqueueSnackbar('This slot is full', { variant: 'warning' });
      return;
    }
    // Keep full ISO offset datetime to satisfy CreateAppointmentRequest OffsetDateTime fields.
    setCreateForm((prev) => ({ ...prev, slotStart: slot.start, slotEnd: slot.end }));
  };

  const handleReschedule = async () => {
    if (rescheduleIds.length === 0) return;
    if (!rescheduleDate?.trim()) {
      enqueueSnackbar('New date is required', { variant: 'warning' });
      return;
    }
    if (!rescheduleForm.newSlotStart?.trim() || !rescheduleForm.newSlotEnd?.trim()) {
      enqueueSnackbar('New slot start and end are required', { variant: 'warning' });
      return;
    }
    const selectedRescheduleSlot = rescheduleSlots.find((slot) =>
      slotsEqual(slot.start, slot.end, rescheduleForm.newSlotStart, rescheduleForm.newSlotEnd)
    );
    if (!selectedRescheduleSlot) {
      enqueueSnackbar('Please select a valid available slot', { variant: 'warning' });
      return;
    }
    const slotDate = slotCalendarDate(selectedRescheduleSlot.start);
    if (!slotDate) {
      enqueueSnackbar('Invalid slot time', { variant: 'warning' });
      return;
    }
    if (rescheduleDate.trim() && slotDate !== rescheduleDate.trim()) {
      enqueueSnackbar('Selected slot does not match the chosen date; please pick again', { variant: 'warning' });
      return;
    }
    const isBulk = rescheduleIds.length > 1;
    const idsNeedingMove = rescheduleIds.filter((id) => {
      const src = rescheduleSources.find((s) => s.id === id);
      if (!src) return true;
      return !slotsEqual(src.slotStart, src.slotEnd, selectedRescheduleSlot.start, selectedRescheduleSlot.end);
    });
    if (idsNeedingMove.length === 0) {
      enqueueSnackbar(
        isBulk ? 'All selected appointments are already on this slot' : 'Appointment is already scheduled for this slot',
        { variant: 'info' }
      );
      return;
    }
    if (isBulk && selectedRescheduleSlot.availableCount < idsNeedingMove.length) {
      enqueueSnackbar(
        `This slot only has ${selectedRescheduleSlot.availableCount} opening(s), but ${idsNeedingMove.length} appointment(s) need to move`,
        { variant: 'warning' }
      );
      return;
    }
    // Single reschedule: availability may count the patient's own reservation; backend excludes it on reschedule.
    if (isBulk && selectedRescheduleSlot.availableCount <= 0) {
      enqueueSnackbar('Please select a valid available slot', { variant: 'warning' });
      return;
    }
    if (parseISO(selectedRescheduleSlot.start).getTime() <= now.getTime()) {
      enqueueSnackbar('Selected slot time has already passed', { variant: 'warning' });
      return;
    }
    try {
      setRescheduleSubmitting(true);
      let patientSmsDisplayName: string | undefined;
      let patientSmsPhone: string | undefined;
      if (!isBulk && reschedulePatientId.trim()) {
        try {
          const { data } = await hospitalService.getPatient(reschedulePatientId.trim());
          patientSmsDisplayName = data.fullName?.trim() || undefined;
          if (data.consentTextMessaging === false) {
            enqueueSnackbar('Patient has not consented to SMS; no reschedule confirmation SMS will be sent.', {
              variant: 'info',
            });
          }
        } catch {
          /* SMS enrichment optional */
        }
        const smsDigits = digitsOnlyPhone(rescheduleAppointmentSmsPhone.trim());
        if (smsDigits) {
          patientSmsPhone = smsDigits;
        }
      }
      const baseRequest = {
        newSlotStart: selectedRescheduleSlot.start,
        newSlotEnd: selectedRescheduleSlot.end,
        idempotencyKey: !isBulk ? rescheduleForm.idempotencyKey?.trim() || undefined : undefined,
        patientSmsDisplayName: !isBulk ? patientSmsDisplayName : undefined,
        patientSmsPhone: !isBulk ? patientSmsPhone : undefined,
        serialStartFrom: rescheduleDoctor?.serialStartFrom,
      };

      const idsForThisRun = [...idsNeedingMove];
      let succeeded = 0;
      let lastError: unknown;
      // Sequential calls avoid race when multiple appointments target the same slot capacity.
      for (const id of idsForThisRun) {
        try {
          await hospitalSchedulingService.rescheduleAppointment(id, baseRequest);
          succeeded += 1;
        } catch (err) {
          lastError = err;
        }
      }
      const rejected = idsForThisRun.length - succeeded;
      if (succeeded === 0) {
        enqueueSnackbar(ehrApiErrorMessage(lastError, 'Failed to reschedule'), { variant: 'error' });
        return;
      }
      if (rejected > 0) {
        enqueueSnackbar(
          `Rescheduled ${succeeded} of ${idsForThisRun.length}. ${ehrApiErrorMessage(lastError, 'Some failed')}`,
          { variant: 'warning' }
        );
      } else {
        enqueueSnackbar(isBulk ? `Rescheduled ${idsForThisRun.length} appointment(s)` : 'Appointment rescheduled', { variant: 'success' });
      }

      const newAppointmentDate = rescheduleDate.trim() || slotDate;
      setAppointments((prev) =>
        prev.map((a) =>
          idsForThisRun.includes(a.id)
            ? {
                ...a,
                appointmentDate: newAppointmentDate,
                slotStart: selectedRescheduleSlot.start,
                slotEnd: selectedRescheduleSlot.end,
              }
            : a
        )
      );

      let filtersForReload: AppointmentListFilters = appliedFilters;
      let dateFilterExpanded = false;
      if (newAppointmentDate) {
        const from = appliedFilters.fromDate.trim();
        const to = appliedFilters.toDate.trim();
        if (from && to && (newAppointmentDate < from || newAppointmentDate > to)) {
          filtersForReload = { ...appliedFilters, fromDate: newAppointmentDate, toDate: newAppointmentDate };
          dateFilterExpanded = true;
        }
      }

      resetRescheduleDialog();
      if (dateFilterExpanded) {
        suppressAppointmentsLoadEffectRef.current = true;
        setPage(0);
        setAppliedFilters(filtersForReload);
        setFilterDraft((prev) => ({ ...prev, fromDate: newAppointmentDate, toDate: newAppointmentDate }));
      }
      await loadAppointments(filtersForReload, dateFilterExpanded ? 0 : undefined);
      if (viewDialogOpen && viewDetail?.id && idsForThisRun.includes(viewDetail.id)) {
        setViewDialogOpen(false);
        setViewDetail(null);
      }
    } catch (err) {
      console.error('Failed to reschedule', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to reschedule'), { variant: 'error' });
    } finally {
      setRescheduleSubmitting(false);
    }
  };

  const openCancel = (a: AppointmentResponse) => {
    setCancelId(a.id);
    setCancelReason('');
    setCancelDialogOpen(true);
  };

  const handleCancel = async () => {
    if (!cancelId) return;
    try {
      setCancelSubmitting(true);
      await hospitalSchedulingService.cancelAppointment(cancelId, { reason: cancelReason.trim() || undefined });
      enqueueSnackbar('Appointment cancelled', { variant: 'success' });
      setCancelDialogOpen(false);
      setCancelId(null);
      setViewDialogOpen(false);
      setViewDetail(null);
      loadAppointments();
    } catch (err) {
      console.error('Failed to cancel', err);
      enqueueSnackbar('Failed to cancel', { variant: 'error' });
    } finally {
      setCancelSubmitting(false);
    }
  };

  const openDelete = (a: AppointmentResponse) => {
    setDeleteId(a.id);
    setDeleteDialogOpen(true);
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      setDeleteSubmitting(true);
      await hospitalSchedulingService.deleteAppointment(deleteId);
      enqueueSnackbar('Appointment deleted', { variant: 'success' });
      setDeleteDialogOpen(false);
      setDeleteId(null);
      setViewDialogOpen(false);
      setViewDetail(null);
      loadAppointments();
    } catch (err) {
      console.error('Failed to delete appointment', err);
      enqueueSnackbar('Failed to delete appointment', { variant: 'error' });
    } finally {
      setDeleteSubmitting(false);
    }
  };

  const handleCheckIn = async (a: AppointmentResponse) => {
    try {
      setLoading(true);
      await hospitalSchedulingService.checkInAppointment(a.id);
      enqueueSnackbar('Checked in', { variant: 'success' });
      loadAppointments();
      if (viewDialogOpen && viewDetail?.id === a.id) {
        const detail = await hospitalSchedulingService.getAppointment(a.id);
        setViewDetail(detail);
      }
    } catch (err) {
      console.error('Failed to check in', err);
      enqueueSnackbar('Failed to check in', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleNoShow = async (a: AppointmentResponse) => {
    try {
      setLoading(true);
      await hospitalSchedulingService.noShowAppointment(a.id);
      enqueueSnackbar('Marked no-show', { variant: 'success' });
      loadAppointments();
      if (viewDialogOpen && viewDetail?.id === a.id) {
        setViewDialogOpen(false);
        setViewDetail(null);
      }
    } catch (err) {
      console.error('Failed to mark no-show', err);
      enqueueSnackbar('Failed to mark no-show', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleComplete = async (a: AppointmentResponse) => {
    try {
      setLoading(true);
      await hospitalSchedulingService.completeAppointment(a.id);
      enqueueSnackbar('Marked complete', { variant: 'success' });
      loadAppointments();
      if (viewDialogOpen && viewDetail?.id === a.id) {
        const detail = await hospitalSchedulingService.getAppointment(a.id);
        setViewDetail(detail);
      }
    } catch (err) {
      console.error('Failed to complete appointment', err);
      enqueueSnackbar('Failed to mark complete', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const resourceMap = new Map(resources.map((r) => [r.id, r]));
  const resourceDisplayLabel = (resourceId: string) => {
    const r = resourceMap.get(resourceId);
    return r ? schedulingResourceDoctorLabel(r, doctorDepartmentById) : resourceId;
  };
  const formatDateTime = (s: string | null | undefined) => {
    if (!s?.trim()) return '—';
    try {
      const dt = parseISO(s);
      if (isNaN(dt.getTime())) return s.slice(0, 19).replace('T', ' ');
      return format(dt, 'yyyy-MM-dd h:mm a');
    } catch {
      return s.slice(0, 19).replace('T', ' ');
    }
  };
  const formatTimeOnly = (s: string | null | undefined) => {
    if (!s?.trim()) return '—';
    try {
      const dt = parseISO(s);
      if (isNaN(dt.getTime())) return s;
      return format(dt, 'h:mm a');
    } catch {
      return s;
    }
  };

  const canAct = (status: string) =>
    status !== 'CANCELLED' && status !== 'COMPLETED';

  const handleCancelSelected = async () => {
    const ids = [...selectedAppointmentIds];
    if (ids.length === 0) return;
    const selectedApps = appointments.filter((a) => ids.includes(a.id));
    const cancellable = selectedApps.filter((a) => {
      const st = (a.status ?? '').toUpperCase();
      return st === 'CONFIRMED' || st === 'CHECKED_IN';
    });
    const skipped = selectedApps.length - cancellable.length;
    if (cancellable.length === 0) {
      enqueueSnackbar('Bulk cancel is only allowed for CONFIRMED / CHECKED_IN appointments', { variant: 'warning' });
      return;
    }
    if (!window.confirm(`Cancel ${cancellable.length} selected appointment(s)?${skipped > 0 ? ` (${skipped} will be skipped due to status)` : ''}`)) return;

    try {
      setLoading(true);
      const results = await Promise.allSettled(
        cancellable.map((a) => hospitalSchedulingService.cancelAppointment(a.id, { reason: 'Cancelled (bulk)' }))
      );
      const rejected = results.filter((r) => r.status === 'rejected').length;
      if (rejected > 0) {
        enqueueSnackbar(
          `Cancelled ${cancellable.length - rejected} / ${cancellable.length} appointment(s)${skipped > 0 ? ` (${skipped} skipped)` : ''}`,
          { variant: 'warning' }
        );
      } else {
        enqueueSnackbar(
          `Cancelled ${cancellable.length} appointment(s)${skipped > 0 ? ` (${skipped} skipped)` : ''}`,
          { variant: 'success' }
        );
      }
      setSelectedAppointmentIds(new Set());
      await loadAppointments();
    } catch (err) {
      console.error('Bulk cancel failed', err);
      enqueueSnackbar('Failed to cancel selected appointments', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRescheduleSelected = () => {
    const ids = [...selectedAppointmentIds];
    if (ids.length === 0) return;

    const selectedApps = appointments.filter((a) => ids.includes(a.id));
    if (selectedApps.length === 0) return;

    const first = selectedApps[0];
    const allSameResourceAndDate = selectedApps.every(
      (a) => a.resourceId === first.resourceId && a.appointmentDate === first.appointmentDate
    );
    if (!allSameResourceAndDate) {
      enqueueSnackbar('Bulk reschedule requires appointments from the same doctor (resource) and date', { variant: 'warning' });
      return;
    }

    const invalid = selectedApps.filter((a) => {
      const st = (a.status ?? '').toUpperCase();
      return st !== 'CONFIRMED';
    });
    if (invalid.length > 0) {
      enqueueSnackbar('Bulk reschedule is only allowed for CONFIRMED appointments', { variant: 'warning' });
      return;
    }

    setRescheduleIds(ids);
    setRescheduleSources(
      selectedApps.map((a) => ({ id: a.id, slotStart: a.slotStart, slotEnd: a.slotEnd }))
    );
    setReschedulePatientId(first.patientId || '');
    setRescheduleResourceId(first.resourceId);
    setRescheduleDate(first.appointmentDate);
    setRescheduleForm({
      newSlotStart: '',
      newSlotEnd: '',
      idempotencyKey: '',
    });
    setRescheduleSlots([]);
    setRescheduleDayBlackedOut(false);
    setRescheduleAvailabilityError(false);
    setRescheduleDialogOpen(true);
  };

  const handleDownloadList = () => {
    const showDoctorCol = !appliedFilters.resourceId.trim();
    const headers = showDoctorCol
      ? ['SL', 'Patient', 'Phone', 'Age', 'Doctor', 'Date', 'Time', 'Type', 'Status']
      : ['SL', 'Patient', 'Phone', 'Age', 'Date', 'Time', 'Type', 'Status'];
    const rows = appointments.map((a, idx) => {
      const details = patientDetailsById[a.patientId];
      const patientLabel = details?.fullName?.trim() || patientDisplayLabel(a.patientId);
      const phoneLabel = appointmentPhone(a) || '';
      const ageLabel = details?.dateOfBirth ? formatAge(details.dateOfBirth) : '';
      const slotLabel = formatTimeOnly(a.slotStart);
      const base = [
        String(page * size + idx + 1),
        patientLabel,
        phoneLabel,
        ageLabel,
        ...(showDoctorCol ? [resourceDisplayLabel(a.resourceId)] : []),
        a.appointmentDate ?? '',
        slotLabel,
        a.appointmentType ?? '',
        a.status ?? '',
      ];
      return base;
    });
    const csv = [headers, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'Appointment_list.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  const handlePrintList = () => {
    const printWin = window.open('', '_blank', 'width=1000,height=700');
    if (!printWin) return;
    const doctorHeadingHtml = (() => {
      if (appointments.length === 0) return '';
      const rid = appliedFilters.resourceId.trim();
      if (rid) {
        const r = resourceMap.get(rid);
        const name = r ? schedulingResourceDoctorLabel(r, doctorDepartmentById) : rid;
        return `<p style="margin:8px 0 6px 0;font-size:15px"><strong>Doctor:</strong> ${escapeHtmlForPrint(name)}</p>`;
      }
      return '';
    })();
    const doctorWisePrint = !!appliedFilters.resourceId.trim();
    const rowsHtml = appointments.map((a, idx) => {
      const details = patientDetailsById[a.patientId];
      const patientLabel = escapeHtmlForPrint(details?.fullName?.trim() || patientDisplayLabel(a.patientId));
      const phoneLabel = escapeHtmlForPrint(appointmentPhone(a) || '—');
      const ageLabel = escapeHtmlForPrint(
        details?.dateOfBirth ? formatAge(details.dateOfBirth) : '—',
      );
      const slotDisplay = formatTimeOnly(a.slotStart);
      return `<tr>
        <td>${page * size + idx + 1}</td>
        <td>${patientLabel}</td>
        <td>${phoneLabel}</td>
        <td>${ageLabel}</td>
        ${doctorWisePrint ? '' : `<td>${escapeHtmlForPrint(resourceDisplayLabel(a.resourceId))}</td>`}
        <td>${a.appointmentDate ?? '—'}</td>
        <td>${slotDisplay}</td>
        <td>${a.appointmentType}</td>
        <td>${a.status}</td>
      </tr>`;
    }).join('');
    const colCount = doctorWisePrint ? 8 : 9;
    printWin.document.write(`
      <html>
        <head><title>Appointment List</title></head>
        <body>
          <h2>Appointment List</h2>
          ${doctorHeadingHtml}
          <p>Printed on: ${new Date().toLocaleString()}</p>
          <table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;font-family:Arial,sans-serif;font-size:12px">
            <thead>
              <tr><th>SL</th><th>Patient</th><th>Phone</th><th>Age</th>${doctorWisePrint ? '' : '<th>Doctor</th>'}<th>Date</th><th>Time</th><th>Type</th><th>Status</th></tr>
            </thead>
            <tbody>${rowsHtml || `<tr><td colspan="${colCount}" style="text-align:center">No appointments found.</td></tr>`}</tbody>
          </table>
        </body>
      </html>
    `);
    printWin.document.close();
    printWin.focus();
    printWin.print();
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Appointments
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateDialogOpen(true)}
            disabled={loading || !canBookAppointment}
          >
            Book appointment
          </Button>
        </Box>
      </Box>

      {/* Book appointment section */}
      {createDialogOpen && (
        <Card ref={bookingSectionRef} sx={{ mb: 3 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ px: 2.5, py: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography variant="h6">Book Appointment</Typography>
              <Typography variant="body2" color="text.secondary">
                Complete steps below to create an appointment.
              </Typography>
            </Box>

          {/* ─── Step 1: Doctor ─── */}
          <Box sx={{ p: 2.5 }}>
            <Box display="flex" alignItems="center" gap={1} mb={1.5}>
              <Avatar sx={{ width: 24, height: 24, fontSize: 12, bgcolor: (selectedDoctor && !doctorResolving) ? 'success.main' : 'primary.main' }}>
                {(selectedDoctor && !doctorResolving) ? <CheckCircleIcon sx={{ fontSize: 14 }} /> : 1}
              </Avatar>
              <Typography variant="subtitle2" fontWeight={600}>Select Doctor</Typography>
              {doctorResolving && <CircularProgress size={16} sx={{ ml: 1 }} />}
            </Box>
            {selectedDoctor && !doctorResolving ? (
              <Box display="flex" flexDirection="column" gap={1}>
                <Box display="flex" alignItems="center" gap={1} p={1.5}
                  sx={{ borderRadius: 1, bgcolor: 'action.selected', border: '1px solid', borderColor: 'divider' }}>
                  <Box flex={1}>
                    <Typography variant="body2" fontWeight={600}>
                      {doctorNameWithDepartment(selectedDoctor.doctorName, selectedDoctor.departmentName)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {[selectedDoctor.speciality, selectedDoctor.degree].filter(Boolean).join(' · ')}
                    </Typography>
                  </Box>
                  <Button size="small" onClick={() => handleSelectDoctor(null)} startIcon={<CloseIcon fontSize="small" />}>Change</Button>
                </Box>
                <Box
                  sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 1.5,
                    alignItems: 'flex-start',
                    justifyContent: 'flex-start',
                    p: 1.25,
                    borderRadius: 1,
                    bgcolor: 'background.default',
                    border: '1px solid',
                    borderColor: 'divider',
                  }}
                >
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: { xs: 1, sm: 1.25 }, flex: '1 1 260px', minWidth: 0 }}>
                    {[
                      {
                        label: createForm.slotStart ? 'Next SL No (this slot)' : 'Next SL No (day)',
                        value:
                          nextSerialInfo != null
                            ? `${nextSerialInfo.n} (${format(parseISO(nextSerialInfo.date + 'T12:00:00'), 'MMM d, yyyy')})`
                            : '—',
                        loading: nextSerialLoading,
                      },
                      { label: 'Chamber No', value: selectedDoctor.chamberRoom?.trim() || '—' },
                      {
                        label: 'First visit fee',
                        value: selectedDoctor.visitFeeNew != null ? String(selectedDoctor.visitFeeNew) : '—',
                      },
                      {
                        label: 'Second visit fee',
                        value: selectedDoctor.visitFeeOld != null ? String(selectedDoctor.visitFeeOld) : '—',
                      },
                    ].map((row) => (
                      <Box key={row.label} sx={{ display: 'inline-flex', alignItems: 'baseline', gap: 0.35, minWidth: 118 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                          {row.label}
                          {row.loading ? ' …' : ':'}
                        </Typography>
                        <Typography variant="caption" fontWeight={600}>
                          {row.value}
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                  <Box
                    sx={{
                      flex: '1 1 200px',
                      minWidth: 180,
                      pl: { xs: 0, sm: 1 },
                      borderLeft: { xs: 'none', sm: '1px solid' },
                      borderColor: { sm: 'divider' },
                    }}
                  >
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" gutterBottom>
                      Doctor schedule
                    </Typography>
                    {slotDaySummary ? (
                      <Typography variant="caption" component="div" sx={{ lineHeight: 1.45 }}>
                        {slotDaySummary.rangeLabel}
                        <br />
                        {slotDaySummary.minutesPerSlot} min/appointment · {slotDaySummary.totalSlots} slot
                        {slotDaySummary.totalSlots !== 1 ? 's' : ''}
                      </Typography>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        Select a date to view schedule times.
                      </Typography>
                    )}
                  </Box>
                </Box>
              </Box>
            ) : (
              <Autocomplete
                options={doctors}
                value={selectedDoctor}
                onChange={(_, doctor) => handleSelectDoctor(doctor)}
                getOptionLabel={(d) => doctorNameWithDepartment(d.doctorName, d.departmentName)}
                filterOptions={(opts, { inputValue }) => {
                  const q = inputValue.toLowerCase();
                  return opts.filter((d) =>
                    d.doctorName.toLowerCase().includes(q) ||
                    (d.speciality ?? '').toLowerCase().includes(q) ||
                    (d.departmentName ?? '').toLowerCase().includes(q)
                  );
                }}
                renderOption={(props, d) => (
                  <li {...props} key={d.doctorId}>
                    <Box>
                      <Typography variant="body2" fontWeight={600}>
                        {doctorNameWithDepartment(d.doctorName, d.departmentName)}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {[d.speciality, d.degree].filter(Boolean).join(' · ')}
                      </Typography>
                    </Box>
                  </li>
                )}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Search doctor"
                    size="small"
                    placeholder="Type name, specialty or department…"
                    autoFocus
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {doctorResolving && <CircularProgress size={16} />}
                          {params.InputProps.endAdornment}
                        </>
                      ),
                    }}
                  />
                )}
                isOptionEqualToValue={(a, b) => a.doctorId === b.doctorId}
                noOptionsText="No doctors found"
                fullWidth
              />
            )}
          </Box>

          {selectedDoctor && !doctorResolving && (
            <>
              <Divider />

              {/* ─── Steps 2 + 3: Date & Slots side-by-side ─── */}
              <Box sx={{ p: 2.5 }}>
                {/* Section header */}
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <Avatar sx={{ width: 24, height: 24, fontSize: 12, bgcolor: createForm.appointmentDate ? 'success.main' : 'primary.main' }}>
                    {createForm.appointmentDate ? <CheckCircleIcon sx={{ fontSize: 14 }} /> : 2}
                  </Avatar>
                  <Typography variant="subtitle2" fontWeight={600}>Select Date</Typography>
                  {monthAvailabilityLoading && <CircularProgress size={14} />}
                </Box>

                <Box display="flex" gap={2.5} alignItems="flex-start">
                  {/* ── Left: Calendar ── */}
                  <Box sx={{ width: 270, flexShrink: 0 }}>
                    {/* Month navigation */}
                    <Box display="flex" alignItems="center" justifyContent="space-between" mb={0.5}>
                      <IconButton
                        size="small"
                        onClick={() => setCalendarMonth((m) => subMonths(m, 1))}
                        disabled={isBefore(endOfMonth(subMonths(calendarMonth, 1)), startOfDay(new Date()))}
                      >
                        <ChevronLeftIcon fontSize="small" />
                      </IconButton>
                      <Typography variant="body2" fontWeight={600}>
                        {format(calendarMonth, 'MMMM yyyy')}
                      </Typography>
                      <IconButton
                        size="small"
                        onClick={() => setCalendarMonth((m) => addMonths(m, 1))}
                        disabled={!!bookingWindowEnd && isBefore(bookingWindowEnd, startOfMonth(addMonths(calendarMonth, 1)))}
                      >
                        <ChevronRightIcon fontSize="small" />
                      </IconButton>
                    </Box>
                    {bookingWindowEnd && (
                      <Typography variant="caption" color="text.secondary" display="block" mb={0.75}>
                        Up to {format(bookingWindowEnd, 'MMM d')} ({selectedDoctor?.numberOfDaysCanAppointment}d limit)
                      </Typography>
                    )}

                    {/* Day-of-week header */}
                    <Box display="grid" sx={{ gridTemplateColumns: 'repeat(7, 1fr)', mb: 0.5 }}>
                      {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map((d) => (
                        <Typography key={d} variant="caption" align="center" sx={{ fontWeight: 700, color: 'text.secondary', fontSize: 11 }}>
                          {d}
                        </Typography>
                      ))}
                    </Box>

                    {/* Calendar grid */}
                    <Box
                      display="grid"
                      sx={{
                        gridTemplateColumns: 'repeat(7, 1fr)',
                        gap: '2px',
                        opacity: monthAvailabilityLoading ? 0.5 : 1,
                        transition: 'opacity 0.2s',
                      }}
                    >
                      {eachDayOfInterval({
                        start: startOfWeek(startOfMonth(calendarMonth)),
                        end: endOfWeek(endOfMonth(calendarMonth)),
                      }).map((day) => {
                        const dateStr = format(day, 'yyyy-MM-dd');
                        const isPast = isBefore(day, todayStart);
                        const isCurrentMonth = isSameMonth(day, calendarMonth);
                        const withinWindow = isWithinBookingWindow(day);
                        const isTodayDate = isToday(day);
                        const avail = monthAvailability.get(dateStr);
                        const hasSlots = (avail?.slotCount ?? 0) > 0;
                        const isBlackedOut = avail?.blackedOut === true;
                        const isSelected = createForm.appointmentDate === dateStr;
                        const isClickable = isCurrentMonth && !isPast && withinWindow && !isBlackedOut && hasSlots;
                        const showsAsAvailableSlot =
                          hasSlots && isCurrentMonth && !isPast && withinWindow && !isBlackedOut;

                        return (
                          <Box
                            key={dateStr}
                            onClick={() => {
                              if (!isClickable) return;
                              setCreateForm((prev) => ({ ...prev, appointmentDate: dateStr, slotStart: '', slotEnd: '' }));
                            }}
                            sx={{
                              display: 'flex',
                              flexDirection: 'column',
                              alignItems: 'center',
                              justifyContent: 'center',
                              py: 0.6,
                              borderRadius: 1,
                              cursor: isClickable ? 'pointer' : 'default',
                              opacity: !isCurrentMonth ? 0.18 : (isPast || !withinWindow) ? 0.35 : 1,
                              bgcolor: isSelected
                                ? 'primary.main'
                                : isBlackedOut && isCurrentMonth && !isPast
                                ? 'rgba(211,47,47,0.14)'
                                : showsAsAvailableSlot
                                ? 'rgba(46,125,50,0.14)'
                                : 'transparent',
                              border: '1px solid',
                              borderColor: isSelected
                                ? 'primary.main'
                                : isTodayDate
                                ? 'primary.light'
                                : 'transparent',
                              ...(showsAsAvailableSlot && !isSelected
                                ? { boxShadow: `inset 0 -3px 0 0 ${theme.palette.success.dark}` }
                                : {}),
                              '&:hover': isClickable
                                ? {
                                    bgcolor: isSelected
                                      ? 'primary.dark'
                                      : isBlackedOut
                                      ? 'rgba(211,47,47,0.2)'
                                      : hasSlots
                                      ? 'rgba(46,125,50,0.22)'
                                      : 'action.hover',
                                  }
                                : {},
                              transition: 'background-color 0.12s',
                            }}
                          >
                            <Typography
                              variant="caption"
                              sx={{
                                lineHeight: 1.2,
                                fontSize: 12,
                                fontWeight: isSelected || isTodayDate ? 700 : 400,
                                color: isSelected
                                  ? 'primary.contrastText'
                                  : isBlackedOut && isCurrentMonth
                                  ? 'error.main'
                                  : !hasSlots || isPast || !isCurrentMonth || !withinWindow
                                  ? 'text.disabled'
                                  : 'success.dark',
                              }}
                            >
                              {format(day, 'd')}
                            </Typography>
                            {isCurrentMonth && !isPast && isBlackedOut && (
                              <Typography variant="caption" sx={{ fontSize: 8, lineHeight: 1, color: 'error.main', mt: '1px' }}>
                                off
                              </Typography>
                            )}
                          </Box>
                        );
                      })}
                    </Box>

                    {/* Legend */}
                    {!monthFetchError && (
                      <Box display="flex" flexWrap="wrap" gap={1.5} mt={1}>
                        <Box display="flex" alignItems="center" gap={0.5}>
                          <Box
                            sx={(theme) => ({
                              width: 18,
                              height: 10,
                              borderRadius: 0.5,
                              bgcolor: 'rgba(46,125,50,0.14)',
                              boxShadow: `inset 0 -2px 0 0 ${theme.palette.success.dark}`,
                            })}
                          />
                          <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>Available</Typography>
                        </Box>
                        <Box display="flex" alignItems="center" gap={0.5}>
                          <Box sx={{ width: 18, height: 10, borderRadius: 0.5, bgcolor: 'rgba(211,47,47,0.14)' }} />
                          <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>Doctor off</Typography>
                        </Box>
                        <Box display="flex" alignItems="center" gap={0.5}>
                          <Box sx={{ width: 18, height: 10, borderRadius: 0.5, border: '1px dashed', borderColor: 'divider' }} />
                          <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>No schedule</Typography>
                        </Box>
                      </Box>
                    )}

                    {selectedDoctor && (
                      <Box mt={1.5} p={1.25} sx={{ bgcolor: 'action.hover', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
                        <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" gutterBottom>
                          Availability notes
                        </Typography>
                        {selectedDoctor.availabilityStatus === 'NOT_AVAILABLE' && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            The doctor hasn&apos;t come yet.
                          </Typography>
                        )}
                        {mergeOffDayRanges(selectedDoctor.offDays ?? []).map((r) => (
                          <Typography key={`${r.start}-${r.end}`} variant="caption" color="text.secondary" display="block">
                            {r.start === r.end
                              ? `On leave on ${format(parseISO(r.start + 'T12:00:00'), 'yyyy-MM-dd')}.`
                              : `On leave from ${format(parseISO(r.start + 'T12:00:00'), 'yyyy-MM-dd')} to ${format(parseISO(r.end + 'T12:00:00'), 'yyyy-MM-dd')}.`}
                          </Typography>
                        ))}
                        {selectedDoctor.availabilityStatus !== 'NOT_AVAILABLE' &&
                          (selectedDoctor.offDays?.length ?? 0) === 0 && (
                            <Typography variant="caption" color="text.secondary" display="block">
                              No extra leave dates on file.
                            </Typography>
                          )}
                      </Box>
                    )}

                    {/* Error state */}
                    {monthFetchError && (
                      <Box mt={1} p={1.5} sx={{ bgcolor: 'error.50', border: '1px solid', borderColor: 'error.light', borderRadius: 1 }}>
                        <Typography variant="body2" color="error.main" fontWeight={600} gutterBottom>
                          Could not load availability
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                          Check that the scheduling service is running.
                        </Typography>
                        <Button size="small" variant="outlined" color="error" onClick={() => setCalendarMonth((m) => new Date(m))}>
                          Retry
                        </Button>
                      </Box>
                    )}

                    {/* No-schedule empty state */}
                    {!monthAvailabilityLoading && !monthFetchError && monthFetchDone && (() => {
                      const hasAnySlot = Array.from(monthAvailability.values()).some((v) => v.slotCount > 0);
                      return !hasAnySlot ? (
                        <Box mt={1} p={1.5} sx={{ bgcolor: 'warning.50', border: '1px solid', borderColor: 'warning.light', borderRadius: 1 }}>
                          <Typography variant="body2" color="warning.dark" fontWeight={600} gutterBottom>
                            No slots this month
                          </Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            Go to <strong>Doctor Settings → Appointment Slots</strong> and re-save.
                          </Typography>
                        </Box>
                      ) : null;
                    })()}
                  </Box>

                  {/* ── Right: Slot cards ── */}
                  <Box flex={1} minWidth={0}>
                    <Box display="flex" alignItems="center" gap={1} mb={1}>
                      <Avatar sx={{ width: 24, height: 24, fontSize: 12, bgcolor: createForm.slotStart ? 'success.main' : 'primary.main' }}>
                        {createForm.slotStart ? <CheckCircleIcon sx={{ fontSize: 14 }} /> : 3}
                      </Avatar>
                      <Typography variant="subtitle2" fontWeight={600}>Select Appointment Slot</Typography>
                      {availabilityLoading && createForm.appointmentDate && <CircularProgress size={14} />}
                      {selectedDoctor &&
                        (selectedDoctor.serialStartFrom ?? 1) >= 4 &&
                        (selectedDoctor.serialStartFrom ?? 1) <= 6 && (
                          <Typography variant="caption" color="warning.main" sx={{ ml: 1 }}>
                            First {(selectedDoctor.serialStartFrom ?? 1) - 1} slot
                            {(selectedDoctor.serialStartFrom ?? 1) - 1 !== 1 ? 's are' : ' is'} reserved
                          </Typography>
                        )}
                    </Box>
                    {!createForm.appointmentDate ? (
                      <Box
                        display="flex"
                        alignItems="center"
                        justifyContent="center"
                        sx={{
                          height: 260,
                          border: '2px dashed',
                          borderColor: 'divider',
                          borderRadius: 2,
                          color: 'text.disabled',
                        }}
                      >
                        <Typography variant="body2" align="center">
                          Select a date on the calendar<br />to see available appointments
                        </Typography>
                      </Box>
                    ) : availabilityLoading ? (
                          <Box display="flex" alignItems="center" justifyContent="center" sx={{ height: 220 }}>
                            <CircularProgress size={32} />
                          </Box>
                        ) : dedupedAvailabilitySlots.length > 0 ? (
                          <>
                            <Typography variant="body2" color="text.secondary" mb={1} component="div">
                              {slotDaySummary ? (
                                <>
                                  <strong>{format(new Date(createForm.appointmentDate + 'T00:00:00'), 'EEEE, MMMM d')}</strong>
                                  {' — '}
                                  <strong>Available:</strong> {slotDaySummary.rangeLabel}
                                  {' | '}
                                  <strong>Total:</strong> {String(slotDaySummary.totalSlots).padStart(2, '0')} slot
                                  {slotDaySummary.totalSlots !== 1 ? 's' : ''}
                                  {' | '}
                                  {slotDaySummary.minutesPerSlot} min/appointment
                                </>
                              ) : (
                                <>
                                  <strong>{format(new Date(createForm.appointmentDate + 'T00:00:00'), 'EEEE, MMMM d')}</strong>
                                  {' '}— {slotDaySummary?.totalSlots ?? dedupedAvailabilitySlots.length} slot
                                  {(slotDaySummary?.totalSlots ?? dedupedAvailabilitySlots.length) !== 1 ? 's' : ''}
                                </>
                              )}
                            </Typography>
                            <Box
                              sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 0.75,
                                maxHeight: 340,
                                overflowY: 'auto',
                                pr: 0.5,
                              }}
                            >
                              {dedupedAvailabilitySlots.map((slot, idx) => {
                                const selected = slotInstantEqual(createForm.slotStart, slot.start);
                                const apptNumber = projectedSerialForSlot(
                                  queueDayAppointments,
                                  selectedDoctor.serialStartFrom,
                                  slot.start,
                                  slot.end,
                                );
                                const isTimePassed = parseISO(slot.start).getTime() <= now.getTime();
                                const isSelectable =
                                  slot.availableCount > 0 && !isTimePassed && !patientBookedSameDay;
                                const slotStartDt = parseISO(slot.start);
                                const slotEndDt = parseISO(slot.end);
                                const startTime =
                                  !isNaN(slotStartDt.getTime()) ? format(slotStartDt, 'h:mm a') : (slot.start?.slice(11, 16) ?? '?');
                                const endTime =
                                  !isNaN(slotEndDt.getTime()) ? format(slotEndDt, 'h:mm a') : (slot.end?.slice(11, 16) ?? '?');
                                const spotsLeft = slot.availableCount;
                                return (
                                  <Box
                                    key={`${slot.start}-${idx}`}
                                    onClick={() => (isSelectable ? pickSlot(slot) : undefined)}
                                    sx={{
                                      display: 'flex',
                                      alignItems: 'center',
                                      gap: 1.5,
                                      px: 1.5,
                                      py: 1,
                                      borderRadius: 1.5,
                                      border: '1px solid',
                                      borderColor: selected ? 'primary.main' : 'divider',
                                      bgcolor: selected
                                        ? 'primary.main'
                                        : isSelectable
                                        ? 'background.paper'
                                        : 'action.disabledBackground',
                                      cursor: isSelectable ? 'pointer' : 'default',
                                      opacity: selected ? 1 : isSelectable ? 1 : 0.5,
                                      '&:hover': isSelectable ? {
                                        bgcolor: selected ? 'primary.dark' : 'action.hover',
                                        borderColor: selected ? 'primary.dark' : 'primary.light',
                                      } : {},
                                      transition: 'all 0.12s',
                                    }}
                                  >
                                    <Box
                                      sx={{
                                        width: 28,
                                        height: 28,
                                        borderRadius: '50%',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        bgcolor: selected ? 'rgba(255,255,255,0.2)' : 'action.selected',
                                        flexShrink: 0,
                                      }}
                                    >
                                      <Typography
                                        variant="caption"
                                        sx={{ fontWeight: 700, fontSize: 11, color: selected ? 'primary.contrastText' : 'text.secondary' }}
                                      >
                                        #{apptNumber}
                                      </Typography>
                                    </Box>
                                    <Box flex={1} minWidth={0}>
                                      <Typography
                                        variant="body2"
                                        fontWeight={600}
                                        sx={{ color: selected ? 'primary.contrastText' : 'text.primary' }}
                                      >
                                        {startTime} – {endTime}
                                      </Typography>
                                      <Typography
                                        variant="caption"
                                        sx={{ color: selected ? 'rgba(255,255,255,0.8)' : 'text.secondary' }}
                                      >
                                        Est. appointment at {startTime}
                                      </Typography>
                                    </Box>
                                    <Chip
                                      label={isTimePassed ? 'Passed' : spotsLeft > 0 ? `${spotsLeft} spot${spotsLeft !== 1 ? 's' : ''}` : 'Full'}
                                      size="small"
                                      color={isSelectable ? (selected ? undefined : 'success') : 'default'}
                                      variant={selected ? 'outlined' : 'filled'}
                                      sx={{
                                        fontSize: 11,
                                        height: 22,
                                        ...(selected && { color: 'primary.contrastText', borderColor: 'rgba(255,255,255,0.5)' }),
                                      }}
                                    />
                                    {selected && (
                                      <CheckCircleIcon sx={{ fontSize: 18, color: 'primary.contrastText', flexShrink: 0 }} />
                                    )}
                                  </Box>
                                );
                              })}
                            </Box>
                          </>
                        ) : (
                          <Box
                            display="flex"
                            alignItems="center"
                            justifyContent="center"
                            sx={{
                              height: 220,
                              border: '2px dashed',
                              borderColor: 'divider',
                              borderRadius: 2,
                              color: 'text.disabled',
                            }}
                          >
                            <Typography variant="body2" align="center">
                              No available slots for<br />
                              <strong style={{ color: 'inherit' }}>
                                {format(new Date(createForm.appointmentDate + 'T00:00:00'), 'MMMM d')}
                              </strong>
                              <br />Try another date.
                            </Typography>
                          </Box>
                        )}
                  </Box>
                </Box>
              </Box>
            </>
          )}

          {createForm.slotStart && (
            <>
              <Divider />

              {/* ─── Step 4: Patient ─── */}
              <Box sx={{ p: 2.5 }}>
                <Box display="flex" alignItems="center" gap={1} mb={1.5}>
                  <Avatar sx={{ width: 24, height: 24, fontSize: 12, bgcolor: (selectedPatient || (newPatientMode && newPatientName)) ? 'success.main' : 'primary.main' }}>
                    {(selectedPatient || (newPatientMode && newPatientName)) ? <CheckCircleIcon sx={{ fontSize: 14 }} /> : 4}
                  </Avatar>
                  <Typography variant="subtitle2" fontWeight={600}>Patient</Typography>
                </Box>

                {selectedPatient ? (
                  <Box display="flex" alignItems="center" gap={1} p={1.5}
                    sx={{ border: '1px solid', borderColor: 'success.light', borderRadius: 1 }}>
                    <Box flex={1}>
                      <Typography variant="body2" fontWeight={600}>{selectedPatient.fullName}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        MRN: {selectedPatient.mrn}
                        {selectedPatient.primaryPhone && ` · ${selectedPatient.primaryPhone}`}
                        {selectedPatient.dateOfBirth && ` · Age: ${formatAge(selectedPatient.dateOfBirth)}`}
                      </Typography>
                    </Box>
                    <Button size="small" onClick={clearSelectedPatient} startIcon={<CloseIcon fontSize="small" />}>Change</Button>
                  </Box>
                ) : newPatientMode ? (
                  <Box display="flex" flexDirection="column" gap={1.5} p={1.5}
                    sx={{ border: '1px solid', borderColor: 'primary.light', borderRadius: 1 }}>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Typography variant="body2" fontWeight={600} color="primary">New patient</Typography>
                      <Button
                        size="small"
                        onClick={() => {
                          setNewPatientMode(false);
                          setPatientSearchTerm('');
                          setNewPatientName('');
                          setNewPatientAge('');
                          setNewPatientGender('');
                          setNewPatientPhone('');
                          setNewPatientPhoneHint(null);
                          setAppointmentSmsPhone('');
                          setAppointmentSmsTouched(false);
                        }}
                        startIcon={<CloseIcon fontSize="small" />}
                      >
                        Cancel
                      </Button>
                    </Box>
                    <TextField
                      label="Full name *"
                      size="small"
                      value={newPatientName}
                      onChange={(e) => setNewPatientName(e.target.value)}
                      autoFocus
                    />
                    <Box display="flex" gap={1}>
                      <TextField
                        label="Age (years) *"
                        size="small"
                        type="number"
                        inputProps={{ min: 0, max: 150 }}
                        value={newPatientAge}
                        onChange={(e) => setNewPatientAge(e.target.value)}
                        error={newPatientAge !== '' && !isNaN(Number(newPatientAge)) && Number(newPatientAge) > 150}
                        helperText={
                          newPatientAge !== '' && !isNaN(Number(newPatientAge)) && Number(newPatientAge) > 150
                            ? 'Maximum age is 150 years'
                            : ' '
                        }
                        sx={{ flex: 1 }}
                      />
                      <FormControl size="small" sx={{ flex: 1 }}>
                        <InputLabel>Gender *</InputLabel>
                        <Select
                          label="Gender *"
                          value={newPatientGender}
                          onChange={(e) => setNewPatientGender(e.target.value as 'Male' | 'Female' | 'Other' | '')}
                        >
                          <MenuItem value=""><em>Select</em></MenuItem>
                          <MenuItem value="Male">Male</MenuItem>
                          <MenuItem value="Female">Female</MenuItem>
                          <MenuItem value="Other">Other</MenuItem>
                        </Select>
                        <FormHelperText>{' '}</FormHelperText>
                      </FormControl>
                      <TextField
                        label="Patient mobile *"
                        size="small"
                        value={newPatientPhone}
                        onChange={(e) => {
                          setNewPatientPhone(digitsOnlyPhone(e.target.value));
                          setNewPatientPhoneHint(null);
                        }}
                        onBlur={() => void checkNewPatientPhoneHint(newPatientPhone)}
                        helperText={newPatientPhoneHint || 'On patient record'}
                        sx={{ flex: 2 }}
                      />
                    </Box>
                  </Box>
                ) : (
                  <Box position="relative">
                    <TextField
                      label="Search patient"
                      size="small"
                      fullWidth
                      placeholder="Name, phone number, or MRN…"
                      value={patientSearchTerm}
                      onChange={(e) => handlePatientSearchChange(e.target.value)}
                      onKeyDown={handlePatientSearchKeyDown}
                      autoFocus
                      InputProps={{
                        endAdornment: patientSearchLoading ? <CircularProgress size={16} /> : undefined,
                      }}
                    />
                    {showPatientDropdown && (
                      <Paper elevation={4} sx={{ position: 'absolute', zIndex: 1300, width: '100%', maxHeight: 240, overflow: 'auto', mt: 0.5 }}>
                        {patientSearchResults.length > 0 ? (
                          <List dense disablePadding>
                            {patientSearchResults.map((p, idx) => (
                              <ListItemButton
                                key={p.patientId}
                                selected={patientDropdownHighlight === idx}
                                onClick={() => selectPatient(p)}
                              >
                                <ListItemText
                                  primary={p.fullName}
                                  secondary={`MRN: ${p.mrn}${p.primaryPhone ? ' · ' + p.primaryPhone : ''}${p.dateOfBirth ? ' · Age: ' + formatAge(p.dateOfBirth) : ''}`}
                                />
                              </ListItemButton>
                            ))}
                            <Divider />
                          </List>
                        ) : null}
                        <ListItemButton
                          selected={patientDropdownHighlight === patientSearchResults.length}
                          onClick={openRegisterNewPatientFromSearch}
                        >
                          <PersonAddIcon fontSize="small" sx={{ mr: 1, color: 'primary.main' }} />
                          <ListItemText primary="Register new patient" primaryTypographyProps={{ color: 'primary', fontWeight: 600 }} />
                        </ListItemButton>
                      </Paper>
                    )}
                  </Box>
                )}

                {(selectedPatient || newPatientMode) && (
                  <Box mt={2}>
                    <TextField
                      label="Appointment SMS number"
                      size="small"
                      fullWidth
                      value={appointmentSmsPhone}
                      onChange={(e) => {
                        setAppointmentSmsTouched(true);
                        setAppointmentSmsPhone(digitsOnlyPhone(e.target.value));
                      }}
                      helperText="SMS confirmations go to this number. May differ from the patient record (e.g. parent booking for a child)."
                    />
                    {selectedPatient && patientRegistryPhone(selectedPatient) && (
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Patient record mobile: {patientRegistryPhone(selectedPatient)}
                      </Typography>
                    )}
                  </Box>
                )}
              </Box>

              <Divider />

              {/* Appointment type */}
              <Box sx={{ p: 2.5 }}>
                <FormControl fullWidth size="small">
                  <InputLabel>Operation</InputLabel>
                  <Select
                    label="Operation"
                    value={createForm.appointmentType || 'NEW'}
                    onChange={(e) => setCreateForm({ ...createForm, appointmentType: e.target.value })}
                  >
                    {APPOINTMENT_TYPES.map((t) => (
                      <MenuItem key={t} value={t}>{t}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>
            </>
          )}

            {patientBookedSameDay && (
              <Box
                sx={{
                  mx: 2.5,
                  mb: 1,
                  px: 2,
                  py: 1.25,
                  borderRadius: 1,
                  bgcolor: '#fffbeb',
                  border: '1px solid #fcd34d',
                }}
              >
                <Typography variant="body2" fontWeight={600} color="#92400e">
                  This patient already has an appointment with this doctor on the selected date. Choose another date or
                  patient.
                </Typography>
              </Box>
            )}

            <Box display="flex" justifyContent="flex-end" gap={1} sx={{ px: 2.5, py: 2, borderTop: '1px solid', borderColor: 'divider' }}>
              <Button onClick={handleCloseCreateDialog}>Cancel</Button>
              <Button
                variant="contained"
                onClick={handleCreate}
                disabled={
                  loading ||
                  patientBookedSameDay ||
                  !createForm.slotStart ||
                  (!createForm.patientId && !newPatientMode) ||
                  (newPatientMode &&
                    (!newPatientName.trim() ||
                      !newPatientAge.trim() ||
                      !newPatientGender ||
                      !newPatientPhone.trim() ||
                      !isValidPhoneDigitLength(newPatientPhone))) ||
                  (createForm.slotStart ? parseISO(createForm.slotStart).getTime() <= now.getTime() : false)
                }
              >
                {loading ? 'Booking…' : 'Book Appointment'}
              </Button>
            </Box>
          </CardContent>
        </Card>
      )}


      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>Filters</Typography>
            <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-start">
            <Autocomplete
              freeSolo
              options={patientFilterOptions}
              filterOptions={(opts) => opts}
              loading={patientFilterSearching}
              getOptionLabel={(opt) => (typeof opt === 'string' ? opt : opt.label)}
              isOptionEqualToValue={(a, b) =>
                typeof a === 'string' || typeof b === 'string' ? a === b : a.id === b.id
              }
              inputValue={patientFilterInputValue}
              onInputChange={(_, val, reason) => {
                setPatientFilterInputValue(val);
                if (reason === 'reset') return;
                setFilterDraft((prev) => ({ ...prev, patientId: val.trim() }));
                if (patientFilterSearchTimer.current) clearTimeout(patientFilterSearchTimer.current);
                if (reason === 'input') {
                  patientFilterSearchTimer.current = setTimeout(() => void fetchPatientFilterOptions(val), 300);
                }
              }}
              onChange={(_, val) => {
                if (val && typeof val !== 'string') {
                  setFilterDraft((prev) => ({ ...prev, patientId: val.id }));
                  setPatientFilterInputValue(val.label);
                } else if (val === null) {
                  setFilterDraft((prev) => ({ ...prev, patientId: '' }));
                  setPatientFilterInputValue('');
                  setPatientFilterOptions([]);
                } else if (typeof val === 'string') {
                  setFilterDraft((prev) => ({ ...prev, patientId: val.trim() }));
                  setPatientFilterInputValue(val);
                }
              }}
              sx={{ minWidth: 280 }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  size="small"
                  label="Patient"
                  placeholder="Search name or MRN, or paste UUID"
                  helperText="Type at least 2 characters, or paste a patient UUID"
                />
              )}
            />
            <Autocomplete
              options={resourceFilterInput.trim().length >= 2 ? resources : []}
              value={resources.find((r) => r.id === filterDraft.resourceId) ?? null}
              inputValue={resourceFilterInput}
              onInputChange={(_, value) => setResourceFilterInput(value)}
              onChange={(_, value) => setFilterDraft((prev) => ({ ...prev, resourceId: value?.id ?? '' }))}
              getOptionLabel={(option) => schedulingResourceDoctorLabel(option, doctorDepartmentById)}
              isOptionEqualToValue={(a, b) => a.id === b.id}
              noOptionsText={resourceFilterInput.trim().length < 2 ? 'Type at least 2 letters to search doctors' : 'No doctors found'}
              sx={{ minWidth: 260 }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  size="small"
                  label="Resource (doctor)"
                  placeholder="Type doctor name"
                  helperText=" "
                />
              )}
            />
            <TextField label="From date" type="date" size="small" value={filterDraft.fromDate} onChange={(e) => setFilterDraft((prev) => ({ ...prev, fromDate: e.target.value }))} InputLabelProps={{ shrink: true }} />
            <TextField label="To date" type="date" size="small" value={filterDraft.toDate} onChange={(e) => setFilterDraft((prev) => ({ ...prev, toDate: e.target.value }))} InputLabelProps={{ shrink: true }} />
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={filterDraft.status}
                onChange={(e) => setFilterDraft((prev) => ({ ...prev, status: e.target.value }))}
                renderValue={(selected) =>
                  selected ? formatSchedulingStatusLabel(String(selected)) : 'Any'
                }
              >
                <MenuItem value=""><em>Any</em></MenuItem>
                {APPOINTMENT_STATUSES.map((s) => (
                  <MenuItem key={s} value={s} sx={{ textTransform: 'none' }}>
                    {formatSchedulingStatusLabel(s)}
                  </MenuItem>
                ))}
              </Select>
             </FormControl>
             <FormControl size="small" sx={{ minWidth: 168 }}>
               <InputLabel
                 id="filter-appointment-type-label"
                 sx={{ fontSize: '0.8125rem' }}
                 shrink={!!filterDraft.appointmentType}
               >
                 Appointment Type
               </InputLabel>
               <Select
                 labelId="filter-appointment-type-label"
                 label="Appointment Type"
                 value={filterDraft.appointmentType}
                 onChange={(e) => setFilterDraft((prev) => ({ ...prev, appointmentType: e.target.value }))}
                 renderValue={(selected) =>
                   selected ? formatSchedulingAppointmentTypeLabel(String(selected)) : 'Any'
                 }
               >
                 <MenuItem value=""><em>Any</em></MenuItem>
                 {APPOINTMENT_TYPES.map((type) => (
                   <MenuItem key={type} value={type} sx={{ textTransform: 'none' }}>
                     {formatSchedulingAppointmentTypeLabel(type)}
                   </MenuItem>
                 ))}
               </Select>
             </FormControl>
             <Button variant="outlined" size="small" onClick={handleApplyFilters}>Apply</Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">
              Appointments
              {appliedFilters.resourceId.trim() && (
                <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                  · {resourceDisplayLabel(appliedFilters.resourceId) || 'Selected doctor'}
                </Typography>
              )}
            </Typography>
            <Box display="flex" alignItems="center" gap={1.5}>
              <Typography variant="body2">Total: {totalElements}</Typography>
                <Button
                  size="small"
                  variant="outlined"
                  color="error"
                  disabled={loading || selectedAppointmentIds.size === 0 || !canRescheduleOrCancelAppointments}
                  onClick={handleCancelSelected}
                >
                  Cancel Selected
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  disabled={loading || selectedAppointmentIds.size === 0 || !canRescheduleOrCancelAppointments}
                  onClick={handleRescheduleSelected}
                >
                  Reschedule Selected
                </Button>
              <Button size="small" variant="outlined" onClick={handleDownloadList} disabled={appointments.length === 0}>
                Download
              </Button>
              <Button size="small" variant="outlined" onClick={handlePrintList} disabled={appointments.length === 0}>
                Print
              </Button>
            </Box>
          </Box>
          {!appliedFilters.resourceId.trim() && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Select a doctor and click Apply to load appointments.
            </Typography>
          )}
          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" py={4}><CircularProgress /></Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={appointments.length > 0 && appointments.every((a) => selectedAppointmentIds.has(a.id))}
                        indeterminate={
                          appointments.some((a) => selectedAppointmentIds.has(a.id)) &&
                          !(appointments.length > 0 && appointments.every((a) => selectedAppointmentIds.has(a.id)))
                        }
                        onChange={(e) => {
                          const checked = e.target.checked;
                          setSelectedAppointmentIds(checked ? new Set(appointments.map((a) => a.id)) : new Set());
                        }}
                      />
                    </TableCell>
                    <TableCell>SL</TableCell>
                    <TableCell>Patient</TableCell>
                    {!appliedFilters.resourceId.trim() && <TableCell>Doctor (resource)</TableCell>}
                    <TableCell>Phone</TableCell>
                    <TableCell>Age</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Slot</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {appointments.map((a, idx) => (
                    <TableRow key={a.id}>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selectedAppointmentIds.has(a.id)}
                          onChange={(e) => {
                            const checked = e.target.checked;
                            setSelectedAppointmentIds((prev) => {
                              const next = new Set(prev);
                              if (checked) next.add(a.id);
                              else next.delete(a.id);
                              return next;
                            });
                          }}
                        />
                      </TableCell>
                      <TableCell>{page * size + idx + 1}</TableCell>
                      <TableCell>
                        <Typography variant="body2">{patientDisplayLabel(a.patientId)}</Typography>
                      </TableCell>
                      {!appliedFilters.resourceId.trim() && (
                        <TableCell>{resourceDisplayLabel(a.resourceId)}</TableCell>
                      )}
                      <TableCell>{appointmentPhone(a) || '—'}</TableCell>
                      <TableCell>
                        {patientDetailsById[a.patientId]?.dateOfBirth
                          ? formatAge(patientDetailsById[a.patientId]!.dateOfBirth!)
                          : '—'}
                      </TableCell>
                      <TableCell>{a.appointmentDate}</TableCell>
                      <TableCell>{formatTimeOnly(a.slotStart)} – {formatTimeOnly(a.slotEnd)}</TableCell>
                      <TableCell>{a.appointmentType ? formatSchedulingAppointmentTypeLabel(a.appointmentType) : '—'}</TableCell>
                      <TableCell>{a.status ? formatSchedulingStatusLabel(a.status) : '—'}</TableCell>
                      <TableCell align="right">
                        <Button size="small" startIcon={<ViewIcon />} onClick={() => openView(a)}>View</Button>
                        {canAct(a.status) && (
                          <>
                            {canRescheduleOrCancelAppointments && a.status?.toUpperCase() === 'CONFIRMED' && (
                              <Button size="small" startIcon={<RescheduleIcon />} onClick={() => openReschedule(a)}>Reschedule</Button>
                            )}
                            {canRescheduleOrCancelAppointments &&
                              (a.status?.toUpperCase() === 'CONFIRMED' || a.status?.toUpperCase() === 'CHECKED_IN') && (
                              <Button size="small" color="error" startIcon={<CancelIcon />} onClick={() => openCancel(a)}>Cancel</Button>
                            )}
                            {canUpdateAppointmentStatus &&
                              (a.status?.toUpperCase() === 'CONFIRMED' || a.status?.toUpperCase() === 'NO_SHOW') && (
                              <Button size="small" startIcon={<CheckInIcon />} onClick={() => handleCheckIn(a)}>Check-in</Button>
                            )}
                            {canUpdateAppointmentStatus && a.status?.toUpperCase() === 'CONFIRMED' && (
                              <Button size="small" startIcon={<NoShowIcon />} onClick={() => handleNoShow(a)}>No-show</Button>
                            )}
                            {canUpdateAppointmentStatus &&
                              (a.status?.toUpperCase() === 'CONFIRMED' || a.status?.toUpperCase() === 'CHECKED_IN') && (
                              <Button size="small" startIcon={<CheckCircleIcon />} onClick={() => handleComplete(a)}>Complete</Button>
                            )}
                          </>
                        )}
                        {canDeleteAppointment && (
                          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => openDelete(a)}>Delete</Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {appointments.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={appliedFilters.resourceId.trim() ? 11 : 12} align="center">No appointments found.</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={(_, nextPage) => setPage(nextPage)}
            rowsPerPage={size}
            onRowsPerPageChange={(e) => {
              setSize(parseInt(e.target.value, 10));
              setPage(0);
            }}
            rowsPerPageOptions={[10, 20]}
          />
        </CardContent>
      </Card>

      {/* View detail dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Appointment details</DialogTitle>
        <DialogContent>
          {viewLoading ? (
            <Box display="flex" justifyContent="center" py={2}><CircularProgress /></Box>
          ) : viewDetail ? (
            <Box display="flex" flexDirection="column" gap={1} pt={1}>
              <Typography variant="body2" component="div">
                <strong>Patient:</strong>{' '}
                {(() => {
                  const pid = viewDetail.patientId;
                  if (!pid) return '—';
                  const name = patientNamesById[pid];
                  if (name === undefined) return '…';
                  return name || 'Unknown patient';
                })()}
              </Typography>
              <Typography variant="body2"><strong>Resource:</strong> {resourceDisplayLabel(viewDetail.resourceId)}</Typography>
              <Typography variant="body2"><strong>Date:</strong> {viewDetail.appointmentDate}</Typography>
              <Typography variant="body2"><strong>Slot:</strong> {formatDateTime(viewDetail.slotStart)} – {formatDateTime(viewDetail.slotEnd)}</Typography>
              <Typography variant="body2"><strong>Type:</strong> {viewDetail.appointmentType}</Typography>
              <Typography variant="body2"><strong>Status:</strong> {viewDetail.status ? formatSchedulingStatusLabel(viewDetail.status) : '—'}</Typography>
              {viewDetail.tokenNumber != null && <Typography variant="body2"><strong>Token:</strong> {viewDetail.tokenNumber}</Typography>}
              {viewDetail.reservation && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Reservation status: {viewDetail.reservation.status}
                </Typography>
              )}
            </Box>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          {viewDetail && canAct(viewDetail.status) && (
            <>
              {canRescheduleOrCancelAppointments && viewDetail.status?.toUpperCase() === 'CONFIRMED' && (
                <Button variant="outlined" onClick={() => { setViewDialogOpen(false); openReschedule(viewDetail); }}>Reschedule</Button>
              )}
              {canRescheduleOrCancelAppointments &&
                (viewDetail.status?.toUpperCase() === 'CONFIRMED' || viewDetail.status?.toUpperCase() === 'CHECKED_IN') && (
                <Button color="error" variant="outlined" onClick={() => { setViewDialogOpen(false); openCancel(viewDetail); }}>Cancel</Button>
              )}
              {canUpdateAppointmentStatus &&
                (viewDetail.status?.toUpperCase() === 'CONFIRMED' || viewDetail.status?.toUpperCase() === 'NO_SHOW') && (
                <Button variant="outlined" onClick={() => handleCheckIn(viewDetail)}>Check-in</Button>
              )}
              {canUpdateAppointmentStatus && viewDetail.status?.toUpperCase() === 'CONFIRMED' && (
                <Button variant="outlined" onClick={() => handleNoShow(viewDetail)}>No-show</Button>
              )}
              {canUpdateAppointmentStatus &&
                (viewDetail.status?.toUpperCase() === 'CONFIRMED' || viewDetail.status?.toUpperCase() === 'CHECKED_IN') && (
                <Button variant="outlined" onClick={() => handleComplete(viewDetail)}>Complete</Button>
              )}
            </>
          )}
        </DialogActions>
      </Dialog>

      {/* Reschedule dialog */}
      <Dialog
        open={rescheduleDialogOpen}
        onClose={() => {
          if (rescheduleSubmitting) return;
          resetRescheduleDialog();
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          {rescheduleIds.length > 1
            ? `Reschedule ${rescheduleIds.length} appointments`
            : 'Reschedule appointment'}
        </DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            {rescheduleSources.length > 0 && (
              <Typography variant="body2" color="text.secondary">
                Current date:{' '}
                {[...new Set(rescheduleSources.map((s) => slotCalendarDate(s.slotStart)).filter(Boolean))].join(', ') ||
                  rescheduleDate ||
                  '—'}
              </Typography>
            )}
            <TextField
              label="Appointment date"
              type="date"
              size="small"
              fullWidth
              required
              value={rescheduleDate}
              inputProps={{ min: format(new Date(), 'yyyy-MM-dd') }}
              InputLabelProps={{ shrink: true }}
              helperText="Choose the day for the new visit, then pick an available time slot below."
              onChange={(e) => {
                setRescheduleDate(e.target.value);
                setRescheduleForm((prev) => ({ ...prev, newSlotStart: '', newSlotEnd: '' }));
              }}
            />
            {rescheduleAvailabilityError && (
              <Typography variant="caption" color="error">
                Could not load availability. Check your connection and try another date.
              </Typography>
            )}
            {rescheduleDayBlackedOut && !rescheduleSlotsLoading && !rescheduleAvailabilityError && (
              <Typography variant="caption" color="text.secondary">
                Doctor is off on this date. Choose another date to see available slots.
              </Typography>
            )}
            <FormControl size="small" fullWidth required disabled={rescheduleSlotsLoading || !rescheduleDate}>
              <InputLabel id="reschedule-slot-label">New slot</InputLabel>
              <Select
                labelId="reschedule-slot-label"
                label="New slot"
                displayEmpty
                value={rescheduleSlotSelectValue(rescheduleForm.newSlotStart, rescheduleForm.newSlotEnd)}
                onChange={(e) => {
                  const { start: newSlotStart, end: newSlotEnd } = parseRescheduleSlotSelectValue(String(e.target.value));
                  setRescheduleForm((prev) => ({ ...prev, newSlotStart, newSlotEnd }));
                }}
                renderValue={(selected) => {
                  if (!selected) return <em>Select a slot</em>;
                  const { start, end } = parseRescheduleSlotSelectValue(String(selected));
                  const startDate = start ? parseISO(start) : null;
                  const endDate = end ? parseISO(end) : null;
                  const startLabel =
                    startDate && !isNaN(startDate.getTime()) ? format(startDate, 'hh:mm a') : start?.slice(11, 16) ?? '?';
                  const endLabel =
                    endDate && !isNaN(endDate.getTime()) ? format(endDate, 'hh:mm a') : end?.slice(11, 16) ?? '?';
                  const slotDay = start ? slotCalendarDate(start) : '';
                  const showDay =
                    slotDay && rescheduleDate.trim() && slotDay !== rescheduleDate.trim();
                  const dayPrefix =
                    startDate && !isNaN(startDate.getTime())
                      ? format(startDate, 'MMM d, yyyy')
                      : slotDay;
                  return showDay ? `${dayPrefix} · ${startLabel} – ${endLabel}` : `${startLabel} – ${endLabel}`;
                }}
              >
                {rescheduleSlotsLoading && (
                  <MenuItem value="" disabled><em>Loading slots…</em></MenuItem>
                )}
                {!rescheduleSlotsLoading && rescheduleSlots.length === 0 && rescheduleDate && !rescheduleAvailabilityError && (
                  <MenuItem value="" disabled>
                    <em>{rescheduleDayBlackedOut ? 'Doctor off — no slots' : 'No slots available for this date'}</em>
                  </MenuItem>
                )}
                {rescheduleSlots.map((slot, slotIdx) => {
                  const value = rescheduleSlotSelectValue(slot.start, slot.end);
                  const startDate = slot.start ? parseISO(slot.start) : null;
                  const endDate = slot.end ? parseISO(slot.end) : null;
                  const timeLabel = `${startDate && !isNaN(startDate.getTime()) ? format(startDate, 'hh:mm a') : slot.start?.slice(11, 16) ?? '?'} - ${endDate && !isNaN(endDate.getTime()) ? format(endDate, 'hh:mm a') : slot.end?.slice(11, 16) ?? '?'}`;
                  const slotDay = slot.start ? slotCalendarDate(slot.start) : '';
                  const label =
                    slotDay && rescheduleDate.trim() && slotDay !== rescheduleDate.trim() && startDate && !isNaN(startDate.getTime())
                      ? `${format(startDate, 'MMM d')} · ${timeLabel}`
                      : timeLabel;
                  const isTimePassed = startDate ? startDate.getTime() <= now.getTime() : false;
                  const slotFull = slot.availableCount <= 0;
                  const disableSlot =
                    isTimePassed || (slotFull && rescheduleIds.length > 1);
                  return (
                    <MenuItem key={value || `reschedule-slot-${slotIdx}`} value={value} disabled={disableSlot}>
                      {label}{' '}
                      {slot.availableCount > 0
                        ? `(${slot.availableCount} available)`
                        : isTimePassed
                          ? '(Passed)'
                          : slotFull && rescheduleIds.length === 1
                            ? '(May be available)'
                            : '(Full)'}
                    </MenuItem>
                  );
                })}
              </Select>
            </FormControl>
            {rescheduleIds.length === 1 && (
              <TextField
                label="Appointment SMS number"
                size="small"
                fullWidth
                value={rescheduleAppointmentSmsPhone}
                onChange={(e) => setRescheduleAppointmentSmsPhone(digitsOnlyPhone(e.target.value))}
                helperText="Reschedule confirmation SMS. Can differ from the patient record."
              />
            )}
            <TextField
              label="Idempotency key (optional)"
              size="small"
              value={rescheduleForm.idempotencyKey || ''}
              onChange={(e) => setRescheduleForm((prev) => ({ ...prev, idempotencyKey: e.target.value }))}
              disabled={rescheduleIds.length > 1}
              helperText={rescheduleIds.length > 1 ? 'Not used for bulk reschedule' : undefined}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={resetRescheduleDialog} disabled={rescheduleSubmitting}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleReschedule}
            disabled={
              rescheduleSubmitting ||
              !rescheduleForm.newSlotStart?.trim() ||
              !rescheduleForm.newSlotEnd?.trim() ||
              rescheduleSlotsLoading
            }
          >
            {rescheduleSubmitting ? 'Rescheduling…' : 'Reschedule'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => !deleteSubmitting && setDeleteDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete appointment</DialogTitle>
        <DialogContent>
          <Typography>This will permanently delete the appointment record. This action cannot be undone.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)} disabled={deleteSubmitting}>Back</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={deleteSubmitting}>{deleteSubmitting ? 'Deleting…' : 'Delete permanently'}</Button>
        </DialogActions>
      </Dialog>

      {/* Cancel dialog */}
      <Dialog open={cancelDialogOpen} onClose={() => !cancelSubmitting && setCancelDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Cancel appointment</DialogTitle>
        <DialogContent>
          <TextField label="Reason (optional)" size="small" fullWidth multiline value={cancelReason} onChange={(e) => setCancelReason(e.target.value)} sx={{ mt: 1 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelDialogOpen(false)} disabled={cancelSubmitting}>Back</Button>
          <Button color="error" variant="contained" onClick={handleCancel} disabled={cancelSubmitting}>{cancelSubmitting ? 'Cancelling…' : 'Cancel appointment'}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingAppointmentsPage;
