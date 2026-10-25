
package com.avega.tax_gap_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(
        name = "reported_income",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reported_income_source_reference",
                columnNames = {"reporting_source", "source_reference"}
        )
)
public class ReportedIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    private Taxpayer taxpayer;

    @Column(name = "reporting_source", nullable = false, length = 100)
    private String reportingSource;

    @Column(name = "source_reference", nullable = false, length = 100)
    private String sourceReference;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "reported_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal reportedIncome;

    @Generated(event = EventType.INSERT)
    @Column(
            name = "received_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime receivedAt;

    protected ReportedIncome() {
    }

    public ReportedIncome(
            Taxpayer taxpayer,
            String reportingSource,
            String sourceReference,
            Integer taxYear,
            BigDecimal reportedIncome
    ) {
        this.taxpayer = taxpayer;
        this.reportingSource = reportingSource;
        this.sourceReference = sourceReference;
        this.taxYear = taxYear;
        this.reportedIncome = reportedIncome;
    }

    public Long getId() {
        return id;
    }

    public Taxpayer getTaxpayer() {
        return taxpayer;
    }

    public String getReportingSource() {
        return reportingSource;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public Integer getTaxYear() {
        return taxYear;
    }

    public BigDecimal getReportedIncome() {
        return reportedIncome;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
}
