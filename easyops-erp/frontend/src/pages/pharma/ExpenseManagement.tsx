import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel,
  Grid, Alert, Tooltip, Tabs, Tab, LinearProgress
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  Category as CategoryIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Expense, ExpenseCategory, Territory, Target } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';
import { useSnackbar } from 'notistack';

const ExpenseManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [categories, setCategories] = useState<ExpenseCategory[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [showDialog, setShowDialog] = useState(false);
  const [showCategoryDialog, setShowCategoryDialog] = useState(false);
  const [selectedExpense, setSelectedExpense] = useState<Expense | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<ExpenseCategory | null>(null);
  const [currentExpenseTotal, setCurrentExpenseTotal] = useState<number>(0);
  const [expenseLimit, setExpenseLimit] = useState<number>(0);
  const [formData, setFormData] = useState<Partial<Expense>>({
    organizationId: currentOrganizationId,
    expenseDate: new Date().toISOString().split('T')[0],
    status: 'DRAFT',
    expenseAmount: 0
  });
  const [categoryFormData, setCategoryFormData] = useState<Partial<ExpenseCategory>>({
    organizationId: currentOrganizationId,
    isActive: true
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [expensesData, categoriesData, territoriesData, employeesRes] = await Promise.all([
        pharmaService.getExpenses(currentOrganizationId),
        pharmaService.getExpenseCategories(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' })
      ]);
      setExpenses(expensesData);
      setCategories(categoriesData);
      setTerritories(territoriesData);
      setEmployees(employeesRes.data || []);
    } catch (error) {
      console.error('Failed to load data:', error);
      enqueueSnackbar('Failed to load data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const loadExpenseLimits = async (territoryId: string, year: number, month: number) => {
    if (!territoryId || !year || !month) {
      setCurrentExpenseTotal(0);
      setExpenseLimit(0);
      return;
    }

    try {
      const [totalExpenses, target] = await Promise.all([
        pharmaService.getTotalExpensesForTerritory(territoryId, year, month),
        pharmaService.getActiveTargetForTerritoryAndMonth(territoryId, year, month)
      ]);
      setCurrentExpenseTotal(totalExpenses || 0);
      
      if (target) {
        const limit = target.targetAmount * 0.3; // 30% of target
        setExpenseLimit(limit);
      } else {
        setExpenseLimit(0);
      }
    } catch (error) {
      console.error('Failed to load expense limits:', error);
    }
  };

  const handleOpenDialog = (expense?: Expense) => {
    if (expense) {
      setSelectedExpense(expense);
      setFormData({
        ...expense,
        expenseDate: expense.expenseDate?.split('T')[0] || new Date().toISOString().split('T')[0]
      });
      if (expense.territoryId && expense.year && expense.month) {
        loadExpenseLimits(expense.territoryId, expense.year, expense.month);
      }
    } else {
      setSelectedExpense(null);
      setFormData({
        organizationId: currentOrganizationId,
        territoryId: '',
        expenseCategoryId: '',
        sourceEmployeeId: '',
        expenseAmount: 0,
        expenseDate: new Date().toISOString().split('T')[0],
        description: '',
        status: 'DRAFT'
      });
      setCurrentExpenseTotal(0);
      setExpenseLimit(0);
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedExpense(null);
    setCurrentExpenseTotal(0);
    setExpenseLimit(0);
  };

  const handleTerritoryChange = async (territoryId: string) => {
    setFormData({ ...formData, territoryId });
    if (territoryId && formData.expenseDate) {
      const date = new Date(formData.expenseDate + 'T00:00:00');
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      await loadExpenseLimits(territoryId, year, month);
    }
  };

  const handleDateChange = async (dateStr: string) => {
    const date = new Date(dateStr + 'T00:00:00');
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    setFormData({ ...formData, expenseDate: dateStr, year, month });
    if (formData.territoryId) {
      await loadExpenseLimits(formData.territoryId!, year, month);
    }
  };

  const handleCategoryOpenDialog = (category?: ExpenseCategory) => {
    if (category) {
      setSelectedCategory(category);
      setCategoryFormData(category);
    } else {
      setSelectedCategory(null);
      setCategoryFormData({
        organizationId: currentOrganizationId,
        name: '',
        description: '',
        isActive: true
      });
    }
    setShowCategoryDialog(true);
  };

  const handleCategorySave = async () => {
    if (!categoryFormData.name) {
      enqueueSnackbar('Please enter category name', { variant: 'warning' });
      return;
    }

    try {
      if (selectedCategory?.id) {
        await pharmaService.updateExpenseCategory(selectedCategory.id, categoryFormData as ExpenseCategory);
        enqueueSnackbar('Category updated successfully', { variant: 'success' });
      } else {
        await pharmaService.createExpenseCategory(categoryFormData as ExpenseCategory);
        enqueueSnackbar('Category created successfully', { variant: 'success' });
      }
      await loadData();
      setShowCategoryDialog(false);
    } catch (error: any) {
      console.error('Failed to save category:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to save category', { variant: 'error' });
    }
  };

  const handleSave = async () => {
    if (!formData.territoryId || !formData.expenseCategoryId || !formData.expenseAmount) {
      enqueueSnackbar('Please fill in all required fields', { variant: 'warning' });
      return;
    }

    try {
      if (selectedExpense?.id) {
        await pharmaService.updateExpense(selectedExpense.id, formData as Expense);
        enqueueSnackbar('Expense updated successfully', { variant: 'success' });
      } else {
        await pharmaService.createExpense(formData as Expense);
        enqueueSnackbar('Expense created successfully', { variant: 'success' });
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save expense:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to save expense', { variant: 'error' });
    }
  };

  const handleSubmit = async (id: string) => {
    const expense = expenses.find(e => e.id === id);
    if (!expense) return;

    // Check if this expense would exceed the limit
    if (expense.territoryId && expense.year && expense.month) {
      try {
        const currentTotal = await pharmaService.getTotalExpensesForTerritory(expense.territoryId, expense.year, expense.month);
        const target = await pharmaService.getActiveTargetForTerritoryAndMonth(expense.territoryId, expense.year, expense.month);
        
        if (target) {
          const limit = target.targetAmount * 0.3;
          const projectedTotal = currentTotal + (expense.expenseAmount || 0);
          
          if (projectedTotal > limit) {
            const exceedAmount = projectedTotal - limit;
            const message = `WARNING: Submitting this expense will exceed the 30% limit by ${exceedAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}. This will make the area INELIGIBLE for incentives. Continue?`;
            if (!window.confirm(message)) return;
          }
        }
      } catch (error) {
        console.error('Failed to check expense limit:', error);
      }
    }

    try {
      await pharmaService.submitExpense(id);
      enqueueSnackbar('Expense submitted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to submit expense:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to submit expense', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this draft expense?')) return;
    try {
      await pharmaService.deleteExpense(id);
      enqueueSnackbar('Expense deleted successfully', { variant: 'success' });
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete expense:', error);
      enqueueSnackbar(error.response?.data?.message || 'Failed to delete expense', { variant: 'error' });
    }
  };

  const getTerritoryName = (territoryId: string): string => {
    const territory = territories.find(t => t.id === territoryId);
    return territory ? territory.name : '-';
  };

  const getCategoryName = (categoryId: string): string => {
    const category = categories.find(c => c.id === categoryId);
    return category ? category.name : '-';
  };

  const getEmployeeName = (employeeId: string): string => {
    if (!employeeId) return '-';
    const employee = employees.find(e => (e.id === employeeId) || (e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || '-') : '-';
  };

  const expensePercentage = expenseLimit > 0 ? (currentExpenseTotal / expenseLimit) * 100 : 0;
  const projectedTotal = formData.territoryId && formData.expenseAmount ? currentExpenseTotal + formData.expenseAmount : currentExpenseTotal;
  const projectedPercentage = expenseLimit > 0 ? (projectedTotal / expenseLimit) * 100 : 0;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Expense Management</Typography>
        {tabValue === 0 && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            New Expense
          </Button>
        )}
        {tabValue === 1 && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleCategoryOpenDialog()}
          >
            New Category
          </Button>
        )}
      </Box>

      <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ mb: 3 }}>
        <Tab label="Expenses" />
        <Tab label="Expense Categories" icon={<CategoryIcon />} iconPosition="start" />
      </Tabs>

      {tabValue === 0 && (
        <Card>
          <CardContent>
            <Alert severity="warning" sx={{ mb: 2 }}>
              Reminder: Total area-wise expenses must remain within <strong>30% of the monthly target</strong> to be eligible for incentives.
            </Alert>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Territory</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Employee</TableCell>
                    <TableCell align="right">Amount</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {expenses.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No expenses found
                      </TableCell>
                    </TableRow>
                  ) : (
                    expenses.map((e) => (
                      <TableRow key={e.id}>
                        <TableCell>{e.expenseDate?.split('T')[0]}</TableCell>
                        <TableCell>{getTerritoryName(e.territoryId)}</TableCell>
                        <TableCell>{getCategoryName(e.expenseCategoryId)}</TableCell>
                        <TableCell>{getEmployeeName(e.sourceEmployeeId || '')}</TableCell>
                        <TableCell align="right">{e.expenseAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</TableCell>
                        <TableCell>
                          <Chip
                            label={e.status}
                            size="small"
                            color={e.status === 'SUBMITTED' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton size="small" onClick={() => handleOpenDialog(e)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {e.status === 'DRAFT' && (
                            <Tooltip title="Submit">
                              <IconButton size="small" color="primary" onClick={() => e.id && handleSubmit(e.id)}>
                                <SendIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {e.status === 'DRAFT' && (
                            <Tooltip title="Delete">
                              <IconButton size="small" color="error" onClick={() => e.id && handleDelete(e.id)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
      )}

      {tabValue === 1 && (
        <Card>
          <CardContent>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Category Name</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {categories.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">No categories found</TableCell>
                    </TableRow>
                  ) : (
                    categories.map((cat) => (
                      <TableRow key={cat.id}>
                        <TableCell>{cat.name}</TableCell>
                        <TableCell>{cat.description || '-'}</TableCell>
                        <TableCell>
                          <Chip label={cat.isActive ? 'Active' : 'Inactive'} size="small" color={cat.isActive ? 'success' : 'default'} />
                        </TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleCategoryOpenDialog(cat)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {/* Expense Dialog */}
      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedExpense ? 'Edit Expense' : 'New Expense Entry'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TerritoryTreeSelector
                label="Territory"
                value={formData.territoryId || ''}
                onChange={handleTerritoryChange}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>Category</InputLabel>
                <Select
                  value={formData.expenseCategoryId || ''}
                  label="Category"
                  onChange={(e) => setFormData({ ...formData, expenseCategoryId: e.target.value })}
                >
                  {categories.map((cat) => (
                    <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Expense Date"
                type="date"
                value={formData.expenseDate || ''}
                onChange={(e) => handleDateChange(e.target.value)}
                required
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Amount"
                type="number"
                value={formData.expenseAmount || 0}
                onChange={(e) => {
                  const amount = Number(e.target.value);
                  setFormData({ ...formData, expenseAmount: amount });
                }}
                required
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
              />
            </Grid>
            
            {/* Expense Limit Indicator */}
            {formData.territoryId && expenseLimit > 0 && (
              <Grid item xs={12}>
                <Card variant="outlined" sx={{ p: 2, bgcolor: projectedPercentage > 100 ? 'error.light' : projectedPercentage > 80 ? 'warning.light' : 'success.light' }}>
                  <Typography variant="subtitle2" gutterBottom>Expense Limit Tracking (30% of Target)</Typography>
                  <Box sx={{ mb: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="body2">Current Expenses: {currentExpenseTotal.toLocaleString(undefined, { minimumFractionDigits: 2 })}</Typography>
                      <Typography variant="body2">Limit: {expenseLimit.toLocaleString(undefined, { minimumFractionDigits: 2 })}</Typography>
                    </Box>
                    {formData.expenseAmount > 0 && (
                      <Typography variant="body2" color={projectedPercentage > 100 ? 'error' : 'text.secondary'}>
                        Projected Total: {projectedTotal.toLocaleString(undefined, { minimumFractionDigits: 2 })} ({projectedPercentage.toFixed(1)}%)
                      </Typography>
                    )}
                  </Box>
                  <LinearProgress 
                    variant="determinate" 
                    value={Math.min(projectedPercentage, 100)} 
                    color={projectedPercentage > 100 ? 'error' : projectedPercentage > 80 ? 'warning' : 'success'}
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                  {projectedPercentage > 100 && (
                    <Alert severity="error" sx={{ mt: 1 }}>
                      This expense will exceed the 30% limit! Area will be INELIGIBLE for incentives.
                    </Alert>
                  )}
                  {projectedPercentage > 80 && projectedPercentage <= 100 && (
                    <Alert severity="warning" sx={{ mt: 1 }}>
                      Approaching expense limit. Current: {expensePercentage.toFixed(1)}%
                    </Alert>
                  )}
                </Card>
              </Grid>
            )}
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Responsible Employee</InputLabel>
                <Select
                  value={formData.sourceEmployeeId || ''}
                  label="Responsible Employee"
                  onChange={(e) => setFormData({ ...formData, sourceEmployeeId: e.target.value })}
                >
                  <MenuItem value="">None</MenuItem>
                  {employees.map((emp) => {
                    const employeeIdValue = emp.employeeId || emp.id || '';
                    return (
                      <MenuItem key={employeeIdValue} value={employeeIdValue}>
                        {emp.name || emp.employeeNumber}
                        {emp.employeeNumber ? ` (${emp.employeeNumber})` : ''}
                      </MenuItem>
                    );
                  })}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={2}
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">Save Draft</Button>
        </DialogActions>
      </Dialog>

      {/* Category Dialog */}
      <Dialog open={showCategoryDialog} onClose={() => setShowCategoryDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedCategory ? 'Edit Expense Category' : 'New Expense Category'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Category Name"
                value={categoryFormData.name || ''}
                onChange={(e) => setCategoryFormData({ ...categoryFormData, name: e.target.value })}
                required
                placeholder="e.g., Sample Products, Gifts to Doctors"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={3}
                value={categoryFormData.description || ''}
                onChange={(e) => setCategoryFormData({ ...categoryFormData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={categoryFormData.isActive ? 'active' : 'inactive'}
                  label="Status"
                  onChange={(e) => setCategoryFormData({ ...categoryFormData, isActive: e.target.value === 'active' })}
                >
                  <MenuItem value="active">Active</MenuItem>
                  <MenuItem value="inactive">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowCategoryDialog(false)}>Cancel</Button>
          <Button onClick={handleCategorySave} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ExpenseManagement;
