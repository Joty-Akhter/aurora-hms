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
  TextField,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Event as ReserveIcon,
  CheckCircle as ConvertIcon,
  Schedule as ExpireIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreatePlannedAdmissionRequest,
  PlannedAdmissionResponse,
  ResourceResponse,
  UpdatePlannedAdmissionStatusRequest,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const PLANNED_STATUSES = ['REQUESTED', 'RESERVED', 'CONVERTED', 'EXPIRED', 'CANCELLED'];

const SchedulingPlannedAdmissionsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [list, setList] = useState<PlannedAdmissionResponse[]>([]);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [patientIdFilter, setPatientIdFilter] = useState<string>('');
  const [fromDateFilter, setFromDateFilter] = useState<string>('');
  const [toDateFilter, setToDateFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreatePlannedAdmissionRequest>({
    patientId: '',
    preferredDate: new Date().toISOString().slice(0, 10),
    preferredWardOrBedClass: '',
  });

  const [reserveDialogOpen, setReserveDialogOpen] = useState<boolean>(false);
  const [reserveId, setReserveId] = useState<string | null>(null);
  const [reserveBedGroupId, setReserveBedGroupId] = useState<string>('');
  const [reserveExpiresAt, setReserveExpiresAt] = useState<string>('');
  const [reserveSubmitting, setReserveSubmitting] = useState<boolean>(false);

  const loadResources = useCallback(async () => {
    try {
      const response = await hospitalSchedulingService.getResources({
        page: 0,
        size: 500,
        status: 'ACTIVE',
      });
      setResources(response.content);
    } catch {
      setResources([]);
    }
  }, []);

  const loadList = useCallback(async () => {
    try {
      setLoading(true);
      const params: {
        page: number;
        size: number;
        patientId?: string;
        preferredDateFrom?: string;
        preferredDateTo?: string;
        status?: string;
      } = { page, size };
      if (patientIdFilter.trim()) params.patientId = patientIdFilter.trim();
      if (fromDateFilter.trim()) params.preferredDateFrom = fromDateFilter.trim();
      if (toDateFilter.trim()) params.preferredDateTo = toDateFilter.trim();
      if (statusFilter.trim()) params.status = statusFilter.trim();
      const response = await hospitalSchedulingService.getPlannedAdmissions(params);
      setList(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load planned admissions', err);
      enqueueSnackbar('Failed to load planned admissions', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, patientIdFilter, fromDateFilter, toDateFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const handleRefresh = () => {
    setPage(0);
    loadList();
  };

  const handleCreate = async () => {
    if (!createForm.patientId?.trim() || !createForm.preferredDate?.trim()) {
      enqueueSnackbar('Patient ID and preferred date are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalSchedulingService.createPlannedAdmission({
        patientId: createForm.patientId.trim(),
        preferredDate: createForm.preferredDate.trim(),
        preferredWardOrBedClass: createForm.preferredWardOrBedClass?.trim() || undefined,
      });
      enqueueSnackbar('Planned admission created', { variant: 'success' });
      setCreateDialogOpen(false);
      setCreateForm({ patientId: '', preferredDate: new Date().toISOString().slice(0, 10), preferredWardOrBedClass: '' });
      loadList();
    } catch (err) {
      console.error('Failed to create planned admission', err);
      enqueueSnackbar('Failed to create planned admission', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openReserve = (p: PlannedAdmissionResponse) => {
    setReserveId(p.id);
    setReserveBedGroupId('');
    setReserveExpiresAt('');
    setReserveDialogOpen(true);
  };

  const handleReserve = async () => {
    if (!reserveId || !reserveBedGroupId.trim()) {
      enqueueSnackbar('Select a bed group resource', { variant: 'warning' });
      return;
    }
    try {
      setReserveSubmitting(true);
      const body: UpdatePlannedAdmissionStatusRequest = {
        status: 'RESERVED',
        bedGroupResourceId: reserveBedGroupId.trim(),
      };
      if (reserveExpiresAt.trim()) body.expiresAt = new Date(reserveExpiresAt.trim()).toISOString();
      await hospitalSchedulingService.updatePlannedAdmissionStatus(reserveId, body);
      enqueueSnackbar('Status set to Reserved', { variant: 'success' });
      setReserveDialogOpen(false);
      setReserveId(null);
      loadList();
    } catch (err) {
      console.error('Failed to reserve', err);
      enqueueSnackbar('Failed to reserve', { variant: 'error' });
    } finally {
      setReserveSubmitting(false);
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    try {
      setLoading(true);
      await hospitalSchedulingService.updatePlannedAdmissionStatus(id, { status });
      enqueueSnackbar(`Status set to ${status}`, { variant: 'success' });
      loadList();
    } catch (err) {
      console.error('Failed to update status', err);
      enqueueSnackbar('Failed to update status', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const bedGroupResources = resources.filter((r) => r.resourceType === 'BED_GROUP');
  const formatDateTime = (s: string | undefined) => (s ? s.slice(0, 19).replace('T', ' ') : '—');

  const canReserve = (status: string) => status === 'REQUESTED';
  const canConvert = (status: string) => status === 'RESERVED';
  const canExpireOrCancel = (status: string) => status === 'REQUESTED' || status === 'RESERVED';

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Planned admissions
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} disabled={loading}>
            Create planned admission
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>Filters</Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField label="Patient ID" size="small" value={patientIdFilter} onChange={(e) => setPatientIdFilter(e.target.value)} placeholder="UUID" />
            <TextField label="From date" type="date" size="small" value={fromDateFilter} onChange={(e) => setFromDateFilter(e.target.value)} InputLabelProps={{ shrink: true }} />
            <TextField label="To date" type="date" size="small" value={toDateFilter} onChange={(e) => setToDateFilter(e.target.value)} InputLabelProps={{ shrink: true }} />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value=""><em>Any</em></MenuItem>
                {PLANNED_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="outlined" size="small" onClick={handleRefresh}>Apply</Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Planned admissions</Typography>
            <Typography variant="body2">Total: {totalElements}</Typography>
          </Box>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Preferred date</TableCell>
                    <TableCell>Ward / bed class</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Expires at</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {list.map((p) => (
                    <TableRow key={p.id}>
                      <TableCell>{p.patientId ?? '—'}</TableCell>
                      <TableCell>{p.preferredDate}</TableCell>
                      <TableCell>{p.preferredWardOrBedClass ?? '—'}</TableCell>
                      <TableCell>{p.status}</TableCell>
                      <TableCell>{formatDateTime(p.expiresAt)}</TableCell>
                      <TableCell align="right">
                        {canReserve(p.status) && (
                          <Button size="small" startIcon={<ReserveIcon />} onClick={() => openReserve(p)}>Reserve</Button>
                        )}
                        {canConvert(p.status) && (
                          <Button size="small" startIcon={<ConvertIcon />} onClick={() => handleUpdateStatus(p.id, 'CONVERTED')}>Convert</Button>
                        )}
                        {canExpireOrCancel(p.status) && (
                          <>
                            <Button size="small" startIcon={<ExpireIcon />} onClick={() => handleUpdateStatus(p.id, 'EXPIRED')}>Expire</Button>
                            <Button size="small" color="error" startIcon={<CancelIcon />} onClick={() => handleUpdateStatus(p.id, 'CANCELLED')}>Cancel</Button>
                          </>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {list.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center">No planned admissions found.</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create planned admission</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField label="Patient ID" size="small" value={createForm.patientId} onChange={(e) => setCreateForm({ ...createForm, patientId: e.target.value })} required placeholder="UUID" />
            <TextField label="Preferred date" type="date" size="small" value={createForm.preferredDate} onChange={(e) => setCreateForm({ ...createForm, preferredDate: e.target.value })} InputLabelProps={{ shrink: true }} required />
            <TextField label="Preferred ward / bed class" size="small" value={createForm.preferredWardOrBedClass || ''} onChange={(e) => setCreateForm({ ...createForm, preferredWardOrBedClass: e.target.value })} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={loading}>Create</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={reserveDialogOpen} onClose={() => !reserveSubmitting && setReserveDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reserve (set status to RESERVED)</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small" required>
              <InputLabel>Bed group resource</InputLabel>
              <Select
                label="Bed group resource"
                value={reserveBedGroupId}
                onChange={(e) => setReserveBedGroupId(e.target.value)}
              >
                <MenuItem value=""><em>Select</em></MenuItem>
                {bedGroupResources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
                ))}
                {bedGroupResources.length === 0 && (
                  <MenuItem value="" disabled>No BED_GROUP resources</MenuItem>
                )}
              </Select>
            </FormControl>
            <TextField
              label="Expires at (optional)"
              type="datetime-local"
              size="small"
              value={reserveExpiresAt}
              onChange={(e) => setReserveExpiresAt(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReserveDialogOpen(false)} disabled={reserveSubmitting}>Cancel</Button>
          <Button variant="contained" onClick={handleReserve} disabled={reserveSubmitting || !reserveBedGroupId.trim()}>
            {reserveSubmitting ? 'Reserving…' : 'Reserve'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingPlannedAdmissionsPage;
