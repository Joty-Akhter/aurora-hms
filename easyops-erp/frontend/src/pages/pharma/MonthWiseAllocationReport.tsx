import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Tabs, Tab, Chip
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { 
  MonthWiseAllocationReport, 
  AnnualAllocationReport, 
  Area 
} from '../../services/pharmaService';

const MonthWiseAllocationReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [reportType, setReportType] = useState<'month-wise' | 'annual'>('month-wise');
  const [monthWiseReport, setMonthWiseReport] = useState<MonthWiseAllocationReport | null>(null);
  const [annualReport, setAnnualReport] = useState<AnnualAllocationReport | null>(null);
  
  const [filters, setFilters] = useState({
    areaId: '',
    year: new Date().getFullYear()
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
    if (!currentOrganizationId) {
      setError('Organization ID is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      if (reportType === 'month-wise') {
        const data = await pharmaService.getMonthWiseAllocationReport(
          currentOrganizationId,
          filters.year,
          filters.areaId || undefined
        );
        setMonthWiseReport(data);
        setAnnualReport(null);
      } else {
        const data = await pharmaService.getAnnualAllocationReport(
          currentOrganizationId,
          filters.year,
          filters.areaId || undefined
        );
        setAnnualReport(data);
        setMonthWiseReport(null);
      }
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

  const getMonthName = (month: number) => {
    return new Date(2000, month - 1).toLocaleString('default', { month: 'long' });
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Month-Wise / Annual Allocation Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs 
            value={reportType} 
            onChange={(e, newValue) => {
              setReportType(newValue);
              setMonthWiseReport(null);
              setAnnualReport(null);
            }}
            sx={{ mb: 3 }}
          >
            <Tab label="Month-Wise" value="month-wise" />
            <Tab label="Annual" value="annual" />
          </Tabs>

          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Area (Optional)"
                value={filters.areaId}
                onChange={(e) => setFilters({ ...filters, areaId: e.target.value })}
              >
                <MenuItem value="">All Areas</MenuItem>
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

      {reportType === 'month-wise' && monthWiseReport && (
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                MONTH-WISE ALLOCATION REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Year: {monthWiseReport.year}
              </Typography>
              {filters.areaId && (
                <Typography variant="body2">
                  Area: {areas.find(a => a.id === filters.areaId)?.name}
                </Typography>
              )}
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
                  <TableCell>Month</TableCell>
                  <TableCell align="right">Total Quantity</TableCell>
                  <TableCell align="right">Total Amount</TableCell>
                  <TableCell align="right">No. of Allocations</TableCell>
                  <TableCell align="right">No. of Areas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {monthWiseReport.monthlyDetails.map((detail, index) => (
                  <TableRow key={index}>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {getMonthName(detail.month)} {detail.year}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">{detail.totalQuantity.toFixed(2)}</TableCell>
                    <TableCell align="right">{formatCurrency(detail.totalAmount)}</TableCell>
                    <TableCell align="right">{detail.numberOfAllocations}</TableCell>
                    <TableCell align="right">{detail.numberOfAreas}</TableCell>
                  </TableRow>
                ))}
                <TableRow sx={{ fontWeight: 'bold', bgcolor: 'grey.100' }}>
                  <TableCell>Grand Total</TableCell>
                  <TableCell align="right">
                    {monthWiseReport.grandTotalQuantity.toFixed(2)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(monthWiseReport.grandTotalAmount)}
                  </TableCell>
                  <TableCell align="right">
                    {monthWiseReport.grandTotalAllocations}
                  </TableCell>
                  <TableCell align="right">-</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}

      {reportType === 'annual' && annualReport && (
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                ANNUAL ALLOCATION REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Year: {annualReport.year}
              </Typography>
              {filters.areaId && (
                <Typography variant="body2">
                  Area: {areas.find(a => a.id === filters.areaId)?.name}
                </Typography>
              )}
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

          <Grid container spacing={3} sx={{ mb: 4 }}>
            <Grid item xs={12} md={3}>
              <Card sx={{ bgcolor: 'primary.light', color: 'primary.contrastText' }}>
                <CardContent>
                  <Typography variant="body2">Total Allocations</Typography>
                  <Typography variant="h5">{annualReport.totalAllocations}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card sx={{ bgcolor: 'success.light', color: 'success.contrastText' }}>
                <CardContent>
                  <Typography variant="body2">Total Quantity</Typography>
                  <Typography variant="h5">{annualReport.totalQuantity.toFixed(2)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card sx={{ bgcolor: 'info.light', color: 'info.contrastText' }}>
                <CardContent>
                  <Typography variant="body2">Total Amount</Typography>
                  <Typography variant="h5">{formatCurrency(annualReport.totalAmount)}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card sx={{ bgcolor: 'warning.light', color: 'warning.contrastText' }}>
                <CardContent>
                  <Typography variant="body2">Avg Monthly</Typography>
                  <Typography variant="h5">{formatCurrency(annualReport.averageMonthlyAllocation)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {annualReport.peakMonth && (
            <Box sx={{ mb: 3, p: 2, bgcolor: 'success.light', borderRadius: 2 }}>
              <Typography variant="body1" color="success.contrastText">
                <strong>Peak Month:</strong> {annualReport.peakMonth} ({formatCurrency(
                  annualReport.monthlyBreakdown.find(m => 
                    new Date(m.year, m.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' }) === annualReport.peakMonth
                  )?.totalAmount || 0
                )})
              </Typography>
            </Box>
          )}

          <Typography variant="h6" gutterBottom sx={{ mt: 3, mb: 2 }}>
            Month-Wise Breakdown
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Month</TableCell>
                  <TableCell align="right">Total Quantity</TableCell>
                  <TableCell align="right">Total Amount</TableCell>
                  <TableCell align="right">No. of Allocations</TableCell>
                  <TableCell align="right">No. of Areas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {annualReport.monthlyBreakdown.map((detail, index) => (
                  <TableRow key={index}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2" fontWeight="medium">
                          {getMonthName(detail.month)} {detail.year}
                        </Typography>
                        {annualReport.peakMonth && 
                         new Date(detail.year, detail.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' }) === annualReport.peakMonth && (
                          <Chip label="Peak" color="success" size="small" />
                        )}
                      </Box>
                    </TableCell>
                    <TableCell align="right">{detail.totalQuantity.toFixed(2)}</TableCell>
                    <TableCell align="right">{formatCurrency(detail.totalAmount)}</TableCell>
                    <TableCell align="right">{detail.numberOfAllocations}</TableCell>
                    <TableCell align="right">{detail.numberOfAreas}</TableCell>
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

export default MonthWiseAllocationReportPage;

