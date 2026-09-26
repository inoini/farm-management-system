package com.example.demo.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1_100L;

    private final RestClient restClient;
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();
    private long lastRequestStartedAt;

    public GeocodingService(
            RestClient.Builder builder,
            @Value("${app.geocoding.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.geocoding.user-agent:SekineFarmManagement/1.0}") String userAgent) {

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                .build();
    }

    /**
     * 日本国内の住所を緯度・経度へ変換します。
     * 同一住所はキャッシュし、外部サービスへの連続アクセスを抑えます。
     */
    public Optional<Coordinates> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = normalizeAddress(address);
        Coordinates cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        synchronized (requestLock) {
            cached = cache.get(cacheKey);
            if (cached != null) {
                return Optional.of(cached);
            }

            if (!waitForRequestInterval()) {
                return Optional.empty();
            }

            lastRequestStartedAt = System.currentTimeMillis();

            try {
                NominatimResult[] results = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", address)
                                .queryParam("format", "jsonv2")
                                .queryParam("limit", 1)
                                .queryParam("countrycodes", "jp")
                                .build())
                        .retrieve()
                        .body(NominatimResult[].class);

                if (results == null || results.length == 0) {
                    return Optional.empty();
                }

                Coordinates coordinates = toCoordinates(results[0]);
                if (coordinates == null) {
                    return Optional.empty();
                }

                cache.put(cacheKey, coordinates);
                return Optional.of(coordinates);
            } catch (RuntimeException ex) {
                // 住所そのものはログへ出さず、利用者情報の不要な記録を避けます。
                log.warn("住所から地図位置を取得できませんでした。", ex);
                return Optional.empty();
            }
        }
    }

    private boolean waitForRequestInterval() {
        long elapsed = System.currentTimeMillis() - lastRequestStartedAt;
        long waitMillis = MIN_REQUEST_INTERVAL_MILLIS - elapsed;

        if (waitMillis <= 0) {
            return true;
        }

        try {
            Thread.sleep(waitMillis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Coordinates toCoordinates(NominatimResult result) {
        if (result == null || result.lat() == null || result.lon() == null) {
            return null;
        }

        try {
            double latitude = Double.parseDouble(result.lat());
            double longitude = Double.parseDouble(result.lon());

            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return null;
            }

            return new Coordinates(latitude, longitude);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeAddress(String address) {
        return Normalizer.normalize(address, Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.JAPAN);
    }

    public record Coordinates(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(String lat, String lon) {
    }
}