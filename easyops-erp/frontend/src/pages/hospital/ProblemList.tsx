import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Autocomplete, TextField, Box, Typography } from '@mui/material';
import hospitalService, { 
  PatientProblem, 
  PatientProblemRequest, 
  ProblemResolutionRequest,
  ProblemHistory,
  ProblemListSummary,
  CodeSuggestion,
  Patient
} from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatClinicalEnumLabel, formatGenderLabel, formatProblemStatusLabel } from '../../utils/patientDisplay';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_NESTED_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ProblemListPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [problems, setProblems] = useState<PatientProblem[]>([]);
  const [summary, setSummary] = useState<ProblemListSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<PatientProblem | null>(null);
  const [viewing, setViewing] = useState<PatientProblem | null>(null);
  const [showHistory, setShowHistory] = useState(false);
  const [problemHistory, setProblemHistory] = useState<ProblemHistory[]>([]);
  const [showResolveDialog, setShowResolveDialog] = useState(false);
  const [problemToResolve, setProblemToResolve] = useState<PatientProblem | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterType, setFilterType] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [problemMedications, setProblemMedications] = useState<any[]>([]);
  const [showLinkMedicationDialog, setShowLinkMedicationDialog] = useState(false);
  const [availableMedications, setAvailableMedications] = useState<any[]>([]);
  const [selectedMedicationId, setSelectedMedicationId] = useState<string>('');
  const [linkType, setLinkType] = useState<string>('TREATS');
  const [linkStrength, setLinkStrength] = useState<string>('MODERATE');
  
  // Code suggestion states
  const [icd10Suggestions, setIcd10Suggestions] = useState<CodeSuggestion[]>([]);
  const [icd11Suggestions, setIcd11Suggestions] = useState<CodeSuggestion[]>([]);
  const [snomedSuggestions, setSnomedSuggestions] = useState<CodeSuggestion[]>([]);
  const [icd10InputValue, setIcd10InputValue] = useState('');
  const [icd11InputValue, setIcd11InputValue] = useState('');
  const [snomedInputValue, setSnomedInputValue] = useState('');
  const problemFormRef = useRef<HTMLDivElement>(null);

  const scrollToProblemForm = () => {
    requestAnimationFrame(() => {
      problemFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  const openAddProblemForm = () => {
    setEditing(null);
    resetForm();
    setShowForm(true);
    scrollToProblemForm();
  };
  
  const [formData, setFormData] = useState<PatientProblemRequest>({
    patientId: id || '',
    problemName: '',
    icd10Code: '',
    icd11Code: '',
    snomedCode: '',
    problemType: 'DIAGNOSIS',
    status: 'ACTIVE',
    onsetDate: '',
    resolutionDate: '',
    severity: undefined,
    chronicity: '',
    priority: undefined,
    notes: '',
  });

  const [resolveData, setResolveData] = useState<ProblemResolutionRequest>({
    resolutionDate: new Date().toISOString().split('T')[0],
    resolutionNotes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadProblems();
      loadSummary();
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

  const loadProblems = async () => {
    if (!id) return;
    try {
      setLoading(true);
      let response;
      if (filterStatus) {
        response = await hospitalService.getProblemsByStatus(id, filterStatus);
      } else if (filterType) {
        response = await hospitalService.getProblemsByType(id, filterType);
      } else {
        response = await hospitalService.getProblems(id);
      }
      setProblems(response.data);
    } catch (err: any) {
      console.error('Failed to load problems:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load problems'));
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getProblemListSummary(id);
      setSummary(response.data);
    } catch (err: any) {
      console.error('Failed to load summary:', err);
    }
  };

  const handleSearch = async () => {
    if (!id || !searchTerm.trim()) {
      loadProblems();
      return;
    }
    try {
      setLoading(true);
      const response = await hospitalService.searchProblems(id, searchTerm);
      setProblems(response.data);
    } catch (err: any) {
      console.error('Failed to search problems:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search problems'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      loadProblems();
    }
  }, [filterStatus, filterType]);

  // Debounced search for code suggestions
  const searchIcd10 = useCallback(
    async (searchTerm: string) => {
      if (!id || !searchTerm.trim() || searchTerm.length < 1) {
        setIcd10Suggestions([]);
        return;
      }
      try {
        const response = await hospitalService.searchIcd10Codes(id, searchTerm, 50);
        setIcd10Suggestions(response.data);
      } catch (err: any) {
        console.error('Failed to search ICD-10 codes:', err);
        setIcd10Suggestions([]);
      }
    },
    [id]
  );

  const searchIcd11 = useCallback(
    async (searchTerm: string) => {
      if (!id || !searchTerm.trim() || searchTerm.length < 1) {
        setIcd11Suggestions([]);
        return;
      }
      try {
        const response = await hospitalService.searchIcd11Codes(id, searchTerm, 50);
        setIcd11Suggestions(response.data);
      } catch (err: any) {
        console.error('Failed to search ICD-11 codes:', err);
        setIcd11Suggestions([]);
      }
    },
    [id]
  );

  const searchSnomed = useCallback(
    async (searchTerm: string) => {
      if (!id || !searchTerm.trim() || searchTerm.length < 1) {
        setSnomedSuggestions([]);
        return;
      }
      try {
        const response = await hospitalService.searchSnomedCodes(id, searchTerm, 50);
        setSnomedSuggestions(response.data);
      } catch (err: any) {
        console.error('Failed to search SNOMED codes:', err);
        setSnomedSuggestions([]);
      }
    },
    [id]
  );

  // Debounce effect for ICD-10
  useEffect(() => {
    const timer = setTimeout(() => {
      if (icd10InputValue) {
        searchIcd10(icd10InputValue);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [icd10InputValue, searchIcd10]);

  // Debounce effect for ICD-11
  useEffect(() => {
    const timer = setTimeout(() => {
      if (icd11InputValue) {
        searchIcd11(icd11InputValue);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [icd11InputValue, searchIcd11]);

  // Debounce effect for SNOMED
  useEffect(() => {
    const timer = setTimeout(() => {
      if (snomedInputValue) {
        searchSnomed(snomedInputValue);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [snomedInputValue, searchSnomed]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    const payload: PatientProblemRequest = {
      ...formData,
      icd10Code: formData.icd10Code?.trim() || '',
      icd11Code: formData.icd11Code?.trim() || '',
      snomedCode: formData.snomedCode?.trim() || '',
      chronicity: formData.chronicity?.trim() || '',
      notes: formData.notes?.trim() || '',
      // Backend expects LocalDate values; empty strings can fail deserialization.
      onsetDate: formData.onsetDate || undefined,
      resolutionDate: formData.resolutionDate || undefined,
    };

    try {
      if (editing) {
        await hospitalService.updateProblem(id, editing.problemId, payload);
      } else {
        await hospitalService.createProblem(id, payload);
      }
      setShowForm(false);
      setEditing(null);
      resetForm();
      loadProblems();
      loadSummary();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save problem'));
    }
  };

  const resetForm = () => {
    setIcd10InputValue('');
    setIcd11InputValue('');
    setSnomedInputValue('');
    setIcd10Suggestions([]);
    setIcd11Suggestions([]);
    setSnomedSuggestions([]);
    setFormData({
      patientId: id || '',
      problemName: '',
      icd10Code: '',
      icd11Code: '',
      snomedCode: '',
      problemType: 'DIAGNOSIS',
      status: 'ACTIVE',
      onsetDate: '',
      resolutionDate: '',
      severity: undefined,
      chronicity: '',
      priority: undefined,
      notes: '',
    });
  };

  const handleEdit = (problem: PatientProblem) => {
    setEditing(problem);
    setIcd10InputValue(problem.icd10Code || '');
    setIcd11InputValue(problem.icd11Code || '');
    setSnomedInputValue(problem.snomedCode || '');
    setFormData({
      patientId: problem.patientId,
      encounterId: problem.encounterId,
      problemName: problem.problemName,
      icd10Code: problem.icd10Code || '',
      icd11Code: problem.icd11Code || '',
      snomedCode: problem.snomedCode || '',
      problemType: problem.problemType,
      status: problem.status,
      onsetDate: problem.onsetDate || '',
      resolutionDate: problem.resolutionDate || '',
      severity: problem.severity,
      chronicity: problem.chronicity || '',
      priority: problem.priority,
      notes: problem.notes || '',
    });
    setShowForm(true);
    scrollToProblemForm();
  };

  const handleView = async (problemId: string) => {
    if (!id) return;
    try {
      const [problemResponse, medicationsResponse] = await Promise.all([
        hospitalService.getProblem(id, problemId),
        hospitalService.getMedicationsByProblem(id, problemId).catch(() => ({ data: [] }))
      ]);
      setViewing(problemResponse.data);
      setProblemMedications(medicationsResponse.data);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load problem'));
    }
  };

  const handleLinkMedication = async () => {
    if (!id || !viewing || !selectedMedicationId) return;
    try {
      await hospitalService.linkMedicationToProblem(
        id,
        viewing.problemId,
        selectedMedicationId,
        linkType,
        linkStrength
      );
      setShowLinkMedicationDialog(false);
      setSelectedMedicationId('');
      // Reload medications
      const medicationsResponse = await hospitalService.getMedicationsByProblem(id, viewing.problemId);
      setProblemMedications(medicationsResponse.data);
      alert('Medication linked successfully');
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to link medication'));
    }
  };

  const handleUnlinkMedication = async (medicationId: string) => {
    if (!id || !viewing) return;
    if (!window.confirm('Are you sure you want to unlink this medication?')) {
      return;
    }
    try {
      await hospitalService.unlinkMedicationFromProblem(id, viewing.problemId, medicationId);
      // Reload medications
      const medicationsResponse = await hospitalService.getMedicationsByProblem(id, viewing.problemId);
      setProblemMedications(medicationsResponse.data);
      alert('Medication unlinked successfully');
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to unlink medication'));
    }
  };

  const loadAvailableMedications = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getActiveMedications(id);
      setAvailableMedications(response.data);
    } catch (err: any) {
      console.error('Failed to load medications:', err);
    }
  };

  const handleViewHistory = async (problemId: string) => {
    if (!id) return;
    try {
      const response = await hospitalService.getProblemHistory(id, problemId);
      setProblemHistory(response.data);
      setShowHistory(true);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load problem history'));
    }
  };

  const handleDelete = async (problemId: string) => {
    if (!window.confirm('Are you sure you want to delete this problem?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteProblem(id, problemId);
      loadProblems();
      loadSummary();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete problem'));
    }
  };

  const handleResolve = async () => {
    if (!id || !problemToResolve) return;
    try {
      await hospitalService.resolveProblem(id, problemToResolve.problemId, resolveData);
      setShowResolveDialog(false);
      setProblemToResolve(null);
      setResolveData({
        resolutionDate: new Date().toISOString().split('T')[0],
        resolutionNotes: '',
      });
      loadProblems();
      loadSummary();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to resolve problem'));
    }
  };

  const handleReactivate = async (problemId: string) => {
    if (!window.confirm('Are you sure you want to reactivate this problem?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.reactivateProblem(id, problemId);
      loadProblems();
      loadSummary();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to reactivate problem'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const formatHistoryFieldValue = (fieldName: string | undefined, value: string | undefined) => {
    if (!value) return '-';
    const field = (fieldName || '').toLowerCase().replace(/\s+/g, '');
    if (field === 'status') return formatProblemStatusLabel(value);
    if (['severity', 'priority', 'problemtype', 'chronicity', 'problemstatus'].includes(field)) {
      return formatClinicalEnumLabel(value);
    }
    return value;
  };

  if (loading && !problems.length) {
    return <div className="loading">Loading problem list...</div>;
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
        <h3 style={{ margin: 0 }}>Problem List</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={openAddProblemForm}>
            + Add Problem
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Cards */}
      {summary && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
          <div className="info-card">
            <div className="info-card-title">Total Problems</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
              {summary.totalProblems}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">Active</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#3b82f6' }}>
              {summary.activeProblems}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">Resolved</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#6b7280' }}>
              {summary.resolvedProblems}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">Chronic</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b' }}>
              {summary.chronicProblems}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">High Priority</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
              {summary.highPriorityProblems?.length || 0}
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="filters-section">
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search by name, ICD-10, ICD-11, or SNOMED code..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="RESOLVED">Resolved</option>
            <option value="INACTIVE">Inactive</option>
            <option value="RULED_OUT">Ruled Out</option>
            <option value="CHRONIC">Chronic</option>
            <option value="REMISSION">Remission</option>
          </select>
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="filter-select"
          >
            <option value="">All Types</option>
            <option value="DIAGNOSIS">Diagnosis</option>
            <option value="SYMPTOM">Symptom</option>
            <option value="FINDING">Finding</option>
            <option value="CONDITION">Condition</option>
            <option value="ALLERGY">Allergy</option>
            <option value="OTHER">Other</option>
          </select>
          <button className="btn-secondary" onClick={handleSearch}>Search</button>
          <button className="btn-secondary" onClick={() => { setSearchTerm(''); setFilterStatus(''); setFilterType(''); loadProblems(); }}>Clear</button>
        </div>
      </div>

      {/* Create/Edit Form */}
      {showForm && (
        <div ref={problemFormRef} className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Problem</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Problem Name *</label>
                <input
                  type="text"
                  required
                  value={formData.problemName}
                  onChange={(e) => setFormData({ ...formData, problemName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Problem Type *</label>
                <select
                  required
                  value={formData.problemType}
                  onChange={(e) => setFormData({ ...formData, problemType: e.target.value as any })}
                >
                  <option value="DIAGNOSIS">Diagnosis</option>
                  <option value="SYMPTOM">Symptom</option>
                  <option value="FINDING">Finding</option>
                  <option value="CONDITION">Condition</option>
                  <option value="ALLERGY">Allergy</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>ICD-10 Code</label>
                <Autocomplete
                  freeSolo
                  options={icd10Suggestions}
                  getOptionLabel={(option) => 
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={icd10InputValue}
                  onInputChange={(event, newInputValue) => {
                    setIcd10InputValue(newInputValue);
                    setFormData({ ...formData, icd10Code: newInputValue });
                  }}
                  onChange={(event, newValue) => {
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
                <label>ICD-11 Code</label>
                <Autocomplete
                  freeSolo
                  options={icd11Suggestions}
                  getOptionLabel={(option) => 
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={icd11InputValue}
                  onInputChange={(event, newInputValue) => {
                    setIcd11InputValue(newInputValue);
                    setFormData({ ...formData, icd11Code: newInputValue });
                  }}
                  onChange={(event, newValue) => {
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
                <label>SNOMED Code</label>
                <Autocomplete
                  freeSolo
                  options={snomedSuggestions}
                  getOptionLabel={(option) => 
                    typeof option === 'string' ? option : `${option.code} - ${option.description}`
                  }
                  inputValue={snomedInputValue}
                  onInputChange={(event, newInputValue) => {
                    setSnomedInputValue(newInputValue);
                    setFormData({ ...formData, snomedCode: newInputValue });
                  }}
                  onChange={(event, newValue) => {
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
                <label>Status</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="RULED_OUT">Ruled Out</option>
                  <option value="CHRONIC">Chronic</option>
                  <option value="REMISSION">Remission</option>
                </select>
              </div>
              <div className="form-group">
                <label>Onset Date</label>
                <input
                  type="date"
                  value={formData.onsetDate}
                  onChange={(e) => setFormData({ ...formData, onsetDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Severity</label>
                <select
                  value={formData.severity || ''}
                  onChange={(e) => setFormData({ ...formData, severity: e.target.value ? e.target.value as any : undefined })}
                >
                  <option value="">Select Severity</option>
                  <option value="MILD">Mild</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="SEVERE">Severe</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
              <div className="form-group">
                <label>Priority</label>
                <select
                  value={formData.priority || ''}
                  onChange={(e) => setFormData({ ...formData, priority: e.target.value ? e.target.value as any : undefined })}
                >
                  <option value="">Select Priority</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
              <div className="form-group">
                <label>Chronicity</label>
                <select
                  value={formData.chronicity}
                  onChange={(e) => setFormData({ ...formData, chronicity: e.target.value })}
                >
                  <option value="">Select Chronicity</option>
                  <option value="ACUTE">Acute</option>
                  <option value="SUBACUTE">Subacute</option>
                  <option value="CHRONIC">Chronic</option>
                  <option value="RECURRENT">Recurrent</option>
                  <option value="INTERMITTENT">Intermittent</option>
                  <option value="UNKNOWN">Unknown</option>
                </select>
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Notes</label>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  rows={4}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save Problem</button>
            </div>
          </form>
        </div>
      )}

      {/* Resolve Dialog */}
      {showResolveDialog && problemToResolve && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '500px', width: 'min(500px, 100%)' }}>
            <div className="modal-body">
            <h3>Resolve Problem</h3>
            <p><strong>{problemToResolve.problemName}</strong></p>
            <div className="form-group">
              <label>Resolution Date *</label>
              <input
                type="date"
                required
                value={resolveData.resolutionDate}
                onChange={(e) => setResolveData({ ...resolveData, resolutionDate: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Resolution Notes</label>
              <textarea
                value={resolveData.resolutionNotes}
                onChange={(e) => setResolveData({ ...resolveData, resolutionNotes: e.target.value })}
                rows={4}
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowResolveDialog(false); setProblemToResolve(null); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleResolve}>Resolve Problem</button>
            </div>
            </div>
          </div>
        </div>
      )}

      {/* View Problem Dialog */}
      {viewing && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '700px' }}>
            <div className="modal-header">
              <h3>Problem Details</h3>
              <button className="modal-close" onClick={() => setViewing(null)} aria-label="Close">×</button>
            </div>
            <div className="modal-body">
            <div className="info-card">
              <div className="info-row">
                <span className="info-label">Problem Name:</span>
                <span className="info-value"><strong>{viewing.problemName}</strong></span>
              </div>
              <div className="info-row">
                <span className="info-label">Type:</span>
                <span className="info-value">{formatClinicalEnumLabel(viewing.problemType)}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Status:</span>
                <span className="info-value">{formatProblemStatusLabel(viewing.status)}</span>
              </div>
              {viewing.icd10Code && (
                <div className="info-row">
                  <span className="info-label">ICD-10:</span>
                  <span className="info-value">
                    {viewing.icd10Code} {viewing.problemName ? `- ${viewing.problemName}` : ''}
                  </span>
                </div>
              )}
              {viewing.icd11Code && (
                <div className="info-row">
                  <span className="info-label">ICD-11:</span>
                  <span className="info-value">
                    {viewing.icd11Code} {viewing.problemName ? `- ${viewing.problemName}` : ''}
                  </span>
                </div>
              )}
              {viewing.snomedCode && (
                <div className="info-row">
                  <span className="info-label">SNOMED:</span>
                  <span className="info-value">
                    {viewing.snomedCode} {viewing.problemName ? `- ${viewing.problemName}` : ''}
                  </span>
                </div>
              )}
              {viewing.onsetDate && (
                <div className="info-row">
                  <span className="info-label">Onset Date:</span>
                  <span className="info-value">{formatDate(viewing.onsetDate)}</span>
                </div>
              )}
              {viewing.resolutionDate && (
                <div className="info-row">
                  <span className="info-label">Resolution Date:</span>
                  <span className="info-value">{formatDate(viewing.resolutionDate)}</span>
                </div>
              )}
              {viewing.severity && (
                <div className="info-row">
                  <span className="info-label">Severity:</span>
                  <span className="info-value">{formatClinicalEnumLabel(viewing.severity)}</span>
                </div>
              )}
              {viewing.chronicity && (
                <div className="info-row">
                  <span className="info-label">Chronicity:</span>
                  <span className="info-value">{formatClinicalEnumLabel(viewing.chronicity)}</span>
                </div>
              )}
              {viewing.priority && (
                <div className="info-row">
                  <span className="info-label">Priority:</span>
                  <span className="info-value">{formatClinicalEnumLabel(viewing.priority)}</span>
                </div>
              )}
              {viewing.notes && (
                <div className="info-row">
                  <span className="info-label">Notes:</span>
                  <span className="info-value" style={{ whiteSpace: 'pre-wrap' }}>{viewing.notes}</span>
                </div>
              )}
            </div>
            {/* Linked Medications */}
            <div className="info-card" style={{ marginTop: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h4 className="info-card-title">
                  Linked Medications ({problemMedications.length})
                </h4>
                <button 
                  className="btn-link" 
                  onClick={() => {
                    loadAvailableMedications();
                    setShowLinkMedicationDialog(true);
                  }}
                >
                  + Link Medication
                </button>
              </div>
              {problemMedications.length > 0 ? (
                <div style={{ display: 'grid', gap: '12px', marginTop: '12px' }}>
                  {problemMedications.map((med) => (
                    <div key={med.linkId} style={{ 
                      padding: '12px', 
                      background: '#f9fafb', 
                      borderRadius: '8px',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600 }}>{med.medicationName}</div>
                        {med.genericName && (
                          <div style={{ fontSize: '14px', color: '#6b7280', marginTop: '4px' }}>
                            Generic: {med.genericName}
                          </div>
                        )}
                        <div style={{ fontSize: '12px', color: '#9ca3af', marginTop: '4px', display: 'flex', gap: '12px' }}>
                          <span>Type: {med.linkType}</span>
                          <span>Strength: {med.linkStrength}</span>
                        </div>
                        {med.clinicalRelevance && (
                          <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                            {med.clinicalRelevance}
                          </div>
                        )}
                      </div>
                      <button 
                        className="btn-link btn-danger" 
                        onClick={() => handleUnlinkMedication(med.medicationId)}
                        style={{ marginLeft: '12px' }}
                      >
                        Unlink
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ color: '#6b7280', fontStyle: 'italic' }}>No medications linked to this problem</p>
              )}
            </div>
            <div style={{ marginTop: '16px' }}>
              <button className="btn-secondary" onClick={() => handleViewHistory(viewing.problemId)}>
                View History
              </button>
            </div>
            </div>
          </div>
        </div>
      )}

      {/* Link Medication Dialog */}
      {showLinkMedicationDialog && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_NESTED_Z }}>
          <div className="modal-content" style={{ maxWidth: '500px', width: 'min(500px, 100%)' }}>
            <div className="modal-body">
            <h3>Link Medication to Problem</h3>
            <div className="form-group">
              <label>Medication *</label>
              <select
                required
                value={selectedMedicationId}
                onChange={(e) => setSelectedMedicationId(e.target.value)}
              >
                <option value="">Select a medication...</option>
                {availableMedications.map((med) => (
                  <option key={med.medicationId} value={med.medicationId}>
                    {med.medicationName} {med.genericName ? `(${med.genericName})` : ''}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Link Type</label>
              <select
                value={linkType}
                onChange={(e) => setLinkType(e.target.value)}
              >
                <option value="TREATS">Treats</option>
                <option value="CAUSES">Causes</option>
                <option value="PREVENTS">Prevents</option>
                <option value="RELATED">Related</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label>Link Strength</label>
              <select
                value={linkStrength}
                onChange={(e) => setLinkStrength(e.target.value)}
              >
                <option value="WEAK">Weak</option>
                <option value="MODERATE">Moderate</option>
                <option value="STRONG">Strong</option>
              </select>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowLinkMedicationDialog(false); setSelectedMedicationId(''); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleLinkMedication}>Link Medication</button>
            </div>
            </div>
          </div>
        </div>
      )}

      {/* History Dialog */}
      {showHistory && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '700px', width: 'min(700px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3>Problem History</h3>
              <button className="btn-secondary" onClick={() => { setShowHistory(false); setProblemHistory([]); }}>Close</button>
            </div>
            {problemHistory.length === 0 ? (
              <p>No history available</p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Change Type</th>
                      <th>Field</th>
                      <th>Previous Value</th>
                      <th>New Value</th>
                      <th>Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {problemHistory.map((history) => (
                      <tr key={history.historyId}>
                        <td>{new Date(history.changedDate).toLocaleString()}</td>
                        <td>{formatClinicalEnumLabel(history.changeType)}</td>
                        <td>{history.fieldName || '-'}</td>
                        <td>{formatHistoryFieldValue(history.fieldName, history.previousValue)}</td>
                        <td>{formatHistoryFieldValue(history.fieldName, history.newValue)}</td>
                        <td>{history.changeReason || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* Problems List */}
      {problems.length === 0 ? (
        <div className="empty-state">
          <p>No problems found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Problem Name</th>
                <th>Type</th>
                <th>ICD-10</th>
                <th>Status</th>
                <th>Severity</th>
                <th>Priority</th>
                <th>Onset Date</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {problems.map((problem) => (
                <tr key={problem.problemId}>
                  <td><strong>{problem.problemName}</strong></td>
                  <td>{formatClinicalEnumLabel(problem.problemType)}</td>
                  <td>{problem.icd10Code || '-'}</td>
                  <td>{formatProblemStatusLabel(problem.status)}</td>
                  <td>{problem.severity ? formatClinicalEnumLabel(problem.severity) : '-'}</td>
                  <td>{problem.priority ? formatClinicalEnumLabel(problem.priority) : '-'}</td>
                  <td>{formatDate(problem.onsetDate)}</td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleView(problem.problemId)}>View</button>
                      <button className="btn-link" onClick={() => handleEdit(problem)}>Edit</button>
                      {problem.status === 'ACTIVE' && (
                        <button className="btn-link" onClick={() => { setProblemToResolve(problem); setShowResolveDialog(true); }}>
                          Resolve
                        </button>
                      )}
                      {problem.status === 'RESOLVED' && (
                        <button className="btn-link" onClick={() => handleReactivate(problem.problemId)}>Reactivate</button>
                      )}
                      <button className="btn-link" onClick={() => handleViewHistory(problem.problemId)}>History</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(problem.problemId)}>Delete</button>
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

export default ProblemListPage;
