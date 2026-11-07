package com.shipmonk.testingday.client;

import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;

import java.time.LocalDate;

public interface RateSnapshotClient
{

    /**
     * Fetches the rates for the given day.
     *
     * @throws FixerClientException if the request fails or Fixer reports an error
     */
    RatesSnapshot fetchSnapshot(LocalDate day);

}
