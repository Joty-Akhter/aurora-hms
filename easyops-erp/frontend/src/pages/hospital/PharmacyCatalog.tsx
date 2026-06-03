import React, { useEffect, useMemo, useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Typography,
  Chip,
  FormControlLabel,
  CircularProgress,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalPharmacyService, {
  Drug,
  DrugRequest,
  Manufacturer,
} from '../../services/hospitalPharmacyService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PharmacyCatalogPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(true);
  const [drugs, setDrugs] = useState<Drug[]>([]);
  const [manufacturers, setManufacturers] = useState<Manufacturer[]>([]);
  const [search, setSearch] = useState('');
  const [activeOnly, setActiveOnly] = useState(true);
  const [manufacturerFilter, setManufacturerFilter] = useState<string>('');
  const [formSelectValue, setFormSelectValue] = useState<string>(''); // dropdown choice for form ('', preset, or '__other__')

  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(50);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingDrug, setEditingDrug] = useState<Drug | null>(null);
  const [form, setForm] = useState<DrugRequest>({
    genericName: '',
    brandName: '',
    strength: '',
    form: '',
    route: '',
    packSize: '',
    unitOfMeasure: '',
    therapeuticClassId: undefined,
    active: true,
    controlledDrugFlag: false,
    batchRequired: true,
    expiryRequired: true,
    manufacturerId: '',
  });

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    loadDrugs();
  }, [activeOnly, manufacturerFilter]);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const [mfrs] = await Promise.all([
        hospitalPharmacyService.getManufacturers({ activeOnly: true }),
      ]);
      setManufacturers(mfrs);
      await loadDrugs();
    } catch (err: any) {
      console.error('Failed to load pharmacy catalog data:', err);
      enqueueSnackbar('Failed to load pharmacy catalog data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const loadDrugs = async () => {
    try {
      const params: { activeOnly: boolean; manufacturerId?: string } = {
        activeOnly,
      };
      if (manufacturerFilter) params.manufacturerId = manufacturerFilter;
      const data = await hospitalPharmacyService.getDrugs(params);
      setDrugs(data);
    } catch (err: any) {
      console.error('Failed to load drugs:', err);
      enqueueSnackbar('Failed to load drugs', { variant: 'error' });
    }
  };

  const filteredDrugs = useMemo(() => {
    let list = drugs;
    if (activeOnly) {
      list = list.filter((d) => d.active);
    }
    if (!search) return list;
    const term = search.toLowerCase();
    return list.filter(
      (d) =>
        d.genericName.toLowerCase().includes(term) ||
        (d.brandName && d.brandName.toLowerCase().includes(term))
    );
  }, [drugs, search, activeOnly]);

  const pagedDrugs = useMemo(
    () =>
      filteredDrugs.slice(
        page * rowsPerPage,
        page * rowsPerPage + rowsPerPage,
      ),
    [filteredDrugs, page, rowsPerPage],
  );

  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleOpenCreate = () => {
    setEditingDrug(null);
    setForm({
      genericName: '',
      brandName: '',
      strength: '',
      form: '',
      route: '',
      packSize: '',
      unitOfMeasure: '',
      therapeuticClassId: undefined,
      active: true,
      controlledDrugFlag: false,
      batchRequired: true,
      expiryRequired: true,
      manufacturerId: '',
    });
    setFormSelectValue('');
    setDialogOpen(true);
  };

  const handleOpenEdit = (drug: Drug) => {
    setEditingDrug(drug);
    setForm({
      genericName: drug.genericName,
      brandName: drug.brandName,
      strength: drug.strength,
      form: drug.form,
      route: drug.route,
      packSize: drug.packSize,
      unitOfMeasure: drug.unitOfMeasure,
      therapeuticClassId: drug.therapeuticClassId,
      active: drug.active,
      controlledDrugFlag: drug.controlledDrugFlag,
      batchRequired: drug.batchRequired,
      expiryRequired: drug.expiryRequired,
      manufacturerId: drug.manufacturerId,
    });
    setFormSelectValue(drug.form || '');
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (!form.genericName || !form.manufacturerId) {
        enqueueSnackbar('Generic name and manufacturer are required', { variant: 'warning' });
        return;
      }

      if (editingDrug) {
        await hospitalPharmacyService.updateDrug(editingDrug.id, form);
        enqueueSnackbar('Drug updated successfully', { variant: 'success' });
      } else {
        await hospitalPharmacyService.createDrug(form);
        enqueueSnackbar('Drug created successfully', { variant: 'success' });
      }

      setDialogOpen(false);
      setPage(0);
      await loadDrugs();
    } catch (err: any) {
      console.error('Failed to save drug:', err);
      const message = ehrApiErrorMessage(err, 'Failed to save drug');
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  if (loading) {
    return (
      <Box className="hospital-page" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4" component="h1">
            Pharmacy Drug Catalog
          </Typography>
          <Typography variant="body2">
            Centralized hospital drug master used for prescribing and dispensing.
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button
            variant="contained"
            color="secondary"
            startIcon={<RefreshIcon />}
            onClick={loadDrugs}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleOpenCreate}
          >
            Add Drug
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Search by generic or brand name"
              size="small"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ minWidth: 260, flex: 1 }}
            />
            <Autocomplete
              size="small"
              sx={{ minWidth: 220 }}
              options={manufacturers}
              getOptionLabel={(option) => option.name}
              value={
                manufacturers.find((m) => m.id === manufacturerFilter) || null
              }
              onChange={(_, value) =>
                setManufacturerFilter(value ? value.id : '')
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Manufacturer filter"
                  placeholder="All manufacturers"
                />
              )}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={activeOnly}
                  onChange={(e) => setActiveOnly(e.target.checked)}
                  color="primary"
                />
              }
              label="Active only"
            />
          </Box>
        </CardContent>
      </Card>

      <TableContainer component={Card}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Generic Name</TableCell>
              <TableCell>Brand Name</TableCell>
              <TableCell>Strength</TableCell>
              <TableCell>Form</TableCell>
              <TableCell>Route</TableCell>
              <TableCell>Pack Size</TableCell>
              <TableCell>Manufacturer</TableCell>
              <TableCell>Flags</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {pagedDrugs.map((drug) => (
              <TableRow key={drug.id} hover>
                <TableCell>{drug.genericName}</TableCell>
                <TableCell>{drug.brandName || '-'}</TableCell>
                <TableCell>{drug.strength || '-'}</TableCell>
                <TableCell>{drug.form || '-'}</TableCell>
                <TableCell>{drug.route || '-'}</TableCell>
                <TableCell>{drug.packSize || '-'}</TableCell>
                <TableCell>{drug.manufacturerName}</TableCell>
                <TableCell>
                  <Box display="flex" flexWrap="wrap" gap={0.5}>
                    <Chip
                      size="small"
                      label={drug.active ? 'Active' : 'Inactive'}
                      color={drug.active ? 'success' : 'default'}
                    />
                    {drug.controlledDrugFlag && (
                      <Chip size="small" label="Controlled" color="warning" />
                    )}
                    {!drug.batchRequired && (
                      <Chip size="small" label="No Batch" variant="outlined" />
                    )}
                    {!drug.expiryRequired && (
                      <Chip size="small" label="No Expiry" variant="outlined" />
                    )}
                  </Box>
                </TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    variant="text"
                    startIcon={<EditIcon />}
                    onClick={() => handleOpenEdit(drug)}
                  >
                    Edit
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {filteredDrugs.length === 0 && (
              <TableRow>
                <TableCell colSpan={9} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No drugs found. Try adjusting your filters or add a new drug.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          rowsPerPageOptions={[25, 50, 100]}
          count={filteredDrugs.length}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </TableContainer>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingDrug ? 'Edit Drug' : 'Add New Drug'}</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <TextField
              label="Generic Name"
              value={form.genericName}
              onChange={(e) => setForm({ ...form, genericName: e.target.value })}
              required
            />
            <TextField
              label="Brand Name"
              value={form.brandName}
              onChange={(e) => setForm({ ...form, brandName: e.target.value })}
            />
            <TextField
              label="Strength"
              value={form.strength}
              onChange={(e) => setForm({ ...form, strength: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Form</InputLabel>
              <Select
                label="Form"
                value={formSelectValue}
                onChange={(e) => {
                  const value = e.target.value as string;
                  setFormSelectValue(value);
                  if (value !== '__other__') {
                    setForm({ ...form, form: value || '' });
                  } else {
                    // keep existing free-text form value, show text field below
                    setForm({ ...form, form: form.form || '' });
                  }
                }}
                size="small"
              >
                <MenuItem value="">
                  <em>Not specified</em>
                </MenuItem>
                <MenuItem value="Tablet">Tablet</MenuItem>
                <MenuItem value="Chewable Tablet">Chewable Tablet</MenuItem>
                <MenuItem value="Dispersible Tablet">Dispersible Tablet</MenuItem>
                <MenuItem value="Effervescent Tablet">Effervescent Tablet</MenuItem>
                <MenuItem value="Capsule">Capsule</MenuItem>
                <MenuItem value="Syrup">Syrup</MenuItem>
                <MenuItem value="Oral Suspension">Oral Suspension</MenuItem>
                <MenuItem value="Powder for Suspension">Powder for Suspension</MenuItem>
                <MenuItem value="Oral Solution">Oral Solution</MenuItem>
                <MenuItem value="Solution">Solution</MenuItem>
                <MenuItem value="Suspension">Suspension</MenuItem>
                <MenuItem value="Injection">Injection</MenuItem>
                <MenuItem value="IV Injection">IV Injection</MenuItem>
                <MenuItem value="IM Injection">IM Injection</MenuItem>
                <MenuItem value="SC Injection">SC Injection</MenuItem>
                <MenuItem value="IV Infusion">IV Infusion</MenuItem>
                <MenuItem value="Infusion">Infusion</MenuItem>
                <MenuItem value="Cream">Cream</MenuItem>
                <MenuItem value="Ointment">Ointment</MenuItem>
                <MenuItem value="Lotion">Lotion</MenuItem>
                <MenuItem value="Gel">Gel</MenuItem>
                <MenuItem value="Powder">Powder</MenuItem>
                <MenuItem value="Granules">Granules</MenuItem>
                <MenuItem value="Inhaler">Inhaler</MenuItem>
                <MenuItem value="Drops">Drops</MenuItem>
                <MenuItem value="Pediatric Drops">Pediatric Drops</MenuItem>
                <MenuItem value="Nasal Drop">Nasal Drop</MenuItem>
                <MenuItem value="Ear Drop">Ear Drop</MenuItem>
                <MenuItem value="Ophthalmic Solution">Ophthalmic Solution</MenuItem>
                <MenuItem value="Nasal Spray">Nasal Spray</MenuItem>
                <MenuItem value="Spray">Spray</MenuItem>
                <MenuItem value="Suppository">Suppository</MenuItem>
                <MenuItem value="Patch">Patch</MenuItem>
                <MenuItem value="Mouthwash">Mouthwash</MenuItem>
                <MenuItem value="__other__">Other (specify)</MenuItem>
              </Select>
            </FormControl>
            {(formSelectValue === '__other__' || (!formSelectValue && form.form)) && (
              <TextField
                label="Form (custom)"
                value={form.form}
                onChange={(e) => setForm({ ...form, form: e.target.value })}
              />
            )}
            <TextField
              label="Route"
              value={form.route}
              onChange={(e) => setForm({ ...form, route: e.target.value })}
            />
            <TextField
              label="Pack Size"
              value={form.packSize}
              onChange={(e) => setForm({ ...form, packSize: e.target.value })}
            />
            <TextField
              label="Unit of Measure"
              value={form.unitOfMeasure}
              onChange={(e) => setForm({ ...form, unitOfMeasure: e.target.value })}
            />
            <Autocomplete
              options={manufacturers}
              getOptionLabel={(option) => option.name}
              value={
                manufacturers.find((m) => m.id === form.manufacturerId) || null
              }
              onChange={(_, value) =>
                setForm({ ...form, manufacturerId: value ? value.id : '' })
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Manufacturer"
                  required
                />
              )}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                  color="primary"
                />
              }
              label="Active"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.controlledDrugFlag}
                  onChange={(e) =>
                    setForm({ ...form, controlledDrugFlag: e.target.checked })
                  }
                  color="warning"
                />
              }
              label="Controlled drug"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.batchRequired}
                  onChange={(e) => setForm({ ...form, batchRequired: e.target.checked })}
                  color="primary"
                />
              }
              label="Batch required"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={!!form.expiryRequired}
                  onChange={(e) => setForm({ ...form, expiryRequired: e.target.checked })}
                  color="primary"
                />
              }
              label="Expiry required"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleSave}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PharmacyCatalogPage;

