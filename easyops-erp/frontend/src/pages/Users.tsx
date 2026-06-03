import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Tooltip,
  Autocomplete,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  CheckCircle,
  Cancel,
  Security as SecurityIcon,
  LockReset as LockResetIcon,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import userService from '@services/userService';
import authService from '@services/authService';
import rbacService from '@services/rbacService';
import organizationService from '@services/organizationService';
import { User, UserCreateRequest, UserUpdateRequest, Role } from '@types/index';
import { Organization } from '@types/organization';
import appConfig from '@config';
import { useAuth } from '@contexts/AuthContext';
import { evaluatePasswordPolicy, PASSWORD_REQUIREMENTS } from '@/utils/passwordPolicy';

const GENERIC_CLIENT_ERROR_PHRASES = new Set(
  ['bad request', 'validation failed', 'unprocessable entity', 'invalid request'].map((s) =>
    s.toLowerCase()
  )
);

function isUsefulMessage(text: string): boolean {
  const t = text.trim().toLowerCase();
  return t.length > 0 && !GENERIC_CLIENT_ERROR_PHRASES.has(t);
}

/** Pull validation / field messages from Spring Boot 3.x Problem Details and legacy shapes (incl. nested `cause`). */
function collectValidationMessages(node: unknown, out: string[], depth: number): void {
  if (depth > 14 || node == null) return;

  if (typeof node === 'string') {
    if (isUsefulMessage(node)) out.push(node.trim());
    return;
  }

  if (!node || typeof node !== 'object') return;
  const o = node as Record<string, unknown>;

  if (typeof o.defaultMessage === 'string' && o.defaultMessage.trim()) {
    out.push(o.defaultMessage.trim());
  } else if (typeof o.message === 'string' && isUsefulMessage(o.message)) {
    out.push((o.message as string).trim());
  }

  const walk = (v: unknown) => {
    if (v == null) return;
    if (Array.isArray(v)) {
      v.forEach((item) => collectValidationMessages(item, out, depth + 1));
    } else {
      collectValidationMessages(v, out, depth + 1);
    }
  };

  walk(o.cause);
  walk(o.properties);
  walk(o.violations);
  walk(o.fieldErrors);
  walk(o.invalidParams);
  walk(o.invalid_params);

  const errs = o.errors;
  if (errs != null) {
    if (Array.isArray(errs)) {
      walk(errs);
    } else if (typeof errs === 'object') {
      for (const v of Object.values(errs as Record<string, unknown>)) {
        walk(v);
      }
    }
  }

  if (typeof o.detail === 'string' && isUsefulMessage(o.detail)) {
    out.push(o.detail.trim());
  }
}

function getApiErrorMessage(err: unknown, fallback: string): string {
  const ax = err as { response?: { data?: unknown } };
  const data = ax.response?.data;
  if (!data || typeof data !== 'object') {
    return fallback;
  }
  const d = data as Record<string, unknown>;

  const fromFields: string[] = [];
  collectValidationMessages(d, fromFields, 0);
  const unique = [...new Set(fromFields.filter(Boolean))];
  if (unique.length) {
    return unique.join(' ');
  }

  if (typeof d.detail === 'string' && isUsefulMessage(d.detail)) {
    return d.detail.trim();
  }
  if (typeof d.message === 'string' && isUsefulMessage(d.message)) {
    return d.message.trim();
  }
  if (typeof d.error === 'string' && isUsefulMessage(d.error)) {
    return d.error.trim();
  }
  if (typeof d.title === 'string' && isUsefulMessage(d.title)) {
    return d.title.trim();
  }
  return fallback;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

const Users: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [newUser, setNewUser] = useState<UserCreateRequest>({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
  });
  const [openEditDialog, setOpenEditDialog] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [editUserData, setEditUserData] = useState<UserUpdateRequest>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
  });
  const [assignRolesOpen, setAssignRolesOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [availableRoles, setAvailableRoles] = useState<Role[]>([]);
  const [assignedRoleIds, setAssignedRoleIds] = useState<string[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [userRolesMap, setUserRolesMap] = useState<Record<string, Role[]>>({});
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<string>('');
  const [showPassword, setShowPassword] = useState(false);
  const [createUserError, setCreateUserError] = useState('');
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordDialogUser, setPasswordDialogUser] = useState<User | null>(null);
  const [passwordForm, setPasswordForm] = useState({
    newPassword: '',
    confirmPassword: '',
  });
  const [showChangePasswordNew, setShowChangePasswordNew] = useState(false);
  const [showChangePasswordConfirm, setShowChangePasswordConfirm] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState('');

  const { enqueueSnackbar } = useSnackbar();
  const { canManageResource } = useAuth();
  const canManageRoles = canManageResource('roles');
  // ORG_ADMIN has roles:manage — allow them to create/edit users so they can assign roles
  const canManageUsers = canManageResource('users') || canManageRoles;
  /** ROLE_MANAGE (included on ORG_ADMIN) satisfies rbac assign-role APIs and UI. */
  const canAssignRoles = canManageRoles;
  const trimmedCreateEmail = newUser.email.trim();
  const createEmailInvalid = trimmedCreateEmail.length > 0 && !EMAIL_REGEX.test(trimmedCreateEmail);
  const trimmedEditEmail = (editUserData.email || '').trim();
  const editEmailInvalid = trimmedEditEmail.length > 0 && !EMAIL_REGEX.test(trimmedEditEmail);
  const createPasswordChecks = useMemo(
    () => evaluatePasswordPolicy(newUser.password),
    [newUser.password]
  );
  const createPasswordMeetsPolicy = useMemo(
    () => PASSWORD_REQUIREMENTS.every((r) => createPasswordChecks[r.key]),
    [createPasswordChecks]
  );
  const showCreatePasswordPolicyFeedback = newUser.password.length > 0;
  const changePasswordChecks = useMemo(
    () => evaluatePasswordPolicy(passwordForm.newPassword),
    [passwordForm.newPassword]
  );
  const changePasswordMeetsPolicy = useMemo(
    () => PASSWORD_REQUIREMENTS.every((r) => changePasswordChecks[r.key]),
    [changePasswordChecks]
  );
  const changePasswordMismatch =
    passwordForm.confirmPassword.length > 0 &&
    passwordForm.newPassword !== passwordForm.confirmPassword;

  const preloadUserRoles = useCallback(
    async (userList: User[]) => {
      const missingUsers = userList.filter((user) => !userRolesMap[user.id]);
      if (missingUsers.length === 0) {
        return;
      }

      try {
        const entries = await Promise.all(
          missingUsers.map(async (user) => {
            const roles = await rbacService.getUserRoles(user.id);
            return [user.id, roles] as [string, Role[]];
          })
        );

        setUserRolesMap((prev) => {
          const next = { ...prev };
          entries.forEach(([userId, roles]) => {
            next[userId] = roles;
          });
          return next;
        });
      } catch (error) {
        console.error('Failed to preload user roles', error);
      }
    },
    [userRolesMap]
  );

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await userService.getAllUsers({
        page,
        size: rowsPerPage,
      });
      const fetchedUsers = response.content || [];
      setUsers(fetchedUsers);
      setTotalElements(response.totalElements);
      preloadUserRoles(fetchedUsers);
    } catch (error) {
      enqueueSnackbar('Failed to load users', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page, rowsPerPage]);

  /** Load role checkboxes for the selected organization context (not the logged-in user's default org). */
  useEffect(() => {
    if (!assignRolesOpen || !selectedUser || !selectedOrganizationId) {
      if (assignRolesOpen && selectedUser && !selectedOrganizationId) {
        setAssignedRoleIds([]);
      }
      return;
    }

    let cancelled = false;
    (async () => {
      setRolesLoading(true);
      try {
        const [active, userRoles] = await Promise.all([
          rbacService.getActiveRoles(),
          rbacService.getUserRoles(selectedUser.id, selectedOrganizationId),
        ]);
        if (cancelled) return;
        const merged = [...active];
        userRoles.forEach((role) => {
          if (!merged.some((r) => r.id === role.id)) {
            merged.push(role);
          }
        });
        setAvailableRoles(merged);
        setAssignedRoleIds(userRoles.map((r) => r.id));
      } catch (error: unknown) {
        if (!cancelled) {
          console.error('Failed to load roles for organization', error);
          const msg =
            (error as { response?: { data?: { error?: string } } })?.response?.data?.error ||
            'Failed to load roles for this organization';
          enqueueSnackbar(msg, { variant: 'error' });
        }
      } finally {
        if (!cancelled) {
          setRolesLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [assignRolesOpen, selectedUser?.id, selectedOrganizationId]);

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      fetchUsers();
      return;
    }

    try {
      setLoading(true);
      const response = await userService.searchUsers(searchTerm, {
        page,
        size: rowsPerPage,
      });
      const fetchedUsers = response.content || [];
      setUsers(fetchedUsers);
      setTotalElements(response.totalElements);
      preloadUserRoles(fetchedUsers);
    } catch (error) {
      enqueueSnackbar('Search failed', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async () => {
    if (!canManageUsers) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }
    if (createEmailInvalid) {
      const msg = 'Please enter a valid email address (example: user@example.com).';
      setCreateUserError(msg);
      enqueueSnackbar(msg, { variant: 'error' });
      return;
    }
    if (!createPasswordMeetsPolicy) {
      const msg = 'Password does not meet the required criteria.';
      setCreateUserError(msg);
      enqueueSnackbar(msg, { variant: 'error' });
      return;
    }

    setCreateUserError('');
    try {
      const payload: UserCreateRequest = {
        username: newUser.username.trim(),
        password: newUser.password,
        firstName: newUser.firstName?.trim() || undefined,
        lastName: newUser.lastName?.trim() || undefined,
        email: newUser.email.trim() ? newUser.email.trim() : null,
        phone: newUser.phone?.replace(/\D/g, '') || undefined,
      };
      await userService.createUser(payload);
      enqueueSnackbar('User created successfully', { variant: 'success' });
      setOpenDialog(false);
      setCreateUserError('');
      setNewUser({
        username: '',
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        phone: '',
      });
      fetchUsers();
    } catch (error: unknown) {
      const errorMessage = getApiErrorMessage(error, 'Failed to create user');
      setCreateUserError(errorMessage);
      enqueueSnackbar(errorMessage, { variant: 'error' });
    }
  };

  const handleEditUser = (user: User) => {
    if (!canManageUsers) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }

    setEditingUser(user);
    setEditUserData({
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      email: user.email || '',
      phone: user.phone || '',
    });
    setOpenEditDialog(true);
  };

  const handleUpdateUser = async () => {
    if (!canManageUsers || !editingUser) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }
    if (editEmailInvalid) {
      enqueueSnackbar('Please enter a valid email address (example: user@example.com).', {
        variant: 'error',
      });
      return;
    }

    try {
      const payload: UserUpdateRequest = {
        firstName: editUserData.firstName?.trim() || undefined,
        lastName: editUserData.lastName?.trim() || undefined,
        email: trimmedEditEmail || undefined,
        phone: (editUserData.phone || '').replace(/\D/g, '') || undefined,
      };
      await userService.updateUser(editingUser.id, payload);
      enqueueSnackbar('User updated successfully', { variant: 'success' });
      setOpenEditDialog(false);
      setEditingUser(null);
      setEditUserData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
      });
      fetchUsers();
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to update user';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    }
  };

  const handleOpenChangePasswordDialog = (user: User) => {
    if (!canManageUsers) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }
    setPasswordDialogUser(user);
    setChangePasswordError('');
    setPasswordForm({ newPassword: '', confirmPassword: '' });
    setShowChangePasswordNew(false);
    setShowChangePasswordConfirm(false);
    setPasswordDialogOpen(true);
  };

  const handleChangePassword = async () => {
    if (!passwordDialogUser) {
      return;
    }
    if (!changePasswordMeetsPolicy) {
      setChangePasswordError('New password does not meet the required criteria.');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setChangePasswordError('Confirm password must match new password.');
      return;
    }
    try {
      setChangePasswordError('');
      await authService.changePassword(passwordDialogUser.id, {
        newPassword: passwordForm.newPassword,
      });
      enqueueSnackbar('Password changed successfully', { variant: 'success' });
      setPasswordDialogOpen(false);
      setPasswordDialogUser(null);
      setPasswordForm({ newPassword: '', confirmPassword: '' });
    } catch (error: unknown) {
      const errorMessage = getApiErrorMessage(error, 'Failed to change password');
      setChangePasswordError(errorMessage);
      enqueueSnackbar(errorMessage, { variant: 'error' });
    }
  };

  const handleToggleActive = async (user: User) => {
    if (!canManageUsers) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }

    try {
      if (user.isActive) {
        await userService.deactivateUser(user.id);
        enqueueSnackbar('User deactivated', { variant: 'success' });
      } else {
        await userService.activateUser(user.id);
        enqueueSnackbar('User activated', { variant: 'success' });
      }
      fetchUsers();
    } catch (error) {
      enqueueSnackbar('Failed to update user status', { variant: 'error' });
    }
  };

  const handleDeleteUser = async (userId: string) => {
    if (!canManageUsers) {
      enqueueSnackbar('You do not have permission to manage users', { variant: 'warning' });
      return;
    }

    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await userService.deleteUser(userId);
        enqueueSnackbar('User deleted successfully', { variant: 'success' });
        fetchUsers();
      } catch (error) {
        enqueueSnackbar('Failed to delete user', { variant: 'error' });
      }
    }
  };

  const handleManageRoles = async (user: User) => {
    if (!canAssignRoles) {
      enqueueSnackbar('You do not have permission to manage roles', { variant: 'warning' });
      return;
    }

    setSelectedUser(user);
    setAssignRolesOpen(true);
    setRolesLoading(true);
    setSelectedOrganizationId('');
    setAssignedRoleIds([]);

    try {
      const [orgsResponse, activeRoles] = await Promise.all([
        organizationService.getAllOrganizations(0, 100),
        rbacService.getActiveRoles(),
      ]);

      const orgs = orgsResponse.content || [];
      setOrganizations(orgs);
      setAvailableRoles(activeRoles);

      // Auto-select Aurora Specialized Hospital if present
      const auroraOrg = orgs.find((o) =>
        o.name?.toLowerCase().includes('aurora')
      );
      if (auroraOrg) {
        setSelectedOrganizationId(auroraOrg.id);
      }

      const summaryRoles = await rbacService.getUserRoles(user.id);
      setUserRolesMap((prev) => ({ ...prev, [user.id]: summaryRoles }));
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to load organizations';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setRolesLoading(false);
    }
  };

  const handleSaveRoles = async () => {
    if (!selectedUser) {
      return;
    }

    if (!canAssignRoles) {
      enqueueSnackbar('You do not have permission to manage roles', { variant: 'warning' });
      return;
    }

    if (!selectedOrganizationId) {
      enqueueSnackbar(
        'Select an organization first. Roles like ORG_ADMIN are stored per organization — pick AURORA (or the org you want to edit).',
        { variant: 'warning' }
      );
      return;
    }

    try {
      setRolesLoading(true);
      const updatedRoles = await rbacService.assignRolesToUser({
        userId: selectedUser.id,
        roleIds: assignedRoleIds,
        organizationId: selectedOrganizationId,
      });

      const summaryRoles = await rbacService.getUserRoles(selectedUser.id);
      setUserRolesMap((prev) => ({
        ...prev,
        [selectedUser.id]: summaryRoles,
      }));

      enqueueSnackbar('User roles updated successfully', { variant: 'success' });
      setAssignRolesOpen(false);
      setSelectedUser(null);
      setAssignedRoleIds([]);
      setSelectedOrganizationId('');
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || 'Failed to update user roles';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setRolesLoading(false);
    }
  };

  const handleCloseRolesDialog = () => {
    setAssignRolesOpen(false);
    setSelectedUser(null);
    setAssignedRoleIds([]);
    setSelectedOrganizationId('');
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight="bold">
          User Management
        </Typography>
        <Tooltip
          title={
            canManageUsers
              ? 'Create a new user'
              : 'You do not have permission to create users'
          }
        >
          <span>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setCreateUserError('');
                setOpenDialog(true);
              }}
              disabled={!canManageUsers}
            >
              Add User
            </Button>
          </span>
        </Tooltip>
      </Box>

      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField
          fullWidth
          placeholder="Search users by name, email, or username..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
            endAdornment: (
              <Button onClick={handleSearch}>Search</Button>
            ),
          }}
        />
      </Paper>

      <TableContainer component={Paper}>
        {loading ? (
          <Box display="flex" justifyContent="center" p={4}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Username</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Roles</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      <Typography variant="body2" color="text.secondary" py={4}>
                        No users found
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((user) => (
                    <TableRow key={user.id} hover>
                      <TableCell>
                        <Typography fontWeight="medium">{user.username}</Typography>
                      </TableCell>
                      <TableCell>{user.email ?? '—'}</TableCell>
                      <TableCell>
                        {user.firstName || user.lastName
                          ? `${user.firstName || ''} ${user.lastName || ''}`.trim()
                          : '-'}
                      </TableCell>
                      <TableCell>
                        <Tooltip
                          title={
                            canManageUsers
                              ? 'Toggle user status'
                              : 'You do not have permission to manage users'
                          }
                        >
                          <span style={{ display: 'inline-block' }}>
                            <Chip
                              icon={user.isActive ? <CheckCircle /> : <Cancel />}
                              label={user.isActive ? 'Active' : 'Inactive'}
                              color={user.isActive ? 'success' : 'default'}
                              size="small"
                              onClick={canManageUsers ? () => handleToggleActive(user) : undefined}
                              sx={{
                                cursor: canManageUsers ? 'pointer' : 'not-allowed',
                                opacity: canManageUsers ? 1 : 0.75,
                              }}
                            />
                          </span>
                        </Tooltip>
                      </TableCell>
                      <TableCell>
                        {userRolesMap[user.id]?.length ? (
                          <Box display="flex" flexWrap="wrap" gap={0.5}>
                            {userRolesMap[user.id].map((role) => (
                              <Chip key={role.id} label={role.code} size="small" variant="outlined" />
                            ))}
                          </Box>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            No roles
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {new Date(user.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip
                          title={
                            canAssignRoles
                              ? 'Manage user roles'
                              : 'You do not have permission to manage roles'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="secondary"
                              onClick={() => handleManageRoles(user)}
                              disabled={!canAssignRoles}
                            >
                              <SecurityIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip
                          title={
                            canManageUsers
                              ? 'Edit user'
                              : 'You do not have permission to manage users'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleEditUser(user)}
                              disabled={!canManageUsers}
                            >
                              <EditIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip
                          title={
                            canManageUsers
                              ? 'Change user password'
                              : 'You do not have permission to manage users'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="info"
                              onClick={() => handleOpenChangePasswordDialog(user)}
                              disabled={!canManageUsers}
                            >
                              <LockResetIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip
                          title={
                            canManageUsers
                              ? 'Delete user'
                              : 'You do not have permission to manage users'
                          }
                        >
                          <span>
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDeleteUser(user.id)}
                              disabled={!canManageUsers}
                            >
                              <DeleteIcon />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={totalElements}
              page={page}
              onPageChange={(_, newPage) => setPage(newPage)}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={(e) => {
                setRowsPerPage(parseInt(e.target.value, 10));
                setPage(0);
              }}
            />
          </>
        )}
      </TableContainer>

      {/* Create User Dialog */}
      <Dialog
        open={openDialog}
        onClose={() => {
          setCreateUserError('');
          setOpenDialog(false);
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create New User</DialogTitle>
        <DialogContent>
          {createUserError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {createUserError}
            </Alert>
          ) : null}
          <TextField
            autoFocus
            margin="dense"
            label="Username"
            fullWidth
            required
            value={newUser.username}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, username: e.target.value });
            }}
          />
          <TextField
            margin="dense"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            fullWidth
            required
            value={newUser.password}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, password: e.target.value });
            }}
            error={showCreatePasswordPolicyFeedback && !createPasswordMeetsPolicy}
            color={
              showCreatePasswordPolicyFeedback && createPasswordMeetsPolicy ? 'success' : 'primary'
            }
            helperText={
              showCreatePasswordPolicyFeedback ? (
                createPasswordMeetsPolicy ? (
                  <Typography variant="caption" color="success.main" component="span">
                    Password matches {appConfig.appName} requirements.
                  </Typography>
                ) : (
                  <Box component="span" sx={{ display: 'block' }}>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      component="div"
                      sx={{ mb: 0.5 }}
                    >
                      Password must include:
                    </Typography>
                    {PASSWORD_REQUIREMENTS.map(({ key, label }) => (
                      <Typography
                        key={key}
                        variant="caption"
                        component="div"
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 0.75,
                          color: createPasswordChecks[key] ? 'success.main' : 'text.secondary',
                        }}
                      >
                        <Box
                          component="span"
                          sx={{
                            width: 16,
                            textAlign: 'center',
                            fontWeight: 600,
                          }}
                          aria-hidden
                        >
                          {createPasswordChecks[key] ? '✓' : '○'}
                        </Box>
                        {label}
                      </Typography>
                    ))}
                  </Box>
                )
              ) : undefined
            }
            FormHelperTextProps={{ component: 'div' }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <Button
                    onClick={() => setShowPassword((prev) => !prev)}
                    size="small"
                    sx={{ minWidth: 0, mr: 0.5 }}
                  >
                    {showPassword ? 'Hide' : 'View'}
                  </Button>
                  <IconButton
                    onClick={() => setShowPassword((prev) => !prev)}
                    edge="end"
                    size="small"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <TextField
            margin="dense"
            label="First Name"
            fullWidth
            value={newUser.firstName}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, firstName: e.target.value });
            }}
          />
          <TextField
            margin="dense"
            label="Last Name"
            fullWidth
            value={newUser.lastName}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, lastName: e.target.value });
            }}
          />
          <TextField
            margin="dense"
            label="Email"
            type="email"
            fullWidth
            error={createEmailInvalid}
            helperText={
              createEmailInvalid
                ? 'Enter a valid email address (example: user@example.com)'
                : 'Optional'
            }
            value={newUser.email}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, email: e.target.value });
            }}
          />
          <TextField
            margin="dense"
            label="Phone (optional)"
            fullWidth
            helperText="Digits only — used for SMS password recovery"
            value={newUser.phone}
            onChange={(e) => {
              setCreateUserError('');
              setNewUser({ ...newUser, phone: e.target.value.replace(/\D/g, '') });
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setCreateUserError('');
              setOpenDialog(false);
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleCreateUser}
            variant="contained"
            disabled={
              !canManageUsers ||
              !newUser.username.trim() ||
              !newUser.password.trim() ||
              !createPasswordMeetsPolicy ||
              createEmailInvalid
            }
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit User Dialog */}
      <Dialog open={openEditDialog} onClose={() => setOpenEditDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit User{editingUser ? ` – ${editingUser.username}` : ''}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="First Name"
            fullWidth
            value={editUserData.firstName}
            onChange={(e) => setEditUserData({ ...editUserData, firstName: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Last Name"
            fullWidth
            value={editUserData.lastName}
            onChange={(e) => setEditUserData({ ...editUserData, lastName: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Email"
            type="email"
            fullWidth
            error={editEmailInvalid}
            helperText={
              editEmailInvalid ? 'Enter a valid email address (example: user@example.com)' : 'Optional'
            }
            value={editUserData.email}
            onChange={(e) => setEditUserData({ ...editUserData, email: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Phone"
            fullWidth
            value={editUserData.phone}
            helperText="Only numbers are allowed"
            inputProps={{ inputMode: 'numeric', pattern: '[0-9]*' }}
            onChange={(e) =>
              setEditUserData({ ...editUserData, phone: e.target.value.replace(/\D/g, '') })
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenEditDialog(false)}>Cancel</Button>
          <Button
            onClick={handleUpdateUser}
            variant="contained"
            disabled={!canManageUsers || !editingUser || editEmailInvalid}
          >
            Update
          </Button>
        </DialogActions>
      </Dialog>

      {/* Change Password Dialog */}
      <Dialog
        open={passwordDialogOpen}
        onClose={() => setPasswordDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Change Password{passwordDialogUser ? ` – ${passwordDialogUser.username}` : ''}
        </DialogTitle>
        <DialogContent>
          {changePasswordError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {changePasswordError}
            </Alert>
          ) : null}
          <TextField
            autoFocus
            margin="dense"
            label="New Password"
            type={showChangePasswordNew ? 'text' : 'password'}
            fullWidth
            required
            value={passwordForm.newPassword}
            onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
            error={passwordForm.newPassword.length > 0 && !changePasswordMeetsPolicy}
            color={
              passwordForm.newPassword.length > 0 && changePasswordMeetsPolicy ? 'success' : 'primary'
            }
            helperText={
              passwordForm.newPassword.length > 0 ? (
                changePasswordMeetsPolicy ? (
                  <Typography variant="caption" color="success.main" component="span">
                    Password matches {appConfig.appName} requirements.
                  </Typography>
                ) : (
                  <Box component="span" sx={{ display: 'block' }}>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      component="div"
                      sx={{ mb: 0.5 }}
                    >
                      Password must include:
                    </Typography>
                    {PASSWORD_REQUIREMENTS.map(({ key, label }) => (
                      <Typography
                        key={key}
                        variant="caption"
                        component="div"
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 0.75,
                          color: changePasswordChecks[key] ? 'success.main' : 'text.secondary',
                        }}
                      >
                        <Box
                          component="span"
                          sx={{
                            width: 16,
                            textAlign: 'center',
                            fontWeight: 600,
                          }}
                          aria-hidden
                        >
                          {changePasswordChecks[key] ? '✓' : '○'}
                        </Box>
                        {label}
                      </Typography>
                    ))}
                  </Box>
                )
              ) : undefined
            }
            FormHelperTextProps={{ component: 'div' }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowChangePasswordNew((prev) => !prev)}
                    edge="end"
                    size="small"
                  >
                    {showChangePasswordNew ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <TextField
            margin="dense"
            label="Confirm New Password"
            type={showChangePasswordConfirm ? 'text' : 'password'}
            fullWidth
            required
            error={changePasswordMismatch}
            helperText={changePasswordMismatch ? 'Confirm password must match new password' : undefined}
            value={passwordForm.confirmPassword}
            onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowChangePasswordConfirm((prev) => !prev)}
                    edge="end"
                    size="small"
                  >
                    {showChangePasswordConfirm ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleChangePassword}
            variant="contained"
            disabled={
              !canManageUsers ||
              !passwordDialogUser ||
              !passwordForm.newPassword.trim() ||
              !passwordForm.confirmPassword.trim() ||
              !changePasswordMeetsPolicy ||
              changePasswordMismatch
            }
          >
            Change Password
          </Button>
        </DialogActions>
      </Dialog>

      {/* Manage Roles Dialog */}
      <Dialog open={assignRolesOpen} onClose={handleCloseRolesDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          Manage Roles{selectedUser ? ` – ${selectedUser.username}` : ''}
        </DialogTitle>
        <DialogContent>
          {rolesLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <>
              <FormControl fullWidth margin="normal" size="small">
                <InputLabel id="organization-select-label" shrink>
                  Organization
                </InputLabel>
                <Select
                  labelId="organization-select-label"
                  label="Organization"
                  displayEmpty
                  value={selectedOrganizationId}
                  onChange={(e) => setSelectedOrganizationId(e.target.value)}
                  renderValue={(selected) => {
                    if (!selected) {
                      return <span style={{ color: '#9ca3af' }}>Select organization...</span>;
                    }
                    const org = organizations.find((item) => item.id === selected);
                    return org ? `${org.name} (${org.code})` : selected;
                  }}
                >
                  <MenuItem value="">
                    <em>Select organization...</em>
                  </MenuItem>
                  {organizations.map((org) => (
                    <MenuItem key={org.id} value={org.id}>
                      {org.name} ({org.code})
                    </MenuItem>
                  ))}
                </Select>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.75 }}>
                  Choose which organization&apos;s assignments to view or edit (e.g. AURORA). This
                  is not your logged-in org - pick the tenant you configured.
                </Typography>
              </FormControl>
              <Autocomplete
                multiple
                options={availableRoles}
                value={availableRoles.filter((role) => assignedRoleIds.includes(role.id))}
                onChange={(_, selected) => setAssignedRoleIds(selected.map((role) => role.id))}
                getOptionLabel={(option) => `${option.name} (${option.code})`}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                renderInput={(params) => (
                  <TextField {...params} label="Roles" placeholder="Assign roles" margin="normal" />
                )}
              />
              <Typography variant="caption" color="text.secondary">
                Users inherit permissions from the roles assigned here. Select an organization first — role membership is stored per organization (ORG_ADMIN for AURORA is separate from DEMO).
              </Typography>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseRolesDialog}>Cancel</Button>
          <Button
            onClick={handleSaveRoles}
            variant="contained"
            disabled={
              rolesLoading || !canAssignRoles || !selectedUser || !selectedOrganizationId
            }
          >
            {rolesLoading ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Users;

