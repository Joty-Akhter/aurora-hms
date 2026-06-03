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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  FormControl,
  InputLabel,
  Select,
} from '@mui/material';
import { Add, Delete, Save, ArrowBack } from '@mui/icons-material';
import { qualityInspectionApi, QualityInspection, QualityInspectionItem } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const QualityInspectionForm: React.FC = () => {
  const { inspectionId } = useParams<{ inspectionId?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [inspection, setInspection] = useState<Partial<QualityInspection>>({
    organizationId: user?.organizationId || '',
    inspectionType: 'IN_PROCESS',
    inspectionDate: new Date().toISOString(),
    status: 'PENDING',
    sampleSize: 1,
  });
  const [items, setItems] = useState<Partial<QualityInspectionItem>[]>([{
    organizationId: user?.organizationId || '',
    parameterName: '',
    passFail: 'PASS',
  }]);

  useEffect(() => {
    if (inspectionId) {
      loadInspection();
    }
  }, [inspectionId]);

  const loadInspection = async () => {
    try {
      const [inspResponse, itemsResponse] = await Promise.all([
        qualityInspectionApi.getInspectionById(inspectionId!),
        qualityInspectionApi.getInspectionItems(inspectionId!),
      ]);
      setInspection(inspResponse.data);
      setItems(itemsResponse.data);
    } catch (error) {
      console.error('Failed to load inspection:', error);
    }
  };

  const handleChange = (field: string, value: any) => {
    setInspection({ ...inspection, [field]: value });
  };

  const handleItemChange = (index: number, field: string, value: any) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: value };
    
    // Auto-calculate pass/fail
    if (field === 'measuredValue' || field === 'specificationMin' || field === 'specificationMax') {
      const item = newItems[index];
      if (item.measuredValue !== undefined) {
        const pass = 
          (item.specificationMin === undefined || item.measuredValue >= item.specificationMin) &&
          (item.specificationMax === undefined || item.measuredValue <= item.specificationMax);
        newItems[index].passFail = pass ? 'PASS' : 'FAIL';
      }
    }
    
    setItems(newItems);
  };

  const handleAddItem = () => {
    setItems([...items, {
      organizationId: user?.organizationId || '',
      parameterName: '',
      passFail: 'PASS',
    }]);
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    try {
      let savedInspection;
      if (inspectionId) {
        savedInspection = await qualityInspectionApi.updateInspection(inspectionId, inspection as QualityInspection);
      } else {
        inspection.createdBy = user!.id;
        savedInspection = await qualityInspectionApi.createInspection(inspection as QualityInspection);
      }

      // Save items
      for (const item of items) {
        if (item.itemId) {
          await qualityInspectionApi.updateInspectionItem(item.itemId, item as QualityInspectionItem);
        } else {
          item.qualityInspection = { inspectionId: savedInspection.data.inspectionId } as any;
          await qualityInspectionApi.addInspectionItem(item as QualityInspectionItem);
        }
      }

      navigate('/manufacturing/quality/inspections');
    } catch (error) {
      console.error('Failed to save inspection:', error);
    }
  };

  const handleComplete = async () => {
    try {
      // Calculate overall result
      const failCount = items.filter(i => i.passFail === 'FAIL').length;
      const criticalFail = items.filter(i => i.passFail === 'FAIL' && i.isCritical).length;
      
      const overallResult = criticalFail > 0 ? 'FAIL' : failCount > 0 ? 'CONDITIONAL_PASS' : 'PASS';
      
      await qualityInspectionApi.completeInspection(
        inspectionId || inspection.inspectionId!,
        overallResult,
        user!.id
      );
      
      navigate('/manufacturing/quality/inspections');
    } catch (error) {
      console.error('Failed to complete inspection:', error);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={() => navigate('/manufacturing/quality/inspections')} sx={{ mr: 2 }}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h4">
          {inspectionId ? 'Edit' : 'Create'} Quality Inspection
        </Typography>
      </Box>

      <Paper sx={{ p: 3 }}>
        <Grid container spacing={3}>
          {/* Basic Info */}
          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>Inspection Type</InputLabel>
              <Select
                value={inspection.inspectionType}
                onChange={(e) => handleChange('inspectionType', e.target.value)}
                label="Inspection Type"
              >
                <MenuItem value="RECEIVING">Receiving Inspection</MenuItem>
                <MenuItem value="IN_PROCESS">In-Process Inspection</MenuItem>
                <MenuItem value="FINAL">Final Inspection</MenuItem>
                <MenuItem value="AUDIT">Audit Inspection</MenuItem>
                <MenuItem value="FIRST_ARTICLE">First Article Inspection</MenuItem>
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Product Code"
              value={inspection.productCode || ''}
              onChange={(e) => handleChange('productCode', e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Inspector Name"
              value={inspection.inspectorName || ''}
              onChange={(e) => handleChange('inspectorName', e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Sample Size"
              type="number"
              value={inspection.sampleSize}
              onChange={(e) => handleChange('sampleSize', parseInt(e.target.value))}
            />
          </Grid>

          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              label="Lot Number"
              value={inspection.lotNumber || ''}
              onChange={(e) => handleChange('lotNumber', e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              label="Batch Number"
              value={inspection.batchNumber || ''}
              onChange={(e) => handleChange('batchNumber', e.target.value)}
            />
          </Grid>

          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              label="Serial Number"
              value={inspection.serialNumber || ''}
              onChange={(e) => handleChange('serialNumber', e.target.value)}
            />
          </Grid>

          {/* Inspection Parameters */}
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Inspection Parameters</Typography>
              <Button startIcon={<Add />} onClick={handleAddItem}>Add Parameter</Button>
            </Box>
            
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Parameter Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Spec Min</TableCell>
                    <TableCell>Spec Target</TableCell>
                    <TableCell>Spec Max</TableCell>
                    <TableCell>Measured</TableCell>
                    <TableCell>Unit</TableCell>
                    <TableCell>Result</TableCell>
                    <TableCell>Critical</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.map((item, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <TextField
                          size="small"
                          value={item.parameterName}
                          onChange={(e) => handleItemChange(index, 'parameterName', e.target.value)}
                          placeholder="Parameter name"
                        />
                      </TableCell>
                      <TableCell>
                        <Select
                          size="small"
                          value={item.parameterType || 'DIMENSION'}
                          onChange={(e) => handleItemChange(index, 'parameterType', e.target.value)}
                        >
                          <MenuItem value="DIMENSION">Dimension</MenuItem>
                          <MenuItem value="VISUAL">Visual</MenuItem>
                          <MenuItem value="FUNCTIONAL">Functional</MenuItem>
                          <MenuItem value="CHEMICAL">Chemical</MenuItem>
                        </Select>
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={item.specificationMin || ''}
                          onChange={(e) => handleItemChange(index, 'specificationMin', parseFloat(e.target.value))}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={item.specificationTarget || ''}
                          onChange={(e) => handleItemChange(index, 'specificationTarget', parseFloat(e.target.value))}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={item.specificationMax || ''}
                          onChange={(e) => handleItemChange(index, 'specificationMax', parseFloat(e.target.value))}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={item.measuredValue || ''}
                          onChange={(e) => handleItemChange(index, 'measuredValue', parseFloat(e.target.value))}
                          sx={{ width: 80 }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          value={item.specificationUnit || ''}
                          onChange={(e) => handleItemChange(index, 'specificationUnit', e.target.value)}
                          sx={{ width: 60 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={item.passFail} 
                          size="small"
                          color={item.passFail === 'PASS' ? 'success' : 'error'}
                        />
                      </TableCell>
                      <TableCell>
                        <Select
                          size="small"
                          value={item.isCritical ? 'Yes' : 'No'}
                          onChange={(e) => handleItemChange(index, 'isCritical', e.target.value === 'Yes')}
                        >
                          <MenuItem value="Yes">Yes</MenuItem>
                          <MenuItem value="No">No</MenuItem>
                        </Select>
                      </TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleRemoveItem(index)}>
                          <Delete />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Grid>

          {/* Notes */}
          <Grid item xs={12}>
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Inspection Notes"
              value={inspection.notes || ''}
              onChange={(e) => handleChange('notes', e.target.value)}
            />
          </Grid>

          {/* Actions */}
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
              <Button onClick={() => navigate('/manufacturing/quality/inspections')}>
                Cancel
              </Button>
              <Button variant="outlined" startIcon={<Save />} onClick={handleSave}>
                Save Draft
              </Button>
              <Button variant="contained" color="success" onClick={handleComplete}>
                Complete Inspection
              </Button>
            </Box>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
};

export default QualityInspectionForm;

