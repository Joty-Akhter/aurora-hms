import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Grid,
  IconButton,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';

const BankAccounts: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [accounts, setAccounts] = useState<any[]>([]);
  const [glAccounts, setGlAccounts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingAccount, setEditingAccount] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';

  const [formData, setFormData] = useState({
    accountName: '',
    accountNumber: '',
    accountType: 'CHECKING',
    bankName: '',
    branchName: '',
    currency: 'USD',
    openingBalance: 0,
    currentBalance: 0,
    glAccountId: '',
    status: 'ACTIVE',
    notes: '',
  });

  useEffect(() => {
    if (organizationId) {
      loadBankAccounts();
      loadGLAccounts();
    }
  }, [organizationId]);

  const loadBankAccounts = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getBankAccounts(organizationId);
      setAccounts(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load bank accounts');
    } finally {
      setLoading(false);
    }
  };

  const loadGLAccounts = async () => {
    try {
      const data = await accountingService.getActiveAccounts(organizationId);
      // Filter for asset accounts (bank accounts are assets)
      const assetAccounts = data.filter((acc: any) => 
        acc.accountType === 'ASSET' || acc.accountType === 'BANK' || acc.accountType === 'CASH'
      );
      setGlAccounts(assetAccounts);
    } catch (err: any) {
      console.error('Failed to load GL accounts:', err);
    }
  };

  const openAccountDialog = (account?: any) => {
    if (account) {
      setEditingAccount(account);
      setFormData({
        accountName: account.accountName || '',
        accountNumber: account.accountNumber || '',
        accountType: account.accountType || 'CHECKING',
        bankName: account.bankName || '',
        branchName: account.branchName || '',
        currency: account.currency || 'USD',
        openingBalance: account.openingBalance ?? 0,
        currentBalance: account.currentBalance ?? 0,
        glAccountId: account.glAccountId || '',
        status: account.isActive === false ? 'INACTIVE' : 'ACTIVE',
        notes: account.notes || '',
      });
    } else {
      setEditingAccount(null);
      setFormData({
        accountName: '',
        accountNumber: '',
        accountType: 'CHECKING',
        bankName: '',
        branchName: '',
        currency: 'USD',
        openingBalance: 0,
        currentBalance: 0,
        glAccountId: '',
        status: 'ACTIVE',
        notes: '',
      });
    }
    setOpenDialog(true);
  };

  const handleSaveAccount = async () => {
    setError(null);
    setSuccess(null);

    // Validation
    if (!formData.accountName) {
      setError('Account name is required');
      return;
    }
    if (!formData.accountNumber) {
      setError('Account number is required');
      return;
    }
    if (!formData.glAccountId) {
      setError('Please select a GL account');
      return;
    }

    try {
      const accountData = {
        organizationId,
        accountName: formData.accountName,
        accountNumber: formData.accountNumber,
        accountType: formData.accountType,
        bankName: formData.bankName,
        branchName: formData.branchName,
        currency: formData.currency,
        openingBalance: formData.openingBalance,
        glAccountId: formData.glAccountId,
        isActive: formData.status !== 'INACTIVE',
      };

      if (editingAccount) {
        await accountingService.updateBankAccount(editingAccount.id, accountData);
        setSuccess('Bank account updated successfully!');
      } else {
        await accountingService.createBankAccount(accountData);
        setSuccess('Bank account created successfully!');
      }
      setOpenDialog(false);
      
      // Reset form
      setFormData({
        accountName: '',
        accountNumber: '',
        accountType: 'CHECKING',
        bankName: '',
        branchName: '',
        currency: 'USD',
        openingBalance: 0,
        currentBalance: 0,
        glAccountId: '',
        status: 'ACTIVE',
        notes: '',
      });
      
      loadBankAccounts();
    } catch (err: any) {
      setError(err.message || 'Failed to create bank account');
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Bank Accounts</Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => openAccountDialog()}
          >
            Add Bank Account
          </Button>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Bank Accounts List
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Account Name</TableCell>
                  <TableCell>Account Number</TableCell>
                  <TableCell>Bank Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell align="right">Current Balance</TableCell>
                  <TableCell>Currency</TableCell>
                  <TableCell>Status</TableCell>
                  {canManage && <TableCell align="right">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2">Loading bank accounts...</Typography>
                    </TableCell>
                  </TableRow>
                ) : accounts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No bank accounts yet. Add your first bank account to get started!
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  accounts.map((account) => (
                    <TableRow key={account.id}>
                      <TableCell>{account.accountName}</TableCell>
                      <TableCell>{account.accountNumber}</TableCell>
                      <TableCell>{account.bankName}</TableCell>
                      <TableCell>{account.accountType}</TableCell>
                      <TableCell align="right">
                        <strong>{formatCurrency(account.currentBalance || 0)}</strong>
                      </TableCell>
                      <TableCell>{account.currency}</TableCell>
                      <TableCell>
                        <Chip 
                          label={account.status} 
                          size="small"
                          color={account.status === 'ACTIVE' ? 'success' : 'default'}
                        />
                      </TableCell>
                      {canManage && (
                        <TableCell align="right">
                          <IconButton size="small" onClick={() => openAccountDialog(account)}><EditIcon /></IconButton>
                          <IconButton size="small" color="error" onClick={async () => {
                            if (window.confirm('Delete this bank account?')) {
                              await accountingService.deleteBankAccount(account.id);
                              loadBankAccounts();
                            }
                          }}><DeleteIcon /></IconButton>
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Bank Account Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingAccount ? 'Edit Bank Account' : 'Add Bank Account'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={1}>
            <Grid item xs={6}>
              <TextField
                label="Account Name"
                value={formData.accountName}
                onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Account Number"
                value={formData.accountNumber}
                onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
                fullWidth
                required
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Bank Name"
                value={formData.bankName}
                onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
                fullWidth
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Branch Name"
                value={formData.branchName}
                onChange={(e) => setFormData({ ...formData, branchName: e.target.value })}
                fullWidth
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Account Type"
                value={formData.accountType}
                onChange={(e) => setFormData({ ...formData, accountType: e.target.value })}
                fullWidth
              >
                <MenuItem value="CHECKING">Checking</MenuItem>
                <MenuItem value="SAVINGS">Savings</MenuItem>
                <MenuItem value="CASH">Cash</MenuItem>
                <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Currency"
                value={formData.currency}
                onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                fullWidth
              >
                <MenuItem value="USD">USD</MenuItem>
                <MenuItem value="EUR">EUR</MenuItem>
                <MenuItem value="GBP">GBP</MenuItem>
                <MenuItem value="INR">INR</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                select
                label="GL Account"
                value={formData.glAccountId}
                onChange={(e) => setFormData({ ...formData, glAccountId: e.target.value })}
                fullWidth
                required
              >
                <MenuItem value="">Select GL Account</MenuItem>
                {glAccounts.map((account) => (
                  <MenuItem key={account.id} value={account.id}>
                    {account.accountCode} - {account.accountName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Opening Balance"
                type="number"
                value={formData.openingBalance}
                onChange={(e) => {
                  const value = parseFloat(e.target.value) || 0;
                  setFormData({ ...formData, openingBalance: value, currentBalance: value });
                }}
                fullWidth
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Status"
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                fullWidth
              >
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
                <MenuItem value="CLOSED">Closed</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Notes"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                fullWidth
                multiline
                rows={3}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={handleSaveAccount}
            disabled={!formData.accountName || !formData.accountNumber || !formData.glAccountId}
          >
            Create Account
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BankAccounts;
