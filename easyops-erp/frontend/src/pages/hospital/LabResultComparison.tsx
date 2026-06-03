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
  TrendingUp as TrendIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService from '../../services/hospitalService';
import type { LabResultComparison as LabResultComparisonType } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultComparison: React.FC = () => {
  const { id, resultId } = useParams<{ id: string; resultId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [comparison, setComparison] = useState<LabResultComparisonType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    if (resultId) {
      loadComparison();
    }
  }, [resultId]);
  
  const loadComparison = async () => {
    if (!resultId) return;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.compareResults(resultId);
      setComparison(response.data);
    } catch (err: any) {
      console.error('Failed to load comparison:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load comparison'));
      enqueueSnackbar('Failed to load comparison', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  if (error || !comparison) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error || 'Comparison not available'}</Alert>
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
  
  const current = comparison.currentResult;
  const previous = comparison.previousResult;
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            startIcon={<BackIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/lab-results/${resultId}`)}
          >
            Back
          </Button>
          <Typography variant="h4">Result Comparison</Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<TrendIcon />}
          onClick={() => navigate(`/hospital/patients/${id}/lab-results/trend?loincCode=${comparison.loincCode}`)}
        >
          View Trend
        </Button>
      </Box>
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>{comparison.testName}</Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            LOINC Code: {comparison.loincCode}
          </Typography>
          {comparison.comparisonNotes && (
            <Alert severity={comparison.isSignificantChange ? 'warning' : 'info'} sx={{ mt: 2 }}>
              {comparison.comparisonNotes}
            </Alert>
          )}
        </CardContent>
      </Card>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Current Result</Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="h4" fontWeight="bold" color="primary">
                {current.resultValue || current.resultValueNumeric || '-'}
                {current.resultUnits && ` ${current.resultUnits}`}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Date: {new Date(current.resultDate).toLocaleString()}
              </Typography>
              {current.referenceRangeLow !== undefined && current.referenceRangeHigh !== undefined && (
                <Typography variant="body2" color="text.secondary">
                  Reference: {current.referenceRangeLow} - {current.referenceRangeHigh} {current.referenceRangeUnits || ''}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Previous Result</Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="h4" fontWeight="bold">
                {previous.resultValue || previous.resultValueNumeric || '-'}
                {previous.resultUnits && ` ${previous.resultUnits}`}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Date: {new Date(previous.resultDate).toLocaleString()}
              </Typography>
              {previous.referenceRangeLow !== undefined && previous.referenceRangeHigh !== undefined && (
                <Typography variant="body2" color="text.secondary">
                  Reference: {previous.referenceRangeLow} - {previous.referenceRangeHigh} {previous.referenceRangeUnits || ''}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Comparison Metrics</Typography>
          <Divider sx={{ mb: 2 }} />
          <TableContainer>
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell><strong>Change Direction</strong></TableCell>
                  <TableCell>
                    <Chip
                      label={comparison.changeDirection || 'N/A'}
                      color={
                        comparison.changeDirection === 'INCREASED' ? 'error' :
                        comparison.changeDirection === 'DECREASED' ? 'success' : 'default'
                      }
                    />
                  </TableCell>
                </TableRow>
                {comparison.absoluteDifference !== undefined && (
                  <TableRow>
                    <TableCell><strong>Absolute Difference</strong></TableCell>
                    <TableCell>
                      {comparison.absoluteDifference > 0 ? '+' : ''}
                      {comparison.absoluteDifference.toFixed(2)} {current.resultUnits || ''}
                    </TableCell>
                  </TableRow>
                )}
                {comparison.percentChange !== undefined && (
                  <TableRow>
                    <TableCell><strong>Percent Change</strong></TableCell>
                    <TableCell>
                      {comparison.percentChange > 0 ? '+' : ''}
                      {comparison.percentChange.toFixed(2)}%
                    </TableCell>
                  </TableRow>
                )}
                {comparison.daysBetweenResults !== undefined && (
                  <TableRow>
                    <TableCell><strong>Time Between Results</strong></TableCell>
                    <TableCell>{comparison.daysBetweenResults} days</TableCell>
                  </TableRow>
                )}
                <TableRow>
                  <TableCell><strong>Significant Change</strong></TableCell>
                  <TableCell>
                    <Chip
                      label={comparison.isSignificantChange ? 'Yes' : 'No'}
                      color={comparison.isSignificantChange ? 'warning' : 'default'}
                    />
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LabResultComparison;
