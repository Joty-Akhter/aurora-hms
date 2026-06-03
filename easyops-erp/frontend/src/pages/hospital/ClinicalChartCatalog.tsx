import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Grid,
} from '@mui/material';
import hospitalService, { ClinicalChartItemRow } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ClinicalChartCatalogPage: React.FC = () => {
  const [items, setItems] = useState<ClinicalChartItemRow[]>([]);
  /** Typing in the box does not hit the API until Search (avoids churn and duplicate loads vs pagination). */
  const [searchInput, setSearchInput] = useState('');
  const [appliedSearchTerm, setAppliedSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(25);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [investigationsOnly, setInvestigationsOnly] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [formValues, setFormValues] = useState({
    pcode: '',
    description: '',
    charge: '',
    deptName: '',
    subDeptName: '',
    subSubDeptName: '',
    reportGroupName: '',
    outTest: '0',
    statusLegacy: '1',
  });
  /** Bumped on every Search click so repeated searches reload even when filter text is unchanged. */
  const [searchNonce, setSearchNonce] = useState(0);
  const loadSeqRef = useRef(0);

  const load = useCallback(
    async (forPage?: number) => {
      const seq = ++loadSeqRef.current;
      const p = forPage !== undefined ? forPage : page;
      try {
        setLoading(true);
        const res = await hospitalService.getClinicalChartCatalog(appliedSearchTerm, p, size, investigationsOnly);
        if (seq !== loadSeqRef.current) return;
        const body = res.data;
        if (body == null) {
          setItems([]);
          setTotalPages(0);
          setTotalElements(0);
          return;
        }
        const tpRaw = body.totalPages;
        const teRaw = body.totalElements;
        const tp = typeof tpRaw === 'number' && Number.isFinite(tpRaw) ? Math.max(0, tpRaw) : 0;
        const te = typeof teRaw === 'number' && Number.isFinite(teRaw) ? Math.max(0, teRaw) : 0;
        const clampedPage = tp > 0 ? Math.min(p, tp - 1) : 0;
        if (clampedPage !== p) {
          setPage(clampedPage);
          return;
        }
        setItems(Array.isArray(body.items) ? body.items : []);
        setTotalPages(tp);
        setTotalElements(te);
      } catch (err: unknown) {
        if (seq !== loadSeqRef.current) return;
        alert(ehrApiErrorMessage(err, 'Failed to load clinical chart catalog'));
        setItems([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        if (seq === loadSeqRef.current) setLoading(false);
      }
    },
    [page, appliedSearchTerm, size, investigationsOnly],
  );

  useEffect(() => {
    void load();
  }, [load, searchNonce]);

  const openAddDialog = () => {
    setEditingItemId(null);
    setFormValues({
      pcode: '',
      description: '',
      charge: '',
      deptName: '',
      subDeptName: '',
      subSubDeptName: '',
      reportGroupName: '',
      outTest: '0',
      statusLegacy: '1',
    });
    setDialogOpen(true);
  };

  const openEditDialog = async (itemId: string) => {
    setLoading(true);
    try {
      const response = await hospitalService.getClinicalChartItem(itemId);
      const item = response.data;
      setEditingItemId(itemId);
      setFormValues({
        pcode: item.pcode ?? '',
        description: item.description ?? '',
        charge: item.charge != null ? String(item.charge) : '',
        deptName: item.deptName ?? '',
        subDeptName: item.subDeptName ?? '',
        subSubDeptName: item.subSubDeptName ?? '',
        reportGroupName: item.reportGroupName ?? '',
        outTest: item.outTest != null ? String(item.outTest) : '0',
        statusLegacy: item.statusLegacy != null ? String(item.statusLegacy) : '1',
      });
      setDialogOpen(true);
    } catch (err: unknown) {
      alert(ehrApiErrorMessage(err, 'Failed to load clinical chart row')); 
    } finally {
      setLoading(false);
    }
  };

  const closeDialog = () => {
    setDialogOpen(false);
  };

  const saveItem = async () => {
    const payload = {
      pcode: formValues.pcode || undefined,
      description: formValues.description,
      charge: formValues.charge ? Number(formValues.charge) : undefined,
      deptName: formValues.deptName || undefined,
      subDeptName: formValues.subDeptName || undefined,
      subSubDeptName: formValues.subSubDeptName || undefined,
      reportGroupName: formValues.reportGroupName || undefined,
      outTest: Number(formValues.outTest),
      statusLegacy: Number(formValues.statusLegacy),
    };

    setLoading(true);
    try {
      if (editingItemId) {
        await hospitalService.updateClinicalChartItem(editingItemId, payload);
      } else {
        await hospitalService.createClinicalChartItem(payload);
      }
      closeDialog();
      void load();
    } catch (err: unknown) {
      alert(ehrApiErrorMessage(err, 'Failed to save clinical chart row'));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setAppliedSearchTerm(searchInput.trim());
    setPage(0);
    setSearchNonce((n) => n + 1);
  };

  return (
    <div className="hospital-page">
      <div className="page-header" style={{ marginBottom: 24 }}>
        <div>
          <h1>Clinical Chart</h1>
          <p style={{ color: 'var(--text-secondary)', margin: '8px 0 0', maxWidth: 720 }}>
            Legacy charge and investigation master imported from your previous system. Rows here align with billing reference data.
            Prescription/template investigation suggestions use active rows whose sub-department is Diagnostic, Radiology, or LabTest only.
          </p>
        </div>
        <button type="button" className="btn-primary" onClick={openAddDialog} style={{ height: 36, alignSelf: 'center' }}>
          Add clinical chart row
        </button>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16, alignItems: 'center' }}>
        <input
          type="search"
          aria-label="Clinical chart search"
          placeholder="Description, P-code, dept, sub-sub-dept, report group — press Search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleSearch();
          }}
          style={{ flex: '1 1 240px', padding: '8px 12px', fontSize: 14 }}
        />
        <button type="button" className="btn-primary" aria-label="Apply clinical chart search" onClick={handleSearch} disabled={loading}>
          Search
        </button>
        <label style={{ display: 'inline-flex', alignItems: 'center', gap: 8, fontSize: 14, cursor: 'pointer', userSelect: 'none' }}>
          <input
            type="checkbox"
            aria-label="Show only investigations rows (Diagnostic, Radiology, or LabTest sub-department)"
            checked={investigationsOnly}
            onChange={(e) => {
              setInvestigationsOnly(e.target.checked);
              setPage(0);
            }}
          />
          Investigations only (Sub-dept: Diagnostic, Radiology, LabTest)
        </label>
      </div>

      {investigationsOnly ? (
        <p style={{ fontSize: 12, color: '#0369a1', marginBottom: 8, marginTop: 0 }}>
          View limited to the same investigation slice used for prescription/test autosuggest (active rows, those three sub-departments).
        </p>
      ) : null}

      {appliedSearchTerm ? (
        <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>
          Active filter: <strong>{appliedSearchTerm}</strong>
          {' · '}
          <button
            type="button"
            className="btn-secondary"
            aria-label="Clear clinical chart text filter"
            style={{ fontSize: 12, padding: '2px 8px' }}
            onClick={() => {
              setSearchInput('');
              setAppliedSearchTerm('');
              setPage(0);
              setSearchNonce((n) => n + 1);
            }}
          >
            Clear filter
          </button>
        </p>
      ) : null}

      {loading && items.length === 0 ? (
        <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
      ) : (
        <>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 12 }} aria-live="polite">
            {loading ? (
              <span style={{ marginRight: 8, fontStyle: 'italic' }}>Refreshing…</span>
            ) : null}
            {totalElements.toLocaleString()} item{totalElements === 1 ? '' : 's'}
            {totalPages > 1 ? ` · Page ${page + 1} of ${totalPages}` : ''}
          </p>
          <div
            style={{
              overflowX: 'auto',
              opacity: loading ? 0.55 : 1,
              pointerEvents: loading ? 'none' : 'auto',
              transition: 'opacity 0.15s ease',
            }}
          >
            <table className="data-table" style={{ width: '100%', fontSize: 13 }}>
              <thead>
                <tr>
                  <th>P-code</th>
                  <th>Description</th>
                  <th>Charge</th>
                  <th>Department</th>
                  <th>Sub-dept</th>
                  <th>Sub-sub-dept</th>
                  <th>Report group</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {!loading && items.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ padding: 24, textAlign: 'center', color: 'var(--text-secondary)' }}>
                      {appliedSearchTerm || investigationsOnly
                        ? 'No rows match your current filters.'
                        : 'No active clinical chart rows to display.'}
                    </td>
                  </tr>
                ) : (
                  items.map((row) => (
                    <tr key={row.clinicalChartItemId ?? `legacy-${row.legacyRowId}`}>
                      <td>{row.pcode ?? '—'}</td>
                      <td>{row.description}</td>
                      <td>{row.charge != null ? Number(row.charge).toLocaleString(undefined, { maximumFractionDigits: 2 }) : '—'}</td>
                      <td>{row.deptName ?? '—'}</td>
                      <td>{row.subDeptName ?? '—'}</td>
                      <td>{row.subSubDeptName ?? '—'}</td>
                      <td>{row.reportGroupName ?? '—'}</td>
                      <td>
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => void openEditDialog(row.clinicalChartItemId)}
                          style={{ fontSize: 12, padding: '4px 8px' }}>
                          Edit
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 16, alignItems: 'center' }}>
              <button
                type="button"
                className="btn-secondary"
                aria-label="Previous page"
                disabled={loading || page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn-secondary"
                aria-label="Next page"
                disabled={loading || page >= totalPages - 1}
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      <Dialog open={dialogOpen} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editingItemId ? 'Edit clinical chart row' : 'Add clinical chart row'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="P-code"
                fullWidth
                size="small"
                value={formValues.pcode}
                onChange={(e) => setFormValues({ ...formValues, pcode: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Charge"
                fullWidth
                size="small"
                type="number"
                value={formValues.charge}
                onChange={(e) => setFormValues({ ...formValues, charge: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Description"
                fullWidth
                size="small"
                value={formValues.description}
                onChange={(e) => setFormValues({ ...formValues, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Dept Name"
                fullWidth
                size="small"
                value={formValues.deptName}
                onChange={(e) => setFormValues({ ...formValues, deptName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Sub Dept Name"
                fullWidth
                size="small"
                value={formValues.subDeptName}
                onChange={(e) => setFormValues({ ...formValues, subDeptName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Sub Sub Dept Name"
                fullWidth
                size="small"
                value={formValues.subSubDeptName}
                onChange={(e) => setFormValues({ ...formValues, subSubDeptName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Report Group Name"
                fullWidth
                size="small"
                value={formValues.reportGroupName}
                onChange={(e) => setFormValues({ ...formValues, reportGroupName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Out Test"
                fullWidth
                size="small"
                type="number"
                value={formValues.outTest}
                onChange={(e) => setFormValues({ ...formValues, outTest: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Status Legacy"
                fullWidth
                size="small"
                type="number"
                value={formValues.statusLegacy}
                onChange={(e) => setFormValues({ ...formValues, statusLegacy: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={loading}>Cancel</Button>
          <Button variant="contained" onClick={saveItem} disabled={loading}>Save</Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};

export default ClinicalChartCatalogPage;
