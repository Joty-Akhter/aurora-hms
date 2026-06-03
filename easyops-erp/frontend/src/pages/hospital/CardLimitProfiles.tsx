import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Refresh as RefreshIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalCardManagementService, {
  CardWithLimitUsageResponse,
  CreateLimitProfileRequest,
  LimitProfileResponse,
  PagedResponse,
} from '../../services/hospitalCardManagementService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const RESET_POLICIES = ['DAILY', 'MONTHLY', 'PER_EPISODE'];

const CardLimitProfilesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();

  const [loading, setLoading] = useState<boolean>(false);
  const [profiles, setProfiles] = useState<LimitProfileResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [nameFilter, setNameFilter] = useState<string>('');

  const [createOpen, setCreateOpen] = useState<boolean>(false);
  const [viewOpen, setViewOpen] = useState<boolean>(false);
  const [selectedProfile, setSelectedProfile] = useState<LimitProfileResponse | null>(null);
  const [cardsWithUsage, setCardsWithUsage] = useState<CardWithLimitUsageResponse[]>([]);
  const [cardsWithUsageLoading, setCardsWithUsageLoading] = useState<boolean>(false);

  const [newProfile, setNewProfile] = useState<CreateLimitProfileRequest>({
    name: '',
    description: '',
    dailyAmountLimit: undefined,
    monthlyAmountLimit: undefined,
    dailyMealLimit: undefined,
    dailyVisitLimit: undefined,
    resetPolicy: 'DAILY',
    currency: 'INR',
  });

  const loadProfiles = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; name?: string } = { page, size };
      if (nameFilter.trim()) params.name = nameFilter.trim();
      const response: PagedResponse<LimitProfileResponse> =
        await hospitalCardManagementService.getLimitProfiles(params);
      setProfiles(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load limit profiles', err);
      enqueueSnackbar('Failed to load limit profiles', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, nameFilter, enqueueSnackbar]);

  useEffect(() => {
    loadProfiles();
  }, [loadProfiles]);

  const handleRefresh = () => {
    setPage(0);
    loadProfiles();
  };

  const handleApplyFilters = () => {
    setPage(0);
    loadProfiles();
  };

  const handleCreateOpen = () => {
    setNewProfile({
      name: '',
      description: '',
      dailyAmountLimit: undefined,
      monthlyAmountLimit: undefined,
      dailyMealLimit: undefined,
      dailyVisitLimit: undefined,
      resetPolicy: 'DAILY',
      currency: 'INR',
    });
    setCreateOpen(true);
  };

  const handleCreateSubmit = async () => {
    if (!newProfile.name?.trim()) {
      enqueueSnackbar('Name is required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      await hospitalCardManagementService.createLimitProfile(newProfile);
      enqueueSnackbar('Limit profile created', { variant: 'success' });
      setCreateOpen(false);
      loadProfiles();
    } catch (err: unknown) {
      console.error('Failed to create limit profile', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to create limit profile'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleView = (row: LimitProfileResponse) => {
    setSelectedProfile(row);
    setCardsWithUsage([]);
    setViewOpen(true);
  };

  useEffect(() => {
    if (!viewOpen || !selectedProfile?.id) return;
    let cancelled = false;
    setCardsWithUsageLoading(true);
    hospitalCardManagementService
      .getLimitProfileCardsWithUsage(selectedProfile.id)
      .then((list) => {
        if (!cancelled) setCardsWithUsage(list);
      })
      .catch((err) => {
        if (!cancelled) {
          console.error('Failed to load cards using profile', err);
          enqueueSnackbar('Failed to load cards using this profile', { variant: 'error' });
        }
      })
      .finally(() => {
        if (!cancelled) setCardsWithUsageLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [viewOpen, selectedProfile?.id, enqueueSnackbar]);

  const handleViewCard = (cardId: string) => {
    setViewOpen(false);
    navigate('/hospital/cards', { state: { openCardId: cardId } });
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Cards – Limit profiles
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreateOpen} disabled={loading}>
            Create limit profile
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Name"
              size="small"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
              sx={{ minWidth: 200 }}
              placeholder="Search by name"
            />
            <Button variant="outlined" size="medium" onClick={handleApplyFilters}>
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {loading && profiles.length === 0 ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell align="right">Daily amount limit</TableCell>
                    <TableCell align="right">Monthly amount limit</TableCell>
                    <TableCell align="right">Daily meal limit</TableCell>
                    <TableCell>Reset policy</TableCell>
                    <TableCell>Currency</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {profiles.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No limit profiles found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    profiles.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.name}</TableCell>
                        <TableCell align="right">
                          {row.dailyAmountLimit != null ? row.dailyAmountLimit : '—'}
                        </TableCell>
                        <TableCell align="right">
                          {row.monthlyAmountLimit != null ? row.monthlyAmountLimit : '—'}
                        </TableCell>
                        <TableCell align="right">
                          {row.dailyMealLimit != null ? row.dailyMealLimit : '—'}
                        </TableCell>
                        <TableCell>{row.resetPolicy}</TableCell>
                        <TableCell>{row.currency ?? '—'}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>
                            View
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {totalElements > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Total: {totalElements}
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create limit profile</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Name"
              required
              size="small"
              value={newProfile.name}
              onChange={(e) => setNewProfile({ ...newProfile, name: e.target.value })}
            />
            <TextField
              label="Description"
              size="small"
              multiline
              value={newProfile.description ?? ''}
              onChange={(e) => setNewProfile({ ...newProfile, description: e.target.value })}
            />
            <TextField
              label="Daily amount limit"
              type="number"
              size="small"
              inputProps={{ min: 0, step: 0.01 }}
              value={newProfile.dailyAmountLimit ?? ''}
              onChange={(e) =>
                setNewProfile({
                  ...newProfile,
                  dailyAmountLimit: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <TextField
              label="Monthly amount limit"
              type="number"
              size="small"
              inputProps={{ min: 0, step: 0.01 }}
              value={newProfile.monthlyAmountLimit ?? ''}
              onChange={(e) =>
                setNewProfile({
                  ...newProfile,
                  monthlyAmountLimit: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <TextField
              label="Daily meal limit"
              type="number"
              size="small"
              inputProps={{ min: 0 }}
              value={newProfile.dailyMealLimit ?? ''}
              onChange={(e) =>
                setNewProfile({
                  ...newProfile,
                  dailyMealLimit: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <TextField
              label="Daily visit limit"
              type="number"
              size="small"
              inputProps={{ min: 0 }}
              value={newProfile.dailyVisitLimit ?? ''}
              onChange={(e) =>
                setNewProfile({
                  ...newProfile,
                  dailyVisitLimit: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <TextField
              label="Reset policy"
              select
              size="small"
              value={newProfile.resetPolicy}
              onChange={(e) => setNewProfile({ ...newProfile, resetPolicy: e.target.value })}
            >
              {RESET_POLICIES.map((p) => (
                <MenuItem key={p} value={p}>
                  {p}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Currency"
              size="small"
              value={newProfile.currency ?? 'INR'}
              onChange={(e) => setNewProfile({ ...newProfile, currency: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateSubmit} disabled={loading}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* View dialog */}
      <Dialog open={viewOpen} onClose={() => setViewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Limit profile</DialogTitle>
        <DialogContent>
          {selectedProfile && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <Box>
                <Typography><strong>Name:</strong> {selectedProfile.name}</Typography>
                <Typography><strong>Description:</strong> {selectedProfile.description ?? '—'}</Typography>
                <Typography><strong>Daily amount limit:</strong> {selectedProfile.dailyAmountLimit != null ? selectedProfile.dailyAmountLimit : '—'}</Typography>
                <Typography><strong>Monthly amount limit:</strong> {selectedProfile.monthlyAmountLimit != null ? selectedProfile.monthlyAmountLimit : '—'}</Typography>
                <Typography><strong>Daily meal limit:</strong> {selectedProfile.dailyMealLimit != null ? selectedProfile.dailyMealLimit : '—'}</Typography>
                <Typography><strong>Daily visit limit:</strong> {selectedProfile.dailyVisitLimit != null ? selectedProfile.dailyVisitLimit : '—'}</Typography>
                <Typography><strong>Reset policy:</strong> {selectedProfile.resetPolicy}</Typography>
                <Typography><strong>Currency:</strong> {selectedProfile.currency ?? '—'}</Typography>
              </Box>
              <Typography variant="subtitle2" gutterBottom>Cards using this profile</Typography>
              {cardsWithUsageLoading ? (
                <Box display="flex" justifyContent="center" py={2}>
                  <CircularProgress size={24} />
                </Box>
              ) : cardsWithUsage.length === 0 ? (
                <Typography variant="body2" color="text.secondary">No cards use this profile.</Typography>
              ) : (
                <TableContainer sx={{ maxHeight: 220 }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell>Card number</TableCell>
                        <TableCell>Owner</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Usage (current period)</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {cardsWithUsage.map((row) => (
                        <TableRow key={row.cardId}>
                          <TableCell>{row.cardNumber}</TableCell>
                          <TableCell>{row.ownerType} – {row.ownerReferenceId}</TableCell>
                          <TableCell>{row.status}</TableCell>
                          <TableCell>
                            {row.limitUsage ? (
                              <>
                                {row.limitUsage.amountConsumed ?? 0}
                                {(row.limitUsage.dailyAmountLimit != null || row.limitUsage.monthlyAmountLimit != null) && (
                                  <> / {row.limitUsage.dailyAmountLimit ?? row.limitUsage.monthlyAmountLimit ?? '—'}</>
                                )}
                                {row.limitUsage.dailyMealLimit != null && (
                                  <> · Meals: {row.limitUsage.mealCountConsumed ?? 0}/{row.limitUsage.dailyMealLimit}</>
                                )}
                                {row.limitUsage.dailyVisitLimit != null && (
                                  <> · Visits: {row.limitUsage.visitCountConsumed ?? 0}/{row.limitUsage.dailyVisitLimit}</>
                                )}
                              </>
                            ) : '—'}
                          </TableCell>
                          <TableCell align="right">
                            <Button size="small" startIcon={<ViewIcon />} onClick={() => handleViewCard(row.cardId)}>
                              View
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CardLimitProfilesPage;
