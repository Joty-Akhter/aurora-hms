import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getLeaveBalances, getLeaveTypes, getEmployees, getMyEmployeeProfile } from '../../services/hrService';
import './Hr.css';

type LeaveBalanceRecord = {
  balanceId: string;
  leaveTypeId: string;
  allocatedDays: number;
  carriedForwardDays: number;
  usedDays: number;
  totalDays: number;
  remainingDays: number;
  leaveTypeName: string;
  isPaid?: boolean;
  allowCarryForward?: boolean;
  maxDaysPerYear?: number;
  description?: string;
};

const LeaveBalance: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [employees, setEmployees] = useState<any[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('');
  const [balances, setBalances] = useState<LeaveBalanceRecord[]>([]);
  const [leaveTypes, setLeaveTypes] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setLoading(false);
      setError('No organization selected');
      return;
    }

    initializeData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentOrganizationId, user?.id]);

  const initializeData = async () => {
    try {
      setLoading(true);
      setError(null);

      const leaveTypesRes = await getLeaveTypes(currentOrganizationId!);

      let employeeList: any[] = [];
      try {
        const employeesRes = await getEmployees(currentOrganizationId!, { status: 'ACTIVE' });
        employeeList = employeesRes.data || [];
      } catch {
        try {
          const meRes = await getMyEmployeeProfile(currentOrganizationId!);
          employeeList = meRes.data ? [meRes.data] : [];
        } catch {
          employeeList = [];
        }
      }
      const types = leaveTypesRes.data || [];

      setEmployees(employeeList);
      setLeaveTypes(types);

      if (employeeList.length === 0) {
        setBalances([]);
        setError('No employees found for this organization. Add an employee to track leave balances.');
        return;
      }

      const employeeForUser = employeeList.find((emp: any) => emp.userId === user?.id);
      const defaultEmployee = employeeForUser || employeeList[0];
      const defaultEmpId = defaultEmployee?.employeeId ?? defaultEmployee?.id;
      if (!defaultEmpId) {
        setBalances([]);
        setError('Could not resolve employee id for the selected record.');
        return;
      }

      setSelectedEmployeeId(defaultEmpId);
      await loadBalancesForEmployee(defaultEmpId, types);
    } catch (err) {
      console.error('Failed to load leave balances:', err);
      setError('Failed to load leave balances');
    } finally {
      setLoading(false);
    }
  };

  const numberFrom = (value: any) => {
    if (value === null || value === undefined) return 0;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  };

  const enrichBalances = (rawBalances: any[], types: any[]): LeaveBalanceRecord[] => {
    const typeMap = new Map(types.map((t: any) => [t.leaveTypeId, t]));

    return rawBalances.map((balance: any) => {
      const type = typeMap.get(balance.leaveTypeId);
      const allocated = numberFrom(balance.allocatedDays);
      const carryForward = numberFrom(balance.carriedForwardDays);
      const used = numberFrom(balance.usedDays);
      const total = allocated + carryForward;
      const remaining = Math.max(total - used, 0);

      return {
        balanceId: balance.balanceId,
        leaveTypeId: balance.leaveTypeId,
        allocatedDays: allocated,
        carriedForwardDays: carryForward,
        usedDays: used,
        totalDays: total,
        remainingDays: remaining,
        leaveTypeName: type?.typeName || 'Leave',
        isPaid: type?.isPaid,
        allowCarryForward: type?.carryForward,
        maxDaysPerYear: type?.maxDaysPerYear,
        description: type?.description,
      };
    });
  };

  const loadBalancesForEmployee = async (employeeId: string, typesSnapshot?: any[]) => {
    try {
      setLoading(true);
      setError(null);

      const response = await getLeaveBalances(currentOrganizationId!, employeeId);
      const enriched = enrichBalances(response.data || [], typesSnapshot || leaveTypes);
      setBalances(enriched);
    } catch (err) {
      console.error('Failed to load leave balances:', err);
      setError('Failed to load leave balances');
      setBalances([]);
    } finally {
      setLoading(false);
    }
  };

  const handleEmployeeChange = async (event: React.ChangeEvent<HTMLSelectElement>) => {
    const employeeId = event.target.value;
    setSelectedEmployeeId(employeeId);

    if (employeeId) {
      await loadBalancesForEmployee(employeeId);
    } else {
      setBalances([]);
    }
  };

  const selectedEmployee = useMemo(
    () =>
      employees.find(
        (emp: any) => (emp.employeeId ?? emp.id) === selectedEmployeeId
      ),
    [employees, selectedEmployeeId]
  );

  if (loading) return <div className="loading">Loading leave balances...</div>;
  if (error) return <div className="error-message">{error}</div>;

  const calculateUsedPercentage = (used: number, total: number) => {
    if (!total) return 0;
    return Math.min(100, Math.round((used / total) * 100));
  };

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Leave Balance</h1>
          <p>
            {selectedEmployee
              ? `Leave balances for ${selectedEmployee.name}`
              : 'View leave balances'}
          </p>
        </div>
        {employees.length > 1 && (
          <div className="header-actions">
            <div className="field-group mini">
              <label className="input-label">Employee</label>
              <select
                className="filter-select"
                value={selectedEmployeeId}
                onChange={handleEmployeeChange}
              >
                {employees.map((emp: any) => {
                  const pk = emp.employeeId ?? emp.id;
                  return (
                    <option key={pk} value={pk}>
                      {emp.name}
                    </option>
                  );
                })}
              </select>
            </div>
          </div>
        )}
      </div>

      <div className="hr-summary-cards">
        {balances.map((balance) => {
          const usedPercent = calculateUsedPercentage(balance.usedDays, balance.totalDays);
          return (
            <div key={balance.balanceId} className="hr-summary-card">
              <h3>{balance.leaveTypeName}</h3>
              {balance.description && <p className="card-subtext">{balance.description}</p>}
              <div className="leave-balance-details">
                <div className="balance-stat">
                  <span className="stat-label">Allocated</span>
                  <span className="stat-value">{balance.allocatedDays} days</span>
                </div>
                <div className="balance-stat">
                  <span className="stat-label">Carried Forward</span>
                  <span className="stat-value">{balance.carriedForwardDays} days</span>
                </div>
                <div className="balance-stat">
                  <span className="stat-label">Used</span>
                  <span className="stat-value">{balance.usedDays} days</span>
                </div>
                <div className="balance-stat">
                  <span className="stat-label">Available</span>
                  <span className="stat-value highlight">{balance.remainingDays} days</span>
                </div>
                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${usedPercent}%`,
                      backgroundColor: usedPercent > 80 ? '#f97316' : '#22c55e',
                    }}
                  />
                </div>
                <small>{usedPercent}% used</small>
              </div>
            </div>
          );
        })}
      </div>

      {balances.length === 0 && (
        <div className="hr-section">
          <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
            No leave balances found. Balances are generated after leave entitlements are configured for the
            employee.
          </p>
        </div>
      )}

      <div className="hr-section">
        <h2>Leave Types</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Leave Type</th>
              <th>Annual Quota</th>
              <th>Carry Forward</th>
              <th>Requires Approval</th>
              <th>Paid</th>
            </tr>
          </thead>
          <tbody>
            {leaveTypes.map((type) => (
              <tr key={type.leaveTypeId}>
                <td>
                  <strong>{type.typeName}</strong>
                  {type.description && <div className="muted-text">{type.description}</div>}
                </td>
                <td>{type.maxDaysPerYear ?? '-'} days</td>
                <td>{type.carryForward ? 'Yes' : 'No'}</td>
                <td>{type.requiresApproval ? 'Yes' : 'No'}</td>
                <td>
                  <span className={type.isPaid ? 'status-badge status-active' : 'status-badge status-inactive'}>
                    {type.isPaid ? 'Paid' : 'Unpaid'}
                  </span>
                </td>
              </tr>
            ))}
            {leaveTypes.length === 0 && (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center' }}>
                  No leave types configured
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LeaveBalance;

