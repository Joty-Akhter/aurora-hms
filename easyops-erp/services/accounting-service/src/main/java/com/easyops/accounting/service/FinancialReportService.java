package com.easyops.accounting.service;

import com.easyops.accounting.dto.*;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportService {
    
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountsRepository coaRepository;
    private final PeriodRepository periodRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    
    /**
     * Generate General Ledger for a specific account
     */
    public List<GeneralLedgerResponse> getGeneralLedger(UUID organizationId, UUID accountId, 
                                                        LocalDate startDate, LocalDate endDate) {
        log.info("Generating general ledger for account: {} from {} to {}", accountId, startDate, endDate);
        
        ChartOfAccounts account = coaRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
            
        // Get all journal lines for this account within the date range
        List<JournalLine> lines = journalLineRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        
        List<GeneralLedgerResponse> ledgerEntries = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        
        for (JournalLine line : lines) {
            JournalEntry entry = line.getJournalEntry();
                
            if (entry == null || !"POSTED".equals(entry.getStatus())) {
                continue; // Skip draft or invalid entries
            }
            
            // Calculate running balance
            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
            
            // For assets and expenses, debit increases balance
            // For liabilities, equity, and revenue, credit increases balance
            if (account.getAccountType().equals("ASSET") || account.getAccountType().equals("EXPENSE")) {
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                runningBalance = runningBalance.add(credit).subtract(debit);
            }
            
            ledgerEntries.add(GeneralLedgerResponse.builder()
                .transactionDate(entry.getJournalDate())
                .journalNumber(entry.getJournalNumber())
                .description(line.getDescription() != null ? line.getDescription() : entry.getDescription())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .debit(debit)
                .credit(credit)
                .runningBalance(runningBalance)
                .journalEntryId(entry.getId())
                .journalLineId(line.getId())
                .status(entry.getStatus())
                .reference(entry.getReferenceNumber())
                .build());
        }
        
        return ledgerEntries;
    }
    
    /**
     * Generate Profit & Loss Statement
     */
    public ProfitLossResponse getProfitAndLoss(UUID organizationId, UUID periodId) {
        log.info("Generating Profit & Loss for organization: {}, period: {}", organizationId, periodId);
        
        Period period = requirePeriodForOrganization(organizationId, periodId);
        
        // Get all accounts for this organization
        List<ChartOfAccounts> allAccounts = coaRepository.findByOrganizationIdOrderByAccountCode(organizationId);
        
        // Filter accounts by type
        List<ChartOfAccounts> revenueAccounts = allAccounts.stream()
            .filter(a -> "REVENUE".equals(a.getAccountType()) && !a.getIsGroup())
            .collect(Collectors.toList());
            
        List<ChartOfAccounts> expenseAccounts = allAccounts.stream()
            .filter(a -> "EXPENSE".equals(a.getAccountType()) && !a.getIsGroup())
            .collect(Collectors.toList());
        
        // Calculate balances
        List<ProfitLossResponse.AccountLineItem> revenueItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : revenueAccounts) {
            BigDecimal balance = getPeriodActivity(account, period.getStartDate(), period.getEndDate());
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                revenueItems.add(ProfitLossResponse.AccountLineItem.builder()
                    .accountCode(account.getAccountCode())
                    .accountName(account.getAccountName())
                    .amount(balance)
                    .accountCategory(account.getAccountCategory())
                    .build());
                totalRevenue = totalRevenue.add(balance);
            }
        }
        
        // Separate COGS and Operating Expenses
        List<ProfitLossResponse.AccountLineItem> cogsItems = new ArrayList<>();
        List<ProfitLossResponse.AccountLineItem> opexItems = new ArrayList<>();
        BigDecimal totalCOGS = BigDecimal.ZERO;
        BigDecimal totalOpex = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : expenseAccounts) {
            BigDecimal balance = getPeriodActivity(account, period.getStartDate(), period.getEndDate());
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                ProfitLossResponse.AccountLineItem item = ProfitLossResponse.AccountLineItem.builder()
                    .accountCode(account.getAccountCode())
                    .accountName(account.getAccountName())
                    .amount(balance)
                    .accountCategory(account.getAccountCategory())
                    .build();
                    
                if (account.getAccountCategory() != null && 
                    account.getAccountCategory().contains("Cost of")) {
                    cogsItems.add(item);
                    totalCOGS = totalCOGS.add(balance);
                } else {
                    opexItems.add(item);
                    totalOpex = totalOpex.add(balance);
                }
            }
        }
        
        BigDecimal grossProfit = totalRevenue.subtract(totalCOGS);
        BigDecimal operatingIncome = grossProfit.subtract(totalOpex);
        BigDecimal netIncome = operatingIncome;
        
        BigDecimal grossProfitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
            ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) 
            : BigDecimal.ZERO;
            
        BigDecimal netProfitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
            ? netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) 
            : BigDecimal.ZERO;
        
        return ProfitLossResponse.builder()
            .organizationName("Organization") // Could fetch from org service
            .periodName(period.getPeriodName())
            .startDate(period.getStartDate().toString())
            .endDate(period.getEndDate().toString())
            .revenueAccounts(revenueItems)
            .totalRevenue(totalRevenue)
            .cogsAccounts(cogsItems)
            .totalCOGS(totalCOGS)
            .grossProfit(grossProfit)
            .grossProfitMargin(grossProfitMargin)
            .operatingExpenses(opexItems)
            .totalOperatingExpenses(totalOpex)
            .operatingIncome(operatingIncome)
            .netIncome(netIncome)
            .netProfitMargin(netProfitMargin)
            .build();
    }
    
    /**
     * Generate Balance Sheet
     */
    public BalanceSheetResponse getBalanceSheet(UUID organizationId, LocalDate asOfDate) {
        log.info("Generating Balance Sheet for organization: {} as of {}", organizationId, asOfDate);
        
        List<ChartOfAccounts> allAccounts = coaRepository.findByOrganizationIdOrderByAccountCode(organizationId);
        
        // Assets
        List<BalanceSheetResponse.AccountLineItem> currentAssets = new ArrayList<>();
        List<BalanceSheetResponse.AccountLineItem> fixedAssets = new ArrayList<>();
        BigDecimal totalCurrentAssets = BigDecimal.ZERO;
        BigDecimal totalFixedAssets = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : allAccounts) {
            if ("ASSET".equals(account.getAccountType()) && !account.getIsGroup()) {
                BigDecimal balance = calculateAccountBalanceAsOf(account.getId(), asOfDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    BalanceSheetResponse.AccountLineItem item = BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(balance)
                        .accountCategory(account.getAccountCategory())
                        .build();
                        
                    if (account.getAccountCategory() != null && account.getAccountCategory().contains("Current")) {
                        currentAssets.add(item);
                        totalCurrentAssets = totalCurrentAssets.add(balance);
                    } else {
                        fixedAssets.add(item);
                        totalFixedAssets = totalFixedAssets.add(balance);
                    }
                }
            }
        }
        
        // Liabilities
        List<BalanceSheetResponse.AccountLineItem> currentLiabilities = new ArrayList<>();
        List<BalanceSheetResponse.AccountLineItem> longTermLiabilities = new ArrayList<>();
        BigDecimal totalCurrentLiabilities = BigDecimal.ZERO;
        BigDecimal totalLongTermLiabilities = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : allAccounts) {
            if ("LIABILITY".equals(account.getAccountType()) && !account.getIsGroup()) {
                BigDecimal balance = calculateAccountBalanceAsOf(account.getId(), asOfDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    BalanceSheetResponse.AccountLineItem item = BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(balance)
                        .accountCategory(account.getAccountCategory())
                        .build();
                        
                    if (account.getAccountCategory() != null && account.getAccountCategory().contains("Current")) {
                        currentLiabilities.add(item);
                        totalCurrentLiabilities = totalCurrentLiabilities.add(balance);
                    } else {
                        longTermLiabilities.add(item);
                        totalLongTermLiabilities = totalLongTermLiabilities.add(balance);
                    }
                }
            }
        }
        
        // Equity
        List<BalanceSheetResponse.AccountLineItem> equityAccounts = new ArrayList<>();
        BigDecimal totalEquity = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : allAccounts) {
            if ("EQUITY".equals(account.getAccountType()) && !account.getIsGroup()) {
                BigDecimal balance = calculateAccountBalanceAsOf(account.getId(), asOfDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    equityAccounts.add(BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(balance)
                        .accountCategory(account.getAccountCategory())
                        .build());
                    totalEquity = totalEquity.add(balance);
                }
            }
        }
        
        BigDecimal totalAssets = totalCurrentAssets.add(totalFixedAssets);
        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalLongTermLiabilities);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        boolean balanced = totalAssets.compareTo(totalLiabilitiesAndEquity) == 0;
        
        return BalanceSheetResponse.builder()
            .organizationName("Organization")
            .asOfDate(asOfDate.toString())
            .currentAssets(currentAssets)
            .totalCurrentAssets(totalCurrentAssets)
            .fixedAssets(fixedAssets)
            .totalFixedAssets(totalFixedAssets)
            .totalAssets(totalAssets)
            .currentLiabilities(currentLiabilities)
            .totalCurrentLiabilities(totalCurrentLiabilities)
            .longTermLiabilities(longTermLiabilities)
            .totalLongTermLiabilities(totalLongTermLiabilities)
            .totalLiabilities(totalLiabilities)
            .equityAccounts(equityAccounts)
            .totalEquity(totalEquity)
            .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
            .balanced(balanced)
            .build();
    }
    
    /**
     * Generate Cash Flow Statement (indirect method).
     */
    public CashFlowResponse getCashFlow(UUID organizationId, UUID periodId) {
        log.info("Generating Cash Flow for organization: {}, period: {}", organizationId, periodId);
        
        Period period = requirePeriodForOrganization(organizationId, periodId);
        
        LocalDate periodStart = period.getStartDate();
        LocalDate periodEnd = period.getEndDate();
        LocalDate dayBeforePeriod = periodStart.minusDays(1);
        
        List<ChartOfAccounts> allAccounts = coaRepository.findByOrganizationIdOrderByAccountCode(organizationId);
        BigDecimal netIncome = computeNetIncome(allAccounts, periodStart, periodEnd);
        
        List<CashFlowResponse.CashFlowLineItem> operatingAdjustments = new ArrayList<>();
        BigDecimal netOperatingAdjustments = BigDecimal.ZERO;
        
        // Non-cash add-backs (depreciation expense; skip accumulated depreciation contra accounts)
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup()) || isAccumulatedDepreciation(account)) {
                continue;
            }
            if ("EXPENSE".equals(account.getAccountType())
                    && account.getAccountName() != null
                    && account.getAccountName().toLowerCase().contains("depreciation")) {
                BigDecimal depreciation = getPeriodActivity(account, periodStart, periodEnd);
                if (depreciation.compareTo(BigDecimal.ZERO) != 0) {
                    operatingAdjustments.add(buildCashFlowLineItem(
                        "Depreciation and amortization", depreciation, account.getAccountCode()));
                    netOperatingAdjustments = netOperatingAdjustments.add(depreciation);
                }
            }
        }
        
        // Working capital: current assets (excluding cash and contra assets)
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup()) || !isCurrentAsset(account)
                    || isCashAccount(account) || isContraAsset(account)) {
                continue;
            }
            BigDecimal change = calculateBalanceChange(account.getId(), dayBeforePeriod, periodEnd);
            if (change.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal cashEffect = change.negate();
            operatingAdjustments.add(buildCashFlowLineItem(
                formatBalanceChangeDescription(account.getAccountName(), change), cashEffect, account.getAccountCode()));
            netOperatingAdjustments = netOperatingAdjustments.add(cashEffect);
        }
        
        // Working capital: current liabilities (excluding short-term debt)
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup()) || !isOperatingWorkingCapitalLiability(account)) {
                continue;
            }
            BigDecimal change = calculateBalanceChange(account.getId(), dayBeforePeriod, periodEnd);
            if (change.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            operatingAdjustments.add(buildCashFlowLineItem(
                formatBalanceChangeDescription(account.getAccountName(), change), change, account.getAccountCode()));
            netOperatingAdjustments = netOperatingAdjustments.add(change);
        }
        
        BigDecimal netCashFromOperations = netIncome.add(netOperatingAdjustments);
        
        // Investing: fixed assets (excluding accumulated depreciation)
        List<CashFlowResponse.CashFlowLineItem> investingActivities = new ArrayList<>();
        BigDecimal netCashFromInvesting = BigDecimal.ZERO;
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup()) || !isFixedAsset(account) || isAccumulatedDepreciation(account)) {
                continue;
            }
            BigDecimal change = calculateBalanceChange(account.getId(), dayBeforePeriod, periodEnd);
            if (change.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal cashEffect = change.negate();
            investingActivities.add(buildCashFlowLineItem(
                formatInvestingDescription(account.getAccountName(), change), cashEffect, account.getAccountCode()));
            netCashFromInvesting = netCashFromInvesting.add(cashEffect);
        }
        
        // Financing: long-term liabilities and capital contributions
        List<CashFlowResponse.CashFlowLineItem> financingActivities = new ArrayList<>();
        BigDecimal netCashFromFinancing = BigDecimal.ZERO;
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup()) || !isFinancingAccount(account)) {
                continue;
            }
            BigDecimal change = calculateBalanceChange(account.getId(), dayBeforePeriod, periodEnd);
            if (change.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            financingActivities.add(buildCashFlowLineItem(
                formatBalanceChangeDescription(account.getAccountName(), change), change, account.getAccountCode()));
            netCashFromFinancing = netCashFromFinancing.add(change);
        }
        
        BigDecimal cashAtBeginning = sumCashBalances(allAccounts, dayBeforePeriod);
        BigDecimal cashAtEnd = sumCashBalances(allAccounts, periodEnd);
        BigDecimal netCashFlow = cashAtEnd.subtract(cashAtBeginning);
        
        BigDecimal sectionTotal = netCashFromOperations.add(netCashFromInvesting).add(netCashFromFinancing);
        BigDecimal reconciliationGap = netCashFlow.subtract(sectionTotal);
        if (reconciliationGap.compareTo(BigDecimal.ZERO) != 0) {
            operatingAdjustments.add(buildCashFlowLineItem(
                "Reconciliation to cash balances", reconciliationGap, null));
            netCashFromOperations = netCashFromOperations.add(reconciliationGap);
        }
        
        return CashFlowResponse.builder()
            .organizationName("Organization")
            .periodName(period.getPeriodName())
            .startDate(periodStart.toString())
            .endDate(periodEnd.toString())
            .netIncome(netIncome)
            .operatingAdjustments(operatingAdjustments)
            .netCashFromOperations(netCashFromOperations)
            .investingActivities(investingActivities)
            .netCashFromInvesting(netCashFromInvesting)
            .financingActivities(financingActivities)
            .netCashFromFinancing(netCashFromFinancing)
            .netCashFlow(netCashFlow)
            .cashAtBeginning(cashAtBeginning)
            .cashAtEnd(cashAtEnd)
            .build();
    }
    
    private CashFlowResponse.CashFlowLineItem buildCashFlowLineItem(String description, BigDecimal amount, String accountCode) {
        return CashFlowResponse.CashFlowLineItem.builder()
            .description(description)
            .amount(amount)
            .accountCode(accountCode)
            .build();
    }
    
    private Period requirePeriodForOrganization(UUID organizationId, UUID periodId) {
        Period period = periodRepository.findById(periodId)
            .orElseThrow(() -> new RuntimeException("Period not found"));
        if (!organizationId.equals(period.getOrganizationId())) {
            throw new RuntimeException("Period does not belong to organization");
        }
        return period;
    }
    
    private BigDecimal computeNetIncome(List<ChartOfAccounts> allAccounts, LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO;
        BigDecimal totalOpex = BigDecimal.ZERO;
        
        for (ChartOfAccounts account : allAccounts) {
            if (Boolean.TRUE.equals(account.getIsGroup())) {
                continue;
            }
            BigDecimal activity = getPeriodActivity(account, periodStart, periodEnd);
            if (activity.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if ("REVENUE".equals(account.getAccountType())) {
                totalRevenue = totalRevenue.add(activity);
            } else if ("EXPENSE".equals(account.getAccountType())) {
                if (account.getAccountCategory() != null && account.getAccountCategory().contains("Cost of")) {
                    totalCOGS = totalCOGS.add(activity);
                } else {
                    totalOpex = totalOpex.add(activity);
                }
            }
        }
        
        return totalRevenue.subtract(totalCOGS).subtract(totalOpex);
    }
    
    private boolean isCashAccount(ChartOfAccounts account) {
        if (!"ASSET".equals(account.getAccountType()) || Boolean.TRUE.equals(account.getIsGroup())) {
            return false;
        }
        if ("CASH".equalsIgnoreCase(account.getAccountCode())) {
            return true;
        }
        int codeNum = parseAccountCodePrefix(account.getAccountCode());
        if (codeNum >= 1010 && codeNum <= 1049) {
            return true;
        }
        String name = account.getAccountName() != null ? account.getAccountName().toLowerCase() : "";
        return name.contains("cash on hand") || name.contains("petty cash")
            || name.contains("bank -") || name.contains("bank account")
            || (name.contains("cash") && name.contains("bank"));
    }
    
    private int parseAccountCodePrefix(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) {
            return -1;
        }
        try {
            return Integer.parseInt(accountCode.substring(0, 4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private boolean isContraAsset(ChartOfAccounts account) {
        if (!"ASSET".equals(account.getAccountType())) {
            return false;
        }
        String name = account.getAccountName() != null ? account.getAccountName().toLowerCase() : "";
        return name.contains("allowance") || name.contains("accumulated depreciation") || isAccumulatedDepreciation(account);
    }
    
    private boolean isCurrentAsset(ChartOfAccounts account) {
        return "ASSET".equals(account.getAccountType())
            && account.getAccountCategory() != null
            && account.getAccountCategory().contains("Current");
    }
    
    private boolean isCurrentLiability(ChartOfAccounts account) {
        return "LIABILITY".equals(account.getAccountType())
            && account.getAccountCategory() != null
            && account.getAccountCategory().contains("Current");
    }

    /** AP, accrued expenses, tax payable, etc. — excludes short-term loans (financing). */
    private boolean isOperatingWorkingCapitalLiability(ChartOfAccounts account) {
        if (!isCurrentLiability(account)) {
            return false;
        }
        String name = account.getAccountName() != null ? account.getAccountName().toLowerCase() : "";
        return !name.contains("loan");
    }
    
    private boolean isFixedAsset(ChartOfAccounts account) {
        return "ASSET".equals(account.getAccountType())
            && account.getAccountCategory() != null
            && account.getAccountCategory().contains("Fixed");
    }
    
    private boolean isAccumulatedDepreciation(ChartOfAccounts account) {
        if (account.getAccountCode() != null && account.getAccountCode().startsWith("155")) {
            return true;
        }
        return account.getAccountName() != null
            && account.getAccountName().toLowerCase().contains("accumulated depreciation");
    }
    
    private boolean isFinancingAccount(ChartOfAccounts account) {
        if ("LIABILITY".equals(account.getAccountType())) {
            String name = account.getAccountName() != null ? account.getAccountName().toLowerCase() : "";
            if (account.getAccountCategory() != null && account.getAccountCategory().contains("Long-term")) {
                return true;
            }
            return isCurrentLiability(account) && name.contains("loan");
        }
        if ("EQUITY".equals(account.getAccountType())) {
            String name = account.getAccountName() != null ? account.getAccountName().toLowerCase() : "";
            return name.contains("capital") && !name.contains("retained") && !name.contains("earnings");
        }
        return false;
    }
    
    private String formatBalanceChangeDescription(String accountName, BigDecimal change) {
        String direction = change.compareTo(BigDecimal.ZERO) > 0 ? "Increase" : "Decrease";
        return direction + " in " + accountName;
    }
    
    private String formatInvestingDescription(String accountName, BigDecimal change) {
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "Purchase of " + accountName;
        }
        return "Proceeds from " + accountName;
    }
    
    private BigDecimal sumCashBalances(List<ChartOfAccounts> accounts, LocalDate asOfDate) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChartOfAccounts account : accounts) {
            if (!Boolean.TRUE.equals(account.getIsGroup()) && isCashAccount(account)) {
                total = total.add(calculateAccountBalanceAsOf(account.getId(), asOfDate));
            }
        }
        return total;
    }
    
    private BigDecimal calculateBalanceChange(UUID accountId, LocalDate asOfBeforePeriod, LocalDate periodEnd) {
        BigDecimal begin = calculateAccountBalanceAsOf(accountId, asOfBeforePeriod);
        BigDecimal end = calculateAccountBalanceAsOf(accountId, periodEnd);
        return end.subtract(begin);
    }
    
    private BigDecimal getPeriodActivity(ChartOfAccounts account, LocalDate startDate, LocalDate endDate) {
        List<JournalLine> lines = journalLineRepository.findByAccountIdAndDateRange(account.getId(), startDate, endDate);
        
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (JournalLine line : lines) {
            JournalEntry entry = line.getJournalEntry();
            if (entry != null && "POSTED".equals(entry.getStatus())) {
                if (line.getDebitAmount() != null) {
                    totalDebit = totalDebit.add(line.getDebitAmount());
                }
                if (line.getCreditAmount() != null) {
                    totalCredit = totalCredit.add(line.getCreditAmount());
                }
            }
        }
        
        if ("ASSET".equals(account.getAccountType()) || "EXPENSE".equals(account.getAccountType())) {
            return totalDebit.subtract(totalCredit);
        }
        return totalCredit.subtract(totalDebit);
    }
    
    // Helper method to calculate account balance as of a specific date
    private BigDecimal calculateAccountBalanceAsOf(UUID accountId, LocalDate asOfDate) {
        // Get account opening balance
        ChartOfAccounts account = coaRepository.findById(accountId).orElse(null);
        BigDecimal balance = (account != null && account.getOpeningBalance() != null) 
            ? account.getOpeningBalance() 
            : BigDecimal.ZERO;
        
        // Add all transactions up to the date
        List<JournalLine> lines = journalLineRepository.findByAccountIdAndDateRange(
            accountId, LocalDate.of(1900, 1, 1), asOfDate);
        
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (JournalLine line : lines) {
            JournalEntry entry = line.getJournalEntry();
            if (entry != null && "POSTED".equals(entry.getStatus())) {
                if (line.getDebitAmount() != null) {
                    totalDebit = totalDebit.add(line.getDebitAmount());
                }
                if (line.getCreditAmount() != null) {
                    totalCredit = totalCredit.add(line.getCreditAmount());
                }
            }
        }
        
        // For assets/expenses: balance = opening + debits - credits
        // For liabilities/equity/revenue: balance = opening + credits - debits
        if (account != null && 
            ("ASSET".equals(account.getAccountType()) || "EXPENSE".equals(account.getAccountType()))) {
            return balance.add(totalDebit).subtract(totalCredit);
        } else {
            return balance.add(totalCredit).subtract(totalDebit);
        }
    }
}

