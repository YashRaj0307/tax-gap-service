package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.ReportedIncome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportedIncomeRepository
        extends JpaRepository<ReportedIncome, Long> {

    List<ReportedIncome> findByTaxpayerIdAndTaxYear(
            Long taxpayerId,
            Integer taxYear);

    Page<ReportedIncome> findByTaxpayerIdAndTaxYear(
            Long taxpayerId,
            Integer taxYear,
            Pageable pageable);

    boolean existsByReportingSourceAndSourceReference(
            String reportingSource,
            String sourceReference);
}