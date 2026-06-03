import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  Paper,
  TextField,
  Typography,
  Grid,
  MenuItem,
} from '@mui/material';
import { Save, Cancel } from '@mui/icons-material';
import { routingApi, ProductRouting } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const ProductRoutingForm: React.FC = () => {
  const { routingId } = useParams<{ routingId?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEdit = !!routingId && routingId !== 'new';

  const [formData, setFormData] = useState<Partial<ProductRouting>>({
    organizationId: user?.organizationId || '',
    productId: '',
    productCode: '',
    operationCode: '',
    operationName: '',
    operationSequence: 10,
    workCenterId: '',
    workCenterCode: '',
    setupTime: 0,
    runTimePerUnit: 0,
    setupCost: 0,
    laborCostPerHour: 0,
    overheadCostPerHour: 0,
    description: '',
    isActive: true,
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadRouting();
    }
  }, [routingId]);

  const loadRouting = async () => {
    if (!routingId) return;
    try {
      const response = await routingApi.getRoutingById(routingId);
      setFormData(response.data);
    } catch (error) {
      console.error('Failed to load routing:', error);
    }
  };

  const handleChange = (field: keyof ProductRouting) => (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setFormData({ ...formData, [field]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isEdit) {
        await routingApi.updateRouting(routingId!, formData as ProductRouting);
      } else {
        await routingApi.createRouting(formData as ProductRouting);
      }
      navigate('/manufacturing/routings');
    } catch (error) {
      console.error('Failed to save routing:', error);
      alert('Failed to save routing');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        {isEdit ? 'Edit Product Routing' : 'Create New Product Routing'}
      </Typography>

      <Paper sx={{ p: 3, mt: 3 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Product Code"
                value={formData.productCode || ''}
                onChange={handleChange('productCode')}
                helperText="Code of the product being manufactured"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Operation Code"
                value={formData.operationCode || ''}
                onChange={handleChange('operationCode')}
                helperText="Unique code for this operation"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Operation Name"
                value={formData.operationName || ''}
                onChange={handleChange('operationName')}
                helperText="e.g., Cutting, Assembly, Welding"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                type="number"
                label="Operation Sequence"
                value={formData.operationSequence || 10}
                onChange={handleChange('operationSequence')}
                inputProps={{ min: 1, step: 10 }}
                helperText="Order in which operations are performed"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Work Center Code"
                value={formData.workCenterCode || ''}
                onChange={handleChange('workCenterCode')}
                helperText="Work center where operation is performed"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                select
                label="Status"
                value={formData.isActive ? 'active' : 'inactive'}
                onChange={(e) => setFormData({ ...formData, isActive: e.target.value === 'active' })}
              >
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="inactive">Inactive</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Time & Cost Information
              </Typography>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="number"
                label="Setup Time (minutes)"
                value={formData.setupTime || 0}
                onChange={handleChange('setupTime')}
                inputProps={{ min: 0, step: 1 }}
                helperText="One-time setup required before production"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="number"
                label="Run Time per Unit (minutes)"
                value={formData.runTimePerUnit || 0}
                onChange={handleChange('runTimePerUnit')}
                inputProps={{ min: 0, step: 0.1 }}
                helperText="Time to process one unit"
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="number"
                label="Setup Cost ($)"
                value={formData.setupCost || 0}
                onChange={handleChange('setupCost')}
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="number"
                label="Labor Cost per Hour ($)"
                value={formData.laborCostPerHour || 0}
                onChange={handleChange('laborCostPerHour')}
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="number"
                label="Overhead Cost per Hour ($)"
                value={formData.overheadCostPerHour || 0}
                onChange={handleChange('overheadCostPerHour')}
                inputProps={{ min: 0, step: 0.01 }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Description / Instructions"
                value={formData.description || ''}
                onChange={handleChange('description')}
                helperText="Detailed operation instructions"
              />
            </Grid>

            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                <Button
                  variant="outlined"
                  startIcon={<Cancel />}
                  onClick={() => navigate('/manufacturing/routings')}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<Save />}
                  disabled={loading}
                >
                  {loading ? 'Saving...' : 'Save Routing'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default ProductRoutingForm;

