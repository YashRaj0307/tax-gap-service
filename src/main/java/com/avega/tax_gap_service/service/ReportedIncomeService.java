
package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReportedIncomeService {

    private final ReportedIncomeRepository reportedIncomeRepository;
    private final TaxpayerRepository taxpayerRepository;

    public ReportedIncomeService(
            ReportedIncomeRepository reportedIncomeRepository,
            TaxpayerRepository taxpayerRepository
    ) {
        this.reportedIncomeRepository = reportedIncomeRepository;
        this.taxpayerRepository = taxpayerRepository;
    }

    @Transactional
    public ReportedIncomeResponse create(
            CreateReportedIncomeRequest request
    ) {
        Taxpayer taxpayer = taxpayerRepository.findById(request.taxpayerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Taxpayer not found: " + request.taxpayerId()
                ));

        if (!taxpayer.getTaxYear().equals(request.taxYear())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reported income tax year must match the taxpayer's tax year"
            );
        }

        boolean duplicate = reportedIncomeRepository
                .existsByReportingSourceAndSourceReference(
                        request.reportingSource(),
                        request.sourceReference()
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This source reference has already been ingested"
            );
        }

        ReportedIncome reportedIncome = new ReportedIncome(
                taxpayer,
                request.reportingSource(),
                request.sourceReference(),
                request.taxYear(),
                request.reportedIncome()
        );

        try {
            ReportedIncome saved =
                    reportedIncomeRepository.saveAndFlush(reportedIncome);

            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A duplicate source reference or database constraint violation occurred",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ReportedIncomeResponse> findByTaxpayerAndYear(
            Long taxpayerId,
            Integer taxYear
    ) {
        if (!taxpayerRepository.existsById(taxpayerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Taxpayer not found: " + taxpayerId
            );
        }

        return reportedIncomeRepository
                .findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReportedIncomeResponse toResponse(ReportedIncome reportedIncome) {
        return new ReportedIncomeResponse(
                reportedIncome.getId(),
                reportedIncome.getTaxpayer().getId(),
                reportedIncome.getReportingSource(),
                reportedIncome.getSourceReference(),
                reportedIncome.getTaxYear(),
                reportedIncome.getReportedIncome(),
                reportedIncome.getReceivedAt()
        );
    }
}
