import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getContacts, deleteContact, Contact } from '../../services/crmService';
import './Crm.css';

const ContactList: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();

  const [contacts, setContacts] = useState<Contact[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setContacts([]);
      setError('No organization selected');
      return;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getContacts(currentOrganizationId, {
          search: searchTerm || undefined,
        });
        if (!controller.signal.aborted) {
          setContacts(response.data ?? []);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          console.error('Failed to load contacts:', err);
          setError('Failed to load contacts');
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }, 250);

    return () => {
      controller.abort();
      clearTimeout(timer);
    };
  }, [currentOrganizationId, searchTerm]);

  const stats = useMemo(() => {
    const total = contacts.length;
    const primary = contacts.filter((contact) => contact.isPrimary).length;
    const active = contacts.filter((contact) => contact.isActive).length;
    return { total, primary, active };
  }, [contacts]);

  const handleDelete = async (contactId: string) => {
    if (!confirm('Delete this contact?')) return;
    try {
      await deleteContact(contactId);
      if (currentOrganizationId) {
        const response = await getContacts(currentOrganizationId, {
          search: searchTerm || undefined,
        });
        setContacts(response.data ?? []);
      }
    } catch (err) {
      console.error('Failed to delete contact:', err);
      alert('Failed to delete contact');
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Contacts</h1>
          <p>Maintain individual relationships and key stakeholders</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/contacts/new')}>
            + New Contact
          </button>
        </div>
      </div>

      <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
        <div className="crm-summary-card">
          <h3>Total Contacts</h3>
          <div className="crm-card-value">{stats.total}</div>
          <small>Synced with CRM accounts</small>
        </div>
        <div className="crm-summary-card">
          <h3>Primary Contacts</h3>
          <div className="crm-card-value">{stats.primary}</div>
          <small>Marked as primary for communication</small>
        </div>
        <div className="crm-summary-card">
          <h3>Active</h3>
          <div className="crm-card-value">{stats.active}</div>
          <small>Currently active in this organization</small>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="contact-search">Search</label>
            <input
              id="contact-search"
              type="text"
              placeholder="Search by name, email or phone"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading contacts...
          </div>
        ) : contacts.length === 0 ? (
          <div className="crm-empty-state">
            <p>No contacts match your filters.</p>
            <div className="empty-actions">
              <button className="btn-secondary" onClick={() => setSearchTerm('')}>
                Clear Search
              </button>
              <button className="btn-primary" onClick={() => navigate('/crm/contacts/new')}>
                Create Contact
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Contact</th>
                <th>Job Title</th>
                <th>Account</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Mobile</th>
                <th>Primary</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {contacts.map((contact) => (
                <tr key={contact.contactId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>
                        {[contact.firstName, contact.lastName].filter(Boolean).join(' ') || contact.email || 'Contact'}
                      </strong>
                      <small>{contact.email || '--'}</small>
                    </div>
                  </td>
                  <td>{contact.jobTitle || '--'}</td>
                  <td>{contact.accountId || '--'}</td>
                  <td>{contact.email || '--'}</td>
                  <td>{contact.phone || '--'}</td>
                  <td>{contact.mobile || '--'}</td>
                  <td>{contact.isPrimary ? '* Primary' : '--'}</td>
                  <td>
                    <span className={`status-badge ${contact.isActive ? 'status-qualified' : 'status-unqualified'}`}>
                      {contact.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-sm btn-primary"
                        onClick={() => navigate(`/crm/contacts/${contact.contactId}`)}
                      >
                        View
                      </button>
                      <button
                        className="btn-sm btn-secondary"
                        onClick={() => navigate(`/crm/contacts/${contact.contactId}/edit`)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn-sm btn-disqualify"
                        onClick={() => handleDelete(contact.contactId!)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default ContactList;






