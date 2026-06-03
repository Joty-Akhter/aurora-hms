import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  Grid,
  Alert,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
} from '@mui/material';
import { Save as SaveIcon, Send as SendIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';

const PaymentReminders: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [config, setConfig] = useState<any>({
    enabled: false,
    daysBeforeDue: -7,
    daysAfterDueLevel1: 1,
    daysAfterDueLevel2: 7,
    daysAfterDueLevel3: 14,
    emailTemplateBeforeDue: '',
    emailTemplateLevel1: '',
    emailTemplateLevel2: '',
    emailTemplateLevel3: '',
    sendCopyToEmail: '',
  });
  const [history, setHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (organizationId) {
      loadConfig();
      loadHistory();
    }
  }, [organizationId]);

  const loadConfig = async () => {
    setLoading(true);
    try {
      const data = await accountingService.getReminderConfig(organizationId);
      if (data) {
        setConfig(data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load reminder configuration');
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    try {
      const data = await accountingService.getReminderHistory(organizationId, 30);
      setHistory(data);
    } catch (err: any) {
      console.error('Failed to load reminder history:', err);
    }
  };

  const handleSaveConfig = async () => {
    setError(null);
    setSuccess(null);

    try {
      const configData = {
        ...config,
        organizationId,
      };
      
      await accountingService.saveReminderConfig(configData);
      setSuccess('Reminder configuration saved successfully!');
      loadConfig();
    } catch (err: any) {
      setError(err.message || 'Failed to save configuration');
    }
  };

  const handleSendNow = async () => {
    setError(null);
    setSuccess(null);

    try {
      await accountingService.sendRemindersNow(organizationId);
      setSuccess('Reminders sent successfully!');
      loadHistory();
    } catch (err: any) {
      setError(err.message || 'Failed to send reminders');
    }
  };

  const handleRefreshHistory = () => {
    loadHistory();
  };

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Payment Reminders Configuration
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Configuration */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Reminder Settings</Typography>
            <FormControlLabel
              control={
                <Switch
                  checked={config.enabled}
                  onChange={(e) => setConfig({ ...config, enabled: e.target.checked })}
                  color="primary"
                />
              }
              label={config.enabled ? "Enabled" : "Disabled"}
            />
          </Box>

          <Alert severity="info" sx={{ mb: 3 }}>
            Automated reminders run daily at 9:00 AM. Configure the timing and content of reminders below.
          </Alert>

          <Grid container spacing={3}>
            {/* Reminder Timing */}
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Reminder Schedule
              </Typography>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                label="Days Before Due Date"
                type="number"
                value={Math.abs(config.daysBeforeDue || 0)}
                onChange={(e) => setConfig({ ...config, daysBeforeDue: -Math.abs(parseInt(e.target.value) || 0) })}
                fullWidth
                helperText="Send reminder X days before invoice is due"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                label="Level 1 - Days After Due"
                type="number"
                value={config.daysAfterDueLevel1}
                onChange={(e) => setConfig({ ...config, daysAfterDueLevel1: parseInt(e.target.value) || 1 })}
                fullWidth
                helperText="First overdue reminder"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                label="Level 2 - Days After Due"
                type="number"
                value={config.daysAfterDueLevel2}
                onChange={(e) => setConfig({ ...config, daysAfterDueLevel2: parseInt(e.target.value) || 7 })}
                fullWidth
                helperText="Second overdue reminder"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                label="Level 3 - Days After Due"
                type="number"
                value={config.daysAfterDueLevel3}
                onChange={(e) => setConfig({ ...config, daysAfterDueLevel3: parseInt(e.target.value) || 14 })}
                fullWidth
                helperText="Final overdue reminder"
              />
            </Grid>

            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" gutterBottom>
                Email Templates
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                Available variables: {'{customerName}'}, {'{invoiceNumber}'}, {'{invoiceDate}'}, {'{dueDate}'}, {'{totalAmount}'}, {'{balanceDue}'}
              </Typography>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Before Due Date Email"
                value={config.emailTemplateBeforeDue}
                onChange={(e) => setConfig({ ...config, emailTemplateBeforeDue: e.target.value })}
                fullWidth
                multiline
                rows={4}
                helperText="Friendly reminder before invoice is due"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Level 1 Email (Gentle)"
                value={config.emailTemplateLevel1}
                onChange={(e) => setConfig({ ...config, emailTemplateLevel1: e.target.value })}
                fullWidth
                multiline
                rows={4}
                helperText="First overdue reminder - polite tone"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Level 2 Email (Urgent)"
                value={config.emailTemplateLevel2}
                onChange={(e) => setConfig({ ...config, emailTemplateLevel2: e.target.value })}
                fullWidth
                multiline
                rows={4}
                helperText="Second overdue reminder - urgent tone"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Level 3 Email (Final)"
                value={config.emailTemplateLevel3}
                onChange={(e) => setConfig({ ...config, emailTemplateLevel3: e.target.value })}
                fullWidth
                multiline
                rows={4}
                helperText="Final overdue reminder - firm tone"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="CC Email (Optional)"
                type="email"
                value={config.sendCopyToEmail || ''}
                onChange={(e) => setConfig({ ...config, sendCopyToEmail: e.target.value })}
                fullWidth
                helperText="Send a copy of all reminders to this email"
              />
            </Grid>
          </Grid>

          <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
            <Button
              variant="outlined"
              startIcon={<SendIcon />}
              onClick={handleSendNow}
              disabled={!config.enabled}
            >
              Send Reminders Now
            </Button>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              onClick={handleSaveConfig}
            >
              Save Configuration
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Reminder History */}
      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Reminder History (Last 30 Days)</Typography>
            <Button
              variant="outlined"
              size="small"
              startIcon={<RefreshIcon />}
              onClick={handleRefreshHistory}
            >
              Refresh
            </Button>
          </Box>

          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date Sent</TableCell>
                  <TableCell>Invoice</TableCell>
                  <TableCell>Customer</TableCell>
                  <TableCell>Level</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      <CircularProgress size={24} />
                    </TableCell>
                  </TableRow>
                ) : history.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No reminders sent yet
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  history.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>{item.sentDate}</TableCell>
                      <TableCell>{item.invoice?.invoiceNumber}</TableCell>
                      <TableCell>{item.invoice?.customer?.customerName}</TableCell>
                      <TableCell>
                        <Chip 
                          label={
                            item.reminderLevel === 0 ? 'Before Due' : 
                            `Level ${item.reminderLevel}`
                          }
                          size="small"
                          color={
                            item.reminderLevel === 0 ? 'info' :
                            item.reminderLevel === 1 ? 'warning' : 'error'
                          }
                        />
                      </TableCell>
                      <TableCell>{item.recipientEmail}</TableCell>
                      <TableCell>
                        <Chip 
                          label={item.emailSent ? 'Sent' : 'Failed'}
                          size="small"
                          color={item.emailSent ? 'success' : 'error'}
                        />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default PaymentReminders;

