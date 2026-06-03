import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getDashboardStats, getRecentHires, getHeadcountByDepartment, HrDashboardStats } from '../../services/hrService';
import './Hr.css';

const HrDashboard: React.FC = () => {
  const { currentOrganizationId, currentOrganizationName } = useAuth();
  const [stats, setStats] = useState<HrDashboardStats | null>(null);
  const [recentHires, setRecentHires] = useState<any[]>([]);
  const [headcount, setHeadcount] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    console.log('HrDashboard - currentOrganizationId:', currentOrganizationId);
    if (currentOrganizationId) {
      loadDashboardData();
    } else {
      setLoading(false);
      setError('No organization selected. Please select an organization from the dropdown.');
    }
  }, [currentOrganizationId]);

  const loadDashboardData = async () => {
    if (!currentOrganizationId) {
      setLoading(false);
      setError('No organization selected');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      console.log('Loading HR dashboard data for org:', currentOrganizationId);

      const [statsRes, hiresRes, headcountRes] = await Promise.all([
        getDashboardStats(currentOrganizationId),
        getRecentHires(currentOrganizationId, 10),
        getHeadcountByDepartment(currentOrganizationId)
      ]);

      console.log('HR dashboard data loaded:', { stats: statsRes.data, hires: hiresRes.data, headcount: headcountRes.data });
      setStats(statsRes.data);
      setRecentHires(hiresRes.data);
      setHeadcount(headcountRes.data);
    } catch (err: any) {
      console.error('Failed to load dashboard data:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading HR dashboard...</div>;
  }

  if (error) {
    return <div className="error-message">{error}</div>;
  }

  if (!stats) {
    return <div className="no-data">No dashboard data available</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <h1>HR Dashboard</h1>
        <p>Overview of human resources metrics and statistics</p>
      </div>

      {/* Summary Cards */}
      <div className="dashboard-cards">
        <div className="summary-card">
          <div className="card-icon">👥</div>
          <div className="card-content">
            <div className="card-value">{stats.total_employees || 0}</div>
            <div className="card-label">Total Employees</div>
          </div>
        </div>

        <div className="summary-card">
          <div className="card-icon">✅</div>
          <div className="card-content">
            <div className="card-value">{stats.active_employees || 0}</div>
            <div className="card-label">Active Employees</div>
          </div>
        </div>

        <div className="summary-card">
          <div className="card-icon">🏢</div>
          <div className="card-content">
            <div className="card-value">{stats.department_count || 0}</div>
            <div className="card-label">Departments</div>
          </div>
        </div>

        <div className="summary-card">
          <div className="card-icon">💼</div>
          <div className="card-content">
            <div className="card-value">{stats.position_count || 0}</div>
            <div className="card-label">Positions</div>
          </div>
        </div>

        <div className="summary-card">
          <div className="card-icon">🆕</div>
          <div className="card-content">
            <div className="card-value">{stats.recent_hires_30d || 0}</div>
            <div className="card-label">New Hires (30d)</div>
          </div>
        </div>

        <div className="summary-card">
          <div className="card-icon">📅</div>
          <div className="card-content">
            <div className="card-value">{stats.avg_tenure_years ? stats.avg_tenure_years.toFixed(1) : '0.0'}</div>
            <div className="card-label">Avg Tenure (Years)</div>
          </div>
        </div>
      </div>

      {/* Employment Type Breakdown */}
      <div className="dashboard-section">
        <h2>Employment Type Breakdown</h2>
        <div className="dashboard-cards">
          <div className="info-card">
            <div className="card-label">Full-Time</div>
            <div className="card-value">{stats.full_time_employees || 0}</div>
          </div>
          <div className="info-card">
            <div className="card-label">Part-Time</div>
            <div className="card-value">{stats.part_time_employees || 0}</div>
          </div>
          <div className="info-card">
            <div className="card-label">Contract</div>
            <div className="card-value">{stats.contract_employees || 0}</div>
          </div>
          <div className="info-card">
            <div className="card-label">Intern</div>
            <div className="card-value">{stats.intern_employees || 0}</div>
          </div>
        </div>
      </div>

      {/* Recent Hires */}
      <div className="dashboard-section">
        <h2>Recent Hires</h2>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee #</th>
                <th>Name</th>
                <th>Email</th>
                <th>Position</th>
                <th>Department</th>
                <th>Hire Date</th>
              </tr>
            </thead>
            <tbody>
              {recentHires.length === 0 ? (
                <tr>
                  <td colSpan={6} className="no-data">No recent hires</td>
                </tr>
              ) : (
                recentHires.map((hire) => (
                  <tr key={hire.employee_id}>
                    <td>{hire.employee_number}</td>
                    <td>{hire.first_name} {hire.last_name}</td>
                    <td>{hire.email}</td>
                    <td>{hire.position_title || '-'}</td>
                    <td>{hire.department_name || '-'}</td>
                    <td>{new Date(hire.hire_date).toLocaleDateString()}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Headcount by Department */}
      <div className="dashboard-section">
        <h2>Headcount by Department</h2>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Department</th>
                <th>Manager</th>
                <th>Total Employees</th>
                <th>Active</th>
                <th>Full-Time</th>
                <th>Part-Time</th>
                <th>Contract</th>
              </tr>
            </thead>
            <tbody>
              {headcount.length === 0 ? (
                <tr>
                  <td colSpan={7} className="no-data">No department data</td>
                </tr>
              ) : (
                headcount.map((dept) => (
                  <tr key={dept.department_id}>
                    <td><strong>{dept.department_name}</strong></td>
                    <td>{dept.manager_name || '-'}</td>
                    <td>{dept.total_employees || 0}</td>
                    <td>{dept.active_employees || 0}</td>
                    <td>{dept.full_time_count || 0}</td>
                    <td>{dept.part_time_count || 0}</td>
                    <td>{dept.contract_count || 0}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default HrDashboard;

