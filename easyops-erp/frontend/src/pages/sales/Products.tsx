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
  MenuItem,
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
import { Product } from "../../types/sales";

const Products = () => {
  const { currentOrganizationId } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const [openDialog, setOpenDialog] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  
  const [formData, setFormData] = useState({
    productCode: "",
    productName: "",
    description: "",
    productType: "GOODS",
    category: "",
    unitOfMeasure: "",
    unitPrice: 0,
    costPrice: 0,
    taxRate: 0,
    isActive: true,
    trackInventory: false,
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadProducts();
    }
  }, [currentOrganizationId]);

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await salesService.getProducts(currentOrganizationId!, false);
      setProducts(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (product?: Product) => {
    if (product) {
      setEditingProduct(product);
      setFormData({
        productCode: product.productCode,
        productName: product.productName,
        description: product.description || "",
        productType: product.productType,
        category: product.category || "",
        unitOfMeasure: product.unitOfMeasure || "",
        unitPrice: product.unitPrice,
        costPrice: product.costPrice || 0,
        taxRate: product.taxRate || 0,
        isActive: product.isActive,
        trackInventory: product.trackInventory || false,
      });
    } else {
      setEditingProduct(null);
      setFormData({
        productCode: "",
        productName: "",
        description: "",
        productType: "GOODS",
        category: "",
        unitOfMeasure: "",
        unitPrice: 0,
        costPrice: 0,
        taxRate: 0,
        isActive: true,
        trackInventory: false,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingProduct(null);
  };

  const handleSubmit = async () => {
    if (!formData.productCode || !formData.productName) {
      setError("Product code and name are required");
      return;
    }

    setLoading(true);
    try {
      const request = {
        organizationId: currentOrganizationId!,
        ...formData,
      };

      if (editingProduct) {
        await salesService.updateProduct(editingProduct.id, request);
        setSuccess("Product updated successfully");
      } else {
        await salesService.createProduct(request);
        setSuccess("Product created successfully");
      }

      await loadProducts();
      handleCloseDialog();
      setError("");
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm("Are you sure you want to delete this product?")) {
      try {
        await salesService.deleteProduct(id);
        await loadProducts();
        setSuccess("Product deleted successfully");
      } catch (err: any) {
        setError(err.message);
      }
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      await salesService.deactivateProduct(id);
      await loadProducts();
      setSuccess("Product deactivated successfully");
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
        <Typography variant="h4">Products</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Add Product
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
                  <TableCell>Product Code</TableCell>
                  <TableCell>Product Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Unit</TableCell>
                  <TableCell align="right">Unit Price</TableCell>
                  <TableCell align="right">Cost Price</TableCell>
                  <TableCell align="right">Tax Rate</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>{product.productCode}</TableCell>
                    <TableCell>{product.productName}</TableCell>
                    <TableCell>
                      <Chip label={product.productType} size="small" color={product.productType === 'GOODS' ? 'primary' : 'secondary'} />
                    </TableCell>
                    <TableCell>{product.category}</TableCell>
                    <TableCell>{product.unitOfMeasure}</TableCell>
                    <TableCell align="right">{formatCurrency(product.unitPrice)}</TableCell>
                    <TableCell align="right">{formatCurrency(product.costPrice || 0)}</TableCell>
                    <TableCell align="right">{product.taxRate}%</TableCell>
                    <TableCell>
                      <Chip 
                        label={product.isActive ? "Active" : "Inactive"} 
                        color={product.isActive ? "success" : "default"} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => handleOpenDialog(product)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      {product.isActive && (
                        <IconButton size="small" color="warning" onClick={() => handleDeactivate(product.id)}>
                          <DeactivateIcon fontSize="small" />
                        </IconButton>
                      )}
                      <IconButton size="small" color="error" onClick={() => handleDelete(product.id)}>
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
        <DialogTitle>{editingProduct ? "Edit Product" : "Add Product"}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Product Code"
                value={formData.productCode}
                onChange={(e) => setFormData({ ...formData, productCode: e.target.value })}
                disabled={!!editingProduct}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Product Name"
                value={formData.productName}
                onChange={(e) => setFormData({ ...formData, productName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Product Type"
                value={formData.productType}
                onChange={(e) => setFormData({ ...formData, productType: e.target.value })}
              >
                <MenuItem value="GOODS">Goods</MenuItem>
                <MenuItem value="SERVICE">Service</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Category"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Unit of Measure"
                value={formData.unitOfMeasure}
                onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
                placeholder="e.g., pcs, kg, hour"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Unit Price"
                type="number"
                value={formData.unitPrice}
                onChange={(e) => setFormData({ ...formData, unitPrice: parseFloat(e.target.value) })}
                required
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Cost Price"
                type="number"
                value={formData.costPrice}
                onChange={(e) => setFormData({ ...formData, costPrice: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Tax Rate (%)"
                type="number"
                value={formData.taxRate}
                onChange={(e) => setFormData({ ...formData, taxRate: parseFloat(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.trackInventory}
                    onChange={(e) => setFormData({ ...formData, trackInventory: e.target.checked })}
                  />
                }
                label="Track Inventory"
              />
            </Grid>
            <Grid item xs={12} md={6}>
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
            {editingProduct ? "Update" : "Create"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Products;

