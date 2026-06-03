import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { PatientSummary as PatientSummaryType } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatClinicalEnumLabel, formatGenderLabel, formatProblemStatusLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PatientSummaryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<PatientSummaryType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadSummary();
    }
  }, [id]);

  const loadSummary = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getPatientSummary(id);
      setSummary(response.data);
    } catch (err: any) {
      console.error('Failed to load patient summary:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load patient summary'));
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString?: string, timeString?: string) => {
    if (!dateString) return '-';
    if (timeString) {
      return `${new Date(dateString).toLocaleDateString()} ${timeString}`;
    }
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading patient summary...</div>;
  }

  if (error || !summary) {
    return <div className="error-message">{error || 'Failed to load patient summary'}</div>;
  }

  const handleExport = () => {
    if (!summary) return;
    
    // Create export data
    const exportData = {
      patient: {
        name: summary.patientName,
        mrn: summary.mrn,
        dateOfBirth: summary.dateOfBirth,
        age: summary.age,
        gender: summary.gender,
      },
      summary: {
        activeProblems: summary.activeProblems,
        activePrescriptions: summary.activePrescriptions,
        activeAllergies: summary.activeAllergies,
        latestVitalSigns: summary.latestVitalSigns,
        recentNotes: summary.recentNotes,
        recentImmunizations: summary.recentImmunizations,
        activeMedications: summary.activeMedications,
      },
      exportedAt: new Date().toISOString(),
    };
    
    // Convert to JSON and download
    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `patient-record-${summary.mrn}-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>{summary.patientName}</h1>
          <p style={{ color: '#6b7280', marginTop: '4px' }}>
            MRN: {summary.mrn} | Age: {formatAge(summary.dateOfBirth) ?? summary.age ?? 'N/A'} | {formatGenderLabel(summary.gender) || 'N/A'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/clinical-report`)}>
            Clinical Report
          </button>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/prescription-report`)}>
            Prescription Report
          </button>
          <button className="btn-secondary" onClick={handleExport}>
            Export Record
          </button>
          <button className="btn-secondary" onClick={handlePrint}>
            Print
          </button>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/timeline`)}>
            View Timeline
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="info-card">
          <div className="info-card-title">Active Problems</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#3b82f6' }}>
            {summary.activeProblemsCount}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Active Prescriptions</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
            {summary.activePrescriptionsCount}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Active Allergies</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
            {summary.activeAllergiesCount}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Recent Notes</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8b5cf6' }}>
            {summary.recentNotesCount}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Recent Immunizations</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b' }}>
            {summary.recentImmunizationsCount}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Active Medications</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#06b6d4' }}>
            {summary.activeMedicationsCount || 0}
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' }}>
        {/* Latest Vital Signs */}
        {summary.latestVitalSigns && (
          <div className="info-card">
            <h3 className="info-card-title">Latest Vital Signs</h3>
            <div className="info-row">
              <span className="info-label">Date & Time:</span>
              <span className="info-value">
                {formatDateTime(summary.latestVitalSigns.measurementDate, summary.latestVitalSigns.measurementTime)}
              </span>
            </div>
            {summary.latestVitalSigns.systolicBp && summary.latestVitalSigns.diastolicBp && (
              <div className="info-row">
                <span className="info-label">Blood Pressure:</span>
                <span className="info-value">
                  {summary.latestVitalSigns.systolicBp}/{summary.latestVitalSigns.diastolicBp} mmHg
                </span>
              </div>
            )}
            {summary.latestVitalSigns.heartRate && (
              <div className="info-row">
                <span className="info-label">Heart Rate:</span>
                <span className="info-value">{summary.latestVitalSigns.heartRate} bpm</span>
              </div>
            )}
            {summary.latestVitalSigns.temperature && (
              <div className="info-row">
                <span className="info-label">Temperature:</span>
                <span className="info-value">
                  {summary.latestVitalSigns.temperature}°{summary.latestVitalSigns.temperatureUnit || 'F'}
                </span>
              </div>
            )}
            {summary.latestVitalSigns.oxygenSaturation && (
              <div className="info-row">
                <span className="info-label">Oxygen Saturation:</span>
                <span className="info-value">{summary.latestVitalSigns.oxygenSaturation}%</span>
              </div>
            )}
            {summary.latestVitalSigns.weight && (
              <div className="info-row">
                <span className="info-label">Weight:</span>
                <span className="info-value">
                  {summary.latestVitalSigns.weight} {summary.latestVitalSigns.weightUnit || 'kg'}
                </span>
              </div>
            )}
            {summary.latestVitalSigns.height && (
              <div className="info-row">
                <span className="info-label">Height:</span>
                <span className="info-value">
                  {(!summary.latestVitalSigns.heightUnit || summary.latestVitalSigns.heightUnit === 'in')
                    ? `${Math.floor(summary.latestVitalSigns.height / 12)} ft ${Math.round((summary.latestVitalSigns.height % 12) * 10) / 10} in`
                    : `${summary.latestVitalSigns.height} ${summary.latestVitalSigns.heightUnit}`}
                </span>
              </div>
            )}
            {summary.latestVitalSigns.bmi && (
              <div className="info-row">
                <span className="info-label">BMI:</span>
                <span className="info-value">{summary.latestVitalSigns.bmi.toFixed(1)}</span>
              </div>
            )}
            <div style={{ marginTop: '12px' }}>
              <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}>
                View All Vital Signs →
              </button>
            </div>
          </div>
        )}

        {/* Active Allergies */}
        {summary.activeAllergies.length > 0 && (
          <div className="info-card">
            <h3 className="info-card-title">Active Allergies</h3>
            <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
              {summary.activeAllergies.map((allergy) => (
                <div key={allergy.allergyId} style={{ marginBottom: '12px', padding: '12px', background: '#fee2e2', borderRadius: '8px' }}>
                  <div style={{ fontWeight: 600, marginBottom: '4px' }}>{allergy.allergenName}</div>
                  <div style={{ fontSize: '14px', color: '#6b7280' }}>
                    {formatClinicalEnumLabel(allergy.allergenType)} — {formatClinicalEnumLabel(allergy.severity)}
                    {allergy.reactionType && ` — ${formatClinicalEnumLabel(allergy.reactionType)}`}
                  </div>
                </div>
              ))}
            </div>
            <div style={{ marginTop: '12px' }}>
              <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/allergies`)}>
                View All Allergies →
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Active Problems */}
      {summary.activeProblems.length > 0 && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Active Problems</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/problems`)}>
              View All Problems →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Problem</th>
                  <th>Type</th>
                  <th>ICD-10</th>
                  <th>Status</th>
                  <th>Onset Date</th>
                </tr>
              </thead>
              <tbody>
                {summary.activeProblems.map((problem) => (
                  <tr key={problem.problemId}>
                    <td><strong>{problem.problemName}</strong></td>
                    <td>{formatClinicalEnumLabel(problem.problemType)}</td>
                    <td>{problem.icd10Code || '-'}</td>
                    <td>{formatProblemStatusLabel(problem.status)}</td>
                    <td>{formatDate(problem.onsetDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Active Prescriptions */}
      {summary.activePrescriptions.length > 0 && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Active Prescriptions</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/prescriptions`)}>
              View All Prescriptions →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Medication</th>
                  <th>Dosage</th>
                  <th>Route</th>
                  <th>Frequency</th>
                  <th>Start Date</th>
                </tr>
              </thead>
              <tbody>
                {summary.activePrescriptions.map((prescription) => (
                  <tr key={prescription.prescriptionId}>
                    <td><strong>{prescription.medicationName}</strong></td>
                    <td>{prescription.dosage || '-'}</td>
                    <td>{prescription.route}</td>
                    <td>{prescription.frequency || '-'}</td>
                    <td>{formatDate(prescription.startDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Recent Clinical Notes */}
      {summary.recentNotes.length > 0 && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Recent Clinical Notes</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>
              View All Notes →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>Type</th>
                  <th>Chief Complaint</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {summary.recentNotes.map((note) => (
                  <tr key={note.noteId}>
                    <td>{formatDateTime(note.noteDate, note.noteTime)}</td>
                    <td>{note.noteType}</td>
                    <td>{note.chiefComplaint || '-'}</td>
                    <td>
                      <span className={`status-badge status-${note.noteStatus.toLowerCase()}`}>
                        {note.noteStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Recent Immunizations */}
      {summary.recentImmunizations.length > 0 && (
        <div className="info-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Recent Immunizations</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/immunizations`)}>
              View All Immunizations →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Vaccine</th>
                  <th>Administration Date</th>
                </tr>
              </thead>
              <tbody>
                {summary.recentImmunizations.map((immunization) => (
                  <tr key={immunization.immunizationId}>
                    <td><strong>{immunization.vaccineName}</strong></td>
                    <td>{formatDate(immunization.administrationDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Active Medications */}
      {summary.activeMedications && summary.activeMedications.length > 0 && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Active Medications</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/medications`)}>
              View All Medications →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Medication</th>
                  <th>Generic Name</th>
                  <th>Dosage</th>
                  <th>Route</th>
                  <th>Frequency</th>
                  <th>Indication</th>
                  <th>Start Date</th>
                </tr>
              </thead>
              <tbody>
                {summary.activeMedications.map((medication) => (
                  <tr key={medication.medicationId}>
                    <td><strong>{medication.medicationName}</strong></td>
                    <td>{medication.genericName || '-'}</td>
                    <td>{medication.dosage || '-'}</td>
                    <td>{medication.route || '-'}</td>
                    <td>{medication.frequency || '-'}</td>
                    <td>{medication.indication || '-'}</td>
                    <td>{formatDate(medication.startDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {summary.activeProblems.length === 0 && 
       summary.activePrescriptions.length === 0 && 
       summary.activeAllergies.length === 0 && 
       summary.recentNotes.length === 0 && 
       summary.recentImmunizations.length === 0 && 
       (!summary.activeMedications || summary.activeMedications.length === 0) && (
        <div className="empty-state">
          <p>No summary data available</p>
        </div>
      )}
    </div>
  );
};

export default PatientSummaryPage;
