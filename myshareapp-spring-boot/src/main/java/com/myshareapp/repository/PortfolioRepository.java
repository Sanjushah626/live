package com.myshareapp.repository;

import com.myshareapp.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    List<Portfolio> findByUserId(Integer userId);
    Optional<Portfolio> findByIdAndUserId(Integer id, Integer userId);
    boolean existsByUserIdAndPortfolioNameIgnoreCase(Integer userId, String portfolioName);
    long countByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
