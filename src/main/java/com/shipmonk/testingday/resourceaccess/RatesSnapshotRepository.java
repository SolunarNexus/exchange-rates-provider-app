package com.shipmonk.testingday.resourceaccess;

import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.Optional;

public interface RatesSnapshotRepository extends Repository<RatesSnapshot, Long>
{

    RatesSnapshot save(RatesSnapshot snapshot);

    @EntityGraph(attributePaths = "rates")
    Optional<RatesSnapshot> findByDate(LocalDate date);

}
