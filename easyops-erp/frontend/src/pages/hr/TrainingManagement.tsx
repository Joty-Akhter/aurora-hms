import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getTrainingCertifications } from '../../services/hrService';
import './Hr.css';

const TrainingManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [trainings, setTrainings] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'training' | 'certifications'>('training');

  useEffect(() => {
    if (currentOrganizationId) {
      loadTrainings();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  const loadTrainings = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await getTrainingCertifications(currentOrganizationId);
      setTrainings(response.data);
    } catch (err) {
      console.error('Failed to load trainings:', err);
      setError('Failed to load trainings');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading trainings...</div>;
  if (error) return <div className="error-message">{error}</div>;

  const trainingCourses = trainings.filter(t => t.type === 'TRAINING');
  const certifications = trainings.filter(t => t.type === 'CERTIFICATION');

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Training & Certifications</h1>
        <p>Manage employee training programs and certifications</p>
      </div>

      <div className="tabs">
        <button
          className={activeTab === 'training' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('training')}
        >
          Training Courses
        </button>
        <button
          className={activeTab === 'certifications' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('certifications')}
        >
          Certifications
        </button>
      </div>

      {activeTab === 'training' && (
        <div className="hr-section">
          <h2>My Training Courses</h2>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Course Name</th>
                <th>Provider</th>
                <th>Type</th>
                <th>Start Date</th>
                <th>Completion Date</th>
                <th>Status</th>
                <th>Certificate</th>
              </tr>
            </thead>
            <tbody>
              {trainingCourses.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center' }}>
                    No training courses found. Check with HR for available training programs.
                  </td>
                </tr>
              ) : (
                trainingCourses.map((course) => (
                  <tr key={course.trainingCertificationId}>
                    <td><strong>{course.courseName || 'N/A'}</strong></td>
                    <td>{course.provider || '-'}</td>
                    <td>{course.trainingType || 'General'}</td>
                    <td>{course.startDate ? new Date(course.startDate).toLocaleDateString() : '-'}</td>
                    <td>{course.completionDate ? new Date(course.completionDate).toLocaleDateString() : '-'}</td>
                    <td>
                      <span className={`status-badge status-${course.status?.toLowerCase()}`}>
                        {course.status}
                      </span>
                    </td>
                    <td>
                      {course.certificateUrl ? (
                        <a href={course.certificateUrl} target="_blank" rel="noopener noreferrer" className="btn-sm">
                          View
                        </a>
                      ) : (
                        '-'
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'certifications' && (
        <div className="hr-section">
          <h2>My Certifications</h2>
          <table className="hr-table">
            <thead>
              <tr>
                <th>Certification Name</th>
                <th>Issuing Organization</th>
                <th>Issue Date</th>
                <th>Expiry Date</th>
                <th>Status</th>
                <th>Certificate</th>
              </tr>
            </thead>
            <tbody>
              {certifications.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center' }}>
                    No certifications found. Add your professional certifications here.
                  </td>
                </tr>
              ) : (
                certifications.map((cert) => (
                  <tr key={cert.trainingCertificationId}>
                    <td><strong>{cert.certificationName || cert.courseName || 'N/A'}</strong></td>
                    <td>{cert.provider || cert.issuingOrganization || '-'}</td>
                    <td>{cert.issueDate ? new Date(cert.issueDate).toLocaleDateString() : '-'}</td>
                    <td>
                      {cert.expiryDate ? (
                        <span style={{ 
                          color: new Date(cert.expiryDate) < new Date() ? '#f44336' : 'inherit' 
                        }}>
                          {new Date(cert.expiryDate).toLocaleDateString()}
                        </span>
                      ) : (
                        'No expiry'
                      )}
                    </td>
                    <td>
                      <span className={`status-badge status-${cert.status?.toLowerCase()}`}>
                        {cert.status}
                      </span>
                    </td>
                    <td>
                      {cert.certificateUrl ? (
                        <a href={cert.certificateUrl} target="_blank" rel="noopener noreferrer" className="btn-sm">
                          View
                        </a>
                      ) : (
                        '-'
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      <div className="hr-section">
        <h2>Training Categories</h2>
        <div className="info-cards">
          <div className="info-card">
            <h3>💻 Technical Training</h3>
            <p>Software, tools, and technical skill development programs.</p>
          </div>
          <div className="info-card">
            <h3>🎯 Professional Development</h3>
            <p>Leadership, communication, and soft skills training.</p>
          </div>
          <div className="info-card">
            <h3>🛡️ Compliance & Safety</h3>
            <p>Mandatory compliance, safety, and regulatory training.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TrainingManagement;

