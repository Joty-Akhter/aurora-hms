import React, { useState, useEffect } from 'react';
import { Autocomplete, TextField } from '@mui/material';
import { createFilterOptions } from '@mui/material/Autocomplete';
import { useAuth } from '../../contexts/AuthContext';
import {
  getSalaryStructures,
  getSalaryStructure,
  getStructureSummary,
  getStructureRevisionHistory,
  getEmployeeSalaryDetails,
  getEmployeeAssignment,
  getEmployeeSalaryRevisionHistory,
  createSalaryStructure,
  updateSalaryStructure,
  getGradesByStructure,
  createGrade,
  updateGrade,
  getBandsByGrade,
  createBand,
  updateBand,
  getPositions,
  getEmployees,
  getSalaryComponents,
  getSalaryComponentById,
  getSalaryComponentByCode,
  getComponentUsage,
  createSalaryComponent,
  updateSalaryComponent,
  createEmployeeSalaryDetail,
  updateEmployeeSalaryDetail,
  endEmployeeSalaryDetail,
  createEmployeeSalaryAssignment,
  updateEmployeeSalaryAssignment,
  exportComponentMaster,
  getComponentDependencies,
  bulkImportComponents,
  getPayslip,
  getPayrollRuns,
  getPositionSalaryDefaults,
  createSalaryBulkRevision,
  listSalaryBulkRevisions,
  approveSalaryBulkRevision,
  rejectSalaryBulkRevision,
  getGradeHeadcountReport,
  Employee,
} from '../../services/hrService';
import { portalLayoutOverlay, LAYOUT_OVERLAY_DETECT_CLASS } from '@/utils/layoutOverlayPortal';
import './Hr.css';

/** Resolve id from API (camelCase or snake_case JSON). */
function getPayrollRunIdStr(run: any): string {
  const id = run?.payrollRunId ?? run?.payroll_run_id;
  return id != null ? String(id) : '';
}

/** Label for payroll run autocomplete (name + period + status). */
function formatPayrollRunOptionLabel(run: any): string {
  if (!run) return '';
  const name = (run.runName ?? run.run_name ?? '').trim() || 'Unnamed run';
  const start = run.payPeriodStart ?? run.pay_period_start;
  const end = run.payPeriodEnd ?? run.pay_period_end;
  const startStr = start ? String(start).slice(0, 10) : '—';
  const endStr = end ? String(end).slice(0, 10) : '—';
  const status = (run.status || '').toString();
  return `${name} · ${startStr} – ${endStr}${status ? ` · ${status}` : ''}`;
}

const filterPayrollRunOptions = createFilterOptions({
  stringify: (option) => {
    const label = formatPayrollRunOptionLabel(option);
    const id = getPayrollRunIdStr(option);
    return `${label} ${id}`;
  },
});

const SalaryStructureManager: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [structures, setStructures] = useState<any[]>([]);
  const [employeeSalaries, setEmployeeSalaries] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'structures' | 'components' | 'employees'>('structures');
  const [showStructureForm, setShowStructureForm] = useState(false);
  const [editingStructureId, setEditingStructureId] = useState<string | null>(null);
  const [structureFormError, setStructureFormError] = useState<string | null>(null);
  const [positions, setPositions] = useState<any[]>([]);
  const [structureForm, setStructureForm] = useState({
    code: '',
    structureName: '',
    description: '',
    currency: 'BDT',
    payFrequency: 'monthly',
    effectiveFrom: new Date().toISOString().split('T')[0],
    effectiveTo: '',
    isDefault: false,
  });
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [salaryComponents, setSalaryComponents] = useState<any[]>([]);
  const [showAssignForm, setShowAssignForm] = useState(false);
  const [assignFormError, setAssignFormError] = useState<string | null>(null);
  const [assignForm, setAssignForm] = useState({
    employeeId: '',
    salaryStructureId: '',
    componentId: '',
    valueType: 'AMOUNT' as 'AMOUNT' | 'PERCENTAGE' | 'USE_MASTER_DEFAULT',
    amount: '',
    percentage: '',
    effectiveFrom: new Date().toISOString().split('T')[0],
    effectiveTo: '',
  });
  const [editingDetailId, setEditingDetailId] = useState<string | null>(null);
  /** Bulk assign: per-component selected flag; optional amount when master has no default (FIXED/MANUAL) */
  const [bulkComponentAssignments, setBulkComponentAssignments] = useState<
    Record<string, { selected: boolean; amountOverride?: string }>
  >({});
  const [employeeSalaryAsOfDate, setEmployeeSalaryAsOfDate] = useState<string>('');
  // Employee Salary side panel (ES-38, ES-42, ES-45)
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string | null>(null);
  const [selectedEmployeeName, setSelectedEmployeeName] = useState<string>('');
  const [showEmployeePanel, setShowEmployeePanel] = useState(false);
  const [employeePanelTab, setEmployeePanelTab] = useState<'current' | 'history' | 'payslip'>('current');
  const [employeePanelLoading, setEmployeePanelLoading] = useState(false);
  const [employeePanelError, setEmployeePanelError] = useState<string | null>(null);
  const [employeeAssignment, setEmployeeAssignment] = useState<any | null>(null);
  const [employeePanelDetails, setEmployeePanelDetails] = useState<any[]>([]);
  const [employeeRevisionHistory, setEmployeeRevisionHistory] = useState<any[]>([]);
  // Assignment modal (structure + grade + optional band/template)
  const [showAssignmentModal, setShowAssignmentModal] = useState(false);
  const [assignmentFormError, setAssignmentFormError] = useState<string | null>(null);
  const [editingAssignmentId, setEditingAssignmentId] = useState<string | null>(null);
  const [assignmentForm, setAssignmentForm] = useState({
    employeeId: '',
    salaryStructureId: '',
    salaryGradeId: '',
    salaryBandId: '',
    effectiveFrom: new Date().toISOString().split('T')[0],
    effectiveTo: '',
    source: 'OVERRIDE' as 'POSITION' | 'OVERRIDE',
  });
  const [assignmentGrades, setAssignmentGrades] = useState<any[]>([]);
  const [assignmentBands, setAssignmentBands] = useState<any[]>([]);
  const [showComponentForm, setShowComponentForm] = useState(false);
  const [editingComponentId, setEditingComponentId] = useState<string | null>(null);
  const [componentUsage, setComponentUsage] = useState<{ employeeCount: number; referencedInFormulasCount: number } | null>(null);
  const [componentFormError, setComponentFormError] = useState<string | null>(null);
  const [componentFilters, setComponentFilters] = useState({ type: '', category: '', effectiveDate: '', includeInactive: false });
  const [componentForm, setComponentForm] = useState({
    code: '',
    componentName: '',
    description: '',
    componentType: 'EARNING',
    category: '',
    calculationBasis: 'FIXED',
    defaultAmount: '' as string,
    percentageValue: '' as string,
    baseComponentCode: '',
    formulaExpression: '',
    statutoryType: '',
    ceilingAmount: '' as string,
    floorAmount: '' as string,
    isTaxable: true,
    isStatutory: false,
    shortName: '',
    currency: '',
    effectiveFrom: new Date().toISOString().split('T')[0],
    effectiveTo: '',
    displayOrder: 0,
    isActive: true,
    taxability: '',
    statutoryTags: '',
    prorationRule: 'BY_DAYS' as 'BY_DAYS' | 'NO_PRORATION' | 'BY_HOURS',
    expenseAccountCode: '',
    liabilityAccountCode: '',
  });
  // SS-37: Effective date and include inactive filters
  const [effectiveDateFilter, setEffectiveDateFilter] = useState<string>('');
  const [includeInactiveFilter, setIncludeInactiveFilter] = useState(false);
  // SS-36: Structure summary (grades × bands) modal
  const [structureSummary, setStructureSummary] = useState<any | null>(null);
  const [showSummaryModal, setShowSummaryModal] = useState(false);
  // SS-29: Revision history modal
  const [revisionHistory, setRevisionHistory] = useState<any[]>([]);
  const [showRevisionModal, setShowRevisionModal] = useState(false);
  const [revisionStructureName, setRevisionStructureName] = useState('');
  // SC-49: Component dependency report modal
  const [componentDependencies, setComponentDependencies] = useState<any[]>([]);
  const [showDependencyModal, setShowDependencyModal] = useState(false);
  const [dependencyLoading, setDependencyLoading] = useState(false);
  // SC-50: Component bulk import
  const [componentImportResult, setComponentImportResult] = useState<{ createdComponents: number; errors: { row: number; type: string; message: string }[] } | null>(null);
  const [componentImportLoading, setComponentImportLoading] = useState(false);
  const componentFileInputRef = React.useRef<HTMLInputElement>(null);
  // Payslip view (ES-26)
  const [payslipRunId, setPayslipRunId] = useState<string>('');
  const [payslipPayrollRuns, setPayslipPayrollRuns] = useState<any[]>([]);
  const [payslipRunsLoading, setPayslipRunsLoading] = useState(false);
  const [payslip, setPayslip] = useState<any | null>(null);
  const [payslipLoading, setPayslipLoading] = useState(false);
  const [payslipError, setPayslipError] = useState<string | null>(null);
  // SS-32–SS-35: Manage grades & bands (structure → grades → bands hierarchy)
  const [manageStructureId, setManageStructureId] = useState<string | null>(null);
  const [manageStructureName, setManageStructureName] = useState('');
  const [gradesList, setGradesList] = useState<any[]>([]);
  const [bandsByGradeId, setBandsByGradeId] = useState<Record<string, any[]>>({});
  const [addGradeForm, setAddGradeForm] = useState({
    code: '',
    name: '',
    displayOrder: 0,
    /** SS-20: After creating a grade, copy bands from this existing grade (optional). */
    copyBandsFromGradeId: '' as string,
  });
  const [addGradeError, setAddGradeError] = useState<string | null>(null);
  const [editingGradeId, setEditingGradeId] = useState<string | null>(null);
  const [editGradeForm, setEditGradeForm] = useState({ name: '', displayOrder: 0 });
  const [addBandGradeId, setAddBandGradeId] = useState<string | null>(null);
  const [addBandForm, setAddBandForm] = useState({ minimumAmount: '', maximumAmount: '', midPoint: '', name: '', code: '', displayOrder: 0 });
  const [addBandError, setAddBandError] = useState<string | null>(null);
  const [editingBandId, setEditingBandId] = useState<string | null>(null);
  const [editBandForm, setEditBandForm] = useState({ minimumAmount: '', maximumAmount: '', midPoint: '', name: '', code: '', displayOrder: 0 });
  const [manageLoading, setManageLoading] = useState(false);
  /** SS-21: When set, new structure creation copies grades/bands from this structure id. */
  const [copySourceStructureId, setCopySourceStructureId] = useState<string | null>(null);
  const [structureCopying, setStructureCopying] = useState(false);
  /** SS-22: Headcount rows for Manage grades & bands warnings. */
  const [gradeHeadcountRows, setGradeHeadcountRows] = useState<any[]>([]);
  /** ES-21: Bulk revision modal */
  const [showBulkRevisionModal, setShowBulkRevisionModal] = useState(false);
  const [bulkRevisionList, setBulkRevisionList] = useState<any[]>([]);
  const [bulkRevisionLoading, setBulkRevisionLoading] = useState(false);
  const [bulkRevisionForm, setBulkRevisionForm] = useState({
    targetType: 'BY_GRADE' as 'BY_GRADE' | 'BY_STRUCTURE',
    targetStructureId: '',
    targetGradeId: '',
    componentCode: 'BASIC',
    percentageValue: '5',
    effectiveFrom: new Date().toISOString().split('T')[0],
    comment: '',
  });
  const [bulkRevisionError, setBulkRevisionError] = useState<string | null>(null);
  const [bulkRevisionGrades, setBulkRevisionGrades] = useState<any[]>([]);
  const [bulkRevisionStructureForGrade, setBulkRevisionStructureForGrade] = useState('');

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId, effectiveDateFilter, includeInactiveFilter]);

  useEffect(() => {
    if (employeePanelTab !== 'payslip' || !currentOrganizationId) return;
    let cancelled = false;
    (async () => {
      setPayslipRunsLoading(true);
      try {
        const res = await getPayrollRuns(currentOrganizationId);
        const list = Array.isArray(res.data) ? res.data : [];
        if (!cancelled) setPayslipPayrollRuns(list);
      } catch (err) {
        console.error('Failed to load payroll runs for payslip picker', err);
        if (!cancelled) setPayslipPayrollRuns([]);
      } finally {
        if (!cancelled) setPayslipRunsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [employeePanelTab, currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const params = effectiveDateFilter || includeInactiveFilter
        ? { effectiveDate: effectiveDateFilter || undefined, includeInactive: includeInactiveFilter }
        : undefined;
      // Load components without type/category filter so Assign form has both Earning and Deduction components.
      // Components tab filters client-side below.
      const compFilters = componentFilters.effectiveDate || componentFilters.includeInactive
        ? {
            effectiveDate: componentFilters.effectiveDate || undefined,
            includeInactive: componentFilters.includeInactive,
          }
        : undefined;
      const [structuresRes, salariesRes, positionsRes, employeesRes, componentsRes] = await Promise.all([
        getSalaryStructures(currentOrganizationId, params),
        getEmployeeSalaryDetails(currentOrganizationId),
        getPositions(currentOrganizationId, { activeOnly: true }),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
        getSalaryComponents(currentOrganizationId, compFilters),
      ]);
      setStructures(structuresRes.data);
      setEmployeeSalaries(salariesRes.data);
      setPositions(positionsRes.data || []);
      setEmployees(employeesRes.data || []);
      setSalaryComponents(componentsRes.data || []);
      if (employeePanelTab === 'payslip') {
        try {
          const pr = await getPayrollRuns(currentOrganizationId);
          setPayslipPayrollRuns(Array.isArray(pr.data) ? pr.data : []);
        } catch (e) {
          console.error('Failed to refresh payroll runs', e);
        }
      }
    } catch (err) {
      console.error('Failed to load salary data:', err);
      setError('Failed to load salary data');
    } finally {
      setLoading(false);
    }
  };

  const resetStructureForm = () => {
    setEditingStructureId(null);
    setCopySourceStructureId(null);
    setStructureForm({
      code: '',
      structureName: '',
      description: '',
      currency: 'BDT',
      payFrequency: 'monthly',
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
      isDefault: false,
    });
    setStructureFormError(null);
  };

  const openEditStructure = (structure: any) => {
    setEditingStructureId(structure.salaryStructureId);
    setStructureForm({
      code: structure.code || '',
      structureName: structure.structureName || '',
      description: structure.description || '',
      currency: structure.currency || 'BDT',
      payFrequency: structure.payFrequency || 'monthly',
      effectiveFrom: structure.effectiveFrom ? structure.effectiveFrom.split('T')[0] : new Date().toISOString().split('T')[0],
      effectiveTo: structure.effectiveTo ? structure.effectiveTo.split('T')[0] : '',
      isDefault: Boolean(structure.isDefault),
    });
    setStructureFormError(null);
    setShowStructureForm(true);
  };

  /** SS-38 / SS-21: Copy structure — open create form with fields pre-filled; grades/bands copied after save. */
  const openCopyStructure = (structure: any) => {
    setEditingStructureId(null);
    setCopySourceStructureId(structure.salaryStructureId || null);
    setStructureForm({
      code: (structure.code ? structure.code + '-COPY' : '').substring(0, 50),
      structureName: (structure.structureName || '') + ' (Copy)',
      description: structure.description || '',
      currency: structure.currency || 'BDT',
      payFrequency: structure.payFrequency || 'monthly',
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
      isDefault: false,
    });
    setStructureFormError(null);
    setShowStructureForm(true);
  };

  /** SS-36: Load and show structure summary (grades × bands). */
  const openStructureSummary = async (structure: any) => {
    try {
      const res = await getStructureSummary(structure.salaryStructureId);
      setStructureSummary(res.data);
      setShowSummaryModal(true);
    } catch (e) {
      console.error('Failed to load structure summary:', e);
      setStructureSummary(null);
      setShowSummaryModal(true);
    }
  };

  /** SS-29: Load and show revision history. */
  const openRevisionHistory = async (structure: any) => {
    if (!currentOrganizationId) return;
    try {
      const res = await getStructureRevisionHistory(structure.salaryStructureId, currentOrganizationId);
      setRevisionHistory(res.data || []);
      setRevisionStructureName(structure.structureName || structure.code || 'Structure');
      setShowRevisionModal(true);
    } catch (e) {
      console.error('Failed to load revision history:', e);
      setRevisionHistory([]);
      setShowRevisionModal(true);
    }
  };

  /** SS-32–SS-35: Open Manage Grades & Bands for a structure; load grades then bands per grade. */
  const openManageGradesBands = async (structure: any) => {
    const sid = structure.salaryStructureId;
    if (!sid) return;
    setManageStructureId(sid);
    setManageStructureName(structure.structureName || structure.code || 'Structure');
    setGradesList([]);
    setBandsByGradeId({});
    setAddGradeForm({ code: '', name: '', displayOrder: 0, copyBandsFromGradeId: '' });
    setAddGradeError(null);
    setEditingGradeId(null);
    setAddBandGradeId(null);
    setAddBandForm({ minimumAmount: '', maximumAmount: '', midPoint: '', name: '', code: '', displayOrder: 0 });
    setAddBandError(null);
    setEditingBandId(null);
    setManageLoading(true);
    try {
      const gradesRes = await getGradesByStructure(sid);
      const grades = Array.isArray(gradesRes.data) ? gradesRes.data : [];
      setGradesList(grades);
      const bandsMap: Record<string, any[]> = {};
      await Promise.all(
        grades.map(async (g: any) => {
          try {
            const bandsRes = await getBandsByGrade(g.salaryGradeId);
            bandsMap[g.salaryGradeId] = Array.isArray(bandsRes.data) ? bandsRes.data : [];
          } catch {
            bandsMap[g.salaryGradeId] = [];
          }
        })
      );
      setBandsByGradeId(bandsMap);
      if (currentOrganizationId) {
        try {
          const headRes = await getGradeHeadcountReport(
            currentOrganizationId,
            new Date().toISOString().split('T')[0]
          );
          setGradeHeadcountRows(Array.isArray(headRes.data) ? headRes.data : []);
        } catch {
          setGradeHeadcountRows([]);
        }
      }
    } catch (e) {
      console.error('Failed to load grades:', e);
      setGradesList([]);
      setBandsByGradeId({});
      setGradeHeadcountRows([]);
    } finally {
      setManageLoading(false);
    }
  };

  const closeManageGradesBands = () => {
    setManageStructureId(null);
    setManageStructureName('');
    setGradesList([]);
    setBandsByGradeId({});
    setGradeHeadcountRows([]);
    setAddGradeError(null);
    setAddBandError(null);
    setEditingGradeId(null);
    setEditingBandId(null);
    setAddBandGradeId(null);
  };

  const loadBandsForGrade = async (gradeId: string) => {
    try {
      const res = await getBandsByGrade(gradeId);
      setBandsByGradeId((prev) => ({ ...prev, [gradeId]: Array.isArray(res.data) ? res.data : [] }));
    } catch {
      setBandsByGradeId((prev) => ({ ...prev, [gradeId]: [] }));
    }
  };

  const handleAddGrade = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!manageStructureId) return;
    const code = addGradeForm.code.trim();
    const name = addGradeForm.name.trim();
    if (!code) {
      setAddGradeError('Grade code is required.');
      return;
    }
    const duplicate = gradesList.some((g: any) => (g.code || '').toLowerCase() === code.toLowerCase());
    if (duplicate) {
      setAddGradeError('Grade code must be unique within this structure.');
      return;
    }
    setAddGradeError(null);
    setManageLoading(true);
    try {
      await createGrade(manageStructureId, {
        code,
        name: name || code,
        displayOrder: addGradeForm.displayOrder,
      });
      const gradesRes = await getGradesByStructure(manageStructureId);
      const grades = Array.isArray(gradesRes.data) ? gradesRes.data : [];
      const newGrade = grades.find((g: any) => (g.code || '').toLowerCase() === code.toLowerCase());
      const copyFrom = addGradeForm.copyBandsFromGradeId?.trim();
      const effectiveFromFallback = new Date().toISOString().split('T')[0];
      if (newGrade && copyFrom && copyFrom !== newGrade.salaryGradeId) {
        let srcBands = bandsByGradeId[copyFrom] || [];
        if (srcBands.length === 0) {
          try {
            const bandsRes = await getBandsByGrade(copyFrom);
            srcBands = Array.isArray(bandsRes.data) ? bandsRes.data : [];
          } catch {
            srcBands = [];
          }
        }
        for (const b of srcBands) {
          const bandPayload: Record<string, unknown> = {
            minimumAmount: Number(b.minimumAmount),
            maximumAmount: Number(b.maximumAmount),
            displayOrder: b.displayOrder ?? 0,
          };
          if (b.midPoint != null) bandPayload.midPoint = Number(b.midPoint);
          if (b.name) bandPayload.name = b.name;
          if (b.code) bandPayload.code = b.code;
          if (b.currency) bandPayload.currency = b.currency;
          if (b.effectiveFrom) bandPayload.effectiveFrom = String(b.effectiveFrom).split('T')[0];
          else bandPayload.effectiveFrom = effectiveFromFallback;
          if (b.effectiveTo) bandPayload.effectiveTo = String(b.effectiveTo).split('T')[0];
          await createBand(newGrade.salaryGradeId, bandPayload);
        }
      }
      setGradesList(grades);
      const bandsMap: Record<string, any[]> = {};
      for (const g of grades) {
        try {
          const bandsRes = await getBandsByGrade(g.salaryGradeId);
          bandsMap[g.salaryGradeId] = Array.isArray(bandsRes.data) ? bandsRes.data : [];
        } catch {
          bandsMap[g.salaryGradeId] = [];
        }
      }
      setBandsByGradeId(bandsMap);
      setAddGradeForm({ code: '', name: '', displayOrder: grades.length, copyBandsFromGradeId: '' });
      await refreshGradeHeadcount();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to add grade.';
      setAddGradeError(msg);
    } finally {
      setManageLoading(false);
    }
  };

  const handleUpdateGrade = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingGradeId) return;
    setManageLoading(true);
    try {
      await updateGrade(editingGradeId, {
        name: editGradeForm.name.trim() || undefined,
        displayOrder: editGradeForm.displayOrder,
      });
      if (manageStructureId) {
        const gradesRes = await getGradesByStructure(manageStructureId);
        setGradesList(Array.isArray(gradesRes.data) ? gradesRes.data : []);
      }
      setEditingGradeId(null);
      await refreshGradeHeadcount();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to update grade.';
      setAddGradeError(msg);
    } finally {
      setManageLoading(false);
    }
  };

  const startEditGrade = (grade: any) => {
    setEditingGradeId(grade.salaryGradeId);
    setEditGradeForm({ name: grade.name || '', displayOrder: grade.displayOrder ?? 0 });
    setAddGradeError(null);
  };

  const validateBandAmounts = (minS: string, maxS: string, midS: string): string | null => {
    const min = parseFloat(minS);
    const max = parseFloat(maxS);
    if (isNaN(min) || isNaN(max) || min >= max) return 'Minimum must be less than maximum.';
    if (midS.trim() !== '') {
      const mid = parseFloat(midS);
      if (isNaN(mid) || mid < min || mid > max) return 'Mid point must be between minimum and maximum.';
    }
    return null;
  };

  const handleAddBand = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!addBandGradeId) return;
    const minS = addBandForm.minimumAmount.trim();
    const maxS = addBandForm.maximumAmount.trim();
    const midS = addBandForm.midPoint.trim();
    const err = validateBandAmounts(minS, maxS, midS);
    if (err) {
      setAddBandError(err);
      return;
    }
    setAddBandError(null);
    setManageLoading(true);
    try {
      await createBand(addBandGradeId, {
        minimumAmount: parseFloat(minS),
        maximumAmount: parseFloat(maxS),
        midPoint: midS ? parseFloat(midS) : undefined,
        name: addBandForm.name.trim() || undefined,
        code: addBandForm.code.trim() || undefined,
        displayOrder: addBandForm.displayOrder,
      });
      await loadBandsForGrade(addBandGradeId);
      setAddBandForm({ minimumAmount: '', maximumAmount: '', midPoint: '', name: '', code: '', displayOrder: 0 });
      setAddBandGradeId(null);
      await refreshGradeHeadcount();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to add band.';
      setAddBandError(msg);
    } finally {
      setManageLoading(false);
    }
  };

  const handleUpdateBand = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingBandId) return;
    const minS = editBandForm.minimumAmount.trim();
    const maxS = editBandForm.maximumAmount.trim();
    const midS = editBandForm.midPoint.trim();
    const err = validateBandAmounts(minS, maxS, midS);
    if (err) {
      setAddBandError(err);
      return;
    }
    setAddBandError(null);
    setManageLoading(true);
    try {
      await updateBand(editingBandId, {
        minimumAmount: parseFloat(minS),
        maximumAmount: parseFloat(maxS),
        midPoint: midS ? parseFloat(midS) : undefined,
        name: editBandForm.name.trim() || undefined,
        code: editBandForm.code.trim() || undefined,
        displayOrder: editBandForm.displayOrder,
      });
      const gradeWithBand = gradesList.find((g: any) =>
        (bandsByGradeId[g.salaryGradeId] || []).some((b: any) => b.salaryBandId === editingBandId)
      );
      if (gradeWithBand) await loadBandsForGrade(gradeWithBand.salaryGradeId);
      setEditingBandId(null);
      await refreshGradeHeadcount();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to update band.';
      setAddBandError(msg);
    } finally {
      setManageLoading(false);
    }
  };

  const startEditBand = (band: any, _gradeId: string) => {
    setEditingBandId(band.salaryBandId);
    setEditBandForm({
      minimumAmount: String(band.minimumAmount ?? ''),
      maximumAmount: String(band.maximumAmount ?? ''),
      midPoint: band.midPoint != null ? String(band.midPoint) : '',
      name: band.name || '',
      code: band.code || '',
      displayOrder: band.displayOrder ?? 0,
    });
    setAddBandError(null);
  };

  const cancelEditBand = () => {
    setEditingBandId(null);
  };

  /** SS-21: Copy grades and bands from an existing structure into a newly created one. */
  const copyGradesBandsFromSource = async (
    sourceStructureId: string,
    targetStructureId: string,
    effectiveFromFallback: string
  ) => {
    const gradesRes = await getGradesByStructure(sourceStructureId);
    const grades = Array.isArray(gradesRes.data) ? gradesRes.data : [];
    for (const g of grades) {
      const gradePayload: Record<string, unknown> = {
        code: g.code,
        name: g.name || g.code,
        displayOrder: g.displayOrder ?? 0,
      };
      if (g.effectiveFrom) gradePayload.effectiveFrom = String(g.effectiveFrom).split('T')[0];
      else gradePayload.effectiveFrom = effectiveFromFallback;
      if (g.effectiveTo) gradePayload.effectiveTo = String(g.effectiveTo).split('T')[0];
      if (g.description) gradePayload.description = g.description;
      const newGradeRes = await createGrade(targetStructureId, gradePayload);
      const newGradeId = (newGradeRes.data as any)?.salaryGradeId;
      if (!newGradeId) continue;
      const bandsRes = await getBandsByGrade(g.salaryGradeId);
      const bands = Array.isArray(bandsRes.data) ? bandsRes.data : [];
      for (const b of bands) {
        const bandPayload: Record<string, unknown> = {
          minimumAmount: Number(b.minimumAmount),
          maximumAmount: Number(b.maximumAmount),
          displayOrder: b.displayOrder ?? 0,
        };
        if (b.midPoint != null) bandPayload.midPoint = Number(b.midPoint);
        if (b.name) bandPayload.name = b.name;
        if (b.code) bandPayload.code = b.code;
        if (b.currency) bandPayload.currency = b.currency;
        if (b.effectiveFrom) bandPayload.effectiveFrom = String(b.effectiveFrom).split('T')[0];
        else bandPayload.effectiveFrom = effectiveFromFallback;
        if (b.effectiveTo) bandPayload.effectiveTo = String(b.effectiveTo).split('T')[0];
        await createBand(newGradeId, bandPayload);
      }
    }
  };

  const refreshGradeHeadcount = async () => {
    if (!currentOrganizationId) return;
    try {
      const headRes = await getGradeHeadcountReport(
        currentOrganizationId,
        new Date().toISOString().split('T')[0]
      );
      setGradeHeadcountRows(Array.isArray(headRes.data) ? headRes.data : []);
    } catch {
      /* ignore */
    }
  };

  const headcountForGrade = (structureId: string | null, gradeId: string) => {
    if (!structureId) return undefined;
    const row = gradeHeadcountRows.find(
      (r: any) =>
        r.structureId === structureId &&
        r.gradeId === gradeId &&
        (r.bandId == null || r.bandId === '')
    );
    return row?.headcount;
  };

  const headcountForBand = (structureId: string | null, gradeId: string, bandId: string) => {
    if (!structureId) return undefined;
    const row = gradeHeadcountRows.find(
      (r: any) => r.structureId === structureId && r.gradeId === gradeId && r.bandId === bandId
    );
    return row?.headcount;
  };

  const handleStructureSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentOrganizationId) {
      setStructureFormError('No organization selected.');
      return;
    }

    if (!structureForm.structureName.trim()) {
      setStructureFormError('Structure name is required.');
      return;
    }
    // SS-35: effectiveTo must be >= effectiveFrom
    if (structureForm.effectiveTo && structureForm.effectiveFrom && structureForm.effectiveTo < structureForm.effectiveFrom) {
      setStructureFormError('Effective To must be greater than or equal to Effective From.');
      return;
    }

    const isEdit = Boolean(editingStructureId);
    if (!isEdit && !structureForm.code.trim()) {
      setStructureFormError('Structure code is required.');
      return;
    }

    try {
      setStructureFormError(null);
      if (isEdit) {
        await updateSalaryStructure(editingStructureId!, {
          structureName: structureForm.structureName.trim(),
          description: structureForm.description?.trim() || undefined,
          currency: structureForm.currency || 'BDT',
          payFrequency: structureForm.payFrequency,
          effectiveFrom: structureForm.effectiveFrom,
          effectiveTo: structureForm.effectiveTo || undefined,
          isActive: true,
          isDefault: structureForm.isDefault,
        });
        alert('Salary structure updated successfully!');
      } else {
        const created = await createSalaryStructure({
          organizationId: currentOrganizationId,
          code: structureForm.code.trim(),
          structureName: structureForm.structureName.trim(),
          description: structureForm.description?.trim() || undefined,
          currency: structureForm.currency || 'BDT',
          payFrequency: structureForm.payFrequency,
          effectiveFrom: structureForm.effectiveFrom,
          effectiveTo: structureForm.effectiveTo || undefined,
          isActive: true,
          isDefault: structureForm.isDefault,
        });
        const newStructureId = (created.data as any)?.salaryStructureId as string | undefined;
        if (newStructureId && copySourceStructureId && copySourceStructureId !== newStructureId) {
          setStructureCopying(true);
          try {
            await copyGradesBandsFromSource(copySourceStructureId, newStructureId, structureForm.effectiveFrom);
            alert('Salary structure created. Grades and bands were copied from the source structure (SS-21).');
          } catch (copyErr: any) {
            console.error(copyErr);
            const cm =
              copyErr?.response?.data?.message ||
              copyErr?.message ||
              'Structure was created but copying grades/bands failed. Add them manually via Manage.';
            alert(cm);
          } finally {
            setStructureCopying(false);
          }
        } else {
          alert('Salary structure created successfully!');
        }
      }
      resetStructureForm();
      setShowStructureForm(false);
      await loadData();
    } catch (err: any) {
      console.error('Failed to save salary structure:', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to save salary structure. Please try again.';
      setStructureFormError(msg);
    }
  };

  const resetAssignForm = () => {
    setAssignForm({
      employeeId: '',
      salaryStructureId: '',
      componentId: '',
      valueType: 'AMOUNT',
      amount: '',
      percentage: '',
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
    });
    setEditingDetailId(null);
    setAssignFormError(null);
    setBulkComponentAssignments({});
  };

  /** Initialize bulk assignments when opening assign modal for new (all components selected). */
  const initBulkAssign = () => {
    const active = salaryComponents.filter((c: any) => c.isActive);
    const initial: Record<string, { selected: boolean }> = {};
    active.forEach((c: any) => {
      initial[c.componentId] = { selected: true };
    });
    setBulkComponentAssignments(initial);
  };

  /** Get component type (handles both camelCase and snake_case from API). */
  const getComponentType = (c: any) => (c?.componentType ?? c?.component_type ?? '');

  /** Components filtered by type/category for the Components tab (client-side). */
  const filteredComponentsForTab = salaryComponents.filter((c: any) => {
    if (componentFilters.type && getComponentType(c).toUpperCase() !== componentFilters.type.toUpperCase()) return false;
    if (componentFilters.category && (c.category ?? '') !== componentFilters.category) return false;
    return true;
  });

  /** Derive valueType, amount, percentage from component definition (ES-07: copy from master). */
  const getComponentAssignValues = (comp: any): { valueType: 'AMOUNT' | 'PERCENTAGE' | 'USE_MASTER_DEFAULT'; amount?: number; percentage?: number } => {
    const basis = comp.calculationBasis || comp.calculation_type || '';
    if (basis === 'FIXED' && (comp.defaultAmount != null || comp.default_amount != null)) {
      const amt = Number(comp.defaultAmount ?? comp.default_amount ?? 0);
      return { valueType: 'AMOUNT', amount: amt };
    }
    if ((basis === 'PERCENTAGE_OF_BASIC' || basis === 'PERCENTAGE_OF_GROSS') && (comp.percentageValue != null || comp.percentage_value != null)) {
      const pct = Number(comp.percentageValue ?? comp.percentage_value ?? 0);
      return { valueType: 'PERCENTAGE', percentage: pct };
    }
    return { valueType: 'USE_MASTER_DEFAULT' };
  };

  /** FIXED/MANUAL with no master default — bulk assign must collect per-employee amount (e.g. Basic). */
  const componentNeedsAmountForBulkAssign = (comp: any) => {
    const basis = (comp.calculationBasis || comp.calculation_type || '') as string;
    if (basis !== 'FIXED' && basis !== 'MANUAL') return false;
    return comp.defaultAmount == null && comp.default_amount == null;
  };

  const resetComponentForm = () => {
    setEditingComponentId(null);
    setComponentUsage(null);
    setComponentForm({
      code: '',
      componentName: '',
      description: '',
      componentType: 'EARNING',
      category: '',
      calculationBasis: 'FIXED',
      defaultAmount: '',
      percentageValue: '',
      baseComponentCode: '',
      formulaExpression: '',
      statutoryType: '',
      ceilingAmount: '',
      floorAmount: '',
      isTaxable: true,
      isStatutory: false,
      shortName: '',
      currency: '',
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
      displayOrder: 0,
      isActive: true,
      taxability: '',
      statutoryTags: '',
      prorationRule: 'BY_DAYS',
      expenseAccountCode: '',
      liabilityAccountCode: '',
    });
    setComponentFormError(null);
  };

  const openEditComponent = async (component: any) => {
    setEditingComponentId(component.componentId);
    try {
      const [compRes, usageRes] = await Promise.all([
        getSalaryComponentById(component.componentId),
        getComponentUsage(component.componentId),
      ]);
      const c = compRes.data;
      setComponentUsage(usageRes.data || null);
      setComponentForm({
        code: c.code ?? '',
        componentName: c.componentName ?? '',
        description: c.description ?? '',
        componentType: c.componentType ?? 'EARNING',
        category: c.category ?? '',
        calculationBasis: c.calculationBasis ?? 'FIXED',
        defaultAmount: c.defaultAmount != null ? String(c.defaultAmount) : '',
        percentageValue: c.percentageValue != null ? String(c.percentageValue) : '',
        baseComponentCode: c.baseComponentCode ?? '',
        formulaExpression: c.formulaExpression ?? '',
        statutoryType: c.statutoryType ?? '',
        ceilingAmount: c.ceilingAmount != null ? String(c.ceilingAmount) : '',
        floorAmount: c.floorAmount != null ? String(c.floorAmount) : '',
        isTaxable: c.isTaxable !== false,
        isStatutory: c.isStatutory === true,
        shortName: c.shortName ?? '',
        currency: c.currency ?? '',
        effectiveFrom: c.effectiveFrom ? new Date(c.effectiveFrom).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
        effectiveTo: c.effectiveTo ? new Date(c.effectiveTo).toISOString().split('T')[0] : '',
        displayOrder: c.displayOrder ?? 0,
        isActive: c.isActive !== false,
        taxability: c.taxability ?? '',
        statutoryTags: Array.isArray(c.statutoryTags) ? c.statutoryTags.join(', ') : (c.statutoryTags ?? ''),
        prorationRule: (c.prorationRule ?? 'BY_DAYS') as 'BY_DAYS' | 'NO_PRORATION' | 'BY_HOURS',
        expenseAccountCode: c.expenseAccountCode ?? '',
        liabilityAccountCode: c.liabilityAccountCode ?? '',
      });
      setShowComponentForm(true);
    } catch (err) {
      console.error('Failed to load component for edit', err);
      setComponentFormError('Failed to load component.');
    }
  };

  const handleComponentReorder = async (component: any, direction: 'up' | 'down') => {
    const list = filteredComponentsForTab.length < salaryComponents.length ? filteredComponentsForTab : salaryComponents;
    const idx = list.findIndex((c: any) => c.componentId === component.componentId);
    if (idx < 0) return;
    const otherIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (otherIdx < 0 || otherIdx >= list.length) return;
    const other = list[otherIdx];
    const myOrder = component.displayOrder ?? idx;
    const otherOrder = other.displayOrder ?? otherIdx;
    try {
      await Promise.all([
        updateSalaryComponent(component.componentId, { ...component, displayOrder: otherOrder }),
        updateSalaryComponent(other.componentId, { ...other, displayOrder: myOrder }),
      ]);
      await loadData();
    } catch (err) {
      console.error('Failed to reorder component', err);
      alert('Failed to update display order.');
    }
  };

  const buildComponentPayload = () => {
    const payload: any = {
      componentName: componentForm.componentName.trim(),
      description: componentForm.description?.trim() || null,
      componentType: componentForm.componentType,
      category: componentForm.category || null,
      calculationBasis: componentForm.calculationBasis,
      isTaxable: componentForm.isTaxable,
      isStatutory: componentForm.isStatutory,
      isActive: editingComponentId ? componentForm.isActive : true,
      effectiveFrom: componentForm.effectiveFrom || new Date().toISOString().split('T')[0],
      effectiveTo: componentForm.effectiveTo?.trim() || null,
      displayOrder: componentForm.displayOrder ?? 0,
      shortName: componentForm.shortName?.trim() || null,
      currency: componentForm.currency?.trim() || null,
      defaultAmount: componentForm.defaultAmount ? parseFloat(componentForm.defaultAmount) : null,
      percentageValue: componentForm.percentageValue ? parseFloat(componentForm.percentageValue) : null,
      baseComponentCode: componentForm.baseComponentCode?.trim() || null,
      formulaExpression: componentForm.formulaExpression?.trim() || null,
      statutoryType: componentForm.statutoryType?.trim() || null,
        ceilingAmount: componentForm.ceilingAmount ? parseFloat(componentForm.ceilingAmount) : null,
        floorAmount: componentForm.floorAmount ? parseFloat(componentForm.floorAmount) : null,
      taxability: componentForm.taxability || null,
      statutoryTags: componentForm.statutoryTags
        ? componentForm.statutoryTags.split(/[,;]/).map((s) => s.trim()).filter(Boolean)
        : [],
      prorationRule: componentForm.prorationRule || 'BY_DAYS',
      expenseAccountCode: componentForm.expenseAccountCode?.trim() || null,
      liabilityAccountCode: componentForm.liabilityAccountCode?.trim() || null,
    };
    if (editingComponentId) {
      payload.isActive = componentForm.isActive;
    }
    if (!editingComponentId) {
      payload.organizationId = currentOrganizationId;
      payload.code = componentForm.code.trim();
    }
    return payload;
  };

  const handleComponentSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentOrganizationId && !editingComponentId) {
      setComponentFormError('No organization selected.');
      return;
    }

    if (!editingComponentId) {
      if (!componentForm.code.trim()) {
        setComponentFormError('Code is required and must be unique per organization.');
        return;
      }
      if (!/^[A-Za-z0-9_]{1,20}$/.test(componentForm.code.trim())) {
        setComponentFormError('Code must be alphanumeric or underscore, max 20 characters (SC-25).');
        return;
      }
    }
    if (!componentForm.componentName.trim()) {
      setComponentFormError('Component name is required.');
      return;
    }
    if (componentForm.effectiveTo && componentForm.effectiveFrom && componentForm.effectiveTo < componentForm.effectiveFrom) {
      setComponentFormError('Effective To must be on or after Effective From.');
      return;
    }
    const ceil = componentForm.ceilingAmount ? parseFloat(componentForm.ceilingAmount) : NaN;
    const floor = componentForm.floorAmount ? parseFloat(componentForm.floorAmount) : NaN;
    if (!Number.isNaN(ceil) && !Number.isNaN(floor) && ceil < floor) {
      setComponentFormError('Ceiling must be greater than or equal to floor (SC-27).');
      return;
    }

    try {
      setComponentFormError(null);
      const payload = buildComponentPayload();
      if (editingComponentId) {
        await updateSalaryComponent(editingComponentId, payload);
        resetComponentForm();
        setShowComponentForm(false);
        await loadData();
        alert('Salary component updated successfully!');
      } else {
        await createSalaryComponent(payload);
        resetComponentForm();
        setShowComponentForm(false);
        await loadData();
        alert('Salary component created successfully!');
      }
    } catch (err: any) {
      console.error('Failed to save salary component:', err);
      const status = err?.response?.status;
      const msg = err?.response?.data?.message || err?.message;
      const displayMsg = msg && typeof msg === 'string' ? msg : 'Failed to save salary component. Please try again.';
      setComponentFormError(status === 409 ? `Conflict: ${displayMsg}` : displayMsg);
    }
  };

  /** SC-48: Export component master to Excel or PDF. */
  const handleExportComponentMaster = async (format: 'excel' | 'pdf') => {
    if (!currentOrganizationId) return;
    try {
      const res = await exportComponentMaster(currentOrganizationId, format, {
        effectiveDate: componentFilters.effectiveDate || undefined,
        includeInactive: componentFilters.includeInactive,
        type: componentFilters.type || undefined,
        category: componentFilters.category || undefined,
      });
      const blob = new Blob([res.data], { type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `component-master.${format === 'pdf' ? 'pdf' : 'xlsx'}`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Export failed', err);
      alert(err?.response?.data?.message || 'Export failed.');
    }
  };

  /** SC-49: Load and show component dependency report. */
  const handleShowDependencyReport = async () => {
    if (!currentOrganizationId) return;
    setDependencyLoading(true);
    setShowDependencyModal(true);
    setComponentDependencies([]);
    try {
      const res = await getComponentDependencies(currentOrganizationId);
      setComponentDependencies(Array.isArray(res.data) ? res.data : []);
    } catch (err: any) {
      console.error('Dependency report failed', err);
      setComponentDependencies([]);
    } finally {
      setDependencyLoading(false);
    }
  };

  /** SC-50: Bulk import components from file. */
  const handleBulkImportComponents = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !currentOrganizationId) return;
    setComponentImportLoading(true);
    setComponentImportResult(null);
    try {
      const res = await bulkImportComponents(currentOrganizationId, file);
      setComponentImportResult({
        createdComponents: res.data?.createdComponents ?? 0,
        errors: res.data?.errors ?? [],
      });
      await loadData();
    } catch (err: any) {
      setComponentImportResult({
        createdComponents: 0,
        errors: [{ row: 0, type: 'Request', message: err?.response?.data?.message || err?.message || 'Import failed' }],
      });
    } finally {
      setComponentImportLoading(false);
    }
  };

  const handleAssignSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentOrganizationId) {
      setAssignFormError('No organization selected.');
      return;
    }

    if (editingDetailId) {
      // Single-component edit mode
      if (!assignForm.employeeId || !assignForm.salaryStructureId || !assignForm.componentId) {
        setAssignFormError('Employee, Salary Structure, and Component are required.');
        return;
      }
      const valueType = assignForm.valueType || 'AMOUNT';
      if (valueType === 'AMOUNT') {
        const amount = assignForm.amount ? parseFloat(assignForm.amount) : NaN;
        if (Number.isNaN(amount) || amount < 0) {
          setAssignFormError('Amount must be a non-negative number when Value type is Amount.');
          return;
        }
      } else if (valueType === 'PERCENTAGE') {
        const pct = assignForm.percentage ? parseFloat(assignForm.percentage) : NaN;
        if (Number.isNaN(pct) || pct < 0) {
          setAssignFormError('Percentage must be a non-negative number when Value type is Percentage.');
          return;
        }
      }
      const payload: any = {
        valueType,
        effectiveFrom: assignForm.effectiveFrom,
        effectiveTo: assignForm.effectiveTo || undefined,
        isActive: true,
      };
      if (valueType === 'AMOUNT') payload.amount = assignForm.amount ? parseFloat(assignForm.amount) : 0;
      else if (valueType === 'PERCENTAGE') payload.percentage = assignForm.percentage ? parseFloat(assignForm.percentage) : 0;
      try {
        setAssignFormError(null);
        await updateEmployeeSalaryDetail(editingDetailId, payload);
        alert('Salary component updated.');
        resetAssignForm();
        setShowAssignForm(false);
        await loadData();
      } catch (err: any) {
        const msg = err?.response?.data?.message || err?.response?.data?.error || err?.message || 'Failed to save.';
        setAssignFormError(typeof msg === 'string' ? msg : 'Failed to save.');
      }
      return;
    }

    // Bulk assign mode: multiple components at once; value type/amount/percentage from component definition
    if (!assignForm.employeeId || !assignForm.salaryStructureId) {
      setAssignFormError('Employee and Salary Structure are required.');
      return;
    }
    const selected = Object.entries(bulkComponentAssignments).filter(([, v]) => v.selected);
    if (selected.length === 0) {
      setAssignFormError('Select at least one salary component to assign.');
      return;
    }

    const bulkValidationErrors: string[] = [];
    for (const [compId] of selected) {
      const comp = salaryComponents.find((c: any) => c.componentId === compId);
      if (comp && componentNeedsAmountForBulkAssign(comp)) {
        const raw = bulkComponentAssignments[compId]?.amountOverride?.trim();
        if (!raw) {
          bulkValidationErrors.push(
            `${comp.componentName || comp.code || compId}: enter per-period amount (this component has no default amount on the master).`,
          );
          continue;
        }
        const n = parseFloat(raw);
        if (Number.isNaN(n) || n < 0) {
          bulkValidationErrors.push(`${comp.componentName || comp.code || compId}: amount must be a non-negative number.`);
        }
      }
    }
    if (bulkValidationErrors.length > 0) {
      setAssignFormError(bulkValidationErrors.join(' '));
      return;
    }

    try {
      setAssignFormError(null);
      let created = 0;
      const errors: string[] = [];
      for (const [compId] of selected) {
        const comp = salaryComponents.find((c: any) => c.componentId === compId);
        let { valueType: vt, amount: amt, percentage: pct } = comp ? getComponentAssignValues(comp) : { valueType: 'USE_MASTER_DEFAULT' as const };
        if (comp && componentNeedsAmountForBulkAssign(comp)) {
          const raw = bulkComponentAssignments[compId]?.amountOverride?.trim() ?? '';
          const n = parseFloat(raw);
          vt = 'AMOUNT';
          amt = n;
        }
        const payload: any = {
          employeeId: assignForm.employeeId,
          organizationId: currentOrganizationId,
          salaryStructureId: assignForm.salaryStructureId,
          componentId: compId,
          valueType: vt,
          effectiveFrom: assignForm.effectiveFrom,
          effectiveTo: assignForm.effectiveTo || undefined,
          isActive: true,
        };
        if (vt === 'AMOUNT' && amt != null) payload.amount = amt;
        else if (vt === 'PERCENTAGE' && pct != null) payload.percentage = pct;
        try {
          await createEmployeeSalaryDetail(payload);
          created++;
        } catch (e: any) {
          errors.push(`${comp?.componentName || compId}: ${e?.response?.data?.message || e?.message || 'Failed'}`);
        }
      }
      if (created > 0) {
        alert(errors.length > 0
          ? `${created} component(s) assigned. ${errors.length} failed: ${errors.join('; ')}`
          : `${created} salary component(s) assigned successfully!`);
      }
      if (errors.length > 0 && created === 0) {
        setAssignFormError(errors.join('; '));
        return;
      }
      resetAssignForm();
      setShowAssignForm(false);
      await loadData();
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.response?.data?.error || err?.message || 'Failed to save.';
      setAssignFormError(typeof msg === 'string' ? msg : 'Failed to save.');
    }
  };

  const openEmployeePanel = async (employeeId: string) => {
    if (!currentOrganizationId) {
      setEmployeePanelError('No organization selected.');
      return;
    }
    const emp = employees.find((e) => e.employeeId === employeeId);
    const name =
      emp && (`${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.employeeNumber || employeeId);
    setSelectedEmployeeId(employeeId);
    setSelectedEmployeeName(name || employeeId);
    setShowEmployeePanel(true);
    setEmployeePanelTab('current');
    setEmployeePanelLoading(true);
    setEmployeePanelError(null);
    try {
      const asOf = employeeSalaryAsOfDate || undefined;
      let assignment: any | null = null;

      try {
        const assignmentRes = await getEmployeeAssignment(employeeId, currentOrganizationId, asOf);
        assignment = assignmentRes.data || null;

        // Resolve human-readable names for structure, grade and band for display.
        if (assignment && assignment.salaryStructureId) {
          try {
            const summaryRes = await getStructureSummary(assignment.salaryStructureId, asOf);
            const summary = summaryRes.data;
            if (summary) {
              assignment.structureName = summary.structureName || summary.code || assignment.salaryStructureId;
              if (assignment.salaryGradeId && Array.isArray(summary.grades)) {
                const grade = summary.grades.find((g: any) => g.salaryGradeId === assignment.salaryGradeId);
                if (grade) {
                  assignment.gradeName = grade.name || grade.code || assignment.salaryGradeId;
                  if (assignment.salaryBandId && Array.isArray(grade.bands)) {
                    const band = grade.bands.find((b: any) => b.salaryBandId === assignment.salaryBandId);
                    if (band) {
                      assignment.bandName = band.name || band.code || assignment.salaryBandId;
                    }
                  }
                }
              }
            }
          } catch (e) {
            // If name resolution fails, fall back to IDs without blocking the panel.
          }
        }
      } catch (err: any) {
        // Treat 404 as "no active assignment" instead of a hard error.
        if (err?.response?.status === 404) {
          assignment = null;
        } else {
          const msg = err?.response?.data?.message || err?.message || 'Failed to load employee salary assignment.';
          setEmployeePanelError(typeof msg === 'string' ? msg : 'Failed to load employee salary assignment.');
        }
      }

      const [detailsRes, historyRes] = await Promise.all([
        getEmployeeSalaryDetails(currentOrganizationId, { employeeId, asOfDate: asOf }),
        getEmployeeSalaryRevisionHistory(employeeId, currentOrganizationId),
      ]);

      setEmployeeAssignment(assignment);
      setEmployeePanelDetails(detailsRes.data || []);
      setEmployeeRevisionHistory(historyRes.data || []);
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to load employee salary.';
      setEmployeePanelError(typeof msg === 'string' ? msg : 'Failed to load employee salary.');
    } finally {
      setEmployeePanelLoading(false);
    }
  };

  const resetAssignmentForm = () => {
    setAssignmentForm({
      employeeId: selectedEmployeeId || '',
      salaryStructureId: employeeAssignment?.salaryStructureId || '',
      salaryGradeId: employeeAssignment?.salaryGradeId || '',
      salaryBandId: employeeAssignment?.salaryBandId || '',
      effectiveFrom:
        (employeeAssignment?.effectiveFrom
          ? new Date(employeeAssignment.effectiveFrom).toISOString().split('T')[0]
          : new Date().toISOString().split('T')[0]),
      effectiveTo: employeeAssignment?.effectiveTo
        ? new Date(employeeAssignment.effectiveTo).toISOString().split('T')[0]
        : '',
      source: (employeeAssignment?.source || 'OVERRIDE') as 'POSITION' | 'OVERRIDE',
    });
    setAssignmentFormError(null);
    setEditingAssignmentId(employeeAssignment?.assignmentId || null);
    setAssignmentGrades([]);
    setAssignmentBands([]);
  };

  const loadAssignmentGradesAndBands = async (structureId: string, gradeId?: string) => {
    try {
      if (!structureId) {
        setAssignmentGrades([]);
        setAssignmentBands([]);
        return;
      }
      const gradesRes = await getGradesByStructure(structureId);
      const grades = gradesRes.data || [];
      setAssignmentGrades(grades);
      let effectiveGradeId = gradeId || assignmentForm.salaryGradeId;
      if (!effectiveGradeId && grades.length > 0) {
        effectiveGradeId = grades[0].salaryGradeId;
      }
      if (effectiveGradeId) {
        const bandsRes = await getBandsByGrade(effectiveGradeId);
        setAssignmentBands(bandsRes.data || []);
      } else {
        setAssignmentBands([]);
      }
    } catch (err) {
      console.error('Failed to load grades/bands for assignment', err);
    }
  };

  const openAssignmentModal = async () => {
    if (!selectedEmployeeId) return;
    resetAssignmentForm();
    if (employeeAssignment?.salaryStructureId) {
      await loadAssignmentGradesAndBands(employeeAssignment.salaryStructureId, employeeAssignment.salaryGradeId);
    }
    setShowAssignmentModal(true);
  };

  const handleAssignmentStructureChange = async (structureId: string) => {
    setAssignmentForm((prev) => ({ ...prev, salaryStructureId: structureId, salaryGradeId: '', salaryBandId: '' }));
    setAssignmentGrades([]);
    setAssignmentBands([]);
    if (structureId) {
      await loadAssignmentGradesAndBands(structureId);
    }
  };

  const handleAssignmentGradeChange = async (gradeId: string) => {
    setAssignmentForm((prev) => ({ ...prev, salaryGradeId: gradeId, salaryBandId: '' }));
    setAssignmentBands([]);
    if (gradeId) {
      const bandsRes = await getBandsByGrade(gradeId);
      setAssignmentBands(bandsRes.data || []);
    }
  };

  const handleAssignmentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEmployeeId) {
      setAssignmentFormError('No employee selected.');
      return;
    }
    if (!assignmentForm.salaryStructureId || !assignmentForm.salaryGradeId) {
      setAssignmentFormError('Structure and Grade are required.');
      return;
    }
    if (assignmentForm.effectiveTo && assignmentForm.effectiveTo < assignmentForm.effectiveFrom) {
      setAssignmentFormError('Effective To must be on or after Effective From.');
      return;
    }
    try {
      setAssignmentFormError(null);
      const payload: any = {
        employeeId: selectedEmployeeId,
        salaryStructureId: assignmentForm.salaryStructureId,
        salaryGradeId: assignmentForm.salaryGradeId,
        salaryBandId: assignmentForm.salaryBandId || null,
        effectiveFrom: assignmentForm.effectiveFrom,
        effectiveTo: assignmentForm.effectiveTo || null,
        source: assignmentForm.source,
        revisionReason: employeeAssignment ? 'Updated via UI' : 'Initial assignment via UI',
      };
      if (editingAssignmentId) {
        await updateEmployeeSalaryAssignment(editingAssignmentId, payload);
        alert('Salary assignment updated successfully.');
      } else {
        await createEmployeeSalaryAssignment(payload);
        alert('Salary assignment created successfully.');
      }
      setShowAssignmentModal(false);
      await openEmployeePanel(selectedEmployeeId);
    } catch (err: any) {
      console.error('Failed to save salary assignment', err);
      const msg = err?.response?.data?.message || err?.response?.data?.error || err?.message || 'Failed to save salary assignment.';
      setAssignmentFormError(typeof msg === 'string' ? msg : 'Failed to save salary assignment.');
    }
  };

  /** INT-28–INT-31: Fill structure/grade/band from employee's position defaults. */
  const fillAssignmentFromPosition = async () => {
    if (!selectedEmployeeId) return;
    const emp = employees.find((e) => e.employeeId === selectedEmployeeId);
    const pid = emp?.positionId;
    if (!pid) {
      setAssignmentFormError(
        'This employee has no position. Assign a position on the employee record first.'
      );
      return;
    }
    try {
      setAssignmentFormError(null);
      const res = await getPositionSalaryDefaults(pid);
      const d = res.data;
      if (!d?.defaultSalaryStructureId) {
        setAssignmentFormError(
          'This position has no default salary structure. Configure defaults on the position (Positions).'
        );
        return;
      }
      const sid = d.defaultSalaryStructureId;
      const gid = d.defaultSalaryGradeId || '';
      const bid = d.defaultSalaryBandId || '';
      setAssignmentForm((prev) => ({
        ...prev,
        salaryStructureId: sid,
        salaryGradeId: gid,
        salaryBandId: bid,
        source: 'POSITION',
      }));
      await loadAssignmentGradesAndBands(sid, gid || undefined);
      if (gid && bid) {
        const bandsRes = await getBandsByGrade(gid);
        const bands = Array.isArray(bandsRes.data) ? bandsRes.data : [];
        if (!bands.some((b: any) => b.salaryBandId === bid)) {
          setAssignmentForm((prev) => ({ ...prev, salaryBandId: '' }));
        }
      }
    } catch (err: any) {
      const msg =
        err?.response?.status === 404
          ? 'No salary defaults found for this position.'
          : err?.response?.data?.message || err?.message || 'Failed to load position salary defaults.';
      setAssignmentFormError(typeof msg === 'string' ? msg : 'Failed to load defaults.');
    }
  };

  const loadBulkRevisionGrades = async (structureId: string) => {
    if (!structureId) {
      setBulkRevisionGrades([]);
      return;
    }
    try {
      const res = await getGradesByStructure(structureId);
      setBulkRevisionGrades(Array.isArray(res.data) ? res.data : []);
    } catch {
      setBulkRevisionGrades([]);
    }
  };

  const openBulkRevisionModal = async () => {
    setShowBulkRevisionModal(true);
    setBulkRevisionError(null);
    setBulkRevisionGrades([]);
    setBulkRevisionStructureForGrade('');
    if (!currentOrganizationId) return;
    setBulkRevisionLoading(true);
    try {
      const res = await listSalaryBulkRevisions(currentOrganizationId, 'PENDING');
      setBulkRevisionList(Array.isArray(res.data) ? res.data : []);
    } catch {
      setBulkRevisionList([]);
    } finally {
      setBulkRevisionLoading(false);
    }
  };

  const submitBulkRevision = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;
    const pct = parseFloat(bulkRevisionForm.percentageValue);
    if (Number.isNaN(pct) || pct < 0) {
      setBulkRevisionError('Enter a valid non-negative percentage.');
      return;
    }
    if (bulkRevisionForm.targetType === 'BY_GRADE' && !bulkRevisionForm.targetGradeId) {
      setBulkRevisionError('Select a structure and grade.');
      return;
    }
    if (bulkRevisionForm.targetType === 'BY_STRUCTURE' && !bulkRevisionForm.targetStructureId) {
      setBulkRevisionError('Select a salary structure.');
      return;
    }
    if (!bulkRevisionForm.componentCode.trim()) {
      setBulkRevisionError('Component code is required (e.g. BASIC).');
      return;
    }
    try {
      setBulkRevisionError(null);
      setBulkRevisionLoading(true);
      await createSalaryBulkRevision({
        organizationId: currentOrganizationId,
        revisionType: 'BULK_PERCENTAGE',
        targetType: bulkRevisionForm.targetType,
        targetGradeId: bulkRevisionForm.targetType === 'BY_GRADE' ? bulkRevisionForm.targetGradeId : null,
        targetStructureId: bulkRevisionForm.targetType === 'BY_STRUCTURE' ? bulkRevisionForm.targetStructureId : null,
        componentCode: bulkRevisionForm.componentCode.trim().toUpperCase(),
        percentageValue: pct,
        effectiveFrom: bulkRevisionForm.effectiveFrom,
        comment: bulkRevisionForm.comment.trim() || undefined,
        requestedBy: user?.id || user?.email || 'ui',
      });
      const res = await listSalaryBulkRevisions(currentOrganizationId, 'PENDING');
      setBulkRevisionList(Array.isArray(res.data) ? res.data : []);
      alert('Bulk revision request created (PENDING). Approve below to apply to employee salary details.');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to create bulk revision.';
      setBulkRevisionError(typeof msg === 'string' ? msg : 'Failed.');
    } finally {
      setBulkRevisionLoading(false);
    }
  };

  const handleLoadPayslip = async () => {
    if (!selectedEmployeeId) {
      setPayslipError('No employee selected.');
      return;
    }
    if (!payslipRunId.trim()) {
      setPayslipError('Please select a payroll run (type to search by name or period).');
      return;
    }
    try {
      setPayslipLoading(true);
      setPayslipError(null);
      setPayslip(null);
      const res = await getPayslip(payslipRunId.trim(), selectedEmployeeId);
      setPayslip(res.data || null);
    } catch (err: any) {
      console.error('Failed to load payslip', err);
      const msg = err?.response?.data?.message || err?.response?.data?.error || err?.message || 'Failed to load payslip.';
      setPayslipError(typeof msg === 'string' ? msg : 'Failed to load payslip.');
    } finally {
      setPayslipLoading(false);
    }
  };

  const formatCurrency = (amount?: number, currency?: string) => {
    const curr = currency || 'BDT';
    if (typeof amount !== 'number' || Number.isNaN(amount)) {
      return curr === 'BDT' ? '৳0.00' : `${curr} 0.00`;
    }
    try {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: curr,
        minimumFractionDigits: 2,
      }).format(amount);
    } catch (err) {
      const symbol = curr === 'BDT' ? '৳' : `${curr} `;
      return `${symbol}${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
  };

  if (loading) return <div className="loading">Loading salary data...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Salary Management</h1>
        <p>Manage salary structures and employee compensation</p>
        <div className="header-actions">
          <button className="btn-secondary" onClick={loadData}>
            Refresh
          </button>
          <button
            className="btn-secondary"
            onClick={() => {
              resetAssignForm();
              initBulkAssign();
              setShowAssignForm(true);
            }}
          >
            + Assign Salary to Employee
          </button>
          <button
            className="btn-primary"
            onClick={() => {
              resetStructureForm();
              setCopySourceStructureId(null);
              setShowStructureForm(true);
            }}
          >
            + New Salary Structure
          </button>
        </div>
      </div>

      {showAssignForm && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          onClick={() => {
            setShowAssignForm(false);
            resetAssignForm();
          }}
        >
          <div className="hr-modal" onClick={(e) => e.stopPropagation()} style={editingDetailId ? undefined : { maxWidth: '720px', maxHeight: '90vh', overflow: 'auto' }}>
            <h2>{editingDetailId ? 'Edit Employee Salary Component' : 'Assign Salary Components to Employee'}</h2>
            <form onSubmit={handleAssignSubmit}>
              <div className="form-row">
                <label>Employee *</label>
                <select
                  value={assignForm.employeeId}
                  onChange={(event) =>
                    setAssignForm({ ...assignForm, employeeId: event.target.value })
                  }
                  required
                >
                  <option value="">-- Select Employee --</option>
                  {employees.map((employee) => (
                    <option key={employee.employeeId} value={employee.employeeId}>
                      {employee.firstName} {employee.lastName} ({employee.employeeNumber || employee.employeeId?.substring(0, 8)})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <label>Salary Structure *</label>
                <select
                  value={assignForm.salaryStructureId}
                  onChange={(event) =>
                    setAssignForm({ ...assignForm, salaryStructureId: event.target.value })
                  }
                  required
                >
                  <option value="">-- Select Salary Structure --</option>
                  {structures.filter(s => s.isActive).map((structure) => (
                    <option key={structure.salaryStructureId} value={structure.salaryStructureId}>
                      {structure.structureName} {structure.code ? `(${structure.code})` : ''} — {structure.currency || 'BDT'}
                    </option>
                  ))}
                </select>
              </div>

              {editingDetailId ? (
                <>
                  <div className="form-row">
                    <label>Salary Component *</label>
                    <select
                      value={assignForm.componentId}
                      onChange={(event) =>
                        setAssignForm({ ...assignForm, componentId: event.target.value })
                      }
                      required
                    >
                      <option value="">-- Select Component --</option>
                      {salaryComponents.filter(c => c.isActive).map((component) => (
                        <option key={component.componentId} value={component.componentId}>
                          {component.componentName} {component.code ? `(${component.code})` : ''} — {getComponentType(component) || '-'}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-row">
                    <label>Value type (ES-07)</label>
                    <select
                      value={assignForm.valueType}
                      onChange={(e) =>
                        setAssignForm({ ...assignForm, valueType: e.target.value as 'AMOUNT' | 'PERCENTAGE' | 'USE_MASTER_DEFAULT' })
                      }
                    >
                      <option value="AMOUNT">Amount (fixed per period)</option>
                      <option value="PERCENTAGE">Percentage (override %; base from master)</option>
                      <option value="USE_MASTER_DEFAULT">Use master default</option>
                    </select>
                  </div>
                  {assignForm.valueType === 'AMOUNT' && (
                    <div className="form-row">
                      <label>Amount *</label>
                      <input
                        type="number"
                        step="0.01"
                        min={0}
                        value={assignForm.amount}
                        onChange={(event) =>
                          setAssignForm({ ...assignForm, amount: event.target.value })
                        }
                        placeholder="Fixed amount per pay period"
                      />
                    </div>
                  )}
                  {assignForm.valueType === 'PERCENTAGE' && (
                    <div className="form-row">
                      <label>Percentage *</label>
                      <input
                        type="number"
                        step="0.01"
                        min={0}
                        value={assignForm.percentage}
                        onChange={(event) =>
                          setAssignForm({ ...assignForm, percentage: event.target.value })
                        }
                        placeholder="e.g. 40 for 40%"
                      />
                    </div>
                  )}
                  <div className="form-row">
                    <label>Effective From *</label>
                    <input
                      type="date"
                      value={assignForm.effectiveFrom}
                      onChange={(event) =>
                        setAssignForm({ ...assignForm, effectiveFrom: event.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="form-row">
                    <label>Effective To</label>
                    <input
                      type="date"
                      value={assignForm.effectiveTo}
                      onChange={(event) =>
                        setAssignForm({ ...assignForm, effectiveTo: event.target.value })
                      }
                    />
                  </div>
                </>
              ) : (
                <>
                  <div className="form-row" style={{ marginBottom: '0.75rem' }}>
                    <label style={{ marginRight: '0.5rem' }}>Effective From *</label>
                    <input
                      type="date"
                      value={assignForm.effectiveFrom}
                      onChange={(event) =>
                        setAssignForm({ ...assignForm, effectiveFrom: event.target.value })
                      }
                      required
                    />
                    <label style={{ marginLeft: '1rem', marginRight: '0.5rem' }}>Effective To</label>
                    <input
                      type="date"
                      value={assignForm.effectiveTo}
                      onChange={(event) =>
                        setAssignForm({ ...assignForm, effectiveTo: event.target.value })
                      }
                    />
                  </div>
                  <div style={{ marginBottom: '0.5rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <strong>Select components to assign.</strong>{' '}
                    <span style={{ fontWeight: 'normal', color: '#555' }}>
                      Percentages copy from the master. For Fixed/Manual components with no default amount on the master, fill <strong>Per-period amount</strong> (required for payroll).
                    </span>
                    <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem' }}
                      onClick={() => {
                        const next: Record<string, { selected: boolean; amountOverride?: string }> = {};
                        Object.keys(bulkComponentAssignments).forEach(id => {
                          next[id] = {
                            selected: true,
                            amountOverride: bulkComponentAssignments[id]?.amountOverride,
                          };
                        });
                        setBulkComponentAssignments(next);
                      }}>
                      Select all
                    </button>
                    <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem' }}
                      onClick={() => {
                        const next: Record<string, { selected: boolean; amountOverride?: string }> = {};
                        Object.keys(bulkComponentAssignments).forEach(id => {
                          next[id] = {
                            selected: false,
                            amountOverride: bulkComponentAssignments[id]?.amountOverride,
                          };
                        });
                        setBulkComponentAssignments(next);
                      }}>
                      Deselect all
                    </button>
                  </div>
                  <div style={{ maxHeight: '280px', overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 6, marginBottom: '1rem' }}>
                    <table className="hr-table" style={{ marginBottom: 0 }}>
                      <thead>
                        <tr>
                          <th style={{ width: 36 }}></th>
                          <th>Component</th>
                          <th>Type</th>
                          <th>From definition</th>
                          <th>Per-period amount</th>
                        </tr>
                      </thead>
                      <tbody>
                        {salaryComponents.filter((c: any) => c.isActive).map((component) => {
                          const cfg = bulkComponentAssignments[component.componentId] ?? { selected: false };
                          const assignVals = getComponentAssignValues(component);
                          const needsAmount = componentNeedsAmountForBulkAssign(component);
                          const defLabel = assignVals.valueType === 'AMOUNT' && assignVals.amount != null
                            ? `${component.currency || ''} ${assignVals.amount}`
                            : assignVals.valueType === 'PERCENTAGE' && assignVals.percentage != null
                              ? `${assignVals.percentage}%`
                              : needsAmount
                                ? 'No master default'
                                : 'Use default';
                          return (
                            <tr key={component.componentId}>
                              <td>
                                <input
                                  type="checkbox"
                                  checked={cfg.selected}
                                  onChange={(e) =>
                                    setBulkComponentAssignments(prev => ({
                                      ...prev,
                                      [component.componentId]: {
                                        selected: e.target.checked,
                                        amountOverride: prev[component.componentId]?.amountOverride,
                                      },
                                    }))
                                  }
                                />
                              </td>
                              <td><strong>{component.componentName}</strong> {component.code ? <code style={{ fontSize: '0.75rem' }}>({component.code})</code> : ''}</td>
                              <td>{getComponentType(component).toUpperCase() === 'EARNING' ? 'Earning' : 'Deduction'}</td>
                              <td style={{ color: '#666', fontSize: '0.875rem' }}>{defLabel}</td>
                              <td>
                                {needsAmount ? (
                                  <input
                                    type="number"
                                    step="0.01"
                                    min={0}
                                    value={cfg.amountOverride ?? ''}
                                    onChange={(e) =>
                                      setBulkComponentAssignments(prev => ({
                                        ...prev,
                                        [component.componentId]: {
                                          selected: prev[component.componentId]?.selected ?? false,
                                          amountOverride: e.target.value,
                                        },
                                      }))
                                    }
                                    placeholder="Required"
                                    title="Required when the component master has no default amount (e.g. Basic Salary)."
                                    style={{ width: '7.5rem' }}
                                  />
                                ) : (
                                  <span style={{ color: '#aaa' }}>—</span>
                                )}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    {salaryComponents.filter((c: any) => c.isActive).length === 0 && (
                      <p style={{ padding: '1rem', color: '#666' }}>No salary components found. Create components first.</p>
                    )}
                  </div>
                </>
              )}

              {assignFormError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>
                  {assignFormError}
                </div>
              )}

              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setShowAssignForm(false);
                    resetAssignForm();
                  }}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={salaryComponents.filter((c: any) => c.isActive).length === 0}>
                  {editingDetailId ? 'Update' : 'Save All'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showStructureForm && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          onClick={() => {
            setShowStructureForm(false);
            resetStructureForm();
          }}
        >
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{editingStructureId ? 'Edit Salary Structure' : 'Create Salary Structure'}</h2>
            <form onSubmit={handleStructureSubmit}>
              <div className="form-row">
                <label>Structure Code *</label>
                <input
                  type="text"
                  value={structureForm.code}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, code: event.target.value })
                  }
                  required={!editingStructureId}
                  readOnly={!!editingStructureId}
                  placeholder="e.g. IND-MONTHLY, US-BIWEEKLY"
                  title={editingStructureId ? 'Code cannot be changed after creation' : ''}
                />
                {editingStructureId && (
                  <small style={{ color: '#666', marginTop: '0.25rem', display: 'block' }}>
                    Code is immutable after creation.
                  </small>
                )}
              </div>

              <div className="form-row">
                <label>Structure Name *</label>
                <input
                  type="text"
                  value={structureForm.structureName}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, structureName: event.target.value })
                  }
                  required
                />
              </div>

              <div className="form-row">
                <label>Description</label>
                <textarea
                  value={structureForm.description}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, description: event.target.value })
                  }
                  rows={2}
                  placeholder="Optional description of the salary structure"
                />
              </div>

              <div className="form-row">
                <label>Currency</label>
                <select
                  value={structureForm.currency}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, currency: event.target.value })
                  }
                >
                  <option value="BDT">BDT - Bangladeshi Taka</option>
                  <option value="USD">USD - US Dollar</option>
                  <option value="EUR">EUR - Euro</option>
                  <option value="GBP">GBP - British Pound</option>
                  <option value="INR">INR - Indian Rupee</option>
                </select>
              </div>

              <div className="form-row">
                <label>Pay Frequency</label>
                <select
                  value={structureForm.payFrequency}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, payFrequency: event.target.value })
                  }
                >
                  <option value="monthly">Monthly</option>
                  <option value="bi-weekly">Bi-weekly</option>
                  <option value="weekly">Weekly</option>
                  <option value="annual">Annual</option>
                </select>
              </div>

              <div className="form-row">
                <label>Effective From *</label>
                <input
                  type="date"
                  value={structureForm.effectiveFrom}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, effectiveFrom: event.target.value })
                  }
                  required
                />
              </div>

              <div className="form-row">
                <label>Effective To</label>
                <input
                  type="date"
                  value={structureForm.effectiveTo}
                  onChange={(event) =>
                    setStructureForm({ ...structureForm, effectiveTo: event.target.value })
                  }
                />
              </div>

              <div className="form-row">
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={structureForm.isDefault}
                    onChange={(event) =>
                      setStructureForm({ ...structureForm, isDefault: event.target.checked })
                    }
                  />
                  Set as default structure for this organization (SS-06)
                </label>
                <small style={{ color: '#666', marginTop: '0.25rem', display: 'block' }}>
                  Only one structure can be default per organization. Used when no structure is explicitly assigned.
                </small>
              </div>

              {!editingStructureId && copySourceStructureId && (
                <p style={{ fontSize: '0.9rem', color: '#0b5ed7', marginBottom: '0.75rem' }}>
                  SS-21: After you save, grades and bands are copied from the <strong>Copy source</strong> structure into
                  this new one.
                </p>
              )}

              {structureFormError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>
                  {structureFormError}
                </div>
              )}

              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setShowStructureForm(false);
                    resetStructureForm();
                  }}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={structureCopying}>
                  {structureCopying
                    ? 'Copying grades/bands…'
                    : editingStructureId
                      ? 'Save Changes'
                      : 'Create Structure'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="tabs">
        <button
          className={activeTab === 'structures' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('structures')}
        >
          Salary Structures
        </button>
        <button
          className={activeTab === 'components' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('components')}
        >
          Salary Components
        </button>
        <button
          className={activeTab === 'employees' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('employees')}
        >
          Employee Salaries
        </button>
      </div>

      {activeTab === 'structures' && (
        <div className="hr-section">
          <h2>Salary Structures</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', marginBottom: '1rem', padding: '0.75rem', background: 'var(--surface)', borderRadius: 6 }}>
            <span style={{ fontWeight: 600, color: 'var(--text-secondary)', marginRight: '0.25rem' }}>Filters:</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <label style={{ fontSize: '0.875rem', whiteSpace: 'nowrap' }}>Effective date</label>
              <input
                type="date"
                value={effectiveDateFilter}
                onChange={(e) => setEffectiveDateFilter(e.target.value)}
                title="Show structures effective on this date"
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', whiteSpace: 'nowrap', fontSize: '0.875rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={includeInactiveFilter}
                onChange={(e) => setIncludeInactiveFilter(e.target.checked)}
              />
              Include inactive
            </label>
          </div>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Structure Name</th>
                <th>Description</th>
                <th>Currency</th>
                <th>Pay Frequency</th>
                <th>Effective From</th>
                <th>Effective To</th>
                <th>Default</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {structures.length === 0 ? (
                <tr>
                  <td colSpan={10} style={{ textAlign: 'center' }}>
                    No salary structures found. Create salary structures to define compensation templates.
                  </td>
                </tr>
              ) : (
                structures.map((structure) => (
                  <tr key={structure.salaryStructureId}>
                    <td><code>{structure.code || '-'}</code></td>
                    <td><strong>{structure.structureName}</strong></td>
                    <td>{structure.description ? (structure.description.length > 50 ? structure.description.slice(0, 50) + '…' : structure.description) : '—'}</td>
                    <td>{structure.currency || 'USD'}</td>
                    <td>{structure.payFrequency || 'monthly'}</td>
                    <td>{structure.effectiveFrom ? new Date(structure.effectiveFrom).toLocaleDateString() : '-'}</td>
                    <td>{structure.effectiveTo ? new Date(structure.effectiveTo).toLocaleDateString() : '-'}</td>
                    <td>
                      {structure.isDefault ? (
                        <span className="status-badge status-active" title="Default structure for this organization">Default</span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      <span className={`status-badge status-${structure.isActive ? 'active' : 'inactive'}`}>
                        {structure.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.25rem' }}>
                        <button
                          type="button"
                          className="btn-secondary"
                          style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                          onClick={() => openEditStructure(structure)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn-primary"
                          style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                          onClick={() => openManageGradesBands(structure)}
                        >
                          Manage
                        </button>
                        <button
                          type="button"
                          className="btn-secondary"
                          style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                          onClick={() => openStructureSummary(structure)}
                        >
                          Summary
                        </button>
                        <button
                          type="button"
                          className="btn-secondary"
                          style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                          onClick={() => openCopyStructure(structure)}
                        >
                          Copy
                        </button>
                        <button
                          type="button"
                          className="btn-secondary"
                          style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                          onClick={() => openRevisionHistory(structure)}
                        >
                          History
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'components' && (
        <div className="hr-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <h2>Salary Components</h2>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
              <button
                className="btn-secondary"
                onClick={() => handleExportComponentMaster('excel')}
                title="SC-48: Export component master to Excel"
              >
                Export Excel
              </button>
              <button
                className="btn-secondary"
                onClick={() => handleExportComponentMaster('pdf')}
                title="SC-48: Export component master to PDF"
              >
                Export PDF
              </button>
              <button
                className="btn-secondary"
                onClick={handleShowDependencyReport}
                title="SC-49: Components that reference other components (base or formula)"
              >
                Dependency report
              </button>
              <input
                ref={componentFileInputRef}
                type="file"
                accept=".xlsx,.xls"
                style={{ display: 'none' }}
                onChange={handleBulkImportComponents}
              />
              <button
                className="btn-secondary"
                disabled={componentImportLoading}
                onClick={() => componentFileInputRef.current?.click()}
                title="SC-50: Bulk import from XLSX (sheet 'Components')"
              >
                {componentImportLoading ? 'Importing…' : 'Bulk import'}
              </button>
              <button
                className="btn-primary"
                onClick={() => {
                  resetComponentForm();
                  setShowComponentForm(true);
                }}
              >
                + New Component
              </button>
            </div>
          </div>
          {componentImportResult != null && (
            <div style={{ marginBottom: '1rem', padding: '0.75rem', background: 'var(--surface)', borderRadius: 6, border: '1px solid var(--border)' }}>
              <strong>Bulk import result:</strong> {componentImportResult.createdComponents} component(s) created.
              {componentImportResult.errors.length > 0 && (
                <ul style={{ margin: '0.5rem 0 0 1rem', padding: 0 }}>
                  {componentImportResult.errors.map((err: any, i: number) => (
                    <li key={i} style={{ color: err.row === 0 ? 'var(--error)' : undefined }}>
                      Row {err.row} ({err.type}): {err.message}
                    </li>
                  ))}
                </ul>
              )}
              <button type="button" className="btn-secondary" style={{ marginTop: '0.5rem' }} onClick={() => setComponentImportResult(null)}>Dismiss</button>
            </div>
          )}
          <div className="form-row" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', marginBottom: '1rem', padding: '0.75rem', background: 'var(--surface)', borderRadius: 6 }}>
            <span style={{ fontWeight: 600, color: 'var(--text-secondary)', marginRight: '0.25rem' }}>Filters:</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <label style={{ fontSize: '0.875rem', whiteSpace: 'nowrap' }}>Type</label>
              <select
                value={componentFilters.type}
                onChange={(e) => setComponentFilters((f) => ({ ...f, type: e.target.value }))}
                style={{ minWidth: '120px' }}
              >
                <option value="">All</option>
                <option value="EARNING">Earning</option>
                <option value="DEDUCTION">Deduction</option>
                <option value="ALLOWANCE">Allowance</option>
              </select>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <label style={{ fontSize: '0.875rem', whiteSpace: 'nowrap' }}>Category</label>
              <select
                value={componentFilters.category}
                onChange={(e) => setComponentFilters((f) => ({ ...f, category: e.target.value }))}
                style={{ minWidth: '160px' }}
              >
                <option value="">All</option>
                <optgroup label="Earnings">
                  <option value="BASIC">Basic</option>
                  <option value="HRA">HRA</option>
                  <option value="SPECIAL_ALLOWANCE">Special Allowance</option>
                  <option value="OTHER_ALLOWANCE">Other Allowance</option>
                </optgroup>
                <optgroup label="Deductions">
                  <option value="STATUTORY_DEDUCTION">Statutory Deduction</option>
                  <option value="LOAN_REPAYMENT">Loan Repayment</option>
                  <option value="OTHER_DEDUCTION">Other Deduction</option>
                </optgroup>
              </select>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <label style={{ fontSize: '0.875rem', whiteSpace: 'nowrap' }}>Effective date</label>
              <input
                type="date"
                value={componentFilters.effectiveDate}
                onChange={(e) => setComponentFilters((f) => ({ ...f, effectiveDate: e.target.value }))}
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', whiteSpace: 'nowrap', fontSize: '0.875rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={componentFilters.includeInactive}
                onChange={(e) => setComponentFilters((f) => ({ ...f, includeInactive: e.target.checked }))}
              />
              Include inactive
            </label>
            <button type="button" className="btn-secondary" onClick={() => loadData()}>
              Apply
            </button>
          </div>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Type</th>
                <th>Category</th>
                <th>Calculation</th>
                <th>Effective From</th>
                <th>Order</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredComponentsForTab.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center' }}>
                    No salary components found. Apply filters or create components (e.g., Basic Salary, Allowances, Deductions).
                  </td>
                </tr>
              ) : (
                filteredComponentsForTab.map((component, idx) => (
                  <tr key={component.componentId}>
                    <td><code>{component.code ?? '—'}</code></td>
                    <td><strong>{component.componentName}</strong></td>
                    <td>{getComponentType(component) || '-'}</td>
                    <td>{component.category ? component.category.replace(/_/g, ' ') : '-'}</td>
                    <td>{component.calculationBasis ?? '—'}</td>
                    <td>{component.effectiveFrom ? new Date(component.effectiveFrom).toLocaleDateString() : '—'}</td>
                    <td>{component.displayOrder ?? '—'}</td>
                    <td>
                      <span className={`status-badge status-${component.isActive ? 'active' : 'inactive'}`}>
                        {component.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                        <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.4rem', fontSize: '0.8rem' }} onClick={() => openEditComponent(component)}>Edit</button>
                        <button type="button" title="Move up" style={{ padding: '0.2rem 0.4rem' }} onClick={() => handleComponentReorder(component, 'up')} disabled={idx === 0}>↑</button>
                        <button type="button" title="Move down" style={{ padding: '0.2rem 0.4rem' }} onClick={() => handleComponentReorder(component, 'down')} disabled={idx === filteredComponentsForTab.length - 1}>↓</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {showComponentForm && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          onClick={() => {
            setShowComponentForm(false);
            resetComponentForm();
          }}
        >
          <div className="hr-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '560px', maxHeight: '90vh', overflow: 'auto' }}>
            <h2>{editingComponentId ? 'Edit Salary Component' : 'Create Salary Component'}</h2>
            {editingComponentId && componentUsage != null && (
              <div style={{ marginBottom: '1rem', padding: '0.5rem 0.75rem', background: 'var(--surface)', borderRadius: 6, fontSize: '0.9rem' }}>
                <strong>Usage (SC-41):</strong> Used by {componentUsage.employeeCount} employee(s), referenced in {componentUsage.referencedInFormulasCount} formula(s).
              </div>
            )}
            <form onSubmit={handleComponentSubmit}>
              <div className="form-row">
                <label>Code *</label>
                <input
                  type="text"
                  value={componentForm.code}
                  onChange={(event) => !editingComponentId && setComponentForm({ ...componentForm, code: event.target.value })}
                  placeholder="e.g., BASIC, HRA — alphanumeric + underscore, max 20 chars; immutable after creation"
                  required
                  readOnly={!!editingComponentId}
                  maxLength={20}
                  style={editingComponentId ? { opacity: 0.8 } : undefined}
                />
              </div>

              <div className="form-row">
                <label>Component Name *</label>
                <input
                  type="text"
                  value={componentForm.componentName}
                  onChange={(event) => setComponentForm({ ...componentForm, componentName: event.target.value })}
                  placeholder="e.g., Basic Salary, House Rent Allowance"
                  required
                />
              </div>

              <div className="form-row">
                <label>Description</label>
                <textarea
                  value={componentForm.description}
                  onChange={(event) => setComponentForm({ ...componentForm, description: event.target.value })}
                  placeholder="Optional description"
                  rows={2}
                />
              </div>

              <div className="form-row">
                <label>Component Type *</label>
                <select
                  value={componentForm.componentType}
                  onChange={(event) => !editingComponentId && setComponentForm({ ...componentForm, componentType: event.target.value })}
                  required
                  disabled={!!editingComponentId}
                  style={editingComponentId ? { opacity: 0.8 } : undefined}
                >
                  <option value="EARNING">Earning</option>
                  <option value="DEDUCTION">Deduction</option>
                  <option value="ALLOWANCE">Allowance</option>
                </select>
              </div>

              <div className="form-row">
                <label>Category</label>
                <select
                  value={componentForm.category}
                  onChange={(event) => setComponentForm({ ...componentForm, category: event.target.value })}
                >
                  <option value="">— Select (optional) —</option>
                  <optgroup label="Earnings">
                    <option value="BASIC">Basic</option>
                    <option value="HRA">HRA</option>
                    <option value="SPECIAL_ALLOWANCE">Special Allowance</option>
                    <option value="CONVEYANCE">Conveyance</option>
                    <option value="MEDICAL">Medical</option>
                    <option value="LEAVE_TRAVEL">Leave Travel</option>
                    <option value="OTHER_ALLOWANCE">Other Allowance</option>
                  </optgroup>
                  <optgroup label="Deductions">
                    <option value="STATUTORY_DEDUCTION">Statutory Deduction</option>
                    <option value="VOLUNTARY_DEDUCTION">Voluntary Deduction</option>
                    <option value="LOAN_REPAYMENT">Loan Repayment</option>
                    <option value="RECOVERY">Recovery</option>
                    <option value="OTHER_DEDUCTION">Other Deduction</option>
                  </optgroup>
                </select>
              </div>

              <div className="form-row">
                <label>Calculation basis *</label>
                <select
                  value={componentForm.calculationBasis}
                  onChange={(event) => setComponentForm({ ...componentForm, calculationBasis: event.target.value })}
                >
                  <option value="FIXED">Fixed (default amount)</option>
                  <option value="PERCENTAGE_OF_BASIC">Percentage of Basic</option>
                  <option value="PERCENTAGE_OF_GROSS">Percentage of Gross</option>
                  <option value="FORMULA">Formula</option>
                  <option value="STATUTORY">Statutory</option>
                  <option value="MANUAL">Manual</option>
                </select>
              </div>

              {(componentForm.calculationBasis === 'FIXED' || componentForm.calculationBasis === 'MANUAL') && (
                <div className="form-row">
                  <label>Default amount</label>
                  <input
                    type="number"
                    step="0.01"
                    min={0}
                    value={componentForm.defaultAmount}
                    onChange={(e) => setComponentForm({ ...componentForm, defaultAmount: e.target.value })}
                    placeholder="Org-wide default (recommended for Basic). If empty, enter per employee under Assign Salary."
                  />
                </div>
              )}

              {(componentForm.calculationBasis === 'PERCENTAGE_OF_BASIC' || componentForm.calculationBasis === 'PERCENTAGE_OF_GROSS') && (
                <>
                  <div className="form-row">
                    <label>Base component code *</label>
                    <input
                      type="text"
                      value={componentForm.baseComponentCode}
                      onChange={(e) => setComponentForm({ ...componentForm, baseComponentCode: e.target.value })}
                      placeholder="e.g., BASIC"
                    />
                  </div>
                  <div className="form-row">
                    <label>Percentage *</label>
                    <input
                      type="number"
                      step="0.01"
                      min={0}
                      value={componentForm.percentageValue}
                      onChange={(e) => setComponentForm({ ...componentForm, percentageValue: e.target.value })}
                      placeholder="e.g., 40"
                    />
                  </div>
                </>
              )}

              {componentForm.calculationBasis === 'FORMULA' && (
                <div className="form-row">
                  <label>Formula expression *</label>
                  <input
                    type="text"
                    value={componentForm.formulaExpression}
                    onChange={(e) => setComponentForm({ ...componentForm, formulaExpression: e.target.value })}
                    placeholder="e.g., BASIC * 0.4 or BASIC + HRA * 0.1"
                  />
                  <small style={{ display: 'block', marginTop: 4, color: 'var(--text-secondary)' }}>
                    (SC-40) Use component codes (e.g. BASIC, HRA), numbers, and operators + - * / ( ). Example: BASIC * 0.4
                  </small>
                </div>
              )}

              {componentForm.calculationBasis === 'STATUTORY' && (
                <div className="form-row">
                  <label>Statutory type *</label>
                  <input
                    type="text"
                    value={componentForm.statutoryType}
                    onChange={(e) => setComponentForm({ ...componentForm, statutoryType: e.target.value })}
                    placeholder="e.g., PF_EMPLOYEE, INCOME_TAX"
                  />
                </div>
              )}

              <div className="form-row" style={{ display: 'flex', gap: '0.5rem' }}>
                <div style={{ flex: 1 }}>
                  <label>Floor (min amount)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={componentForm.floorAmount}
                    onChange={(e) => setComponentForm({ ...componentForm, floorAmount: e.target.value })}
                    placeholder="Optional"
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label>Ceiling (max amount)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={componentForm.ceilingAmount}
                    onChange={(e) => setComponentForm({ ...componentForm, ceilingAmount: e.target.value })}
                    placeholder="Optional"
                  />
                </div>
              </div>

              <div className="form-row" style={{ borderTop: '1px solid rgba(0,0,0,0.08)', paddingTop: '0.75rem', marginTop: '0.25rem' }}>
                <div style={{ fontWeight: 600, marginBottom: 6 }}>Accounting (INT-20)</div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: '0 0 0.5rem 0' }}>
                  Optional chart-of-accounts codes for payroll journal mapping. Included on accounting export lines for finance integration.
                </p>
                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <div style={{ flex: '1 1 200px' }}>
                    <label>Expense account (earnings)</label>
                    <input
                      type="text"
                      value={componentForm.expenseAccountCode}
                      onChange={(e) => setComponentForm({ ...componentForm, expenseAccountCode: e.target.value })}
                      placeholder="e.g. 6110"
                      autoComplete="off"
                    />
                  </div>
                  <div style={{ flex: '1 1 200px' }}>
                    <label>Liability account (deductions)</label>
                    <input
                      type="text"
                      value={componentForm.liabilityAccountCode}
                      onChange={(e) => setComponentForm({ ...componentForm, liabilityAccountCode: e.target.value })}
                      placeholder="e.g. 2020"
                      autoComplete="off"
                    />
                  </div>
                </div>
              </div>

              <div className="form-row" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ flex: '1 1 120px' }}>
                  <label>Effective From *</label>
                  <input
                    type="date"
                    value={componentForm.effectiveFrom}
                    onChange={(e) => setComponentForm({ ...componentForm, effectiveFrom: e.target.value })}
                  />
                </div>
                <div style={{ flex: '1 1 120px' }}>
                  <label>Effective To (blank = open-ended)</label>
                  <input
                    type="date"
                    value={componentForm.effectiveTo}
                    onChange={(e) => setComponentForm({ ...componentForm, effectiveTo: e.target.value })}
                  />
                </div>
                <div style={{ flex: '0 0 70px' }}>
                  <label>Order</label>
                  <input
                    type="number"
                    min={0}
                    value={componentForm.displayOrder}
                    onChange={(e) => setComponentForm({ ...componentForm, displayOrder: parseInt(e.target.value, 10) || 0 })}
                  />
                </div>
              </div>

              <div className="form-row">
                <label>Short name / payslip label (optional)</label>
                <input
                  type="text"
                  value={componentForm.shortName}
                  onChange={(e) => setComponentForm({ ...componentForm, shortName: e.target.value })}
                  placeholder="Short name on payslip, e.g. Basic Sal"
                />
              </div>

              <div className="form-row">
                <label>Currency (optional)</label>
                <input
                  type="text"
                  value={componentForm.currency}
                  onChange={(e) => setComponentForm({ ...componentForm, currency: e.target.value })}
                  placeholder="e.g., BDT, USD"
                  maxLength={3}
                  style={{ width: '6ch' }}
                />
              </div>

              <div className="form-row">
                <label>Proration rule (ES-29)</label>
                <select
                  value={componentForm.prorationRule}
                  onChange={(e) => setComponentForm({ ...componentForm, prorationRule: e.target.value as 'BY_DAYS' | 'NO_PRORATION' | 'BY_HOURS' })}
                >
                  <option value="BY_DAYS">By days (prorate by working days in period)</option>
                  <option value="NO_PRORATION">No proration (full amount if employed any day)</option>
                  <option value="BY_HOURS">By hours (when time data available)</option>
                </select>
              </div>

              <div className="form-row">
                <label>Taxability (income tax)</label>
                <select
                  value={componentForm.taxability}
                  onChange={(e) => setComponentForm({ ...componentForm, taxability: e.target.value, isTaxable: e.target.value === 'TAXABLE' })}
                >
                  <option value="">— Select (optional) —</option>
                  <option value="TAXABLE">Taxable</option>
                  <option value="EXEMPT">Exempt</option>
                  <option value="PARTIALLY_TAXABLE">Partially taxable</option>
                </select>
              </div>

              <div className="form-row">
                <label>Statutory tags (comma-separated)</label>
                <input
                  type="text"
                  value={componentForm.statutoryTags}
                  onChange={(e) => setComponentForm({ ...componentForm, statutoryTags: e.target.value })}
                  placeholder="e.g. PF_WAGE, PF_EMPLOYEE, TAXABLE, ESI_WAGE"
                />
                <small style={{ display: 'block', marginTop: 4, color: 'var(--text-secondary)' }}>
                  Used for statutory wage bases (e.g. PF wage = sum of components with PF_WAGE).
                </small>
              </div>

              <div className="form-row">
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={componentForm.isTaxable}
                    onChange={(e) => setComponentForm({ ...componentForm, isTaxable: e.target.checked })}
                  />
                  Taxable
                </label>
              </div>

              <div className="form-row">
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={componentForm.isStatutory}
                    onChange={(e) => setComponentForm({ ...componentForm, isStatutory: e.target.checked })}
                  />
                  Statutory
                </label>
              </div>

              {editingComponentId && (
                <div className="form-row">
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={componentForm.isActive}
                      onChange={(e) => setComponentForm({ ...componentForm, isActive: e.target.checked })}
                    />
                    Active (uncheck to deactivate)
                  </label>
                </div>
              )}

              {componentFormError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>
                  {componentFormError}
                </div>
              )}

              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => { setShowComponentForm(false); resetComponentForm(); }}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingComponentId ? 'Update Component' : 'Create Component'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {activeTab === 'employees' && (
        <div className="hr-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
            <h2 style={{ margin: 0 }}>Employee Salary Details (ES-07–ES-15)</h2>
            <button type="button" className="btn-secondary" onClick={openBulkRevisionModal}>
              Bulk salary revision (ES-21)
            </button>
          </div>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
            <label>
              As-of date (optional):{' '}
              <input
                type="date"
                value={employeeSalaryAsOfDate}
                onChange={(e) => setEmployeeSalaryAsOfDate(e.target.value)}
              />
            </label>
            {employeeSalaryAsOfDate && (
              <button type="button" className="btn-secondary" onClick={() => setEmployeeSalaryAsOfDate('')}>
                Clear filter
              </button>
            )}
          </div>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Component</th>
                <th>Value type</th>
                <th>Amount / %</th>
                <th>Effective from</th>
                <th>Effective to</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {(() => {
                const list = employeeSalaryAsOfDate
                  ? employeeSalaries.filter((s: any) => {
                      const from = s.effectiveFrom ? new Date(s.effectiveFrom).toISOString().split('T')[0] : '';
                      const to = s.effectiveTo ? new Date(s.effectiveTo).toISOString().split('T')[0] : null;
                      return from <= employeeSalaryAsOfDate && (!to || to >= employeeSalaryAsOfDate);
                    })
                  : employeeSalaries;
                if (list.length === 0) {
                  return (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center' }}>
                        {employeeSalaries.length === 0
                          ? 'No employee salary component records. Use "Assign Salary to Employee" to add components.'
                          : 'No records effective on the selected as-of date.'}
                      </td>
                    </tr>
                  );
                }
                return list.map((salary: any) => {
                  const emp = employees.find((e: Employee) => e.employeeId === salary.employeeId);
                  const comp = salaryComponents.find((c: any) => c.componentId === salary.componentId);
                  const empName = emp ? `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.employeeNumber : salary.employeeId;
                  const compName = comp ? (comp.componentName || comp.code || salary.componentId) : salary.componentId;
                  const valueDisplay =
                    salary.valueType === 'AMOUNT' && salary.amount != null
                      ? formatCurrency(Number(salary.amount))
                      : salary.valueType === 'PERCENTAGE' && salary.percentage != null
                        ? `${salary.percentage}%`
                        : salary.valueType === 'USE_MASTER_DEFAULT'
                          ? 'Use default'
                          : '—';
                  return (
                    <tr key={salary.salaryDetailId}>
                      <td>
                        <button
                          type="button"
                          className="btn-link"
                          style={{ padding: 0, border: 'none', background: 'none', color: '#0b5ed7', cursor: 'pointer' }}
                          onClick={() => openEmployeePanel(salary.employeeId)}
                        >
                          {empName}
                        </button>
                      </td>
                      <td>{compName}</td>
                      <td>{salary.valueType || 'AMOUNT'}</td>
                      <td>{valueDisplay}</td>
                      <td>{salary.effectiveFrom ? new Date(salary.effectiveFrom).toLocaleDateString() : '—'}</td>
                      <td>{salary.effectiveTo ? new Date(salary.effectiveTo).toLocaleDateString() : 'Open'}</td>
                      <td>
                        {salary.isActive !== false && (
                          <>
                            <button
                              type="button"
                              className="btn-secondary"
                              style={{ marginRight: '0.5rem' }}
                              onClick={() => {
                                setAssignForm({
                                  employeeId: salary.employeeId,
                                  salaryStructureId: salary.salaryStructureId || '',
                                  componentId: salary.componentId,
                                  valueType: (salary.valueType || 'AMOUNT') as 'AMOUNT' | 'PERCENTAGE' | 'USE_MASTER_DEFAULT',
                                  amount: salary.amount != null ? String(salary.amount) : '',
                                  percentage: salary.percentage != null ? String(salary.percentage) : '',
                                  effectiveFrom: salary.effectiveFrom ? new Date(salary.effectiveFrom).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
                                  effectiveTo: salary.effectiveTo ? new Date(salary.effectiveTo).toISOString().split('T')[0] : '',
                                });
                                setEditingDetailId(salary.salaryDetailId);
                                setShowAssignForm(true);
                                setAssignFormError(null);
                              }}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => {
                                const to = prompt('End date (effective To) for this component (YYYY-MM-DD):', new Date().toISOString().split('T')[0]);
                                if (!to) return;
                                endEmployeeSalaryDetail(salary.salaryDetailId, to)
                                  .then(() => { loadData(); })
                                  .catch((err) => alert(err?.response?.data?.message || err?.message || 'Failed to end assignment.'));
                              }}
                            >
                              End
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                });
              })()}
            </tbody>
          </table>
        </div>
      )}

      {/* SS-36: Structure summary modal (grades × bands) */}
      {showSummaryModal && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => { setShowSummaryModal(false); setStructureSummary(null); }}>
          <div className="hr-modal" style={{ maxWidth: '90vw' }} onClick={(e) => e.stopPropagation()}>
            <h2>Structure Summary {structureSummary?.structureName ? `– ${structureSummary.structureName}` : ''}</h2>
            {structureSummary?.grades?.length ? (
              <div style={{ overflowX: 'auto' }}>
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Grade (code)</th>
                      <th>Bands (min – mid – max)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {structureSummary.grades.map((g: any) => (
                      <tr key={g.salaryGradeId}>
                        <td><strong>{g.name}</strong> <code>{g.code}</code></td>
                        <td>
                          {g.bands?.length ? (
                            <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
                              {g.bands.map((b: any) => (
                                <li key={b.salaryBandId}>
                                  {formatCurrency(b.minimumAmount, b.currency || structureSummary.currency)}
                                  {b.midPoint != null ? ` – ${formatCurrency(b.midPoint, b.currency || structureSummary.currency)}` : ''}
                                  {' – '}{formatCurrency(b.maximumAmount, b.currency || structureSummary.currency)}
                                  {b.name ? ` (${b.name})` : ''}
                                </li>
                              ))}
                            </ul>
                          ) : (
                            <span style={{ color: '#666' }}>No bands</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ color: '#666' }}>No grades defined for this structure. Use &quot;Manage&quot; to add grades and bands.</p>
            )}
            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn-secondary" onClick={() => { setShowSummaryModal(false); setStructureSummary(null); }}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* SS-32–SS-35: Manage Grades & Bands modal (structure → grades → bands) */}
      {manageStructureId && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={closeManageGradesBands}>
          <div className="hr-modal" style={{ maxWidth: '90vw', width: '800px' }} onClick={(e) => e.stopPropagation()}>
            <h2>Manage Grades & Bands – {manageStructureName}</h2>
            {manageLoading && <p style={{ color: '#666' }}>Loading…</p>}

            {/* Add Grade */}
            <section style={{ marginBottom: '1.5rem', padding: '1rem', border: '1px solid #eee', borderRadius: '6px' }}>
              <h3 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Add Grade</h3>
              <form onSubmit={handleAddGrade} style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'flex-end' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Code *</label>
                  <input
                    type="text"
                    value={addGradeForm.code}
                    onChange={(e) => setAddGradeForm({ ...addGradeForm, code: e.target.value })}
                    placeholder="e.g. G1"
                    style={{ width: '100px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Name</label>
                  <input
                    type="text"
                    value={addGradeForm.name}
                    onChange={(e) => setAddGradeForm({ ...addGradeForm, name: e.target.value })}
                    placeholder="Grade name"
                    style={{ width: '140px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Order</label>
                  <input
                    type="number"
                    min={0}
                    value={addGradeForm.displayOrder}
                    onChange={(e) => setAddGradeForm({ ...addGradeForm, displayOrder: parseInt(e.target.value, 10) || 0 })}
                    style={{ width: '60px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Copy bands from</label>
                  <select
                    value={addGradeForm.copyBandsFromGradeId}
                    onChange={(e) => setAddGradeForm({ ...addGradeForm, copyBandsFromGradeId: e.target.value })}
                    style={{ minWidth: '160px', padding: '4px 8px' }}
                  >
                    <option value="">— None —</option>
                    {gradesList.map((g: any) => (
                      <option key={g.salaryGradeId} value={g.salaryGradeId}>
                        {g.code || g.name}
                      </option>
                    ))}
                  </select>
                </div>
                <button type="submit" className="btn-primary" disabled={manageLoading}>Add Grade</button>
              </form>
              {addGradeError && <div className="error-message" style={{ marginTop: '0.5rem' }}>{addGradeError}</div>}
            </section>

            {/* Grades list with bands */}
            <section>
              <h3 style={{ marginBottom: '0.75rem' }}>Grades & Bands</h3>
              {gradesList.length === 0 && !manageLoading && (
                <p style={{ color: '#666' }}>No grades yet. Add a grade above.</p>
              )}
              {gradesList.map((grade: any) => (
                <div key={grade.salaryGradeId} style={{ marginBottom: '1.25rem', border: '1px solid #ddd', borderRadius: '6px', overflow: 'hidden' }}>
                  <div style={{ padding: '0.75rem 1rem', background: '#f6f6f6', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {editingGradeId === grade.salaryGradeId ? (
                      <form onSubmit={handleUpdateGrade} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                        <input
                          type="text"
                          value={editGradeForm.name}
                          onChange={(e) => setEditGradeForm({ ...editGradeForm, name: e.target.value })}
                          placeholder="Name"
                          style={{ width: '120px' }}
                        />
                        <input
                          type="number"
                          min={0}
                          value={editGradeForm.displayOrder}
                          onChange={(e) => setEditGradeForm({ ...editGradeForm, displayOrder: parseInt(e.target.value, 10) || 0 })}
                          style={{ width: '50px' }}
                        />
                        <button type="submit" className="btn-primary" style={{ padding: '0.2rem 0.5rem' }}>Save</button>
                        <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.5rem' }} onClick={() => setEditingGradeId(null)}>Cancel</button>
                      </form>
                    ) : (
                      <>
                        <strong>{grade.name || grade.code}</strong> <code>{grade.code}</code> (order: {grade.displayOrder ?? 0})
                        {headcountForGrade(manageStructureId, grade.salaryGradeId) === 0 && (
                          <span style={{ color: '#b45309', fontSize: '0.8rem', marginLeft: '0.5rem' }}>
                            SS-22: no employees on this grade
                          </span>
                        )}
                        <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem' }} onClick={() => startEditGrade(grade)}>Edit</button>
                      </>
                    )}
                  </div>
                  <div style={{ padding: '0.75rem 1rem' }}>
                    {/* Add Band for this grade */}
                    {addBandGradeId === grade.salaryGradeId ? (
                      <form onSubmit={handleAddBand} style={{ marginBottom: '0.75rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(100px, 1fr))', gap: '0.5rem', alignItems: 'flex-end' }}>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Min</label>
                          <input type="number" step="any" min={0} value={addBandForm.minimumAmount} onChange={(e) => setAddBandForm({ ...addBandForm, minimumAmount: e.target.value })} placeholder="Min" required />
                        </div>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Max</label>
                          <input type="number" step="any" min={0} value={addBandForm.maximumAmount} onChange={(e) => setAddBandForm({ ...addBandForm, maximumAmount: e.target.value })} placeholder="Max" required />
                        </div>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Mid</label>
                          <input type="number" step="any" value={addBandForm.midPoint} onChange={(e) => setAddBandForm({ ...addBandForm, midPoint: e.target.value })} placeholder="Mid" />
                        </div>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Name</label>
                          <input type="text" value={addBandForm.name} onChange={(e) => setAddBandForm({ ...addBandForm, name: e.target.value })} placeholder="Band name" />
                        </div>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Code</label>
                          <input type="text" value={addBandForm.code} onChange={(e) => setAddBandForm({ ...addBandForm, code: e.target.value })} placeholder="Code" />
                        </div>
                        <div>
                          <label style={{ fontSize: '0.75rem' }}>Order</label>
                          <input type="number" min={0} value={addBandForm.displayOrder} onChange={(e) => setAddBandForm({ ...addBandForm, displayOrder: parseInt(e.target.value, 10) || 0 })} style={{ width: '50px' }} />
                        </div>
                        <button type="submit" className="btn-primary" style={{ padding: '0.2rem 0.5rem' }}>Add Band</button>
                        <button type="button" className="btn-secondary" style={{ padding: '0.2rem 0.5rem' }} onClick={() => { setAddBandGradeId(null); setAddBandError(null); }}>Cancel</button>
                      </form>
                    ) : (
                      <button type="button" className="btn-secondary" style={{ marginBottom: '0.5rem', padding: '0.2rem 0.5rem', fontSize: '0.8rem' }} onClick={() => { setAddBandGradeId(grade.salaryGradeId); setAddBandForm({ minimumAmount: '', maximumAmount: '', midPoint: '', name: '', code: '', displayOrder: (bandsByGradeId[grade.salaryGradeId]?.length ?? 0) }); setAddBandError(null); }}>+ Add Band</button>
                    )}
                    {addBandGradeId === grade.salaryGradeId && addBandError && <div className="error-message" style={{ marginBottom: '0.5rem' }}>{addBandError}</div>}
                    {/* Bands list */}
                    <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
                      {(bandsByGradeId[grade.salaryGradeId] || []).map((band: any) => (
                        <li key={band.salaryBandId} style={{ marginBottom: '0.35rem' }}>
                          {editingBandId === band.salaryBandId ? (
                            <form onSubmit={handleUpdateBand} style={{ display: 'inline-flex', flexWrap: 'wrap', gap: '0.35rem', alignItems: 'center' }}>
                              <input type="number" step="any" size={6} value={editBandForm.minimumAmount} onChange={(e) => setEditBandForm({ ...editBandForm, minimumAmount: e.target.value })} placeholder="Min" />
                              <input type="number" step="any" size={6} value={editBandForm.maximumAmount} onChange={(e) => setEditBandForm({ ...editBandForm, maximumAmount: e.target.value })} placeholder="Max" />
                              <input type="number" step="any" size={6} value={editBandForm.midPoint} onChange={(e) => setEditBandForm({ ...editBandForm, midPoint: e.target.value })} placeholder="Mid" />
                              <input type="text" size={8} value={editBandForm.name} onChange={(e) => setEditBandForm({ ...editBandForm, name: e.target.value })} placeholder="Name" />
                              <button type="submit" className="btn-primary" style={{ padding: '0.15rem 0.4rem', fontSize: '0.8rem' }}>Save</button>
                              <button type="button" className="btn-secondary" style={{ padding: '0.15rem 0.4rem', fontSize: '0.8rem' }} onClick={cancelEditBand}>Cancel</button>
                            </form>
                          ) : (
                            <>
                              {band.minimumAmount} – {band.midPoint != null ? `${band.midPoint} – ` : ''}{band.maximumAmount}
                              {band.name ? ` (${band.name})` : ''}
                              {headcountForBand(manageStructureId, grade.salaryGradeId, band.salaryBandId) === 0 && (
                                <span style={{ color: '#b45309', fontSize: '0.75rem', marginLeft: '0.35rem' }}>
                                  SS-22: no employees in band
                                </span>
                              )}
                              <button type="button" className="btn-secondary" style={{ marginLeft: '0.5rem', padding: '0.15rem 0.4rem', fontSize: '0.8rem' }} onClick={() => startEditBand(band, grade.salaryGradeId)}>Edit</button>
                            </>
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              ))}
            </section>

            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn-secondary" onClick={closeManageGradesBands}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* ES-21: Bulk salary revision */}
      {showBulkRevisionModal && currentOrganizationId && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          onClick={() => {
            setShowBulkRevisionModal(false);
            setBulkRevisionError(null);
          }}
        >
          <div className="hr-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px', maxHeight: '90vh', overflow: 'auto' }}>
            <h2>Bulk salary revision</h2>
            <p style={{ color: '#666', fontSize: '0.9rem' }}>
              Create a pending request to increase a component amount by a percentage for all employees on a grade or structure.
              Approve to apply new effective-dated salary detail rows (ES-21).
            </p>
            <form onSubmit={submitBulkRevision}>
              <div className="form-row">
                <label>Target</label>
                <select
                  value={bulkRevisionForm.targetType}
                  onChange={(e) => {
                    const targetType = e.target.value as 'BY_GRADE' | 'BY_STRUCTURE';
                    setBulkRevisionForm((prev) => ({
                      ...prev,
                      targetType,
                      targetStructureId: '',
                      targetGradeId: '',
                    }));
                    setBulkRevisionStructureForGrade('');
                    setBulkRevisionGrades([]);
                  }}
                >
                  <option value="BY_GRADE">By grade (employees on this grade)</option>
                  <option value="BY_STRUCTURE">By structure (employees on this structure)</option>
                </select>
              </div>
              {bulkRevisionForm.targetType === 'BY_STRUCTURE' ? (
                <div className="form-row">
                  <label>Salary structure *</label>
                  <select
                    value={bulkRevisionForm.targetStructureId}
                    onChange={(e) =>
                      setBulkRevisionForm((prev) => ({ ...prev, targetStructureId: e.target.value }))
                    }
                    required
                  >
                    <option value="">-- Select --</option>
                    {structures.filter((s: any) => s.isActive).map((s: any) => (
                      <option key={s.salaryStructureId} value={s.salaryStructureId}>
                        {s.structureName} {s.code ? `(${s.code})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <>
                  <div className="form-row">
                    <label>Structure (to pick grade) *</label>
                    <select
                      value={bulkRevisionStructureForGrade}
                      onChange={async (e) => {
                        const sid = e.target.value;
                        setBulkRevisionStructureForGrade(sid);
                        setBulkRevisionForm((prev) => ({ ...prev, targetGradeId: '', targetStructureId: '' }));
                        await loadBulkRevisionGrades(sid);
                      }}
                      required
                    >
                      <option value="">-- Select structure --</option>
                      {structures.filter((s: any) => s.isActive).map((s: any) => (
                        <option key={s.salaryStructureId} value={s.salaryStructureId}>
                          {s.structureName} {s.code ? `(${s.code})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-row">
                    <label>Grade *</label>
                    <select
                      value={bulkRevisionForm.targetGradeId}
                      onChange={(e) =>
                        setBulkRevisionForm((prev) => ({ ...prev, targetGradeId: e.target.value }))
                      }
                      required
                      disabled={!bulkRevisionStructureForGrade || bulkRevisionGrades.length === 0}
                    >
                      <option value="">
                        {!bulkRevisionStructureForGrade
                          ? '-- Select structure first --'
                          : bulkRevisionGrades.length === 0
                            ? '-- No grades --'
                            : '-- Select grade --'}
                      </option>
                      {bulkRevisionGrades.map((g: any) => (
                        <option key={g.salaryGradeId} value={g.salaryGradeId}>
                          {g.name || g.code} ({g.code})
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}
              <div className="form-row">
                <label>Component code *</label>
                <input
                  type="text"
                  value={bulkRevisionForm.componentCode}
                  onChange={(e) =>
                    setBulkRevisionForm((prev) => ({ ...prev, componentCode: e.target.value }))
                  }
                  placeholder="e.g. BASIC"
                  required
                />
              </div>
              <div className="form-row">
                <label>Increase (%)</label>
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={bulkRevisionForm.percentageValue}
                  onChange={(e) =>
                    setBulkRevisionForm((prev) => ({ ...prev, percentageValue: e.target.value }))
                  }
                  required
                />
              </div>
              <div className="form-row">
                <label>Effective from *</label>
                <input
                  type="date"
                  value={bulkRevisionForm.effectiveFrom}
                  onChange={(e) =>
                    setBulkRevisionForm((prev) => ({ ...prev, effectiveFrom: e.target.value }))
                  }
                  required
                />
              </div>
              <div className="form-row">
                <label>Comment</label>
                <input
                  type="text"
                  value={bulkRevisionForm.comment}
                  onChange={(e) =>
                    setBulkRevisionForm((prev) => ({ ...prev, comment: e.target.value }))
                  }
                  placeholder="Optional note for audit"
                />
              </div>
              {bulkRevisionError && <div className="error-message" style={{ marginBottom: '0.75rem' }}>{bulkRevisionError}</div>}
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowBulkRevisionModal(false)}>
                  Close
                </button>
                <button type="submit" className="btn-primary" disabled={bulkRevisionLoading}>
                  {bulkRevisionLoading ? 'Saving…' : 'Create request'}
                </button>
              </div>
            </form>
            <hr style={{ margin: '1rem 0' }} />
            <h3 style={{ marginTop: 0 }}>Pending approvals</h3>
            {bulkRevisionLoading && bulkRevisionList.length === 0 ? (
              <p style={{ color: '#666' }}>Loading…</p>
            ) : bulkRevisionList.length === 0 ? (
              <p style={{ color: '#666' }}>No pending bulk revisions.</p>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {bulkRevisionList.map((br: any) => (
                  <li
                    key={br.bulkRevisionId}
                    style={{
                      border: '1px solid #ddd',
                      borderRadius: 6,
                      padding: '0.75rem',
                      marginBottom: '0.5rem',
                      fontSize: '0.9rem',
                    }}
                  >
                    <div>
                      <strong>{br.componentCode}</strong> +{br.percentageValue}% effective {br.effectiveFrom} ·{' '}
                      {br.targetType === 'BY_GRADE' ? 'By grade' : 'By structure'}
                    </div>
                    <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        className="btn-primary"
                        style={{ padding: '0.25rem 0.6rem', fontSize: '0.85rem' }}
                        disabled={bulkRevisionLoading || !user?.id}
                        onClick={async () => {
                          if (!br.bulkRevisionId || !user?.id) return;
                          try {
                            setBulkRevisionLoading(true);
                            await approveSalaryBulkRevision(br.bulkRevisionId, user.id);
                            const res = await listSalaryBulkRevisions(currentOrganizationId!, 'PENDING');
                            setBulkRevisionList(Array.isArray(res.data) ? res.data : []);
                            await loadData();
                            alert('Bulk revision applied.');
                          } catch (err: any) {
                            alert(err?.response?.data?.message || err?.message || 'Approve failed.');
                          } finally {
                            setBulkRevisionLoading(false);
                          }
                        }}
                      >
                        Approve &amp; apply
                      </button>
                      <button
                        type="button"
                        className="btn-secondary"
                        style={{ padding: '0.25rem 0.6rem', fontSize: '0.85rem' }}
                        disabled={bulkRevisionLoading}
                        onClick={async () => {
                          if (!br.bulkRevisionId) return;
                          const reason = window.prompt('Rejection reason?', 'Rejected');
                          if (reason === null) return;
                          try {
                            setBulkRevisionLoading(true);
                            await rejectSalaryBulkRevision(br.bulkRevisionId, reason || 'Rejected');
                            const res = await listSalaryBulkRevisions(currentOrganizationId!, 'PENDING');
                            setBulkRevisionList(Array.isArray(res.data) ? res.data : []);
                          } catch (err: any) {
                            alert(err?.response?.data?.message || err?.message || 'Reject failed.');
                          } finally {
                            setBulkRevisionLoading(false);
                          }
                        }}
                      >
                        Reject
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* ES-38, ES-42, ES-45: Employee Salary side panel (assignment + components + history) */}
      {showEmployeePanel && selectedEmployeeId && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ alignItems: 'stretch', justifyContent: 'flex-end' }}
          onClick={() => {
            setShowEmployeePanel(false);
            setSelectedEmployeeId(null);
            setEmployeePanelDetails([]);
            setEmployeeAssignment(null);
            setEmployeeRevisionHistory([]);
            setEmployeePanelError(null);
          }}
        >
          <div
            className="hr-modal"
            style={{
              maxWidth: '480px',
              width: '100%',
              height: '100%',
              margin: 0,
              borderRadius: 0,
              display: 'flex',
              flexDirection: 'column',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <h2 style={{ margin: 0 }}>Employee Salary – {selectedEmployeeName}</h2>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  setShowEmployeePanel(false);
                  setSelectedEmployeeId(null);
                  setEmployeePanelDetails([]);
                  setEmployeeAssignment(null);
                  setEmployeeRevisionHistory([]);
                  setEmployeePanelError(null);
                }}
              >
                Close
              </button>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <button
                type="button"
                className={employeePanelTab === 'current' ? 'tab active' : 'tab'}
                onClick={() => setEmployeePanelTab('current')}
              >
                Current
              </button>
              <button
                type="button"
                className={employeePanelTab === 'history' ? 'tab active' : 'tab'}
                onClick={() => setEmployeePanelTab('history')}
              >
                History
              </button>
            <button
              type="button"
              className={employeePanelTab === 'payslip' ? 'tab active' : 'tab'}
              onClick={() => setEmployeePanelTab('payslip')}
            >
              Payslip
            </button>
            </div>

            {employeePanelLoading ? (
              <p style={{ color: '#666' }}>Loading…</p>
            ) : employeePanelError && employeePanelTab !== 'payslip' ? (
              <div className="error-message" style={{ marginBottom: '0.75rem' }}>{employeePanelError}</div>
            ) : employeePanelTab === 'current' ? (
              <div style={{ flex: 1, overflowY: 'auto' }}>
                <section style={{ marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
                    <h3 style={{ marginTop: 0 }}>Current Assignment</h3>
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={openAssignmentModal}
                    >
                      Manage Assignment
                    </button>
                  </div>
                  {employeeAssignment ? (
                    <div style={{ fontSize: '0.9rem', lineHeight: 1.5 }}>
                      <div>
                        <strong>Structure:</strong>{' '}
                        {employeeAssignment.structureName || employeeAssignment.salaryStructureId}
                      </div>
                      <div>
                        <strong>Grade:</strong>{' '}
                        {employeeAssignment.gradeName || employeeAssignment.salaryGradeId}
                      </div>
                      <div>
                        <strong>Band:</strong>{' '}
                        {employeeAssignment.bandName || employeeAssignment.salaryBandId || '—'}
                      </div>
                      <div>
                        <strong>Effective:</strong>{' '}
                        {employeeAssignment.effectiveFrom
                          ? new Date(employeeAssignment.effectiveFrom).toLocaleDateString()
                          : '—'}{' '}
                        –{' '}
                        {employeeAssignment.effectiveTo
                          ? new Date(employeeAssignment.effectiveTo).toLocaleDateString()
                          : 'Open'}
                      </div>
                    </div>
                  ) : (
                    <p style={{ color: '#666' }}>No active salary assignment found for this employee on the selected date.</p>
                  )}
                </section>

                <section>
                  <h3 style={{ marginTop: 0 }}>Components</h3>
                  {employeePanelDetails.length === 0 ? (
                    <p style={{ color: '#666' }}>No salary components found for this employee on the selected date.</p>
                  ) : (
                    <table className="hr-table">
                      <thead>
                        <tr>
                          <th>Component</th>
                          <th>Value type</th>
                          <th>Amount / %</th>
                          <th>Effective</th>
                        </tr>
                      </thead>
                      <tbody>
                        {employeePanelDetails.map((d: any) => {
                          const comp = salaryComponents.find((c: any) => c.componentId === d.componentId);
                          const compName = comp ? (comp.componentName || comp.code || d.componentId) : d.componentId;
                          const valueDisplay =
                            d.valueType === 'AMOUNT' && d.amount != null
                              ? formatCurrency(Number(d.amount))
                              : d.valueType === 'PERCENTAGE' && d.percentage != null
                                ? `${d.percentage}%`
                                : d.valueType === 'USE_MASTER_DEFAULT'
                                  ? 'Use default'
                                  : '—';
                          return (
                            <tr key={d.salaryDetailId}>
                              <td>{compName}</td>
                              <td>{d.valueType || 'AMOUNT'}</td>
                              <td>{valueDisplay}</td>
                              <td>
                                {d.effectiveFrom
                                  ? new Date(d.effectiveFrom).toLocaleDateString()
                                  : '—'}{' '}
                                –{' '}
                                {d.effectiveTo
                                  ? new Date(d.effectiveTo).toLocaleDateString()
                                  : 'Open'}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </section>
              </div>
            ) : employeePanelTab === 'history' ? (
              <div style={{ flex: 1, overflowY: 'auto' }}>
                <section>
                  <h3 style={{ marginTop: 0 }}>Revision history</h3>
                  {employeeRevisionHistory.length === 0 ? (
                    <p style={{ color: '#666' }}>No revision history recorded yet for this employee.</p>
                  ) : (
                    <table className="hr-table">
                      <thead>
                        <tr>
                          <th>Kind</th>
                          <th>Effective</th>
                          <th>Type</th>
                          <th>Reason</th>
                          <th>Summary</th>
                        </tr>
                      </thead>
                      <tbody>
                        {employeeRevisionHistory.map((item: any) => (
                          <tr key={item.id}>
                            <td>{item.kind}</td>
                            <td>
                              {item.effectiveFrom
                                ? new Date(item.effectiveFrom).toLocaleDateString()
                                : '—'}{' '}
                              –{' '}
                              {item.effectiveTo
                                ? new Date(item.effectiveTo).toLocaleDateString()
                                : 'Open'}
                            </td>
                            <td>{item.revisionType || '—'}</td>
                            <td>{item.revisionReason || '—'}</td>
                            <td>{item.summary || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </section>
              </div>
            ) : (
              <div style={{ flex: 1, overflowY: 'auto' }}>
                <section>
                  <h3 style={{ marginTop: 0 }}>Payslip view</h3>
                  <div
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8,
                      maxWidth: 520,
                      width: '100%',
                    }}
                  >
                    <label htmlFor="payslip-payroll-run">Payroll run</label>
                    <Autocomplete
                      id="payslip-payroll-run"
                      size="small"
                      loading={payslipRunsLoading}
                      openOnFocus
                      options={payslipPayrollRuns}
                      getOptionLabel={(run) => formatPayrollRunOptionLabel(run)}
                      isOptionEqualToValue={(a, b) =>
                        getPayrollRunIdStr(a) === getPayrollRunIdStr(b)
                      }
                      value={
                        payslipPayrollRuns.find((r) => getPayrollRunIdStr(r) === payslipRunId) ||
                        null
                      }
                      onChange={(_, run) => {
                        setPayslipRunId(run ? getPayrollRunIdStr(run) : '');
                        setPayslip(null);
                        setPayslipError(null);
                      }}
                      filterOptions={filterPayrollRunOptions}
                      componentsProps={{
                        popper: {
                          sx: { zIndex: 2200 },
                          placement: 'bottom-start',
                        },
                      }}
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          placeholder="Type to search (e.g. Jan) or click to list runs…"
                          helperText={
                            payslipRunsLoading
                              ? 'Loading payroll runs…'
                              : payslipPayrollRuns.length === 0
                                ? 'No payroll runs in this organization yet.'
                                : 'Search by run name, dates, status, or run ID.'
                          }
                        />
                      )}
                    />
                  </div>
                  <div className="form-actions" style={{ marginBottom: '1rem' }}>
                    <button type="button" className="btn-secondary" onClick={() => { setPayslipRunId(''); setPayslip(null); setPayslipError(null); }}>
                      Clear
                    </button>
                    <button type="button" className="btn-primary" onClick={handleLoadPayslip} disabled={payslipLoading}>
                      {payslipLoading ? 'Loading…' : 'Load payslip'}
                    </button>
                    {payslip && (
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => window.print()}
                        style={{ marginLeft: '0.5rem' }}
                      >
                        Print / Save as PDF
                      </button>
                    )}
                  </div>
                  {payslipError && (
                    <div className="error-message" style={{ marginBottom: '0.75rem' }}>{payslipError}</div>
                  )}
                  {payslip && (
                    <div style={{ fontSize: '0.9rem' }}>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <div><strong>Employee:</strong> {payslip.employeeName || selectedEmployeeName} ({payslip.employeeNumber || selectedEmployeeId})</div>
                        <div><strong>Basic:</strong> {formatCurrency(Number(payslip.basicSalary || 0))}</div>
                        <div><strong>Gross:</strong> {formatCurrency(Number(payslip.grossSalary || 0))}</div>
                        <div><strong>Total deductions:</strong> {formatCurrency(Number(payslip.totalDeductions || 0))}</div>
                        <div><strong>Net pay:</strong> {formatCurrency(Number(payslip.netSalary || 0))}</div>
                        {payslip.periodTaxableGross != null && payslip.periodTaxableGross !== '' && (
                          <div>
                            <strong>Taxable gross (period):</strong>{' '}
                            {formatCurrency(Number(payslip.periodTaxableGross))}
                          </div>
                        )}
                        {(payslip.yearToDateGross != null ||
                          payslip.yearToDateDeductions != null ||
                          payslip.yearToDateNet != null ||
                          payslip.yearToDateIncomeTaxWithheld != null) && (
                          <div style={{ marginTop: '0.65rem', paddingTop: '0.65rem', borderTop: '1px solid rgba(0,0,0,0.12)' }}>
                            <div style={{ fontWeight: 600, marginBottom: 4 }}>Year to date (calendar year)</div>
                            {payslip.yearToDateGross != null && payslip.yearToDateGross !== '' && (
                              <div>YTD gross: {formatCurrency(Number(payslip.yearToDateGross))}</div>
                            )}
                            {payslip.yearToDateDeductions != null && payslip.yearToDateDeductions !== '' && (
                              <div>YTD deductions: {formatCurrency(Number(payslip.yearToDateDeductions))}</div>
                            )}
                            {payslip.yearToDateNet != null && payslip.yearToDateNet !== '' && (
                              <div>YTD net: {formatCurrency(Number(payslip.yearToDateNet))}</div>
                            )}
                            {payslip.yearToDateIncomeTaxWithheld != null && payslip.yearToDateIncomeTaxWithheld !== '' && (
                              <div>YTD income tax withheld: {formatCurrency(Number(payslip.yearToDateIncomeTaxWithheld))}</div>
                            )}
                          </div>
                        )}
                        {(payslip.workingDays != null ||
                          payslip.presentDays != null ||
                          payslip.leaveDays != null ||
                          payslip.overtimeHours != null ||
                          payslip.overtimeAmount != null ||
                          payslip.lopDays != null ||
                          payslip.lopAmount != null) && (
                          <div style={{ marginTop: '0.65rem', paddingTop: '0.65rem', borderTop: '1px solid rgba(0,0,0,0.12)' }}>
                            <div style={{ fontWeight: 600, marginBottom: 4 }}>Time and attendance</div>
                            {payslip.workingDays != null && <div>Working days: {payslip.workingDays}</div>}
                            {payslip.presentDays != null && <div>Present days: {payslip.presentDays}</div>}
                            {payslip.leaveDays != null && payslip.leaveDays !== '' && (
                              <div>Paid leave (days): {String(payslip.leaveDays)}</div>
                            )}
                            {payslip.lopDays != null && payslip.lopDays !== '' && (
                              <div>LOP (days): {String(payslip.lopDays)}</div>
                            )}
                            {payslip.lopAmount != null && payslip.lopAmount !== '' && (
                              <div>LOP amount: {formatCurrency(Number(payslip.lopAmount))}</div>
                            )}
                            {payslip.overtimeHours != null && payslip.overtimeHours !== '' && (
                              <div>Overtime hours: {String(payslip.overtimeHours)}</div>
                            )}
                            {payslip.overtimeAmount != null && payslip.overtimeAmount !== '' && (
                              <div>Overtime pay: {formatCurrency(Number(payslip.overtimeAmount))}</div>
                            )}
                          </div>
                        )}
                      </div>
                      <table className="hr-table">
                        <thead>
                          <tr>
                            <th>Component</th>
                            <th>Type</th>
                            <th>Amount</th>
                            <th>Taxability</th>
                            <th>Statutory</th>
                            <th>PF wage</th>
                            <th>ESI wage</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(payslip.lines || []).map((line: any, idx: number) => (
                            <tr key={idx}>
                              <td>{line.componentName || line.componentCode}</td>
                              <td>{line.componentType}</td>
                              <td>{formatCurrency(Number(line.amount || 0))}</td>
                              <td>{line.taxability ?? '—'}</td>
                              <td>{line.statutoryType ?? '—'}</td>
                              <td>{line.includedInPfWage === true ? 'Yes' : line.includedInPfWage === false ? 'No' : '—'}</td>
                              <td>{line.includedInEsiWage === true ? 'Yes' : line.includedInEsiWage === false ? 'No' : '—'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Manage Assignment modal (structure + grade + band) */}
      {showAssignmentModal && portalLayoutOverlay(
        <div
          className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          onClick={() => {
            setShowAssignmentModal(false);
            setAssignmentFormError(null);
          }}
        >
          <div className="hr-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <h2>Manage Salary Assignment</h2>
            <p style={{ color: '#666', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
              Assign structure and grade to this employee. Both are required for payroll.
            </p>
            <div style={{ marginBottom: '1rem' }}>
              <button type="button" className="btn-secondary" onClick={fillAssignmentFromPosition}>
                Fill from position defaults
              </button>
              <span style={{ fontSize: '0.8rem', color: '#666', marginLeft: '0.5rem' }}>
                Uses default structure/grade/band from the employee&apos;s position (INT-28–INT-31).
              </span>
            </div>
            <form onSubmit={handleAssignmentSubmit}>
              <div className="form-row">
                <label>Salary Structure *</label>
                <select
                  value={assignmentForm.salaryStructureId}
                  onChange={(e) => handleAssignmentStructureChange(e.target.value)}
                  required
                >
                  <option value="">-- Select Structure --</option>
                  {structures.filter((s: any) => s.isActive).map((s: any) => (
                    <option key={s.salaryStructureId} value={s.salaryStructureId}>
                      {s.structureName} {s.code ? `(${s.code})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <label>Grade *</label>
                <select
                  value={assignmentForm.salaryGradeId}
                  onChange={(e) => handleAssignmentGradeChange(e.target.value)}
                  required
                  disabled={!assignmentForm.salaryStructureId || assignmentGrades.length === 0}
                >
                  <option value="">
                    {!assignmentForm.salaryStructureId
                      ? '-- Select structure first --'
                      : assignmentGrades.length === 0
                        ? '-- No grades in this structure --'
                        : '-- Select Grade --'}
                  </option>
                  {assignmentGrades.map((g: any) => (
                    <option key={g.salaryGradeId} value={g.salaryGradeId}>
                      {g.name || g.code} {g.code ? `(${g.code})` : ''}
                    </option>
                  ))}
                </select>
                {assignmentForm.salaryStructureId && assignmentGrades.length === 0 && (
                  <small style={{ color: '#c00', marginTop: 4, display: 'block' }}>
                    No grades defined. Go to Salary Structures → Manage (on your structure) → Add Grade.
                  </small>
                )}
              </div>
              <div className="form-row">
                <label>Band (optional)</label>
                <select
                  value={assignmentForm.salaryBandId || ''}
                  onChange={(e) => setAssignmentForm((prev) => ({ ...prev, salaryBandId: e.target.value || '' }))}
                  disabled={!assignmentForm.salaryGradeId}
                >
                  <option value="">-- None --</option>
                  {assignmentBands.map((b: any) => (
                    <option key={b.salaryBandId} value={b.salaryBandId}>
                      {b.name || b.code || `${b.minimumAmount}–${b.maximumAmount}`}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <label>Effective From *</label>
                <input
                  type="date"
                  value={assignmentForm.effectiveFrom}
                  onChange={(e) => setAssignmentForm((prev) => ({ ...prev, effectiveFrom: e.target.value }))}
                  required
                />
              </div>
              <div className="form-row">
                <label>Effective To</label>
                <input
                  type="date"
                  value={assignmentForm.effectiveTo}
                  onChange={(e) => setAssignmentForm((prev) => ({ ...prev, effectiveTo: e.target.value }))}
                  placeholder="Leave blank for open-ended"
                />
              </div>
              {assignmentFormError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>{assignmentFormError}</div>
              )}
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowAssignmentModal(false); setAssignmentFormError(null); }}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Save Assignment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SS-29: Revision history modal */}
      {showRevisionModal && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => { setShowRevisionModal(false); setRevisionHistory([]); }}>
          <div className="hr-modal" style={{ maxWidth: '90vw' }} onClick={(e) => e.stopPropagation()}>
            <h2>Revision History – {revisionStructureName}</h2>
            {revisionHistory.length > 0 ? (
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>When</th>
                    <th>By</th>
                    <th>Action</th>
                    <th>Old / New</th>
                  </tr>
                </thead>
                <tbody>
                  {revisionHistory.map((entry: any) => (
                    <tr key={entry.auditId}>
                      <td>{entry.performedAt ? new Date(entry.performedAt).toLocaleString() : '-'}</td>
                      <td>{entry.performedBy || '-'}</td>
                      <td>{entry.action || '-'}</td>
                      <td style={{ fontSize: '0.85rem', maxWidth: '20rem', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {entry.oldValues && <div title={entry.oldValues}>Old: {String(entry.oldValues).slice(0, 60)}…</div>}
                        {entry.newValues && <div title={entry.newValues}>New: {String(entry.newValues).slice(0, 60)}…</div>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p style={{ color: '#666' }}>No revision history recorded yet.</p>
            )}
            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn-secondary" onClick={() => { setShowRevisionModal(false); setRevisionHistory([]); }}>Close</button>
            </div>
          </div>
        </div>
      )}

      {/* SC-49: Component dependency report modal */}
      {showDependencyModal && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => { setShowDependencyModal(false); setComponentDependencies([]); }}>
          <div className="hr-modal" style={{ maxWidth: '90vw' }} onClick={(e) => e.stopPropagation()}>
            <h2>Component dependency report</h2>
            <p style={{ color: '#666', marginBottom: '1rem' }}>Components that reference other components (base component or formula).</p>
            {dependencyLoading ? (
              <p>Loading…</p>
            ) : componentDependencies.length > 0 ? (
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Component (code)</th>
                    <th>Component name</th>
                    <th>Dependency type</th>
                    <th>References (code)</th>
                    <th>References (name)</th>
                  </tr>
                </thead>
                <tbody>
                  {componentDependencies.map((d: any, i: number) => (
                    <tr key={i}>
                      <td><code>{d.componentCode ?? '—'}</code></td>
                      <td>{d.componentName ?? '—'}</td>
                      <td>{d.dependencyType ?? '—'}</td>
                      <td><code>{d.referencedCode ?? '—'}</code></td>
                      <td>{d.referencedName ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p style={{ color: '#666' }}>No dependencies found (no percentage base or formula references).</p>
            )}
            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn-secondary" onClick={() => { setShowDependencyModal(false); setComponentDependencies([]); }}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SalaryStructureManager;

