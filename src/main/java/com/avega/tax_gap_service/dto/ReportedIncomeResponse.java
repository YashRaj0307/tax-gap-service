
package com.avega.tax_gap_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReportedIncomeResponse(
        Long id,
        Long taxpayerId,
        String reportingSource,
        String sourceReference,
        Integer taxYear,
        BigDecimal reportedIncome,
        OffsetDateTime receivedAt
) {
}
