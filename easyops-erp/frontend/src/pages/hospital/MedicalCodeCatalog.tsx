import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import hospitalService, { MedicalCode, MedicalCodeUpsertRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

type CatalogType = 'ICD10' | 'ICD11';

const ICD10_CODE_PATTERN = /^[A-TV-Z][0-9][0-9AB]([A-Z0-9]{0,4})?$/;
const ICD11_CODE_PATTERN = /^[0-9A-HJ-NP-Z]{4}(\.[0-9A-HJ-NP-Z]{1,4})*$/;

const validateMedicalCode = (catalogType: CatalogType, code: string): string | null => {
  const normalized = code.trim().toUpperCase();
  if (!normalized) {
    return 'Code is required.';
  }
  if (catalogType === 'ICD10') {
    if (normalized.length < 3 || normalized.length > 7) {
      return 'ICD-10 code must be 3 to 7 characters (e.g. A00, E11.9).';
    }
    if (!ICD10_CODE_PATTERN.test(normalized)) {
      return 'Invalid ICD-10 format. Use letter + digits (e.g. A00, J06.9, E11.65).';
    }
    return null;
  }
  if (normalized.length < 4) {
    return 'ICD-11 code must be at least 4 characters (e.g. 1A00).';
  }
  if (!ICD11_CODE_PATTERN.test(normalized)) {
    return 'Invalid ICD-11 format. Use alphanumeric codes (e.g. 1A00, 5A11.0).';
  }
  return null;
};

const MedicalCodeCatalogPage: React.FC = () => {
  const location = useLocation();
  const catalogType: CatalogType = location.pathname.endsWith('/icd11') ? 'ICD11' : 'ICD10';
  const isDedicatedRoute = location.pathname.endsWith('/icd10') || location.pathname.endsWith('/icd11');
  const [codes, setCodes] = useState<MedicalCode[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [showInactive, setShowInactive] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [editingCode, setEditingCode] = useState<MedicalCode | null>(null);
  const [formData, setFormData] = useState<MedicalCodeUpsertRequest>({
    code: '',
    description: '',
    category: '',
    chapter: '',
    isValid: true,
  });

  const loadCatalog = async () => {
    try {
      setLoading(true);
      const res = catalogType === 'ICD10'
        ? await hospitalService.getIcd10Catalog(searchTerm, page, size, showInactive)
        : await hospitalService.getIcd11Catalog(searchTerm, page, size, showInactive);
      setCodes(res.data.items);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load medical code catalog'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPage(0);
  }, [location.pathname]);

  useEffect(() => {
    loadCatalog();
  }, [catalogType, page, size, showInactive]);

  const handleSearch = () => {
    setPage(0);
    loadCatalog();
  };

  const resetForm = () => {
    setEditingCode(null);
    setFormError(null);
    setFormData({
      code: '',
      description: '',
      category: '',
      chapter: '',
      isValid: true,
    });
  };

  const handleEdit = (item: MedicalCode) => {
    setEditingCode(item);
    setFormError(null);
    setFormData({
      code: item.code,
      description: item.description,
      category: item.category || '',
      chapter: item.chapter || '',
      isValid: item.isValid,
    });
    setShowForm(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const codeError = validateMedicalCode(catalogType, formData.code);
    if (codeError) {
      setFormError(codeError);
      return;
    }
    if (!formData.description.trim()) {
      setFormError('Description is required.');
      return;
    }

    try {
      setFormError(null);
      const payload = {
        ...formData,
        code: formData.code.trim().toUpperCase(),
        description: formData.description.trim(),
      };
      if (catalogType === 'ICD10') {
        await hospitalService.upsertIcd10CatalogCode(payload);
      } else {
        await hospitalService.upsertIcd11CatalogCode(payload);
      }
      setShowForm(false);
      resetForm();
      loadCatalog();
    } catch (err: any) {
      setFormError(ehrApiErrorMessage(err, 'Failed to save code'));
    }
  };

  const handleDeactivate = async (code: string) => {
    if (!window.confirm(`Inactivate ${catalogType} code ${code}?`)) return;
    try {
      if (catalogType === 'ICD10') {
        await hospitalService.deactivateIcd10CatalogCode(code);
      } else {
        await hospitalService.deactivateIcd11CatalogCode(code);
      }
      loadCatalog();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to inactivate code'));
    }
  };

  const handleReactivate = async (item: MedicalCode) => {
    if (!window.confirm(`Reactivate ${catalogType} code ${item.code}?`)) return;
    try {
      const payload: MedicalCodeUpsertRequest = {
        code: item.code,
        description: item.description,
        category: item.category || '',
        chapter: item.chapter || '',
        isValid: true,
      };
      if (catalogType === 'ICD10') {
        await hospitalService.upsertIcd10CatalogCode(payload);
      } else {
        await hospitalService.upsertIcd11CatalogCode(payload);
      }
      loadCatalog();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to reactivate code'));
    }
  };

  const getStatusBadgeClass = (isValid: boolean) =>
    isValid ? 'status-badge status-active' : 'status-badge status-inactive';

  const pageStart = page * size;
  const showingFrom = totalElements === 0 ? 0 : pageStart + 1;
  const showingTo = Math.min(pageStart + codes.length, totalElements);
  const pageTitle = catalogType === 'ICD10' ? 'ICD-10 Codes' : 'ICD-11 Codes';

  return (
    <div className="hospital-page">
      <div className="page-header" style={{ marginBottom: 24 }}>
        <div>
          <h1>{pageTitle}</h1>
          <p>{catalogType === 'ICD10' ? 'View and manage ICD-10 diagnosis codes' : 'View and manage ICD-11 diagnosis codes'}</p>
        </div>
      </div>

      <div className="filters-section">
        <div className="filter-row">
          {!isDedicatedRoute && (
            <select value={catalogType} disabled>
              <option value="ICD10">ICD-10</option>
              <option value="ICD11">ICD-11</option>
            </select>
          )}
          <input
            className="search-input"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder={`Search ${catalogType} by code or description`}
          />
          <button className="btn-secondary" onClick={handleSearch}>Search</button>
          <button className="btn-primary" onClick={() => { resetForm(); setShowForm(true); }}>+ Add Code</button>
        </div>
        <div className="filter-row" style={{ marginTop: 8 }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={showInactive}
              onChange={(e) => {
                setShowInactive(e.target.checked);
                setPage(0);
              }}
            />
            <span>Show inactive codes</span>
          </label>
        </div>
      </div>

      {showForm && (
        <div className="form-container">
          <h3>{editingCode ? `Edit ${catalogType} Code` : `Add ${catalogType} Code`}</h3>
          {formError && <div className="error-message">{formError}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Code *</label>
                <input
                  required
                  maxLength={20}
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                  disabled={!!editingCode}
                  placeholder={catalogType === 'ICD10' ? 'e.g. E11.9' : 'e.g. 1A00'}
                />
                <small style={{ color: '#6b7280' }}>
                  {catalogType === 'ICD10'
                    ? 'Format: letter + 2 digits, optional suffix (3–7 chars)'
                    : 'Format: alphanumeric ICD-11 code (e.g. 1A00)'}
                </small>
              </div>
              <div className="form-group">
                <label>Description *</label>
                <input
                  required
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Category</label>
                <input
                  value={formData.category || ''}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Chapter</label>
                <input
                  value={formData.chapter || ''}
                  onChange={(e) => setFormData({ ...formData, chapter: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Status *</label>
                <select
                  value={formData.isValid ? 'ACTIVE' : 'INACTIVE'}
                  onChange={(e) => setFormData({ ...formData, isValid: e.target.value === 'ACTIVE' })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); resetForm(); }}>Cancel</button>
              <button type="submit" className="btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Description</th>
              <th>Category</th>
              <th>Chapter</th>
              <th>Status</th>
              <th className="col-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6}>Loading...</td></tr>
            ) : codes.length === 0 ? (
              <tr><td colSpan={6}>No codes found</td></tr>
            ) : (
              codes.map((item) => (
                <tr key={item.code} style={!item.isValid ? { opacity: 0.75 } : undefined}>
                  <td><strong>{item.code}</strong></td>
                  <td>{item.description}</td>
                  <td>{item.category || '-'}</td>
                  <td>{item.chapter || '-'}</td>
                  <td>
                    <span className={getStatusBadgeClass(item.isValid)}>
                      {item.isValid ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(item)}>Edit</button>
                      {item.isValid ? (
                        <button className="btn-link btn-danger" onClick={() => handleDeactivate(item.code)}>Inactivate</button>
                      ) : (
                        <button className="btn-link" onClick={() => handleReactivate(item)}>Reactivate</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalElements > 0 && (
        <div
          className="table-footer"
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            marginTop: 12,
          }}
        >
          <p style={{ margin: 0 }}>
            Showing {showingFrom}–{showingTo} of {totalElements} code(s)
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px' }}>
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '14px' }}>
              Per page
              <select
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
                className="filter-select"
                style={{ minWidth: '72px' }}
              >
                {[10, 25, 50, 100].map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <button className="btn-secondary" disabled={page <= 0} onClick={() => setPage((prev) => prev - 1)}>Previous</button>
              <span style={{ fontSize: '14px', color: 'var(--text-secondary, #6b7280)' }}>
                Page {page + 1} / {Math.max(totalPages, 1)}
              </span>
              <button className="btn-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((prev) => prev + 1)}>Next</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MedicalCodeCatalogPage;
