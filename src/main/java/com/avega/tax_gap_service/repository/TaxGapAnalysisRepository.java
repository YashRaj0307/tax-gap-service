
package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxGapAnalysisRepository
        extends JpaRepository<TaxGapAnalysis, Long> {

    List<TaxGapAnalysis> findByTaxpayerIdAndTaxYear(
            Long taxpayerId,
            Integer taxYear
    );
}
