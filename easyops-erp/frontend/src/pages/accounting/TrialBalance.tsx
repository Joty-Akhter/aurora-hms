import React, { useEffect, useState } from 'react';
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
  Alert,
  TextField,
  MenuItem,
  Chip,
  Grid,
  CircularProgress,
} from '@mui/material';
import { Download as DownloadIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { TrialBalance as TrialBalanceType, AccountType, Period, FiscalYear } from '../../types/accounting';
import { exportTrialBalanceToExcel } from '../../utils/excelExport';

const TrialBalance: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [trialBalance, setTrialBalance] = useState<TrialBalanceType[]>([]);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [periodId, setPeriodId] = useState<string>('');
  const [selectedFiscalYear, setSelectedFiscalYear] = useState<string>('');
  
  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadFiscalYearsAndPeriods();
    }
  }, [organizationId]);

  useEffect(() => {
    if (periodId && organizationId) {
      loadTrialBalance();
    }
  }, [periodId]);

  const loadFiscalYearsAndPeriods = async () => {
    try {
      const [years, allPeriods] = await Promise.all([
        accountingService.getOpenFiscalYears(organizationId),
        accountingService.getOpenPeriods(organizationId)
      ]);
      
      setFiscalYears(years);
      setPeriods(allPeriods);
      
      // Auto-select current period based on today's date
      const today = new Date().toISOString().split('T')[0];
      const currentPeriod = allPeriods.find(
        p => p.startDate <= today && p.endDate >= today && p.status === 'OPEN'
      );
      
      if (currentPeriod) {
        setPeriodId(currentPeriod.id);
        setSelectedFiscalYear(currentPeriod.fiscalYearId);
      } else if (allPeriods.length > 0) {
        // If no current period, select the most recent open period
        setPeriodId(allPeriods[0].id);
        setSelectedFiscalYear(allPeriods[0].fiscalYearId);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load periods');
    }
  };

  const loadTrialBalance = async () => {
    if (!periodId) {
      setError('Please select a period');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getTrialBalance(organizationId, periodId);
      setTrialBalance(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load trial balance');
    } finally {
      setLoading(false);
    }
  };

  const handlePeriodChange = (newPeriodId: string) => {
    setPeriodId(newPeriodId);
    const selectedPeriod = periods.find(p => p.id === newPeriodId);
    if (selectedPeriod) {
      setSelectedFiscalYear(selectedPeriod.fiscalYearId);
    }
  };

  const filteredPeriods = selectedFiscalYear
    ? periods.filter(p => p.fiscalYearId === selectedFiscalYear)
    : periods;

  const calculateTotals = () => {
    return trialBalance.reduce(
      (acc, row) => ({
        openingBalance: acc.openingBalance + row.openingBalance,
        debitTotal: acc.debitTotal + row.debitTotal,
        creditTotal: acc.creditTotal + row.creditTotal,
        closingBalance: acc.closingBalance + row.closingBalance,
      }),
      { openingBalance: 0, debitTotal: 0, creditTotal: 0, closingBalance: 0 }
    );
  };

  const totals = trialBalance.length > 0 ? calculateTotals() : null;

  const getAccountTypeColor = (type: AccountType) => {
    const colors: Record<AccountType, string> = {
      ASSET: 'success',
      LIABILITY: 'error',
      EQUITY: 'info',
      REVENUE: 'primary',
      EXPENSE: 'warning',
    };
    return colors[type] || 'default';
  };

  const groupedByType = trialBalance.reduce((acc, row) => {
    if (!acc[row.accountType]) {
      acc[row.accountType] = [];
    }
    acc[row.accountType].push(row);
    return acc;
  }, {} as Record<string, TrialBalanceType[]>);

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Trial Balance</Typography>
        <Box display="flex" gap={2}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadTrialBalance}
            disabled={!periodId}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            disabled={trialBalance.length === 0}
            onClick={() => exportTrialBalanceToExcel(trialBalance)}
          >
            Export to Excel
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {!organizationId && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          No organization selected. Please log in and select an organization.
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Fiscal Year"
                value={selectedFiscalYear}
                onChange={(e) => {
                  setSelectedFiscalYear(e.target.value);
                  // Reset period selection when fiscal year changes
                  setPeriodId('');
                }}
                disabled={fiscalYears.length === 0}
              >
                <MenuItem value="">
                  <em>All Years</em>
                </MenuItem>
                {fiscalYears.map((fy) => (
                  <MenuItem key={fy.id} value={fy.id}>
                    {fy.yearCode} ({fy.startDate} to {fy.endDate})
                    {fy.isClosed && <Chip label="Closed" size="small" sx={{ ml: 1 }} />}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                select
                fullWidth
                label="Period"
                value={periodId}
                onChange={(e) => handlePeriodChange(e.target.value)}
                disabled={periods.length === 0}
                helperText={periods.length === 0 ? "No periods available. Please set up a fiscal year first." : ""}
              >
                <MenuItem value="">
                  <em>Select Period</em>
                </MenuItem>
                {filteredPeriods.map((period) => (
                  <MenuItem key={period.id} value={period.id}>
                    {period.periodName} ({period.startDate} to {period.endDate})
                    <Chip 
                      label={period.status} 
                      size="small" 
                      color={period.status === 'OPEN' ? 'success' : 'default'}
                      sx={{ ml: 1 }} 
                    />
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            
            <Grid item xs={12} md={2}>
              <Button
                variant="contained"
                onClick={loadTrialBalance}
                disabled={!periodId || loading}
                fullWidth
                startIcon={loading ? <CircularProgress size={20} /> : null}
              >
                {loading ? 'Loading...' : 'Generate'}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {trialBalance.length > 0 && (
        <Card>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Box>
                <Typography variant="h6" gutterBottom>
                  Trial Balance Report
                </Typography>
                {periodId && (
                  <Typography variant="body2" color="text.secondary">
                    Period: {periods.find(p => p.id === periodId)?.periodName || 'Unknown'} 
                    {' '}({periods.find(p => p.id === periodId)?.startDate} to {periods.find(p => p.id === periodId)?.endDate})
                  </Typography>
                )}
              </Box>
              <Chip 
                label={`${trialBalance.length} Accounts`} 
                color="primary" 
                variant="outlined"
              />
            </Box>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Account Code</strong></TableCell>
                    <TableCell><strong>Account Name</strong></TableCell>
                    <TableCell><strong>Type</strong></TableCell>
                    <TableCell align="right"><strong>Opening Balance</strong></TableCell>
                    <TableCell align="right"><strong>Debit</strong></TableCell>
                    <TableCell align="right"><strong>Credit</strong></TableCell>
                    <TableCell align="right"><strong>Closing Balance</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {Object.entries(groupedByType).map(([type, accounts]) => (
                    <React.Fragment key={type}>
                      <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                        <TableCell colSpan={7}>
                          <Chip
                            label={type}
                            color={getAccountTypeColor(type as AccountType) as any}
                            size="small"
                          />
                          <strong style={{ marginLeft: 8 }}>{type}</strong>
                        </TableCell>
                      </TableRow>
                      {accounts.map((row) => (
                        <TableRow key={row.accountCode}>
                          <TableCell>{row.accountCode}</TableCell>
                          <TableCell>{row.accountName}</TableCell>
                          <TableCell>
                            <Chip
                              label={row.accountType}
                              size="small"
                              color={getAccountTypeColor(row.accountType) as any}
                            />
                          </TableCell>
                          <TableCell align="right">
                            {row.openingBalance.toLocaleString(undefined, {
                              minimumFractionDigits: 2,
                              maximumFractionDigits: 2,
                            })}
                          </TableCell>
                          <TableCell align="right">
                            {row.debitTotal.toLocaleString(undefined, {
                              minimumFractionDigits: 2,
                              maximumFractionDigits: 2,
                            })}
                          </TableCell>
                          <TableCell align="right">
                            {row.creditTotal.toLocaleString(undefined, {
                              minimumFractionDigits: 2,
                              maximumFractionDigits: 2,
                            })}
                          </TableCell>
                          <TableCell align="right">
                            {row.closingBalance.toLocaleString(undefined, {
                              minimumFractionDigits: 2,
                              maximumFractionDigits: 2,
                            })}
                          </TableCell>
                        </TableRow>
                      ))}
                    </React.Fragment>
                  ))}
                  {totals && (
                    <TableRow sx={{ backgroundColor: '#fff9c4' }}>
                      <TableCell colSpan={3}>
                        <strong>GRAND TOTAL</strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>
                          {totals.openingBalance.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>
                          {totals.debitTotal.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>
                          {totals.creditTotal.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>
                          {totals.closingBalance.toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </strong>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            {totals && Math.abs(totals.debitTotal - totals.creditTotal) < 0.01 && (
              <Alert severity="success" sx={{ mt: 2 }}>
                ✓ Trial Balance is balanced! Debits ({totals.debitTotal.toFixed(2)}) = Credits ({totals.creditTotal.toFixed(2)})
              </Alert>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default TrialBalance;

