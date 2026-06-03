import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAccountById, getContacts } from '../../services/crmService';
import './Crm.css';

const AccountDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [account, setAccount] = useState<any>(null);
  const [contacts, setContacts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id]);

  const loadData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const [accountRes, contactsRes] = await Promise.all([
        getAccountById(id),
        getContacts('', { accountId: id }),
      ]);
      setAccount(accountRes.data);
      setContacts(contactsRes.data);
    } catch (err) {
      console.error('Failed to load account:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading account...</div>;
  if (!account) return <div className="error-message">Account not found</div>;

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{account.accountName}</h1>
        <p>{account.accountType}</p>
        <button onClick={() => navigate(`/crm/accounts/${id}/edit`)} className="btn-primary">
          Edit Account
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        <div>
          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', marginBottom: '2rem' }}>
            <h2>Account Information</h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginTop: '1rem' }}>
              <div><strong>Industry:</strong> {account.industry || '-'}</div>
              <div><strong>Phone:</strong> {account.phone || '-'}</div>
              <div><strong>Email:</strong> {account.email || '-'}</div>
              <div><strong>Website:</strong> {account.website || '-'}</div>
              <div><strong>Annual Revenue:</strong> ${account.annualRevenue?.toLocaleString() || '-'}</div>
              <div><strong>Employees:</strong> {account.numberOfEmployees || '-'}</div>
            </div>
            {account.description && (
              <div style={{ marginTop: '1rem' }}>
                <strong>Description:</strong>
                <p>{account.description}</p>
              </div>
            )}
          </div>

          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h2>Contacts</h2>
              <button onClick={() => navigate(`/crm/contacts/new?accountId=${id}`)} className="btn-primary btn-sm">
                + Add Contact
              </button>
            </div>
            <table className="crm-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Job Title</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Primary</th>
                </tr>
              </thead>
              <tbody>
                {contacts.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center' }}>
                      No contacts found
                    </td>
                  </tr>
                ) : (
                  contacts.map((contact) => (
                    <tr key={contact.contactId} onClick={() => navigate(`/crm/contacts/${contact.contactId}`)} style={{ cursor: 'pointer' }}>
                      <td>{contact.firstName} {contact.lastName}</td>
                      <td>{contact.jobTitle || '-'}</td>
                      <td>{contact.email || '-'}</td>
                      <td>{contact.phone || '-'}</td>
                      <td>{contact.isPrimary ? '✓' : ''}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div>
          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
            <h3>Billing Address</h3>
            <div style={{ marginTop: '1rem', fontSize: '0.875rem' }}>
              {account.billingStreet && <div>{account.billingStreet}</div>}
              {account.billingCity && <div>{account.billingCity}, {account.billingState} {account.billingPostalCode}</div>}
              {account.billingCountry && <div>{account.billingCountry}</div>}
              {!account.billingStreet && !account.billingCity && <div style={{ color: '#999' }}>No address provided</div>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AccountDetail;













