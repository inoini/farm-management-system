package com.example.demo.conotroller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

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
                () -> controller.gallery("CROP", 12L, null, "", new ExtendedModelMap()));
    }

    @Test
    void refusesToOpenAnotherFarmsFieldAlbum() {
        when(user.email()).thenReturn("farm-one");
        when(fields.findByIdAndOwnerEmail(17L, "farm-one"))
                .thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> controller.gallery("FIELD", 17L, "2026-10-14", "list",
                        new ExtendedModelMap()));
        verify(fields).findByIdAndOwnerEmail(17L, "farm-one");
    }

    @Test
    void fieldPhotoAlbumKeepsItsFieldIdAndCompactMode() {
        when(user.email()).thenReturn("farm-one");
        com.example.demo.entity.Field field = new com.example.demo.entity.Field();
        field.setId(17L);
        field.setName("第1圃場");
        when(fields.findByIdAndOwnerEmail(17L, "farm-one"))
                .thenReturn(Optional.of(field));

        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("farm-photos", controller.gallery("FIELD", 17L, "2026-10-14", "list", model));
        assertEquals("FIELD", model.get("albumType"));
        assertEquals(17L, model.get("targetId"));
        assertEquals(true, model.get("compactView"));
        verify(photos).findByOwnerEmailAndKindAndReferenceIdOrderByCreatedAtDesc("farm-one", "FIELD", 17L);
    }

    @Test
    void albumTypeIsWhitelisted() {
        when(user.email()).thenReturn("farm-one");
        assertThrows(ResponseStatusException.class,
                () -> controller.gallery("NOT_SUPPORTED", 3L, "2026-10-13", "",
                        new ExtendedModelMap()));
    }

    @Test
    void compactListModeIsOnlyEnabledForTheRequestedView() {
        when(user.email()).thenReturn("farm-one");
        ExtendedModelMap minimal = new ExtendedModelMap();
        assertEquals("farm-photos", controller.gallery("DAY", null, "2026-10-14", "list", minimal));
        assertEquals(true, minimal.get("compactView"));

        ExtendedModelMap full = new ExtendedModelMap();
        assertEquals("farm-photos", controller.gallery("DAY", null, "2026-10-14", "", full));
        assertEquals(false, full.get("compactView"));
    }

    @Test
    void deletingFromCompactListReturnsToTheSameDateAlbum() {
        when(user.email()).thenReturn("farm-one");
        FarmPhoto photo = new FarmPhoto();
        photo.setId(8L);
        photo.setKind("DAY");
        photo.setPhotoDate(LocalDate.parse("2026-10-14"));
        photo.setStorageKey("example.jpg");
        when(photos.findByIdAndOwnerEmail(8L, "farm-one"))
                .thenReturn(Optional.of(photo));

        String result = controller.delete(8L, "list", new RedirectAttributesModelMap());
        assertEquals("redirect:/farm-photos?type=DAY&date=2026-10-14&view=list", result);
        verify(storage).delete("example.jpg");
        verify(photos).delete(photo);
    }
}
