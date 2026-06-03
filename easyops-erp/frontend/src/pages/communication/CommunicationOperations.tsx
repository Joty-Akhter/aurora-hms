import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import communicationService, {
  CommunicationDelivery,
  OpsAlertStatusResponse,
  ProviderHealthResponse,
  ProviderSecretStatusResponse,
  TestSmsSendResponse,
} from '@services/communicationService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';

const RESENDABLE = new Set(['FAILED', 'DLQ', 'SKIPPED']);

const statusColor = (status: string): 'success' | 'warning' | 'error' | 'default' => {
  if (status === 'UP' || status === 'OK' || status === 'SENT') return 'success';
  if (status === 'WARN' || status === 'RETRYING') return 'warning';
  if (status === 'CRITICAL' || status === 'DOWN' || status === 'FAILED' || status === 'DLQ') return 'error';
  return 'default';
};

const CommunicationOperations: React.FC = () => {
  const [providerHealth, setProviderHealth] = useState<ProviderHealthResponse[]>([]);
  const [alerts, setAlerts] = useState<OpsAlertStatusResponse | null>(null);
  const [secrets, setSecrets] = useState<ProviderSecretStatusResponse | null>(null);
  const [deliveries, setDeliveries] = useState<CommunicationDelivery[]>([]);
  const [statusFilter, setStatusFilter] = useState('FAILED');
  const [channelFilter, setChannelFilter] = useState('');
  const [searchKey, setSearchKey] = useState('');
  const [searchType, setSearchType] = useState<'eventId' | 'correlationId'>('eventId');
  const [smsRecipient, setSmsRecipient] = useState('');
  const [smsResult, setSmsResult] = useState<TestSmsSendResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const queryParams = useMemo(() => {
    const base: { eventId?: string; correlationId?: string; status?: string; channel?: string } = {};
    if (searchKey.trim()) {
      base[searchType] = searchKey.trim();
    } else {
      base.status = statusFilter || undefined;
      base.channel = channelFilter || undefined;
    }
    return base;
  }, [channelFilter, searchKey, searchType, statusFilter]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [health, alertStatus, secretStatus, page] = await Promise.all([
        communicationService.getProviderHealth(),
        communicationService.getOpsAlerts(),
        communicationService.getProviderSecretsStatus(),
        communicationService.queryCommunicationDeliveries(queryParams),
      ]);
      setProviderHealth(health);
      setAlerts(alertStatus);
      setSecrets(secretStatus);
      setDeliveries(page.content);
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to load operations data.'));
    } finally {
      setLoading(false);
    }
  }, [queryParams]);

  useEffect(() => {
    void load();
  }, [load]);

  const resend = async (delivery: CommunicationDelivery) => {
    try {
      await communicationService.resendCommunicationDelivery(delivery.id, 'manual_resend_from_ops');
      await load();
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to request resend.'));
    }
  };

  const sendTestSms = async () => {
    try {
      setSmsResult(null);
      const result = await communicationService.sendTestSms(smsRecipient.trim());
      setSmsResult(result);
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'SMS test failed.'));
    }
  };

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h4">Operations</Typography>
        <Typography variant="body2" color="text.secondary">
          Monitor alerts, verify provider credentials, resend failed messages, and run connectivity tests.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Alerts
              </Typography>
              {alerts?.alerts.map((alert) => (
                <Box
                  key={alert.key}
                  sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}
                >
                  <Typography variant="body2">{alert.message}</Typography>
                  <Chip label={alert.level} size="small" color={statusColor(alert.level)} />
                </Box>
              )) ?? (
                <Typography variant="body2" color="text.secondary">
                  No alert data available.
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Provider Credentials
              </Typography>
              <Stack spacing={1}>
                {secrets?.providers.map((item) => (
                  <Box
                    key={`${item.provider}-${item.channel}`}
                    sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                  >
                    <Typography variant="body2">
                      {item.provider} ({item.channel})
                    </Typography>
                    <Chip
                      label={item.credentialsConfigured ? 'Configured' : 'Missing'}
                      size="small"
                      color={item.credentialsConfigured ? 'success' : 'error'}
                    />
                  </Box>
                )) ?? (
                  <Typography variant="body2" color="text.secondary">
                    Credential status unavailable.
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Provider Health
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {providerHealth.map((provider) => (
                  <Chip
                    key={`${provider.provider}-${provider.channel}`}
                    label={`${provider.provider} (${provider.channel}) — ${provider.status}`}
                    color={statusColor(provider.status)}
                  />
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Direct SMS Test
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <TextField
                  size="small"
                  label="Phone number"
                  value={smsRecipient}
                  onChange={(e) => setSmsRecipient(e.target.value)}
                  fullWidth
                />
                <Button variant="contained" onClick={() => void sendTestSms()} disabled={!smsRecipient.trim()}>
                  Send Test SMS
                </Button>
              </Stack>
              {smsResult && (
                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                  <Chip label={`Provider: ${smsResult.provider}`} />
                  <Chip color="success" label={`Status: ${smsResult.status}`} />
                  <Chip label={`Ref: ${smsResult.providerReference}`} />
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Failed & Stuck Deliveries
          </Typography>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ mb: 2 }}>
            <TextField
              size="small"
              label="Search type"
              select
              value={searchType}
              onChange={(e) => setSearchType(e.target.value as 'eventId' | 'correlationId')}
              sx={{ minWidth: 160 }}
            >
              <MenuItem value="eventId">Event ID</MenuItem>
              <MenuItem value="correlationId">Correlation ID</MenuItem>
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
              {['FAILED', 'DLQ', 'SKIPPED', 'RETRYING', 'QUEUED', 'SENT'].map((status) => (
                <MenuItem key={status} value={status}>
                  {status}
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
              <MenuItem value="">All</MenuItem>
              <MenuItem value="SMS">SMS</MenuItem>
              <MenuItem value="EMAIL">EMAIL</MenuItem>
            </TextField>
            <Button variant="contained" onClick={() => void load()} disabled={loading}>
              Refresh
            </Button>
          </Stack>

          <Stack spacing={1}>
            {deliveries.map((delivery) => (
              <Box
                key={delivery.id}
                sx={{ p: 1.5, borderRadius: 1, border: '1px solid', borderColor: 'divider' }}
              >
                <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" spacing={1}>
                  <Box>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                      <Chip label={delivery.status} size="small" color={statusColor(delivery.status)} />
                      <Typography variant="body2" fontWeight={700}>
                        {delivery.eventType}
                      </Typography>
                    </Stack>
                    <Typography variant="body2">
                      To: {delivery.recipient} · {delivery.channel}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Event: {delivery.eventId} · Correlation: {delivery.correlationId}
                    </Typography>
                    {delivery.failureReason && (
                      <Typography variant="caption" color="error" display="block">
                        {delivery.failureCategory ?? 'Error'}: {delivery.failureReason}
                      </Typography>
                    )}
                  </Box>
                  <Button
                    variant="outlined"
                    size="small"
                    disabled={!RESENDABLE.has(delivery.status)}
                    onClick={() => void resend(delivery)}
                  >
                    Resend
                  </Button>
                </Stack>
              </Box>
            ))}
            {!deliveries.length && (
              <Typography variant="body2" color="text.secondary">
                No deliveries found for the current filters.
              </Typography>
            )}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
};

export default CommunicationOperations;
