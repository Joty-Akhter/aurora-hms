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
  Visibility as ViewIcon,
  Edit as EditIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreateReservationRequest,
  CreateMultiResourceReservationRequest,
  ResourceResponse,
  ReservationResponse,
  UpdateReservationStatusRequest,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const RESERVATION_STATUSES = [
  'TENTATIVE',
  'CONFIRMED',
  'CHECKED_IN',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
];

const SchedulingReservationsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [resourceIdFilter, setResourceIdFilter] = useState<string>('');
  const [patientIdFilter, setPatientIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [fromFilter, setFromFilter] = useState<string>('');
  const [toFilter, setToFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateReservationRequest & { additionalResourceIds?: string[] }>({
    resourceId: '',
    slotStart: '',
    slotEnd: '',
    status: 'CONFIRMED',
    patientId: '',
    idempotencyKey: '',
    referenceType: '',
    referenceId: '',
    additionalResourceIds: [],
  });

  const [viewDialogOpen, setViewDialogOpen] = useState<boolean>(false);
  const [viewReservation, setViewReservation] = useState<ReservationResponse | null>(null);

  const [statusDialogOpen, setStatusDialogOpen] = useState<boolean>(false);
  const [statusReservationId, setStatusReservationId] = useState<string | null>(null);
  const [statusForm, setStatusForm] = useState<UpdateReservationStatusRequest>({
    status: 'CONFIRMED',
    reason: '',
  });
  const [statusSubmitting, setStatusSubmitting] = useState<boolean>(false);

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

  const loadReservations = useCallback(async () => {
    try {
      setLoading(true);
      const params: {
        page: number;
        size: number;
        resourceId?: string;
        patientId?: string;
        status?: string;
        from?: string;
        to?: string;
      } = { page, size };
      if (resourceIdFilter.trim()) params.resourceId = resourceIdFilter.trim();
      if (patientIdFilter.trim()) params.patientId = patientIdFilter.trim();
      if (statusFilter) params.status = statusFilter;
      if (fromFilter.trim()) params.from = fromFilter.trim();
      if (toFilter.trim()) params.to = toFilter.trim();
      const response = await hospitalSchedulingService.getReservations(params);
      setReservations(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load reservations', err);
      enqueueSnackbar('Failed to load reservations', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, resourceIdFilter, patientIdFilter, statusFilter, fromFilter, toFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    loadReservations();
  }, [loadReservations]);

  const handleRefresh = () => {
    setPage(0);
    loadReservations();
  };

  const handleCreate = async () => {
    if (!createForm.resourceId?.trim()) {
      enqueueSnackbar('Resource is required', { variant: 'warning' });
      return;
    }
    if (!createForm.slotStart?.trim() || !createForm.slotEnd?.trim()) {
      enqueueSnackbar('Slot start and end are required', { variant: 'warning' });
      return;
    }
    const norm = (s: string) => {
      const t = s.trim();
      return t.length === 16 ? `${t}:00` : t;
    };
    const slotStart = norm(createForm.slotStart);
    const slotEnd = norm(createForm.slotEnd);
    const additionalIds = (createForm.additionalResourceIds ?? []).filter((id) => id && id !== createForm.resourceId);
    const isMulti = additionalIds.length > 0;
    const resourceIds = isMulti ? [createForm.resourceId.trim(), ...additionalIds] : [createForm.resourceId.trim()];

    try {
      setLoading(true);
      if (isMulti) {
        const payload: CreateMultiResourceReservationRequest = {
          resourceIds,
          slotStart,
          slotEnd,
          referenceType: createForm.referenceType?.trim() || undefined,
          referenceId: createForm.referenceId?.trim() || undefined,
          patientId: createForm.patientId?.trim() || undefined,
          idempotencyKey: createForm.idempotencyKey?.trim() || undefined,
        };
        const created = await hospitalSchedulingService.createMultiResourceReservation(payload);
        enqueueSnackbar(`${created.length} reservation(s) created (multi-resource)`, { variant: 'success' });
      } else {
        const payload: CreateReservationRequest = {
          resourceId: createForm.resourceId.trim(),
          slotStart,
          slotEnd,
          status: createForm.status || 'CONFIRMED',
          patientId: createForm.patientId?.trim() || undefined,
          idempotencyKey: createForm.idempotencyKey?.trim() || undefined,
          referenceType: createForm.referenceType?.trim() || undefined,
          referenceId: createForm.referenceId?.trim() || undefined,
        };
        await hospitalSchedulingService.createReservation(payload);
        enqueueSnackbar('Reservation created', { variant: 'success' });
      }
      setCreateDialogOpen(false);
      setCreateForm({
        resourceId: '',
        slotStart: '',
        slotEnd: '',
        status: 'CONFIRMED',
        patientId: '',
        idempotencyKey: '',
        referenceType: '',
        referenceId: '',
        additionalResourceIds: [],
      });
      loadReservations();
    } catch (err) {
      console.error('Failed to create reservation', err);
      enqueueSnackbar('Failed to create reservation', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openView = (r: ReservationResponse) => {
    setViewReservation(r);
    setViewDialogOpen(true);
  };

  const openUpdateStatus = (r: ReservationResponse) => {
    setStatusReservationId(r.id);
    setStatusForm({ status: r.status, reason: '' });
    setStatusDialogOpen(true);
  };

  const handleUpdateStatus = async () => {
    if (!statusReservationId) return;
    try {
      setStatusSubmitting(true);
      await hospitalSchedulingService.updateReservationStatus(statusReservationId, statusForm);
      enqueueSnackbar('Status updated', { variant: 'success' });
      setStatusDialogOpen(false);
      setStatusReservationId(null);
      loadReservations();
    } catch (err) {
      console.error('Failed to update status', err);
      enqueueSnackbar('Failed to update status', { variant: 'error' });
    } finally {
      setStatusSubmitting(false);
    }
  };

  const handleCancel = (r: ReservationResponse) => {
    setStatusReservationId(r.id);
    setStatusForm({ status: 'CANCELLED', reason: '' });
    setStatusDialogOpen(true);
  };

  const resourceMap = new Map(resources.map((r) => [r.id, r]));

  const formatDateTime = (s: string) => {
    if (!s) return '—';
    return s.slice(0, 19).replace('T', ' ');
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Reservations
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
            onClick={() => setCreateDialogOpen(true)}
            disabled={loading}
          >
            Create reservation
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={resourceIdFilter}
                onChange={(e) => setResourceIdFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Patient ID"
              size="small"
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
              placeholder="UUID"
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {RESERVATION_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="From"
              type="datetime-local"
              size="small"
              value={fromFilter}
              onChange={(e) => setFromFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To"
              type="datetime-local"
              size="small"
              value={toFilter}
              onChange={(e) => setToFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button variant="outlined" size="small" onClick={handleRefresh}>
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Reservations</Typography>
            <Typography variant="body2">Total: {totalElements}</Typography>
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
                    <TableCell>Resource</TableCell>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Slot start</TableCell>
                    <TableCell>Slot end</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Reference</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {reservations.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell>
                        {resourceMap.get(r.resourceId)?.name ?? r.resourceId}
                      </TableCell>
                      <TableCell>{r.patientId ?? '—'}</TableCell>
                      <TableCell>{formatDateTime(r.slotStart)}</TableCell>
                      <TableCell>{formatDateTime(r.slotEnd)}</TableCell>
                      <TableCell>{r.status}</TableCell>
                      <TableCell>
                        {r.referenceType && r.referenceId
                          ? `${r.referenceType}: ${r.referenceId}`
                          : '—'}
                      </TableCell>
                      <TableCell align="right">
                        <Button size="small" startIcon={<ViewIcon />} onClick={() => openView(r)}>
                          View
                        </Button>
                        {r.status !== 'CANCELLED' && r.status !== 'NO_SHOW' && (
                          <>
                            <Button
                              size="small"
                              startIcon={<EditIcon />}
                              onClick={() => openUpdateStatus(r)}
                            >
                              Update status
                            </Button>
                            <Button
                              size="small"
                              color="error"
                              startIcon={<CancelIcon />}
                              onClick={() => handleCancel(r)}
                            >
                              Cancel
                            </Button>
                          </>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {reservations.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No reservations found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create reservation dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create reservation</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small" required>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={createForm.resourceId}
                onChange={(e) => setCreateForm({ ...createForm, resourceId: e.target.value })}
              >
                <MenuItem value="">
                  <em>Select resource</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>Additional resources (multi-resource booking)</InputLabel>
              <Select
                label="Additional resources (multi-resource booking)"
                multiple
                value={createForm.additionalResourceIds ?? []}
                onChange={(e) =>
                  setCreateForm({
                    ...createForm,
                    additionalResourceIds: typeof e.target.value === 'string' ? [] : (e.target.value as string[]),
                  })
                }
                renderValue={(selected) => (selected as string[]).map((id) => resourceMap.get(id)?.name ?? id).join(', ') || 'None'}
              >
                {resources
                  .filter((r) => r.id !== createForm.resourceId)
                  .map((r) => (
                    <MenuItem key={r.id} value={r.id}>
                      {r.name} ({r.resourceType})
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
            <TextField
              label="Slot start"
              type="datetime-local"
              size="small"
              value={createForm.slotStart}
              onChange={(e) => setCreateForm({ ...createForm, slotStart: e.target.value })}
              InputLabelProps={{ shrink: true }}
              required
            />
            <TextField
              label="Slot end"
              type="datetime-local"
              size="small"
              value={createForm.slotEnd}
              onChange={(e) => setCreateForm({ ...createForm, slotEnd: e.target.value })}
              InputLabelProps={{ shrink: true }}
              required
            />
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={createForm.status || 'CONFIRMED'}
                onChange={(e) => setCreateForm({ ...createForm, status: e.target.value })}
              >
                {RESERVATION_STATUSES.filter((s) => s !== 'CANCELLED' && s !== 'NO_SHOW').map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Patient ID"
              size="small"
              value={createForm.patientId || ''}
              onChange={(e) => setCreateForm({ ...createForm, patientId: e.target.value })}
              placeholder="UUID"
            />
            <TextField
              label="Idempotency key (optional)"
              size="small"
              value={createForm.idempotencyKey || ''}
              onChange={(e) => setCreateForm({ ...createForm, idempotencyKey: e.target.value })}
            />
            <TextField
              label="Reference type (optional)"
              size="small"
              value={createForm.referenceType || ''}
              onChange={(e) => setCreateForm({ ...createForm, referenceType: e.target.value })}
              placeholder="APPOINTMENT, IPD_ADMISSION, etc."
            />
            <TextField
              label="Reference ID (optional)"
              size="small"
              value={createForm.referenceId || ''}
              onChange={(e) => setCreateForm({ ...createForm, referenceId: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={loading}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* View reservation dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reservation details</DialogTitle>
        <DialogContent>
          {viewReservation && (
            <Box display="flex" flexDirection="column" gap={1} pt={1}>
              <Typography variant="body2">
                <strong>ID:</strong> {viewReservation.id}
              </Typography>
              <Typography variant="body2">
                <strong>Resource:</strong>{' '}
                {resourceMap.get(viewReservation.resourceId)?.name ?? viewReservation.resourceId}
              </Typography>
              <Typography variant="body2">
                <strong>Slot start:</strong> {formatDateTime(viewReservation.slotStart)}
              </Typography>
              <Typography variant="body2">
                <strong>Slot end:</strong> {formatDateTime(viewReservation.slotEnd)}
              </Typography>
              <Typography variant="body2">
                <strong>Status:</strong> {viewReservation.status}
              </Typography>
              {viewReservation.patientId && (
                <Typography variant="body2">
                  <strong>Patient ID:</strong> {viewReservation.patientId}
                </Typography>
              )}
              {viewReservation.referenceType && (
                <Typography variant="body2">
                  <strong>Reference:</strong> {viewReservation.referenceType}
                  {viewReservation.referenceId && ` – ${viewReservation.referenceId}`}
                </Typography>
              )}
              {viewReservation.createdAt && (
                <Typography variant="body2" color="text.secondary">
                  Created: {viewReservation.createdAt.slice(0, 19).replace('T', ' ')}
                </Typography>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          {viewReservation && viewReservation.status !== 'CANCELLED' && viewReservation.status !== 'NO_SHOW' && (
            <Button
              variant="outlined"
              onClick={() => {
                setViewDialogOpen(false);
                openUpdateStatus(viewReservation);
              }}
            >
              Update status
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Update status dialog */}
      <Dialog
        open={statusDialogOpen}
        onClose={() => !statusSubmitting && setStatusDialogOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Update reservation status</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusForm.status}
                onChange={(e) => setStatusForm({ ...statusForm, status: e.target.value })}
              >
                {RESERVATION_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Reason (optional)"
              size="small"
              value={statusForm.reason || ''}
              onChange={(e) => setStatusForm({ ...statusForm, reason: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusDialogOpen(false)} disabled={statusSubmitting}>
            Cancel
          </Button>
          <Button variant="contained" onClick={handleUpdateStatus} disabled={statusSubmitting}>
            {statusSubmitting ? 'Updating…' : 'Update'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingReservationsPage;
