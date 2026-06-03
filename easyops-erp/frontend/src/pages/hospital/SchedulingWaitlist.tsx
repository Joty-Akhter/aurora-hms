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
  Upgrade as PromoteIcon,
  Cancel as CancelIcon,
  Schedule as ExpireIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreateWaitlistEntryRequest,
  PromoteWaitlistRequest,
  ResourceResponse,
  UpdateWaitlistStatusRequest,
  WaitlistEntryResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const WAITLIST_STATUSES = ['PENDING', 'PROMOTED', 'CANCELLED', 'EXPIRED'];

const SchedulingWaitlistPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [list, setList] = useState<WaitlistEntryResponse[]>([]);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [resourceIdFilter, setResourceIdFilter] = useState<string>('');
  const [patientIdFilter, setPatientIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateWaitlistEntryRequest>({
    patientId: '',
    resourceId: '',
    preferredFromDate: '',
    preferredToDate: '',
    priority: 0,
    priorityReason: '',
  });

  const [promoteDialogOpen, setPromoteDialogOpen] = useState<boolean>(false);
  const [promoteForm, setPromoteForm] = useState<PromoteWaitlistRequest & { slotStartLocal: string; slotEndLocal: string }>({
    resourceId: '',
    slotStart: '',
    slotEnd: '',
    maxCandidates: 1,
    slotStartLocal: '',
    slotEndLocal: '',
  });
  const [promoteSubmitting, setPromoteSubmitting] = useState<boolean>(false);

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
      const params: { page: number; size: number; resourceId?: string; patientId?: string; status?: string } = {
        page,
        size,
      };
      if (resourceIdFilter.trim()) params.resourceId = resourceIdFilter.trim();
      if (patientIdFilter.trim()) params.patientId = patientIdFilter.trim();
      if (statusFilter.trim()) params.status = statusFilter.trim();
      const response = await hospitalSchedulingService.getWaitlist(params);
      setList(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load waitlist', err);
      enqueueSnackbar('Failed to load waitlist', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, resourceIdFilter, patientIdFilter, statusFilter, enqueueSnackbar]);

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
    if (!createForm.patientId?.trim() || !createForm.resourceId?.trim()) {
      enqueueSnackbar('Patient ID and resource are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalSchedulingService.createWaitlistEntry({
        patientId: createForm.patientId.trim(),
        resourceId: createForm.resourceId.trim(),
        preferredFromDate: createForm.preferredFromDate?.trim() || undefined,
        preferredToDate: createForm.preferredToDate?.trim() || undefined,
        priority: createForm.priority ?? 0,
        priorityReason: createForm.priorityReason?.trim() || undefined,
      });
      enqueueSnackbar('Waitlist entry added', { variant: 'success' });
      setCreateDialogOpen(false);
      setCreateForm({
        patientId: '',
        resourceId: '',
        preferredFromDate: '',
        preferredToDate: '',
        priority: 0,
        priorityReason: '',
      });
      loadList();
    } catch (err) {
      console.error('Failed to add waitlist entry', err);
      enqueueSnackbar('Failed to add waitlist entry', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    try {
      setLoading(true);
      const body: UpdateWaitlistStatusRequest = { status };
      await hospitalSchedulingService.updateWaitlistStatus(id, body);
      enqueueSnackbar(`Status set to ${status}`, { variant: 'success' });
      loadList();
    } catch (err) {
      console.error('Failed to update status', err);
      enqueueSnackbar('Failed to update status', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openPromoteDialog = () => {
    const start = new Date();
    start.setMinutes(0, 0, 0);
    const end = new Date(start);
    end.setHours(end.getHours() + 1, 0, 0, 0);
    const toLocal = (d: Date) => d.toISOString().slice(0, 16);
    setPromoteForm({
      resourceId: resourceIdFilter.trim() || '',
      slotStart: start.toISOString(),
      slotEnd: end.toISOString(),
      maxCandidates: 1,
      slotStartLocal: toLocal(start),
      slotEndLocal: toLocal(end),
    });
    setPromoteDialogOpen(true);
  };

  const handlePromote = async () => {
    if (!promoteForm.resourceId?.trim()) {
      enqueueSnackbar('Select a resource', { variant: 'warning' });
      return;
    }
    const slotStart = promoteForm.slotStartLocal
      ? new Date(promoteForm.slotStartLocal).toISOString()
      : promoteForm.slotStart;
    const slotEnd = promoteForm.slotEndLocal
      ? new Date(promoteForm.slotEndLocal).toISOString()
      : promoteForm.slotEnd;
    if (!slotStart || !slotEnd || new Date(slotEnd) <= new Date(slotStart)) {
      enqueueSnackbar('Slot end must be after slot start', { variant: 'warning' });
      return;
    }
    try {
      setPromoteSubmitting(true);
      const result = await hospitalSchedulingService.promoteWaitlist({
        resourceId: promoteForm.resourceId.trim(),
        slotStart,
        slotEnd,
        maxCandidates: promoteForm.maxCandidates ?? 1,
      });
      setPromoteDialogOpen(false);
      const contacted = result.candidatesContacted ?? 0;
      if (contacted > 0 && result.appointment) {
        enqueueSnackbar(
          `Promoted ${contacted} candidate(s). Appointment created: ${result.appointment.id}`,
          { variant: 'success' }
        );
      } else if (contacted > 0) {
        enqueueSnackbar(`Promoted ${contacted} candidate(s).`, { variant: 'success' });
      } else {
        enqueueSnackbar('No pending waitlist entries for this resource/slot.', { variant: 'info' });
      }
      loadList();
    } catch (err) {
      console.error('Failed to promote', err);
      enqueueSnackbar('Failed to promote from waitlist', { variant: 'error' });
    } finally {
      setPromoteSubmitting(false);
    }
  };

  const resourceMap = new Map(resources.map((r) => [r.id, r]));
  const canChangeStatus = (status: string) => status === 'PENDING';

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Waitlist
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button
            variant="outlined"
            startIcon={<PromoteIcon />}
            onClick={openPromoteDialog}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Promote for slot
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} disabled={loading}>
            Add entry
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 220 }}>
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
                    {r.name} ({r.resourceType})
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
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {WAITLIST_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="outlined" size="small" onClick={handleRefresh}>
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1">Waitlist entries</Typography>
            <Typography variant="body2">Total: {totalElements}</Typography>
          </Box>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Resource</TableCell>
                    <TableCell>Preferred from</TableCell>
                    <TableCell>Preferred to</TableCell>
                    <TableCell>Priority</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {list.map((w) => (
                    <TableRow key={w.id}>
                      <TableCell>{w.patientId ?? '—'}</TableCell>
                      <TableCell>{resourceMap.get(w.resourceId)?.name ?? w.resourceId}</TableCell>
                      <TableCell>{w.preferredFromDate ?? '—'}</TableCell>
                      <TableCell>{w.preferredToDate ?? '—'}</TableCell>
                      <TableCell>{w.priority}</TableCell>
                      <TableCell>{w.priorityReason ?? '—'}</TableCell>
                      <TableCell>{w.status}</TableCell>
                      <TableCell align="right">
                        {canChangeStatus(w.status) && (
                          <>
                            <Button
                              size="small"
                              startIcon={<ExpireIcon />}
                              onClick={() => handleUpdateStatus(w.id, 'EXPIRED')}
                            >
                              Expire
                            </Button>
                            <Button
                              size="small"
                              color="error"
                              startIcon={<CancelIcon />}
                              onClick={() => handleUpdateStatus(w.id, 'CANCELLED')}
                            >
                              Cancel
                            </Button>
                          </>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {list.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} align="center">
                        No waitlist entries found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add waitlist entry</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Patient ID"
              size="small"
              value={createForm.patientId}
              onChange={(e) => setCreateForm({ ...createForm, patientId: e.target.value })}
              required
              placeholder="UUID"
            />
            <FormControl fullWidth size="small" required>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={createForm.resourceId}
                onChange={(e) => setCreateForm({ ...createForm, resourceId: e.target.value })}
              >
                <MenuItem value="">
                  <em>Select</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Preferred from date"
              type="date"
              size="small"
              value={createForm.preferredFromDate || ''}
              onChange={(e) => setCreateForm({ ...createForm, preferredFromDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Preferred to date"
              type="date"
              size="small"
              value={createForm.preferredToDate || ''}
              onChange={(e) => setCreateForm({ ...createForm, preferredToDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Priority (number)"
              type="number"
              size="small"
              value={createForm.priority ?? 0}
              onChange={(e) => setCreateForm({ ...createForm, priority: parseInt(e.target.value, 10) || 0 })}
              inputProps={{ min: 0 }}
            />
            <TextField
              label="Priority reason"
              size="small"
              value={createForm.priorityReason || ''}
              onChange={(e) => setCreateForm({ ...createForm, priorityReason: e.target.value })}
              placeholder="EMERGENCY, VIP, FOLLOW_UP, ROUTINE"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={loading}>
            Add
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={promoteDialogOpen} onClose={() => !promoteSubmitting && setPromoteDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Promote from waitlist for slot</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small" required>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={promoteForm.resourceId}
                onChange={(e) => setPromoteForm({ ...promoteForm, resourceId: e.target.value })}
              >
                <MenuItem value="">
                  <em>Select</em>
                </MenuItem>
                {resources.map((r) => (
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
              value={promoteForm.slotStartLocal}
              onChange={(e) => setPromoteForm({ ...promoteForm, slotStartLocal: e.target.value })}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="Slot end"
              type="datetime-local"
              size="small"
              value={promoteForm.slotEndLocal}
              onChange={(e) => setPromoteForm({ ...promoteForm, slotEndLocal: e.target.value })}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="Max candidates"
              type="number"
              size="small"
              value={promoteForm.maxCandidates ?? 1}
              onChange={(e) =>
                setPromoteForm({ ...promoteForm, maxCandidates: parseInt(e.target.value, 10) || 1 })
              }
              inputProps={{ min: 1 }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPromoteDialogOpen(false)} disabled={promoteSubmitting}>
            Cancel
          </Button>
          <Button variant="contained" onClick={handlePromote} disabled={promoteSubmitting || !promoteForm.resourceId?.trim()}>
            {promoteSubmitting ? 'Promoting…' : 'Promote'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingWaitlistPage;
