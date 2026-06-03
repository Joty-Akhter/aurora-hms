import React, { useEffect, useState } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  CircularProgress,
  Alert,
  Divider,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  TrendingUp as TrendIcon,
  ShowChart as ChartIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import {
  LineChart,
  AreaChart,
  Line,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine,
  ReferenceArea,
} from 'recharts';
import hospitalService from '../../services/hospitalService';
import type { LabResultTrend as LabResultTrendType } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultTrend: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const loincCode = searchParams.get('loincCode') || '';
  const [trend, setTrend] = useState<LabResultTrendType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [chartType, setChartType] = useState<'line' | 'area'>('line');
  
  useEffect(() => {
    if (id && loincCode) {
      loadTrend();
    }
  }, [id, loincCode]);
  
  const loadTrend = async () => {
    if (!id || !loincCode) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getTrendData(
        id,
        loincCode,
        startDate || undefined,
        endDate || undefined
      );
      setTrend(response.data);
    } catch (err: any) {
      console.error('Failed to load trend:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load trend data'));
      enqueueSnackbar('Failed to load trend data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleApplyFilter = () => {
    loadTrend();
  };
  
  // Prepare chart data
  const chartData = trend?.dataPoints
    .filter(dp => dp.value !== undefined && dp.value !== null)
    .map(dp => {
      const date = new Date(dp.resultDate);
      return {
        date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
        dateTime: date.getTime(),
        value: dp.value,
        resultValue: dp.resultValue,
        abnormalFlag: dp.abnormalFlag,
        isCritical: dp.isCriticalValue,
        resultStatus: dp.resultStatus,
        resultId: dp.resultId,
      };
    })
    .sort((a, b) => a.dateTime - b.dateTime) || [];
  
  // Custom tooltip for chart
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <Paper sx={{ p: 2, border: '1px solid #e0e0e0' }}>
          <Typography variant="subtitle2" gutterBottom>
            {label}
          </Typography>
          <Typography variant="body2" color="primary">
            Value: {data.value?.toFixed(2)} {trend?.resultUnits || ''}
          </Typography>
          {data.abnormalFlag && (
            <Typography variant="caption" color={data.abnormalFlag === 'H' || data.abnormalFlag === 'C' ? 'error' : 'warning'}>
              Flag: {data.abnormalFlag}
            </Typography>
          )}
          {data.isCritical && (
            <Typography variant="caption" color="error" display="block">
              Critical Value
            </Typography>
          )}
        </Paper>
      );
    }
    return null;
  };
  
  // Get color based on abnormal flag
  const getValueColor = (abnormalFlag?: string, isCritical?: boolean) => {
    if (isCritical) return '#d32f2f'; // Red for critical
    if (abnormalFlag === 'H') return '#f57c00'; // Orange for high
    if (abnormalFlag === 'L') return '#fbc02d'; // Yellow for low
    if (abnormalFlag === 'A') return '#f57c00'; // Orange for abnormal
    return '#1976d2'; // Blue for normal
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  if (error || !trend) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error || 'Trend data not available'}</Alert>
        <Button
          startIcon={<BackIcon />}
          onClick={() => navigate(`/hospital/patients/${id}/lab-results`)}
          sx={{ mt: 2 }}
        >
          Back to Results
        </Button>
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            startIcon={<BackIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/lab-results`)}
          >
            Back
          </Button>
          <Typography variant="h4">Trend Analysis</Typography>
        </Box>
      </Box>
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>{trend.testName}</Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            LOINC Code: {trend.loincCode}
          </Typography>
          <Box display="flex" gap={1} mt={2} flexWrap="wrap">
            <Chip
              label={`Trend: ${trend.trendDirection || 'N/A'}`}
              color={
                trend.trendDirection === 'INCREASING' ? 'error' :
                trend.trendDirection === 'DECREASING' ? 'success' : 'default'
              }
              icon={<TrendIcon />}
            />
            {trend.trendSlope !== undefined && (
              <Chip
                label={`Slope: ${trend.trendSlope.toFixed(4)} ${trend.resultUnits || ''}/day`}
              />
            )}
            <Chip label={`Data Points: ${trend.totalDataPoints}`} />
          </Box>
        </CardContent>
      </Card>
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Date Range Filter</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Start Date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="End Date"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Button
                variant="contained"
                fullWidth
                onClick={handleApplyFilter}
              >
                Apply Filter
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      {chartData.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">Trend Visualization</Typography>
              <ToggleButtonGroup
                value={chartType}
                exclusive
                onChange={(_, newType) => newType && setChartType(newType)}
                size="small"
              >
                <ToggleButton value="line">
                  <ChartIcon sx={{ mr: 1 }} />
                  Line Chart
                </ToggleButton>
                <ToggleButton value="area">
                  <TrendIcon sx={{ mr: 1 }} />
                  Area Chart
                </ToggleButton>
              </ToggleButtonGroup>
            </Box>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ width: '100%', height: 400 }}>
              <ResponsiveContainer width="100%" height="100%">
                {chartType === 'area' ? (
                  <AreaChart
                    data={chartData}
                    margin={{ top: 10, right: 30, left: 20, bottom: 60 }}
                  >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis
                    dataKey="date"
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    interval="preserveStartEnd"
                    tick={{ fontSize: 12 }}
                  />
                  <YAxis
                    label={{ value: `Value (${trend.resultUnits || ''})`, angle: -90, position: 'insideLeft' }}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend />
                  
                  {/* Reference range area */}
                  {trend.referenceRangeLow !== undefined && trend.referenceRangeHigh !== undefined && (
                    <ReferenceArea
                      y1={trend.referenceRangeLow}
                      y2={trend.referenceRangeHigh}
                      strokeOpacity={0.2}
                      fill="#4caf50"
                      label={{ value: 'Normal Range', position: 'insideTopRight' }}
                    />
                  )}
                  
                  {/* Reference range lines */}
                  {trend.referenceRangeLow !== undefined && (
                    <ReferenceLine
                      y={trend.referenceRangeLow}
                      stroke="#4caf50"
                      strokeDasharray="5 5"
                      label={{ value: `Low: ${trend.referenceRangeLow}`, position: 'right' }}
                    />
                  )}
                  {trend.referenceRangeHigh !== undefined && (
                    <ReferenceLine
                      y={trend.referenceRangeHigh}
                      stroke="#4caf50"
                      strokeDasharray="5 5"
                      label={{ value: `High: ${trend.referenceRangeHigh}`, position: 'right' }}
                    />
                  )}
                  
                  {/* Area chart with gradient */}
                  <Area
                    type="monotone"
                    dataKey="value"
                    stroke="#1976d2"
                    fill="#1976d2"
                    fillOpacity={0.3}
                    strokeWidth={2}
                    dot={(props: any) => {
                      const { cx, cy, payload } = props;
                      const color = getValueColor(payload.abnormalFlag, payload.isCritical);
                      const size = payload.isCritical ? 6 : payload.abnormalFlag ? 5 : 4;
                      return (
                        <circle
                          cx={cx}
                          cy={cy}
                          r={size}
                          fill={color}
                          stroke="#fff"
                          strokeWidth={2}
                        />
                      );
                    }}
                    activeDot={{ r: 8 }}
                    connectNulls
                    isAnimationActive={true}
                    name={`Value (${trend.resultUnits || ''})`}
                  />
                </AreaChart>
                ) : (
                  <LineChart
                    data={chartData}
                    margin={{ top: 10, right: 30, left: 20, bottom: 60 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                    <XAxis
                      dataKey="date"
                      angle={-45}
                      textAnchor="end"
                      height={80}
                      interval="preserveStartEnd"
                      tick={{ fontSize: 12 }}
                    />
                    <YAxis
                      label={{ value: `Value (${trend.resultUnits || ''})`, angle: -90, position: 'insideLeft' }}
                      tick={{ fontSize: 12 }}
                    />
                    <Tooltip content={<CustomTooltip />} />
                    <Legend />
                    
                    {/* Reference range area */}
                    {trend.referenceRangeLow !== undefined && trend.referenceRangeHigh !== undefined && (
                      <ReferenceArea
                        y1={trend.referenceRangeLow}
                        y2={trend.referenceRangeHigh}
                        strokeOpacity={0.2}
                        fill="#4caf50"
                        label={{ value: 'Normal Range', position: 'insideTopRight' }}
                      />
                    )}
                    
                    {/* Reference range lines */}
                    {trend.referenceRangeLow !== undefined && (
                      <ReferenceLine
                        y={trend.referenceRangeLow}
                        stroke="#4caf50"
                        strokeDasharray="5 5"
                        label={{ value: `Low: ${trend.referenceRangeLow}`, position: 'right' }}
                      />
                    )}
                    {trend.referenceRangeHigh !== undefined && (
                      <ReferenceLine
                        y={trend.referenceRangeHigh}
                        stroke="#4caf50"
                        strokeDasharray="5 5"
                        label={{ value: `High: ${trend.referenceRangeHigh}`, position: 'right' }}
                      />
                    )}
                    
                    {/* Main trend line with custom dot colors */}
                    <Line
                      type="monotone"
                      dataKey="value"
                      stroke="#1976d2"
                      strokeWidth={2}
                      dot={(props: any) => {
                        const { cx, cy, payload } = props;
                        const color = getValueColor(payload.abnormalFlag, payload.isCritical);
                        const size = payload.isCritical ? 6 : payload.abnormalFlag ? 5 : 4;
                        return (
                          <circle
                            cx={cx}
                            cy={cy}
                            r={size}
                            fill={color}
                            stroke="#fff"
                            strokeWidth={2}
                          />
                        );
                      }}
                      activeDot={{ r: 8 }}
                      connectNulls
                      isAnimationActive={true}
                      name={`Value (${trend.resultUnits || ''})`}
                    />
                  </LineChart>
                )}
              </ResponsiveContainer>
            </Box>
            
            {/* Legend for abnormal flags */}
            <Box sx={{ mt: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              <Box display="flex" alignItems="center" gap={1}>
                <Box sx={{ width: 16, height: 16, backgroundColor: '#1976d2', borderRadius: '50%' }} />
                <Typography variant="caption">Normal</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <Box sx={{ width: 16, height: 16, backgroundColor: '#f57c00', borderRadius: '50%' }} />
                <Typography variant="caption">High/Abnormal</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <Box sx={{ width: 16, height: 16, backgroundColor: '#fbc02d', borderRadius: '50%' }} />
                <Typography variant="caption">Low</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <Box sx={{ width: 16, height: 16, backgroundColor: '#d32f2f', borderRadius: '50%' }} />
                <Typography variant="caption">Critical</Typography>
              </Box>
              {trend.referenceRangeLow !== undefined && trend.referenceRangeHigh !== undefined && (
                <Box display="flex" alignItems="center" gap={1}>
                  <Box sx={{ width: 16, height: 16, backgroundColor: '#4caf50', opacity: 0.3 }} />
                  <Typography variant="caption">Reference Range</Typography>
                </Box>
              )}
            </Box>
          </CardContent>
        </Card>
      )}
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Statistics</Typography>
              <Divider sx={{ mb: 2 }} />
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    {trend.minValue !== undefined && (
                      <TableRow>
                        <TableCell><strong>Minimum</strong></TableCell>
                        <TableCell>
                          {trend.minValue.toFixed(2)} {trend.resultUnits || ''}
                        </TableCell>
                      </TableRow>
                    )}
                    {trend.maxValue !== undefined && (
                      <TableRow>
                        <TableCell><strong>Maximum</strong></TableCell>
                        <TableCell>
                          {trend.maxValue.toFixed(2)} {trend.resultUnits || ''}
                        </TableCell>
                      </TableRow>
                    )}
                    {trend.averageValue !== undefined && (
                      <TableRow>
                        <TableCell><strong>Average</strong></TableCell>
                        <TableCell>
                          {trend.averageValue.toFixed(2)} {trend.resultUnits || ''}
                        </TableCell>
                      </TableRow>
                    )}
                    {trend.medianValue !== undefined && (
                      <TableRow>
                        <TableCell><strong>Median</strong></TableCell>
                        <TableCell>
                          {trend.medianValue.toFixed(2)} {trend.resultUnits || ''}
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Reference Range</Typography>
              <Divider sx={{ mb: 2 }} />
              {trend.referenceRangeLow !== undefined && trend.referenceRangeHigh !== undefined ? (
                <Typography variant="body1">
                  {trend.referenceRangeLow} - {trend.referenceRangeHigh} {trend.resultUnits || ''}
                </Typography>
              ) : (
                <Typography variant="body2" color="text.secondary">Not available</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Data Points</Typography>
          <Divider sx={{ mb: 2 }} />
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell align="right">Value</TableCell>
                  <TableCell>Flag</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {trend.dataPoints.map((point) => (
                  <TableRow key={point.resultId}>
                    <TableCell>{new Date(point.resultDate).toLocaleString()}</TableCell>
                    <TableCell align="right">
                      {point.value !== undefined ? point.value.toFixed(2) : point.resultValue || '-'}
                      {trend.resultUnits && point.value !== undefined && ` ${trend.resultUnits}`}
                    </TableCell>
                    <TableCell>
                      {point.abnormalFlag && (
                        <Chip
                          label={point.abnormalFlag}
                          size="small"
                          color={point.abnormalFlag === 'H' || point.abnormalFlag === 'C' ? 'error' : 'warning'}
                        />
                      )}
                      {point.isCriticalValue && (
                        <Chip label="Critical" size="small" color="error" sx={{ ml: 0.5 }} />
                      )}
                    </TableCell>
                    <TableCell>{point.resultStatus}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LabResultTrend;
