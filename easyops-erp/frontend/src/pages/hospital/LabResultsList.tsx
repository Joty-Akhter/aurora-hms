import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
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
  Tabs,
  Tab,
  Typography,
  Tooltip,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  CompareArrows as CompareIcon,
  TrendingUp as TrendIcon,
  Link as LinkIcon,
  Visibility as ViewIcon,
  Refresh as RefreshIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { LabResultListView } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const LabResultsList: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [results, setResults] = useState<LabResultListView[]>([]);
  const [resultsByCategory, setResultsByCategory] = useState<Record<string, LabResultListView[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'chronological' | 'category'>('chronological');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [selectedResult, setSelectedResult] = useState<LabResultListView | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  
  useEffect(() => {
    if (id) {
      loadResults();
    }
  }, [id, viewMode]);
  
  const loadResults = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      if (viewMode === 'chronological') {
        const response = await hospitalService.getLabResultsChronological(id);
        setResults(response.data);
      } else {
        const response = await hospitalService.getLabResultsByCategory(id);
        setResultsByCategory(response.data);
        const categories = Object.keys(response.data);
        if (categories.length > 0 && !selectedCategory) {
          setSelectedCategory(categories[0]);
        }
      }
    } catch (err: any) {
      console.error('Failed to load lab results:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load lab results'));
      enqueueSnackbar('Failed to load lab results', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleViewDetail = (result: LabResultListView) => {
    setSelectedResult(result);
    setShowDetailDialog(true);
  };
  
  const handleCompare = (resultId: string) => {
    navigate(`/hospital/patients/${id}/lab-results/${resultId}/compare`);
  };
  
  const handleViewTrend = (loincCode: string) => {
    navigate(`/hospital/patients/${id}/lab-results/trend?loincCode=${loincCode}`);
  };
  
  const handleViewCorrelated = (resultId: string) => {
    navigate(`/hospital/patients/${id}/lab-results/${resultId}/correlated`);
  };
  
  const getHighlightColor = (color?: string) => {
    switch (color) {
      case 'RED': return '#f44336';
      case 'ORANGE': return '#ff9800';
      case 'YELLOW': return '#ffeb3b';
      case 'GREEN': return '#4caf50';
      default: return 'transparent';
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
  
  const displayResults = viewMode === 'chronological' 
    ? results 
    : selectedCategory ? (resultsByCategory[selectedCategory] || []) : [];
  
  if (loading && results.length === 0 && Object.keys(resultsByCategory).length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Laboratory Results</Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadResults}
        >
          Refresh
        </Button>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs
            value={viewMode}
            onChange={(_, newValue) => {
              setViewMode(newValue);
              setSelectedCategory('');
            }}
            sx={{ mb: 2 }}
          >
            <Tab label="Chronological" value="chronological" />
            <Tab label="By Category" value="category" />
          </Tabs>
          
          {viewMode === 'category' && (
            <FormControl fullWidth sx={{ mt: 2 }}>
              <InputLabel>Select Category</InputLabel>
              <Select
                value={selectedCategory}
                label="Select Category"
                onChange={(e) => setSelectedCategory(e.target.value)}
              >
                {Object.keys(resultsByCategory).map((category) => (
                  <MenuItem key={category} value={category}>
                    {category || 'Uncategorized'} ({resultsByCategory[category].length})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </CardContent>
      </Card>
      
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Test Name</TableCell>
              <TableCell>LOINC Code</TableCell>
              <TableCell>Result</TableCell>
              <TableCell>Reference Range</TableCell>
              <TableCell>Flag</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Result Date</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {displayResults.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                    No lab results found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              displayResults.map((result) => (
                <TableRow 
                  key={result.resultId} 
                  hover
                  sx={{
                    backgroundColor: result.highlightColor 
                      ? `${getHighlightColor(result.highlightColor)}20` 
                      : 'transparent',
                    borderLeft: result.requiresAttention 
                      ? `4px solid ${getHighlightColor(result.highlightColor)}` 
                      : 'none',
                  }}
                >
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {result.requiresAttention && (
                        <WarningIcon 
                          fontSize="small" 
                          color="error"
                          sx={{ opacity: 0.7 }}
                        />
                      )}
                      <Typography variant="body2" fontWeight={result.requiresAttention ? 600 : 400}>
                        {result.testName}
                      </Typography>
                    </Box>
                    {result.testCategory && (
                      <Typography variant="caption" color="text.secondary">
                        {result.testCategory}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{result.loincCode}</TableCell>
                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="bold">
                        {result.resultValue || result.resultValueNumeric || '-'}
                        {result.resultUnits && ` ${result.resultUnits}`}
                      </Typography>
                      {result.highlightReason && (
                        <Typography variant="caption" color="text.secondary">
                          {result.highlightReason.replace(/_/g, ' ')}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    {result.referenceRangeLow !== undefined && result.referenceRangeHigh !== undefined ? (
                      <Typography variant="body2">
                        {result.referenceRangeLow} - {result.referenceRangeHigh} {result.referenceRangeUnits || ''}
                      </Typography>
                    ) : (
                      '-'
                    )}
                  </TableCell>
                  <TableCell>
                    <Box display="flex" flexDirection="column" gap={0.5}>
                      <Box display="flex" gap={0.5} flexWrap="wrap">
                        {result.abnormalFlag && (
                          <Chip
                            label={getAbnormalFlagLabel(result.abnormalFlag)}
                            size="small"
                            color={getAbnormalFlagColor(result.abnormalFlag) as any}
                          />
                        )}
                        {result.isCriticalValue && (
                          <Chip
                            label="Critical"
                            size="small"
                            color="error"
                          />
                        )}
                        {result.isDeltaCheck && (
                          <Chip
                            label="Delta"
                            size="small"
                            color="warning"
                          />
                        )}
                      </Box>
                      {(result as any).clinicalSignificance && (
                        <Chip
                          label={(result as any).clinicalSignificance.replace(/_/g, ' ')}
                          size="small"
                          color={
                            (result as any).clinicalSignificance === 'CRITICAL' ? 'error' :
                            (result as any).clinicalSignificance === 'ABNORMAL' ? 'warning' :
                            (result as any).clinicalSignificance === 'SIGNIFICANT_CHANGE' ? 'warning' :
                            'success'
                          }
                          variant="outlined"
                        />
                      )}
                      {(result as any).clinicalSignificanceLevel && (
                        <Typography variant="caption" color="text.secondary">
                          Level: {(result as any).clinicalSignificanceLevel}
                        </Typography>
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Chip
                        label={result.resultStatus}
                        size="small"
                        color={result.resultStatus === 'FINAL' ? 'success' : 'default'}
                      />
                      {result.isReviewed ? (
                        <CheckCircleIcon fontSize="small" color="success" />
                      ) : (
                        <WarningIcon fontSize="small" color="warning" />
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>
                    {new Date(result.resultDate).toLocaleDateString()}
                    <Typography variant="caption" display="block" color="text.secondary">
                      {new Date(result.resultDate).toLocaleTimeString()}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Box display="flex" gap={0.5}>
                      <Tooltip title="View Detail">
                        <IconButton
                          size="small"
                          onClick={() => handleViewDetail(result)}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {result.resultType === 'NUMERIC' && (
                        <>
                          <Tooltip title="Compare with Previous">
                            <IconButton
                              size="small"
                              onClick={() => handleCompare(result.resultId)}
                            >
                              <CompareIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="View Trend">
                            <IconButton
                              size="small"
                              onClick={() => handleViewTrend(result.loincCode)}
                            >
                              <TrendIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </>
                      )}
                      <Tooltip title="View Correlated Results">
                        <IconButton
                          size="small"
                          onClick={() => handleViewCorrelated(result.resultId)}
                        >
                          <LinkIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      
      {/* Detail Dialog */}
      <Dialog
        open={showDetailDialog}
        onClose={() => {
          setShowDetailDialog(false);
          setSelectedResult(null);
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Lab Result Details</DialogTitle>
        <DialogContent>
          {selectedResult && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Test Name</Typography>
                <Typography variant="body1">{selectedResult.testName}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">LOINC Code</Typography>
                <Typography variant="body1">{selectedResult.loincCode}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Result</Typography>
                <Typography variant="body1" fontWeight="bold">
                  {selectedResult.resultValue || selectedResult.resultValueNumeric || '-'}
                  {selectedResult.resultUnits && ` ${selectedResult.resultUnits}`}
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Reference Range</Typography>
                <Typography variant="body1">
                  {selectedResult.referenceRangeLow !== undefined && selectedResult.referenceRangeHigh !== undefined
                    ? `${selectedResult.referenceRangeLow} - ${selectedResult.referenceRangeHigh} ${selectedResult.referenceRangeUnits || ''}`
                    : '-'}
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                <Chip
                  label={selectedResult.resultStatus}
                  size="small"
                  color={selectedResult.resultStatus === 'FINAL' ? 'success' : 'default'}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Result Date</Typography>
                <Typography variant="body1">
                  {new Date(selectedResult.resultDate).toLocaleString()}
                </Typography>
              </Grid>
              {selectedResult.performingLaboratoryName && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Laboratory</Typography>
                  <Typography variant="body1">{selectedResult.performingLaboratoryName}</Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default LabResultsList;
