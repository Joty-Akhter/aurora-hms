import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getCampaignById, createCampaign, updateCampaign } from '../../services/crmService';
import './Crm.css';

const CampaignForm: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);

  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  const [formData, setFormData] = useState({
    organizationId,
    campaignName: '',
    campaignType: 'EMAIL',
    status: 'PLANNING',
    startDate: '',
    endDate: '',
    budgetedCost: 0,
    actualCost: 0,
    currency: 'USD',
    description: '',
    objectives: '',
    targetAudience: '',
    priority: 'MEDIUM'
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit && id) {
      loadCampaign();
    }
  }, [id]);

  const loadCampaign = async () => {
    if (!id) return;
    try {
      const data = await getCampaignById(id);
      setFormData(data);
    } catch (error) {
      console.error('Error loading campaign:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEdit && id) {
        await updateCampaign(id, formData);
      } else {
        await createCampaign(formData);
      }
      navigate('/crm/campaigns');
    } catch (error) {
      console.error('Error saving campaign:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>{isEdit ? 'Edit Campaign' : 'New Campaign'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="crm-form">
        <div className="crm-form-grid">
          <div className="crm-form-group">
            <label>Campaign Name *</label>
            <input type="text" name="campaignName" value={formData.campaignName} onChange={handleChange} required />
          </div>

          <div className="crm-form-group">
            <label>Campaign Type *</label>
            <select name="campaignType" value={formData.campaignType} onChange={handleChange} required>
              <option value="EMAIL">Email</option>
              <option value="SOCIAL">Social Media</option>
              <option value="EVENT">Event</option>
              <option value="WEBINAR">Webinar</option>
              <option value="DIRECT_MAIL">Direct Mail</option>
              <option value="TELEMARKETING">Telemarketing</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Status</label>
            <select name="status" value={formData.status} onChange={handleChange}>
              <option value="PLANNING">Planning</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Priority</label>
            <select name="priority" value={formData.priority} onChange={handleChange}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>

          <div className="crm-form-group">
            <label>Start Date</label>
            <input type="date" name="startDate" value={formData.startDate} onChange={handleChange} />
          </div>

          <div className="crm-form-group">
            <label>End Date</label>
            <input type="date" name="endDate" value={formData.endDate} onChange={handleChange} />
          </div>

          <div className="crm-form-group">
            <label>Budgeted Cost</label>
            <input type="number" name="budgetedCost" value={formData.budgetedCost} onChange={handleChange} step="0.01" />
          </div>

          <div className="crm-form-group">
            <label>Actual Cost</label>
            <input type="number" name="actualCost" value={formData.actualCost} onChange={handleChange} step="0.01" />
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Description</label>
            <textarea name="description" value={formData.description} onChange={handleChange} rows={3} />
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Objectives</label>
            <textarea name="objectives" value={formData.objectives} onChange={handleChange} rows={2} />
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Target Audience</label>
            <textarea name="targetAudience" value={formData.targetAudience} onChange={handleChange} rows={2} />
          </div>
        </div>

        <div className="crm-form-actions">
          <button type="button" className="crm-btn-secondary" onClick={() => navigate('/crm/campaigns')} disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="crm-btn-primary" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Update Campaign' : 'Create Campaign')}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CampaignForm;

