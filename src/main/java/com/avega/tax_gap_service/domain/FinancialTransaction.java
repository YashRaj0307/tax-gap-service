package com.avega.tax_gap_service.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "financial_transaction")
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_rate", precision = 9, scale = 6)
    private BigDecimal taxRate;

    @Column(name = "reported_tax", precision = 19, scale = 2)
    private BigDecimal reportedTax;

    @Column(name = "transaction_type", length = 20)
    private String transactionType;

    @Column(name = "original_transaction_id", length = 100)
    private String originalTransactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_transaction", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawTransaction;

    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failure_reasons", columnDefinition = "jsonb")
    private JsonNode failureReasons;

    @Column(name = "expected_tax", precision = 19, scale = 2)
    private BigDecimal expectedTax;

    @Column(name = "tax_gap", precision = 19, scale = 2)
    private BigDecimal taxGap;

    @Column(name = "compliance_status", length = 30)
    private String complianceStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public FinancialTransaction() {
    }

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getReportedTax() { return reportedTax; }
    public void setReportedTax(BigDecimal reportedTax) { this.reportedTax = reportedTax; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }
    public JsonNode getRawTransaction() { return rawTransaction; }
    public void setRawTransaction(JsonNode rawTransaction) { this.rawTransaction = rawTransaction; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public JsonNode getFailureReasons() { return failureReasons; }
    public void setFailureReasons(JsonNode failureReasons) { this.failureReasons = failureReasons; }
    public BigDecimal getExpectedTax() { return expectedTax; }
    public void setExpectedTax(BigDecimal expectedTax) { this.expectedTax = expectedTax; }
    public BigDecimal getTaxGap() { return taxGap; }
    public void setTaxGap(BigDecimal taxGap) { this.taxGap = taxGap; }
    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
