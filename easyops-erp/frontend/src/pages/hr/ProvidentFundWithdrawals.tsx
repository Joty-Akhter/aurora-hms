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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Alert,
  MenuItem,
} from '@mui/material';
import { Add, CheckCircle, Cancel } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

interface Withdrawal {
  withdrawalId: string;
  epfAccountId: string;
  employeeId: string;
  withdrawalType: string;
  requestedAmount: number;
  approvedAmount?: number;
  requestDate: string;
  status: string;
  reason?: string;
}

const ProvidentFundWithdrawals: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState({
    epfAccountId: '',
    employeeId: '',
    withdrawalType: 'partial',
    requestedAmount: '',
    reason: '',
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadWithdrawals();
    }
  }, [currentOrganizationId]);

  const loadWithdrawals = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      // This would need accountId - for now placeholder
      setWithdrawals([]);
    } catch (err: any) {
      console.error('Failed to load withdrawals:', err);
      setError(err.response?.data?.message || 'Failed to load withdrawals');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      await hrService.createWithdrawal({
        ...formData,
        requestedAmount: parseFloat(formData.requestedAmount),
        organizationId: currentOrganizationId,
      });
      setOpenDialog(false);
      loadWithdrawals();
    } catch (err: any) {
      console.error('Failed to create withdrawal:', err);
      setError(err.response?.data?.message || 'Failed to create withdrawal');
    }
  };

  const handleApprove = async (withdrawalId: string) => {
    try {
      // Approve withdrawal logic
      loadWithdrawals();
    } catch (err: any) {
      console.error('Failed to approve withdrawal:', err);
      setError(err.response?.data?.message || 'Failed to approve withdrawal');
    }
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
        <Typography variant="h4">Provident Fund Withdrawals</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setOpenDialog(true)}
        >
          Create Withdrawal Request
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Account ID</TableCell>
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Requested Amount</TableCell>
                  <TableCell>Request Date</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {withdrawals.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No withdrawal requests found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  withdrawals.map((withdrawal) => (
                    <TableRow key={withdrawal.withdrawalId}>
                      <TableCell>{withdrawal.epfAccountId}</TableCell>
                      <TableCell>{withdrawal.employeeId}</TableCell>
                      <TableCell>{withdrawal.withdrawalType}</TableCell>
                      <TableCell>₹{withdrawal.requestedAmount.toLocaleString('en-IN')}</TableCell>
                      <TableCell>{new Date(withdrawal.requestDate).toLocaleDateString()}</TableCell>
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
                      <TableCell>
                        {withdrawal.status === 'pending' && (
                          <>
                            <Button
                              size="small"
                              color="success"
                              startIcon={<CheckCircle />}
                              onClick={() => handleApprove(withdrawal.withdrawalId)}
                            >
                              Approve
                            </Button>
                            <Button
                              size="small"
                              color="error"
                              startIcon={<Cancel />}
                            >
                              Reject
                            </Button>
                          </>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Withdrawal Request</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="EPF Account ID"
            margin="normal"
            value={formData.epfAccountId}
            onChange={(e) => setFormData({ ...formData, epfAccountId: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Employee ID"
            margin="normal"
            value={formData.employeeId}
            onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
            required
          />
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
            margin="normal"
            multiline
            rows={3}
            value={formData.reason}
            onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Submit</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundWithdrawals;

