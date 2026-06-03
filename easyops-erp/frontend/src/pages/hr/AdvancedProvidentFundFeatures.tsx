import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
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
} from '@mui/material';
import {
  Lightbulb,
  TrendingUp,
  Assessment,
  Analytics,
  Security,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import hrService, {
  getMyEmployeeProfile,
  getEmployees,
  type Employee,
} from '../../services/hrService';
import './Hr.css';

const AdvancedProvidentFundFeatures: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [tabValue, setTabValue] = useState(0);
  const [recommendations, setRecommendations] = useState<any>(null);
  const [optimization, setOptimization] = useState<any>(null);
  const [forecast, setForecast] = useState<any>(null);
  const [riskAssessment, setRiskAssessment] = useState<any>(null);
  const [compliance, setCompliance] = useState<any>(null);
  const [analytics, setAnalytics] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openForecastDialog, setOpenForecastDialog] = useState(false);
  const [forecastParams, setForecastParams] = useState({
    epfAccountId: '',
    months: 12,
    interestRate: '8.50',
  });

  const [linkedEmployeeId, setLinkedEmployeeId] = useState<string | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);

  useEffect(() => {
    if (!currentOrganizationId || !user?.id) {
      setLinkedEmployeeId(null);
      setProfileLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setProfileLoading(true);
      try {
        const meRes = await getMyEmployeeProfile(currentOrganizationId);
        const me = meRes.data as Employee | undefined;
        const id = me?.employeeId ?? me?.id ?? null;
        if (!cancelled) {
          setLinkedEmployeeId(id);
        }
      } catch {
        try {
          const emRes = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
          const list = (emRes.data || []) as Employee[];
          const match = list.find((e) => e.userId === user.id);
          const id = match?.employeeId ?? match?.id ?? null;
          if (!cancelled) setLinkedEmployeeId(id);
        } catch {
          if (!cancelled) setLinkedEmployeeId(null);
        }
      } finally {
        if (!cancelled) setProfileLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId, user?.id]);

  useEffect(() => {
    if (tabValue !== 0 || !currentOrganizationId || profileLoading) return;
    if (!linkedEmployeeId) {
      setRecommendations(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await hrService.getProvidentFundRecommendations(linkedEmployeeId, currentOrganizationId);
        if (!cancelled) setRecommendations(response.data);
      } catch (err: any) {
        console.error('Failed to load recommendations:', err);
        if (!cancelled) setError(err.response?.data?.message || 'Failed to load recommendations');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tabValue, currentOrganizationId, linkedEmployeeId, profileLoading]);

  const loadOptimization = async (accountId: string) => {
    try {
      setLoading(true);
      const response = await hrService.optimizeProvidentFundContributions(accountId);
      setOptimization(response.data);
    } catch (err: any) {
      console.error('Failed to load optimization:', err);
      setError(err.response?.data?.message || 'Failed to load optimization');
    } finally {
      setLoading(false);
    }
  };

  const loadForecast = async () => {
    if (!forecastParams.epfAccountId) {
      setError('Please provide EPF Account ID');
      return;
    }
    try {
      setLoading(true);
      const response = await hrService.forecastProvidentFund(
        forecastParams.epfAccountId,
        forecastParams.months,
        parseFloat(forecastParams.interestRate)
      );
      setForecast(response.data);
      setOpenForecastDialog(false);
    } catch (err: any) {
      console.error('Failed to load forecast:', err);
      setError(err.response?.data?.message || 'Failed to load forecast');
    } finally {
      setLoading(false);
    }
  };

  const loadRiskAssessment = async (accountId: string) => {
    try {
      setLoading(true);
      const response = await hrService.assessProvidentFundRisk(accountId);
      setRiskAssessment(response.data);
    } catch (err: any) {
      console.error('Failed to load risk assessment:', err);
      setError(err.response?.data?.message || 'Failed to load risk assessment');
    } finally {
      setLoading(false);
    }
  };

  const loadCompliance = async (month: number, year: number) => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await hrService.checkProvidentFundCompliance(currentOrganizationId, month, year);
      setCompliance(response.data);
    } catch (err: any) {
      console.error('Failed to load compliance:', err);
      setError(err.response?.data?.message || 'Failed to load compliance');
    } finally {
      setLoading(false);
    }
  };

  const loadAnalytics = async (type: string) => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      let response;
      if (type === 'participation') {
        response = await hrService.getProvidentFundParticipationMetrics(currentOrganizationId);
      } else if (type === 'costs') {
        response = await hrService.getProvidentFundCostAnalysis(currentOrganizationId, new Date().getFullYear());
      } else if (type === 'roi') {
        response = await hrService.getProvidentFundROI(currentOrganizationId, new Date().getFullYear().toString());
      } else {
        response = await hrService.getProvidentFundImpactAnalysis(currentOrganizationId);
      }
      setAnalytics(response.data);
    } catch (err: any) {
      console.error('Failed to load analytics:', err);
      setError(err.response?.data?.message || 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  };

  if ((loading || profileLoading) && tabValue === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="hr-page">
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Advanced Provident Fund Features
        </Typography>
        <Typography variant="body2" color="text.secondary">
          AI recommendations, optimization, forecasting, risk assessment, compliance, and analytics
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            <Tab label="AI Recommendations" icon={<Lightbulb />} />
            <Tab label="Optimization" icon={<TrendingUp />} />
            <Tab label="Forecasting" icon={<Analytics />} />
            <Tab label="Risk Assessment" icon={<Security />} />
            <Tab label="Compliance" icon={<Assessment />} />
            <Tab label="Analytics" icon={<Analytics />} />
          </Tabs>
        </Box>

        <CardContent>
          {tabValue === 0 && (
            <>
              <Typography variant="h6" gutterBottom>
                AI-Powered Provident Fund Recommendations
              </Typography>
              {recommendations ? (
                <Grid container spacing={2}>
                  {Object.entries(recommendations).map(([key, value]) => (
                    <Grid item xs={12} md={6} key={key}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {key.replace(/_/g, ' ').toUpperCase()}
                          </Typography>
                          <Typography variant="body2" sx={{ mt: 1 }}>
                            {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : !linkedEmployeeId ? (
                <Alert severity="warning">
                  No employee profile is linked to your account. Link an HR employee record to load personalized
                  recommendations.
                </Alert>
              ) : (
                <Alert severity="info">No recommendations available</Alert>
              )}
            </>
          )}

          {tabValue === 1 && (
            <>
              <Typography variant="h6" gutterBottom>
                Contribution Optimization
              </Typography>
              <TextField
                fullWidth
                label="EPF Account ID"
                sx={{ mb: 2 }}
                onBlur={(e) => {
                  if (e.target.value) {
                    loadOptimization(e.target.value);
                  }
                }}
              />
              {optimization ? (
                <Grid container spacing={2}>
                  {Object.entries(optimization).map(([key, value]) => (
                    <Grid item xs={12} md={6} key={key}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {key.replace(/([A-Z])/g, ' $1').trim()}
                          </Typography>
                          <Typography variant="body2" sx={{ mt: 1 }}>
                            {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Alert severity="info">Enter an account ID and click outside to load optimization</Alert>
              )}
            </>
          )}

          {tabValue === 2 && (
            <>
              <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6">Provident Fund Forecasting</Typography>
                <Button
                  variant="contained"
                  onClick={() => setOpenForecastDialog(true)}
                >
                  Generate Forecast
                </Button>
              </Box>
              {forecast ? (
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Projected Balance
                        </Typography>
                        <Typography variant="h5">
                          ₹{(forecast.projectedBalance || 0).toLocaleString('en-IN')}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Initial Balance
                        </Typography>
                        <Typography variant="h5">
                          ₹{(forecast.initialBalance || 0).toLocaleString('en-IN')}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                </Grid>
              ) : (
                <Alert severity="info">Click "Generate Forecast" to create a forecast</Alert>
              )}
            </>
          )}

          {tabValue === 3 && (
            <>
              <Typography variant="h6" gutterBottom>
                Risk Assessment
              </Typography>
              <TextField
                fullWidth
                label="EPF Account ID"
                sx={{ mb: 2 }}
                onBlur={(e) => {
                  if (e.target.value) {
                    loadRiskAssessment(e.target.value);
                  }
                }}
              />
              {riskAssessment ? (
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Risk Level
                        </Typography>
                        <Typography variant="h5" color={
                          riskAssessment.riskLevel === 'High' ? 'error.main' :
                          riskAssessment.riskLevel === 'Medium' ? 'warning.main' : 'success.main'
                        }>
                          {riskAssessment.riskLevel}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Risk Score
                        </Typography>
                        <Typography variant="h5">
                          {riskAssessment.riskScore || 0}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  {riskAssessment.riskFactors && riskAssessment.riskFactors.length > 0 && (
                    <Grid item xs={12}>
                      <Card>
                        <CardContent>
                          <Typography variant="subtitle2" gutterBottom>
                            Risk Factors
                          </Typography>
                          {riskAssessment.riskFactors.map((factor: string, index: number) => (
                            <Typography key={index} variant="body2" color="error">
                              • {factor}
                            </Typography>
                          ))}
                        </CardContent>
                      </Card>
                    </Grid>
                  )}
                </Grid>
              ) : (
                <Alert severity="info">Enter an account ID and click outside to assess risk</Alert>
              )}
            </>
          )}

          {tabValue === 4 && (
            <>
              <Typography variant="h6" gutterBottom>
                Compliance Management
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Month"
                    type="number"
                    defaultValue={new Date().getMonth() + 1}
                    onChange={(e) => {
                      const month = parseInt(e.target.value);
                      const year = new Date().getFullYear();
                      loadCompliance(month, year);
                    }}
                    select
                  >
                    {Array.from({ length: 12 }, (_, i) => i + 1).map((month) => (
                      <MenuItem key={month} value={month}>
                        {month}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Year"
                    type="number"
                    defaultValue={new Date().getFullYear()}
                    onChange={(e) => {
                      const month = new Date().getMonth() + 1;
                      const year = parseInt(e.target.value);
                      loadCompliance(month, year);
                    }}
                  />
                </Grid>
              </Grid>
              {compliance && (
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Compliance Status
                        </Typography>
                        <Typography variant="h5" color={
                          compliance.overallCompliant ? 'success.main' : 'error.main'
                        }>
                          {compliance.overallCompliant ? 'Compliant' : 'Non-Compliant'}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Card>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                          Non-Compliant Contributions
                        </Typography>
                        <Typography variant="h5">
                          {compliance.nonCompliantContributions?.length || 0}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                </Grid>
              )}
            </>
          )}

          {tabValue === 5 && (
            <>
              <Typography variant="h6" gutterBottom>
                Provident Fund Analytics
              </Typography>
              <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
                <Button variant="outlined" onClick={() => loadAnalytics('participation')}>
                  Participation Metrics
                </Button>
                <Button variant="outlined" onClick={() => loadAnalytics('costs')}>
                  Cost Analysis
                </Button>
                <Button variant="outlined" onClick={() => loadAnalytics('roi')}>
                  ROI Measurement
                </Button>
                <Button variant="outlined" onClick={() => loadAnalytics('impact')}>
                  Impact Analysis
                </Button>
              </Box>
              {analytics && (
                <Grid container spacing={2}>
                  {Object.entries(analytics).map(([key, value]) => (
                    <Grid item xs={12} md={6} key={key}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {key.replace(/([A-Z])/g, ' $1').trim()}
                          </Typography>
                          <Typography variant="body2" sx={{ mt: 1 }}>
                            {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={openForecastDialog} onClose={() => setOpenForecastDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate Forecast</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="EPF Account ID"
                value={forecastParams.epfAccountId}
                onChange={(e) => setForecastParams({ ...forecastParams, epfAccountId: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Months to Forecast"
                type="number"
                value={forecastParams.months}
                onChange={(e) => setForecastParams({ ...forecastParams, months: parseInt(e.target.value) })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Annual Interest Rate (%)"
                type="number"
                value={forecastParams.interestRate}
                onChange={(e) => setForecastParams({ ...forecastParams, interestRate: e.target.value })}
                required
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenForecastDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={loadForecast} disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Generate'}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default AdvancedProvidentFundFeatures;

