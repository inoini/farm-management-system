(() => {
    "use strict";

    document.addEventListener("DOMContentLoaded", () => {
        const mapElement = document.getElementById("fieldMap");
        const messageElement = document.getElementById("fieldMapMessage");

        if (!mapElement) {
            return;
        }

        if (typeof window.L === "undefined") {
            showMessage(messageElement, "地図を読み込めませんでした。通信状況を確認してください。");
            return;
        }

        const defaultCenter = [35.6074, 140.1065];
        const map = window.L.map(mapElement, {
            center: defaultCenter,
            zoom: 9,
            minZoom: 5,
            maxZoom: 18,
            scrollWheelZoom: true
        });

        const tileLayer = window.L.tileLayer(
            "https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png",
            {
                maxZoom: 18,
                attribution: '<a href="https://maps.gsi.go.jp/development/ichiran.html" target="_blank" rel="noopener noreferrer">地理院タイル</a>'
            }
        ).addTo(map);

        tileLayer.on("tileerror", () => {
            showMessage(messageElement, "地図画像の一部を読み込めませんでした。");
        });

        const markerButtons = Array.from(document.querySelectorAll("[data-field-marker]"));
        const markers = [];

        markerButtons.forEach((button) => {
            const latitude = Number.parseFloat(button.dataset.latitude);
            const longitude = Number.parseFloat(button.dataset.longitude);

            if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
                return;
            }

            const marker = window.L.marker([latitude, longitude], {
                keyboard: true,
                riseOnHover: true,
                title: button.dataset.fieldName || "圃場"
            }).addTo(map);

            marker.bindPopup(createPopup(button.dataset));
            markers.push(marker);

            const activate = () => {
                markerButtons.forEach((item) => item.classList.remove("is-active"));
                button.classList.add("is-active");
            };

            button.addEventListener("click", () => {
                map.flyTo([latitude, longitude], Math.max(map.getZoom(), 15), {
                    duration: 0.6
                });
                marker.openPopup();
                activate();
            });

            marker.on("click", activate);
        });

        if (markers.length === 1) {
            map.setView(markers[0].getLatLng(), 15);
            markers[0].openPopup();
        } else if (markers.length > 1) {
            const markerGroup = window.L.featureGroup(markers);
            map.fitBounds(markerGroup.getBounds(), {
                padding: [40, 40],
                maxZoom: 15
            });
        } else {
            showMessage(
                messageElement,
                "地図に表示できる圃場がありません。住所を詳しく入力して保存し直してください。"
            );
        }

        window.setTimeout(() => map.invalidateSize(), 100);
    });

    function createPopup(field) {
        const container = document.createElement("div");
        container.className = "field-map-popup";

        const title = document.createElement("strong");
        title.textContent = field.fieldName || "圃場";
        container.append(title);

        const address = document.createElement("p");
        address.textContent = field.address || "住所未登録";
        container.append(address);

        const details = document.createElement("dl");
        appendDetail(details, "作物", field.crop);
        appendDetail(details, "面積", field.area ? `${field.area}a` : "");
        if (details.children.length > 0) {
            container.append(details);
        }

        if (field.editUrl) {
            const editLink = document.createElement("a");
            editLink.href = field.editUrl;
            editLink.textContent = "この圃場を編集";
            container.append(editLink);
        }

        return container;
    }

    function appendDetail(list, label, value) {
        if (!value) {
            return;
        }

        const term = document.createElement("dt");
        term.textContent = label;
        const description = document.createElement("dd");
        description.textContent = value;
        list.append(term, description);
    }

    function showMessage(element, message) {
        if (!element) {
            return;
        }
        element.textContent = message;
        element.hidden = false;
    }
})();