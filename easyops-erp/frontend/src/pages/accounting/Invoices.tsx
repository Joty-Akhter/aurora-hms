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
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Grid,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  Payment as PaymentIcon,
  Check as PostIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';
import FiscalPeriodWarning from '../../components/accounting/FiscalPeriodWarning';
import { getApiErrorMessage } from '../../utils/apiError';

const Invoices: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [invoices, setInvoices] = useState<any[]>([]);
  const [customers, setCustomers] = useState<any[]>([]);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [periods, setPeriods] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [openPaymentDialog, setOpenPaymentDialog] = useState(false);
  const [openViewDialog, setOpenViewDialog] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';
  
  const [paymentData, setPaymentData] = useState({
    amount: 0,
    paymentDate: new Date().toISOString().split('T')[0],
    paymentMethod: 'CASH',
    referenceNumber: '',
    notes: '',
  });

  const [formData, setFormData] = useState({
    invoiceNumber: '',
    invoiceDate: new Date().toISOString().split('T')[0],
    customerId: '',
    paymentTerms: 30,
    notes: '',
    lines: [
      { lineNumber: 1, description: '', quantity: 1, unitPrice: 0, taxPercent: 0, discountPercent: 0, accountId: '' },
    ],
  });

  const handleAddLine = () => {
    const nextLineNumber = formData.lines.length + 1;
    setFormData({
      ...formData,
      lines: [...formData.lines, { 
        lineNumber: nextLineNumber, 
        description: '', 
        quantity: 1, 
        unitPrice: 0, 
        taxPercent: 0, 
        discountPercent: 0,
        accountId: '' 
      }],
    });
  };

  const calculateTotal = () => {
    return formData.lines.reduce((sum, line) => {
      const subtotal = line.quantity * line.unitPrice;
      const tax = subtotal * (line.taxPercent / 100);
      return sum + subtotal + tax;
    }, 0);
  };

  useEffect(() => {
    if (organizationId) {
      loadInvoices();
      loadCustomers();
      loadAccounts();
      loadPeriods();
    }
  }, [organizationId]);

  const loadInvoices = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getARInvoices(organizationId);
      setInvoices(data);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load invoices'));
    } finally {
      setLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const data = await accountingService.getARCustomers(organizationId);
      setCustomers(data);
    } catch (err: any) {
      console.error('Failed to load customers:', err);
    }
  };

  const loadAccounts = async () => {
    try {
      const data = await accountingService.getActiveAccounts(organizationId);
      const revenueAccounts = data.filter(a => a.accountType === 'REVENUE' && !a.isGroup);
      setAccounts(revenueAccounts);
    } catch (err: any) {
      console.error('Failed to load accounts:', err);
    }
  };

  const loadPeriods = async () => {
    try {
      const data = await accountingService.getOpenPeriods(organizationId);
      setPeriods(data);
    } catch (err: any) {
      console.error('Failed to load periods:', err);
    }
  };

  const handleViewInvoice = (invoice: any) => {
    setSelectedInvoice(invoice);
    setOpenViewDialog(true);
  };

  const handleOpenPaymentDialog = (invoice: any) => {
    setSelectedInvoice(invoice);
    setPaymentData({
      amount: invoice.balanceDue || invoice.totalAmount,
      paymentDate: new Date().toISOString().split('T')[0],
      paymentMethod: 'CASH',
      referenceNumber: '',
      notes: '',
    });
    setOpenPaymentDialog(true);
  };

  const handleRecordPayment = async () => {
    if (!selectedInvoice) return;
    
    setError(null);
    setSuccess(null);

    if (paymentData.amount <= 0) {
      setError('Payment amount must be greater than 0');
      return;
    }

    if (paymentData.amount > selectedInvoice.balanceDue) {
      setError('Payment amount cannot exceed balance due');
      return;
    }

    try {
      // Generate receipt number
      const timestamp = Date.now();
      const receiptNumber = `RCT-${selectedInvoice.invoiceNumber}-${timestamp}`;

      // Create receipt with allocation to the invoice
      const receiptData = {
        organizationId,
        receiptNumber,
        receiptDate: paymentData.paymentDate,
        customerId: selectedInvoice.customer?.id || selectedInvoice.customerId,
        paymentMethod: paymentData.paymentMethod,
        referenceNumber: paymentData.referenceNumber,
        amount: paymentData.amount,
        notes: paymentData.notes,
        allocations: [
          {
            invoiceId: selectedInvoice.id,
            allocatedAmount: paymentData.amount,
          }
        ],
      };

      // Create and post the receipt
      const receipt = await accountingService.createARReceipt(receiptData);
      await accountingService.postARReceipt(receipt.id);
      
      setSuccess(`Payment of $${paymentData.amount} recorded for invoice ${selectedInvoice.invoiceNumber}`);
      setOpenPaymentDialog(false);
      setSelectedInvoice(null);
      loadInvoices();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to record payment'));
    }
  };

  const handlePostInvoice = async (invoiceId: string) => {
    setError(null);
    setSuccess(null);
    try {
      await accountingService.postARInvoice(invoiceId);
      setSuccess('Invoice posted successfully!');
      loadInvoices();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to post invoice'));
    }
  };

  const handleCreateInvoice = async (postAfterCreate = false) => {
    setError(null);
    setSuccess(null);

    // Validation
    if (!formData.invoiceNumber) {
      setError('Invoice number is required');
      return;
    }
    if (!formData.customerId) {
      setError('Please select a customer');
      return;
    }
    if (formData.lines.length === 0 || !formData.lines[0].accountId) {
      setError('Please add at least one invoice line');
      return;
    }

    try {
      // Calculate due date
      const invoiceDate = new Date(formData.invoiceDate);
      const dueDate = new Date(invoiceDate);
      dueDate.setDate(dueDate.getDate() + formData.paymentTerms);

      const invoiceData = {
        organizationId,
        invoiceNumber: formData.invoiceNumber,
        invoiceDate: formData.invoiceDate,
        dueDate: dueDate.toISOString().split('T')[0],
        customerId: formData.customerId,
        notes: formData.notes,
        lines: formData.lines.map(line => ({
          lineNumber: line.lineNumber,
          description: line.description,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
          discountPercent: line.discountPercent || 0,
          taxPercent: line.taxPercent || 0,
          accountId: line.accountId,
        })),
      };

      const created = await accountingService.createARInvoice(invoiceData);
      if (postAfterCreate && created?.id) {
        await accountingService.postARInvoice(created.id);
        setSuccess('Invoice created and posted successfully!');
      } else {
        setSuccess(
          postAfterCreate
            ? 'Invoice created but could not be posted automatically. Use Post from the list.'
            : 'Invoice created successfully! Post it from the list when ready.'
        );
      }
      setOpenDialog(false);
      
      // Reset form
      setFormData({
        invoiceNumber: '',
        invoiceDate: new Date().toISOString().split('T')[0],
        customerId: '',
        paymentTerms: 30,
        notes: '',
        lines: [
          { lineNumber: 1, description: '', quantity: 1, unitPrice: 0, taxPercent: 0, discountPercent: 0, accountId: '' },
        ],
      });
      
      loadInvoices();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to create invoice'));
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Accounts Receivable - Invoices</Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Create Invoice
          </Button>
        )}
      </Box>

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

      <FiscalPeriodWarning hasOpenPeriods={periods.length > 0} />

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Recent Invoices
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Invoice #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Due Date</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell align="right">Balance</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2">Loading invoices...</Typography>
                    </TableCell>
                  </TableRow>
                ) : invoices.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No invoices yet. Create your first invoice to get started!
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  invoices.map((invoice) => (
                    <TableRow key={invoice.id}>
                      <TableCell>{invoice.invoiceNumber}</TableCell>
                      <TableCell>{invoice.invoiceDate}</TableCell>
                      <TableCell>{invoice.customer?.customerName || invoice.customerId}</TableCell>
                      <TableCell>{invoice.dueDate}</TableCell>
                      <TableCell align="right">${invoice.totalAmount?.toLocaleString()}</TableCell>
                      <TableCell align="right">${invoice.balanceDue?.toLocaleString()}</TableCell>
                      <TableCell>
                        <Chip 
                          label={invoice.status} 
                          size="small"
                          color={invoice.status === 'PAID' ? 'success' : invoice.status === 'OVERDUE' ? 'error' : 'warning'}
                        />
                      </TableCell>
                      <TableCell>
                        {canManage && invoice.status === 'DRAFT' && (
                          <IconButton
                            size="small"
                            title="Post Invoice"
                            onClick={() => handlePostInvoice(invoice.id)}
                            color="primary"
                          >
                            <PostIcon fontSize="small" />
                          </IconButton>
                        )}
                        <IconButton 
                          size="small" 
                          title="View Invoice"
                          onClick={() => handleViewInvoice(invoice)}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                        <IconButton 
                          size="small" 
                          title="Record Payment"
                          onClick={() => handleOpenPaymentDialog(invoice)}
                          disabled={invoice.status === 'DRAFT' || Number(invoice.balanceDue) <= 0}
                        >
                          <PaymentIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Invoice Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create Customer Invoice</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={1}>
            <Grid item xs={6}>
              <TextField
                label="Invoice Date"
                type="date"
                value={formData.invoiceDate}
                onChange={(e) => setFormData({ ...formData, invoiceDate: e.target.value })}
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Payment Terms (days)"
                type="number"
                value={formData.paymentTerms}
                onChange={(e) => setFormData({ ...formData, paymentTerms: parseInt(e.target.value) })}
                fullWidth
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Invoice Number"
                value={formData.invoiceNumber}
                onChange={(e) => setFormData({ ...formData, invoiceNumber: e.target.value })}
                fullWidth
                required
                placeholder="e.g., INV-001"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Customer"
                value={formData.customerId}
                onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
                fullWidth
                required
              >
                <MenuItem value="">Select Customer</MenuItem>
                {customers.map(customer => (
                  <MenuItem key={customer.id} value={customer.id}>
                    {customer.customerCode} - {customer.customerName || 'N/A'}
                  </MenuItem>
                ))}
              </TextField>
              {customers.length === 0 && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  No customers found. Please create customers in the AR module first.
                </Typography>
              )}
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Invoice Lines
              </Typography>
              {formData.lines.map((line, index) => (
                <Grid container spacing={2} key={index} sx={{ mb: 2 }}>
                  <Grid item xs={3}>
                    <TextField
                      label="Description"
                      value={line.description}
                      onChange={(e) => {
                        const newLines = [...formData.lines];
                        newLines[index].description = e.target.value;
                        setFormData({ ...formData, lines: newLines });
                      }}
                      fullWidth
                      size="small"
                    />
                  </Grid>
                  <Grid item xs={2}>
                    <TextField
                      select
                      label="Revenue Account"
                      value={line.accountId}
                      onChange={(e) => {
                        const newLines = [...formData.lines];
                        newLines[index].accountId = e.target.value;
                        setFormData({ ...formData, lines: newLines });
                      }}
                      fullWidth
                      size="small"
                    >
                      <MenuItem value="">Select</MenuItem>
                      {accounts.map(account => (
                        <MenuItem key={account.id} value={account.id}>
                          {account.accountCode} - {account.accountName}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={1.5}>
                    <TextField
                      label="Qty"
                      type="number"
                      value={line.quantity}
                      onChange={(e) => {
                        const newLines = [...formData.lines];
                        newLines[index].quantity = parseFloat(e.target.value);
                        setFormData({ ...formData, lines: newLines });
                      }}
                      fullWidth
                      size="small"
                    />
                  </Grid>
                  <Grid item xs={1.5}>
                    <TextField
                      label="Unit Price"
                      type="number"
                      value={line.unitPrice}
                      onChange={(e) => {
                        const newLines = [...formData.lines];
                        newLines[index].unitPrice = parseFloat(e.target.value);
                        setFormData({ ...formData, lines: newLines });
                      }}
                      fullWidth
                      size="small"
                    />
                  </Grid>
                  <Grid item xs={1.5}>
                    <TextField
                      label="Tax %"
                      type="number"
                      value={line.taxPercent}
                      onChange={(e) => {
                        const newLines = [...formData.lines];
                        newLines[index].taxPercent = parseFloat(e.target.value);
                        setFormData({ ...formData, lines: newLines });
                      }}
                      fullWidth
                      size="small"
                    />
                  </Grid>
                  <Grid item xs={2.5}>
                    <Box display="flex" alignItems="center" gap={1} sx={{ pt: 1 }}>
                      <Typography variant="body2">
                        ${(line.quantity * line.unitPrice * (1 + line.taxPercent / 100)).toFixed(2)}
                      </Typography>
                      {formData.lines.length > 1 && (
                        <IconButton 
                          size="small" 
                          onClick={() => {
                            const newLines = formData.lines.filter((_, i) => i !== index);
                            setFormData({ ...formData, lines: newLines });
                          }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      )}
                    </Box>
                  </Grid>
                </Grid>
              ))}
              <Button size="small" startIcon={<AddIcon />} onClick={handleAddLine}>
                Add Line
              </Button>
            </Grid>

            <Grid item xs={12}>
              <Box display="flex" justifyContent="space-between" sx={{ mt: 2, p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
                <Typography variant="h6">Total Amount:</Typography>
                <Typography variant="h6">${calculateTotal().toFixed(2)}</Typography>
              </Box>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={() => handleCreateInvoice(false)} disabled={!formData.invoiceNumber || !formData.customerId}>
            Create Draft
          </Button>
          <Button
            variant="contained"
            onClick={() => handleCreateInvoice(true)}
            disabled={!formData.invoiceNumber || !formData.customerId}
          >
            Create &amp; Post
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Invoice Dialog */}
      <Dialog open={openViewDialog} onClose={() => setOpenViewDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Invoice Details</DialogTitle>
        <DialogContent>
          {selectedInvoice && (
            <Box sx={{ mt: 2 }}>
              <Grid container spacing={3}>
                {/* Invoice Header */}
                <Grid item xs={12}>
                  <Box sx={{ p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
                    <Grid container spacing={2}>
                      <Grid item xs={6}>
                        <Typography variant="subtitle2" color="textSecondary">Invoice Number</Typography>
                        <Typography variant="h6">{selectedInvoice.invoiceNumber}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Chip 
                          label={selectedInvoice.status} 
                          color={selectedInvoice.status === 'PAID' ? 'success' : selectedInvoice.status === 'OVERDUE' ? 'error' : 'warning'}
                          sx={{ float: 'right' }}
                        />
                      </Grid>
                    </Grid>
                  </Box>
                </Grid>

                {/* Invoice Information */}
                <Grid item xs={6}>
                  <Typography variant="subtitle2" color="textSecondary">Customer</Typography>
                  <Typography variant="body1">{selectedInvoice.customer?.customerName || 'N/A'}</Typography>
                  <Typography variant="caption" color="textSecondary">
                    {selectedInvoice.customer?.customerCode} | {selectedInvoice.customer?.email}
                  </Typography>
                </Grid>
                <Grid item xs={3}>
                  <Typography variant="subtitle2" color="textSecondary">Invoice Date</Typography>
                  <Typography variant="body1">{selectedInvoice.invoiceDate}</Typography>
                </Grid>
                <Grid item xs={3}>
                  <Typography variant="subtitle2" color="textSecondary">Due Date</Typography>
                  <Typography variant="body1">{selectedInvoice.dueDate}</Typography>
                </Grid>

                {/* Invoice Lines */}
                <Grid item xs={12}>
                  <Typography variant="subtitle1" gutterBottom sx={{ mt: 2, mb: 1 }}>
                    Line Items
                  </Typography>
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>#</TableCell>
                          <TableCell>Description</TableCell>
                          <TableCell align="right">Qty</TableCell>
                          <TableCell align="right">Unit Price</TableCell>
                          <TableCell align="right">Tax %</TableCell>
                          <TableCell align="right">Amount</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {selectedInvoice.lines?.map((line: any) => (
                          <TableRow key={line.id}>
                            <TableCell>{line.lineNumber}</TableCell>
                            <TableCell>{line.description}</TableCell>
                            <TableCell align="right">{line.quantity}</TableCell>
                            <TableCell align="right">${line.unitPrice?.toLocaleString()}</TableCell>
                            <TableCell align="right">{line.taxPercent}%</TableCell>
                            <TableCell align="right">${line.lineTotal?.toLocaleString()}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </Grid>

                {/* Totals */}
                <Grid item xs={12}>
                  <Box sx={{ mt: 2, p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
                    <Grid container spacing={1}>
                      <Grid item xs={6}>
                        <Typography variant="body2">Subtotal:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" align="right">${selectedInvoice.subtotal?.toLocaleString()}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2">Tax:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" align="right">${selectedInvoice.taxAmount?.toLocaleString()}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2">Discount:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" align="right">${selectedInvoice.discountAmount?.toLocaleString()}</Typography>
                      </Grid>
                      <Grid item xs={12}>
                        <Box sx={{ borderTop: 1, borderColor: 'divider', my: 1 }} />
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="h6">Total Amount:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="h6" align="right">${selectedInvoice.totalAmount?.toLocaleString()}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" color="primary">Paid Amount:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" align="right" color="primary">${selectedInvoice.paidAmount?.toLocaleString()}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body1" fontWeight="bold" color="error">Balance Due:</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body1" fontWeight="bold" align="right" color="error">${selectedInvoice.balanceDue?.toLocaleString()}</Typography>
                      </Grid>
                    </Grid>
                  </Box>
                </Grid>

                {/* Notes */}
                {selectedInvoice.notes && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="textSecondary">Notes</Typography>
                    <Typography variant="body2">{selectedInvoice.notes}</Typography>
                  </Grid>
                )}
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenViewDialog(false)}>Close</Button>
          {selectedInvoice && Number(selectedInvoice.balanceDue) > 0 && selectedInvoice.status !== 'DRAFT' && (
            <Button 
              variant="contained" 
              startIcon={<PaymentIcon />}
              onClick={() => {
                setOpenViewDialog(false);
                handleOpenPaymentDialog(selectedInvoice);
              }}
            >
              Record Payment
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Record Payment Dialog */}
      <Dialog open={openPaymentDialog} onClose={() => setOpenPaymentDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Payment</DialogTitle>
        <DialogContent>
          {selectedInvoice && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Invoice: {selectedInvoice.invoiceNumber}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Customer: {selectedInvoice.customer?.customerName || selectedInvoice.customerId}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Total Amount: ${selectedInvoice.totalAmount?.toLocaleString()}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom sx={{ mb: 3 }}>
                Balance Due: ${selectedInvoice.balanceDue?.toLocaleString()}
              </Typography>

              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField
                    label="Payment Amount"
                    type="number"
                    value={paymentData.amount}
                    onChange={(e) => setPaymentData({ ...paymentData, amount: parseFloat(e.target.value) })}
                    fullWidth
                    required
                    InputProps={{
                      startAdornment: <Typography sx={{ mr: 1 }}>$</Typography>,
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    label="Payment Date"
                    type="date"
                    value={paymentData.paymentDate}
                    onChange={(e) => setPaymentData({ ...paymentData, paymentDate: e.target.value })}
                    fullWidth
                    required
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    select
                    label="Payment Method"
                    value={paymentData.paymentMethod}
                    onChange={(e) => setPaymentData({ ...paymentData, paymentMethod: e.target.value })}
                    fullWidth
                    required
                  >
                    <MenuItem value="CASH">Cash</MenuItem>
                    <MenuItem value="CHECK">Check</MenuItem>
                    <MenuItem value="BANK_TRANSFER">Bank Transfer</MenuItem>
                    <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
                    <MenuItem value="OTHER">Other</MenuItem>
                  </TextField>
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    label="Reference Number"
                    value={paymentData.referenceNumber}
                    onChange={(e) => setPaymentData({ ...paymentData, referenceNumber: e.target.value })}
                    fullWidth
                    placeholder="e.g., Check #, Transaction ID"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    label="Notes"
                    value={paymentData.notes}
                    onChange={(e) => setPaymentData({ ...paymentData, notes: e.target.value })}
                    fullWidth
                    multiline
                    rows={2}
                  />
                </Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenPaymentDialog(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={handleRecordPayment}
            disabled={!paymentData.amount || paymentData.amount <= 0}
          >
            Record Payment
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Invoices;


