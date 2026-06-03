import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getCampaigns } from '../../services/crmService';
import './Crm.css';

type Campaign = {
  campaignId: string;
  campaignNumber?: string;
  campaignName: string;
  campaignType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  budgetedCost?: number;
  actualCost?: number;
  currency?: string;
};

type CampaignSummary = {
  totalCampaigns: number;
  activeCampaigns: number;
  totalBudget: number;
  totalActual: number;
};

const formatCurrency = (amount?: number, currency: string = 'BDT') => {
  if (amount === undefined || amount === null) return '-';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toLocaleString()}`;
  }
};

const CampaignDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [stats, setStats] = useState<CampaignSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setCampaigns([]);
      setStats(null);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const allCampaigns = await getCampaigns(currentOrganizationId);
        const campaignList = Array.isArray(allCampaigns) ? allCampaigns : [];
        const active = campaignList.filter((campaign) => campaign.status === 'ACTIVE');

        setCampaigns(active);
        setStats({
          totalCampaigns: campaignList.length,
          activeCampaigns: active.length,
          totalBudget: campaignList.reduce((sum, c) => sum + (c.budgetedCost ?? 0), 0),
          totalActual: campaignList.reduce((sum, c) => sum + (c.actualCost ?? 0), 0),
        });
      } catch (err) {
        console.error('Failed to load campaign dashboard:', err);
        setError('Failed to load campaign dashboard');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId]);

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Campaign Dashboard</h1>
          <p>Track marketing initiatives, budgets, and performance</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/campaigns/new')}>
            + New Campaign
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      {loading ? (
        <div className="table-loading" style={{ background: 'white', borderRadius: 16 }}>
          <span className="spinner" /> Loading campaigns...
        </div>
      ) : (
        <>
          <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
            <div className="crm-summary-card">
              <h3>Active Campaigns</h3>
              <div className="crm-card-value">{campaigns.length}</div>
              <small>Currently running</small>
            </div>
            <div className="crm-summary-card">
              <h3>Total Campaigns</h3>
              <div className="crm-card-value">{stats?.totalCampaigns ?? campaigns.length}</div>
              <small>Historical campaigns in the system</small>
            </div>
            <div className="crm-summary-card">
              <h3>Budgeted Spend</h3>
              <div className="crm-card-value">{formatCurrency(stats?.totalBudget)}</div>
              <small>Cumulative planned spend</small>
            </div>
            <div className="crm-summary-card">
              <h3>Actual Spend</h3>
              <div className="crm-card-value">{formatCurrency(stats?.totalActual)}</div>
              <small>Recorded campaign costs</small>
            </div>
          </div>

          <div className="crm-section">
            <div className="crm-section-header">
              <div>
                <h2>Active Campaigns</h2>
                <p className="section-subtitle">Monitor in-flight campaigns and compare budget to actuals.</p>
              </div>
              <div className="section-actions">
                <button className="btn-secondary" onClick={() => navigate('/crm/campaigns')}>
                  View Campaign List
                </button>
              </div>
            </div>

            <div className="table-wrapper" style={{ boxShadow: 'none', marginTop: 0 }}>
              {campaigns.length === 0 ? (
                <div className="crm-empty-state">
                  <p>No active campaigns right now.</p>
                  <div className="empty-actions">
                    <button className="btn-primary" onClick={() => navigate('/crm/campaigns/new')}>
                      Launch Campaign
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
                      <th>Start</th>
                      <th>End</th>
                      <th>Budget</th>
                      <th>Actual</th>
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
                          <span className={`status-badge status-${campaign.status?.toLowerCase() || 'planned'}`}>
                            {campaign.status || 'Active'}
                          </span>
                        </td>
                        <td>{campaign.startDate ? new Date(campaign.startDate).toLocaleDateString() : '--'}</td>
                        <td>{campaign.endDate ? new Date(campaign.endDate).toLocaleDateString() : '--'}</td>
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
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default CampaignDashboard;
