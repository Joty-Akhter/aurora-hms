import React, { useState, useEffect } from 'react';
import { getCrmAnalyticsDashboard } from '../../services/crmService';
import './Crm.css';

const CrmReports: React.FC = () => {
  const [analytics, setAnalytics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  
  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  useEffect(() => {
    loadAnalytics();
  }, []);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      const data = await getCrmAnalyticsDashboard(organizationId);
      setAnalytics(data);
    } catch (error) {
      console.error('Error loading analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="crm-loading">Loading analytics...</div>;
  }

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>CRM Analytics & Reports</h1>
      </div>

      <div className="crm-analytics-grid">
        {/* Leads Section */}
        <div className="crm-analytics-section">
          <h2>Lead Analytics</h2>
          <div className="crm-stats-grid">
            <div className="crm-stat-card">
              <h3>Total Leads</h3>
              <p className="crm-stat-value">{analytics?.totalLeads || 0}</p>
            </div>
            <div className="crm-stat-card">
              <h3>New Leads</h3>
              <p className="crm-stat-value">{analytics?.newLeads || 0}</p>
            </div>
            <div className="crm-stat-card success">
              <h3>Converted</h3>
              <p className="crm-stat-value">{analytics?.convertedLeads || 0}</p>
            </div>
            <div className="crm-stat-card info">
              <h3>Conversion Rate</h3>
              <p className="crm-stat-value">{analytics?.conversionRate || 0}%</p>
            </div>
          </div>
        </div>

        {/* Opportunities Section */}
        <div className="crm-analytics-section">
          <h2>Opportunity Analytics</h2>
          <div className="crm-stats-grid">
            <div className="crm-stat-card">
              <h3>Open Opportunities</h3>
              <p className="crm-stat-value">{analytics?.openOpportunities || 0}</p>
            </div>
            <div className="crm-stat-card success">
              <h3>Won</h3>
              <p className="crm-stat-value">{analytics?.wonOpportunities || 0}</p>
            </div>
            <div className="crm-stat-card danger">
              <h3>Lost</h3>
              <p className="crm-stat-value">{analytics?.lostOpportunities || 0}</p>
            </div>
            <div className="crm-stat-card info">
              <h3>Win Rate</h3>
              <p className="crm-stat-value">{analytics?.winRate || 0}%</p>
            </div>
          </div>
        </div>

        {/* Support Section */}
        <div className="crm-analytics-section">
          <h2>Support Analytics</h2>
          <div className="crm-stats-grid">
            <div className="crm-stat-card">
              <h3>Active Cases</h3>
              <p className="crm-stat-value">{analytics?.activeCases || 0}</p>
            </div>
            <div className="crm-stat-card">
              <h3>New Cases</h3>
              <p className="crm-stat-value">{analytics?.newCases || 0}</p>
            </div>
            <div className="crm-stat-card success">
              <h3>Resolved</h3>
              <p className="crm-stat-value">{analytics?.resolvedCases || 0}</p>
            </div>
          </div>
        </div>

        {/* General Stats */}
        <div className="crm-analytics-section">
          <h2>General Statistics</h2>
          <div className="crm-stats-grid">
            <div className="crm-stat-card">
              <h3>Total Accounts</h3>
              <p className="crm-stat-value">{analytics?.totalAccounts || 0}</p>
            </div>
            <div className="crm-stat-card">
              <h3>Active Campaigns</h3>
              <p className="crm-stat-value">{analytics?.activeCampaigns || 0}</p>
            </div>
            <div className="crm-stat-card">
              <h3>Open Tasks</h3>
              <p className="crm-stat-value">{analytics?.openTasks || 0}</p>
            </div>
            <div className="crm-stat-card">
              <h3>KB Articles</h3>
              <p className="crm-stat-value">{analytics?.publishedArticles || 0}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CrmReports;

