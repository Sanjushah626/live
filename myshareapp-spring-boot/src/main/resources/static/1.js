function toggleStocks(id) {
  var stocks = document.getElementById("stocks-" + id);
  var card = document.querySelector('.portfolio-card[data-id="' + id + '"]');
  if (!stocks) return;
  var opening = stocks.style.display !== "block";
  stocks.style.display = opening ? "block" : "none";
  if (card) card.classList.toggle("is-expanded", opening);
}

var portfolioModal = document.getElementById("portfolioModal");
var addPortfolioBtn = document.getElementById("addPortfolioBtn");
var closePortfolio = document.getElementById("closePortfolio");
if (addPortfolioBtn) addPortfolioBtn.addEventListener("click", function () { portfolioModal.style.display = "block"; });
if (closePortfolio) closePortfolio.addEventListener("click", function () { portfolioModal.style.display = "none"; });

var stockModal = document.getElementById("addStockModal");
var closeStock = document.getElementById("closeStock");
var purchaseTypeSelect = document.getElementById("purchase_type");
var buyPriceField = document.getElementById("buyPriceField");
var buyPriceInput = document.getElementById("buy_price");
var typeHint = document.getElementById("typeHint");

document.querySelectorAll(".add-stock-btn").forEach(function (button) {
  button.addEventListener("click", function (e) {
    e.stopPropagation();
    var portfolioId = this.closest(".portfolio-card").dataset.id;
    document.getElementById("portfolio_id").value = portfolioId;
    stockModal.style.display = "block";
    loadSymbolList();
  });
});

if (closeStock) closeStock.addEventListener("click", function () { stockModal.style.display = "none"; });

function applyTransactionTypeRules() {
  if (!purchaseTypeSelect) return;
  var type = purchaseTypeSelect.value;
  if (type === "Bonus") {
    buyPriceField.classList.add("field-hidden");
    buyPriceInput.required = false;
    buyPriceInput.value = 0;
    typeHint.textContent = "Bonus shares are received at zero cost and carry no buying charges. Only quantity is recorded.";
  } else if (type === "IPO") {
    buyPriceField.classList.remove("field-hidden");
    buyPriceInput.required = true;
    typeHint.textContent = "IPO allotment — no broker commission, SEBON fee or DP charge on buying.";
  } else {
    buyPriceField.classList.remove("field-hidden");
    buyPriceInput.required = true;
    typeHint.textContent = "Secondary market purchase — broker commission, SEBON fee & DP charge apply.";
  }
}

if (purchaseTypeSelect) {
  purchaseTypeSelect.addEventListener("change", applyTransactionTypeRules);
  applyTransactionTypeRules();
}

var sellModal = document.getElementById("sellStockModal");
var closeSell = document.getElementById("closeSell");
if (closeSell) closeSell.addEventListener("click", function () { sellModal.style.display = "none"; });

var NON_TRADING_DAYS = [0, 6];
function isNonTradingDay(date) {
  return NON_TRADING_DAYS.indexOf(date.getDay()) !== -1;
}

var buyPicker = null;
var sellPicker = null;

document.addEventListener("DOMContentLoaded", function () {
  var buyInput = document.getElementById("buy_date");
  var sellInput = document.getElementById("sell_date");

  if (buyInput && typeof flatpickr !== "undefined") {
    buyPicker = flatpickr(buyInput, {
      dateFormat: "Y-m-d",
      maxDate: "today",
      disable: [isNonTradingDay],
      disableMobile: true
    });
  }

  if (sellInput && typeof flatpickr !== "undefined") {
    sellPicker = flatpickr(sellInput, {
      dateFormat: "Y-m-d",
      maxDate: "today",
      disable: [isNonTradingDay],
      disableMobile: true
    });
  }

  // Auto-expand first portfolio card for immediate visibility
  var firstCard = document.querySelector(".portfolio-card");
  if (firstCard && firstCard.dataset.id) {
    toggleStocks(firstCard.dataset.id);
  }
});

var sellMinDate = null;

function openSellModal(stockId, symbol, availableQty, buyDateStr) {
  document.getElementById("sell_stock_id").value = stockId;
  document.getElementById("sell_symbol").value = symbol;
  document.getElementById("sell_available").value = availableQty;

  var qtyInput = document.getElementById("sell_quantity");
  qtyInput.value = "";
  qtyInput.max = availableQty;
  document.getElementById("sell_price").value = "";

  var buyDate = normalizeDate(buyDateStr);
  if (!buyDate) {
    var card = document.querySelector('.stock-card[data-id="' + stockId + '"]');
    if (card) buyDate = normalizeDate(card.dataset.buydate);
  }
  sellMinDate = buyDate;

  if (sellPicker) {
    sellPicker.clear();
    sellPicker.set("maxDate", "today");
    sellPicker.set("minDate", buyDate);
    sellPicker.jumpToDate(buyDate || "today");
  } else {
    document.getElementById("sell_date").value = "";
  }

  sellModal.style.display = "block";
}

function normalizeDate(str) {
  if (!str) return null;
  str = String(str).trim();
  var iso = str.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) return iso[1] + "-" + iso[2] + "-" + iso[3];
  var d = new Date(str);
  if (isNaN(d.getTime())) return null;
  var m = ("0" + (d.getMonth() + 1)).slice(-2);
  var day = ("0" + d.getDate()).slice(-2);
  return d.getFullYear() + "-" + m + "-" + day;
}

if (sellModal) {
  var sellForm = sellModal.querySelector("form");
  if (sellForm) {
    sellForm.addEventListener("submit", function (e) {
      var val = document.getElementById("sell_date").value;
      var d = val ? new Date(val + "T00:00:00") : null;
      var invalid = !val ||
        (sellMinDate && val < sellMinDate) ||
        val > localToday() ||
        (d && isNonTradingDay(d));
      if (invalid) {
        e.preventDefault();
        var errBox = document.getElementById("sellDateError");
        if (errBox) {
          errBox.textContent = "Invalid sell date. It cannot be before the buy date, in the future, or on a weekend.";
          errBox.style.display = "block";
        }
      }
    });
  }
}

function localToday() {
  var n = new Date();
  return n.getFullYear() + "-" + ("0" + (n.getMonth() + 1)).slice(-2) + "-" + ("0" + n.getDate()).slice(-2);
}

window.addEventListener("click", function (e) {
  if (e.target === portfolioModal) portfolioModal.style.display = "none";
  if (e.target === stockModal) stockModal.style.display = "none";
  if (e.target === sellModal) sellModal.style.display = "none";
  if (e.target === depthModal) closeDepthModal();
});

var symbolListLoaded = false;
async function loadSymbolList() {
  if (symbolListLoaded) return;
  try {
    var res = await fetch("/PriceVolume");
    var stocks = await res.json();
    var datalist = document.getElementById("symbolList");
    datalist.innerHTML = "";
    stocks.forEach(function (item) {
      if (!item.symbol) return;
      var opt = document.createElement("option");
      opt.value = item.symbol.toUpperCase();
      var label = item.securityName || item.name || "";
      if (label) opt.label = item.symbol.toUpperCase() + " — " + label;
      datalist.appendChild(opt);
    });
    symbolListLoaded = true;
  } catch (err) {
    console.error("Could not load symbol list:", err);
  }
}

function formatMoney(num) {
  var value = Number(num) || 0;
  return value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function calculateBrokerCommission(amount) {
  amount = Number(amount) || 0;
  var rate;
  if (amount <= 50000) rate = 0.0036;
  else if (amount <= 500000) rate = 0.0033;
  else if (amount <= 2000000) rate = 0.0031;
  else if (amount <= 10000000) rate = 0.0027;
  else rate = 0.0024;
  var commission = amount * rate;
  if (commission < 10) commission = 10;
  return commission;
}

function calculateSebonFee(amount) {
  return (Number(amount) || 0) * 0.00015;
}

function calculateDpCharge() { return 25; }

function calculateBuyCharges(amount, purchaseType) {
  var type = (purchaseType || "").toLowerCase();
  if (type === "ipo" || type === "bonus") return { broker: 0, sebon: 0, dp: 0, total: 0 };
  var broker = calculateBrokerCommission(amount);
  var sebon = calculateSebonFee(amount);
  var dp = calculateDpCharge();
  return { broker: broker, sebon: sebon, dp: dp, total: broker + sebon + dp };
}

function calculateSellCharges(amount) {
  var broker = calculateBrokerCommission(amount);
  var sebon = calculateSebonFee(amount);
  var dp = calculateDpCharge();
  return { broker: broker, sebon: sebon, dp: dp, total: broker + sebon + dp };
}

function getCapitalGainTaxRate(buyDateStr, portfolioType) {
  var type = (portfolioType || "personal").toLowerCase();
  if (type === "institutional") return 0.10;
  var buyDate = new Date(buyDateStr);
  var today = new Date();
  if (isNaN(buyDate.getTime())) return 0.075;
  var holdingDays = Math.floor((today - buyDate) / (1000 * 60 * 60 * 24));
  return holdingDays >= 365 ? 0.05 : 0.075;
}

function getPortfolioType(card) {
  if (card.dataset && card.dataset.type) return card.dataset.type.toLowerCase();
  return "personal";
}

function calculateStockBreakdown(row, ltp, portfolioType) {
  var qty = parseFloat(row.dataset.quantity) || 0;
  if (qty === 0) {
    return {
      investment: 0, currentValue: 0, receivableAmount: 0,
      grossProfit: 0, capitalGainTax: 0, netProfit: 0, profitPercent: 0,
      buyCharges: { broker: 0, sebon: 0, dp: 0, total: 0 },
      sellCharges: { broker: 0, sebon: 0, dp: 0, total: 0 }
    };
  }
  var buyPrice = parseFloat(row.dataset.buyprice) || 0;
  var buyDate = row.dataset.buydate || "";
  var purchaseType = row.dataset.purchasetype || "secondary";
  var investment = qty * buyPrice;
  var currentValue = qty * ltp;
  var buyCharges = calculateBuyCharges(investment, purchaseType);
  var sellCharges = calculateSellCharges(currentValue);
  var receivableAmount = currentValue - sellCharges.total;
  var totalCost = investment + buyCharges.total;
  var grossProfit = receivableAmount - totalCost;
  var taxRate = getCapitalGainTaxRate(buyDate, portfolioType);
  var capitalGainTax = grossProfit > 0 ? grossProfit * taxRate : 0;
  var netProfit = grossProfit - capitalGainTax;
  var profitPercent = totalCost > 0 ? (netProfit / totalCost) * 100 : 0;
  return {
    investment: investment, currentValue: currentValue, receivableAmount: receivableAmount,
    grossProfit: grossProfit, capitalGainTax: capitalGainTax, netProfit: netProfit,
    profitPercent: profitPercent, buyCharges: buyCharges, sellCharges: sellCharges
  };
}

async function updateStockPrices() {
  try {
    var response = await fetch("/PriceVolume");
    if (!response.ok) throw new Error("Unable to fetch API");
    var stocks = await response.json();

    stocks.forEach(function (item) {
      var symbol = String(item.symbol || "").toUpperCase();
      if (!symbol) return;
      var priceCells = document.querySelectorAll('.stock-ltp[data-symbol="' + symbol + '"]');
      priceCells.forEach(function (priceCell) {
        var card = priceCell.closest(".stock-card");
        if (!card) return;
        var qty = parseFloat(card.dataset.quantity) || 0;
        if (qty === 0) {
          priceCell.innerText = "-";
          return;
        }
        var ltp = parseFloat(item.lastTradedPrice ?? item.ltp) || 0;
        var previousClose = parseFloat(item.previousClose ?? item.previous_close) || ltp;
        var closePrice = parseFloat(item.closePrice ?? item.ltp) || ltp;
        var percentageChange = parseFloat(item.percentageChange ?? item.percent_change) || 0;
        var todaysGain = qty * (closePrice - previousClose);
        card.dataset.prevclose = String(previousClose);

        var portfolioCard = card.closest(".portfolio-card");
        var portfolioType = portfolioCard ? getPortfolioType(portfolioCard) : "personal";
        var breakdown = calculateStockBreakdown(card, ltp, portfolioType);
        var totalInvestment = breakdown.investment + breakdown.buyCharges.total;
        var receivableAmount = breakdown.receivableAmount;
        var profitLoss = breakdown.netProfit;

        priceCell.innerText = "Rs. " + formatMoney(ltp);
        priceCell.classList.remove("loading");

        var pctBadge = card.querySelector(".stock-pct-change");
        if (pctBadge) {
          pctBadge.innerText = (percentageChange >= 0 ? "+" : "") + percentageChange.toFixed(2) + "%";
          pctBadge.className = "stock-pct-change " + (percentageChange >= 0 ? "up" : "down");
        }

        var investmentCell = card.querySelector(".investment");
        if (investmentCell) {
          investmentCell.innerText = "Rs. " + formatMoney(totalInvestment);
          investmentCell.title = "Raw Investment: Rs. " + formatMoney(breakdown.investment) +
            "\nBuy Charges: Rs. " + formatMoney(breakdown.buyCharges.total);
        }

        var currentValueCell = card.querySelector(".current-value");
        if (currentValueCell) {
          currentValueCell.innerText = "Rs. " + formatMoney(breakdown.currentValue);
        }

        var receivableCell = card.querySelector(".receivable-amount");
        if (receivableCell) {
          receivableCell.innerText = "Rs. " + formatMoney(receivableAmount);
        }

        var plValue = card.querySelector(".profit-loss");
        var plPercent = card.querySelector(".profit-percent");
        var profitBar = card.querySelector(".stock-profit-bar");
        if (plValue) {
          plValue.innerText = (profitLoss >= 0 ? "+" : "") + "Rs. " + formatMoney(profitLoss);
          plValue.className = "profit-bar-value profit-loss " + (profitLoss >= 0 ? "positive" : "negative");
        }
        if (plPercent) {
          plPercent.innerText = (breakdown.profitPercent >= 0 ? "+" : "") + breakdown.profitPercent.toFixed(2) + "%";
          plPercent.className = "profit-bar-percent profit-percent " + (profitLoss >= 0 ? "positive" : "negative");
        }
        if (profitBar) {
          profitBar.className = "stock-profit-bar " + (profitLoss >= 0 ? "positive" : "negative");
        }

        var tg = card.querySelector(".todays-gain");
        if (tg) {
          tg.innerText = (todaysGain >= 0 ? "+" : "") + "Rs. " + formatMoney(todaysGain);
          tg.className = "stat-value todays-gain " + (todaysGain >= 0 ? "positive" : "negative");
        }
      });
    });

    var grandInv = 0, grandVal = 0, grandPL = 0, grandRealized = 0;

    document.querySelectorAll(".portfolio-card").forEach(function (card) {
      var totalInvestment = 0;
      var totalCurrentValue = 0;
      var totalReceivableAmount = 0;
      var totalProfitLoss = 0;
      var totalUnits = 0;
      var totalTodaysGain = 0;
      var totalPreviousValue = 0;

      card.querySelectorAll(".stock-card[data-quantity]").forEach(function (stockCard) {
        var qty = parseFloat(stockCard.dataset.quantity) || 0;
        totalUnits += qty;
        var invText = (stockCard.querySelector(".investment")?.innerText || "0").replace(/Rs\.|,/g, "");
        var curText = (stockCard.querySelector(".current-value")?.innerText || "0").replace(/Rs\.|,/g, "");
        var recText = (stockCard.querySelector(".receivable-amount")?.innerText || "0").replace(/Rs\.|,/g, "");
        var plText = (stockCard.querySelector(".profit-loss")?.innerText || "0").replace(/Rs\.|,/g, "");
        var tgText = (stockCard.querySelector(".todays-gain")?.innerText || "0").replace(/Rs\.|,/g, "");
        var prevClose = parseFloat(stockCard.dataset.prevclose || "0") || 0;

        totalInvestment += parseFloat(invText) || 0;
        totalCurrentValue += parseFloat(curText) || 0;
        totalReceivableAmount += parseFloat(recText) || 0;
        totalProfitLoss += parseFloat(plText) || 0;
        totalTodaysGain += parseFloat(tgText) || 0;
        totalPreviousValue += prevClose * qty;
      });

      var totalProfitPercent = totalInvestment > 0 ? (totalProfitLoss / totalInvestment) * 100 : 0;
      var todaysGainPercent = totalPreviousValue > 0 ? (totalTodaysGain / totalPreviousValue) * 100 : 0;

      var metrics = card.querySelectorAll(".metrics div");
      if (metrics.length >= 8) {
        metrics[0].innerHTML = "<strong>Total Investment</strong>" +
          '<span class="metric-value neutral">Rs. ' + formatMoney(totalInvestment) + '</span>';
        metrics[1].innerHTML = "<strong>Profit / Loss</strong>" +
          '<span class="metric-value ' + (totalProfitLoss >= 0 ? 'positive' : 'negative') + '">Rs. ' + formatMoney(totalProfitLoss) + '</span>';
        metrics[2].innerHTML = "<strong>Current Units</strong>" +
          '<span class="metric-value neutral">' + totalUnits + '</span>';
        metrics[3].innerHTML = "<strong>Current Value</strong>" +
          '<span class="metric-value neutral">Rs. ' + formatMoney(totalCurrentValue) + '</span>';
        metrics[4].innerHTML = "<strong>Receivable Amount</strong>" +
          '<span class="metric-value neutral">Rs. ' + formatMoney(totalReceivableAmount) + '</span>';
        metrics[5].innerHTML = "<strong>Total Profit %</strong>" +
          '<span class="metric-value ' + (totalProfitPercent >= 0 ? 'positive' : 'negative') + '">' + totalProfitPercent.toFixed(2) + '%</span>';
        metrics[6].innerHTML = "<strong>Today\'s Gain</strong>" +
          '<span class="metric-value ' + (totalTodaysGain >= 0 ? 'positive' : 'negative') + '">Rs. ' + formatMoney(totalTodaysGain) + '</span>';
        metrics[7].innerHTML = "<strong>Today\'s Gain %</strong>" +
          '<span class="metric-value ' + (todaysGainPercent >= 0 ? 'positive' : 'negative') + '">' + todaysGainPercent.toFixed(2) + '%</span>';
      }

      grandInv += totalInvestment;
      grandVal += totalCurrentValue;
      grandPL += totalProfitLoss;
      grandRealized += parseFloat(card.dataset.realized || "0") || 0;
    });

    var sInv = document.getElementById("summaryTotalInvestment");
    var sVal = document.getElementById("summaryCurrentValue");
    var sPL = document.getElementById("summaryUnrealizedPL");
    var sReal = document.getElementById("summaryRealizedPL");
    if (sInv) sInv.textContent = "Rs. " + formatMoney(grandInv);
    if (sVal) sVal.textContent = "Rs. " + formatMoney(grandVal);
    if (sPL) {
      sPL.textContent = (grandPL >= 0 ? "+" : "") + "Rs. " + formatMoney(grandPL);
      sPL.className = "overview-val " + (grandPL >= 0 ? "positive" : "negative");
    }
    if (sReal) {
      sReal.textContent = (grandRealized >= 0 ? "+" : "") + "Rs. " + formatMoney(grandRealized);
      sReal.className = "overview-val " + (grandRealized >= 0 ? "positive" : "negative");
    }
  } catch (error) {
    console.error("API Error:", error);
    document.querySelectorAll(".stock-ltp.loading").forEach(function (cell) {
      cell.innerText = "N/A";
    });
  }
}

updateStockPrices();
setInterval(updateStockPrices, 30000);

async function checkMarketStatus() {
  var statusDiv = document.getElementById("market-status");
  if (!statusDiv) return;
  try {
    var response = await fetch("/IsNepseOpen");
    var data = await response.json();
    if (data.isOpen === "OPEN") {
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

var depthModal = document.getElementById("depthModal");
var currentDepthSymbol = "";

function openDepthModal(symbol) {
  currentDepthSymbol = symbol;
  document.getElementById("depthSymbol").textContent = symbol;
  document.getElementById("buySymbol").textContent = symbol;
  document.getElementById("sellSymbol").textContent = symbol;
  depthModal.style.display = "block";
  document.getElementById("depthLoading").style.display = "block";
  document.getElementById("depthTableWrap").style.display = "none";
  document.getElementById("depthClosed").style.display = "none";
  loadMarketDepth(symbol);
}

function closeDepthModal() {
  if (depthModal) depthModal.style.display = "none";
}

document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && depthModal && depthModal.style.display === "block") {
    closeDepthModal();
  }
});

function loadMarketDepth(symbol) {
  Promise.all([
    fetch("/MarketDepth?symbol=" + encodeURIComponent(symbol)),
    fetch("/PriceVolume")
  ])
    .then(function (responses) {
      return Promise.all(responses.map(function (r) {
        if (!r.ok) throw new Error("API Error: " + r.status);
        return r.json();
      }));
    })
    .then(function (results) {
      var depthData = results[0];
      var priceData = results[1];

      document.getElementById("depthLoading").style.display = "none";

      var buyList = (depthData.marketDepth && depthData.marketDepth.buyMarketDepthList) ? depthData.marketDepth.buyMarketDepthList : [];
      var sellList = (depthData.marketDepth && depthData.marketDepth.sellMarketDepthList) ? depthData.marketDepth.sellMarketDepthList : [];

      if (buyList.length === 0 && sellList.length === 0) {
        document.getElementById("depthClosed").style.display = "block";
        document.getElementById("depthTableWrap").style.display = "none";
        return;
      }

      document.getElementById("depthTableWrap").style.display = "block";

      var ltp = 0;
      var pctChange = 0;
      if (Array.isArray(priceData)) {
        var stockInfo = priceData.find(function (item) {
          return item.symbol && item.symbol.toUpperCase() === symbol.toUpperCase();
        });
        if (stockInfo) {
          ltp = parseFloat(stockInfo.lastTradedPrice ?? stockInfo.ltp) || 0;
          pctChange = parseFloat(stockInfo.percentageChange ?? stockInfo.percent_change) || 0;
        }
      }

      var ltpHtml = "";
      if (ltp > 0) {
        var pctSign = pctChange >= 0 ? "+" : "";
        ltpHtml = '<div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">' +
          '<span style="font-size:26px;font-weight:800;color:var(--text-primary);">Rs. ' + formatMoney(ltp) + '</span>' +
          '<span style="font-size:13px;font-weight:700;padding:4px 10px;border-radius:8px;background:' + (pctChange >= 0 ? 'var(--green-soft)' : 'var(--red-soft)') + ';color:' + (pctChange >= 0 ? 'var(--green)' : 'var(--red)') + ';">' +
          pctSign + pctChange.toFixed(2) + '%</span></div>';
      }

      var tableWrap = document.getElementById("depthTableWrap");
      var existingLtp = document.getElementById("depthLTP");
      if (existingLtp) existingLtp.remove();
      var ltpDiv = document.createElement("div");
      ltpDiv.id = "depthLTP";
      ltpDiv.innerHTML = ltpHtml;
      tableWrap.insertBefore(ltpDiv, tableWrap.firstChild);

      var tbody = document.getElementById("depthTableBody");
      tbody.innerHTML = "";

      for (var i = 0; i < 5; i++) {
        var buy = buyList[i] || {};
        var sell = sellList[i] || {};
        var row = document.createElement("tr");
        row.innerHTML =
          '<td class="buy-cell">' + (buy.orderCount != null ? buy.orderCount : "-") + '</td>' +
          '<td class="buy-cell">' + (buy.quantity != null ? Number(buy.quantity).toLocaleString() : "-") + '</td>' +
          '<td class="buy-cell">' + (buy.orderBookOrderPrice != null ? buy.orderBookOrderPrice : "-") + '</td>' +
          '<td class="sell-cell">' + (sell.orderBookOrderPrice != null ? sell.orderBookOrderPrice : "-") + '</td>' +
          '<td class="sell-cell">' + (sell.quantity != null ? Number(sell.quantity).toLocaleString() : "-") + '</td>' +
          '<td class="sell-cell">' + (sell.orderCount != null ? sell.orderCount : "-") + '</td>';
        tbody.appendChild(row);
      }

      document.getElementById("totalBuyQty").textContent = Number(depthData.totalBuyQty || 0).toLocaleString();
      document.getElementById("totalSellQty").textContent = Number(depthData.totalSellQty || 0).toLocaleString();
    })
    .catch(function (err) {
      console.error("Depth API Error:", err);
      document.getElementById("depthLoading").style.display = "none";
      document.getElementById("depthClosed").style.display = "block";
      document.getElementById("depthTableWrap").style.display = "none";
    });
}

function handleDepthBuy() {
  var link = document.getElementById("depthBuyLink");
  if (link) {
    link.href = "https://tms87.nepsetms.com.np/tms/me/memberclientorderentry?symbol=" + encodeURIComponent(currentDepthSymbol) + "&transaction=buy";
  }
}

function handleDepthSell() {
  var link = document.getElementById("depthSellLink");
  if (link) {
    link.href = "https://tms87.nepsetms.com.np/tms/me/memberclientorderentry?symbol=" + encodeURIComponent(currentDepthSymbol) + "&transaction=sell";
  }
}
