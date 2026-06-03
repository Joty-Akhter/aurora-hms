import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  MenuItem,
  Select,
  InputLabel,
  FormControl,
  CircularProgress,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalPharmacyService, {
  PharmacyLocation,
  PharmacyLocationRequest,
  PharmacyWorkflowType,
} from '../../services/hospitalPharmacyService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import {
  defaultWeeklyHours,
  formatOperationalHoursDisplay,
  parseOperationalHours,
  serializeOperationalHours,
  WEEKDAY_ROWS,
  type WeeklyHoursState,
} from '../../utils/pharmacyOperationalHours';
import './Hospital.css';

const WORKFLOW_TYPE_LABELS: Record<PharmacyWorkflowType, string> = {
  SUPPLIER: 'Supplier',
  CENTRAL_STORE: 'Central Store',
  OUTLET_PHARMACY: 'Outlet Pharmacy',
};

const PharmacyLocationsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(true);
  const [locations, setLocations] = useState<PharmacyLocation[]>([]);
  const [activeOnly, setActiveOnly] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingLocation, setEditingLocation] = useState<PharmacyLocation | null>(null);
  const [weeklyHours, setWeeklyHours] = useState<WeeklyHoursState>(defaultWeeklyHours());
  const [manualHoursNote, setManualHoursNote] = useState('');
  const [form, setForm] = useState<PharmacyLocationRequest>({
    name: '',
    type: 'OPD',
    workflowType: '',
    is24x7: false,
    operationalHours: '',
    active: true,
  });

  useEffect(() => {
    loadLocations();
  }, [activeOnly]);

  const loadLocations = async () => {
    try {
      setLoading(true);
      const params: any = {};
      if (activeOnly) params.activeOnly = true;
      const data = await hospitalPharmacyService.getPharmacies(params);
      setLocations(data);
    } catch (err: any) {
      console.error('Failed to load pharmacy locations:', err);
      enqueueSnackbar('Failed to load pharmacy locations', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingLocation(null);
    setWeeklyHours(defaultWeeklyHours());
    setManualHoursNote('');
    setForm({
      name: '',
      type: 'OPD',
      workflowType: '',
      is24x7: false,
      operationalHours: '',
      active: true,
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (loc: PharmacyLocation) => {
    const parsed = parseOperationalHours(loc.operationalHours);
    setEditingLocation(loc);
    setWeeklyHours(parsed.weekly);
    setManualHoursNote(parsed.manualNote);
    setForm({
      name: loc.name,
      type: loc.type,
      workflowType: loc.workflowType || '',
      is24x7: loc.is24x7,
      operationalHours: loc.operationalHours,
      active: loc.active,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (!form.name || !form.type) {
        enqueueSnackbar('Name and type are required', { variant: 'warning' });
        return;
      }
      const payload: PharmacyLocationRequest = {
        ...form,
        operationalHours: form.is24x7
          ? ''
          : serializeOperationalHours(weeklyHours, manualHoursNote),
      };
      if (editingLocation) {
        await hospitalPharmacyService.updatePharmacy(editingLocation.id, payload);
        enqueueSnackbar('Pharmacy location updated', { variant: 'success' });
      } else {
        await hospitalPharmacyService.createPharmacy(payload);
        enqueueSnackbar('Pharmacy location created', { variant: 'success' });
      }
      setDialogOpen(false);
      await loadLocations();
    } catch (err: any) {
      console.error('Failed to save pharmacy location:', err);
      const message = ehrApiErrorMessage(err, 'Failed to save pharmacy location');
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  if (loading) {
    return (
      <Box className="hospital-page" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4" component="h1">
            Pharmacy Locations
          </Typography>
          <Typography variant="body2">
            Configure OPD, IPD, main store, and ward pharmacy locations.
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button
            variant="contained"
            color="secondary"
            startIcon={<RefreshIcon />}
            onClick={loadLocations}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleOpenCreate}
          >
            Add Location
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1">Location filters</Typography>
          <FormControlLabel
            control={
              <Switch
                checked={activeOnly}
                onChange={(e) => setActiveOnly(e.target.checked)}
                color="primary"
              />
            }
            label="Active only"
          />
        </CardContent>
      </Card>

      <TableContainer component={Card}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Workflow Type</TableCell>
              <TableCell>24x7</TableCell>
              <TableCell>Operational Hours</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {locations.map((loc) => (
              <TableRow key={loc.id} hover>
                <TableCell>{loc.name}</TableCell>
                <TableCell>{loc.type}</TableCell>
                <TableCell>
                  {loc.workflowType
                    ? WORKFLOW_TYPE_LABELS[loc.workflowType as PharmacyWorkflowType] || loc.workflowType
                    : <Typography variant="body2" color="text.secondary">—</Typography>}
                </TableCell>
                <TableCell>
                  {loc.is24x7 ? (
                    <Chip size="small" label="24x7" color="success" />
                  ) : (
                    <Chip size="small" label="Scheduled" variant="outlined" />
                  )}
                </TableCell>
                <TableCell>{loc.is24x7 ? 'Open 24×7' : formatOperationalHoursDisplay(loc.operationalHours) || '—'}</TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={loc.active ? 'Active' : 'Inactive'}
                    color={loc.active ? 'success' : 'default'}
                  />
                </TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    variant="text"
                    startIcon={<EditIcon />}
                    onClick={() => handleOpenEdit(loc)}
                  >
                    Edit
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {locations.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No pharmacy locations configured yet. Add your first location to begin.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingLocation ? 'Edit Pharmacy Location' : 'Add Pharmacy Location'}</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <TextField
              label="Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select
                label="Type"
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value as string })}
              >
                <MenuItem value="OPD">OPD Pharmacy</MenuItem>
                <MenuItem value="IPD">IPD Pharmacy</MenuItem>
                <MenuItem value="store">Main Store</MenuItem>
                <MenuItem value="ward_store">Ward Store</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Workflow Type</InputLabel>
              <Select
                label="Workflow Type"
                value={form.workflowType || ''}
                onChange={(e) => setForm({ ...form, workflowType: e.target.value as string })}
              >
                <MenuItem value=""><em>Not specified</em></MenuItem>
                <MenuItem value="SUPPLIER">Supplier</MenuItem>
                <MenuItem value="CENTRAL_STORE">Central Store</MenuItem>
                <MenuItem value="OUTLET_PHARMACY">Outlet Pharmacy</MenuItem>
              </Select>
            </FormControl>
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.is24x7}
                  onChange={(e) => setForm({ ...form, is24x7: e.target.checked })}
                  color="primary"
                />
              }
              label="Open 24x7"
            />
            {!form.is24x7 && (
              <Box gridColumn="1 / -1" display="flex" flexDirection="column" gap={1}>
                <Typography variant="subtitle2">Weekly hours</Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Day</TableCell>
                      <TableCell>Open</TableCell>
                      <TableCell>Close</TableCell>
                      <TableCell>Closed</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {WEEKDAY_ROWS.map(({ key, label }) => (
                      <TableRow key={key}>
                        <TableCell>{label}</TableCell>
                        <TableCell>
                          <TextField
                            type="time"
                            size="small"
                            value={weeklyHours[key].open}
                            disabled={weeklyHours[key].closed}
                            onChange={(e) =>
                              setWeeklyHours((prev) => ({
                                ...prev,
                                [key]: { ...prev[key], open: e.target.value },
                              }))
                            }
                            InputLabelProps={{ shrink: true }}
                          />
                        </TableCell>
                        <TableCell>
                          <TextField
                            type="time"
                            size="small"
                            value={weeklyHours[key].close}
                            disabled={weeklyHours[key].closed}
                            onChange={(e) =>
                              setWeeklyHours((prev) => ({
                                ...prev,
                                [key]: { ...prev[key], close: e.target.value },
                              }))
                            }
                            InputLabelProps={{ shrink: true }}
                          />
                        </TableCell>
                        <TableCell>
                          <Checkbox
                            checked={weeklyHours[key].closed}
                            onChange={(e) =>
                              setWeeklyHours((prev) => ({
                                ...prev,
                                [key]: { ...prev[key], closed: e.target.checked },
                              }))
                            }
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
                <TextField
                  label="Additional notes (optional)"
                  placeholder="e.g., closed on public holidays, special Ramadan hours"
                  value={manualHoursNote}
                  onChange={(e) => setManualHoursNote(e.target.value)}
                  multiline
                  minRows={2}
                  fullWidth
                />
              </Box>
            )}
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                  color="primary"
                />
              }
              label="Active"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleSave}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PharmacyLocationsPage;

