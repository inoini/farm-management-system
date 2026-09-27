(function () {
    "use strict";

    const cropName = document.getElementById("cropName");
    const variety = document.getElementById("variety");
    const plantingDate = document.getElementById("plantingDate");
    const harvestDate = document.getElementById("harvestDate");
    const fieldName = document.getElementById("fieldName");
    const panel = document.getElementById("cropGuidePanel");
    const cropForm = document.getElementById("cropForm");
    const cropIdInput = document.getElementById("id");

    if (!cropName || !plantingDate || !harvestDate || !panel) return;

    const cropLabel = document.getElementById("cropGuideCrop");
    const topdressing = document.getElementById("cropGuideTopdressing");
    const harvest = document.getElementById("cropGuideHarvest");
    const care = document.getElementById("cropGuideCare");
    const status = document.getElementById("cropGuideStatus");
    const weatherHarvest = document.getElementById("cropWeatherHarvest");
    const weatherStats = document.getElementById("cropWeatherStats");
    const weatherSummary = document.getElementById("cropWeatherSummary");
    const weatherStatus = document.getElementById("cropWeatherStatus");
    const refreshWeatherButton = document.getElementById("refreshWeatherAdvice");
    const topdressingGuideInput = document.getElementById("topdressingGuide");
    const weatherAdjustedInput = document.getElementById("weatherAdjustedHarvestDate");
    const weatherAdviceInput = document.getElementById("weatherAdvice");
    const weatherAnalyzedAtInput = document.getElementById("weatherAnalyzedAt");

    function normalize(value) {
        return String(value || "")
            .normalize("NFKC")
            .toLowerCase()
            .replace(/[ァ-ヶ]/g, function (ch) {
                return String.fromCharCode(ch.charCodeAt(0) - 0x60);
            })
            .replace(/[\s　・･_-]/g, "");
    }

    function dateFromIso(iso) {
        if (!iso) return null;
        const parts = iso.split("-").map(Number);
        if (parts.length !== 3 || parts.some(Number.isNaN)) return null;
        return new Date(parts[0], parts[1] - 1, parts[2], 12, 0, 0);
    }

    function addDays(date, days) {
        const result = new Date(date.getTime());
        result.setDate(result.getDate() + days);
        return result;
    }

    function isoDate(date) {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, "0");
        const d = String(date.getDate()).padStart(2, "0");
        return y + "-" + m + "-" + d;
    }

    function jpDate(date) {
        return (date.getMonth() + 1) + "月" + date.getDate() + "日";
    }

    function rangeText(base, range) {
        if (!base) return "植付後" + range[0] + "〜" + range[1] + "日ごろ";
        return jpDate(addDays(base, range[0])) + "〜" + jpDate(addDays(base, range[1]));
    }

    const profiles = [
        { key: "さつまいも", aliases: ["さつまいも", "甘藷", "かんしょ"], harvest: [120, 150], top: [[30, 45]], care: "活着後は除草を行い、つるが繁り過ぎる場合は窒素過多に注意。必要に応じてつる返しを行います。", varieties: {
            "紅はるか": { harvest: [130, 150], care: "紅はるかは十分に肥大させてから収穫。収穫後に貯蔵・追熟すると甘みが増しやすい品種です。" },
            "シルクスイート": { harvest: [120, 140] }, "べにはるか": { harvest: [130, 150] }
        }},
        { key: "じゃがいも", aliases: ["じゃがいも", "馬鈴薯", "ばれいしょ"], harvest: [90, 110], top: [[25, 35], [45, 55]], care: "草丈10〜15cmを目安に芽かき。追肥とあわせて土寄せし、いもが地表に出ないようにします。", varieties: {
            "キタアカリ": { harvest: [85, 100] }, "男爵": { harvest: [90, 105] }, "男爵薯": { harvest: [90, 105] }, "メークイン": { harvest: [100, 120] }
        }},
        { key: "にんじん", aliases: ["にんじん", "人参"], harvest: [100, 120], top: [[30, 40], [55, 65]], care: "本葉2〜3枚、本葉5〜6枚を目安に間引き。最終株間を確保し、根肩が出たら軽く土寄せします。" },
        { key: "だいこん", aliases: ["だいこん", "大根"], harvest: [60, 90], top: [[20, 30], [40, 50]], care: "本葉の生育に合わせて2〜3回間引き、追肥後に株元へ軽く土寄せします。" },
        { key: "かぶ", aliases: ["かぶ", "蕪"], harvest: [40, 70], top: [[20, 30]], care: "混み合わないよう段階的に間引き、根が太り始める前に株間を確保します。" },
        { key: "とうもろこし", aliases: ["とうもろこし", "玉蜀黍", "コーン"], harvest: [80, 100], top: [[30, 40], [50, 60]], care: "草丈30〜40cm頃と雄穂が見え始める頃が追肥の目安。倒伏防止に土寄せします。" },
        { key: "トマト", aliases: ["とまと", "トマト"], harvest: [60, 90], top: [[20, 30], [45, 60]], care: "わき芽かきと誘引を継続。着果後は草勢を見ながら少量ずつ追肥し、過繁茂を避けます。" },
        { key: "ミニトマト", aliases: ["みにとまと", "ミニトマト"], harvest: [55, 85], top: [[20, 30], [45, 60]], care: "わき芽かき・誘引を行い、実がつき始めてから草勢を見て追肥します。" },
        { key: "なす", aliases: ["なす", "茄子"], harvest: [60, 90], top: [[20, 30], [45, 60]], care: "一番花の下の勢いのよい枝を残して整枝。収穫が始まったら肥切れさせないよう定期的に追肥します。" },
        { key: "きゅうり", aliases: ["きゅうり", "胡瓜"], harvest: [40, 60], top: [[15, 25], [35, 45]], care: "つるを誘引し、収穫開始後は肥切れしやすいため少量ずつ追肥。取り遅れを避けます。" },
        { key: "ピーマン", aliases: ["ぴーまん", "ピーマン", "ぱぷりか", "パプリカ"], harvest: [60, 90], top: [[20, 30], [50, 65]], care: "一番果は若採りして株を充実させ、収穫が続く間は草勢を見て追肥します。" },
        { key: "かぼちゃ", aliases: ["かぼちゃ", "南瓜"], harvest: [90, 120], top: [[20, 30], [45, 55]], care: "つるを整理し、着果位置を管理。果梗がコルク化してきた頃が収穫判断の目安です。" },
        { key: "ズッキーニ", aliases: ["ずっきーに", "ズッキーニ"], harvest: [40, 60], top: [[15, 25], [35, 45]], care: "実が肥大し始めると成長が速いため、若い実をこまめに収穫します。" },
        { key: "オクラ", aliases: ["おくら", "オクラ"], harvest: [50, 70], top: [[20, 30], [45, 60]], care: "収穫が始まったら取り遅れに注意。下葉を整理し、株の勢いを見ながら追肥します。" },
        { key: "すいか", aliases: ["すいか", "西瓜"], harvest: [80, 100], top: [[20, 30], [45, 55]], care: "つる数と着果数を調整し、着果日を記録すると収穫時期を判断しやすくなります。" },
        { key: "メロン", aliases: ["めろん", "メロン"], harvest: [75, 100], top: [[20, 30], [45, 55]], care: "つるを整理し着果数を調整。品種ごとの成熟日数を優先して収穫判断します。" },
        { key: "えだまめ", aliases: ["えだまめ", "枝豆"], harvest: [75, 95], top: [[30, 40]], care: "マメ科は窒素過多に注意。生育が弱い場合だけ少量追肥し、さやが十分ふくらんだら収穫します。" },
        { key: "落花生", aliases: ["らっかせい", "落花生", "ピーナッツ"], harvest: [130, 150], top: [[35, 50]], care: "窒素肥料は控えめにし、開花後は子房柄が土へ入りやすいよう株元をやわらかく保ちます。" },
        { key: "キャベツ", aliases: ["きゃべつ", "キャベツ"], harvest: [90, 120], top: [[20, 30], [45, 55]], care: "活着後と結球開始前が追肥の目安。害虫の食害を早めに確認します。" },
        { key: "はくさい", aliases: ["はくさい", "白菜"], harvest: [70, 100], top: [[20, 30], [40, 50]], care: "活着後と結球前に追肥。結球初期の肥切れと害虫被害に注意します。" },
        { key: "ブロッコリー", aliases: ["ぶろっこりー", "ブロッコリー"], harvest: [80, 110], top: [[20, 30], [45, 55]], care: "活着後と花蕾が見え始める前に追肥。頂花蕾収穫後に側花蕾も収穫できます。" },
        { key: "レタス", aliases: ["れたす", "レタス"], harvest: [55, 75], top: [[20, 30]], care: "活着後に生育を見て少量追肥。高温期は抽だい、過湿期は病害に注意します。" },
        { key: "ほうれんそう", aliases: ["ほうれんそう", "ほうれん草", "菠菜"], harvest: [35, 55], top: [[18, 25]], care: "生育が弱いときだけ少量追肥。草丈20〜25cm前後を収穫の目安にします。" },
        { key: "こまつな", aliases: ["こまつな", "小松菜"], harvest: [25, 40], top: [[15, 20]], care: "短期栽培なので元肥中心。生育が弱い場合のみ少量追肥し、葉が硬くなる前に収穫します。" },
        { key: "ねぎ", aliases: ["ねぎ", "葱", "長ねぎ", "長葱"], harvest: [120, 180], top: [[30, 45], [70, 90]], care: "追肥と土寄せを複数回行い、軟白部を長くします。株元へ一度に土を寄せ過ぎないようにします。" },
        { key: "たまねぎ", aliases: ["たまねぎ", "玉ねぎ", "玉葱"], harvest: [150, 210], top: [[30, 45], [75, 90]], care: "冬〜早春に生育を見て追肥。収穫直前の遅い窒素追肥は貯蔵性を落とすため避けます。" },
        { key: "にんにく", aliases: ["にんにく", "大蒜"], harvest: [220, 260], top: [[45, 60], [120, 150]], care: "冬越し後の生育再開期までに追肥し、春遅くの窒素過多を避けます。" },
        { key: "さといも", aliases: ["さといも", "里芋"], harvest: [150, 210], top: [[45, 60], [80, 100]], care: "追肥にあわせて土寄せ。乾燥に弱いため夏場は土の水分を保ちます。" },
        { key: "しょうが", aliases: ["しょうが", "生姜"], harvest: [180, 240], top: [[60, 80], [100, 120]], care: "生育期に追肥・土寄せ。乾燥と強い直射による地温上昇を防ぐため敷きわら等が有効です。" }
    ];

    const prepared = profiles.map(function (profile) {
        profile._aliases = [profile.key].concat(profile.aliases || []).map(normalize);
        profile._varieties = {};
        Object.keys(profile.varieties || {}).forEach(function (name) {
            profile._varieties[normalize(name)] = { name: name, data: profile.varieties[name] };
        });
        return profile;
    });

    function findProfile(name) {
        const n = normalize(name);
        if (!n) return null;

        const exact = prepared.find(function (profile) {
            return profile._aliases.some(function (alias) { return n === alias; });
        });
        if (exact) return exact;

        let best = null;
        let bestLength = -1;
        prepared.forEach(function (profile) {
            profile._aliases.forEach(function (alias) {
                if (n.length >= 2 && (n.includes(alias) || alias.includes(n)) && alias.length > bestLength) {
                    best = profile;
                    bestLength = alias.length;
                }
            });
        });
        return best;
    }

    function resolvedProfile(profile, varietyName) {
        const v = normalize(varietyName);
        if (!v || !profile) return { data: profile, varietyLabel: "" };
        const matchKey = Object.keys(profile._varieties).find(function (key) {
            return v === key || v.includes(key) || key.includes(v);
        });
        if (!matchKey) return { data: profile, varietyLabel: "" };
        const override = profile._varieties[matchKey];
        return {
            data: Object.assign({}, profile, override.data),
            varietyLabel: override.name
        };
    }

    let autoHarvestValue = "";
    let harvestWasManuallyEdited = Boolean(harvestDate.value);

    harvestDate.addEventListener("input", function () {
        if (harvestDate.value !== autoHarvestValue) {
            harvestWasManuallyEdited = Boolean(harvestDate.value);
        }
    });

    function updateGuide() {
        const profile = findProfile(cropName.value);
        if (!profile) {
            panel.hidden = false;
            cropLabel.textContent = cropName.value.trim() ? cropName.value.trim() : "作物名を入力してください";
            status.textContent = cropName.value.trim()
                ? "この作物の自動補完データはまだ登録されていません。収穫予定日は手動で入力できます。"
                : "作物名だけでも補完できます。品種は任意です。";
            topdressing.textContent = "作物名を入力すると表示されます";
            if (topdressingGuideInput) topdressingGuideInput.value = "";
            harvest.textContent = "作物名を入力すると表示されます";
            care.textContent = "地域・作型・土壌によって適期は変わるため、表示内容は栽培計画の目安として使用してください。";
            return;
        }

        const resolved = resolvedProfile(profile, variety ? variety.value : "");
        const data = resolved.data;
        const base = dateFromIso(plantingDate.value);

        cropLabel.textContent = resolved.varietyLabel
            ? profile.key + "（" + resolved.varietyLabel + "）"
            : profile.key + "（一般的な目安）";
        status.textContent = resolved.varietyLabel
            ? "入力した品種に近い目安を自動表示しています。"
            : "品種未入力でも、作物名から一般的な目安を自動表示しています。";

        const topdressingText = (data.top || []).map(function (range, index) {
            return (index + 1) + "回目：" + rangeText(base, range);
        }).join(" ／ ") || "基本は元肥中心。生育を見て調整してください。";
        topdressing.textContent = topdressingText;
        if (topdressingGuideInput) topdressingGuideInput.value = topdressingText;

        harvest.textContent = rangeText(base, data.harvest);
        care.textContent = data.care || profile.care || "生育状況を確認しながら管理してください。";

        if (base) {
            const suggested = isoDate(addDays(base, data.harvest[0]));
            if (!harvestDate.value || !harvestWasManuallyEdited || harvestDate.value === autoHarvestValue) {
                harvestDate.value = suggested;
                autoHarvestValue = suggested;
                harvestWasManuallyEdited = false;
            }
        }
        scheduleWeatherAdvice();
    }

    let weatherTimer = null;
    let weatherRequest = null;

    function clearStoredWeatherAdvice() {
        if (weatherAdjustedInput) weatherAdjustedInput.value = "";
        if (weatherAdviceInput) weatherAdviceInput.value = "";
        if (weatherAnalyzedAtInput) weatherAnalyzedAtInput.value = "";
    }

    function weatherReady() {
        return Boolean(cropName.value.trim() && plantingDate.value && harvestDate.value
            && fieldName && fieldName.value.trim());
    }

    function scheduleWeatherAdvice() {
        if (!weatherHarvest || !weatherSummary) return;
        if (weatherTimer) window.clearTimeout(weatherTimer);

        if (!weatherReady()) {
            if (weatherStatus) weatherStatus.textContent = "圃場・植付日・収穫予定日を入力すると天候補正します。";
            return;
        }

        weatherTimer = window.setTimeout(function () {
            fetchWeatherAdvice(false);
        }, 700);
    }

    function formatWeatherStats(data) {
        const parts = [];
        if (typeof data.meanTemperature === "number") {
            parts.push("平均気温 約" + data.meanTemperature.toFixed(1) + "℃");
        }
        if (typeof data.rainMm === "number") {
            parts.push("積算降水量 約" + Math.round(data.rainMm) + "mm");
        }
        if (data.forecastThrough) {
            parts.push("予報 " + data.forecastThrough + "まで");
        }
        return parts.length ? parts.join(" ／ ") : "気温・降水量を反映しました";
    }

    async function persistWeatherAdjustedHarvest(data) {
        if (!cropForm || !cropIdInput || !cropIdInput.value || !data || !data.adjustedHarvestDate) {
            return false;
        }

        harvestDate.value = data.adjustedHarvestDate;
        autoHarvestValue = data.adjustedHarvestDate;
        harvestWasManuallyEdited = false;

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
        const formData = new FormData(cropForm);
        const headers = { "X-Requested-With": "XMLHttpRequest" };
        if (csrfToken) headers[csrfHeader] = csrfToken;

        const response = await fetch(cropForm.action, {
            method: "POST",
            body: formData,
            credentials: "same-origin",
            headers: headers
        });

        if (!response.ok) throw new Error("crop save http " + response.status);
        return true;
    }

    async function fetchWeatherAdvice(force) {
        if (!weatherReady()) {
            scheduleWeatherAdvice();
            return;
        }

        if (weatherRequest) weatherRequest.abort();
        weatherRequest = new AbortController();

        const params = new URLSearchParams({
            cropName: cropName.value.trim(),
            variety: variety ? variety.value.trim() : "",
            plantingDate: plantingDate.value,
            harvestDate: harvestDate.value,
            fieldName: fieldName.value.trim()
        });
        if (force) params.set("refresh", String(Date.now()));

        if (weatherStatus) weatherStatus.textContent = "天気を確認しています…";
        if (refreshWeatherButton) refreshWeatherButton.disabled = true;

        try {
            const response = await fetch("/crop/weather-advice?" + params.toString(), {
                method: "GET",
                credentials: "same-origin",
                headers: { "Accept": "application/json" },
                signal: weatherRequest.signal
            });
            if (!response.ok) throw new Error("weather advice http " + response.status);
            const data = await response.json();

            if (!data.available) {
                clearStoredWeatherAdvice();
                weatherHarvest.textContent = "天候補正を利用できません";
                if (weatherStats) weatherStats.textContent = "圃場の位置情報を確認してください";
                weatherSummary.textContent = data.message || "天候データを取得できませんでした。";
                if (weatherStatus) weatherStatus.textContent = "";
                return;
            }

            weatherHarvest.textContent = data.adjustedHarvestDate || harvestDate.value;
            if (weatherStats) weatherStats.textContent = formatWeatherStats(data);
            weatherSummary.textContent = data.summary || data.message || "天候データを反映しました。";
            if (weatherAdjustedInput) weatherAdjustedInput.value = data.adjustedHarvestDate || "";
            if (weatherAdviceInput) weatherAdviceInput.value = data.summary || "";
            if (weatherAnalyzedAtInput) weatherAnalyzedAtInput.value = data.analyzedAt || "";

            if (force && cropIdInput && cropIdInput.value && data.adjustedHarvestDate) {
                if (weatherStatus) weatherStatus.textContent = "収穫日を更新して保存しています…";
                await persistWeatherAdjustedHarvest(data);
                if (weatherStatus) weatherStatus.textContent = "収穫日を更新し、一覧にも保存しました";
            } else {
                if (weatherStatus) weatherStatus.textContent = "天候補正を更新しました";
            }
        } catch (error) {
            if (error && error.name === "AbortError") return;
            if (weatherStatus) weatherStatus.textContent = "天気を取得できませんでした。一般的な目安は利用できます。";
        } finally {
            if (refreshWeatherButton) refreshWeatherButton.disabled = false;
        }
    }

    [cropName, plantingDate].forEach(function (element) {
        element.addEventListener("input", updateGuide);
        element.addEventListener("change", updateGuide);
    });
    if (variety) {
        variety.addEventListener("input", updateGuide);
        variety.addEventListener("change", updateGuide);
    }
    if (fieldName) {
        fieldName.addEventListener("input", scheduleWeatherAdvice);
        fieldName.addEventListener("change", scheduleWeatherAdvice);
    }
    harvestDate.addEventListener("change", scheduleWeatherAdvice);
    harvestDate.addEventListener("input", scheduleWeatherAdvice);
    if (refreshWeatherButton) {
        refreshWeatherButton.addEventListener("click", function () { fetchWeatherAdvice(true); });
    }

    updateGuide();
    scheduleWeatherAdvice();
})();
