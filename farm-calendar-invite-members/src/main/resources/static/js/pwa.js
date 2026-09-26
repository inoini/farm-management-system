(function () {
    "use strict";

    var deferredPrompt = null;

    function isIos() {
        return /iphone|ipad|ipod/i.test(navigator.userAgent);
    }

    function isStandalone() {
        return window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;
    }

    function buttons() {
        return Array.prototype.slice.call(document.querySelectorAll("[data-pwa-install]"));
    }

    function refreshButtons() {
        buttons().forEach(function (button) {
            button.hidden = isStandalone() || (!deferredPrompt && !isIos());
        });
    }

    if ("serviceWorker" in navigator) {
        window.addEventListener("load", function () {
            navigator.serviceWorker.register("/service-worker.js", { scope: "/" })
                .then(function (registration) {
                    return registration.update();
                })
                .catch(function (error) {
                    console.error("Service Worker registration failed:", error);
                });
        });
    }

    window.addEventListener("beforeinstallprompt", function (event) {
        event.preventDefault();
        deferredPrompt = event;
        refreshButtons();
    });

    document.addEventListener("DOMContentLoaded", function () {
        refreshButtons();
        buttons().forEach(function (button) {
            button.addEventListener("click", async function () {
                if (isIos() && !deferredPrompt) {
                    alert("iPhoneでは共有ボタンから「ホーム画面に追加」を選択してください。");
                    return;
                }
                if (!deferredPrompt) {
                    alert("インストール準備中です。Chromeでこのページを再読み込みして、もう一度お試しください。");
                    return;
                }
                deferredPrompt.prompt();
                try {
                    await deferredPrompt.userChoice;
                } catch (e) {
                    console.error(e);
                }
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
