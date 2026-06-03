import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import hospitalService, { VitalSignsTrend, VitalSignsSummary } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const VitalSignsTrendsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<VitalSignsSummary | null>(null);
  const [trends, setTrends] = useState<VitalSignsTrend[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );

  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id, startDate]);

  const loadData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const [summaryRes, trendsRes] = await Promise.all([
        hospitalService.getVitalSignsSummary(id),
        hospitalService.getVitalSignsTrends(id, startDate)
      ]);
      setSummary(summaryRes.data);
      setTrends(trendsRes.data);
    } catch (err: any) {
      console.error('Failed to load vital signs trends:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load vital signs trends'));
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="loading">Loading trends...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => navigate(-1)}
            aria-label="Go back"
          >
            Back
          </button>
          <h3 style={{ margin: 0 }}>Vital Signs Trends & Analytics</h3>
        </div>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <label>Start Date:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="search-input"
            style={{ width: 'auto' }}
          />
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {summary && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
          <div className="info-card">
            <div className="info-card-title">Total Measurements</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
              {summary.totalMeasurements}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">Abnormal Values</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b' }}>
              {summary.abnormalCount}
            </div>
          </div>
          <div className="info-card">
            <div className="info-card-title">Critical Values</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
              {summary.criticalCount}
            </div>
          </div>
        </div>
      )}

      {summary?.latest && (
        <div className="info-card" style={{ marginBottom: '24px' }}>
          <h3 className="info-card-title">Latest Vital Signs</h3>
          <div className="info-row">
            <span className="info-label">Date & Time:</span>
            <span className="info-value">
              {formatDate(summary.latest.measurementDate)} {summary.latest.measurementTime}
            </span>
          </div>
          {summary.latest.systolicBp && summary.latest.diastolicBp && (
            <div className="info-row">
              <span className="info-label">Blood Pressure:</span>
              <span className="info-value">
                {summary.latest.systolicBp}/{summary.latest.diastolicBp} mmHg
              </span>
            </div>
          )}
          {summary.latest.heartRate && (
            <div className="info-row">
              <span className="info-label">Heart Rate:</span>
              <span className="info-value">{summary.latest.heartRate} bpm</span>
            </div>
          )}
          {summary.latest.respiratoryRate && (
            <div className="info-row">
              <span className="info-label">Respiratory Rate:</span>
              <span className="info-value">{summary.latest.respiratoryRate} breaths/min</span>
            </div>
          )}
          {summary.latest.temperature && (
            <div className="info-row">
              <span className="info-label">Temperature:</span>
              <span className="info-value">
                {summary.latest.temperature}°{summary.latest.temperatureUnit || 'F'}
              </span>
            </div>
          )}
          {summary.latest.oxygenSaturation && (
            <div className="info-row">
              <span className="info-label">Oxygen Saturation:</span>
              <span className="info-value">{summary.latest.oxygenSaturation}%</span>
            </div>
          )}
          {summary.latest.weight && (
            <div className="info-row">
              <span className="info-label">Weight:</span>
              <span className="info-value">
                {summary.latest.weight} {summary.latest.weightUnit || 'lbs'}
              </span>
            </div>
          )}
          {summary.latest.bmi && (
            <div className="info-row">
              <span className="info-label">BMI:</span>
              <span className="info-value">{summary.latest.bmi.toFixed(1)}</span>
            </div>
          )}
        </div>
      )}

      {trends.length === 0 ? (
        <div className="empty-state">
          <p>No trend data available for the selected date range</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Avg BP</th>
                <th>Avg HR</th>
                <th>Avg RR</th>
                <th>Avg Temp</th>
                <th>Avg O2 Sat</th>
                <th>Avg Weight</th>
                <th>Avg BMI</th>
                <th>Measurements</th>
              </tr>
            </thead>
            <tbody>
              {trends.map((trend, idx) => (
                <tr key={idx}>
                  <td>{formatDate(trend.measurementDate)}</td>
                  <td>
                    {trend.avgSystolicBp && trend.avgDiastolicBp
                      ? `${trend.avgSystolicBp.toFixed(0)}/${trend.avgDiastolicBp.toFixed(0)}`
                      : '-'}
                  </td>
                  <td>{trend.avgHeartRate ? trend.avgHeartRate.toFixed(0) : '-'}</td>
                  <td>{trend.avgRespiratoryRate ? trend.avgRespiratoryRate.toFixed(0) : '-'}</td>
                  <td>{trend.avgTemperature ? trend.avgTemperature.toFixed(1) : '-'}</td>
                  <td>{trend.avgOxygenSaturation ? trend.avgOxygenSaturation.toFixed(1) : '-'}</td>
                  <td>{trend.avgWeight ? trend.avgWeight.toFixed(1) : '-'}</td>
                  <td>{trend.avgBmi ? trend.avgBmi.toFixed(1) : '-'}</td>
                  <td>{trend.measurementCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Simple text-based trend visualization */}
      {trends.length > 0 && (
        <div className="info-card" style={{ marginTop: '24px' }}>
          <h3 className="info-card-title">Trend Visualization</h3>
          <div style={{ padding: '20px' }}>
            <p style={{ color: '#6b7280', fontStyle: 'italic' }}>
              Chart visualization can be enhanced with a charting library (e.g., Chart.js, Recharts, or D3.js)
              to display line charts, bar charts, and trend analysis graphs.
            </p>
            <p style={{ marginTop: '12px', color: '#6b7280' }}>
              Data is available in the trends array for visualization.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default VitalSignsTrendsPage;
