import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, CircularProgress, Alert, Tabs, Tab,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, LinearProgress
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  Assessment as AssessmentIcon,
  Insights as InsightsIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { 
  TerritoryAnalytics, 
  Territory, 
  TerritoryOptimization,
  Division,
  Region
} from '../../services/pharmaService';
import { useSnackbar } from 'notistack';

const TerritoryAnalyticsDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [selectedTerritory, setSelectedTerritory] = useState<string>('');
  const [filters, setFilters] = useState({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
  });
  
  const [analytics, setAnalytics] = useState<TerritoryAnalytics | null>(null);
  const [optimization, setOptimization] = useState<TerritoryOptimization | null>(null);
  const [activeTab, setActiveTab] = useState(0);

  useEffect(() => {
    if (currentOrganizationId) {
      loadTerritories();
    }
  }, [currentOrganizationId]);

  useEffect(() => {
    if (selectedTerritory) {
      loadAnalytics();
      if (activeTab === 1) {
        loadOptimization();
      }
    }
  }, [selectedTerritory, filters.year, filters.month, activeTab]);

  const loadTerritories = async () => {
    if (!currentOrganizationId) return;
    try {
      // Load divisions, then regions, then territories
      const divisions = await pharmaService.getActiveDivisions(currentOrganizationId);
      const allTerritories: Territory[] = [];
      
      for (const division of divisions) {
        const regions = await pharmaService.getRegionsByDivision(division.id);
        for (const region of regions) {
          const regionTerritories = await pharmaService.getTerritoriesByRegion(region.id);
          allTerritories.push(...regionTerritories.filter(t => t.isActive));
        }
      }
      
      setTerritories(allTerritories);
    } catch (error) {
      console.error('Failed to load territories:', error);
    }
  };

  const loadAnalytics = async () => {
    if (!selectedTerritory) return;
    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getTerritoryAnalytics(
        selectedTerritory,
        filters.year,
        filters.month
      );
      setAnalytics(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load analytics');
      enqueueSnackbar('Failed to load territory analytics', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const loadOptimization = async () => {
    if (!selectedTerritory) return;
    try {
      setLoading(true);
      const data = await pharmaService.getTerritoryOptimization(selectedTerritory);
      setOptimization(data);
    } catch (err: any) {
      enqueueSnackbar('Failed to load optimization recommendations', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (loading && !analytics) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <InsightsIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
        Territory Analytics & Optimization
      </Typography>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              label="Territory"
              select
              value={selectedTerritory}
              onChange={(e) => setSelectedTerritory(e.target.value)}
              required
            >
              <MenuItem value="">Select Territory</MenuItem>
              {territories.map((t) => (
                <MenuItem key={t.id} value={t.id}>
                  {t.name}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              label="Year"
              type="number"
              value={filters.year}
              onChange={(e) => setFilters({ ...filters, year: parseInt(e.target.value) })}
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              label="Month"
              type="number"
              inputProps={{ min: 1, max: 12 }}
              value={filters.month}
              onChange={(e) => setFilters({ ...filters, month: parseInt(e.target.value) })}
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Button
              fullWidth
              variant="contained"
              onClick={loadAnalytics}
              disabled={!selectedTerritory || loading}
            >
              Load
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {!selectedTerritory && (
        <Alert severity="info">
          Please select a territory to view analytics and optimization recommendations.
        </Alert>
      )}

      {selectedTerritory && (
        <>
          <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ mb: 2 }}>
            <Tab label="Performance Analytics" icon={<AssessmentIcon />} iconPosition="start" />
            <Tab label="Optimization Recommendations" icon={<TrendingUpIcon />} iconPosition="start" />
          </Tabs>

          {activeTab === 0 && analytics && (
            <Box>
              <Grid container spacing={3} sx={{ mb: 3 }}>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="textSecondary" gutterBottom>
                        Target Achievement
                      </Typography>
                      <Typography variant="h4">
                        {(Number(analytics.summary.targetAchievementRate) || 0).toFixed(1)}%
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(Number(analytics.summary.targetAchievementRate) || 0, 100)}
                        sx={{ mt: 1 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="textSecondary" gutterBottom>
                        Efficiency Score
                      </Typography>
                      <Typography variant="h4">
                        {(Number(analytics.summary.territoryEfficiencyScore) || 0).toFixed(1)}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="textSecondary" gutterBottom>
                        Total Employees
                      </Typography>
                      <Typography variant="h4">{analytics.summary.totalEmployees ?? 0}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="textSecondary" gutterBottom>
                        Status
                      </Typography>
                      <Box sx={{ mt: 1 }}>
                        <Chip
                          label={analytics.summary.targetAchieved ? 'Target Achieved' : 'Not Achieved'}
                          color={analytics.summary.targetAchieved ? 'success' : 'default'}
                          size="small"
                          sx={{ mr: 0.5 }}
                        />
                        <Chip
                          label={analytics.summary.incentiveEligible ? 'Eligible' : 'Not Eligible'}
                          color={analytics.summary.incentiveEligible ? 'success' : 'default'}
                          size="small"
                        />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>

              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Territory Summary</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={6} md={3}>
                      <Typography variant="body2" color="textSecondary">Total Target</Typography>
                      <Typography variant="h6">
                        {Number(analytics.summary.totalTarget || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}
                      </Typography>
                    </Grid>
                    <Grid item xs={6} md={3}>
                      <Typography variant="body2" color="textSecondary">Total Covered</Typography>
                      <Typography variant="h6">
                        {Number(analytics.summary.totalCovered || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}
                      </Typography>
                    </Grid>
                    <Grid item xs={6} md={3}>
                      <Typography variant="body2" color="textSecondary">Total Expenses</Typography>
                      <Typography variant="h6">
                        {Number(analytics.summary.totalExpenses || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}
                      </Typography>
                    </Grid>
                    <Grid item xs={6} md={3}>
                      <Typography variant="body2" color="textSecondary">Total Incentives</Typography>
                      <Typography variant="h6">
                        {Number(analytics.summary.totalIncentives || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}
                      </Typography>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Box>
          )}

          {activeTab === 1 && optimization && (
            <Box>
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Workload Analysis</Typography>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Territory</TableCell>
                          <TableCell align="right">Employees</TableCell>
                          <TableCell align="right">Target</TableCell>
                          <TableCell align="right">Workload/Employee</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {(optimization.workloadAnalysis || []).map((item: { territoryId?: string; territoryName?: string; areaId?: string; areaName?: string; employeeCount: number; targetAmount: number; workloadPerEmployee: number }, idx: number) => (
                          <TableRow key={item.territoryId || item.areaId || idx}>
                            <TableCell>{item.territoryName ?? item.areaName ?? '-'}</TableCell>
                            <TableCell align="right">{item.employeeCount}</TableCell>
                            <TableCell align="right">{Number(item.targetAmount || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}</TableCell>
                            <TableCell align="right">{Number(item.workloadPerEmployee || 0).toLocaleString('en-BD', { style: 'currency', currency: 'BDT', minimumFractionDigits: 0 })}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>

              {optimization.performanceGaps.length > 0 && (
                <Card sx={{ mb: 3 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom color="warning.main">
                      Performance Gaps
                    </Typography>
                    {optimization.performanceGaps.map((gap: { territoryId?: string; areaId?: string; territoryName?: string; areaName?: string; achievementRate: number; recommendation: string }, idx: number) => (
                      <Alert key={gap.territoryId || gap.areaId || idx} severity="warning" sx={{ mb: 1 }}>
                        <Typography variant="subtitle2">{gap.territoryName ?? gap.areaName ?? 'Territory'}</Typography>
                        <Typography variant="body2">
                          Achievement: {gap.achievementRate.toFixed(1)}% | {gap.recommendation}
                        </Typography>
                      </Alert>
                    ))}
                  </CardContent>
                </Card>
              )}

              {optimization.resourceAllocation.length > 0 && (
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>Resource Allocation Recommendations</Typography>
                    {(optimization.resourceAllocation || []).map((rec: { territoryId?: string; areaId?: string; territoryName?: string; areaName?: string; recommendation: string }, idx: number) => (
                      <Alert key={rec.territoryId || rec.areaId || idx} severity="info" sx={{ mb: 1 }}>
                        <Typography variant="subtitle2">{rec.territoryName ?? rec.areaName ?? 'Territory'}</Typography>
                        <Typography variant="body2">{rec.recommendation}</Typography>
                      </Alert>
                    ))}
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
        </>
      )}
    </Box>
  );
};

export default TerritoryAnalyticsDashboard;
