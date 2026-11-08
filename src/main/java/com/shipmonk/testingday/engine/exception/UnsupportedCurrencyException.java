package com.shipmonk.testingday.engine.exception;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currency) {
        super("Unable to use " + currency + " as a base currency");
    }

}
