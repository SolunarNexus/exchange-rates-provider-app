package com.shipmonk.testingday.resourceaccess.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "rates_snapshot")
public class RatesSnapshot
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @OneToMany(mappedBy = "ratesSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Rate> rates = new HashSet<>();

    protected RatesSnapshot()
    {
    }

    public RatesSnapshot(LocalDate date, String baseCurrency, Instant fetchedAt)
    {
        this.date = date;
        this.baseCurrency = baseCurrency;
        this.fetchedAt = fetchedAt;
    }

    public Rate addRate(String currency, BigDecimal value)
    {
        Rate rate = new Rate(this, currency, value);
        if (!rates.add(rate))
            throw new IllegalArgumentException("Snapshot already has a rate for currency " + currency);

        return rate;
    }

    public Long getId()
    {
        return id;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public String getBaseCurrency()
    {
        return baseCurrency;
    }

    public Instant getFetchedAt()
    {
        return fetchedAt;
    }

    public Set<Rate> getRates()
    {
        return Collections.unmodifiableSet(rates);
    }

    @Override
    public boolean equals(Object other)
    {
        return this == other
            || other instanceof RatesSnapshot snapshot && Objects.equals(getDate(), snapshot.getDate());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(getDate());
    }

}
