import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
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
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Tooltip,
  CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  Cancel as CancelIcon,
  Visibility as ViewIcon,
  Refresh as RefreshIcon,
  Schedule as ScheduleIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabOrder, LabOrderRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { localDateTimeInputMin } from '../../utils/formValidation';
import './Hospital.css';

const LabOrderManagement: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get('returnTo');
  const { enqueueSnackbar } = useSnackbar();
  const scheduleMin = localDateTimeInputMin();

  const [labOrders, setLabOrders] = useState<LabOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<LabOrder | null>(null);
  const [viewing, setViewing] = useState<LabOrder | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterPriority, setFilterPriority] = useState<string>('');
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [orderToCancel, setOrderToCancel] = useState<LabOrder | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [showRescheduleDialog, setShowRescheduleDialog] = useState(false);
  const [orderToReschedule, setOrderToReschedule] = useState<LabOrder | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [showHL7Dialog, setShowHL7Dialog] = useState(false);
  const [hl7Message, setHl7Message] = useState<string | null>(null);
  const [hl7MessageType, setHl7MessageType] = useState<string>('');
  
  const [formData, setFormData] = useState<LabOrderRequest>({
    patientId: id || '',
    testName: '',
    loincCode: '',
    testCategory: '',
    testType: '',
    isTestPanel: false,
    panelName: '',
    clinicalIndication: '',
    priority: 'ROUTINE',
    specialInstructions: '',
    fastingRequired: false,
    patientPreparationInstructions: '',
    orderingProviderId: '',
    orderingProviderName: '',
    orderingFacilityId: '',
    orderingFacilityName: '',
    laboratoryId: '',
    laboratoryName: '',
  });

  useEffect(() => {
    if (id) {
      loadLabOrders();
    }
  }, [id]);

  const loadLabOrders = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getLabOrders(id);
      setLabOrders(response.data);
    } catch (err: any) {
      console.error('Failed to load lab orders:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load lab orders'));
      enqueueSnackbar('Failed to load lab orders', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const filteredLabOrders = useMemo(() => {
    return labOrders.filter((order) => {
      if (filterStatus) {
        const status = order.orderStatus?.toUpperCase();
        if (filterStatus === 'pending' && status !== 'PENDING') return false;
        if (filterStatus === 'sent' && status !== 'SENT') return false;
        if (filterStatus === 'completed' && status !== 'COMPLETED') return false;
      }
      if (filterPriority && order.priority?.toUpperCase() !== filterPriority.toUpperCase()) {
        return false;
      }
      return true;
    });
  }, [labOrders, filterStatus, filterPriority]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    if (formData.scheduledDate && formData.scheduledDate < scheduleMin) {
      enqueueSnackbar('Scheduled date/time cannot be in the past', { variant: 'warning' });
      return;
    }

    try {
      if (editing) {
        await hospitalService.updateLabOrder(editing.orderId, formData);
        enqueueSnackbar('Lab order updated successfully', { variant: 'success' });
      } else {
        await hospitalService.createLabOrder(formData);
        enqueueSnackbar('Lab order created successfully', { variant: 'success' });
      }
      setShowForm(false);
      setEditing(null);
      resetForm();
      loadLabOrders();
    } catch (err: any) {
      const errorMsg = ehrApiErrorMessage(err, 'Failed to save lab order');
      enqueueSnackbar(errorMsg, { variant: 'error' });
    }
  };

  const resetForm = () => {
    setFormData({
      patientId: id || '',
      testName: '',
      loincCode: '',
      testCategory: '',
      testType: '',
      isTestPanel: false,
      panelName: '',
      clinicalIndication: '',
      priority: 'ROUTINE',
      specialInstructions: '',
      fastingRequired: false,
      patientPreparationInstructions: '',
      orderingProviderId: '',
      orderingProviderName: '',
      orderingFacilityId: '',
      orderingFacilityName: '',
      laboratoryId: '',
      laboratoryName: '',
      scheduledDate: '',
    });
  };

  const handleEdit = (order: LabOrder) => {
    setEditing(order);
    setFormData({
      patientId: order.patientId,
      encounterId: order.encounterId,
      orderNumber: order.orderNumber,
      testName: order.testName,
      loincCode: order.loincCode || '',
      testCategory: order.testCategory || '',
      testType: order.testType || '',
      isTestPanel: order.isTestPanel || false,
      panelName: order.panelName || '',
      clinicalIndication: order.clinicalIndication || '',
      priority: order.priority,
      specialInstructions: order.specialInstructions || '',
      fastingRequired: order.fastingRequired || false,
      patientPreparationInstructions: order.patientPreparationInstructions || '',
      orderingProviderId: order.orderingProviderId,
      orderingProviderName: order.orderingProviderName || '',
      orderingFacilityId: order.orderingFacilityId,
      orderingFacilityName: order.orderingFacilityName || '',
      laboratoryId: order.laboratoryId,
      laboratoryName: order.laboratoryName || '',
      scheduledDate: order.scheduledDate || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (orderId: string) => {
    if (!window.confirm('Are you sure you want to delete this lab order?')) return;
    
    try {
      await hospitalService.deleteLabOrder(orderId);
      enqueueSnackbar('Lab order deleted successfully', { variant: 'success' });
      loadLabOrders();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to delete lab order'), { variant: 'error' });
    }
  };

  const handleSend = async (orderId: string) => {
    try {
      await hospitalService.sendLabOrder(orderId);
      enqueueSnackbar('Lab order sent successfully', { variant: 'success' });
      loadLabOrders();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to send lab order'), { variant: 'error' });
    }
  };

  const handleCancel = async () => {
    if (!orderToCancel) return;
    
    try {
      await hospitalService.cancelLabOrder(orderToCancel.orderId, cancelReason);
      enqueueSnackbar('Lab order cancelled successfully', { variant: 'success' });
      setShowCancelDialog(false);
      setOrderToCancel(null);
      setCancelReason('');
      loadLabOrders();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to cancel lab order'), { variant: 'error' });
    }
  };

  const handleReschedule = async () => {
    if (!orderToReschedule || !rescheduleDate) return;
    if (rescheduleDate < scheduleMin) {
      enqueueSnackbar('Rescheduled date/time cannot be in the past', { variant: 'warning' });
      return;
    }

    try {
      await hospitalService.rescheduleLabOrder(orderToReschedule.orderId, rescheduleDate);
      enqueueSnackbar('Lab order rescheduled successfully', { variant: 'success' });
      setShowRescheduleDialog(false);
      setOrderToReschedule(null);
      setRescheduleDate('');
      loadLabOrders();
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to reschedule lab order'), { variant: 'error' });
    }
  };

  const handleGenerateHL7V2 = async (orderId: string) => {
    try {
      const response = await hospitalService.generateHL7V2OrmMessage(orderId);
      setHl7Message(response.data.messageContent);
      setHl7MessageType('HL7 V2 ORM');
      setShowHL7Dialog(true);
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to generate HL7 V2 message'), { variant: 'error' });
    }
  };

  const handleGenerateHL7Fhir = async (orderId: string) => {
    try {
      const response = await hospitalService.generateHL7FhirServiceRequest(orderId);
      setHl7Message(JSON.stringify(response.data.messageResource, null, 2));
      setHl7MessageType('HL7 FHIR ServiceRequest');
      setShowHL7Dialog(true);
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to generate HL7 FHIR message'), { variant: 'error' });
    }
  };

  const handleDownloadHL7 = () => {
    if (!hl7Message) return;
    const blob = new Blob([hl7Message], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `lab-order-${hl7MessageType.toLowerCase().replace(/\s+/g, '-')}-${Date.now()}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'default';
      case 'SENT': return 'info';
      case 'COLLECTED': return 'primary';
      case 'IN_PROCESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'STAT': return 'error';
      case 'ASAP': return 'warning';
      case 'URGENT': return 'warning';
      case 'ROUTINE': return 'default';
      default: return 'default';
    }
  };

  if (loading && labOrders.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3} flexWrap="wrap" gap={2}>
        <Typography variant="h4">Laboratory Orders</Typography>
        <Box display="flex" flexWrap="wrap" gap={1}>
          {returnTo && (
            <Button variant="outlined" onClick={() => navigate(decodeURIComponent(returnTo))}>
              ← Back to prescription
            </Button>
          )}
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadLabOrders}
            sx={{ mr: 2 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setEditing(null);
              resetForm();
              setShowForm(true);
            }}
          >
            New Lab Order
          </Button>
        </Box>
      </Box>

      {error && (
        <Box mb={2}>
          <Typography color="error">{error}</Typography>
        </Box>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Filter by Status</InputLabel>
                <Select
                  value={filterStatus}
                  label="Filter by Status"
                  onChange={(e) => setFilterStatus(e.target.value)}
                >
                  <MenuItem value="">All Orders</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                  <MenuItem value="sent">Sent</MenuItem>
                  <MenuItem value="completed">Completed</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Filter by Priority</InputLabel>
                <Select
                  value={filterPriority}
                  label="Filter by Priority"
                  onChange={(e) => setFilterPriority(e.target.value)}
                >
                  <MenuItem value="">All priorities</MenuItem>
                  <MenuItem value="ROUTINE">Routine</MenuItem>
                  <MenuItem value="STAT">Stat</MenuItem>
                  <MenuItem value="ASAP">ASAP</MenuItem>
                  <MenuItem value="URGENT">Urgent</MenuItem>
                  <MenuItem value="TIMED">Timed</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Order Number</TableCell>
              <TableCell>Test Name</TableCell>
              <TableCell>LOINC Code</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Order Date</TableCell>
              <TableCell>Scheduled</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredLabOrders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                    No lab orders found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              filteredLabOrders.map((order) => (
                <TableRow key={order.orderId} hover>
                  <TableCell>{order.orderNumber}</TableCell>
                  <TableCell>{order.testName}</TableCell>
                  <TableCell>{order.loincCode || '-'}</TableCell>
                  <TableCell>{order.testCategory || '-'}</TableCell>
                  <TableCell>
                    <Chip
                      label={order.priority}
                      size="small"
                      color={getPriorityColor(order.priority) as any}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={order.orderStatus}
                      size="small"
                      color={getStatusColor(order.orderStatus) as any}
                    />
                  </TableCell>
                  <TableCell>
                    {new Date(order.orderDate).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    {order.scheduledDate
                      ? new Date(order.scheduledDate).toLocaleString()
                      : '—'}
                  </TableCell>
                  <TableCell>
                    <Box display="flex" gap={1}>
                      <Tooltip title="View Details">
                        <IconButton
                          size="small"
                          onClick={() => setViewing(order)}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {order.orderStatus === 'PENDING' && (
                        <>
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => handleEdit(order)}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Send">
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleSend(order.orderId)}
                            >
                              <SendIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDelete(order.orderId)}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </>
                      )}
                      {order.orderStatus !== 'CANCELLED' && order.orderStatus !== 'COMPLETED' && (
                        <>
                          <Tooltip title="Cancel">
                            <IconButton
                              size="small"
                              color="warning"
                              onClick={() => {
                                setOrderToCancel(order);
                                setShowCancelDialog(true);
                              }}
                            >
                              <CancelIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {(order.orderStatus === 'PENDING' || order.orderStatus === 'SENT') && (
                            <Tooltip title="Reschedule">
                              <IconButton
                                size="small"
                                color="info"
                                onClick={() => {
                                  setOrderToReschedule(order);
                                  setRescheduleDate(order.scheduledDate || new Date().toISOString().slice(0, 16));
                                  setShowRescheduleDialog(true);
                                }}
                              >
                                <ScheduleIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Lab Order Form Dialog */}
      <Dialog
        open={showForm}
        onClose={() => {
          setShowForm(false);
          setEditing(null);
          resetForm();
        }}
        maxWidth="md"
        fullWidth
      >
        <form onSubmit={handleSubmit}>
          <DialogTitle>
            {editing ? 'Edit Lab Order' : 'New Lab Order'}
          </DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} md={8}>
                <TextField
                  fullWidth
                  label="Test Name"
                  required
                  value={formData.testName}
                  onChange={(e) => setFormData({ ...formData, testName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="LOINC Code"
                  value={formData.loincCode}
                  onChange={(e) => setFormData({ ...formData, loincCode: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Test Category"
                  value={formData.testCategory}
                  onChange={(e) => setFormData({ ...formData, testCategory: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Test Type"
                  value={formData.testType}
                  onChange={(e) => setFormData({ ...formData, testType: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Priority</InputLabel>
                  <Select
                    value={formData.priority}
                    label="Priority"
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value as any })}
                  >
                    <MenuItem value="ROUTINE">Routine</MenuItem>
                    <MenuItem value="STAT">Stat</MenuItem>
                    <MenuItem value="ASAP">ASAP</MenuItem>
                    <MenuItem value="TIMED">Timed</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Is Test Panel</InputLabel>
                  <Select
                    value={formData.isTestPanel ? 'true' : 'false'}
                    label="Is Test Panel"
                    onChange={(e) => setFormData({ ...formData, isTestPanel: e.target.value === 'true' })}
                  >
                    <MenuItem value="false">No</MenuItem>
                    <MenuItem value="true">Yes</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              {formData.isTestPanel && (
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Panel Name"
                    value={formData.panelName}
                    onChange={(e) => setFormData({ ...formData, panelName: e.target.value })}
                  />
                </Grid>
              )}
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Clinical Indication"
                  multiline
                  rows={3}
                  value={formData.clinicalIndication}
                  onChange={(e) => setFormData({ ...formData, clinicalIndication: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Special Instructions"
                  multiline
                  rows={2}
                  value={formData.specialInstructions}
                  onChange={(e) => setFormData({ ...formData, specialInstructions: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Fasting Required</InputLabel>
                  <Select
                    value={formData.fastingRequired ? 'true' : 'false'}
                    label="Fasting Required"
                    onChange={(e) => setFormData({ ...formData, fastingRequired: e.target.value === 'true' })}
                  >
                    <MenuItem value="false">No</MenuItem>
                    <MenuItem value="true">Yes</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              {formData.fastingRequired && (
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Patient Preparation Instructions"
                    multiline
                    rows={2}
                    value={formData.patientPreparationInstructions}
                    onChange={(e) => setFormData({ ...formData, patientPreparationInstructions: e.target.value })}
                  />
                </Grid>
              )}
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Laboratory Name"
                  value={formData.laboratoryName}
                  onChange={(e) => setFormData({ ...formData, laboratoryName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Ordering Provider Name"
                  value={formData.orderingProviderName}
                  onChange={(e) => setFormData({ ...formData, orderingProviderName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Scheduled Date/Time"
                  type="datetime-local"
                  inputProps={{ min: scheduleMin }}
                  value={formData.scheduledDate}
                  onChange={(e) => setFormData({ ...formData, scheduledDate: e.target.value })}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => {
              setShowForm(false);
              setEditing(null);
              resetForm();
            }}>
              Cancel
            </Button>
            <Button type="submit" variant="contained">
              {editing ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* View Details Dialog */}
      <Dialog
        open={!!viewing}
        onClose={() => setViewing(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Lab Order Details</DialogTitle>
        <DialogContent>
          {viewing && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Order Number</Typography>
                <Typography variant="body1">{viewing.orderNumber}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                <Chip
                  label={viewing.orderStatus}
                  size="small"
                  color={getStatusColor(viewing.orderStatus) as any}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Test Name</Typography>
                <Typography variant="body1">{viewing.testName}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">LOINC Code</Typography>
                <Typography variant="body1">{viewing.loincCode || '-'}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Priority</Typography>
                <Chip
                  label={viewing.priority}
                  size="small"
                  color={getPriorityColor(viewing.priority) as any}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Order Date</Typography>
                <Typography variant="body1">
                  {new Date(viewing.orderDate).toLocaleString()}
                </Typography>
              </Grid>
              {viewing.scheduledDate && (
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Scheduled Date/Time</Typography>
                  <Typography variant="body1">
                    {new Date(viewing.scheduledDate).toLocaleString()}
                  </Typography>
                </Grid>
              )}
              {viewing.clinicalIndication && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Clinical Indication</Typography>
                  <Typography variant="body1">{viewing.clinicalIndication}</Typography>
                </Grid>
              )}
              {viewing.specialInstructions && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Special Instructions</Typography>
                  <Typography variant="body1">{viewing.specialInstructions}</Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          {viewing && (
            <>
              <Button onClick={() => handleGenerateHL7V2(viewing.orderId)}>HL7 V2 ORM</Button>
              <Button onClick={() => handleGenerateHL7Fhir(viewing.orderId)}>HL7 FHIR</Button>
            </>
          )}
          <Button onClick={() => setViewing(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Cancel Order Dialog */}
      <Dialog
        open={showCancelDialog}
        onClose={() => {
          setShowCancelDialog(false);
          setOrderToCancel(null);
          setCancelReason('');
        }}
      >
        <DialogTitle>Cancel Lab Order</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Cancellation Reason"
            multiline
            rows={3}
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setShowCancelDialog(false);
            setOrderToCancel(null);
            setCancelReason('');
          }}>
            Cancel
          </Button>
          <Button onClick={handleCancel} variant="contained" color="error">
            Confirm Cancel
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reschedule Order Dialog */}
      <Dialog
        open={showRescheduleDialog}
        onClose={() => {
          setShowRescheduleDialog(false);
          setOrderToReschedule(null);
          setRescheduleDate('');
        }}
      >
        <DialogTitle>Reschedule Lab Order</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="New Scheduled Date/Time"
            type="datetime-local"
            inputProps={{ min: scheduleMin }}
            value={rescheduleDate}
            onChange={(e) => setRescheduleDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setShowRescheduleDialog(false);
            setOrderToReschedule(null);
            setRescheduleDate('');
          }}>
            Cancel
          </Button>
          <Button onClick={handleReschedule} variant="contained" color="primary">
            Reschedule
          </Button>
        </DialogActions>
      </Dialog>

      {/* HL7 Message Dialog */}
      <Dialog
        open={showHL7Dialog}
        onClose={() => {
          setShowHL7Dialog(false);
          setHl7Message(null);
          setHl7MessageType('');
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          {hl7MessageType} Message
          <Button
            startIcon={<DownloadIcon />}
            onClick={handleDownloadHL7}
            variant="outlined"
            size="small"
            sx={{ ml: 2 }}
          >
            Download
          </Button>
        </DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={15}
            value={hl7Message || ''}
            variant="outlined"
            InputProps={{
              readOnly: true,
              sx: { fontFamily: 'monospace', fontSize: '0.875rem' }
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setShowHL7Dialog(false);
            setHl7Message(null);
            setHl7MessageType('');
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LabOrderManagement;
