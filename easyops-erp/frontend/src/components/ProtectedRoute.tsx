import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@contexts/AuthContext';
import { Box, CircularProgress } from '@mui/material';
import { getAccessRequirementForPath } from '@utils/accessControl';
import { userHasHospitalPathAccess } from '@utils/hospitalPathAccess';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const location = useLocation();
  const {
    isAuthenticated,
    isLoading,
    canViewResource,
    canManageResource,
    hasAnyPermission,
  } = useAuth();

  if (isLoading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const requirement = getAccessRequirementForPath(location.pathname);

  if (requirement) {
    const { resource, action } = requirement;
    if (resource === 'hospital' && userHasHospitalPathAccess(hasAnyPermission, location.pathname)) {
      return <>{children}</>;
    }
    const needsManage = action === 'manage' || action === 'admin';
    const hasAccess = needsManage
      ? canManageResource(resource)
      : canViewResource(resource) || canManageResource(resource);

    if (!hasAccess) {
      console.warn(`[ProtectedRoute] Access denied for ${location.pathname}`, {
        resource,
        action,
        needsManage,
        hasAccess,
        canView: canViewResource(resource),
        canManage: canManageResource(resource)
      });
      return <Navigate to="/forbidden" replace />;
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;

