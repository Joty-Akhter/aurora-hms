import React, { useEffect, useState } from 'react';
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Add as AddIcon } from '@mui/icons-material';
import { workOrderApi, WorkOrder } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';
import './Manufacturing.css';

const WorkOrderList: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadWorkOrders();
    }
  }, [user?.organizationId]);

  const loadWorkOrders = async () => {
    try {
      const response = await workOrderApi.getAllWorkOrders(user!.organizationId);
      setWorkOrders(response.data);
    } catch (error) {
      console.error('Failed to load work orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status?: string) => {
    const statusColors: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'> = {
      'CREATED': 'default',
      'RELEASED': 'info',
      'IN_PROGRESS': 'primary',
      'PAUSED': 'warning',
      'COMPLETED': 'success',
      'CLOSED': 'default',
      'CANCELLED': 'error',
    };
    return statusColors[status || 'CREATED'] || 'default';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Work Orders</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/manufacturing/work-orders/new')}>
          Create Work Order
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>WO Number</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>Qty Planned</TableCell>
              <TableCell>Qty Completed</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Progress</TableCell>
              <TableCell>Planned End</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {workOrders.map((wo) => (
              <TableRow 
                key={wo.workOrderId} 
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/manufacturing/work-orders/${wo.workOrderId}`)}
              >
                <TableCell>{wo.workOrderNumber}</TableCell>
                <TableCell>{wo.productCode} - {wo.productName}</TableCell>
                <TableCell>{wo.quantityPlanned}</TableCell>
                <TableCell>{wo.quantityCompleted}</TableCell>
                <TableCell>
                  <Chip label={wo.status} color={getStatusColor(wo.status)} size="small" />
                </TableCell>
                <TableCell>
                  <Chip label={wo.priority} size="small" />
                </TableCell>
                <TableCell>{wo.completionPercentage?.toFixed(0) || 0}%</TableCell>
                <TableCell>{wo.plannedEndDate ? new Date(wo.plannedEndDate).toLocaleDateString() : '-'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {!loading && workOrders.length === 0 && (
        <Box sx={{ textAlign: 'center', mt: 4 }}>
          <Typography color="textSecondary">No work orders found. Create your first work order to start production.</Typography>
        </Box>
      )}
    </Box>
  );
};

export default WorkOrderList;

