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
  TextField,
  Typography,
} from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalSchedulingService, {
  AvailabilityResponse,
  ResourceResponse,
  SlotTemplateResponse,
} from '../../services/hospitalSchedulingService';
import './Hospital.css';

const SchedulingAvailabilityPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [resources, setResources] = useState<ResourceResponse[]>([]);
  const [templates, setTemplates] = useState<SlotTemplateResponse[]>([]);
  const [resourceId, setResourceId] = useState<string>('');
  const [fromDate, setFromDate] = useState<string>(() =>
    new Date().toISOString().slice(0, 10)
  );
  const [toDate, setToDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 6);
    return d.toISOString().slice(0, 10);
  });
  const [slotTemplateId, setSlotTemplateId] = useState<string>('');
  const [availability, setAvailability] = useState<AvailabilityResponse[]>([]);
  const [searched, setSearched] = useState<boolean>(false);

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

  const loadTemplates = useCallback(async () => {
    try {
      const response = await hospitalSchedulingService.getSlotTemplates({
        page: 0,
        size: 200,
        status: 'ACTIVE',
      });
      setTemplates(response.content);
    } catch {
      setTemplates([]);
    }
  }, []);

  useEffect(() => {
    loadResources();
    loadTemplates();
  }, [loadResources, loadTemplates]);

  const handleSearch = async () => {
    if (!resourceId.trim()) {
      enqueueSnackbar('Select a resource', { variant: 'warning' });
      return;
    }
    if (!fromDate || !toDate) {
      enqueueSnackbar('Select from and to date', { variant: 'warning' });
      return;
    }
    if (fromDate > toDate) {
      enqueueSnackbar('From date must be before or equal to to date', {
        variant: 'warning',
      });
      return;
    }
    try {
      setLoading(true);
      setSearched(true);
      const params: { resourceId: string; fromDate: string; toDate: string; slotTemplateId?: string } = {
        resourceId: resourceId.trim(),
        fromDate,
        toDate,
      };
      if (slotTemplateId.trim()) params.slotTemplateId = slotTemplateId.trim();
      const data = await hospitalSchedulingService.getAvailability(params);
      setAvailability(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load availability', err);
      enqueueSnackbar('Failed to load availability', { variant: 'error' });
      setAvailability([]);
    } finally {
      setLoading(false);
    }
  };

  const resourceName = resources.find((r) => r.id === resourceId)?.name ?? resourceId;

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Scheduling – Availability
        </Typography>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Search availability
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-end">
            <FormControl size="small" sx={{ minWidth: 260 }}>
              <InputLabel>Resource</InputLabel>
              <Select
                label="Resource"
                value={resourceId}
                onChange={(e) => setResourceId(e.target.value)}
              >
                <MenuItem value="">
                  <em>Select resource</em>
                </MenuItem>
                {resources.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.name} ({r.resourceType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
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
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Slot template (optional)</InputLabel>
              <Select
                label="Slot template (optional)"
                value={slotTemplateId}
                onChange={(e) => setSlotTemplateId(e.target.value)}
              >
                <MenuItem value="">
                  <em>Default</em>
                </MenuItem>
                {templates.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    {t.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={18} /> : <SearchIcon />}
              onClick={handleSearch}
              disabled={loading}
            >
              {loading ? 'Loading…' : 'Load availability'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {searched && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              {resourceName ? `Availability for ${resourceName}` : 'Availability'}
            </Typography>
            {loading ? (
              <Box display="flex" justifyContent="center" alignItems="center" py={4}>
                <CircularProgress />
              </Box>
            ) : availability.length === 0 ? (
              <Typography color="text.secondary">No availability data for the selected range.</Typography>
            ) : (
              <Box display="flex" flexDirection="column" gap={3}>
                {availability.map((day) => (
                  <Box key={day.date}>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      {day.date}
                      {day.blackedOut && (
                        <Typography component="span" color="error" sx={{ ml: 1 }}>
                          (Blacked out)
                        </Typography>
                      )}
                    </Typography>
                    {day.slots && day.slots.length > 0 ? (
                      <TableContainer>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Slot start</TableCell>
                              <TableCell>Slot end</TableCell>
                              <TableCell align="right">Available count</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {day.slots.map((slot, idx) => (
                              <TableRow key={`${day.date}-${idx}`}>
                                <TableCell>
                                  {typeof slot.start === 'string'
                                    ? slot.start.slice(0, 16).replace('T', ' ')
                                    : '—'}
                                </TableCell>
                                <TableCell>
                                  {typeof slot.end === 'string'
                                    ? slot.end.slice(0, 16).replace('T', ' ')
                                    : '—'}
                                </TableCell>
                                <TableCell align="right">{slot.availableCount ?? 0}</TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        No slots
                      </Typography>
                    )}
                  </Box>
                ))}
              </Box>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default SchedulingAvailabilityPage;
