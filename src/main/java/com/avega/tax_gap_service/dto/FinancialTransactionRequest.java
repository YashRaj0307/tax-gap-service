
package com.avega.tax_gap_service.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record FinancialTransactionRequest(
        JsonNode transactionId,
        JsonNode date,
        JsonNode customerId,
        JsonNode amount,
        JsonNode taxRate,
        JsonNode reportedTax,
        JsonNode transactionType,
        JsonNode originalTransactionId
) {
}
