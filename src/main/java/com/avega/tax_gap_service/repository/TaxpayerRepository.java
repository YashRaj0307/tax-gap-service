package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.Taxpayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxpayerRepository extends JpaRepository<Taxpayer, Long> {

    Optional<Taxpayer> findByTaxpayerReference(String taxpayerReference);

    boolean existsByTaxIdentifier(String taxIdentifier);
}