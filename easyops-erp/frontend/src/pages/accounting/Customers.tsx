import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';

interface Customer {
  id: string;
  customerCode: string;
  customerName: string;
  email?: string;
  phone?: string;
  creditLimit?: number;
  currentBalance?: number;
  paymentTerms?: number;
  isActive?: boolean;
}

const Customers: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [formData, setFormData] = useState({
    customerCode: '',
    customerName: '',
    email: '',
    phone: '',
    creditLimit: 0,
    paymentTerms: 30,
    isActive: true,
  });

  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) loadCustomers();
  }, [organizationId]);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      const data = await accountingService.getARCustomers(organizationId, false);
      setCustomers(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (customer?: Customer) => {
    if (customer) {
      setEditing(customer);
      setFormData({
        customerCode: customer.customerCode,
        customerName: customer.customerName,
        email: customer.email || '',
        phone: customer.phone || '',
        creditLimit: customer.creditLimit ?? 0,
        paymentTerms: customer.paymentTerms ?? 30,
        isActive: customer.isActive ?? true,
      });
    } else {
      setEditing(null);
      setFormData({
        customerCode: '',
        customerName: '',
        email: '',
        phone: '',
        creditLimit: 0,
        paymentTerms: 30,
        isActive: true,
      });
    }
    setOpenDialog(true);
  };

  const handleSave = async () => {
    if (!canManage) return;
    setError('');
    setSuccess('');
    try {
      const payload = { organizationId, ...formData };
      if (editing) {
        await accountingService.updateARCustomer(editing.id, payload);
        setSuccess('Customer updated');
      } else {
        await accountingService.createARCustomer(payload);
        setSuccess('Customer created');
      }
      setOpenDialog(false);
      loadCustomers();
    } catch (err: any) {
      setError(err.message || 'Failed to save customer');
    }
  };

  const handleDelete = async (id: string) => {
    if (!canManage || !window.confirm('Deactivate this customer?')) return;
    try {
      await accountingService.deleteARCustomer(id);
      setSuccess('Customer removed');
      loadCustomers();
    } catch (err: any) {
      setError(err.message || 'Failed to delete customer');
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" mb={3}>
        <Typography variant="h4">AR Customers</Typography>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
            Add Customer
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}
      {!canManage && (
        <Alert severity="info" sx={{ mb: 2 }}>You have view-only access. Contact an administrator to manage customers.</Alert>
      )}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell align="right">Credit Limit</TableCell>
                  <TableCell align="right">Balance</TableCell>
                  <TableCell>Status</TableCell>
                  {canManage && <TableCell align="right">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={7}>Loading...</TableCell></TableRow>
                ) : customers.length === 0 ? (
                  <TableRow><TableCell colSpan={7}>No customers found</TableCell></TableRow>
                ) : (
                  customers.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell>{c.customerCode}</TableCell>
                      <TableCell>{c.customerName}</TableCell>
                      <TableCell>{c.email || '—'}</TableCell>
                      <TableCell align="right">{c.creditLimit ?? 0}</TableCell>
                      <TableCell align="right">{c.currentBalance ?? 0}</TableCell>
                      <TableCell>
                        <Chip label={c.isActive ? 'Active' : 'Inactive'} size="small" color={c.isActive ? 'success' : 'default'} />
                      </TableCell>
                      {canManage && (
                        <TableCell align="right">
                          <IconButton size="small" onClick={() => handleOpenDialog(c)}><EditIcon /></IconButton>
                          <IconButton size="small" color="error" onClick={() => handleDelete(c.id)}><DeleteIcon /></IconButton>
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Customer' : 'New Customer'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <TextField label="Customer Code" fullWidth required value={formData.customerCode}
                onChange={(e) => setFormData({ ...formData, customerCode: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Customer Name" fullWidth required value={formData.customerName}
                onChange={(e) => setFormData({ ...formData, customerName: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Email" fullWidth value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Phone" fullWidth value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Credit Limit" type="number" fullWidth value={formData.creditLimit}
                onChange={(e) => setFormData({ ...formData, creditLimit: Number(e.target.value) })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Payment Terms (days)" type="number" fullWidth value={formData.paymentTerms}
                onChange={(e) => setFormData({ ...formData, paymentTerms: Number(e.target.value) })} />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel control={
                <Switch checked={formData.isActive} onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })} />
              } label="Active" />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Customers;
