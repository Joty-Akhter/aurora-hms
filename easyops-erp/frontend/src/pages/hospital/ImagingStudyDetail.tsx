import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Image as ImageIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Print as PrintIcon,
  PictureAsPdf as PdfIcon,
  Notifications as NotificationIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { ImagingStudy } from '../../services/hospitalService';
import DICOMImageViewer from './DICOMImageViewer';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ImagingStudyDetail: React.FC = () => {
  const { id, studyId } = useParams<{ id?: string; studyId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [study, setStudy] = useState<ImagingStudy | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewerOpen, setViewerOpen] = useState(false);
  
  useEffect(() => {
    if (studyId) {
      loadStudy();
    }
  }, [studyId]);
  
  const loadStudy = async () => {
    if (!studyId) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getImagingStudyById(studyId);
      setStudy(response.data);
    } catch (err: any) {
      console.error('Failed to load imaging study:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load imaging study'));
      enqueueSnackbar('Failed to load imaging study', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const getModalityColor = (modality?: string) => {
    switch (modality) {
      case 'CT': return 'primary';
      case 'MRI': return 'secondary';
      case 'XRAY': return 'default';
      case 'ULTRASOUND': return 'info';
      case 'MAMMOGRAPHY': return 'warning';
      default: return 'default';
    }
  };
  
  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'FINAL': return 'success';
      case 'PRELIMINARY': return 'warning';
      case 'AMENDED': return 'info';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };
  
  const handlePrint = async () => {
    if (!studyId) return;
    
    try {
      const response = await hospitalService.getPrintableReport(studyId);
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(response.data);
        printWindow.document.close();
        printWindow.onload = () => {
          printWindow.print();
        };
      }
    } catch (err: any) {
      console.error('Failed to print report:', err);
      enqueueSnackbar('Failed to print report', { variant: 'error' });
    }
  };
  
  const handleExportPdf = async () => {
    if (!studyId) return;
    
    try {
      const response = await hospitalService.exportReportToPdf(studyId);
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Imaging_Report_${study?.accessionNumber || studyId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      enqueueSnackbar('Report exported successfully', { variant: 'success' });
    } catch (err: any) {
      console.error('Failed to export PDF:', err);
      enqueueSnackbar('Failed to export PDF', { variant: 'error' });
    }
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  if (error || !study) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error || 'Imaging study not found'}</Alert>
        <Button
          startIcon={<BackIcon />}
          onClick={() => navigate(`/hospital/patients/${id}/imaging-studies`)}
          sx={{ mt: 2 }}
        >
          Back to Studies
        </Button>
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
          <Typography variant="h4">Imaging Study Details</Typography>
        </Box>
        <Box display="flex" gap={1}>
          <Button
            variant="outlined"
            startIcon={<PrintIcon />}
            onClick={handlePrint}
          >
            Print
          </Button>
          <Button
            variant="outlined"
            startIcon={<PdfIcon />}
            onClick={handleExportPdf}
          >
            Export PDF
          </Button>
        <Box display="flex" gap={1}>
          {study.imagesAvailable && studyId && (
            <Button
              variant="outlined"
              startIcon={<ImageIcon />}
              onClick={() => setViewerOpen(true)}
            >
              View Images
            </Button>
          )}
          <Button
            variant="outlined"
            startIcon={<NotificationIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/imaging-alerts`)}
          >
            View Alerts
          </Button>
        </Box>
        </Box>
      </Box>
      
      {study.hasCriticalFindings && !study.isCriticalFindingAcknowledged && (
        <Alert severity="error" sx={{ mb: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Box>
              <Typography variant="subtitle2" fontWeight="bold">
                Critical Finding - Requires Acknowledgment
              </Typography>
              {study.criticalFindingResponse && (
                <Typography variant="body2" sx={{ mt: 1 }}>
                  {study.criticalFindingResponse}
                </Typography>
              )}
            </Box>
            <Button
              variant="contained"
              color="error"
              onClick={async () => {
                try {
                  await hospitalService.acknowledgeCriticalFinding(study.studyId, 'Acknowledged');
                  enqueueSnackbar('Critical finding acknowledged', { variant: 'success' });
                  loadStudy();
                } catch (err: any) {
                  enqueueSnackbar('Failed to acknowledge critical finding', { variant: 'error' });
                }
              }}
            >
              Acknowledge
            </Button>
          </Box>
        </Alert>
      )}
      
      <Grid container spacing={3}>
        {/* Study Information */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Study Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Study Name</Typography>
                  <Typography variant="body1" fontWeight="bold">{study.studyName}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Modality</Typography>
                  <Chip
                    label={study.studyModality}
                    size="small"
                    color={getModalityColor(study.studyModality) as any}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Accession Number</Typography>
                  <Typography variant="body1" fontFamily="monospace">{study.accessionNumber}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Study Number</Typography>
                  <Typography variant="body1" fontFamily="monospace">{study.studyNumber}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">CPT Code</Typography>
                  <Typography variant="body1">{study.cptCode}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                  <Box display="flex" gap={1} flexWrap="wrap">
                    <Chip
                      label={study.studyStatus}
                      size="small"
                      color={getStatusColor(study.studyStatus) as any}
                    />
                    {study.isFinal && (
                      <Chip label="Final" size="small" color="success" variant="outlined" />
                    )}
                    {study.isPreliminary && (
                      <Chip label="Preliminary" size="small" color="warning" variant="outlined" />
                    )}
                    {study.isAmended && (
                      <Chip label="Amended" size="small" color="info" variant="outlined" />
                    )}
                    {study.isAddendum && (
                      <Chip label="Addendum" size="small" color="info" variant="outlined" />
                    )}
                  </Box>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Body Part Examined</Typography>
                  <Typography variant="body1">
                    {study.bodyPartExamined}
                    {study.laterality && ` (${study.laterality})`}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Study Date</Typography>
                  <Typography variant="body1">
                    {new Date(study.studyDate).toLocaleString()}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Completion Date</Typography>
                  <Typography variant="body1">
                    {new Date(study.studyCompletionDate).toLocaleString()}
                  </Typography>
                </Grid>
                {study.contrastUsed && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Contrast</Typography>
                    <Typography variant="body1">
                      {study.contrastType || 'Yes'}
                    </Typography>
                  </Grid>
                )}
                {study.numberOfImages && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Number of Images</Typography>
                    <Typography variant="body1">{study.numberOfImages}</Typography>
                  </Grid>
                )}
                {study.numberOfSeries && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Number of Series</Typography>
                    <Typography variant="body1">{study.numberOfSeries}</Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          {/* Report Content */}
          {(study.findings || study.impressionConclusion || study.recommendations) && (
            <Card sx={{ mt: 3 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Report Content</Typography>
                <Divider sx={{ mb: 2 }} />
                
                {study.clinicalHistory && (
                  <Box sx={{ mb: 3 }}>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      Clinical History
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                      {study.clinicalHistory}
                    </Typography>
                  </Box>
                )}
                
                {study.techniqueDescription && (
                  <Box sx={{ mb: 3 }}>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      Technique
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                      {study.techniqueDescription}
                    </Typography>
                  </Box>
                )}
                
                {study.findings && (
                  <Box sx={{ mb: 3 }}>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      Findings
                    </Typography>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
                      <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                        {study.findings}
                      </Typography>
                    </Paper>
                  </Box>
                )}
                
                {study.impressionConclusion && (
                  <Box sx={{ mb: 3 }}>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      Impression/Conclusion
                    </Typography>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: 'primary.50' }}>
                      <Typography variant="body2" fontWeight="bold" sx={{ whiteSpace: 'pre-wrap' }}>
                        {study.impressionConclusion}
                      </Typography>
                    </Paper>
                  </Box>
                )}
                
                {study.recommendations && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                      Recommendations
                    </Typography>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: 'warning.50' }}>
                      <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                        {study.recommendations}
                      </Typography>
                    </Paper>
                  </Box>
                )}
              </CardContent>
            </Card>
          )}
        </Grid>
        
        {/* Sidebar Information */}
        <Grid item xs={12} md={4}>
          {/* Radiologist Information */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Radiologist Information</Typography>
              <Divider sx={{ mb: 2 }} />
              {study.interpretingRadiologistName ? (
                <>
                  <Typography variant="subtitle2" color="text.secondary">Interpreting Radiologist</Typography>
                  <Typography variant="body1" fontWeight="bold" sx={{ mb: 1 }}>
                    {study.interpretingRadiologistName}
                  </Typography>
                  {study.interpretingRadiologistNpi && (
                    <>
                      <Typography variant="subtitle2" color="text.secondary">NPI</Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        {study.interpretingRadiologistNpi}
                      </Typography>
                    </>
                  )}
                  {study.interpretingRadiologistSpecialty && (
                    <>
                      <Typography variant="subtitle2" color="text.secondary">Specialty</Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        {study.interpretingRadiologistSpecialty}
                      </Typography>
                    </>
                  )}
                </>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Not specified
                </Typography>
              )}
              {study.preliminaryReadingBy && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" color="text.secondary">Preliminary Reading By</Typography>
                  <Typography variant="body2">{study.preliminaryReadingBy}</Typography>
                </>
              )}
              {study.reviewingRadiologist && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" color="text.secondary">Reviewing Radiologist</Typography>
                  <Typography variant="body2">{study.reviewingRadiologist}</Typography>
                </>
              )}
            </CardContent>
          </Card>
          
          {/* Report Status */}
          <Card sx={{ mt: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Report Status</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Box display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="subtitle2" color="text.secondary">Reviewed</Typography>
                    {study.isReviewed ? (
                      <CheckCircleIcon color="success" />
                    ) : (
                      <WarningIcon color="warning" />
                    )}
                  </Box>
                </Grid>
                {study.reviewedDate && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Reviewed Date</Typography>
                    <Typography variant="body2">
                      {new Date(study.reviewedDate).toLocaleString()}
                    </Typography>
                  </Grid>
                )}
                {study.reportDate && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Report Date</Typography>
                    <Typography variant="body2">
                      {new Date(study.reportDate).toLocaleString()}
                    </Typography>
                  </Grid>
                )}
                {study.reportFinalizedDate && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Finalized Date</Typography>
                    <Typography variant="body2">
                      {new Date(study.reportFinalizedDate).toLocaleString()}
                    </Typography>
                  </Grid>
                )}
                {study.hasCriticalFindings && (
                  <Grid item xs={12}>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Typography variant="subtitle2" color="text.secondary">Critical Finding</Typography>
                      <Chip label="Yes" size="small" color="error" />
                    </Box>
                    {study.isCriticalFindingAcknowledged && (
                      <>
                        <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 1 }}>
                          Acknowledged
                        </Typography>
                        <Typography variant="body2">
                          {study.criticalFindingAcknowledgedDate 
                            ? new Date(study.criticalFindingAcknowledgedDate).toLocaleString()
                            : 'Yes'}
                        </Typography>
                      </>
                    )}
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          {/* Equipment Information */}
          {(study.equipmentUsed || study.equipmentModel || study.techniqueProtocol) && (
            <Card sx={{ mt: 3 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Equipment & Technique</Typography>
                <Divider sx={{ mb: 2 }} />
                {study.equipmentUsed && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">Equipment</Typography>
                    <Typography variant="body2">{study.equipmentUsed}</Typography>
                  </Box>
                )}
                {study.equipmentModel && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">Model</Typography>
                    <Typography variant="body2">{study.equipmentModel}</Typography>
                  </Box>
                )}
                {study.techniqueProtocol && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Protocol</Typography>
                    <Typography variant="body2">{study.techniqueProtocol}</Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          )}
          
          {/* DICOM Information */}
          {study.pacsIntegrated && (
            <Card sx={{ mt: 3 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>DICOM Information</Typography>
                <Divider sx={{ mb: 2 }} />
                {study.dicomStudyInstanceUid && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">Study Instance UID</Typography>
                    <Typography variant="body2" fontFamily="monospace" fontSize="0.75rem">
                      {study.dicomStudyInstanceUid}
                    </Typography>
                  </Box>
                )}
                {study.dicomSeriesInstanceUid && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">Series Instance UID</Typography>
                    <Typography variant="body2" fontFamily="monospace" fontSize="0.75rem">
                      {study.dicomSeriesInstanceUid}
                    </Typography>
                  </Box>
                )}
                {study.imagesAvailable && (
                  <Box display="flex" alignItems="center" gap={1}>
                    <ImageIcon color="primary" />
                    <Typography variant="body2">Images Available</Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          )}
        </Grid>
      </Grid>
      
      {/* Integration Section */}
      <ImagingStudyIntegrationSection studyId={study.studyId} patientId={id || ''} />
      
      {/* DICOM Image Viewer */}
      {studyId && (
        <DICOMImageViewer
          studyId={studyId}
          open={viewerOpen}
          onClose={() => setViewerOpen(false)}
        />
      )}
    </Box>
  );
};

// Integration Section Component
const ImagingStudyIntegrationSection: React.FC<{ studyId: string; patientId: string }> = ({ studyId, patientId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [clinicalNotes, setClinicalNotes] = useState<any[]>([]);
  const [problems, setProblems] = useState<any[]>([]);
  const [medications, setMedications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    loadIntegrationData();
  }, [studyId]);
  
  const loadIntegrationData = async () => {
    try {
      setLoading(true);
      const [notesRes, problemsRes, medicationsRes] = await Promise.all([
        hospitalService.getClinicalNotesForImagingStudy(studyId),
        hospitalService.getProblemsForImagingStudy(studyId),
        hospitalService.getMedicationsForImagingStudy(studyId),
      ]);
      setClinicalNotes(notesRes.data || []);
      setProblems(problemsRes.data || []);
      setMedications(medicationsRes.data || []);
    } catch (err: any) {
      console.error('Failed to load integration data:', err);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>Clinical Integration</Typography>
        <Divider sx={{ mb: 2 }} />
        
        <Grid container spacing={3}>
          {/* Clinical Notes */}
          <Grid item xs={12} md={4}>
            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
              Linked Clinical Notes ({clinicalNotes.length})
            </Typography>
            {clinicalNotes.length > 0 ? (
              <Box>
                {clinicalNotes.map((link) => (
                  <Chip
                    key={link.linkId}
                    label={`Note ${link.targetId.substring(0, 8)}`}
                    size="small"
                    sx={{ mr: 1, mb: 1 }}
                  />
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No clinical notes linked
              </Typography>
            )}
          </Grid>
          
          {/* Problems */}
          <Grid item xs={12} md={4}>
            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
              Linked Problems ({problems.length})
            </Typography>
            {problems.length > 0 ? (
              <Box>
                {problems.map((link) => (
                  <Chip
                    key={link.linkId}
                    label={`Problem ${link.targetId.substring(0, 8)}`}
                    size="small"
                    sx={{ mr: 1, mb: 1 }}
                  />
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No problems linked
              </Typography>
            )}
          </Grid>
          
          {/* Medications */}
          <Grid item xs={12} md={4}>
            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
              Linked Medications ({medications.length})
            </Typography>
            {medications.length > 0 ? (
              <Box>
                {medications.map((link) => (
                  <Chip
                    key={link.linkId}
                    label={`Medication ${link.targetId.substring(0, 8)}`}
                    size="small"
                    sx={{ mr: 1, mb: 1 }}
                  />
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No medications linked
              </Typography>
            )}
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};

export default ImagingStudyDetail;
