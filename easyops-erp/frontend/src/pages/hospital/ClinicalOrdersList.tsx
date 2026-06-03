import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
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
  TablePagination,
  TextField,
  Typography,
} from '@mui/material';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalClinicalOrdersService, {
  ClinicalOrderResponse,
  PagedResponse,
} from '../../services/hospitalClinicalOrdersService';
import './Hospital.css';

const ORDER_TYPES = ['LAB', 'RADIOLOGY', 'PROCEDURE'];
const ORDER_STATUSES = [
  'REQUESTED',
  'VERIFIED',
  'SCHEDULED',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'REJECTED',
];

const ClinicalOrdersListPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [orders, setOrders] = useState<ClinicalOrderResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [facilityIdFilter, setFacilityIdFilter] = useState('');
  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [visitIdFilter, setVisitIdFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; facilityId?: string; patientId?: string; visitId?: string; type?: string; status?: string } = {
        page,
        size,
      };
      if (facilityIdFilter.trim()) params.facilityId = facilityIdFilter.trim();
      if (patientIdFilter.trim()) params.patientId = patientIdFilter.trim();
      if (visitIdFilter.trim()) params.visitId = visitIdFilter.trim();
      if (typeFilter) params.type = typeFilter;
      if (statusFilter) params.status = statusFilter;
      const res: PagedResponse<ClinicalOrderResponse> = await hospitalClinicalOrdersService.getOrders(params);
      setOrders(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Load orders failed', err);
      enqueueSnackbar('Failed to load orders', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, facilityIdFilter, patientIdFilter, visitIdFilter, typeFilter, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const handleRefresh = () => {
    setPage(0);
    loadOrders();
  };

  const handlePageChange = (_: unknown, newPage: number) => setPage(newPage);
  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  const handleViewOrderSet = (orderSetId: string) => {
    navigate(`/hospital/clinical-orders/sets/${orderSetId}`);
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Clinical Orders – Orders</Typography>
          <Typography variant="body2" color="text.secondary">
            List clinical orders by patient, visit, type, and status. View opens the order set detail.
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={loading}>
          Refresh
        </Button>
      </Box>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>Filters</Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              size="small"
              label="Facility ID"
              value={facilityIdFilter}
              onChange={(e) => setFacilityIdFilter(e.target.value)}
              placeholder="UUID"
              sx={{ width: 220 }}
            />
            <TextField
              size="small"
              label="Patient ID"
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
              placeholder="UUID"
              sx={{ width: 220 }}
            />
            <TextField
              size="small"
              label="Visit ID"
              value={visitIdFilter}
              onChange={(e) => setVisitIdFilter(e.target.value)}
              placeholder="UUID"
              sx={{ width: 220 }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Type</InputLabel>
              <Select value={typeFilter} label="Type" onChange={(e) => setTypeFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {ORDER_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {ORDER_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="outlined" onClick={handleRefresh}>Search</Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}><CircularProgress /></Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Order ID</TableCell>
                    <TableCell>Order set ID</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Item code</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Result status</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {orders.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={8} align="center">No orders found.</TableCell>
                    </TableRow>
                  ) : (
                    orders.map((row) => (
                      <TableRow key={row.id} hover>
                        <TableCell>{row.id}</TableCell>
                        <TableCell>{row.orderSetId}</TableCell>
                        <TableCell>{row.orderType}</TableCell>
                        <TableCell>{row.itemCode}</TableCell>
                        <TableCell>{row.status}</TableCell>
                        <TableCell>
                          {row.resultStatus ? (
                            <Chip size="small" label={row.resultStatus} color={row.resultStatus === 'FINAL' ? 'success' : 'default'} variant="outlined" />
                          ) : '—'}
                        </TableCell>
                        <TableCell>{row.createdAt ? new Date(row.createdAt).toLocaleString() : '—'}</TableCell>
                        <TableCell align="right">
                          <Button size="small" onClick={() => navigate(`/hospital/clinical-orders/orders/${row.id}`)} sx={{ mr: 1 }}>
                            View order
                          </Button>
                          <Button size="small" variant="outlined" onClick={() => handleViewOrderSet(row.orderSetId)}>
                            View order set
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={handlePageChange}
            rowsPerPage={size}
            onRowsPerPageChange={handleRowsPerPageChange}
            rowsPerPageOptions={[10, 20, 50]}
            labelRowsPerPage="Rows per page:"
          />
        </CardContent>
      </Card>
    </Box>
  );
};

export default ClinicalOrdersListPage;
