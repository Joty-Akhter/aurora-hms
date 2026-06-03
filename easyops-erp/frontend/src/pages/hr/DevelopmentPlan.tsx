import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getDevelopmentPlans } from '../../services/hrService';
import './Hr.css';

const DevelopmentPlan: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [plans, setPlans] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadPlans();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadPlans = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await getDevelopmentPlans(currentOrganizationId);
      setPlans(response.data);
    } catch (err) {
      console.error('Failed to load development plans:', err);
      setError('Failed to load development plans');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading development plans...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Development Plans</h1>
        <p>Track your professional development and career growth</p>
      </div>

      <div className="hr-section">
        <h2>My Development Plans</h2>
        {plans.length === 0 ? (
          <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
            No development plans found. Work with your manager to create a development plan.
          </p>
        ) : (
          <div className="plans-grid">
            {plans.map((plan) => (
              <div key={plan.developmentPlanId} className="plan-card">
                <div className="plan-header">
                  <h3>{plan.planName || 'Development Plan'}</h3>
                  <span className={`status-badge status-${plan.status?.toLowerCase()}`}>
                    {plan.status}
                  </span>
                </div>
                <p className="plan-description">{plan.description || 'No description'}</p>
                <div className="plan-details">
                  <div className="plan-stat">
                    <span className="label">Focus Area:</span>
                    <span className="value">{plan.focusArea || 'General'}</span>
                  </div>
                  <div className="plan-stat">
                    <span className="label">Start Date:</span>
                    <span className="value">{plan.startDate ? new Date(plan.startDate).toLocaleDateString() : 'N/A'}</span>
                  </div>
                  <div className="plan-stat">
                    <span className="label">Target Date:</span>
                    <span className="value">{plan.targetDate ? new Date(plan.targetDate).toLocaleDateString() : 'N/A'}</span>
                  </div>
                  <div className="plan-stat">
                    <span className="label">Progress:</span>
                    <span className="value">{plan.completionPercentage || 0}%</span>
                  </div>
                </div>
                <div className="progress-bar">
                  <div 
                    className="progress-fill" 
                    style={{ width: `${plan.completionPercentage || 0}%` }}
                  />
                </div>
                {plan.notes && (
                  <div className="plan-notes">
                    <strong>Notes:</strong>
                    <p>{plan.notes}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="hr-section">
        <h2>Development Resources</h2>
        <div className="info-cards">
          <div className="info-card">
            <h3>📚 Learning Resources</h3>
            <p>Access online courses, books, and learning materials to support your growth.</p>
          </div>
          <div className="info-card">
            <h3>🎯 Skill Development</h3>
            <p>Identify skills gaps and create actionable plans to develop new competencies.</p>
          </div>
          <div className="info-card">
            <h3>👥 Mentorship</h3>
            <p>Connect with mentors and peers to accelerate your professional development.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DevelopmentPlan;

