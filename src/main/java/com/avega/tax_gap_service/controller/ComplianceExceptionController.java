package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.domain.ComplianceException;
import com.avega.tax_gap_service.dto.ComplianceExceptionResponse;
import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/exceptions")
public class ComplianceExceptionController {
    private static final Set<String> SEVERITIES = Set.of("HIGH", "MEDIUM", "LOW");
    private final ComplianceExceptionRepository repository;
    public ComplianceExceptionController(ComplianceExceptionRepository repository) { this.repository = repository; }

    @GetMapping
    public List<ComplianceExceptionResponse> find(@RequestParam(required = false) String customerId,
                                                  @RequestParam(required = false) String severity,
                                                  @RequestParam(required = false) String ruleName) {
        String normalizedSeverity = severity == null ? null : severity.toUpperCase(java.util.Locale.ROOT);
        if (normalizedSeverity != null && !SEVERITIES.contains(normalizedSeverity)) {
            throw new IllegalArgumentException("severity must be HIGH, MEDIUM, or LOW");
        }
        return repository.search(blankToNull(customerId), normalizedSeverity, blankToNull(ruleName))
                .stream().map(this::toResponse).toList();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ComplianceExceptionResponse toResponse(ComplianceException e) {
        return new ComplianceExceptionResponse(e.getId(), e.getTransactionId(), e.getCustomerId(),
                e.getRuleName(), e.getSeverity(), e.getMessage(), e.getTimestamp());
    }
}
