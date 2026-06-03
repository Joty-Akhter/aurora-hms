import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import hospitalService, {
  Patient,
  PatientDocument,
  PatientDocumentType,
} from '../../services/hospitalService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

// ── Constants ─────────────────────────────────────────────────────────────────

const DOCUMENT_TYPES: { value: PatientDocumentType; label: string; icon: string }[] = [
  { value: 'PATHOLOGY_REPORT',  label: 'Pathology Report',   icon: '🔬' },
  { value: 'RADIOLOGY_REPORT',  label: 'Radiology Report',   icon: '🩻' },
  { value: 'LAB_REPORT',        label: 'Lab Report',         icon: '🧪' },
  { value: 'CLINICAL_PHOTO',    label: 'Clinical Photo',     icon: '📷' },
  { value: 'SURGICAL_REPORT',   label: 'Surgical Report',    icon: '🏥' },
  { value: 'PRESCRIPTION',      label: 'Prescription',       icon: '💊' },
  { value: 'REFERRAL_LETTER',   label: 'Referral Letter',    icon: '📨' },
  { value: 'DISCHARGE_SUMMARY', label: 'Discharge Summary',  icon: '📋' },
  { value: 'CONSENT_FORM',      label: 'Consent Form',       icon: '✍️' },
  { value: 'INSURANCE_DOCUMENT',label: 'Insurance Document', icon: '🛡️' },
  { value: 'VITAL_RECORDS',     label: 'Vital Records',      icon: '📜' },
  { value: 'IDENTITY_DOCUMENT', label: 'Identity Document',  icon: '🪪' },
  { value: 'EXTERNAL_RECORD',   label: 'External Record',    icon: '🏛️' },
  { value: 'ADVANCE_DIRECTIVE', label: 'Advance Directive',  icon: '📝' },
  { value: 'OTHER',             label: 'Other',              icon: '📎' },
];

const TYPE_MAP = Object.fromEntries(DOCUMENT_TYPES.map(t => [t.value, t]));

function formatBytes(bytes?: number): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(dt?: string): string {
  if (!dt) return '—';
  return new Date(dt).toLocaleString();
}

// ── Component ─────────────────────────────────────────────────────────────────

const PatientDocuments: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [patient, setPatient] = useState<Patient | null>(null);
  const [documents, setDocuments] = useState<PatientDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter
  const [filterType, setFilterType] = useState<PatientDocumentType | ''>('');

  // Upload dialog state
  const [showUpload, setShowUpload] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [uploadForm, setUploadForm] = useState({
    documentType: 'OTHER' as PatientDocumentType,
    title: '',
    description: '',
    sourceFacility: '',
    documentDate: '',
    isConfidential: false,
  });

  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id]);

  const loadData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      const [patientRes, docsRes] = await Promise.all([
        hospitalService.getPatient(id),
        hospitalService.getPatientDocuments(id),
      ]);
      setPatient(patientRes.data);
      setDocuments(docsRes.data);
    } catch (err: any) {
      setError(ehrApiErrorMessage(err, 'Failed to load documents'));
    } finally {
      setLoading(false);
    }
  };

  const loadDocuments = async () => {
    if (!id) return;
    try {
      const res = filterType
        ? await hospitalService.getPatientDocuments(id, filterType)
        : await hospitalService.getPatientDocuments(id);
      setDocuments(res.data);
    } catch (err: any) {
      setError(ehrApiErrorMessage(err, 'Failed to reload documents'));
    }
  };

  // ── File selection & drag-drop ─────────────────────────────────────────────

  const handleFileSelect = (file: File) => {
    setSelectedFile(file);
    if (!uploadForm.title) {
      setUploadForm(f => ({ ...f, title: file.name }));
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelect(file);
  };

  // ── Upload ────────────────────────────────────────────────────────────────

  const handleUpload = async () => {
    if (!id || !selectedFile) return;
    setUploading(true);
    setUploadError(null);
    try {
      await hospitalService.uploadPatientDocument(
        id,
        selectedFile,
        uploadForm.documentType,
        {
          title: uploadForm.title || selectedFile.name,
          description: uploadForm.description || undefined,
          sourceFacility: uploadForm.sourceFacility || undefined,
          documentDate: uploadForm.documentDate || undefined,
          isConfidential: uploadForm.isConfidential,
        }
      );
      setShowUpload(false);
      setSelectedFile(null);
      setUploadForm({
        documentType: 'OTHER',
        title: '',
        description: '',
        sourceFacility: '',
        documentDate: '',
        isConfidential: false,
      });
      await loadDocuments();
    } catch (err: any) {
      setUploadError(ehrApiErrorMessage(err, 'Upload failed'));
    } finally {
      setUploading(false);
    }
  };

  // ── Download ──────────────────────────────────────────────────────────────

  const handleDownload = async (doc: PatientDocument) => {
    try {
      const res = await hospitalService.downloadPatientDocument(doc.documentId);
      const url = window.URL.createObjectURL(new Blob([res.data as BlobPart]));
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFileName || doc.fileName;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      alert('Download failed: ' + ehrApiErrorMessage(err, 'Download failed'));
    }
  };

  // ── Delete ────────────────────────────────────────────────────────────────

  const handleDelete = async (docId: string) => {
    if (!window.confirm('Delete this document? This cannot be undone.')) return;
    try {
      await hospitalService.deletePatientDocument(docId);
      setDocuments(prev => prev.filter(d => d.documentId !== docId));
    } catch (err: any) {
      alert('Delete failed: ' + ehrApiErrorMessage(err, 'Delete failed'));
    }
  };

  // ── Filtered list ─────────────────────────────────────────────────────────

  const filtered = filterType ? documents.filter(d => d.documentType === filterType) : documents;

  // ── Render ─────────────────────────────────────────────────────────────────

  if (loading) return <div className="loading">Loading documents...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1>Patient Documents</h1>
          {patient && (
            <p className="page-subtitle">
              {patient.fullName} &middot; MRN: {patient.mrn}
            </p>
          )}
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button className="btn-primary" onClick={() => setShowUpload(true)}>
            + Upload Document
          </button>
        </div>
      </div>

      {/* Filter bar */}
      <div className="filter-bar" style={{ marginBottom: '16px', display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
        <label style={{ fontWeight: 600 }}>Filter by type:</label>
        <select
          value={filterType}
          onChange={e => setFilterType(e.target.value as PatientDocumentType | '')}
          className="form-control"
          style={{ width: '220px' }}
        >
          <option value="">All Types</option>
          {DOCUMENT_TYPES.map(t => (
            <option key={t.value} value={t.value}>{t.icon} {t.label}</option>
          ))}
        </select>
        <span style={{ color: '#666', fontSize: '14px' }}>{filtered.length} document(s)</span>
      </div>

      {/* Document list */}
      {filtered.length === 0 ? (
        <div className="empty-state">
          <p>No documents found.{filterType ? ' Try clearing the filter.' : ' Upload the first document using the button above.'}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Title</th>
                <th>File</th>
                <th>Size</th>
                <th>Source</th>
                <th>Uploaded</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(doc => {
                const typeInfo = TYPE_MAP[doc.documentType] || { icon: '📎', label: doc.documentType };
                return (
                  <tr key={doc.documentId}>
                    <td>
                      <span title={typeInfo.label}>{typeInfo.icon}</span>{' '}
                      <span style={{ fontSize: '13px' }}>{typeInfo.label}</span>
                      {doc.isConfidential && (
                        <span className="badge badge-warning" style={{ marginLeft: '6px', fontSize: '11px' }}>
                          Confidential
                        </span>
                      )}
                    </td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{doc.title}</div>
                      {doc.description && (
                        <div style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>{doc.description}</div>
                      )}
                    </td>
                    <td style={{ fontSize: '13px', color: '#555' }}>
                      {doc.originalFileName || doc.fileName}
                    </td>
                    <td style={{ fontSize: '13px' }}>{formatBytes(doc.fileSize)}</td>
                    <td style={{ fontSize: '13px' }}>{doc.sourceFacility || '—'}</td>
                    <td style={{ fontSize: '13px' }}>{formatDate(doc.uploadedDate)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          className="btn-link"
                          onClick={() => handleDownload(doc)}
                          title="Download"
                        >
                          Download
                        </button>
                        <button
                          className="btn-link"
                          style={{ color: '#c0392b' }}
                          onClick={() => handleDelete(doc.documentId)}
                          title="Delete"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* ── Upload Modal ──────────────────────────────────────────────────── */}
      {showUpload && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
          onClick={() => !uploading && setShowUpload(false)}
        >
          <div className="modal-content" style={{ maxWidth: '560px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Upload Document</h3>
              <button className="modal-close" onClick={() => !uploading && setShowUpload(false)}>×</button>
            </div>
            <div className="modal-body">
              {uploadError && <div className="error-message" style={{ marginBottom: '12px' }}>{uploadError}</div>}

              {/* Drop zone */}
              <div
                className={`drop-zone${dragOver ? ' drag-over' : ''}${selectedFile ? ' has-file' : ''}`}
                style={{
                  border: `2px dashed ${dragOver ? '#3498db' : selectedFile ? '#27ae60' : '#ccc'}`,
                  borderRadius: '8px',
                  padding: '28px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  marginBottom: '16px',
                  background: dragOver ? '#ebf5fb' : selectedFile ? '#eafaf1' : '#fafafa',
                  transition: 'all 0.2s',
                }}
                onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  style={{ display: 'none' }}
                  accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.webp,.docx,.doc,.xlsx,.xls,.csv,.txt,.mp3,.wav,.mp4,.mov"
                  onChange={e => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
                />
                {selectedFile ? (
                  <div>
                    <div style={{ fontSize: '24px', marginBottom: '6px' }}>✅</div>
                    <div style={{ fontWeight: 600 }}>{selectedFile.name}</div>
                    <div style={{ fontSize: '13px', color: '#666' }}>{formatBytes(selectedFile.size)}</div>
                    <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>Click or drop to replace</div>
                  </div>
                ) : (
                  <div>
                    <div style={{ fontSize: '32px', marginBottom: '8px' }}>📁</div>
                    <div style={{ fontWeight: 500 }}>Drag & drop a file here, or click to browse</div>
                    <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>
                      PDF, Images (JPEG/PNG/TIFF), DOCX, CSV, Audio, Video — max 50 MB
                    </div>
                  </div>
                )}
              </div>

              {/* Form fields */}
              <div className="form-group">
                <label className="form-label">Document Type *</label>
                <select
                  className="form-control"
                  value={uploadForm.documentType}
                  onChange={e => setUploadForm(f => ({ ...f, documentType: e.target.value as PatientDocumentType }))}
                >
                  {DOCUMENT_TYPES.map(t => (
                    <option key={t.value} value={t.value}>{t.icon} {t.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Title</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. CBC Report – March 2026"
                  value={uploadForm.title}
                  onChange={e => setUploadForm(f => ({ ...f, title: e.target.value }))}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-control"
                  rows={2}
                  placeholder="Optional notes about this document"
                  value={uploadForm.description}
                  onChange={e => setUploadForm(f => ({ ...f, description: e.target.value }))}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div className="form-group">
                  <label className="form-label">Source Facility</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. City Lab, Radiology Dept"
                    value={uploadForm.sourceFacility}
                    onChange={e => setUploadForm(f => ({ ...f, sourceFacility: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Document Date</label>
                  <input
                    type="date"
                    className="form-control"
                    value={uploadForm.documentDate}
                    onChange={e => setUploadForm(f => ({ ...f, documentDate: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <input
                  type="checkbox"
                  id="isConfidential"
                  checked={uploadForm.isConfidential}
                  onChange={e => setUploadForm(f => ({ ...f, isConfidential: e.target.checked }))}
                />
                <label htmlFor="isConfidential" style={{ margin: 0, cursor: 'pointer' }}>
                  Mark as confidential
                </label>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowUpload(false)} disabled={uploading}>
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={handleUpload}
                disabled={!selectedFile || uploading}
              >
                {uploading ? 'Uploading...' : 'Upload'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PatientDocuments;
