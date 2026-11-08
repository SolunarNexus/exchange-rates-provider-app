package com.shipmonk.testingday.manager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shipmonk.testingday.resourceaccess.exception.FixerResourceAccessException;
import com.shipmonk.testingday.resourceaccess.FixerResourceAccess;
import com.shipmonk.testingday.engine.impl.ComputingEngineImpl;
import com.shipmonk.testingday.engine.exception.UnsupportedCurrencyException;
import com.shipmonk.testingday.engine.ValidatingEngine;
import com.shipmonk.testingday.manager.exception.RatesSnapshotUnavailableException;
import com.shipmonk.testingday.manager.impl.RatesManagerImpl;
import com.shipmonk.testingday.model.RatesSnapshotData;
import com.shipmonk.testingday.resourceaccess.RatesSnapshotResourceAccess;
import com.shipmonk.testingday.resourceaccess.entity.Rate;
import com.shipmonk.testingday.resourceaccess.entity.RatesSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatesManagerImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);
    private static final Instant FETCHED_AT = Instant.parse("2026-11-07T08:00:00Z");
    private static final int SAMPLE_RATE_COUNT = 6;

    @Mock
    private RatesSnapshotResourceAccess ratesSnapshotResourceAccess;

    @Mock
    private FixerResourceAccess rateSnapshotClient;

    @Mock
    private ValidatingEngine validatingEngine;

    private RatesManagerImpl ratesManager;

    private RatesSnapshotData fixerSample;

    @BeforeEach
    void setUp() throws IOException {
        ratesManager = new RatesManagerImpl(
            ratesSnapshotResourceAccess,
            rateSnapshotClient,
            validatingEngine,
            new ComputingEngineImpl()
        );
        fixerSample = loadFixerSample();
    }

    @Test
    void storedSnapshotShouldBeReturnedInDefaultBaseCurrencyWithoutFetching() {
        when(ratesSnapshotResourceAccess.findByDate(DAY)).thenReturn(Optional.of(storedSnapshot(fixerSample)));

        RatesSnapshotData result = ratesManager.getSnapshot(DAY);

        assertConvertedToDefaultBase(result);
        verifyNoInteractions(rateSnapshotClient);
        verify(ratesSnapshotResourceAccess, never()).save(any());
    }

    @Test
    void missingSnapshotShouldBeFetchedAndStored() {
        when(ratesSnapshotResourceAccess.findByDate(DAY)).thenReturn(Optional.empty());
        when(rateSnapshotClient.fetchSnapshot(DAY)).thenReturn(fixerSample);
        when(ratesSnapshotResourceAccess.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RatesSnapshotData result = ratesManager.getSnapshot(DAY);

        ArgumentCaptor<RatesSnapshot> saved = ArgumentCaptor.forClass(RatesSnapshot.class);
        verify(ratesSnapshotResourceAccess).save(saved.capture());
        RatesSnapshot stored = saved.getValue();
        assertThat(stored.getDate()).isEqualTo(DAY);
        assertThat(stored.getBaseCurrency()).isEqualTo("EUR");
        assertThat(stored.getFetchedAt()).isNotNull();
        assertThat(stored.getRates()).hasSize(SAMPLE_RATE_COUNT);
        assertThat(rateOf(stored, "CZK")).isEqualByComparingTo("24.346956");
        assertThat(rateOf(stored, "BTC")).isEqualByComparingTo("0.000014714976");
        assertConvertedToDefaultBase(result);
    }

    @Test
    void invalidDayShouldBeRejectedBeforeAnyLookup() {
        IllegalArgumentException exception = new IllegalArgumentException("future date");
        doThrow(exception).when(validatingEngine).validate(DAY);

        assertThatThrownBy(() -> ratesManager.getSnapshot(DAY)).isSameAs(exception);
        verifyNoInteractions(ratesSnapshotResourceAccess, rateSnapshotClient);
    }

    @Test
    void fetchFailureShouldBeReported() {
        FixerResourceAccessException fixerFailure = new FixerResourceAccessException("error");
        when(ratesSnapshotResourceAccess.findByDate(DAY)).thenReturn(Optional.empty());
        when(rateSnapshotClient.fetchSnapshot(DAY)).thenThrow(fixerFailure);

        assertThatThrownBy(() -> ratesManager.getSnapshot(DAY))
            .isInstanceOfSatisfying(RatesSnapshotUnavailableException.class,
                e -> assertThat(e.getCause()).isSameAs(fixerFailure));
        verify(ratesSnapshotResourceAccess, never()).save(any());
    }

    @Test
    void snapshotStoredConcurrentlyShouldBeReturnedWhenInsertFails() {
        when(ratesSnapshotResourceAccess.findByDate(DAY))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(storedSnapshot(fixerSample)));
        when(rateSnapshotClient.fetchSnapshot(DAY)).thenReturn(fixerSample);
        when(ratesSnapshotResourceAccess.save(any())).thenThrow(new DataIntegrityViolationException(""));

        RatesSnapshotData result = ratesManager.getSnapshot(DAY);

        assertConvertedToDefaultBase(result);
        verify(rateSnapshotClient, times(1)).fetchSnapshot(DAY);
        verify(ratesSnapshotResourceAccess, times(2)).findByDate(DAY);
    }

    @Test
    void snapshotWithoutDefaultBaseCurrencyShouldFailConversion() {
        RatesSnapshot withoutUsd = new RatesSnapshot(DAY, "EUR", FETCHED_AT);
        withoutUsd.addRate("EUR", BigDecimal.ONE);
        withoutUsd.addRate("CZK", new BigDecimal("24.346956"));
        when(ratesSnapshotResourceAccess.findByDate(DAY)).thenReturn(Optional.of(withoutUsd));

        assertThatThrownBy(() -> ratesManager.getSnapshot(DAY))
            .isInstanceOf(UnsupportedCurrencyException.class)
            .hasMessageContaining("USD");
    }

    /**
     * Expected values are the sample's EUR rates divided by its USD rate (1.115071), computed independently.
     */
    private static void assertConvertedToDefaultBase(RatesSnapshotData result) {
        assertThat(result.date()).isEqualTo(DAY);
        assertThat(result.baseCurrency()).isEqualTo("USD");
        assertThat(result.rates()).hasSize(SAMPLE_RATE_COUNT);
        assertThat(result.rates().get("USD")).isEqualByComparingTo("1");
        assertThat(result.rates().get("EUR")).isCloseTo(new BigDecimal("0.8968038806497523"), withinPercentage(1e-10));
        assertThat(result.rates().get("CZK")).isCloseTo(new BigDecimal("21.83444462280877"), withinPercentage(1e-10));
        assertThat(result.rates().get("GBP")).isCloseTo(new BigDecimal("0.7486707124479069"), withinPercentage(1e-10));
        assertThat(result.rates().get("BTC")).isCloseTo(new BigDecimal("0.00001319644758046797"), withinPercentage(1e-10));
    }

    private static RatesSnapshot storedSnapshot(RatesSnapshotData data) {
        RatesSnapshot snapshot = new RatesSnapshot(data.date(), data.baseCurrency(), FETCHED_AT);
        data.rates().forEach(snapshot::addRate);
        return snapshot;
    }

    private static BigDecimal rateOf(RatesSnapshot snapshot, String currency) {
        return snapshot.getRates().stream()
            .filter(rate -> rate.getCurrency().equals(currency))
            .map(Rate::getRate)
            .findFirst()
            .orElseThrow();
    }

    private static RatesSnapshotData loadFixerSample() throws IOException {
        try (InputStream json = RatesManagerImplTest.class.getResourceAsStream("/fixer/sample-2026-11-06.json")) {
            FixerSample sample = JsonMapper.builder().build().readValue(json, FixerSample.class);
            return new RatesSnapshotData(sample.date(), sample.base(), sample.rates());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FixerSample(String base, LocalDate date, Map<String, BigDecimal> rates) {
    }

}
