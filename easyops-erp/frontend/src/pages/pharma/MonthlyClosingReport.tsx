import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { MonthlyClosingReport, Area, Territory } from '../../services/pharmaService';

const MonthlyClosingReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [report, setReport] = useState<MonthlyClosingReport | null>(null);
  
  const [filters, setFilters] = useState<{
    areaId?: string;
    territoryId?: string;
    employeeId: string;
    year: number;
    month: number;
  }>({
    areaId: '',
    territoryId: '',
    employeeId: '',
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
      const [areasData, territoriesData] = await Promise.all([
        pharmaService.getAreas(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId)
      ]);
      setAreas(areasData.filter(a => a.isActive));
      setTerritories(territoriesData);
    } catch (error) {
      console.error('Failed to load areas/territories:', error);
    }
  };

  const handleGenerate = async () => {
    if (!currentOrganizationId || (!filters.areaId && !filters.territoryId)) {
      setError('Please select an area or territory');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const options = filters.territoryId
        ? { territoryId: filters.territoryId }
        : filters.areaId
          ? { areaId: filters.areaId }
          : {};
      const data = await pharmaService.getMonthlyClosingReport(
        currentOrganizationId,
        options,
        filters.year,
        filters.month,
        filters.employeeId || undefined
      );
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
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
        Monthly Closing Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Area (aggregates territories)"
                value={filters.areaId || ''}
                onChange={(e) => setFilters({ ...filters, areaId: e.target.value || undefined, territoryId: undefined })}
              >
                <MenuItem value="">-- None --</MenuItem>
                {areas.map((area) => (
                  <MenuItem key={area.id} value={area.id}>
                    {area.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={4}>
              <TerritoryTreeSelector
                label="Territory (specific)"
                value={filters.territoryId || ''}
                onChange={(territoryId) => setFilters({ ...filters, territoryId: territoryId || undefined, areaId: undefined })}
                allowClear
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                type="number"
                label="Year"
                value={filters.year}
                onChange={(e) => setFilters({ ...filters, year: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={2}>
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
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Employee ID (Optional)"
                value={filters.employeeId}
                onChange={(e) => setFilters({ ...filters, employeeId: e.target.value })}
              />
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
                MONTHLY CLOSING REPORT - SALES REPRESENTATIVE
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {new Date(report.year, report.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' })}
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
              {report.employeeId && (
                <Typography variant="body2">Employee: {report.employeeId} - {report.designation}</Typography>
              )}
            </Box>
            <Box>
              <Button startIcon={<PrintIcon />} onClick={handlePrint} sx={{ mr: 1 }}>
                Print
              </Button>
              <Button startIcon={<DownloadIcon />} variant="outlined">
                Download
              </Button>
            </Box>
          </Box>

          <Typography variant="h6" gutterBottom sx={{ mt: 3, mb: 2 }}>
            PRODUCT-WISE SUMMARY
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Product Name</TableCell>
                  <TableCell align="right">Opening</TableCell>
                  <TableCell align="right">Received</TableCell>
                  <TableCell align="right">Sold</TableCell>
                  <TableCell align="right">Adjusted</TableCell>
                  <TableCell align="right">Closing</TableCell>
                  <TableCell align="right">TP/Unit</TableCell>
                  <TableCell align="right">Closing Value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.productDetails.map((product, index) => (
                  <TableRow key={index}>
                    <TableCell>{product.productName}</TableCell>
                    <TableCell align="right">{product.openingBalance.toFixed(2)}</TableCell>
                    <TableCell align="right">{product.quantityReceived.toFixed(2)}</TableCell>
                    <TableCell align="right">{product.quantitySold.toFixed(2)}</TableCell>
                    <TableCell align="right">{product.quantityAdjusted.toFixed(2)}</TableCell>
                    <TableCell align="right">{product.closingBalance.toFixed(2)}</TableCell>
                    <TableCell align="right">{formatCurrency(product.tradePricePerUnit)}</TableCell>
                    <TableCell align="right">{formatCurrency(product.totalValue)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <Box sx={{ mt: 4 }}>
            <Typography variant="h6" gutterBottom>
              FINANCIAL SUMMARY
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Typography variant="body1">
                  Total Products Supplied Value: <strong>{formatCurrency(report.totalProductsSuppliedValue)}</strong>
                </Typography>
                <Typography variant="body1">
                  Total Deposits Received: <strong>{formatCurrency(report.totalDepositsReceived)}</strong>
                </Typography>
                <Typography variant="body1">
                  Due Amount: <strong>{formatCurrency(report.dueAmount)}</strong>
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                {report.targetAmount && (
                  <>
                    <Typography variant="body1">
                      Target Amount: <strong>{formatCurrency(report.targetAmount)}</strong>
                    </Typography>
                    {report.coveredAmount && (
                      <Typography variant="body1">
                        Covered Amount: <strong>{formatCurrency(report.coveredAmount)}</strong>
                      </Typography>
                    )}
                    {report.targetCoveragePercentage !== undefined && (
                      <Typography variant="body1">
                        Target Coverage: <strong>{report.targetCoveragePercentage.toFixed(2)}%</strong>
                      </Typography>
                    )}
                  </>
                )}
              </Grid>
            </Grid>
          </Box>

          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              PERFORMANCE INDICATORS
            </Typography>
            <Typography variant="body1">
              Target Achievement: <strong>{report.targetAchieved ? 'Yes' : 'No'}</strong>
            </Typography>
            {report.collectionEfficiency !== undefined && (
              <Typography variant="body1">
                Collection Efficiency: <strong>{report.collectionEfficiency.toFixed(2)}%</strong>
              </Typography>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};

export default MonthlyClosingReportPage;

