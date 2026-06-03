import React, { useEffect, useState } from 'react';
import hospitalService, { 
  PrescriptionRefillRequest,
  RefillApprovalRequest,
  RefillDenialRequest,
  RefillModificationRequest,
  Prescription
} from '../../services/hospitalService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PrescriptionRefillQueuePage: React.FC = () => {
  const [refillRequests, setRefillRequests] = useState<PrescriptionRefillRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewing, setViewing] = useState<PrescriptionRefillRequest | null>(null);
  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showDenyDialog, setShowDenyDialog] = useState(false);
  const [showModifyDialog, setShowModifyDialog] = useState(false);
  const [requestToAction, setRequestToAction] = useState<PrescriptionRefillRequest | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterUrgency, setFilterUrgency] = useState<string>('');
  const [sortBy, setSortBy] = useState<'date' | 'urgency' | 'patient'>('date');
  
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

  useEffect(() => {
    loadRefillRequests();
  }, [filterStatus, filterUrgency]);

  const loadRefillRequests = async () => {
    try {
      setLoading(true);
      const response = await hospitalService.getPendingRefillRequests();
      let filtered = response.data;
      
      // Apply filters
      if (filterStatus) {
        filtered = filtered.filter(r => r.requestStatus === filterStatus);
      }
      if (filterUrgency) {
        filtered = filtered.filter(r => r.urgencyLevel === filterUrgency);
      }
      
      // Sort
      filtered.sort((a, b) => {
        if (sortBy === 'urgency') {
          const urgencyOrder = { 'URGENT': 0, 'HIGH': 1, 'MEDIUM': 2, 'LOW': 3 };
          return (urgencyOrder[a.urgencyLevel as keyof typeof urgencyOrder] ?? 4) - 
                 (urgencyOrder[b.urgencyLevel as keyof typeof urgencyOrder] ?? 4);
        } else if (sortBy === 'patient') {
          return (a.medicationName || '').localeCompare(b.medicationName || '');
        } else {
          return new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime();
        }
      });
      
      setRefillRequests(filtered);
    } catch (err: any) {
      console.error('Failed to load refill requests:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load refill requests'));
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!requestToAction) return;
    try {
      await hospitalService.approveRefillRequest(requestToAction.refillRequestId, approvalData);
      setShowApproveDialog(false);
      setRequestToAction(null);
      setApprovalData({ approvalNotes: '', refillsApproved: undefined });
      loadRefillRequests();
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
      loadRefillRequests();
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
      loadRefillRequests();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to modify refill request'));
    }
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

  const getUrgencyColor = (urgency?: string) => {
    switch (urgency) {
      case 'URGENT': return '#ef4444';
      case 'HIGH': return '#f59e0b';
      case 'MEDIUM': return '#3b82f6';
      case 'LOW': return '#10b981';
      default: return '#6b7280';
    }
  };

  if (loading && !refillRequests.length) {
    return <div className="loading">Loading refill queue...</div>;
  }

  const pendingCount = refillRequests.filter(r => r.requestStatus === 'PENDING').length;
  const urgentCount = refillRequests.filter(r => r.urgencyLevel === 'URGENT' && r.requestStatus === 'PENDING').length;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h3>Prescription Refill Queue</h3>
          <p style={{ color: '#6b7280', marginTop: '4px' }}>
            Manage pending refill requests from pharmacies and patients
          </p>
        </div>
        <button className="btn-secondary" onClick={loadRefillRequests}>
          Refresh
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="info-card" style={{ background: '#fef3c7', borderLeft: '4px solid #f59e0b' }}>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#92400e' }}>{pendingCount}</div>
          <div style={{ color: '#78350f', fontSize: '14px' }}>Pending Requests</div>
        </div>
        <div className="info-card" style={{ background: '#fee2e2', borderLeft: '4px solid #ef4444' }}>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#991b1b' }}>{urgentCount}</div>
          <div style={{ color: '#7f1d1d', fontSize: '14px' }}>Urgent Requests</div>
        </div>
        <div className="info-card" style={{ background: '#dbeafe', borderLeft: '4px solid #3b82f6' }}>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1e40af' }}>{refillRequests.length}</div>
          <div style={{ color: '#1e3a8a', fontSize: '14px' }}>Total Requests</div>
        </div>
      </div>

      {/* Filters */}
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
          <select
            value={filterUrgency}
            onChange={(e) => setFilterUrgency(e.target.value)}
            className="filter-select"
          >
            <option value="">All Urgency Levels</option>
            <option value="URGENT">Urgent</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="filter-select"
          >
            <option value="date">Sort by Date</option>
            <option value="urgency">Sort by Urgency</option>
            <option value="patient">Sort by Medication</option>
          </select>
          <button className="btn-secondary" onClick={() => { setFilterStatus(''); setFilterUrgency(''); setSortBy('date'); }}>
            Clear Filters
          </button>
        </div>
      </div>

      {/* Approve Dialog */}
      {showApproveDialog && requestToAction && portalLayoutOverlay(
        <div className={LAYOUT_OVERLAY_DETECT_CLASS} style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Approve Refill Request</h3>
            <p><strong>{requestToAction.medicationName}</strong></p>
            <p>Requested: {requestToAction.refillsRequested} refill(s)</p>
            <p>Remaining: {requestToAction.refillsRemaining || 0} refill(s)</p>
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
        <div className={LAYOUT_OVERLAY_DETECT_CLASS} style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="form-container" style={{ maxWidth: '700px', width: '90%', maxHeight: '90vh', overflow: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3>Refill Request Details</h3>
              <button className="btn-secondary" onClick={() => setViewing(null)}>Close</button>
            </div>
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
              {viewing.pharmacyName && (
                <div className="info-row">
                  <span className="info-label">Pharmacy:</span>
                  <span className="info-value">{viewing.pharmacyName}</span>
                </div>
              )}
              {viewing.requestedByName && (
                <div className="info-row">
                  <span className="info-label">Requested By:</span>
                  <span className="info-value">{viewing.requestedByName}</span>
                </div>
              )}
              {viewing.notes && (
                <div className="info-row">
                  <span className="info-label">Notes:</span>
                  <span className="info-value" style={{ whiteSpace: 'pre-wrap' }}>{viewing.notes}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Refill Requests Table */}
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
                <tr key={request.refillRequestId} style={{
                  backgroundColor: request.urgencyLevel === 'URGENT' && request.requestStatus === 'PENDING' 
                    ? '#fef2f2' 
                    : 'transparent'
                }}>
                  <td><strong>{request.medicationName || '-'}</strong></td>
                  <td>{request.prescriptionNumber || '-'}</td>
                  <td>{request.requestSource}</td>
                  <td>{request.refillsRequested || 0}</td>
                  <td>{request.refillsRemaining || 0}</td>
                  <td>{request.daysSinceLastFill !== undefined ? request.daysSinceLastFill : '-'}</td>
                  <td>
                    <span 
                      className="status-badge" 
                      style={{ 
                        backgroundColor: getUrgencyColor(request.urgencyLevel),
                        color: 'white'
                      }}
                    >
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
                          <button className="btn-link" onClick={() => { 
                            setRequestToAction(request); 
                            setApprovalData({ approvalNotes: '', refillsApproved: request.refillsRequested }); 
                            setShowApproveDialog(true); 
                          }}>
                            Approve
                          </button>
                          <button className="btn-link" onClick={() => { 
                            setRequestToAction(request); 
                            setModificationData({ refillsApproved: request.refillsRequested || 1, modificationNotes: '' }); 
                            setShowModifyDialog(true); 
                          }}>
                            Modify
                          </button>
                          <button className="btn-link btn-danger" onClick={() => { 
                            setRequestToAction(request); 
                            setDenialData({ denialReason: '' }); 
                            setShowDenyDialog(true); 
                          }}>
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
    </div>
  );
};

export default PrescriptionRefillQueuePage;
