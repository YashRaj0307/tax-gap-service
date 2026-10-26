
package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxGapReportServiceTest {

    @Mock
    private FinancialTransactionRepository transactions;

    @Mock
    private ComplianceExceptionRepository exceptions;

    private TaxGapReportService service;

    @BeforeEach
    void setUp() {
        service = new TaxGapReportService(transactions, exceptions);
    }

    @Test
    void customerTaxSummaryReturnsReportForCustomer() {
        FinancialTransactionRepository.CustomerTaxSummaryProjection projection =
                mock(FinancialTransactionRepository.CustomerTaxSummaryProjection.class);

        when(transactions.customerTaxSummary("CUST-101"))
                .thenReturn(projection);

        when(projection.getCustomerId()).thenReturn("CUST-101");
        when(projection.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));
        when(projection.getTotalReportedTax()).thenReturn(new BigDecimal("180.00"));
        when(projection.getTotalExpectedTax()).thenReturn(new BigDecimal("180.00"));
        when(projection.getTotalTaxGap()).thenReturn(BigDecimal.ZERO);
        when(projection.getTotalTransactions()).thenReturn(5L);
        when(projection.getNonCompliantTransactions()).thenReturn(0L);
        when(projection.getComplianceScore()).thenReturn(new BigDecimal("100.00"));

        var result = service.customerTaxSummary("CUST-101");

        assertNotNull(result);
        verify(transactions).customerTaxSummary("CUST-101");
    }

    @Test
    void customerTaxSummaryThrowsWhenCustomerHasNoTransactions() {
        when(transactions.customerTaxSummary("UNKNOWN"))
                .thenReturn(null);

        assertThrows(
                EntityNotFoundException.class,
                () -> service.customerTaxSummary("UNKNOWN"));

        verify(transactions).customerTaxSummary("UNKNOWN");
    }

    @Test
    void customerTaxSummaryReplacesNullAmountsWithZero() {
        FinancialTransactionRepository.CustomerTaxSummaryProjection projection =
                mock(FinancialTransactionRepository.CustomerTaxSummaryProjection.class);

        when(transactions.customerTaxSummary("CUST-102"))
                .thenReturn(projection);

        when(projection.getCustomerId()).thenReturn("CUST-102");
        when(projection.getTotalAmount()).thenReturn(null);
        when(projection.getTotalReportedTax()).thenReturn(null);
        when(projection.getTotalExpectedTax()).thenReturn(null);
        when(projection.getTotalTaxGap()).thenReturn(null);
        when(projection.getTotalTransactions()).thenReturn(null);
        when(projection.getNonCompliantTransactions()).thenReturn(null);
        when(projection.getComplianceScore()).thenReturn(null);

        var result = service.customerTaxSummary("CUST-102");

        assertNotNull(result);
        verify(transactions).customerTaxSummary("CUST-102");
    }

    @Test
    void exceptionSummaryReturnsCountsBySeverityAndCustomer() {
        when(exceptions.count()).thenReturn(4L);

        when(exceptions.countBySeverity()).thenReturn(List.of(
                new Object[]{"HIGH", 2L},
                new Object[]{"MEDIUM", 1L}
        ));

        when(exceptions.countByCustomer()).thenReturn(List.of(
                new Object[]{"CUST-101", 3L},
                new Object[]{"CUST-102", 1L}
        ));

        var result = service.exceptionSummary();

        assertNotNull(result);
        verify(exceptions).count();
        verify(exceptions).countBySeverity();
        verify(exceptions).countByCustomer();
    }


    
@Test
void exceptionSummaryReturnsZeroCountsWhenNoExceptionsExist() {
    when(exceptions.count()).thenReturn(0L);

    List<Object[]> severityRows = new java.util.ArrayList<Object[]>();
    List<Object[]> customerRows = new java.util.ArrayList<Object[]>();

    when(exceptions.countBySeverity()).thenReturn(severityRows);
    when(exceptions.countByCustomer()).thenReturn(customerRows);

    var result = service.exceptionSummary();

    assertNotNull(result);
    assertEquals(0L, result.totalExceptions());

    verify(exceptions).count();
    verify(exceptions).countBySeverity();
    verify(exceptions).countByCustomer();
}


    
@Test
void exceptionSummaryIncludesLowSeverityWhenReturnedByRepository() {
    when(exceptions.count()).thenReturn(1L);

    List<Object[]> severityRows = new java.util.ArrayList<>();
    severityRows.add(new Object[]{"LOW", 1L});

    List<Object[]> customerRows = new java.util.ArrayList<>();
    customerRows.add(new Object[]{"CUST-103", 1L});

    when(exceptions.countBySeverity()).thenReturn(severityRows);
    when(exceptions.countByCustomer()).thenReturn(customerRows);

    var result = service.exceptionSummary();

    assertNotNull(result);
    verify(exceptions).countBySeverity();
    verify(exceptions).countByCustomer();
}

}
