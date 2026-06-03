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
  Menu,
  Autocomplete,
} from "@mui/material";
import {
  Add as AddIcon,
  MoreVert as MoreIcon,
  Visibility as ViewIcon,
  Receipt as InvoiceIcon,
} from "@mui/icons-material";
import { useAuth } from "../../contexts/AuthContext";
import salesService from "../../services/salesService";
import { SalesOrder, Customer, Product } from "../../types/sales";
import { useNavigate } from "react-router-dom";

const SalesOrders = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");
  const [openDialog, setOpenDialog] = useState(false);
  const [viewDialog, setViewDialog] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<SalesOrder | null>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [menuOrderId, setMenuOrderId] = useState<string | null>(null);
  
  const [formData, setFormData] = useState({
    customerId: "",
    orderDate: new Date().toISOString().split('T')[0],
    expectedDeliveryDate: "",
    priority: "NORMAL",
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
      loadOrders();
      loadCustomers();
      loadProducts();
    }
  }, [currentOrganizationId]);

  const loadOrders = async () => {
    try {
      const data = await salesService.getSalesOrders(currentOrganizationId!);
      setOrders(data);
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
      orderDate: new Date().toISOString().split('T')[0],
      expectedDeliveryDate: "",
      priority: "NORMAL",
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
        orderDate: formData.orderDate,
        expectedDeliveryDate: formData.expectedDeliveryDate || undefined,
        priority: formData.priority,
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

      await salesService.createSalesOrder(request);
      await loadOrders();
      setOpenDialog(false);
      setSuccess("Sales order created successfully");
      setError("");
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, orderId: string) => {
    setAnchorEl(event.currentTarget);
    setMenuOrderId(orderId);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setMenuOrderId(null);
  };

  const handleConfirmOrder = async () => {
    if (!menuOrderId) return;
    try {
      await salesService.confirmOrder(menuOrderId);
      await loadOrders();
      setSuccess("Order confirmed successfully");
      handleMenuClose();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleStartProcessing = async () => {
    if (!menuOrderId) return;
    try {
      await salesService.startProcessing(menuOrderId);
      await loadOrders();
      setSuccess("Order processing started");
      handleMenuClose();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleCompleteOrder = async () => {
    if (!menuOrderId) return;
    try {
      await salesService.completeOrder(menuOrderId);
      await loadOrders();
      setSuccess("Order completed");
      handleMenuClose();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleConvertToInvoice = async () => {
    if (!menuOrderId) return;
    try {
      const result = await salesService.convertToInvoice(menuOrderId);
      await loadOrders();
      setSuccess(`Invoice created: ${result.invoiceId}`);
      handleMenuClose();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleCancelOrder = async () => {
    if (!menuOrderId) return;
    if (window.confirm("Are you sure you want to cancel this order?")) {
      try {
        await salesService.cancelOrder(menuOrderId);
        await loadOrders();
        setSuccess("Order cancelled");
        handleMenuClose();
      } catch (err: any) {
        setError(err.message);
      }
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "DRAFT": return "default";
      case "CONFIRMED": return "primary";
      case "IN_PROGRESS": return "info";
      case "COMPLETED": return "success";
      case "INVOICED": return "secondary";
      case "CANCELLED": return "error";
      default: return "default";
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  };

  const selectedOrderForMenu = orders.find(o => o.id === menuOrderId);

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Sales Orders</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenDialog}
        >
          Create Order
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
                  <TableCell>Order #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell align="right">Total Amount</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell>{order.orderNumber}</TableCell>
                    <TableCell>{new Date(order.orderDate).toLocaleDateString()}</TableCell>
                    <TableCell>{order.customerName}</TableCell>
                    <TableCell>
                      <Chip label={order.priority} size="small" />
                    </TableCell>
                    <TableCell align="right">{formatCurrency(order.totalAmount)}</TableCell>
                    <TableCell>
                      <Chip label={order.status} color={getStatusColor(order.status) as any} size="small" />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton size="small" onClick={() => { setSelectedOrder(order); setViewDialog(true); }}>
                        <ViewIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" onClick={(e) => handleMenuOpen(e, order.id)}>
                        <MoreIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Actions Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        {selectedOrderForMenu?.status === "DRAFT" && (
          <MenuItem onClick={handleConfirmOrder}>Confirm Order</MenuItem>
        )}
        {selectedOrderForMenu?.status === "CONFIRMED" && (
          <MenuItem onClick={handleStartProcessing}>Start Processing</MenuItem>
        )}
        {selectedOrderForMenu?.status === "IN_PROGRESS" && (
          <MenuItem onClick={handleCompleteOrder}>Complete Order</MenuItem>
        )}
        {(selectedOrderForMenu?.status === "COMPLETED" || selectedOrderForMenu?.status === "CONFIRMED") && !selectedOrderForMenu?.convertedToInvoiceId && (
          <MenuItem onClick={handleConvertToInvoice}>Convert to Invoice</MenuItem>
        )}
        {selectedOrderForMenu?.status !== "INVOICED" && selectedOrderForMenu?.status !== "CANCELLED" && (
          <MenuItem onClick={handleCancelOrder}>Cancel Order</MenuItem>
        )}
      </Menu>

      {/* Create Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="lg" fullWidth>
        <DialogTitle>Create Sales Order</DialogTitle>
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
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                label="Order Date"
                type="date"
                value={formData.orderDate}
                onChange={(e) => setFormData({ ...formData, orderDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                label="Expected Delivery"
                type="date"
                value={formData.expectedDeliveryDate}
                onChange={(e) => setFormData({ ...formData, expectedDeliveryDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                select
                fullWidth
                label="Priority"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="NORMAL">Normal</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="URGENT">Urgent</MenuItem>
              </TextField>
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
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={loading}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Dialog */}
      <Dialog open={viewDialog} onClose={() => setViewDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Order Details</DialogTitle>
        <DialogContent>
          {selectedOrder && (
            <Box>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Order #:</Typography>
                  <Typography>{selectedOrder.orderNumber}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Status:</Typography>
                  <Chip label={selectedOrder.status} color={getStatusColor(selectedOrder.status) as any} />
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Customer:</Typography>
                  <Typography>{selectedOrder.customerName}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Date:</Typography>
                  <Typography>{new Date(selectedOrder.orderDate).toLocaleDateString()}</Typography>
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
                      {selectedOrder.lines.map((line) => (
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
                      Total: {formatCurrency(selectedOrder.totalAmount)}
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

export default SalesOrders;

