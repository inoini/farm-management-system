/* Lossy photo optimization before upload. Original files and old server photos are unchanged. */
(function () {
    "use strict";
    const MAX_PIXELS = 1600;
    const MAX_UPLOAD = 5 * 1024 * 1024;
    const ALLOWED = ["image/jpeg", "image/png", "image/webp"];
    function sizeLabel(bytes) {
        return bytes >= 1024 * 1024 ? (bytes / 1048576).toFixed(1) + "MB"
                                   : Math.max(1, Math.round(bytes / 1024)) + "KB";
    }
    async function decodeImage(file) {
        if (typeof createImageBitmap === "function") return createImageBitmap(file);
        const url = URL.createObjectURL(file);
        try {
            return await new Promise(function (resolve, reject) {
                const img = new Image();
                img.onload = function () { resolve(img); };
                img.onerror = function () { reject(new Error("画像を読み込めませんでした。")); };
                img.src = url;
            });
        } finally {
            URL.revokeObjectURL(url);
        }
    }
    async function compress(file) {
        if (!file || !ALLOWED.includes(file.type)) {
            throw new Error("JPEG・PNG・WebPの写真を選んでください。");
        }
        let image, canvas;
        try {
            image = await decodeImage(file);
            const originalWidth = image.width || image.naturalWidth;
            const originalHeight = image.height || image.naturalHeight;
            if (!originalWidth || !originalHeight) throw new Error("写真を読み込めませんでした。");
            const scale = Math.min(1, MAX_PIXELS / Math.max(originalWidth, originalHeight));
            canvas = document.createElement("canvas");
            canvas.width = Math.max(1, Math.round(originalWidth * scale));
            canvas.height = Math.max(1, Math.round(originalHeight * scale));
            const context = canvas.getContext("2d");
            if (!context) throw new Error("画像圧縮に対応していないブラウザです。");
            const mime = file.type === "image/png" ? "image/png" : "image/jpeg";
            if (mime === "image/jpeg") {
                context.fillStyle = "#ffffff";
                context.fillRect(0, 0, canvas.width, canvas.height);
            }
            context.drawImage(image, 0, 0, canvas.width, canvas.height);
            const blob = await new Promise(function (resolve) {
                canvas.toBlob(resolve, mime, 0.76);
            });
            if (blob && blob.size > 0 && blob.size < file.size && blob.size <= MAX_UPLOAD) {
                const newName = (file.name || "photo").replace(/\.[^.]+$/, "") +
                    (mime === "image/png" ? ".png" : ".jpg");
                return new File([blob], newName, { type: mime, lastModified: Date.now() });
            }
        } catch (e) {
            // Existing small, valid files remain usable on older devices.
            if (file.size > MAX_UPLOAD) {
                throw new Error("写真の自動圧縮に失敗しました。別の写真を選んでください。");
            }
            return file;
        } finally {
            if (image && typeof image.close === "function") image.close();
            if (canvas) { canvas.width = 0; canvas.height = 0; }
        }
        if (file.size <= MAX_UPLOAD) return file;
        throw new Error("圧縮しても5MBを超えます。別の写真を選んでください。");
    }
    window.FarmPhotoCompression = { compress: compress, sizeLabel: sizeLabel };
})();
