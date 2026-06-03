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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Visibility as ViewIcon,
  Link as LinkIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabResult } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const EncounterLabResults: React.FC = () => {
  const { encounterId } = useParams<{ encounterId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [results, setResults] = useState<LabResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    if (encounterId) {
      loadResults();
    }
  }, [encounterId]);
  
  const loadResults = async () => {
    if (!encounterId) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getLabResultsByEncounter(encounterId);
      setResults(response.data);
    } catch (err: any) {
      console.error('Failed to load lab results:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load lab results'));
      enqueueSnackbar('Failed to load lab results', { variant: 'error' });
    } finally {
      setLoading(false);
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
  
  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };
  
  const formatValue = (result: LabResult) => {
    if (result.resultValueNumeric !== undefined && result.resultValueNumeric !== null) {
      return `${result.resultValueNumeric} ${result.resultUnits || ''}`.trim();
    }
    if (result.resultValue) {
      return result.resultValue;
    }
    if (result.qualitativeResult) {
      return result.qualitativeResult;
    }
    return '-';
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  if (error) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
        <Button startIcon={<BackIcon />} onClick={() => navigate(-1)} sx={{ mt: 2 }}>
          Go Back
        </Button>
      </Box>
    );
  }
  
  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Lab Results - Encounter</Typography>
        <Button startIcon={<BackIcon />} onClick={() => navigate(-1)}>
          Back
        </Button>
      </Box>
      
      {results.length === 0 ? (
        <Card>
          <CardContent>
            <Typography variant="body1" color="textSecondary">
              No lab results found for this encounter.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><strong>Test Name</strong></TableCell>
                <TableCell><strong>Result</strong></TableCell>
                <TableCell><strong>Reference Range</strong></TableCell>
                <TableCell><strong>Flag</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Result Date</strong></TableCell>
                <TableCell><strong>Actions</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {results.map((result) => (
                <TableRow key={result.resultId}>
                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="bold">
                        {result.testName}
                      </Typography>
                      {result.loincCode && (
                        <Typography variant="caption" color="textSecondary">
                          LOINC: {result.loincCode}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Box>
                      <Typography variant="body2">
                        {formatValue(result)}
                      </Typography>
                      {result.isCriticalValue && (
                        <Chip
                          label="Critical"
                          color="error"
                          size="small"
                          sx={{ mt: 0.5 }}
                        />
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    {result.referenceRangeText ? (
                      <Typography variant="body2">{result.referenceRangeText}</Typography>
                    ) : result.referenceRangeLow !== undefined && result.referenceRangeHigh !== undefined ? (
                      <Typography variant="body2">
                        {result.referenceRangeLow} - {result.referenceRangeHigh} {result.referenceRangeUnits || ''}
                      </Typography>
                    ) : (
                      <Typography variant="body2" color="textSecondary">-</Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {result.abnormalFlag && (
                      <Chip
                        label={getAbnormalFlagLabel(result.abnormalFlag)}
                        color={getAbnormalFlagColor(result.abnormalFlag) as any}
                        size="small"
                      />
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={result.resultStatus}
                      color={result.resultStatus === 'FINAL' ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {formatDate(result.resultDate)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Tooltip title="View Details">
                      <IconButton
                        size="small"
                        onClick={() => navigate(`/hospital/patients/${result.patientId}/lab-results/${result.resultId}`)}
                      >
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default EncounterLabResults;
