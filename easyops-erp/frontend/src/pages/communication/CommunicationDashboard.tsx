import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import communicationService, {
  CommunicationDelivery,
  OpsAlertStatusResponse,
  ProviderHealthResponse,
} from '@services/communicationService';

const statusColor = (status: string): 'success' | 'warning' | 'error' | 'default' => {
  if (status === 'UP' || status === 'OK' || status === 'SENT') return 'success';
  if (status === 'WARN' || status === 'RETRYING') return 'warning';
  if (status === 'CRITICAL' || status === 'DOWN' || status === 'FAILED' || status === 'DLQ') return 'error';
  return 'default';
};

const CommunicationDashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [providerHealth, setProviderHealth] = useState<ProviderHealthResponse[]>([]);
  const [alerts, setAlerts] = useState<OpsAlertStatusResponse | null>(null);
  const [recentDeliveries, setRecentDeliveries] = useState<CommunicationDelivery[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const [health, alertStatus, deliveriesPage] = await Promise.all([
          communicationService.getProviderHealth(),
          communicationService.getOpsAlerts().catch(() => null),
          communicationService.queryCommunicationDeliveries({}),
        ]);
        setProviderHealth(health);
        setAlerts(alertStatus);
        setRecentDeliveries(deliveriesPage.content.slice(0, 8));
        setError(null);
      } catch {
        setError('Failed to load communication dashboard.');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const deliveryStats = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const row of recentDeliveries) {
      counts[row.status] = (counts[row.status] ?? 0) + 1;
    }
    return counts;
  }, [recentDeliveries]);

  const triggeredAlerts = alerts?.alerts.filter((a) => a.triggered) ?? [];

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 240 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 1 }}>
        Communication Dashboard
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Monitor provider health, operational alerts, and recent message delivery activity.
      </Typography>

      {triggeredAlerts.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
            Active alerts
          </Typography>
          {triggeredAlerts.map((alert) => (
            <Typography key={alert.key} variant="body2">
              {alert.message}
            </Typography>
          ))}
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Provider Health
              </Typography>
              <Stack spacing={1.5}>
                {providerHealth.map((provider) => (
                  <Box
                    key={`${provider.provider}-${provider.channel}`}
                    sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                  >
                    <Box>
                      <Typography variant="body2" fontWeight={600}>
                        {provider.provider}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {provider.channel}
                      </Typography>
                    </Box>
                    <Chip label={provider.status} size="small" color={statusColor(provider.status)} />
                  </Box>
                ))}
                {!providerHealth.length && (
                  <Typography variant="body2" color="text.secondary">
                    No providers configured.
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                System Alerts
              </Typography>
              {alerts ? (
                <Stack spacing={1}>
                  {alerts.alerts.map((alert) => (
                    <Box
                      key={alert.key}
                      sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 1 }}
                    >
                      <Typography variant="body2">{alert.message}</Typography>
                      <Chip label={alert.level} size="small" color={statusColor(alert.level)} />
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Alert status unavailable (operations permission may be required).
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Recent Deliveries
              </Typography>
              {Object.keys(deliveryStats).length > 0 && (
                <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
                  {Object.entries(deliveryStats).map(([status, count]) => (
                    <Chip key={status} label={`${status}: ${count}`} size="small" color={statusColor(status)} />
                  ))}
                </Stack>
              )}
              <List dense disablePadding>
                {recentDeliveries.map((row) => (
                  <ListItem key={row.id} disableGutters sx={{ py: 0.75, borderBottom: '1px solid', borderColor: 'divider' }}>
                    <ListItemText
                      primary={`${row.eventType} → ${row.recipient}`}
                      secondary={`${row.channel} · ${row.status} · ${new Date(row.createdAt).toLocaleString()}`}
                    />
                    <Chip label={row.status} size="small" color={statusColor(row.status)} />
                  </ListItem>
                ))}
                {!recentDeliveries.length && (
                  <Typography variant="body2" color="text.secondary">
                    No deliveries yet. Messages appear here when hospital events trigger notifications.
                  </Typography>
                )}
              </List>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 1 }}>
                Supported Event Types
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                The communication service listens for these lifecycle events from hospital modules.
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {[
                  'APPOINTMENT_CREATED',
                  'APPOINTMENT_CONFIRMED',
                  'APPOINTMENT_CANCELLED',
                  'INVOICE_CREATED',
                  'INVOICE_OVERDUE',
                  'INVOICE_PAID',
                ].map((event) => (
                  <Chip key={event} label={event} variant="outlined" size="small" />
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default CommunicationDashboard;
