import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Chip
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { AreaPerformanceReport, Area } from '../../services/pharmaService';

const AreaPerformanceReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [report, setReport] = useState<AreaPerformanceReport | null>(null);
  
  const [filters, setFilters] = useState({
    areaId: '',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadAreas();
    }
  }, [currentOrganizationId]);

  const loadAreas = async () => {
    if (!currentOrganizationId) return;
    try {
      const data = await pharmaService.getAreas(currentOrganizationId);
      setAreas(data.filter(a => a.isActive));
    } catch (error) {
      console.error('Failed to load areas:', error);
    }
  };

  const handleGenerate = async () => {
    if (!currentOrganizationId || !filters.areaId) {
      setError('Please select an area');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getAreaPerformanceReport(
        currentOrganizationId,
        filters.areaId,
        filters.year,
        filters.month
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

  const handleExportPDF = () => {
    window.print();
  };

  const handleExportExcel = () => {
    if (!report) return;
    const rows = [
      ['Area Performance Report', report.areaName],
      ['Period', `${filters.year}-${String(filters.month).padStart(2, '0')}`],
      ['Target Amount', String(report.targetAmount ?? 0)],
      ['Covered Amount', String(report.coveredAmount ?? 0)],
      ['Coverage %', String(report.targetCoveragePercentage ?? 0)],
      ['Total Expenses', String(report.totalExpenses ?? 0)],
      ['Expense %', String(report.expensePercentage ?? 0)],
      ['Incentive Base', String(report.incentiveBaseAmount ?? 0)],
      ['Expense Within Limit', report.expenseWithinLimit ? 'Yes' : 'No'],
      ['Incentive Eligible', report.incentiveEligible ? 'Yes' : 'No'],
      [],
      ['Employee Performance'],
      ['Employee ID', 'Name', 'Role', 'Incentive Amount'],
      ...(report.employeePerformances?.map(e => [
        e.employeeIdCode || e.employeeId,
        e.employeeName || 'N/A',
        e.role,
        String(e.incentiveAmount ?? 0)
      ]) ?? [])
    ];
    const csv = rows.map(r => Array.isArray(r) ? r.map(c => `"${c}"`).join(',') : '').join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `area-performance-${report.areaName}-${filters.year}-${filters.month}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Area Performance Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Area"
                value={filters.areaId}
                onChange={(e) => setFilters({ ...filters, areaId: e.target.value })}
                required
              >
                {areas.map((area) => (
                  <MenuItem key={area.id} value={area.id}>
                    {area.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="number"
                label="Year"
                value={filters.year}
                onChange={(e) => setFilters({ ...filters, year: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Month"
                value={filters.month}
                onChange={(e) => setFilters({ ...filters, month: parseInt(e.target.value) })}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => (
                  <MenuItem key={m} value={m}>
                    {new Date(2000, m - 1).toLocaleString('default', { month: 'long' })}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleGenerate}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                {loading ? <CircularProgress size={24} /> : 'Generate'}
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
                AREA PERFORMANCE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {new Date(filters.year, filters.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' })}
              </Typography>
              {report.divisionName && (
                <Typography variant="body2">Division: {report.divisionName}</Typography>
              )}
              {report.regionName && (
                <Typography variant="body2">Region: {report.regionName}</Typography>
              )}
              {report.territoryName && (
                <Typography variant="body2">Territory: {report.territoryName}</Typography>
              )}
              <Typography variant="body2">Area: {report.areaName}</Typography>
            </Box>
            <Box>
              <Button startIcon={<PrintIcon />} onClick={handleExportPDF} sx={{ mr: 1 }}>
                Print
              </Button>
              <Button startIcon={<DownloadIcon />} variant="outlined" onClick={handleExportExcel}>
                Export Excel
              </Button>
            </Box>
          </Box>

          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Target & Coverage
                  </Typography>
                  {report.targetAmount !== undefined && (
                    <Typography variant="body1">
                      Target Amount: <strong>{formatCurrency(report.targetAmount)}</strong>
                    </Typography>
                  )}
                  {report.coveredAmount !== undefined && (
                    <Typography variant="body1">
                      Covered Amount: <strong>{formatCurrency(report.coveredAmount)}</strong>
                    </Typography>
                  )}
                  {report.targetCoveragePercentage !== undefined && (
                    <Typography variant="body1">
                      Coverage: <strong>{report.targetCoveragePercentage.toFixed(2)}%</strong>
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Expenses & Incentive
                  </Typography>
                  <Typography variant="body1">
                    Total Expenses: <strong>{formatCurrency(report.totalExpenses)}</strong>
                  </Typography>
                  {report.expensePercentage !== undefined && (
                    <Typography variant="body1">
                      Expense %: <strong>{report.expensePercentage.toFixed(2)}%</strong>
                    </Typography>
                  )}
                  <Box sx={{ mt: 1 }}>
                    <Chip
                      label={report.expenseWithinLimit ? 'Within Limit' : 'Exceeded Limit'}
                      color={report.expenseWithinLimit ? 'success' : 'error'}
                      size="small"
                    />
                  </Box>
                  {report.incentiveBaseAmount !== undefined && (
                    <Typography variant="body1" sx={{ mt: 1 }}>
                      Incentive Base: <strong>{formatCurrency(report.incentiveBaseAmount)}</strong>
                    </Typography>
                  )}
                  <Box sx={{ mt: 1 }}>
                    <Chip
                      label={report.incentiveEligible ? 'Eligible' : 'Not Eligible'}
                      color={report.incentiveEligible ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Typography variant="h6" gutterBottom sx={{ mt: 3, mb: 2 }}>
            Employee Performance
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell align="right">Incentive Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.employeePerformances.map((emp, index) => (
                  <TableRow key={index}>
                    <TableCell>{emp.employeeIdCode || emp.employeeId}</TableCell>
                    <TableCell>{emp.employeeName || 'N/A'}</TableCell>
                    <TableCell>{emp.role}</TableCell>
                    <TableCell align="right">{formatCurrency(emp.incentiveAmount)}</TableCell>
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

export default AreaPerformanceReportPage;

