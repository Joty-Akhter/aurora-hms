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
  Button,
  Alert,
  Grid,
  Paper,
  Divider,
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { BalanceSheetReport } from '../../types/accounting';
import { exportBalanceSheetToExcel } from '../../utils/excelExport';

const BalanceSheet: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [report, setReport] = useState<BalanceSheetReport | null>(null);
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadReport();
    }
  }, [organizationId]);

  const loadReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getBalanceSheet(organizationId, asOfDate);
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load Balance Sheet');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Balance Sheet</Typography>
        {report && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button startIcon={<PrintIcon />} onClick={() => window.print()} variant="outlined">
              Print
            </Button>
            <Button 
              startIcon={<DownloadIcon />} 
              variant="outlined"
              onClick={() => report && exportBalanceSheetToExcel(report)}
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

      {/* Date Selection */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                type="date"
                fullWidth
                label="As of Date"
                value={asOfDate}
                onChange={(e) => setAsOfDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="contained"
                onClick={loadReport}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                Generate
              </Button>
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
              Select a date and click Generate to view the Balance Sheet.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent>
            <Paper sx={{ mb: 3, p: 2, textAlign: 'center' }}>
              <Typography variant="h5">{report.organizationName}</Typography>
              <Typography variant="h6">Balance Sheet</Typography>
              <Typography color="text.secondary">As of {report.asOfDate}</Typography>
            </Paper>

            {!report.balanced && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                Balance Sheet is not balanced! Assets: {report.totalAssets.toLocaleString()} vs 
                Liabilities + Equity: {report.totalLiabilitiesAndEquity.toLocaleString()}
              </Alert>
            )}

            <TableContainer>
              <Table>
                <TableBody>
                  {/* ASSETS */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>ASSETS</strong></TableCell>
                  </TableRow>
                  
                  <TableRow>
                    <TableCell sx={{ pl: 2 }}><strong>Current Assets</strong></TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                  {report.currentAssets.map((item) => (
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
                    <TableCell sx={{ pl: 2 }}>Total Current Assets</TableCell>
                    <TableCell align="right"><strong>
                      {report.totalCurrentAssets.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 1 }}></TableCell></TableRow>

                  <TableRow>
                    <TableCell sx={{ pl: 2 }}><strong>Fixed Assets</strong></TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                  {report.fixedAssets.map((item) => (
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
                    <TableCell sx={{ pl: 2 }}>Total Fixed Assets</TableCell>
                    <TableCell align="right"><strong>
                      {report.totalFixedAssets.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow sx={{ backgroundColor: '#c8e6c9' }}>
                    <TableCell><strong>TOTAL ASSETS</strong></TableCell>
                    <TableCell align="right"><strong style={{ fontSize: '1.1rem' }}>
                      {report.totalAssets.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 2 }}><Divider /></TableCell></TableRow>

                  {/* LIABILITIES */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>LIABILITIES</strong></TableCell>
                  </TableRow>

                  <TableRow>
                    <TableCell sx={{ pl: 2 }}><strong>Current Liabilities</strong></TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                  {report.currentLiabilities.map((item) => (
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
                    <TableCell sx={{ pl: 2 }}>Total Current Liabilities</TableCell>
                    <TableCell align="right"><strong>
                      {report.totalCurrentLiabilities.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 1 }}></TableCell></TableRow>

                  <TableRow>
                    <TableCell sx={{ pl: 2 }}><strong>Long-term Liabilities</strong></TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                  {report.longTermLiabilities.map((item) => (
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
                    <TableCell sx={{ pl: 2 }}>Total Long-term Liabilities</TableCell>
                    <TableCell align="right"><strong>
                      {report.totalLongTermLiabilities.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow sx={{ backgroundColor: '#ffe0b2' }}>
                    <TableCell><strong>Total Liabilities</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.totalLiabilities.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2} sx={{ py: 1 }}></TableCell></TableRow>

                  {/* EQUITY */}
                  <TableRow sx={{ backgroundColor: '#e3f2fd' }}>
                    <TableCell colSpan={2}><strong>EQUITY</strong></TableCell>
                  </TableRow>
                  {report.equityAccounts.map((item) => (
                    <TableRow key={item.accountCode}>
                      <TableCell sx={{ pl: 4 }}>
                        {item.accountCode} - {item.accountName}
                      </TableCell>
                      <TableCell align="right">
                        {item.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow sx={{ backgroundColor: '#ffe0b2' }}>
                    <TableCell><strong>Total Equity</strong></TableCell>
                    <TableCell align="right"><strong>
                      {report.totalEquity.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </strong></TableCell>
                  </TableRow>

                  <TableRow><TableCell colSpan={2}><Divider sx={{ my: 1 }} /></TableCell></TableRow>

                  {/* TOTAL */}
                  <TableRow sx={{ backgroundColor: report.balanced ? '#c8e6c9' : '#ffcdd2' }}>
                    <TableCell><strong>TOTAL LIABILITIES & EQUITY</strong></TableCell>
                    <TableCell align="right"><strong style={{ fontSize: '1.1rem' }}>
                      {report.totalLiabilitiesAndEquity.toLocaleString(undefined, { minimumFractionDigits: 2 })}
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

export default BalanceSheet;

