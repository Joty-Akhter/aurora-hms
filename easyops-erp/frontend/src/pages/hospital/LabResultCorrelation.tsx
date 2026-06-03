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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabResult } from '../../services/hospitalService';
import type { LabResultCorrelation as LabResultCorrelationType } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultCorrelation: React.FC = () => {
  const { id, resultId } = useParams<{ id: string; resultId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [correlation, setCorrelation] = useState<LabResultCorrelationType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    if (resultId) {
      loadCorrelation();
    }
  }, [resultId]);
  
  const loadCorrelation = async () => {
    if (!resultId) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getCorrelatedResults(resultId);
      setCorrelation(response.data);
    } catch (err: any) {
      console.error('Failed to load correlation:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load correlated results'));
      enqueueSnackbar('Failed to load correlated results', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleViewResult = (result: LabResult) => {
    navigate(`/hospital/patients/${id}/lab-results/${result.resultId}`);
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
  
  if (error || !correlation) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error || 'Correlated results not available'}</Alert>
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
  
  const primary = correlation.primaryResult;
  
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
          <Typography variant="h4">Correlated Results</Typography>
        </Box>
      </Box>
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Correlation Group</Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {correlation.correlationGroup || 'Related Tests'}
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {correlation.correlationReason || 'Results collected together or from same order'}
          </Typography>
          <Box display="flex" gap={1} mt={2} flexWrap="wrap">
            <Chip label={`Total: ${correlation.totalRelatedResults + 1}`} />
            <Chip 
              label={`Abnormal: ${correlation.abnormalResultsCount}`}
              color={correlation.abnormalResultsCount > 0 ? 'warning' : 'default'}
            />
            <Chip 
              label={`Critical: ${correlation.criticalResultsCount}`}
              color={correlation.criticalResultsCount > 0 ? 'error' : 'default'}
            />
          </Box>
        </CardContent>
      </Card>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Primary Result</Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="h5" fontWeight="bold">
                {primary.testName}
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="primary" sx={{ mt: 1 }}>
                {primary.resultValue || primary.resultValueNumeric || '-'}
                {primary.resultUnits && ` ${primary.resultUnits}`}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Date: {new Date(primary.resultDate).toLocaleString()}
              </Typography>
              <Box display="flex" gap={1} mt={2} flexWrap="wrap">
                {primary.abnormalFlag && (
                  <Chip
                    label={getAbnormalFlagLabel(primary.abnormalFlag)}
                    color={getAbnormalFlagColor(primary.abnormalFlag) as any}
                    size="small"
                  />
                )}
                {primary.isCriticalValue && (
                  <Chip label="Critical" color="error" size="small" />
                )}
                <Chip
                  label={primary.resultStatus}
                  color={primary.resultStatus === 'FINAL' ? 'success' : 'default'}
                  size="small"
                />
              </Box>
              <Button
                variant="outlined"
                startIcon={<ViewIcon />}
                onClick={() => handleViewResult(primary)}
                sx={{ mt: 2 }}
              >
                View Details
              </Button>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Collection Information</Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="body2" color="text.secondary">
                Collection Date
              </Typography>
              <Typography variant="body1">
                {new Date(correlation.collectionDate).toLocaleString()}
              </Typography>
              {correlation.encounterId && (
                <>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                    Encounter ID
                  </Typography>
                  <Typography variant="body1">{correlation.encounterId}</Typography>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {correlation.relatedResults.length > 0 && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>Related Results</Typography>
            <Divider sx={{ mb: 2 }} />
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Test Name</TableCell>
                    <TableCell>LOINC Code</TableCell>
                    <TableCell>Result</TableCell>
                    <TableCell>Reference Range</TableCell>
                    <TableCell>Flag</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {correlation.relatedResults.map((result) => (
                    <TableRow key={result.resultId} hover>
                      <TableCell>{result.testName}</TableCell>
                      <TableCell>{result.loincCode}</TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="bold">
                          {result.resultValue || result.resultValueNumeric || '-'}
                          {result.resultUnits && ` ${result.resultUnits}`}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {result.referenceRangeLow !== undefined && result.referenceRangeHigh !== undefined
                          ? `${result.referenceRangeLow} - ${result.referenceRangeHigh} ${result.referenceRangeUnits || ''}`
                          : '-'}
                      </TableCell>
                      <TableCell>
                        <Box display="flex" gap={0.5} flexWrap="wrap">
                          {result.abnormalFlag && (
                            <Chip
                              label={getAbnormalFlagLabel(result.abnormalFlag)}
                              size="small"
                              color={getAbnormalFlagColor(result.abnormalFlag) as any}
                            />
                          )}
                          {result.isCriticalValue && (
                            <Chip label="Critical" size="small" color="error" />
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={result.resultStatus}
                          size="small"
                          color={result.resultStatus === 'FINAL' ? 'success' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        {new Date(result.resultDate).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          startIcon={<ViewIcon />}
                          onClick={() => handleViewResult(result)}
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default LabResultCorrelation;
