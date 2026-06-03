import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  LocationOn as LocationIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';
import { Location, LocationFormData } from '@/types/organization';

interface LocationTabProps {
  organizationId: string;
}

const LocationTab: React.FC<LocationTabProps> = ({ organizationId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [locations, setLocations] = useState<Location[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingLocation, setEditingLocation] = useState<Location | null>(null);
  const [formData, setFormData] = useState<LocationFormData>({
    code: '',
    name: '',
    type: 'BRANCH',
  });

  useEffect(() => {
    fetchLocations();
  }, [organizationId]);

  const fetchLocations = async () => {
    try {
      const data = await organizationService.getLocations(organizationId);
      setLocations(data);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch locations', {
        variant: 'error',
      });
    }
  };

  const handleOpenDialog = (location?: Location) => {
    if (location) {
      setEditingLocation(location);
      setFormData({
        code: location.code,
        name: location.name,
        type: location.type || 'BRANCH',
        addressLine1: location.addressLine1,
        city: location.city,
        state: location.state,
        postalCode: location.postalCode,
        country: location.country,
        phone: location.phone,
        email: location.email,
        timezone: location.timezone,
      });
    } else {
      setEditingLocation(null);
      setFormData({
        code: '',
        name: '',
        type: 'BRANCH',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingLocation(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingLocation) {
        await organizationService.updateLocation(editingLocation.id, formData);
        enqueueSnackbar('Location updated successfully', { variant: 'success' });
      } else {
        await organizationService.createLocation(organizationId, formData);
        enqueueSnackbar('Location created successfully', { variant: 'success' });
      }
      handleCloseDialog();
      fetchLocations();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Operation failed', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this location?')) {
      try {
        await organizationService.deleteLocation(id);
        enqueueSnackbar('Location deleted successfully', { variant: 'success' });
        fetchLocations();
      } catch (error: any) {
        enqueueSnackbar(error.response?.data?.message || 'Delete failed', { variant: 'error' });
      }
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6">Locations</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
          Add Location
        </Button>
      </Box>

      {locations.length > 0 ? (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Address</TableCell>
                <TableCell>Contact</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {locations.map((location) => (
                <TableRow key={location.id} hover>
                  <TableCell>{location.code}</TableCell>
                  <TableCell>{location.name}</TableCell>
                  <TableCell>
                    <Chip label={location.type} size="small" />
                  </TableCell>
                  <TableCell>
                    {location.city && location.state
                      ? `${location.city}, ${location.state}`
                      : location.city || location.state || '-'}
                  </TableCell>
                  <TableCell>{location.phone || location.email || '-'}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => handleOpenDialog(location)}>
                      <EditIcon />
                    </IconButton>
                    <IconButton size="small" onClick={() => handleDelete(location.id)} color="error">
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      ) : (
        <Card>
          <CardContent>
            <Typography variant="body2" color="textSecondary" align="center">
              No locations found. Create one to get started!
            </Typography>
          </CardContent>
        </Card>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editingLocation ? 'Edit Location' : 'Create Location'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Code *"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                disabled={!!editingLocation}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Name *"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Type"
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              >
                <MenuItem value="HEADQUARTERS">Headquarters</MenuItem>
                <MenuItem value="BRANCH">Branch</MenuItem>
                <MenuItem value="WAREHOUSE">Warehouse</MenuItem>
                <MenuItem value="OFFICE">Office</MenuItem>
                <MenuItem value="RETAIL">Retail</MenuItem>
                <MenuItem value="FACTORY">Factory</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Phone"
                value={formData.phone || ''}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Address Line 1"
                value={formData.addressLine1 || ''}
                onChange={(e) => setFormData({ ...formData, addressLine1: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="City"
                value={formData.city || ''}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="State"
                value={formData.state || ''}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Postal Code"
                value={formData.postalCode || ''}
                onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Country"
                value={formData.country || ''}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={formData.email || ''}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={!formData.code || !formData.name}>
            {editingLocation ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LocationTab;

