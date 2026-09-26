const CACHE_PREFIX = "farm-system-";
const CACHE_NAME = "farm-system-20260927-install-fix-1";

self.addEventListener("install", (event) => {
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(
                keys
                    .filter((key) => key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME)
                    .map((key) => caches.delete(key))
            ))
            .then(() => self.clients.claim())
    );
});

// Network-first passthrough. Keeping a fetch handler makes this compatible
// with older Chromium PWA installability checks without caching login pages.
self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET") return;
    event.respondWith(fetch(request));
});
