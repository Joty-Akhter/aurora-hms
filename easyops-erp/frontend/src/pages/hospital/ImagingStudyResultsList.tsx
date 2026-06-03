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
  Visibility as ViewIcon,
  Refresh as RefreshIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Image as ImageIcon,
  Print as PrintIcon,
  PictureAsPdf as PdfIcon,
  Timeline as TimelineIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { ImagingStudy } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ImagingStudyResultsList: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const [studies, setStudies] = useState<ImagingStudy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'chronological' | 'modality'>('chronological');
  const [selectedModality, setSelectedModality] = useState<string>('');
  const [selectedStudy, setSelectedStudy] = useState<ImagingStudy | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  
  useEffect(() => {
    if (id) {
      loadStudies();
    }
  }, [id, viewMode]);
  
  const loadStudies = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      if (viewMode === 'chronological') {
        const response = await hospitalService.getImagingStudies(id);
        setStudies(response.data);
      } else if (selectedModality) {
        const response = await hospitalService.getImagingStudiesByModality(id, selectedModality);
        setStudies(response.data);
      }
    } catch (err: any) {
      console.error('Failed to load imaging studies:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load imaging studies'));
      enqueueSnackbar('Failed to load imaging studies', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };
  
  const handleViewDetail = (study: ImagingStudy) => {
    setSelectedStudy(study);
    setShowDetailDialog(true);
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
  
  const handlePrint = async (studyId: string) => {
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
  
  const handleExportPdf = async (studyId: string, accessionNumber?: string) => {
    try {
      const response = await hospitalService.exportReportToPdf(studyId);
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Imaging_Report_${accessionNumber || studyId}.pdf`);
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
  
  const modalities = Array.from(new Set(studies.map(s => s.studyModality)));
  
  if (loading && studies.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Imaging Study Results</Typography>
        <Box display="flex" gap={1}>
          <Button
            variant="contained"
            startIcon={<TimelineIcon />}
            onClick={() => navigate(`/hospital/patients/${id}/imaging-studies/timeline`)}
          >
            Timeline & Trends
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadStudies}
          >
            Refresh
          </Button>
        </Box>
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
              setSelectedModality('');
            }}
            sx={{ mb: 2 }}
          >
            <Tab label="Chronological" value="chronological" />
            <Tab label="By Modality" value="modality" />
          </Tabs>
          
          {viewMode === 'modality' && (
            <FormControl fullWidth sx={{ mt: 2 }}>
              <InputLabel>Select Modality</InputLabel>
              <Select
                value={selectedModality}
                label="Select Modality"
                onChange={(e) => {
                  setSelectedModality(e.target.value);
                  if (e.target.value) {
                    loadStudies();
                  }
                }}
              >
                {modalities.map((modality) => (
                  <MenuItem key={modality} value={modality}>
                    {modality} ({studies.filter(s => s.studyModality === modality).length})
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
              <TableCell>Study Name</TableCell>
              <TableCell>Modality</TableCell>
              <TableCell>Body Part</TableCell>
              <TableCell>Accession Number</TableCell>
              <TableCell>Radiologist</TableCell>
              <TableCell>Report Status</TableCell>
              <TableCell>Study Date</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {studies.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                    No imaging studies found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              studies.map((study) => (
                <TableRow 
                  key={study.studyId} 
                  hover
                  sx={{
                    backgroundColor: study.hasCriticalFindings && !study.isCriticalFindingAcknowledged
                      ? '#ffebee'
                      : 'transparent',
                    borderLeft: study.hasCriticalFindings && !study.isCriticalFindingAcknowledged
                      ? '4px solid #f44336'
                      : 'none',
                  }}
                >
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {study.hasCriticalFindings && !study.isCriticalFindingAcknowledged && (
                        <WarningIcon 
                          fontSize="small" 
                          color="error"
                          sx={{ opacity: 0.7 }}
                        />
                      )}
                      <Typography variant="body2" fontWeight={study.hasCriticalFindings ? 600 : 400}>
                        {study.studyName}
                      </Typography>
                    </Box>
                    {study.cptCode && (
                      <Typography variant="caption" color="text.secondary">
                        CPT: {study.cptCode}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={study.studyModality}
                      size="small"
                      color={getModalityColor(study.studyModality) as any}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {study.bodyPartExamined}
                      {study.laterality && ` (${study.laterality})`}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {study.accessionNumber}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {study.interpretingRadiologistName ? (
                      <Box>
                        <Typography variant="body2">
                          {study.interpretingRadiologistName}
                        </Typography>
                        {study.interpretingRadiologistNpi && (
                          <Typography variant="caption" color="text.secondary">
                            NPI: {study.interpretingRadiologistNpi}
                          </Typography>
                        )}
                      </Box>
                    ) : (
                      <Typography variant="body2" color="text.secondary">-</Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1} flexDirection="column">
                      <Chip
                        label={study.studyStatus}
                        size="small"
                        color={getStatusColor(study.studyStatus) as any}
                      />
                      <Box display="flex" gap={0.5}>
                        {study.isFinal && (
                          <Chip label="Final" size="small" color="success" variant="outlined" />
                        )}
                        {study.isPreliminary && (
                          <Chip label="Preliminary" size="small" color="warning" variant="outlined" />
                        )}
                        {study.isReviewed ? (
                          <CheckCircleIcon fontSize="small" color="success" />
                        ) : (
                          <WarningIcon fontSize="small" color="warning" />
                        )}
                      </Box>
                    </Box>
                  </TableCell>
                  <TableCell>
                    {new Date(study.studyDate).toLocaleDateString()}
                    <Typography variant="caption" display="block" color="text.secondary">
                      {new Date(study.studyDate).toLocaleTimeString()}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Box display="flex" gap={0.5}>
                      <Tooltip title="View Detail">
                        <IconButton
                          size="small"
                          onClick={() => handleViewDetail(study)}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Print Report">
                        <IconButton
                          size="small"
                          onClick={() => handlePrint(study.studyId)}
                        >
                          <PrintIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Export PDF">
                        <IconButton
                          size="small"
                          onClick={() => handleExportPdf(study.studyId, study.accessionNumber)}
                        >
                          <PdfIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {study.imagesAvailable && (
                        <Tooltip title="Images Available">
                          <IconButton size="small" color="primary">
                            <ImageIcon fontSize="small" />
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
        onClose={() => {
          setShowDetailDialog(false);
          setSelectedStudy(null);
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Imaging Study Details</DialogTitle>
        <DialogContent>
          {selectedStudy && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Study Name</Typography>
                <Typography variant="body1">{selectedStudy.studyName}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Modality</Typography>
                <Chip label={selectedStudy.studyModality} size="small" />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Accession Number</Typography>
                <Typography variant="body1" fontFamily="monospace">{selectedStudy.accessionNumber}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">CPT Code</Typography>
                <Typography variant="body1">{selectedStudy.cptCode}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Body Part</Typography>
                <Typography variant="body1">
                  {selectedStudy.bodyPartExamined}
                  {selectedStudy.laterality && ` (${selectedStudy.laterality})`}
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                <Chip
                  label={selectedStudy.studyStatus}
                  size="small"
                  color={getStatusColor(selectedStudy.studyStatus) as any}
                />
              </Grid>
              {selectedStudy.interpretingRadiologistName && (
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="text.secondary">Interpreting Radiologist</Typography>
                  <Typography variant="body1">{selectedStudy.interpretingRadiologistName}</Typography>
                  {selectedStudy.interpretingRadiologistNpi && (
                    <Typography variant="caption" color="text.secondary">
                      NPI: {selectedStudy.interpretingRadiologistNpi}
                    </Typography>
                  )}
                </Grid>
              )}
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="text.secondary">Study Date</Typography>
                <Typography variant="body1">
                  {new Date(selectedStudy.studyDate).toLocaleString()}
                </Typography>
              </Grid>
              {selectedStudy.findings && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Findings</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {selectedStudy.findings}
                  </Typography>
                </Grid>
              )}
              {selectedStudy.impressionConclusion && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Impression</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {selectedStudy.impressionConclusion}
                  </Typography>
                </Grid>
              )}
              {selectedStudy.recommendations && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="text.secondary">Recommendations</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {selectedStudy.recommendations}
                  </Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default ImagingStudyResultsList;
