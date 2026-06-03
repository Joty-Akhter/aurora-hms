import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { createLead, updateLead, getLeadById, Lead } from '../../services/crmService';
import './Crm.css';

const LeadForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState<Partial<Lead>>({
    organizationId: currentOrganizationId || '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    mobile: '',
    company: '',
    jobTitle: '',
    status: 'NEW',
    rating: 'WARM',
    leadScore: 50,
    industry: '',
    street: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
    website: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadLead(id);
    }
  }, [id]);

  const loadLead = async (leadId: string) => {
    try {
      const response = await getLeadById(leadId);
      setFormData(response.data);
    } catch (err) {
      setError('Failed to load lead');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      const leadData = { ...formData, organizationId: currentOrganizationId };

      if (id) {
        await updateLead(id, leadData);
      } else {
        await createLead(leadData as Lead);
      }

      navigate('/crm/leads');
    } catch (err) {
      setError('Failed to save lead');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{id ? 'Edit Lead' : 'New Lead'}</h1>
        <p>Enter lead information</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
          <h3>Basic Information</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-row">
              <label>First Name *</label>
              <input
                type="text"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <label>Last Name *</label>
              <input
                type="text"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <label>Email *</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <label>Phone</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Company *</label>
              <input
                type="text"
                value={formData.company}
                onChange={(e) => setFormData({ ...formData, company: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <label>Job Title</label>
              <input
                type="text"
                value={formData.jobTitle}
                onChange={(e) => setFormData({ ...formData, jobTitle: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Status</label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              >
                <option value="NEW">New</option>
                <option value="CONTACTED">Contacted</option>
                <option value="QUALIFIED">Qualified</option>
                <option value="UNQUALIFIED">Unqualified</option>
              </select>
            </div>
            <div className="form-row">
              <label>Rating</label>
              <select
                value={formData.rating}
                onChange={(e) => setFormData({ ...formData, rating: e.target.value })}
              >
                <option value="HOT">Hot</option>
                <option value="WARM">Warm</option>
                <option value="COLD">Cold</option>
              </select>
            </div>
          </div>

          <h3 style={{ marginTop: '2rem' }}>Address</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-row">
              <label>Street</label>
              <input
                type="text"
                value={formData.street}
                onChange={(e) => setFormData({ ...formData, street: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>City</label>
              <input
                type="text"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>State</label>
              <input
                type="text"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Country</label>
              <input
                type="text"
                value={formData.country}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row" style={{ marginTop: '1rem' }}>
            <label>Notes</label>
            <textarea
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              rows={4}
            />
          </div>

          <div className="form-actions">
            <button type="button" onClick={() => navigate('/crm/leads')} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="btn-primary">
              {id ? 'Update' : 'Create'} Lead
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default LeadForm;













