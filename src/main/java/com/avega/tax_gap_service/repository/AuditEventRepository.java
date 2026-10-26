package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByTransactionIdOrderByTimestampAsc(String transactionId);
}
