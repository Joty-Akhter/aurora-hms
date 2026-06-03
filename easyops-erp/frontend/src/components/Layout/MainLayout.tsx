import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import {
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  Divider,
} from "@mui/material";
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  Security as SecurityIcon,
  AdminPanelSettings as AdminIcon,
  Business as BusinessIcon,
  AccountCircle,
  Logout,
  Settings,
  Refresh,
  AccountBalance as AccountingIcon,
  AccountTree as CoAIcon,
  Receipt as JournalIcon,
  Assessment as ReportIcon,
  Notifications as NotificationIcon,
  Campaign as CampaignIcon,
  Factory as ManufacturingIcon,
  Work as WorkOrderIcon,
  Build as BomIcon,
  FactCheck as QualityIcon,
  Engineering as WorkCenterIcon,
  Analytics as AnalyticsIcon,
  Payments as PaymentsIcon,
  LocalHospital as HospitalIcon,
  Assignment as NoteIcon,
  Medication as PrescriptionIcon,
  Description as DescriptionIcon,
  Science as LabIcon,
  Warehouse as WarehouseIcon,
  LocalPharmacy as LocalPharmacyIcon,
  Receipt as ReceiptIcon,
  CreditCard as CreditCardIcon,
  Schedule as ScheduleIcon,
  ViewList as PackageIcon,
  LocalOffer as DiscountSchemeIcon,
  HowToReg as HowToRegIcon,
  Email as EmailIcon,
  History as HistoryIcon,
  Build as OpsIcon,
} from "@mui/icons-material";
import { useAuth } from "@contexts/AuthContext";
import { useModuleConfig } from "@contexts/ModuleConfigContext";
import { resolveOrganizationLogoUrl } from "@/utils/organizationLogo";
import { userHasHospitalPathAccess } from "@/utils/hospitalPathAccess";
import appConfig from "@config";

const drawerWidth = 240;
const BRAND_PRIMARY = "#7b2a90";
const BRAND_SECONDARY = "#05a79c";

type MenuAction = "view" | "manage" | "admin";

interface MenuPermission {
  resource: string;
  action?: MenuAction;
}

interface MenuItemType {
  text: string;
  icon: React.ReactElement;
  path: string;
  permission?: MenuPermission;
  children?: MenuItemType[];
  isSectionHeader?: boolean;
}

const menuItems: MenuItemType[] = [
  {
    text: "Dashboard",
    icon: <DashboardIcon />,
    path: "/dashboard",
    permission: { resource: "dashboard", action: "view" },
  },
  {
    text: "Organizations",
    icon: <BusinessIcon />,
    path: "/organizations",
    permission: { resource: "organizations", action: "manage" },
  },
  {
    text: "Accounting",
    icon: <AccountingIcon />,
    path: "/accounting",
    permission: { resource: "accounting", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/accounting/dashboard",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Chart of Accounts",
        icon: <CoAIcon />,
        path: "/accounting/chart-of-accounts",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Journal Entry",
        icon: <JournalIcon />,
        path: "/accounting/journal-entry",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Fiscal Year Setup",
        icon: <ReportIcon />,
        path: "/accounting/fiscal-year-setup",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "AR Customers",
        icon: <PeopleIcon />,
        path: "/accounting/customers",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Outstanding & Overdue",
        icon: <ReportIcon />,
        path: "/accounting/outstanding",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Trial Balance",
        icon: <ReportIcon />,
        path: "/accounting/trial-balance",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "General Ledger",
        icon: <ReportIcon />,
        path: "/accounting/general-ledger",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Profit & Loss",
        icon: <ReportIcon />,
        path: "/accounting/profit-loss",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Balance Sheet",
        icon: <ReportIcon />,
        path: "/accounting/balance-sheet",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Cash Flow",
        icon: <ReportIcon />,
        path: "/accounting/cash-flow",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Customer Invoices",
        icon: <JournalIcon />,
        path: "/accounting/invoices",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Credit Notes",
        icon: <JournalIcon />,
        path: "/accounting/credit-notes",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Vendors",
        icon: <PeopleIcon />,
        path: "/accounting/vendors",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Vendor Bills",
        icon: <JournalIcon />,
        path: "/accounting/bills",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Bank Accounts",
        icon: <AccountingIcon />,
        path: "/accounting/bank-accounts",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Bank Transactions",
        icon: <JournalIcon />,
        path: "/accounting/bank-transactions",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Bank Reconciliation",
        icon: <AccountingIcon />,
        path: "/accounting/bank-reconciliation",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Aging Reports",
        icon: <ReportIcon />,
        path: "/accounting/aging-reports",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "AR Aging Report",
        icon: <ReportIcon />,
        path: "/accounting/ar-aging-report",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "AP Aging Report",
        icon: <ReportIcon />,
        path: "/accounting/ap-aging-report",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Customer Statements",
        icon: <ReportIcon />,
        path: "/accounting/customer-statements",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Vendor Statements",
        icon: <ReportIcon />,
        path: "/accounting/vendor-statements",
        permission: { resource: "accounting", action: "view" },
      },
      {
        text: "Payment Reminders",
        icon: <ReportIcon />,
        path: "/accounting/payment-reminders",
        permission: { resource: "accounting", action: "view" },
      },
    ],
  },
  {
    text: "Sales",
    icon: <BusinessIcon />,
    path: "/sales",
    permission: { resource: "sales", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/sales/dashboard",
        permission: { resource: "sales", action: "view" },
      },
      {
        text: "Customers",
        icon: <PeopleIcon />,
        path: "/sales/customers",
        permission: { resource: "sales", action: "view" },
      },
      {
        text: "Quotations",
        icon: <ReportIcon />,
        path: "/sales/quotations",
        permission: { resource: "sales", action: "view" },
      },
      {
        text: "Sales Orders",
        icon: <JournalIcon />,
        path: "/sales/orders",
        permission: { resource: "sales", action: "view" },
      },
    ],
  },
  {
    text: "Inventory",
    icon: <BusinessIcon />,
    path: "/inventory",
    permission: { resource: "inventory", action: "view" },
    children: [
      {
        text: "Products",
        icon: <BusinessIcon />,
        path: "/inventory/products",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Stock Levels",
        icon: <ReportIcon />,
        path: "/inventory/stock",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Warehouses",
        icon: <BusinessIcon />,
        path: "/inventory/warehouses",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Receive Inventory",
        icon: <BusinessIcon />,
        path: "/inventory/receive",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Batch/Lot Tracking",
        icon: <ReportIcon />,
        path: "/inventory/batches",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Serial Numbers",
        icon: <ReportIcon />,
        path: "/inventory/serials",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Stock Counting",
        icon: <ReportIcon />,
        path: "/inventory/counting",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Stock Transfers",
        icon: <JournalIcon />,
        path: "/inventory/transfers",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Inventory Valuation",
        icon: <ReportIcon />,
        path: "/inventory/valuation",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Reports & Analytics",
        icon: <ReportIcon />,
        path: "/inventory/reports",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Reorder Management",
        icon: <ReportIcon />,
        path: "/inventory/reorder",
        permission: { resource: "inventory", action: "view" },
      },
    ],
  },
  {
    text: "Purchase",
    icon: <BusinessIcon />,
    path: "/purchase",
    permission: { resource: "purchase", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/purchase/dashboard",
        permission: { resource: "purchase", action: "view" },
      },
      {
        text: "Purchase Orders",
        icon: <JournalIcon />,
        path: "/purchase/orders",
        permission: { resource: "purchase", action: "view" },
      },
      {
        text: "Purchase Receipts",
        icon: <ReportIcon />,
        path: "/purchase/receipts",
        permission: { resource: "purchase", action: "view" },
      },
      {
        text: "Purchase Invoices",
        icon: <JournalIcon />,
        path: "/purchase/invoices",
        permission: { resource: "purchase", action: "view" },
      },
      {
        text: "Variance Management",
        icon: <SecurityIcon />,
        path: "/purchase/variances",
        permission: { resource: "purchase", action: "view" },
      },
      {
        text: "Reports & Analytics",
        icon: <ReportIcon />,
        path: "/purchase/reports",
        permission: { resource: "purchase", action: "view" },
      },
    ],
  },
  {
    text: "Pharma",
    icon: <BusinessIcon />,
    path: "/pharma",
    permission: { resource: "pharma", action: "view" },
    children: [
      {
        text: "Territory Management",
        icon: <BusinessIcon />,
        path: "/pharma/territories",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Employee Assignments",
        icon: <PeopleIcon />,
        path: "/pharma/employee-assignments",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Product Receipts",
        icon: <JournalIcon />,
        path: "/pharma/product-receipts",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Product Disbursements",
        icon: <JournalIcon />,
        path: "/pharma/product-disbursements",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Target Management",
        icon: <AnalyticsIcon />,
        path: "/pharma/targets",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Sold Product Entry",
        icon: <JournalIcon />,
        path: "/pharma/sold-product-entries",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Deposit Amount Entry",
        icon: <AccountingIcon />,
        path: "/pharma/deposits",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Expense Management",
        icon: <ReportIcon />,
        path: "/pharma/expenses",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Product Adjustments",
        icon: <Settings />,
        path: "/pharma/adjustments",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Warehouse stock write-off",
        icon: <WarehouseIcon />,
        path: "/pharma/warehouse-stock-adjustment",
        permission: { resource: "inventory", action: "view" },
      },
      {
        text: "Incentive Management",
        icon: <PaymentsIcon />,
        path: "/pharma/incentives",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Incentive Rules",
        icon: <Settings />,
        path: "/pharma/incentive-rules",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Reports",
        icon: <ReportIcon />,
        path: "/pharma/reports",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Analytics Dashboard",
        icon: <AnalyticsIcon />,
        path: "/pharma/analytics",
        permission: { resource: "pharma", action: "view" },
      },
      {
        text: "Territory Analytics",
        icon: <AnalyticsIcon />,
        path: "/pharma/territory-analytics",
        permission: { resource: "pharma", action: "view" },
      },
    ],
  },
  {
    text: "HR",
    icon: <PeopleIcon />,
    path: "/hr",
    permission: { resource: "hr", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/hr/dashboard",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Employees",
        icon: <PeopleIcon />,
        path: "/hr/employees",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Loans register",
        icon: <AccountingIcon />,
        path: "/hr/loans",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Loan applications",
        icon: <ReportIcon />,
        path: "/hr/loans/applications",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Loan org settings",
        icon: <Settings />,
        path: "/hr/loans/settings",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Loan org audit",
        icon: <JournalIcon />,
        path: "/hr/loans/org-audit",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Loan payroll recoveries",
        icon: <JournalIcon />,
        path: "/hr/loans/payroll-recoveries",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "My loans",
        icon: <AccountingIcon />,
        path: "/hr/my-loans",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Departments",
        icon: <BusinessIcon />,
        path: "/hr/departments",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Positions",
        icon: <AdminIcon />,
        path: "/hr/positions",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Shift definitions",
        icon: <JournalIcon />,
        path: "/hr/shift-definitions",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Roster planner",
        icon: <ReportIcon />,
        path: "/hr/roster",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Attendance",
        icon: <DashboardIcon />,
        path: "/hr/attendance",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Device Sync",
        icon: <DashboardIcon />,
        path: "/hr/device-attendance",
        permission: { resource: "hr", action: "manage" },
      },
      {
        text: "Timesheets",
        icon: <JournalIcon />,
        path: "/hr/timesheets",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Leave Requests",
        icon: <ReportIcon />,
        path: "/hr/leave-requests",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Leave approvals",
        icon: <HowToRegIcon />,
        path: "/hr/leave-approvals",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Leave Balance",
        icon: <ReportIcon />,
        path: "/hr/leave-balance",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Holidays",
        icon: <ReportIcon />,
        path: "/hr/holidays",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Payroll Dashboard",
        icon: <DashboardIcon />,
        path: "/hr/payroll",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Payroll Runs",
        icon: <JournalIcon />,
        path: "/hr/payroll-runs",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Salary Management",
        icon: <AccountingIcon />,
        path: "/hr/salary",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Benefits",
        icon: <BusinessIcon />,
        path: "/hr/benefits",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Reimbursements",
        icon: <ReportIcon />,
        path: "/hr/reimbursements",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Bonuses",
        icon: <ReportIcon />,
        path: "/hr/bonuses",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Performance Reviews",
        icon: <ReportIcon />,
        path: "/hr/performance",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Goals",
        icon: <ReportIcon />,
        path: "/hr/goals",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Development Plans",
        icon: <BusinessIcon />,
        path: "/hr/development",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Training",
        icon: <ReportIcon />,
        path: "/hr/training",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Provident Fund",
        icon: <AccountingIcon />,
        path: "/hr/provident-fund",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Provident Fund Accounts",
        icon: <AccountingIcon />,
        path: "/hr/provident-fund/accounts",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Contributions",
        icon: <JournalIcon />,
        path: "/hr/provident-fund/contributions",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Withdrawals",
        icon: <ReportIcon />,
        path: "/hr/provident-fund/withdrawals",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Nominations",
        icon: <PeopleIcon />,
        path: "/hr/provident-fund/nominations",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Interest Calculation",
        icon: <AnalyticsIcon />,
        path: "/hr/provident-fund/interest",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Statements",
        icon: <ReportIcon />,
        path: "/hr/provident-fund/statements",
        permission: { resource: "hr", action: "view" },
      },
      // Phase 6 - Advanced Provident Fund
      {
        text: "Advanced PF Features",
        icon: <AnalyticsIcon />,
        path: "/hr/provident-fund/advanced",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "My PF Account",
        icon: <PeopleIcon />,
        path: "/hr/provident-fund/my-account",
        permission: { resource: "hr", action: "view" },
      },
      // Phase 7 - Reporting & Analytics
      {
        text: "PF Reporting",
        icon: <ReportIcon />,
        path: "/hr/reports/provident-fund",
        permission: { resource: "hr", action: "view" },
      },
      {
        text: "Custom Reports",
        icon: <ReportIcon />,
        path: "/hr/reports/custom",
        permission: { resource: "hr", action: "manage" },
      },
      {
        text: "Scheduled Reports",
        icon: <NotificationIcon />,
        path: "/hr/reports/scheduled",
        permission: { resource: "hr", action: "manage" },
      },
      // Phase 8 - Integration & Testing
      {
        text: "System Integration",
        icon: <Settings />,
        path: "/hr/system/integration",
        permission: { resource: "hr", action: "admin" },
      },
      {
        text: "Performance Monitoring",
        icon: <AnalyticsIcon />,
        path: "/hr/system/performance",
        permission: { resource: "hr", action: "admin" },
      },
    ],
  },
  {
    text: "CRM",
    icon: <BusinessIcon />,
    path: "/crm",
    permission: { resource: "crm", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/crm/dashboard",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Leads",
        icon: <PeopleIcon />,
        path: "/crm/leads",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Accounts",
        icon: <BusinessIcon />,
        path: "/crm/accounts",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Contacts",
        icon: <PeopleIcon />,
        path: "/crm/contacts",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Opportunities",
        icon: <ReportIcon />,
        path: "/crm/opportunities",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Pipeline",
        icon: <DashboardIcon />,
        path: "/crm/pipeline",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Forecast",
        icon: <ReportIcon />,
        path: "/crm/forecast",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Campaigns",
        icon: <CampaignIcon />,
        path: "/crm/campaigns",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Tasks",
        icon: <ReportIcon />,
        path: "/crm/tasks",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Calendar",
        icon: <DashboardIcon />,
        path: "/crm/calendar",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Support",
        icon: <ReportIcon />,
        path: "/crm/support",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Cases",
        icon: <ReportIcon />,
        path: "/crm/cases",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Knowledge Base",
        icon: <ReportIcon />,
        path: "/crm/knowledge-base",
        permission: { resource: "crm", action: "view" },
      },
      {
        text: "Analytics",
        icon: <ReportIcon />,
        path: "/crm/analytics",
        permission: { resource: "crm", action: "view" },
      },
    ],
  },
  {
    text: "Manufacturing",
    icon: <ManufacturingIcon />,
    path: "/manufacturing",
    permission: { resource: "manufacturing", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/manufacturing/dashboard",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "BOMs",
        icon: <BomIcon />,
        path: "/manufacturing/boms",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Routings",
        icon: <WorkCenterIcon />,
        path: "/manufacturing/routings",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Work Orders",
        icon: <WorkOrderIcon />,
        path: "/manufacturing/work-orders",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Shop Floor",
        icon: <DashboardIcon />,
        path: "/manufacturing/shop-floor",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Production Tracking",
        icon: <ReportIcon />,
        path: "/manufacturing/production-tracking",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Quality Inspections",
        icon: <QualityIcon />,
        path: "/manufacturing/quality/inspections",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Non-Conformances",
        icon: <NotificationIcon />,
        path: "/manufacturing/quality/non-conformances",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Work Centers",
        icon: <WorkCenterIcon />,
        path: "/manufacturing/work-centers",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Maintenance",
        icon: <WorkOrderIcon />,
        path: "/manufacturing/maintenance",
        permission: { resource: "manufacturing", action: "view" },
      },
      {
        text: "Analytics",
        icon: <AnalyticsIcon />,
        path: "/manufacturing/analytics",
        permission: { resource: "manufacturing", action: "view" },
      },
    ],
  },
  {
    text: "Hospital",
    icon: <HospitalIcon />,
    path: "/hospital",
    permission: { resource: "hospital", action: "view" },
    children: [
      // Master data
      {
        text: "Easy Prescription",
        icon: <PrescriptionIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Doctor Dashboard",
        icon: <PrescriptionIcon />,
        path: "/hospital/doctor-dashboard",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Doctor Notes",
        icon: <PrescriptionIcon />,
        path: "/hospital/doctor-notes",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Rx Templates",
        icon: <PrescriptionIcon />,
        path: "/hospital/prescriptions/templates",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Rx Settings",
        icon: <PrescriptionIcon />,
        path: "/hospital/prescriptions/admin",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Master Data",
        icon: <HospitalIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Patients",
        icon: <PeopleIcon />,
        path: "/hospital/patients",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Doctors",
        icon: <PeopleIcon />,
        path: "/hospital/doctors",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Doctor Schedules",
        icon: <PeopleIcon />,
        path: "/hospital/doctors/schedule",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Configurations",
        icon: <Settings />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Doctor Departments",
        icon: <PeopleIcon />,
        path: "/hospital/doctor-departments",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "ICD-10 Codes",
        icon: <DescriptionIcon />,
        path: "/hospital/medical-codes/icd10",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "ICD-11 Codes",
        icon: <DescriptionIcon />,
        path: "/hospital/medical-codes/icd11",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Clinical Chart",
        icon: <LabIcon />,
        path: "/hospital/configurations/clinical-chart",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Note Templates",
        icon: <NoteIcon />,
        path: "/hospital/notes/templates",
        permission: { resource: "hospital", action: "view" },
      },
      // Pharmacy
      {
        text: "Pharmacy",
        icon: <LocalPharmacyIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Pharmacy Catalog",
        icon: <PrescriptionIcon />,
        path: "/hospital/pharmacy/catalog",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Pharmacy Locations",
        icon: <WarehouseIcon />,
        path: "/hospital/pharmacy/locations",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Pharmacy Stock",
        icon: <WarehouseIcon />,
        path: "/hospital/pharmacy/stock",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Pharmacy Dispensing",
        icon: <LocalPharmacyIcon />,
        path: "/hospital/pharmacy/dispense",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Pharmacy Reports",
        icon: <ReportIcon />,
        path: "/hospital/pharmacy/reports",
        permission: { resource: "hospital", action: "view" },
      },
      // Billing
      {
        text: "Billing",
        icon: <ReceiptIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Billing – Charges",
        icon: <ReceiptIcon />,
        path: "/hospital/billing/charges",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Billing – Invoices",
        icon: <ReceiptIcon />,
        path: "/hospital/billing/invoices",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Billing – Payments",
        icon: <ReceiptIcon />,
        path: "/hospital/billing/payments",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Billing – Reports",
        icon: <ReportIcon />,
        path: "/hospital/billing/reports",
        permission: { resource: "hospital", action: "view" },
      },
      // Clinical Orders
      {
        text: "Clinical Orders",
        icon: <NoteIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Clinical Orders – Entry",
        icon: <NoteIcon />,
        path: "/hospital/clinical-orders/entry",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Clinical Orders – Order Sets",
        icon: <NoteIcon />,
        path: "/hospital/clinical-orders/sets",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Clinical Orders – Orders",
        icon: <NoteIcon />,
        path: "/hospital/clinical-orders/orders",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Clinical Orders – Worklists",
        icon: <NoteIcon />,
        path: "/hospital/clinical-orders/worklists",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Clinical Orders – Reports",
        icon: <ReportIcon />,
        path: "/hospital/clinical-orders/reports",
        permission: { resource: "hospital", action: "view" },
      },
      // Corporate & Discount
      {
        text: "Corporate & Discount",
        icon: <BusinessIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Corporate & Discount – Corporates",
        icon: <BusinessIcon />,
        path: "/hospital/corporate-discount/corporates",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Corporate & Discount – Contracts",
        icon: <DescriptionIcon />,
        path: "/hospital/corporate-discount/contracts",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Corporate & Discount – Packages",
        icon: <PackageIcon />,
        path: "/hospital/corporate-discount/packages",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Corporate & Discount – Discount schemes",
        icon: <DiscountSchemeIcon />,
        path: "/hospital/corporate-discount/discount-schemes",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Corporate & Discount – Decisions",
        icon: <DescriptionIcon />,
        path: "/hospital/corporate-discount/decisions",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Corporate & Discount – Reports",
        icon: <ReportIcon />,
        path: "/hospital/corporate-discount/reports",
        permission: { resource: "hospital", action: "view" },
      },
      // Cards
      {
        text: "Cards",
        icon: <CreditCardIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Cards – Management",
        icon: <CreditCardIcon />,
        path: "/hospital/cards",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Cards – Products",
        icon: <CreditCardIcon />,
        path: "/hospital/cards/products",
        permission: { resource: "hospital", action: "view" },
      },
      // Scheduling
      {
        text: "Scheduling",
        icon: <ScheduleIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "Scheduling – Resources",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/resources",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Slot templates",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/slot-templates",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Blackouts",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/blackouts",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Availability",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/availability",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Reservations",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/reservations",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Appointments",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/appointments",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Queue",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/queue",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Planned admissions",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/planned-admissions",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Expected admissions",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/expected-admissions",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Roster blocks",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/roster-blocks",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Waitlist",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/waitlist",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Reports",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/reports",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Doctor mappings",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/doctor-mappings",
        permission: { resource: "hospital", action: "view" },
      },
      {
        text: "Scheduling – Audit log",
        icon: <ScheduleIcon />,
        path: "/hospital/scheduling/audit-log",
        permission: { resource: "hospital", action: "view" },
      },
      // Portal
      {
        text: "Portal",
        icon: <CreditCardIcon />,
        path: "",
        isSectionHeader: true,
      },
      {
        text: "My cards",
        icon: <CreditCardIcon />,
        path: "/portal/cards",
        permission: { resource: "hospital", action: "view" },
      },
    ],
  },
  {
    text: "Communication",
    icon: <NotificationIcon />,
    path: "/communication",
    permission: { resource: "communication", action: "view" },
    children: [
      {
        text: "Dashboard",
        icon: <DashboardIcon />,
        path: "/communication",
        permission: { resource: "communication", action: "view" },
      },
      {
        text: "Message Templates",
        icon: <EmailIcon />,
        path: "/communication/templates",
        permission: { resource: "communication", action: "view" },
      },
      {
        text: "Delivery Log",
        icon: <HistoryIcon />,
        path: "/communication/deliveries",
        permission: { resource: "communication", action: "view" },
      },
      {
        text: "Operations",
        icon: <OpsIcon />,
        path: "/communication/operations",
        permission: { resource: "communication", action: "manage" },
      },
    ],
  },
  {
    text: "Users",
    icon: <PeopleIcon />,
    path: "/users",
    permission: { resource: "users", action: "view" },
  },
  {
    text: "Roles",
    icon: <SecurityIcon />,
    path: "/roles",
    permission: { resource: "roles", action: "view" },
  },
  {
    text: "Permissions",
    icon: <AdminIcon />,
    path: "/permissions",
    permission: { resource: "permissions", action: "view" },
  },
];

const MainLayout: React.FC = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const navigate = useNavigate();
  const {
    user,
    logout,
    canViewResource,
    canManageResource,
    hasAnyPermission,
    hasPermission,
    hasRole,
    reloadRbacState,
    currentOrganizationName,
    currentOrganizationLogo,
  } = useAuth();

  const ADMIN_ROLES = ['SYSTEM_ADMIN', 'SYSTEM_ADMINISTRATOR', 'SUPER_ADMIN', 'ORG_ADMIN'];
  const hasAdminRole = ADMIN_ROLES.some((r) => hasRole(r));
  /** Narrow hospital roles are often stacked with USER; they must not use the dashboard-only menu. */
  const hasHospitalNarrowRole = ['CALL_CENTER', 'HOSPITAL_DOCTOR', 'DOCTOR_ATTENDANTS', 'EHR_OPD'].some((r) =>
    hasRole(r),
  );
  // Plain USER: has USER role but no elevated roles
  const isPlainUser =
    hasRole('USER') &&
    !hasAdminRole &&
    !hasRole('PRESCRIBING_AUTHORITY') &&
    !hasHospitalNarrowRole;
  // Prescribing authority: doctor-level role without admin privileges
  const isPrescribingAuthority = hasRole('PRESCRIBING_AUTHORITY') && !hasAdminRole;
  // EHR OPD staff: patient EHR access only (often stacked with USER)
  const isEhrOpdStaff =
    hasRole('EHR_OPD') &&
    !hasAdminRole &&
    !isPrescribingAuthority &&
    !['CALL_CENTER', 'HOSPITAL_DOCTOR', 'DOCTOR_ATTENDANTS'].some((r) => hasRole(r));

  const orgLogoSrc = resolveOrganizationLogoUrl(currentOrganizationLogo);

  const hasHospitalMenuAccess = useCallback(
    (path: string) => userHasHospitalPathAccess(hasAnyPermission, path),
    [hasAnyPermission]
  );

  const hasAccess = useCallback(
    (permission?: MenuPermission, path?: string) => {
      if (!permission) {
        return true;
      }

      if (permission.resource === 'hospital' && path) {
        return hasHospitalMenuAccess(path);
      }

      const action = permission.action ?? "view";
      if (action === "manage" || action === "admin") {
        return canManageResource(permission.resource);
      }

      return (
        canViewResource(permission.resource) ||
        canManageResource(permission.resource)
      );
    },
    [canManageResource, canViewResource, hasHospitalMenuAccess],
  );

  const { isModuleEnabled, getModuleFromResource } = useModuleConfig();

  const accessibleMenuItems = useMemo(() => {
    // Plain USER role: dashboard + HR self-service entry points (backend enforces linked employee / matrix scope).
    if (isPlainUser) {
      const dashboardItem = menuItems.find((item) => item.path === '/dashboard');
      const hrSelfLeaves: MenuItemType[] = [
        {
          text: 'My leave',
          icon: <ReportIcon />,
          path: '/hr/leave-requests',
        },
        {
          text: 'Leave approvals',
          icon: <HowToRegIcon />,
          path: '/hr/leave-approvals',
        },
        {
          text: 'My salary',
          icon: <AccountingIcon />,
          path: '/hr/my-salary',
        },
        {
          text: 'Leave balance',
          icon: <ReportIcon />,
          path: '/hr/leave-balance',
        },
      ];
      return [dashboardItem, ...hrSelfLeaves].filter(
        (item): item is MenuItemType => item !== undefined,
      );
    }

    // PRESCRIBING_AUTHORITY: Doctor Dashboard, Rx Templates, Rx Settings, Patients
    if (isPrescribingAuthority) {
      const dashboardItem = menuItems.find((item) => item.path === '/dashboard');
      const hospitalItem = menuItems.find((item) => item.path === '/hospital');
      const canDoctorNotes =
        hasPermission('HOSPITAL_FEAT_DOCTOR_NOTES') ||
        hasPermission('HOSPITAL_DOCTOR_NOTES_MANAGE') ||
        canManageResource('hospital');
      const prescribingChildren: MenuItemType[] = [
        { text: 'Easy Prescription', icon: <PrescriptionIcon />, path: '', isSectionHeader: true },
        { text: 'Doctor Dashboard', icon: <PrescriptionIcon />, path: '/hospital/doctor-dashboard' },
        ...(canDoctorNotes
          ? [{ text: 'Doctor Notes', icon: <PrescriptionIcon />, path: '/hospital/doctor-notes' }]
          : []),
        { text: 'Rx Templates', icon: <PrescriptionIcon />, path: '/hospital/prescriptions/templates' },
        { text: 'Rx Settings', icon: <PrescriptionIcon />, path: '/hospital/prescriptions/admin' },
        { text: 'Master Data', icon: <HospitalIcon />, path: '', isSectionHeader: true },
        { text: 'Patients', icon: <PeopleIcon />, path: '/hospital/patients' },
        { text: 'Clinical Chart', icon: <LabIcon />, path: '/hospital/configurations/clinical-chart' },
      ];
      const prescribingHospital: MenuItemType = {
        ...(hospitalItem ?? { text: 'Hospital', icon: <HospitalIcon />, path: '/hospital' }),
        children: prescribingChildren,
      };
      return [dashboardItem, prescribingHospital].filter(
        (item): item is MenuItemType => item !== undefined,
      );
    }

    // EHR_OPD: Hospital module with Patients only
    if (isEhrOpdStaff) {
      const dashboardItem = menuItems.find((item) => item.path === '/dashboard');
      const hospitalItem = menuItems.find((item) => item.path === '/hospital');
      const ehrOpdChildren: MenuItemType[] = [
        { text: 'Patients', icon: <PeopleIcon />, path: '/hospital/patients' },
      ];
      const ehrOpdHospital: MenuItemType = {
        ...(hospitalItem ?? { text: 'Hospital', icon: <HospitalIcon />, path: '/hospital/patients' }),
        path: '/hospital/patients',
        children: ehrOpdChildren,
      };
      return [dashboardItem, ehrOpdHospital].filter(
        (item): item is MenuItemType => item !== undefined,
      );
    }

    const pruneOrphanSectionHeaders = (items: MenuItemType[]): MenuItemType[] => {
      const pruned: MenuItemType[] = [];
      for (let i = 0; i < items.length; i += 1) {
        const item = items[i];
        if (item.isSectionHeader) {
          const hasFollowingRoute = items.slice(i + 1).some((next) => !next.isSectionHeader);
          if (hasFollowingRoute) pruned.push(item);
        } else {
          pruned.push(item);
        }
      }
      return pruned;
    };

    const filterItems = (items: MenuItemType[]): MenuItemType[] =>
      pruneOrphanSectionHeaders(
      items
        .map((item) => {
          // Check if module is enabled
          const moduleEnabled = item.permission
            ? (() => {
                const module = getModuleFromResource(item.permission.resource);
                return module ? isModuleEnabled(module) : true;
              })()
            : true;

          // If module is disabled, exclude this item
          if (!moduleEnabled) {
            return null;
          }

          const childItems = item.children
            ? filterItems(item.children)
            : undefined;
          const hasChildren = childItems && childItems.length > 0;
          const allowedByRbac = !item.permission || hasAccess(item.permission, item.path);
          const isAllowed =
            moduleEnabled && (!item.children || hasChildren) && allowedByRbac;

          if (!isAllowed) {
            return null;
          }

          return {
            ...item,
            children: childItems,
          };
        })
        .filter((entry): entry is NonNullable<typeof entry> => entry !== null));

    return filterItems(menuItems);
  }, [hasAccess, isModuleEnabled, getModuleFromResource, isPlainUser, isPrescribingAuthority, isEhrOpdStaff]);

  const flattenedMenuItems = useMemo(() => {
    const flatten = (items: MenuItemType[]): MenuItemType[] =>
      items.flatMap((item) => {
        const children = item.children ?? [];
        if (children.length > 0) {
          return [item, ...flatten(children)];
        }
        return [item];
      });

    return flatten(accessibleMenuItems);
  }, [accessibleMenuItems]);

  const currentPath =
    typeof window !== "undefined" && window.location
      ? window.location.pathname
      : "/dashboard";

  const matchingMenuPaths = useMemo(() => {
    return flattenedMenuItems
      .filter(
        (item) =>
          item.path &&
          (currentPath === item.path || currentPath.startsWith(`${item.path}/`)),
      )
      .map((item) => item.path as string)
      .sort((a, b) => b.length - a.length);
  }, [currentPath, flattenedMenuItems]);

  const isPathActive = useCallback(
    (path: string) => {
      if (!path) return false;
      if (matchingMenuPaths.length === 0) return false;
      return matchingMenuPaths[0] === path;
    },
    [matchingMenuPaths],
  );

  const activeMenuItem = useMemo(() => {
    const bestPath = matchingMenuPaths[0];
    if (!bestPath) return undefined;
    return flattenedMenuItems.find((item) => item.path === bestPath);
  }, [matchingMenuPaths, flattenedMenuItems]);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
    navigate("/login");
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    setMobileOpen(false);
  };

  useEffect(() => {
    const detectDialogState = () => {
      const hasOpenDialog =
        document.querySelector(".MuiDialog-root") !== null ||
        document.querySelector(".hospital-layout-overlay") !== null;
      setIsDialogOpen(hasOpenDialog);
    };

    detectDialogState();
    const observer = new MutationObserver(detectDialogState);
    observer.observe(document.body, { childList: true, subtree: true });

    return () => observer.disconnect();
  }, []);

  const [expandedMenus, setExpandedMenus] = useState<Record<string, boolean>>(
    {},
  );

  const toggleMenu = (menuText: string) => {
    setExpandedMenus((prev) => {
      const isCurrentlyExpanded = prev[menuText];
      // Collapse all other menus and toggle current one
      const newState: Record<string, boolean> = {};
      newState[menuText] = !isCurrentlyExpanded;
      return newState;
    });
  };

  const drawer = (
    <div>
      <Toolbar
        sx={{
          alignItems: "flex-start",
          flexDirection: "column",
          gap: 0.5,
          py: 1.5,
          color: "common.white",
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            width: "100%",
            minWidth: 0,
          }}
        >
          {orgLogoSrc ? (
            <Box
              component="img"
              src={orgLogoSrc}
              alt=""
              sx={{
                maxHeight: 40,
                maxWidth: 140,
                width: "auto",
                objectFit: "contain",
                flexShrink: 0,
              }}
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = "none";
              }}
            />
          ) : null}
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="subtitle2"
              component="div"
              noWrap
              title={currentOrganizationName || undefined}
              sx={{ color: "common.white" }}
            >
              {currentOrganizationName || "—"}
            </Typography>
            <Typography
              variant="caption"
              noWrap
              sx={{ color: "rgba(255,255,255,0.78)" }}
            >
              {appConfig.appName}
            </Typography>
          </Box>
        </Box>
      </Toolbar>
      <Divider sx={{ borderColor: "rgba(255,255,255,0.14)" }} />
      <List>
        {accessibleMenuItems.length === 0 ? (
          <ListItem>
            <ListItemText
              primary="No modules available"
              primaryTypographyProps={{
                variant: "body2",
                sx: { color: "rgba(255,255,255,0.78)" },
              }}
            />
          </ListItem>
        ) : (
          accessibleMenuItems.map((item) => (
            <React.Fragment key={item.text}>
              <ListItem disablePadding>
                <ListItemButton
                  onClick={() => {
                    if (item.children && item.children.length > 0) {
                      toggleMenu(item.text);
                    } else {
                      handleNavigation(item.path);
                    }
                  }}
                  selected={item.path ? isPathActive(item.path) : false}
                  sx={{
                    mx: 1,
                    my: 0.25,
                    borderRadius: 2,
                    color: "rgba(255,255,255,0.9)",
                    "& .MuiListItemIcon-root": {
                      color: "rgba(255,255,255,0.85)",
                      minWidth: 36,
                    },
                    "&:hover": {
                      backgroundColor: "rgba(255,255,255,0.10)",
                    },
                    "&.Mui-selected, &.Mui-selected:hover": {
                      backgroundColor: "rgba(5,167,156,0.22)",
                      boxShadow: "inset 0 0 0 1px rgba(5,167,156,0.35)",
                    },
                  }}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText
                    primary={item.text}
                    primaryTypographyProps={{ fontWeight: 600, fontSize: 14 }}
                  />
                  {item.children && item.children.length > 0 && (
                    <Typography
                      variant="caption"
                      sx={{ color: "rgba(255,255,255,0.75)" }}
                    >
                      {expandedMenus[item.text] ? "▼" : "▶"}
                    </Typography>
                  )}
                </ListItemButton>
              </ListItem>
              {item.children &&
                item.children.length > 0 &&
                expandedMenus[item.text] && (
                  <List component="div" disablePadding>
                    {item.children.map((child) =>
                      child.isSectionHeader ? (
                        <ListItem key={child.text} sx={{ pl: 3, pt: 1, pb: 0 }}>
                          <ListItemText
                            primary={child.text}
                            primaryTypographyProps={{
                              variant: "caption",
                              fontWeight: 700,
                              sx: { color: "rgba(255,255,255,0.70)" },
                            }}
                          />
                        </ListItem>
                      ) : (
                        <ListItem
                          key={child.text}
                          disablePadding
                          sx={{ pl: 2 }}
                        >
                          <ListItemButton
                            onClick={() =>
                              child.path && handleNavigation(child.path)
                            }
                            selected={child.path ? isPathActive(child.path) : false}
                            sx={{
                              mx: 1,
                              my: 0.15,
                              borderRadius: 2,
                              color: "rgba(255,255,255,0.86)",
                              "& .MuiListItemIcon-root": {
                                color: "rgba(255,255,255,0.80)",
                                minWidth: 34,
                              },
                              "&:hover": {
                                backgroundColor: "rgba(255,255,255,0.08)",
                              },
                              "&.Mui-selected, &.Mui-selected:hover": {
                                backgroundColor: "rgba(5,167,156,0.20)",
                                boxShadow: "inset 0 0 0 1px rgba(5,167,156,0.30)",
                              },
                            }}
                          >
                            <ListItemIcon sx={{ minWidth: 36 }}>
                              {child.icon}
                            </ListItemIcon>
                            <ListItemText
                              primary={child.text}
                              primaryTypographyProps={{
                                variant: "body2",
                                fontSize: 13,
                              }}
                            />
                          </ListItemButton>
                        </ListItem>
                      ),
                    )}
                  </List>
                )}
            </React.Fragment>
          ))
        )}
      </List>
    </div>
  );

  return (
    <Box sx={{ display: "flex" }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
          borderRadius: 0,
          zIndex: (theme) => theme.zIndex.modal - 2,
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            disabled={isDialogOpen}
            sx={{ mr: 2, display: { sm: "none" } }}
          >
            <MenuIcon />
          </IconButton>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1.5,
              flexGrow: 1,
              minWidth: 0,
            }}
          >
            {orgLogoSrc ? (
              <Box
                component="img"
                src={orgLogoSrc}
                alt=""
                sx={{
                  display: { xs: "none", md: "block" },
                  maxHeight: 32,
                  maxWidth: 120,
                  objectFit: "contain",
                }}
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display = "none";
                }}
              />
            ) : null}
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h6" noWrap component="div">
                {activeMenuItem?.text || "Dashboard"}
              </Typography>
              {currentOrganizationName ? (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  noWrap
                  component="div"
                >
                  {currentOrganizationName}
                </Typography>
              ) : null}
            </Box>
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            <Typography variant="body2">
              {[user?.firstName, user?.lastName].filter(Boolean).join(" ").trim() || user?.username || ""}
            </Typography>
            <IconButton onClick={handleMenuOpen} color="inherit">
              <Avatar sx={{ width: 32, height: 32 }}>
                {(
                  [user?.firstName?.[0], user?.lastName?.[0]].filter(Boolean).join("") ||
                  user?.username?.[0] ||
                  "U"
                ).toUpperCase()}
              </Avatar>
            </IconButton>
          </Box>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
          >
            <MenuItem
              onClick={() => {
                handleMenuClose();
                navigate("/profile");
              }}
            >
              <ListItemIcon>
                <AccountCircle fontSize="small" />
              </ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem
              onClick={() => {
                handleMenuClose();
                navigate("/settings");
              }}
            >
              <ListItemIcon>
                <Settings fontSize="small" />
              </ListItemIcon>
              Settings
            </MenuItem>
            <MenuItem
              onClick={async () => {
                handleMenuClose();
                try {
                  await reloadRbacState();
                } catch (error) {
                  console.error("Failed to refresh permissions:", error);
                }
                window.location.reload();
              }}
            >
              <ListItemIcon>
                <Refresh fontSize="small" />
              </ListItemIcon>
              Refresh Permissions
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <Logout fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{
          width: { sm: drawerWidth },
          flexShrink: { sm: 0 },
          pointerEvents: isDialogOpen ? "none" : "auto",
        }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", sm: "none" },
            zIndex: (theme) => theme.zIndex.modal - 1,
            "& .MuiDrawer-paper": {
              boxSizing: "border-box",
              width: drawerWidth,
              borderRadius: 0,
              zIndex: (theme) => theme.zIndex.modal - 1,
              color: "common.white",
              background: `linear-gradient(180deg, ${BRAND_PRIMARY} 0%, #5b1f6b 45%, ${BRAND_SECONDARY} 140%)`,
              borderRight: "1px solid rgba(255,255,255,0.14)",
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", sm: "block" },
            "& .MuiDrawer-paper": {
              boxSizing: "border-box",
              width: drawerWidth,
              borderRadius: 0,
              zIndex: (theme) => theme.zIndex.modal - 3,
              color: "common.white",
              background: `linear-gradient(180deg, ${BRAND_PRIMARY} 0%, #5b1f6b 45%, ${BRAND_SECONDARY} 140%)`,
              borderRight: "1px solid rgba(255,255,255,0.14)",
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
};

export default MainLayout;
