import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Tabs,
  Tab,
  TextField,
  Grid,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  LinearProgress,
} from '@mui/material';
import {
  Assessment as AssessmentIcon,
  Download as DownloadIcon,
  Print as PrintIcon,
} from '@mui/icons-material';
import hospitalService, {
  MedicationListReport,
  MedicationIndicationReport,
  MedicationAdherenceReport,
  MedicationCompletenessMetrics,
  MedicationClinicalReport,
  MedicationQualityMetrics,
} from '../../services/hospitalService';
import { format } from 'date-fns';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const MedicationReporting: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [patientId, setPatientId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Report data states
  const [completeReport, setCompleteReport] = useState<MedicationListReport | null>(null);
  const [currentReport, setCurrentReport] = useState<MedicationListReport | null>(null);
  const [historicalReport, setHistoricalReport] = useState<MedicationListReport | null>(null);
  const [indicationReport, setIndicationReport] = useState<MedicationIndicationReport | null>(null);
  const [adherenceReport, setAdherenceReport] = useState<MedicationAdherenceReport | null>(null);
  const [completenessMetrics, setCompletenessMetrics] = useState<MedicationCompletenessMetrics | null>(null);
  const [providerReport, setProviderReport] = useState<MedicationClinicalReport | null>(null);
  const [problemReport, setProblemReport] = useState<MedicationClinicalReport | null>(null);
  const [qualityMetrics, setQualityMetrics] = useState<MedicationQualityMetrics | null>(null);
  
  const [indicationFilter, setIndicationFilter] = useState('');

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const loadReport = async (reportType: string) => {
    if (!patientId) {
      setError('Please enter a patient ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      switch (reportType) {
        case 'complete':
          const complete = await hospitalService.generateCompleteMedicationListReport(patientId, startDate || undefined, endDate || undefined);
          setCompleteReport(complete.data);
          break;
        case 'current':
          const current = await hospitalService.generateCurrentMedicationListReport(patientId);
          setCurrentReport(current.data);
          break;
        case 'historical':
          const historical = await hospitalService.generateHistoricalMedicationListReport(patientId, startDate || undefined, endDate || undefined);
          setHistoricalReport(historical.data);
          break;
        case 'indication':
          const indication = await hospitalService.generateMedicationsByIndicationReport(patientId, indicationFilter || undefined, startDate || undefined, endDate || undefined);
          setIndicationReport(indication.data);
          break;
        case 'adherence':
          const adherence = await hospitalService.generateMedicationAdherenceReport(patientId, startDate || undefined, endDate || undefined);
          setAdherenceReport(adherence.data);
          break;
        case 'completeness':
          const completeness = await hospitalService.generateMedicationCompletenessMetrics(patientId);
          setCompletenessMetrics(completeness.data);
          break;
        case 'provider':
          const provider = await hospitalService.generateMedicationsByProviderReport(patientId, startDate || undefined, endDate || undefined);
          setProviderReport(provider.data);
          break;
        case 'problem':
          const problem = await hospitalService.generateMedicationsByProblemReport(patientId, startDate || undefined, endDate || undefined);
          setProblemReport(problem.data);
          break;
        case 'quality':
          const quality = await hospitalService.generateMedicationQualityMetrics(patientId, startDate || undefined, endDate || undefined);
          setQualityMetrics(quality.data);
          break;
      }
    } catch (err: any) {
      setError(ehrApiErrorMessage(err, 'Failed to load report'));
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = () => {
    const reportTypes = ['complete', 'current', 'historical', 'indication', 'adherence', 'completeness', 'provider', 'problem', 'quality'];
    const currentType = reportTypes[tabValue];
    loadReport(currentType);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <AssessmentIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
        Medication Reporting and Analytics
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Patient ID"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                required
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="date"
                label="Start Date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                type="date"
                label="End Date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleGenerateReport}
                disabled={loading || !patientId}
                startIcon={loading ? <CircularProgress size={20} /> : <AssessmentIcon />}
              >
                Generate Report
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab label="Complete List" />
            <Tab label="Current List" />
            <Tab label="Historical List" />
            <Tab label="By Indication" />
            <Tab label="Adherence" />
            <Tab label="Completeness" />
            <Tab label="By Provider" />
            <Tab label="By Problem" />
            <Tab label="Quality Metrics" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          {completeReport && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Complete Medication List Report
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Total Medications</Typography>
                      <Typography variant="h4">{completeReport.totalMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Active</Typography>
                      <Typography variant="h4" color="success.main">{completeReport.activeMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Discontinued</Typography>
                      <Typography variant="h4" color="error.main">{completeReport.discontinuedMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">On Hold</Typography>
                      <Typography variant="h4" color="warning.main">{completeReport.onHoldMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Start Date</TableCell>
                      <TableCell>Indication</TableCell>
                      <TableCell>Provider</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {completeReport.medications.map((med) => (
                      <TableRow key={med.medicationId}>
                        <TableCell>{med.medicationName}</TableCell>
                        <TableCell>
                          <Chip
                            label={med.medicationStatus}
                            color={med.medicationStatus === 'ACTIVE' ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{med.startDate ? format(new Date(med.startDate), 'MM/dd/yyyy') : '-'}</TableCell>
                        <TableCell>{med.indication || '-'}</TableCell>
                        <TableCell>{med.prescribingProviderName || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          {currentReport && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Current Medication List Report
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Total Medications</Typography>
                      <Typography variant="h4">{currentReport.totalMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Active</Typography>
                      <Typography variant="h4" color="success.main">{currentReport.activeMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Report Date</Typography>
                      <Typography variant="h6">{currentReport.reportDate ? format(new Date(currentReport.reportDate), 'MM/dd/yyyy') : '-'}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Dosage</TableCell>
                      <TableCell>Frequency</TableCell>
                      <TableCell>Indication</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {currentReport.medications.map((med) => (
                      <TableRow key={med.medicationId}>
                        <TableCell>{med.medicationName}</TableCell>
                        <TableCell>
                          <Chip
                            label={med.medicationStatus}
                            color={med.medicationStatus === 'ACTIVE' ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{med.dosageStrength} {med.dosageUnit}</TableCell>
                        <TableCell>{med.frequency || '-'}</TableCell>
                        <TableCell>{med.indication || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          {historicalReport && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Historical Medication List Report
              </Typography>
              <Typography variant="body1" color="textSecondary" gutterBottom>
                Total Historical Medications: {historicalReport.totalMedications}
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Start Date</TableCell>
                      <TableCell>End Date</TableCell>
                      <TableCell>Indication</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {historicalReport.medications.map((med) => (
                      <TableRow key={med.medicationId}>
                        <TableCell>{med.medicationName}</TableCell>
                        <TableCell>
                          <Chip label={med.medicationStatus} size="small" />
                        </TableCell>
                        <TableCell>{med.startDate ? format(new Date(med.startDate), 'MM/dd/yyyy') : '-'}</TableCell>
                        <TableCell>{med.endDate ? format(new Date(med.endDate), 'MM/dd/yyyy') : '-'}</TableCell>
                        <TableCell>{med.indication || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          <Box sx={{ mb: 2 }}>
            <TextField
              label="Indication Filter"
              value={indicationFilter}
              onChange={(e) => setIndicationFilter(e.target.value)}
              placeholder="Enter indication (optional)"
              sx={{ mb: 2 }}
            />
          </Box>
          {indicationReport && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medications by Indication Report
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                {indicationReport.indicationSummaries.map((summary) => (
                  <Grid item xs={12} md={4} key={summary.indication}>
                    <Card>
                      <CardContent>
                        <Typography variant="h6">{summary.indication}</Typography>
                        <Typography variant="body2">Total: {summary.medicationCount}</Typography>
                        <Typography variant="body2" color="success.main">Active: {summary.activeCount}</Typography>
                        <Typography variant="body2" color="error.main">Discontinued: {summary.discontinuedCount}</Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Indication</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Start Date</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {indicationReport.medications.map((med) => (
                      <TableRow key={med.medicationId}>
                        <TableCell>{med.medicationName}</TableCell>
                        <TableCell>{med.indication || '-'}</TableCell>
                        <TableCell>
                          <Chip
                            label={med.medicationStatus}
                            color={med.medicationStatus === 'ACTIVE' ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{med.startDate ? format(new Date(med.startDate), 'MM/dd/yyyy') : '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          {adherenceReport && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medication Adherence Report
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Overall Adherence Rate</Typography>
                      <Typography variant="h4" color="primary.main">
                        {adherenceReport.overallAdherenceRate.toFixed(1)}%
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={adherenceReport.overallAdherenceRate}
                        sx={{ mt: 1 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Adherent Medications</Typography>
                      <Typography variant="h4" color="success.main">{adherenceReport.adherentMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Non-Adherent Medications</Typography>
                      <Typography variant="h4" color="error.main">{adherenceReport.nonAdherentMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Adherence Rate</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Expected Doses</TableCell>
                      <TableCell>Actual Doses</TableCell>
                      <TableCell>Missed Doses</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {adherenceReport.medicationDetails.map((detail) => (
                      <TableRow key={detail.medicationId}>
                        <TableCell>{detail.medicationName}</TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Typography variant="body2" sx={{ mr: 1 }}>
                              {detail.adherenceRate.toFixed(1)}%
                            </Typography>
                            <LinearProgress
                              variant="determinate"
                              value={detail.adherenceRate}
                              sx={{ width: 100 }}
                              color={detail.adherenceRate >= 80 ? 'success' : detail.adherenceRate >= 50 ? 'warning' : 'error'}
                            />
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={detail.adherenceStatus}
                            color={
                              detail.adherenceStatus === 'ADHERENT' ? 'success' :
                              detail.adherenceStatus === 'PARTIAL' ? 'warning' : 'error'
                            }
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{detail.expectedDoses}</TableCell>
                        <TableCell>{detail.actualDoses}</TableCell>
                        <TableCell>{detail.missedDoses}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={5}>
          {completenessMetrics && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medication List Completeness Metrics
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Overall Completeness Score</Typography>
                      <Typography variant="h4" color="primary.main">
                        {completenessMetrics.completenessScore.toFixed(1)}%
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={completenessMetrics.completenessScore}
                        sx={{ mt: 1 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Complete Medications</Typography>
                      <Typography variant="h4" color="success.main">{completenessMetrics.completeMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Incomplete Medications</Typography>
                      <Typography variant="h4" color="error.main">{completenessMetrics.incompleteMedications}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
              <TableContainer component={Paper} sx={{ mb: 2 }}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Medication</TableCell>
                      <TableCell>Completeness Score</TableCell>
                      <TableCell>Missing Fields</TableCell>
                      <TableCell>Incomplete Fields</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {completenessMetrics.completenessDetails.map((detail) => (
                      <TableRow key={detail.medicationId}>
                        <TableCell>{detail.medicationName}</TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Typography variant="body2" sx={{ mr: 1 }}>
                              {detail.completenessScore.toFixed(1)}%
                            </Typography>
                            <LinearProgress
                              variant="determinate"
                              value={detail.completenessScore}
                              sx={{ width: 100 }}
                              color={detail.completenessScore >= 90 ? 'success' : 'warning'}
                            />
                          </Box>
                        </TableCell>
                        <TableCell>{detail.missingFields.join(', ') || 'None'}</TableCell>
                        <TableCell>{detail.incompleteFields.join(', ') || 'None'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={6}>
          {providerReport && providerReport.providerSummaries && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medications by Provider Report
              </Typography>
              {providerReport.providerSummaries.map((summary) => (
                <Card key={summary.providerId} sx={{ mb: 2 }}>
                  <CardContent>
                    <Typography variant="h6">{summary.providerName}</Typography>
                    <Typography variant="body2" color="textSecondary">NPI: {summary.providerNpi || 'N/A'}</Typography>
                    <Typography variant="body2">Total Medications: {summary.totalMedications}</Typography>
                    <Typography variant="body2" color="success.main">Active: {summary.activeMedications}</Typography>
                    <TableContainer component={Paper} sx={{ mt: 2 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Medication</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Indication</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {summary.medications.map((med) => (
                            <TableRow key={med.medicationId}>
                              <TableCell>{med.medicationName}</TableCell>
                              <TableCell>
                                <Chip
                                  label={med.medicationStatus}
                                  color={med.medicationStatus === 'ACTIVE' ? 'success' : 'default'}
                                  size="small"
                                />
                              </TableCell>
                              <TableCell>{med.indication || '-'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              ))}
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={7}>
          {problemReport && problemReport.problemSummaries && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medications by Problem Report
              </Typography>
              {problemReport.problemSummaries.map((summary) => (
                <Card key={summary.problemId} sx={{ mb: 2 }}>
                  <CardContent>
                    <Typography variant="h6">{summary.problemName}</Typography>
                    <Typography variant="body2" color="textSecondary">Diagnosis Code: {summary.diagnosisCode || 'N/A'}</Typography>
                    <Typography variant="body2">Total Medications: {summary.totalMedications}</Typography>
                    <Typography variant="body2" color="success.main">Active: {summary.activeMedications}</Typography>
                    <TableContainer component={Paper} sx={{ mt: 2 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Medication</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Indication</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {summary.medications.map((med) => (
                            <TableRow key={med.medicationId}>
                              <TableCell>{med.medicationName}</TableCell>
                              <TableCell>
                                <Chip
                                  label={med.medicationStatus}
                                  color={med.medicationStatus === 'ACTIVE' ? 'success' : 'default'}
                                  size="small"
                                />
                              </TableCell>
                              <TableCell>{med.indication || '-'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              ))}
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={8}>
          {qualityMetrics && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Medication Quality Metrics
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Overall Quality Score</Typography>
                      <Typography variant="h4" color="primary.main">
                        {qualityMetrics.overallQualityScore.toFixed(1)}%
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={qualityMetrics.overallQualityScore}
                        sx={{ mt: 1 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Data Quality Score</Typography>
                      <Typography variant="h4" color="success.main">
                        {qualityMetrics.medicationListQuality.dataQualityScore.toFixed(1)}%
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Card>
                    <CardContent>
                      <Typography variant="body2" color="textSecondary">Reconciliation Compliance</Typography>
                      <Typography variant="h4" color="info.main">
                        {qualityMetrics.reconciliationCompliance.complianceRate.toFixed(1)}%
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
              
              <Card sx={{ mb: 2 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Data Quality Details</Typography>
                  <Typography variant="body2">Total Medications: {qualityMetrics.medicationListQuality.totalMedications}</Typography>
                  <Typography variant="body2" color="success.main">
                    Complete Data: {qualityMetrics.medicationListQuality.medicationsWithCompleteData}
                  </Typography>
                  <Typography variant="body2" color="error.main">
                    Missing Data: {qualityMetrics.medicationListQuality.medicationsWithMissingData}
                  </Typography>
                  <Typography variant="body2" color="warning.main">
                    Duplicates: {qualityMetrics.medicationListQuality.duplicateMedications}
                  </Typography>
                </CardContent>
              </Card>

              <Card sx={{ mb: 2 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Reconciliation Compliance</Typography>
                  <Typography variant="body2">Total Reconciliations: {qualityMetrics.reconciliationCompliance.totalReconciliations}</Typography>
                  <Typography variant="body2" color="success.main">
                    Completed: {qualityMetrics.reconciliationCompliance.completedReconciliations}
                  </Typography>
                  <Typography variant="body2" color="warning.main">
                    Pending: {qualityMetrics.reconciliationCompliance.pendingReconciliations}
                  </Typography>
                  <Typography variant="body2" color="error.main">
                    Overdue: {qualityMetrics.reconciliationCompliance.overdueReconciliations}
                  </Typography>
                  {qualityMetrics.reconciliationCompliance.lastReconciliationDate && (
                    <Typography variant="body2">
                      Last Reconciliation: {format(new Date(qualityMetrics.reconciliationCompliance.lastReconciliationDate), 'MM/dd/yyyy')}
                    </Typography>
                  )}
                </CardContent>
              </Card>

              {qualityMetrics.qualityIssues.length > 0 && (
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>Quality Issues</Typography>
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Issue Type</TableCell>
                            <TableCell>Severity</TableCell>
                            <TableCell>Medication</TableCell>
                            <TableCell>Description</TableCell>
                            <TableCell>Recommendation</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {qualityMetrics.qualityIssues.map((issue, idx) => (
                            <TableRow key={idx}>
                              <TableCell>{issue.issueType}</TableCell>
                              <TableCell>
                                <Chip
                                  label={issue.severity}
                                  color={
                                    issue.severity === 'CRITICAL' ? 'error' :
                                    issue.severity === 'HIGH' ? 'warning' : 'default'
                                  }
                                  size="small"
                                />
                              </TableCell>
                              <TableCell>{issue.medicationName}</TableCell>
                              <TableCell>{issue.description}</TableCell>
                              <TableCell>{issue.recommendation}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
        </TabPanel>
      </Card>
    </Box>
  );
};

export default MedicationReporting;
