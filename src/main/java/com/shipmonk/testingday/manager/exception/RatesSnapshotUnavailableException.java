package com.shipmonk.testingday.manager.exception;

import java.time.LocalDate;

public class RatesSnapshotUnavailableException extends RuntimeException {

    public RatesSnapshotUnavailableException(LocalDate day, Throwable cause) {
        super("Exchange rate snapshot for " + day + " is unavailable", cause);
    }

}
