import React from 'react';
import { Box, Button, Stack, Typography } from '@mui/material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@contexts/AuthContext';
import authService from '@services/authService';
import { getDefaultHomePathFromStoredPermissions } from '@utils/defaultHomePath';

/**
 * Shown when ProtectedRoute denies access or user navigates here after authorization failure.
 * Optional query: ?reason=api — e.g. after a 403 from an API (if the app routes here).
 */
const AccessDenied: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const reason = searchParams.get('reason');
  const { currentOrganizationId, canViewResource, logout } = useAuth();
  const pendingOrgSelection = authService.hasPendingOrganizationSelection();

  const handleSignOut = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const canUseDashboard = canViewResource('dashboard');
  const homePath = getDefaultHomePathFromStoredPermissions();
  const canNavigateHome = homePath !== '/dashboard' || canUseDashboard;

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight="60vh"
      textAlign="center"
      gap={3}
      px={2}
    >
      <Typography variant="h3" component="h1" fontWeight="bold">
        Access denied
      </Typography>
      <Typography variant="body1" color="text.secondary" maxWidth={520}>
        You are signed in, but you do not have the required <strong>permission</strong> for this page or
        action in the current organization. Permissions are assigned by your administrator through roles
        (for example, hospital view, prescribing, or module-specific access).
      </Typography>
      {pendingOrgSelection && (
        <Typography variant="body2" color="text.secondary" maxWidth={520}>
          Finish choosing an organization so your roles and permissions can load for that workspace.
        </Typography>
      )}
      {!pendingOrgSelection && !currentOrganizationId && (
        <Typography variant="body2" color="text.secondary" maxWidth={520}>
          No organization is active for this session. Ask your administrator to assign you to an organization
          and a role (for example <strong>USER</strong> with <strong>DASHBOARD_VIEW</strong>) so the app can load
          your access.
        </Typography>
      )}
      {reason === 'api' && (
        <Typography variant="body2" color="text.secondary" maxWidth={520}>
          This can happen when the server returned <strong>403 Forbidden</strong> — you are authenticated,
          but your roles do not include the operation needed. Ask your administrator to assign the correct
          role or permission in RBAC.
        </Typography>
      )}
      <Typography variant="body2" color="text.secondary" maxWidth={520}>
        If you need access to regulated features (for example electronic prescribing), your administrator may
        need to assign a role such as <strong>Prescribing authority</strong> or the relevant permission codes
        for your organization.
      </Typography>
      <Stack direction="row" spacing={2} flexWrap="wrap" justifyContent="center">
        {pendingOrgSelection ? (
          <Button variant="contained" onClick={() => navigate('/select-organization')}>
            Choose organization
          </Button>
        ) : canNavigateHome ? (
          <Button variant="contained" onClick={() => navigate(homePath)}>
            {homePath === '/dashboard' ? 'Back to dashboard' : 'Go to home'}
          </Button>
        ) : (
          <Button variant="contained" onClick={() => navigate(-1)}>
            Go back
          </Button>
        )}
        <Button variant="outlined" onClick={handleSignOut}>
          Sign out
        </Button>
      </Stack>
    </Box>
  );
};

export default AccessDenied;
