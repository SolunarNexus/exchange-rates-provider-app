package com.shipmonk.testingday.engine;

import java.time.LocalDate;

public interface ValidatingEngine {

    /**
     * Checks if exchange rates can exist for the given day.
     *
     * @throws IllegalArgumentException if the day is in the future (UTC)
     */
    void validate(LocalDate day);

}
