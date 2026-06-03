package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loan_repayment_allocations", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id")
    private UUID allocationId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "installment_id", nullable = false)
    private UUID installmentId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
