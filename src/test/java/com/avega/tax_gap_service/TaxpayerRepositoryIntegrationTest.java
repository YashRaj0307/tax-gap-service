
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaxpayerRepositoryIntegrationTest {

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Test
    void shouldPersistAndRetrieveTaxpayer() {
        Taxpayer taxpayer = new Taxpayer(
                "INT-TEST-001",
                "Integration Test Taxpayer",
                "INT-TAX-001",
                2026,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00")
        );

        Taxpayer savedTaxpayer = taxpayerRepository.saveAndFlush(taxpayer);

        assertTrue(savedTaxpayer.getId() != null);

        Taxpayer retrievedTaxpayer = taxpayerRepository
                .findById(savedTaxpayer.getId())
                .orElseThrow();

        assertEquals("INT-TEST-001", retrievedTaxpayer.getTaxpayerReference());
        assertEquals("Integration Test Taxpayer", retrievedTaxpayer.getLegalName());
        assertEquals(new BigDecimal("100000.00"), retrievedTaxpayer.getDeclaredIncome());
    }
}
