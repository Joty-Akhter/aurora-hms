import React, { useCallback, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  FormControl,
  IconButton,
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
  Tooltip,
  Typography,
} from '@mui/material';
import {
  Search as SearchIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  AuditLogResponse,
  PagedResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const ENTITY_TYPES = ['APPOINTMENT', 'RESERVATION', 'PLANNED_ADMISSION', 'WAITLIST_ENTRY'];
const ACTIONS = [
  'CREATED', 'RESCHEDULED', 'CANCELLED', 'CHECKED_IN', 'NO_SHOW',
  'STATUS_CHANGED', 'PROMOTED', 'CONVERTED', 'EXPIRED',
];

const SchedulingAuditLogPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(false);
  const [entries, setEntries] = useState<AuditLogResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page] = useState(0);
  const [size] = useState(20);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [entityIdFilter, setEntityIdFilter] = useState('');
  const [actorIdFilter, setActorIdFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');
  const [correlationIdFilter, setCorrelationIdFilter] = useState('');

  const handleSearch = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { page, size };
      if (entityTypeFilter) params.entityType = entityTypeFilter;
      if (entityIdFilter.trim()) params.entityId = entityIdFilter.trim();
      if (actorIdFilter.trim()) params.actorId = actorIdFilter.trim();
      if (actionFilter) params.action = actionFilter;
      if (fromDateFilter) params.fromDate = fromDateFilter + 'T00:00:00Z';
      if (toDateFilter) params.toDate = toDateFilter + 'T23:59:59Z';
      if (correlationIdFilter.trim()) params.correlationId = correlationIdFilter.trim();
      const r: PagedResponse<AuditLogResponse> = await hospitalSchedulingService.getAuditLog(params);
      setEntries(r.content);
      setTotalElements(r.totalElements);
    } catch {
      enqueueSnackbar('Failed to load audit log', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, entityTypeFilter, entityIdFilter, actorIdFilter, actionFilter, fromDateFilter, toDateFilter, correlationIdFilter, enqueueSnackbar]);

  const actionColor = (action: string): 'default' | 'error' | 'warning' | 'success' | 'info' => {
    if (['CANCELLED', 'EXPIRED'].includes(action)) return 'error';
    if (['NO_SHOW'].includes(action)) return 'warning';
    if (['CREATED', 'PROMOTED', 'CONVERTED'].includes(action)) return 'success';
    if (['CHECKED_IN', 'STATUS_CHANGED'].includes(action)) return 'info';
    return 'default';
  };

  const formatJson = (s?: string): string => {
    if (!s) return '—';
    try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s; }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">Scheduling – Audit Log</Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Search filters</Typography>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="flex-end">
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Entity Type</InputLabel>
              <Select label="Entity Type" value={entityTypeFilter} onChange={(e) => setEntityTypeFilter(e.target.value)}>
                <MenuItem value=""><em>All</em></MenuItem>
                {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Entity ID (UUID)" size="small" value={entityIdFilter} onChange={(e) => setEntityIdFilter(e.target.value)} sx={{ minWidth: 280 }} />
            <TextField label="Actor ID (UUID)" size="small" value={actorIdFilter} onChange={(e) => setActorIdFilter(e.target.value)} sx={{ minWidth: 280 }} />
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Action</InputLabel>
              <Select label="Action" value={actionFilter} onChange={(e) => setActionFilter(e.target.value)}>
                <MenuItem value=""><em>All</em></MenuItem>
                {ACTIONS.map((a) => <MenuItem key={a} value={a}>{a}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="From Date" size="small" type="date" value={fromDateFilter} onChange={(e) => setFromDateFilter(e.target.value)} InputLabelProps={{ shrink: true }} />
            <TextField label="To Date" size="small" type="date" value={toDateFilter} onChange={(e) => setToDateFilter(e.target.value)} InputLabelProps={{ shrink: true }} />
            <TextField label="Correlation ID" size="small" value={correlationIdFilter} onChange={(e) => setCorrelationIdFilter(e.target.value)} sx={{ minWidth: 240 }} />
            <Button variant="contained" startIcon={loading ? <CircularProgress size={18} /> : <SearchIcon />} onClick={handleSearch} disabled={loading}>
              {loading ? 'Searching…' : 'Search'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Results ({totalElements})</Typography>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : entries.length === 0 ? (
            <Typography color="text.secondary" align="center" py={3}>Run a search to see audit log entries.</Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Timestamp</TableCell>
                    <TableCell>Entity Type</TableCell>
                    <TableCell>Entity ID</TableCell>
                    <TableCell>Action</TableCell>
                    <TableCell>Actor</TableCell>
                    <TableCell>Channel</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell>Details</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {entries.map((e) => (
                    <React.Fragment key={e.id}>
                      <TableRow hover>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>{e.createdAt ? e.createdAt.slice(0, 19).replace('T', ' ') : '—'}</TableCell>
                        <TableCell>{e.entityType}</TableCell>
                        <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>{e.entityId}</TableCell>
                        <TableCell><Chip label={e.action} size="small" color={actionColor(e.action)} /></TableCell>
                        <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>{e.actorId ?? '—'}</TableCell>
                        <TableCell>{e.bookingChannel ?? '—'}</TableCell>
                        <TableCell>{e.reason ?? '—'}</TableCell>
                        <TableCell>
                          <Tooltip title={expandedId === e.id ? 'Collapse' : 'Expand before/after state'}>
                            <IconButton size="small" onClick={() => setExpandedId(expandedId === e.id ? null : e.id)}>
                              {expandedId === e.id ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell colSpan={8} sx={{ p: 0, border: 0 }}>
                          <Collapse in={expandedId === e.id} timeout="auto" unmountOnExit>
                            <Box display="flex" gap={2} p={2} bgcolor="grey.50">
                              <Box flex={1}>
                                <Typography variant="caption" color="text.secondary">Before state</Typography>
                                <Box component="pre" sx={{ fontSize: '0.7rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all', mt: 0.5, maxHeight: 200, overflow: 'auto', bgcolor: 'white', p: 1, borderRadius: 1 }}>
                                  {formatJson(e.beforeState)}
                                </Box>
                              </Box>
                              <Box flex={1}>
                                <Typography variant="caption" color="text.secondary">After state</Typography>
                                <Box component="pre" sx={{ fontSize: '0.7rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all', mt: 0.5, maxHeight: 200, overflow: 'auto', bgcolor: 'white', p: 1, borderRadius: 1 }}>
                                  {formatJson(e.afterState)}
                                </Box>
                              </Box>
                              {e.correlationId && (
                                <Box>
                                  <Typography variant="caption" color="text.secondary">Correlation ID</Typography>
                                  <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>{e.correlationId}</Typography>
                                </Box>
                              )}
                            </Box>
                          </Collapse>
                        </TableCell>
                      </TableRow>
                    </React.Fragment>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default SchedulingAuditLogPage;
