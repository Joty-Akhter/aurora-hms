import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@contexts/AuthContext';
import hospitalService, {
  Patient,
  Encounter,
  DoctorHospitalNote,
  epTemplateService,
  PrescriptionTemplate,
  epConfigService,
  epRecentRxService,
  EpRecentRxEntry,
} from '../../services/hospitalService';
import { pullEpWorkspaceFromServer } from '../../services/epWorkspaceSync';
import hospitalSchedulingService, {
  AppointmentResponse,
} from '../../services/hospitalSchedulingService';
import { formatAge } from '../../utils/ageUtils';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const RECENT_PATIENTS_KEY = 'ep_recent_patients';
const MAX_RECENT = 10;
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const PRESCRIBED_TODAY_PREFIX = 'ep_prescribed_today_';

function saveRecentPatient(patientId: string, patientName: string) {
  try {
    const raw = localStorage.getItem(RECENT_PATIENTS_KEY);
    const list: { id: string; name: string; ts: number }[] = raw ? JSON.parse(raw) : [];
    const filtered = list.filter(p => p.id !== patientId);
    filtered.unshift({ id: patientId, name: patientName, ts: Date.now() });
    localStorage.setItem(RECENT_PATIENTS_KEY, JSON.stringify(filtered.slice(0, MAX_RECENT)));
  } catch {
    // ignore
  }
}

function getRecentPatients(): { id: string; name: string; ts: number }[] {
  try {
    const raw = localStorage.getItem(RECENT_PATIENTS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function getPrescribedTodayPatientIds(dateIso: string): Set<string> {
  try {
    const raw = localStorage.getItem(`${PRESCRIBED_TODAY_PREFIX}${dateIso}`);
    const ids: unknown = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(ids)) return new Set();
    return new Set(ids.filter((id): id is string => typeof id === 'string' && id.trim().length > 0));
  } catch {
    return new Set();
  }
}

interface TodayPatientRow {
  appointmentId: string;
  patientId: string;
  slotStart: string;
  tokenNumber?: number;
  status: string;
  patient?: Patient;
  loadingPatient: boolean;
}

const EP_DASHBOARD_MODE_KEY = 'ep_dashboard_clinical_mode';

const DoctorDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { user, currentOrganizationId, hasPermission, canManageResource } = useAuth();
  const canViewDoctorNotes =
    hasPermission('HOSPITAL_FEAT_DOCTOR_NOTES') ||
    hasPermission('HOSPITAL_DOCTOR_NOTES_MANAGE') ||
    canManageResource('hospital');

  const today = new Date().toISOString().split('T')[0];
  const [todayRows, setTodayRows] = useState<TodayPatientRow[]>([]);
  const [loadingQueue, setLoadingQueue] = useState(true);
  const [queueError, setQueueError] = useState<string | null>(null);

  const [recentPatients, setRecentPatients] = useState<{ id: string; name: string; ts: number }[]>([]);
  const [templates, setTemplates] = useState<PrescriptionTemplate[]>([]);
  const [favTemplateIds, setFavTemplateIds] = useState<string[]>([]);
  const [recentRx, setRecentRx] = useState<EpRecentRxEntry[]>([]);
  const [doctorNotes, setDoctorNotes] = useState<DoctorHospitalNote[]>([]);

  /** EP-1 / EP-11: OPD (scheduling queue) vs IPD (active inpatient encounters) */
  const [clinicalViewMode, setClinicalViewMode] = useState<'OPD' | 'IPD'>(() => {
    try {
      const s = sessionStorage.getItem(EP_DASHBOARD_MODE_KEY);
      if (s === 'IPD' || s === 'OPD') return s;
    } catch { /* ignore */ }
    const d = epConfigService.get().epEncounterModeDefault;
    return d === 'IPD' ? 'IPD' : 'OPD';
  });
  const [ipdRows, setIpdRows] = useState<Encounter[]>([]);
  const [loadingIpd, setLoadingIpd] = useState(false);
  const [ipdError, setIpdError] = useState<string | null>(null);
  /** When true, inpatient API filters by current user as attending physician; when false, all active inpatients in org. */
  const [ipdFilterMyPatients, setIpdFilterMyPatients] = useState(true);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Patient[]>([]);
  const [searching, setSearching] = useState(false);
  const [opdPageSize, setOpdPageSize] = useState(20);
  const [opdPage, setOpdPage] = useState(1);

  const loadQueue = useCallback(async () => {
    setLoadingQueue(true);
    setQueueError(null);
    try {
      const cfg = epConfigService.get();
      const apptParams: {
        fromDate: string;
        toDate: string;
        size: number;
        resourceId?: string;
      } = {
        fromDate: today,
        toDate: today,
        size: 100,
      };
      const rid = cfg.mySchedulingResourceId?.trim();
      if (rid) {
        apptParams.resourceId = rid;
      }
      const data = await hospitalSchedulingService.getAppointments(apptParams);
      const appointments: AppointmentResponse[] = data.content ?? (data as any).appointments ?? [];
      const prescribedToday = getPrescribedTodayPatientIds(today);
      const rows: TodayPatientRow[] = appointments
        .filter(a => {
          const st = a.status?.toUpperCase();
          // Keep COMPLETED appointments for the "Completed Today" counter,
          // while still de-duping the OPD queue for already-prescribed patients.
          if (st === 'COMPLETED') return true;
          return !prescribedToday.has(a.patientId);
        })
        .map(a => ({
        appointmentId: a.id,
        patientId: a.patientId,
        slotStart: a.slotStart,
        tokenNumber: a.tokenNumber,
        status: a.status,
        loadingPatient: true,
      }));
      setTodayRows(rows);

      // Resolve patient names in parallel (batches of 5)
      const fetchBatch = async (batch: TodayPatientRow[]) => {
        const settled = await Promise.allSettled(
          batch.map(r => hospitalService.getPatient(r.patientId))
        );
        setTodayRows(prev =>
          prev.map(row => {
            const batchIdx = batch.findIndex(b => b.patientId === row.patientId && b.appointmentId === row.appointmentId);
            if (batchIdx < 0) return row;
            const result = settled[batchIdx];
            return {
              ...row,
              patient: result.status === 'fulfilled' ? result.value.data : undefined,
              loadingPatient: false,
            };
          })
        );
      };

      for (let i = 0; i < rows.length; i += 5) {
        fetchBatch(rows.slice(i, i + 5));
      }
    } catch (err: any) {
      setQueueError(ehrApiErrorMessage(err, "Could not load today's appointments"));
    } finally {
      setLoadingQueue(false);
    }
  }, [today]);

  const refreshRecentRx = useCallback(() => {
    setRecentRx(epRecentRxService.list());
  }, []);

  const handleRefreshRecentRx = useCallback(async () => {
    await pullEpWorkspaceFromServer();
    refreshRecentRx();
  }, [refreshRecentRx]);

  const refreshWorkspaceUiFromStorage = useCallback(() => {
    setRecentPatients(getRecentPatients());
    refreshRecentRx();
    setTemplates(epTemplateService.getAll());
    setFavTemplateIds(epConfigService.get().favoriteTemplateIds ?? []);
  }, [refreshRecentRx]);

  useEffect(() => {
    void loadQueue();
  }, [loadQueue]);

  // OPD workflow integration: when PrescriptionManagement marks an appointment completed,
  // refresh the queue so "Completed Today" and the OPD list stay in sync.
  useEffect(() => {
    const handler = () => {
      void loadQueue();
    };
    window.addEventListener('ep-queue-updated', handler as unknown as EventListener);
    return () => window.removeEventListener('ep-queue-updated', handler as unknown as EventListener);
  }, [loadQueue]);

  useEffect(() => {
    if (!currentOrganizationId) {
      refreshWorkspaceUiFromStorage();
      return;
    }
    let cancelled = false;
    (async () => {
      await pullEpWorkspaceFromServer();
      if (cancelled) return;
      refreshWorkspaceUiFromStorage();
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId, refreshWorkspaceUiFromStorage]);

  useEffect(() => {
    if (!canViewDoctorNotes) {
      setDoctorNotes([]);
      return;
    }
    let cancelled = false;
    hospitalService
      .getDoctorHospitalNotes()
      .then((res) => {
        if (!cancelled) setDoctorNotes((res.data ?? []).slice(0, 5));
      })
      .catch(() => {
        if (!cancelled) setDoctorNotes([]);
      });
    return () => {
      cancelled = true;
    };
  }, [canViewDoctorNotes]);

  useEffect(() => {
    const onFocus = () => refreshRecentRx();
    const onStorage = (e: StorageEvent) => {
      if (e.key === 'ep_recent_prescriptions') refreshRecentRx();
    };
    const onRecentRx = () => refreshRecentRx();
    window.addEventListener('focus', onFocus);
    window.addEventListener('storage', onStorage);
    window.addEventListener('ep-recent-rx-updated', onRecentRx);
    return () => {
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('storage', onStorage);
      window.removeEventListener('ep-recent-rx-updated', onRecentRx);
    };
  }, [refreshRecentRx]);

  useEffect(() => {
    try {
      sessionStorage.setItem(EP_DASHBOARD_MODE_KEY, clinicalViewMode);
    } catch { /* ignore */ }
  }, [clinicalViewMode]);

  const loadIpd = useCallback(async () => {
    if (!currentOrganizationId) {
      setIpdError('Select an organization to load IPD list');
      setIpdRows([]);
      return;
    }
    setLoadingIpd(true);
    setIpdError(null);
    try {
      const rawUserId = typeof user?.id === 'string' ? user.id.trim() : '';
      const attendingId = ipdFilterMyPatients && UUID_RE.test(rawUserId) ? rawUserId : undefined;
      const res = await hospitalService.getActiveInpatientEncounters(
        currentOrganizationId,
        attendingId
      );
      setIpdRows(res.data ?? []);
    } catch (e: unknown) {
      // Fallback: if "my patients only" fails due physician linkage/data issue, retry org-wide IPD list.
      if (ipdFilterMyPatients) {
        try {
          const fallbackRes = await hospitalService.getActiveInpatientEncounters(currentOrganizationId);
          setIpdRows(fallbackRes.data ?? []);
          setIpdError('Could not apply attending filter. Showing all active inpatients.');
          return;
        } catch {
          // continue to standard error extraction below
        }
      }
      setIpdError(ehrApiErrorMessage(e, 'Could not load inpatient encounters'));
      setIpdRows([]);
    } finally {
      setLoadingIpd(false);
    }
  }, [currentOrganizationId, user?.id, ipdFilterMyPatients]);

  useEffect(() => {
    if (clinicalViewMode === 'IPD') {
      void loadIpd();
    }
  }, [clinicalViewMode, loadIpd]);

  const handleSearchPatient = useCallback(async (q: string) => {
    if (q.length < 2) { setSearchResults([]); return; }
    setSearching(true);
    try {
      const res = await hospitalService.getPatients(q);
      setSearchResults((res.data as any).content ?? res.data);
    } catch {
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    const t = setTimeout(() => handleSearchPatient(searchQuery), 300);
    return () => clearTimeout(t);
  }, [searchQuery, handleSearchPatient]);

  /** EP-3: land on prescribing screen in Quick Mode with a new Rx (inline layout, not a blocking modal). */
  const goPrescribe = (patientId: string, patientName?: string, schedulingAppointmentId?: string) => {
    if (patientName) saveRecentPatient(patientId, patientName);
    navigate(
      `/hospital/patients/${patientId}/prescriptions?epQuick=1&epNew=1&epMode=${clinicalViewMode}`,
      { state: { schedulingAppointmentId: schedulingAppointmentId ?? undefined } }
    );
  };

  /** Apply a template on the prescribing screen (requires a patient context). */
  const goPrescribeWithTemplate = (templateId: string) => {
    const rx = epRecentRxService.list();
    if (rx.length > 0) {
      navigate(`/hospital/patients/${rx[0].patientId}/prescriptions?applyTemplate=${encodeURIComponent(templateId)}&epQuick=1&epMode=${clinicalViewMode}`);
      return;
    }
    const rp = getRecentPatients();
    if (rp.length > 0) {
      navigate(`/hospital/patients/${rp[0].id}/prescriptions?applyTemplate=${encodeURIComponent(templateId)}&epQuick=1&epMode=${clinicalViewMode}`);
      return;
    }
    window.alert(
      'Search for a patient above first, or complete one prescription — then you can apply a template in one tap.'
    );
  };

  const toggleFav = (templateId: string) => {
    const cfg = epConfigService.get();
    const favs = cfg.favoriteTemplateIds ?? [];
    const next = favs.includes(templateId)
      ? favs.filter(id => id !== templateId)
      : [...favs, templateId];
    epConfigService.save({ ...cfg, favoriteTemplateIds: next });
    setFavTemplateIds(next);
  };

  const formatTime = (iso: string) => {
    try {
      return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  };

  const statusColor = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'CHECKED_IN': return '#22c55e';
      case 'CONFIRMED': return '#f59e0b';
      case 'SCHEDULED': return '#3b82f6';
      case 'IN_PROGRESS': return '#f59e0b';
      case 'COMPLETED': return '#6b7280';
      case 'CANCELLED': return '#ef4444';
      case 'NO_SHOW': return '#9ca3af';
      default: return '#6b7280';
    }
  };

  const favTemplates = templates.filter(t => favTemplateIds.includes(t.templateId));
  const systemTemplates = templates.filter(t => t.templateType === 'SYSTEM');
  const myResourceId = epConfigService.get().mySchedulingResourceId?.trim();
  const dateLabel = new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  const checkedInCount = todayRows.filter(r => r.status?.toUpperCase() === 'CHECKED_IN' || r.status?.toUpperCase() === 'IN_PROGRESS').length;
  const completedCount = todayRows.filter(r => r.status?.toUpperCase() === 'COMPLETED').length;
  const opdQueueRows = useMemo(
    () =>
      todayRows
        .filter((r) => {
          const st = r.status?.toUpperCase();
          return st === 'CHECKED_IN' || st === 'CONFIRMED';
        })
        .slice()
        .sort((a, b) => {
          const aPri = a.status?.toUpperCase() === 'CHECKED_IN' ? 0 : 1;
          const bPri = b.status?.toUpperCase() === 'CHECKED_IN' ? 0 : 1;
          if (aPri !== bPri) return aPri - bPri;
          const at = a.slotStart ? new Date(a.slotStart).getTime() : 0;
          const bt = b.slotStart ? new Date(b.slotStart).getTime() : 0;
          return at - bt;
        }),
    [todayRows],
  );

  const opdTotalPages = Math.max(1, Math.ceil(opdQueueRows.length / opdPageSize));
  const opdSafePage = Math.min(opdPage, opdTotalPages);
  const opdPageStart = (opdSafePage - 1) * opdPageSize;
  const opdPageRows = opdQueueRows.slice(opdPageStart, opdPageStart + opdPageSize);

  /** Slot-ordered SL for display (1…n); avoids duplicate tokenNumber on legacy rows. */
  const opdDisplaySerialByAppointmentId = useMemo(() => {
    const map = new Map<string, number>();
    opdQueueRows.forEach((row, idx) => {
      map.set(row.appointmentId, idx + 1);
    });
    return map;
  }, [opdQueueRows]);

  useEffect(() => {
    setOpdPage(1);
  }, [today]);

  useEffect(() => {
    if (opdPage > opdTotalPages) {
      setOpdPage(opdTotalPages);
    }
  }, [opdPage, opdTotalPages]);

  const recentPrescribedCount = Math.max(recentPatients.length, recentRx.length);

  const statsStrip = clinicalViewMode === 'IPD'
    ? [
        { label: 'Active inpatients', value: loadingIpd ? '…' : String(ipdRows.length), color: '#1d4ed8', bg: '#dbeafe' },
        { label: 'Attending filter', value: ipdFilterMyPatients ? 'My patients' : 'All in org', color: '#6b7280', bg: '#f3f4f6' },
        { label: 'OPD today (reference)', value: loadingQueue ? '…' : String(todayRows.length), color: '#93c5fd', bg: '#eff6ff' },
        { label: 'Recent Rx / opened', value: String(recentPrescribedCount), color: '#7c3aed', bg: '#ede9fe' },
      ]
    : [
        { label: "Today's Appointments", value: loadingQueue ? '…' : String(todayRows.length), color: '#1d4ed8', bg: '#dbeafe' },
        { label: 'Checked In / In Progress', value: loadingQueue ? '…' : String(checkedInCount), color: '#d97706', bg: '#fef3c7' },
        { label: 'Completed Today', value: loadingQueue ? '…' : String(completedCount), color: '#059669', bg: '#d1fae5' },
        { label: 'Recent Rx / opened', value: String(recentPrescribedCount), color: '#7c3aed', bg: '#ede9fe' },
      ];

  return (
    <div className="hospital-page">
      {/* Header */}
      <div className="page-header" style={{ marginBottom: '16px' }}>
        <div>
          <h1 style={{ fontSize: '22px', marginBottom: '4px' }}>
            Doctor Dashboard
          </h1>
          <p style={{ color: 'var(--text-secondary, #6b7280)', margin: 0 }}>
            {dateLabel} &nbsp;·&nbsp;
            {user?.firstName ? `Dr. ${user.firstName} ${user.lastName || ''}`.trim() : user?.username}
            {myResourceId && (
              <span style={{ display: 'block', marginTop: '6px', fontSize: '12px' }}>
                Today&apos;s queue filtered to scheduling resource: <code style={{ background: 'rgba(0,0,0,0.06)', padding: '2px 6px', borderRadius: '4px' }}>{myResourceId}</code>
                {' '}(change in EP Settings)
              </span>
            )}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn-secondary" onClick={() => navigate('/hospital/prescriptions/templates')}>
            Rx Templates
          </button>
          <button className="btn-secondary" onClick={() => navigate('/hospital/prescriptions/admin')}>
            EP Settings
          </button>
        </div>
      </div>

      {/* EP-1 / EP-11: OPD vs IPD */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
        <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          Clinical view
        </span>
        <div style={{ display: 'inline-flex', borderRadius: '8px', border: '1px solid var(--border-color, #e5e7eb)', overflow: 'hidden' }}>
          <button
            type="button"
            className={clinicalViewMode === 'OPD' ? 'btn-primary' : 'btn-secondary'}
            style={{ fontSize: '13px', borderRadius: 0, border: 'none' }}
            onClick={() => setClinicalViewMode('OPD')}
          >
            OPD (appointments)
          </button>
          <button
            type="button"
            className={clinicalViewMode === 'IPD' ? 'btn-primary' : 'btn-secondary'}
            style={{ fontSize: '13px', borderRadius: 0, border: 'none' }}
            onClick={() => setClinicalViewMode('IPD')}
          >
            IPD (inpatients)
          </button>
        </div>
        <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
          {clinicalViewMode === 'OPD'
            ? "Today's queue uses scheduling. Switch to IPD for active admissions / inpatient encounters."
            : 'IPD list: active inpatient encounters. Toggle whether to show only patients where you are the attending physician, or all inpatients in this organization.'}
        </span>
      </div>

      {/* Stats strip — OPD = scheduling; IPD = inpatient list + filter context */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: '12px', marginBottom: '20px' }}>
        {statsStrip.map(stat => (
          <div key={stat.label} style={{ background: stat.bg, borderRadius: '10px', padding: '14px 16px' }}>
            <div style={{ fontSize: '22px', fontWeight: 700, color: stat.color }}>{stat.value}</div>
            <div style={{ fontSize: '12px', color: stat.color, opacity: 0.8, marginTop: '2px' }}>{stat.label}</div>
          </div>
        ))}
      </div>

      {/* EP-2: favourite template shortcuts (one tap when a recent patient exists) */}
      {favTemplates.length > 0 && (
        <div className="form-container" style={{ marginBottom: '20px', padding: '12px 16px' }}>
          <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Favourite template shortcuts
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}>
            {favTemplates.map(t => (
              <button
                key={t.templateId}
                type="button"
                className="btn-secondary"
                style={{ fontSize: '13px', padding: '8px 14px', borderRadius: '999px', fontWeight: 500 }}
                onClick={() => goPrescribeWithTemplate(t.templateId)}
                title="Opens prescribe screen with this template (needs a recent patient)"
              >
                ★ {t.templateName}
              </button>
            ))}
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '10px 0 0' }}>
            Tip: complete one prescription or use search above so the system knows which patient to open.
          </p>
        </div>
      )}

      {/* Quick patient search */}
      <div className="form-container" style={{ marginBottom: '24px', padding: '16px 20px' }}>
        <h4 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 600, color: 'var(--text-secondary)' }}>
          QUICK PRESCRIBE — Search Patient
        </h4>
        <div style={{ position: 'relative' }}>
          <input
            type="text"
            placeholder="Search by name, MRN, or phone…"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            style={{ width: '100%', fontSize: '15px', padding: '10px 12px', boxSizing: 'border-box' }}
          />
          {(searching || searchResults.length > 0) && (
            <div className="autocomplete-dropdown">
              {searching && <div className="autocomplete-item">Searching…</div>}
              {!searching && searchResults.length === 0 && searchQuery.length >= 2 && (
                <div className="autocomplete-item" style={{ color: '#888' }}>No patients found</div>
              )}
              {searchResults.map(p => (
                <div
                  key={p.patientId}
                  className="autocomplete-item"
                  style={{ cursor: 'pointer' }}
                  onMouseDown={() => {
                    setSearchQuery('');
                    setSearchResults([]);
                    goPrescribe(p.patientId, p.fullName);
                  }}
                >
                  <strong>{p.fullName || '—'}</strong>
                  <span style={{ color: '#666', fontSize: '12px' }}> · MRN: {p.mrn}</span>
                  {p.primaryPhone && (
                    <span style={{ color: '#888', fontSize: '12px' }}> · {p.primaryPhone}</span>
                  )}
                  {p.dateOfBirth && (
                    <span style={{ color: '#888', fontSize: '12px' }}>
                      {' '}· Age: {formatAge(p.dateOfBirth)}
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '24px', alignItems: 'start' }}>
        {/* Today's OPD Queue OR IPD inpatient list */}
        <div>
          {clinicalViewMode === 'IPD' ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', flexWrap: 'wrap', gap: '10px' }}>
                <h3 style={{ margin: 0, fontSize: '16px' }}>
                  Active inpatients
                  {!loadingIpd && (
                    <span style={{ marginLeft: '8px', fontSize: '13px', fontWeight: 400, color: 'var(--text-secondary)' }}>
                      {ipdRows.length} encounter{ipdRows.length !== 1 ? 's' : ''}
                    </span>
                  )}
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                  <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: 'var(--text-secondary)', cursor: 'pointer', userSelect: 'none' }}>
                    <input
                      type="checkbox"
                      checked={ipdFilterMyPatients}
                      onChange={e => setIpdFilterMyPatients(e.target.checked)}
                    />
                    My patients only (attending)
                  </label>
                  <button className="btn-secondary" style={{ fontSize: '12px', padding: '4px 10px' }} onClick={() => void loadIpd()}>
                    Refresh
                  </button>
                </div>
              </div>
              {loadingIpd && <div className="loading">Loading inpatient encounters…</div>}
              {ipdError && <div className="error-message">{ipdError}</div>}
              {!loadingIpd && ipdRows.length === 0 && !ipdError && (
                <div className="empty-state">
                  <p>No active inpatient encounters</p>
                  <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                    Create admissions in Encounter management, or use OPD view for today&apos;s appointments.
                  </p>
                </div>
              )}
              {ipdRows.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {ipdRows.map(enc => (
                    <div
                      key={enc.encounterId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        padding: '12px 16px',
                        border: '1px solid var(--border-color, #e5e7eb)',
                        borderRadius: '8px',
                        background: 'var(--bg-elevated, #fff)',
                      }}
                    >
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: '14px' }}>
                          {enc.patientName || enc.mrn || enc.patientId}
                        </div>
                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                          {enc.encounterNumber} · {String(enc.encounterType).replace(/_/g, ' ')}
                          {enc.roomNumber && ` · Room ${enc.roomNumber}`}
                          {enc.bedNumber && ` · Bed ${enc.bedNumber}`}
                        </div>
                      </div>
                      <button
                        className="btn-primary"
                        style={{ fontSize: '12px', padding: '5px 12px' }}
                        onClick={() => goPrescribe(enc.patientId, enc.patientName)}
                      >
                        Prescribe
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : (
            <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '16px' }}>
              Today's OPD Queue
              {!loadingQueue && (
                <span style={{ marginLeft: '8px', fontSize: '13px', fontWeight: 400, color: 'var(--text-secondary)' }}>
                  {opdQueueRows.length} appointment{opdQueueRows.length !== 1 ? 's' : ''}
                </span>
              )}
            </h3>
            <button className="btn-secondary" style={{ fontSize: '12px', padding: '4px 10px' }} onClick={loadQueue}>
              Refresh
            </button>
          </div>

          {loadingQueue && <div className="loading">Loading today's queue…</div>}
          {queueError && <div className="error-message">{queueError}</div>}

          {!loadingQueue && opdQueueRows.length === 0 && !queueError && (
            <div className="empty-state">
              <p>No appointments scheduled for today</p>
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                Use the search box above to prescribe for any patient
              </p>
            </div>
          )}

          {opdQueueRows.length > 0 && (
            <>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {opdPageRows.map(row => {
                const pName = row.patient?.fullName || (row.loadingPatient ? '…' : `Patient ${row.patientId.slice(-6)}`);
                const pMrn = row.patient?.mrn;
                return (
                  <div
                    key={row.appointmentId}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px 16px',
                      border: '1px solid var(--border-color, #e5e7eb)',
                      borderRadius: '8px',
                      background: 'var(--bg-elevated, #fff)',
                    }}
                  >
                    <div style={{
                      flexShrink: 0,
                      width: '36px', height: '36px',
                      borderRadius: '50%',
                      background: 'var(--color-primary-light, #dbeafe)',
                      color: 'var(--color-primary, #1d4ed8)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontWeight: 700, fontSize: '14px',
                    }}>
                      {opdDisplaySerialByAppointmentId.get(row.appointmentId) ?? row.tokenNumber ?? '—'}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, fontSize: '14px' }}>{pName}</div>
                      <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                        {pMrn && <span>MRN: {pMrn} &nbsp;·&nbsp;</span>}
                        {formatTime(row.slotStart)}
                        &nbsp;·&nbsp;
                        <span style={{ color: statusColor(row.status) }}>
                          {row.status.replace(/_/g, ' ')}
                        </span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        className="btn-secondary"
                        style={{ fontSize: '12px', padding: '5px 10px' }}
                        onClick={() => navigate(`/hospital/patients/${row.patientId}`)}
                      >
                        Patient
                      </button>
                      <button
                        className="btn-primary"
                        style={{ fontSize: '12px', padding: '5px 12px' }}
                        onClick={() => goPrescribe(row.patientId, row.patient?.fullName, row.appointmentId)}
                      >
                        Prescribe
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
            <div className="table-footer" style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '12px', marginTop: '12px' }}>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--text-secondary)' }}>
                Showing {opdPageStart + 1}–{Math.min(opdPageStart + opdPageSize, opdQueueRows.length)} of {opdQueueRows.length} in queue
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px' }}>
                <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}>
                  Per page
                  <select
                    value={opdPageSize}
                    onChange={(e) => {
                      setOpdPageSize(Number(e.target.value));
                      setOpdPage(1);
                    }}
                    className="filter-select"
                    style={{ minWidth: '72px' }}
                  >
                    {[20, 50, 100].map((n) => (
                      <option key={n} value={n}>
                        {n}
                      </option>
                    ))}
                  </select>
                </label>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '12px', padding: '4px 10px' }}
                    disabled={opdSafePage <= 1}
                    onClick={() => setOpdPage((p) => Math.max(1, p - 1))}
                  >
                    Previous Page
                  </button>
                  <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                    Page {opdSafePage} / {opdTotalPages}
                  </span>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '12px', padding: '4px 10px' }}
                    disabled={opdSafePage >= opdTotalPages}
                    onClick={() => setOpdPage((p) => Math.min(opdTotalPages, p + 1))}
                  >
                    Next Page
                  </button>
                </div>
              </div>
            </div>
            </>
          )}
            </>
          )}
        </div>

        {/* Sidebar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {canViewDoctorNotes && (
            <div className="form-container" style={{ padding: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h4 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Doctor messages
                </h4>
                <button type="button" className="btn-link" style={{ fontSize: '11px' }} onClick={() => navigate('/hospital/doctor-notes')}>
                  View all
                </button>
              </div>
              {doctorNotes.length === 0 ? (
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0 }}>
                  No hospital-wide doctor messages right now.
                </p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {doctorNotes.map((n) => (
                    <div
                      key={n.noteId}
                      style={{
                        padding: '8px 10px',
                        borderRadius: '8px',
                        border: '1px solid var(--border-color, #e5e7eb)',
                        background: 'var(--bg-elevated, #fff)',
                      }}
                    >
                      <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                        {n.doctorName}
                        {n.createdAt
                          ? ` · ${new Date(n.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`
                          : ''}
                      </div>
                      <div style={{ fontSize: '12px', whiteSpace: 'pre-wrap' }}>{n.message}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* EP-2: recent prescriptions (browser-local) */}
          <div className="form-container" style={{ padding: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h4 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Recent prescriptions
              </h4>
              {recentRx.length > 0 && (
                <button type="button" className="btn-link" style={{ fontSize: '11px' }} onClick={handleRefreshRecentRx}>
                  Refresh
                </button>
              )}
            </div>
            {recentRx.length === 0 ? (
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0 }}>
                No recent prescriptions in this browser yet. After you save a prescription, it appears here for quick re-open.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {recentRx.slice(0, 8).map(rx => (
                  <div
                    key={`${rx.prescriptionId}-${rx.createdAt}`}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px',
                      padding: '8px 10px',
                      borderRadius: '8px',
                      border: '1px solid var(--border-color, #e5e7eb)',
                      background: 'var(--bg-elevated, #fff)',
                    }}
                  >
                    <div style={{ fontSize: '12px', fontWeight: 600 }}>{rx.patientName || 'Patient'}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)', lineHeight: 1.35 }}>
                      {rx.medicationSummary}
                      {rx.prescriptionNumber && (
                        <span style={{ color: '#9ca3af' }}> · {rx.prescriptionNumber}</span>
                      )}
                    </div>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        className="btn-link"
                        style={{ fontSize: '12px' }}
                        onClick={() => goPrescribe(rx.patientId, rx.patientName)}
                      >
                        Prescribe again
                      </button>
                      <button
                        type="button"
                        className="btn-link"
                        style={{ fontSize: '12px' }}
                        onClick={() => {
                          if (!UUID_RE.test(rx.patientId)) {
                            alert('Patient chart is unavailable for this prescription (invalid patient reference). Open the patient from search or Recently Opened.');
                            return;
                          }
                          navigate(`/hospital/patients/${rx.patientId}`);
                        }}
                        disabled={!UUID_RE.test(rx.patientId)}
                      >
                        Patient chart
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {recentPatients.length > 0 && (
            <div className="form-container" style={{ padding: '16px' }}>
              <h4 style={{ margin: '0 0 12px', fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Recently opened
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {recentPatients.slice(0, 6).map(p => (
                  <div key={p.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                    <span style={{ fontSize: '13px', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {p.name}
                    </span>
                    <button
                      className="btn-link"
                      style={{ fontSize: '12px', flexShrink: 0 }}
                      onClick={() => goPrescribe(p.id, p.name)}
                    >
                      Prescribe
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Favourite Templates */}
          <div className="form-container" style={{ padding: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h4 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Favourite Templates
              </h4>
              <button
                className="btn-link"
                style={{ fontSize: '12px' }}
                onClick={() => navigate('/hospital/prescriptions/templates')}
              >
                Manage
              </button>
            </div>
            {favTemplates.length === 0 ? (
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0 }}>
                Star templates on the Templates page to pin them here.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {favTemplates.map(t => (
                  <div key={t.templateId} style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '12px', flex: 1, color: 'var(--text-primary)' }}>{t.templateName}</span>
                    <span style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
                      {t.medications.length} med{t.medications.length !== 1 ? 's' : ''}
                    </span>
                    <button
                      type="button"
                      className="btn-primary"
                      style={{ fontSize: '11px', padding: '4px 10px' }}
                      onClick={() => goPrescribeWithTemplate(t.templateId)}
                    >
                      Use
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* System Templates Quick Reference */}
          <div className="form-container" style={{ padding: '16px' }}>
            <h4 style={{ margin: '0 0 12px', fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              System Templates
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {systemTemplates.map(t => (
                <div key={t.templateId} style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    onClick={() => toggleFav(t.templateId)}
                    title={favTemplateIds.includes(t.templateId) ? 'Remove from favourites' : 'Add to favourites'}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '14px', color: favTemplateIds.includes(t.templateId) ? '#f59e0b' : '#d1d5db', padding: '0 2px', flexShrink: 0 }}
                  >
                    ★
                  </button>
                  <span style={{ fontSize: '12px', flex: 1, color: 'var(--text-primary)', minWidth: '100px' }}>{t.templateName}</span>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '11px', padding: '3px 8px' }}
                    onClick={() => goPrescribeWithTemplate(t.templateId)}
                  >
                    Use
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DoctorDashboard;
