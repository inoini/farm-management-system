"use strict";
document.addEventListener("DOMContentLoaded", () => {
    const weight = document.querySelector('[name="baseWeightKg"]');
    const price = document.querySelector('[name="basePrice"]');
    const count = document.querySelector('[name="packageCount"]');
    const amount = document.querySelector('[name="amount"]');
    const preview = document.getElementById("sales-price-preview");
    if (!weight || !price || !count || !amount || !preview) return;
    const inputs = [weight, price, count];
    let calculated = false;
    function refresh() {
        const active = inputs.some(input => input.value !== "");
        inputs.forEach(input => { input.required = active; });
        amount.readOnly = active;
        amount.required = !active;
        count.setCustomValidity("");
        if (!active) {
            if (calculated) amount.value = "";
            calculated = false;
            preview.textContent = "";
            return;
        }
        calculated = true;
        amount.value = "";
        if (inputs.some(input => input.value === "" || !input.validity.valid)) {
            preview.textContent = "重量・価格・販売数量を入力すると売上金額を自動計算します。";
            return;
        }
        const total = Number(price.value) * Number(count.value);
        if (!Number.isSafeInteger(total) || total > 999999999999) {
            count.setCustomValidity("売上金額の合計は999999999999円以下にしてください。");
            preview.textContent = "売上金額が上限を超えています。";
            return;
        }
        const totalWeight = Number((Number(weight.value) * Number(count.value)).toFixed(3));
        amount.value = String(total);
        preview.textContent = `${weight.value}kgあたり${Number(price.value).toLocaleString("ja-JP")}円 × ${count.value}セット → 合計${totalWeight.toLocaleString("ja-JP", {maximumFractionDigits: 3})}kg・${total.toLocaleString("ja-JP")}円`;
    }
    inputs.forEach(input => input.addEventListener("input", refresh));
    refresh();
});
