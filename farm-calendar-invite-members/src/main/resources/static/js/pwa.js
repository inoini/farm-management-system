(function () {
    "use strict";

    if ("serviceWorker" in navigator) {
        window.addEventListener("load", function () {
            navigator.serviceWorker.register("/service-worker.js?v=20260927-green-icon-2")
                .then(function (registration) { registration.update(); })
                .catch(function (error) { console.log("PWA Error:", error); });
        });
    }

    var deferredPrompt = null;
    var installButtons = [];

    function isIos() {
        return /iphone|ipad|ipod/i.test(navigator.userAgent);
    }

    function isStandalone() {
        return window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;
    }

    function refreshButtons() {
        installButtons = Array.prototype.slice.call(document.querySelectorAll("[data-pwa-install]"));
        installButtons.forEach(function (button) {
            if (isStandalone()) {
                button.hidden = true;
                return;
            }
            if (deferredPrompt || isIos()) {
                button.hidden = false;
            }
        });
    }

    window.addEventListener("beforeinstallprompt", function (event) {
        event.preventDefault();
        deferredPrompt = event;
        refreshButtons();
    });

    document.addEventListener("DOMContentLoaded", function () {
        refreshButtons();
        installButtons.forEach(function (button) {
            button.addEventListener("click", async function () {
                if (isIos() && !deferredPrompt) {
                    alert("iPhoneではSafariの共有ボタンを押し、「ホーム画面に追加」を選択してください。");
                    return;
                }
                if (!deferredPrompt) return;
                deferredPrompt.prompt();
                try { await deferredPrompt.userChoice; } catch (e) { /* noop */ }
                deferredPrompt = null;
                refreshButtons();
            });
        });
    });

    window.addEventListener("appinstalled", function () {
        deferredPrompt = null;
        refreshButtons();
    });
})();
