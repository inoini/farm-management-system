package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FarmPhotoStorageTest {
    private final FarmPhotoStorage storage = new FarmPhotoStorage();

    @Test
    void detectsJpegPngAndWebpFromBytes() {
        assertEquals("image/jpeg", storage.detectImageType(new byte[]{
                (byte)255, (byte)216, (byte)255, 10, 10}).mime());

        assertEquals("image/png", storage.detectImageType(new byte[]{
                (byte)137, 80, 78, 71, 13, 10, 26, 10}).mime());

        assertEquals("image/webp", storage.detectImageType(new byte[]{
                82, 73, 70, 70, 0, 0, 0, 0, 87, 69, 66, 80}).mime());
    }

    @Test
    void rejectsNonImagesAndTooLargeUploads() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.detectImageType("<svg onload='alert(1)'>".getBytes()));
        assertThrows(IllegalArgumentException.class,
                () -> storage.detectImageType(new byte[(int) FarmPhotoStorage.MAX_FILE_BYTES + 1]));
        assertThrows(IllegalArgumentException.class,
                () -> storage.detectImageType(new byte[0]));
    }

    @Test
    void remainsOffWithoutStorageCredentials() {
        assertFalse(storage.isConfigured());
    }
}
