package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.domain.AuditEvent;
import com.avega.tax_gap_service.repository.AuditEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {
    private final AuditEventRepository repository;
    public AuditEventController(AuditEventRepository repository) { this.repository = repository; }

    @GetMapping("/transactions/{transactionId}")
    public List<AuditEvent> findByTransaction(@PathVariable String transactionId) {
        List<AuditEvent> events = repository.findByTransactionIdOrderByTimestampAsc(transactionId);
        if (events.isEmpty()) throw new EntityNotFoundException("No audit events found for transaction: " + transactionId);
        return events;
    }
}
