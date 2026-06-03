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

const Bills: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [bills, setBills] = useState<any[]>([]);
  const [vendors, setVendors] = useState<any[]>([]);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [periods, setPeriods] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [openPaymentDialog, setOpenPaymentDialog] = useState(false);
  const [selectedBill, setSelectedBill] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';

  const [paymentData, setPaymentData] = useState({
    amount: 0,
    paymentDate: new Date().toISOString().split('T')[0],
    paymentMethod: 'BANK_TRANSFER',
    checkNumber: '',
    referenceNumber: '',
    notes: '',
  });

  const [formData, setFormData] = useState({
    billNumber: '',
    billDate: new Date().toISOString().split('T')[0],
    vendorId: '',
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
      loadBills();
      loadVendors();
      loadAccounts();
      loadPeriods();
    }
  }, [organizationId]);

  const loadBills = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getAPBills(organizationId);
      setBills(data);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load bills'));
    } finally {
      setLoading(false);
    }
  };

  const loadVendors = async () => {
    try {
      const data = await accountingService.getAPVendors(organizationId);
      setVendors(data);
    } catch (err: any) {
      console.error('Failed to load vendors:', err);
    }
  };

  const loadAccounts = async () => {
    try {
      const data = await accountingService.getActiveAccounts(organizationId);
      const expenseAccounts = data.filter(a => a.accountType === 'EXPENSE' && !a.isGroup);
      setAccounts(expenseAccounts);
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

  const handlePostBill = async (billId: string) => {
    setError(null);
    setSuccess(null);
    try {
      await accountingService.postAPBill(billId);
      setSuccess('Bill posted successfully!');
      loadBills();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to post bill'));
    }
  };

  const handleCreateBill = async (postAfterCreate = false) => {
    setError(null);
    setSuccess(null);

    // Validation
    if (!formData.billNumber) {
      setError('Bill number is required');
      return;
    }
    if (!formData.vendorId) {
      setError('Please select a vendor');
      return;
    }
    if (formData.lines.length === 0 || !formData.lines[0].accountId) {
      setError('Please add at least one bill line with an expense account');
      return;
    }

    try {
      // Calculate due date
      const billDate = new Date(formData.billDate);
      const dueDate = new Date(billDate);
      dueDate.setDate(dueDate.getDate() + formData.paymentTerms);

      const billData = {
        organizationId,
        billNumber: formData.billNumber,
        billDate: formData.billDate,
        dueDate: dueDate.toISOString().split('T')[0],
        vendorId: formData.vendorId,
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

      const created = await accountingService.createAPBill(billData);
      if (postAfterCreate && created?.id) {
        await accountingService.postAPBill(created.id);
        setSuccess('Bill created and posted successfully!');
      } else {
        setSuccess(
          postAfterCreate
            ? 'Bill created but could not be posted automatically. Use Post from the list.'
            : 'Bill created successfully! Post it from the list when ready.'
        );
      }
      setOpenDialog(false);
      
      // Reset form
      setFormData({
        billNumber: '',
        billDate: new Date().toISOString().split('T')[0],
        vendorId: '',
        paymentTerms: 30,
        notes: '',
        lines: [
          { lineNumber: 1, description: '', quantity: 1, unitPrice: 0, taxPercent: 0, discountPercent: 0, accountId: '' },
        ],
      });
      
      loadBills();
    } catch (err: any) {
      setError(err.message || 'Failed to create bill');
    }
  };

  const handleOpenPaymentDialog = (bill: any) => {
    setSelectedBill(bill);
    setPaymentData({
      amount: bill.balanceDue || 0,
      paymentDate: new Date().toISOString().split('T')[0],
      paymentMethod: 'BANK_TRANSFER',
      checkNumber: '',
      referenceNumber: '',
      notes: '',
    });
    setOpenPaymentDialog(true);
  };

  const handleRecordPayment = async () => {
    if (!selectedBill) return;
    
    setError(null);
    setSuccess(null);

    if (paymentData.amount <= 0) {
      setError('Payment amount must be greater than 0');
      return;
    }

    if (paymentData.amount > selectedBill.balanceDue) {
      setError('Payment amount cannot exceed balance due');
      return;
    }

    try {
      // Generate payment number
      const timestamp = Date.now();
      const paymentNumber = `PAY-${selectedBill.billNumber}-${timestamp}`;

      // Create payment with allocation to the bill
      const paymentRequestData = {
        organizationId,
        paymentNumber,
        paymentDate: paymentData.paymentDate,
        vendorId: selectedBill.vendor?.id || selectedBill.vendorId,
        paymentMethod: paymentData.paymentMethod,
        checkNumber: paymentData.checkNumber,
        referenceNumber: paymentData.referenceNumber,
        amount: paymentData.amount,
        notes: paymentData.notes,
        allocations: [
          {
            billId: selectedBill.id,
            allocatedAmount: paymentData.amount,
          }
        ],
      };

      // Create and post the payment
      const payment = await accountingService.createAPPayment(paymentRequestData);
      await accountingService.postAPPayment(payment.id);
      
      setSuccess(`Payment of $${paymentData.amount} recorded for bill ${selectedBill.billNumber}`);
      setOpenPaymentDialog(false);
      setSelectedBill(null);
      loadBills();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to record payment'));
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Accounts Payable - Bills</Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
          >
            Create Bill
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
            Vendor Bills
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Bill #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Vendor</TableCell>
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
                      <Typography variant="body2">Loading bills...</Typography>
                    </TableCell>
                  </TableRow>
                ) : bills.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No bills yet. Create your first vendor bill to get started!
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  bills.map((bill) => (
                    <TableRow key={bill.id}>
                      <TableCell>{bill.billNumber}</TableCell>
                      <TableCell>{bill.billDate}</TableCell>
                      <TableCell>{bill.vendorId}</TableCell>
                      <TableCell>{bill.dueDate}</TableCell>
                      <TableCell align="right">{bill.totalAmount?.toLocaleString()}</TableCell>
                      <TableCell align="right">{bill.balanceDue?.toLocaleString()}</TableCell>
                      <TableCell>
                        <Chip 
                          label={bill.status} 
                          size="small"
                          color={bill.status === 'PAID' ? 'success' : bill.status === 'OVERDUE' ? 'error' : 'warning'}
                        />
                      </TableCell>
                      <TableCell>
                        {canManage && bill.status === 'DRAFT' && (
                          <IconButton
                            size="small"
                            title="Post Bill"
                            onClick={() => handlePostBill(bill.id)}
                            color="primary"
                          >
                            <PostIcon fontSize="small" />
                          </IconButton>
                        )}
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => handleOpenPaymentDialog(bill)}
                          disabled={bill.status === 'DRAFT' || Number(bill.balanceDue) <= 0}
                        >
                          Pay
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Bill Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create Vendor Bill</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={1}>
            <Grid item xs={6}>
              <TextField
                label="Bill Date"
                type="date"
                value={formData.billDate}
                onChange={(e) => setFormData({ ...formData, billDate: e.target.value })}
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
                label="Bill Number"
                value={formData.billNumber}
                onChange={(e) => setFormData({ ...formData, billNumber: e.target.value })}
                fullWidth
                required
                placeholder="e.g., BILL-001"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Vendor"
                value={formData.vendorId}
                onChange={(e) => setFormData({ ...formData, vendorId: e.target.value })}
                fullWidth
                required
              >
                <MenuItem value="">Select Vendor</MenuItem>
                {vendors.map(vendor => (
                  <MenuItem key={vendor.id} value={vendor.id}>
                    {vendor.vendorCode} - {vendor.vendorName}
                  </MenuItem>
                ))}
              </TextField>
              {vendors.length === 0 && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  No vendors found. Please create vendors in the AP module first.
                </Typography>
              )}
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Bill Lines
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
                      label="Expense Account"
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
                        newLines[index].quantity = parseFloat(e.target.value) || 0;
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
                        newLines[index].unitPrice = parseFloat(e.target.value) || 0;
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
                        newLines[index].taxPercent = parseFloat(e.target.value) || 0;
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
          <Button onClick={() => handleCreateBill(false)} disabled={!formData.billNumber || !formData.vendorId}>
            Create Draft
          </Button>
          <Button
            variant="contained"
            onClick={() => handleCreateBill(true)}
            disabled={!formData.billNumber || !formData.vendorId}
          >
            Create &amp; Post
          </Button>
        </DialogActions>
      </Dialog>

      {/* Payment Dialog */}
      <Dialog open={openPaymentDialog} onClose={() => setOpenPaymentDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Payment</DialogTitle>
        <DialogContent>
          {selectedBill && (
            <Box mt={2}>
              <Typography variant="subtitle2" gutterBottom>
                Bill: {selectedBill.billNumber}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Vendor: {selectedBill.vendor?.vendorName || selectedBill.vendorId}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Total Amount: ${selectedBill.totalAmount?.toFixed(2)}
              </Typography>
              <Typography variant="body2" color="primary" gutterBottom>
                <strong>Balance Due: ${selectedBill.balanceDue?.toFixed(2)}</strong>
              </Typography>

              <Grid container spacing={2} mt={2}>
                <Grid item xs={12}>
                  <TextField
                    label="Payment Amount"
                    type="number"
                    value={paymentData.amount}
                    onChange={(e) => setPaymentData({ ...paymentData, amount: parseFloat(e.target.value) || 0 })}
                    fullWidth
                    inputProps={{ min: 0, max: selectedBill.balanceDue, step: 0.01 }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    label="Payment Date"
                    type="date"
                    value={paymentData.paymentDate}
                    onChange={(e) => setPaymentData({ ...paymentData, paymentDate: e.target.value })}
                    fullWidth
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
                  >
                    <MenuItem value="CASH">Cash</MenuItem>
                    <MenuItem value="CHECK">Check</MenuItem>
                    <MenuItem value="BANK_TRANSFER">Bank Transfer</MenuItem>
                    <MenuItem value="WIRE">Wire</MenuItem>
                    <MenuItem value="CARD">Card</MenuItem>
                  </TextField>
                </Grid>

                {paymentData.paymentMethod === 'CHECK' && (
                  <Grid item xs={12}>
                    <TextField
                      label="Check Number"
                      value={paymentData.checkNumber}
                      onChange={(e) => setPaymentData({ ...paymentData, checkNumber: e.target.value })}
                      fullWidth
                    />
                  </Grid>
                )}

                <Grid item xs={12}>
                  <TextField
                    label="Reference Number"
                    value={paymentData.referenceNumber}
                    onChange={(e) => setPaymentData({ ...paymentData, referenceNumber: e.target.value })}
                    fullWidth
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

export default Bills;


