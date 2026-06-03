import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import { useNavigate } from 'react-router-dom';
import hospitalClinicalOrdersService, {
  CreateOrderLineRequest,
  CreateOrderSetRequest,
} from '../../services/hospitalClinicalOrdersService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ORDER_TYPES = ['LAB', 'RADIOLOGY', 'PROCEDURE'];
const ORDER_CONTEXTS = ['OPD', 'IPD', 'ED'];
const PRIORITIES = ['ROUTINE', 'STAT', 'URGENT'];

const ClinicalOrderEntryPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [patientId, setPatientId] = useState('');
  const [visitId, setVisitId] = useState('');
  const [orderingDoctorId, setOrderingDoctorId] = useState('');
  const [orderingDepartmentId, setOrderingDepartmentId] = useState('');
  const [orderContext, setOrderContext] = useState('OPD');
  const [priority, setPriority] = useState('ROUTINE');
  const [lines, setLines] = useState<CreateOrderLineRequest[]>([
    { orderType: 'LAB', itemCode: '', orderingNotes: '', priority: 'ROUTINE' },
  ]);

  const addLine = () => {
    setLines((prev) => [...prev, { orderType: 'LAB', itemCode: '', orderingNotes: '', priority: 'ROUTINE' }]);
  };

  const removeLine = (index: number) => {
    if (lines.length <= 1) return;
    setLines((prev) => prev.filter((_, i) => i !== index));
  };

  const updateLine = (index: number, field: keyof CreateOrderLineRequest, value: string) => {
    setLines((prev) => {
      const next = [...prev];
      (next[index] as Record<string, string>)[field] = value;
      return next;
    });
  };

  const handleSubmit = async () => {
    if (!patientId.trim()) {
      enqueueSnackbar('Patient ID is required', { variant: 'warning' });
      return;
    }
    const validLines = lines.filter((l) => l.itemCode?.trim());
    if (validLines.length === 0) {
      enqueueSnackbar('Add at least one order line with item code', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const payload: CreateOrderSetRequest = {
        patientId: patientId.trim(),
        visitId: visitId.trim() || undefined,
        orderingDoctorId: orderingDoctorId.trim() || undefined,
        orderingDepartmentId: orderingDepartmentId.trim() || undefined,
        orderContext,
        priority,
        orders: validLines,
      };
      const created = await hospitalClinicalOrdersService.createOrderSet(payload);
      const viewPath = `/hospital/clinical-orders/sets/${created.id}`;
      enqueueSnackbar(`Order set created: ${created.id}`, {
        variant: 'success',
        action: () => (
          <Button color="inherit" size="small" onClick={() => navigate(viewPath)}>
            View
          </Button>
        ),
      });
      navigate(viewPath);
    } catch (err: any) {
      console.error('Create order set failed', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to create order set'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Clinical Orders – Entry</Typography>
          <Typography variant="body2" color="text.secondary">
            Create a new order set: patient, visit (optional), ordering doctor/department, context, priority, and order lines (LAB/RADIOLOGY/PROCEDURE).
          </Typography>
        </Box>
      </Box>
      <Card>
        <CardContent>
          <Box display="flex" flexDirection="column" gap={2}>
            <Box display="flex" flexWrap="wrap" gap={2}>
              <TextField
                label="Patient ID"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                required
                size="small"
                sx={{ minWidth: 220 }}
              />
              <TextField
                label="Visit ID"
                value={visitId}
                onChange={(e) => setVisitId(e.target.value)}
                size="small"
                sx={{ minWidth: 220 }}
              />
              <TextField
                label="Ordering Doctor ID"
                value={orderingDoctorId}
                onChange={(e) => setOrderingDoctorId(e.target.value)}
                size="small"
                sx={{ minWidth: 220 }}
              />
              <TextField
                label="Ordering Department ID"
                value={orderingDepartmentId}
                onChange={(e) => setOrderingDepartmentId(e.target.value)}
                size="small"
                sx={{ minWidth: 220 }}
              />
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Context</InputLabel>
                <Select value={orderContext} label="Context" onChange={(e) => setOrderContext(e.target.value)}>
                  {ORDER_CONTEXTS.map((c) => (
                    <MenuItem key={c} value={c}>{c}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Priority</InputLabel>
                <Select value={priority} label="Priority" onChange={(e) => setPriority(e.target.value)}>
                  {PRIORITIES.map((p) => (
                    <MenuItem key={p} value={p}>{p}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
            <Typography variant="subtitle2" sx={{ mt: 2 }}>Order lines</Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell>Item code</TableCell>
                    <TableCell>Notes</TableCell>
                    <TableCell>Priority</TableCell>
                    <TableCell width={60}></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lines.map((line, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <Select
                          size="small"
                          value={line.orderType}
                          onChange={(e) => updateLine(index, 'orderType', e.target.value)}
                          sx={{ minWidth: 130 }}
                        >
                          {ORDER_TYPES.map((t) => (
                            <MenuItem key={t} value={t}>{t}</MenuItem>
                          ))}
                        </Select>
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          placeholder="Item code"
                          value={line.itemCode}
                          onChange={(e) => updateLine(index, 'itemCode', e.target.value)}
                          fullWidth
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          placeholder="Notes"
                          value={line.orderingNotes || ''}
                          onChange={(e) => updateLine(index, 'orderingNotes', e.target.value)}
                          fullWidth
                        />
                      </TableCell>
                      <TableCell>
                        <Select
                          size="small"
                          value={line.priority || 'ROUTINE'}
                          onChange={(e) => updateLine(index, 'priority', e.target.value)}
                          sx={{ minWidth: 110 }}
                        >
                          {PRIORITIES.map((p) => (
                            <MenuItem key={p} value={p}>{p}</MenuItem>
                          ))}
                        </Select>
                      </TableCell>
                      <TableCell>
                        <Button size="small" onClick={() => removeLine(index)} disabled={lines.length <= 1}>
                          <DeleteIcon fontSize="small" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <Button startIcon={<AddIcon />} onClick={addLine} variant="outlined" size="small" sx={{ alignSelf: 'flex-start' }}>
              Add line
            </Button>
            <Button variant="contained" onClick={handleSubmit} disabled={loading} sx={{ mt: 2 }}>
              {loading ? 'Creating…' : 'Create order set'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ClinicalOrderEntryPage;
