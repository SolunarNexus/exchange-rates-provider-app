package com.shipmonk.testingday.engine;

import com.shipmonk.testingday.engine.exception.UnsupportedCurrencyException;
import com.shipmonk.testingday.engine.impl.BaseCurrency;
import com.shipmonk.testingday.model.RatesSnapshotData;

public interface ComputingEngine {

    /**
     * Re-expresses the rates relative to another base currency. Returns the given rates unchanged if they already
     * use that base.
     *
     * @throws UnsupportedCurrencyException if the rates contain no usable rate for the target currency
     */
    RatesSnapshotData convertToBaseCurrency(RatesSnapshotData snapshot, BaseCurrency targetBase);

}
