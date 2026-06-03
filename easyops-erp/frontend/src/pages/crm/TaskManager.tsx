import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getTasks,
  getTaskStats,
  createTask,
  updateTask,
  deleteTask,
  completeTask,
} from '../../services/crmService';
import './Crm.css';

type TaskRecord = {
  taskId?: string;
  organizationId: string;
  subject: string;
  description?: string;
  taskType?: string;
  status?: string;
  priority?: string;
  dueDate?: string;
  assignedTo?: string;
};

type TaskFormState = Partial<TaskRecord> & {
  organizationId: string;
  assignedTo?: string;
};

const defaultTaskValues = (organizationId: string, assignedTo?: string): TaskFormState => ({
  organizationId,
  subject: '',
  description: '',
  taskType: 'TODO',
  status: 'NOT_STARTED',
  priority: 'MEDIUM',
  dueDate: '',
  assignedTo,
});

const TaskManager: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();

  const [tasks, setTasks] = useState<TaskRecord[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [formData, setFormData] = useState<TaskFormState | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setTasks([]);
      setStats(null);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [taskResponse, statsResponse] = await Promise.all([
          getTasks(currentOrganizationId, user?.id, statusFilter || undefined, priorityFilter || undefined),
          getTaskStats(currentOrganizationId, user?.id || ''),
        ]);
        const taskList = Array.isArray(taskResponse) ? taskResponse : [];
        setTasks(
          searchTerm
            ? taskList.filter((task) =>
                (task.subject || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                (task.description || '').toLowerCase().includes(searchTerm.toLowerCase()),
              )
            : taskList,
        );
        setStats(statsResponse ?? null);
      } catch (err) {
        console.error('Failed to load tasks:', err);
        setError('Failed to load tasks');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId, user?.id, statusFilter, priorityFilter, searchTerm]);

  const openForm = (task?: TaskRecord) => {
    if (!currentOrganizationId) return;
    setIsEditing(Boolean(task));
    setFormData(
      task
        ? { ...task, organizationId: currentOrganizationId }
        : defaultTaskValues(currentOrganizationId, user?.id),
    );
  };

  const closeForm = () => {
    setIsEditing(false);
    if (currentOrganizationId) {
      setFormData(defaultTaskValues(currentOrganizationId, user?.id));
    } else {
      setFormData(null);
    }
  };

  useEffect(() => {
    if (!formData && currentOrganizationId) {
      setFormData(defaultTaskValues(currentOrganizationId, user?.id));
    }
  }, [currentOrganizationId, user?.id, formData]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!formData || !currentOrganizationId) return;

    try {
      setLoading(true);
      setError(null);
      const payload: TaskRecord = {
        ...formData,
        organizationId: currentOrganizationId,
        assignedTo: formData.assignedTo || user?.id,
      } as TaskRecord;

      if (formData.taskId) {
        await updateTask(formData.taskId, payload);
      } else {
        await createTask(payload);
      }
      closeForm();
      const taskResponse = await getTasks(
        currentOrganizationId,
        user?.id,
        statusFilter || undefined,
        priorityFilter || undefined,
      );
      setTasks(Array.isArray(taskResponse) ? taskResponse : []);
    } catch (err) {
      console.error('Failed to save task:', err);
      setError('Failed to save task');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (taskId: string) => {
    if (!confirm('Delete this task?')) return;
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      await deleteTask(taskId);
      const taskResponse = await getTasks(
        currentOrganizationId,
        user?.id,
        statusFilter || undefined,
        priorityFilter || undefined,
      );
      setTasks(Array.isArray(taskResponse) ? taskResponse : []);
    } catch (err) {
      console.error('Failed to delete task:', err);
      setError('Failed to delete task');
    } finally {
      setLoading(false);
    }
  };

  const handleComplete = async (taskId: string) => {
    if (!currentOrganizationId || !user?.id) return;
    try {
      setLoading(true);
      await completeTask(taskId, user.id, 'Completed from CRM');
      const taskResponse = await getTasks(
        currentOrganizationId,
        user.id,
        statusFilter || undefined,
        priorityFilter || undefined,
      );
      setTasks(Array.isArray(taskResponse) ? taskResponse : []);
    } catch (err) {
      console.error('Failed to complete task:', err);
      setError('Failed to complete task');
    } finally {
      setLoading(false);
    }
  };

  const priorityBadge = (priority?: string) => {
    if (!priority) return 'priority-low';
    switch (priority) {
      case 'HIGH':
        return 'priority-high';
      case 'MEDIUM':
        return 'priority-medium';
      case 'LOW':
      default:
        return 'priority-low';
    }
  };

  const statusBadge = (status?: string) => {
    const normalized = status?.toLowerCase() ?? 'planned';
    if (normalized === 'completed') return 'status-converted';
    if (normalized === 'in_progress') return 'status-in-progress';
    if (normalized === 'deferred') return 'status-nurturing';
    return 'status-planned';
  };

  const totals = useMemo(
    () => ({
      total: tasks.length,
      completed: tasks.filter((task) => task.status === 'COMPLETED').length,
      overdue: tasks.filter((task) => task.status === 'OVERDUE').length,
    }),
    [tasks],
  );

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Tasks</h1>
          <p>Stay on top of follow-ups, calls, and meetings</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => openForm()}>
            + New Task
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
        <div className="crm-summary-card">
          <h3>Total Tasks</h3>
          <div className="crm-card-value">{totals.total}</div>
          <small>Assigned to you</small>
        </div>
        <div className="crm-summary-card">
          <h3>Completed</h3>
          <div className="crm-card-value">{totals.completed}</div>
          <small>Marked as done</small>
        </div>
        <div className="crm-summary-card">
          <h3>Overdue</h3>
          <div className="crm-card-value">{totals.overdue}</div>
          <small>Past due date</small>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="task-status">Status</label>
            <select
              id="task-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="NOT_STARTED">Not Started</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="OVERDUE">Overdue</option>
            </select>
          </div>
          <div className="form-row">
            <label htmlFor="task-priority">Priority</label>
            <select
              id="task-priority"
              value={priorityFilter}
              onChange={(event) => setPriorityFilter(event.target.value)}
            >
              <option value="">All Priorities</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
          <div className="form-row">
            <label htmlFor="task-search">Search</label>
            <input
              id="task-search"
              type="text"
              placeholder="Search by subject or description"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
        </div>
      </div>

      {formData && (
        <div className="crm-form" style={{ marginBottom: 32 }}>
          <h2>{isEditing ? 'Edit Task' : 'Create Task'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="task-subject">Subject *</label>
                <input
                  id="task-subject"
                  name="subject"
                  value={formData.subject || ''}
                  onChange={(event) => setFormData({ ...formData, subject: event.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="task-type">Type</label>
                <select
                  id="task-type"
                  name="taskType"
                  value={formData.taskType || 'TODO'}
                  onChange={(event) => setFormData({ ...formData, taskType: event.target.value })}
                >
                  <option value="TODO">To-Do</option>
                  <option value="CALL">Call</option>
                  <option value="EMAIL">Email</option>
                  <option value="MEETING">Meeting</option>
                  <option value="FOLLOW_UP">Follow Up</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="task-priority-input">Priority</label>
                <select
                  id="task-priority-input"
                  name="priority"
                  value={formData.priority || 'MEDIUM'}
                  onChange={(event) => setFormData({ ...formData, priority: event.target.value })}
                >
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="task-status-input">Status</label>
                <select
                  id="task-status-input"
                  name="status"
                  value={formData.status || 'NOT_STARTED'}
                  onChange={(event) => setFormData({ ...formData, status: event.target.value })}
                >
                  <option value="NOT_STARTED">Not Started</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="DEFERRED">Deferred</option>
                  <option value="OVERDUE">Overdue</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="task-due">Due Date</label>
                <input
                  id="task-due"
                  name="dueDate"
                  type="date"
                  value={formData.dueDate ? formData.dueDate.slice(0, 10) : ''}
                  onChange={(event) => setFormData({ ...formData, dueDate: event.target.value })}
                />
              </div>
              <div className="form-group">
                <label htmlFor="task-assignee">Assigned To</label>
                <input
                  id="task-assignee"
                  name="assignedTo"
                  value={formData.assignedTo || user?.id || ''}
                  onChange={(event) => setFormData({ ...formData, assignedTo: event.target.value })}
                />
              </div>
              <div className="form-group form-group-full">
                <label htmlFor="task-description">Description</label>
                <textarea
                  id="task-description"
                  name="description"
                  rows={3}
                  value={formData.description || ''}
                  onChange={(event) => setFormData({ ...formData, description: event.target.value })}
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={closeForm}>
                Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? 'Saving...' : isEditing ? 'Update Task' : 'Create Task'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading tasks...
          </div>
        ) : tasks.length === 0 ? (
          <div className="crm-empty-state">
            <p>No tasks match your current filters.</p>
            <div className="empty-actions">
              <button className="btn-primary" onClick={() => openForm()}>
                Create Task
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Due</th>
                <th>Assigned To</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => (
                <tr key={task.taskId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{task.subject}</strong>
                      {task.description && <small>{task.description}</small>}
                    </div>
                  </td>
                  <td>{task.taskType || '--'}</td>
                  <td>
                    <span className={`priority-badge ${priorityBadge(task.priority)}`}>
                      {task.priority || 'LOW'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${statusBadge(task.status)}`}>
                      {task.status || 'NOT_STARTED'}
                    </span>
                  </td>
                  <td>{task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '--'}</td>
                  <td>{task.assignedTo || user?.username || '--'}</td>
                  <td>
                    <div className="action-buttons">
                      {task.status !== 'COMPLETED' && task.taskId && (
                        <button className="btn-sm btn-primary" onClick={() => handleComplete(task.taskId!)}>
                          Complete
                        </button>
                      )}
                      <button className="btn-sm btn-secondary" onClick={() => openForm(task)}>
                        Edit
                      </button>
                      {task.taskId && (
                        <button className="btn-sm btn-disqualify" onClick={() => handleDelete(task.taskId!)}>
                          Delete
                        </button>
                      )}
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

export default TaskManager;
