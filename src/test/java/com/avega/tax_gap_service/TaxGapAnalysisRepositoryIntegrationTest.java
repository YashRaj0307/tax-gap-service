
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.AnalysisStatus;
import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxGapAnalysisRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import com.avega.tax_gap_service.service.TaxGapAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaxGapAnalysisRepositoryIntegrationTest {

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Autowired
    private ReportedIncomeRepository reportedIncomeRepository;

    @Autowired
    private TaxGapAnalysisRepository taxGapAnalysisRepository;

    @Autowired
    private TaxGapAnalysisService taxGapAnalysisService;

    @Test
    void shouldRequireReviewWhenReportedIncomeExceedsDeclaredIncome() {
        Taxpayer taxpayer = createTaxpayer(
                "AN-INT-001", "AN-TAX-001", "100000.00");

        addReportedIncome(taxpayer, "AN-SOURCE-001", "125000.00");

        TaxGapAnalysis analysis = taxGapAnalysisService.analyze(
                taxpayer.getId(), 2026);

        assertEquals(new BigDecimal("125000.00"), analysis.getReportedIncome());
        assertEquals(new BigDecimal("25000.00"), analysis.getIncomeDiscrepancy());
        assertEquals(new BigDecimal("25.0000"), analysis.getDiscrepancyPercentage());
        assertEquals(AnalysisStatus.REVIEW_REQUIRED, analysis.getAnalysisStatus());
        assertNotNull(analysis.getAnalyzedAt());
    }

    @Test
    void shouldReportNoDiscrepancyWhenIncomesAreEqual() {
        Taxpayer taxpayer = createTaxpayer(
                "AN-INT-002", "AN-TAX-002", "100000.00");

        addReportedIncome(taxpayer, "AN-SOURCE-002", "100000.00");

        TaxGapAnalysis analysis = taxGapAnalysisService.analyze(
                taxpayer.getId(), 2026);

        assertEquals(new BigDecimal("0.00"), analysis.getIncomeDiscrepancy());
        assertEquals(new BigDecimal("0.0000"), analysis.getDiscrepancyPercentage());
        assertEquals(AnalysisStatus.NO_DISCREPANCY, analysis.getAnalysisStatus());
    }

    @Test
    void shouldAggregateMultipleReportedIncomeRecords() {
        Taxpayer taxpayer = createTaxpayer(
                "AN-INT-003", "AN-TAX-003", "100000.00");

        addReportedIncome(taxpayer, "AN-SOURCE-003A", "60000.00");
        addReportedIncome(taxpayer, "AN-SOURCE-003B", "50000.00");

        TaxGapAnalysis analysis = taxGapAnalysisService.analyze(
                taxpayer.getId(), 2026);

        assertEquals(new BigDecimal("110000.00"), analysis.getReportedIncome());
        assertEquals(new BigDecimal("10000.00"), analysis.getIncomeDiscrepancy());
        assertEquals(new BigDecimal("10.0000"), analysis.getDiscrepancyPercentage());
        assertEquals(AnalysisStatus.REVIEW_REQUIRED, analysis.getAnalysisStatus());
    }

    @Test
    void shouldLeavePercentageNullWhenDeclaredIncomeIsZero() {
        Taxpayer taxpayer = createTaxpayer(
                "AN-INT-004", "AN-TAX-004", "0.00");

        addReportedIncome(taxpayer, "AN-SOURCE-004", "5000.00");

        TaxGapAnalysis analysis = taxGapAnalysisService.analyze(
                taxpayer.getId(), 2026);

        assertEquals(new BigDecimal("5000.00"), analysis.getIncomeDiscrepancy());
        assertNull(analysis.getDiscrepancyPercentage());
        assertEquals(AnalysisStatus.REVIEW_REQUIRED, analysis.getAnalysisStatus());
    }

    @Test
    void shouldPersistAndRetrieveAnalysisByTaxpayerAndYear() {
        Taxpayer taxpayer = createTaxpayer(
                "AN-INT-005", "AN-TAX-005", "100000.00");

        addReportedIncome(taxpayer, "AN-SOURCE-005", "125000.00");

        TaxGapAnalysis saved = taxGapAnalysisService.analyze(
                taxpayer.getId(), 2026);

        List<TaxGapAnalysis> results =
                taxGapAnalysisService.findByTaxpayerAndYear(
                        taxpayer.getId(), 2026);

        assertEquals(1, results.size());
        assertEquals(saved.getId(), results.get(0).getId());
        assertEquals(
                AnalysisStatus.REVIEW_REQUIRED,
                results.get(0).getAnalysisStatus());

        TaxGapAnalysis persisted = taxGapAnalysisRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals(new BigDecimal("25000.00"), persisted.getIncomeDiscrepancy());
    }

    private Taxpayer createTaxpayer(
            String reference,
            String identifier,
            String declaredIncome) {

        Taxpayer taxpayer = new Taxpayer(
                reference,
                "Integration Test Taxpayer",
                identifier,
                2026,
                new BigDecimal(declaredIncome),
                new BigDecimal("0.00"));

        return taxpayerRepository.saveAndFlush(taxpayer);
    }

    private void addReportedIncome(
            Taxpayer taxpayer,
            String sourceReference,
            String amount) {

        ReportedIncome income = new ReportedIncome(
                taxpayer,
                "INTEGRATION_TEST",
                sourceReference,
                2026,
                new BigDecimal(amount));

        reportedIncomeRepository.saveAndFlush(income);
    }
}
