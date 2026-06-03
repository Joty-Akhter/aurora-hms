package com.easyops.accounting.service;

import com.easyops.accounting.dto.CoARequest;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.repository.ChartOfAccountsRepository;
import com.easyops.accounting.repository.JournalLineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartOfAccountsServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ORG_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @Mock
    private ChartOfAccountsRepository coaRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @InjectMocks
    private ChartOfAccountsService chartOfAccountsService;

    @Test
    void updateAccount_persistsCodeWhenNoPostedActivity() {
        ChartOfAccounts existing = account("1000", "Cash");
        CoARequest request = updateRequest("1010", "Cash on Hand");

        when(coaRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(journalLineRepository.existsPostedOrReversedLinesForAccount(ACCOUNT_ID)).thenReturn(false);
        when(coaRepository.existsByOrganizationIdAndAccountCode(ORG_ID, "1010")).thenReturn(false);
        when(coaRepository.save(any(ChartOfAccounts.class))).thenAnswer(inv -> inv.getArgument(0));

        ChartOfAccounts updated = chartOfAccountsService.updateAccount(ACCOUNT_ID, request, USER_ID);

        assertThat(updated.getAccountCode()).isEqualTo("1010");
        assertThat(updated.getAccountName()).isEqualTo("Cash on Hand");
    }

    @Test
    void updateAccount_rejectsCodeChangeWhenPostedLinesExist() {
        ChartOfAccounts existing = account("1000", "Cash");
        CoARequest request = updateRequest("1010", "Cash");

        when(coaRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(journalLineRepository.existsPostedOrReversedLinesForAccount(ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> chartOfAccountsService.updateAccount(ACCOUNT_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot change account code");

        verify(coaRepository, never()).save(any());
    }

    private static ChartOfAccounts account(String code, String name) {
        ChartOfAccounts account = new ChartOfAccounts();
        account.setId(ACCOUNT_ID);
        account.setOrganizationId(ORG_ID);
        account.setAccountCode(code);
        account.setAccountName(name);
        account.setIsActive(true);
        return account;
    }

    private static CoARequest updateRequest(String code, String name) {
        CoARequest request = new CoARequest();
        request.setOrganizationId(ORG_ID);
        request.setAccountCode(code);
        request.setAccountName(name);
        request.setAccountType("ASSET");
        return request;
    }
}
