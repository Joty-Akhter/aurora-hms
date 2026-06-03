import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getCampaigns, deleteCampaign } from '../../services/crmService';
import './Crm.css';

type Campaign = {
  campaignId: string;
  campaignNumber?: string;
  campaignName: string;
  campaignType?: string;
  status?: string;
  budgetedCost?: number;
  actualCost?: number;
  currency?: string;
};

const statusClass = (status?: string) => {
  const value = status?.toLowerCase();
  switch (value) {
    case 'planning':
      return 'status-planned';
    case 'active':
      return 'status-in-progress';
    case 'completed':
      return 'status-converted';
    case 'cancelled':
      return 'status-unqualified';
    default:
      return 'status-planned';
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

const CampaignList: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setError('No organization selected');
      setCampaigns([]);
      return;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getCampaigns(
          currentOrganizationId,
          statusFilter || undefined,
          typeFilter || undefined,
          searchTerm || undefined
        );
        if (!controller.signal.aborted) {
          setCampaigns(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          console.error('Failed to load campaigns:', err);
          setError('Failed to load campaigns');
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
  }, [currentOrganizationId, statusFilter, typeFilter, searchTerm]);

  const handleDelete = async (campaignId: string) => {
    if (!confirm('Delete this campaign?')) return;
    try {
      await deleteCampaign(campaignId);
      if (currentOrganizationId) {
        const data = await getCampaigns(
          currentOrganizationId,
          statusFilter || undefined,
          typeFilter || undefined,
          searchTerm || undefined
        );
        setCampaigns(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      console.error('Failed to delete campaign:', err);
      alert('Failed to delete campaign');
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Campaigns</h1>
          <p>Plan and track marketing initiatives across the funnel</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/campaigns/new')}>
            + New Campaign
          </button>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="campaign-search">Search</label>
            <input
              id="campaign-search"
              type="text"
              placeholder="Search by name or number"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
          <div className="form-row">
            <label htmlFor="campaign-status">Status</label>
            <select
              id="campaign-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="PLANNING">Planning</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div className="form-row">
            <label htmlFor="campaign-type">Type</label>
            <select
              id="campaign-type"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value)}
            >
              <option value="">All Types</option>
              <option value="EMAIL">Email</option>
              <option value="SOCIAL">Social</option>
              <option value="WEBINAR">Webinar</option>
              <option value="EVENT">Event</option>
            </select>
          </div>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading campaigns...
          </div>
        ) : campaigns.length === 0 ? (
          <div className="crm-empty-state">
            <p>No campaigns match your filters.</p>
            <div className="empty-actions">
              <button className="btn-secondary" onClick={() => setSearchTerm('')}>
                Clear Search
              </button>
              <button className="btn-primary" onClick={() => navigate('/crm/campaigns/new')}>
                Create Campaign
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Campaign</th>
                <th>Type</th>
                <th>Status</th>
                <th>Budget</th>
                <th>Actual Cost</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {campaigns.map((campaign) => (
                <tr key={campaign.campaignId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{campaign.campaignName}</strong>
                      <small>{campaign.campaignNumber || '--'}</small>
                    </div>
                  </td>
                  <td>{campaign.campaignType || '--'}</td>
                  <td>
                    <span className={`status-badge ${statusClass(campaign.status)}`}>
                      {campaign.status || 'Unknown'}
                    </span>
                  </td>
                  <td>{formatCurrency(campaign.budgetedCost, campaign.currency)}</td>
                  <td>{formatCurrency(campaign.actualCost, campaign.currency)}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-sm btn-primary"
                        onClick={() => navigate(`/crm/campaigns/${campaign.campaignId}`)}
                      >
                        View
                      </button>
                      <button
                        className="btn-sm btn-secondary"
                        onClick={() => navigate(`/crm/campaigns/${campaign.campaignId}/edit`)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn-sm btn-disqualify"
                        onClick={() => handleDelete(campaign.campaignId)}
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

export default CampaignList;
