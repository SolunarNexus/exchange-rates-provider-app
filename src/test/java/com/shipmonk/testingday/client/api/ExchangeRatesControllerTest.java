package com.shipmonk.testingday.client.api;

import com.shipmonk.testingday.config.TimeConfig;
import com.shipmonk.testingday.manager.RatesManager;
import com.shipmonk.testingday.model.RatesSnapshotData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ExchangeRatesController.class)
@Import(TimeConfig.class)
class ExchangeRatesControllerTest
{

    // 23:30 UTC is already the next day in Central Europe
    private static final Instant NOW = Instant.parse("2026-10-15T23:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RatesManager ratesManager;

    @TestBean
    private Clock clock;

    static Clock clock()
    {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @BeforeEach
    void stubManager()
    {
        when(ratesManager.getSnapshot(any())).thenAnswer(invocation -> new RatesSnapshotData(
            invocation.getArgument(0),
            "USD",
            Map.of("EUR", new BigDecimal("0.950650"))
        ));
    }

    @Test
    void todayShouldSucceed()
    {
        String today = TODAY.toString();

        assertThat(mvc.get().uri("/api/v1/rates/{day}", today))
            .hasStatusOk()
            .bodyJson().extractingPath("$.date").isEqualTo(today);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2022-06-20", "1999-01-01", "2024-02-29"})
    void pastDayShouldSucceed(String day)
    {
        assertThat(mvc.get().uri("/api/v1/rates/{day}", day))
            .hasStatusOk()
            .bodyJson().extractingPath("$.date").isEqualTo(day);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/rates", "/api/v1/rates/", "/api/v1/rates/2022/06/20"})
    void pathWithoutDaySegmentShouldNotBeFound(String path)
    {
        assertThat(mvc.get().uri(path)).hasStatus(HttpStatus.NOT_FOUND);
        verifyNoInteractions(ratesManager);
    }

    @Test
    void blankDayShouldFail()
    {
        // A blank day converts to null, which Spring rejects for a required path variable before validation runs
        assertThat(mvc.get().uri("/api/v1/rates/{day}", " ")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(ratesManager);
    }

    @Test
    void futureDayShouldFail()
    {
        String tomorrow = TODAY.plusDays(1).toString();

        assertBadRequest("/api/v1/rates/" + tomorrow);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2022-02-30", "2023-02-29", "2022-13-01", "2022-00-10", "2022-04-31"})
    void impossibleDateShouldFail(String day)
    {
        assertBadRequest("/api/v1/rates/" + day);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20220620", "2022-6-20", "20-06-2022", "2022.06.20", "2022-06-20T10:00"})
    void malformedDateShouldFail(String day)
    {
        assertBadRequest("/api/v1/rates/" + day);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "today", "null", "-1"})
    void nonDateValueShouldFail(String day)
    {
        assertBadRequest("/api/v1/rates/" + day);
    }

    private void assertBadRequest(String path)
    {
        assertThat(mvc.get().uri(path))
            .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(ratesManager);
    }

}
