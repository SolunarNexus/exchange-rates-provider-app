package com.shipmonk.testingday.engine.impl;

import com.shipmonk.testingday.engine.ComputingEngine;
import com.shipmonk.testingday.engine.exception.UnsupportedCurrencyException;
import com.shipmonk.testingday.model.RatesSnapshotData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class ComputingEngineImpl implements ComputingEngine {

    private static final MathContext PRECISION = MathContext.DECIMAL64;
    private static final int DECIMAL_PLACES = 2;
    private static final int SIGNIFICANT_DIGITS = 2;

    @Override
    public RatesSnapshotData convertToBaseCurrency(RatesSnapshotData snapshot, BaseCurrency targetBase) {
        String sourceCurrency = snapshot.baseCurrency();
        String targetCurrency = targetBase.name();
        if (sourceCurrency.equals(targetCurrency))
            return roundRates(snapshot);

        BigDecimal targetRate = snapshot.rates().get(targetCurrency);
        if (targetRate == null || targetRate.signum() <= 0)
            throw new UnsupportedCurrencyException(targetCurrency);

        Map<String, BigDecimal> converted = new HashMap<>();
        snapshot.rates().forEach((currency, rate) -> converted.put(currency, rate.divide(targetRate, PRECISION)));
        converted.put(sourceCurrency, BigDecimal.ONE.divide(targetRate, PRECISION));
        converted.put(targetCurrency, BigDecimal.ONE);

        return roundRates(new RatesSnapshotData(snapshot.date(), targetCurrency, converted));
    }

    /**
     * Rounds every rate half-up to 2 decimal places. Rates too small to show 2 non-zero digits that way (below 0.1)
     * are rounded after their first 2 non-zero decimal digits instead, e.g. {@code 0.0000131964} to {@code 0.000013}.
     */
    private static RatesSnapshotData roundRates(RatesSnapshotData snapshot) {
        Map<String, BigDecimal> rounded = new HashMap<>();
        snapshot.rates().forEach((currency, rate) -> rounded.put(currency, round(rate)));
        return new RatesSnapshotData(snapshot.date(), snapshot.baseCurrency(), rounded);
    }

    private static BigDecimal round(BigDecimal rate) {
        if (rate.signum() == 0)
            return rate.setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);

        int leadingDigitExponent = rate.precision() - rate.scale() - 1;
        int scale = Math.max(DECIMAL_PLACES, SIGNIFICANT_DIGITS - 1 - leadingDigitExponent);
        return rate.setScale(scale, RoundingMode.HALF_UP);
    }

}
