package com.avega.tax_gap_service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record ComplianceRuleResponse(Long id, String ruleName, String ruleType,
                                     boolean enabled, String severity,
                                     JsonNode configuration, OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt) {}
