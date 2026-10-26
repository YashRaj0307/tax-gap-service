
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.domain.ReportedIncome;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.repository.ReportedIncomeRepository;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import com.avega.tax_gap_service.service.ReportedIncomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportedIncomeServiceTest {

    @Mock
    private ReportedIncomeRepository reportedIncomeRepository;

    @Mock
    private TaxpayerRepository taxpayerRepository;

    private ReportedIncomeService service;

    @BeforeEach
    void setUp() {
        service = new ReportedIncomeService(
                reportedIncomeRepository,
                taxpayerRepository
        );
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

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

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

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

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

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(reportedIncomeRepository, never())
                .saveAndFlush(any(ReportedIncome.class));
    }
}
