import React, { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
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
  TextField,
  Typography,
  Tabs,
  Tab,
} from '@mui/material';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalCardManagementService, {
  CardProductResponse,
  LiabilityReportItem,
  UsageByDomainItem,
  CorporateExposureItem,
  CorporateExposureResponse,
} from '../../services/hospitalCardManagementService';
import './Hospital.css';

type TabValue = 'liabilities' | 'usage' | 'corporate';

const CardReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [tab, setTab] = useState<TabValue>('liabilities');
  const [loading, setLoading] = useState(false);
  const [products, setProducts] = useState<CardProductResponse[]>([]);

  // Liabilities
  const [asOfDate, setAsOfDate] = useState('');
  const [cardProductIdFilter, setCardProductIdFilter] = useState('');
  const [ownerTypeFilter, setOwnerTypeFilter] = useState('');
  const [liabilities, setLiabilities] = useState<LiabilityReportItem[]>([]);

  // Usage by domain
  const [usageFrom, setUsageFrom] = useState('');
  const [usageTo, setUsageTo] = useState('');
  const [sourceSystemFilter, setSourceSystemFilter] = useState('');
  const [usageRows, setUsageRows] = useState<UsageByDomainItem[]>([]);

  // Corporate exposure
  const [corporateId, setCorporateId] = useState('');
  const [corporateAsOf, setCorporateAsOf] = useState('');
  const [corporateData, setCorporateData] = useState<CorporateExposureResponse | null>(null);

  const loadProducts = useCallback(async () => {
    try {
      const res = await hospitalCardManagementService.getCardProducts({ page: 0, size: 200 });
      setProducts(res.content ?? []);
    } catch (err) {
      console.error('Failed to load products', err);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const loadLiabilities = async () => {
    try {
      setLoading(true);
      const params: { asOf?: string; cardProductId?: string; ownerType?: string } = {};
      if (asOfDate) params.asOf = `${asOfDate}T23:59:59Z`;
      if (cardProductIdFilter.trim()) params.cardProductId = cardProductIdFilter.trim();
      if (ownerTypeFilter.trim()) params.ownerType = ownerTypeFilter.trim();
      const data = await hospitalCardManagementService.getLiabilities(params);
      setLiabilities(data ?? []);
    } catch (err) {
      console.error('Failed to load liabilities', err);
      enqueueSnackbar('Failed to load liabilities report', { variant: 'error' });
      setLiabilities([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsageByDomain = async () => {
    try {
      setLoading(true);
      const params: { from?: string; to?: string; sourceSystem?: string } = {};
      if (usageFrom) params.from = `${usageFrom}T00:00:00Z`;
      if (usageTo) params.to = `${usageTo}T23:59:59Z`;
      if (sourceSystemFilter.trim()) params.sourceSystem = sourceSystemFilter.trim();
      const data = await hospitalCardManagementService.getUsageByDomain(params);
      setUsageRows(data ?? []);
    } catch (err) {
      console.error('Failed to load usage by domain', err);
      enqueueSnackbar('Failed to load usage by domain report', { variant: 'error' });
      setUsageRows([]);
    } finally {
      setLoading(false);
    }
  };

  const loadCorporateExposure = async () => {
    if (!corporateId.trim()) {
      enqueueSnackbar('Enter Corporate ID', { variant: 'warning' });
      return;
    }
    try {
      setLoading(true);
      const params: { corporateId: string; asOf?: string } = { corporateId: corporateId.trim() };
      if (corporateAsOf) params.asOf = `${corporateAsOf}T23:59:59Z`;
      const data = await hospitalCardManagementService.getCorporateExposure(params);
      setCorporateData(data);
    } catch (err) {
      console.error('Failed to load corporate exposure', err);
      enqueueSnackbar('Failed to load corporate exposure report', { variant: 'error' });
      setCorporateData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (_: React.SyntheticEvent, value: TabValue) => {
    setTab(value);
  };

  const handleApply = () => {
    if (tab === 'liabilities') loadLiabilities();
    else if (tab === 'usage') loadUsageByDomain();
    else loadCorporateExposure();
  };

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5" className="hospital-page-title">
          Cards – Reports
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={handleApply}
          disabled={loading}
        >
          {loading ? 'Loading…' : 'Apply'}
        </Button>
      </Box>

      <Card sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={handleTabChange}>
          <Tab label="Liabilities" value="liabilities" />
          <Tab label="Usage by domain" value="usage" />
          <Tab label="Corporate exposure" value="corporate" />
        </Tabs>
        <CardContent>
          {tab === 'liabilities' && (
            <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
              <TextField
                label="As of date"
                type="date"
                size="small"
                value={asOfDate}
                onChange={(e) => setAsOfDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
                helperText={asOfDate ? 'Point-in-time balance' : 'Empty = current balance'}
              />
              <FormControl size="small" sx={{ minWidth: 220 }}>
                <InputLabel>Product</InputLabel>
                <Select
                  label="Product"
                  value={cardProductIdFilter}
                  onChange={(e) => setCardProductIdFilter(e.target.value)}
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
                label="Owner type"
                size="small"
                value={ownerTypeFilter}
                onChange={(e) => setOwnerTypeFilter(e.target.value)}
                placeholder="e.g. PATIENT, STAFF (optional)"
                sx={{ minWidth: 160 }}
              />
            </Box>
          )}
          {tab === 'usage' && (
            <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
              <TextField
                label="From"
                type="date"
                size="small"
                value={usageFrom}
                onChange={(e) => setUsageFrom(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="To"
                type="date"
                size="small"
                value={usageTo}
                onChange={(e) => setUsageTo(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Source system"
                size="small"
                value={sourceSystemFilter}
                onChange={(e) => setSourceSystemFilter(e.target.value)}
                placeholder="e.g. CANTEEN, HOSPITAL_BILLING (optional)"
              />
            </Box>
          )}
          {tab === 'corporate' && (
            <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
              <TextField
                label="Corporate ID"
                size="small"
                value={corporateId}
                onChange={(e) => setCorporateId(e.target.value)}
                placeholder="UUID (required)"
                required
              />
              <TextField
                label="As of date"
                type="date"
                size="small"
                value={corporateAsOf}
                onChange={(e) => setCorporateAsOf(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          )}
        </CardContent>
      </Card>

      {loading && (
        <Box display="flex" justifyContent="center" py={4}>
          <CircularProgress />
        </Box>
      )}

      {!loading && tab === 'liabilities' && (
        <TableContainer component={Card} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Card ID</TableCell>
                <TableCell>Card number</TableCell>
                <TableCell>Owner type</TableCell>
                <TableCell>Owner reference</TableCell>
                <TableCell align="right">Balance</TableCell>
                <TableCell>Currency</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {liabilities.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No prepaid liabilities. Apply filters and click Apply.
                  </TableCell>
                </TableRow>
              ) : (
                liabilities.map((row) => (
                  <TableRow key={row.cardId}>
                    <TableCell>{row.cardId}</TableCell>
                    <TableCell>{row.cardNumber}</TableCell>
                    <TableCell>{row.ownerType}</TableCell>
                    <TableCell>{row.ownerReferenceId}</TableCell>
                    <TableCell align="right">{row.currentBalance}</TableCell>
                    <TableCell>{row.currency}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {!loading && tab === 'usage' && (
        <TableContainer component={Card} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Source system</TableCell>
                <TableCell align="right">Total amount</TableCell>
                <TableCell align="right">Transaction count</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {usageRows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center">
                    No usage data. Set date range and click Apply.
                  </TableCell>
                </TableRow>
              ) : (
                usageRows.map((row) => (
                  <TableRow key={row.sourceSystem}>
                    <TableCell>{row.sourceSystem}</TableCell>
                    <TableCell align="right">{row.totalAmount}</TableCell>
                    <TableCell align="right">{row.transactionCount}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {!loading && tab === 'corporate' && (
        <>
          {corporateData && (
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              Total balance: {corporateData.totalBalance}
            </Typography>
          )}
          <TableContainer component={Card} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Card ID</TableCell>
                  <TableCell>Card number</TableCell>
                  <TableCell>Owner type</TableCell>
                  <TableCell>Owner reference</TableCell>
                  <TableCell align="right">Balance</TableCell>
                  <TableCell align="right">Credit limit</TableCell>
                  <TableCell>Currency</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {!corporateData || corporateData.items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      Enter Corporate ID and click Apply.
                    </TableCell>
                  </TableRow>
                ) : (
                  corporateData.items.map((row: CorporateExposureItem) => (
                    <TableRow key={row.cardId}>
                      <TableCell>{row.cardId}</TableCell>
                      <TableCell>{row.cardNumber}</TableCell>
                      <TableCell>{row.ownerType}</TableCell>
                      <TableCell>{row.ownerReferenceId}</TableCell>
                      <TableCell align="right">{row.currentBalance}</TableCell>
                      <TableCell align="right">{row.creditLimit ?? '–'}</TableCell>
                      <TableCell>{row.currency}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Box>
  );
};

export default CardReportsPage;
