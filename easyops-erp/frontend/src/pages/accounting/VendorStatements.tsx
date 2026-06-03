import React, { useState, useEffect } from 'react';
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
  Grid,
  MenuItem,
  TextField,
  Paper,
  CircularProgress,
  Divider,
} from '@mui/material';
import { PictureAsPdf as PdfIcon, Email as EmailIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';

const VendorStatements: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [vendors, setVendors] = useState<any[]>([]);
  const [selectedVendor, setSelectedVendor] = useState<string>('');
  const [startDate, setStartDate] = useState<string>(
    new Date(new Date().getFullYear(), new Date().getMonth() - 1, 1).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(
    new Date(new Date().getFullYear(), new Date().getMonth(), 0).toISOString().split('T')[0]
  );
  const [statement, setStatement] = useState<any | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadVendors();
    }
  }, [organizationId]);

  const loadVendors = async () => {
    try {
      const data = await accountingService.getAPVendors(organizationId, true);
      setVendors(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load vendors');
    }
  };

  const handleGenerateStatement = async () => {
    if (!selectedVendor) {
      setError('Please select a vendor');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const data = await accountingService.getVendorStatement(
        selectedVendor,
        startDate,
        endDate
      );
      setStatement(data);
      setSuccess('Statement generated successfully');
    } catch (err: any) {
      setError(err.message || 'Failed to generate statement');
      setStatement(null);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount || 0);
  };

  const handlePrintPDF = () => {
    window.print();
  };

  const handleEmailStatement = async () => {
    if (!selectedVendor || !statement) {
      setError('Please generate a statement first');
      return;
    }

    setError(null);
    setSuccess(null);

    try {
      await accountingService.emailVendorStatement(
        selectedVendor,
        startDate,
        endDate
      );
      setSuccess('Statement emailed successfully to vendor!');
    } catch (err: any) {
      setError(err.message || 'Failed to email statement');
    }
  };

  return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>
          Vendor Statements
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
            {success}
          </Alert>
        )}

        {/* Statement Parameters */}
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Statement Parameters
            </Typography>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={4}>
                <TextField
                  select
                  label="Vendor"
                  value={selectedVendor}
                  onChange={(e) => setSelectedVendor(e.target.value)}
                  fullWidth
                  required
                >
                  <MenuItem value="">Select Vendor</MenuItem>
                  {vendors.map((vendor) => (
                    <MenuItem key={vendor.id} value={vendor.id}>
                      {vendor.vendorName} ({vendor.vendorCode})
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} md={3}>
                <TextField
                  label="Start Date"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <TextField
                  label="End Date"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={2}>
                <Button
                  variant="contained"
                  onClick={handleGenerateStatement}
                  disabled={loading || !selectedVendor}
                  fullWidth
                >
                  {loading ? <CircularProgress size={24} /> : 'Generate'}
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Statement Display */}
        {statement && (
          <Card>
            <CardContent>
              {/* Statement Header */}
              <Box sx={{ mb: 3, '@media print': { mb: 2 } }}>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <Typography variant="h5" gutterBottom>
                      VENDOR STATEMENT
                    </Typography>
                    <Typography variant="body2">
                      <strong>Vendor:</strong> {statement.vendorName}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Vendor Code:</strong> {statement.vendorCode}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} md={6} textAlign="right">
                    <Typography variant="body2">
                      <strong>Statement Date:</strong> {statement.statementDate}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Period:</strong> {statement.periodStart} to {statement.periodEnd}
                    </Typography>
                  </Grid>
                </Grid>
              </Box>

              <Divider sx={{ mb: 2 }} />

              {/* Balance Summary */}
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={6} md={3}>
                  <Paper elevation={0} sx={{ p: 2, bgcolor: '#f5f5f5' }}>
                    <Typography variant="caption" color="text.secondary">Opening Balance</Typography>
                    <Typography variant="h6">{formatCurrency(statement.openingBalance)}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Paper elevation={0} sx={{ p: 2, bgcolor: '#f5f5f5' }}>
                    <Typography variant="caption" color="text.secondary">Closing Balance</Typography>
                    <Typography variant="h6" color="primary">
                      <strong>{formatCurrency(statement.closingBalance)}</strong>
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              {/* Transactions Table */}
              <Typography variant="h6" gutterBottom>
                Transaction Details
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Reference</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell align="right">Debit</TableCell>
                      <TableCell align="right">Credit</TableCell>
                      <TableCell align="right">Balance</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {statement.transactions && statement.transactions.length > 0 ? (
                      statement.transactions.map((txn: any, index: number) => (
                        <TableRow key={index}>
                          <TableCell>{txn.date}</TableCell>
                          <TableCell>
                            <Typography 
                              variant="body2" 
                              color={
                                txn.type === 'BILL' ? 'error.main' : 
                                txn.type === 'PAYMENT' ? 'success.main' : 
                                'warning.main'
                              }
                            >
                              {txn.type}
                            </Typography>
                          </TableCell>
                          <TableCell>{txn.reference}</TableCell>
                          <TableCell>{txn.description}</TableCell>
                          <TableCell align="right">
                            {txn.debit > 0 ? formatCurrency(txn.debit) : '-'}
                          </TableCell>
                          <TableCell align="right">
                            {txn.credit > 0 ? formatCurrency(txn.credit) : '-'}
                          </TableCell>
                          <TableCell align="right">
                            <strong>{formatCurrency(txn.balance)}</strong>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={7} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No transactions in selected period
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Action Buttons */}
              <Box display="flex" justifyContent="flex-end" gap={2} mt={3} className="no-print">
                <Button
                  variant="outlined"
                  startIcon={<PdfIcon />}
                  onClick={handlePrintPDF}
                >
                  Print/PDF
                </Button>
                <Button
                  variant="contained"
                  startIcon={<EmailIcon />}
                  onClick={handleEmailStatement}
                >
                  Email Statement
                </Button>
              </Box>
            </CardContent>
          </Card>
        )}

        {!statement && !loading && (
          <Card>
            <CardContent>
              <Box textAlign="center" py={8}>
                <Typography variant="body1" color="text.secondary">
                  Select a vendor and date range, then click "Generate" to view the statement
                </Typography>
              </Box>
            </CardContent>
          </Card>
        )}
      </Box>
  );
};

export default VendorStatements;

