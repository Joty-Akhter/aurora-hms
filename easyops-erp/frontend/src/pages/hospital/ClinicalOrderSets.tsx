import React, { useCallback, useEffect, useState } from 'react';
import {
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
  Link,
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
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import { useNavigate, useParams } from 'react-router-dom';
import hospitalClinicalOrdersService, {
  ClinicalOrderResponse,
  CreateResultLinkRequest,
  OrderSetDetailResponse,
  OrderSetResponse,
  PagedResponse,
  ResultLinkResponse,
} from '../../services/hospitalClinicalOrdersService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const RESULT_SYSTEM_TYPES = ['LIS', 'RIS', 'PACS', 'INTERNAL'];

const ClinicalOrderSetsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const { id: detailId } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState<OrderSetResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [facilityIdFilter, setFacilityIdFilter] = useState('');
  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [visitIdFilter, setVisitIdFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');
  const [detail, setDetail] = useState<OrderSetDetailResponse | null>(null);
  const [cancelOrderId, setCancelOrderId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resultLinksOrderId, setResultLinksOrderId] = useState<string | null>(null);
  const [resultLinks, setResultLinks] = useState<ResultLinkResponse[]>([]);
  const [loadingResultLinks, setLoadingResultLinks] = useState(false);
  const [addResultForm, setAddResultForm] = useState<CreateResultLinkRequest>({ systemType: 'LIS', resultStatus: '' });
  const [addResultSubmitting, setAddResultSubmitting] = useState(false);

  const loadList = async () => {
    try {
      setLoading(true);
      const params: { page: number; size: number; facilityId?: string; patientId?: string; visitId?: string; from?: string; to?: string } = { page, size };
      if (facilityIdFilter.trim()) params.facilityId = facilityIdFilter.trim();
      if (patientIdFilter.trim()) params.patientId = patientIdFilter.trim();
      if (visitIdFilter.trim()) params.visitId = visitIdFilter.trim();
      // Send full ISO date-time so backend (ISO.DATE_TIME) parses unambiguously: from = start of day UTC, to = end of day UTC
      if (fromDateFilter) params.from = `${fromDateFilter}T00:00:00.000Z`;
      if (toDateFilter) params.to = `${toDateFilter}T23:59:59.999Z`;
      const res: PagedResponse<OrderSetResponse> = await hospitalClinicalOrdersService.getOrderSets(params);
      setList(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Load order sets failed', err);
      enqueueSnackbar('Failed to load order sets', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadList();
  }, [page]);

  useEffect(() => {
    if (detailId) {
      hospitalClinicalOrdersService.getOrderSet(detailId).then(setDetail).catch(() => {
        enqueueSnackbar('Failed to load order set', { variant: 'error' });
      });
    } else {
      setDetail(null);
    }
  }, [detailId]);

  const handleCancelOrder = async () => {
    if (!cancelOrderId || !cancelReason.trim()) return;
    try {
      setSubmitting(true);
      await hospitalClinicalOrdersService.cancelOrder(cancelOrderId, { reason: cancelReason.trim() });
      enqueueSnackbar('Order cancelled', { variant: 'success' });
      setCancelOrderId(null);
      setCancelReason('');
      if (detailId) {
        const updated = await hospitalClinicalOrdersService.getOrderSet(detailId);
        setDetail(updated);
      }
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Cancel failed'), { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const loadResultLinks = useCallback(async (orderId: string) => {
    try {
      setLoadingResultLinks(true);
      const links = await hospitalClinicalOrdersService.getResultLinks(orderId);
      setResultLinks(links);
    } catch (err) {
      console.error('Load result links failed', err);
      enqueueSnackbar('Failed to load result links', { variant: 'error' });
      setResultLinks([]);
    } finally {
      setLoadingResultLinks(false);
    }
  }, [enqueueSnackbar]);

  const handleOpenResultLinks = (orderId: string) => {
    setResultLinksOrderId(orderId);
    setResultLinks([]);
    setAddResultForm({ systemType: 'LIS', resultStatus: '' });
    loadResultLinks(orderId);
  };

  const handleCloseResultLinksDialog = () => {
    setResultLinksOrderId(null);
    if (detailId) {
      hospitalClinicalOrdersService.getOrderSet(detailId).then(setDetail).catch(() => {});
    }
  };

  const handleAddResult = async () => {
    if (!resultLinksOrderId || !addResultForm.systemType?.trim()) {
      enqueueSnackbar('System type is required', { variant: 'warning' });
      return;
    }
    try {
      setAddResultSubmitting(true);
      await hospitalClinicalOrdersService.createResultLink(resultLinksOrderId, {
        systemType: addResultForm.systemType.trim(),
        externalSystemId: addResultForm.externalSystemId?.trim() || undefined,
        viewerUrl: addResultForm.viewerUrl?.trim() || undefined,
        version: addResultForm.version,
        resultStatus: addResultForm.resultStatus?.trim() || undefined,
      });
      enqueueSnackbar('Result link added', { variant: 'success' });
      setAddResultForm({ systemType: 'LIS', resultStatus: '' });
      await loadResultLinks(resultLinksOrderId);
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Add result failed'), { variant: 'error' });
    } finally {
      setAddResultSubmitting(false);
    }
  };

  const handleRepeatOrderSet = async () => {
    if (!detail) return;
    try {
      setSubmitting(true);
      const created = await hospitalClinicalOrdersService.copyOrderSet({
        sourceOrderSetId: detail.id,
        orderContext: detail.orderContext,
        priority: detail.priority,
      });
      enqueueSnackbar('Order set repeated', { variant: 'success' });
      navigate(`/hospital/clinical-orders/sets/${created.id}`);
    } catch (err: any) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Repeat order set failed'), { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Clinical Orders – Order Sets</Typography>
          <Typography variant="body2" color="text.secondary">
            List order sets by patient, visit, and date range. Click a row to view detail and cancel orders.
          </Typography>
        </Box>
        <Box display="flex" gap={1}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => { setPage(0); loadList(); }}>
            Refresh
          </Button>
          <Button variant="contained" onClick={() => navigate('/hospital/clinical-orders/entry')}>
            New order set
          </Button>
        </Box>
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
              sx={{ width: 200 }}
            />
            <TextField
              size="small"
              label="Visit ID"
              value={visitIdFilter}
              onChange={(e) => setVisitIdFilter(e.target.value)}
              sx={{ width: 200 }}
            />
            <TextField
              size="small"
              type="date"
              label="From date"
              value={fromDateFilter}
              onChange={(e) => setFromDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 160 }}
            />
            <TextField
              size="small"
              type="date"
              label="To date"
              value={toDateFilter}
              onChange={(e) => setToDateFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 160 }}
            />
            <Button variant="outlined" onClick={() => { setPage(0); loadList(); }}>Search</Button>
          </Box>
        </CardContent>
      </Card>
      <Card>
        <CardContent>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Patient ID</TableCell>
                  <TableCell>Visit ID</TableCell>
                  <TableCell>Context</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={7}>Loading…</TableCell></TableRow>
                ) : list.length === 0 ? (
                  <TableRow><TableCell colSpan={7} align="center">No order sets found.</TableCell></TableRow>
                ) : (
                  list.map((os) => (
                    <TableRow
                      key={os.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => navigate(`/hospital/clinical-orders/sets/${os.id}`)}
                    >
                      <TableCell>{os.id}</TableCell>
                      <TableCell>{os.patientId}</TableCell>
                      <TableCell>{os.visitId || '—'}</TableCell>
                      <TableCell>{os.orderContext || '—'}</TableCell>
                      <TableCell>{os.priority || '—'}</TableCell>
                      <TableCell>{os.createdAt ? new Date(os.createdAt).toLocaleString() : '—'}</TableCell>
                      <TableCell onClick={(e) => e.stopPropagation()}>
                        <Button size="small" onClick={() => navigate(`/hospital/clinical-orders/sets/${os.id}`)}>
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          {totalElements > size && (
            <Box sx={{ mt: 2 }}>
              Page {page + 1} (total {totalElements})
              <Button size="small" disabled={page === 0} onClick={() => setPage((p) => p - 1)} sx={{ ml: 1 }}>Previous</Button>
              <Button size="small" disabled={(page + 1) * size >= totalElements} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </Box>
          )}
        </CardContent>
      </Card>

      {detail && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2}>
              <Box>
                <Typography variant="h6">Order set: {detail.id}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Patient: {detail.patientId} | Visit: {detail.visitId || '—'} | Context: {detail.orderContext} | Priority: {detail.priority}
                </Typography>
              </Box>
              <Box display="flex" gap={1}>
                <Button size="small" variant="outlined" onClick={() => navigate('/hospital/clinical-orders/sets')}>
                  Back to list
                </Button>
                <Button
                  size="small"
                  variant="contained"
                  disabled={submitting}
                  onClick={handleRepeatOrderSet}
                >
                  Repeat order set
                </Button>
              </Box>
            </Box>
            <Typography variant="subtitle2" sx={{ mt: 2 }}>Orders</Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell>Item code</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Result status</TableCell>
                    <TableCell>Result at</TableCell>
                    <TableCell>Performed at</TableCell>
                    <TableCell>Cancel reason</TableCell>
                    <TableCell>Result links</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(detail.orders || []).map((o: ClinicalOrderResponse) => (
                    <TableRow key={o.id}>
                      <TableCell>{o.orderType}</TableCell>
                      <TableCell>{o.itemCode}</TableCell>
                      <TableCell>{o.status}</TableCell>
                      <TableCell>{o.resultStatus || '—'}</TableCell>
                      <TableCell>{o.resultAvailableAt ? new Date(o.resultAvailableAt).toLocaleString() : '—'}</TableCell>
                      <TableCell>{o.performedAt ? new Date(o.performedAt).toLocaleString() : '—'}</TableCell>
                      <TableCell>{o.cancelReason || '—'}</TableCell>
                      <TableCell>
                        <Button size="small" variant="outlined" onClick={() => handleOpenResultLinks(o.id)}>
                          View links
                        </Button>
                      </TableCell>
                      <TableCell>
                        {o.status !== 'CANCELLED' && o.status !== 'COMPLETED' && (
                          <Button size="small" color="error" onClick={() => setCancelOrderId(o.id)}>
                            Cancel
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      <Dialog open={!!cancelOrderId} onClose={() => !submitting && setCancelOrderId(null)}>
        <DialogTitle>Cancel order</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Reason"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelOrderId(null)} disabled={submitting}>Close</Button>
          <Button onClick={handleCancelOrder} color="error" disabled={submitting || !cancelReason.trim()}>
            {submitting ? 'Cancelling…' : 'Cancel order'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!resultLinksOrderId} onClose={handleCloseResultLinksDialog} maxWidth="md" fullWidth>
        <DialogTitle>Result links</DialogTitle>
        <DialogContent dividers>
          {loadingResultLinks ? (
            <Typography color="text.secondary">Loading…</Typography>
          ) : (
            <>
              <Typography variant="subtitle2" gutterBottom>Links (viewer URL, system type, external ID, revised at)</Typography>
              {resultLinks.length === 0 ? (
                <Typography color="text.secondary">No result links for this order.</Typography>
              ) : (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>System type</TableCell>
                        <TableCell>External ID</TableCell>
                        <TableCell>Viewer URL</TableCell>
                        <TableCell>Revised at</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {resultLinks.map((link) => (
                        <TableRow key={link.id}>
                          <TableCell>{link.systemType}</TableCell>
                          <TableCell>{link.externalSystemId || '—'}</TableCell>
                          <TableCell>
                            {link.viewerUrl ? (
                              <Link href={link.viewerUrl} target="_blank" rel="noopener noreferrer">
                                Open
                              </Link>
                            ) : '—'}
                          </TableCell>
                          <TableCell>{link.revisedAt ? new Date(link.revisedAt).toLocaleString() : '—'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              <Typography variant="subtitle2" sx={{ mt: 3 }}>Add result (testing – typically called by LIS/RIS/PACS)</Typography>
              <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 1 }}>
                <FormControl size="small" fullWidth>
                  <InputLabel>System type</InputLabel>
                  <Select
                    value={addResultForm.systemType}
                    label="System type"
                    onChange={(e) => setAddResultForm((f) => ({ ...f, systemType: e.target.value }))}
                  >
                    {RESULT_SYSTEM_TYPES.map((t) => (
                      <MenuItem key={t} value={t}>{t}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  size="small"
                  label="External system ID"
                  value={addResultForm.externalSystemId ?? ''}
                  onChange={(e) => setAddResultForm((f) => ({ ...f, externalSystemId: e.target.value }))}
                  fullWidth
                />
                <TextField
                  size="small"
                  label="Viewer URL"
                  value={addResultForm.viewerUrl ?? ''}
                  onChange={(e) => setAddResultForm((f) => ({ ...f, viewerUrl: e.target.value }))}
                  fullWidth
                />
                <FormControl size="small" fullWidth>
                  <InputLabel>Result status (FINAL updates order)</InputLabel>
                  <Select
                    value={addResultForm.resultStatus ?? ''}
                    label="Result status (FINAL updates order)"
                    onChange={(e) => setAddResultForm((f) => ({ ...f, resultStatus: e.target.value }))}
                  >
                    <MenuItem value="">—</MenuItem>
                    <MenuItem value="PENDING">PENDING</MenuItem>
                    <MenuItem value="PARTIAL">PARTIAL</MenuItem>
                    <MenuItem value="FINAL">FINAL</MenuItem>
                  </Select>
                </FormControl>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseResultLinksDialog}>Close</Button>
          <Button variant="contained" onClick={handleAddResult} disabled={addResultSubmitting || !addResultForm.systemType?.trim()}>
            {addResultSubmitting ? 'Adding…' : 'Add result'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ClinicalOrderSetsPage;
