(() => {
    "use strict";

    const normalize = (value) => String(value ?? "")
        .normalize("NFKC")
        .toLocaleLowerCase("ja-JP")
        .trim();

    document.addEventListener("DOMContentLoaded", () => {
        const tableBody = document.getElementById("fieldTableBody");
        if (!tableBody) {
            return;
        }

        const rows = Array.from(tableBody.querySelectorAll("[data-field-row]"));
        const toolbar = document.querySelector("[data-field-toolbar]");
        const searchInput = document.getElementById("fieldSearch");
        const cropFilter = document.getElementById("fieldCropFilter");
        const resetButton = document.getElementById("fieldFilterReset");
        const noResultsRow = document.getElementById("fieldNoResults");
        const totalCount = document.getElementById("fieldTotalCount");
        const totalArea = document.getElementById("fieldTotalArea");
        const cropCount = document.getElementById("fieldCropCount");
        const resultCount = document.getElementById("fieldResultCount");
        const collator = new Intl.Collator("ja", {
            numeric: true,
            sensitivity: "base"
        });

        if (toolbar) {
            toolbar.hidden = rows.length === 0;
        }

        const crops = [...new Set(
            rows
                .map((row) => row.dataset.crop?.trim())
                .filter(Boolean)
        )].sort(collator.compare);

        crops.forEach((crop) => {
            const option = document.createElement("option");
            option.value = crop;
            option.textContent = crop;
            cropFilter?.append(option);
        });

        const areaSum = rows.reduce((sum, row) => {
            const value = Number.parseFloat(row.dataset.area);
            return sum + (Number.isFinite(value) ? value : 0);
        }, 0);

        if (totalCount) {
            totalCount.textContent = rows.length.toLocaleString("ja-JP");
        }
        if (totalArea) {
            totalArea.textContent = areaSum.toLocaleString("ja-JP", {
                maximumFractionDigits: 2
            });
        }
        if (cropCount) {
            cropCount.textContent = crops.length.toLocaleString("ja-JP");
        }

        const applyFilters = () => {
            const keyword = normalize(searchInput?.value);
            const selectedCrop = normalize(cropFilter?.value);
            let visibleCount = 0;

            rows.forEach((row) => {
                const searchableText = normalize([
                    row.dataset.name,
                    row.dataset.location,
                    row.dataset.crop,
                    row.textContent
                ].join(" "));
                const matchesKeyword = !keyword || searchableText.includes(keyword);
                const matchesCrop = !selectedCrop || normalize(row.dataset.crop) === selectedCrop;
                const isVisible = matchesKeyword && matchesCrop;

                row.hidden = !isVisible;
                if (isVisible) {
                    visibleCount += 1;
                }
            });

            if (resultCount) {
                resultCount.textContent = visibleCount.toLocaleString("ja-JP");
            }
            if (noResultsRow) {
                noResultsRow.hidden = rows.length === 0 || visibleCount !== 0;
            }
        };

        searchInput?.addEventListener("input", applyFilters);
        cropFilter?.addEventListener("change", applyFilters);
        resetButton?.addEventListener("click", () => {
            if (searchInput) {
                searchInput.value = "";
            }
            if (cropFilter) {
                cropFilter.value = "";
            }
            applyFilters();
            searchInput?.focus();
        });

        let currentSort = "";
        let sortDirection = 1;

        document.querySelectorAll("[data-sort]").forEach((button) => {
            button.addEventListener("click", () => {
                const key = button.dataset.sort;
                sortDirection = currentSort === key ? sortDirection * -1 : 1;
                currentSort = key;

                rows.sort((left, right) => {
                    if (key === "area") {
                        const leftArea = Number.parseFloat(left.dataset.area) || 0;
                        const rightArea = Number.parseFloat(right.dataset.area) || 0;
                        return (leftArea - rightArea) * sortDirection;
                    }
                    return collator.compare(left.dataset[key] || "", right.dataset[key] || "")
                        * sortDirection;
                });

                rows.forEach((row) => tableBody.append(row));
                document.querySelectorAll("[data-sort-column]").forEach((header) => {
                    header.removeAttribute("aria-sort");
                });
                document.querySelector(`[data-sort-column="${key}"]`)
                    ?.setAttribute("aria-sort", sortDirection === 1 ? "ascending" : "descending");
            });
        });

        document.querySelectorAll(".field-delete-button").forEach((link) => {
            link.addEventListener("click", (event) => {
                const fieldName = link.dataset.fieldName || "この圃場";
                const confirmed = window.confirm(
                    `「${fieldName}」を削除しますか？\nこの操作は取り消せません。`
                );
                if (!confirmed) {
                    event.preventDefault();
                }
            });
        });

        applyFilters();
    });
})();