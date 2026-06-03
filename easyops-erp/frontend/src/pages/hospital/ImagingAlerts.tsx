import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Notifications as NotificationIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

interface ImagingAlert {
  alertId: string;
  studyId: string;
  patientId: string;
  alertStatus: string;
  alertPriority: string;
  alertMessage: string;
  findingKeywords?: string;
  isAcknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  acknowledgmentNotes?: string;
  notificationSentDate?: string;
  notificationDelivered: boolean;
  createdAt: string;
}

const ImagingAlerts: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [alerts, setAlerts] = useState<ImagingAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'all' | 'unacknowledged'>('unacknowledged');
  const [selectedAlert, setSelectedAlert] = useState<ImagingAlert | null>(null);
  const [acknowledgeDialog, setAcknowledgeDialog] = useState(false);
  const [acknowledgmentNotes, setAcknowledgmentNotes] = useState('');
  
  useEffect(() => {
    if (id) {
      loadAlerts();
    }
  }, [id, filter]);
  
  const loadAlerts = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      const response = filter === 'unacknowledged'
        ? await hospitalService.getUnacknowledgedImagingAlerts(id)
        : await hospitalService.getImagingAlerts(id);
      
      setAlerts(response.data);
    } catch (err: any) {
      console.error('Failed to load alerts:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load imaging alerts'));
      enqueueSnackbar('Failed to load imaging alerts', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleAcknowledge = async () => {
    if (!selectedAlert) return;
    
    try {
      await hospitalService.acknowledgeImagingAlert(selectedAlert.alertId, acknowledgmentNotes);
      enqueueSnackbar('Alert acknowledged successfully', { variant: 'success' });
      setAcknowledgeDialog(false);
      setSelectedAlert(null);
      setAcknowledgmentNotes('');
      loadAlerts();
    } catch (err: any) {
      console.error('Failed to acknowledge alert:', err);
      enqueueSnackbar('Failed to acknowledge alert', { variant: 'error' });
    }
  };
  
  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      case 'LOW': return 'default';
      default: return 'default';
    }
  };
  
  const getPriorityIcon = (priority?: string) => {
    switch (priority) {
      case 'CRITICAL': return <ErrorIcon />;
      case 'HIGH': return <WarningIcon />;
      default: return <NotificationIcon />;
    }
  };
  
  if (loading && alerts.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            startIcon={<BackIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/imaging-studies`)}
          >
            Back
          </Button>
          <Typography variant="h4">Imaging Alerts & Notifications</Typography>
        </Box>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      {/* Filter */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <FormControl fullWidth>
            <InputLabel>Filter</InputLabel>
            <Select
              value={filter}
              label="Filter"
              onChange={(e) => setFilter(e.target.value as 'all' | 'unacknowledged')}
            >
              <MenuItem value="unacknowledged">Unacknowledged</MenuItem>
              <MenuItem value="all">All Alerts</MenuItem>
            </Select>
          </FormControl>
        </CardContent>
      </Card>
      
      {/* Alerts Table */}
      <TableContainer component={Card}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Alert Message</TableCell>
              <TableCell>Finding Keywords</TableCell>
              <TableCell>Notification</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {alerts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                    No alerts found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              alerts.map((alert) => (
                <TableRow key={alert.alertId} hover>
                  <TableCell>
                    <Chip
                      icon={getPriorityIcon(alert.alertPriority)}
                      label={alert.alertPriority}
                      size="small"
                      color={getPriorityColor(alert.alertPriority) as any}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={alert.alertStatus}
                      size="small"
                      color={alert.isAcknowledged ? 'success' : 'warning'}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{alert.alertMessage}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {alert.findingKeywords || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {alert.notificationDelivered ? (
                      <Chip
                        icon={<CheckCircleIcon />}
                        label="Delivered"
                        size="small"
                        color="success"
                      />
                    ) : (
                      <Chip label="Pending" size="small" />
                    )}
                  </TableCell>
                  <TableCell>
                    {new Date(alert.createdAt).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    {!alert.isAcknowledged && (
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          setSelectedAlert(alert);
                          setAcknowledgeDialog(true);
                        }}
                      >
                        Acknowledge
                      </Button>
                    )}
                    {alert.isAcknowledged && (
                      <Chip
                        icon={<CheckCircleIcon />}
                        label="Acknowledged"
                        size="small"
                        color="success"
                      />
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      
      {/* Acknowledge Dialog */}
      <Dialog open={acknowledgeDialog} onClose={() => setAcknowledgeDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Acknowledge Alert</DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Alert Message
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                {selectedAlert.alertMessage}
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Acknowledgment Notes"
                value={acknowledgmentNotes}
                onChange={(e) => setAcknowledgmentNotes(e.target.value)}
                sx={{ mt: 2 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAcknowledgeDialog(false)}>Cancel</Button>
          <Button onClick={handleAcknowledge} variant="contained" color="primary">
            Acknowledge
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ImagingAlerts;
