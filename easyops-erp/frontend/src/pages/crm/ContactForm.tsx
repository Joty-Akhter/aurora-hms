import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createContact, updateContact, getContactById, getAccounts, Contact } from '../../services/crmService';
import './Crm.css';

const ContactForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [accounts, setAccounts] = useState<any[]>([]);

  const [formData, setFormData] = useState<Partial<Contact>>({
    organizationId: currentOrganizationId || '',
    accountId: searchParams.get('accountId') || '',
    firstName: '',
    lastName: '',
    salutation: '',
    jobTitle: '',
    department: '',
    email: '',
    phone: '',
    mobile: '',
    isPrimary: false,
    isActive: true,
    notes: '',
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadAccounts();
    }
    if (id) {
      loadContact(id);
    }
  }, [id, currentOrganizationId]);

  const loadAccounts = async () => {
    if (!currentOrganizationId) return;
    try {
      const response = await getAccounts(currentOrganizationId);
      setAccounts(response.data);
    } catch (err) {
      console.error('Failed to load accounts');
    }
  };

  const loadContact = async (contactId: string) => {
    try {
      const response = await getContactById(contactId);
      setFormData(response.data);
    } catch (err) {
      alert('Failed to load contact');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      const contactData = { ...formData, organizationId: currentOrganizationId };

      if (id) {
        await updateContact(id, contactData);
      } else {
        await createContact(contactData as Contact);
      }

      navigate('/crm/contacts');
    } catch (err) {
      alert('Failed to save contact');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{id ? 'Edit Contact' : 'New Contact'}</h1>
        <p>Enter contact information</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-row">
              <label>Account</label>
              <select
                value={formData.accountId}
                onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
              >
                <option value="">Select Account</option>
                {accounts.map((acc) => (
                  <option key={acc.accountId} value={acc.accountId}>
                    {acc.accountName}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-row">
              <label>Salutation</label>
              <select
                value={formData.salutation}
                onChange={(e) => setFormData({ ...formData, salutation: e.target.value })}
              >
                <option value="">Select</option>
                <option value="Mr.">Mr.</option>
                <option value="Ms.">Ms.</option>
                <option value="Mrs.">Mrs.</option>
                <option value="Dr.">Dr.</option>
              </select>
            </div>
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
              <label>Job Title</label>
              <input
                type="text"
                value={formData.jobTitle}
                onChange={(e) => setFormData({ ...formData, jobTitle: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>Department</label>
              <input
                type="text"
                value={formData.department}
                onChange={(e) => setFormData({ ...formData, department: e.target.value })}
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
              <label>Mobile</label>
              <input
                type="tel"
                value={formData.mobile}
                onChange={(e) => setFormData({ ...formData, mobile: e.target.value })}
              />
            </div>
            <div className="form-row">
              <label>
                <input
                  type="checkbox"
                  checked={formData.isPrimary}
                  onChange={(e) => setFormData({ ...formData, isPrimary: e.target.checked })}
                />
                {' '}Primary Contact
              </label>
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
            <button type="button" onClick={() => navigate('/crm/contacts')} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="btn-primary">
              {id ? 'Update' : 'Create'} Contact
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default ContactForm;













