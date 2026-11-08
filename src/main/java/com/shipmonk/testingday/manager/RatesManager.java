package com.shipmonk.testingday.manager;

import com.shipmonk.testingday.model.RatesSnapshotData;

import java.time.LocalDate;

public interface RatesManager
{

    /**
     * Returns the stored rates snapshot for the given day. If the local snapshot copy is missing, fetches the fresh
     * snapshot from external provider. Persists the fetched snapshots. The returned rates are converted to USD as
     * their base currency.
     *
     * @throws IllegalArgumentException if the day is in the future (UTC)
     * @throws RatesSnapshotUnavailableException if fetching the snapshot fails
     */
    RatesSnapshotData getSnapshot(LocalDate day);

}
