import React, { useState, useCallback } from 'react';
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, IconButton, Paper, Snackbar,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Tooltip, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import SyncIcon from '@mui/icons-material/Sync';
import { useAuth } from '../../contexts/AuthContext';
import {
  deviceSync, processDeviceLogs, getDeviceLogs,
  DevicePunchDto, AttendanceRawLog, DeviceSyncResultDto,
} from '../../services/hrService';

const today = () => new Date().toISOString().slice(0, 10);

const emptyPunch = (): DevicePunchDto => ({
  employeeId: '',
  deviceId: '',
  punchTime: new Date().toISOString().slice(0, 16),
  punchType: 'UNKNOWN',
  rawEmployeeCode: '',
  notes: '',
});

const DeviceAttendanceSync: React.FC = () => {
  const { currentOrganizationId } = useAuth();

  // Raw log viewer state
  const [logsFrom, setLogsFrom] = useState(today());
  const [logsTo, setLogsTo] = useState(today());
  const [logs, setLogs] = useState<AttendanceRawLog[]>([]);
  const [logsLoading, setLogsLoading] = useState(false);

  // Process panel state
  const [processFrom, setProcessFrom] = useState(today());
  const [processTo, setProcessTo] = useState(today());
  const [stdHours, setStdHours] = useState('8');
  const [processing, setProcessing] = useState(false);
  const [processResult, setProcessResult] = useState<DeviceSyncResultDto | null>(null);

  // Ingest dialog state
  const [ingestOpen, setIngestOpen] = useState(false);
  const [punches, setPunches] = useState<DevicePunchDto[]>([emptyPunch()]);
  const [ingesting, setIngesting] = useState(false);
  const [ingestResult, setIngestResult] = useState<DeviceSyncResultDto | null>(null);

  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' }>({
    open: false, msg: '', severity: 'success',
  });

  const loadLogs = useCallback(async () => {
    if (!currentOrganizationId) return;
    setLogsLoading(true);
    try {
      const res = await getDeviceLogs(currentOrganizationId, {
        from: logsFrom + 'T00:00:00',
        to: logsTo + 'T23:59:59',
      });
      setLogs(res.data);
    } catch {
      setSnack({ open: true, msg: 'Failed to load device logs', severity: 'error' });
    } finally {
      setLogsLoading(false);
    }
  }, [currentOrganizationId, logsFrom, logsTo]);

  const handleProcess = async () => {
    if (!currentOrganizationId) return;
    setProcessing(true);
    setProcessResult(null);
    try {
      const res = await processDeviceLogs(
        currentOrganizationId,
        processFrom,
        processTo,
        parseFloat(stdHours) || 8
      );
      setProcessResult(res.data);
      setSnack({ open: true, msg: `Processed: ${res.data.attendanceRecordsAffected} attendance record(s) updated`, severity: 'success' });
    } catch {
      setSnack({ open: true, msg: 'Processing failed', severity: 'error' });
    } finally {
      setProcessing(false);
    }
  };

  const handleIngest = async () => {
    if (!currentOrganizationId) return;
    const valid = punches.filter((p) => p.punchTime && (p.employeeId || p.rawEmployeeCode));
    if (valid.length === 0) {
      setSnack({ open: true, msg: 'At least one punch needs punchTime and employeeId or rawEmployeeCode', severity: 'error' });
      return;
    }
    setIngesting(true);
    try {
      const res = await deviceSync(currentOrganizationId, valid);
      setIngestResult(res.data);
      setPunches([emptyPunch()]);
      setSnack({ open: true, msg: `Ingested ${res.data.ingested} punch(es)`, severity: 'success' });
    } catch {
      setSnack({ open: true, msg: 'Ingest failed', severity: 'error' });
    } finally {
      setIngesting(false);
    }
  };

  const updatePunch = (i: number, field: keyof DevicePunchDto, value: string) => {
    setPunches((prev) => prev.map((p, idx) => idx === i ? { ...p, [field]: value } : p));
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight={600} mb={1}>Device Attendance Sync</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Ingest raw biometric / access-control punch data and process it into attendance records
        that feed the payroll calculation.
      </Typography>

      {/* --- How it flows into payroll --- */}
      <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle2" fontWeight={600} mb={0.5}>How device attendance affects payroll</Typography>
        <Typography variant="body2" color="text.secondary">
          Device punches &rarr; <strong>Raw Logs</strong> (this page, phase 1) &rarr;
          <strong> Process</strong> (phase 2) &rarr; <strong>Attendance Records</strong> &rarr;
          <strong> Payroll Populate</strong> computes present days, LOP days, and overtime pay
          using the attendance rollup.
        </Typography>
      </Paper>

      {/* === Section 1: Ingest punches === */}
      <Paper elevation={1} sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle1" fontWeight={600}>Phase 1 — Ingest Device Punches</Typography>
          <Button
            variant="contained"
            startIcon={<CloudUploadIcon />}
            onClick={() => { setIngestOpen(true); setIngestResult(null); }}
          >
            Enter Punches
          </Button>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Submit raw punch events (clock-in / clock-out) from a biometric terminal or access control system.
          Each punch is saved as a raw log and is not yet reflected in attendance records.
        </Typography>
      </Paper>

      {/* === Section 2: Process === */}
      <Paper elevation={1} sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle1" fontWeight={600} mb={1}>Phase 2 — Process Raw Logs into Attendance</Typography>
        <Typography variant="body2" color="text.secondary" mb={2}>
          Pairs IN/OUT punches per employee per day, calculates total and overtime hours, then
          creates or updates attendance records. After processing, run "Populate from Salary" in
          Payroll to include the OT pay and LOP deductions in the next payroll run.
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <TextField
            label="From date"
            type="date"
            size="small"
            value={processFrom}
            onChange={(e) => setProcessFrom(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            label="To date"
            type="date"
            size="small"
            value={processTo}
            onChange={(e) => setProcessTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            label="Std hours/day"
            type="number"
            size="small"
            value={stdHours}
            onChange={(e) => setStdHours(e.target.value)}
            sx={{ width: 130 }}
          />
          <Button
            variant="contained"
            color="secondary"
            startIcon={processing ? <CircularProgress size={16} color="inherit" /> : <SyncIcon />}
            onClick={handleProcess}
            disabled={processing}
          >
            Process
          </Button>
        </Box>
        {processResult && (
          <Box sx={{ mt: 2 }}>
            <Alert severity={processResult.attendanceRecordsAffected > 0 ? 'success' : 'info'}>
              Attendance records updated: <strong>{processResult.attendanceRecordsAffected}</strong>
              {' | '}Skipped: <strong>{processResult.skipped}</strong>
            </Alert>
            {processResult.warnings && processResult.warnings.length > 0 && (
              <Box sx={{ mt: 1 }}>
                {processResult.warnings.map((w, i) => (
                  <Typography key={i} variant="caption" color="warning.main" display="block">⚠ {w}</Typography>
                ))}
              </Box>
            )}
          </Box>
        )}
      </Paper>

      {/* === Section 3: View raw logs === */}
      <Paper elevation={1} sx={{ p: 2 }}>
        <Typography variant="subtitle1" fontWeight={600} mb={1}>Raw Device Log Viewer</Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-end', mb: 2 }}>
          <TextField
            label="From date"
            type="date"
            size="small"
            value={logsFrom}
            onChange={(e) => setLogsFrom(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            label="To date"
            type="date"
            size="small"
            value={logsTo}
            onChange={(e) => setLogsTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <Button variant="outlined" onClick={loadLogs}>Load Logs</Button>
        </Box>

        {logsLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Punch Time</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Device</TableCell>
                  <TableCell>Raw Code</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Attendance Record</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {logs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      No device logs found for this range. Click "Load Logs" after selecting dates.
                    </TableCell>
                  </TableRow>
                ) : (
                  logs.map((l) => (
                    <TableRow key={l.rawLogId} hover>
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>
                        {l.punchTime ? new Date(l.punchTime).toLocaleString() : '—'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={l.punchType || 'UNKNOWN'}
                          size="small"
                          color={l.punchType === 'IN' ? 'success' : l.punchType === 'OUT' ? 'warning' : 'default'}
                        />
                      </TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {l.employeeId ? l.employeeId.slice(0, 8) + '…' : '—'}
                      </TableCell>
                      <TableCell>{l.deviceId || '—'}</TableCell>
                      <TableCell>{l.rawEmployeeCode || '—'}</TableCell>
                      <TableCell>{l.source || '—'}</TableCell>
                      <TableCell>
                        <Chip
                          label={l.processed ? 'Processed' : 'Pending'}
                          size="small"
                          color={l.processed ? 'success' : 'default'}
                        />
                      </TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {l.attendanceRecordId ? l.attendanceRecordId.slice(0, 8) + '…' : '—'}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>

      {/* === Ingest dialog === */}
      <Dialog open={ingestOpen} onClose={() => setIngestOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Enter Device Punches</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Enter one row per punch event. Provide either Employee ID (UUID) or Raw Employee Code
            (device's own code, matched against employee number).
          </Typography>

          {punches.map((p, i) => (
            <Box key={i} sx={{ display: 'flex', gap: 1, mb: 1, flexWrap: 'wrap', alignItems: 'center' }}>
              <TextField
                label="Punch Time"
                type="datetime-local"
                size="small"
                value={p.punchTime}
                onChange={(e) => updatePunch(i, 'punchTime', e.target.value)}
                InputLabelProps={{ shrink: true }}
                sx={{ width: 200 }}
                required
              />
              <TextField
                select
                label="Type"
                size="small"
                value={p.punchType || 'UNKNOWN'}
                onChange={(e) => updatePunch(i, 'punchType', e.target.value)}
                sx={{ width: 110 }}
                SelectProps={{ native: true }}
              >
                <option value="IN">IN</option>
                <option value="OUT">OUT</option>
                <option value="UNKNOWN">UNKNOWN</option>
              </TextField>
              <TextField
                label="Employee ID (UUID)"
                size="small"
                value={p.employeeId || ''}
                onChange={(e) => updatePunch(i, 'employeeId', e.target.value)}
                sx={{ flex: 2, minWidth: 120 }}
              />
              <TextField
                label="Raw Emp Code"
                size="small"
                value={p.rawEmployeeCode || ''}
                onChange={(e) => updatePunch(i, 'rawEmployeeCode', e.target.value)}
                sx={{ width: 130 }}
              />
              <TextField
                label="Device ID"
                size="small"
                value={p.deviceId || ''}
                onChange={(e) => updatePunch(i, 'deviceId', e.target.value)}
                sx={{ width: 120 }}
              />
              <Tooltip title="Remove row">
                <span>
                  <IconButton size="small" color="error" onClick={() => setPunches((prev) => prev.filter((_, j) => j !== i))} disabled={punches.length === 1}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
            </Box>
          ))}

          <Button
            size="small"
            startIcon={<AddIcon />}
            onClick={() => setPunches((prev) => [...prev, emptyPunch()])}
            sx={{ mt: 1 }}
          >
            Add row
          </Button>

          {ingestResult && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Ingested <strong>{ingestResult.ingested}</strong> punch(es).
              Skipped: {ingestResult.skipped}.
              {ingestResult.warnings?.length ? ' Warnings: ' + ingestResult.warnings.join('; ') : ''}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIngestOpen(false)}>Close</Button>
          <Button variant="contained" onClick={handleIngest} disabled={ingesting}>
            {ingesting ? <CircularProgress size={20} /> : 'Ingest Punches'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snack.open}
        autoHideDuration={5000}
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

export default DeviceAttendanceSync;
