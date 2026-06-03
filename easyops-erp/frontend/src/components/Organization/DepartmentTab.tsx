import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Paper,
  TextField,
  Typography,
  Card,
  CardContent,
  Collapse,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ExpandMore,
  ChevronRight,
  FolderOpen,
  Folder,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';
import { Department, DepartmentFormData } from '@/types/organization';

interface DepartmentTabProps {
  organizationId: string;
}

const DepartmentTreeNode: React.FC<{
  department: Department;
  onEdit: (dept: Department) => void;
  onDelete: (id: string) => void;
}> = ({ department, onEdit, onDelete }) => {
  const [open, setOpen] = useState(false);
  const hasChildren = department.children && department.children.length > 0;

  return (
    <>
      <ListItem
        sx={{
          pl: 2,
          '&:hover': { bgcolor: 'action.hover' },
          borderRadius: 1,
        }}
      >
        <ListItemIcon onClick={() => hasChildren && setOpen(!open)} sx={{ cursor: 'pointer', minWidth: 36 }}>
          {hasChildren ? (open ? <ExpandMore /> : <ChevronRight />) : <Folder />}
        </ListItemIcon>
        <ListItemText
          primary={department.name}
          secondary={`${department.code} • ${department.type || 'DEPARTMENT'}`}
        />
        <Box>
          <IconButton size="small" onClick={() => onEdit(department)}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" onClick={() => onDelete(department.id)} color="error">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      </ListItem>
      {hasChildren && (
        <Collapse in={open} timeout="auto" unmountOnExit>
          <List component="div" disablePadding sx={{ pl: 4 }}>
            {department.children!.map((child) => (
              <DepartmentTreeNode
                key={child.id}
                department={child}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </List>
        </Collapse>
      )}
    </>
  );
};

const DepartmentTab: React.FC<DepartmentTabProps> = ({ organizationId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingDept, setEditingDept] = useState<Department | null>(null);
  const [formData, setFormData] = useState<DepartmentFormData>({
    code: '',
    name: '',
    type: 'DEPARTMENT',
  });

  useEffect(() => {
    fetchDepartments();
  }, [organizationId]);

  const fetchDepartments = async () => {
    try {
      const data = await organizationService.getDepartmentTree(organizationId);
      setDepartments(data);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch departments', {
        variant: 'error',
      });
    }
  };

  const handleOpenDialog = (dept?: Department) => {
    if (dept) {
      setEditingDept(dept);
      setFormData({
        code: dept.code,
        name: dept.name,
        description: dept.description,
        type: dept.type || 'DEPARTMENT',
        parentDepartmentId: dept.parentDepartmentId,
        costCenter: dept.costCenter,
        status: dept.status,
      });
    } else {
      setEditingDept(null);
      setFormData({
        code: '',
        name: '',
        type: 'DEPARTMENT',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingDept(null);
  };

  const handleSubmit = async () => {
    try {
      if (editingDept) {
        await organizationService.updateDepartment(editingDept.id, formData);
        enqueueSnackbar('Department updated successfully', { variant: 'success' });
      } else {
        await organizationService.createDepartment(organizationId, formData);
        enqueueSnackbar('Department created successfully', { variant: 'success' });
      }
      handleCloseDialog();
      fetchDepartments();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Operation failed', { variant: 'error' });
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this department?')) {
      try {
        await organizationService.deleteDepartment(id);
        enqueueSnackbar('Department deleted successfully', { variant: 'success' });
        fetchDepartments();
      } catch (error: any) {
        enqueueSnackbar(error.response?.data?.message || 'Delete failed', { variant: 'error' });
      }
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6">Department Structure</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog()}>
          Add Department
        </Button>
      </Box>

      {departments.length > 0 ? (
        <Paper>
          <List>
            {departments.map((dept) => (
              <DepartmentTreeNode
                key={dept.id}
                department={dept}
                onEdit={handleOpenDialog}
                onDelete={handleDelete}
              />
            ))}
          </List>
        </Paper>
      ) : (
        <Card>
          <CardContent>
            <Typography variant="body2" color="textSecondary" align="center">
              No departments found. Create one to get started!
            </Typography>
          </CardContent>
        </Card>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingDept ? 'Edit Department' : 'Create Department'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Code *"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                disabled={!!editingDept}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Name *"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Type"
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              >
                <MenuItem value="DEPARTMENT">Department</MenuItem>
                <MenuItem value="DIVISION">Division</MenuItem>
                <MenuItem value="UNIT">Unit</MenuItem>
                <MenuItem value="TEAM">Team</MenuItem>
                <MenuItem value="BRANCH">Branch</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Cost Center"
                value={formData.costCenter || ''}
                onChange={(e) => setFormData({ ...formData, costCenter: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Description"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={!formData.code || !formData.name}>
            {editingDept ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DepartmentTab;

