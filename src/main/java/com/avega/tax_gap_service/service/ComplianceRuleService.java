package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.ComplianceRule;
import com.avega.tax_gap_service.dto.ComplianceRuleResponse;
import com.avega.tax_gap_service.repository.ComplianceRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ComplianceRuleService {
    private final ComplianceRuleRepository repository;
    public ComplianceRuleService(ComplianceRuleRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<ComplianceRuleResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ComplianceRuleResponse setEnabled(Long id, boolean enabled) {
        ComplianceRule rule = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compliance rule not found: " + id));
        rule.setEnabled(enabled);
        return toResponse(repository.save(rule));
    }

    @Transactional
    public ComplianceRuleResponse updateConfiguration(Long id, JsonNode configuration) {
        if (configuration == null || !configuration.isObject()) {
            throw new IllegalArgumentException("Rule configuration must be a JSON object");
        }
        ComplianceRule rule = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compliance rule not found: " + id));
        rule.setConfiguration(configuration.deepCopy());
        return toResponse(repository.save(rule));
    }

    private ComplianceRuleResponse toResponse(ComplianceRule rule) {
        return new ComplianceRuleResponse(rule.getId(), rule.getRuleName(), rule.getRuleType(),
                rule.isEnabled(), rule.getSeverity(), rule.getConfiguration(), rule.getCreatedAt(), rule.getUpdatedAt());
    }
}
