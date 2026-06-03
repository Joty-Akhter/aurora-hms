import React, { useState, useEffect } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  TextField, Typography, Grid, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, IconButton, Chip, Tabs, Tab, Link, Tooltip,
  CircularProgress, LinearProgress, Alert, FormControl, InputLabel, Select, MenuItem
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon, Warehouse as WarehouseIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Division, Region, Territory, Area } from '../../services/pharmaService';
import inventoryService from '../../services/inventoryService';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';

const TerritoryManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [tabValue, setTabValue] = useState(0);
  const [divisions, setDivisions] = useState<Division[]>([]);
  const [regions, setRegions] = useState<Region[]>([]);
  const [territories, setTerritories] = useState<Territory[]>([]);
  const [areas, setAreas] = useState<Area[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [bulkCreating, setBulkCreating] = useState(false);
  const [selectedDivision, setSelectedDivision] = useState<Division | null>(null);
  const [selectedRegion, setSelectedRegion] = useState<Region | null>(null);
  const [selectedArea, setSelectedArea] = useState<Area | null>(null);
  const [showDialog, setShowDialog] = useState(false);
  const [dialogType, setDialogType] = useState<'division' | 'region' | 'territory' | 'area'>('division');
  const [formData, setFormData] = useState<any>({});

  useEffect(() => {
    if (currentOrganizationId) {
      loadDivisions();
    }
  }, [currentOrganizationId]);

  const loadDivisions = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const data = await pharmaService.getActiveDivisions(currentOrganizationId);
      setDivisions(data);
    } catch (error) {
      console.error('Failed to load divisions:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadRegions = async (divisionId: string) => {
    try {
      const data = await pharmaService.getRegionsByDivision(divisionId);
      setRegions(data);
    } catch (error) {
      console.error('Failed to load regions:', error);
    }
  };

  const loadAreas = async (regionId: string) => {
    try {
      const data = await pharmaService.getAreasByRegion(regionId);
      setAreas(data);
    } catch (error) {
      console.error('Failed to load areas:', error);
    }
  };

  const loadTerritories = async (areaId: string) => {
    try {
      const data = await pharmaService.getTerritoriesByArea(areaId);
      setTerritories(data);
    } catch (error) {
      console.error('Failed to load territories:', error);
    }
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
    if (newValue === 1 && selectedDivision) {
      loadRegions(selectedDivision.id);
    } else if (newValue === 2 && selectedRegion) {
      loadAreas(selectedRegion.id);
    } else if (newValue === 3 && selectedArea) {
      loadTerritories(selectedArea.id);
    }
  };

  const handleDivisionChange = (divisionId: string) => {
    const div = divisions.find(d => d.id === divisionId);
    if (div) {
      setSelectedDivision(div);
      setSelectedRegion(null);
      setSelectedArea(null);
      loadRegions(div.id);
      if (tabValue === 2) {
        setAreas([]);
      }
      if (tabValue === 3) {
        setAreas([]);
        setTerritories([]);
      }
    }
  };

  const handleRegionChange = (regionId: string) => {
    const reg = regions.find(r => r.id === regionId);
    if (reg) {
      setSelectedRegion(reg);
      setSelectedArea(null);
      loadAreas(reg.id);
      if (tabValue === 3) setTerritories([]);
    }
  };

  const handleAreaChange = (areaId: string) => {
    const area = areas.find(a => a.id === areaId);
    if (area) {
      setSelectedArea(area);
      loadTerritories(area.id);
    }
  };

  const handleOpenDialog = (type: 'division' | 'region' | 'territory' | 'area', item?: any) => {
    setDialogType(type);
    if (item) {
      setFormData(item);
    } else {
      setFormData({
        organizationId: currentOrganizationId,
        name: '',
        code: '',
        description: '',
        status: 'ACTIVE',
        isActive: true,
        ...(type === 'region' && { divisionId: selectedDivision?.id }),
        ...(type === 'area' && { regionId: selectedRegion?.id, divisionId: selectedDivision?.id }),
        ...(type === 'territory' && { areaId: selectedArea?.id, regionId: selectedRegion?.id, divisionId: selectedDivision?.id }),
      });
    }
    setShowDialog(true);
  };

  const handleSave = async () => {
    if (dialogType === 'territory' && !formData.id) {
      // Show loading state for territory creation (includes warehouse creation)
      setSaving(true);
    }
    
    try {
      if (formData.id) {
        // Update - exclude warehouseId as it's managed by backend
        const updateData = { ...formData };
        if (dialogType === 'territory') {
          delete updateData.warehouseId; // Don't send warehouseId on update
        }
        
        if (dialogType === 'division') {
          await pharmaService.updateDivision(formData.id, updateData);
          await loadDivisions();
          enqueueSnackbar('Division updated successfully', { variant: 'success' });
        } else if (dialogType === 'region') {
          await pharmaService.updateRegion(formData.id, updateData);
          if (selectedDivision) await loadRegions(selectedDivision.id);
          enqueueSnackbar('Region updated successfully', { variant: 'success' });
        } else if (dialogType === 'territory') {
          await pharmaService.updateTerritory(formData.id, updateData);
          if (selectedArea) await loadTerritories(selectedArea.id);
          enqueueSnackbar('Territory updated successfully', { variant: 'success' });
        } else if (dialogType === 'area') {
          await pharmaService.updateArea(formData.id, updateData);
          if (selectedRegion) await loadAreas(selectedRegion.id);
          enqueueSnackbar('Area updated successfully', { variant: 'success' });
        }
      } else {
        // Create - warehouseId will be auto-created by backend for territories
        const createData = { ...formData };
        if (dialogType === 'territory') {
          delete createData.warehouseId; // Don't send warehouseId on create, backend will create it
        }
        
        if (dialogType === 'division') {
          await pharmaService.createDivision(createData);
          await loadDivisions();
          enqueueSnackbar('Division created successfully', { variant: 'success' });
        } else if (dialogType === 'region') {
          await pharmaService.createRegion(createData);
          if (selectedDivision) await loadRegions(selectedDivision.id);
          enqueueSnackbar('Region created successfully', { variant: 'success' });
        } else if (dialogType === 'territory') {
          try {
            await pharmaService.createTerritory(createData);
            if (selectedArea) await loadTerritories(selectedArea.id);
            enqueueSnackbar('Territory and warehouse created successfully', { variant: 'success' });
          } catch (territoryError: any) {
            // Enhanced error handling for warehouse creation
            const errorMessage = territoryError.response?.data?.message || territoryError.message || 'Failed to create territory';
            let userMessage = 'Failed to create territory';
            
            if (errorMessage.toLowerCase().includes('warehouse') || errorMessage.toLowerCase().includes('inventory')) {
              userMessage = 'Territory created, but warehouse creation failed. The territory was saved but may need manual warehouse setup.';
              enqueueSnackbar(userMessage, { variant: 'warning', autoHideDuration: 6000 });
              if (selectedArea) await loadTerritories(selectedArea.id);
            } else {
              userMessage = `Failed to create territory: ${errorMessage}`;
              enqueueSnackbar(userMessage, { variant: 'error', autoHideDuration: 6000 });
              throw territoryError;
            }
            return;
          }
        } else if (dialogType === 'area') {
          await pharmaService.createArea(createData);
          if (selectedRegion) await loadAreas(selectedRegion.id);
          enqueueSnackbar('Area created successfully', { variant: 'success' });
        }
      }
      setShowDialog(false);
      setFormData({});
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to save';
      
      // Specific error handling for different scenarios
      if (error.response?.status === 400) {
        enqueueSnackbar(`Validation error: ${errorMessage}`, { variant: 'error' });
      } else if (error.response?.status === 404) {
        enqueueSnackbar('Resource not found. Please refresh the page.', { variant: 'error' });
      } else if (error.response?.status === 500) {
        enqueueSnackbar('Server error occurred. Please try again later.', { variant: 'error' });
      } else if (errorMessage.toLowerCase().includes('warehouse')) {
        enqueueSnackbar(`Warehouse operation failed: ${errorMessage}`, { variant: 'error', autoHideDuration: 6000 });
      } else {
        enqueueSnackbar(errorMessage, { variant: 'error' });
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (type: string, id: string) => {
    if (!window.confirm('Are you sure you want to delete this item?')) return;
    try {
      if (type === 'division') {
        await pharmaService.deleteDivision(id);
        await loadDivisions();
        enqueueSnackbar('Division deleted successfully', { variant: 'success' });
      } else if (type === 'region') {
        await pharmaService.deleteRegion(id);
        if (selectedDivision) await loadRegions(selectedDivision.id);
        enqueueSnackbar('Region deleted successfully', { variant: 'success' });
      } else if (type === 'territory') {
        await pharmaService.deleteTerritory(id);
        if (selectedArea) await loadTerritories(selectedArea.id);
        enqueueSnackbar('Territory deleted successfully', { variant: 'success' });
      } else if (type === 'area') {
        await pharmaService.deleteArea(id);
        if (selectedRegion) await loadAreas(selectedRegion.id);
        enqueueSnackbar('Area deleted successfully', { variant: 'success' });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to delete';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    }
  };

  const handleBulkCreateWarehouses = async () => {
    const territoriesWithoutWarehouses = territories.filter(t => !t.warehouseId);
    
    if (territoriesWithoutWarehouses.length === 0) {
      enqueueSnackbar('All territories already have warehouses', { variant: 'info' });
      return;
    }

    if (!window.confirm(`This will create warehouses for ${territoriesWithoutWarehouses.length} territory(ies). Continue?`)) {
      return;
    }

    if (!currentOrganizationId) {
      enqueueSnackbar('Organization ID is required', { variant: 'error' });
      return;
    }

    setBulkCreating(true);
    let successCount = 0;
    let failureCount = 0;
    const errors: string[] = [];

    try {
      for (const territory of territoriesWithoutWarehouses) {
        try {
          // Generate warehouse code from territory code or name
          const warehouseCode = territory.code 
            ? `${territory.code.toUpperCase()}-WH` 
            : `TERR-${territory.name.replace(/[^A-Za-z0-9]/g, '-').toUpperCase()}-WH`;
          
          // Create warehouse via inventory service
          const warehouseData = {
            organizationId: currentOrganizationId,
            code: warehouseCode,
            name: `${territory.name} Warehouse`,
            warehouseType: 'DISTRIBUTION',
            description: `Auto-created warehouse for territory: ${territory.name}`,
            isActive: territory.isActive !== false,
            status: 'OPERATIONAL',
            createdBy: territory.createdBy
          };

          const createdWarehouse = await inventoryService.createWarehouse(warehouseData);
          
          // Update territory with warehouse ID
          try {
            await pharmaService.updateTerritory(territory.id, {
              ...territory,
              warehouseId: createdWarehouse.id
            });
            successCount++;
          } catch (updateError: any) {
            // Warehouse created but failed to link - log error but continue
            errors.push(`${territory.name}: Warehouse created but failed to link: ${updateError.message || 'Update failed'}`);
            failureCount++;
          }
        } catch (error: any) {
          failureCount++;
          const errorMessage = error.response?.data?.message || error.message || 'Failed to create warehouse';
          errors.push(`${territory.name}: ${errorMessage}`);
          console.error(`Failed to create warehouse for territory ${territory.name}:`, error);
        }
      }

      // Show summary
      if (successCount > 0) {
        enqueueSnackbar(
          `Successfully created warehouses for ${successCount} territory(ies)${failureCount > 0 ? `, ${failureCount} failed` : ''}`,
          { variant: failureCount > 0 ? 'warning' : 'success', autoHideDuration: 6000 }
        );
        if (selectedArea) await loadTerritories(selectedArea.id);
      } else {
        enqueueSnackbar(
          `Failed to create warehouses. Please check errors.`,
          { variant: 'error', autoHideDuration: 6000 }
        );
      }

      // Log detailed errors for debugging
      if (errors.length > 0) {
        console.error('Bulk warehouse creation errors:', errors);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Bulk operation failed';
      enqueueSnackbar(errorMessage, { variant: 'error' });
    } finally {
      setBulkCreating(false);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>Territory Management</Typography>
      
      <Tabs value={tabValue} onChange={handleTabChange} sx={{ mb: 3 }}>
        <Tab label="Divisions" />
        <Tab label="Regions" disabled={!selectedDivision} />
        <Tab label="Areas" disabled={!selectedRegion} />
        <Tab label="Territories" disabled={!selectedArea} />
      </Tabs>

      {tabValue === 0 && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">Divisions</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog('division')}>
                Add Division
              </Button>
            </Box>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Code</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {divisions.map((div) => (
                    <TableRow key={div.id} onClick={() => { setSelectedDivision(div); setTabValue(1); loadRegions(div.id); }} style={{ cursor: 'pointer' }}>
                      <TableCell>{div.name}</TableCell>
                      <TableCell>{div.code || '-'}</TableCell>
                      <TableCell><Chip label={div.status} color={div.isActive ? 'success' : 'default'} size="small" /></TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleOpenDialog('division', div); }}>
                          <EditIcon />
                        </IconButton>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDelete('division', div.id); }}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {tabValue === 1 && selectedDivision && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', mb: 2 }}>
              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Division</InputLabel>
                <Select
                  value={selectedDivision.id}
                  label="Division"
                  onChange={(e) => handleDivisionChange(e.target.value)}
                >
                  {divisions.map((div) => (
                    <MenuItem key={div.id} value={div.id}>{div.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Box sx={{ flex: 1 }} />
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog('region')}>
                Add Region
              </Button>
            </Box>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Code</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {regions.map((region) => (
                    <TableRow key={region.id} onClick={() => { setSelectedRegion(region); setTabValue(2); loadAreas(region.id); }} style={{ cursor: 'pointer' }}>
                      <TableCell>{region.name}</TableCell>
                      <TableCell>{region.code || '-'}</TableCell>
                      <TableCell><Chip label={region.status} color={region.isActive ? 'success' : 'default'} size="small" /></TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleOpenDialog('region', region); }}>
                          <EditIcon />
                        </IconButton>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDelete('region', region.id); }}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {tabValue === 2 && selectedDivision && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', mb: 2 }}>
              <FormControl size="small" sx={{ minWidth: 180 }}>
                <InputLabel>Division</InputLabel>
                <Select
                  value={selectedDivision.id}
                  label="Division"
                  onChange={(e) => handleDivisionChange(e.target.value)}
                >
                  {divisions.map((div) => (
                    <MenuItem key={div.id} value={div.id}>{div.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 180 }}>
                <InputLabel>Region</InputLabel>
                <Select
                  value={selectedRegion?.id ?? ''}
                  label="Region"
                  onChange={(e) => handleRegionChange(e.target.value)}
                >
                  <MenuItem value=""><em>Select region</em></MenuItem>
                  {regions.map((reg) => (
                    <MenuItem key={reg.id} value={reg.id}>{reg.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Box sx={{ flex: 1 }} />
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog('area')} disabled={!selectedRegion}>
                Add Area
              </Button>
            </Box>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Code</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {areas.map((area) => (
                    <TableRow key={area.id} onClick={() => { setSelectedArea(area); setTabValue(3); loadTerritories(area.id); }} style={{ cursor: 'pointer' }}>
                      <TableCell>{area.name}</TableCell>
                      <TableCell>{area.code || '-'}</TableCell>
                      <TableCell><Chip label={area.status} color={area.isActive ? 'success' : 'default'} size="small" /></TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleOpenDialog('area', area); }}>
                          <EditIcon />
                        </IconButton>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDelete('area', area.id); }}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      {tabValue === 3 && selectedDivision && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', mb: 2 }}>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Division</InputLabel>
                <Select
                  value={selectedDivision.id}
                  label="Division"
                  onChange={(e) => handleDivisionChange(e.target.value)}
                >
                  {divisions.map((div) => (
                    <MenuItem key={div.id} value={div.id}>{div.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Region</InputLabel>
                <Select
                  value={selectedRegion?.id ?? ''}
                  label="Region"
                  onChange={(e) => handleRegionChange(e.target.value)}
                >
                  <MenuItem value=""><em>Select region</em></MenuItem>
                  {regions.map((reg) => (
                    <MenuItem key={reg.id} value={reg.id}>{reg.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Area</InputLabel>
                <Select
                  value={selectedArea?.id ?? ''}
                  label="Area"
                  onChange={(e) => handleAreaChange(e.target.value)}
                >
                  <MenuItem value=""><em>Select area</em></MenuItem>
                  {areas.map((a) => (
                    <MenuItem key={a.id} value={a.id}>{a.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Box sx={{ flex: 1 }} />
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                {territories.filter(t => !t.warehouseId).length > 0 && (
                  <Button
                    variant="outlined"
                    startIcon={bulkCreating ? <CircularProgress size={16} /> : <RefreshIcon />}
                    onClick={handleBulkCreateWarehouses}
                    disabled={bulkCreating}
                    color="warning"
                  >
                    {bulkCreating ? 'Creating...' : `Create Warehouses (${territories.filter(t => !t.warehouseId).length})`}
                  </Button>
                )}
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpenDialog('territory')} disabled={!selectedArea}>
                  Add Territory
                </Button>
              </Box>
            </Box>
            {territories.filter(t => !t.warehouseId).length > 0 && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {territories.filter(t => !t.warehouseId).length} territory(ies) without warehouses. 
                Use the "Create Warehouses" button to create warehouses for all territories at once.
              </Alert>
            )}
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Code</TableCell>
                    <TableCell>Warehouse</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {territories.map((territory) => (
                    <TableRow key={territory.id}>
                      <TableCell>{territory.name}</TableCell>
                      <TableCell>{territory.code || '-'}</TableCell>
                      <TableCell>
                        {territory.warehouseId ? (
                          <Tooltip title="View warehouse details">
                            <Link
                              component="button"
                              variant="body2"
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/inventory/warehouses/${territory.warehouseId}`);
                              }}
                              sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
                            >
                              <WarehouseIcon fontSize="small" />
                              Linked
                            </Link>
                          </Tooltip>
                        ) : (
                          <Chip label="Not created" color="warning" size="small" variant="outlined" />
                        )}
                      </TableCell>
                      <TableCell><Chip label={territory.status} color={territory.isActive ? 'success' : 'default'} size="small" /></TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleOpenDialog('territory', territory)}>
                          <EditIcon />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleDelete('territory', territory.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      <Dialog open={showDialog} onClose={() => !saving && setShowDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{formData.id ? 'Edit' : 'Create'} {dialogType.charAt(0).toUpperCase() + dialogType.slice(1)}</DialogTitle>
        {saving && <LinearProgress />}
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField fullWidth label="Name" value={formData.name || ''} onChange={(e) => setFormData({...formData, name: e.target.value})} required />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Code" value={formData.code || ''} onChange={(e) => setFormData({...formData, code: e.target.value})} />
            </Grid>
            {dialogType === 'territory' && formData.warehouseId && (
              <Grid item xs={12}>
                <Box sx={{ p: 2, bgcolor: 'info.light', borderRadius: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <WarehouseIcon color="primary" />
                  <Typography variant="body2">
                    Warehouse: <Link component="button" variant="body2" onClick={() => navigate(`/inventory/warehouses/${formData.warehouseId}`)}>
                      View Warehouse
                    </Link>
                  </Typography>
                </Box>
              </Grid>
            )}
            {dialogType === 'territory' && !formData.id && (
              <Grid item xs={12}>
                <Box sx={{ p: 1.5, bgcolor: 'success.light', borderRadius: 1 }}>
                  <Typography variant="body2" color="success.dark">
                    <strong>Note:</strong> A warehouse will be automatically created for this territory when you save.
                  </Typography>
                </Box>
              </Grid>
            )}
            <Grid item xs={12}>
              <TextField fullWidth label="Description" multiline rows={3} value={formData.description || ''} onChange={(e) => setFormData({...formData, description: e.target.value})} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowDialog(false)} disabled={saving}>Cancel</Button>
          <Button 
            onClick={handleSave} 
            variant="contained" 
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? (dialogType === 'territory' && !formData.id ? 'Creating Territory & Warehouse...' : 'Saving...') : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TerritoryManagement;

