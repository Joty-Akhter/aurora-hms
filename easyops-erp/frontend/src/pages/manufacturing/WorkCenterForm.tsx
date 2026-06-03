import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Grid,
  TextField,
  MenuItem,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  Card,
  CardContent,
  Switch,
  FormControlLabel,
} from '@mui/material';
import { Save, ArrowBack } from '@mui/icons-material';
import { workCenterApi, WorkCenter } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const WorkCenterForm: React.FC = () => {
  const { workCenterId } = useParams<{ workCenterId?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [workCenter, setWorkCenter] = useState<Partial<WorkCenter>>({
    organizationId: user?.organizationId || '',
    workCenterCode: '',
    workCenterName: '',
    workCenterType: 'MACHINE',
    category: 'PRODUCTION',
    status: 'AVAILABLE',
    isActive: true,
    numberOfMachines: 1,
    numberOfOperators: 1,
    shiftsPerDay: 1,
    hoursPerShift: 8,
    workingDaysPerWeek: 5,
    efficiencyPercentage: 100,
  });

  useEffect(() => {
    if (workCenterId) {
      loadWorkCenter();
    }
  }, [workCenterId]);

  const loadWorkCenter = async () => {
    try {
      const response = await workCenterApi.getWorkCenterById(workCenterId!);
      setWorkCenter(response.data);
    } catch (error) {
      console.error('Failed to load work center:', error);
    }
  };

  const handleChange = (field: string, value: any) => {
    setWorkCenter({ ...workCenter, [field]: value });
  };

  const handleSave = async () => {
    try {
      if (workCenterId) {
        await workCenterApi.updateWorkCenter(workCenterId, workCenter as WorkCenter);
      } else {
        workCenter.createdBy = user!.id;
        await workCenterApi.createWorkCenter(workCenter as WorkCenter);
      }
      navigate('/manufacturing/work-centers');
    } catch (error) {
      console.error('Failed to save work center:', error);
    }
  };

  // Calculate daily capacity
  const calculateDailyCapacity = () => {
    const capacity = (workCenter.capacityPerHour || 0) * 
                     (workCenter.hoursPerShift || 0) * 
                     (workCenter.shiftsPerDay || 0);
    return capacity.toFixed(2);
  };

  // Calculate weekly capacity
  const calculateWeeklyCapacity = () => {
    const dailyCapacity = parseFloat(calculateDailyCapacity());
    const weeklyCapacity = dailyCapacity * (workCenter.workingDaysPerWeek || 0);
    return weeklyCapacity.toFixed(2);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={() => navigate('/manufacturing/work-centers')} sx={{ mr: 2 }}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h4">
          {workCenterId ? 'Edit' : 'Create'} Work Center
        </Typography>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          {/* Basic Information */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Basic Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Work Center Code *"
                  value={workCenter.workCenterCode}
                  onChange={(e) => handleChange('workCenterCode', e.target.value)}
                  required
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Work Center Name *"
                  value={workCenter.workCenterName}
                  onChange={(e) => handleChange('workCenterName', e.target.value)}
                  required
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Work Center Type</InputLabel>
                  <Select
                    value={workCenter.workCenterType}
                    onChange={(e) => handleChange('workCenterType', e.target.value)}
                    label="Work Center Type"
                  >
                    <MenuItem value="MACHINE">Machine</MenuItem>
                    <MenuItem value="ASSEMBLY_LINE">Assembly Line</MenuItem>
                    <MenuItem value="WORK_STATION">Work Station</MenuItem>
                    <MenuItem value="TESTING">Testing</MenuItem>
                    <MenuItem value="PACKAGING">Packaging</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Category</InputLabel>
                  <Select
                    value={workCenter.category}
                    onChange={(e) => handleChange('category', e.target.value)}
                    label="Category"
                  >
                    <MenuItem value="PRODUCTION">Production</MenuItem>
                    <MenuItem value="QUALITY">Quality</MenuItem>
                    <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                    <MenuItem value="SUPPORT">Support</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Location"
                  value={workCenter.location || ''}
                  onChange={(e) => handleChange('location', e.target.value)}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Department"
                  value={workCenter.department || ''}
                  onChange={(e) => handleChange('department', e.target.value)}
                />
              </Grid>
            </Grid>
          </Paper>

          {/* Capacity Configuration */}
          <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h6" gutterBottom>Capacity Configuration</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Capacity Per Hour"
                  type="number"
                  value={workCenter.capacityPerHour || ''}
                  onChange={(e) => handleChange('capacityPerHour', parseFloat(e.target.value))}
                  inputProps={{ min: 0, step: 0.01 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Capacity UOM"
                  value={workCenter.capacityUom || ''}
                  onChange={(e) => handleChange('capacityUom', e.target.value)}
                  placeholder="Units, KG, etc."
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Number of Machines"
                  type="number"
                  value={workCenter.numberOfMachines}
                  onChange={(e) => handleChange('numberOfMachines', parseInt(e.target.value))}
                  inputProps={{ min: 1 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Number of Operators"
                  type="number"
                  value={workCenter.numberOfOperators}
                  onChange={(e) => handleChange('numberOfOperators', parseInt(e.target.value))}
                  inputProps={{ min: 0 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Shifts Per Day"
                  type="number"
                  value={workCenter.shiftsPerDay}
                  onChange={(e) => handleChange('shiftsPerDay', parseInt(e.target.value))}
                  inputProps={{ min: 1, max: 3 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Hours Per Shift"
                  type="number"
                  value={workCenter.hoursPerShift}
                  onChange={(e) => handleChange('hoursPerShift', parseFloat(e.target.value))}
                  inputProps={{ min: 1, max: 24, step: 0.5 }}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Working Days Per Week"
                  type="number"
                  value={workCenter.workingDaysPerWeek}
                  onChange={(e) => handleChange('workingDaysPerWeek', parseInt(e.target.value))}
                  inputProps={{ min: 1, max: 7 }}
                />
              </Grid>
            </Grid>
          </Paper>

          {/* Performance & Cost */}
          <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h6" gutterBottom>Performance & Cost</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Target Efficiency %"
                  type="number"
                  value={workCenter.efficiencyPercentage}
                  onChange={(e) => handleChange('efficiencyPercentage', parseFloat(e.target.value))}
                  inputProps={{ min: 0, max: 100, step: 0.1 }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="OEE Target %"
                  type="number"
                  value={workCenter.oeeTarget || ''}
                  onChange={(e) => handleChange('oeeTarget', parseFloat(e.target.value))}
                  inputProps={{ min: 0, max: 100, step: 0.1 }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Cost Per Hour"
                  type="number"
                  value={workCenter.costPerHour || ''}
                  onChange={(e) => handleChange('costPerHour', parseFloat(e.target.value))}
                  InputProps={{ startAdornment: '$' }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Setup Cost"
                  type="number"
                  value={workCenter.setupCost || ''}
                  onChange={(e) => handleChange('setupCost', parseFloat(e.target.value))}
                  InputProps={{ startAdornment: '$' }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Overhead Rate %"
                  type="number"
                  value={workCenter.overheadRate || ''}
                  onChange={(e) => handleChange('overheadRate', parseFloat(e.target.value))}
                  inputProps={{ min: 0, step: 0.1 }}
                />
              </Grid>
            </Grid>
          </Paper>

          {/* Maintenance */}
          <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h6" gutterBottom>Maintenance Schedule</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Maintenance Frequency (Days)"
                  type="number"
                  value={workCenter.maintenanceFrequencyDays || ''}
                  onChange={(e) => handleChange('maintenanceFrequencyDays', parseInt(e.target.value))}
                  inputProps={{ min: 1 }}
                  helperText="Days between preventive maintenance"
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Current Status</InputLabel>
                  <Select
                    value={workCenter.status}
                    onChange={(e) => handleChange('status', e.target.value)}
                    label="Current Status"
                  >
                    <MenuItem value="AVAILABLE">Available</MenuItem>
                    <MenuItem value="IN_USE">In Use</MenuItem>
                    <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                    <MenuItem value="DOWN">Down</MenuItem>
                    <MenuItem value="INACTIVE">Inactive</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </Paper>

          {/* Actions */}
          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
            <Button onClick={() => navigate('/manufacturing/work-centers')}>Cancel</Button>
            <Button variant="contained" startIcon={<Save />} onClick={handleSave}>
              Save Work Center
            </Button>
          </Box>
        </Grid>

        {/* Summary Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Capacity Summary</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Daily Capacity</Typography>
                <Typography variant="h6">
                  {calculateDailyCapacity()} {workCenter.capacityUom || 'units'}/day
                </Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Weekly Capacity</Typography>
                <Typography variant="h6">
                  {calculateWeeklyCapacity()} {workCenter.capacityUom || 'units'}/week
                </Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Total Hours/Day</Typography>
                <Typography variant="h6">
                  {(workCenter.hoursPerShift || 0) * (workCenter.shiftsPerDay || 0)} hours
                </Typography>
              </Box>
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Status</Typography>
              <FormControlLabel
                control={
                  <Switch
                    checked={workCenter.isActive}
                    onChange={(e) => handleChange('isActive', e.target.checked)}
                  />
                }
                label="Active"
              />
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Current Status</Typography>
                <Typography variant="h6">{workCenter.status}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default WorkCenterForm;

