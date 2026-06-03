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
  IconButton,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  FormControl,
  InputLabel,
  Chip,
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Edit as EditIcon,
  Search as SearchIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  DoctorResourceMappingResponse,
  CreateDoctorResourceMappingRequest,
  UpdateDoctorResourceMappingRequest,
  ResourceResponse,
  PagedResponse,
} from '../../services/hospitalSchedulingService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const STATUSES = ['ACTIVE', 'INACTIVE'];

const SchedulingDoctorMappingsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(false);
  const [mappings, setMappings] = useState<DoctorResourceMappingResponse[]>([]);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [page] = useState(0);
  const [size] = useState(20);
  const [totalElements, setTotalElements] = useState(0);

  // filters
  const [doctorUserIdFilter, setDoctorUserIdFilter] = useState('');
  const [resourceIdFilter, setResourceIdFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // resolve
  const [resolveInput, setResolveInput] = useState('');
  const [resolveResult, setResolveResult] = useState<DoctorResourceMappingResponse | null>(null);
  const [resolveLoading, setResolveLoading] = useState(false);

  // create dialog
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateDoctorResourceMappingRequest>({
    doctorUserId: '',
    resourceId: '',
    branchId: '',
    isPrimary: true,
    effectiveFrom: '',
    effectiveTo: '',
  });
  const [createLoading, setCreateLoading] = useState(false);

  // edit dialog
  const [editOpen, setEditOpen] = useState(false);
  const [editId, setEditId] = useState('');
  const [editForm, setEditForm] = useState<UpdateDoctorResourceMappingRequest>({});
  const [editLoading, setEditLoading] = useState(false);

  const loadResources = useCallback(async () => {
    try {
      const r = await hospitalSchedulingService.getResources({ page: 0, size: 500, status: 'ACTIVE' });
      setResources(r.content.filter((res) => res.resourceType === 'DOCTOR'));
    } catch {
      setResources([]);
    }
  }, []);

  const loadMappings = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { page, size };
      if (doctorUserIdFilter.trim()) params.doctorUserId = doctorUserIdFilter.trim();
      if (resourceIdFilter.trim()) params.resourceId = resourceIdFilter.trim();
      if (statusFilter.trim()) params.status = statusFilter.trim();
      const r = await hospitalSchedulingService.getDoctorResourceMappings(params);
      setMappings(r.content);
      setTotalElements(r.totalElements);
    } catch {
      enqueueSnackbar('Failed to load doctor resource mappings', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, doctorUserIdFilter, resourceIdFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
    loadMappings();
  }, [loadResources, loadMappings]);

  const handleCreate = async () => {
    if (!createForm.doctorUserId.trim()) { enqueueSnackbar('Doctor user ID is required', { variant: 'warning' }); return; }
    if (!createForm.resourceId.trim()) { enqueueSnackbar('Resource is required', { variant: 'warning' }); return; }
    setCreateLoading(true);
    try {
      await hospitalSchedulingService.createDoctorResourceMapping({
        ...createForm,
        branchId: createForm.branchId?.trim() || undefined,
        effectiveFrom: createForm.effectiveFrom?.trim() || undefined,
        effectiveTo: createForm.effectiveTo?.trim() || undefined,
      });
      enqueueSnackbar('Mapping created', { variant: 'success' });
      setCreateOpen(false);
      loadMappings();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to create mapping'), { variant: 'error' });
    } finally {
      setCreateLoading(false);
    }
  };

  const handleUpdate = async () => {
    setEditLoading(true);
    try {
      await hospitalSchedulingService.updateDoctorResourceMapping(editId, editForm);
      enqueueSnackbar('Mapping updated', { variant: 'success' });
      setEditOpen(false);
      loadMappings();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to update mapping'), { variant: 'error' });
    } finally {
      setEditLoading(false);
    }
  };

  const openEdit = (m: DoctorResourceMappingResponse) => {
    setEditId(m.id);
    setEditForm({ status: m.status, isPrimary: m.isPrimary, effectiveFrom: m.effectiveFrom ?? '', effectiveTo: m.effectiveTo ?? '' });
    setEditOpen(true);
  };

  const handleResolve = async () => {
    if (!resolveInput.trim()) { enqueueSnackbar('Enter a doctor user ID', { variant: 'warning' }); return; }
    setResolveLoading(true);
    try {
      const r = await hospitalSchedulingService.resolveDoctorResourceMapping(resolveInput.trim());
      setResolveResult(r);
    } catch (err: any) {
      const msg = ehrApiErrorMessage(err, 'Mapping not found');
      enqueueSnackbar(msg, { variant: 'error' });
      setResolveResult(null);
    } finally {
      setResolveLoading(false);
    }
  };

  const resourceName = (id: string) => resources.find((r) => r.id === id)?.name ?? id;

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">Scheduling – Doctor Resource Mappings</Typography>
        <Box display="flex" gap={1}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadMappings} disabled={loading}>Refresh</Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>Add Mapping</Button>
        </Box>
      </Box>

      {/* Resolve section */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Resolve mapping for a doctor user</Typography>
          <Box display="flex" gap={2} alignItems="flex-end" flexWrap="wrap">
            <TextField label="Doctor User ID (UUID)" size="small" value={resolveInput} onChange={(e) => setResolveInput(e.target.value)} sx={{ minWidth: 320 }} />
            <Button variant="outlined" startIcon={resolveLoading ? <CircularProgress size={18} /> : <SearchIcon />} onClick={handleResolve} disabled={resolveLoading}>Resolve</Button>
          </Box>
          {resolveResult && (
            <Box mt={2} p={2} bgcolor="success.50" borderRadius={1}>
              <Typography variant="body2"><b>Resolved:</b> resource={resourceName(resolveResult.resourceId)} | branch={resolveResult.branchId ?? 'any'} | primary={resolveResult.isPrimary ? 'yes' : 'no'} | status={resolveResult.status}</Typography>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="flex-end">
            <TextField label="Doctor User ID" size="small" value={doctorUserIdFilter} onChange={(e) => setDoctorUserIdFilter(e.target.value)} sx={{ minWidth: 280 }} />
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Resource</InputLabel>
              <Select label="Resource" value={resourceIdFilter} onChange={(e) => setResourceIdFilter(e.target.value)}>
                <MenuItem value=""><em>All</em></MenuItem>
                {resources.map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value=""><em>All</em></MenuItem>
                {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
            <Button variant="outlined" onClick={loadMappings}>Filter</Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Mappings ({totalElements})</Typography>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Doctor User ID</TableCell>
                    <TableCell>Resource</TableCell>
                    <TableCell>Branch ID</TableCell>
                    <TableCell>Primary</TableCell>
                    <TableCell>Effective From</TableCell>
                    <TableCell>Effective To</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {mappings.length === 0 ? (
                    <TableRow><TableCell colSpan={8}><Typography color="text.secondary" align="center">No mappings found.</Typography></TableCell></TableRow>
                  ) : mappings.map((m) => (
                    <TableRow key={m.id}>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>{m.doctorUserId}</TableCell>
                      <TableCell>{resourceName(m.resourceId)}</TableCell>
                      <TableCell>{m.branchId ?? '—'}</TableCell>
                      <TableCell><Chip label={m.isPrimary ? 'Yes' : 'No'} size="small" color={m.isPrimary ? 'primary' : 'default'} /></TableCell>
                      <TableCell>{m.effectiveFrom ?? '—'}</TableCell>
                      <TableCell>{m.effectiveTo ?? '—'}</TableCell>
                      <TableCell><Chip label={m.status} size="small" color={m.status === 'ACTIVE' ? 'success' : 'default'} /></TableCell>
                      <TableCell>
                        <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(m)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Doctor Resource Mapping</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField label="Doctor User ID (UUID)" size="small" required value={createForm.doctorUserId} onChange={(e) => setCreateForm((f) => ({ ...f, doctorUserId: e.target.value }))} />
            <FormControl size="small" required>
              <InputLabel>Resource (DOCTOR type)</InputLabel>
              <Select label="Resource (DOCTOR type)" value={createForm.resourceId} onChange={(e) => setCreateForm((f) => ({ ...f, resourceId: e.target.value }))}>
                <MenuItem value=""><em>Select</em></MenuItem>
                {resources.map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Branch ID (UUID, optional)" size="small" value={createForm.branchId ?? ''} onChange={(e) => setCreateForm((f) => ({ ...f, branchId: e.target.value }))} />
            <FormControl size="small">
              <InputLabel>Is Primary</InputLabel>
              <Select label="Is Primary" value={createForm.isPrimary ? 'true' : 'false'} onChange={(e) => setCreateForm((f) => ({ ...f, isPrimary: e.target.value === 'true' }))}>
                <MenuItem value="true">Yes</MenuItem>
                <MenuItem value="false">No</MenuItem>
              </Select>
            </FormControl>
            <TextField label="Effective From (YYYY-MM-DD)" size="small" type="date" value={createForm.effectiveFrom ?? ''} onChange={(e) => setCreateForm((f) => ({ ...f, effectiveFrom: e.target.value }))} InputLabelProps={{ shrink: true }} />
            <TextField label="Effective To (YYYY-MM-DD)" size="small" type="date" value={createForm.effectiveTo ?? ''} onChange={(e) => setCreateForm((f) => ({ ...f, effectiveTo: e.target.value }))} InputLabelProps={{ shrink: true }} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={createLoading}>{createLoading ? 'Saving…' : 'Create'}</Button>
        </DialogActions>
      </Dialog>

      {/* Edit dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Update Mapping</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <FormControl size="small">
              <InputLabel>Status</InputLabel>
              <Select label="Status" value={editForm.status ?? 'ACTIVE'} onChange={(e) => setEditForm((f) => ({ ...f, status: e.target.value }))}>
                {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl size="small">
              <InputLabel>Is Primary</InputLabel>
              <Select label="Is Primary" value={editForm.isPrimary ? 'true' : 'false'} onChange={(e) => setEditForm((f) => ({ ...f, isPrimary: e.target.value === 'true' }))}>
                <MenuItem value="true">Yes</MenuItem>
                <MenuItem value="false">No</MenuItem>
              </Select>
            </FormControl>
            <TextField label="Effective From" size="small" type="date" value={editForm.effectiveFrom ?? ''} onChange={(e) => setEditForm((f) => ({ ...f, effectiveFrom: e.target.value }))} InputLabelProps={{ shrink: true }} />
            <TextField label="Effective To" size="small" type="date" value={editForm.effectiveTo ?? ''} onChange={(e) => setEditForm((f) => ({ ...f, effectiveTo: e.target.value }))} InputLabelProps={{ shrink: true }} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleUpdate} disabled={editLoading}>{editLoading ? 'Saving…' : 'Update'}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingDoctorMappingsPage;
