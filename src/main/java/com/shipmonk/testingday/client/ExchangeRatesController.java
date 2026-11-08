package com.shipmonk.testingday.client;

import com.shipmonk.testingday.manager.RatesManager;
import com.shipmonk.testingday.manager.exception.RatesSnapshotUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Tag(name = "Exchange rates", description = "Historical exchange rates for a single day")
@RestController
@RequestMapping(
    path = "/api/v1/rates"
)
public class ExchangeRatesController
{

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRatesController.class);

    private final RatesManager ratesManager;

    public ExchangeRatesController(RatesManager ratesManager)
    {
        this.ratesManager = ratesManager;
    }

    @Operation(
        summary = "Get exchange rates for a day",
        description = """
            Returns the exchange rates for one day, with USD as the base currency. Rates are rounded to 2 decimal
            places; rates below 0.1 are rounded after their first 2 non-zero decimal digits (e.g. `0.000013`).

            The `day` must be in ISO-8601 format `yyyy-MM-dd` (for example `2026-11-06`), must be a real calendar date
            and must not be later than today in UTC.

            The first request for a day fetches its rates from external resource (fixer.io) and caches them."""
    )
    @GetMapping(path = "/{day}")
    public ExchangeRatesResponse getRates(
        @Parameter(
            required = true,
            schema = @Schema(type = "string", format = "date", pattern = "^\\d{4}-\\d{2}-\\d{2}$", example = "2026-11-06")
        )
        @PathVariable(name = "day")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @NotNull(message = "day must not be empty")
        @PastOrPresent(message = "day must not be in the future")
        LocalDate day
    )
    {
        return ExchangeRatesResponse.from(ratesManager.getSnapshot(day));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidFormat(MethodArgumentTypeMismatchException e)
    {
        LOG.error("Invalid date format provided", e);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getName() + " must be a valid date in yyyy-MM-dd format"
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleConstraintViolation(HandlerMethodValidationException e)
    {
        LOG.error("Invalid request", e);
        String detail = e.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(RatesSnapshotUnavailableException.class)
    public ProblemDetail handleUnavailable(RatesSnapshotUnavailableException e)
    {
        LOG.error("Rates snapshot is currently unavailable", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

}
