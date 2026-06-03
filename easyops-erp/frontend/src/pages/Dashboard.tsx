import React, { useEffect, useState } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Avatar,
  CircularProgress,
} from '@mui/material';
import {
  People as PeopleIcon,
  Security as SecurityIcon,
  CheckCircle as ActiveIcon,
  Cancel as InactiveIcon,
} from '@mui/icons-material';
import { useAuth } from '@contexts/AuthContext';
import userService from '@services/userService';
import rbacService from '@services/rbacService';
import { useSnackbar } from 'notistack';

interface StatCardProps {
  title: string;
  value: number | string;
  icon: React.ReactElement;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color }) => (
  <Card sx={{ height: '100%' }}>
    <CardContent>
      <Box display="flex" alignItems="center" justifyContent="space-between">
        <Box>
          <Typography color="textSecondary" variant="body2" gutterBottom>
            {title}
          </Typography>
          <Typography variant="h4" fontWeight="bold">
            {value}
          </Typography>
        </Box>
        <Avatar
          sx={{
            backgroundColor: color,
            width: 56,
            height: 56,
          }}
        >
          {icon}
        </Avatar>
      </Box>
    </CardContent>
  </Card>
);

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();

  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    inactiveUsers: 0,
    totalRoles: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const userStats = await userService.getUserStats();
        let totalRoles = 0;
        try {
          const rolesPage = await rbacService.getAllRoles({ page: 0, size: 1 });
          totalRoles = rolesPage?.totalElements ?? rolesPage?.content?.length ?? 0;
        } catch {
          try {
            const activeRoles = await rbacService.getActiveRoles();
            totalRoles = activeRoles?.length ?? 0;
          } catch {
            totalRoles = 0;
          }
        }
        setStats({
          totalUsers: userStats.totalUsers ?? 0,
          activeUsers: userStats.activeUsers ?? 0,
          inactiveUsers: userStats.inactiveUsers ?? 0,
          totalRoles,
        });
      } catch (error: any) {
        console.error('Dashboard stats error:', error);
        enqueueSnackbar('Failed to load dashboard stats', { variant: 'warning' });
        // Set defaults even on error
        setStats({
          totalUsers: 0,
          activeUsers: 0,
          inactiveUsers: 0,
          totalRoles: 0,
        });
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, [enqueueSnackbar]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Welcome back, {[user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.username}!
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Here's what's happening with your system today.
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Users"
            value={stats.totalUsers}
            icon={<PeopleIcon />}
            color="#3f51b5"
          />
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Active Users"
            value={stats.activeUsers}
            icon={<ActiveIcon />}
            color="#4caf50"
          />
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Inactive Users"
            value={stats.inactiveUsers}
            icon={<InactiveIcon />}
            color="#f44336"
          />
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Roles"
            value={stats.totalRoles}
            icon={<SecurityIcon />}
            color="#ff9800"
          />
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Navigate using the sidebar to manage users, roles, and permissions.
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              System Information
            </Typography>
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary">
                <strong>Version:</strong> 1.0.0
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                <strong>Environment:</strong> Development
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                <strong>Current User:</strong> {user?.username}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                <strong>Email:</strong> {user?.email}
              </Typography>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Activity tracking coming soon...
            </Typography>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

