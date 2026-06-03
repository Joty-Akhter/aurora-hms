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
} from '@mui/material';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalCorporateDiscountService, {
  CorporateResponse,
  CorporateUtilizationResponse,
  DiscountSummaryResponse,
  DiscountSchemeResponse,
} from '../../services/hospitalCorporateDiscountService';
import './Hospital.css';

const CorporateDiscountReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [corporates, setCorporates] = useState<CorporateResponse[]>([]);
  const [schemes, setSchemes] = useState<DiscountSchemeResponse[]>([]);

  const [utilFrom, setUtilFrom] = useState('');
  const [utilTo, setUtilTo] = useState('');
  const [utilCorporateId, setUtilCorporateId] = useState<string>('');
  const [utilLoading, setUtilLoading] = useState(false);
  const [utilData, setUtilData] = useState<CorporateUtilizationResponse | null>(null);

  const [summaryFrom, setSummaryFrom] = useState('');
  const [summaryTo, setSummaryTo] = useState('');
  const [summarySchemeId, setSummarySchemeId] = useState<string>('');
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryData, setSummaryData] = useState<DiscountSummaryResponse | null>(null);

  const loadCorporates = useCallback(async () => {
    try {
      const res = await hospitalCorporateDiscountService.getCorporates({ page: 0, size: 500 });
      setCorporates(res.content);
    } catch (err) {
      console.error('Failed to load corporates', err);
      enqueueSnackbar('Failed to load corporates', { variant: 'error' });
    }
  }, [enqueueSnackbar]);

  const loadSchemes = useCallback(async () => {
    try {
      const res = await hospitalCorporateDiscountService.getDiscountSchemes({ page: 0, size: 500 });
      setSchemes(res.content);
    } catch (err) {
      console.error('Failed to load discount schemes', err);
      enqueueSnackbar('Failed to load discount schemes', { variant: 'error' });
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadCorporates();
    loadSchemes();
  }, [loadCorporates, loadSchemes]);

  const corporateNameById = (id: string) => corporates.find((c) => c.id === id)?.name ?? id;

  const loadUtilization = async () => {
    if (!utilFrom || !utilTo) {
      enqueueSnackbar('Select From and To dates for corporate utilization', { variant: 'warning' });
      return;
    }
    try {
      setUtilLoading(true);
      const data = await hospitalCorporateDiscountService.getCorporateUtilization({
        from: utilFrom,
        to: utilTo,
        corporateId: utilCorporateId || undefined,
      });
      setUtilData(data);
    } catch (err) {
      console.error('Failed to load corporate utilization', err);
      enqueueSnackbar('Failed to load corporate utilization', { variant: 'error' });
    } finally {
      setUtilLoading(false);
    }
  };

  const loadDiscountSummary = async () => {
    if (!summaryFrom || !summaryTo) {
      enqueueSnackbar('Select From and To dates for discount summary', { variant: 'warning' });
      return;
    }
    try {
      setSummaryLoading(true);
      const data = await hospitalCorporateDiscountService.getDiscountSummary({
        from: summaryFrom,
        to: summaryTo,
        schemeId: summarySchemeId || undefined,
      });
      setSummaryData(data);
    } catch (err) {
      console.error('Failed to load discount summary', err);
      enqueueSnackbar('Failed to load discount summary', { variant: 'error' });
    } finally {
      setSummaryLoading(false);
    }
  };

  const utilizationRows = utilData
    ? utilData.single
      ? [utilData.single]
      : (utilData.byCorporate ?? [])
    : [];
  const summaryRows = summaryData
    ? summaryData.single
      ? [summaryData.single]
      : (summaryData.byScheme ?? [])
    : [];

  return (
    <Box className="hospital-page">
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" className="hospital-page-title">
          Corporate & Discount – Reports
        </Typography>
      </Box>

      {/* Corporate utilization */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Corporate utilization
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Count of discount decisions per corporate in the selected date range.
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center" mb={2}>
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Corporate (optional)</InputLabel>
              <Select
                value={utilCorporateId}
                label="Corporate (optional)"
                onChange={(e) => setUtilCorporateId(e.target.value)}
              >
                <MenuItem value="">All corporates</MenuItem>
                {corporates.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.code} – {c.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="From"
              type="date"
              size="small"
              value={utilFrom}
              onChange={(e) => setUtilFrom(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To"
              type="date"
              size="small"
              value={utilTo}
              onChange={(e) => setUtilTo(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              startIcon={utilLoading ? <CircularProgress size={18} /> : <RefreshIcon />}
              onClick={loadUtilization}
              disabled={utilLoading}
            >
              Run
            </Button>
          </Box>
          {utilData && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Period: {utilData.from} to {utilData.to}
            </Typography>
          )}
          {utilLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : utilizationRows.length > 0 ? (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Corporate</TableCell>
                    <TableCell align="right">Decision count</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {utilizationRows.map((row) => (
                    <TableRow key={row.corporateId}>
                      <TableCell>{corporateNameById(row.corporateId)}</TableCell>
                      <TableCell align="right">{row.decisionCount}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : utilData ? (
            <Typography color="text.secondary">No data for the selected period.</Typography>
          ) : null}
        </CardContent>
      </Card>

      {/* Discount summary */}
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Discount summary
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Total discount amount and decision count by scheme in the selected date range.
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center" mb={2}>
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Scheme (optional)</InputLabel>
              <Select
                value={summarySchemeId}
                label="Scheme (optional)"
                onChange={(e) => setSummarySchemeId(e.target.value)}
              >
                <MenuItem value="">All schemes</MenuItem>
                {schemes.map((s) => (
                  <MenuItem key={s.id} value={s.id}>
                    {s.code} – {s.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="From"
              type="date"
              size="small"
              value={summaryFrom}
              onChange={(e) => setSummaryFrom(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="To"
              type="date"
              size="small"
              value={summaryTo}
              onChange={(e) => setSummaryTo(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              startIcon={summaryLoading ? <CircularProgress size={18} /> : <RefreshIcon />}
              onClick={loadDiscountSummary}
              disabled={summaryLoading}
            >
              Run
            </Button>
          </Box>
          {summaryData && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Period: {summaryData.from} to {summaryData.to}
            </Typography>
          )}
          {summaryLoading ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : summaryRows.length > 0 ? (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Scheme code</TableCell>
                    <TableCell align="right">Total discount amount</TableCell>
                    <TableCell align="right">Decision count</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {summaryRows.map((row) => (
                    <TableRow key={row.schemeId}>
                      <TableCell>{row.schemeCode}</TableCell>
                      <TableCell align="right">
                        {typeof row.totalAmount === 'number'
                          ? row.totalAmount.toLocaleString(undefined, {
                              minimumFractionDigits: 2,
                              maximumFractionDigits: 2,
                            })
                          : row.totalAmount}
                      </TableCell>
                      <TableCell align="right">{row.decisionCount}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : summaryData ? (
            <Typography color="text.secondary">No data for the selected period.</Typography>
          ) : null}
        </CardContent>
      </Card>
    </Box>
  );
};

export default CorporateDiscountReportsPage;
