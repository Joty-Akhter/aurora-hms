import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Tooltip,
  Menu,
  MenuItem,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Visibility as ViewIcon,
  MoreVert as MoreVertIcon,
  PersonAdd as EscalateIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { CriticalValueAlert, CriticalValueAcknowledgmentRequest, CriticalValueEscalationRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`critical-value-tabpanel-${index}`}
      aria-labelledby={`critical-value-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const CriticalValueAlerts: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [alerts, setAlerts] = useState<CriticalValueAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [selectedAlert, setSelectedAlert] = useState<CriticalValueAlert | null>(null);
  const [acknowledgeDialogOpen, setAcknowledgeDialogOpen] = useState(false);
  const [escalateDialogOpen, setEscalateDialogOpen] = useState(false);
  const [acknowledgeResponse, setAcknowledgeResponse] = useState('');
  const [escalateProviderId, setEscalateProviderId] = useState('');
  const [escalateReason, setEscalateReason] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [menuAlert, setMenuAlert] = useState<CriticalValueAlert | null>(null);
  
  useEffect(() => {
    loadAlerts();
  }, [id, tabValue]);
  
  const loadAlerts = async () => {
    try {
      setLoading(true);
      setError(null);
      let response;
      
      if (id) {
        // Load alerts for specific patient
        response = await hospitalService.getCriticalValueAlertsByPatient(id);
      } else if (tabValue === 0) {
        // All alerts
        response = await hospitalService.getAllCriticalValueAlerts();
      } else {
        // Unacknowledged alerts
        response = await hospitalService.getUnacknowledgedCriticalValueAlerts();
      }
      
      setAlerts(response.data);
    } catch (err: any) {
      console.error('Failed to load critical value alerts:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load critical value alerts'));
      enqueueSnackbar('Failed to load critical value alerts', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleAcknowledge = async () => {
    if (!selectedAlert) return;
    
    try {
      const request: CriticalValueAcknowledgmentRequest = {
        providerResponse: acknowledgeResponse || 'Acknowledged',
        acknowledgmentNotes: acknowledgeResponse,
      };
      
      await hospitalService.acknowledgeCriticalValueAlert(selectedAlert.alertId, request);
      enqueueSnackbar('Critical value acknowledged successfully', { variant: 'success' });
      setAcknowledgeDialogOpen(false);
      setSelectedAlert(null);
      setAcknowledgeResponse('');
      loadAlerts();
    } catch (err: any) {
      console.error('Failed to acknowledge critical value:', err);
      enqueueSnackbar('Failed to acknowledge critical value', { variant: 'error' });
    }
  };
  
  const handleEscalate = async () => {
    if (!selectedAlert) return;
    
    try {
      const request: CriticalValueEscalationRequest = {
        escalatedToUserId: escalateProviderId,
        escalationReason: escalateReason,
      };
      
      await hospitalService.escalateCriticalValueAlert(selectedAlert.alertId, request);
      enqueueSnackbar('Critical value escalated successfully', { variant: 'success' });
      setEscalateDialogOpen(false);
      setSelectedAlert(null);
      setEscalateProviderId('');
      setEscalateReason('');
      loadAlerts();
    } catch (err: any) {
      console.error('Failed to escalate critical value:', err);
      enqueueSnackbar('Failed to escalate critical value', { variant: 'error' });
    }
  };
  
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, alert: CriticalValueAlert) => {
    setAnchorEl(event.currentTarget);
    setMenuAlert(alert);
  };
  
  const handleMenuClose = () => {
    setAnchorEl(null);
    setMenuAlert(null);
  };
  
  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'error';
      case 'MEDIUM': return 'warning';
      case 'LOW': return 'info';
      default: return 'default';
    }
  };
  
  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'ACKNOWLEDGED': return 'success';
      case 'PENDING': return 'warning';
      case 'NOTIFIED': return 'info';
      case 'ESCALATED': return 'error';
      case 'RESOLVED': return 'success';
      default: return 'default';
    }
  };
  
  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };
  
  const filteredAlerts = tabValue === 1 
    ? alerts.filter(a => !a.isAcknowledged)
    : alerts;
  
  if (loading) {
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
          {id && (
            <Button startIcon={<BackIcon />} onClick={() => navigate(`/hospital/patients/${id}`)}>
              ← Back to Patient
            </Button>
          )}
          <Typography variant="h4">Critical Value Alerts</Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={loadAlerts}>
          Refresh
        </Button>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>
      )}
      
      {!id && (
        <Card sx={{ mb: 3 }}>
          <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
            <Tab label={`All Alerts (${alerts.length})`} />
            <Tab label={`Unacknowledged (${alerts.filter(a => !a.isAcknowledged).length})`} />
          </Tabs>
        </Card>
      )}
      
      {filteredAlerts.length === 0 ? (
        <Card>
          <CardContent>
            <Typography variant="body1" color="textSecondary" align="center">
              {tabValue === 1 ? 'No unacknowledged critical value alerts' : 'No critical value alerts found'}
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><strong>Patient</strong></TableCell>
                <TableCell><strong>Test</strong></TableCell>
                <TableCell><strong>Result</strong></TableCell>
                <TableCell><strong>Reference Range</strong></TableCell>
                <TableCell><strong>Priority</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Alert Date</strong></TableCell>
                <TableCell><strong>Actions</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredAlerts.map((alert) => (
                <TableRow 
                  key={alert.alertId}
                  sx={{ 
                    backgroundColor: !alert.isAcknowledged ? 'rgba(255, 152, 0, 0.1)' : 'inherit',
                    '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' }
                  }}
                >
                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="bold">
                        {alert.patientName || 'Unknown Patient'}
                      </Typography>
                      {alert.mrn && (
                        <Typography variant="caption" color="textSecondary">
                          MRN: {alert.mrn}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="bold">
                        {alert.testName}
                      </Typography>
                      {alert.loincCode && (
                        <Typography variant="caption" color="textSecondary">
                          LOINC: {alert.loincCode}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="bold" color="error">
                        {alert.resultValue} {alert.resultUnits || ''}
                      </Typography>
                      {alert.alertMessage && (
                        <Typography variant="caption" color="error">
                          {alert.alertMessage}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    {alert.referenceRangeLow !== undefined && alert.referenceRangeHigh !== undefined ? (
                      <Typography variant="body2">
                        {alert.referenceRangeLow} - {alert.referenceRangeHigh} {alert.resultUnits || ''}
                      </Typography>
                    ) : (
                      <Typography variant="body2" color="textSecondary">-</Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={alert.alertPriority || 'MEDIUM'}
                      color={getPriorityColor(alert.alertPriority) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={alert.alertStatus || 'PENDING'}
                      color={getStatusColor(alert.alertStatus) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {formatDate(alert.createdAt)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Box display="flex" gap={1}>
                      <Tooltip title="View Details">
                        <IconButton
                          size="small"
                          onClick={() => navigate(`/hospital/patients/${alert.patientId}/lab-results/${alert.resultId}`)}
                        >
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                      {!alert.isAcknowledged && (
                        <>
                          <Tooltip title="Acknowledge">
                            <IconButton
                              size="small"
                              color="success"
                              onClick={() => {
                                setSelectedAlert(alert);
                                setAcknowledgeDialogOpen(true);
                              }}
                            >
                              <CheckCircleIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="More Actions">
                            <IconButton
                              size="small"
                              onClick={(e) => handleMenuOpen(e, alert)}
                            >
                              <MoreVertIcon />
                            </IconButton>
                          </Tooltip>
                        </>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      
      {/* Acknowledge Dialog */}
      <Dialog open={acknowledgeDialogOpen} onClose={() => setAcknowledgeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Acknowledge Critical Value</DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Patient: {selectedAlert.patientName}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Test: {selectedAlert.testName}
              </Typography>
              <Typography variant="body2" color="error" gutterBottom>
                Result: {selectedAlert.resultValue} {selectedAlert.resultUnits}
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Provider Response"
                value={acknowledgeResponse}
                onChange={(e) => setAcknowledgeResponse(e.target.value)}
                placeholder="Enter your response or action taken..."
                sx={{ mt: 2 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAcknowledgeDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAcknowledge} variant="contained" color="success">
            Acknowledge
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Escalate Dialog */}
      <Dialog open={escalateDialogOpen} onClose={() => setEscalateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Escalate Critical Value</DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Patient: {selectedAlert.patientName}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Test: {selectedAlert.testName}
              </Typography>
              <TextField
                fullWidth
                label="Escalate To (Provider ID)"
                value={escalateProviderId}
                onChange={(e) => setEscalateProviderId(e.target.value)}
                sx={{ mt: 2 }}
                required
              />
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Escalation Reason"
                value={escalateReason}
                onChange={(e) => setEscalateReason(e.target.value)}
                placeholder="Enter reason for escalation..."
                sx={{ mt: 2 }}
                required
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEscalateDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleEscalate} variant="contained" color="error">
            Escalate
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem
          onClick={() => {
            if (menuAlert) {
              setSelectedAlert(menuAlert);
              setEscalateDialogOpen(true);
            }
            handleMenuClose();
          }}
        >
          <EscalateIcon sx={{ mr: 1 }} fontSize="small" />
          Escalate
        </MenuItem>
        <MenuItem
          onClick={() => {
            if (menuAlert) {
              navigate(`/hospital/patients/${menuAlert.patientId}/lab-results/${menuAlert.resultId}`);
            }
            handleMenuClose();
          }}
        >
          <ViewIcon sx={{ mr: 1 }} fontSize="small" />
          View Result Details
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default CriticalValueAlerts;
