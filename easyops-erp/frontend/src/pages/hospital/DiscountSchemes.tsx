import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon, Visibility as ViewIcon, Security as ApprovalLevelsIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CorporateResponse,
  CreateDiscountSchemeRequest,
  DiscountSchemeResponse,
  PagedResponse,
  UpdateDiscountSchemeRequest,
} from '../../services/hospitalCorporateDiscountService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const DISCOUNT_TYPES = ['PERCENT', 'AMOUNT'];
const STATUSES = ['ACTIVE', 'INACTIVE'];

const DiscountSchemesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [schemes, setSchemes] = useState<DiscountSchemeResponse[]>([]);
  const [corporates, setCorporates] = useState<CorporateResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [codeFilter, setCodeFilter] = useState('');
  const [corporateFilter, setCorporateFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewingScheme, setViewingScheme] = useState<DiscountSchemeResponse | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<CreateDiscountSchemeRequest>({
    code: '',
    name: '',
    corporateClientId: undefined,
    visitType: undefined,
    departmentId: undefined,
    serviceCode: undefined,
    patientCategory: undefined,
    discountType: 'PERCENT',
    discountValue: 0,
    maxDiscountAmount: undefined,
    maxDiscountPercent: undefined,
    requiresApproval: false,
    status: 'ACTIVE',
    validFrom: undefined,
    validTo: undefined,
  });

  const loadCorporates = useCallback(async () => {
    try {
      const res = await hospitalCorporateDiscountService.getCorporates({ page: 0, size: 500 });
      setCorporates(res.content);
    } catch {
      setCorporates([]);
    }
  }, []);

  const loadSchemes = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; code?: string; corporateClientId?: string; status?: string } = {
        page,
        size,
      };
      if (codeFilter.trim()) params.code = codeFilter.trim();
      if (corporateFilter.trim()) params.corporateClientId = corporateFilter.trim();
      if (statusFilter.trim()) params.status = statusFilter.trim();
      const response: PagedResponse<DiscountSchemeResponse> =
        await hospitalCorporateDiscountService.getDiscountSchemes(params);
      setSchemes(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load discount schemes', err);
      enqueueSnackbar('Failed to load discount schemes', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, codeFilter, corporateFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadCorporates();
  }, [loadCorporates]);

  useEffect(() => {
    loadSchemes();
  }, [loadSchemes]);

  const handleRefresh = () => {
    setPage(0);
    loadSchemes();
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setForm({
      code: '',
      name: '',
      corporateClientId: undefined,
      visitType: undefined,
      departmentId: undefined,
      serviceCode: undefined,
      patientCategory: undefined,
      discountType: 'PERCENT',
      discountValue: 0,
      maxDiscountAmount: undefined,
      maxDiscountPercent: undefined,
      requiresApproval: false,
      status: 'ACTIVE',
      validFrom: undefined,
      validTo: undefined,
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (row: DiscountSchemeResponse) => {
    setEditingId(row.id);
    setForm({
      code: row.code,
      name: row.name,
      corporateClientId: row.corporateClientId ?? undefined,
      visitType: row.visitType ?? undefined,
      departmentId: row.departmentId ?? undefined,
      serviceCode: row.serviceCode ?? undefined,
      patientCategory: row.patientCategory ?? undefined,
      discountType: row.discountType,
      discountValue: row.discountValue,
      maxDiscountAmount: row.maxDiscountAmount ?? undefined,
      maxDiscountPercent: row.maxDiscountPercent ?? undefined,
      requiresApproval: row.requiresApproval ?? false,
      status: row.status ?? 'ACTIVE',
      validFrom: row.validFrom ?? undefined,
      validTo: row.validTo ?? undefined,
    });
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingId(null);
  };

  const handleSave = async () => {
    if (!form.code?.trim() || !form.name?.trim()) {
      enqueueSnackbar('Code and name are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      if (editingId) {
        await hospitalCorporateDiscountService.updateDiscountScheme(editingId, form as UpdateDiscountSchemeRequest);
        enqueueSnackbar('Discount scheme updated', { variant: 'success' });
      } else {
        await hospitalCorporateDiscountService.createDiscountScheme(form);
        enqueueSnackbar('Discount scheme created', { variant: 'success' });
      }
      handleCloseDialog();
      loadSchemes();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to save'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleView = (row: DiscountSchemeResponse) => {
    setViewingScheme(row);
    setViewDialogOpen(true);
  };

  const handleManageApprovalLevels = (schemeId: string) => {
    navigate(`/hospital/corporate-discount/discount-schemes/${schemeId}`);
  };

  const handlePageChange = (_: unknown, newPage: number) => setPage(newPage);
  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  const corporateLabel = (id: string | null | undefined) => {
    if (!id) return '–';
    const c = corporates.find((x) => x.id === id);
    return c ? `${c.code} – ${c.name}` : id;
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4">Corporate &amp; Discount – Discount schemes</Typography>
          <Typography variant="body2">Manage discount schemes and approval levels</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Create scheme
        </Button>
      </Box>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="center" mb={2}>
            <TextField
              size="small"
              label="Code"
              value={codeFilter}
              onChange={(e) => setCodeFilter(e.target.value)}
              placeholder="Filter by code"
              sx={{ minWidth: 160 }}
            />
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Corporate</InputLabel>
              <Select value={corporateFilter} label="Corporate" onChange={(e) => setCorporateFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {corporates.map((c) => (
                  <MenuItem key={c.id} value={c.id}>{c.code} – {c.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button startIcon={<RefreshIcon />} onClick={handleRefresh}>Refresh</Button>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Code</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Corporate</TableCell>
                    <TableCell>Discount</TableCell>
                    <TableCell>Requires approval</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Valid from / to</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {schemes.length === 0 ? (
                    <TableRow><TableCell colSpan={8} align="center">No discount schemes found.</TableCell></TableRow>
                  ) : (
                    schemes.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.code}</TableCell>
                        <TableCell>{row.name}</TableCell>
                        <TableCell>{corporateLabel(row.corporateClientId)}</TableCell>
                        <TableCell>{row.discountType} {row.discountValue}{row.discountType === 'PERCENT' ? '%' : ''}</TableCell>
                        <TableCell>{row.requiresApproval ? 'Yes' : 'No'}</TableCell>
                        <TableCell>{row.status ?? '–'}</TableCell>
                        <TableCell>{row.validFrom ?? '–'} / {row.validTo ?? '–'}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>View</Button>
                          <Button size="small" startIcon={<EditIcon />} onClick={() => handleOpenEdit(row)}>Edit</Button>
                          <Button size="small" startIcon={<ApprovalLevelsIcon />} onClick={() => handleManageApprovalLevels(row.id)}>Manage approval levels</Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={handlePageChange}
            rowsPerPage={size}
            onRowsPerPageChange={handleRowsPerPageChange}
            rowsPerPageOptions={[10, 20, 50]}
            labelRowsPerPage="Rows per page:"
          />
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit discount scheme' : 'Create discount scheme'}</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField label="Code" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))} required fullWidth disabled={!!editingId} />
            <TextField label="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required fullWidth />
            <FormControl fullWidth>
              <InputLabel>Corporate (optional)</InputLabel>
              <Select value={form.corporateClientId ?? ''} label="Corporate (optional)" onChange={(e) => setForm((f) => ({ ...f, corporateClientId: e.target.value || undefined }))}>
                <MenuItem value="">None (general scheme)</MenuItem>
                {corporates.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} – {c.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Visit type (OP, IP, ED)" value={form.visitType ?? ''} onChange={(e) => setForm((f) => ({ ...f, visitType: e.target.value || undefined }))} fullWidth />
            <TextField label="Service code filter" value={form.serviceCode ?? ''} onChange={(e) => setForm((f) => ({ ...f, serviceCode: e.target.value || undefined }))} fullWidth />
            <TextField label="Patient category" value={form.patientCategory ?? ''} onChange={(e) => setForm((f) => ({ ...f, patientCategory: e.target.value || undefined }))} fullWidth />
            <FormControl fullWidth>
              <InputLabel>Discount type</InputLabel>
              <Select value={form.discountType} label="Discount type" onChange={(e) => setForm((f) => ({ ...f, discountType: e.target.value }))}>
                {DISCOUNT_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField type="number" label="Discount value" value={form.discountValue ?? ''} onChange={(e) => setForm((f) => ({ ...f, discountValue: parseFloat(e.target.value) || 0 }))} inputProps={{ min: 0, step: 0.01 }} fullWidth required />
            <TextField type="number" label="Max discount amount (cap)" value={form.maxDiscountAmount ?? ''} onChange={(e) => setForm((f) => ({ ...f, maxDiscountAmount: e.target.value ? parseFloat(e.target.value) : undefined }))} inputProps={{ min: 0, step: 0.01 }} fullWidth />
            <TextField type="number" label="Max discount % (cap)" value={form.maxDiscountPercent ?? ''} onChange={(e) => setForm((f) => ({ ...f, maxDiscountPercent: e.target.value ? parseFloat(e.target.value) : undefined }))} inputProps={{ min: 0, max: 100, step: 0.01 }} fullWidth />
            <FormControlLabel control={<Checkbox checked={form.requiresApproval ?? false} onChange={(e) => setForm((f) => ({ ...f, requiresApproval: e.target.checked }))} />} label="Requires approval" />
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select value={form.status ?? 'ACTIVE'} label="Status" onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}>
                {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField type="date" label="Valid from" value={form.validFrom ?? ''} onChange={(e) => setForm((f) => ({ ...f, validFrom: e.target.value || undefined }))} InputLabelProps={{ shrink: true }} fullWidth />
            <TextField type="date" label="Valid to" value={form.validTo ?? ''} onChange={(e) => setForm((f) => ({ ...f, validTo: e.target.value || undefined }))} InputLabelProps={{ shrink: true }} fullWidth />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>Save</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Discount scheme details</DialogTitle>
        <DialogContent>
          {viewingScheme && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="Code" value={viewingScheme.code} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Name" value={viewingScheme.name} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Corporate" value={corporateLabel(viewingScheme.corporateClientId)} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Visit type" value={viewingScheme.visitType ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Discount" value={`${viewingScheme.discountType} ${viewingScheme.discountValue}${viewingScheme.discountType === 'PERCENT' ? '%' : ''}`} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Max amount / %" value={[viewingScheme.maxDiscountAmount, viewingScheme.maxDiscountPercent].filter(Boolean).join(' / ') || '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Requires approval" value={viewingScheme.requiresApproval ? 'Yes' : 'No'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Status" value={viewingScheme.status ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid from / to" value={`${viewingScheme.validFrom ?? '–'} / ${viewingScheme.validTo ?? '–'}`} InputProps={{ readOnly: true }} fullWidth />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button variant="outlined" startIcon={<ApprovalLevelsIcon />} onClick={() => viewingScheme && (setViewDialogOpen(false), handleManageApprovalLevels(viewingScheme.id))}>Manage approval levels</Button>
          <Button variant="contained" onClick={() => viewingScheme && (setViewDialogOpen(false), handleOpenEdit(viewingScheme))}>Edit</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DiscountSchemesPage;
