/* Optional enhancement for the existing full-page multipart upload form.
 * Compression runs before submitting; without JavaScript the existing server path still works. */
(function () {
    "use strict";
    document.addEventListener("DOMContentLoaded", function () {
        const form = document.querySelector(".photo-upload-form");
        if (!form) return;
        const fileInput = form.querySelector('input[name="photo"]');
        const button = form.querySelector('button[type="submit"]');
        const status = document.getElementById("photo-compression-status");
        if (!fileInput || !button) return;
        fileInput.addEventListener("change", function () { form.dataset.compressionDone = ""; });
        form.addEventListener("submit", async function (event) {
            if (form.dataset.compressionDone === "yes") return;
            event.preventDefault();
            const original = fileInput.files && fileInput.files[0];
            if (!original) return;
            button.disabled = true;
            if (status) {
                status.hidden = false;
                status.textContent = "写真を自動圧縮しています。";
            }
            try {
                if (!window.FarmPhotoCompression) {
                    throw new Error("画像圧縮機能を読み込めませんでした。再読み込みしてください。");
                }
                const optimized = await window.FarmPhotoCompression.compress(original);
                if (optimized !== original) {
                    const transfer = new DataTransfer();
                    transfer.items.add(optimized);
                    fileInput.files = transfer.files;
                }
                form.dataset.compressionDone = "yes";
                if (status) status.textContent = "写真を保存しています。";
                form.requestSubmit();
            } catch (error) {
                if (status) {
                    status.hidden = false;
                    status.textContent = error.message || "写真を圧縮できませんでした。";
                }
            } finally {
                button.disabled = false;
            }
        });
    });
})();
