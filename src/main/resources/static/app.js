// Builds the navbar from /api/datasets, then draws the selected dataset's
// dashboard from /api/datasets/{id}/dashboard with Chart.js.

const formatters = {
    MONEY: new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }),
    COUNT: new Intl.NumberFormat(),
    PERCENT: new Intl.NumberFormat(undefined, { style: 'percent', maximumFractionDigits: 1 })
};
const compact = {
    MONEY: new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', notation: 'compact' }),
    COUNT: new Intl.NumberFormat(undefined, { notation: 'compact' }),
    PERCENT: formatters.PERCENT
};
const monthName = new Intl.DateTimeFormat(undefined, { month: 'short', year: '2-digit' });

const format = (value, kind) => formatters[kind].format(value);
// Big totals (views in the millions) read better as 12.3M
const formatTile = (value, kind) => kind === 'COUNT' && value >= 1e6 ? compact.COUNT.format(value) : format(value, kind);

const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
const $ = id => document.getElementById(id);

const segmentSelect = $('segment');
const charts = {};
let datasets = [];
let current = null;

async function getJson(path) {
    const response = await fetch(path);
    if (!response.ok) throw new Error(`${path} returned ${response.status}`);
    return response.json();
}

function showError(message) {
    $('error').textContent = message;
    $('error').hidden = !message;
}

// --- Navbar ---------------------------------------------------------------

function buildNav() {
    const nav = $('nav');
    for (const dataset of datasets) {
        const button = document.createElement('button');
        button.type = 'button';
        button.dataset.id = dataset.id;
        button.innerHTML = `<span class="tab-name"></span><span class="tab-industry"></span>`;
        button.querySelector('.tab-name').textContent = dataset.name;
        button.querySelector('.tab-industry').textContent = dataset.industry;
        button.addEventListener('click', () => { location.hash = dataset.id; });
        nav.append(button);
    }
}

function selectFromHash() {
    const id = location.hash.slice(1);
    const dataset = datasets.find(d => d.id === id) ?? datasets[0];
    if (dataset === current) return;
    current = dataset;

    for (const button of $('nav').children) {
        const active = button.dataset.id === dataset.id;
        button.classList.toggle('active', active);
        button.setAttribute('aria-current', active ? 'page' : 'false');
    }
    $('industry').textContent = dataset.industry;
    $('title').textContent = dataset.name;
    $('description').textContent = dataset.description;
    $('filter-label').textContent = dataset.segmentLabel;
    document.title = `${dataset.name} · DataGraph`;

    segmentSelect.replaceChildren(new Option(`All ${dataset.segmentLabel.toLowerCase()}s`, ''));
    for (const segment of dataset.segments) segmentSelect.add(new Option(segment, segment));

    // Different datasets have different chart shapes, so start each one fresh,
    // and don't leave the previous dataset's numbers showing while this one loads
    for (const id of Object.keys(charts)) { charts[id].destroy(); delete charts[id]; }
    $('kpis').replaceChildren();
    for (const id of ['title-trend', 'title-segment', 'title-group', 'title-top']) $(id).textContent = '';
    load();
}

// --- Charts ---------------------------------------------------------------

function palette() {
    return ['--series-1', '--series-2', '--series-3', '--series-4'].map(css);
}

function valueAxis(axis, kind) {
    return { [axis]: { beginAtZero: true, ticks: { callback: value => compact[kind].format(value) } } };
}

function tooltip(kind, pick) {
    return { callbacks: { label: ctx => `${ctx.dataset.label ?? ctx.label}: ${format(pick(ctx), kind)}` } };
}

// Creates the chart the first time, then only swaps its data
function draw(id, config) {
    if (charts[id]) {
        charts[id].data = config.data;
        charts[id].update();
    } else {
        charts[id] = new Chart($(id), config);
    }
}

function render(dashboard) {
    const colors = palette();
    const faded = css('--series-faded');
    const selected = segmentSelect.value;

    $('kpis').replaceChildren(...dashboard.kpis.map(kpi => {
        const tile = document.createElement('div');
        tile.className = 'kpi';
        tile.innerHTML = '<span class="kpi-label"></span><span class="kpi-value"></span>';
        tile.querySelector('.kpi-label').textContent = kpi.label;
        tile.querySelector('.kpi-value').textContent = formatTile(kpi.value, kpi.format);
        tile.querySelector('.kpi-value').title = format(kpi.value, kpi.format);
        return tile;
    }));

    const { trend, bySegment, byGroup, topItems } = dashboard;
    $('title-trend').textContent = trend.title;
    $('title-segment').textContent = bySegment.title;
    $('title-group').textContent = byGroup.title;
    $('title-top').textContent = topItems.title;

    draw('chart-trend', {
        type: 'line',
        data: {
            labels: trend.labels.map(label => monthName.format(new Date(`${label}-01T00:00:00`))),
            datasets: trend.series.map((series, i) => ({
                label: series.label, data: series.values,
                borderColor: colors[i], backgroundColor: colors[i] + '26',
                fill: trend.series.length === 1, tension: 0.3, pointRadius: 2
            }))
        },
        options: {
            maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
            plugins: { legend: { display: trend.series.length > 1 }, tooltip: tooltip(trend.format, ctx => ctx.parsed.y) },
            scales: valueAxis('y', trend.format)
        }
    });

    draw('chart-segment', {
        type: 'bar',
        data: {
            labels: bySegment.data.map(p => p.label),
            datasets: [{
                label: bySegment.title, data: bySegment.data.map(p => p.value), borderRadius: 4,
                // Highlight the selected segment, fade the others
                backgroundColor: bySegment.data.map(p => !selected || p.label === selected ? colors[1] : faded)
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false }, tooltip: tooltip(bySegment.format, ctx => ctx.parsed.y) },
            scales: valueAxis('y', bySegment.format)
        }
    });

    draw('chart-group', {
        type: 'doughnut',
        data: {
            labels: byGroup.data.map(p => p.label),
            datasets: [{ data: byGroup.data.map(p => p.value), backgroundColor: colors, borderColor: css('--surface'), borderWidth: 2 }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'right' },
                tooltip: { callbacks: { label: ctx => `${ctx.label}: ${format(ctx.parsed, byGroup.format)}` } }
            }
        }
    });

    draw('chart-top', {
        type: 'bar',
        data: {
            labels: topItems.data.map(p => p.label),
            datasets: [{ label: topItems.title, data: topItems.data.map(p => p.value), backgroundColor: colors[2], borderRadius: 4 }]
        },
        options: {
            indexAxis: 'y', maintainAspectRatio: false,
            plugins: { legend: { display: false }, tooltip: { callbacks: { label: ctx => format(ctx.parsed.x, topItems.format) } } },
            scales: valueAxis('x', topItems.format)
        }
    });
}

// Ignores answers that arrive after the user has already moved on
let requestNumber = 0;

async function load() {
    const request = ++requestNumber;
    const segment = segmentSelect.value;
    const path = `/api/datasets/${encodeURIComponent(current.id)}/dashboard`
        + (segment ? `?segment=${encodeURIComponent(segment)}` : '');
    document.body.classList.add('loading');
    try {
        const dashboard = await getJson(path);
        if (request !== requestNumber) return;
        showError('');
        render(dashboard);
    } catch (e) {
        if (request === requestNumber) showError(`Couldn't load the data: ${e.message}`);
    } finally {
        if (request === requestNumber) document.body.classList.remove('loading');
    }
}

async function init() {
    if (typeof Chart === 'undefined') {
        showError("Couldn't load the chart library.");
        return;
    }
    Chart.defaults.color = css('--muted');
    Chart.defaults.borderColor = css('--grid');
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;

    try {
        datasets = await getJson('/api/datasets');
    } catch (e) {
        showError(`Couldn't load the list of datasets: ${e.message}`);
        return;
    }
    buildNav();
    segmentSelect.addEventListener('change', load);
    window.addEventListener('hashchange', selectFromHash);
    selectFromHash();
}

init();
