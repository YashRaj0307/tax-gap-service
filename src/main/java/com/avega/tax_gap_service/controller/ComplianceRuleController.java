package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.ComplianceRuleResponse;
import com.avega.tax_gap_service.service.ComplianceRuleService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rules")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class ComplianceRuleController {
    private final ComplianceRuleService service;
    public ComplianceRuleController(ComplianceRuleService service) { this.service = service; }

    @GetMapping
    public List<ComplianceRuleResponse> findAll() { return service.findAll(); }

    @PatchMapping("/{id}/enabled")
    public ComplianceRuleResponse setEnabled(@PathVariable Long id, @RequestParam @NotNull Boolean enabled) {
        return service.setEnabled(id, enabled);
    }

    @PutMapping("/{id}/configuration")
    public ComplianceRuleResponse updateConfiguration(@PathVariable Long id, @RequestBody JsonNode configuration) {
        return service.updateConfiguration(id, configuration);
    }
}
