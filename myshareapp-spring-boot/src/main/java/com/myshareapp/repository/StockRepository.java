package com.myshareapp.repository;

import com.myshareapp.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Integer> {
    List<Stock> findByPortfolioIdOrderBySymbolAsc(Integer portfolioId);
    Optional<Stock> findByPortfolioIdAndSymbolIgnoreCase(Integer portfolioId, String symbol);
    long countByPortfolioId(Integer portfolioId);
    void deleteByPortfolioId(Integer portfolioId);
}
