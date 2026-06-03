import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Typography,
  CircularProgress,
  Alert,
  Grid,
  Button,
  TextField,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  Dashboard,
  Group,
  Person,
  Security,
  TrendingUp,
  Assessment,
  FileDownload,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const ProvidentFundReporting: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reportData, setReportData] = useState<any>(null);
  const [filters, setFilters] = useState({
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    startDate: '',
    endDate: '',
    managerId: '',
    departmentId: '',
    employeeId: '',
    epfAccountId: '',
  });

  const loadReport = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      let response;
      
      if (tabValue === 0) {
        response = await hrService.getExecutiveDashboard(currentOrganizationId);
      } else if (tabValue === 1) {
        if (!filters.managerId || !filters.departmentId) {
          setError('Please provide Manager ID and Department ID');
          return;
        }
        response = await hrService.getManagerTeamReport(
          filters.managerId,
          filters.departmentId,
          currentOrganizationId
        );
      } else if (tabValue === 2) {
        if (!filters.employeeId || !filters.epfAccountId) {
          setError('Please provide Employee ID and EPF Account ID');
          return;
        }
        response = await hrService.getEmployeeStatementReport(
          filters.employeeId,
          filters.epfAccountId,
          filters.startDate,
          filters.endDate
        );
      } else if (tabValue === 3) {
        if (!filters.startDate || !filters.endDate) {
          setError('Please provide start and end dates');
          return;
        }
        response = await hrService.getComplianceReport(
          currentOrganizationId,
          filters.startDate,
          filters.endDate
        );
      } else if (tabValue === 4) {
        response = await hrService.getCostAnalysisReport(currentOrganizationId, filters.year);
      } else if (tabValue === 5) {
        response = await hrService.getTrendAnalysisReport(currentOrganizationId, 12);
      }
      
      setReportData(response.data);
    } catch (err: any) {
      console.error('Failed to load report:', err);
      setError(err.response?.data?.message || 'Failed to load report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (currentOrganizationId && tabValue === 0) {
      loadReport();
    }
  }, [currentOrganizationId, tabValue]);

  const renderReportContent = () => {
    if (!reportData) {
      return <Alert severity="info">Click "Generate Report" to view data</Alert>;
    }

    return (
      <Grid container spacing={2}>
        {Object.entries(reportData).map(([key, value]) => (
          <Grid item xs={12} md={6} key={key}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  {key.replace(/([A-Z])/g, ' $1').trim()}
                </Typography>
                <Typography variant="h6" sx={{ mt: 1 }}>
                  {typeof value === 'object' ? (
                    <pre style={{ fontSize: '0.875rem', overflow: 'auto' }}>
                      {JSON.stringify(value, null, 2)}
                    </pre>
                  ) : (
                    String(value)
                  )}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    );
  };

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Provident Fund Reporting
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Comprehensive reporting and analytics for Provident Fund
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="Executive Dashboard" icon={<Dashboard />} />
            <Tab label="Manager Team Report" icon={<Group />} />
            <Tab label="Employee Statement" icon={<Person />} />
            <Tab label="Compliance Report" icon={<Security />} />
            <Tab label="Cost Analysis" icon={<Assessment />} />
            <Tab label="Trend Analysis" icon={<TrendingUp />} />
          </Tabs>
        </Box>

        <CardContent>
          {(tabValue === 1 || tabValue === 2 || tabValue === 3) && (
            <Box sx={{ mb: 3 }}>
              <Grid container spacing={2}>
                {tabValue === 1 && (
                  <>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Manager ID"
                        value={filters.managerId}
                        onChange={(e) => setFilters({ ...filters, managerId: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Department ID"
                        value={filters.departmentId}
                        onChange={(e) => setFilters({ ...filters, departmentId: e.target.value })}
                      />
                    </Grid>
                  </>
                )}
                {tabValue === 2 && (
                  <>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Employee ID"
                        value={filters.employeeId}
                        onChange={(e) => setFilters({ ...filters, employeeId: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="EPF Account ID"
                        value={filters.epfAccountId}
                        onChange={(e) => setFilters({ ...filters, epfAccountId: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Start Date"
                        type="date"
                        value={filters.startDate}
                        onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="End Date"
                        type="date"
                        value={filters.endDate}
                        onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                  </>
                )}
                {tabValue === 3 && (
                  <>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Start Date"
                        type="date"
                        value={filters.startDate}
                        onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="End Date"
                        type="date"
                        value={filters.endDate}
                        onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                        InputLabelProps={{ shrink: true }}
                      />
                    </Grid>
                  </>
                )}
                {tabValue === 4 && (
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      label="Year"
                      type="number"
                      value={filters.year}
                      onChange={(e) => setFilters({ ...filters, year: parseInt(e.target.value) })}
                    />
                  </Grid>
                )}
              </Grid>
              <Box sx={{ mt: 2 }}>
                <Button
                  variant="contained"
                  startIcon={<FileDownload />}
                  onClick={loadReport}
                  disabled={loading}
                >
                  {loading ? <CircularProgress size={24} /> : 'Generate Report'}
                </Button>
              </Box>
            </Box>
          )}

          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
              <CircularProgress />
            </Box>
          ) : (
            renderReportContent()
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default ProvidentFundReporting;

