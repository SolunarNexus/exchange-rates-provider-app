package com.shipmonk.testingday.client.api;

import com.shipmonk.testingday.manager.RatesManager;
import com.shipmonk.testingday.manager.RatesSnapshotUnavailableException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getName() + " must be a valid date in yyyy-MM-dd format"
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleConstraintViolation(HandlerMethodValidationException e)
    {
        String detail = e.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(RatesSnapshotUnavailableException.class)
    public ProblemDetail handleUnavailable(RatesSnapshotUnavailableException e)
    {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

}
