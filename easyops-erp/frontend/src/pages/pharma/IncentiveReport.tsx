import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Chip, Accordion,
  AccordionSummary, AccordionDetails, Collapse, IconButton
} from '@mui/material';
import { Print as PrintIcon, Download as DownloadIcon, ExpandMore as ExpandMoreIcon, ChevronRight as ChevronRightIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { getEmployees, Employee } from '../../services/hrService';
import pharmaService, { IncentiveReport } from '../../services/pharmaService';

const IncentiveReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [report, setReport] = useState<IncentiveReport | null>(null);
  const [expandedArea, setExpandedArea] = useState<string | null>(null);
  
  const [filters, setFilters] = useState({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
  });

  useEffect(() => {
    if (currentOrganizationId) {
      getEmployees(currentOrganizationId, { status: 'ACTIVE' })
        .then((res) => setEmployees(res.data || []))
        .catch(() => setEmployees([]));
    }
  }, [currentOrganizationId]);

  const getEmployeeName = (employeeId: string | undefined): string => {
    if (!employeeId) return 'N/A';
    const employee = employees.find(e => (e.id === employeeId || e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || 'N/A') : 'N/A';
  };

  const handleGenerate = async () => {
    if (!currentOrganizationId) {
      setError('Organization ID is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getIncentiveReport(
        currentOrganizationId,
        filters.year,
        filters.month
      );
      setReport(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-BD', {
      style: 'currency',
      currency: 'BDT',
      minimumFractionDigits: 2
    }).format(amount);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Incentive Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                type="number"
                label="Year"
                value={filters.year}
                onChange={(e) => setFilters({ ...filters, year: parseInt(e.target.value) })}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Month"
                value={filters.month}
                onChange={(e) => setFilters({ ...filters, month: parseInt(e.target.value) })}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => (
                  <MenuItem key={m} value={m}>
                    {new Date(2000, m - 1).toLocaleString('default', { month: 'long' })}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={4}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleGenerate}
                disabled={loading}
                sx={{ height: '56px' }}
              >
                {loading ? <CircularProgress size={24} /> : 'Generate Report'}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {report && (
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                INCENTIVE REPORT
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {new Date(report.year, report.month - 1).toLocaleString('default', { month: 'long', year: 'numeric' })}
              </Typography>
              <Typography variant="body2">
                Eligible Areas: {report.eligibleAreas} / {report.totalAreas}
              </Typography>
            </Box>
            <Box>
              <Button startIcon={<PrintIcon />} onClick={() => window.print()} sx={{ mr: 1 }}>
                Print
              </Button>
              <Button startIcon={<DownloadIcon />} variant="outlined">
                Download
              </Button>
            </Box>
          </Box>

          <Box sx={{ mb: 4, textAlign: 'center', py: 2, bgcolor: 'primary.light', borderRadius: 2 }}>
            <Typography variant="h6" color="primary.contrastText">
              Total Incentive Amount
            </Typography>
            <Typography variant="h4" color="primary.contrastText" sx={{ fontWeight: 'bold' }}>
              {formatCurrency(report.totalIncentiveAmount)}
            </Typography>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell width="50px"></TableCell>
                  <TableCell>Area Name</TableCell>
                  <TableCell align="right">Target</TableCell>
                  <TableCell align="right">Covered</TableCell>
                  <TableCell align="right">Coverage %</TableCell>
                  <TableCell align="center">Status</TableCell>
                  <TableCell align="right">Incentive Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.areaIncentives.map((area, index) => (
                  <React.Fragment key={index}>
                    <TableRow>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => setExpandedArea(expandedArea === area.areaId ? null : area.areaId)}
                        >
                          {expandedArea === area.areaId ? <ExpandMoreIcon /> : <ChevronRightIcon />}
                        </IconButton>
                      </TableCell>
                      <TableCell>{area.areaName}</TableCell>
                      <TableCell align="right">{formatCurrency(area.targetAmount)}</TableCell>
                      <TableCell align="right">{formatCurrency(area.coveredAmount)}</TableCell>
                      <TableCell align="right">
                        <Chip
                          label={`${area.targetCoveragePercentage?.toFixed(2) || 0}%`}
                          color={area.targetCoveragePercentage && area.targetCoveragePercentage >= 100 ? 'success' : 'warning'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                          <Chip
                            label={area.targetAchieved ? 'Target Met' : 'Not Met'}
                            color={area.targetAchieved ? 'success' : 'default'}
                            size="small"
                          />
                          <Chip
                            label={area.expenseWithinLimit ? 'Expense OK' : 'Expense Over'}
                            color={area.expenseWithinLimit ? 'success' : 'error'}
                            size="small"
                          />
                          <Chip
                            label={area.eligible ? 'Eligible' : 'Not Eligible'}
                            color={area.eligible ? 'primary' : 'default'}
                            size="small"
                          />
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" fontWeight="bold">
                          {formatCurrency(area.incentiveBaseAmount)}
                        </Typography>
                      </TableCell>
                    </TableRow>
                    {area.employeeIncentives && area.employeeIncentives.length > 0 && (
                      <TableRow>
                        <TableCell colSpan={7} sx={{ py: 0, border: 0 }}>
                          <Collapse in={expandedArea === area.areaId} timeout="auto" unmountOnExit>
                            <Box sx={{ margin: 2 }}>
                              <Typography variant="subtitle2" gutterBottom>
                                Employee Incentive Distribution
                              </Typography>
                              <Table size="small">
                                <TableHead>
                                  <TableRow>
                                    <TableCell>Employee ID</TableCell>
                                    <TableCell>Name</TableCell>
                                    <TableCell>Role</TableCell>
                                    <TableCell align="right">Incentive Amount</TableCell>
                                  </TableRow>
                                </TableHead>
                                <TableBody>
                                  {area.employeeIncentives.map((emp, empIndex) => (
                                    <TableRow key={empIndex}>
                                      <TableCell>{emp.employeeIdCode || emp.employeeId}</TableCell>
                                      <TableCell>{emp.employeeName || getEmployeeName(emp.employeeId)}</TableCell>
                                      <TableCell>{emp.role}</TableCell>
                                      <TableCell align="right">{formatCurrency(emp.incentiveAmount)}</TableCell>
                                    </TableRow>
                                  ))}
                                </TableBody>
                              </Table>
                            </Box>
                          </Collapse>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Box>
  );
};

export default IncentiveReportPage;

