package com.shipmonk.testingday.client;

import com.shipmonk.testingday.manager.RatesManager;
import com.shipmonk.testingday.manager.exception.RatesSnapshotUnavailableException;
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

    @GetMapping(path = "/{day}")
    public ExchangeRatesResponse getRates(
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
