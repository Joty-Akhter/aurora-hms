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
  TextField,
  Typography,
  Tooltip,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  History as HistoryIcon,
  RestartAlt as ReactivateIcon,
  CalendarToday as CalendarIcon,
  FilterList as FilterIcon,
  Clear as ClearIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { MedicationHistory } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const MedicationHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [history, setHistory] = useState<MedicationHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'chronological' | 'complete'>('chronological');
  
  // Search and filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [showFilters, setShowFilters] = useState(false);
  
  // Detail dialog state
  const [selectedHistory, setSelectedHistory] = useState<MedicationHistory | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  
  useEffect(() => {
    if (id) {
      loadHistory();
    }
  }, [id, viewMode]);
  
  const loadHistory = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      setError(null);
      
      let response;
      if (viewMode === 'complete') {
        response = await hospitalService.getCompleteMedicationHistory(id);
      } else {
        response = await hospitalService.getMedicationHistory(id);
      }
      
      setHistory(response.data);
    } catch (err: any) {
      console.error('Failed to load medication history:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load medication history'));
      enqueueSnackbar('Failed to load medication history', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleSearch = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      setError(null);
      
      const searchParams: any = {};
      if (searchTerm) {
        searchParams.medicationName = searchTerm;
      }
      if (statusFilter && statusFilter !== 'ALL') {
        searchParams.status = statusFilter;
      }
      if (startDate) {
        searchParams.startDate = startDate;
      }
      if (endDate) {
        searchParams.endDate = endDate;
      }
      
      const response = await hospitalService.searchMedicationHistory(id, searchParams);
      setHistory(response.data);
      
      if (response.data.length === 0) {
        enqueueSnackbar('No medications found matching search criteria', { variant: 'info' });
      }
    } catch (err: any) {
      console.error('Failed to search medication history:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search medication history'));
      enqueueSnackbar('Failed to search medication history', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleClearFilters = () => {
    setSearchTerm('');
    setStatusFilter('ALL');
    setStartDate('');
    setEndDate('');
    loadHistory();
  };
  
  const handleReactivate = async (historyId: string) => {
    if (!window.confirm('Are you sure you want to reactivate this medication?')) {
      return;
    }
    
    try {
      await hospitalService.reactivateHistoricalMedication(historyId);
      enqueueSnackbar('Medication reactivated successfully', { variant: 'success' });
      loadHistory();
    } catch (err: any) {
      console.error('Failed to reactivate medication:', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to reactivate medication'), { variant: 'error' });
    }
  };
  
  const handleViewDetail = (historyItem: MedicationHistory) => {
    setSelectedHistory(historyItem);
    setShowDetailDialog(true);
  };
  
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'DISCONTINUED':
        return 'error';
      case 'ON_HOLD':
        return 'warning';
      case 'COMPLETED':
        return 'info';
      default:
        return 'default';
    }
  };
  
  const formatDate = (date: string | null | undefined) => {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString();
  };
  
  const filteredHistory = history.filter(item => {
    if (!searchTerm && statusFilter === 'ALL' && !startDate && !endDate) {
      return true;
    }
    
    const matchesSearch = !searchTerm || 
      item.medicationName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.genericName?.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStatus = statusFilter === 'ALL' || item.medicationStatus === statusFilter;
    
    const matchesDateRange = (!startDate || !item.startDate || new Date(item.startDate) >= new Date(startDate)) &&
      (!endDate || !item.endDate || new Date(item.endDate) <= new Date(endDate));
    
    return matchesSearch && matchesStatus && matchesDateRange;
  });
  
  if (loading && history.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          <HistoryIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
          Medication History
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadHistory}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="outlined"
            startIcon={<FilterIcon />}
            onClick={() => setShowFilters(!showFilters)}
          >
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </Button>
        </Box>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      {/* View Mode Tabs */}
      <Box sx={{ mb: 2 }}>
        <Tabs value={viewMode} onChange={(_, newValue) => setViewMode(newValue)}>
          <Tab label="Recent First" value="chronological" />
          <Tab label="Complete History (Chronological)" value="complete" />
        </Tabs>
      </Box>
      
      {/* Search and Filter Section */}
      {showFilters && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Search Medication Name"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Enter medication or generic name"
                  InputProps={{
                    startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
                  }}
                />
              </Grid>
              <Grid item xs={12} md={2}>
                <FormControl fullWidth>
                  <InputLabel>Status</InputLabel>
                  <Select
                    value={statusFilter}
                    label="Status"
                    onChange={(e) => setStatusFilter(e.target.value)}
                  >
                    <MenuItem value="ALL">All Status</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="DISCONTINUED">Discontinued</MenuItem>
                    <MenuItem value="ON_HOLD">On Hold</MenuItem>
                    <MenuItem value="COMPLETED">Completed</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={2}>
                <TextField
                  fullWidth
                  label="Start Date"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={2}>
                <TextField
                  fullWidth
                  label="End Date"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={2}>
                <Button
                  fullWidth
                  variant="contained"
                  startIcon={<SearchIcon />}
                  onClick={handleSearch}
                  sx={{ height: '56px' }}
                >
                  Search
                </Button>
              </Grid>
              <Grid item xs={12}>
                <Button
                  variant="outlined"
                  startIcon={<ClearIcon />}
                  onClick={handleClearFilters}
                  size="small"
                >
                  Clear Filters
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
      
      {/* History Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell><strong>Medication Name</strong></TableCell>
              <TableCell><strong>Generic Name</strong></TableCell>
              <TableCell><strong>Dosage</strong></TableCell>
              <TableCell><strong>Route</strong></TableCell>
              <TableCell><strong>Frequency</strong></TableCell>
              <TableCell><strong>Start Date</strong></TableCell>
              <TableCell><strong>End Date</strong></TableCell>
              <TableCell><strong>Status</strong></TableCell>
              <TableCell><strong>Discontinuation Reason</strong></TableCell>
              <TableCell><strong>Actions</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredHistory.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    No medication history found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              filteredHistory.map((item) => (
                <TableRow key={item.historyId} hover>
                  <TableCell>{item.medicationName}</TableCell>
                  <TableCell>{item.genericName || 'N/A'}</TableCell>
                  <TableCell>
                    {item.dosageStrength} {item.dosageUnit} {item.dosageForm}
                  </TableCell>
                  <TableCell>{item.route || 'N/A'}</TableCell>
                  <TableCell>{item.frequency || 'N/A'}</TableCell>
                  <TableCell>{formatDate(item.startDate)}</TableCell>
                  <TableCell>{formatDate(item.endDate)}</TableCell>
                  <TableCell>
                    <Chip
                      label={item.medicationStatus}
                      color={getStatusColor(item.medicationStatus) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {item.discontinuationReason ? (
                      <Tooltip title={item.discontinuationReason}>
                        <Typography variant="body2" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {item.discontinuationReason}
                        </Typography>
                      </Tooltip>
                    ) : (
                      'N/A'
                    )}
                  </TableCell>
                  <TableCell>
                    <Box display="flex" gap={1}>
                      <Tooltip title="View Details">
                        <IconButton size="small" onClick={() => handleViewDetail(item)}>
                          <HistoryIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {item.medicationStatus === 'DISCONTINUED' && (
                        <Tooltip title="Reactivate Medication">
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleReactivate(item.historyId)}
                          >
                            <ReactivateIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
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
        onClose={() => setShowDetailDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Medication History Details
          <IconButton
            onClick={() => setShowDetailDialog(false)}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            <ClearIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          {selectedHistory && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Medication Name</Typography>
                <Typography variant="body1">{selectedHistory.medicationName}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Generic Name</Typography>
                <Typography variant="body1">{selectedHistory.genericName || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Medication Code</Typography>
                <Typography variant="body1">{selectedHistory.medicationCode || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                <Chip
                  label={selectedHistory.medicationStatus}
                  color={getStatusColor(selectedHistory.medicationStatus) as any}
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Dosage</Typography>
                <Typography variant="body1">
                  {selectedHistory.dosageStrength} {selectedHistory.dosageUnit} {selectedHistory.dosageForm}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Route</Typography>
                <Typography variant="body1">{selectedHistory.route || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Frequency</Typography>
                <Typography variant="body1">{selectedHistory.frequency || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Start Date</Typography>
                <Typography variant="body1">{formatDate(selectedHistory.startDate)}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">End Date</Typography>
                <Typography variant="body1">{formatDate(selectedHistory.endDate)}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Status Date</Typography>
                <Typography variant="body1">{formatDate(selectedHistory.statusDate)}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Source</Typography>
                <Typography variant="body1">{selectedHistory.medicationSource || 'N/A'}</Typography>
              </Grid>
              {selectedHistory.discontinuationReason && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Discontinuation Reason</Typography>
                  <Typography variant="body1">{selectedHistory.discontinuationReason}</Typography>
                </Grid>
              )}
              {selectedHistory.indication && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Indication</Typography>
                  <Typography variant="body1">{selectedHistory.indication}</Typography>
                </Grid>
              )}
              {selectedHistory.instructions && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Instructions</Typography>
                  <Typography variant="body1">{selectedHistory.instructions}</Typography>
                </Grid>
              )}
              {selectedHistory.notes && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Notes</Typography>
                  <Typography variant="body1">{selectedHistory.notes}</Typography>
                </Grid>
              )}
              {selectedHistory.prescribingProviderName && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Prescribing Provider</Typography>
                  <Typography variant="body1">{selectedHistory.prescribingProviderName}</Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default MedicationHistoryPage;
