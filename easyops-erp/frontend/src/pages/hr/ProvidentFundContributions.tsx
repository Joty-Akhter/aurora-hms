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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Alert,
  MenuItem,
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService, { EpfContribution } from '../../services/hrService';
import './Hr.css';

const ProvidentFundContributions: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [contributions, setContributions] = useState<EpfContribution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState({
    epfAccountId: '',
    employeeId: '',
    contributionMonth: new Date().getMonth() + 1,
    contributionYear: new Date().getFullYear(),
    employeeBasicSalary: '',
    employeeContributionRate: '12.00',
    employerContributionRate: '12.00',
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadContributions();
    }
  }, [currentOrganizationId]);

  const loadContributions = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      // This would need to be implemented in hrService
      // For now, we'll show a placeholder
      setContributions([]);
    } catch (err: any) {
      console.error('Failed to load contributions:', err);
      setError(err.response?.data?.message || 'Failed to load contributions');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      await hrService.createContribution({
        ...formData,
        employeeBasicSalary: parseFloat(formData.employeeBasicSalary),
        employeeContributionRate: parseFloat(formData.employeeContributionRate),
        employerContributionRate: parseFloat(formData.employerContributionRate),
      });
      setOpenDialog(false);
      loadContributions();
    } catch (err: any) {
      console.error('Failed to create contribution:', err);
      setError(err.response?.data?.message || 'Failed to create contribution');
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
        <Typography variant="h4">Provident Fund Contributions</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setOpenDialog(true)}
        >
          Create Contribution
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
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Period</TableCell>
                  <TableCell>Basic Salary</TableCell>
                  <TableCell>Employee Contribution</TableCell>
                  <TableCell>Employer Contribution</TableCell>
                  <TableCell>Total Contribution</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {contributions.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No contributions found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  contributions.map((contrib) => (
                    <TableRow key={contrib.contributionId}>
                      <TableCell>{contrib.epfAccountId}</TableCell>
                      <TableCell>{contrib.employeeId}</TableCell>
                      <TableCell>
                        {contrib.contributionMonth}/{contrib.contributionYear}
                      </TableCell>
                      <TableCell>₹{contrib.employeeBasicSalary.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{contrib.employeeContributionAmount.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{contrib.employerContributionAmount.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{contrib.totalContribution.toLocaleString('en-IN')}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Contribution</DialogTitle>
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
            label="Month"
            type="number"
            margin="normal"
            value={formData.contributionMonth}
            onChange={(e) => setFormData({ ...formData, contributionMonth: parseInt(e.target.value) })}
            select
            required
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((month) => (
              <MenuItem key={month} value={month}>
                {month}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            label="Year"
            type="number"
            margin="normal"
            value={formData.contributionYear}
            onChange={(e) => setFormData({ ...formData, contributionYear: parseInt(e.target.value) })}
            required
          />
          <TextField
            fullWidth
            label="Basic Salary"
            type="number"
            margin="normal"
            value={formData.employeeBasicSalary}
            onChange={(e) => setFormData({ ...formData, employeeBasicSalary: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Employee Contribution Rate (%)"
            type="number"
            margin="normal"
            value={formData.employeeContributionRate}
            onChange={(e) => setFormData({ ...formData, employeeContributionRate: e.target.value })}
            required
          />
          <TextField
            fullWidth
            label="Employer Contribution Rate (%)"
            type="number"
            margin="normal"
            value={formData.employerContributionRate}
            onChange={(e) => setFormData({ ...formData, employerContributionRate: e.target.value })}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Create</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundContributions;

