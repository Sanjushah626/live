package com.myshareapp.controller;

import com.myshareapp.service.YonepseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class NepseMarketController {

    private final YonepseService yonepse;

    public NepseMarketController(YonepseService yonepse) {
        this.yonepse = yonepse;
    }

    @GetMapping("/IsNepseOpen")
    public Map<String, Object> isNepseOpen() {
        return yonepse.getMarketStatus();
    }

    @GetMapping("/DailyNepseIndexGraph")
    public List<List<Object>> dailyNepseIndexGraph() {
        return yonepse.getDailyNepseIndexGraph();
    }

    @GetMapping("/TopTenTradeScrips")
    public Object topTenTradeScrips() {
        return yonepse.getTopStocks().getOrDefault("top_trade", List.of());
    }

    @GetMapping("/TopTurnoverScrips")
    public Object topTurnoverScrips() {
        return yonepse.getTopStocks().getOrDefault("top_turnover", List.of());
    }

    @GetMapping("/TopGainers")
    public Object topGainers() {
        return yonepse.getTopStocks().getOrDefault("top_gainer", List.of());
    }

    @GetMapping("/TopLosers")
    public Object topLosers() {
        return yonepse.getTopStocks().getOrDefault("top_loser", List.of());
    }

    @GetMapping("/NepseIndex")
    public Map<String, Object> nepseIndex() {
        return yonepse.getNepseIndex();
    }

    @GetMapping("/NepseSubIndices")
    public Map<String, Object> nepseSubIndices() {
        return yonepse.getNepseSubIndices();
    }

    @GetMapping("/Summary")
    public Map<String, Object> summary() {
        return yonepse.getSummary();
    }

    @GetMapping({"/LiveMarket", "/PriceVolume"})
    public List<Map<String, Object>> liveMarket() {
        return yonepse.getLiveMarket();
    }

    @GetMapping("/MarketDepth")
    public Map<String, Object> marketDepth(@RequestParam(defaultValue = "NABIL") String symbol) {
        return yonepse.getMarketDepth(symbol);
    }

    @GetMapping("/DailyScripPriceGraph")
    public List<Map<String, Object>> dailyScripPriceGraph(@RequestParam(defaultValue = "NABIL") String symbol) {
        return yonepse.getDailyScripPriceGraph(symbol);
    }

    @GetMapping("/MarketHistory")
    public List<Map<String, Object>> marketHistory(@RequestParam(defaultValue = "30") int limit) {
        return yonepse.getMarketHistory(Math.min(90, Math.max(5, limit)));
    }

    @GetMapping({"/TopBrokers", "/Brokers", "/api/brokers"})
    public Map<String, Object> topBrokers() {
        return yonepse.getTopBrokers();
    }

    @GetMapping("/api/yonepse/news")
    public List<Map<String, Object>> apiYonepseNews() {
        return yonepse.getExchangeNews(40);
    }

    @GetMapping("/api/yonepse/ipos")
    public Map<String, Object> apiYonepseIpos() {
        return Map.of(
                "upcoming", yonepse.getUpcomingIpos(),
                "recent", yonepse.getRecentIpos(20)
        );
    }

    @GetMapping("/api/yonepse/dividends")
    public List<Map<String, Object>> apiYonepseDividends() {
        return yonepse.getRecentDividends(80);
    }
}
