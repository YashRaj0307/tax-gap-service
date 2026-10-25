
package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.ReportedIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportedIncomeRepository
        extends JpaRepository<ReportedIncome, Long> {

    List<ReportedIncome> findByTaxpayerIdAndTaxYear(
            Long taxpayerId,
            Integer taxYear
    );

    boolean existsByReportingSourceAndSourceReference(
            String reportingSource,
            String sourceReference
    );
}
