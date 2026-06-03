import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getPayrollRuns, getPayrollStats } from '../../services/hrService';
import './Hr.css';

const PayrollDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState<any>(null);
  const [recentRuns, setRecentRuns] = useState<any[]>([]);
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
      const [statsRes, runsRes] = await Promise.all([
        getPayrollStats(currentOrganizationId),
        getPayrollRuns(currentOrganizationId),
      ]);
      setStats(statsRes.data);
      setRecentRuns((runsRes.data || []).slice(0, 5));
    } catch (err) {
      console.error('Failed to load payroll data:', err);
      setError('Failed to load payroll data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading payroll dashboard...</div>;
  if (error) return <div className="error-message">{error}</div>;

  const goToPayrollRuns = () => navigate('/hr/payroll-runs');
  const goToSalaryManagement = () => navigate('/hr/salary');
  const goToPayrollReports = () => navigate('/hr/payroll-runs'); // placeholder route

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Payroll Dashboard</h1>
        <p>Manage and process employee payroll</p>
      </div>

      <div className="hr-summary-cards">
        <div className="hr-summary-card">
          <h3>Current Month Payroll</h3>
          <div className="hr-card-value">${stats?.currentMonthTotal?.toLocaleString() || '0'}</div>
          <small>{stats?.employeeCount || 0} employees</small>
        </div>
        <div className="hr-summary-card">
          <h3>Pending Runs</h3>
          <div className="hr-card-value">{stats?.pendingRuns || 0}</div>
          <small>Awaiting processing</small>
        </div>
        <div className="hr-summary-card">
          <h3>Processed This Month</h3>
          <div className="hr-card-value">{stats?.processedRuns || 0}</div>
          <small>Completed runs</small>
        </div>
        <div className="hr-summary-card">
          <h3>YTD Payroll</h3>
          <div className="hr-card-value">${stats?.ytdTotal?.toLocaleString() || '0'}</div>
          <small>Year to date</small>
        </div>
      </div>

      <div className="hr-section">
        <h2>Recent Payroll Runs</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Run ID</th>
              <th>Period</th>
              <th>Employees</th>
              <th>Gross Amount</th>
              <th>Net Amount</th>
              <th>Status</th>
              <th>Processed Date</th>
            </tr>
          </thead>
          <tbody>
            {recentRuns.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center' }}>
                  No payroll runs found. Create a new payroll run to get started.
                </td>
              </tr>
            ) : (
              recentRuns.map((run) => (
                <tr key={run.payrollRunId}>
                  <td>#{run.runNumber || run.payrollRunId?.substring(0, 8)}</td>
                  <td>
                    {new Date(run.payPeriodStart || run.periodStart).toLocaleDateString()} -{' '}
                    {new Date(run.payPeriodEnd || run.periodEnd).toLocaleDateString()}
                  </td>
                  <td>{run.employeeCount || 0}</td>
                  <td>
                    {Number(run.totalGrossPay ?? run.totalGross ?? 0).toLocaleString(undefined, {
                      style: 'currency',
                      currency: 'USD',
                    })}
                  </td>
                  <td>
                    {Number(run.totalNetPay ?? run.totalNet ?? 0).toLocaleString(undefined, {
                      style: 'currency',
                      currency: 'USD',
                    })}
                  </td>
                  <td>
                    <span className={`status-badge status-${run.status?.toLowerCase()}`}>
                      {run.status}
                    </span>
                  </td>
                  <td>{run.processedAt ? new Date(run.processedAt).toLocaleDateString() : '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="hr-section">
        <h2>Quick Actions</h2>
        <div className="action-cards">
          <div className="action-card">
            <h3>🔄 Process Payroll</h3>
            <p>Create and process new payroll run</p>
          <button className="btn-primary" onClick={goToPayrollRuns}>
            Go to Payroll Runs
          </button>
          </div>
          <div className="action-card">
            <h3>💰 Manage Salaries</h3>
            <p>Update employee salary structures</p>
          <button className="btn-primary" onClick={goToSalaryManagement}>
            Salary Management
          </button>
          </div>
          <div className="action-card">
            <h3>📊 Reports</h3>
            <p>View payroll reports and analytics</p>
          <button className="btn-primary" onClick={goToPayrollReports}>
            View Reports
          </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PayrollDashboard;

