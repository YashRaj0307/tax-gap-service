
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackIntegrationTest {

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldRollbackTaxpayerInsertWhenTransactionFails() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        assertThrows(
                IllegalStateException.class,
                () -> transactionTemplate.executeWithoutResult(status -> {
                    Taxpayer taxpayer = new Taxpayer(
                            "ROLLBACK-INT-001",
                            "Rollback Test Taxpayer",
                            "ROLLBACK-TAX-001",
                            2026,
                            new BigDecimal("100000.00"),
                            new BigDecimal("10000.00")
                    );

                    taxpayerRepository.saveAndFlush(taxpayer);

                    throw new IllegalStateException(
                            "Simulated failure after taxpayer insert"
                    );
                })
        );

        Integer remainingRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM taxpayer
                WHERE taxpayer_reference = ?
                """,
                Integer.class,
                "ROLLBACK-INT-001"
        );

        assertEquals(0, remainingRows);
    }
}
