
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportedIncomeConstraintIntegrationTest {

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Autowired
    private ReportedIncomeRepository reportedIncomeRepository;

    @Test
    void shouldRejectDuplicateSourceReferenceAtDatabaseLevel() {
        Taxpayer taxpayer = new Taxpayer(
                "CONSTRAINT-INT-001",
                "Constraint Test Taxpayer",
                "CONSTRAINT-TAX-001",
                2026,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00")
        );

        Taxpayer savedTaxpayer = taxpayerRepository.saveAndFlush(taxpayer);

        ReportedIncome first = new ReportedIncome(
                savedTaxpayer,
                "EMPLOYER",
                "DUPLICATE-DB-001",
                2026,
                new BigDecimal("50000.00")
        );

        ReportedIncome savedFirst =
                reportedIncomeRepository.saveAndFlush(first);

        assertNotNull(savedFirst.getId());

        ReportedIncome duplicate = new ReportedIncome(
                savedTaxpayer,
                "EMPLOYER",
                "DUPLICATE-DB-001",
                2026,
                new BigDecimal("60000.00")
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> reportedIncomeRepository.saveAndFlush(duplicate)
        );
    }
}
