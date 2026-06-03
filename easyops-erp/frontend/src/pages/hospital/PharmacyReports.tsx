import React, { useEffect, useMemo, useState } from 'react';
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
import { Download as DownloadIcon, PlayArrow as RunIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalPharmacyService, {
  ConsumptionReportItemResponse,
  ControlledSubstanceRegisterRow,
  DispenseOrder,
  DispenseLine,
  PharmacyLocation,
  PharmacyStockItem,
  SalesSummaryResponse,
  StockOverrideLineReportResponse,
} from '../../services/hospitalPharmacyService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

type ReportType =
  | 'NEAR_EXPIRY'
  | 'CONSUMPTION'
  | 'SALES_SUMMARY'
  | 'CONTROLLED_SUBSTANCE'
  | 'STOCK_OVERRIDES'
  | 'DISPENSE_ORDERS';

const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  NEAR_EXPIRY: 'Near-Expiry Stock',
  CONSUMPTION: 'Consumption',
  SALES_SUMMARY: 'Sales Summary',
  CONTROLLED_SUBSTANCE: 'Controlled Substance Register',
  STOCK_OVERRIDES: 'Stock Override Audit',
  DISPENSE_ORDERS: 'Dispense Orders',
};

const REQUIRES_DATE_RANGE: ReportType[] = [
  'CONSUMPTION',
  'SALES_SUMMARY',
  'CONTROLLED_SUBSTANCE',
  'STOCK_OVERRIDES',
  'DISPENSE_ORDERS',
];

const EXPORTABLE: ReportType[] = ['CONSUMPTION'];
const DISPENSED_LINE_STATUSES: DispenseLine['status'][] = [
  'DISPENSED',
  'PARTIALLY_DISPENSED',
  'FILLED_WITH_STOCK_OVERRIDE',
];

const PharmacyReportsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [pharmacies, setPharmacies] = useState<PharmacyLocation[]>([]);
  const [selectedPharmacyId, setSelectedPharmacyId] = useState<string>('');

  // Filters
  const [reportType, setReportType] = useState<ReportType>('NEAR_EXPIRY');
  const [exportType, setExportType] = useState<string>('VIEW');
  const [productCode, setProductCode] = useState('');
  const [companyCode, setCompanyCode] = useState('');
  const [nearExpiryDays, setNearExpiryDays] = useState<number>(30);
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // Results
  const [nearExpiryItems, setNearExpiryItems] = useState<PharmacyStockItem[]>([]);
  const [consumptionItems, setConsumptionItems] = useState<ConsumptionReportItemResponse[]>([]);
  const [salesSummary, setSalesSummary] = useState<SalesSummaryResponse | null>(null);
  const [controlledRows, setControlledRows] = useState<ControlledSubstanceRegisterRow[]>([]);
  const [overrideRows, setOverrideRows] = useState<StockOverrideLineReportResponse[]>([]);
  const [dispenseOrders, setDispenseOrders] = useState<DispenseOrder[]>([]);
  const [dispenseStatusFilter, setDispenseStatusFilter] = useState<'ALL' | 'COMPLETED' | 'DISPENSED'>('ALL');

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (!EXPORTABLE.includes(reportType) && exportType === 'CSV') {
      setExportType('VIEW');
    }
  }, [reportType, exportType]);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const pharms = await hospitalPharmacyService.getPharmacies({ activeOnly: true });
      setPharmacies(pharms);
      if (pharms.length > 0) {
        setSelectedPharmacyId(pharms[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load pharmacy report data:', err);
      enqueueSnackbar('Failed to load pharmacy report data', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const currentPharmacy = useMemo(
    () => pharmacies.find((p) => p.id === selectedPharmacyId),
    [pharmacies, selectedPharmacyId]
  );

  const needsDateRange = REQUIRES_DATE_RANGE.includes(reportType);

  const reportFilters = {
    productCode: productCode.trim() || undefined,
    companyCode: companyCode.trim() || undefined,
  };

  const isTimestampInRange = (timestamp: string | undefined, from: string, to: string) => {
    if (!timestamp || !from || !to) {
      return false;
    }
    const date = new Date(timestamp);
    const start = new Date(`${from}T00:00:00`);
    const end = new Date(`${to}T23:59:59`);
    return date >= start && date <= end;
  };

  const orderHasDispensedLineInRange = (order: DispenseOrder, from: string, to: string) =>
    order.lines.some((line) =>
      DISPENSED_LINE_STATUSES.includes(line.status) && isTimestampInRange(line.createdAt, from, to)
    );

  const orderMatchesDateRange = (order: DispenseOrder, from: string, to: string) =>
    isTimestampInRange(order.completedAt, from, to)
    || orderHasDispensedLineInRange(order, from, to)
    || isTimestampInRange(order.createdAt, from, to);

  const handleRunReport = async () => {
    if (needsDateRange && (!fromDate || !toDate)) {
      enqueueSnackbar('From and To dates are required for this report', { variant: 'warning' });
      return;
    }
    if (EXPORTABLE.includes(reportType) && exportType === 'CSV') {
      handleExportCsv();
      return;
    }
    setRunning(true);
    try {
      switch (reportType) {
        case 'NEAR_EXPIRY': {
          const items = await hospitalPharmacyService.getNearExpiryStock(
            selectedPharmacyId || undefined,
            nearExpiryDays,
            reportFilters
          );
          setNearExpiryItems(items);
          break;
        }
        case 'CONSUMPTION': {
          if (!selectedPharmacyId) { enqueueSnackbar('Select a pharmacy', { variant: 'warning' }); return; }
          const items = await hospitalPharmacyService.getConsumptionReport(
            selectedPharmacyId,
            fromDate,
            toDate,
            reportFilters
          );
          setConsumptionItems(items);
          break;
        }
        case 'SALES_SUMMARY': {
          if (!selectedPharmacyId) { enqueueSnackbar('Select a pharmacy', { variant: 'warning' }); return; }
          const data = await hospitalPharmacyService.getSalesSummary(
            selectedPharmacyId,
            fromDate,
            toDate,
            reportFilters
          );
          setSalesSummary(data);
          break;
        }
        case 'CONTROLLED_SUBSTANCE': {
          if (!selectedPharmacyId) { enqueueSnackbar('Select a pharmacy', { variant: 'warning' }); return; }
          const rows = await hospitalPharmacyService.getControlledSubstanceRegister(
            selectedPharmacyId,
            fromDate,
            toDate,
            reportFilters
          );
          setControlledRows(rows);
          break;
        }
        case 'STOCK_OVERRIDES': {
          if (!selectedPharmacyId) { enqueueSnackbar('Select a pharmacy', { variant: 'warning' }); return; }
          const rows = await hospitalPharmacyService.getStockOverrideReport(
            selectedPharmacyId,
            fromDate,
            toDate,
            reportFilters
          );
          setOverrideRows(rows);
          break;
        }
        case 'DISPENSE_ORDERS': {
          if (!selectedPharmacyId) { enqueueSnackbar('Select a pharmacy', { variant: 'warning' }); return; }
          const orders = await hospitalPharmacyService.searchDispenseOrders({
            pharmacyLocationId: selectedPharmacyId,
          });
          const filteredByDate = orders.filter((order) => {
            if (!fromDate || !toDate) return true;
            return orderMatchesDateRange(order, fromDate, toDate);
          });
          const filteredByStatus = filteredByDate.filter((order) => {
            const hasDispensedLines = order.lines.some((line) => DISPENSED_LINE_STATUSES.includes(line.status));
            if (dispenseStatusFilter === 'COMPLETED') return order.status === 'COMPLETED';
            if (dispenseStatusFilter === 'DISPENSED') return hasDispensedLines;
            return order.status !== 'CANCELLED';
          });
          setDispenseOrders(filteredByStatus);
          break;
        }
      }
    } catch (err: any) {
      console.error('Failed to run report:', err);
      enqueueSnackbar(ehrApiErrorMessage(err, 'Failed to run report'), { variant: 'error' });
    } finally {
      setRunning(false);
    }
  };

  const handleExportCsv = async () => {
    if (!selectedPharmacyId || !fromDate || !toDate) {
      enqueueSnackbar('Pharmacy and date range are required for export', { variant: 'warning' });
      return;
    }
    try {
      setRunning(true);
      const blob = await hospitalPharmacyService.downloadConsumptionExport(
        selectedPharmacyId,
        fromDate,
        toDate,
        reportFilters
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `consumption-report-${fromDate}-to-${toDate}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Export failed:', err);
      enqueueSnackbar('Export failed', { variant: 'error' });
    } finally {
      setRunning(false);
    }
  };

  if (loading && !currentPharmacy) {
    return (
      <Box className="hospital-page" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box className="hospital-page">
      <Box className="page-header">
        <Box>
          <Typography variant="h4" component="h1">
            Pharmacy Reports
          </Typography>
          <Typography variant="body2">
            Near-expiry, consumption, sales, controlled substances, and stock override reports.
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="secondary"
          startIcon={<RefreshIcon />}
          onClick={loadInitialData}
        >
          Refresh
        </Button>
      </Box>

      {/* Filter bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Report Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="flex-end">
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Report Type</InputLabel>
              <Select
                label="Report Type"
                value={reportType}
                onChange={(e) => setReportType(e.target.value as ReportType)}
              >
                {(Object.keys(REPORT_TYPE_LABELS) as ReportType[]).map((rt) => (
                  <MenuItem key={rt} value={rt}>{REPORT_TYPE_LABELS[rt]}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Pharmacy</InputLabel>
              <Select
                label="Pharmacy"
                value={selectedPharmacyId}
                onChange={(e) => setSelectedPharmacyId(e.target.value as string)}
              >
                <MenuItem value="">All Pharmacies</MenuItem>
                {pharmacies.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.name} ({p.type})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {reportType === 'NEAR_EXPIRY' && (
              <TextField
                label="Days Until Expiry"
                type="number"
                size="small"
                sx={{ width: 160 }}
                value={nearExpiryDays}
                onChange={(e) => setNearExpiryDays(Number(e.target.value) || 0)}
              />
            )}

            {needsDateRange && (
              <>
                <TextField
                  label="From Date"
                  type="date"
                  size="small"
                  InputLabelProps={{ shrink: true }}
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                />
                <TextField
                  label="To Date"
                  type="date"
                  size="small"
                  InputLabelProps={{ shrink: true }}
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                />
              </>
            )}
            {reportType === 'DISPENSE_ORDERS' && (
              <FormControl size="small" sx={{ minWidth: 170 }}>
                <InputLabel>Dispense Status</InputLabel>
                <Select
                  label="Dispense Status"
                  value={dispenseStatusFilter}
                  onChange={(e) => setDispenseStatusFilter(e.target.value as 'ALL' | 'COMPLETED' | 'DISPENSED')}
                >
                  <MenuItem value="ALL">All Orders</MenuItem>
                  <MenuItem value="COMPLETED">Completed Orders</MenuItem>
                  <MenuItem value="DISPENSED">Dispensed Orders</MenuItem>
                </Select>
              </FormControl>
            )}

            <TextField
              label="Product Code"
              size="small"
              placeholder="Filter by drug name"
              value={productCode}
              onChange={(e) => setProductCode(e.target.value)}
              sx={{ minWidth: 200 }}
            />

            <TextField
              label="Company Code"
              size="small"
              placeholder="Filter by brand name"
              value={companyCode}
              onChange={(e) => setCompanyCode(e.target.value)}
              sx={{ minWidth: 180 }}
            />

            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Export Type</InputLabel>
              <Select
                label="Export Type"
                value={exportType}
                onChange={(e) => setExportType(e.target.value as string)}
              >
                <MenuItem value="VIEW">View</MenuItem>
                {EXPORTABLE.includes(reportType) && <MenuItem value="CSV">CSV Download</MenuItem>}
              </Select>
            </FormControl>

            <Button
              variant="contained"
              startIcon={exportType === 'CSV' ? <DownloadIcon /> : <RunIcon />}
              onClick={handleRunReport}
              disabled={running}
            >
              {exportType === 'CSV' ? 'Export CSV' : 'Run Report'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Results */}
      <Card>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            {REPORT_TYPE_LABELS[reportType]}
          </Typography>

          {running && (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          )}

          {!running && reportType === 'NEAR_EXPIRY' && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Drug (Generic)</TableCell>
                    <TableCell>Brand</TableCell>
                    <TableCell>Batch</TableCell>
                    <TableCell>Expiry Date</TableCell>
                    <TableCell align="right">Qty On Hand</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {nearExpiryItems.map((item) => (
                    <TableRow key={item.stockId} hover>
                      <TableCell>{item.genericName}</TableCell>
                      <TableCell>{item.brandName || '-'}</TableCell>
                      <TableCell>{item.batchNumber || '-'}</TableCell>
                      <TableCell>{item.expiryDate || '-'}</TableCell>
                      <TableCell align="right">{item.quantityOnHand}</TableCell>
                    </TableRow>
                  ))}
                  {nearExpiryItems.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No near-expiry items found. Click Run Report to load.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {!running && reportType === 'CONSUMPTION' && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Drug (Generic)</TableCell>
                    <TableCell>Brand</TableCell>
                    <TableCell>Form</TableCell>
                    <TableCell>Strength</TableCell>
                    <TableCell align="right">Total Qty Issued</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {consumptionItems.map((item) => (
                    <TableRow key={String(item.drugId)} hover>
                      <TableCell>{item.genericName}</TableCell>
                      <TableCell>{item.brandName || '-'}</TableCell>
                      <TableCell>{item.form || '-'}</TableCell>
                      <TableCell>{item.strength || '-'}</TableCell>
                      <TableCell align="right">{Number(item.totalQuantityIssued) || 0}</TableCell>
                    </TableRow>
                  ))}
                  {consumptionItems.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No consumption data. Select date range and click Run Report.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {!running && reportType === 'SALES_SUMMARY' && salesSummary && (
            <Box>
              <Box display="flex" gap={4} mb={2}>
                <Typography variant="body2">
                  Total Qty Issued: <strong>{Number(salesSummary.totalQuantityIssued) || 0}</strong>
                </Typography>
                <Typography variant="body2">
                  Distinct Drugs: <strong>{salesSummary.distinctDrugCount}</strong>
                </Typography>
                {salesSummary.estimatedRevenueTotal != null && (
                  <Typography variant="body2">
                    Est. Revenue: <strong>{Number(salesSummary.estimatedRevenueTotal).toFixed(2)}</strong>
                  </Typography>
                )}
              </Box>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Drug (Generic)</TableCell>
                      <TableCell>Brand</TableCell>
                      <TableCell align="right">Qty Issued</TableCell>
                      <TableCell align="right">Est. Revenue</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(salesSummary?.byDrug ?? []).map((item) => (
                      <TableRow key={String(item.drugId)} hover>
                        <TableCell>{item.genericName}</TableCell>
                        <TableCell>{item.brandName || '-'}</TableCell>
                        <TableCell align="right">{Number(item.totalQuantityIssued) || 0}</TableCell>
                        <TableCell align="right">
                          {item.estimatedRevenue != null ? Number(item.estimatedRevenue).toFixed(2) : '-'}
                        </TableCell>
                      </TableRow>
                    ))}
                    {(salesSummary?.byDrug ?? []).length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No data. Select date range and click Run Report.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
          {!running && reportType === 'SALES_SUMMARY' && !salesSummary && (
            <Typography variant="body2" color="text.secondary" align="center" py={3}>
              Select date range and click Run Report.
            </Typography>
          )}

          {!running && reportType === 'CONTROLLED_SUBSTANCE' && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Dispense Time</TableCell>
                    <TableCell>Drug (Generic)</TableCell>
                    <TableCell>Brand</TableCell>
                    <TableCell>Patient ID</TableCell>
                    <TableCell align="right">Qty Dispensed</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {controlledRows.map((row) => (
                    <TableRow key={String(row.dispenseLineId)} hover>
                      <TableCell>{row.dispensedAt ? new Date(row.dispensedAt).toLocaleString() : '-'}</TableCell>
                      <TableCell>{row.genericName || '-'}</TableCell>
                      <TableCell>{row.brandName || '-'}</TableCell>
                      <TableCell>{row.patientId || '-'}</TableCell>
                      <TableCell align="right">{Number(row.quantityDispensed) || 0}</TableCell>
                    </TableRow>
                  ))}
                  {controlledRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No controlled substance records. Select date range and click Run Report.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {!running && reportType === 'STOCK_OVERRIDES' && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Dispensed At</TableCell>
                    <TableCell>Pharmacy</TableCell>
                    <TableCell>Drug (Generic)</TableCell>
                    <TableCell>Override Reason</TableCell>
                    <TableCell align="right">Qty Dispensed</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {overrideRows.map((row) => (
                    <TableRow key={String(row.dispenseLineId)} hover>
                      <TableCell>{row.dispensedAt ? new Date(row.dispensedAt).toLocaleString() : '-'}</TableCell>
                      <TableCell>{row.pharmacyLocationName || '-'}</TableCell>
                      <TableCell>{row.genericName || '-'}</TableCell>
                      <TableCell>{row.overrideReasonCode || '-'}</TableCell>
                      <TableCell align="right">{Number(row.quantityDispensed) || 0}</TableCell>
                    </TableRow>
                  ))}
                  {overrideRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No stock override records. Select date range and click Run Report.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {!running && reportType === 'DISPENSE_ORDERS' && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Order ID</TableCell>
                    <TableCell>Patient</TableCell>
                    <TableCell>Order Status</TableCell>
                    <TableCell align="right">Dispensed Lines</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell>Completed</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dispenseOrders.map((order) => {
                    const dispensedCount = order.lines.filter((line) => DISPENSED_LINE_STATUSES.includes(line.status)).length;
                    return (
                      <TableRow key={order.id} hover>
                        <TableCell>{order.id}</TableCell>
                        <TableCell>{order.patientId || '-'}</TableCell>
                        <TableCell>{order.status}</TableCell>
                        <TableCell align="right">{dispensedCount}</TableCell>
                        <TableCell>{new Date(order.createdAt).toLocaleString()}</TableCell>
                        <TableCell>{order.completedAt ? new Date(order.completedAt).toLocaleString() : '-'}</TableCell>
                      </TableRow>
                    );
                  })}
                  {dispenseOrders.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography variant="body2" color="text.secondary">
                          No completed/dispensed orders in range. Set filters and click Run Report.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default PharmacyReportsPage;
