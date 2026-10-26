package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.dto.CustomerTaxSummaryResponse;
import com.avega.tax_gap_service.dto.ExceptionSummaryResponse;
import com.avega.tax_gap_service.repository.ComplianceExceptionRepository;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaxGapReportService {
    private final FinancialTransactionRepository transactions;
    private final ComplianceExceptionRepository exceptions;
    public TaxGapReportService(FinancialTransactionRepository transactions, ComplianceExceptionRepository exceptions) {
        this.transactions = transactions;
        this.exceptions = exceptions;
    }

    @Transactional(readOnly = true)
    public CustomerTaxSummaryResponse customerTaxSummary(String customerId) {
        FinancialTransactionRepository.CustomerTaxSummaryProjection p = transactions.customerTaxSummary(customerId);
        if (p == null) throw new EntityNotFoundException("No transactions found for customer: " + customerId);
        return new CustomerTaxSummaryResponse(p.getCustomerId(), zero(p.getTotalAmount()),
                zero(p.getTotalReportedTax()), zero(p.getTotalExpectedTax()), zero(p.getTotalTaxGap()),
                p.getTotalTransactions() == null ? 0 : p.getTotalTransactions(),
                p.getNonCompliantTransactions() == null ? 0 : p.getNonCompliantTransactions(),
                zero(p.getComplianceScore()).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public ExceptionSummaryResponse exceptionSummary() {
        long total = exceptions.count();
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        bySeverity.put("HIGH", 0L); bySeverity.put("MEDIUM", 0L); bySeverity.put("LOW", 0L);
        for (Object[] row : exceptions.countBySeverity()) bySeverity.put((String) row[0], ((Number) row[1]).longValue());
        List<ExceptionSummaryResponse.CustomerExceptionCount> byCustomer = new ArrayList<>();
        for (Object[] row : exceptions.countByCustomer()) {
            byCustomer.add(new ExceptionSummaryResponse.CustomerExceptionCount((String) row[0], ((Number) row[1]).longValue()));
        }
        return new ExceptionSummaryResponse(total, bySeverity, byCustomer);
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
