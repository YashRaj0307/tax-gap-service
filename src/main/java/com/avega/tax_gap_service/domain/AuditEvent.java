package com.avega.tax_gap_service.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;
    @Column(name = "event_timestamp", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime timestamp;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode detailJson;
    protected AuditEvent() {}
    public AuditEvent(String transactionId, String eventType, JsonNode detailJson) {
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.detailJson = detailJson;
    }
    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getEventType() { return eventType; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public JsonNode getDetailJson() { return detailJson; }
}
