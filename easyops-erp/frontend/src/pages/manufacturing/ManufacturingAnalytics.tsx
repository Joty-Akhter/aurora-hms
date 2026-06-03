import React, { useEffect, useState } from 'react';
import { Box, Typography, Grid, Paper } from '@mui/material';
import { analyticsApi } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const ManufacturingAnalytics: React.FC = () => {
  const { user } = useAuth();
  const [oeeData, setOeeData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadAnalytics();
    }
  }, [user?.organizationId]);

  const loadAnalytics = async () => {
    try {
      const oeeResponse = await analyticsApi.calculateOEE(user!.organizationId);
      setOeeData(oeeResponse.data);
    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Manufacturing Analytics
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography color="textSecondary" gutterBottom>OEE</Typography>
            <Typography variant="h4">
              {oeeData?.averages?.oee?.toFixed(1) || 0}%
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography color="textSecondary" gutterBottom>Availability</Typography>
            <Typography variant="h4">
              {oeeData?.averages?.availability?.toFixed(1) || 0}%
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography color="textSecondary" gutterBottom>Performance</Typography>
            <Typography variant="h4">
              {oeeData?.averages?.performance?.toFixed(1) || 0}%
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography color="textSecondary" gutterBottom>Quality</Typography>
            <Typography variant="h4">
              {oeeData?.averages?.quality?.toFixed(1) || 0}%
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>OEE Breakdown</Typography>
            <Typography color="textSecondary">
              OEE (Overall Equipment Effectiveness) = Availability × Performance × Quality
            </Typography>
            <Typography variant="body2" sx={{ mt: 2 }}>
              • Availability: Actual production time / Planned production time<br />
              • Performance: Ideal cycle time / Actual cycle time<br />
              • Quality: Good units / Total units
            </Typography>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ManufacturingAnalytics;

