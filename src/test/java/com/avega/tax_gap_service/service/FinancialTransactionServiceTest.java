package com.avega.tax_gap_service.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.avega.tax_gap_service.domain.AuditEvent;
import com.avega.tax_gap_service.domain.ComplianceRule;
import com.avega.tax_gap_service.domain.FinancialTransaction;
import com.avega.tax_gap_service.repository.AuditEventRepository;
import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import com.avega.tax_gap_service.repository.ComplianceRuleRepository;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Test
void gstRuleCreatesExceptionWhenTaxRateIsBelowRequiredRate() throws Exception {
    ObjectNode config = mapper.createObjectNode();
    config.putArray("slabs")
            .addObject()
            .put("threshold", 1000)
            .put("requiredRate", 0.18);

    ComplianceRule rule = new ComplianceRule(
            "GST_SLAB_RULE", "GST_SLAB", true, "HIGH", config);
    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(rule));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"GST-1","date":"2026-10-20","customerId":"C-1",
         "amount":1500,"taxRate":0.05,"reportedTax":75,"transactionType":"SALE"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions).save(argThat(e ->
            e.getRuleName().equals("GST_SLAB_RULE")
            && e.getSeverity().equals("HIGH")));
}

@Test
void gstRuleDoesNotCreateExceptionWhenTaxRateMeetsRequiredRate() throws Exception {
    ObjectNode config = mapper.createObjectNode();
    config.putArray("slabs")
            .addObject()
            .put("threshold", 1000)
            .put("requiredRate", 0.18);

    ComplianceRule rule = new ComplianceRule(
            "GST_SLAB_RULE", "GST_SLAB", true, "HIGH", config);
    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(rule));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"GST-2","date":"2026-10-20","customerId":"C-1",
         "amount":1500,"taxRate":0.18,"reportedTax":270,"transactionType":"SALE"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions, never()).save(any());
}

@Test
void gstRuleDoesNotApplySlabWhenAmountEqualsThreshold() throws Exception {
    ObjectNode config = mapper.createObjectNode();
    config.putArray("slabs")
            .addObject()
            .put("threshold", 1000)
            .put("requiredRate", 0.18);

    ComplianceRule rule = new ComplianceRule(
            "GST_SLAB_RULE", "GST_SLAB", true, "HIGH", config);
    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(rule));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"GST-3","date":"2026-10-20","customerId":"C-1",
         "amount":1000,"taxRate":0.05,"reportedTax":50,"transactionType":"SALE"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions, never()).save(any());
}

@Test
void missingAndInvalidRequiredFieldsAreRecordedAsFailures() {
    ObjectNode missingTransactionId = validTransaction("VAL-1");
    missingTransactionId.remove("transactionId");

    ObjectNode blankCustomerId = validTransaction("VAL-2");
    blankCustomerId.put("customerId", " ");

    ObjectNode longTransactionId = validTransaction("VAL-3");
    longTransactionId.put("transactionId", "T".repeat(101));

    ObjectNode missingDate = validTransaction("VAL-4");
    missingDate.remove("date");

    ObjectNode nonTextDate = validTransaction("VAL-5");
    nonTextDate.put("date", 12345);

    ObjectNode invalidType = validTransaction("VAL-6");
    invalidType.put("transactionType", "INVALID");

    for (ObjectNode raw : List.of(
            missingTransactionId,
            blankCustomerId,
            longTransactionId,
            missingDate,
            nonTextDate,
            invalidType)) {
        var response = service.ingestBatch(List.of(raw));

        assertEquals(1, response.failedTransactions(),
                "Expected validation failure for: " + raw);
        assertEquals("FAILURE",
                response.results().getFirst().validationStatus());
    }
}


@Test
void invalidAmountsAreRecordedAsFailures() {
    ObjectNode missingAmount = validTransaction("AMT-1");
    missingAmount.remove("amount");

    ObjectNode textAmount = validTransaction("AMT-2");
    textAmount.put("amount", "not-a-number");

    ObjectNode negativeAmount = validTransaction("AMT-3");
    negativeAmount.put("amount", -10);

    ObjectNode zeroAmount = validTransaction("AMT-4");
    zeroAmount.put("amount", 0);

    ObjectNode excessivePrecision = validTransaction("AMT-5");
    excessivePrecision.put("amount", 123456789012345678.00);

    for (ObjectNode raw : List.of(
            missingAmount,
            textAmount,
            negativeAmount,
            zeroAmount,
            excessivePrecision)) {
        var response = service.ingestBatch(List.of(raw));

        assertEquals(1, response.failedTransactions(),
                "Expected amount validation failure for: " + raw);
    }
}

@Test
void invalidTaxRatesAndReportedTaxAreRecordedAsFailures() {
    ObjectNode missingRate = validTransaction("RATE-1");
    missingRate.remove("taxRate");

    ObjectNode textRate = validTransaction("RATE-2");
    textRate.put("taxRate", "invalid");

    ObjectNode negativeRate = validTransaction("RATE-3");
    negativeRate.put("taxRate", -0.05);

    ObjectNode excessiveRate = validTransaction("RATE-4");
    excessiveRate.put("taxRate", 1234.5);

    ObjectNode textReportedTax = validTransaction("RATE-5");
    textReportedTax.put("reportedTax", "invalid");

    for (ObjectNode raw : List.of(
            missingRate,
            textRate,
            negativeRate,
            excessiveRate,
            textReportedTax)) {
        var response = service.ingestBatch(List.of(raw));

        assertEquals(1, response.failedTransactions(),
                "Expected numeric validation failure for: " + raw);
    }
}

private ObjectNode validTransaction(String transactionId) {
    return mapper.createObjectNode()
            .put("transactionId", transactionId)
            .put("date", "2026-10-20")
            .put("customerId", "C-1")
            .put("amount", 1000.00)
            .put("taxRate", 0.18)
            .put("reportedTax", 180.00)
            .put("transactionType", "SALE");
}

@Test
void refundExceedingOriginalSaleCreatesException() throws Exception {
    FinancialTransaction sale = successfulSale("SALE-1", "C-1", "100.00");

    when(transactions.findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
            "SALE-1", "SALE", "SUCCESS"))
            .thenReturn(java.util.Optional.of(sale));

    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            new ComplianceRule(
                    "REFUND_LIMIT_RULE", "REFUND_LIMIT", true, "HIGH",
                    mapper.createObjectNode())));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"REFUND-1","originalTransactionId":"SALE-1",
         "date":"2026-10-21","customerId":"C-1","amount":120,
         "taxRate":0.18,"reportedTax":21.60,"transactionType":"REFUND"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions).save(argThat(e ->
            e.getRuleName().equals("REFUND_LIMIT_RULE")
            && e.getMessage().contains("exceeds original sale amount")));
}

@Test
void refundWithMissingOriginalSaleCreatesException() throws Exception {
    when(transactions.findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
            "UNKNOWN-SALE", "SALE", "SUCCESS"))
            .thenReturn(java.util.Optional.empty());

    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            new ComplianceRule(
                    "REFUND_LIMIT_RULE", "REFUND_LIMIT", true, "HIGH",
                    mapper.createObjectNode())));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"REFUND-2","originalTransactionId":"UNKNOWN-SALE",
         "date":"2026-10-21","customerId":"C-1","amount":50,
         "taxRate":0.18,"reportedTax":9,"transactionType":"REFUND"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions).save(argThat(e ->
            e.getRuleName().equals("REFUND_LIMIT_RULE")
            && e.getMessage().contains("was not found")));
}

@Test
void refundEqualToOriginalSaleAmountDoesNotCreateException() throws Exception {
    FinancialTransaction sale = successfulSale("SALE-2", "C-1", "100.00");

    when(transactions.findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
            "SALE-2", "SALE", "SUCCESS"))
            .thenReturn(java.util.Optional.of(sale));

    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            new ComplianceRule(
                    "REFUND_LIMIT_RULE", "REFUND_LIMIT", true, "HIGH",
                    mapper.createObjectNode())));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"REFUND-3","originalTransactionId":"SALE-2",
         "date":"2026-10-21","customerId":"C-1","amount":100,
         "taxRate":0.18,"reportedTax":18,"transactionType":"REFUND"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions, never()).save(any());
}

@Test
void refundForDifferentCustomerCreatesException() throws Exception {
    FinancialTransaction sale = successfulSale("SALE-3", "C-1", "100.00");

    when(transactions.findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
            "SALE-3", "SALE", "SUCCESS"))
            .thenReturn(java.util.Optional.of(sale));

    when(rules.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            new ComplianceRule(
                    "REFUND_LIMIT_RULE", "REFUND_LIMIT", true, "HIGH",
                    mapper.createObjectNode())));

    JsonNode raw = mapper.readTree("""
        {"transactionId":"REFUND-4","originalTransactionId":"SALE-3",
         "date":"2026-10-21","customerId":"C-2","amount":50,
         "taxRate":0.18,"reportedTax":9,"transactionType":"REFUND"}
        """);

    service.ingestBatch(List.of(raw));

    verify(exceptions).save(argThat(e ->
            e.getRuleName().equals("REFUND_LIMIT_RULE")
            && e.getMessage().contains("must match")));
}

private FinancialTransaction successfulSale(
        String transactionId, String customerId, String amount) {
    FinancialTransaction sale = new FinancialTransaction();
    sale.setTransactionId(transactionId);
    sale.setCustomerId(customerId);
    sale.setTransactionType("SALE");
    sale.setValidationStatus("SUCCESS");
    sale.setAmount(new java.math.BigDecimal(amount));
    return sale;
   }
}
