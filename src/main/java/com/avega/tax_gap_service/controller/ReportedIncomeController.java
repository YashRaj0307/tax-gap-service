
package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.service.ReportedIncomeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ReportedIncomeController {

    private final ReportedIncomeService reportedIncomeService;

    public ReportedIncomeController(
            ReportedIncomeService reportedIncomeService) {
        this.reportedIncomeService = reportedIncomeService;
    }

    @PostMapping("/reported-income")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportedIncomeResponse create(
            @Valid @RequestBody CreateReportedIncomeRequest request) {
        return reportedIncomeService.create(request);
    }

    @GetMapping("/taxpayers/{taxpayerId}/reported-income")
    public List<ReportedIncomeResponse> findByTaxpayerAndYear(
            @PathVariable @Positive Long taxpayerId,
            @RequestParam @Min(2000) @Max(2100) Integer taxYear) {
        return reportedIncomeService.findByTaxpayerAndYear(
                taxpayerId, taxYear);
    }

    @GetMapping("/taxpayers/{taxpayerId}/reported-income/paged")
    public Page<ReportedIncomeResponse> findByTaxpayerAndYearPaged(
            @PathVariable @Positive Long taxpayerId,
            @RequestParam @Min(2000) @Max(2100) Integer taxYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Size must be between 1 and 100");
        }

        if (!sortBy.equals("id")
                && !sortBy.equals("taxYear")
                && !sortBy.equals("reportedIncome")
                && !sortBy.equals("receivedAt")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sort field");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortDirection, sortBy));

        return reportedIncomeService.findByTaxpayerAndYear(
                taxpayerId, taxYear, pageable);
    }
}