package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.AuditEvent;
import com.avega.tax_gap_service.domain.ComplianceException;
import com.avega.tax_gap_service.domain.ComplianceRule;
import com.avega.tax_gap_service.domain.FinancialTransaction;
import com.avega.tax_gap_service.dto.FinancialTransactionBatchResponse;
import com.avega.tax_gap_service.repository.AuditEventRepository;
import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import com.avega.tax_gap_service.repository.ComplianceRuleRepository;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FinancialTransactionService {
    private static final BigDecimal TOLERANCE = new BigDecimal("1.00");
    private final FinancialTransactionRepository transactions;
    private final ComplianceRuleRepository rules;
    private final ComplianceExceptionRepository exceptions;
    private final AuditEventRepository auditEvents;
    private final ObjectMapper objectMapper;

    public FinancialTransactionService(FinancialTransactionRepository transactions,
                                       ComplianceRuleRepository rules,
                                       ComplianceExceptionRepository exceptions,
                                       AuditEventRepository auditEvents,
                                       ObjectMapper objectMapper) {
        this.transactions = transactions;
        this.rules = rules;
        this.exceptions = exceptions;
        this.auditEvents = auditEvents;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FinancialTransactionBatchResponse ingestBatch(List<JsonNode> batch) {
        if (batch == null || batch.isEmpty()) {
            throw new IllegalArgumentException("Transaction batch must not be empty");
        }
        if (batch.size() > 1000) {
            throw new IllegalArgumentException("A batch cannot contain more than 1000 transactions");
        }
        List<FinancialTransactionBatchResponse.FinancialTransactionResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        for (JsonNode raw : batch) {
            FinancialTransaction transaction = validateAndCalculate(raw);
            FinancialTransaction saved = transactions.saveAndFlush(transaction);
            auditEvents.save(new AuditEvent(saved.getTransactionId(), "INGESTION",
                    detail("validationStatus", saved.getValidationStatus(), "rawRecordStored", true)));
            if ("SUCCESS".equals(saved.getValidationStatus())) {
                successful++;
                auditEvents.save(new AuditEvent(saved.getTransactionId(), "TAX_COMPUTATION",
                        taxDetail(saved)));
            } else {
                failed++;
            }
            // Every ingested record is considered by each active rule; rules skip
            // checks whose required fields are unavailable on invalid records.
            runRules(saved, raw);
            results.add(new FinancialTransactionBatchResponse.FinancialTransactionResult(
                    saved.getId(), saved.getTransactionId(), saved.getValidationStatus(),
                    saved.getComplianceStatus(), jsonStrings(saved.getFailureReasons())));
        }
        return new FinancialTransactionBatchResponse(batch.size(), successful, failed, results);
    }

    private FinancialTransaction validateAndCalculate(JsonNode raw) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setCreatedAt(OffsetDateTime.now());
        tx.setRawTransaction(raw == null ? objectMapper.createObjectNode() : raw.deepCopy());
        List<String> failures = new ArrayList<>();
        if (raw == null || !raw.isObject()) {
            tx.setTransactionId("INVALID-" + UUID.randomUUID());
            return fail(tx, failures, "Transaction must be a JSON object");
        }

        String id = text(raw.get("transactionId"));
        if (isBlank(id)) {
            failures.add("transactionId is required and must be a non-empty string");
            tx.setTransactionId("INVALID-" + UUID.randomUUID());
        } else if (id.length() > 100) {
            failures.add("transactionId must not exceed 100 characters");
            tx.setTransactionId("INVALID-" + UUID.randomUUID());
        } else {
            tx.setTransactionId(id);
        }
        String customerId = text(raw.get("customerId"));
        if (isBlank(customerId)) {
            failures.add("customerId is required and must be a non-empty string");
        } else if (customerId.length() > 100) {
            failures.add("customerId must not exceed 100 characters");
        } else {
            tx.setCustomerId(customerId);
        }

        JsonNode originalIdNode = raw.get("originalTransactionId");
        String originalId = text(originalIdNode);
        if (originalId != null && originalId.length() > 100) {
            failures.add("originalTransactionId must not exceed 100 characters");
        } else {
            tx.setOriginalTransactionId(originalId);
        }

        JsonNode dateNode = raw.get("date");
        if (dateNode == null || !dateNode.isTextual() || dateNode.asText().isBlank()) {
            failures.add("date is required and must use YYYY-MM-DD format");
        } else {
            try { tx.setTransactionDate(LocalDate.parse(dateNode.asText())); }
            catch (DateTimeParseException ex) { failures.add("date must be a valid date in YYYY-MM-DD format"); }
        }

        tx.setAmount(parseMoney(raw.get("amount"), "amount", failures, true));
        tx.setTaxRate(parseRate(raw.get("taxRate"), failures));
        tx.setReportedTax(parseMoney(raw.get("reportedTax"), "reportedTax", failures, false));

        String type = text(raw.get("transactionType"));
        if (type == null) {
            failures.add("transactionType is required and must be SALE, REFUND, or EXPENSE");
        } else {
            type = type.toUpperCase(Locale.ROOT);
            if (!List.of("SALE", "REFUND", "EXPENSE").contains(type)) {
                failures.add("transactionType must be SALE, REFUND, or EXPENSE");
            } else {
                tx.setTransactionType(type);
            }
        }

        if (!failures.isEmpty()) {
            tx.setValidationStatus("FAILURE");
            tx.setFailureReasons(toArray(failures));
            tx.setComplianceStatus("NON_COMPLIANT");
            return tx;
        }

        tx.setValidationStatus("SUCCESS");
        BigDecimal expectedTax = tx.getAmount().multiply(tx.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        tx.setExpectedTax(expectedTax);
        if (tx.getReportedTax() == null) {
            tx.setTaxGap(null);
            tx.setComplianceStatus("NON_COMPLIANT");
        } else {
            BigDecimal gap = expectedTax.subtract(tx.getReportedTax()).setScale(2, RoundingMode.HALF_UP);
            tx.setTaxGap(gap);
            if (gap.compareTo(TOLERANCE) > 0) tx.setComplianceStatus("UNDERPAID");
            else if (gap.compareTo(TOLERANCE.negate()) < 0) tx.setComplianceStatus("OVERPAID");
            else tx.setComplianceStatus("COMPLIANT");
        }
        return tx;
    }

    private BigDecimal parseMoney(JsonNode node, String field, List<String> failures, boolean required) {
        if (node == null || node.isNull()) {
            if (required) failures.add(field + " is required and must be numeric");
            return null;
        }
        if (!node.isNumber()) {
            failures.add(field + " must be numeric");
            return null;
        }
        BigDecimal value = node.decimalValue();
        if (value.scale() > 2) value = value.setScale(2, RoundingMode.HALF_UP);
        if (value.precision() - value.scale() > 17) {
            failures.add(field + " exceeds the supported precision");
            return null;
        }
        if (value.signum() < 0) {
            failures.add(field + " must not be negative");
            return null;
        }
        if (required && value.signum() == 0) {
            failures.add("amount must be greater than zero");
            return null;
        }
        return value;
    }

    private BigDecimal parseRate(JsonNode node, List<String> failures) {
        if (node == null || node.isNull() || !node.isNumber()) {
            failures.add("taxRate is required and must be numeric");
            return null;
        }
        BigDecimal value = node.decimalValue();
        if (value.signum() < 0) {
            failures.add("taxRate must not be negative");
            return null;
        }
        value = value.setScale(Math.min(Math.max(value.scale(), 0), 6), RoundingMode.HALF_UP);
        if (value.precision() - value.scale() > 3) {
            failures.add("taxRate exceeds supported precision (maximum 999.999999)");
            return null;
        }
        return value;
    }

    private void runRules(FinancialTransaction tx, JsonNode raw) {
        int executed = 0;
        int generated = 0;
        for (ComplianceRule rule : rules.findByEnabledTrueOrderByIdAsc()) {
            executed++;
            String message = evaluate(rule, tx, raw);
            if (message != null) {
                exceptions.save(new ComplianceException(tx.getTransactionId(), tx.getCustomerId(),
                        rule.getRuleName(), rule.getSeverity(), message));
                generated++;
            }
        }
        auditEvents.save(new AuditEvent(tx.getTransactionId(), "RULE_EXECUTION",
                detail("activeRulesExecuted", executed, "exceptionsGenerated", generated)));
    }

    private String evaluate(ComplianceRule rule, FinancialTransaction tx, JsonNode raw) {
        JsonNode config = rule.getConfiguration();
        return switch (rule.getRuleType()) {
            case "HIGH_VALUE" -> {
                if (tx.getAmount() == null) yield null;
                BigDecimal threshold = config.path("threshold").decimalValue();
                yield tx.getAmount().compareTo(threshold) > 0
                        ? "Transaction amount " + tx.getAmount() + " exceeds configured threshold " + threshold
                        : null;
            }
            case "REFUND_LIMIT" -> evaluateRefund(tx);
            case "GST_SLAB" -> evaluateGstSlab(rule, tx);
            default -> null;
        };
    }

    private String evaluateRefund(FinancialTransaction tx) {
        if (!"REFUND".equals(tx.getTransactionType())) return null;
        if (isBlank(tx.getOriginalTransactionId())) {
            return "Refund must include originalTransactionId referencing the original sale";
        }
        FinancialTransaction sale = transactions.findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
                tx.getOriginalTransactionId(), "SALE", "SUCCESS").orElse(null);
        if (sale == null) return "Original sale transaction " + tx.getOriginalTransactionId() + " was not found";
        if (!sale.getCustomerId().equals(tx.getCustomerId())) return "Refund customerId must match the original sale customerId";
        if (tx.getAmount().compareTo(sale.getAmount()) > 0) {
            return "Refund amount " + tx.getAmount() + " exceeds original sale amount " + sale.getAmount();
        }
        return null;
    }

    private String evaluateGstSlab(ComplianceRule rule, FinancialTransaction tx) {
        if (tx.getAmount() == null || tx.getTaxRate() == null) return null;
        JsonNode slabs = rule.getConfiguration().path("slabs");
        BigDecimal requiredRate = null;
        BigDecimal matchedThreshold = null;
        for (JsonNode slab : slabs) {
            BigDecimal threshold = slab.path("threshold").decimalValue();
            if (tx.getAmount().compareTo(threshold) > 0
                    && (matchedThreshold == null || threshold.compareTo(matchedThreshold) > 0)) {
                matchedThreshold = threshold;
                requiredRate = slab.path("requiredRate").decimalValue();
            }
        }
        if (requiredRate != null && tx.getTaxRate().compareTo(requiredRate) < 0) {
            return "Tax rate " + tx.getTaxRate() + " is below required rate " + requiredRate
                    + " for amount slab starting at " + matchedThreshold;
        }
        return null;
    }

    private FinancialTransaction fail(FinancialTransaction tx, List<String> failures, String reason) {
        failures.add(reason);
        tx.setValidationStatus("FAILURE");
        tx.setFailureReasons(toArray(failures));
        tx.setComplianceStatus("NON_COMPLIANT");
        return tx;
    }

    private JsonNode taxDetail(FinancialTransaction tx) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("amount", tx.getAmount());
        node.put("taxRate", tx.getTaxRate());
        node.put("expectedTax", tx.getExpectedTax());
        if (tx.getReportedTax() != null) node.put("reportedTax", tx.getReportedTax());
        else node.putNull("reportedTax");
        if (tx.getTaxGap() != null) node.put("taxGap", tx.getTaxGap());
        else node.putNull("taxGap");
        node.put("complianceStatus", tx.getComplianceStatus());
        return node;
    }

    private ObjectNode detail(String key1, String value1, String key2, boolean value2) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(key1, value1);
        node.put(key2, value2);
        return node;
    }

    private ObjectNode detail(String key1, int value1, String key2, int value2) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(key1, value1);
        node.put(key2, value2);
        return node;
    }

    private ArrayNode toArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private List<String> jsonStrings(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.asText().trim() : null;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
