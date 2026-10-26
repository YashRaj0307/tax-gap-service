
package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.IdempotencyRecord;
import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.repository.IdempotencyRecordRepository;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class ReportedIncomeService {

    private final ReportedIncomeRepository reportedIncomeRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public ReportedIncomeService(
            ReportedIncomeRepository reportedIncomeRepository,
            TaxpayerRepository taxpayerRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper) {
        this.reportedIncomeRepository = reportedIncomeRepository;
        this.taxpayerRepository = taxpayerRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReportedIncomeResponse create(
            CreateReportedIncomeRequest request) {
        return create(request, null);
    }

    @Transactional
    public ReportedIncomeResponse create(
            CreateReportedIncomeRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createReportedIncome(request);
        }

        if (idempotencyKey.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must not exceed 100 characters");
        }

        String fingerprint = fingerprint(request);

        int inserted = idempotencyRecordRepository.insertIfAbsent(
                idempotencyKey,
                fingerprint);

        if (inserted == 0) {
            IdempotencyRecord existing = idempotencyRecordRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Idempotency record is not available; retry the request"));

            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key was already used with a different request");
            }

            if (existing.getCompletedAt() == null
                    || existing.getResponseBody() == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A request with this Idempotency-Key is still in progress");
            }

            try {
                return objectMapper.treeToValue(
                        existing.getResponseBody(),
                        ReportedIncomeResponse.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(
                        "Could not read the stored idempotency response",
                        exception);
            }
        }

        ReportedIncomeResponse response = createReportedIncome(request);

        try {
            JsonNode responseBody = objectMapper.valueToTree(response);

            IdempotencyRecord record = idempotencyRecordRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency record disappeared during processing"));

            record.complete(201, responseBody, Instant.now());
            idempotencyRecordRepository.saveAndFlush(record);

            return response;
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not persist the idempotency response",
                    exception);
        }
    }

    private ReportedIncomeResponse createReportedIncome(
            CreateReportedIncomeRequest request) {

        Taxpayer taxpayer = taxpayerRepository.findById(request.taxpayerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Taxpayer not found: " + request.taxpayerId()));

        if (!taxpayer.getTaxYear().equals(request.taxYear())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reported income tax year must match the taxpayer's tax year");
        }

        boolean duplicate = reportedIncomeRepository
                .existsByReportingSourceAndSourceReference(
                        request.reportingSource(),
                        request.sourceReference());

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This source reference has already been ingested");
        }

        ReportedIncome reportedIncome = new ReportedIncome(
                taxpayer,
                request.reportingSource(),
                request.sourceReference(),
                request.taxYear(),
                request.reportedIncome());

        try {
            ReportedIncome saved =
                    reportedIncomeRepository.saveAndFlush(reportedIncome);

            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A duplicate source reference or database constraint violation occurred",
                    exception);
        }
    }

    private String fingerprint(CreateReportedIncomeRequest request) {
        String canonicalRequest = String.join(
                "\n",
                request.taxpayerId().toString(),
                request.reportingSource(),
                request.sourceReference(),
                request.taxYear().toString(),
                request.reportedIncome().stripTrailingZeros().toPlainString());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ReportedIncomeResponse> findByTaxpayerAndYear(
            Long taxpayerId,
            Integer taxYear) {
        if (!taxpayerRepository.existsById(taxpayerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Taxpayer not found: " + taxpayerId);
        }

        return reportedIncomeRepository
                .findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ReportedIncomeResponse> findByTaxpayerAndYear(
            Long taxpayerId,
            Integer taxYear,
            Pageable pageable) {
        if (!taxpayerRepository.existsById(taxpayerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Taxpayer not found: " + taxpayerId);
        }

        return reportedIncomeRepository
                .findByTaxpayerIdAndTaxYear(
                        taxpayerId, taxYear, pageable)
                .map(this::toResponse);
    }

    private ReportedIncomeResponse toResponse(
            ReportedIncome reportedIncome) {
        return new ReportedIncomeResponse(
                reportedIncome.getId(),
                reportedIncome.getTaxpayer().getId(),
                reportedIncome.getReportingSource(),
                reportedIncome.getSourceReference(),
                reportedIncome.getTaxYear(),
                reportedIncome.getReportedIncome(),
                reportedIncome.getReceivedAt());
    }
}
