
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    void shouldRejectReportedIncomeForNonexistentTaxpayer() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> transactionTemplate.executeWithoutResult(status ->
                        jdbcTemplate.update(
                                """
                                INSERT INTO reported_income (
                                    taxpayer_id,
                                    reporting_source,
                                    source_reference,
                                    tax_year,
                                    reported_income
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """,
                                -999999L,
                                "FOREIGN_KEY_TEST",
                                "FK-TEST-001",
                                2026,
                                new BigDecimal("50000.00")
                        )
                )
        );
    }
}
