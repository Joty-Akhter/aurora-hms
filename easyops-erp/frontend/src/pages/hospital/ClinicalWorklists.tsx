import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
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
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalClinicalOrdersService, {
  PagedResponse,
  UpdateWorklistStatusRequest,
  WorklistItemDetailResponse,
} from '../../services/hospitalClinicalOrdersService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const WORKLIST_TYPES = ['LAB_SECTION', 'RADIOLOGY_ROOM', 'PROCEDURE_ROOM', 'MOBILE_COLLECTION'];
const STATUSES = ['QUEUED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD'];

const ClinicalWorklistsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<WorklistItemDetailResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(50);
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [facilityIdFilter, setFacilityIdFilter] = useState('');
  const [departmentIdFilter, setDepartmentIdFilter] = useState('');
  const [sectionFilter, setSectionFilter] = useState('');
  const [sortMode, setSortMode] = useState<'PRIORITY' | 'NEWEST'>('PRIORITY');
  const [assignItem, setAssignItem] = useState<WorklistItemDetailResponse | null>(null);
  const [assignUserId, setAssignUserId] = useState('');
  const [assignRole, setAssignRole] = useState('');
  const [statusItem, setStatusItem] = useState<WorklistItemDetailResponse | null>(null);
  const [newStatus, setNewStatus] = useState('');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadWorklists = async () => {
    try {
      setLoading(true);
      const params: {
        page: number;
        size: number;
        facilityId?: string;
        type?: string;
        status?: string;
        departmentId?: string;
        section?: string;
      } = { page, size };
      if (facilityIdFilter.trim()) params.facilityId = facilityIdFilter.trim();
      if (typeFilter) params.type = typeFilter;
      if (statusFilter) params.status = statusFilter;
      if (departmentIdFilter.trim()) params.departmentId = departmentIdFilter.trim();
      if (sectionFilter.trim()) params.section = sectionFilter.trim();
      const res: PagedResponse<WorklistItemDetailResponse> = await hospitalClinicalOrdersService.getWorklists(params);
      setItems(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Load worklists failed', err);
      enqueueSnackbar('Failed to load worklists', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorklists();
  }, [page, facilityIdFilter, typeFilter, statusFilter, departmentIdFilter, sectionFilter]);

  const displayItems = React.useMemo(() => {
    if (sortMode === 'NEWEST') {
      return [...items].sort((a, b) => {
        const aCreated = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bCreated = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bCreated - aCreated;
      });
    }
    // Default: backend already returns priority-first ordering
    return items;
  }, [items, sortMode]);

  const handleAssign = async () => {
    if (!assignItem) return;
    try {
      setSubmitting(true);
      await hospitalClinicalOrdersService.assignWorklistItem(assignItem.id, {
        assignedToUserId: assignUserId.trim() || undefined,
        assignedToRole: assignRole.trim() || undefined,
      });
      enqueueSnackbar('Assignment updated', { variant: 'success' });
      setAssignItem(null);
      setAssignUserId('');
      setAssignRole('');
      loadWorklists();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Assign failed'), { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!statusItem || !newStatus) return;
    try {
      setSubmitting(true);
      await hospitalClinicalOrdersService.updateWorklistStatus(statusItem.id, {
        status: newStatus,
        remarks: remarks.trim() || undefined,
      });
      enqueueSnackbar('Status updated', { variant: 'success' });
      setStatusItem(null);
      setNewStatus('');
      setRemarks('');
      loadWorklists();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Update failed'), { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Clinical Orders – Worklists</Typography>
          <Typography variant="body2" color="text.secondary">
            View and manage worklist items by type and status. Assign to user/role or update status (QUEUED → ASSIGNED → IN_PROGRESS → COMPLETED).
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadWorklists}>
          Refresh
        </Button>
      </Box>
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Filters</Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Worklist type</InputLabel>
              <Select value={typeFilter} label="Worklist type" onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}>
                <MenuItem value="">All</MenuItem>
                {WORKLIST_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select value={statusFilter} label="Status" onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
                <MenuItem value="">All</MenuItem>
                {STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              size="small"
              label="Facility ID"
              value={facilityIdFilter}
              onChange={(e) => { setFacilityIdFilter(e.target.value); setPage(0); }}
              placeholder="UUID"
              sx={{ width: 220 }}
            />
            <TextField
              size="small"
              label="Department ID"
              value={departmentIdFilter}
              onChange={(e) => { setDepartmentIdFilter(e.target.value); setPage(0); }}
              sx={{ width: 220 }}
            />
            <TextField
              size="small"
              label="Section (e.g. LAB_SECTION)"
              value={sectionFilter}
              onChange={(e) => { setSectionFilter(e.target.value); setPage(0); }}
              sx={{ width: 220 }}
            />
            <FormControl size="small" sx={{ minWidth: 200, ml: 'auto' }}>
              <InputLabel>Sort by</InputLabel>
              <Select
                value={sortMode}
                label="Sort by"
                onChange={(e) => setSortMode(e.target.value as 'PRIORITY' | 'NEWEST')}
              >
                <MenuItem value="PRIORITY">Priority (STAT, URGENT, ROUTINE)</MenuItem>
                <MenuItem value="NEWEST">Created time (newest first)</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>
      <Card>
        <CardContent>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Worklist item ID</TableCell>
                  <TableCell>Order ID</TableCell>
                  <TableCell>Patient ID</TableCell>
                  <TableCell>Visit ID</TableCell>
                  <TableCell>Order type</TableCell>
                  <TableCell>Item code</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Result</TableCell>
                  <TableCell>Assigned to</TableCell>
                  <TableCell>Scheduled time</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={11}>Loading…</TableCell></TableRow>
                ) : items.length === 0 ? (
                  <TableRow><TableCell colSpan={11} align="center">No worklist items found.</TableCell></TableRow>
                ) : (
                  displayItems.map((w) => (
                    <TableRow key={w.id}>
                      <TableCell>{w.id}</TableCell>
                      <TableCell>{w.orderId}</TableCell>
                      <TableCell>{w.patientId || '—'}</TableCell>
                      <TableCell>{w.visitId || '—'}</TableCell>
                      <TableCell>{w.order?.orderType || '—'}</TableCell>
                      <TableCell>{w.order?.itemCode || '—'}</TableCell>
                      <TableCell>{w.status}</TableCell>
                      <TableCell>
                        {w.order?.resultStatus ? (
                          <Chip size="small" label={w.order.resultStatus} color={w.order.resultStatus === 'FINAL' ? 'success' : 'default'} variant="outlined" />
                        ) : '—'}
                      </TableCell>
                      <TableCell>{w.assignedToUserId || w.assignedToRole || '—'}</TableCell>
                      <TableCell>{w.scheduledTime ? new Date(w.scheduledTime).toLocaleString() : '—'}</TableCell>
                      <TableCell>
                        <Button size="small" onClick={() => { setAssignItem(w); setAssignUserId(w.assignedToUserId || ''); setAssignRole(w.assignedToRole || ''); }}>
                          Assign
                        </Button>
                        <Button size="small" onClick={() => { setStatusItem(w); setNewStatus(w.status); setRemarks(w.remarks || ''); }} sx={{ ml: 0.5 }}>
                          Update status
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          {totalElements > size && (
            <Box sx={{ mt: 2 }}>
              Page {page + 1} (total {totalElements})
              <Button size="small" disabled={page === 0} onClick={() => setPage((p) => p - 1)} sx={{ ml: 1 }}>Previous</Button>
              <Button size="small" disabled={(page + 1) * size >= totalElements} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </Box>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!assignItem} onClose={() => !submitting && setAssignItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Assign worklist item</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Assign to a user (ID) and/or role. Leave blank to unassign.
          </Typography>
          <TextField fullWidth label="Assigned to user ID" value={assignUserId} onChange={(e) => setAssignUserId(e.target.value)} sx={{ mt: 2 }} />
          <TextField fullWidth label="Assigned to role" value={assignRole} onChange={(e) => setAssignRole(e.target.value)} sx={{ mt: 2 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssignItem(null)} disabled={submitting}>Cancel</Button>
          <Button onClick={handleAssign} variant="contained" disabled={submitting}>Save</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!statusItem} onClose={() => !submitting && setStatusItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Update status</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ mt: 1 }}>
            <InputLabel>Status</InputLabel>
            <Select value={newStatus} label="Status" onChange={(e) => setNewStatus(e.target.value)}>
              {STATUSES.map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField fullWidth label="Remarks" value={remarks} onChange={(e) => setRemarks(e.target.value)} sx={{ mt: 2 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusItem(null)} disabled={submitting}>Cancel</Button>
          <Button onClick={handleUpdateStatus} variant="contained" disabled={submitting || !newStatus}>
            {submitting ? 'Saving…' : 'Update status'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ClinicalWorklistsPage;
