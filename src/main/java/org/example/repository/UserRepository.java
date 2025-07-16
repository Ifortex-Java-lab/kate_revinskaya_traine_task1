package org.example.repository;

import org.example.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStripeCustomerId(String stripeCustomerId);
    Optional<User> findByEmail(String email);
}