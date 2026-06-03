import React, { useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
} from '@mui/material';
import {
  Lock as LockIcon,
  Person as PersonIcon,
  Business as OrgIcon,
  Security as SecurityIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@contexts/AuthContext';
import authService from '@services/authService';

const PASSWORD_REQUIREMENTS = [
  { key: 'minLength', label: 'At least 8 characters' },
  { key: 'uppercase', label: 'One uppercase letter' },
  { key: 'lowercase', label: 'One lowercase letter' },
  { key: 'digit', label: 'One number' },
  { key: 'special', label: 'One special character' },
] as const;

function evaluatePolicy(password: string) {
  return {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    digit: /\d/.test(password),
    special: /[^A-Za-z0-9]/.test(password),
  };
}

type Section = 'account' | 'password';

const Settings: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState<Section>('account');

  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const checks = useMemo(() => evaluatePolicy(form.newPassword), [form.newPassword]);
  const meetsPolicy = PASSWORD_REQUIREMENTS.every((r) => checks[r.key]);
  const mismatch = form.confirmPassword.length > 0 && form.newPassword !== form.confirmPassword;
  const showChecks = form.newPassword.length > 0;

  const handleChange = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    setSuccessMsg('');
    setErrorMsg('');
  };

  const handleSubmit = async () => {
    if (!user?.id) return;
    if (!meetsPolicy) { setErrorMsg('New password does not meet policy requirements.'); return; }
    if (mismatch || form.newPassword !== form.confirmPassword) { setErrorMsg('Passwords do not match.'); return; }
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    try {
      await authService.changePassword(user.id, { newPassword: form.newPassword });
      setSuccessMsg('Password changed successfully.');
      setForm({ newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      const msg = err?.response?.data?.error || err?.response?.data?.message || 'Failed to change password.';
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  };

  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.username || '';

  return (
    <Box display="flex" gap={3} alignItems="flex-start">
      {/* Sidebar */}
      <Card sx={{ minWidth: 220, flexShrink: 0 }}>
        <CardContent sx={{ p: 0 }}>
          <Typography variant="subtitle2" sx={{ px: 2, pt: 2, pb: 1, color: 'text.secondary' }}>
            Settings
          </Typography>
          <List dense disablePadding>
            <ListItemButton selected={activeSection === 'account'} onClick={() => setActiveSection('account')}>
              <ListItemIcon sx={{ minWidth: 36 }}><PersonIcon fontSize="small" /></ListItemIcon>
              <ListItemText primary="Account" />
            </ListItemButton>
            <ListItemButton selected={activeSection === 'password'} onClick={() => setActiveSection('password')}>
              <ListItemIcon sx={{ minWidth: 36 }}><LockIcon fontSize="small" /></ListItemIcon>
              <ListItemText primary="Change Password" />
            </ListItemButton>
            <Divider />
            <ListItem>
              <ListItemButton onClick={() => navigate('/organizations')}>
                <ListItemIcon sx={{ minWidth: 36 }}><OrgIcon fontSize="small" /></ListItemIcon>
                <ListItemText primary="Organizations" primaryTypographyProps={{ variant: 'body2' }} />
              </ListItemButton>
            </ListItem>
            <ListItem>
              <ListItemButton onClick={() => navigate('/roles')}>
                <ListItemIcon sx={{ minWidth: 36 }}><SecurityIcon fontSize="small" /></ListItemIcon>
                <ListItemText primary="Roles & Permissions" primaryTypographyProps={{ variant: 'body2' }} />
              </ListItemButton>
            </ListItem>
          </List>
        </CardContent>
      </Card>

      {/* Main content */}
      <Box flex={1} maxWidth={520}>
        {activeSection === 'account' && (
          <>
            <Typography variant="h5" fontWeight="bold" gutterBottom>Account</Typography>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Profile Information</Typography>
                <Typography><strong>Name:</strong> {fullName}</Typography>
                <Typography><strong>Username:</strong> {user?.username}</Typography>
                <Typography><strong>Email:</strong> {user?.email || '—'}</Typography>
              </CardContent>
            </Card>
          </>
        )}

        {activeSection === 'password' && (
          <>
            <Typography variant="h5" fontWeight="bold" gutterBottom>Change Password</Typography>
            <Card>
              <CardContent>
                {successMsg && <Alert severity="success" sx={{ mb: 2 }}>{successMsg}</Alert>}
                {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}

                <TextField
                  label="New Password"
                  type="password"
                  fullWidth
                  value={form.newPassword}
                  onChange={handleChange('newPassword')}
                  sx={{ mb: 1 }}
                />
                {showChecks && (
                  <Box sx={{ mb: 2, pl: 1 }}>
                    {PASSWORD_REQUIREMENTS.map((r) => (
                      <Typography
                        key={r.key}
                        variant="caption"
                        display="block"
                        sx={{ color: checks[r.key] ? 'success.main' : 'text.secondary' }}
                      >
                        {checks[r.key] ? '✓' : '○'} {r.label}
                      </Typography>
                    ))}
                  </Box>
                )}
                <TextField
                  label="Confirm New Password"
                  type="password"
                  fullWidth
                  value={form.confirmPassword}
                  onChange={handleChange('confirmPassword')}
                  error={mismatch}
                  helperText={mismatch ? 'Passwords do not match' : undefined}
                  sx={{ mb: 2 }}
                />
                <Button
                  variant="contained"
                  onClick={handleSubmit}
                  disabled={loading || !meetsPolicy || mismatch}
                  startIcon={loading ? <CircularProgress size={18} /> : undefined}
                >
                  {loading ? 'Changing...' : 'Change Password'}
                </Button>
              </CardContent>
            </Card>
          </>
        )}
      </Box>
    </Box>
  );
};

export default Settings;
