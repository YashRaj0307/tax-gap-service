package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.AuditEvent;
import com.avega.tax_gap_service.domain.ComplianceRule;
import com.avega.tax_gap_service.domain.FinancialTransaction;
import com.avega.tax_gap_service.repository.AuditEventRepository;
import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import com.avega.tax_gap_service.repository.ComplianceRuleRepository;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceTest {
    @Mock FinancialTransactionRepository transactions;
    @Mock ComplianceRuleRepository rules;
    @Mock ComplianceExceptionRepository exceptions;
    @Mock AuditEventRepository auditEvents;
    private ObjectMapper mapper;
    private FinancialTransactionService service;

    @BeforeEach
void setUp() {
    mapper = new ObjectMapper();
    service = new FinancialTransactionService(
            transactions, rules, exceptions, auditEvents, mapper);

    lenient().when(transactions.saveAndFlush(
            ArgumentMatchers.any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    lenient().when(auditEvents.save(
            ArgumentMatchers.any(AuditEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    lenient().when(rules.findByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of());
}

    @Test
    void calculatesExpectedTaxAndCompliantStatus() throws Exception {
        JsonNode raw = mapper.readTree("""
            {"transactionId":"TX-1","date":"2026-10-20","customerId":"C-1","amount":1000.00,"taxRate":0.18,"reportedTax":180.00,"transactionType":"SALE"}
            """);
        var response = service.ingestBatch(List.of(raw));
        assertEquals(1, response.totalTransactions());
        assertEquals(1, response.successfulTransactions());
        assertEquals("COMPLIANT", response.results().getFirst().complianceStatus());
    }

    @Test
    void invalidDateIsStoredAsFailedRecord() throws Exception {
        JsonNode raw = mapper.readTree("""
            {"transactionId":"TX-2","date":"not-a-date","customerId":"C-1","amount":1000.00,"taxRate":0.18,"reportedTax":180.00,"transactionType":"SALE"}
            """);
        var response = service.ingestBatch(List.of(raw));
        assertEquals(1, response.failedTransactions());
        assertEquals("FAILURE", response.results().getFirst().validationStatus());
        assertTrue(response.results().getFirst().failureReasons().stream().anyMatch(s -> s.contains("valid date")));
    }

    @Test
    void missingReportedTaxIsNonCompliantButValid() throws Exception {
        JsonNode raw = mapper.readTree("""
            {"transactionId":"TX-3","date":"2026-10-20","customerId":"C-1","amount":200.00,"taxRate":0.18,"transactionType":"EXPENSE"}
            """);
        var response = service.ingestBatch(List.of(raw));
        assertEquals(1, response.successfulTransactions());
        assertEquals("SUCCESS", response.results().getFirst().validationStatus());
        assertEquals("NON_COMPLIANT", response.results().getFirst().complianceStatus());
    }


    @Test
    void amountAboveConfiguredThresholdCreatesHighValueException() throws Exception {
        ObjectNode config = mapper.createObjectNode().put("threshold", 500);
        ComplianceRule rule = new ComplianceRule("HIGH_VALUE_TRANSACTION", "HIGH_VALUE", true, "HIGH", config);
        when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(rule));
        JsonNode raw = mapper.readTree("""
            {"transactionId":"TX-4","date":"2026-10-20","customerId":"C-1","amount":1000.00,"taxRate":0.18,"reportedTax":180.00,"transactionType":"SALE"}
            """);
        service.ingestBatch(List.of(raw));
        verify(exceptions).save(argThat(e -> e.getRuleName().equals("HIGH_VALUE_TRANSACTION")
                && e.getSeverity().equals("HIGH") && e.getTransactionId().equals("TX-4")));
    }

    @Test
    void taxGapGreaterThanOneIsUnderpaid() throws Exception {
        JsonNode raw = mapper.readTree("""
            {"transactionId":"TX-5","date":"2026-10-20","customerId":"C-1","amount":1000.00,"taxRate":0.18,"reportedTax":178.00,"transactionType":"SALE"}
            """);
        var response = service.ingestBatch(List.of(raw));
        assertEquals("UNDERPAID", response.results().getFirst().complianceStatus());
    }

    @Test
    void nonObjectRecordIsStoredAsFailure() throws Exception {
        JsonNode raw = mapper.readTree("42");
        var response = service.ingestBatch(List.of(raw));
        assertEquals("FAILURE", response.results().getFirst().validationStatus());
        assertFalse(response.results().getFirst().failureReasons().isEmpty());
    }

    @Test
    void emptyBatchIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.ingestBatch(List.of()));
    }
}
