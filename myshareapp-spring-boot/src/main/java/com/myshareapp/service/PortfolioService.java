package com.myshareapp.service;

import com.myshareapp.model.Portfolio;
import com.myshareapp.model.Stock;
import com.myshareapp.repository.PortfolioRepository;
import com.myshareapp.repository.SoldStockRepository;
import com.myshareapp.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepo;
    private final StockRepository stockRepo;
    private final SoldStockRepository soldStockRepo;

    public PortfolioService(PortfolioRepository portfolioRepo, StockRepository stockRepo, SoldStockRepository soldStockRepo) {
        this.portfolioRepo = portfolioRepo;
        this.stockRepo = stockRepo;
        this.soldStockRepo = soldStockRepo;
    }

    public List<Map<String, Object>> getUserPortfoliosWithViews(Integer userId) {
        List<Portfolio> list = portfolioRepo.findByUserId(userId);
        List<Map<String, Object>> views = new ArrayList<>();
        for (Portfolio p : list) {
            List<Stock> stocks = stockRepo.findByPortfolioIdOrderBySymbolAsc(p.getId());
            BigDecimal totalInv = BigDecimal.ZERO;
            int totalUnits = 0;
            List<Map<String, Object>> stockViews = new ArrayList<>();
            for (Stock s : stocks) {
                BigDecimal inv = s.getBuyPrice().multiply(BigDecimal.valueOf(s.getQuantity()));
                totalInv = totalInv.add(inv);
                totalUnits += s.getQuantity();
                String pType = s.getPurchaseType() == null ? "Secondary" : s.getPurchaseType();
                Map<String, Object> sv = new LinkedHashMap<>();
                sv.put("id", s.getId());
                sv.put("symbol", s.getSymbol().toUpperCase());
                sv.put("purchaseType", pType);
                sv.put("purchaseTypeLower", pType.toLowerCase());
                sv.put("quantity", s.getQuantity());
                sv.put("buyPrice", s.getBuyPrice());
                sv.put("investment", inv);
                sv.put("buyDate", s.getBuyDate() != null ? s.getBuyDate().toString() : "");
                sv.put("soldUnits", s.getSoldUnits());
                sv.put("soldValue", s.getSoldValue());
                sv.put("soldOut", s.getQuantity() == 0);
                stockViews.add(sv);
            }
            BigDecimal realized = p.getTotalRealizedProfit() != null ? p.getTotalRealizedProfit() : BigDecimal.ZERO;
            String rClass = realized.compareTo(BigDecimal.ZERO) > 0 ? "positive"
                    : realized.compareTo(BigDecimal.ZERO) < 0 ? "negative" : "neutral";
            String rLabel = realized.compareTo(BigDecimal.ZERO) > 0 ? "Realized Profit"
                    : realized.compareTo(BigDecimal.ZERO) < 0 ? "Realized Loss" : "Realized P/L";

            Map<String, Object> pv = new LinkedHashMap<>();
            pv.put("id", p.getId());
            pv.put("portfolioName", p.getPortfolioName());
            pv.put("portfolioType", p.getPortfolioType());
            pv.put("totalInvestment", totalInv);
            pv.put("totalUnits", totalUnits);
            pv.put("totalSoldValue", p.getTotalSoldValue());
            pv.put("totalRealizedProfit", realized);
            pv.put("totalRealizedAbs", realized.abs());
            pv.put("realizedClass", rClass);
            pv.put("realizedLabel", rLabel);
            pv.put("stocks", stockViews);
            views.add(pv);
        }
        return views;
    }

    public Portfolio createPortfolio(Integer userId, String name, String type) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Portfolio name is required.");
        if (portfolioRepo.existsByUserIdAndPortfolioNameIgnoreCase(userId, clean)) {
            throw new IllegalStateException("duplicate_portfolio");
        }
        Portfolio p = new Portfolio();
        p.setUserId(userId);
        p.setPortfolioName(clean);
        p.setPortfolioType("institutional".equalsIgnoreCase(type) ? "institutional" : "personal");
        return portfolioRepo.save(p);
    }

    public void updatePortfolio(Integer userId, Integer portfolioId, String name, String type) {
        Portfolio p = portfolioRepo.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));
        String clean = name == null ? "" : name.trim();
        if (!clean.isEmpty()) p.setPortfolioName(clean);
        p.setPortfolioType("institutional".equalsIgnoreCase(type) ? "institutional" : "personal");
        portfolioRepo.save(p);
    }

    @Transactional
    public void deletePortfolio(Integer userId, Integer portfolioId) {
        Portfolio p = portfolioRepo.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));
        soldStockRepo.deleteByPortfolioId(p.getId());
        stockRepo.deleteByPortfolioId(p.getId());
        portfolioRepo.delete(p);
    }

    public String buildCsv(List<Portfolio> selected) {
        StringBuilder sb = new StringBuilder("Portfolio,Type,Symbol,Purchase Type,Quantity,Avg Buy Price,Investment,Sold Units,Sold Value,Realized Profit,Buy Date\r\n");
        for (Portfolio p : selected) {
            List<Stock> stocks = stockRepo.findByPortfolioIdOrderBySymbolAsc(p.getId());
            if (stocks.isEmpty()) {
                sb.append(csvCell(p.getPortfolioName())).append(',')
                  .append(csvCell(p.getPortfolioType())).append(",-,-,0,0.00,0.00,0,")
                  .append(p.getTotalSoldValue()).append(',').append(p.getTotalRealizedProfit()).append(",-\r\n");
                continue;
            }
            for (Stock s : stocks) {
                BigDecimal inv = s.getBuyPrice().multiply(BigDecimal.valueOf(s.getQuantity()));
                sb.append(csvCell(p.getPortfolioName())).append(',')
                  .append(csvCell(p.getPortfolioType())).append(',')
                  .append(csvCell(s.getSymbol())).append(',')
                  .append(csvCell(s.getPurchaseType())).append(',')
                  .append(s.getQuantity()).append(',')
                  .append(s.getBuyPrice()).append(',')
                  .append(inv).append(',')
                  .append(s.getSoldUnits()).append(',')
                  .append(s.getSoldValue()).append(',')
                  .append(p.getTotalRealizedProfit()).append(',')
                  .append(s.getBuyDate() != null ? s.getBuyDate().toString() : "-").append("\r\n");
            }
        }
        return sb.toString();
    }

    private String csvCell(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
