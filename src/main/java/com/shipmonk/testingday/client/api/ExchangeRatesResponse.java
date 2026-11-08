package com.shipmonk.testingday.client.api;

import com.shipmonk.testingday.model.RatesSnapshotData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

public record ExchangeRatesResponse(LocalDate date, String base, SortedMap<String, BigDecimal> rates)
{

    public static ExchangeRatesResponse from(RatesSnapshotData snapshot)
    {
        return new ExchangeRatesResponse(snapshot.date(), snapshot.baseCurrency(), new TreeMap<>(snapshot.rates()));
    }

}
