import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getCases, deleteCase } from '../../services/crmService';
import './Crm.css';

type CaseRecord = {
  caseId: string;
  caseNumber?: string;
  subject: string;
  status?: string;
  priority?: string;
  assignedTo?: string;
  createdAt?: string;
};

const statusBadge = (status?: string) => {
  const normalized = status?.toLowerCase() ?? 'planned';
  if (normalized === 'resolved') return 'status-converted';
  if (normalized === 'closed') return 'status-unqualified';
  if (normalized === 'in_progress') return 'status-in-progress';
  return 'status-planned';
};

const priorityBadge = (priority?: string) => {
  const normalized = priority?.toLowerCase();
  switch (normalized) {
    case 'critical':
      return 'priority-urgent';
    case 'high':
      return 'priority-high';
    case 'medium':
      return 'priority-medium';
    default:
      return 'priority-low';
  }
};

const CaseList: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [cases, setCases] = useState<CaseRecord[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setCases([]);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getCases(
          currentOrganizationId,
          statusFilter || undefined,
          priorityFilter || undefined,
        );
        setCases(Array.isArray(response) ? response : []);
      } catch (err) {
        console.error('Failed to load cases:', err);
        setError('Failed to load cases');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId, statusFilter, priorityFilter]);

  const handleDelete = async (caseId: string) => {
    if (!confirm('Delete this case?')) return;
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      await deleteCase(caseId);
      const response = await getCases(
        currentOrganizationId,
        statusFilter || undefined,
        priorityFilter || undefined,
      );
      setCases(Array.isArray(response) ? response : []);
    } catch (err) {
      console.error('Failed to delete case:', err);
      setError('Failed to delete case');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Support Cases</h1>
          <p>Manage customer issues and track resolution progress</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/cases/new')}>
            + New Case
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="case-status">Status</label>
            <select
              id="case-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="NEW">New</option>
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
          <div className="form-row">
            <label htmlFor="case-priority">Priority</label>
            <select
              id="case-priority"
              value={priorityFilter}
              onChange={(event) => setPriorityFilter(event.target.value)}
            >
              <option value="">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading cases...
          </div>
        ) : cases.length === 0 ? (
          <div className="crm-empty-state">
            <p>No cases match your filters.</p>
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
              {cases.map((caseRecord) => (
                <tr key={caseRecord.caseId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{caseRecord.subject}</strong>
                      <small>{caseRecord.caseNumber || '--'}</small>
                    </div>
                  </td>
                  <td>
                    <span className={`status-badge ${statusBadge(caseRecord.status)}`}>
                      {caseRecord.status || 'Open'}
                    </span>
                  </td>
                  <td>
                    <span className={`priority-badge ${priorityBadge(caseRecord.priority)}`}>
                      {caseRecord.priority || 'LOW'}
                    </span>
                  </td>
                  <td>
                    {caseRecord.createdAt
                      ? new Date(caseRecord.createdAt).toLocaleDateString()
                      : '--'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-sm btn-primary"
                        onClick={() => navigate(`/crm/cases/${caseRecord.caseId}`)}
                      >
                        View
                      </button>
                      <button
                        className="btn-sm btn-secondary"
                        onClick={() => navigate(`/crm/cases/${caseRecord.caseId}/edit`)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn-sm btn-disqualify"
                        onClick={() => handleDelete(caseRecord.caseId)}
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

export default CaseList;
