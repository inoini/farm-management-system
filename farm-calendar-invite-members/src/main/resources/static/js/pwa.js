(function () {
  "use strict";

  var deferredPrompt = null;

  function isIos() { return /iphone|ipad|ipod/i.test(navigator.userAgent); }
  function isAndroid() { return /android/i.test(navigator.userAgent); }
  function isStandalone() {
    return window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;
  }
  function buttons() { return Array.prototype.slice.call(document.querySelectorAll("[data-pwa-install]")); }
  function refreshButtons() {
    buttons().forEach(function (button) {
      button.hidden = isStandalone() || !(deferredPrompt || isIos() || isAndroid());
    });
  }

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", function () {
      navigator.serviceWorker.register("/service-worker.js", { scope: "/" })
        .then(function (registration) { return registration.update(); })
        .catch(function (error) { console.error("Service Worker registration failed:", error); });
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
        if (deferredPrompt) {
          deferredPrompt.prompt();
          try { await deferredPrompt.userChoice; } catch (e) { console.error(e); }
          deferredPrompt = null;
          refreshButtons();
          return;
        }
        if (isIos()) {
          alert("iPhoneでは共有ボタンから『ホーム画面に追加』を選択してください。");
          return;
        }
        if (isAndroid()) {
          alert("Chrome右上の︙メニューから『アプリをインストール』または『ホーム画面に追加』を選択してください。表示されない場合は、ページを再読み込みして30秒ほど待ってからもう一度お試しください。");
        }
      });
    });
  });

  window.addEventListener("appinstalled", function () {
    deferredPrompt = null;
    refreshButtons();
  });
})();
