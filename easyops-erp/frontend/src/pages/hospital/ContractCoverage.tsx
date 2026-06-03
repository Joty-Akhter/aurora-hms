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
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, ArrowBack as ArrowBackIcon, Delete as DeleteIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  ContractResponse,
  CoverageRuleResponse,
  CreateCoverageRuleRequest,
  CreateCorporateTariffRequest,
  CorporateTariffResponse,
} from '../../services/hospitalCorporateDiscountService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const SCOPE_TYPES = ['SERVICE_CODE', 'SERVICE_GROUP', 'DEPARTMENT'];
const TARIFF_TYPES = ['FIXED', 'PERCENT_OF_BASE'];

const ContractCoveragePage: React.FC = () => {
  const { contractId } = useParams<{ contractId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(true);
  const [contract, setContract] = useState<ContractResponse | null>(null);
  const [coverageRules, setCoverageRules] = useState<CoverageRuleResponse[]>([]);
  const [tariffs, setTariffs] = useState<CorporateTariffResponse[]>([]);
  const [coverageDialogOpen, setCoverageDialogOpen] = useState(false);
  const [tariffDialogOpen, setTariffDialogOpen] = useState(false);
  const [coverageForm, setCoverageForm] = useState<CreateCoverageRuleRequest>({
    scopeType: 'SERVICE_CODE',
    scopeValue: '',
    coveragePercent: 100,
    maxAmount: undefined,
    coPayPercent: 0,
    deductibleAmount: 0,
    applicableVisitTypes: '',
  });
  const [tariffForm, setTariffForm] = useState<CreateCorporateTariffRequest>({
    scopeType: 'SERVICE_CODE',
    scopeValue: '',
    tariffType: 'FIXED',
    tariffAmount: undefined,
    tariffPercent: undefined,
  });

  const loadData = useCallback(async () => {
    if (!contractId) return;
    try {
      setLoading(true);
      const [contractRes, rulesRes, tariffsRes] = await Promise.all([
        hospitalCorporateDiscountService.getContract(contractId),
        hospitalCorporateDiscountService.getCoverageRules(contractId),
        hospitalCorporateDiscountService.getTariffs(contractId),
      ]);
      setContract(contractRes);
      setCoverageRules(rulesRes);
      setTariffs(tariffsRes);
    } catch (err) {
      console.error('Failed to load contract coverage', err);
      enqueueSnackbar('Failed to load contract', { variant: 'error' });
      setContract(null);
      setCoverageRules([]);
      setTariffs([]);
    } finally {
      setLoading(false);
    }
  }, [contractId, enqueueSnackbar]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleBack = () => {
    if (contract?.corporateClientId) {
      navigate(`/hospital/corporate-discount/contracts?corporateId=${contract.corporateClientId}`);
    } else {
      navigate('/hospital/corporate-discount/contracts');
    }
  };

  const handleOpenCoverageDialog = () => {
    setCoverageForm({
      scopeType: 'SERVICE_CODE',
      scopeValue: '',
      coveragePercent: 100,
      maxAmount: undefined,
      coPayPercent: 0,
      deductibleAmount: 0,
      applicableVisitTypes: '',
    });
    setCoverageDialogOpen(true);
  };

  const handleOpenTariffDialog = () => {
    setTariffForm({
      scopeType: 'SERVICE_CODE',
      scopeValue: '',
      tariffType: 'FIXED',
      tariffAmount: undefined,
      tariffPercent: undefined,
    });
    setTariffDialogOpen(true);
  };

  const handleAddCoverageRule = async () => {
    if (!contractId || !coverageForm.scopeValue?.trim()) {
      enqueueSnackbar('Scope value is required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.createCoverageRule(contractId, {
        ...coverageForm,
        maxAmount: coverageForm.maxAmount ?? undefined,
        coPayPercent: coverageForm.coPayPercent ?? 0,
        deductibleAmount: coverageForm.deductibleAmount ?? 0,
        applicableVisitTypes: coverageForm.applicableVisitTypes?.trim() || undefined,
      });
      enqueueSnackbar('Coverage rule added', { variant: 'success' });
      setCoverageDialogOpen(false);
      loadData();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to add coverage rule'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCoverageRule = async (id: string) => {
    if (!window.confirm('Delete this coverage rule?')) return;
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.deleteCoverageRule(id);
      enqueueSnackbar('Coverage rule deleted', { variant: 'success' });
      loadData();
    } catch (err: unknown) {
      enqueueSnackbar('Failed to delete coverage rule', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleAddTariff = async () => {
    if (!contractId || !tariffForm.scopeValue?.trim()) {
      enqueueSnackbar('Scope value is required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.createTariff(contractId, tariffForm);
      enqueueSnackbar('Tariff added', { variant: 'success' });
      setTariffDialogOpen(false);
      loadData();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to add tariff'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTariff = async (id: string) => {
    if (!window.confirm('Delete this tariff?')) return;
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.deleteTariff(id);
      enqueueSnackbar('Tariff deleted', { variant: 'success' });
      loadData();
    } catch (err: unknown) {
      enqueueSnackbar('Failed to delete tariff', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!contractId) {
    return (
      <Box className="hospital-page">
        <Typography color="error">Missing contract ID</Typography>
        <Button onClick={() => navigate('/hospital/corporate-discount/contracts')}>Back to contracts</Button>
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Button startIcon={<ArrowBackIcon />} onClick={handleBack} sx={{ color: 'white', mb: 1 }}>
            Back
          </Button>
          <Typography variant="h4">Coverage &amp; Tariffs</Typography>
          <Typography variant="body2">
            {contract ? `${contract.contractCode}${contract.contractName ? ` – ${contract.contractName}` : ''}` : `Contract ${contractId}`}
          </Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={loadData} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Refresh
        </Button>
      </Box>

      {loading && !contract ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : (
        <>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Coverage rules</Typography>
                <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleOpenCoverageDialog}>
                  Add rule
                </Button>
              </Box>
              {loading ? (
                <Box display="flex" justifyContent="center" py={2}><CircularProgress size={24} /></Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Scope type</TableCell>
                        <TableCell>Scope value</TableCell>
                        <TableCell align="right">Coverage %</TableCell>
                        <TableCell align="right">Max amount</TableCell>
                        <TableCell align="right">Co-pay %</TableCell>
                        <TableCell align="right">Deductible</TableCell>
                        <TableCell>Visit types</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {coverageRules.length === 0 ? (
                        <TableRow><TableCell colSpan={8} align="center">No coverage rules. Add one to define what is covered.</TableCell></TableRow>
                      ) : (
                        coverageRules.map((r) => (
                          <TableRow key={r.id}>
                            <TableCell>{r.scopeType}</TableCell>
                            <TableCell>{r.scopeValue}</TableCell>
                            <TableCell align="right">{r.coveragePercent}</TableCell>
                            <TableCell align="right">{r.maxAmount != null ? r.maxAmount : '–'}</TableCell>
                            <TableCell align="right">{r.coPayPercent ?? 0}</TableCell>
                            <TableCell align="right">{r.deductibleAmount ?? 0}</TableCell>
                            <TableCell>{r.applicableVisitTypes || '–'}</TableCell>
                            <TableCell align="right">
                              <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDeleteCoverageRule(r.id)}>Delete</Button>
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Tariffs</Typography>
                <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleOpenTariffDialog}>
                  Add tariff
                </Button>
              </Box>
              {loading ? (
                <Box display="flex" justifyContent="center" py={2}><CircularProgress size={24} /></Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Scope type</TableCell>
                        <TableCell>Scope value</TableCell>
                        <TableCell>Tariff type</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell align="right">Percent</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {tariffs.length === 0 ? (
                        <TableRow><TableCell colSpan={6} align="center">No tariffs. Add one to define corporate-specific pricing.</TableCell></TableRow>
                      ) : (
                        tariffs.map((t) => (
                          <TableRow key={t.id}>
                            <TableCell>{t.scopeType}</TableCell>
                            <TableCell>{t.scopeValue}</TableCell>
                            <TableCell>{t.tariffType}</TableCell>
                            <TableCell align="right">{t.tariffAmount != null ? t.tariffAmount : '–'}</TableCell>
                            <TableCell align="right">{t.tariffPercent != null ? t.tariffPercent : '–'}</TableCell>
                            <TableCell align="right">
                              <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDeleteTariff(t.id)}>Delete</Button>
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </>
      )}

      <Dialog open={coverageDialogOpen} onClose={() => setCoverageDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add coverage rule</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth>
              <InputLabel>Scope type</InputLabel>
              <Select value={coverageForm.scopeType} label="Scope type" onChange={(e) => setCoverageForm((f) => ({ ...f, scopeType: e.target.value }))}>
                {SCOPE_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Scope value" value={coverageForm.scopeValue} onChange={(e) => setCoverageForm((f) => ({ ...f, scopeValue: e.target.value }))} required fullWidth />
            <TextField type="number" label="Coverage %" value={coverageForm.coveragePercent ?? ''} onChange={(e) => setCoverageForm((f) => ({ ...f, coveragePercent: parseFloat(e.target.value) || 0 }))} inputProps={{ min: 0, max: 100, step: 0.01 }} fullWidth />
            <TextField type="number" label="Max amount" value={coverageForm.maxAmount ?? ''} onChange={(e) => setCoverageForm((f) => ({ ...f, maxAmount: e.target.value ? parseFloat(e.target.value) : undefined }))} inputProps={{ min: 0, step: 0.01 }} fullWidth />
            <TextField type="number" label="Co-pay %" value={coverageForm.coPayPercent ?? 0} onChange={(e) => setCoverageForm((f) => ({ ...f, coPayPercent: parseFloat(e.target.value) || 0 }))} inputProps={{ min: 0, max: 100 }} fullWidth />
            <TextField type="number" label="Deductible amount" value={coverageForm.deductibleAmount ?? 0} onChange={(e) => setCoverageForm((f) => ({ ...f, deductibleAmount: parseFloat(e.target.value) || 0 }))} inputProps={{ min: 0 }} fullWidth />
            <TextField label="Applicable visit types (e.g. OP,IP,ED)" value={coverageForm.applicableVisitTypes ?? ''} onChange={(e) => setCoverageForm((f) => ({ ...f, applicableVisitTypes: e.target.value }))} fullWidth placeholder="OP,IP,ED" />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCoverageDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddCoverageRule}>Add</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={tariffDialogOpen} onClose={() => setTariffDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add tariff</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl fullWidth>
              <InputLabel>Scope type</InputLabel>
              <Select value={tariffForm.scopeType} label="Scope type" onChange={(e) => setTariffForm((f) => ({ ...f, scopeType: e.target.value }))}>
                {SCOPE_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Scope value" value={tariffForm.scopeValue} onChange={(e) => setTariffForm((f) => ({ ...f, scopeValue: e.target.value }))} required fullWidth />
            <FormControl fullWidth>
              <InputLabel>Tariff type</InputLabel>
              <Select value={tariffForm.tariffType} label="Tariff type" onChange={(e) => setTariffForm((f) => ({ ...f, tariffType: e.target.value }))}>
                {TARIFF_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField type="number" label="Tariff amount (FIXED)" value={tariffForm.tariffAmount ?? ''} onChange={(e) => setTariffForm((f) => ({ ...f, tariffAmount: e.target.value ? parseFloat(e.target.value) : undefined }))} inputProps={{ min: 0, step: 0.01 }} fullWidth />
            <TextField type="number" label="Tariff % (PERCENT_OF_BASE)" value={tariffForm.tariffPercent ?? ''} onChange={(e) => setTariffForm((f) => ({ ...f, tariffPercent: e.target.value ? parseFloat(e.target.value) : undefined }))} inputProps={{ min: 0, step: 0.01 }} fullWidth />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTariffDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddTariff}>Add</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ContractCoveragePage;
