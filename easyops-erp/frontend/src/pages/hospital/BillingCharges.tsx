import React, { useEffect, useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
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
} from '@mui/material';
import { Add as AddIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalBillingService, {
  ChargeResponse,
  CreateChargeRequest,
  PagedResponse,
} from '../../services/hospitalBillingService';
import hospitalService from '../../services/hospitalService';
import './Hospital.css';

/** Matches canonical UUID strings accepted by `java.util.UUID` (do not restrict version/variant nibble). */
const isCanonicalUuid = (value: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim());

function buildCreateChargePayload(input: {
  patientId: string;
  visitId?: string;
  corporateContractId?: string;
  sourceService: string;
  sourceReferenceId: string;
  itemCode: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  taxAmount: number;
  discountSource?: string;
}): CreateChargeRequest {
  const payload: CreateChargeRequest = {
    patientId: input.patientId,
    sourceService: input.sourceService,
    sourceReferenceId: input.sourceReferenceId,
    itemCode: input.itemCode,
    quantity: input.quantity,
    unitPrice: input.unitPrice,
    discountAmount: input.discountAmount,
    taxAmount: input.taxAmount,
  };
  if (input.visitId) payload.visitId = input.visitId;
  if (input.corporateContractId) payload.corporateContractId = input.corporateContractId;
  if (input.itemDescription) payload.itemDescription = input.itemDescription;
  if (input.discountSource) payload.discountSource = input.discountSource;
  return payload;
}

const BillingChargesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [charges, setCharges] = useState<ChargeResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(50);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [patientIdFilter, setPatientIdFilter] = useState<string>('');
  const [visitIdFilter, setVisitIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sourceServiceFilter, setSourceServiceFilter] = useState<string>('');
  const [fromDateFilter, setFromDateFilter] = useState<string>('');
  const [toDateFilter, setToDateFilter] = useState<string>('');

  const [newCharge, setNewCharge] = useState<CreateChargeRequest>({
    sourceService: '',
    sourceReferenceId: '',
    patientId: '',
    visitId: '',
    corporateContractId: '',
    itemCode: '',
    itemDescription: '',
    quantity: 1,
    unitPrice: 0,
    discountAmount: 0,
    discountSource: '',
    taxAmount: 0,
  });
  const [patientSuggestions, setPatientSuggestions] = useState<Array<{ patientId: string; mrn?: string; fullName?: string }>>([]);

  useEffect(() => {
    loadCharges();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    const q = (newCharge.patientId || '').trim();
    if (q.length < 2) {
      setPatientSuggestions([]);
      return;
    }
    if (isCanonicalUuid(q)) {
      const timer = setTimeout(async () => {
        try {
          const { data } = await hospitalService.getPatient(q);
          setPatientSuggestions([
            { patientId: data.patientId, mrn: data.mrn, fullName: data.fullName },
          ]);
        } catch {
          setPatientSuggestions([]);
        }
      }, 200);
      return () => clearTimeout(timer);
    }
    const timer = setTimeout(async () => {
      try {
        const res = await hospitalService.searchPatients(q);
        const next = (res.data ?? []).slice(0, 10).map((p) => ({
          patientId: p.patientId,
          mrn: p.mrn,
          fullName: p.fullName,
        }));
        setPatientSuggestions(next);
      } catch {
        setPatientSuggestions([]);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [newCharge.patientId]);

  const loadCharges = async () => {
    try {
      setLoading(true);
      const params: { patientId?: string; page: number; size: number } = {
        page,
        size,
      };
      if (patientIdFilter.trim()) {
        params.patientId = patientIdFilter.trim();
      }
      const response: PagedResponse<ChargeResponse> = await hospitalBillingService.getCharges(params);
      setCharges(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load charges', err);
      enqueueSnackbar('Failed to load charges', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    setPage(0);
    loadCharges();
  };

  const handleCreateCharge = async () => {
    const patientRaw = (newCharge.patientId || '').trim();
    const sourceService = (newCharge.sourceService || '').trim();
    const sourceReferenceId = (newCharge.sourceReferenceId || '').trim();
    const itemCode = (newCharge.itemCode || '').trim();

    const visitRaw = (newCharge.visitId || '').trim();
    const corporateContractId = (newCharge.corporateContractId || '').trim();

    if (!patientRaw || !sourceService || !sourceReferenceId || !itemCode) {
      enqueueSnackbar('Please fill in patient, source, and item code', { variant: 'warning' });
      return;
    }

    if (sourceService.length > 50) {
      enqueueSnackbar('Source service must be at most 50 characters', { variant: 'warning' });
      return;
    }
    if (itemCode.length > 100) {
      enqueueSnackbar('Item code must be at most 100 characters', { variant: 'warning' });
      return;
    }
    if (sourceReferenceId.length > 255) {
      enqueueSnackbar('Source reference must be at most 255 characters', { variant: 'warning' });
      return;
    }
    const ds = newCharge.discountSource?.trim();
    if (ds && ds.length > 50) {
      enqueueSnackbar('Discount source must be at most 50 characters', { variant: 'warning' });
      return;
    }

    let patientId = patientRaw;
    if (!isCanonicalUuid(patientRaw)) {
      try {
        const p = await hospitalService.getPatientByMrn(encodeURIComponent(patientRaw));
        patientId = p.data.patientId;
      } catch {
        try {
          const res = await hospitalService.searchPatients(patientRaw);
          const list = res.data ?? [];
          if (list.length > 0) {
            patientId = list[0].patientId;
            if (list.length > 1) {
              enqueueSnackbar('Multiple patients matched by name; using the first result. Prefer MRN for exact match.', {
                variant: 'warning',
              });
            }
          } else {
            enqueueSnackbar(
              'Patient: use a patient UUID, valid MRN, or an existing patient name.',
              { variant: 'warning' }
            );
            return;
          }
        } catch {
          enqueueSnackbar(
            'Patient: use a patient UUID, valid MRN, or an existing patient name.',
            { variant: 'warning' }
          );
          return;
        }
      }
    }

    let visitId: string | undefined;
    if (visitRaw) {
      if (isCanonicalUuid(visitRaw)) {
        visitId = visitRaw;
      } else {
        try {
          const enc = await hospitalService.getEncounterByNumber(encodeURIComponent(visitRaw));
          visitId = enc.data.encounterId;
        } catch {
          enqueueSnackbar(
            'Visit: use a visit UUID, a valid encounter number, or leave blank.',
            { variant: 'warning' }
          );
          return;
        }
      }
    }

    if (corporateContractId && !isCanonicalUuid(corporateContractId)) {
      enqueueSnackbar('Corporate contract ID must be a valid UUID (or leave blank)', {
        variant: 'warning',
      });
      return;
    }

    const qty = Number(newCharge.quantity);
    const unit = Number(newCharge.unitPrice);
    if (!Number.isFinite(qty) || qty <= 0) {
      enqueueSnackbar('Quantity must be a positive number', { variant: 'warning' });
      return;
    }
    if (!Number.isFinite(unit) || unit < 0) {
      enqueueSnackbar('Unit price must be zero or a positive number', { variant: 'warning' });
      return;
    }

    const payload = buildCreateChargePayload({
      patientId,
      visitId,
      corporateContractId: corporateContractId || undefined,
      sourceService,
      sourceReferenceId,
      itemCode,
      itemDescription: newCharge.itemDescription?.trim() || undefined,
      quantity: qty,
      unitPrice: unit,
      discountAmount: Number(newCharge.discountAmount) || 0,
      taxAmount: Number(newCharge.taxAmount) || 0,
      discountSource: ds || undefined,
    });

    try {
      setLoading(true);
      const created = await hospitalBillingService.createCharges([payload]);
      enqueueSnackbar(`Created ${created.length} charge(s)`, { variant: 'success' });
      // Reload first page to include new charge
      setPage(0);
      loadCharges();
    } catch (err: unknown) {
      console.error('Failed to create charge', err);
      const ax = err as { response?: { data?: { message?: string; error?: string } | string } };
      const data = ax.response?.data;
      let message = 'Failed to create charge';
      if (typeof data === 'string') {
        message = data;
      } else if (data && typeof data === 'object') {
        const o = data as { message?: string; error?: string };
        if (o.message) message = o.message;
        else if (o.error) message = o.error;
      }
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Billing – Charges
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateCharge}
            disabled={loading}
          >
            Create Charge
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <TextField
              label="Patient ID"
              size="small"
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
            />
            <TextField
              label="Visit ID"
              size="small"
              value={visitIdFilter}
              onChange={(e) => setVisitIdFilter(e.target.value)}
            />
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="POSTED">Posted</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
                <MenuItem value="REVERSED">Reversed</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Source Service"
              size="small"
              value={sourceServiceFilter}
              onChange={(e) => setSourceServiceFilter(e.target.value)}
            />
            <TextField
              label="From Date"
              type="date"
              size="small"
              value={fromDateFilter}
              onChange={(e) => setFromDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To Date"
              type="date"
              size="small"
              value={toDateFilter}
              onChange={(e) => setToDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Create Charge
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <Autocomplete
              freeSolo
              options={patientSuggestions}
              filterOptions={(options) => options}
              getOptionLabel={(option) =>
                typeof option === 'string'
                  ? option
                  : `${option.fullName || 'Patient'}${option.mrn ? ` • ${option.mrn}` : ''} • ${option.patientId}`
              }
              inputValue={newCharge.patientId}
              onInputChange={(_, value) => setNewCharge({ ...newCharge, patientId: value })}
              onChange={(_, value) => {
                if (typeof value === 'string') {
                  setNewCharge({ ...newCharge, patientId: value });
                  return;
                }
                if (!value) return;
                setNewCharge({ ...newCharge, patientId: value.mrn || value.patientId });
              }}
              sx={{ minWidth: 320 }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Patient"
                  size="small"
                  placeholder="UUID, MRN, or name"
                  helperText="Patient UUID, MRN, or patient name"
                />
              )}
            />
            <TextField
              label="Visit / encounter"
              size="small"
              placeholder="UUID or encounter number"
              helperText="Optional: visit UUID or hospital encounter number"
              value={newCharge.visitId || ''}
              onChange={(e) => setNewCharge({ ...newCharge, visitId: e.target.value })}
            />
            <TextField
              label="Source Service"
              size="small"
              value={newCharge.sourceService}
              onChange={(e) => setNewCharge({ ...newCharge, sourceService: e.target.value })}
            />
            <TextField
              label="Source Reference ID"
              size="small"
              value={newCharge.sourceReferenceId}
              onChange={(e) => setNewCharge({ ...newCharge, sourceReferenceId: e.target.value })}
            />
            <TextField
              label="Item Code"
              size="small"
              value={newCharge.itemCode}
              onChange={(e) => setNewCharge({ ...newCharge, itemCode: e.target.value })}
            />
            <TextField
              label="Item Description"
              size="small"
              value={newCharge.itemDescription || ''}
              onChange={(e) => setNewCharge({ ...newCharge, itemDescription: e.target.value })}
            />
            <TextField
              label="Quantity"
              type="number"
              size="small"
              value={newCharge.quantity === 0 ? '' : newCharge.quantity}
              inputProps={{ min: 0, onFocus: (e: React.FocusEvent<HTMLInputElement>) => e.target.select() }}
              onChange={(e) =>
                setNewCharge({ ...newCharge, quantity: e.target.value === '' ? 0 : Number(e.target.value) })
              }
            />
            <TextField
              label="Unit Price"
              type="number"
              size="small"
              value={newCharge.unitPrice === 0 ? '' : newCharge.unitPrice}
              inputProps={{ min: 0, onFocus: (e: React.FocusEvent<HTMLInputElement>) => e.target.select() }}
              onChange={(e) =>
                setNewCharge({ ...newCharge, unitPrice: e.target.value === '' ? 0 : Number(e.target.value) })
              }
            />
            <TextField
              label="Discount"
              type="number"
              size="small"
              value={newCharge.discountAmount === 0 ? '' : newCharge.discountAmount}
              inputProps={{ min: 0, onFocus: (e: React.FocusEvent<HTMLInputElement>) => e.target.select() }}
              onChange={(e) =>
                setNewCharge({
                  ...newCharge,
                  discountAmount: e.target.value === '' ? 0 : Number(e.target.value),
                })
              }
            />
            <TextField
              label="Tax"
              type="number"
              size="small"
              value={newCharge.taxAmount === 0 ? '' : newCharge.taxAmount}
              inputProps={{ min: 0, onFocus: (e: React.FocusEvent<HTMLInputElement>) => e.target.select() }}
              onChange={(e) =>
                setNewCharge({
                  ...newCharge,
                  taxAmount: e.target.value === '' ? 0 : Number(e.target.value),
                })
              }
            />
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Charges</Typography>
            <Typography variant="body2">
              Total: {totalElements}
            </Typography>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Visit ID</TableCell>
                    <TableCell>Source</TableCell>
                    <TableCell>Source Ref</TableCell>
                    <TableCell>Item Code</TableCell>
                    <TableCell>Quantity</TableCell>
                    <TableCell>Unit Price</TableCell>
                    <TableCell>Gross</TableCell>
                    <TableCell>Discount</TableCell>
                    <TableCell>Tax</TableCell>
                    <TableCell>Net</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Invoice ID</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {charges.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell>{c.patientId}</TableCell>
                      <TableCell>{c.visitId}</TableCell>
                      <TableCell>{c.sourceService}</TableCell>
                      <TableCell>{c.sourceReferenceId}</TableCell>
                      <TableCell>{c.itemCode}</TableCell>
                      <TableCell>{c.quantity}</TableCell>
                      <TableCell>{c.unitPrice}</TableCell>
                      <TableCell>{c.grossAmount}</TableCell>
                      <TableCell>{c.discountAmount}</TableCell>
                      <TableCell>{c.taxAmount}</TableCell>
                      <TableCell>{c.netAmount}</TableCell>
                      <TableCell>{c.status}</TableCell>
                      <TableCell>{c.invoiceId}</TableCell>
                    </TableRow>
                  ))}
                  {charges.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={13} align="center">
                        No charges found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default BillingChargesPage;

