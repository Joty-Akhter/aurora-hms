import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Search as SearchIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  CancellationReportResponse,
  NoShowReportResponse,
  ResourceResponse,
  UtilizationDataPoint,
  UtilizationReportResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

type ReportTab = 'utilization' | 'no-show' | 'cancellations';

const SchedulingReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [tab, setTab] = useState<ReportTab>('utilization');

  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [fromDate, setFromDate] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [toDate, setToDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 13);
    return d.toISOString().slice(0, 10);
  });
  const [resourceId, setResourceId] = useState<string>('');
  const [groupBy, setGroupBy] = useState<string>('DAY');

  const [loading, setLoading] = useState<boolean>(false);
  const [utilizationResult, setUtilizationResult] = useState<UtilizationReportResponse | null>(null);
  const [noShowResult, setNoShowResult] = useState<NoShowReportResponse | null>(null);
  const [cancellationResult, setCancellationResult] = useState<CancellationReportResponse | null>(null);

  const loadResources = useCallback(async () => {
    try {
      const response = await hospitalSchedulingService.getResources({
        page: 0,
        size: 500,
        status: 'ACTIVE',
      });
      setResources(response.content);
    } catch {
      setResources([]);
    }
  }, []);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  const validateDates = (): boolean => {
    if (!fromDate.trim() || !toDate.trim()) {
      enqueueSnackbar('From date and to date are required', { variant: 'warning' });
      return false;
    }
    if (fromDate > toDate) {
      enqueueSnackbar('From date must be before or equal to to date', { variant: 'warning' });
      return false;
    }
    return true;
  };

  const handleLoadUtilization = async () => {
    if (!validateDates()) return;
    try {
      setLoading(true);
      const data = await hospitalSchedulingService.getUtilizationReport({
        fromDate: fromDate.trim(),
        toDate: toDate.trim(),
        resourceId: resourceId.trim() || undefined,
        groupBy,
      });
      setUtilizationResult(data);
    } catch (err) {
      console.error('Failed to load utilization report', err);
      enqueueSnackbar('Failed to load utilization report', { variant: 'error' });
      setUtilizationResult(null);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadNoShow = async () => {
    if (!validateDates()) return;
    try {
      setLoading(true);
      const data = await hospitalSchedulingService.getNoShowReport({
        fromDate: fromDate.trim(),
        toDate: toDate.trim(),
        resourceId: resourceId.trim() || undefined,
      });
      setNoShowResult(data);
    } catch (err) {
      console.error('Failed to load no-show report', err);
      enqueueSnackbar('Failed to load no-show report', { variant: 'error' });
      setNoShowResult(null);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadCancellations = async () => {
    if (!validateDates()) return;
    try {
      setLoading(true);
      const data = await hospitalSchedulingService.getCancellationReport({
        fromDate: fromDate.trim(),
        toDate: toDate.trim(),
        resourceId: resourceId.trim() || undefined,
      });
      setCancellationResult(data);
    } catch (err) {
      console.error('Failed to load cancellation report', err);
      enqueueSnackbar('Failed to load cancellation report', { variant: 'error' });
      setCancellationResult(null);
    } finally {
      setLoading(false);
    }
  };

  const handleLoad = () => {
    if (tab === 'utilization') handleLoadUtilization();
    else if (tab === 'no-show') handleLoadNoShow();
    else handleLoadCancellations();
  };

  const utilizationDataPoints: UtilizationDataPoint[] = utilizationResult?.dataPoints ?? [];
  const chartData = utilizationDataPoints.map((d) => ({
    date: d.date,
    slotUsed: d.slotUsed,
    slotAvailable: d.slotAvailable ?? 0,
  }));

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Reports
        </Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs value={tab} onChange={(_, v) => setTab(v as ReportTab)} sx={{ mb: 2 }}>
            <Tab label="Utilization" value="utilization" />
            <Tab label="No-show" value="no-show" />
            <Tab label="Cancellations" value="cancellations" />
          </Tabs>

          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            {tab === 'utilization' && 'Slots used vs available by resource and date (group by day or week).'}
            {tab === 'no-show' && 'No-show count and rate by resource and date range.'}
            {tab === 'cancellations' && 'Cancellation count and rate by resource and date range.'}
          </Typography>

          <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-end">
            <TextField
              label="From date"
              type="date"
              size="small"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To date"
              type="date"
              size="small"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Resource (optional)</InputLabel>
              <Select
                value={resourceId}
                onChange={(e) => setResourceId(e.target.value)}
                label="Resource (optional)"
              >
                <MenuItem value="">All resources</MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {tab === 'utilization' && (
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Group by</InputLabel>
                <Select value={groupBy} onChange={(e) => setGroupBy(e.target.value)} label="Group by">
                  <MenuItem value="DAY">Day</MenuItem>
                  <MenuItem value="WEEK">Week</MenuItem>
                </Select>
              </FormControl>
            )}
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={18} /> : <SearchIcon />}
              onClick={handleLoad}
              disabled={loading}
            >
              {loading ? 'Loading…' : 'Load'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {tab === 'utilization' && (utilizationResult || loading) && (
        <Card sx={{ mb: 3 }}>
              <CardContent>
                <Typography variant="subtitle1" gutterBottom>
                  Utilization (group by {groupBy})
                </Typography>
                {loading ? (
                  <Box display="flex" justifyContent="center" py={4}>
                    <CircularProgress />
                  </Box>
                ) : utilizationDataPoints.length > 0 ? (
                  <>
                    <Box sx={{ width: '100%', height: 320, mb: 2 }}>
                      <ResponsiveContainer>
                        <BarChart data={chartData} margin={{ top: 8, right: 8, left: 8, bottom: 8 }}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="date" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="slotUsed" name="Slots used" fill="#059669" />
                          <Bar dataKey="slotAvailable" name="Slots available" fill="#94a3b8" />
                        </BarChart>
                      </ResponsiveContainer>
                    </Box>
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Date</TableCell>
                            <TableCell align="right">Slots used</TableCell>
                            <TableCell align="right">Slots available</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {utilizationDataPoints.map((row, idx) => (
                            <TableRow key={`${row.date}-${idx}`}>
                              <TableCell>{row.date}</TableCell>
                              <TableCell align="right">{row.slotUsed}</TableCell>
                              <TableCell align="right">{row.slotAvailable ?? '—'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </>
                ) : (
                  <Typography color="text.secondary">No utilization data in the selected range.</Typography>
                )}
              </CardContent>
            </Card>
      )}

      {tab === 'no-show' && (noShowResult !== null || loading) && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              No-show report
            </Typography>
            {loading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : noShowResult ? (
              <Box component="dl" sx={{ m: 0 }}>
                <Typography component="dt" variant="body2" color="text.secondary">
                  From – To
                </Typography>
                <Typography component="dd" sx={{ mb: 1 }}>
                  {noShowResult.fromDate} – {noShowResult.toDate}
                </Typography>
                <Typography component="dt" variant="body2" color="text.secondary">
                  No-show count
                </Typography>
                <Typography component="dd" sx={{ mb: 1 }}>
                  {noShowResult.count}
                </Typography>
                {noShowResult.totalAppointmentsInRange != null && (
                  <>
                    <Typography component="dt" variant="body2" color="text.secondary">
                      Total appointments in range
                    </Typography>
                    <Typography component="dd" sx={{ mb: 1 }}>
                      {noShowResult.totalAppointmentsInRange}
                    </Typography>
                  </>
                )}
                {noShowResult.noShowRate != null && (
                  <>
                    <Typography component="dt" variant="body2" color="text.secondary">
                      No-show rate
                    </Typography>
                    <Typography component="dd">
                      {(noShowResult.noShowRate * 100).toFixed(1)}%
                    </Typography>
                  </>
                )}
              </Box>
            ) : (
              <Typography color="text.secondary">Click Load to run the report.</Typography>
            )}
          </CardContent>
        </Card>
      )}

      {tab === 'cancellations' && (cancellationResult !== null || loading) && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              Cancellation report
            </Typography>
            {loading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : cancellationResult ? (
              <Box component="dl" sx={{ m: 0 }}>
                <Typography component="dt" variant="body2" color="text.secondary">
                  From – To
                </Typography>
                <Typography component="dd" sx={{ mb: 1 }}>
                  {cancellationResult.fromDate} – {cancellationResult.toDate}
                </Typography>
                <Typography component="dt" variant="body2" color="text.secondary">
                  Cancellation count
                </Typography>
                <Typography component="dd" sx={{ mb: 1 }}>
                  {cancellationResult.count}
                </Typography>
                {cancellationResult.totalAppointmentsInRange != null && (
                  <>
                    <Typography component="dt" variant="body2" color="text.secondary">
                      Total appointments in range
                    </Typography>
                    <Typography component="dd" sx={{ mb: 1 }}>
                      {cancellationResult.totalAppointmentsInRange}
                    </Typography>
                  </>
                )}
                {cancellationResult.cancellationRate != null && (
                  <>
                    <Typography component="dt" variant="body2" color="text.secondary">
                      Cancellation rate
                    </Typography>
                    <Typography component="dd">
                      {(cancellationResult.cancellationRate * 100).toFixed(1)}%
                    </Typography>
                  </>
                )}
              </Box>
            ) : (
              <Typography color="text.secondary">Click Load to run the report.</Typography>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default SchedulingReportsPage;
