import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel,
  Grid, Alert
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon, CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { ProductReceipt, ProductReceiptLine } from '../../services/pharmaService';
import inventoryService, { Product } from '../../services/inventoryService';

const ProductReceiptPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [receipts, setReceipts] = useState<ProductReceipt[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState<ProductReceipt | null>(null);
  const [formData, setFormData] = useState<Partial<ProductReceipt>>({
    organizationId: currentOrganizationId,
    receiptDate: new Date().toISOString().split('T')[0],
    status: 'DRAFT',
    receiptLines: []
  });
  const [editingLine, setEditingLine] = useState<Partial<ProductReceiptLine> | null>(null);
  const [showLineDialog, setShowLineDialog] = useState(false);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [receiptsData, productsData] = await Promise.all([
        pharmaService.getReceipts(currentOrganizationId),
        inventoryService.getProducts(currentOrganizationId, true)
      ]);
      setReceipts(receiptsData);
      setProducts(productsData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (receipt?: ProductReceipt) => {
    if (receipt) {
      setSelectedReceipt(receipt);
      setFormData({
        ...receipt,
        receiptDate: receipt.receiptDate?.split('T')[0] || new Date().toISOString().split('T')[0],
        receiptLines: receipt.receiptLines || []
      });
    } else {
      setSelectedReceipt(null);
      setFormData({
        organizationId: currentOrganizationId,
        receiptDate: new Date().toISOString().split('T')[0],
        receiptNumber: '',
        status: 'DRAFT',
        userName: '',
        userDesignation: '',
        notes: '',
        receiptLines: [],
        totalValue: 0
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedReceipt(null);
    setFormData({
      organizationId: currentOrganizationId,
      receiptDate: new Date().toISOString().split('T')[0],
      status: 'DRAFT',
      receiptLines: []
    });
  };

  const handleAddLine = () => {
    setEditingLine({
      productId: '',
      quantity: 0,
      tpWithVat: 0,
      mrp: 0,
      packSize: 1
    });
    setShowLineDialog(true);
  };

  const handleEditLine = (line: ProductReceiptLine, index: number) => {
    setEditingLine({ ...line, index });
    setShowLineDialog(true);
  };

  const handleSaveLine = () => {
    if (!editingLine || !editingLine.productId || !editingLine.quantity || !editingLine.tpWithVat) {
      alert('Please fill in all required fields');
      return;
    }

    const product = products.find(p => p.id === editingLine.productId);
    const newLine: ProductReceiptLine = {
      ...editingLine,
      productId: editingLine.productId,
      productName: product?.name || '',
      quantity: editingLine.quantity,
      tpWithVat: editingLine.tpWithVat,
      mrp: editingLine.mrp || product?.retailPrice || 0,
      packSize: editingLine.packSize || 1,
      amount: (editingLine.quantity || 0) * (editingLine.tpWithVat || 0)
    };

    const currentLines = formData.receiptLines || [];
    if (editingLine.index !== undefined && editingLine.index >= 0) {
      // Update existing line
      const updatedLines = [...currentLines];
      updatedLines[editingLine.index] = newLine;
      setFormData({ ...formData, receiptLines: updatedLines });
    } else {
      // Add new line
      setFormData({ ...formData, receiptLines: [...currentLines, newLine] });
    }

    // Recalculate total
    const totalValue = [...(formData.receiptLines || []), newLine]
      .filter((_, idx) => editingLine.index === undefined || idx !== editingLine.index)
      .reduce((sum, line) => sum + (line.amount || 0), 0);
    setFormData(prev => ({ ...prev, totalValue }));

    setEditingLine(null);
    setShowLineDialog(false);
  };

  const handleDeleteLine = (index: number) => {
    const updatedLines = formData.receiptLines?.filter((_, idx) => idx !== index) || [];
    const totalValue = updatedLines.reduce((sum, line) => sum + (line.amount || 0), 0);
    setFormData({ ...formData, receiptLines: updatedLines, totalValue });
  };

  const handleSave = async () => {
    if (!formData.receiptDate) {
      alert('Please select receipt date');
      return;
    }

    if (!formData.receiptLines || formData.receiptLines.length === 0) {
      alert('Please add at least one product line');
      return;
    }

    // Recalculate total before saving
    const totalValue = formData.receiptLines.reduce((sum, line) => sum + (line.amount || 0), 0);
    const dataToSave = { ...formData, totalValue };

    try {
      if (selectedReceipt?.id) {
        await pharmaService.updateReceipt(selectedReceipt.id, dataToSave as ProductReceipt);
      } else {
        await pharmaService.createReceipt(dataToSave as ProductReceipt);
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save receipt:', error);
      alert(error.response?.data?.message || 'Failed to save receipt');
    }
  };

  const handleSubmit = async (id: string) => {
    if (!window.confirm('Are you sure you want to submit this receipt? This will update inventory stock.')) return;
    try {
      await pharmaService.submitReceipt(id);
      await loadData();
      alert('Receipt submitted successfully!');
    } catch (error: any) {
      console.error('Failed to submit receipt:', error);
      alert(error.response?.data?.message || 'Failed to submit receipt');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this receipt?')) return;
    try {
      await pharmaService.deleteReceipt(id);
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete receipt:', error);
      alert(error.response?.data?.message || 'Failed to delete receipt');
    }
  };

  const getProductName = (productId: string): string => {
    const product = products.find(p => p.id === productId);
    return product ? product.name : '-';
  };

  const filteredReceipts = receipts.filter(receipt => {
    if (!currentOrganizationId) return false;
    return receipt.organizationId === currentOrganizationId;
  });

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Product Receipt from Factory</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Receipt
        </Button>
      </Box>

      <Card>
        <CardContent>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Receipt Number</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>User</TableCell>
                    <TableCell>Designation</TableCell>
                    <TableCell>Total Value</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredReceipts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No receipts found
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredReceipts.map((receipt) => (
                      <TableRow key={receipt.id}>
                        <TableCell>{receipt.receiptNumber || '-'}</TableCell>
                        <TableCell>{receipt.receiptDate?.split('T')[0] || '-'}</TableCell>
                        <TableCell>{receipt.userName || '-'}</TableCell>
                        <TableCell>{receipt.userDesignation || '-'}</TableCell>
                        <TableCell>{receipt.totalValue?.toFixed(2) || '0.00'}</TableCell>
                        <TableCell>
                          <Chip
                            label={receipt.status}
                            size="small"
                            color={receipt.status === 'SUBMITTED' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(receipt)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          {receipt.status === 'DRAFT' && (
                            <IconButton
                              size="small"
                              onClick={() => receipt.id && handleSubmit(receipt.id)}
                              color="success"
                            >
                              <CheckCircleIcon fontSize="small" />
                            </IconButton>
                          )}
                          {receipt.status === 'DRAFT' && (
                            <IconButton
                              size="small"
                              onClick={() => receipt.id && handleDelete(receipt.id)}
                              color="error"
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Receipt Dialog */}
      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
        <DialogTitle>
          {selectedReceipt ? 'Edit Product Receipt' : 'New Product Receipt'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Alert severity="info">
              Product receipt from factory to central depot. This will update inventory stock when submitted.
            </Alert>

            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Receipt Date"
                  type="date"
                  value={formData.receiptDate || ''}
                  onChange={(e) => setFormData({ ...formData, receiptDate: e.target.value })}
                  fullWidth
                  required
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Receipt Number"
                  value={formData.receiptNumber || ''}
                  onChange={(e) => setFormData({ ...formData, receiptNumber: e.target.value })}
                  fullWidth
                  placeholder="Auto-generated if left empty"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="User Name"
                  value={formData.userName || ''}
                  onChange={(e) => setFormData({ ...formData, userName: e.target.value })}
                  fullWidth
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="User Designation"
                  value={formData.userDesignation || ''}
                  onChange={(e) => setFormData({ ...formData, userDesignation: e.target.value })}
                  fullWidth
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Notes"
                  value={formData.notes || ''}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  fullWidth
                  multiline
                  rows={2}
                />
              </Grid>
            </Grid>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 2 }}>
              <Typography variant="h6">Product Lines</Typography>
              <Button
                variant="outlined"
                startIcon={<AddIcon />}
                onClick={handleAddLine}
                disabled={formData.status === 'SUBMITTED'}
              >
                Add Product
              </Button>
            </Box>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Product</TableCell>
                    <TableCell>Pack Size</TableCell>
                    <TableCell>Quantity</TableCell>
                    <TableCell>TP with VAT</TableCell>
                    <TableCell>MRP</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {formData.receiptLines?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No products added
                      </TableCell>
                    </TableRow>
                  ) : (
                    formData.receiptLines?.map((line, index) => (
                      <TableRow key={index}>
                        <TableCell>{line.productName || getProductName(line.productId)}</TableCell>
                        <TableCell>{line.packSize || 1}</TableCell>
                        <TableCell>{line.quantity}</TableCell>
                        <TableCell>{line.tpWithVat?.toFixed(2) || '0.00'}</TableCell>
                        <TableCell>{line.mrp?.toFixed(2) || '0.00'}</TableCell>
                        <TableCell>{line.amount?.toFixed(2) || '0.00'}</TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleEditLine(line, index)}
                            disabled={formData.status === 'SUBMITTED'}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => handleDeleteLine(index)}
                            disabled={formData.status === 'SUBMITTED'}
                            color="error"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
              <Typography variant="h6">
                Total Value: {formData.totalValue?.toFixed(2) || '0.00'}
              </Typography>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={formData.status === 'SUBMITTED'}>
            {selectedReceipt ? 'Update' : 'Save Draft'}
          </Button>
          {!selectedReceipt && (
            <Button
              onClick={async () => {
                await handleSave();
                if (selectedReceipt?.id) {
                  await handleSubmit(selectedReceipt.id);
                }
              }}
              variant="contained"
              color="success"
            >
              Save & Submit
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Product Line Dialog */}
      <Dialog open={showLineDialog} onClose={() => setShowLineDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingLine?.index !== undefined ? 'Edit Product Line' : 'Add Product Line'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel>Product</InputLabel>
              <Select
                value={editingLine?.productId || ''}
                label="Product"
                onChange={(e) => {
                  const product = products.find(p => p.id === e.target.value);
                  setEditingLine({
                    ...editingLine,
                    productId: e.target.value,
                    productName: product?.name,
                    tpWithVat: product?.wholesalePrice || 0,
                    mrp: product?.retailPrice || 0,
                    packSize: product?.packSize || 1
                  });
                }}
              >
                {products.map((product) => (
                  <MenuItem key={product.id} value={product.id}>
                    {product.name} {product.wholesalePrice ? `(TP: ${product.wholesalePrice})` : ''}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Pack Size"
              type="number"
              value={editingLine?.packSize || 1}
              onChange={(e) => setEditingLine({ ...editingLine, packSize: Number(e.target.value) })}
              fullWidth
              inputProps={{ min: 1 }}
            />

            <TextField
              label="Quantity"
              type="number"
              value={editingLine?.quantity || 0}
              onChange={(e) => setEditingLine({ ...editingLine, quantity: Number(e.target.value) })}
              fullWidth
              required
              inputProps={{ min: 1 }}
            />

            <TextField
              label="TP with VAT"
              type="number"
              value={editingLine?.tpWithVat || 0}
              onChange={(e) => {
                const tpWithVat = Number(e.target.value);
                const quantity = editingLine?.quantity || 0;
                setEditingLine({
                  ...editingLine,
                  tpWithVat,
                  amount: quantity * tpWithVat
                });
              }}
              fullWidth
              required
              inputProps={{ step: '0.01', min: 0 }}
            />

            <TextField
              label="MRP"
              type="number"
              value={editingLine?.mrp || 0}
              onChange={(e) => setEditingLine({ ...editingLine, mrp: Number(e.target.value) })}
              fullWidth
              inputProps={{ step: '0.01', min: 0 }}
            />

            {editingLine?.quantity && editingLine?.tpWithVat && (
              <TextField
                label="Amount (Auto-calculated)"
                value={((editingLine.quantity || 0) * (editingLine.tpWithVat || 0)).toFixed(2)}
                fullWidth
                disabled
              />
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowLineDialog(false)}>Cancel</Button>
          <Button onClick={handleSaveLine} variant="contained">
            {editingLine?.index !== undefined ? 'Update' : 'Add'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ProductReceiptPage;

