package com.resumeai.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.resumeai.auth.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(String role);

    List<User> findBySubscriptionPlan(String plan);

    List<User> findByIsActive(boolean active);

    List<User> findByPremiumActiveTrueAndSubscriptionEndDateBefore(LocalDateTime currentDateTime);

    void deleteById(Long userId);
}
