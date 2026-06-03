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
  TablePagination,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CorporateResponse,
  CreateCorporateRequest,
  PagedResponse,
  UpdateCorporateRequest,
} from '../../services/hospitalCorporateDiscountService';
import './Hospital.css';

const CORPORATE_TYPES = ['EMPLOYER', 'INSURER', 'TPA', 'GOVT_SCHEME', 'NGO', 'OTHER'];
const STATUSES = ['ACTIVE', 'INACTIVE'];

const CorporateClientsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(false);
  const [corporates, setCorporates] = useState<CorporateResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [codeFilter, setCodeFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewingCorporate, setViewingCorporate] = useState<CorporateResponse | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<CreateCorporateRequest>({
    name: '',
    code: '',
    type: 'EMPLOYER',
    status: 'ACTIVE',
    primaryContactName: '',
    primaryContactPhone: '',
    primaryContactEmail: '',
  });

  const loadCorporates = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; code?: string; type?: string; status?: string } = {
        page,
        size,
      };
      if (codeFilter.trim()) params.code = codeFilter.trim();
      if (typeFilter) params.type = typeFilter;
      if (statusFilter) params.status = statusFilter;
      const response: PagedResponse<CorporateResponse> = await hospitalCorporateDiscountService.getCorporates(params);
      setCorporates(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load corporates', err);
      enqueueSnackbar('Failed to load corporates', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, codeFilter, typeFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadCorporates();
  }, [loadCorporates]);

  const handleRefresh = () => {
    setPage(0);
    loadCorporates();
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setForm({
      name: '',
      code: '',
      type: 'EMPLOYER',
      status: 'ACTIVE',
      primaryContactName: '',
      primaryContactPhone: '',
      primaryContactEmail: '',
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (row: CorporateResponse) => {
    setEditingId(row.id);
    setForm({
      name: row.name,
      code: row.code,
      type: row.type,
      status: row.status,
      validFrom: row.validFrom ?? undefined,
      validTo: row.validTo ?? undefined,
      primaryContactName: row.primaryContactName ?? '',
      primaryContactPhone: row.primaryContactPhone ?? '',
      primaryContactEmail: row.primaryContactEmail ?? '',
    });
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingId(null);
  };

  const handleSave = async () => {
    if (!form.name?.trim() || !form.code?.trim() || !form.type?.trim()) {
      enqueueSnackbar('Name, code and type are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      if (editingId) {
        await hospitalCorporateDiscountService.updateCorporate(editingId, form as UpdateCorporateRequest);
        enqueueSnackbar('Corporate updated', { variant: 'success' });
      } else {
        await hospitalCorporateDiscountService.createCorporate(form);
        enqueueSnackbar('Corporate created', { variant: 'success' });
      }
      handleCloseDialog();
      loadCorporates();
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'response' in err && err.response && typeof (err.response as { data?: { message?: string } }).data?.message === 'string'
        ? (err.response as { data: { message: string } }).data.message
        : 'Failed to save';
      enqueueSnackbar(msg, { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleView = (row: CorporateResponse) => {
    setViewingCorporate(row);
    setViewDialogOpen(true);
  };

  const handleManageContracts = (corporateId: string) => {
    navigate(`/hospital/corporate-discount/contracts?corporateId=${corporateId}`);
  };

  const handlePageChange = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4">Corporate & Discount – Corporates</Typography>
          <Typography variant="body2">Manage corporate clients (employers, insurers, TPAs, etc.)</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Create corporate
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
              sx={{ minWidth: 140 }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Type</InputLabel>
              <Select value={typeFilter} label="Type" onChange={(e) => setTypeFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {CORPORATE_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
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
                    <TableCell>Type</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Valid from / to</TableCell>
                    <TableCell>Primary contact</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {corporates.length === 0 ? (
                    <TableRow><TableCell colSpan={7} align="center">No corporates found.</TableCell></TableRow>
                  ) : (
                    corporates.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.code}</TableCell>
                        <TableCell>{row.name}</TableCell>
                        <TableCell>{row.type}</TableCell>
                        <TableCell>{row.status}</TableCell>
                        <TableCell>{row.validFrom ?? '–'} / {row.validTo ?? '–'}</TableCell>
                        <TableCell>{row.primaryContactName || row.primaryContactEmail || '–'}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>View</Button>
                          <Button size="small" startIcon={<EditIcon />} onClick={() => handleOpenEdit(row)}>Edit</Button>
                          <Button size="small" onClick={() => handleManageContracts(row.id)}>Manage contracts</Button>
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
        <DialogTitle>{editingId ? 'Edit corporate' : 'Create corporate'}</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField label="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required fullWidth />
            <TextField label="Code" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))} required fullWidth disabled={!!editingId} />
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select value={form.type} label="Type" onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}>
                {CORPORATE_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select value={form.status ?? 'ACTIVE'} label="Status" onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}>
                {STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField type="date" label="Valid from" value={form.validFrom ?? ''} onChange={(e) => setForm((f) => ({ ...f, validFrom: e.target.value || undefined }))} InputLabelProps={{ shrink: true }} fullWidth />
            <TextField type="date" label="Valid to" value={form.validTo ?? ''} onChange={(e) => setForm((f) => ({ ...f, validTo: e.target.value || undefined }))} InputLabelProps={{ shrink: true }} fullWidth />
            <TextField label="Primary contact name" value={form.primaryContactName ?? ''} onChange={(e) => setForm((f) => ({ ...f, primaryContactName: e.target.value }))} fullWidth />
            <TextField label="Primary contact phone" value={form.primaryContactPhone ?? ''} onChange={(e) => setForm((f) => ({ ...f, primaryContactPhone: e.target.value }))} fullWidth />
            <TextField label="Primary contact email" value={form.primaryContactEmail ?? ''} onChange={(e) => setForm((f) => ({ ...f, primaryContactEmail: e.target.value }))} fullWidth />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>Save</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Corporate details</DialogTitle>
        <DialogContent>
          {viewingCorporate && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="Name" value={viewingCorporate.name} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Code" value={viewingCorporate.code} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Type" value={viewingCorporate.type} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Status" value={viewingCorporate.status} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid from" value={viewingCorporate.validFrom ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid to" value={viewingCorporate.validTo ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Primary contact name" value={viewingCorporate.primaryContactName ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Primary contact phone" value={viewingCorporate.primaryContactPhone ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Primary contact email" value={viewingCorporate.primaryContactEmail ?? '–'} InputProps={{ readOnly: true }} fullWidth />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button variant="contained" onClick={() => viewingCorporate && (setViewDialogOpen(false), handleOpenEdit(viewingCorporate))}>
            Edit
          </Button>
          <Button variant="outlined" onClick={() => viewingCorporate && (setViewDialogOpen(false), handleManageContracts(viewingCorporate.id))}>
            Manage contracts
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CorporateClientsPage;
