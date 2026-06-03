import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getOpportunityStages, getOpportunities, OpportunityStage, Opportunity } from '../../services/crmService';
import './Crm.css';

type StageWithTotals = OpportunityStage & {
  totalExpected: number;
  opportunities: Opportunity[];
};

const PipelineKanban: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();

  const [stages, setStages] = useState<StageWithTotals[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) {
      setStages([]);
      setError('No organization selected');
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [stageResponse, opportunitiesResponse] = await Promise.all([
          getOpportunityStages(currentOrganizationId),
          getOpportunities(currentOrganizationId, 'OPEN'),
        ]);
        const stages: OpportunityStage[] = Array.isArray(stageResponse) ? stageResponse : [];
        const opportunities: Opportunity[] = Array.isArray(opportunitiesResponse) ? opportunitiesResponse : [];
        
        const stageMap = stages.map((stage: OpportunityStage) => ({
          ...stage,
          totalExpected: 0,
          opportunities: [] as Opportunity[],
        }));
        const stageTotals = stageMap.reduce<Record<string, StageWithTotals>>((acc: Record<string, StageWithTotals>, stage: StageWithTotals) => {
          if (stage.stageId) {
            acc[stage.stageId] = stage;
          }
          return acc;
        }, {});

        opportunities.forEach((opportunity: Opportunity) => {
          if (!opportunity.stageId || !stageTotals[opportunity.stageId]) return;
          stageTotals[opportunity.stageId].opportunities.push(opportunity);
          stageTotals[opportunity.stageId].totalExpected += opportunity.expectedRevenue ?? 0;
        });

        const orderedStages = Object.values(stageTotals).sort(
          (a: StageWithTotals, b: StageWithTotals) => (a.stageOrder ?? 0) - (b.stageOrder ?? 0),
        );
        setStages(orderedStages);
      } catch (err) {
        console.error('Failed to load pipeline data:', err);
        setError('Failed to load pipeline data');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [currentOrganizationId]);

  const totalExpectedRevenue = useMemo(
    () => stages.reduce((sum, stage) => sum + stage.totalExpected, 0),
    [stages],
  );

  const formatCurrency = (amount?: number, currency?: string) => {
    if (amount === undefined || amount === null) return '-';
    const code = currency || 'BDT';
    try {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: code,
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount);
    } catch {
      return `${code} ${amount.toLocaleString()}`;
    }
  };

  return (
    <div className="crm-page">
      <div className="page-header">
        <div>
          <h1>Sales Pipeline</h1>
          <p>Visualise opportunities by stage and identify revenue risk early</p>
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
          <span className="spinner" /> Loading pipeline...
        </div>
      ) : stages.length === 0 ? (
        <div className="crm-empty-state">
          <p>No pipeline stages configured.</p>
          <p className="crm-help-text">
            Define opportunity stages to unlock the pipeline view.
          </p>
        </div>
      ) : (
        <>
          <div className="crm-summary-cards" style={{ marginBottom: 24 }}>
            <div className="crm-summary-card">
              <h3>Stages</h3>
              <div className="crm-card-value">{stages.length}</div>
              <small>Active pipeline stages</small>
            </div>
            <div className="crm-summary-card">
              <h3>Open Opportunities</h3>
              <div className="crm-card-value">
                {stages.reduce((sum, stage) => sum + stage.opportunities.length, 0)}
              </div>
              <small>Total deals in the pipeline</small>
            </div>
            <div className="crm-summary-card">
              <h3>Expected Revenue</h3>
              <div className="crm-card-value">{formatCurrency(totalExpectedRevenue)}</div>
              <small>Weighted pipeline value</small>
            </div>
          </div>

          <div className="crm-pipeline-board">
            {stages.map((stage) => (
              <section key={stage.stageId} className="crm-pipeline-column">
                <header className="crm-pipeline-column-header">
                  <div>
                    <h2>{stage.stageName}</h2>
                    <p>{stage.probability}% probability</p>
                  </div>
                  <div className="crm-pipeline-column-meta">
                    <span>{stage.opportunities.length} deals</span>
                    <strong>{formatCurrency(stage.totalExpected)}</strong>
                  </div>
                </header>

                <div className="crm-pipeline-cards">
                  {stage.opportunities.length === 0 ? (
                    <div className="crm-pipeline-empty">No opportunities</div>
                  ) : (
                    stage.opportunities.map((opportunity) => (
                      <article
                        key={opportunity.opportunityId}
                        className="crm-pipeline-card"
                        onClick={() => navigate(`/crm/opportunities/${opportunity.opportunityId}`)}
                      >
                        <div className="crm-pipeline-card-title">
                          <strong>{opportunity.opportunityName}</strong>
                          <small>{opportunity.opportunityNumber || '--'}</small>
                        </div>
                        <div className="crm-pipeline-card-values">
                          <span>{formatCurrency(opportunity.amount, opportunity.currency)}</span>
                          <span className="expected">
                            Expected {formatCurrency(opportunity.expectedRevenue, opportunity.currency)}
                          </span>
                        </div>
                      </article>
                    ))
                  )}
                </div>
              </section>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default PipelineKanban;
