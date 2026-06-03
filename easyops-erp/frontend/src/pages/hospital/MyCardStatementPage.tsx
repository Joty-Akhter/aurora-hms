import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { useAuth } from '@contexts/AuthContext';
import { useSnackbar } from 'notistack';
import hospitalCardManagementService, {
  CardTransactionResponse,
  PagedResponse,
} from '../../services/hospitalCardManagementService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PAGE_SIZE = 20;

/**
 * Portal card statement: paged transaction list for one card.
 * GET /me/cards/:id/statement with from, to, page, size.
 */
const MyCardStatementPage: React.FC = () => {
  const { id: cardId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState<boolean>(true);
  const [data, setData] = useState<PagedResponse<CardTransactionResponse> | null>(null);
  const [from, setFrom] = useState<string>('');
  const [to, setTo] = useState<string>('');
  const [page, setPage] = useState<number>(0);

  const ownerReferenceId = user?.id != null ? String(user.id) : null;

  const loadStatement = useCallback(async () => {
    if (!cardId || !ownerReferenceId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const res = await hospitalCardManagementService.getMyCardStatement(
        cardId,
        ownerReferenceId,
        'STAFF',
        {
          from: from ? `${from}T00:00:00Z` : undefined,
          to: to ? `${to}T23:59:59Z` : undefined,
          page,
          size: PAGE_SIZE,
        }
      );
      setData(res);
    } catch (e: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(e, 'Failed to load statement'), { variant: 'error' });
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [cardId, ownerReferenceId, from, to, page, enqueueSnackbar]);

  useEffect(() => {
    loadStatement();
  }, [loadStatement]);

  if (!cardId || !ownerReferenceId) {
    return (
      <Box p={2}>
        <Typography color="text.secondary">Sign in and select a card to view statement.</Typography>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/portal/cards')} sx={{ mt: 1 }}>
          Back to My cards
        </Button>
      </Box>
    );
  }

  const totalPages = data?.totalPages ?? 0;
  const content = data?.content ?? [];

  return (
    <Box p={2}>
      <Button startIcon={<BackIcon />} onClick={() => navigate('/portal/cards')} sx={{ mb: 2 }}>
        Back to My cards
      </Button>
      <Typography variant="h5" gutterBottom>
        Statement – Card {cardId.slice(0, 8)}…
      </Typography>
      <Box display="flex" flexWrap="wrap" gap={2} alignItems="center" sx={{ mb: 2 }}>
        <TextField
          label="From"
          size="small"
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          InputLabelProps={{ shrink: true }}
        />
        <TextField
          label="To"
          size="small"
          type="date"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          InputLabelProps={{ shrink: true }}
        />
        <Button variant="outlined" size="small" onClick={() => setPage(0)}>
          Apply
        </Button>
      </Box>
      {loading ? (
        <Box display="flex" justifyContent="center" py={4}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <TableContainer component={Card} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Amount</TableCell>
                  <TableCell>Reference</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {content.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      No transactions in this range.
                    </TableCell>
                  </TableRow>
                ) : (
                  content.map((tx) => (
                    <TableRow key={tx.id}>
                      <TableCell>
                        {tx.postedAt
                          ? new Date(tx.postedAt).toLocaleString()
                          : tx.createdAt
                            ? new Date(tx.createdAt).toLocaleString()
                            : '–'}
                      </TableCell>
                      <TableCell>{tx.transactionType ?? '–'}</TableCell>
                      <TableCell>{tx.status ?? '–'}</TableCell>
                      <TableCell align="right">
                        {tx.amount != null ? `${tx.amount} ${tx.currency ?? ''}` : '–'}
                      </TableCell>
                      <TableCell>{tx.externalReferenceId ?? tx.sourceSystem ?? '–'}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          {totalPages > 1 && (
            <Box display="flex" alignItems="center" gap={1} mt={2}>
              <Button
                size="small"
                disabled={page <= 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <Typography variant="body2">
                Page {page + 1} of {totalPages} (total {data?.totalElements ?? 0})
              </Typography>
              <Button
                size="small"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </Box>
          )}
        </>
      )}
    </Box>
  );
};

export default MyCardStatementPage;
