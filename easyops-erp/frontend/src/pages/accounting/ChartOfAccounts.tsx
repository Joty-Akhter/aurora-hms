import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  FormControlLabel,
  Switch,
  Alert,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  AccountTree as TreeIcon,
  List as ListIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { ChartOfAccount, CoARequest, AccountType } from '../../types/accounting';
import { getApiErrorMessage } from '../../utils/apiError';

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const isServerAccountId = (id?: string): boolean =>
  Boolean(id && UUID_PATTERN.test(id));

const ChartOfAccounts: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [accounts, setAccounts] = useState<ChartOfAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingAccount, setEditingAccount] = useState<ChartOfAccount | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'tree'>('list');
  
  const organizationId = currentOrganizationId || '';

  const [formData, setFormData] = useState<CoARequest>({
    organizationId: organizationId,
    accountCode: '',
    accountName: '',
    accountType: 'ASSET',
    level: 1,
    isGroup: false,
    currency: 'USD',
    openingBalance: 0,
    allowManualEntry: true,
  });

  useEffect(() => {
    if (organizationId) {
      loadAccounts();
    }
  }, [organizationId]);

  const normalizeAccount = (raw: any, orgId: string): ChartOfAccount => {
    const parseNumber = (value: any) => {
      if (typeof value === 'number') return value;
      if (typeof value === 'string') {
        const parsed = Number(value);
        return Number.isNaN(parsed) ? 0 : parsed;
      }
      return 0;
    };

    const code = raw?.accountCode || raw?.code || raw?.acctCode || '';
    return {
      id: raw.id,
      organizationId: raw?.organizationId || orgId,
      accountCode: code,
      accountName: raw?.accountName || raw?.name || 'Unnamed Account',
      parentAccountId: raw?.parentAccountId || undefined,
      accountType: (raw?.accountType || 'ASSET') as AccountType,
      accountCategory: raw?.accountCategory || '',
      accountSubcategory: raw?.accountSubcategory,
      level: typeof raw?.level === 'number' ? raw.level : Number(raw?.level ?? 1),
      isGroup: Boolean(raw?.isGroup),
      isSystemAccount: raw?.isSystemAccount ?? false,
      currency: raw?.currency || 'USD',
      openingBalance: parseNumber(raw?.openingBalance),
      openingBalanceDate: raw?.openingBalanceDate,
      currentBalance: parseNumber(raw?.currentBalance),
      isActive: raw?.isActive ?? true,
      allowManualEntry: raw?.allowManualEntry ?? !raw?.isGroup,
      description: raw?.description,
      taxType: raw?.taxType,
      tags: raw?.tags || [],
      createdAt: raw?.createdAt || new Date().toISOString(),
      updatedAt: raw?.updatedAt || new Date().toISOString(),
      createdBy: raw?.createdBy,
      updatedBy: raw?.updatedBy,
    };
  };

  const loadAccounts = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await accountingService.getAccounts(organizationId);
      const hasMeaningfulData = Array.isArray(data) && data.some((item) => item && Object.keys(item).length > 0);

      if (!hasMeaningfulData) {
        setAccounts([]);
        return;
      }

      const normalized = data
        .filter((item) => item?.id)
        .map((item) => normalizeAccount(item, organizationId));
      setAccounts(normalized);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to load accounts'));
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (account?: ChartOfAccount) => {
    if (account && !isServerAccountId(account.id)) {
      setError('This account cannot be edited until it is saved on the server. Use Load Standard COA or create a new account.');
      return;
    }
    if (account) {
      setEditingAccount(account);
      setFormData({
        organizationId: account.organizationId,
        accountCode: account.accountCode,
        accountName: account.accountName,
        parentAccountId: account.parentAccountId,
        accountType: account.accountType,
        accountCategory: account.accountCategory,
        accountSubcategory: account.accountSubcategory,
        level: account.level,
        isGroup: account.isGroup,
        currency: account.currency,
        openingBalance: account.openingBalance,
        openingBalanceDate: account.openingBalanceDate,
        allowManualEntry: account.allowManualEntry,
        description: account.description,
        taxType: account.taxType,
        tags: account.tags,
      });
    } else {
      setEditingAccount(null);
      setFormData({
        organizationId: organizationId,
        accountCode: '',
        accountName: '',
        accountType: 'ASSET',
        level: 1,
        isGroup: false,
        currency: 'USD',
        openingBalance: 0,
        allowManualEntry: true,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingAccount(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingAccount) {
        await accountingService.updateAccount(editingAccount.id, formData);
      } else {
        await accountingService.createAccount(formData);
      }
      handleCloseDialog();
      loadAccounts();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to save account'));
    }
  };

  const handleDeactivate = async (accountId: string) => {
    if (!isServerAccountId(accountId)) {
      setError('Cannot deactivate an account that is not stored on the server.');
      return;
    }
    if (window.confirm('Are you sure you want to deactivate this account?')) {
      try {
        await accountingService.deactivateAccount(accountId, organizationId);
        loadAccounts();
      } catch (err: unknown) {
        setError(getApiErrorMessage(err, 'Failed to deactivate account'));
      }
    }
  };

  const handleLoadStandardCOA = async () => {
    if (window.confirm('This will load a standard chart of accounts template. Continue?')) {
      try {
        await accountingService.loadStandardCOA(organizationId);
        loadAccounts();
      } catch (err: unknown) {
        setError(getApiErrorMessage(err, 'Failed to load standard chart of accounts'));
      }
    }
  };

  const getAccountTypeColor = (type: AccountType) => {
    const colors: Record<AccountType, string> = {
      ASSET: 'success',
      LIABILITY: 'error',
      EQUITY: 'info',
      REVENUE: 'primary',
      EXPENSE: 'warning',
    };
    return colors[type] || 'default';
  };

  const formatAmount = (value?: number) => {
    if (typeof value === 'number' && !Number.isNaN(value)) {
      return value.toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    }

    return '0.00';
  };

  const groupedAccounts = accounts.reduce((acc, account) => {
    if (!acc[account.accountType]) {
      acc[account.accountType] = [];
    }
    acc[account.accountType].push(account);
    return acc;
  }, {} as Record<string, ChartOfAccount[]>);

  return (
    <Box>
      {!organizationId && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          No organization selected. Please select an organization to view and manage your chart of accounts.
        </Alert>
      )}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Chart of Accounts</Typography>
        <Box display="flex" gap={2}>
          <Button
            variant="outlined"
            startIcon={viewMode === 'list' ? <TreeIcon /> : <ListIcon />}
            onClick={() => setViewMode(viewMode === 'list' ? 'tree' : 'list')}
          >
            {viewMode === 'list' ? 'Tree View' : 'List View'}
          </Button>
          <Button
            variant="outlined"
            onClick={handleLoadStandardCOA}
            disabled={!organizationId || loading}
          >
            Load Standard COA
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
            disabled={!organizationId}
          >
            Add Account
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {viewMode === 'list' ? (
        <Card>
          <CardContent>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Account Code</TableCell>
                    <TableCell>Account Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell align="right">Opening Balance</TableCell>
                    <TableCell align="right">Current Balance</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={8} align="center">
                        Loading...
                      </TableCell>
                    </TableRow>
                  ) : accounts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={8} align="center">
                        No accounts found. Use &quot;Load Standard COA&quot; to seed accounts from the server, or add accounts manually.
                      </TableCell>
                    </TableRow>
                  ) : (
                    accounts.map((account) => (
                      <TableRow
                        key={account.id}
                        sx={{
                          backgroundColor: account.isGroup ? '#f5f5f5' : 'inherit',
                          fontWeight: account.isGroup ? 'bold' : 'normal',
                        }}
                      >
                        <TableCell>
                          {account.isGroup && '📁 '}
                          {account.accountCode}
                        </TableCell>
                        <TableCell>{account.accountName}</TableCell>
                        <TableCell>
                          <Chip
                            label={account.accountType}
                            size="small"
                            color={getAccountTypeColor(account.accountType) as any}
                          />
                        </TableCell>
                        <TableCell>{account.accountCategory}</TableCell>
                        <TableCell align="right">
                          {account.isGroup ? '-' : formatAmount(account.openingBalance)}
                        </TableCell>
                        <TableCell align="right">
                          {account.isGroup ? '-' : formatAmount(account.currentBalance)}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={account.isActive ? 'Active' : 'Inactive'}
                            size="small"
                            color={account.isActive ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell align="center">
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(account)}
                            color="primary"
                            title={account.isSystemAccount ? "View/Edit Account" : "Edit Account"}
                          >
                            <EditIcon />
                          </IconButton>
                          {!account.isSystemAccount && (
                            <IconButton
                              size="small"
                              onClick={() => handleDeactivate(account.id)}
                              color="error"
                              title="Deactivate Account"
                            >
                              <DeleteIcon />
                            </IconButton>
                          )}
                          {account.isSystemAccount && (
                            <Chip
                              label="System"
                              size="small"
                              color="default"
                              variant="outlined"
                              sx={{ ml: 1 }}
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      ) : accounts.length === 0 ? (
        <Card>
          <CardContent>
            <Typography align="center" color="text.secondary">
              No accounts found. Use &quot;Load Standard COA&quot; to seed accounts from the server, or add accounts manually.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Box>
          {Object.entries(groupedAccounts).map(([type, accts]) => (
            <Card key={type} sx={{ mb: 2 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <Chip label={type} color={getAccountTypeColor(type as AccountType) as any} sx={{ mr: 1 }} />
                  {type} ({accts.length} accounts)
                </Typography>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Code</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell align="right">Balance</TableCell>
                        <TableCell>Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {accts.map((account) => (
                        <TableRow key={account.id}>
                          <TableCell>{account.accountCode}</TableCell>
                          <TableCell>{account.accountName}</TableCell>
                          <TableCell align="right">
                            {formatAmount(account.currentBalance)}
                          </TableCell>
                          <TableCell>
                            <IconButton
                              size="small"
                              onClick={() => handleOpenDialog(account)}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
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

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingAccount ? 'Edit Account' : 'Create New Account'}
        </DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={2}>
            <TextField
              label="Account Code"
              value={formData.accountCode}
              onChange={(e) => setFormData({ ...formData, accountCode: e.target.value })}
              required
              fullWidth
              disabled={!!editingAccount}
            />
            <TextField
              label="Account Name"
              value={formData.accountName}
              onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
              required
              fullWidth
            />
            <TextField
              select
              label="Account Type"
              value={formData.accountType}
              onChange={(e) => setFormData({ ...formData, accountType: e.target.value as AccountType })}
              required
              fullWidth
            >
              <MenuItem value="ASSET">Asset</MenuItem>
              <MenuItem value="LIABILITY">Liability</MenuItem>
              <MenuItem value="EQUITY">Equity</MenuItem>
              <MenuItem value="REVENUE">Revenue</MenuItem>
              <MenuItem value="EXPENSE">Expense</MenuItem>
            </TextField>
            <TextField
              label="Account Category"
              value={formData.accountCategory || ''}
              onChange={(e) => setFormData({ ...formData, accountCategory: e.target.value })}
              fullWidth
              placeholder="e.g., Current Assets, Fixed Assets"
            />
            <TextField
              label="Opening Balance"
              type="number"
              value={formData.openingBalance}
              onChange={(e) => setFormData({ ...formData, openingBalance: parseFloat(e.target.value) || 0 })}
              fullWidth
              disabled={formData.isGroup}
            />
            <TextField
              label="Description"
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              multiline
              rows={3}
              fullWidth
            />
            <FormControlLabel
              control={
                <Switch
                  checked={formData.isGroup || false}
                  onChange={(e) => setFormData({ ...formData, isGroup: e.target.checked })}
                />
              }
              label="Is Group Account (summary only, no posting)"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            {editingAccount ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ChartOfAccounts;

