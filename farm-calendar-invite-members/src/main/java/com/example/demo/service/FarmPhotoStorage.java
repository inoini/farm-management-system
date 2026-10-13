package com.example.demo.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Uses a PRIVATE Supabase Storage bucket. All traffic goes through our
 * authenticated Spring Boot routes; no storage secrets are sent to browsers.
 */
@Service
public class FarmPhotoStorage {
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    // Reserve a safety margin below the 1GB free storage quota.
    public static final long APPLICATION_QUOTA_BYTES = 800L * 1024 * 1024;
    public static final int MAX_PHOTOS_PER_ALBUM = 5;

    @Value("${app.photo.supabase.url:}")
    private String projectUrl;

    @Value("${app.photo.supabase.service-role-key:}")
    private String serviceRoleKey;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public record ImageType(String mime, String suffix) {}

    public boolean isConfigured() {
        try {
            URI uri = URI.create(projectUrl.strip());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && (host.toLowerCase(Locale.ROOT).endsWith(".supabase.co"))
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))
                    && uri.getPort() == -1
                    && serviceRoleKey != null && !serviceRoleKey.isBlank();
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return false;
        }
    }

    /** Detect actual file magic bytes, not the untrusted name/content-type header. */
    public ImageType detectImageType(byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("写真は1枚5MB以下にしてください。");
        }
        if (content.length > 3
                && (content[0] & 255) == 255 && (content[1] & 255) == 216
                && (content[2] & 255) == 255) {
            return new ImageType("image/jpeg", "jpg");
        }
        if (content.length > 7
                && (content[0] & 255) == 137 && content[1] == 80
                && content[2] == 78 && content[3] == 71
                && content[4] == 13 && content[5] == 10
                && content[6] == 26 && content[7] == 10) {
            return new ImageType("image/png", "png");
        }
        if (content.length > 11
                && content[0] == 'R' && content[1] == 'I'
                && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E'
                && content[10] == 'B' && content[11] == 'P') {
            return new ImageType("image/webp", "webp");
        }
        throw new IllegalArgumentException("JPEG、PNG、WebPの写真だけ登録できます。");
    }

    private URI objectUri(String objectKey) {
        if (!isConfigured()) {
            throw new IllegalStateException("写真保存先が未設定です。");
        }
        // Only keys issued by this server are ever accepted.
        if (!objectKey.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("画像の保存キーが不正です。");
        }
        return URI.create(projectUrl.strip().replaceAll("/+$", "")
                + "/storage/v1/object/farm-photos/" + objectKey);
    }

    private HttpRequest.Builder request(String objectKey) {
        return HttpRequest.newBuilder(objectUri(objectKey))
                .timeout(Duration.ofSeconds(30))
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey);
    }

    public String upload(byte[] content, ImageType imageType) {
        String key = UUID.randomUUID() + "." + imageType.suffix();
        HttpRequest req = request(key)
                .header("Content-Type", imageType.mime())
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        invoke(req, false);
        return key;
    }

    public byte[] read(String key) {
        HttpRequest req = request(key).GET().build();
        return invoke(req, true);
    }

    public void delete(String key) {
        HttpRequest req = request(key).DELETE().build();
        invoke(req, false);
    }

    private byte[] invoke(HttpRequest req, boolean includeBody) {
        try {
            HttpResponse<byte[]> result = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int code = result.statusCode();
            if (code < 200 || code > 299) {
                // Do not log URLs with tokens, response bodies, or the secret key.
                throw new IllegalStateException("写真保存サービスへの接続に失敗しました (HTTP " + code + ")。");
            }
            return includeBody ? result.body() : new byte[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("写真保存サービスへの通信が中断されました。", e);
        } catch (IOException e) {
            throw new IllegalStateException("写真保存サービスへ接続できませんでした。", e);
        }
    }
}
