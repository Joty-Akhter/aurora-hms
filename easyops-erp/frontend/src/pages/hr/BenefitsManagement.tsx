import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getBenefits, getEmployeeBenefits, enrollEmployeeBenefit } from '../../services/hrService';
import './Hr.css';

const BenefitsManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [benefits, setBenefits] = useState<any[]>([]);
  const [myBenefits, setMyBenefits] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'available' | 'enrolled'>('enrolled');

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
      const [benefitsRes, myBenefitsRes] = await Promise.all([
        getBenefits(currentOrganizationId),
        getEmployeeBenefits(user?.id || '', currentOrganizationId),
      ]);
      setBenefits(benefitsRes.data);
      setMyBenefits(myBenefitsRes.data);
    } catch (err) {
      console.error('Failed to load benefits:', err);
      setError('Failed to load benefits');
    } finally {
      setLoading(false);
    }
  };

  const handleEnroll = async (benefitId: string) => {
    if (!user?.id || !currentOrganizationId) return;
    if (!confirm('Are you sure you want to enroll in this benefit?')) return;

    try {
      await enrollEmployeeBenefit({
        employeeId: user.id,
        benefitId,
        organizationId: currentOrganizationId,
      });
      loadData();
      alert('Successfully enrolled in benefit!');
    } catch (err) {
      console.error('Failed to enroll:', err);
      alert('Failed to enroll in benefit');
    }
  };

  if (loading) return <div className="loading">Loading benefits...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Benefits Management</h1>
        <p>View and manage employee benefits</p>
      </div>

      <div className="tabs">
        <button
          className={activeTab === 'enrolled' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('enrolled')}
        >
          My Benefits
        </button>
        <button
          className={activeTab === 'available' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('available')}
        >
          Available Benefits
        </button>
      </div>

      {activeTab === 'enrolled' && (
        <div className="hr-section">
          <h2>My Enrolled Benefits</h2>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Benefit</th>
                <th>Type</th>
                <th>Coverage</th>
                <th>Monthly Cost</th>
                <th>Start Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {myBenefits.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center' }}>
                    You are not enrolled in any benefits. Check available benefits to enroll.
                  </td>
                </tr>
              ) : (
                myBenefits.map((benefit) => (
                  <tr key={benefit.employeeBenefitId}>
                    <td><strong>{benefit.benefitName}</strong></td>
                    <td>{benefit.benefitType}</td>
                    <td>{benefit.coverageLevel || 'N/A'}</td>
                    <td>${benefit.employeeCost?.toLocaleString() || '0'}/mo</td>
                    <td>{benefit.startDate ? new Date(benefit.startDate).toLocaleDateString() : '-'}</td>
                    <td>
                      <span className={`status-badge status-${benefit.status?.toLowerCase()}`}>
                        {benefit.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'available' && (
        <div className="hr-section">
          <h2>Available Benefits</h2>
          <div className="benefits-grid">
            {benefits.length === 0 ? (
              <p style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
                No benefits available at this time.
              </p>
            ) : (
              benefits.map((benefit) => (
                <div key={benefit.benefitId} className="benefit-card">
                  <h3>{benefit.name}</h3>
                  <p className="benefit-type">{benefit.benefitType}</p>
                  <p className="benefit-description">{benefit.description || 'No description available'}</p>
                  <div className="benefit-details">
                    <div className="benefit-stat">
                      <span className="label">Employer Cost:</span>
                      <span className="value">${benefit.employerCost?.toLocaleString() || '0'}/mo</span>
                    </div>
                    <div className="benefit-stat">
                      <span className="label">Employee Cost:</span>
                      <span className="value">${benefit.employeeCost?.toLocaleString() || '0'}/mo</span>
                    </div>
                  </div>
                  <button onClick={() => handleEnroll(benefit.benefitId)} className="btn-primary btn-block">
                    Enroll
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default BenefitsManagement;

