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
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalBillingService, {
  CollectedPaymentReportItem,
  OutstandingInvoiceReportItem,
} from '../../services/hospitalBillingService';
import './Hospital.css';

const BillingReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [mode, setMode] = useState<'OUTSTANDING' | 'COLLECTED'>('OUTSTANDING');

  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [corporateIdFilter, setCorporateIdFilter] = useState('');
  const [asOfDateFilter, setAsOfDateFilter] = useState('');

  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');

  const [loading, setLoading] = useState(false);
  const [outstandingRows, setOutstandingRows] = useState<OutstandingInvoiceReportItem[]>([]);
  const [collectedRows, setCollectedRows] = useState<CollectedPaymentReportItem[]>([]);

  const handleModeChange = (_: React.MouseEvent<HTMLElement>, value: 'OUTSTANDING' | 'COLLECTED') => {
    if (value) {
      setMode(value);
    }
  };

  const loadReports = async () => {
    try {
      setLoading(true);
      if (mode === 'OUTSTANDING') {
        const params: { patientId?: string; corporateId?: string; asOf?: string } = {};
        if (patientIdFilter.trim()) {
          params.patientId = patientIdFilter.trim();
        }
        if (corporateIdFilter.trim()) {
          params.corporateId = corporateIdFilter.trim();
        }
        if (asOfDateFilter) {
          params.asOf = `${asOfDateFilter}T23:59:59Z`;
        }
        const rows = await hospitalBillingService.getOutstandingReports(params);
        setOutstandingRows(rows);
      } else {
        if (!fromDateFilter || !toDateFilter) {
          enqueueSnackbar('Select From/To dates for collected report', { variant: 'warning' });
          return;
        }
        const params = {
          from: `${fromDateFilter}T00:00:00Z`,
          to: `${toDateFilter}T23:59:59Z`,
        };
        const rows = await hospitalBillingService.getCollectedReports(params);
        setCollectedRows(rows);
      }
    } catch (err) {
      console.error('Failed to load billing reports', err);
      enqueueSnackbar('Failed to load billing reports', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadReports();
  };

  const exportCsv = () => {
    const rows =
      mode === 'OUTSTANDING'
        ? outstandingRows.map((r) => ({
            invoiceNumber: r.invoiceNumber,
            invoiceId: r.invoiceId,
            patientId: r.patientId,
            payerType: r.payerType,
            payerId: r.payerId,
            netAmount: r.netAmount,
            balanceDue: r.balanceDue,
            issuedAt: r.issuedAt,
          }))
        : collectedRows.map((r) => ({
            paymentId: r.paymentId,
            invoiceId: r.invoiceId,
            amount: r.amount,
            paymentMethod: r.paymentMethod,
            paymentDate: r.paymentDate,
            receivedByUserId: r.receivedByUserId,
          }));

    const headers = Object.keys(rows[0] || {});
    const csvLines = [
      headers.join(','),
      ...rows.map((row) => headers.map((h) => JSON.stringify((row as any)[h] ?? '')).join(',')),
    ];
    const blob = new Blob([csvLines.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute(
      'download',
      mode === 'OUTSTANDING' ? 'billing-outstanding.csv' : 'billing-collected.csv',
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const collectedSummaryByMethodAndUser = collectedRows.reduce<
    Record<string, { count: number; total: number }>
  >((acc, row) => {
    const key = `${row.paymentMethod || 'UNKNOWN'}::${
      row.receivedByUserId || 'UNKNOWN_USER'
    }`;
    const current = acc[key] || { count: 0, total: 0 };
    acc[key] = {
      count: current.count + 1,
      total: current.total + (row.amount || 0),
    };
    return acc;
  }, {});

  return (
    <Box className="hospital-page">
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Billing – Reports
        </Typography>
        <Box display="flex" gap={2} alignItems="center">
          <ToggleButtonGroup
            size="small"
            value={mode}
            exclusive
            onChange={handleModeChange}
          >
            <ToggleButton value="OUTSTANDING">Outstanding</ToggleButton>
            <ToggleButton value="COLLECTED">Collected</ToggleButton>
          </ToggleButtonGroup>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button variant="outlined" onClick={exportCsv} disabled={loading}>
            Export CSV
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          {mode === 'OUTSTANDING' ? (
            <Box display="flex" flexWrap="wrap" gap={2}>
              <TextField
                label="Patient ID"
                size="small"
                value={patientIdFilter}
                onChange={(e) => setPatientIdFilter(e.target.value)}
              />
              <TextField
                label="Corporate ID"
                size="small"
                value={corporateIdFilter}
                onChange={(e) => setCorporateIdFilter(e.target.value)}
              />
              <TextField
                label="As of Date"
                type="date"
                size="small"
                value={asOfDateFilter}
                onChange={(e) => setAsOfDateFilter(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <Button variant="contained" onClick={loadReports} disabled={loading}>
                Apply
              </Button>
            </Box>
          ) : (
            <Box display="flex" flexWrap="wrap" gap={2}>
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
              <Button variant="contained" onClick={loadReports} disabled={loading}>
                Apply
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" py={4}>
              <CircularProgress />
            </Box>
          ) : mode === 'OUTSTANDING' ? (
            <>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="subtitle1">Outstanding Invoices</Typography>
                <Typography variant="body2">Total: {outstandingRows.length}</Typography>
              </Box>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Invoice #</TableCell>
                      <TableCell>Invoice ID</TableCell>
                      <TableCell>Patient ID</TableCell>
                      <TableCell>Payer Type</TableCell>
                      <TableCell>Payer ID</TableCell>
                      <TableCell>Net Amount</TableCell>
                      <TableCell>Balance Due</TableCell>
                      <TableCell>Issued At</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {outstandingRows.map((row) => (
                      <TableRow key={row.invoiceId}>
                        <TableCell>{row.invoiceNumber}</TableCell>
                        <TableCell>{row.invoiceId}</TableCell>
                        <TableCell>{row.patientId}</TableCell>
                        <TableCell>{row.payerType}</TableCell>
                        <TableCell>{row.payerId}</TableCell>
                        <TableCell>{row.netAmount}</TableCell>
                        <TableCell>{row.balanceDue}</TableCell>
                        <TableCell>{row.issuedAt}</TableCell>
                      </TableRow>
                    ))}
                    {outstandingRows.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={8} align="center">
                          No outstanding invoices found.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          ) : (
            <>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="subtitle1">Collected Payments</Typography>
                <Typography variant="body2">Total: {collectedRows.length}</Typography>
              </Box>
              {Object.keys(collectedSummaryByMethodAndUser).length > 0 && (
                <Box mb={2}>
                  <Typography variant="subtitle2">Summary by method and user</Typography>
                  <Typography variant="body2">
                    {Object.entries(collectedSummaryByMethodAndUser).map(([key, value]) => {
                      const [method, user] = key.split('::');
                      return (
                        <span key={key}>
                          {method} / {user}: {value.count} payments, total {value.total}{' '}
                          &nbsp;&nbsp;
                        </span>
                      );
                    })}
                  </Typography>
                </Box>
              )}
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Payment ID</TableCell>
                      <TableCell>Invoice ID</TableCell>
                      <TableCell>Amount</TableCell>
                      <TableCell>Method</TableCell>
                      <TableCell>Payment Date</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {collectedRows.map((row) => (
                      <TableRow key={row.paymentId}>
                        <TableCell>{row.paymentId}</TableCell>
                        <TableCell>{row.invoiceId}</TableCell>
                        <TableCell>{row.amount}</TableCell>
                        <TableCell>{row.paymentMethod}</TableCell>
                        <TableCell>{row.paymentDate}</TableCell>
                      </TableRow>
                    ))}
                    {collectedRows.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} align="center">
                          No collected payments found.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default BillingReportsPage;

