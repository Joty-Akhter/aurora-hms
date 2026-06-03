import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getCaseById, getCaseComments, addCaseComment, resolveCase, closeCase } from '../../services/crmService';
import './Crm.css';

const CaseDetail: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [caseData, setCaseData] = useState<any>(null);
  const [comments, setComments] = useState<any[]>([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const userId = '123e4567-e89b-12d3-a456-426614174001';

  useEffect(() => {
    if (id) {
      loadCaseDetails();
    }
  }, [id]);

  const loadCaseDetails = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const [caseDataResult, commentsData] = await Promise.all([
        getCaseById(id),
        getCaseComments(id)
      ]);
      setCaseData(caseDataResult);
      setComments(commentsData);
    } catch (error) {
      console.error('Error loading case details:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = async () => {
    if (!id || !newComment.trim()) return;
    try {
      await addCaseComment(id, {
        caseId: id,
        organizationId: caseData.organizationId,
        commentText: newComment,
        commentType: 'EXTERNAL',
        isPublic: true,
        createdBy: userId
      });
      setNewComment('');
      loadCaseDetails();
    } catch (error) {
      console.error('Error adding comment:', error);
    }
  };

  const handleResolve = async () => {
    if (!id) return;
    const resolution = prompt('Enter resolution:');
    if (resolution) {
      try {
        await resolveCase(id, userId, resolution);
        loadCaseDetails();
      } catch (error) {
        console.error('Error resolving case:', error);
      }
    }
  };

  const handleClose = async () => {
    if (!id) return;
    if (window.confirm('Close this case?')) {
      try {
        await closeCase(id);
        loadCaseDetails();
      } catch (error) {
        console.error('Error closing case:', error);
      }
    }
  };

  if (loading || !caseData) {
    return <div className="crm-loading">Loading case details...</div>;
  }

  return (
    <div className="crm-container">
      <div className="crm-header">
        <div>
          <h1>{caseData.subject}</h1>
          <p className="crm-subtitle">{caseData.caseNumber}</p>
        </div>
        <div className="crm-header-actions">
          {caseData.status !== 'RESOLVED' && caseData.status !== 'CLOSED' && (
            <button className="crm-btn-success" onClick={handleResolve}>
              Resolve
            </button>
          )}
          {caseData.status === 'RESOLVED' && (
            <button className="crm-btn-primary" onClick={handleClose}>
              Close
            </button>
          )}
          <button className="crm-btn-secondary" onClick={() => navigate(`/crm/cases/${id}/edit`)}>
            Edit
          </button>
        </div>
      </div>

      <div className="crm-stats-grid">
        <div className="crm-stat-card">
          <h3>Status</h3>
          <p><span className={`crm-badge crm-badge-${caseData.status.toLowerCase()}`}>{caseData.status}</span></p>
        </div>
        <div className="crm-stat-card">
          <h3>Priority</h3>
          <p><span className={`crm-badge crm-badge-${caseData.priority.toLowerCase()}`}>{caseData.priority}</span></p>
        </div>
        <div className="crm-stat-card">
          <h3>Type</h3>
          <p>{caseData.caseType}</p>
        </div>
        <div className="crm-stat-card">
          <h3>Origin</h3>
          <p>{caseData.origin}</p>
        </div>
      </div>

      <div className="crm-section">
        <h2>Description</h2>
        <p>{caseData.description}</p>
      </div>

      {caseData.resolution && (
        <div className="crm-section">
          <h2>Resolution</h2>
          <p>{caseData.resolution}</p>
        </div>
      )}

      <div className="crm-section">
        <h2>Comments ({comments.length})</h2>
        <div className="crm-comment-list">
          {comments.map((comment) => (
            <div key={comment.commentId} className="crm-comment-item">
              <div className="crm-comment-meta">
                <span className="crm-badge">{comment.commentType}</span>
                <span>{new Date(comment.createdAt).toLocaleString()}</span>
              </div>
              <p>{comment.commentText}</p>
            </div>
          ))}
        </div>
        
        {caseData.status !== 'CLOSED' && (
          <div className="crm-comment-form">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Add a comment..."
              rows={3}
            />
            <button className="crm-btn-primary" onClick={handleAddComment}>
              Add Comment
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default CaseDetail;

