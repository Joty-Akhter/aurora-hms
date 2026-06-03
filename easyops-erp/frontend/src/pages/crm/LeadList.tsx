import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getLeads, deleteLead, qualifyLead, Lead } from '../../services/crmService';
import { Refresh as RefreshIcon, Clear as ClearIcon } from '@mui/icons-material';
import './Crm.css';

const statusQuickFilters: Array<{ label: string; value: string }> = [
  { label: 'All Leads', value: '' },
  { label: 'New', value: 'NEW' },
  { label: 'Contacted', value: 'CONTACTED' },
  { label: 'Qualified', value: 'QUALIFIED' },
  { label: 'Converted', value: 'CONVERTED' },
  { label: 'Lost', value: 'LOST' },
];

const ratingQuickFilters: Array<{ label: string; value: string; emoji: string }> = [
  { label: 'Hot', value: 'HOT', emoji: '🔥' },
  { label: 'Warm', value: 'WARM', emoji: '☀️' },
  { label: 'Cold', value: 'COLD', emoji: '❄️' },
];

const getScoreLevel = (score?: number) => {
  if ((score ?? 0) >= 70) return 'high';
  if ((score ?? 0) >= 40) return 'medium';
  return 'low';
};

const formatDate = (value?: string) => {
  if (!value) return '—';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '—';
  return parsed.toLocaleDateString();
};

const LeadList: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const navigate = useNavigate();
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [ratingFilter, setRatingFilter] = useState('');
  const [lastRefreshed, setLastRefreshed] = useState<Date | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    if (currentOrganizationId) {
      loadLeads();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadLeads = async (overrides: Partial<{ statusFilter: string; searchTerm: string }> = {}) => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await getLeads(currentOrganizationId, {
        status: (overrides.statusFilter ?? statusFilter) || undefined,
        search: (overrides.searchTerm ?? searchTerm) || undefined,
      });
      setLeads(response.data);
      setLastRefreshed(new Date());
      setError(null);
    } catch (err) {
      console.error('Failed to load leads:', err);
      setError('Failed to load leads');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await loadLeads();
    setIsRefreshing(false);
  };

  const handleSearch = () => {
    loadLeads();
  };

  const handleSearchInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleSearch();
    }
  };

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value);
    loadLeads({ statusFilter: value });
  };

  const handleRatingFilterChange = (value: string) => {
    setRatingFilter((prev) => (prev === value ? '' : value));
  };

  const handleResetFilters = () => {
    if (!statusFilter && !ratingFilter && !searchTerm) return;
    setStatusFilter('');
    setRatingFilter('');
    setSearchTerm('');
    loadLeads({ statusFilter: '', searchTerm: '' });
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this lead?')) return;
    try {
      await deleteLead(id);
      loadLeads();
    } catch (err) {
      alert('Failed to delete lead');
    }
  };

  const handleQualify = async (id: string) => {
    if (!user?.id) return;
    try {
      await qualifyLead(id, user.id);
      loadLeads();
      alert('Lead qualified successfully!');
    } catch (err) {
      alert('Failed to qualify lead');
    }
  };

  const leadMetrics = useMemo(() => {
    const total = leads.length;
    const hot = leads.filter((lead) => lead.rating === 'HOT').length;
    const contacted = leads.filter((lead) => lead.status === 'CONTACTED').length;
    const converted = leads.filter((lead) => lead.status === 'CONVERTED').length;
    const averageScore = total
      ? Math.round(
          leads.reduce((sum, lead) => sum + (lead.leadScore || 0), 0) / total,
        )
      : 0;
    const conversionRate = total ? Math.round((converted / total) * 100) : 0;
    return { total, hot, contacted, converted, averageScore, conversionRate };
  }, [leads]);

  const statusCounts = useMemo(() => {
    return leads.reduce<Record<string, number>>((acc, lead) => {
      if (lead.status) {
        acc[lead.status] = (acc[lead.status] || 0) + 1;
      }
      return acc;
    }, {});
  }, [leads]);

  const ratingCounts = useMemo(() => {
    return leads.reduce<Record<string, number>>((acc, lead) => {
      if (lead.rating) {
        acc[lead.rating] = (acc[lead.rating] || 0) + 1;
      }
      return acc;
    }, {});
  }, [leads]);

  const displayedLeads = useMemo(() => {
    if (!ratingFilter) {
      return leads;
    }
    return leads.filter((lead) => lead.rating === ratingFilter);
  }, [leads, ratingFilter]);

  const emptyStateMessage =
    searchTerm || statusFilter || ratingFilter
      ? 'No leads match your current filters. Adjust the filters or clear them to see more leads.'
      : 'You have not captured any leads yet. Start by importing data or creating your first lead.';

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Leads</h1>
          <p>Manage, qualify, and convert prospects across your funnel.</p>
        </div>
        <div className="header-actions">
          <button
            className="btn-icon"
            onClick={handleRefresh}
            disabled={loading && leads.length === 0}
            title="Refresh leads"
            aria-label="Refresh leads"
          >
            <RefreshIcon className={isRefreshing ? 'icon-rotate' : ''} fontSize="small" />
          </button>
          <button onClick={() => navigate('/crm/leads/new')} className="btn-primary">
            + New Lead
          </button>
        </div>
      </div>

      <div className="crm-summary-cards">
        <div className="crm-summary-card">
          <h3>Total Leads</h3>
          <div className="crm-card-value">{leadMetrics.total}</div>
          <span className="metric-delta">
            {lastRefreshed ? `Updated ${lastRefreshed.toLocaleTimeString()}` : 'Synced moments ago'}
          </span>
        </div>
        <div className="crm-summary-card accent-teal">
          <h3>Hot Leads</h3>
          <div className="crm-card-value">{leadMetrics.hot}</div>
          <span className="metric-delta positive">
            {leadMetrics.total ? Math.round((leadMetrics.hot / leadMetrics.total) * 100) : 0}% of pipeline
          </span>
        </div>
        <div className="crm-summary-card accent-gold">
          <h3>Conversion Rate</h3>
          <div className="crm-card-value">{leadMetrics.conversionRate}%</div>
          <span className="metric-delta neutral">{leadMetrics.converted} converted leads</span>
        </div>
        <div className="crm-summary-card accent-slate">
          <h3>Avg. Lead Score</h3>
          <div className="crm-card-value">{leadMetrics.averageScore}</div>
          <span className="metric-delta">
            {leadMetrics.contacted} contacted • {leadMetrics.total - leadMetrics.contacted} awaiting outreach
          </span>
        </div>
      </div>

      <div className="crm-toolbar">
        <div className="filter-chips" role="group" aria-label="Lead status quick filters">
          {statusQuickFilters.map((filter) => (
            <button
              key={filter.value || 'all'}
              onClick={() => handleStatusFilterChange(filter.value)}
              className={`filter-chip ${statusFilter === filter.value ? 'active' : ''}`}
            >
              {filter.label}
              <span className="chip-count">
                {filter.value ? statusCounts[filter.value] || 0 : leadMetrics.total}
              </span>
            </button>
          ))}
        </div>
        <div className="toolbar-actions">
          <button
            className="btn-ghost"
            onClick={handleResetFilters}
            disabled={!statusFilter && !ratingFilter && !searchTerm}
          >
            <ClearIcon fontSize="small" />
            Clear filters
          </button>
        </div>
      </div>

      <div className="crm-section">
        <div className="crm-section-header">
          <div>
            <h2>Filter &amp; Search</h2>
            <p className="section-subtitle">Combine filters to focus on the leads that need attention right now.</p>
          </div>
          <div className="section-actions">
            <button className="btn-secondary btn-sm" onClick={handleSearch}>
              Apply filters
            </button>
          </div>
        </div>

        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="lead-search">Search</label>
            <input
              id="lead-search"
              type="text"
              placeholder="Search by name, email, company..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={handleSearchInputKeyDown}
            />
          </div>
          <div className="form-row">
            <label htmlFor="lead-status">Status</label>
            <select
              id="lead-status"
              value={statusFilter}
              onChange={(e) => handleStatusFilterChange(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="NEW">New</option>
              <option value="CONTACTED">Contacted</option>
              <option value="QUALIFIED">Qualified</option>
              <option value="UNQUALIFIED">Unqualified</option>
              <option value="CONVERTED">Converted</option>
              <option value="LOST">Lost</option>
            </select>
          </div>
          <div className="form-row">
            <label>Rating</label>
            <div className="filter-chips subtle" role="group" aria-label="Lead rating quick filters">
              {ratingQuickFilters.map((filter) => (
                <button
                  key={filter.value}
                  onClick={() => handleRatingFilterChange(filter.value)}
                  className={`filter-chip light ${ratingFilter === filter.value ? 'active' : ''}`}
                >
                  <span className="chip-emoji" aria-hidden="true">
                    {filter.emoji}
                  </span>
                  {filter.label}
                  <span className="chip-count">{ratingCounts[filter.value] || 0}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert error">
          {error}
        </div>
      )}

      <div className="table-wrapper">
        <table className="crm-table">
          <thead>
            <tr>
              <th>Lead #</th>
              <th>Name</th>
              <th>Company</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Rating</th>
              <th>Score</th>
              <th>Source</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={11} className="table-loading">
                  <span className="spinner" aria-hidden="true"></span>
                  Loading leads...
                </td>
              </tr>
            ) : displayedLeads.length === 0 ? (
              <tr>
                <td colSpan={11}>
                  <div className="empty-state">
                    <h3>No leads to display</h3>
                    <p>{emptyStateMessage}</p>
                    <div className="empty-actions">
                      <button className="btn-primary" onClick={() => navigate('/crm/leads/new')}>
                        Create a lead
                      </button>
                      <button className="btn-secondary" onClick={handleResetFilters}>
                        View all leads
                      </button>
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              displayedLeads.map((lead) => (
                <tr key={lead.leadId}>
                  <td>{lead.leadNumber || '—'}</td>
                  <td>
                    <div className="lead-name">
                      <span className="primary">{[lead.firstName, lead.lastName].filter(Boolean).join(' ') || '—'}</span>
                      {lead.jobTitle && <span className="secondary">{lead.jobTitle}</span>}
                    </div>
                  </td>
                  <td>{lead.company || '—'}</td>
                  <td>{lead.email || '—'}</td>
                  <td>{lead.phone || lead.mobile || '—'}</td>
                  <td>
                    {lead.status ? (
                      <span className={`status-badge status-${lead.status.toLowerCase()}`}>
                        {lead.status}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    {lead.rating ? (
                      <span className={`rating-${lead.rating.toLowerCase()}`}>
                        {lead.rating}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    <span className={`score-pill ${getScoreLevel(lead.leadScore)}`}>
                      {lead.leadScore ?? 0}
                    </span>
                  </td>
                  <td>{lead.leadSourceId || '—'}</td>
                  <td>{formatDate(lead.createdAt)}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        onClick={() => navigate(`/crm/leads/${lead.leadId}`)}
                        className="btn-sm btn-primary"
                      >
                        View
                      </button>
                      {lead.status === 'CONTACTED' && (
                        <button
                          onClick={() => handleQualify(lead.leadId!)}
                          className="btn-sm btn-qualify"
                        >
                          Qualify
                        </button>
                      )}
                      <button
                        onClick={() => handleDelete(lead.leadId!)}
                        className="btn-sm btn-disqualify"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LeadList;







