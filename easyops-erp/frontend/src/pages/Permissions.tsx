import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Tooltip,
  FormControlLabel,
  Switch,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  VpnKey as PermissionIcon,
  CheckCircle,
  Cancel,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import rbacService from '@services/rbacService';
import { Permission, PermissionRequest } from '@types/index';
import { useAuth } from '@contexts/AuthContext';
import Autocomplete from '@mui/material/Autocomplete';

const DEFAULT_RESOURCES = [
  'dashboard',
  'organizations',
  'users',
  'roles',
  'permissions',
  'accounting',
  'sales',
  'inventory',
  'purchase',
  'hr',
  'crm',
  'manufacturing',
  'pharma',
  'hospital',
  'system',
  'audit',
  'notifications',
  'reports',
];

const DEFAULT_ACTIONS = ['view', 'manage', 'create', 'read', 'update', 'delete', 'admin', 'configure', 'export'];

const Permissions: React.FC = () => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterResource, setFilterResource] = useState<string>('all');
  const [filterAction, setFilterAction] = useState<string>('all');
  const [openDialog, setOpenDialog] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [currentPermission, setCurrentPermission] = useState<Permission | null>(null);
  const [newPermission, setNewPermission] = useState<PermissionRequest>({
    name: '',
    code: '',
    resource: '',
    action: '',
    description: '',
    isActive: true,
  });
  const [allPermissionsCache, setAllPermissionsCache] = useState<Permission[]>([]);
  const [isFilteredView, setIsFilteredView] = useState(false);
  const [resourceOptions, setResourceOptions] = useState<string[]>([...DEFAULT_RESOURCES]);
  const [actionOptions, setActionOptions] = useState<string[]>([...DEFAULT_ACTIONS]);
  const [filterOptionsLoading, setFilterOptionsLoading] = useState(false);
  const [dialogSaving, setDialogSaving] = useState(false);

  const { enqueueSnackbar } = useSnackbar();
  const { canManageResource } = useAuth();
  const canManagePermissions = canManageResource('permissions');

  const updateFilterOptions = useCallback((records: Permission[]) => {
    const resources = Array.from(
      new Set([
        ...DEFAULT_RESOURCES,
        ...records.map((permission) => permission.resource).filter(Boolean),
      ])
    ).sort();
    const actions = Array.from(
      new Set([
        ...DEFAULT_ACTIONS,
        ...records.map((permission) => permission.action).filter(Boolean),
      ])
    ).sort();

    setResourceOptions(resources);
    setActionOptions(actions);
  }, []);

  const refreshPermissionCache = useCallback(
    async (silent = false) => {
      try {
        setFilterOptionsLoading(true);
        const response = await rbacService.getAllPermissions({ page: 0, size: 1000 });
        const items = response.content || [];
        setAllPermissionsCache(items);
        updateFilterOptions(items);
      } catch (error: any) {
        if (!silent) {
          const errorMessage = error.response?.data?.error || 'Failed to load permissions cache';
          enqueueSnackbar(errorMessage, { variant: 'error' });
        }
      } finally {
        setFilterOptionsLoading(false);
      }
    },
    [enqueueSnackbar, updateFilterOptions]
  );

  const loadPermissionsPage = useCallback(
    async (pageIndex: number, pageSize: number) => {
      try {
        setLoading(true);
        const response = await rbacService.getAllPermissions({ page: pageIndex, size: pageSize });
        setPermissions(response.content || []);
        setTotalElements(response.totalElements || 0);
        setIsFilteredView(false);
      } catch (error: any) {
        const errorMessage = error.response?.data?.error || 'Failed to load permissions';
        enqueueSnackbar(errorMessage, { variant: 'error' });
      } finally {
        setLoading(false);
      }
    },
    [enqueueSnackbar]
  );

  useEffect(() => {
    refreshPermissionCache(true);
  }, [refreshPermissionCache]);

  useEffect(() => {
    if (!isFilteredView) {
      loadPermissionsPage(page, rowsPerPage);
    }
  }, [page, rowsPerPage, isFilteredView, loadPermissionsPage]);

  const applyFiltersAndSearch = useCallback(
    async (showToast = false) => {
      setLoading(true);

      try {
        const noFiltersApplied =
          filterResource === 'all' &&
          filterAction === 'all' &&
          !searchTerm.trim();

        if (noFiltersApplied) {
          setPage(0);
          await loadPermissionsPage(0, rowsPerPage);
          return;
        }

        if (!allPermissionsCache.length) {
          await refreshPermissionCache(true);
        }

        let dataset = allPermissionsCache;

        if (filterResource !== 'all') {
          dataset = dataset.filter((permission) => permission.resource === filterResource);
        }

        if (filterAction !== 'all') {
          dataset = dataset.filter((permission) => permission.action === filterAction);
        }

        if (searchTerm.trim()) {
          const term = searchTerm.toLowerCase();
          dataset = dataset.filter((permission) =>
            permission.name.toLowerCase().includes(term) ||
            permission.code.toLowerCase().includes(term) ||
            permission.resource.toLowerCase().includes(term) ||
            permission.action.toLowerCase().includes(term) ||
            (permission.description && permission.description.toLowerCase().includes(term))
          );

          if (showToast) {
            enqueueSnackbar(
              dataset.length
                ? `Found ${dataset.length} permission(s)`
                : 'No permissions found matching your search',
              { variant: dataset.length ? 'success' : 'info' }
            );
          }
        } else if (showToast) {
          enqueueSnackbar(
            dataset.length
              ? `Filtered to ${dataset.length} permission(s)`
              : 'No permissions match the selected filters',
            { variant: dataset.length ? 'info' : 'warning' }
          );
        }

        setIsFilteredView(true);
        setPage(0);
        setPermissions(dataset);
        setTotalElements(dataset.length);
      } catch (error: any) {
        const errorMessage = error.response?.data?.error || 'Failed to filter permissions';
        enqueueSnackbar(errorMessage, { variant: 'error' });
      } finally {
        setLoading(false);
      }
    },
    [
      allPermissionsCache,
      enqueueSnackbar,
      filterAction,
      filterResource,
      loadPermissionsPage,
      refreshPermissionCache,
      rowsPerPage,
      searchTerm,
    ]
  );

  const handleSearch = async () => {
    await applyFiltersAndSearch(true);
  };

  const handleCreatePermission = async () => {
    if (!canManagePermissions) {
      enqueueSnackbar('You do not have permission to manage permissions', { variant: 'warning' });
      return;
    }

    try {
      setDialogSaving(true);
      await rbacService.createPermission({
        ...newPermission,
        code: newPermission.code.toUpperCase(),
      });
      enqueueSnackbar('Permission created successfully', { variant: 'success' });
      setOpenDialog(false);
      resetForm();
      await refreshPermissionCache(true);
      if (isFilteredView) {
        await applyFiltersAndSearch();
      } else {
        setPage(0);
        await loadPermissionsPage(0, rowsPerPage);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to create permission';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setDialogSaving(false);
    }
  };

  const handleUpdatePermission = async () => {
    if (!currentPermission) return;

    if (!canManagePermissions) {
      enqueueSnackbar('You do not have permission to manage permissions', { variant: 'warning' });
      return;
    }

    try {
      setDialogSaving(true);
      await rbacService.updatePermission(currentPermission.id, {
        ...newPermission,
        code: newPermission.code.toUpperCase(),
      });
      enqueueSnackbar('Permission updated successfully', { variant: 'success' });
      setOpenDialog(false);
      resetForm();
      await refreshPermissionCache(true);
      if (isFilteredView) {
        await applyFiltersAndSearch();
      } else {
        await loadPermissionsPage(page, rowsPerPage);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to update permission';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setDialogSaving(false);
    }
  };

  const handleEditPermission = (permission: Permission) => {
    setCurrentPermission(permission);
    setNewPermission({
      name: permission.name,
      code: permission.code,
      resource: permission.resource,
      action: permission.action,
      description: permission.description || '',
      isActive: permission.isActive,
    });
    setEditMode(true);
    setOpenDialog(true);
  };

  const handleToggleActive = async (permission: Permission) => {
    if (!canManagePermissions) {
      enqueueSnackbar('You do not have permission to manage permissions', { variant: 'warning' });
      return;
    }

    try {
      await rbacService.updatePermission(permission.id, {
        name: permission.name,
        code: permission.code,
        resource: permission.resource,
        action: permission.action,
        description: permission.description || '',
        isActive: !permission.isActive,
      });
      enqueueSnackbar('Permission status updated', { variant: 'success' });
      await refreshPermissionCache(true);
      if (isFilteredView) {
        await applyFiltersAndSearch();
      } else {
        await loadPermissionsPage(page, rowsPerPage);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to update permission status';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    }
  };

  const handleDeletePermission = async (permissionId: string) => {
    if (!canManagePermissions) {
      enqueueSnackbar('You do not have permission to manage permissions', { variant: 'warning' });
      return;
    }

    if (window.confirm('Are you sure you want to delete this permission?')) {
      try {
        await rbacService.deletePermission(permissionId);
        enqueueSnackbar('Permission deleted successfully', { variant: 'success' });
        await refreshPermissionCache(true);
        if (isFilteredView) {
          await applyFiltersAndSearch();
        } else {
          await loadPermissionsPage(page, rowsPerPage);
        }
      } catch (error: any) {
        const errorMessage = error.response?.data?.error || 'Failed to delete permission';
        enqueueSnackbar(errorMessage, { variant: 'error' });
      }
    }
  };

  const resetForm = () => {
    setNewPermission({
      name: '',
      code: '',
      resource: '',
      action: '',
      description: '',
      isActive: true,
    });
    setCurrentPermission(null);
    setEditMode(false);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    resetForm();
  };

  const getActionColor = (action: string) => {
    const colors: Record<string, "default" | "primary" | "secondary" | "error" | "info" | "success" | "warning"> = {
      create: 'success',
      read: 'info',
      update: 'warning',
      delete: 'error',
      manage: 'primary',
      view: 'info',
      configure: 'secondary',
      export: 'default',
    };
    return colors[action] || 'default';
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Permission Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Define and manage system permissions
          </Typography>
        </Box>
        <Tooltip
          title={
            canManagePermissions
              ? 'Create a new permission'
              : 'You do not have permission to create permissions'
          }
        >
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                resetForm();
                setOpenDialog(true);
              }}
              disabled={!canManagePermissions}
            >
              Add Permission
            </Button>
          </span>
        </Tooltip>
      </Box>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
          <TextField
            fullWidth
            placeholder="Search permissions by name, code, or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Filter by Resource</InputLabel>
            <Select
              value={filterResource}
              label="Filter by Resource"
              onChange={(e) => setFilterResource(e.target.value)}
              disabled={filterOptionsLoading}
            >
              <MenuItem value="all">All Resources</MenuItem>
              {resourceOptions
                .filter((resource) => !!resource)
                .map((resource) => (
                  <MenuItem key={resource} value={resource}>
                    {resource.charAt(0).toUpperCase() + resource.slice(1)}
                  </MenuItem>
                ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Filter by Action</InputLabel>
            <Select
              value={filterAction}
              label="Filter by Action"
              onChange={(e) => setFilterAction(e.target.value)}
              disabled={filterOptionsLoading}
            >
              <MenuItem value="all">All Actions</MenuItem>
              {actionOptions
                .filter((action) => !!action)
                .map((action) => (
                  <MenuItem key={action} value={action}>
                    {action.charAt(0).toUpperCase() + action.slice(1)}
                  </MenuItem>
                ))}
            </Select>
          </FormControl>
          <Button onClick={handleSearch} variant="outlined" disabled={loading}>
            Search
          </Button>
        </Box>
      </Paper>

      <TableContainer component={Paper}>
        {loading ? (
          <Box display="flex" justifyContent="center" p={4}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Permission Name</TableCell>
                  <TableCell>Code</TableCell>
                  <TableCell>Resource</TableCell>
                  <TableCell>Action</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {permissions.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary" py={4}>
                        No permissions found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  permissions.map((permission) => (
                    <TableRow key={permission.id} hover>
                      <TableCell>
                        <Box display="flex" alignItems="center">
                          <PermissionIcon sx={{ mr: 1, color: 'primary.main' }} fontSize="small" />
                          <Typography fontWeight="medium">{permission.name}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={permission.code} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={permission.resource}
                          size="small"
                          color="secondary"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={permission.action}
                          size="small"
                          color={getActionColor(permission.action)}
                        />
                      </TableCell>
                      <TableCell>{permission.description || '-'}</TableCell>
                      <TableCell>
                        <Tooltip
                          title={
                            canManagePermissions
                              ? 'Toggle permission status'
                              : 'You do not have permission to update permissions'
                          }
                        >
                          <span style={{ display: 'inline-block' }}>
                            <Chip
                              icon={permission.isActive ? <CheckCircle /> : <Cancel />}
                              label={permission.isActive ? 'Active' : 'Inactive'}
                              color={permission.isActive ? 'success' : 'default'}
                              size="small"
                              onClick={
                                canManagePermissions ? () => handleToggleActive(permission) : undefined
                              }
                              sx={{
                                cursor: canManagePermissions ? 'pointer' : 'not-allowed',
                                opacity: canManagePermissions ? 1 : 0.75,
                              }}
                            />
                          </span>
                        </Tooltip>
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip
                          title={
                            canManagePermissions
                              ? 'Edit permission'
                              : 'You do not have permission to edit permissions'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleEditPermission(permission)}
                              disabled={!canManagePermissions}
                            >
                              <EditIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip
                          title={
                            canManagePermissions
                              ? 'Delete permission'
                              : 'You do not have permission to delete permissions'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDeletePermission(permission.id)}
                              disabled={!canManagePermissions}
                            >
                              <DeleteIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={totalElements}
              page={page}
              onPageChange={(_, newPage) => setPage(newPage)}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={(e) => {
                setRowsPerPage(parseInt(e.target.value, 10));
                setPage(0);
              }}
            />
          </>
        )}
      </TableContainer>

      {/* Create/Edit Permission Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editMode ? 'Edit Permission' : 'Create New Permission'}
        </DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Permission Name"
            fullWidth
            required
            value={newPermission.name}
            onChange={(e) => setNewPermission({ ...newPermission, name: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Permission Code"
            fullWidth
            required
            value={newPermission.code}
            onChange={(e) => setNewPermission({ ...newPermission, code: e.target.value.toUpperCase() })}
            helperText="Unique identifier in UPPER_CASE format (e.g., USER_CREATE)"
          />
          <Autocomplete
            freeSolo
            options={resourceOptions}
            value={newPermission.resource || ''}
            onChange={(_, value) =>
              setNewPermission({ ...newPermission, resource: typeof value === 'string' ? value : '' })
            }
            onInputChange={(_, value) =>
              setNewPermission({ ...newPermission, resource: value })
            }
            loading={filterOptionsLoading}
            renderInput={(params) => (
              <TextField
                {...params}
                margin="dense"
                label="Resource"
                required
              />
            )}
          />
          <Autocomplete
            freeSolo
            options={actionOptions}
            value={newPermission.action || ''}
            onChange={(_, value) =>
              setNewPermission({ ...newPermission, action: typeof value === 'string' ? value : '' })
            }
            onInputChange={(_, value) =>
              setNewPermission({ ...newPermission, action: value })
            }
            renderInput={(params) => (
              <TextField
                {...params}
                margin="dense"
                label="Action"
                required
              />
            )}
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            multiline
            rows={3}
            value={newPermission.description}
            onChange={(e) => setNewPermission({ ...newPermission, description: e.target.value })}
          />
          <FormControlLabel
            control={
              <Switch
                checked={newPermission.isActive}
                onChange={(e) => setNewPermission({ ...newPermission, isActive: e.target.checked })}
                disabled={!canManagePermissions || dialogSaving}
              />
            }
            label="Active"
            sx={{ mt: 2 }}
          />
          <Box sx={{ mt: 2, p: 2, bgcolor: 'warning.lighter', borderRadius: 1 }}>
            <Typography variant="caption" color="warning.dark">
              ⚠️ Note: Permissions define what actions users can perform on resources. Assign permissions to roles in the Role Management page.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={editMode ? handleUpdatePermission : handleCreatePermission}
            variant="contained"
            disabled={
              !canManagePermissions ||
              dialogSaving ||
              !newPermission.name ||
              !newPermission.code ||
              !newPermission.resource ||
              !newPermission.action
            }
          >
            {dialogSaving ? 'Saving...' : editMode ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Permissions;

