import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import communicationService, { CommunicationDelivery } from '@services/communicationService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';

const STATUS_OPTIONS = ['', 'QUEUED', 'RETRYING', 'SENT', 'FAILED', 'DLQ', 'SKIPPED'] as const;
const CHANNEL_OPTIONS = ['', 'SMS', 'EMAIL'] as const;

const statusColor = (status: string): 'success' | 'warning' | 'error' | 'default' => {
  if (status === 'SENT') return 'success';
  if (status === 'RETRYING' || status === 'QUEUED') return 'warning';
  if (status === 'FAILED' || status === 'DLQ') return 'error';
  return 'default';
};

const CommunicationDeliveries: React.FC = () => {
  const [deliveries, setDeliveries] = useState<CommunicationDelivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState('');
  const [channelFilter, setChannelFilter] = useState('');
  const [searchType, setSearchType] = useState<'eventId' | 'correlationId'>('correlationId');
  const [searchKey, setSearchKey] = useState('');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params: {
        status?: string;
        channel?: string;
        eventId?: string;
        correlationId?: string;
      } = {};

      if (searchKey.trim()) {
        params[searchType] = searchKey.trim();
      } else {
        if (statusFilter) params.status = statusFilter;
        if (channelFilter) params.channel = channelFilter;
      }

      const page = await communicationService.queryCommunicationDeliveries(params);
      setDeliveries(page.content);
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to load delivery log.'));
    } finally {
      setLoading(false);
    }
  }, [channelFilter, searchKey, searchType, statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 1 }}>
        Delivery Log
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Search and review outbound messages triggered by hospital events.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ mb: 2 }}>
        <TextField
          size="small"
          label="Search by"
          select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value as 'eventId' | 'correlationId')}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="correlationId">Correlation ID</MenuItem>
          <MenuItem value="eventId">Event ID</MenuItem>
        </TextField>
        <TextField
          size="small"
          label="Search value"
          value={searchKey}
          onChange={(e) => setSearchKey(e.target.value)}
          fullWidth
        />
        <TextField
          size="small"
          label="Status"
          select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          sx={{ minWidth: 140 }}
          disabled={!!searchKey.trim()}
        >
          {STATUS_OPTIONS.map((status) => (
            <MenuItem key={status || 'all'} value={status}>
              {status || 'All statuses'}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label="Channel"
          select
          value={channelFilter}
          onChange={(e) => setChannelFilter(e.target.value)}
          sx={{ minWidth: 120 }}
          disabled={!!searchKey.trim()}
        >
          {CHANNEL_OPTIONS.map((channel) => (
            <MenuItem key={channel || 'all'} value={channel}>
              {channel || 'All channels'}
            </MenuItem>
          ))}
        </TextField>
        <Button variant="contained" onClick={() => void load()} disabled={loading}>
          Search
        </Button>
      </Stack>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Status</TableCell>
                <TableCell>Event</TableCell>
                <TableCell>Recipient</TableCell>
                <TableCell>Channel</TableCell>
                <TableCell>Template</TableCell>
                <TableCell>Attempts</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {deliveries.map((row) => (
                <TableRow key={row.id} hover>
                  <TableCell>
                    <Chip label={row.status} size="small" color={statusColor(row.status)} />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{row.eventType}</Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      {row.correlationId}
                    </Typography>
                  </TableCell>
                  <TableCell>{row.recipient}</TableCell>
                  <TableCell>{row.channel}</TableCell>
                  <TableCell>
                    {row.templateKey ? `${row.templateKey} v${row.templateVersion}` : '—'}
                  </TableCell>
                  <TableCell>{row.attemptCount}</TableCell>
                  <TableCell>{new Date(row.createdAt).toLocaleString()}</TableCell>
                </TableRow>
              ))}
              {!deliveries.length && (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                      No deliveries match your filters.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default CommunicationDeliveries;
