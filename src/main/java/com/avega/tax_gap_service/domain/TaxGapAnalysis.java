
package com.avega.tax_gap_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tax_gap_analysis")
public class TaxGapAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    private Taxpayer taxpayer;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "declared_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal declaredIncome;

    @Column(name = "reported_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal reportedIncome;

    @Column(name = "income_discrepancy", nullable = false, precision = 19, scale = 2)
    private BigDecimal incomeDiscrepancy;

    @Column(name = "discrepancy_percentage", precision = 9, scale = 4)
    private BigDecimal discrepancyPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_reason", nullable = false, columnDefinition = "text")
    private String analysisReason;

    @Generated(event = EventType.INSERT)
    @Column(name = "analyzed_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime analyzedAt;

    protected TaxGapAnalysis() {
    }

    public TaxGapAnalysis(
            Taxpayer taxpayer,
            Integer taxYear,
            BigDecimal declaredIncome,
            BigDecimal reportedIncome,
            BigDecimal incomeDiscrepancy,
            BigDecimal discrepancyPercentage,
            AnalysisStatus analysisStatus,
            String analysisReason) {
        this.taxpayer = taxpayer;
        this.taxYear = taxYear;
        this.declaredIncome = declaredIncome;
        this.reportedIncome = reportedIncome;
        this.incomeDiscrepancy = incomeDiscrepancy;
        this.discrepancyPercentage = discrepancyPercentage;
        this.analysisStatus = analysisStatus;
        this.analysisReason = analysisReason;
    }

    public Long getId() {
        return id;
    }

    public Taxpayer getTaxpayer() {
        return taxpayer;
    }

    public Integer getTaxYear() {
        return taxYear;
    }

    public BigDecimal getDeclaredIncome() {
        return declaredIncome;
    }

    public BigDecimal getReportedIncome() {
        return reportedIncome;
    }

    public BigDecimal getIncomeDiscrepancy() {
        return incomeDiscrepancy;
    }

    public BigDecimal getDiscrepancyPercentage() {
        return discrepancyPercentage;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public String getAnalysisReason() {
        return analysisReason;
    }

    public OffsetDateTime getAnalyzedAt() {
        return analyzedAt;
    }
}
