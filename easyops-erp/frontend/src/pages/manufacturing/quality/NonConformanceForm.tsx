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
  Chip,
  Card,
  CardContent,
} from '@mui/material';
import { Save, ArrowBack } from '@mui/icons-material';
import { nonConformanceApi, NonConformance } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const NonConformanceForm: React.FC = () => {
  const { ncId } = useParams<{ ncId?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [nc, setNc] = useState<Partial<NonConformance>>({
    organizationId: user?.organizationId || '',
    ncType: 'DEFECT',
    severity: 'MINOR',
    status: 'OPEN',
    description: '',
    reportedBy: user?.id,
    reportedDate: new Date().toISOString(),
  });

  useEffect(() => {
    if (ncId) {
      loadNonConformance();
    }
  }, [ncId]);

  const loadNonConformance = async () => {
    try {
      const response = await nonConformanceApi.getNonConformanceById(ncId!);
      setNc(response.data);
    } catch (error) {
      console.error('Failed to load NC:', error);
    }
  };

  const handleChange = (field: string, value: any) => {
    setNc({ ...nc, [field]: value });
  };

  const handleSave = async () => {
    try {
      if (ncId) {
        await nonConformanceApi.updateNonConformance(ncId, nc as NonConformance);
      } else {
        nc.createdBy = user!.id;
        await nonConformanceApi.createNonConformance(nc as NonConformance);
      }
      navigate('/manufacturing/quality/non-conformances');
    } catch (error) {
      console.error('Failed to save NC:', error);
    }
  };

  const handleResolve = async () => {
    try {
      await nonConformanceApi.resolveNonConformance(
        ncId!,
        nc.disposition || 'USE_AS_IS',
        nc.resolutionNotes || '',
        user!.id
      );
      navigate('/manufacturing/quality/non-conformances');
    } catch (error) {
      console.error('Failed to resolve NC:', error);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={() => navigate('/manufacturing/quality/non-conformances')} sx={{ mr: 2 }}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h4">
          {ncId ? 'Edit' : 'Create'} Non-Conformance
        </Typography>
        {nc.ncNumber && <Chip label={nc.ncNumber} sx={{ ml: 2 }} />}
      </Box>

      <Grid container spacing={3}>
        {/* Basic Information */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Basic Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <FormControl fullWidth>
                  <InputLabel>NC Type</InputLabel>
                  <Select
                    value={nc.ncType}
                    onChange={(e) => handleChange('ncType', e.target.value)}
                    label="NC Type"
                  >
                    <MenuItem value="DEFECT">Defect</MenuItem>
                    <MenuItem value="DEVIATION">Deviation</MenuItem>
                    <MenuItem value="NON_COMPLIANCE">Non-Compliance</MenuItem>
                    <MenuItem value="PROCESS_FAILURE">Process Failure</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={4}>
                <FormControl fullWidth>
                  <InputLabel>Severity</InputLabel>
                  <Select
                    value={nc.severity}
                    onChange={(e) => handleChange('severity', e.target.value)}
                    label="Severity"
                  >
                    <MenuItem value="CRITICAL">Critical</MenuItem>
                    <MenuItem value="MAJOR">Major</MenuItem>
                    <MenuItem value="MINOR">Minor</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={4}>
                <FormControl fullWidth>
                  <InputLabel>Category</InputLabel>
                  <Select
                    value={nc.category || 'MATERIAL'}
                    onChange={(e) => handleChange('category', e.target.value)}
                    label="Category"
                  >
                    <MenuItem value="MATERIAL">Material</MenuItem>
                    <MenuItem value="PROCESS">Process</MenuItem>
                    <MenuItem value="EQUIPMENT">Equipment</MenuItem>
                    <MenuItem value="HUMAN_ERROR">Human Error</MenuItem>
                    <MenuItem value="DESIGN">Design</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Description *"
                  value={nc.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  required
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Location"
                  value={nc.location || ''}
                  onChange={(e) => handleChange('location', e.target.value)}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Quantity Affected"
                  type="number"
                  value={nc.quantityAffected || ''}
                  onChange={(e) => handleChange('quantityAffected', parseFloat(e.target.value))}
                />
              </Grid>
            </Grid>
          </Paper>

          {/* Root Cause Analysis */}
          <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h6" gutterBottom>Root Cause Analysis</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Root Cause</InputLabel>
                  <Select
                    value={nc.rootCause || ''}
                    onChange={(e) => handleChange('rootCause', e.target.value)}
                    label="Root Cause"
                  >
                    <MenuItem value="MATERIAL_DEFECT">Material Defect</MenuItem>
                    <MenuItem value="PROCESS_ERROR">Process Error</MenuItem>
                    <MenuItem value="EQUIPMENT_FAILURE">Equipment Failure</MenuItem>
                    <MenuItem value="OPERATOR_ERROR">Operator Error</MenuItem>
                    <MenuItem value="DESIGN_ISSUE">Design Issue</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Root Cause Description"
                  value={nc.rootCauseDescription || ''}
                  onChange={(e) => handleChange('rootCauseDescription', e.target.value)}
                />
              </Grid>
            </Grid>
          </Paper>

          {/* CAPA (Corrective and Preventive Actions) */}
          <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h6" gutterBottom>CAPA - Corrective & Preventive Actions</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Immediate Action Taken"
                  value={nc.immediateAction || ''}
                  onChange={(e) => handleChange('immediateAction', e.target.value)}
                  helperText="What immediate action was taken to contain the problem?"
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Corrective Action"
                  value={nc.correctiveAction || ''}
                  onChange={(e) => handleChange('correctiveAction', e.target.value)}
                  helperText="What corrective action will prevent recurrence?"
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Preventive Action"
                  value={nc.preventiveAction || ''}
                  onChange={(e) => handleChange('preventiveAction', e.target.value)}
                  helperText="What preventive measures will be implemented?"
                />
              </Grid>
            </Grid>
          </Paper>

          {/* Disposition */}
          {ncId && (
            <Paper sx={{ p: 3, mt: 2 }}>
              <Typography variant="h6" gutterBottom>Disposition & Resolution</Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Disposition</InputLabel>
                    <Select
                      value={nc.disposition || ''}
                      onChange={(e) => handleChange('disposition', e.target.value)}
                      label="Disposition"
                    >
                      <MenuItem value="USE_AS_IS">Use As-Is</MenuItem>
                      <MenuItem value="REWORK">Rework</MenuItem>
                      <MenuItem value="SCRAP">Scrap</MenuItem>
                      <MenuItem value="RETURN_TO_VENDOR">Return to Vendor</MenuItem>
                      <MenuItem value="MRB_REVIEW">MRB Review</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Cost Impact"
                    type="number"
                    value={nc.costImpact || ''}
                    onChange={(e) => handleChange('costImpact', parseFloat(e.target.value))}
                    InputProps={{ startAdornment: '$' }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Resolution Notes"
                    value={nc.resolutionNotes || ''}
                    onChange={(e) => handleChange('resolutionNotes', e.target.value)}
                  />
                </Grid>
              </Grid>
            </Paper>
          )}

          {/* Actions */}
          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
            <Button onClick={() => navigate('/manufacturing/quality/non-conformances')}>
              Cancel
            </Button>
            <Button variant="outlined" startIcon={<Save />} onClick={handleSave}>
              Save
            </Button>
            {ncId && nc.status !== 'CLOSED' && (
              <Button variant="contained" color="success" onClick={handleResolve}>
                Resolve NC
              </Button>
            )}
          </Box>
        </Grid>

        {/* Summary Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>NC Summary</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Status</Typography>
                <Chip label={nc.status} color={nc.status === 'CLOSED' ? 'success' : 'warning'} sx={{ mt: 0.5 }} />
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Severity</Typography>
                <Chip 
                  label={nc.severity} 
                  color={nc.severity === 'CRITICAL' ? 'error' : nc.severity === 'MAJOR' ? 'warning' : 'default'}
                  sx={{ mt: 0.5 }}
                />
              </Box>
              {nc.quantityAffected && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" color="textSecondary">Quantity Affected</Typography>
                  <Typography variant="h6">{nc.quantityAffected}</Typography>
                </Box>
              )}
              {nc.costImpact && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" color="textSecondary">Cost Impact</Typography>
                  <Typography variant="h6" color="error.main">${nc.costImpact.toFixed(2)}</Typography>
                </Box>
              )}
            </CardContent>
          </Card>

          {/* CAPA Progress Card */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>CAPA Progress</Typography>
              <Box sx={{ mt: 2 }}>
                <Chip 
                  label={nc.immediateAction ? '✓ Immediate Action' : '○ Immediate Action'} 
                  color={nc.immediateAction ? 'success' : 'default'}
                  sx={{ mb: 1, width: '100%' }}
                />
                <Chip 
                  label={nc.correctiveAction ? '✓ Corrective Action' : '○ Corrective Action'} 
                  color={nc.correctiveAction ? 'success' : 'default'}
                  sx={{ mb: 1, width: '100%' }}
                />
                <Chip 
                  label={nc.preventiveAction ? '✓ Preventive Action' : '○ Preventive Action'} 
                  color={nc.preventiveAction ? 'success' : 'default'}
                  sx={{ width: '100%' }}
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default NonConformanceForm;

