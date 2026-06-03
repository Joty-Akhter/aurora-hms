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
  CompareArrows as CompareIcon,
  TrendingUp as TrendIcon,
  Link as LinkIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabResult } from '../../services/hospitalService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultDetail: React.FC = () => {
  const { id, resultId } = useParams<{ id: string; resultId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [result, setResult] = useState<LabResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [linkedMedications, setLinkedMedications] = useState<any[]>([]);
  const [showLinkMedicationDialog, setShowLinkMedicationDialog] = useState(false);
  const [availableMedications, setAvailableMedications] = useState<any[]>([]);
  const [selectedMedicationId, setSelectedMedicationId] = useState<string>('');
  
  useEffect(() => {
    if (resultId) {
      loadResult();
    }
  }, [resultId]);
  
  const loadResult = async () => {
    if (!resultId) return;
    try {
      setLoading(true);
      setError(null);
      const [resultResponse, medicationsResponse] = await Promise.all([
        hospitalService.getLabResultDetail(resultId),
        hospitalService.getLinkedMedicationsForResult(resultId).catch(() => ({ data: [] }))
      ]);
      setResult(resultResponse.data);
      setLinkedMedications(medicationsResponse.data);
    } catch (err: any) {
      console.error('Failed to load lab result:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load lab result'));
      enqueueSnackbar('Failed to load lab result', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleLinkMedication = async () => {
    if (!resultId || !selectedMedicationId) return;
    try {
      await hospitalService.linkResultToMedication(resultId, selectedMedicationId);
      setShowLinkMedicationDialog(false);
      setSelectedMedicationId('');
      // Reload medications
      const medicationsResponse = await hospitalService.getLinkedMedicationsForResult(resultId);
      setLinkedMedications(medicationsResponse.data);
      enqueueSnackbar('Medication linked successfully', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to link medication'), { variant: 'error' });
    }
  };

  const handleUnlinkMedication = async (medicationId: string) => {
    if (!resultId) return;
    if (!window.confirm('Are you sure you want to unlink this medication?')) {
      return;
    }
    try {
      await hospitalService.unlinkResultFromMedication(resultId, medicationId);
      // Reload medications
      const medicationsResponse = await hospitalService.getLinkedMedicationsForResult(resultId);
      setLinkedMedications(medicationsResponse.data);
      enqueueSnackbar('Medication unlinked successfully', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to unlink medication'), { variant: 'error' });
    }
  };

  const loadAvailableMedications = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getActiveMedications(id);
      setAvailableMedications(response.data);
    } catch (err: any) {
      console.error('Failed to load medications:', err);
    }
  };
  
  const getAbnormalFlagLabel = (flag?: string) => {
    switch (flag) {
      case 'H': return 'High';
      case 'L': return 'Low';
      case 'A': return 'Abnormal';
      case 'C': return 'Critical';
      case 'N': return 'Normal';
      default: return '-';
    }
  };
  
  const getAbnormalFlagColor = (flag?: string) => {
    switch (flag) {
      case 'H': return 'error';
      case 'L': return 'warning';
      case 'A': return 'error';
      case 'C': return 'error';
      case 'N': return 'success';
      default: return 'default';
    }
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  if (error || !result) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error || 'Lab result not found'}</Alert>
        <Button
          startIcon={<BackIcon />}
          onClick={() => navigate(`/hospital/patients/${id}/lab-results`)}
          sx={{ mt: 2 }}
        >
          Back to Results
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
            onClick={() => navigate(`/hospital/patients/${id}/lab-results`)}
          >
            Back
          </Button>
          <Typography variant="h4">Lab Result Details</Typography>
        </Box>
        <Box display="flex" gap={1}>
          {result.resultType === 'NUMERIC' && (
            <>
              <Button
                variant="outlined"
                startIcon={<CompareIcon />}
                onClick={() => navigate(`/hospital/patients/${id}/lab-results/${resultId}/compare`)}
              >
                Compare
              </Button>
              <Button
                variant="outlined"
                startIcon={<TrendIcon />}
                onClick={() => navigate(`/hospital/patients/${id}/lab-results/trend?loincCode=${result.loincCode}`)}
              >
                View Trend
              </Button>
            </>
          )}
          <Button
            variant="outlined"
            startIcon={<LinkIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/lab-results/${resultId}/correlated`)}
          >
            Correlated Results
          </Button>
        </Box>
      </Box>
      
      <Grid container spacing={3}>
        {/* Main Result Information */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Test Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Test Name</Typography>
                  <Typography variant="body1" fontWeight="bold">{result.testName}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">LOINC Code</Typography>
                  <Typography variant="body1">{result.loincCode}</Typography>
                </Grid>
                {result.testCategory && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Category</Typography>
                    <Typography variant="body1">{result.testCategory}</Typography>
                  </Grid>
                )}
                {result.testType && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Test Type</Typography>
                    <Typography variant="body1">{result.testType}</Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Result</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Result Value</Typography>
                  <Typography variant="h5" fontWeight="bold" color={result.isCriticalValue ? 'error' : 'inherit'}>
                    {result.resultValue || result.resultValueNumeric || '-'}
                    {result.resultUnits && ` ${result.resultUnits}`}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Reference Range</Typography>
                  <Typography variant="body1">
                    {result.referenceRangeLow !== undefined && result.referenceRangeHigh !== undefined
                      ? `${result.referenceRangeLow} - ${result.referenceRangeHigh} ${result.referenceRangeUnits || ''}`
                      : result.referenceRangeText || '-'}
                  </Typography>
                </Grid>
                {result.abnormalFlag && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Abnormal Flag</Typography>
                    <Chip
                      label={getAbnormalFlagLabel(result.abnormalFlag)}
                      color={getAbnormalFlagColor(result.abnormalFlag) as any}
                    />
                  </Grid>
                )}
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                  <Box display="flex" gap={1} flexWrap="wrap">
                    <Chip
                      label={result.resultStatus}
                      color={result.resultStatus === 'FINAL' ? 'success' : 'default'}
                    />
                    {result.isCriticalValue && (
                      <Chip label="Critical" color="error" />
                    )}
                    {result.isPanicValue && (
                      <Chip label="Panic Value" color="error" />
                    )}
                    {result.isDeltaCheck && (
                      <Chip label="Delta Check" color="warning" />
                    )}
                  </Box>
                </Grid>
                {result.resultInterpretation && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Interpretation</Typography>
                    <Typography variant="body1">{result.resultInterpretation}</Typography>
                  </Grid>
                )}
                {(result.clinicalSignificance || result.clinicalSignificanceLevel) && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Clinical Significance</Typography>
                    <Box display="flex" gap={1} flexWrap="wrap" alignItems="center">
                      {result.clinicalSignificance && (
                        <Chip
                          label={result.clinicalSignificance.replace(/_/g, ' ')}
                          color={
                            result.clinicalSignificance === 'CRITICAL' ? 'error' :
                            result.clinicalSignificance === 'ABNORMAL' ? 'warning' :
                            result.clinicalSignificance === 'SIGNIFICANT_CHANGE' ? 'warning' :
                            'success'
                          }
                          size="small"
                        />
                      )}
                      {result.clinicalSignificanceLevel && (
                        <Chip
                          label={`Level: ${result.clinicalSignificanceLevel}`}
                          color={
                            result.clinicalSignificanceLevel === 'CRITICAL' ? 'error' :
                            result.clinicalSignificanceLevel === 'HIGH' ? 'warning' :
                            result.clinicalSignificanceLevel === 'MEDIUM' ? 'info' :
                            'default'
                          }
                          size="small"
                          variant="outlined"
                        />
                      )}
                    </Box>
                  </Grid>
                )}
                {result.interpretationNotes && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Interpretation Notes</Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                      {result.interpretationNotes}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          {/* Specimen Information */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Specimen Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                {result.specimenType && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Specimen Type</Typography>
                    <Typography variant="body1">{result.specimenType}</Typography>
                  </Grid>
                )}
                {result.specimenSource && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Specimen Source</Typography>
                    <Typography variant="body1">{result.specimenSource}</Typography>
                  </Grid>
                )}
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Collection Date</Typography>
                  <Typography variant="body1">
                    {new Date(result.specimenCollectionDate).toLocaleString()}
                  </Typography>
                </Grid>
                {result.specimenReceivedDate && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Received Date</Typography>
                    <Typography variant="body1">
                      {new Date(result.specimenReceivedDate).toLocaleString()}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          {/* Laboratory Information */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Laboratory Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Laboratory Name</Typography>
                  <Typography variant="body1">{result.performingLaboratoryName}</Typography>
                </Grid>
                {result.performingTechnologist && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Technologist</Typography>
                    <Typography variant="body1">{result.performingTechnologist}</Typography>
                  </Grid>
                )}
                {result.reviewingPhysician && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="text.secondary">Reviewing Physician</Typography>
                    <Typography variant="body1">{result.reviewingPhysician}</Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
        </Grid>
        
        {/* Sidebar Information */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Timeline</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Order Date</Typography>
                  <Typography variant="body2">
                    {result.orderDate ? new Date(result.orderDate).toLocaleString() : '-'}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Result Date</Typography>
                  <Typography variant="body2">
                    {new Date(result.resultDate).toLocaleString()}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Reported Date</Typography>
                  <Typography variant="body2">
                    {new Date(result.resultReportedDate).toLocaleString()}
                  </Typography>
                </Grid>
                {result.resultVerifiedDate && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="text.secondary">Verified Date</Typography>
                    <Typography variant="body2">
                      {new Date(result.resultVerifiedDate).toLocaleString()}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </CardContent>
          </Card>
          
          {/* Comments */}
          {(result.laboratoryComments || result.providerComments || result.resultNotes) && (
            <Card sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Comments</Typography>
                <Divider sx={{ mb: 2 }} />
                {result.laboratoryComments && (
                  <Box mb={2}>
                    <Typography variant="subtitle2" color="text.secondary">Laboratory Comments</Typography>
                    <Typography variant="body2">{result.laboratoryComments}</Typography>
                  </Box>
                )}
                {result.providerComments && (
                  <Box mb={2}>
                    <Typography variant="subtitle2" color="text.secondary">Provider Comments</Typography>
                    <Typography variant="body2">{result.providerComments}</Typography>
                  </Box>
                )}
                {result.resultNotes && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Notes</Typography>
                    <Typography variant="body2">{result.resultNotes}</Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          )}

          {/* Linked Medications */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Linked Medications ({linkedMedications.length})</Typography>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => {
                    loadAvailableMedications();
                    setShowLinkMedicationDialog(true);
                  }}
                >
                  + Link Medication
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />
              {linkedMedications.length > 0 ? (
                <Box>
                  {linkedMedications.map((med: any) => (
                    <Box
                      key={med.linkId || med.medicationId}
                      sx={{
                        p: 2,
                        mb: 1,
                        bgcolor: 'grey.50',
                        borderRadius: 1,
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                      }}
                    >
                      <Box>
                        <Typography variant="body1" fontWeight="bold">
                          {med.medicationName || med.prescriptionName || 'Unknown Medication'}
                        </Typography>
                        {med.genericName && (
                          <Typography variant="caption" color="text.secondary">
                            Generic: {med.genericName}
                          </Typography>
                        )}
                        {med.dosage && (
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                            Dosage: {med.dosage}
                          </Typography>
                        )}
                      </Box>
                      <Button
                        size="small"
                        color="error"
                        onClick={() => handleUnlinkMedication(med.medicationId || med.prescriptionId)}
                      >
                        Unlink
                      </Button>
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                  No medications linked to this result
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Link Medication Dialog */}
      {showLinkMedicationDialog && portalLayoutOverlay(
        <Box
          className={LAYOUT_OVERLAY_DETECT_CLASS}
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            bgcolor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: LAYOUT_OVERLAY_ROOT_Z,
          }}
        >
          <Card sx={{ maxWidth: 500, width: '90%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Link Medication to Lab Result</Typography>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>Medication *</Typography>
                <select
                  required
                  value={selectedMedicationId}
                  onChange={(e) => setSelectedMedicationId(e.target.value)}
                  style={{ width: '100%', padding: '8px', fontSize: '14px' }}
                >
                  <option value="">Select a medication...</option>
                  {availableMedications.map((med) => (
                    <option key={med.medicationId} value={med.medicationId}>
                      {med.medicationName} {med.genericName ? `(${med.genericName})` : ''}
                    </option>
                  ))}
                </select>
              </Box>
              <Box display="flex" gap={2} justifyContent="flex-end">
                <Button
                  variant="outlined"
                  onClick={() => {
                    setShowLinkMedicationDialog(false);
                    setSelectedMedicationId('');
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  onClick={handleLinkMedication}
                  disabled={!selectedMedicationId}
                >
                  Link Medication
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Box>
      )}
    </Box>
  );
};

export default LabResultDetail;
