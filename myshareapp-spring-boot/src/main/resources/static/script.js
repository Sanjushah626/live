// Theme (Dark / Bright Mode) & Public Header Mobile Menu
(function initThemeImmediate() {
  try {
    const saved = localStorage.getItem('myshareapp-theme');
    const theme = saved === 'dark' ? 'dark' : 'light';
    document.documentElement.dataset.theme = theme;
  } catch {
    // ignore storage errors
  }
})();

function getActiveTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
}

function syncThemeToggleButton() {
  const btn = document.getElementById('themeToggleBtn');
  if (!btn) return;
  const isDark = getActiveTheme() === 'dark';
  btn.innerHTML = isDark
    ? '<span aria-hidden="true">&#9728;&#65039;</span><span>Bright</span>'
    : '<span aria-hidden="true">&#127769;</span><span>Dark</span>';
  btn.setAttribute('aria-pressed', String(isDark));
  btn.setAttribute('title', isDark ? 'Switch to Bright Mode' : 'Switch to Dark Mode');
}

function setActiveTheme(theme) {
  const normalized = theme === 'dark' ? 'dark' : 'light';
  document.documentElement.dataset.theme = normalized;
  try {
    localStorage.setItem('myshareapp-theme', normalized);
  } catch {
    // ignore storage errors
  }
  syncThemeToggleButton();
  window.dispatchEvent(new CustomEvent('themechange', { detail: { theme: normalized } }));
}

document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.getElementById('mobileMenuToggle');
  const nav = document.getElementById('mainNav');
  if (toggle && nav) {
    toggle.addEventListener('click', () => nav.classList.toggle('open'));
  }

  syncThemeToggleButton();
  const themeBtn = document.getElementById('themeToggleBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', () => {
      setActiveTheme(getActiveTheme() === 'dark' ? 'light' : 'dark');
    });
  }
});

// NEPSE Dashboard Script (YONEPSE API backed)
const NPT_OFFSET_MIN = 5 * 60 + 45;
let nepseChartInstance = null;

function nepalDateParts(ms) {
  const fmt = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kathmandu',
    year: 'numeric', month: '2-digit', day: '2-digit'
  });
  const [y, m, d] = fmt.format(new Date(ms)).split('-').map(Number);
  return { y, m, d };
}

function nepalTimeToUtcMs(refMs, hour, minute) {
  const { y, m, d } = nepalDateParts(refMs);
  const utcMinutes = hour * 60 + minute - NPT_OFFSET_MIN;
  return Date.UTC(y, m - 1, d, 0, 0, 0) + utcMinutes * 60 * 1000;
}

function fmtNum(val, decimals = 2) {
  return (Number(val) || 0).toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  });
}

function fmtCompactRs(val) {
  const num = Number(val) || 0;
  if (num >= 1e12) return 'Rs. ' + (num / 1e12).toFixed(2) + ' Tn';
  if (num >= 1e9) return 'Rs. ' + (num / 1e9).toFixed(2) + ' Arba';
  if (num >= 1e7) return 'Rs. ' + (num / 1e7).toFixed(2) + ' Cr';
  if (num >= 1e5) return 'Rs. ' + (num / 1e5).toFixed(2) + ' Lakh';
  return 'Rs. ' + num.toLocaleString('en-US', { maximumFractionDigits: 2 });
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function loadNepseChart() {
  const chartEl = document.querySelector('#chart');
  if (!chartEl) return;
  try {
    if (typeof ApexCharts === 'undefined') {
      chartEl.innerHTML = "<p style='color:#dc2626; padding:20px;'>Chart library failed to load.</p>";
      return;
    }

    const [statusRes, dataRes] = await Promise.all([
      fetch('/IsNepseOpen'),
      fetch('/DailyNepseIndexGraph')
    ]);
    const status = await statusRes.json();
    const rawData = await dataRes.json();

    if (!Array.isArray(rawData) || !rawData.length) {
      chartEl.innerHTML = "<p style='color:var(--text-muted); padding:20px;'>No chart data available right now.</p>";
      return;
    }

    const sampleTs = rawData[0][0];
    const msPerUnit = sampleTs < 3e10 ? 1000 : 1;
    const seriesData = rawData.map(row => ({ x: row[0] * msPerUnit, y: Number(row[1]) }));

    const firstValue = seriesData[0].y;
    const lastValue = seriesData[seriesData.length - 1].y;
    const change = lastValue - firstValue;
    const changePct = firstValue !== 0 ? (change / firstValue) * 100 : 0;
    const isUp = change >= 0;
    const isDark = getActiveTheme() === 'dark';
    const lineColor = isUp ? (isDark ? '#22c55e' : '#16a34a') : (isDark ? '#f87171' : '#ef4444');
    const axisText = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? '#334155' : '#e2e8f0';
    const titleColor = isDark ? '#f1f5f9' : '#0f172a';

    const firstMs = seriesData[0].x;
    const sessionStart = nepalTimeToUtcMs(firstMs, 11, 0);
    const sessionEndFull = nepalTimeToUtcMs(firstMs, 15, 0);
    const isOpen = status.isOpen === 'OPEN';
    const lastMs = seriesData[seriesData.length - 1].x;
    const sessionEnd = isOpen ? Math.max(lastMs, sessionStart) : sessionEndFull;

    const tickPositions = [];
    for (let t = sessionStart; t <= sessionEnd; t += 30 * 60 * 1000) {
      tickPositions.push(t);
    }

    const sessionDateLabel = new Date(firstMs).toLocaleDateString('en-US', {
      timeZone: 'Asia/Kathmandu', month: 'short', day: 'numeric', year: 'numeric'
    });

    const options = {
      chart: {
        type: 'area',
        height: 380,
        foreColor: axisText,
        fontFamily: 'Inter, sans-serif',
        background: 'transparent',
        toolbar: { show: false },
        zoom: { enabled: false },
        animations: { enabled: true, easing: 'easeinout', speed: 350 }
      },
      colors: [lineColor],
      stroke: { curve: 'smooth', width: 2.5 },
      fill: {
        type: 'gradient',
        gradient: {
          shadeIntensity: 1,
          opacityFrom: isDark ? 0.36 : 0.30,
          opacityTo: 0.02,
          stops: [0, 100]
        }
      },
      grid: {
        borderColor: gridColor,
        strokeDashArray: 4,
        xaxis: { lines: { show: false } },
        yaxis: { lines: { show: true } },
        padding: { left: 8, right: 16 }
      },
      markers: { size: 0, hover: { size: 5 } },
      title: {
        text: `NEPSE Index — ${sessionDateLabel}${isOpen ? ' · Live' : ''}`,
        align: 'left',
        style: { color: titleColor, fontSize: '15px', fontWeight: 700 }
      },
      subtitle: {
        text: `${lastValue.toFixed(2)}  ${isUp ? '▲' : '▼'} ${Math.abs(change).toFixed(2)} (${changePct >= 0 ? '+' : ''}${changePct.toFixed(2)}%)`,
        align: 'left',
        style: { color: lineColor, fontSize: '13px', fontWeight: 700 }
      },
      xaxis: {
        type: 'datetime',
        min: sessionStart,
        max: sessionEnd,
        tickPositions,
        axisBorder: { color: gridColor },
        axisTicks: { color: gridColor },
        labels: {
          style: { colors: axisText, fontSize: '11px' },
          formatter: (_val, timestamp) => new Date(timestamp).toLocaleTimeString('en-US', {
            timeZone: 'Asia/Kathmandu', hour: '2-digit', minute: '2-digit', hour12: false
          })
        }
      },
      yaxis: {
        labels: {
          style: { colors: axisText, fontSize: '11px' },
          formatter: val => val.toFixed(0)
        }
      },
      tooltip: {
        theme: isDark ? 'dark' : 'light',
        x: {
          formatter: timestamp => new Date(timestamp).toLocaleTimeString('en-US', {
            timeZone: 'Asia/Kathmandu', hour: '2-digit', minute: '2-digit', hour12: false
          })
        },
        y: { formatter: val => val.toFixed(2) }
      },
      dataLabels: { enabled: false },
      series: [{ name: 'NEPSE Index', data: seriesData }]
    };

    if (nepseChartInstance) {
      nepseChartInstance.destroy();
      nepseChartInstance = null;
    }
    chartEl.innerHTML = '';
    nepseChartInstance = new ApexCharts(chartEl, options);
    nepseChartInstance.render();
  } catch (error) {
    console.error('Error loading NEPSE chart:', error);
    chartEl.innerHTML = "<p style='color:var(--red); padding:20px;'>Unable to load chart data.</p>";
  }
}

let topTradeData = [];
let topTurnoverData = [];

function renderActiveScrips(mode) {
  const tbody = document.querySelector('#traded tbody');
  const colHeader = document.getElementById('tradedMetricHeader');
  if (!tbody) return;
  tbody.innerHTML = '';

  const list = mode === 'turnover' ? topTurnoverData : topTradeData;
  if (colHeader) colHeader.textContent = mode === 'turnover' ? 'Turnover (Rs.)' : 'Volume';

  if (!Array.isArray(list) || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:20px;">No data available</td></tr>';
    return;
  }

  list.slice(0, 6).forEach(item => {
    const row = document.createElement('tr');
    const metricVal = mode === 'turnover'
      ? fmtCompactRs(item.turnover)
      : (Number(item.shareTraded || 0)).toLocaleString();
    row.innerHTML = `
      <td><span class="sym-badge">${escapeHtml(item.symbol || '-')}</span></td>
      <td class="company-ellipsis" title="${escapeHtml(item.securityName || '')}">${escapeHtml(item.securityName || '-')}</td>
      <td>${metricVal}</td>
      <td><strong>${fmtNum(item.closingPrice ?? item.ltp ?? 0)}</strong></td>
    `;
    tbody.appendChild(row);
  });
}

function loadDashboardTables() {
  Promise.all([
    fetch('/TopTenTradeScrips').then(r => r.json()).catch(() => []),
    fetch('/TopTurnoverScrips').then(r => r.json()).catch(() => [])
  ]).then(([trade, turnover]) => {
    topTradeData = Array.isArray(trade) ? trade : [];
    topTurnoverData = Array.isArray(turnover) ? turnover : [];
    renderActiveScrips('volume');
  });

  document.querySelectorAll('[data-traded-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('[data-traded-tab]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      renderActiveScrips(btn.getAttribute('data-traded-tab'));
    });
  });

  fetch('/TopGainers')
    .then(res => res.json())
    .then(data => {
      const tbody = document.querySelector('#gainersTable tbody');
      if (!tbody) return;
      tbody.innerHTML = '';
      data.slice(0, 5).forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td><span class="sym-badge">${escapeHtml(item.symbol)}</span></td>
          <td class="company-ellipsis" title="${escapeHtml(item.securityName || '')}">${escapeHtml(item.securityName || '-')}</td>
          <td>${fmtNum(item.ltp)}</td>
          <td class="positive">+${fmtNum(item.pointChange)}</td>
          <td class="positive">+${fmtNum(item.percentageChange)}%</td>
        `;
        tbody.appendChild(row);
      });
    })
    .catch(err => console.error('Error fetching Top Gainers:', err));

  fetch('/TopLosers')
    .then(res => res.json())
    .then(data => {
      const tbody = document.querySelector('#losersTable tbody');
      if (!tbody) return;
      tbody.innerHTML = '';
      data.slice(0, 5).forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td><span class="sym-badge">${escapeHtml(item.symbol)}</span></td>
          <td class="company-ellipsis" title="${escapeHtml(item.securityName || '')}">${escapeHtml(item.securityName || '-')}</td>
          <td>${fmtNum(item.ltp)}</td>
          <td class="negative">${fmtNum(item.pointChange)}</td>
          <td class="negative">${fmtNum(item.percentageChange)}%</td>
        `;
        tbody.appendChild(row);
      });
    })
    .catch(err => console.error('Error fetching Top Losers:', err));
}

function loadIndicesAndSummary() {
  fetch('/NepseIndex')
    .then(r => r.json())
    .then(data => {
      const ticker = document.getElementById('heroTickerStrip');
      const mainBox = document.getElementById('main-index');
      const entries = [
        ['NEPSE Index', data['NEPSE Index']],
        ['Sensitive Index', data['Sensitive Index']],
        ['Float Index', data['Float Index']],
        ['Sensitive Float Index', data['Sensitive Float Index']]
      ];

      if (ticker) {
        ticker.innerHTML = entries.map(([label, obj]) => {
          if (!obj) return '';
          const up = Number(obj.change) >= 0;
          const cls = up ? 'positive' : 'negative';
          const arrow = up ? '▲' : '▼';
          return `
            <div class="ticker-card">
              <div class="ticker-label">${escapeHtml(label)}</div>
              <div class="ticker-value">${fmtNum(obj.currentValue)}</div>
              <div class="ticker-meta ${cls}">
                ${arrow} ${up ? '+' : ''}${fmtNum(obj.change)} (${up ? '+' : ''}${fmtNum(obj.perChange)}%)
              </div>
            </div>
          `;
        }).join('');
      }

      if (mainBox) {
        mainBox.innerHTML = entries
          .filter(([, obj]) => Boolean(obj))
          .map(([label, obj]) => {
            const up = Number(obj.change) >= 0;
            const cls = up ? 'positive' : 'negative';
            return `
              <div class="index-card">
                <h3>${escapeHtml(label)}</h3>
                <div class="index-value ${cls}">${fmtNum(obj.currentValue)}</div>
                <div class="index-subtext">
                  Change: <span class="${cls}">${up ? '+' : ''}${fmtNum(obj.change)} (${up ? '+' : ''}${fmtNum(obj.perChange)}%)</span>
                  ${obj.high && obj.low ? `<br>Range: ${fmtNum(obj.low)} – ${fmtNum(obj.high)}` : ''}
                </div>
              </div>
            `;
          }).join('');
      }
    })
    .catch(err => console.error('Error fetching NEPSE indices:', err));

  fetch('/NepseSubIndices')
    .then(r => r.json())
    .then(data => {
      const container = document.getElementById('sub-index');
      if (!container || !data) return;
      const entries = Object.entries(data).sort((a, b) => Number(b[1]?.turnover || 0) - Number(a[1]?.turnover || 0));
      if (!entries.length) {
        container.innerHTML = '<div class="index-card"><h3>Sector Performance</h3><div class="index-subtext">No sector data available right now.</div></div>';
        return;
      }
      container.innerHTML = entries.map(([name, info]) => {
        const pct = Number(info.perChange ?? 0);
        const up = pct >= 0;
        const cls = up ? 'positive' : 'negative';
        const adv = Number(info.advancers || 0);
        const dec = Number(info.decliners || 0);
        const scripCount = Number(info.scripCount || 0);
        const breadthTotal = adv + dec;
        const advWidth = breadthTotal > 0 ? Math.round((adv / breadthTotal) * 100) : 50;
        return `
          <div class="index-card">
            <h3>${escapeHtml(name)}</h3>
            <div class="index-value ${cls}">${up ? '+' : ''}${pct.toFixed(2)}%</div>
            <div class="index-subtext">
              ${scripCount ? `${scripCount} scrips · <span class="positive">${adv} up</span> / <span class="negative">${dec} down</span>` : ''}
              ${info.turnover ? `<br>Turnover: <strong>${fmtCompactRs(info.turnover)}</strong>` : ''}
            </div>
            <div class="sector-bar-track" title="Advancers ${adv} vs Decliners ${dec}">
              <div class="sector-bar-fill" style="width:${advWidth}%;background:${advWidth >= 50 ? 'var(--green)' : 'var(--red)'};"></div>
            </div>
          </div>
        `;
      }).join('');
    })
    .catch(err => console.error('Error fetching sub-indices:', err));

  fetch('/Summary')
    .then(r => r.json())
    .then(summary => {
      const box = document.getElementById('market-summary');
      if (!box || !summary) return;
      const items = [
        { label: 'Total Turnover', value: fmtCompactRs(summary['Total Turnover Rs:']), sub: 'Rs. ' + fmtNum(summary['Total Turnover Rs:']) },
        { label: 'Total Traded Shares', value: Number(summary['Total Traded Shares'] || 0).toLocaleString(), sub: 'Shares traded' },
        { label: 'Total Transactions', value: Number(summary['Total Transactions'] || 0).toLocaleString(), sub: 'Executed trades' },
        { label: 'Total Scrips Traded', value: Number(summary['Total Scrips Traded'] || 0).toLocaleString(), sub: 'Active symbols' },
        { label: 'Market Capitalization', value: fmtCompactRs(summary['Total Market Capitalization Rs:']), sub: 'Total NEPSE cap' },
        { label: 'Float Market Cap', value: fmtCompactRs(summary['Total Float Market Capitalization Rs:']), sub: 'Free-float cap' }
      ];
      box.innerHTML = items.map(it => `
        <div class="index-card">
          <h3>${escapeHtml(it.label)}</h3>
          <div class="index-value">${escapeHtml(it.value)}</div>
          <div class="index-subtext">${escapeHtml(it.sub)}</div>
        </div>
      `).join('');
    })
    .catch(err => console.error('Error fetching summary:', err));
}

function loadTopBrokers() {
  const tbody = document.getElementById('topBrokersBody');
  const summaryEl = document.getElementById('topBrokersSummary');
  if (!tbody) return;

  fetch('/TopBrokers')
    .then(r => r.json())
    .then(data => {
      const brokers = Array.isArray(data?.brokers) ? data.brokers : [];
      const summary = data?.summary || {};
      if (summaryEl) {
        summaryEl.textContent = brokers.length
          ? `${summary.activeTodayCount || 0} active today · Total volume ${Number(summary.activeVolumeTotal || 0).toLocaleString()} shares`
          : 'Broker volume unavailable';
      }
      if (!brokers.length) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:20px;">No broker activity available right now.</td></tr>';
        return;
      }
      tbody.innerHTML = brokers.slice(0, 10).map((b, idx) => {
        const rating = b.averageRating != null ? `${b.averageRating.toFixed(1)} ★ (${b.totalRatings})` : '—';
        return `
          <tr>
            <td><span class="broker-rank-badge">#${idx + 1}</span></td>
            <td><span class="broker-code-pill">Broker #${escapeHtml(b.memberCode)}</span></td>
            <td style="text-align:left;font-weight:600;color:var(--text-primary);">${escapeHtml(b.memberName)}</td>
            <td>${Number(b.totalVolume || 0).toLocaleString()}</td>
            <td><span class="positive">${fmtCompactRs(b.buyAmount)}</span> / <span class="negative">${fmtCompactRs(b.sellAmount)}</span></td>
            <td><strong>${fmtCompactRs(b.latestTurnover)}</strong></td>
            <td>${b.topStock ? `<span class="sym-badge">${escapeHtml(b.topStock)}</span>` : '—'}</td>
            <td>${rating}</td>
          </tr>
        `;
      }).join('');
    })
    .catch(() => {
      if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:20px;">Unable to load top brokers right now.</td></tr>';
      }
    });
}

async function checkMarketStatus() {
  const statusDiv = document.getElementById('market-status');
  if (!statusDiv) return;
  try {
    const response = await fetch('/IsNepseOpen');
    const data = await response.json();
    if (data.isOpen === 'OPEN') {
      statusDiv.innerHTML = '<span class="dot open"></span> Market is Open';
    } else {
      statusDiv.innerHTML = '<span class="dot closed"></span> Market is Closed';
    }
  } catch {
    statusDiv.innerHTML = '<span class="dot closed"></span> Status Unavailable';
  }
}

checkMarketStatus();
setInterval(checkMarketStatus, 60000);

if (document.getElementById('chart')) {
  loadNepseChart();
  loadDashboardTables();
  loadIndicesAndSummary();
  loadTopBrokers();
  window.addEventListener('themechange', () => {
    loadNepseChart();
  });
}
