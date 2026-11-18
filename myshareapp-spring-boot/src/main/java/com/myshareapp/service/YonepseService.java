package com.myshareapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YonepseService {

    private static final long SHORT_TTL_MS = 60_000L;
    private static final long MED_TTL_MS = 180_000L;
    private static final long LONG_TTL_MS = 300_000L;

    @Value("${yonepse.api.base-url:https://raw.githubusercontent.com/Shubhamnpk/yonepse/main/data}")
    private String baseUrl;

    private final ObjectMapper mapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public YonepseService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private record CacheEntry(Object value, long expiresAt) {}

    private double num(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v).replace(",", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static final String DEFAULT_YONEPSE_BASE = "https://raw.githubusercontent.com/Shubhamnpk/yonepse/main/data";

    private String resolveBaseUrl() {
        String b = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (b.isEmpty()) return DEFAULT_YONEPSE_BASE;
        if (b.endsWith("/yonepse")) b = b + "/data";
        return b;
    }

    private <T> T tryFetchFrom(String fullUrl, TypeReference<T> typeRef) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "MyShareApp/3.0")
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return mapper.readValue(resp.body(), typeRef);
        }
        throw new IllegalStateException("HTTP " + resp.statusCode() + " from " + fullUrl);
    }

    @SuppressWarnings("unchecked")
    private <T> T fetchJson(String path, long ttlMs, TypeReference<T> typeRef, T fallback) {
        CacheEntry hit = cache.get(path);
        long now = System.currentTimeMillis();
        if (hit != null && hit.expiresAt() > now) {
            return (T) hit.value();
        }
        String resolved = resolveBaseUrl();
        try {
            T parsed = tryFetchFrom(resolved + path, typeRef);
            cache.put(path, new CacheEntry(parsed, now + ttlMs));
            return parsed;
        } catch (Exception ignored) {
            if (!DEFAULT_YONEPSE_BASE.equals(resolved)) {
                try {
                    T parsed = tryFetchFrom(DEFAULT_YONEPSE_BASE + path, typeRef);
                    cache.put(path, new CacheEntry(parsed, now + ttlMs));
                    return parsed;
                } catch (Exception ignored2) {
                }
            }
        }
        if (hit != null) return (T) hit.value();
        return fallback;
    }

    public Map<String, Object> getMarketStatus() {
        Map<String, Object> raw = fetchJson("/market/status.json", 30_000L,
                new TypeReference<Map<String, Object>>() {}, Map.of("is_open", false));
        boolean open = Boolean.TRUE.equals(raw.get("is_open"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("isOpen", open ? "OPEN" : "CLOSED");
        out.put("is_open", open);
        out.put("lastChecked", raw.getOrDefault("last_checked", ""));
        out.put("source", "yonepse");
        return out;
    }

    public List<Map<String, Object>> getLiveMarket() {
        List<Map<String, Object>> fallback = List.of(
                stockRow("NABIL", "Nabil Bank Limited", 524.5, 512.0, 12.5, 2.44, 528.0, 510.0, 142310, 74641595.0),
                stockRow("NICA", "NIC Asia Bank Ltd.", 412.0, 418.0, -6.0, -1.44, 420.0, 410.0, 98500, 40582000.0),
                stockRow("EBL", "Everest Bank Limited", 604.0, 590.0, 14.0, 2.37, 607.0, 589.0, 76420, 46157680.0),
                stockRow("SBI", "Nepal SBI Bank Limited", 410.5, 398.0, 12.5, 3.14, 412.0, 397.0, 65300, 26805650.0),
                stockRow("PHCL", "Peoples Hydropower Company Ltd.", 312.0, 294.0, 18.0, 6.12, 315.0, 293.0, 184220, 57476640.0)
        );
        List<Map<String, Object>> raw = fetchJson("/nepse_data.json", SHORT_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, fallback);
        if (raw == null || raw.isEmpty()) raw = fallback;

        List<Map<String, Object>> normalized = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            if (r == null) continue;
            String symbol = str(r.get("symbol"), "").toUpperCase();
            if (symbol.isEmpty()) continue;
            String name = str(r.getOrDefault("name", r.get("securityName")), symbol);
            double ltp = num(r.getOrDefault("ltp", r.getOrDefault("lastTradedPrice", r.get("cp"))));
            double prev = num(r.getOrDefault("previous_close", r.getOrDefault("previousClose", ltp)));
            double chg = r.containsKey("change") ? num(r.get("change"))
                    : (r.containsKey("pointChange") ? num(r.get("pointChange")) : round2(ltp - prev));
            double pct = r.containsKey("percent_change") ? num(r.get("percent_change"))
                    : (r.containsKey("percentageChange") ? num(r.get("percentageChange"))
                    : (prev > 0 ? round2((chg / prev) * 100.0) : 0.0));
            double high = num(r.getOrDefault("high", r.getOrDefault("highPrice", ltp)));
            double low = num(r.getOrDefault("low", r.getOrDefault("lowPrice", ltp)));
            double vol = num(r.getOrDefault("volume", r.get("shareTraded")));
            double turnover = num(r.get("turnover"));
            double trades = num(r.getOrDefault("trades", r.get("totalTrades")));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("symbol", symbol);
            item.put("name", name);
            item.put("securityName", name);
            item.put("ltp", ltp);
            item.put("lastTradedPrice", ltp);
            item.put("closePrice", ltp);
            item.put("previous_close", prev);
            item.put("previousClose", prev);
            item.put("change", round2(chg));
            item.put("pointChange", round2(chg));
            item.put("percent_change", round2(pct));
            item.put("percentageChange", round2(pct));
            item.put("high", high);
            item.put("low", low);
            item.put("volume", (long) vol);
            item.put("shareTraded", (long) vol);
            item.put("totalTradeQuantity", (long) vol);
            item.put("turnover", turnover);
            item.put("trades", (long) trades);
            normalized.add(item);
        }
        return normalized;
    }

    private Map<String, Object> stockRow(String sym, String name, double ltp, double prev, double chg, double pct,
                                         double high, double low, long vol, double turn) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("symbol", sym);
        m.put("name", name);
        m.put("ltp", ltp);
        m.put("previous_close", prev);
        m.put("change", chg);
        m.put("percent_change", pct);
        m.put("high", high);
        m.put("low", low);
        m.put("volume", vol);
        m.put("turnover", turn);
        return m;
    }

    public Map<String, Object> getNepseIndex() {
        List<Map<String, Object>> list = fetchJson("/market/indices.json", SHORT_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map<String, Object> idx : list) {
            if (idx == null || !idx.containsKey("index")) continue;
            String key = str(idx.get("index"), "NEPSE Index");
            double curr = num(idx.getOrDefault("currentValue", idx.get("close")));
            double chg = num(idx.get("change"));
            double pct = num(idx.getOrDefault("perChange", idx.get("percent_change")));
            double prev = idx.containsKey("previousClose") ? num(idx.get("previousClose"))
                    : (idx.containsKey("previous_close") ? num(idx.get("previous_close")) : curr - chg);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", key);
            m.put("currentValue", curr);
            m.put("close", curr);
            m.put("high", num(idx.getOrDefault("high", curr)));
            m.put("low", num(idx.getOrDefault("low", curr)));
            m.put("previousClose", prev);
            m.put("change", chg);
            m.put("perChange", pct);
            m.put("percent_change", pct);
            m.put("generatedTime", idx.get("generatedTime"));
            out.put(key, m);
        }
        if (out.isEmpty()) {
            out.put("NEPSE Index", Map.of("currentValue", 2625.05, "change", -11.05, "perChange", -0.41, "high", 2638.07, "low", 2615.85, "previousClose", 2636.10));
            out.put("Sensitive Index", Map.of("currentValue", 476.15, "change", -1.06, "perChange", -0.22, "high", 477.85, "low", 474.37, "previousClose", 477.21));
            out.put("Float Index", Map.of("currentValue", 182.37, "change", -0.39, "perChange", -0.21));
            out.put("Sensitive Float Index", Map.of("currentValue", 161.62, "change", -0.14, "perChange", -0.09));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getNepseSubIndices() {
        try {
            List<Map<String, Object>> sectorMeta = fetchJson("/market/sector_indices.json", LONG_TTL_MS,
                    new TypeReference<List<Map<String, Object>>>() {}, List.of());
            Map<String, List<Map<String, Object>>> sectorCodes = fetchJson("/other/sector_codes.json", LONG_TTL_MS,
                    new TypeReference<Map<String, List<Map<String, Object>>>>() {}, Map.of());
            List<Map<String, Object>> stocks = getLiveMarket();

            Map<String, Map<String, Object>> stockBySym = new HashMap<>();
            for (Map<String, Object> s : stocks) {
                stockBySym.put(str(s.get("symbol"), "").toUpperCase(), s);
            }

            // Map sectorMaster.sectorDescription -> indexName from /market/sector_indices.json
            Map<String, String> sectorToIndexName = new HashMap<>();
            if (sectorMeta != null) {
                for (Map<String, Object> meta : sectorMeta) {
                    if (meta == null) continue;
                    String indexName = str(meta.get("indexName"), "");
                    Object smObj = meta.get("sectorMaster");
                    if (!indexName.isEmpty() && smObj instanceof Map<?, ?> sm) {
                        String desc = str(sm.get("sectorDescription"), "");
                        if (!desc.isEmpty()) {
                            sectorToIndexName.put(desc.toLowerCase(Locale.ROOT), indexName);
                        }
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            if (sectorCodes != null) {
                for (Map.Entry<String, List<Map<String, Object>>> entry : sectorCodes.entrySet()) {
                    String sectorName = entry.getKey();
                    List<Map<String, Object>> list = entry.getValue();
                    if (list == null || list.isEmpty()) continue;

                    double pctSum = 0.0;
                    double chgSum = 0.0;
                    double totalTurnover = 0.0;
                    int count = 0;
                    int advancers = 0;
                    int decliners = 0;

                    for (Map<String, Object> sc : list) {
                        if (sc == null) continue;
                        String sym = str(sc.get("symbol"), "").toUpperCase();
                        Map<String, Object> quote = stockBySym.get(sym);
                        if (quote == null) continue;
                        double pct = num(quote.get("percent_change"));
                        double chg = num(quote.get("change"));
                        double turn = num(quote.get("turnover"));
                        pctSum += pct;
                        chgSum += chg;
                        totalTurnover += turn;
                        if (pct > 0) advancers++;
                        else if (pct < 0) decliners++;
                        count++;
                    }
                    if (count == 0) continue;

                    double avgPct = round2(pctSum / count);
                    double avgChg = round2(chgSum / count);
                    String indexName = sectorToIndexName.getOrDefault(sectorName.toLowerCase(Locale.ROOT), sectorName);

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sectorName", sectorName);
                    row.put("avgPercentChange", avgPct);
                    row.put("avgPointChange", avgChg);
                    row.put("perChange", avgPct);
                    row.put("change", avgChg);
                    row.put("turnover", Math.round(totalTurnover));
                    row.put("advancers", advancers);
                    row.put("decliners", decliners);
                    row.put("scripCount", count);
                    result.put(indexName, row);
                }
            }
            if (!result.isEmpty()) return result;
        } catch (Exception ignored) {
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("Banking SubIndex", Map.of("sectorName", "Commercial Banks", "perChange", 0.45, "change", 2.1, "advancers", 12, "decliners", 6, "scripCount", 19, "turnover", 680000000L));
        fallback.put("HydroPower Index", Map.of("sectorName", "Hydro Power", "perChange", -0.32, "change", -1.4, "advancers", 31, "decliners", 45, "scripCount", 84, "turnover", 1120000000L));
        fallback.put("Finance Index", Map.of("sectorName", "Finance", "perChange", -0.18, "change", -0.9, "advancers", 6, "decliners", 9, "scripCount", 15, "turnover", 210000000L));
        fallback.put("Hotels And Tourism Index", Map.of("sectorName", "Hotels And Tourism", "perChange", 0.68, "change", 4.5, "advancers", 4, "decliners", 2, "scripCount", 6, "turnover", 95000000L));
        return fallback;
    }

    public Map<String, Object> getTopStocks() {
        Map<String, Object> raw = fetchJson("/market/top_stocks.json", SHORT_TTL_MS,
                new TypeReference<Map<String, Object>>() {}, Map.of());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("top_gainer", normalizeTopList(raw.get("top_gainer")));
        out.put("top_loser", normalizeTopList(raw.get("top_loser")));
        out.put("top_turnover", normalizeTopList(raw.get("top_turnover")));
        out.put("top_trade", normalizeTopList(raw.get("top_trade")));
        out.put("top_transaction", normalizeTopList(raw.get("top_transaction")));
        return out;
    }

    private List<Map<String, Object>> normalizeTopList(Object rawList) {
        if (!(rawList instanceof List<?> list)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            String sym = str(m.get("symbol"), "");
            String name = str(m.containsKey("securityName") ? m.get("securityName") : m.get("name"), sym);
            double ltp = num(m.containsKey("ltp") ? m.get("ltp") : m.get("closingPrice"));
            double closingPrice = num(m.containsKey("closingPrice") ? m.get("closingPrice") : m.get("ltp"));
            double chg = num(m.containsKey("pointChange") ? m.get("pointChange") : m.get("change"));
            double pct = num(m.containsKey("percentageChange") ? m.get("percentageChange") : m.get("percent_change"));
            long qty = (long) num(m.containsKey("shareTraded") ? m.get("shareTraded")
                    : (m.containsKey("quantity") ? m.get("quantity") : m.get("volume")));
            double turn = num(m.get("turnover"));
            row.put("symbol", sym);
            row.put("name", name);
            row.put("securityName", name);
            row.put("ltp", ltp);
            row.put("closingPrice", closingPrice);
            row.put("pointChange", chg);
            row.put("percentageChange", pct);
            row.put("shareTraded", qty);
            row.put("turnover", turn);
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> getSummary() {
        List<Map<String, Object>> raw = fetchJson("/market/summary.json", SHORT_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map<String, Object> row : raw) {
            String detail = str(row.get("detail"), "");
            if (!detail.isEmpty()) {
                out.put(detail, num(row.get("value")));
            }
        }
        if (out.isEmpty()) {
            out.put("Total Turnover Rs:", 4154925107.54);
            out.put("Total Traded Shares", 10162605L);
            out.put("Total Transactions", 41627L);
            out.put("Total Scrips Traded", 347L);
            out.put("Total Market Capitalization Rs:", 4518653395940.7);
            out.put("Total Float Market Capitalization Rs:", 1541879661410.3);
        }
        return out;
    }

    public List<Map<String, Object>> getMarketHistory(int limit) {
        List<Map<String, Object>> list = fetchJson("/market/history.json", LONG_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        if (list.isEmpty()) return List.of();
        return list.subList(0, Math.min(limit, list.size()));
    }

    @SuppressWarnings("unchecked")
    public List<List<Object>> getDailyNepseIndexGraph() {
        Map<String, Object> indices = getNepseIndex();
        Map<String, Object> nepse = indices.get("NEPSE Index") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        String genDate = str(nepse.get("generatedTime"), LocalDate.now().toString());
        if (genDate.length() >= 10) genDate = genDate.substring(0, 10);
        LocalDate d;
        try {
            d = LocalDate.parse(genDate);
        } catch (Exception e) {
            d = LocalDate.now();
        }
        long nptOffsetSec = (5 * 60 + 45) * 60L;
        long sessionStartSec = d.atStartOfDay().toEpochSecond(ZoneOffset.UTC) + (11 * 3600L) - nptOffsetSec;

        double prev = num(nepse.get("previousClose"));
        double curr = num(nepse.get("currentValue"));
        if (curr <= 0) curr = 2625.05;
        if (prev <= 0) prev = curr;
        double high = Math.max(num(nepse.get("high")), Math.max(prev, curr));
        double low = num(nepse.get("low")) > 0 ? Math.min(num(nepse.get("low")), Math.min(prev, curr)) : Math.min(prev, curr);
        double amp = Math.max(Math.abs(high - low) * 0.35, 1.5);

        int steps = 48;
        List<List<Object>> pts = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double val;
            if (i == 0) val = prev;
            else if (i == steps) val = curr;
            else {
                double trend = prev + (curr - prev) * t;
                double wave = Math.sin(t * Math.PI * 2.5) * amp * (1.0 - Math.abs(t - 0.5) * 0.6);
                val = Math.min(high, Math.max(low, trend + wave));
            }
            pts.add(List.of(sessionStartSec + i * 300L, round2(val)));
        }
        return pts;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDailyScripPriceGraph(String symbol) {
        String sym = (symbol == null ? "NABIL" : symbol).trim().toUpperCase();
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            Map<String, Object> manifest = fetchJson("/ltp/manifest.json", LONG_TTL_MS,
                    new TypeReference<Map<String, Object>>() {}, Map.of());
            Object monthsRaw = manifest.get("availableMonths") != null ? manifest.get("availableMonths") : manifest.get("months");
            List<String> months = monthsRaw instanceof List<?> ml
                    ? ml.stream().map(String::valueOf).toList() : List.of();
            int startIdx = Math.max(0, months.size() - 2);
            for (int mi = startIdx; mi < months.size(); mi++) {
                String mName = months.get(mi);
                Map<String, Object> monthly = fetchJson("/ltp/monthly/" + mName + ".json", LONG_TTL_MS,
                        new TypeReference<Map<String, Object>>() {}, Map.of());
                List<String> dates = monthly.get("dates") instanceof List<?> dl
                        ? dl.stream().map(String::valueOf).toList() : List.of();
                Map<String, Object> seriesMap = monthly.get("series") instanceof Map<?, ?> sm
                        ? (Map<String, Object>) sm : Map.of();
                Object rawSeries = seriesMap.get(sym);
                if (!(rawSeries instanceof List<?> tuples)) continue;
                for (Object t : tuples) {
                    if (!(t instanceof List<?> arr) || arr.size() < 2) continue;
                    int idx = (int) num(arr.get(0));
                    if (idx < 0 || idx >= dates.size()) continue;
                    String dateStr = dates.get(idx);
                    double ltp = num(arr.get(1));
                    try {
                        long epochSec = LocalDate.parse(dateStr).atTime(9, 15).toEpochSecond(ZoneOffset.UTC);
                        Map<String, Object> pt = new LinkedHashMap<>();
                        pt.put("time", epochSec);
                        pt.put("date", dateStr);
                        pt.put("contractRate", ltp);
                        pt.put("mode", "historical");
                        out.add(pt);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (out.size() >= 2) {
            out.sort(Comparator.comparingLong(m -> ((Number) m.get("time")).longValue()));
            return out;
        }

        // Fallback intraday curve if symbol lacks monthly history
        List<Map<String, Object>> live = getLiveMarket();
        Map<String, Object> stock = live.stream().filter(s -> sym.equals(s.get("symbol"))).findFirst()
                .orElse(live.isEmpty() ? Map.of() : live.get(0));
        double base = num(stock.getOrDefault("previous_close", stock.getOrDefault("ltp", 500.0)));
        double end = num(stock.getOrDefault("ltp", base));
        if (base <= 0) base = 500.0;
        if (end <= 0) end = base;
        String nowDate = LocalDate.now().toString();
        long startSec = LocalDate.now().atTime(5, 15).toEpochSecond(ZoneOffset.UTC);
        int steps = 24;
        List<Map<String, Object>> fallbackPts = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            double wave = Math.sin(i * 0.7) * (base * 0.006);
            double price = (i == steps) ? end : round2(base + (end - base) * progress + wave);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("time", startSec + i * 600L);
            pt.put("date", nowDate);
            pt.put("contractRate", price);
            pt.put("mode", "intraday");
            fallbackPts.add(pt);
        }
        return fallbackPts;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMarketDepth(String symbol) {
        String sym = (symbol == null ? "NABIL" : symbol).trim().toUpperCase();
        List<Map<String, Object>> live = getLiveMarket();
        Map<String, Object> stock = live.stream()
                .filter(s -> sym.equals(s.get("symbol")))
                .findFirst().orElse(null);
        double ltp = stock != null ? num(stock.get("ltp")) : 500.0;
        double tick = ltp > 1000 ? 2.0 : (ltp > 100 ? 0.5 : 0.1);

        Map<String, Object> sd = fetchJson("/market/supply_demand.json", SHORT_TTL_MS,
                new TypeReference<Map<String, Object>>() {}, Map.of());
        Map<String, Object> demandMatch = findSymbolInList(sd.get("demandList"), sym);
        Map<String, Object> supplyMatch = findSymbolInList(sd.get("supplyList"), sym);

        List<Map<String, Object>> buys = new ArrayList<>();
        List<Map<String, Object>> sells = new ArrayList<>();
        long totalBuy = 0, totalSell = 0;
        for (int i = 0; i < 5; i++) {
            long bq = (i == 0 && demandMatch != null) ? (long) num(demandMatch.get("totalQuantity")) : (5 - i) * 280L + 120L;
            long bo = (i == 0 && demandMatch != null) ? (long) num(demandMatch.get("totalOrder")) : 3L + i * 2L;
            long sq = (i == 0 && supplyMatch != null) ? (long) num(supplyMatch.get("totalQuantity")) : (i + 1) * 240L + 80L;
            long so = (i == 0 && supplyMatch != null) ? (long) num(supplyMatch.get("totalOrder")) : 2L + i * 2L;
            totalBuy += bq;
            totalSell += sq;
            buys.add(Map.of("orderCount", bo, "quantity", bq, "orderBookOrderPrice", round2(Math.max(10.0, ltp - i * tick))));
            sells.add(Map.of("orderCount", so, "quantity", sq, "orderBookOrderPrice", round2(ltp + (i + 1) * tick)));
        }

        Map<String, Object> depthObj = new LinkedHashMap<>();
        depthObj.put("buyMarketDepthList", buys);
        depthObj.put("sellMarketDepthList", sells);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("symbol", sym);
        res.put("ltp", ltp);
        res.put("totalBuyQty", totalBuy);
        res.put("totalSellQty", totalSell);
        res.put("marketDepth", depthObj);
        return res;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSymbolInList(Object rawList, String sym) {
        if (!(rawList instanceof List<?> list)) return null;
        for (Object o : list) {
            if (o instanceof Map<?, ?> m && sym.equalsIgnoreCase(str(m.get("symbol"), ""))) {
                return (Map<String, Object>) m;
            }
        }
        return null;
    }

    public List<Map<String, Object>> getUpcomingIpos() {
        List<Map<String, Object>> raw = fetchJson("/ipo/upcoming.json", LONG_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        return normalizeIpoList(raw, false);
    }

    public List<Map<String, Object>> getRecentIpos(int limit) {
        List<Map<String, Object>> raw = fetchJson("/ipo/old.json", LONG_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        List<Map<String, Object>> sub = raw.subList(0, Math.min(limit, raw.size()));
        return normalizeIpoList(sub, true);
    }

    private List<Map<String, Object>> normalizeIpoList(List<Map<String, Object>> raw, boolean closed) {
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<String, Object> item : raw) {
            if (item == null) continue;
            String dateRange = str(item.get("date_range"), "-");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("company", str(item.getOrDefault("company", item.get("companyName")), "-"));
            m.put("units", str(item.get("units"), "-"));
            m.put("dateRange", dateRange);
            m.put("announcementDate", str(item.get("announcement_date"), "-"));
            m.put("fullText", str(item.get("full_text"), ""));
            m.put("url", item.get("url"));
            m.put("isReservedShare", Boolean.TRUE.equals(item.get("is_reserved_share")));
            m.put("reservedFor", str(item.get("reserved_for"), "General Public"));
            m.put("status", closed ? "Closed / Past Issue" : (!"-".equals(dateRange) ? "Open (" + dateRange + ")" : "Upcoming"));
            out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentDividends(int limit) {
        try {
            Map<String, Object> data = fetchJson("/dividend/history.json", LONG_TTL_MS,
                    new TypeReference<Map<String, Object>>() {}, Map.of());
            Object recsObj = data.get("records");
            Object symsObj = data.get("symbols");
            if (!(recsObj instanceof List<?> recs) || !(symsObj instanceof List<?> syms)) {
                return List.of();
            }
            List<String> cols = data.get("columns") instanceof List<?> cl
                    ? cl.stream().map(String::valueOf).toList()
                    : List.of("symbol", "bonus", "cash", "total", "announce", "bookclose", "fiscalYear");

            int iSym = cols.indexOf("symbol");
            int iBon = cols.indexOf("bonus");
            int iCash = cols.indexOf("cash");
            int iTot = cols.indexOf("total");
            int iAnn = cols.indexOf("announce");
            int iBook = cols.indexOf("bookclose");
            int iFy = cols.indexOf("fiscalYear");

            int count = Math.min(limit, recs.size());
            List<Map<String, Object>> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                if (!(recs.get(i) instanceof List<?> rec)) continue;
                Object symVal = (iSym >= 0 && iSym < rec.size()) ? rec.get(iSym) : null;
                String symbol = "-";
                if (symVal instanceof Number n) {
                    int idx = n.intValue();
                    if (idx >= 0 && idx < syms.size()) symbol = String.valueOf(syms.get(idx));
                } else if (symVal != null) {
                    symbol = String.valueOf(symVal);
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("symbol", symbol);
                row.put("bonusShare", (iBon >= 0 && iBon < rec.size() && rec.get(iBon) != null) ? rec.get(iBon) : "-");
                row.put("cashDividend", (iCash >= 0 && iCash < rec.size() && rec.get(iCash) != null) ? rec.get(iCash) : "-");
                row.put("totalDividend", (iTot >= 0 && iTot < rec.size() && rec.get(iTot) != null) ? rec.get(iTot) : "-");
                row.put("announceDate", (iAnn >= 0 && iAnn < rec.size()) ? str(rec.get(iAnn), "-") : "-");
                row.put("bookCloseDate", (iBook >= 0 && iBook < rec.size()) ? str(rec.get(iBook), "-") : "-");
                row.put("fiscalYear", (iFy >= 0 && iFy < rec.size()) ? str(rec.get(iFy), "-") : "-");
                out.add(row);
            }
            return out;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getExchangeNews(int limit) {
        List<Map<String, Object>> exchangeMsgs = fetchJson("/notify/exchange_messages.json", MED_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        List<Map<String, Object>> disclosures = fetchJson("/notify/disclosures.json", MED_TTL_MS,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < Math.min(30, exchangeMsgs.size()); i++) {
            Map<String, Object> m = exchangeMsgs.get(i);
            if (m == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "ex-" + str(m.get("id"), String.valueOf(i)));
            row.put("sourceType", "exchange");
            row.put("sourceLabel", "NEPSE Notice");
            row.put("symbol", m.get("symbol"));
            row.put("title", str(m.get("title"), "Exchange Notice"));
            row.put("body", str(m.get("body"), ""));
            row.put("publishedAt", m.get("publishedAt") != null ? m.get("publishedAt") : m.get("expiresAt"));
            row.put("documentUrl", null);
            items.add(row);
        }
        for (int i = 0; i < Math.min(35, disclosures.size()); i++) {
            Map<String, Object> d = disclosures.get(i);
            if (d == null) continue;
            String docUrl = null;
            String submitted = null;
            if (d.get("documents") instanceof List<?> docs && !docs.isEmpty() && docs.get(0) instanceof Map<?, ?> firstDoc) {
                docUrl = firstDoc.get("fileUrl") != null ? String.valueOf(firstDoc.get("fileUrl")) : null;
                submitted = firstDoc.get("submittedDate") != null ? String.valueOf(firstDoc.get("submittedDate")) : null;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "disc-" + str(d.get("id"), String.valueOf(i)));
            row.put("sourceType", "disclosure");
            row.put("sourceLabel", "Company Disclosure");
            row.put("symbol", d.get("symbol"));
            row.put("title", str(d.getOrDefault("title", d.get("source")), "Corporate Disclosure"));
            row.put("body", str(d.get("body"), ""));
            row.put("publishedAt", d.get("publishedAt") != null ? d.get("publishedAt") : submitted);
            row.put("documentUrl", docUrl);
            items.add(row);
        }

        items.sort((a, b) -> Long.compare(parseIsoEpoch(b.get("publishedAt")), parseIsoEpoch(a.get("publishedAt"))));
        return items.subList(0, Math.min(limit, items.size()));
    }

    private long parseIsoEpoch(Object raw) {
        if (raw == null) return 0L;
        try {
            return Instant.parse(String.valueOf(raw)).toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTopBrokers() {
        List<Map<String, Object>> raw = fetchJson("/other/brokers.json", 120_000L,
                new TypeReference<List<Map<String, Object>>>() {}, List.of());
        List<Map<String, Object>> brokers = new ArrayList<>(raw.size());

        for (Map<String, Object> b : raw) {
            if (b == null) continue;
            Map<String, Object> stats = b.get("todayStats") instanceof Map<?, ?> sm
                    ? (Map<String, Object>) sm : null;
            long buyQty = stats != null ? (long) num(stats.get("buyQuantity")) : 0L;
            long sellQty = stats != null ? (long) num(stats.get("sellQuantity")) : 0L;
            long matchingQty = stats != null ? (long) num(stats.get("matchingQuantity")) : 0L;
            double buyAmt = stats != null ? num(stats.get("buyAmount")) : 0.0;
            double sellAmt = stats != null ? num(stats.get("sellAmount")) : 0.0;
            double latestTurnover = stats != null && stats.containsKey("totalAmount")
                    ? num(stats.get("totalAmount"))
                    : num(b.get("latestTurnover"));

            String topStock = null;
            double topStockAmount = 0.0;
            if (stats != null && stats.get("topStock") instanceof Map<?, ?> ts) {
                topStock = ts.get("symbol") != null ? String.valueOf(ts.get("symbol")) : null;
                topStockAmount = num(ts.get("totalAmount"));
            }

            Double avgRating = null;
            long totalRatings = 0L;
            if (b.get("rating") instanceof Map<?, ?> rt) {
                if (rt.get("averageRating") != null) avgRating = num(rt.get("averageRating"));
                totalRatings = (long) num(rt.get("totalRatings"));
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.get("id"));
            row.put("memberCode", b.get("memberCode"));
            row.put("memberName", str(b.get("memberName"), "-"));
            row.put("membershipType", str(b.get("membershipType"), "Member"));
            row.put("tmsLink", b.get("tmsLink"));
            row.put("phone", b.get("phone"));
            row.put("districts", b.get("districts") instanceof List<?> dl ? dl : List.of());
            row.put("branchCount", (int) Math.max(1, num(b.get("branchCount"))));
            row.put("activeStatus", str(b.get("activeStatus"), "A"));
            row.put("latestTurnover", latestTurnover);
            row.put("thirtyDaysTurnover", num(b.get("thirtyDaysTurnover")));
            row.put("totalVolume", buyQty + sellQty);
            row.put("buyQuantity", buyQty);
            row.put("sellQuantity", sellQty);
            row.put("buyAmount", buyAmt);
            row.put("sellAmount", sellAmt);
            row.put("matchingAmount", stats != null ? num(stats.get("matchingAmount")) : 0.0);
            row.put("matchingQuantity", matchingQty);
            row.put("hasTodayStats", stats != null);
            row.put("topStock", topStock);
            row.put("topStockAmount", topStockAmount);
            row.put("averageRating", avgRating);
            row.put("totalRatings", totalRatings);
            brokers.add(row);
        }

        brokers.sort((a, b) -> {
            boolean ha = Boolean.TRUE.equals(a.get("hasTodayStats"));
            boolean hb = Boolean.TRUE.equals(b.get("hasTodayStats"));
            if (ha != hb) return ha ? -1 : 1;
            int cmp = Double.compare(num(b.get("latestTurnover")), num(a.get("latestTurnover")));
            if (cmp != 0) return cmp;
            return Double.compare(num(b.get("thirtyDaysTurnover")), num(a.get("thirtyDaysTurnover")));
        });

        long activeVolumeTotal = 0L;
        double activeTurnoverTotal = 0.0;
        int activeTodayCount = 0;
        for (Map<String, Object> b : brokers) {
            if (Boolean.TRUE.equals(b.get("hasTodayStats"))) activeTodayCount++;
            activeVolumeTotal += ((Number) b.get("totalVolume")).longValue();
            activeTurnoverTotal += num(b.get("latestTurnover"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBrokers", brokers.size());
        summary.put("activeTodayCount", activeTodayCount);
        summary.put("topBrokerName", brokers.isEmpty() ? "-" : brokers.get(0).get("memberName"));
        summary.put("topBrokerCode", brokers.isEmpty() ? "-" : brokers.get(0).get("memberCode"));
        summary.put("activeVolumeTotal", activeVolumeTotal);
        summary.put("activeTurnoverTotal", activeTurnoverTotal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("brokers", brokers);
        return result;
    }
}
