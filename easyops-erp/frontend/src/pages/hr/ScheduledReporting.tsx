import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Alert,
  MenuItem,
  Grid,
  IconButton,
} from '@mui/material';
import {
  Add,
  Edit,
  PlayArrow,
  Delete,
  Schedule,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

interface ScheduledReport {
  scheduledReportId: string;
  reportName: string;
  reportType: string;
  scheduleFrequency: string;
  nextExecutionDate: string;
  status: string;
  recipients: string[];
}

const ScheduledReporting: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [scheduledReports, setScheduledReports] = useState<ScheduledReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState({
    reportName: '',
    reportType: '',
    scheduleFrequency: 'daily',
    recipients: '',
    reportConfig: {} as any,
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadScheduledReports();
    }
  }, [currentOrganizationId]);

  const loadScheduledReports = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.getScheduledReports(currentOrganizationId);
      setScheduledReports(response.data || []);
    } catch (err: any) {
      console.error('Failed to load scheduled reports:', err);
      setError(err.response?.data?.message || 'Failed to load scheduled reports');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!currentOrganizationId) return;
    try {
      await hrService.createScheduledReport({
        ...formData,
        organizationId: currentOrganizationId,
        recipients: formData.recipients.split(',').map(r => r.trim()),
      });
      setOpenDialog(false);
      loadScheduledReports();
    } catch (err: any) {
      console.error('Failed to create scheduled report:', err);
      setError(err.response?.data?.message || 'Failed to create scheduled report');
    }
  };

  const handleExecute = async (reportId: string) => {
    try {
      await hrService.executeScheduledReport(reportId);
      loadScheduledReports();
    } catch (err: any) {
      console.error('Failed to execute report:', err);
      setError(err.response?.data?.message || 'Failed to execute report');
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Scheduled Reporting</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setOpenDialog(true)}
        >
          Create Scheduled Report
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Report Name</TableCell>
                  <TableCell>Report Type</TableCell>
                  <TableCell>Frequency</TableCell>
                  <TableCell>Next Execution</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {scheduledReports.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No scheduled reports found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  scheduledReports.map((report) => (
                    <TableRow key={report.scheduledReportId}>
                      <TableCell>{report.reportName}</TableCell>
                      <TableCell>{report.reportType}</TableCell>
                      <TableCell>{report.scheduleFrequency}</TableCell>
                      <TableCell>
                        {report.nextExecutionDate
                          ? new Date(report.nextExecutionDate).toLocaleDateString()
                          : '-'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={report.status}
                          color={
                            report.status === 'active' ? 'success' :
                            report.status === 'paused' ? 'warning' : 'default'
                          }
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleExecute(report.scheduledReportId)}
                          title="Execute Now"
                        >
                          <PlayArrow />
                        </IconButton>
                        <IconButton size="small" title="Edit">
                          <Edit />
                        </IconButton>
                        <IconButton size="small" title="Delete">
                          <Delete />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Scheduled Report</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Report Name"
                value={formData.reportName}
                onChange={(e) => setFormData({ ...formData, reportName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Report Type"
                value={formData.reportType}
                onChange={(e) => setFormData({ ...formData, reportType: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Schedule Frequency"
                value={formData.scheduleFrequency}
                onChange={(e) => setFormData({ ...formData, scheduleFrequency: e.target.value })}
                select
                required
              >
                <MenuItem value="daily">Daily</MenuItem>
                <MenuItem value="weekly">Weekly</MenuItem>
                <MenuItem value="monthly">Monthly</MenuItem>
                <MenuItem value="quarterly">Quarterly</MenuItem>
                <MenuItem value="yearly">Yearly</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Recipients (comma-separated emails)"
                value={formData.recipients}
                onChange={(e) => setFormData({ ...formData, recipients: e.target.value })}
                placeholder="email1@example.com, email2@example.com"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ScheduledReporting;

