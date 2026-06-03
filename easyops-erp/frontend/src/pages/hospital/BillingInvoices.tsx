import React, { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Refresh as RefreshIcon, Visibility as VisibilityIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalBillingService, {
  ChargeResponse,
  CreateInvoiceRequest,
  InvoiceDetailResponse,
  InvoiceResponse,
  PagedResponse,
  CreatePaymentRequest,
  PaymentResponse,
  CreateRefundRequest,
  EstimateRequest,
  EstimateResponse,
  DiscountLineResponse,
  CreateAdjustmentRequest,
} from '../../services/hospitalBillingService';
import './Hospital.css';

const BillingInvoicesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(false);
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(50);
  const [totalElements, setTotalElements] = useState(0);

  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [visitIdFilter, setVisitIdFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [payerTypeFilter, setPayerTypeFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');

  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceDetailResponse | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [createPatientId, setCreatePatientId] = useState('');
  const [createVisitId, setCreateVisitId] = useState('');
  const [createPayerType, setCreatePayerType] = useState('SELF');
  const [createPayerId, setCreatePayerId] = useState('');
  const [createDueDate, setCreateDueDate] = useState('');
  const [availableCharges, setAvailableCharges] = useState<ChargeResponse[]>([]);
  const [selectedChargeIds, setSelectedChargeIds] = useState<Set<string>>(new Set());

  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [paymentsLoading, setPaymentsLoading] = useState(false);

  const [recordPaymentOpen, setRecordPaymentOpen] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [paymentReference, setPaymentReference] = useState('');
  const [paymentDate, setPaymentDate] = useState('');

  const [refundDialogOpen, setRefundDialogOpen] = useState(false);
  const [refundPaymentId, setRefundPaymentId] = useState<string | null>(null);
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');

  const [estimate, setEstimate] = useState<EstimateResponse | null>(null);
  const [estimateLoading, setEstimateLoading] = useState(false);
  const [invoiceDiscounts, setInvoiceDiscounts] = useState<DiscountLineResponse[]>([]);
  const [invoiceDiscountsLoading, setInvoiceDiscountsLoading] = useState(false);

  const [adjustmentAmount, setAdjustmentAmount] = useState('');
  const [adjustmentType, setAdjustmentType] = useState<'WRITE_OFF' | 'CREDIT' | 'ADJUSTMENT'>('WRITE_OFF');
  const [adjustmentReason, setAdjustmentReason] = useState('');

  /** Manual candidate lines for estimate (when not using pending charges). */
  const [manualEstimateLines, setManualEstimateLines] = useState<
    { itemCode: string; itemDescription: string; quantity: string; unitPrice: string }[]
  >([]);

  useEffect(() => {
    loadInvoices();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const params: { patientId?: string; visitId?: string; page: number; size: number } = {
        page,
        size,
      };
      if (patientIdFilter.trim()) {
        params.patientId = patientIdFilter.trim();
      }
      if (visitIdFilter.trim()) {
        params.visitId = visitIdFilter.trim();
      }
      const response: PagedResponse<InvoiceResponse> = await hospitalBillingService.getInvoices(params);
      setInvoices(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load invoices', err);
      enqueueSnackbar('Failed to load invoices', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    setPage(0);
    loadInvoices();
  };

  const loadPayments = async (invoiceId: string) => {
    try {
      setPaymentsLoading(true);
      const response = await hospitalBillingService.getPayments(invoiceId);
      setPayments(response);
    } catch (err) {
      console.error('Failed to load payments', err);
      enqueueSnackbar('Failed to load payments', { variant: 'error' });
    } finally {
      setPaymentsLoading(false);
    }
  };

  const reloadInvoiceAndPayments = async (invoiceId: string) => {
    try {
      const detail = await hospitalBillingService.getInvoice(invoiceId);
      setSelectedInvoice(detail);
      await loadPayments(invoiceId);
      await loadInvoiceDiscounts(invoiceId);
    } catch (err) {
      console.error('Failed to refresh invoice after payment/refund', err);
      enqueueSnackbar('Failed to refresh invoice after payment/refund', { variant: 'error' });
    }
  };

  const loadInvoiceDiscounts = async (invoiceId: string) => {
    try {
      setInvoiceDiscountsLoading(true);
      const list = await hospitalBillingService.getInvoiceDiscounts(invoiceId);
      setInvoiceDiscounts(list);
    } catch (err) {
      console.error('Failed to load invoice discounts', err);
      setInvoiceDiscounts([]);
    } finally {
      setInvoiceDiscountsLoading(false);
    }
  };

  const handleViewInvoice = async (id: string) => {
    try {
      setLoading(true);
      const detail = await hospitalBillingService.getInvoice(id);
      setSelectedInvoice(detail);
      await loadPayments(detail.invoice.id);
      await loadInvoiceDiscounts(detail.invoice.id);
      setDetailOpen(true);
    } catch (err) {
      console.error('Failed to load invoice detail', err);
      enqueueSnackbar('Failed to load invoice detail', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAdjustment = async () => {
    if (!selectedInvoice) {
      return;
    }
    if (!adjustmentAmount || Number(adjustmentAmount) <= 0) {
      enqueueSnackbar('Enter a positive adjustment amount', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const body: CreateAdjustmentRequest = {
        type: adjustmentType,
        amount: Number(adjustmentAmount),
        reason: adjustmentReason || undefined,
      };
      const updated = await hospitalBillingService.createAdjustment(selectedInvoice.invoice.id, body);
      setSelectedInvoice(updated);
      enqueueSnackbar('Adjustment recorded', { variant: 'success' });
      await loadInvoices();
    } catch (err) {
      console.error('Failed to create adjustment', err);
      enqueueSnackbar('Failed to create adjustment', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleIssueInvoice = async (id: string) => {
    try {
      setLoading(true);
      await hospitalBillingService.issueInvoice(id);
      enqueueSnackbar('Invoice issued', { variant: 'success' });
      loadInvoices();
    } catch (err) {
      console.error('Failed to issue invoice', err);
      enqueueSnackbar('Failed to issue invoice', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleCancelInvoice = async (id: string) => {
    try {
      setLoading(true);
      await hospitalBillingService.cancelInvoice(id);
      enqueueSnackbar('Invoice cancelled', { variant: 'success' });
      loadInvoices();
    } catch (err) {
      console.error('Failed to cancel invoice', err);
      enqueueSnackbar('Failed to cancel invoice', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openCreateDialog = async () => {
    if (!createPatientId.trim()) {
      enqueueSnackbar('Enter patient ID to load pending charges', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      setEstimate(null);
      const chargesPage = await hospitalBillingService.getCharges({
        patientId: createPatientId.trim(),
        page: 0,
        size: 200,
      });
      const pending = chargesPage.content.filter((c) => c.status === 'PENDING');
      setAvailableCharges(pending);
      setSelectedChargeIds(new Set());
      setManualEstimateLines([]);
      setCreateDialogOpen(true);
    } catch (err) {
      console.error('Failed to load charges for invoice creation', err);
      enqueueSnackbar('Failed to load charges for invoice creation', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const addManualEstimateLine = () => {
    setManualEstimateLines((prev) => [
      ...prev,
      { itemCode: '', itemDescription: '', quantity: '1', unitPrice: '0' },
    ]);
  };

  const updateManualEstimateLine = (
    index: number,
    field: 'itemCode' | 'itemDescription' | 'quantity' | 'unitPrice',
    value: string,
  ) => {
    setManualEstimateLines((prev) =>
      prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)),
    );
  };

  const removeManualEstimateLine = (index: number) => {
    setManualEstimateLines((prev) => prev.filter((_, i) => i !== index));
  };

  const handleGetEstimate = async () => {
    const fromCharges = availableCharges
      .filter((c) => selectedChargeIds.has(c.id))
      .map((c) => ({
        itemCode: c.itemCode,
        itemDescription: c.itemDescription,
        quantity: c.quantity,
        unitPrice: c.unitPrice,
      }));
    const fromManual = manualEstimateLines
      .filter((row) => row.itemCode.trim() && !Number.isNaN(Number(row.quantity)) && !Number.isNaN(Number(row.unitPrice)))
      .map((row) => ({
        itemCode: row.itemCode.trim(),
        itemDescription: row.itemDescription.trim() || undefined,
        quantity: Number(row.quantity),
        unitPrice: Number(row.unitPrice),
      }));
    const lineItems = [...fromCharges, ...fromManual];
    if (lineItems.length === 0) {
      enqueueSnackbar(
        'Select at least one pending charge or enter manual lines to get an estimate',
        { variant: 'warning' },
      );
      return;
    }
    const body: EstimateRequest = {
      lineItems,
      patientId: createPatientId.trim() || undefined,
      corporateContractId:
        createPayerType === 'CORPORATE' && createPayerId?.trim()
          ? createPayerId.trim()
          : undefined,
    };
    try {
      setEstimateLoading(true);
      const result = await hospitalBillingService.getEstimate(body);
      setEstimate(result);
    } catch (err) {
      console.error('Failed to get estimate', err);
      enqueueSnackbar('Failed to get estimate', { variant: 'error' });
    } finally {
      setEstimateLoading(false);
    }
  };

  const toggleChargeSelection = (id: string) => {
    setSelectedChargeIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleCreateInvoice = async () => {
    if (!createPatientId.trim()) {
      enqueueSnackbar('Patient ID is required', { variant: 'warning' });
      return;
    }
    if (selectedChargeIds.size === 0) {
      enqueueSnackbar('Select at least one charge', { variant: 'warning' });
      return;
    }

    const payload: CreateInvoiceRequest = {
      patientId: createPatientId.trim(),
      visitId: createVisitId.trim() || undefined,
      payerType: createPayerType,
      payerId: createPayerId.trim() || undefined,
      chargeLineIds: Array.from(selectedChargeIds),
      dueDate: createDueDate || undefined,
    };

    try {
      setLoading(true);
      const created = await hospitalBillingService.createInvoice(payload);
      enqueueSnackbar(`Invoice ${created.invoice.invoiceNumber} created`, { variant: 'success' });
      setCreateDialogOpen(false);
      setSelectedInvoice(created);
      setDetailOpen(true);
      loadInvoices();
    } catch (err) {
      console.error('Failed to create invoice', err);
      enqueueSnackbar('Failed to create invoice', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const totalNetSelected = useMemo(() => {
    return availableCharges
      .filter((c) => selectedChargeIds.has(c.id))
      .reduce((sum, c) => sum + (c.netAmount || 0), 0);
  }, [availableCharges, selectedChargeIds]);

  const handleOpenRecordPayment = () => {
    if (!selectedInvoice) {
      return;
    }
    setPaymentAmount('');
    setPaymentMethod('CASH');
    setPaymentReference('');
    setPaymentDate('');
    setRecordPaymentOpen(true);
  };

  const handleSubmitRecordPayment = async () => {
    if (!selectedInvoice) {
      return;
    }
    const amountNumber = Number(paymentAmount);
    if (!paymentAmount || Number.isNaN(amountNumber) || amountNumber <= 0) {
      enqueueSnackbar('Enter a valid payment amount', { variant: 'warning' });
      return;
    }
    const body: CreatePaymentRequest = {
      amount: amountNumber,
      paymentMethod,
      paymentReference: paymentReference || undefined,
      paymentDate: paymentDate || undefined,
    };
    try {
      setLoading(true);
      await hospitalBillingService.createPayment(selectedInvoice.invoice.id, body);
      enqueueSnackbar('Payment recorded', { variant: 'success' });
      setRecordPaymentOpen(false);
      await reloadInvoiceAndPayments(selectedInvoice.invoice.id);
    } catch (err) {
      console.error('Failed to record payment', err);
      enqueueSnackbar('Failed to record payment', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenRefundDialog = (paymentId: string) => {
    setRefundPaymentId(paymentId);
    setRefundAmount('');
    setRefundReason('');
    setRefundDialogOpen(true);
  };

  const handleSubmitRefund = async () => {
    if (!selectedInvoice || !refundPaymentId) {
      return;
    }
    const amountNumber = Number(refundAmount);
    if (!refundAmount || Number.isNaN(amountNumber) || amountNumber <= 0) {
      enqueueSnackbar('Enter a valid refund amount', { variant: 'warning' });
      return;
    }
    const body: CreateRefundRequest = {
      amount: amountNumber,
      reason: refundReason || undefined,
    };
    try {
      setLoading(true);
      await hospitalBillingService.createRefund(refundPaymentId, body);
      enqueueSnackbar('Refund recorded', { variant: 'success' });
      setRefundDialogOpen(false);
      await reloadInvoiceAndPayments(selectedInvoice.invoice.id);
    } catch (err) {
      console.error('Failed to record refund', err);
      enqueueSnackbar('Failed to record refund', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Billing – Invoices
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={openCreateDialog}
            disabled={loading}
          >
            Create Invoice
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <TextField
              label="Patient ID"
              size="small"
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
            />
            <TextField
              label="Visit ID"
              size="small"
              value={visitIdFilter}
              onChange={(e) => setVisitIdFilter(e.target.value)}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                <MenuItem value="DRAFT">Draft</MenuItem>
                <MenuItem value="ISSUED">Issued</MenuItem>
                <MenuItem value="PARTIALLY_PAID">Partially Paid</MenuItem>
                <MenuItem value="PAID">Paid</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Payer Type</InputLabel>
              <Select
                label="Payer Type"
                value={payerTypeFilter}
                onChange={(e) => setPayerTypeFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                <MenuItem value="SELF">Self</MenuItem>
                <MenuItem value="CORPORATE">Corporate</MenuItem>
                <MenuItem value="MIXED">Mixed</MenuItem>
              </Select>
            </FormControl>
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
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Invoices</Typography>
            <Typography variant="body2">Total: {totalElements}</Typography>
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
                    <TableCell>Invoice #</TableCell>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Visit ID</TableCell>
                    <TableCell>Payer Type</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Net Amount</TableCell>
                    <TableCell>Balance Due</TableCell>
                    <TableCell>Issued At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {invoices.map((inv) => (
                    <TableRow key={inv.id}>
                      <TableCell>{inv.invoiceNumber}</TableCell>
                      <TableCell>{inv.patientId}</TableCell>
                      <TableCell>{inv.visitId}</TableCell>
                      <TableCell>{inv.payerType}</TableCell>
                      <TableCell>{inv.status}</TableCell>
                      <TableCell>{inv.netAmount}</TableCell>
                      <TableCell>{inv.balanceDue}</TableCell>
                      <TableCell>{inv.issuedAt}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          startIcon={<VisibilityIcon />}
                          onClick={() => handleViewInvoice(inv.id)}
                          sx={{ mr: 1 }}
                        >
                          View
                        </Button>
                        {inv.status === 'DRAFT' && (
                          <Button
                            size="small"
                            onClick={() => handleIssueInvoice(inv.id)}
                            sx={{ mr: 1 }}
                          >
                            Issue
                          </Button>
                        )}
                        {inv.status !== 'CANCELLED' && (
                          <Button
                            size="small"
                            color="error"
                            onClick={() => handleCancelInvoice(inv.id)}
                          >
                            Cancel
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {invoices.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={9} align="center">
                        No invoices found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create Invoice Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="lg" fullWidth>
        <DialogTitle>Create Invoice</DialogTitle>
        <DialogContent dividers>
          <Box display="flex" flexWrap="wrap" gap={2} mb={3}>
            <TextField
              label="Patient ID"
              size="small"
              value={createPatientId}
              onChange={(e) => setCreatePatientId(e.target.value)}
            />
            <TextField
              label="Visit ID"
              size="small"
              value={createVisitId}
              onChange={(e) => setCreateVisitId(e.target.value)}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Payer Type</InputLabel>
              <Select
                label="Payer Type"
                value={createPayerType}
                onChange={(e) => setCreatePayerType(e.target.value as string)}
              >
                <MenuItem value="SELF">Self</MenuItem>
                <MenuItem value="CORPORATE">Corporate</MenuItem>
                <MenuItem value="MIXED">Mixed</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Payer ID"
              size="small"
              value={createPayerId}
              onChange={(e) => setCreatePayerId(e.target.value)}
            />
            <TextField
              label="Due Date"
              type="date"
              size="small"
              value={createDueDate}
              onChange={(e) => setCreateDueDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Box>

          <Typography variant="subtitle1" gutterBottom>
            Select Pending Charges
          </Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell />
                  <TableCell>Item Code</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Source Ref</TableCell>
                  <TableCell>Quantity</TableCell>
                  <TableCell>Net Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {availableCharges.map((c) => (
                  <TableRow
                    key={c.id}
                    hover
                    selected={selectedChargeIds.has(c.id)}
                    onClick={() => toggleChargeSelection(c.id)}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedChargeIds.has(c.id)}
                        onChange={() => toggleChargeSelection(c.id)}
                      />
                    </TableCell>
                    <TableCell>{c.itemCode}</TableCell>
                    <TableCell>{c.sourceService}</TableCell>
                    <TableCell>{c.sourceReferenceId}</TableCell>
                    <TableCell>{c.quantity}</TableCell>
                    <TableCell>{c.netAmount}</TableCell>
                  </TableRow>
                ))}
                {availableCharges.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      No pending charges found for this patient.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>

          <Typography variant="subtitle2" sx={{ mt: 3, mb: 1 }}>
            Or enter candidate lines manually for estimate
          </Typography>
          <Box display="flex" alignItems="center" gap={1} mb={1}>
            <Button variant="outlined" size="small" onClick={addManualEstimateLine}>
              Add line
            </Button>
          </Box>
          {manualEstimateLines.length > 0 && (
            <TableContainer sx={{ mb: 2 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Item Code</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Quantity</TableCell>
                    <TableCell>Unit Price</TableCell>
                    <TableCell width={60} />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {manualEstimateLines.map((row, idx) => (
                    <TableRow key={idx}>
                      <TableCell>
                        <TextField
                          size="small"
                          placeholder="e.g. LAB-001"
                          value={row.itemCode}
                          onChange={(e) => updateManualEstimateLine(idx, 'itemCode', e.target.value)}
                          fullWidth
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          placeholder="Optional"
                          value={row.itemDescription}
                          onChange={(e) =>
                            updateManualEstimateLine(idx, 'itemDescription', e.target.value)
                          }
                          fullWidth
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={row.quantity}
                          onChange={(e) => updateManualEstimateLine(idx, 'quantity', e.target.value)}
                          inputProps={{ min: 0, step: 1 }}
                          sx={{ width: 90 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={row.unitPrice}
                          onChange={(e) =>
                            updateManualEstimateLine(idx, 'unitPrice', e.target.value)
                          }
                          inputProps={{ min: 0, step: 0.01 }}
                          sx={{ width: 100 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          color="error"
                          onClick={() => removeManualEstimateLine(idx)}
                        >
                          Remove
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          <Box mt={2} display="flex" alignItems="center" gap={2} flexWrap="wrap">
            <Typography variant="body2">
              Selected charges net total: {totalNetSelected}
            </Typography>
            <Button
              variant="outlined"
              size="small"
              onClick={handleGetEstimate}
              disabled={
                estimateLoading ||
                (selectedChargeIds.size === 0 &&
                  manualEstimateLines.filter(
                    (r) =>
                      r.itemCode.trim() &&
                      !Number.isNaN(Number(r.quantity)) &&
                      !Number.isNaN(Number(r.unitPrice)),
                  ).length === 0)
              }
            >
              {estimateLoading ? 'Loading…' : 'Get Estimate'}
            </Button>
          </Box>
          {estimate && (
            <Box mt={2} p={2} sx={{ bgcolor: 'action.hover' }} borderRadius={1}>
              <Typography variant="subtitle2" gutterBottom>
                Estimate
              </Typography>
              <Typography variant="body2">
                Total gross: {estimate.totalGross} · Discount: {estimate.totalDiscount} ·{' '}
                <strong>Net payable: {estimate.netPayable}</strong>
              </Typography>
              {estimate.discountLines && estimate.discountLines.length > 0 && (
                <TableContainer sx={{ mt: 1 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Description</TableCell>
                        <TableCell>Source</TableCell>
                        <TableCell align="right">Amount</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {estimate.discountLines.map((d, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{d.description}</TableCell>
                          <TableCell>{d.source}</TableCell>
                          <TableCell align="right">{d.amount}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Close</Button>
          <Button variant="contained" onClick={handleCreateInvoice}>
            Create Invoice
          </Button>
        </DialogActions>
      </Dialog>

      {/* Invoice Detail Dialog */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Invoice Detail</DialogTitle>
        <DialogContent dividers>
          {selectedInvoice ? (
            <>
              <Typography variant="subtitle1" gutterBottom>
                Header
              </Typography>
              <Box display="flex" flexWrap="wrap" gap={2} mb={2}>
                <TextField
                  label="Invoice #"
                  size="small"
                  value={selectedInvoice.invoice.invoiceNumber}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Patient ID"
                  size="small"
                  value={selectedInvoice.invoice.patientId}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Visit ID"
                  size="small"
                  value={selectedInvoice.invoice.visitId || ''}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Payer Type"
                  size="small"
                  value={selectedInvoice.invoice.payerType}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Status"
                  size="small"
                  value={selectedInvoice.invoice.status}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Net Amount"
                  size="small"
                  value={selectedInvoice.invoice.netAmount}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Balance Due"
                  size="small"
                  value={selectedInvoice.invoice.balanceDue}
                  InputProps={{ readOnly: true }}
                />
                <TextField
                  label="Issued At"
                  size="small"
                  value={selectedInvoice.invoice.issuedAt || ''}
                  InputProps={{ readOnly: true }}
                />
              </Box>

              <Typography variant="subtitle1" gutterBottom>
                Charge Lines
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Item Code</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell>Quantity</TableCell>
                      <TableCell>Unit Price</TableCell>
                      <TableCell>Net Amount</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedInvoice.chargeLines.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>{c.itemCode}</TableCell>
                        <TableCell>{c.itemDescription}</TableCell>
                        <TableCell>{c.quantity}</TableCell>
                        <TableCell>{c.unitPrice}</TableCell>
                        <TableCell>{c.netAmount}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
                Discounts
              </Typography>
              {invoiceDiscountsLoading ? (
                <Box display="flex" justifyContent="center" alignItems="center" py={1}>
                  <CircularProgress size={24} />
                </Box>
              ) : invoiceDiscounts.length > 0 ? (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Description</TableCell>
                        <TableCell>Source</TableCell>
                        <TableCell align="right">Amount</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {invoiceDiscounts.map((d, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{d.description}</TableCell>
                          <TableCell>{d.source}</TableCell>
                          <TableCell align="right">{d.amount}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No discounts applied.
                </Typography>
              )}

              <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
                Adjustments
              </Typography>
              <Box display="flex" gap={2} alignItems="center" mb={2}>
                <FormControl size="small" sx={{ minWidth: 160 }}>
                  <InputLabel id="adjustment-type-label">Type</InputLabel>
                  <Select
                    labelId="adjustment-type-label"
                    label="Type"
                    value={adjustmentType}
                    onChange={(e) =>
                      setAdjustmentType(e.target.value as 'WRITE_OFF' | 'CREDIT' | 'ADJUSTMENT')
                    }
                  >
                    <MenuItem value="WRITE_OFF">Write-off</MenuItem>
                    <MenuItem value="CREDIT">Credit</MenuItem>
                    <MenuItem value="ADJUSTMENT">Adjustment</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="Amount"
                  type="number"
                  size="small"
                  value={adjustmentAmount}
                  onChange={(e) => setAdjustmentAmount(e.target.value)}
                />
                <TextField
                  label="Reason"
                  size="small"
                  value={adjustmentReason}
                  onChange={(e) => setAdjustmentReason(e.target.value)}
                  sx={{ flex: 1, minWidth: 200 }}
                />
                <Button
                  variant="contained"
                  size="small"
                  onClick={handleCreateAdjustment}
                  disabled={loading || !selectedInvoice || selectedInvoice.invoice.status === 'CANCELLED'}
                >
                  Add Write-off / Credit
                </Button>
              </Box>

              <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
                Payments Summary
              </Typography>
              <Typography variant="body2">
                Total Paid: {selectedInvoice.paymentsSummary.totalPaid} | Last Payment:{' '}
                {selectedInvoice.paymentsSummary.lastPaymentAt || 'N/A'}
              </Typography>

              <Box display="flex" justifyContent="space-between" alignItems="center" mt={3} mb={1}>
                <Typography variant="subtitle1">Payments</Typography>
                <Button variant="contained" size="small" onClick={handleOpenRecordPayment} disabled={loading}>
                  Record Payment
                </Button>
              </Box>
              {paymentsLoading ? (
                <Box display="flex" justifyContent="center" alignItems="center" py={2}>
                  <CircularProgress size={24} />
                </Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Amount</TableCell>
                        <TableCell>Method</TableCell>
                        <TableCell>Date</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Reference</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {payments.map((p) => (
                        <TableRow key={p.id}>
                          <TableCell>{p.amount}</TableCell>
                          <TableCell>{p.paymentMethod}</TableCell>
                          <TableCell>{p.paymentDate}</TableCell>
                          <TableCell>{p.status}</TableCell>
                          <TableCell>{p.paymentReference}</TableCell>
                          <TableCell align="right">
                            <Button
                              size="small"
                              onClick={() => handleOpenRefundDialog(p.id)}
                              disabled={loading}
                            >
                              Refund
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                      {payments.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={6} align="center">
                            No payments recorded.
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </>
          ) : (
            <Typography>Loading...</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Record Payment Dialog */}
      <Dialog open={recordPaymentOpen} onClose={() => setRecordPaymentOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Payment</DialogTitle>
        <DialogContent dividers>
          <Box display="flex" flexDirection="column" gap={2}>
            <TextField
              label="Amount"
              type="number"
              size="small"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
            />
            <FormControl size="small">
              <InputLabel>Payment Method</InputLabel>
              <Select
                label="Payment Method"
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value as string)}
              >
                <MenuItem value="CASH">Cash</MenuItem>
                <MenuItem value="CARD">Card</MenuItem>
                <MenuItem value="UPI">UPI</MenuItem>
                <MenuItem value="BANK_TRANSFER">Bank Transfer</MenuItem>
                <MenuItem value="CORPORATE_ADJUSTMENT">Corporate Adjustment</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Reference"
              size="small"
              value={paymentReference}
              onChange={(e) => setPaymentReference(e.target.value)}
            />
            <TextField
              label="Payment Date"
              type="datetime-local"
              size="small"
              value={paymentDate}
              onChange={(e) => setPaymentDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRecordPaymentOpen(false)}>Close</Button>
          <Button variant="contained" onClick={handleSubmitRecordPayment} disabled={loading}>
            Save Payment
          </Button>
        </DialogActions>
      </Dialog>

      {/* Refund Dialog */}
      <Dialog open={refundDialogOpen} onClose={() => setRefundDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Refund Payment</DialogTitle>
        <DialogContent dividers>
          <Box display="flex" flexDirection="column" gap={2}>
            <TextField
              label="Refund Amount"
              type="number"
              size="small"
              value={refundAmount}
              onChange={(e) => setRefundAmount(e.target.value)}
            />
            <TextField
              label="Reason"
              size="small"
              multiline
              minRows={2}
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRefundDialogOpen(false)}>Close</Button>
          <Button variant="contained" onClick={handleSubmitRefund} disabled={loading}>
            Save Refund
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BillingInvoicesPage;

