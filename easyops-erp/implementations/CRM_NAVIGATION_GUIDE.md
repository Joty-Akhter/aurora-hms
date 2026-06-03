# 🎯 CRM Navigation Guide

## 📋 **Complete CRM Menu Structure**

Your left sidebar now includes the complete CRM menu with all 4 phases:

### **📊 Phase 6.1: Leads & Accounts**
```
CRM
  ├── Dashboard          → /crm/dashboard (Lead overview)
  ├── ─────────────
  ├── Leads             → /crm/leads (Lead management)
  ├── Accounts          → /crm/accounts (Account management)
  ├── Contacts          → /crm/contacts (Contact management)
```

### **💼 Phase 6.2: Opportunities & Pipeline**
```
  ├── ─────────────
  ├── Opportunities     → /crm/opportunities (Opportunity list)
  ├── Pipeline          → /crm/pipeline (Kanban board)
  ├── Forecast          → /crm/forecast (Sales forecast)
```

### **📣 Phase 6.3: Activities & Campaigns**
```
  ├── ─────────────
  ├── Campaigns         → /crm/campaigns (Campaign list)
  ├── Tasks             → /crm/tasks (Task manager)
  ├── Calendar          → /crm/calendar (Event calendar)
```

### **🎫 Phase 6.4: Support & Analytics**
```
  ├── ─────────────
  ├── Support           → /crm/support (Support dashboard)
  ├── Cases             → /crm/cases (Case/ticket list)
  ├── Knowledge Base    → /crm/knowledge-base (KB articles)
  ├── ─────────────
  └── Analytics         → /crm/analytics (Comprehensive CRM analytics)
```

---

## 🔧 **Troubleshooting**

### **If Links Don't Work**:

1. **Clear Browser Cache**: 
   - Press `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)
   - Or clear browser cache completely

2. **Wait for Frontend to Restart**:
   ```bash
   # Check if frontend is ready
   docker logs easyops-frontend --tail 20
   ```

3. **Verify Frontend is Running**:
   - Open http://localhost:3000
   - You should see the login page or dashboard

4. **Check Console for Errors**:
   - Open browser Developer Tools (F12)
   - Check Console tab for errors
   - Check Network tab for failed requests

### **If Components Don't Load**:

The issue might be with import paths. All components are created in:
```
/frontend/src/pages/crm/
```

Verify all components exist:
- ✅ OpportunityDashboard.tsx, OpportunityList.tsx, OpportunityForm.tsx, OpportunityDetail.tsx
- ✅ PipelineKanban.tsx, SalesForecast.tsx
- ✅ CampaignDashboard.tsx, CampaignList.tsx, CampaignForm.tsx
- ✅ TaskManager.tsx, CalendarView.tsx, EmailTemplateManager.tsx
- ✅ CaseDashboard.tsx, CaseList.tsx, CaseForm.tsx, CaseDetail.tsx
- ✅ KnowledgeBaseList.tsx, KnowledgeBaseForm.tsx
- ✅ CrmReports.tsx

---

## 🚀 **Quick Start**

### **Access CRM Module**:
1. Login at http://localhost:3000
2. Click **CRM** in the left sidebar
3. The menu will expand to show all sections
4. Click any item to navigate

### **Complete Menu Items** (19 items):
- **Dashboard** - Lead overview with key metrics
- **Leads** - Lead capture and management
- **Accounts** - Customer/company records
- **Contacts** - Contact person management
- **Opportunities** - Sales pipeline management
- **Pipeline** - Visual Kanban board
- **Forecast** - Sales revenue forecasting
- **Campaigns** - Marketing campaign management
- **Tasks** - Task and to-do management
- **Calendar** - Event and meeting scheduling
- **Support** - Support ticket dashboard
- **Cases** - Case/ticket management
- **Knowledge Base** - Self-service articles
- **Analytics** - Comprehensive CRM analytics

---

## 📊 **Feature Access**

### **Create New Records**:
- New Lead: Click Leads → "+ New Lead" button
- New Account: Click Accounts → "+ New Account" button
- New Opportunity: Click Opportunities → "+ New Opportunity" button
- New Campaign: Click Campaigns → "+ New Campaign" button
- New Task: Click Tasks → "+ New Task" button
- New Case: Click Cases → "+ New Case" button
- New Article: Click Knowledge Base → "+ New Article" button

### **View Dashboards**:
- Lead Dashboard: Click "Dashboard" in CRM menu
- Opportunity Dashboard: Navigate to /crm/opportunity-dashboard
- Campaign Dashboard: Navigate to /crm/campaign-dashboard
- Support Dashboard: Click "Support" in CRM menu
- Analytics: Click "Analytics" in CRM menu

---

## ✅ **Verification**

After frontend restart, you should see:
1. ✅ CRM menu item in left sidebar
2. ✅ Click CRM to expand submenu
3. ✅ 19 menu items visible (with dividers)
4. ✅ All links clickable
5. ✅ Pages load without errors

---

**If you still see errors, please check the browser console (F12) and let me know the specific error message!**

