import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import hospitalService, { Immunization, ImmunizationRequest, Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const CVX_OPTIONS: Array<{ code: string; label: string }> = [
  { code: '08', label: '08 - Hep B, adolescent or pediatric' },
  { code: '10', label: '10 - IPV (Polio), inactivated' },
  { code: '20', label: '20 - DTaP (Diphtheria, Tetanus, Pertussis)' },
  { code: '21', label: '21 - Varicella (Chickenpox)' },
  { code: '33', label: '33 - Pneumococcal polysaccharide (PPSV23)' },
  { code: '88', label: '88 - Influenza, unspecified formulation' },
  { code: '94', label: '94 - MMR (Measles, Mumps, Rubella)' },
  { code: '115', label: '115 - Tdap' },
  { code: '140', label: '140 - Influenza, seasonal, injectable, preservative free' },
  { code: '141', label: '141 - Influenza, seasonal, injectable' },
  { code: '158', label: '158 - Influenza, injectable, quadrivalent, preservative free' },
  { code: '165', label: '165 - HPV9 (Human Papillomavirus 9-valent)' },
  { code: '207', label: '207 - COVID-19, mRNA, LNP-S, PF, 100 mcg/0.5 mL dose' },
  { code: '208', label: '208 - COVID-19, mRNA, LNP-S, PF, 30 mcg/0.3 mL dose' },
  { code: '210', label: '210 - COVID-19, vector-nr, rS-ChAdOx1' },
  { code: '212', label: '212 - COVID-19, vector-nr, Ad26, PF' },
];

const ImmunizationRecordsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [immunizations, setImmunizations] = useState<Immunization[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Immunization | null>(null);
  const [formData, setFormData] = useState<ImmunizationRequest>({
    vaccineName: '',
    cvxCode: '',
    administrationDate: new Date().toISOString().split('T')[0],
    lotNumber: '',
    manufacturer: '',
    route: 'IM',
    site: '',
    dose: '',
    reaction: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadImmunizations();
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

  const loadImmunizations = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getImmunizations(id);
      setImmunizations(response.data);
    } catch (err: any) {
      console.error('Failed to load immunizations:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load immunizations'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      if (editing) {
        await hospitalService.updateImmunization(id, editing.immunizationId, formData);
      } else {
        await hospitalService.createImmunization(id, formData);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        vaccineName: '',
        cvxCode: '',
        administrationDate: new Date().toISOString().split('T')[0],
        lotNumber: '',
        manufacturer: '',
        route: 'IM',
        site: '',
        dose: '',
        reaction: '',
        notes: '',
      });
      loadImmunizations();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save immunization'));
    }
  };

  const handleEdit = (immunization: Immunization) => {
    setEditing(immunization);
    setFormData({
      vaccineName: immunization.vaccineName,
      cvxCode: immunization.cvxCode || '',
      administrationDate: immunization.administrationDate,
      lotNumber: immunization.lotNumber || '',
      manufacturer: immunization.manufacturer || '',
      route: immunization.route || 'IM',
      site: immunization.site || '',
      dose: immunization.dose || '',
      reaction: immunization.reaction || '',
      notes: immunization.notes || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (immunizationId: string) => {
    if (!window.confirm('Are you sure you want to delete this immunization record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteImmunization(id, immunizationId);
      loadImmunizations();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete immunization'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading immunizations...</div>;
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
        <h3 style={{ margin: 0 }}>Immunization Records</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Immunization
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Immunization</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Vaccine Name *</label>
                <input
                  type="text"
                  required
                  value={formData.vaccineName}
                  onChange={(e) => setFormData({ ...formData, vaccineName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <FormControl fullWidth size="small">
                  <InputLabel id="immunization-cvx-label">CVX Code</InputLabel>
                  <Select
                    labelId="immunization-cvx-label"
                    label="CVX Code"
                    value={formData.cvxCode || ''}
                    onChange={(e) => setFormData({ ...formData, cvxCode: e.target.value as string })}
                    MenuProps={{ PaperProps: { sx: { maxHeight: 280 } } }}
                  >
                    <MenuItem value="">
                      <em>Select CVX code</em>
                    </MenuItem>
                    {CVX_OPTIONS.map((option) => (
                      <MenuItem key={option.code} value={option.code}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </div>
              <div className="form-group">
                <label>Administration Date *</label>
                <input
                  type="date"
                  required
                  value={formData.administrationDate}
                  onChange={(e) => setFormData({ ...formData, administrationDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Lot Number</label>
                <input
                  type="text"
                  value={formData.lotNumber}
                  onChange={(e) => setFormData({ ...formData, lotNumber: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Manufacturer</label>
                <input
                  type="text"
                  value={formData.manufacturer}
                  onChange={(e) => setFormData({ ...formData, manufacturer: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Route</label>
                <select
                  value={formData.route}
                  onChange={(e) => setFormData({ ...formData, route: e.target.value as any })}
                >
                  <option value="IM">Intramuscular (IM)</option>
                  <option value="SC">Subcutaneous (SC)</option>
                  <option value="ID">Intradermal (ID)</option>
                  <option value="IN">Intranasal (IN)</option>
                  <option value="PO">Oral (PO)</option>
                  <option value="IV">Intravenous (IV)</option>
                  <option value="NASAL">Nasal</option>
                  <option value="OPHTHALMIC">Ophthalmic</option>
                  <option value="OTIC">Otic</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>Site</label>
                <input
                  type="text"
                  value={formData.site}
                  onChange={(e) => setFormData({ ...formData, site: e.target.value })}
                  placeholder="e.g., Left arm, Right deltoid"
                />
              </div>
              <div className="form-group">
                <label>Dose</label>
                <input
                  type="text"
                  value={formData.dose}
                  onChange={(e) => setFormData({ ...formData, dose: e.target.value })}
                  placeholder="e.g., 0.5 mL"
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Reaction</label>
                <textarea
                  value={formData.reaction}
                  onChange={(e) => setFormData({ ...formData, reaction: e.target.value })}
                  placeholder="Any adverse reactions to the vaccine"
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

      {immunizations.length === 0 ? (
        <div className="empty-state">
          <p>No immunization records found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Vaccine</th>
                <th>CVX Code</th>
                <th>Date</th>
                <th>Route</th>
                <th>Site</th>
                <th>Dose</th>
                <th>Manufacturer</th>
                <th>Lot Number</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {immunizations.map((immunization) => (
                <tr key={immunization.immunizationId}>
                  <td><strong>{immunization.vaccineName}</strong></td>
                  <td>{immunization.cvxCode || '-'}</td>
                  <td>{formatDate(immunization.administrationDate)}</td>
                  <td>{immunization.route || '-'}</td>
                  <td>{immunization.site || '-'}</td>
                  <td>{immunization.dose || '-'}</td>
                  <td>{immunization.manufacturer || '-'}</td>
                  <td>{immunization.lotNumber || '-'}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(immunization)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(immunization.immunizationId)}>Delete</button>
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

export default ImmunizationRecordsPage;
