
package com.avega.tax_gap_service.dto;

import com.avega.tax_gap_service.domain.AnalysisStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TaxGapAnalysisResponse(
        Long id,
        Long taxpayerId,
        Integer taxYear,
        BigDecimal declaredIncome,
        BigDecimal reportedIncome,
        BigDecimal incomeDiscrepancy,
        BigDecimal discrepancyPercentage,
        AnalysisStatus analysisStatus,
        String analysisReason,
        OffsetDateTime analyzedAt
) {
}
