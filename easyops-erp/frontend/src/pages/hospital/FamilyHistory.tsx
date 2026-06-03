import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Autocomplete, Box, TextField, Typography } from '@mui/material';
import hospitalService, {
  CodeSuggestion,
  FamilyHistory,
  FamilyHistoryRequest,
  Patient,
} from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { blockNegativeNumberInput, parseOptionalNonNegativeInt } from '../../utils/formValidation';
import './Hospital.css';

type FamilyHistoryFormState = FamilyHistoryRequest & {
  severity?: 'MILD' | 'MODERATE' | 'SEVERE' | 'CRITICAL';
};

const SEVERITY_NOTE_PREFIX = 'Severity: ';

function extractSeverityAndNotes(notes?: string): {
  severity?: FamilyHistoryFormState['severity'];
  cleanedNotes: string;
} {
  if (!notes) return { cleanedNotes: '' };
  const lines = notes.split('\n');
  const first = lines[0]?.trim() ?? '';
  const match = first.match(/^Severity:\s*(MILD|MODERATE|SEVERE|CRITICAL)\s*$/i);
  if (!match) {
    return { cleanedNotes: notes };
  }
  const sev = match[1].toUpperCase() as NonNullable<FamilyHistoryFormState['severity']>;
  return {
    severity: sev,
    cleanedNotes: lines.slice(1).join('\n').trim(),
  };
}

function buildPayload(formData: FamilyHistoryFormState): FamilyHistoryRequest {
  const normalizeCode = (value: string | undefined, maxLength: number, label: string) => {
    const trimmed = value?.trim();
    if (!trimmed) return undefined;
    const codeOnly = trimmed.includes(' - ') ? trimmed.split(' - ')[0].trim() : trimmed;
    if (codeOnly.length > maxLength) {
      throw new Error(`${label} must be at most ${maxLength} characters`);
    }
    return codeOnly;
  };
  const baseNotes = (formData.notes || '').trim();
  const severityLine = formData.severity ? `${SEVERITY_NOTE_PREFIX}${formData.severity}` : '';
  const notes = severityLine ? [severityLine, baseNotes].filter(Boolean).join('\n') : baseNotes;
  return {
    familyMemberRelationship: formData.familyMemberRelationship,
    conditionName: formData.conditionName,
    icd10Code: normalizeCode(formData.icd10Code, 20, 'ICD-10 code'),
    icd11Code: normalizeCode(formData.icd11Code, 20, 'ICD-11 code'),
    snomedCode: normalizeCode(formData.snomedCode, 50, 'SNOMED code'),
    ageAtOnset: formData.ageAtOnset,
    ageAtDeath: formData.ageAtDeath,
    notes: notes || undefined,
    documentedDate: formData.documentedDate,
  };
}

const FamilyHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [familyHistory, setFamilyHistory] = useState<FamilyHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<FamilyHistory | null>(null);
  const [formData, setFormData] = useState<FamilyHistoryFormState>({
    familyMemberRelationship: 'Mother',
    conditionName: '',
    icd10Code: '',
    icd11Code: '',
    snomedCode: '',
    ageAtOnset: undefined,
    ageAtDeath: undefined,
    severity: undefined,
    notes: '',
  });
  const [icd10Suggestions, setIcd10Suggestions] = useState<CodeSuggestion[]>([]);
  const [icd11Suggestions, setIcd11Suggestions] = useState<CodeSuggestion[]>([]);
  const [snomedSuggestions, setSnomedSuggestions] = useState<CodeSuggestion[]>([]);
  const [icd10InputValue, setIcd10InputValue] = useState('');
  const [icd11InputValue, setIcd11InputValue] = useState('');
  const [snomedInputValue, setSnomedInputValue] = useState('');

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadFamilyHistory();
    }
  }, [id]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getPatient(id);
      setPatient(response.data);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
    }
  };

  const loadFamilyHistory = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getFamilyHistory(id);
      setFamilyHistory(response.data);
    } catch (err: any) {
      console.error('Failed to load family history:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load family history'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      const payload = buildPayload(formData);
      if (!payload.icd10Code || !payload.icd11Code || !payload.snomedCode) {
        alert('ICD-10, ICD-11, and SNOMED codes are required for family history.');
        return;
      }
      if (editing) {
        await hospitalService.updateFamilyHistory(id, editing.familyHistoryId, payload);
      } else {
        await hospitalService.createFamilyHistory(id, payload);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        familyMemberRelationship: 'Mother',
        conditionName: '',
        icd10Code: '',
        icd11Code: '',
        snomedCode: '',
        ageAtOnset: undefined,
        ageAtDeath: undefined,
        severity: undefined,
        notes: '',
      });
      setIcd10InputValue('');
      setIcd11InputValue('');
      setSnomedInputValue('');
      setIcd10Suggestions([]);
      setIcd11Suggestions([]);
      setSnomedSuggestions([]);
      loadFamilyHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save family history'));
    }
  };

  const handleEdit = (item: FamilyHistory) => {
    const parsed = extractSeverityAndNotes(item.notes);
    setEditing(item);
    setFormData({
      familyMemberRelationship: item.familyMemberRelationship,
      conditionName: item.conditionName,
      icd10Code: item.icd10Code || '',
      icd11Code: item.icd11Code || '',
      snomedCode: item.snomedCode || '',
      ageAtOnset: item.ageAtOnset,
      ageAtDeath: item.ageAtDeath,
      severity: parsed.severity,
      notes: parsed.cleanedNotes,
    });
    setIcd10InputValue(item.icd10Code || '');
    setIcd11InputValue(item.icd11Code || '');
    setSnomedInputValue(item.snomedCode || '');
    setShowForm(true);
  };

  useEffect(() => {
    if (!id || icd10InputValue.trim().length < 2) {
      setIcd10Suggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const response = await hospitalService.searchIcd10Codes(id, icd10InputValue, 50);
        setIcd10Suggestions(response.data);
      } catch {
        setIcd10Suggestions([]);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [id, icd10InputValue]);

  useEffect(() => {
    if (!id || icd11InputValue.trim().length < 2) {
      setIcd11Suggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const response = await hospitalService.searchIcd11Codes(id, icd11InputValue, 50);
        setIcd11Suggestions(response.data);
      } catch {
        setIcd11Suggestions([]);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [id, icd11InputValue]);

  useEffect(() => {
    if (!id || snomedInputValue.trim().length < 2) {
      setSnomedSuggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const response = await hospitalService.searchSnomedCodes(id, snomedInputValue, 50);
        setSnomedSuggestions(response.data);
      } catch {
        setSnomedSuggestions([]);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [id, snomedInputValue]);

  const handleDelete = async (familyHistoryId: string) => {
    if (!window.confirm('Are you sure you want to delete this family history record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteFamilyHistory(id, familyHistoryId);
      loadFamilyHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete family history'));
    }
  };

  if (loading) {
    return <div className="loading">Loading family history...</div>;
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
        <h3 style={{ margin: 0 }}>Family History</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Family History
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Family History</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Family Member Relationship *</label>
                <select
                  required
                  value={formData.familyMemberRelationship}
                  onChange={(e) => setFormData({ ...formData, familyMemberRelationship: e.target.value as any })}
                >
                  <option value="Mother">Mother</option>
                  <option value="Father">Father</option>
                  <option value="Sister">Sister</option>
                  <option value="Brother">Brother</option>
                  <option value="Maternal_Grandmother">Maternal Grandmother</option>
                  <option value="Maternal_Grandfather">Maternal Grandfather</option>
                  <option value="Paternal_Grandmother">Paternal Grandmother</option>
                  <option value="Paternal_Grandfather">Paternal Grandfather</option>
                  <option value="Aunt">Aunt</option>
                  <option value="Uncle">Uncle</option>
                  <option value="Cousin">Cousin</option>
                  <option value="Other">Other</option>
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
                <label>ICD-10 Code *</label>
                <Autocomplete
                  freeSolo
                  options={icd10Suggestions}
                  getOptionLabel={(option) =>
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={icd10InputValue}
                  onInputChange={(_, v) => {
                    setIcd10InputValue(v);
                    setFormData({ ...formData, icd10Code: v });
                  }}
                  onChange={(_, v) => {
                    if (v && typeof v !== 'string') {
                      setFormData({ ...formData, icd10Code: v.code });
                      setIcd10InputValue(v.code);
                    }
                  }}
                  value={icd10Suggestions.find((s) => s.code === formData.icd10Code) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search ICD-10 codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">
                          {option.code}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {option.description}
                        </Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>ICD-11 Code *</label>
                <Autocomplete
                  freeSolo
                  options={icd11Suggestions}
                  getOptionLabel={(option) =>
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={icd11InputValue}
                  onInputChange={(_, v) => {
                    setIcd11InputValue(v);
                    setFormData({ ...formData, icd11Code: v });
                  }}
                  onChange={(_, v) => {
                    if (v && typeof v !== 'string') {
                      setFormData({ ...formData, icd11Code: v.code });
                      setIcd11InputValue(v.code);
                    }
                  }}
                  value={icd11Suggestions.find((s) => s.code === formData.icd11Code) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search ICD-11 codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">
                          {option.code}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {option.description}
                        </Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>SNOMED Code *</label>
                <Autocomplete
                  freeSolo
                  options={snomedSuggestions}
                  getOptionLabel={(option) =>
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={snomedInputValue}
                  onInputChange={(_, v) => {
                    setSnomedInputValue(v);
                    setFormData({ ...formData, snomedCode: v });
                  }}
                  onChange={(_, v) => {
                    if (v && typeof v !== 'string') {
                      setFormData({ ...formData, snomedCode: v.code });
                      setSnomedInputValue(v.code);
                    }
                  }}
                  value={snomedSuggestions.find((s) => s.code === formData.snomedCode) || null}
                  renderInput={(params) => (
                    <TextField {...params} placeholder="Type to search SNOMED codes..." />
                  )}
                  renderOption={(props, option) => (
                    <Box component="li" {...props} key={option.code}>
                      <Box>
                        <Typography variant="body2" fontWeight="bold">
                          {option.code}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {option.description}
                        </Typography>
                      </Box>
                    </Box>
                  )}
                />
              </div>
              <div className="form-group">
                <label>Severity</label>
                <select
                  value={formData.severity || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      severity: e.target.value
                        ? (e.target.value as NonNullable<FamilyHistoryFormState['severity']>)
                        : undefined,
                    })
                  }
                >
                  <option value="">Select Severity</option>
                  <option value="MILD">Mild</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="SEVERE">Severe</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
              <div className="form-group">
                <label>Age at Onset</label>
                <input
                  type="number"
                  min={0}
                  value={formData.ageAtOnset || ''}
                  onKeyDown={blockNegativeNumberInput}
                  onChange={(e) => setFormData({
                    ...formData,
                    ageAtOnset: e.target.value ? parseOptionalNonNegativeInt(e.target.value) : undefined,
                  })}
                />
              </div>
              <div className="form-group">
                <label>Age at Death</label>
                <input
                  type="number"
                  min={0}
                  value={formData.ageAtDeath || ''}
                  onKeyDown={blockNegativeNumberInput}
                  onChange={(e) => setFormData({
                    ...formData,
                    ageAtDeath: e.target.value ? parseOptionalNonNegativeInt(e.target.value) : undefined,
                  })}
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

      {familyHistory.length === 0 ? (
        <div className="empty-state">
          <p>No family history records found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Family Member</th>
                <th>Condition</th>
                <th>ICD-10</th>
                <th>Age at Onset</th>
                <th>Age at Death</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {familyHistory.map((item) => (
                <tr key={item.familyHistoryId}>
                  <td>{item.familyMemberRelationship.replace('_', ' ')}</td>
                  <td><strong>{item.conditionName}</strong></td>
                  <td>{item.icd10Code || '-'}</td>
                  <td>{item.ageAtOnset ? `${item.ageAtOnset} years` : '-'}</td>
                  <td>{item.ageAtDeath ? `${item.ageAtDeath} years` : '-'}</td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(item)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(item.familyHistoryId)}>Delete</button>
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

export default FamilyHistoryPage;
