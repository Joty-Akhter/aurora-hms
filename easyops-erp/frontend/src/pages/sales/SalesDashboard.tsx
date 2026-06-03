import React, { useState, useEffect } from "react";
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Alert,
  Button,
} from "@mui/material";
import {
  TrendingUp,
  Receipt,
  ShoppingCart,
  AttachMoney,
  People,
} from "@mui/icons-material";
import { useAuth } from "../../contexts/AuthContext";
import salesService from "../../services/salesService";
import { Quotation, SalesOrder } from "../../types/sales";
import { useNavigate } from "react-router-dom";

const SalesDashboard = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    if (currentOrganizationId) {
      loadDashboardData();
    }
  }, [currentOrganizationId]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [quotationsData, ordersData] = await Promise.all([
        salesService.getQuotations(currentOrganizationId!),
        salesService.getSalesOrders(currentOrganizationId!),
      ]);
      setQuotations(quotationsData);
      setOrders(ordersData);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  };

  // Calculate metrics
  const totalQuotations = quotations.length;
  const activeQuotations = quotations.filter(q => q.status === 'SENT' || q.status === 'DRAFT').length;
  const acceptedQuotations = quotations.filter(q => q.status === 'ACCEPTED').length;
  const quotationValue = quotations
    .filter(q => q.status !== 'REJECTED' && q.status !== 'EXPIRED')
    .reduce((sum, q) => sum + q.totalAmount, 0);

  const totalOrders = orders.length;
  const activeOrders = orders.filter(o => o.status === 'CONFIRMED' || o.status === 'IN_PROGRESS').length;
  const completedOrders = orders.filter(o => o.status === 'COMPLETED' || o.status === 'INVOICED').length;
  const orderValue = orders
    .filter(o => o.status !== 'CANCELLED')
    .reduce((sum, o) => sum + o.totalAmount, 0);

  const recentQuotations = quotations
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  const recentOrders = orders
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  // Top customers by order value
  const customerOrderValue = orders.reduce((acc, order) => {
    if (order.status !== 'CANCELLED') {
      acc[order.customerName] = (acc[order.customerName] || 0) + order.totalAmount;
    }
    return acc;
  }, {} as Record<string, number>);

  const topCustomers = Object.entries(customerOrderValue)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Sales Dashboard</Typography>
        <Box>
          <Button variant="outlined" onClick={() => navigate("/sales/quotations")} sx={{ mr: 1 }}>
            Quotations
          </Button>
          <Button variant="outlined" onClick={() => navigate("/sales/orders")}>
            Orders
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {/* Quotations Summary */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ bgcolor: "#e3f2fd", height: "100%" }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Quotations
                  </Typography>
                  <Typography variant="h4">
                    {totalQuotations}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {activeQuotations} active, {acceptedQuotations} accepted
                  </Typography>
                  <Typography variant="h6" sx={{ mt: 1 }}>
                    {formatCurrency(quotationValue)}
                  </Typography>
                </Box>
                <Receipt sx={{ fontSize: 40, color: "#1976d2" }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Orders Summary */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ bgcolor: "#e8f5e9", height: "100%" }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Sales Orders
                  </Typography>
                  <Typography variant="h4">
                    {totalOrders}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {activeOrders} in progress, {completedOrders} completed
                  </Typography>
                  <Typography variant="h6" sx={{ mt: 1 }}>
                    {formatCurrency(orderValue)}
                  </Typography>
                </Box>
                <ShoppingCart sx={{ fontSize: 40, color: "#388e3c" }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Conversion Rate */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ bgcolor: "#fff3e0", height: "100%" }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Conversion Rate
                  </Typography>
                  <Typography variant="h4">
                    {totalQuotations > 0 ? Math.round((acceptedQuotations / totalQuotations) * 100) : 0}%
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    Quote to Acceptance
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    {acceptedQuotations} of {totalQuotations} accepted
                  </Typography>
                </Box>
                <TrendingUp sx={{ fontSize: 40, color: "#f57c00" }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Average Order Value */}
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ bgcolor: "#f3e5f5", height: "100%" }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Avg Order Value
                  </Typography>
                  <Typography variant="h4">
                    {formatCurrency(totalOrders > 0 ? orderValue / totalOrders : 0)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    Per order
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    Total: {formatCurrency(orderValue)}
                  </Typography>
                </Box>
                <AttachMoney sx={{ fontSize: 40, color: "#7b1fa2" }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* Recent Quotations */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Quotations
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Quotation #</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recentQuotations.map((quotation) => (
                    <TableRow key={quotation.id}>
                      <TableCell>{quotation.quotationNumber}</TableCell>
                      <TableCell>{quotation.customerName}</TableCell>
                      <TableCell align="right">{formatCurrency(quotation.totalAmount)}</TableCell>
                      <TableCell>
                        <Chip label={quotation.status} size="small" />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Orders */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Orders
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Order #</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recentOrders.map((order) => (
                    <TableRow key={order.id}>
                      <TableCell>{order.orderNumber}</TableCell>
                      <TableCell>{order.customerName}</TableCell>
                      <TableCell align="right">{formatCurrency(order.totalAmount)}</TableCell>
                      <TableCell>
                        <Chip label={order.status} size="small" />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>

        {/* Top Customers */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <People sx={{ mr: 1 }} />
                <Typography variant="h6">
                  Top Customers by Order Value
                </Typography>
              </Box>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Customer</TableCell>
                    <TableCell align="right">Total Orders</TableCell>
                    <TableCell align="right">Total Value</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {topCustomers.map(([customerName, value]) => {
                    const customerOrders = orders.filter(o => o.customerName === customerName && o.status !== 'CANCELLED');
                    return (
                      <TableRow key={customerName}>
                        <TableCell>{customerName}</TableCell>
                        <TableCell align="right">{customerOrders.length}</TableCell>
                        <TableCell align="right">{formatCurrency(value)}</TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </Grid>

        {/* Sales Pipeline */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Sales Pipeline
              </Typography>
              <Box sx={{ mt: 2 }}>
                {[
                  { status: "Quotations (Draft)", count: quotations.filter(q => q.status === 'DRAFT').length, color: "#757575" },
                  { status: "Quotations (Sent)", count: quotations.filter(q => q.status === 'SENT').length, color: "#2196f3" },
                  { status: "Quotations (Accepted)", count: quotations.filter(q => q.status === 'ACCEPTED').length, color: "#4caf50" },
                  { status: "Orders (Confirmed)", count: orders.filter(o => o.status === 'CONFIRMED').length, color: "#ff9800" },
                  { status: "Orders (In Progress)", count: orders.filter(o => o.status === 'IN_PROGRESS').length, color: "#f57c00" },
                  { status: "Orders (Completed)", count: orders.filter(o => o.status === 'COMPLETED').length, color: "#8bc34a" },
                  { status: "Orders (Invoiced)", count: orders.filter(o => o.status === 'INVOICED').length, color: "#9c27b0" },
                ].map((stage) => (
                  <Box key={stage.status} sx={{ mb: 2 }}>
                    <Box display="flex" justifyContent="space-between" mb={0.5}>
                      <Typography variant="body2">{stage.status}</Typography>
                      <Typography variant="body2" fontWeight="bold">{stage.count}</Typography>
                    </Box>
                    <Box sx={{ height: 8, bgcolor: "#e0e0e0", borderRadius: 1, overflow: "hidden" }}>
                      <Box
                        sx={{
                          height: "100%",
                          width: `${Math.min((stage.count / Math.max(totalQuotations, totalOrders)) * 100, 100)}%`,
                          bgcolor: stage.color,
                        }}
                      />
                    </Box>
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SalesDashboard;

