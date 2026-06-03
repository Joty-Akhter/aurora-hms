import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalBillingService, { PaymentResponse } from '../../services/hospitalBillingService';
import './Hospital.css';

const BillingPaymentsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();

  const [invoiceIdFilter, setInvoiceIdFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');

  const [loading, setLoading] = useState(false);
  const [payments, setPayments] = useState<PaymentResponse[]>([]);

  const loadPayments = async () => {
    try {
      setLoading(true);
      const params: { invoiceId?: string; from?: string; to?: string } = {};
      if (invoiceIdFilter.trim()) {
        params.invoiceId = invoiceIdFilter.trim();
      }
      if (fromDateFilter) {
        params.from = `${fromDateFilter}T00:00:00Z`;
      }
      if (toDateFilter) {
        params.to = `${toDateFilter}T23:59:59Z`;
      }
      const result = await hospitalBillingService.getPaymentsGlobal(params);
      setPayments(result);
    } catch (err) {
      console.error('Failed to load payments', err);
      enqueueSnackbar('Failed to load payments', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadPayments();
  };

  const handleViewInvoice = (invoiceId: string) => {
    navigate('/hospital/billing/invoices');
    // Detail navigation is handled from the invoices page
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Billing – Payments
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={handleRefresh}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <TextField
              label="Invoice ID"
              size="small"
              value={invoiceIdFilter}
              onChange={(e) => setInvoiceIdFilter(e.target.value)}
            />
            <TextField
              label="From Date"
              type="date"
              size="small"
              value={fromDateFilter}
              onChange={(e) => setFromDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To Date"
              type="date"
              size="small"
              value={toDateFilter}
              onChange={(e) => setToDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button variant="contained" onClick={loadPayments} disabled={loading}>
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Payments</Typography>
            <Typography variant="body2">Total: {payments.length}</Typography>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Invoice ID</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Method</TableCell>
                    <TableCell>Payment Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Reference</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {payments.map((p) => (
                    <TableRow key={p.id}>
                      <TableCell>{p.invoiceId}</TableCell>
                      <TableCell>{p.amount}</TableCell>
                      <TableCell>{p.paymentMethod}</TableCell>
                      <TableCell>{p.paymentDate}</TableCell>
                      <TableCell>{p.status}</TableCell>
                      <TableCell>{p.paymentReference}</TableCell>
                      <TableCell align="right">
                        <Button size="small" onClick={() => handleViewInvoice(p.invoiceId)}>
                          View Invoice
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {payments.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No payments found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default BillingPaymentsPage;

