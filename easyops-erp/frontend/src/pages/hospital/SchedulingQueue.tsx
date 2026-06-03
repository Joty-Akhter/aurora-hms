import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
  TextField,
  Typography,
} from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  AppointmentResponse,
  QueueResponse,
  ResourceResponse,
} from '../../services/hospitalSchedulingService';
import hospitalService, { Doctor } from '../../services/hospitalService';
import { doctorDepartmentMap, schedulingResourceDoctorLabel } from '../../utils/schedulingDoctorLabel';
import './Hospital.css';

const SchedulingQueuePage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [resourceId, setResourceId] = useState<string>('');
  const [date, setDate] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [queue, setQueue] = useState<QueueResponse | null>(null);
  const [loaded, setLoaded] = useState<boolean>(false);

  const doctorDepartmentById = useMemo(() => doctorDepartmentMap(doctors), [doctors]);

  const loadResources = useCallback(async () => {
    try {
      const [response, doctorsRes] = await Promise.all([
        hospitalSchedulingService.getResources({
          page: 0,
          size: 500,
          status: 'ACTIVE',
        }),
        hospitalService.getDoctors(),
      ]);
      setResources(response.content);
      setDoctors(doctorsRes.data);
    } catch {
      setResources([]);
      setDoctors([]);
    }
  }, []);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  const handleLoad = async () => {
    if (!resourceId.trim()) {
      enqueueSnackbar('Select a resource (doctor)', { variant: 'warning' });
      return;
    }
    if (!date.trim()) {
      enqueueSnackbar('Select a date', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      setLoaded(true);
      const data = await hospitalSchedulingService.getAppointmentQueue(resourceId.trim(), date.trim());
      setQueue(data);
    } catch (err) {
      console.error('Failed to load queue', err);
      enqueueSnackbar('Failed to load queue', { variant: 'error' });
      setQueue(null);
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (s: string) => (s ? s.slice(0, 19).replace('T', ' ') : '—');
  const selectedResource = resources.find((r) => r.id === resourceId);
  const resourceQueueTitle = selectedResource
    ? schedulingResourceDoctorLabel(selectedResource, doctorDepartmentById)
    : resourceId.trim();

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Queue
        </Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            View queue by resource and date
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-end">
            <FormControl size="small" sx={{ minWidth: 260 }}>
              <InputLabel>Resource (doctor)</InputLabel>
              <Select
                label="Resource (doctor)"
                value={resourceId}
                onChange={(e) => setResourceId(e.target.value)}
              >
                <MenuItem value="">
                  <em>Select resource</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {schedulingResourceDoctorLabel(r, doctorDepartmentById)} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Date"
              type="date"
              size="small"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={18} /> : <SearchIcon />}
              onClick={handleLoad}
              disabled={loading}
            >
              {loading ? 'Loading…' : 'Load queue'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {loaded && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              {resourceQueueTitle ? `Queue for ${resourceQueueTitle}` : 'Queue'} – {date}
            </Typography>
            {loading ? (
              <Box display="flex" justifyContent="center" alignItems="center" py={4}>
                <CircularProgress />
              </Box>
            ) : queue ? (
              queue.appointments && queue.appointments.length > 0 ? (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Token</TableCell>
                        <TableCell>Patient ID</TableCell>
                        <TableCell>Slot</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Type</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {queue.appointments.map((a: AppointmentResponse) => (
                        <TableRow key={a.id}>
                          <TableCell>{a.tokenNumber ?? '—'}</TableCell>
                          <TableCell>{a.patientId ?? '—'}</TableCell>
                          <TableCell>
                            {formatDateTime(a.slotStart)} – {formatDateTime(a.slotEnd)}
                          </TableCell>
                          <TableCell>{a.status}</TableCell>
                          <TableCell>{a.appointmentType}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Typography color="text.secondary">No appointments in queue for this date.</Typography>
              )
            ) : null}
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default SchedulingQueuePage;
