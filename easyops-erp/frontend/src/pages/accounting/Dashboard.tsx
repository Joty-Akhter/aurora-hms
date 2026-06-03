import React, { useState, useEffect } from "react";
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Chip,
  Paper,
  IconButton,
  Divider
} from "@mui/material";
import {
  TrendingUp,
  TrendingDown,
  AccountBalance,
  Receipt,
  AttachMoney,
  Warning,
  ArrowForward,
  Refresh
} from "@mui/icons-material";
import { useAuth } from "../../contexts/AuthContext";
import accountingService from "../../services/accountingService";
import { useNavigate } from "react-router-dom";

interface DashboardSummary {
  // AR Summary
  totalReceivables: number;
  currentReceivables: number;
  overdueReceivables: number;
  totalInvoices: number;
  overdueInvoices: number;
  customersOverCreditLimit: number;
  
  // AP Summary
  totalPayables: number;
  currentPayables: number;
  overduePayables: number;
  totalBills: number;
  overdueBills: number;
  billsDueThisWeek: number;
  
  // Cash Position
  totalCash: number;
  totalBankBalance: number;
  bankAccounts: BankAccountSummary[];
  
  // GL Summary
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  netIncome: number;
  
  // Recent Activity
  recentTransactions: RecentTransaction[];
  
  // Alerts
  alerts: DashboardAlert[];
}

interface BankAccountSummary {
  accountName: string;
  accountNumber: string;
  balance: number;
  accountType: string;
}

interface RecentTransaction {
  date: string;
  type: string;
  reference: string;
  description: string;
  amount: number;
}

interface DashboardAlert {
  type: string;
  severity: string;
  message: string;
  actionUrl: string;
}

const Dashboard = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    loadDashboard();
  }, [currentOrganizationId]);

  const loadDashboard = async () => {
    if (!currentOrganizationId) return;
    
    setLoading(true);
    setError("");
    
    try {
      const data = await accountingService.getDashboardSummary(currentOrganizationId);
      setSummary(data);
    } catch (err: any) {
      console.error("Failed to load dashboard:", err);
      setError(err.message || "Failed to load dashboard");
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number | undefined | null) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case "ERROR":
        return "error";
      case "WARNING":
        return "warning";
      case "INFO":
        return "info";
      default:
        return "default";
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  if (!summary) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="info">No dashboard data available</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant="h4">Accounting Dashboard</Typography>
        <IconButton onClick={loadDashboard} color="primary">
          <Refresh />
        </IconButton>
      </Box>

      {/* Alerts Section */}
      {summary.alerts && summary.alerts.length > 0 && (
        <Box sx={{ mb: 3 }}>
          {summary.alerts.map((alert, index) => (
            <Alert 
              key={index} 
              severity={getSeverityColor(alert.severity) as any}
              action={
                <Button 
                  color="inherit" 
                  size="small"
                  onClick={() => navigate(alert.actionUrl)}
                >
                  View
                </Button>
              }
              sx={{ mb: 1 }}
            >
              {alert.message}
            </Alert>
          ))}
        </Box>
      )}

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {/* AR Summary */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ height: "100%", bgcolor: "#e3f2fd" }}>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Accounts Receivable
                  </Typography>
                  <Typography variant="h5">
                    {formatCurrency(summary.totalReceivables)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {summary.totalInvoices} invoices
                  </Typography>
                </Box>
                <Receipt sx={{ fontSize: 40, color: "#1976d2" }} />
              </Box>
              <Divider sx={{ my: 1 }} />
              <Box sx={{ display: "flex", justifyContent: "space-between", mt: 1 }}>
                <Typography variant="body2" color="success.main">
                  Current: {formatCurrency(summary.currentReceivables)}
                </Typography>
              </Box>
              <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                <Typography variant="body2" color="error.main">
                  Overdue: {formatCurrency(summary.overdueReceivables)}
                </Typography>
                {summary.overdueInvoices > 0 && (
                  <Chip 
                    label={`${summary.overdueInvoices} overdue`} 
                    size="small" 
                    color="error"
                  />
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* AP Summary */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ height: "100%", bgcolor: "#fff3e0" }}>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Accounts Payable
                  </Typography>
                  <Typography variant="h5">
                    {formatCurrency(summary.totalPayables)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {summary.totalBills} bills
                  </Typography>
                </Box>
                <AttachMoney sx={{ fontSize: 40, color: "#f57c00" }} />
              </Box>
              <Divider sx={{ my: 1 }} />
              <Box sx={{ display: "flex", justifyContent: "space-between", mt: 1 }}>
                <Typography variant="body2" color="success.main">
                  Current: {formatCurrency(summary.currentPayables)}
                </Typography>
              </Box>
              <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                <Typography variant="body2" color="error.main">
                  Overdue: {formatCurrency(summary.overduePayables)}
                </Typography>
                {summary.overdueBills > 0 && (
                  <Chip 
                    label={`${summary.overdueBills} overdue`} 
                    size="small" 
                    color="error"
                  />
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Cash Position */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ height: "100%", bgcolor: "#e8f5e9" }}>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Cash Balance
                  </Typography>
                  <Typography variant="h5">
                    {formatCurrency(summary.totalCash)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {summary.bankAccounts?.length || 0} accounts
                  </Typography>
                </Box>
                <AccountBalance sx={{ fontSize: 40, color: "#388e3c" }} />
              </Box>
              <Divider sx={{ my: 1 }} />
              <Box sx={{ mt: 1 }}>
                {summary.bankAccounts && summary.bankAccounts.slice(0, 2).map((acc, idx) => (
                  <Typography key={idx} variant="body2">
                    {acc.accountName}: {formatCurrency(acc.balance)}
                  </Typography>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Working Capital */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ height: "100%", bgcolor: "#f3e5f5" }}>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Working Capital
                  </Typography>
                  <Typography variant="h5">
                    {formatCurrency((summary.totalReceivables || 0) + (summary.totalCash || 0) - (summary.totalPayables || 0))}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    AR + Cash - AP
                  </Typography>
                </Box>
                {((summary.totalReceivables || 0) + (summary.totalCash || 0) - (summary.totalPayables || 0)) > 0 ? (
                  <TrendingUp sx={{ fontSize: 40, color: "#388e3c" }} />
                ) : (
                  <TrendingDown sx={{ fontSize: 40, color: "#d32f2f" }} />
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Quick Actions */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Quick Actions
          </Typography>
          <Grid container spacing={2}>
            <Grid item>
              <Button 
                variant="contained" 
                onClick={() => navigate("/accounting/invoices")}
                startIcon={<Receipt />}
              >
                Create Invoice
              </Button>
            </Grid>
            <Grid item>
              <Button 
                variant="contained" 
                onClick={() => navigate("/accounting/bills")}
                startIcon={<AttachMoney />}
              >
                Create Bill
              </Button>
            </Grid>
            <Grid item>
              <Button 
                variant="outlined" 
                onClick={() => navigate("/accounting/journal-entries")}
              >
                Journal Entry
              </Button>
            </Grid>
            <Grid item>
              <Button 
                variant="outlined" 
                onClick={() => navigate("/accounting/bank-transactions")}
              >
                Bank Transaction
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Recent Transactions */}
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
                <Typography variant="h6">Recent Transactions</Typography>
                <Button 
                  endIcon={<ArrowForward />}
                  onClick={() => navigate("/accounting/journal-entries")}
                >
                  View All
                </Button>
              </Box>
              {summary.recentTransactions && summary.recentTransactions.length > 0 ? (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Reference</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell align="right">Amount</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {summary.recentTransactions.map((txn, index) => (
                      <TableRow key={index}>
                        <TableCell>{txn.date}</TableCell>
                        <TableCell>
                          <Chip label={txn.type} size="small" />
                        </TableCell>
                        <TableCell>{txn.reference}</TableCell>
                        <TableCell>{txn.description}</TableCell>
                        <TableCell align="right">
                          {formatCurrency(txn.amount)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <Typography color="textSecondary">No recent transactions</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Bank Accounts Summary */}
        <Grid item xs={12} lg={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
                <Typography variant="h6">Bank Accounts</Typography>
                <Button 
                  endIcon={<ArrowForward />}
                  onClick={() => navigate("/accounting/bank-accounts")}
                >
                  View All
                </Button>
              </Box>
              {summary.bankAccounts && summary.bankAccounts.length > 0 ? (
                <Box>
                  {summary.bankAccounts.map((acc, index) => (
                    <Box key={index} sx={{ mb: 2 }}>
                      <Typography variant="subtitle2">{acc.accountName}</Typography>
                      <Typography variant="caption" color="textSecondary">
                        {acc.accountNumber}
                      </Typography>
                      <Typography variant="h6" color="primary">
                        {formatCurrency(acc.balance)}
                      </Typography>
                      {index < summary.bankAccounts.length - 1 && <Divider sx={{ mt: 1 }} />}
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography color="textSecondary">No bank accounts</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

