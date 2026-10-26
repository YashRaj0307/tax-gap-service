package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {
    List<ComplianceRule> findByEnabledTrueOrderByIdAsc();
    Optional<ComplianceRule> findByRuleName(String ruleName);
}
