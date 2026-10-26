package com.avega.tax_gap_service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.avega.tax_gap_service.repository.FinancialTransactionRepository;
import com.avega.tax_gap_service.service.FinancialTransactionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = com.avega.tax_gap_service.controller.FinancialTransactionController.class,
        excludeFilters = {})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(
        addFilters = false)
class FinancialTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialTransactionService service;

    @MockitoBean
    private FinancialTransactionRepository repository;

    @MockitoBean
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void findWithoutFiltersPassesNullFiltersToRepository() throws Exception {
        when(repository.search(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/financial-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(repository).search(null, null, null);
    }

    @Test
    void findNormalizesAndTrimsFilters() throws Exception {
        when(repository.search("C123", "COMPLIANT", "VALID"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/financial-transactions")
                        .param("customerId", "  C123  ")
                        .param("complianceStatus", " compliant ")
                        .param("validationStatus", " valid "))
                .andExpect(status().isOk());

        verify(repository).search("C123", "COMPLIANT", "VALID");
    }

    @Test
    void findConvertsBlankFiltersToNull() throws Exception {
        when(repository.search(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/financial-transactions")
                        .param("customerId", "   ")
                        .param("complianceStatus", " ")
                        .param("validationStatus", ""))
                .andExpect(status().isOk());

        verify(repository).search(null, null, null);
    }

    @Test
    void ingestBatchRejectsJsonObject() throws Exception {
        mockMvc.perform(post("/api/financial-transactions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C123\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void ingestBatchRejectsJsonString() throws Exception {
        mockMvc.perform(post("/api/financial-transactions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"not-an-array\""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void ingestBatchRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/financial-transactions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"customerId\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}