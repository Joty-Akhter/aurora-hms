import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { createAccount, updateAccount, getAccountById, Account } from '../../services/crmService';
import './Crm.css';

const AccountForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState<Partial<Account>>({
    organizationId: currentOrganizationId || '',
    accountName: '',
    accountType: 'CUSTOMER',
    industry: '',
    phone: '',
    email: '',
    website: '',
    billingStreet: '',
    billingCity: '',
    billingState: '',
    billingPostalCode: '',
    billingCountry: '',
    description: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadAccount(id);
    }
  }, [id]);

  const loadAccount = async (accountId: string) => {
    try {
      const response = await getAccountById(accountId);
      setFormData(response.data);
    } catch (err) {
      alert('Failed to load account');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      const accountData = { ...formData, organizationId: currentOrganizationId };

      if (id) {
        await updateAccount(id, accountData);
      } else {
        await createAccount(accountData as Account);
      }

      navigate('/crm/accounts');
    } catch (err) {
      alert('Failed to save account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{id ? 'Edit Account' : 'New Account'}</h1>
        <p>Enter account information</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
          <h3>Basic Information</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-row">
              <label>Account Name *</label>
              <input
                type="text"
                value={formData.accountName}
                onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <label>Account Type</label>
              <select
                value={formData.accountType}
                onChange={(e) => setFormData({ ...formData, accountType: e.target.value })}
              >
                <option value="CUSTOMER">Customer</option>
                <option value="PROSPECT">Prospect</option>
                <option value="PARTNER">Partner</option>
                <option value="COMPETITOR">Competitor</option>
              </select>
            </div>
            <div className="form-row">
              <label>Industry</label>
              <input
                type="text"
                value={formData.industry}
                onChange={(e) => setFormData({ ...formData, industry: e.target.value })}
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
              <label>Email</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Website</label>
              <input
                type="url"
                value={formData.website}
                onChange={(e) => setFormData({ ...formData, website: e.target.value })}
              />
            </div>
          </div>

          <h3 style={{ marginTop: '2rem' }}>Billing Address</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-row" style={{ gridColumn: '1 / -1' }}>
              <label>Street</label>
              <input
                type="text"
                value={formData.billingStreet}
                onChange={(e) => setFormData({ ...formData, billingStreet: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>City</label>
              <input
                type="text"
                value={formData.billingCity}
                onChange={(e) => setFormData({ ...formData, billingCity: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>State</label>
              <input
                type="text"
                value={formData.billingState}
                onChange={(e) => setFormData({ ...formData, billingState: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Postal Code</label>
              <input
                type="text"
                value={formData.billingPostalCode}
                onChange={(e) => setFormData({ ...formData, billingPostalCode: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Country</label>
              <input
                type="text"
                value={formData.billingCountry}
                onChange={(e) => setFormData({ ...formData, billingCountry: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row" style={{ marginTop: '1rem' }}>
            <label>Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={4}
            />
          </div>

          <div className="form-actions">
            <button type="button" onClick={() => navigate('/crm/accounts')} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="btn-primary">
              {id ? 'Update' : 'Create'} Account
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default AccountForm;













