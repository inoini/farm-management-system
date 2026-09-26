const CACHE_NAME = "farm-system-20260926-members-ui-1";

const STATIC_ASSETS = [
    "/css/calendar-home.css?v=20260925-4",
    "/css/discord-theme.css?v=20260922-1",
    "/manifest.json",
    "/icons/icon-192.png",
    "/icons/icon-512.png",
    "/css/style.css?v=20260922-1",
    "/css/professional-ui.css?v=20260922-1",
    "/css/calendar-dashboard.css?v=20260922-1",
    "/css/operations.css?v=20260922-1",
    "/js/script.js?v=20260925-3"
];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => cache.addAll(STATIC_ASSETS))
    );

    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(
                keys
                    .filter((key) => key !== CACHE_NAME)
                    .map((key) => caches.delete(key))
            ))
            .then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    const request = event.request;

    if (request.method !== "GET") {
        return;
    }

    // ダッシュボードや一覧はDBの最新値を表示するため、HTMLをキャッシュしない。
    if (request.mode === "navigate") {
        event.respondWith(fetch(request));
        return;
    }

    const url = new URL(request.url);
    const isStaticAsset = url.origin === self.location.origin
        && ["style", "script", "font", "image", "manifest"].includes(request.destination);

    if (!isStaticAsset) {
        return;
    }

    // CSS・JavaScriptはネットワークを優先し、オフライン時だけキャッシュを使う。
    event.respondWith(
        fetch(request)
            .then((response) => {
                if (response && response.ok) {
                    const copy = response.clone();
                    caches.open(CACHE_NAME)
                        .then((cache) => cache.put(request, copy));
                }
                return response;
            })
            .catch(() => caches.match(request))
    );
});
