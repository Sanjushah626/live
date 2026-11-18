package com.myshareapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "portfolio_id", nullable = false)
    private Integer portfolioId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(name = "purchase_type", nullable = false, length = 50)
    private String purchaseType = "Secondary";

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "buy_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal buyPrice = BigDecimal.ZERO;

    @Column(name = "buy_date")
    private LocalDate buyDate;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "sold_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal soldValue = BigDecimal.ZERO;

    @Column(name = "sold_units", nullable = false)
    private Integer soldUnits = 0;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getPurchaseType() { return purchaseType; }
    public void setPurchaseType(String purchaseType) { this.purchaseType = purchaseType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }
    public LocalDate getBuyDate() { return buyDate; }
    public void setBuyDate(LocalDate buyDate) { this.buyDate = buyDate; }
    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }
    public BigDecimal getSoldValue() { return soldValue; }
    public void setSoldValue(BigDecimal soldValue) { this.soldValue = soldValue; }
    public Integer getSoldUnits() { return soldUnits; }
    public void setSoldUnits(Integer soldUnits) { this.soldUnits = soldUnits; }
}
