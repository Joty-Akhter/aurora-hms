/**
 * Currency formatting utility for Aurora HMS
 * Default locale: bd-BD (Bangladesh)
 * Default currency: BDT (Bangladeshi Taka)
 */

export interface CurrencyConfig {
  locale: string;
  currency: string;
  symbol?: string;
}

// Default configuration for Bangladesh
const DEFAULT_CONFIG: CurrencyConfig = {
  locale: 'bd-BD',
  currency: 'BDT',
  symbol: '৳'
};

/**
 * Format a number as currency based on organization settings
 * @param amount - The numeric amount to format
 * @param config - Optional currency configuration (defaults to BDT)
 * @returns Formatted currency string
 */
export const formatCurrency = (
  amount: number | null | undefined,
  config: Partial<CurrencyConfig> = {}
): string => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '-';
  }

  const { locale, symbol } = { ...DEFAULT_CONFIG, ...config };

  try {
    // Use Intl.NumberFormat for proper locale-aware formatting
    const formatter = new Intl.NumberFormat(locale, {
      style: 'decimal',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });

    const formattedNumber = formatter.format(Math.abs(amount));
    const sign = amount < 0 ? '-' : '';
    
    // For Bangladesh, symbol comes before the number
    return `${sign}${symbol}${formattedNumber}`;
  } catch (error) {
    // Fallback to simple formatting if locale is not supported
    const formattedNumber = Math.abs(amount).toFixed(2);
    const sign = amount < 0 ? '-' : '';
    return `${sign}${symbol}${formattedNumber}`;
  }
};

/**
 * Parse a currency string back to a number
 * @param currencyString - The currency string to parse
 * @returns Numeric value
 */
export const parseCurrency = (currencyString: string): number => {
  // Remove currency symbols, commas, and whitespace
  const cleaned = currencyString.replace(/[৳$,\s]/g, '');
  return parseFloat(cleaned) || 0;
};

/**
 * Format amount for input fields (no symbol, just number)
 */
export const formatAmountForInput = (amount: number | null | undefined): string => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '';
  }
  return amount.toFixed(2);
};

/**
 * Get currency symbol for display
 */
export const getCurrencySymbol = (config: Partial<CurrencyConfig> = {}): string => {
  const { symbol } = { ...DEFAULT_CONFIG, ...config };
  return symbol || '৳';
};

export default {
  formatCurrency,
  parseCurrency,
  formatAmountForInput,
  getCurrencySymbol
};

