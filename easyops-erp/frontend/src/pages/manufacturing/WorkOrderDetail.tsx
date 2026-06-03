import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  ArrowBack,
  PlayArrow,
  Stop,
  CheckCircle,
  Cancel,
  LocalShipping,
} from '@mui/icons-material';
import { workOrderApi, workOrderOperationApi, workOrderMaterialApi, WorkOrder, WorkOrderOperation, WorkOrderMaterial } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const WorkOrderDetail: React.FC = () => {
  const { workOrderId } = useParams<{ workOrderId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [workOrder, setWorkOrder] = useState<WorkOrder | null>(null);
  const [operations, setOperations] = useState<WorkOrderOperation[]>([]);
  const [materials, setMaterials] = useState<WorkOrderMaterial[]>([]);
  const [loading, setLoading] = useState(true);
  const [issueDialog, setIssueDialog] = useState(false);
  const [selectedMaterial, setSelectedMaterial] = useState<WorkOrderMaterial | null>(null);
  const [issueQuantity, setIssueQuantity] = useState(0);

  useEffect(() => {
    if (workOrderId) {
      loadWorkOrderData();
    }
  }, [workOrderId]);

  const loadWorkOrderData = async () => {
    try {
      const [woResponse, opsResponse] = await Promise.all([
        workOrderApi.getWorkOrderById(workOrderId!),
        workOrderOperationApi.getWorkOrderOperations(workOrderId!),
      ]);
      setWorkOrder(woResponse.data);
      setOperations(opsResponse.data);
    } catch (error) {
      console.error('Failed to load work order:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleReleaseWorkOrder = async () => {
    try {
      await workOrderApi.releaseWorkOrder(workOrderId!, user!.id);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to release work order:', error);
    }
  };

  const handleStartWorkOrder = async () => {
    try {
      await workOrderApi.startWorkOrder(workOrderId!, user!.id);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to start work order:', error);
    }
  };

  const handleCompleteWorkOrder = async () => {
    try {
      await workOrderApi.completeWorkOrder(workOrderId!, user!.id);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to complete work order:', error);
    }
  };

  const handleStartOperation = async (operationId: string) => {
    try {
      await workOrderOperationApi.startOperation(operationId, user!.id);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to start operation:', error);
    }
  };

  const handleCompleteOperation = async (operationId: string, quantity: number) => {
    try {
      await workOrderOperationApi.completeOperation(operationId, quantity, user!.id);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to complete operation:', error);
    }
  };

  const handleIssueMaterial = async () => {
    if (!selectedMaterial) return;
    try {
      await workOrderMaterialApi.issueMaterial(selectedMaterial.materialId!, issueQuantity, user!.id);
      setIssueDialog(false);
      setSelectedMaterial(null);
      loadWorkOrderData();
    } catch (error) {
      console.error('Failed to issue material:', error);
    }
  };

  const getStatusColor = (status?: string) => {
    const colors: Record<string, any> = {
      'CREATED': 'default',
      'RELEASED': 'info',
      'IN_PROGRESS': 'primary',
      'COMPLETED': 'success',
      'CLOSED': 'default',
      'CANCELLED': 'error',
    };
    return colors[status || 'CREATED'] || 'default';
  };

  if (loading) return <Typography>Loading...</Typography>;
  if (!workOrder) return <Typography>Work Order not found</Typography>;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <IconButton onClick={() => navigate('/manufacturing/work-orders')} sx={{ mr: 2 }}>
            <ArrowBack />
          </IconButton>
          <Box>
            <Typography variant="h4">{workOrder.workOrderNumber}</Typography>
            <Typography variant="subtitle1" color="textSecondary">
              {workOrder.productCode} - {workOrder.productName}
            </Typography>
          </Box>
        </Box>
        <Box>
          <Chip label={workOrder.status} color={getStatusColor(workOrder.status)} sx={{ mr: 1 }} />
          <Chip label={workOrder.priority} sx={{ mr: 1 }} />
          {workOrder.status === 'CREATED' && (
            <Button variant="contained" onClick={handleReleaseWorkOrder} startIcon={<PlayArrow />} sx={{ mr: 1 }}>
              Release
            </Button>
          )}
          {workOrder.status === 'RELEASED' && (
            <Button variant="contained" onClick={handleStartWorkOrder} startIcon={<PlayArrow />} sx={{ mr: 1 }}>
              Start
            </Button>
          )}
          {workOrder.status === 'IN_PROGRESS' && (
            <Button variant="contained" color="success" onClick={handleCompleteWorkOrder} startIcon={<CheckCircle />}>
              Complete
            </Button>
          )}
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Work Order Info */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Work Order Info</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Quantity Planned</Typography>
                <Typography variant="h6">{workOrder.quantityPlanned}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Quantity Completed</Typography>
                <Typography variant="h6">{workOrder.quantityCompleted || 0}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Progress</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <LinearProgress 
                    variant="determinate" 
                    value={workOrder.completionPercentage || 0} 
                    sx={{ flex: 1, mr: 1 }} 
                  />
                  <Typography variant="body2">{workOrder.completionPercentage?.toFixed(0) || 0}%</Typography>
                </Box>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Operations</Typography>
                <Typography>{workOrder.operationsCompleted || 0} / {workOrder.totalOperations || 0}</Typography>
              </Box>
            </CardContent>
          </Card>

          {/* Cost Card */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Cost Summary</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Material Cost</Typography>
                <Typography>${workOrder.materialCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Labor Cost</Typography>
                <Typography>${workOrder.laborCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Overhead Cost</Typography>
                <Typography>${workOrder.overheadCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                <Typography variant="body2" color="textSecondary">Total Cost</Typography>
                <Typography variant="h6" color="primary">${workOrder.totalCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Operations & Materials */}
        <Grid item xs={12} md={8}>
          {/* Operations Table */}
          <Paper sx={{ p: 2, mb: 2 }}>
            <Typography variant="h6" gutterBottom>Operations</Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Seq</TableCell>
                    <TableCell>Operation</TableCell>
                    <TableCell>Work Center</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Planned Time</TableCell>
                    <TableCell>Actual Time</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {operations.map((op) => (
                    <TableRow key={op.operationId}>
                      <TableCell>{op.operationSequence}</TableCell>
                      <TableCell>{op.operationName}</TableCell>
                      <TableCell>{op.workCenterCode}</TableCell>
                      <TableCell>
                        <Chip label={op.status} size="small" 
                          color={op.status === 'COMPLETED' ? 'success' : 
                                 op.status === 'IN_PROGRESS' ? 'primary' : 'default'} 
                        />
                      </TableCell>
                      <TableCell>{op.totalTimePlanned?.toFixed(1) || 0} min</TableCell>
                      <TableCell>{op.totalTimeActual?.toFixed(1) || 0} min</TableCell>
                      <TableCell>
                        {op.status === 'PENDING' && (
                          <IconButton size="small" onClick={() => handleStartOperation(op.operationId!)}>
                            <PlayArrow />
                          </IconButton>
                        )}
                        {op.status === 'IN_PROGRESS' && (
                          <IconButton 
                            size="small" 
                            color="success"
                            onClick={() => handleCompleteOperation(op.operationId!, workOrder.quantityPlanned!)}
                          >
                            <CheckCircle />
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>
      </Grid>

      {/* Material Issue Dialog */}
      <Dialog open={issueDialog} onClose={() => setIssueDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Issue Material</DialogTitle>
        <DialogContent>
          {selectedMaterial && (
            <Box>
              <Typography variant="body2" gutterBottom>
                {selectedMaterial.componentCode} - {selectedMaterial.componentName}
              </Typography>
              <TextField
                fullWidth
                label="Quantity to Issue"
                type="number"
                value={issueQuantity}
                onChange={(e) => setIssueQuantity(parseFloat(e.target.value))}
                margin="normal"
                inputProps={{ 
                  min: 0, 
                  max: selectedMaterial.quantityRequired,
                  step: 0.01 
                }}
                helperText={`Required: ${selectedMaterial.quantityRequired} ${selectedMaterial.uom}`}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIssueDialog(false)}>Cancel</Button>
          <Button onClick={handleIssueMaterial} variant="contained">Issue</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default WorkOrderDetail;

