import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  AccountBalance,
  History,
  FileDownload,
  PersonAdd,
  Add,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService, {
  getMyEmployeeProfile,
  getEmployees,
  type Employee,
} from '../../services/hrService';
import './Hr.css';

const ProvidentFundEmployeeSelfService: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [contributions, setContributions] = useState<any[]>([]);
  const [withdrawals, setWithdrawals] = useState<any[]>([]);
  const [nominations, setNominations] = useState<any[]>([]);
  const [statement, setStatement] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openWithdrawalDialog, setOpenWithdrawalDialog] = useState(false);
  const [openNominationDialog, setOpenNominationDialog] = useState(false);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [formData, setFormData] = useState({
    withdrawalType: 'partial',
    requestedAmount: '',
    reason: '',
    nomineeName: '',
    relationship: '',
    sharePercentage: '',
  });

  const [linkedEmployeeId, setLinkedEmployeeId] = useState<string | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState<string | null>(null);
  /** Bump after mutations so tab data reload runs without referencing a stale closure. */
  const [reloadNonce, setReloadNonce] = useState(0);

  useEffect(() => {
    if (!currentOrganizationId || !user?.id) {
      setProfileLoading(false);
      setLinkedEmployeeId(null);
      setProfileError(!currentOrganizationId ? 'Please select an organization.' : null);
      return;
    }
    let cancelled = false;
    (async () => {
      setProfileLoading(true);
      setProfileError(null);
      try {
        const meRes = await getMyEmployeeProfile(currentOrganizationId);
        const me = meRes.data as Employee | undefined;
        const id = me?.employeeId ?? me?.id ?? null;
        if (cancelled) return;
        if (!id) {
          setLinkedEmployeeId(null);
          setProfileError('No employee profile is linked to this user. Please contact HR.');
        } else {
          setLinkedEmployeeId(id);
          setProfileError(null);
        }
      } catch {
        try {
          const emRes = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
          const list = (emRes.data || []) as Employee[];
          const match = list.find((e) => e.userId === user.id);
          const id = match?.employeeId ?? match?.id ?? null;
          if (cancelled) return;
          if (!id) {
            setLinkedEmployeeId(null);
            setProfileError('No employee profile is linked to this user. Please contact HR.');
          } else {
            setLinkedEmployeeId(id);
            setProfileError(null);
          }
        } catch {
          if (!cancelled) {
            setLinkedEmployeeId(null);
            setProfileError('Failed to load employee profile.');
          }
        }
      } finally {
        if (!cancelled) setProfileLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId, user?.id]);

  useEffect(() => {
    if (!linkedEmployeeId || profileLoading) return;

    const loadData = async () => {
      try {
        setLoading(true);
        if (tabValue === 0) {
          const response = await hrService.getMyEpfAccount(linkedEmployeeId);
          setAccounts(response.data || []);
          if (response.data && response.data.length > 0) {
            setSelectedAccountId(response.data[0].epfAccountId);
          }
        } else if (tabValue === 1 && selectedAccountId) {
          const response = await hrService.getMyContributions(linkedEmployeeId, selectedAccountId);
          setContributions(response.data || []);
        } else if (tabValue === 2) {
          const response = await hrService.getMyWithdrawals(linkedEmployeeId);
          setWithdrawals(response.data || []);
        } else if (tabValue === 3 && selectedAccountId) {
          const response = await hrService.getMyNominations(linkedEmployeeId, selectedAccountId);
          setNominations(response.data || []);
        }
      } catch (err: any) {
        console.error('Failed to load data:', err);
        setError(err.response?.data?.message || 'Failed to load data');
      } finally {
        setLoading(false);
      }
    };

    void loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- tab-driven PF loads share one loader
  }, [linkedEmployeeId, profileLoading, tabValue, selectedAccountId, reloadNonce]);

  const handleDownloadStatement = async () => {
    if (!linkedEmployeeId) {
      setError('Employee profile not loaded');
      return;
    }
    if (!selectedAccountId) {
      setError('Please select an account');
      return;
    }
    try {
      setLoading(true);
      const response = await hrService.downloadMyStatement(linkedEmployeeId, selectedAccountId);
      setStatement(response.data);
    } catch (err: any) {
      console.error('Failed to download statement:', err);
      setError(err.response?.data?.message || 'Failed to download statement');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitWithdrawal = async () => {
    if (!linkedEmployeeId) {
      setError('Employee profile not loaded');
      return;
    }
    try {
      await hrService.submitWithdrawalRequest({
        epfAccountId: selectedAccountId,
        employeeId: linkedEmployeeId,
        withdrawalType: formData.withdrawalType,
        requestedAmount: parseFloat(formData.requestedAmount),
        reason: formData.reason,
      });
      setOpenWithdrawalDialog(false);
      setReloadNonce((n) => n + 1);
    } catch (err: any) {
      console.error('Failed to submit withdrawal:', err);
      setError(err.response?.data?.message || 'Failed to submit withdrawal');
    }
  };

  const handleCreateNomination = async () => {
    if (!linkedEmployeeId) {
      setError('Employee profile not loaded');
      return;
    }
    try {
      await hrService.createMyNomination({
        epfAccountId: selectedAccountId,
        employeeId: linkedEmployeeId,
        nomineeName: formData.nomineeName,
        relationship: formData.relationship,
        sharePercentage: parseFloat(formData.sharePercentage),
      });
      setOpenNominationDialog(false);
      setReloadNonce((n) => n + 1);
    } catch (err: any) {
      console.error('Failed to create nomination:', err);
      setError(err.response?.data?.message || 'Failed to create nomination');
    }
  };

  if (profileLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (profileError) {
    return (
      <div className="hr-page">
        <Alert severity="error">{profileError}</Alert>
      </div>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          My Provident Fund
        </Typography>
        <Typography variant="body2" color="text.secondary">
          View your EPF account, contributions, withdrawals, and manage nominations
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="My Account" icon={<AccountBalance />} />
            <Tab label="Contributions" icon={<History />} />
            <Tab label="Withdrawals" icon={<AccountBalance />} />
            <Tab label="Nominations" icon={<PersonAdd />} />
            <Tab label="Statements" icon={<FileDownload />} />
          </Tabs>
        </Box>

        <CardContent>
          {tabValue === 0 && (
            <>
              {accounts.length === 0 ? (
                <Alert severity="info">No EPF account found</Alert>
              ) : (
                <Grid container spacing={2}>
                  {accounts.map((account) => (
                    <Grid item xs={12} md={6} key={account.epfAccountId}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="h6">{account.epfAccountNumber}</Typography>
                          <Typography variant="body2" color="text.secondary" gutterBottom>
                            UAN: {account.uanNumber || 'N/A'}
                          </Typography>
                          <Box sx={{ mt: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                              Current Balance
                            </Typography>
                            <Typography variant="h5" color="primary">
                              ₹{account.currentBalance.toLocaleString('en-IN')}
                            </Typography>
                          </Box>
                          <Box sx={{ mt: 2 }}>
                            <Chip
                              label={account.isActive ? 'Active' : 'Inactive'}
                              color={account.isActive ? 'success' : 'default'}
                              size="small"
                            />
                          </Box>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </>
          )}

          {tabValue === 1 && (
            <>
              {!selectedAccountId ? (
                <Alert severity="info">Please select an account from "My Account" tab</Alert>
              ) : (
                <>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Period</TableCell>
                          <TableCell>Employee Contribution</TableCell>
                          <TableCell>Employer Contribution</TableCell>
                          <TableCell>Total</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {contributions.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={4} align="center">
                              <Typography variant="body2" color="text.secondary">
                                No contributions found
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ) : (
                          contributions.map((contrib) => (
                            <TableRow key={contrib.contributionId}>
                              <TableCell>
                                {contrib.contributionMonth}/{contrib.contributionYear}
                              </TableCell>
                              <TableCell>₹{contrib.employeeContributionAmount.toLocaleString('en-IN')}</TableCell>
                              <TableCell>₹{contrib.employerContributionAmount.toLocaleString('en-IN')}</TableCell>
                              <TableCell>₹{contrib.totalContribution.toLocaleString('en-IN')}</TableCell>
                            </TableRow>
                          ))
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </>
              )}
            </>
          )}

          {tabValue === 2 && (
            <>
              <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<Add />}
                  onClick={() => setOpenWithdrawalDialog(true)}
                  disabled={!selectedAccountId}
                >
                  Request Withdrawal
                </Button>
              </Box>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Request Date</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Amount</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {withdrawals.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No withdrawal requests found
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ) : (
                      withdrawals.map((withdrawal) => (
                        <TableRow key={withdrawal.withdrawalId}>
                          <TableCell>
                            {new Date(withdrawal.requestDate).toLocaleDateString()}
                          </TableCell>
                          <TableCell>{withdrawal.withdrawalType}</TableCell>
                          <TableCell>₹{withdrawal.requestedAmount.toLocaleString('en-IN')}</TableCell>
                          <TableCell>
                            <Chip
                              label={withdrawal.status}
                              color={
                                withdrawal.status === 'approved' ? 'success' :
                                withdrawal.status === 'pending' ? 'warning' :
                                withdrawal.status === 'rejected' ? 'error' : 'default'
                              }
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}

          {tabValue === 3 && (
            <>
              <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<Add />}
                  onClick={() => setOpenNominationDialog(true)}
                  disabled={!selectedAccountId}
                >
                  Add Nomination
                </Button>
              </Box>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Nominee Name</TableCell>
                      <TableCell>Relationship</TableCell>
                      <TableCell>Share %</TableCell>
                      <TableCell>Primary</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {nominations.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No nominations found
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ) : (
                      nominations.map((nomination) => (
                        <TableRow key={nomination.nominationId}>
                          <TableCell>{nomination.nomineeName}</TableCell>
                          <TableCell>{nomination.relationship}</TableCell>
                          <TableCell>{nomination.sharePercentage}%</TableCell>
                          <TableCell>
                            <Chip
                              label={nomination.isPrimary ? 'Yes' : 'No'}
                              color={nomination.isPrimary ? 'primary' : 'default'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}

          {tabValue === 4 && (
            <>
              <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<FileDownload />}
                  onClick={handleDownloadStatement}
                  disabled={!selectedAccountId || loading}
                >
                  {loading ? <CircularProgress size={24} /> : 'Download Statement'}
                </Button>
              </Box>
              {statement && (
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Account Statement
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Period: {statement.period}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Opening Balance: ₹{statement.openingBalance?.toLocaleString('en-IN') || '0'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Closing Balance: ₹{statement.closingBalance?.toLocaleString('en-IN') || '0'}
                    </Typography>
                  </CardContent>
                </Card>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={openWithdrawalDialog} onClose={() => setOpenWithdrawalDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Withdrawal</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Withdrawal Type"
            margin="normal"
            value={formData.withdrawalType}
            onChange={(e) => setFormData({ ...formData, withdrawalType: e.target.value })}
            select
            required
          >
            <MenuItem value="partial">Partial</MenuItem>
            <MenuItem value="full">Full</MenuItem>
            <MenuItem value="pension">Pension</MenuItem>
          </TextField>
          <TextField
            fullWidth
            label="Requested Amount"
            type="number"
            margin="normal"
            value={formData.requestedAmount}
            onChange={(e) => setFormData({ ...formData, requestedAmount: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Reason"
            multiline
            rows={3}
            margin="normal"
            value={formData.reason}
            onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenWithdrawalDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmitWithdrawal}>Submit</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={openNominationDialog} onClose={() => setOpenNominationDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Nomination</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Nominee Name"
            margin="normal"
            value={formData.nomineeName}
            onChange={(e) => setFormData({ ...formData, nomineeName: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Relationship"
            margin="normal"
            value={formData.relationship}
            onChange={(e) => setFormData({ ...formData, relationship: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Share Percentage"
            type="number"
            margin="normal"
            value={formData.sharePercentage}
            onChange={(e) => setFormData({ ...formData, sharePercentage: e.target.value })}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenNominationDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateNomination}>Add</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundEmployeeSelfService;

