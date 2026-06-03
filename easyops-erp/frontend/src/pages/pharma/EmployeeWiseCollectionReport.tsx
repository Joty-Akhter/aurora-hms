import React, { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper,
  TextField, MenuItem, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, CircularProgress, Alert, Chip
} from '@mui/material';
import { Print as PrintIcon, GetApp as DownloadIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { EmployeeWiseCollectionReport } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';

const EmployeeWiseCollectionReportPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [report, setReport] = useState<EmployeeWiseCollectionReport | null>(null);
  
  const [filters, setFilters] = useState({
    employeeId: '',
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadEmployees();
    }
  }, [currentOrganizationId]);

  const loadEmployees = async () => {
    if (!currentOrganizationId) return;
    try {
      const res = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
      setEmployees(res.data || []);
    } catch (error) {
      console.error('Failed to load employees:', error);
    }
  };

  const handleGenerate = async () => {
    if (!currentOrganizationId) {
      setError('Organization ID is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await pharmaService.getEmployeeWiseCollectionReport(
        currentOrganizationId,
        filters.startDate,
        filters.endDate,
        filters.employeeId || undefined
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

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  };

  const handleExportPDF = () => {
    if (!report) return;
    window.print();
  };

  const handleExportExcel = () => {
    if (!report) return;
    // Create CSV content
    const headers = ['Employee ID', 'Employee Name', 'Role', 'Number of Deposits', 'Total Collection', 'Assigned Areas'];
    const rows = report.employees.map(emp => [
      emp.employeeIdCode || emp.employeeId,
      emp.employeeName || emp.employeeId,
      emp.role || 'N/A',
      emp.numberOfDeposits.toString(),
      emp.totalCollectionAmount.toFixed(2),
      emp.assignedAreaNames?.join(', ') || 'N/A'
    ]);
    
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `employee-wise-collection-report-${filters.startDate}-to-${filters.endDate}.csv`;
    link.click();
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Employee-Wise Collection Report
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                select
                label="Employee (Optional)"
                value={filters.employeeId}
                onChange={(e) => setFilters({ ...filters, employeeId: e.target.value })}
              >
                <MenuItem value="">All Employees</MenuItem>
                {employees.map((emp) => {
                  const employeeIdValue = emp.employeeId || emp.id || '';
                  return (
                    <MenuItem key={employeeIdValue} value={employeeIdValue}>
                      {emp.name || emp.employeeNumber || employeeIdValue}
                      {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                    </MenuItem>
                  );
                })}
              </TextField>
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="date"
                label="Start Date"
                value={filters.startDate}
                onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="date"
                label="End Date"
                value={filters.endDate}
                onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
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
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Employee-Wise Collection Report
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Period: {formatDate(report.startDate)} to {formatDate(report.endDate)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                variant="outlined"
                startIcon={<PrintIcon />}
                onClick={handleExportPDF}
              >
                Print/PDF
              </Button>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={handleExportExcel}
              >
                Export Excel
              </Button>
            </Box>
          </Box>

          <Box sx={{ mb: 3, p: 2, bgcolor: 'primary.light', borderRadius: 1 }}>
            <Typography variant="h6" color="primary.contrastText">
              Grand Total Collection: {formatCurrency(report.grandTotalCollection)}
            </Typography>
            <Typography variant="body2" color="primary.contrastText">
              Total Employees: {report.employees.length}
            </Typography>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Employee ID</TableCell>
                  <TableCell>Employee Name</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell align="center">Deposits</TableCell>
                  <TableCell align="right">Total Collection</TableCell>
                  <TableCell>Assigned Areas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {report.employees.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      No data found for the selected period
                    </TableCell>
                  </TableRow>
                ) : (
                  report.employees.map((emp, index) => (
                    <TableRow key={emp.employeeId} hover>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {emp.employeeIdCode || emp.employeeId}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {emp.employeeName || emp.employeeId}
                      </TableCell>
                      <TableCell>
                        {emp.role ? (
                          <Chip label={emp.role} size="small" />
                        ) : (
                          '-'
                        )}
                      </TableCell>
                      <TableCell align="center">
                        {emp.numberOfDeposits}
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" fontWeight="bold" color="success.main">
                          {formatCurrency(emp.totalCollectionAmount)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {emp.assignedAreaNames && emp.assignedAreaNames.length > 0 ? (
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {emp.assignedAreaNames.map((area, idx) => (
                              <Chip key={idx} label={area} size="small" variant="outlined" />
                            ))}
                          </Box>
                        ) : (
                          '-'
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Box>
  );
};

export default EmployeeWiseCollectionReportPage;
