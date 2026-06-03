import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  ExpectedAdmissionsItem,
  ExpectedAdmissionsResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const SchedulingExpectedAdmissionsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [fromDate, setFromDate] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [toDate, setToDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 13);
    return d.toISOString().slice(0, 10);
  });
  const [wardOrBedClass, setWardOrBedClass] = useState<string>('');
  const [result, setResult] = useState<ExpectedAdmissionsResponse | null>(null);
  const [loaded, setLoaded] = useState<boolean>(false);

  const handleLoad = async () => {
    if (!fromDate.trim() || !toDate.trim()) {
      enqueueSnackbar('From date and to date are required', { variant: 'warning' });
      return;
    }
    if (fromDate > toDate) {
      enqueueSnackbar('From date must be before or equal to to date', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      setLoaded(true);
      const data = await hospitalSchedulingService.getExpectedAdmissions(
        fromDate.trim(),
        toDate.trim(),
        wardOrBedClass.trim() || undefined
      );
      setResult(data);
    } catch (err) {
      console.error('Failed to load expected admissions', err);
      enqueueSnackbar('Failed to load expected admissions', { variant: 'error' });
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const items: ExpectedAdmissionsItem[] = result?.items ?? [];

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Expected admissions
        </Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Expected admissions by date (for dashboards)
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
            <TextField
              label="Ward / bed class (optional)"
              size="small"
              value={wardOrBedClass}
              onChange={(e) => setWardOrBedClass(e.target.value)}
              placeholder="Filter by ward or class"
            />
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

      {loaded && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              Expected admissions
              {wardOrBedClass.trim() ? ` (ward/class: ${wardOrBedClass.trim()})` : ''}
            </Typography>
            {loading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : items.length > 0 ? (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell align="right">Count</TableCell>
                      <TableCell>Ward / bed class</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {items.map((row, idx) => (
                      <TableRow key={`${row.date}-${idx}`}>
                        <TableCell>{row.date}</TableCell>
                        <TableCell align="right">{row.count}</TableCell>
                        <TableCell>{row.wardOrBedClass ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Typography color="text.secondary">No expected admissions in the selected range.</Typography>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default SchedulingExpectedAdmissionsPage;
