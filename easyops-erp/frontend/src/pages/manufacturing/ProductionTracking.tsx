import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  LinearProgress,
  Grid,
  Card,
  CardContent,
  TextField,
  MenuItem,
  IconButton,
} from '@mui/material';
import {
  Refresh,
  CheckCircle,
  Warning,
  Schedule,
} from '@mui/icons-material';
import { workOrderApi } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

interface WorkOrderTracking {
  workOrderId: string;
  workOrderNumber: string;
  productCode: string;
  productName: string;
  quantityPlanned: number;
  quantityCompleted: number;
  quantityInProcess: number;
  status: string;
  priority: string;
  plannedStartDate: string;
  actualStartDate?: string;
  currentOperation?: string;
  completionPercentage: number;
}

const ProductionTracking: React.FC = () => {
  const { user } = useAuth();
  const [workOrders, setWorkOrders] = useState<WorkOrderTracking[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('IN_PROGRESS');

  useEffect(() => {
    if (user?.organizationId) {
      loadProductionData();
    }
  }, [user?.organizationId, filter]);

  const loadProductionData = async () => {
    setLoading(true);
    try {
      // Get work orders by status
      const response = await workOrderApi.getWorkOrdersByStatus(user!.organizationId, filter);
      
      // Transform to tracking format
      const trackingData = response.data.map((wo: any) => ({
        workOrderId: wo.workOrderId,
        workOrderNumber: wo.workOrderNumber,
        productCode: wo.productCode,
        productName: wo.productName,
        quantityPlanned: wo.quantityPlanned || 0,
        quantityCompleted: wo.quantityCompleted || 0,
        quantityInProcess: wo.quantityInProcess || 0,
        status: wo.status,
        priority: wo.priority || 'MEDIUM',
        plannedStartDate: wo.plannedStartDate,
        actualStartDate: wo.actualStartDate,
        currentOperation: wo.currentOperation,
        completionPercentage: wo.quantityPlanned > 0 
          ? Math.round((wo.quantityCompleted / wo.quantityPlanned) * 100)
          : 0,
      }));
      
      setWorkOrders(trackingData);
    } catch (error) {
      console.error('Failed to load production tracking data:', error);
      setWorkOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'IN_PROGRESS':
        return 'primary';
      case 'RELEASED':
        return 'info';
      case 'ON_HOLD':
        return 'warning';
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'success';
      default:
        return 'default';
    }
  };

  const getProgressColor = (percentage: number) => {
    if (percentage >= 90) return 'success';
    if (percentage >= 50) return 'primary';
    if (percentage >= 25) return 'warning';
    return 'error';
  };

  // Calculate summary stats
  const totalPlanned = workOrders.reduce((sum, wo) => sum + wo.quantityPlanned, 0);
  const totalCompleted = workOrders.reduce((sum, wo) => sum + wo.quantityCompleted, 0);
  const totalInProcess = workOrders.reduce((sum, wo) => sum + wo.quantityInProcess, 0);
  const overallProgress = totalPlanned > 0 ? Math.round((totalCompleted / totalPlanned) * 100) : 0;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Production Tracking</Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            select
            size="small"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="RELEASED">Released</MenuItem>
            <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="ON_HOLD">On Hold</MenuItem>
          </TextField>
          <IconButton onClick={loadProductionData} title="Refresh">
            <Refresh />
          </IconButton>
        </Box>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Schedule color="primary" />
                <Box>
                  <Typography variant="caption" color="textSecondary">
                    Planned Units
                  </Typography>
                  <Typography variant="h5">{totalPlanned.toLocaleString()}</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Warning color="warning" />
                <Box>
                  <Typography variant="caption" color="textSecondary">
                    In Process
                  </Typography>
                  <Typography variant="h5">{totalInProcess.toLocaleString()}</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CheckCircle color="success" />
                <Box>
                  <Typography variant="caption" color="textSecondary">
                    Completed
                  </Typography>
                  <Typography variant="h5">{totalCompleted.toLocaleString()}</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="caption" color="textSecondary">
                Overall Progress
              </Typography>
              <Typography variant="h5">{overallProgress}%</Typography>
              <LinearProgress
                variant="determinate"
                value={overallProgress}
                color={getProgressColor(overallProgress)}
                sx={{ mt: 1 }}
              />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Work Orders Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Work Order</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>Current Operation</TableCell>
              <TableCell align="center">Priority</TableCell>
              <TableCell align="right">Planned</TableCell>
              <TableCell align="right">In Process</TableCell>
              <TableCell align="right">Completed</TableCell>
              <TableCell align="center">Progress</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Typography>Loading production data...</Typography>
                </TableCell>
              </TableRow>
            ) : workOrders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Box sx={{ py: 3 }}>
                    <Typography color="textSecondary">
                      No work orders found for status: {filter}
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              workOrders.map((wo) => (
                <TableRow key={wo.workOrderId} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="bold">
                      {wo.workOrderNumber}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {wo.actualStartDate ? new Date(wo.actualStartDate).toLocaleDateString() : 'Not started'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{wo.productCode}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {wo.productName}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {wo.currentOperation || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={wo.priority}
                      color={getPriorityColor(wo.priority)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">{wo.quantityPlanned}</TableCell>
                  <TableCell align="right">{wo.quantityInProcess}</TableCell>
                  <TableCell align="right">{wo.quantityCompleted}</TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <LinearProgress
                        variant="determinate"
                        value={wo.completionPercentage}
                        color={getProgressColor(wo.completionPercentage)}
                        sx={{ flexGrow: 1, minWidth: 100 }}
                      />
                      <Typography variant="caption">
                        {wo.completionPercentage}%
                      </Typography>
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={wo.status.replace('_', ' ')}
                      color={getStatusColor(wo.status)}
                      size="small"
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};

export default ProductionTracking;

