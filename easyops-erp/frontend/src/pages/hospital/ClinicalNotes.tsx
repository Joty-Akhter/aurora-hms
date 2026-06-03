import React, { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, {
  ClinicalNote,
  ClinicalNoteRequest,
  NoteSignRequest,
  NoteAmendmentRequest,
  NoteAttachment,
  NoteTemplate,
  Patient
} from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_NESTED_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const ClinicalNotesPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [notes, setNotes] = useState<ClinicalNote[]>([]);
  const [templates, setTemplates] = useState<NoteTemplate[]>([]);
  const [attachments, setAttachments] = useState<NoteAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<ClinicalNote | null>(null);
  const [viewing, setViewing] = useState<ClinicalNote | null>(null);
  const [filterType, setFilterType] = useState<string>('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [showSignDialog, setShowSignDialog] = useState(false);
  const [showAmendDialog, setShowAmendDialog] = useState(false);
  const [noteToSign, setNoteToSign] = useState<ClinicalNote | null>(null);
  const [noteToAmend, setNoteToAmend] = useState<ClinicalNote | null>(null);
  const [showLinkMedicationDialog, setShowLinkMedicationDialog] = useState(false);
  const [availableMedications, setAvailableMedications] = useState<any[]>([]);
  const [selectedMedicationId, setSelectedMedicationId] = useState<string>('');
  const [linkType, setLinkType] = useState<string>('DOCUMENTED');
  const [linkStrength, setLinkStrength] = useState<string>('MODERATE');

  // File attachment upload state (view modal + create/edit form)
  const attachFileInputRef = useRef<HTMLInputElement>(null);
  const formAttachFileInputRef = useRef<HTMLInputElement>(null);
  const [attachUploading, setAttachUploading] = useState(false);
  const [attachUploadError, setAttachUploadError] = useState<string | null>(null);
  const [formAttachDragOver, setFormAttachDragOver] = useState(false);
  const [formAttachments, setFormAttachments] = useState<NoteAttachment[]>([]);
  const [formNoteDetail, setFormNoteDetail] = useState<ClinicalNote | null>(null);

  const toClinicalNotePayload = (source: ClinicalNoteRequest): Partial<ClinicalNoteRequest> => ({
    patientId: source.patientId,
    encounterId: source.encounterId,
    templateId: source.templateId,
    noteType: source.noteType,
    noteDate: source.noteDate,
    noteTime: source.noteTime,
    noteStatus: source.noteStatus,
    subjective: source.subjective,
    objective: source.objective,
    assessment: source.assessment,
    plan: source.plan,
    chiefComplaint: source.chiefComplaint,
    reviewOfSystems: source.reviewOfSystems,
    physicalExamination: source.physicalExamination,
    clinicalImpression: source.clinicalImpression,
    treatmentPlan: source.treatmentPlan,
    followUpInstructions: source.followUpInstructions,
    notes: source.notes,
  });
  
  const [formData, setFormData] = useState<ClinicalNoteRequest>({
    patientId: id || '',
    noteType: 'SOAP',
    noteDate: new Date().toISOString().split('T')[0],
    noteTime: new Date().toTimeString().slice(0, 5),
    noteStatus: 'DRAFT',
    subjective: '',
    objective: '',
    assessment: '',
    plan: '',
    chiefComplaint: '',
    reviewOfSystems: '',
    physicalExamination: '',
    clinicalImpression: '',
    treatmentPlan: '',
    followUpInstructions: '',
    notes: '',
  });

  const [signData, setSignData] = useState<NoteSignRequest>({
    signatureMethod: 'ELECTRONIC',
    notes: '',
  });

  const [amendData, setAmendData] = useState<NoteAmendmentRequest>({
    amendmentReason: '',
    subjective: '',
    objective: '',
    assessment: '',
    plan: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadNotes();
      loadTemplates();
    }
  }, [id]);

  useEffect(() => {
    if (!showForm || !editing?.noteId) {
      setFormAttachments([]);
      setFormNoteDetail(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const nid = editing.noteId;
        const [noteRes, attRes] = await Promise.all([
          hospitalService.getClinicalNote(nid),
          hospitalService.getNoteAttachments(nid).catch(() => ({ data: [] as NoteAttachment[] })),
        ]);
        if (!cancelled) {
          setFormNoteDetail(noteRes.data);
          setFormAttachments(attRes.data);
        }
      } catch {
        if (!cancelled) {
          setFormNoteDetail(null);
          setFormAttachments([]);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [showForm, editing?.noteId]);

  useEffect(() => {
    if (showLinkMedicationDialog) {
      loadAvailableMedications();
    }
  }, [showLinkMedicationDialog]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getPatient(id);
      setPatient(response.data);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
    }
  };

  const loadNotes = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getCurrentVersionNotes(id);
      setNotes(response.data);
    } catch (err: any) {
      console.error('Failed to load clinical notes:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load clinical notes'));
    } finally {
      setLoading(false);
    }
  };

  const loadTemplates = async () => {
    try {
      const response = await hospitalService.getNoteTemplates();
      setTemplates(response.data.filter(t => t.isActive));
    } catch (err: any) {
      console.error('Failed to load templates:', err);
    }
  };

  const handleSearch = async () => {
    if (!id) return;
    if (!searchTerm.trim()) {
      await handleFilter(filterType);
      return;
    }
    try {
      setLoading(true);
      const response = await hospitalService.searchNotes(id, searchTerm);
      setNotes(response.data);
    } catch (err: any) {
      console.error('Failed to search notes:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search notes'));
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = async (type: string) => {
    if (!id) return;
    setFilterType(type);
    try {
      setLoading(true);
      let response;
      if (type) {
        response = await hospitalService.getNotesByType(id, type);
      } else {
        response = await hospitalService.getCurrentVersionNotes(id);
      }
      setNotes(response.data);
    } catch (err: any) {
      console.error('Failed to filter notes:', err);
      setError(ehrApiErrorMessage(err, 'Failed to filter notes'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      if (editing) {
        await hospitalService.updateClinicalNote(editing.noteId, toClinicalNotePayload(formData));
        setShowForm(false);
        setEditing(null);
        setFormNoteDetail(null);
        setFormAttachments([]);
        resetForm();
        await handleFilter(filterType);
      } else {
        await hospitalService.createClinicalNote({ ...toClinicalNotePayload(formData), patientId: id } as ClinicalNoteRequest);
        setShowForm(false);
        setEditing(null);
        setFormNoteDetail(null);
        setFormAttachments([]);
        resetForm();
        await handleFilter(filterType);
      }
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save clinical note'));
    }
  };

  const resetForm = () => {
    setFormData({
      patientId: id || '',
      noteType: 'SOAP',
      noteDate: new Date().toISOString().split('T')[0],
      noteTime: new Date().toTimeString().slice(0, 5),
      noteStatus: 'DRAFT',
      subjective: '',
      objective: '',
      assessment: '',
      plan: '',
      chiefComplaint: '',
      reviewOfSystems: '',
      physicalExamination: '',
      clinicalImpression: '',
      treatmentPlan: '',
      followUpInstructions: '',
      notes: '',
    });
  };

  const handleEdit = (note: ClinicalNote) => {
    if (note.noteStatus !== 'DRAFT') {
      alert('Only DRAFT notes can be edited. Please create an amendment for signed notes.');
      return;
    }
    setEditing(note);
    setFormData({
      patientId: note.patientId,
      encounterId: note.encounterId,
      noteType: note.noteType,
      noteDate: note.noteDate,
      noteTime: note.noteTime,
      noteStatus: note.noteStatus,
      subjective: note.subjective || '',
      objective: note.objective || '',
      assessment: note.assessment || '',
      plan: note.plan || '',
      chiefComplaint: note.chiefComplaint || '',
      reviewOfSystems: note.reviewOfSystems || '',
      physicalExamination: note.physicalExamination || '',
      clinicalImpression: note.clinicalImpression || '',
      treatmentPlan: note.treatmentPlan || '',
      followUpInstructions: note.followUpInstructions || '',
      notes: note.notes || '',
    });
    setShowForm(true);
  };

  const handleView = async (noteId: string) => {
    setAttachUploadError(null);
    try {
      const [noteResponse, attachmentsResponse] = await Promise.all([
        hospitalService.getClinicalNote(noteId),
        hospitalService.getNoteAttachments(noteId).catch(() => ({ data: [] }))
      ]);
      setViewing(noteResponse.data);
      setAttachments(attachmentsResponse.data);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load note'));
    }
  };

  const handleOpenAmend = async (note: ClinicalNote) => {
    setAttachUploadError(null);
    setSelectedMedicationId('');
    setLinkType('DOCUMENTED');
    setLinkStrength('MODERATE');
    setAmendData({
      amendmentReason: '',
      subjective: note.subjective || '',
      objective: note.objective || '',
      assessment: note.assessment || '',
      plan: note.plan || '',
    });
    try {
      const [noteRes, attRes] = await Promise.all([
        hospitalService.getClinicalNote(note.noteId),
        hospitalService.getNoteAttachments(note.noteId),
      ]);
      setNoteToAmend(noteRes.data);
      setAttachments(attRes.data);
    } catch {
      setNoteToAmend(note);
      setAttachments([]);
    }
    setShowAmendDialog(true);
  };

  const ensureDraftNoteForForm = async (): Promise<string | null> => {
    if (!id) return null;
    if (editing?.noteId) return editing.noteId;
    const res = await hospitalService.createClinicalNote({ ...toClinicalNotePayload(formData), patientId: id } as ClinicalNoteRequest);
    const created = res.data;
    setEditing(created);
    setFormData({
      patientId: created.patientId || id,
      encounterId: created.encounterId,
      noteType: created.noteType,
      noteDate: created.noteDate,
      noteTime: created.noteTime,
      noteStatus: created.noteStatus,
      subjective: created.subjective || '',
      objective: created.objective || '',
      assessment: created.assessment || '',
      plan: created.plan || '',
      chiefComplaint: created.chiefComplaint || '',
      reviewOfSystems: created.reviewOfSystems || '',
      physicalExamination: created.physicalExamination || '',
      clinicalImpression: created.clinicalImpression || '',
      treatmentPlan: created.treatmentPlan || '',
      followUpInstructions: created.followUpInstructions || '',
      notes: created.notes || '',
    });
    loadNotes();
    return created.noteId;
  };

  const resolveNoteIdForAttachments = (): string | null =>
    noteToAmend?.noteId ?? viewing?.noteId ?? (showForm && editing?.noteId ? editing.noteId : null);

  const resolveNoteIdForMedication = (): string | null =>
    noteToAmend?.noteId ?? viewing?.noteId ?? editing?.noteId ?? null;

  const handleAttachFileUpload = async (file: File) => {
    const noteId = resolveNoteIdForAttachments();
    if (!noteId) return;
    setAttachUploading(true);
    setAttachUploadError(null);
    try {
      await hospitalService.uploadNoteAttachment(noteId, file);
      const res = await hospitalService.getNoteAttachments(noteId);
      if (viewing?.noteId === noteId) setAttachments(res.data);
      if (noteToAmend?.noteId === noteId) setAttachments(res.data);
      if (editing?.noteId === noteId) setFormAttachments(res.data);
    } catch (err: any) {
      setAttachUploadError(ehrApiErrorMessage(err, 'Upload failed'));
    } finally {
      setAttachUploading(false);
    }
  };

  const handleAttachDeleteAttachment = async (attachmentId: string) => {
    if (!window.confirm('Remove this attachment?')) return;
    try {
      await hospitalService.deleteNoteAttachment(attachmentId);
      setAttachments(prev => prev.filter(a => a.attachmentId !== attachmentId));
      setFormAttachments(prev => prev.filter(a => a.attachmentId !== attachmentId));
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete attachment'));
    }
  };

  const handleLinkMedication = async () => {
    const noteId = resolveNoteIdForMedication();
    if (!noteId) {
      alert('No clinical note is open. Save the note first or open Amend on a signed note, then try again.');
      return;
    }
    if (!selectedMedicationId) {
      alert('Select a medication to link.');
      return;
    }
    try {
      await hospitalService.linkMedicationToNote(
        noteId,
        selectedMedicationId,
        linkType,
        linkStrength
      );
      setShowLinkMedicationDialog(false);
      setSelectedMedicationId('');
      if (viewing?.noteId === noteId) {
        await handleView(noteId);
      }
      if (editing?.noteId === noteId) {
        const r = await hospitalService.getClinicalNote(noteId);
        setFormNoteDetail(r.data);
      }
      if (noteToAmend?.noteId === noteId) {
        const [noteRes, attRes] = await Promise.all([
          hospitalService.getClinicalNote(noteId),
          hospitalService.getNoteAttachments(noteId),
        ]);
        setNoteToAmend(noteRes.data);
        setAttachments(attRes.data);
      }
      alert('Medication linked successfully');
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to link medication'));
    }
  };

  const handleUnlinkMedication = async (medicationId: string) => {
    const noteId = resolveNoteIdForMedication();
    if (!noteId) return;
    if (!window.confirm('Are you sure you want to unlink this medication?')) {
      return;
    }
    try {
      await hospitalService.unlinkMedicationFromNote(noteId, medicationId);
      if (viewing?.noteId === noteId) {
        await handleView(noteId);
      }
      if (editing?.noteId === noteId) {
        const r = await hospitalService.getClinicalNote(noteId);
        setFormNoteDetail(r.data);
      }
      if (noteToAmend?.noteId === noteId) {
        const [noteRes, attRes] = await Promise.all([
          hospitalService.getClinicalNote(noteId),
          hospitalService.getNoteAttachments(noteId),
        ]);
        setNoteToAmend(noteRes.data);
        setAttachments(attRes.data);
      }
      alert('Medication unlinked successfully');
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to unlink medication'));
    }
  };

  const loadAvailableMedications = async () => {
    if (!id) return;
    try {
      const [medicationsRes, prescriptionsRes] = await Promise.all([
        hospitalService.getActiveMedications(id).catch(() => ({ data: [] as any[] })),
        hospitalService.getActivePrescriptions(id).catch(() => ({ data: [] as any[] })),
      ]);

      const fromMedicationHistory = (medicationsRes.data || []).map((med: any) => ({
        medicationId: med.medicationId || med.id,
        medicationName: med.medicationName,
        genericName: med.genericName,
      }));
      const fromPrescriptions = (prescriptionsRes.data || [])
        .flatMap((rx: any) => {
          if (Array.isArray(rx.medications) && rx.medications.length > 0) {
            return rx.medications.map((med: any) => ({
              medicationId: med.medicationId,
              medicationName: med.medicationName || rx.medicationName,
              genericName: med.genericName || '',
            }));
          }
          return [];
        })
        .filter((m: any) => m.medicationId && m.medicationName);

      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
      const deduped = [...fromMedicationHistory, ...fromPrescriptions].filter(
        (med, idx, arr) => arr.findIndex((x) => x.medicationId === med.medicationId) === idx
      ).filter((med) => uuidRegex.test(String(med.medicationId)));
      setAvailableMedications(deduped);
    } catch (err: any) {
      console.error('Failed to load medications:', err);
    }
  };

  const handleDelete = async (noteId: string) => {
    const note = notes.find((n) => n.noteId === noteId);
    if (!note) return;
    const isDraft = note.noteStatus === 'DRAFT';
    const actionLabel = isDraft ? 'delete' : 'void';
    if (!window.confirm(`Are you sure you want to ${actionLabel} this clinical note?`)) {
      return;
    }
    try {
      if (isDraft) {
        await hospitalService.deleteClinicalNote(noteId);
      } else {
        await hospitalService.voidNote(noteId);
      }
      loadNotes();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, `Failed to ${actionLabel} note`));
    }
  };

  const handleSign = async () => {
    if (!noteToSign) return;
    try {
      await hospitalService.signNote(noteToSign.noteId, signData);
      setShowSignDialog(false);
      setNoteToSign(null);
      setSignData({ signatureMethod: 'ELECTRONIC', notes: '' });
      loadNotes();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to sign note'));
    }
  };

  const handleAmend = async () => {
    if (!noteToAmend) return;
    try {
      await hospitalService.amendNote(noteToAmend.noteId, amendData);
      setShowAmendDialog(false);
      setNoteToAmend(null);
      setAttachments([]);
      setAttachUploadError(null);
      setSelectedMedicationId('');
      setLinkType('DOCUMENTED');
      setLinkStrength('MODERATE');
      setAmendData({ amendmentReason: '', subjective: '', objective: '', assessment: '', plan: '' });
      loadNotes();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to amend note'));
    }
  };

  const handleUseTemplate = (template: NoteTemplate) => {
    try {
      const content = template.templateContent ? JSON.parse(template.templateContent) : {};
      setFormData({
        ...formData,
        templateId: template.templateId,
        subjective: content.subjective || '',
        objective: content.objective || '',
        assessment: content.assessment || '',
        plan: content.plan || '',
        chiefComplaint: content.chiefComplaint || '',
        reviewOfSystems: content.reviewOfSystems || '',
        physicalExamination: content.physicalExamination || '',
        clinicalImpression: content.clinicalImpression || '',
        treatmentPlan: content.treatmentPlan || '',
        followUpInstructions: content.followUpInstructions || '',
      });
    } catch (err) {
      console.error('Failed to parse template:', err);
    }
  };

  const formatDateTime = (date: string, time: string) => {
    return `${new Date(date).toLocaleDateString()} ${time}`;
  };

  if (loading && !notes.length) {
    return <div className="loading">Loading clinical notes...</div>;
  }

  const filteredNotes = notes.filter(note => {
    if (filterType && note.noteType !== filterType) return false;
    if (filterStatus && note.noteStatus !== filterStatus) return false;
    return true;
  });

  return (
    <div className="hospital-page">
      {/* Patient Information Header */}
      {patient && (
        <div className="page-header" style={{ marginBottom: '24px' }}>
          <div>
            <h1>{patient.fullName || '—'}</h1>
            <p>
              MRN: {patient.mrn} 
              {patient.dateOfBirth && ` | Age: ${formatAge(patient.dateOfBirth)}`}
              {patient.gender && ` | ${formatGenderLabel(patient.gender)}`}
            </p>
          </div>
        </div>
      )}

      <div className="page-header">
        <div>
          <h1>Clinical Notes</h1>
          <p>Document patient encounters with SOAP notes, progress notes, and other clinical documentation</p>
        </div>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/notes/templates`)}>
            Manage Templates
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); setFormNoteDetail(null); setFormAttachments([]); setAttachments([]); setAttachUploadError(null); resetForm(); }}>
            + Create Clinical Note
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Filters */}
      <div className="filters-section">
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search notes by content..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          <select
            value={filterType}
            onChange={(e) => {
              void handleFilter(e.target.value);
            }}
            className="filter-select"
          >
            <option value="">All Types</option>
            <option value="SOAP">SOAP</option>
            <option value="PROGRESS">Progress</option>
            <option value="CONSULTATION">Consultation</option>
            <option value="DISCHARGE">Discharge</option>
            <option value="PROCEDURE">Procedure</option>
            <option value="ADMISSION">Admission</option>
            <option value="OPERATIVE">Operative</option>
          </select>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="SIGNED">Signed</option>
            <option value="FINAL">Final</option>
            <option value="AMENDED">Amended</option>
            <option value="VOIDED">Voided</option>
          </select>
          <button className="btn-secondary" onClick={handleSearch}>Search</button>
          <button className="btn-secondary" onClick={() => { 
            setSearchTerm(''); 
            setFilterType(''); 
            setFilterStatus('');
            loadNotes(); 
          }}>Clear</button>
        </div>
      </div>

      {/* Create/Edit Form */}
      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Create'} Clinical Note</h3>
          
          {/* Template Selection */}
          {templates.length > 0 && !editing && (
            <div style={{ marginBottom: '20px', padding: '16px', background: '#f0f9ff', borderRadius: '8px' }}>
              <label style={{ fontWeight: 600, marginBottom: '8px', display: 'block' }}>Use Template:</label>
              <select
                onChange={(e) => {
                  const template = templates.find(t => t.templateId === e.target.value);
                  if (template) handleUseTemplate(template);
                }}
                style={{ width: '100%', padding: '8px' }}
              >
                <option value="">Select a template...</option>
                {templates.map(t => (
                  <option key={t.templateId} value={t.templateId}>{t.templateName}</option>
                ))}
              </select>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Note Type *</label>
                <select
                  required
                  value={formData.noteType}
                  onChange={(e) => setFormData({ ...formData, noteType: e.target.value as any })}
                >
                  <option value="SOAP">SOAP</option>
                  <option value="PROGRESS">Progress</option>
                  <option value="CONSULTATION">Consultation</option>
                  <option value="DISCHARGE">Discharge</option>
                  <option value="PROCEDURE">Procedure</option>
                  <option value="ADMISSION">Admission</option>
                  <option value="OPERATIVE">Operative</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>Note Date *</label>
                <input
                  type="date"
                  required
                  value={formData.noteDate}
                  onChange={(e) => setFormData({ ...formData, noteDate: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Note Time *</label>
                <input
                  type="time"
                  required
                  value={formData.noteTime}
                  onChange={(e) => setFormData({ ...formData, noteTime: e.target.value })}
                />
              </div>
            </div>

            {/* SOAP Note Fields */}
            {formData.noteType === 'SOAP' && (
              <>
                <div className="form-section">
                  <h4 className="form-section-title">SOAP Note</h4>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label>Subjective (S)</label>
                    <textarea
                      value={formData.subjective}
                      onChange={(e) => setFormData({ ...formData, subjective: e.target.value })}
                      rows={4}
                      placeholder="Patient's description of symptoms, history, concerns..."
                    />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label>Objective (O)</label>
                    <textarea
                      value={formData.objective}
                      onChange={(e) => setFormData({ ...formData, objective: e.target.value })}
                      rows={4}
                      placeholder="Observable findings, vital signs, physical examination results..."
                    />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label>Assessment (A)</label>
                    <textarea
                      value={formData.assessment}
                      onChange={(e) => setFormData({ ...formData, assessment: e.target.value })}
                      rows={4}
                      placeholder="Clinical assessment, diagnosis, differential diagnosis..."
                    />
                  </div>
                  <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label>Plan (P)</label>
                    <textarea
                      value={formData.plan}
                      onChange={(e) => setFormData({ ...formData, plan: e.target.value })}
                      rows={4}
                      placeholder="Treatment plan, medications, follow-up, patient education..."
                    />
                  </div>
                </div>
              </>
            )}

            {/* Additional Fields */}
            <div className="form-section">
              <h4 className="form-section-title">Additional Information</h4>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Chief Complaint</label>
                <textarea
                  value={formData.chiefComplaint}
                  onChange={(e) => setFormData({ ...formData, chiefComplaint: e.target.value })}
                  rows={2}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Review of Systems</label>
                <textarea
                  value={formData.reviewOfSystems}
                  onChange={(e) => setFormData({ ...formData, reviewOfSystems: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Physical Examination</label>
                <textarea
                  value={formData.physicalExamination}
                  onChange={(e) => setFormData({ ...formData, physicalExamination: e.target.value })}
                  rows={4}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Clinical Impression</label>
                <textarea
                  value={formData.clinicalImpression}
                  onChange={(e) => setFormData({ ...formData, clinicalImpression: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Treatment Plan</label>
                <textarea
                  value={formData.treatmentPlan}
                  onChange={(e) => setFormData({ ...formData, treatmentPlan: e.target.value })}
                  rows={4}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Follow-up Instructions</label>
                <textarea
                  value={formData.followUpInstructions}
                  onChange={(e) => setFormData({ ...formData, followUpInstructions: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Additional Notes</label>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  rows={3}
                />
              </div>
            </div>

            {editing?.noteId && (
              <div className="form-section" style={{ marginTop: '8px', borderTop: '1px solid #e5e7eb', paddingTop: '16px' }}>
                <h4 className="form-section-title">Attachments &amp; medications</h4>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', flexWrap: 'wrap', gap: '8px' }}>
                  <span style={{ fontSize: '13px', color: '#6b7280' }}>{formAttachments.length} file(s)</span>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      className="btn-secondary"
                      style={{ fontSize: '13px', padding: '4px 12px' }}
                      onClick={() => formAttachFileInputRef.current?.click()}
                      disabled={attachUploading}
                    >
                      {attachUploading ? 'Uploading…' : '+ Attach file'}
                    </button>
                    <input
                      ref={formAttachFileInputRef}
                      type="file"
                      style={{ display: 'none' }}
                      accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.docx,.doc,.csv,.txt,.mp3,.wav,.mp4"
                      onChange={e => e.target.files?.[0] && handleAttachFileUpload(e.target.files[0])}
                    />
                    <button
                      type="button"
                      className="btn-secondary"
                      style={{ fontSize: '13px', padding: '4px 12px' }}
                      onClick={() => {
                        loadAvailableMedications();
                        setShowLinkMedicationDialog(true);
                      }}
                    >
                      + Link medication
                    </button>
                  </div>
                </div>
                {attachUploadError && (
                  <div className="error-message" style={{ marginBottom: '10px', fontSize: '13px' }}>
                    {attachUploadError}
                  </div>
                )}
                <div
                  style={{
                    border: `2px dashed ${formAttachDragOver ? '#3498db' : '#e5e7eb'}`,
                    borderRadius: '6px',
                    padding: '12px',
                    textAlign: 'center',
                    fontSize: '12px',
                    color: '#9ca3af',
                    marginBottom: formAttachments.length ? '12px' : 0,
                    background: formAttachDragOver ? '#ebf5fb' : 'transparent',
                    cursor: 'pointer',
                  }}
                  onDragOver={e => { e.preventDefault(); setFormAttachDragOver(true); }}
                  onDragLeave={() => setFormAttachDragOver(false)}
                  onDrop={e => {
                    e.preventDefault();
                    setFormAttachDragOver(false);
                    const file = e.dataTransfer.files[0];
                    if (file) handleAttachFileUpload(file);
                  }}
                  onClick={() => formAttachFileInputRef.current?.click()}
                >
                  Drop a file here or click to browse — same types as in View mode
                </div>
                {formAttachments.length > 0 && (
                  <div style={{ display: 'grid', gap: '8px', marginBottom: '16px' }}>
                    {formAttachments.map((attachment) => (
                      <div
                        key={attachment.attachmentId}
                        style={{
                          padding: '8px 10px',
                          background: '#f9fafb',
                          borderRadius: '8px',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          fontSize: '13px',
                        }}
                      >
                        <span style={{ fontWeight: 600 }}>{attachment.fileName}</span>
                        <button type="button" className="btn-link" style={{ color: '#c0392b' }} onClick={() => handleAttachDeleteAttachment(attachment.attachmentId)}>
                          Remove
                        </button>
                      </div>
                    ))}
                  </div>
                )}
                <div style={{ marginTop: '8px' }}>
                  <strong style={{ fontSize: '14px' }}>Linked medications ({formNoteDetail?.medications?.length ?? 0})</strong>
                  {formNoteDetail?.medications && formNoteDetail.medications.length > 0 ? (
                    <div style={{ display: 'grid', gap: '8px', marginTop: '10px' }}>
                      {formNoteDetail.medications.map((med) => (
                        <div
                          key={med.linkId}
                          style={{
                            padding: '10px 12px',
                            background: '#f9fafb',
                            borderRadius: '8px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                          }}
                        >
                          <div>
                            <div style={{ fontWeight: 600 }}>{med.medicationName}</div>
                            <div style={{ fontSize: '12px', color: '#9ca3af' }}>
                              {med.linkType} · {med.linkStrength}
                            </div>
                          </div>
                          <button type="button" className="btn-link btn-danger" onClick={() => handleUnlinkMedication(med.medicationId)}>
                            Unlink
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p style={{ color: '#6b7280', fontStyle: 'italic', fontSize: '13px', marginTop: '8px' }}>No medications linked yet</p>
                  )}
                </div>
              </div>
            )}
            {!editing && (
              <div className="form-section" style={{ marginTop: '8px', borderTop: '1px solid #e5e7eb', paddingTop: '16px' }}>
                <h4 className="form-section-title">Attachments &amp; medications</h4>
                <p style={{ fontSize: '13px', color: '#6b7280', marginBottom: '10px' }}>
                  Attachments and medication links can be added immediately; the note will be saved as a draft first.
                </p>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '13px', padding: '4px 12px' }}
                    onClick={async () => {
                      try {
                        await ensureDraftNoteForForm();
                        setTimeout(() => formAttachFileInputRef.current?.click(), 0);
                      } catch (err: any) {
                        alert(ehrApiErrorMessage(err, 'Failed to initialize note for attachment'));
                      }
                    }}
                  >
                    + Attach file
                  </button>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '13px', padding: '4px 12px' }}
                    onClick={async () => {
                      try {
                        await ensureDraftNoteForForm();
                        loadAvailableMedications();
                        setShowLinkMedicationDialog(true);
                      } catch (err: any) {
                        alert(ehrApiErrorMessage(err, 'Failed to initialize note for medication linking'));
                      }
                    }}
                  >
                    + Link medication
                  </button>
                </div>
              </div>
            )}

            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); setFormNoteDetail(null); setFormAttachments([]); setAttachments([]); setAttachUploadError(null); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">{editing ? 'Save changes' : 'Save note'}</button>
            </div>
          </form>
        </div>
      )}

      {/* Sign Dialog */}
      {showSignDialog && noteToSign && portalLayoutOverlay(
        <div
          className={LAYOUT_OVERLAY_DETECT_CLASS}
          style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z }}
        >
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Sign Clinical Note</h3>
            <div className="form-group">
              <label>Signature Method *</label>
              <select
                required
                value={signData.signatureMethod}
                onChange={(e) => setSignData({ ...signData, signatureMethod: e.target.value as any })}
              >
                <option value="ELECTRONIC">Electronic</option>
                <option value="DIGITAL">Digital</option>
                <option value="TYPED">Typed</option>
                <option value="VOICE">Voice</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label>Notes</label>
              <textarea
                value={signData.notes}
                onChange={(e) => setSignData({ ...signData, notes: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowSignDialog(false); setNoteToSign(null); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleSign}>Sign Note</button>
            </div>
          </div>
        </div>,
      )}

      {/* Amend Dialog */}
      {showAmendDialog && noteToAmend && portalLayoutOverlay(
        <div
          className={LAYOUT_OVERLAY_DETECT_CLASS}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_ROOT_Z, padding: '24px', overflowY: 'auto', boxSizing: 'border-box' }}
        >
          <div className="form-container" style={{ maxWidth: '600px', width: 'min(600px, 100%)', maxHeight: 'calc(100vh - 48px)', overflowY: 'auto', margin: '0 auto', boxSizing: 'border-box' }}>
            <h3>Amend Clinical Note</h3>
            <div className="form-group">
              <label>Amendment Reason *</label>
              <textarea
                required
                value={amendData.amendmentReason}
                onChange={(e) => setAmendData({ ...amendData, amendmentReason: e.target.value })}
                rows={2}
              />
            </div>
            <div className="form-group">
              <label>Updated Subjective</label>
              <textarea
                value={amendData.subjective}
                onChange={(e) => setAmendData({ ...amendData, subjective: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-group">
              <label>Updated Objective</label>
              <textarea
                value={amendData.objective}
                onChange={(e) => setAmendData({ ...amendData, objective: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-group">
              <label>Updated Assessment</label>
              <textarea
                value={amendData.assessment}
                onChange={(e) => setAmendData({ ...amendData, assessment: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-group">
              <label>Updated Plan</label>
              <textarea
                value={amendData.plan}
                onChange={(e) => setAmendData({ ...amendData, plan: e.target.value })}
                rows={3}
              />
            </div>
            <div className="form-section" style={{ marginTop: '8px', borderTop: '1px solid #e5e7eb', paddingTop: '16px' }}>
              <h4 className="form-section-title">Attachments &amp; medications</h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', flexWrap: 'wrap', gap: '8px' }}>
                <span style={{ fontSize: '13px', color: '#6b7280' }}>{attachments.length} file(s)</span>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '13px', padding: '4px 12px' }}
                    onClick={() => attachFileInputRef.current?.click()}
                    disabled={attachUploading}
                  >
                    {attachUploading ? 'Uploading...' : '+ Attach file'}
                  </button>
                  <input
                    ref={attachFileInputRef}
                    type="file"
                    style={{ display: 'none' }}
                    accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.docx,.doc,.csv,.txt,.mp3,.wav,.mp4"
                    onChange={e => e.target.files?.[0] && handleAttachFileUpload(e.target.files[0])}
                  />
                  <button
                    type="button"
                    className="btn-secondary"
                    style={{ fontSize: '13px', padding: '4px 12px' }}
                    onClick={() => {
                      loadAvailableMedications();
                      setShowLinkMedicationDialog(true);
                    }}
                  >
                    + Link medication
                  </button>
                </div>
              </div>
              {attachUploadError && (
                <div className="error-message" style={{ marginBottom: '10px', fontSize: '13px' }}>
                  {attachUploadError}
                </div>
              )}
              {attachments.length > 0 && (
                <div style={{ display: 'grid', gap: '8px', marginBottom: '12px' }}>
                  {attachments.map((attachment) => (
                    <div
                      key={attachment.attachmentId}
                      style={{
                        padding: '8px 10px',
                        background: '#f9fafb',
                        borderRadius: '8px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '13px',
                      }}
                    >
                      <span style={{ fontWeight: 600 }}>{attachment.fileName}</span>
                      <div style={{ display: 'flex', gap: '10px' }}>
                        <button
                          type="button"
                          className="btn-link"
                          onClick={async () => {
                            try {
                              const res = await hospitalService.downloadNoteAttachment(attachment.attachmentId);
                              const url = window.URL.createObjectURL(new Blob([res.data as BlobPart]));
                              const a = document.createElement('a');
                              a.href = url;
                              a.download = attachment.fileName;
                              a.click();
                              window.URL.revokeObjectURL(url);
                            } catch {
                              alert('Failed to download attachment');
                            }
                          }}
                        >
                          Download
                        </button>
                        <button type="button" className="btn-link" style={{ color: '#c0392b' }} onClick={() => handleAttachDeleteAttachment(attachment.attachmentId)}>
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
              <div style={{ marginTop: '8px' }}>
                <strong style={{ fontSize: '14px' }}>Linked medications ({noteToAmend?.medications?.length ?? 0})</strong>
                {noteToAmend?.medications && noteToAmend.medications.length > 0 ? (
                  <div style={{ display: 'grid', gap: '8px', marginTop: '10px' }}>
                    {noteToAmend.medications.map((med) => (
                      <div
                        key={med.linkId}
                        style={{
                          padding: '10px 12px',
                          background: '#f9fafb',
                          borderRadius: '8px',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 600 }}>{med.medicationName}</div>
                          <div style={{ fontSize: '12px', color: '#9ca3af' }}>
                            {med.linkType} · {med.linkStrength}
                          </div>
                        </div>
                        <button type="button" className="btn-link btn-danger" onClick={() => handleUnlinkMedication(med.medicationId)}>
                          Unlink
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p style={{ color: '#6b7280', fontStyle: 'italic', fontSize: '13px', marginTop: '8px' }}>No medications linked yet</p>
                )}
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowAmendDialog(false); setNoteToAmend(null); setAttachments([]); setAttachUploadError(null); setSelectedMedicationId(''); }}>
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleAmend}>Save Amendment</button>
            </div>
          </div>
        </div>,
      )}

      {/* View Note Dialog */}
      {viewing && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}
          role="dialog"
          aria-modal="true"
          aria-labelledby="clinical-note-view-title"
        >
          <div className="modal-content clinical-note-view-modal" style={{ maxWidth: '860px', width: 'min(860px, calc(100vw - 32px))' }}>
            <div className="modal-header">
              <h3 id="clinical-note-view-title">Clinical Note - {viewing.noteType}</h3>
              <button
                className="modal-close"
                onClick={() => {
                  setViewing(null);
                  setAttachments([]);
                  setAttachUploadError(null);
                }}
                aria-label="Close clinical note"
              >
                ×
              </button>
            </div>
            <div className="modal-body">
            <div className="info-card">
              <div className="info-row">
                <span className="info-label">Date & Time:</span>
                <span className="info-value">{formatDateTime(viewing.noteDate, viewing.noteTime)}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Status:</span>
                <span className="info-value">
                  <span className={`status-badge status-${viewing.noteStatus.toLowerCase()}`}>
                    {viewing.noteStatus}
                  </span>
                </span>
              </div>
              {viewing.signedDate && (
                <div className="info-row">
                  <span className="info-label">Signed:</span>
                  <span className="info-value">{new Date(viewing.signedDate).toLocaleString()}</span>
                </div>
              )}
            </div>
            {viewing.subjective && (
              <div className="info-card">
                <h4 className="info-card-title">Subjective</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.subjective}</p>
              </div>
            )}
            {viewing.objective && (
              <div className="info-card">
                <h4 className="info-card-title">Objective</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.objective}</p>
              </div>
            )}
            {viewing.assessment && (
              <div className="info-card">
                <h4 className="info-card-title">Assessment</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.assessment}</p>
              </div>
            )}
            {viewing.plan && (
              <div className="info-card">
                <h4 className="info-card-title">Plan</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.plan}</p>
              </div>
            )}
            {viewing.chiefComplaint && (
              <div className="info-card">
                <h4 className="info-card-title">Chief Complaint</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.chiefComplaint}</p>
              </div>
            )}
            {viewing.reviewOfSystems && (
              <div className="info-card">
                <h4 className="info-card-title">Review of Systems</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.reviewOfSystems}</p>
              </div>
            )}
            {viewing.physicalExamination && (
              <div className="info-card">
                <h4 className="info-card-title">Physical Examination</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.physicalExamination}</p>
              </div>
            )}
            {viewing.clinicalImpression && (
              <div className="info-card">
                <h4 className="info-card-title">Clinical Impression</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.clinicalImpression}</p>
              </div>
            )}
            {viewing.treatmentPlan && (
              <div className="info-card">
                <h4 className="info-card-title">Treatment Plan</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.treatmentPlan}</p>
              </div>
            )}
            {viewing.followUpInstructions && (
              <div className="info-card">
                <h4 className="info-card-title">Follow-up Instructions</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.followUpInstructions}</p>
              </div>
            )}
            {viewing.notes && (
              <div className="info-card">
                <h4 className="info-card-title">Additional Notes</h4>
                <p style={{ whiteSpace: 'pre-wrap' }}>{viewing.notes}</p>
              </div>
            )}
            {/* Attachments section — visible and manageable in view mode */}
            <div className="info-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h4 className="info-card-title" style={{ margin: 0 }}>
                  Attachments ({attachments.length})
                </h4>
                <button
                  className="btn-secondary"
                  style={{ fontSize: '13px', padding: '4px 12px' }}
                  onClick={() => attachFileInputRef.current?.click()}
                  disabled={attachUploading}
                >
                  {attachUploading ? 'Uploading...' : '+ Attach File'}
                </button>
                <input
                  ref={attachFileInputRef}
                  type="file"
                  style={{ display: 'none' }}
                  accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.docx,.doc,.csv,.txt,.mp3,.wav,.mp4"
                  onChange={e => e.target.files?.[0] && handleAttachFileUpload(e.target.files[0])}
                />
              </div>

              {attachUploadError && (
                <div className="error-message" style={{ marginBottom: '10px', fontSize: '13px' }}>
                  {attachUploadError}
                </div>
              )}

              {attachments.length > 0 && (
                <div style={{ display: 'grid', gap: '8px' }}>
                  {attachments.map((attachment) => (
                    <div
                      key={attachment.attachmentId}
                      style={{
                        padding: '10px 12px',
                        background: '#f9fafb',
                        borderRadius: '8px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '14px' }}>{attachment.fileName}</div>
                        {attachment.description && (
                          <div style={{ fontSize: '13px', color: '#6b7280', marginTop: '2px' }}>
                            {attachment.description}
                          </div>
                        )}
                        <div style={{ fontSize: '12px', color: '#9ca3af', marginTop: '2px' }}>
                          {attachment.mimeType || attachment.fileType || 'file'}
                          {attachment.fileSize ? ` • ${(attachment.fileSize / 1024).toFixed(1)} KB` : ''}
                          {attachment.attachmentType ? ` • ${attachment.attachmentType}` : ''}
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn-link"
                          onClick={async () => {
                            try {
                              const res = await hospitalService.downloadNoteAttachment(attachment.attachmentId);
                              const url = window.URL.createObjectURL(new Blob([res.data as BlobPart]));
                              const a = document.createElement('a');
                              a.href = url;
                              a.download = attachment.fileName;
                              a.click();
                              window.URL.revokeObjectURL(url);
                            } catch {
                              alert('Failed to download attachment');
                            }
                          }}
                        >
                          Download
                        </button>
                        <button className="btn-link" style={{ color: '#c0392b' }} onClick={() => handleAttachDeleteAttachment(attachment.attachmentId)}>
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            {viewing.versionNumber && viewing.versionNumber > 1 && (
              <div className="info-card" style={{ borderLeft: '4px solid #f59e0b' }}>
                <h4 className="info-card-title">Version Information</h4>
                <div className="info-row">
                  <span className="info-label">Version:</span>
                  <span className="info-value">{viewing.versionNumber}</span>
                </div>
                {viewing.amendmentReason && (
                  <div className="info-row">
                    <span className="info-label">Amendment Reason:</span>
                    <span className="info-value">{viewing.amendmentReason}</span>
                  </div>
                )}
                {viewing.amendedDate && (
                  <div className="info-row">
                    <span className="info-label">Amended Date:</span>
                    <span className="info-value">{new Date(viewing.amendedDate).toLocaleString()}</span>
                  </div>
                )}
              </div>
            )}
            {/* Linked Medications */}
            <div className="info-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h4 className="info-card-title">
                  Linked Medications ({viewing.medications?.length || 0})
                </h4>
                <button
                  className="btn-link"
                  onClick={() => {
                    loadAvailableMedications();
                    setShowLinkMedicationDialog(true);
                  }}
                >
                  + Link Medication
                </button>
              </div>
              {viewing.medications && viewing.medications.length > 0 ? (
                <div style={{ display: 'grid', gap: '12px', marginTop: '12px' }}>
                  {viewing.medications.map((med) => (
                    <div key={med.linkId} style={{ 
                      padding: '12px', 
                      background: '#f9fafb', 
                      borderRadius: '8px',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600 }}>{med.medicationName}</div>
                        {med.genericName && (
                          <div style={{ fontSize: '14px', color: '#6b7280', marginTop: '4px' }}>
                            Generic: {med.genericName}
                          </div>
                        )}
                        <div style={{ fontSize: '12px', color: '#9ca3af', marginTop: '4px', display: 'flex', gap: '12px' }}>
                          <span>Type: {med.linkType}</span>
                          <span>Strength: {med.linkStrength}</span>
                        </div>
                        {med.clinicalRelevance && (
                          <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                            {med.clinicalRelevance}
                          </div>
                        )}
                      </div>
                      <button
                        className="btn-link btn-danger"
                        onClick={() => handleUnlinkMedication(med.medicationId)}
                        style={{ marginLeft: '12px' }}
                      >
                        Unlink
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ color: '#6b7280', fontStyle: 'italic' }}>No medications linked to this note</p>
              )}
            </div>
            <div className="modal-footer">
              <button
                className="btn-secondary"
                onClick={() => {
                  setViewing(null);
                  setAttachments([]);
                  setAttachUploadError(null);
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </div>,
      )}

      {/* Link Medication Dialog */}
      {showLinkMedicationDialog && portalLayoutOverlay(
        <div
          className={LAYOUT_OVERLAY_DETECT_CLASS}
          style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: LAYOUT_OVERLAY_NESTED_Z }}
        >
          <div className="form-container" style={{ maxWidth: '500px', width: '90%' }}>
            <h3>Link Medication to Clinical Note</h3>
            <div className="form-group">
              <label>Medication *</label>
              <select
                required
                value={selectedMedicationId}
                onChange={(e) => setSelectedMedicationId(e.target.value)}
              >
                <option value="">Select a medication...</option>
                {availableMedications.map((med) => (
                  <option key={med.medicationId} value={med.medicationId}>
                    {med.medicationName} {med.genericName ? `(${med.genericName})` : ''}
                  </option>
                ))}
              </select>
              {availableMedications.length === 0 && (
                <p style={{ fontSize: '13px', color: '#b45309', marginTop: '8px', marginBottom: 0 }}>
                  No medications with a valid record ID were found. Add an active medication or a prescription line that includes a medication ID for this patient, then try again.
                </p>
              )}
            </div>
            <div className="form-group">
              <label>Link Type</label>
              <select
                value={linkType}
                onChange={(e) => setLinkType(e.target.value)}
              >
                <option value="DOCUMENTED">Documented</option>
                <option value="PRESCRIBED">Prescribed</option>
                <option value="DISCONTINUED">Discontinued</option>
                <option value="MODIFIED">Modified</option>
                <option value="MONITORED">Monitored</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label>Link Strength</label>
              <select
                value={linkStrength}
                onChange={(e) => setLinkStrength(e.target.value)}
              >
                <option value="WEAK">Weak</option>
                <option value="MODERATE">Moderate</option>
                <option value="STRONG">Strong</option>
              </select>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowLinkMedicationDialog(false); setSelectedMedicationId(''); setLinkType('DOCUMENTED'); setLinkStrength('MODERATE'); }}>
                Cancel
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={handleLinkMedication}
                disabled={availableMedications.length === 0}
              >
                Link Medication
              </button>
            </div>
          </div>
        </div>,
      )}

      {/* Notes List */}
      {filteredNotes.length === 0 ? (
        <div className="empty-state">
          <p>No clinical notes found{filterType || filterStatus ? ` (filtered)` : ''}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>Type</th>
                <th>Status</th>
                <th>Chief Complaint</th>
                <th>Signed</th>
                <th>Version</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredNotes.map((note) => (
                <tr key={note.noteId}>
                  <td>{formatDateTime(note.noteDate, note.noteTime)}</td>
                  <td>{note.noteType}</td>
                  <td>
                    <span className={`status-badge status-${note.noteStatus.toLowerCase()}`}>
                      {note.noteStatus}
                    </span>
                  </td>
                  <td>{note.chiefComplaint || '-'}</td>
                  <td>{note.signedDate ? new Date(note.signedDate).toLocaleDateString() : (note.amendedDate ? new Date(note.amendedDate).toLocaleDateString() : '-')}</td>
                  <td>
                    {note.versionNumber && note.versionNumber > 1 ? (
                      <span className="status-badge" style={{ backgroundColor: '#f59e0b', color: 'white' }}>
                        v{note.versionNumber}
                      </span>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleView(note.noteId)}>View</button>
                      {note.noteStatus === 'DRAFT' && (
                        <>
                          <button className="btn-link" onClick={() => handleEdit(note)}>Edit</button>
                          <button className="btn-link btn-danger" onClick={() => handleDelete(note.noteId)}>Delete</button>
                        </>
                      )}
                      {note.noteStatus === 'DRAFT' && (
                        <button className="btn-link" onClick={() => { setNoteToSign(note); setShowSignDialog(true); }}>
                          Sign
                        </button>
                      )}
                      {(note.noteStatus === 'SIGNED' || note.noteStatus === 'AMENDED') && (
                        <button className="btn-link" onClick={() => handleOpenAmend(note)}>
                          Amend
                        </button>
                      )}
                      {(note.noteStatus === 'SIGNED' || note.noteStatus === 'AMENDED') && (
                        <button className="btn-link btn-danger" onClick={() => handleDelete(note.noteId)}>Void</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {filteredNotes.length > 0 && (
        <div className="table-footer">
          <p>Showing {filteredNotes.length} clinical note(s)</p>
        </div>
      )}
    </div>
  );
};

export default ClinicalNotesPage;
