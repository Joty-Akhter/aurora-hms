import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Button,
} from '@mui/material';
import { Receipt as StatementIcon } from '@mui/icons-material';
import { useAuth } from '@contexts/AuthContext';
import { useSnackbar } from 'notistack';
import hospitalCardManagementService, {
  CardResponse,
} from '../../services/hospitalCardManagementService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

/**
 * Portal "My cards" page: lists cards for the current user (owner).
 * Uses GET /me/cards with X-Owner-Reference-Id (and X-Owner-Type) from logged-in user.
 */
const MyCardsPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState<boolean>(true);
  const [cards, setCards] = useState<CardResponse[]>([]);

  const loadCards = useCallback(async () => {
    const ownerReferenceId = user?.id != null ? String(user.id) : null;
    if (!ownerReferenceId) {
      setLoading(false);
      setCards([]);
      return;
    }
    setLoading(true);
    try {
      const list = await hospitalCardManagementService.getMyCards(
        ownerReferenceId,
        'STAFF'
      );
      setCards(list ?? []);
    } catch (e: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(e, 'Failed to load cards'), { variant: 'error' });
      setCards([]);
    } finally {
      setLoading(false);
    }
  }, [user?.id, enqueueSnackbar]);

  useEffect(() => {
    if (!user?.id) {
      setLoading(false);
      setCards([]);
      return;
    }
    loadCards();
  }, [user?.id, loadCards]);

  if (!user?.id) {
    return (
      <Box p={2}>
        <Typography color="text.secondary">Sign in to see your cards.</Typography>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={2}>
      <Typography variant="h5" gutterBottom>
        My cards
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Cards linked to your account. Open a statement to see transaction history.
      </Typography>
      {cards.length === 0 ? (
        <Card variant="outlined">
          <CardContent>
            <Typography color="text.secondary">No cards found for your account.</Typography>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Card} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Card number</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {cards.map((card) => (
                <TableRow key={card.id}>
                  <TableCell>{card.cardNumber}</TableCell>
                  <TableCell>{card.status}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      startIcon={<StatementIcon />}
                      onClick={() => navigate(`/portal/cards/${card.id}/statement`)}
                    >
                      Statement
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default MyCardsPage;
