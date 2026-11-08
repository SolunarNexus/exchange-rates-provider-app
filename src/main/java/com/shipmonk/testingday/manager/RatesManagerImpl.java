package com.shipmonk.testingday.manager;

import com.shipmonk.testingday.client.FixerClientException;
import com.shipmonk.testingday.client.RateSnapshotClient;
import com.shipmonk.testingday.client.RatesSnapshotData;
import com.shipmonk.testingday.engine.ValidatingEngine;
import com.shipmonk.testingday.resourceaccess.RatesSnapshotRepository;
import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class RatesManagerImpl implements RatesManager {

    private final RatesSnapshotRepository ratesSnapshotRepository;
    private final RateSnapshotClient rateSnapshotClient;
    private final ValidatingEngine validatingEngine;

    public RatesManagerImpl(
        RatesSnapshotRepository ratesSnapshotRepository,
        RateSnapshotClient rateSnapshotClient,
        ValidatingEngine validatingEngine
    ) {
        this.ratesSnapshotRepository = ratesSnapshotRepository;
        this.rateSnapshotClient = rateSnapshotClient;
        this.validatingEngine = validatingEngine;
    }

    // Runs outside any caller's transaction: the HTTP call should not hold a database connection,
    // and a failed insert has to be recoverable by reading the row stored in the meantime,
    // which is impossible inside a transaction the failed insert has already aborted.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RatesSnapshot getSnapshot(LocalDate day) {
        validatingEngine.validate(day);

        return ratesSnapshotRepository.findByDate(day)
            .orElseGet(() -> {
                RatesSnapshot snapshot = tryFetchSnapshot(day);
                return saveSnapshot(snapshot);
            });
    }

    private RatesSnapshot tryFetchSnapshot(LocalDate day) {
        try {
            return toSnapshot(day, rateSnapshotClient.fetchSnapshot(day));
        } catch (FixerClientException ex) {
            throw new RatesSnapshotUnavailableException(day, ex);
        }
    }

    private RatesSnapshot saveSnapshot(RatesSnapshot snapshot) {
        try {
            return ratesSnapshotRepository.save(snapshot);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request stored the same day first, so use its snapshot.
            return ratesSnapshotRepository.findByDate(snapshot.getDate()).orElseThrow(() -> e);
        }
    }

    /**
     * Maps provider data to a new, unsaved snapshot stored under the requested day.
     */
    private static RatesSnapshot toSnapshot(LocalDate day, RatesSnapshotData data) {
        RatesSnapshot snapshot = new RatesSnapshot(day, data.baseCurrency(), Instant.now());
        data.rates().forEach(snapshot::addRate);
        return snapshot;
    }

}
