import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon, Visibility as ViewIcon, List as ManageItemsIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CreatePackageRequest,
  PackageResponse,
  PagedResponse,
  UpdatePackageRequest,
} from '../../services/hospitalCorporateDiscountService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PackagesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [packages, setPackages] = useState<PackageResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [codeFilter, setCodeFilter] = useState('');
  const [isPublicFilter, setIsPublicFilter] = useState<'' | 'true' | 'false'>('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewingPackage, setViewingPackage] = useState<PackageResponse | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<CreatePackageRequest>({
    code: '',
    name: '',
    description: undefined,
    defaultPrice: undefined,
    isCorporateOnly: false,
    isPublic: true,
  });

  const loadPackages = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; code?: string; isPublic?: boolean } = { page, size };
      if (codeFilter.trim()) params.code = codeFilter.trim();
      if (isPublicFilter === 'true') params.isPublic = true;
      if (isPublicFilter === 'false') params.isPublic = false;
      const response: PagedResponse<PackageResponse> = await hospitalCorporateDiscountService.getPackages(params);
      setPackages(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load packages', err);
      enqueueSnackbar('Failed to load packages', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, codeFilter, isPublicFilter, enqueueSnackbar]);

  useEffect(() => {
    loadPackages();
  }, [loadPackages]);

  const handleRefresh = () => {
    setPage(0);
    loadPackages();
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setForm({
      code: '',
      name: '',
      description: undefined,
      defaultPrice: undefined,
      isCorporateOnly: false,
      isPublic: true,
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (row: PackageResponse) => {
    setEditingId(row.id);
    setForm({
      code: row.code,
      name: row.name,
      description: row.description ?? undefined,
      defaultPrice: row.defaultPrice ?? undefined,
      isCorporateOnly: row.isCorporateOnly ?? false,
      isPublic: row.isPublic ?? true,
    });
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingId(null);
  };

  const handleSave = async () => {
    if (!form.code?.trim() || !form.name?.trim()) {
      enqueueSnackbar('Code and name are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      if (editingId) {
        await hospitalCorporateDiscountService.updatePackage(editingId, form as UpdatePackageRequest);
        enqueueSnackbar('Package updated', { variant: 'success' });
      } else {
        await hospitalCorporateDiscountService.createPackage(form);
        enqueueSnackbar('Package created', { variant: 'success' });
      }
      handleCloseDialog();
      loadPackages();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to save'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleView = (row: PackageResponse) => {
    setViewingPackage(row);
    setViewDialogOpen(true);
  };

  const handleManageItems = (packageId: string) => {
    navigate(`/hospital/corporate-discount/packages/${packageId}`);
  };

  const handlePageChange = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4">Corporate &amp; Discount – Packages</Typography>
          <Typography variant="body2">Manage service packages and package items</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Create package
        </Button>
      </Box>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Box display="flex" gap={2} flexWrap="wrap" alignItems="center" mb={2}>
            <TextField
              size="small"
              label="Code"
              value={codeFilter}
              onChange={(e) => setCodeFilter(e.target.value)}
              placeholder="Filter by code"
              sx={{ minWidth: 160 }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Public</InputLabel>
              <Select value={isPublicFilter} label="Public" onChange={(e) => setIsPublicFilter(e.target.value as '' | 'true' | 'false')}>
                <MenuItem value="">All</MenuItem>
                <MenuItem value="true">Yes</MenuItem>
                <MenuItem value="false">No</MenuItem>
              </Select>
            </FormControl>
            <Button startIcon={<RefreshIcon />} onClick={handleRefresh}>
              Refresh
            </Button>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Code</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell align="right">Default price</TableCell>
                    <TableCell>Public</TableCell>
                    <TableCell>Corporate only</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {packages.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        No packages found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    packages.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.code}</TableCell>
                        <TableCell>{row.name}</TableCell>
                        <TableCell align="right">{row.defaultPrice != null ? row.defaultPrice : '–'}</TableCell>
                        <TableCell>{row.isPublic ? 'Yes' : 'No'}</TableCell>
                        <TableCell>{row.isCorporateOnly ? 'Yes' : 'No'}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>
                            View
                          </Button>
                          <Button size="small" startIcon={<EditIcon />} onClick={() => handleOpenEdit(row)}>
                            Edit
                          </Button>
                          <Button size="small" startIcon={<ManageItemsIcon />} onClick={() => handleManageItems(row.id)}>
                            Manage items
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={handlePageChange}
            rowsPerPage={size}
            onRowsPerPageChange={handleRowsPerPageChange}
            rowsPerPageOptions={[10, 20, 50]}
            labelRowsPerPage="Rows per page:"
          />
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit package' : 'Create package'}</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Code"
              value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
              required
              fullWidth
              disabled={!!editingId}
            />
            <TextField label="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required fullWidth />
            <TextField
              label="Description"
              value={form.description ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value || undefined }))}
              fullWidth
              multiline
              rows={2}
            />
            <TextField
              type="number"
              label="Default price"
              value={form.defaultPrice ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, defaultPrice: e.target.value ? parseFloat(e.target.value) : undefined }))}
              inputProps={{ min: 0, step: 0.01 }}
              fullWidth
            />
            <FormControlLabel
              control={<Checkbox checked={form.isPublic ?? true} onChange={(e) => setForm((f) => ({ ...f, isPublic: e.target.checked }))} />}
              label="Public"
            />
            <FormControlLabel
              control={<Checkbox checked={form.isCorporateOnly ?? false} onChange={(e) => setForm((f) => ({ ...f, isCorporateOnly: e.target.checked }))} />}
              label="Corporate only"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Package details</DialogTitle>
        <DialogContent>
          {viewingPackage && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="Code" value={viewingPackage.code} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Name" value={viewingPackage.name} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Description" value={viewingPackage.description ?? '–'} InputProps={{ readOnly: true }} fullWidth multiline minRows={2} />
              <TextField label="Default price" value={viewingPackage.defaultPrice ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Public" value={viewingPackage.isPublic ? 'Yes' : 'No'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Corporate only" value={viewingPackage.isCorporateOnly ? 'Yes' : 'No'} InputProps={{ readOnly: true }} fullWidth />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button variant="outlined" startIcon={<ManageItemsIcon />} onClick={() => viewingPackage && (setViewDialogOpen(false), handleManageItems(viewingPackage.id))}>
            Manage items
          </Button>
          <Button variant="contained" onClick={() => viewingPackage && (setViewDialogOpen(false), handleOpenEdit(viewingPackage))}>
            Edit
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PackagesPage;
