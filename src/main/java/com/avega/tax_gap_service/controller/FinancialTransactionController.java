
package com.avega.tax_gap_service.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.avega.tax_gap_service.domain.FinancialTransaction;
import com.avega.tax_gap_service.dto.FinancialTransactionBatchResponse;
import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.avega.tax_gap_service.service.FinancialTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/financial-transactions")
public class FinancialTransactionController {

    private final ObjectMapper objectMapper;
    private final FinancialTransactionService service;
    private final FinancialTransactionRepository repository;

    public FinancialTransactionController(
            FinancialTransactionService service,
            FinancialTransactionRepository repository,
            ObjectMapper objectMapper) {
        this.service = service;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<FinancialTransaction> find(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String complianceStatus,
            @RequestParam(required = false) String validationStatus) {
        return repository.search(
                blankToNull(customerId),
                normalize(complianceStatus),
                normalize(validationStatus));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase(Locale.ROOT);
    }

    @PostMapping("/batch")
    public ResponseEntity<FinancialTransactionBatchResponse> ingestBatch(
            @RequestBody JsonNode requestBody)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        if (requestBody == null || !requestBody.isArray()) {
            throw new IllegalArgumentException(
                    "Request body must be a JSON array of transactions");
        }

        com.fasterxml.jackson.databind.JsonNode jackson2Body =
                objectMapper.readTree(requestBody.toString());

        List<com.fasterxml.jackson.databind.JsonNode> transactions =
                new ArrayList<>();

        jackson2Body.forEach(transactions::add);

        FinancialTransactionBatchResponse response =
                service.ingestBatch(transactions);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
