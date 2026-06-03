import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  TextField,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import { Download, Print } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const ProvidentFundStatements: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statement, setStatement] = useState<any>(null);
  const [formData, setFormData] = useState({
    employeeId: '',
    epfAccountId: '',
    startDate: '',
    endDate: '',
  });

  const handleGenerate = async () => {
    if (!formData.epfAccountId || !formData.employeeId) {
      setError('Please provide Employee ID and EPF Account ID');
      return;
    }

    try {
      setLoading(true);
      const response = await hrService.getEmployeeStatement(
        formData.employeeId,
        formData.epfAccountId,
        formData.startDate || undefined,
        formData.endDate || undefined
      );
      setStatement(response.data);
    } catch (err: any) {
      console.error('Failed to generate statement:', err);
      setError(err.response?.data?.message || 'Failed to generate statement');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    // Download statement as PDF
    alert('Download functionality will be implemented');
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Provident Fund Statements
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Generate and download EPF account statements
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <TextField
              label="Employee ID"
              value={formData.employeeId}
              onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
              required
            />
            <TextField
              label="EPF Account ID"
              value={formData.epfAccountId}
              onChange={(e) => setFormData({ ...formData, epfAccountId: e.target.value })}
              required
            />
            <TextField
              label="Start Date"
              type="date"
              value={formData.startDate}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="End Date"
              type="date"
              value={formData.endDate}
              onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              onClick={handleGenerate}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Generate Statement'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {statement && (
        <Card>
          <CardContent>
            <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h6">Account Statement</Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  variant="outlined"
                  startIcon={<Download />}
                  onClick={handleDownload}
                >
                  Download PDF
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Print />}
                  onClick={handlePrint}
                >
                  Print
                </Button>
              </Box>
            </Box>

            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary">
                Account Number: {statement.accountNumber}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {statement.period}
              </Typography>
            </Box>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Opening Balance</TableCell>
                    <TableCell>Total Contributions</TableCell>
                    <TableCell>Withdrawals</TableCell>
                    <TableCell>Closing Balance</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>₹{statement.openingBalance?.toLocaleString('en-IN') || '0'}</TableCell>
                    <TableCell>₹{statement.totalContributions?.toLocaleString('en-IN') || '0'}</TableCell>
                    <TableCell>₹{(statement.withdrawals?.reduce((sum: number, w: any) => sum + (w.amount || 0), 0) || 0).toLocaleString('en-IN')}</TableCell>
                    <TableCell>₹{statement.closingBalance?.toLocaleString('en-IN') || '0'}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </TableContainer>

            {statement.contributions && statement.contributions.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Contributions
                </Typography>
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
                      {statement.contributions.map((contrib: any, index: number) => (
                        <TableRow key={index}>
                          <TableCell>
                            {contrib.contributionMonth}/{contrib.contributionYear}
                          </TableCell>
                          <TableCell>₹{contrib.employeeContributionAmount?.toLocaleString('en-IN') || '0'}</TableCell>
                          <TableCell>₹{contrib.employerContributionAmount?.toLocaleString('en-IN') || '0'}</TableCell>
                          <TableCell>₹{contrib.totalContribution?.toLocaleString('en-IN') || '0'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default ProvidentFundStatements;

