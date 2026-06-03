import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import hospitalService, { VitalSigns, VitalSignsRequest, VitalSignsSummary, Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

type HeightUnit = 'in' | 'cm' | 'm';
type WeightUnit = 'kg' | 'lbs';
type TemperatureUnit = 'F' | 'C';

const HEIGHT_UNIT_OPTIONS: { value: HeightUnit; label: string }[] = [
  { value: 'in', label: 'ft/in' },
  { value: 'cm', label: 'cm' },
  { value: 'm', label: 'm' },
];

function formatHeight(height?: number, heightUnit?: string): string {
  if (!height) return '-';
  if (heightUnit === 'ft') return `${height} ft`;
  if (!heightUnit || heightUnit === 'in') {
    const ft = Math.floor(height / 12);
    const ins = Math.round((height % 12) * 10) / 10;
    return `${ft} ft ${ins} in`;
  }
  return `${height} ${heightUnit}`;
}

/** Backend stores height as total inches when unit is `in`, as cm when `cm`, as meters when `m`. */
function heightToTotalInches(height: number, unit: HeightUnit): number {
  if (unit === 'in') return height;
  if (unit === 'cm') return height / 2.54;
  return height / 0.0254;
}

function totalInchesToStoredHeight(totalInches: number, unit: HeightUnit): number {
  if (unit === 'in') return Math.round(totalInches * 10) / 10;
  if (unit === 'cm') return Math.round(totalInches * 2.54 * 10) / 10;
  return Math.round(totalInches * 0.0254 * 100) / 100;
}

function totalInchesToFeetInches(totalInches: number): { feet: string; inches: string } {
  return {
    feet: String(Math.floor(totalInches / 12)),
    inches: String(Math.round((totalInches % 12) * 10) / 10),
  };
}

function convertWeight(value: number, from: WeightUnit, to: WeightUnit): number {
  if (from === to) return value;
  return from === 'kg'
    ? Math.round(value * 2.20462 * 10) / 10
    : Math.round((value / 2.20462) * 10) / 10;
}

function convertTemperature(value: number, from: TemperatureUnit, to: TemperatureUnit): number {
  if (from === to) return value;
  return from === 'F'
    ? Math.round(((value - 32) * 5 / 9) * 10) / 10
    : Math.round((value * 9 / 5 + 32) * 10) / 10;
}

function isImperialHeightUnit(unit?: string): boolean {
  return !unit || unit === 'in';
}

/** Roll inches >= 12 into feet (e.g. 5 ft + 14 in → 6 ft 2 in). */
function normalizeImperialHeightFields(
  feet: string,
  inches: string,
): { feet: string; inches: string; total: number | undefined } {
  let ft = parseFloat(feet) || 0;
  let ins = parseFloat(inches) || 0;
  if (ins >= 12) {
    ft += Math.floor(ins / 12);
    ins = Math.round((ins % 12) * 10) / 10;
  }
  const total = ft * 12 + ins;
  return {
    feet: ft > 0 ? String(ft) : '',
    inches: ins > 0 ? String(ins) : (ft > 0 ? '0' : ''),
    total: total > 0 ? Math.round(total * 10) / 10 : undefined,
  };
}

function resolveStoredHeight(
  height: number | undefined,
  heightUnit: HeightUnit | string | undefined,
  feet: string,
  inches: string,
): number | undefined {
  if (isImperialHeightUnit(heightUnit)) {
    const { total } = normalizeImperialHeightFields(feet, inches);
    return total ?? height;
  }
  return height;
}

/** Coerce API/legacy height units to a value the form controls understand. */
function normalizeHeightUnitForForm(unit?: string): HeightUnit {
  if (unit === 'cm' || unit === 'm') return unit;
  return 'in';
}

function parseOptionalNumber(raw: string): number | undefined {
  if (!raw.trim()) return undefined;
  const n = parseFloat(raw);
  return Number.isFinite(n) ? n : undefined;
}

function parseOptionalInt(raw: string): number | undefined {
  if (!raw.trim()) return undefined;
  const n = parseInt(raw, 10);
  return Number.isFinite(n) ? n : undefined;
}

const VitalSignsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [vitalSigns, setVitalSigns] = useState<VitalSigns[]>([]);
  const [latestVitalSigns, setLatestVitalSigns] = useState<VitalSigns | null>(null);
  const [summary, setSummary] = useState<VitalSignsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<VitalSigns | null>(null);
  const [filter, setFilter] = useState<'all' | 'abnormal' | 'critical'>('all');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [heightFeet, setHeightFeet] = useState<string>('');
  const [heightInches, setHeightInches] = useState<string>('');
  const [formData, setFormData] = useState<VitalSignsRequest>({
    measurementDate: new Date().toISOString().split('T')[0],
    measurementTime: new Date().toTimeString().slice(0, 5),
    encounterId: undefined,
    systolicBp: undefined,
    diastolicBp: undefined,
    heartRate: undefined,
    respiratoryRate: undefined,
    temperature: undefined,
    temperatureUnit: 'F',
    oxygenSaturation: undefined,
    weight: undefined,
    weightUnit: 'kg',
    height: undefined,
    heightUnit: 'in',
    painScale: undefined,
    bloodGlucose: undefined,
    headCircumference: undefined,
    deviceUsed: '',
    patientPosition: '',
    notes: '',
  });

  useEffect(() => {
    if (id) {
      loadPatientData();
      loadVitalSigns();
    }
  }, [id]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const response = await hospitalService.getPatient(id);
      setPatient(response.data);
    } catch (err: any) {
      console.error('Failed to load patient data:', err);
    }
  };

  useEffect(() => {
    if (id && (filter !== 'all' || startDate || endDate)) {
      loadFilteredVitalSigns();
    } else if (id && filter === 'all' && !startDate && !endDate) {
      loadVitalSigns();
    }
  }, [filter, startDate, endDate]);

  const loadVitalSigns = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      // Load all vital signs, latest, and summary
      const [allResponse, latestResponse, summaryResponse] = await Promise.all([
        hospitalService.getVitalSigns(id),
        hospitalService.getLatestVitalSigns(id).catch(() => ({ data: null })),
        hospitalService.getVitalSignsSummary(id).catch(() => ({ data: null }))
      ]);
      
      setVitalSigns(allResponse.data);
      setLatestVitalSigns(latestResponse.data ?? null);
      setSummary(summaryResponse.data);
    } catch (err: any) {
      console.error('Failed to load vital signs:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load vital signs'));
    } finally {
      setLoading(false);
    }
  };

  const loadFilteredVitalSigns = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      
      let response;
      if (filter === 'abnormal') {
        response = await hospitalService.getAbnormalVitalSigns(id);
      } else if (filter === 'critical') {
        response = await hospitalService.getCriticalVitalSigns(id);
      } else if (startDate && endDate) {
        response = await hospitalService.getVitalSignsByDateRange(id, startDate, endDate);
      } else {
        response = await hospitalService.getVitalSigns(id);
      }
      
      setVitalSigns(response.data);
    } catch (err: any) {
      console.error('Failed to load vital signs:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load vital signs'));
    } finally {
      setLoading(false);
    }
  };

  const buildSubmitPayload = (): VitalSignsRequest => {
    const heightUnit = normalizeHeightUnitForForm(formData.heightUnit);
    const resolvedHeight = resolveStoredHeight(formData.height, heightUnit, heightFeet, heightInches);
    return {
      ...formData,
      height: resolvedHeight,
      heightUnit,
      deviceUsed: formData.deviceUsed?.trim() || undefined,
    };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    const payload = buildSubmitPayload();

    try {
      if (editing) {
        await hospitalService.updateVitalSigns(id, editing.vitalSignId, payload);
      } else {
        await hospitalService.createVitalSigns(id, payload);
      }
      setShowForm(false);
      setEditing(null);
      resetForm();
      loadVitalSigns();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save vital signs'));
    }
  };

  const resetForm = () => {
    setFormData({
      measurementDate: new Date().toISOString().split('T')[0],
      measurementTime: new Date().toTimeString().slice(0, 5),
      encounterId: undefined,
      systolicBp: undefined,
      diastolicBp: undefined,
      heartRate: undefined,
      respiratoryRate: undefined,
      temperature: undefined,
      temperatureUnit: 'F',
      oxygenSaturation: undefined,
      weight: undefined,
      weightUnit: 'kg',
      height: undefined,
      heightUnit: 'in',
      painScale: undefined,
      bloodGlucose: undefined,
      headCircumference: undefined,
      deviceUsed: '',
      patientPosition: '',
      notes: '',
    });
    setHeightFeet('');
    setHeightInches('');
  };

  const handleEdit = (item: VitalSigns) => {
    setEditing(item);
    const resolvedHeightUnit = normalizeHeightUnitForForm(item.heightUnit);
    const storedHeight =
      item.heightUnit === 'ft' && item.height
        ? item.height * 12
        : item.height;
    setFormData({
      measurementDate: item.measurementDate,
      measurementTime: item.measurementTime,
      encounterId: item.encounterId,
      systolicBp: item.systolicBp,
      diastolicBp: item.diastolicBp,
      heartRate: item.heartRate,
      respiratoryRate: item.respiratoryRate,
      temperature: item.temperature,
      temperatureUnit: item.temperatureUnit || 'F',
      oxygenSaturation: item.oxygenSaturation,
      weight: item.weight,
      weightUnit: item.weightUnit || 'kg',
      height: storedHeight,
      heightUnit: resolvedHeightUnit,
      painScale: item.painScale,
      bloodGlucose: item.bloodGlucose,
      headCircumference: item.headCircumference,
      deviceUsed: item.deviceUsed || '',
      patientPosition: item.patientPosition || '',
      notes: item.notes || '',
    });
    if (isImperialHeightUnit(resolvedHeightUnit) && storedHeight) {
      const parts = totalInchesToFeetInches(storedHeight);
      setHeightFeet(parts.feet);
      setHeightInches(parts.inches);
    } else {
      setHeightFeet('');
      setHeightInches('');
    }
    setShowForm(true);
  };

  const handleWeightUnitChange = (newUnit: WeightUnit) => {
    const currentUnit = (formData.weightUnit || 'kg') as WeightUnit;
    if (newUnit === currentUnit) return;
    const convertedWeight =
      formData.weight != null
        ? convertWeight(formData.weight, currentUnit, newUnit)
        : undefined;
    setFormData({ ...formData, weightUnit: newUnit, weight: convertedWeight });
  };

  const handleTemperatureUnitChange = (newUnit: TemperatureUnit) => {
    const currentUnit = (formData.temperatureUnit || 'F') as TemperatureUnit;
    if (newUnit === currentUnit) return;
    const convertedTemp =
      formData.temperature != null
        ? convertTemperature(formData.temperature, currentUnit, newUnit)
        : undefined;
    setFormData({ ...formData, temperatureUnit: newUnit, temperature: convertedTemp });
  };

  const handleImperialHeightBlur = () => {
    const normalized = normalizeImperialHeightFields(heightFeet, heightInches);
    if (
      normalized.feet !== heightFeet ||
      normalized.inches !== heightInches ||
      normalized.total !== formData.height
    ) {
      setHeightFeet(normalized.feet);
      setHeightInches(normalized.inches);
      setFormData({ ...formData, height: normalized.total });
    }
  };

  const handleHeightUnitChange = (newUnit: HeightUnit) => {
    const currentUnit = normalizeHeightUnitForForm(formData.heightUnit);
    if (newUnit === currentUnit) return;

    const sourceHeight = resolveStoredHeight(formData.height, currentUnit, heightFeet, heightInches);
    let convertedHeight: number | undefined;
    if (sourceHeight != null && sourceHeight > 0) {
      const totalInches = heightToTotalInches(sourceHeight, currentUnit);
      convertedHeight = totalInchesToStoredHeight(totalInches, newUnit);
    } else {
      convertedHeight = undefined;
    }

    if (isImperialHeightUnit(newUnit)) {
      if (convertedHeight != null && convertedHeight > 0) {
        const parts = totalInchesToFeetInches(convertedHeight);
        setHeightFeet(parts.feet);
        setHeightInches(parts.inches);
      } else {
        setHeightFeet('');
        setHeightInches('');
      }
    } else {
      setHeightFeet('');
      setHeightInches('');
    }

    setFormData({
      ...formData,
      heightUnit: newUnit,
      height: convertedHeight,
    });
  };

  const handleDelete = async (vitalSignId: string) => {
    if (!window.confirm('Are you sure you want to delete this vital signs record?')) {
      return;
    }
    if (!id) return;
    try {
      await hospitalService.deleteVitalSigns(id, vitalSignId);
      loadVitalSigns();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete vital signs'));
    }
  };

  const formatDateTime = (date: string, time: string) => {
    return `${new Date(date).toLocaleDateString()} ${time}`;
  };

  const renderHeightUnitSelect = () => (
    <select
      className="unit-select"
      value={normalizeHeightUnitForForm(formData.heightUnit)}
      onChange={(e) => handleHeightUnitChange(e.target.value as HeightUnit)}
      aria-label="Height unit"
    >
      {HEIGHT_UNIT_OPTIONS.map((opt) => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  );

  const previewHeight = resolveStoredHeight(
    formData.height,
    normalizeHeightUnitForForm(formData.heightUnit),
    heightFeet,
    heightInches,
  );

  if (loading) {
    return <div className="loading">Loading vital signs...</div>;
  }

  const filteredVitalSigns = vitalSigns.filter(vs => {
    if (filter === 'abnormal' && !vs.isAbnormal) return false;
    if (filter === 'critical' && !vs.isCritical) return false;
    return true;
  });

  type VitalSeverity = 'normal' | 'abnormal' | 'critical';

  const getSeverityColor = (severity: VitalSeverity) => {
    if (severity === 'critical') return '#ef4444';
    if (severity === 'abnormal') return '#f59e0b';
    return '#10b981';
  };

  const getBloodPressureSeverity = (systolic?: number, diastolic?: number): VitalSeverity => {
    if (systolic === undefined || diastolic === undefined) return 'normal';
    if (systolic > 180 || diastolic > 120 || systolic < 80 || diastolic < 50) return 'critical';
    if (systolic > 120 || diastolic > 80 || systolic < 90 || diastolic < 60) return 'abnormal';
    return 'normal';
  };

  const getHeartRateSeverity = (heartRate?: number): VitalSeverity => {
    if (heartRate === undefined) return 'normal';
    if (heartRate < 50 || heartRate > 120) return 'critical';
    if (heartRate < 60 || heartRate > 100) return 'abnormal';
    return 'normal';
  };

  const getRespiratoryRateSeverity = (respiratoryRate?: number): VitalSeverity => {
    if (respiratoryRate === undefined) return 'normal';
    if (respiratoryRate < 10 || respiratoryRate > 24) return 'critical';
    if (respiratoryRate < 12 || respiratoryRate > 20) return 'abnormal';
    return 'normal';
  };

  const getOxygenSaturationSeverity = (oxygenSaturation?: number): VitalSeverity => {
    if (oxygenSaturation === undefined) return 'normal';
    if (oxygenSaturation < 90) return 'critical';
    if (oxygenSaturation < 95) return 'abnormal';
    return 'normal';
  };

  return (
    <div className="hospital-page">
      {/* Patient Information Header */}
      {patient && (
        <div className="page-header" style={{ marginBottom: '24px' }}>
          <div>
            <h1>{patient.fullName || '—'}</h1>
            <p>
              MRN: {patient.mrn} 
              {patient.dateOfBirth && ` | Age: ${formatAge(patient.dateOfBirth)}`}
              {patient.gender && ` | ${formatGenderLabel(patient.gender)}`}
            </p>
          </div>
        </div>
      )}

      <div className="page-header">
        <div>
          <h1>Vital Signs</h1>
          <p>Record and monitor patient vital signs and clinical measurements</p>
        </div>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}`)}>
            ← Back to Patient
          </button>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/hospital/patients/${id}/vital-signs/trends`)}>
            View Trends
          </button>
          <button type="button" className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); resetForm(); }}>
            + Record Vital Signs
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Statistics */}
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

      {/* Latest Vital Signs Display */}
      {latestVitalSigns && (
        <div className="info-card" style={{ marginBottom: '24px', borderLeft: '4px solid #2563eb' }}>
          <h3 className="info-card-title" style={{ marginBottom: '16px' }}>Latest Vital Signs</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
            {latestVitalSigns.systolicBp && latestVitalSigns.diastolicBp && (
              <div className="info-row">
                <span className="info-label">Blood Pressure:</span>
                <span className="info-value" style={{
                  color: getSeverityColor(getBloodPressureSeverity(latestVitalSigns.systolicBp, latestVitalSigns.diastolicBp)),
                  fontWeight: 'bold'
                }}>
                  {latestVitalSigns.systolicBp}/{latestVitalSigns.diastolicBp} mmHg
                </span>
              </div>
            )}
            {latestVitalSigns.heartRate && (
              <div className="info-row">
                <span className="info-label">Heart Rate:</span>
                <span className="info-value" style={{
                  color: getSeverityColor(getHeartRateSeverity(latestVitalSigns.heartRate)),
                  fontWeight: 'bold'
                }}>
                  {latestVitalSigns.heartRate} bpm
                </span>
              </div>
            )}
            {latestVitalSigns.respiratoryRate && (
              <div className="info-row">
                <span className="info-label">Respiratory Rate:</span>
                <span className="info-value" style={{
                  color: getSeverityColor(getRespiratoryRateSeverity(latestVitalSigns.respiratoryRate)),
                  fontWeight: 'bold'
                }}>
                  {latestVitalSigns.respiratoryRate} breaths/min
                </span>
              </div>
            )}
            {latestVitalSigns.temperature && (
              <div className="info-row">
                <span className="info-label">Temperature:</span>
                <span className="info-value" style={{ 
                  color: latestVitalSigns.isCritical ? '#ef4444' : latestVitalSigns.isAbnormal ? '#f59e0b' : '#10b981',
                  fontWeight: 'bold'
                }}>
                  {latestVitalSigns.temperature}°{latestVitalSigns.temperatureUnit || 'F'}
                </span>
              </div>
            )}
            {latestVitalSigns.oxygenSaturation && (
              <div className="info-row">
                <span className="info-label">Oxygen Saturation:</span>
                <span className="info-value" style={{
                  color: getSeverityColor(getOxygenSaturationSeverity(latestVitalSigns.oxygenSaturation)),
                  fontWeight: 'bold'
                }}>
                  {latestVitalSigns.oxygenSaturation}%
                </span>
              </div>
            )}
            {latestVitalSigns.weight && (
              <div className="info-row">
                <span className="info-label">Weight:</span>
                <span className="info-value">
                  {latestVitalSigns.weight} {latestVitalSigns.weightUnit || 'kg'}
                </span>
              </div>
            )}
            {latestVitalSigns.height && (
              <div className="info-row">
                <span className="info-label">Height:</span>
                <span className="info-value">
                  {formatHeight(latestVitalSigns.height, latestVitalSigns.heightUnit)}
                </span>
              </div>
            )}
            {latestVitalSigns.bmi && (
              <div className="info-row">
                <span className="info-label">BMI:</span>
                <span className="info-value" style={{ fontWeight: 'bold' }}>
                  {latestVitalSigns.bmi.toFixed(1)}
                </span>
              </div>
            )}
            {latestVitalSigns.painScale !== undefined && (
              <div className="info-row">
                <span className="info-label">Pain Scale:</span>
                <span className="info-value" style={{ fontWeight: 'bold' }}>
                  {latestVitalSigns.painScale}/10
                </span>
              </div>
            )}
          </div>
          <div style={{ marginTop: '12px', fontSize: '14px', color: '#6b7280' }}>
            Measured: {formatDateTime(latestVitalSigns.measurementDate, latestVitalSigns.measurementTime)}
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="filters-section" style={{ marginBottom: '20px' }}>
        <div className="filter-row">
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as any)}
            className="filter-select"
          >
            <option value="all">All Vital Signs</option>
            <option value="abnormal">Abnormal Only</option>
            <option value="critical">Critical Only</option>
          </select>

          <div className="form-group" style={{ gap: '4px' }}>
            <label style={{ fontSize: '12px' }}>From Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              style={{ width: '170px', minWidth: '170px' }}
            />
          </div>

          <div className="form-group" style={{ gap: '4px' }}>
            <label style={{ fontSize: '12px' }}>To Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              style={{ width: '170px', minWidth: '170px' }}
            />
          </div>
          
          {(startDate || endDate || filter !== 'all') && (
            <button 
              className="btn-secondary" 
              onClick={() => {
                setFilter('all');
                setStartDate('');
                setEndDate('');
              }}
            >
              Clear Filters
            </button>
          )}
        </div>
      </div>

      {showForm && (
        <div className="form-container form-container--vitals">
          <h3>{editing ? 'Edit' : 'Record'} Vital Signs</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-section">
              <h4 className="form-section-title">Measurement Information</h4>
              <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 180px))' }}>
                <div className="form-group">
                  <label>Measurement Date *</label>
                  <input
                    type="date"
                    required
                    value={formData.measurementDate}
                    onChange={(e) => setFormData({ ...formData, measurementDate: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Measurement Time *</label>
                  <input
                    type="time"
                    required
                    value={formData.measurementTime}
                    onChange={(e) => setFormData({ ...formData, measurementTime: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Patient Position</label>
                  <input
                    type="text"
                    value={formData.patientPosition}
                    onChange={(e) => setFormData({ ...formData, patientPosition: e.target.value })}
                    placeholder="e.g., Sitting, Standing, Lying"
                  />
                </div>
                <div className="form-group">
                  <label>Device Used</label>
                  <input
                    type="text"
                    value={formData.deviceUsed}
                    onChange={(e) => setFormData({ ...formData, deviceUsed: e.target.value })}
                    placeholder="e.g., Welch Allyn monitor"
                  />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h4 className="form-section-title">Blood Pressure, Heart Rate & Respiratory</h4>
              <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
                <div className="form-group">
                  <label>Systolic BP (mmHg)</label>
                  <input
                    type="number"
                    value={formData.systolicBp || ''}
                    onChange={(e) => setFormData({ ...formData, systolicBp: parseOptionalInt(e.target.value) })}
                    min="0"
                    max="300"
                  />
                </div>
                <div className="form-group">
                  <label>Diastolic BP (mmHg)</label>
                  <input
                    type="number"
                    value={formData.diastolicBp || ''}
                    onChange={(e) => setFormData({ ...formData, diastolicBp: parseOptionalInt(e.target.value) })}
                    min="0"
                    max="200"
                  />
                </div>
                <div className="form-group">
                  <label>Heart Rate (bpm)</label>
                  <input
                    type="number"
                    value={formData.heartRate || ''}
                    onChange={(e) => setFormData({ ...formData, heartRate: parseOptionalInt(e.target.value) })}
                    min="0"
                    max="300"
                  />
                </div>
                <div className="form-group">
                  <label>Respiratory Rate (breaths/min)</label>
                  <input
                    type="number"
                    value={formData.respiratoryRate || ''}
                    onChange={(e) => setFormData({ ...formData, respiratoryRate: parseOptionalInt(e.target.value) })}
                    min="0"
                    max="60"
                  />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h4 className="form-section-title">Temperature, Oxygen, Weight & Height</h4>
              <div className="form-grid form-grid--vitals-measurements">
                <div className="form-group">
                  <label>Temperature</label>
                  <div className="measurement-inline">
                    <input
                      type="number"
                      step="0.1"
                      value={formData.temperature || ''}
                      onChange={(e) => setFormData({ ...formData, temperature: parseOptionalNumber(e.target.value) })}
                    />
                    <select
                      className="unit-select"
                      value={formData.temperatureUnit}
                      onChange={(e) => handleTemperatureUnitChange(e.target.value as TemperatureUnit)}
                      aria-label="Temperature unit"
                    >
                      <option value="F">°F</option>
                      <option value="C">°C</option>
                    </select>
                  </div>
                </div>
                <div className="form-group">
                  <label>Oxygen Saturation (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={formData.oxygenSaturation || ''}
                    onChange={(e) => setFormData({ ...formData, oxygenSaturation: parseOptionalNumber(e.target.value) })}
                    min="0"
                    max="100"
                  />
                </div>
                <div className="form-group">
                  <label>Weight</label>
                  <div className="measurement-inline">
                    <input
                      type="number"
                      step="0.1"
                      value={formData.weight || ''}
                      onChange={(e) => setFormData({ ...formData, weight: parseOptionalNumber(e.target.value) })}
                    />
                    <select
                      className="unit-select"
                      value={formData.weightUnit}
                      onChange={(e) => handleWeightUnitChange(e.target.value as WeightUnit)}
                      aria-label="Weight unit"
                    >
                      <option value="lbs">lbs</option>
                      <option value="kg">kg</option>
                    </select>
                  </div>
                </div>
                <div className="form-group form-group--height">
                  <label>Height</label>
                  {isImperialHeightUnit(formData.heightUnit) ? (
                    <div className="height-inline">
                      <input
                        type="number"
                        min="0"
                        max="9"
                        placeholder="ft"
                        value={heightFeet}
                        onChange={(e) => {
                          const ft = e.target.value;
                          setHeightFeet(ft);
                          setFormData({
                            ...formData,
                            height: normalizeImperialHeightFields(ft, heightInches).total,
                          });
                        }}
                        onBlur={handleImperialHeightBlur}
                      />
                      <span className="height-unit-label">ft</span>
                      <input
                        type="number"
                        min="0"
                        max="11.9"
                        step="0.1"
                        placeholder="in"
                        value={heightInches}
                        onChange={(e) => {
                          const ins = e.target.value;
                          setHeightInches(ins);
                          setFormData({
                            ...formData,
                            height: normalizeImperialHeightFields(heightFeet, ins).total,
                          });
                        }}
                        onBlur={handleImperialHeightBlur}
                      />
                      <span className="height-unit-label">in</span>
                      {renderHeightUnitSelect()}
                    </div>
                  ) : (
                    <div className="measurement-inline">
                      <input
                        type="number"
                        step="0.1"
                        min="0"
                        max={normalizeHeightUnitForForm(formData.heightUnit) === 'cm' ? 300 : 3}
                        value={formData.height || ''}
                        onChange={(e) => setFormData({ ...formData, height: parseOptionalNumber(e.target.value) })}
                      />
                      {renderHeightUnitSelect()}
                    </div>
                  )}
                </div>
              </div>
              {(formData.weight && previewHeight) && (
                <div style={{ marginTop: '12px', padding: '12px', background: '#f0f9ff', borderRadius: '8px' }}>
                  <strong>BMI will be calculated automatically</strong>
                </div>
              )}
            </div>

            <div className="form-section">
              <h4 className="form-section-title">Additional Measurements</h4>
              <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 180px))' }}>
                <div className="form-group">
                  <label>Pain Scale (0-10)</label>
                  <input
                    type="number"
                    value={formData.painScale ?? ''}
                    onChange={(e) => setFormData({ ...formData, painScale: parseOptionalInt(e.target.value) })}
                    min="0"
                    max="10"
                  />
                </div>
                <div className="form-group">
                  <label>Blood Glucose (mg/dL)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={formData.bloodGlucose || ''}
                    onChange={(e) => setFormData({ ...formData, bloodGlucose: parseOptionalNumber(e.target.value) })}
                  />
                </div>
                <div className="form-group">
                  <label>Head Circumference (cm)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={formData.headCircumference || ''}
                    onChange={(e) => setFormData({ ...formData, headCircumference: parseOptionalNumber(e.target.value) })}
                  />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h4 className="form-section-title">Notes</h4>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  rows={4}
                />
              </div>
            </div>

            <div className="form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}

      {filteredVitalSigns.length === 0 ? (
        <div className="empty-state">
          <p>No vital signs found{filter !== 'all' ? ` (${filter})` : ''}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>BP</th>
                <th>HR</th>
                <th>RR</th>
                <th>Temp</th>
                <th>O2 Sat</th>
                <th>Weight</th>
                <th>Height</th>
                <th>BMI</th>
                <th>Pain</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredVitalSigns.map((item) => (
                <tr 
                  key={item.vitalSignId}
                  style={{
                    backgroundColor: item.isCritical ? '#fee2e2' : item.isAbnormal ? '#fef3c7' : 'transparent'
                  }}
                >
                  <td>{formatDateTime(item.measurementDate, item.measurementTime)}</td>
                  <td style={{ 
                    color: getSeverityColor(getBloodPressureSeverity(item.systolicBp, item.diastolicBp)),
                    fontWeight: getBloodPressureSeverity(item.systolicBp, item.diastolicBp) === 'normal' ? 'normal' : 'bold'
                  }}>
                    {item.systolicBp && item.diastolicBp 
                      ? `${item.systolicBp}/${item.diastolicBp}` 
                      : '-'}
                  </td>
                  <td style={{ 
                    color: getSeverityColor(getHeartRateSeverity(item.heartRate)),
                    fontWeight: getHeartRateSeverity(item.heartRate) === 'normal' ? 'normal' : 'bold'
                  }}>
                    {item.heartRate || '-'}
                  </td>
                  <td style={{ 
                    color: getSeverityColor(getRespiratoryRateSeverity(item.respiratoryRate)),
                    fontWeight: getRespiratoryRateSeverity(item.respiratoryRate) === 'normal' ? 'normal' : 'bold'
                  }}>
                    {item.respiratoryRate || '-'}
                  </td>
                  <td>
                    {item.temperature 
                      ? `${item.temperature}°${item.temperatureUnit || 'F'}` 
                      : '-'}
                  </td>
                  <td style={{ 
                    color: getSeverityColor(getOxygenSaturationSeverity(item.oxygenSaturation)),
                    fontWeight: getOxygenSaturationSeverity(item.oxygenSaturation) === 'normal' ? 'normal' : 'bold'
                  }}>
                    {item.oxygenSaturation ? `${item.oxygenSaturation}%` : '-'}
                  </td>
                  <td>
                    {item.weight
                      ? `${item.weight} ${item.weightUnit || 'kg'}`
                      : '-'}
                  </td>
                  <td>{formatHeight(item.height, item.heightUnit)}</td>
                  <td>{item.bmi ? item.bmi.toFixed(1) : '-'}</td>
                  <td>{item.painScale !== undefined ? item.painScale : '-'}</td>
                  <td>
                    {item.isCritical ? (
                      <span className="status-badge status-deceased" style={{ backgroundColor: '#dc2626', color: 'white' }}>
                        CRITICAL
                      </span>
                    ) : item.isAbnormal ? (
                      <span className="status-badge status-inactive" style={{ backgroundColor: '#d97706', color: 'white' }}>
                        ABNORMAL
                      </span>
                    ) : (
                      <span className="status-badge status-active">NORMAL</span>
                    )}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-link" onClick={() => handleEdit(item)}>Edit</button>
                      <button className="btn-link btn-danger" onClick={() => handleDelete(item.vitalSignId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {filteredVitalSigns.length > 0 && (
        <div className="table-footer">
          <p>Showing {filteredVitalSigns.length} vital signs record(s)</p>
        </div>
      )}
    </div>
  );
};

export default VitalSignsPage;
