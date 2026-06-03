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
import accountingService from "../../services/accountingService";
import { Vendor } from "../../types/ar-ap";

const Vendors = () => {
  const { currentOrganizationId } = useAuth();
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const [openDialog, setOpenDialog] = useState(false);
  const [editingVendor, setEditingVendor] = useState<Vendor | null>(null);
  
  const [formData, setFormData] = useState({
    vendorCode: "",
    vendorName: "",
    email: "",
    phone: "",
    paymentTerms: 30,
    taxId: "",
    isActive: true,
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadVendors();
    }
  }, [currentOrganizationId]);

  const loadVendors = async () => {
    try {
      setLoading(true);
      const data = await accountingService.getAPVendors(currentOrganizationId!, false);
      setVendors(data);
    } catch (err: any) {
      setError(err.message || "Failed to load vendors");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (vendor?: Vendor) => {
    if (vendor) {
      setEditingVendor(vendor);
      setFormData({
        vendorCode: vendor.vendorCode,
        vendorName: vendor.vendorName,
        email: vendor.email || "",
        phone: vendor.phone || "",
        paymentTerms: vendor.paymentTerms || 30,
        taxId: vendor.taxId || "",
        isActive: vendor.isActive,
      });
    } else {
      setEditingVendor(null);
      setFormData({
        vendorCode: "",
        vendorName: "",
        email: "",
        phone: "",
        paymentTerms: 30,
        taxId: "",
        isActive: true,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingVendor(null);
  };

  const handleSubmit = async () => {
    if (!formData.vendorCode || !formData.vendorName) {
      setError("Vendor code and name are required");
      return;
    }

    setLoading(true);
    try {
      const request = {
        organizationId: currentOrganizationId!,
        ...formData,
      };

      if (editingVendor) {
        await accountingService.updateVendor(editingVendor.id, request);
        setSuccess("Vendor updated successfully");
      } else {
        await accountingService.createVendor(request);
        setSuccess("Vendor created successfully");
      }

      await loadVendors();
      handleCloseDialog();
      setError("");
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || "Failed to save vendor");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm("Are you sure you want to delete this vendor?")) {
      try {
        await accountingService.deleteVendor(id);
        await loadVendors();
        setSuccess("Vendor deleted successfully");
      } catch (err: any) {
        setError(err.response?.data?.message || err.message || "Failed to delete vendor");
      }
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      const vendor = vendors.find(v => v.id === id);
      if (vendor) {
        await accountingService.updateVendor(id, {
          organizationId: currentOrganizationId!,
          vendorCode: vendor.vendorCode,
          vendorName: vendor.vendorName,
          email: vendor.email,
          phone: vendor.phone,
          paymentTerms: vendor.paymentTerms,
          taxId: vendor.taxId,
          isActive: false,
        });
        await loadVendors();
        setSuccess("Vendor deactivated successfully");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || "Failed to deactivate vendor");
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Vendors</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Add Vendor
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
                  <TableCell>Vendor Code</TableCell>
                  <TableCell>Vendor Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell>Payment Terms</TableCell>
                  <TableCell>Tax ID</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading && vendors.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      Loading vendors...
                    </TableCell>
                  </TableRow>
                ) : vendors.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      No vendors found. Click "Add Vendor" to create one.
                    </TableCell>
                  </TableRow>
                ) : (
                  vendors.map((vendor) => (
                    <TableRow key={vendor.id}>
                      <TableCell>{vendor.vendorCode}</TableCell>
                      <TableCell>{vendor.vendorName}</TableCell>
                      <TableCell>{vendor.email || "-"}</TableCell>
                      <TableCell>{vendor.phone || "-"}</TableCell>
                      <TableCell>Net {vendor.paymentTerms || 30}</TableCell>
                      <TableCell>{vendor.taxId || "-"}</TableCell>
                      <TableCell>
                        <Chip 
                          label={vendor.isActive ? "Active" : "Inactive"} 
                          color={vendor.isActive ? "success" : "default"} 
                          size="small" 
                        />
                      </TableCell>
                      <TableCell align="center">
                        <IconButton size="small" onClick={() => handleOpenDialog(vendor)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                        {vendor.isActive && (
                          <IconButton size="small" color="warning" onClick={() => handleDeactivate(vendor.id)}>
                            <DeactivateIcon fontSize="small" />
                          </IconButton>
                        )}
                        <IconButton size="small" color="error" onClick={() => handleDelete(vendor.id)}>
                          <DeleteIcon fontSize="small" />
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

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editingVendor ? "Edit Vendor" : "Add Vendor"}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Vendor Code"
                value={formData.vendorCode}
                onChange={(e) => setFormData({ ...formData, vendorCode: e.target.value })}
                disabled={!!editingVendor}
                required
                helperText={editingVendor ? "Vendor code cannot be changed" : "Unique identifier for the vendor"}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Vendor Name"
                value={formData.vendorName}
                onChange={(e) => setFormData({ ...formData, vendorName: e.target.value })}
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
                label="Payment Terms (Days)"
                type="number"
                value={formData.paymentTerms}
                onChange={(e) => setFormData({ ...formData, paymentTerms: parseInt(e.target.value) || 30 })}
                helperText="Number of days until payment is due (e.g., 30 for Net 30)"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Tax ID"
                value={formData.taxId}
                onChange={(e) => setFormData({ ...formData, taxId: e.target.value })}
                helperText="Tax identification number"
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
            {editingVendor ? "Update" : "Create"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Vendors;
