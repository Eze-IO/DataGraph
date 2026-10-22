// Loads chart data from the Spring Boot API and draws it with Chart.js.

const money = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
const moneyShort = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', notation: 'compact' });
const count = new Intl.NumberFormat();
const monthName = new Intl.DateTimeFormat(undefined, { month: 'short', year: '2-digit' });

const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
const series = () => ['--series-1', '--series-2', '--series-3', '--series-4'].map(css);

const regionSelect = document.getElementById('region');
const errorBox = document.getElementById('error');
const charts = {};

async function getJson(path) {
    const response = await fetch(path);
    if (!response.ok) throw new Error(`${path} returned ${response.status}`);
    return response.json();
}

function withRegion(path) {
    const region = regionSelect.value;
    return region ? `${path}${path.includes('?') ? '&' : '?'}region=${encodeURIComponent(region)}` : path;
}

function applyTheme() {
    Chart.defaults.color = css('--muted');
    Chart.defaults.borderColor = css('--grid');
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
}

function moneyAxis(axis) {
    return { [axis]: { beginAtZero: true, ticks: { callback: value => moneyShort.format(value) } } };
}

const moneyTooltip = {
    callbacks: { label: ctx => `${ctx.label}: ${money.format(ctx.parsed.y ?? ctx.parsed.x ?? ctx.parsed)}` }
};

// Creates the chart the first time, then only swaps its data
function draw(id, config) {
    if (charts[id]) {
        charts[id].data = config.data;
        charts[id].update();
    } else {
        charts[id] = new Chart(document.getElementById(id), config);
    }
}

async function load() {
    errorBox.hidden = true;
    try {
        const [summary, monthly, byRegion, byCategory, products] = await Promise.all([
            getJson(withRegion('/api/summary')),
            getJson(withRegion('/api/revenue/monthly')),
            getJson('/api/revenue/by-region'),
            getJson(withRegion('/api/revenue/by-category')),
            getJson(withRegion('/api/top-products?limit=6'))
        ]);
        const [c1, c2, c3, c4] = series();
        const faded = css('--series-faded');
        const selected = regionSelect.value;

        document.getElementById('kpi-revenue').textContent = money.format(summary.revenue);
        document.getElementById('kpi-orders').textContent = count.format(summary.orders);
        document.getElementById('kpi-units').textContent = count.format(summary.units);
        document.getElementById('kpi-average').textContent = money.format(summary.averageOrderValue);

        draw('chart-monthly', {
            type: 'line',
            data: {
                labels: monthly.map(p => monthName.format(new Date(`${p.label}-01T00:00:00`))),
                datasets: [{
                    label: 'Revenue', data: monthly.map(p => p.value),
                    borderColor: c1, backgroundColor: c1 + '33', fill: true, tension: 0.3, pointRadius: 2
                }]
            },
            options: { maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: moneyTooltip }, scales: moneyAxis('y') }
        });

        draw('chart-region', {
            type: 'bar',
            data: {
                labels: byRegion.map(p => p.label),
                datasets: [{
                    label: 'Revenue', data: byRegion.map(p => p.value), borderRadius: 4,
                    // Highlight the selected region, fade the others
                    backgroundColor: byRegion.map(p => !selected || p.label === selected ? c2 : faded)
                }]
            },
            options: { maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: moneyTooltip }, scales: moneyAxis('y') }
        });

        draw('chart-category', {
            type: 'doughnut',
            data: {
                labels: byCategory.map(p => p.label),
                datasets: [{ data: byCategory.map(p => p.value), backgroundColor: [c1, c2, c3, c4], borderColor: css('--surface'), borderWidth: 2 }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: { legend: { position: 'right' }, tooltip: { callbacks: { label: ctx => `${ctx.label}: ${money.format(ctx.parsed)}` } } }
            }
        });

        draw('chart-products', {
            type: 'bar',
            data: {
                labels: products.map(p => p.label),
                datasets: [{ label: 'Revenue', data: products.map(p => p.value), backgroundColor: c3, borderRadius: 4 }]
            },
            options: {
                indexAxis: 'y', maintainAspectRatio: false,
                plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => money.format(ctx.parsed.x) } } },
                scales: moneyAxis('x')
            }
        });
    } catch (e) {
        errorBox.textContent = `Couldn't load the data: ${e.message}`;
        errorBox.hidden = false;
    }
}

async function init() {
    if (typeof Chart === 'undefined') {
        errorBox.textContent = "Couldn't load the chart library.";
        errorBox.hidden = false;
        return;
    }
    applyTheme();
    try {
        for (const region of await getJson('/api/regions')) {
            regionSelect.add(new Option(region, region));
        }
    } catch (e) {
        // The dashboard still works for all regions
    }
    regionSelect.addEventListener('change', load);
    load();
}

init();
