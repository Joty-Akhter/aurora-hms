import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useParams, useNavigate } from 'react-router-dom';
import { ErrorBoundary } from '@components/ErrorBoundary';
import { ModuleConfigProvider, useModuleConfig } from '@contexts/ModuleConfigContext';
import { ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { ThemeProvider as OrgThemeProvider, useOrgTheme } from '@contexts/ThemeContext';
import CloseIcon from '@mui/icons-material/Close';
import IconButton from '@mui/material/IconButton';
import { SnackbarProvider, closeSnackbar } from 'notistack';
import { AuthProvider } from '@contexts/AuthContext';
import ProtectedRoute from '@components/ProtectedRoute';
import MainLayout from '@components/Layout/MainLayout';
import Login from '@pages/Login';
import ForgotPassword from '@pages/ForgotPassword';
import WebAppointmentBooking from '@pages/public/WebAppointmentBooking';
import SelectOrganization from '@pages/SelectOrganization';
import Dashboard from '@pages/Dashboard';
import Organizations from '@pages/Organizations';
import OrganizationDetails from '@pages/OrganizationDetails';
import Users from '@pages/Users';
import Roles from '@pages/Roles';
import Permissions from '@pages/Permissions';
import AccessDenied from '@pages/AccessDenied';
import Profile from '@pages/Profile';
import Settings from '@pages/Settings';
import ChartOfAccounts from '@pages/accounting/ChartOfAccounts';
import JournalEntry from '@pages/accounting/JournalEntry';
import TrialBalance from '@pages/accounting/TrialBalance';
import GeneralLedger from '@pages/accounting/GeneralLedger';
import ProfitLoss from '@pages/accounting/ProfitLoss';
import BalanceSheet from '@pages/accounting/BalanceSheet';
import CashFlow from '@pages/accounting/CashFlow';
import Invoices from '@pages/accounting/Invoices';
import Bills from '@pages/accounting/Bills';
import BankReconciliation from '@pages/accounting/BankReconciliation';
import BankAccounts from '@pages/accounting/BankAccounts';
import BankTransactions from '@pages/accounting/BankTransactions';
import AgingReports from '@pages/accounting/AgingReports';
import ARAgingReport from '@pages/accounting/ARAgingReport';
import APAgingReport from '@pages/accounting/APAgingReport';
import CreditNotes from '@pages/accounting/CreditNotes';
import CustomerStatements from '@pages/accounting/CustomerStatements';
import VendorStatements from '@pages/accounting/VendorStatements';
import Vendors from '@pages/accounting/Vendors';
import PaymentReminders from '@pages/accounting/PaymentReminders';
import AccountingDashboard from '@pages/accounting/Dashboard';
import Customers from '@pages/accounting/Customers';
import FiscalYearSetup from '@pages/accounting/FiscalYearSetup';
import OutstandingDocuments from '@pages/accounting/OutstandingDocuments';
import Quotations from '@pages/sales/Quotations';
import SalesOrders from '@pages/sales/SalesOrders';
import SalesDashboard from '@pages/sales/SalesDashboard';
import Products from '@pages/sales/Products';
import SalesCustomers from '@pages/sales/Customers';
import InventoryProducts from '@pages/inventory/Products';
import StockLevels from '@pages/inventory/StockLevels';
import Warehouses from '@pages/inventory/Warehouses';
import InventoryReceive from '@pages/inventory/InventoryReceive';
import BatchTracking from '@pages/inventory/BatchTracking';
import SerialTracking from '@pages/inventory/SerialTracking';
import StockCounting from '@pages/inventory/StockCounting';
import InventoryValuation from '@pages/inventory/InventoryValuation';
import InventoryReports from '@pages/inventory/InventoryReports';
import ReorderManagement from '@pages/inventory/ReorderManagement';
import StockTransfers from '@pages/inventory/StockTransfers';
import TerritoryManagement from '@pages/pharma/TerritoryManagement';
import EmployeeAssignment from '@pages/pharma/EmployeeAssignment';
import ProductReceipt from '@pages/pharma/ProductReceipt';
import ProductDisbursement from '@pages/pharma/ProductDisbursement';
import TargetManagement from '@pages/pharma/TargetManagement';
import SoldProductEntry from '@pages/pharma/SoldProductEntry';
import DepositAmountEntry from '@pages/pharma/DepositAmountEntry';
import ExpenseManagement from '@pages/pharma/ExpenseManagement';
import ProductAdjustment from '@pages/pharma/ProductAdjustment';
import WarehouseStockAdjustment from '@pages/pharma/WarehouseStockAdjustment';
import IncentiveManagement from '@pages/pharma/IncentiveManagement';
import Reports from '@pages/pharma/Reports';
import MonthlyClosingReport from '@pages/pharma/MonthlyClosingReport';
import AreaPerformanceReport from '@pages/pharma/AreaPerformanceReport';
import TerritoryPerformanceReport from '@pages/pharma/TerritoryPerformanceReport';
import InStockTotalAmountReport from '@pages/pharma/InStockTotalAmountReport';
import InStockProductWiseReport from '@pages/pharma/InStockProductWiseReport';
import AreaAllocationReport from '@pages/pharma/AreaAllocationReport';
import MonthWiseAllocationReport from '@pages/pharma/MonthWiseAllocationReport';
import AreaCollectionReport from '@pages/pharma/AreaCollectionReport';
import EmployeeWiseCollectionReport from '@pages/pharma/EmployeeWiseCollectionReport';
import AccountsBalanceReport from '@pages/pharma/AccountsBalanceReport';
import IncomeExpenseReport from '@pages/pharma/IncomeExpenseReport';
import IncentiveReport from '@pages/pharma/IncentiveReport';
import AnalyticsDashboard from '@pages/pharma/AnalyticsDashboard';
import TerritoryAnalyticsDashboard from '@pages/pharma/TerritoryAnalyticsDashboard';
import IncentiveRulesManagement from '@pages/pharma/IncentiveRulesManagement';
import PurchaseDashboard from '@pages/purchase/PurchaseDashboard';
import PurchaseOrders from '@pages/purchase/PurchaseOrders';
import PurchaseReceipts from '@pages/purchase/PurchaseReceipts';
import PurchaseInvoices from '@pages/purchase/PurchaseInvoices';
import PurchaseReports from '@pages/purchase/PurchaseReports';
import VarianceManagement from '@pages/purchase/VarianceManagement';
import HrDashboard from '@pages/hr/HrDashboard';
import EmployeeList from '@pages/hr/EmployeeList';
import EmployeeForm from '@pages/hr/EmployeeForm';
import EmployeeDetail from '@pages/hr/EmployeeDetail';
import DepartmentManagement from '@pages/hr/DepartmentManagement';
import PositionManagement from '@pages/hr/PositionManagement';
import ShiftDefinitions from '@pages/hr/ShiftDefinitions';
import RosterPlanner from '@pages/hr/RosterPlanner';
import AttendanceDashboard from '@pages/hr/AttendanceDashboard';
import DeviceAttendanceSync from '@pages/hr/DeviceAttendanceSync';
import TimesheetManager from '@pages/hr/TimesheetManager';
import LeaveRequestForm from '@pages/hr/LeaveRequestForm';
import LeaveApprovals from '@pages/hr/LeaveApprovals';
import LeaveBalance from '@pages/hr/LeaveBalance';
import HolidayManagement from '@pages/hr/HolidayManagement';
import PayrollDashboard from '@pages/hr/PayrollDashboard';
import PayrollRunManager from '@pages/hr/PayrollRunManager';
import SalaryStructureManager from '@pages/hr/SalaryStructureManager';
import EmployeeSalarySelfService from '@pages/hr/EmployeeSalarySelfService';
import BenefitsManagement from '@pages/hr/BenefitsManagement';
import ReimbursementManagement from '@pages/hr/ReimbursementManagement';
import BonusManagement from '@pages/hr/BonusManagement';
import PerformanceManagement from '@pages/hr/PerformanceManagement';
import GoalManagement from '@pages/hr/GoalManagement';
import DevelopmentPlan from '@pages/hr/DevelopmentPlan';
import TrainingManagement from '@pages/hr/TrainingManagement';
import ProvidentFundDashboard from '@pages/hr/ProvidentFundDashboard';
import ProvidentFundAccounts from '@pages/hr/ProvidentFundAccounts';
import ProvidentFundContributions from '@pages/hr/ProvidentFundContributions';
import ProvidentFundWithdrawals from '@pages/hr/ProvidentFundWithdrawals';
import ProvidentFundNominations from '@pages/hr/ProvidentFundNominations';
import ProvidentFundInterest from '@pages/hr/ProvidentFundInterest';
import ProvidentFundStatements from '@pages/hr/ProvidentFundStatements';
import EpfOrganizationPolicyPage from '@pages/hr/EpfOrganizationPolicyPage';
// TODO: Uncomment when these features are implemented
// import PerformanceIncentiveManagement from '@pages/hr/PerformanceIncentiveManagement';
// import ProjectIncentiveManagement from '@pages/hr/ProjectIncentiveManagement';
// import RetentionReferralManagement from '@pages/hr/RetentionReferralManagement';
// import AdvancedIncentiveFeatures from '@pages/hr/AdvancedIncentiveFeatures';
// import IncentiveDisputeResolution from '@pages/hr/IncentiveDisputeResolution';
// import AdvancedProvidentFundFeatures from '@pages/hr/AdvancedProvidentFundFeatures';
// import ProvidentFundEmployeeSelfService from '@pages/hr/ProvidentFundEmployeeSelfService';
// import ProvidentFundReporting from '@pages/hr/ProvidentFundReporting';
// import IncentiveReporting from '@pages/hr/IncentiveReporting';
// import AdvancedAnalytics from '@pages/hr/AdvancedAnalytics';
// import CustomReportBuilder from '@pages/hr/CustomReportBuilder';
// import ScheduledReporting from '@pages/hr/ScheduledReporting';
import SystemIntegration from '@pages/hr/SystemIntegration';
import PerformanceMonitoring from '@pages/hr/PerformanceMonitoring';
import LoanRegister from '@pages/hr/LoanRegister';
import EmployeeMyLoans from '@pages/hr/EmployeeMyLoans';
import EmployeeMyLoanDetail from '@pages/hr/EmployeeMyLoanDetail';
import LoanDetail from '@pages/hr/LoanDetail';
import LoanApplications from '@pages/hr/LoanApplications';
import LoanApplicationDetail from '@pages/hr/LoanApplicationDetail';
import LoanOrgSettings from '@pages/hr/LoanOrgSettings';
import LoanOrgAudit from '@pages/hr/LoanOrgAudit';
import LoanPayrollRecoveries from '@pages/hr/LoanPayrollRecoveries';
import LeadDashboard from '@pages/crm/LeadDashboard';
import LeadList from '@pages/crm/LeadList';
import LeadForm from '@pages/crm/LeadForm';
import LeadDetail from '@pages/crm/LeadDetail';
import AccountList from '@pages/crm/AccountList';
import AccountForm from '@pages/crm/AccountForm';
import AccountDetail from '@pages/crm/AccountDetail';
import ContactList from '@pages/crm/ContactList';
import ContactForm from '@pages/crm/ContactForm';
import ContactDetail from '@pages/crm/ContactDetail';
import OpportunityDashboard from '@pages/crm/OpportunityDashboard';
import OpportunityList from '@pages/crm/OpportunityList';
import OpportunityForm from '@pages/crm/OpportunityForm';
import OpportunityDetail from '@pages/crm/OpportunityDetail';
import PipelineKanban from '@pages/crm/PipelineKanban';
import SalesForecast from '@pages/crm/SalesForecast';
import CampaignDashboard from '@pages/crm/CampaignDashboard';
import CampaignList from '@pages/crm/CampaignList';
import CampaignForm from '@pages/crm/CampaignForm';
import TaskManager from '@pages/crm/TaskManager';
import CalendarView from '@pages/crm/CalendarView';
import EmailTemplateManager from '@pages/crm/EmailTemplateManager';
import CaseDashboard from '@pages/crm/CaseDashboard';
import CaseList from '@pages/crm/CaseList';
import CaseForm from '@pages/crm/CaseForm';
import CaseDetail from '@pages/crm/CaseDetail';
import KnowledgeBaseList from '@pages/crm/KnowledgeBaseList';
import KnowledgeBaseForm from '@pages/crm/KnowledgeBaseForm';
import CrmReports from '@pages/crm/CrmReports';

// Hospital/EHR Module
import PatientList from '@pages/hospital/PatientList';
import PatientForm from '@pages/hospital/PatientForm';
import PatientDetail from '@pages/hospital/PatientDetail';
import MedicalHistoryPage from '@pages/hospital/MedicalHistory';
import AllergyManagementPage from '@pages/hospital/AllergyManagement';
import ImmunizationRecordsPage from '@pages/hospital/ImmunizationRecords';
import FamilyHistoryPage from '@pages/hospital/FamilyHistory';
import SocialHistoryPage from '@pages/hospital/SocialHistory';
import VitalSignsPage from '@pages/hospital/VitalSigns';
import VitalSignsTrendsPage from '@pages/hospital/VitalSignsTrends';
import ClinicalNotesPage from '@pages/hospital/ClinicalNotes';
import DoctorNotesPage from '@pages/hospital/DoctorNotes';
import EncounterManagementPage from '@pages/hospital/EncounterManagement';
import NoteTemplatesPage from '@pages/hospital/NoteTemplates';
import ProblemListPage from '@pages/hospital/ProblemList';
import PrescriptionManagementPage from '@pages/hospital/PrescriptionManagement';
import LabOrderManagement from '@pages/hospital/LabOrderManagement';
import LabResultsList from '@pages/hospital/LabResultsList';
import LabResultDetail from '@pages/hospital/LabResultDetail';
import LabResultComparison from '@pages/hospital/LabResultComparison';
import LabResultTrend from '@pages/hospital/LabResultTrend';
import LabResultCorrelation from '@pages/hospital/LabResultCorrelation';
import LabResultReceiptForm from '@pages/hospital/LabResultReceiptForm';
import ImagingStudyResultsList from '@pages/hospital/ImagingStudyResultsList';
import ImagingStudyDetail from '@pages/hospital/ImagingStudyDetail';
import ImagingStudyTimeline from '@pages/hospital/ImagingStudyTimeline';
import DICOMImageViewer from '@pages/hospital/DICOMImageViewer';
import ImagingAlerts from '@pages/hospital/ImagingAlerts';
import PatientTimelinePage from '@pages/hospital/PatientTimeline';
import ClinicalReportingPage from '@pages/hospital/ClinicalReporting';
import PrescriptionReportingPage from '@pages/hospital/PrescriptionReporting';
import MedicationHistoryPage from '@pages/hospital/MedicationHistory';
import MedicationReportingPage from '@pages/hospital/MedicationReporting';
import DoctorList from '@pages/hospital/DoctorList';
import DoctorForm from '@pages/hospital/DoctorForm';
import DoctorScheduleManagement from '@pages/hospital/DoctorScheduleManagement';
import DoctorDepartmentList from '@pages/hospital/DoctorDepartmentList';
import DoctorDashboard from '@pages/hospital/DoctorDashboard';
import MedicalCodeCatalogPage from '@pages/hospital/MedicalCodeCatalog';
import ClinicalChartCatalogPage from '@pages/hospital/ClinicalChartCatalog';
import PrescriptionTemplates from '@pages/hospital/PrescriptionTemplates';
import PrescriptionAdmin from '@pages/hospital/PrescriptionAdmin';
import PharmacyCatalogPage from '@pages/hospital/PharmacyCatalog';
import PharmacyLocationsPage from '@pages/hospital/PharmacyLocations';
import PharmacyDispensePage from '@pages/hospital/PharmacyDispense';
import PharmacyStockPage from '@pages/hospital/PharmacyStock';
import PharmacyReportsPage from '@pages/hospital/PharmacyReports';
import BillingChargesPage from '@pages/hospital/BillingCharges';
import BillingInvoicesPage from '@pages/hospital/BillingInvoices';
import BillingPaymentsPage from '@pages/hospital/BillingPayments';
import BillingReportsPage from '@pages/hospital/BillingReports';
import CardManagementPage from '@pages/hospital/CardManagement';
import CardProductsPage from '@pages/hospital/CardProducts';
import MyCardsPage from '@pages/hospital/MyCardsPage';
import MyCardStatementPage from '@pages/hospital/MyCardStatementPage';
import CorporateClientsPage from '@pages/hospital/CorporateClients';
import CorporateContractsPage from '@pages/hospital/CorporateContracts';
import ContractCoveragePage from '@pages/hospital/ContractCoverage';
import PackagesPage from '@pages/hospital/Packages';
import PackageDetailPage from '@pages/hospital/PackageDetail';
import DiscountSchemesPage from '@pages/hospital/DiscountSchemes';
import SchemeApprovalLevelsPage from '@pages/hospital/SchemeApprovalLevels';
import DiscountDecisionsPage from '@pages/hospital/DiscountDecisions';
import CorporateDiscountReportsPage from '@pages/hospital/CorporateDiscountReports';
import ClinicalOrderEntryPage from '@pages/hospital/ClinicalOrderEntry';
import ClinicalOrderSetsPage from '@pages/hospital/ClinicalOrderSets';
import ClinicalOrdersListPage from '@pages/hospital/ClinicalOrdersList';
import ClinicalOrderDetailPage from '@pages/hospital/ClinicalOrderDetail';
import ClinicalWorklistsPage from '@pages/hospital/ClinicalWorklists';
import ClinicalOrdersReportsPage from '@pages/hospital/ClinicalOrdersReports';
import SchedulingResourcesPage from '@pages/hospital/SchedulingResources';
import SchedulingSlotTemplatesPage from '@pages/hospital/SchedulingSlotTemplates';
import SchedulingBlackoutsPage from '@pages/hospital/SchedulingBlackouts';
import SchedulingAvailabilityPage from '@pages/hospital/SchedulingAvailability';
import SchedulingReservationsPage from '@pages/hospital/SchedulingReservations';
import SchedulingAppointmentsPage from '@pages/hospital/SchedulingAppointments';
import SchedulingQueuePage from '@pages/hospital/SchedulingQueue';
import SchedulingPlannedAdmissionsPage from '@pages/hospital/SchedulingPlannedAdmissions';
import SchedulingExpectedAdmissionsPage from '@pages/hospital/SchedulingExpectedAdmissions';
import SchedulingRosterBlocksPage from '@pages/hospital/SchedulingRosterBlocks';
import SchedulingWaitlistPage from '@pages/hospital/SchedulingWaitlist';
import SchedulingReportsPage from '@pages/hospital/SchedulingReports';
import SchedulingDoctorMappingsPage from '@pages/hospital/SchedulingDoctorMappings';
import SchedulingAuditLogPage from '@pages/hospital/SchedulingAuditLog';
import CommunicationDashboard from '@pages/communication/CommunicationDashboard';
import CommunicationTemplates from '@pages/communication/CommunicationTemplates';
import CommunicationDeliveries from '@pages/communication/CommunicationDeliveries';
import CommunicationOperations from '@pages/communication/CommunicationOperations';

// Manufacturing Module
import ManufacturingDashboard from '@pages/manufacturing/ManufacturingDashboard';
import BomList from '@pages/manufacturing/BomList';
import BomForm from '@pages/manufacturing/BomForm';
import BomTreeView from '@pages/manufacturing/BomTreeView';
import ProductRoutingList from '@pages/manufacturing/ProductRoutingList';
import ProductRoutingForm from '@pages/manufacturing/ProductRoutingForm';
import WorkOrderList from '@pages/manufacturing/WorkOrderList';
import ProductionTracking from '@pages/manufacturing/ProductionTracking';
import WorkOrderWizard from '@pages/manufacturing/WorkOrderWizard';
import WorkOrderDetail from '@pages/manufacturing/WorkOrderDetail';
import ShopFloorDashboard from '@pages/manufacturing/ShopFloorDashboard';
import QualityInspectionList from '@pages/manufacturing/quality/QualityInspectionList';
import QualityInspectionForm from '@pages/manufacturing/quality/QualityInspectionForm';
import NonConformanceList from '@pages/manufacturing/quality/NonConformanceList';
import NonConformanceForm from '@pages/manufacturing/quality/NonConformanceForm';
import WorkCenterList from '@pages/manufacturing/WorkCenterList';
import WorkCenterForm from '@pages/manufacturing/WorkCenterForm';
import MaintenanceCalendar from '@pages/manufacturing/MaintenanceCalendar';
// TODO: Uncomment when these features are implemented
// import MaintenanceList from '@pages/manufacturing/MaintenanceList';
// import ManufacturingAnalytics from '@pages/manufacturing/ManufacturingAnalytics';
import ManufacturingAnalyticsDashboard from '@pages/manufacturing/AnalyticsDashboard';


// Wrapper component for DICOM Image Viewer route
const DICOMImageViewerWrapper: React.FC = () => {
  const { studyId } = useParams<{ studyId: string }>();
  const navigate = useNavigate();
  const [open, setOpen] = useState(true);

  if (!studyId) {
    return null;
  }

  return (
    <DICOMImageViewer
      studyId={studyId}
      open={open}
      onClose={() => {
        setOpen(false);
        navigate(-1);
      }}
    />
  );
};

const AppContent: React.FC = () => {
  const { isModuleEnabled } = useModuleConfig();
  return (
    <AuthProvider>
      <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
            <Routes>
              {/* Public Routes */}
              <Route path="/login" element={<Login />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/book-appointment" element={<WebAppointmentBooking />} />
              <Route
                path="/select-organization"
                element={
                  <ProtectedRoute>
                    <SelectOrganization />
                  </ProtectedRoute>
                }
              />

              {/* Protected Routes */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <MainLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<Dashboard />} />
                <Route path="organizations" element={<Organizations />} />
                <Route path="organizations/:id" element={<OrganizationDetails />} />
                <Route path="users" element={<Users />} />
                <Route path="roles" element={<Roles />} />
                <Route path="permissions" element={<Permissions />} />
                
                {/* Accounting Routes - Phase 1.1 */}
                {isModuleEnabled('accounting') && (
                  <>
                    <Route path="accounting/dashboard" element={<AccountingDashboard />} />
                <Route path="accounting/chart-of-accounts" element={<ChartOfAccounts />} />
                <Route path="accounting/journal-entry" element={<JournalEntry />} />
                <Route path="accounting/fiscal-year-setup" element={<FiscalYearSetup />} />
                <Route path="accounting/customers" element={<Customers />} />
                <Route path="accounting/outstanding" element={<OutstandingDocuments />} />
                <Route path="accounting/trial-balance" element={<TrialBalance />} />
                <Route path="accounting/general-ledger" element={<GeneralLedger />} />
                <Route path="accounting/profit-loss" element={<ProfitLoss />} />
                <Route path="accounting/balance-sheet" element={<BalanceSheet />} />
                <Route path="accounting/cash-flow" element={<CashFlow />} />
                
                {/* Accounting Routes - Phase 1.2 */}
                <Route path="accounting/invoices" element={<Invoices />} />
                <Route path="accounting/credit-notes" element={<CreditNotes />} />
                <Route path="accounting/bills" element={<Bills />} />
                <Route path="accounting/bank-accounts" element={<BankAccounts />} />
                <Route path="accounting/bank-transactions" element={<BankTransactions />} />
                <Route path="accounting/bank-reconciliation" element={<BankReconciliation />} />
                <Route path="accounting/aging-reports" element={<AgingReports />} />
                <Route path="accounting/ar-aging-report" element={<ARAgingReport />} />
                <Route path="accounting/ap-aging-report" element={<APAgingReport />} />
                <Route path="accounting/customer-statements" element={<CustomerStatements />} />
                <Route path="accounting/vendors" element={<Vendors />} />
                <Route path="accounting/vendor-statements" element={<VendorStatements />} />
                    <Route path="accounting/payment-reminders" element={<PaymentReminders />} />
                  </>
                )}
                
                {/* Sales Routes - Phase 2.0 */}
                {isModuleEnabled('sales') && (
                  <>
                    <Route path="sales/dashboard" element={<SalesDashboard />} />
                <Route path="sales/products" element={<Products />} />
                <Route path="sales/customers" element={<SalesCustomers />} />
                <Route path="sales/quotations" element={<Quotations />} />
                    <Route path="sales/orders" element={<SalesOrders />} />
                  </>
                )}
                
                {/* Inventory Routes - Phase 3 Complete */}
                {isModuleEnabled('inventory') && (
                  <>
                    <Route path="inventory/products" element={<InventoryProducts />} />
                <Route path="inventory/stock" element={<StockLevels />} />
                <Route path="inventory/warehouses" element={<Warehouses />} />
                <Route path="inventory/receive" element={<InventoryReceive />} />
                <Route path="inventory/batches" element={<BatchTracking />} />
                <Route path="inventory/serials" element={<SerialTracking />} />
                <Route path="inventory/counting" element={<StockCounting />} />
                <Route path="inventory/valuation" element={<InventoryValuation />} />
                <Route path="inventory/reports" element={<InventoryReports />} />
                <Route path="inventory/reorder" element={<ReorderManagement />} />
                    <Route path="inventory/transfers" element={<StockTransfers />} />
                  </>
                )}
                
                {/* Pharma Routes */}
                {isModuleEnabled('pharma') && (
                  <>
                    <Route path="pharma/territories" element={<TerritoryManagement />} />
                <Route path="pharma/employee-assignments" element={<EmployeeAssignment />} />
                <Route path="pharma/product-receipts" element={<ProductReceipt />} />
                <Route path="pharma/product-disbursements" element={<ProductDisbursement />} />
                <Route path="pharma/targets" element={<TargetManagement />} />
                <Route path="pharma/sold-product-entries" element={<SoldProductEntry />} />
                <Route path="pharma/deposits" element={<DepositAmountEntry />} />
                <Route path="pharma/expenses" element={<ExpenseManagement />} />
                <Route path="pharma/adjustments" element={<ProductAdjustment />} />
                {isModuleEnabled('inventory') && (
                  <Route path="pharma/warehouse-stock-adjustment" element={<WarehouseStockAdjustment />} />
                )}
                <Route path="pharma/incentives" element={<IncentiveManagement />} />
                <Route path="pharma/reports" element={<Reports />} />
                <Route path="pharma/reports/monthly-closing" element={<MonthlyClosingReport />} />
                <Route path="pharma/reports/area-performance" element={<AreaPerformanceReport />} />
                <Route path="pharma/reports/territory-performance" element={<TerritoryPerformanceReport />} />
                <Route path="pharma/reports/inventory/total-amount" element={<InStockTotalAmountReport />} />
                <Route path="pharma/reports/inventory/product-wise" element={<InStockProductWiseReport />} />
                <Route path="pharma/reports/inventory/area-allocation" element={<AreaAllocationReport />} />
                <Route path="pharma/reports/inventory/month-wise-allocation" element={<MonthWiseAllocationReport />} />
                <Route path="pharma/reports/collection/area-wise" element={<AreaCollectionReport />} />
                <Route path="pharma/reports/collection/employee-wise" element={<EmployeeWiseCollectionReport />} />
                <Route path="pharma/reports/financial/accounts-balance" element={<AccountsBalanceReport />} />
                <Route path="pharma/reports/financial/income-expense" element={<IncomeExpenseReport />} />
                <Route path="pharma/reports/financial/incentive" element={<IncentiveReport />} />
                <Route path="pharma/analytics" element={<AnalyticsDashboard />} />
                <Route path="pharma/territory-analytics" element={<TerritoryAnalyticsDashboard />} />
                    <Route path="pharma/incentive-rules" element={<IncentiveRulesManagement />} />
                  </>
                )}
                
                {/* Purchase Routes - Phase 4 */}
                {isModuleEnabled('purchase') && (
                  <>
                    <Route path="purchase/dashboard" element={<PurchaseDashboard />} />
                <Route path="purchase/orders" element={<PurchaseOrders />} />
                <Route path="purchase/receipts" element={<PurchaseReceipts />} />
                <Route path="purchase/invoices" element={<PurchaseInvoices />} />
                <Route path="purchase/variances" element={<VarianceManagement />} />
                    <Route path="purchase/reports" element={<PurchaseReports />} />
                  </>
                )}
                
                {/* HR Routes - Phase 5.1 */}
                {isModuleEnabled('hr') && (
                  <>
                    <Route path="hr/dashboard" element={<HrDashboard />} />
                <Route path="hr/loans/applications/:applicationId" element={<LoanApplicationDetail />} />
                <Route path="hr/loans/applications" element={<LoanApplications />} />
                <Route path="hr/loans/settings" element={<LoanOrgSettings />} />
                <Route path="hr/loans/org-audit" element={<LoanOrgAudit />} />
                <Route path="hr/loans/payroll-recoveries" element={<LoanPayrollRecoveries />} />
                <Route path="hr/loans/:loanId" element={<LoanDetail />} />
                <Route path="hr/loans" element={<LoanRegister />} />
                <Route path="hr/my-loans" element={<EmployeeMyLoans />} />
                <Route path="hr/my-loans/:loanId" element={<EmployeeMyLoanDetail />} />
                <Route path="hr/employees" element={<EmployeeList />} />
                <Route path="hr/employees/new" element={<EmployeeForm />} />
                <Route path="hr/employees/:id" element={<EmployeeDetail />} />
                <Route path="hr/employees/:id/edit" element={<EmployeeForm />} />
                <Route path="hr/departments" element={<DepartmentManagement />} />
                <Route path="hr/positions" element={<PositionManagement />} />
                <Route path="hr/shift-definitions" element={<ShiftDefinitions />} />
                <Route path="hr/roster" element={<RosterPlanner />} />
                <Route path="hr/attendance" element={<AttendanceDashboard />} />
                <Route path="hr/device-attendance" element={<DeviceAttendanceSync />} />
                <Route path="hr/timesheets" element={<TimesheetManager />} />
                <Route path="hr/leave-requests" element={<LeaveRequestForm />} />
                <Route path="hr/leave-approvals" element={<LeaveApprovals />} />
                <Route path="hr/leave-balance" element={<LeaveBalance />} />
                <Route path="hr/holidays" element={<HolidayManagement />} />
                <Route path="hr/payroll" element={<PayrollDashboard />} />
                <Route path="hr/payroll-runs" element={<PayrollRunManager />} />
                <Route path="hr/salary" element={<SalaryStructureManager />} />
                <Route path="hr/my-salary" element={<EmployeeSalarySelfService />} />
                <Route path="hr/benefits" element={<BenefitsManagement />} />
                <Route path="hr/reimbursements" element={<ReimbursementManagement />} />
                <Route path="hr/bonuses" element={<BonusManagement />} />
                <Route path="hr/performance" element={<PerformanceManagement />} />
                <Route path="hr/goals" element={<GoalManagement />} />
                <Route path="hr/development" element={<DevelopmentPlan />} />
                <Route path="hr/training" element={<TrainingManagement />} />
                {/* HR Routes - Provident Fund & Incentives */}
                <Route path="hr/provident-fund" element={<ProvidentFundDashboard />} />
                <Route path="hr/provident-fund/accounts" element={<ProvidentFundAccounts />} />
                <Route path="hr/provident-fund/contributions" element={<ProvidentFundContributions />} />
                <Route path="hr/provident-fund/withdrawals" element={<ProvidentFundWithdrawals />} />
                <Route path="hr/provident-fund/nominations" element={<ProvidentFundNominations />} />
                <Route path="hr/provident-fund/interest" element={<ProvidentFundInterest />} />
                <Route path="hr/provident-fund/statements" element={<ProvidentFundStatements />} />
                <Route path="hr/provident-fund/organization-policy" element={<EpfOrganizationPolicyPage />} />
                    {/* Phase 8 - Integration & Testing */}
                    <Route path="hr/system/integration" element={<SystemIntegration />} />
                    <Route path="hr/system/performance" element={<PerformanceMonitoring />} />
                  </>
                )}
                
                {/* CRM Routes - Phase 6.1, 6.2, 6.3 & 6.4 Complete */}
                {isModuleEnabled('crm') && (
                  <>
                    <Route path="crm" element={<LeadDashboard />} />
                <Route path="crm/dashboard" element={<LeadDashboard />} />
                <Route path="crm/leads" element={<LeadList />} />
                <Route path="crm/leads/new" element={<LeadForm />} />
                <Route path="crm/leads/:id" element={<LeadDetail />} />
                <Route path="crm/leads/:id/edit" element={<LeadForm />} />
                <Route path="crm/accounts" element={<AccountList />} />
                <Route path="crm/accounts/new" element={<AccountForm />} />
                <Route path="crm/accounts/:id" element={<AccountDetail />} />
                <Route path="crm/accounts/:id/edit" element={<AccountForm />} />
                <Route path="crm/contacts" element={<ContactList />} />
                <Route path="crm/contacts/new" element={<ContactForm />} />
                <Route path="crm/contacts/:id" element={<ContactDetail />} />
                <Route path="crm/contacts/:id/edit" element={<ContactForm />} />
                <Route path="crm/opportunities" element={<OpportunityList />} />
                <Route path="crm/opportunities/new" element={<OpportunityForm />} />
                <Route path="crm/opportunities/:id" element={<OpportunityDetail />} />
                <Route path="crm/opportunities/:id/edit" element={<OpportunityForm />} />
                <Route path="crm/opportunity-dashboard" element={<OpportunityDashboard />} />
                <Route path="crm/pipeline" element={<PipelineKanban />} />
                <Route path="crm/forecast" element={<SalesForecast />} />
                <Route path="crm/campaigns" element={<CampaignList />} />
                <Route path="crm/campaigns/new" element={<CampaignForm />} />
                <Route path="crm/campaigns/:id/edit" element={<CampaignForm />} />
                <Route path="crm/campaign-dashboard" element={<CampaignDashboard />} />
                <Route path="crm/tasks" element={<TaskManager />} />
                <Route path="crm/calendar" element={<CalendarView />} />
                <Route path="crm/email-templates" element={<EmailTemplateManager />} />
                <Route path="crm/cases" element={<CaseList />} />
                <Route path="crm/cases/new" element={<CaseForm />} />
                <Route path="crm/cases/:id" element={<CaseDetail />} />
                <Route path="crm/cases/:id/edit" element={<CaseForm />} />
                <Route path="crm/support" element={<CaseDashboard />} />
                <Route path="crm/knowledge-base" element={<KnowledgeBaseList />} />
                <Route path="crm/knowledge-base/new" element={<KnowledgeBaseForm />} />
                <Route path="crm/knowledge-base/:id/edit" element={<KnowledgeBaseForm />} />
                    <Route path="crm/analytics" element={<CrmReports />} />
                  </>
                )}
                
                {/* Manufacturing Module Routes */}
                {isModuleEnabled('manufacturing') && (
                  <>
                    <Route path="manufacturing" element={<ManufacturingDashboard />} />
                <Route path="manufacturing/dashboard" element={<ManufacturingDashboard />} />
                
                {/* BOM Routes */}
                <Route path="manufacturing/boms" element={<BomList />} />
                <Route path="manufacturing/boms/new" element={<BomForm />} />
                <Route path="manufacturing/boms/:bomId" element={<BomTreeView />} />
                <Route path="manufacturing/boms/:bomId/edit" element={<BomForm />} />
                
                {/* Routing Routes */}
                <Route path="manufacturing/routings" element={<ProductRoutingList />} />
                <Route path="manufacturing/routings/new" element={<ProductRoutingForm />} />
                <Route path="manufacturing/routings/:routingId" element={<ProductRoutingForm />} />
                <Route path="manufacturing/routings/:routingId/edit" element={<ProductRoutingForm />} />
                
                {/* Work Order Routes */}
                <Route path="manufacturing/work-orders" element={<WorkOrderList />} />
                <Route path="manufacturing/work-orders/new" element={<WorkOrderWizard />} />
                <Route path="manufacturing/work-orders/:workOrderId" element={<WorkOrderDetail />} />
                <Route path="manufacturing/shop-floor" element={<ShopFloorDashboard />} />
                <Route path="manufacturing/production-tracking" element={<ProductionTracking />} />
                
                {/* Quality Routes */}
                <Route path="manufacturing/quality/inspections" element={<QualityInspectionList />} />
                <Route path="manufacturing/quality/inspections/new" element={<QualityInspectionForm />} />
                <Route path="manufacturing/quality/inspections/:inspectionId" element={<QualityInspectionForm />} />
                <Route path="manufacturing/quality/non-conformances" element={<NonConformanceList />} />
                <Route path="manufacturing/quality/non-conformances/new" element={<NonConformanceForm />} />
                <Route path="manufacturing/quality/non-conformances/:ncId" element={<NonConformanceForm />} />
                
                {/* Work Center & Maintenance Routes */}
                <Route path="manufacturing/work-centers" element={<WorkCenterList />} />
                <Route path="manufacturing/work-centers/new" element={<WorkCenterForm />} />
                <Route path="manufacturing/work-centers/:workCenterId" element={<WorkCenterForm />} />
                <Route path="manufacturing/maintenance" element={<MaintenanceCalendar />} />
                
                    {/* Analytics Routes */}
                    <Route path="manufacturing/analytics" element={<ManufacturingAnalyticsDashboard />} />
                  </>
                )}
                
                {/* Hospital/EHR Routes - Phase EHR.1 & EHR.2 */}
                {isModuleEnabled('hospital') && (
                  <>
                    <Route path="hospital" element={<Navigate to="/hospital/patients" replace />} />
                    {/* Doctor Management Routes - Phase 1.3 */}
                    <Route path="hospital/doctors" element={<DoctorList />} />
                    <Route path="hospital/doctors/new" element={<DoctorForm />} />
                    <Route path="hospital/doctors/:id" element={<DoctorForm />} />
                    <Route path="hospital/doctors/schedule" element={<DoctorScheduleManagement />} />
                    <Route path="hospital/doctor-departments" element={<DoctorDepartmentList />} />
                    
                    <Route path="hospital/patients" element={<PatientList />} />
                    <Route path="hospital/patients/new" element={<PatientForm />} />
                    <Route path="hospital/patients/:id" element={<PatientDetail />} />
                    <Route path="hospital/patients/:id/edit" element={<PatientForm />} />
                    <Route path="hospital/patients/:id/medical-history" element={<MedicalHistoryPage />} />
                    <Route path="hospital/patients/:id/allergies" element={<AllergyManagementPage />} />
                    <Route path="hospital/patients/:id/immunizations" element={<ImmunizationRecordsPage />} />
                    <Route path="hospital/patients/:id/family-history" element={<FamilyHistoryPage />} />
                    <Route path="hospital/patients/:id/social-history" element={<SocialHistoryPage />} />
                    <Route path="hospital/patients/:id/vital-signs" element={<VitalSignsPage />} />
                    <Route path="hospital/patients/:id/vital-signs/trends" element={<VitalSignsTrendsPage />} />
                    <Route path="hospital/patients/:id/encounters" element={<EncounterManagementPage />} />
                    <Route path="hospital/patients/:id/clinical-notes" element={<ClinicalNotesPage />} />
                    <Route path="hospital/notes/templates" element={<NoteTemplatesPage />} />
                    <Route path="hospital/patients/:id/problems" element={<ProblemListPage />} />
                    <Route path="hospital/patients/:id/prescriptions" element={<PrescriptionManagementPage />} />
                    <Route path="hospital/prescriptions/templates" element={<PrescriptionTemplates />} />
                    <Route path="hospital/prescriptions/admin" element={<PrescriptionAdmin />} />
                    <Route path="hospital/doctor-dashboard" element={<DoctorDashboard />} />
                    <Route path="hospital/doctor-notes" element={<DoctorNotesPage />} />
                    <Route path="hospital/medical-codes" element={<MedicalCodeCatalogPage />} />
                    <Route path="hospital/medical-codes/icd10" element={<MedicalCodeCatalogPage />} />
                    <Route path="hospital/medical-codes/icd11" element={<MedicalCodeCatalogPage />} />
                    <Route path="hospital/configurations/clinical-chart" element={<ClinicalChartCatalogPage />} />
                    <Route path="hospital/patients/:id/medication-history" element={<MedicationHistoryPage />} />
                    <Route path="hospital/patients/:id/lab-orders" element={<LabOrderManagement />} />
                    <Route path="hospital/patients/:id/lab-results" element={<LabResultsList />} />
                    <Route path="hospital/patients/:id/lab-results/receive/:orderId" element={<LabResultReceiptForm />} />
                    <Route path="hospital/patients/:id/lab-results/:resultId" element={<LabResultDetail />} />
                    <Route path="hospital/patients/:id/lab-results/:resultId/compare" element={<LabResultComparison />} />
                    <Route path="hospital/patients/:id/lab-results/trend" element={<LabResultTrend />} />
                    <Route path="hospital/patients/:id/lab-results/:resultId/correlated" element={<LabResultCorrelation />} />
                    <Route path="hospital/patients/:id/timeline" element={<PatientTimelinePage />} />
                    <Route path="hospital/patients/:id/clinical-report" element={<ClinicalReportingPage />} />
                    <Route path="hospital/patients/:id/prescription-report" element={<PrescriptionReportingPage />} />
                    <Route path="hospital/patients/:id/medication-report" element={<MedicationReportingPage />} />
                    <Route path="hospital/patients/:id/imaging-studies" element={<ImagingStudyResultsList />} />
                    <Route path="hospital/patients/:id/imaging-studies/timeline" element={<ImagingStudyTimeline />} />
                    <Route path="hospital/patients/:id/imaging-studies/:studyId" element={<ImagingStudyDetail />} />
                    <Route path="hospital/patients/:id/imaging-alerts" element={<ImagingAlerts />} />
                    <Route
                      path="hospital/imaging-studies/:studyId/images"
                      element={
                        <DICOMImageViewerWrapper />
                      }
                    />
                    {/* Hospital Pharmacy – Phases 1–4 */}
                    <Route path="hospital/pharmacy/catalog" element={<PharmacyCatalogPage />} />
                    <Route path="hospital/pharmacy/locations" element={<PharmacyLocationsPage />} />
                    <Route path="hospital/pharmacy/stock" element={<PharmacyStockPage />} />
                    <Route path="hospital/pharmacy/dispense" element={<PharmacyDispensePage />} />
                    <Route path="hospital/pharmacy/reports" element={<PharmacyReportsPage />} />
                    {/* Hospital Billing – Phases 1–4 */}
                    <Route path="hospital/billing/charges" element={<BillingChargesPage />} />
                    <Route path="hospital/billing/invoices" element={<BillingInvoicesPage />} />
                    <Route path="hospital/billing/payments" element={<BillingPaymentsPage />} />
                    <Route path="hospital/billing/reports" element={<BillingReportsPage />} />
                    <Route path="hospital/clinical-orders/entry" element={<ClinicalOrderEntryPage />} />
                    <Route path="hospital/clinical-orders/sets" element={<ClinicalOrderSetsPage />} />
                    <Route path="hospital/clinical-orders/sets/:id" element={<ClinicalOrderSetsPage />} />
                    <Route path="hospital/clinical-orders/orders/:id" element={<ClinicalOrderDetailPage />} />
                    <Route path="hospital/clinical-orders/orders" element={<ClinicalOrdersListPage />} />
                    <Route path="hospital/clinical-orders/worklists" element={<ClinicalWorklistsPage />} />
                    <Route path="hospital/clinical-orders/reports" element={<ClinicalOrdersReportsPage />} />
                    <Route path="hospital/corporate-discount/corporates" element={<CorporateClientsPage />} />
                    <Route path="hospital/corporate-discount/contracts" element={<CorporateContractsPage />} />
                    <Route path="hospital/corporate-discount/contracts/:contractId/coverage" element={<ContractCoveragePage />} />
                    <Route path="hospital/corporate-discount/packages" element={<PackagesPage />} />
                    <Route path="hospital/corporate-discount/packages/:packageId" element={<PackageDetailPage />} />
                    <Route path="hospital/corporate-discount/discount-schemes" element={<DiscountSchemesPage />} />
                    <Route path="hospital/corporate-discount/discount-schemes/:schemeId" element={<SchemeApprovalLevelsPage />} />
                    <Route path="hospital/corporate-discount/decisions" element={<DiscountDecisionsPage />} />
                    <Route path="hospital/corporate-discount/reports" element={<CorporateDiscountReportsPage />} />
                    <Route path="hospital/cards" element={<CardManagementPage />} />
                    <Route path="hospital/cards/products" element={<CardProductsPage />} />
                    <Route path="hospital/scheduling/resources" element={<SchedulingResourcesPage />} />
                    <Route path="hospital/scheduling/slot-templates" element={<SchedulingSlotTemplatesPage />} />
                    <Route path="hospital/scheduling/blackouts" element={<SchedulingBlackoutsPage />} />
                    <Route path="hospital/scheduling/availability" element={<SchedulingAvailabilityPage />} />
                    <Route path="hospital/scheduling/reservations" element={<SchedulingReservationsPage />} />
                    <Route path="hospital/scheduling/appointments" element={<SchedulingAppointmentsPage />} />
                    <Route path="hospital/scheduling/queue" element={<SchedulingQueuePage />} />
                    <Route path="hospital/scheduling/planned-admissions" element={<SchedulingPlannedAdmissionsPage />} />
                    <Route path="hospital/scheduling/expected-admissions" element={<SchedulingExpectedAdmissionsPage />} />
                    <Route path="hospital/scheduling/roster-blocks" element={<SchedulingRosterBlocksPage />} />
                    <Route path="hospital/scheduling/waitlist" element={<SchedulingWaitlistPage />} />
                    <Route path="hospital/scheduling/reports" element={<SchedulingReportsPage />} />
                    <Route path="hospital/scheduling/doctor-mappings" element={<SchedulingDoctorMappingsPage />} />
                    <Route path="hospital/scheduling/audit-log" element={<SchedulingAuditLogPage />} />
                    <Route path="portal/cards" element={<MyCardsPage />} />
                    <Route path="portal/cards/:id/statement" element={<MyCardStatementPage />} />
                  </>
                )}
                <Route path="communication" element={<CommunicationDashboard />} />
                <Route path="communication/templates" element={<CommunicationTemplates />} />
                <Route path="communication/deliveries" element={<CommunicationDeliveries />} />
                <Route path="communication/operations" element={<CommunicationOperations />} />
                <Route path="communication/foundation" element={<Navigate to="/communication" replace />} />
                <Route path="communication/phase-2-workbench" element={<Navigate to="/communication/templates" replace />} />
                <Route path="communication/phase-3-workbench" element={<Navigate to="/communication/deliveries" replace />} />
                
                <Route path="profile" element={<Profile />} />
                <Route path="settings" element={<Settings />} />
                <Route path="forbidden" element={<AccessDenied />} />
              </Route>

              {/* Catch all */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Router>
        </AuthProvider>
  );
};

const ThemedApp: React.FC = () => {
  const { muiTheme } = useOrgTheme();
  return (
    <MuiThemeProvider theme={muiTheme}>
      <CssBaseline />
      <SnackbarProvider
        maxSnack={3}
        autoHideDuration={2800}
        preventDuplicate
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        action={(snackbarId) => (
          <IconButton
            size="small"
            aria-label="Dismiss notification"
            color="inherit"
            onClick={() => closeSnackbar(snackbarId)}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        )}
      >
        <ModuleConfigProvider>
          <ErrorBoundary>
            <AppContent />
          </ErrorBoundary>
        </ModuleConfigProvider>
      </SnackbarProvider>
    </MuiThemeProvider>
  );
};

const App: React.FC = () => {
  return (
    <OrgThemeProvider>
      <ThemedApp />
    </OrgThemeProvider>
  );
};

export default App;

