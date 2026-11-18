package com.myshareapp.repository;

import com.myshareapp.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Integer> {
    List<News> findAllByOrderByCreatedAtDesc();
}
