import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, CircularProgress, Alert, Dialog,
  DialogTitle, DialogContent, DialogActions, Table,
  TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, Tabs, Tab, MenuItem
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  History as HistoryIcon,
  Warning as WarningIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { getEmployees, Employee } from '../../services/hrService';
import pharmaService, {
  TerritoryIncentiveRule,
  TerritoryIncentiveRuleResponse,
  TerritoryIncentiveRuleRequest,
  TerritoryIncentiveAllocationRequestItem,
  EmployeeTerritoryAssignment,
  Territory
} from '../../services/pharmaService';
import { useSnackbar } from 'notistack';

const isManagerOrMpo = (role: string | undefined): boolean => {
  if (!role) return false;
  if (role === 'MPO') return true;
  return ['AM', 'TM', 'RM', 'DSM', 'ASM', 'SM'].some(r => role.includes(r));
};

const IncentiveRulesManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [selectedTerritory, setSelectedTerritory] = useState<string>('');
  const [currentRule, setCurrentRule] = useState<TerritoryIncentiveRuleResponse | null>(null);
  const [ruleHistory, setRuleHistory] = useState<TerritoryIncentiveRule[]>([]);
  const [territoryAssignments, setTerritoryAssignments] = useState<EmployeeTerritoryAssignment[]>([]);
  const [territoriesWithoutRules, setTerritoriesWithoutRules] = useState<string[]>([]);
  const [showDialog, setShowDialog] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [employees, setEmployees] = useState<Employee[]>([]);

  const [formData, setFormData] = useState<Partial<TerritoryIncentiveRuleRequest>>({
    incentivePercentage: 0.04,
    srSharePercentage: 0.09,
    developmentFundPercentage: 0.01,
    hasDedicatedSr: true,
    dualRoleEmployeeId: null,
    expenseLimitPercentage: 0.30,
    isActive: true,
    allocations: []
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadTerritories();
      loadEmployees();
    }
  }, [currentOrganizationId]);

  const loadEmployees = async () => {
    if (!currentOrganizationId) return;
    try {
      const res = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
      setEmployees(res.data || []);
    } catch (error) {
      console.error('Failed to load employees:', error);
    }
  };

  const getEmployeeName = (employeeId: string | undefined): string => {
    if (!employeeId) return '-';
    const employee = employees.find(e => (e.id === employeeId || e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || '-') : '-';
  };

  useEffect(() => {
    if (selectedTerritory) {
      loadCurrentRule();
      loadTerritoryAssignments();
      if (activeTab === 1) {
        loadRuleHistory();
      }
    }
  }, [selectedTerritory, activeTab]);

  useEffect(() => {
    if (currentOrganizationId && territories.length > 0) {
      checkTerritoriesWithoutRules();
    }
  }, [currentOrganizationId, territories]);

  const loadTerritories = async (retryCount = 0) => {
    if (!currentOrganizationId) return;
    const maxRetries = 2;
    try {
      const data = await pharmaService.getAllTerritoriesForOrganization(currentOrganizationId);
      setTerritories(data);
    } catch (error: any) {
      const is503 = error?.response?.status === 503;
      if (is503 && retryCount < maxRetries) {
        setTimeout(() => loadTerritories(retryCount + 1), 1500);
      } else {
        console.error('Failed to load territories:', error);
      }
    }
  };

  const loadCurrentRule = async () => {
    if (!selectedTerritory) return;
    try {
      setLoading(true);
      const rule = await pharmaService.getIncentiveRuleWithAllocations(selectedTerritory);
      setCurrentRule(rule);
    } catch (error: any) {
      enqueueSnackbar('Failed to load incentive rule', { variant: 'error' });
      setCurrentRule(null);
    } finally {
      setLoading(false);
    }
  };

  const loadTerritoryAssignments = async () => {
    if (!selectedTerritory) return;
    try {
      const assignments = await pharmaService.getActiveAssignmentsByTerritory(selectedTerritory);
      setTerritoryAssignments(assignments);
    } catch (error) {
      console.error('Failed to load assignments:', error);
      setTerritoryAssignments([]);
    }
  };

  const loadRuleHistory = async () => {
    if (!selectedTerritory) return;
    try {
      const history = await pharmaService.getIncentiveRuleHistory(selectedTerritory);
      setRuleHistory(history);
    } catch (error: any) {
      enqueueSnackbar('Failed to load rule history', { variant: 'error' });
    }
  };

  const checkTerritoriesWithoutRules = async () => {
    if (!currentOrganizationId || territories.length === 0) return;
    const results = await Promise.all(
      territories.map(async (t) => {
        try {
          const rule = await pharmaService.getIncentiveRuleWithAllocations(t.id);
          return rule ? null : (t.name || t.id);
        } catch {
          return t.name || t.id;
        }
      })
    );
    setTerritoriesWithoutRules(results.filter((x): x is string => x != null));
  };

  const managerMpoAssignments = territoryAssignments.filter(a => isManagerOrMpo(a.roleInTerritory));
  const mpoAssignments = territoryAssignments.filter(a => a.roleInTerritory === 'MPO');
  const srAssignments = territoryAssignments.filter(a => a.roleInTerritory === 'SR');

  const getEffectiveSrEmployeeId = (): string | null => {
    if (srAssignments.length > 0) return srAssignments[0].employeeId;
    return formData.dualRoleEmployeeId || null;
  };

  const hasDedicatedSr = srAssignments.length > 0;

  const getEffectiveSrEmployeeIdForRule = (rule: TerritoryIncentiveRuleResponse | null): string | null => {
    if (!rule) return null;
    if (rule.hasDedicatedSr && srAssignments.length > 0) return srAssignments[0].employeeId;
    if (!rule.hasDedicatedSr && rule.dualRoleEmployeeId) return rule.dualRoleEmployeeId;
    return null;
  };

  const allocationSum = (formData.allocations || []).reduce((s, a) => s + (a.allocationPercentage || 0), 0);
  const allocationValid = Math.abs(allocationSum - 100) < 0.01;

  const handleOpenDialog = () => {
    if (currentRule) {
      setFormData({
        id: currentRule.id,
        organizationId: currentOrganizationId || '',
        territoryId: selectedTerritory,
        incentivePercentage: currentRule.incentivePercentage,
        srSharePercentage: currentRule.srSharePercentage,
        developmentFundPercentage: currentRule.developmentFundPercentage ?? 0.01,
        hasDedicatedSr: currentRule.hasDedicatedSr ?? true,
        dualRoleEmployeeId: currentRule.dualRoleEmployeeId || null,
        expenseLimitPercentage: currentRule.expenseLimitPercentage,
        isActive: currentRule.isActive,
        allocations: currentRule.allocations?.map(a => ({
          employeeId: a.employeeId,
          roleInTerritory: a.roleInTerritory,
          allocationPercentage: a.allocationPercentage
        })) || []
      });
    } else {
      const n = managerMpoAssignments.length;
      const defaultAllocations: TerritoryIncentiveAllocationRequestItem[] = managerMpoAssignments.map((a, i) => {
        const basePct = n > 0 ? 100 / n : 0;
        const pct = i === n - 1 ? 100 - (Math.round(basePct * 100) / 100) * (n - 1) : Math.round(basePct * 100) / 100;
        return { employeeId: a.employeeId, roleInTerritory: a.roleInTerritory, allocationPercentage: pct };
      });
      setFormData({
        organizationId: currentOrganizationId || '',
        territoryId: selectedTerritory,
        incentivePercentage: 0.04,
        srSharePercentage: 0.09,
        developmentFundPercentage: 0.01,
        hasDedicatedSr: srAssignments.length > 0,
        dualRoleEmployeeId: null,
        expenseLimitPercentage: 0.30,
        isActive: true,
        allocations: defaultAllocations
      });
    }
    setShowDialog(true);
  };

  const handleAllocationChange = (employeeId: string, allocationPercentage: number) => {
    const allocs = [...(formData.allocations || [])];
    const idx = allocs.findIndex(a => a.employeeId === employeeId);
    if (idx >= 0) {
      allocs[idx] = { ...allocs[idx], allocationPercentage };
    } else {
      const a = managerMpoAssignments.find(m => m.employeeId === employeeId);
      allocs.push({ employeeId, roleInTerritory: a?.roleInTerritory, allocationPercentage });
    }
    setFormData({ ...formData, allocations: allocs });
  };

  const handleSaveRule = async () => {
    if (!selectedTerritory || !currentOrganizationId) return;
    if (!allocationValid && (formData.allocations?.length ?? 0) > 0) {
      enqueueSnackbar('Allocation percentages must sum to 100%', { variant: 'error' });
      return;
    }
    if (!hasDedicatedSr && !formData.dualRoleEmployeeId && (formData.allocations?.length ?? 0) > 0) {
      enqueueSnackbar('Please select an MPO to act as SR when territory has no dedicated SR', { variant: 'error' });
      return;
    }
    try {
      setLoading(true);
      const request: TerritoryIncentiveRuleRequest = {
        id: formData.id,
        organizationId: currentOrganizationId,
        territoryId: selectedTerritory,
        incentivePercentage: formData.incentivePercentage ?? 0.04,
        srSharePercentage: formData.srSharePercentage ?? 0.09,
        developmentFundPercentage: formData.developmentFundPercentage ?? 0.01,
        hasDedicatedSr: hasDedicatedSr,
        dualRoleEmployeeId: formData.dualRoleEmployeeId || undefined,
        expenseLimitPercentage: formData.expenseLimitPercentage ?? 0.30,
        effectiveFromDate: formData.effectiveFromDate,
        effectiveToDate: formData.effectiveToDate,
        isActive: formData.isActive ?? true,
        description: formData.description,
        notes: formData.notes,
        allocations: formData.allocations || []
      };
      await pharmaService.saveIncentiveRuleWithAllocations(request);
      enqueueSnackbar('Incentive rule saved successfully', { variant: 'success' });
      setShowDialog(false);
      loadCurrentRule();
      loadRuleHistory();
      checkTerritoriesWithoutRules();
    } catch (error: any) {
      const msg = error?.response?.data?.error || 'Failed to save incentive rule';
      enqueueSnackbar(msg, { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivateRule = async () => {
    if (!currentRule?.id) return;
    if (!window.confirm('Are you sure you want to deactivate this rule? This territory will have no incentive rule.')) {
      return;
    }
    try {
      await pharmaService.deactivateIncentiveRule(currentRule.id);
      enqueueSnackbar('Incentive rule deactivated successfully', { variant: 'success' });
      loadCurrentRule();
      loadRuleHistory();
      checkTerritoriesWithoutRules();
    } catch (error: any) {
      enqueueSnackbar('Failed to deactivate incentive rule', { variant: 'error' });
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Incentive Rules Management
      </Typography>

      {territoriesWithoutRules.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }} icon={<WarningIcon />}>
          <strong>Territories without rules:</strong> {territoriesWithoutRules.length} territory(ies) have no incentive rule defined.
          {territoriesWithoutRules.slice(0, 5).join(', ')}
          {territoriesWithoutRules.length > 5 && ` and ${territoriesWithoutRules.length - 5} more`}.
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6}>
            <TerritoryTreeSelector
              label="Select Territory"
              value={selectedTerritory}
              onChange={setSelectedTerritory}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Button
              variant="contained"
              startIcon={currentRule ? <EditIcon /> : <AddIcon />}
              onClick={handleOpenDialog}
              disabled={!selectedTerritory}
              fullWidth
            >
              {currentRule ? 'Edit Rule' : 'Create Rule'}
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {!selectedTerritory && (
        <Alert severity="info">
          Please select a territory to view or manage incentive rules.
        </Alert>
      )}

      {selectedTerritory && (
        <>
          <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ mb: 2 }}>
            <Tab label="Current Rule" />
            <Tab label="Rule History" icon={<HistoryIcon />} iconPosition="end" />
          </Tabs>

          {activeTab === 0 && (
            <Card>
              <CardContent>
                {loading ? (
                  <Box display="flex" justifyContent="center" p={4}>
                    <CircularProgress />
                  </Box>
                ) : !currentRule ? (
                  <Alert severity="info">
                    No incentive rule defined for this territory. Click &quot;Create Rule&quot; to add one.
                  </Alert>
                ) : (
                  <>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                      <Typography variant="h6">Current Incentive Rule</Typography>
                      <Box>
                        <Button size="small" startIcon={<EditIcon />} onClick={handleOpenDialog} sx={{ mr: 1 }}>
                          Edit
                        </Button>
                        <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={handleDeactivateRule}>
                          Deactivate
                        </Button>
                      </Box>
                    </Box>
                    <Grid container spacing={2}>
                      <Grid item xs={12} md={3}>
                        <Paper sx={{ p: 2 }}>
                          <Typography variant="subtitle2" color="textSecondary">Incentive %</Typography>
                          <Typography variant="h4">{(currentRule.incentivePercentage * 100).toFixed(2)}%</Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} md={3}>
                        <Paper sx={{ p: 2 }}>
                          <Typography variant="subtitle2" color="textSecondary">SR Share</Typography>
                          <Typography variant="h4">{(currentRule.srSharePercentage * 100).toFixed(0)}%</Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} md={3}>
                        <Paper sx={{ p: 2 }}>
                          <Typography variant="subtitle2" color="textSecondary">Development Fund</Typography>
                          <Typography variant="h4">{((currentRule.developmentFundPercentage ?? 0.01) * 100).toFixed(0)}%</Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12} md={3}>
                        <Paper sx={{ p: 2 }}>
                          <Typography variant="subtitle2" color="textSecondary">Expense Limit</Typography>
                          <Typography variant="h4">{(currentRule.expenseLimitPercentage * 100).toFixed(0)}%</Typography>
                        </Paper>
                      </Grid>
                      <Grid item xs={12}>
                        <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>SR Commission &amp; Development Fund</Typography>
                        <TableContainer component={Paper} variant="outlined">
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell>Name</TableCell>
                                <TableCell>Role</TableCell>
                                <TableCell align="right">Commission %</TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              <TableRow>
                                <TableCell>{getEmployeeName(getEffectiveSrEmployeeIdForRule(currentRule))}</TableCell>
                                <TableCell>SR</TableCell>
                                <TableCell align="right">{(currentRule.srSharePercentage * 100).toFixed(0)}%</TableCell>
                              </TableRow>
                              <TableRow>
                                <TableCell>Development Fund</TableCell>
                                <TableCell>Fund</TableCell>
                                <TableCell align="right">{((currentRule.developmentFundPercentage ?? 0.01) * 100).toFixed(0)}%</TableCell>
                              </TableRow>
                            </TableBody>
                          </Table>
                        </TableContainer>
                      </Grid>
                      {currentRule.allocations && currentRule.allocations.length > 0 && (
                        <Grid item xs={12}>
                          <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>Other Employee Commissions</Typography>
                          <TableContainer component={Paper} variant="outlined">
                            <Table size="small">
                              <TableHead>
                                <TableRow>
                                  <TableCell>Name</TableCell>
                                  <TableCell>Role</TableCell>
                                  <TableCell align="right">Commission %</TableCell>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {currentRule.allocations.map((a) => (
                                  <TableRow key={a.employeeId}>
                                    <TableCell>{getEmployeeName(a.employeeId)}</TableCell>
                                    <TableCell>{a.roleInTerritory || '-'}</TableCell>
                                    <TableCell align="right">{a.allocationPercentage}%</TableCell>
                                  </TableRow>
                                ))}
                                <TableRow>
                                  <TableCell colSpan={2}><strong>Total</strong></TableCell>
                                  <TableCell align="right">100%</TableCell>
                                </TableRow>
                              </TableBody>
                            </Table>
                          </TableContainer>
                        </Grid>
                      )}
                    </Grid>
                  </>
                )}
              </CardContent>
            </Card>
          )}

          {activeTab === 1 && (
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Rule History</Typography>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Version</TableCell>
                        <TableCell>Incentive %</TableCell>
                        <TableCell>Effective From</TableCell>
                        <TableCell>Effective To</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {ruleHistory.map((rule) => (
                        <TableRow key={rule.id}>
                          <TableCell>v{rule.ruleVersion}</TableCell>
                          <TableCell>{(rule.incentivePercentage * 100).toFixed(2)}%</TableCell>
                          <TableCell>{rule.effectiveFromDate || 'N/A'}</TableCell>
                          <TableCell>{rule.effectiveToDate || 'Active'}</TableCell>
                          <TableCell>
                            <Chip
                              label={rule.isActive ? 'Active' : 'Inactive'}
                              color={rule.isActive ? 'success' : 'default'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </CardContent>
            </Card>
          )}
        </>
      )}

      <Dialog open={showDialog} onClose={() => setShowDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {currentRule ? 'Edit Incentive Rule' : 'Create Incentive Rule'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Incentive Percentage (%)"
                type="number"
                inputProps={{ step: 0.01, min: 0, max: 100 }}
                value={((formData.incentivePercentage ?? 0.04) * 100).toFixed(2)}
                onChange={(e) => setFormData({ ...formData, incentivePercentage: parseFloat(e.target.value) / 100 })}
                helperText="% of sales for incentives (e.g., 4%)"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Expense Limit (%)"
                type="number"
                inputProps={{ step: 0.01, min: 0, max: 100 }}
                value={(formData.expenseLimitPercentage ?? 0.30) * 100}
                onChange={(e) => setFormData({ ...formData, expenseLimitPercentage: parseFloat(e.target.value) / 100 })}
                helperText="Max expense % of target (e.g., 30%)"
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle1" sx={{ mt: 2, mb: 1 }}>
                SR Commission &amp; Development Fund
              </Typography>
              <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>Role</TableCell>
                      <TableCell align="right" sx={{ width: 120 }}>Commission %</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <TableRow>
                      <TableCell>
                        {hasDedicatedSr ? (
                          getEmployeeName(getEffectiveSrEmployeeId())
                        ) : mpoAssignments.length > 0 ? (
                          <TextField
                            fullWidth
                            select
                            size="small"
                            value={formData.dualRoleEmployeeId || ''}
                            onChange={(e) => setFormData({ ...formData, dualRoleEmployeeId: e.target.value || null })}
                            sx={{ minWidth: 180 }}
                          >
                            <MenuItem value="">— Select MPO as SR —</MenuItem>
                            {mpoAssignments.map((a) => (
                              <MenuItem key={a.employeeId} value={a.employeeId}>
                                {getEmployeeName(a.employeeId)}
                              </MenuItem>
                            ))}
                          </TextField>
                        ) : (
                          '-'
                        )}
                      </TableCell>
                      <TableCell>SR</TableCell>
                      <TableCell align="right">
                        <TextField
                          type="number"
                          size="small"
                          inputProps={{ step: 0.01, min: 0, max: 100 }}
                          value={(formData.srSharePercentage ?? 0.09) * 100}
                          onChange={(e) => setFormData({ ...formData, srSharePercentage: parseFloat(e.target.value) / 100 })}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Development Fund</TableCell>
                      <TableCell>Fund</TableCell>
                      <TableCell align="right">
                        <TextField
                          type="number"
                          size="small"
                          inputProps={{ step: 0.01, min: 0, max: 100 }}
                          value={(formData.developmentFundPercentage ?? 0.01) * 100}
                          onChange={(e) => setFormData({ ...formData, developmentFundPercentage: parseFloat(e.target.value) / 100 })}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle1" sx={{ mt: 2, mb: 1 }}>
                Other Employee Commissions (% of remaining pool — must total 100%)
              </Typography>
              {managerMpoAssignments.length === 0 ? (
                <Alert severity="info">No Managers or MPOs assigned to this territory. Add employee assignments first.</Alert>
              ) : (
                <>
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Name</TableCell>
                          <TableCell>Role</TableCell>
                          <TableCell align="right" sx={{ width: 120 }}>Commission %</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {managerMpoAssignments.map((a) => {
                          const alloc = formData.allocations?.find(x => x.employeeId === a.employeeId);
                          const pct = alloc?.allocationPercentage ?? 0;
                          return (
                            <TableRow key={a.employeeId}>
                              <TableCell>{getEmployeeName(a.employeeId)}</TableCell>
                              <TableCell>{a.roleInTerritory || '-'}</TableCell>
                              <TableCell align="right">
                                <TextField
                                  type="number"
                                  size="small"
                                  inputProps={{ step: 0.01, min: 0, max: 100 }}
                                  value={pct}
                                  onChange={(e) => handleAllocationChange(a.employeeId, parseFloat(e.target.value) || 0)}
                                  sx={{ width: 80 }}
                                />
                              </TableCell>
                            </TableRow>
                          );
                        })}
                        <TableRow>
                          <TableCell colSpan={2}><strong>Total</strong></TableCell>
                          <TableCell align="right">
                            <Typography color={allocationValid ? 'textPrimary' : 'error'} fontWeight="bold">
                              {allocationSum.toFixed(2)}%
                            </Typography>
                          </TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>
                  {!allocationValid && (formData.allocations?.length ?? 0) > 0 && (
                    <Alert severity="error" sx={{ mt: 1 }}>
                      Allocation percentages must sum to 100%. Current: {allocationSum.toFixed(2)}%
                    </Alert>
                  )}
                </>
              )}
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Effective From Date"
                type="date"
                value={formData.effectiveFromDate || ''}
                onChange={(e) => setFormData({ ...formData, effectiveFromDate: e.target.value || undefined })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Effective To Date"
                type="date"
                value={formData.effectiveToDate || ''}
                onChange={(e) => setFormData({ ...formData, effectiveToDate: e.target.value || undefined })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={2}
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowDialog(false)}>Cancel</Button>
          <Button
            onClick={handleSaveRule}
            variant="contained"
            disabled={loading || (!allocationValid && (formData.allocations?.length ?? 0) > 0)}
          >
            {loading ? <CircularProgress size={24} /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default IncentiveRulesManagement;
