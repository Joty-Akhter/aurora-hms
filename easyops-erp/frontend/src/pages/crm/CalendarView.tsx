import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getEvents, createEvent, updateEvent, deleteEvent } from '../../services/crmService';
import './Crm.css';

type EventRecord = {
  eventId?: string;
  organizationId: string;
  subject: string;
  description?: string;
  eventType?: string;
  startDatetime: string;
  endDatetime: string;
  location?: string;
  status?: string;
  organizerId?: string;
};

type EventFormState = Partial<EventRecord> & {
  organizationId: string;
};

const defaultEventValues = (organizationId: string, organizerId?: string): EventFormState => ({
  organizationId,
  subject: '',
  description: '',
  eventType: 'MEETING',
  startDatetime: '',
  endDatetime: '',
  location: '',
  status: 'PLANNED',
  organizerId,
});

const statusClassName = (status?: string) => {
  if (!status) return 'planned';
  return status.toLowerCase().replace(/_/g, '-');
};

const CalendarView: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();

  const [events, setEvents] = useState<EventRecord[]>([]);
  const [formData, setFormData] = useState<EventFormState | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setEvents([]);
      setFormData(null);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getEvents(
          currentOrganizationId,
          user?.id,
          statusFilter || undefined,
        );
        setEvents(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error('Failed to load events:', err);
        setError('Failed to load events');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId, user?.id, statusFilter]);

  useEffect(() => {
    if (!formData && currentOrganizationId) {
      setFormData(defaultEventValues(currentOrganizationId, user?.id));
    }
  }, [currentOrganizationId, user?.id, formData]);

  const openForm = (eventRecord?: EventRecord) => {
    if (!currentOrganizationId) return;
    setIsEditing(Boolean(eventRecord));
    setFormData(
      eventRecord
        ? { ...eventRecord, organizationId: currentOrganizationId }
        : defaultEventValues(currentOrganizationId, user?.id),
    );
  };

  const closeForm = () => {
    if (!currentOrganizationId) {
      setFormData(null);
      return;
    }
    setIsEditing(false);
    setFormData(defaultEventValues(currentOrganizationId, user?.id));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!formData || !currentOrganizationId) return;

    try {
      setLoading(true);
      setError(null);
      const payload: EventRecord = {
        ...formData,
        organizationId: currentOrganizationId,
        organizerId: formData.organizerId || user?.id,
      } as EventRecord;

      if (formData.eventId) {
        await updateEvent(formData.eventId, payload);
      } else {
        await createEvent(payload);
      }
      closeForm();
      const data = await getEvents(currentOrganizationId, user?.id, statusFilter || undefined);
      setEvents(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to save event:', err);
      setError('Failed to save event');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (eventId: string) => {
    if (!confirm('Delete this event?')) return;
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      await deleteEvent(eventId);
      const data = await getEvents(currentOrganizationId, user?.id, statusFilter || undefined);
      setEvents(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to delete event:', err);
      setError('Failed to delete event');
    } finally {
      setLoading(false);
    }
  };

  const totals = useMemo(
    () => ({
      total: events.length,
      upcoming: events.filter((event) => new Date(event.startDatetime) > new Date()).length,
    }),
    [events],
  );

  const formatDateTime = (value?: string) => {
    if (!value) return '--';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return '--';
    return parsed.toLocaleString();
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Calendar</h1>
          <p>Schedule demos, meetings, and client activities</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => openForm()}>
            + New Event
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
        <div className="crm-summary-card">
          <h3>Total Events</h3>
          <div className="crm-card-value">{totals.total}</div>
          <small>Managed for this organization</small>
        </div>
        <div className="crm-summary-card">
          <h3>Upcoming</h3>
          <div className="crm-card-value">{totals.upcoming}</div>
          <small>Scheduled after today</small>
        </div>
      </div>

      <div className="filters-section">
        <div className="filters-grid">
          <div className="form-row">
            <label htmlFor="event-status">Status</label>
            <select
              id="event-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="PLANNED">Planned</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>
      </div>

      {formData && (
        <div className="crm-form" style={{ marginBottom: 32 }}>
          <h2>{isEditing ? 'Edit Event' : 'Create Event'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="event-subject">Subject *</label>
                <input
                  id="event-subject"
                  name="subject"
                  value={formData.subject || ''}
                  onChange={(event) => setFormData({ ...formData, subject: event.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="event-type">Event Type</label>
                <select
                  id="event-type"
                  name="eventType"
                  value={formData.eventType || 'MEETING'}
                  onChange={(event) => setFormData({ ...formData, eventType: event.target.value })}
                >
                  <option value="MEETING">Meeting</option>
                  <option value="CALL">Call</option>
                  <option value="WEBINAR">Webinar</option>
                  <option value="CONFERENCE">Conference</option>
                  <option value="DEMO">Demo</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="event-status-input">Status</label>
                <select
                  id="event-status-input"
                  name="status"
                  value={formData.status || 'PLANNED'}
                  onChange={(event) => setFormData({ ...formData, status: event.target.value })}
                >
                  <option value="PLANNED">Planned</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="event-start">Start *</label>
                <input
                  id="event-start"
                  name="startDatetime"
                  type="datetime-local"
                  value={formData.startDatetime?.slice(0, 16) || ''}
                  onChange={(event) =>
                    setFormData({ ...formData, startDatetime: event.target.value })
                  }
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="event-end">End *</label>
                <input
                  id="event-end"
                  name="endDatetime"
                  type="datetime-local"
                  value={formData.endDatetime?.slice(0, 16) || ''}
                  onChange={(event) =>
                    setFormData({ ...formData, endDatetime: event.target.value })
                  }
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="event-location">Location</label>
                <input
                  id="event-location"
                  name="location"
                  value={formData.location || ''}
                  onChange={(event) => setFormData({ ...formData, location: event.target.value })}
                />
              </div>
              <div className="form-group form-group-full">
                <label htmlFor="event-description">Description</label>
                <textarea
                  id="event-description"
                  name="description"
                  rows={3}
                  value={formData.description || ''}
                  onChange={(event) =>
                    setFormData({ ...formData, description: event.target.value })
                  }
                />
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={closeForm}>
                Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? 'Saving...' : isEditing ? 'Update Event' : 'Create Event'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-wrapper">
        {loading ? (
          <div className="table-loading">
            <span className="spinner" /> Loading events...
          </div>
        ) : events.length === 0 ? (
          <div className="crm-empty-state">
            <p>No events match your filters.</p>
            <div className="empty-actions">
              <button className="btn-primary" onClick={() => openForm()}>
                Schedule Event
              </button>
            </div>
          </div>
        ) : (
          <table className="crm-table">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Type</th>
                <th>Status</th>
                <th>Start</th>
                <th>End</th>
                <th>Location</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {events.map((record) => (
                <tr key={record.eventId}>
                  <td>
                    <div className="crm-opportunity-cell">
                      <strong>{record.subject}</strong>
                      {record.description && <small>{record.description}</small>}
                    </div>
                  </td>
                  <td>{record.eventType || '--'}</td>
                  <td>
                    <span className={`status-badge status-${statusClassName(record.status)}`}>
                      {record.status || 'Planned'}
                    </span>
                  </td>
                  <td>{formatDateTime(record.startDatetime)}</td>
                  <td>{formatDateTime(record.endDatetime)}</td>
                  <td>{record.location || '--'}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-sm btn-secondary" onClick={() => openForm(record)}>
                        Edit
                      </button>
                      {record.eventId && (
                        <button className="btn-sm btn-disqualify" onClick={() => handleDelete(record.eventId!)}>
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

export default CalendarView;

