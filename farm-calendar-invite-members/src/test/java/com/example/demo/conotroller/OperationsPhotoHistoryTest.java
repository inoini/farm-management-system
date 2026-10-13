package com.example.demo.conotroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.example.demo.entity.FarmPhoto;
import com.example.demo.entity.Schedule;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FarmPhotoRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.HarvestShipmentRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.WorkerRepository;
import com.example.demo.service.CurrentUserService;

class OperationsPhotoHistoryTest {
    private final ScheduleRepository schedules = mock(ScheduleRepository.class);
    private final FarmPhotoRepository photos = mock(FarmPhotoRepository.class);
    private final CurrentUserService user = mock(CurrentUserService.class);
    private final OperationsController controller = new OperationsController(
            schedules, photos, mock(WorkerRepository.class),
            mock(HarvestShipmentRepository.class), mock(AppSettingRepository.class),
            mock(CropRepository.class), mock(FieldRepository.class),
            mock(SalesRepository.class), mock(ExpenseRepository.class), user);

    private static FarmPhoto calendarPhoto(long id, String day) {
        FarmPhoto photo = new FarmPhoto();
        photo.setId(id);
        photo.setKind("DAY");
        photo.setPhotoDate(LocalDate.parse(day));
        return photo;
    }

    @SuppressWarnings("unchecked")
    @Test
    void showsCalendarPhotosOnTheMatchingWorkDate() {
        when(user.email()).thenReturn("farm-a");
        Schedule work = new Schedule();
        work.setId(42L);
        work.setDate("2026-10-13");
        FarmPhoto photo = calendarPhoto(5L, "2026-10-13");
        when(schedules.findAllByOwnerEmailOrderByDateDescStartTimeDesc("farm-a"))
                .thenReturn(List.of(work));
        when(photos.findByOwnerEmailAndKindOrderByCreatedAtDesc("farm-a", "DAY"))
                .thenReturn(List.of(photo));

        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("operations/work-history", controller.workHistory(model));

        Map<String, List<FarmPhoto>> byDate =
                (Map<String, List<FarmPhoto>>) model.get("dayPhotosByDate");
        Map<String, List<FarmPhoto>> unmatched =
                (Map<String, List<FarmPhoto>>) model.get("photosWithoutWork");
        assertEquals(List.of(photo), byDate.get("2026-10-13"));
        assertTrue(unmatched.isEmpty());
        verify(photos).findByOwnerEmailAndKindOrderByCreatedAtDesc("farm-a", "DAY");
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesPhotosWhenNoWorkWasEnteredForTheDate() {
        when(user.email()).thenReturn("farm-a");
        when(schedules.findAllByOwnerEmailOrderByDateDescStartTimeDesc("farm-a"))
                .thenReturn(List.of());
        FarmPhoto photo = calendarPhoto(6L, "2026-10-12");
        when(photos.findByOwnerEmailAndKindOrderByCreatedAtDesc("farm-a", "DAY"))
                .thenReturn(List.of(photo));

        ExtendedModelMap model = new ExtendedModelMap();
        controller.workHistory(model);

        Map<String, List<FarmPhoto>> unmatched =
                (Map<String, List<FarmPhoto>>) model.get("photosWithoutWork");
        assertFalse(unmatched.isEmpty());
        assertEquals(List.of(photo), unmatched.get("2026-10-12"));
    }
}
