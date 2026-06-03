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
  Grid,
} from '@mui/material';
import { Add, Edit, Delete } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

interface Nomination {
  nominationId: string;
  epfAccountId: string;
  employeeId: string;
  nomineeName: string;
  relationship: string;
  dateOfBirth?: string;
  sharePercentage: number;
  isPrimary: boolean;
  status: string;
}

const ProvidentFundNominations: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [nominations, setNominations] = useState<Nomination[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [formData, setFormData] = useState({
    epfAccountId: '',
    nomineeName: '',
    relationship: '',
    dateOfBirth: '',
    sharePercentage: '',
    isPrimary: false,
  });

  useEffect(() => {
    if (currentOrganizationId && selectedAccountId) {
      loadNominations();
    }
  }, [currentOrganizationId, selectedAccountId]);

  const loadNominations = async () => {
    if (!selectedAccountId) return;
    try {
      setLoading(true);
      // Load nominations for selected account
      setNominations([]);
    } catch (err: any) {
      console.error('Failed to load nominations:', err);
      setError(err.response?.data?.message || 'Failed to load nominations');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      // Create nomination logic
      setOpenDialog(false);
      loadNominations();
    } catch (err: any) {
      console.error('Failed to create nomination:', err);
      setError(err.response?.data?.message || 'Failed to create nomination');
    }
  };

  const totalSharePercentage = nominations.reduce((sum, nom) => sum + nom.sharePercentage, 0);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Provident Fund Nominations
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Manage nominations for EPF accounts
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            label="Select EPF Account ID"
            value={selectedAccountId}
            onChange={(e) => setSelectedAccountId(e.target.value)}
            placeholder="Enter EPF Account ID to view nominations"
          />
        </CardContent>
      </Card>

      {selectedAccountId && (
        <Card>
          <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Nominations for Account: {selectedAccountId}</Typography>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => {
                setFormData({ ...formData, epfAccountId: selectedAccountId });
                setOpenDialog(true);
              }}
            >
              Add Nomination
            </Button>
          </Box>
          <CardContent>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Nominee Name</TableCell>
                    <TableCell>Relationship</TableCell>
                    <TableCell>Date of Birth</TableCell>
                    <TableCell>Share %</TableCell>
                    <TableCell>Primary</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {nominations.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No nominations found for this account
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    <>
                      {nominations.map((nomination) => (
                        <TableRow key={nomination.nominationId}>
                          <TableCell>{nomination.nomineeName}</TableCell>
                          <TableCell>{nomination.relationship}</TableCell>
                          <TableCell>
                            {nomination.dateOfBirth
                              ? new Date(nomination.dateOfBirth).toLocaleDateString()
                              : '-'}
                          </TableCell>
                          <TableCell>{nomination.sharePercentage}%</TableCell>
                          <TableCell>
                            <Chip
                              label={nomination.isPrimary ? 'Yes' : 'No'}
                              color={nomination.isPrimary ? 'primary' : 'default'}
                              size="small"
                            />
                          </TableCell>
                          <TableCell>
                            <Button size="small" startIcon={<Edit />}>
                              Edit
                            </Button>
                            <Button size="small" color="error" startIcon={<Delete />}>
                              Delete
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                      <TableRow>
                        <TableCell colSpan={3}>
                          <Typography variant="body2" fontWeight="bold">
                            Total Share Percentage:
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography
                            variant="body2"
                            color={totalSharePercentage === 100 ? 'success.main' : 'error.main'}
                            fontWeight="bold"
                          >
                            {totalSharePercentage}%
                          </Typography>
                        </TableCell>
                        <TableCell colSpan={2} />
                      </TableRow>
                    </>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Nomination</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nominee Name"
                value={formData.nomineeName}
                onChange={(e) => setFormData({ ...formData, nomineeName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Relationship"
                value={formData.relationship}
                onChange={(e) => setFormData({ ...formData, relationship: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Date of Birth"
                type="date"
                value={formData.dateOfBirth}
                onChange={(e) => setFormData({ ...formData, dateOfBirth: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Share Percentage"
                type="number"
                value={formData.sharePercentage}
                onChange={(e) => setFormData({ ...formData, sharePercentage: e.target.value })}
                required
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Add</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ProvidentFundNominations;

