import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel,
  Grid, Alert, Tooltip, Divider
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as SendIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Adjustment, AdjustmentLine, Territory } from '../../services/pharmaService';
import inventoryService, { Product } from '../../services/inventoryService';
import { useSnackbar } from 'notistack';

const ProductAdjustment: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [adjustments, setAdjustments] = useState<Adjustment[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedAdjustment, setSelectedAdjustment] = useState<Adjustment | null>(null);
  const [formData, setFormData] = useState<Partial<Adjustment>>({
    organizationId: currentOrganizationId,
    adjustmentDate: new Date().toISOString().split('T')[0],
    adjustmentType: 'DAMAGE',
    status: 'DRAFT',
    adjustmentLines: []
  });
  
  const [showLineDialog, setShowLineDialog] = useState(false);
  const [editingLine, setEditingLine] = useState<Partial<AdjustmentLine> & { index?: number } | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [adjustmentsData, territoriesData, productsData] = await Promise.all([
        pharmaService.getAdjustments(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        inventoryService.getProducts(currentOrganizationId, true)
      ]);
      setAdjustments(adjustmentsData);
      setTerritories(territoriesData);
      setProducts(productsData);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (adjustment?: Adjustment) => {
    if (adjustment) {
      setSelectedAdjustment(adjustment);
      setFormData({
        ...adjustment,
        adjustmentDate: adjustment.adjustmentDate?.split('T')[0] || new Date().toISOString().split('T')[0]
      });
    } else {
      setSelectedAdjustment(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        adjustmentDate: new Date().toISOString().split('T')[0],
        adjustmentType: 'DAMAGE',
        status: 'DRAFT',
        notes: '',
        adjustmentLines: []
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedAdjustment(null);
  };

  const handleAddLine = () => {
    setEditingLine({
      productId: '',
      quantity: 0,
      reason: ''
    });
    setShowLineDialog(true);
  };

  const handleEditLine = (line: AdjustmentLine, index: number) => {
    setEditingLine({ ...line, index });
    setShowLineDialog(true);
  };

  const handleSaveLine = () => {
    if (!editingLine || !editingLine.productId || !editingLine.quantity) {
      enqueueSnackbar('Please fill in required fields', { variant: 'warning' });
      return;
    }

    const product = products.find(p => p.id === editingLine.productId);
    const newLine: AdjustmentLine = {
      ...editingLine,
      productName: product?.name || '',
      tpWithVat: product?.wholesalePrice || 0,
      amount: (editingLine.quantity || 0) * (product?.wholesalePrice || 0)
    } as AdjustmentLine;

    const currentLines = [...(formData.adjustmentLines || [])];
    if (editingLine.index !== undefined) {
      currentLines[editingLine.index] = newLine;
    } else {
      currentLines.push(newLine);
    }

    setFormData({ ...formData, adjustmentLines: currentLines });
    setShowLineDialog(false);
    setEditingLine(null);
  };

  const handleDeleteLine = (index: number) => {
    const currentLines = [...(formData.adjustmentLines || [])];
    currentLines.splice(index, 1);
    setFormData({ ...formData, adjustmentLines: currentLines });
  };

  const handleSave = async () => {
    if (!formData.territoryId || !formData.adjustmentLines?.length) {
      enqueueSnackbar('Please select a territory and add at least one product line', { variant: 'warning' });
      return;
    }

    try {
      await pharmaService.createAdjustment(formData as Adjustment);
      enqueueSnackbar('Adjustment saved successfully', { variant: 'success' });
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save adjustment:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to save adjustment', { variant: 'error' });
    }
  };

  const handleSubmit = async (id: string) => {
    if (!window.confirm('Are you sure you want to submit this adjustment? This will update territory stock.')) return;
    try {
      await pharmaService.submitAdjustment(id);
      enqueueSnackbar('Adjustment submitted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to submit adjustment:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to submit adjustment', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this draft adjustment?')) return;
    try {
      await pharmaService.deleteAdjustment(id);
      enqueueSnackbar('Adjustment deleted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete adjustment:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to delete adjustment', { variant: 'error' });
    }
  };

  const getTerritoryName = (territoryId: string): string => {
    const territory = territories.find(t => t.id === territoryId);
    return territory ? territory.name : '-';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Damage & Expiry Adjustments</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Adjustment
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Use this form to record stock losses in territories due to damage or expiry. These adjustments will reduce territory inventory.
          </Alert>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Territory</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell align="right">Total Value</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {adjustments.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        No adjustments found
                      </TableCell>
                    </TableRow>
                  ) : (
                    adjustments.map((a) => (
                      <TableRow key={a.id}>
                        <TableCell>{a.adjustmentDate?.split('T')[0]}</TableCell>
                        <TableCell>{getTerritoryName(a.territoryId)}</TableCell>
                        <TableCell>
                          <Chip label={a.adjustmentType} size="small" variant="outlined" color={a.adjustmentType === 'DAMAGE' ? 'error' : 'warning'} />
                        </TableCell>
                        <TableCell align="right">{a.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                        <TableCell>
                          <Chip
                            label={a.status}
                            size="small"
                            color={a.status === 'SUBMITTED' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="View">
                            <IconButton size="small" onClick={() => handleOpenDialog(a)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {a.status === 'DRAFT' && (
                            <Tooltip title="Submit">
                              <IconButton size="small" color="success" onClick={() => a.id && handleSubmit(a.id)}>
                                <SendIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {a.status === 'DRAFT' && (
                            <Tooltip title="Delete">
                              <IconButton size="small" color="error" onClick={() => a.id && handleDelete(a.id)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
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

      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedAdjustment ? 'View/Edit Adjustment' : 'New Stock Adjustment'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Territory"
                value={formData.territoryId || ''}
                onChange={(territoryId) => setFormData({ ...formData, territoryId })}
                required
                disabled={!!selectedAdjustment}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth required>
                <InputLabel>Adjustment Type</InputLabel>
                <Select
                  value={formData.adjustmentType || 'DAMAGE'}
                  label="Adjustment Type"
                  onChange={(e) => setFormData({ ...formData, adjustmentType: e.target.value as any })}
                  disabled={!!selectedAdjustment}
                >
                  <MenuItem value="DAMAGE">Damage</MenuItem>
                  <MenuItem value="EXPIRY">Expiry</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Adjustment Date"
                type="date"
                value={formData.adjustmentDate || ''}
                onChange={(e) => setFormData({ ...formData, adjustmentDate: e.target.value })}
                required
                InputLabelProps={{ shrink: true }}
                disabled={!!selectedAdjustment}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notes"
                multiline
                rows={2}
                value={formData.notes || ''}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                disabled={!!selectedAdjustment && formData.status !== 'DRAFT'}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Product Lines</Typography>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={handleAddLine}
                  disabled={!!selectedAdjustment && formData.status !== 'DRAFT'}
                >
                  Add Product
                </Button>
              </Box>
              
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell align="right">Quantity</TableCell>
                      <TableCell align="right">TP with VAT</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Reason</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {!formData.adjustmentLines?.length ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center">No lines added</TableCell>
                      </TableRow>
                    ) : (
                      formData.adjustmentLines.map((line, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{line.productName || products.find(p => p.id === line.productId)?.name}</TableCell>
                          <TableCell align="right">{line.quantity}</TableCell>
                          <TableCell align="right">{line.tpWithVat?.toFixed(2)}</TableCell>
                          <TableCell align="right">{line.amount?.toFixed(2)}</TableCell>
                          <TableCell>{line.reason || '-'}</TableCell>
                          <TableCell align="center">
                            <IconButton size="small" onClick={() => handleEditLine(line, idx)} disabled={formData.status !== 'DRAFT'}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                            <IconButton size="small" color="error" onClick={() => handleDeleteLine(idx)} disabled={formData.status !== 'DRAFT'}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Close</Button>
          {!selectedAdjustment && (
            <Button onClick={handleSave} variant="contained">Save Draft</Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Line Item Dialog */}
      <Dialog open={showLineDialog} onClose={() => setShowLineDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editingLine?.index !== undefined ? 'Edit Product Line' : 'Add Product Line'}</DialogTitle>
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
                    tpWithVat: product?.wholesalePrice || 0
                  });
                }}
              >
                {products.map((p) => (
                  <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth
              label="Quantity"
              type="number"
              value={editingLine?.quantity || 0}
              onChange={(e) => setEditingLine({ ...editingLine, quantity: Number(e.target.value) })}
              required
            />
            <TextField
              fullWidth
              label="Reason (Optional)"
              value={editingLine?.reason || ''}
              onChange={(e) => setEditingLine({ ...editingLine, reason: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowLineDialog(false)}>Cancel</Button>
          <Button onClick={handleSaveLine} variant="contained">Confirm</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ProductAdjustment;
