import React, { useState } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Chip
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { AccountsBalanceReport } from '../../services/pharmaService';

const AccountsBalanceReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [report, setReport] = useState<AccountsBalanceReport | null>(null);
  
  const [filters, setFilters] = useState({
    asOfDate: new Date().toISOString().split('T')[0]
  });

  const handleGenerate = async () => {
    if (!currentOrganizationId) {
      setError('Organization ID is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getAccountsBalanceReport(
        currentOrganizationId,
        filters.asOfDate
      );
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-BD', {
      style: 'currency',
      currency: 'BDT',
      minimumFractionDigits: 2
    }).format(amount);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Accounts Balance Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="date"
                label="As of Date"
                value={filters.asOfDate}
                onChange={(e) => setFilters({ ...filters, asOfDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleGenerate}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                {loading ? <CircularProgress size={24} /> : 'Generate Report'}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {report && (
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                ACCOUNTS BALANCE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                As of Date: {new Date(report.asOfDate).toLocaleDateString()}
              </Typography>
            </Box>
            <Box>
              <Button startIcon={<PrintIcon />} onClick={() => window.print()} sx={{ mr: 1 }}>
                Print
              </Button>
              <Button startIcon={<DownloadIcon />} variant="outlined">
                Download
              </Button>
            </Box>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Area Name</TableCell>
                  <TableCell align="right">Products Supplied</TableCell>
                  <TableCell align="right">Deposits Received</TableCell>
                  <TableCell align="right">Due Amount</TableCell>
                  <TableCell align="right">Overdue Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.areaBalances.map((area, index) => (
                  <TableRow key={index}>
                    <TableCell>{area.areaName}</TableCell>
                    <TableCell align="right">{formatCurrency(area.totalProductsSupplied)}</TableCell>
                    <TableCell align="right">{formatCurrency(area.totalDepositsReceived)}</TableCell>
                    <TableCell align="right">
                      <Chip
                        label={formatCurrency(area.dueAmount)}
                        color={area.dueAmount > 0 ? 'error' : 'success'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {area.overdueAmount > 0 ? (
                        <Chip
                          label={formatCurrency(area.overdueAmount)}
                          color="error"
                          size="small"
                        />
                      ) : (
                        '-'
                      )}
                    </TableCell>
                  </TableRow>
                ))}
                <TableRow sx={{ fontWeight: 'bold', bgcolor: 'grey.100' }}>
                  <TableCell>Total</TableCell>
                  <TableCell align="right">
                    {formatCurrency(
                      report.areaBalances.reduce((sum, a) => sum + a.totalProductsSupplied, 0)
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(
                      report.areaBalances.reduce((sum, a) => sum + a.totalDepositsReceived, 0)
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Chip
                      label={formatCurrency(report.totalDueAmount)}
                      color={report.totalDueAmount > 0 ? 'error' : 'success'}
                      size="medium"
                    />
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(
                      report.areaBalances.reduce((sum, a) => sum + a.overdueAmount, 0)
                    )}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Box>
  );
};

export default AccountsBalanceReportPage;

