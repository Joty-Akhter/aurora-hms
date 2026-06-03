import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { 
  createEmployee, 
  updateEmployee, 
  getEmployeeById, 
  getDepartments, 
  getPositions,
  getEmployees,
  Employee,
  Department,
  Position
} from '../../services/hrService';
import appDataService, { OrganizationAppData } from '../../services/appDataService';
import { COUNTRY_OPTIONS } from '../../constants/countries';
import { genderLabel } from '../../constants/gender';
import userService from '../../services/userService';
import { User } from '../../types';
import './Hr.css';

const DEFAULT_GENDER_OPTIONS: OrganizationAppData[] = [
  { id: 'default-male', organizationId: '', type: 'GENDER', code: 'MALE', name: 'Male' },
  { id: 'default-female', organizationId: '', type: 'GENDER', code: 'FEMALE', name: 'Female' },
  { id: 'default-other', organizationId: '', type: 'GENDER', code: 'OTHER', name: 'Others' },
];

function withLegacyGenderOption(options: OrganizationAppData[], employeeGender?: string): OrganizationAppData[] {
  if (!employeeGender || options.some((g) => g.code === employeeGender)) {
    return options;
  }
  return [
    ...options,
    {
      id: `legacy-${employeeGender}`,
      organizationId: '',
      type: 'GENDER',
      code: employeeGender,
      name: genderLabel(employeeGender),
    },
  ];
}

const EmployeeForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [initializing, setInitializing] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [managers, setManagers] = useState<Employee[]>([]);
  const [availableUsers, setAvailableUsers] = useState<User[]>([]);
  const [genders, setGenders] = useState<OrganizationAppData[]>(DEFAULT_GENDER_OPTIONS);
  
  const [formData, setFormData] = useState<Partial<Employee>>({
    organizationId: currentOrganizationId || '',
    employeeNumber: '',
    name: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    gender: '',
    hireDate: new Date().toISOString().split('T')[0],
    departmentId: '',
    positionId: '',
    managerId: '',
    employmentType: 'FULL_TIME',
    employmentStatus: 'ACTIVE',
    addressLine1: '',
    addressLine2: '',
    city: '',
    stateProvince: '',
    postalCode: '',
    country: id ? '' : 'Bangladesh',
    emergencyContactName: '',
    emergencyContactPhone: '',
    emergencyContactRelationship: '',
    bankName: '',
    bankBranch: '',
    bankAccountNumber: '',
    bankRoutingOrIban: '',
    payrollOvertimeRateMultiplier: undefined,
    payrollStandardHoursPerDay: undefined,
    userId: '',
  });

  useEffect(() => {
    if (!currentOrganizationId) {
      setInitializing(false);
      return;
    }

    const initialize = async () => {
      try {
        setInitializing(true);
        let selectedUserId: string | undefined;
        let employeeGender: string | undefined;
        if (id) {
          const loaded = await loadEmployee(id);
          selectedUserId = loaded?.userId;
          employeeGender = loaded?.gender;
        }
        await loadFormData(selectedUserId, employeeGender);
      } catch (err: any) {
        console.error('Failed to initialize employee form:', err);
        setError(err.response?.data?.message || err.response?.data?.error || 'Failed to load employee data');
      } finally {
        setInitializing(false);
      }
    };

    void initialize();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentOrganizationId, id]);

  const loadFormData = async (selectedUserId?: string, employeeGender?: string) => {
    if (!currentOrganizationId) return;

    try {
      const [departmentsRes, positionsRes, employeesRes, usersPage, genderData] = await Promise.all([
        getDepartments(currentOrganizationId, { activeOnly: true }),
        getPositions(currentOrganizationId, { activeOnly: true }),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
        userService.getAllUsers({ page: 0, size: 200 }),
        appDataService.getAppData(currentOrganizationId, 'GENDER').catch(() => [] as OrganizationAppData[]),
      ]);

      setDepartments(departmentsRes.data);
      setPositions(positionsRes.data);
      setManagers(employeesRes.data);
      const genderOptions = withLegacyGenderOption(
        genderData.length > 0 ? genderData : DEFAULT_GENDER_OPTIONS,
        employeeGender
      );
      setGenders(genderOptions);

      const linkedUserIds = new Set(
        (employeesRes.data || [])
          .filter(emp => !!emp.userId)
          .map(emp => emp.userId as string)
      );

      if (selectedUserId) {
        linkedUserIds.delete(selectedUserId);
      }

      let userOptions = (usersPage.content || []).filter(
        user => !linkedUserIds.has(user.id)
      );

      if (selectedUserId && !userOptions.some(user => user.id === selectedUserId)) {
        try {
          const selectedUser = await userService.getUserById(selectedUserId);
          userOptions = [...userOptions, selectedUser];
        } catch (fetchErr) {
          console.warn('Unable to fetch selected user for employee form', fetchErr);
        }
      }

      userOptions.sort((a, b) => {
        const left = (a.firstName || a.username || '').toLowerCase();
        const right = (b.firstName || b.username || '').toLowerCase();
        if (left < right) return -1;
        if (left > right) return 1;
        return 0;
      });

      setAvailableUsers(userOptions);
    } catch (err) {
      console.error('Failed to load form data:', err);
      setGenders(withLegacyGenderOption(DEFAULT_GENDER_OPTIONS, employeeGender));
    }
  };

  const loadEmployee = async (employeeId: string): Promise<{ userId?: string; gender?: string } | undefined> => {
    try {
      const response = await getEmployeeById(employeeId);
      const employee = response.data;
      
      setFormData({
        ...employee,
        dateOfBirth: employee.dateOfBirth || '',
        hireDate: employee.hireDate || '',
        terminationDate: employee.terminationDate || '',
        userId: employee.userId || '',
        country: employee.country || '',
      });
      return { userId: employee.userId || undefined, gender: employee.gender || undefined };
    } catch (err: any) {
      console.error('Failed to load employee:', err);
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to load employee');
    }
    return undefined;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!currentOrganizationId) {
      setError('No organization selected');
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const employeeData: Employee = {
        ...formData,
        userId: formData.userId ? formData.userId : undefined,
        managerId: formData.managerId ? formData.managerId : undefined,
        departmentId: formData.departmentId ? formData.departmentId : undefined,
        positionId: formData.positionId ? formData.positionId : undefined,
        country: formData.country || (id ? '' : 'Bangladesh'),
        organizationId: currentOrganizationId,
      } as Employee;

      if (id) {
        await updateEmployee(id, employeeData);
      } else {
        await createEmployee(employeeData);
      }

      navigate('/hr/employees');
    } catch (err: any) {
      console.error('Failed to save employee:', err);
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to save employee');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>{id ? 'Edit Employee' : 'Add New Employee'}</h1>
        <p>Enter employee information below</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      {initializing ? (
        <div className="loading">Loading employee form…</div>
      ) : (
      <form onSubmit={handleSubmit} className="hr-form">
        {/* Basic Information */}
        <div className="form-section">
          <h2>Basic Information</h2>
          <div className="form-grid">
            <div className="form-group">
              <label>Employee Number *</label>
              <input
                type="text"
                name="employeeNumber"
                value={formData.employeeNumber}
                onChange={handleChange}
                required
                disabled={!!id}
                placeholder="EMP001"
              />
            </div>

            <div className="form-group">
              <label>Name *</label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
                placeholder="John Doe"
              />
            </div>

            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="employee@example.com"
              />
            </div>

            <div className="form-group">
              <label>Linked User Account</label>
              <select
                name="userId"
                value={formData.userId || ''}
                onChange={handleChange}
              >
                <option value="">No linked user</option>
                {availableUsers.map(user => {
                  const displayName = [user.firstName, user.lastName]
                    .filter(Boolean)
                    .join(' ')
                    .trim();
                  return (
                    <option key={user.id} value={user.id}>
                      {displayName || user.username}
                      {user.email ? ` (${user.email})` : ''}
                    </option>
                  );
                })}
              </select>
            </div>

            <div className="form-group">
              <label>Phone</label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                placeholder="+1 (555) 123-4567"
              />
            </div>

            <div className="form-group">
              <label>Date of Birth</label>
              <input
                type="date"
                name="dateOfBirth"
                value={formData.dateOfBirth}
                onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label>Gender</label>
              <select name="gender" value={formData.gender} onChange={handleChange}>
                <option value="">Select Gender</option>
                {genders.map((g) => (
                  <option key={g.id} value={g.code}>
                    {g.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Employment Information */}
        <div className="form-section">
          <h2>Employment Information</h2>
          <div className="form-grid">
            <div className="form-group">
              <label>Hire Date *</label>
              <input
                type="date"
                name="hireDate"
                value={formData.hireDate}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label>Department</label>
              <select name="departmentId" value={formData.departmentId} onChange={handleChange}>
                <option value="">Select Department</option>
                {departments.map(dept => (
                  <option key={dept.departmentId} value={dept.departmentId}>
                    {dept.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Position</label>
              <select name="positionId" value={formData.positionId} onChange={handleChange}>
                <option value="">Select Position</option>
                {positions.map(pos => (
                  <option key={pos.positionId} value={pos.positionId}>
                    {pos.title}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Manager</label>
              <select name="managerId" value={formData.managerId} onChange={handleChange}>
                <option value="">Select Manager</option>
                {managers.map(mgr => (
                  <option key={mgr.employeeId} value={mgr.employeeId}>
                    {mgr.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Employment Type *</label>
              <select 
                name="employmentType" 
                value={formData.employmentType} 
                onChange={handleChange}
                required
              >
                <option value="FULL_TIME">Full-Time</option>
                <option value="PART_TIME">Part-Time</option>
                <option value="CONTRACT">Contract</option>
                <option value="INTERN">Intern</option>
              </select>
            </div>

            <div className="form-group">
              <label>Employment Status *</label>
              <select 
                name="employmentStatus" 
                value={formData.employmentStatus} 
                onChange={handleChange}
                required
              >
                <option value="ACTIVE">Active</option>
                <option value="ON_LEAVE">On Leave</option>
                <option value="TERMINATED">Terminated</option>
              </select>
            </div>

            {formData.employmentStatus === 'TERMINATED' && (
              <div className="form-group">
                <label>Termination Date</label>
                <input
                  type="date"
                  name="terminationDate"
                  value={formData.terminationDate}
                  onChange={handleChange}
                />
              </div>
            )}
          </div>
        </div>

        {/* Address Information */}
        <div className="form-section">
          <h2>Address Information</h2>
          <div className="form-grid">
            <div className="form-group form-group-full">
              <label>Address Line 1</label>
              <input
                type="text"
                name="addressLine1"
                value={formData.addressLine1}
                onChange={handleChange}
                autoComplete="off"
              />
            </div>

            <div className="form-group form-group-full">
              <label>Address Line 2</label>
              <input
                type="text"
                name="addressLine2"
                value={formData.addressLine2}
                onChange={handleChange}
                autoComplete="off"
              />
            </div>

            <div className="form-group">
              <label>City</label>
              <input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleChange}
                autoComplete="off"
              />
            </div>

            <div className="form-group">
              <label>State/Province</label>
              <input
                type="text"
                name="stateProvince"
                value={formData.stateProvince}
                onChange={handleChange}
                autoComplete="off"
              />
            </div>

            <div className="form-group">
              <label>Postal Code</label>
              <input
                type="text"
                name="postalCode"
                value={formData.postalCode}
                onChange={handleChange}
                autoComplete="off"
              />
            </div>

            <div className="form-group">
              <label>Country</label>
              <select
                name="country"
                value={id ? (formData.country || '') : (formData.country || 'Bangladesh')}
                onChange={handleChange}
              >
                {id && <option value="">Select country</option>}
                {id && formData.country && !COUNTRY_OPTIONS.includes(formData.country) && (
                  <option value={formData.country}>{formData.country}</option>
                )}
                {COUNTRY_OPTIONS.map((country) => (
                  <option key={country} value={country}>
                    {country}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Bank & payroll overrides (Phase C HR-MD-04 / HR-AT-05) */}
        <div className="form-section">
          <h2>Bank &amp; payroll overrides</h2>
          <p style={{ color: '#666', marginTop: 0 }}>
            Bank fields follow HR-MD-04; protect stored values per organization policy. OT overrides apply only when set —
            otherwise roster shift band or org time policy is used.
          </p>
          <div className="form-grid">
            <div className="form-group">
              <label>Bank name</label>
              <input type="text" name="bankName" value={formData.bankName ?? ''} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label>Branch</label>
              <input type="text" name="bankBranch" value={formData.bankBranch ?? ''} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label>Account number</label>
              <input type="text" name="bankAccountNumber" value={formData.bankAccountNumber ?? ''} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label>Routing / IBAN</label>
              <input type="text" name="bankRoutingOrIban" value={formData.bankRoutingOrIban ?? ''} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label>Payroll OT multiplier override</label>
              <input
                type="number"
                step="0.01"
                min="0"
                name="payrollOvertimeRateMultiplier"
                value={
                  formData.payrollOvertimeRateMultiplier === undefined || formData.payrollOvertimeRateMultiplier === null
                    ? ''
                    : String(formData.payrollOvertimeRateMultiplier)
                }
                onChange={(e) => {
                  const v = e.target.value;
                  setFormData((prev) => ({
                    ...prev,
                    payrollOvertimeRateMultiplier: v === '' ? undefined : parseFloat(v),
                  }));
                }}
                placeholder="Leave blank for policy / roster"
              />
            </div>
            <div className="form-group">
              <label>Standard hours / day (OT divisor)</label>
              <input
                type="number"
                step="0.25"
                min="0"
                name="payrollStandardHoursPerDay"
                value={
                  formData.payrollStandardHoursPerDay === undefined || formData.payrollStandardHoursPerDay === null
                    ? ''
                    : String(formData.payrollStandardHoursPerDay)
                }
                onChange={(e) => {
                  const v = e.target.value;
                  setFormData((prev) => ({
                    ...prev,
                    payrollStandardHoursPerDay: v === '' ? undefined : parseFloat(v),
                  }));
                }}
                placeholder="Leave blank for shift band / policy"
              />
            </div>
          </div>
        </div>

        {/* Emergency Contact */}
        <div className="form-section">
          <h2>Emergency Contact</h2>
          <div className="form-grid">
            <div className="form-group">
              <label>Emergency Contact Name</label>
              <input
                type="text"
                name="emergencyContactName"
                value={formData.emergencyContactName}
                onChange={handleChange}
                placeholder="Jane Doe"
              />
            </div>

            <div className="form-group">
              <label>Emergency Contact Phone</label>
              <input
                type="tel"
                name="emergencyContactPhone"
                value={formData.emergencyContactPhone}
                onChange={handleChange}
                placeholder="+1 (555) 987-6543"
              />
            </div>

            <div className="form-group">
              <label>Relationship</label>
              <input
                type="text"
                name="emergencyContactRelationship"
                value={formData.emergencyContactRelationship}
                onChange={handleChange}
                placeholder="Spouse, Parent, etc."
              />
            </div>
          </div>
        </div>

        {/* Form Actions */}
        <div className="form-actions">
          <button 
            type="button" 
            className="btn-outline" 
            onClick={() => navigate('/hr/employees')}
            disabled={saving}
          >
            Cancel
          </button>
          <button 
            type="submit" 
            className="btn-primary" 
            disabled={saving}
          >
            {saving ? 'Saving...' : (id ? 'Update Employee' : 'Create Employee')}
          </button>
        </div>
      </form>
      )}
    </div>
  );
};

export default EmployeeForm;

