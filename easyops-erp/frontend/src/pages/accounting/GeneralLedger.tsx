import React, { useState, useEffect } from 'react';
import {
  Box,
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
  MenuItem,
  Button,
  Alert,
  Grid,
  Paper,
} from '@mui/material';
import {
  Print as PrintIcon,
  Download as DownloadIcon,
  Search as SearchIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { GeneralLedgerEntry, ChartOfAccount } from '../../types/accounting';
import { exportGeneralLedgerToExcel } from '../../utils/excelExport';

const GeneralLedger: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [entries, setEntries] = useState<GeneralLedgerEntry[]>([]);
  const [accounts, setAccounts] = useState<ChartOfAccount[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [startDate, setStartDate] = useState(new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadAccounts();
    }
  }, [organizationId]);

  const loadAccounts = async () => {
    try {
      const data = await accountingService.getAccounts(organizationId);
      const postingAccounts = data.filter(a => !a.isGroup);
      setAccounts(postingAccounts);
      if (postingAccounts.length > 0) {
        setSelectedAccountId(postingAccounts[0].id);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load accounts');
    }
  };

  const handleSearch = async () => {
    if (!selectedAccountId) {
      setError('Please select an account');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getGeneralLedger(
        organizationId,
        selectedAccountId,
        startDate,
        endDate
      );
      setEntries(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load general ledger');
    } finally {
      setLoading(false);
    }
  };

  const selectedAccount = accounts.find(a => a.id === selectedAccountId);

  const totalDebit = entries.reduce((sum, entry) => sum + (entry.debit || 0), 0);
  const totalCredit = entries.reduce((sum, entry) => sum + (entry.credit || 0), 0);
  const balance = entries.length > 0 ? entries[entries.length - 1].runningBalance : 0;

  const handlePrint = () => {
    window.print();
  };

  const handleExport = () => {
    const exportData = entries.map(entry => ({
      date: entry.transactionDate,
      accountCode: selectedAccount?.accountCode || '',
      accountName: selectedAccount?.accountName || '',
      reference: entry.journalNumber,
      description: entry.description,
      debit: entry.debit,
      credit: entry.credit,
      balance: entry.runningBalance,
    }));
    
    exportGeneralLedgerToExcel(exportData);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        General Ledger
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Account"
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
              >
                {accounts.map((account) => (
                  <MenuItem key={account.id} value={account.id}>
                    {account.accountCode} - {account.accountName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                type="date"
                fullWidth
                label="Start Date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                type="date"
                fullWidth
                label="End Date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<SearchIcon />}
                onClick={handleSearch}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                Search
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Report Header */}
      {entries.length > 0 && (
        <>
          <Paper sx={{ mb: 2, p: 2 }}>
            <Typography variant="h6" align="center">
              General Ledger Report
            </Typography>
            <Typography align="center" color="text.secondary">
              Account: {selectedAccount?.accountCode} - {selectedAccount?.accountName}
            </Typography>
            <Typography align="center" color="text.secondary">
              Period: {startDate} to {endDate}
            </Typography>
          </Paper>

          <Box sx={{ mb: 2, display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            <Button startIcon={<PrintIcon />} onClick={handlePrint} variant="outlined">
              Print
            </Button>
            <Button startIcon={<DownloadIcon />} onClick={handleExport} variant="outlined">
              Export CSV
            </Button>
          </Box>
        </>
      )}

      {/* General Ledger Table */}
      <Card>
        <CardContent>
          {loading ? (
            <Typography align="center" sx={{ py: 4 }}>
              Loading...
            </Typography>
          ) : entries.length === 0 ? (
            <Typography align="center" sx={{ py: 4 }} color="text.secondary">
              No transactions found. Select an account and click Search.
            </Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Journal #</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell align="right">Debit</TableCell>
                    <TableCell align="right">Credit</TableCell>
                    <TableCell align="right">Balance</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {entries.map((entry) => (
                    <TableRow key={entry.journalLineId}>
                      <TableCell>{entry.transactionDate}</TableCell>
                      <TableCell>{entry.journalNumber}</TableCell>
                      <TableCell>{entry.description}</TableCell>
                      <TableCell align="right">
                        {entry.debit > 0 ? entry.debit.toLocaleString(undefined, { minimumFractionDigits: 2 }) : ''}
                      </TableCell>
                      <TableCell align="right">
                        {entry.credit > 0 ? entry.credit.toLocaleString(undefined, { minimumFractionDigits: 2 }) : ''}
                      </TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                        {entry.runningBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  {/* Totals Row */}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell colSpan={3} sx={{ fontWeight: 'bold' }}>
                      Total
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                      {totalDebit.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                      {totalCredit.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                      {balance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default GeneralLedger;

