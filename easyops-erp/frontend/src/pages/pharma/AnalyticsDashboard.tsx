import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, CircularProgress, Alert, Tabs, Tab,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, LinearProgress
} from '@mui/material';
// Icon imports
import {
  TrendingUp as TrendingUpIcon,
  Assessment as AssessmentIcon,
  People as PeopleIcon,
  TrackChanges as TargetIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { 
  AreaPerformanceReport, 
  Area, 
  Territory,
  AreaWiseCollectionReport,
  IncentiveReport 
} from '../../services/pharmaService';

const AnalyticsDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [activeTab, setActiveTab] = useState(0);
  
  const [filters, setFilters] = useState({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    areaId: '',
    territoryId: ''
  });

  // Analytics Data
  const [areaPerformanceData, setAreaPerformanceData] = useState<AreaPerformanceReport[]>([]);
  const [targetAchievementData, setTargetAchievementData] = useState<any[]>([]);
  const [employeePerformanceData, setEmployeePerformanceData] = useState<any[]>([]);
  const [collectionData, setCollectionData] = useState<AreaWiseCollectionReport | null>(null);
  const [incentiveData, setIncentiveData] = useState<IncentiveReport | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadAreas();
      loadTerritories();
      loadAnalyticsData();
    }
  }, [currentOrganizationId, filters.year, filters.month]);

  const loadAreas = async () => {
    if (!currentOrganizationId) return;
    try {
      const data = await pharmaService.getAreas(currentOrganizationId);
      setAreas(data.filter(a => a.isActive));
    } catch (error) {
      console.error('Failed to load areas:', error);
    }
  };

  const loadTerritories = async () => {
    if (!currentOrganizationId) return;
    try {
      const data = await pharmaService.getAllTerritoriesForOrganization(currentOrganizationId);
      setTerritories(data);
    } catch (error) {
      console.error('Failed to load territories:', error);
    }
  };

  const loadAnalyticsData = async () => {
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      setError(null);

      // Load data for all areas/territories or selected filter
      // Incentive report returns territory-level data (areaIncentives key is legacy)
      const targetAreas = filters.areaId ? [filters.areaId] : filters.territoryId ? [] : areas.map(a => a.id);
      const targetTerritoryId = filters.territoryId;

      // Load area performance reports (or skip if territory filter - use incentive data for territory view)
      const performanceReports: AreaPerformanceReport[] = [];
      if (!targetTerritoryId) {
        for (const areaId of targetAreas) {
          try {
            const report = await pharmaService.getAreaPerformanceReport(
              currentOrganizationId,
              areaId,
              filters.year,
              filters.month
            );
            performanceReports.push(report);
          } catch (err) {
            console.warn(`Failed to load performance for area ${areaId}:`, err);
          }
        }
      }
      setAreaPerformanceData(performanceReports);

      // Load incentive report for target achievement
      try {
        const incentiveReport = await pharmaService.getIncentiveReport(
          currentOrganizationId,
          filters.year,
          filters.month
        );
        setIncentiveData(incentiveReport);
        
        // Process target achievement data (areaIncentives is territory-level from backend)
        let achievementData = incentiveReport.areaIncentives.map(area => ({
          areaId: area.areaId,
          areaName: area.areaName,
          targetAmount: area.targetAmount,
          coveredAmount: area.coveredAmount,
          coveragePercentage: area.targetCoveragePercentage || 0,
          targetAchieved: area.targetAchieved,
          eligible: area.eligible
        }));
        if (targetTerritoryId) {
          achievementData = achievementData.filter(a => a.areaId === targetTerritoryId);
        }
        setTargetAchievementData(achievementData);

        // Extract employee performance data
        const employeeData: any[] = [];
        const areasToProcess = targetTerritoryId
          ? incentiveReport.areaIncentives.filter(a => a.areaId === targetTerritoryId)
          : incentiveReport.areaIncentives;
        areasToProcess.forEach(area => {
          if (area.employeeIncentives) {
            area.employeeIncentives.forEach(emp => {
              employeeData.push({
                employeeId: emp.employeeId,
                employeeName: emp.employeeName || emp.employeeIdCode || 'N/A',
                employeeIdCode: emp.employeeIdCode,
                role: emp.role,
                areaName: area.areaName,
                incentiveAmount: emp.incentiveAmount,
                areaTarget: area.targetAmount,
                areaCovered: area.coveredAmount
              });
            });
          }
        });
        setEmployeePerformanceData(employeeData);

      } catch (err) {
        console.warn('Failed to load incentive report:', err);
      }

      // Load collection data (area filter only - backend uses areaId)
      const startDate = new Date(filters.year, filters.month - 1, 1).toISOString().split('T')[0];
      const endDate = new Date(filters.year, filters.month, 0).toISOString().split('T')[0];
      try {
        const collectionReport = await pharmaService.getAreaWiseCollectionReport(
          currentOrganizationId,
          startDate,
          endDate,
          filters.areaId || undefined
        );
        setCollectionData(collectionReport);
      } catch (err) {
        console.warn('Failed to load collection report:', err);
      }

    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load analytics data');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-BD', {
      style: 'currency',
      currency: 'BDT',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  };

  // Calculate summary metrics
  const summaryMetrics = {
    totalAreas: areaPerformanceData.length,
    areasTargetMet: targetAchievementData.filter(a => a.targetAchieved).length,
    totalTargetAmount: targetAchievementData.reduce((sum, a) => sum + a.targetAmount, 0),
    totalCoveredAmount: targetAchievementData.reduce((sum, a) => sum + a.coveredAmount, 0),
    overallCoveragePercentage: targetAchievementData.length > 0
      ? (targetAchievementData.reduce((sum, a) => sum + a.coveredAmount, 0) /
         targetAchievementData.reduce((sum, a) => sum + a.targetAmount, 0)) * 100
      : 0,
    totalEmployees: employeePerformanceData.length,
    totalIncentives: employeePerformanceData.reduce((sum, e) => sum + e.incentiveAmount, 0)
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Analytics Dashboard
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Comprehensive analytics for area/territory performance, target achievement, and employee tracking
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Area (Optional)"
                value={filters.areaId}
                onChange={(e) => {
                  setFilters({ ...filters, areaId: e.target.value, territoryId: '' });
                  setTimeout(loadAnalyticsData, 100);
                }}
              >
                <MenuItem value="">All Areas</MenuItem>
                {areas.map((area) => (
                  <MenuItem key={area.id} value={area.id}>
                    {area.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Territory (Optional)"
                value={filters.territoryId}
                onChange={(e) => {
                  setFilters({ ...filters, territoryId: e.target.value, areaId: '' });
                  setTimeout(loadAnalyticsData, 100);
                }}
              >
                <MenuItem value="">All Territories</MenuItem>
                {territories.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    {t.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <TextField
                fullWidth
                type="number"
                label="Year"
                value={filters.year}
                onChange={(e) => {
                  setFilters({ ...filters, year: parseInt(e.target.value) });
                  setTimeout(loadAnalyticsData, 100);
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <TextField
                fullWidth
                select
                label="Month"
                value={filters.month}
                onChange={(e) => {
                  setFilters({ ...filters, month: parseInt(e.target.value) });
                  setTimeout(loadAnalyticsData, 100);
                }}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => (
                  <MenuItem key={m} value={m}>
                    {new Date(2000, m - 1).toLocaleString('default', { month: 'long' })}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Button
                fullWidth
                variant="contained"
                onClick={loadAnalyticsData}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                {loading ? <CircularProgress size={24} /> : 'Refresh'}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'primary.light', color: 'primary.contrastText' }}>
            <CardContent>
              <Typography variant="body2">Total Areas</Typography>
              <Typography variant="h4">{summaryMetrics.totalAreas}</Typography>
              <Typography variant="caption">
                {summaryMetrics.areasTargetMet} targets met
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'success.light', color: 'success.contrastText' }}>
            <CardContent>
              <Typography variant="body2">Target Coverage</Typography>
              <Typography variant="h4">{summaryMetrics.overallCoveragePercentage.toFixed(1)}%</Typography>
              <Typography variant="caption">
                {formatCurrency(summaryMetrics.totalCoveredAmount)} / {formatCurrency(summaryMetrics.totalTargetAmount)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'info.light', color: 'info.contrastText' }}>
            <CardContent>
              <Typography variant="body2">Active Employees</Typography>
              <Typography variant="h4">{summaryMetrics.totalEmployees}</Typography>
              <Typography variant="caption">
                {formatCurrency(summaryMetrics.totalIncentives)} total incentives
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card sx={{ bgcolor: 'warning.light', color: 'warning.contrastText' }}>
            <CardContent>
              <Typography variant="body2">Collection Status</Typography>
              <Typography variant="h4">
                {collectionData ? formatCurrency(collectionData.grandTotalCollection) : '-'}
              </Typography>
              <Typography variant="caption">Total collected</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs for different analytics views */}
      <Paper sx={{ p: 3 }}>
        <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)} sx={{ mb: 3 }}>
          <Tab icon={<AssessmentIcon />} label="Area Performance" iconPosition="start" />
          <Tab icon={<TargetIcon />} label="Target Achievement" iconPosition="start" />
          <Tab icon={<PeopleIcon />} label="Employee Performance" iconPosition="start" />
        </Tabs>

        {/* Area Performance Tab */}
        {activeTab === 0 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Area Performance Overview
            </Typography>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Area</TableCell>
                    <TableCell align="right">Target</TableCell>
                    <TableCell align="right">Covered</TableCell>
                    <TableCell align="right">Coverage %</TableCell>
                    <TableCell align="right">Expenses</TableCell>
                    <TableCell align="right">Expense %</TableCell>
                    <TableCell align="center">Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {areaPerformanceData.map((area, index) => (
                    <TableRow key={index}>
                      <TableCell>{area.areaName}</TableCell>
                      <TableCell align="right">
                        {area.targetAmount ? formatCurrency(area.targetAmount) : '-'}
                      </TableCell>
                      <TableCell align="right">
                        {area.coveredAmount ? formatCurrency(area.coveredAmount) : '-'}
                      </TableCell>
                      <TableCell align="right">
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, justifyContent: 'flex-end' }}>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(area.targetCoveragePercentage || 0, 100)}
                            sx={{ width: 60, height: 8, borderRadius: 1 }}
                            color={area.targetCoveragePercentage && area.targetCoveragePercentage >= 100 ? 'success' : 'primary'}
                          />
                          <Typography variant="body2">
                            {(area.targetCoveragePercentage || 0).toFixed(1)}%
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        {formatCurrency(area.totalExpenses)}
                      </TableCell>
                      <TableCell align="right">
                        {(area.expensePercentage || 0).toFixed(1)}%
                      </TableCell>
                      <TableCell align="center">
                        <Chip
                          label={area.incentiveEligible ? 'Eligible' : 'Not Eligible'}
                          color={area.incentiveEligible ? 'success' : 'default'}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Target Achievement Tab */}
        {activeTab === 1 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Target Achievement Analytics
            </Typography>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>Target Achievement Rate</Typography>
                    <Typography variant="h4" color="primary">
                      {summaryMetrics.areasTargetMet} / {summaryMetrics.totalAreas}
                    </Typography>
                    <Typography variant="caption">
                      {summaryMetrics.totalAreas > 0 
                        ? ((summaryMetrics.areasTargetMet / summaryMetrics.totalAreas) * 100).toFixed(1)
                        : 0}% of areas met targets
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>Overall Coverage</Typography>
                    <Typography variant="h4" color="success.main">
                      {summaryMetrics.overallCoveragePercentage.toFixed(1)}%
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={Math.min(summaryMetrics.overallCoveragePercentage, 100)}
                      sx={{ mt: 1, height: 8, borderRadius: 1 }}
                      color={summaryMetrics.overallCoveragePercentage >= 100 ? 'success' : 'primary'}
                    />
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Area</TableCell>
                    <TableCell align="right">Target</TableCell>
                    <TableCell align="right">Covered</TableCell>
                    <TableCell>Progress</TableCell>
                    <TableCell align="center">Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {targetAchievementData
                    .sort((a, b) => b.coveragePercentage - a.coveragePercentage)
                    .map((area, index) => (
                      <TableRow key={index}>
                        <TableCell>{area.areaName}</TableCell>
                        <TableCell align="right">{formatCurrency(area.targetAmount)}</TableCell>
                        <TableCell align="right">{formatCurrency(area.coveredAmount)}</TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <LinearProgress
                              variant="determinate"
                              value={Math.min(area.coveragePercentage, 100)}
                              sx={{ flexGrow: 1, height: 8, borderRadius: 1 }}
                              color={area.coveragePercentage >= 100 ? 'success' : area.coveragePercentage >= 80 ? 'warning' : 'error'}
                            />
                            <Typography variant="body2" sx={{ minWidth: 50 }}>
                              {area.coveragePercentage.toFixed(1)}%
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell align="center">
                          <Chip
                            label={area.targetAchieved ? 'Achieved' : 'Not Achieved'}
                            color={area.targetAchieved ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Employee Performance Tab */}
        {activeTab === 2 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Employee Performance Tracking
            </Typography>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Employee</TableCell>
                    <TableCell>Role</TableCell>
                    <TableCell>Area</TableCell>
                    <TableCell align="right">Incentive</TableCell>
                    <TableCell align="right">Area Target</TableCell>
                    <TableCell align="right">Area Covered</TableCell>
                    <TableCell>Contribution</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {employeePerformanceData
                    .sort((a, b) => b.incentiveAmount - a.incentiveAmount)
                    .map((emp, index) => (
                      <TableRow key={index}>
                        <TableCell>
                          <Typography variant="body2" fontWeight="medium">
                            {emp.employeeName}
                          </Typography>
                          {emp.employeeIdCode && (
                            <Typography variant="caption" color="text.secondary">
                              {emp.employeeIdCode}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Chip label={emp.role} size="small" />
                        </TableCell>
                        <TableCell>{emp.areaName}</TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" fontWeight="bold" color="success.main">
                            {formatCurrency(emp.incentiveAmount)}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">{formatCurrency(emp.areaTarget)}</TableCell>
                        <TableCell align="right">{formatCurrency(emp.areaCovered)}</TableCell>
                        <TableCell>
                          <LinearProgress
                            variant="determinate"
                            value={emp.areaTarget > 0 ? (emp.areaCovered / emp.areaTarget) * 100 : 0}
                            sx={{ height: 6, borderRadius: 1 }}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default AnalyticsDashboard;

