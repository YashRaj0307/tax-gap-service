
package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import com.avega.tax_gap_service.dto.TaxGapAnalysisResponse;
import com.avega.tax_gap_service.service.TaxGapAnalysisService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-gap-analyses")
@Validated
public class TaxGapAnalysisController {

    private final TaxGapAnalysisService taxGapAnalysisService;

    public TaxGapAnalysisController(
            TaxGapAnalysisService taxGapAnalysisService) {
        this.taxGapAnalysisService = taxGapAnalysisService;
    }

    @PostMapping("/taxpayers/{taxpayerId}")
    public TaxGapAnalysisResponse analyze(
            @PathVariable @Positive Long taxpayerId,
            @RequestParam @Min(2000) @Max(2100) Integer taxYear) {

        TaxGapAnalysis analysis =
                taxGapAnalysisService.analyze(taxpayerId, taxYear);

        return toResponse(analysis);
    }

    @GetMapping("/taxpayers/{taxpayerId}")
    public List<TaxGapAnalysisResponse> findAnalyses(
            @PathVariable @Positive Long taxpayerId,
            @RequestParam @Min(2000) @Max(2100) Integer taxYear) {

        return taxGapAnalysisService
                .findByTaxpayerAndYear(taxpayerId, taxYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TaxGapAnalysisResponse toResponse(TaxGapAnalysis analysis) {
        return new TaxGapAnalysisResponse(
                analysis.getId(),
                analysis.getTaxpayer().getId(),
                analysis.getTaxYear(),
                analysis.getDeclaredIncome(),
                analysis.getReportedIncome(),
                analysis.getIncomeDiscrepancy(),
                analysis.getDiscrepancyPercentage(),
                analysis.getAnalysisStatus(),
                analysis.getAnalysisReason(),
                analysis.getAnalyzedAt()
        );
    }
}
