import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getGoals, createGoal } from '../../services/hrService';
import './Hr.css';

const GoalManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [goals, setGoals] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadGoals();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadGoals = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await getGoals(currentOrganizationId);
      setGoals(response.data);
    } catch (err) {
      console.error('Failed to load goals:', err);
      setError('Failed to load goals');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading goals...</div>;
  if (error) return <div className="error-message">{error}</div>;

  const activeGoals = goals.filter(g => g.status === 'IN_PROGRESS');
  const completedGoals = goals.filter(g => g.status === 'COMPLETED');
  const avgProgress = goals.length > 0 ? goals.reduce((sum, g) => sum + (g.progress || 0), 0) / goals.length : 0;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Goals Management</h1>
        <p>Set and track personal and team goals</p>
      </div>

      <div className="hr-summary-cards">
        <div className="hr-summary-card">
          <h3>Active Goals</h3>
          <div className="hr-card-value">{activeGoals.length}</div>
        </div>
        <div className="hr-summary-card">
          <h3>Completed</h3>
          <div className="hr-card-value">{completedGoals.length}</div>
        </div>
        <div className="hr-summary-card">
          <h3>Average Progress</h3>
          <div className="hr-card-value">{avgProgress.toFixed(0)}%</div>
        </div>
      </div>

      <div className="hr-section">
        <h2>My Goals</h2>
        {goals.length === 0 ? (
          <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
            No goals set. Start by setting your first goal!
          </p>
        ) : (
          <div className="goals-list">
            {goals.map((goal) => (
              <div key={goal.goalId} className="goal-card">
                <div className="goal-header">
                  <h3>{goal.title}</h3>
                  <span className={`status-badge status-${goal.status?.toLowerCase()}`}>
                    {goal.status}
                  </span>
                </div>
                <p className="goal-description">{goal.description || 'No description'}</p>
                <div className="goal-details">
                  <div className="goal-stat">
                    <span className="label">Category:</span>
                    <span className="value">{goal.category || 'N/A'}</span>
                  </div>
                  <div className="goal-stat">
                    <span className="label">Priority:</span>
                    <span className="value">{goal.priority || 'Medium'}</span>
                  </div>
                  <div className="goal-stat">
                    <span className="label">Target Date:</span>
                    <span className="value">{goal.targetDate ? new Date(goal.targetDate).toLocaleDateString() : 'N/A'}</span>
                  </div>
                </div>
                <div className="progress-section">
                  <div className="progress-header">
                    <span>Progress</span>
                    <span>{goal.progress || 0}%</span>
                  </div>
                  <div className="progress-bar">
                    <div 
                      className="progress-fill" 
                      style={{ 
                        width: `${goal.progress || 0}%`,
                        backgroundColor: (goal.progress || 0) >= 100 ? '#4caf50' : '#2196f3'
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default GoalManagement;

