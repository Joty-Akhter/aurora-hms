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
  TextField,
  MenuItem,
  Button,
  Alert,
  Grid,
  Paper,
  Divider,
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { ProfitLossReport, Period } from '../../types/accounting';
import { exportProfitLossToExcel } from '../../utils/excelExport';

const ProfitLoss: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [report, setReport] = useState<ProfitLossReport | null>(null);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadPeriods();
    }
  }, [organizationId]);

  const loadPeriods = async () => {
    try {
      const data = await accountingService.getOpenPeriods(organizationId);
      setPeriods(data);
      if (data.length > 0) {
        setSelectedPeriod(data[0].id);
        loadReport(data[0].id);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load periods');
    }
  };

  const loadReport = async (periodId: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getProfitAndLoss(organizationId, periodId);
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load Profit & Loss statement');
    } finally {
      setLoading(false);
    }
  };

  const handlePeriodChange = (periodId: string) => {
    setSelectedPeriod(periodId);
    loadReport(periodId);
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Profit & Loss Statement</Typography>
        {report && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button startIcon={<PrintIcon />} onClick={() => window.print()} variant="outlined">
              Print
            </Button>
            <Button 
              startIcon={<DownloadIcon />} 
              variant="outlined"
              onClick={() => report && exportProfitLossToExcel(report)}
              disabled={!report}
            >
              Export to Excel
            </Button>
          </Box>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Period Selection */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                select
                fullWidth
                label="Period"
                value={selectedPeriod}
                onChange={(e) => handlePeriodChange(e.target.value)}
              >
                {periods.map((period) => (
                  <MenuItem key={period.id} value={period.id}>
                    {period.periodName} ({period.startDate} to {period.endDate})
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Report */}
      {loading ? (
        <Card>
          <CardContent>
            <Typography align="center" sx={{ py: 4 }}>
              Loading...
            </Typography>
          </CardContent>
        </Card>
      ) : !report ? (
        <Card>
          <CardContent>
            <Typography align="center" sx={{ py: 4 }} color="text.secondary">
              No data available. Please select a period.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent>
            <Paper sx={{ mb: 3, p: 2, textAlign: 'center' }}>
              <Typography variant="h5">{report.organizationName}</Typography>
              <Typography variant="h6">Profit & Loss Statement</Typography>
              <Typography color="text.secondary">
                For the period: {report.startDate} to {report.endDate}
              </Typography>
            </Paper>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Account</strong></TableCell>
                    <TableCell align="right"><strong>Amount</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {/* Revenue Section */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>REVENUE</strong></TableCell>
                  </TableRow>
                  {report.revenueAccounts.map((item) => (
                    <TableRow key={item.accountCode}>
                      <TableCell sx={{ pl: 4 }}>
                        {item.accountCode} - {item.accountName}
                      </TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Total Revenue</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2}></TableCell></TableRow>

                  {/* COGS Section */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>COST OF GOODS SOLD</strong></TableCell>
                  </TableRow>
                  {report.cogsAccounts.map((item) => (
                    <TableRow key={item.accountCode}>
                      <TableCell sx={{ pl: 4 }}>
                        {item.accountCode} - {item.accountName}
                      </TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Total COGS</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.totalCOGS.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  {/* Gross Profit */}
                  <TableRow sx={{ backgroundColor: '#fff3e0' }}>
                    <TableCell><strong>GROSS PROFIT</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.grossProfit.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ pl: 4 }}><em>Gross Profit Margin</em></TableCell>
                    <TableCell align="right"><em>
                      {report.grossProfitMargin.toFixed(2)}%
                    </em></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2}></TableCell></TableRow>

                  {/* Operating Expenses */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>OPERATING EXPENSES</strong></TableCell>
                  </TableRow>
                  {report.operatingExpenses.map((item) => (
                    <TableRow key={item.accountCode}>
                      <TableCell sx={{ pl: 4 }}>
                        {item.accountCode} - {item.accountName}
                      </TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Total Operating Expenses</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.totalOperatingExpenses.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2}><Divider sx={{ my: 1 }} /></TableCell></TableRow>

                  {/* Net Income */}
                  <TableRow sx={{ backgroundColor: report.netIncome >= 0 ? '#c8e6c9' : '#ffcdd2' }}>
                    <TableCell><strong>NET INCOME</strong></TableCell>
                    <TableCell align="right"><strong style={{ fontSize: '1.1rem' }}>
                      {report.netIncome.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ pl: 4 }}><em>Net Profit Margin</em></TableCell>
                    <TableCell align="right"><em>
                      {report.netProfitMargin.toFixed(2)}%
                    </em></TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default ProfitLoss;

