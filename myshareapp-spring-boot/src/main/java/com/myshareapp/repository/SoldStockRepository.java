package com.myshareapp.repository;

import com.myshareapp.model.SoldStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoldStockRepository extends JpaRepository<SoldStock, Integer> {
    List<SoldStock> findByStockId(Integer stockId);
    void deleteByStockId(Integer stockId);
    void deleteByPortfolioId(Integer portfolioId);
}
