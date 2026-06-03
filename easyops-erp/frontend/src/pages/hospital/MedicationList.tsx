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
  Checkbox,
  FormControlLabel,
  FormGroup,
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Print as PrintIcon,
  FileDownload as ExportIcon,
  ViewList as ListIcon,
  ViewModule as GridIcon,
  Timeline as TimelineIcon,
  FilterList as FilterIcon,
  Clear as ClearIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { Medication } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

type ViewType = 'summary' | 'detailed' | 'timeline';
type DisplayOption = 'all' | 'byType' | 'byIndication';

interface MedicationListCustomization {
  showGenericName: boolean;
  showIndication: boolean;
  showProvider: boolean;
  showPharmacy: boolean;
  showInstructions: boolean;
  showNotes: boolean;
  showDates: boolean;
  showStatus: boolean;
}

const MedicationListPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [medications, setMedications] = useState<Medication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewType, setViewType] = useState<ViewType>('summary');
  const [displayOption, setDisplayOption] = useState<DisplayOption>('all');
  
  // Filter states
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [indicationFilter, setIndicationFilter] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [showFilters, setShowFilters] = useState(false);
  
  // Indications list
  const [indications, setIndications] = useState<string[]>([]);
  
  // Customization
  const [showCustomization, setShowCustomization] = useState(false);
  const [customization, setCustomization] = useState<MedicationListCustomization>({
    showGenericName: true,
    showIndication: true,
    showProvider: true,
    showPharmacy: false,
    showInstructions: true,
    showNotes: false,
    showDates: true,
    showStatus: true,
  });
  
  // Detail dialog state
  const [selectedMedication, setSelectedMedication] = useState<Medication | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  
  useEffect(() => {
    if (id) {
      loadIndications();
      loadMedications();
    }
  }, [id, viewType, displayOption, statusFilter, indicationFilter, startDate, endDate]);
  
  const loadIndications = async () => {
    if (!id) return;
    
    try {
      const response = await hospitalService.getDistinctIndications(id);
      setIndications(response.data);
    } catch (err: any) {
      console.error('Failed to load indications:', err);
    }
  };
  
  const loadMedications = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      setError(null);
      
      const params: any = {};
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (indicationFilter) {
        params.indication = indicationFilter;
      }
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }
      
      let response;
      if (displayOption === 'byIndication' && indicationFilter) {
        response = await hospitalService.getMedicationsByIndication(id, indicationFilter);
      } else {
        switch (viewType) {
          case 'summary':
            response = await hospitalService.getMedicationListSummary(id, params);
            break;
          case 'detailed':
            response = await hospitalService.getMedicationListDetailed(id, params);
            break;
          case 'timeline':
            response = await hospitalService.getMedicationListTimeline(id, params);
            break;
          default:
            response = await hospitalService.getMedicationListSummary(id, params);
        }
      }
      
      setMedications(response.data);
    } catch (err: any) {
      console.error('Failed to load medications:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load medications'));
      enqueueSnackbar('Failed to load medications', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handlePrint = async () => {
    if (!id) return;
    
    try {
      const params: any = {
        viewType,
      };
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (indicationFilter) {
        params.indication = indicationFilter;
      }
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }
      
      const response = await hospitalService.printMedicationList(id, params);
      
      // Open print window
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(response.data);
        printWindow.document.close();
        printWindow.focus();
        setTimeout(() => {
          printWindow.print();
        }, 250);
      }
    } catch (err: any) {
      console.error('Failed to print medication list:', err);
      enqueueSnackbar('Failed to print medication list', { variant: 'error' });
    }
  };
  
  const handleExport = async (format: 'pdf' | 'csv') => {
    if (!id) return;
    
    try {
      const params: any = {
        viewType,
      };
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (indicationFilter) {
        params.indication = indicationFilter;
      }
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }
      
      let response;
      let filename;
      
      if (format === 'pdf') {
        response = await hospitalService.exportMedicationListToPdf(id, params);
        filename = `Medication_List_${id.substring(0, 8)}_${viewType}.pdf`;
      } else {
        response = await hospitalService.exportMedicationListToCsv(id, params);
        filename = `Medication_List_${id.substring(0, 8)}_${viewType}.csv`;
      }
      
      // Download file
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      enqueueSnackbar(`Medication list exported to ${format.toUpperCase()} successfully`, { variant: 'success' });
    } catch (err: any) {
      console.error(`Failed to export medication list to ${format}:`, err);
      enqueueSnackbar(`Failed to export medication list to ${format.toUpperCase()}`, { variant: 'error' });
    }
  };
  
  const handleClearFilters = () => {
    setStatusFilter('ALL');
    setIndicationFilter('');
    setStartDate('');
    setEndDate('');
  };
  
  const handleViewDetail = (medication: Medication) => {
    setSelectedMedication(medication);
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
  
  if (loading && medications.length === 0) {
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
          <ListIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
          Medication List
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadMedications}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="outlined"
            startIcon={<FilterIcon />}
            onClick={() => setShowFilters(!showFilters)}
            sx={{ mr: 1 }}
          >
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </Button>
          <Button
            variant="outlined"
            startIcon={<SettingsIcon />}
            onClick={() => setShowCustomization(!showCustomization)}
            sx={{ mr: 1 }}
          >
            Customize
          </Button>
          <Button
            variant="outlined"
            startIcon={<PrintIcon />}
            onClick={handlePrint}
            sx={{ mr: 1 }}
          >
            Print
          </Button>
          <Button
            variant="outlined"
            startIcon={<ExportIcon />}
            onClick={() => {
              const menu = document.createElement('div');
              menu.style.position = 'absolute';
              menu.style.zIndex = '1000';
              // Simple export menu - in production, use a proper menu component
              if (window.confirm('Export as PDF?')) {
                handleExport('pdf');
              } else if (window.confirm('Export as CSV?')) {
                handleExport('csv');
              }
            }}
          >
            Export
          </Button>
        </Box>
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      {/* View Type Tabs */}
      <Box sx={{ mb: 2 }}>
        <Tabs value={viewType} onChange={(_, newValue) => setViewType(newValue)}>
          <Tab icon={<ListIcon />} iconPosition="start" label="Summary" value="summary" />
          <Tab icon={<GridIcon />} iconPosition="start" label="Detailed" value="detailed" />
          <Tab icon={<TimelineIcon />} iconPosition="start" label="Timeline" value="timeline" />
        </Tabs>
      </Box>
      
      {/* Display Options */}
      <Box sx={{ mb: 2 }}>
        <FormControl sx={{ minWidth: 200, mr: 2 }}>
          <InputLabel>Display Options</InputLabel>
          <Select
            value={displayOption}
            label="Display Options"
            onChange={(e) => setDisplayOption(e.target.value as DisplayOption)}
          >
            <MenuItem value="all">All Medications</MenuItem>
            <MenuItem value="byType">By Type</MenuItem>
            <MenuItem value="byIndication">By Indication</MenuItem>
          </Select>
        </FormControl>
      </Box>
      
      {/* Search and Filter Section */}
      {showFilters && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={3}>
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
              <Grid item xs={12} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Indication</InputLabel>
                  <Select
                    value={indicationFilter}
                    label="Indication"
                    onChange={(e) => setIndicationFilter(e.target.value)}
                  >
                    <MenuItem value="">All Indications</MenuItem>
                    {indications.map((ind) => (
                      <MenuItem key={ind} value={ind}>{ind}</MenuItem>
                    ))}
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
                  variant="outlined"
                  startIcon={<ClearIcon />}
                  onClick={handleClearFilters}
                >
                  Clear
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
      
      {/* Customization Panel */}
      {showCustomization && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>Customize Display</Typography>
            <FormGroup>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showGenericName}
                        onChange={(e) => setCustomization({ ...customization, showGenericName: e.target.checked })}
                      />
                    }
                    label="Show Generic Name"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showIndication}
                        onChange={(e) => setCustomization({ ...customization, showIndication: e.target.checked })}
                      />
                    }
                    label="Show Indication"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showProvider}
                        onChange={(e) => setCustomization({ ...customization, showProvider: e.target.checked })}
                      />
                    }
                    label="Show Provider"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showPharmacy}
                        onChange={(e) => setCustomization({ ...customization, showPharmacy: e.target.checked })}
                      />
                    }
                    label="Show Pharmacy"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showInstructions}
                        onChange={(e) => setCustomization({ ...customization, showInstructions: e.target.checked })}
                      />
                    }
                    label="Show Instructions"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showNotes}
                        onChange={(e) => setCustomization({ ...customization, showNotes: e.target.checked })}
                      />
                    }
                    label="Show Notes"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showDates}
                        onChange={(e) => setCustomization({ ...customization, showDates: e.target.checked })}
                      />
                    }
                    label="Show Dates"
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={customization.showStatus}
                        onChange={(e) => setCustomization({ ...customization, showStatus: e.target.checked })}
                      />
                    }
                    label="Show Status"
                  />
                </Grid>
              </Grid>
            </FormGroup>
          </CardContent>
        </Card>
      )}
      
      {/* Medication List Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell><strong>Medication Name</strong></TableCell>
              {customization.showGenericName && <TableCell><strong>Generic Name</strong></TableCell>}
              <TableCell><strong>Dosage</strong></TableCell>
              <TableCell><strong>Route</strong></TableCell>
              <TableCell><strong>Frequency</strong></TableCell>
              {customization.showStatus && <TableCell><strong>Status</strong></TableCell>}
              {customization.showIndication && <TableCell><strong>Indication</strong></TableCell>}
              {customization.showProvider && <TableCell><strong>Provider</strong></TableCell>}
              {customization.showPharmacy && <TableCell><strong>Pharmacy</strong></TableCell>}
              {customization.showDates && (
                <>
                  <TableCell><strong>Start Date</strong></TableCell>
                  <TableCell><strong>End Date</strong></TableCell>
                </>
              )}
              <TableCell><strong>Actions</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {medications.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    No medications found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              medications.map((med) => (
                <React.Fragment key={med.medicationId}>
                  <TableRow hover>
                    <TableCell>
                      <strong>{med.medicationName}</strong>
                      {customization.showInstructions && med.instructions && (
                        <Typography variant="caption" display="block" color="text.secondary">
                          {med.instructions}
                        </Typography>
                      )}
                    </TableCell>
                    {customization.showGenericName && (
                      <TableCell>{med.genericName || 'N/A'}</TableCell>
                    )}
                    <TableCell>
                      {med.dosageStrength} {med.dosageUnit} {med.dosageForm}
                    </TableCell>
                    <TableCell>{med.route || 'N/A'}</TableCell>
                    <TableCell>{med.frequency || 'N/A'}</TableCell>
                    {customization.showStatus && (
                      <TableCell>
                        <Chip
                          label={med.medicationStatus}
                          color={getStatusColor(med.medicationStatus) as any}
                          size="small"
                        />
                      </TableCell>
                    )}
                    {customization.showIndication && (
                      <TableCell>{med.indication || 'N/A'}</TableCell>
                    )}
                    {customization.showProvider && (
                      <TableCell>{med.prescribingProviderName || 'N/A'}</TableCell>
                    )}
                    {customization.showPharmacy && (
                      <TableCell>{med.pharmacyName || 'N/A'}</TableCell>
                    )}
                    {customization.showDates && (
                      <>
                        <TableCell>{formatDate(med.startDate)}</TableCell>
                        <TableCell>{formatDate(med.endDate)}</TableCell>
                      </>
                    )}
                    <TableCell>
                      <Button
                        size="small"
                        onClick={() => handleViewDetail(med)}
                      >
                        View
                      </Button>
                    </TableCell>
                  </TableRow>
                  {customization.showNotes && med.notes && (
                    <TableRow>
                      <TableCell colSpan={10} sx={{ py: 1, px: 2, backgroundColor: '#f5f5f5' }}>
                        <Typography variant="caption">
                          <strong>Notes:</strong> {med.notes}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
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
          Medication Details
          <IconButton
            onClick={() => setShowDetailDialog(false)}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            <ClearIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          {selectedMedication && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Medication Name</Typography>
                <Typography variant="body1">{selectedMedication.medicationName}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Generic Name</Typography>
                <Typography variant="body1">{selectedMedication.genericName || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Dosage</Typography>
                <Typography variant="body1">
                  {selectedMedication.dosageStrength} {selectedMedication.dosageUnit} {selectedMedication.dosageForm}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Route</Typography>
                <Typography variant="body1">{selectedMedication.route || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Frequency</Typography>
                <Typography variant="body1">{selectedMedication.frequency || 'N/A'}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                <Chip
                  label={selectedMedication.medicationStatus}
                  color={getStatusColor(selectedMedication.medicationStatus) as any}
                  size="small"
                />
              </Grid>
              {selectedMedication.indication && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Indication</Typography>
                  <Typography variant="body1">{selectedMedication.indication}</Typography>
                </Grid>
              )}
              {selectedMedication.instructions && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Instructions</Typography>
                  <Typography variant="body1">{selectedMedication.instructions}</Typography>
                </Grid>
              )}
              {selectedMedication.prescribingProviderName && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Prescribing Provider</Typography>
                  <Typography variant="body1">{selectedMedication.prescribingProviderName}</Typography>
                </Grid>
              )}
              {selectedMedication.pharmacyName && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Pharmacy</Typography>
                  <Typography variant="body1">{selectedMedication.pharmacyName}</Typography>
                </Grid>
              )}
              {selectedMedication.notes && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Notes</Typography>
                  <Typography variant="body1">{selectedMedication.notes}</Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default MedicationListPage;
