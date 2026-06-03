import React, { useState, useEffect } from "react";
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Chip,
  Alert,
  Grid,
  Switch,
  FormControlLabel,
} from "@mui/material";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Block as DeactivateIcon,
} from "@mui/icons-material";
import { useAuth } from "../../contexts/AuthContext";
import salesService from "../../services/salesService";
import { Customer } from "../../types/sales";

const Customers = () => {
  const { currentOrganizationId } = useAuth();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const [openDialog, setOpenDialog] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);
  
  const [formData, setFormData] = useState({
    customerCode: "",
    customerName: "",
    email: "",
    phone: "",
    contactPerson: "",
    billingAddress: "",
    shippingAddress: "",
    creditLimit: 0,
    paymentTerms: "Net 30",
    taxNumber: "",
    isActive: true,
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadCustomers();
    }
  }, [currentOrganizationId]);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      const data = await salesService.getCustomers(currentOrganizationId!, false);
      setCustomers(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (customer?: Customer) => {
    if (customer) {
      setEditingCustomer(customer);
      setFormData({
        customerCode: customer.customerCode,
        customerName: customer.customerName,
        email: customer.email || "",
        phone: customer.phone || "",
        contactPerson: customer.contactPerson || "",
        billingAddress: customer.billingAddress || "",
        shippingAddress: customer.shippingAddress || "",
        creditLimit: customer.creditLimit || 0,
        paymentTerms: customer.paymentTerms || "Net 30",
        taxNumber: customer.taxNumber || "",
        isActive: customer.isActive,
      });
    } else {
      setEditingCustomer(null);
      setFormData({
        customerCode: "",
        customerName: "",
        email: "",
        phone: "",
        contactPerson: "",
        billingAddress: "",
        shippingAddress: "",
        creditLimit: 0,
        paymentTerms: "Net 30",
        taxNumber: "",
        isActive: true,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingCustomer(null);
  };

  const handleSubmit = async () => {
    if (!formData.customerCode || !formData.customerName) {
      setError("Customer code and name are required");
      return;
    }

    setLoading(true);
    try {
      const request = {
        organizationId: currentOrganizationId!,
        ...formData,
      };

      if (editingCustomer) {
        await salesService.updateCustomer(editingCustomer.id, request);
        setSuccess("Customer updated successfully");
      } else {
        await salesService.createCustomer(request);
        setSuccess("Customer created successfully");
      }

      await loadCustomers();
      handleCloseDialog();
      setError("");
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm("Are you sure you want to delete this customer?")) {
      try {
        await salesService.deleteCustomer(id);
        await loadCustomers();
        setSuccess("Customer deleted successfully");
      } catch (err: any) {
        setError(err.message);
      }
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      await salesService.deactivateCustomer(id);
      await loadCustomers();
      setSuccess("Customer deactivated successfully");
    } catch (err: any) {
      setError(err.message);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Customers</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Add Customer
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess("")}>
          {success}
        </Alert>
      )}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Customer Code</TableCell>
                  <TableCell>Customer Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell>Contact Person</TableCell>
                  <TableCell align="right">Credit Limit</TableCell>
                  <TableCell>Payment Terms</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {customers.map((customer) => (
                  <TableRow key={customer.id}>
                    <TableCell>{customer.customerCode}</TableCell>
                    <TableCell>{customer.customerName}</TableCell>
                    <TableCell>{customer.email}</TableCell>
                    <TableCell>{customer.phone}</TableCell>
                    <TableCell>{customer.contactPerson}</TableCell>
                    <TableCell align="right">{formatCurrency(customer.creditLimit || 0)}</TableCell>
                    <TableCell>{customer.paymentTerms}</TableCell>
                    <TableCell>
                      <Chip 
                        label={customer.isActive ? "Active" : "Inactive"} 
                        color={customer.isActive ? "success" : "default"} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => handleOpenDialog(customer)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      {customer.isActive && (
                        <IconButton size="small" color="warning" onClick={() => handleDeactivate(customer.id)}>
                          <DeactivateIcon fontSize="small" />
                        </IconButton>
                      )}
                      <IconButton size="small" color="error" onClick={() => handleDelete(customer.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editingCustomer ? "Edit Customer" : "Add Customer"}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Customer Code"
                value={formData.customerCode}
                onChange={(e) => setFormData({ ...formData, customerCode: e.target.value })}
                disabled={!!editingCustomer}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Customer Name"
                value={formData.customerName}
                onChange={(e) => setFormData({ ...formData, customerName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Phone"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Contact Person"
                value={formData.contactPerson}
                onChange={(e) => setFormData({ ...formData, contactPerson: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Tax Number"
                value={formData.taxNumber}
                onChange={(e) => setFormData({ ...formData, taxNumber: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Billing Address"
                value={formData.billingAddress}
                onChange={(e) => setFormData({ ...formData, billingAddress: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Shipping Address"
                value={formData.shippingAddress}
                onChange={(e) => setFormData({ ...formData, shippingAddress: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Credit Limit"
                type="number"
                value={formData.creditLimit}
                onChange={(e) => setFormData({ ...formData, creditLimit: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Payment Terms"
                value={formData.paymentTerms}
                onChange={(e) => setFormData({ ...formData, paymentTerms: e.target.value })}
                placeholder="e.g., Net 30, Net 15"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                  />
                }
                label="Active"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={loading}>
            {editingCustomer ? "Update" : "Create"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Customers;

