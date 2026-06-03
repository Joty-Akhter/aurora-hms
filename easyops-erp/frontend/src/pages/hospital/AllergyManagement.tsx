import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { Allergy, AllergyRequest, Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatClinicalEnumLabel, formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ALLERGEN_CODE_OPTIONS: Record<AllergyRequest['allergenType'], string[]> = {
  DRUG: ['RXNORM', 'NDC', 'SNOMED_CT', 'UNII'],
  FOOD: ['SNOMED_CT', 'FOODON', 'LOINC'],
  ENVIRONMENTAL: ['SNOMED_CT', 'ICD10_CM', 'LOINC'],
  LATEX: ['SNOMED_CT', 'UNII'],
  OTHER: ['SNOMED_CT', 'LOCAL_CUSTOM'],
};

const getDefaultAllergenCode = (allergenType: AllergyRequest['allergenType']) =>
  ALLERGEN_CODE_OPTIONS[allergenType][0] || '';

const AllergyManagementPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [allergies, setAllergies] = useState<Allergy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Allergy | null>(null);
  const [formData, setFormData] = useState<AllergyRequest>({
    allergenName: '',
    allergenType: 'DRUG',
    allergenCode: getDefaultAllergenCode('DRUG'),
    reactionType: '',
    severity: 'MILD',
    onsetDate: '',
    status: 'ACTIVE',
    verificationStatus: 'UNCONFIRMED',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadAllergies();
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

  const loadAllergies = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getAllergies(id);
      setAllergies(response.data);
    } catch (err: any) {
      console.error('Failed to load allergies:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load allergies'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      if (editing) {
        await hospitalService.updateAllergy(id, editing.allergyId, formData);
      } else {
        await hospitalService.createAllergy(id, formData);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        allergenName: '',
        allergenType: 'DRUG',
        allergenCode: getDefaultAllergenCode('DRUG'),
        reactionType: '',
        severity: 'MILD',
        onsetDate: '',
        status: 'ACTIVE',
        verificationStatus: 'UNCONFIRMED',
        notes: '',
      });
      loadAllergies();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save allergy'));
    }
  };

  const handleEdit = (allergy: Allergy) => {
    setEditing(allergy);
    setFormData({
      allergenName: allergy.allergenName,
      allergenType: allergy.allergenType,
      allergenCode: allergy.allergenCode || '',
      reactionType: allergy.reactionType || '',
      severity: allergy.severity,
      onsetDate: allergy.onsetDate || '',
      status: allergy.status,
      verificationStatus: allergy.verificationStatus,
      notes: allergy.notes || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (allergyId: string) => {
    if (!window.confirm('Are you sure you want to delete this allergy record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteAllergy(id, allergyId);
      loadAllergies();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete allergy'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const getSeverityBadgeClass = (severity: string) => {
    switch (severity) {
      case 'LIFE_THREATENING': return 'status-badge status-deceased';
      case 'SEVERE': return 'status-badge status-inactive';
      case 'MODERATE': return 'status-badge';
      default: return 'status-badge status-active';
    }
  };

  const handleAllergenTypeChange = (allergenType: AllergyRequest['allergenType']) => {
    const options = ALLERGEN_CODE_OPTIONS[allergenType];
    const existingCode = formData.allergenCode || '';
    const nextAllergenCode = existingCode && options.includes(existingCode)
      ? existingCode
      : getDefaultAllergenCode(allergenType);

    setFormData({
      ...formData,
      allergenType,
      allergenCode: nextAllergenCode,
    });
  };

  if (loading) {
    return <div className="loading">Loading allergies...</div>;
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
        <h3 style={{ margin: 0 }}>Allergies & Adverse Reactions</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Allergy
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Allergy</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Allergen Name *</label>
                <input
                  type="text"
                  required
                  value={formData.allergenName}
                  onChange={(e) => setFormData({ ...formData, allergenName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Allergen Type *</label>
                <select
                  required
                  value={formData.allergenType}
                  onChange={(e) => handleAllergenTypeChange(e.target.value as AllergyRequest['allergenType'])}
                >
                  <option value="DRUG">Drug</option>
                  <option value="FOOD">Food</option>
                  <option value="ENVIRONMENTAL">Environmental</option>
                  <option value="LATEX">Latex</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>Allergen Code</label>
                <select
                  value={formData.allergenCode || ''}
                  onChange={(e) => setFormData({ ...formData, allergenCode: e.target.value })}
                >
                  {ALLERGEN_CODE_OPTIONS[formData.allergenType].map((code) => (
                    <option key={code} value={code}>
                      {code}
                    </option>
                  ))}
                  {formData.allergenCode &&
                    !ALLERGEN_CODE_OPTIONS[formData.allergenType].includes(formData.allergenCode) && (
                      <option value={formData.allergenCode}>{formData.allergenCode}</option>
                    )}
                </select>
              </div>
              <div className="form-group">
                <label>Reaction Type</label>
                <input
                  type="text"
                  value={formData.reactionType}
                  onChange={(e) => setFormData({ ...formData, reactionType: e.target.value })}
                  placeholder="e.g., Hives, Anaphylaxis"
                />
              </div>
              <div className="form-group">
                <label>Severity *</label>
                <select
                  required
                  value={formData.severity}
                  onChange={(e) => setFormData({ ...formData, severity: e.target.value as any })}
                >
                  <option value="MILD">Mild</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="SEVERE">Severe</option>
                  <option value="LIFE_THREATENING">Life Threatening</option>
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
                <label>Status</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="UNKNOWN">Unknown</option>
                </select>
              </div>
              <div className="form-group">
                <label>Verification Status</label>
                <select
                  value={formData.verificationStatus}
                  onChange={(e) => setFormData({ ...formData, verificationStatus: e.target.value as any })}
                >
                  <option value="UNCONFIRMED">Unconfirmed</option>
                  <option value="CONFIRMED">Confirmed</option>
                  <option value="REFUTED">Refuted</option>
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

      {allergies.length === 0 ? (
        <div className="empty-state">
          <p>No allergies recorded</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Allergen</th>
                <th>Type</th>
                <th>Reaction</th>
                <th>Severity</th>
                <th>Status</th>
                <th>Verification</th>
                <th>Onset Date</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {allergies.map((allergy) => (
                <tr key={allergy.allergyId}>
                  <td><strong>{allergy.allergenName}</strong></td>
                  <td>{formatClinicalEnumLabel(allergy.allergenType)}</td>
                  <td>{allergy.reactionType ? formatClinicalEnumLabel(allergy.reactionType) : '-'}</td>
                  <td>
                    <span className={getSeverityBadgeClass(allergy.severity)}>
                      {formatClinicalEnumLabel(allergy.severity)}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge status-${allergy.status.toLowerCase()}`}>
                      {formatClinicalEnumLabel(allergy.status)}
                    </span>
                  </td>
                  <td>{formatClinicalEnumLabel(allergy.verificationStatus)}</td>
                  <td>{formatDate(allergy.onsetDate)}</td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(allergy)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(allergy.allergyId)}>Delete</button>
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

export default AllergyManagementPage;
