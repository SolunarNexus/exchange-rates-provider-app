package com.shipmonk.testingday.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RatesSnapshotData(LocalDate date, String baseCurrency, Map<String, BigDecimal> rates) {
}
