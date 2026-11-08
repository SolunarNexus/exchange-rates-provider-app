package com.shipmonk.testingday.client.api;

import com.shipmonk.testingday.resourceaccess.entity.Rate;
import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

public record ExchangeRatesResponse(LocalDate date, String base, SortedMap<String, BigDecimal> rates)
{

    public static ExchangeRatesResponse from(RatesSnapshot snapshot)
    {
        SortedMap<String, BigDecimal> rates = new TreeMap<>();
        for (Rate rate : snapshot.getRates()) {
            rates.put(rate.getCurrency(), rate.getRate());
        }
        return new ExchangeRatesResponse(snapshot.getDate(), snapshot.getBaseCurrency(), rates);
    }

}
