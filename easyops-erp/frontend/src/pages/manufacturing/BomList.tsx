import React, { useEffect, useState } from 'react';
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Add as AddIcon } from '@mui/icons-material';
import { bomApi, BomHeader } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';
import './Manufacturing.css';

const BomList: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [boms, setBoms] = useState<BomHeader[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.organizationId) {
      loadBoms();
    }
  }, [user?.organizationId]);

  const loadBoms = async () => {
    try {
      const response = await bomApi.getAllBoms(user!.organizationId);
      setBoms(response.data);
    } catch (error) {
      console.error('Failed to load BOMs:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Bill of Materials</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/manufacturing/boms/new')}>
          Create BOM
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>BOM Number</TableCell>
              <TableCell>Product Code</TableCell>
              <TableCell>Product Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Total Cost</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {boms.map((bom) => (
              <TableRow 
                key={bom.bomId} 
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/manufacturing/boms/${bom.bomId}`)}
              >
                <TableCell>{bom.bomNumber}</TableCell>
                <TableCell>{bom.productCode}</TableCell>
                <TableCell>{bom.productName}</TableCell>
                <TableCell>{bom.bomType}</TableCell>
                <TableCell>{bom.versionNumber}</TableCell>
                <TableCell>{bom.status}</TableCell>
                <TableCell>${bom.totalCost?.toFixed(2) || '0.00'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {!loading && boms.length === 0 && (
        <Box sx={{ textAlign: 'center', mt: 4 }}>
          <Typography color="textSecondary">No BOMs found. Create your first BOM to get started.</Typography>
        </Box>
      )}
    </Box>
  );
};

export default BomList;

