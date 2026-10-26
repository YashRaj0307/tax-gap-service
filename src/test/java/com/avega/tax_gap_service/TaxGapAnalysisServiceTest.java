
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.AnalysisStatus;
import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxGapAnalysisRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import com.avega.tax_gap_service.service.TaxGapAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxGapAnalysisServiceTest {

    @Mock
    private TaxpayerRepository taxpayerRepository;

    @Mock
    private ReportedIncomeRepository reportedIncomeRepository;

    @Mock
    private TaxGapAnalysisRepository taxGapAnalysisRepository;

    private TaxGapAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new TaxGapAnalysisService(
                taxpayerRepository,
                reportedIncomeRepository,
                taxGapAnalysisRepository
        );
    }

    @Test
    void shouldFlagWhenReportedIncomeExceedsDeclaredIncome() {
        Taxpayer taxpayer = new Taxpayer(
                "TAX-2026-TEST",
                "Test Taxpayer",
                "TEST-ID-2026",
                2026,
                new BigDecimal("850000.00"),
                new BigDecimal("85000.00")
        );

        ReportedIncome reportedIncome = new ReportedIncome(
                taxpayer,
                "EMPLOYER",
                "EMP-TEST-001",
                2026,
                new BigDecimal("1000000.00")
        );

        when(taxpayerRepository.findById(1L))
                .thenReturn(Optional.of(taxpayer));

        when(reportedIncomeRepository
                .findByTaxpayerIdAndTaxYear(1L, 2026))
                .thenReturn(List.of(reportedIncome));

        when(taxGapAnalysisRepository.saveAndFlush(any(TaxGapAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaxGapAnalysis result = service.analyze(1L, 2026);

        assertEquals(
                new BigDecimal("150000.00"),
                result.getIncomeDiscrepancy()
        );

        assertEquals(
                new BigDecimal("17.6471"),
                result.getDiscrepancyPercentage()
        );

        assertEquals(
                AnalysisStatus.REVIEW_REQUIRED,
                result.getAnalysisStatus()
        );

        assertNotNull(result.getAnalysisReason());

        verify(taxGapAnalysisRepository).saveAndFlush(any(TaxGapAnalysis.class));
    }
    
@Test
void shouldReturnNoDiscrepancyWhenIncomesAreEqual() {
    Taxpayer taxpayer = new Taxpayer(
            "TAX-2026-EQUAL",
            "Equal Income Taxpayer",
            "EQUAL-ID-2026",
            2026,
            new BigDecimal("1000000.00"),
            new BigDecimal("100000.00")
    );

    ReportedIncome reportedIncome = new ReportedIncome(
            taxpayer,
            "EMPLOYER",
            "EMP-EQUAL-001",
            2026,
            new BigDecimal("1000000.00")
    );

    when(taxpayerRepository.findById(2L))
            .thenReturn(Optional.of(taxpayer));

    when(reportedIncomeRepository
            .findByTaxpayerIdAndTaxYear(2L, 2026))
            .thenReturn(List.of(reportedIncome));

    when(taxGapAnalysisRepository.saveAndFlush(any(TaxGapAnalysis.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    TaxGapAnalysis result = service.analyze(2L, 2026);

    assertEquals(
            new BigDecimal("0.00"),
            result.getIncomeDiscrepancy()
    );

    assertEquals(
            AnalysisStatus.NO_DISCREPANCY,
            result.getAnalysisStatus()
    );

    assertEquals(
            "Reported income does not exceed declared income.",
            result.getAnalysisReason()
    );
}

@Test
void shouldAggregateMultipleReportedIncomeRecords() {
    Taxpayer taxpayer = new Taxpayer(
            "TAX-2026-MULTI",
            "Multiple Sources Taxpayer",
            "MULTI-ID-2026",
            2026,
            new BigDecimal("850000.00"),
            new BigDecimal("85000.00")
    );

    List<ReportedIncome> incomeRecords = List.of(
            new ReportedIncome(
                    taxpayer, "EMPLOYER", "EMP-MULTI-001",
                    2026, new BigDecimal("600000.00")
            ),
            new ReportedIncome(
                    taxpayer, "BANK", "BANK-MULTI-001",
                    2026, new BigDecimal("400000.00")
            )
    );

    when(taxpayerRepository.findById(3L))
            .thenReturn(Optional.of(taxpayer));

    when(reportedIncomeRepository
            .findByTaxpayerIdAndTaxYear(3L, 2026))
            .thenReturn(incomeRecords);

    when(taxGapAnalysisRepository.saveAndFlush(any(TaxGapAnalysis.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    TaxGapAnalysis result = service.analyze(3L, 2026);

    assertEquals(
            new BigDecimal("1000000.00"),
            result.getReportedIncome()
    );

    assertEquals(
            new BigDecimal("150000.00"),
            result.getIncomeDiscrepancy()
    );

    assertEquals(
            AnalysisStatus.REVIEW_REQUIRED,
            result.getAnalysisStatus()
    );
}

}
