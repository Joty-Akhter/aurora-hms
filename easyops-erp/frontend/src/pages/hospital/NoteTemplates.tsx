import React, { useEffect, useState } from 'react';
import hospitalService, { NoteTemplate, NoteTemplateRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

type TemplateContentFields = {
  chiefComplaint: string;
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
  reviewOfSystems: string;
  physicalExamination: string;
  clinicalImpression: string;
  treatmentPlan: string;
  followUpInstructions: string;
};

const EMPTY_CONTENT: TemplateContentFields = {
  chiefComplaint: '',
  subjective: '',
  objective: '',
  assessment: '',
  plan: '',
  reviewOfSystems: '',
  physicalExamination: '',
  clinicalImpression: '',
  treatmentPlan: '',
  followUpInstructions: '',
};

function parseTemplateContent(raw: string | undefined | null): TemplateContentFields {
  if (!raw?.trim()) return { ...EMPTY_CONTENT };
  try {
    const parsed = JSON.parse(raw) as Partial<TemplateContentFields>;
    return {
      chiefComplaint: parsed.chiefComplaint ?? '',
      subjective: parsed.subjective ?? '',
      objective: parsed.objective ?? '',
      assessment: parsed.assessment ?? '',
      plan: parsed.plan ?? '',
      reviewOfSystems: parsed.reviewOfSystems ?? '',
      physicalExamination: parsed.physicalExamination ?? '',
      clinicalImpression: parsed.clinicalImpression ?? '',
      treatmentPlan: parsed.treatmentPlan ?? '',
      followUpInstructions: parsed.followUpInstructions ?? '',
    };
  } catch {
    return { ...EMPTY_CONTENT, subjective: raw.trim() };
  }
}

function serializeTemplateContent(fields: TemplateContentFields): string {
  return JSON.stringify(fields);
}

const NoteTemplatesPage: React.FC = () => {
  const [templates, setTemplates] = useState<NoteTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<NoteTemplate | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');

  const [formData, setFormData] = useState<NoteTemplateRequest>({
    templateName: '',
    templateType: 'SOAP',
    specialty: '',
    description: '',
    templateContent: '',
    isSystemTemplate: false,
    isActive: true,
    isPublic: false,
  });
  const [contentFields, setContentFields] = useState<TemplateContentFields>({ ...EMPTY_CONTENT });

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const response = await hospitalService.getNoteTemplates();
      setTemplates(response.data);
    } catch (err: any) {
      console.error('Failed to load templates:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load templates'));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadTemplates();
      return;
    }
    try {
      setLoading(true);
      const response = await hospitalService.searchTemplates(searchTerm);
      setTemplates(response.data);
    } catch (err: any) {
      console.error('Failed to search templates:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search templates'));
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = async (type: string) => {
    setFilterType(type);
    try {
      setLoading(true);
      const response = type
        ? await hospitalService.getTemplatesByType(type)
        : await hospitalService.getNoteTemplates();
      setTemplates(response.data);
    } catch (err: any) {
      console.error('Failed to filter templates:', err);
      setError(ehrApiErrorMessage(err, 'Failed to filter templates'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: NoteTemplateRequest = {
      ...formData,
      templateContent: serializeTemplateContent(contentFields),
    };
    try {
      if (editing) {
        await hospitalService.updateNoteTemplate(editing.templateId, payload);
      } else {
        await hospitalService.createNoteTemplate(payload);
      }
      setShowForm(false);
      setEditing(null);
      resetForm();
      loadTemplates();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save template'));
    }
  };

  const resetForm = () => {
    setFormData({
      templateName: '',
      templateType: 'SOAP',
      specialty: '',
      description: '',
      templateContent: '',
      isSystemTemplate: false,
      isActive: true,
      isPublic: false,
    });
    setContentFields({ ...EMPTY_CONTENT });
  };

  const handleEdit = (template: NoteTemplate) => {
    if (template.isSystemTemplate) {
      alert('System templates cannot be edited');
      return;
    }
    setEditing(template);
    setFormData({
      templateName: template.templateName,
      templateType: template.templateType,
      specialty: template.specialty || '',
      description: template.description || '',
      templateContent: template.templateContent || '',
      isSystemTemplate: template.isSystemTemplate || false,
      isActive: template.isActive !== false,
      isPublic: template.isPublic || false,
    });
    setContentFields(parseTemplateContent(template.templateContent));
    setShowForm(true);
  };

  const handleDelete = async (templateId: string) => {
    if (!window.confirm('Are you sure you want to delete this template?')) {
      return;
    }
    try {
      await hospitalService.deleteNoteTemplate(templateId);
      loadTemplates();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete template'));
    }
  };

  const updateContentField = (field: keyof TemplateContentFields, value: string) => {
    setContentFields((prev) => ({ ...prev, [field]: value }));
  };

  if (loading && !templates.length) {
    return <div className="loading">Loading templates...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
        <h3>Note Templates</h3>
        <button className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); resetForm(); }}>
          + Create Template
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="filters-section">
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search templates..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          <select
            value={filterType}
            onChange={(e) => handleFilter(e.target.value)}
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
          <button className="btn-secondary" onClick={handleSearch}>Search</button>
          <button className="btn-secondary" onClick={() => { setSearchTerm(''); setFilterType(''); loadTemplates(); }}>Clear</button>
        </div>
      </div>

      {showForm && (
        <div className="form-container">
          <h3>{editing ? 'Edit' : 'Create'} Note Template</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Template Name *</label>
                <input
                  type="text"
                  required
                  value={formData.templateName}
                  onChange={(e) => setFormData({ ...formData, templateName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Template Type *</label>
                <select
                  required
                  value={formData.templateType}
                  onChange={(e) => setFormData({ ...formData, templateType: e.target.value as any })}
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
                <label>Specialty</label>
                <input
                  type="text"
                  value={formData.specialty}
                  onChange={(e) => setFormData({ ...formData, specialty: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={formData.isPublic}
                    onChange={(e) => setFormData({ ...formData, isPublic: e.target.checked })}
                  />
                  Public Template
                </label>
              </div>
            </div>

            <div className="form-section" style={{ marginTop: '16px' }}>
              <h4 style={{ margin: '0 0 12px' }}>Template content</h4>
              <p style={{ margin: '0 0 12px', fontSize: '13px', color: '#6b7280' }}>
                Pre-filled sections used when this template is applied to a clinical note.
              </p>
              <div className="form-grid">
                {([
                  ['chiefComplaint', 'Chief complaint'],
                  ['subjective', 'Subjective'],
                  ['objective', 'Objective'],
                  ['assessment', 'Assessment'],
                  ['plan', 'Plan'],
                  ['reviewOfSystems', 'Review of systems'],
                  ['physicalExamination', 'Physical examination'],
                  ['clinicalImpression', 'Clinical impression'],
                  ['treatmentPlan', 'Treatment plan'],
                  ['followUpInstructions', 'Follow-up instructions'],
                ] as const).map(([field, label]) => (
                  <div key={field} className="form-group" style={{ gridColumn: '1 / -1' }}>
                    <label>{label}</label>
                    <textarea
                      rows={3}
                      value={contentFields[field]}
                      onChange={(e) => updateContentField(field, e.target.value)}
                      placeholder={`Default ${label.toLowerCase()} text…`}
                    />
                  </div>
                ))}
              </div>
            </div>

            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save Template</button>
            </div>
          </form>
        </div>
      )}

      {templates.length === 0 ? (
        <div className="empty-state">
          <p>No templates found</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Template Name</th>
                <th>Type</th>
                <th>Specialty</th>
                <th>Public</th>
                <th>System</th>
                <th>Usage Count</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr key={template.templateId}>
                  <td><strong>{template.templateName}</strong></td>
                  <td>{template.templateType}</td>
                  <td>{template.specialty || '—'}</td>
                  <td>{template.isPublic ? 'Yes' : 'No'}</td>
                  <td>{template.isSystemTemplate ? 'Yes' : 'No'}</td>
                  <td>{template.usageCount || 0}</td>
                  <td>
                    <div className="action-buttons">
                      {!template.isSystemTemplate && (
                        <>
                          <button className="btn-link" onClick={() => handleEdit(template)}>Edit</button>
                          <button className="btn-link btn-danger" onClick={() => handleDelete(template.templateId)}>Delete</button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default NoteTemplatesPage;
