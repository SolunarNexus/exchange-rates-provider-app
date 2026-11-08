package com.shipmonk.testingday.engine.impl;

import com.shipmonk.testingday.engine.ComputingEngine;
import com.shipmonk.testingday.engine.exception.UnsupportedCurrencyException;
import com.shipmonk.testingday.model.RatesSnapshotData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

@Component
public class ComputingEngineImpl implements ComputingEngine {

    private static final MathContext PRECISION = MathContext.DECIMAL64;

    @Override
    public RatesSnapshotData convertToBaseCurrency(RatesSnapshotData snapshot, BaseCurrency targetBase) {
        String sourceCurrency = snapshot.baseCurrency();
        String targetCurrency = targetBase.name();
        if (sourceCurrency.equals(targetCurrency))
            return snapshot;

        BigDecimal targetRate = snapshot.rates().get(targetCurrency);
        if (targetRate == null || targetRate.signum() <= 0)
            throw new UnsupportedCurrencyException(targetCurrency);

        Map<String, BigDecimal> converted = new HashMap<>();
        snapshot.rates().forEach((currency, rate) -> converted.put(currency, rate.divide(targetRate, PRECISION)));
        converted.put(sourceCurrency, BigDecimal.ONE.divide(targetRate, PRECISION));
        converted.put(targetCurrency, BigDecimal.ONE);

        return new RatesSnapshotData(snapshot.date(), targetCurrency, converted);
    }

}
