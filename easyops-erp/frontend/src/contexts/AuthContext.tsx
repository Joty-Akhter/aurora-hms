import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import authService, { DEFAULT_THEME } from '@services/authService';
import rbacService from '@services/rbacService';
import { LoginRequest, LoginResponse, Permission, Role } from '../types/index';

/**
 * Platform routes (organizations, users, roles, permissions) can also be allowed via global
 * `system` permissions (e.g. SYSTEM_VIEW, SYSTEM_CONFIG), so admins do not need org-scoped
 * ORG_* / USER_* / ROLE_* permissions for those screens.
 */
const PLATFORM_RESOURCES_USING_SYSTEM_FALLBACK = new Set([
  'organizations',
  'users',
  'roles',
  'permissions',
]);

interface AuthContextType {
  user: any | null;
  currentOrganizationId: string | null;
  currentOrganizationName: string | null;
  currentOrganizationLogo: string | null;
  roles: string[];
  permissions: Permission[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  refreshAuth: () => void;
  setCurrentOrganization: (organizationId: string, organizationName: string, organizationLogo?: string | null, themeOverrides?: { themeMode?: string; themePrimaryColor?: string; themeSecondaryColor?: string; themeAccentColor?: string; themeSidebarColor?: string; themeSidebarTextColor?: string }) => void;
  setGlobalWorkspace: () => void;
  hasRole: (roleCode: string) => boolean;
  hasPermission: (permissionCode: string) => boolean;
  hasAnyPermission: (permissionCodes: string[]) => boolean;
  canViewResource: (resource: string) => boolean;
  canManageResource: (resource: string) => boolean;
  reloadRbacState: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<any | null>(null);
  const [currentOrganizationId, setCurrentOrganizationId] = useState<string | null>(null);
  const [currentOrganizationName, setCurrentOrganizationName] = useState<string | null>(null);
  const [currentOrganizationLogo, setCurrentOrganizationLogo] = useState<string | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadStoredRbacState = useCallback(() => {
    try {
      const storedRoles = localStorage.getItem('userRoles');
      if (storedRoles) {
        const parsedRoles = JSON.parse(storedRoles);
        if (Array.isArray(parsedRoles)) {
          setRoles(parsedRoles);
        }
      }
    } catch (error) {
      console.error('Failed to parse stored role data', error);
      setRoles([]);
    }

    try {
      const storedPermissions = localStorage.getItem('userPermissions');
      if (storedPermissions) {
        const parsedPermissions = JSON.parse(storedPermissions);
        if (Array.isArray(parsedPermissions)) {
          setPermissions(parsedPermissions);
        }
      }
    } catch (error) {
      console.error('Failed to parse stored permission data', error);
      setPermissions([]);
    }
  }, []);

  const syncRbacState = useCallback(
    async (userId: string, orgId?: string | null | undefined) => {
      try {
        if (orgId === undefined) {
          setRoles([]);
          setPermissions([]);
          localStorage.removeItem('userRoles');
          localStorage.removeItem('userPermissions');
          return;
        }
        console.log('[AuthContext] Syncing RBAC state for user:', userId, 'org:', orgId);
        const [roleResponses, permissionResponses] = await Promise.all([
          rbacService.getUserRoles(userId, orgId),
          rbacService.getUserPermissions(userId, orgId),
        ]);

        const roleCodes = (roleResponses || [])
          .map((role: Role) => role.code)
          .filter((code): code is string => Boolean(code));

        console.log('[AuthContext] User roles:', roleCodes);
        setRoles(roleCodes);
        localStorage.setItem('userRoles', JSON.stringify(roleCodes));

        const permissionList = (permissionResponses || []).filter(
          (permission: Permission) => Boolean(permission?.code)
        );
        
        console.log('[AuthContext] User permissions count:', permissionList.length);
        console.log('[AuthContext] HR permissions:', permissionList.filter(p => p.resource === 'hr'));
        console.log('[AuthContext] All permissions:', permissionList.map(p => `${p.resource}:${p.action}`));
        
        setPermissions(permissionList);
        localStorage.setItem('userPermissions', JSON.stringify(permissionList));
      } catch (error) {
        console.error('[AuthContext] Failed to synchronize RBAC state', error);
        // Keep any previously cached value to avoid locking user out unexpectedly
      }
    },
    []
  );

  useEffect(() => {
    let isMounted = true;

    const initializeAuth = async () => {
      setIsLoading(true);

      const currentUser = authService.getCurrentUser();
      if (currentUser && isMounted) {
        setUser(currentUser);
      }

      loadStoredRbacState();

      const orgId = authService.getCurrentOrganizationId();
      const orgName = authService.getCurrentOrganizationName();
      const orgLogo = authService.getCurrentOrganizationLogo();
      if (isMounted) {
        setCurrentOrganizationId(orgId);
        setCurrentOrganizationName(orgName);
        setCurrentOrganizationLogo(orgLogo);
      }

      if (currentUser?.id) {
        try {
          await syncRbacState(currentUser.id, orgId);
        } catch (error) {
          // errors already logged inside syncRbacState
        }
      }

      if (isMounted) {
        setIsLoading(false);
      }
    };

    initializeAuth();

    return () => {
      isMounted = false;
    };
  }, [loadStoredRbacState, syncRbacState]);

  // Reload RBAC when tenant context changes (org or global workspace)
  useEffect(() => {
    if (!user?.id || authService.hasPendingOrganizationSelection()) {
      return;
    }
    syncRbacState(user.id, authService.getEffectiveOrganizationIdForRbac()).catch(() => {
      // errors already logged
    });
  }, [currentOrganizationId, currentOrganizationName, user?.id, syncRbacState]);

  const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await authService.login(credentials);
    setUser(authService.getCurrentUser() ?? {
      id: response.userId,
      username: response.username,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
    });

    const orgId = authService.getCurrentOrganizationId();
    const orgName = authService.getCurrentOrganizationName();
    const orgLogo = authService.getCurrentOrganizationLogo();
    setCurrentOrganizationId(orgId);
    setCurrentOrganizationName(orgName);
    setCurrentOrganizationLogo(orgLogo);

    if (authService.hasPendingOrganizationSelection()) {
      setRoles([]);
      setPermissions([]);
      localStorage.removeItem('userRoles');
      localStorage.removeItem('userPermissions');
      return response;
    }

    const roleCodes = Array.isArray(response.roles) ? response.roles : [];
    setRoles(roleCodes);
    localStorage.setItem('userRoles', JSON.stringify(roleCodes));

    if (response.userId) {
      await syncRbacState(response.userId.toString(), authService.getEffectiveOrganizationIdForRbac());
    } else {
      const permissionCodes = Array.isArray(response.permissions) ? response.permissions : [];
      const fallbackPermissions = permissionCodes.map((code) => ({
        id: code,
        name: code,
        code,
        resource: '',
        action: '',
        description: '',
        isActive: true,
        createdAt: '',
        updatedAt: '',
      }));
      localStorage.setItem('userPermissions', JSON.stringify(fallbackPermissions));
      setPermissions(fallbackPermissions);
    }

    return response;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setCurrentOrganizationId(null);
    setCurrentOrganizationName(null);
    setCurrentOrganizationLogo(null);
    setRoles([]);
    setPermissions([]);
    localStorage.removeItem('userRoles');
    localStorage.removeItem('userPermissions');
  };

  const refreshAuth = () => {
    const currentUser = authService.getCurrentUser();
    setUser(currentUser);
    
    const orgId = authService.getCurrentOrganizationId();
    const orgName = authService.getCurrentOrganizationName();
    const orgLogo = authService.getCurrentOrganizationLogo();
    setCurrentOrganizationId(orgId);
    setCurrentOrganizationName(orgName);
    setCurrentOrganizationLogo(orgLogo);

    loadStoredRbacState();

    if (currentUser?.id && !authService.hasPendingOrganizationSelection()) {
      syncRbacState(currentUser.id, authService.getEffectiveOrganizationIdForRbac()).catch(() => {
        // errors already logged
      });
    }
  };

  const setGlobalWorkspace = () => {
    authService.setGlobalWorkspace();
    setCurrentOrganizationId(null);
    setCurrentOrganizationName(authService.getCurrentOrganizationName());
    setCurrentOrganizationLogo(null);
  };

  const setCurrentOrganization = (
    organizationId: string,
    organizationName: string,
    organizationLogo?: string | null,
    themeOverrides?: { themeMode?: string; themePrimaryColor?: string; themeSecondaryColor?: string; themeAccentColor?: string; themeSidebarColor?: string; themeSidebarTextColor?: string }
  ) => {
    authService.setCurrentOrganization(organizationId, organizationName, organizationLogo);
    if (themeOverrides) {
      const theme = {
        mode: (themeOverrides.themeMode as 'light' | 'dark') || DEFAULT_THEME.mode,
        primaryColor: themeOverrides.themePrimaryColor || DEFAULT_THEME.primaryColor,
        secondaryColor: themeOverrides.themeSecondaryColor || DEFAULT_THEME.secondaryColor,
        accentColor: themeOverrides.themeAccentColor || DEFAULT_THEME.accentColor,
        sidebarColor: themeOverrides.themeSidebarColor || DEFAULT_THEME.sidebarColor,
        sidebarTextColor: themeOverrides.themeSidebarTextColor || DEFAULT_THEME.sidebarTextColor,
      };
      localStorage.setItem('currentOrganizationTheme', JSON.stringify(theme));
      // Dispatch storage event so ThemeContext picks it up in the same tab
      window.dispatchEvent(new StorageEvent('storage', { key: 'currentOrganizationTheme', newValue: JSON.stringify(theme) }));
    }
    setCurrentOrganizationId(organizationId);
    setCurrentOrganizationName(organizationName);
    if (organizationLogo === null) {
      setCurrentOrganizationLogo(null);
    } else if (organizationLogo !== undefined) {
      setCurrentOrganizationLogo(organizationLogo);
    }
  };

  const hasRole = useCallback(
    (roleCode: string) => roles.includes(roleCode),
    [roles]
  );

  const hasPermission = useCallback(
    (permissionCode: string) => permissions.some((permission) => permission.code === permissionCode),
    [permissions]
  );

  const hasAnyPermission = useCallback(
    (permissionCodes: string[]) =>
      permissionCodes.some((permission) =>
        permissions.some((userPermission) => userPermission.code === permission)
      ),
    [permissions]
  );

  const normalizedAction = (action?: string | null) => (action || '').toLowerCase();

  const hasSystemAdminRole = roles.some((role) =>
    ['SYSTEM_ADMIN', 'SYSTEM_ADMINISTRATOR', 'SUPER_ADMIN'].includes((role || '').toUpperCase())
  );

  const canViewResource = useCallback(
    (resource: string) => {
      if (hasSystemAdminRole) {
        return true;
      }

      const direct = permissions.some(
        (permission) =>
          permission.isActive &&
          permission.resource === resource &&
          ['view', 'manage', 'admin'].includes(normalizedAction(permission.action))
      );
      if (direct) {
        return true;
      }

      if (PLATFORM_RESOURCES_USING_SYSTEM_FALLBACK.has(resource)) {
        const viaSystem = permissions.some(
          (p) =>
            p.isActive &&
            p.resource === 'system' &&
            ['view', 'manage', 'admin', 'configure'].includes(normalizedAction(p.action))
        );
        if (viaSystem) {
          return true;
        }
      }

      if (resource === 'hr') {
        console.warn('[AuthContext] No HR access. Roles:', roles, 'Permissions:', permissions.map(p => `${p.resource}:${p.action}`));
      }
      return false;
    },
    [hasSystemAdminRole, permissions, roles]
  );

  const canManageResource = useCallback(
    (resource: string) => {
      if (hasSystemAdminRole) {
        return true;
      }

      const direct = permissions.some(
        (permission) =>
          permission.isActive &&
          permission.resource === resource &&
          ['manage', 'admin'].includes(normalizedAction(permission.action))
      );
      if (direct) {
        return true;
      }

      if (PLATFORM_RESOURCES_USING_SYSTEM_FALLBACK.has(resource)) {
        return permissions.some(
          (p) =>
            p.isActive &&
            p.resource === 'system' &&
            ['configure', 'manage', 'admin'].includes(normalizedAction(p.action))
        );
      }

      return false;
    },
    [hasSystemAdminRole, permissions, roles]
  );

  const value = {
    user,
    currentOrganizationId,
    currentOrganizationName,
    currentOrganizationLogo,
    roles,
    permissions,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    refreshAuth,
    setCurrentOrganization,
    setGlobalWorkspace,
    hasRole,
    hasPermission,
    hasAnyPermission,
    canViewResource,
    canManageResource,
    reloadRbacState: async () => {
      if (user?.id) {
        console.log('[AuthContext] Force reloading RBAC state...');
        localStorage.removeItem('userRoles');
        localStorage.removeItem('userPermissions');
        await syncRbacState(user.id, authService.getEffectiveOrganizationIdForRbac());
      } else {
        console.warn('[AuthContext] Cannot reload RBAC state: user not found');
      }
    },
  };

  // Expose debug function globally for console access
  if (typeof window !== 'undefined') {
    (window as any).debugPermissions = () => {
      console.log('=== PERMISSION DEBUG INFO ===');
      console.log('User:', user);
      console.log('User ID:', user?.id);
      console.log('Roles:', roles);
      console.log('Is SYSTEM_ADMIN:', roles.includes('SYSTEM_ADMIN'));
      console.log('Permissions Count:', permissions.length);
      console.log('Permissions:', permissions);
      console.log('HR Permissions:', permissions.filter(p => p.resource === 'hr'));
      console.log('HR View Access:', canViewResource('hr'));
      console.log('HR Manage Access:', canManageResource('hr'));
      console.log('Cached Roles:', localStorage.getItem('userRoles'));
      console.log('Cached Permissions:', localStorage.getItem('userPermissions'));
      console.log('===========================');
    };
    (window as any).refreshPermissions = async () => {
      console.log('Refreshing permissions...');
      if (user?.id) {
        localStorage.removeItem('userRoles');
        localStorage.removeItem('userPermissions');
        await syncRbacState(user.id, authService.getEffectiveOrganizationIdForRbac());
        console.log('Permissions refreshed! Reloading page...');
        window.location.reload();
      } else {
        console.error('User not found');
      }
    };
    (window as any).checkUserRole = async () => {
      if (user?.id) {
        try {
          const roles = await rbacService.getUserRoles(user.id);
          console.log('Backend returned roles:', roles);
          const roleCodes = roles.map((r: Role) => r.code);
          console.log('Role codes:', roleCodes);
          console.log('Has SYSTEM_ADMIN:', roleCodes.includes('SYSTEM_ADMIN'));
        } catch (error) {
          console.error('Error checking roles:', error);
        }
      } else {
        console.error('User not found');
      }
    };
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

