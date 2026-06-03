package com.easyops.hr.service;

import com.easyops.hr.entity.LoanInterestMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * LC-04: builds per-month scheduled repayment amounts from category interest method.
 * NONE: equal principal (no interest). FLAT: total interest on full principal for tenure, equal combined payments.
 * REDUCING_BALANCE: standard EMI amortization using annual rate / 12 as monthly rate.
 */
public final class LoanScheduleBuilder {

    private static final RoundingMode ROUND = RoundingMode.HALF_UP;
    private static final int SCALE = 2;

    private LoanScheduleBuilder() {}

    /**
     * Scheduled total due each period (principal + interest combined in one line, matching {@code LoanInstallment.scheduledAmount}).
     */
    public static List<BigDecimal> monthlyScheduledAmounts(
            BigDecimal principal,
            int tenureMonths,
            LoanInterestMethod method,
            BigDecimal flatAnnualRatePercent) {
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        LoanInterestMethod m = method != null ? method : LoanInterestMethod.NONE;
        return switch (m) {
            case NONE -> equalPrincipalAmounts(principal, tenureMonths);
            case FLAT -> flatInterestEqualPayments(principal, tenureMonths, requireRate(flatAnnualRatePercent, "FLAT"));
            case REDUCING_BALANCE -> reducingBalanceEmiPayments(principal, tenureMonths, requireRate(flatAnnualRatePercent, "REDUCING_BALANCE"));
        };
    }

    private static BigDecimal requireRate(BigDecimal rate, String label) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("flatAnnualRatePercent is required and must be positive for " + label);
        }
        return rate;
    }

    /** Equal principal per month (legacy v1). */
    static List<BigDecimal> equalPrincipalAmounts(BigDecimal principal, int tenureMonths) {
        BigDecimal n = BigDecimal.valueOf(tenureMonths);
        BigDecimal base = principal.divide(n, SCALE, ROUND);
        List<BigDecimal> list = new ArrayList<>(tenureMonths);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal amt = (i == tenureMonths)
                    ? principal.subtract(allocated).setScale(SCALE, ROUND)
                    : base;
            allocated = allocated.add(amt);
            list.add(amt);
        }
        return list;
    }

    /**
     * Flat interest on full principal for the whole tenure: total interest = P × (R/100) × (T/12), combined payment = (P + TI) / T per month.
     */
    static List<BigDecimal> flatInterestEqualPayments(BigDecimal principal, int tenureMonths, BigDecimal annualPercent) {
        BigDecimal t = BigDecimal.valueOf(tenureMonths);
        BigDecimal totalInterest = principal
                .multiply(annualPercent)
                .divide(BigDecimal.valueOf(100), 16, ROUND)
                .multiply(t)
                .divide(BigDecimal.valueOf(12), 16, ROUND)
                .setScale(SCALE, ROUND);
        BigDecimal totalRepayable = principal.add(totalInterest);
        BigDecimal n = BigDecimal.valueOf(tenureMonths);
        BigDecimal base = totalRepayable.divide(n, SCALE, ROUND);
        List<BigDecimal> list = new ArrayList<>(tenureMonths);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal amt = (i == tenureMonths)
                    ? totalRepayable.subtract(allocated).setScale(SCALE, ROUND)
                    : base;
            allocated = allocated.add(amt);
            list.add(amt);
        }
        return list;
    }

    /**
     * Reducing balance: EMI amortization using monthly rate = annual%/12/100.
     */
    static List<BigDecimal> reducingBalanceEmiPayments(BigDecimal principal, int tenureMonths, BigDecimal annualPercent) {
        BigDecimal monthlyRate = annualPercent.divide(BigDecimal.valueOf(1200), 16, ROUND);
        int n = tenureMonths;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return equalPrincipalAmounts(principal, tenureMonths);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(n);
        BigDecimal emi = principal.multiply(monthlyRate).multiply(pow).divide(pow.subtract(BigDecimal.ONE), SCALE, ROUND);

        List<BigDecimal> amounts = new ArrayList<>(n);
        BigDecimal balance = principal.setScale(SCALE, ROUND);
        for (int i = 1; i <= n; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(SCALE, ROUND);
            BigDecimal principalPart;
            if (i == n) {
                principalPart = balance;
            } else {
                principalPart = emi.subtract(interest);
                if (principalPart.compareTo(balance) > 0) {
                    principalPart = balance;
                }
                if (principalPart.compareTo(BigDecimal.ZERO) < 0) {
                    principalPart = BigDecimal.ZERO;
                }
            }
            BigDecimal payment = principalPart.add(interest).setScale(SCALE, ROUND);
            amounts.add(payment);
            balance = balance.subtract(principalPart).setScale(SCALE, ROUND);
        }
        return amounts;
    }
}
