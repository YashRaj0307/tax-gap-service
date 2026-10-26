package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.ComplianceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;

public interface ComplianceExceptionRepository extends JpaRepository<ComplianceException, Long> {
    @Query("""
        select e from ComplianceException e
        where (:customerId is null or e.customerId = :customerId)
          and (:severity is null or e.severity = :severity)
          and (:ruleName is null or e.ruleName = :ruleName)
        order by e.timestamp desc
        """)
    List<ComplianceException> search(@Param("customerId") String customerId,
                                     @Param("severity") String severity,
                                     @Param("ruleName") String ruleName);

    @Query(value = "SELECT severity, COUNT(*) AS exception_count FROM compliance_exception GROUP BY severity", nativeQuery = true)
    List<Object[]> countBySeverity();

    @Query(value = "SELECT COALESCE(customer_id, 'UNKNOWN') AS customer_id, COUNT(*) AS exception_count FROM compliance_exception GROUP BY customer_id ORDER BY COUNT(*) DESC, customer_id", nativeQuery = true)
    List<Object[]> countByCustomer();
}
