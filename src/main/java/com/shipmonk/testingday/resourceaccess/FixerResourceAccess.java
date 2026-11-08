package com.shipmonk.testingday.resourceaccess;

import com.shipmonk.testingday.resourceaccess.exception.FixerResourceAccessException;
import com.shipmonk.testingday.model.RatesSnapshotData;
import java.time.LocalDate;

public interface FixerResourceAccess
{

    /**
     * Fetches the rates for the given day.
     *
     * @throws FixerResourceAccessException if the request fails or Fixer reports an error
     */
    RatesSnapshotData fetchSnapshot(LocalDate day);

}
