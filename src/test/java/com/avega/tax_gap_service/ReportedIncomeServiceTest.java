package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.IdempotencyRecord;
import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.repository.IdempotencyRecordRepository;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import com.avega.tax_gap_service.service.ReportedIncomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportedIncomeServiceTest {

    @Mock
    private ReportedIncomeRepository reportedIncomeRepository;

    @Mock
    private TaxpayerRepository taxpayerRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ReportedIncomeService service;

    @BeforeEach
    void setUp() {
        service = new ReportedIncomeService(
                reportedIncomeRepository,
                taxpayerRepository,
                idempotencyRecordRepository,
                objectMapper
        );
    }
    @Test
    void shouldReturnStoredResponseForSameIdempotencyKeyAndPayload() throws Exception {
    CreateReportedIncomeRequest request =
            new CreateReportedIncomeRequest(
                    1L,
                    "EMPLOYER",
                    "EMP-IDEM-REPLAY-001",
                    2026,
                    new BigDecimal("500000.00")
            );

    String key = "income-request-replay-001";

    // First request: claim the key and create the income.
    when(idempotencyRecordRepository.insertIfAbsent(
            eq(key), any(String.class)))
            .thenReturn(1, 0);

    Taxpayer taxpayer = new Taxpayer(
            "TAX-2026-REPLAY",
            "Replay Test Taxpayer",
            "REPLAY-ID-2026",
            2026,
            new BigDecimal("850000.00"),
            new BigDecimal("85000.00")
    );

    org.springframework.test.util.ReflectionTestUtils.setField(
            taxpayer, "id", 1L);

    when(taxpayerRepository.findById(1L))
            .thenReturn(Optional.of(taxpayer));

    when(reportedIncomeRepository
            .existsByReportingSourceAndSourceReference(
                    "EMPLOYER", "EMP-IDEM-REPLAY-001"))
            .thenReturn(false);

    ReportedIncome savedIncome =
            org.mockito.Mockito.mock(ReportedIncome.class);

    when(reportedIncomeRepository.saveAndFlush(any(ReportedIncome.class)))
            .thenReturn(savedIncome);

    when(savedIncome.getId()).thenReturn(10L);
    when(savedIncome.getTaxpayer()).thenReturn(taxpayer);
    when(savedIncome.getReportingSource()).thenReturn("EMPLOYER");
    when(savedIncome.getSourceReference())
            .thenReturn("EMP-IDEM-REPLAY-001");
    when(savedIncome.getTaxYear()).thenReturn(2026);
    when(savedIncome.getReportedIncome())
            .thenReturn(new BigDecimal("500000.00"));

    java.time.OffsetDateTime receivedAt =
            java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z");

    when(savedIncome.getReceivedAt()).thenReturn(receivedAt);

    ReportedIncomeResponse originalResponse =
            new ReportedIncomeResponse(
                    10L,
                    1L,
                    "EMPLOYER",
                    "EMP-IDEM-REPLAY-001",
                    2026,
                    new BigDecimal("500000.00"),
                    receivedAt
            );

    com.fasterxml.jackson.databind.JsonNode storedBody =
            org.mockito.Mockito.mock(
                    com.fasterxml.jackson.databind.JsonNode.class);

    when(objectMapper.valueToTree(any(ReportedIncomeResponse.class)))
            .thenReturn(storedBody);

    when(objectMapper.treeToValue(
            eq(storedBody), eq(ReportedIncomeResponse.class)))
            .thenReturn(originalResponse);

    // The first lookup returns the record created for this request.
    // The second lookup returns the completed record for the replay.
    IdempotencyRecord completedRecord =
            new IdempotencyRecord(key, "placeholder", 202);

    when(idempotencyRecordRepository.findByIdempotencyKey(key))
            .thenReturn(Optional.of(completedRecord));

    ReportedIncomeResponse firstResponse = service.create(request, key);

    // Mark the stored record as completed before replaying the request.
    ReflectionTestUtils.setField(
            completedRecord, "requestFingerprint",
            captureFingerprint(request));

    completedRecord.complete(201, storedBody, java.time.Instant.now());

    ReportedIncomeResponse replayedResponse = service.create(request, key);

    assertEquals(originalResponse, firstResponse);
    assertEquals(originalResponse, replayedResponse);

    verify(reportedIncomeRepository,
            org.mockito.Mockito.times(1))
            .saveAndFlush(any(ReportedIncome.class));

    verify(taxpayerRepository,
            org.mockito.Mockito.times(1))
            .findById(1L);
}

private String captureFingerprint(CreateReportedIncomeRequest request)
        throws Exception {
    String canonical = String.join(
            "\n",
            request.taxpayerId().toString(),
            request.reportingSource(),
            request.sourceReference(),
            request.taxYear().toString(),
            request.reportedIncome().stripTrailingZeros().toPlainString()
    );

    byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    return java.util.HexFormat.of().formatHex(hash);
}

    @Test
    void shouldReturnNotFoundWhenTaxpayerDoesNotExist() {
        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        999L,
                        "EMPLOYER",
                        "EMP-TEST-001",
                        2026,
                        new BigDecimal("500000.00")
                );

        when(taxpayerRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(reportedIncomeRepository, never())
                .saveAndFlush(any(ReportedIncome.class));
    }

    @Test
    void shouldRejectTaxYearMismatch() {
        Taxpayer taxpayer = new Taxpayer(
                "TAX-2026-TEST",
                "Test Taxpayer",
                "TEST-ID-2026",
                2026,
                new BigDecimal("850000.00"),
                new BigDecimal("85000.00")
        );

        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        1L,
                        "EMPLOYER",
                        "EMP-TEST-002",
                        2025,
                        new BigDecimal("500000.00")
                );

        when(taxpayerRepository.findById(1L))
                .thenReturn(Optional.of(taxpayer));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(reportedIncomeRepository, never())
                .saveAndFlush(any(ReportedIncome.class));
    }

    @Test
    void shouldRejectDuplicateSourceReference() {
        Taxpayer taxpayer = new Taxpayer(
                "TAX-2026-DUP",
                "Duplicate Test Taxpayer",
                "DUP-ID-2026",
                2026,
                new BigDecimal("850000.00"),
                new BigDecimal("85000.00")
        );

        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        1L,
                        "EMPLOYER",
                        "EMP-DUP-001",
                        2026,
                        new BigDecimal("500000.00")
                );

        when(taxpayerRepository.findById(1L))
                .thenReturn(Optional.of(taxpayer));

        when(reportedIncomeRepository
                .existsByReportingSourceAndSourceReference(
                        "EMPLOYER", "EMP-DUP-001"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(reportedIncomeRepository, never())
                .saveAndFlush(any(ReportedIncome.class));
    }

    @Test
    void shouldCreateIncomeWithoutIdempotencyKey() {
        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        1L,
                        "EMPLOYER",
                        "EMP-IDEM-001",
                        2026,
                        new BigDecimal("500000.00")
                );

        Taxpayer taxpayer = new Taxpayer(
                "TAX-2026-IDEM",
                "Idempotency Test Taxpayer",
                "IDEM-ID-2026",
                2026,
                new BigDecimal("850000.00"),
                new BigDecimal("85000.00")
        );

        // Assign an ID because this taxpayer is not persisted in this unit test.
        ReflectionTestUtils.setField(taxpayer, "id", 1L);

        when(taxpayerRepository.findById(1L))
                .thenReturn(Optional.of(taxpayer));

        when(reportedIncomeRepository
                .existsByReportingSourceAndSourceReference(
                        "EMPLOYER", "EMP-IDEM-001"))
                .thenReturn(false);

        ReportedIncome savedIncome =
                org.mockito.Mockito.mock(ReportedIncome.class);

        when(reportedIncomeRepository.saveAndFlush(any(ReportedIncome.class)))
                .thenReturn(savedIncome);

        when(savedIncome.getId()).thenReturn(10L);
        when(savedIncome.getTaxpayer()).thenReturn(taxpayer);
        when(savedIncome.getReportingSource()).thenReturn("EMPLOYER");
        when(savedIncome.getSourceReference()).thenReturn("EMP-IDEM-001");
        when(savedIncome.getTaxYear()).thenReturn(2026);
        when(savedIncome.getReportedIncome())
                .thenReturn(new BigDecimal("500000.00"));
        when(savedIncome.getReceivedAt()).thenReturn(
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z")
        );

        ReportedIncomeResponse response = service.create(request, null);

        assertEquals(10L, response.id());
        assertEquals(1L, response.taxpayerId());

        verify(idempotencyRecordRepository, never())
                .insertIfAbsent(any(), any());
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentPayload() {
        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        1L,
                        "EMPLOYER",
                        "EMP-IDEM-002",
                        2026,
                        new BigDecimal("500000.00")
                );

        String key = "income-request-002";
        String fingerprint = "different-fingerprint";

        when(idempotencyRecordRepository.insertIfAbsent(
                eq(key), any(String.class)))
                .thenReturn(0);

        IdempotencyRecord existing = new IdempotencyRecord(
                key,
                fingerprint,
                201
        );

        when(idempotencyRecordRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, key)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(taxpayerRepository, never()).findById(any());
    }

    @Test
    void shouldRejectRequestWhenExistingIdempotencyRecordIsIncomplete() {
        CreateReportedIncomeRequest request =
                new CreateReportedIncomeRequest(
                        1L,
                        "EMPLOYER",
                        "EMP-IDEM-003",
                        2026,
                        new BigDecimal("500000.00")
                );

        String key = "income-request-003";

        when(idempotencyRecordRepository.insertIfAbsent(
                eq(key), any(String.class)))
                .thenReturn(0);

        String canonical = String.join(
                "\n",
                request.taxpayerId().toString(),
                request.reportingSource(),
                request.sourceReference(),
                request.taxYear().toString(),
                request.reportedIncome()
                        .stripTrailingZeros()
                        .toPlainString()
        );

        String expectedFingerprint;

        try {
            byte[] hash = java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonical.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );

            expectedFingerprint =
                    java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }

        IdempotencyRecord matchingRecord = new IdempotencyRecord(
                key,
                expectedFingerprint,
                202
        );

        // The record has no completed timestamp or response body.
        when(idempotencyRecordRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(matchingRecord));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request, key)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(taxpayerRepository, never()).findById(any());
    }
}