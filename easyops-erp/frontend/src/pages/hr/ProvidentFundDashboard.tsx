import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  AccountBalance,
  TrendingUp,
  People,
  CheckCircle,
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const ProvidentFundDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadDashboard();
    }
  }, [currentOrganizationId]);

  const loadDashboard = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.getExecutiveDashboard(currentOrganizationId);
      setDashboardData(response.data);
    } catch (err: any) {
      console.error('Failed to load Provident Fund dashboard:', err);
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  if (!dashboardData) {
    return <Alert severity="info">No data available</Alert>;
  }

  const statCards = [
    {
      title: 'Total Balance',
      value: `₹${(dashboardData.totalBalance || 0).toLocaleString('en-IN')}`,
      icon: <AccountBalance />,
      color: '#1976d2',
    },
    {
      title: 'Total Contributions',
      value: `₹${(dashboardData.totalContributions || 0).toLocaleString('en-IN')}`,
      icon: <TrendingUp />,
      color: '#2e7d32',
    },
    {
      title: 'Active Accounts',
      value: dashboardData.activeAccounts || 0,
      icon: <People />,
      color: '#ed6c02',
    },
    {
      title: 'Compliance Status',
      value: dashboardData.complianceStatus || 'Unknown',
      icon: <CheckCircle />,
      color: dashboardData.complianceStatus === 'Compliant' ? '#2e7d32' : '#d32f2f',
    },
  ];

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Provident Fund Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Overview of Provident Fund accounts, contributions, and compliance
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {statCards.map((card, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" mb={2}>
                  <Box
                    sx={{
                      color: card.color,
                      mr: 2,
                      display: 'flex',
                      alignItems: 'center',
                    }}
                  >
                    {card.icon}
                  </Box>
                  <Typography variant="h6" component="div">
                    {card.title}
                  </Typography>
                </Box>
                <Typography variant="h4" component="div" sx={{ fontWeight: 'bold' }}>
                  {card.value}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Participation Metrics
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Total Employees: {dashboardData.totalEmployees || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Enrollment Rate: {dashboardData.participationRate || '0%'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Active Accounts: {dashboardData.activeAccounts || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Configuration
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Set organization-level EPF contribution rates, PF wage ceiling/floor, and employment eligibility (INT-09 /
                INT-12). Required when payroll uses deferred statutory PF lines.
              </Typography>
              <Button component={Link} to="/hr/provident-fund/organization-policy" variant="outlined" size="small">
                EPF organization policy
              </Button>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Compliance Status
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Pending: {dashboardData.pendingCompliance || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Overdue: {dashboardData.overdueCompliance || 0}
              </Typography>
              <Typography
                variant="body2"
                sx={{
                  color: dashboardData.complianceStatus === 'Compliant' ? 'success.main' : 'error.main',
                  fontWeight: 'bold',
                }}
              >
                Status: {dashboardData.complianceStatus || 'Unknown'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  );
};

export default ProvidentFundDashboard;

