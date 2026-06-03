import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  FormControl,
  FormControlLabel,
  FormHelperText,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  TextField,
  Typography,
  Divider,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
  Send as SendIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabResultRequest, LabOrder } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultReceiptForm: React.FC = () => {
  const { id, orderId } = useParams<{ id: string; orderId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [labOrder, setLabOrder] = useState<LabOrder | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  
  const [formData, setFormData] = useState<LabResultRequest>({
    orderId: orderId || '',
    patientId: id || '',
    testName: '',
    loincCode: '',
    testCategory: '',
    testType: '',
    resultType: 'NUMERIC',
    resultValue: '',
    resultValueNumeric: undefined,
    resultUnits: '',
    qualitativeResult: '',
    quantitativeResult: undefined,
    resultStatus: 'FINAL',
    referenceRangeLow: undefined,
    referenceRangeHigh: undefined,
    referenceRangeUnits: '',
    referenceRangeText: '',
    referenceRangeSource: '',
    ageSpecificRange: false,
    genderSpecificRange: false,
    abnormalFlag: undefined,
    isCriticalValue: false,
    isDeltaCheck: false,
    isPanicValue: false,
    resultInterpretation: '',
    specimenCollectionDate: new Date().toISOString(),
    specimenReceivedDate: new Date().toISOString(),
    resultDate: new Date().toISOString(),
    resultReportedDate: new Date().toISOString(),
    resultVerifiedDate: undefined,
    specimenType: '',
    specimenSource: '',
    specimenCollectionMethod: '',
    specimenId: '',
    specimenVolume: '',
    specimenQuality: '',
    performingLaboratoryName: '',
    laboratoryId: '',
    laboratoryNpi: '',
    laboratoryAddressLine1: '',
    laboratoryAddressLine2: '',
    laboratoryCity: '',
    laboratoryState: '',
    laboratoryZip: '',
    laboratoryPhone: '',
    performingTechnologist: '',
    reviewingPathologist: '',
    reviewingPhysician: '',
    laboratoryReferenceNumber: '',
    laboratoryComments: '',
    providerComments: '',
    resultNotes: '',
    methodUsed: '',
  });
  
  useEffect(() => {
    if (orderId) {
      loadLabOrder();
    }
  }, [orderId]);
  
  const loadLabOrder = async () => {
    if (!orderId) return;
    try {
      setLoading(true);
      const response = await hospitalService.getLabOrderById(orderId);
      const order = response.data;
      setLabOrder(order);
      
      // Pre-fill form with order data
      setFormData(prev => ({
        ...prev,
        orderId: order.orderId,
        patientId: order.patientId,
        testName: order.testName || '',
        loincCode: order.loincCode || '',
        testCategory: order.testCategory || '',
        testType: order.testType || '',
      }));
    } catch (err: any) {
      console.error('Failed to load lab order:', err);
      enqueueSnackbar('Failed to load lab order', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleInputChange = (field: keyof LabResultRequest, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));
  };
  
  const validateForm = (): boolean => {
    if (!formData.orderId) {
      setError('Lab order ID is required');
      return false;
    }
    if (!formData.testName) {
      setError('Test name is required');
      return false;
    }
    if (!formData.loincCode) {
      setError('LOINC code is required');
      return false;
    }
    if (!formData.resultType) {
      setError('Result type is required');
      return false;
    }
    if (formData.resultType === 'NUMERIC' && !formData.resultValueNumeric && !formData.resultValue) {
      setError('Result value is required for numeric results');
      return false;
    }
    if (!formData.specimenCollectionDate) {
      setError('Specimen collection date is required');
      return false;
    }
    if (!formData.resultDate) {
      setError('Result date is required');
      return false;
    }
    if (!formData.resultReportedDate) {
      setError('Result reported date is required');
      return false;
    }
    if (!formData.performingLaboratoryName) {
      setError('Performing laboratory name is required');
      return false;
    }
    setError(null);
    return true;
  };
  
  const handleSave = async (status: 'FINAL' | 'PRELIMINARY' = 'FINAL') => {
    if (!validateForm()) {
      return;
    }
    
    try {
      setSaving(true);
      const dataToSave: LabResultRequest = {
        ...formData,
        resultStatus: status,
      };
      
      const response = await hospitalService.createLabResult(dataToSave);
      
      enqueueSnackbar(
        `Lab result ${status === 'FINAL' ? 'saved and finalized' : 'saved as preliminary'} successfully`,
        { variant: 'success' }
      );
      
      // Navigate to result detail or list
      if (id) {
        navigate(`/hospital/patients/${id}/lab-results/${response.data.resultId}`);
      } else {
        navigate(`/hospital/lab-results/${response.data.resultId}`);
      }
    } catch (err: any) {
      console.error('Failed to save lab result:', err);
      const errorMessage = ehrApiErrorMessage(err, 'Failed to save lab result');
      setError(errorMessage);
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };
  
  const handleCancel = () => {
    if (id) {
      navigate(`/hospital/patients/${id}/lab-results`);
    } else {
      navigate('/hospital/lab-results');
    }
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  const formatDateTimeForInput = (dateString: string | undefined): string => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };
  
  const handleDateTimeChange = (field: keyof LabResultRequest, value: string) => {
    if (value) {
      handleInputChange(field, new Date(value).toISOString());
    }
  };
  
  return (
    <Box sx={{ p: 3 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h4">Receive Laboratory Result</Typography>
          <Box display="flex" gap={1}>
            <Button
              variant="outlined"
              startIcon={<CancelIcon />}
              onClick={handleCancel}
            >
              Cancel
            </Button>
            <Button
              variant="outlined"
              startIcon={<SaveIcon />}
              onClick={() => handleSave('PRELIMINARY')}
              disabled={saving}
            >
              Save as Preliminary
            </Button>
            <Button
              variant="contained"
              startIcon={<SendIcon />}
              onClick={() => handleSave('FINAL')}
              disabled={saving}
            >
              {saving ? <CircularProgress size={20} /> : 'Save and Finalize'}
            </Button>
          </Box>
        </Box>
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        
        {labOrder && (
          <Alert severity="info" sx={{ mb: 2 }}>
            <Typography variant="body2">
              <strong>Order:</strong> {labOrder.orderNumber} | 
              <strong> Test:</strong> {labOrder.testName} | 
              <strong> LOINC:</strong> {labOrder.loincCode}
            </Typography>
          </Alert>
        )}
        
        <Card>
          <CardContent>
            <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)} sx={{ mb: 3 }}>
              <Tab label="Result Identification" />
              <Tab label="Result Values" />
              <Tab label="Reference Ranges" />
              <Tab label="Specimen Information" />
              <Tab label="Laboratory Information" />
              <Tab label="Additional Information" />
            </Tabs>
            
            {/* Tab 1: Result Identification */}
            {activeTab === 0 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Order ID"
                    value={formData.orderId}
                    disabled
                    helperText="Lab order ID (from order)"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Result Number"
                    value={formData.resultNumber || ''}
                    onChange={(e) => handleInputChange('resultNumber', e.target.value)}
                    helperText="Auto-generated if left empty"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    label="Test Name"
                    value={formData.testName}
                    onChange={(e) => handleInputChange('testName', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    label="LOINC Code"
                    value={formData.loincCode}
                    onChange={(e) => handleInputChange('loincCode', e.target.value)}
                    helperText="Logical Observation Identifiers Names and Codes"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Test Category"
                    value={formData.testCategory || ''}
                    onChange={(e) => handleInputChange('testCategory', e.target.value)}
                    helperText="e.g., Hematology, Chemistry, Microbiology"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Test Type"
                    value={formData.testType || ''}
                    onChange={(e) => handleInputChange('testType', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth required>
                    <InputLabel>Result Status</InputLabel>
                    <Select
                      value={formData.resultStatus}
                      label="Result Status"
                      onChange={(e) => handleInputChange('resultStatus', e.target.value)}
                    >
                      <MenuItem value="FINAL">Final</MenuItem>
                      <MenuItem value="PRELIMINARY">Preliminary</MenuItem>
                      <MenuItem value="CORRECTED">Corrected</MenuItem>
                      <MenuItem value="AMENDED">Amended</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>
            )}
            
            {/* Tab 2: Result Values */}
            {activeTab === 1 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth required>
                    <InputLabel>Result Type</InputLabel>
                    <Select
                      value={formData.resultType}
                      label="Result Type"
                      onChange={(e) => handleInputChange('resultType', e.target.value)}
                    >
                      <MenuItem value="NUMERIC">Numeric</MenuItem>
                      <MenuItem value="TEXT">Text</MenuItem>
                      <MenuItem value="CODED">Coded</MenuItem>
                      <MenuItem value="STRUCTURED">Structured</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Result Units"
                    value={formData.resultUnits || ''}
                    onChange={(e) => handleInputChange('resultUnits', e.target.value)}
                    helperText="e.g., mg/dL, mmol/L, cells/μL"
                  />
                </Grid>
                
                {formData.resultType === 'NUMERIC' && (
                  <>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        type="number"
                        label="Numeric Result Value"
                        value={formData.resultValueNumeric || ''}
                        onChange={(e) => handleInputChange('resultValueNumeric', parseFloat(e.target.value) || undefined)}
                        inputProps={{ step: 'any' }}
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Result Value (Text)"
                        value={formData.resultValue || ''}
                        onChange={(e) => handleInputChange('resultValue', e.target.value)}
                        helperText="Alternative text representation"
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        type="number"
                        label="Quantitative Result"
                        value={formData.quantitativeResult || ''}
                        onChange={(e) => handleInputChange('quantitativeResult', parseFloat(e.target.value) || undefined)}
                        inputProps={{ step: 'any' }}
                      />
                    </Grid>
                  </>
                )}
                
                {formData.resultType === 'TEXT' && (
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      multiline
                      rows={3}
                      label="Text Result"
                      value={formData.resultValue || ''}
                      onChange={(e) => handleInputChange('resultValue', e.target.value)}
                    />
                  </Grid>
                )}
                
                {formData.resultType === 'CODED' && (
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Coded Result"
                      value={formData.qualitativeResult || ''}
                      onChange={(e) => handleInputChange('qualitativeResult', e.target.value)}
                      helperText="e.g., POSITIVE, NEGATIVE, REACTIVE, NON-REACTIVE"
                    />
                  </Grid>
                )}
                
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Result Interpretation"
                    value={formData.resultInterpretation || ''}
                    onChange={(e) => handleInputChange('resultInterpretation', e.target.value)}
                    helperText="Clinical interpretation of the result"
                  />
                </Grid>
                
                <Grid item xs={12}>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" gutterBottom>
                    Abnormal Flags
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <FormControl fullWidth>
                    <InputLabel>Abnormal Flag</InputLabel>
                    <Select
                      value={formData.abnormalFlag || ''}
                      label="Abnormal Flag"
                      onChange={(e) => handleInputChange('abnormalFlag', e.target.value || undefined)}
                    >
                      <MenuItem value="">Normal</MenuItem>
                      <MenuItem value="H">High (H)</MenuItem>
                      <MenuItem value="L">Low (L)</MenuItem>
                      <MenuItem value="A">Abnormal (A)</MenuItem>
                      <MenuItem value="C">Critical (C)</MenuItem>
                      <MenuItem value="N">Normal (N)</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={formData.isCriticalValue || false}
                        onChange={(e) => handleInputChange('isCriticalValue', e.target.checked)}
                      />
                    }
                    label="Critical Value"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={formData.isPanicValue || false}
                        onChange={(e) => handleInputChange('isPanicValue', e.target.checked)}
                      />
                    }
                    label="Panic Value"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={formData.isDeltaCheck || false}
                        onChange={(e) => handleInputChange('isDeltaCheck', e.target.checked)}
                      />
                    }
                    label="Delta Check Flag"
                  />
                </Grid>
              </Grid>
            )}
            
            {/* Tab 3: Reference Ranges */}
            {activeTab === 2 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Reference Range Low"
                    value={formData.referenceRangeLow || ''}
                    onChange={(e) => handleInputChange('referenceRangeLow', parseFloat(e.target.value) || undefined)}
                    inputProps={{ step: 'any' }}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Reference Range High"
                    value={formData.referenceRangeHigh || ''}
                    onChange={(e) => handleInputChange('referenceRangeHigh', parseFloat(e.target.value) || undefined)}
                    inputProps={{ step: 'any' }}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Reference Range Units"
                    value={formData.referenceRangeUnits || ''}
                    onChange={(e) => handleInputChange('referenceRangeUnits', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={2}
                    label="Reference Range Text"
                    value={formData.referenceRangeText || ''}
                    onChange={(e) => handleInputChange('referenceRangeText', e.target.value)}
                    helperText="Text description of reference range (e.g., 'Negative' for qualitative tests)"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Reference Range Source"
                    value={formData.referenceRangeSource || ''}
                    onChange={(e) => handleInputChange('referenceRangeSource', e.target.value)}
                    helperText="Source of reference range (e.g., 'Lab Standard', 'Age-Specific', 'Gender-Specific')"
                  />
                </Grid>
                <Grid item xs={12} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={formData.ageSpecificRange || false}
                        onChange={(e) => handleInputChange('ageSpecificRange', e.target.checked)}
                      />
                    }
                    label="Age-Specific Range"
                  />
                </Grid>
                <Grid item xs={12} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={formData.genderSpecificRange || false}
                        onChange={(e) => handleInputChange('genderSpecificRange', e.target.checked)}
                      />
                    }
                    label="Gender-Specific Range"
                  />
                </Grid>
              </Grid>
            )}
            
            {/* Tab 4: Specimen Information */}
            {activeTab === 3 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    type="datetime-local"
                    label="Specimen Collection Date"
                    value={formatDateTimeForInput(formData.specimenCollectionDate)}
                    onChange={(e) => handleDateTimeChange('specimenCollectionDate', e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    type="datetime-local"
                    label="Specimen Received Date"
                    value={formatDateTimeForInput(formData.specimenReceivedDate)}
                    onChange={(e) => handleDateTimeChange('specimenReceivedDate', e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Specimen Type"
                    value={formData.specimenType || ''}
                    onChange={(e) => handleInputChange('specimenType', e.target.value)}
                    helperText="e.g., Blood, Urine, Stool, Tissue"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Specimen Source"
                    value={formData.specimenSource || ''}
                    onChange={(e) => handleInputChange('specimenSource', e.target.value)}
                    helperText="e.g., Venous, Arterial, Random, First Morning"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Collection Method"
                    value={formData.specimenCollectionMethod || ''}
                    onChange={(e) => handleInputChange('specimenCollectionMethod', e.target.value)}
                    helperText="e.g., Venipuncture, Catheter, Swab"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Specimen ID"
                    value={formData.specimenId || ''}
                    onChange={(e) => handleInputChange('specimenId', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Specimen Volume"
                    value={formData.specimenVolume || ''}
                    onChange={(e) => handleInputChange('specimenVolume', e.target.value)}
                    helperText="e.g., 5 mL, 10 mL"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Specimen Quality"
                    value={formData.specimenQuality || ''}
                    onChange={(e) => handleInputChange('specimenQuality', e.target.value)}
                    helperText="e.g., Adequate, Hemolyzed, Clotted"
                  />
                </Grid>
              </Grid>
            )}
            
            {/* Tab 5: Laboratory Information */}
            {activeTab === 4 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    label="Performing Laboratory Name"
                    value={formData.performingLaboratoryName}
                    onChange={(e) => handleInputChange('performingLaboratoryName', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Laboratory ID"
                    value={formData.laboratoryId || ''}
                    onChange={(e) => handleInputChange('laboratoryId', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Laboratory NPI"
                    value={formData.laboratoryNpi || ''}
                    onChange={(e) => handleInputChange('laboratoryNpi', e.target.value)}
                    helperText="National Provider Identifier"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Laboratory Reference Number"
                    value={formData.laboratoryReferenceNumber || ''}
                    onChange={(e) => handleInputChange('laboratoryReferenceNumber', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Laboratory Address Line 1"
                    value={formData.laboratoryAddressLine1 || ''}
                    onChange={(e) => handleInputChange('laboratoryAddressLine1', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Laboratory Address Line 2"
                    value={formData.laboratoryAddressLine2 || ''}
                    onChange={(e) => handleInputChange('laboratoryAddressLine2', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="City"
                    value={formData.laboratoryCity || ''}
                    onChange={(e) => handleInputChange('laboratoryCity', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="State"
                    value={formData.laboratoryState || ''}
                    onChange={(e) => handleInputChange('laboratoryState', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Zip Code"
                    value={formData.laboratoryZip || ''}
                    onChange={(e) => handleInputChange('laboratoryZip', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Laboratory Phone"
                    value={formData.laboratoryPhone || ''}
                    onChange={(e) => handleInputChange('laboratoryPhone', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle2" gutterBottom>
                    Personnel Information
                  </Typography>
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Performing Technologist"
                    value={formData.performingTechnologist || ''}
                    onChange={(e) => handleInputChange('performingTechnologist', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Reviewing Pathologist"
                    value={formData.reviewingPathologist || ''}
                    onChange={(e) => handleInputChange('reviewingPathologist', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Reviewing Physician"
                    value={formData.reviewingPhysician || ''}
                    onChange={(e) => handleInputChange('reviewingPhysician', e.target.value)}
                  />
                </Grid>
              </Grid>
            )}
            
            {/* Tab 6: Additional Information */}
            {activeTab === 5 && (
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    type="datetime-local"
                    label="Result Date"
                    value={formatDateTimeForInput(formData.resultDate)}
                    onChange={(e) => handleDateTimeChange('resultDate', e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    required
                    type="datetime-local"
                    label="Result Reported Date"
                    value={formatDateTimeForInput(formData.resultReportedDate)}
                    onChange={(e) => handleDateTimeChange('resultReportedDate', e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    type="datetime-local"
                    label="Result Verified Date"
                    value={formatDateTimeForInput(formData.resultVerifiedDate)}
                    onChange={(e) => handleDateTimeChange('resultVerifiedDate', e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Method Used"
                    value={formData.methodUsed || ''}
                    onChange={(e) => handleInputChange('methodUsed', e.target.value)}
                    helperText="Laboratory method/technique used"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Laboratory Comments"
                    value={formData.laboratoryComments || ''}
                    onChange={(e) => handleInputChange('laboratoryComments', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Provider Comments"
                    value={formData.providerComments || ''}
                    onChange={(e) => handleInputChange('providerComments', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Result Notes"
                    value={formData.resultNotes || ''}
                    onChange={(e) => handleInputChange('resultNotes', e.target.value)}
                  />
                </Grid>
              </Grid>
            )}
          </CardContent>
        </Card>
        
        <Box display="flex" justifyContent="flex-end" gap={1} mt={3}>
          <Button
            variant="outlined"
            startIcon={<CancelIcon />}
            onClick={handleCancel}
          >
            Cancel
          </Button>
          <Button
            variant="outlined"
            startIcon={<SaveIcon />}
            onClick={() => handleSave('PRELIMINARY')}
            disabled={saving}
          >
            Save as Preliminary
          </Button>
          <Button
            variant="contained"
            startIcon={<SendIcon />}
            onClick={() => handleSave('FINAL')}
            disabled={saving}
          >
            {saving ? <CircularProgress size={20} /> : 'Save and Finalize'}
          </Button>
        </Box>
      </Box>
  );
};

export default LabResultReceiptForm;
