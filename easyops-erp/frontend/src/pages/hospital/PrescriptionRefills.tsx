import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import hospitalService, { 
  PrescriptionRefillRequest,
  PrescriptionRefill,
  RefillRequestRequest,
  RefillApprovalRequest,
  RefillDenialRequest,
  RefillModificationRequest,
  PrescriptionRefillRequestForRecording,
  Prescription
} from '../../services/hospitalService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PrescriptionRefillsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [refillRequests, setRefillRequests] = useState<PrescriptionRefillRequest[]>([]);
  const [refills, setRefills] = useState<PrescriptionRefill[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showRequestForm, setShowRequestForm] = useState(false);
  const [showRecordRefillForm, setShowRecordRefillForm] = useState(false);
  const [viewing, setViewing] = useState<PrescriptionRefillRequest | null>(null);
  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showDenyDialog, setShowDenyDialog] = useState(false);
  const [showModifyDialog, setShowModifyDialog] = useState(false);
  const [requestToAction, setRequestToAction] = useState<PrescriptionRefillRequest | null>(null);
  const [activeTab, setActiveTab] = useState<'requests' | 'refills'>('requests');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const quantityUnitOptions = [
    'tablet(s)',
    'capsule(s)',
    'mL',
    'L',
    'drop(s)',
    'puff(s)',
    'vial(s)',
    'ampule(s)',
    'syringe(s)',
    'patch(es)',
    'packet(s)',
    'bottle(s)',
    'tube(s)',
    'suppository(ies)',
    'unit(s)',
  ];
  
  const [requestFormData, setRequestFormData] = useState<RefillRequestRequest>({
    prescriptionId: '',
    requestSource: 'PHARMACY',
    pharmacyName: '',
    pharmacyNpi: '',
    pharmacyPhone: '',
    refillsRequested: 1,
    urgencyLevel: 'MEDIUM',
    notes: '',
  });

  const [approvalData, setApprovalData] = useState<RefillApprovalRequest>({
    approvalNotes: '',
    refillsApproved: undefined,
  });

  const [denialData, setDenialData] = useState<RefillDenialRequest>({
    denialReason: '',
  });

  const [modificationData, setModificationData] = useState<RefillModificationRequest>({
    refillsApproved: 1,
    modificationNotes: '',
  });

  const [refillFormData, setRefillFormData] = useState<PrescriptionRefillRequestForRecording>({
    prescriptionId: '',
    refillRequestId: '',
    refillDate: new Date().toISOString().split('T')[0],
    quantityDispensed: undefined,
    quantityUnit: '',
    pharmacyName: '',
    pharmacyNpi: '',
    notes: '',
    lotNumber: '',
    expirationDate: '',
  });

  const digitsOnly = (value: string) => value.replace(/\D/g, '');

  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id, filterStatus]);

  const isRefillEligible = (prescription: Prescription): boolean => {
    // Refill can only proceed for prescriptions that are still clinically/operationally open.
    const excludedStatuses = new Set(['CANCELLED', 'EXPIRED', 'REJECTED']);
    const hasRefillsRemaining = (prescription.refillsRemaining ?? 0) > 0;
    return !excludedStatuses.has(prescription.prescriptionStatus) && hasRefillsRemaining;
  };

  const prescriptionLabel = (p: Prescription): string => {
    const medName =
      p.medicationName ||
      (p.medications && p.medications.length > 0 ? p.medications.map(m => m.medicationName).filter(Boolean).join(', ') : '') ||
      'Unnamed';
    return `${medName} — ${p.prescriptionNumber || p.prescriptionId.slice(0, 8)}`;
  };

  const loadData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      const [requestsRes, refillsRes, prescriptionsRes] = await Promise.all([
        hospitalService.getRefillRequestsByPatient(id),
        hospitalService.getRefillsByPatient(id),
        hospitalService.getActivePrescriptions(id)
      ]);
      
      let filteredRequests = requestsRes.data;
      if (filterStatus) {
        filteredRequests = filteredRequests.filter(r => r.requestStatus === filterStatus);
      }
      
      setRefillRequests(filteredRequests);
      setRefills(refillsRes.data);
      let refillEligiblePrescriptions = prescriptionsRes.data.filter(isRefillEligible);

      // Fallback: if "active" endpoint returns none, try all prescriptions and filter locally.
      if (refillEligiblePrescriptions.length === 0) {
        const allPrescriptionsRes = await hospitalService.getPrescriptions(id);
        refillEligiblePrescriptions = allPrescriptionsRes.data.filter(isRefillEligible);
      }

      setPrescriptions(refillEligiblePrescriptions);
    } catch (err: any) {
      console.error('Failed to load refill data:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load refill data'));
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      await hospitalService.createRefillRequest(requestFormData);
      setShowRequestForm(false);
      resetRequestForm();
      loadData();
    } catch (err: any) {
      const status = err.response?.status;
      const details = ehrApiErrorMessage(err, '');
      if (status === 422 && /no refills remaining/i.test(details || '')) {
        alert('This prescription has no refills remaining. Please select another prescription or create a new one.');
      } else {
        alert(details || 'Failed to create refill request');
      }
    }
  };

  const resetRequestForm = () => {
    setRequestFormData({
      prescriptionId: '',
      requestSource: 'PHARMACY',
      pharmacyName: '',
      pharmacyNpi: '',
      pharmacyPhone: '',
      refillsRequested: 1,
      urgencyLevel: 'MEDIUM',
      notes: '',
    });
  };

  const handleApprove = async () => {
    if (!requestToAction) return;
    try {
      await hospitalService.approveRefillRequest(requestToAction.refillRequestId, approvalData);
      setShowApproveDialog(false);
      setRequestToAction(null);
      setApprovalData({ approvalNotes: '', refillsApproved: undefined });
      loadData();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to approve refill request'));
    }
  };

  const handleDeny = async () => {
    if (!requestToAction) return;
    try {
      await hospitalService.denyRefillRequest(requestToAction.refillRequestId, denialData);
      setShowDenyDialog(false);
      setRequestToAction(null);
      setDenialData({ denialReason: '' });
      loadData();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to deny refill request'));
    }
  };

  const handleModify = async () => {
    if (!requestToAction) return;
    try {
      await hospitalService.modifyRefillRequest(requestToAction.refillRequestId, modificationData);
      setShowModifyDialog(false);
      setRequestToAction(null);
      setModificationData({ refillsApproved: 1, modificationNotes: '' });
      loadData();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to modify refill request'));
    }
  };

  const handleRecordRefill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      const payload: PrescriptionRefillRequestForRecording = {
        prescriptionId: refillFormData.prescriptionId,
        refillDate: refillFormData.refillDate,
        ...(refillFormData.refillRequestId ? { refillRequestId: refillFormData.refillRequestId } : {}),
        ...(refillFormData.quantityDispensed !== undefined ? { quantityDispensed: refillFormData.quantityDispensed } : {}),
        ...(refillFormData.quantityUnit ? { quantityUnit: refillFormData.quantityUnit } : {}),
        ...(refillFormData.pharmacyName ? { pharmacyName: refillFormData.pharmacyName } : {}),
        ...(refillFormData.pharmacyNpi ? { pharmacyNpi: refillFormData.pharmacyNpi } : {}),
        ...(refillFormData.notes ? { notes: refillFormData.notes } : {}),
        ...(refillFormData.lotNumber ? { lotNumber: refillFormData.lotNumber } : {}),
        ...(refillFormData.expirationDate ? { expirationDate: refillFormData.expirationDate } : {}),
      };
      await hospitalService.recordRefill(payload);
      setShowRecordRefillForm(false);
      resetRefillForm();
      loadData();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to record refill'));
    }
  };

  const resetRefillForm = () => {
    setRefillFormData({
      prescriptionId: '',
      refillRequestId: '',
      refillDate: new Date().toISOString().split('T')[0],
      quantityDispensed: undefined,
      quantityUnit: '',
      pharmacyName: '',
      pharmacyNpi: '',
      notes: '',
      lotNumber: '',
      expirationDate: '',
    });
  };

  const handleView = async (refillRequestId: string) => {
    try {
      const response = await hospitalService.getRefillRequest(refillRequestId);
      setViewing(response.data);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load refill request'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };

  if (loading && !refillRequests.length && !refills.length) {
    return <div className="loading">Loading refill data...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px', gap: '12px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/prescriptions`)}>
            Prescriptions
          </button>
          <h3 style={{ margin: 0 }}>Prescription Refills</h3>
        </div>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button className="btn-secondary" onClick={() => setShowRequestForm(true)}>
            + Request Refill
          </button>
          <button
            className="btn-secondary"
            onClick={() => setShowRecordRefillForm(true)}
            disabled={prescriptions.length === 0}
            title={prescriptions.length === 0 ? 'Create a prescription first' : undefined}
          >
            + Record Refill
          </button>
          {prescriptions.length === 0 && (
            <button className="btn-primary" onClick={() => navigate(`/hospital/patients/${id}/prescriptions`)}>
              + Create Prescription
            </button>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Tabs */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'requests' ? 'active' : ''}`}
            onClick={() => setActiveTab('requests')}
          >
            Refill Requests ({refillRequests.length})
          </button>
          <button
            className={`tab ${activeTab === 'refills' ? 'active' : ''}`}
            onClick={() => setActiveTab('refills')}
          >
            Refill History ({refills.length})
          </button>
        </div>
      </div>

      {/* Filters */}
      {activeTab === 'requests' && (
        <div className="filters-section">
          <div className="filter-row">
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="filter-select"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="DENIED">Denied</option>
              <option value="MODIFIED">Modified</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            <button className="btn-secondary" onClick={() => { setFilterStatus(''); loadData(); }}>Clear</button>
          </div>
        </div>
      )}

      {/* Create Request Form */}
      {showRequestForm && (
        <div className="form-container">
          <h3>Create Refill Request</h3>
          <form onSubmit={handleCreateRequest}>
            <div className="form-grid">
              <div className="form-group">
                <label>Prescription *</label>
                <select
                  required
                  value={requestFormData.prescriptionId}
                  onChange={(e) => setRequestFormData({ ...requestFormData, prescriptionId: e.target.value })}
                >
                  <option value="">Select Prescription</option>
                  {prescriptions.map(p => (
                    <option key={p.prescriptionId} value={p.prescriptionId}>
                      {prescriptionLabel(p)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Request Source</label>
                <select
                  value={requestFormData.requestSource}
                  onChange={(e) => setRequestFormData({ ...requestFormData, requestSource: e.target.value as any })}
                >
                  <option value="PHARMACY">Pharmacy</option>
                  <option value="PATIENT">Patient</option>
                  <option value="PROVIDER">Provider</option>
                  <option value="SYSTEM">System</option>
                </select>
              </div>
              <div className="form-group">
                <label>Refills Requested</label>
                <input
                  type="number"
                  min="1"
                  value={requestFormData.refillsRequested || 1}
                  onChange={(e) => setRequestFormData({ ...requestFormData, refillsRequested: parseInt(e.target.value) || 1 })}
                />
              </div>
              <div className="form-group">
                <label>Urgency Level</label>
                <select
                  value={requestFormData.urgencyLevel}
                  onChange={(e) => setRequestFormData({ ...requestFormData, urgencyLevel: e.target.value as any })}
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="URGENT">Urgent</option>
                </select>
              </div>
              <div className="form-group">
                <label>Pharmacy Name</label>
                <input
                  type="text"
                  value={requestFormData.pharmacyName}
                  onChange={(e) => setRequestFormData({ ...requestFormData, pharmacyName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Pharmacy NPI</label>
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={requestFormData.pharmacyNpi}
                  onChange={(e) => setRequestFormData({ ...requestFormData, pharmacyNpi: digitsOnly(e.target.value) })}
                />
              </div>
              <div className="form-group">
                <label>Pharmacy Phone</label>
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={requestFormData.pharmacyPhone}
                  onChange={(e) => setRequestFormData({ ...requestFormData, pharmacyPhone: digitsOnly(e.target.value) })}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Notes</label>
                <textarea
                  value={requestFormData.notes}
                  onChange={(e) => setRequestFormData({ ...requestFormData, notes: e.target.value })}
                  rows={3}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowRequestForm(false); resetRequestForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Create Request</button>
            </div>
          </form>
        </div>
      )}

      {/* Record Refill Form */}
      {showRecordRefillForm && (
        <div className="form-container">
          <h3>Record Prescription Refill</h3>
          <form onSubmit={handleRecordRefill}>
            <div className="form-grid">
              <div className="form-group">
                <label>Prescription *</label>
                <select
                  required
                  value={refillFormData.prescriptionId}
                  onChange={(e) => setRefillFormData({ ...refillFormData, prescriptionId: e.target.value })}
                >
                  <option value="">Select Prescription</option>
                  {prescriptions.map(p => (
                    <option key={p.prescriptionId} value={p.prescriptionId}>
                      {prescriptionLabel(p)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Refill Date *</label>
                <input
                  type="date"
                  required
                  value={refillFormData.refillDate}
                  onChange={(e) => setRefillFormData({ ...refillFormData, refillDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Quantity Dispensed</label>
                <input
                  type="number"
                  step="0.1"
                  value={refillFormData.quantityDispensed || ''}
                  onChange={(e) => setRefillFormData({ ...refillFormData, quantityDispensed: e.target.value ? parseFloat(e.target.value) : undefined })}
                />
              </div>
              <div className="form-group">
                <label>Quantity Unit</label>
                <input
                  type="text"
                  list="quantity-unit-options"
                  value={refillFormData.quantityUnit}
                  onChange={(e) => setRefillFormData({ ...refillFormData, quantityUnit: e.target.value })}
                  placeholder="Select or type unit"
                />
                <datalist id="quantity-unit-options">
                  {quantityUnitOptions.map((unit) => (
                    <option key={unit} value={unit} />
                  ))}
                </datalist>
              </div>
              <div className="form-group">
                <label>Pharmacy Name</label>
                <input
                  type="text"
                  value={refillFormData.pharmacyName}
                  onChange={(e) => setRefillFormData({ ...refillFormData, pharmacyName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Pharmacy NPI</label>
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={refillFormData.pharmacyNpi}
                  onChange={(e) => setRefillFormData({ ...refillFormData, pharmacyNpi: digitsOnly(e.target.value) })}
                />
              </div>
              <div className="form-group">
                <label>Lot Number</label>
                <input
                  type="text"
                  value={refillFormData.lotNumber}
                  onChange={(e) => setRefillFormData({ ...refillFormData, lotNumber: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Expiration Date</label>
                <input
                  type="date"
                  value={refillFormData.expirationDate}
                  onChange={(e) => setRefillFormData({ ...refillFormData, expirationDate: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Notes</label>
                <textarea
                  value={refillFormData.notes}
                  onChange={(e) => setRefillFormData({ ...refillFormData, notes: e.target.value })}
                  rows={3}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowRecordRefillForm(false); resetRefillForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Record Refill</button>
            </div>
          </form>
        </div>
      )}

      {/* Approve Dialog */}
      {showApproveDialog && requestToAction && portalLayoutOverlay(
        <div className={LAYOUT_OVERLAY_DETECT_CLASS} style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Approve Refill Request</h3>
            <p><strong>{requestToAction.medicationName}</strong></p>
            <p>Requested: {requestToAction.refillsRequested} refill(s)</p>
            <div className="form-group">
              <label>Refills Approved</label>
              <input
                type="number"
                min="1"
                value={approvalData.refillsApproved || requestToAction.refillsRequested || 1}
                onChange={(e) => setApprovalData({ ...approvalData, refillsApproved: parseInt(e.target.value) || undefined })}
              />
              <small>Leave empty to approve requested amount</small>
            </div>
            <div className="form-group">
              <label>Approval Notes</label>
              <textarea
                value={approvalData.approvalNotes}
                onChange={(e) => setApprovalData({ ...approvalData, approvalNotes: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowApproveDialog(false); setRequestToAction(null); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleApprove}>Approve</button>
            </div>
          </div>
        </div>
      )}

      {/* Deny Dialog */}
      {showDenyDialog && requestToAction && portalLayoutOverlay(
        <div className={LAYOUT_OVERLAY_DETECT_CLASS} style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Deny Refill Request</h3>
            <p><strong>{requestToAction.medicationName}</strong></p>
            <div className="form-group">
              <label>Denial Reason *</label>
              <textarea
                required
                value={denialData.denialReason}
                onChange={(e) => setDenialData({ ...denialData, denialReason: e.target.value })}
                rows={4}
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowDenyDialog(false); setRequestToAction(null); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleDeny}>Deny Request</button>
            </div>
          </div>
        </div>
      )}

      {/* Modify Dialog */}
      {showModifyDialog && requestToAction && portalLayoutOverlay(
        <div className={LAYOUT_OVERLAY_DETECT_CLASS} style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Modify Refill Request</h3>
            <p><strong>{requestToAction.medicationName}</strong></p>
            <p>Original Request: {requestToAction.refillsRequested} refill(s)</p>
            <div className="form-group">
              <label>Refills Approved *</label>
              <input
                type="number"
                required
                min="1"
                value={modificationData.refillsApproved}
                onChange={(e) => setModificationData({ ...modificationData, refillsApproved: parseInt(e.target.value) || 1 })}
              />
            </div>
            <div className="form-group">
              <label>Modification Notes</label>
              <textarea
                value={modificationData.modificationNotes}
                onChange={(e) => setModificationData({ ...modificationData, modificationNotes: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowModifyDialog(false); setRequestToAction(null); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleModify}>Modify & Approve</button>
            </div>
          </div>
        </div>
      )}

      {/* View Request Dialog */}
      {viewing && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '760px', width: 'min(760px, calc(100vw - 32px))' }}>
            <div className="modal-header">
              <h3 style={{ margin: 0 }}>Refill Request Details</h3>
              <button className="modal-close" onClick={() => setViewing(null)} aria-label="Close">×</button>
            </div>
            <div className="modal-body">
            <div className="info-card">
              <div className="info-row">
                <span className="info-label">Medication:</span>
                <span className="info-value"><strong>{viewing.medicationName}</strong></span>
              </div>
              <div className="info-row">
                <span className="info-label">Prescription #:</span>
                <span className="info-value">{viewing.prescriptionNumber || '-'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Status:</span>
                <span className="info-value">
                  <span className={`status-badge status-${viewing.requestStatus.toLowerCase()}`}>
                    {viewing.requestStatus}
                  </span>
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Request Source:</span>
                <span className="info-value">{viewing.requestSource}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Refills Requested:</span>
                <span className="info-value">{viewing.refillsRequested || 0}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Refills Remaining:</span>
                <span className="info-value">{viewing.refillsRemaining || 0}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Urgency:</span>
                <span className="info-value">
                  <span className={`status-badge ${
                    viewing.urgencyLevel === 'URGENT' ? 'status-deceased' :
                    viewing.urgencyLevel === 'HIGH' ? 'status-inactive' :
                    'status-active'
                  }`}>
                    {viewing.urgencyLevel}
                  </span>
                </span>
              </div>
              {viewing.daysSinceLastFill !== undefined && (
                <div className="info-row">
                  <span className="info-label">Days Since Last Fill:</span>
                  <span className="info-value">{viewing.daysSinceLastFill}</span>
                </div>
              )}
              {viewing.approvedDate && (
                <div className="info-row">
                  <span className="info-label">Approved:</span>
                  <span className="info-value">{formatDateTime(viewing.approvedDate)}</span>
                </div>
              )}
              {viewing.deniedDate && (
                <div className="info-row">
                  <span className="info-label">Denied:</span>
                  <span className="info-value">{formatDateTime(viewing.deniedDate)}</span>
                </div>
              )}
              {viewing.denialReason && (
                <div className="info-row">
                  <span className="info-label">Denial Reason:</span>
                  <span className="info-value" style={{ whiteSpace: 'pre-wrap' }}>{viewing.denialReason}</span>
                </div>
              )}
              {viewing.notes && (
                <div className="info-row">
                  <span className="info-label">Notes:</span>
                  <span className="info-value" style={{ whiteSpace: 'pre-wrap' }}>{viewing.notes}</span>
                </div>
              )}
            </div>
            {viewing.refills && viewing.refills.length > 0 && (
              <div className="info-card" style={{ marginTop: '16px' }}>
                <h4 className="info-card-title">Refills</h4>
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Refill #</th>
                        <th>Date</th>
                        <th>Quantity</th>
                        <th>Pharmacy</th>
                      </tr>
                    </thead>
                    <tbody>
                      {viewing.refills.map((refill) => (
                        <tr key={refill.refillId}>
                          <td>{refill.refillNumber || '-'}</td>
                          <td>{formatDate(refill.refillDate)}</td>
                          <td>{refill.quantityDispensed || '-'} {refill.quantityUnit || ''}</td>
                          <td>{refill.pharmacyName || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setViewing(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Refill Requests Tab */}
      {activeTab === 'requests' && (
        <>
          {refillRequests.length === 0 ? (
            <div className="empty-state">
              <p>No refill requests found</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Medication</th>
                    <th>Prescription #</th>
                    <th>Request Source</th>
                    <th>Refills Requested</th>
                    <th>Refills Remaining</th>
                    <th>Days Since Last Fill</th>
                    <th>Urgency</th>
                    <th>Status</th>
                    <th>Request Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {refillRequests.map((request) => (
                    <tr key={request.refillRequestId}>
                      <td><strong>{request.medicationName || '-'}</strong></td>
                      <td>{request.prescriptionNumber || '-'}</td>
                      <td>{request.requestSource}</td>
                      <td>{request.refillsRequested || 0}</td>
                      <td>{request.refillsRemaining || 0}</td>
                      <td>{request.daysSinceLastFill !== undefined ? request.daysSinceLastFill : '-'}</td>
                      <td>
                        <span className={`status-badge ${
                          request.urgencyLevel === 'URGENT' ? 'status-deceased' :
                          request.urgencyLevel === 'HIGH' ? 'status-inactive' :
                          'status-active'
                        }`}>
                          {request.urgencyLevel || 'MEDIUM'}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-${request.requestStatus.toLowerCase()}`}>
                          {request.requestStatus}
                        </span>
                      </td>
                      <td>{formatDateTime(request.requestDate)}</td>
                      <td>
                        <div className="action-buttons">
                          <button className="btn-link" onClick={() => handleView(request.refillRequestId)}>View</button>
                          {request.requestStatus === 'PENDING' && (
                            <>
                              <button className="btn-link" onClick={() => { setRequestToAction(request); setApprovalData({ approvalNotes: '', refillsApproved: request.refillsRequested }); setShowApproveDialog(true); }}>
                                Approve
                              </button>
                              <button className="btn-link" onClick={() => { setRequestToAction(request); setModificationData({ refillsApproved: request.refillsRequested || 1, modificationNotes: '' }); setShowModifyDialog(true); }}>
                                Modify
                              </button>
                              <button className="btn-link btn-danger" onClick={() => { setRequestToAction(request); setDenialData({ denialReason: '' }); setShowDenyDialog(true); }}>
                                Deny
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* Refills History Tab */}
      {activeTab === 'refills' && (
        <>
          {refills.length === 0 ? (
            <div className="empty-state">
              <p>No refills recorded</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Refill #</th>
                    <th>Refill Date</th>
                    <th>Quantity Dispensed</th>
                    <th>Pharmacy</th>
                    <th>Lot Number</th>
                    <th>Expiration Date</th>
                    <th>Filled Date</th>
                  </tr>
                </thead>
                <tbody>
                  {refills.map((refill) => (
                    <tr key={refill.refillId}>
                      <td>{refill.refillNumber || '-'}</td>
                      <td>{formatDate(refill.refillDate)}</td>
                      <td>
                        {refill.quantityDispensed || '-'} {refill.quantityUnit || ''}
                      </td>
                      <td>{refill.pharmacyName || '-'}</td>
                      <td>{refill.lotNumber || '-'}</td>
                      <td>{formatDate(refill.expirationDate)}</td>
                      <td>{formatDateTime(refill.filledDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default PrescriptionRefillsPage;
