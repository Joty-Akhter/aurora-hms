import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getCaseById, createCase, updateCase, getAccounts, getContacts } from '../../services/crmService';
import './Crm.css';

const CaseForm: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);

  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  const [formData, setFormData] = useState({
    organizationId,
    subject: '',
    description: '',
    caseType: 'QUESTION',
    status: 'NEW',
    priority: 'MEDIUM',
    contactId: '',
    accountId: '',
    origin: 'WEB',
    category: ''
  });

  const [accounts, setAccounts] = useState<any[]>([]);
  const [contacts, setContacts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadFormData();
  }, [id]);

  const loadFormData = async () => {
    try {
      const [accountsData, contactsData] = await Promise.all([
        getAccounts(organizationId),
        getContacts(organizationId)
      ]);
      setAccounts(accountsData.data);
      setContacts(contactsData.data);

      if (isEdit && id) {
        const caseData = await getCaseById(id);
        setFormData(caseData);
      }
    } catch (error) {
      console.error('Error loading form data:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEdit && id) {
        await updateCase(id, formData);
      } else {
        await createCase(formData);
      }
      navigate('/crm/cases');
    } catch (error) {
      console.error('Error saving case:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>{isEdit ? 'Edit Case' : 'New Case'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="crm-form">
        <div className="crm-form-grid">
          <div className="crm-form-group">
            <label>Subject *</label>
            <input type="text" name="subject" value={formData.subject} onChange={handleChange} required />
          </div>

          <div className="crm-form-group">
            <label>Type *</label>
            <select name="caseType" value={formData.caseType} onChange={handleChange} required>
              <option value="QUESTION">Question</option>
              <option value="PROBLEM">Problem</option>
              <option value="FEATURE_REQUEST">Feature Request</option>
              <option value="BUG">Bug</option>
              <option value="COMPLAINT">Complaint</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Priority *</label>
            <select name="priority" value={formData.priority} onChange={handleChange} required>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Origin</label>
            <select name="origin" value={formData.origin} onChange={handleChange}>
              <option value="EMAIL">Email</option>
              <option value="PHONE">Phone</option>
              <option value="WEB">Web</option>
              <option value="CHAT">Chat</option>
              <option value="SOCIAL">Social Media</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Account</label>
            <select name="accountId" value={formData.accountId} onChange={handleChange}>
              <option value="">Select Account</option>
              {accounts.map(account => (
                <option key={account.accountId} value={account.accountId}>
                  {account.accountName}
                </option>
              ))}
            </select>
          </div>

          <div className="crm-form-group">
            <label>Contact</label>
            <select name="contactId" value={formData.contactId} onChange={handleChange}>
              <option value="">Select Contact</option>
              {contacts.map(contact => (
                <option key={contact.contactId} value={contact.contactId}>
                  {contact.firstName} {contact.lastName}
                </option>
              ))}
            </select>
          </div>

          <div className="crm-form-group">
            <label>Category</label>
            <input type="text" name="category" value={formData.category} onChange={handleChange} />
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Description *</label>
            <textarea name="description" value={formData.description} onChange={handleChange} rows={5} required />
          </div>
        </div>

        <div className="crm-form-actions">
          <button type="button" className="crm-btn-secondary" onClick={() => navigate('/crm/cases')} disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="crm-btn-primary" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Update Case' : 'Create Case')}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CaseForm;

