import React, { useState } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { InStockProductWiseReport } from '../../services/pharmaService';

const InStockProductWiseReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [report, setReport] = useState<InStockProductWiseReport | null>(null);
  
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
      const data = await pharmaService.getInStockProductWiseReport(
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
        In-Stock Product-Wise Report
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
                IN-STOCK PRODUCT-WISE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Date Range: {new Date(report.startDate).toLocaleDateString()} to {new Date(report.endDate).toLocaleDateString()}
              </Typography>
              <Typography variant="body2">
                Location: {report.location}
              </Typography>
              <Typography variant="body2">
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
                  <TableCell>Product Name</TableCell>
                  <TableCell align="right">Pack Size</TableCell>
                  <TableCell align="right">TP+VAT</TableCell>
                  <TableCell align="right">Quantity</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.products.map((product, index) => (
                  <TableRow key={index}>
                    <TableCell>{product.productName}</TableCell>
                    <TableCell align="right">{product.packSize?.toFixed(2) || '-'}</TableCell>
                    <TableCell align="right">{formatCurrency(product.tpWithVat || 0)}</TableCell>
                    <TableCell align="right">{product.quantity.toFixed(2)}</TableCell>
                    <TableCell align="right">{formatCurrency(product.amount || 0)}</TableCell>
                  </TableRow>
                ))}
                <TableRow sx={{ fontWeight: 'bold', bgcolor: 'grey.100' }}>
                  <TableCell colSpan={4}>Total In-Stock Amount</TableCell>
                  <TableCell align="right">
                    <Typography variant="h6">
                      {formatCurrency(report.totalInStockAmount)}
                    </Typography>
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

export default InStockProductWiseReportPage;

