
package com.avega.tax_gap_service;

import com.avega.tax_gap_service.controller.TaxpayerController;
import org.springframework.context.annotation.Import;
import com.avega.tax_gap_service.config.SecurityConfig;
import com.avega.tax_gap_service.dto.CreateTaxpayerRequest;
import com.avega.tax_gap_service.dto.TaxpayerResponse;
import com.avega.tax_gap_service.service.TaxpayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaxpayerController.class)
@Import(SecurityConfig.class)
class TaxpayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxpayerService taxpayerService;

    @Test
    void createTaxpayerWithValidRequestReturnsCreated() throws Exception {
        when(taxpayerService.create(any(CreateTaxpayerRequest.class)))
                .thenReturn(new TaxpayerResponse(
                        1L,
                        "TAX-2026-001",
                        "Example Company",
                        "PAN-ABCDE1234F",
                        2026,
                        new BigDecimal("1000000.00"),
                        new BigDecimal("100000.00"),
                        OffsetDateTime.parse("2026-10-26T10:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/taxpayers")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerReference": "TAX-2026-001",
                                  "legalName": "Example Company",
                                  "taxIdentifier": "PAN-ABCDE1234F",
                                  "taxYear": 2026,
                                  "declaredIncome": 1000000.00,
                                  "declaredTax": 100000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxpayerReference").value("TAX-2026-001"))
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.declaredIncome").value(1000000.00));
    }

    @Test
    void createTaxpayerWithBlankLegalNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/taxpayers")
                        .with(httpBasic("devuser", "LocalDevOnly-ChangeMe-2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerReference": "TAX-2026-002",
                                  "legalName": " ",
                                  "taxIdentifier": "PAN-ABCDE1234G",
                                  "taxYear": 2026,
                                  "declaredIncome": 1000000.00,
                                  "declaredTax": 100000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/taxpayers"));
    }

    @Test
    void createTaxpayerWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/taxpayers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxpayerReference": "TAX-2026-003",
                                  "legalName": "Example Company",
                                  "taxIdentifier": "PAN-ABCDE1234H",
                                  "taxYear": 2026,
                                  "declaredIncome": 1000000.00,
                                  "declaredTax": 100000.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
