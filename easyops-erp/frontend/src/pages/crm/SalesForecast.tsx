import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getOpportunities, getPipelineStats } from '../../services/crmService';
import './Crm.css';

type ForecastRow = {
  month: string;
  openCount: number;
  openAmount: number;
  wonCount: number;
  wonAmount: number;
  expectedRevenue: number;
};

const formatCurrency = (amount?: number, currency: string = 'BDT') => {
  if (amount === undefined || amount === null) return '-';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toLocaleString()}`;
  }
};

const formatMonth = (monthKey: string) => {
  const [year, month] = monthKey.split('-');
  const date = new Date(Number(year), Number(month) - 1);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
};

const SalesForecast: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [rows, setRows] = useState<ForecastRow[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setRows([]);
      setStats(null);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [opportunities, statsResponse] = await Promise.all([
          getOpportunities(currentOrganizationId),
          getPipelineStats(currentOrganizationId),
        ]);

        setStats(statsResponse ?? null);

        const grouped = new Map<string, ForecastRow>();
        (Array.isArray(opportunities) ? opportunities : []).forEach((opportunity) => {
          if (!opportunity.expectedCloseDate) return;
          const date = new Date(opportunity.expectedCloseDate);
          if (Number.isNaN(date.getTime())) return;
          const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
          if (!grouped.has(key)) {
            grouped.set(key, {
              month: key,
              openCount: 0,
              openAmount: 0,
              wonCount: 0,
              wonAmount: 0,
              expectedRevenue: 0,
            });
          }
          const row = grouped.get(key)!;
          if (opportunity.status === 'WON') {
            row.wonCount += 1;
            row.wonAmount += opportunity.amount ?? 0;
          } else {
            row.openCount += 1;
            row.openAmount += opportunity.amount ?? 0;
            row.expectedRevenue += opportunity.expectedRevenue ?? 0;
          }
        });

        setRows(Array.from(grouped.values()).sort((a, b) => a.month.localeCompare(b.month)));
      } catch (err) {
        console.error('Failed to load sales forecast:', err);
        setError('Failed to load sales forecast');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId]);

  const totals = useMemo(() => ({
    openCount: rows.reduce((sum, row) => sum + row.openCount, 0),
    openAmount: rows.reduce((sum, row) => sum + row.openAmount, 0),
    expectedRevenue: rows.reduce((sum, row) => sum + row.expectedRevenue, 0),
    wonCount: rows.reduce((sum, row) => sum + row.wonCount, 0),
    wonAmount: rows.reduce((sum, row) => sum + row.wonAmount, 0),
  }), [rows]);

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Sales Forecast</h1>
          <p>Forecast pipeline performance across monthly periods</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={() => navigate('/crm/opportunities/new')}>
            + New Opportunity
          </button>
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}

      {loading ? (
        <div className="table-loading" style={{ background: 'white', borderRadius: 16 }}>
          <span className="spinner" /> Loading forecast...
        </div>
      ) : (
        <>
          {stats && (
            <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
              <div className="crm-summary-card">
                <h3>Open Pipeline</h3>
                <div className="crm-card-value">{stats.totalOpen ?? 0}</div>
                <small>Active opportunities</small>
              </div>
              <div className="crm-summary-card">
                <h3>Won Opportunities</h3>
                <div className="crm-card-value">{stats.totalWon ?? 0}</div>
                <small>Closed in the selected period</small>
              </div>
              <div className="crm-summary-card">
                <h3>Win Rate</h3>
                <div className="crm-card-value">{stats.winRate ?? 0}%</div>
                <small>Conversion rate for all deals</small>
              </div>
            </div>
          )}

          <div className="crm-section">
            <div className="crm-section-header">
              <div>
                <h2>Forecast by Month</h2>
                <p className="section-subtitle">Weighted values consider expected revenue for open deals.</p>
              </div>
            </div>

            <div className="table-wrapper" style={{ boxShadow: 'none', marginTop: 0 }}>
              {rows.length === 0 ? (
                <div className="crm-empty-state">
                  <p>No forecast data available for the selected organization.</p>
                  <div className="empty-actions">
                    <button className="btn-primary" onClick={() => navigate('/crm/opportunities/new')}>
                      Create Opportunity
                    </button>
                  </div>
                </div>
              ) : (
                <table className="crm-table">
                  <thead>
                    <tr>
                      <th>Period</th>
                      <th>Open Count</th>
                      <th>Open Amount</th>
                      <th>Expected Revenue</th>
                      <th>Won Count</th>
                      <th>Won Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr key={row.month}>
                        <td><strong>{formatMonth(row.month)}</strong></td>
                        <td>{row.openCount}</td>
                        <td>{formatCurrency(row.openAmount)}</td>
                        <td>{formatCurrency(row.expectedRevenue)}</td>
                        <td>{row.wonCount}</td>
                        <td>{formatCurrency(row.wonAmount)}</td>
                      </tr>
                    ))}
                    <tr className="crm-table-total">
                      <td><strong>Total</strong></td>
                      <td><strong>{totals.openCount}</strong></td>
                      <td><strong>{formatCurrency(totals.openAmount)}</strong></td>
                      <td><strong>{formatCurrency(totals.expectedRevenue)}</strong></td>
                      <td><strong>{totals.wonCount}</strong></td>
                      <td><strong>{formatCurrency(totals.wonAmount)}</strong></td>
                    </tr>
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default SalesForecast;
