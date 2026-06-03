# CRM: Campaigns & Marketing

## Table of Contents
- Purpose
- Roles & Permissions
- Data Model
- Core Flows
- Validations & Rules
- Integrations & Events
- Reports & KPIs
- Edge Cases
- Appendix

## Purpose
Run targeted campaigns and attribute leads and revenue to marketing efforts.

## Roles & Permissions
- Marketing: create campaigns, segments, templates
- Sales: view campaign leads, follow-up tasks

## Data Model
- Campaign
  - Name, Type, Budget, Start/End, Target Segment, Channels, Owner
  - Goals (leads, MQLs, revenue), UTM parameters, Assets
- Segment
  - Criteria builder (attributes, behavior, lists)

## Core Flows
1. Define campaign with goals and budget
2. Build audience segment and send communications
3. Capture responses and attribute leads/opportunities
4. Analyze ROI and optimize

## Validations & Rules
- Consent checks; frequency capping; unsubscribe links
- Suppression lists and do-not-contact; quiet hours per region

## Integrations & Events
- Email/SMS providers; landing pages/forms; ad platforms tagging
- Events: campaign.created, message.sent, conversion.recorded

## Reports & KPIs
- Cost per lead, MQL rate, pipeline and revenue attribution
- Open/click rates, unsubscribe rate, deliverability

## Edge Cases
- Multi-language assets; time-window throttling

## Appendix
- Attribution example: last-touch vs multi-touch (linear/time-decay) for pipeline attribution
- Consent example: double opt-in flow with confirmation email
