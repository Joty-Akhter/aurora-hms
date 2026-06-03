import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CircularProgress,
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
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Business as BusinessIcon,
  CheckCircle as ActiveIcon,
  Cancel as InactiveIcon,
  CloudUpload as CloudUploadIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import { useAuth } from '@contexts/AuthContext';
import organizationService from '@/services/organizationService';
import { Organization, OrganizationFormData } from '@/types/organization';
import { resolveOrganizationLogoUrl } from '@/utils/organizationLogo';

const Organizations: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const { currentOrganizationId, setCurrentOrganization, hasRole, canManageResource } = useAuth();
  const canAddOrg = canManageResource('organizations') ||
    hasRole('SYSTEM_ADMIN') || hasRole('SYSTEM_ADMINISTRATOR') ||
    hasRole('SUPER_ADMIN') || hasRole('ORG_ADMIN');
  const logoFileInputRef = useRef<HTMLInputElement>(null);
  const [logoUploading, setLogoUploading] = useState(false);
  /** Bumps after file upload so <img> refetches the same logo URL (browser cache). */
  const [logoPreviewVersion, setLogoPreviewVersion] = useState(0);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingOrg, setEditingOrg] = useState<Organization | null>(null);
  const [formData, setFormData] = useState<OrganizationFormData>({
    code: '',
    name: '',
    email: '',
    currency: 'BDT',
    timezone: 'Asia/Dhaka',
    locale: 'bn-BD',
    subscriptionPlan: 'FREE',
  });

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const data = await organizationService.getAllOrganizations(0, 100);
      setOrganizations(data.content || []);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch organizations', {
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (org?: Organization) => {
    if (org) {
      setEditingOrg(org);
      setFormData({
        code: org.code,
        name: org.name,
        legalName: org.legalName,
        description: org.description,
        logo: org.logo,
        email: org.email,
        phone: org.phone,
        website: org.website,
        industry: org.industry,
        businessType: org.businessType,
        taxId: org.taxId,
        currency: org.currency || 'USD',
        timezone: org.timezone || 'UTC',
        locale: org.locale || 'en-US',
        addressLine1: org.addressLine1,
        addressLine2: org.addressLine2,
        city: org.city,
        state: org.state,
        postalCode: org.postalCode,
        country: org.country,
        subscriptionPlan: org.subscriptionPlan || 'FREE',
      });
    } else {
      setEditingOrg(null);
      setFormData({
        code: '',
        name: '',
        email: '',
        currency: 'USD',
        timezone: 'UTC',
        locale: 'en-US',
        subscriptionPlan: 'FREE',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingOrg(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingOrg) {
        await organizationService.updateOrganization(editingOrg.id, formData);
        enqueueSnackbar('Organization updated successfully', { variant: 'success' });
      } else {
        await organizationService.createOrganization(formData);
        enqueueSnackbar('Organization created successfully', { variant: 'success' });
      }
      handleCloseDialog();
      fetchOrganizations();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Operation failed', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this organization?')) {
      try {
        await organizationService.deleteOrganization(id);
        enqueueSnackbar('Organization deleted successfully', { variant: 'success' });
        fetchOrganizations();
      } catch (error: any) {
        enqueueSnackbar(error.response?.data?.message || 'Delete failed', { variant: 'error' });
      }
    }
  };

  const handleToggleStatus = async (org: Organization) => {
    try {
      if (org.status === 'ACTIVE') {
        await organizationService.suspendOrganization(org.id);
        enqueueSnackbar('Organization suspended', { variant: 'success' });
      } else {
        await organizationService.activateOrganization(org.id);
        enqueueSnackbar('Organization activated', { variant: 'success' });
      }
      fetchOrganizations();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Status update failed', { variant: 'error' });
    }
  };

  const handleLogoFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !editingOrg) return;
    try {
      setLogoUploading(true);
      const updated = await organizationService.uploadOrganizationLogo(editingOrg.id, file);
      setFormData((prev) => ({ ...prev, logo: updated.logo }));
      setEditingOrg((prev) => (prev ? { ...prev, ...updated } : null));
      setLogoPreviewVersion((v) => v + 1);
      enqueueSnackbar('Logo uploaded successfully', { variant: 'success' });
      if (currentOrganizationId === editingOrg.id) {
        const path = updated.logo?.trim();
        let logoForAuth: string | null = updated.logo ?? null;
        if (path && !path.startsWith('data:') && !/^https?:\/\//i.test(path)) {
          logoForAuth = `${path}${path.includes('?') ? '&' : '?'}t=${Date.now()}`;
        }
        setCurrentOrganization(editingOrg.id, updated.name, logoForAuth);
      }
      fetchOrganizations();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || error.message || 'Logo upload failed', {
        variant: 'error',
      });
    } finally {
      setLogoUploading(false);
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'SUSPENDED':
        return 'error';
      case 'TRIAL':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          <BusinessIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
          Organizations
        </Typography>
        {canAddOrg && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Add Organization
          </Button>
        )}
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Code</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Plan</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Max Users</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {organizations.map((org) => (
              <TableRow key={org.id} hover onClick={() => navigate(`/organizations/${org.id}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>{org.code}</TableCell>
                <TableCell>{org.name}</TableCell>
                <TableCell>{org.email || '-'}</TableCell>
                <TableCell>
                  <Chip label={org.subscriptionPlan} size="small" color="primary" variant="outlined" />
                </TableCell>
                <TableCell>
                  <Chip
                    label={org.status}
                    size="small"
                    color={getStatusColor(org.status)}
                    icon={org.status === 'ACTIVE' ? <ActiveIcon /> : <InactiveIcon />}
                  />
                </TableCell>
                <TableCell>{org.maxUsers || 10}</TableCell>
                <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOpenDialog(org);
                    }}
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggleStatus(org);
                    }}
                    color={org.status === 'ACTIVE' ? 'error' : 'success'}
                  >
                    {org.status === 'ACTIVE' ? <InactiveIcon /> : <ActiveIcon />}
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(org.id);
                    }}
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {organizations.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="textSecondary">
                    No organizations found. Create one to get started!
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editingOrg ? 'Edit Organization' : 'Create Organization'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Code *"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                disabled={!!editingOrg}
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
                label="Legal Name"
                value={formData.legalName || ''}
                onChange={(e) => setFormData({ ...formData, legalName: e.target.value })}
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
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Phone"
                value={formData.phone || ''}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Website"
                value={formData.website || ''}
                onChange={(e) => setFormData({ ...formData, website: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Currency"
                value={formData.currency}
                onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
              >
                <MenuItem value="BDT">BDT - Bangladeshi Taka</MenuItem>
                <MenuItem value="USD">USD - US Dollar</MenuItem>
                <MenuItem value="EUR">EUR - Euro</MenuItem>
                <MenuItem value="GBP">GBP - British Pound</MenuItem>
                <MenuItem value="INR">INR - Indian Rupee</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Subscription Plan"
                value={formData.subscriptionPlan}
                onChange={(e) => setFormData({ ...formData, subscriptionPlan: e.target.value })}
              >
                <MenuItem value="FREE">Free</MenuItem>
                <MenuItem value="BASIC">Basic</MenuItem>
                <MenuItem value="PROFESSIONAL">Professional</MenuItem>
                <MenuItem value="ENTERPRISE">Enterprise</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Description"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Logo URL (optional)"
                helperText="Or upload a file below. HTTPS URL to an image, or leave blank if you only upload."
                value={formData.logo || ''}
                onChange={(e) => setFormData({ ...formData, logo: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <input
                ref={logoFileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif,image/svg+xml"
                hidden
                onChange={handleLogoFileSelected}
              />
              <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 2 }}>
                <Button
                  variant="outlined"
                  startIcon={logoUploading ? <CircularProgress size={18} /> : <CloudUploadIcon />}
                  disabled={!editingOrg || logoUploading}
                  onClick={() => logoFileInputRef.current?.click()}
                >
                  Upload logo image
                </Button>
                {!editingOrg && (
                  <Typography variant="body2" color="text.secondary">
                    Save the organization first, then open Edit to upload a logo file (max 2 MB).
                  </Typography>
                )}
              </Box>
              {formData.logo ? (
                <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Preview
                  </Typography>
                  <Box
                    component="img"
                    src={(() => {
                      const raw = (formData.logo ?? '').trim();
                      const base = resolveOrganizationLogoUrl(formData.logo);
                      if (!base) return undefined;
                      if (raw.startsWith('data:') || /^https?:\/\//i.test(raw)) {
                        return base;
                      }
                      const sep = base.includes('?') ? '&' : '?';
                      return `${base}${sep}v=${logoPreviewVersion}`;
                    })()}
                    alt=""
                    sx={{ maxHeight: 56, maxWidth: 220, objectFit: 'contain', border: 1, borderColor: 'divider', p: 0.5, borderRadius: 1 }}
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = 'none';
                    }}
                  />
                </Box>
              ) : null}
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Address line 1"
                value={formData.addressLine1 || ''}
                onChange={(e) => setFormData({ ...formData, addressLine1: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Address line 2"
                value={formData.addressLine2 || ''}
                onChange={(e) => setFormData({ ...formData, addressLine2: e.target.value })}
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
                label="State / region"
                value={formData.state || ''}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Postal code"
                value={formData.postalCode || ''}
                onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Country (ISO 2-letter)"
                placeholder="BD"
                inputProps={{ maxLength: 2 }}
                value={formData.country || ''}
                onChange={(e) => setFormData({ ...formData, country: e.target.value.toUpperCase() })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={!formData.code || !formData.name}>
            {editingOrg ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Organizations;

