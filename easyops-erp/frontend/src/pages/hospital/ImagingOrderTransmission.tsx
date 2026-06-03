import React, { useState } from 'react';
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
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  Divider,
} from '@mui/material';
import {
  Send as SendIcon,
  Refresh as RefreshIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { ImagingOrder } from '../../services/hospitalService';
import './Hospital.css';

interface ImagingOrderTransmissionProps {
  order: ImagingOrder;
  onUpdate?: () => void;
}

const ImagingOrderTransmission: React.FC<ImagingOrderTransmissionProps> = ({ order, onUpdate }) => {
  const { enqueueSnackbar } = useSnackbar();
  const enableTransmitFeature = false;
  const [transmitting, setTransmitting] = useState(false);
  const [submittingWorklist, setSubmittingWorklist] = useState(false);
  const [worklistDialogOpen, setWorklistDialogOpen] = useState(false);
  const [queryDialogOpen, setQueryDialogOpen] = useState(false);
  const [worklistResults, setWorklistResults] = useState<any[]>([]);
  const [queryModality, setQueryModality] = useState<string>('');
  const [queryDate, setQueryDate] = useState<string>('');

  const handleTransmitToRISPACS = async () => {
    try {
      setTransmitting(true);
      const response = await hospitalService.transmitToRISPACS(order.orderId);
      if (response.data.success) {
        enqueueSnackbar('Order successfully transmitted to RIS/PACS', { variant: 'success' });
      } else {
        enqueueSnackbar(`Transmission failed: ${response.data.message}`, { variant: 'warning' });
      }
      if (onUpdate) onUpdate();
    } catch (err: any) {
      console.error('Failed to transmit to RIS/PACS:', err);
      enqueueSnackbar('Failed to transmit to RIS/PACS', { variant: 'error' });
    } finally {
      setTransmitting(false);
    }
  };

  const handleSubmitToWorklist = async () => {
    try {
      setSubmittingWorklist(true);
      const response = await hospitalService.submitToDICOMWorklist(order.orderId);
      if (response.data.success) {
        enqueueSnackbar('Order successfully submitted to DICOM worklist', { variant: 'success' });
        setWorklistDialogOpen(false);
      } else {
        enqueueSnackbar(`Worklist submission failed: ${response.data.message}`, { variant: 'warning' });
      }
      if (onUpdate) onUpdate();
    } catch (err: any) {
      console.error('Failed to submit to DICOM worklist:', err);
      enqueueSnackbar('Failed to submit to DICOM worklist', { variant: 'error' });
    } finally {
      setSubmittingWorklist(false);
    }
  };

  const handleQueryWorklist = async () => {
    try {
      const response = await hospitalService.queryDICOMWorklist(
        queryModality,
        queryDate || undefined
      );
      setWorklistResults(response.data || []);
      enqueueSnackbar(`Found ${response.data?.length || 0} worklist entries`, { variant: 'success' });
    } catch (err: any) {
      console.error('Failed to query DICOM worklist:', err);
      enqueueSnackbar('Failed to query DICOM worklist', { variant: 'error' });
      setWorklistResults([]);
    }
  };

  const getTransmissionStatusColor = (status?: string) => {
    switch (status) {
      case 'SENT':
      case 'SENT_MANUAL':
        return 'success';
      case 'TRANSMISSION_ERROR':
      case 'HTTP_ERROR':
        return 'error';
      case 'DISABLED':
      case 'CONFIGURATION_ERROR':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            RIS/PACS Transmission & DICOM Worklist
          </Typography>
          <Divider sx={{ mb: 2 }} />

          {enableTransmitFeature && (
            <>
              {/* RIS/PACS Transmission Section */}
              <Box sx={{ mb: 3 }}>
                <Typography variant="subtitle1" gutterBottom>
                  RIS/PACS Transmission
                </Typography>
                <Grid container spacing={2} alignItems="center">
                  <Grid item xs={12} sm={6}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Transmission Status
                      </Typography>
                      <Box sx={{ mt: 0.5 }}>
                        {order.transmissionStatus ? (
                          <Chip
                            label={order.transmissionStatus}
                            size="small"
                            color={getTransmissionStatusColor(order.transmissionStatus) as any}
                          />
                        ) : (
                          <Chip label="Not Transmitted" size="small" color="default" />
                        )}
                      </Box>
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Transmission Method
                      </Typography>
                      <Typography variant="body2">
                        {order.transmissionMethod || 'N/A'}
                      </Typography>
                    </Box>
                  </Grid>
                  {order.transmissionDate && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">
                        Transmission Date
                      </Typography>
                      <Typography variant="body2">
                        {new Date(order.transmissionDate).toLocaleString()}
                      </Typography>
                    </Grid>
                  )}
                  <Grid item xs={12}>
                    <Button
                      variant="outlined"
                      startIcon={<SendIcon />}
                      onClick={handleTransmitToRISPACS}
                      disabled={transmitting}
                      size="small"
                    >
                      {transmitting ? 'Transmitting...' : 'Transmit to RIS/PACS'}
                    </Button>
                    {transmitting && <LinearProgress sx={{ mt: 1 }} />}
                  </Grid>
                </Grid>
              </Box>

              <Divider sx={{ my: 2 }} />
            </>
          )}

          {/* DICOM Worklist Section */}
          <Box>
            <Typography variant="subtitle1" gutterBottom>
              DICOM Modality Worklist
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<SendIcon />}
                onClick={handleSubmitToWorklist}
                disabled={submittingWorklist || !order.scheduledDate}
                size="small"
              >
                {submittingWorklist ? 'Submitting...' : 'Submit to Worklist'}
              </Button>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => setQueryDialogOpen(true)}
                size="small"
              >
                Query Worklist
              </Button>
            </Box>
            {!order.scheduledDate && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Order must be scheduled before submitting to DICOM worklist.
              </Alert>
            )}
            {submittingWorklist && <LinearProgress sx={{ mt: 1 }} />}
          </Box>
        </CardContent>
      </Card>

      {/* Query Worklist Dialog */}
      <Dialog open={queryDialogOpen} onClose={() => setQueryDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">Query DICOM Worklist</Typography>
            <IconButton onClick={() => setQueryDialogOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Modality"
                fullWidth
                value={queryModality}
                onChange={(e) => setQueryModality(e.target.value)}
                placeholder="CT, MRI, XRAY, etc."
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Scheduled Date"
                type="date"
                fullWidth
                value={queryDate}
                onChange={(e) => setQueryDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <Button variant="contained" onClick={handleQueryWorklist} fullWidth>
                Query
              </Button>
            </Grid>
            {worklistResults.length > 0 && (
              <Grid item xs={12}>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Patient Name</TableCell>
                        <TableCell>Patient ID</TableCell>
                        <TableCell>Modality</TableCell>
                        <TableCell>Study Date</TableCell>
                        <TableCell>Accession Number</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {worklistResults.map((entry, index) => (
                        <TableRow key={index}>
                          <TableCell>{entry.patientName || 'N/A'}</TableCell>
                          <TableCell>{entry.patientId || 'N/A'}</TableCell>
                          <TableCell>{entry.modality || 'N/A'}</TableCell>
                          <TableCell>{entry.studyDate || 'N/A'}</TableCell>
                          <TableCell>{entry.accessionNumber || 'N/A'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setQueryDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ImagingOrderTransmission;
