import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel,
  Grid, Alert, Tooltip
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Calculate as CalculateIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Target, Territory } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';
import { useSnackbar } from 'notistack';

const MONTHS = [
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

const TargetManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [targets, setTargets] = useState<Target[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedTarget, setSelectedTarget] = useState<Target | null>(null);
  const [formData, setFormData] = useState<Partial<Target>>({
    organizationId: currentOrganizationId,
    year: new Date().getFullYear(),
    startMonth: 1,
    endMonth: 6,
    status: 'ACTIVE',
    targetAmount: 0
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [targetsData, territoriesData, employeesRes] = await Promise.all([
        pharmaService.getTargets(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' })
      ]);
      setTargets(targetsData);
      setTerritories(territoriesData);
      setEmployees(employeesRes.data || []);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (target?: Target) => {
    if (target) {
      setSelectedTarget(target);
      setFormData({ ...target });
    } else {
      setSelectedTarget(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        employeeId: '',
        year: new Date().getFullYear(),
        startMonth: 1,
        endMonth: 6,
        targetAmount: 0,
        status: 'ACTIVE'
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedTarget(null);
  };

  const handleSave = async () => {
    if (!formData.territoryId || !formData.employeeId || !formData.targetAmount) {
      enqueueSnackbar('Please fill in all required fields', { variant: 'warning' });
      return;
    }

    try {
      if (selectedTarget?.id) {
        await pharmaService.updateTarget(selectedTarget.id, formData as Target);
        enqueueSnackbar('Target updated successfully', { variant: 'success' });
      } else {
        await pharmaService.createTarget(formData as Target);
        enqueueSnackbar('Target created successfully', { variant: 'success' });
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save target:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to save target', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this target?')) return;
    try {
      await pharmaService.deleteTarget(id);
      enqueueSnackbar('Target deleted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete target:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to delete target', { variant: 'error' });
    }
  };

  const handleCalculateCoverage = async (target: Target) => {
    try {
      // Calculate for the current month by default or prompt user
      const currentMonth = new Date().getMonth() + 1;
      const currentYear = new Date().getFullYear();
      
      await pharmaService.calculateCoverage(target.territoryId, currentYear, currentMonth);
      enqueueSnackbar('Coverage calculated successfully', { variant: 'success' });
    } catch (error: any) {
      console.error('Failed to calculate coverage:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to calculate coverage', { variant: 'error' });
    }
  };

  const getTerritoryName = (territoryId: string): string => {
    const territory = territories.find(t => t.id === territoryId);
    return territory ? territory.name : '-';
  };

  const getEmployeeName = (employeeId: string): string => {
    const employee = employees.find(e => (e.id === employeeId) || (e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || '-') : '-';
  };

  const getMonthName = (month: number) => {
    return MONTHS.find(m => m.id === month)?.name || '-';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Target Management</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Target
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Targets are set area-wise for 6-month periods. Each month in the period shares the same target amount.
          </Alert>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Territory</TableCell>
                    <TableCell>Manager / Responsible</TableCell>
                    <TableCell>Year</TableCell>
                    <TableCell>Period</TableCell>
                    <TableCell>Monthly Target</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {targets.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No targets found
                      </TableCell>
                    </TableRow>
                  ) : (
                    targets.map((target) => (
                      <TableRow key={target.id}>
                        <TableCell>{getTerritoryName(target.territoryId)}</TableCell>
                        <TableCell>{getEmployeeName(target.employeeId)}</TableCell>
                        <TableCell>{target.year}</TableCell>
                        <TableCell>
                          {getMonthName(target.startMonth)} - {getMonthName(target.endMonth)}
                        </TableCell>
                        <TableCell>{target.targetAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                        <TableCell>
                          <Chip
                            label={target.status}
                            size="small"
                            color={target.status === 'ACTIVE' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => handleOpenDialog(target)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Calculate Coverage">
                            <IconButton size="small" color="primary" onClick={() => handleCalculateCoverage(target)}>
                              <CalculateIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton size="small" color="error" onClick={() => target.id && handleDelete(target.id)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedTarget ? 'Edit Target' : 'New Target'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Territory"
                value={formData.territoryId || ''}
                onChange={(territoryId) => setFormData({ ...formData, territoryId })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth required>
                <InputLabel>Responsible Employee</InputLabel>
                <Select
                  value={formData.employeeId || ''}
                  label="Responsible Employee"
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                >
                  {employees.map((emp) => {
                    const employeeIdValue = emp.employeeId || emp.id || '';
                    return (
                      <MenuItem key={employeeIdValue} value={employeeIdValue}>
                        {emp.name || emp.employeeNumber}
                        {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Year"
                type="number"
                value={formData.year || new Date().getFullYear()}
                onChange={(e) => setFormData({ ...formData, year: Number(e.target.value) })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Monthly Target Amount"
                type="number"
                value={formData.targetAmount || 0}
                onChange={(e) => setFormData({ ...formData, targetAmount: Number(e.target.value) })}
                required
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Start Month</InputLabel>
                <Select
                  value={formData.startMonth || 1}
                  label="Start Month"
                  onChange={(e) => setFormData({ ...formData, startMonth: Number(e.target.value) })}
                >
                  {MONTHS.map((m) => (
                    <MenuItem key={m.id} value={m.id}>{m.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>End Month</InputLabel>
                <Select
                  value={formData.endMonth || 6}
                  label="End Month"
                  onChange={(e) => setFormData({ ...formData, endMonth: Number(e.target.value) })}
                >
                  {MONTHS.map((m) => (
                    <MenuItem key={m.id} value={m.id}>{m.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status || 'ACTIVE'}
                  label="Status"
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TargetManagement;
