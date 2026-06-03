import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getPerformanceReviews, getPerformanceCycles } from '../../services/hrService';
import './Hr.css';

const PerformanceManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [reviews, setReviews] = useState<any[]>([]);
  const [cycles, setCycles] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const [reviewsRes, cyclesRes] = await Promise.all([
        getPerformanceReviews(currentOrganizationId),
        getPerformanceCycles(currentOrganizationId),
      ]);
      setReviews(reviewsRes.data);
      setCycles(cyclesRes.data);
    } catch (err) {
      console.error('Failed to load performance data:', err);
      setError('Failed to load performance data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading performance reviews...</div>;
  if (error) return <div className="error-message">{error}</div>;

  const activeCycle = cycles.find(c => c.status === 'ACTIVE');
  const completedReviews = reviews.filter(r => r.status === 'COMPLETED');
  const pendingReviews = reviews.filter(r => r.status === 'PENDING' || r.status === 'IN_PROGRESS');

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Performance Reviews</h1>
        <p>Manage and track employee performance evaluations</p>
      </div>

      {activeCycle && (
        <div className="alert alert-info">
          <h3>📅 Active Review Cycle: {activeCycle.cycleName}</h3>
          <p>
            Period: {new Date(activeCycle.startDate).toLocaleDateString()} - {new Date(activeCycle.endDate).toLocaleDateString()}
          </p>
        </div>
      )}

      <div className="hr-summary-cards">
        <div className="hr-summary-card">
          <h3>Total Reviews</h3>
          <div className="hr-card-value">{reviews.length}</div>
        </div>
        <div className="hr-summary-card">
          <h3>Completed</h3>
          <div className="hr-card-value">{completedReviews.length}</div>
        </div>
        <div className="hr-summary-card">
          <h3>Pending</h3>
          <div className="hr-card-value">{pendingReviews.length}</div>
        </div>
      </div>

      <div className="hr-section">
        <h2>My Performance Reviews</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Review Cycle</th>
              <th>Review Type</th>
              <th>Reviewer</th>
              <th>Period</th>
              <th>Overall Rating</th>
              <th>Status</th>
              <th>Completed Date</th>
            </tr>
          </thead>
          <tbody>
            {reviews.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center' }}>
                  No performance reviews found.
                </td>
              </tr>
            ) : (
              reviews.map((review) => (
                <tr key={review.reviewId}>
                  <td>{review.cycleName || 'N/A'}</td>
                  <td>{review.reviewType || 'Annual'}</td>
                  <td>{review.reviewerName || '-'}</td>
                  <td>
                    {review.periodStart && review.periodEnd
                      ? `${new Date(review.periodStart).toLocaleDateString()} - ${new Date(review.periodEnd).toLocaleDateString()}`
                      : '-'}
                  </td>
                  <td>
                    {review.overallRating ? (
                      <div className="rating-display">
                        {'⭐'.repeat(review.overallRating)}
                        {' '}
                        {review.overallRating}/5
                      </div>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td>
                    <span className={`status-badge status-${review.status?.toLowerCase()}`}>
                      {review.status}
                    </span>
                  </td>
                  <td>{review.completedAt ? new Date(review.completedAt).toLocaleDateString() : '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="hr-section">
        <h2>Review Cycles</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Cycle Name</th>
              <th>Period</th>
              <th>Type</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {cycles.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center' }}>
                  No review cycles configured.
                </td>
              </tr>
            ) : (
              cycles.map((cycle) => (
                <tr key={cycle.cycleId}>
                  <td><strong>{cycle.cycleName}</strong></td>
                  <td>
                    {new Date(cycle.startDate).toLocaleDateString()} - {new Date(cycle.endDate).toLocaleDateString()}
                  </td>
                  <td>{cycle.cycleType || 'Annual'}</td>
                  <td>
                    <span className={`status-badge status-${cycle.status?.toLowerCase()}`}>
                      {cycle.status}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PerformanceManagement;

