import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Card, CardContent, Grid, Typography, Button, Paper
} from '@mui/material';
import {
  Description as ReportIcon,
  Assessment as AssessmentIcon,
  Inventory as InventoryIcon,
  AccountBalance as AccountBalanceIcon,
  Dashboard as DashboardIcon,
  Insights as InsightsIcon
} from '@mui/icons-material';

const Reports: React.FC = () => {
  const navigate = useNavigate();

  const reportCategories = [
    {
      title: 'Operational Reports',
      icon: <ReportIcon sx={{ fontSize: 40 }} />,
      reports: [
        { name: 'Monthly Closing Report', path: '/pharma/reports/monthly-closing', description: 'Comprehensive monthly report for sales representatives (area or territory)' },
        { name: 'Area Performance Report', path: '/pharma/reports/area-performance', description: 'Area-wise target, coverage, and performance metrics' },
        { name: 'Territory Performance Report', path: '/pharma/reports/territory-performance', description: 'Territory-level performance, target achievement, and incentive metrics' },
        { name: 'Area-Wise Collection Report', path: '/pharma/reports/collection/area-wise', description: 'Collection and deposit details by area' },
        { name: 'Employee-Wise Collection Report', path: '/pharma/reports/collection/employee-wise', description: 'Collection and deposit details by employee' },
      ]
    },
    {
      title: 'Inventory Reports',
      icon: <InventoryIcon sx={{ fontSize: 40 }} />,
      reports: [
        { name: 'In-Stock Total Amount', path: '/pharma/reports/inventory/total-amount', description: 'Total inventory value at central depot' },
        { name: 'In-Stock Product-Wise', path: '/pharma/reports/inventory/product-wise', description: 'Product-wise inventory quantities and amounts' },
        { name: 'Area-Wise Allocation', path: '/pharma/reports/inventory/area-allocation', description: 'Product allocation details grouped by area' },
        { name: 'Month-Wise / Annual Allocation', path: '/pharma/reports/inventory/month-wise-allocation', description: 'Allocation summary grouped by month or year' },
      ]
    },
    {
      title: 'Financial Reports',
      icon: <AccountBalanceIcon sx={{ fontSize: 40 }} />,
      reports: [
        { name: 'Accounts Balance Report', path: '/pharma/reports/financial/accounts-balance', description: 'Area-wise outstanding balances' },
        { name: 'Income & Expense Report', path: '/pharma/reports/financial/income-expense', description: 'Income and expense summary by area' },
        { name: 'Incentive Report', path: '/pharma/reports/financial/incentive', description: 'Incentive calculation and distribution details' },
      ]
    }
  ];

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Pharma Reports & Analytics
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Generate comprehensive reports for operations, inventory, and financial analysis
        </Typography>
        
        {/* Analytics Dashboards */}
        <Grid container spacing={2} sx={{ mb: 4 }}>
          <Grid item xs={12} md={6}>
            <Card 
              sx={{ 
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                cursor: 'pointer',
                height: '100%',
                '&:hover': { boxShadow: 6, transform: 'translateY(-2px)' },
                transition: 'all 0.3s'
              }}
              onClick={() => navigate('/pharma/analytics')}
            >
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <DashboardIcon sx={{ fontSize: 48 }} />
                <Box>
                  <Typography variant="h5" gutterBottom>
                    Analytics Dashboard
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Comprehensive analytics for territory/area performance, target achievement, and employee tracking
                  </Typography>
                </Box>
              </Box>
              <Button 
                variant="contained" 
                sx={{ 
                  bgcolor: 'white',
                  color: 'primary.main',
                  '&:hover': { bgcolor: 'grey.100' }
                }}
                onClick={(e) => {
                  e.stopPropagation();
                  navigate('/pharma/analytics');
                }}
              >
                Open Dashboard
              </Button>
            </Box>
          </CardContent>
        </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card 
              sx={{ 
                bgcolor: 'secondary.main',
                color: 'secondary.contrastText',
                cursor: 'pointer',
                height: '100%',
                '&:hover': { boxShadow: 6, transform: 'translateY(-2px)' },
                transition: 'all 0.3s'
              }}
              onClick={() => navigate('/pharma/territory-analytics')}
            >
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <InsightsIcon sx={{ fontSize: 48 }} />
                    <Box>
                      <Typography variant="h5" gutterBottom>
                        Territory Analytics
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        Territory-level performance analytics and optimization recommendations
                      </Typography>
                    </Box>
                  </Box>
                  <Button 
                    variant="contained" 
                    sx={{ 
                      bgcolor: 'white',
                      color: 'secondary.main',
                      '&:hover': { bgcolor: 'grey.100' }
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate('/pharma/territory-analytics');
                    }}
                  >
                    Open
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>

      <Grid container spacing={3}>
        {reportCategories.map((category, categoryIndex) => (
          <Grid item xs={12} key={categoryIndex}>
            <Paper sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Box sx={{ mr: 2, color: 'primary.main' }}>
                  {category.icon}
                </Box>
                <Typography variant="h5">
                  {category.title}
                </Typography>
              </Box>
              <Grid container spacing={2}>
                {category.reports.map((report, reportIndex) => (
                  <Grid item xs={12} md={6} key={reportIndex}>
                    <Card 
                      sx={{ 
                        height: '100%',
                        cursor: 'pointer',
                        '&:hover': { boxShadow: 4 }
                      }}
                      onClick={() => navigate(report.path)}
                    >
                      <CardContent>
                        <Typography variant="h6" gutterBottom>
                          {report.name}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          {report.description}
                        </Typography>
                        <Button 
                          variant="outlined" 
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(report.path);
                          }}
                        >
                          Generate Report
                        </Button>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default Reports;

