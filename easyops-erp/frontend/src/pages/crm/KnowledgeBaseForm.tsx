import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getKnowledgeBaseArticleById, createKnowledgeBaseArticle, updateKnowledgeBaseArticle } from '../../services/crmService';
import './Crm.css';

const KnowledgeBaseForm: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);

  const organizationId = '123e4567-e89b-12d3-a456-426614174000';

  const [formData, setFormData] = useState({
    organizationId,
    title: '',
    summary: '',
    content: '',
    category: '',
    subcategory: '',
    status: 'DRAFT',
    isPublic: false,
    isFeatured: false
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit && id) {
      loadArticle();
    }
  }, [id]);

  const loadArticle = async () => {
    if (!id) return;
    try {
      const data = await getKnowledgeBaseArticleById(id);
      setFormData(data);
    } catch (error) {
      console.error('Error loading article:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEdit && id) {
        await updateKnowledgeBaseArticle(id, formData);
      } else {
        await createKnowledgeBaseArticle(formData);
      }
      navigate('/crm/knowledge-base');
    } catch (error) {
      console.error('Error saving article:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData(prev => ({ ...prev, [name]: checked }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  return (
    <div className="crm-container">
      <div className="crm-header">
        <h1>{isEdit ? 'Edit Article' : 'New Article'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="crm-form">
        <div className="crm-form-grid">
          <div className="crm-form-group crm-form-group-full">
            <label>Title *</label>
            <input type="text" name="title" value={formData.title} onChange={handleChange} required />
          </div>

          <div className="crm-form-group">
            <label>Category</label>
            <input type="text" name="category" value={formData.category} onChange={handleChange} />
          </div>

          <div className="crm-form-group">
            <label>Subcategory</label>
            <input type="text" name="subcategory" value={formData.subcategory} onChange={handleChange} />
          </div>

          <div className="crm-form-group">
            <label>Status</label>
            <select name="status" value={formData.status} onChange={handleChange}>
              <option value="DRAFT">Draft</option>
              <option value="PUBLISHED">Published</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Summary</label>
            <textarea name="summary" value={formData.summary} onChange={handleChange} rows={2} />
          </div>

          <div className="crm-form-group crm-form-group-full">
            <label>Content *</label>
            <textarea name="content" value={formData.content} onChange={handleChange} rows={10} required />
          </div>

          <div className="crm-form-group">
            <label>
              <input type="checkbox" name="isPublic" checked={formData.isPublic} onChange={handleChange} />
              {' '}Public (visible to customers)
            </label>
          </div>

          <div className="crm-form-group">
            <label>
              <input type="checkbox" name="isFeatured" checked={formData.isFeatured} onChange={handleChange} />
              {' '}Featured
            </label>
          </div>
        </div>

        <div className="crm-form-actions">
          <button type="button" className="crm-btn-secondary" onClick={() => navigate('/crm/knowledge-base')} disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="crm-btn-primary" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Update Article' : 'Create Article')}
          </button>
        </div>
      </form>
    </div>
  );
};

export default KnowledgeBaseForm;

