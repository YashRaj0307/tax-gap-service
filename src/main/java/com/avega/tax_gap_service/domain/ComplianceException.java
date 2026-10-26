package com.avega.tax_gap_service.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "compliance_exception")
public class ComplianceException {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
    @Column(name = "customer_id", length = 100)
    private String customerId;
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    @Column(nullable = false, length = 10)
    private String severity;
    @Column(nullable = false, columnDefinition = "text")
    private String message;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime timestamp;
    protected ComplianceException() {}
    public ComplianceException(String transactionId, String customerId, String ruleName, String severity, String message) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.message = message;
    }
    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getCustomerId() { return customerId; }
    public String getRuleName() { return ruleName; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
