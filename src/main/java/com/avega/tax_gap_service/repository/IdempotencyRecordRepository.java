
package com.avega.tax_gap_service.repository;

import com.avega.tax_gap_service.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(
            value = """
                    INSERT INTO idempotency_record (
                        idempotency_key,
                        request_fingerprint,
                        response_status
                    )
                    VALUES (
                        :idempotencyKey,
                        :requestFingerprint,
                        202
                    )
                    ON CONFLICT (idempotency_key) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestFingerprint") String requestFingerprint
    );
}
