import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Divider
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { AreaWiseAllocationReport, Area } from '../../services/pharmaService';

const AreaAllocationReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [report, setReport] = useState<AreaWiseAllocationReport | null>(null);
  
  const [filters, setFilters] = useState({
    areaId: '',
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
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
      const data = await pharmaService.getAreaWiseAllocationReport(
        currentOrganizationId,
        filters.startDate,
        filters.endDate,
        filters.areaId || undefined
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

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Area-Wise Allocation Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
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
                type="date"
                label="Start Date"
                value={filters.startDate}
                onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="date"
                label="End Date"
                value={filters.endDate}
                onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
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
                AREA-WISE ALLOCATION REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Date Range: {formatDate(report.startDate)} to {formatDate(report.endDate)}
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

          {report.areas.map((area, areaIndex) => (
            <Box key={areaIndex} sx={{ mb: 4 }}>
              <Typography variant="h6" gutterBottom>
                Area: {area.areaName}
              </Typography>
              {area.receivingEmployeeName && (
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Receiving Employee: {area.receivingEmployeeName}
                </Typography>
              )}
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Total Allocations: {area.totalAllocations}
              </Typography>

              <TableContainer sx={{ mt: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product Name</TableCell>
                      <TableCell align="right">Total Quantity</TableCell>
                      <TableCell align="right">Total Amount</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {area.productDetails.map((product, prodIndex) => (
                      <TableRow key={prodIndex}>
                        <TableCell>{product.productName}</TableCell>
                        <TableCell align="right">{product.totalQuantityAllocated.toFixed(2)}</TableCell>
                        <TableCell align="right">{formatCurrency(product.totalAmount)}</TableCell>
                      </TableRow>
                    ))}
                    <TableRow sx={{ fontWeight: 'bold' }}>
                      <TableCell>Total</TableCell>
                      <TableCell align="right">
                        {area.productDetails.reduce((sum, p) => sum + p.totalQuantityAllocated, 0).toFixed(2)}
                      </TableCell>
                      <TableCell align="right">
                        {formatCurrency(area.totalAllocationAmount)}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>

              {area.allocationDates && area.allocationDates.length > 0 && (
                <Typography variant="body2" sx={{ mt: 2, color: 'text.secondary' }}>
                  Allocation Dates: {area.allocationDates.map(d => formatDate(d)).join(' | ')}
                </Typography>
              )}

              {areaIndex < report.areas.length - 1 && <Divider sx={{ mt: 3 }} />}
            </Box>
          ))}
        </Paper>
      )}
    </Box>
  );
};

export default AreaAllocationReportPage;

