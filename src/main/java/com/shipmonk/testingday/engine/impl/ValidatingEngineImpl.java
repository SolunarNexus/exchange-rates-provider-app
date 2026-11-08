package com.shipmonk.testingday.engine.impl;

import com.shipmonk.testingday.engine.ValidatingEngine;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ValidatingEngineImpl implements ValidatingEngine {

    private final Clock clock;

    public ValidatingEngineImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void validate(LocalDate day) {
        if (day.isAfter(LocalDate.now(clock)))
            throw new IllegalArgumentException("No exchange rates exist for future day " + day);
    }

}
