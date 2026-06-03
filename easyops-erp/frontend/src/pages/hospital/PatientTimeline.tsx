import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { PatientTimeline, TimelineEvent } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const PatientTimelinePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [timeline, setTimeline] = useState<PatientTimeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );

  useEffect(() => {
    if (id) {
      loadTimeline();
    }
  }, [id, startDate, endDate]);

  const loadTimeline = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await hospitalService.getPatientTimeline(id, startDate, endDate);
      setTimeline(response.data);
    } catch (err: any) {
      console.error('Failed to load patient timeline:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load patient timeline'));
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString: string, timeString?: string) => {
    if (timeString) {
      return `${new Date(dateString).toLocaleDateString()} ${timeString}`;
    }
    return new Date(dateString).toLocaleDateString();
  };

  const getEventIcon = (eventType: string) => {
    switch (eventType) {
      case 'VITAL_SIGNS':
        return '📊';
      case 'CLINICAL_NOTE':
        return '📝';
      case 'PRESCRIPTION':
        return '💊';
      case 'PROBLEM':
        return '⚠️';
      case 'IMMUNIZATION':
        return '💉';
      case 'ALLERGY':
        return '🚫';
      default:
        return '📋';
    }
  };

  const getEventColor = (eventType: string) => {
    switch (eventType) {
      case 'VITAL_SIGNS':
        return '#3b82f6';
      case 'CLINICAL_NOTE':
        return '#8b5cf6';
      case 'PRESCRIPTION':
        return '#10b981';
      case 'PROBLEM':
        return '#ef4444';
      case 'IMMUNIZATION':
        return '#f59e0b';
      case 'ALLERGY':
        return '#ef4444';
      default:
        return '#6b7280';
    }
  };

  // Group events by date
  const groupedEvents = timeline?.events.reduce((acc, event) => {
    const dateKey = event.eventDate;
    if (!acc[dateKey]) {
      acc[dateKey] = [];
    }
    acc[dateKey].push(event);
    return acc;
  }, {} as Record<string, TimelineEvent[]>) || {};

  const sortedDates = Object.keys(groupedEvents).sort((a, b) => 
    new Date(b).getTime() - new Date(a).getTime()
  );

  if (loading && !timeline) {
    return <div className="loading">Loading patient timeline...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px', alignItems: 'center' }}>
        <h3>Patient Timeline</h3>
        <button className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
          Back to Overview
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Date Range Filter */}
      <div className="filters-section" style={{ marginBottom: '24px' }}>
        <div className="filter-row">
          <label>Start Date:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="search-input"
            style={{ width: 'auto' }}
          />
          <label>End Date:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="search-input"
            style={{ width: 'auto' }}
          />
          <button className="btn-secondary" onClick={loadTimeline}>Refresh</button>
        </div>
      </div>

      {/* Timeline Summary */}
      {timeline && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <div className="info-row">
            <span className="info-label">Date Range:</span>
            <span className="info-value">
              {formatDate(timeline.startDate)} to {formatDate(timeline.endDate)}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">Total Events:</span>
            <span className="info-value">{timeline.totalEvents}</span>
          </div>
        </div>
      )}

      {/* Timeline Events */}
      {sortedDates.length === 0 ? (
        <div className="empty-state">
          <p>No events found in the selected date range</p>
        </div>
      ) : (
        <div style={{ position: 'relative' }}>
          {sortedDates.map((date, dateIndex) => (
            <div key={date} style={{ marginBottom: '32px' }}>
              {/* Date Header */}
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                marginBottom: '16px',
                paddingBottom: '8px',
                borderBottom: '2px solid #e5e7eb'
              }}>
                <div style={{ 
                  fontSize: '18px', 
                  fontWeight: 600, 
                  color: '#374151',
                  marginRight: '12px'
                }}>
                  {formatDate(date)}
                </div>
                <div style={{ 
                  fontSize: '14px', 
                  color: '#6b7280',
                  background: '#f3f4f6',
                  padding: '4px 12px',
                  borderRadius: '12px'
                }}>
                  {groupedEvents[date].length} event(s)
                </div>
              </div>

              {/* Events for this date */}
              <div style={{ paddingLeft: '24px', borderLeft: '2px solid #e5e7eb', marginLeft: '12px' }}>
                {groupedEvents[date].map((event, eventIndex) => (
                  <div 
                    key={`${date}-${eventIndex}`}
                    style={{
                      marginBottom: '20px',
                      padding: '16px',
                      background: '#ffffff',
                      border: `2px solid ${getEventColor(event.eventType)}`,
                      borderRadius: '8px',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                      position: 'relative'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                      <div style={{ 
                        fontSize: '24px',
                        width: '40px',
                        height: '40px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background: `${getEventColor(event.eventType)}20`,
                        borderRadius: '8px',
                        flexShrink: 0
                      }}>
                        {getEventIcon(event.eventType)}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                          <div>
                            <div style={{ fontWeight: 600, fontSize: '16px', marginBottom: '4px' }}>
                              {event.title}
                            </div>
                            <div style={{ fontSize: '12px', color: '#6b7280', textTransform: 'uppercase' }}>
                              {event.eventType.replace('_', ' ')}
                            </div>
                          </div>
                          {event.eventTime && (
                            <div style={{ fontSize: '14px', color: '#6b7280' }}>
                              {event.eventTime}
                            </div>
                          )}
                        </div>
                        {event.description && (
                          <div style={{ 
                            marginTop: '8px', 
                            padding: '8px', 
                            background: '#f9fafb', 
                            borderRadius: '4px',
                            fontSize: '14px',
                            color: '#374151'
                          }}>
                            {event.description}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default PatientTimelinePage;
