import React, { useState } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { IncomeExpenseReport } from '../../services/pharmaService';

const IncomeExpenseReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [report, setReport] = useState<IncomeExpenseReport | null>(null);
  
  const [filters, setFilters] = useState({
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  const handleGenerate = async () => {
    if (!currentOrganizationId) {
      setError('Organization ID is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getIncomeExpenseReport(
        currentOrganizationId,
        filters.startDate,
        filters.endDate
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
        Income & Expense Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="date"
                label="Start Date"
                value={filters.startDate}
                onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="date"
                label="End Date"
                value={filters.endDate}
                onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
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
                INCOME & EXPENSE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Date Range: {new Date(report.startDate).toLocaleDateString()} to {new Date(report.endDate).toLocaleDateString()}
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

          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} md={4}>
              <Card sx={{ bgcolor: 'success.light', color: 'success.contrastText' }}>
                <CardContent>
                  <Typography variant="h6">Total Income</Typography>
                  <Typography variant="h4">{formatCurrency(report.totalIncome)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card sx={{ bgcolor: 'error.light', color: 'error.contrastText' }}>
                <CardContent>
                  <Typography variant="h6">Total Expenses</Typography>
                  <Typography variant="h4">{formatCurrency(report.totalExpenses)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card sx={{ bgcolor: report.netIncome >= 0 ? 'primary.light' : 'error.light', color: 'primary.contrastText' }}>
                <CardContent>
                  <Typography variant="h6">Net Income</Typography>
                  <Typography variant="h4">{formatCurrency(report.netIncome)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Typography variant="h6" gutterBottom sx={{ mt: 3 }}>
            Area-Wise Details
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Area Name</TableCell>
                  <TableCell align="right">Income</TableCell>
                  <TableCell align="right">Expenses</TableCell>
                  <TableCell align="right">Net Income</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.areaDetails.map((area, index) => (
                  <TableRow key={index}>
                    <TableCell>{area.areaName}</TableCell>
                    <TableCell align="right">{formatCurrency(area.income)}</TableCell>
                    <TableCell align="right">{formatCurrency(area.expenses)}</TableCell>
                    <TableCell align="right">
                      <Typography
                        variant="body2"
                        color={area.netIncome >= 0 ? 'success.main' : 'error.main'}
                        fontWeight="bold"
                      >
                        {formatCurrency(area.netIncome)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Box>
  );
};

export default IncomeExpenseReportPage;

