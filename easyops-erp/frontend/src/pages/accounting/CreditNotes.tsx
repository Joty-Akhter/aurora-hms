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
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Grid,
  IconButton,
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon, Check as PostIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';
import FiscalPeriodWarning from '../../components/accounting/FiscalPeriodWarning';
import { getApiErrorMessage } from '../../utils/apiError';

const CreditNotes: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [creditNotes, setCreditNotes] = useState<any[]>([]);
  const [customers, setCustomers] = useState<any[]>([]);
  const [invoices, setInvoices] = useState<any[]>([]);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [periods, setPeriods] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';

  const [formData, setFormData] = useState({
    creditNoteNumber: '',
    creditNoteDate: new Date().toISOString().split('T')[0],
    customerId: '',
    invoiceId: '',
    reason: 'OTHER',
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

  const handleDeleteLine = (index: number) => {
    if (formData.lines.length > 1) {
      const newLines = formData.lines.filter((_, i) => i !== index);
      // Renumber lines
      newLines.forEach((line, idx) => {
        line.lineNumber = idx + 1;
      });
      setFormData({ ...formData, lines: newLines });
    }
  };

  const calculateTotal = () => {
    return formData.lines.reduce((sum, line) => {
      const subtotal = line.quantity * line.unitPrice;
      const discount = subtotal * (line.discountPercent / 100);
      const afterDiscount = subtotal - discount;
      const tax = afterDiscount * (line.taxPercent / 100);
      return sum + afterDiscount + tax;
    }, 0);
  };

  useEffect(() => {
    if (organizationId) {
      loadCreditNotes();
      loadCustomers();
      loadAccounts();
      loadPeriods();
    }
  }, [organizationId]);

  const loadPeriods = async () => {
    try {
      const data = await accountingService.getOpenPeriods(organizationId);
      setPeriods(data);
    } catch (err: unknown) {
      console.error('Failed to load periods:', err);
    }
  };

  const loadCreditNotes = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getARCreditNotes(organizationId);
      setCreditNotes(data);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load credit notes'));
    } finally {
      setLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const data = await accountingService.getARCustomers(organizationId, true);
      setCustomers(data);
    } catch (err: any) {
      console.error('Failed to load customers:', err);
    }
  };

  const loadAccounts = async () => {
    try {
      const data = await accountingService.getActiveAccounts(organizationId);
      // Filter for revenue accounts
      const revenueAccounts = data.filter((acc: any) => 
        acc.accountType === 'REVENUE' || acc.accountType === 'INCOME'
      );
      setAccounts(revenueAccounts);
    } catch (err: any) {
      console.error('Failed to load accounts:', err);
    }
  };

  const loadCustomerInvoices = async (customerId: string) => {
    try {
      const data = await accountingService.getARInvoices(organizationId);
      // Filter for outstanding invoices of selected customer
      const customerInvoices = data.filter(
        (inv: any) =>
          (inv.customer?.id === customerId || inv.customerId === customerId) &&
          inv.status === 'POSTED' &&
          Number(inv.balanceDue) > 0
      );
      setInvoices(customerInvoices);
    } catch (err: any) {
      console.error('Failed to load invoices:', err);
    }
  };

  const handleCustomerChange = (customerId: string) => {
    setFormData({ ...formData, customerId, invoiceId: '' });
    if (customerId) {
      loadCustomerInvoices(customerId);
    } else {
      setInvoices([]);
    }
  };

  const handlePostCreditNote = async (creditNoteId: string) => {
    setError(null);
    setSuccess(null);
    try {
      await accountingService.postARCreditNote(creditNoteId);
      setSuccess('Credit note posted successfully!');
      loadCreditNotes();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to post credit note'));
    }
  };

  const handleCreateCreditNote = async (postAfterCreate = false) => {
    setError(null);
    setSuccess(null);

    // Validation
    if (!formData.creditNoteNumber) {
      setError('Credit note number is required');
      return;
    }
    if (!formData.customerId) {
      setError('Please select a customer');
      return;
    }
    if (formData.lines.length === 0 || !formData.lines[0].accountId) {
      setError('Please add at least one line with a revenue account');
      return;
    }

    try {
      const creditNoteData = {
        organizationId,
        creditNoteNumber: formData.creditNoteNumber,
        creditNoteDate: formData.creditNoteDate,
        customerId: formData.customerId,
        invoiceId: formData.invoiceId || null,
        reason: formData.reason,
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

      const created = await accountingService.createARCreditNote(creditNoteData);
      if (postAfterCreate && created?.id) {
        await accountingService.postARCreditNote(created.id);
        setSuccess('Credit note created and posted successfully!');
      } else {
        setSuccess(
          postAfterCreate
            ? 'Credit note created but could not be posted automatically. Use Post from the list.'
            : 'Credit note created successfully! Post it from the list when ready.'
        );
      }
      setOpenDialog(false);
      
      // Reset form
      setFormData({
        creditNoteNumber: '',
        creditNoteDate: new Date().toISOString().split('T')[0],
        customerId: '',
        invoiceId: '',
        reason: 'OTHER',
        notes: '',
        lines: [
          { lineNumber: 1, description: '', quantity: 1, unitPrice: 0, taxPercent: 0, discountPercent: 0, accountId: '' },
        ],
      });
      
      loadCreditNotes();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to create credit note'));
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Credit Notes</Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Create Credit Note
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
            Credit Notes List
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Credit Note #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Invoice</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell>Reason</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2">Loading credit notes...</Typography>
                    </TableCell>
                  </TableRow>
                ) : creditNotes.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No credit notes yet. Create your first credit note to get started!
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  creditNotes.map((cn) => (
                    <TableRow key={cn.id}>
                      <TableCell>{cn.creditNoteNumber}</TableCell>
                      <TableCell>{cn.creditNoteDate}</TableCell>
                      <TableCell>{cn.customer?.customerName}</TableCell>
                      <TableCell>{cn.invoice?.invoiceNumber || '-'}</TableCell>
                      <TableCell align="right">${cn.totalAmount?.toFixed(2)}</TableCell>
                      <TableCell>{cn.reason}</TableCell>
                      <TableCell>
                        <Chip 
                          label={cn.status} 
                          size="small"
                          color={cn.status === 'POSTED' ? 'success' : cn.status === 'DRAFT' ? 'warning' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        {canManage && cn.status === 'DRAFT' && (
                          <IconButton
                            size="small"
                            title="Post Credit Note"
                            onClick={() => handlePostCreditNote(cn.id)}
                            color="primary"
                          >
                            <PostIcon fontSize="small" />
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Credit Note Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create Credit Note</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={1}>
            <Grid item xs={6}>
              <TextField
                label="Credit Note Date"
                type="date"
                value={formData.creditNoteDate}
                onChange={(e) => setFormData({ ...formData, creditNoteDate: e.target.value })}
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Credit Note Number"
                value={formData.creditNoteNumber}
                onChange={(e) => setFormData({ ...formData, creditNoteNumber: e.target.value })}
                fullWidth
                required
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                select
                label="Customer"
                value={formData.customerId}
                onChange={(e) => handleCustomerChange(e.target.value)}
                fullWidth
                required
              >
                <MenuItem value="">Select Customer</MenuItem>
                {customers.map((customer) => (
                  <MenuItem key={customer.id} value={customer.id}>
                    {customer.customerName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Related Invoice (Optional)"
                value={formData.invoiceId}
                onChange={(e) => setFormData({ ...formData, invoiceId: e.target.value })}
                fullWidth
                disabled={!formData.customerId}
              >
                <MenuItem value="">None</MenuItem>
                {invoices.map((invoice) => (
                  <MenuItem key={invoice.id} value={invoice.id}>
                    {invoice.invoiceNumber} - ${invoice.balanceDue?.toFixed(2)}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                select
                label="Reason"
                value={formData.reason}
                onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                fullWidth
              >
                <MenuItem value="RETURN">Return</MenuItem>
                <MenuItem value="DAMAGE">Damage</MenuItem>
                <MenuItem value="PRICING_ERROR">Pricing Error</MenuItem>
                <MenuItem value="DISCOUNT">Discount</MenuItem>
                <MenuItem value="CANCELLATION">Cancellation</MenuItem>
                <MenuItem value="OTHER">Other</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Notes"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                fullWidth
                multiline
                rows={2}
              />
            </Grid>

            {/* Credit Note Lines */}
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
                Credit Note Lines
              </Typography>
              {formData.lines.map((line, index) => (
                <Box key={index} sx={{ border: '1px solid #e0e0e0', p: 2, mb: 2, borderRadius: 1 }}>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <TextField
                        label="Description"
                        value={line.description}
                        onChange={(e) => {
                          const newLines = [...formData.lines];
                          newLines[index].description = e.target.value;
                          setFormData({ ...formData, lines: newLines });
                        }}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={4}>
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
                        required
                      >
                        <MenuItem value="">Select Account</MenuItem>
                        {accounts.map((account) => (
                          <MenuItem key={account.id} value={account.id}>
                            {account.accountCode} - {account.accountName}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={2}>
                      <TextField
                        label="Quantity"
                        type="number"
                        value={line.quantity}
                        onChange={(e) => {
                          const newLines = [...formData.lines];
                          newLines[index].quantity = parseFloat(e.target.value) || 0;
                          setFormData({ ...formData, lines: newLines });
                        }}
                        fullWidth
                        inputProps={{ min: 0, step: 0.01 }}
                      />
                    </Grid>
                    <Grid item xs={2}>
                      <TextField
                        label="Unit Price"
                        type="number"
                        value={line.unitPrice}
                        onChange={(e) => {
                          const newLines = [...formData.lines];
                          newLines[index].unitPrice = parseFloat(e.target.value) || 0;
                          setFormData({ ...formData, lines: newLines });
                        }}
                        fullWidth
                        inputProps={{ min: 0, step: 0.01 }}
                      />
                    </Grid>
                    <Grid item xs={2}>
                      <TextField
                        label="Tax %"
                        type="number"
                        value={line.taxPercent}
                        onChange={(e) => {
                          const newLines = [...formData.lines];
                          newLines[index].taxPercent = parseFloat(e.target.value) || 0;
                          setFormData({ ...formData, lines: newLines });
                        }}
                        fullWidth
                        inputProps={{ min: 0, max: 100, step: 0.01 }}
                      />
                    </Grid>
                    <Grid item xs={2}>
                      <IconButton
                        onClick={() => handleDeleteLine(index)}
                        disabled={formData.lines.length === 1}
                        color="error"
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Grid>
                  </Grid>
                </Box>
              ))}
              <Button
                startIcon={<AddIcon />}
                onClick={handleAddLine}
                variant="outlined"
              >
                Add Line
              </Button>
            </Grid>

            <Grid item xs={12}>
              <Box sx={{ mt: 2, p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
                <Typography variant="h6" align="right">
                  Total: ${calculateTotal().toFixed(2)}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button
            onClick={() => handleCreateCreditNote(false)}
            disabled={!formData.creditNoteNumber || !formData.customerId}
          >
            Create Draft
          </Button>
          <Button
            variant="contained"
            onClick={() => handleCreateCreditNote(true)}
            disabled={!formData.creditNoteNumber || !formData.customerId}
          >
            Create &amp; Post
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CreditNotes;