import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import { useModuleConfig } from '../../contexts/ModuleConfigContext';
import inventoryService, { Product, Stock, Warehouse } from '../../services/inventoryService';
import { useSnackbar } from 'notistack';

/**
 * Pharma-facing entry point for warehouse inventory adjustments (e.g. expired goods destroyed).
 * Uses inventory-service stock adjust with quantityDelta.
 */
const WarehouseStockAdjustment: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const { isModuleEnabled } = useModuleConfig();
  const { enqueueSnackbar } = useSnackbar();

  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [stockRows, setStockRows] = useState<Stock[]>([]);
  const [loading, setLoading] = useState(false);

  const [warehouseId, setWarehouseId] = useState('');
  const [productId, setProductId] = useState('');
  const [quantityDelta, setQuantityDelta] = useState('');
  const [reason, setReason] = useState('EXPIRED');

  const inventoryOn = isModuleEnabled('inventory');

  useEffect(() => {
    if (!currentOrganizationId || !inventoryOn) return;
    (async () => {
      try {
        const [wh, pr, st] = await Promise.all([
          inventoryService.getWarehouses(currentOrganizationId, true),
          inventoryService.getProducts(currentOrganizationId, true),
          inventoryService.getStock(currentOrganizationId),
        ]);
        setWarehouses(wh);
        setProducts(pr);
        setStockRows(st);
      } catch (e) {
        console.error(e);
        enqueueSnackbar('Failed to load inventory data', { variant: 'error' });
      }
    })();
  }, [currentOrganizationId, inventoryOn, enqueueSnackbar]);

  const currentOnHand = useMemo(() => {
    if (!warehouseId || !productId) return null;
    const row = stockRows.find((s) => s.productId === productId && s.warehouseId === warehouseId);
    return row?.quantityOnHand ?? null;
  }, [stockRows, warehouseId, productId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id || !warehouseId || !productId) {
      enqueueSnackbar('Select warehouse and product', { variant: 'warning' });
      return;
    }
    const delta = parseFloat(quantityDelta);
    if (Number.isNaN(delta) || delta === 0) {
      enqueueSnackbar('Enter a non-zero quantity change (negative removes stock)', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await inventoryService.adjustStock({
        organizationId: currentOrganizationId,
        productId,
        warehouseId,
        quantityDelta: delta,
        reason,
        createdBy: user.id,
      });
      enqueueSnackbar('Stock adjustment applied', { variant: 'success' });
      setQuantityDelta('');
      const st = await inventoryService.getStock(currentOrganizationId);
      setStockRows(st);
    } catch (err: unknown) {
      console.error(err);
      enqueueSnackbar('Failed to apply adjustment', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!inventoryOn) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="info">
          The Inventory module is not enabled for this organization. Enable Inventory to record warehouse stock
          write-offs.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }} component="form" onSubmit={handleSubmit}>
      <Typography variant="h4" gutterBottom>
        Warehouse stock write-off
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3, maxWidth: 720 }}>
        Record additions or removals against warehouse on-hand stock—for example, quantity destroyed after expiry.
        Use a negative quantity change to reduce stock.
      </Typography>

      <Card sx={{ maxWidth: 560 }}>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            This updates central inventory (warehouse on-hand). Territory &quot;Product Adjustments&quot; are separate and
            do not change warehouse stock.
          </Alert>

          <FormControl fullWidth sx={{ mb: 2 }} required>
            <InputLabel id="wh-label">Warehouse</InputLabel>
            <Select
              labelId="wh-label"
              label="Warehouse"
              value={warehouseId}
              onChange={(e) => setWarehouseId(e.target.value)}
            >
              {warehouses.map((w) => (
                <MenuItem key={w.id} value={w.id}>
                  {w.name} ({w.code})
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth sx={{ mb: 2 }} required>
            <InputLabel id="pr-label">Product</InputLabel>
            <Select
              labelId="pr-label"
              label="Product"
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
            >
              {products.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name} ({p.sku})
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {warehouseId && productId && (
            <Typography variant="body2" sx={{ mb: 2 }}>
              Current on-hand:{' '}
              <strong>
                {currentOnHand !== null && currentOnHand !== undefined ? currentOnHand.toFixed(2) : '— (no stock row)'}
              </strong>
            </Typography>
          )}

          <TextField
            fullWidth
            required
            type="number"
            label="Quantity change"
            value={quantityDelta}
            onChange={(e) => setQuantityDelta(e.target.value)}
            inputProps={{ step: '0.01' }}
            helperText="Negative reduces on-hand (e.g. -24 to remove 24 units destroyed). Positive adds stock."
            sx={{ mb: 2 }}
          />

          <FormControl fullWidth sx={{ mb: 2 }} required>
            <InputLabel id="rs-label">Reason</InputLabel>
            <Select labelId="rs-label" label="Reason" value={reason} onChange={(e) => setReason(e.target.value)}>
              <MenuItem value="EXPIRED">Expired / destroyed</MenuItem>
              <MenuItem value="DAMAGE">Damage</MenuItem>
              <MenuItem value="THEFT">Theft</MenuItem>
              <MenuItem value="PHYSICAL_COUNT">Physical count</MenuItem>
              <MenuItem value="ADJUSTMENT">General adjustment</MenuItem>
              <MenuItem value="CORRECTION">Correction</MenuItem>
            </Select>
          </FormControl>

          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Applying…' : 'Apply adjustment'}
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default WarehouseStockAdjustment;
