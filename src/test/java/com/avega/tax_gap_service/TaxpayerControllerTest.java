
package com.avega.tax_gap_service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avega.tax_gap_service.config.SecurityConfig;
import com.avega.tax_gap_service.controller.TaxpayerController;
import com.avega.tax_gap_service.dto.CreateTaxpayerRequest;
import com.avega.tax_gap_service.dto.TaxpayerResponse;
import com.avega.tax_gap_service.service.TaxpayerService;

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
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
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
                .andExpect(jsonPath("$.taxpayerReference")
                        .value("TAX-2026-001"))
                .andExpect(jsonPath("$.taxYear").value(2026))
                .andExpect(jsonPath("$.declaredIncome")
                        .value(1000000.00));
    }

    @Test
    void createTaxpayerWithBlankLegalNameReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/v1/taxpayers")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026"))
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
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/taxpayers"));
    }

    @Test
    void createTaxpayerWithoutAuthenticationReturnsUnauthorized()
            throws Exception {
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

    @Test
    void findAllPagedReturnsPageMetadataAndTaxpayers()
            throws Exception {
        TaxpayerResponse taxpayer = new TaxpayerResponse(
                1L,
                "TAX-2026-001",
                "Example Company",
                "PAN-ABCDE1234F",
                2026,
                new BigDecimal("1000000.00"),
                new BigDecimal("100000.00"),
                OffsetDateTime.parse("2026-10-26T10:00:00Z")
        );

        Page<TaxpayerResponse> result = new PageImpl<>(
                List.of(taxpayer),
                PageRequest.of(
                        0, 20, Sort.by("id").ascending()),
                1
        );

        when(taxpayerService.findAll(any(Pageable.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/taxpayers/paged")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].legalName")
                        .value("Example Company"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void findAllPagedWithCustomSortingReturnsOk() throws Exception {
        when(taxpayerService.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/taxpayers/paged")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "legalName")
                        .param("direction", "desc")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isOk());

        verify(taxpayerService).findAll(eq(
                PageRequest.of(
                        1,
                        10,
                        Sort.by(Sort.Direction.DESC, "legalName")
                )
        ));
    }

    @Test
    void findAllPagedWithNegativePageReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers/paged")
                        .param("page", "-1")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllPagedWithOversizedPageSizeReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers/paged")
                        .param("size", "101")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllPagedWithUnsupportedSortFieldReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers/paged")
                        .param("sortBy", "taxIdentifier")
                        .with(httpBasic(
                                "devuser", "LocalDevOnly-ChangeMe-2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllPagedWithoutAuthenticationReturnsUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/taxpayers/paged"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void createTaxpayerWithMissingRequiredFieldsReturnsBadRequest()
        throws Exception {
    mockMvc.perform(post("/api/v1/taxpayers")
                    .with(httpBasic(
                            "devuser", "LocalDevOnly-ChangeMe-2026"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "taxpayerReference": "",
                              "legalName": "",
                              "taxIdentifier": "",
                              "taxYear": 2026,
                              "declaredIncome": 1000000.00,
                              "declaredTax": 100000.00
                            }
                            """))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(taxpayerService);
}

    @Test
    void createTaxpayerWithMalformedJsonReturnsBadRequest()
        throws Exception {
    mockMvc.perform(post("/api/v1/taxpayers")
                    .with(httpBasic(
                            "devuser", "LocalDevOnly-ChangeMe-2026"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid-json }"))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(taxpayerService);
}

    @Test
    void findAllPagedWithInvalidPageParameterReturnsBadRequest()
        throws Exception {
    mockMvc.perform(get("/api/v1/taxpayers/paged")
                    .param("page", "abc")
                    .with(httpBasic(
                            "devuser", "LocalDevOnly-ChangeMe-2026")))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(taxpayerService);
}

    @Test
    void findAllPagedWithZeroPageSizeReturnsBadRequest()
        throws Exception {
    mockMvc.perform(get("/api/v1/taxpayers/paged")
                    .param("size", "0")
                    .with(httpBasic(
                            "devuser", "LocalDevOnly-ChangeMe-2026")))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(taxpayerService);
}

}
