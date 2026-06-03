import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Typography,
  CircularProgress,
  Alert,
  Grid,
  Button,
  TextField,
  MenuItem,
} from '@mui/material';
import {
  TrendingUp,
  Assessment,
  Timeline,
  Build,
  Schedule,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const AdvancedAnalytics: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analyticsData, setAnalyticsData] = useState<any>(null);
  const [filters, setFilters] = useState({
    months: 6,
    entityType: 'provident-fund',
  });

  const loadAnalytics = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      let response;
      
      if (tabValue === 0) {
        response = await hrService.forecastProvidentFundParticipation(currentOrganizationId, filters.months);
      } else {
        response = await hrService.analyzeTrendsAndPatterns(
          currentOrganizationId,
          filters.entityType,
          filters.months
        );
      }
      
      setAnalyticsData(response.data);
    } catch (err: any) {
      console.error('Failed to load analytics:', err);
      setError(err.response?.data?.message || 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  };

  const renderAnalyticsContent = () => {
    if (!analyticsData) {
      return <Alert severity="info">Click "Generate Analytics" to view data</Alert>;
    }

    return (
      <Grid container spacing={2}>
        {Object.entries(analyticsData).map(([key, value]) => (
          <Grid item xs={12} md={6} key={key}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  {key.replace(/([A-Z])/g, ' $1').trim()}
                </Typography>
                <Typography variant="h6" sx={{ mt: 1 }}>
                  {typeof value === 'object' ? (
                    <pre style={{ fontSize: '0.875rem', overflow: 'auto' }}>
                      {JSON.stringify(value, null, 2)}
                    </pre>
                  ) : (
                    String(value)
                  )}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    );
  };

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Advanced Analytics
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Predictive analytics, effectiveness measurement, forecasting, and trend analysis
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="PF Participation Forecast" icon={<Timeline />} />
            <Tab label="Trend Analysis" icon={<TrendingUp />} />
          </Tabs>
        </Box>

        <CardContent>
          <Box sx={{ mb: 3 }}>
            <Grid container spacing={2}>
              {tabValue === 0 && (
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Months to Forecast"
                    type="number"
                    value={filters.months}
                    onChange={(e) => setFilters({ ...filters, months: parseInt(e.target.value) })}
                  />
                </Grid>
              )}
              {tabValue === 1 && (
                <>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      label="Entity Type"
                      value={filters.entityType}
                      onChange={(e) => setFilters({ ...filters, entityType: e.target.value })}
                      select
                    >
                      <MenuItem value="provident-fund">Provident Fund</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      label="Months"
                      type="number"
                      value={filters.months}
                      onChange={(e) => setFilters({ ...filters, months: parseInt(e.target.value) })}
                    />
                  </Grid>
                </>
              )}
            </Grid>
            <Box sx={{ mt: 2 }}>
              <Button
                variant="contained"
                onClick={loadAnalytics}
                disabled={loading}
              >
                {loading ? <CircularProgress size={24} /> : 'Generate Analytics'}
              </Button>
            </Box>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
              <CircularProgress />
            </Box>
          ) : (
            renderAnalyticsContent()
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default AdvancedAnalytics;

