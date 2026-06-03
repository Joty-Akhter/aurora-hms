import React, { useState, useEffect } from 'react';
import { getEmailTemplates, createEmailTemplate, updateEmailTemplate, deleteEmailTemplate } from '../../services/crmService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Crm.css';

interface EmailTemplate {
  templateId?: string;
  organizationId: string;
  templateName: string;
  templateCode: string;
  subject: string;
  bodyHtml?: string;
  bodyText?: string;
  templateType?: string;
  isActive: boolean;
}

const EmailTemplateManager: React.FC = () => {
  const [templates, setTemplates] = useState<EmailTemplate[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  
  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  const [formData, setFormData] = useState<EmailTemplate>({
    organizationId,
    templateName: '',
    templateCode: '',
    subject: '',
    bodyHtml: '',
    bodyText: '',
    templateType: 'CAMPAIGN',
    isActive: true
  });

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const data = await getEmailTemplates(organizationId);
      setTemplates(data);
    } catch (error) {
      console.error('Error loading templates:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (formData.templateId) {
        await updateEmailTemplate(formData.templateId, formData);
      } else {
        await createEmailTemplate(formData);
      }
      setShowForm(false);
      resetForm();
      loadTemplates();
    } catch (error) {
      console.error('Error saving template:', error);
    }
  };

  const handleDelete = async (templateId: string) => {
    if (window.confirm('Delete this template?')) {
      try {
        await deleteEmailTemplate(templateId);
        loadTemplates();
      } catch (error) {
        console.error('Error deleting template:', error);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      organizationId,
      templateName: '',
      templateCode: '',
      subject: '',
      bodyHtml: '',
      bodyText: '',
      templateType: 'CAMPAIGN',
      isActive: true
    });
  };

  if (loading) {
    return <div className="crm-loading">Loading email templates...</div>;
  }

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>Email Templates</h1>
        <button className="crm-btn-primary" onClick={() => setShowForm(true)}>
          + New Template
        </button>
      </div>

      {showForm && portalLayoutOverlay(
        <div
          className={`crm-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: LAYOUT_OVERLAY_ROOT_Z,
          }}
          onClick={() => setShowForm(false)}
        >
          <div className="crm-modal" onClick={(e) => e.stopPropagation()}>
            <h2>{formData.templateId ? 'Edit Template' : 'New Template'}</h2>
            <form onSubmit={handleSubmit} className="crm-form">
              <div className="crm-form-group">
                <label>Template Name *</label>
                <input type="text" value={formData.templateName} onChange={(e) => setFormData({...formData, templateName: e.target.value})} required />
              </div>
              <div className="crm-form-group">
                <label>Template Code *</label>
                <input type="text" value={formData.templateCode} onChange={(e) => setFormData({...formData, templateCode: e.target.value})} required />
              </div>
              <div className="crm-form-group">
                <label>Template Type</label>
                <select value={formData.templateType} onChange={(e) => setFormData({...formData, templateType: e.target.value})}>
                  <option value="CAMPAIGN">Campaign</option>
                  <option value="FOLLOW_UP">Follow-up</option>
                  <option value="THANK_YOU">Thank You</option>
                  <option value="REMINDER">Reminder</option>
                  <option value="CUSTOM">Custom</option>
                </select>
              </div>
              <div className="crm-form-group crm-form-group-full">
                <label>Subject *</label>
                <input type="text" value={formData.subject} onChange={(e) => setFormData({...formData, subject: e.target.value})} required />
              </div>
              <div className="crm-form-group crm-form-group-full">
                <label>Body (HTML)</label>
                <textarea value={formData.bodyHtml} onChange={(e) => setFormData({...formData, bodyHtml: e.target.value})} rows={5} />
              </div>
              <div className="crm-form-group crm-form-group-full">
                <label>Body (Plain Text)</label>
                <textarea value={formData.bodyText} onChange={(e) => setFormData({...formData, bodyText: e.target.value})} rows={5} />
              </div>
              <div className="crm-form-group">
                <label>
                  <input type="checkbox" checked={formData.isActive} onChange={(e) => setFormData({...formData, isActive: e.target.checked})} />
                  {' '}Active
                </label>
              </div>
              <div className="crm-form-actions">
                <button type="button" className="crm-btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="crm-btn-primary">Save Template</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="crm-table-container">
        {templates.length === 0 ? (
          <p className="crm-empty-state">No email templates found</p>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Template Name</th>
                <th>Code</th>
                <th>Type</th>
                <th>Subject</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr key={template.templateId}>
                  <td><strong>{template.templateName}</strong></td>
                  <td>{template.templateCode}</td>
                  <td><span className="crm-badge">{template.templateType}</span></td>
                  <td>{template.subject}</td>
                  <td><span className={`crm-badge ${template.isActive ? 'crm-badge-active' : 'crm-badge-inactive'}`}>
                    {template.isActive ? 'Active' : 'Inactive'}
                  </span></td>
                  <td>
                    <button className="crm-btn-link" onClick={() => template.templateId && handleDelete(template.templateId)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default EmailTemplateManager;

