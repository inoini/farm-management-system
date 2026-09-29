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

  function parseYen(text) {
    var normalized = String(text || "").replace(/[^0-9-]/g, "");
    return normalized ? Number(normalized) : 0;
  }

  function formatYen(value) {
    return Math.round(value).toLocaleString("ja-JP") + "円";
  }

  function simplifyManagementScreen() {
    if (!document.body.classList.contains("management-screen")) return;

    var annualForecast = document.getElementById("annual-forecast");
    if (annualForecast) annualForecast.remove();

    var annualForecastLink = document.querySelector('.analysis-nav a[href="#annual-forecast"]');
    if (annualForecastLink) annualForecastLink.remove();

    document.querySelectorAll(".management-screen .support-grid").forEach(function (grid) {
      grid.remove();
    });

    var budgetSection = document.getElementById("budget");
    var table = budgetSection ? budgetSection.querySelector("table.analysis-table") : null;
    if (!table || table.querySelector('[data-budget-total="true"]')) return;

    var rows = Array.prototype.slice.call(table.querySelectorAll("tbody tr"));
    if (rows.length === 0) return;

    var totals = [0, 0, 0, 0, 0, 0];
    rows.forEach(function (row) {
      var cells = row.querySelectorAll("td");
      for (var i = 1; i <= 6 && i < cells.length; i += 1) {
        totals[i - 1] += parseYen(cells[i].textContent);
      }
    });

    var tfoot = table.tFoot || table.createTFoot();
    var totalRow = tfoot.insertRow();
    totalRow.setAttribute("data-budget-total", "true");
    totalRow.style.fontWeight = "900";
    totalRow.style.background = "var(--discord-surface-raised, #eef4ee)";
    totalRow.style.borderTop = "2px solid var(--discord-border, #cfd9d0)";

    var labelCell = totalRow.insertCell();
    labelCell.textContent = "合計";

    totals.forEach(function (total, index) {
      var cell = totalRow.insertCell();
      cell.className = "money-cell";
      cell.textContent = formatYen(total);
      if (index === 4 || index === 5) {
        cell.classList.add(total >= 0 ? "profit-positive" : "profit-negative");
      }
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
    simplifyManagementScreen();
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
