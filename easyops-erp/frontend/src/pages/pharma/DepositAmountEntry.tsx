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
  Send as SendIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Deposit, Territory } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';
import accountingService from '../../services/accountingService';
import { useSnackbar } from 'notistack';

const DepositAmountEntryPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [deposits, setDeposits] = useState<Deposit[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [bankAccounts, setBankAccounts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedDeposit, setSelectedDeposit] = useState<Deposit | null>(null);
  const [formData, setFormData] = useState<Partial<Deposit>>({
    organizationId: currentOrganizationId,
    depositDate: new Date().toISOString().split('T')[0],
    status: 'DRAFT',
    depositAmount: 0,
    bankAccountId: '',
    bankName: '',
    bankAccountNumber: ''
  });

  useEffect(() => {
    if (currentOrganizationId) loadData();
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [depositsData, territoriesData, employeesRes, bankAccountsData] = await Promise.all([
        pharmaService.getDeposits(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
        accountingService.getBankAccounts(currentOrganizationId).catch(() => [])
      ]);
      setDeposits(depositsData);
      setTerritories(territoriesData);
      setEmployees(employeesRes.data || []);
      setBankAccounts(bankAccountsData || []);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (deposit?: Deposit) => {
    if (deposit) {
      setSelectedDeposit(deposit);
      // When editing: if no bankAccountId, try to match by bank name + account number
      let bankAccountId = deposit.bankAccountId ?? '';
      if (!bankAccountId && deposit.bankName && deposit.bankAccountNumber) {
        const match = bankAccounts.find(
          (a) =>
            (a.bankName === deposit.bankName || a.accountName === deposit.bankName) &&
            a.accountNumber === deposit.bankAccountNumber
        );
        if (match) bankAccountId = match.id;
      }
      setFormData({
        ...deposit,
        bankAccountId: bankAccountId || undefined,
        depositDate: deposit.depositDate?.split('T')[0] || new Date().toISOString().split('T')[0]
      });
    } else {
      setSelectedDeposit(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        employeeId: '',
        depositDate: new Date().toISOString().split('T')[0],
        depositAmount: 0,
        bankAccountId: '',
        bankName: '',
        bankAccountNumber: '',
        status: 'DRAFT',
        notes: ''
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedDeposit(null);
  };

  const handleSave = async () => {
    if (!formData.territoryId || formData.depositAmount == null || formData.depositAmount <= 0) {
      enqueueSnackbar('Please fill territory and amount', { variant: 'warning' });
      return;
    }
    if (!formData.bankAccountId || !formData.bankName || !formData.bankAccountNumber) {
      enqueueSnackbar('Please select a bank account', { variant: 'warning' });
      return;
    }
    try {
      const payload: Deposit = {
        ...formData,
        organizationId: currentOrganizationId!,
        territoryId: formData.territoryId!,
        depositDate: formData.depositDate!,
        depositAmount: formData.depositAmount!,
        bankAccountId: formData.bankAccountId || undefined,
        bankName: formData.bankName!,
        bankAccountNumber: formData.bankAccountNumber!,
        status: formData.status ?? 'DRAFT'
      } as Deposit;
      if (selectedDeposit?.id) {
        await pharmaService.updateDeposit(selectedDeposit.id, payload);
        enqueueSnackbar('Deposit updated', { variant: 'success' });
      } else {
        await pharmaService.createDeposit(payload);
        enqueueSnackbar('Deposit created', { variant: 'success' });
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save:', error);
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to save', { variant: 'error' });
    }
  };

  const handleSubmit = async (id: string) => {
    if (!window.confirm('Submit this deposit?')) return;
    try {
      await pharmaService.submitDeposit(id);
      enqueueSnackbar('Deposit submitted', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to submit', { variant: 'error' });
    }
  };

  const handleComplete = async (id: string) => {
    if (!window.confirm('Mark this deposit as COMPLETED?')) return;
    try {
      await pharmaService.completeDeposit(id);
      enqueueSnackbar('Deposit completed', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to complete', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this draft deposit?')) return;
    try {
      await pharmaService.deleteDeposit(id);
      enqueueSnackbar('Deposit deleted', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to delete', { variant: 'error' });
    }
  };

  const getTerritoryName = (territoryId: string) => territories.find(t => t.id === territoryId)?.name ?? '-';
  const getEmployeeName = (empId?: string) => {
    if (!empId) return '-';
    const e = employees.find(x => x.id === empId || x.employeeId === empId);
    return e ? (e.name ?? e.employeeNumber ?? '-') : '-';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Deposit Amount Entry</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
          New Deposit
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Record money deposited to the bank by territory. This is separate from product sales.
          </Alert>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Territory</TableCell>
                    <TableCell>Bank / Account</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!deposits.length ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">No deposits</TableCell>
                    </TableRow>
                  ) : (
                    deposits.map((d) => (
                      <TableRow key={d.id!}>
                        <TableCell>{d.depositDate?.split('T')[0]}</TableCell>
                        <TableCell>{getTerritoryName(d.territoryId)}</TableCell>
                        <TableCell>
                          {d.bankName}
                          <br />
                          <Typography variant="caption">{d.bankAccountNumber}</Typography>
                        </TableCell>
                        <TableCell align="right">
                          {d.depositAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 }) ?? '-'}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={d.status}
                            size="small"
                            color={d.status === 'COMPLETED' ? 'success' : d.status === 'SUBMITTED' ? 'info' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => handleOpenDialog(d)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {d.status === 'DRAFT' && (
                            <>
                              <Tooltip title="Submit">
                                <IconButton size="small" color="primary" onClick={() => d.id && handleSubmit(d.id)}>
                                  <SendIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton size="small" color="error" onClick={() => d.id && handleDelete(d.id)}>
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}
                          {d.status === 'SUBMITTED' && (
                            <Tooltip title="Complete">
                              <IconButton size="small" color="success" onClick={() => d.id && handleComplete(d.id)}>
                                <CheckCircleIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
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
        <DialogTitle>{selectedDeposit ? 'Edit Deposit' : 'New Deposit'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Territory"
                value={formData.territoryId ?? ''}
                onChange={(territoryId) => setFormData({ ...formData, territoryId })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Deposit Date"
                type="date"
                value={formData.depositDate ?? ''}
                onChange={(e) => setFormData({ ...formData, depositDate: e.target.value })}
                required
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label="Bank / Cash Account"
                value={formData.bankAccountId ?? ''}
                onChange={(e) => {
                  const id = e.target.value;
                  const acc = bankAccounts.find((a) => a.id === id);
                  setFormData({
                    ...formData,
                    bankAccountId: id,
                    bankName: acc?.bankName ?? acc?.accountName ?? '',
                    bankAccountNumber: acc?.accountNumber ?? ''
                  });
                }}
                required
                helperText={bankAccounts.length === 0 ? 'No bank/cash accounts configured. Add them in Accounting > Bank Accounts.' : undefined}
              >
                <MenuItem value="">Select Bank or Cash Account</MenuItem>
                {bankAccounts.map((acc) => (
                  <MenuItem key={acc.id} value={acc.id}>
                    {(acc.bankName || acc.accountName) + (acc.accountNumber ? ` - ${acc.accountNumber}` : '')}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Deposit Amount"
                type="number"
                value={formData.depositAmount ?? 0}
                onChange={(e) => setFormData({ ...formData, depositAmount: Number(e.target.value) })}
                required
                InputProps={{ inputProps: { step: 0.01, min: 0 } }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Deposited By (optional)</InputLabel>
                <Select
                  value={formData.employeeId ?? ''}
                  label="Deposited By (optional)"
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                >
                  <MenuItem value="">None</MenuItem>
                  {employees.map((emp) => {
                    const v = emp.employeeId ?? emp.id ?? '';
                    return (
                      <MenuItem key={v} value={v}>
                        {emp.name ?? emp.employeeNumber}
                        {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notes"
                multiline
                rows={2}
                value={formData.notes ?? ''}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={formData.status !== 'DRAFT'}>
            {selectedDeposit ? 'Update' : 'Save Draft'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DepositAmountEntryPage;
