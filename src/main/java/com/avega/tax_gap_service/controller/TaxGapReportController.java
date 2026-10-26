package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.CustomerTaxSummaryResponse;
import com.avega.tax_gap_service.dto.ExceptionSummaryResponse;
import com.avega.tax_gap_service.service.TaxGapReportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class TaxGapReportController {
    private final TaxGapReportService service;
    public TaxGapReportController(TaxGapReportService service) { this.service = service; }

    @GetMapping("/customers/{customerId}/tax-summary")
    public CustomerTaxSummaryResponse customerTaxSummary(@PathVariable String customerId) {
        return service.customerTaxSummary(customerId);
    }

    @GetMapping("/exceptions/summary")
    public ExceptionSummaryResponse exceptionSummary() { return service.exceptionSummary(); }
}
