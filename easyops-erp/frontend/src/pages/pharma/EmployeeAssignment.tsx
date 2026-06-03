import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, IconButton, Chip, MenuItem, Select, FormControl, InputLabel
} from '@mui/material';
import TerritoryTreeSelector from '../../components/pharma/TerritoryTreeSelector';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { EmployeeTerritoryAssignment, Territory } from '../../services/pharmaService';
import { getEmployees, Employee } from '../../services/hrService';

const PHARMA_ROLES = [
  'SR', 'MPO', 'AM', 'TM', 'Sr.AM', 'RM', 'Sr.RM', 'DSM', 'ASM', 'SM'
];

const EmployeeAssignment: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [assignments, setAssignments] = useState<EmployeeTerritoryAssignment[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDialog, setShowDialog] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<EmployeeTerritoryAssignment | null>(null);
  const [formData, setFormData] = useState<Partial<EmployeeTerritoryAssignment>>({
    organizationId: currentOrganizationId,
    assignmentDate: new Date().toISOString().split('T')[0],
    status: 'ACTIVE',
    roleInTerritory: 'SR'
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
      const [assignmentsData, territoriesData, employeesRes] = await Promise.all([
        pharmaService.getAssignments(currentOrganizationId),
        pharmaService.getAllTerritoriesForOrganization(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' })
      ]);
      setAssignments(assignmentsData);
      setTerritories(territoriesData);
      setEmployees(employeesRes.data || []);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (assignment?: EmployeeTerritoryAssignment) => {
    if (assignment) {
      setSelectedAssignment(assignment);
      setFormData({
        ...assignment,
        assignmentDate: assignment.assignmentDate?.split('T')[0] || new Date().toISOString().split('T')[0],
        endDate: assignment.endDate?.split('T')[0] || undefined
      });
    } else {
      setSelectedAssignment(null);
      setFormData({
        organizationId: currentOrganizationId,
        employeeId: '',
        territoryId: '',
        assignmentDate: new Date().toISOString().split('T')[0],
        endDate: undefined,
        roleInTerritory: 'SR',
        status: 'ACTIVE',
        notes: ''
      });
    }
    setShowDialog(true);
  };

  const handleCloseDialog = () => {
    setShowDialog(false);
    setSelectedAssignment(null);
    setFormData({
      organizationId: currentOrganizationId,
      assignmentDate: new Date().toISOString().split('T')[0],
      status: 'ACTIVE',
      roleInTerritory: 'SR'
    });
  };

  const handleSave = async () => {
    if (!formData.employeeId || !formData.territoryId) {
      alert('Please select employee and territory');
      return;
    }

    try {
      if (selectedAssignment?.id) {
        await pharmaService.updateAssignment(selectedAssignment.id, formData as EmployeeTerritoryAssignment);
      } else {
        await pharmaService.createAssignment(formData as EmployeeTerritoryAssignment);
      }
      await loadData();
      handleCloseDialog();
    } catch (error: any) {
      console.error('Failed to save assignment:', error);
      alert(error.response?.data?.message || 'Failed to save assignment');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this assignment?')) return;
    try {
      await pharmaService.deleteAssignment(id);
      await loadData();
    } catch (error: any) {
      console.error('Failed to delete assignment:', error);
      alert(error.response?.data?.message || 'Failed to delete assignment');
    }
  };

  const getTerritoryName = (territoryId: string): string => {
    const territory = territories.find(t => t.id === territoryId);
    return territory ? territory.name : '-';
  };

  const getEmployeeName = (employeeId: string): string => {
    const employee = employees.find(e => (e.id === employeeId) || (e.employeeId === employeeId));
    return employee ? (employee.name || employee.employeeNumber || '-') : '-';
  };

  const filteredAssignments = assignments.filter(assignment => {
    if (!currentOrganizationId) return false;
    return assignment.organizationId === currentOrganizationId;
  });

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Employee Territory Assignments</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          New Assignment
        </Button>
      </Box>

      <Card>
        <CardContent>
          {loading ? (
            <Typography>Loading...</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Employee</TableCell>
                    <TableCell>Territory</TableCell>
                    <TableCell>Role</TableCell>
                    <TableCell>Assignment Date</TableCell>
                    <TableCell>End Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAssignments.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No assignments found
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredAssignments.map((assignment) => (
                      <TableRow key={assignment.id}>
                        <TableCell>{getEmployeeName(assignment.employeeId)}</TableCell>
                        <TableCell>{getTerritoryName(assignment.territoryId)}</TableCell>
                        <TableCell>
                          <Chip label={assignment.roleInTerritory} size="small" color="primary" />
                        </TableCell>
                        <TableCell>{assignment.assignmentDate?.split('T')[0] || '-'}</TableCell>
                        <TableCell>{assignment.endDate?.split('T')[0] || '-'}</TableCell>
                        <TableCell>
                          <Chip
                            label={assignment.status}
                            size="small"
                            color={assignment.status === 'ACTIVE' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(assignment)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => handleDelete(assignment.id)}
                            color="error"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
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

      {/* Assignment Dialog */}
      <Dialog open={showDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedAssignment ? 'Edit Assignment' : 'New Assignment'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel>Employee</InputLabel>
              <Select
                value={formData.employeeId || ''}
                label="Employee"
                onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
              >
                {employees.map((employee) => {
                  const employeeIdValue = employee.employeeId || employee.id || '';
                  return (
                    <MenuItem key={employeeIdValue} value={employeeIdValue}>
                      {employee.name || employee.employeeNumber} 
                      {employee.employeeNumber ? ` (${employee.employeeNumber})` : ''}
                    </MenuItem>
                  );
                })}
              </Select>
            </FormControl>

            <TerritoryTreeSelector
              label="Territory"
              value={formData.territoryId || ''}
              onChange={(territoryId) => setFormData({ ...formData, territoryId })}
              required
            />

            <FormControl fullWidth required>
              <InputLabel>Role in Territory</InputLabel>
              <Select
                value={formData.roleInTerritory || 'SR'}
                label="Role in Territory"
                onChange={(e) => setFormData({ ...formData, roleInTerritory: e.target.value })}
              >
                {PHARMA_ROLES.map((role) => (
                  <MenuItem key={role} value={role}>
                    {role}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Assignment Date"
              type="date"
              value={formData.assignmentDate || ''}
              onChange={(e) => setFormData({ ...formData, assignmentDate: e.target.value })}
              fullWidth
              required
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="End Date (Optional)"
              type="date"
              value={formData.endDate || ''}
              onChange={(e) => setFormData({ ...formData, endDate: e.target.value || undefined })}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />

            <FormControl fullWidth required>
              <InputLabel>Status</InputLabel>
              <Select
                value={formData.status || 'ACTIVE'}
                label="Status"
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              >
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
            </FormControl>

            <TextField
              label="Notes"
              value={formData.notes || ''}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              fullWidth
              multiline
              rows={3}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default EmployeeAssignment;

