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
  Paper,
  Autocomplete,
} from "@mui/material";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Visibility as ViewIcon,
} from "@mui/icons-material";
import { useAuth } from "../../contexts/AuthContext";
import salesService from "../../services/salesService";
import { Quotation, Customer, Product } from "../../types/sales";

const Quotations = () => {
  const { currentOrganizationId } = useAuth();
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [openDialog, setOpenDialog] = useState(false);
  const [viewDialog, setViewDialog] = useState(false);
  const [selectedQuotation, setSelectedQuotation] = useState<Quotation | null>(null);
  
  const [formData, setFormData] = useState({
    customerId: "",
    quotationDate: new Date().toISOString().split('T')[0],
    validUntil: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    notes: "",
    lines: [{
      lineNumber: 1,
      productId: "",
      productName: "",
      description: "",
      quantity: 1,
      unitPrice: 0,
      discountPercent: 0,
      taxPercent: 0,
    }],
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadQuotations();
      loadCustomers();
      loadProducts();
    }
  }, [currentOrganizationId]);

  const loadQuotations = async () => {
    try {
      const data = await salesService.getQuotations(currentOrganizationId!);
      setQuotations(data);
    } catch (err: any) {
      setError(err.message);
    }
  };

  const loadCustomers = async () => {
    try {
      const data = await salesService.getCustomers(currentOrganizationId!, true);
      setCustomers(data);
    } catch (err: any) {
      console.error("Failed to load customers:", err);
    }
  };

  const loadProducts = async () => {
    try {
      const data = await salesService.getProducts(currentOrganizationId!, true);
      setProducts(data);
    } catch (err: any) {
      console.error("Failed to load products:", err);
    }
  };

  const handleOpenDialog = () => {
    setFormData({
      customerId: "",
      quotationDate: new Date().toISOString().split('T')[0],
      validUntil: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      notes: "",
      lines: [{
        lineNumber: 1,
        productId: "",
        productName: "",
        description: "",
        quantity: 1,
        unitPrice: 0,
        discountPercent: 0,
        taxPercent: 0,
      }],
    });
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
  };

  const handleAddLine = () => {
    setFormData({
      ...formData,
      lines: [
        ...formData.lines,
        {
          lineNumber: formData.lines.length + 1,
          productId: "",
          productName: "",
          description: "",
          quantity: 1,
          unitPrice: 0,
          discountPercent: 0,
          taxPercent: 0,
        },
      ],
    });
  };

  const handleRemoveLine = (index: number) => {
    const newLines = formData.lines.filter((_, i) => i !== index);
    setFormData({
      ...formData,
      lines: newLines.map((line, i) => ({ ...line, lineNumber: i + 1 })),
    });
  };

  const handleLineChange = (index: number, field: string, value: any) => {
    const newLines = [...formData.lines];
    newLines[index] = { ...newLines[index], [field]: value };
    
    // Auto-populate product details when product is selected
    // Handle both Sales Product and Inventory Product property names
    if (field === "productId" && value) {
      const product = products.find(p => p.id === value);
      if (product) {
        newLines[index].productName = (product as any).productName || (product as any).name || "";
        newLines[index].productCode = (product as any).productCode || (product as any).sku || "";
        newLines[index].unitPrice = (product as any).unitPrice || (product as any).sellingPrice || 0;
        newLines[index].unitOfMeasure = (product as any).unitOfMeasure || (product as any).uom || "";
        newLines[index].taxPercent = (product as any).taxRate || 0;
      }
    }
    
    setFormData({ ...formData, lines: newLines });
  };

  const handleSubmit = async () => {
    if (!formData.customerId) {
      setError("Please select a customer");
      return;
    }

    setLoading(true);
    try {
      const request = {
        organizationId: currentOrganizationId!,
        customerId: formData.customerId,
        quotationDate: formData.quotationDate,
        validUntil: formData.validUntil,
        notes: formData.notes,
        lines: formData.lines.map((line, index) => ({
          lineNumber: index + 1,
          productId: line.productId || undefined,
          productName: line.productName,
          description: line.description,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
          discountPercent: line.discountPercent || 0,
          taxPercent: line.taxPercent || 0,
        })),
      };

      await salesService.createQuotation(request);
      await loadQuotations();
      handleCloseDialog();
      setError("");
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSendQuotation = async (id: string) => {
    try {
      await salesService.sendQuotation(id);
      await loadQuotations();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleAcceptQuotation = async (id: string) => {
    try {
      await salesService.acceptQuotation(id);
      await loadQuotations();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleDeleteQuotation = async (id: string) => {
    if (window.confirm("Are you sure you want to delete this quotation?")) {
      try {
        await salesService.deleteQuotation(id);
        await loadQuotations();
      } catch (err: any) {
        setError(err.message);
      }
    }
  };

  const handleViewQuotation = (quotation: Quotation) => {
    setSelectedQuotation(quotation);
    setViewDialog(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "DRAFT": return "default";
      case "SENT": return "info";
      case "ACCEPTED": return "success";
      case "REJECTED": return "error";
      case "EXPIRED": return "warning";
      case "CONVERTED": return "secondary";
      default: return "default";
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Sales Quotations</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenDialog}
        >
          Create Quotation
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Quotation #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Valid Until</TableCell>
                  <TableCell align="right">Total Amount</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {quotations.map((quotation) => (
                  <TableRow key={quotation.id}>
                    <TableCell>{quotation.quotationNumber}</TableCell>
                    <TableCell>{new Date(quotation.quotationDate).toLocaleDateString()}</TableCell>
                    <TableCell>{quotation.customerName}</TableCell>
                    <TableCell>{new Date(quotation.validUntil).toLocaleDateString()}</TableCell>
                    <TableCell align="right">{formatCurrency(quotation.totalAmount)}</TableCell>
                    <TableCell>
                      <Chip label={quotation.status} color={getStatusColor(quotation.status) as any} size="small" />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => handleViewQuotation(quotation)}>
                        <ViewIcon fontSize="small" />
                      </IconButton>
                      {quotation.status === "DRAFT" && (
                        <>
                          <IconButton size="small" onClick={() => handleSendQuotation(quotation.id)}>
                            <SendIcon fontSize="small" />
                          </IconButton>
                          <IconButton size="small" onClick={() => handleDeleteQuotation(quotation.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </>
                      )}
                      {quotation.status === "SENT" && (
                        <IconButton size="small" color="success" onClick={() => handleAcceptQuotation(quotation.id)}>
                          <CheckIcon fontSize="small" />
                        </IconButton>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
        <DialogTitle>Create Quotation</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                select
                fullWidth
                label="Customer"
                value={formData.customerId}
                onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
              >
                {customers.map((customer) => (
                  <MenuItem key={customer.id} value={customer.id}>
                    {customer.customerName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Quotation Date"
                type="date"
                value={formData.quotationDate}
                onChange={(e) => setFormData({ ...formData, quotationDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Valid Until"
                type="date"
                value={formData.validUntil}
                onChange={(e) => setFormData({ ...formData, validUntil: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>Line Items</Typography>
              {formData.lines.map((line, index) => (
                <Paper key={index} sx={{ p: 2, mb: 2 }}>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={4}>
                      <Autocomplete
                        fullWidth
                        options={products}
                        getOptionLabel={(option) => option.productName || option.name || ''}
                        value={products.find(p => p.id === line.productId) || null}
                        onChange={(event, newValue) => {
                          // handleLineChange will auto-populate product details
                          handleLineChange(index, "productId", newValue?.id || "");
                        }}
                        renderInput={(params) => (
                          <TextField {...params} label="Product" placeholder="Search products..." />
                        )}
                        renderOption={(props, option) => (
                          <li {...props} key={option.id}>
                            <Box>
                              <Typography variant="body2">{option.productName || option.name}</Typography>
                              <Typography variant="caption" color="text.secondary">
                                {option.productCode || option.sku} - ${option.unitPrice || option.sellingPrice || 0}
                              </Typography>
                            </Box>
                          </li>
                        )}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        fullWidth
                        label="Quantity"
                        type="number"
                        value={line.quantity}
                        onChange={(e) => handleLineChange(index, "quantity", parseFloat(e.target.value))}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        fullWidth
                        label="Unit Price"
                        type="number"
                        value={line.unitPrice}
                        onChange={(e) => handleLineChange(index, "unitPrice", parseFloat(e.target.value))}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        fullWidth
                        label="Tax %"
                        type="number"
                        value={line.taxPercent}
                        onChange={(e) => handleLineChange(index, "taxPercent", parseFloat(e.target.value))}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <IconButton color="error" onClick={() => handleRemoveLine(index)}>
                        <DeleteIcon />
                      </IconButton>
                    </Grid>
                  </Grid>
                </Paper>
              ))}
              <Button onClick={handleAddLine} startIcon={<AddIcon />}>
                Add Line
              </Button>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Notes"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={loading}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Dialog */}
      <Dialog open={viewDialog} onClose={() => setViewDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Quotation Details</DialogTitle>
        <DialogContent>
          {selectedQuotation && (
            <Box>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Quotation #:</Typography>
                  <Typography>{selectedQuotation.quotationNumber}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Status:</Typography>
                  <Chip label={selectedQuotation.status} color={getStatusColor(selectedQuotation.status) as any} />
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Customer:</Typography>
                  <Typography>{selectedQuotation.customerName}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Date:</Typography>
                  <Typography>{new Date(selectedQuotation.quotationDate).toLocaleDateString()}</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="h6" sx={{ mt: 2 }}>Line Items</Typography>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Product</TableCell>
                        <TableCell align="right">Qty</TableCell>
                        <TableCell align="right">Price</TableCell>
                        <TableCell align="right">Total</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {selectedQuotation.lines.map((line) => (
                        <TableRow key={line.id}>
                          <TableCell>{line.productName}</TableCell>
                          <TableCell align="right">{line.quantity}</TableCell>
                          <TableCell align="right">{formatCurrency(line.unitPrice)}</TableCell>
                          <TableCell align="right">{formatCurrency(line.lineTotal)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Grid>
                <Grid item xs={12}>
                  <Box sx={{ textAlign: "right", mt: 2 }}>
                    <Typography variant="h6">
                      Total: {formatCurrency(selectedQuotation.totalAmount)}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Quotations;

