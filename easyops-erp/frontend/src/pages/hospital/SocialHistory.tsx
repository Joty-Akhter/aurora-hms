import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { SocialHistory, SocialHistoryRequest, Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const SocialHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [socialHistory, setSocialHistory] = useState<SocialHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<SocialHistory | null>(null);
  const [formData, setFormData] = useState<SocialHistoryRequest>({
    category: 'SMOKING',
    status: 'CURRENT',
    frequency: '',
    quantity: '',
    durationYears: undefined,
    startDate: '',
    endDate: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadSocialHistory();
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

  const loadSocialHistory = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getSocialHistory(id);
      setSocialHistory(response.data);
    } catch (err: any) {
      console.error('Failed to load social history:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load social history'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    if (formData.category === 'OTHER' && !(formData.notes ?? '').trim()) {
      alert('Please provide details when category is Other.');
      return;
    }

    try {
      if (editing) {
        await hospitalService.updateSocialHistory(id, editing.socialHistoryId, formData);
      } else {
        await hospitalService.createSocialHistory(id, formData);
      }
      setShowForm(false);
      setEditing(null);
      setFormData({
        category: 'SMOKING',
        status: 'CURRENT',
        frequency: '',
        quantity: '',
        durationYears: undefined,
        startDate: '',
        endDate: '',
        notes: '',
      });
      loadSocialHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save social history'));
    }
  };

  const handleEdit = (item: SocialHistory) => {
    setEditing(item);
    setFormData({
      category: item.category,
      status: item.status,
      frequency: item.frequency || '',
      quantity: item.quantity || '',
      durationYears: item.durationYears,
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      notes: item.notes || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (socialHistoryId: string) => {
    if (!window.confirm('Are you sure you want to delete this social history record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteSocialHistory(id, socialHistoryId);
      loadSocialHistory();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete social history'));
    }
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading social history...</div>;
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
        <h3 style={{ margin: 0 }}>Social History</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); }}>
            + Add Social History
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Social History</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div
                className="form-group"
                style={
                  formData.category === 'OTHER'
                    ? {
                        gridColumn: 'span 2',
                        display: 'flex',
                        gap: '16px',
                        alignItems: 'flex-start',
                        flexWrap: 'nowrap',
                      }
                    : {}
                }
              >
                <div style={{ flex: formData.category === 'OTHER' ? '0 0 220px' : undefined }}>
                  <label>Category *</label>
                  <select
                    required
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value as any })}
                  >
                    <option value="SMOKING">Smoking</option>
                    <option value="ALCOHOL">Alcohol</option>
                    <option value="DRUGS">Drugs</option>
                    <option value="OCCUPATION">Occupation</option>
                    <option value="LIFESTYLE">Lifestyle</option>
                    <option value="EXERCISE">Exercise</option>
                    <option value="DIET">Diet</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                {formData.category === 'OTHER' && (
                  <div style={{ flex: 1 }}>
                    <label>Please explain *</label>
                    <textarea
                      required
                      rows={2}
                      value={formData.notes}
                      onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                      placeholder="Enter details for Other social history category"
                    />
                  </div>
                )}
              </div>
              <div className="form-group">
                <label>Status *</label>
                <select
                  required
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                >
                  <option value="CURRENT">Current</option>
                  <option value="PAST">Past</option>
                  <option value="NEVER">Never</option>
                </select>
              </div>
              <div className="form-group">
                <label>Frequency</label>
                <input
                  type="text"
                  value={formData.frequency}
                  onChange={(e) => setFormData({ ...formData, frequency: e.target.value })}
                  placeholder="e.g., Daily, Weekly, Occasionally"
                />
              </div>
              <div className="form-group">
                <label>Quantity</label>
                <input
                  type="text"
                  value={formData.quantity}
                  onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                  placeholder="e.g., 1 pack/day, 2 drinks/week"
                />
              </div>
              <div className="form-group">
                <label>Duration (Years)</label>
                <input
                  type="number"
                  value={formData.durationYears || ''}
                  onChange={(e) => setFormData({ ...formData, durationYears: e.target.value ? parseInt(e.target.value) : undefined })}
                />
              </div>
              <div className="form-group">
                <label>Start Date</label>
                <input
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>End Date</label>
                <input
                  type="date"
                  value={formData.endDate}
                  onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                />
              </div>
              {formData.category !== 'OTHER' && (
                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                  <label>Notes</label>
                  <textarea
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  />
                </div>
              )}
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

      {socialHistory.length === 0 ? (
        <div className="empty-state">
          <p>No social history records found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Status</th>
                <th>Frequency</th>
                <th>Quantity</th>
                <th>Duration</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {socialHistory.map((item) => (
                <tr key={item.socialHistoryId}>
                  <td><strong>{item.category}</strong></td>
                  <td>
                    <span className={`status-badge status-${item.status.toLowerCase()}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>{item.frequency || '-'}</td>
                  <td>{item.quantity || '-'}</td>
                  <td>{item.durationYears ? `${item.durationYears} years` : '-'}</td>
                  <td>{formatDate(item.startDate)}</td>
                  <td>{formatDate(item.endDate)}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(item)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(item.socialHistoryId)}>Delete</button>
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

export default SocialHistoryPage;
