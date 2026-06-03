package com.easyops.hr.integration;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.repository.EpfAccountRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ST-02: derives available PF balance from active EPF accounts for the employee (internal integration).
 */
@Component
@Primary
public class ProvidentFundPfSettlementClient implements PfSettlementClient {

    private final EpfAccountRepository epfAccountRepository;

    public ProvidentFundPfSettlementClient(EpfAccountRepository epfAccountRepository) {
        this.epfAccountRepository = epfAccountRepository;
    }

    @Override
    public Optional<BigDecimal> getAvailableSettlementAmount(UUID organizationId, UUID employeeId) {
        List<EpfAccount> accounts = epfAccountRepository.findByEmployeeId(employeeId);
        BigDecimal sum = BigDecimal.ZERO;
        for (EpfAccount a : accounts) {
            if (!organizationId.equals(a.getOrganizationId())) {
                continue;
            }
            if (!Boolean.TRUE.equals(a.getIsActive())) {
                continue;
            }
            if (a.getCurrentBalance() != null) {
                sum = sum.add(a.getCurrentBalance());
            }
        }
        return sum.compareTo(BigDecimal.ZERO) > 0 ? Optional.of(sum) : Optional.empty();
    }
}
