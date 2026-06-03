import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
  Grid,
  Chip,
  LinearProgress,
} from '@mui/material';
import {
  CheckCircle,
  Error,
  Warning,
  Link as LinkIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

interface IntegrationStatus {
  systemName: string;
  status: 'connected' | 'disconnected' | 'error';
  lastSync: string;
  health: number;
}

const SystemIntegration: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [integrationStatus, setIntegrationStatus] = useState<IntegrationStatus[]>([]);
  const [systemHealth, setSystemHealth] = useState<any>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadIntegrationStatus();
      loadSystemHealth();
    }
  }, [currentOrganizationId]);

  const loadIntegrationStatus = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.getIntegrationStatus(currentOrganizationId);
      setIntegrationStatus(response.data?.integrations || []);
    } catch (err: any) {
      console.error('Failed to load integration status:', err);
      setError(err.response?.data?.message || 'Failed to load integration status');
    } finally {
      setLoading(false);
    }
  };

  const loadSystemHealth = async () => {
    try {
      const response = await hrService.getSystemHealth();
      setSystemHealth(response.data);
    } catch (err: any) {
      console.error('Failed to load system health:', err);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected':
        return <CheckCircle color="success" />;
      case 'error':
        return <Error color="error" />;
      default:
        return <Warning color="warning" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected':
        return 'success';
      case 'error':
        return 'error';
      default:
        return 'warning';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          System Integration
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Monitor integration status with external systems
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <LinkIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Integration Status</Typography>
              </Box>
              {integrationStatus.length === 0 ? (
                <Alert severity="info">No integrations configured</Alert>
              ) : (
                <Grid container spacing={2}>
                  {integrationStatus.map((integration) => (
                    <Grid item xs={12} key={integration.systemName}>
                      <Card variant="outlined">
                        <CardContent>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              {getStatusIcon(integration.status)}
                              <Typography variant="subtitle1">{integration.systemName}</Typography>
                            </Box>
                            <Chip
                              label={integration.status}
                              color={getStatusColor(integration.status) as any}
                              size="small"
                            />
                          </Box>
                          <Box sx={{ mt: 1 }}>
                            <Typography variant="body2" color="text.secondary">
                              Last Sync: {integration.lastSync
                                ? new Date(integration.lastSync).toLocaleString()
                                : 'Never'}
                            </Typography>
                            <Box sx={{ mt: 1 }}>
                              <Typography variant="caption" color="text.secondary">
                                Health: {integration.health}%
                              </Typography>
                              <LinearProgress
                                variant="determinate"
                                value={integration.health}
                                sx={{ mt: 0.5 }}
                                color={integration.health > 80 ? 'success' : integration.health > 50 ? 'warning' : 'error'}
                              />
                            </Box>
                          </Box>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Health
              </Typography>
              {systemHealth ? (
                <Box>
                  {Object.entries(systemHealth).map(([key, value]) => (
                    <Box key={key} sx={{ mb: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        {key.replace(/([A-Z])/g, ' $1').trim()}
                      </Typography>
                      <Typography variant="h6">
                        {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              ) : (
                <Alert severity="info">System health data not available</Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  );
};

export default SystemIntegration;

