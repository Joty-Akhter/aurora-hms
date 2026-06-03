import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getOpportunities, deleteOpportunity } from '../../services/crmService';
import './Crm.css';

type Opportunity = {
  opportunityId: string;
  opportunityNumber?: string;
  opportunityName: string;
  stageName?: string;
  status?: string;
  priority?: string;
  amount?: number;
  expectedRevenue?: number;
  expectedCloseDate?: string;
  currency?: string;
};

const statusLabel = (status?: string) => {
  if (!status) return 'Unknown';
  return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
};

const statusClassName = (status?: string) => {
  const value = status?.toLowerCase();
  switch (value) {
    case 'open':
    case 'active':
      return 'status-in-progress';
    case 'won':
      return 'status-converted';
    case 'lost':
      return 'status-lost';
    case 'abandoned':
      return 'status-unqualified';
    default:
      return 'status-planned';
  }
};

const priorityClassName = (priority?: string) => {
  const value = priority?.toLowerCase();
  switch (value) {
    case 'urgent':
      return 'priority-urgent';
    case 'high':
      return 'priority-high';
    case 'medium':
      return 'priority-medium';
    default:
      return 'priority-low';
  }
};

const formatCurrency = (amount?: number, currency?: string) => {
  if (amount === undefined || amount === null) return '-';
  const code = currency || 'BDT';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: code,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${code} ${amount.toLocaleString()}`;
  }
};

const formatDate = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleDateString();
};

const OpportunityList: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setError('No organization selected');
      setOpportunities([]);
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getOpportunities(currentOrganizationId, statusFilter || undefined);
        setOpportunities(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error('Failed to load opportunities:', err);
        setError('Failed to load opportunities');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId, statusFilter]);

  const filteredOpportunities = useMemo(() => {
    if (!searchTerm) return opportunities;
    const term = searchTerm.toLowerCase();
    return opportunities.filter((opp) => {
      return (
        (opp.opportunityName && opp.opportunityName.toLowerCase().includes(term)) ||
        (opp.opportunityNumber && opp.opportunityNumber.toLowerCase().includes(term)) ||
        (opp.stageName && opp.stageName.toLowerCase().includes(term))
      );
    });
  }, [opportunities, searchTerm]);

  const handleDelete = async (opportunityId: string) => {
    if (!confirm('Delete this opportunity?')) return;
    try {
      await deleteOpportunity(opportunityId);
      if (currentOrganizationId) {
        const data = await getOpportunities(currentOrganizationId, statusFilter || undefined);
        setOpportunities(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      console.error('Failed to delete opportunity:', err);
      alert('Failed to delete opportunity');
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Opportunities</h1>
          <p>Monitor deals across every pipeline stage</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/opportunities/new')}>
            + New Opportunity
          </button>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="opportunity-search">Search</label>
            <input
              id="opportunity-search"
              type="text"
              placeholder="Search by name, number or stage"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
          <div className="form-row">
            <label htmlFor="opportunity-status">Status</label>
            <select
              id="opportunity-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open</option>
              <option value="WON">Won</option>
              <option value="LOST">Lost</option>
              <option value="ABANDONED">Abandoned</option>
            </select>
          </div>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading opportunities...
          </div>
        ) : filteredOpportunities.length === 0 ? (
          <div className="crm-empty-state">
            <p>No opportunities match your filters.</p>
            <div className="empty-actions">
              <button className="btn-secondary" onClick={() => setSearchTerm('')}>
                Clear Search
              </button>
              <button className="btn-primary" onClick={() => navigate('/crm/opportunities/new')}>
                Create Opportunity
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Opportunity</th>
                <th>Stage</th>
                <th>Status</th>
                <th>Amount</th>
                <th>Expected Revenue</th>
                <th>Expected Close</th>
                <th>Priority</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredOpportunities.map((opportunity) => (
                <tr key={opportunity.opportunityId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{opportunity.opportunityName}</strong>
                      <small>{opportunity.opportunityNumber || '--'}</small>
                    </div>
                  </td>
                  <td>{opportunity.stageName || '--'}</td>
                  <td>
                    <span className={`status-badge ${statusClassName(opportunity.status)}`}>
                      {statusLabel(opportunity.status)}
                    </span>
                  </td>
                  <td>{formatCurrency(opportunity.amount, opportunity.currency)}</td>
                  <td>{formatCurrency(opportunity.expectedRevenue, opportunity.currency)}</td>
                  <td>{formatDate(opportunity.expectedCloseDate)}</td>
                  <td>
                    {opportunity.priority ? (
                      <span className={`priority-badge ${priorityClassName(opportunity.priority)}`}>
                        {opportunity.priority}
                      </span>
                    ) : (
                      '--'
                    )}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-sm btn-primary"
                        onClick={() => navigate(`/crm/opportunities/${opportunity.opportunityId}`)}
                      >
                        View
                      </button>
                      <button
                        className="btn-sm btn-secondary"
                        onClick={() => navigate(`/crm/opportunities/${opportunity.opportunityId}/edit`)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn-sm btn-disqualify"
                        onClick={() => handleDelete(opportunity.opportunityId)}
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

export default OpportunityList;
