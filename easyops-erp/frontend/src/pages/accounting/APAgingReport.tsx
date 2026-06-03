import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Grid,
} from '@mui/material';
import { Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { exportAPAgingToExcel } from '../../utils/excelExport';
import {
  AgingReportRow,
  getDocumentLabel,
  getPartyLabel,
  getRowKey,
  normalizeAgingRows,
  sumBalanceDue,
  sumBucket,
  toNumber,
} from '../../utils/agingReportUtils';

const APAgingReport: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [agingData, setAgingData] = useState<AgingReportRow[]>([]);
  const [asOfDate, setAsOfDate] = useState<string>(new Date().toISOString().split('T')[0]);

  const organizationId = currentOrganizationId || '';

  const loadAgingReport = async () => {
    if (!organizationId) return;

    setLoading(true);
    setError(null);

    try {
      const data = await accountingService.getAPAgingReport(organizationId, asOfDate);
      setAgingData(normalizeAgingRows(data));
    } catch (err: any) {
      setError(err.message || 'Failed to load aging report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (organizationId) {
      loadAgingReport();
    }
  }, [organizationId, asOfDate]);

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

  const bucketAmount = (row: AgingReportRow, field: keyof Pick<AgingReportRow, 'current' | 'days1To30' | 'days31To60' | 'days61To90' | 'days90Plus'>) =>
    toNumber(row[field]);

  if (!organizationId) {
    return (
      <Box p={3}>
        <Alert severity="warning">Please select an organization to view aging reports.</Alert>
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">AP Aging Report</Typography>
        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
          onClick={() => {
            exportAPAgingToExcel(
              agingData.map((item) => ({
                vendorName: item.vendorName,
                billNumber: item.billNumber,
                dueDate: item.dueDate,
                daysOverdue: item.daysOverdue,
                current: item.current,
                days1to30: item.days1To30,
                days31to60: item.days31To60,
                days61to90: item.days61To90,
                over90: item.days90Plus,
                total: item.balanceDue,
              }))
            );
          }}
          disabled={agingData.length === 0}
        >
          Export to Excel
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                label="As of Date"
                type="date"
                value={asOfDate}
                onChange={(e) => setAsOfDate(e.target.value)}
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <Button variant="contained" onClick={loadAgingReport} disabled={loading} fullWidth>
                {loading ? <CircularProgress size={24} /> : 'Refresh'}
              </Button>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="h6" color="primary">
                Total Outstanding: {formatCurrency(sumBalanceDue(agingData))}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      ) : (
        <Card>
          <CardContent>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Vendor</strong></TableCell>
                    <TableCell><strong>Bill #</strong></TableCell>
                    <TableCell><strong>Due Date</strong></TableCell>
                    <TableCell align="right"><strong>Days Overdue</strong></TableCell>
                    <TableCell align="right"><strong>Current</strong></TableCell>
                    <TableCell align="right"><strong>1-30 Days</strong></TableCell>
                    <TableCell align="right"><strong>31-60 Days</strong></TableCell>
                    <TableCell align="right"><strong>61-90 Days</strong></TableCell>
                    <TableCell align="right"><strong>Over 90 Days</strong></TableCell>
                    <TableCell align="right"><strong>Balance Due</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {agingData.map((item, index) => (
                    <TableRow key={getRowKey(item, index)}>
                      <TableCell>{getPartyLabel(item, false)}</TableCell>
                      <TableCell>{getDocumentLabel(item, false)}</TableCell>
                      <TableCell>{item.dueDate || '—'}</TableCell>
                      <TableCell align="right">{item.daysOverdue ?? 0}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'current'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days1To30'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days31To60'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days61To90'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days90Plus'))}</TableCell>
                      <TableCell align="right">
                        <strong>{formatCurrency(item.balanceDue ?? 0)}</strong>
                      </TableCell>
                    </TableRow>
                  ))}
                  {agingData.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No aging data available for the selected date.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
                {agingData.length > 0 && (
                  <TableHead>
                    <TableRow>
                      <TableCell colSpan={4}><strong>TOTALS</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBucket(agingData, 'current'))}</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBucket(agingData, 'days1To30'))}</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBucket(agingData, 'days31To60'))}</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBucket(agingData, 'days61To90'))}</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBucket(agingData, 'days90Plus'))}</strong></TableCell>
                      <TableCell align="right"><strong>{formatCurrency(sumBalanceDue(agingData))}</strong></TableCell>
                    </TableRow>
                  </TableHead>
                )}
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default APAgingReport;
