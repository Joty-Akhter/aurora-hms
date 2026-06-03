import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getOpportunityById,
  getOpportunityProducts,
  getOpportunityActivities,
  deleteOpportunity,
  markOpportunityWon,
  markOpportunityLost
} from '../../services/crmService';
import './Crm.css';

const OpportunityDetail: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [opportunity, setOpportunity] = useState<any>(null);
  const [products, setProducts] = useState<any[]>([]);
  const [activities, setActivities] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('details');

  useEffect(() => {
    if (id) {
      loadOpportunityDetails();
    }
  }, [id]);

  const loadOpportunityDetails = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const [oppData, productsData, activitiesData] = await Promise.all([
        getOpportunityById(id),
        getOpportunityProducts(id),
        getOpportunityActivities(id)
      ]);
      setOpportunity(oppData);
      setProducts(productsData);
      setActivities(activitiesData);
    } catch (error) {
      console.error('Error loading opportunity details:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    if (window.confirm('Are you sure you want to delete this opportunity?')) {
      try {
        await deleteOpportunity(id);
        navigate('/crm/opportunities');
      } catch (error) {
        console.error('Error deleting opportunity:', error);
        alert('Failed to delete opportunity');
      }
    }
  };

  const handleMarkWon = async () => {
    if (!id) return;
    const winDescription = prompt('Enter win notes (optional):');
    try {
      await markOpportunityWon(id, winDescription || '');
      loadOpportunityDetails();
    } catch (error) {
      console.error('Error marking as won:', error);
    }
  };

  const handleMarkLost = async () => {
    if (!id) return;
    const lossReason = prompt('Enter loss reason:');
    if (lossReason) {
      try {
        await markOpportunityLost(id, lossReason, '');
        loadOpportunityDetails();
      } catch (error) {
        console.error('Error marking as lost:', error);
      }
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  if (loading || !opportunity) {
    return <div className="crm-loading">Loading opportunity details...</div>;
  }

  return (
    <div className="crm-container">
      <div className="crm-header">
        <div>
          <h1>{opportunity.opportunityName}</h1>
          <p className="crm-subtitle">{opportunity.opportunityNumber}</p>
        </div>
        <div className="crm-header-actions">
          {opportunity.status === 'OPEN' && (
            <>
              <button className="crm-btn-success" onClick={handleMarkWon}>
                Mark as Won
              </button>
              <button className="crm-btn-danger" onClick={handleMarkLost}>
                Mark as Lost
              </button>
            </>
          )}
          <button 
            className="crm-btn-secondary"
            onClick={() => navigate(`/crm/opportunities/${id}/edit`)}
          >
            Edit
          </button>
          <button className="crm-btn-danger" onClick={handleDelete}>
            Delete
          </button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="crm-stats-grid">
        <div className="crm-stat-card">
          <h3>Amount</h3>
          <p className="crm-stat-value">{formatCurrency(opportunity.amount)}</p>
        </div>
        <div className="crm-stat-card">
          <h3>Expected Revenue</h3>
          <p className="crm-stat-value">{formatCurrency(opportunity.expectedRevenue)}</p>
        </div>
        <div className="crm-stat-card">
          <h3>Status</h3>
          <p className="crm-stat-value">
            <span className={`crm-badge crm-badge-${opportunity.status.toLowerCase()}`}>
              {opportunity.status}
            </span>
          </p>
        </div>
        <div className="crm-stat-card">
          <h3>Probability</h3>
          <p className="crm-stat-value">{opportunity.probability}%</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="crm-tabs">
        <button
          className={`crm-tab ${activeTab === 'details' ? 'active' : ''}`}
          onClick={() => setActiveTab('details')}
        >
          Details
        </button>
        <button
          className={`crm-tab ${activeTab === 'products' ? 'active' : ''}`}
          onClick={() => setActiveTab('products')}
        >
          Products ({products.length})
        </button>
        <button
          className={`crm-tab ${activeTab === 'activities' ? 'active' : ''}`}
          onClick={() => setActiveTab('activities')}
        >
          Activities ({activities.length})
        </button>
      </div>

      {/* Tab Content */}
      <div className="crm-tab-content">
        {activeTab === 'details' && (
          <div className="crm-detail-grid">
            <div className="crm-detail-item">
              <label>Type</label>
              <span>{opportunity.type}</span>
            </div>
            <div className="crm-detail-item">
              <label>Priority</label>
              <span className={`crm-badge crm-badge-${opportunity.priority?.toLowerCase()}`}>
                {opportunity.priority}
              </span>
            </div>
            <div className="crm-detail-item">
              <label>Expected Close Date</label>
              <span>{opportunity.expectedCloseDate ? formatDate(opportunity.expectedCloseDate) : '-'}</span>
            </div>
            <div className="crm-detail-item">
              <label>Currency</label>
              <span>{opportunity.currency}</span>
            </div>
            <div className="crm-detail-item crm-detail-item-full">
              <label>Description</label>
              <p>{opportunity.description || 'No description'}</p>
            </div>
            <div className="crm-detail-item crm-detail-item-full">
              <label>Next Step</label>
              <p>{opportunity.nextStep || 'No next step defined'}</p>
            </div>
            {opportunity.competitors && (
              <div className="crm-detail-item crm-detail-item-full">
                <label>Competitors</label>
                <p>{opportunity.competitors}</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'products' && (
          <div className="crm-table-container">
            {products.length === 0 ? (
              <p className="crm-empty-state">No products added</p>
            ) : (
              <table className="crm-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Quantity</th>
                    <th>Unit Price</th>
                    <th>Discount</th>
                    <th>Tax</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.oppProductId}>
                      <td>
                        <strong>{product.productName}</strong>
                        {product.productCode && <small> ({product.productCode})</small>}
                      </td>
                      <td>{product.quantity}</td>
                      <td>{formatCurrency(product.unitPrice)}</td>
                      <td>{formatCurrency(product.discountTotal)}</td>
                      <td>{formatCurrency(product.taxAmount)}</td>
                      <td><strong>{formatCurrency(product.lineTotal)}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {activeTab === 'activities' && (
          <div className="crm-activities-list">
            {activities.length === 0 ? (
              <p className="crm-empty-state">No activities recorded</p>
            ) : (
              activities.map((activity) => (
                <div key={activity.activityId} className="crm-activity-item">
                  <div className="crm-activity-header">
                    <span className={`crm-badge crm-badge-${activity.activityType.toLowerCase()}`}>
                      {activity.activityType}
                    </span>
                    <span className="crm-activity-date">
                      {formatDate(activity.activityDate)}
                    </span>
                  </div>
                  <h4>{activity.subject}</h4>
                  {activity.description && <p>{activity.description}</p>}
                  {activity.outcome && (
                    <div className="crm-activity-outcome">
                      <strong>Outcome:</strong> {activity.outcome}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default OpportunityDetail;

