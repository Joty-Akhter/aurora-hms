import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { CalendarMonth as CalendarIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useAccountingManage } from '../../hooks/useAccountingManage';
import accountingService from '../../services/accountingService';
import { FiscalYear, Period } from '../../types/accounting';

const FiscalYearSetup: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const canManage = useAccountingManage();
  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const organizationId = currentOrganizationId || '';

  const loadData = async () => {
    if (!organizationId) return;
    setLoading(true);
    setError(null);
    try {
      const [fy, per] = await Promise.all([
        accountingService.getFiscalYears(organizationId),
        accountingService.getPeriods(organizationId),
      ]);
      setFiscalYears(fy);
      setPeriods(per);
    } catch (err: any) {
      setError(err.message || 'Failed to load fiscal data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [organizationId]);

  const handleSetupCurrentYear = async () => {
    if (!canManage || !organizationId) return;
    setError(null);
    setSuccess(null);
    try {
      await accountingService.setupCurrentFiscalYear(organizationId);
      setSuccess('Current fiscal year and monthly periods created.');
      loadData();
    } catch (err: any) {
      setError(err.message || 'Failed to set up fiscal year');
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={1}>
          <CalendarIcon fontSize="large" />
          <Typography variant="h4">Fiscal Year & Periods</Typography>
        </Box>
        {canManage && (
          <Button variant="contained" onClick={handleSetupCurrentYear} disabled={loading}>
            Set Up Current Fiscal Year
          </Button>
        )}
      </Box>

      {!canManage && (
        <Alert severity="info" sx={{ mb: 2 }}>
          View-only access. An accounting manager can create fiscal years and periods.
        </Alert>
      )}

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Fiscal Years</Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Start</TableCell>
                  <TableCell>End</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {fiscalYears.length === 0 ? (
                  <TableRow><TableCell colSpan={4}>No fiscal years. Run setup to create the current year.</TableCell></TableRow>
                ) : (
                  fiscalYears.map((fy) => (
                    <TableRow key={fy.id}>
                      <TableCell>{fy.yearCode}</TableCell>
                      <TableCell>{fy.startDate}</TableCell>
                      <TableCell>{fy.endDate}</TableCell>
                      <TableCell>
                        <Chip label={fy.isClosed ? 'CLOSED' : 'OPEN'} size="small" />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>Accounting Periods</Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Period</TableCell>
                  <TableCell>Start</TableCell>
                  <TableCell>End</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {periods.length === 0 ? (
                  <TableRow><TableCell colSpan={4}>No periods defined.</TableCell></TableRow>
                ) : (
                  periods.map((p) => (
                    <TableRow key={p.id}>
                      <TableCell>{p.periodName}</TableCell>
                      <TableCell>{p.startDate}</TableCell>
                      <TableCell>{p.endDate}</TableCell>
                      <TableCell>
                        <Chip
                          label={p.status}
                          size="small"
                          color={p.status === 'OPEN' ? 'success' : 'default'}
                        />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default FiscalYearSetup;
