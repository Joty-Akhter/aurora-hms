import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getLeadById, convertLead, getLeadActivities, createLeadActivity } from '../../services/crmService';
import './Crm.css';

const LeadDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [lead, setLead] = useState<any>(null);
  const [activities, setActivities] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showActivityForm, setShowActivityForm] = useState(false);
  const [activityData, setActivityData] = useState({
    activityType: 'NOTE',
    subject: '',
    description: '',
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
      const [leadRes, activitiesRes] = await Promise.all([
        getLeadById(id),
        getLeadActivities(id),
      ]);
      setLead(leadRes.data);
      setActivities(activitiesRes.data);
    } catch (err) {
      console.error('Failed to load lead:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleConvert = async () => {
    if (!id || !user?.id) return;
    if (!confirm('Convert this lead to an Account and Contact?')) return;

    try {
      await convertLead(id, user.id);
      alert('Lead converted successfully!');
      navigate('/crm/leads');
    } catch (err) {
      alert('Failed to convert lead');
    }
  };

  const handleAddActivity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      await createLeadActivity(id, {
        ...activityData,
        organizationId: lead.organizationId,
        activityDate: new Date().toISOString(),
        status: 'COMPLETED',
      });
      setShowActivityForm(false);
      setActivityData({ activityType: 'NOTE', subject: '', description: '' });
      loadData();
    } catch (err) {
      alert('Failed to add activity');
    }
  };

  if (loading) return <div className="loading">Loading lead...</div>;
  if (!lead) return <div className="error-message">Lead not found</div>;

  return (
    <div className="crm-page">
      <div className="page-header">
        <h1>{lead.firstName} {lead.lastName}</h1>
        <p>{lead.company}</p>
        <div className="action-buttons">
          <button onClick={() => navigate(`/crm/leads/${id}/edit`)} className="btn-primary">
            Edit
          </button>
          {lead.status === 'QUALIFIED' && (
            <button onClick={handleConvert} className="btn-convert">
              Convert to Account
            </button>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        <div>
          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', marginBottom: '2rem' }}>
            <h2>Lead Information</h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginTop: '1rem' }}>
              <div>
                <strong>Email:</strong> {lead.email || '-'}
              </div>
              <div>
                <strong>Phone:</strong> {lead.phone || '-'}
              </div>
              <div>
                <strong>Mobile:</strong> {lead.mobile || '-'}
              </div>
              <div>
                <strong>Job Title:</strong> {lead.jobTitle || '-'}
              </div>
              <div>
                <strong>Industry:</strong> {lead.industry || '-'}
              </div>
              <div>
                <strong>Company Size:</strong> {lead.companySize || '-'}
              </div>
              <div>
                <strong>Website:</strong> {lead.website || '-'}
              </div>
              <div>
                <strong>Lead Score:</strong> {lead.leadScore || 0}
              </div>
            </div>
            {lead.notes && (
              <div style={{ marginTop: '1rem' }}>
                <strong>Notes:</strong>
                <p>{lead.notes}</p>
              </div>
            )}
          </div>

          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h2>Activities</h2>
              <button onClick={() => setShowActivityForm(!showActivityForm)} className="btn-primary btn-sm">
                + Add Activity
              </button>
            </div>

            {showActivityForm && (
              <form onSubmit={handleAddActivity} style={{ marginBottom: '2rem', padding: '1rem', background: '#f8f9fa', borderRadius: '4px' }}>
                <div className="form-row">
                  <label>Type</label>
                  <select
                    value={activityData.activityType}
                    onChange={(e) => setActivityData({ ...activityData, activityType: e.target.value })}
                  >
                    <option value="NOTE">Note</option>
                    <option value="CALL">Call</option>
                    <option value="EMAIL">Email</option>
                    <option value="MEETING">Meeting</option>
                  </select>
                </div>
                <div className="form-row">
                  <label>Subject</label>
                  <input
                    type="text"
                    value={activityData.subject}
                    onChange={(e) => setActivityData({ ...activityData, subject: e.target.value })}
                    required
                  />
                </div>
                <div className="form-row">
                  <label>Description</label>
                  <textarea
                    value={activityData.description}
                    onChange={(e) => setActivityData({ ...activityData, description: e.target.value })}
                    rows={3}
                  />
                </div>
                <div className="form-actions">
                  <button type="button" onClick={() => setShowActivityForm(false)} className="btn-secondary btn-sm">
                    Cancel
                  </button>
                  <button type="submit" className="btn-primary btn-sm">
                    Add Activity
                  </button>
                </div>
              </form>
            )}

            <div className="activity-timeline">
              {activities.length === 0 ? (
                <p style={{ textAlign: 'center', color: '#999' }}>No activities yet</p>
              ) : (
                activities.map((activity) => (
                  <div key={activity.activityId} className="activity-item">
                    <div className="activity-header">
                      <span className="activity-type">{activity.activityType}</span>
                      <span className="activity-date">
                        {new Date(activity.activityDate).toLocaleString()}
                      </span>
                    </div>
                    <div>
                      <strong>{activity.subject}</strong>
                      <p>{activity.description}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        <div>
          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', marginBottom: '2rem' }}>
            <h3>Status</h3>
            <div style={{ marginTop: '1rem' }}>
              <span className={`status-badge status-${lead.status?.toLowerCase()}`}>
                {lead.status}
              </span>
            </div>
            {lead.rating && (
              <div style={{ marginTop: '1rem' }}>
                <strong>Rating:</strong>
                <div style={{ marginTop: '0.5rem' }}>
                  <span className={`rating-${lead.rating?.toLowerCase()}`}>
                    {lead.rating}
                  </span>
                </div>
              </div>
            )}
            <div style={{ marginTop: '1rem' }}>
              <strong>Lead Score:</strong>
              <div style={{ marginTop: '0.5rem' }}>
                <span className={`lead-score lead-score-${lead.leadScore >= 70 ? 'high' : lead.leadScore >= 40 ? 'medium' : 'low'}`}>
                  {lead.leadScore}
                </span>
              </div>
            </div>
          </div>

          <div style={{ background: 'white', padding: '2rem', borderRadius: '8px' }}>
            <h3>Timeline</h3>
            <div style={{ marginTop: '1rem', fontSize: '0.875rem' }}>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>Created:</strong> {new Date(lead.createdAt).toLocaleDateString()}
              </div>
              {lead.qualifiedAt && (
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Qualified:</strong> {new Date(lead.qualifiedAt).toLocaleDateString()}
                </div>
              )}
              {lead.convertedAt && (
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Converted:</strong> {new Date(lead.convertedAt).toLocaleDateString()}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LeadDetail;













