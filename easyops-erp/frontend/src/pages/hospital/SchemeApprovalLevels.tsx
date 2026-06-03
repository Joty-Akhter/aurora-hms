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
  CreateApprovalLevelRequest,
  DiscountApprovalLevelResponse,
  DiscountSchemeDetailResponse,
} from '../../services/hospitalCorporateDiscountService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const SchemeApprovalLevelsPage: React.FC = () => {
  const { schemeId } = useParams<{ schemeId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(true);
  const [scheme, setScheme] = useState<DiscountSchemeDetailResponse | null>(null);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [levelForm, setLevelForm] = useState<CreateApprovalLevelRequest>({
    roleOrGroupId: '',
    maxDiscountPercent: undefined,
    maxDiscountAmount: undefined,
    sortOrder: 0,
  });

  const loadScheme = useCallback(async () => {
    if (!schemeId) return;
    try {
      setLoading(true);
      const data = await hospitalCorporateDiscountService.getDiscountScheme(schemeId);
      setScheme(data);
    } catch (err) {
      console.error('Failed to load discount scheme', err);
      enqueueSnackbar('Failed to load discount scheme', { variant: 'error' });
      setScheme(null);
    } finally {
      setLoading(false);
    }
  }, [schemeId, enqueueSnackbar]);

  useEffect(() => {
    loadScheme();
  }, [loadScheme]);

  const handleBack = () => navigate('/hospital/corporate-discount/discount-schemes');

  const handleOpenAddLevel = () => {
    setLevelForm({
      roleOrGroupId: '',
      maxDiscountPercent: undefined,
      maxDiscountAmount: undefined,
      sortOrder: (scheme?.approvalLevels?.length ?? 0),
    });
    setAddDialogOpen(true);
  };

  const handleAddLevel = async () => {
    if (!schemeId || !levelForm.roleOrGroupId?.trim()) {
      enqueueSnackbar('Role or group is required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.addApprovalLevel(schemeId, {
        ...levelForm,
        sortOrder: levelForm.sortOrder ?? 0,
      });
      enqueueSnackbar('Approval level added', { variant: 'success' });
      setAddDialogOpen(false);
      loadScheme();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to add approval level'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveLevel = async (levelId: string) => {
    if (!schemeId || !window.confirm('Remove this approval level?')) return;
    try {
      setLoading(true);
      await hospitalCorporateDiscountService.deleteApprovalLevel(schemeId, levelId);
      enqueueSnackbar('Approval level removed', { variant: 'success' });
      loadScheme();
    } catch (err: unknown) {
      enqueueSnackbar('Failed to remove approval level', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!schemeId) {
    return (
      <Box className="hospital-page">
        <Typography color="error">Missing scheme ID</Typography>
        <Button onClick={() => navigate('/hospital/corporate-discount/discount-schemes')}>Back to discount schemes</Button>
      </Box>
    );
  }

  const levels = (scheme?.approvalLevels ?? []) as DiscountApprovalLevelResponse[];

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Button startIcon={<ArrowBackIcon />} onClick={handleBack} sx={{ color: 'white', mb: 1 }}>
            Back
          </Button>
          <Typography variant="h4">Approval levels</Typography>
          <Typography variant="body2">
            {scheme ? `${scheme.code} – ${scheme.name}` : `Scheme ${schemeId}`}
          </Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={loadScheme} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Refresh
        </Button>
      </Box>

      {loading && !scheme ? (
        <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
      ) : scheme ? (
        <>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary">
                Code: {scheme.code} · Name: {scheme.name} · Discount: {scheme.discountType} {scheme.discountValue}
                {scheme.discountType === 'PERCENT' ? '%' : ''} · Status: {scheme.status}
              </Typography>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Approval levels</Typography>
                <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleOpenAddLevel}>
                  Add level
                </Button>
              </Box>
              {loading ? (
                <Box display="flex" justifyContent="center" py={2}><CircularProgress size={24} /></Box>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Sort order</TableCell>
                        <TableCell>Role or group</TableCell>
                        <TableCell align="right">Max discount %</TableCell>
                        <TableCell align="right">Max discount amount</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {levels.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={5} align="center">
                            No approval levels. Add one to define who can approve discounts.
                          </TableCell>
                        </TableRow>
                      ) : (
                        levels.map((level) => (
                          <TableRow key={level.id}>
                            <TableCell>{level.sortOrder ?? 0}</TableCell>
                            <TableCell>{level.roleOrGroupId}</TableCell>
                            <TableCell align="right">{level.maxDiscountPercent != null ? level.maxDiscountPercent : '–'}</TableCell>
                            <TableCell align="right">{level.maxDiscountAmount != null ? level.maxDiscountAmount : '–'}</TableCell>
                            <TableCell align="right">
                              <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleRemoveLevel(level.id)}>
                                Remove
                              </Button>
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
      ) : null}

      <Dialog open={addDialogOpen} onClose={() => setAddDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add approval level</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Role or group ID"
              value={levelForm.roleOrGroupId}
              onChange={(e) => setLevelForm((f) => ({ ...f, roleOrGroupId: e.target.value }))}
              required
              fullWidth
              placeholder="e.g. BILLING_APPROVER or group UUID"
            />
            <TextField
              type="number"
              label="Max discount %"
              value={levelForm.maxDiscountPercent ?? ''}
              onChange={(e) => setLevelForm((f) => ({ ...f, maxDiscountPercent: e.target.value ? parseFloat(e.target.value) : undefined }))}
              inputProps={{ min: 0, max: 100, step: 0.01 }}
              fullWidth
            />
            <TextField
              type="number"
              label="Max discount amount"
              value={levelForm.maxDiscountAmount ?? ''}
              onChange={(e) => setLevelForm((f) => ({ ...f, maxDiscountAmount: e.target.value ? parseFloat(e.target.value) : undefined }))}
              inputProps={{ min: 0, step: 0.01 }}
              fullWidth
            />
            <TextField
              type="number"
              label="Sort order"
              value={levelForm.sortOrder ?? 0}
              onChange={(e) => setLevelForm((f) => ({ ...f, sortOrder: parseInt(e.target.value, 10) || 0 }))}
              inputProps={{ min: 0 }}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddLevel}>Add</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchemeApprovalLevelsPage;
