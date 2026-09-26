"use strict";
function farmCsrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !header || !token.content || !header.content) {
        throw new Error("ページを再読み込みしてから操作してください。");
    }
    return { [header.content]: token.content };
}
// A restored browser-history page must re-check its session after logout.
window.addEventListener("pageshow", event => {
    if (event.persisted) window.location.reload();
});
