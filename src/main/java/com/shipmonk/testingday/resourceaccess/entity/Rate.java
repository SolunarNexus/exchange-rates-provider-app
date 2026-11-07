package com.shipmonk.testingday.resourceaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "rate")
public class Rate
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rates_snapshot_id", nullable = false)
    private RatesSnapshot ratesSnapshot;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    protected Rate()
    {
    }

    Rate(RatesSnapshot ratesSnapshot, String currency, BigDecimal rate)
    {
        this.ratesSnapshot = ratesSnapshot;
        this.currency = currency;
        this.rate = rate;
    }

    public Long getId()
    {
        return id;
    }

    public RatesSnapshot getRatesSnapshot()
    {
        return ratesSnapshot;
    }

    public String getCurrency()
    {
        return currency;
    }

    public BigDecimal getRate()
    {
        return rate;
    }

    @Override
    public boolean equals(Object other)
    {
        return this == other
            || other instanceof Rate that
            && Objects.equals(getRatesSnapshot(), that.getRatesSnapshot())
            && Objects.equals(getCurrency(), that.getCurrency());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getRatesSnapshot(), getCurrency());
    }

}
