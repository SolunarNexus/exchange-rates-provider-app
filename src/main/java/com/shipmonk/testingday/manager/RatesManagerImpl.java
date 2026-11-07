package com.shipmonk.testingday.manager;

import com.shipmonk.testingday.resourceaccess.RatesSnapshotRepository;
import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class RatesManagerImpl implements RatesManager
{

    private final RatesSnapshotRepository ratesSnapshotRepository;

    public RatesManagerImpl(RatesSnapshotRepository ratesSnapshotRepository)
    {
        this.ratesSnapshotRepository = ratesSnapshotRepository;
    }

    @Override
    public RatesSnapshot saveSnapshot(RatesSnapshot snapshot)
    {
        return ratesSnapshotRepository.save(snapshot);
    }

    @Override
    public Optional<RatesSnapshot> findSnapshot(LocalDate date)
    {
        return ratesSnapshotRepository.findByDate(date);
    }

}
