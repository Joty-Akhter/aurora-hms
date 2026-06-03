import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Grid,
  Card,
  CardContent,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
} from '@mui/material';
import { Add, Edit, CheckCircle, PlayArrow } from '@mui/icons-material';
import { equipmentMaintenanceApi, workCenterApi, EquipmentMaintenance, WorkCenter } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const MaintenanceCalendar: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [maintenanceList, setMaintenanceList] = useState<EquipmentMaintenance[]>([]);
  const [workCenters, setWorkCenters] = useState<WorkCenter[]>([]);
  const [overdueMaintenance, setOverdueMaintenance] = useState<EquipmentMaintenance[]>([]);
  const [dialog, setDialog] = useState(false);
  const [newMaintenance, setNewMaintenance] = useState<Partial<EquipmentMaintenance>>({
    organizationId: user?.organizationId || '',
    maintenanceType: 'PREVENTIVE',
    priority: 'MEDIUM',
    status: 'SCHEDULED',
    description: '',
  });

  useEffect(() => {
    if (user?.organizationId) {
      loadData();
    }
  }, [user?.organizationId]);

  const loadData = async () => {
    try {
      const [maintenanceResponse, workCentersResponse, overdueResponse] = await Promise.all([
        equipmentMaintenanceApi.getAllMaintenance(user!.organizationId),
        workCenterApi.getAllWorkCenters(user!.organizationId),
        equipmentMaintenanceApi.getOverdueMaintenance(),
      ]);
      setMaintenanceList(maintenanceResponse.data);
      setWorkCenters(workCentersResponse.data);
      setOverdueMaintenance(overdueResponse.data);
    } catch (error) {
      console.error('Failed to load data:', error);
    }
  };

  const handleCreateMaintenance = async () => {
    try {
      newMaintenance.createdBy = user!.id;
      await equipmentMaintenanceApi.createMaintenance(newMaintenance as EquipmentMaintenance);
      setDialog(false);
      setNewMaintenance({
        organizationId: user?.organizationId || '',
        maintenanceType: 'PREVENTIVE',
        priority: 'MEDIUM',
        status: 'SCHEDULED',
        description: '',
      });
      loadData();
    } catch (error) {
      console.error('Failed to create maintenance:', error);
    }
  };

  const handleStartMaintenance = async (maintenanceId: string) => {
    try {
      await equipmentMaintenanceApi.startMaintenance(maintenanceId, user!.id);
      loadData();
    } catch (error) {
      console.error('Failed to start maintenance:', error);
    }
  };

  const handleCompleteMaintenance = async (maintenanceId: string) => {
    try {
      await equipmentMaintenanceApi.completeMaintenance(maintenanceId, user!.id);
      loadData();
    } catch (error) {
      console.error('Failed to complete maintenance:', error);
    }
  };

  const getStatusColor = (status?: string) => {
    const colors: Record<string, any> = {
      'SCHEDULED': 'info',
      'IN_PROGRESS': 'primary',
      'COMPLETED': 'success',
      'OVERDUE': 'error',
      'CANCELLED': 'default',
    };
    return colors[status || 'SCHEDULED'] || 'default';
  };

  const getPriorityColor = (priority?: string) => {
    const colors: Record<string, any> = {
      'URGENT': 'error',
      'HIGH': 'warning',
      'MEDIUM': 'info',
      'LOW': 'default',
    };
    return colors[priority || 'MEDIUM'] || 'default';
  };

  // Group maintenance by month
  const groupedMaintenance = maintenanceList.reduce((groups, maintenance) => {
    const date = new Date(maintenance.scheduledDate);
    const monthYear = `${date.toLocaleString('default', { month: 'long' })} ${date.getFullYear()}`;
    if (!groups[monthYear]) {
      groups[monthYear] = [];
    }
    groups[monthYear].push(maintenance);
    return groups;
  }, {} as Record<string, EquipmentMaintenance[]>);

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Maintenance Schedule</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setDialog(true)}>
          Schedule Maintenance
        </Button>
      </Box>

      <Grid container spacing={3}>
        {/* Overdue Alerts */}
        {overdueMaintenance.length > 0 && (
          <Grid item xs={12}>
            <Card sx={{ bgcolor: 'error.light', color: 'error.contrastText' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  ⚠️ Overdue Maintenance ({overdueMaintenance.length})
                </Typography>
                <List dense>
                  {overdueMaintenance.slice(0, 5).map((maint) => (
                    <ListItem key={maint.maintenanceId}>
                      <ListItemText
                        primary={`${maint.maintenanceNumber} - ${workCenters.find(wc => wc.workCenterId === maint.workCenterId)?.workCenterName || ''}`}
                        secondary={`Scheduled: ${new Date(maint.scheduledDate).toLocaleDateString()}`}
                      />
                      <ListItemSecondaryAction>
                        <IconButton onClick={() => handleStartMaintenance(maint.maintenanceId!)}>
                          <PlayArrow />
                        </IconButton>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Grid>
        )}

        {/* Maintenance Calendar View */}
        <Grid item xs={12}>
          {Object.entries(groupedMaintenance).map(([monthYear, maintenances]) => (
            <Paper key={monthYear} sx={{ p: 2, mb: 2 }}>
              <Typography variant="h6" gutterBottom>{monthYear}</Typography>
              <List>
                {maintenances.map((maint) => {
                  const workCenter = workCenters.find(wc => wc.workCenterId === maint.workCenterId);
                  return (
                    <ListItem key={maint.maintenanceId} divider>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body1">{maint.maintenanceNumber}</Typography>
                            <Chip label={maint.maintenanceType} size="small" />
                            <Chip label={maint.priority} size="small" color={getPriorityColor(maint.priority)} />
                            <Chip label={maint.status} size="small" color={getStatusColor(maint.status)} />
                          </Box>
                        }
                        secondary={
                          <>
                            <Typography variant="body2">
                              Work Center: {workCenter?.workCenterCode} - {workCenter?.workCenterName}
                            </Typography>
                            <Typography variant="body2">
                              Scheduled: {new Date(maint.scheduledDate).toLocaleString()} 
                              ({maint.scheduledDurationHours || 0}h)
                            </Typography>
                            <Typography variant="body2">
                              {maint.description}
                            </Typography>
                          </>
                        }
                      />
                      <ListItemSecondaryAction>
                        {maint.status === 'SCHEDULED' && (
                          <IconButton onClick={() => handleStartMaintenance(maint.maintenanceId!)}>
                            <PlayArrow />
                          </IconButton>
                        )}
                        {maint.status === 'IN_PROGRESS' && (
                          <IconButton color="success" onClick={() => handleCompleteMaintenance(maint.maintenanceId!)}>
                            <CheckCircle />
                          </IconButton>
                        )}
                        <IconButton onClick={() => navigate(`/manufacturing/maintenance/${maint.maintenanceId}/edit`)}>
                          <Edit />
                        </IconButton>
                      </ListItemSecondaryAction>
                    </ListItem>
                  );
                })}
              </List>
            </Paper>
          ))}
        </Grid>
      </Grid>

      {/* Create Maintenance Dialog */}
      <Dialog open={dialog} onClose={() => setDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Schedule Maintenance</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Work Center</InputLabel>
                <Select
                  value={newMaintenance.workCenterId || ''}
                  onChange={(e) => setNewMaintenance({ ...newMaintenance, workCenterId: e.target.value })}
                  label="Work Center"
                >
                  {workCenters.map((wc) => (
                    <MenuItem key={wc.workCenterId} value={wc.workCenterId}>
                      {wc.workCenterCode} - {wc.workCenterName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Maintenance Type</InputLabel>
                <Select
                  value={newMaintenance.maintenanceType}
                  onChange={(e) => setNewMaintenance({ ...newMaintenance, maintenanceType: e.target.value })}
                  label="Maintenance Type"
                >
                  <MenuItem value="PREVENTIVE">Preventive</MenuItem>
                  <MenuItem value="CORRECTIVE">Corrective</MenuItem>
                  <MenuItem value="PREDICTIVE">Predictive</MenuItem>
                  <MenuItem value="BREAKDOWN">Breakdown</MenuItem>
                  <MenuItem value="CALIBRATION">Calibration</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Scheduled Date"
                type="datetime-local"
                value={newMaintenance.scheduledDate || ''}
                onChange={(e) => setNewMaintenance({ ...newMaintenance, scheduledDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Estimated Duration (hours)"
                type="number"
                value={newMaintenance.scheduledDurationHours || ''}
                onChange={(e) => setNewMaintenance({ ...newMaintenance, scheduledDurationHours: parseFloat(e.target.value) })}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description *"
                value={newMaintenance.description}
                onChange={(e) => setNewMaintenance({ ...newMaintenance, description: e.target.value })}
                required
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Priority</InputLabel>
                <Select
                  value={newMaintenance.priority}
                  onChange={(e) => setNewMaintenance({ ...newMaintenance, priority: e.target.value })}
                  label="Priority"
                >
                  <MenuItem value="LOW">Low</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="URGENT">Urgent</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateMaintenance} variant="contained">Schedule</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MaintenanceCalendar;

