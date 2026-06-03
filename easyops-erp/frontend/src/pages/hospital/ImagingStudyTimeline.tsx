import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Timeline as TimelineIcon,
  TrendingUp as TrendIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import hospitalService, { ImagingStudy } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

interface TimelineData {
  patientId: string;
  studies: ImagingStudy[];
  intervals: any[];
  timelineByPeriod: Record<string, ImagingStudy[]>;
  totalStudies: number;
}

interface TrendData {
  patientId: string;
  totalStudies: number;
  frequencyStats: {
    totalStudies: number;
    earliestStudyDate: string;
    latestStudyDate: string;
    timeSpanDays: number;
    timeSpanMonths: number;
    timeSpanYears: number;
    averageDaysBetween?: number;
    averageMonthsBetween?: number;
    studiesPerYear?: number;
  };
  intervalStats: {
    totalIntervals: number;
    minDaysBetween: number;
    maxDaysBetween: number;
    averageDaysBetween: number;
    medianDaysBetween: number;
  };
  patterns: Array<{
    type: string;
    description: string;
    severity: string;
  }>;
  studiesByModality: Record<string, number>;
  studiesByBodyPart: Record<string, number>;
  studiesByYear: Record<string, number>;
  studiesByMonth: Record<string, number>;
}

const ImagingStudyTimeline: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [timelineData, setTimelineData] = useState<TimelineData | null>(null);
  const [trendData, setTrendData] = useState<TrendData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'timeline' | 'trends'>('timeline');
  const [selectedModality, setSelectedModality] = useState<string>('');
  const [selectedBodyPart, setSelectedBodyPart] = useState<string>('');
  
  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id, selectedModality, selectedBodyPart]);
  
  const loadData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      const params: any = {};
      if (selectedModality) params.modality = selectedModality;
      if (selectedBodyPart) params.bodyPart = selectedBodyPart;
      
      const [timelineResponse, trendsResponse] = await Promise.all([
        hospitalService.getImagingStudyTimeline(id, params.modality, params.bodyPart),
        hospitalService.getImagingStudyTrends(id, params.modality, params.bodyPart),
      ]);
      
      setTimelineData(timelineResponse.data);
      setTrendData(trendsResponse.data);
    } catch (err: any) {
      console.error('Failed to load timeline/trends:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load timeline and trends data'));
      enqueueSnackbar('Failed to load timeline and trends data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const getModalityColor = (modality?: string) => {
    switch (modality) {
      case 'CT': return 'primary';
      case 'MRI': return 'secondary';
      case 'XRAY': return 'default';
      case 'ULTRASOUND': return 'info';
      case 'MAMMOGRAPHY': return 'warning';
      default: return 'default';
    }
  };
  
  // Prepare chart data for trends
  const modalityChartData = trendData?.studiesByModality
    ? Object.entries(trendData.studiesByModality).map(([modality, count]) => ({
        modality,
        count,
      }))
    : [];
  
  const bodyPartChartData = trendData?.studiesByBodyPart
    ? Object.entries(trendData.studiesByBodyPart).map(([bodyPart, count]) => ({
        bodyPart,
        count,
      }))
    : [];
  
  const monthlyChartData = trendData?.studiesByMonth
    ? Object.entries(trendData.studiesByMonth)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([month, count]) => ({
          month,
          count,
        }))
    : [];
  
  // Get unique modalities and body parts for filters
  const modalities = timelineData?.studies
    ? Array.from(new Set(timelineData.studies.map(s => s.studyModality)))
    : [];
  
  const bodyParts = timelineData?.studies
    ? Array.from(new Set(timelineData.studies.map(s => s.bodyPartExamined)))
    : [];
  
  if (loading && !timelineData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            startIcon={<BackIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/imaging-studies`)}
          >
            Back
          </Button>
          <Typography variant="h4">Imaging Study Timeline & Trends</Typography>
        </Box>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <FilterIcon />
            <Typography variant="h6">Filters</Typography>
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Modality</InputLabel>
                <Select
                  value={selectedModality}
                  label="Modality"
                  onChange={(e) => setSelectedModality(e.target.value)}
                >
                  <MenuItem value="">All Modalities</MenuItem>
                  {modalities.map((modality) => (
                    <MenuItem key={modality} value={modality}>
                      {modality}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Body Part</InputLabel>
                <Select
                  value={selectedBodyPart}
                  label="Body Part"
                  onChange={(e) => setSelectedBodyPart(e.target.value)}
                >
                  <MenuItem value="">All Body Parts</MenuItem>
                  {bodyParts.map((bodyPart) => (
                    <MenuItem key={bodyPart} value={bodyPart}>
                      {bodyPart}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <Button
                variant="outlined"
                onClick={() => {
                  setSelectedModality('');
                  setSelectedBodyPart('');
                }}
                fullWidth
              >
                Clear Filters
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      {/* View Mode Tabs */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs
            value={viewMode}
            onChange={(_, newValue) => setViewMode(newValue)}
            sx={{ mb: 3 }}
          >
            <Tab icon={<TimelineIcon />} label="Timeline" value="timeline" />
            <Tab icon={<TrendIcon />} label="Trends & Analysis" value="trends" />
          </Tabs>
          
          {viewMode === 'timeline' && timelineData && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Chronological Timeline ({timelineData.totalStudies} studies)
              </Typography>
              
              {/* Timeline by Period */}
              {Object.entries(timelineData.timelineByPeriod || {})
                .sort(([a], [b]) => b.localeCompare(a))
                .map(([period, studies]) => (
                  <Card key={period} sx={{ mb: 2 }}>
                    <CardContent>
                      <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                        {period}
                      </Typography>
                      <TableContainer>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Date</TableCell>
                              <TableCell>Study Name</TableCell>
                              <TableCell>Modality</TableCell>
                              <TableCell>Body Part</TableCell>
                              <TableCell>Accession Number</TableCell>
                              <TableCell>Status</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {studies.map((study) => (
                              <TableRow key={study.studyId} hover>
                                <TableCell>
                                  {new Date(study.studyDate).toLocaleDateString()}
                                </TableCell>
                                <TableCell>{study.studyName}</TableCell>
                                <TableCell>
                                  <Chip
                                    label={study.studyModality}
                                    size="small"
                                    color={getModalityColor(study.studyModality) as any}
                                  />
                                </TableCell>
                                <TableCell>{study.bodyPartExamined}</TableCell>
                                <TableCell>
                                  <Typography variant="body2" fontFamily="monospace">
                                    {study.accessionNumber}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <Chip
                                    label={study.studyStatus}
                                    size="small"
                                    color={
                                      study.studyStatus === 'FINAL'
                                        ? 'success'
                                        : study.studyStatus === 'PRELIMINARY'
                                        ? 'warning'
                                        : 'default'
                                    }
                                  />
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </CardContent>
                  </Card>
                ))}
              
              {/* Intervals */}
              {timelineData.intervals && timelineData.intervals.length > 0 && (
                <Card sx={{ mt: 3 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Study Intervals
                    </Typography>
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Previous Study</TableCell>
                            <TableCell>Current Study</TableCell>
                            <TableCell>Interval</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {timelineData.intervals.map((interval: any, index: number) => (
                            <TableRow key={index}>
                              <TableCell>
                                <Typography variant="body2">
                                  {interval.previousStudyName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {new Date(interval.previousStudyDate).toLocaleDateString()}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {interval.currentStudyName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {new Date(interval.currentStudyDate).toLocaleDateString()}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                {interval.daysBetween > 0 && (
                                  <Typography variant="body2">
                                    {interval.daysBetween} days
                                    {interval.monthsBetween > 0 && ` (${interval.monthsBetween} months)`}
                                  </Typography>
                                )}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
          
          {viewMode === 'trends' && trendData && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Trends & Analysis ({trendData.totalStudies} studies)
              </Typography>
              
              <Grid container spacing={3}>
                {/* Frequency Statistics */}
                <Grid item xs={12} md={6}>
                  <Card>
                    <CardContent>
                      <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                        Frequency Statistics
                      </Typography>
                      <Divider sx={{ mb: 2 }} />
                      <Grid container spacing={2}>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Time Span
                          </Typography>
                          <Typography variant="body1">
                            {trendData.frequencyStats.timeSpanYears > 0
                              ? `${trendData.frequencyStats.timeSpanYears} years`
                              : trendData.frequencyStats.timeSpanMonths > 0
                              ? `${trendData.frequencyStats.timeSpanMonths} months`
                              : `${trendData.frequencyStats.timeSpanDays} days`}
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Studies per Year
                          </Typography>
                          <Typography variant="body1">
                            {trendData.frequencyStats.studiesPerYear?.toFixed(1) || 'N/A'}
                          </Typography>
                        </Grid>
                        {trendData.frequencyStats.averageDaysBetween && (
                          <Grid item xs={12}>
                            <Typography variant="caption" color="text.secondary">
                              Average Interval
                            </Typography>
                            <Typography variant="body1">
                              {trendData.frequencyStats.averageDaysBetween.toFixed(1)} days
                            </Typography>
                          </Grid>
                        )}
                      </Grid>
                    </CardContent>
                  </Card>
                </Grid>
                
                {/* Interval Statistics */}
                <Grid item xs={12} md={6}>
                  <Card>
                    <CardContent>
                      <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                        Interval Statistics
                      </Typography>
                      <Divider sx={{ mb: 2 }} />
                      <Grid container spacing={2}>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Min Interval
                          </Typography>
                          <Typography variant="body1">
                            {trendData.intervalStats.minDaysBetween} days
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Max Interval
                          </Typography>
                          <Typography variant="body1">
                            {trendData.intervalStats.maxDaysBetween} days
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Average Interval
                          </Typography>
                          <Typography variant="body1">
                            {trendData.intervalStats.averageDaysBetween.toFixed(1)} days
                          </Typography>
                        </Grid>
                        <Grid item xs={6}>
                          <Typography variant="caption" color="text.secondary">
                            Median Interval
                          </Typography>
                          <Typography variant="body1">
                            {trendData.intervalStats.medianDaysBetween.toFixed(1)} days
                          </Typography>
                        </Grid>
                      </Grid>
                    </CardContent>
                  </Card>
                </Grid>
                
                {/* Patterns */}
                {trendData.patterns && trendData.patterns.length > 0 && (
                  <Grid item xs={12}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                          Identified Patterns
                        </Typography>
                        <Divider sx={{ mb: 2 }} />
                        <List>
                          {trendData.patterns.map((pattern, index) => (
                            <ListItem key={index}>
                              <ListItemText
                                primary={pattern.description}
                                secondary={pattern.type}
                              />
                              <Chip
                                label={pattern.severity}
                                size="small"
                                color={
                                  pattern.severity === 'HIGH'
                                    ? 'error'
                                    : pattern.severity === 'MEDIUM'
                                    ? 'warning'
                                    : 'default'
                                }
                              />
                            </ListItem>
                          ))}
                        </List>
                      </CardContent>
                    </Card>
                  </Grid>
                )}
                
                {/* Charts */}
                {modalityChartData.length > 0 && (
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                          Studies by Modality
                        </Typography>
                        <ResponsiveContainer width="100%" height={300}>
                          <BarChart data={modalityChartData}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="modality" />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Bar dataKey="count" fill="#8884d8" />
                          </BarChart>
                        </ResponsiveContainer>
                      </CardContent>
                    </Card>
                  </Grid>
                )}
                
                {bodyPartChartData.length > 0 && (
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                          Studies by Body Part
                        </Typography>
                        <ResponsiveContainer width="100%" height={300}>
                          <BarChart data={bodyPartChartData}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="bodyPart" angle={-45} textAnchor="end" height={100} />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Bar dataKey="count" fill="#82ca9d" />
                          </BarChart>
                        </ResponsiveContainer>
                      </CardContent>
                    </Card>
                  </Grid>
                )}
                
                {monthlyChartData.length > 0 && (
                  <Grid item xs={12}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                          Studies Over Time
                        </Typography>
                        <ResponsiveContainer width="100%" height={300}>
                          <LineChart data={monthlyChartData}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="month" angle={-45} textAnchor="end" height={100} />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Line type="monotone" dataKey="count" stroke="#8884d8" strokeWidth={2} />
                          </LineChart>
                        </ResponsiveContainer>
                      </CardContent>
                    </Card>
                  </Grid>
                )}
              </Grid>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default ImagingStudyTimeline;
