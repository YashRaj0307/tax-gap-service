package com.avega.tax_gap_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "taxpayer")
public class Taxpayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_reference", nullable = false, unique = true, length = 50)
    private String taxpayerReference;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "tax_identifier", nullable = false, unique = true, length = 50)
    private String taxIdentifier;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "declared_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal declaredIncome;

    @Column(name = "declared_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal declaredTax;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Taxpayer() {
    }

    public Taxpayer(
            String taxpayerReference,
            String legalName,
            String taxIdentifier,
            Integer taxYear,
            BigDecimal declaredIncome,
            BigDecimal declaredTax
    ) {
        this.taxpayerReference = taxpayerReference;
        this.legalName = legalName;
        this.taxIdentifier = taxIdentifier;
        this.taxYear = taxYear;
        this.declaredIncome = declaredIncome;
        this.declaredTax = declaredTax;
    }

    public Long getId() {
        return id;
    }

    public String getTaxpayerReference() {
        return taxpayerReference;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public Integer getTaxYear() {
        return taxYear;
    }

    public BigDecimal getDeclaredIncome() {
        return declaredIncome;
    }

    public BigDecimal getDeclaredTax() {
        return declaredTax;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}