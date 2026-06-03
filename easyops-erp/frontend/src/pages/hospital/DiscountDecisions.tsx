import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import { Refresh as RefreshIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  DiscountDecisionResponse,
  PagedResponse,
} from '../../services/hospitalCorporateDiscountService';
import './Hospital.css';

const formatDate = (s: string | null | undefined) => {
  if (!s) return '–';
  try {
    const d = new Date(s);
    return d.toLocaleString();
  } catch {
    return s;
  }
};

const DiscountDecisionsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [decisions, setDecisions] = useState<DiscountDecisionResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewingDecision, setViewingDecision] = useState<DiscountDecisionResponse | null>(null);

  const loadDecisions = useCallback(async () => {
    try {
      setLoading(true);
      const response: PagedResponse<DiscountDecisionResponse> =
        await hospitalCorporateDiscountService.getDiscountDecisions({ page, size });
      setDecisions(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load discount decisions', err);
      enqueueSnackbar('Failed to load discount decisions', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, enqueueSnackbar]);

  useEffect(() => {
    loadDecisions();
  }, [loadDecisions]);

  const handleRefresh = () => {
    setPage(0);
    loadDecisions();
  };

  const handleView = (row: DiscountDecisionResponse) => {
    setViewingDecision(row);
    setViewDialogOpen(true);
  };

  const handlePageChange = (_: unknown, newPage: number) => setPage(newPage);
  const handleRowsPerPageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSize(Math.max(5, parseInt(e.target.value, 10) || 20));
    setPage(0);
  };

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4">Corporate &amp; Discount – Discount decisions</Typography>
          <Typography variant="body2">Audit view of discount decisions (decided by, approved by, dates)</Typography>
        </Box>
        <Button startIcon={<RefreshIcon />} onClick={handleRefresh} sx={{ bgcolor: 'white', color: 'primary.main' }}>
          Refresh
        </Button>
      </Box>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          {loading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Bill context</TableCell>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Scheme ID</TableCell>
                    <TableCell align="right">Discount amount</TableCell>
                    <TableCell>Discount %</TableCell>
                    <TableCell>Decided by</TableCell>
                    <TableCell>Approved by</TableCell>
                    <TableCell>Created at</TableCell>
                    <TableCell>Approved at</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {decisions.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        No discount decisions found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    decisions.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.billContextId ?? '–'}</TableCell>
                        <TableCell>{row.patientId ?? '–'}</TableCell>
                        <TableCell>{row.discountSchemeId ?? '–'}</TableCell>
                        <TableCell align="right">{row.discountAmount ?? '–'}</TableCell>
                        <TableCell>{row.discountPercent != null ? row.discountPercent : '–'}</TableCell>
                        <TableCell>{row.decidedByUserId ?? '–'}</TableCell>
                        <TableCell>{row.approvedByUserId ?? '–'}</TableCell>
                        <TableCell>{formatDate(row.createdAt)}</TableCell>
                        <TableCell>{formatDate(row.approvedAt)}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => handleView(row)}>
                            View
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

      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Discount decision details</DialogTitle>
        <DialogContent>
          {viewingDecision && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <TextField label="ID" value={viewingDecision.id} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Bill context" value={viewingDecision.billContextId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Patient ID" value={viewingDecision.patientId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Corporate client ID" value={viewingDecision.corporateClientId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Discount scheme ID" value={viewingDecision.discountSchemeId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Discount amount" value={viewingDecision.discountAmount ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Discount %" value={viewingDecision.discountPercent ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Decided by user ID" value={viewingDecision.decidedByUserId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Approved by user ID" value={viewingDecision.approvedByUserId ?? '–'} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Created at" value={formatDate(viewingDecision.createdAt)} InputProps={{ readOnly: true }} fullWidth />
              <TextField label="Approved at" value={formatDate(viewingDecision.approvedAt)} InputProps={{ readOnly: true }} fullWidth />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DiscountDecisionsPage;
