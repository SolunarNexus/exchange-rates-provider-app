package com.shipmonk.testingday.manager;

import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;

import java.time.LocalDate;

public interface RatesManager
{

    /**
     * Returns the stored rates snapshot for the given day. If the local snapshot copy is missing, fetches the fresh
     * snapshot from external provider. Persists the fetched snapshots.
     *
     * @throws IllegalArgumentException if the day is in the future (UTC)
     * @throws RatesSnapshotUnavailableException if fetching the snapshot fails
     */
    RatesSnapshot getSnapshot(LocalDate day);

}
