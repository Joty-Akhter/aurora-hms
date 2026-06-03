/**
 * Excel Export Utility
 * 
 * Provides functions to export data to Excel format.
 * Uses the browser's native capabilities to create and download Excel files.
 */

export interface ExcelColumn {
  header: string;
  key: string;
  width?: number;
  format?: 'text' | 'number' | 'currency' | 'date' | 'percentage';
}

export interface ExcelSheet {
  name: string;
  columns: ExcelColumn[];
  data: any[];
}

/**
 * Export data to Excel (CSV format)
 */
export const exportToExcel = (
  sheets: ExcelSheet[],
  filename: string = 'export'
) => {
  // For now, we'll export as CSV (simple and widely compatible)
  // In the future, we can use a library like SheetJS for true XLSX format
  
  if (sheets.length === 0) {
    console.error('No sheets to export');
    return;
  }

  // Export first sheet as CSV
  const sheet = sheets[0];
  const csv = convertToCSV(sheet);
  downloadFile(csv, `${filename}.csv`, 'text/csv');
};

/**
 * Export multiple sheets to separate CSV files
 */
export const exportMultipleSheets = (
  sheets: ExcelSheet[],
  filenamePrefix: string = 'export'
) => {
  sheets.forEach((sheet) => {
    const csv = convertToCSV(sheet);
    downloadFile(csv, `${filenamePrefix}_${sheet.name}.csv`, 'text/csv');
  });
};

/**
 * Convert sheet data to CSV format
 */
const convertToCSV = (sheet: ExcelSheet): string => {
  const { columns, data } = sheet;
  
  // Create header row
  const headers = columns.map(col => col.header).join(',');
  
  // Create data rows
  const rows = data.map(row => {
    return columns.map(col => {
      const value = row[col.key];
      return formatCellValue(value, col.format);
    }).join(',');
  });
  
  return [headers, ...rows].join('\n');
};

/**
 * Format cell value based on format type
 */
const formatCellValue = (value: any, format?: string): string => {
  if (value === null || value === undefined) {
    return '';
  }
  
  switch (format) {
    case 'currency':
      return `"$${Number(value).toFixed(2)}"`;
    case 'number':
      return String(value);
    case 'percentage':
      return `"${Number(value).toFixed(2)}%"`;
    case 'date':
      return `"${value}"`;
    case 'text':
    default:
      // Escape quotes and wrap in quotes if contains comma
      const stringValue = String(value);
      if (stringValue.includes(',') || stringValue.includes('"') || stringValue.includes('\n')) {
        return `"${stringValue.replace(/"/g, '""')}"`;
      }
      return stringValue;
  }
};

/**
 * Download file to user's computer
 */
const downloadFile = (content: string, filename: string, mimeType: string) => {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * Export Trial Balance to Excel
 */
export const exportTrialBalanceToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'Trial Balance',
    columns: [
      { header: 'Account Code', key: 'accountCode', width: 15 },
      { header: 'Account Name', key: 'accountName', width: 30 },
      { header: 'Account Type', key: 'accountType', width: 20 },
      { header: 'Debit', key: 'debit', format: 'currency', width: 15 },
      { header: 'Credit', key: 'credit', format: 'currency', width: 15 },
    ],
    data
  };
  
  exportToExcel([sheet], 'Trial_Balance');
};

/**
 * Export General Ledger to Excel
 */
export const exportGeneralLedgerToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'General Ledger',
    columns: [
      { header: 'Date', key: 'date', format: 'date', width: 12 },
      { header: 'Account Code', key: 'accountCode', width: 15 },
      { header: 'Account Name', key: 'accountName', width: 30 },
      { header: 'Reference', key: 'reference', width: 15 },
      { header: 'Description', key: 'description', width: 30 },
      { header: 'Debit', key: 'debit', format: 'currency', width: 15 },
      { header: 'Credit', key: 'credit', format: 'currency', width: 15 },
      { header: 'Balance', key: 'balance', format: 'currency', width: 15 },
    ],
    data
  };
  
  exportToExcel([sheet], 'General_Ledger');
};

/**
 * Export Profit & Loss to Excel
 */
export const exportProfitLossToExcel = (data: any) => {
  const revenue = data.revenue?.map((item: any) => ({
    category: 'Revenue',
    accountName: item.accountName,
    amount: item.amount
  })) || [];
  
  const expenses = data.expenses?.map((item: any) => ({
    category: 'Expenses',
    accountName: item.accountName,
    amount: item.amount
  })) || [];
  
  const allData = [
    ...revenue,
    { category: '', accountName: 'Total Revenue', amount: data.totalRevenue },
    { category: '', accountName: '', amount: '' },
    ...expenses,
    { category: '', accountName: 'Total Expenses', amount: data.totalExpenses },
    { category: '', accountName: '', amount: '' },
    { category: '', accountName: 'Net Income', amount: data.netIncome },
  ];
  
  const sheet: ExcelSheet = {
    name: 'Profit & Loss',
    columns: [
      { header: 'Category', key: 'category', width: 20 },
      { header: 'Account', key: 'accountName', width: 30 },
      { header: 'Amount', key: 'amount', format: 'currency', width: 15 },
    ],
    data: allData
  };
  
  exportToExcel([sheet], 'Profit_Loss');
};

/**
 * Export Balance Sheet to Excel
 */
export const exportBalanceSheetToExcel = (data: any) => {
  const assets = data.assets?.map((item: any) => ({
    category: 'Assets',
    accountName: item.accountName,
    amount: item.amount
  })) || [];
  
  const liabilities = data.liabilities?.map((item: any) => ({
    category: 'Liabilities',
    accountName: item.accountName,
    amount: item.amount
  })) || [];
  
  const equity = data.equity?.map((item: any) => ({
    category: 'Equity',
    accountName: item.accountName,
    amount: item.amount
  })) || [];
  
  const allData = [
    ...assets,
    { category: '', accountName: 'Total Assets', amount: data.totalAssets },
    { category: '', accountName: '', amount: '' },
    ...liabilities,
    { category: '', accountName: 'Total Liabilities', amount: data.totalLiabilities },
    { category: '', accountName: '', amount: '' },
    ...equity,
    { category: '', accountName: 'Total Equity', amount: data.totalEquity },
  ];
  
  const sheet: ExcelSheet = {
    name: 'Balance Sheet',
    columns: [
      { header: 'Category', key: 'category', width: 20 },
      { header: 'Account', key: 'accountName', width: 30 },
      { header: 'Amount', key: 'amount', format: 'currency', width: 15 },
    ],
    data: allData
  };
  
  exportToExcel([sheet], 'Balance_Sheet');
};

/**
 * Export Cash Flow to Excel
 */
export const exportCashFlowToExcel = (data: any) => {
  const operating = data.operatingActivities?.map((item: any) => ({
    category: 'Operating Activities',
    description: item.description,
    amount: item.amount
  })) || [];
  
  const investing = data.investingActivities?.map((item: any) => ({
    category: 'Investing Activities',
    description: item.description,
    amount: item.amount
  })) || [];
  
  const financing = data.financingActivities?.map((item: any) => ({
    category: 'Financing Activities',
    description: item.description,
    amount: item.amount
  })) || [];
  
  const allData = [
    ...operating,
    { category: '', description: 'Net Operating Cash Flow', amount: data.netOperatingCashFlow },
    { category: '', description: '', amount: '' },
    ...investing,
    { category: '', description: 'Net Investing Cash Flow', amount: data.netInvestingCashFlow },
    { category: '', description: '', amount: '' },
    ...financing,
    { category: '', description: 'Net Financing Cash Flow', amount: data.netFinancingCashFlow },
    { category: '', description: '', amount: '' },
    { category: '', description: 'Net Change in Cash', amount: data.netChangeInCash },
  ];
  
  const sheet: ExcelSheet = {
    name: 'Cash Flow',
    columns: [
      { header: 'Category', key: 'category', width: 25 },
      { header: 'Description', key: 'description', width: 30 },
      { header: 'Amount', key: 'amount', format: 'currency', width: 15 },
    ],
    data: allData
  };
  
  exportToExcel([sheet], 'Cash_Flow');
};

/**
 * Export AR Aging Report to Excel
 */
export const exportARAgingToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'AR Aging',
    columns: [
      { header: 'Customer Name', key: 'customerName', width: 30 },
      { header: 'Invoice #', key: 'invoiceNumber', width: 15 },
      { header: 'Due Date', key: 'dueDate', format: 'date', width: 12 },
      { header: 'Days Overdue', key: 'daysOverdue', format: 'number', width: 12 },
      { header: 'Current', key: 'current', format: 'currency', width: 15 },
      { header: '1-30 Days', key: 'days1to30', format: 'currency', width: 15 },
      { header: '31-60 Days', key: 'days31to60', format: 'currency', width: 15 },
      { header: '61-90 Days', key: 'days61to90', format: 'currency', width: 15 },
      { header: '90+ Days', key: 'over90', format: 'currency', width: 15 },
      { header: 'Balance Due', key: 'total', format: 'currency', width: 15 },
    ],
    data
  };
  
  exportToExcel([sheet], 'AR_Aging_Report');
};

/**
 * Export AP Aging Report to Excel
 */
export const exportAPAgingToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'AP Aging',
    columns: [
      { header: 'Vendor Name', key: 'vendorName', width: 30 },
      { header: 'Bill #', key: 'billNumber', width: 15 },
      { header: 'Due Date', key: 'dueDate', format: 'date', width: 12 },
      { header: 'Days Overdue', key: 'daysOverdue', format: 'number', width: 12 },
      { header: 'Current', key: 'current', format: 'currency', width: 15 },
      { header: '1-30 Days', key: 'days1to30', format: 'currency', width: 15 },
      { header: '31-60 Days', key: 'days31to60', format: 'currency', width: 15 },
      { header: '61-90 Days', key: 'days61to90', format: 'currency', width: 15 },
      { header: '90+ Days', key: 'over90', format: 'currency', width: 15 },
      { header: 'Balance Due', key: 'total', format: 'currency', width: 15 },
    ],
    data
  };
  
  exportToExcel([sheet], 'AP_Aging_Report');
};

/**
 * Export Invoices to Excel
 */
export const exportInvoicesToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'Invoices',
    columns: [
      { header: 'Invoice #', key: 'invoiceNumber', width: 15 },
      { header: 'Date', key: 'invoiceDate', format: 'date', width: 12 },
      { header: 'Customer', key: 'customerName', width: 25 },
      { header: 'Due Date', key: 'dueDate', format: 'date', width: 12 },
      { header: 'Total Amount', key: 'totalAmount', format: 'currency', width: 15 },
      { header: 'Balance Due', key: 'balanceDue', format: 'currency', width: 15 },
      { header: 'Status', key: 'status', width: 12 },
    ],
    data
  };
  
  exportToExcel([sheet], 'Invoices');
};

/**
 * Export Bills to Excel
 */
export const exportBillsToExcel = (data: any[]) => {
  const sheet: ExcelSheet = {
    name: 'Bills',
    columns: [
      { header: 'Bill #', key: 'billNumber', width: 15 },
      { header: 'Date', key: 'billDate', format: 'date', width: 12 },
      { header: 'Vendor', key: 'vendorName', width: 25 },
      { header: 'Due Date', key: 'dueDate', format: 'date', width: 12 },
      { header: 'Total Amount', key: 'totalAmount', format: 'currency', width: 15 },
      { header: 'Balance Due', key: 'balanceDue', format: 'currency', width: 15 },
      { header: 'Status', key: 'status', width: 12 },
    ],
    data
  };
  
  exportToExcel([sheet], 'Bills');
};

