import React, { useEffect, useState } from 'react';
import { Box, Typography, Grid, Paper, Card, CardContent } from '@mui/material';
import { analyticsApi } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';
import './Manufacturing.css';

const ManufacturingDashboard: React.FC = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadDashboard();
    } else {
      // If no user, stop loading after a short delay
      setTimeout(() => setLoading(false), 1000);
    }
  }, [user?.organizationId]);

  const loadDashboard = async () => {
    try {
      const response = await analyticsApi.getManufacturingDashboard(user!.organizationId);
      setDashboardData(response.data);
    } catch (error) {
      console.error('Failed to load dashboard:', error);
      // Set empty data on error so dashboard still displays
      setDashboardData({});
    } finally {
      setLoading(false);
    }
  };

  if (loading) return (
    <Box sx={{ p: 3 }}>
      <Typography>Loading Manufacturing Dashboard...</Typography>
    </Box>
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Manufacturing Dashboard
      </Typography>

      <Grid container spacing={3}>
        {/* Work Order Metrics */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>Total Work Orders</Typography>
              <Typography variant="h4">{dashboardData?.workOrders?.total || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>In Progress</Typography>
              <Typography variant="h4">{dashboardData?.workOrders?.inProgress || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>Completed</Typography>
              <Typography variant="h4">{dashboardData?.workOrders?.completed || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>Overdue</Typography>
              <Typography variant="h4" color="error">{dashboardData?.workOrders?.overdue || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Production Metrics */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Production Metrics (Last 30 Days)</Typography>
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Units Produced</Typography>
                <Typography variant="h6">{dashboardData?.production?.total_completed_qty || 0}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Overall Yield</Typography>
                <Typography variant="h6">{dashboardData?.production?.overall_yield?.toFixed(1) || 0}%</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Total Cost</Typography>
                <Typography variant="h6">${dashboardData?.costs?.total_manufacturing_cost?.toFixed(0) || 0}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Cost/Unit</Typography>
                <Typography variant="h6">${dashboardData?.costs?.cost_per_unit?.toFixed(2) || 0}</Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        {/* Quality Metrics */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Quality Metrics</Typography>
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Inspections</Typography>
                <Typography variant="h6">{dashboardData?.quality?.total_inspections || 0}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Pass Rate</Typography>
                <Typography variant="h6">{dashboardData?.quality?.avg_pass_rate?.toFixed(1) || 0}%</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Total Defects</Typography>
                <Typography variant="h6">{dashboardData?.quality?.total_defects || 0}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography color="textSecondary">Open NCs</Typography>
                <Typography variant="h6" color="warning.main">{dashboardData?.quality?.openNonConformances || 0}</Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ManufacturingDashboard;

