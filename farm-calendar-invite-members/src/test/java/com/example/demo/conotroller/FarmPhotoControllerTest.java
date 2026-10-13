package com.example.demo.conotroller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.FarmPhoto;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.FarmPhotoRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FarmPhotoStorage;

class FarmPhotoControllerTest {
    private final FarmPhotoRepository photos = mock(FarmPhotoRepository.class);
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final CropRepository crops = mock(CropRepository.class);
    private final FieldRepository fields = mock(FieldRepository.class);
    private final FarmPhotoStorage storage = mock(FarmPhotoStorage.class);
    private final CurrentUserService user = mock(CurrentUserService.class);
    private final FarmPhotoController controller =
            new FarmPhotoController(photos, schedules, crops, fields, storage, user);

    @Test
    void refusesToServeAnotherFarmsPhoto() {
        when(user.email()).thenReturn("farm-one");
        when(photos.findByIdAndOwnerEmail(9L, "farm-one"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.photoFile(9L));
        verifyNoInteractions(storage);
    }

    @Test
    void refusesToOpenAnotherFarmsCropAlbum() {
        when(user.email()).thenReturn("farm-one");
        when(crops.findByIdAndOwnerEmail(12L, "farm-one"))
                .thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> controller.gallery("CROP", 12L, null, new ExtendedModelMap()));
    }

    @Test
    void albumTypeIsWhitelisted() {
        when(user.email()).thenReturn("farm-one");
        assertThrows(ResponseStatusException.class,
                () -> controller.gallery("NOT_SUPPORTED", 3L, "2026-10-13",
                        new ExtendedModelMap()));
    }
}
