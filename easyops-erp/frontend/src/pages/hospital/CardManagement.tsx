import React, { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
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
  Drawer,
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
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  AccountBalance as BalanceIcon,
  Block as BlockIcon,
  CheckCircle as ActivateIcon,
  SwapHoriz as ReplaceIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalCardManagementService, {
  AuthorizationResponse,
  CardBalanceResponse,
  CardDetailResponse,
  CardProductResponse,
  CardResponse,
  CardTransactionResponse,
  CreateAdjustmentRequest,
  IssueCardRequest,
  LimitProfileResponse,
  PagedResponse,
  RefundRequest,
  TopupRequest,
  UpdateCardStatusRequest,
} from '../../services/hospitalCardManagementService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const OWNER_TYPES = ['PATIENT', 'STAFF', 'CORPORATE_BENEFICIARY', 'VISITOR'];
const CARD_STATUSES = ['ISSUED', 'ACTIVE', 'BLOCKED', 'LOST', 'EXPIRED', 'CLOSED'];
const ENABLE_PHASE_4_5_FEATURES = false;

const CardManagementPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const location = useLocation();
  const navigate = useNavigate();

  const [loading, setLoading] = useState<boolean>(false);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [products, setProducts] = useState<CardProductResponse[]>([]);
  const [productsLoaded, setProductsLoaded] = useState<boolean>(false);
  const [limitProfiles, setLimitProfiles] = useState<LimitProfileResponse[]>([]);
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [cardNumberFilter, setCardNumberFilter] = useState<string>('');
  const [ownerReferenceIdFilter, setOwnerReferenceIdFilter] = useState<string>('');
  const [ownerTypeFilter, setOwnerTypeFilter] = useState<string>('');
  const [corporateIdFilter, setCorporateIdFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [productFilter, setProductFilter] = useState<string>('');
  const [issuedAtFromFilter, setIssuedAtFromFilter] = useState<string>('');
  const [issuedAtToFilter, setIssuedAtToFilter] = useState<string>('');

  const [issueDialogOpen, setIssueDialogOpen] = useState<boolean>(false);
  const [issueForm, setIssueForm] = useState<IssueCardRequest>({
    cardProductId: '',
    ownerType: 'PATIENT',
    ownerReferenceId: '',
    corporateId: '',
    limitProfileId: '',
    cardNumber: '',
    physicalSerial: '',
  });

  const [detailDrawerOpen, setDetailDrawerOpen] = useState<boolean>(false);
  const [detailCard, setDetailCard] = useState<CardDetailResponse | null>(null);
  const [detailBalance, setDetailBalance] = useState<CardBalanceResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState<boolean>(false);

  const [topupAmount, setTopupAmount] = useState<string>('');
  const [topupReference, setTopupReference] = useState<string>('');
  const [topupIdempotencyKey, setTopupIdempotencyKey] = useState<string>('');
  const [topupSubmitting, setTopupSubmitting] = useState<boolean>(false);

  const [adjustmentAmount, setAdjustmentAmount] = useState<string>('');
  const [adjustmentReason, setAdjustmentReason] = useState<string>('');
  const [adjustmentSubmitting, setAdjustmentSubmitting] = useState<boolean>(false);

  const [statusUpdateOpen, setStatusUpdateOpen] = useState<boolean>(false);
  const [statusUpdateValue, setStatusUpdateValue] = useState<string>('ACTIVE');
  const [statusUpdateReason, setStatusUpdateReason] = useState<string>('');
  const [statusSubmitting, setStatusSubmitting] = useState<boolean>(false);

  const [replaceDialogOpen, setReplaceDialogOpen] = useState<boolean>(false);
  const [replaceReason, setReplaceReason] = useState<string>('');
  const [replaceSubmitting, setReplaceSubmitting] = useState<boolean>(false);

  /** When status dialog is opened from table row (no drawer), we need the card id. */
  const [selectedCardIdForStatus, setSelectedCardIdForStatus] = useState<string | null>(null);

  // Transactions (card detail)
  const [transactions, setTransactions] = useState<CardTransactionResponse[]>([]);
  const [transactionsTotal, setTransactionsTotal] = useState<number>(0);
  const [transactionsPage, setTransactionsPage] = useState<number>(0);
  const [transactionsSize] = useState<number>(10);
  const [txFilterFrom, setTxFilterFrom] = useState<string>('');
  const [txFilterTo, setTxFilterTo] = useState<string>('');
  const [txFilterType, setTxFilterType] = useState<string>('');
  const [txFilterStatus, setTxFilterStatus] = useState<string>('');
  const [transactionsLoading, setTransactionsLoading] = useState<boolean>(false);

  // Refund dialog
  const [refundDialogOpen, setRefundDialogOpen] = useState<boolean>(false);
  const [refundTx, setRefundTx] = useState<CardTransactionResponse | null>(null);
  const [refundAmount, setRefundAmount] = useState<string>('');
  const [refundReason, setRefundReason] = useState<string>('');
  const [refundIdempotencyKey, setRefundIdempotencyKey] = useState<string>('');
  const [refundSubmitting, setRefundSubmitting] = useState<boolean>(false);

  // Test authorize (pilot)
  const [testAuthCardNumber, setTestAuthCardNumber] = useState<string>('');
  const [testAuthAmount, setTestAuthAmount] = useState<string>('');
  const [testAuthUsageDomain, setTestAuthUsageDomain] = useState<string>('CANTEEN');
  const [testAuthSourceSystem, setTestAuthSourceSystem] = useState<string>('');
  const [testAuthSubmitting, setTestAuthSubmitting] = useState<boolean>(false);
  const [testAuthResult, setTestAuthResult] = useState<{
    approved: boolean;
    authorizationId?: string;
    reasonCode?: string;
    remainingBalance?: number;
  } | null>(null);

  const productMap = useCallback(() => {
    const m: Record<string, CardProductResponse> = {};
    products.forEach((p) => {
      m[p.id] = p;
    });
    return m;
  }, [products]);
  const pm = productMap();
  const detailProduct = detailCard ? pm[detailCard.cardProductId] : undefined;
  const isIdentityOnlyCard =
    detailProduct?.code === 'PATIENT_IDENTITY' ||
    detailProduct?.usageDomains?.toUpperCase().includes('IDENTITY') === true;
  const walletOperationsAllowed = !isIdentityOnlyCard;

  const loadCards = useCallback(async () => {
    try {
      setLoading(true);
      const params: {
        page: number;
        size: number;
        cardNumber?: string;
        ownerReferenceId?: string;
        ownerType?: string;
        corporateId?: string;
        status?: string;
        cardProductId?: string;
        issuedAtFrom?: string;
        issuedAtTo?: string;
      } = { page, size };
      if (cardNumberFilter.trim()) params.cardNumber = cardNumberFilter.trim();
      if (ownerReferenceIdFilter.trim()) params.ownerReferenceId = ownerReferenceIdFilter.trim();
      if (ownerTypeFilter) params.ownerType = ownerTypeFilter;
      if (corporateIdFilter.trim()) params.corporateId = corporateIdFilter.trim();
      if (statusFilter) params.status = statusFilter;
      if (productFilter.trim()) params.cardProductId = productFilter.trim();
      if (issuedAtFromFilter.trim()) params.issuedAtFrom = issuedAtFromFilter.trim().endsWith('Z') ? issuedAtFromFilter.trim() : issuedAtFromFilter.trim() + 'T00:00:00Z';
      if (issuedAtToFilter.trim()) params.issuedAtTo = issuedAtToFilter.trim().endsWith('Z') ? issuedAtToFilter.trim() : issuedAtToFilter.trim() + 'T23:59:59Z';
      const response: PagedResponse<CardResponse> =
        await hospitalCardManagementService.searchCards(params);
      setCards(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Failed to load cards', err);
      enqueueSnackbar('Failed to load cards', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [
    page,
    size,
    cardNumberFilter,
    ownerReferenceIdFilter,
    ownerTypeFilter,
    corporateIdFilter,
    statusFilter,
    productFilter,
    issuedAtFromFilter,
    issuedAtToFilter,
    enqueueSnackbar,
  ]);

  const loadProducts = useCallback(async () => {
    try {
      const res = await hospitalCardManagementService.getCardProducts({ page: 0, size: 200 });
      setProducts(res.content);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setProductsLoaded(true);
    }
  }, []);

  const loadLimitProfiles = useCallback(async () => {
    try {
      const res = await hospitalCardManagementService.getLimitProfiles({ page: 0, size: 200 });
      setLimitProfiles(res.content);
    } catch (err) {
      console.error('Failed to load limit profiles', err);
    }
  }, []);

  useEffect(() => {
    loadCards();
  }, [loadCards]);

  useEffect(() => {
    loadProducts();
    loadLimitProfiles();
  }, [loadProducts, loadLimitProfiles]);

  const handleRefresh = () => {
    setPage(0);
    loadCards();
  };

  const handleApplyFilters = () => {
    setPage(0);
    loadCards();
  };

  const handleIssueOpen = () => {
    setIssueForm({
      cardProductId: products[0]?.id ?? '',
      ownerType: 'PATIENT',
      ownerReferenceId: '',
      corporateId: '',
      limitProfileId: '',
      cardNumber: '',
      physicalSerial: '',
    });
    setIssueDialogOpen(true);
  };

  const handleIssueSubmit = async () => {
    if (!issueForm.cardProductId || !issueForm.ownerReferenceId?.trim()) {
      enqueueSnackbar('Product and owner reference ID are required', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const payload: IssueCardRequest = {
        cardProductId: issueForm.cardProductId,
        ownerType: issueForm.ownerType,
        ownerReferenceId: issueForm.ownerReferenceId.trim(),
      };
      if (issueForm.corporateId?.trim()) payload.corporateId = issueForm.corporateId.trim();
      if (issueForm.limitProfileId?.trim()) payload.limitProfileId = issueForm.limitProfileId.trim();
      if (issueForm.cardNumber?.trim()) payload.cardNumber = issueForm.cardNumber.trim();
      if (issueForm.physicalSerial?.trim()) payload.physicalSerial = issueForm.physicalSerial.trim();
      await hospitalCardManagementService.issueCard(payload);
      enqueueSnackbar('Card issued', { variant: 'success' });
      setIssueDialogOpen(false);
      loadCards();
    } catch (err: unknown) {
      console.error('Failed to issue card', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to issue card'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const loadTransactions = useCallback(
    async (cardId: string) => {
      try {
        setTransactionsLoading(true);
        const params: { page: number; size: number; from?: string; to?: string; type?: string; status?: string } = {
          page: transactionsPage,
          size: transactionsSize,
        };
        if (txFilterFrom.trim()) params.from = txFilterFrom.trim().endsWith('Z') ? txFilterFrom.trim() : txFilterFrom.trim() + 'Z';
        if (txFilterTo.trim()) params.to = txFilterTo.trim().endsWith('Z') ? txFilterTo.trim() : txFilterTo.trim() + 'Z';
        if (txFilterType) params.type = txFilterType;
        if (txFilterStatus) params.status = txFilterStatus;
        const res = await hospitalCardManagementService.getCardTransactions(cardId, params);
        setTransactions(res.content);
        setTransactionsTotal(res.totalElements);
      } catch (err) {
        console.error('Failed to load transactions', err);
        enqueueSnackbar('Failed to load transactions', { variant: 'error' });
      } finally {
        setTransactionsLoading(false);
      }
    },
    [
      transactionsPage,
      transactionsSize,
      txFilterFrom,
      txFilterTo,
      txFilterType,
      txFilterStatus,
      enqueueSnackbar,
    ]
  );

  useEffect(() => {
    if (!detailCard?.id || !detailDrawerOpen || !productsLoaded) return;
    if (walletOperationsAllowed) {
      loadTransactions(detailCard.id);
    } else {
      setTransactions([]);
      setTransactionsTotal(0);
    }
  }, [detailCard?.id, detailDrawerOpen, walletOperationsAllowed, loadTransactions, productsLoaded]);

  // Open card detail when navigated from Limit Profiles "View" (state.openCardId)
  useEffect(() => {
    const openCardId = (location.state as { openCardId?: string } | null)?.openCardId;
    if (openCardId) {
      openDetail(openCardId);
      navigate(location.pathname, { replace: true, state: {} });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openDetail = async (cardId: string) => {
    setDetailDrawerOpen(true);
    setDetailCard(null);
    setDetailBalance(null);
    setDetailLoading(true);
    setTransactionsPage(0);
    try {
      const detail = await hospitalCardManagementService.getCard(cardId);
      setDetailCard(detail);
      if (ENABLE_PHASE_4_5_FEATURES) {
        try {
          const balance = await hospitalCardManagementService.getCardBalance(cardId);
          setDetailBalance(balance);
        } catch {
          setDetailBalance(null);
        }
      }
    } catch (err) {
      console.error('Failed to load card detail', err);
      enqueueSnackbar('Failed to load card detail', { variant: 'error' });
      setDetailDrawerOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleApplyTransactionFilters = () => {
    if (!walletOperationsAllowed) return;
    setTransactionsPage(0);
    if (detailCard?.id) loadTransactions(detailCard.id);
  };

  const openRefundDialog = (tx: CardTransactionResponse) => {
    setRefundTx(tx);
    setRefundAmount(String(Math.abs(tx.amount)));
    setRefundReason('');
    setRefundIdempotencyKey('');
    setRefundDialogOpen(true);
  };

  const handleRefundSubmit = async () => {
    if (!refundTx || !refundAmount || Number(refundAmount) <= 0) {
      enqueueSnackbar('Enter a positive refund amount', { variant: 'warning' });
      return;
    }
    const maxRefund = Math.abs(refundTx.amount);
    if (Number(refundAmount) > maxRefund) {
      enqueueSnackbar(`Refund cannot exceed ${maxRefund}`, { variant: 'warning' });
      return;
    }
    try {
      setRefundSubmitting(true);
      const payload: RefundRequest = { amount: Number(refundAmount) };
      if (refundReason.trim()) payload.reason = refundReason.trim();
      if (refundIdempotencyKey.trim()) payload.idempotencyKey = refundIdempotencyKey.trim();
      await hospitalCardManagementService.refundTransaction(refundTx.id, payload);
      enqueueSnackbar('Refund successful', { variant: 'success' });
      setRefundDialogOpen(false);
      setRefundTx(null);
      setRefundAmount('');
      setRefundReason('');
      setRefundIdempotencyKey('');
      if (detailCard?.id) {
        openDetail(detailCard.id);
        loadTransactions(detailCard.id);
      }
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Refund failed'), { variant: 'error' });
    } finally {
      setRefundSubmitting(false);
    }
  };

  const handleTopupSubmit = async () => {
    if (!walletOperationsAllowed) {
      enqueueSnackbar('Top-up is not allowed for identity-only cards', { variant: 'info' });
      return;
    }
    if (!detailCard || !topupAmount || Number(topupAmount) <= 0) {
      enqueueSnackbar('Enter a positive amount', { variant: 'warning' });
      return;
    }
    try {
      setTopupSubmitting(true);
      const payload: TopupRequest = { amount: Number(topupAmount) };
      if (topupReference.trim()) payload.reference = topupReference.trim();
      if (topupIdempotencyKey.trim()) payload.idempotencyKey = topupIdempotencyKey.trim();
      await hospitalCardManagementService.topupCard(detailCard.id, payload);
      enqueueSnackbar('Top-up successful', { variant: 'success' });
      setTopupAmount('');
      setTopupReference('');
      setTopupIdempotencyKey('');
      openDetail(detailCard.id);
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Top-up failed'), { variant: 'error' });
    } finally {
      setTopupSubmitting(false);
    }
  };

  const handleAdjustmentSubmit = async () => {
    if (!walletOperationsAllowed) {
      enqueueSnackbar('Adjustment is not allowed for identity-only cards', { variant: 'info' });
      return;
    }
    if (!detailCard || adjustmentAmount === '' || !adjustmentReason.trim()) {
      enqueueSnackbar('Amount and reason are required', { variant: 'warning' });
      return;
    }
    try {
      setAdjustmentSubmitting(true);
      const payload: CreateAdjustmentRequest = {
        amount: Number(adjustmentAmount),
        reason: adjustmentReason.trim(),
      };
      await hospitalCardManagementService.createAdjustment(detailCard.id, payload);
      enqueueSnackbar('Adjustment applied', { variant: 'success' });
      setAdjustmentAmount('');
      setAdjustmentReason('');
      openDetail(detailCard.id);
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Adjustment failed'), { variant: 'error' });
    } finally {
      setAdjustmentSubmitting(false);
    }
  };

  const handleStatusUpdate = async () => {
    const cardId = detailCard?.id ?? selectedCardIdForStatus;
    if (!cardId) return;
    try {
      setStatusSubmitting(true);
      const payload: UpdateCardStatusRequest = {
        status: statusUpdateValue,
        reason: statusUpdateReason.trim() || undefined,
      };
      await hospitalCardManagementService.updateCardStatus(cardId, payload);
      enqueueSnackbar('Status updated', { variant: 'success' });
      setStatusUpdateOpen(false);
      setStatusUpdateReason('');
      setSelectedCardIdForStatus(null);
      if (detailCard) openDetail(detailCard.id);
      loadCards();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Status update failed'), { variant: 'error' });
    } finally {
      setStatusSubmitting(false);
    }
  };

  const handleTestAuthorize = async () => {
    if (!testAuthCardNumber.trim() || !testAuthAmount || Number(testAuthAmount) <= 0 || !testAuthSourceSystem.trim()) {
      enqueueSnackbar('Card number, amount, and source system are required', { variant: 'warning' });
      return;
    }
    try {
      setTestAuthSubmitting(true);
      setTestAuthResult(null);
      const res: AuthorizationResponse = await hospitalCardManagementService.authorize({
        cardNumber: testAuthCardNumber.trim(),
        amount: Number(testAuthAmount),
        usageDomain: testAuthUsageDomain as 'HOSPITAL' | 'CANTEEN',
        sourceSystem: testAuthSourceSystem.trim(),
      });
      setTestAuthResult({
        approved: res.approved,
        authorizationId: res.authorizationId,
        reasonCode: res.reasonCode,
        remainingBalance: res.remainingBalance,
      });
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Authorize failed'), { variant: 'error' });
      setTestAuthResult(null);
    } finally {
      setTestAuthSubmitting(false);
    }
  };

  const handleReplaceSubmit = async () => {
    if (!detailCard) return;
    try {
      setReplaceSubmitting(true);
      const newCard = await hospitalCardManagementService.replaceCard(detailCard.id, {
        reason: replaceReason.trim() || undefined,
      });
      enqueueSnackbar(`Replacement card issued: ${newCard.cardNumber}`, { variant: 'success' });
      setReplaceDialogOpen(false);
      setReplaceReason('');
      setDetailDrawerOpen(false);
      setDetailCard(null);
      loadCards();
    } catch (err: unknown) {
      enqueueSnackbar(ehrApiErrorMessage(err, 'Replace failed'), { variant: 'error' });
    } finally {
      setReplaceSubmitting(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Cards – Management
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleIssueOpen} disabled={loading}>
            Issue card
          </Button>
        </Box>
      </Box>

      {ENABLE_PHASE_4_5_FEATURES && (
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Card number"
              size="small"
              value={cardNumberFilter}
              onChange={(e) => setCardNumberFilter(e.target.value)}
              sx={{ minWidth: 160 }}
            />
            <TextField
              label="Owner reference ID"
              size="small"
              value={ownerReferenceIdFilter}
              onChange={(e) => setOwnerReferenceIdFilter(e.target.value)}
              sx={{ minWidth: 160 }}
            />
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Owner type</InputLabel>
              <Select
                label="Owner type"
                value={ownerTypeFilter}
                onChange={(e) => setOwnerTypeFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {OWNER_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Corporate ID"
              size="small"
              value={corporateIdFilter}
              onChange={(e) => setCorporateIdFilter(e.target.value)}
              sx={{ minWidth: 140 }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {CARD_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Product</InputLabel>
              <Select
                label="Product"
                value={productFilter}
                onChange={(e) => setProductFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {products.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.code} – {p.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Issued at from"
              size="small"
              type="date"
              value={issuedAtFromFilter}
              onChange={(e) => setIssuedAtFromFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 150 }}
            />
            <TextField
              label="Issued at to"
              size="small"
              type="date"
              value={issuedAtToFilter}
              onChange={(e) => setIssuedAtToFilter(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 150 }}
            />
            <Button variant="outlined" size="medium" onClick={handleApplyFilters}>
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Test authorize (pilot)
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Quick test for canteen / external clients: authorize by card number and show approval/decline and remaining balance.
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-start">
            <TextField
              label="Card number"
              size="small"
              value={testAuthCardNumber}
              onChange={(e) => setTestAuthCardNumber(e.target.value)}
              placeholder="Card number"
              sx={{ minWidth: 160 }}
            />
            <TextField
              label="Amount"
              type="number"
              size="small"
              inputProps={{ min: 0.01, step: 0.01 }}
              value={testAuthAmount}
              onChange={(e) => setTestAuthAmount(e.target.value)}
              sx={{ minWidth: 100 }}
            />
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Usage domain</InputLabel>
              <Select
                label="Usage domain"
                value={testAuthUsageDomain}
                onChange={(e) => setTestAuthUsageDomain(e.target.value)}
              >
                <MenuItem value="HOSPITAL">HOSPITAL</MenuItem>
                <MenuItem value="CANTEEN">CANTEEN</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Source system"
              size="small"
              value={testAuthSourceSystem}
              onChange={(e) => setTestAuthSourceSystem(e.target.value)}
              placeholder="e.g. CANTEEN_POS"
              sx={{ minWidth: 140 }}
            />
            <Button
              variant="outlined"
              onClick={handleTestAuthorize}
              disabled={testAuthSubmitting}
            >
              {testAuthSubmitting ? 'Authorizing…' : 'Authorize'}
            </Button>
          </Box>
          {testAuthResult != null && (
            <Box
              sx={{
                mt: 2,
                p: 1.5,
                bgcolor: testAuthResult.approved ? 'success.light' : 'error.light',
                color: testAuthResult.approved ? 'success.contrastText' : 'error.contrastText',
                borderRadius: 1,
              }}
            >
              <Typography variant="subtitle2">
                {testAuthResult.approved ? 'Approved' : 'Declined'}
                {testAuthResult.reasonCode != null && testAuthResult.reasonCode !== '' && ` – ${testAuthResult.reasonCode}`}
              </Typography>
              {testAuthResult.authorizationId && (
                <Typography variant="body2">Authorization ID: {testAuthResult.authorizationId}</Typography>
              )}
              {testAuthResult.remainingBalance != null && (
                <Typography variant="body2">Remaining balance: {testAuthResult.remainingBalance}</Typography>
              )}
            </Box>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {loading && cards.length === 0 ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Card number</TableCell>
                    <TableCell>Owner type</TableCell>
                    <TableCell>Owner reference ID</TableCell>
                    <TableCell>Product</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Issued at</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cards.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No cards found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    cards.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.cardNumber}</TableCell>
                        <TableCell>{row.ownerType}</TableCell>
                        <TableCell>{row.ownerReferenceId}</TableCell>
                        <TableCell>{pm[row.cardProductId]?.name ?? row.cardProductId}</TableCell>
                        <TableCell>{row.status}</TableCell>
                        <TableCell>
                          {row.issuedAt
                            ? new Date(row.issuedAt).toLocaleString(undefined, {
                                dateStyle: 'short',
                                timeStyle: 'short',
                              })
                            : '—'}
                        </TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<ViewIcon />} onClick={() => openDetail(row.id)}>
                            View
                          </Button>
                          {ENABLE_PHASE_4_5_FEATURES && (
                            <Button size="small" startIcon={<BalanceIcon />} onClick={() => openDetail(row.id)}>
                              Top-up
                            </Button>
                          )}
                          {ENABLE_PHASE_4_5_FEATURES && (
                            <Button size="small" onClick={() => openDetail(row.id)}>
                              Adjust
                            </Button>
                          )}
                          <Button
                            size="small"
                            onClick={() => {
                              setSelectedCardIdForStatus(row.id);
                              setStatusUpdateValue(row.status);
                              setStatusUpdateOpen(true);
                            }}
                          >
                            Status
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {totalElements > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Total: {totalElements}
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Issue card dialog */}
      <Dialog open={issueDialogOpen} onClose={() => setIssueDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Issue card</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <FormControl size="small" fullWidth required>
              <InputLabel>Card product</InputLabel>
              <Select
                label="Card product"
                value={issueForm.cardProductId}
                onChange={(e) => setIssueForm({ ...issueForm, cardProductId: e.target.value })}
              >
                {products.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.code} – {p.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {ENABLE_PHASE_4_5_FEATURES && (
            <FormControl size="small" fullWidth>
              <InputLabel>Owner type</InputLabel>
              <Select
                label="Owner type"
                value={issueForm.ownerType}
                onChange={(e) => setIssueForm({ ...issueForm, ownerType: e.target.value })}
              >
                {OWNER_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            )}
            <TextField
              label="Owner reference ID"
              required
              size="small"
              value={issueForm.ownerReferenceId}
              onChange={(e) => setIssueForm({ ...issueForm, ownerReferenceId: e.target.value })}
              placeholder="Patient ID, staff ID, etc."
            />
            <TextField
              label="Corporate ID (optional)"
              size="small"
              value={issueForm.corporateId}
              onChange={(e) => setIssueForm({ ...issueForm, corporateId: e.target.value })}
            />
            <FormControl size="small" fullWidth>
              <InputLabel>Limit profile (optional)</InputLabel>
              <Select
                label="Limit profile (optional)"
                value={issueForm.limitProfileId}
                onChange={(e) => setIssueForm({ ...issueForm, limitProfileId: e.target.value })}
              >
                <MenuItem value="">
                  <em>None</em>
                </MenuItem>
                {limitProfiles.map((lp) => (
                  <MenuItem key={lp.id} value={lp.id}>
                    {lp.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Card number (optional, auto-generated if empty)"
              size="small"
              value={issueForm.cardNumber}
              onChange={(e) => setIssueForm({ ...issueForm, cardNumber: e.target.value })}
            />
            <TextField
              label="Physical serial (optional)"
              size="small"
              value={issueForm.physicalSerial ?? ''}
              onChange={(e) => setIssueForm({ ...issueForm, physicalSerial: e.target.value })}
              placeholder="Hardware serial if applicable"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIssueDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleIssueSubmit} disabled={loading}>
            Issue
          </Button>
        </DialogActions>
      </Dialog>

      {/* Card detail drawer */}
      <Drawer
        anchor="right"
        open={detailDrawerOpen}
        onClose={() => setDetailDrawerOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 420 } } }}
      >
        <Box p={2} height="100%" display="flex" flexDirection="column" overflow="auto">
          <Typography variant="h6" gutterBottom>
            Card detail
          </Typography>
          {detailLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : detailCard ? (
            <>
              <Card variant="outlined" sx={{ mb: 2 }}>
                <CardContent>
                  <Typography variant="body2"><strong>Card number:</strong> {detailCard.cardNumber}</Typography>
                  <Typography variant="body2"><strong>Owner:</strong> {detailCard.ownerType} – {detailCard.ownerReferenceId}</Typography>
                  <Typography variant="body2"><strong>Status:</strong> {detailCard.status}</Typography>
                  <Typography variant="body2"><strong>Product:</strong> {pm[detailCard.cardProductId]?.name ?? detailCard.cardProductId}</Typography>
                  {detailCard.replacedByCardId && (
                    <Typography variant="body2" sx={{ mt: 0.5 }}>
                      Replaced by:{' '}
                      <Button
                        size="small"
                        variant="text"
                        sx={{ p: 0, minWidth: 0, textTransform: 'none' }}
                        onClick={() => openDetail(detailCard.replacedByCardId!)}
                      >
                        View card {detailCard.replacedByCardId.slice(0, 8)}…
                      </Button>
                    </Typography>
                  )}
                  {detailCard.closedAt && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Closed at: {new Date(detailCard.closedAt).toLocaleString()}
                    </Typography>
                  )}
                  {detailCard.blockedAt && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Blocked at: {new Date(detailCard.blockedAt).toLocaleString()}
                    </Typography>
                  )}
                  {detailCard.statusChangeReason && (detailCard.status === 'BLOCKED' || detailCard.status === 'CLOSED') && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Reason: {detailCard.statusChangeReason}
                    </Typography>
                  )}
                  {detailCard.accountSummary && (
                    <Box mt={1}>
                      <Typography variant="subtitle2">Balance</Typography>
                      <Typography variant="body2">
                        {detailCard.accountSummary.currentBalance} {detailCard.accountSummary.currency}
                        {detailCard.accountSummary.creditLimit != null && ` (credit limit: ${detailCard.accountSummary.creditLimit})`}
                      </Typography>
                    </Box>
                  )}
                  {detailBalance?.limitUsage && (
                    <Box mt={1.5}>
                      <Typography variant="subtitle2">Limit usage</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {detailBalance.limitUsage.resetPolicy === 'MONTHLY' ? 'This month' : 'Today'}
                        {detailBalance.limitUsage.periodStart && detailBalance.limitUsage.periodEnd && (
                          <> ({new Date(detailBalance.limitUsage.periodStart).toLocaleDateString()} – {new Date(detailBalance.limitUsage.periodEnd).toLocaleDateString()})</>
                        )}
                      </Typography>
                      <Box component="ul" sx={{ m: 0, pl: 2, typography: 'body2' }}>
                        {(detailBalance.limitUsage.dailyAmountLimit != null || detailBalance.limitUsage.monthlyAmountLimit != null) && (
                          <li>
                            Amount: {detailBalance.limitUsage.amountConsumed ?? 0}
                            {detailBalance.limitUsage.dailyAmountLimit != null && ` / ${detailBalance.limitUsage.dailyAmountLimit} daily`}
                            {detailBalance.limitUsage.monthlyAmountLimit != null && ` / ${detailBalance.limitUsage.monthlyAmountLimit} monthly`}
                          </li>
                        )}
                        {detailBalance.limitUsage.dailyMealLimit != null && (
                          <li>
                            Meals: {detailBalance.limitUsage.mealCountConsumed ?? 0} / {detailBalance.limitUsage.dailyMealLimit} per day
                          </li>
                        )}
                        {detailBalance.limitUsage.dailyVisitLimit != null && (
                          <li>
                            Visits: {detailBalance.limitUsage.visitCountConsumed ?? 0} / {detailBalance.limitUsage.dailyVisitLimit} per day
                          </li>
                        )}
                      </Box>
                    </Box>
                  )}
                </CardContent>
              </Card>

              {ENABLE_PHASE_4_5_FEATURES && (
              <>
              <Typography variant="subtitle2" gutterBottom>Top-up</Typography>
              <Box display="flex" flexDirection="column" gap={1} mb={2}>
                <TextField
                  label="Amount"
                  type="number"
                  size="small"
                  inputProps={{ min: 0.01, step: 0.01 }}
                  value={topupAmount}
                  onChange={(e) => setTopupAmount(e.target.value)}
                />
                <TextField
                  label="Reference (optional)"
                  size="small"
                  value={topupReference}
                  onChange={(e) => setTopupReference(e.target.value)}
                />
                <TextField
                  label="Idempotency key (optional)"
                  size="small"
                  value={topupIdempotencyKey}
                  onChange={(e) => setTopupIdempotencyKey(e.target.value)}
                  placeholder="For retry safety"
                />
                <Button
                  variant="contained"
                  size="small"
                  onClick={handleTopupSubmit}
                  disabled={topupSubmitting || !walletOperationsAllowed}
                >
                  Top-up
                </Button>
              </Box>

              <Typography variant="subtitle2" gutterBottom>Adjustment</Typography>
              <Box display="flex" flexDirection="column" gap={1} mb={2}>
                <TextField
                  label="Amount (signed)"
                  type="number"
                  size="small"
                  value={adjustmentAmount}
                  onChange={(e) => setAdjustmentAmount(e.target.value)}
                  placeholder="e.g. -10 or 5"
                />
                <TextField
                  label="Reason (required)"
                  size="small"
                  value={adjustmentReason}
                  onChange={(e) => setAdjustmentReason(e.target.value)}
                />
                <Button
                  variant="outlined"
                  size="small"
                  onClick={handleAdjustmentSubmit}
                  disabled={adjustmentSubmitting || !walletOperationsAllowed}
                >
                  Apply adjustment
                </Button>
              </Box>
              {!walletOperationsAllowed && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  This card product is identity-only. Wallet balance and transaction operations are unavailable.
                </Typography>
              )}

              <Typography variant="subtitle2" gutterBottom>Status</Typography>
              <Box display="flex" flexWrap="wrap" gap={1} mb={2}>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<ActivateIcon />}
                  onClick={() => (setSelectedCardIdForStatus(null), setStatusUpdateValue('ACTIVE'), setStatusUpdateOpen(true))}
                >
                  Activate
                </Button>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<BlockIcon />}
                  onClick={() => (setSelectedCardIdForStatus(null), setStatusUpdateValue('BLOCKED'), setStatusUpdateOpen(true))}
                >
                  Block
                </Button>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => (setSelectedCardIdForStatus(null), setStatusUpdateValue('CLOSED'), setStatusUpdateOpen(true))}
                >
                  Close
                </Button>
              </Box>

              <Typography variant="subtitle2" gutterBottom>Replace card</Typography>
              <Button
                variant="outlined"
                color="warning"
                size="small"
                startIcon={<ReplaceIcon />}
                onClick={() => setReplaceDialogOpen(true)}
              >
                Replace card
              </Button>

              <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>
                Transactions
              </Typography>
              <Box display="flex" flexWrap="wrap" gap={1} alignItems="center" mb={1}>
                <TextField
                  label="From"
                  size="small"
                  type="datetime-local"
                  value={txFilterFrom}
                  onChange={(e) => setTxFilterFrom(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  sx={{ width: 180 }}
                />
                <TextField
                  label="To"
                  size="small"
                  type="datetime-local"
                  value={txFilterTo}
                  onChange={(e) => setTxFilterTo(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  sx={{ width: 180 }}
                />
                <FormControl size="small" sx={{ minWidth: 100 }}>
                  <InputLabel>Type</InputLabel>
                  <Select
                    label="Type"
                    value={txFilterType}
                    onChange={(e) => setTxFilterType(e.target.value)}
                  >
                    <MenuItem value=""><em>Any</em></MenuItem>
                    <MenuItem value="AUTH">AUTH</MenuItem>
                    <MenuItem value="CAPTURE">CAPTURE</MenuItem>
                    <MenuItem value="TOPUP">TOPUP</MenuItem>
                    <MenuItem value="REFUND">REFUND</MenuItem>
                    <MenuItem value="ADJUSTMENT">ADJUSTMENT</MenuItem>
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 100 }}>
                  <InputLabel>Status</InputLabel>
                  <Select
                    label="Status"
                    value={txFilterStatus}
                    onChange={(e) => setTxFilterStatus(e.target.value)}
                  >
                    <MenuItem value=""><em>Any</em></MenuItem>
                    <MenuItem value="PENDING">PENDING</MenuItem>
                    <MenuItem value="COMMITTED">COMMITTED</MenuItem>
                    <MenuItem value="REVERSED">REVERSED</MenuItem>
                  </Select>
                </FormControl>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={handleApplyTransactionFilters}
                  disabled={!walletOperationsAllowed}
                >
                  Apply
                </Button>
              </Box>
              {transactionsLoading ? (
                <Box display="flex" justifyContent="center" py={2}>
                  <CircularProgress size={24} />
                </Box>
              ) : !walletOperationsAllowed ? (
                <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                  No transaction history for identity-only card products.
                </Typography>
              ) : (
                <TableContainer sx={{ maxHeight: 220, mb: 1 }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell>Type</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell>Source</TableCell>
                        <TableCell>Ref</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Created</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {transactions.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center">No transactions</TableCell>
                        </TableRow>
                      ) : (
                        transactions.map((tx) => (
                          <TableRow key={tx.id}>
                            <TableCell>{tx.transactionType}</TableCell>
                            <TableCell align="right">{tx.amount} {tx.currency}</TableCell>
                            <TableCell>{tx.sourceSystem ?? '—'}</TableCell>
                            <TableCell sx={{ maxWidth: 80, overflow: 'hidden', textOverflow: 'ellipsis' }} title={tx.externalReferenceId ?? ''}>
                              {tx.externalReferenceId ?? '—'}
                            </TableCell>
                            <TableCell>{tx.status}</TableCell>
                            <TableCell>
                              {tx.createdAt
                                ? new Date(tx.createdAt).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' })
                                : '—'}
                            </TableCell>
                            <TableCell align="right">
                              {tx.status === 'COMMITTED' && (tx.transactionType === 'CAPTURE' || tx.transactionType === 'TOPUP') && (
                                <Button size="small" variant="text" onClick={() => openRefundDialog(tx)}>
                                  Refund
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              {walletOperationsAllowed && transactionsTotal > 0 && (
                <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
                  <Typography variant="body2" color="text.secondary">
                    Total: {transactionsTotal}
                  </Typography>
                  <Button
                    size="small"
                    disabled={transactionsPage <= 0}
                    onClick={() => { setTransactionsPage((p) => Math.max(0, p - 1)); }}
                  >
                    Previous
                  </Button>
                  <Typography variant="body2">
                    Page {transactionsPage + 1}
                  </Typography>
                  <Button
                    size="small"
                    disabled={(transactionsPage + 1) * transactionsSize >= transactionsTotal}
                    onClick={() => setTransactionsPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </Box>
              )}
              </>
              )}

              <Box flex={1} />
              <Button variant="text" onClick={() => setDetailDrawerOpen(false)} sx={{ mt: 2 }}>
                Close
              </Button>
            </>
          ) : null}
        </Box>
      </Drawer>

      {/* Status update dialog (when opened from table row without opening drawer first) */}
      <Dialog open={statusUpdateOpen} onClose={() => setStatusUpdateOpen(false)}>
        <DialogTitle>Update card status</DialogTitle>
        <DialogContent>
          <Box pt={1}>
            <FormControl size="small" fullWidth sx={{ mt: 1 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusUpdateValue}
                onChange={(e) => setStatusUpdateValue(e.target.value)}
              >
                <MenuItem value="ACTIVE">ACTIVE</MenuItem>
                <MenuItem value="BLOCKED">BLOCKED</MenuItem>
                <MenuItem value="CLOSED">CLOSED</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Reason (optional)"
              size="small"
              fullWidth
              value={statusUpdateReason}
              onChange={(e) => setStatusUpdateReason(e.target.value)}
              sx={{ mt: 2 }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusUpdateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleStatusUpdate}
            disabled={statusSubmitting}
          >
            Update
          </Button>
        </DialogActions>
      </Dialog>

      {/* Replace card dialog */}
      <Dialog open={replaceDialogOpen} onClose={() => setReplaceDialogOpen(false)}>
        <DialogTitle>Replace card</DialogTitle>
        <DialogContent>
          <TextField
            label="Reason (optional)"
            size="small"
            fullWidth
            value={replaceReason}
            onChange={(e) => setReplaceReason(e.target.value)}
            placeholder="LOST, DAMAGED, OTHER"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReplaceDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={handleReplaceSubmit} disabled={replaceSubmitting}>
            Replace
          </Button>
        </DialogActions>
      </Dialog>

      {/* Refund dialog */}
      {ENABLE_PHASE_4_5_FEATURES && (
      <Dialog open={refundDialogOpen} onClose={() => setRefundDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Refund transaction</DialogTitle>
        <DialogContent>
          {refundTx && (
            <Box pt={1}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Transaction: {refundTx.transactionType} – {refundTx.amount} {refundTx.currency} (max refund: {Math.abs(refundTx.amount)})
              </Typography>
              <TextField
                label="Amount"
                type="number"
                size="small"
                fullWidth
                required
                inputProps={{ min: 0.01, max: Math.abs(refundTx.amount), step: 0.01 }}
                value={refundAmount}
                onChange={(e) => setRefundAmount(e.target.value)}
                sx={{ mt: 1 }}
              />
              <TextField
                label="Reason (optional)"
                size="small"
                fullWidth
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                sx={{ mt: 2 }}
              />
              <TextField
                label="Idempotency key (optional)"
                size="small"
                fullWidth
                value={refundIdempotencyKey}
                onChange={(e) => setRefundIdempotencyKey(e.target.value)}
                placeholder="For retry safety"
                sx={{ mt: 2 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRefundDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleRefundSubmit} disabled={refundSubmitting}>
            Refund
          </Button>
        </DialogActions>
      </Dialog>
      )}
    </Box>
  );
};

export default CardManagementPage;
