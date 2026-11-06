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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<Rate> rates = new ArrayList<>();

    protected RatesSnapshot()
    {
    }

    public RatesSnapshot(LocalDate date, String baseCurrency, Instant fetchedAt)
    {
        this.date = date;
        this.baseCurrency = baseCurrency;
        this.fetchedAt = fetchedAt;
    }

    /**
     * Adds a rate through the owning side, so both ends of the association stay in sync.
     */
    public Rate addRate(String currency, BigDecimal value)
    {
        Rate rate = new Rate(this, currency, value);
        rates.add(rate);
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

    public List<Rate> getRates()
    {
        return Collections.unmodifiableList(rates);
    }

}
