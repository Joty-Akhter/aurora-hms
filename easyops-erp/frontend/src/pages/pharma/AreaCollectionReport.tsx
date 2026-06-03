import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Divider, Chip
} from '@mui/material';
import { Print as PrintIcon, GetApp as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { AreaWiseCollectionReport, Area } from '../../services/pharmaService';

const AreaCollectionReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [report, setReport] = useState<AreaWiseCollectionReport | null>(null);
  
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
      const data = await pharmaService.getAreaWiseCollectionReport(
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

  const handleExportPDF = () => {
    if (!report) return;
    window.print();
  };

  const handleExportExcel = () => {
    if (!report) return;
    // Create CSV content
    const headers = ['Area Name', 'Number of Deposits', 'Total Collection', 'Target Amount', 'Coverage %'];
    const rows = report.areas.map(area => [
      area.areaName,
      area.numberOfDeposits.toString(),
      area.totalCollectionAmount.toFixed(2),
      area.targetAmount?.toFixed(2) || 'N/A',
      area.coveragePercentage?.toFixed(2) || 'N/A'
    ]);
    
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `area-wise-collection-report-${filters.startDate}-to-${filters.endDate}.csv`;
    link.click();
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Area-Wise Collection Report
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
                AREA-WISE COLLECTION REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Date Range: {formatDate(report.startDate)} to {formatDate(report.endDate)}
              </Typography>
            </Box>
            <Box>
              <Button startIcon={<PrintIcon />} onClick={handleExportPDF} sx={{ mr: 1 }}>
                Print/PDF
              </Button>
              <Button startIcon={<DownloadIcon />} variant="outlined" onClick={handleExportExcel}>
                Export Excel
              </Button>
            </Box>
          </Box>

          {report.areas.map((area, areaIndex) => (
            <Box key={areaIndex} sx={{ mb: 4 }}>
              <Typography variant="h6" gutterBottom>
                Area: {area.areaName}
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2">
                    Number of Deposits: <strong>{area.numberOfDeposits}</strong>
                  </Typography>
                  <Typography variant="body2">
                    Total Collection: <strong>{formatCurrency(area.totalCollectionAmount)}</strong>
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  {area.targetAmount !== undefined && (
                    <>
                      <Typography variant="body2">
                        Target Amount: <strong>{formatCurrency(area.targetAmount)}</strong>
                      </Typography>
                      {area.coveragePercentage !== undefined && (
                        <Box sx={{ mt: 1 }}>
                          <Typography variant="body2" component="span">Coverage: </Typography>
                          <Chip
                            label={`${area.coveragePercentage.toFixed(2)}%`}
                            color={area.coveragePercentage >= 100 ? 'success' : 'warning'}
                            size="small"
                          />
                        </Box>
                      )}
                    </>
                  )}
                </Grid>
              </Grid>

              {area.deposits && area.deposits.length > 0 && (
                <TableContainer sx={{ mt: 2 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Date</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell>Status</TableCell>
                        {area.deposits.some(d => d.collectedBy) && (
                          <TableCell>Collected By</TableCell>
                        )}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {area.deposits.map((deposit, depIndex) => (
                        <TableRow key={depIndex}>
                          <TableCell>{formatDate(deposit.depositDate)}</TableCell>
                          <TableCell align="right">{formatCurrency(deposit.depositAmount)}</TableCell>
                          <TableCell>
                            <Chip
                              label={deposit.status}
                              color={deposit.status === 'COMPLETED' ? 'success' : 'default'}
                              size="small"
                            />
                          </TableCell>
                          {area.deposits.some(d => d.collectedBy) && (
                            <TableCell>{deposit.collectedBy || '-'}</TableCell>
                          )}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}

              {areaIndex < report.areas.length - 1 && <Divider sx={{ mt: 3 }} />}
            </Box>
          ))}

          <Box sx={{ mt: 4, pt: 3, borderTop: 2, borderColor: 'divider' }}>
            <Typography variant="h6" align="right">
              Grand Total Collection: <strong>{formatCurrency(report.grandTotalCollection)}</strong>
            </Typography>
          </Box>
        </Paper>
      )}
    </Box>
  );
};

export default AreaCollectionReportPage;

