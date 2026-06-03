import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel,
  Grid, Alert, Tooltip, Divider
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { SoldProductEntry, SoldProductEntryLine, Territory } from '../../services/pharmaService';
import inventoryService, { Product } from '../../services/inventoryService';
import { getEmployees, Employee } from '../../services/hrService';
import { useSnackbar } from 'notistack';

const SoldProductEntryPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [entries, setEntries] = useState<SoldProductEntry[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState<SoldProductEntry | null>(null);
  const [formData, setFormData] = useState<Partial<SoldProductEntry>>({
    organizationId: currentOrganizationId,
    entryDate: new Date().toISOString().split('T')[0],
    status: 'DRAFT',
    lines: []
  });
  const [showLineDialog, setShowLineDialog] = useState(false);
  const [editingLine, setEditingLine] = useState<Partial<SoldProductEntryLine> & { index?: number } | null>(null);

  useEffect(() => {
    if (currentOrganizationId) loadData();
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [entriesData, territoriesData, employeesRes, productsData] = await Promise.all([
        pharmaService.getSoldProductEntries(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
        inventoryService.getProducts(currentOrganizationId, true)
      ]);
      const toArray = <T,>(v: unknown): T[] => {
        if (Array.isArray(v)) return v as T[];
        if (typeof v === 'string') {
          try {
            const p = JSON.parse(v) as unknown;
            return Array.isArray(p) ? (p as T[]) : [];
          } catch {
            return [];
          }
        }
        return [];
      };
      const processedEntries = toArray<SoldProductEntry>(entriesData);
      setEntries(processedEntries);
      setTerritories(Array.isArray(territoriesData) ? territoriesData : []);
      setEmployees(Array.isArray(employeesRes?.data) ? employeesRes.data : []);
      setProducts(Array.isArray(productsData) ? productsData : []);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
      setEntries([]);
      setAreas([]);
      setEmployees([]);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = async (entry?: SoldProductEntry) => {
    if (entry?.id) {
      try {
        const full = await pharmaService.getSoldProductEntryById(entry.id);
        setSelectedEntry(full);
        setFormData({
          ...full,
          entryDate: full.entryDate?.split('T')[0] || new Date().toISOString().split('T')[0],
          lines: full.lines || []
        });
      } catch (e) {
        enqueueSnackbar('Failed to load entry', { variant: 'error' });
        return;
      }
    } else {
      setSelectedEntry(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        employeeId: '',
        entryDate: new Date().toISOString().split('T')[0],
        status: 'DRAFT',
        notes: '',
        lines: []
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedEntry(null);
  };

  const handleAddLine = () => {
    setEditingLine({ productId: '', quantitySold: 0, tpWithVat: 0 });
    setShowLineDialog(true);
  };

  const handleEditLine = (line: SoldProductEntryLine, index: number) => {
    setEditingLine({ ...line, index });
    setShowLineDialog(true);
  };

  const handleSaveLine = () => {
    if (!editingLine || !editingLine.productId || !editingLine.quantitySold) {
      enqueueSnackbar('Please fill in required fields', { variant: 'warning' });
      return;
    }
    const product = safeProducts.find(p => p.id === editingLine.productId);
    const tpWithVat = editingLine.tpWithVat ?? product?.wholesalePrice ?? 0;
    const newLine: SoldProductEntryLine = {
      ...editingLine,
      productName: product?.name ?? '',
      tpWithVat,
      productAmount: (editingLine.quantitySold ?? 0) * tpWithVat
    } as SoldProductEntryLine;
    const currentLines = [...(formData.lines || [])];
    if (editingLine.index !== undefined) {
      currentLines[editingLine.index] = newLine;
    } else {
      currentLines.push(newLine);
    }
    const totalAmount = currentLines.reduce((sum, l) => sum + (l.productAmount ?? 0), 0);
    setFormData({ ...formData, lines: currentLines, totalProductAmount: totalAmount });
    setShowLineDialog(false);
    setEditingLine(null);
  };

  const handleDeleteLine = (index: number) => {
    const currentLines = [...(formData.lines || [])];
    currentLines.splice(index, 1);
    const totalAmount = currentLines.reduce((sum, l) => sum + (l.productAmount ?? 0), 0);
    setFormData({ ...formData, lines: currentLines, totalProductAmount: totalAmount });
  };

  const handleSave = async () => {
    if (!formData.territoryId || !formData.lines?.length) {
      enqueueSnackbar('Please select territory and add at least one product sale', { variant: 'warning' });
      return;
    }
    try {
      const payload: SoldProductEntry = {
        ...formData,
        organizationId: currentOrganizationId!,
        territoryId: formData.territoryId!,
        entryDate: formData.entryDate!,
        status: formData.status ?? 'DRAFT',
        lines: formData.lines
      } as SoldProductEntry;
      let result;
      if (selectedEntry?.id) {
        result = await pharmaService.updateSoldProductEntry(selectedEntry.id, payload);
        enqueueSnackbar('Sold product entry updated', { variant: 'success' });
        handleCloseDialog();
        await loadData();
      } else {
        result = await pharmaService.createSoldProductEntry(payload);
        enqueueSnackbar('Sold product entry created', { variant: 'success' });
        handleCloseDialog();
        if (result && result.id) {
          setEntries(prev => {
            const exists = prev.some(e => e.id === result.id);
            if (exists) return prev.map(e => (e.id === result.id ? result : e));
            return [result, ...prev];
          });
        }
        setTimeout(() => loadData(), 500);
      }
    } catch (error: any) {
      console.error('Failed to save:', error);
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to save', { variant: 'error' });
    }
  };

  const handleSubmit = async (id: string) => {
    if (!window.confirm('Submit this sold product entry?')) return;
    try {
      await pharmaService.submitSoldProductEntry(id);
      enqueueSnackbar('Entry submitted', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to submit', { variant: 'error' });
    }
  };

  const handleComplete = async (id: string) => {
    if (!window.confirm('Mark this entry as COMPLETED?')) return;
    try {
      await pharmaService.completeSoldProductEntry(id);
      enqueueSnackbar('Entry completed', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to complete', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this draft entry?')) return;
    try {
      await pharmaService.deleteSoldProductEntry(id);
      enqueueSnackbar('Entry deleted', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message ?? 'Failed to delete', { variant: 'error' });
    }
  };

  const safeEntries = entries ?? [];
  const safeTerritories = territories ?? [];
  const safeEmployees = employees ?? [];
  const safeProducts = products ?? [];

  const getTerritoryName = (territoryId: string) => safeTerritories.find(t => t.id === territoryId)?.name ?? '-';
  const getEmployeeName = (empId?: string) => {
    if (!empId) return '-';
    const e = safeEmployees.find(x => x.id === empId || x.employeeId === empId);
    return e ? (e.name ?? e.employeeNumber ?? '-') : '-';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Sold Product Entry</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" onClick={loadData}>
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
            New Sold Product Entry
          </Button>
        </Box>
      </Box>

      <Card>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Record product-wise sales by territory. Use this for sales tracking, outstanding quantity, and target coverage.
          </Alert>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <>
              {safeEntries.length > 0 && (
                <Typography variant="caption" sx={{ mb: 1, display: 'block' }}>
                  Showing {safeEntries.length} entry(ies)
                </Typography>
              )}
              <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Territory</TableCell>
                    <TableCell>Employee</TableCell>
                    <TableCell align="right">Total Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!safeEntries.length ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">No sold product entries</TableCell>
                    </TableRow>
                  ) : (
                    safeEntries.map((e) => (
                      <TableRow key={e.id!}>
                        <TableCell>{e.entryDate?.split('T')[0]}</TableCell>
                        <TableCell>{getTerritoryName(e.territoryId)}</TableCell>
                        <TableCell>{getEmployeeName(e.employeeId)}</TableCell>
                        <TableCell align="right">
                          {e.totalProductAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 }) ?? '-'}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={e.status}
                            size="small"
                            color={e.status === 'COMPLETED' ? 'success' : e.status === 'SUBMITTED' ? 'info' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => handleOpenDialog(e)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {e.status === 'DRAFT' && (
                            <>
                              <Tooltip title="Submit">
                                <IconButton size="small" color="primary" onClick={() => e.id && handleSubmit(e.id)}>
                                  <SendIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton size="small" color="error" onClick={() => e.id && handleDelete(e.id)}>
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}
                          {e.status === 'SUBMITTED' && (
                            <Tooltip title="Complete">
                              <IconButton size="small" color="success" onClick={() => e.id && handleComplete(e.id)}>
                                <CheckCircleIcon fontSize="small" />
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
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="lg" fullWidth>
        <DialogTitle>{selectedEntry ? 'Edit Sold Product Entry' : 'New Sold Product Entry'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Territory"
                value={formData.territoryId ?? ''}
                onChange={(territoryId) => setFormData({ ...formData, territoryId })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Entry Date"
                type="date"
                value={formData.entryDate ?? ''}
                onChange={(e) => setFormData({ ...formData, entryDate: e.target.value })}
                required
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Employee (optional)</InputLabel>
                <Select
                  value={formData.employeeId ?? ''}
                  label="Employee (optional)"
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                >
                  <MenuItem value="">None</MenuItem>
                  {safeEmployees.map((emp) => {
                    const v = emp.employeeId ?? emp.id ?? '';
                    return (
                      <MenuItem key={v} value={v}>
                        {emp.name ?? emp.employeeNumber}
                        {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Total Product Amount"
                value={formData.totalProductAmount ?? 0}
                disabled
                helperText="From product lines below"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notes"
                multiline
                rows={1}
                value={formData.notes ?? ''}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Product Sales</Typography>
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
                      <TableCell align="right">TP with VAT</TableCell>
                      <TableCell align="right">Qty Sold</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {!formData.lines?.length ? (
                      <TableRow>
                        <TableCell colSpan={5} align="center">No products. Add at least one.</TableCell>
                      </TableRow>
                    ) : (
                      formData.lines.map((line, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{line.productName ?? safeProducts.find(p => p.id === line.productId)?.name}</TableCell>
                          <TableCell align="right">{line.tpWithVat?.toFixed(2)}</TableCell>
                          <TableCell align="right">{line.quantitySold}</TableCell>
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
            {selectedEntry ? 'Update' : 'Save Draft'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={showLineDialog} onClose={() => setShowLineDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editingLine?.index !== undefined ? 'Edit Product' : 'Add Product'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel>Product</InputLabel>
              <Select
                value={editingLine?.productId ?? ''}
                label="Product"
                onChange={async (e) => {
                  const productId = e.target.value;
                  const product = safeProducts.find(p => p.id === productId);
                  let outstanding = 0;
                  if (formData.territoryId && productId) {
                    try {
                      outstanding = await pharmaService.getOutstandingQuantity(formData.territoryId, productId);
                    } catch (err) {
                      console.error(err);
                    }
                  }
                  setEditingLine({
                    ...editingLine,
                    productId,
                    tpWithVat: product?.wholesalePrice ?? 0,
                    currentOutstandingQuantity: outstanding
                  });
                }}
              >
                {products.map((p) => (
                  <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            {editingLine?.productId && (
              <Alert severity={editingLine.currentOutstandingQuantity && editingLine.currentOutstandingQuantity > 0 ? 'info' : 'warning'} sx={{ py: 0 }}>
                Outstanding in territory: <strong>{editingLine.currentOutstandingQuantity ?? 0}</strong>
              </Alert>
            )}
            <TextField
              fullWidth
              label="Quantity Sold"
              type="number"
              value={editingLine?.quantitySold ?? 0}
              onChange={(e) => setEditingLine({ ...editingLine, quantitySold: Number(e.target.value) })}
              required
              error={!!(editingLine?.quantitySold && editingLine?.currentOutstandingQuantity != null && editingLine.quantitySold > editingLine.currentOutstandingQuantity)}
              helperText={editingLine?.quantitySold && editingLine?.currentOutstandingQuantity != null && editingLine.quantitySold > editingLine.currentOutstandingQuantity ? 'Exceeds outstanding' : ''}
            />
            <TextField
              fullWidth
              label="TP with VAT"
              type="number"
              value={editingLine?.tpWithVat ?? 0}
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

export default SoldProductEntryPage;
