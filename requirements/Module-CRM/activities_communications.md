# CRM: Activities & Communications

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
Plan, execute, and track all interactions with customers across channels.

## Roles & Permissions
- All CRM users: create/view activities on owned accounts
- Managers: view team calendars; reassign tasks

## Data Model
- Activity/Task
  - Type (call/email/meeting/task), Subject, Description, Start/End, Due Date
  - Owner, Priority, Status, Outcome, Next Action, Attachments
- Communication Log
  - Channel, Direction, Template, Delivery Status, Open/Click

## Core Flows
1. Schedule and assign activities; reminders and SLAs
2. Log calls/emails; auto-sync from integrations
3. Update outcomes; create follow-up tasks
4. Calendar view; team capacity planning

## Validations & Rules
- Overdue escalation; required outcome on completion
- Visibility by account/team; private notes

## Integrations & Events
- Email/Calendar sync (IMAP/Graph/Google), telephony (SIP/dialer)
- Events: activity.created, activity.completed, email.sent, call.logged

## Reports & KPIs
- Activities completed by type/user; SLA adherence; response times

## Edge Cases
- Timezone handling for meetings; recurring tasks exceptions

## Appendix
- SLA example: first response within 2 business hours for high-priority leads
- Reminder policy: meeting reminders at T-24h and T-1h; calls at T-15m
