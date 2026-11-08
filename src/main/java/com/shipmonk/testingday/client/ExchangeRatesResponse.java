package com.shipmonk.testingday.client;

import com.shipmonk.testingday.model.RatesSnapshotData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

@Schema(description = "Exchange rates for one day")
public record ExchangeRatesResponse(
    @Schema(description = "Day the rates apply to", format = "date", example = "2026-11-06")
    LocalDate date,

    @Schema(description = "Base currency the rates are expressed against", example = "USD")
    String base,

    @Schema(
        description = "Units of each currency that one unit of the base currency buys, keyed by ISO 4217 code "
            + "and sorted alphabetically",
        example = "{\"CZK\": 21.83444462280877, \"EUR\": 0.8968038806497523, \"USD\": 1}"
    )
    SortedMap<String, BigDecimal> rates
)
{

    public static ExchangeRatesResponse from(RatesSnapshotData snapshot)
    {
        return new ExchangeRatesResponse(snapshot.date(), snapshot.baseCurrency(), new TreeMap<>(snapshot.rates()));
    }

}
