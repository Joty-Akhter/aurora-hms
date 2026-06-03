# CRM: Service & Support

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
Provide post-sales support through case management and knowledge base to improve CSAT.

## Roles & Permissions
- Agent: create/update cases; use knowledge base
- Supervisor: assign cases; manage SLAs; approvals

## Data Model
- Case/Ticket
  - Subject, Description, Priority, Severity, Status, SLA Plan
  - Account/Contact, Product, Serial/Asset, Channel, Attachments
- Knowledge Article
  - Title, Content, Category, Version, Visibility, Related Cases

## Core Flows
1. Case intake (email, phone, portal, chat); auto-categorize and prioritize
2. Assignment and SLA tracking; internal/external comments
3. Escalations, approvals, and problem/bug linkage
4. Resolution and CSAT survey; closure with root-cause

## Validations & Rules
- SLA timers; required fields by severity; entitlement checks
- Escalation matrix by priority/breach; holiday calendars

## Integrations & Events
- Email-to-case; chat; bug tracker; RMA with Inventory
- Events: case.created, case.updated, case.closed

## Reports & KPIs
- First response/resolution time, backlog, CSAT, reopen rate

## Edge Cases
- Major incident handling; multi-party communications

## Appendix
- Priority-to-SLA example: P1 → first response 15m, resolution 4h; P2 → 1h/8h; P3 → 4h/2d
- Entitlement example: Bronze plan excludes on-site support; Gold includes 24/7 phone
