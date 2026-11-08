package com.shipmonk.testingday.resourceaccess.impl;

import com.shipmonk.testingday.resourceaccess.exception.FixerRateLimitException;
import com.shipmonk.testingday.resourceaccess.exception.FixerResourceAccessException;
import com.shipmonk.testingday.model.RatesSnapshotData;
import com.shipmonk.testingday.resourceaccess.FixerResourceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class FixerResourceAccessImpl implements FixerResourceAccess {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final List<Duration> RATE_LIMIT_RETRY_DELAYS =
        List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4));
    private static final int RATE_LIMIT_ERROR_CODE = 106;

    private static final Logger LOG = LoggerFactory.getLogger(FixerResourceAccessImpl.class);

    private final RestClient restClient;
    private final String accessKey;

    public FixerResourceAccessImpl(
        @Value("${fixer.base-url}") String baseUrl,
        @Value("${fixer.access-key}") String accessKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
        this.accessKey = accessKey;
    }

    @Override
    public RatesSnapshotData fetchSnapshot(LocalDate day) {
        for (Duration delay : RATE_LIMIT_RETRY_DELAYS) {
            try {
                return fetchSnapshotOnce(day);
            } catch (FixerRateLimitException e) {
                LOG.warn("Fixer rate limit reached for {}, retrying in {} s", day, delay.toSeconds());
                waitBeforeRetry(delay, day);
            }
        }
        return fetchSnapshotOnce(day);
    }

    private RatesSnapshotData fetchSnapshotOnce(LocalDate day) {
        FixerResponse response = fetchResponse(day);

        if (isRateLimitError(response))
            throw new FixerRateLimitException("Fixer rate limit reached for " + day + ": " + describeError(response));
        if (hasErrorResponse(response))
            throw new FixerResourceAccessException("Couldn't retrieve rates for " + day + ": " + describeError(response));

        return new RatesSnapshotData(response.date(), response.base(), response.rates());
    }

    private static boolean isRateLimitError(FixerResponse response) {
        return response != null && response.error() != null && response.error().code() == RATE_LIMIT_ERROR_CODE;
    }

    private static void waitBeforeRetry(Duration delay, LocalDate day) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FixerResourceAccessException("Interrupted while waiting to retry rates for " + day, e);
        }
    }

    private static boolean hasErrorResponse(FixerResponse response) {
        return response == null
            || !response.success()
            || response.date() == null
            || response.base() == null
            || response.rates() == null;
    }

    private FixerResponse fetchResponse(LocalDate day) {
        try {
            return restClient.get()
                .uri("/{day}?access_key={accessKey}", day, accessKey)
                .retrieve()
                .body(FixerResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new FixerRateLimitException("Fixer rate limit reached for " + day, e);
        } catch (RestClientException e) {
            throw new FixerResourceAccessException("Failed to fetch rates for " + day, e);
        }
    }

    private String describeError(FixerResponse response) {
        if (response == null || response.error() == null)
            return "unexpected response";

        return response.error().code() + " " + response.error().info();
    }

    record FixerResponse(
        boolean success,
        LocalDate date,
        String base,
        Map<String, BigDecimal> rates,
        FixerError error
    ) {
    }

    record FixerError(int code, String info) {
    }

}
