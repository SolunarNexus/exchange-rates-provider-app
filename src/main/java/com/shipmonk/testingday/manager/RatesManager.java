package com.shipmonk.testingday.manager;

import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;

import java.time.LocalDate;
import java.util.Optional;

public interface RatesManager
{

    RatesSnapshot saveSnapshot(RatesSnapshot snapshot);

    Optional<RatesSnapshot> findSnapshot(LocalDate date);

}
