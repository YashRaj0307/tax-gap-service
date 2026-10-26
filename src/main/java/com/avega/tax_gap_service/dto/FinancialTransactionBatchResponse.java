
package com.avega.tax_gap_service.dto;

import java.util.List;

public record FinancialTransactionBatchResponse(
        int totalTransactions,
        int successfulTransactions,
        int failedTransactions,
        List<FinancialTransactionResult> results
) {
    public record FinancialTransactionResult(
            Long id,
            String transactionId,
            String validationStatus,
            String complianceStatus,
            List<String> failureReasons
    ) {
    }
}
