
package com.avega.tax_gap_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateReportedIncomeRequest(
        @NotNull
        @Positive
        Long taxpayerId,

        @NotBlank
        String reportingSource,

        @NotBlank
        String sourceReference,

        @NotNull
        @Min(2000)
        @Max(2100)
        Integer taxYear,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal reportedIncome
) {
}
