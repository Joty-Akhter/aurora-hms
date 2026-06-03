import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getKnowledgeBaseArticles, deleteKnowledgeBaseArticle } from '../../services/crmService';
import './Crm.css';

interface Article {
  articleId: string;
  articleNumber: string;
  title: string;
  category?: string;
  status: string;
  viewCount: number;
  helpfulCount: number;
  isPublic: boolean;
  isFeatured: boolean;
}

const KnowledgeBaseList: React.FC = () => {
  const navigate = useNavigate();
  const [articles, setArticles] = useState<Article[]>([]);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [loading, setLoading] = useState(true);
  
  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  useEffect(() => {
    loadArticles();
  }, [categoryFilter]);

  const loadArticles = async () => {
    try {
      setLoading(true);
      const data = await getKnowledgeBaseArticles(organizationId, false, false, categoryFilter);
      setArticles(data);
    } catch (error) {
      console.error('Error loading articles:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Delete this article?')) {
      try {
        await deleteKnowledgeBaseArticle(id);
        loadArticles();
      } catch (error) {
        console.error('Error deleting article:', error);
      }
    }
  };

  if (loading) {
    return <div className="crm-loading">Loading knowledge base...</div>;
  }

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>Knowledge Base</h1>
        <button className="crm-btn-primary" onClick={() => navigate('/crm/knowledge-base/new')}>
          + New Article
        </button>
      </div>

      <div className="crm-table-container">
        {articles.length === 0 ? (
          <div className="crm-empty-state">
            <p>No articles found</p>
            <button className="crm-btn-primary" onClick={() => navigate('/crm/knowledge-base/new')}>
              Create First Article
            </button>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Status</th>
                <th>Views</th>
                <th>Helpful</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {articles.map((article) => (
                <tr key={article.articleId}>
                  <td>
                    <strong>{article.title}</strong>
                    <br />
                    <small>{article.articleNumber}</small>
                    {article.isFeatured && <span className="crm-badge crm-badge-featured">⭐ Featured</span>}
                  </td>
                  <td>{article.category}</td>
                  <td><span className={`crm-badge crm-badge-${article.status.toLowerCase()}`}>{article.status}</span></td>
                  <td>{article.viewCount}</td>
                  <td>{article.helpfulCount}</td>
                  <td>
                    <div className="crm-action-buttons">
                      <button className="crm-btn-link" onClick={() => navigate(`/crm/knowledge-base/${article.articleId}`)}>View</button>
                      <button className="crm-btn-link" onClick={() => navigate(`/crm/knowledge-base/${article.articleId}/edit`)}>Edit</button>
                      <button className="crm-btn-link crm-btn-danger" onClick={() => handleDelete(article.articleId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default KnowledgeBaseList;

