# HR Module - API Documentation

## Overview

This document provides comprehensive API documentation for the HR Module's Provident Fund and Incentives features (Phases 1-8).

**Base URL**: `/api/hr`

**Authentication**: Bearer token required for all endpoints

---

## Provident Fund APIs

### Account Management

#### Get EPF Accounts
```
GET /provident-fund/accounts?organizationId={organizationId}
```

**Response**: List of EPF accounts

#### Create EPF Account
```
POST /provident-fund/accounts
Body: EpfAccount
```

#### Get EPF Account by ID
```
GET /provident-fund/accounts/{accountId}
```

### Contributions

#### Get Contributions
```
GET /provident-fund/contributions?epfAccountId={accountId}
```

#### Create Contribution
```
POST /provident-fund/contributions
Body: EpfContribution
```

### Interest Calculations

#### Calculate Interest
```
POST /provident-fund/interest/calculate
Body: {
  epfAccountId: UUID,
  financialYear: Integer,
  interestRate: BigDecimal
}
```

### Withdrawals

#### Get Withdrawals
```
GET /provident-fund/withdrawals?epfAccountId={accountId}
```

#### Create Withdrawal Request
```
POST /provident-fund/withdrawals
Body: EpfWithdrawal
```

### Advanced Provident Fund

#### Get Recommendations
```
GET /provident-fund/advanced/recommendations?employeeId={employeeId}&organizationId={organizationId}
```

#### Optimize Contributions
```
GET /provident-fund/advanced/optimize?epfAccountId={accountId}
```

#### Forecast Provident Fund
```
GET /provident-fund/advanced/forecast?epfAccountId={accountId}&months={months}
```

#### Risk Assessment
```
GET /provident-fund/advanced/risk-assessment?epfAccountId={accountId}
```

#### Compliance Check
```
GET /provident-fund/advanced/compliance/check?organizationId={organizationId}&month={month}&year={year}
```

### Provident Fund Reporting

#### Executive Dashboard
```
GET /provident-fund/reports/executive-dashboard?organizationId={organizationId}
```

#### Manager Team Report
```
GET /provident-fund/reports/manager-team?managerId={managerId}&departmentId={departmentId}&organizationId={organizationId}
```

#### Employee Statement
```
GET /provident-fund/reports/employee-statement?employeeId={employeeId}&epfAccountId={accountId}&startDate={date}&endDate={date}
```

#### Compliance Report
```
GET /provident-fund/reports/compliance?organizationId={organizationId}&startDate={date}&endDate={date}
```

#### Cost Analysis Report
```
GET /provident-fund/reports/cost-analysis?organizationId={organizationId}&year={year}
```

#### Trend Analysis Report
```
GET /provident-fund/reports/trend-analysis?organizationId={organizationId}&months={months}
```

### Employee Self-Service

#### Get My EPF Account
```
GET /provident-fund/employee/account?employeeId={employeeId}
```

#### Get My Contributions
```
GET /provident-fund/employee/contributions?employeeId={employeeId}&epfAccountId={accountId}
```

#### Submit Withdrawal Request
```
POST /provident-fund/employee/withdrawals
Body: EpfWithdrawal
```

#### Download Statement
```
GET /provident-fund/employee/statements?employeeId={employeeId}&epfAccountId={accountId}&startDate={date}&endDate={date}
```

#### Get Nominations
```
GET /provident-fund/employee/nominations?employeeId={employeeId}&epfAccountId={accountId}
```

#### Create Nomination
```
POST /provident-fund/employee/nominations
Body: EpfNomination
```

---

## Incentive APIs

### Incentive Plans

#### Create Incentive Plan
```
POST /incentives/plans
Body: IncentivePlan
```

#### Get Incentive Plans
```
GET /incentives/plans/organization/{organizationId}
```

#### Get Incentive Plan by ID
```
GET /incentives/plans/{planId}
```

### Sales Targets

#### Create Sales Target
```
POST /incentives/sales-targets
Body: SalesTarget
```

#### Get Sales Targets
```
GET /incentives/sales-targets/organization/{organizationId}
```

#### Update Achievement
```
PUT /incentives/sales-targets/{targetId}/achievement?achievementAmount={amount}
```

### Incentive Calculations

#### Calculate Incentive
```
POST /incentives/calculations?planId={planId}&employeeId={employeeId}&month={month}&year={year}
```

#### Get Calculations
```
GET /incentives/calculations/employee/{employeeId}
```

#### Approve Incentive
```
PUT /incentives/calculations/{id}/approve?approvedBy={userId}
```

#### Reject Incentive
```
PUT /incentives/calculations/{id}/reject?rejectedBy={userId}&reason={reason}
```

### Performance Incentives

#### Calculate Individual Performance
```
POST /incentives/performance/individual?planId={planId}&employeeId={employeeId}&month={month}&year={year}
```

#### Calculate Team Performance
```
POST /incentives/performance/team?planId={planId}&teamLeadId={teamLeadId}&month={month}&year={year}
```

#### Calculate Department Performance
```
POST /incentives/performance/department?planId={planId}&departmentId={departmentId}&month={month}&year={year}
```

#### Calculate Company-Wide Performance
```
POST /incentives/performance/company-wide?planId={planId}&organizationId={organizationId}&month={month}&year={year}
```

### Project Incentives

#### Create Milestone Incentive
```
POST /incentives/project/milestone
Body: ProjectIncentive
```

#### Calculate Completion Bonus
```
POST /incentives/project/{id}/completion-bonus
```

#### Distribute Project Incentive
```
POST /incentives/project/distribute?projectId={projectId}&organizationId={organizationId}&totalIncentiveAmount={amount}&distributionMethod={method}
Body: Map<UUID, BigDecimal>
```

### Retention & Referral

#### Create Retention Bonus
```
POST /incentives/retention-referral/retention
Body: RetentionBonus
```

#### Create Referral
```
POST /incentives/retention-referral/referral
Body: ReferralIncentive
```

#### Validate Referral
```
PUT /incentives/retention-referral/referral/{id}/validate?validatedBy={userId}&isValid={boolean}&validationNotes={notes}
```

### Advanced Incentive Features

#### Get Recommendations
```
GET /incentives/advanced/recommendations?employeeId={employeeId}&organizationId={organizationId}
```

#### Forecast Incentive Costs
```
GET /incentives/advanced/forecast?organizationId={organizationId}&month={month}&year={year}
```

#### Optimize Incentive Plan
```
GET /incentives/advanced/optimize?planId={planId}&organizationId={organizationId}
```

#### Get Notifications
```
GET /incentives/advanced/notifications/employee/{employeeId}
```

### Incentive Reporting

#### Sales Achievement Dashboard
```
GET /incentives/reports/sales-dashboard?organizationId={organizationId}&month={month}&year={year}
```

#### Individual Incentive Report
```
GET /incentives/reports/individual?employeeId={employeeId}&month={month}&year={year}
```

#### Team Incentive Report
```
GET /incentives/reports/team?teamLeadId={teamLeadId}&organizationId={organizationId}&month={month}&year={year}
```

#### Department Incentive Analysis
```
GET /incentives/reports/department?departmentId={departmentId}&organizationId={organizationId}&month={month}&year={year}
```

#### Target vs Achievement Report
```
GET /incentives/reports/target-vs-achievement?organizationId={organizationId}&month={month}&year={year}
```

#### Incentive Payout Report
```
GET /incentives/reports/payout?organizationId={organizationId}&month={month}&year={year}
```

#### Incentive Cost Analysis
```
GET /incentives/reports/cost-analysis?organizationId={organizationId}&year={year}
```

#### Incentive ROI Analysis
```
GET /incentives/reports/roi?organizationId={organizationId}&year={year}
```

### Incentive Payouts

#### Create Payout
```
POST /incentives/payouts?calculationId={calculationId}&payoutDate={date}&payoutMethod={method}
```

#### Process Payout
```
PUT /incentives/payouts/{id}/process?payrollRunId={runId}&paymentReference={reference}
```

---

## Sales Target Configuration

#### Get Configurations
```
GET /sales-target-configurations/organization/{organizationId}
```

#### Create Configuration
```
POST /sales-target-configurations
Body: SalesTargetConfiguration
```

---

## Advanced Analytics

#### Predict Sales Achievement
```
GET /analytics/predictive/sales?employeeId={employeeId}&months={months}
```

#### Measure Incentive Effectiveness
```
GET /analytics/effectiveness/incentives?organizationId={organizationId}&year={year}
```

#### Forecast PF Participation
```
GET /analytics/forecast/provident-fund-participation?organizationId={organizationId}&months={months}
```

#### Analyze Trends
```
GET /analytics/trends?organizationId={organizationId}&entityType={type}&months={months}
```

#### Build Custom Report
```
POST /analytics/custom-report
Body: Map<String, Object> (reportConfig)
```

#### Get Available Report Types
```
GET /analytics/custom-report/types
```

---

## Scheduled Reporting

#### Create Scheduled Report
```
POST /analytics/scheduled-reports
Body: ScheduledReport
```

#### Get Scheduled Reports
```
GET /analytics/scheduled-reports?organizationId={organizationId}
```

#### Update Scheduled Report
```
PUT /analytics/scheduled-reports/{id}
Body: ScheduledReport
```

#### Execute Scheduled Report
```
POST /analytics/scheduled-reports/{id}/execute
```

---

## System Integration

#### Get Integration Status
```
GET /integration/status?organizationId={organizationId}
```

#### Get System Health
```
GET /system/health
```

#### Get Performance Metrics
```
GET /system/performance?organizationId={organizationId}
```

---

## Error Responses

All endpoints may return the following error responses:

- **400 Bad Request**: Invalid input parameters
- **401 Unauthorized**: Missing or invalid authentication token
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server error

---

## Data Models

### EpfAccount
```json
{
  "epfAccountId": "UUID",
  "employeeId": "UUID",
  "organizationId": "UUID",
  "epfAccountNumber": "string",
  "uanNumber": "string",
  "accountStatus": "string",
  "currentBalance": "decimal",
  "isActive": "boolean"
}
```

### IncentivePlan
```json
{
  "incentivePlanId": "UUID",
  "organizationId": "UUID",
  "planName": "string",
  "planCode": "string",
  "planType": "string",
  "effectiveFrom": "date",
  "isActive": "boolean"
}
```

### SalesTarget
```json
{
  "salesTargetId": "UUID",
  "employeeId": "UUID",
  "targetMonth": "integer",
  "targetYear": "integer",
  "targetAmount": "decimal",
  "achievementAmount": "decimal",
  "achievementPercentage": "decimal"
}
```

---

**Last Updated**: Current Date
**API Version**: 1.0
