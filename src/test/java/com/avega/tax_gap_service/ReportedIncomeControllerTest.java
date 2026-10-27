
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.config.SecurityConfig;
import com.avega.tax_gap_service.controller.ReportedIncomeController;
import com.avega.tax_gap_service.dto.CreateReportedIncomeRequest;
import com.avega.tax_gap_service.dto.ReportedIncomeResponse;
import com.avega.tax_gap_service.service.ReportedIncomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avega.tax_gap_service.domain.AppUser;
import com.avega.tax_gap_service.repository.AppUserRepository;
import com.avega.tax_gap_service.support.TestAppUserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;

@WebMvcTest(ReportedIncomeController.class)
@Import(SecurityConfig.class)
class ReportedIncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportedIncomeService reportedIncomeService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void configureTestUser() throws ReflectiveOperationException {
    AppUser user = TestAppUserFactory.enabledAdmin(
            "devuser",
            passwordEncoder.encode("LocalDevOnly-ChangeMe-2026")
    );

    when(appUserRepository.findByUsername("devuser"))
            .thenReturn(java.util.Optional.of(user));
}

    @Test
    void createReportedIncomeWithValidRequestReturnsCreated()
            throws Exception {
        when(reportedIncomeService.create(
                any(CreateReportedIncomeRequest.class),
                any()))
                .thenReturn(new ReportedIncomeResponse(
                        1L,
                        10L,
                        "BANK",
                        "BANK-TXN-1001",
                        2026,
                        new BigDecimal("125000.00"),
                        OffsetDateTime.parse("2026-10-26T10:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/reported-income")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerId": 10,
                                  "reportingSource": "BANK",
                                  "sourceReference": "BANK-TXN-1001",
                                  "taxYear": 2026,
                                  "reportedIncome": 125000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxpayerId").value(10))
                .andExpect(jsonPath("$.reportingSource").value("BANK"))
                .andExpect(jsonPath("$.sourceReference")
                        .value("BANK-TXN-1001"))
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.reportedIncome").value(125000.00));
    }

    @Test
    void createReportedIncomeWithBlankSourceReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/v1/reported-income")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerId": 10,
                                  "reportingSource": " ",
                                  "sourceReference": "BANK-TXN-1002",
                                  "taxYear": 2026,
                                  "reportedIncome": 125000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/reported-income"));
    }

    @Test
    void createReportedIncomeWithoutAuthenticationReturnsUnauthorized()
            throws Exception {
        mockMvc.perform(post("/api/v1/reported-income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerId": 10,
                                  "reportingSource": "BANK",
                                  "sourceReference": "BANK-TXN-1003",
                                  "taxYear": 2026,
                                  "reportedIncome": 125000.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findReportedIncomeByTaxpayerAndYearReturnsList()
            throws Exception {
        when(reportedIncomeService.findByTaxpayerAndYear(10L, 2026))
                .thenReturn(List.of(
                        new ReportedIncomeResponse(
                                1L,
                                10L,
                                "BANK",
                                "BANK-TXN-1001",
                                2026,
                                new BigDecimal("125000.00"),
                                OffsetDateTime.parse("2026-10-26T10:00:00Z")
                        )
                ));

        mockMvc.perform(get("/api/v1/taxpayers/10/reported-income")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].taxpayerId").value(10))
                .andExpect(jsonPath("$[0].reportingSource").value("BANK"))
                .andExpect(jsonPath("$[0].taxYear").value(2026));
    }

    @Test
    void findReportedIncomeWithInvalidTaxpayerIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers/0/reported-income")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findReportedIncomePagedReturnsPageMetadataAndRecords()
            throws Exception {
        ReportedIncomeResponse income = new ReportedIncomeResponse(
                1L,
                10L,
                "BANK",
                "BANK-TXN-1001",
                2026,
                new BigDecimal("125000.00"),
                OffsetDateTime.parse("2026-10-26T10:00:00Z")
        );

        Page<ReportedIncomeResponse> result = new PageImpl<>(
                List.of(income),
                PageRequest.of(
                        0, 20, Sort.by("receivedAt").descending()),
                1
        );

        when(reportedIncomeService.findByTaxpayerAndYear(
                eq(10L), eq(2026), any(Pageable.class)))
                .thenReturn(result);

        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].taxpayerId").value(10))
                .andExpect(jsonPath("$.content[0].reportingSource")
                        .value("BANK"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void findReportedIncomePagedWithCustomSortingReturnsOk()
            throws Exception {
        when(reportedIncomeService.findByTaxpayerAndYear(
                eq(10L), eq(2026), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "reportedIncome")
                        .param("direction", "asc")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isOk());

        verify(reportedIncomeService).findByTaxpayerAndYear(
                eq(10L),
                eq(2026),
                eq(PageRequest.of(
                        1, 10, Sort.by(
                                Sort.Direction.ASC, "reportedIncome")))
        );
    }

    @Test
    void findReportedIncomePagedWithNegativePageReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026")
                        .param("page", "-1")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findReportedIncomePagedWithOversizedPageReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026")
                        .param("size", "101")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findReportedIncomePagedWithUnsupportedSortFieldReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026")
                        .param("sortBy", "sourceReference")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findReportedIncomePagedWithInvalidDirectionReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026")
                        .param("direction", "sideways")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findReportedIncomePagedWithoutAuthenticationReturnsUnauthorized()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/taxpayers/10/reported-income/paged")
                        .param("taxYear", "2026"))
                .andExpect(status().isUnauthorized());
    }
}
