import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, ArrowBack as ArrowBackIcon, Delete as DeleteIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CreatePackageItemRequest,
  PackageDetailResponse,
  PackageItemResponse,
} from '../../services/hospitalCorporateDiscountService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PackageDetailPage: React.FC = () => {
  const { packageId } = useParams<{ packageId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(true);
  const [pkg, setPkg] = useState<PackageDetailResponse | null>(null);
  const [addItemDialogOpen, setAddItemDialogOpen] = useState(false);
  const [itemForm, setItemForm] = useState<CreatePackageItemRequest>({
    itemType: 'SERVICE',
    itemCode: '',
    quantityIncluded: 1,
  });

  const loadPackage = useCallback(async () => {
    if (!packageId) return;
    try {
      setLoading(true);
      const data = await hospitalCorporateDiscountService.getPackage(packageId);
      setPkg(data);
    } catch (err) {
      console.error('Failed to load package', err);
      enqueueSnackbar('Failed to load package', { variant: 'error' });
      setPkg(null);
    } finally {
      setLoading(false);
    }
  }, [packageId, enqueueSnackbar]);

  useEffect(() => {
    loadPackage();
  }, [loadPackage]);

  const handleBack = () => navigate('/hospital/corporate-discount/packages');

  const handleOpenAddItem = () => {
    setItemForm({ itemType: 'SERVICE', itemCode: '', quantityIncluded: 1 });
    setAddItemDialogOpen(true);
  };

  const handleAddItem = async () => {
    if (!packageId || !itemForm.itemCode?.trim()) {
      enqueueSnackbar('Item code is required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.addPackageItem(packageId, {
        ...itemForm,
        quantityIncluded: itemForm.quantityIncluded ?? 1,
      });
      enqueueSnackbar('Item added', { variant: 'success' });
      setAddItemDialogOpen(false);
      loadPackage();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to add item'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    if (!packageId || !window.confirm('Remove this item from the package?')) return;
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.deletePackageItem(packageId, itemId);
      enqueueSnackbar('Item removed', { variant: 'success' });
      loadPackage();
    } catch (err: unknown) {
      enqueueSnackbar('Failed to remove item', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!packageId) {
    return (
      <Box className="hospital-page">
        <Typography color="error">Missing package ID</Typography>
        <Button onClick={() => navigate('/hospital/corporate-discount/packages')}>Back to packages</Button>
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Button startIcon={<ArrowBackIcon />} onClick={handleBack} sx={{ color: 'white', mb: 1 }}>
            Back
          </Button>
          <Typography variant="h4">Package items</Typography>
          <Typography variant="body2">
            {pkg ? `${pkg.code} – ${pkg.name}` : `Package ${packageId}`}
          </Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={loadPackage} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Refresh
        </Button>
      </Box>

      {loading && !pkg ? (
        <Box display="flex" justifyContent="center" py={4}>
          <CircularProgress />
        </Box>
      ) : pkg ? (
        <>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary">
                Code: {pkg.code} · Name: {pkg.name}
                {pkg.defaultPrice != null && ` · Default price: ${pkg.defaultPrice}`}
                {' · '}
                Public: {pkg.isPublic ? 'Yes' : 'No'} · Corporate only: {pkg.isCorporateOnly ? 'Yes' : 'No'}
              </Typography>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Items</Typography>
                <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleOpenAddItem}>
                  Add item
                </Button>
              </Box>
              {loading ? (
                <Box display="flex" justifyContent="center" py={2}>
                  <CircularProgress size={24} />
                </Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Item type</TableCell>
                        <TableCell>Item code</TableCell>
                        <TableCell align="right">Quantity included</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {(!pkg.items || pkg.items.length === 0) ? (
                        <TableRow>
                          <TableCell colSpan={4} align="center">
                            No items. Add items to define what is included in this package.
                          </TableCell>
                        </TableRow>
                      ) : (
                        (pkg.items as PackageItemResponse[]).map((item) => (
                          <TableRow key={item.id}>
                            <TableCell>{item.itemType}</TableCell>
                            <TableCell>{item.itemCode}</TableCell>
                            <TableCell align="right">{item.quantityIncluded ?? 1}</TableCell>
                            <TableCell align="right">
                              <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleRemoveItem(item.id)}>
                                Remove
                              </Button>
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
        </>
      ) : null}

      <Dialog open={addItemDialogOpen} onClose={() => setAddItemDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add item</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Item type"
              value={itemForm.itemType}
              onChange={(e) => setItemForm((f) => ({ ...f, itemType: e.target.value }))}
              fullWidth
              placeholder="e.g. SERVICE, PACKAGE"
            />
            <TextField
              label="Item code"
              value={itemForm.itemCode}
              onChange={(e) => setItemForm((f) => ({ ...f, itemCode: e.target.value }))}
              required
              fullWidth
              placeholder="Service code or package code"
            />
            <TextField
              type="number"
              label="Quantity included"
              value={itemForm.quantityIncluded ?? 1}
              onChange={(e) => setItemForm((f) => ({ ...f, quantityIncluded: parseInt(e.target.value, 10) || 1 }))}
              inputProps={{ min: 1 }}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddItemDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddItem}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PackageDetailPage;
