/* Shared private-photo picker for the day popup and crop viewer.
 * Camera and file selection use separate native inputs so Android/iPhone can choose. */
(function () {
    "use strict";

    function by(root, selector) { return root.querySelector(selector); }
    function selectedTarget(widget) {
        const params = new URLSearchParams({ type: widget.dataset.photoType || "DAY" });
        if (widget.dataset.photoTarget) params.set("targetId", widget.dataset.photoTarget);
        if (widget.dataset.photoDate) params.set("date", widget.dataset.photoDate);
        return params;
    }
    async function jsonOrError(response) {
        let payload;
        try { payload = await response.json(); } catch (ignored) {
            throw new Error("通信またはログインの状態を確認してください。");
        }
        if (!response.ok) throw new Error(payload.error || "写真を取得・保存できませんでした。");
        return payload;
    }
    function message(widget, text, error) {
        const node = by(widget, ".farm-photo-message");
        node.textContent = text;
        node.classList.toggle("is-error", Boolean(error));
    }
    function formatPhotoDate(value) {
        const matched = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "");
        return matched ? Number(matched[1]) + "年" + Number(matched[2]) + "月" + Number(matched[3]) + "日" : "日付未設定";
    }
    function todayLocal() {
        const now = new Date();
        const pad = function (n) { return String(n).padStart(2, "0"); };
        return now.getFullYear() + "-" + pad(now.getMonth() + 1) + "-" + pad(now.getDate());
    }
    function createThumb(photo) {
        const card = document.createElement("figure");
        card.className = "farm-photo-thumb";
        const link = document.createElement("a");
        link.href = photo.url;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.setAttribute("aria-label", "保存した写真を拡大");
        const img = document.createElement("img");
        img.src = photo.url;
        img.alt = photo.caption ? photo.caption : "農場の写真";
        img.loading = "lazy";
        link.appendChild(img);
        card.appendChild(link);
        const description = document.createElement("figcaption");
        const photoDate = document.createElement("strong");
        photoDate.className = "farm-photo-taken-date";
        photoDate.textContent = "撮影日： " + formatPhotoDate(photo.date);
        description.appendChild(photoDate);
        if (photo.caption) {
            const caption = document.createElement("span");
            caption.className = "farm-photo-caption-text";
            caption.textContent = photo.caption;
            description.appendChild(caption);
        }
        card.appendChild(description);
        return card;
    }
    function setup(widget) {
        const camera = by(widget, ".farm-photo-camera-input");
        const files = by(widget, ".farm-photo-file-input");
        const cameraButton = by(widget, ".farm-photo-camera-button");
        const fileButton = by(widget, ".farm-photo-file-button");
        const saveButton = by(widget, ".farm-photo-save-button");
        const gallery = by(widget, ".farm-photo-widget-gallery");
        const preview = by(widget, ".farm-photo-selected");
        const previewImage = by(widget, ".farm-photo-preview-image");
        const fileName = by(widget, ".farm-photo-filename");
        const caption = by(widget, ".farm-photo-caption");
        const captureDate = by(widget, ".farm-photo-capture-date");
        const fullGallery = by(widget, ".farm-photo-full-gallery");
        let currentFile = null;
        let objectUrl = null;
        let generation = 0;
        let uploading = false;
        let uploadAllowed = false;

        function clearPreview() {
            currentFile = null;
            camera.value = "";
            files.value = "";
            if (objectUrl) URL.revokeObjectURL(objectUrl);
            objectUrl = null;
            previewImage.removeAttribute("src");
            preview.hidden = true;
            fileName.textContent = "";
            saveButton.disabled = true;
        }
        function choose(input, other) {
            const file = input.files && input.files[0];
            if (!file) return;
            other.value = "";
            clearPreview();
            if (!["image/jpeg", "image/png", "image/webp"].includes(file.type) || file.size > 5 * 1024 * 1024) {
                message(widget, "JPEG・PNG・WebP形式の5MB以下の写真を選んでください。", true);
                return;
            }
            currentFile = file;
            objectUrl = URL.createObjectURL(file);
            previewImage.src = objectUrl;
            fileName.textContent = file.name;
            preview.hidden = false;
            saveButton.disabled = !uploadAllowed || uploading;
            message(widget, "写真を確認して「写真を保存」を押してください。", false);
        }
        cameraButton.addEventListener("click", function () { camera.click(); });
        fileButton.addEventListener("click", function () { files.click(); });
        camera.addEventListener("change", function () { choose(camera, files); });
        files.addEventListener("change", function () { choose(files, camera); });

        async function load() {
            const request = ++generation;
            clearPreview();
            caption.value = "";
            message(widget, "写真を読み込んでいます。", false);
            fullGallery.href = "/farm-photos?" + selectedTarget(widget).toString();
            gallery.replaceChildren();
            try {
                const response = await fetch("/farm-photos/api?" + selectedTarget(widget).toString(),
                    { credentials: "same-origin", cache: "no-store" });
                const data = await jsonOrError(response);
                if (request !== generation) return;
                uploadAllowed = Boolean(data.canUpload);
                cameraButton.disabled = !uploadAllowed;
                fileButton.disabled = !uploadAllowed;
                saveButton.disabled = true;
                (data.images || []).forEach(function (photo) {
                    gallery.appendChild(createThumb(photo));
                });
                if (!data.photoEnabled) {
                    message(widget, "写真の保存先が未設定です。Renderの保存先設定を確認してください。", true);
                } else if (!uploadAllowed) {
                    message(widget, "この一覧の写真上限（5枚）か保存容量の上限に達しています。", false);
                } else if (!data.images || data.images.length === 0) {
                    message(widget, "まだ写真はありません。撮影またはファイルから追加できます。", false);
                } else {
                    message(widget, "保存済み写真を表示しています。新しく追加することもできます。", false);
                }
            } catch (e) {
                if (request !== generation) return;
                uploadAllowed = false;
                cameraButton.disabled = true;
                fileButton.disabled = true;
                message(widget, e.message || "写真を読み込めませんでした。", true);
            }
        }
        saveButton.addEventListener("click", async function () {
            if (!currentFile || !uploadAllowed || uploading) return;
            uploading = true;
            saveButton.disabled = true;
            cameraButton.disabled = true;
            fileButton.disabled = true;
            const form = new FormData();
            selectedTarget(widget).forEach(function (value, key) { form.append(key, value); });
            if (captureDate) {
                if (!captureDate.value || !captureDate.checkValidity()) {
                    uploading = false;
                    cameraButton.disabled = !uploadAllowed;
                    fileButton.disabled = !uploadAllowed;
                    saveButton.disabled = false;
                    message(widget, "撮影日を確認して入力してください。", true);
                    captureDate.focus();
                    return;
                }
                form.set("date", captureDate.value);
            }
            form.append("photo", currentFile, currentFile.name);
            form.append("caption", caption.value);
            const csrf = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            const headers = {};
            if (csrf && header) headers[header.content] = csrf.content;
            message(widget, "写真を保存しています。", false);
            try {
                const response = await fetch("/farm-photos/api/upload", {
                    method: "POST", headers: headers, credentials: "same-origin", body: form
                });
                await jsonOrError(response);
                uploading = false;
                await load();
                message(widget, "写真を保存しました。一覧に掲載されています。", false);
            } catch (e) {
                uploading = false;
                cameraButton.disabled = !uploadAllowed;
                fileButton.disabled = !uploadAllowed;
                saveButton.disabled = !currentFile || !uploadAllowed;
                message(widget, e.message || "写真を保存できませんでした。", true);
            }
        });
        widget.farmPhotoLoad = load;
        widget.farmPhotoClear = clearPreview;
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".farm-photo-widget").forEach(setup);
        const calendarWidget = document.querySelector('.farm-photo-widget[data-photo-type="DAY"]');
        window.farmPhotosOpenDate = function (date) {
            if (!calendarWidget || !date) return;
            calendarWidget.dataset.photoDate = date;
            const title = by(calendarWidget, ".farm-photo-date-label");
            if (title) title.textContent = date;
            calendarWidget.farmPhotoLoad();
        };
        const cropDialog = document.getElementById("crop-photo-dialog");
        const cropWidget = cropDialog && by(cropDialog, ".farm-photo-widget");
        let previouslyFocused = null;
        document.querySelectorAll("[data-crop-photo-open]").forEach(function (button) {
            button.addEventListener("click", function () {
                if (!cropDialog || !cropWidget) return;
                previouslyFocused = document.activeElement;
                cropWidget.dataset.photoTarget = button.dataset.cropPhotoOpen;
                const cropDateInput = by(cropWidget, ".farm-photo-capture-date");
                if (cropDateInput) {
                    cropDateInput.max = todayLocal();
                    cropDateInput.value = todayLocal();
                }
                const heading = by(cropDialog, ".crop-photo-dialog-heading");
                if (heading) heading.textContent = (button.dataset.cropName || "作物") + "の写真";
                cropDialog.hidden = false;
                cropDialog.setAttribute("aria-hidden", "false");
                cropWidget.farmPhotoLoad();
                by(cropDialog, ".crop-photo-dialog-close").focus();
            });
        });
        function closeCrop() {
            if (!cropDialog || cropDialog.hidden) return;
            cropDialog.hidden = true;
            cropDialog.setAttribute("aria-hidden", "true");
            if (cropWidget) cropWidget.farmPhotoClear();
            if (previouslyFocused) previouslyFocused.focus();
        }
        if (cropDialog) {
            by(cropDialog, ".crop-photo-dialog-close").addEventListener("click", closeCrop);
            cropDialog.addEventListener("click", function (event) {
                if (event.target === cropDialog) closeCrop();
            });
            document.addEventListener("keydown", function (event) {
                if (event.key === "Escape" && !cropDialog.hidden) closeCrop();
            });
        }
    });
})();
