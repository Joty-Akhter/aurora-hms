import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Chip
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import { Print as PrintIcon, GetApp as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { TerritoryAnalytics, Territory } from '../../services/pharmaService';

const TerritoryPerformanceReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [report, setReport] = useState<TerritoryAnalytics | null>(null);

  const [filters, setFilters] = useState({
    territoryId: '',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadTerritories();
    }
  }, [currentOrganizationId]);

  const loadTerritories = async () => {
    if (!currentOrganizationId) return;
    try {
      const data = await pharmaService.getAllTerritoriesForOrganization(currentOrganizationId);
      setTerritories(data);
    } catch (err) {
      console.error('Failed to load territories:', err);
    }
  };

  const handleGenerate = async () => {
    if (!currentOrganizationId || !filters.territoryId) {
      setError('Please select a territory');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getTerritoryAnalytics(
        filters.territoryId,
        filters.year,
        filters.month
      );
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to generate report');
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
    if (!report) return;
    window.print();
  };

  const handleExportExcel = () => {
    if (!report) return;
    const s = report.summary;
    const rows = [
      ['Metric', 'Value'],
      ['Territory', report.territoryName],
      ['Period', `${filters.year}-${String(filters.month).padStart(2, '0')}`],
      ['Total Employees', String(s.totalEmployees)],
      ['Total Target', String(s.totalTarget)],
      ['Total Covered', String(s.totalCovered)],
      ['Target Achievement %', `${s.targetAchievementRate?.toFixed(2) ?? 0}%`],
      ['Total Expenses', String(s.totalExpenses)],
      ['Expense Ratio %', `${s.expenseRatio?.toFixed(2) ?? 0}%`],
      ['Total Incentives', String(s.totalIncentives)],
      ['Total Deposits', String(s.totalDeposits)],
      ['Efficiency Score', String(s.territoryEfficiencyScore)],
      ['Target Achieved', s.targetAchieved ? 'Yes' : 'No'],
      ['Incentive Eligible', s.incentiveEligible ? 'Yes' : 'No']
    ];
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `territory-performance-${report.territoryName}-${filters.year}-${filters.month}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Territory Performance Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TerritoryTreeSelector
                label="Territory"
                value={filters.territoryId || ''}
                onChange={(territoryId) => setFilters({ ...filters, territoryId: territoryId })}
                required
              />
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
        <Paper sx={{ p: 3 }} className="print-only-content">
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, flexWrap: 'wrap', gap: 2 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                TERRITORY PERFORMANCE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {new Date(filters.year, filters.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' })}
              </Typography>
              <Typography variant="body2">Territory: {report.territoryName}</Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button startIcon={<PrintIcon />} onClick={handleExportPDF}>
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
                  <Typography variant="body1">
                    Total Target: <strong>{formatCurrency(Number(report.summary.totalTarget ?? 0))}</strong>
                  </Typography>
                  <Typography variant="body1">
                    Total Covered: <strong>{formatCurrency(Number(report.summary.totalCovered ?? 0))}</strong>
                  </Typography>
                  <Typography variant="body1">
                    Achievement Rate: <strong>{(report.summary.targetAchievementRate ?? 0).toFixed(2)}%</strong>
                  </Typography>
                  <Box sx={{ mt: 1 }}>
                    <Chip
                      label={report.summary.targetAchieved ? 'Target Achieved' : 'Not Achieved'}
                      color={report.summary.targetAchieved ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>
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
                    Total Expenses: <strong>{formatCurrency(Number(report.summary.totalExpenses ?? 0))}</strong>
                  </Typography>
                  <Typography variant="body1">
                    Expense Ratio: <strong>{(report.summary.expenseRatio ?? 0).toFixed(2)}%</strong>
                  </Typography>
                  <Typography variant="body1">
                    Total Incentives: <strong>{formatCurrency(Number(report.summary.totalIncentives ?? 0))}</strong>
                  </Typography>
                  <Typography variant="body1">
                    Total Deposits: <strong>{formatCurrency(Number(report.summary.totalDeposits ?? 0))}</strong>
                  </Typography>
                  <Box sx={{ mt: 1 }}>
                    <Chip
                      label={report.summary.incentiveEligible ? 'Incentive Eligible' : 'Not Eligible'}
                      color={report.summary.incentiveEligible ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Summary Metrics</Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Metric</TableCell>
                      <TableCell align="right">Value</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <TableRow>
                      <TableCell>Total Employees</TableCell>
                      <TableCell align="right">{report.summary.totalEmployees ?? 0}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Territory Efficiency Score</TableCell>
                      <TableCell align="right">{(report.summary.territoryEfficiencyScore ?? 0).toFixed(2)}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Incentive Rate</TableCell>
                      <TableCell align="right">{(report.summary.incentiveRate ?? 0).toFixed(2)}%</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>

          {report.trend && (
            <Card sx={{ mt: 3 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Trend (vs Previous Month)</Typography>
                <Typography variant="body2">
                  Coverage Change: {(report.trend.coverageChangePercent ?? 0).toFixed(2)}% | Direction: {report.trend.trendDirection ?? 'N/A'}
                </Typography>
              </CardContent>
            </Card>
          )}
        </Paper>
      )}
    </Box>
  );
};

export default TerritoryPerformanceReportPage;
