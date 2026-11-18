package com.myshareapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sold_stocks")
public class SoldStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "portfolio_id", nullable = false)
    private Integer portfolioId;

    @Column(name = "stock_id")
    private Integer stockId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;

    @Column(name = "buy_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "investment_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal investmentCost;

    @Column(name = "sale_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal saleValue;

    @Column(name = "broker_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal brokerCharge;

    @Column(name = "sebon_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal sebonCharge;

    @Column(name = "dp_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal dpCharge;

    @Column(name = "capital_gain_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal capitalGainTax;

    @Column(name = "net_profit", nullable = false, precision = 14, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "sell_date")
    private LocalDate sellDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }
    public Integer getStockId() { return stockId; }
    public void setStockId(Integer stockId) { this.stockId = stockId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }
    public BigDecimal getSellPrice() { return sellPrice; }
    public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }
    public BigDecimal getInvestmentCost() { return investmentCost; }
    public void setInvestmentCost(BigDecimal investmentCost) { this.investmentCost = investmentCost; }
    public BigDecimal getSaleValue() { return saleValue; }
    public void setSaleValue(BigDecimal saleValue) { this.saleValue = saleValue; }
    public BigDecimal getBrokerCharge() { return brokerCharge; }
    public void setBrokerCharge(BigDecimal brokerCharge) { this.brokerCharge = brokerCharge; }
    public BigDecimal getSebonCharge() { return sebonCharge; }
    public void setSebonCharge(BigDecimal sebonCharge) { this.sebonCharge = sebonCharge; }
    public BigDecimal getDpCharge() { return dpCharge; }
    public void setDpCharge(BigDecimal dpCharge) { this.dpCharge = dpCharge; }
    public BigDecimal getCapitalGainTax() { return capitalGainTax; }
    public void setCapitalGainTax(BigDecimal capitalGainTax) { this.capitalGainTax = capitalGainTax; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public LocalDate getSellDate() { return sellDate; }
    public void setSellDate(LocalDate sellDate) { this.sellDate = sellDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
