import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Add, Visibility, Edit } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService, { EpfAccount } from '../../services/hrService';
import './Hr.css';

const ProvidentFundAccounts: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [accounts, setAccounts] = useState<EpfAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<EpfAccount | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadAccounts();
    }
  }, [currentOrganizationId]);

  const loadAccounts = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.getEpfAccounts(currentOrganizationId);
      setAccounts(response.data || []);
    } catch (err: any) {
      console.error('Failed to load EPF accounts:', err);
      setError(err.response?.data?.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAccount = () => {
    setSelectedAccount(null);
    setOpenDialog(true);
  };

  const handleViewAccount = (account: EpfAccount) => {
    setSelectedAccount(account);
    setOpenDialog(true);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Provident Fund Accounts</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleCreateAccount}
        >
          Create Account
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Account Number</TableCell>
                  <TableCell>UAN Number</TableCell>
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Current Balance</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {accounts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No EPF accounts found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  accounts.map((account) => (
                    <TableRow key={account.epfAccountId}>
                      <TableCell>{account.epfAccountNumber}</TableCell>
                      <TableCell>{account.uanNumber || '-'}</TableCell>
                      <TableCell>{account.employeeId}</TableCell>
                      <TableCell>₹{account.currentBalance.toLocaleString('en-IN')}</TableCell>
                      <TableCell>
                        <Chip
                          label={account.isActive ? 'Active' : 'Inactive'}
                          color={account.isActive ? 'success' : 'default'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleViewAccount(account)}
                        >
                          <Visibility />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedAccount ? 'View EPF Account' : 'Create EPF Account'}
        </DialogTitle>
        <DialogContent>
          {selectedAccount ? (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Account Details
              </Typography>
              <Typography variant="body1" gutterBottom>
                Account Number: {selectedAccount.epfAccountNumber}
              </Typography>
              <Typography variant="body1" gutterBottom>
                Current Balance: ₹{selectedAccount.currentBalance.toLocaleString('en-IN')}
              </Typography>
              <Typography variant="body1" gutterBottom>
                Employee Contribution: ₹{selectedAccount.employeeContributionBalance.toLocaleString('en-IN')}
              </Typography>
              <Typography variant="body1" gutterBottom>
                Employer Contribution: ₹{selectedAccount.employerContributionBalance.toLocaleString('en-IN')}
              </Typography>
            </Box>
          ) : (
            <Box sx={{ mt: 2 }}>
              <TextField
                fullWidth
                label="Employee ID"
                margin="normal"
                required
              />
              <TextField
                fullWidth
                label="EPF Account Number"
                margin="normal"
                required
              />
              <TextField
                fullWidth
                label="UAN Number"
                margin="normal"
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Close</Button>
          {!selectedAccount && <Button variant="contained">Create</Button>}
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundAccounts;

