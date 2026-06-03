import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Link,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import hospitalClinicalOrdersService, {
  ClinicalOrderDetailResponse,
  ResultLinkResponse,
  WorklistItemResponse,
} from '../../services/hospitalClinicalOrdersService';
import './Hospital.css';

const ClinicalOrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<ClinicalOrderDetailResponse | null>(null);

  const loadOrder = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await hospitalClinicalOrdersService.getOrder(id);
      setOrder(data);
    } catch (err) {
      console.error('Load order failed', err);
      enqueueSnackbar('Order not found or failed to load', { variant: 'error' });
      setOrder(null);
    } finally {
      setLoading(false);
    }
  }, [id, enqueueSnackbar]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  if (loading) {
    return (
      <Box className="hospital-page" display="flex" justifyContent="center" alignItems="center" minHeight={200}>
        <CircularProgress />
      </Box>
    );
  }

  if (!order) {
    return (
      <Box className="hospital-page" sx={{ p: 2 }}>
        <Button size="small" onClick={() => navigate('/hospital/clinical-orders/orders')}>
          Back to orders
        </Button>
        <Typography color="text.secondary" sx={{ mt: 2 }}>Order not found.</Typography>
      </Box>
    );
  }

  const worklistItems: WorklistItemResponse[] = order.worklistItems ?? [];
  const resultLinks: ResultLinkResponse[] = order.resultLinks ?? [];

  return (
    <Box className="hospital-page">
      <Box className="page-header" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5">Order detail</Typography>
          <Typography variant="body2" color="text.secondary">{order.id}</Typography>
        </Box>
        <Box display="flex" gap={1}>
          <Button size="small" variant="outlined" onClick={() => navigate('/hospital/clinical-orders/orders')}>
            Back to orders
          </Button>
          <Button
            size="small"
            variant="outlined"
            onClick={() => navigate(`/hospital/clinical-orders/sets/${order.orderSetId}`)}
          >
            View order set
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>Order</Typography>
          <Box display="flex" flexWrap="wrap" gap={2} sx={{ mt: 1 }}>
            <Typography variant="body2"><strong>Order set:</strong> {order.orderSetId}</Typography>
            <Typography variant="body2"><strong>Type:</strong> {order.orderType}</Typography>
            <Typography variant="body2"><strong>Item code:</strong> {order.itemCode}</Typography>
            <Typography variant="body2">
              <strong>Status:</strong>{' '}
              <Chip size="small" label={order.status} variant="outlined" />
            </Typography>
            {order.resultStatus && (
              <Typography variant="body2">
                <strong>Result status:</strong>{' '}
                <Chip size="small" label={order.resultStatus} color={order.resultStatus === 'FINAL' ? 'success' : 'default'} variant="outlined" />
              </Typography>
            )}
            {order.priority && <Typography variant="body2"><strong>Priority:</strong> {order.priority}</Typography>}
            {order.resultAvailableAt && (
              <Typography variant="body2"><strong>Result at:</strong> {new Date(order.resultAvailableAt).toLocaleString()}</Typography>
            )}
            {order.performedAt && (
              <Typography variant="body2"><strong>Performed at:</strong> {new Date(order.performedAt).toLocaleString()}</Typography>
            )}
            {order.orderingNotes && (
              <Typography variant="body2" sx={{ width: '100%' }}><strong>Notes:</strong> {order.orderingNotes}</Typography>
            )}
            {order.cancelReason && (
              <Typography variant="body2" color="error"><strong>Cancel reason:</strong> {order.cancelReason}</Typography>
            )}
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>Worklist items</Typography>
          {worklistItems.length === 0 ? (
            <Typography color="text.secondary">No worklist items.</Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Assigned to (user / role)</TableCell>
                    <TableCell>Scheduled time</TableCell>
                    <TableCell>Remarks</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {worklistItems.map((w) => (
                    <TableRow key={w.id}>
                      <TableCell>{w.id}</TableCell>
                      <TableCell>{w.worklistType}</TableCell>
                      <TableCell>{w.status}</TableCell>
                      <TableCell>{w.assignedToUserId || w.assignedToRole || '—'}</TableCell>
                      <TableCell>{w.scheduledTime ? new Date(w.scheduledTime).toLocaleString() : '—'}</TableCell>
                      <TableCell>{w.remarks || '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>Result links</Typography>
          {resultLinks.length === 0 ? (
            <Typography color="text.secondary">No result links.</Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>System type</TableCell>
                    <TableCell>External ID</TableCell>
                    <TableCell>Viewer URL</TableCell>
                    <TableCell>Version</TableCell>
                    <TableCell>Revised at</TableCell>
                    <TableCell>Created at</TableCell>
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
                            Open viewer
                          </Link>
                        ) : (
                          '—'
                        )}
                      </TableCell>
                      <TableCell>{link.version ?? '—'}</TableCell>
                      <TableCell>{link.revisedAt ? new Date(link.revisedAt).toLocaleString() : '—'}</TableCell>
                      <TableCell>{link.createdAt ? new Date(link.createdAt).toLocaleString() : '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default ClinicalOrderDetailPage;
