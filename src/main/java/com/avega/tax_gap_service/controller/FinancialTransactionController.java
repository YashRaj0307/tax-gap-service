
package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.FinancialTransactionBatchResponse;
import com.avega.tax_gap_service.service.FinancialTransactionService;
import com.avega.tax_gap_service.domain.FinancialTransaction;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/financial-transactions")
public class FinancialTransactionController {

    private final FinancialTransactionService service;
    private final FinancialTransactionRepository repository;

    public FinancialTransactionController(
            FinancialTransactionService service,
            FinancialTransactionRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @org.springframework.web.bind.annotation.GetMapping
    public List<FinancialTransaction> find(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String customerId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String complianceStatus,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String validationStatus) {
        return repository.search(blankToNull(customerId), normalize(complianceStatus), normalize(validationStatus));
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT); }

    @PostMapping("/batch")
    public ResponseEntity<FinancialTransactionBatchResponse> ingestBatch(
            @RequestBody JsonNode requestBody) {

        if (requestBody == null || !requestBody.isArray()) {
            throw new IllegalArgumentException(
                    "Request body must be a JSON array of transactions");
        }

        List<JsonNode> transactions = new ArrayList<>();
        requestBody.forEach(transactions::add);

        FinancialTransactionBatchResponse response =
                service.ingestBatch(transactions);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
