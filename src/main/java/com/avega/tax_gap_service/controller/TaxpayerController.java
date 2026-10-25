package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.CreateTaxpayerRequest;
import com.avega.tax_gap_service.dto.TaxpayerResponse;
import com.avega.tax_gap_service.service.TaxpayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxpayers")
public class TaxpayerController {

    private final TaxpayerService taxpayerService;

    public TaxpayerController(TaxpayerService taxpayerService) {
        this.taxpayerService = taxpayerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxpayerResponse create(
            @Valid @RequestBody CreateTaxpayerRequest request
    ) {
        return taxpayerService.create(request);
    }

    @GetMapping
    public List<TaxpayerResponse> findAll() {
        return taxpayerService.findAll();
    }
}