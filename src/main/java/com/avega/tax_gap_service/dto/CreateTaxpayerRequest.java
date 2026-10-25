package com.avega.tax_gap_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTaxpayerRequest(
        @NotBlank
        @Size(max = 50)
        String taxpayerReference,

        @NotBlank
        @Size(max = 200)
        String legalName,

        @NotBlank
        @Size(max = 50)
        String taxIdentifier,

        @NotNull
        @Min(2000)
        @Max(2100)
        Integer taxYear,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal declaredIncome,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal declaredTax
) {
}