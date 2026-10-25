
package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.service.ReportedIncomeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ReportedIncomeController {

    private final ReportedIncomeService reportedIncomeService;

    public ReportedIncomeController(
            ReportedIncomeService reportedIncomeService
    ) {
        this.reportedIncomeService = reportedIncomeService;
    }

    @PostMapping("/reported-income")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportedIncomeResponse create(
            @Valid @RequestBody CreateReportedIncomeRequest request
    ) {
        return reportedIncomeService.create(request);
    }

    @GetMapping("/taxpayers/{taxpayerId}/reported-income")
    public List<ReportedIncomeResponse> findByTaxpayerAndYear(
            @PathVariable @Positive Long taxpayerId,
            @RequestParam @Min(2000) @Max(2100) Integer taxYear
    ) {
        return reportedIncomeService.findByTaxpayerAndYear(
                taxpayerId,
                taxYear
        );
    }
}
