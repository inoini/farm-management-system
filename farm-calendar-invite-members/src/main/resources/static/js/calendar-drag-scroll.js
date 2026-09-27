(function () {
    "use strict";

    function initCalendarDragScroll() {
        const panel = document.querySelector(".calendar-month-panel");
        if (!panel) return;

        panel.classList.add("calendar-drag-scroll-enabled");

        // 指・ホイール・タッチパッドはブラウザ標準の縦スクロールに任せる。
        // マウス左ドラッグだけ、ページ本体を軽量に上下移動する。
        let mouseDown = false;
        let dragging = false;
        let startX = 0;
        let startY = 0;
        let lastY = 0;
        let suppressClickUntil = 0;
        let frame = 0;
        let pendingDeltaY = 0;
        const threshold = 6;
        const scrollRoot = document.scrollingElement || document.documentElement;

        function applyMouseScroll() {
            frame = 0;
            if (!pendingDeltaY) return;
            scrollRoot.scrollTop = Math.max(0, scrollRoot.scrollTop - pendingDeltaY);
            pendingDeltaY = 0;
        }

        function onMouseMove(event) {
            if (!mouseDown) return;

            const dx = event.clientX - startX;
            const dy = event.clientY - startY;

            if (!dragging) {
                if (Math.abs(dy) <= threshold || Math.abs(dy) < Math.abs(dx)) return;
                dragging = true;
                panel.classList.add("is-drag-scrolling");
                document.documentElement.classList.add("calendar-mouse-dragging");
            }

            event.preventDefault();
            pendingDeltaY += event.clientY - lastY;
            lastY = event.clientY;
            suppressClickUntil = Date.now() + 350;

            if (!frame) frame = window.requestAnimationFrame(applyMouseScroll);
        }

        function finishMouseDrag() {
            if (!mouseDown) return;
            mouseDown = false;
            if (dragging) suppressClickUntil = Date.now() + 350;
            dragging = false;
            panel.classList.remove("is-drag-scrolling");
            document.documentElement.classList.remove("calendar-mouse-dragging");
            window.removeEventListener("mousemove", onMouseMove, true);
            window.removeEventListener("mouseup", finishMouseDrag, true);
            if (frame) {
                window.cancelAnimationFrame(frame);
                frame = 0;
            }
            if (pendingDeltaY) applyMouseScroll();
        }

        panel.addEventListener("mousedown", function (event) {
            if (event.button !== 0) return;
            if (event.target.closest("button, a, input, select, textarea, label")) return;

            mouseDown = true;
            dragging = false;
            startX = event.clientX;
            startY = event.clientY;
            lastY = event.clientY;
            pendingDeltaY = 0;
            window.addEventListener("mousemove", onMouseMove, true);
            window.addEventListener("mouseup", finishMouseDrag, true);
        });

        panel.addEventListener("click", function (event) {
            if (Date.now() < suppressClickUntil) {
                event.preventDefault();
                event.stopPropagation();
                event.stopImmediatePropagation();
            }
        }, true);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initCalendarDragScroll, { once: true });
    } else {
        initCalendarDragScroll();
    }
})();
