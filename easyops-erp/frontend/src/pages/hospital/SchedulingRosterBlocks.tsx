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
import { Add as AddIcon, Delete as DeleteIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreateRosterBlockRequest,
  ResourceResponse,
  RosterBlockResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const ROSTER_BLOCK_TYPES: Array<'AVAILABLE' | 'UNAVAILABLE' | 'SUBSTITUTE'> = [
  'AVAILABLE',
  'UNAVAILABLE',
  'SUBSTITUTE',
];

const SchedulingRosterBlocksPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [selectedResourceId, setSelectedResourceId] = useState<string>('');
  const [blocks, setBlocks] = useState<RosterBlockResponse[]>([]);
  const [fromFilter, setFromFilter] = useState<string>('');
  const [toFilter, setToFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateRosterBlockRequest & { startTimeLocal: string; endTimeLocal: string }>({
    startTime: '',
    endTime: '',
    type: 'UNAVAILABLE',
    substituteResourceId: undefined,
    startTimeLocal: '',
    endTimeLocal: '',
  });
  const [createSubmitting, setCreateSubmitting] = useState<boolean>(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<boolean>(false);

  const loadResources = useCallback(async () => {
    try {
      const response = await hospitalSchedulingService.getResources({ page: 0, size: 500, status: 'ACTIVE' });
      setResources(response.content);
    } catch {
      setResources([]);
    }
  }, []);

  const loadBlocks = useCallback(async () => {
    if (!selectedResourceId.trim()) {
      setBlocks([]);
      return;
    }
    try {
      setLoading(true);
      const params: { from?: string; to?: string } = {};
      if (fromFilter.trim()) params.from = fromFilter.trim();
      if (toFilter.trim()) params.to = toFilter.trim();
      const list = await hospitalSchedulingService.getRosterBlocks(selectedResourceId.trim(), params);
      setBlocks(list);
    } catch (err) {
      console.error('Failed to load roster blocks', err);
      enqueueSnackbar('Failed to load roster blocks', { variant: 'error' });
      setBlocks([]);
    } finally {
      setLoading(false);
    }
  }, [selectedResourceId, fromFilter, toFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    loadBlocks();
  }, [loadBlocks]);

  const handleRefresh = () => loadBlocks();

  const openCreateDialog = () => {
    const now = new Date();
    const start = new Date(now);
    start.setMinutes(0, 0, 0);
    const end = new Date(start);
    end.setHours(end.getHours() + 2, 0, 0, 0);
    const toLocal = (d: Date) => d.toISOString().slice(0, 16);
    setCreateForm({
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      type: 'UNAVAILABLE',
      substituteResourceId: undefined,
      startTimeLocal: toLocal(start),
      endTimeLocal: toLocal(end),
    });
    setCreateDialogOpen(true);
  };

  const handleCreate = async () => {
    if (!selectedResourceId.trim()) {
      enqueueSnackbar('Select a resource first', { variant: 'warning' });
      return;
    }
    const norm = (s: string) => {
      const t = s.trim();
      return t.length === 16 ? `${t}:00.000Z` : t;
    };
    const startIso = createForm.startTimeLocal ? new Date(createForm.startTimeLocal).toISOString() : createForm.startTime;
    const endIso = createForm.endTimeLocal ? new Date(createForm.endTimeLocal).toISOString() : createForm.endTime;
    if (!startIso || !endIso || new Date(endIso) <= new Date(startIso)) {
      enqueueSnackbar('End time must be after start time', { variant: 'warning' });
      return;
    }
    if (createForm.type === 'SUBSTITUTE' && !createForm.substituteResourceId?.trim()) {
      enqueueSnackbar('Substitute resource is required when type is SUBSTITUTE', { variant: 'warning' });
      return;
    }
    try {
      setCreateSubmitting(true);
      const payload: CreateRosterBlockRequest = {
        startTime: startIso,
        endTime: endIso,
        type: createForm.type,
        substituteResourceId: createForm.substituteResourceId?.trim() || undefined,
      };
      await hospitalSchedulingService.createRosterBlock(selectedResourceId.trim(), payload);
      enqueueSnackbar('Roster block created', { variant: 'success' });
      setCreateDialogOpen(false);
      loadBlocks();
    } catch (err) {
      console.error('Failed to create roster block', err);
      enqueueSnackbar('Failed to create roster block', { variant: 'error' });
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      setDeleting(true);
      await hospitalSchedulingService.deleteRosterBlock(id);
      enqueueSnackbar('Roster block deleted', { variant: 'success' });
      setDeleteConfirmId(null);
      loadBlocks();
    } catch (err) {
      console.error('Failed to delete roster block', err);
      enqueueSnackbar('Failed to delete roster block', { variant: 'error' });
    } finally {
      setDeleting(false);
    }
  };

  const resourceMap = new Map(resources.map((r) => [r.id, r]));

  const formatDateTime = (s: string) => (s ? s.slice(0, 19).replace('T', ' ') : '—');

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Roster blocks
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={openCreateDialog}
            disabled={loading || !selectedResourceId.trim()}
          >
            Add block
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Resource and filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 260 }}>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={selectedResourceId}
                onChange={(e) => setSelectedResourceId(e.target.value)}
              >
                <MenuItem value="">
                  <em>Select a resource</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="From (optional)"
              type="datetime-local"
              size="small"
              value={fromFilter}
              onChange={(e) => setFromFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To (optional)"
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
          <Typography variant="subtitle1" gutterBottom>
            Roster blocks
            {selectedResourceId && resourceMap.get(selectedResourceId) && (
              <> for {resourceMap.get(selectedResourceId)!.name}</>
            )}
          </Typography>
          {!selectedResourceId ? (
            <Typography color="text.secondary">Select a resource to view and manage roster blocks.</Typography>
          ) : loading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : blocks.length === 0 ? (
            <Typography color="text.secondary">No roster blocks. Add blocks to mark periods as AVAILABLE, UNAVAILABLE, or SUBSTITUTE.</Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Start</TableCell>
                    <TableCell>End</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Substitute</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {blocks.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell>{formatDateTime(b.startTime)}</TableCell>
                      <TableCell>{formatDateTime(b.endTime)}</TableCell>
                      <TableCell>{b.type}</TableCell>
                      <TableCell>{b.substituteResourceId ? resourceMap.get(b.substituteResourceId)?.name ?? b.substituteResourceId : '—'}</TableCell>
                      <TableCell align="right">
                        {deleteConfirmId === b.id ? (
                          <>
                            <Button size="small" onClick={() => setDeleteConfirmId(null)}>
                              Cancel
                            </Button>
                            <Button size="small" color="error" variant="contained" onClick={() => handleDelete(b.id)} disabled={deleting}>
                              {deleting ? 'Deleting…' : 'Confirm delete'}
                            </Button>
                          </>
                        ) : (
                          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteConfirmId(b.id)}>
                            Delete
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add roster block</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Start"
              type="datetime-local"
              value={createForm.startTimeLocal}
              onChange={(e) => setCreateForm((f) => ({ ...f, startTimeLocal: e.target.value }))}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="End"
              type="datetime-local"
              value={createForm.endTimeLocal}
              onChange={(e) => setCreateForm((f) => ({ ...f, endTimeLocal: e.target.value }))}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select
                label="Type"
                value={createForm.type}
                onChange={(e) => setCreateForm((f) => ({ ...f, type: e.target.value as CreateRosterBlockRequest['type'] }))}
              >
                {ROSTER_BLOCK_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {createForm.type === 'SUBSTITUTE' && (
              <FormControl fullWidth>
                <InputLabel>Substitute resource</InputLabel>
                <Select
                  label="Substitute resource"
                  value={createForm.substituteResourceId ?? ''}
                  onChange={(e) => setCreateForm((f) => ({ ...f, substituteResourceId: e.target.value || undefined }))}
                >
                  <MenuItem value="">
                    <em>Select</em>
                  </MenuItem>
                  {resources
                    .filter((r) => r.id !== selectedResourceId)
                    .map((r) => (
                      <MenuItem key={r.id} value={r.id}>
                        {r.name} ({r.resourceType})
                      </MenuItem>
                    ))}
                </Select>
              </FormControl>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={createSubmitting}>
            {createSubmitting ? 'Creating…' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingRosterBlocksPage;
