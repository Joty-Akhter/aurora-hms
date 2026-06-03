import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  Add,
  Edit,
  Visibility,
  Search,
} from '@mui/icons-material';
import { routingApi, ProductRouting } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const ProductRoutingList: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [routings, setRoutings] = useState<ProductRouting[]>([]);
  const [filteredRoutings, setFilteredRoutings] = useState<ProductRouting[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    if (user?.organizationId) {
      loadRoutings();
    } else {
      // If no user, stop loading after a short delay
      setTimeout(() => setLoading(false), 1000);
    }
  }, [user?.organizationId]);

  useEffect(() => {
    filterRoutings();
  }, [searchTerm, routings]);

  const loadRoutings = async () => {
    try {
      const response = await routingApi.getAllRoutings(user!.organizationId);
      setRoutings(response.data);
      setFilteredRoutings(response.data);
    } catch (error) {
      console.error('Failed to load routings:', error);
      // Set empty array on error so page still displays
      setRoutings([]);
      setFilteredRoutings([]);
    } finally {
      setLoading(false);
    }
  };

  const filterRoutings = () => {
    if (!searchTerm) {
      setFilteredRoutings(routings);
      return;
    }

    const filtered = routings.filter(
      (routing) =>
        routing.productCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        routing.operationCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        routing.workCenterCode?.toLowerCase().includes(searchTerm.toLowerCase())
    );
    setFilteredRoutings(filtered);
  };

  const getStatusColor = (isActive: boolean) => {
    return isActive ? 'success' : 'default';
  };

  if (loading) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography>Loading Product Routings...</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Product Routings</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/manufacturing/routings/new')}
        >
          Create New Routing
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search by product code, operation, or work center..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
        </CardContent>
      </Card>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Product Code</TableCell>
              <TableCell>Operation</TableCell>
              <TableCell>Sequence</TableCell>
              <TableCell>Work Center</TableCell>
              <TableCell align="right">Setup Time (min)</TableCell>
              <TableCell align="right">Run Time/Unit (min)</TableCell>
              <TableCell align="right">Setup Cost</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredRoutings.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Box sx={{ py: 3 }}>
                    <Typography color="textSecondary">
                      {searchTerm ? 'No routings found matching your search.' : 'No product routings yet. Create your first routing!'}
                    </Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              filteredRoutings.map((routing) => (
                <TableRow key={routing.routingId} hover>
                  <TableCell>{routing.productCode}</TableCell>
                  <TableCell>
                    <Typography variant="body2">{routing.operationCode}</Typography>
                    <Typography variant="caption" color="textSecondary">
                      {routing.operationName}
                    </Typography>
                  </TableCell>
                  <TableCell>{routing.operationSequence}</TableCell>
                  <TableCell>{routing.workCenterCode}</TableCell>
                  <TableCell align="right">{routing.setupTime || 0}</TableCell>
                  <TableCell align="right">{routing.runTimePerUnit || 0}</TableCell>
                  <TableCell align="right">${routing.setupCost?.toFixed(2) || '0.00'}</TableCell>
                  <TableCell>
                    <Chip
                      label={routing.isActive ? 'Active' : 'Inactive'}
                      color={getStatusColor(routing.isActive || false)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/manufacturing/routings/${routing.routingId}`)}
                      title="View Details"
                    >
                      <Visibility />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/manufacturing/routings/${routing.routingId}/edit`)}
                      title="Edit"
                    >
                      <Edit />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {!searchTerm && filteredRoutings.length > 0 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="body2" color="textSecondary">
            Total: {filteredRoutings.length} routing(s)
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default ProductRoutingList;

