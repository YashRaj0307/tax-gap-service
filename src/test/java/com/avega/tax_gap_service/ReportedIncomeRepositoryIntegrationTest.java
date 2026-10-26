
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportedIncomeRepositoryIntegrationTest {

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Autowired
    private ReportedIncomeRepository reportedIncomeRepository;

    @Test
    void shouldPersistAndRetrieveReportedIncomeForTaxpayer() {
        Taxpayer taxpayer = new Taxpayer(
                "RI-INT-001",
                "Reported Income Test Taxpayer",
                "RI-TAX-001",
                2026,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00")
        );

        Taxpayer savedTaxpayer = taxpayerRepository.saveAndFlush(taxpayer);

        ReportedIncome reportedIncome = new ReportedIncome(
                savedTaxpayer,
                "EMPLOYER",
                "RI-SOURCE-001",
                2026,
                new BigDecimal("125000.00")
        );

        ReportedIncome savedIncome =
                reportedIncomeRepository.saveAndFlush(reportedIncome);

        assertNotNull(savedIncome.getId());
        assertNotNull(savedIncome.getReceivedAt());

        List<ReportedIncome> results =
                reportedIncomeRepository.findByTaxpayerIdAndTaxYear(
                        savedTaxpayer.getId(),
                        2026
                );

        assertEquals(1, results.size());
        assertEquals(
                new BigDecimal("125000.00"),
                results.get(0).getReportedIncome()
        );
        assertEquals(
                savedTaxpayer.getId(),
                results.get(0).getTaxpayer().getId()
        );

        assertTrue(
                reportedIncomeRepository.existsByReportingSourceAndSourceReference(
                        "EMPLOYER",
                        "RI-SOURCE-001"
                )
        );
    }
}
