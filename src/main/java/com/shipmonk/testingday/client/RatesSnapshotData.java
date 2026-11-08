package com.shipmonk.testingday.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Exchange rates for one day as returned by the rates provider, independent of how they are stored.
 */
public record RatesSnapshotData(LocalDate date, String baseCurrency, Map<String, BigDecimal> rates) {
}
