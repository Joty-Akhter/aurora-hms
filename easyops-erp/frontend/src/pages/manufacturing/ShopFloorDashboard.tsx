import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip } from '@mui/material';
import { analyticsApi } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const ShopFloorDashboard: React.FC = () => {
  const { user } = useAuth();
  const [shopFloorData, setShopFloorData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadShopFloorData();
      const interval = setInterval(loadShopFloorData, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [user?.organizationId]);

  const loadShopFloorData = async () => {
    try {
      const response = await analyticsApi.getShopFloorDashboard(user!.organizationId);
      setShopFloorData(response.data);
    } catch (error) {
      console.error('Failed to load shop floor data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getAlertColor = (alertStatus?: string) => {
    const colors: Record<string, 'error' | 'warning' | 'success'> = {
      'OVERDUE': 'error',
      'DUE_SOON': 'warning',
      'ON_TRACK': 'success',
    };
    return colors[alertStatus || 'ON_TRACK'] || 'success';
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Shop Floor Dashboard (Real-Time)
      </Typography>
      <Typography variant="subtitle1" color="textSecondary" gutterBottom>
        Last updated: {new Date().toLocaleTimeString()}
      </Typography>

      <TableContainer component={Paper} sx={{ mt: 2 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>WO #</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Progress</TableCell>
              <TableCell>Current Operation</TableCell>
              <TableCell>Work Center</TableCell>
              <TableCell>Materials Pending</TableCell>
              <TableCell>Days Until Due</TableCell>
              <TableCell>Alert</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {shopFloorData.map((wo: any) => (
              <TableRow key={wo.work_order_id}>
                <TableCell>{wo.work_order_number}</TableCell>
                <TableCell>{wo.product_code} - {wo.product_name}</TableCell>
                <TableCell>
                  <Chip label={wo.priority} size="small" color={wo.priority === 'URGENT' ? 'error' : 'default'} />
                </TableCell>
                <TableCell>{wo.completion_percentage?.toFixed(0) || 0}%</TableCell>
                <TableCell>{wo.current_operation || '-'}</TableCell>
                <TableCell>{wo.current_work_center || '-'}</TableCell>
                <TableCell>{wo.materials_pending || 0}</TableCell>
                <TableCell>{wo.days_until_due?.toFixed(0) || '-'}</TableCell>
                <TableCell>
                  <Chip label={wo.alert_status} color={getAlertColor(wo.alert_status)} size="small" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {!loading && shopFloorData.length === 0 && (
        <Box sx={{ textAlign: 'center', mt: 4 }}>
          <Typography color="textSecondary">No active work orders on the shop floor.</Typography>
        </Box>
      )}
    </Box>
  );
};

export default ShopFloorDashboard;

