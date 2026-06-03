import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Paper,
  Card,
  CardContent,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  LinearProgress,
} from '@mui/material';
import { analyticsApi } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
};

const AnalyticsDashboard: React.FC = () => {
  const { user } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [period, setPeriod] = useState('DAILY');
  const [oeeData, setOeeData] = useState<any>(null);
  const [trends, setTrends] = useState<any>(null);
  const [costAnalysis, setCostAnalysis] = useState<any[]>([]);
  const [qualityMetrics, setQualityMetrics] = useState<any>(null);
  const [performance, setPerformance] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadAnalytics();
    }
  }, [user?.organizationId, period]);

  const loadAnalytics = async () => {
    try {
      const [oeeResp, trendsResp, costResp, qualityResp, perfResp] = await Promise.all([
        analyticsApi.calculateOEE(user!.organizationId),
        analyticsApi.getProductionTrends(user!.organizationId, period, 30),
        analyticsApi.getCostAnalysisByProduct(user!.organizationId),
        analyticsApi.getQualityMetricsSummary(user!.organizationId),
        analyticsApi.getPerformanceSummary(user!.organizationId),
      ]);
      setOeeData(oeeResp.data);
      setTrends(trendsResp.data);
      setCostAnalysis(costResp.data);
      setQualityMetrics(qualityResp.data);
      setPerformance(perfResp.data);
    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <Typography>Loading analytics...</Typography>;

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>Manufacturing Analytics</Typography>

      <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)} sx={{ mb: 3 }}>
        <Tab label="OEE Analysis" />
        <Tab label="Production Trends" />
        <Tab label="Cost Analysis" />
        <Tab label="Quality Metrics" />
        <Tab label="Performance" />
      </Tabs>

      {/* OEE Analysis Tab */}
      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>Overall OEE</Typography>
                <Typography variant="h3" color="primary">
                  {oeeData?.averages?.oee?.toFixed(1) || 0}%
                </Typography>
                <LinearProgress 
                  variant="determinate" 
                  value={oeeData?.averages?.oee || 0} 
                  sx={{ mt: 2 }} 
                />
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>Availability</Typography>
                <Typography variant="h3">
                  {oeeData?.averages?.availability?.toFixed(1) || 0}%
                </Typography>
                <LinearProgress 
                  variant="determinate" 
                  value={oeeData?.averages?.availability || 0} 
                  color="info"
                  sx={{ mt: 2 }} 
                />
                <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                  Actual / Planned Time
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>Performance</Typography>
                <Typography variant="h3">
                  {oeeData?.averages?.performance?.toFixed(1) || 0}%
                </Typography>
                <LinearProgress 
                  variant="determinate" 
                  value={oeeData?.averages?.performance || 0} 
                  color="warning"
                  sx={{ mt: 2 }} 
                />
                <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                  Ideal / Actual Cycle Time
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>Quality</Typography>
                <Typography variant="h3">
                  {oeeData?.averages?.quality?.toFixed(1) || 0}%
                </Typography>
                <LinearProgress 
                  variant="determinate" 
                  value={oeeData?.averages?.quality || 0} 
                  color="success"
                  sx={{ mt: 2 }} 
                />
                <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                  Good / Total Units
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          {/* OEE Formula */}
          <Grid item xs={12}>
            <Paper sx={{ p: 3, bgcolor: 'info.light' }}>
              <Typography variant="h6" gutterBottom>OEE Calculation</Typography>
              <Typography variant="body1">
                OEE = <strong>Availability</strong> × <strong>Performance</strong> × <strong>Quality</strong>
              </Typography>
              <Typography variant="body2" sx={{ mt: 2 }}>
                = {oeeData?.averages?.availability?.toFixed(1) || 0}% × {oeeData?.averages?.performance?.toFixed(1) || 0}% × {oeeData?.averages?.quality?.toFixed(1) || 0}%
                = <strong>{oeeData?.averages?.oee?.toFixed(1) || 0}%</strong>
              </Typography>
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Production Trends Tab */}
      <TabPanel value={tabValue} index={1}>
        <Box sx={{ mb: 2 }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Period</InputLabel>
            <Select value={period} onChange={(e) => setPeriod(e.target.value)} label="Period">
              <MenuItem value="DAILY">Daily</MenuItem>
              <MenuItem value="WEEKLY">Weekly</MenuItem>
              <MenuItem value="MONTHLY">Monthly</MenuItem>
            </Select>
          </FormControl>
        </Box>

        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Period</TableCell>
                <TableCell align="right">WOs Completed</TableCell>
                <TableCell align="right">Qty Produced</TableCell>
                <TableCell align="right">Qty Scrapped</TableCell>
                <TableCell align="right">Yield %</TableCell>
                <TableCell align="right">Total Cost</TableCell>
                <TableCell align="right">Cost/Unit</TableCell>
                <TableCell align="right">On-Time %</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {trends?.trends?.map((trend: any, index: number) => (
                <TableRow key={index}>
                  <TableCell>{new Date(trend.period).toLocaleDateString()}</TableCell>
                  <TableCell align="right">{trend.work_orders_completed}</TableCell>
                  <TableCell align="right">{trend.total_quantity_produced?.toFixed(0)}</TableCell>
                  <TableCell align="right">{trend.total_quantity_scrapped?.toFixed(0)}</TableCell>
                  <TableCell align="right">{trend.daily_yield_percentage?.toFixed(1)}%</TableCell>
                  <TableCell align="right">${trend.total_production_cost?.toFixed(0)}</TableCell>
                  <TableCell align="right">${trend.cost_per_unit?.toFixed(2)}</TableCell>
                  <TableCell align="right">{trend.on_time_percentage?.toFixed(1)}%</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* Cost Analysis Tab */}
      <TabPanel value={tabValue} index={2}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Product</TableCell>
                <TableCell align="right">WOs</TableCell>
                <TableCell align="right">Qty Produced</TableCell>
                <TableCell align="right">Material Cost</TableCell>
                <TableCell align="right">Labor Cost</TableCell>
                <TableCell align="right">Overhead Cost</TableCell>
                <TableCell align="right">Total Cost</TableCell>
                <TableCell align="right">Cost/Unit</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {costAnalysis?.map((product: any, index: number) => (
                <TableRow key={index}>
                  <TableCell>{product.product_code} - {product.product_name}</TableCell>
                  <TableCell align="right">{product.total_work_orders}</TableCell>
                  <TableCell align="right">{product.total_quantity_produced?.toFixed(0)}</TableCell>
                  <TableCell align="right">${product.total_material_cost?.toFixed(2)}</TableCell>
                  <TableCell align="right">${product.total_labor_cost?.toFixed(2)}</TableCell>
                  <TableCell align="right">${product.total_overhead_cost?.toFixed(2)}</TableCell>
                  <TableCell align="right">${product.total_cost?.toFixed(2)}</TableCell>
                  <TableCell align="right">${product.total_cost_per_unit?.toFixed(2)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* Quality Metrics Tab */}
      <TabPanel value={tabValue} index={3}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Total Inspections</Typography>
                <Typography variant="h4">{qualityMetrics?.totalInspections || 0}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Pass Rate</Typography>
                <Typography variant="h4" color="success.main">
                  {qualityMetrics?.overallPassRate?.toFixed(1) || 0}%
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Passed</Typography>
                <Typography variant="h4">{qualityMetrics?.passedInspections || 0}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Failed</Typography>
                <Typography variant="h4" color="error.main">{qualityMetrics?.failedInspections || 0}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>Quality by Product</Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell align="right">Inspections</TableCell>
                      <TableCell align="right">Passed</TableCell>
                      <TableCell align="right">Failed</TableCell>
                      <TableCell align="right">Pass Rate</TableCell>
                      <TableCell align="right">First Pass Yield</TableCell>
                      <TableCell align="right">Total Defects</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {qualityMetrics?.productMetrics?.map((product: any, index: number) => (
                      <TableRow key={index}>
                        <TableCell>{product.product_code} - {product.product_name}</TableCell>
                        <TableCell align="right">{product.total_inspections}</TableCell>
                        <TableCell align="right">{product.passed_inspections}</TableCell>
                        <TableCell align="right">{product.failed_inspections}</TableCell>
                        <TableCell align="right">{product.avg_pass_rate?.toFixed(1)}%</TableCell>
                        <TableCell align="right">{product.first_pass_yield?.toFixed(1)}%</TableCell>
                        <TableCell align="right">{product.total_defects}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Performance Tab */}
      <TabPanel value={tabValue} index={4}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Units Produced (30d)</Typography>
                <Typography variant="h4">{performance?.units_produced_last_30_days?.toFixed(0) || 0}</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">Avg Cycle Time</Typography>
                <Typography variant="h4">{performance?.avg_cycle_time_hours?.toFixed(1) || 0}h</Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">First Pass Yield</Typography>
                <Typography variant="h4" color="success.main">
                  {performance?.first_pass_yield_30_days?.toFixed(1) || 0}%
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary">On-Time Delivery</Typography>
                <Typography variant="h4">
                  {performance?.on_time_delivery_30_days?.toFixed(1) || 0}%
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>Performance Summary (Last 30 Days)</Typography>
              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <Typography variant="body2" color="textSecondary">Completed WOs</Typography>
                  <Typography variant="h6">{performance?.completed_last_30_days || 0}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="body2" color="textSecondary">Avg Units/WO</Typography>
                  <Typography variant="h6">{performance?.avg_units_per_work_order?.toFixed(1) || 0}</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="body2" color="textSecondary">Avg Lead Time</Typography>
                  <Typography variant="h6">{performance?.avg_lead_time_days?.toFixed(1) || 0} days</Typography>
                </Grid>
                <Grid item xs={6} md={3}>
                  <Typography variant="body2" color="textSecondary">Capacity Utilization</Typography>
                  <Typography variant="h6">{performance?.capacity_utilization_30_days?.toFixed(1) || 0}%</Typography>
                </Grid>
              </Grid>
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>
    </Box>
  );
};

export default AnalyticsDashboard;

