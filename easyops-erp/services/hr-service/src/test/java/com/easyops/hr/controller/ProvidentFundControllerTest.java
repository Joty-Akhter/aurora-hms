package com.easyops.hr.controller;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.service.EmployeeService;
import com.easyops.hr.service.ProvidentFundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled("Mockito inline mock + JDK 25 ByteBuddy cannot instrument ProvidentFundService in this environment; run under JDK 21 or update Mockito when available.")
class ProvidentFundControllerTest {
    
    @Mock
    private ProvidentFundService providentFundService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private HrRbacService hrRbac;
    
    @InjectMocks
    private ProvidentFundController providentFundController;
    
    private UUID organizationId;
    private UUID employeeId;
    private UUID epfAccountId;
    
    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        epfAccountId = UUID.randomUUID();
        doNothing().when(hrRbac).requireHrView(any(), any());
        doNothing().when(hrRbac).requireHrManage(any(), any());
    }
    
    @Test
    void testGetEpfAccounts_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .organizationId(organizationId)
                .build();
        
        when(providentFundService.getEpfAccountsByOrganization(organizationId))
                .thenReturn(List.of(account));
        
        // When
        ResponseEntity<List<EpfAccount>> response = providentFundController.getEpfAccounts(USER_HEADER, organizationId);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
    
    @Test
    void testCreateEpfAccount_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .employeeId(employeeId)
                .organizationId(organizationId)
                .epfAccountNumber("EPF001")
                .openingDate(LocalDate.now())
                .currentBalance(BigDecimal.ZERO)
                .build();
        
        when(providentFundService.createEpfAccount(any(EpfAccount.class))).thenReturn(account);
        
        // When
        ResponseEntity<EpfAccount> response = providentFundController.createEpfAccount(USER_HEADER, account);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

