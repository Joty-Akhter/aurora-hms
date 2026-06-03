package com.easyops.ar.service;

import com.easyops.accountingperiod.AccountingPeriodResolver;
import com.easyops.ar.dto.InvoiceLineRequest;
import com.easyops.ar.dto.InvoiceRequest;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID RESOLVED_PERIOD_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");
    private static final LocalDate INVOICE_DATE = LocalDate.of(2026, 5, 15);

    @Mock
    private ARInvoiceRepository invoiceRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountingPeriodResolver accountingPeriodResolver;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository,
                customerRepository,
                accountingPeriodResolver,
                null);
    }

    @Test
    void createInvoice_resolvesPeriodWhenNotProvided() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setCustomerName("Acme");
        customer.setCreditLimit(BigDecimal.valueOf(10000));

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(invoiceRepository.existsByInvoiceNumber("INV-000001")).thenReturn(false);
        when(accountingPeriodResolver.resolvePeriodId(ORG_ID, INVOICE_DATE, ACTOR_ID)).thenReturn(RESOLVED_PERIOD_ID);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceRequest request = buildRequest(null);

        var invoice = invoiceService.createInvoice(request, ACTOR_ID);

        assertThat(invoice.getPeriodId()).isEqualTo(RESOLVED_PERIOD_ID);
        verify(accountingPeriodResolver).resolvePeriodId(ORG_ID, INVOICE_DATE, ACTOR_ID);
    }

    @Test
    void createInvoice_requiresActorUserId() {
        InvoiceRequest request = buildRequest(null);
        assertThatThrownBy(() -> invoiceService.createInvoice(request, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Authenticated user is required");
    }

    @Test
    void createInvoice_usesProvidedPeriodWhenItMatchesInvoiceDate() {
        UUID explicitPeriod = UUID.fromString("44444444-5555-6666-7777-888888888888");
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setCustomerName("Acme");
        customer.setCreditLimit(BigDecimal.valueOf(10000));

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(invoiceRepository.existsByInvoiceNumber("INV-000001")).thenReturn(false);
        when(accountingPeriodResolver.resolvePeriodId(ORG_ID, INVOICE_DATE, ACTOR_ID)).thenReturn(explicitPeriod);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceRequest request = buildRequest(explicitPeriod);

        var invoice = invoiceService.createInvoice(request, ACTOR_ID);

        assertThat(invoice.getPeriodId()).isEqualTo(explicitPeriod);
        verify(accountingPeriodResolver).resolvePeriodId(ORG_ID, INVOICE_DATE, ACTOR_ID);
    }

    @Test
    void createInvoice_rejectsMismatchedProvidedPeriod() {
        UUID wrongPeriod = UUID.fromString("44444444-5555-6666-7777-888888888888");
        when(invoiceRepository.existsByInvoiceNumber("INV-000001")).thenReturn(false);
        when(accountingPeriodResolver.resolvePeriodId(ORG_ID, INVOICE_DATE, ACTOR_ID)).thenReturn(RESOLVED_PERIOD_ID);

        InvoiceRequest request = buildRequest(wrongPeriod);

        assertThatThrownBy(() -> invoiceService.createInvoice(request, ACTOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not contain invoice date");
    }

    private static InvoiceRequest buildRequest(UUID periodId) {
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setDescription("Consulting");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(BigDecimal.valueOf(100));
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setTaxPercent(BigDecimal.ZERO);
        line.setAccountId(UUID.randomUUID());

        InvoiceRequest request = new InvoiceRequest();
        request.setOrganizationId(ORG_ID);
        request.setInvoiceNumber("INV-000001");
        request.setInvoiceDate(INVOICE_DATE);
        request.setDueDate(INVOICE_DATE.plusDays(30));
        request.setCustomerId(CUSTOMER_ID);
        request.setPeriodId(periodId);
        request.setLines(List.of(line));
        return request;
    }
}
