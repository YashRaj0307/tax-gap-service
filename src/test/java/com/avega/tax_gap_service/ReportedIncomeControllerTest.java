
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportedIncomeController.class)
@Import(SecurityConfig.class)
class ReportedIncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportedIncomeService reportedIncomeService;

    @Test
    void createReportedIncomeWithValidRequestReturnsCreated() throws Exception {
        when(reportedIncomeService.create(any(CreateReportedIncomeRequest.class)))
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
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
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
                .andExpect(jsonPath("$.sourceReference").value("BANK-TXN-1001"))
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.reportedIncome").value(125000.00));
    }

    @Test
    void createReportedIncomeWithBlankSourceReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/reported-income")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
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
                .andExpect(jsonPath("$.path").value("/api/v1/reported-income"));
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
    void findReportedIncomeByTaxpayerAndYearReturnsList() throws Exception {
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
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
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
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .param("taxYear", "2026"))
                .andExpect(status().isBadRequest());
    }
}
