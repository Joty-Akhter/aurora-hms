import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, InputLabel, MenuItem, Paper, Select, Snackbar, Alert,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField,
  Typography, Tooltip, IconButton,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import { useAuth } from '../../contexts/AuthContext';
import {
  getBonuses, createBonus, approveBonus, rejectBonus, BonusDto,
} from '../../services/hrService';

const fmt = (n: number | string | undefined | null) =>
  n == null ? '—' : Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const statusColor = (s?: string): 'default' | 'warning' | 'success' | 'error' => {
  switch (s?.toLowerCase()) {
    case 'pending': return 'warning';
    case 'approved': return 'success';
    case 'rejected': return 'error';
    default: return 'default';
  }
};

const emptyForm = {
  employeeId: '',
  bonusType: '',
  amount: '',
  currency: 'BDT',
  bonusPeriod: '',
  paymentDate: '',
  description: '',
  isTaxable: true,
};

const BonusManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [bonuses, setBonuses] = useState<BonusDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' }>({
    open: false, msg: '', severity: 'success',
  });

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({ ...emptyForm });
  const [saving, setSaving] = useState(false);

  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<BonusDto | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!currentOrganizationId) return;
    setLoading(true);
    try {
      const res = await getBonuses(currentOrganizationId, statusFilter ? { status: statusFilter } : {});
      setBonuses(res.data);
    } catch {
      setSnack({ open: true, msg: 'Failed to load bonuses', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [currentOrganizationId, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    if (!currentOrganizationId) return;
    setSaving(true);
    try {
      await createBonus({
        ...form,
        amount: parseFloat(form.amount) || 0,
        organizationId: currentOrganizationId,
        isTaxable: form.isTaxable,
        paymentDate: form.paymentDate || undefined,
      });
      setCreateOpen(false);
      setForm({ ...emptyForm });
      setSnack({ open: true, msg: 'Bonus created', severity: 'success' });
      load();
    } catch {
      setSnack({ open: true, msg: 'Failed to create bonus', severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async (bonus: BonusDto) => {
    if (!user?.id || !bonus.bonusId) return;
    setActionLoading(bonus.bonusId);
    try {
      await approveBonus(bonus.bonusId, user.id);
      setSnack({ open: true, msg: 'Bonus approved', severity: 'success' });
      load();
    } catch {
      setSnack({ open: true, msg: 'Failed to approve bonus', severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const openReject = (bonus: BonusDto) => {
    setRejectTarget(bonus);
    setRejectReason('');
    setRejectOpen(true);
  };

  const handleReject = async () => {
    if (!user?.id || !rejectTarget?.bonusId) return;
    setActionLoading(rejectTarget.bonusId);
    try {
      await rejectBonus(rejectTarget.bonusId, user.id, rejectReason);
      setRejectOpen(false);
      setRejectTarget(null);
      setSnack({ open: true, msg: 'Bonus rejected', severity: 'success' });
      load();
    } catch {
      setSnack({ open: true, msg: 'Failed to reject bonus', severity: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const totalAmount = bonuses.reduce((s, b) => s + Number(b.amount || 0), 0);
  const pendingCount = bonuses.filter((b) => b.status === 'pending').length;
  const approvedCount = bonuses.filter((b) => b.status === 'approved').length;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={600}>Bonus Management</Typography>
          <Typography variant="body2" color="text.secondary">Create and manage employee bonuses</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New Bonus
        </Button>
      </Box>

      {/* Summary cards */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        {[
          { label: 'Total Amount', value: fmt(totalAmount), sub: `${bonuses.length} record(s)` },
          { label: 'Pending', value: String(pendingCount), sub: 'Awaiting approval' },
          { label: 'Approved', value: String(approvedCount), sub: 'Ready for payroll' },
        ].map((c) => (
          <Paper key={c.label} elevation={1} sx={{ p: 2, minWidth: 160, flex: '1 1 160px' }}>
            <Typography variant="caption" color="text.secondary">{c.label}</Typography>
            <Typography variant="h5" fontWeight={700}>{c.value}</Typography>
            <Typography variant="caption" color="text.secondary">{c.sub}</Typography>
          </Paper>
        ))}
      </Box>

      {/* Filters */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Status</InputLabel>
          <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="pending">Pending</MenuItem>
            <MenuItem value="approved">Approved</MenuItem>
            <MenuItem value="rejected">Rejected</MenuItem>
          </Select>
        </FormControl>
        <Button size="small" onClick={load}>Refresh</Button>
      </Box>

      {/* Table */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
      ) : (
        <TableContainer component={Paper} elevation={1}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Employee ID</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Period</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Payment Date</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Payroll Run</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {bonuses.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    No bonuses found.
                  </TableCell>
                </TableRow>
              ) : (
                bonuses.map((b) => (
                  <TableRow key={b.bonusId} hover>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                      {b.employeeId?.slice(0, 8)}…
                    </TableCell>
                    <TableCell>{b.bonusType}</TableCell>
                    <TableCell>{b.bonusPeriod || '—'}</TableCell>
                    <TableCell align="right">
                      {b.currency || 'BDT'} {fmt(b.amount)}
                    </TableCell>
                    <TableCell>{b.paymentDate || '—'}</TableCell>
                    <TableCell>
                      <Chip label={b.status || 'unknown'} color={statusColor(b.status)} size="small" />
                    </TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                      {b.payrollRunId ? (
                        <Tooltip title={b.payrollRunId}>
                          <span>{b.payrollRunId.slice(0, 8)}…</span>
                        </Tooltip>
                      ) : '—'}
                    </TableCell>
                    <TableCell>
                      {b.status === 'pending' && (
                        <Box sx={{ display: 'flex', gap: 0.5 }}>
                          <Tooltip title="Approve">
                            <span>
                              <IconButton
                                size="small"
                                color="success"
                                disabled={actionLoading === b.bonusId}
                                onClick={() => handleApprove(b)}
                              >
                                {actionLoading === b.bonusId ? <CircularProgress size={16} /> : <CheckIcon fontSize="small" />}
                              </IconButton>
                            </span>
                          </Tooltip>
                          <Tooltip title="Reject">
                            <span>
                              <IconButton
                                size="small"
                                color="error"
                                disabled={actionLoading === b.bonusId}
                                onClick={() => openReject(b)}
                              >
                                <CloseIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                        </Box>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Create bonus dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Bonus</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <TextField
            label="Employee ID"
            value={form.employeeId}
            onChange={(e) => setForm((f) => ({ ...f, employeeId: e.target.value }))}
            required
            size="small"
          />
          <TextField
            label="Bonus Type"
            value={form.bonusType}
            onChange={(e) => setForm((f) => ({ ...f, bonusType: e.target.value }))}
            required
            size="small"
            placeholder="e.g. Performance, Annual, Spot"
          />
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              label="Amount"
              type="number"
              value={form.amount}
              onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
              required
              size="small"
              sx={{ flex: 2 }}
            />
            <TextField
              label="Currency"
              value={form.currency}
              onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value }))}
              size="small"
              sx={{ flex: 1 }}
            />
          </Box>
          <TextField
            label="Bonus Period"
            value={form.bonusPeriod}
            onChange={(e) => setForm((f) => ({ ...f, bonusPeriod: e.target.value }))}
            size="small"
            placeholder="e.g. Q1-2026"
          />
          <TextField
            label="Payment Date"
            type="date"
            value={form.paymentDate}
            onChange={(e) => setForm((f) => ({ ...f, paymentDate: e.target.value }))}
            size="small"
            InputLabelProps={{ shrink: true }}
            helperText="Used to match against payroll run period"
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            size="small"
            multiline
            rows={2}
          />
          <FormControl size="small">
            <InputLabel>Taxable</InputLabel>
            <Select
              value={form.isTaxable ? 'true' : 'false'}
              label="Taxable"
              onChange={(e) => setForm((f) => ({ ...f, isTaxable: e.target.value === 'true' }))}
            >
              <MenuItem value="true">Yes</MenuItem>
              <MenuItem value="false">No</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={saving || !form.employeeId || !form.bonusType || !form.amount}
          >
            {saving ? <CircularProgress size={20} /> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject dialog */}
      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Reject Bonus</DialogTitle>
        <DialogContent>
          <TextField
            label="Rejection Reason"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            multiline
            rows={3}
            fullWidth
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleReject} disabled={!!actionLoading}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snack.severity} onClose={() => setSnack((s) => ({ ...s, open: false }))}>
          {snack.msg}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default BonusManagement;
