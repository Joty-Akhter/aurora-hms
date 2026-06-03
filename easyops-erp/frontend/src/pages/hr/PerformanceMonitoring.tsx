import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
  Grid,
  LinearProgress,
} from '@mui/material';
import {
  Speed,
  Memory,
  Storage,
  NetworkCheck,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const PerformanceMonitoring: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [performanceMetrics, setPerformanceMetrics] = useState<any>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadPerformanceMetrics();
    }
  }, [currentOrganizationId]);

  const loadPerformanceMetrics = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.getPerformanceMetrics(currentOrganizationId);
      setPerformanceMetrics(response.data);
    } catch (err: any) {
      console.error('Failed to load performance metrics:', err);
      setError(err.response?.data?.message || 'Failed to load performance metrics');
    } finally {
      setLoading(false);
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
          Performance Monitoring
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Monitor system performance metrics and optimization
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2}>
        {performanceMetrics ? (
          Object.entries(performanceMetrics).map(([key, value]) => (
            <Grid item xs={12} md={6} key={key}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    {key.replace(/([A-Z])/g, ' $1').trim()}
                  </Typography>
                  {typeof value === 'number' && key.toLowerCase().includes('percent') ? (
                    <>
                      <Typography variant="h4">{value}%</Typography>
                      <LinearProgress
                        variant="determinate"
                        value={value}
                        sx={{ mt: 2 }}
                        color={value > 80 ? 'success' : value > 50 ? 'warning' : 'error'}
                      />
                    </>
                  ) : (
                    <Typography variant="h5">
                      {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))
        ) : (
          <Grid item xs={12}>
            <Alert severity="info">Performance metrics not available</Alert>
          </Grid>
        )}
      </Grid>
    </div>
  );
};

export default PerformanceMonitoring;

