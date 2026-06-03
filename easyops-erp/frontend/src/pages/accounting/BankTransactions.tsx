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
  FormControlLabel,
  Switch,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';
import { getApiErrorMessage } from '../../utils/apiError';
import { toBankAmount } from '../../utils/bankUtils';

const BankTransactions: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [transactions, setTransactions] = useState<any[]>([]);
  const [bankAccounts, setBankAccounts] = useState<any[]>([]);
  const [glAccounts, setGlAccounts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  
  const organizationId = currentOrganizationId || '';

  const [formData, setFormData] = useState({
    bankAccountId: '',
    transactionDate: new Date().toISOString().split('T')[0],
    transactionType: 'DEPOSIT',
    amount: 0,
    referenceNumber: '',
    description: '',
    offsetGlAccountId: '',
    postToGl: true,
  });

  useEffect(() => {
    if (organizationId) {
      loadBankAccounts();
      loadGlAccounts();
    }
  }, [organizationId]);

  const loadGlAccounts = async () => {
    try {
      const data = await accountingService.getPostingAccounts(organizationId);
      setGlAccounts(data);
    } catch {
      setGlAccounts([]);
    }
  };

  useEffect(() => {
    if (selectedAccountId) {
      loadTransactions();
    }
  }, [selectedAccountId]);

  const loadBankAccounts = async () => {
    try {
      const data = await accountingService.getBankAccounts(organizationId);
      setBankAccounts(data);
      if (data.length > 0 && !selectedAccountId) {
        setSelectedAccountId(data[0].id);
      }
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load bank accounts'));
    }
  };

  const loadTransactions = async (accountId?: string) => {
    const targetAccountId = accountId || selectedAccountId;
    if (!targetAccountId) return;

    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getBankTransactions(targetAccountId);
      setTransactions(data);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load transactions'));
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTransaction = async () => {
    setError(null);
    setSuccess(null);

    // Validation
    if (!formData.bankAccountId) {
      setError('Please select a bank account');
      return;
    }
    if (!formData.amount || formData.amount <= 0) {
      setError('Amount must be greater than 0');
      return;
    }
    try {
      const debitAmount =
        formData.transactionType === 'WITHDRAWAL' ? formData.amount : 0;
      const creditAmount =
        formData.transactionType === 'DEPOSIT' ? formData.amount : 0;

      const transactionData = {
        bankAccountId: formData.bankAccountId,
        transactionDate: formData.transactionDate,
        transactionType: formData.transactionType,
        debitAmount,
        creditAmount,
        referenceNumber: formData.referenceNumber,
        description: formData.description,
        postToGl: formData.postToGl,
        offsetGlAccountId: formData.postToGl ? formData.offsetGlAccountId : undefined,
      };

      await accountingService.createBankTransaction(transactionData);
      setSuccess('Transaction created successfully!');
      setOpenDialog(false);

      const createdAccountId = formData.bankAccountId;
      if (createdAccountId) {
        setSelectedAccountId(createdAccountId);
      }
      await loadBankAccounts();
      await loadTransactions(createdAccountId);

      // Reset form
      setFormData({
        bankAccountId: createdAccountId || selectedAccountId || '',
        transactionDate: new Date().toISOString().split('T')[0],
        transactionType: 'DEPOSIT',
        amount: 0,
        referenceNumber: '',
        description: '',
        offsetGlAccountId: glAccounts[0]?.id || '',
        postToGl: true,
      });
      
      loadTransactions();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to create transaction'));
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const getSelectedAccountName = () => {
    const account = bankAccounts.find(acc => acc.id === selectedAccountId);
    return account ? account.accountName : 'Select Account';
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Bank Transactions</Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setFormData((prev) => ({
                ...prev,
                bankAccountId: selectedAccountId || prev.bankAccountId,
              }));
              setOpenDialog(true);
            }}
            disabled={bankAccounts.length === 0}
          >
            Add Transaction
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

      {bankAccounts.length === 0 ? (
        <Alert severity="info" sx={{ mb: 2 }}>
          No bank accounts found. Please create a bank account first.
        </Alert>
      ) : (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={6}>
                <TextField
                  select
                  label="Bank Account"
                  value={selectedAccountId}
                  onChange={(e) => setSelectedAccountId(e.target.value)}
                  fullWidth
                >
                  {bankAccounts.map((account) => (
                    <MenuItem key={account.id} value={account.id}>
                      {account.accountName} - {account.accountNumber} ({formatCurrency(account.currentBalance || 0)})
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="body2" color="text.secondary">
                  Current Balance: <strong>{formatCurrency(
                    bankAccounts.find(acc => acc.id === selectedAccountId)?.currentBalance || 0
                  )}</strong>
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Transactions for {getSelectedAccountName()}
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Reference</TableCell>
                  <TableCell align="right">Debit</TableCell>
                  <TableCell align="right">Credit</TableCell>
                  <TableCell align="right">Balance</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2">Loading transactions...</Typography>
                    </TableCell>
                  </TableRow>
                ) : !selectedAccountId ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="textSecondary">
                        Please select a bank account to view transactions
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : transactions.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No transactions yet. Add your first transaction to get started!
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  transactions.map((txn) => (
                    <TableRow key={txn.id}>
                      <TableCell>{txn.transactionDate}</TableCell>
                      <TableCell>
                        <Chip 
                          label={txn.transactionType} 
                          size="small"
                          color={txn.transactionType === 'DEPOSIT' ? 'success' : 'error'}
                        />
                      </TableCell>
                      <TableCell>{txn.description}</TableCell>
                      <TableCell>{txn.referenceNumber || '-'}</TableCell>
                      <TableCell align="right">
                        {toBankAmount(txn.debitAmount) > 0
                          ? formatCurrency(toBankAmount(txn.debitAmount))
                          : '-'}
                      </TableCell>
                      <TableCell align="right">
                        {toBankAmount(txn.creditAmount) > 0
                          ? formatCurrency(toBankAmount(txn.creditAmount))
                          : '-'}
                      </TableCell>
                      <TableCell align="right">
                        <strong>{formatCurrency(toBankAmount(txn.runningBalance))}</strong>
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={txn.status} 
                          size="small"
                          color={txn.status === 'CLEARED' ? 'success' : 'default'}
                        />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Transaction Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Bank Transaction</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={1}>
            <Grid item xs={12}>
              <TextField
                select
                label="Bank Account"
                value={formData.bankAccountId}
                onChange={(e) => setFormData({ ...formData, bankAccountId: e.target.value })}
                fullWidth
                required
              >
                <MenuItem value="">Select Bank Account</MenuItem>
                {bankAccounts.map((account) => (
                  <MenuItem key={account.id} value={account.id}>
                    {account.accountName} - {account.accountNumber}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Transaction Date"
                type="date"
                value={formData.transactionDate}
                onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Transaction Type"
                value={formData.transactionType}
                onChange={(e) => setFormData({ ...formData, transactionType: e.target.value })}
                fullWidth
              >
                <MenuItem value="DEPOSIT">Deposit</MenuItem>
                <MenuItem value="WITHDRAWAL">Withdrawal</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Amount"
                type="number"
                value={formData.amount}
                onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })}
                fullWidth
                required
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                fullWidth
                multiline
                rows={2}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Reference Number"
                value={formData.referenceNumber}
                onChange={(e) => setFormData({ ...formData, referenceNumber: e.target.value })}
                fullWidth
              />
            </Grid>

            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.postToGl}
                    onChange={(e) => setFormData({ ...formData, postToGl: e.target.checked })}
                  />
                }
                label="Post to general ledger"
              />
            </Grid>

            {formData.postToGl && (
              <Grid item xs={12}>
                <TextField
                  select
                  label="Offset GL Account"
                  value={formData.offsetGlAccountId}
                  onChange={(e) => setFormData({ ...formData, offsetGlAccountId: e.target.value })}
                  fullWidth
                  required
                  helperText="Revenue, expense, or other account balancing the bank movement"
                >
                  {glAccounts.map((acc) => (
                    <MenuItem key={acc.id} value={acc.id}>
                      {acc.accountCode} - {acc.accountName}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            )}

          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={handleCreateTransaction}
            disabled={
              !formData.bankAccountId ||
              !formData.amount ||
              !['DEPOSIT', 'WITHDRAWAL'].includes(formData.transactionType) ||
              (formData.postToGl && !formData.offsetGlAccountId)
            }
          >
            Create Transaction
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BankTransactions;
