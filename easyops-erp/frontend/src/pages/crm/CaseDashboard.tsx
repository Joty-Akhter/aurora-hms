import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getCases, getCaseStats } from '../../services/crmService';
import './Crm.css';

type CaseRecord = {
  caseId: string;
  caseNumber?: string;
  subject: string;
  status?: string;
  priority?: string;
  createdAt?: string;
};

type CaseStats = {
  totalCases: number;
  newCases: number;
  openCases: number;
  activeCases: number;
  resolvedCases: number;
  closedCases: number;
};

const statusBadge = (status?: string) => {
  const normalized = status?.toLowerCase() ?? 'planned';
  if (normalized === 'resolved') return 'status-converted';
  if (normalized === 'closed') return 'status-unqualified';
  if (normalized === 'active') return 'status-in-progress';
  if (normalized === 'open') return 'status-planned';
  return 'status-planned';
};

const priorityBadge = (priority?: string) => {
  const normalized = priority?.toLowerCase();
  switch (normalized) {
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

const CaseDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [stats, setStats] = useState<CaseStats | null>(null);
  const [recentCases, setRecentCases] = useState<CaseRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setStats(null);
      setRecentCases([]);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [statsResponse, casesResponse] = await Promise.all([
          getCaseStats(currentOrganizationId),
          getCases(currentOrganizationId, undefined, undefined, undefined, undefined),
        ]);
        setStats(statsResponse ?? null);
        setRecentCases(Array.isArray(casesResponse) ? casesResponse.slice(0, 10) : []);
      } catch (err) {
        console.error('Failed to load support dashboard:', err);
        setError('Failed to load support dashboard');
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
          <h1>Support Dashboard</h1>
          <p>Track incoming cases and resolution performance</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/cases/new')}>
            + New Case
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      {loading ? (
        <div className="table-loading" style={{ background: 'white', borderRadius: 16 }}>
          <span className="spinner" /> Loading support metrics...
        </div>
      ) : (
        <>
          <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
            <div className="crm-summary-card">
              <h3>Active Cases</h3>
              <div className="crm-card-value">{stats?.activeCases ?? 0}</div>
              <small>Currently assigned</small>
            </div>
            <div className="crm-summary-card">
              <h3>New This Week</h3>
              <div className="crm-card-value">{stats?.newCases ?? 0}</div>
              <small>Inbound requests</small>
            </div>
            <div className="crm-summary-card">
              <h3>Resolved</h3>
              <div className="crm-card-value">{stats?.resolvedCases ?? 0}</div>
              <small>Marked resolved</small>
            </div>
            <div className="crm-summary-card">
              <h3>Closed</h3>
              <div className="crm-card-value">{stats?.closedCases ?? 0}</div>
              <small>Completed and closed</small>
            </div>
          </div>

          <div className="crm-section">
            <div className="crm-section-header">
              <div>
                <h2>Recent Cases</h2>
                <p className="section-subtitle">Latest 10 cases sorted by creation date.</p>
              </div>
              <div className="section-actions">
                <button className="btn-secondary" onClick={() => navigate('/crm/cases')}>
                  View All Cases
                </button>
              </div>
            </div>
            <div className="table-wrapper" style={{ boxShadow: 'none', marginTop: 0 }}>
              {recentCases.length === 0 ? (
                <div className="crm-empty-state">
                  <p>No recent cases found.</p>
                  <div className="empty-actions">
                    <button className="btn-primary" onClick={() => navigate('/crm/cases/new')}>
                      Create Support Case
                    </button>
                  </div>
                </div>
              ) : (
                <table className="crm-table">
                  <thead>
                    <tr>
                      <th>Case</th>
                      <th>Status</th>
                      <th>Priority</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentCases.map((caseItem) => (
                      <tr key={caseItem.caseId}>
                        <td>
                          <div className="crm-opportunity-cell">
                            <strong>{caseItem.subject}</strong>
                            <small>{caseItem.caseNumber || '--'}</small>
                          </div>
                        </td>
                        <td>
                          <span className={`status-badge ${statusBadge(caseItem.status)}`}>
                            {caseItem.status || 'Open'}
                          </span>
                        </td>
                        <td>
                          <span className={`priority-badge ${priorityBadge(caseItem.priority)}`}>
                            {caseItem.priority || 'LOW'}
                          </span>
                        </td>
                        <td>
                          {caseItem.createdAt
                            ? new Date(caseItem.createdAt).toLocaleDateString()
                            : '--'}
                        </td>
                        <td>
                          <div className="action-buttons">
                            <button
                              className="btn-sm btn-primary"
                              onClick={() => navigate(`/crm/cases/${caseItem.caseId}`)}
                            >
                              View
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

export default CaseDashboard;
