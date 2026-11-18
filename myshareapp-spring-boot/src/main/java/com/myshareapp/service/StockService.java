package com.myshareapp.service;

import com.myshareapp.model.Portfolio;
import com.myshareapp.model.SoldStock;
import com.myshareapp.model.Stock;
import com.myshareapp.repository.PortfolioRepository;
import com.myshareapp.repository.SoldStockRepository;
import com.myshareapp.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StockService {
    private final PortfolioRepository portfolioRepo;
    private final StockRepository stockRepo;
    private final SoldStockRepository soldStockRepo;

    public StockService(PortfolioRepository portfolioRepo, StockRepository stockRepo, SoldStockRepository soldStockRepo) {
        this.portfolioRepo = portfolioRepo;
        this.stockRepo = stockRepo;
        this.soldStockRepo = soldStockRepo;
    }

    public Stock addStock(Integer userId, Integer portfolioId, String rawSymbol, String purchaseType,
                          int quantity, BigDecimal rawBuyPrice, String buyDateStr) {
        Portfolio portfolio = portfolioRepo.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));
        String symbol = rawSymbol == null ? "" : rawSymbol.trim().toUpperCase();
        if (symbol.isEmpty() || quantity < 1) throw new IllegalArgumentException("Invalid stock input.");
        String type = (purchaseType == null || purchaseType.isBlank()) ? "Secondary" : purchaseType.trim();
        BigDecimal buyPrice = "bonus".equalsIgnoreCase(type) ? BigDecimal.ZERO : (rawBuyPrice == null ? BigDecimal.ZERO : rawBuyPrice);
        LocalDate buyDate = (buyDateStr == null || buyDateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(buyDateStr.trim());

        Optional<Stock> existingOpt = stockRepo.findByPortfolioIdAndSymbolIgnoreCase(portfolio.getId(), symbol);
        if (existingOpt.isPresent()) {
            Stock existing = existingOpt.get();
            int newQty = existing.getQuantity() + quantity;
            BigDecimal totalCost = existing.getBuyPrice().multiply(BigDecimal.valueOf(existing.getQuantity()))
                    .add(buyPrice.multiply(BigDecimal.valueOf(quantity)));
            BigDecimal avg = newQty > 0 ? totalCost.divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            List<String> types = new ArrayList<>(Arrays.stream(existing.getPurchaseType().split("\\+"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
            if (!types.contains(type)) types.add(type);
            existing.setPurchaseType(String.join("+", types));
            existing.setQuantity(newQty);
            existing.setBuyPrice(avg);
            if (existing.getBuyDate() == null || buyDate.isBefore(existing.getBuyDate())) {
                existing.setBuyDate(buyDate);
            }
            return stockRepo.save(existing);
        }

        Stock s = new Stock();
        s.setPortfolioId(portfolio.getId());
        s.setSymbol(symbol);
        s.setPurchaseType(type);
        s.setQuantity(quantity);
        s.setBuyPrice(buyPrice.setScale(2, RoundingMode.HALF_UP));
        s.setBuyDate(buyDate);
        return stockRepo.save(s);
    }

    public Stock editStock(Integer userId, Integer stockId, String symbol, String purchaseType,
                           int quantity, BigDecimal buyPrice, String buyDateStr) {
        Stock s = findForUser(stockId, userId);
        String type = (purchaseType == null || purchaseType.isBlank()) ? "Secondary" : purchaseType.trim();
        if (symbol != null && !symbol.isBlank()) s.setSymbol(symbol.trim().toUpperCase());
        s.setPurchaseType(type);
        s.setQuantity(Math.max(0, quantity));
        s.setBuyPrice("bonus".equalsIgnoreCase(type) ? BigDecimal.ZERO : (buyPrice == null ? BigDecimal.ZERO : buyPrice));
        s.setBuyDate(buyDateStr != null && !buyDateStr.isBlank() ? LocalDate.parse(buyDateStr.trim()) : null);
        return stockRepo.save(s);
    }

    @Transactional
    public void deleteStock(Integer userId, Integer stockId) {
        Stock s = findForUser(stockId, userId);
        Portfolio p = portfolioRepo.findById(s.getPortfolioId()).orElseThrow();
        List<SoldStock> sales = soldStockRepo.findByStockId(s.getId());
        BigDecimal soldVal = BigDecimal.ZERO;
        BigDecimal netProf = BigDecimal.ZERO;
        for (SoldStock ss : sales) {
            soldVal = soldVal.add(ss.getSaleValue());
            netProf = netProf.add(ss.getNetProfit());
        }
        p.setTotalSoldValue(p.getTotalSoldValue().subtract(soldVal));
        p.setTotalRealizedProfit(p.getTotalRealizedProfit().subtract(netProf));
        portfolioRepo.save(p);
        soldStockRepo.deleteByStockId(s.getId());
        stockRepo.delete(s);
    }

    @Transactional
    public SoldStock sellStock(Integer userId, Integer stockId, int sellQty, BigDecimal sellPrice, String sellDateStr) {
        if (sellQty <= 0 || sellPrice == null || sellPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid sell input.");
        }
        Stock s = findForUser(stockId, userId);
        Portfolio p = portfolioRepo.findById(s.getPortfolioId()).orElseThrow();
        if (sellQty > s.getQuantity()) {
            throw new IllegalArgumentException("You can't sell more units than you currently hold.");
        }
        BigDecimal inv = s.getBuyPrice().multiply(BigDecimal.valueOf(sellQty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saleVal = sellPrice.multiply(BigDecimal.valueOf(sellQty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal broker = brokerCommission(saleVal);
        BigDecimal sebon = saleVal.multiply(new BigDecimal("0.00015")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dp = new BigDecimal("25.00");
        BigDecimal netRecv = saleVal.subtract(broker).subtract(sebon).subtract(dp);
        BigDecimal gross = netRecv.subtract(inv);
        double taxRate = cgtRate(s.getBuyDate(), p.getPortfolioType());
        BigDecimal tax = gross.compareTo(BigDecimal.ZERO) > 0
                ? gross.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal netProfit = gross.subtract(tax).setScale(2, RoundingMode.HALF_UP);

        SoldStock ss = new SoldStock();
        ss.setPortfolioId(p.getId());
        ss.setStockId(s.getId());
        ss.setSymbol(s.getSymbol());
        ss.setQuantitySold(sellQty);
        ss.setBuyPrice(s.getBuyPrice());
        ss.setSellPrice(sellPrice.setScale(2, RoundingMode.HALF_UP));
        ss.setInvestmentCost(inv);
        ss.setSaleValue(saleVal);
        ss.setBrokerCharge(broker);
        ss.setSebonCharge(sebon);
        ss.setDpCharge(dp);
        ss.setCapitalGainTax(tax);
        ss.setNetProfit(netProfit);
        ss.setSellDate(sellDateStr != null && !sellDateStr.isBlank() ? LocalDate.parse(sellDateStr.trim()) : LocalDate.now());
        soldStockRepo.save(ss);

        s.setQuantity(s.getQuantity() - sellQty);
        s.setSoldUnits(s.getSoldUnits() + sellQty);
        s.setSoldValue(s.getSoldValue().add(saleVal));
        stockRepo.save(s);

        p.setTotalSoldValue(p.getTotalSoldValue().add(saleVal));
        p.setTotalRealizedProfit(p.getTotalRealizedProfit().add(netProfit));
        portfolioRepo.save(p);
        return ss;
    }

    public Stock findForUser(Integer stockId, Integer userId) {
        Stock s = stockRepo.findById(stockId).orElseThrow(() -> new IllegalArgumentException("Stock not found."));
        portfolioRepo.findByIdAndUserId(s.getPortfolioId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Access denied."));
        return s;
    }

    private BigDecimal brokerCommission(BigDecimal amount) {
        double a = amount.doubleValue();
        double rate = a <= 50000 ? 0.0036 : a <= 500000 ? 0.0033 : a <= 2000000 ? 0.0031 : a <= 10000000 ? 0.0027 : 0.0024;
        double c = Math.max(10.0, Math.round(a * rate * 100.0) / 100.0);
        return BigDecimal.valueOf(c).setScale(2, RoundingMode.HALF_UP);
    }

    private double cgtRate(LocalDate buyDate, String portfolioType) {
        if ("institutional".equalsIgnoreCase(portfolioType) || buyDate == null) return 0.10;
        long days = ChronoUnit.DAYS.between(buyDate, LocalDate.now());
        return days >= 365 ? 0.075 : 0.10;
    }
}
