package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findByCustomerId(String customerId);
    List<FinancialTransaction> findByComplianceStatus(String complianceStatus);
    List<FinancialTransaction> findByValidationStatus(String validationStatus);

    @Query("""
        select t from FinancialTransaction t
        where (:customerId is null or t.customerId = :customerId)
          and (:complianceStatus is null or t.complianceStatus = :complianceStatus)
          and (:validationStatus is null or t.validationStatus = :validationStatus)
        order by t.createdAt desc
        """)
    List<FinancialTransaction> search(@Param("customerId") String customerId,
                                      @Param("complianceStatus") String complianceStatus,
                                      @Param("validationStatus") String validationStatus);
    boolean existsByTransactionId(String transactionId);
    Optional<FinancialTransaction> findFirstByTransactionIdAndTransactionTypeAndValidationStatus(
            String transactionId, String transactionType, String validationStatus);

    @Query(value = """
        SELECT customer_id AS "customerId",
               COALESCE(SUM(amount), 0) AS "totalAmount",
               COALESCE(SUM(reported_tax), 0) AS "totalReportedTax",
               COALESCE(SUM(expected_tax), 0) AS "totalExpectedTax",
               COALESCE(SUM(tax_gap), 0) AS "totalTaxGap",
               COUNT(*) AS "totalTransactions",
               COUNT(*) FILTER (WHERE compliance_status = 'NON_COMPLIANT' OR validation_status = 'FAILURE') AS "nonCompliantTransactions",
               CASE WHEN COUNT(*) = 0 THEN 100.00
                    ELSE 100.0 - (COUNT(*) FILTER (WHERE compliance_status = 'NON_COMPLIANT' OR validation_status = 'FAILURE') * 100.0 / COUNT(*)) END AS "complianceScore"
        FROM financial_transaction
        WHERE customer_id = :customerId
        GROUP BY customer_id
        """, nativeQuery = true)
    CustomerTaxSummaryProjection customerTaxSummary(@Param("customerId") String customerId);

    interface CustomerTaxSummaryProjection {
        String getCustomerId();
        java.math.BigDecimal getTotalAmount();
        java.math.BigDecimal getTotalReportedTax();
        java.math.BigDecimal getTotalExpectedTax();
        java.math.BigDecimal getTotalTaxGap();
        Long getTotalTransactions();
        Long getNonCompliantTransactions();
        java.math.BigDecimal getComplianceScore();
    }
}
