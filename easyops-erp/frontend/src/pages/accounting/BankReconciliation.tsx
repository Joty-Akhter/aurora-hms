import React, { useState, useEffect, useMemo } from 'react';
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
  TextField,
  Grid,
  Checkbox,
  Alert,
  Chip,
  CircularProgress,
  MenuItem,
  Paper,
} from '@mui/material';
import {
  Check as CheckIcon,
  AccountBalance as BankIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { getApiErrorMessage } from '../../utils/apiError';
import { toBankAmount, transactionNetAmount } from '../../utils/bankUtils';

const BankReconciliation: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [reconciliationData, setReconciliationData] = useState({
    bankAccountId: '',
    statementDate: new Date().toISOString().split('T')[0],
    openingBalance: 0,
    closingBalance: 0,
  });

  const [bankAccounts, setBankAccounts] = useState<any[]>([]);
  const [transactions, setTransactions] = useState<any[]>([]);
  const [selectedTransactions, setSelectedTransactions] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [reconciliationHistory, setReconciliationHistory] = useState<any[]>([]);

  const selectedAccount = useMemo(
    () => bankAccounts.find((acc) => acc.id === reconciliationData.bankAccountId),
    [bankAccounts, reconciliationData.bankAccountId]
  );

  /** Book balance on the server; reconciliation completes only when statement closing equals this. */
  const bookBalance = toBankAmount(selectedAccount?.currentBalance);

  useEffect(() => {
    if (currentOrganizationId) {
      loadBankAccounts();
    }
  }, [currentOrganizationId]);

  const loadReconciliationHistory = async () => {
    if (!currentOrganizationId || !reconciliationData.bankAccountId) return;
    try {
      const history = await accountingService.getBankReconciliations(
        currentOrganizationId,
        reconciliationData.bankAccountId
      );
      setReconciliationHistory(history);
    } catch {
      setReconciliationHistory([]);
    }
  };

  useEffect(() => {
    if (reconciliationData.bankAccountId) {
      loadUnreconciledTransactions();
      loadReconciliationHistory();
      const account = bankAccounts.find((acc) => acc.id === reconciliationData.bankAccountId);
      if (account) {
        const balance = toBankAmount(account.currentBalance);
        setReconciliationData((prev) => ({
          ...prev,
          openingBalance: balance,
          closingBalance: prev.closingBalance === 0 ? balance : prev.closingBalance,
        }));
      }
    }
  }, [reconciliationData.bankAccountId, bankAccounts]);

  const loadBankAccounts = async () => {
    try {
      setLoading(true);
      const accounts = await accountingService.getBankAccounts(currentOrganizationId || '');
      setBankAccounts(accounts);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load bank accounts'));
    } finally {
      setLoading(false);
    }
  };

  const loadUnreconciledTransactions = async () => {
    if (!reconciliationData.bankAccountId) return;
    try {
      setLoading(true);
      const unreconciled = await accountingService.getUnreconciledTransactions(
        reconciliationData.bankAccountId
      );
      setTransactions(unreconciled);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load transactions'));
    } finally {
      setLoading(false);
    }
  };

  const handleTransactionToggle = (transactionId: string) => {
    const newSelected = new Set(selectedTransactions);
    if (newSelected.has(transactionId)) {
      newSelected.delete(transactionId);
    } else {
      newSelected.add(transactionId);
    }
    setSelectedTransactions(newSelected);
  };

  const selectedTransactionsNet = () =>
    transactions
      .filter((t) => selectedTransactions.has(t.id))
      .reduce((sum, t) => sum + transactionNetAmount(t), 0);

  /** Backend: difference = closingBalance - account.currentBalance at create time. */
  const calculateDifference = () => reconciliationData.closingBalance - bookBalance;

  const handleCompleteReconciliation = async () => {
    setError(null);
    setSuccess(null);

    if (!reconciliationData.bankAccountId) {
      setError('Please select a bank account');
      return;
    }

    if (!user?.id) {
      setError('User session not found. Please sign in again.');
      return;
    }

    if (selectedTransactions.size === 0) {
      setError('Please select at least one transaction to reconcile');
      return;
    }

    const difference = calculateDifference();
    if (Math.abs(difference) > 0.01) {
      setError(
        `Statement closing (${formatCurrency(reconciliationData.closingBalance)}) must match book balance (${formatCurrency(bookBalance)}). Difference: ${formatCurrency(Math.abs(difference))}.`
      );
      return;
    }

    try {
      setLoading(true);

      const reconciliationRequest = {
        bankAccountId: reconciliationData.bankAccountId,
        statementDate: reconciliationData.statementDate,
        openingBalance: reconciliationData.openingBalance,
        closingBalance: reconciliationData.closingBalance,
        matchedTransactionIds: Array.from(selectedTransactions),
      };

      const reconciliation = await accountingService.createBankReconciliation(reconciliationRequest);
      await accountingService.completeBankReconciliation(reconciliation.id, user.id);

      setSuccess('Bank reconciliation completed successfully!');
      setSelectedTransactions(new Set());
      setReconciliationData((prev) => ({ ...prev, closingBalance: 0 }));
      await loadBankAccounts();
      await loadUnreconciledTransactions();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to complete reconciliation'));
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

  const difference = calculateDifference();
  const isBalanced = Math.abs(difference) < 0.01;

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center">
          <BankIcon sx={{ mr: 1, fontSize: 32 }} />
          <Typography variant="h4">Bank Reconciliation</Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadUnreconciledTransactions}
          disabled={!reconciliationData.bankAccountId || loading}
        >
          Refresh
        </Button>
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

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Reconciliation Details
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Closing balance on the statement must equal the current book balance before you can complete reconciliation.
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                select
                label="Bank Account"
                value={reconciliationData.bankAccountId}
                onChange={(e) =>
                  setReconciliationData({
                    ...reconciliationData,
                    bankAccountId: e.target.value,
                    closingBalance: 0,
                  })
                }
                fullWidth
                required
              >
                <MenuItem value="">Select Bank Account</MenuItem>
                {bankAccounts.map((account) => (
                  <MenuItem key={account.id} value={account.id}>
                    {account.accountName} - {account.accountNumber} (
                    {formatCurrency(toBankAmount(account.currentBalance))})
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                label="Statement Date"
                type="date"
                value={reconciliationData.statementDate}
                onChange={(e) =>
                  setReconciliationData({ ...reconciliationData, statementDate: e.target.value })
                }
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                label="Book Balance (system)"
                value={formatCurrency(bookBalance)}
                fullWidth
                InputProps={{ readOnly: true }}
                helperText="Must match statement closing to complete"
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                label="Closing Balance (Statement)"
                type="number"
                value={reconciliationData.closingBalance}
                onChange={(e) =>
                  setReconciliationData({
                    ...reconciliationData,
                    closingBalance: parseFloat(e.target.value) || 0,
                  })
                }
                fullWidth
                required
                inputProps={{ step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                label="Selected Transactions Net"
                value={formatCurrency(selectedTransactionsNet())}
                fullWidth
                InputProps={{ readOnly: true }}
                helperText={`${selectedTransactions.size} transaction(s) selected`}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {reconciliationData.bankAccountId && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2}>
              <Grid item xs={12} md={3}>
                <Paper elevation={0} sx={{ p: 2, bgcolor: '#f5f5f5' }}>
                  <Typography variant="caption" color="text.secondary">
                    Book Balance
                  </Typography>
                  <Typography variant="h6">{formatCurrency(bookBalance)}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} md={3}>
                <Paper elevation={0} sx={{ p: 2, bgcolor: '#f5f5f5' }}>
                  <Typography variant="caption" color="text.secondary">
                    Statement Closing
                  </Typography>
                  <Typography variant="h6">
                    {formatCurrency(reconciliationData.closingBalance)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} md={3}>
                <Paper elevation={0} sx={{ p: 2, bgcolor: '#f5f5f5' }}>
                  <Typography variant="caption" color="text.secondary">
                    Selected Net
                  </Typography>
                  <Typography variant="h6">{formatCurrency(selectedTransactionsNet())}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} md={3}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 2,
                    bgcolor: isBalanced ? '#e8f5e9' : '#ffebee',
                  }}
                >
                  <Typography variant="caption" color="text.secondary">
                    Difference (Statement − Book)
                  </Typography>
                  <Typography variant="h6" color={isBalanced ? 'success.main' : 'error.main'}>
                    {formatCurrency(difference)}
                    {isBalanced && <CheckIcon sx={{ ml: 1, verticalAlign: 'middle' }} />}
                  </Typography>
                </Paper>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Unreconciled Transactions ({transactions.length})</Typography>
            <Typography variant="body2" color="text.secondary">
              Selected: {selectedTransactions.size} transactions
            </Typography>
          </Box>

          {!reconciliationData.bankAccountId ? (
            <Alert severity="info">Please select a bank account to view unreconciled transactions.</Alert>
          ) : loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : transactions.length === 0 ? (
            <Alert severity="success">
              All transactions are reconciled! No unreconciled transactions found.
            </Alert>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox">Select</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Reference</TableCell>
                    <TableCell align="right">Debit</TableCell>
                    <TableCell align="right">Credit</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {transactions.map((txn) => (
                    <TableRow
                      key={txn.id}
                      selected={selectedTransactions.has(txn.id)}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => handleTransactionToggle(txn.id)}
                    >
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={selectedTransactions.has(txn.id)}
                          onChange={() => handleTransactionToggle(txn.id)}
                        />
                      </TableCell>
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
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {reconciliationData.bankAccountId && transactions.length > 0 && (
            <Box display="flex" justifyContent="flex-end" mt={3}>
              <Button
                variant="contained"
                color="primary"
                startIcon={<CheckIcon />}
                onClick={handleCompleteReconciliation}
                disabled={loading || selectedTransactions.size === 0 || !isBalanced}
              >
                Complete Reconciliation
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>

      {reconciliationData.bankAccountId && reconciliationHistory.length > 0 && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>Reconciliation History</Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Statement Date</TableCell>
                    <TableCell align="right">Opening</TableCell>
                    <TableCell align="right">Closing</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {reconciliationHistory.map((rec) => (
                    <TableRow key={rec.id}>
                      <TableCell>{rec.statementDate}</TableCell>
                      <TableCell align="right">{formatCurrency(rec.openingBalance ?? 0)}</TableCell>
                      <TableCell align="right">{formatCurrency(rec.closingBalance ?? 0)}</TableCell>
                      <TableCell><Chip label={rec.status} size="small" /></TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default BankReconciliation;
