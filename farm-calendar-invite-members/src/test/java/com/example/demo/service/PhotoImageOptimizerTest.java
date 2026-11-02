package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PhotoImageOptimizerTest {
    @Test
    void resizesAndCompressesLargeJpeg() throws Exception {
        BufferedImage image = new BufferedImage(3200, 2000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (int i = 0; i < 3200; i += 7) {
            g.setColor(new Color((i * 3) % 256, (i * 5) % 256, (i * 11) % 256));
            g.fillRect(i, 0, 7, 2000);
        }
        g.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "jpeg", output));
        byte[] raw = output.toByteArray();
        var optimized = PhotoImageOptimizer.prepare(raw,
                new FarmPhotoStorage.ImageType("image/jpeg", "jpg"));
        assertEquals("image/jpeg", optimized.imageType().mime());
        assertTrue(optimized.bytes().length < raw.length);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(optimized.bytes()));
        assertNotNull(result);
        assertTrue(Math.max(result.getWidth(), result.getHeight()) <= 1600);
    }

    @Test
    void preservesTransparentPngAndOriginalWhenLarger() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(255, 0, 0, 120).getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        byte[] raw = output.toByteArray();
        var optimized = PhotoImageOptimizer.prepare(raw,
                new FarmPhotoStorage.ImageType("image/png", "png"));
        assertEquals("image/png", optimized.imageType().mime());
        assertTrue(optimized.bytes().length <= raw.length);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(optimized.bytes()));
        assertTrue(result.getColorModel().hasAlpha());
    }

    @Test
    void retainsWebpWhenNoJavaCodecIsInstalled() {
        byte[] raw = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };
        var optimized = PhotoImageOptimizer.prepare(raw,
                new FarmPhotoStorage.ImageType("image/webp", "webp"));
        assertSame(raw, optimized.bytes());
        assertEquals("image/webp", optimized.imageType().mime());
    }
}
