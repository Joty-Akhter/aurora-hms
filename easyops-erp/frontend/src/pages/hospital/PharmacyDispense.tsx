import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
  CircularProgress,
  Checkbox,
  FormControlLabel,
  IconButton,
} from '@mui/material';
import {
  Add as AddIcon,
  DeleteOutline as DeleteOutlineIcon,
  LocalPharmacy as LocalPharmacyIcon,
  Print as PrintIcon,
  Receipt as ReceiptIcon,
  Refresh as RefreshIcon,
  Save as SaveIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalPharmacyService, {
  BillableDispenseItemResponse,
  ConsumptionReportItemResponse,
  ControlledSubstanceRegisterRow,
  DispenseContextType,
  DispenseLine,
  DispenseLineRequest,
  DispenseOrder,
  DispenseOrderStatus,
  DispenseReturnRequest,
  DispenseUnfulfilledLineRequest,
  Drug,
  PharmacyLocation,
  PharmacyStockItem,
  SalesSummaryResponse,
  StockOverrideLineReportResponse,
  PatchDispenseOrderRegionalRequest,
} from '../../services/hospitalPharmacyService';
import hospitalService from '../../services/hospitalService';
import { openPharmacyPosReceiptPrint, type PosReceiptSnapshot } from '../../utils/pharmacyPosReceipt';
import './Hospital.css';

/** Matches canonical UUID strings accepted by Jackson for `java.util.UUID`. */
const UUID_STRING_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuidString(value: string): boolean {
  return UUID_STRING_RE.test(value.trim());
}

/** Prefer server `message` / Bean Validation `errors` map from API error responses (Phase P1). */
function getApiErrorMessage(err: unknown, fallback: string): string {
  const data = (err as { response?: { data?: { message?: string; error?: string; errors?: Record<string, string> } } })
    ?.response?.data;
  if (!data) return fallback;
  if (typeof data.message === 'string' && data.message.trim()) return data.message.trim();
  if (typeof data.error === 'string' && data.error.trim()) return data.error.trim();
  if (data.errors && typeof data.errors === 'object') {
    const first = Object.values(data.errors).find((v) => typeof v === 'string' && String(v).trim());
    if (first) return String(first).trim();
  }
  return fallback;
}

function lineStatusChipColor(
  status: DispenseLine['status']
): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' {
  switch (status) {
    case 'DISPENSED':
      return 'success';
    case 'PARTIALLY_DISPENSED':
      return 'warning';
    case 'FILLED_WITH_STOCK_OVERRIDE':
      return 'warning';
    case 'REFUSED':
    case 'OUT_OF_STOCK':
      return 'error';
    default:
      return 'default';
  }
}

type TabKey = 'orders' | 'lines' | 'reports';
type ReportSubTabKey = 'overrides' | 'sales' | 'consumption' | 'controlled';

/** In-browser cart rows before POST /dispense-orders/{id}/lines (price is UI-only for billing estimate). */
type DraftDispenseLine = {
  key: string;
  drug: Drug;
  batchNumber: string;
  quantityDispensed: number;
  unitPrice: number;
  formularyOverrideReason?: string;
  clinicalSafetyOverrideReason?: string;
  stockOverrideReason?: string;
};

const DRAFT_STORAGE_PREFIX = 'pharmacy-dispense-draft-v1';

function formatExpiryShort(iso?: string): string {
  if (!iso?.trim()) return '—';
  const t = iso.trim();
  const d = new Date(t);
  if (!Number.isNaN(d.getTime())) {
    return `${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
  }
  if (/^\d{4}-\d{2}$/.test(t)) {
    const [y, m] = t.split('-');
    return `${m}/${y}`;
  }
  return t;
}

function stockDisplayClass(totalOnHand: number, requestedQty: number): string {
  if (totalOnHand <= 0) return 'pharmacy-dispense-stock-out';
  if (requestedQty > totalOnHand || totalOnHand <= 10) return 'pharmacy-dispense-stock-low';
  return 'pharmacy-dispense-stock-ok';
}

const PharmacyDispensePage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const enableFormularyFeature = false;

  const [loading, setLoading] = useState(true);
  const [pharmacies, setPharmacies] = useState<PharmacyLocation[]>([]);
  const [drugOptions, setDrugOptions] = useState<Drug[]>([]);
  const [drugSearchLoading, setDrugSearchLoading] = useState(false);
  const [cartDrugQuery, setCartDrugQuery] = useState('');
  const drugSearchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [pharmacyStockItems, setPharmacyStockItems] = useState<PharmacyStockItem[]>([]);
  const [pharmacyStockLoading, setPharmacyStockLoading] = useState(false);
  const [draftDispenseLines, setDraftDispenseLines] = useState<DraftDispenseLine[]>([]);
  const [workspacePatientName, setWorkspacePatientName] = useState('');
  const [workspacePatientPhone, setWorkspacePatientPhone] = useState('');
  const [workspacePrescriptionNotes, setWorkspacePrescriptionNotes] = useState('');
  const [workspaceContextKind, setWorkspaceContextKind] = useState<'WALK_IN' | 'PRESCRIPTION'>('PRESCRIPTION');
  const [billingDiscount, setBillingDiscount] = useState<number>(0);
  const [billingPaid, setBillingPaid] = useState<number>(0);
  const [bulkStockOverrideReason, setBulkStockOverrideReason] = useState('');
  const [selectedPharmacyId, setSelectedPharmacyId] = useState<string>('');

  const [orders, setOrders] = useState<DispenseOrder[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<DispenseOrder | null>(null);
  const [tab, setTab] = useState<TabKey>('orders');

  // Filters
  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [visitIdFilter, setVisitIdFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<DispenseOrderStatus | 'ALL'>('ALL');

  // Create order dialog
  const [orderDialogOpen, setOrderDialogOpen] = useState(false);
  const [orderForm, setOrderForm] = useState<{
    contextType: DispenseContextType;
    pharmacyLocationId: string;
    prescriptionId: string;
    visitId: string;
    patientId: string;
    departmentId: string;
    paperPrescriptionRef: string;
    prescriptionImageAttachmentId: string;
    externalValidationStatus: string;
  }>({
    contextType: 'PATIENT_PRESCRIPTION',
    pharmacyLocationId: '',
    prescriptionId: '',
    visitId: '',
    patientId: '',
    departmentId: '',
    paperPrescriptionRef: '',
    prescriptionImageAttachmentId: '',
    externalValidationStatus: 'NOT_REQUIRED',
  });

  const [unfulfilledDialogOpen, setUnfulfilledDialogOpen] = useState(false);
  const [unfulfilledForm, setUnfulfilledForm] = useState<{
    drugId: string;
    prescriptionLineId: string;
    quantityPrescribed: number | undefined;
    lineStatus: 'OUT_OF_STOCK' | 'REFUSED';
    reasonCode: string;
  }>({
    drugId: '',
    prescriptionLineId: '',
    quantityPrescribed: undefined,
    lineStatus: 'OUT_OF_STOCK',
    reasonCode: '',
  });
  const [unfulfilledDrugQuery, setUnfulfilledDrugQuery] = useState('');
  const [selectedUnfulfilledDrug, setSelectedUnfulfilledDrug] = useState<Drug | null>(null);

  // Returns dialog
  const [returnsDialogOpen, setReturnsDialogOpen] = useState(false);
  const [returnLineId, setReturnLineId] = useState<string>('');
  const [returnQuantity, setReturnQuantity] = useState<number>(0);
  const [returnReason, setReturnReason] = useState<string>('');

  // Patient / prescription search for create-order dialog
  const [patientOptions, setPatientOptions] = useState<{ label: string; id: string }[]>([]);
  const [patientInputValue, setPatientInputValue] = useState('');
  const [prescriptionOptions, setPrescriptionOptions] = useState<{ label: string; id: string }[]>([]);
  const [prescriptionInputValue, setPrescriptionInputValue] = useState('');

  // Billable items (P1 billing preview)
  const [billableDialogOpen, setBillableDialogOpen] = useState(false);
  const [billableLoading, setBillableLoading] = useState(false);
  const [billableItems, setBillableItems] = useState<BillableDispenseItemResponse[]>([]);

  const [stockReportFrom, setStockReportFrom] = useState('');
  const [stockReportTo, setStockReportTo] = useState('');
  const [stockReportRows, setStockReportRows] = useState<StockOverrideLineReportResponse[]>([]);
  const [stockReportLoading, setStockReportLoading] = useState(false);
  const [reportSubTab, setReportSubTab] = useState<ReportSubTabKey>('overrides');
  const [salesSummary, setSalesSummary] = useState<SalesSummaryResponse | null>(null);
  const [consumptionRows, setConsumptionRows] = useState<ConsumptionReportItemResponse[]>([]);
  const [controlledRows, setControlledRows] = useState<ControlledSubstanceRegisterRow[]>([]);
  const [reportExtraLoading, setReportExtraLoading] = useState(false);

  const [regionalDialogOpen, setRegionalDialogOpen] = useState(false);
  const [regionalForm, setRegionalForm] = useState<{
    paperPrescriptionRef: string;
    prescriptionImageAttachmentId: string;
    externalValidationStatus: string;
    clearPrescriptionImageAttachment: boolean;
  }>({
    paperPrescriptionRef: '',
    prescriptionImageAttachmentId: '',
    externalValidationStatus: 'NOT_REQUIRED',
    clearPrescriptionImageAttachment: false,
  });

  const [formularyAlts, setFormularyAlts] = useState<Drug[] | null>(null);
  const [formularyAltsLoading, setFormularyAltsLoading] = useState(false);
  const [formularyDraftLineKey, setFormularyDraftLineKey] = useState<string | null>(null);
  const [workspaceDraftReady, setWorkspaceDraftReady] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedPharmacyId) {
      loadOrders();
    }
  }, [selectedPharmacyId, statusFilter]);

  useEffect(() => {
    return () => {
      if (drugSearchTimeoutRef.current) clearTimeout(drugSearchTimeoutRef.current);
    };
  }, []);

  useEffect(() => {
    if (tab !== 'lines' || !selectedOrder) return;
    const pid = selectedOrder.pharmacyLocationId;
    let cancelled = false;
    (async () => {
      try {
        setPharmacyStockLoading(true);
        const rows = await hospitalPharmacyService.getPharmacyStock(pid);
        if (!cancelled) setPharmacyStockItems(rows);
      } catch (err: unknown) {
        if (!cancelled) {
          enqueueSnackbar(getApiErrorMessage(err, 'Failed to load pharmacy stock'), { variant: 'error' });
          setPharmacyStockItems([]);
        }
      } finally {
        if (!cancelled) setPharmacyStockLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tab, selectedOrder?.id, selectedOrder?.pharmacyLocationId]);

  useEffect(() => {
    if (!selectedOrder) {
      setWorkspaceDraftReady(false);
      return;
    }
    setWorkspaceDraftReady(false);
    try {
      const raw = localStorage.getItem(`${DRAFT_STORAGE_PREFIX}-${selectedOrder.id}`);
      if (!raw) {
        setDraftDispenseLines([]);
        setWorkspacePatientName('');
        setWorkspacePatientPhone('');
        setWorkspacePrescriptionNotes('');
        setWorkspaceContextKind(selectedOrder.contextType === 'WALK_IN' ? 'WALK_IN' : 'PRESCRIPTION');
        setBillingDiscount(0);
        setBillingPaid(0);
        setBulkStockOverrideReason('');
        setWorkspaceDraftReady(true);
        return;
      }
      const p = JSON.parse(raw) as {
        draftDispenseLines?: DraftDispenseLine[];
        workspacePatientName?: string;
        workspacePatientPhone?: string;
        workspacePrescriptionNotes?: string;
        workspaceContextKind?: 'WALK_IN' | 'PRESCRIPTION';
        billingDiscount?: number;
        billingPaid?: number;
        bulkStockOverrideReason?: string;
      };
      setDraftDispenseLines(Array.isArray(p.draftDispenseLines) ? p.draftDispenseLines : []);
      setWorkspacePatientName(p.workspacePatientName ?? '');
      setWorkspacePatientPhone(p.workspacePatientPhone ?? '');
      setWorkspacePrescriptionNotes(p.workspacePrescriptionNotes ?? '');
      setWorkspaceContextKind(p.workspaceContextKind ?? 'PRESCRIPTION');
      setBillingDiscount(Number(p.billingDiscount) || 0);
      setBillingPaid(Number(p.billingPaid) || 0);
      setBulkStockOverrideReason(p.bulkStockOverrideReason ?? '');
    } catch {
      setDraftDispenseLines([]);
      setWorkspaceDraftReady(true);
      return;
    }
    setWorkspaceDraftReady(true);
  }, [selectedOrder?.id]);

  useEffect(() => {
    if (!selectedOrder || !workspaceDraftReady) return;
    try {
      localStorage.setItem(
        `${DRAFT_STORAGE_PREFIX}-${selectedOrder.id}`,
        JSON.stringify({
          draftDispenseLines,
          workspacePatientName,
          workspacePatientPhone,
          workspacePrescriptionNotes,
          workspaceContextKind,
          billingDiscount,
          billingPaid,
          bulkStockOverrideReason,
        })
      );
    } catch {
      /* quota */
    }
  }, [
    selectedOrder?.id,
    workspaceDraftReady,
    draftDispenseLines,
    workspacePatientName,
    workspacePatientPhone,
    workspacePrescriptionNotes,
    workspaceContextKind,
    billingDiscount,
    billingPaid,
    bulkStockOverrideReason,
  ]);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const pharms = await hospitalPharmacyService.getPharmacies({ activeOnly: true });
      setPharmacies(pharms);
      if (pharms.length > 0) {
        setSelectedPharmacyId(pharms[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load pharmacy dispense data:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load pharmacy dispense data'), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const getDrugLabel = (drug: Drug) => {
    const formPrefix = drug.form?.trim() ? drug.form.trim().slice(0, 3) : '';
    const brand = drug.brandName?.trim();
    const generic = drug.genericName?.trim();
    const strength = drug.strength?.trim();

    const brandPart = brand ? `${brand} (${generic})` : generic ?? '';
    const prefix = formPrefix ? `${formPrefix}. ` : '';
    return `${prefix}${brandPart}${strength ? ` ${strength}` : ''}`.trim();
  };

  /** Label for Autocomplete (includes manufacturer for accessibility / closed state). */
  const getDrugOptionLabel = (drug: Drug) => {
    const base = getDrugLabel(drug);
    const mfr = drug.manufacturerName?.trim();
    return mfr ? `${base} — ${mfr}` : base;
  };

  const renderDrugAutocompleteOption = (
    props: React.HTMLAttributes<HTMLLIElement> & { key?: React.Key },
    option: Drug
  ) => {
    const { key, ...liProps } = props;
    return (
      <li key={key ?? option.id} {...liProps}>
        <Box sx={{ py: 0.5 }}>
          <Typography variant="body2">{getDrugLabel(option)}</Typography>
          {option.manufacturerName?.trim() ? (
            <Typography variant="caption" color="text.secondary" display="block">
              {option.manufacturerName.trim()}
            </Typography>
          ) : null}
        </Box>
      </li>
    );
  };

  const cartFieldSx = {
    '& .MuiInputBase-root': { minHeight: 44 },
    '& .MuiInputBase-input': { fontSize: '1rem', py: 1.25 },
  } as const;

  const searchDrugOptions = (searchTerm: string) => {
    if (drugSearchTimeoutRef.current) {
      clearTimeout(drugSearchTimeoutRef.current);
    }
    const query = searchTerm.trim();
    if (query.length < 2) {
      setDrugOptions([]);
      setDrugSearchLoading(false);
      return;
    }
    setDrugSearchLoading(true);
    drugSearchTimeoutRef.current = setTimeout(async () => {
      try {
        const data = await hospitalPharmacyService.getDrugs({ name: query, activeOnly: true });
        const q = query.toLowerCase();
        const scored = data
          .map((d) => {
            const brand = d.brandName?.trim().toLowerCase() ?? '';
            const generic = d.genericName?.trim().toLowerCase() ?? '';
            const form = d.form?.trim().toLowerCase() ?? '';
            const mfr = d.manufacturerName?.trim().toLowerCase() ?? '';

            // Prefer exact match, then "startsWith", then "includes"
            const exact =
              (brand && brand === q) || (generic && generic === q) ? 0 : 1;
            const starts =
              (brand && brand.startsWith(q)) ||
              (generic && generic.startsWith(q)) ||
              (mfr && mfr.startsWith(q))
                ? 0
                : 1;
            const includes =
              (brand && brand.includes(q)) ||
              (generic && generic.includes(q)) ||
              (mfr && mfr.includes(q))
                ? 0
                : 1;

            const formHit = form.startsWith(q) ? -1 : 0;

            const score = exact * 1000 + starts * 100 + includes * 10 + (formHit === -1 ? 1 : 0);
            return { d, score };
          })
          .sort((a, b) => a.score - b.score)
          .map((x) => x.d);

        setDrugOptions(scored.slice(0, 100));
      } catch (err) {
        console.error('Failed to search drugs:', err);
      } finally {
        setDrugSearchLoading(false);
      }
    }, 250);
  };

  const loadOrders = async () => {
    if (!selectedPharmacyId) return;
    try {
      const params: any = { pharmacyLocationId: selectedPharmacyId };
      const patientFilterRaw = patientIdFilter.trim();
      if (patientFilterRaw) {
        params.patientId = patientFilterRaw;
      }
      const visitFilterRaw = visitIdFilter.trim();
      if (visitFilterRaw) {
        params.visitId = visitFilterRaw;
      }
      if (statusFilter !== 'ALL') params.status = statusFilter;
      const data = await hospitalPharmacyService.searchDispenseOrders(params);
      setOrders(data);
      if (selectedOrder) {
        const updated = data.find((o) => o.id === selectedOrder.id) || null;
        setSelectedOrder(updated);
      }
    } catch (err: any) {
      console.error('Failed to load dispense orders:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load dispense orders'), { variant: 'error' });
    }
  };

  const currentPharmacy = useMemo(
    () => pharmacies.find((p) => p.id === selectedPharmacyId),
    [pharmacies, selectedPharmacyId]
  );

  const stockByDrugId = useMemo(() => {
    const m = new Map<string, { total: number; earliestExpiry?: string }>();
    for (const row of pharmacyStockItems) {
      const id = row.drugId;
      const q = Number(row.quantityOnHand) || 0;
      const cur = m.get(id);
      const exp = row.expiryDate?.trim();
      if (!cur) {
        m.set(id, { total: q, earliestExpiry: exp });
      } else {
        cur.total += q;
        if (exp && (!cur.earliestExpiry || exp < cur.earliestExpiry)) {
          cur.earliestExpiry = exp;
        }
      }
    }
    return m;
  }, [pharmacyStockItems]);

  const cartSubtotal = useMemo(
    () =>
      draftDispenseLines.reduce(
        (sum, line) => sum + line.quantityDispensed * (Number(line.unitPrice) || 0),
        0
      ),
    [draftDispenseLines]
  );

  const billingTotalAfterDiscount = Math.max(0, cartSubtotal - (Number(billingDiscount) || 0));
  const billingDue = Math.max(0, billingTotalAfterDiscount - (Number(billingPaid) || 0));

  const goToDispenseWorkspace = (order: DispenseOrder) => {
    setSelectedOrder(order);
    setTab('lines');
  };

  const safeRandomUUID = () => {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `uuid-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  };

  const appendDrugToDraft = (drug: Drug) => {
    setDraftDispenseLines((prev) => [
      ...prev,
      {
        key: safeRandomUUID(),
        drug,
        batchNumber: '',
        quantityDispensed: 1,
        unitPrice: 0,
      },
    ]);
    setCartDrugQuery('');
    setDrugOptions([]);
  };

  const updateDraftLine = (key: string, patch: Partial<DraftDispenseLine>) => {
    setDraftDispenseLines((prev) => prev.map((row) => (row.key === key ? { ...row, ...patch } : row)));
  };

  const removeDraftLine = (key: string) => {
    setDraftDispenseLines((prev) => prev.filter((r) => r.key !== key));
    setFormularyDraftLineKey((k) => {
      if (k === key) {
        setFormularyAlts(null);
        return null;
      }
      return k;
    });
  };

  const handleLoadFormularyAltsForLine = async (lineKey: string, drugId: string) => {
    try {
      setFormularyAltsLoading(true);
      setFormularyDraftLineKey(lineKey);
      const alts = await hospitalPharmacyService.getFormularyAlternatives(drugId);
      setFormularyAlts(alts);
      if (alts.length === 0) {
        enqueueSnackbar('No preferred alternatives configured for this drug in formulary rules', {
          variant: 'info',
        });
      }
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load formulary alternatives'), { variant: 'error' });
    } finally {
      setFormularyAltsLoading(false);
    }
  };

  const applyFormularyDrugToLine = (lineKey: string, drug: Drug) => {
    updateDraftLine(lineKey, { drug });
    setFormularyAlts(null);
    setFormularyDraftLineKey(null);
  };

  const handleDispenseCartAndPrint = async () => {
    if (!selectedOrder) return;
    if (draftDispenseLines.length === 0) {
      enqueueSnackbar('Add at least one medicine to the cart', { variant: 'warning' });
      return;
    }
    const linesOut: DispenseLineRequest[] = [];
    for (const d of draftDispenseLines) {
      if (!d.quantityDispensed || d.quantityDispensed <= 0) {
        enqueueSnackbar(`Enter a positive quantity for ${getDrugLabel(d.drug)}`, { variant: 'warning' });
        return;
      }
      const onHand = stockByDrugId.get(d.drug.id)?.total ?? 0;
      const short = d.quantityDispensed > onHand;
      const mergedReason = (d.stockOverrideReason || bulkStockOverrideReason || '').trim();
      linesOut.push({
        drugId: d.drug.id,
        batchNumber: d.batchNumber.trim() || undefined,
        quantityDispensed: d.quantityDispensed,
        stockOverrideReason: short ? mergedReason || undefined : undefined,
        formularyOverrideReason: d.formularyOverrideReason?.trim() || undefined,
        clinicalSafetyOverrideReason: d.clinicalSafetyOverrideReason?.trim() || undefined,
      });
    }
    const posSnapshot: PosReceiptSnapshot = {
      patientName: workspacePatientName.trim() || undefined,
      patientPhone: workspacePatientPhone.trim() || undefined,
      lines: draftDispenseLines.map((row) => {
        const unit = Number(row.unitPrice) || 0;
        const qty = row.quantityDispensed;
        return {
          drugLabel: getDrugLabel(row.drug),
          quantityDispensed: qty,
          unitPrice: unit,
          lineTotal: qty * unit,
          batchNumber: row.batchNumber.trim() || undefined,
        };
      }),
      discount: billingDiscount,
      paid: billingPaid,
    };
    try {
      const order = await hospitalPharmacyService.addDispenseLines(selectedOrder.id, linesOut, {
        idempotencyKey: safeRandomUUID(),
      });
      enqueueSnackbar('Dispensed successfully', { variant: 'success' });
      setDraftDispenseLines([]);
      setBulkStockOverrideReason('');
      setSelectedOrder(order);
      await loadOrders();
      if (!openPharmacyPosReceiptPrint(order, posSnapshot)) {
        enqueueSnackbar('Allow pop-ups for this site to open the POS print dialog.', { variant: 'warning' });
      }
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to record dispensing'), { variant: 'error' });
    }
  };

  const handleSaveDraftClicked = () => {
    enqueueSnackbar('Draft saved successfully.', { variant: 'success' });
    setTab('orders');
  };

  const handleCancelCart = () => {
    if (!selectedOrder) return;
    setDraftDispenseLines([]);
    setBulkStockOverrideReason('');
    setFormularyAlts(null);
    setFormularyDraftLineKey(null);
    try {
      localStorage.removeItem(`${DRAFT_STORAGE_PREFIX}-${selectedOrder.id}`);
    } catch {
      /* ignore */
    }
  };

  const handleOpenOrderDialog = () => {
    if (!selectedPharmacyId) {
      enqueueSnackbar('Please select a pharmacy first', { variant: 'warning' });
      return;
    }
    setOrderForm({
      contextType: 'PATIENT_PRESCRIPTION',
      pharmacyLocationId: selectedPharmacyId,
      prescriptionId: '',
      visitId: '',
      patientId: '',
      departmentId: '',
      paperPrescriptionRef: '',
      prescriptionImageAttachmentId: '',
      externalValidationStatus: 'NOT_REQUIRED',
    });
    setPatientInputValue('');
    setPatientOptions([]);
    setPrescriptionInputValue('');
    setPrescriptionOptions([]);
    setOrderDialogOpen(true);
  };

  const searchPatients = async (query: string) => {
    if (!query.trim()) { setPatientOptions([]); return; }
    try {
      const res = await hospitalService.searchPatients(query);
      const list = (res.data || []) as any[];
      setPatientOptions(
        list.slice(0, 20).map((p: any) => ({ label: `${p.fullName} (${p.mrn})`, id: p.patientId }))
      );
    } catch { /* ignore */ }
  };

  const searchPrescriptions = async (query: string) => {
    if (!query.trim()) { setPrescriptionOptions([]); return; }
    try {
      const res = await hospitalService.getPrescriptionByNumber(encodeURIComponent(query));
      const p = res.data as any;
      setPrescriptionOptions([{
        label: `Rx#${p.prescriptionNumber || p.prescriptionId?.substring(0, 8)} — ${p.medicationName || p.medications?.[0]?.medicationName || '?'}`,
        id: p.prescriptionId,
      }]);
    } catch { setPrescriptionOptions([]); }
  };

  const handleCreateOrder = async () => {
    try {
      if (!orderForm.pharmacyLocationId) {
        enqueueSnackbar('Pharmacy is required', { variant: 'warning' });
        return;
      }

      const deptRaw = orderForm.departmentId.trim();
      if (deptRaw && !isUuidString(deptRaw)) {
        enqueueSnackbar('Department ID must be a UUID, or leave it blank.', { variant: 'warning' });
        return;
      }

      const hasContext =
        orderForm.patientId.trim() ||
        orderForm.prescriptionId.trim() ||
        deptRaw ||
        orderForm.paperPrescriptionRef.trim();
      if (!hasContext) {
        enqueueSnackbar('Please specify at least a patient, prescription, department, or paper prescription reference.', { variant: 'warning' });
        return;
      }

      let prescriptionId: string | undefined;
      const prescRaw = orderForm.prescriptionId.trim();
      if (prescRaw) {
        if (isUuidString(prescRaw)) {
          prescriptionId = prescRaw.trim();
        } else {
          try {
            const presc = await hospitalService.getPrescriptionByNumber(encodeURIComponent(prescRaw));
            prescriptionId = presc.data.prescriptionId;
          } catch {
            enqueueSnackbar(
              `No prescription found for number "${prescRaw}". Enter the prescription number (e.g. 001) or the prescription UUID.`,
              { variant: 'error' }
            );
            return;
          }
        }
      }

      const patientRaw = orderForm.patientId.trim();
      const patientId = patientRaw ? patientRaw : undefined;

      const visitRaw = orderForm.visitId.trim();
      const visitId = visitRaw ? visitRaw : undefined;

      const paperRaw = orderForm.paperPrescriptionRef.trim();
      const attachRaw = orderForm.prescriptionImageAttachmentId.trim();
      if (attachRaw && !isUuidString(attachRaw)) {
        enqueueSnackbar('Prescription image attachment must be a UUID, or leave it blank.', { variant: 'warning' });
        return;
      }
      const payload = {
        contextType: orderForm.contextType,
        pharmacyLocationId: orderForm.pharmacyLocationId,
        ...(prescriptionId ? { prescriptionId } : {}),
        ...(visitId ? { visitId } : {}),
        ...(patientId ? { patientId } : {}),
        ...(deptRaw ? { departmentId: deptRaw } : {}),
        ...(paperRaw ? { paperPrescriptionRef: paperRaw } : {}),
        ...(attachRaw && isUuidString(attachRaw)
          ? { prescriptionImageAttachmentId: attachRaw.trim() }
          : {}),
        ...(orderForm.externalValidationStatus
          ? { externalValidationStatus: orderForm.externalValidationStatus }
          : {}),
      };
      const order = await hospitalPharmacyService.createDispenseOrder(payload);
      enqueueSnackbar('Dispense order created', { variant: 'success' });
      setOrderDialogOpen(false);
      setSelectedOrder(order);
      await loadOrders();
      setTab('lines');
    } catch (err: any) {
      console.error('Failed to create dispense order:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to create dispense order'), { variant: 'error' });
    }
  };

  const handleOpenRegionalDialog = () => {
    if (!selectedOrder) return;
    setRegionalForm({
      paperPrescriptionRef: selectedOrder.paperPrescriptionRef ?? '',
      prescriptionImageAttachmentId: selectedOrder.prescriptionImageAttachmentId ?? '',
      externalValidationStatus: selectedOrder.externalValidationStatus ?? 'NOT_REQUIRED',
      clearPrescriptionImageAttachment: false,
    });
    setRegionalDialogOpen(true);
  };

  const handleSaveRegional = async () => {
    if (!selectedOrder) return;
    const attachRaw = regionalForm.prescriptionImageAttachmentId.trim();
    if (attachRaw && !isUuidString(attachRaw)) {
      enqueueSnackbar('Prescription image attachment must be a UUID or blank.', { variant: 'warning' });
      return;
    }
    try {
      const payload: PatchDispenseOrderRegionalRequest = {
        paperPrescriptionRef: regionalForm.paperPrescriptionRef.trim(),
        externalValidationStatus: regionalForm.externalValidationStatus,
      };
      if (regionalForm.clearPrescriptionImageAttachment) {
        payload.clearPrescriptionImageAttachment = true;
      } else if (attachRaw) {
        payload.prescriptionImageAttachmentId = attachRaw;
      }
      const order = await hospitalPharmacyService.patchDispenseOrderRegional(selectedOrder.id, payload);
      enqueueSnackbar('Paper / validation updated', { variant: 'success' });
      setRegionalDialogOpen(false);
      setSelectedOrder(order);
      await loadOrders();
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to update regional details'), { variant: 'error' });
    }
  };

  const handleDownloadReceipt = async () => {
    if (!selectedOrder) return;
    try {
      const blob = await hospitalPharmacyService.downloadDispenseReceiptPdf(selectedOrder.id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to open receipt PDF'), { variant: 'error' });
    }
  };

  const handlePrintPosReceipt = () => {
    if (!selectedOrder) return;
    if (!openPharmacyPosReceiptPrint(selectedOrder)) {
      enqueueSnackbar('Allow pop-ups for this site to open the POS print dialog.', { variant: 'warning' });
    }
  };

  const handleLoadStockOverrides = async () => {
    if (!selectedPharmacyId || !stockReportFrom || !stockReportTo) {
      enqueueSnackbar('Select pharmacy and a from/to date range', { variant: 'warning' });
      return;
    }
    try {
      setStockReportLoading(true);
      const rows = await hospitalPharmacyService.getStockOverrideReport(
        selectedPharmacyId,
        stockReportFrom,
        stockReportTo
      );
      setStockReportRows(rows);
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load stock override report'), { variant: 'error' });
    } finally {
      setStockReportLoading(false);
    }
  };

  const handleLoadSalesSummary = async () => {
    if (!selectedPharmacyId || !stockReportFrom || !stockReportTo) {
      enqueueSnackbar('Select pharmacy and a from/to date range', { variant: 'warning' });
      return;
    }
    try {
      setReportExtraLoading(true);
      const data = await hospitalPharmacyService.getSalesSummary(
        selectedPharmacyId,
        stockReportFrom,
        stockReportTo
      );
      setSalesSummary(data);
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load sales summary'), { variant: 'error' });
    } finally {
      setReportExtraLoading(false);
    }
  };

  const handleLoadConsumption = async () => {
    if (!selectedPharmacyId || !stockReportFrom || !stockReportTo) {
      enqueueSnackbar('Select pharmacy and a from/to date range', { variant: 'warning' });
      return;
    }
    try {
      setReportExtraLoading(true);
      const rows = await hospitalPharmacyService.getConsumptionReport(
        selectedPharmacyId,
        stockReportFrom,
        stockReportTo
      );
      setConsumptionRows(rows);
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load consumption report'), { variant: 'error' });
    } finally {
      setReportExtraLoading(false);
    }
  };

  const handleLoadControlledRegister = async () => {
    if (!selectedPharmacyId || !stockReportFrom || !stockReportTo) {
      enqueueSnackbar('Select pharmacy and a from/to date range', { variant: 'warning' });
      return;
    }
    try {
      setReportExtraLoading(true);
      const rows = await hospitalPharmacyService.getControlledSubstanceRegister(
        selectedPharmacyId,
        stockReportFrom,
        stockReportTo
      );
      setControlledRows(rows);
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load controlled substance register'), { variant: 'error' });
    } finally {
      setReportExtraLoading(false);
    }
  };

  const handleDownloadConsumptionCsv = async () => {
    if (!selectedPharmacyId || !stockReportFrom || !stockReportTo) {
      enqueueSnackbar('Select pharmacy and a from/to date range', { variant: 'warning' });
      return;
    }
    try {
      setReportExtraLoading(true);
      const blob = await hospitalPharmacyService.downloadConsumptionExport(
        selectedPharmacyId,
        stockReportFrom,
        stockReportTo
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `consumption-${stockReportFrom}-${stockReportTo}.csv`;
      a.rel = 'noopener';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      enqueueSnackbar('Download started', { variant: 'success' });
    } catch (err: unknown) {
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to download CSV'), { variant: 'error' });
    } finally {
      setReportExtraLoading(false);
    }
  };

  const handleOpenUnfulfilledDialog = () => {
    setUnfulfilledForm({
      drugId: '',
      prescriptionLineId: '',
      quantityPrescribed: undefined,
      lineStatus: 'OUT_OF_STOCK',
      reasonCode: '',
    });
    setUnfulfilledDrugQuery('');
    setSelectedUnfulfilledDrug(null);
    setUnfulfilledDialogOpen(true);
  };

  const handleRecordUnfulfilled = async () => {
    if (!selectedOrder || !unfulfilledForm.drugId || !unfulfilledForm.reasonCode.trim()) {
      enqueueSnackbar('Drug and reason are required', { variant: 'warning' });
      return;
    }
    try {
      const payload: DispenseUnfulfilledLineRequest = {
        drugId: unfulfilledForm.drugId,
        lineStatus: unfulfilledForm.lineStatus,
        reasonCode: unfulfilledForm.reasonCode.trim(),
        quantityPrescribed: unfulfilledForm.quantityPrescribed,
      };
      const pl = unfulfilledForm.prescriptionLineId.trim();
      if (pl && isUuidString(pl)) {
        payload.prescriptionLineId = pl;
      }
      const order = await hospitalPharmacyService.recordUnfulfilledLine(selectedOrder.id, payload);
      enqueueSnackbar('Unfulfilled line recorded', { variant: 'success' });
      setUnfulfilledDialogOpen(false);
      setSelectedOrder(order);
      await loadOrders();
    } catch (err: any) {
      console.error('Failed to record unfulfilled line:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to record unfulfilled line'), { variant: 'error' });
    }
  };

  const handleOpenReturnDialog = (order: DispenseOrder, lineId: string) => {
    setSelectedOrder(order);
    setReturnLineId(lineId);
    setReturnQuantity(0);
    setReturnReason('');
    setReturnsDialogOpen(true);
  };

  const handleRecordReturn = async () => {
    if (!selectedOrder || !returnLineId || returnQuantity <= 0) {
      enqueueSnackbar('Return quantity must be positive', { variant: 'warning' });
      return;
    }
    try {
      const payload: DispenseReturnRequest = {
        lines: [
          {
            dispenseLineId: returnLineId,
            quantityReturned: returnQuantity,
            reason: returnReason || undefined,
          },
        ],
      };
      const order = await hospitalPharmacyService.recordReturns(selectedOrder.id, payload, {
        idempotencyKey: safeRandomUUID(),
      });
      enqueueSnackbar('Return recorded', { variant: 'success' });
      setReturnsDialogOpen(false);
      setSelectedOrder(order);
      await loadOrders();
    } catch (err: any) {
      console.error('Failed to record return:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to record return'), { variant: 'error' });
    }
  };

  const handleOpenBillableDialog = async () => {
    if (!selectedOrder) return;
    setBillableDialogOpen(true);
    setBillableLoading(true);
    setBillableItems([]);
    try {
      const rows = await hospitalPharmacyService.getBillableItems(selectedOrder.id);
      setBillableItems(rows);
    } catch (err: any) {
      console.error('Failed to load billable items:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to load billable items'), { variant: 'error' });
      setBillableDialogOpen(false);
    } finally {
      setBillableLoading(false);
    }
  };

  const handleStatusChange = async (order: DispenseOrder, status: DispenseOrderStatus) => {
    try {
      const updated = await hospitalPharmacyService.updateDispenseOrderStatus(order.id, status);
      enqueueSnackbar('Dispense order status updated', { variant: 'success' });
      setSelectedOrder((prev) => (prev && prev.id === updated.id ? updated : prev));
      await loadOrders();
    } catch (err: any) {
      console.error('Failed to update order status:', err);
      enqueueSnackbar(getApiErrorMessage(err, 'Failed to update order status'), { variant: 'error' });
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
            Pharmacy Dispensing
          </Typography>
          <Typography variant="body2">
            Manage dispense orders, issues to patients, and returns.
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <FormControl size="small" sx={{ minWidth: 220, backgroundColor: 'white', borderRadius: 1 }}>
            <InputLabel>Pharmacy</InputLabel>
            <Select
              label="Pharmacy"
              value={selectedPharmacyId}
              onChange={(e) => setSelectedPharmacyId(e.target.value as string)}
              startAdornment={<LocalPharmacyIcon fontSize="small" sx={{ mr: 1 }} />}
            >
              {pharmacies.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name} ({p.type})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            variant="contained"
            color="secondary"
            startIcon={<RefreshIcon />}
            onClick={loadOrders}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleOpenOrderDialog}
          >
            New Order
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Patient (UUID or MRN)"
              size="small"
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
            />
            <TextField
              label="Visit (UUID or number)"
              size="small"
              value={visitIdFilter}
              onChange={(e) => setVisitIdFilter(e.target.value)}
            />
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as DispenseOrderStatus | 'ALL')}
              >
                <MenuItem value="ALL">All</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
              </Select>
            </FormControl>
            <Button variant="outlined" size="small" onClick={loadOrders}>
              Apply Filters
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Tabs
            value={tab}
            onChange={(_, value) => setTab(value)}
            textColor="primary"
            indicatorColor="primary"
          >
            <Tab label="Orders" value="orders" />
            <Tab label="Lines" value="lines" disabled={!selectedOrder} />
            <Tab label="Reports" value="reports" disabled={!selectedPharmacyId} />
          </Tabs>

          {tab === 'orders' && (
            <Box mt={2}>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Order ID</TableCell>
                      <TableCell>Context</TableCell>
                      <TableCell>Patient</TableCell>
                      <TableCell>Visit</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Created At</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {orders.map((order) => (
                      <TableRow
                        key={order.id}
                        hover
                        selected={selectedOrder?.id === order.id}
                        onClick={() => setSelectedOrder(order)}
                      >
                        <TableCell>{order.id}</TableCell>
                        <TableCell>{order.contextType}</TableCell>
                        <TableCell>{order.patientId || '-'}</TableCell>
                        <TableCell>{order.visitId || '-'}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={order.status}
                            color={
                              order.status === 'COMPLETED'
                                ? 'success'
                                : order.status === 'CANCELLED'
                                ? 'default'
                                : 'warning'
                            }
                          />
                        </TableCell>
                        <TableCell>{new Date(order.createdAt).toLocaleString()}</TableCell>
                        <TableCell align="right">
                          <Box display="flex" justifyContent="flex-end" gap={1}>
                            <Button
                              size="small"
                              variant="text"
                              onClick={(e) => {
                                e.stopPropagation();
                                goToDispenseWorkspace(order);
                              }}
                            >
                              Dispense
                            </Button>
                            <Button
                              size="small"
                              variant="text"
                              color="success"
                              onClick={() => handleStatusChange(order, 'COMPLETED')}
                            >
                              Complete
                            </Button>
                            <Button
                              size="small"
                              variant="text"
                              color="inherit"
                              onClick={() => handleStatusChange(order, 'CANCELLED')}
                            >
                              Cancel
                            </Button>
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}
                    {orders.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No dispense orders found. Create a new order to begin dispensing.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {tab === 'lines' && selectedOrder && (
            <Box mt={2}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2} flexWrap="wrap" gap={2}>
                <Box>
                  <Typography variant="subtitle1">Dispensing workspace</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Order {selectedOrder.id} · {selectedOrder.pharmacyLocationName}
                    {selectedOrder.patientId && (
                      <>
                        {' '}
                        · Patient ref: {selectedOrder.patientId}
                      </>
                    )}
                  </Typography>
                  {(selectedOrder.paperPrescriptionRef ||
                    selectedOrder.externalValidationStatus ||
                    selectedOrder.prescriptionImageAttachmentId) && (
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                      {selectedOrder.paperPrescriptionRef && (
                        <>Paper Rx: {selectedOrder.paperPrescriptionRef} · </>
                      )}
                      {selectedOrder.externalValidationStatus && (
                        <>Validation: {selectedOrder.externalValidationStatus} · </>
                      )}
                      {selectedOrder.prescriptionImageAttachmentId && (
                        <>Attachment: {selectedOrder.prescriptionImageAttachmentId}</>
                      )}
                    </Typography>
                  )}
                </Box>
                <Box display="flex" gap={1} flexWrap="wrap" justifyContent="flex-end">
                  <Button variant="outlined" size="small" onClick={handleOpenRegionalDialog}>
                    Paper / validation
                  </Button>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<ReceiptIcon />}
                    onClick={handlePrintPosReceipt}
                  >
                    Print POS receipt
                  </Button>
                  <Button variant="outlined" size="small" onClick={handleDownloadReceipt}>
                    Receipt PDF
                  </Button>
                  <Button variant="outlined" size="small" onClick={handleOpenBillableDialog}>
                    Billable items
                  </Button>
                  <Button variant="outlined" size="small" onClick={handleOpenUnfulfilledDialog}>
                    Not filled (OOS / refused)
                  </Button>
                </Box>
              </Box>

              <Box
                display="grid"
                gridTemplateColumns={{ xs: '1fr', md: 'repeat(3, 1fr)' }}
                gap={2}
                sx={{ mb: 2 }}
              >
                <Card variant="outlined" className="pharmacy-dispense-workspace-card">
                  <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Patient info
                    </Typography>
                    <TextField
                      fullWidth
                      size="small"
                      label="Display name"
                      placeholder="Patient name"
                      value={workspacePatientName}
                      onChange={(e) => setWorkspacePatientName(e.target.value)}
                      sx={{ mb: 1.5 }}
                    />
                    <TextField
                      fullWidth
                      size="small"
                      label="Phone"
                      placeholder="Phone"
                      value={workspacePatientPhone}
                      onChange={(e) => setWorkspacePatientPhone(e.target.value)}
                      sx={{ mb: 1.5 }}
                    />
                    <FormControl fullWidth size="small">
                      <InputLabel>Type</InputLabel>
                      <Select
                        label="Type"
                        value={workspaceContextKind}
                        onChange={(e) =>
                          setWorkspaceContextKind(e.target.value as 'WALK_IN' | 'PRESCRIPTION')
                        }
                      >
                        <MenuItem value="WALK_IN">Walk-in (OTC)</MenuItem>
                        <MenuItem value="PRESCRIPTION">Prescription</MenuItem>
                      </Select>
                    </FormControl>
                  </CardContent>
                </Card>
                <Card variant="outlined" className="pharmacy-dispense-workspace-card" sx={{ gridColumn: { xs: '1', md: 'span 1' } }}>
                  <CardContent sx={{ py: 2, height: '100%', '&:last-child': { pb: 2 } }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Prescription notes
                    </Typography>
                    <TextField
                      fullWidth
                      multiline
                      minRows={5}
                      placeholder="Doctor prescription, directions, or OTC notes…"
                      value={workspacePrescriptionNotes}
                      onChange={(e) => setWorkspacePrescriptionNotes(e.target.value)}
                    />
                  </CardContent>
                </Card>
                <Card variant="outlined" className="pharmacy-dispense-workspace-card">
                  <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Alerts
                    </Typography>
                    {pharmacyStockLoading ? (
                      <Box display="flex" alignItems="center" gap={1}>
                        <CircularProgress size={20} />
                        <Typography variant="body2" color="text.secondary">
                          Loading stock…
                        </Typography>
                      </Box>
                    ) : (
                      <Box component="ul" sx={{ m: 0, pl: 2.5, pr: 0, py: 0 }}>
                        {draftDispenseLines.length === 0 && (
                          <Typography component="li" variant="body2" color="text.secondary" sx={{ listStyle: 'none', ml: -2 }}>
                            Add medicines below. Interaction and allergy checks run when you dispense.
                          </Typography>
                        )}
                        {draftDispenseLines.map((row) => {
                          const onHand = stockByDrugId.get(row.drug.id)?.total ?? 0;
                          if (row.quantityDispensed <= onHand) return null;
                          return (
                            <Typography
                              component="li"
                              key={row.key}
                              variant="body2"
                              color="warning.main"
                              sx={{ mb: 0.5 }}
                            >
                              Recorded stock ({onHand}) is below requested quantity ({row.quantityDispensed}) for{' '}
                              {getDrugLabel(row.drug)} — you can still dispense; document a reason if your site
                              requires it.
                            </Typography>
                          );
                        })}
                        {draftDispenseLines.some((row) => {
                          const onHand = stockByDrugId.get(row.drug.id)?.total ?? 0;
                          return row.quantityDispensed > onHand;
                        }) === false &&
                          draftDispenseLines.length > 0 && (
                            <Typography component="li" variant="body2" color="success.main">
                              No short-stock rows in cart.
                            </Typography>
                          )}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Box>

              <Card variant="outlined" sx={{ mb: 2, p: 2, borderRadius: 2 }}>
                <Autocomplete
                  options={drugOptions}
                  loading={drugSearchLoading}
                  value={null}
                  inputValue={cartDrugQuery}
                  onInputChange={(_, value) => {
                    setCartDrugQuery(value);
                    searchDrugOptions(value);
                  }}
                  onChange={(_, value) => {
                    if (value) appendDrugToDraft(value);
                  }}
                  isOptionEqualToValue={(option, value) => option.id === value.id}
                  getOptionLabel={getDrugOptionLabel}
                  renderOption={renderDrugAutocompleteOption}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Search medicine"
                      placeholder="Barcode / name (type at least 2 characters)"
                    />
                  )}
                />
              </Card>

              <Card variant="outlined" sx={{ mb: 2, borderRadius: 2 }}>
                <TableContainer>
                  <Table size="medium">
                    <TableHead>
                      <TableRow>
                        <TableCell>Drug</TableCell>
                        <TableCell sx={{ minWidth: 168 }}>Batch</TableCell>
                        <TableCell sx={{ minWidth: 128 }}>Dispense qty</TableCell>
                        <TableCell width={72}>Stock</TableCell>
                        <TableCell width={88}>Expiry</TableCell>
                        <TableCell sx={{ minWidth: 120 }}>Price</TableCell>
                        <TableCell width={88}>Total</TableCell>
                        <TableCell width={56} align="right" />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {draftDispenseLines.map((row) => {
                        const onHand = stockByDrugId.get(row.drug.id)?.total ?? 0;
                        const exp = stockByDrugId.get(row.drug.id)?.earliestExpiry;
                        const lineTotal = row.quantityDispensed * (Number(row.unitPrice) || 0);
                        return (
                          <TableRow key={row.key}>
                            <TableCell>
                              <Typography variant="body2">{getDrugLabel(row.drug)}</Typography>
                              {row.drug.manufacturerName?.trim() ? (
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {row.drug.manufacturerName.trim()}
                                </Typography>
                              ) : null}
                              {enableFormularyFeature && (
                                <>
                                  <Button
                                    size="small"
                                    sx={{ mt: 0.5, p: 0, minWidth: 0, textTransform: 'none' }}
                                    onClick={() => handleLoadFormularyAltsForLine(row.key, row.drug.id)}
                                    disabled={formularyAltsLoading}
                                  >
                                    {formularyAltsLoading && formularyDraftLineKey === row.key
                                      ? 'Loading…'
                                      : 'Formulary alternatives'}
                                  </Button>
                                  {formularyDraftLineKey === row.key && formularyAlts && formularyAlts.length > 0 && (
                                    <Box mt={0.5} display="flex" flexWrap="wrap" gap={0.5}>
                                      {formularyAlts.map((d) => (
                                        <Chip
                                          key={d.id}
                                          size="small"
                                          label={getDrugLabel(d)}
                                          onClick={() => applyFormularyDrugToLine(row.key, d)}
                                          sx={{ cursor: 'pointer' }}
                                        />
                                      ))}
                                    </Box>
                                  )}
                                </>
                              )}
                            </TableCell>
                            <TableCell>
                              <TextField
                                size="medium"
                                fullWidth
                                value={row.batchNumber}
                                onChange={(e) => updateDraftLine(row.key, { batchNumber: e.target.value })}
                                placeholder="—"
                                sx={cartFieldSx}
                              />
                            </TableCell>
                            <TableCell>
                              <TextField
                                size="medium"
                                type="number"
                                fullWidth
                                inputProps={{ min: 1, step: 1 }}
                                value={row.quantityDispensed}
                                onChange={(e) =>
                                  updateDraftLine(row.key, {
                                    quantityDispensed: Math.max(1, Number(e.target.value) || 1),
                                  })
                                }
                                sx={cartFieldSx}
                              />
                            </TableCell>
                            <TableCell>
                              <Typography
                                variant="body2"
                                className={stockDisplayClass(onHand, row.quantityDispensed)}
                              >
                                {pharmacyStockLoading ? '…' : onHand}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {pharmacyStockLoading ? '—' : formatExpiryShort(exp)}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <TextField
                                size="medium"
                                type="number"
                                fullWidth
                                inputProps={{ min: 0, step: 0.01 }}
                                value={row.unitPrice}
                                onChange={(e) =>
                                  updateDraftLine(row.key, { unitPrice: Number(e.target.value) || 0 })
                                }
                                sx={cartFieldSx}
                              />
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">{lineTotal.toFixed(2)}</Typography>
                            </TableCell>
                            <TableCell align="right">
                              <IconButton
                                size="small"
                                aria-label="Remove line"
                                onClick={() => removeDraftLine(row.key)}
                              >
                                <DeleteOutlineIcon fontSize="small" />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                      {draftDispenseLines.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={8}>
                            <Typography variant="body2" color="text.secondary">
                              Search and select a drug to add rows. Quantity can exceed recorded stock — the server
                              records a stock override when configured.
                            </Typography>
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Card>

              <TextField
                fullWidth
                multiline
                minRows={2}
                sx={{ mb: 2 }}
                label="Stock / ledger override reason (bulk)"
                placeholder="Applied to any cart line where quantity exceeds on-hand (required by policy on many sites)"
                value={bulkStockOverrideReason}
                onChange={(e) => setBulkStockOverrideReason(e.target.value)}
                inputProps={{ maxLength: 2000 }}
              />

              <Card variant="outlined" sx={{ mb: 2, p: 2, borderRadius: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Billing (estimate)
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
                  <TextField
                    size="small"
                    type="number"
                    label="Discount"
                    inputProps={{ min: 0, step: 0.01 }}
                    value={billingDiscount}
                    onChange={(e) => setBillingDiscount(Number(e.target.value) || 0)}
                  />
                  <TextField
                    size="small"
                    type="number"
                    label="Paid"
                    inputProps={{ min: 0, step: 0.01 }}
                    value={billingPaid}
                    onChange={(e) => setBillingPaid(Number(e.target.value) || 0)}
                  />
                  <TextField size="small" label="Subtotal" value={cartSubtotal.toFixed(2)} InputProps={{ readOnly: true }} />
                  <TextField
                    size="small"
                    label="Total after discount"
                    value={billingTotalAfterDiscount.toFixed(2)}
                    InputProps={{ readOnly: true }}
                  />
                  <TextField size="small" label="Due" value={billingDue.toFixed(2)} InputProps={{ readOnly: true }} />
                </Box>
              </Card>

              <Box display="flex" flexWrap="wrap" gap={1} sx={{ mb: 3 }}>
                <Button
                  variant="contained"
                  color="success"
                  size="large"
                  startIcon={<PrintIcon />}
                  onClick={handleDispenseCartAndPrint}
                >
                  Dispense + print
                </Button>
                <Button variant="outlined" size="large" startIcon={<SaveIcon />} onClick={handleSaveDraftClicked}>
                  Save draft
                </Button>
                <Button variant="outlined" color="error" size="large" onClick={handleCancelCart}>
                  Cancel
                </Button>
              </Box>

              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Recorded lines
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Drug</TableCell>
                      <TableCell>Batch</TableCell>
                      <TableCell>Qty Prescribed</TableCell>
                      <TableCell>Qty Dispensed</TableCell>
                      <TableCell>Remaining</TableCell>
                      <TableCell>Qty Returned</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedOrder.lines.map((line) => (
                      <TableRow key={line.id} hover>
                        <TableCell>
                          <Typography variant="body2">{line.drugGenericName}</Typography>
                          {line.drugBrandName && (
                            <Typography variant="caption" color="text.secondary">
                              {line.drugBrandName}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>{line.batchNumber || '-'}</TableCell>
                        <TableCell>{line.quantityPrescribed ?? '-'}</TableCell>
                        <TableCell>{line.quantityDispensed}</TableCell>
                        <TableCell>
                          {line.remainingQuantity != null ? line.remainingQuantity : '—'}
                        </TableCell>
                        <TableCell>{line.quantityReturned}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={line.status}
                            color={lineStatusChipColor(line.status)}
                          />
                          {line.overrideReasonCode && (
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              display="block"
                              sx={{ mt: 0.5, maxWidth: 280 }}
                            >
                              Override: {line.overrideReasonCode}
                            </Typography>
                          )}
                          {enableFormularyFeature && line.formularyOverrideReason && (
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              display="block"
                              sx={{ mt: 0.5, maxWidth: 280 }}
                            >
                              Formulary: {line.formularyOverrideReason}
                            </Typography>
                          )}
                          {line.clinicalSafetyOverrideReason && (
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              display="block"
                              sx={{ mt: 0.5, maxWidth: 280 }}
                            >
                              Clinical safety override: {line.clinicalSafetyOverrideReason}
                            </Typography>
                          )}
                          {line.witnessUserId && (
                            <Typography variant="caption" color="text.secondary" display="block">
                              Witness: {line.witnessUserId}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            variant="text"
                            onClick={() => handleOpenReturnDialog(selectedOrder, line.id)}
                          >
                            Return
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                    {selectedOrder.lines.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={8} align="center">
                          <Typography variant="body2" color="text.secondary">
                            No recorded lines yet — use the cart above to dispense.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {tab === 'reports' && selectedPharmacyId && (
            <Box mt={2}>
              <Box display="flex" flexWrap="wrap" gap={2} alignItems="center" mb={2}>
                <TextField
                  label="From"
                  type="date"
                  size="small"
                  InputLabelProps={{ shrink: true }}
                  value={stockReportFrom}
                  onChange={(e) => setStockReportFrom(e.target.value)}
                />
                <TextField
                  label="To"
                  type="date"
                  size="small"
                  InputLabelProps={{ shrink: true }}
                  value={stockReportTo}
                  onChange={(e) => setStockReportTo(e.target.value)}
                />
              </Box>
              <Tabs
                value={reportSubTab}
                onChange={(_, v) => setReportSubTab(v)}
                sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
              >
                <Tab label="Stock overrides" value="overrides" />
                <Tab label="Sales summary" value="sales" />
                <Tab label="Consumption" value="consumption" />
                <Tab label="Controlled register" value="controlled" />
              </Tabs>

              {reportSubTab === 'overrides' && (
                <Box>
                  <Box mb={2}>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={handleLoadStockOverrides}
                      disabled={stockReportLoading}
                    >
                      Load report
                    </Button>
                  </Box>
                  {stockReportLoading ? (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress size={28} />
                    </Box>
                  ) : (
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Line</TableCell>
                            <TableCell>Order</TableCell>
                            <TableCell>Pharmacy</TableCell>
                            <TableCell>Drug</TableCell>
                            <TableCell>Qty</TableCell>
                            <TableCell>Reason</TableCell>
                            <TableCell>Dispensed</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {stockReportRows.map((r) => (
                            <TableRow key={r.dispenseLineId}>
                              <TableCell>{r.dispenseLineId}</TableCell>
                              <TableCell>{r.dispenseOrderId}</TableCell>
                              <TableCell>{r.pharmacyLocationName ?? r.pharmacyLocationId ?? '—'}</TableCell>
                              <TableCell>{r.genericName}</TableCell>
                              <TableCell>{r.quantityDispensed}</TableCell>
                              <TableCell>{r.overrideReasonCode || '—'}</TableCell>
                              <TableCell>{new Date(r.dispensedAt).toLocaleString()}</TableCell>
                            </TableRow>
                          ))}
                          {stockReportRows.length === 0 && (
                            <TableRow>
                              <TableCell colSpan={7} align="center">
                                <Typography variant="body2" color="text.secondary">
                                  Choose dates and load (FILLED_WITH_STOCK_OVERRIDE lines in range).
                                </Typography>
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </Box>
              )}

              {reportSubTab === 'sales' && (
                <Box>
                  <Box display="flex" flexWrap="wrap" gap={1} alignItems="center" mb={2}>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={handleLoadSalesSummary}
                      disabled={reportExtraLoading}
                    >
                      Load sales summary
                    </Button>
                    {salesSummary && salesSummary.revenueEstimateUnitPrice != null && (
                      <Typography variant="caption" color="text.secondary">
                        Revenue estimate uses default unit price {salesSummary.revenueEstimateUnitPrice} (config) ×
                        quantity — not billed ledger totals.
                      </Typography>
                    )}
                  </Box>
                  {reportExtraLoading ? (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress size={28} />
                    </Box>
                  ) : salesSummary ? (
                    <Box>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        Total qty issued: {salesSummary.totalQuantityIssued} · Drugs:{' '}
                        {salesSummary.distinctDrugCount}
                        {salesSummary.estimatedRevenueTotal != null && (
                          <> · Est. revenue: {salesSummary.estimatedRevenueTotal}</>
                        )}
                      </Typography>
                      <TableContainer>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Drug</TableCell>
                              <TableCell align="right">Qty issued</TableCell>
                              <TableCell align="right">Est. revenue</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {salesSummary.byDrug.map((r) => (
                              <TableRow key={r.drugId}>
                                <TableCell>
                                  {r.genericName}
                                  {r.brandName ? ` (${r.brandName})` : ''}
                                </TableCell>
                                <TableCell align="right">{r.totalQuantityIssued}</TableCell>
                                <TableCell align="right">
                                  {r.estimatedRevenue != null ? r.estimatedRevenue : '—'}
                                </TableCell>
                              </TableRow>
                            ))}
                            {salesSummary.byDrug.length === 0 && (
                              <TableRow>
                                <TableCell colSpan={3} align="center">
                                  No issues in range.
                                </TableCell>
                              </TableRow>
                            )}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      Choose dates and load sales summary (issue movements aggregated by drug).
                    </Typography>
                  )}
                </Box>
              )}

              {reportSubTab === 'consumption' && (
                <Box>
                  <Box display="flex" flexWrap="wrap" gap={1} mb={2}>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={handleLoadConsumption}
                      disabled={reportExtraLoading}
                    >
                      Load table
                    </Button>
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={handleDownloadConsumptionCsv}
                      disabled={reportExtraLoading}
                    >
                      Download CSV
                    </Button>
                  </Box>
                  {reportExtraLoading ? (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress size={28} />
                    </Box>
                  ) : (
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Drug</TableCell>
                            <TableCell align="right">Qty issued</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {consumptionRows.map((r) => (
                            <TableRow key={r.drugId}>
                              <TableCell>
                                {r.genericName}
                                {r.brandName ? ` (${r.brandName})` : ''}
                              </TableCell>
                              <TableCell align="right">{r.totalQuantityIssued}</TableCell>
                            </TableRow>
                          ))}
                          {consumptionRows.length === 0 && (
                            <TableRow>
                              <TableCell colSpan={2} align="center">
                                <Typography variant="body2" color="text.secondary">
                                  Choose dates, then Load table or Download CSV (L3 export).
                                </Typography>
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </Box>
              )}

              {reportSubTab === 'controlled' && (
                <Box>
                  <Box mb={2}>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={handleLoadControlledRegister}
                      disabled={reportExtraLoading}
                    >
                      Load register
                    </Button>
                  </Box>
                  {reportExtraLoading ? (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress size={28} />
                    </Box>
                  ) : (
                    <TableContainer>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Line</TableCell>
                            <TableCell>Order</TableCell>
                            <TableCell>Patient</TableCell>
                            <TableCell>Drug</TableCell>
                            <TableCell>Profile</TableCell>
                            <TableCell align="right">Qty</TableCell>
                            <TableCell>Witness</TableCell>
                            <TableCell>Dispensed</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {controlledRows.map((r) => (
                            <TableRow key={r.dispenseLineId}>
                              <TableCell>{r.dispenseLineId}</TableCell>
                              <TableCell>{r.dispenseOrderId}</TableCell>
                              <TableCell>{r.patientId ?? '—'}</TableCell>
                              <TableCell>
                                {r.genericName}
                                {r.brandName ? ` (${r.brandName})` : ''}
                              </TableCell>
                              <TableCell>{r.controlledProfileCode ?? '—'}</TableCell>
                              <TableCell align="right">{r.quantityDispensed}</TableCell>
                              <TableCell>{r.witnessUserId ?? '—'}</TableCell>
                              <TableCell>{new Date(r.dispensedAt).toLocaleString()}</TableCell>
                            </TableRow>
                          ))}
                          {controlledRows.length === 0 && (
                            <TableRow>
                              <TableCell colSpan={8} align="center">
                                <Typography variant="body2" color="text.secondary">
                                  Choose dates and load (controlled / CD-profile lines in range).
                                </Typography>
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </Box>
              )}
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Create Order Dialog */}
      <Dialog open={orderDialogOpen} onClose={() => setOrderDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Dispense Order</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <FormControl fullWidth>
              <InputLabel>Context</InputLabel>
              <Select
                label="Context"
                value={orderForm.contextType}
                onChange={(e) =>
                  setOrderForm({ ...orderForm, contextType: e.target.value as DispenseContextType })
                }
              >
                <MenuItem value="PATIENT_PRESCRIPTION">Patient prescription</MenuItem>
                <MenuItem value="WALK_IN">Walk-in</MenuItem>
                <MenuItem value="DEPARTMENT_ISSUE">Department issue</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Pharmacy</InputLabel>
              <Select
                label="Pharmacy"
                value={orderForm.pharmacyLocationId}
                onChange={(e) =>
                  setOrderForm({ ...orderForm, pharmacyLocationId: e.target.value as string })
                }
              >
                {pharmacies.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.name} ({p.type})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Autocomplete
              freeSolo
              options={patientOptions}
              getOptionLabel={(opt) => typeof opt === 'string' ? opt : opt.label}
              inputValue={patientInputValue}
              onInputChange={(_, val) => {
                setPatientInputValue(val);
                void searchPatients(val);
              }}
              onChange={(_, val) => {
                if (val && typeof val !== 'string') {
                  setOrderForm({ ...orderForm, patientId: val.id });
                  setPatientInputValue(val.label);
                } else {
                  setOrderForm({ ...orderForm, patientId: typeof val === 'string' ? val : '' });
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Patient *"
                  placeholder="Search by name or MRN"
                  helperText="Search patient by name or MRN, or paste UUID"
                />
              )}
            />
            <TextField
              label="Visit / encounter"
              placeholder="UUID or encounter number"
              helperText="Visit UUID as stored for dispensing, or hospital encounter number"
              value={orderForm.visitId}
              onChange={(e) => setOrderForm({ ...orderForm, visitId: e.target.value })}
            />
            <Autocomplete
              freeSolo
              options={prescriptionOptions}
              getOptionLabel={(opt) => typeof opt === 'string' ? opt : opt.label}
              inputValue={prescriptionInputValue}
              onInputChange={(_, val) => {
                setPrescriptionInputValue(val);
                void searchPrescriptions(val);
              }}
              onChange={(_, val) => {
                if (val && typeof val !== 'string') {
                  setOrderForm({ ...orderForm, prescriptionId: val.id });
                  setPrescriptionInputValue(val.label);
                } else {
                  setOrderForm({ ...orderForm, prescriptionId: typeof val === 'string' ? val : '' });
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Prescription (optional)"
                  placeholder="Search by Rx number or paste UUID"
                  helperText="Leave blank for walk-in OTC, or search prescription number"
                />
              )}
            />
            <TextField
              label="Department ID (for ward issues)"
              placeholder="UUID only"
              helperText="Must be a UUID if provided"
              value={orderForm.departmentId}
              onChange={(e) => setOrderForm({ ...orderForm, departmentId: e.target.value })}
            />
            <TextField
              sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}
              label="Paper prescription ref (optional)"
              value={orderForm.paperPrescriptionRef}
              onChange={(e) => setOrderForm({ ...orderForm, paperPrescriptionRef: e.target.value })}
              helperText="When no EHR prescription UUID is linked (e.g. walk-in with paper Rx)"
            />
            <TextField
              label="Prescription image attachment id (optional)"
              placeholder="UUID"
              value={orderForm.prescriptionImageAttachmentId}
              onChange={(e) =>
                setOrderForm({ ...orderForm, prescriptionImageAttachmentId: e.target.value })
              }
              helperText="Document store attachment UUID for scanned Rx image (Phase P3 WS-E)"
            />
            <FormControl fullWidth>
              <InputLabel>External validation</InputLabel>
              <Select
                label="External validation"
                value={orderForm.externalValidationStatus}
                onChange={(e) =>
                  setOrderForm({ ...orderForm, externalValidationStatus: e.target.value as string })
                }
              >
                <MenuItem value="NOT_REQUIRED">Not required</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="VERIFIED">Verified</MenuItem>
                <MenuItem value="FAILED_SOFT">Failed (soft)</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOrderDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleCreateOrder}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={regionalDialogOpen} onClose={() => setRegionalDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Paper prescription and validation</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <TextField
              sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}
              label="Paper prescription ref"
              value={regionalForm.paperPrescriptionRef}
              onChange={(e) => setRegionalForm({ ...regionalForm, paperPrescriptionRef: e.target.value })}
            />
            <TextField
              sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}
              label="Prescription image attachment id"
              value={regionalForm.prescriptionImageAttachmentId}
              onChange={(e) =>
                setRegionalForm({ ...regionalForm, prescriptionImageAttachmentId: e.target.value })
              }
              disabled={regionalForm.clearPrescriptionImageAttachment}
              helperText="UUID from document store; leave blank if unused"
            />
            <FormControlLabel
              sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}
              control={
                <Checkbox
                  checked={regionalForm.clearPrescriptionImageAttachment}
                  onChange={(_, checked) =>
                    setRegionalForm({ ...regionalForm, clearPrescriptionImageAttachment: checked })
                  }
                />
              }
              label="Clear prescription image attachment"
            />
            <FormControl fullWidth sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}>
              <InputLabel>External validation</InputLabel>
              <Select
                label="External validation"
                value={regionalForm.externalValidationStatus}
                onChange={(e) =>
                  setRegionalForm({ ...regionalForm, externalValidationStatus: e.target.value })
                }
              >
                <MenuItem value="NOT_REQUIRED">Not required</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="VERIFIED">Verified</MenuItem>
                <MenuItem value="FAILED_SOFT">Failed (soft)</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRegionalDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveRegional}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={unfulfilledDialogOpen} onClose={() => setUnfulfilledDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record not filled (no stock issued)</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 1fr' }} gap={2} mt={1}>
            <FormControl fullWidth sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}>
              <Autocomplete
                options={drugOptions}
                loading={drugSearchLoading}
                value={selectedUnfulfilledDrug}
                inputValue={unfulfilledDrugQuery}
                onInputChange={(_, value) => {
                  setUnfulfilledDrugQuery(value);
                  searchDrugOptions(value);
                }}
                onChange={(_, value) => {
                  setSelectedUnfulfilledDrug(value);
                  setUnfulfilledForm({ ...unfulfilledForm, drugId: value?.id || '' });
                }}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                getOptionLabel={getDrugOptionLabel}
                renderOption={renderDrugAutocompleteOption}
                renderInput={(params) => (
                  <TextField {...params} label="Drug" placeholder="Type at least 2 characters" required />
                )}
              />
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Outcome</InputLabel>
              <Select
                label="Outcome"
                value={unfulfilledForm.lineStatus}
                onChange={(e) =>
                  setUnfulfilledForm({
                    ...unfulfilledForm,
                    lineStatus: e.target.value as 'OUT_OF_STOCK' | 'REFUSED',
                  })
                }
              >
                <MenuItem value="OUT_OF_STOCK">Out of stock</MenuItem>
                <MenuItem value="REFUSED">Refused</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Prescription line ID (optional)"
              value={unfulfilledForm.prescriptionLineId}
              onChange={(e) => setUnfulfilledForm({ ...unfulfilledForm, prescriptionLineId: e.target.value })}
              helperText="EHR medication line UUID when linked to a prescription"
            />
            <TextField
              label="Qty prescribed (optional)"
              type="number"
              value={unfulfilledForm.quantityPrescribed ?? ''}
              onChange={(e) =>
                setUnfulfilledForm({
                  ...unfulfilledForm,
                  quantityPrescribed: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <TextField
              sx={{ gridColumn: { xs: '1', md: '1 / -1' } }}
              label="Reason (required)"
              value={unfulfilledForm.reasonCode}
              onChange={(e) => setUnfulfilledForm({ ...unfulfilledForm, reasonCode: e.target.value })}
              multiline
              minRows={2}
              required
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUnfulfilledDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleRecordUnfulfilled}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Billable items (billing preview — P1) */}
      <Dialog
        open={billableDialogOpen}
        onClose={() => setBillableDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Billable items</DialogTitle>
        <DialogContent dividers>
          {billableLoading ? (
            <Box display="flex" justifyContent="center" py={3}>
              <CircularProgress size={28} />
            </Box>
          ) : billableItems.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No billable lines (only lines with quantity dispensed greater than zero appear here). Pricing will
              appear when billing integration is enabled.
            </Typography>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Drug</TableCell>
                    <TableCell align="right">Qty dispensed</TableCell>
                    <TableCell>Line status</TableCell>
                    <TableCell>Override reason</TableCell>
                    <TableCell align="right">List price</TableCell>
                    <TableCell>Tax hint</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {billableItems.map((row) => (
                    <TableRow key={row.dispenseLineId}>
                      <TableCell>
                        <Typography variant="body2">{row.drugGenericName}</Typography>
                        {row.drugBrandName && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            {row.drugBrandName}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">{row.quantityDispensed}</TableCell>
                      <TableCell>
                        <Chip size="small" label={row.lineStatus} color={lineStatusChipColor(row.lineStatus)} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ maxWidth: 220 }}>
                          {row.overrideReasonCode ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {row.suggestedListPrice != null ? row.suggestedListPrice : '—'}
                      </TableCell>
                      <TableCell>{row.taxCodeHint ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBillableDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Returns Dialog */}
      <Dialog open={returnsDialogOpen} onClose={() => setReturnsDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Return</DialogTitle>
        <DialogContent dividers>
          <Box display="grid" gridTemplateColumns="1fr" gap={2} mt={1}>
            <TextField
              label="Quantity returned"
              type="number"
              value={returnQuantity}
              onChange={(e) => setReturnQuantity(Number(e.target.value) || 0)}
              required
            />
            <TextField
              label="Reason"
              value={returnReason}
              onChange={(e) => setReturnReason(e.target.value)}
              multiline
              rows={3}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReturnsDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleRecordReturn}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PharmacyDispensePage;

