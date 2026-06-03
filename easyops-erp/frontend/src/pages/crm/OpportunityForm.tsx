import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getOpportunityById,
  createOpportunity,
  updateOpportunity,
  getAccounts,
  getOpportunityStages,
  Account,
  Opportunity,
  OpportunityStage,
} from '../../services/crmService';
import './Crm.css';

type OpportunityFormState = Partial<Opportunity> & {
  organizationId: string;
};

const defaultFormValues = (organizationId: string): OpportunityFormState => ({
  organizationId,
  opportunityName: '',
  accountId: '',
  stageId: '',
  type: 'NEW_BUSINESS',
  amount: 0,
  currency: 'BDT',
  probability: 0,
  expectedCloseDate: '',
  status: 'OPEN',
  priority: 'MEDIUM',
});

const OpportunityForm: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { currentOrganizationId } = useAuth();
  const isEdit = Boolean(id);

  const [formData, setFormData] = useState<OpportunityFormState | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [stages, setStages] = useState<OpportunityStage[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setFormData(null);
      setSubmitError('No organization selected');
      return;
    }

    const initialize = async () => {
      try {
        setLoading(true);
        setSubmitError(null);
        const [accountsResponse, stagesResponse] = await Promise.all([
          getAccounts(currentOrganizationId),
          getOpportunityStages(currentOrganizationId, true),
        ]);
        setAccounts(accountsResponse.data ?? []);
        setStages(stagesResponse ?? []);

        if (isEdit && id) {
          const opportunity = await getOpportunityById(id);
          setFormData({ ...opportunity, organizationId: currentOrganizationId });
        } else {
          setFormData(defaultFormValues(currentOrganizationId));
        }
      } catch (err) {
        console.error('Failed to load opportunity form data:', err);
        setSubmitError('Failed to load opportunity data');
      } finally {
        setLoading(false);
      }
    };

    initialize();
  }, [currentOrganizationId, id, isEdit]);

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target;
    setFormData((prev) => (prev ? { ...prev, [name]: value } : prev));
  };

  const handleNumberChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setFormData((prev) =>
      prev ? { ...prev, [name]: Number.isNaN(parseFloat(value)) ? 0 : parseFloat(value) } : prev,
    );
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!formData || !currentOrganizationId) {
      setSubmitError('Organization context missing');
      return;
    }

    try {
      setLoading(true);
      setSubmitError(null);
      const payload: Opportunity = {
        ...formData,
        organizationId: currentOrganizationId,
      } as Opportunity;

      if (isEdit && id) {
        await updateOpportunity(id, payload);
      } else {
        await createOpportunity(payload);
      }

      navigate('/crm/opportunities');
    } catch (err: any) {
      console.error('Failed to save opportunity:', err);
      setSubmitError(err?.message || 'Failed to save opportunity');
    } finally {
      setLoading(false);
    }
  };

  if (!formData) {
    return (
      <div className="crm-page">
        <div className="page-header">
          <h1>{isEdit ? 'Edit Opportunity' : 'New Opportunity'}</h1>
          <p>Loading opportunity information...</p>
        </div>
        <div className="table-loading">
          <span className="spinner" /> Preparing form...
        </div>
      </div>
    );
  }

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>{isEdit ? 'Edit Opportunity' : 'New Opportunity'}</h1>
          <p>Capture pipeline details and projected revenue</p>
        </div>
      </div>

      <form className="crm-form" onSubmit={handleSubmit}>
        {submitError && <div className="alert error" style={{ marginBottom: 24 }}>{submitError}</div>}

        <div className="form-section">
          <h2>Opportunity Overview</h2>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="opportunityName">Opportunity Name *</label>
              <input
                id="opportunityName"
                name="opportunityName"
                value={formData.opportunityName || ''}
                onChange={handleChange}
                required
                placeholder="Enter opportunity name"
              />
            </div>
            <div className="form-group">
              <label htmlFor="accountId">Account *</label>
              <select
                id="accountId"
                name="accountId"
                value={formData.accountId || ''}
                onChange={handleChange}
                required
              >
                <option value="">Select account</option>
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountName}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="stageId">Stage *</label>
              <select
                id="stageId"
                name="stageId"
                value={formData.stageId || ''}
                onChange={handleChange}
                required
              >
                <option value="">Select stage</option>
                {stages.map((stage) => (
                  <option key={stage.stageId} value={stage.stageId}>
                    {stage.stageName} ({stage.probability}%)
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="type">Opportunity Type *</label>
              <select id="type" name="type" value={formData.type || 'NEW_BUSINESS'} onChange={handleChange}>
                <option value="NEW_BUSINESS">New Business</option>
                <option value="EXISTING_BUSINESS">Existing Business</option>
                <option value="RENEWAL">Renewal</option>
                <option value="UPSELL">Upsell</option>
                <option value="CROSS_SELL">Cross-sell</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="priority">Priority</label>
              <select id="priority" name="priority" value={formData.priority || 'MEDIUM'} onChange={handleChange}>
                <option value="URGENT">Urgent</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="status">Status</label>
              <select id="status" name="status" value={formData.status || 'OPEN'} onChange={handleChange}>
                <option value="OPEN">Open</option>
                <option value="WON">Won</option>
                <option value="LOST">Lost</option>
                <option value="ABANDONED">Abandoned</option>
              </select>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h2>Revenue & Forecast</h2>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="amount">Amount *</label>
              <input
                id="amount"
                name="amount"
                type="number"
                min="0"
                step="0.01"
                value={formData.amount ?? 0}
                onChange={handleNumberChange}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="expectedRevenue">Expected Revenue</label>
              <input
                id="expectedRevenue"
                name="expectedRevenue"
                type="number"
                min="0"
                step="0.01"
                value={formData.expectedRevenue ?? 0}
                onChange={handleNumberChange}
              />
            </div>
            <div className="form-group">
              <label htmlFor="currency">Currency</label>
              <select id="currency" name="currency" value={formData.currency || 'BDT'} onChange={handleChange}>
                <option value="BDT">BDT (৳)</option>
                <option value="USD">USD ($)</option>
                <option value="EUR">EUR (€)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="probability">Probability %</label>
              <input
                id="probability"
                name="probability"
                type="number"
                min="0"
                max="100"
                value={formData.probability ?? 0}
                onChange={handleNumberChange}
              />
            </div>
            <div className="form-group">
              <label htmlFor="expectedCloseDate">Expected Close Date</label>
              <input
                id="expectedCloseDate"
                name="expectedCloseDate"
                type="date"
                value={formData.expectedCloseDate?.slice(0, 10) || ''}
                onChange={handleChange}
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h2>Additional Details</h2>
          <div className="form-grid">
            <div className="form-group form-group-full">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                rows={4}
                value={formData.description || ''}
                onChange={handleChange}
              />
            </div>
            <div className="form-group form-group-full">
              <label htmlFor="nextStep">Next Step</label>
              <textarea
                id="nextStep"
                name="nextStep"
                rows={3}
                value={formData.nextStep || ''}
                onChange={handleChange}
              />
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={() => navigate('/crm/opportunities')}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Update Opportunity' : 'Create Opportunity'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default OpportunityForm;
