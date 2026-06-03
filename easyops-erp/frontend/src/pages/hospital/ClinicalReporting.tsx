import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

interface ClinicalReport {
  patientId: string;
  patientName: string;
  mrn: string;
  reportPeriod: {
    startDate: string;
    endDate: string;
  };
  vitalSignsActivity: {
    totalMeasurements: number;
    abnormalCount: number;
    criticalCount: number;
  };
  clinicalNotesActivity: {
    totalNotes: number;
    signedNotes: number;
    draftNotes: number;
  };
  prescriptionActivity: {
    prescriptionsCreated: number;
    refillsDispensed: number;
  };
}

const ClinicalReportingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<ClinicalReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );

  useEffect(() => {
    if (id) {
      loadReport();
    }
  }, [id, startDate, endDate]);

  const loadReport = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      // Get summary data (can be enhanced with dedicated reporting endpoint)
      const summaryResponse = await hospitalService.getPatientSummary(id);
      const summary = summaryResponse.data;
      
      // Get vital signs summary
      const vitalSignsResponse = await hospitalService.getVitalSignsSummary(id);
      const vitalSigns = vitalSignsResponse.data;
      
      // Get clinical notes
      const notesResponse = await hospitalService.getCurrentVersionNotes(id);
      const notes = notesResponse.data;
      
      // Get prescriptions
      const prescriptionsResponse = await hospitalService.getPrescriptions(id);
      const prescriptions = prescriptionsResponse.data;
      
      // Build report
      const reportData: ClinicalReport = {
        patientId: id,
        patientName: summary.patientName,
        mrn: summary.mrn,
        reportPeriod: {
          startDate,
          endDate,
        },
        vitalSignsActivity: {
          totalMeasurements: vitalSigns.totalMeasurements,
          abnormalCount: vitalSigns.abnormalCount,
          criticalCount: vitalSigns.criticalCount,
        },
        clinicalNotesActivity: {
          totalNotes: notes.length,
          signedNotes: notes.filter(n => n.noteStatus === 'SIGNED').length,
          draftNotes: notes.filter(n => n.noteStatus === 'DRAFT').length,
        },
        prescriptionActivity: {
          prescriptionsCreated: prescriptions.length,
          refillsDispensed: 0, // Can be enhanced with refill data
        },
      };
      
      setReport(reportData);
    } catch (err: any) {
      console.error('Failed to load clinical report:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load clinical report'));
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading clinical report...</div>;
  }

  if (error || !report) {
    return <div className="error-message">{error || 'Failed to load clinical report'}</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>Clinical Activity Report</h1>
          <p>{report.patientName} (MRN: {report.mrn})</p>
          <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>
            Report Period: {formatDate(report.reportPeriod.startDate)} - {formatDate(report.reportPeriod.endDate)}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            Back to Overview
          </button>
          <button className="btn-secondary" onClick={handlePrint}>
            Print Report
          </button>
        </div>
      </div>

      {/* Report Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="info-card">
          <div className="info-card-title">Vital Signs Measurements</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#3b82f6' }}>
            {report.vitalSignsActivity.totalMeasurements}
          </div>
          <div style={{ fontSize: '14px', color: '#6b7280', marginTop: '8px' }}>
            {report.vitalSignsActivity.abnormalCount} abnormal, {report.vitalSignsActivity.criticalCount} critical
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Clinical Notes</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8b5cf6' }}>
            {report.clinicalNotesActivity.totalNotes}
          </div>
          <div style={{ fontSize: '14px', color: '#6b7280', marginTop: '8px' }}>
            {report.clinicalNotesActivity.signedNotes} signed, {report.clinicalNotesActivity.draftNotes} draft
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Prescriptions</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
            {report.prescriptionActivity.prescriptionsCreated}
          </div>
          <div style={{ fontSize: '14px', color: '#6b7280', marginTop: '8px' }}>
            {report.prescriptionActivity.refillsDispensed} refills dispensed
          </div>
        </div>
      </div>

      {/* Date Range Filter */}
      <div className="filters-section" style={{ marginBottom: '24px' }}>
        <div className="filter-row">
          <label>Start Date:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="search-input"
            style={{ width: 'auto' }}
          />
          <label>End Date:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="search-input"
            style={{ width: 'auto' }}
          />
          <button className="btn-secondary" onClick={loadReport}>
            Refresh Report
          </button>
        </div>
      </div>

      {/* Detailed Sections */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' }}>
        <div className="info-card">
          <h3 className="info-card-title">Vital Signs Activity</h3>
          <div className="info-row">
            <span className="info-label">Total Measurements:</span>
            <span className="info-value">{report.vitalSignsActivity.totalMeasurements}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Abnormal Values:</span>
            <span className="info-value" style={{ color: '#f59e0b' }}>
              {report.vitalSignsActivity.abnormalCount}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Critical Values:</span>
            <span className="info-value" style={{ color: '#ef4444' }}>
              {report.vitalSignsActivity.criticalCount}
            </span>
          </div>
          <div style={{ marginTop: '12px' }}>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/vital-signs`)}>
              View All Vital Signs →
            </button>
          </div>
        </div>

        <div className="info-card">
          <h3 className="info-card-title">Clinical Notes Activity</h3>
          <div className="info-row">
            <span className="info-label">Total Notes:</span>
            <span className="info-value">{report.clinicalNotesActivity.totalNotes}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Signed Notes:</span>
            <span className="info-value" style={{ color: '#10b981' }}>
              {report.clinicalNotesActivity.signedNotes}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Draft Notes:</span>
            <span className="info-value" style={{ color: '#f59e0b' }}>
              {report.clinicalNotesActivity.draftNotes}
            </span>
          </div>
          <div style={{ marginTop: '12px' }}>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/clinical-notes`)}>
              View All Notes →
            </button>
          </div>
        </div>
      </div>

      <div className="info-card">
        <h3 className="info-card-title">Prescription Activity</h3>
        <div className="info-row">
          <span className="info-label">Prescriptions Created:</span>
          <span className="info-value">{report.prescriptionActivity.prescriptionsCreated}</span>
        </div>
        <div className="info-row">
          <span className="info-label">Refills Dispensed:</span>
          <span className="info-value">{report.prescriptionActivity.refillsDispensed}</span>
        </div>
        <div style={{ marginTop: '12px' }}>
          <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/prescriptions`)}>
            View All Prescriptions →
          </button>
        </div>
      </div>
    </div>
  );
};

export default ClinicalReportingPage;
