package com.avega.tax_gap_service.dto;

import java.util.List;
import java.util.Map;

public record ExceptionSummaryResponse(long totalExceptions, Map<String, Long> countBySeverity,
                                       List<CustomerExceptionCount> customerWiseCounts) {
    public record CustomerExceptionCount(String customerId, long exceptionCount) {}
}
