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
  BlackoutResponse,
  CreateBlackoutRequest,
  ResourceResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const SchedulingBlackoutsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [blackouts, setBlackouts] = useState<BlackoutResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [resourceIdFilter, setResourceIdFilter] = useState<string>('');
  const [branchIdFilter, setBranchIdFilter] = useState<string>('');
  const [fromDateFilter, setFromDateFilter] = useState<string>('');
  const [toDateFilter, setToDateFilter] = useState<string>('');

  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateBlackoutRequest & { scope: 'resource' | 'branch' }>({
    scope: 'resource',
    resourceId: '',
    branchId: '',
    blackoutDate: new Date().toISOString().slice(0, 10),
    reason: '',
  });
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

  const loadBlackouts = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; resourceId?: string; branchId?: string; fromDate?: string; toDate?: string } = {
        page,
        size,
      };
      if (resourceIdFilter.trim()) params.resourceId = resourceIdFilter.trim();
      if (branchIdFilter.trim()) params.branchId = branchIdFilter.trim();
      if (fromDateFilter.trim()) params.fromDate = fromDateFilter.trim();
      if (toDateFilter.trim()) params.toDate = toDateFilter.trim();
      const response = await hospitalSchedulingService.getBlackouts(params);
      setBlackouts(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load blackouts', err);
      enqueueSnackbar('Failed to load blackouts', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, resourceIdFilter, branchIdFilter, fromDateFilter, toDateFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    loadBlackouts();
  }, [loadBlackouts]);

  const handleRefresh = () => {
    setPage(0);
    loadBlackouts();
  };

  const handleCreate = async () => {
    const payload: CreateBlackoutRequest = {
      blackoutDate: createForm.blackoutDate,
      reason: createForm.reason?.trim() || undefined,
    };
    if (createForm.scope === 'resource') {
      if (!createForm.resourceId?.trim()) {
        enqueueSnackbar('Select a resource or choose branch scope', { variant: 'warning' });
        return;
      }
      payload.resourceId = createForm.resourceId.trim();
    } else {
      if (!createForm.branchId?.trim()) {
        enqueueSnackbar('Enter branch ID for branch blackout', { variant: 'warning' });
        return;
      }
      payload.branchId = createForm.branchId.trim();
    }
    try {
      setLoading(true);
      await hospitalSchedulingService.createBlackout(payload);
      enqueueSnackbar('Blackout created', { variant: 'success' });
      setCreateDialogOpen(false);
      setCreateForm({
        scope: 'resource',
        resourceId: '',
        branchId: '',
        blackoutDate: new Date().toISOString().slice(0, 10),
        reason: '',
      });
      loadBlackouts();
    } catch (err) {
      console.error('Failed to create blackout', err);
      enqueueSnackbar('Failed to create blackout', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      setDeleting(true);
      await hospitalSchedulingService.deleteBlackout(id);
      enqueueSnackbar('Blackout deleted', { variant: 'success' });
      setDeleteConfirmId(null);
      loadBlackouts();
    } catch (err) {
      console.error('Failed to delete blackout', err);
      enqueueSnackbar('Failed to delete blackout', { variant: 'error' });
    } finally {
      setDeleting(false);
    }
  };

  const resourceMap = new Map(resources.map((r) => [r.id, r]));

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Blackouts
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} disabled={loading}>
            Create blackout
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Resource ID"
              size="small"
              value={resourceIdFilter}
              onChange={(e) => setResourceIdFilter(e.target.value)}
              placeholder="UUID"
            />
            <TextField
              label="Branch ID"
              size="small"
              value={branchIdFilter}
              onChange={(e) => setBranchIdFilter(e.target.value)}
              placeholder="UUID"
            />
            <TextField
              label="From date"
              type="date"
              size="small"
              value={fromDateFilter}
              onChange={(e) => setFromDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To date"
              type="date"
              size="small"
              value={toDateFilter}
              onChange={(e) => setToDateFilter(e.target.value)}
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
            <Typography variant="subtitle1">Blackouts</Typography>
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
                    <TableCell>Resource / Branch</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {blackouts.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell>
                        {b.resourceId
                          ? resourceMap.get(b.resourceId)?.name ?? b.resourceId
                          : b.branchId
                            ? `Branch: ${b.branchId}`
                            : '—'}
                      </TableCell>
                      <TableCell>{b.blackoutDate}</TableCell>
                      <TableCell>{b.reason ?? '—'}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          color="error"
                          startIcon={<DeleteIcon />}
                          onClick={() => setDeleteConfirmId(b.id)}
                        >
                          Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {blackouts.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        No blackouts found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create blackout dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create blackout</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small">
              <InputLabel>Scope</InputLabel>
              <Select
                label="Scope"
                value={createForm.scope}
                onChange={(e) =>
                  setCreateForm({ ...createForm, scope: e.target.value as 'resource' | 'branch' })
                }
              >
                <MenuItem value="resource">Resource</MenuItem>
                <MenuItem value="branch">Branch</MenuItem>
              </Select>
            </FormControl>
            {createForm.scope === 'resource' ? (
              <FormControl fullWidth size="small">
                <InputLabel>Resource</InputLabel>
                <Select
                  label="Resource"
                  value={createForm.resourceId || ''}
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
            ) : (
              <TextField
                label="Branch ID"
                size="small"
                value={createForm.branchId || ''}
                onChange={(e) => setCreateForm({ ...createForm, branchId: e.target.value })}
                placeholder="UUID"
              />
            )}
            <TextField
              label="Blackout date"
              type="date"
              size="small"
              value={createForm.blackoutDate}
              onChange={(e) => setCreateForm({ ...createForm, blackoutDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
              required
            />
            <TextField
              label="Reason"
              size="small"
              value={createForm.reason || ''}
              onChange={(e) => setCreateForm({ ...createForm, reason: e.target.value })}
              placeholder="Holiday, training, etc."
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

      {/* Delete confirmation */}
      <Dialog open={!!deleteConfirmId} onClose={() => !deleting && setDeleteConfirmId(null)}>
        <DialogTitle>Delete blackout?</DialogTitle>
        <DialogActions>
          <Button onClick={() => setDeleteConfirmId(null)} disabled={deleting}>
            Cancel
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteConfirmId && handleDelete(deleteConfirmId)}
            disabled={deleting}
          >
            {deleting ? 'Deleting…' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulingBlackoutsPage;
