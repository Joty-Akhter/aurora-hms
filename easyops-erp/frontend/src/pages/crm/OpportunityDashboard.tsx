import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPipelineStats, getClosingSoon } from '../../services/crmService';
import './Crm.css';

type PipelineStats = {
  totalOpen: number;
  totalWon: number;
  totalLost: number;
  totalOpportunities: number;
  winRate: number;
};

type Opportunity = {
  opportunityId: string;
  opportunityNumber?: string;
  opportunityName: string;
  amount?: number;
  expectedRevenue?: number;
  expectedCloseDate?: string;
  currency?: string;
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

const OpportunityDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [stats, setStats] = useState<PipelineStats | null>(null);
  const [closingSoon, setClosingSoon] = useState<Opportunity[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setStats(null);
      setClosingSoon([]);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [statsResponse, closingSoonResponse] = await Promise.all([
          getPipelineStats(currentOrganizationId),
          getClosingSoon(currentOrganizationId, 30),
        ]);
        setStats(statsResponse ?? null);
        setClosingSoon(Array.isArray(closingSoonResponse) ? closingSoonResponse : []);
      } catch (err) {
        console.error('Failed to load opportunity dashboard:', err);
        setError('Failed to load opportunity dashboard');
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
          <h1>Opportunity Pipeline</h1>
          <p>Track pipeline health, conversion trends, and deals closing this month</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/opportunities/new')}>
            + New Opportunity
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      {loading ? (
        <div className="table-loading" style={{ background: 'white', borderRadius: 16 }}>
          <span className="spinner" /> Loading pipeline insights...
        </div>
      ) : (
        <>
          <div className="crm-summary-cards">
            <div className="crm-summary-card">
              <h3>Open Opportunities</h3>
              <div className="crm-card-value">{stats?.totalOpen ?? 0}</div>
              <small>Active deals in the pipeline</small>
            </div>
            <div className="crm-summary-card">
              <h3>Won Opportunities</h3>
              <div className="crm-card-value">{stats?.totalWon ?? 0}</div>
              <small>Closed won in the current fiscal period</small>
            </div>
            <div className="crm-summary-card">
              <h3>Lost Opportunities</h3>
              <div className="crm-card-value">{stats?.totalLost ?? 0}</div>
              <small>Closed lost or cancelled deals</small>
            </div>
            <div className="crm-summary-card">
              <h3>Win Rate</h3>
              <div className="crm-card-value">{stats?.winRate ?? 0}%</div>
              <small>Conversion rate across all stages</small>
            </div>
          </div>

          <div className="crm-section">
            <div className="crm-section-header">
              <div>
                <h2>Quick Actions</h2>
                <p className="section-subtitle">Stay on top of the pipeline with common shortcuts</p>
              </div>
              <div className="section-actions">
                <button className="btn-secondary" onClick={() => navigate('/crm/opportunities')}>
                  View All Opportunities
                </button>
                <button className="btn-secondary" onClick={() => navigate('/crm/pipeline')}>
                  Pipeline Board
                </button>
                <button className="btn-secondary" onClick={() => navigate('/crm/forecast')}>
                  Sales Forecast
                </button>
              </div>
            </div>
          </div>

          <div className="crm-section">
            <h2>Closing in Next 30 Days</h2>
            {closingSoon.length === 0 ? (
              <p className="crm-empty-state">No opportunities are scheduled to close in the next 30 days.</p>
            ) : (
              <div className="table-wrapper" style={{ boxShadow: 'none', marginTop: 0 }}>
                <table className="crm-table">
                  <thead>
                    <tr>
                      <th>Opportunity</th>
                      <th>Amount</th>
                      <th>Expected Revenue</th>
                      <th>Expected Close</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {closingSoon.map((opportunity) => (
                      <tr key={opportunity.opportunityId}>
                        <td>
                          <div className="crm-opportunity-cell">
                            <strong>{opportunity.opportunityName}</strong>
                            <small>{opportunity.opportunityNumber || '--'}</small>
                          </div>
                        </td>
                        <td>{formatCurrency(opportunity.amount, opportunity.currency)}</td>
                        <td>{formatCurrency(opportunity.expectedRevenue, opportunity.currency)}</td>
                        <td>{formatDate(opportunity.expectedCloseDate)}</td>
                        <td>
                          <button
                            className="btn-sm btn-primary"
                            onClick={() => navigate(`/crm/opportunities/${opportunity.opportunityId}`)}
                          >
                            View
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default OpportunityDashboard;
