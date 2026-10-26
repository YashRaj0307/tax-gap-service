
package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.ComplianceRule;
import com.avega.tax_gap_service.dto.ComplianceRuleResponse;
import com.avega.tax_gap_service.repository.ComplianceRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceRuleServiceTest {

    @Mock
    private ComplianceRuleRepository repository;

    @InjectMocks
    private ComplianceRuleService service;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void findAllReturnsMappedRules() {
        ComplianceRule rule = rule(1L, true);
        when(repository.findAll()).thenReturn(List.of(rule));

        List<ComplianceRuleResponse> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("HIGH_VALUE_TRANSACTION", result.getFirst().ruleName());
        assertEquals("HIGH_VALUE", result.getFirst().ruleType());
        assertTrue(result.getFirst().enabled());
        assertEquals("HIGH", result.getFirst().severity());
        verify(repository).findAll();
    }

    @Test
    void findAllReturnsEmptyListWhenNoRulesExist() {
        when(repository.findAll()).thenReturn(List.of());

        List<ComplianceRuleResponse> result = service.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void setEnabledDisablesRule() {
        ComplianceRule rule = rule(1L, true);
        when(repository.findById(1L)).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        ComplianceRuleResponse result = service.setEnabled(1L, false);

        verify(rule).setEnabled(false);
        verify(repository).save(rule);
        assertNotNull(result);
    }

    @Test
    void setEnabledCanEnableDisabledRule() {
        ComplianceRule rule = rule(2L, false);
        when(repository.findById(2L)).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        ComplianceRuleResponse result = service.setEnabled(2L, true);

        verify(rule).setEnabled(true);
        verify(repository).save(rule);
        assertNotNull(result);
    }

    @Test
    void setEnabledThrowsWhenRuleDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.setEnabled(99L, true));

        assertTrue(exception.getMessage().contains("99"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateConfigurationUpdatesAndSavesRule() {
        ComplianceRule rule = rule(3L, true);
        ObjectNode configuration = mapper.createObjectNode()
                .put("threshold", 50000);

        when(repository.findById(3L)).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        ComplianceRuleResponse result =
                service.updateConfiguration(3L, configuration);

        verify(rule).setConfiguration(argThat(
                actual -> actual.get("threshold").asInt() == 50000));
        verify(repository).save(rule);
        assertNotNull(result);
    }

    @Test
    void updateConfigurationStoresDeepCopy() {
        ComplianceRule rule = rule(4L, true);
        ObjectNode configuration = mapper.createObjectNode()
                .put("threshold", 1000);

        when(repository.findById(4L)).thenReturn(Optional.of(rule));
        when(repository.save(rule)).thenReturn(rule);

        service.updateConfiguration(4L, configuration);

        configuration.put("threshold", 9999);

        verify(rule).setConfiguration(argThat(
                actual -> actual.get("threshold").asInt() == 1000));
    }

    @Test
    void updateConfigurationRejectsNullConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfiguration(1L, null));

        verifyNoInteractions(repository);
    }

    @Test
    void updateConfigurationRejectsArrayConfiguration() {
        JsonNode configuration = mapper.createArrayNode().add(100);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfiguration(1L, configuration));

        verifyNoInteractions(repository);
    }

    @Test
    void updateConfigurationRejectsTextConfiguration() {
        JsonNode configuration = mapper.getNodeFactory()
                .textNode("invalid");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfiguration(1L, configuration));

        verifyNoInteractions(repository);
    }

    @Test
    void updateConfigurationThrowsWhenRuleDoesNotExist() {
        ObjectNode configuration = mapper.createObjectNode()
                .put("threshold", 1000);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.updateConfiguration(99L, configuration));

        verify(repository, never()).save(any());
    }

    private ComplianceRule rule(Long id, boolean enabled) {
        ObjectNode configuration = mapper.createObjectNode()
                .put("threshold", 1000);

        ComplianceRule rule = mock(ComplianceRule.class);

        when(rule.getId()).thenReturn(id);
        when(rule.getRuleName()).thenReturn("HIGH_VALUE_TRANSACTION");
        when(rule.getRuleType()).thenReturn("HIGH_VALUE");
        when(rule.isEnabled()).thenReturn(enabled);
        when(rule.getSeverity()).thenReturn("HIGH");
        when(rule.getConfiguration()).thenReturn(configuration);
        when(rule.getCreatedAt()).thenReturn(null);
        when(rule.getUpdatedAt()).thenReturn(null);

        return rule;
    }
}
