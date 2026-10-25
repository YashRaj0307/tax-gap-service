package com.avega.tax_gap_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TaxpayerResponse(
        Long id,
        String taxpayerReference,
        String legalName,
        String taxIdentifier,
        Integer taxYear,
        BigDecimal declaredIncome,
        BigDecimal declaredTax,
        OffsetDateTime createdAt
) {
}