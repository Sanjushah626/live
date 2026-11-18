package com.myshareapp.repository;

import com.myshareapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    List<User> findAllByOrderByCreatedAtDesc();
}
