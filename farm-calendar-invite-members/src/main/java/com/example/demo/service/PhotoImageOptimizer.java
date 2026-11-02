package com.example.demo.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Compresses newly uploaded photos without touching previously stored images.
 * Uses standard JDK image codecs; formats that cannot be decoded (e.g. WebP on
 * typical JDK installations) retain their validated original bytes.
 */
public final class PhotoImageOptimizer {
    public static final int MAX_DIMENSION = 1600;
    private static final float JPEG_QUALITY = 0.76f;

    private PhotoImageOptimizer() { }

    public record PreparedPhoto(byte[] bytes, FarmPhotoStorage.ImageType imageType) { }

    public static PreparedPhoto prepare(byte[] original, FarmPhotoStorage.ImageType type) {
        if (original == null || type == null || original.length == 0
                || original.length > FarmPhotoStorage.MAX_FILE_BYTES) {
            throw new IllegalArgumentException("写真は1枚5MB以下で選んでください。");
        }
        PreparedPhoto unchanged = new PreparedPhoto(original, type);
        if ("image/webp".equals(type.mime())) {
            return unchanged;
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (stream == null) return unchanged;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return unchanged;
            ImageReader reader = readers.next();
            BufferedImage decoded;
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 100_000_000L) {
                    throw new IllegalArgumentException("画像の解像度が高すぎます。");
                }
                // Subsample BEFORE decoding to bound peak Java heap consumption.
                int sample = Math.max(1, (int) Math.ceil(Math.max(width, height) / 2200.0));
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                decoded = reader.read(0, parameters);
            } finally {
                reader.dispose();
            }
            if (decoded == null) return unchanged;
            int width = decoded.getWidth();
            int height = decoded.getHeight();
            double ratio = Math.min(1.0, (double) MAX_DIMENSION / Math.max(width, height));
            int newWidth = Math.max(1, (int) Math.round(width * ratio));
            int newHeight = Math.max(1, (int) Math.round(height * ratio));
            boolean transparent = decoded.getColorModel().hasAlpha();
            // A transparent PNG is kept as PNG so overlays and masks do not lose alpha.
            String format = transparent ? "png" : "jpeg";
            BufferedImage scaled = new BufferedImage(newWidth, newHeight,
                    transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                if (!transparent) {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, newWidth, newHeight);
                }
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(decoded, 0, 0, newWidth, newHeight, null);
            } finally {
                graphics.dispose();
                decoded.flush();
            }
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
            if (!writers.hasNext()) return unchanged;
            ImageWriter writer = writers.next();
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 ImageOutputStream target = ImageIO.createImageOutputStream(output)) {
                if (target == null) return unchanged;
                try {
                    writer.setOutput(target);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (!transparent && param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(JPEG_QUALITY);
                    }
                    writer.write(null, new IIOImage(scaled, null, null), param);
                    target.flush();
                } finally {
                    writer.dispose();
                    scaled.flush();
                }
                byte[] optimized = output.toByteArray();
                if (optimized.length > 0 && optimized.length < original.length
                        && optimized.length <= FarmPhotoStorage.MAX_FILE_BYTES) {
                    return new PreparedPhoto(optimized, transparent
                            ? new FarmPhotoStorage.ImageType("image/png", "png")
                            : new FarmPhotoStorage.ImageType("image/jpeg", "jpg"));
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // A valid legacy image should still upload when an optional codec fails.
            return unchanged;
        }
        return unchanged;
    }
}
