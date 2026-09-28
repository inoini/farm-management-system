package com.example.demo.service;

import java.text.Normalizer;
import java.util.List;
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

    private final RestClient nominatimClient;
    private final RestClient gsiClient;
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();
    private long lastRequestStartedAt;

    public GeocodingService(
            RestClient.Builder builder,
            @Value("${app.geocoding.base-url:https://nominatim.openstreetmap.org}") String nominatimBaseUrl,
            @Value("${app.geocoding.gsi-base-url:https://msearch.gsi.go.jp}") String gsiBaseUrl,
            @Value("${app.geocoding.user-agent:SekineFarmManagement/1.0}") String userAgent) {

        this.nominatimClient = builder.clone()
                .baseUrl(nominatimBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                .build();

        this.gsiClient = builder.clone()
                .baseUrl(gsiBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ja")
                .build();
    }

    /**
     * 日本国内の住所を緯度・経度へ変換します。
     * 国土地理院の地名検索を優先し、取得できない場合は Nominatim へフォールバックします。
     * 同一住所はキャッシュし、外部サービスへの連続アクセスを抑えます。
     */
    public Optional<Coordinates> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        String normalizedAddress = Normalizer.normalize(address.strip(), Normalizer.Form.NFKC);
        String cacheKey = normalizeAddress(normalizedAddress);
        Coordinates cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        synchronized (requestLock) {
            cached = cache.get(cacheKey);
            if (cached != null) {
                return Optional.of(cached);
            }

            Coordinates coordinates = geocodeWithGsi(normalizedAddress);
            if (coordinates == null) {
                coordinates = geocodeWithNominatim(normalizedAddress);
            }

            if (coordinates == null) {
                return Optional.empty();
            }

            cache.put(cacheKey, coordinates);
            return Optional.of(coordinates);
        }
    }

    private Coordinates geocodeWithGsi(String address) {
        try {
            GsiResult[] results = gsiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/address-search/AddressSearch")
                            .queryParam("q", address)
                            .build())
                    .retrieve()
                    .body(GsiResult[].class);

            if (results == null) {
                return null;
            }

            for (GsiResult result : results) {
                Coordinates coordinates = toCoordinates(result);
                if (coordinates != null) {
                    return coordinates;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("国土地理院の住所検索から地図位置を取得できませんでした。", ex);
        }
        return null;
    }

    private Coordinates geocodeWithNominatim(String address) {
        if (!waitForRequestInterval()) {
            return null;
        }
        lastRequestStartedAt = System.currentTimeMillis();

        try {
            NominatimResult[] results = nominatimClient.get()
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
                return null;
            }
            return toCoordinates(results[0]);
        } catch (RuntimeException ex) {
            // 住所そのものはログへ出さず、利用者情報の不要な記録を避けます。
            log.warn("OpenStreetMap の住所検索から地図位置を取得できませんでした。", ex);
            return null;
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

    private Coordinates toCoordinates(GsiResult result) {
        if (result == null || result.geometry() == null || result.geometry().coordinates() == null) {
            return null;
        }

        List<Double> values = result.geometry().coordinates();
        if (values.size() < 2 || values.get(0) == null || values.get(1) == null) {
            return null;
        }

        // GeoJSON は [経度, 緯度] の順です。
        return validatedCoordinates(values.get(1), values.get(0));
    }

    private Coordinates toCoordinates(NominatimResult result) {
        if (result == null || result.lat() == null || result.lon() == null) {
            return null;
        }

        try {
            return validatedCoordinates(Double.parseDouble(result.lat()), Double.parseDouble(result.lon()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Coordinates validatedCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return null;
        }
        return new Coordinates(latitude, longitude);
    }

    private String normalizeAddress(String address) {
        return Normalizer.normalize(address, Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.JAPAN);
    }

    public record Coordinates(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GsiResult(GsiGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GsiGeometry(List<Double> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(String lat, String lon) {
    }
}
