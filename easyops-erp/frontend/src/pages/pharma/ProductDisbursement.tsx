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
  CheckCircle as CheckCircleIcon,
  Send as SendIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { ProductDisbursement, ProductDisbursementLine, Territory } from '../../services/pharmaService';
import inventoryService, { Product } from '../../services/inventoryService';
import { getEmployees, Employee } from '../../services/hrService';
import { useSnackbar } from 'notistack';

const ProductDisbursementPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [disbursements, setDisbursements] = useState<ProductDisbursement[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedDisbursement, setSelectedDisbursement] = useState<ProductDisbursement | null>(null);
  const [formData, setFormData] = useState<Partial<ProductDisbursement>>({
    organizationId: currentOrganizationId,
    disbursementDate: new Date().toISOString().split('T')[0],
    status: 'DRAFT',
    disbursementLines: []
  });
  
  const [showLineDialog, setShowLineDialog] = useState(false);
  const [editingLine, setEditingLine] = useState<Partial<ProductDisbursementLine> & { index?: number } | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [disbursementsData, territoriesData, employeesRes, productsData] = await Promise.all([
        pharmaService.getDisbursements(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
        inventoryService.getProducts(currentOrganizationId, true)
      ]);
      setDisbursements(disbursementsData);
      setTerritories(territoriesData);
      setEmployees(employeesRes.data || []);
      setProducts(productsData);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (disbursement?: ProductDisbursement) => {
    if (disbursement) {
      setSelectedDisbursement(disbursement);
      setFormData({
        ...disbursement,
        disbursementDate: disbursement.disbursementDate?.split('T')[0] || new Date().toISOString().split('T')[0]
      });
    } else {
      setSelectedDisbursement(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        employeeId: '',
        disbursementDate: new Date().toISOString().split('T')[0],
        status: 'DRAFT',
        notes: '',
        disbursementLines: []
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedDisbursement(null);
  };

  const handleAddLine = () => {
    setEditingLine({
      productId: '',
      currentMonthQuantity: 0,
      previousMonthOpeningQuantity: 0,
      tpWithVat: 0,
      mrp: 0
    });
    setShowLineDialog(true);
  };

  const handleEditLine = (line: ProductDisbursementLine, index: number) => {
    setEditingLine({ ...line, index });
    setShowLineDialog(true);
  };

  const handleSaveLine = () => {
    if (!editingLine || !editingLine.productId || !editingLine.currentMonthQuantity) {
      enqueueSnackbar('Please fill in required fields', { variant: 'warning' });
      return;
    }

    const product = products.find(p => p.id === editingLine.productId);
    const newLine: ProductDisbursementLine = {
      ...editingLine,
      productName: product?.name || '',
      tpWithVat: editingLine.tpWithVat || product?.wholesalePrice || 0,
      mrp: editingLine.mrp || product?.retailPrice || 0,
      productAmount: (editingLine.currentMonthQuantity || 0) * (editingLine.tpWithVat || product?.wholesalePrice || 0),
      totalQuantity: (editingLine.currentMonthQuantity || 0) + (editingLine.previousMonthOpeningQuantity || 0)
    } as ProductDisbursementLine;

    const currentLines = [...(formData.disbursementLines || [])];
    if (editingLine.index !== undefined) {
      currentLines[editingLine.index] = newLine;
    } else {
      currentLines.push(newLine);
    }

    setFormData({ ...formData, disbursementLines: currentLines });
    setShowLineDialog(false);
    setEditingLine(null);
  };

  const handleDeleteLine = (index: number) => {
    const currentLines = [...(formData.disbursementLines || [])];
    currentLines.splice(index, 1);
    setFormData({ ...formData, disbursementLines: currentLines });
  };

  const handleSave = async () => {
    if (!formData.territoryId || !formData.employeeId || !formData.disbursementLines?.length) {
      enqueueSnackbar('Please fill in required fields and add product lines', { variant: 'warning' });
      return;
    }

    try {
      if (selectedDisbursement?.id) {
        await pharmaService.updateDisbursement(selectedDisbursement.id, formData as ProductDisbursement);
        enqueueSnackbar('Disbursement updated successfully', { variant: 'success' });
      } else {
        await pharmaService.createDisbursement(formData as ProductDisbursement);
        enqueueSnackbar('Disbursement created successfully', { variant: 'success' });
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save disbursement:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to save disbursement', { variant: 'error' });
    }
  };

  const handleSubmit = async (id: string) => {
    if (!window.confirm('Are you sure you want to submit this disbursement? This will allocate stock to the territory.')) return;
    try {
      await pharmaService.submitDisbursement(id);
      enqueueSnackbar('Disbursement submitted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to submit disbursement:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to submit disbursement', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this disbursement?')) return;
    try {
      await pharmaService.deleteDisbursement(id);
      enqueueSnackbar('Disbursement deleted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete disbursement:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to delete disbursement', { variant: 'error' });
    }
  };

  const getTerritoryName = (territoryId: string): string => {
    const territory = territories.find(t => t.id === territoryId);
    return territory ? territory.name : '-';
  };

  const getEmployeeName = (employeeId: string): string => {
    const employee = employees.find(e => (e.id === employeeId) || (e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || '-') : '-';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Product Disbursement (Allocation)</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Disbursement
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Record product allocation from central depot to specific territories. Each territory is linked to an inventory warehouse.
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
                    <TableCell>Receiving Employee</TableCell>
                    <TableCell>Total Supply Amount</TableCell>
                    <TableCell>Total Balance Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {disbursements.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No disbursements found
                      </TableCell>
                    </TableRow>
                  ) : (
                    disbursements.map((d) => (
                      <TableRow key={d.id}>
                        <TableCell>{d.disbursementDate?.split('T')[0]}</TableCell>
                        <TableCell>{getTerritoryName(d.territoryId)}</TableCell>
                        <TableCell>{getEmployeeName(d.employeeId)}</TableCell>
                        <TableCell>{d.totalSupplyAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                        <TableCell>{d.totalBalanceAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                        <TableCell>
                          <Chip
                            label={d.status}
                            size="small"
                            color={d.status === 'SUBMITTED' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => handleOpenDialog(d)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {d.status === 'DRAFT' && (
                            <Tooltip title="Submit">
                              <IconButton size="small" color="success" onClick={() => d.id && handleSubmit(d.id)}>
                                <SendIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {d.status === 'DRAFT' && (
                            <Tooltip title="Delete">
                              <IconButton size="small" color="error" onClick={() => d.id && handleDelete(d.id)}>
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

      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
        <DialogTitle>
          {selectedDisbursement ? 'Edit Disbursement' : 'New Product Disbursement'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Target Territory"
                value={formData.territoryId || ''}
                onChange={(territoryId) => setFormData({ ...formData, territoryId })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth required>
                <InputLabel>Receiving Employee</InputLabel>
                <Select
                  value={formData.employeeId || ''}
                  label="Receiving Employee"
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                >
                  {employees.map((emp) => {
                    const employeeIdValue = emp.employeeId || emp.id || '';
                    return (
                      <MenuItem key={employeeIdValue} value={employeeIdValue}>
                        {emp.name || emp.employeeNumber}
                        {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Disbursement Date"
                type="date"
                value={formData.disbursementDate || ''}
                onChange={(e) => setFormData({ ...formData, disbursementDate: e.target.value })}
                required
                InputLabelProps={{ shrink: true }}
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
              />
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Product Allocations</Typography>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={handleAddLine}
                  disabled={formData.status !== 'DRAFT'}
                >
                  Add Product
                </Button>
              </Box>
              
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell align="right">Prev. Month Opening</TableCell>
                      <TableCell align="right">Current Supply</TableCell>
                      <TableCell align="right">Total Quantity</TableCell>
                      <TableCell align="right">TP with VAT</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {!formData.disbursementLines?.length ? (
                      <TableRow>
                        <TableCell colSpan={7} align="center">No products added</TableCell>
                      </TableRow>
                    ) : (
                      formData.disbursementLines.map((line, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{line.productName || products.find(p => p.id === line.productId)?.name}</TableCell>
                          <TableCell align="right">{line.previousMonthOpeningQuantity || 0}</TableCell>
                          <TableCell align="right">{line.currentMonthQuantity}</TableCell>
                          <TableCell align="right">{line.totalQuantity}</TableCell>
                          <TableCell align="right">{line.tpWithVat?.toFixed(2)}</TableCell>
                          <TableCell align="right">{line.productAmount?.toFixed(2)}</TableCell>
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
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={formData.status !== 'DRAFT'}>
            {selectedDisbursement ? 'Update' : 'Save Draft'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Line Item Dialog */}
      <Dialog open={showLineDialog} onClose={() => setShowLineDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editingLine?.index !== undefined ? 'Edit Product' : 'Add Product'}</DialogTitle>
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
                    tpWithVat: product?.wholesalePrice || 0,
                    mrp: product?.retailPrice || 0
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
              label="Previous Month Opening Qty"
              type="number"
              value={editingLine?.previousMonthOpeningQuantity || 0}
              onChange={(e) => setEditingLine({ ...editingLine, previousMonthOpeningQuantity: Number(e.target.value) })}
            />
            <TextField
              fullWidth
              label="Current Month Supply Qty"
              type="number"
              value={editingLine?.currentMonthQuantity || 0}
              onChange={(e) => setEditingLine({ ...editingLine, currentMonthQuantity: Number(e.target.value) })}
              required
            />
            <TextField
              fullWidth
              label="TP with VAT"
              type="number"
              value={editingLine?.tpWithVat || 0}
              onChange={(e) => setEditingLine({ ...editingLine, tpWithVat: Number(e.target.value) })}
              InputProps={{ inputProps: { step: 0.01 } }}
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

export default ProductDisbursementPage;
