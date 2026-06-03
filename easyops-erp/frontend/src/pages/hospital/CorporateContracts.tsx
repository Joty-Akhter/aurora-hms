import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
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
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon, Visibility as ViewIcon, AccountBalanceWallet as CoverageIcon } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CorporateResponse,
  ContractResponse,
  CreateContractRequest,
  PagedResponse,
  UpdateContractRequest,
} from '../../services/hospitalCorporateDiscountService';
import './Hospital.css';

const COVERAGE_TYPES = ['CASHLESS', 'REIMBURSEMENT', 'MIXED'];

const CorporateContractsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const corporateIdFromUrl = searchParams.get('corporateId') ?? '';

  const [loading, setLoading] = useState(false);
  const [corporates, setCorporates] = useState<CorporateResponse[]>([]);
  const [selectedCorporateId, setSelectedCorporateId] = useState(corporateIdFromUrl);
  const [selectedCorporate, setSelectedCorporate] = useState<CorporateResponse | null>(null);
  const [contracts, setContracts] = useState<ContractResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [statusFilter, setStatusFilter] = useState('');
  const [totalElements, setTotalElements] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewingContract, setViewingContract] = useState<ContractResponse | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<CreateContractRequest>({
    contractCode: '',
    contractName: '',
    validFrom: '',
    validTo: '',
    coverageType: 'CASHLESS',
    serviceLocations: '',
  });

  const loadCorporatesForDropdown = useCallback(async () => {
    try {
      const res = await hospitalCorporateDiscountService.getCorporates({ page: 0, size: 500 });
      setCorporates(res.content);
    } catch {
      setCorporates([]);
    }
  }, []);

  const loadContracts = useCallback(async () => {
    if (!selectedCorporateId) {
      setContracts([]);
      setTotalElements(0);
      return;
    }
    try {
      setLoading(true);
      const params: { page: number; size: number; status?: string } = { page, size };
      if (statusFilter.trim()) params.status = statusFilter.trim();
      const [contractsRes, corporateRes] = await Promise.all([
        hospitalCorporateDiscountService.getContractsByCorporate(selectedCorporateId, params),
        hospitalCorporateDiscountService.getCorporate(selectedCorporateId),
      ]);
      setContracts(contractsRes.content);
      setTotalElements(contractsRes.totalElements);
      setSelectedCorporate(corporateRes);
    } catch (err) {
      console.error('Failed to load contracts', err);
      enqueueSnackbar('Failed to load contracts', { variant: 'error' });
      setContracts([]);
      setSelectedCorporate(null);
    } finally {
      setLoading(false);
    }
  }, [selectedCorporateId, page, size, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadCorporatesForDropdown();
  }, [loadCorporatesForDropdown]);

  useEffect(() => {
    setSelectedCorporateId((prev) => (corporateIdFromUrl || prev));
  }, [corporateIdFromUrl]);

  useEffect(() => {
    if (selectedCorporateId) {
      setSearchParams({ corporateId: selectedCorporateId }, { replace: true });
      loadContracts();
    } else {
      setContracts([]);
      setSelectedCorporate(null);
    }
  }, [selectedCorporateId, page, loadContracts, setSearchParams]);

  const handleRefresh = () => {
    setPage(0);
    loadContracts();
  };

  const handleCorporateChange = (id: string) => {
    setSelectedCorporateId(id || '');
    setPage(0);
  };

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value);
    setPage(0);
  };

  const handleOpenCreate = () => {
    setEditingId(null);
    setForm({
      contractCode: '',
      contractName: '',
      validFrom: new Date().toISOString().slice(0, 10),
      validTo: '',
      coverageType: 'CASHLESS',
      serviceLocations: '',
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (row: ContractResponse) => {
    setEditingId(row.id);
    setForm({
      contractCode: row.contractCode,
      contractName: row.contractName ?? '',
      validFrom: row.validFrom,
      validTo: row.validTo ?? '',
      coverageType: row.coverageType,
      serviceLocations: row.serviceLocations ?? '',
    });
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingId(null);
  };

  const handleView = (row: ContractResponse) => {
    setViewingContract(row);
    setViewDialogOpen(true);
  };

  const handlePageChange = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  const handleSave = async () => {
    if (!selectedCorporateId || !form.contractCode?.trim() || !form.validFrom || !form.coverageType?.trim()) {
      enqueueSnackbar('Contract code, valid from and coverage type are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      if (editingId) {
        await hospitalCorporateDiscountService.updateContract(editingId, form as UpdateContractRequest);
        enqueueSnackbar('Contract updated', { variant: 'success' });
      } else {
        await hospitalCorporateDiscountService.createContract(selectedCorporateId, form);
        enqueueSnackbar('Contract created', { variant: 'success' });
      }
      handleCloseDialog();
      loadContracts();
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'response' in err && err.response && typeof (err.response as { data?: { message?: string } }).data?.message === 'string'
        ? (err.response as { data: { message: string } }).data.message
        : 'Failed to save';
      enqueueSnackbar(msg, { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4">Corporate & Discount – Contracts</Typography>
          <Typography variant="body2">Manage contracts for a corporate client</Typography>
        </Box>
        {selectedCorporateId && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate} sx={{ bgcolor: 'white', color: 'primary.main' }}>
            Create contract
          </Button>
        )}
      </Box>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <FormControl size="small" sx={{ minWidth: 280 }} fullWidth>
            <InputLabel>Corporate</InputLabel>
            <Select
              value={selectedCorporateId}
              label="Corporate"
              onChange={(e) => handleCorporateChange(e.target.value)}
            >
              <MenuItem value="">Select corporate…</MenuItem>
              {corporates.map((c) => (
                <MenuItem key={c.id} value={c.id}>{c.code} – {c.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          {selectedCorporateId && (
            <>
              {selectedCorporate && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  {selectedCorporate.name} ({selectedCorporate.code})
                </Typography>
              )}
              <Box display="flex" alignItems="center" gap={2} flexWrap="wrap" mt={2} mb={1}>
                <FormControl size="small" sx={{ minWidth: 140 }}>
                  <InputLabel>Status</InputLabel>
                  <Select
                    value={statusFilter}
                    label="Status"
                    onChange={(e) => handleStatusFilterChange(e.target.value)}
                  >
                    <MenuItem value="">All</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="EXPIRED">Expired</MenuItem>
                    <MenuItem value="FUTURE">Future</MenuItem>
                  </Select>
                </FormControl>
                <Button size="small" startIcon={<RefreshIcon />} onClick={handleRefresh}>Refresh</Button>
              </Box>

              {loading ? (
                <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Contract code</TableCell>
                        <TableCell>Contract name</TableCell>
                        <TableCell>Corporate</TableCell>
                        <TableCell>Valid from</TableCell>
                        <TableCell>Valid to</TableCell>
                        <TableCell>Coverage type</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {contracts.length === 0 ? (
                        <TableRow><TableCell colSpan={7} align="center">No contracts for this corporate.</TableCell></TableRow>
                      ) : (
                        contracts.map((row) => (
                          <TableRow key={row.id}>
                            <TableCell>{row.contractCode}</TableCell>
                            <TableCell>{row.contractName || '–'}</TableCell>
                            <TableCell>{selectedCorporate ? `${selectedCorporate.name} (${selectedCorporate.code})` : '–'}</TableCell>
                            <TableCell>{row.validFrom}</TableCell>
                            <TableCell>{row.validTo ?? '–'}</TableCell>
                            <TableCell>{row.coverageType}</TableCell>
                            <TableCell align="right">
                              <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>View</Button>
                              <Button size="small" startIcon={<EditIcon />} onClick={() => handleOpenEdit(row)}>Edit</Button>
                              <Button size="small" startIcon={<CoverageIcon />} onClick={() => navigate(`/hospital/corporate-discount/contracts/${row.id}/coverage`)}>Coverage &amp; tariffs</Button>
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
            </>
          )}

          {!selectedCorporateId && corporates.length > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              Select a corporate above to view and manage contracts.
            </Typography>
          )}
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit contract' : 'Create contract'}</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField label="Contract code" value={form.contractCode} onChange={(e) => setForm((f) => ({ ...f, contractCode: e.target.value }))} required fullWidth disabled={!!editingId} />
            <TextField label="Contract name" value={form.contractName ?? ''} onChange={(e) => setForm((f) => ({ ...f, contractName: e.target.value }))} fullWidth />
            <TextField type="date" label="Valid from" value={form.validFrom ?? ''} onChange={(e) => setForm((f) => ({ ...f, validFrom: e.target.value }))} required InputLabelProps={{ shrink: true }} fullWidth />
            <TextField type="date" label="Valid to" value={form.validTo ?? ''} onChange={(e) => setForm((f) => ({ ...f, validTo: e.target.value || undefined }))} InputLabelProps={{ shrink: true }} fullWidth />
            <FormControl fullWidth>
              <InputLabel>Coverage type</InputLabel>
              <Select value={form.coverageType} label="Coverage type" onChange={(e) => setForm((f) => ({ ...f, coverageType: e.target.value }))}>
                {COVERAGE_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField label="Service locations" value={form.serviceLocations ?? ''} onChange={(e) => setForm((f) => ({ ...f, serviceLocations: e.target.value }))} fullWidth multiline rows={2} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>Save</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Contract details</DialogTitle>
        <DialogContent>
          {viewingContract && selectedCorporate && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="Corporate" value={`${selectedCorporate.name} (${selectedCorporate.code})`} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Contract code" value={viewingContract.contractCode} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Contract name" value={viewingContract.contractName ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid from" value={viewingContract.validFrom} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid to" value={viewingContract.validTo ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Coverage type" value={viewingContract.coverageType} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Service locations" value={viewingContract.serviceLocations ?? '–'} InputProps={{ readOnly: true }} fullWidth multiline minRows={2} />
            </Box>
          )}
          {viewingContract && !selectedCorporate && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="Contract code" value={viewingContract.contractCode} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Contract name" value={viewingContract.contractName ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid from" value={viewingContract.validFrom} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Valid to" value={viewingContract.validTo ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Coverage type" value={viewingContract.coverageType} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Service locations" value={viewingContract.serviceLocations ?? '–'} InputProps={{ readOnly: true }} fullWidth multiline minRows={2} />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          <Button variant="outlined" startIcon={<CoverageIcon />} onClick={() => viewingContract && (setViewDialogOpen(false), navigate(`/hospital/corporate-discount/contracts/${viewingContract.id}/coverage`))}>
            Coverage &amp; tariffs
          </Button>
          <Button variant="contained" onClick={() => viewingContract && (setViewDialogOpen(false), handleOpenEdit(viewingContract))}>
            Edit
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CorporateContractsPage;
