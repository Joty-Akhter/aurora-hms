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
} from '@mui/material';
import { Calculate } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

interface InterestCalculation {
  interestCalculationId: string;
  epfAccountId: string;
  financialYear: string;
  interestRate: number;
  openingBalance: number;
  totalContributions: number;
  interestAmount: number;
  closingBalance: number;
  calculationDate: string;
  status: string;
}

const ProvidentFundInterest: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [calculations, setCalculations] = useState<InterestCalculation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState({
    epfAccountId: '',
    financialYear: '',
    interestRate: '8.50',
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadCalculations();
    }
  }, [currentOrganizationId]);

  const loadCalculations = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      // Load interest calculations
      setCalculations([]);
    } catch (err: any) {
      console.error('Failed to load interest calculations:', err);
      setError(err.response?.data?.message || 'Failed to load calculations');
    } finally {
      setLoading(false);
    }
  };

  const handleCalculate = async () => {
    try {
      await hrService.calculateInterest(
        formData.epfAccountId,
        formData.financialYear,
        parseFloat(formData.interestRate)
      );
      setOpenDialog(false);
      loadCalculations();
    } catch (err: any) {
      console.error('Failed to calculate interest:', err);
      setError(err.response?.data?.message || 'Failed to calculate interest');
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
        <Typography variant="h4">Interest Calculations</Typography>
        <Button
          variant="contained"
          startIcon={<Calculate />}
          onClick={() => setOpenDialog(true)}
        >
          Calculate Interest
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
                  <TableCell>Financial Year</TableCell>
                  <TableCell>Interest Rate (%)</TableCell>
                  <TableCell>Opening Balance</TableCell>
                  <TableCell>Contributions</TableCell>
                  <TableCell>Interest Amount</TableCell>
                  <TableCell>Closing Balance</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {calculations.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No interest calculations found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  calculations.map((calc) => (
                    <TableRow key={calc.interestCalculationId}>
                      <TableCell>{calc.epfAccountId}</TableCell>
                      <TableCell>{calc.financialYear}</TableCell>
                      <TableCell>{calc.interestRate}%</TableCell>
                      <TableCell>₹{calc.openingBalance.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{calc.totalContributions.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{calc.interestAmount.toLocaleString('en-IN')}</TableCell>
                      <TableCell>₹{calc.closingBalance.toLocaleString('en-IN')}</TableCell>
                      <TableCell>{calc.status}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Calculate Interest</DialogTitle>
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
            label="Financial Year (e.g., 2023-2024)"
            margin="normal"
            value={formData.financialYear}
            onChange={(e) => setFormData({ ...formData, financialYear: e.target.value })}
            required
            placeholder="2023-2024"
          />
          <TextField
            fullWidth
            label="Interest Rate (%)"
            type="number"
            margin="normal"
            value={formData.interestRate}
            onChange={(e) => setFormData({ ...formData, interestRate: e.target.value })}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCalculate}>Calculate</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundInterest;

