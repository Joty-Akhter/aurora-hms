import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { getEpPrescriptionReturnPath } from '../../utils/epPrescriptionReturn';
import hospitalService, {
  Patient,
  Prescription,
  EmergencyContact,
  Insurance,
  Consent,
  VitalSigns,
  PatientProblem,
  Allergy,
  EmergencyContactRequest,
  InsuranceRequest,
  ConsentRequest,
  PatientSummary,
  PatientDocument,
  PatientDocumentType,
} from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import {
  formatClinicalEnumLabel,
  formatGenderLabel,
  formatInsuranceVerificationLabel,
  formatProblemStatusLabel,
  normalizeInsuranceVerification,
} from '../../utils/patientDisplay';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { openPatientIdentityCardPrintWindow } from '@/utils/patientIdentityCardPrint';
import { ehrApiErrorMessage } from '@/utils/ehrApiError';
import { digitsOnlyPhone, isValidOptionalEmail } from '../../utils/formValidation';
import './Hospital.css';

const patientPrescriptionsPath = (patientId: string) =>
  `/hospital/patients/${patientId}/prescriptions`;
const patientAddPrescriptionPath = (patientId: string) =>
  `/hospital/patients/${patientId}/prescriptions?epNew=1`;

const PatientDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const prescriptionReturnPath =
    (location.state as { prescriptionReturn?: string } | null)?.prescriptionReturn
    ?? getEpPrescriptionReturnPath();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [patientSummary, setPatientSummary] = useState<PatientSummary | null>(null);
  const [latestVitalSigns, setLatestVitalSigns] = useState<VitalSigns | null>(null);
  const [allPrescriptions, setAllPrescriptions] = useState<Prescription[]>([]);
  const [activeProblemsLive, setActiveProblemsLive] = useState<PatientProblem[]>([]);
  const [activeAllergiesLive, setActiveAllergiesLive] = useState<Allergy[]>([]);
  const [emergencyContacts, setEmergencyContacts] = useState<EmergencyContact[]>([]);
  const [insuranceList, setInsuranceList] = useState<Insurance[]>([]);
  const [consents, setConsents] = useState<Consent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<
    | 'overview'
    | 'other-personal-info'
    | 'contacts'
    | 'insurance'
    | 'consents'
    | 'medical-history'
    | 'allergies'
    | 'immunizations'
    | 'family-history'
    | 'social-history'
    | 'documents'
    | 'vital-signs'
    | 'clinical-notes'
    | 'problems'
    | 'prescriptions'
  >('overview');

  useEffect(() => {
    if (id) {
      loadPatientData();
    }
  }, [id]);

  // Reload prescriptions whenever the overview tab becomes active so newly added prescriptions show up
  useEffect(() => {
    if (activeTab === 'overview' && id) {
      hospitalService.getPrescriptions(id)
        .then(res => setAllPrescriptions(res.data ?? []))
        .catch(() => {});
    }
  }, [activeTab, id]);

  const loadPatientData = async () => {
    if (!id) return;

    try {
      setLoading(true);
      setError(null);

      const [patientRes, summaryRes, contactsRes, insuranceRes, consentsRes, latestVitalsRes, activeProblemsRes, activeAllergiesRes, prescriptionsRes] = await Promise.all([
        hospitalService.getPatient(id),
        hospitalService.getPatientSummary(id).catch(() => ({ data: null })),
        hospitalService.getEmergencyContacts(id).catch(() => ({ data: [] })),
        hospitalService.getInsurance(id).catch(() => ({ data: [] })),
        hospitalService.getConsents(id).catch(() => ({ data: [] })),
        hospitalService.getLatestVitalSigns(id).catch(() => ({ data: null })),
        hospitalService.getActiveProblems(id).catch(() => ({ data: [] })),
        hospitalService.getActiveAllergies(id).catch(() => ({ data: [] })),
        hospitalService.getPrescriptions(id).catch(() => ({ data: [] }))
      ]);

      setPatient(patientRes.data);
      setPatientSummary(summaryRes.data);
      setEmergencyContacts(contactsRes.data);
      setInsuranceList(insuranceRes.data);
      setConsents(consentsRes.data);
      setLatestVitalSigns(latestVitalsRes.data ?? null);
      setActiveProblemsLive(activeProblemsRes.data);
      setActiveAllergiesLive(activeAllergiesRes.data);
      setAllPrescriptions(prescriptionsRes.data ?? []);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load patient data'));
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const handlePrintIdentityCard = async () => {
    if (!id) return;
    try {
      const res = await hospitalService.reprintPatientIdentityCard(id);
      openPatientIdentityCardPrintWindow(res.data);
    } catch (err: any) {
      window.alert(ehrApiErrorMessage(err, 'Failed to print identity card'));
    }
  };

  const handleReplaceIdentityCard = async () => {
    if (!id) return;
    const reason = window.prompt('Replacement reason (e.g. LOST, DAMAGED, OTHER):', 'LOST');
    if (!reason || !reason.trim()) {
      return;
    }
    try {
      const replaceRes = await hospitalService.replacePatientIdentityCard(id, reason.trim());
      await loadPatientData();
      const printRes = await hospitalService.reprintPatientIdentityCard(id);
      openPatientIdentityCardPrintWindow(printRes.data);
      window.alert(
        `Identity card replaced.\nNew card number: ${replaceRes.data.cardNumber}\nReason: ${replaceRes.data.reason}`
      );
    } catch (err: any) {
      window.alert(ehrApiErrorMessage(err, 'Failed to replace patient identity card'));
    }
  };

  const activeProblems = activeProblemsLive.length > 0
    ? activeProblemsLive
    : (patientSummary?.activeProblems ?? []);
  const activeAllergies = activeAllergiesLive.length > 0
    ? activeAllergiesLive
    : (patientSummary?.activeAllergies ?? []);
  const latestVitalSignsForOverview = latestVitalSigns || patientSummary?.latestVitalSigns || null;
  const recentNotes = patientSummary?.recentNotes ?? [];
  const rawActivePrescriptions = patientSummary?.activePrescriptions ?? [];
  const activeMedications = patientSummary?.activeMedications ?? [];
  const activePrescriptionsForOverview =
    rawActivePrescriptions.length > 0
      ? rawActivePrescriptions
      : activeMedications.length > 0
        ? activeMedications.map((medication) => ({
            prescriptionId: medication.medicationId,
            medicationName: medication.medicationName,
            dosage: medication.dosage || '-',
            route: medication.route || '-',
            frequency: medication.frequency,
            startDate: medication.startDate || '',
            refillsRemaining: undefined,
          }))
        : allPrescriptions
            .filter(rx => rx.prescriptionStatus !== 'CANCELLED' && rx.prescriptionStatus !== 'EXPIRED' && rx.prescriptionStatus !== 'REJECTED')
            .map((rx) => {
              const medName =
                rx.medicationName ||
                (rx.medications && rx.medications.length > 0
                  ? rx.medications.map(m => m.medicationName).filter(Boolean).join(', ')
                  : '');
              const firstMed = rx.medications && rx.medications.length > 0 ? rx.medications[0] : null;
              return {
                prescriptionId: rx.prescriptionId,
                medicationName: medName || 'Unnamed',
                dosage: (firstMed?.dosageStrength ?? rx.dosageStrength)
                  ? `${firstMed?.dosageStrength ?? rx.dosageStrength} ${firstMed?.dosageUnit ?? rx.dosageUnit ?? ''}`.trim()
                  : '-',
                route: firstMed?.route ?? rx.route ?? '-',
                frequency: firstMed?.frequency ?? rx.frequency,
                startDate: firstMed?.startDate ?? rx.startDate ?? '',
                refillsRemaining: rx.refillsRemaining,
                prescriptionStatus: rx.prescriptionStatus,
              };
            });
  const activePrescriptionsCountForOverview =
    patientSummary?.activePrescriptionsCount && patientSummary.activePrescriptionsCount > 0
      ? patientSummary.activePrescriptionsCount
      : activePrescriptionsForOverview.length;
  const latestPrescriptionForOverview = [...allPrescriptions]
    .sort((a, b) => {
      const aTs = new Date((a as any).createdAt || (a as any).createdDate || a.startDate || 0).getTime();
      const bTs = new Date((b as any).createdAt || (b as any).createdDate || b.startDate || 0).getTime();
      return bTs - aTs;
    })[0] || null;
  const recentActiveProblemForOverview = activeProblems.length > 0 ? activeProblems[0] : null;
  const activeProblemsCountForOverview =
    activeProblems.length > 0 ? activeProblems.length : (patientSummary?.activeProblemsCount ?? 0);
  const activeAllergiesCountForOverview =
    activeAllergies.length > 0 ? activeAllergies.length : (patientSummary?.activeAllergiesCount ?? 0);

  if (loading) {
    return <div className="loading">Loading patient...</div>;
  }

  if (error || !patient) {
    return <div className="error-message">{error || 'Patient not found'}</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>{patient.fullName || '—'}</h1>
          <p>MRN: {patient.mrn} | Age: {formatAge(patient.dateOfBirth) ?? '-'}</p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            className="btn-secondary"
            onClick={() => navigate(prescriptionReturnPath || '/hospital/doctor-dashboard')}
          >
            {prescriptionReturnPath ? '← Back to prescription' : 'Doctor Dashboard'}
          </button>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/edit`)}>
            Edit Patient
          </button>
          <button className="btn-secondary" onClick={() => navigate('/hospital/patients')}>
            Back to List
          </button>
        </div>
      </div>

      {patient.patientStatus === 'ACTIVE' && patient.identityCardStatus === 'ISSUED' && (
        <div
          className="info-card"
          style={{
            marginBottom: '16px',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: '10px',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ fontSize: '14px', color: '#475569' }}>
            <strong style={{ color: '#0f172a' }}>Patient identity card</strong>
            {patient.identityCardNumber && (
              <span style={{ marginLeft: '8px' }}>· {patient.identityCardNumber}</span>
            )}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            <button type="button" className="btn-secondary" onClick={handlePrintIdentityCard}>
              Print / reprint card
            </button>
            <button type="button" className="btn-secondary" onClick={handleReplaceIdentityCard}>
              Replace card
            </button>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button
            className={`tab ${activeTab === 'other-personal-info' ? 'active' : ''}`}
            onClick={() => setActiveTab('other-personal-info')}
          >
            Other Personal Information
          </button>
          <button
            className={`tab ${activeTab === 'contacts' ? 'active' : ''}`}
            onClick={() => setActiveTab('contacts')}
          >
            Emergency Contacts ({emergencyContacts.length})
          </button>
          <button
            className={`tab ${activeTab === 'insurance' ? 'active' : ''}`}
            onClick={() => setActiveTab('insurance')}
          >
            Insurance ({insuranceList.length})
          </button>
          <button
            className={`tab ${activeTab === 'consents' ? 'active' : ''}`}
            onClick={() => setActiveTab('consents')}
          >
            Consents ({consents.length})
          </button>
          <button
            className={`tab ${activeTab === 'medical-history' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/medical-history`)}
          >
            Medical History
          </button>
          <button
            className={`tab ${activeTab === 'allergies' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/allergies`)}
          >
            Allergies
          </button>
          <button
            className={`tab ${activeTab === 'immunizations' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/immunizations`)}
          >
            Immunizations
          </button>
          <button
            className={`tab ${activeTab === 'family-history' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/family-history`)}
          >
            Family History
          </button>
          <button
            className={`tab ${activeTab === 'social-history' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/social-history`)}
          >
            Social History
          </button>
          <button
            className={`tab ${activeTab === 'vital-signs' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}
          >
            Vital Signs
          </button>
          <button
            className={`tab ${activeTab === 'clinical-notes' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}
          >
            Clinical Notes
          </button>
          <button
            className={`tab ${activeTab === 'documents' ? 'active' : ''}`}
            onClick={() => setActiveTab('documents')}
          >
            Documents
          </button>
          <button
            className={`tab ${activeTab === 'problems' ? 'active' : ''}`}
            onClick={() => navigate(`/hospital/patients/${id}/problems`)}
          >
            Problem List
          </button>
          <button
            className={`tab ${activeTab === 'prescriptions' ? 'active' : ''}`}
            onClick={() => navigate(patientPrescriptionsPath(id!))}
          >
            Prescriptions
          </button>
        </div>

        <div className="tab-content">
          {activeTab === 'overview' && (
            <div>
              {/* Summary Statistics Cards */}
              {patientSummary && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', marginBottom: '24px' }}>
                  <div className="info-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/hospital/patients/${id}/problems`)}>
                    <div className="info-card-title">Active Problems</div>
                    <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
                      {activeProblemsCountForOverview}
                    </div>
                  </div>
                  <div className="info-card" style={{ cursor: 'pointer' }} onClick={() => navigate(patientPrescriptionsPath(id!))}>
                    <div className="info-card-title">Active Prescriptions</div>
                    <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
                      {activePrescriptionsCountForOverview}
                    </div>
                  </div>
                  <div className="info-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>
                    <div className="info-card-title">Active Allergies</div>
                    <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b' }}>
                      {activeAllergiesCountForOverview}
                    </div>
                  </div>
                  <div className="info-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>
                    <div className="info-card-title">Recent Notes</div>
                    <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8b5cf6' }}>
                      {patientSummary.recentNotesCount}
                    </div>
                  </div>
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' }}>
                {/* Left Column */}
                <div>
                  {/* Personal Information */}
                  <div className="info-card" style={{ marginBottom: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Personal Information</h3>
                      <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/edit`)}>Edit</button>
                    </div>
                    <div className="info-row">
                      <span className="info-label">MRN:</span>
                      <span className="info-value">{patient.mrn}</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Name:</span>
                      <span className="info-value">
                        {patient.fullName || '—'}
                        {patient.preferredName && ` (${patient.preferredName})`}
                      </span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Age:</span>
                      <span className="info-value">{formatAge(patient.dateOfBirth) ?? '-'}</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Gender:</span>
                      <span className="info-value">{formatGenderLabel(patient.gender) || '-'}</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Status:</span>
                      <span className="info-value">
                        <span className={`status-badge status-${patient.patientStatus.toLowerCase()}`}>
                          {patient.patientStatus}
                        </span>
                      </span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Primary Phone:</span>
                      <span className="info-value">{patient.primaryPhone || '-'}</span>
                    </div>
                    <div className="info-row">
                      <span className="info-label">Primary Email:</span>
                      <span className="info-value">{patient.primaryEmail || '-'}</span>
                    </div>
                  </div>

                  {/* Active Problems */}
                  <div className="info-card" style={{ marginBottom: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Active Problems</h3>
                      <div>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/problems`)}>View All</button>
                        <button className="btn-link" style={{ marginLeft: '8px' }} onClick={() => navigate(`/hospital/patients/${id}/problems`)}>+ Add</button>
                      </div>
                    </div>
                    {patientSummary && recentActiveProblemForOverview ? (
                      <div>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Problem</div>
                            <div style={{ fontWeight: '500' }}>{recentActiveProblemForOverview.problemName}</div>
                          </div>
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Status</div>
                            <div style={{ fontWeight: '500' }}>
                              {formatProblemStatusLabel(recentActiveProblemForOverview.status)}
                            </div>
                          </div>
                          {recentActiveProblemForOverview.icd10Code && (
                            <div>
                              <div style={{ fontSize: '12px', color: '#6b7280' }}>ICD-10</div>
                              <div style={{ fontWeight: '500' }}>{recentActiveProblemForOverview.icd10Code}</div>
                            </div>
                          )}
                        </div>
                        {activeProblems.length > 1 && (
                          <div style={{ marginTop: '8px', textAlign: 'center' }}>
                            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/problems`)}>
                              View {activeProblems.length - 1} more...
                            </button>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '16px', textAlign: 'center' }}>
                        <p>No active problems</p>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/problems`)}>Add Problem</button>
                      </div>
                    )}
                  </div>

                  {/* Active Allergies */}
                  <div className="info-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Active Allergies</h3>
                      <div>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>View All</button>
                        <button className="btn-link" style={{ marginLeft: '8px' }} onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>+ Add</button>
                      </div>
                    </div>
                    {patientSummary && activeAllergies.length > 0 ? (
                      <div>
                        {activeAllergies.slice(0, 5).map((allergy) => (
                          <div key={allergy.allergyId} style={{ padding: '8px 0', borderBottom: '1px solid #e5e7eb' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <div>
                                <div style={{ fontWeight: '500' }}>{allergy.allergenName}</div>
                                {allergy.reactionType && (
                                  <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                    Reaction: {formatClinicalEnumLabel(allergy.reactionType)}
                                  </div>
                                )}
                              </div>
                              <span className={`status-badge ${
                                allergy.severity === 'LIFE_THREATENING' ? 'status-deceased' :
                                allergy.severity === 'SEVERE' ? 'status-inactive' : 'status-active'
                              }`}>
                                {formatClinicalEnumLabel(allergy.severity)}
                              </span>
                            </div>
                          </div>
                        ))}
                        {activeAllergies.length > 5 && (
                          <div style={{ marginTop: '8px', textAlign: 'center' }}>
                            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>
                              View {activeAllergies.length - 5} more...
                            </button>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '16px', textAlign: 'center' }}>
                        <p>No known allergies</p>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>Add Allergy</button>
                      </div>
                    )}
                  </div>
                </div>

                {/* Right Column */}
                <div>
                  {/* Latest Vital Signs */}
                  <div className="info-card" style={{ marginBottom: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Latest Vital Signs</h3>
                      <div>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}>View All</button>
                        <button className="btn-link" style={{ marginLeft: '8px' }} onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}>+ Record</button>
                      </div>
                    </div>
                    {latestVitalSignsForOverview ? (
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        {latestVitalSignsForOverview.systolicBp && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Blood Pressure</div>
                            <div style={{ fontWeight: '500' }}>
                              {latestVitalSignsForOverview.systolicBp}/{latestVitalSignsForOverview.diastolicBp} mmHg
                            </div>
                          </div>
                        )}
                        {latestVitalSignsForOverview.heartRate && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Heart Rate</div>
                            <div style={{ fontWeight: '500' }}>{latestVitalSignsForOverview.heartRate} bpm</div>
                          </div>
                        )}
                        {latestVitalSignsForOverview.temperature && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Temperature</div>
                            <div style={{ fontWeight: '500' }}>
                              {latestVitalSignsForOverview.temperature}°{latestVitalSignsForOverview.temperatureUnit || 'F'}
                            </div>
                          </div>
                        )}
                        {latestVitalSignsForOverview.respiratoryRate && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Respiratory Rate</div>
                            <div style={{ fontWeight: '500' }}>{latestVitalSignsForOverview.respiratoryRate} /min</div>
                          </div>
                        )}
                        {latestVitalSignsForOverview.oxygenSaturation && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>O2 Saturation</div>
                            <div style={{ fontWeight: '500' }}>{latestVitalSignsForOverview.oxygenSaturation}%</div>
                          </div>
                        )}
                        {latestVitalSignsForOverview.weight && (
                          <div>
                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Weight</div>
                            <div style={{ fontWeight: '500' }}>
                              {latestVitalSignsForOverview.weight} {latestVitalSignsForOverview.weightUnit || 'lbs'}
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '16px', textAlign: 'center' }}>
                        <p>No vital signs recorded</p>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}>Record Vital Signs</button>
                      </div>
                    )}
                  </div>

                  {/* Active Prescriptions */}
                  <div className="info-card" style={{ marginBottom: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Active Prescriptions</h3>
                      <div>
                        <button className="btn-link" onClick={() => navigate(patientPrescriptionsPath(id!))}>View All</button>
                        <button className="btn-link" style={{ marginLeft: '8px' }} onClick={() => navigate(patientAddPrescriptionPath(id!))}>+ Add</button>
                      </div>
                    </div>
                    {latestPrescriptionForOverview ? (
                      <div>
                        <div style={{ padding: '8px 0', borderBottom: '1px solid #e5e7eb' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontWeight: '500' }}>
                              Rx #{latestPrescriptionForOverview.prescriptionNumber || '—'}
                            </span>
                            <button
                              className="btn-link"
                              onClick={() =>
                                navigate(patientPrescriptionsPath(id!), {
                                  state: { printPrescriptionId: latestPrescriptionForOverview.prescriptionId },
                                })
                              }
                            >
                              View
                            </button>
                          </div>
                          <div style={{ fontSize: '12px', color: '#6b7280' }}>
                            {(latestPrescriptionForOverview.medications && latestPrescriptionForOverview.medications.length > 0
                              ? latestPrescriptionForOverview.medications.map((m) => m.medicationName).filter(Boolean).join(', ')
                              : latestPrescriptionForOverview.medicationName) || '-'}
                          </div>
                        </div>
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '16px', textAlign: 'center' }}>
                        <p>No active prescriptions</p>
                        <button className="btn-link" onClick={() => navigate(patientAddPrescriptionPath(id!))}>Add Prescription</button>
                      </div>
                    )}
                  </div>

                  {/* Recent Clinical Notes */}
                  <div className="info-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                      <h3 className="info-card-title">Recent Clinical Notes</h3>
                      <div>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>View All</button>
                        <button className="btn-link" style={{ marginLeft: '8px' }} onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>+ Add</button>
                      </div>
                    </div>
                    {patientSummary && recentNotes.length > 0 ? (
                      <div>
                        {recentNotes.slice(0, 3).map((note) => (
                          <div key={note.noteId} style={{ padding: '10px 0', borderBottom: '1px solid #e5e7eb' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                              <div>
                                <div style={{ fontSize: '12px', color: '#6b7280' }}>Type</div>
                                <div style={{ fontWeight: '500' }}>{note.noteType}</div>
                              </div>
                              <div>
                                <div style={{ fontSize: '12px', color: '#6b7280' }}>Status</div>
                                <div>
                                  <span className={`status-badge status-${note.noteStatus.toLowerCase()}`}>
                                    {note.noteStatus}
                                  </span>
                                </div>
                              </div>
                              <div>
                                <div style={{ fontSize: '12px', color: '#6b7280' }}>Date</div>
                                <div style={{ fontWeight: '500' }}>{formatDate(note.noteDate)}</div>
                              </div>
                              <div>
                                <div style={{ fontSize: '12px', color: '#6b7280' }}>Chief Complaint</div>
                                <div style={{ fontWeight: '500' }}>{note.chiefComplaint || '-'}</div>
                              </div>
                            </div>
                          </div>
                        ))}
                        {recentNotes.length > 3 && (
                          <div style={{ marginTop: '8px', textAlign: 'center' }}>
                            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>
                              View {recentNotes.length - 3} more...
                            </button>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '16px', textAlign: 'center' }}>
                        <p>No clinical notes</p>
                        <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>Add Note</button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'other-personal-info' && (
            <div className="info-card">
              <h3 className="info-card-title">Other Personal Information</h3>
              <div className="info-row"><span className="info-label">Father&apos;s name:</span><span className="info-value">{patient.fatherName || '-'}</span></div>
              <div className="info-row"><span className="info-label">Mother&apos;s name:</span><span className="info-value">{patient.motherName || '-'}</span></div>
              <div className="info-row"><span className="info-label">Spouse&apos;s name:</span><span className="info-value">{patient.spouseName || '-'}</span></div>
              <div className="info-row"><span className="info-label">Blood group:</span><span className="info-value">{patient.bloodGroup ? (patient.bloodGroup === 'UNKNOWN' ? 'Unknown' : patient.bloodGroup) : '-'}</span></div>
              <div className="info-row"><span className="info-label">Religion:</span><span className="info-value">{patient.religion || '-'}</span></div>
              <div className="info-row"><span className="info-label">Occupation:</span><span className="info-value">{patient.occupation || '-'}</span></div>
              <div className="info-row"><span className="info-label">Introduced by:</span><span className="info-value">{patient.introducedBy || '-'}</span></div>
            </div>
          )}

          {activeTab === 'contacts' && (
            <EmergencyContactsTab
              patientId={id!}
              contacts={emergencyContacts}
              onRefresh={loadPatientData}
              onBackToEhrOverview={() => setActiveTab('overview')}
            />
          )}

          {activeTab === 'insurance' && (
            <InsuranceTab
              patientId={id!}
              insuranceList={insuranceList}
              onRefresh={loadPatientData}
              onBackToEhrOverview={() => setActiveTab('overview')}
            />
          )}

          {activeTab === 'consents' && (
            <ConsentsTab
              patientId={id!}
              consents={consents}
              onRefresh={loadPatientData}
              onBackToEhrOverview={() => setActiveTab('overview')}
            />
          )}

          {activeTab === 'documents' && (
            <DocumentsTab patientId={id!} />
          )}
        </div>
      </div>
    </div>
  );
};

// Emergency Contacts Tab Component
const EmergencyContactsTab: React.FC<{
  patientId: string;
  contacts: EmergencyContact[];
  onRefresh: () => void;
  onBackToEhrOverview: () => void;
}> = ({ patientId, contacts, onRefresh, onBackToEhrOverview }) => {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<EmergencyContact | null>(null);
  const [viewing, setViewing] = useState<EmergencyContact | null>(null);
  const [formData, setFormData] = useState<EmergencyContactRequest>({
    contactName: '',
    relationship: 'Other',
    primaryPhone: '',
    secondaryPhone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    zip: '',
    country: '',
    email: '',
    isPrimary: false,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const primaryDigits = digitsOnlyPhone(formData.primaryPhone);
    if (!primaryDigits) {
      alert('Primary phone must contain at least one digit.');
      return;
    }
    if (formData.secondaryPhone?.trim()) {
      const secondaryDigits = digitsOnlyPhone(formData.secondaryPhone);
      if (!secondaryDigits) {
        alert('Secondary phone may only contain digits, or leave the field empty.');
        return;
      }
    }
    if (!isValidOptionalEmail(formData.email || '')) {
      alert('Please enter a valid email address (for example, name@example.com).');
      return;
    }
    const payload: EmergencyContactRequest = {
      ...formData,
      primaryPhone: primaryDigits,
      secondaryPhone: formData.secondaryPhone?.trim() ? digitsOnlyPhone(formData.secondaryPhone) : undefined,
    };
    try {
      if (editing) {
        await hospitalService.updateEmergencyContact(patientId, editing.contactId, payload);
      } else {
        await hospitalService.createEmergencyContact(patientId, payload);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        contactName: '',
        relationship: 'Other',
        primaryPhone: '',
        secondaryPhone: '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        zip: '',
        country: '',
        email: '',
        isPrimary: false,
      });
      onRefresh();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save emergency contact'));
    }
  };

  const handleEdit = (contact: EmergencyContact) => {
    setEditing(contact);
    setFormData({
      contactName: contact.contactName,
      relationship: contact.relationship,
      primaryPhone: digitsOnlyPhone(contact.primaryPhone),
      secondaryPhone: contact.secondaryPhone ? digitsOnlyPhone(contact.secondaryPhone) : '',
      addressLine1: contact.addressLine1 || '',
      addressLine2: contact.addressLine2 || '',
      city: contact.city || '',
      state: contact.state || '',
      zip: contact.zip || '',
      country: contact.country || '',
      email: contact.email || '',
      isPrimary: contact.isPrimary,
    });
    setShowForm(true);
  };

  const handleDelete = async (contactId: string) => {
    if (!window.confirm('Are you sure you want to delete this emergency contact?')) {
      return;
    }
    try {
      await hospitalService.deleteEmergencyContact(patientId, contactId);
      onRefresh();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete emergency contact'));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Emergency Contacts</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={onBackToEhrOverview}>
            ← Back to Patient
          </button>
          <button className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Emergency Contact
          </button>
        </div>
      </div>

      {viewing && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-content" style={{ maxWidth: '520px', width: 'min(520px, calc(100vw - 32px))' }}>
            <div className="modal-header">
              <h3>Emergency contact</h3>
              <button type="button" className="modal-close" onClick={() => setViewing(null)} aria-label="Close">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="info-card">
                <div className="info-row"><span className="info-label">Name</span><span className="info-value">{viewing.contactName}</span></div>
                <div className="info-row"><span className="info-label">Relationship</span><span className="info-value">{viewing.relationship}</span></div>
                <div className="info-row"><span className="info-label">Primary phone</span><span className="info-value">{viewing.primaryPhone}</span></div>
                <div className="info-row"><span className="info-label">Secondary phone</span><span className="info-value">{viewing.secondaryPhone || '—'}</span></div>
                <div className="info-row"><span className="info-label">Email</span><span className="info-value">{viewing.email || '—'}</span></div>
                <div className="info-row"><span className="info-label">Primary contact</span><span className="info-value">{viewing.isPrimary ? 'Yes' : 'No'}</span></div>
              </div>
              <div className="form-actions" style={{ marginTop: '12px' }}>
                <button type="button" className="btn-secondary" onClick={() => setViewing(null)}>Close</button>
                <button type="button" className="btn-primary" onClick={() => { setViewing(null); handleEdit(viewing); }}>Edit</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Emergency Contact</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Contact Name *</label>
                <input
                  type="text"
                  required
                  value={formData.contactName}
                  onChange={(e) => setFormData({ ...formData, contactName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Relationship *</label>
                <select
                  required
                  value={formData.relationship}
                  onChange={(e) => setFormData({ ...formData, relationship: e.target.value as any })}
                >
                  <option value="Spouse">Spouse</option>
                  <option value="Parent">Parent</option>
                  <option value="Child">Child</option>
                  <option value="Sibling">Sibling</option>
                  <option value="Friend">Friend</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="ec-primary-phone">Primary Phone *</label>
                <input
                  id="ec-primary-phone"
                  type="tel"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  autoComplete="tel-national"
                  required
                  value={formData.primaryPhone}
                  onChange={(e) => setFormData({ ...formData, primaryPhone: digitsOnlyPhone(e.target.value) })}
                  placeholder="Digits only"
                />
              </div>
              <div className="form-group">
                <label htmlFor="ec-secondary-phone">Secondary Phone</label>
                <input
                  id="ec-secondary-phone"
                  type="tel"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  autoComplete="tel-national"
                  value={formData.secondaryPhone}
                  onChange={(e) => setFormData({ ...formData, secondaryPhone: digitsOnlyPhone(e.target.value) })}
                  placeholder="Digits only"
                />
              </div>
              <div className="form-group">
                <label htmlFor="ec-email">Email</label>
                <input
                  id="ec-email"
                  type="text"
                  inputMode="email"
                  autoComplete="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
              <div
                className="form-group"
                style={{
                  gridColumn: '1 / -1',
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  gap: '10px',
                  flexWrap: 'nowrap',
                }}
              >
                <input
                  id="ec-is-primary"
                  type="checkbox"
                  checked={formData.isPrimary}
                  onChange={(e) => setFormData({ ...formData, isPrimary: e.target.checked })}
                  style={{ width: '18px', height: '18px', flexShrink: 0, margin: 0, alignSelf: 'center' }}
                />
                <label htmlFor="ec-is-primary" style={{ margin: 0, fontWeight: 500, cursor: 'pointer', lineHeight: 1.35 }}>
                  Primary Emergency Contact
                </label>
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}

      {contacts.length === 0 ? (
        <div className="empty-state">
          <p>No emergency contacts on file</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table" style={{ minWidth: '860px' }}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Relationship</th>
                <th>Primary Phone</th>
                <th>Email</th>
                <th>Primary</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {contacts.map((contact) => (
                <tr key={contact.contactId}>
                  <td>{contact.contactName}</td>
                  <td>{contact.relationship}</td>
                  <td>{contact.primaryPhone}</td>
                  <td>{contact.email || '-'}</td>
                  <td>{contact.isPrimary ? 'Yes' : 'No'}</td>
                  <td>
                    <div className="action-buttons" style={{ justifyContent: 'center' }}>
                      <button type="button" className="btn-link" onClick={() => setViewing(contact)}>View</button>
                      <button type="button" className="btn-link" onClick={() => handleEdit(contact)}>Edit</button>
                      <button type="button" className="btn-link btn-danger" onClick={() => handleDelete(contact.contactId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// Insurance Tab Component
const InsuranceTab: React.FC<{
  patientId: string;
  insuranceList: Insurance[];
  onRefresh: () => void;
  onBackToEhrOverview: () => void;
}> = ({ patientId, insuranceList, onRefresh, onBackToEhrOverview }) => {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Insurance | null>(null);
  const [viewing, setViewing] = useState<Insurance | null>(null);
  const todayYmd = () => new Date().toISOString().split('T')[0];

  const insuranceFormRef = useRef<HTMLDivElement>(null);

  const emptyInsuranceForm = (): InsuranceRequest => ({
    insuranceType: 'PRIMARY',
    insuranceCompanyName: '',
    policyNumber: '',
    groupNumber: '',
    subscriberName: '',
    subscriberDob: '',
    subscriberRelationship: 'Self',
    effectiveDate: '',
    expirationDate: '',
    copayAmount: undefined,
    verificationStatus: 'Not_Verified',
    insurancePhone: '',
  });

  const [formData, setFormData] = useState<InsuranceRequest>(emptyInsuranceForm());

  const scrollToInsuranceForm = () => {
    requestAnimationFrame(() => {
      insuranceFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  const insuranceErrorMessage = (err: unknown): string => ehrApiErrorMessage(err, 'Failed to save insurance');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.insuranceCompanyName?.trim()) {
      alert('Insurance company is required.');
      return;
    }
    if (!formData.policyNumber?.trim()) {
      alert('Policy number is required.');
      return;
    }
    if (!formData.subscriberName?.trim()) {
      alert('Subscriber name is required.');
      return;
    }
    if (!formData.effectiveDate) {
      alert('Effective date is required.');
      return;
    }
    if (formData.expirationDate?.trim() && formData.expirationDate < formData.effectiveDate) {
      alert('Expiration date cannot be before the effective date.');
      return;
    }
    if (formData.subscriberDob && formData.subscriberDob > todayYmd()) {
      alert('Subscriber date of birth cannot be in the future.');
      return;
    }
    if (formData.subscriberDob && formData.effectiveDate && formData.effectiveDate < formData.subscriberDob) {
      alert('Effective date cannot be before the subscriber date of birth.');
      return;
    }
    const payload: InsuranceRequest = {
      ...formData,
      verificationStatus: normalizeInsuranceVerification(formData.verificationStatus as string),
    };
    try {
      if (editing) {
        await hospitalService.updateInsurance(patientId, editing.insuranceId, payload);
      } else {
        await hospitalService.createInsurance(patientId, payload);
      }
      setShowForm(false);
      setEditing(null);
      setFormData(emptyInsuranceForm());
      onRefresh();
    } catch (err: any) {
      alert(insuranceErrorMessage(err));
    }
  };

  const handleEdit = (insurance: Insurance) => {
    setEditing(insurance);
    setFormData({
      insuranceType: insurance.insuranceType,
      insuranceCompanyName: insurance.insuranceCompanyName || '',
      policyNumber: insurance.policyNumber || '',
      groupNumber: insurance.groupNumber || '',
      subscriberName: insurance.subscriberName || '',
      subscriberDob: insurance.subscriberDob || '',
      subscriberRelationship: insurance.subscriberRelationship || 'Self',
      effectiveDate: insurance.effectiveDate || '',
      expirationDate: insurance.expirationDate || '',
      copayAmount: insurance.copayAmount,
      verificationStatus: normalizeInsuranceVerification(insurance.verificationStatus as string),
      insurancePhone: insurance.insurancePhone || '',
    });
    setShowForm(true);
    scrollToInsuranceForm();
  };

  const openAddInsuranceForm = () => {
    setEditing(null);
    setFormData(emptyInsuranceForm());
    setShowForm(true);
    scrollToInsuranceForm();
  };

  const handleDelete = async (insuranceId: string) => {
    if (!window.confirm('Are you sure you want to delete this insurance information?')) {
      return;
    }
    try {
      await hospitalService.deleteInsurance(patientId, insuranceId);
      onRefresh();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete insurance'));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Insurance Information</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={onBackToEhrOverview}>
            ← Back to Patient
          </button>
          <button className="btn-primary" onClick={openAddInsuranceForm}>
            + Add Insurance
          </button>
        </div>
      </div>

      {showForm && (
        <div ref={insuranceFormRef} className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Insurance</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Insurance Type *</label>
                <select
                  required
                  value={formData.insuranceType}
                  onChange={(e) => setFormData({ ...formData, insuranceType: e.target.value as any })}
                >
                  <option value="PRIMARY">Primary</option>
                  <option value="SECONDARY">Secondary</option>
                  <option value="TERTIARY">Tertiary</option>
                </select>
              </div>
              <div className="form-group">
                <label>Insurance Company *</label>
                <input
                  type="text"
                  required
                  value={formData.insuranceCompanyName}
                  onChange={(e) => setFormData({ ...formData, insuranceCompanyName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Policy Number *</label>
                <input
                  type="text"
                  required
                  value={formData.policyNumber}
                  onChange={(e) => setFormData({ ...formData, policyNumber: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Group Number</label>
                <input
                  type="text"
                  value={formData.groupNumber}
                  onChange={(e) => setFormData({ ...formData, groupNumber: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Subscriber Name *</label>
                <input
                  type="text"
                  required
                  value={formData.subscriberName}
                  onChange={(e) => setFormData({ ...formData, subscriberName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Subscriber DOB</label>
                <input
                  type="date"
                  max={todayYmd()}
                  value={formData.subscriberDob}
                  onChange={(e) => setFormData({ ...formData, subscriberDob: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Subscriber Relationship</label>
                <select
                  value={formData.subscriberRelationship}
                  onChange={(e) => setFormData({ ...formData, subscriberRelationship: e.target.value as any })}
                >
                  <option value="Self">Self</option>
                  <option value="Spouse">Spouse</option>
                  <option value="Child">Child</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>Effective Date *</label>
                <input
                  type="date"
                  required
                  min={formData.subscriberDob || undefined}
                  value={formData.effectiveDate}
                  onChange={(e) => setFormData({ ...formData, effectiveDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Expiration Date</label>
                <input
                  type="date"
                  min={formData.effectiveDate || undefined}
                  value={formData.expirationDate}
                  onChange={(e) => setFormData({ ...formData, expirationDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Copay Amount</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.copayAmount || ''}
                  onChange={(e) => setFormData({ ...formData, copayAmount: e.target.value ? parseFloat(e.target.value) : undefined })}
                />
              </div>
              <div className="form-group">
                <label>Verification Status *</label>
                <select
                  required
                  value={formData.verificationStatus}
                  onChange={(e) => setFormData({ ...formData, verificationStatus: e.target.value as InsuranceRequest['verificationStatus'] })}
                >
                  <option value="Not_Verified">Not verified</option>
                  <option value="Pending">Pending</option>
                  <option value="Verified">Verified</option>
                  <option value="Not_Applicable">Not applicable</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}

      {viewing && (
        <div className="modal-overlay" role="presentation" onClick={() => setViewing(null)}>
          <div className="modal-content" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Insurance Details</h3>
              <button type="button" className="modal-close" onClick={() => setViewing(null)} aria-label="Close">×</button>
            </div>
            <div className="modal-body">
              <div className="info-card">
                <div className="info-row"><span className="info-label">Type</span><span className="info-value">{viewing.insuranceType}</span></div>
                <div className="info-row"><span className="info-label">Company</span><span className="info-value">{viewing.insuranceCompanyName || '—'}</span></div>
                <div className="info-row"><span className="info-label">Policy</span><span className="info-value">{viewing.policyNumber || '—'}</span></div>
                <div className="info-row"><span className="info-label">Subscriber</span><span className="info-value">{viewing.subscriberName || '—'}</span></div>
                <div className="info-row"><span className="info-label">Subscriber DOB</span><span className="info-value">{viewing.subscriberDob ? new Date(viewing.subscriberDob).toLocaleDateString() : '—'}</span></div>
                <div className="info-row"><span className="info-label">Effective</span><span className="info-value">{viewing.effectiveDate ? new Date(viewing.effectiveDate).toLocaleDateString() : '—'}</span></div>
                <div className="info-row"><span className="info-label">Expiration</span><span className="info-value">{viewing.expirationDate ? new Date(viewing.expirationDate).toLocaleDateString() : '—'}</span></div>
                <div className="info-row"><span className="info-label">Verification</span><span className="info-value">{formatInsuranceVerificationLabel(viewing.verificationStatus)}</span></div>
              </div>
              <div className="form-actions" style={{ marginTop: '12px' }}>
                <button type="button" className="btn-secondary" onClick={() => setViewing(null)}>Close</button>
                <button type="button" className="btn-primary" onClick={() => { setViewing(null); handleEdit(viewing); }}>Edit</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {insuranceList.length === 0 ? (
        <div className="empty-state">
          <p>No insurance information on file</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table" style={{ minWidth: '980px' }}>
            <thead>
              <tr>
                <th>Type</th>
                <th>Company</th>
                <th>Policy Number</th>
                <th>Subscriber</th>
                <th>Effective Date</th>
                <th>Expiration Date</th>
                <th>Verification</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {insuranceList.map((insurance) => (
                <tr key={insurance.insuranceId}>
                  <td>{insurance.insuranceType}</td>
                  <td>{insurance.insuranceCompanyName || '-'}</td>
                  <td>{insurance.policyNumber || '-'}</td>
                  <td>{insurance.subscriberName || '-'}</td>
                  <td>{insurance.effectiveDate ? new Date(insurance.effectiveDate).toLocaleDateString() : '-'}</td>
                  <td>{insurance.expirationDate ? new Date(insurance.expirationDate).toLocaleDateString() : '-'}</td>
                  <td>{formatInsuranceVerificationLabel(insurance.verificationStatus)}</td>
                  <td>
                    <div className="action-buttons">
                      <button type="button" className="btn-link" onClick={() => setViewing(insurance)}>View</button>
                      <button className="btn-link" onClick={() => handleEdit(insurance)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(insurance.insuranceId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// Consents Tab Component
const ConsentsTab: React.FC<{
  patientId: string;
  consents: Consent[];
  onRefresh: () => void;
  onBackToEhrOverview: () => void;
}> = ({ patientId, consents, onRefresh, onBackToEhrOverview }) => {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Consent | null>(null);
  const [viewing, setViewing] = useState<Consent | null>(null);
  const [formData, setFormData] = useState<ConsentRequest>({
    consentType: 'HIPAA',
    consentStatus: 'GRANTED',
    consentDate: new Date().toISOString().split('T')[0],
    signature: '',
    expiresDate: '',
    notes: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await hospitalService.updateConsent(patientId, editing.consentId, formData);
      } else {
        await hospitalService.createConsent(patientId, formData);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        consentType: 'HIPAA',
        consentStatus: 'GRANTED',
        consentDate: new Date().toISOString().split('T')[0],
        signature: '',
        expiresDate: '',
        notes: '',
      });
      onRefresh();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save consent'));
    }
  };

  const handleEdit = (consent: Consent) => {
    setEditing(consent);
    setFormData({
      consentType: consent.consentType,
      consentStatus: consent.consentStatus,
      consentDate: consent.consentDate,
      signature: consent.signature || '',
      expiresDate: consent.expiresDate || '',
      notes: consent.notes || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (consentId: string) => {
    if (!window.confirm('Are you sure you want to delete this consent record?')) {
      return;
    }
    try {
      await hospitalService.deleteConsent(patientId, consentId);
      onRefresh();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete consent'));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Consents</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={onBackToEhrOverview}>
            ← Back to Patient
          </button>
          <button className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Consent
          </button>
        </div>
      </div>

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Consent</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Consent Type *</label>
                <select
                  required
                  value={formData.consentType}
                  onChange={(e) => setFormData({ ...formData, consentType: e.target.value as any })}
                >
                  <option value="HIPAA">HIPAA</option>
                  <option value="FINANCIAL">Financial</option>
                  <option value="MARKETING">Marketing</option>
                  <option value="TREATMENT">Treatment</option>
                </select>
              </div>
              <div className="form-group">
                <label>Consent Status *</label>
                <select
                  required
                  value={formData.consentStatus}
                  onChange={(e) => setFormData({ ...formData, consentStatus: e.target.value as any })}
                >
                  <option value="GRANTED">Granted</option>
                  <option value="DENIED">Denied</option>
                  <option value="REVOKED">Revoked</option>
                </select>
              </div>
              <div className="form-group">
                <label>Consent Date *</label>
                <input
                  type="date"
                  required
                  value={formData.consentDate}
                  onChange={(e) => setFormData({ ...formData, consentDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Expires Date</label>
                <input
                  type="date"
                  value={formData.expiresDate}
                  onChange={(e) => setFormData({ ...formData, expiresDate: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Notes</label>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}

      {viewing && (
        <div className="modal-overlay" role="presentation" onClick={() => setViewing(null)}>
          <div className="modal-content" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Consent Details</h3>
              <button type="button" className="modal-close" onClick={() => setViewing(null)} aria-label="Close">×</button>
            </div>
            <div className="modal-body">
              <div className="info-card">
                <div className="info-row"><span className="info-label">Type</span><span className="info-value">{viewing.consentType}</span></div>
                <div className="info-row"><span className="info-label">Status</span><span className="info-value">{viewing.consentStatus}</span></div>
                <div className="info-row"><span className="info-label">Date</span><span className="info-value">{new Date(viewing.consentDate).toLocaleDateString()}</span></div>
                <div className="info-row"><span className="info-label">Expires</span><span className="info-value">{viewing.expiresDate ? new Date(viewing.expiresDate).toLocaleDateString() : '—'}</span></div>
                <div className="info-row"><span className="info-label">Notes</span><span className="info-value">{viewing.notes || '—'}</span></div>
              </div>
              <div className="form-actions" style={{ marginTop: '12px' }}>
                <button type="button" className="btn-secondary" onClick={() => setViewing(null)}>Close</button>
                <button type="button" className="btn-primary" onClick={() => { setViewing(null); handleEdit(viewing); }}>Edit</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {consents.length === 0 ? (
        <div className="empty-state">
          <p>No consent records on file</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Status</th>
                <th>Date</th>
                <th>Expires</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {consents.map((consent) => (
                <tr key={consent.consentId}>
                  <td>{consent.consentType}</td>
                  <td>
                    <span className={`status-badge ${
                      consent.consentStatus === 'GRANTED' ? 'status-active' : 
                      consent.consentStatus === 'DENIED' ? 'status-inactive' : 
                      'status-archived'
                    }`}>
                      {consent.consentStatus}
                    </span>
                  </td>
                  <td>{new Date(consent.consentDate).toLocaleDateString()}</td>
                  <td>{consent.expiresDate ? new Date(consent.expiresDate).toLocaleDateString() : '-'}</td>
                  <td>
                    <div className="action-buttons">
                      <button type="button" className="btn-link" onClick={() => setViewing(consent)}>View</button>
                      <button className="btn-link" onClick={() => handleEdit(consent)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(consent.consentId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// ── Documents Tab ──────────────────────────────────────────────────────────────

const DOCUMENT_TYPES: { value: PatientDocumentType; label: string; icon: string }[] = [
  { value: 'PATHOLOGY_REPORT',   label: 'Pathology Report',   icon: '🔬' },
  { value: 'RADIOLOGY_REPORT',   label: 'Radiology Report',   icon: '🩻' },
  { value: 'LAB_REPORT',         label: 'Lab Report',         icon: '🧪' },
  { value: 'CLINICAL_PHOTO',     label: 'Clinical Photo',     icon: '📷' },
  { value: 'SURGICAL_REPORT',    label: 'Surgical Report',    icon: '🏥' },
  { value: 'PRESCRIPTION',       label: 'Prescription',       icon: '💊' },
  { value: 'REFERRAL_LETTER',    label: 'Referral Letter',    icon: '📨' },
  { value: 'DISCHARGE_SUMMARY',  label: 'Discharge Summary',  icon: '📋' },
  { value: 'CONSENT_FORM',       label: 'Consent Form',       icon: '✍️' },
  { value: 'INSURANCE_DOCUMENT', label: 'Insurance Document', icon: '🛡️' },
  { value: 'VITAL_RECORDS',      label: 'Vital Records',      icon: '📜' },
  { value: 'IDENTITY_DOCUMENT',  label: 'Identity Document',  icon: '🪪' },
  { value: 'EXTERNAL_RECORD',    label: 'External Record',    icon: '🏛️' },
  { value: 'ADVANCE_DIRECTIVE',  label: 'Advance Directive',  icon: '📝' },
  { value: 'OTHER',              label: 'Other',              icon: '📎' },
];
const TYPE_MAP = Object.fromEntries(DOCUMENT_TYPES.map(t => [t.value, t]));

function fmtBytes(n?: number) {
  if (!n) return '—';
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

const DocumentsTab: React.FC<{ patientId: string }> = ({ patientId }) => {
  const [docs, setDocs] = useState<PatientDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<PatientDocumentType | ''>('');

  const [showUpload, setShowUpload] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState({
    documentType: 'OTHER' as PatientDocumentType,
    title: '',
    description: '',
    sourceFacility: '',
    documentDate: '',
    isConfidential: false,
  });

  useEffect(() => { load(); }, [patientId]);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await hospitalService.getPatientDocuments(patientId);
      setDocs(res.data);
    } catch (e: any) {
      setError(ehrApiErrorMessage(e, 'Failed to load documents'));
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (file: File) => {
    setSelectedFile(file);
    setForm(f => ({ ...f, title: file.name }));
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    if (form.documentType === 'OTHER' && !form.description.trim()) {
      setUploadError('Description is required when document type is Other.');
      return;
    }
    setUploading(true);
    setUploadError(null);
    try {
      await hospitalService.uploadPatientDocument(patientId, selectedFile, form.documentType, {
        title: form.title || selectedFile.name,
        description: form.description || undefined,
        sourceFacility: form.sourceFacility || undefined,
        documentDate: form.documentDate || undefined,
        isConfidential: form.isConfidential,
      });
      setShowUpload(false);
      setSelectedFile(null);
      setForm({ documentType: 'OTHER', title: '', description: '', sourceFacility: '', documentDate: '', isConfidential: false });
      await load();
    } catch (e: any) {
      setUploadError(ehrApiErrorMessage(e, 'Upload failed'));
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (doc: PatientDocument) => {
    try {
      const res = await hospitalService.downloadPatientDocument(doc.documentId);
      const url = window.URL.createObjectURL(new Blob([res.data as BlobPart]));
      const a = document.createElement('a'); a.href = url;
      a.download = doc.originalFileName || doc.fileName; a.click();
      window.URL.revokeObjectURL(url);
    } catch (e: unknown) {
      alert(`Download failed: ${ehrApiErrorMessage(e, 'Unknown error')}`);
    }
  };

  const handleDelete = async (docId: string) => {
    if (!window.confirm('Delete this document?')) return;
    try {
      await hospitalService.deletePatientDocument(docId);
      setDocs(prev => prev.filter(d => d.documentId !== docId));
    } catch (e: unknown) {
      alert(`Delete failed: ${ehrApiErrorMessage(e, 'Unknown error')}`);
    }
  };

  const filtered = filterType ? docs.filter(d => d.documentType === filterType) : docs;

  if (loading) return <div className="loading">Loading documents...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div>
      {/* Toolbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <select
            className="form-control"
            style={{ width: '210px' }}
            value={filterType}
            onChange={e => setFilterType(e.target.value as PatientDocumentType | '')}
          >
            <option value="">All document types</option>
            {DOCUMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.icon} {t.label}</option>)}
          </select>
          <span style={{ fontSize: '13px', color: '#6b7280' }}>{filtered.length} document(s)</span>
        </div>
        <button className="btn-primary" onClick={() => setShowUpload(true)}>+ Upload Document</button>
      </div>

      {/* List */}
      {filtered.length === 0 ? (
        <div className="empty-state">
          <p>No documents yet.{filterType ? ' Try clearing the filter.' : ' Upload the first one above.'}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Type</th><th>Title</th><th>File</th><th>Size</th><th>Source / Date</th><th>Uploaded</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(doc => {
                const t = TYPE_MAP[doc.documentType] || { icon: '📎', label: doc.documentType };
                return (
                  <tr key={doc.documentId}>
                    <td>
                      <span title={t.label}>{t.icon}</span>{' '}
                      <small>{t.label}</small>
                      {doc.isConfidential && <span className="badge badge-warning" style={{ marginLeft: 6, fontSize: 11 }}>Confidential</span>}
                    </td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{doc.title}</div>
                      {doc.description && <div style={{ fontSize: 12, color: '#6b7280' }}>{doc.description}</div>}
                    </td>
                    <td style={{ fontSize: 13 }}>{doc.originalFileName || doc.fileName}</td>
                    <td style={{ fontSize: 13 }}>{fmtBytes(doc.fileSize)}</td>
                    <td style={{ fontSize: 13 }}>
                      {doc.sourceFacility || '—'}
                      {doc.documentDate && <div style={{ color: '#9ca3af', fontSize: 12 }}>{new Date(doc.documentDate).toLocaleDateString()}</div>}
                    </td>
                    <td style={{ fontSize: 13 }}>{doc.uploadedDate ? new Date(doc.uploadedDate).toLocaleString() : '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn-link" onClick={() => handleDownload(doc)}>Download</button>
                        <button className="btn-link" style={{ color: '#c0392b' }} onClick={() => handleDelete(doc.documentId)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Upload modal */}
      {showUpload && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
          onClick={() => !uploading && setShowUpload(false)}
        >
          <div className="modal-content" style={{ maxWidth: 540 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Upload Document</h3>
              <button className="modal-close" onClick={() => !uploading && setShowUpload(false)}>×</button>
            </div>
            <div className="modal-body">
              {uploadError && <div className="error-message" style={{ marginBottom: 12 }}>{uploadError}</div>}

              {/* Drop zone */}
              <div
                style={{
                  border: `2px dashed ${dragOver ? '#3498db' : selectedFile ? '#27ae60' : '#d1d5db'}`,
                  borderRadius: 8, padding: 24, textAlign: 'center', cursor: 'pointer', marginBottom: 16,
                  background: dragOver ? '#ebf5fb' : selectedFile ? '#eafaf1' : '#f9fafb', transition: 'all 0.2s',
                }}
                onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={e => { e.preventDefault(); setDragOver(false); const f = e.dataTransfer.files[0]; if (f) handleFileSelect(f); }}
                onClick={() => fileInputRef.current?.click()}
              >
                <input ref={fileInputRef} type="file" style={{ display: 'none' }}
                  accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.webp,.docx,.doc,.xlsx,.xls,.csv,.txt,.mp3,.wav,.mp4,.mov"
                  onChange={e => e.target.files?.[0] && handleFileSelect(e.target.files[0])} />
                {selectedFile ? (
                  <>
                    <div style={{ fontSize: 24, marginBottom: 6 }}>✅</div>
                    <div style={{ fontWeight: 600 }}>{selectedFile.name}</div>
                    <div style={{ fontSize: 13, color: '#6b7280' }}>{fmtBytes(selectedFile.size)} — click to replace</div>
                  </>
                ) : (
                  <>
                    <div style={{ fontSize: 32, marginBottom: 8 }}>📁</div>
                    <div style={{ fontWeight: 500 }}>Drag & drop or click to browse</div>
                    <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>PDF, images, DOCX, CSV, audio, video — max 50 MB</div>
                  </>
                )}
              </div>

              <div className="form-group">
                <label className="form-label">Document Type *</label>
                <select className="form-control" value={form.documentType}
                  onChange={e => setForm(f => ({ ...f, documentType: e.target.value as PatientDocumentType }))}>
                  {DOCUMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.icon} {t.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Title</label>
                <input type="text" className="form-control" placeholder="e.g. CBC Report – March 2026"
                  value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-control" rows={2} placeholder="Optional notes"
                  value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label className="form-label">Source Facility</label>
                  <input type="text" className="form-control" placeholder="e.g. City Lab"
                    value={form.sourceFacility} onChange={e => setForm(f => ({ ...f, sourceFacility: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Document Date</label>
                  <input type="date" className="form-control" value={form.documentDate}
                    onChange={e => setForm(f => ({ ...f, documentDate: e.target.value }))} />
                </div>
              </div>
              <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <input type="checkbox" id="docConfidential" checked={form.isConfidential}
                  onChange={e => setForm(f => ({ ...f, isConfidential: e.target.checked }))} />
                <label htmlFor="docConfidential" style={{ margin: 0, cursor: 'pointer' }}>Mark as confidential</label>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowUpload(false)} disabled={uploading}>Cancel</button>
              <button className="btn-primary" onClick={handleUpload} disabled={!selectedFile || uploading}>
                {uploading ? 'Uploading...' : 'Upload'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PatientDetail;
