package com.myshareapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "portfolio_name", nullable = false, length = 100)
    private String portfolioName;

    @Column(name = "portfolio_type", nullable = false, length = 20)
    private String portfolioType = "personal";

    @Column(name = "total_sold_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSoldValue = BigDecimal.ZERO;

    @Column(name = "total_realized_profit", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRealizedProfit = BigDecimal.ZERO;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public String getPortfolioType() { return portfolioType; }
    public void setPortfolioType(String portfolioType) { this.portfolioType = portfolioType; }
    public BigDecimal getTotalSoldValue() { return totalSoldValue; }
    public void setTotalSoldValue(BigDecimal totalSoldValue) { this.totalSoldValue = totalSoldValue; }
    public BigDecimal getTotalRealizedProfit() { return totalRealizedProfit; }
    public void setTotalRealizedProfit(BigDecimal totalRealizedProfit) { this.totalRealizedProfit = totalRealizedProfit; }
}
