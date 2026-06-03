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
import { CashFlowReport, Period } from '../../types/accounting';
import { exportCashFlowToExcel } from '../../utils/excelExport';

const CashFlow: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [report, setReport] = useState<CashFlowReport | null>(null);
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
      const data = await accountingService.getCashFlow(organizationId, periodId);
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load Cash Flow statement');
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
      <Alert severity="info" sx={{ mb: 2 }}>
        Indirect method cash flow derived from posted journal activity and working-capital movements for the selected period.
      </Alert>

      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Cash Flow Statement</Typography>
        {report && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button startIcon={<PrintIcon />} onClick={() => window.print()} variant="outlined">
              Print
            </Button>
            <Button 
              startIcon={<DownloadIcon />} 
              variant="outlined"
              onClick={() => report && exportCashFlowToExcel(report)}
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
              <Typography variant="h6">Cash Flow Statement</Typography>
              <Typography color="text.secondary">
                For the period: {report.startDate} to {report.endDate}
              </Typography>
            </Paper>

            <TableContainer>
              <Table>
                <TableBody>
                  {/* Operating Activities */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>CASH FLOWS FROM OPERATING ACTIVITIES</strong></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ pl: 4 }}>Net Income</TableCell>
                    <TableCell align="right">
                      {report.netIncome.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ pl: 2 }}><em>Adjustments to reconcile net income:</em></TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                  {report.operatingAdjustments.map((item, idx) => (
                    <TableRow key={idx}>
                      <TableCell sx={{ pl: 4 }}>{item.description}</TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  {report.operatingAdjustments.length === 0 && (
                    <TableRow>
                      <TableCell sx={{ pl: 4 }} colSpan={2}>
                        <em>No adjustments</em>
                      </TableCell>
                    </TableRow>
                  )}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Net Cash from Operating Activities</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.netCashFromOperations.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 1 }}></TableCell></TableRow>

                  {/* Investing Activities */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>CASH FLOWS FROM INVESTING ACTIVITIES</strong></TableCell>
                  </TableRow>
                  {report.investingActivities.map((item, idx) => (
                    <TableRow key={idx}>
                      <TableCell sx={{ pl: 4 }}>{item.description}</TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  {report.investingActivities.length === 0 && (
                    <TableRow>
                      <TableCell sx={{ pl: 4 }} colSpan={2}>
                        <em>No investing activities</em>
                      </TableCell>
                    </TableRow>
                  )}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Net Cash from Investing Activities</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.netCashFromInvesting.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 1 }}></TableCell></TableRow>

                  {/* Financing Activities */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>CASH FLOWS FROM FINANCING ACTIVITIES</strong></TableCell>
                  </TableRow>
                  {report.financingActivities.map((item, idx) => (
                    <TableRow key={idx}>
                      <TableCell sx={{ pl: 4 }}>{item.description}</TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  {report.financingActivities.length === 0 && (
                    <TableRow>
                      <TableCell sx={{ pl: 4 }} colSpan={2}>
                        <em>No financing activities</em>
                      </TableCell>
                    </TableRow>
                  )}
                  <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                    <TableCell><strong>Net Cash from Financing Activities</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.netCashFromFinancing.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2}><Divider sx={{ my: 1 }} /></TableCell></TableRow>

                  {/* Summary */}
                  <TableRow sx={{ backgroundColor: '#fff3e0' }}>
                    <TableCell><strong>NET INCREASE (DECREASE) IN CASH</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.netCashFlow.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell sx={{ pl: 4 }}>Cash at Beginning of Period</TableCell>
                    <TableCell align="right">
                      {report.cashAtBeginning.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                  </TableRow>
                  <TableRow sx={{ backgroundColor: '#c8e6c9' }}>
                    <TableCell><strong>CASH AT END OF PERIOD</strong></TableCell>
                    <TableCell align="right"><strong style={{ fontSize: '1.1rem' }}>
                      {report.cashAtEnd.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
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

export default CashFlow;

