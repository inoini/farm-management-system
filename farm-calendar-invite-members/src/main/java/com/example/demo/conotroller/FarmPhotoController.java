package com.example.demo.conotroller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.entity.FarmPhoto;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.FarmPhotoRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.FarmPhotoStorage;

/**
 * Photo albums are never public, even when the application landing page is public.
 * Every operation scopes records and referenced farm objects to the active farm.
 */
@Controller
@RequestMapping("/farm-photos")
public class FarmPhotoController {
    private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

    private final FarmPhotoRepository photos;
    private final ScheduleRepository schedules;
    private final CropRepository crops;
    private final FieldRepository fields;
    private final FarmPhotoStorage storage;
    private final CurrentUserService user;

    public FarmPhotoController(FarmPhotoRepository photos,
            ScheduleRepository schedules, CropRepository crops, FieldRepository fields,
            FarmPhotoStorage storage, CurrentUserService user) {
        this.photos = photos;
        this.schedules = schedules;
        this.crops = crops;
        this.fields = fields;
        this.storage = storage;
        this.user = user;
    }

    private record Album(String kind, Long targetId, LocalDate date, String title) {}

    private Album album(String type, Long targetId, String date) {
        String kind = type == null ? "DAY" : type.strip().toUpperCase(Locale.ROOT);
        String owner = user.email();
        LocalDate selectedDate;
        try {
            selectedDate = date == null || date.isBlank()
                    ? LocalDate.now(JAPAN) : LocalDate.parse(date);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日付の形式が正しくありません。");
        }

        return switch (kind) {
            case "DAY" -> new Album("DAY", null, selectedDate,
                    selectedDate + "の写真");
            case "SCHEDULE" -> {
                if (targetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                var schedule = schedules.findByIdAndOwnerEmail(targetId, owner)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                LocalDate workDate;
                try {
                    workDate = LocalDate.parse(schedule.getDate());
                } catch (RuntimeException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "作業日が正しくありません。");
                }
                yield new Album("SCHEDULE", targetId, workDate,
                        schedule.getSchedule() == null || schedule.getSchedule().isBlank()
                                ? "作業の写真" : schedule.getSchedule() + " の写真");
            }
            case "CROP" -> {
                if (targetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                var crop = crops.findByIdAndOwnerEmail(targetId, owner)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                yield new Album("CROP", targetId, selectedDate, crop.getCropName() + " の写真");
            }
            case "FIELD" -> {
                if (targetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                var field = fields.findByIdAndOwnerEmail(targetId, owner)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                yield new Album("FIELD", targetId, selectedDate, field.getName() + " の写真");
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "写真の種類が不正です。");
        };
    }

    private List<FarmPhoto> list(Album album) {
        if ("DAY".equals(album.kind())) {
            return photos.findByOwnerEmailAndPhotoDateOrderByCreatedAtDesc(
                    user.email(), album.date());
        }
        return photos.findByOwnerEmailAndKindAndReferenceIdOrderByCreatedAtDesc(
                user.email(), album.kind(), album.targetId());
    }

    @GetMapping
    public String gallery(@RequestParam(defaultValue = "DAY") String type,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String date,
            Model model) {
        Album album = album(type, targetId, date);
        List<FarmPhoto> images = list(album);
        long albumCount = "DAY".equals(album.kind())
                ? photos.countByOwnerEmailAndKindAndPhotoDate(user.email(), "DAY", album.date())
                : photos.countByOwnerEmailAndKindAndReferenceId(user.email(), album.kind(), album.targetId());
        Long currentSize = photos.totalStoredBytes();
        long used = currentSize == null ? 0L : currentSize;
        model.addAttribute("albumTitle", album.title());
        model.addAttribute("albumType", album.kind());
        model.addAttribute("targetId", album.targetId());
        model.addAttribute("photoDate", album.date());
        model.addAttribute("photoEnabled", storage.isConfigured());
        model.addAttribute("images", images);
        model.addAttribute("canUpload", storage.isConfigured()
                && albumCount < FarmPhotoStorage.MAX_PHOTOS_PER_ALBUM
                && used < FarmPhotoStorage.APPLICATION_QUOTA_BYTES);
        model.addAttribute("remainingCount", Math.max(0,
                FarmPhotoStorage.MAX_PHOTOS_PER_ALBUM - albumCount));
        return "farm-photos";
    }


    /** Private JSON endpoints used by calendar popup and crop photo viewer. */
    private record PhotoInfo(Long id, String url, String caption, String date) {}
    private record AlbumInfo(boolean photoEnabled, boolean canUpload, int remainingCount,
            List<PhotoInfo> images) {}

    private String fileUrl(FarmPhoto image) {
        return "/farm-photos/" + image.getId() + "/file";
    }

    private AlbumInfo info(Album album) {
        List<FarmPhoto> images = list(album);
        long albumCount = "DAY".equals(album.kind())
                ? photos.countByOwnerEmailAndKindAndPhotoDate(user.email(), "DAY", album.date())
                : photos.countByOwnerEmailAndKindAndReferenceId(user.email(), album.kind(), album.targetId());
        Long total = photos.totalStoredBytes();
        boolean enabled = storage.isConfigured();
        boolean canUpload = enabled
                && albumCount < FarmPhotoStorage.MAX_PHOTOS_PER_ALBUM
                && (total == null ? 0L : total) < FarmPhotoStorage.APPLICATION_QUOTA_BYTES;
        return new AlbumInfo(enabled, canUpload,
                (int) Math.max(0, FarmPhotoStorage.MAX_PHOTOS_PER_ALBUM - albumCount),
                images.stream().map(p -> new PhotoInfo(p.getId(), fileUrl(p), p.getCaption(),
                        p.getPhotoDate().toString())).toList());
    }

    @GetMapping("/api")
    @ResponseBody
    public AlbumInfo photoApi(@RequestParam(defaultValue = "DAY") String type,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String date) {
        return info(album(type, targetId, date));
    }

    /** Single request for crop thumbnails; never returns another farm's photos. */
    @GetMapping("/api/crop-covers")
    @ResponseBody
    public Map<Long, String> cropCovers() {
        Map<Long, String> covers = new LinkedHashMap<>();
        for (FarmPhoto p : photos.findByOwnerEmailAndKindOrderByCreatedAtDesc(user.email(), "CROP")) {
            if (p.getReferenceId() != null) {
                covers.putIfAbsent(p.getReferenceId(), fileUrl(p));
            }
        }
        return covers;
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> photoApiUpload(
            @RequestParam(defaultValue = "DAY") String type,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String date,
            @RequestParam("photo") MultipartFile file,
            @RequestParam(defaultValue = "") String caption) {
        Album album = album(type, targetId, date);
        try {
            FarmPhoto saved = savePhoto(album, file, caption);
            return ResponseEntity.ok(Map.of("id", saved.getId(), "url", fileUrl(saved),
                    "message", "写真を保存しました。"));
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "保存できませんでした：" + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public String upload(@RequestParam(defaultValue = "DAY") String type,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String date,
            @RequestParam("photo") MultipartFile file,
            @RequestParam(defaultValue = "") String caption,
            RedirectAttributes messages) {
        Album album = album(type, targetId, date);
        try {
            savePhoto(album, file, caption);
            messages.addFlashAttribute("photoSuccess", "写真を登録しました。");
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            messages.addFlashAttribute("photoError", "登録できませんでした：" + e.getMessage());
        }
        return link(album);
    }

    /** Shared validated save path for the original gallery and both inline views. */
    private FarmPhoto savePhoto(Album album, MultipartFile file, String caption) throws IOException {
        if (!storage.isConfigured()) {
            throw new IllegalStateException("写真の保存先が設定されていません。");
        }
        if (file == null || file.isEmpty() || file.getSize() > FarmPhotoStorage.MAX_FILE_BYTES) {
            throw new IllegalArgumentException("写真は1枚5MB以下で選んでください。");
        }
        if (caption == null || caption.length() > 120) {
            throw new IllegalArgumentException("メモは120文字以内にしてください。");
        }
        // The instance-level lock follows the existing quota policy.
        synchronized (storage) {
            long existing = "DAY".equals(album.kind())
                    ? photos.countByOwnerEmailAndKindAndPhotoDate(user.email(), "DAY", album.date())
                    : photos.countByOwnerEmailAndKindAndReferenceId(user.email(), album.kind(), album.targetId());
            if (existing >= FarmPhotoStorage.MAX_PHOTOS_PER_ALBUM) {
                throw new IllegalArgumentException("この写真一覧には5枚まで登録できます。");
            }
            Long total = photos.totalStoredBytes();
            if ((total == null ? 0L : total) + file.getSize()
                    > FarmPhotoStorage.APPLICATION_QUOTA_BYTES) {
                throw new IllegalArgumentException("無料枠の安全上限に近づいたため、追加を停止しています。");
            }
            byte[] content = file.getBytes();
            FarmPhotoStorage.ImageType mime = storage.detectImageType(content);
            String key = storage.upload(content, mime);
            try {
                FarmPhoto photo = new FarmPhoto();
                photo.setOwnerEmail(user.email());
                photo.setKind(album.kind());
                photo.setReferenceId(album.targetId());
                photo.setPhotoDate(album.date());
                photo.setStorageKey(key);
                photo.setContentType(mime.mime());
                photo.setSizeBytes(content.length);
                photo.setCaption(caption.strip());
                photo.setCreatedBy(user.username());
                return photos.save(photo);
            } catch (RuntimeException e) {
                try { storage.delete(key); } catch (RuntimeException ignored) { }
                throw e;
            }
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> photoFile(@PathVariable Long id) {
        FarmPhoto photo = photos.findByIdAndOwnerEmail(id, user.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        byte[] image;
        try {
            image = storage.read(photo.getStorageKey());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "写真を読み込めません。");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(image);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes messages) {
        FarmPhoto photo = photos.findByIdAndOwnerEmail(id, user.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Even if the linked crop/field/work was already deleted, its photo
        // must remain removable from the calendar date gallery.
        Album destination = new Album("DAY", null, photo.getPhotoDate(), "当日の写真");
        try {
            destination = album(photo.getKind(), photo.getReferenceId(), photo.getPhotoDate().toString());
        } catch (ResponseStatusException ignored) {
            // Fall back to the date album when a linked record no longer exists.
        }
        try {
            storage.delete(photo.getStorageKey());
            photos.delete(photo);
            messages.addFlashAttribute("photoSuccess", "写真を削除しました。");
        } catch (IllegalStateException e) {
            messages.addFlashAttribute("photoError", "写真を削除できませんでした。");
        }
        return link(destination);
    }

    private String link(Album album) {
        var builder = UriComponentsBuilder.fromPath("/farm-photos")
                .queryParam("type", album.kind())
                .queryParam("date", album.date());
        if (album.targetId() != null) builder.queryParam("targetId", album.targetId());
        return "redirect:" + builder.build().encode().toUriString();
    }
}
