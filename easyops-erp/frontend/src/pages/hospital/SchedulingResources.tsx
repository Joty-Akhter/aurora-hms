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
  Drawer,
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
import { Add as AddIcon, Refresh as RefreshIcon, Edit as EditIcon, Schedule as ScheduleIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CreateResourceRequest,
  ResourceResponse,
  UpdateResourceRequest,
  WorkingHoursResponse,
  SetWorkingHoursRequest,
  WorkingHoursEntryDto,
} from '../../services/hospitalSchedulingService';
import { useAuth } from '../../contexts/AuthContext';
import organizationService from '../../services/organizationService';
import { Department, Location } from '../../types/organization';
import './Hospital.css';

const RESOURCE_TYPES = ['DOCTOR', 'ROOM', 'THEATRE', 'BED_GROUP', 'EQUIPMENT'];
const STATUSES = ['ACTIVE', 'INACTIVE'];
const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const SchedulingResourcesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const { currentOrganizationId } = useAuth();

  const [loading, setLoading] = useState<boolean>(false);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [resourceTypeFilter, setResourceTypeFilter] = useState<string>('');
  const [branchIdFilter, setBranchIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateResourceRequest>({
    resourceType: 'DOCTOR',
    externalReferenceId: '',
    name: '',
    branchId: '',
    departmentId: '',
    metadata: '',
    status: 'ACTIVE',
  });

  const [editDialogOpen, setEditDialogOpen] = useState<boolean>(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<UpdateResourceRequest>({});

  const [workingHoursDrawerOpen, setWorkingHoursDrawerOpen] = useState<boolean>(false);
  const [workingHoursResourceId, setWorkingHoursResourceId] = useState<string | null>(null);
  const [workingHoursResourceName, setWorkingHoursResourceName] = useState<string>('');
  const [workingHoursList, setWorkingHoursList] = useState<WorkingHoursResponse[]>([]);
  const [workingHoursLoading, setWorkingHoursLoading] = useState<boolean>(false);
  const [setHoursForm, setSetHoursForm] = useState<WorkingHoursEntryDto[]>(
    DAY_NAMES.map((_, i) => ({ dayOfWeek: i, startTime: '09:00', endTime: '17:00' }))
  );
  const [setHoursSubmitting, setSetHoursSubmitting] = useState<boolean>(false);
  const [departmentsById, setDepartmentsById] = useState<Map<string, string>>(new Map());
  const [locationsById, setLocationsById] = useState<Map<string, string>>(new Map());

  const loadResources = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; resourceType?: string; branchId?: string; status?: string } = {
        page,
        size,
      };
      if (resourceTypeFilter) params.resourceType = resourceTypeFilter;
      if (branchIdFilter.trim()) params.branchId = branchIdFilter.trim();
      if (statusFilter) params.status = statusFilter;
      const response = await hospitalSchedulingService.getResources(params);
      setResources(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load resources', err);
      enqueueSnackbar('Failed to load resources', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, resourceTypeFilter, branchIdFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  useEffect(() => {
    if (!currentOrganizationId) {
      setDepartmentsById(new Map());
      setLocationsById(new Map());
      return;
    }
    const loadReferenceData = async () => {
      try {
        const [departments, locations] = await Promise.all([
          organizationService.getDepartments(currentOrganizationId),
          organizationService.getLocations(currentOrganizationId),
        ]);

        setDepartmentsById(
          new Map((departments as Department[]).map((d) => [d.id, d.name]))
        );
        setLocationsById(
          new Map((locations as Location[]).map((l) => [l.id, l.name]))
        );
      } catch (err) {
        console.warn('Failed to load branch/department names', err);
        setDepartmentsById(new Map());
        setLocationsById(new Map());
      }
    };
    loadReferenceData();
  }, [currentOrganizationId]);

  const formatRefName = (id?: string | null, map?: Map<string, string>) => {
    if (!id) return '—';
    const name = map?.get(id);
    return name ? `${name}` : id;
  };

  const handleRefresh = () => {
    setPage(0);
    loadResources();
  };

  const handleCreate = async () => {
    if (!createForm.resourceType || !createForm.externalReferenceId?.trim() || !createForm.name?.trim()) {
      enqueueSnackbar('Resource type, external reference ID and name are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const payload: CreateResourceRequest = {
        ...createForm,
        branchId: createForm.branchId?.trim() || undefined,
        departmentId: createForm.departmentId?.trim() || undefined,
        metadata: createForm.metadata?.trim() || undefined,
        status: createForm.status || 'ACTIVE',
      };
      await hospitalSchedulingService.createResource(payload);
      enqueueSnackbar('Resource created', { variant: 'success' });
      setCreateDialogOpen(false);
      setCreateForm({ resourceType: 'DOCTOR', externalReferenceId: '', name: '', status: 'ACTIVE' });
      loadResources();
    } catch (err) {
      console.error('Failed to create resource', err);
      enqueueSnackbar('Failed to create resource', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openEdit = (r: ResourceResponse) => {
    setEditId(r.id);
    setEditForm({
      resourceType: r.resourceType,
      externalReferenceId: r.externalReferenceId,
      name: r.name,
      branchId: r.branchId,
      departmentId: r.departmentId,
      metadata: r.metadata,
      status: r.status,
    });
    setEditDialogOpen(true);
  };

  const handleEdit = async () => {
    if (!editId) return;
    try {
      setLoading(true);
      await hospitalSchedulingService.updateResource(editId, editForm);
      enqueueSnackbar('Resource updated', { variant: 'success' });
      setEditDialogOpen(false);
      setEditId(null);
      loadResources();
    } catch (err) {
      console.error('Failed to update resource', err);
      enqueueSnackbar('Failed to update resource', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const openWorkingHours = async (r: ResourceResponse) => {
    setWorkingHoursResourceId(r.id);
    setWorkingHoursResourceName(r.name);
    setWorkingHoursDrawerOpen(true);
    setWorkingHoursList([]);
    try {
      setWorkingHoursLoading(true);
      const list = await hospitalSchedulingService.getWorkingHours(r.id);
      setWorkingHoursList(list);
      if (list.length > 0) {
        const entries: WorkingHoursEntryDto[] = DAY_NAMES.map((_, i) => {
          const existing = list.filter((wh) => wh.dayOfWeek === i)[0];
          if (existing) {
            const start = existing.startTime?.toString().slice(0, 5) ?? '09:00';
            const end = existing.endTime?.toString().slice(0, 5) ?? '17:00';
            return { dayOfWeek: i, startTime: start, endTime: end };
          }
          return { dayOfWeek: i, startTime: '09:00', endTime: '17:00' };
        });
        setSetHoursForm(entries);
      } else {
        setSetHoursForm(DAY_NAMES.map((_, i) => ({ dayOfWeek: i, startTime: '09:00', endTime: '17:00' })));
      }
    } catch (err) {
      console.error('Failed to load working hours', err);
      enqueueSnackbar('Failed to load working hours', { variant: 'error' });
    } finally {
      setWorkingHoursLoading(false);
    }
  };

  const handleSetWorkingHours = async () => {
    if (!workingHoursResourceId) return;
    try {
      setSetHoursSubmitting(true);
      const body: SetWorkingHoursRequest = { entries: setHoursForm };
      await hospitalSchedulingService.setWorkingHours(workingHoursResourceId, body);
      enqueueSnackbar('Working hours updated', { variant: 'success' });
      const list = await hospitalSchedulingService.getWorkingHours(workingHoursResourceId);
      setWorkingHoursList(list);
    } catch (err) {
      console.error('Failed to set working hours', err);
      enqueueSnackbar('Failed to set working hours', { variant: 'error' });
    } finally {
      setSetHoursSubmitting(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Resources
        </Typography>
        <Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading} sx={{ mr: 1 }}>
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} disabled={loading}>
            Create resource
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
                {RESOURCE_TYPES.map((t) => (
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
            <Typography variant="subtitle1">Resources</Typography>
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
                    <TableCell>Type</TableCell>
                    <TableCell>External ref</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Branch</TableCell>
                    <TableCell>Department</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {resources.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell>{r.resourceType}</TableCell>
                      <TableCell>{r.externalReferenceId}</TableCell>
                      <TableCell>{r.name}</TableCell>
                      <TableCell>{formatRefName(r.branchId, locationsById)}</TableCell>
                      <TableCell>{formatRefName(r.departmentId, departmentsById)}</TableCell>
                      <TableCell>{r.status}</TableCell>
                      <TableCell align="right">
                        <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(r)}>
                          Edit
                        </Button>
                        <Button size="small" startIcon={<ScheduleIcon />} onClick={() => openWorkingHours(r)}>
                          Working hours
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {resources.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No resources found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create resource dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create resource</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth size="small">
              <InputLabel>Resource type</InputLabel>
              <Select
                label="Resource type"
                value={createForm.resourceType}
                onChange={(e) => setCreateForm({ ...createForm, resourceType: e.target.value })}
              >
                {RESOURCE_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="External reference ID"
              size="small"
              value={createForm.externalReferenceId}
              onChange={(e) => setCreateForm({ ...createForm, externalReferenceId: e.target.value })}
              required
            />
            <TextField
              label="Name"
              size="small"
              value={createForm.name}
              onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              required
            />
            <TextField
              label="Branch ID"
              size="small"
              value={createForm.branchId || ''}
              onChange={(e) => setCreateForm({ ...createForm, branchId: e.target.value })}
            />
            <TextField
              label="Department ID"
              size="small"
              value={createForm.departmentId || ''}
              onChange={(e) => setCreateForm({ ...createForm, departmentId: e.target.value })}
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

      {/* Edit resource dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit resource</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="External reference ID"
              size="small"
              value={editForm.externalReferenceId || ''}
              onChange={(e) => setEditForm({ ...editForm, externalReferenceId: e.target.value })}
            />
            <TextField
              label="Name"
              size="small"
              value={editForm.name || ''}
              onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
            />
            <TextField
              label="Branch ID"
              size="small"
              value={editForm.branchId || ''}
              onChange={(e) => setEditForm({ ...editForm, branchId: e.target.value })}
            />
            <TextField
              label="Department ID"
              size="small"
              value={editForm.departmentId || ''}
              onChange={(e) => setEditForm({ ...editForm, departmentId: e.target.value })}
            />
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={editForm.status || ''}
                onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
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
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleEdit} disabled={loading}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Working hours drawer */}
      <Drawer
        anchor="right"
        open={workingHoursDrawerOpen}
        onClose={() => setWorkingHoursDrawerOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 420 } } }}
      >
        <Box p={2}>
          <Typography variant="h6" gutterBottom>
            Working hours – {workingHoursResourceName}
          </Typography>

          {workingHoursLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <>
              <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
                Current
              </Typography>
              {workingHoursList.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No working hours set.
                </Typography>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Day</TableCell>
                        <TableCell>Start</TableCell>
                        <TableCell>End</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {workingHoursList.map((wh) => (
                        <TableRow key={wh.id}>
                          <TableCell>{DAY_NAMES[wh.dayOfWeek] ?? wh.dayOfWeek}</TableCell>
                          <TableCell>{String(wh.startTime).slice(0, 5)}</TableCell>
                          <TableCell>{String(wh.endTime).slice(0, 5)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}

              <Typography variant="subtitle2" sx={{ mt: 3, mb: 1 }}>
                Set working hours (batch)
              </Typography>
              <Box display="flex" flexDirection="column" gap={1}>
                {setHoursForm.map((entry, idx) => (
                  <Box key={entry.dayOfWeek} display="flex" alignItems="center" gap={1}>
                    <Typography sx={{ width: 36 }}>{DAY_NAMES[entry.dayOfWeek]}</Typography>
                    <TextField
                      type="time"
                      size="small"
                      value={entry.startTime}
                      onChange={(e) => {
                        const next = [...setHoursForm];
                        next[idx] = { ...next[idx], startTime: e.target.value };
                        setSetHoursForm(next);
                      }}
                      InputLabelProps={{ shrink: true }}
                      inputProps={{ step: 300 }}
                    />
                    <TextField
                      type="time"
                      size="small"
                      value={entry.endTime}
                      onChange={(e) => {
                        const next = [...setHoursForm];
                        next[idx] = { ...next[idx], endTime: e.target.value };
                        setSetHoursForm(next);
                      }}
                      InputLabelProps={{ shrink: true }}
                      inputProps={{ step: 300 }}
                    />
                  </Box>
                ))}
              </Box>
              <Button
                variant="contained"
                fullWidth
                sx={{ mt: 2 }}
                onClick={handleSetWorkingHours}
                disabled={setHoursSubmitting}
              >
                {setHoursSubmitting ? 'Saving…' : 'Save working hours'}
              </Button>
            </>
          )}
        </Box>
      </Drawer>
    </Box>
  );
};

export default SchedulingResourcesPage;
