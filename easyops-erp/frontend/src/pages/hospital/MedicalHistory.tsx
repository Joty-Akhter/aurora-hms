import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Autocomplete, Box, TextField, Typography } from '@mui/material';
import hospitalService, { CodeSuggestion, MedicalHistory, MedicalHistoryRequest, Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_DETECT_CLASS,
  LAYOUT_OVERLAY_ROOT_Z,
} from '../../utils/layoutOverlayPortal';
import './Hospital.css';

const MedicalHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [history, setHistory] = useState<MedicalHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<MedicalHistory | null>(null);
  const [viewing, setViewing] = useState<MedicalHistory | null>(null);
  const [icd10Suggestions, setIcd10Suggestions] = useState<CodeSuggestion[]>([]);
  const [icd11Suggestions, setIcd11Suggestions] = useState<CodeSuggestion[]>([]);
  const [snomedSuggestions, setSnomedSuggestions] = useState<CodeSuggestion[]>([]);
  const [icd10InputValue, setIcd10InputValue] = useState('');
  const [icd11InputValue, setIcd11InputValue] = useState('');
  const [snomedInputValue, setSnomedInputValue] = useState('');
  const [formData, setFormData] = useState<MedicalHistoryRequest>({
    historyType: 'PAST_MEDICAL',
    conditionName: '',
    icd10Code: '',
    icd11Code: '',
    snomedCode: '',
    onsetDate: '',
    resolutionDate: '',
    status: 'ACTIVE',
    severity: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadHistory();
    }
  }, [id]);

  const searchIcd10 = useCallback(async (searchTerm: string) => {
    if (!id || !searchTerm.trim() || searchTerm.length < 1) {
      setIcd10Suggestions([]);
      return;
    }
    try {
      const response = await hospitalService.searchIcd10Codes(id, searchTerm, 50);
      setIcd10Suggestions(response.data);
    } catch {
      setIcd10Suggestions([]);
    }
  }, [id]);

  const searchIcd11 = useCallback(async (searchTerm: string) => {
    if (!id || !searchTerm.trim() || searchTerm.length < 1) {
      setIcd11Suggestions([]);
      return;
    }
    try {
      const response = await hospitalService.searchIcd11Codes(id, searchTerm, 50);
      setIcd11Suggestions(response.data);
    } catch {
      setIcd11Suggestions([]);
    }
  }, [id]);

  const searchSnomed = useCallback(async (searchTerm: string) => {
    if (!id || !searchTerm.trim() || searchTerm.length < 1) {
      setSnomedSuggestions([]);
      return;
    }
    try {
      const response = await hospitalService.searchSnomedCodes(id, searchTerm, 50);
      setSnomedSuggestions(response.data);
    } catch {
      setSnomedSuggestions([]);
    }
  }, [id]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (icd10InputValue) searchIcd10(icd10InputValue);
    }, 300);
    return () => clearTimeout(timer);
  }, [icd10InputValue, searchIcd10]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (icd11InputValue) searchIcd11(icd11InputValue);
    }, 300);
    return () => clearTimeout(timer);
  }, [icd11InputValue, searchIcd11]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (snomedInputValue) searchSnomed(snomedInputValue);
    }, 300);
    return () => clearTimeout(timer);
  }, [snomedInputValue, searchSnomed]);

  useEffect(() => {
    if (!viewing) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setViewing(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [viewing]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getPatient(id);
      setPatient(response.data);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
      // Don't set error here, just log it - patient info is not critical for this page
    }
  };

  const loadHistory = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getPastMedicalHistory(id);
      setHistory(response.data);
    } catch (err: any) {
      console.error('Failed to load medical history:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load medical history'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    const needsResolutionDate = formData.status === 'INACTIVE' || formData.status === 'RESOLVED';
    if (needsResolutionDate && !formData.resolutionDate) {
      alert('Resolution date is required when status is Inactive or Resolved.');
      return;
    }
    const today = new Date().toISOString().split('T')[0];
    if (formData.historyType === 'PAST_MEDICAL' && formData.onsetDate && formData.onsetDate > today) {
      alert('Onset date cannot be in the future for past medical history.');
      return;
    }
    if (formData.resolutionDate) {
      if (formData.resolutionDate > today) {
        alert('Resolution date cannot be in the future.');
        return;
      }
      if (formData.onsetDate && formData.resolutionDate < formData.onsetDate) {
        alert('Resolution date cannot be before the onset date.');
        return;
      }
    }

    try {
      const normalizeCode = (value: string, maxLength: number, label: string) => {
        const trimmed = value.trim();
        if (!trimmed) return undefined;
        const codeOnly = trimmed.includes(' - ') ? trimmed.split(' - ')[0].trim() : trimmed;
        if (codeOnly.length > maxLength) {
          throw new Error(`${label} must be at most ${maxLength} characters`);
        }
        return codeOnly;
      };
      const icd10Code = normalizeCode(formData.icd10Code ?? '', 20, 'ICD-10 code');
      const icd11Code = normalizeCode(formData.icd11Code ?? '', 20, 'ICD-11 code');
      const snomedCode = normalizeCode(formData.snomedCode ?? '', 50, 'SNOMED code');

      // Strip empty strings from optional fields — backend rejects empty strings for nullable columns
      const payload: MedicalHistoryRequest = {
        historyType: formData.historyType,
        conditionName: formData.conditionName,
        status: formData.status,
        ...(icd10Code ? { icd10Code } : {}),
        ...(icd11Code ? { icd11Code } : {}),
        ...(snomedCode ? { snomedCode } : {}),
        ...(formData.onsetDate ? { onsetDate: formData.onsetDate } : {}),
        ...(formData.resolutionDate ? { resolutionDate: formData.resolutionDate } : {}),
        ...(formData.severity?.trim() ? { severity: formData.severity.trim() } : {}),
        ...(formData.notes?.trim() ? { notes: formData.notes.trim() } : {}),
      };
      if (editing) {
        await hospitalService.updateMedicalHistory(id, editing.historyId, payload);
      } else {
        await hospitalService.createMedicalHistory(id, payload);
      }
      setShowForm(false);
      setEditing(null);
      setIcd10InputValue('');
      setIcd11InputValue('');
      setSnomedInputValue('');
      setIcd10Suggestions([]);
      setIcd11Suggestions([]);
      setSnomedSuggestions([]);
      setFormData({
        historyType: 'PAST_MEDICAL',
        conditionName: '',
        icd10Code: '',
        icd11Code: '',
        snomedCode: '',
        onsetDate: '',
        resolutionDate: '',
        status: 'ACTIVE',
        severity: '',
        notes: '',
      });
      loadHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save medical history'));
    }
  };

  const handleEdit = (item: MedicalHistory) => {
    setEditing(item);
    setIcd10InputValue(item.icd10Code || '');
    setIcd11InputValue(item.icd11Code || '');
    setSnomedInputValue(item.snomedCode || '');
    setFormData({
      historyType: item.historyType,
      conditionName: item.conditionName,
      icd10Code: item.icd10Code || '',
      icd11Code: item.icd11Code || '',
      snomedCode: item.snomedCode || '',
      onsetDate: item.onsetDate || '',
      resolutionDate: item.resolutionDate || '',
      status: item.status,
      severity: item.severity || '',
      notes: item.notes || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (historyId: string) => {
    if (!window.confirm('Are you sure you want to delete this medical history record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteMedicalHistory(id, historyId);
      loadHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete medical history'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading medical history...</div>;
  }

  return (
    <div className="hospital-page">
      {/* Patient Information Header */}
      {patient && (
        <div className="page-header" style={{ marginBottom: '24px' }}>
          <div>
            <h1>{patient.fullName || '—'}</h1>
            <p>
              MRN: {patient.mrn} 
              {patient.dateOfBirth && ` | Age: ${formatAge(patient.dateOfBirth)}`}
              {patient.gender && ` | ${formatGenderLabel(patient.gender)}`}
            </p>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Past Medical History</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Medical History
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Medical History</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>History Type *</label>
                <select
                  required
                  value={formData.historyType}
                  onChange={(e) => setFormData({ ...formData, historyType: e.target.value as any })}
                >
                  <option value="PAST_MEDICAL">Past Medical</option>
                </select>
              </div>
              <div className="form-group">
                <label>Condition Name *</label>
                <input
                  type="text"
                  required
                  value={formData.conditionName}
                  onChange={(e) => setFormData({ ...formData, conditionName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>ICD-10 Code</label>
                <Autocomplete
                  freeSolo
                  filterOptions={(options) => options}
                  options={icd10Suggestions}
                  isOptionEqualToValue={(option, value) => option.code === value.code}
                  getOptionLabel={(option) => typeof option === 'string' ? option : `${option.code} - ${option.description}`}
                  inputValue={icd10InputValue}
                  onInputChange={(_, newInputValue) => {
                    setIcd10InputValue(newInputValue);
                    setFormData({ ...formData, icd10Code: newInputValue });
                  }}
                  onChange={(_, newValue) => {
                    if (newValue && typeof newValue !== 'string') {
                      setFormData({ ...formData, icd10Code: newValue.code });
                      setIcd10InputValue(newValue.code);
                    }
                  }}
                  value={icd10Suggestions.find(s => s.code === formData.icd10Code) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search ICD-10 codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">{option.code}</Typography>
                        <Typography variant="caption" color="text.secondary">{option.description}</Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>ICD-11 Code</label>
                <Autocomplete
                  freeSolo
                  filterOptions={(options) => options}
                  options={icd11Suggestions}
                  isOptionEqualToValue={(option, value) => option.code === value.code}
                  getOptionLabel={(option) => typeof option === 'string' ? option : `${option.code} - ${option.description}`}
                  inputValue={icd11InputValue}
                  onInputChange={(_, newInputValue) => {
                    setIcd11InputValue(newInputValue);
                    setFormData({ ...formData, icd11Code: newInputValue });
                  }}
                  onChange={(_, newValue) => {
                    if (newValue && typeof newValue !== 'string') {
                      setFormData({ ...formData, icd11Code: newValue.code });
                      setIcd11InputValue(newValue.code);
                    }
                  }}
                  value={icd11Suggestions.find(s => s.code === formData.icd11Code) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search ICD-11 codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">{option.code}</Typography>
                        <Typography variant="caption" color="text.secondary">{option.description}</Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>SNOMED Code</label>
                <Autocomplete
                  freeSolo
                  filterOptions={(options) => options}
                  options={snomedSuggestions}
                  isOptionEqualToValue={(option, value) => option.code === value.code}
                  getOptionLabel={(option) => typeof option === 'string' ? option : `${option.code} - ${option.description}`}
                  inputValue={snomedInputValue}
                  onInputChange={(_, newInputValue) => {
                    setSnomedInputValue(newInputValue);
                    setFormData({ ...formData, snomedCode: newInputValue });
                  }}
                  onChange={(_, newValue) => {
                    if (newValue && typeof newValue !== 'string') {
                      setFormData({ ...formData, snomedCode: newValue.code });
                      setSnomedInputValue(newValue.code);
                    }
                  }}
                  value={snomedSuggestions.find(s => s.code === formData.snomedCode) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search SNOMED codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">{option.code}</Typography>
                        <Typography variant="caption" color="text.secondary">{option.description}</Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>Onset Date</label>
                <input
                  type="date"
                  max={new Date().toISOString().split('T')[0]}
                  value={formData.onsetDate}
                  onChange={(e) => setFormData({ ...formData, onsetDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Status</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="CHRONIC">Chronic</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
              {(formData.status === 'INACTIVE' || formData.status === 'RESOLVED') && (
                <div className="form-group">
                  <label>Resolution Date *</label>
                  <input
                    type="date"
                    required
                    max={new Date().toISOString().split('T')[0]}
                    min={formData.onsetDate || undefined}
                    value={formData.resolutionDate}
                    onChange={(e) => setFormData({ ...formData, resolutionDate: e.target.value })}
                  />
                </div>
              )}
              <div className="form-group">
                <label>Severity</label>
                <select
                  value={formData.severity || ''}
                  onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
                >
                  <option value="">Select Severity</option>
                  <option value="MILD">Mild</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="SEVERE">Severe</option>
                  <option value="CRITICAL">Critical</option>
                </select>
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

      {viewing && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          role="presentation"
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}
          onClick={() => setViewing(null)}
        >
          <div
            className="modal-content"
            style={{ maxWidth: '700px', width: 'min(700px, calc(100vw - 32px))' }}
            role="dialog"
            aria-modal="true"
            aria-labelledby="medical-history-view-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <h3 id="medical-history-view-title">Medical History</h3>
              <button type="button" className="modal-close" onClick={() => setViewing(null)} aria-label="Close">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="info-card">
                <div className="info-row"><span className="info-label">Condition</span><span className="info-value">{viewing.conditionName}</span></div>
                <div className="info-row"><span className="info-label">History Type</span><span className="info-value">{viewing.historyType}</span></div>
                <div className="info-row"><span className="info-label">Status</span><span className="info-value">{viewing.status}</span></div>
                <div className="info-row"><span className="info-label">Severity</span><span className="info-value">{viewing.severity || '-'}</span></div>
                <div className="info-row"><span className="info-label">ICD-10</span><span className="info-value">{viewing.icd10Code || '-'}</span></div>
                <div className="info-row"><span className="info-label">ICD-11</span><span className="info-value">{viewing.icd11Code || '-'}</span></div>
                <div className="info-row"><span className="info-label">SNOMED</span><span className="info-value">{viewing.snomedCode || '-'}</span></div>
                <div className="info-row"><span className="info-label">Onset Date</span><span className="info-value">{formatDate(viewing.onsetDate)}</span></div>
                <div className="info-row"><span className="info-label">Resolution Date</span><span className="info-value">{formatDate(viewing.resolutionDate)}</span></div>
                <div className="info-row"><span className="info-label">Notes</span><span className="info-value">{viewing.notes || '-'}</span></div>
              </div>
            </div>
          </div>
        </div>,
      )}

      {history.length === 0 ? (
        <div className="empty-state">
          <p>No medical history records found</p>
        </div>
      ) : (
        <div className="table-container" style={{ overflowX: 'auto' }}>
          <table className="data-table" style={{ minWidth: '920px' }}>
            <thead>
              <tr>
                <th>Condition</th>
                <th>ICD-10</th>
                <th>Onset Date</th>
                <th>Resolution Date</th>
                <th>Status</th>
                <th>Severity</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.historyId}>
                  <td>{item.conditionName}</td>
                  <td>{item.icd10Code || '-'}</td>
                  <td>{formatDate(item.onsetDate)}</td>
                  <td>{formatDate(item.resolutionDate)}</td>
                  <td>
                    <span className={`status-badge status-${item.status.toLowerCase()}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>{item.severity || '-'}</td>
                  <td>
                    <div className="action-buttons" style={{ flexWrap: 'wrap', justifyContent: 'flex-start' }}>
                      <button className="btn-link" onClick={() => setViewing(item)}>View</button>
                      <button className="btn-link" onClick={() => handleEdit(item)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(item.historyId)}>Delete</button>
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

export default MedicalHistoryPage;
