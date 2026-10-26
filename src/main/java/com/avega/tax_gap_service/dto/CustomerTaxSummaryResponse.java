package com.avega.tax_gap_service.dto;

import java.math.BigDecimal;

public record CustomerTaxSummaryResponse(String customerId, BigDecimal totalAmount,
                                         BigDecimal totalReportedTax, BigDecimal totalExpectedTax,
                                         BigDecimal totalTaxGap, long totalTransactions,
                                         long nonCompliantTransactions, BigDecimal complianceScore) {}
