import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Button,
  CircularProgress,
  Alert,
  Chip,
  FormControlLabel,
  Switch,
} from '@mui/material';
import { AccountBalance, Receipt, Visibility, Download } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import {
  getSelfSalarySummary,
  getMyPayslips,
  getPayslip,
  getMyEmployeeProfile,
  getEmployees,
  type Employee,
} from '../../services/hrService';
import './Hr.css';

function resolveEmployeeRecordId(emp: Employee | null | undefined): string | null {
  if (!emp) return null;
  return emp.employeeId ?? emp.id ?? null;
}

/**
 * ES-53, ES-54: Employee self-service – view own salary (structure, grade, components; maskable amounts)
 * and view/download payslips for past periods.
 */
const EmployeeSalarySelfService: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [linkedEmployeeId, setLinkedEmployeeId] = useState<string | null>(null);
  const [employeeLoadError, setEmployeeLoadError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [salarySummary, setSalarySummary] = useState<any>(null);
  const [payslips, setPayslips] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [maskAmounts, setMaskAmounts] = useState(false);
  const [asOfDate, setAsOfDate] = useState<string>(new Date().toISOString().split('T')[0]);

  useEffect(() => {
    if (!currentOrganizationId || !user?.id) {
      setLoading(false);
      setLinkedEmployeeId(null);
      setEmployeeLoadError(!currentOrganizationId ? 'No organization selected.' : null);
      setSalarySummary(null);
      setPayslips([]);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setEmployeeLoadError(null);
      setError(null);

      let empId: string | null = null;
      try {
        const meRes = await getMyEmployeeProfile(currentOrganizationId);
        empId = resolveEmployeeRecordId(meRes.data as Employee | undefined);
      } catch {
        try {
          const employeesRes = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
          const employees = (employeesRes.data || []) as Employee[];
          const match = employees.find((e) => e.userId === user.id);
          empId = resolveEmployeeRecordId(match);
        } catch {
          empId = null;
        }
      }

      if (cancelled) return;

      if (!empId) {
        setLinkedEmployeeId(null);
        setEmployeeLoadError(
          'No employee profile is linked to this user. Please contact your administrator.'
        );
        setSalarySummary(null);
        setPayslips([]);
        setLoading(false);
        return;
      }

      setLinkedEmployeeId(empId);
      setEmployeeLoadError(null);

      try {
        if (tabValue === 0) {
          const res = await getSelfSalarySummary(empId, currentOrganizationId, {
            asOfDate: asOfDate || undefined,
            maskAmounts,
          });
          if (!cancelled) setSalarySummary(res.data ?? null);
        } else {
          const res = await getMyPayslips(empId, currentOrganizationId);
          if (!cancelled) setPayslips(Array.isArray(res.data) ? res.data : []);
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.response?.data?.message || e?.message || 'Failed to load data');
          if (tabValue === 0) setSalarySummary(null);
          else setPayslips([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void run();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId, user?.id, tabValue, maskAmounts, asOfDate]);

  const handleViewPayslip = async (runId: string) => {
    if (!linkedEmployeeId) {
      setError('Employee profile not loaded.');
      return;
    }
    try {
      const res = await getPayslip(runId, linkedEmployeeId);
      const payslip = res.data;
      if (!payslip) return;
      const win = window.open('', '_blank');
      if (!win) return;
      win.document.write(`
        <!DOCTYPE html><html><head><title>Payslip</title></head><body style="font-family:sans-serif;padding:24px">
        <h2>Payslip</h2>
        <p><strong>Employee:</strong> ${payslip.employeeName || ''} (${payslip.employeeNumber || ''})</p>
        <p><strong>Period:</strong> ${payslip.payrollRunId}</p>
        <p><strong>Currency:</strong> ${payslip.currency || ''} | <strong>Pay frequency:</strong> ${payslip.payFrequency || ''}</p>
        ${payslip.periodTaxableGross != null && payslip.periodTaxableGross !== '' ? `<p><strong>Taxable gross (period):</strong> ${payslip.periodTaxableGross}</p>` : ''}
        ${payslip.yearToDateGross != null || payslip.yearToDateNet != null ? `<p><strong>YTD:</strong> gross ${payslip.yearToDateGross ?? '—'} | deductions ${payslip.yearToDateDeductions ?? '—'} | net ${payslip.yearToDateNet ?? '—'} | tax withheld ${payslip.yearToDateIncomeTaxWithheld ?? '—'}</p>` : ''}
        <table border="1" cellpadding="8" style="border-collapse:collapse;width:100%;font-size:12px">
          <thead><tr><th>Component</th><th>Type</th><th>Amount</th><th>Taxability</th><th>Statutory</th><th>PF wage</th><th>ESI wage</th></tr></thead>
          <tbody>
          ${(payslip.lines || []).map((l: any) =>
            `<tr><td>${l.componentName || l.componentCode || ''}</td><td>${l.componentType || ''}</td><td>${l.amount != null ? l.amount : ''}</td><td>${l.taxability ?? ''}</td><td>${l.statutoryType ?? ''}</td><td>${l.includedInPfWage === true ? 'Yes' : l.includedInPfWage === false ? 'No' : ''}</td><td>${l.includedInEsiWage === true ? 'Yes' : l.includedInEsiWage === false ? 'No' : ''}</td></tr>`
          ).join('')}
          </tbody>
        </table>
        <p><strong>Basic:</strong> ${payslip.basicSalary ?? ''} | <strong>Gross:</strong> ${payslip.grossSalary ?? ''} | <strong>Deductions:</strong> ${payslip.totalDeductions ?? ''} | <strong>Net:</strong> ${payslip.netSalary ?? ''}</p>
        <p><button onclick="window.print()">Print / Save as PDF</button></p>
        </body></html>
      `);
      win.document.close();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load payslip');
    }
  };

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" gutterBottom>
        My Salary & Payslips
      </Typography>
      {employeeLoadError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {employeeLoadError}
        </Alert>
      )}
      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ mb: 2 }}>
        <Tab icon={<AccountBalance />} iconPosition="start" label="My Salary" />
        <Tab icon={<Receipt />} iconPosition="start" label="My Payslips" />
      </Tabs>

      {loading ? (
        <Box display="flex" justifyContent="center" p={3}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          {tabValue === 0 && (
            <Card>
              <CardContent>
                {salarySummary ? (
                  <>
                    <Box display="flex" alignItems="center" flexWrap="wrap" gap={2} mb={2}>
                      <FormControlLabel
                        control={
                          <Switch
                            checked={maskAmounts}
                            onChange={(e) => setMaskAmounts(e.target.checked)}
                          />
                        }
                        label="Mask amounts"
                      />
                      <Typography variant="body2" color="text.secondary">
                        As of: {asOfDate}
                      </Typography>
                    </Box>
                    <Typography variant="subtitle1">
                      <strong>Structure:</strong> {salarySummary.structureName || salarySummary.structureCode || '—'} &nbsp;
                      <strong>Grade:</strong> {salarySummary.gradeName || salarySummary.gradeCode || '—'} &nbsp;
                      {salarySummary.bandName || salarySummary.bandCode ? (
                        <><strong>Band:</strong> {salarySummary.bandName || salarySummary.bandCode}</>
                      ) : null}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      {salarySummary.currency && `${salarySummary.currency} · `}
                      {salarySummary.payFrequency && salarySummary.payFrequency}
                    </Typography>
                    <TableContainer sx={{ mt: 2 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Component</TableCell>
                            <TableCell>Type</TableCell>
                            <TableCell align="right">Amount</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {(salarySummary.components || []).map((c: any, i: number) => (
                            <TableRow key={i}>
                              <TableCell>{c.componentName || c.componentCode}</TableCell>
                              <TableCell>{c.componentType}</TableCell>
                              <TableCell align="right">
                                {c.amount != null ? (salarySummary.currency ? `${salarySummary.currency} ` : '') + c.amount : '***'}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </>
                ) : (
                  <Typography color="text.secondary">No salary assignment found for you as of the selected date.</Typography>
                )}
              </CardContent>
            </Card>
          )}

          {tabValue === 1 && (
            <Card>
              <CardContent>
                {payslips.length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Run</TableCell>
                          <TableCell>Period</TableCell>
                          <TableCell>Status</TableCell>
                          <TableCell align="right">Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {payslips.map((p: any) => (
                          <TableRow key={p.payrollRunId}>
                            <TableCell>{p.runName || p.payrollRunId}</TableCell>
                            <TableCell>
                              {p.payPeriodStart} – {p.payPeriodEnd}
                            </TableCell>
                            <TableCell>
                              <Chip size="small" label={p.status || '—'} color={p.status === 'APPROVED' ? 'success' : 'default'} />
                            </TableCell>
                            <TableCell align="right">
                              <Button
                                size="small"
                                startIcon={<Visibility />}
                                onClick={() => handleViewPayslip(p.payrollRunId)}
                              >
                                View
                              </Button>
                              <Button
                                size="small"
                                startIcon={<Download />}
                                onClick={() => handleViewPayslip(p.payrollRunId)}
                              >
                                Download
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography color="text.secondary">No payslips available yet.</Typography>
                )}
              </CardContent>
            </Card>
          )}
        </>
      )}
    </Box>
  );
};

export default EmployeeSalarySelfService;
