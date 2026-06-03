import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Button,
  Paper,
  TextField,
  Grid,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
} from '@mui/material';
import { workOrderApi, bomApi, BomHeader, WorkOrder } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const steps = ['Basic Info', 'Review & Confirm', 'Complete'];

const WorkOrderWizard: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [activeStep, setActiveStep] = useState(0);
  const [boms, setBoms] = useState<BomHeader[]>([]);
  const [bomExplosion, setBomExplosion] = useState<any>(null);
  
  const [formData, setFormData] = useState<Partial<WorkOrder>>({
    organizationId: user?.organizationId || '',
    productId: '',
    bomId: '',
    quantityPlanned: 1,
    orderType: 'PRODUCTION',
    priority: 'MEDIUM',
    status: 'CREATED',
  });

  useEffect(() => {
    if (user?.organizationId) {
      loadBoms();
    }
  }, [user?.organizationId]);

  useEffect(() => {
    if (formData.bomId && formData.quantityPlanned) {
      loadBomExplosion();
    }
  }, [formData.bomId, formData.quantityPlanned]);

  const loadBoms = async () => {
    try {
      const response = await bomApi.getActiveBoms(user!.organizationId);
      setBoms(response.data);
    } catch (error) {
      console.error('Failed to load BOMs:', error);
    }
  };

  const loadBomExplosion = async () => {
    try {
      const response = await bomApi.bomExplosionApi.explodeBom(
        formData.bomId!, 
        formData.quantityPlanned!
      );
      setBomExplosion(response.data);
    } catch (error) {
      console.error('Failed to load BOM explosion:', error);
    }
  };

  const handleNext = () => {
    setActiveStep((prevStep) => prevStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevStep) => prevStep - 1);
  };

  const handleChange = (field: string, value: any) => {
    setFormData({ ...formData, [field]: value });
  };

  const handleBomChange = (bomId: string) => {
    const selectedBom = boms.find(b => b.bomId === bomId);
    if (selectedBom) {
      setFormData({
        ...formData,
        bomId: bomId,
        productId: selectedBom.productId,
        productCode: selectedBom.productCode,
        productName: selectedBom.productName,
      });
    }
  };

  const handleSubmit = async () => {
    try {
      const response = await workOrderApi.createWorkOrder({
        ...formData,
        plannedStartDate: new Date().toISOString(),
        createdBy: user!.id,
      } as WorkOrder);
      
      navigate(`/manufacturing/work-orders/${response.data.workOrderId}`);
    } catch (error) {
      console.error('Failed to create work order:', error);
    }
  };

  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>Work Order Details</Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Select BOM</InputLabel>
                  <Select
                    value={formData.bomId || ''}
                    onChange={(e) => handleBomChange(e.target.value)}
                    label="Select BOM"
                  >
                    {boms.map((bom) => (
                      <MenuItem key={bom.bomId} value={bom.bomId}>
                        {bom.bomNumber} - {bom.productCode} ({bom.productName})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Quantity to Produce"
                  type="number"
                  value={formData.quantityPlanned}
                  onChange={(e) => handleChange('quantityPlanned', parseFloat(e.target.value))}
                  inputProps={{ min: 1 }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Order Type</InputLabel>
                  <Select
                    value={formData.orderType}
                    onChange={(e) => handleChange('orderType', e.target.value)}
                    label="Order Type"
                  >
                    <MenuItem value="PRODUCTION">Production</MenuItem>
                    <MenuItem value="REWORK">Rework</MenuItem>
                    <MenuItem value="ASSEMBLY">Assembly</MenuItem>
                    <MenuItem value="DISASSEMBLY">Disassembly</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Priority</InputLabel>
                  <Select
                    value={formData.priority}
                    onChange={(e) => handleChange('priority', e.target.value)}
                    label="Priority"
                  >
                    <MenuItem value="LOW">Low</MenuItem>
                    <MenuItem value="MEDIUM">Medium</MenuItem>
                    <MenuItem value="HIGH">High</MenuItem>
                    <MenuItem value="URGENT">Urgent</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Planned Start Date"
                  type="datetime-local"
                  value={formData.plannedStartDate || ''}
                  onChange={(e) => handleChange('plannedStartDate', e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Planned End Date"
                  type="datetime-local"
                  value={formData.plannedEndDate || ''}
                  onChange={(e) => handleChange('plannedEndDate', e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Special Instructions"
                  multiline
                  rows={3}
                  value={formData.specialInstructions || ''}
                  onChange={(e) => handleChange('specialInstructions', e.target.value)}
                />
              </Grid>
            </Grid>
          </Box>
        );

      case 1:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>Review Work Order</Typography>
            
            {/* Work Order Summary */}
            <Card sx={{ mb: 3 }}>
              <CardContent>
                <Typography variant="subtitle1" gutterBottom>Work Order Summary</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">Product</Typography>
                    <Typography>{formData.productCode} - {formData.productName}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">Quantity</Typography>
                    <Typography>{formData.quantityPlanned}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">Order Type</Typography>
                    <Typography>{formData.orderType}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">Priority</Typography>
                    <Typography>{formData.priority}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>

            {/* Material Requirements */}
            {bomExplosion && (
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Material Requirements ({bomExplosion.totalComponents} components)
                </Typography>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Component</TableCell>
                        <TableCell align="right">Required Qty</TableCell>
                        <TableCell>UOM</TableCell>
                        <TableCell align="right">Unit Cost</TableCell>
                        <TableCell align="right">Total Cost</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {bomExplosion.components?.map((comp: any, index: number) => (
                        <TableRow key={index}>
                          <TableCell>{comp.componentCode} - {comp.componentName}</TableCell>
                          <TableCell align="right">{comp.requiredQuantity?.toFixed(2)}</TableCell>
                          <TableCell>{comp.uom}</TableCell>
                          <TableCell align="right">${comp.unitCost?.toFixed(2)}</TableCell>
                          <TableCell align="right">${comp.extendedCost?.toFixed(2)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            )}
          </Box>
        );

      case 2:
        return (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="h5" gutterBottom color="success.main">
              Work Order Created Successfully!
            </Typography>
            <Typography variant="body1" color="textSecondary" sx={{ mb: 3 }}>
              The work order has been created and materials/operations have been auto-generated.
            </Typography>
            <Button variant="contained" onClick={() => navigate('/manufacturing/work-orders')}>
              View All Work Orders
            </Button>
          </Box>
        );

      default:
        return null;
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>Create Work Order</Typography>
      
      <Paper sx={{ p: 3, mt: 3 }}>
        <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {renderStepContent()}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
          <Button
            disabled={activeStep === 0 || activeStep === steps.length - 1}
            onClick={handleBack}
          >
            Back
          </Button>
          <Box>
            {activeStep < steps.length - 2 && (
              <Button variant="contained" onClick={handleNext} disabled={!formData.bomId}>
                Next
              </Button>
            )}
            {activeStep === steps.length - 2 && (
              <Button variant="contained" color="success" onClick={async () => {
                await handleSubmit();
                handleNext();
              }}>
                Create Work Order
              </Button>
            )}
          </Box>
        </Box>
      </Paper>
    </Box>
  );
};

export default WorkOrderWizard;

