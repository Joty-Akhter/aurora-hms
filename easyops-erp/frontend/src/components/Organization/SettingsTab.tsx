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
  FormControlLabel,
  Grid,
  IconButton,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Lock as LockIcon,
  LockOpen as LockOpenIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';

interface SettingsTabProps {
  organizationId: string;
}

const SettingsTab: React.FC<SettingsTabProps> = ({ organizationId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [settings, setSettings] = useState<Record<string, string>>({});
  const [openDialog, setOpenDialog] = useState(false);
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    key: '',
    value: '',
    type: 'string',
    encrypted: false,
  });

  useEffect(() => {
    fetchSettings();
  }, [organizationId]);

  const fetchSettings = async () => {
    try {
      const data = await organizationService.getSettings(organizationId);
      setSettings(data);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch settings', {
        variant: 'error',
      });
    }
  };

  const handleOpenDialog = (key?: string) => {
    if (key) {
      setEditingKey(key);
      setFormData({
        key,
        value: settings[key] || '',
        type: 'string',
        encrypted: false,
      });
    } else {
      setEditingKey(null);
      setFormData({
        key: '',
        value: '',
        type: 'string',
        encrypted: false,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingKey(null);
  };

  const handleSubmit = async () => {
    try {
      await organizationService.setSetting(organizationId, formData);
      enqueueSnackbar(
        `Setting ${editingKey ? 'updated' : 'created'} successfully`,
        { variant: 'success' }
      );
      handleCloseDialog();
      fetchSettings();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Operation failed', { variant: 'error' });
    }
  };

  const handleDelete = async (key: string) => {
    if (window.confirm(`Are you sure you want to delete setting "${key}"?`)) {
      try {
        await organizationService.deleteSetting(organizationId, key);
        enqueueSnackbar('Setting deleted successfully', { variant: 'success' });
        fetchSettings();
      } catch (error: any) {
        enqueueSnackbar(error.response?.data?.message || 'Delete failed', { variant: 'error' });
      }
    }
  };

  const settingsArray = Object.entries(settings);

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6">Organization Settings</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
          Add Setting
        </Button>
      </Box>

      {settingsArray.length > 0 ? (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {settingsArray.map(([key, value]) => (
                <TableRow key={key} hover>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {key}
                      {key.toLowerCase().includes('secret') ||
                      key.toLowerCase().includes('password') ||
                      key.toLowerCase().includes('key') ? (
                        <Chip icon={<LockIcon />} label="Encrypted" size="small" color="warning" />
                      ) : null}
                    </Box>
                  </TableCell>
                  <TableCell>
                    {key.toLowerCase().includes('secret') ||
                    key.toLowerCase().includes('password') ||
                    key.toLowerCase().includes('key')
                      ? '••••••••'
                      : value}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => handleOpenDialog(key)}>
                      <EditIcon />
                    </IconButton>
                    <IconButton size="small" onClick={() => handleDelete(key)} color="error">
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
              No settings found. Create one to get started!
            </Typography>
          </CardContent>
        </Card>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingKey ? 'Edit Setting' : 'Create Setting'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Key *"
                value={formData.key}
                onChange={(e) => setFormData({ ...formData, key: e.target.value })}
                disabled={!!editingKey}
                helperText="Use dot notation for nested keys (e.g., api.endpoint.url)"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Value *"
                value={formData.value}
                onChange={(e) => setFormData({ ...formData, value: e.target.value })}
                multiline
                rows={3}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.encrypted}
                    onChange={(e) => setFormData({ ...formData, encrypted: e.target.checked })}
                  />
                }
                label="Encrypt this setting (for sensitive data like API keys)"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={!formData.key || !formData.value}
          >
            {editingKey ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SettingsTab;

