import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Button, Card, CardContent, Grid, Typography, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select,
  FormControl, InputLabel, Divider, Alert, Tooltip, Collapse, List, ListItem, ListItemText,
  Tabs, Tab, TextField
} from '@mui/material';
import {
  Calculate as CalculateIcon,
  Visibility as ViewIcon,
  Payments as PaymentsIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  People as PeopleIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import pharmaService, { IncentiveCalculation, Territory, IncentiveDistribution } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';
import { useSnackbar } from 'notistack';

const MONTHS = [
  { id: 0, name: 'All Months' },
  { id: 1, name: 'January' },
  { id: 2, name: 'February' },
  { id: 3, name: 'March' },
  { id: 4, name: 'April' },
  { id: 5, name: 'May' },
  { id: 6, name: 'June' },
  { id: 7, name: 'July' },
  { id: 8, name: 'August' },
  { id: 9, name: 'September' },
  { id: 10, name: 'October' },
  { id: 11, name: 'November' },
  { id: 12, name: 'December' }
];

const IncentiveManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [selectedTerritory, setSelectedTerritory] = useState<string>('');
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(0);
  const [calculations, setCalculations] = useState<IncentiveCalculation[]>([]);
  const [employeeTotals, setEmployeeTotals] = useState<Map<string, { total: number; territories: string[] }>>(new Map());
  const [employeeWiseCalculation, setEmployeeWiseCalculation] = useState<IncentiveCalculation | null>(null);
  const [loading, setLoading] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const loadEmployeeWiseForMonth = useCallback(async () => {
    if (!selectedTerritory || selectedMonth < 1) return;
    try {
      setLoading(true);
      const calc = await pharmaService.getIncentiveByTerritoryAndMonth(selectedTerritory, selectedYear, selectedMonth);
      setEmployeeWiseCalculation(calc);
    } catch (error) {
      console.error('Failed to load employee-wise incentive:', error);
      setEmployeeWiseCalculation(null);
    } finally {
      setLoading(false);
    }
  }, [selectedTerritory, selectedYear, selectedMonth]);

  useEffect(() => {
    if (currentOrganizationId) {
      loadTerritories();
      loadEmployees();
    }
  }, [currentOrganizationId]);

  useEffect(() => {
    if (selectedTerritory && selectedYear && tabValue === 0) {
      loadCalculations(selectedTerritory, selectedYear);
    }
  }, [selectedTerritory, selectedYear, tabValue]);

  useEffect(() => {
    if (tabValue === 1 && currentOrganizationId && territories.length > 0) {
      if (selectedMonth > 0 && selectedTerritory) {
        loadEmployeeWiseForMonth();
      } else {
        loadEmployeeTotals();
      }
    }
  }, [tabValue, selectedYear, selectedMonth, selectedTerritory, currentOrganizationId, territories, loadEmployeeWiseForMonth]);

  const loadTerritories = async () => {
    try {
      const data = await pharmaService.getAllTerritoriesForOrganization(currentOrganizationId);
      setTerritories(data);
      if (data.length > 0) {
        setSelectedTerritory(data[0].id);
      }
    } catch (error) {
      console.error('Failed to load territories:', error);
    }
  };

  const loadEmployees = async () => {
    try {
      const res = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
      setEmployees(res.data || []);
    } catch (error) {
      console.error('Failed to load employees:', error);
    }
  };

  const loadEmployeeTotals = async () => {
    if (!currentOrganizationId || territories.length === 0) return;
    try {
      setLoading(true);
      const totalsMap = new Map<string, { total: number; territories: string[] }>();
      
      for (const territory of territories) {
        try {
          const territoryCalculations = await pharmaService.getCalculationsByTerritory(territory.id, selectedYear);
          for (const calc of territoryCalculations) {
            if (calc.distributions && calc.territoryEligible) {
              for (const dist of calc.distributions) {
                if (dist.employeeId) {
                  const existing = totalsMap.get(dist.employeeId) || { total: 0, territories: [] };
                  existing.total += dist.incentiveAmount || 0;
                  if (!existing.territories.includes(territory.name)) {
                    existing.territories.push(territory.name);
                  }
                  totalsMap.set(dist.employeeId, existing);
                }
              }
            }
          }
        } catch (error) {
          console.error(`Failed to load calculations for territory ${territory.name}:`, error);
        }
      }
      
      setEmployeeTotals(totalsMap);
    } catch (error) {
      console.error('Failed to load employee totals:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadCalculations = async (territoryId: string, year: number) => {
    try {
      setLoading(true);
      const data = await pharmaService.getCalculationsByTerritory(territoryId, year);
      setCalculations(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load calculations:', error);
      setCalculations([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCalculate = async () => {
    if (!selectedTerritory || !selectedYear || !selectedMonth) {
      enqueueSnackbar('Please select territory, year and month', { variant: 'warning' });
      return;
    }

    try {
      setLoading(true);
      await pharmaService.calculateIncentive(selectedTerritory, selectedYear, selectedMonth);
      enqueueSnackbar('Incentive calculated successfully', { variant: 'success' });
      loadCalculations(selectedTerritory, selectedYear);
      if (tabValue === 1 && selectedMonth > 0) {
        loadEmployeeWiseForMonth();
      }
    } catch (error: any) {
      console.error('Failed to calculate incentive:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to calculate incentive', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleMarkPaid = async (id: string) => {
    if (!window.confirm('Are you sure you want to mark this incentive as paid?')) return;
    try {
      await pharmaService.markIncentivesAsPaid(id);
      enqueueSnackbar('Incentive marked as paid', { variant: 'success' });
      loadCalculations(selectedTerritory, selectedYear);
    } catch (error: any) {
      console.error('Failed to mark as paid:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to mark as paid', { variant: 'error' });
    }
  };

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const getMonthName = (month: number) => MONTHS.find(m => m.id === month)?.name || month;

  const getEmployeeName = (employeeId: string): string => {
    const employee = employees.find(e => (e.id === employeeId) || (e.employeeId === employeeId));
    if (!employee) return employeeId;
    return employee.name || employee.employeeNumber || employeeId;
  };

  const displayedCalculations = selectedMonth > 0
    ? (calculations ?? []).filter((c) => c != null && Number(c.month) === selectedMonth)
    : (calculations ?? []);

  const employeeTotalsArray = Array.from(employeeTotals.entries())
    .map(([employeeId, data]) => ({
      employeeId,
      employeeName: getEmployeeName(employeeId),
      total: data.total,
      territories: data.territories
    }))
    .sort((a, b) => b.total - a.total);

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Incentive Management</Typography>
      </Box>

      <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ mb: 3 }}>
        <Tab label="Territory-wise Incentives" />
        <Tab label="Employee-wise Totals" icon={<PeopleIcon />} iconPosition="start" />
      </Tabs>
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={3}>
              <TerritoryTreeSelector
                label="Territory"
                value={selectedTerritory}
                onChange={(territoryId) => {
                  setSelectedTerritory(territoryId);
                  loadCalculations(territoryId, selectedYear);
                }}
                required
              />
            </Grid>
            <Grid item xs={12} sm={2}>
              <TextField
                fullWidth
                size="small"
                label="Year"
                type="number"
                value={selectedYear}
                onChange={(e) => {
                  const year = Number(e.target.value);
                  setSelectedYear(year);
                  if (selectedTerritory) loadCalculations(selectedTerritory, year);
                }}
              />
            </Grid>
            <Grid item xs={12} sm={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Month</InputLabel>
                <Select
                  value={selectedMonth}
                  label="Month"
                  onChange={(e) => {
                    const month = Number(e.target.value);
                    setSelectedMonth(month);
                    setExpandedId(null);
                  }}
                >
                  {MONTHS.map((m) => (
                    <MenuItem key={m.id} value={m.id}>{m.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={3}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<CalculateIcon />}
                onClick={handleCalculate}
                disabled={loading || selectedMonth === 0}
              >
                Calculate Incentive
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {tabValue === 0 && (
        <>
          <Alert severity="info" sx={{ mb: 3 }}>
            Incentives are calculated as <strong>4% of total collection</strong>. 
            Eligibility requires achieving the <strong>territory target</strong> and keeping expenses below <strong>30% of target</strong>.
            Distribution: 10% to SRs, then 80% of remaining to MPOs and 20% to Managers.
            If you don&apos;t see expected data, check that you have selected the correct <strong>Territory</strong> and <strong>Year</strong>.
          </Alert>

          <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell width="40px" />
              <TableCell>Month</TableCell>
              <TableCell align="right">Target</TableCell>
              <TableCell align="right">Achievement</TableCell>
              <TableCell align="center">Target Achieved</TableCell>
              <TableCell align="center">Expense OK</TableCell>
              <TableCell align="center">Eligibility</TableCell>
              <TableCell align="right">Total Incentive</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow><TableCell colSpan={10} align="center">Loading...</TableCell></TableRow>
            ) : displayedCalculations.length === 0 ? (
              <TableRow><TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                <Box>
                  <Typography variant="body1" color="text.secondary" gutterBottom>
                    {selectedMonth > 0
                      ? `No calculation found for ${getMonthName(selectedMonth)} ${selectedYear}`
                      : 'No calculations found for this year'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {selectedTerritory && selectedMonth > 0
                      ? 'Click "Calculate Incentive" above to generate the calculation. Ensure the territory has a target, incentive rule, and deposit data for this period.'
                      : 'Select a territory and month, then click "Calculate Incentive" to generate.'}
                  </Typography>
                </Box>
              </TableCell></TableRow>
            ) : (
              displayedCalculations.map((calc) => (
                <React.Fragment key={calc.id}>
                  <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
                    <TableCell>
                      <IconButton size="small" onClick={() => calc.id && toggleExpand(calc.id)}>
                        {expandedId === calc.id ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                      </IconButton>
                    </TableCell>
                    <TableCell>{getMonthName(calc.month)}</TableCell>
                    <TableCell align="right">{(calc.targetAmount ?? 0).toLocaleString()}</TableCell>
                    <TableCell align="right">{(calc.coveredAmount ?? 0).toLocaleString()}</TableCell>
                    <TableCell align="center">
                      {calc.targetAchieved ? <CheckCircleIcon color="success" /> : <CancelIcon color="error" />}
                    </TableCell>
                    <TableCell align="center">
                      {calc.expenseWithinLimit ? <CheckCircleIcon color="success" /> : <CancelIcon color="error" />}
                    </TableCell>
                    <TableCell align="center">
                      <Chip 
                        label={calc.territoryEligible ? 'ELIGIBLE' : 'NOT ELIGIBLE'} 
                        color={calc.territoryEligible ? 'success' : 'error'} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell align="right">{(calc.totalIncentiveDistributed ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                    <TableCell>
                      <Chip label={calc.status} size="small" color={calc.status === 'PAID' ? 'primary' : 'default'} />
                    </TableCell>
                    <TableCell align="center">
                      {calc.status === 'CALCULATED' && calc.territoryEligible && (
                        <Tooltip title="Mark as Paid">
                          <IconButton size="small" color="primary" onClick={() => calc.id && handleMarkPaid(calc.id)}>
                            <PaymentsIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={10}>
                      <Collapse in={expandedId === calc.id} timeout="auto" unmountOnExit>
                        <Box sx={{ margin: 2 }}>
                          <Typography variant="h6" gutterBottom component="div">Distribution Breakdown</Typography>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell>Employee</TableCell>
                                <TableCell>Role</TableCell>
                                <TableCell>Type</TableCell>
                                <TableCell align="right">Amount</TableCell>
                                <TableCell>Status</TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                                {calc.distributions?.map((dist, idx) => (
                                    <TableRow key={idx}>
                                      <TableCell>{dist.employeeId ? getEmployeeName(dist.employeeId) : (dist.distributionType === 'DEVELOPMENT_FUND' ? 'Development Fund' : '-')}</TableCell>
                                  <TableCell><Chip label={dist.roleInTerritory || dist.roleInArea || '-'} size="small" /></TableCell>
                                  <TableCell>{dist.distributionType}</TableCell>
                                  <TableCell align="right">{(dist.incentiveAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                                  <TableCell>{dist.status}</TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </Box>
                      </Collapse>
                    </TableCell>
                  </TableRow>
                </React.Fragment>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
        </>
      )}

      {tabValue === 1 && (
        <>
          <Alert severity="info" sx={{ mb: 3 }}>
            {selectedMonth > 0 ? (
              <>Employee-wise incentive breakdown for selected <strong>Territory</strong>, <strong>{getMonthName(selectedMonth)} {selectedYear}</strong>. Select &quot;All Months&quot; to see yearly totals across all territories.</>
            ) : (
              <>Employee-wise incentive totals across all territories for <strong>{selectedYear}</strong>. Select a specific month to see breakdown for that territory and month.</>
            )}
          </Alert>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Employee</TableCell>
                  {selectedMonth > 0 ? (
                    <>
                      <TableCell>Role</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell align="right">Incentive Amount</TableCell>
                      <TableCell>Status</TableCell>
                    </>
                  ) : (
                    <>
                      <TableCell>Territories</TableCell>
                      <TableCell align="right">Total Incentive</TableCell>
                      <TableCell>Territory Count</TableCell>
                    </>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={selectedMonth > 0 ? 5 : 4} align="center">Loading...</TableCell></TableRow>
                ) : selectedMonth > 0 ? (
                  !employeeWiseCalculation ? (
                    <TableRow><TableCell colSpan={5} align="center">
                      No incentives calculated for {getMonthName(selectedMonth)} {selectedYear}. Select Territory and click Calculate to compute.
                    </TableCell></TableRow>
                  ) : !employeeWiseCalculation.distributions || employeeWiseCalculation.distributions.length === 0 ? (
                    <TableRow>                    <TableCell colSpan={5} align="center">
                      {employeeWiseCalculation.territoryEligible
                        ? 'No employee distributions for this calculation.'
                        : `Territory not eligible for ${getMonthName(selectedMonth)} (target not achieved or expenses over limit).`}
                    </TableCell></TableRow>
                  ) : (
                    employeeWiseCalculation.distributions.map((dist, idx) => (
                      <TableRow key={idx}>
                        <TableCell>
                          <Typography variant="body2" fontWeight="medium">{getEmployeeName(dist.employeeId)}</Typography>
                          <Typography variant="caption" color="text.secondary">{dist.employeeId}</Typography>
                        </TableCell>
                        <TableCell><Chip label={dist.roleInTerritory || dist.roleInArea || '-'} size="small" /></TableCell>
                        <TableCell>{dist.distributionType?.replace('_SHARE', '')}</TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" fontWeight="medium">
                            {(dist.incentiveAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                          </Typography>
                        </TableCell>
                        <TableCell>{dist.status}</TableCell>
                      </TableRow>
                    ))
                  )
                ) : employeeTotalsArray.length === 0 ? (
                  <TableRow><TableCell colSpan={4} align="center">No employee incentives found for {selectedYear}</TableCell></TableRow>
                ) : (
                  employeeTotalsArray.map((item) => (
                    <TableRow key={item.employeeId}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">{item.employeeName}</Typography>
                        <Typography variant="caption" color="text.secondary">{item.employeeId}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {item.territories.map((territory, idx) => (
                            <Chip key={idx} label={territory} size="small" variant="outlined" />
                          ))}
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" fontWeight="medium">
                          {item.total.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip label={item.territories.length} size="small" color="primary" />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Box>
  );
};

export default IncentiveManagement;
