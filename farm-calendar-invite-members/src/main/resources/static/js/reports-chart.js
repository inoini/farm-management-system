(() => {
    const data = Array.isArray(window.reportTrendData) ? window.reportTrendData : [];
    const cropTrends = Array.isArray(window.cropTrendData) ? window.cropTrendData : [];

    const moneyLabel = (value) => {
        const rounded = Math.round(Number(value) || 0);
        const abs = Math.abs(rounded);
        const sign = rounded < 0 ? '−' : '';
        if (abs >= 100000000) {
            const oku = Math.floor(abs / 100000000);
            const rest = abs % 100000000;
            const man = Math.floor(rest / 10000);
            return `${sign}${oku}億${man ? man.toLocaleString('ja-JP') + '万' : ''}円`;
        }
        if (abs >= 10000) {
            const man = Math.floor(abs / 10000);
            const rest = abs % 10000;
            return `${sign}${man.toLocaleString('ja-JP')}万${rest ? rest.toLocaleString('ja-JP') : ''}円`;
        }
        return `${sign}${abs.toLocaleString('ja-JP')}円`;
    };

    const kgLabel = (value) => {
        const number = Number(value) || 0;
        const rounded = Math.abs(number - Math.round(number)) < 0.001
            ? Math.round(number).toLocaleString('ja-JP')
            : number.toLocaleString('ja-JP', { maximumFractionDigits: 1 });
        return `${rounded}kg`;
    };

    const palette = () => {
        const styles = getComputedStyle(document.documentElement);
        return {
            grid: 'rgba(125, 145, 133, .20)',
            text: styles.getPropertyValue('--discord-text').trim() || '#173f2b',
            muted: styles.getPropertyValue('--discord-text-muted').trim() || '#68776e',
            accent: styles.getPropertyValue('--discord-accent').trim() || '#2e9a62',
            sales: '#2f80ed',
            expenses: '#e67e22',
            profit: '#2e9a62',
            shipped: '#7b61ff'
        };
    };

    function fitCanvas(canvas, chartData) {
        const parent = canvas.parentElement;
        const cssWidth = Math.max(parent.clientWidth, chartData.length * 72, 620);
        const cssHeight = window.innerWidth <= 600 ? 280 : 330;
        const ratio = window.devicePixelRatio || 1;
        canvas.style.width = `${cssWidth}px`;
        canvas.style.height = `${cssHeight}px`;
        canvas.width = Math.round(cssWidth * ratio);
        canvas.height = Math.round(cssHeight * ratio);
        const ctx = canvas.getContext('2d');
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        return {ctx, width: cssWidth, height: cssHeight};
    }

    function niceMax(values) {
        const max = Math.max(0, ...values.map(v => Number(v) || 0));
        if (max <= 0) return 10;
        const roughMax = max * 1.15;
        const power = Math.pow(10, Math.floor(Math.log10(roughMax)));
        const normalized = roughMax / power;
        let nice;
        if (normalized <= 1) nice = 1;
        else if (normalized <= 2) nice = 2;
        else if (normalized <= 5) nice = 5;
        else nice = 10;
        return nice * power;
    }

    function axisLabel(value, options) {
        if (options.money) return moneyLabel(value);
        if (options.unit === 'kg') return kgLabel(value);
        return Math.round(value).toLocaleString('ja-JP');
    }

    function drawLineChart(canvas, chartData, series, options = {}) {
        if (!canvas || !Array.isArray(chartData) || chartData.length === 0) return;
        const {ctx, width, height} = fitCanvas(canvas, chartData);
        const colors = palette();
        const pad = {left: options.money ? 112 : 82, right: 18, top: 22, bottom: 54};
        const plotW = width - pad.left - pad.right;
        const plotH = height - pad.top - pad.bottom;
        const allValues = series.flatMap(s => chartData.map(row => Number(row[s.key]) || 0));
        const minValue = Math.min(0, ...allValues);
        const maxValue = niceMax(allValues);
        const span = Math.max(1, maxValue - minValue);
        const x = i => pad.left + (chartData.length <= 1 ? plotW / 2 : (i * plotW / (chartData.length - 1)));
        const y = value => pad.top + ((maxValue - value) / span) * plotH;

        ctx.clearRect(0, 0, width, height);
        ctx.font = '12px system-ui, sans-serif';
        ctx.textBaseline = 'middle';

        for (let i = 0; i <= 4; i++) {
            const value = maxValue - (span * i / 4);
            const yy = pad.top + plotH * i / 4;
            ctx.beginPath();
            ctx.strokeStyle = colors.grid;
            ctx.lineWidth = 1;
            ctx.moveTo(pad.left, yy);
            ctx.lineTo(width - pad.right, yy);
            ctx.stroke();
            ctx.fillStyle = colors.muted;
            ctx.textAlign = 'right';
            ctx.fillText(axisLabel(value, options), pad.left - 9, yy);
        }

        chartData.forEach((row, i) => {
            ctx.fillStyle = colors.muted;
            ctx.textAlign = 'center';
            const raw = String(row.month || '');
            const label = raw.replace(/^\d{4}年/, '');
            ctx.fillText(label, x(i), height - 24);
        });

        series.forEach((item) => {
            const color = item.color || colors.accent;
            ctx.beginPath();
            ctx.lineWidth = 3;
            ctx.strokeStyle = color;
            ctx.lineJoin = 'round';
            ctx.lineCap = 'round';
            chartData.forEach((row, i) => {
                const yy = y(Number(row[item.key]) || 0);
                if (i === 0) ctx.moveTo(x(i), yy); else ctx.lineTo(x(i), yy);
            });
            ctx.stroke();

            chartData.forEach((row, i) => {
                const yy = y(Number(row[item.key]) || 0);
                ctx.beginPath();
                ctx.fillStyle = color;
                ctx.arc(x(i), yy, series.length === 1 ? 4.5 : 3.5, 0, Math.PI * 2);
                ctx.fill();
            });
        });
    }

    const draw = () => {
        const colors = palette();
        drawLineChart(document.getElementById('harvestChart'), data, [
            {key: 'harvestKg', color: colors.accent}
        ], {unit: 'kg'});

        drawLineChart(document.getElementById('profitChart'), data, [
            {key: 'sales', color: colors.sales},
            {key: 'expenses', color: colors.expenses},
            {key: 'profit', color: colors.profit}
        ], {money: true});

        cropTrends.forEach((cropTrend, index) => {
            const points = Array.isArray(cropTrend.points) ? cropTrend.points : [];
            drawLineChart(document.getElementById(`cropChart-${index}`), points, [
                {key: 'harvestKg', color: colors.accent},
                {key: 'shippedKg', color: colors.shipped}
            ], {unit: 'kg'});
        });
    };

    let resizeTimer;
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(draw, 120);
    });
    window.addEventListener('DOMContentLoaded', draw);
})();
