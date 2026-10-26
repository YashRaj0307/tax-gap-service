package com.avega.tax_gap_service.dto;

import java.time.OffsetDateTime;

public record ComplianceExceptionResponse(Long id, String transactionId, String customerId,
                                          String ruleName, String severity,
                                          String message, OffsetDateTime timestamp) {}
