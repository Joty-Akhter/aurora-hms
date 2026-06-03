import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getContactById } from '../../services/crmService';
import './Crm.css';

const ContactDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [contact, setContact] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      loadContact();
    }
  }, [id]);

  const loadContact = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await getContactById(id);
      setContact(response.data);
    } catch (err) {
      console.error('Failed to load contact:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading contact...</div>;
  if (!contact) return <div className="error-message">Contact not found</div>;

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{contact.salutation} {contact.firstName} {contact.lastName}</h1>
        <p>{contact.jobTitle} {contact.department && `- ${contact.department}`}</p>
        <button onClick={() => navigate(`/crm/contacts/${id}/edit`)} className="btn-primary">
          Edit Contact
        </button>
      </div>

      <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
        <h2>Contact Information</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginTop: '1rem' }}>
          <div><strong>Email:</strong> {contact.email || '-'}</div>
          <div><strong>Phone:</strong> {contact.phone || '-'}</div>
          <div><strong>Mobile:</strong> {contact.mobile || '-'}</div>
          <div><strong>Department:</strong> {contact.department || '-'}</div>
          <div><strong>Primary Contact:</strong> {contact.isPrimary ? 'Yes' : 'No'}</div>
          <div><strong>Status:</strong> {contact.isActive ? 'Active' : 'Inactive'}</div>
        </div>
        {contact.notes && (
          <div style={{ marginTop: '1.5rem' }}>
            <strong>Notes:</strong>
            <p>{contact.notes}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ContactDetail;













