import React, { useState, useEffect, FormEvent } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getTimesheets, createTimesheet, submitTimesheet, getEmployees, Employee } from '../../services/hrService';
import './Hr.css';

const TimesheetManager: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [timesheets, setTimesheets] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [currentEmployee, setCurrentEmployee] = useState<Employee | null>(null);
  const [employeeLoadError, setEmployeeLoadError] = useState<string | null>(null);
  const [formWeekStart, setFormWeekStart] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    if (!currentOrganizationId) {
      setTimesheets([]);
      setCurrentEmployee(null);
      setEmployeeLoadError(null);
      setError('No organization selected');
      setLoading(false);
      return;
    }

    if (!user?.id) {
      setTimesheets([]);
      setCurrentEmployee(null);
      setEmployeeLoadError('You must be logged in to manage timesheets.');
      setLoading(false);
      return;
    }

    const initialize = async () => {
      setLoading(true);
      setError(null);

      try {
        const employeesResponse = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
        const employeeMatch = employeesResponse.data.find((employee) => employee.userId === user.id);

        if (!employeeMatch) {
          setCurrentEmployee(null);
          setEmployeeLoadError('No employee profile is linked to this user. Contact your administrator to be added to HR.');
          setTimesheets([]);
          return;
        }

        setCurrentEmployee(employeeMatch);
        setEmployeeLoadError(null);
        await loadTimesheets(currentOrganizationId, employeeMatch.employeeId!);
      } catch (err) {
        console.error('Failed to initialize timesheets:', err);
        setError('Failed to load timesheets.');
      } finally {
        setLoading(false);
      }
    };

    void initialize();
  }, [currentOrganizationId, user?.id]);

  const loadTimesheets = async (organizationId: string, employeeId: string) => {
    try {
      const response = await getTimesheets(organizationId, { employeeId });
      setTimesheets(response.data);
    } catch (err) {
      console.error('Failed to load timesheets:', err);
      setError('Failed to load timesheets');
    }
  };

  const handleSubmit = async (timesheetId: string) => {
    if (!currentOrganizationId || !currentEmployee?.employeeId) return;
    try {
      setLoading(true);
      await submitTimesheet(timesheetId);
      await loadTimesheets(currentOrganizationId, currentEmployee.employeeId);
      alert('Timesheet submitted successfully!');
    } catch (err) {
      console.error('Failed to submit timesheet:', err);
      alert('Failed to submit timesheet');
    } finally {
      setLoading(false);
    }
  };

  const parseDate = (value: string) => {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  };

  const formatDate = (date: Date) => {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const getWeekEnd = (weekStart: string) => {
    if (!weekStart) return null;
    const start = parseDate(weekStart);
    const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6);
    return formatDate(end);
  };

  const handleCreateTimesheet = async (event: FormEvent) => {
    event.preventDefault();

    if (!currentOrganizationId || !currentEmployee?.employeeId) {
      setFormError('Employee context is missing. Please contact your administrator.');
      return;
    }

    if (!formWeekStart) {
      setFormError('Please select a week start date.');
      return;
    }

    const weekEndDate = getWeekEnd(formWeekStart);

    if (!weekEndDate) {
      setFormError('Unable to calculate week end date. Please try again.');
      return;
    }

    setIsCreating(true);
    setFormError(null);

    try {
      await createTimesheet({
        organizationId: currentOrganizationId,
        employeeId: currentEmployee.employeeId,
        weekStartDate: formWeekStart,
        weekEndDate,
        totalHours: 0,
        regularHours: 0,
        overtimeHours: 0,
      });

      alert('Timesheet created successfully!');
      setShowForm(false);
      setFormWeekStart('');

      await loadTimesheets(currentOrganizationId, currentEmployee.employeeId);
    } catch (err: any) {
      console.error('Failed to create timesheet:', err);
      const duplicateError =
        err?.response?.status === 500 || err?.response?.status === 400
          ? 'A timesheet already exists for the selected week. Please choose a different start date.'
          : 'Failed to create timesheet. Please try again.';
      setFormError(duplicateError);
    } finally {
      setIsCreating(false);
    }
  };

  const handleOpenForm = () => {
    setFormError(null);
    setShowForm(true);
  };

  const handleCancelForm = () => {
    setShowForm(false);
    setFormWeekStart('');
    setFormError(null);
  };

  if (loading) return <div className="loading">Loading timesheets...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Timesheet Management</h1>
        <p>Track and submit your weekly timesheets</p>
        <button
          onClick={handleOpenForm}
          className="btn-primary"
          disabled={!currentEmployee?.employeeId}
          title={!currentEmployee?.employeeId ? 'No employee profile linked to your account.' : undefined}
        >
          + New Timesheet
        </button>
      </div>

      {employeeLoadError && (
        <div className="alert alert-warning" role="alert">
          {employeeLoadError}
        </div>
      )}

      {showForm && currentEmployee?.employeeId && (
        <div className="hr-section">
          <div className="hr-card">
            <h3>Create Timesheet</h3>
            <form onSubmit={handleCreateTimesheet}>
              <div className="form-row">
                <label htmlFor="weekStartDate">Week Starting</label>
                <input
                  id="weekStartDate"
                  type="date"
                  value={formWeekStart}
                  onChange={(event) => setFormWeekStart(event.target.value)}
                  required
                />
              </div>

              <div className="form-row">
                <label>Week Ending</label>
                <div>{getWeekEnd(formWeekStart) ?? '-'}</div>
              </div>

              {formError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>
                  {formError}
                </div>
              )}

              <div className="form-actions">
                <button type="submit" className="btn-primary" disabled={isCreating}>
                  {isCreating ? 'Creating...' : 'Create Timesheet'}
                </button>
                <button type="button" className="btn-secondary" onClick={handleCancelForm} disabled={isCreating}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="hr-section">
        <h2>My Timesheets</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Week</th>
              <th>Period</th>
              <th>Total Hours</th>
              <th>Status</th>
              <th>Submitted</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {timesheets.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center' }}>
                  No timesheets found. Create your first timesheet to get started.
                </td>
              </tr>
            ) : (
              timesheets.map((ts) => (
                <tr key={ts.timesheetId}>
                  <td>Week {ts.weekNumber}</td>
                  <td>
                    {new Date(ts.periodStart).toLocaleDateString()} - {new Date(ts.periodEnd).toLocaleDateString()}
                  </td>
                  <td>{ts.totalHours || 0} hrs</td>
                  <td>
                    <span className={`status-badge status-${ts.status?.toLowerCase()}`}>
                      {ts.status}
                    </span>
                  </td>
                  <td>{ts.submittedAt ? new Date(ts.submittedAt).toLocaleDateString() : '-'}</td>
                  <td>
                    {ts.status === 'DRAFT' && (
                      <button onClick={() => handleSubmit(ts.timesheetId)} className="btn-sm btn-primary">
                        Submit
                      </button>
                    )}
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

export default TimesheetManager;

