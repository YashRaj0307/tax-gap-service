
package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.AnalysisStatus;
import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxGapAnalysisRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TaxGapAnalysisService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("5.00");

    private final TaxpayerRepository taxpayerRepository;
    private final ReportedIncomeRepository reportedIncomeRepository;
    private final TaxGapAnalysisRepository taxGapAnalysisRepository;

    public TaxGapAnalysisService(
            TaxpayerRepository taxpayerRepository,
            ReportedIncomeRepository reportedIncomeRepository,
            TaxGapAnalysisRepository taxGapAnalysisRepository) {
        this.taxpayerRepository = taxpayerRepository;
        this.reportedIncomeRepository = reportedIncomeRepository;
        this.taxGapAnalysisRepository = taxGapAnalysisRepository;
    }

    @Transactional
    public TaxGapAnalysis analyze(Long taxpayerId, Integer taxYear) {

        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Taxpayer not found: " + taxpayerId));

        if (!taxpayer.getTaxYear().equals(taxYear)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tax year does not match the taxpayer's registered tax year");
        }

        BigDecimal declaredIncome = taxpayer.getDeclaredIncome();

        BigDecimal reportedIncome = reportedIncomeRepository
                .findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .stream()
                .map(income -> income.getReportedIncome())
                .reduce(ZERO, BigDecimal::add);

        BigDecimal incomeDiscrepancy = reportedIncome
                .subtract(declaredIncome)
                .max(ZERO);

        BigDecimal discrepancyPercentage = null;

        if (declaredIncome.compareTo(ZERO) > 0) {
            discrepancyPercentage = incomeDiscrepancy
                    .multiply(new BigDecimal("100"))
                    .divide(declaredIncome, 4, RoundingMode.HALF_UP);
        }

        AnalysisStatus status = incomeDiscrepancy.compareTo(ZERO) > 0
                ? AnalysisStatus.REVIEW_REQUIRED
                : AnalysisStatus.NO_DISCREPANCY;

        String reason = buildReason(
                declaredIncome,
                reportedIncome,
                incomeDiscrepancy,
                discrepancyPercentage);

        TaxGapAnalysis analysis = new TaxGapAnalysis(
                taxpayer,
                taxYear,
                declaredIncome,
                reportedIncome,
                incomeDiscrepancy,
                discrepancyPercentage,
                status,
                reason);

        return taxGapAnalysisRepository.saveAndFlush(analysis);
    }

    @Transactional(readOnly = true)
    public List<TaxGapAnalysis> findByTaxpayerAndYear(
            Long taxpayerId,
            Integer taxYear) {

        if (!taxpayerRepository.existsById(taxpayerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Taxpayer not found: " + taxpayerId);
        }

        return taxGapAnalysisRepository
                .findByTaxpayerIdAndTaxYear(taxpayerId, taxYear);
    }

    private String buildReason(
            BigDecimal declaredIncome,
            BigDecimal reportedIncome,
            BigDecimal discrepancy,
            BigDecimal percentage) {

        if (discrepancy.compareTo(ZERO) == 0) {
            return "Reported income does not exceed declared income.";
        }

        if (percentage == null) {
            return "Reported income is positive while declared income is zero; percentage is undefined.";
        }

        if (percentage.compareTo(REVIEW_THRESHOLD) >= 0) {
            return "Reported income exceeds declared income by "
                    + discrepancy.toPlainString()
                    + ", representing "
                    + percentage.toPlainString()
                    + "%; review required.";
        }

        return "A positive income discrepancy of "
                + discrepancy.toPlainString()
                + " was detected; review required.";
    }
}
