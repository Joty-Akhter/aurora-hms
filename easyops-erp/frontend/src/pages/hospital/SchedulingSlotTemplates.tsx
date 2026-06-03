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
import { Add as AddIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreateSlotTemplateRequest,
  SlotTemplateResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const RESOURCE_TYPES = ['', 'DOCTOR', 'ROOM', 'THEATRE', 'BED_GROUP', 'EQUIPMENT'];
const STATUSES = ['ACTIVE', 'INACTIVE'];

const SchedulingSlotTemplatesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [templates, setTemplates] = useState<SlotTemplateResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [resourceTypeFilter, setResourceTypeFilter] = useState<string>('');
  const [branchIdFilter, setBranchIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateSlotTemplateRequest>({
    name: '',
    resourceType: '',
    branchId: '',
    slotDurationMinutes: 15,
    slotsPerInterval: 1,
    startTime: '09:00',
    endTime: '17:00',
    leadTimeDays: 0,
    maxAdvanceDays: undefined,
    status: 'ACTIVE',
  });

  const loadTemplates = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; resourceType?: string; branchId?: string; status?: string } = {
        page,
        size,
      };
      if (resourceTypeFilter) params.resourceType = resourceTypeFilter;
      if (branchIdFilter.trim()) params.branchId = branchIdFilter.trim();
      if (statusFilter) params.status = statusFilter;
      const response = await hospitalSchedulingService.getSlotTemplates(params);
      setTemplates(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load slot templates', err);
      enqueueSnackbar('Failed to load slot templates', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, resourceTypeFilter, branchIdFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  const handleRefresh = () => {
    setPage(0);
    loadTemplates();
  };

  const handleCreate = async () => {
    if (!createForm.name?.trim()) {
      enqueueSnackbar('Name is required', { variant: 'warning' });
      return;
    }
    if (createForm.slotDurationMinutes < 1 || createForm.slotsPerInterval < 1) {
      enqueueSnackbar('Slot duration and slots per interval must be at least 1', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const payload: CreateSlotTemplateRequest = {
        ...createForm,
        resourceType: createForm.resourceType?.trim() || undefined,
        branchId: createForm.branchId?.trim() || undefined,
        leadTimeDays: createForm.leadTimeDays ?? 0,
        maxAdvanceDays: createForm.maxAdvanceDays ?? undefined,
        status: createForm.status || 'ACTIVE',
      };
      await hospitalSchedulingService.createSlotTemplate(payload);
      enqueueSnackbar('Slot template created', { variant: 'success' });
      setCreateDialogOpen(false);
      setCreateForm({
        name: '',
        resourceType: '',
        branchId: '',
        slotDurationMinutes: 15,
        slotsPerInterval: 1,
        startTime: '09:00',
        endTime: '17:00',
        leadTimeDays: 0,
        maxAdvanceDays: undefined,
        status: 'ACTIVE',
      });
      loadTemplates();
    } catch (err) {
      console.error('Failed to create slot template', err);
      enqueueSnackbar('Failed to create slot template', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Slot templates
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} disabled={loading}>
            Create slot template
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2}>
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Resource type</InputLabel>
              <Select
                label="Resource type"
                value={resourceTypeFilter}
                onChange={(e) => setResourceTypeFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {RESOURCE_TYPES.filter(Boolean).map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Branch ID"
              size="small"
              value={branchIdFilter}
              onChange={(e) => setBranchIdFilter(e.target.value)}
              placeholder="UUID"
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {STATUSES.map((s) => (
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
            <Typography variant="subtitle1">Slot templates</Typography>
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
                    <TableCell>Name</TableCell>
                    <TableCell>Resource type</TableCell>
                    <TableCell>Branch ID</TableCell>
                    <TableCell>Duration (min)</TableCell>
                    <TableCell>Slots/interval</TableCell>
                    <TableCell>Start</TableCell>
                    <TableCell>End</TableCell>
                    <TableCell>Lead days</TableCell>
                    <TableCell>Max advance days</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {templates.map((t) => (
                    <TableRow key={t.id}>
                      <TableCell>{t.name}</TableCell>
                      <TableCell>{t.resourceType ?? '—'}</TableCell>
                      <TableCell>{t.branchId ?? '—'}</TableCell>
                      <TableCell>{t.slotDurationMinutes}</TableCell>
                      <TableCell>{t.slotsPerInterval}</TableCell>
                      <TableCell>{String(t.startTime).slice(0, 5)}</TableCell>
                      <TableCell>{String(t.endTime).slice(0, 5)}</TableCell>
                      <TableCell>{t.leadTimeDays ?? 0}</TableCell>
                      <TableCell>{t.maxAdvanceDays ?? '—'}</TableCell>
                      <TableCell>{t.status}</TableCell>
                    </TableRow>
                  ))}
                  {templates.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        No slot templates found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create slot template dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create slot template</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Name"
              size="small"
              value={createForm.name}
              onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              required
              placeholder="e.g. OPD Morning 10min"
            />
            <FormControl fullWidth size="small">
              <InputLabel>Resource type</InputLabel>
              <Select
                label="Resource type"
                value={createForm.resourceType || ''}
                onChange={(e) => setCreateForm({ ...createForm, resourceType: e.target.value })}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {RESOURCE_TYPES.filter(Boolean).map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Branch ID"
              size="small"
              value={createForm.branchId || ''}
              onChange={(e) => setCreateForm({ ...createForm, branchId: e.target.value })}
            />
            <TextField
              label="Slot duration (minutes)"
              type="number"
              size="small"
              value={createForm.slotDurationMinutes}
              onChange={(e) =>
                setCreateForm({ ...createForm, slotDurationMinutes: Math.max(1, Number(e.target.value) || 1) })
              }
              inputProps={{ min: 1 }}
              required
            />
            <TextField
              label="Slots per interval (capacity)"
              type="number"
              size="small"
              value={createForm.slotsPerInterval}
              onChange={(e) =>
                setCreateForm({ ...createForm, slotsPerInterval: Math.max(1, Number(e.target.value) || 1) })
              }
              inputProps={{ min: 1 }}
              required
            />
            <TextField
              label="Start time"
              type="time"
              size="small"
              value={createForm.startTime}
              onChange={(e) => setCreateForm({ ...createForm, startTime: e.target.value })}
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 300 }}
            />
            <TextField
              label="End time"
              type="time"
              size="small"
              value={createForm.endTime}
              onChange={(e) => setCreateForm({ ...createForm, endTime: e.target.value })}
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 300 }}
            />
            <TextField
              label="Lead time (days)"
              type="number"
              size="small"
              value={createForm.leadTimeDays ?? 0}
              onChange={(e) => setCreateForm({ ...createForm, leadTimeDays: Math.max(0, Number(e.target.value) || 0) })}
              inputProps={{ min: 0 }}
            />
            <TextField
              label="Max advance (days)"
              type="number"
              size="small"
              value={createForm.maxAdvanceDays ?? ''}
              onChange={(e) => {
                const v = e.target.value;
                setCreateForm({ ...createForm, maxAdvanceDays: v === '' ? undefined : Math.max(0, Number(v) || 0) });
              }}
              inputProps={{ min: 0 }}
              placeholder="Optional"
            />
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={createForm.status || 'ACTIVE'}
                onChange={(e) => setCreateForm({ ...createForm, status: e.target.value })}
              >
                {STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={loading}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingSlotTemplatesPage;
