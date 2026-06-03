import api from './api';
import {
  ChartOfAccount,
  CoARequest,
  JournalEntry,
  JournalEntryRequest,
  JournalLine,
  TrialBalance,
  Period,
  FiscalYear
} from '../types/accounting';

const accountingService = {
  // Chart of Accounts APIs
  async getAccounts(organizationId: string): Promise<ChartOfAccount[]> {
    const response = await api.get(`/api/accounting/coa/organization/${organizationId}`);
    return response.data;
  },

  async getActiveAccounts(organizationId: string): Promise<ChartOfAccount[]> {
    const response = await api.get(`/api/accounting/coa/organization/${organizationId}/active`);
    return response.data;
  },

  async getPostingAccounts(organizationId: string): Promise<ChartOfAccount[]> {
    const response = await api.get(`/api/accounting/coa/organization/${organizationId}/posting`);
    return response.data;
  },

  async getAccountsByType(organizationId: string, accountType: string): Promise<ChartOfAccount[]> {
    const response = await api.get(`/api/accounting/coa/organization/${organizationId}/type/${accountType}`);
    return response.data;
  },

  async getAccount(accountId: string): Promise<ChartOfAccount> {
    const response = await api.get(`/api/accounting/coa/${accountId}`);
    return response.data;
  },

  async createAccount(data: CoARequest): Promise<ChartOfAccount> {
    const response = await api.post('/api/accounting/coa', data);
    return response.data;
  },

  async updateAccount(accountId: string, data: CoARequest): Promise<ChartOfAccount> {
    const response = await api.put(`/api/accounting/coa/${accountId}`, data);
    return response.data;
  },

  async deactivateAccount(accountId: string, organizationId: string): Promise<void> {
    await api.delete(`/api/accounting/coa/${accountId}?organizationId=${organizationId}`);
  },

  async loadStandardCOA(organizationId: string): Promise<string> {
    const response = await api.post(`/api/accounting/coa/organization/${organizationId}/load-standard-coa`);
    return response.data;
  },

  // Financial Reports APIs
  async getGeneralLedger(organizationId: string, accountId: string, startDate: string, endDate: string): Promise<any[]> {
    const response = await api.get('/api/accounting/reports/general-ledger', {
      params: { organizationId, accountId, startDate, endDate }
    });
    return response.data;
  },

  async getProfitAndLoss(organizationId: string, periodId: string): Promise<any> {
    const response = await api.get('/api/accounting/reports/profit-loss', {
      params: { organizationId, periodId }
    });
    return response.data;
  },

  async getBalanceSheet(organizationId: string, asOfDate: string): Promise<any> {
    const response = await api.get('/api/accounting/reports/balance-sheet', {
      params: { organizationId, asOfDate }
    });
    return response.data;
  },

  async getCashFlow(organizationId: string, periodId: string): Promise<any> {
    const response = await api.get('/api/accounting/reports/cash-flow', {
      params: { organizationId, periodId }
    });
    return response.data;
  },

  // Journal Entry APIs
  async getJournalEntries(organizationId: string, page = 0, size = 20): Promise<any> {
    const response = await api.get(`/api/accounting/journals/organization/${organizationId}`, {
      params: { page, size }
    });
    return response.data;
  },

  async getJournalEntry(journalId: string): Promise<JournalEntry> {
    const response = await api.get(`/api/accounting/journals/${journalId}`);
    return response.data;
  },

  async getJournalLines(journalId: string): Promise<JournalLine[]> {
    const response = await api.get(`/api/accounting/journals/${journalId}/lines`);
    return response.data;
  },

  async createJournalEntry(data: JournalEntryRequest): Promise<JournalEntry> {
    const response = await api.post('/api/accounting/journals', data);
    return response.data;
  },

  async postJournalEntry(journalId: string): Promise<JournalEntry> {
    const response = await api.post(`/api/accounting/journals/${journalId}/post`);
    return response.data;
  },

  async reverseJournalEntry(journalId: string): Promise<JournalEntry> {
    const response = await api.post(`/api/accounting/journals/${journalId}/reverse`);
    return response.data;
  },

  // Reports APIs
  async getTrialBalance(organizationId: string, periodId: string): Promise<TrialBalance[]> {
    const response = await api.get('/api/accounting/reports/trial-balance', {
      params: { organizationId, periodId }
    });
    return response.data;
  },

  // Fiscal Year & Period APIs
  async getFiscalYears(organizationId: string): Promise<FiscalYear[]> {
    const response = await api.get(`/api/accounting/fiscal-years/organization/${organizationId}`);
    return response.data;
  },

  async getOpenFiscalYears(organizationId: string): Promise<FiscalYear[]> {
    const response = await api.get(`/api/accounting/fiscal-years/organization/${organizationId}/open`);
    return response.data;
  },

  async getPeriods(organizationId: string): Promise<Period[]> {
    const response = await api.get(`/api/accounting/fiscal-years/organization/${organizationId}/periods`);
    return response.data;
  },

  async getOpenPeriods(organizationId: string): Promise<Period[]> {
    const response = await api.get(`/api/accounting/fiscal-years/organization/${organizationId}/periods/open`);
    return response.data;
  },

  async setupCurrentFiscalYear(organizationId: string): Promise<FiscalYear> {
    const response = await api.post(`/api/accounting/fiscal-years/organization/${organizationId}/setup-current-year`);
    return response.data;
  },

  // Bank Service APIs
  async getBankAccounts(organizationId: string): Promise<any[]> {
    const response = await api.get('/api/bank/accounts', {
      params: { organizationId, activeOnly: true }
    });
    return response.data;
  },

  async getBankTransactions(accountId: string, startDate?: string, endDate?: string): Promise<any[]> {
    const params: any = { accountId };
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    
    const response = await api.get('/api/bank/transactions', { params });
    return response.data;
  },

  async getUnreconciledTransactions(accountId: string): Promise<any[]> {
    const response = await api.get('/api/bank/transactions/unreconciled', {
      params: { accountId }
    });
    return response.data;
  },

  async createReconciliation(data: any): Promise<any> {
    const response = await api.post('/api/bank/reconciliations', data);
    return response.data;
  },

  // AR Service APIs
  async getARCustomers(organizationId: string, activeOnly: boolean = true): Promise<any[]> {
    const response = await api.get('/api/ar/customers', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getARCustomer(customerId: string): Promise<any> {
    const response = await api.get(`/api/ar/customers/${customerId}`);
    return response.data;
  },

  async createARCustomer(data: any): Promise<any> {
    const response = await api.post('/api/ar/customers', data);
    return response.data;
  },

  async updateARCustomer(customerId: string, data: any): Promise<any> {
    const response = await api.put(`/api/ar/customers/${customerId}`, data);
    return response.data;
  },

  async deleteARCustomer(customerId: string): Promise<void> {
    await api.delete(`/api/ar/customers/${customerId}`);
  },

  async getOverdueInvoices(organizationId: string): Promise<any[]> {
    const response = await api.get('/api/ar/invoices/overdue', {
      params: { organizationId }
    });
    return response.data;
  },

  async getOverdueBills(organizationId: string): Promise<any[]> {
    const response = await api.get('/api/ap/bills/overdue', {
      params: { organizationId }
    });
    return response.data;
  },

  async createARInvoice(data: any): Promise<any> {
    const response = await api.post('/api/ar/invoices', data);
    return response.data;
  },

  async postARInvoice(invoiceId: string): Promise<any> {
    const response = await api.post(`/api/ar/invoices/${invoiceId}/post`);
    return response.data;
  },

  async getARAgingReport(organizationId: string, asOfDate?: string): Promise<any[]> {
    const params: any = { organizationId };
    if (asOfDate) params.asOfDate = asOfDate;
    
    const response = await api.get('/api/ar/aging', { params });
    return response.data;
  },

  async getARInvoices(organizationId: string, status?: string): Promise<any[]> {
    const params: any = { organizationId };
    if (status) params.status = status;
    
    const response = await api.get('/api/ar/invoices', { params });
    return response.data;
  },

  async getOutstandingInvoices(organizationId: string): Promise<any[]> {
    const response = await api.get('/api/ar/invoices/outstanding', {
      params: { organizationId }
    });
    return response.data;
  },

  async createARReceipt(data: any): Promise<any> {
    const response = await api.post('/api/ar/receipts', data);
    return response.data;
  },

  async postARReceipt(receiptId: string): Promise<any> {
    const response = await api.post(`/api/ar/receipts/${receiptId}/post`);
    return response.data;
  },

  async getARCreditNotes(organizationId: string, status?: string): Promise<any[]> {
    const params: any = { organizationId };
    if (status) params.status = status;
    
    const response = await api.get('/api/ar/credit-notes', { params });
    return response.data;
  },

  async createARCreditNote(data: any): Promise<any> {
    const response = await api.post('/api/ar/credit-notes', data);
    return response.data;
  },

  async postARCreditNote(creditNoteId: string): Promise<any> {
    const response = await api.post(`/api/ar/credit-notes/${creditNoteId}/post`);
    return response.data;
  },

  // AP Service APIs
  async getAPBills(organizationId: string, status?: string): Promise<any[]> {
    const params: any = { organizationId };
    if (status) params.status = status;
    
    const response = await api.get('/api/ap/bills', { params });
    return response.data;
  },

  async getOutstandingBills(organizationId: string): Promise<any[]> {
    const response = await api.get('/api/ap/bills/outstanding', {
      params: { organizationId }
    });
    return response.data;
  },

  async getAPVendors(organizationId: string, activeOnly: boolean = true): Promise<any[]> {
    const response = await api.get('/api/ap/vendors', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getVendorById(vendorId: string): Promise<any> {
    const response = await api.get(`/api/ap/vendors/${vendorId}`);
    return response.data;
  },

  async createVendor(data: any): Promise<any> {
    const response = await api.post('/api/ap/vendors', data);
    return response.data;
  },

  async updateVendor(vendorId: string, data: any): Promise<any> {
    const response = await api.put(`/api/ap/vendors/${vendorId}`, data);
    return response.data;
  },

  async deleteVendor(vendorId: string): Promise<void> {
    await api.delete(`/api/ap/vendors/${vendorId}`);
  },

  async createAPBill(data: any): Promise<any> {
    const response = await api.post('/api/ap/bills', data);
    return response.data;
  },

  async postAPBill(billId: string): Promise<any> {
    const response = await api.post(`/api/ap/bills/${billId}/post`);
    return response.data;
  },

  async createAPPayment(data: any): Promise<any> {
    const response = await api.post('/api/ap/payments', data);
    return response.data;
  },

  async postAPPayment(paymentId: string): Promise<any> {
    const response = await api.post(`/api/ap/payments/${paymentId}/post`);
    return response.data;
  },

  async getAPAgingReport(organizationId: string, asOfDate?: string): Promise<any[]> {
    const params: any = { organizationId };
    if (asOfDate) params.asOfDate = asOfDate;
    
    const response = await api.get('/api/ap/aging', { params });
    return response.data;
  },

  // Bank Service APIs (create/other - getBank* and getUnreconciledTransactions are above)
  async createBankAccount(data: any): Promise<any> {
    const response = await api.post('/api/bank/accounts', data);
    return response.data;
  },

  async updateBankAccount(accountId: string, data: any): Promise<any> {
    const response = await api.put(`/api/bank/accounts/${accountId}`, data);
    return response.data;
  },

  async deleteBankAccount(accountId: string): Promise<void> {
    await api.delete(`/api/bank/accounts/${accountId}`);
  },

  async deleteBankTransaction(transactionId: string): Promise<void> {
    await api.delete(`/api/bank/transactions/${transactionId}`);
  },

  async createBankTransaction(data: any): Promise<any> {
    const response = await api.post('/api/bank/transactions', data);
    return response.data;
  },

  async getBankReconciliations(accountId: string): Promise<any[]> {
    const response = await api.get('/api/bank/reconciliations', {
      params: { accountId },
    });
    return response.data;
  },

  async createBankReconciliation(data: any): Promise<any> {
    const response = await api.post('/api/bank/reconciliations', data);
    return response.data;
  },

  async completeBankReconciliation(reconciliationId: string, userId: string): Promise<any> {
    const response = await api.post(`/api/bank/reconciliations/${reconciliationId}/complete`, null, {
      params: { userId },
    });
    return response.data;
  },

  // Statement APIs
  async getCustomerStatement(customerId: string, startDate: string, endDate: string): Promise<any> {
    const response = await api.get(`/api/ar/statements/customer/${customerId}`, {
      params: { startDate, endDate }
    });
    return response.data;
  },

  async getVendorStatement(vendorId: string, startDate: string, endDate: string): Promise<any> {
    const response = await api.get(`/api/ap/statements/vendor/${vendorId}`, {
      params: { startDate, endDate }
    });
    return response.data;
  },

  // Payment Reminder APIs
  async getReminderConfig(organizationId: string): Promise<any> {
    const response = await api.get('/api/ar/reminders/config', {
      params: { organizationId }
    });
    return response.data;
  },

  async saveReminderConfig(config: any): Promise<any> {
    const response = await api.post('/api/ar/reminders/config', config);
    return response.data;
  },

  async sendRemindersNow(organizationId: string): Promise<any> {
    const response = await api.post('/api/ar/reminders/send-now', null, {
      params: { organizationId }
    });
    return response.data;
  },

  async getReminderHistory(organizationId: string, days: number = 30): Promise<any[]> {
    const response = await api.get('/api/ar/reminders/history', {
      params: { organizationId, days }
    });
    return response.data;
  },

  async emailCustomerStatement(customerId: string, startDate: string, endDate: string): Promise<any> {
    const response = await api.post(`/api/ar/statements/customer/${customerId}/email`, null, {
      params: { startDate, endDate }
    });
    return response.data;
  },

  async emailVendorStatement(vendorId: string, startDate: string, endDate: string): Promise<any> {
    const response = await api.post(`/api/ap/statements/vendor/${vendorId}/email`, null, {
      params: { startDate, endDate }
    });
    return response.data;
  },

  // Dashboard APIs
  async getDashboardSummary(organizationId: string): Promise<any> {
    const response = await api.get('/api/accounting/dashboard/summary', {
      params: { organizationId }
    });
    return response.data;
  },
};

export default accountingService;

