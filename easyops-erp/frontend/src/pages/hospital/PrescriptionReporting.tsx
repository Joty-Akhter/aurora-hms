import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { Prescription, PrescriptionRefillRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

interface PrescriptionReport {
  patientId: string;
  patientName: string;
  mrn: string;
  reportPeriod: {
    startDate: string;
    endDate: string;
  };
  summary: {
    totalPrescriptions: number;
    activePrescriptions: number;
    filledPrescriptions: number;
    pendingRefills: number;
    controlledSubstances: number;
  };
  prescriptions: Prescription[];
  refillRequests: PrescriptionRefillRequest[];
}

const PrescriptionReportingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<PrescriptionReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
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
      
      // Get patient info
      const patientResponse = await hospitalService.getPatient(id);
      const patient = patientResponse.data;
      
      // Get all prescriptions
      const prescriptionsResponse = await hospitalService.getPrescriptions(id);
      const prescriptions = prescriptionsResponse.data;
      
      // Get refill requests
      const refillRequestsResponse = await hospitalService.getRefillRequestsByPatient(id);
      const refillRequests = refillRequestsResponse.data;
      
      // Filter prescriptions by date range
      const filteredPrescriptions = prescriptions.filter(p => {
        if (!p.startDate) return false;
        const start = new Date(p.startDate);
        const reportStart = new Date(startDate);
        const reportEnd = new Date(endDate);
        return start >= reportStart && start <= reportEnd;
      });
      
      // Build report
      const reportData: PrescriptionReport = {
        patientId: id,
        patientName: patient.fullName || '',
        mrn: patient.mrn,
        reportPeriod: {
          startDate,
          endDate,
        },
        summary: {
          totalPrescriptions: filteredPrescriptions.length,
          activePrescriptions: filteredPrescriptions.filter(p => 
            ['SENT', 'FILLED', 'PARTIALLY_FILLED'].includes(p.prescriptionStatus || '')
          ).length,
          filledPrescriptions: filteredPrescriptions.filter(p => 
            p.prescriptionStatus === 'FILLED'
          ).length,
          pendingRefills: refillRequests.filter(r => r.requestStatus === 'PENDING').length,
          controlledSubstances: filteredPrescriptions.filter(p => p.isControlledSubstance).length,
        },
        prescriptions: filteredPrescriptions,
        refillRequests: refillRequests.filter(r => {
          const requestDate = new Date(r.requestDate);
          const reportStart = new Date(startDate);
          const reportEnd = new Date(endDate);
          return requestDate >= reportStart && requestDate <= reportEnd;
        }),
      };
      
      setReport(reportData);
    } catch (err: any) {
      console.error('Failed to load prescription report:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load prescription report'));
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return <div className="loading">Loading prescription report...</div>;
  }

  if (error || !report) {
    return <div className="error-message">{error || 'Failed to load prescription report'}</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>Prescription Report</h1>
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
          <div className="info-card-title">Total Prescriptions</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#3b82f6' }}>
            {report.summary.totalPrescriptions}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Active Prescriptions</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
            {report.summary.activePrescriptions}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Filled Prescriptions</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8b5cf6' }}>
            {report.summary.filledPrescriptions}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Pending Refills</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b' }}>
            {report.summary.pendingRefills}
          </div>
        </div>
        <div className="info-card">
          <div className="info-card-title">Controlled Substances</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
            {report.summary.controlledSubstances}
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

      {/* Prescriptions Table */}
      {report.prescriptions.length > 0 && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Prescriptions</h3>
            <button className="btn-link" onClick={() => navigate(`/hospital/patients/${id}/prescriptions`)}>
              View All Prescriptions →
            </button>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Prescription #</th>
                  <th>Medication</th>
                  <th>Dosage</th>
                  <th>Status</th>
                  <th>Start Date</th>
                  <th>Refills Remaining</th>
                  <th>Controlled</th>
                </tr>
              </thead>
              <tbody>
                {report.prescriptions.map((prescription) => (
                  <tr key={prescription.prescriptionId}>
                    <td>{prescription.prescriptionNumber || '-'}</td>
                    <td><strong>{prescription.medicationName}</strong></td>
                    <td>
                      {prescription.dosageStrength && prescription.dosageUnit
                        ? `${prescription.dosageStrength} ${prescription.dosageUnit}`
                        : '-'}
                    </td>
                    <td>
                      <span className={`status-badge status-${prescription.prescriptionStatus?.toLowerCase() || 'unknown'}`}>
                        {prescription.prescriptionStatus || '-'}
                      </span>
                    </td>
                    <td>{formatDate(prescription.startDate)}</td>
                    <td>{prescription.refillsRemaining || 0}</td>
                    <td>
                      {prescription.isControlledSubstance ? (
                        <span className="status-badge" style={{ backgroundColor: '#ef4444', color: 'white' }}>
                          {prescription.schedule || 'CS'}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Refill Requests Table */}
      {report.refillRequests.length > 0 && (
        <div className="info-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="info-card-title">Refill Requests</h3>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Request Date</th>
                  <th>Medication</th>
                  <th>Source</th>
                  <th>Status</th>
                  <th>Refills Requested</th>
                  <th>Urgency</th>
                </tr>
              </thead>
              <tbody>
                {report.refillRequests.map((request) => (
                  <tr key={request.refillRequestId}>
                    <td>{formatDateTime(request.requestDate)}</td>
                    <td><strong>{request.medicationName || '-'}</strong></td>
                    <td>{request.requestSource}</td>
                    <td>
                      <span className={`status-badge status-${request.requestStatus.toLowerCase()}`}>
                        {request.requestStatus}
                      </span>
                    </td>
                    <td>{request.refillsRequested || '-'}</td>
                    <td>
                      {request.urgencyLevel && (
                        <span className="status-badge" style={{
                          backgroundColor: request.urgencyLevel === 'URGENT' ? '#ef4444' : 
                                         request.urgencyLevel === 'HIGH' ? '#f59e0b' : '#6b7280',
                          color: 'white'
                        }}>
                          {request.urgencyLevel}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {report.prescriptions.length === 0 && report.refillRequests.length === 0 && (
        <div className="empty-state">
          <p>No prescription data found for the selected period</p>
        </div>
      )}
    </div>
  );
};

export default PrescriptionReportingPage;
