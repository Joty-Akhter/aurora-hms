import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getLeadDashboardStats, getLeads } from '../../services/crmService';
import './Crm.css';

const LeadDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState<any>(null);
  const [recentLeads, setRecentLeads] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [statsRes, leadsRes] = await Promise.all([
        getLeadDashboardStats(currentOrganizationId),
        getLeads(currentOrganizationId, {}),
      ]);
      setStats(statsRes.data);
      setRecentLeads(leadsRes.data.slice(0, 10));
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading CRM dashboard...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>CRM Dashboard</h1>
        <p>Customer Relationship Management Overview</p>
        <button onClick={() => navigate('/crm/leads/new')} className="btn-primary">
          + New Lead
        </button>
      </div>

      <div className="crm-summary-cards">
        <div className="crm-summary-card">
          <h3>Total Leads</h3>
          <div className="crm-card-value">{stats?.totalLeads || 0}</div>
        </div>
        <div className="crm-summary-card">
          <h3>New Leads</h3>
          <div className="crm-card-value">{stats?.newLeads || 0}</div>
          <small>Uncontacted</small>
        </div>
        <div className="crm-summary-card">
          <h3>Qualified</h3>
          <div className="crm-card-value">{stats?.qualifiedLeads || 0}</div>
          <small>Ready for conversion</small>
        </div>
        <div className="crm-summary-card">
          <h3>Converted</h3>
          <div className="crm-card-value">{stats?.convertedLeads || 0}</div>
          <small>{stats?.conversionRate?.toFixed(1) || 0}% conversion rate</small>
        </div>
      </div>

      <div className="crm-section">
        <h2>Recent Leads</h2>
        <table className="crm-table">
          <thead>
            <tr>
              <th>Lead #</th>
              <th>Name</th>
              <th>Company</th>
              <th>Email</th>
              <th>Status</th>
              <th>Rating</th>
              <th>Score</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {recentLeads.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center' }}>
                  No leads found. Create your first lead to get started.
                </td>
              </tr>
            ) : (
              recentLeads.map((lead) => (
                <tr key={lead.leadId} onClick={() => navigate(`/crm/leads/${lead.leadId}`)} style={{ cursor: 'pointer' }}>
                  <td>{lead.leadNumber}</td>
                  <td>{lead.firstName} {lead.lastName}</td>
                  <td>{lead.company || '-'}</td>
                  <td>{lead.email || '-'}</td>
                  <td>
                    <span className={`status-badge status-${lead.status?.toLowerCase()}`}>
                      {lead.status}
                    </span>
                  </td>
                  <td>
                    {lead.rating && (
                      <span className={`rating-${lead.rating?.toLowerCase()}`}>
                        {lead.rating}
                      </span>
                    )}
                  </td>
                  <td>
                    <span className={`lead-score lead-score-${lead.leadScore >= 70 ? 'high' : lead.leadScore >= 40 ? 'medium' : 'low'}`}>
                      {lead.leadScore || 0}
                    </span>
                  </td>
                  <td>{new Date(lead.createdAt).toLocaleDateString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LeadDashboard;













