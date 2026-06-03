import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getAccounts, deleteAccount, Account } from '../../services/crmService';
import './Crm.css';

const accountTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Customer', value: 'CUSTOMER' },
  { label: 'Prospect', value: 'PROSPECT' },
  { label: 'Partner', value: 'PARTNER' },
  { label: 'Competitor', value: 'COMPETITOR' },
];

const AccountList: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setAccounts([]);
      setError('No organization selected');
      return;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getAccounts(currentOrganizationId, {
          accountType: typeFilter || undefined,
          search: searchTerm || undefined,
        });
        if (!controller.signal.aborted) {
          setAccounts(response.data ?? []);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          console.error('Failed to load accounts:', err);
          setError('Failed to load accounts');
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
  }, [currentOrganizationId, searchTerm, typeFilter]);

  const filteredStats = useMemo(() => {
    return {
      total: accounts.length,
      active: accounts.filter((account) => account.isActive).length,
    };
  }, [accounts]);

  const handleDelete = async (accountId: string) => {
    if (!confirm('Delete this account?')) return;
    try {
      await deleteAccount(accountId);
      if (currentOrganizationId) {
        const response = await getAccounts(currentOrganizationId, {
          accountType: typeFilter || undefined,
          search: searchTerm || undefined,
        });
        setAccounts(response.data ?? []);
      }
    } catch (err) {
      console.error('Failed to delete account:', err);
      alert('Failed to delete account');
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Accounts</h1>
          <p>Manage customers, partners, and companies</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/accounts/new')}>
            + New Account
          </button>
        </div>
      </div>

      <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
        <div className="crm-summary-card">
          <h3>Total Accounts</h3>
          <div className="crm-card-value">{filteredStats.total}</div>
          <small>Across all account types</small>
        </div>
        <div className="crm-summary-card">
          <h3>Active Accounts</h3>
          <div className="crm-card-value">{filteredStats.active}</div>
          <small>Marked as active</small>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="account-search">Search</label>
            <input
              id="account-search"
              type="text"
              placeholder="Search by name, email, city, or industry"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
          <div className="form-row">
            <label htmlFor="account-type">Account Type</label>
            <select
              id="account-type"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value)}
            >
              {accountTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading accounts...
          </div>
        ) : accounts.length === 0 ? (
          <div className="crm-empty-state">
            <p>No accounts match your current filters.</p>
            <div className="empty-actions">
              <button className="btn-secondary" onClick={() => setSearchTerm('')}>
                Clear Search
              </button>
              <button className="btn-primary" onClick={() => navigate('/crm/accounts/new')}>
                Create Account
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Account</th>
                <th>Type</th>
                <th>Industry</th>
                <th>Phone</th>
                <th>Email</th>
                <th>City</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.accountId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{account.accountName}</strong>
                      <small>{account.accountNumber || '--'}</small>
                    </div>
                  </td>
                  <td>{account.accountType || '--'}</td>
                  <td>{account.industry || '--'}</td>
                  <td>{account.phone || '--'}</td>
                  <td>{account.email || '--'}</td>
                  <td>{account.billingCity || '--'}</td>
                  <td>
                    <span className={`status-badge ${account.isActive ? 'status-qualified' : 'status-unqualified'}`}>
                      {account.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-sm btn-primary"
                        onClick={() => navigate(`/crm/accounts/${account.accountId}`)}
                      >
                        View
                      </button>
                      <button
                        className="btn-sm btn-secondary"
                        onClick={() => navigate(`/crm/accounts/${account.accountId}/edit`)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn-sm btn-disqualify"
                        onClick={() => handleDelete(account.accountId!)}
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

export default AccountList;







