import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
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
  TextField,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import hospitalClinicalOrdersService, {
  TatReportItem,
  VolumeReportItem,
} from '../../services/hospitalClinicalOrdersService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ClinicalOrdersReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [orderTypeFilter, setOrderTypeFilter] = useState('');
  const [groupBy, setGroupBy] = useState<'orderType' | 'department'>('orderType');
  const [loading, setLoading] = useState(false);
  const [tatData, setTatData] = useState<TatReportItem[]>([]);
  const [volumeData, setVolumeData] = useState<VolumeReportItem[]>([]);

  const loadReports = async () => {
    if (!fromDate || !toDate) {
      enqueueSnackbar('Please select a from and to date', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      // Full-day range in UTC so backend exclusive "to" includes the selected end date
      const fromIso = `${fromDate}T00:00:00.000Z`;
      const toIso = `${toDate}T23:59:59.999Z`;

      const [tat, volumes] = await Promise.all([
        hospitalClinicalOrdersService.getTatReport({
          from: fromIso,
          to: toIso,
          orderType: orderTypeFilter || undefined,
        }),
        hospitalClinicalOrdersService.getVolumeReport({
          from: fromIso,
          to: toIso,
          groupBy,
        }),
      ]);
      setTatData(tat);
      setVolumeData(volumes);
    } catch (err: any) {
      console.error('Load clinical orders reports failed', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to load clinical orders reports'), {
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const today = new Date();
    const last7 = new Date();
    last7.setDate(today.getDate() - 7);
    setToDate(today.toISOString().slice(0, 10));
    setFromDate(last7.toISOString().slice(0, 10));
  }, []);

  useEffect(() => {
    if (fromDate && toDate) {
      loadReports();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fromDate, toDate, groupBy]);

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Clinical Orders – Reports</Typography>
          <Typography variant="body2" color="text.secondary">
            Turnaround time and volume analytics for clinical orders, grouped by order type or department.
          </Typography>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              size="small"
              type="date"
              label="From date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 180 }}
            />
            <TextField
              size="small"
              type="date"
              label="To date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 180 }}
            />
            <TextField
              size="small"
              label="Order type (LAB/RADIOLOGY/PROCEDURE)"
              value={orderTypeFilter}
              onChange={(e) => setOrderTypeFilter(e.target.value)}
              sx={{ width: 260 }}
            />
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Group volumes by</InputLabel>
              <Select
                value={groupBy}
                label="Group volumes by"
                onChange={(e) => setGroupBy(e.target.value as 'orderType' | 'department')}
              >
                <MenuItem value="orderType">Order type</MenuItem>
                <MenuItem value="department">Ordering department</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      <Box display="flex" flexDirection={{ xs: 'column', md: 'row' }} gap={3}>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Turnaround time (hours)
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Calculated from order created time to result availability or completion.
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Order type</TableCell>
                    <TableCell align="right">Count</TableCell>
                    <TableCell align="right">Avg TAT (hours)</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={3}>Loading…</TableCell>
                    </TableRow>
                  ) : tatData.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={3} align="center">
                        No data for selected filters.
                      </TableCell>
                    </TableRow>
                  ) : (
                    tatData.map((row) => (
                      <TableRow key={row.orderType}>
                        <TableCell>{row.orderType}</TableCell>
                        <TableCell align="right">{row.count}</TableCell>
                        <TableCell align="right">
                          {row.avgTatHours != null ? row.avgTatHours.toFixed(2) : '—'}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Order volumes
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Total orders in the period grouped by {groupBy === 'orderType' ? 'order type' : 'ordering department'}.
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{groupBy === 'orderType' ? 'Order type' : 'Department ID'}</TableCell>
                    <TableCell align="right">Count</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={2}>Loading…</TableCell>
                    </TableRow>
                  ) : volumeData.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={2} align="center">
                        No data for selected filters.
                      </TableCell>
                    </TableRow>
                  ) : (
                    volumeData.map((row, idx) => (
                      <TableRow key={`${row.groupKey ?? 'NULL'}-${idx}`}>
                        <TableCell>{row.groupKey || '—'}</TableCell>
                        <TableCell align="right">{row.count}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};

export default ClinicalOrdersReportsPage;

