import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Grid,
  MenuItem,
  Paper,
} from '@mui/material';
import { Save, Cancel } from '@mui/icons-material';
import { bomApi, BomHeader } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const BomForm: React.FC = () => {
  const { bomId } = useParams<{ bomId?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEdit = !!bomId && bomId !== 'new';

  const [formData, setFormData] = useState<Partial<BomHeader>>({
    organizationId: user?.organizationId || '',
    bomNumber: '',
    productId: '',
    productCode: '',
    productName: '',
    bomType: 'STANDARD',
    versionNumber: 1,
    status: 'DRAFT',
    baseQuantity: 1,
    uom: 'EA',
    description: '',
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadBom();
    }
  }, [bomId]);

  const loadBom = async () => {
    if (!bomId) return;
    try {
      const response = await bomApi.getBomById(bomId);
      setFormData(response.data);
    } catch (error) {
      console.error('Failed to load BOM:', error);
    }
  };

  const handleChange = (field: keyof BomHeader) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [field]: event.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isEdit) {
        await bomApi.updateBom(bomId!, formData as BomHeader);
      } else {
        const response = await bomApi.createBom(formData as BomHeader);
        navigate(`/manufacturing/boms/${response.data.bomId}`);
        return;
      }
      navigate('/manufacturing/boms');
    } catch (error) {
      console.error('Failed to save BOM:', error);
      alert('Failed to save BOM');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        {isEdit ? 'Edit BOM' : 'Create New BOM'}
      </Typography>

      <Paper sx={{ p: 3, mt: 3 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="BOM Number"
                value={formData.bomNumber || ''}
                onChange={handleChange('bomNumber')}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                select
                required
                label="BOM Type"
                value={formData.bomType || 'STANDARD'}
                onChange={handleChange('bomType')}
              >
                <MenuItem value="STANDARD">Standard</MenuItem>
                <MenuItem value="PHANTOM">Phantom</MenuItem>
                <MenuItem value="CONFIGURABLE">Configurable</MenuItem>
                <MenuItem value="ENGINEERING">Engineering</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Product Code"
                value={formData.productCode || ''}
                onChange={handleChange('productCode')}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                required
                label="Product Name"
                value={formData.productName || ''}
                onChange={handleChange('productName')}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                required
                type="number"
                label="Base Quantity"
                value={formData.baseQuantity || 1}
                onChange={handleChange('baseQuantity')}
                inputProps={{ min: 1, step: 1 }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                required
                label="UOM"
                value={formData.uom || 'EA'}
                onChange={handleChange('uom')}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                required
                label="Status"
                value={formData.status || 'DRAFT'}
                onChange={handleChange('status')}
              >
                <MenuItem value="DRAFT">Draft</MenuItem>
                <MenuItem value="PENDING_APPROVAL">Pending Approval</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Description"
                value={formData.description || ''}
                onChange={handleChange('description')}
              />
            </Grid>

            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                <Button
                  variant="outlined"
                  startIcon={<Cancel />}
                  onClick={() => navigate('/manufacturing/boms')}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<Save />}
                  disabled={loading}
                >
                  {loading ? 'Saving...' : 'Save BOM'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default BomForm;

