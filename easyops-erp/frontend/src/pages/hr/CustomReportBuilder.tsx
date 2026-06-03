import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
  Grid,
  Button,
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  Checkbox,
  FormControlLabel,
} from '@mui/material';
import {
  Build,
  Add,
  PlayArrow,
  Save,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService from '../../services/hrService';
import './Hr.css';

const CustomReportBuilder: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reportTypes, setReportTypes] = useState<any[]>([]);
  const [reportData, setReportData] = useState<any>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [reportConfig, setReportConfig] = useState({
    reportType: '',
    reportName: '',
    dataSource: '',
    filters: {} as any,
    columns: [] as string[],
    groupBy: '',
    sortBy: '',
    format: 'json',
  });

  useEffect(() => {
    loadReportTypes();
  }, []);

  const loadReportTypes = async () => {
    try {
      const response = await hrService.getAvailableReportTypes();
      setReportTypes(response.data || []);
    } catch (err: any) {
      console.error('Failed to load report types:', err);
      setError(err.response?.data?.message || 'Failed to load report types');
    }
  };

  const handleBuildReport = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.buildCustomReport({
        ...reportConfig,
        organizationId: currentOrganizationId,
      });
      setReportData(response.data);
      setOpenDialog(false);
    } catch (err: any) {
      console.error('Failed to build report:', err);
      setError(err.response?.data?.message || 'Failed to build report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Custom Report Builder
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Build custom reports with flexible configurations
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setOpenDialog(true)}
        >
          Build New Report
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <CardContent>
          {reportData ? (
            <Box>
              <Typography variant="h6" gutterBottom>
                Report Results
              </Typography>
              <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1, overflow: 'auto' }}>
                <pre style={{ margin: 0, fontSize: '0.875rem' }}>
                  {JSON.stringify(reportData, null, 2)}
                </pre>
              </Box>
            </Box>
          ) : (
            <Alert severity="info">
              Click "Build New Report" to create a custom report
            </Alert>
          )}
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Build Custom Report</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Report Type</InputLabel>
                <Select
                  value={reportConfig.reportType}
                  onChange={(e) => setReportConfig({ ...reportConfig, reportType: e.target.value })}
                  label="Report Type"
                >
                  {reportTypes.map((type: any) => (
                    <MenuItem key={type.value} value={type.value}>
                      {type.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Report Name"
                value={reportConfig.reportName}
                onChange={(e) => setReportConfig({ ...reportConfig, reportName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Data Source"
                value={reportConfig.dataSource}
                onChange={(e) => setReportConfig({ ...reportConfig, dataSource: e.target.value })}
                placeholder="e.g., incentives, provident-fund, sales"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Group By"
                value={reportConfig.groupBy}
                onChange={(e) => setReportConfig({ ...reportConfig, groupBy: e.target.value })}
                placeholder="e.g., department, month, employee"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Sort By"
                value={reportConfig.sortBy}
                onChange={(e) => setReportConfig({ ...reportConfig, sortBy: e.target.value })}
                placeholder="e.g., amount DESC, date ASC"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Output Format</InputLabel>
                <Select
                  value={reportConfig.format}
                  onChange={(e) => setReportConfig({ ...reportConfig, format: e.target.value })}
                  label="Output Format"
                >
                  <MenuItem value="json">JSON</MenuItem>
                  <MenuItem value="csv">CSV</MenuItem>
                  <MenuItem value="pdf">PDF</MenuItem>
                  <MenuItem value="excel">Excel</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Filters (JSON)"
                multiline
                rows={3}
                value={JSON.stringify(reportConfig.filters, null, 2)}
                onChange={(e) => {
                  try {
                    setReportConfig({ ...reportConfig, filters: JSON.parse(e.target.value) });
                  } catch (err) {
                    // Invalid JSON, ignore
                  }
                }}
                placeholder='{"month": 12, "year": 2024}'
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button
            variant="contained"
            startIcon={<PlayArrow />}
            onClick={handleBuildReport}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} /> : 'Build Report'}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default CustomReportBuilder;

