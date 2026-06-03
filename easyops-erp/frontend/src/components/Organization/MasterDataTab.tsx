import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Paper,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import appDataService, { OrganizationAppData } from '@/services/appDataService';

interface MasterDataTabProps {
  organizationId: string;
}

const SUPPORTED_TYPES = [
  { value: 'UOM', label: 'Units of Measure' },
  { value: 'GENDER', label: 'Gender' },
];

const MasterDataTab: React.FC<MasterDataTabProps> = ({ organizationId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [type, setType] = useState<string>('UOM');
  const [items, setItems] = useState<OrganizationAppData[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingItem, setEditingItem] = useState<OrganizationAppData | null>(null);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    displayOrder: 0,
  });

  useEffect(() => {
    if (organizationId) {
      void loadItems();
    }
  }, [organizationId, type]);

  const loadItems = async () => {
    try {
      setLoading(true);
      const data = await appDataService.getAppData(organizationId, type);
      setItems(data);
    } catch (error: any) {
      console.error('Failed to load master data:', error);
      enqueueSnackbar(
        error.response?.data?.message || 'Failed to load master data',
        { variant: 'error' }
      );
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (item?: OrganizationAppData) => {
    if (item) {
      setEditingItem(item);
      setFormData({
        code: item.code,
        name: item.name,
        description: item.description || '',
        displayOrder: item.displayOrder ?? 0,
      });
    } else {
      setEditingItem(null);
      setFormData({
        code: '',
        name: '',
        description: '',
        displayOrder: items.length,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingItem(null);
  };

  // NOTE: Backend currently exposes read-only API.
  // This UI is primarily for viewing. Save/delete would require corresponding POST/PUT/DELETE endpoints.

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6">Master Data</Typography>
          <Select
            size="small"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            {SUPPORTED_TYPES.map((t) => (
              <MenuItem key={t.value} value={t.value}>
                {t.label}
              </MenuItem>
            ))}
          </Select>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
          disabled
        >
          Add Entry (coming soon)
        </Button>
      </Box>

      {loading ? (
        <Typography>Loading master data...</Typography>
      ) : items.length === 0 ? (
        <Card>
          <CardContent>
            <Typography variant="body2" color="textSecondary" align="center">
              No master data entries found for this type.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Order</TableCell>
                <TableCell>Active</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>{item.code}</TableCell>
                  <TableCell>{item.name}</TableCell>
                  <TableCell>{item.description || '-'}</TableCell>
                  <TableCell>{item.displayOrder ?? ''}</TableCell>
                  <TableCell>{item.isActive ? 'Yes' : 'No'}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      onClick={() => handleOpenDialog(item)}
                      disabled
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingItem ? 'View Master Data Entry' : 'Add Master Data Entry'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Code"
                value={formData.code}
                disabled
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Name"
                value={formData.name}
                disabled
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                value={formData.description}
                disabled
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MasterDataTab;

