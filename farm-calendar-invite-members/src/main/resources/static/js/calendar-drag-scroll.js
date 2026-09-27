(function () {
    "use strict";

    function initCalendarDragScroll() {
        const panel = document.querySelector(".calendar-month-panel");
        if (!panel) return;

        panel.classList.add("calendar-drag-scroll-enabled");

        let pointerId = null;
        let pointerType = "";
        let startX = 0;
        let startY = 0;
        let lastY = 0;
        let moved = false;
        let mouseDragging = false;
        let suppressClickUntil = 0;
        const threshold = 7;

        function markMoved() {
            moved = true;
            suppressClickUntil = Date.now() + 450;
        }

        panel.addEventListener("pointerdown", function (event) {
            if (event.pointerType === "mouse" && event.button !== 0) return;

            pointerId = event.pointerId;
            pointerType = event.pointerType || "mouse";
            startX = event.clientX;
            startY = event.clientY;
            lastY = event.clientY;
            moved = false;
            mouseDragging = false;

            if (pointerType === "mouse" || pointerType === "pen") {
                try { panel.setPointerCapture(pointerId); } catch (ignore) {}
            }
        });

        panel.addEventListener("pointermove", function (event) {
            if (pointerId === null || event.pointerId !== pointerId) return;

            const dx = event.clientX - startX;
            const dy = event.clientY - startY;

            if (!moved && (Math.abs(dx) > threshold || Math.abs(dy) > threshold)) {
                markMoved();
            }

            // 指は touch-action: pan-y に任せ、ページ本来の縦スワイプを使う。
            if (pointerType === "touch") return;

            // マウス／ペンは縦方向にドラッグしたときページをスクロールする。
            if (!mouseDragging) {
                if (Math.abs(dy) <= threshold || Math.abs(dy) < Math.abs(dx)) return;
                mouseDragging = true;
                panel.classList.add("is-drag-scrolling");
            }

            event.preventDefault();
            const deltaY = event.clientY - lastY;
            lastY = event.clientY;
            if (deltaY !== 0) {
                window.scrollBy(0, -deltaY);
                markMoved();
            }
        }, { passive: false });

        function endPointer(event) {
            if (pointerId === null || (event && event.pointerId !== pointerId)) return;

            if (moved || mouseDragging) suppressClickUntil = Date.now() + 450;

            if ((pointerType === "mouse" || pointerType === "pen") && pointerId !== null) {
                try { panel.releasePointerCapture(pointerId); } catch (ignore) {}
            }

            pointerId = null;
            pointerType = "";
            moved = false;
            mouseDragging = false;
            panel.classList.remove("is-drag-scrolling");
        }

        panel.addEventListener("pointerup", endPointer);
        panel.addEventListener("pointercancel", endPointer);
        panel.addEventListener("lostpointercapture", function () {
            pointerId = null;
            pointerType = "";
            moved = false;
            mouseDragging = false;
            panel.classList.remove("is-drag-scrolling");
        });

        panel.addEventListener("click", function (event) {
            if (Date.now() < suppressClickUntil) {
                event.preventDefault();
                event.stopPropagation();
                event.stopImmediatePropagation();
            }
        }, true);

        // ホイール／タッチパッドの縦操作もカレンダー内部ではなくページ全体を動かす。
        panel.addEventListener("wheel", function (event) {
            if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return;
            event.preventDefault();
            window.scrollBy(0, event.deltaY);
        }, { passive: false });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initCalendarDragScroll, { once: true });
    } else {
        initCalendarDragScroll();
    }
})();
