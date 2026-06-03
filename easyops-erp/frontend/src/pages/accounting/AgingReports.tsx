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
  Tabs,
  Tab,
  Alert,
  Button,
  CircularProgress,
  Grid,
  TextField,
} from '@mui/material';
import { Download as DownloadIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { formatCurrency } from '../../utils/currencyFormatter';
import {
  AGING_BUCKET_FIELDS,
  AgingReportRow,
  getDocumentLabel,
  getPartyLabel,
  getRowKey,
  normalizeAgingRows,
  sumBalanceDue,
  sumBucket,
  toNumber,
} from '../../utils/agingReportUtils';

const AGING_BUCKETS = [
  { label: 'Current', field: 'current' as const },
  { label: '1-30 Days', field: 'days1To30' as const },
  { label: '31-60 Days', field: 'days31To60' as const },
  { label: '61-90 Days', field: 'days61To90' as const },
  { label: 'Over 90 Days', field: 'days90Plus' as const },
];

const AgingReports: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().split('T')[0]);
  const [agingData, setAgingData] = useState<AgingReportRow[]>([]);

  const isAR = tabValue === 0;

  useEffect(() => {
    if (currentOrganizationId) {
      loadAgingReport();
    }
  }, [currentOrganizationId, tabValue, asOfDate]);

  const loadAgingReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = isAR
        ? await accountingService.getARAgingReport(currentOrganizationId || '', asOfDate)
        : await accountingService.getAPAgingReport(currentOrganizationId || '', asOfDate);
      setAgingData(normalizeAgingRows(data));
    } catch (err: any) {
      setError(err.message || 'Failed to load aging report');
    } finally {
      setLoading(false);
    }
  };

  const bucketAmount = (row: AgingReportRow, field: (typeof AGING_BUCKET_FIELDS)[number]) =>
    toNumber(row[field]);

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Aging Reports</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="As of Date"
            type="date"
            value={asOfDate}
            onChange={(e) => setAsOfDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            size="small"
          />
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadAgingReport}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button variant="outlined" startIcon={<DownloadIcon />}>
            Export
          </Button>
        </Box>
      </Box>

      {!currentOrganizationId && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          No organization selected. Please select an organization or contact your administrator.
        </Alert>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Tabs value={tabValue} onChange={(_e, v) => setTabValue(v)} sx={{ mb: 2 }}>
        <Tab label="Accounts Receivable Aging" />
        <Tab label="Accounts Payable Aging" />
      </Tabs>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            {isAR ? 'AR Aging Detail' : 'AP Aging Detail'}
          </Typography>

          <Grid container spacing={2} sx={{ mb: 3 }}>
            {AGING_BUCKETS.map((bucket) => {
              const bucketTotal = sumBucket(agingData, bucket.field);
              const itemCount = agingData.filter((item) => bucketAmount(item, bucket.field) > 0).length;

              return (
                <Grid item xs={12} sm={6} md={2.4} key={bucket.label}>
                  <Card variant="outlined">
                    <CardContent sx={{ textAlign: 'center' }}>
                      <Typography variant="caption" color="textSecondary">
                        {bucket.label}
                      </Typography>
                      <Typography variant="h6">{formatCurrency(bucketTotal)}</Typography>
                      <Typography variant="caption">{itemCount} documents</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>

          <Box sx={{ mb: 2, p: 2, bgcolor: 'primary.50', borderRadius: 1 }}>
            <Typography variant="h6">
              Total Outstanding: {formatCurrency(sumBalanceDue(agingData))}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {agingData.length} {isAR ? 'invoices' : 'bills'} with outstanding balances
            </Typography>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>{isAR ? 'Customer' : 'Vendor'}</TableCell>
                  <TableCell>{isAR ? 'Invoice #' : 'Bill #'}</TableCell>
                  <TableCell>Due Date</TableCell>
                  <TableCell align="right">Days Overdue</TableCell>
                  <TableCell align="right">Current</TableCell>
                  <TableCell align="right">1-30 Days</TableCell>
                  <TableCell align="right">31-60 Days</TableCell>
                  <TableCell align="right">61-90 Days</TableCell>
                  <TableCell align="right">Over 90 Days</TableCell>
                  <TableCell align="right">Balance Due</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">
                      <CircularProgress size={24} />
                      <Typography variant="body2" sx={{ mt: 1 }}>
                        Loading aging report...
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : agingData.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">
                      <Typography variant="body2" color="textSecondary">
                        No {isAR ? 'receivables' : 'payables'} data available
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  agingData.map((item, index) => (
                    <TableRow key={getRowKey(item, index)}>
                      <TableCell>{getPartyLabel(item, isAR)}</TableCell>
                      <TableCell>{getDocumentLabel(item, isAR)}</TableCell>
                      <TableCell>{item.dueDate || '—'}</TableCell>
                      <TableCell align="right">{item.daysOverdue ?? 0}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'current'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days1To30'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days31To60'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days61To90'))}</TableCell>
                      <TableCell align="right">{formatCurrency(bucketAmount(item, 'days90Plus'))}</TableCell>
                      <TableCell align="right">
                        <Typography fontWeight="bold">
                          {formatCurrency(item.balanceDue)}
                        </Typography>
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

export default AgingReports;
