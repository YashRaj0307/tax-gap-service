
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.config.SecurityConfig;
import com.avega.tax_gap_service.controller.TaxGapAnalysisController;
import com.avega.tax_gap_service.domain.AnalysisStatus;
import com.avega.tax_gap_service.domain.TaxGapAnalysis;
import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.service.TaxGapAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.avega.tax_gap_service.domain.AppUser;
import com.avega.tax_gap_service.repository.AppUserRepository;
import com.avega.tax_gap_service.support.TestAppUserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaxGapAnalysisController.class)
@Import(SecurityConfig.class)
class TaxGapAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxGapAnalysisService taxGapAnalysisService;

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
    void analyzeWithValidRequestReturnsAnalysisResponse() throws Exception {
        TaxGapAnalysis analysis = createAnalysis(
                1L,
                10L,
                new BigDecimal("1000000.00"),
                new BigDecimal("1200000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("20.0000"),
                AnalysisStatus.REVIEW_REQUIRED
        );

        when(taxGapAnalysisService.analyze(10L, 2026)).thenReturn(analysis);

        mockMvc.perform(post("/api/v1/tax-gap-analyses/taxpayers/10")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxpayerId").value(10))
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.incomeDiscrepancy").value(200000.00))
                .andExpect(jsonPath("$.analysisStatus").value("REVIEW_REQUIRED"));
    }

    @Test
    void findAnalysesReturnsAnalysisList() throws Exception {
        TaxGapAnalysis analysis = createAnalysis(
                2L,
                10L,
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                AnalysisStatus.NO_DISCREPANCY
        );

        when(taxGapAnalysisService.findByTaxpayerAndYear(10L, 2026))
                .thenReturn(List.of(analysis));

        mockMvc.perform(get("/api/v1/tax-gap-analyses/taxpayers/10")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].taxpayerId").value(10))
                .andExpect(jsonPath("$[0].analysisStatus").value("NO_DISCREPANCY"));
    }

    @Test
    void analyzeWithInvalidTaxpayerIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tax-gap-analyses/taxpayers/0")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void analyzeWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/tax-gap-analyses/taxpayers/10")
                        .param("taxYear", "2026"))
                .andExpect(status().isUnauthorized());
    }

    private TaxGapAnalysis createAnalysis(
            Long analysisId,
            Long taxpayerId,
            BigDecimal declaredIncome,
            BigDecimal reportedIncome,
            BigDecimal discrepancy,
            BigDecimal percentage,
            AnalysisStatus status
    ) {
        Taxpayer taxpayer = mock(Taxpayer.class);
        when(taxpayer.getId()).thenReturn(taxpayerId);

        TaxGapAnalysis analysis = mock(TaxGapAnalysis.class);
        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getTaxpayer()).thenReturn(taxpayer);
        when(analysis.getTaxYear()).thenReturn(2026);
        when(analysis.getDeclaredIncome()).thenReturn(declaredIncome);
        when(analysis.getReportedIncome()).thenReturn(reportedIncome);
        when(analysis.getIncomeDiscrepancy()).thenReturn(discrepancy);
        when(analysis.getDiscrepancyPercentage()).thenReturn(percentage);
        when(analysis.getAnalysisStatus()).thenReturn(status);
        when(analysis.getAnalysisReason()).thenReturn("Test analysis result");
        when(analysis.getAnalyzedAt()).thenReturn(
                OffsetDateTime.parse("2026-10-26T10:00:00Z"));

        return analysis;
    }
}
