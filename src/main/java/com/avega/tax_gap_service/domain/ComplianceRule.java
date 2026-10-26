package com.avega.tax_gap_service.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "compliance_rule")
public class ComplianceRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_name", nullable = false, unique = true, length = 100)
    private String ruleName;
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;
    @Column(nullable = false)
    private boolean enabled;
    @Column(nullable = false, length = 10)
    private String severity;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode configuration;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
    protected ComplianceRule() {}
    public ComplianceRule(String ruleName, String ruleType, boolean enabled,
                          String severity, JsonNode configuration) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.enabled = enabled;
        this.severity = severity;
        this.configuration = configuration;
    }
    public Long getId() { return id; }
    public String getRuleName() { return ruleName; }
    public String getRuleType() { return ruleType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSeverity() { return severity; }
    public JsonNode getConfiguration() { return configuration; }
    public void setConfiguration(JsonNode configuration) { this.configuration = configuration; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
