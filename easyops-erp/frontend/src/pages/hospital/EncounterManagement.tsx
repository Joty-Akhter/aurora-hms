import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import hospitalService, { Encounter, EncounterRequest, Patient } from '../../services/hospitalService';
import { useAuth } from '../../contexts/AuthContext';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import './Hospital.css';

function encounterApiErrorMessage(err: unknown, fallback: string): string {
  const data =
    err && typeof err === 'object' && 'response' in err
      ? (err as { response?: { data?: { message?: string; error?: string } } }).response?.data
      : undefined;
  return (typeof data?.message === 'string' && data.message) ||
    (typeof data?.error === 'string' && data.error) ||
    fallback;
}

const EncounterManagement: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [activeEncounters, setActiveEncounters] = useState<Encounter[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Encounter | null>(null);
  const [viewing, setViewing] = useState<Encounter | null>(null);
  const [filter, setFilter] = useState<'all' | 'active' | 'completed'>('all');
  const [tabValue, setTabValue] = useState(0);
  const [formData, setFormData] = useState<EncounterRequest>({
    patientId: id || '',
    encounterType: 'OFFICE_VISIT',
    startDate: new Date().toISOString().split('T')[0],
    startTime: new Date().toTimeString().slice(0, 5),
    status: 'PLANNED',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadEncounters();
    }
  }, [id, filter]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getPatient(id);
      setPatient(response.data);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
    }
  };

  const loadEncounters = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      if (filter === 'active') {
        const response = await hospitalService.getActiveEncountersByPatient(id);
        setEncounters(response.data);
        setActiveEncounters(response.data);
      } else {
        const response = await hospitalService.getEncountersByPatient(id);
        setEncounters(response.data);
        const activeResponse = await hospitalService.getActiveEncountersByPatient(id);
        setActiveEncounters(activeResponse.data);
      }
    } catch (err: any) {
      console.error('Failed to load encounters:', err);
      setError(encounterApiErrorMessage(err, 'Failed to load encounters'));
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!id || !user?.organizationId) return;
    try {
      setError(null);
      await hospitalService.createEncounter(user.organizationId, formData);
      setShowForm(false);
      resetForm();
      loadEncounters();
    } catch (err: any) {
      console.error('Failed to create encounter:', err);
      setError(encounterApiErrorMessage(err, 'Failed to create encounter'));
    }
  };

  const handleUpdate = async () => {
    if (!editing) return;
    try {
      setError(null);
      await hospitalService.updateEncounter(editing.encounterId, formData);
      setShowForm(false);
      setEditing(null);
      resetForm();
      loadEncounters();
    } catch (err: any) {
      console.error('Failed to update encounter:', err);
      setError(encounterApiErrorMessage(err, 'Failed to update encounter'));
    }
  };

  const handleDelete = async (encounterId: string) => {
    if (!window.confirm('Are you sure you want to delete this encounter?')) return;
    try {
      setError(null);
      await hospitalService.deleteEncounter(encounterId);
      loadEncounters();
    } catch (err: any) {
      console.error('Failed to delete encounter:', err);
      setError(encounterApiErrorMessage(err, 'Failed to delete encounter'));
    }
  };

  const handleStatusUpdate = async (encounterId: string, status: string) => {
    try {
      setError(null);
      await hospitalService.updateEncounterStatus(encounterId, status);
      loadEncounters();
    } catch (err: any) {
      console.error('Failed to update status:', err);
      setError(encounterApiErrorMessage(err, 'Failed to update status'));
    }
  };

  const handleEdit = (encounter: Encounter) => {
    setEditing(encounter);
    setFormData({
      patientId: encounter.patientId,
      encounterType: encounter.encounterType,
      startDate: encounter.startDate,
      startTime: encounter.startTime,
      endDate: encounter.endDate || undefined,
      endTime: encounter.endTime || undefined,
      admissionDate: encounter.admissionDate || undefined,
      admissionTime: encounter.admissionTime || undefined,
      dischargeDate: encounter.dischargeDate || undefined,
      dischargeTime: encounter.dischargeTime || undefined,
      status: encounter.status,
      locationId: encounter.locationId || undefined,
      departmentId: encounter.departmentId || undefined,
      roomNumber: encounter.roomNumber || undefined,
      bedNumber: encounter.bedNumber || undefined,
      attendingPhysicianId: encounter.attendingPhysicianId || undefined,
      admittingPhysicianId: encounter.admittingPhysicianId || undefined,
      primaryCareProviderId: encounter.primaryCareProviderId || undefined,
      referringPhysicianId: encounter.referringPhysicianId || undefined,
      chiefComplaint: encounter.chiefComplaint || undefined,
      admissionDiagnosis: encounter.admissionDiagnosis || undefined,
      primaryDiagnosis: encounter.primaryDiagnosis || undefined,
      secondaryDiagnoses: encounter.secondaryDiagnoses || undefined,
      dischargeDiagnosis: encounter.dischargeDiagnosis || undefined,
      dischargeDisposition: encounter.dischargeDisposition || undefined,
      dischargeInstructions: encounter.dischargeInstructions || undefined,
      visitReason: encounter.visitReason || undefined,
      visitType: encounter.visitType || undefined,
      serviceType: encounter.serviceType || undefined,
      insuranceProviderId: encounter.insuranceProviderId || undefined,
      insurancePolicyNumber: encounter.insurancePolicyNumber || undefined,
      authorizationNumber: encounter.authorizationNumber || undefined,
      billingStatus: encounter.billingStatus || undefined,
      notes: encounter.notes || undefined,
      specialInstructions: encounter.specialInstructions || undefined,
      isEmergency: encounter.isEmergency || undefined,
      isReadmission: encounter.isReadmission || undefined,
      readmissionReason: encounter.readmissionReason || undefined,
    });
    setShowForm(true);
  };

  const resetForm = () => {
    setFormData({
      patientId: id || '',
      encounterType: 'OFFICE_VISIT',
      startDate: new Date().toISOString().split('T')[0],
      startTime: new Date().toTimeString().slice(0, 5),
      status: 'PLANNED',
    });
    setEditing(null);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'IN_PROGRESS':
      case 'ARRIVED':
      case 'ADMITTED':
        return 'primary';
      case 'COMPLETED':
      case 'DISCHARGED':
        return 'success';
      case 'CANCELLED':
      case 'NO_SHOW':
      case 'LEFT_WITHOUT_BEING_SEEN':
        return 'error';
      default:
        return 'default';
    }
  };

  const getEncounterTypeLabel = (type: string) => {
    return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Patient Information Header */}
      {patient && (
        <Box sx={{ mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 2, boxShadow: 1 }}>
          <Box>
            <Typography variant="h5">
              {patient.fullName || '—'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              MRN: {patient.mrn} 
              {patient.dateOfBirth && ` | Age: ${formatAge(patient.dateOfBirth)}`}
              {patient.gender && ` | ${formatGenderLabel(patient.gender)}`}
            </Typography>
          </Box>
        </Box>
      )}

      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3} flexWrap="wrap" gap={2}>
        <Typography variant="h4">Encounter Management</Typography>
        <Box display="flex" gap={1} flexWrap="wrap">
          <Button variant="outlined" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              resetForm();
              setShowForm(true);
            }}
          >
            New Encounter
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 2 }}>
        <Tab label={`All Encounters (${encounters.length})`} />
        <Tab label={`Active (${activeEncounters.length})`} />
      </Tabs>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Encounter Number</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Start Date/Time</TableCell>
              <TableCell>End Date/Time</TableCell>
              <TableCell>Chief Complaint</TableCell>
              <TableCell>Attending Physician</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(tabValue === 0 ? encounters : activeEncounters).map((encounter) => (
              <TableRow key={encounter.encounterId}>
                <TableCell>{encounter.encounterNumber}</TableCell>
                <TableCell>{getEncounterTypeLabel(encounter.encounterType)}</TableCell>
                <TableCell>
                  <Chip
                    label={encounter.status.replace(/_/g, ' ')}
                    color={getStatusColor(encounter.status) as any}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {encounter.startDate} {encounter.startTime}
                </TableCell>
                <TableCell>
                  {encounter.endDate ? `${encounter.endDate} ${encounter.endTime || ''}` : '-'}
                </TableCell>
                <TableCell>{encounter.chiefComplaint || '-'}</TableCell>
                <TableCell>{encounter.attendingPhysicianName || '-'}</TableCell>
                <TableCell>
                  <IconButton size="small" onClick={() => setViewing(encounter)}>
                    <ViewIcon />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleEdit(encounter)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => handleStatusUpdate(encounter.encounterId, 'COMPLETED')}
                    disabled={encounter.status === 'COMPLETED' || encounter.status === 'DISCHARGED'}
                  >
                    <CheckCircleIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => handleDelete(encounter.encounterId)}
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Create/Edit Form Dialog */}
      <Dialog open={showForm} onClose={() => setShowForm(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editing ? 'Edit Encounter' : 'New Encounter'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Encounter Type</InputLabel>
                <Select
                  value={formData.encounterType}
                  onChange={(e) => setFormData({ ...formData, encounterType: e.target.value as any })}
                  label="Encounter Type"
                >
                  <MenuItem value="OFFICE_VISIT">Office Visit</MenuItem>
                  <MenuItem value="HOSPITAL_ADMISSION">Hospital Admission</MenuItem>
                  <MenuItem value="EMERGENCY">Emergency</MenuItem>
                  <MenuItem value="OUTPATIENT">Outpatient</MenuItem>
                  <MenuItem value="INPATIENT">Inpatient</MenuItem>
                  <MenuItem value="OBSERVATION">Observation</MenuItem>
                  <MenuItem value="SURGERY">Surgery</MenuItem>
                  <MenuItem value="CONSULTATION">Consultation</MenuItem>
                  <MenuItem value="TELEHEALTH">Telehealth</MenuItem>
                  <MenuItem value="HOME_VISIT">Home Visit</MenuItem>
                  <MenuItem value="URGENT_CARE">Urgent Care</MenuItem>
                  <MenuItem value="AMBULATORY">Ambulatory</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status || 'PLANNED'}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                  label="Status"
                >
                  <MenuItem value="PLANNED">Planned</MenuItem>
                  <MenuItem value="ARRIVED">Arrived</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="COMPLETED">Completed</MenuItem>
                  <MenuItem value="DISCHARGED">Discharged</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
                  <MenuItem value="NO_SHOW">No Show</MenuItem>
                  <MenuItem value="LEFT_WITHOUT_BEING_SEEN">Left Without Being Seen</MenuItem>
                  <MenuItem value="ADMITTED">Admitted</MenuItem>
                  <MenuItem value="TRANSFERRED">Transferred</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Start Date"
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Start Time"
                type="time"
                value={formData.startTime}
                onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="End Date"
                type="date"
                value={formData.endDate || ''}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value || undefined })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="End Time"
                type="time"
                value={formData.endTime || ''}
                onChange={(e) => setFormData({ ...formData, endTime: e.target.value || undefined })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Chief Complaint"
                multiline
                rows={2}
                value={formData.chiefComplaint || ''}
                onChange={(e) => setFormData({ ...formData, chiefComplaint: e.target.value || undefined })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Primary Diagnosis"
                value={formData.primaryDiagnosis || ''}
                onChange={(e) => setFormData({ ...formData, primaryDiagnosis: e.target.value || undefined })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Room Number"
                value={formData.roomNumber || ''}
                onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value || undefined })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Bed Number"
                value={formData.bedNumber || ''}
                onChange={(e) => setFormData({ ...formData, bedNumber: e.target.value || undefined })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notes"
                multiline
                rows={3}
                value={formData.notes || ''}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value || undefined })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowForm(false)}>Cancel</Button>
          <Button onClick={editing ? handleUpdate : handleCreate} variant="contained">
            {editing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Dialog */}
      <Dialog open={!!viewing} onClose={() => setViewing(null)} maxWidth="md" fullWidth>
        {viewing && (
          <>
            <DialogTitle>Encounter Details - {viewing.encounterNumber}</DialogTitle>
            <DialogContent>
              <Grid container spacing={2} sx={{ mt: 1 }}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2">Type</Typography>
                  <Typography>{getEncounterTypeLabel(viewing.encounterType)}</Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2">Status</Typography>
                  <Chip
                    label={viewing.status.replace(/_/g, ' ')}
                    color={getStatusColor(viewing.status) as any}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2">Start Date/Time</Typography>
                  <Typography>{viewing.startDate} {viewing.startTime}</Typography>
                </Grid>
                {viewing.endDate && (
                  <Grid item xs={12} sm={6}>
                    <Typography variant="subtitle2">End Date/Time</Typography>
                    <Typography>{viewing.endDate} {viewing.endTime || ''}</Typography>
                  </Grid>
                )}
                {viewing.chiefComplaint && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Chief Complaint</Typography>
                    <Typography>{viewing.chiefComplaint}</Typography>
                  </Grid>
                )}
                {viewing.primaryDiagnosis && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Primary Diagnosis</Typography>
                    <Typography>{viewing.primaryDiagnosis}</Typography>
                  </Grid>
                )}
                {viewing.notes && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Notes</Typography>
                    <Typography>{viewing.notes}</Typography>
                  </Grid>
                )}
                {viewing.lengthOfStayDays !== null && viewing.lengthOfStayDays !== undefined && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Length of Stay</Typography>
                    <Typography>{viewing.lengthOfStayDays} days</Typography>
                  </Grid>
                )}
              </Grid>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setViewing(null)}>Close</Button>
              <Button onClick={() => { setViewing(null); handleEdit(viewing); }} variant="contained">
                Edit
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </Box>
  );
};

export default EncounterManagement;
