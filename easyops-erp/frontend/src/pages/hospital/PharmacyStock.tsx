import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Tabs,
  Tab,
} from '@mui/material';
import { Refresh as RefreshIcon, Add as AddIcon, SwapHoriz as TransferIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalPharmacyService, {
  Drug,
  PharmacyLocation,
  PharmacyStockItem,
  StockAdjustmentMovement,
  StockAdjustmentLine,
  StockReceiptLine,
  StockTransferMovement,
  StockTransferLine,
} from '../../services/hospitalPharmacyService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import userService from '../../services/userService';
import { User } from '../../types';
import './Hospital.css';

type TabKey = 'stock' | 'receipts' | 'adjustments' | 'transfers';


const PharmacyStockPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(true);
  const [pharmacies, setPharmacies] = useState<PharmacyLocation[]>([]);
  const [drugOptions, setDrugOptions] = useState<Drug[]>([]);
  const [drugSearchLoading, setDrugSearchLoading] = useState(false);
  const drugSearchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [selectedPharmacyId, setSelectedPharmacyId] = useState<string>('');
  const [stock, setStock] = useState<PharmacyStockItem[]>([]);
  const [adjustments, setAdjustments] = useState<StockAdjustmentMovement[]>([]);
  const [transfers, setTransfers] = useState<StockTransferMovement[]>([]);
  const [tab, setTab] = useState<TabKey>('stock');

  // Receipt form
  const [receiptDrug, setReceiptDrug] = useState<Drug | null>(null);
  const [receiptDrugQuery, setReceiptDrugQuery] = useState('');
  const [receiptQuantity, setReceiptQuantity] = useState<number>(0);
  const [receiptBatch, setReceiptBatch] = useState('');
  const [receiptExpiry, setReceiptExpiry] = useState('');

  // Adjustment form
  const [adjustDrug, setAdjustDrug] = useState<Drug | null>(null);
  const [adjustDrugQuery, setAdjustDrugQuery] = useState('');
  const [adjustBatch, setAdjustBatch] = useState('');
  const [adjustDelta, setAdjustDelta] = useState<number>(0);
  const [adjustReasonCode, setAdjustReasonCode] = useState('');
  const [adjustRemarks, setAdjustRemarks] = useState('');
  const [adjustRequisitionRef, setAdjustRequisitionRef] = useState('');

  // Transfer form
  const [transferDestinationId, setTransferDestinationId] = useState('');
  const [transferDrug, setTransferDrug] = useState<Drug | null>(null);
  const [transferDrugQuery, setTransferDrugQuery] = useState('');
  const [transferStockLine, setTransferStockLine] = useState<PharmacyStockItem | null>(null);
  const [transferQuantity, setTransferQuantity] = useState<number>(0);
  const [transferRequisitionRef, setTransferRequisitionRef] = useState('');
  /** Optional approver; if null the current user is used. */
  const [approverUser, setApproverUser] = useState<User | null>(null);
  const [approverQuery, setApproverQuery] = useState('');
  const [approverOptions, setApproverOptions] = useState<User[]>([]);
  const [approverLoading, setApproverLoading] = useState(false);
  const approverSearchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Modals
  const [transferDialogOpen, setTransferDialogOpen] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedPharmacyId) {
      loadStock();
    }
  }, [selectedPharmacyId]);

  useEffect(() => {
    return () => {
      if (drugSearchTimeoutRef.current) clearTimeout(drugSearchTimeoutRef.current);
      if (approverSearchTimeoutRef.current) clearTimeout(approverSearchTimeoutRef.current);
    };
  }, []);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const pharms = await hospitalPharmacyService.getPharmacies({ activeOnly: true });
      setPharmacies(pharms);
      if (pharms.length > 0) {
        setSelectedPharmacyId(pharms[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load stock data:', err);
      enqueueSnackbar('Failed to load pharmacy stock data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const getDrugLabel = (drug: Drug) => {
    const formPrefix = drug.form?.trim() ? drug.form.trim().slice(0, 3) : '';
    const brand = drug.brandName?.trim();
    const generic = drug.genericName?.trim();
    const strength = drug.strength?.trim();

    const brandPart = brand ? `${brand} (${generic})` : generic ?? '';
    const prefix = formPrefix ? `${formPrefix}. ` : '';
    return `${prefix}${brandPart}${strength ? ` ${strength}` : ''}`.trim();
  };

  const searchDrugOptions = (searchTerm: string) => {
    if (drugSearchTimeoutRef.current) {
      clearTimeout(drugSearchTimeoutRef.current);
    }
    const query = searchTerm.trim();
    if (query.length < 2) {
      setDrugOptions([]);
      setDrugSearchLoading(false);
      return;
    }
    setDrugSearchLoading(true);
    drugSearchTimeoutRef.current = setTimeout(async () => {
      try {
        const data = await hospitalPharmacyService.getDrugs({ name: query, activeOnly: true });
        const q = query.toLowerCase();
        const scored = data
          .map((d) => {
            const brand = d.brandName?.trim().toLowerCase() ?? '';
            const generic = d.genericName?.trim().toLowerCase() ?? '';
            const form = d.form?.trim().toLowerCase() ?? '';

            // Prefer exact match, then "startsWith", then "includes"
            const exact =
              (brand && brand === q) || (generic && generic === q) ? 0 : 1;
            const starts =
              (brand && brand.startsWith(q)) || (generic && generic.startsWith(q)) ? 0 : 1;
            const includes =
              (brand && brand.includes(q)) || (generic && generic.includes(q)) ? 0 : 1;

            // If user typed the form prefix (rare), bump matches there too
            const formHit = form.startsWith(q) ? -1 : 0;

            const score = exact * 1000 + starts * 100 + includes * 10 + (formHit === -1 ? 1 : 0);
            return { d, score };
          })
          .sort((a, b) => a.score - b.score)
          .map((x) => x.d);

        setDrugOptions(scored.slice(0, 100));
      } catch (err) {
        console.error('Failed to search drugs:', err);
      } finally {
        setDrugSearchLoading(false);
      }
    }, 250);
  };

  const searchApproverOptions = (query: string) => {
    if (approverSearchTimeoutRef.current) clearTimeout(approverSearchTimeoutRef.current);
    const q = query.trim();
    if (q.length < 2) {
      setApproverOptions([]);
      setApproverLoading(false);
      return;
    }
    setApproverLoading(true);
    approverSearchTimeoutRef.current = setTimeout(async () => {
      try {
        const result = await userService.searchUsers(q, { page: 0, size: 20 });
        setApproverOptions(result.content ?? []);
      } catch {
        setApproverOptions([]);
      } finally {
        setApproverLoading(false);
      }
    }, 250);
  };

  const getApproverLabel = (u: User) => {
    const name = [u.firstName, u.lastName].filter(Boolean).join(' ');
    return name ? `${name} (${u.username})` : u.username;
  };

  const loadStock = async () => {
    if (!selectedPharmacyId) return;
    try {
      const [items, adjustmentRows, transferRows] = await Promise.all([
        hospitalPharmacyService.getPharmacyStock(selectedPharmacyId),
        hospitalPharmacyService.getStockAdjustments(selectedPharmacyId).catch(() => [] as StockAdjustmentMovement[]),
        hospitalPharmacyService.getStockTransfers(selectedPharmacyId).catch(() => [] as StockTransferMovement[]),
      ]);
      setStock(items);
      setAdjustments(adjustmentRows);
      setTransfers(transferRows);
    } catch (err: any) {
      console.error('Failed to load stock:', err);
      enqueueSnackbar('Failed to load stock', { variant: 'error' });
    }
  };

  const currentPharmacy = useMemo(
    () => pharmacies.find((p) => p.id === selectedPharmacyId),
    [pharmacies, selectedPharmacyId]
  );

  const transferStockOptions = useMemo(() => {
    if (!transferDrug?.id) return [];
    return stock.filter((s) => s.drugId === transferDrug.id);
  }, [stock, transferDrug]);

  const formatTransferBatchOption = (item: PharmacyStockItem) => {
    const batch = item.batchNumber?.trim() ? item.batchNumber.trim() : '(no batch)';
    const exp = item.expiryDate ? ` · exp ${item.expiryDate}` : '';
    return `${batch}${exp} · qty ${item.quantityOnHand}`;
  };

  const handleReceive = async () => {
    if (!selectedPharmacyId) return;
    if (!receiptDrug?.id || receiptQuantity <= 0) {
      enqueueSnackbar('Drug and positive quantity are required for receipt', { variant: 'warning' });
      return;
    }
    const line: StockReceiptLine = {
      drugId: receiptDrug.id,
      quantity: receiptQuantity,
      batchNumber: receiptBatch || undefined,
      expiryDate: receiptExpiry || undefined,
    };
    try {
      await hospitalPharmacyService.receiveStock(selectedPharmacyId, [line]);
      enqueueSnackbar('Stock received successfully', { variant: 'success' });
      setReceiptDrug(null);
      setReceiptDrugQuery('');
      setReceiptQuantity(0);
      setReceiptBatch('');
      setReceiptExpiry('');
      await loadStock();
    } catch (err: any) {
      console.error('Failed to receive stock:', err);
      const message =
        ehrApiErrorMessage(err, 'Failed to receive stock');
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleAdjust = async () => {
    if (!selectedPharmacyId) return;
    if (!adjustDrug?.id || adjustDelta === 0) {
      enqueueSnackbar('Drug and non-zero delta are required for adjustment', { variant: 'warning' });
      return;
    }
    if (!adjustReasonCode) {
      enqueueSnackbar('Adjustment reason is required', { variant: 'warning' });
      return;
    }
    if (!adjustRequisitionRef.trim()) {
      enqueueSnackbar('Requisition reference is required before approval.', { variant: 'warning' });
      return;
    }
    const reasonParts = [`REQ:${adjustRequisitionRef.trim()}`, adjustReasonCode];
    if (adjustRemarks.trim()) reasonParts.push(adjustRemarks.trim());
    const line: StockAdjustmentLine = {
      drugId: adjustDrug.id,
      quantityDelta: adjustDelta,
      batchNumber: adjustBatch || undefined,
      reason: reasonParts.join(': '),
    };
    try {
      await hospitalPharmacyService.adjustStock(selectedPharmacyId, [line], approverUser?.id || undefined);
      enqueueSnackbar('Stock adjusted successfully', { variant: 'success' });
      setAdjustDrug(null);
      setAdjustDrugQuery('');
      setAdjustDelta(0);
      setAdjustBatch('');
      setAdjustReasonCode('');
      setAdjustRemarks('');
      setAdjustRequisitionRef('');
      setApproverUser(null);
      setApproverQuery('');
      await loadStock();
      setTab('adjustments');
    } catch (err: any) {
      console.error('Failed to adjust stock:', err);
      const message =
        ehrApiErrorMessage(err, 'Failed to adjust stock');
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleOpenTransferDialog = () => {
    setTransferDrug(null);
    setTransferDrugQuery('');
    setTransferStockLine(null);
    setTransferQuantity(0);
    setTransferDestinationId('');
    setTransferRequisitionRef('');
    setApproverUser(null);
    setApproverQuery('');
    setApproverOptions([]);
    setTransferDialogOpen(true);
  };

  const handleTransfer = async () => {
    if (!selectedPharmacyId) return;
    if (!transferDestinationId || !transferDrug?.id || transferQuantity <= 0) {
      enqueueSnackbar('Destination, drug, and positive quantity are required', { variant: 'warning' });
      return;
    }
    if (!transferStockLine) {
      enqueueSnackbar('Select a batch from available stock', { variant: 'warning' });
      return;
    }
    if (!transferRequisitionRef.trim()) {
      enqueueSnackbar('Requisition reference is required before approval.', { variant: 'warning' });
      return;
    }
    const line: StockTransferLine = {
      drugId: transferDrug.id,
      quantity: transferQuantity,
      batchNumber: transferStockLine.batchNumber ?? undefined,
      expiryDate: transferStockLine.expiryDate ?? undefined,
      notes: `REQ:${transferRequisitionRef.trim()}`,
    };
    try {
      await hospitalPharmacyService.transferStock(
        selectedPharmacyId,
        transferDestinationId,
        [line],
        approverUser?.id || undefined
      );
      enqueueSnackbar('Stock transferred successfully', { variant: 'success' });
      setTransferDialogOpen(false);
      setApproverUser(null);
      setApproverQuery('');
      await loadStock();
      setTab('transfers');
    } catch (err: any) {
      console.error('Failed to transfer stock:', err);
      const message =
        ehrApiErrorMessage(err, 'Failed to transfer stock');
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  if (loading && !currentPharmacy) {
    return (
      <Box className="hospital-page" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4" component="h1">
            Pharmacy Stock
          </Typography>
          <Typography variant="body2">
            Real-time on-hand stock per pharmacy, with receipts, adjustments, and transfers.
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <FormControl size="small" sx={{ minWidth: 220, backgroundColor: 'white', borderRadius: 1 }}>
            <InputLabel>Pharmacy</InputLabel>
            <Select
              label="Pharmacy"
              value={selectedPharmacyId}
              onChange={(e) => setSelectedPharmacyId(e.target.value as string)}
            >
              {pharmacies.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name} ({p.type})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            variant="contained"
            color="secondary"
            startIcon={<RefreshIcon />}
            onClick={loadStock}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs
            value={tab}
            onChange={(_, value) => setTab(value)}
            textColor="primary"
            indicatorColor="primary"
          >
            <Tab label="On-Hand Stock" value="stock" />
            <Tab label="Receipts" value="receipts" />
            <Tab label="Adjustments" value="adjustments" />
            <Tab label="Transfers" value="transfers" />
          </Tabs>

          {tab === 'stock' && (
            <Box mt={2}>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Drug</TableCell>
                      <TableCell>Batch</TableCell>
                      <TableCell>Expiry</TableCell>
                      <TableCell align="right">Quantity On Hand</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {stock.map((item) => (
                      <TableRow key={item.stockId} hover>
                        <TableCell>
                          <Typography variant="body2">{item.genericName}</Typography>
                          {item.brandName && (
                            <Typography variant="caption" color="text.secondary">
                              {item.brandName}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>{item.batchNumber || '-'}</TableCell>
                        <TableCell>{item.expiryDate || '-'}</TableCell>
                        <TableCell align="right">{item.quantityOnHand}</TableCell>
                      </TableRow>
                    ))}
                    {stock.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No stock records for this pharmacy yet.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {tab === 'receipts' && (
            <Box mt={2} display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2}>
              <Autocomplete
                options={drugOptions}
                loading={drugSearchLoading}
                value={receiptDrug}
                inputValue={receiptDrugQuery}
                onInputChange={(_, value) => {
                  setReceiptDrugQuery(value);
                  searchDrugOptions(value);
                }}
                onChange={(_, value) => setReceiptDrug(value)}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                getOptionLabel={getDrugLabel}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Drug"
                    placeholder="Type at least 2 characters"
                    required
                  />
                )}
              />
              <TextField
                label="Quantity"
                type="number"
                value={receiptQuantity}
                onChange={(e) => setReceiptQuantity(Number(e.target.value) || 0)}
                required
              />
              <TextField
                label="Batch Number"
                value={receiptBatch}
                onChange={(e) => setReceiptBatch(e.target.value)}
              />
              <TextField
                label="Expiry Date"
                type="date"
                value={receiptExpiry}
                onChange={(e) => setReceiptExpiry(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <Box gridColumn={{ xs: '1 / span 1', md: '1 / span 2' }} mt={1}>
                <Button variant="contained" color="primary" startIcon={<AddIcon />} onClick={handleReceive}>
                  Record Receipt
                </Button>
              </Box>
            </Box>
          )}

          {tab === 'adjustments' && (
            <Box mt={2}>
              <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2}>
                <Autocomplete
                  options={drugOptions}
                  loading={drugSearchLoading}
                  value={adjustDrug}
                  inputValue={adjustDrugQuery}
                  onInputChange={(_, value) => {
                    setAdjustDrugQuery(value);
                    searchDrugOptions(value);
                  }}
                  onChange={(_, value) => setAdjustDrug(value)}
                  isOptionEqualToValue={(option, value) => option.id === value.id}
                  getOptionLabel={getDrugLabel}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Drug"
                      placeholder="Type at least 2 characters"
                      required
                    />
                  )}
                />
                <TextField
                  label="Batch Number"
                  value={adjustBatch}
                  onChange={(e) => setAdjustBatch(e.target.value)}
                />
                <TextField
                  label="Quantity Delta (+/-)"
                  type="number"
                  value={adjustDelta}
                  onChange={(e) => setAdjustDelta(Number(e.target.value) || 0)}
                />
                <FormControl fullWidth required>
                  <InputLabel>Reason *</InputLabel>
                  <Select
                    label="Reason *"
                    value={adjustReasonCode}
                    onChange={(e) => setAdjustReasonCode(e.target.value as string)}
                  >
                    <MenuItem value="EXPIRY">Expiry</MenuItem>
                    <MenuItem value="DAMAGE">Damage</MenuItem>
                    <MenuItem value="PHYSICAL_MISMATCH">Physical Stock Mismatch</MenuItem>
                    <MenuItem value="RETURN_TO_SUPPLIER">Return to Supplier</MenuItem>
                    <MenuItem value="OTHER">Other</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="Requisition Reference"
                  required
                  value={adjustRequisitionRef}
                  onChange={(e) => setAdjustRequisitionRef(e.target.value)}
                  placeholder="e.g. REQ-ADJ-2026-001"
                />
                <TextField
                  label="Remarks"
                  placeholder="Additional notes (optional)"
                  value={adjustRemarks}
                  onChange={(e) => setAdjustRemarks(e.target.value)}
                  multiline
                  rows={2}
                  sx={{ gridColumn: { md: '1 / span 2' } }}
                />
                <Autocomplete
                  options={approverOptions}
                  loading={approverLoading}
                  value={approverUser}
                  inputValue={approverQuery}
                  onInputChange={(_, value) => {
                    setApproverQuery(value);
                    searchApproverOptions(value);
                  }}
                  onChange={(_, value) => setApproverUser(value)}
                  isOptionEqualToValue={(option, value) => option.id === value.id}
                  getOptionLabel={getApproverLabel}
                  sx={{ gridColumn: { md: '1 / span 2' } }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Approver (optional)"
                      placeholder="Type name or username to search"
                      helperText="Optional — leave blank to record the current user as approver."
                    />
                  )}
                />
                <Box gridColumn={{ xs: '1 / span 1', md: '1 / span 2' }} mt={1}>
                  <Button variant="contained" color="primary" onClick={handleAdjust}>
                    Record Adjustment
                  </Button>
                </Box>
              </Box>

              <TableContainer sx={{ mt: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date & Time</TableCell>
                      <TableCell>Drug</TableCell>
                      <TableCell>Batch</TableCell>
                      <TableCell align="right">Delta</TableCell>
                      <TableCell>Reason</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {adjustments.map((movement) => (
                      <TableRow key={movement.movementId} hover>
                        <TableCell>{new Date(movement.movementTime).toLocaleString()}</TableCell>
                        <TableCell>
                          <Typography variant="body2">{movement.genericName}</Typography>
                          {movement.brandName && (
                            <Typography variant="caption" color="text.secondary">
                              {movement.brandName}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>{movement.batchNumber || '-'}</TableCell>
                        <TableCell align="right">{Number(movement.quantityDelta) || 0}</TableCell>
                        <TableCell>{movement.reason || '-'}</TableCell>
                      </TableRow>
                    ))}
                    {adjustments.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No adjustment records for this pharmacy yet.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {tab === 'transfers' && (
            <Box mt={2}>
              <Button
                variant="contained"
                color="primary"
                startIcon={<TransferIcon />}
                onClick={handleOpenTransferDialog}
              >
                New Transfer
              </Button>
              <TableContainer sx={{ mt: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date & Time</TableCell>
                      <TableCell>Direction</TableCell>
                      <TableCell>Drug</TableCell>
                      <TableCell>Batch</TableCell>
                      <TableCell align="right">Quantity</TableCell>
                      <TableCell>Notes</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {transfers.map((movement) => {
                      const direction = movement.movementType === 'transfer_in' ? 'IN' : 'OUT';
                      return (
                        <TableRow key={movement.movementId} hover>
                          <TableCell>{new Date(movement.movementTime).toLocaleString()}</TableCell>
                          <TableCell>{direction}</TableCell>
                          <TableCell>
                            <Typography variant="body2">{movement.genericName}</Typography>
                            {movement.brandName && (
                              <Typography variant="caption" color="text.secondary">
                                {movement.brandName}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>{movement.batchNumber || '-'}</TableCell>
                          <TableCell align="right">{Math.abs(Number(movement.quantity) || 0)}</TableCell>
                          <TableCell>{movement.notes || '-'}</TableCell>
                        </TableRow>
                      );
                    })}
                    {transfers.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={6} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No transfer records for this pharmacy yet.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Transfer Dialog */}
      <Dialog open={transferDialogOpen} onClose={() => setTransferDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Transfer Stock</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <FormControl fullWidth>
              <InputLabel>Destination Pharmacy</InputLabel>
              <Select
                label="Destination Pharmacy"
                value={transferDestinationId}
                onChange={(e) => setTransferDestinationId(e.target.value as string)}
              >
                {pharmacies
                  .filter((p) => p.id !== selectedPharmacyId)
                  .map((p) => (
                    <MenuItem key={p.id} value={p.id}>
                      {p.name} ({p.type})
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
            <Autocomplete
              fullWidth
              options={drugOptions}
              loading={drugSearchLoading}
              value={transferDrug}
              inputValue={transferDrugQuery}
              onInputChange={(_, value) => {
                setTransferDrugQuery(value);
                searchDrugOptions(value);
              }}
              onChange={(_, value) => {
                setTransferDrug(value);
                setTransferStockLine(null);
              }}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              getOptionLabel={getDrugLabel}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Drug"
                  placeholder="Type at least 2 characters"
                  required
                />
              )}
            />
            <Autocomplete
              fullWidth
              disabled={!transferDrug}
              options={transferStockOptions}
              value={transferStockLine}
              onChange={(_, value) => setTransferStockLine(value)}
              isOptionEqualToValue={(option, value) => option.stockId === value.stockId}
              getOptionLabel={formatTransferBatchOption}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Batch number"
                  required
                  placeholder={transferDrug ? 'Select a batch' : 'Select drug first'}
                  helperText={
                    transferDrug && transferStockOptions.length === 0
                      ? 'No stock for this drug at this pharmacy. Record a receipt first.'
                      : undefined
                  }
                />
              )}
            />
            <TextField
              label="Quantity"
              type="number"
              value={transferQuantity}
              onChange={(e) => setTransferQuantity(Number(e.target.value) || 0)}
            />
            <TextField
              label="Requisition Reference"
              required
              value={transferRequisitionRef}
              onChange={(e) => setTransferRequisitionRef(e.target.value)}
              placeholder="e.g. REQ-TRF-2026-001"
            />
            <Autocomplete
              options={approverOptions}
              loading={approverLoading}
              value={approverUser}
              inputValue={approverQuery}
              onInputChange={(_, value) => {
                setApproverQuery(value);
                searchApproverOptions(value);
              }}
              onChange={(_, value) => setApproverUser(value)}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              getOptionLabel={getApproverLabel}
              sx={{ gridColumn: { xs: '1 / span 1', md: '1 / span 2' } }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Approver (optional)"
                  placeholder="Type name or username to search"
                  helperText="Optional — leave blank to record the current user as approver."
                />
              )}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTransferDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleTransfer}>
            Transfer
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PharmacyStockPage;

